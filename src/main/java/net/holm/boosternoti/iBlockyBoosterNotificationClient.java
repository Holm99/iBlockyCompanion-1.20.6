package net.holm.boosternoti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iBlockyBoosterNotificationClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(iBlockyBoosterNotification.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Booster Display Mod (Client) has been initialized!");

        // Register the HUD render callback with DrawContext as the parameter type
        HudRenderCallback.EVENT.register(BoosterGUI::render);
    }
}
