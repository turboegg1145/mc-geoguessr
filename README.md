# 🌍 Minecraft GeoGuessr Mod (Fabric 26.3)

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-green.svg)](https://fabricmc.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.20.1-blue.svg)](https://fabricmc.net/)
[![Java 25 LTS](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://adoptium.net/)
[![Docker Ready](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An immersive **GeoGuessr** game mode built as a native Fabric mod for **Minecraft 26.3 ("Wilderness Bound")**, complete with Docker dev/server containerization!

---

## 🌟 Features

- 🎯 **Drop & Guess Gameplay**:
  - Teleports players to a mystery Overworld location within a configurable radius (e.g. ±10,000 blocks).
  - 5 rounds per game with customizable round limits and radii.
  - Safe surface-detection ensures you land safely on ground instead of inside caves or suffocation blocks.
- 🚫 **Anti-Cheat Coordinate Masking**:
  - Automatically activates `EntityEvent.REDUCED_DEBUG_INFO` upon starting, preventing coordinate reading in F3.
  - Automatically restores full debug info upon game end or forfeit.
- 📊 **Exponential Decay Scoring System**:
  - Implements the authentic GeoGuessr formula: $\text{Score} = \text{round}(5000 \cdot e^{-D / \text{scale}})$.
  - Max **5,000 points** per round (25,000 perfect score for 5 rounds).
  - Perfect score within 20 blocks; gradual score curve up to the world boundary.
- 🧭 **In-Game Compass & Client GUI**:
  - Custom item `geoguessr:compass` (right-click to open guess prompt).
  - Client GUI Screen opened via keybind `[G]`: type coordinates or biomes and submit with 1 click.
- 🌲 **Multiple Game Modes**:
  - `COORDINATES`: Classic distance-based pinpointing.
  - `BIOME`: Guess the mystery biome (supports exact match & related biome categories).
  - `LANDMARK`: Identify world structures and points of interest.
- ⏱️ **Live Actionbar HUD & Sound Effects**:
  - Real-time countdown timer (90s default per round).
  - Audio countdown ticks in final 5 seconds.
  - Triumph sound effects (`PLAYER_LEVELUP`, `UI_TOAST_CHALLENGE_COMPLETE`) and Title animations.
- 🏆 **Leaderboards & Rank System**:
  - Ranks: **Grandmaster Cartographer 🌟**, **Master Explorer 🧭**, **Seasoned Traveler 🗺️**, **Wandering Scout 🎒**, **Lost Adventurer ❓**.
  - Persistent leaderboard recording top scores and game stats.

---

## 🎮 Commands

| Command | Description | Example |
|---|---|---|
| `/geoguessr start` | Start a default game (5 rounds, ±10,000 blocks, coordinates) | `/geoguessr start` |
| `/geoguessr start <rounds> <radius> <mode>` | Start customized game session | `/geoguessr start 5 15000 coordinates` |
| `/geoguessr guess <x> <z>` | Submit your coordinate guess | `/geoguessr guess 1250 -3400` |
| `/geoguessr guess <biome>` | Submit your biome guess | `/geoguessr guess cherry_grove` |
| `/geoguessr skip` | Skip current round (scores 0 pts) | `/geoguessr skip` |
| `/geoguessr stop` | Forfeit current game and return to origin | `/geoguessr stop` |
| `/geoguessr leaderboard` | View top player high scores | `/geoguessr leaderboard` |
| `/geoguessr help` | In-game help and guide | `/geoguessr help` |

---

## 🛠️ Project Structure

```
mc-geoguessr/
├── Dockerfile                  # Multi-stage Docker build (builder, server, dev)
├── docker-compose.yml          # Compose config for dedicated server & dev container
├── docker-entrypoint.sh        # Server auto-setup (EULA, Fabric API, GeoGuessr mod)
├── docker-build.sh             # Docker build helper script
├── docker-run-server.sh        # Docker launch script
├── Makefile                    # Make targets (build, test, docker-server, clean)
├── build.gradle                # Gradle build script with Fabric Loom 1.19
├── gradle.properties           # Pinned Minecraft 26.3, Loader 0.20.1, Java 25
├── settings.gradle             # Project name and maven repos
├── .devcontainer/              # VS Code remote container setup
└── src/
    ├── main/
    │   ├── java/com/geoguessr/
    │   │   ├── GeoGuessrMod.java           # Mod entrypoint & event registrations
    │   │   ├── command/GeoGuessrCommand.java # Brigadier commands tree
    │   │   ├── game/GameMode.java          # Game modes enum
    │   │   ├── game/GeoGuessrGame.java     # Per-player game session model
    │   │   ├── game/GeoGuessrManager.java  # Core game loop, scoring & timer
    │   │   └── item/GeoGuessrCompassItem.java # Custom interactive compass item
    │   └── resources/
    │       ├── fabric.mod.json             # Fabric metadata
    │       └── assets/geoguessr/           # Lang (en_us, zh_cn), icons, models
    ├── client/java/com/geoguessr/client/
    │   ├── GeoGuessrModClient.java         # Client initializer & 'G' keymapping
    │   └── gui/GeoGuessrGuessScreen.java   # In-game interactive coordinate GUI
    └── test/java/com/geoguessr/test/
        └── GeoGuessrLogicTest.java         # JUnit 5 logic & scoring tests
```

---

## 🐳 Docker Deployment & Development

### 1. Run Dedicated Server via Docker Compose

```bash
docker compose up -d server
```

The container will:
1. Automatically accept the Minecraft EULA.
2. Download Fabric Server Launcher for Minecraft 26.3.
3. Install the compiled `geoguessr-1.0.0.jar` and `Fabric API` into `mods/`.
4. Start the server on port `25565`.

### 2. Connect to Containerized Dev Environment

```bash
docker compose run --rm dev
```

Or open the repository in **VS Code** with the **Dev Containers** extension (`.devcontainer/devcontainer.json`).

---

## 💻 Local Building & Testing

Requires Java 25 LTS:

```bash
# Build the mod JAR
./gradlew build

# Run unit tests
./gradlew test

# Launch dev test server
./gradlew runServer

# Clean build artifacts
./gradlew clean
```

The output JAR is generated at: `build/libs/geoguessr-1.0.0.jar`.
