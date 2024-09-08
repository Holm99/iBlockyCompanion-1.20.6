package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private static KeyBinding boosterKeyBinding;
    private static KeyBinding toggleHudKeyBinding;
    private String lastKnownPrefix = null;
    private static BoosterConfig config;
    private BoosterStatusWindow boosterStatusWindow;
    private static boolean isHudVisible = true;

    private static final Pattern TOKEN_BOOSTER_PATTERN = Pattern.compile("\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final long BOOSTER_COMMAND_INTERVAL = 20 * 60; // 20 minutes in seconds
    private ScheduledExecutorService boosterScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> boosterTask;

    // Instance of the client to allow calling non-static methods
    private static iBlockyBoosterNotificationClient instance;

    @Override
    public void onInitializeClient() {
        config = BoosterConfig.load();
        boosterStatusWindow = new BoosterStatusWindow(config);
        HudRenderCallback.EVENT.register(boosterStatusWindow);

        BackpackSpaceTracker.init();
        registerMessageListeners();
        registerMouseEvents();
        registerLogoutEvent();
        registerKeyBindings();
        registerJoinEvent();
        registerBoosterScheduler();

        // Register the rankset command using ClientCommandRegistrationCallback
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            RankSetCommand.register(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().currentScreen == null) {
                boosterStatusWindow.handleScreenClose();
            }
        });

        if (isHudVisible && isCorrectServer()) {
            registerBoosterScheduler();
        }
    }

    private void registerBoosterScheduler() {
        if (boosterTask != null && !boosterTask.isCancelled()) {
            boosterTask.cancel(false);
        }

        boosterTask = boosterScheduler.scheduleAtFixedRate(() -> {
            if (isHudVisible && isCorrectServer()) {
                sendBoosterCommand();
            }
        }, 0, BOOSTER_COMMAND_INTERVAL, TimeUnit.SECONDS);
    }

    static boolean isCorrectServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;
            return "play.iblocky.net".equalsIgnoreCase(serverAddress);
        }
        return false;
    }

    private void registerJoinEvent() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (config == null) {
                config = BoosterConfig.load();
            }
            startFetchingDisplayName();
            setHudVisible(isHudVisible);
        });
    }

    // Non-static method to start fetching the display name periodically
    void startFetchingDisplayName() {
        scheduler.scheduleAtFixedRate(this::fetchAndLogPlayerPrefix, config.initialFetchDelaySeconds, config.fetchIntervalSeconds, TimeUnit.SECONDS);
    }

    public static iBlockyBoosterNotificationClient getInstance() {
        return instance;  // Add a method to get the instance
    }

    void fetchAndLogPlayerPrefix() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Check if the player is connected to the correct server
        if (!isCorrectServer()) {
            System.out.println("Player is not connected to the correct server, skipping rank fetch.");
            return;  // Exit the method if not on the correct server
        }

        if (client.player != null) {
            GameProfile playerProfile = client.player.getGameProfile();
            UUID playerUUID = playerProfile.getId();

            BoosterConfig config = BoosterConfig.load();
            String manualRank = config.getManualRank(playerUUID);

            // Log the start of the method
            System.out.println("Starting fetchAndLogPlayerPrefix for player: " + playerUUID);

            // Check if a manual rank is set
            if (manualRank != null) {
                System.out.println("Manual rank found for player: " + manualRank);
                SellBoostCalculator.setRank(manualRank);  // Use manually set rank
                return;  // Exit early if manual rank is set
            } else {
                System.out.println("No manual rank found for player. Fetching rank from server.");
            }

            // Fetch and apply the rank from the server if no manual rank is set
            if (client.getNetworkHandler() == null || client.getNetworkHandler().getPlayerList().isEmpty()) {
                System.out.println("Network handler is unavailable or player list is empty.");
                return;  // Exit if network handler is not available
            }

            // Force rank update after manual clear
            lastKnownPrefix = null; // Reset the last known prefix to ensure rank is updated

            // Iterate through the player list to fetch the prefix
            for (PlayerListEntry entry : Objects.requireNonNull(client.getNetworkHandler()).getPlayerList()) {
                if (entry.getProfile().getId().equals(playerProfile.getId())) {
                    if (entry.getScoreboardTeam() != null) {
                        String teamPrefix = entry.getScoreboardTeam().getPrefix().getString().trim();

                        // Log the fetched team prefix
                        System.out.println("Fetched team prefix: " + teamPrefix);

                        // Update regardless of whether the teamPrefix has changed
                        System.out.println("Updating rank to: " + teamPrefix);
                        SellBoostCalculator.setRank(teamPrefix);  // Apply the server rank boost here
                    } else if (lastKnownPrefix != null) {
                        lastKnownPrefix = null; // Reset the prefix if none is found
                        System.out.println("No team prefix found, resetting the prefix.");
                    }
                    break;  // Exit the loop once the rank is found
                }
            }
        } else {
            System.out.println("Player or client is null, skipping rank fetching.");
        }
    }

    private void registerMessageListeners() {
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString();
            processChatMessage(msg, false);
        });

        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            String msg = message.getString();
            processChatMessage(msg, true);
        });
    }

    private void processChatMessage(String msg, boolean ignoredIsGameMessage) {
        Matcher matcher = TOKEN_BOOSTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            String multiplier = matcher.group(1);
            StringBuilder remaining = new StringBuilder();
            for (int i = 3; i <= 6; i++) {
                if (matcher.group(i) != null) remaining.append(matcher.group(i));
            }

            if (multiplier != null && (!remaining.isEmpty())) {
                boosterStatusWindow.setTokensBoosterActive(true, multiplier.trim(), remaining.toString().replace("remaining", "").trim());
            }
        }

        Matcher richMatcher = RICH_BOOSTER_PATTERN.matcher(msg);
        if (richMatcher.find()) {
            StringBuilder remaining = new StringBuilder();
            for (int i = 1; i <= 4; i++) {
                if (richMatcher.group(i) != null) remaining.append(richMatcher.group(i));
            }

            if (!remaining.isEmpty()) {
                boosterStatusWindow.setRichBoosterActive(true, remaining.toString().trim());
            }
        }
    }

    private void registerMouseEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
                double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    boosterStatusWindow.handleMousePress(mouseX, mouseY);
                } else {
                    boosterStatusWindow.handleMouseRelease(mouseX, mouseY);
                }
            }
        });
    }

    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> boosterStatusWindow.clearBoosterInfo());
    }

    private void registerKeyBindings() {
        boosterKeyBinding = new KeyBinding("key.boosternoti.booster", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.boosternoti.general");
        toggleHudKeyBinding = new KeyBinding("key.boosternoti.toggleHud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.boosternoti.general");

        KeyBindingHelper.registerKeyBinding(boosterKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleHudKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed()) {
                sendBoosterCommand();
                fetchAndLogPlayerPrefix();
            }

            if (toggleHudKeyBinding.wasPressed()) {
                toggleHudVisibility(!isHudVisible);  // Toggle the HUD visibility
            }
        });
    }

    public static void toggleHudVisibility(boolean visible) {
        isHudVisible = visible;
        setHudVisible(isHudVisible);
    }

    public static void setHudVisible(boolean visible) {
        isHudVisible = visible;
        System.out.println("HUD visibility set to: " + visible);
    }

    public static boolean isHudVisible() {
        return isHudVisible;
    }

    private void sendBoosterCommand() {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("booster");
        }
    }

    // Public method to access the instance and call the non-static method
    public static void startFetchingDisplayNameFromInstance() {
        if (instance != null) {
            instance.startFetchingDisplayName();
        }
    }
}
