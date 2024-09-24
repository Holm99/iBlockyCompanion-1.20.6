package net.holm.iblockycompanion;

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
            return;
        }

        // Get the item in slot 1 (index 0) of the hot bar
        ItemStack itemInSlot1 = client.player.getInventory().getStack(0);

        // Clear the current prestige levels to avoid keeping stale data
        enchantPrestigeLevels.clear();  // Clear the map before reading new data

        // Check if the item is a pickaxe
        if (itemInSlot1.getItem() instanceof PickaxeItem) {

            // Extract tooltip information with null context and player, and advanced type
            List<Text> tooltip = itemInSlot1.getTooltip(null, null, TooltipType.ADVANCED);
            for (Text text : tooltip) {
                String componentText = text.getString();

                // Check if the component contains prestige level and process accordingly
                processComponentText(componentText);  // Process each component text
            }
        }
    }

    // Function to process each component text and extract prestige levels
    private static void processComponentText(String componentText) {
        // Example regex to match prestige levels in the component text
        Pattern prestigePattern = Pattern.compile("▐\\s([A-Za-z ]+)\\s(\\d+)(?:\\s\\(Prestige\\s(\\d+)\\))?");
        Matcher matcher = prestigePattern.matcher(componentText);

        if (matcher.find()) {
            String enchantName = matcher.group(1).trim(); // Enchant name
            int enchantLevel = Integer.parseInt(matcher.group(2).trim()); // Enchant level

            // Check if prestige level exists (some enchants may not have prestige)
            int prestigeLevel = 0;
            if (matcher.group(3) != null) {
                prestigeLevel = Integer.parseInt(matcher.group(3).trim());
            }

            // If the enchant level is 0, remove it from the prestige map
            if (enchantLevel == 0) {
                enchantPrestigeLevels.remove(enchantName);
            } else {
                // Otherwise, update the prestige level in the map
                enchantPrestigeLevels.put(enchantName, prestigeLevel);
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
