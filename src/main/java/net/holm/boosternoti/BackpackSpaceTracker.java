package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BackpackSpaceTracker {
    private static int lastKnownLevel = -1;  // Initialize to -1 to detect the first level read
    private static long backpackStartTime = 0;  // Timestamp when the player starts filling their backpack
    private static boolean isFillingBackpack = false;  // Whether the player is actively filling the backpack
    private static long lastElapsedTime = 0;  // Stores the last elapsed time when the backpack was sold

    public static void init() {
        // Register a tick event to check player levels each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                int currentLevel = client.player.experienceLevel;

                if (lastKnownLevel == -1) {
                    lastKnownLevel = currentLevel;  // Initialize the last known level at first tick
                    return;
                }

                if (currentLevel < lastKnownLevel) {
                    // Levels decreased - backpack is filling
                    if (!isFillingBackpack) {
                        backpackStartTime = System.currentTimeMillis();
                        isFillingBackpack = true;
                    }
                } else if (currentLevel > lastKnownLevel && isFillingBackpack) {
                    // Levels increased - backpack was sold
                    lastElapsedTime = System.currentTimeMillis() - backpackStartTime;  // Save the last recorded time after selling
                    isFillingBackpack = false;
                }

                lastKnownLevel = currentLevel;  // Update the last known level for the next tick
            }
        });
    }

    public static String getElapsedTime() {
        if (isFillingBackpack) {
            // If still filling the backpack, calculate elapsed time
            long elapsedMillis = System.currentTimeMillis() - backpackStartTime;
            return formatTime(elapsedMillis / 1000);  // Convert to seconds and format
        } else if (lastElapsedTime > 0) {
            // If the player sold the backpack, show the frozen time
            return formatTime(lastElapsedTime / 1000);
        } else {
            // No data available yet
            return "N/A";
        }
    }

    private static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02dm %02ds", minutes, seconds);
    }
}