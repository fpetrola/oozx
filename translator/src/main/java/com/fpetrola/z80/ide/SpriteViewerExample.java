package com.fpetrola.z80.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SpriteViewerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ZX Spectrum Sprite Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create the sprite viewer
            SpriteViewer spriteViewer = new SpriteViewer();

            // Create example sprites
            BufferedImage sprite1 = createSprite(16, 16, Color.BLUE, Color.WHITE);
            BufferedImage sprite2 = createSprite(16, 16, Color.GREEN, Color.BLACK);

            // Create a control panel
            JPanel controlPanel = new JPanel();
            JButton zoomInButton = new JButton("Zoom In");
            JButton zoomOutButton = new JButton("Zoom Out");
            JButton addSprite1Button = new JButton("Add Sprite 1");
            JButton addSprite2Button = new JButton("Add Sprite 2");
            JButton toggleGridButton = new JButton("Toggle Grid");
            JButton clearSpritesButton = new JButton("Clear Sprites");

            // Zoom controls
            zoomInButton.addActionListener(e -> spriteViewer.setZoomFactor(spriteViewer.zoomFactor * 1.2));
            zoomOutButton.addActionListener(e -> spriteViewer.setZoomFactor(spriteViewer.zoomFactor / 1.2));

            // Add sprite controls
            addSprite1Button.addActionListener(e -> spriteViewer.addSprite(sprite1, 50, 50));
            addSprite2Button.addActionListener(e -> spriteViewer.addSprite(sprite2, 100, 100));

            // Grid and clear controls
            toggleGridButton.addActionListener(e -> spriteViewer.toggleGrid());
            clearSpritesButton.addActionListener(e -> spriteViewer.clearSprites());

            // Add buttons to the control panel
            controlPanel.add(zoomInButton);
            controlPanel.add(zoomOutButton);
            controlPanel.add(addSprite1Button);
            controlPanel.add(addSprite2Button);
            controlPanel.add(toggleGridButton);
            controlPanel.add(clearSpritesButton);

            // Add components to the frame
            frame.setLayout(new BorderLayout());
            frame.add(spriteViewer, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setSize(600, 600);
            frame.setVisible(true);
        });
    }

    // Utility to create a simple sprite
    private static BufferedImage createSprite(int width, int height, Color color1, Color color2) {
        BufferedImage sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sprite.setRGB(x, y, (x + y) % 2 == 0 ? color1.getRGB() : color2.getRGB());
            }
        }
        return sprite;
    }
}
