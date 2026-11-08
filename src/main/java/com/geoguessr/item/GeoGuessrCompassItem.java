package com.geoguessr.item;

import com.geoguessr.game.GeoGuessrManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class GeoGuessrCompassItem extends Item {
    public GeoGuessrCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (GeoGuessrManager.INSTANCE.hasActiveGame(serverPlayer.getUUID())) {
                serverPlayer.sendSystemMessage(Component.literal("§6[GeoGuessr] §e🧭 Guessing Compass: Type §b/geoguessr guess <x> <z> §eto submit coordinates, or §a/geoguessr guess <biome> §efor biomes!"));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("§6[GeoGuessr] §7You are not in an active game! Use §a/geoguessr start §7to begin playing."));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
