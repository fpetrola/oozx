/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.ide;

import javax.swing.*;
import java.awt.*;

public class SpriteViewerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ZX Spectrum Sprite Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create the sprite viewer
            SpriteViewer spriteViewer = new SpriteViewer(16, 16);

            // Create a control panel
            JPanel controlPanel = new JPanel();
            JButton zoomInButton = new JButton("Zoom In");
            JButton zoomOutButton = new JButton("Zoom Out");
            JButton rotateButton = new JButton("Rotate");

            // Zoom controls
            zoomInButton.addActionListener(e -> spriteViewer.setZoomFactor(spriteViewer.zoomFactor * 1.2));
            zoomOutButton.addActionListener(e -> spriteViewer.setZoomFactor(spriteViewer.zoomFactor / 1.2));

            // Rotation control
            rotateButton.addActionListener(e -> spriteViewer.rotateSprite());

            // Add buttons to the control panel
            controlPanel.add(zoomInButton);
            controlPanel.add(zoomOutButton);
            controlPanel.add(rotateButton);

            // Add components to the frame
            frame.setLayout(new BorderLayout());
            frame.add(spriteViewer, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setSize(600, 600);
            frame.setVisible(true);
        });
    }
}
