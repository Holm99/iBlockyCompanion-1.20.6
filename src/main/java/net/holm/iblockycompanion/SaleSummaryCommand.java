package net.holm.iblockycompanion;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SaleSummaryCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("summaryclear")
                .executes(context -> {
                    // Check if the player is on the correct server and game mode
                    if (!MainClient.isCorrectServer() || !MainClient.getInstance().isCorrectGameMode()) {
                        assert MinecraftClient.getInstance().player != null;
                        MinecraftClient.getInstance().player.sendMessage(Text.of("This command is only available on the correct server and game mode."), false);
                        return 0;  // Command fails
                    }

                    // Clear the internal sales data
                    SaleSummaryManager saleSummaryManager = MainClient.getSaleSummaryManager();
                    saleSummaryManager.clearSales();

                    // Clear the HUD display
                    MainClient.boosterStatusWindow.clearTotalSales();  // Clear the total sales on the HUD

                    // Notify the player
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendMessage(Text.of("Sale summaries cleared!"), false);
                    return 1; // Command executed successfully
                })
        );
    }
}