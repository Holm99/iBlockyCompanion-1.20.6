package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private boolean isBoosterActive = false;
    private boolean isRichBoosterActive = false;

    // Pattern to match tokens booster messages
    private static final Pattern BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);

    // Pattern to match rich pet booster messages
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new BoosterStatusWindow());
        registerMessageListeners();
        registerMouseEvents();  // Retain this for mouse dragging
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
            StringBuilder remaining = new StringBuilder();

            // Collect all parts of remaining time
            for (int i = 3; i <= 6; i++) {
                if (matcher.group(i) != null) remaining.append(matcher.group(i));
            }

            if (multiplier != null && (!remaining.isEmpty())) {
                multiplier = multiplier.trim();
                remaining = new StringBuilder(remaining.toString().replace("remaining", "").trim());

                // Update the BoosterStatusWindow with the active booster details
                BoosterStatusWindow.setTokensBoosterActive(true, multiplier, remaining.toString());

                isBoosterActive = true;
            }
        }

        // Check for rich pet booster message
        Matcher richMatcher = RICH_BOOSTER_PATTERN.matcher(msg);
        if (richMatcher.find()) {
            StringBuilder remaining = new StringBuilder();

            // Collect all parts of remaining time
            for (int i = 1; i <= 4; i++) {
                if (richMatcher.group(i) != null) remaining.append(richMatcher.group(i));
            }

            if (!remaining.isEmpty()) {
                remaining = new StringBuilder(remaining.toString().trim());

                // Update the BoosterStatusWindow with the rich pet booster details
                BoosterStatusWindow.setRichBoosterActive(true, remaining.toString());

                isRichBoosterActive = true;
            }
        }
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