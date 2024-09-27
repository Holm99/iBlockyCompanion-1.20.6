package net.holm.iblockycompanion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class RankSetCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("rankset")
                // Base command execution without arguments
                .executes(context -> {
                    if (!isCommandAllowed()) {
                        return 0;
                    }
                    sendPlayerMessage("Usage: /rankset <rank> or /rankset clear");
                    return 1;  // Success, but just showing usage information
                })
                .then(literal("clear")  // Explicitly handle "clear" command
                        .executes(context -> {
                            if (!isCommandAllowed()) {
                                return 0;
                            }

                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client != null && client.player != null) {
                                UUID playerUUID = client.player.getUuid();

                                // Clear manual rank using ConfigMenu
                                ConfigMenu.clearManualRank(playerUUID);
                                sendPlayerMessage("Manual rank cleared. Fetching rank from server...");

                                // Immediately fetch and apply the rank from the server
                                MainClient.getInstance().fetchAndLogPlayerPrefix();

                                return 1;
                            }
                            return 0;
                        }))
                .then(argument("rank", StringArgumentType.greedyString())  // Handle "rank" argument
                        .suggests((context, builder) -> {
                            // Suggest ranks from the aliasMap, sorted alphabetically
                            Map<String, String> aliasMap = SellBoostCalculator.getAliasMap();
                            aliasMap.keySet().stream()
                                    .sorted()  // Sort alphabetically
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!isCommandAllowed()) {
                                return 0;
                            }

                            String rank = StringArgumentType.getString(context, "rank");
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client != null && client.player != null) {
                                UUID playerUUID = client.player.getUuid();

                                // Use the alias map to format the rank correctly
                                Map<String, String> aliasMap = SellBoostCalculator.getAliasMap();
                                Map<String, Double> rankBoostMap = SellBoostCalculator.getRankBoostMap();
                                String formattedRank = aliasMap.getOrDefault(rank.toLowerCase(), rank);

                                // Ensure we are only checking the formatted rank from the alias map
                                if (rankBoostMap.containsKey(formattedRank)) {
                                    // Set manual rank using ConfigMenu
                                    ConfigMenu.setManualRank(playerUUID, formattedRank);

                                    // Apply the rank
                                    SellBoostCalculator.setRank(formattedRank);
                                    sendPlayerMessage("Rank set to: " + formattedRank);
                                    return 1;  // Success
                                } else {
                                    sendPlayerMessage("Invalid rank: " + rank);
                                    return 0;  // Failure
                                }
                            }
                            return 0;
                        }))
        );
    }

    // Helper method to check if the command is allowed based on the server and game mode
    private static boolean isCommandAllowed() {
        if (!MainClient.isCorrectServer() || !MainClient.getInstance().isCorrectGameMode()) {
            sendPlayerMessage("This command is only available on the correct server and game mode.");
            return false;
        }
        return true;
    }

    // Helper method to send a message to the player
    private static void sendPlayerMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.of(message), false);
        }
    }
}
