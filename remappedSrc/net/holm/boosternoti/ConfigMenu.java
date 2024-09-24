package net.holm.iblockycompanion;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class ConfigMenu extends Screen {

    private final Screen parent;

    // Constructor takes in the parent screen (for returning to it later)
    public ConfigMenu(Screen parent) {
        super(Text.of("Mod Configuration"));
        this.parent = parent;
    }

    // Initialize method to set up the buttons and components
    @Override
    protected void init() {
        // Add a 'Done' button to return to the parent screen
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Done"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 25, 200, 20).build());

        // Add additional config buttons or options here
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Toggle Feature"), button -> {
            // Toggle a feature in the mod (this is just a placeholder for an actual feature)
            System.out.println("Feature Toggled!");
        }).dimensions(this.width / 2 - 100, this.height / 2 - 25, 200, 20).build());
    }

    // Render the screen elements, including background and text
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fill the screen with a gradient background
        context.fillGradient(0, 0, this.width, this.height, 0xB0000000, 0xD0000000);

        // Manually calculate the center of the screen for the text
        int centerX = this.width / 2;
        int textWidth = this.textRenderer.getWidth(this.title);
        int textX = centerX - textWidth / 2;  // Center the text horizontally

        // Draw the centered title
        context.drawText(this.textRenderer, this.title, textX, 20, 0xFFFFFF, false);

        // Render buttons and other components
        super.render(context, mouseX, mouseY, delta);
    }

    // Overriding the tick method if needed (optional)
    @Override
    public void tick() {
        super.tick();
    }

    // Close the screen on Esc key
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
