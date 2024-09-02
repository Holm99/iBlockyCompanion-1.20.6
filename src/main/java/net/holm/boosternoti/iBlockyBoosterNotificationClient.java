package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.client.network.PlayerListEntry;
import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private boolean isBoosterActive = false;
    private boolean isRichBoosterActive = false;
    private static KeyBinding boosterKeyBinding;
    private String lastKnownPrefix = null;

    private static final Pattern BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new BoosterStatusWindow());
        registerMessageListeners();
        registerMouseEvents();
        registerLogoutEvent();
        registerKeyBindings();
        registerKeyPressEvent();
        registerJoinEvent();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().currentScreen == null) {
                BoosterStatusWindow.handleScreenClose();
            }
        });
    }

    // Register event when player joins the server
    private void registerJoinEvent() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("[DEBUG] Player joined the server. Starting to fetch display name.");
            startFetchingDisplayName();
        });
    }

    // Method to start fetching the display name with a fixed rate
    private void startFetchingDisplayName() {
        scheduler.scheduleAtFixedRate(this::fetchAndLogPlayerPrefix, 2, 5, TimeUnit.SECONDS);
    }

    private void fetchAndLogPlayerPrefix() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            System.out.println("Player is initialized.");

            // Get the player's UUID and GameProfile
            GameProfile playerProfile = client.player.getGameProfile();

            if (client.getNetworkHandler() == null) {
                System.out.println("Network handler is not available. Cannot fetch player prefix.");
                return;
            }

            if (client.getNetworkHandler().getPlayerList().isEmpty()) {
                System.out.println("Player list is empty, waiting for synchronization...");
                return;
            }

            System.out.println("Fetching player list entries.");

            for (PlayerListEntry entry : Objects.requireNonNull(client.getNetworkHandler()).getPlayerList()) {
                System.out.println("Checking entry for player UUID: " + entry.getProfile().getId());

                if (entry.getProfile().getId().equals(playerProfile.getId())) {
                    Text displayName = entry.getDisplayName();
                    String listName = entry.getProfile().getName(); // Default name without any prefix

                    System.out.println("Matched player UUID. Display name is: " + (displayName != null ? displayName.getString() : "null"));
                    System.out.println("List name (default name): " + listName);

                    // Check if there is any team prefix (some servers use teams to set prefixes)
                    if (entry.getScoreboardTeam() != null) {
                        String teamName = entry.getScoreboardTeam().getName();
                        String teamPrefix = entry.getScoreboardTeam().getPrefix().getString();

                        System.out.println("Player is in team: " + teamName);
                        System.out.println("Team prefix: " + teamPrefix);

                        // Update only if there is a change in the prefix
                        if (!teamPrefix.equals(lastKnownPrefix)) {
                            lastKnownPrefix = teamPrefix;
                            System.out.println("Updated Team Prefix: " + teamPrefix);
                            // Assuming SellBoostCalculator has a method to update rank based on team prefix
                            SellBoostCalculator.setRank(teamPrefix);
                        }
                    } else {
                        System.out.println("Player is not in any team.");
                        // Handle cases where the player is not in any team
                        if (lastKnownPrefix != null) {
                            lastKnownPrefix = null;
                            System.out.println("Player Name (no prefix): " + listName);
                        }
                    }

                    // Additional logic if needed, e.g., further calculations or updates

                    break; // Exit the loop once we find our player
                }
            }
        } else {
            System.out.println("Player is not initialized yet.");
        }
    }

    private void registerMessageListeners() {
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString();
            if (!msg.contains("Backpack Space")) {
                processChatMessage(msg, false);
            }
        });

        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            String msg = message.getString();
            if (!msg.contains("Backpack Space")) {
                processChatMessage(msg, true);
            }
        });
    }

    private void processChatMessage(String msg, boolean isGameMessage) {
        if (msg.contains("Backpack Space")) {
            return;
        }

        Matcher matcher = BOOSTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            String multiplier = matcher.group(1);
            StringBuilder remaining = new StringBuilder();

            for (int i = 3; i <= 6; i++) {
                if (matcher.group(i) != null) remaining.append(matcher.group(i));
            }

            if (multiplier != null && (!remaining.isEmpty())) {
                multiplier = multiplier.trim();
                remaining = new StringBuilder(remaining.toString().replace("remaining", "").trim());

                BoosterStatusWindow.setTokensBoosterActive(true, multiplier, remaining.toString());

                isBoosterActive = true;
            }
        }

        Matcher richMatcher = RICH_BOOSTER_PATTERN.matcher(msg);
        if (richMatcher.find()) {
            StringBuilder remaining = new StringBuilder();

            for (int i = 1; i <= 4; i++) {
                if (richMatcher.group(i) != null) remaining.append(richMatcher.group(i));
            }

            if (!remaining.isEmpty()) {
                remaining = new StringBuilder(remaining.toString().trim());

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

    private void registerKeyBindings() {
        boosterKeyBinding = new KeyBinding(
                "key.boosternoti.booster",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.boosternoti.general"
        );

        KeyBindingHelper.registerKeyBinding(boosterKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed()) {
                sendBoosterCommand();
            }
        });
    }

    private void registerKeyPressEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed()) {
                sendBoosterCommand();
            }
        });
    }

    private void sendBoosterCommand() {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("booster");
        }
    }

    private int parseTimeToSeconds(String time) {
        int totalSeconds = 0;

        String[] parts = time.split(" ");
        for (String part : parts) {
            if (part.endsWith("d")) {
                totalSeconds += Integer.parseInt(part.replace("d", "")) * 86400;
            } else if (part.endsWith("h")) {
                totalSeconds += Integer.parseInt(part.replace("h", "")) * 3600;
            } else if (part.endsWith("m")) {
                totalSeconds += Integer.parseInt(part.replace("m", "")) * 60;
            } else if (part.endsWith("s")) {
                totalSeconds += Integer.parseInt(part.replace("s", ""));
            }
        }

        return totalSeconds;
    }
}
