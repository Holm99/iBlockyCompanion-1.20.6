package net.holm.iblockycompanion;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainClient.class);
    private static KeyBinding boosterKeyBinding;
    private static KeyBinding toggleBoosterHudKeyBinding;
    private static KeyBinding toggleEnchantHudKeyBinding;
    private static KeyBinding showPlayerListKeyBinding; // Keybind for player list
    private static KeyBinding toggleInstructionsKeyBinding;
    private static KeyBinding enchantLeftKeyBinding; // New key bindings for EnchantHUD
    private static KeyBinding enchantRightKeyBinding;
    private static BoosterConfig config;
    static BoosterStatusWindow boosterStatusWindow;
    private static EnchantHUD enchantHUD;
    private static boolean isHudVisible = true;
    private boolean hudInitialized = false;
    private static CustomPlayerList customPlayerList;  // Custom player list instance
    private static boolean showPlayerList = false;  // Flag for toggling player list visibility
    private static SaleSummaryManager saleSummaryManager;
    private boolean gameModeChecked = false;

    private static final Pattern TOKEN_BOOSTER_PATTERN = Pattern.compile(
            "\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");
    private static final Pattern PRESTIGED_ENCHANT_PATTERN = Pattern.compile("You prestiged ([A-Za-z ]+) to (\\d+)");
    private static final Pattern PURCHASED_LEVELS_PATTERN = Pattern.compile("Purchased (\\d+) levels of ([A-Za-z ]+)");
    private static final Pattern LEVELED_UP_PATTERN = Pattern.compile("You leveled up ([A-Za-z ]+) to level (\\d+) for ([\\d,.]+) tokens!");

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long BOOSTER_COMMAND_INTERVAL = 20 * 60; // 20 minutes in seconds
    private final ScheduledExecutorService boosterScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> boosterTask;
    private final ScheduledExecutorService refreshScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> refreshTask;

    private static MainClient instance;

    static final Map<String, String> availableEnchants = new HashMap<>();
    static {
        availableEnchants.put("Locksmith", "Locksmith");
        availableEnchants.put("Jurassic", "Jurassic");
        availableEnchants.put("Terminator", "Terminator");
        availableEnchants.put("Efficiency", "Efficiency");
        availableEnchants.put("Explosive", "Explosive");
        availableEnchants.put("Greed", "Greed");
        availableEnchants.put("Drill", "Drill");
        availableEnchants.put("Profit", "Profit");
        availableEnchants.put("Multiplier", "Multiplier");
        availableEnchants.put("Spelunker", "Spelunker");
        availableEnchants.put("Spirit", "Spirit");
        availableEnchants.put("Vein Miner", "Vein Miner");
        availableEnchants.put("Cubed", "Cubed");
        availableEnchants.put("Jackhammer", "Jackhammer");
        availableEnchants.put("Stellar Sight", "Stellar Sight");
        availableEnchants.put("Speed", "Speed");
        availableEnchants.put("Starstruck", "Starstruck");
        availableEnchants.put("Blackhole", "Blackhole");
        availableEnchants.put("Lucky", "Lucky");
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        config = BoosterConfig.load();

        // Ensure key bindings are registered early
        registerKeyBindings();

        boosterStatusWindow = new BoosterStatusWindow(config, boosterKeyBinding, toggleBoosterHudKeyBinding, toggleEnchantHudKeyBinding, showPlayerListKeyBinding, toggleInstructionsKeyBinding);
        try {
            enchantHUD = new EnchantHUD(config); // Initialize the EnchantHUD here
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
        customPlayerList = new CustomPlayerList();

        registerJoinEvent();

        // Register the HUD rendering after key bindings are set
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (showPlayerList) {
                customPlayerList.renderPlayerList(drawContext);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            RankSetCommand.register(dispatcher);
            SaleSummaryCommand.register(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only proceed if the player is on the correct server
            if (isCorrectServer() && !gameModeChecked) {
                customPlayerList.refreshPlayerList();
                customPlayerList.detectGameMode();

                if (isCorrectGameMode()) {
                    if (!hudInitialized) {
                        initializeHUD();
                    }
                } else {
                    if (hudInitialized) {
                        resetHUD();
                    }
                }

                gameModeChecked = true;
            }
        });
    }

    public void registerJoinEvent() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (config == null) {
                config = BoosterConfig.load();
            }

            scheduler.schedule(() -> {
                if (isCorrectServer()) {
                    HubCommand.register();
                    customPlayerList.refreshPlayerList();
                    customPlayerList.detectGameMode();

                    if (isCorrectGameMode()) {
                        if (!hudInitialized) {
                            initializeHUD();
                        } else {
                            boosterStatusWindow.setHudVisible(true);  // Show booster window
                            enchantHUD.setHudVisible(true);  // Show enchant HUD
                            fetchCSVDataAndUpdateHUD();
                            PickaxeDataFetcher.readPickaxeComponentData();
                            enchantHUD.updateEnchantNames();
                        }
                    } else {
                        if (hudInitialized) {
                            boosterStatusWindow.setHudVisible(false); // Hide booster window
                            enchantHUD.setHudVisible(false);  // Hide enchant HUD
                        }
                    }
                    gameModeChecked = true;
                } else {
                    boosterStatusWindow.setHudVisible(false); // Hide booster window
                    enchantHUD.setHudVisible(false);  // Hide enchant HUD
                }
            }, 2, TimeUnit.SECONDS);
        });
    }

    private void initializeHUD() {
        if (!hudInitialized) {
            hudInitialized = true;
            boosterStatusWindow.setHudVisible(true);
            enchantHUD.setHudVisible(true);

            HudRenderCallback.EVENT.register(boosterStatusWindow);
            HudRenderCallback.EVENT.register(enchantHUD);

            PickaxeDataFetcher.readPickaxeComponentData();
            enchantHUD.updateEnchantNames();
            saleSummaryManager = new SaleSummaryManager();
            BackpackSpaceTracker.init();
            registerMessageListeners();
            registerMouseEvents();
            registerLogoutEvent();
            registerBoosterScheduler();

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (MinecraftClient.getInstance().currentScreen == null) {
                    if (boosterStatusWindow != null) {
                        boosterStatusWindow.handleScreenClose();
                    }
                    if (enchantHUD != null) {
                        enchantHUD.handleScreenClose();  // Assuming this method exists or you can implement it
                    }
                }
            });
        }

        fetchCSVDataAndUpdateHUD();
    }

    private void fetchCSVDataAndUpdateHUD() {
        CSVFetcher.fetchCSVData(); // Fetch the CSV data
        enchantHUD.updateEnchantNames(); // Update the enchant names in the HUD
    }

    private void resetHUD() {
        if (boosterStatusWindow != null) {
            boosterStatusWindow.setHudVisible(false);
        }
        if (enchantHUD != null) {
            enchantHUD.setHudVisible(false);
        }
        showPlayerList = false;
    }

    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (boosterStatusWindow != null) {
                boosterStatusWindow.clearBoosterInfo();
                boosterStatusWindow.setHudVisible(false);
            }
            if (enchantHUD != null) {
                enchantHUD.setHudVisible(false);
            }
            showPlayerList = false;
        });
    }

    public static SaleSummaryManager getSaleSummaryManager() {
        return saleSummaryManager;
    }

    private void registerBoosterScheduler() {
        if (boosterTask != null && !boosterTask.isCancelled()) {
            boosterTask.cancel(false);
        }

        boosterTask = boosterScheduler.scheduleAtFixedRate(() -> {
            if (isHudVisible) {
                sendBoosterCommand();
                fetchAndUpdateBalance();
                fetchAndLogPlayerPrefix();
            }
        }, 0, BOOSTER_COMMAND_INTERVAL, TimeUnit.SECONDS);
    }

    static boolean isCorrectServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;
            return "play.iblocky.net".equalsIgnoreCase(serverAddress) || "mc.iblocky.net".equalsIgnoreCase(serverAddress);
        }
        return false;
    }

    public boolean isCorrectGameMode() {
        return "Prison".equals(customPlayerList.getCurrentGameMode());
    }

    public void fetchAndUpdateBalance() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("balance");
        }
    }

    void startFetchingDisplayName() {
        scheduler.scheduleAtFixedRate(this::fetchAndLogPlayerPrefix, config.initialFetchDelaySeconds, config.fetchIntervalSeconds, TimeUnit.SECONDS);
    }

    public static MainClient getInstance() {
        return instance;
    }

    void fetchAndLogPlayerPrefix() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!isCorrectServer() && isCorrectGameMode()) {
            return;
        }

        if (client.player != null) {
            GameProfile playerProfile = client.player.getGameProfile();
            UUID playerUUID = playerProfile.getId();

            BoosterConfig config = BoosterConfig.load();
            String manualRank = config.getManualRank(playerUUID);

            if (manualRank != null) {
                SellBoostCalculator.setRank(manualRank);
                return;
            }

            if (client.getNetworkHandler() == null || client.getNetworkHandler().getPlayerList().isEmpty()) {
                return;
            }

            for (PlayerListEntry entry : Objects.requireNonNull(client.getNetworkHandler()).getPlayerList()) {
                if (entry.getProfile().getId().equals(playerProfile.getId())) {
                    if (entry.getScoreboardTeam() != null) {
                        String teamPrefix = entry.getScoreboardTeam().getPrefix().getString().trim();
                        SellBoostCalculator.setRank(teamPrefix);
                    }
                    break;
                }
            }
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

    private String extractSenderFromMessage(String msg) {
        // Example chat format: "[PlayerName]: message"
        // Adjust this logic based on your server's chat message structure
        if (msg.contains(":")) {
            // Extract the part before the colon as the player name
            return msg.substring(0, msg.indexOf(":")).trim();
        }

        // Return null if the format doesn't match
        return null;
    }

    void processChatMessage(String msg, boolean ignoredIsGameMessage) {
        MinecraftClient client = MinecraftClient.getInstance();

        // The rest of your message processing code follows...
        if (msg.contains("§6§lBackpack Space §f§l→")) {
            return;
        }

        // Token booster detection
        Matcher matcher = TOKEN_BOOSTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            String multiplier = matcher.group(1);
            StringBuilder remaining = new StringBuilder();
            for (int i = 3; i <= 6; i++) {
                if (matcher.group(i) != null) {
                    remaining.append(matcher.group(i));
                }
            }
            if (!remaining.isEmpty()) {
                boosterStatusWindow.setTokensBoosterActive(true, multiplier.trim(), remaining.toString().replace("remaining", "").trim());
            }
        }

        // Rich booster detection
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

        // Token balance detection
        if (msg.startsWith("Token Balance:")) {
            String balanceString = msg.substring("Token Balance:".length()).replace(",", "").trim();
            try {
                double balance = Double.parseDouble(balanceString);
                boosterStatusWindow.setTokenBalance(balance);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse token balance: " + balanceString);
            }
        }

        // Sale summary detection
        if (msg.startsWith("§f§l(!) §e§lSALE §6§lSUMMARY")) {
            int startIndex = msg.indexOf("§fTotal: §6") + "§fTotal: §6".length();
            int endIndex = msg.indexOf("Tokens", startIndex);
            if (startIndex < "§fTotal: §6".length() || endIndex == -1) {
                System.err.println("Failed to locate total tokens in the message.");
                return;
            }
            String totalString = msg.substring(startIndex, endIndex).replace("§6", "").replace(",", "").trim();

            try {
                long tokens = Long.parseLong(totalString);
                SaleSummaryManager saleSummaryManager = MainClient.getSaleSummaryManager();
                saleSummaryManager.addSale(tokens);
                boosterStatusWindow.setTotalSales(saleSummaryManager.getTotalSales());
                fetchAndUpdateBalance();
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse token value from message: " + totalString);
            }
        }

        Matcher purchaseMatcher = PURCHASED_LEVELS_PATTERN.matcher(msg);
        Matcher prestigeMatcher = PRESTIGED_ENCHANT_PATTERN.matcher(msg);
        Matcher levelUpMatcher = LEVELED_UP_PATTERN.matcher(msg);

        String enchantName = null;

        if (purchaseMatcher.find()) {
            enchantName = purchaseMatcher.group(2).trim();
        } else if (prestigeMatcher.find()) {
            enchantName = prestigeMatcher.group(1).trim();
        } else if (levelUpMatcher.find()) {
            enchantName = levelUpMatcher.group(1).trim();
        }

        if (enchantName != null && availableEnchants.containsKey(enchantName)) {
            fetchAndUpdateBalance();

            // Delay before fetching pickaxe data to allow the server to process the upgrade
            MinecraftClient.getInstance().execute(() -> {
                scheduler.schedule(() -> {
                    // Clear and repopulate the cache with new enchantment data and costs
                    enchantHUD.clearAndRepopulateCache(
                            PickaxeDataFetcher.enchantPrestigeLevels,
                            CSVFetcher.getEnchantCostsCache()
                    );

                    // Fetch new pickaxe component data and refresh the enchantHUD
                    PickaxeDataFetcher.readPickaxeComponentData();
                    fetchCSVDataAndUpdateHUD();

                    // Ensure the HUD's selected enchant is still valid
                    enchantHUD.updateEnchantNames();
                }, 5, TimeUnit.SECONDS); // Adjust the delay time if necessary
            });
        }
    }


    private void registerMouseEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().currentScreen != null) {
                double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
                double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    boosterStatusWindow.handleMousePress(mouseX, mouseY);
                    enchantHUD.handleMousePress(mouseX, mouseY);
                } else if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_RELEASE) {
                    boosterStatusWindow.handleMouseRelease();
                    enchantHUD.handleMouseRelease();
                }

                boosterStatusWindow.onMouseMove(mouseX, mouseY);
                enchantHUD.onMouseMove(mouseX, mouseY);
            }
        });
    }

    private void registerKeyBindings() {
        boosterKeyBinding = new KeyBinding("key.boosternoti.booster", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.boosternoti.general");
        toggleBoosterHudKeyBinding = new KeyBinding("key.boosternoti.toggleBoosterHud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.boosternoti.general");
        toggleEnchantHudKeyBinding = new KeyBinding("key.boosternoti.toggleEnchantHUD", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.boosternoti.general");
        showPlayerListKeyBinding = new KeyBinding("key.boosternoti.showPlayerList", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.boosternoti.general");
        toggleInstructionsKeyBinding = new KeyBinding("key.boosternoti.toggleInstructions", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.boosternoti.general");
        enchantLeftKeyBinding = new KeyBinding("key.enchant_hud.left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, "category.enchant_hud");
        enchantRightKeyBinding = new KeyBinding("key.enchant_hud.right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "category.enchant_hud");

        KeyBindingHelper.registerKeyBinding(boosterKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleBoosterHudKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleEnchantHudKeyBinding);
        KeyBindingHelper.registerKeyBinding(showPlayerListKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleInstructionsKeyBinding);
        KeyBindingHelper.registerKeyBinding(enchantLeftKeyBinding);
        KeyBindingHelper.registerKeyBinding(enchantRightKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                sendBoosterCommand();
                fetchAndLogPlayerPrefix();
                fetchAndUpdateBalance();
            }

            if (toggleBoosterHudKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                toggleBoosterVisibility();
                fetchAndUpdateBalance();
            }

            if (toggleEnchantHudKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                toggleEnchantVisibility();
            }

            if (showPlayerListKeyBinding.isPressed() && isCorrectServer()) {
                if (!showPlayerList) {
                    startRefreshingPlayerList();
                    showPlayerList = true;
                }
            } else {
                if (showPlayerList) {
                    stopRefreshingPlayerList();
                    showPlayerList = false;
                }
            }

            if (enchantLeftKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                enchantHUD.cycleLeft();
            }

            if (enchantRightKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                enchantHUD.cycleRight();
            }

            if (toggleInstructionsKeyBinding.wasPressed() && isCorrectServer() && isCorrectGameMode()) {
                boosterStatusWindow.toggleInstructions();
            }
        });
    }

    private void startRefreshingPlayerList() {
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = refreshScheduler.scheduleAtFixedRate(() -> {
                if (showPlayerList) {
                    customPlayerList.refreshPlayerList();

                    if (!customPlayerList.isGameModeDetected() || customPlayerList.isGameModeChanged()) {
                        customPlayerList.detectGameMode();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void stopRefreshingPlayerList() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel(false);
        }
    }

    // Method to toggle BoosterWindow visibility
    public static void toggleBoosterVisibility() {
        if (boosterStatusWindow != null) {
            boosterStatusWindow.setHudVisible(!boosterStatusWindow.isHudVisible());
        }
    }

    // Method to toggle EnchantHUD visibility
    public static void toggleEnchantVisibility() {
        if (enchantHUD != null) {
            enchantHUD.setHudVisible(!enchantHUD.isHudVisible());
        }
    }

    public static void setHudVisible(boolean visible) {
        isHudVisible = visible;

        // Null checks before attempting to set visibility
        if (boosterStatusWindow != null) {
            boosterStatusWindow.setHudVisible(visible);
        }
        if (enchantHUD != null) {
            enchantHUD.setHudVisible(visible);
        }
    }

    private void sendBoosterCommand() {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("booster");
        }
    }

    public static void startFetchingDisplayNameFromInstance() {
        if (instance != null) {
            instance.startFetchingDisplayName();
        }
    }
}