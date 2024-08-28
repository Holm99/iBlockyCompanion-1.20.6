package net.holm.boosternoti;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
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
    private boolean boosterChecked = false; // Track if booster check command was sent

    // Regular expression pattern to detect booster output precisely without player names
    private static final Pattern BOOSTER_PATTERN = Pattern.compile(".*Tokens \\((.*?)\\) \\((.*?) remaining\\).*|.*Boosters:.*|.*\\s+- None.*|.*Your Token booster has expired.*");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Booster Display Mod (Client) has been initialized!");

        // Register the HUD render callback to display the booster information on screen
        HudRenderCallback.EVENT.register(BoosterGUI::render);

        // Register event to listen for chat messages
        registerChatListener();

        // Send the booster check command periodically
        scheduleBoosterCheck();
    }

    private void registerChatListener() {
        // Register a listener for incoming chat messages using Fabric API
        ClientReceiveMessageEvents.ALLOW_CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            // Convert the message to plain text
            String msg = message.getString(); // Convert message component to string

            LOGGER.info("Received chat message: {}", msg); // Log the received message

            // Use the pattern matcher to detect valid server messages
            Matcher matcher = BOOSTER_PATTERN.matcher(msg);

            if (matcher.matches()) {
                LOGGER.info("Message matches the booster pattern."); // Log pattern match success
                if (msg.equals("Boosters:")) {
                    boosterChecked = true; // Indicates the booster command response has begun
                    LOGGER.info("Booster check started."); // Log booster check start
                } else if (boosterChecked && msg.contains("None")) {
                    // Case: No boosters are active
                    BoosterGUI.clearBoosterInfo();
                    boosterChecked = false; // Reset for the next check
                    LOGGER.info("No active boosters found."); // Log no boosters
                } else if (boosterChecked && msg.startsWith("Tokens")) {
                    // Case: Parse booster information
                    boosterChecked = false; // Reset after parsing
                    String multiplier = matcher.group(1).trim();
                    String remaining = matcher.group(2).replace("remaining", "").trim();
                    BoosterGUI.updateBoosterInfo(multiplier, remaining);
                    LOGGER.info("Updated booster info: Multiplier={}, Remaining={}", multiplier, remaining); // Log booster info update
                } else if (msg.equals("Your Token booster has expired")) {
                    // Case: Booster has expired
                    BoosterGUI.clearBoosterInfo();
                    LOGGER.info("Booster has expired."); // Log booster expiration
                }
            } else {
                LOGGER.info("Message did not match the booster pattern."); // Log pattern match failure
            }

            return true; // Allow the message to be displayed
        });
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void scheduleBoosterCheck() {
        // Schedule a repeating task to run the /booster command every 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.networkHandler.sendCommand("/booster");
                }
            });
        }, 0, 60, TimeUnit.SECONDS);
    }
}