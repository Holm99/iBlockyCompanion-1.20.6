package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private boolean isBoosterActive = false;
    private boolean isRichBoosterActive = false;

    // Pattern to match tokens booster messages
    private static final Pattern BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);

    // Pattern to match rich pet booster messages
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new BoosterStatusWindow());
        registerMessageListeners();
        registerCountdownHandlers(); // Ensure countdown handlers are registered
        registerMouseEvents();
        registerLogoutEvent();
    }

    private void registerMessageListeners() {
        // Register client-side chat listener using lambda expression
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString(); // Convert Text object to plain String
            if (!msg.contains("Backpack Space")) {
                processChatMessage(msg, false); // false indicates this is a chat message
            }
        });

        // Register client-side game message listener
        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            String msg = message.getString(); // Convert Text to String
            if (!msg.contains("Backpack Space")) {
                processChatMessage(msg, true); // true indicates this is a game message
            }
        });
    }

    private void processChatMessage(String msg, boolean isGameMessage) {
        // Ignore "Backpack Space" messages to reduce log spam
        if (msg.contains("Backpack Space")) {
            return; // Skip processing for this message
        }

        // Check for tokens booster message
        Matcher matcher = BOOSTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            String multiplier = matcher.group(1); // Extract the multiplier value
            String remaining = "";

            // Collect all parts of remaining time
            if (matcher.group(3) != null) remaining += matcher.group(3);
            if (matcher.group(4) != null) remaining += matcher.group(4);
            if (matcher.group(5) != null) remaining += matcher.group(5);
            if (matcher.group(6) != null) remaining += matcher.group(6);

            // Ensure both matches are not null
            if (multiplier != null && !remaining.isEmpty()) {
                multiplier = multiplier.trim();
                remaining = remaining.replace("remaining", "").trim();

                // Update the BoosterStatusWindow with the active booster details
                BoosterStatusWindow.setTokensBoosterActive(true, multiplier, remaining);

                // Convert the remaining time to seconds for countdown
                int totalSeconds = parseTimeToSeconds(remaining);

                // Start countdown for Tokens Booster
                BoosterStatusWindow.updateTokensBoosterCountdown(totalSeconds);
                isBoosterActive = true;
            }
        }

        // Check for rich pet booster message
        Matcher richMatcher = RICH_BOOSTER_PATTERN.matcher(msg);
        if (richMatcher.find()) {
            String remaining = "";

            // Collect all parts of remaining time
            if (richMatcher.group(1) != null) remaining += richMatcher.group(1);
            if (richMatcher.group(2) != null) remaining += richMatcher.group(2);
            if (richMatcher.group(3) != null) remaining += richMatcher.group(3);
            if (richMatcher.group(4) != null) remaining += richMatcher.group(4);

            // Ensure remaining time is not empty
            if (!remaining.isEmpty()) {
                remaining = remaining.trim();

                // Update the BoosterStatusWindow with the rich pet booster details
                BoosterStatusWindow.setRichBoosterActive(true, remaining);

                // Convert the remaining time to seconds for countdown
                int totalSeconds = parseTimeToSeconds(remaining);

                // Start countdown for Rich Booster
                BoosterStatusWindow.updateRichBoosterCountdown(totalSeconds);
                isRichBoosterActive = true;
            }
        }
    }

    private void registerCountdownHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                // Move countdown handling to BoosterStatusWindow directly
                BoosterStatusWindow.handleCountdown();
            }
        });
    }

    private void registerMouseEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
                double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    BoosterStatusWindow.handleMousePress(mouseX, mouseY);
                } else {
                    BoosterStatusWindow.handleMouseRelease();
                }
            }
        });
    }

    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BoosterStatusWindow.clearBoosterInfo();
            isBoosterActive = false;
            isRichBoosterActive = false;
        });
    }

    private int parseTimeToSeconds(String time) {
        int totalSeconds = 0;

        String[] parts = time.split(" ");
        for (String part : parts) {
            if (part.endsWith("d")) {
                int days = Integer.parseInt(part.replace("d", ""));
                totalSeconds += days * 86400;
            } else if (part.endsWith("h")) {
                int hours = Integer.parseInt(part.replace("h", ""));
                totalSeconds += hours * 3600;
            } else if (part.endsWith("m")) {
                int minutes = Integer.parseInt(part.replace("m", ""));
                totalSeconds += minutes * 60;
            } else if (part.endsWith("s")) {
                int seconds = Integer.parseInt(part.replace("s", ""));
                totalSeconds += seconds;
            }
        }

        return totalSeconds;
    }
}