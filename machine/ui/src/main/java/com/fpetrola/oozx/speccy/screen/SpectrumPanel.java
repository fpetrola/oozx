/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.screen;


import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SpectrumPanel extends JPanel {

  private BufferedImage image;

  public SpectrumPanel(int w, int h) {
    this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    setPreferredSize(new Dimension(w, h));
  }


  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (image != null) {
      g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
  }
}
