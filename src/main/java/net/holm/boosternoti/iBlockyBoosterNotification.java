package net.holm.boosternoti;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iBlockyBoosterNotification implements ModInitializer {
	public static final String MOD_ID = "iblocky-boosternotification";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Booster Display Mod has been initialized!");
		// Initialize any necessary server-side or common logic here
		BoosterDisplayManager.initialize();  // Placeholder for any future logic
	}
}