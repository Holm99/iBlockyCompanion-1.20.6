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
    private String currentGameMode = "Unknown";
    private boolean gameModeDetected = false;

    static {
        // Rank weight map for Prison
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

        // Rank weight map for Survival
        rankWeightMap.put("ꜱᴇɴɪᴏʀ ᴍᴏᴅ", 12);
        rankWeightMap.put("ʜᴇʟᴘᴇʀ", 10);
        rankWeightMap.put("ɪᴍᴍᴏʀᴛᴀʟ", 8);
        rankWeightMap.put("ᴇᴍᴘᴇʀᴏʀ", 7);
        rankWeightMap.put("ᴛɪᴛᴀɴ", 6);
        rankWeightMap.put("ᴄʜᴀᴍᴘɪᴏɴ", 5);
        rankWeightMap.put("ᴇʟᴅᴇʀ", 4);
        rankWeightMap.put("ʜᴇʀᴏ", 3);
        rankWeightMap.put("ᴅᴇꜰᴀᴜʟᴛ", 2);

        // Prison Rank Color pap
        rankColorMap.put("ꜱᴇɴɪᴏʀ ᴍᴏᴅᴇʀᴀᴛᴏʀ", Formatting.BLUE);
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

        // Placeholder for the Survival Rank Color map (customize later)
        rankColorMap.put("ᴀᴅᴍɪɴ", Formatting.DARK_RED);
        rankColorMap.put("ꜱᴇɴɪᴏʀ ᴍᴏᴅ", Formatting.DARK_AQUA);
        rankColorMap.put("ᴍᴏᴅᴇʀᴀᴛᴏʀ", Formatting.DARK_AQUA);
        rankColorMap.put("ʜᴇʟᴘᴇʀ", Formatting.GREEN);
        rankColorMap.put("ɪᴍᴍᴏʀᴛᴀʟ", Formatting.DARK_RED);
        rankColorMap.put("ᴇᴍᴘᴇʀᴏʀ", Formatting.DARK_GREEN);
        rankColorMap.put("ᴛɪᴛᴀɴ", Formatting.RED);
        rankColorMap.put("ᴄʜᴀᴍᴘɪᴏɴ", Formatting.YELLOW);
        rankColorMap.put("ᴇʟᴅᴇʀ", Formatting.GREEN);
        rankColorMap.put("ʜᴇʀᴏ", Formatting.AQUA);
        rankColorMap.put("ᴅᴇꜰᴀᴜʟᴛ", Formatting.GRAY);
    }

    public void renderPlayerList(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // Detect game mode, checking only if necessary
        if (!gameModeDetected || isGameModeChanged()) {
            detectGameMode();
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

        String headerText = Formatting.GOLD + "iBlocky " + currentGameMode;
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

    // Game mode detection based on ranks
    void detectGameMode() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        int prisonRankCount = 0;
        int survivalRankCount = 0;

        for (PlayerListEntry entry : entries) {
            String playerDisplayName = getPlayerDisplayName(entry);
            String playerRank = getPlayerRankFromDisplayName(playerDisplayName);

            // Exclude checking for the common ranks 'ᴀᴅᴍɪɴ' and 'ɪʙʟᴏᴄᴋʏ'
            if (isPrisonRank(playerRank)) {
                prisonRankCount++;
            } else if (isSurvivalRank(playerRank)) {
                survivalRankCount++;
            }
        }

        // Determine the game mode based on the rank counts
        if (prisonRankCount > survivalRankCount) {
            currentGameMode = "Prison";
        } else if (survivalRankCount > prisonRankCount) {
            currentGameMode = "Survival";
        } else {
            currentGameMode = "Unknown";  // If no conclusive ranks found
        }

        // Set flag to true after detecting
        gameModeDetected = true;
    }

    private boolean isPrisonRank(String rank) {
        return !rank.equals("ᴀᴅᴍɪɴ") && !rank.equals("ɪʙʟᴏᴄᴋʏ") && (
                rank.equals("ꜱᴇɴɪᴏʀ ᴍᴏᴅᴇʀᴀᴛᴏʀ") || rank.equals("ᴍᴏᴅᴇʀᴀᴛᴏʀ") ||
                        rank.equals("ᴛʀɪᴀʟ ᴍᴏᴅᴇʀᴀᴛᴏʀ") || rank.equals("THE 1%") || rank.equals("ᴋɪɴɢᴘɪɴ") ||
                        rank.equals("ɢᴀɴɢꜱᴛᴇʀ") || rank.equals("ᴛʜᴜɢ") || rank.equals("ʜɪᴛᴍᴀɴ") ||
                        rank.equals("ᴄʀɪᴍɪɴᴀʟ") || rank.equals("ᴅᴇᴀʟᴇʀ") || rank.equals("ᴘʀɪꜱᴏɴᴇʀ"));
    }

    private boolean isSurvivalRank(String rank) {
        return !rank.equals("ᴀᴅᴍɪɴ") && !rank.equals("ɪʙʟᴏᴄᴋʏ") && (
                rank.equals("ꜱᴇɴɪᴏʀ ᴍᴏᴅ") || rank.equals("ᴍᴏᴅᴇʀᴀᴛᴏʀ") || rank.equals("ʜᴇʟᴘᴇʀ") ||
                        rank.equals("ɪᴍᴏʀᴛᴀʟ") || rank.equals("ᴇᴍᴘᴇʀᴏʀ") || rank.equals("ᴛɪᴛᴀɴ") ||
                        rank.equals("ᴄʜᴀᴍᴘɪᴏɴ") || rank.equals("ᴇʟᴅᴇʀ") || rank.equals("ʜᴇʀᴏ") || rank.equals("ᴅᴇꜰᴀᴜʟᴛ"));
    }

    public boolean isGameModeChanged() {
        String previousGameMode = currentGameMode;
        String detectedMode = detectGameModeWithoutUpdatingFlag();  // Detect game mode but don’t set it yet
        return !detectedMode.equals(previousGameMode);
    }

    private String detectGameModeWithoutUpdatingFlag() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return currentGameMode;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        int prisonRankCount = 0;
        int survivalRankCount = 0;

        for (PlayerListEntry entry : entries) {
            String playerDisplayName = getPlayerDisplayName(entry);
            String playerRank = getPlayerRankFromDisplayName(playerDisplayName);

            // Check ranks, excluding common ones like 'ᴀᴅᴍɪɴ' and 'ɪʙʟᴏᴄᴋʏ'
            if (isPrisonRank(playerRank)) {
                prisonRankCount++;
            } else if (isSurvivalRank(playerRank)) {
                survivalRankCount++;
            }
        }

        if (prisonRankCount > survivalRankCount) {
            return "Prison";
        } else if (survivalRankCount > prisonRankCount) {
            return "Survival";
        } else {
            return "Unknown";
        }
    }

    public boolean isGameModeDetected() {
        return gameModeDetected;
    }

    public String getCurrentGameMode() {
        return currentGameMode;
    }


}
