package net.holm.boosternoti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.text.Text;
import net.minecraft.client.item.TooltipType;
import java.util.List;

public class PickaxeDataFetcher {

    public static void readPickaxeComponentData() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Check if the client or the player is null to avoid NullPointerException
        if (client == null || client.player == null) {
            System.out.println("Client or player is not available.");
            return;
        }

        // Get the item in slot 1 (index 0) of the hot bar
        ItemStack itemInSlot1 = client.player.getInventory().getStack(0);

        // Check if the item is a pickaxe
        if (itemInSlot1.getItem() instanceof PickaxeItem) {
            System.out.println("Pickaxe found in slot 1, fetching component data...");

            // Extract tooltip information with null context and player, and advanced type
            List<Text> tooltip = itemInSlot1.getTooltip(null, null, TooltipType.ADVANCED);
            for (Text text : tooltip) {
                String componentText = text.getString();
                System.out.println("Component: " + componentText);
            }

        } else {
            System.out.println("No pickaxe found in slot 1.");
        }
    }
}
