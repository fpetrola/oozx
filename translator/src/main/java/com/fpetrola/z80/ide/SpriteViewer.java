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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class SpriteViewer extends JPanel {
    private BufferedImage spriteImage; // The sprite as an image
    public double zoomFactor = 1.0;   // Zoom level
    private int rotationAngle = 0;    // Rotation angle in degrees

    public SpriteViewer(int width, int height) {
        setPreferredSize(new Dimension(400, 400));
        spriteImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill the sprite with a default pattern
        Graphics2D g = spriteImage.createGraphics();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                g.setColor((x + y) % 2 == 0 ? Color.BLACK : Color.WHITE);
                g.fillRect(x, y, 1, 1);
            }
        }
        g.dispose();
    }

    public void setSpriteData(int[][] spriteData, int pixelSize) {
        int width = spriteData[0].length;
        int height = spriteData.length;
        spriteImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = spriteData[y][x] == 1 ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                spriteImage.setRGB(x, y, color);
            }
        }
        repaint();
    }

    public void setZoomFactor(double zoom) {
        this.zoomFactor = Math.max(0.1, zoom); // Prevent zero or negative zoom
        repaint();
    }

    public void rotateSprite() {
        rotationAngle = (rotationAngle + 90) % 360;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply zoom and rotation
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        g2d.translate(centerX, centerY);
        g2d.scale(zoomFactor, zoomFactor);
        g2d.rotate(Math.toRadians(rotationAngle));
        g2d.translate(-spriteImage.getWidth() / 2, -spriteImage.getHeight() / 2);

        // Draw the sprite
        g2d.drawImage(spriteImage, 0, 0, null);
        g2d.dispose();
    }
}
