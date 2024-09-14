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
import java.util.HashMap;
import java.util.Map;
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
    private static KeyBinding showPlayerListKeyBinding; // Keybind for player list
    private static KeyBinding toggleInstructionsKeyBinding;
    private static BoosterConfig config;
    private static BoosterStatusWindow boosterStatusWindow;
    private static CustomPlayerList customPlayerList;  // Custom player list instance
    private static boolean isHudVisible = true;
    private static boolean showPlayerList = false;  // Flag for toggling player list visibility
    private static SaleSummaryManager saleSummaryManager;

    private static final Pattern TOKEN_BOOSTER_PATTERN = Pattern.compile(
            "\\s-\\sTokens\\s\\((\\d+(\\.\\d+)?)x\\)\\s\\((\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s\\s)?remaining\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RICH_BOOSTER_PATTERN = Pattern.compile("iBlocky → Your Rich pet has rewarded you with a 2x sell booster for the next (\\d+d\\s)?(\\d+h\\s)?(\\d+m\\s)?(\\d+s)?!");
    private static final Pattern PURCHASED_LEVELS_PATTERN = Pattern.compile("Purchased (\\d+) levels of ([A-Za-z ]+)");
    private static final Pattern LEVELED_UP_PATTERN = Pattern.compile("You leveled up ([A-Za-z ]+) to level (\\d+) for ([\\d,.]+) tokens!");
    private boolean hudInitialized = false; // Add a flag to check if the HUD has already been initialized

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long BOOSTER_COMMAND_INTERVAL = 20 * 60; // 20 minutes in seconds
    private final ScheduledExecutorService boosterScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> boosterTask;

    private static iBlockyBoosterNotificationClient instance;
    public static BoosterStatusWindow getBoosterStatusWindow() {
        return boosterStatusWindow;
    }
    private static final Map<String, String> availableEnchants = new HashMap<>();

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

        boosterStatusWindow = new BoosterStatusWindow(config, boosterKeyBinding, toggleHudKeyBinding, showPlayerListKeyBinding, toggleInstructionsKeyBinding);
        customPlayerList = new CustomPlayerList();

        registerJoinEvent();

        // Register the HUD rendering after key bindings are set
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (showPlayerList) {  // Only render if the player list toggle is on
                customPlayerList.renderPlayerList(drawContext);  // Pass the drawContext here
            }
        });
    }

    public void registerJoinEvent() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Load config if not already loaded
            if (config == null) {
                config = BoosterConfig.load();
            }

            // Check if connected to the correct server
            if (isCorrectServer()) {
                System.out.println("Connected to the correct server, initializing mod.");

                // Only initialize HUD and related components if not already initialized
                if (!hudInitialized) {
                    System.out.println("HUD is being initialized for the first time.");
                    hudInitialized = true; // Set flag to prevent further initializations

                    // Initialize key mod components only once per session
                    startFetchingDisplayName();
                    setHudVisible(true); // Set HUD visible only on the correct server

                    saleSummaryManager = new SaleSummaryManager();
                    BackpackSpaceTracker.init();
                    HudRenderCallback.EVENT.register(boosterStatusWindow);
                    registerMessageListeners();
                    registerMouseEvents();
                    registerLogoutEvent(); // Ensure logout event is registered properly
                    registerBoosterScheduler();
                    HubCommand.register();

                    // Register commands
                    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> RankSetCommand.register(dispatcher));
                    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SaleSummaryCommand.register(dispatcher));

                    // Handle closing of the booster status window
                    ClientTickEvents.END_CLIENT_TICK.register(client1 -> {
                        if (MinecraftClient.getInstance().currentScreen == null) {
                            boosterStatusWindow.handleScreenClose();
                        }
                    });
                } else {
                    System.out.println("HUD is already initialized, skipping re-initialization.");
                }
            } else {
                // If the server is not correct, ensure the HUD and player list are hidden
                System.out.println("Mod is not initialized: Connected to a non-supported server.");
                setHudVisible(false);  // Hide the HUD if connected to a non-supported server
                showPlayerList = false; // Ensure player list is hidden
            }
        });
    }


    private void registerLogoutEvent() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear booster info and hide HUD when disconnected
            boosterStatusWindow.clearBoosterInfo();
            setHudVisible(false);  // Hide the HUD upon disconnecting
            showPlayerList = false; // Reset player list visibility as well
            System.out.println("Disconnected from server. HUD and player list are now hidden.");
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

    public void fetchAndUpdateBalance() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("balance");
        }
    }

    void startFetchingDisplayName() {
        scheduler.scheduleAtFixedRate(this::fetchAndLogPlayerPrefix, config.initialFetchDelaySeconds, config.fetchIntervalSeconds, TimeUnit.SECONDS);
    }

    public static iBlockyBoosterNotificationClient getInstance() {
        return instance;
    }

    void fetchAndLogPlayerPrefix() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!isCorrectServer()) {
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

    private void processChatMessage(String msg, boolean ignoredIsGameMessage) {
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
                SaleSummaryManager saleSummaryManager = iBlockyBoosterNotificationClient.getSaleSummaryManager();
                saleSummaryManager.addSale(tokens);
                boosterStatusWindow.setTotalSales(saleSummaryManager.getTotalSales());
                fetchAndUpdateBalance();
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse token value from message: " + totalString);
            }
        }

        // Purchased levels of an enchant detection
        Matcher purchaseMatcher = PURCHASED_LEVELS_PATTERN.matcher(msg);
        if (purchaseMatcher.find()) {
            String amount = purchaseMatcher.group(1);
            String enchantName = purchaseMatcher.group(2).trim();

            if (availableEnchants.containsKey(enchantName)) {
                fetchAndUpdateBalance();
            }
        }

        // Leveled up enchant detection
        Matcher levelUpMatcher = LEVELED_UP_PATTERN.matcher(msg);
        if (levelUpMatcher.find()) {
            String enchantName = levelUpMatcher.group(1).trim();
            String amount = levelUpMatcher.group(2).trim();
            fetchAndUpdateBalance();
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

    private void registerKeyBindings() {
        boosterKeyBinding = new KeyBinding("key.boosternoti.booster", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.boosternoti.general");
        toggleHudKeyBinding = new KeyBinding("key.boosternoti.toggleHud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.boosternoti.general");
        showPlayerListKeyBinding = new KeyBinding("key.boosternoti.showPlayerList", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.boosternoti.general");
        toggleInstructionsKeyBinding = new KeyBinding("key.boosternoti.toggleInstructions", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.boosternoti.general");

        KeyBindingHelper.registerKeyBinding(boosterKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleHudKeyBinding);
        KeyBindingHelper.registerKeyBinding(showPlayerListKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleInstructionsKeyBinding);

        // Handle key events during the client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (boosterKeyBinding.wasPressed() && isCorrectServer()) {
                sendBoosterCommand();
                fetchAndLogPlayerPrefix();
                fetchAndUpdateBalance();
            }

            if (toggleHudKeyBinding.wasPressed() && isCorrectServer()) {
                toggleHudVisibility(!isHudVisible);
                fetchAndUpdateBalance();
            }

            if (showPlayerListKeyBinding.wasPressed() && isCorrectServer()) {
                showPlayerList = !showPlayerList;
                customPlayerList.refreshPlayerList();
            }

            if (toggleInstructionsKeyBinding.wasPressed()) {
                boosterStatusWindow.toggleInstructions(); // Trigger instructions toggle
            }
        });
    }



    public static void toggleHudVisibility(boolean visible) {
        isHudVisible = visible;
        setHudVisible(isHudVisible);
    }

    public static void setHudVisible(boolean visible) {
        isHudVisible = visible;
    }

    public static boolean isHudVisible() {
        return isHudVisible;
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
