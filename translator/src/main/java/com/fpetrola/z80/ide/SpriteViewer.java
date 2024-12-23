package com.fpetrola.z80.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SpriteViewer extends JPanel {
    private final List<Sprite> sprites = new ArrayList<>();
    public double zoomFactor = 1.0;   // Zoom level
    private int gridSize = 10;        // Size of each grid cell
    private boolean showGrid = true; // Whether to show the grid

    public SpriteViewer() {
        setPreferredSize(new Dimension(400, 400));
    }

    // Add a sprite to the viewer
    public void addSprite(BufferedImage spriteImage, int x, int y) {
        sprites.add(new Sprite(spriteImage, x, y));
        repaint();
    }

    public void setZoomFactor(double zoom) {
        this.zoomFactor = Math.max(0.1, zoom); // Prevent zero or negative zoom
        repaint();
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        repaint();
    }

    public void clearSprites() {
        sprites.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply zoom
        g2d.scale(zoomFactor, zoomFactor);

        // Draw sprites
        for (Sprite sprite : sprites) {
            g2d.drawImage(sprite.image, sprite.x, sprite.y, null);
        }

        // Draw grid if enabled
        if (showGrid) {
            drawGrid(g2d);
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        int width = getWidth();
        int height = getHeight();
        int scaledGridSize = (int) (gridSize / zoomFactor);

        for (int x = 0; x < width / zoomFactor; x += scaledGridSize) {
            g2d.drawLine(x, 0, x, (int) (height / zoomFactor));
        }
        for (int y = 0; y < height / zoomFactor; y += scaledGridSize) {
            g2d.drawLine(0, y, (int) (width / zoomFactor), y);
        }
    }

    // Inner class to represent a sprite
    private static class Sprite {
        private final BufferedImage image;
        private final int x, y;

        public Sprite(BufferedImage image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }
}
