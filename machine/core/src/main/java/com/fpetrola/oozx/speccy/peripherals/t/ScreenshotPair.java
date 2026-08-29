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

package com.fpetrola.oozx.speccy.peripherals.t;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * A game's two screenshots, drawn at whatever size the row is.
 * <p>
 * They used to be icons in labels, and an icon fixes a label's size at the pixels it happens to
 * have: widening the window moved the empty space around rather than the picture. This asks its
 * parent how wide the row is and paints into that, so the pictures follow the window.
 * <p>
 * Nearest neighbour on purpose. These are 256x192 loading screens, and interpolating them turns
 * the pixel art to mush; scaled hard they stay sharp, which is how a Spectrum screen is meant to
 * look enlarged.
 */
public class ScreenshotPair extends JComponent {

  /** What a Spectrum screen is, and so the shape to reserve before an image has arrived. */
  private static final int SCREEN_WIDTH = 256;
  private static final int SCREEN_HEIGHT = 192;
  private static final int GAP = 10;

  private final BufferedImage[] shots = new BufferedImage[2];
  private boolean anyFailed;

  public ScreenshotPair(String first, String second, MouseAdapter mouseAdapter) {
    setOpaque(false);
    if (mouseAdapter != null) addMouseListener(mouseAdapter);
    load(0, first);
    load(1, second);
  }

  private void load(int slot, String url) {
    if (url == null) return;
    new SwingWorker<BufferedImage, Void>() {
      protected BufferedImage doInBackground() throws Exception {
        return ImageIO.read(new URL(url));
      }

      protected void done() {
        try {
          shots[slot] = get();
        } catch (Exception e) {
          anyFailed = true;
        }
        // The height depends on the picture's shape, so the row has to be measured again.
        revalidate();
        repaint();
      }
    }.execute();
  }

  /**
   * As wide as the row allows, and as tall as that width makes the pictures. Asking the parent
   * is what ties the two together: Swing hands out preferred sizes before it assigns bounds, so
   * a component that wants to grow with its container has to look at the container.
   */
  @Override
  public Dimension getPreferredSize() {
    int available = getParent() == null ? SCREEN_WIDTH * 2 + GAP : getParent().getWidth();
    int each = Math.max(1, (available - GAP) / 2);
    return new Dimension(available, heightOf(each));
  }

  private int heightOf(int width) {
    BufferedImage sample = shots[0] != null ? shots[0] : shots[1];
    if (sample == null) return width * SCREEN_HEIGHT / SCREEN_WIDTH;
    return width * sample.getHeight() / sample.getWidth();
  }

  @Override
  protected void paintComponent(Graphics g) {
    int each = Math.max(1, (getWidth() - GAP) / 2);

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    for (int slot = 0; slot < shots.length; slot++) {
      int x = slot * (each + GAP);
      BufferedImage shot = shots[slot];
      if (shot != null) {
        g2.drawImage(shot, x, 0, each, heightOf(each), null);
      } else {
        paintPlaceholder(g2, x, each);
      }
    }
    g2.dispose();
  }

  private void paintPlaceholder(Graphics2D g2, int x, int width) {
    int height = heightOf(width);
    g2.setColor(new Color(0, 0, 0, 20));
    g2.fillRect(x, 0, width, height);
    g2.setColor(Color.GRAY);
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
    String message = anyFailed ? "no screenshot" : "loading...";
    g2.drawString(message, x + 8, height / 2);
  }
}
