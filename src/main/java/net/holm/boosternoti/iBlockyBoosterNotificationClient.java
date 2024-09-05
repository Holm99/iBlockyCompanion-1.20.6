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
    private static KeyBinding boosterKeyBinding;
    private static KeyBinding toggleHudKeyBinding;  // Keybinding for toggling HUD visibility
    private String lastKnownPrefix = null;
    private static BoosterConfig config;
    private BoosterStatusWindow boosterStatusWindow;
    private boolean isHudVisible = true;  // HUD visibility state

    private static final Pattern TOKEN_BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        config = BoosterConfig.load();  // Load once during initialization
        boosterStatusWindow = new BoosterStatusWindow(config); // Initialize the instance
        HudRenderCallback.EVENT.register(boosterStatusWindow);

        // Initialize other events, listeners, etc.
        registerMessageListeners();
        registerMouseEvents();
        registerLogoutEvent();
        registerKeyBindings();
        registerJoinEvent();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().currentScreen == null) {
                boosterStatusWindow.handleScreenClose(); // Use instance method
            }
        });
    }

    private void registerJoinEvent() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Load config and start fetching after joining
            if (config == null) {
                config = BoosterConfig.load();
            }

            // Start fetching display name after joining
            startFetchingDisplayName();

            // Ensure HUD visibility matches user setting
            boosterStatusWindow.setHudVisible(isHudVisible);
        });
    }

    // Method to start fetching the display name with a fixed rate
    private void startFetchingDisplayName() {
        scheduler.scheduleAtFixedRate(this::fetchAndLogPlayerPrefix, config.initialFetchDelaySeconds, config.fetchIntervalSeconds, TimeUnit.SECONDS);
    }

    private void fetchAndLogPlayerPrefix() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Get the player's UUID and GameProfile
            GameProfile playerProfile = client.player.getGameProfile();

            if (client.getNetworkHandler() == null || client.getNetworkHandler().getPlayerList().isEmpty()) {
                return;
            }

            for (PlayerListEntry entry : Objects.requireNonNull(client.getNetworkHandler()).getPlayerList()) {
                if (entry.getProfile().getId().equals(playerProfile.getId())) {
                    // Check if there is any team prefix (some servers use teams to set prefixes)
                    if (entry.getScoreboardTeam() != null) {
                        String teamPrefix = entry.getScoreboardTeam().getPrefix().getString().trim();  // Trim to remove any surrounding whitespace

                        // Update only if there is a change in the prefix
                        if (!teamPrefix.equals(lastKnownPrefix)) {
                            lastKnownPrefix = teamPrefix;
                            SellBoostCalculator.setRank(teamPrefix);
                        }
                    } else {
                        // Handle cases where the player is not in any team
                        if (lastKnownPrefix != null) {
                            lastKnownPrefix = null;
                        }
                    }

                    break; // Exit the loop once we find our player
                }
            }
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

    private void processChatMessage(String msg, boolean ignoredIsGameMessage) {
        if (msg.contains("Backpack Space")) {
            return;
        }

        Matcher matcher = TOKEN_BOOSTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            String multiplier = matcher.group(1);
            StringBuilder remaining = new StringBuilder();

            for (int i = 3; i <= 6; i++) {
                if (matcher.group(i) != null) remaining.append(matcher.group(i));
            }

            if (multiplier != null && (!remaining.isEmpty())) {
                multiplier = multiplier.trim();
                remaining = new StringBuilder(remaining.toString().replace("remaining", "").trim());

                boosterStatusWindow.setTokensBoosterActive(true, multiplier, remaining.toString());
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

                boosterStatusWindow.setRichBoosterActive(true, remaining.toString());
            }
        }
    }

    private void registerMouseEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
                double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    boosterStatusWindow.handleMousePress(mouseX, mouseY); // Use instance method
                } else {
                    boosterStatusWindow.handleMouseRelease(); // Use instance method
                }
            }
        });
    }

    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> boosterStatusWindow.clearBoosterInfo());
    }

    private void registerKeyBindings() {
        boosterKeyBinding = new KeyBinding(
                "key.boosternoti.booster",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.boosternoti.general"
        );

        toggleHudKeyBinding = new KeyBinding(  // Keybinding for toggling HUD visibility
                "key.boosternoti.toggleHud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.boosternoti.general"
        );

        KeyBindingHelper.registerKeyBinding(boosterKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleHudKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed()) {
                sendBoosterCommand();
                fetchAndLogPlayerPrefix();
            }

            if (toggleHudKeyBinding.wasPressed()) {
                toggleHudVisibility();  // Call toggle method when key is pressed
            }
        });
    }

    private void toggleHudVisibility() {
        isHudVisible = !isHudVisible;  // Toggle the visibility state
        boosterStatusWindow.setHudVisible(isHudVisible);
    }

    private void sendBoosterCommand() {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("booster");
        }
    }
}