package net.holm.boosternoti;

import java.util.HashMap;
import java.util.Map;

public class SellBoostCalculator {
    private static double rankBoost = 1.0;       // Base multiplier (no rank boost)
    private static double tokenBoost = 0.0;      // Starts with no boost
    private static double richPetBoost = 0.0;    // Starts with no boost
    private static double totalSellBoost = 1.0;  // Initial total boost, with base multiplier included

    private static final Map<String, Double> rankBoostMap = new HashMap<>();

    static {
        // Rank multipliers as doubles for accurate calculations
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ᴘʀɪꜱᴏɴᴇʀ", 1.00);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ᴅᴇᴀʟᴇʀ", 1.05);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ᴄʀɪᴍɪɴᴀʟ", 1.10);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ʜɪᴛᴍᴀɴ", 1.15);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ᴛʜᴜɢ", 1.20);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ɢᴀɴɢꜱᴛᴇʀ", 1.30);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ᴋɪɴɢᴘɪɴ", 1.50);
        //noinspection SpellCheckingInspection
        rankBoostMap.put("ɪʙʟᴏᴄᴋʏ", 1.70);
        rankBoostMap.put("THE 1%", 2.00);
    }

    public static void setRank(String rank) {
        // Check if the rank exists in the rankBoostMap
        // If found, set the rankBoost to the corresponding multiplier
        // If not found, handle the missing rank case without assigning a default
        // Reset to base multiplier
        rankBoost = rankBoostMap.getOrDefault(rank, 1.0);
        updateSellBoost();
    }

    public static void setTokenBoost(double multiplier) {
        // Update tokenBoost based on the new multiplier
        tokenBoost = multiplier;
        updateSellBoost();
    }

    public static void setRichPetBoost(double multiplier) {
        // Update richPetBoost based on the new multiplier
        richPetBoost = multiplier;
        updateSellBoost();
    }

    static void updateSellBoost() {
        // Calculate the total sell boost as the sum of all active boosts
        totalSellBoost = rankBoost + tokenBoost + richPetBoost; // Corrected formula to simply sum all multipliers
        BoosterStatusWindow.updateSellBoostDisplay(totalSellBoost); // No error now, because it's static
    }

    public static void resetBoosts() {
        tokenBoost = 0.0;
        richPetBoost = 0.0;
        updateSellBoost();
    }

    public static double getTokenBoost() {
        return tokenBoost;
    }

    public static double getRichPetBoost() {
        return richPetBoost;
    }

    // Add the missing method to get the total sell boost
    public static double getTotalSellBoost() {
        return totalSellBoost;
    }
}
