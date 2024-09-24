package net.holm.iblockycompanion;

import java.util.HashMap;
import java.util.Map;

public class SellBoostCalculator {
    private static double rankBoost = 1.0;       // Base multiplier (no rank boost)
    private static double tokenBoost = 0.0;      // Starts with no boost
    private static double richPetBoost = 0.0;    // Starts with no boost
    private static double totalSellBoost = 1.0;  // Initial total boost, with base multiplier included

    private static final Map<String, Double> rankBoostMap = new HashMap<>();
    private static final Map<String, String> aliasMap = new HashMap<>();  // Alias mapping

    static {
        // Rank multipliers as doubles for accurate calculations
        rankBoostMap.put("ᴘʀɪꜱᴏɴᴇʀ", 1.00);
        rankBoostMap.put("ᴅᴇᴀʟᴇʀ", 1.05);
        rankBoostMap.put("ᴄʀɪᴍɪɴᴀʟ", 1.10);
        rankBoostMap.put("ʜɪᴛᴍᴀɴ", 1.15);
        rankBoostMap.put("ᴛʜᴜɢ", 1.20);
        rankBoostMap.put("ɢᴀɴɢꜱᴛᴇʀ", 1.30);
        rankBoostMap.put("ᴋɪɴɢᴘɪɴ", 1.50);
        rankBoostMap.put("ɪʙʟᴏᴄᴋʏ", 1.70);
        rankBoostMap.put("THE 1%", 2.00);

        // Alias map: plain text rank -> formatted rank
        aliasMap.put("prisoner", "ᴘʀɪꜱᴏɴᴇʀ");
        aliasMap.put("dealer", "ᴅᴇᴀʟᴇʀ");
        aliasMap.put("criminal", "ᴄʀɪᴍɪɴᴀʟ");
        aliasMap.put("hitman", "ʜɪᴛᴍᴀɴ");
        aliasMap.put("thug", "ᴛʜᴜɢ");
        aliasMap.put("gangster", "ɢᴀɴɢꜱᴛᴇʀ");
        aliasMap.put("kingpin", "ᴋɪɴɢᴘɪɴ");
        aliasMap.put("iblocky", "ɪʙʟᴏᴄᴋʏ");
        aliasMap.put("the 1%", "THE 1%");
    }

    public static void setRank(String rank) {
        // First, check if the input rank is an alias
        String formattedRank = aliasMap.getOrDefault(rank.toLowerCase(), rank);

        // Then, check if the formatted rank exists in the rankBoostMap
        rankBoost = rankBoostMap.getOrDefault(formattedRank, 1.0);
        updateSellBoost();
    }

    public static void setTokenBoost(double multiplier) {
        tokenBoost = multiplier;
        updateSellBoost();
    }

    public static void setRichPetBoost(double multiplier) {
        richPetBoost = multiplier;
        updateSellBoost();
    }

    static void updateSellBoost() {
        totalSellBoost = rankBoost + tokenBoost + richPetBoost;
        BoosterStatusWindow.updateSellBoostDisplay(totalSellBoost);
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

    public static double getTotalSellBoost() {
        return totalSellBoost;
    }

    public static Map<String, Double> getRankBoostMap() {
        return rankBoostMap;
    }

    public static Map<String, String> getAliasMap() {
        return aliasMap;
    }
}
