package net.holm.boosternoti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

public class CustomPlayerList {

    private static final Map<String, Integer> rankWeightMap = new HashMap<>();
    private static final Map<String, Formatting> rankColorMap = new HashMap<>();
    private static List<PlayerListEntry> sortedEntries = new ArrayList<>();
    private static long lastUpdateHash = -1;
    private static boolean needsSorting = true;

    static {
        // Rank weight map
        rankWeightMap.put("ᴀᴅᴍɪɴ", 14);
        rankWeightMap.put("ꜱᴇɴɪᴏʀ ᴍᴏᴅᴇʀᴀᴛᴏʀ", 13);
        rankWeightMap.put("ᴍᴏᴅᴇʀᴀᴛᴏʀ", 12);
        rankWeightMap.put("ᴛʀɪᴀʟ ᴍᴏᴅᴇʀᴀᴛᴏʀ", 11);
        rankWeightMap.put("THE 1%", 10);
        rankWeightMap.put("ɪʙʟᴏᴄᴋʏ", 9);
        rankWeightMap.put("ᴋɪɴɢᴘɪɴ", 8);
        rankWeightMap.put("ɢᴀɴɢꜱᴛᴇʀ", 7);
        rankWeightMap.put("ᴛʜᴜɢ", 6);
        rankWeightMap.put("ʜɪᴛᴍᴀɴ", 5);
        rankWeightMap.put("ᴄʀɪᴍɪɴᴀʟ", 4);
        rankWeightMap.put("ᴅᴇᴀʟᴇʀ", 3);
        rankWeightMap.put("ᴘʀɪꜱᴏɴᴇʀ", 2);

        // Rank color map
        rankColorMap.put("ᴀᴅᴍɪɴ", Formatting.DARK_RED);
        rankColorMap.put("ꜱᴇɴɪᴏʀ ᴍᴏᴅᴇʀᴀᴛᴏʀ", Formatting.BLUE);
        rankColorMap.put("ᴍᴏᴅᴇʀᴀᴛᴏʀ", Formatting.BLUE);
        rankColorMap.put("ᴛʀɪᴀʟ ᴍᴏᴅᴇʀᴀᴛᴏʀ", Formatting.BLUE);
        rankColorMap.put("THE 1%", Formatting.WHITE);
        rankColorMap.put("ɪʙʟᴏᴄᴋʏ", Formatting.GOLD);
        rankColorMap.put("ᴋɪɴɢᴘɪɴ", Formatting.RED);
        rankColorMap.put("ɢᴀɴɢꜱᴛᴇʀ", Formatting.AQUA);
        rankColorMap.put("ᴛʜᴜɢ", Formatting.GREEN);
        rankColorMap.put("ʜɪᴛᴍᴀɴ", Formatting.YELLOW);
        rankColorMap.put("ᴄʀɪᴍɪɴᴀʟ", Formatting.LIGHT_PURPLE);
        rankColorMap.put("ᴅᴇᴀʟᴇʀ", Formatting.YELLOW);
        rankColorMap.put("ᴘʀɪꜱᴏɴᴇʀ", Formatting.GRAY);
    }

    public void renderPlayerList(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // Get the player list and compute its hash to detect changes
        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        long currentHash = entries.hashCode();

        if (currentHash != lastUpdateHash) {
            needsSorting = true;  // If the list has changed, mark it for sorting
            lastUpdateHash = currentHash;
        }

        if (needsSorting) {
            sortedEntries = entries.stream()
                    .filter(entry -> hasValidRankPrefix(getPlayerDisplayName(entry)))  // Exclude names without valid prefixes
                    .sorted(Comparator.comparing(this::getPlayerRankWeight).reversed())
                    .collect(Collectors.toList());

            needsSorting = false;
        }

        // Define scaling factors (e.g., 0.8 for 80% of the original size)
        float scale = 1.0f;

        // Dynamic column scaling
        int maxPerColumn = 25;
        List<Integer> columnWidths = calculateColumnWidths(client, maxPerColumn);

        // Calculate the total width and height for centering
        int totalWidth = columnWidths.stream().mapToInt(Integer::intValue).sum();
        int totalHeight = Math.min(maxPerColumn, sortedEntries.size()) * (int) (12 * scale) + 40; // Extra space for headers

        // Calculate the starting X and Y position to center the list
        int centerX = (client.getWindow().getScaledWidth() - (int) (totalWidth * scale)) / 2;
        int centerY = (int) (client.getWindow().getScaledHeight() * 0.1);  // 10% padding from the top

        // Draw background box with transparency
        int backgroundColor = 0x88000000;  // Semi-transparent black
        drawContext.fill(centerX - 10, centerY - 10, centerX + totalWidth + 10, centerY + totalHeight + 10, backgroundColor);

        // Draw border around the background
        int borderColor = 0xFFFFD700;  // Gold border
        drawContext.drawBorder(centerX - 10, centerY - 10, totalWidth + 20, totalHeight + 20, borderColor);

        String headerText = Formatting.GOLD + "iBlocky Prison";
        String footerText = Formatting.AQUA + "There are " + sortedEntries.size() + " currently online!";

// Draw centered header text inside the HUD
        int headerY = centerY + 3;  // Add padding to keep it inside the HUD
        int headerX = centerX + (totalWidth / 2) - (client.textRenderer.getWidth(headerText) / 2);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(headerText), headerX, headerY, 0xFFFFFF);

// Draw centered footer text inside the HUD
        int footerY = centerY + totalHeight - 15;  // Add extra vertical space here
        int footerX = centerX + (totalWidth / 2) - (client.textRenderer.getWidth(footerText) / 2);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(footerText), footerX, footerY, 0xFFFFFF);


        // Begin scaling
        drawContext.getMatrices().push();
        drawContext.getMatrices().scale(scale, scale, 1.0F);  // Apply scaling for text rendering

        // Render the sorted player list with colors
        int x = centerX;  // Starting x position centered
        int y = centerY + 20;  // Starting y position centered, below the header
        int lineHeight = (int) (12 * scale);  // Adjust line height based on the scaling factor

        for (int i = 0; i < sortedEntries.size(); i++) {
            PlayerListEntry entry = sortedEntries.get(i);
            String playerDisplayName = getPlayerDisplayName(entry);
            String playerRank = getPlayerRankFromDisplayName(playerDisplayName);
            String playerName = playerDisplayName.substring(playerRank.length()).trim();  // Extract the name after the rank

            if (!playerName.isEmpty()) {
                Formatting rankColor = rankColorMap.getOrDefault(playerRank, Formatting.WHITE);
                String formattedText = rankColor + playerRank + " " + Formatting.WHITE + playerName;

                int column = i / maxPerColumn;
                int dynamicX = x + columnWidths.subList(0, column).stream().mapToInt(Integer::intValue).sum();

                // Draw the text at the scaled position
                drawContext.drawTextWithShadow(client.textRenderer, Text.of(formattedText), (int) (dynamicX / scale), (int) (y / scale), 0xFFFFFF);

                y += lineHeight;  // Move to the next line

                // Move to the next column if max players per column is reached
                if ((i + 1) % maxPerColumn == 0) {
                    y = centerY + 20;  // Reset y position, but below the header
                }
            }
        }

        // End scaling
        drawContext.getMatrices().pop();
    }

    public void refreshPlayerList() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // Get the latest player list and sort it every time the HUD is triggered
        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();

        sortedEntries = entries.stream()
                .filter(entry -> hasValidRankPrefix(getPlayerDisplayName(entry)))  // Exclude names without valid prefixes
                .sorted(Comparator.comparing(this::getPlayerRankWeight).reversed())
                .collect(Collectors.toList());

        // Debug print to confirm that the refresh happened
        System.out.println("Refreshing player list. Total players: " + sortedEntries.size());
    }


    private List<Integer> calculateColumnWidths(MinecraftClient client, int maxPerColumn) {
        // Calculate the width of each column based on the longest name in that column
        List<Integer> columnWidths = new ArrayList<>();
        int maxWidth = 0;

        for (int i = 0; i < sortedEntries.size(); i++) {
            PlayerListEntry entry = sortedEntries.get(i);
            String playerDisplayName = getPlayerDisplayName(entry);
            String playerRank = getPlayerRankFromDisplayName(playerDisplayName);
            String playerName = playerDisplayName.substring(playerRank.length()).trim();

            String fullText = playerRank + " " + playerName;
            int textWidth = client.textRenderer.getWidth(fullText);

            maxWidth = Math.max(maxWidth, textWidth);

            // Move to the next column after maxPerColumn players
            if ((i + 1) % maxPerColumn == 0) {
                columnWidths.add(maxWidth + 10);  // Add some padding to the width
                maxWidth = 0;
            }
        }

        // Add the last column width if there are remaining entries
        if (sortedEntries.size() % maxPerColumn != 0) {
            columnWidths.add(maxWidth + 10);
        }

        return columnWidths;
    }

    private String getPlayerDisplayName(PlayerListEntry entry) {
        if (entry.getDisplayName() != null) {
            return entry.getDisplayName().getString();
        } else {
            return entry.getProfile().getName();
        }
    }

    private String getPlayerRankFromDisplayName(String displayName) {
        for (String rank : rankWeightMap.keySet()) {
            if (displayName.startsWith(rank)) {
                return rank;
            }
        }
        return "";  // Return empty if no rank is found
    }

    private boolean hasValidRankPrefix(String displayName) {
        for (String rank : rankWeightMap.keySet()) {
            if (displayName.startsWith(rank)) {
                return true;
            }
        }
        return false;
    }

    private int getPlayerRankWeight(PlayerListEntry entry) {
        String displayName = getPlayerDisplayName(entry);
        String playerRank = getPlayerRankFromDisplayName(displayName);
        return rankWeightMap.getOrDefault(playerRank, 1);  // Default weight is 1 if rank not found
    }
}
