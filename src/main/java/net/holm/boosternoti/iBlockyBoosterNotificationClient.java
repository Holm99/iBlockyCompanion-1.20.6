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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(iBlockyBoosterNotification.MOD_ID);
    private boolean boosterChecked = false;
    private boolean isBoosterActive = false;

    // Pattern to match booster messages
    private static final Pattern BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);

    // Pattern to identify "Boosters:" line in the chat
    private static final Pattern BOOSTERS_HEADER_PATTERN = Pattern.compile("Boosters:", Pattern.CASE_INSENSITIVE);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Booster Display Mod (Client) has been initialized!");

        HudRenderCallback.EVENT.register(new BoosterStatusWindow());
        registerMessageListeners();
        scheduleBoosterCheck();
        registerMouseEvents();
        registerLogoutEvent();
    }

    private void registerMessageListeners() {
        LOGGER.info("Registering message listeners...");

        // Register client-side chat listener using lambda expression
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString(); // Convert Text object to plain String

            // Check if message should be processed
            if (!msg.contains("Backpack Space")) {
                LOGGER.info("Received client chat message: " + msg); // Log only if not ignored
                processChatMessage(msg, false); // false indicates this is a chat message
            }
        });

        // Register client-side game message listener
        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            String msg = message.getString(); // Convert Text to String

            // Check if message should be processed
            if (!msg.contains("Backpack Space")) {
                LOGGER.info("Received client game message: " + msg); // Log only if not ignored
                processChatMessage(msg, true); // true indicates this is a game message
            }
        });

        LOGGER.info("Message listeners registered successfully.");
    }

    private void processChatMessage(String msg, boolean isGameMessage) {
        // Ignore "Backpack Space" messages to reduce log spam
        if (msg.contains("Backpack Space")) {
            return; // Skip processing and logging for this message
        }

        // Log the incoming message only if it's not ignored
        LOGGER.info("Processing message: " + msg + " | Is Game Message: " + isGameMessage);

        // Check if the message contains "Boosters:" header
        if (BOOSTERS_HEADER_PATTERN.matcher(msg).find()) {
            boosterChecked = true;
            LOGGER.info("Booster list detected, checking for boosters...");
        }
        // Check for booster details after the header
        else if (boosterChecked) {
            LOGGER.info("Processing potential booster message: " + msg);

            Matcher matcher = BOOSTER_PATTERN.matcher(msg);

            if (matcher.find()) {
                LOGGER.info("Booster detected with pattern match.");

                String multiplier = matcher.group(1); // Extract the multiplier value
                String remaining = "";

                // Collect all parts of remaining time
                if (matcher.group(3) != null) remaining += matcher.group(3);
                if (matcher.group(4) != null) remaining += matcher.group(4);
                if (matcher.group(5) != null) remaining += matcher.group(5);
                if (matcher.group(6) != null) remaining += matcher.group(6);

                // Debug logs to inspect matcher groups
                LOGGER.info("Matcher group 1 (multiplier): " + multiplier);
                LOGGER.info("Parsed remaining time: " + remaining);

                // Ensure both matches are not null
                if (multiplier != null && !remaining.isEmpty()) {
                    multiplier = multiplier.trim();
                    remaining = remaining.replace("remaining", "").trim();

                    LOGGER.info("Parsed Booster Info: Multiplier=" + multiplier + ", Remaining=" + remaining);

                    // Update the BoosterStatusWindow with the active booster details
                    BoosterStatusWindow.setBoosterActive(true, multiplier, remaining);

                    // Convert the remaining time to seconds for countdown
                    int totalSeconds = parseTimeToSeconds(remaining);
                    LOGGER.info("Starting countdown for " + totalSeconds + " seconds.");

                    // Mark booster as active and start the countdown
                    isBoosterActive = true;
                    startCountdown(totalSeconds);
                } else {
                    LOGGER.warn("Failed to parse booster information. Multiplier or remaining time is null.");
                }

            } else {
                LOGGER.info("No booster detected or message does not match pattern: " + msg);
            }
            boosterChecked = false; // Reset the flag after processing booster info
        } else {
            LOGGER.info("Message does not indicate start of boosters or is out of context: " + msg);
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

    private void scheduleBoosterCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isBoosterActive) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        LOGGER.info("Sending /booster command to check for active boosters.");
                        client.player.networkHandler.sendChatMessage("/booster");
                    }
                });
            } else {
                LOGGER.info("Booster is currently active; skipping /booster check.");
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void startCountdown(int seconds) {
        LOGGER.info("Scheduling countdown for booster expiration in {} seconds.", seconds);
        scheduler.schedule(() -> {
            LOGGER.info("Booster expired. Clearing booster information.");
            BoosterStatusWindow.setBoosterActive(false, "", "");
            isBoosterActive = false;
        }, seconds, TimeUnit.SECONDS);
    }

    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Player logged off. Clearing booster information.");
            BoosterStatusWindow.clearBoosterInfo();
            isBoosterActive = false;
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