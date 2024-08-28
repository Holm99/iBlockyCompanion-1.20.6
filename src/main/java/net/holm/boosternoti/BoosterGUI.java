package net.holm.boosternoti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class BoosterGUI {

    private static String boosterInfo = ""; // Booster information to display

    // Render method using DrawContext
    public static void render(DrawContext drawContext) {
        if (!boosterInfo.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int width = client.getWindow().getScaledWidth();

            // Position the text at the top center of the screen
            drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterInfo), width / 2 - client.textRenderer.getWidth(boosterInfo) / 2, 10, 0xFFFFFF);
        }
    }

    public static void updateBoosterInfo(String multiplier, String remaining) {
        boosterInfo = "Booster: " + multiplier + ", Time Left: " + remaining;
    }

    public static void clearBoosterInfo() {
        boosterInfo = ""; // Clear the booster information
    }

    public static void render(DrawContext drawContext, float v) {
    }
}
