package com.geoguessr.client;

import com.geoguessr.client.gui.GeoGuessrGuessScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoGuessrModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("geoguessr-client");

    public static KeyMapping OPEN_GUESS_KEY;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Minecraft GeoGuessr Client...");

        // Register Keybinding (Default 'G' key)
        OPEN_GUESS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.geoguessr.open_gui",
                InputConstants.Type.KEYBOARD,
                71, // G key
                KeyMapping.Category.GAMEPLAY
        ));

        // When 'G' is pressed in game, open the Guessing UI Screen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUESS_KEY.consumeClick()) {
                if (client.player != null && client.canInterruptScreen()) {
                    client.setScreenAndShow(new GeoGuessrGuessScreen());
                }
            }
        });

        LOGGER.info("Minecraft GeoGuessr Client initialized successfully!");
    }
}
