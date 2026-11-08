package com.geoguessr.game;

import com.geoguessr.GeoGuessrMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeoGuessrManager {
    public static final GeoGuessrManager INSTANCE = new GeoGuessrManager();

    private final Map<UUID, GeoGuessrGame> activeGames = new ConcurrentHashMap<>();
    private final List<LeaderboardEntry> leaderboard = new ArrayList<>();
    private final Random random = new Random();

    public record LeaderboardEntry(String playerName, int score, int rounds, String mode, long timestamp) {}

    private GeoGuessrManager() {}

    public boolean hasActiveGame(UUID uuid) {
        return activeGames.containsKey(uuid);
    }

    public GeoGuessrGame getGame(UUID uuid) {
        return activeGames.get(uuid);
    }

    public boolean startGame(ServerPlayer player, int rounds, int radius, GameMode mode) {
        if (hasActiveGame(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou already have an active GeoGuessr game! Use §e/geoguessr stop §cto cancel it."));
            return false;
        }

        rounds = Math.max(1, Math.min(rounds, 20));
        radius = Math.max(500, Math.min(radius, 100000));

        BlockPos origPos = player.blockPosition();
        ResourceKey<Level> origDim = player.level().dimension();
        GameType origGm = player.gameMode.getGameModeForPlayer();

        GeoGuessrGame game = new GeoGuessrGame(
                player.getUUID(),
                player.getName().getString(),
                origPos,
                origDim,
                origGm,
                rounds,
                radius,
                mode
        );

        activeGames.put(player.getUUID(), game);

        // Hide F3 coordinate & biome information using vanilla EntityEvent
        player.connection.send(new ClientboundEntityEventPacket(player, EntityEvent.REDUCED_DEBUG_INFO));

        // Set to Adventure mode to avoid destroying the terrain
        player.setGameMode(GameType.ADVENTURE);

        // Give the GeoGuessr Compass
        player.getInventory().add(new ItemStack(GeoGuessrMod.GEOGUESSR_COMPASS));

        player.sendSystemMessage(Component.literal("§8========================================"));
        player.sendSystemMessage(Component.literal("§6§l🌍 WELCOME TO MINECRAFT GEOGUESSR! 🌍"));
        player.sendSystemMessage(Component.literal("§7Mode: §f" + mode.getDisplayName() + " §7| Rounds: §f" + rounds + " §7| Radius: §f±" + radius + " blocks"));
        player.sendSystemMessage(Component.literal("§eYou have §f90 seconds §eper round to explore and guess."));
        player.sendSystemMessage(Component.literal("§bCommands:"));
        player.sendSystemMessage(Component.literal("  §f• §a/geoguessr guess <x> <z> §7- Submit coordinate guess"));
        player.sendSystemMessage(Component.literal("  §f• §a/geoguessr guess <biome> §7- Submit biome guess"));
        player.sendSystemMessage(Component.literal("  §f• §a/geoguessr skip §7- Skip current round"));
        player.sendSystemMessage(Component.literal("  §f• §a/geoguessr stop §7- Forfeit game"));
        player.sendSystemMessage(Component.literal("§8========================================"));

        startRound(game, player.level().getServer());
        return true;
    }

    public void startRound(GeoGuessrGame game, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(game.getPlayerUuid());
        if (player == null) return;

        ServerLevel level = server.overworld();
        int radius = game.getMaxRadius();

        // Generate mystery coordinates
        int targetX = (random.nextInt(radius * 2) - radius);
        int targetZ = (random.nextInt(radius * 2) - radius);

        // Load chunk
        level.getChunkSource().getChunk(targetX >> 4, targetZ >> 4, ChunkStatus.FULL, true);

        // Find safe surface Y
        int targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, targetX, targetZ);
        if (targetY <= level.getMinY()) {
            targetY = 64;
        }

        BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
        game.setTargetPos(targetPos);

        // Retrieve actual biome name
        Holder<Biome> biomeHolder = level.getBiome(targetPos);
        String biomeName = biomeHolder.unwrapKey().map(k -> k.identifier().getPath()).orElse("plains");
        game.setTargetBiome(biomeName);

        game.setTimeRemainingTicks(90 * 20); // 90 seconds
        game.setRoundActive(true);

        // Teleport player
        player.teleportTo(level, targetX + 0.5, targetY + 1.0, targetZ + 0.5, Set.of(), random.nextFloat() * 360.0f, 0.0f, false);

        // Send Titles
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6Round " + game.getCurrentRound() + "/" + game.getTotalRounds())));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eWhere are you in the Overworld?")));

        player.playSound(SoundEvents.UI_TOAST_IN, 1.0f, 1.0f);
    }

    public boolean submitCoordinateGuess(ServerPlayer player, double guessX, double guessZ) {
        GeoGuessrGame game = activeGames.get(player.getUUID());
        if (game == null || !game.isRoundActive()) {
            player.sendSystemMessage(Component.literal("§cNo active round to submit a guess for!"));
            return false;
        }

        BlockPos target = game.getTargetPos();
        double dx = guessX - target.getX();
        double dz = guessZ - target.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // GeoGuessr exponential decay formula
        // Perfect 5000 points within 20 blocks; scales gracefully with game radius
        double scale = Math.max(1000.0, game.getMaxRadius() * 0.25);
        int score;
        if (distance <= 20.0) {
            score = 5000;
        } else {
            score = (int) Math.round(5000.0 * Math.exp(-distance / scale));
            score = Math.max(0, Math.min(5000, score));
        }

        game.addScore(score);
        game.addDistance(distance);
        game.setRoundActive(false);

        // Round summary feedback
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a+" + score + " Points!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(String.format("§eDistance: §f%.1f blocks §eaway", distance))));

        player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);

        player.sendSystemMessage(Component.literal("§8----------------------------------------"));
        player.sendSystemMessage(Component.literal(String.format("§6Round %d Result: §a+%d pts §7(Total: §6%d§7)", game.getCurrentRound(), score, game.getTotalScore())));
        player.sendSystemMessage(Component.literal(String.format("§7Your Guess: §f(X: %.0f, Z: %.0f)", guessX, guessZ)));
        player.sendSystemMessage(Component.literal(String.format("§7Actual Spot: §e(X: %d, Y: %d, Z: %d) §8[%s]", target.getX(), target.getY(), target.getZ(), game.getTargetBiome())));
        player.sendSystemMessage(Component.literal(String.format("§7Distance Off: §c%.1f blocks", distance)));
        player.sendSystemMessage(Component.literal("§8----------------------------------------"));

        advanceRound(game, player.level().getServer());
        return true;
    }

    public boolean submitBiomeGuess(ServerPlayer player, String guessBiome) {
        GeoGuessrGame game = activeGames.get(player.getUUID());
        if (game == null || !game.isRoundActive()) {
            player.sendSystemMessage(Component.literal("§cNo active round to submit a guess for!"));
            return false;
        }

        String targetBiome = game.getTargetBiome().toLowerCase(Locale.ROOT);
        String cleanGuess = guessBiome.toLowerCase(Locale.ROOT).replace("minecraft:", "").trim();

        boolean exact = cleanGuess.equals(targetBiome);
        int score;
        if (exact) {
            score = 5000;
        } else if (isRelatedBiome(cleanGuess, targetBiome)) {
            score = 2500;
        } else {
            score = 0;
        }

        game.addScore(score);
        game.addDistance(exact ? 0.0 : 1000.0);
        game.setRoundActive(false);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(exact ? "§aPERFECT! +5000" : (score > 0 ? "§eCLOSE! +2500" : "§cMISSED! +0"))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Actual Biome: §b" + targetBiome)));

        player.playSound(exact ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);

        player.sendSystemMessage(Component.literal(String.format("§6Round %d Biome Result: §a+%d pts §7(Actual: §b%s§7, Guessed: §e%s§7)",
                game.getCurrentRound(), score, targetBiome, cleanGuess)));

        advanceRound(game, player.level().getServer());
        return true;
    }

    public void skipRound(ServerPlayer player) {
        GeoGuessrGame game = activeGames.get(player.getUUID());
        if (game == null || !game.isRoundActive()) {
            player.sendSystemMessage(Component.literal("§cNo active round to skip!"));
            return;
        }

        game.addScore(0);
        game.addDistance(game.getMaxRadius());
        game.setRoundActive(false);

        player.sendSystemMessage(Component.literal("§eRound " + game.getCurrentRound() + " skipped (0 pts)."));
        advanceRound(game, player.level().getServer());
    }

    private void advanceRound(GeoGuessrGame game, MinecraftServer server) {
        if (game.getCurrentRound() >= game.getTotalRounds()) {
            endGame(game, server, true);
        } else {
            game.setCurrentRound(game.getCurrentRound() + 1);
            game.setCooldownTicks(60); // 3-second intermission
        }
    }

    public void stopGame(ServerPlayer player) {
        GeoGuessrGame game = activeGames.remove(player.getUUID());
        if (game != null) {
            restorePlayer(game, player.level().getServer());
            player.sendSystemMessage(Component.literal("§eGeoGuessr game stopped. Teleported back to your starting point!"));
        } else {
            player.sendSystemMessage(Component.literal("§cYou don't have an active game running."));
        }
    }

    public void endGame(GeoGuessrGame game, MinecraftServer server, boolean completed) {
        activeGames.remove(game.getPlayerUuid());
        ServerPlayer player = server.getPlayerList().getPlayer(game.getPlayerUuid());

        restorePlayer(game, server);

        if (player != null && completed) {
            int score = game.getTotalScore();
            int maxPossible = game.getTotalRounds() * 5000;
            double percentage = (double) score / maxPossible * 100.0;
            String rank = getRankTitle(percentage);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§lGAME FINISHED!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eScore: §a" + score + "§7/§a" + maxPossible + " §e(" + rank + "§e)")));

            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            player.sendSystemMessage(Component.literal("§8========================================"));
            player.sendSystemMessage(Component.literal("§6§l🏆 GEOGUESSR FINAL RESULTS 🏆"));
            player.sendSystemMessage(Component.literal(String.format("§eFinal Score: §a%d §7/ §a%d §e(%.1f%%)", score, maxPossible, percentage)));
            player.sendSystemMessage(Component.literal("§eRank Achieved: " + rank));

            List<Integer> scores = game.getRoundScores();
            List<Double> dists = game.getRoundDistances();
            for (int i = 0; i < scores.size(); i++) {
                double d = i < dists.size() ? dists.get(i) : 0.0;
                player.sendSystemMessage(Component.literal(String.format("  §7Round %d: §a%d pts §7(Off by §f%.0fm§7)", i + 1, scores.get(i), d)));
            }
            player.sendSystemMessage(Component.literal("§8========================================"));

            // Record to leaderboard
            leaderboard.add(new LeaderboardEntry(game.getPlayerName(), score, game.getTotalRounds(), game.getMode().getDisplayName(), System.currentTimeMillis()));
            leaderboard.sort((a, b) -> Integer.compare(b.score(), a.score()));
        }
    }

    private void restorePlayer(GeoGuessrGame game, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(game.getPlayerUuid());
        if (player == null) return;

        // Restore debug screen to full
        player.connection.send(new ClientboundEntityEventPacket(player, EntityEvent.FULL_DEBUG_INFO));

        // Restore game mode
        player.setGameMode(game.getOriginalGameMode());

        // Teleport back to origin
        ServerLevel origLevel = server.getLevel(game.getOriginalDimension());
        if (origLevel != null) {
            BlockPos pos = game.getOriginalPos();
            player.teleportTo(origLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), 0.0f, 0.0f, false);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        GeoGuessrGame game = activeGames.remove(player.getUUID());
        if (game != null) {
            restorePlayer(game, player.level().getServer());
        }
    }

    public void tick(MinecraftServer server) {
        for (GeoGuessrGame game : activeGames.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(game.getPlayerUuid());
            if (player == null) continue;

            // Handle round transition cooldown
            if (game.getCooldownTicks() > 0) {
                game.decrementCooldown();
                if (game.getCooldownTicks() == 0) {
                    startRound(game, server);
                }
                continue;
            }

            if (!game.isRoundActive()) continue;

            game.decrementTime();
            int remainingTicks = game.getTimeRemainingTicks();

            // Actionbar update once every second (20 ticks)
            if (remainingTicks % 20 == 0) {
                int totalSec = remainingTicks / 20;
                int min = totalSec / 60;
                int sec = totalSec % 60;
                String timeStr = String.format("%02d:%02d", min, sec);

                ChatFormatting timeColor = totalSec <= 15 ? ChatFormatting.RED : (totalSec <= 30 ? ChatFormatting.GOLD : ChatFormatting.GREEN);

                MutableComponent hud = Component.literal(String.format("§6Round %d/%d §7| §eScore: §a%d §7| ⏱ ",
                        game.getCurrentRound(), game.getTotalRounds(), game.getTotalScore()))
                        .append(Component.literal(timeStr).withStyle(timeColor))
                        .append(Component.literal(" §7| §b/geoguessr guess <x> <z>"));

                player.sendSystemMessage(hud, true);
            }

            // Warning sound on 10, 5, 4, 3, 2, 1 seconds
            int totalSec = remainingTicks / 20;
            if (remainingTicks % 20 == 0 && totalSec <= 5 && totalSec > 0) {
                player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.2f);
            }

            // Time's up!
            if (remainingTicks <= 0) {
                player.sendSystemMessage(Component.literal("§c⏱ Time's up for Round " + game.getCurrentRound() + "! +0 points."));
                game.addScore(0);
                game.addDistance(game.getMaxRadius());
                game.setRoundActive(false);
                advanceRound(game, server);
            }
        }
    }

    private String getRankTitle(double percentage) {
        if (percentage >= 95.0) return "§6§lGrandmaster Cartographer 🌟";
        if (percentage >= 80.0) return "§d§lMaster Explorer 🧭";
        if (percentage >= 60.0) return "§b§lSeasoned Traveler 🗺️";
        if (percentage >= 40.0) return "§aWandering Scout 🎒";
        return "§7Lost Adventurer ❓";
    }

    private boolean isRelatedBiome(String a, String b) {
        if (a.contains("forest") && b.contains("forest")) return true;
        if (a.contains("desert") && b.contains("badlands")) return true;
        if (a.contains("snow") && b.contains("ice")) return true;
        if (a.contains("ocean") && b.contains("ocean")) return true;
        if (a.contains("plains") && b.contains("meadow")) return true;
        if (a.contains("swamp") && b.contains("mangrove")) return true;
        return false;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        return Collections.unmodifiableList(leaderboard);
    }
}
