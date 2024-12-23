package com.fpetrola.z80.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpriteViewer extends JPanel {
    private final List<Sprite> sprites = new ArrayList<>();
    private double zoomFactor = 1.0;
    private double offsetX = 0; // X offset for panning
    private double offsetY = 0; // Y offset for panning
    private boolean showGrid = true;

    public SpriteViewer() {
        setPreferredSize(new Dimension(800, 600));

        // Mouse interaction
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int lastX, lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                offsetX += dx / zoomFactor; // Adjust panning by zoom factor
                offsetY += dy / zoomFactor;
                lastX = e.getX();
                lastY = e.getY();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double oldZoomFactor = zoomFactor;
                zoomFactor *= e.getPreciseWheelRotation() < 0 ? 1.1 : 0.9;
                zoomFactor = Math.max(0.1, zoomFactor); // Prevent too small zoom

                // Adjust offsets to maintain cursor position during zoom
                double zoomAdjustment = zoomFactor / oldZoomFactor;
                int mouseX = e.getX();
                int mouseY = e.getY();
                offsetX = mouseX / zoomFactor - (mouseX - offsetX * oldZoomFactor) / oldZoomFactor / zoomAdjustment;
                offsetY = mouseY / zoomFactor - (mouseY - offsetY * oldZoomFactor) / oldZoomFactor / zoomAdjustment;

                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }

    // Add a sprite to the viewer
    public void addRandomSprite() {
        Random random = new Random();
        int size = random.nextInt(16) + 8; // Sprite size between 8x8 and 24x24
        int x = random.nextInt(200) - 100; // Random X coordinate (offset for variety)
        int y = random.nextInt(200) - 100; // Random Y coordinate
        BufferedImage spriteImage = createSampleSprite(size, size);
        sprites.add(new Sprite(spriteImage, x, y));
        repaint();
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply panning and zoom
        g2d.translate(offsetX * zoomFactor, offsetY * zoomFactor);
        g2d.scale(zoomFactor, zoomFactor);

        // Draw sprites
        for (Sprite sprite : sprites) {
            g2d.drawImage(sprite.image, sprite.x, sprite.y, null);
        }

        // Draw grid
        if (showGrid) {
            drawGrid(g2d);
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        int width = getWidth();
        int height = getHeight();
        int gridSize = 1; // Pixel grid size
        for (int x = 0; x < width / zoomFactor; x += gridSize) {
            g2d.drawLine(x, 0, x, (int) (height / zoomFactor));
        }
        for (int y = 0; y < height / zoomFactor; y += gridSize) {
            g2d.drawLine(0, y, (int) (width / zoomFactor), y);
        }
    }

    // Generate a sample sprite with a random pattern
    private BufferedImage createSampleSprite(int width, int height) {
        BufferedImage sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random();
        Color color1 = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        Color color2 = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sprite.setRGB(x, y, (x + y) % 2 == 0 ? color1.getRGB() : color2.getRGB());
            }
        }
        return sprite;
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
