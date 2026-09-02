/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
