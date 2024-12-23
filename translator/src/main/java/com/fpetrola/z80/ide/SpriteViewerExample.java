package com.fpetrola.z80.ide;

import javax.swing.*;
import java.awt.*;

public class SpriteViewerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ZX Spectrum Sprite Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SpriteViewer spriteViewer = new SpriteViewer();

            // Add controls for random sprites and grid toggle
            JButton addSpriteButton = new JButton("Add Random Sprite");
            addSpriteButton.addActionListener(e -> spriteViewer.addRandomSprite());

            JButton toggleGridButton = new JButton("Toggle Grid");
            toggleGridButton.addActionListener(e -> spriteViewer.toggleGrid());

            JPanel controlPanel = new JPanel();
            controlPanel.add(addSpriteButton);
            controlPanel.add(toggleGridButton);

            frame.setLayout(new BorderLayout());
            frame.add(new JScrollPane(spriteViewer), BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setSize(800, 600);
            frame.setVisible(true);
        });
    }
}
