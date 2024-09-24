package net.holm.iblockycompanion.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.holm.iblockycompanion.ConfigMenu;  // Ensure your ConfigMenu class is imported
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    public void onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;

        ButtonWidget configButton = new ButtonWidget.Builder(Text.of("Mod Config"), button -> {
            MinecraftClient.getInstance().setScreen(new ConfigMenu(screen));
        }).dimensions(screen.width / 2 - 100, screen.height / 6 + 24, 200, 20).build();

        // Use addDrawableChild after access widener is applied
        screen.addSelectableChild(configButton);
    }
}