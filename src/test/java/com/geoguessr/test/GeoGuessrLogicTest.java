package com.geoguessr.test;

import com.geoguessr.game.GameMode;
import com.geoguessr.game.GeoGuessrGame;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GeoGuessrLogicTest {

    @Test
    @DisplayName("Test GameMode parsing")
    void testGameModeParsing() {
        assertEquals(GameMode.COORDINATES, GameMode.fromString("coordinates"));
        assertEquals(GameMode.COORDINATES, GameMode.fromString("Coordinates"));
        assertEquals(GameMode.BIOME, GameMode.fromString("biome"));
        assertEquals(GameMode.BIOME, GameMode.fromString("Biome"));
        assertEquals(GameMode.LANDMARK, GameMode.fromString("landmark"));
        assertEquals(GameMode.COORDINATES, GameMode.fromString("unknown_fallback"));
    }

    @Test
    @DisplayName("Test GeoGuessrGame scoring and progression")
    void testGameSessionScoring() {
        UUID testUuid = UUID.randomUUID();
        BlockPos origPos = new BlockPos(100, 64, -200);

        GeoGuessrGame game = new GeoGuessrGame(
                testUuid,
                "Steve",
                origPos,
                null, // No bootstrap needed for unit test
                null,
                5,
                10000,
                GameMode.COORDINATES
        );

        assertEquals(1, game.getCurrentRound());
        assertEquals(5, game.getTotalRounds());
        assertEquals(0, game.getTotalScore());
        assertEquals("Steve", game.getPlayerName());

        // Target: (1000, 70, 2000)
        game.setTargetPos(new BlockPos(1000, 70, 2000));
        game.setTargetBiome("cherry_grove");
        game.setRoundActive(true);

        // Simulated guess: exact point
        double guessX = 1000.0;
        double guessZ = 2000.0;
        double dx = guessX - game.getTargetPos().getX();
        double dz = guessZ - game.getTargetPos().getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        assertEquals(0.0, dist, 0.001);

        int score = dist <= 20.0 ? 5000 : (int) Math.round(5000.0 * Math.exp(-dist / 2500.0));
        assertEquals(5000, score);

        game.addScore(score);
        game.addDistance(dist);
        assertEquals(5000, game.getTotalScore());
        assertEquals(1, game.getRoundScores().size());

        // Round 2 guess: 500 blocks away
        game.setCurrentRound(2);
        game.setTargetPos(new BlockPos(-500, 65, 800));
        double guess2X = -200.0; // dx = 300
        double guess2Z = 1200.0; // dz = 400
        double dist2 = Math.sqrt(300 * 300 + 400 * 400); // 500 blocks
        assertEquals(500.0, dist2, 0.001);

        int score2 = (int) Math.round(5000.0 * Math.exp(-dist2 / 2500.0));
        assertTrue(score2 > 4000 && score2 < 4200, "Score for 500m should be around 4094, got: " + score2);

        game.addScore(score2);
        game.addDistance(dist2);
        assertEquals(5000 + score2, game.getTotalScore());
        assertEquals(2, game.getRoundScores().size());
    }

    @Test
    @DisplayName("Test Timer Countdown")
    void testTimer() {
        UUID testUuid = UUID.randomUUID();
        GeoGuessrGame game = new GeoGuessrGame(
                testUuid,
                "Alex",
                BlockPos.ZERO,
                null,
                null,
                3,
                5000,
                GameMode.BIOME
        );

        game.setTimeRemainingTicks(1800);
        game.decrementTime();
        assertEquals(1799, game.getTimeRemainingTicks());
    }
}
