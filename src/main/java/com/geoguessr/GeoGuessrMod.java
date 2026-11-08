package com.geoguessr;

import com.geoguessr.command.GeoGuessrCommand;
import com.geoguessr.game.GeoGuessrManager;
import com.geoguessr.item.GeoGuessrCompassItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoGuessrMod implements ModInitializer {
    public static final String MOD_ID = "geoguessr";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item GEOGUESSR_COMPASS = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "compass"),
            new GeoGuessrCompassItem(new Item.Properties().stacksTo(1))
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Minecraft GeoGuessr Mod for Minecraft 26.3...");

        // Register /geoguessr commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GeoGuessrCommand.register(dispatcher);
        });

        // Register server tick event for timers and Actionbar HUD updates
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GeoGuessrManager.INSTANCE.tick(server);
        });

        // Register player disconnect handler
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GeoGuessrManager.INSTANCE.onPlayerDisconnect(handler.getPlayer());
        });

        // Clean up when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Shutting down GeoGuessr sessions...");
            for (var player : server.getPlayerList().getPlayers()) {
                if (GeoGuessrManager.INSTANCE.hasActiveGame(player.getUUID())) {
                    GeoGuessrManager.INSTANCE.stopGame(player);
                }
            }
        });

        LOGGER.info("Minecraft GeoGuessr Mod initialized successfully!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
