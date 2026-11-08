package com.geoguessr.game;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GeoGuessrGame {
    private final UUID playerUuid;
    private final String playerName;
    private final BlockPos originalPos;
    private final ResourceKey<Level> originalDimension;
    private final GameType originalGameMode;
    private final int totalRounds;
    private final int maxRadius;
    private final GameMode mode;

    private int currentRound = 1;
    private int totalScore = 0;
    private final List<Integer> roundScores = new ArrayList<>();
    private final List<Double> roundDistances = new ArrayList<>();

    private BlockPos targetPos;
    private String targetBiome = "";
    private String targetLandmark = "";
    private int timeRemainingTicks = 90 * 20; // 90 seconds per round
    private boolean roundActive = false;
    private int cooldownTicks = 0;

    public GeoGuessrGame(UUID playerUuid, String playerName, BlockPos originalPos,
                         ResourceKey<Level> originalDimension, GameType originalGameMode,
                         int totalRounds, int maxRadius, GameMode mode) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.originalPos = originalPos;
        this.originalDimension = originalDimension;
        this.originalGameMode = originalGameMode;
        this.totalRounds = totalRounds;
        this.maxRadius = maxRadius;
        this.mode = mode;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public BlockPos getOriginalPos() {
        return originalPos;
    }

    public ResourceKey<Level> getOriginalDimension() {
        return originalDimension;
    }

    public GameType getOriginalGameMode() {
        return originalGameMode;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public GameMode getMode() {
        return mode;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void addScore(int score) {
        this.totalScore += score;
        this.roundScores.add(score);
    }

    public List<Integer> getRoundScores() {
        return roundScores;
    }

    public List<Double> getRoundDistances() {
        return roundDistances;
    }

    public void addDistance(double dist) {
        this.roundDistances.add(dist);
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public String getTargetBiome() {
        return targetBiome;
    }

    public void setTargetBiome(String targetBiome) {
        this.targetBiome = targetBiome;
    }

    public String getTargetLandmark() {
        return targetLandmark;
    }

    public void setTargetLandmark(String targetLandmark) {
        this.targetLandmark = targetLandmark;
    }

    public int getTimeRemainingTicks() {
        return timeRemainingTicks;
    }

    public void setTimeRemainingTicks(int timeRemainingTicks) {
        this.timeRemainingTicks = timeRemainingTicks;
    }

    public void decrementTime() {
        if (this.timeRemainingTicks > 0) {
            this.timeRemainingTicks--;
        }
    }

    public boolean isRoundActive() {
        return roundActive;
    }

    public void setRoundActive(boolean roundActive) {
        this.roundActive = roundActive;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public void decrementCooldown() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
    }
}
