package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(iBlockyBoosterNotification.MOD_ID);
    private boolean boosterChecked = false; // Track if booster check command was sent
    private boolean isBoosterActive = false; // Flag to check if a booster is currently active

    private static final Pattern BOOSTER_PATTERN = Pattern.compile(".*Tokens \\((.*?)\\) \\((.*?) remaining\\).*");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Booster Display Mod (Client) has been initialized!");

        // Register the HUD render callback
        HudRenderCallback.EVENT.register(new BoosterStatusWindow());

        // Register chat message listener
        registerChatListener();

        // Schedule the booster check command
        scheduleBoosterCheck();

        // Register mouse events for dragging
        registerMouseEvents();
    }

    private void registerChatListener() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString(); // Convert message to string

            LOGGER.info("Received chat message: " + msg); // Log the received message

            Matcher matcher = BOOSTER_PATTERN.matcher(msg);

            if (matcher.matches()) {
                LOGGER.info("Booster detected with pattern match.");
                String multiplier = matcher.group(1).trim();
                String remaining = matcher.group(2).replace("remaining", "").trim();
                BoosterStatusWindow.setBoosterActive(true, remaining);
                LOGGER.info("Booster info updated: Multiplier=" + multiplier + ", Remaining=" + remaining);

                // Parse remaining time and convert it to seconds
                int totalSeconds = parseTimeToSeconds(remaining);

                // Stop further checks until booster expires
                isBoosterActive = true;

                // Start countdown until booster expires
                startCountdown(totalSeconds);
            } else {
                LOGGER.info("No booster detected or message does not match pattern.");
            }

            return true; // Allow other handlers to process this message as well
        });
    }

    private void registerMouseEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) { // Only handle mouse events when a screen is open
                double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
                double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

                // Check if the left mouse button is pressed
                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    BoosterStatusWindow.handleMousePress(mouseX, mouseY);
                } else {
                    BoosterStatusWindow.handleMouseRelease();
                }
            }
        });
    }

    private void scheduleBoosterCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isBoosterActive) { // Only schedule command if no booster is active
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.networkHandler.sendCommand("/booster");
                    }
                });
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void startCountdown(int seconds) {
        scheduler.schedule(() -> {
            BoosterStatusWindow.setBoosterActive(false, "");
            isBoosterActive = false; // Booster expired, resume checks
        }, seconds, TimeUnit.SECONDS);
    }

    // Utility method to parse remaining time in format "2d 8h 51m 33s" to seconds
    private int parseTimeToSeconds(String time) {
        int totalSeconds = 0;

        String[] parts = time.split(" ");
        for (String part : parts) {
            if (part.endsWith("d")) {
                int days = Integer.parseInt(part.replace("d", ""));
                totalSeconds += days * 86400; // Convert days to seconds
            } else if (part.endsWith("h")) {
                int hours = Integer.parseInt(part.replace("h", ""));
                totalSeconds += hours * 3600; // Convert hours to seconds
            } else if (part.endsWith("m")) {
                int minutes = Integer.parseInt(part.replace("m", ""));
                totalSeconds += minutes * 60; // Convert minutes to seconds
            } else if (part.endsWith("s")) {
                int seconds = Integer.parseInt(part.replace("s", ""));
                totalSeconds += seconds;
            }
        }

        return totalSeconds;
    }
}