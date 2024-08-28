package net.holm.boosternoti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class BoosterGUI {
    private static String boosterInfo = "";  // This will hold the booster information to display

    // Method to update the booster information to be displayed
    public static void updateBoosterInfo(String multiplier, String remainingTime) {
        boosterInfo = "Tokens: " + multiplier + " (" + remainingTime + " remaining)";
    }

    // Method to clear booster information when no boosters are active
    public static void clearBoosterInfo() {
        boosterInfo = "";
    }

    // Method to render the booster information on the screen using DrawContext
    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null && !boosterInfo.isEmpty()) {
            int x = 10;
            int y = 10;

            // Draw the booster information at the top left corner of the screen using DrawContext
            context.drawText(client.textRenderer, Text.of(boosterInfo), x, y, 0xFFFFFF, false); // Use DrawContext to draw text
        }
    }
}
