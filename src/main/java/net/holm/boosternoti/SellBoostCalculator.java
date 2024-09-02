package net.holm.boosternoti;

import java.util.HashMap;
import java.util.Map;

public class SellBoostCalculator {
    private static double rankBoost = 1.0;
    private static double tokenBoost = 1.0;
    private static double richPetBoost = 1.0;

    private static final Map<String, Double> rankBoostMap = new HashMap<>();

    static {
        rankBoostMap.put("ᴘʀɪꜱᴏɴᴇʀ", 1.00);
        rankBoostMap.put("ᴅᴇᴀʟᴇʀ", 1.05);
        rankBoostMap.put("ᴄʀɪᴍɪɴᴀʟ", 1.10);
        rankBoostMap.put("ʜɪᴛᴍᴀɴ", 1.15);
        rankBoostMap.put("ᴛʜᴜɢ", 1.20);
        rankBoostMap.put("ɢᴀɴɢꜱᴛᴇʀ", 1.30);
        rankBoostMap.put("ᴋɪɴɢᴘɪɴ", 1.50);
        rankBoostMap.put("ɪʙʟᴏᴄᴋʏ", 1.70);
    }

    public static double getRankBoost(String rank) {
        return rankBoostMap.getOrDefault(rank, 1.00);
    }

    public static void setRank(String rank) {
        rankBoost = rankBoostMap.getOrDefault(rank, 1.0);
        System.out.println("[DEBUG] Rank set to: " + rank + " with boost: " + rankBoost);
        updateSellBoost();
    }

    public static void setTokenBoost(double boost) {
        tokenBoost = boost;
        System.out.println("[DEBUG] Token boost set to: " + tokenBoost);
        updateSellBoost();
    }

    public static void setRichPetBoost(double boost) {
        richPetBoost = boost;
        System.out.println("[DEBUG] Rich pet boost set to: " + richPetBoost);
        updateSellBoost();
    }

    private static void updateSellBoost() {
        double totalSellBoost = rankBoost + (tokenBoost - 1.0) + (richPetBoost - 1.0);
        System.out.println("[DEBUG] Calculating total sell boost:");
        System.out.println("[DEBUG] Rank boost: " + rankBoost);
        System.out.println("[DEBUG] Token boost: " + tokenBoost);
        System.out.println("[DEBUG] Rich pet boost: " + richPetBoost);
        System.out.println("[DEBUG] Total sell boost: " + totalSellBoost);

        BoosterStatusWindow.updateSellBoostDisplay(totalSellBoost);
    }

    public static void resetBoosts() {
        tokenBoost = 1.0;
        richPetBoost = 1.0;
        System.out.println("[DEBUG] Boosts reset. Token boost: " + tokenBoost + ", Rich pet boost: " + richPetBoost);
        updateSellBoost();
    }
}
