package net.holm.boosternoti;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IgnoreManager {
    private static final Set<UUID> ignoredPlayers = new HashSet<>();

    // Add a player to the ignored list
    public static void addIgnoredPlayer(UUID playerUUID) {
        ignoredPlayers.add(playerUUID);
    }

    // Remove a player from the ignored list
    public static void removeIgnoredPlayer(UUID playerUUID) {
        ignoredPlayers.remove(playerUUID);
    }

    // Check if a player is ignored
    public static boolean isPlayerIgnored(UUID playerUUID) {
        return ignoredPlayers.contains(playerUUID);
    }

    // Get all ignored players
    public static Set<UUID> getIgnoredPlayers() {
        return new HashSet<>(ignoredPlayers);
    }

    public static void Commands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            register(dispatcher);  // Register IgnoreCommand commands
        });
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Register /ignore command
        dispatcher.register(ClientCommandManager.literal("ignore")
                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> suggestOnlinePlayers(builder))  // Suggest player names
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (client.getNetworkHandler() != null) {
                                PlayerListEntry playerToIgnore = client.getNetworkHandler().getPlayerList().stream()
                                        .filter(player -> player.getProfile().getName().equals(playerName))
                                        .findFirst()
                                        .orElse(null);

                                if (playerToIgnore != null) {
                                    UUID playerUUID = playerToIgnore.getProfile().getId();  // Get the UUID from PlayerListEntry

                                    // Print debug information to check correctness
                                    System.out.println("DEBUG: Found player - Name: " + playerToIgnore.getProfile().getName() + ", UUID: " + playerUUID);
                                    context.getSource().sendFeedback(Text.literal("DEBUG: Found player - Name: " + playerToIgnore.getProfile().getName() + ", UUID: " + playerUUID));

                                    IgnoreManager.addIgnoredPlayer(playerUUID);  // Add to ignore list

                                    // Print both player name and UUID in chat
                                    context.getSource().sendFeedback(
                                            Text.literal(playerName + " has been ignored. UUID: " + playerUUID.toString())
                                    );
                                } else {
                                    context.getSource().sendError(Text.literal("Player not found."));
                                }
                            } else {
                                context.getSource().sendError(Text.literal("Network handler is not available."));
                            }
                            return 1;
                        })
                )
        );

        // Register /unignore command
        dispatcher.register(ClientCommandManager.literal("unignore")
                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> suggestOnlinePlayers(builder))  // Suggest player names
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (client.getNetworkHandler() != null) {
                                PlayerListEntry playerToUnignore = client.getNetworkHandler().getPlayerList().stream()
                                        .filter(player -> player.getProfile().getName().equals(playerName))
                                        .findFirst()
                                        .orElse(null);

                                if (playerToUnignore != null) {
                                    IgnoreManager.removeIgnoredPlayer(playerToUnignore.getProfile().getId());
                                    context.getSource().sendFeedback(Text.literal(playerName + " is no longer ignored."));
                                } else {
                                    context.getSource().sendError(Text.literal("Player not found."));
                                }
                            } else {
                                context.getSource().sendError(Text.literal("Network handler is not available."));
                            }
                            return 1;
                        })
                )
        );
    }

    // Helper method to suggest online player names
    private static CompletableFuture<Suggestions> suggestOnlinePlayers(SuggestionsBuilder builder) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() != null) {
            // Use network handler for multiplayer mode to get the list of online players
            client.getNetworkHandler().getPlayerList().stream()
                    .map(player -> player.getProfile().getName())  // Corrected to get the player's profile name
                    .filter(playerName -> playerName.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                    .forEach(builder::suggest);  // Suggest matching player names
        }

        return builder.buildFuture();  // Return the CompletableFuture
    }
}
