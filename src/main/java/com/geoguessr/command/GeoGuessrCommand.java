package com.geoguessr.command;

import com.geoguessr.game.GameMode;
import com.geoguessr.game.GeoGuessrManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class GeoGuessrCommand {
    private static final List<String> COMMON_BIOMES = List.of(
            "plains", "sunflower_plains", "snowy_plains", "ice_spikes", "desert",
            "swamp", "mangrove_swamp", "forest", "flower_forest", "birch_forest",
            "dark_forest", "pale_garden", "taiga", "snowy_taiga", "old_growth_pine_taiga",
            "jungle", "sparse_jungle", "bamboo_jungle", "badlands", "eroded_badlands",
            "wooded_badlands", "meadow", "cherry_grove", "grove", "snowy_slopes",
            "jagged_peaks", "frozen_peaks", "stony_peaks", "river", "beach",
            "mushroom_fields", "dripstone_caves", "lush_caves", "deep_dark"
    );

    private static final List<String> MODES = List.of("coordinates", "biome", "landmark");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("geoguessr")
                // /geoguessr start
                .then(Commands.literal("start")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        GeoGuessrManager.INSTANCE.startGame(player, 5, 10000, GameMode.COORDINATES);
                        return 1;
                    })
                    .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int rounds = IntegerArgumentType.getInteger(ctx, "rounds");
                            GeoGuessrManager.INSTANCE.startGame(player, rounds, 10000, GameMode.COORDINATES);
                            return 1;
                        })
                        .then(Commands.argument("radius", IntegerArgumentType.integer(500, 100000))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int rounds = IntegerArgumentType.getInteger(ctx, "rounds");
                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                GeoGuessrManager.INSTANCE.startGame(player, rounds, radius, GameMode.COORDINATES);
                                return 1;
                            })
                            .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(MODES, b))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int rounds = IntegerArgumentType.getInteger(ctx, "rounds");
                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                    String modeStr = StringArgumentType.getString(ctx, "mode");
                                    GameMode mode = GameMode.fromString(modeStr);
                                    GeoGuessrManager.INSTANCE.startGame(player, rounds, radius, mode);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // /geoguessr guess <x> <z> OR /geoguessr guess <biome>
                .then(Commands.literal("guess")
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                double x = DoubleArgumentType.getDouble(ctx, "x");
                                double z = DoubleArgumentType.getDouble(ctx, "z");
                                GeoGuessrManager.INSTANCE.submitCoordinateGuess(player, x, z);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.argument("biome", StringArgumentType.string())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(COMMON_BIOMES, b))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String biome = StringArgumentType.getString(ctx, "biome");
                            GeoGuessrManager.INSTANCE.submitBiomeGuess(player, biome);
                            return 1;
                        })
                    )
                )
                // /geoguessr skip
                .then(Commands.literal("skip")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        GeoGuessrManager.INSTANCE.skipRound(player);
                        return 1;
                    })
                )
                // /geoguessr stop
                .then(Commands.literal("stop")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        GeoGuessrManager.INSTANCE.stopGame(player);
                        return 1;
                    })
                )
                // /geoguessr leaderboard
                .then(Commands.literal("leaderboard")
                    .executes(ctx -> {
                        CommandSourceStack src = ctx.getSource();
                        var lb = GeoGuessrManager.INSTANCE.getLeaderboard();
                        src.sendSystemMessage(Component.literal("§6§l🏆 GEOGUESSR HALL OF FAME 🏆"));
                        if (lb.isEmpty()) {
                            src.sendSystemMessage(Component.literal("§7No games completed yet. Be the first to win!"));
                        } else {
                            int count = Math.min(10, lb.size());
                            for (int i = 0; i < count; i++) {
                                var entry = lb.get(i);
                                src.sendSystemMessage(Component.literal(String.format("§e#%d §f%s §7- §a%d pts §8(%s, %d rounds)",
                                        i + 1, entry.playerName(), entry.score(), entry.mode(), entry.rounds())));
                            }
                        }
                        return 1;
                    })
                )
                // /geoguessr help
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        CommandSourceStack src = ctx.getSource();
                        src.sendSystemMessage(Component.literal("§8========================================"));
                        src.sendSystemMessage(Component.literal("§6§l🌍 Minecraft GeoGuessr Guide"));
                        src.sendSystemMessage(Component.literal("§e1. Start: §f/geoguessr start [rounds] [radius] [mode]"));
                        src.sendSystemMessage(Component.literal("   §7Default: 5 rounds, ±10000 blocks radius."));
                        src.sendSystemMessage(Component.literal("§e2. Explore: §fLook around terrain, biomes, foliage, sun."));
                        src.sendSystemMessage(Component.literal("§e3. Guess: §b/geoguessr guess <x> <z> §7(coordinates)"));
                        src.sendSystemMessage(Component.literal("   §fOr: §b/geoguessr guess <biome> §7(e.g. cherry_grove)"));
                        src.sendSystemMessage(Component.literal("§e4. Compass: §fRight-click GeoGuessr Compass in hand."));
                        src.sendSystemMessage(Component.literal("§e5. Scoring: §a5,000 pts §7max per round (25,000 perfect score)."));
                        src.sendSystemMessage(Component.literal("§8========================================"));
                        return 1;
                    })
                )
        );
    }
}
