package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HubCommand {

    private static boolean suppressMessages = false;
    private static int ticksToSuppress = 0;

    // Pattern for unknown command and kicked messages
    private static final Pattern UNKNOWN_COMMAND_PATTERN = Pattern.compile("Unknown command\\.");
    private static final Pattern KICKED_FROM_PRISON_PATTERN = Pattern.compile("You were kicked from Prison-\\d+: Kicked for spamming");
    private static final Pattern KICKED_FROM_HUB_PATTERN = Pattern.compile("You were kicked from Hub-\\d+: Kicked for spamming");
    private static final Pattern QUICKCOMMAND_PATTERN = Pattern.compile("Slow down, you're executing commands too quickly\\.");
    private static final Pattern KICKED_FROM_SURVIVALSPAWN_PATTERN = Pattern.compile("You were kicked from SurvivalSpawn-\\d+: Kicked for spamming");


    public static void register() {
        // Register the /hub command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("hub").executes(context -> {
                startSpammingServer();
                return 1;
            }));
        });

        // Register a tick event to stop suppressing after a certain time
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksToSuppress > 0) {
                ticksToSuppress--;
            } else {
                suppressMessages = false; // Stop suppressing messages
            }
        });

        // Register the message interception event to block "Unknown command" and "kicked" messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String msg = message.getString();

            // Block unknown command messages or kicked messages
            if (suppressMessages && (UNKNOWN_COMMAND_PATTERN.matcher(msg).find() ||
                    KICKED_FROM_PRISON_PATTERN.matcher(msg).find() || KICKED_FROM_HUB_PATTERN.matcher(msg).find() || KICKED_FROM_SURVIVALSPAWN_PATTERN.matcher(msg).find() || QUICKCOMMAND_PATTERN.matcher(msg).find())) {
                return false;  // Suppress these specific messages
            }
            return true; // Allow other messages to pass through
        });
    }

    private static void startSpammingServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            suppressMessages = true; // Start suppressing messages
            ticksToSuppress = 40;    // Suppress messages for 2 seconds (40 ticks)

            for (int i = 0; i < 20; i++) {  // Send a fake command 10 times
                client.player.networkHandler.sendChatCommand("asdfekugi");
            }
        }
    }
}