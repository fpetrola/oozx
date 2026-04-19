/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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
package com.fpetrola.oozx.speccy.devices;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/** A row of segments that light up to a level, and fall back slowly, the way a VU meter does. */
public class LevelMeter extends JComponent {

  private static final int SEGMENTS = 20;
  private double shown;

  public LevelMeter() {
    setPreferredSize(new Dimension(160, 16));
  }

  /** @param level 0 to 1 */
  public void show(double level) {
    shown = Math.max(Math.min(1, level), shown - 0.08);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics pen) {
    int wide = getWidth() / SEGMENTS;
    int lit = (int) Math.round(shown * SEGMENTS);
    for (int i = 0; i < SEGMENTS; i++) {
      pen.setColor(i < lit ? (i < SEGMENTS * 3 / 4 ? new Color(0x30c030) : i < SEGMENTS - 2 ? new Color(0xe0c020) : new Color(0xd02020))
          : getBackground().darker());
      pen.fillRect(i * wide, 2, wide - 2, getHeight() - 4);
    }
  }
}
