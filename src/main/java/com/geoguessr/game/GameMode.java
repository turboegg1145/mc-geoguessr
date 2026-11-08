package com.geoguessr.game;

public enum GameMode {
    COORDINATES("Coordinates"),
    BIOME("Biome"),
    LANDMARK("Landmark");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GameMode fromString(String str) {
        for (GameMode mode : values()) {
            if (mode.name().equalsIgnoreCase(str) || mode.displayName.equalsIgnoreCase(str)) {
                return mode;
            }
        }
        return COORDINATES;
    }
}
