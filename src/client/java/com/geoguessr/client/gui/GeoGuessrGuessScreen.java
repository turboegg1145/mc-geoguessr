package com.geoguessr.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GeoGuessrGuessScreen extends Screen {
    private EditBox xInput;
    private EditBox zInput;
    private EditBox biomeInput;

    public GeoGuessrGuessScreen() {
        super(Component.literal("Minecraft GeoGuessr - Submit Guess"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Coordinate X Input
        this.xInput = new EditBox(this.font, centerX - 100, centerY - 55, 200, 20, Component.literal("X Coordinate"));
        this.xInput.setHint(Component.literal("§7Enter X Coordinate (e.g. 1500)"));
        this.addRenderableWidget(this.xInput);

        // Coordinate Z Input
        this.zInput = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal("Z Coordinate"));
        this.zInput.setHint(Component.literal("§7Enter Z Coordinate (e.g. -2400)"));
        this.addRenderableWidget(this.zInput);

        // Submit Coordinate Button
        Button submitCoordBtn = Button.builder(Component.literal("§a✔ Submit Coordinate Guess"), btn -> {
            String xText = this.xInput.getValue().trim();
            String zText = this.zInput.getValue().trim();
            if (!xText.isEmpty() && !zText.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                try {
                    double x = Double.parseDouble(xText);
                    double z = Double.parseDouble(zText);
                    this.minecraft.player.connection.sendCommand("geoguessr guess " + x + " " + z);
                    this.onClose();
                } catch (NumberFormatException ignored) {}
            }
        }).bounds(centerX - 100, centerY - 5, 200, 20).build();
        this.addRenderableWidget(submitCoordBtn);

        // Biome Input
        this.biomeInput = new EditBox(this.font, centerX - 100, centerY + 25, 200, 20, Component.literal("Biome"));
        this.biomeInput.setHint(Component.literal("§7Or enter Biome (e.g. cherry_grove)"));
        this.addRenderableWidget(this.biomeInput);

        // Submit Biome Button
        Button submitBiomeBtn = Button.builder(Component.literal("§b🌱 Submit Biome Guess"), btn -> {
            String biomeText = this.biomeInput.getValue().trim();
            if (!biomeText.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand("geoguessr guess " + biomeText);
                this.onClose();
            }
        }).bounds(centerX - 100, centerY + 50, 200, 20).build();
        this.addRenderableWidget(submitBiomeBtn);

        // Close / Resume Button
        Button closeBtn = Button.builder(Component.literal("§c✖ Back to Game"), btn -> this.onClose())
                .bounds(centerX - 100, centerY + 80, 200, 20)
                .build();
        this.addRenderableWidget(closeBtn);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(extractor, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
