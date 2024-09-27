package net.holm.iblockycompanion.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.holm.iblockycompanion.ConfigMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void customConfigButton(CallbackInfo ci) {
        // Log injection point confirmation
        System.out.println("Custom config button injection point reached");

        // Access the OptionsScreen instance
        OptionsScreen screen = (OptionsScreen) (Object) this;

        // Create the custom button
        ButtonWidget customConfigButton = ButtonWidget.builder(Text.of("iBlocky Companion Config"), (button) -> {
            // Open the custom config screen when the button is clicked
            MinecraftClient.getInstance().setScreen(new ConfigMenu(screen));
        }).dimensions(screen.width / 2 - 100, screen.height / 6 + 155, 200, 20).build();

        // Add the button to the screen
        screen.addDrawableChild(customConfigButton);
    }
}