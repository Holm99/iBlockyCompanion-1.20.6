package net.holm.boosternoti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.text.Text;
import net.minecraft.client.item.TooltipType;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PickaxeDataFetcher {

    // Define a map to store the prestige levels for each enchantment
    public static final Map<String, Integer> enchantPrestigeLevels = new HashMap<>();

    // Regex pattern to identify "Prestige X"
    private static final Pattern PRESTIGE_PATTERN = Pattern.compile("\\(Prestige (\\d+)\\)");

    // Define enchants that cannot have prestige
    private static final List<String> nonPrestigeableEnchants = List.of(
            "Efficiency", "Locksmith", "Jurassic", "Speed"
    );

    // Static block to initialize the enchantPrestigeLevels with default values (0)
    static {
        for (String enchant : iBlockyBoosterNotificationClient.availableEnchants.keySet()) {
            enchantPrestigeLevels.put(enchant, 0); // Initialize each enchant with prestige level 0
        }
    }

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

                // Check if the component contains prestige level and process accordingly
                processComponentText(componentText);
            }

            // Output the extracted prestige levels
            for (Map.Entry<String, Integer> entry : enchantPrestigeLevels.entrySet()) {
                System.out.println(entry.getKey() + " Prestige Level: " + entry.getValue());
            }

        } else {
            System.out.println("No pickaxe found in slot 1.");
        }
    }

    // Function to process each component text and extract prestige levels
    private static void processComponentText(String componentText) {
        // Iterate over the available enchants from iBlockyBoosterNotificationClient class
        for (String enchant : iBlockyBoosterNotificationClient.availableEnchants.keySet()) {
            if (componentText.contains(enchant) && !nonPrestigeableEnchants.contains(enchant)) {
                // Extract the prestige level if it exists
                int prestigeLevel = extractPrestigeLevel(componentText);
                enchantPrestigeLevels.put(enchant, prestigeLevel);
            }
        }
    }

    public static Map<String, Integer> getEnchantPrestigeLevels() {
        return enchantPrestigeLevels;
    }

    // Function to extract the prestige level from the component text
    private static int extractPrestigeLevel(String componentText) {
        Matcher matcher = PRESTIGE_PATTERN.matcher(componentText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)); // Extract and return the prestige level
        }
        return 0; // Return 0 if no prestige level is found
    }

    // Getter method for retrieving the prestige levels of a specific enchant (optional)
    public static int getPrestigeLevelForEnchant(String enchant) {
        return enchantPrestigeLevels.getOrDefault(enchant, 0);
    }
}
