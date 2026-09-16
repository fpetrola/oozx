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

package com.fpetrola.oozx.speccy.desktop;

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
 * A game's screenshots, drawn at whatever size the space allows: two side by side in a row of
 * search results, one on its own in a tile of the gallery.
 * <p>
 * They used to be icons in labels, and an icon fixes a label's size at the pixels it happens to
 * have: widening the window moved the empty space around rather than the picture. This asks its
 * parent how wide the row is and paints into that, so the pictures follow the window.
 * <p>
 * Nearest neighbour on purpose. These are 256x192 loading screens, and interpolating them turns
 * the pixel art to mush; scaled hard they stay sharp, which is how a Spectrum screen is meant to
 * look enlarged.
 */
public class Screenshots extends JComponent {

  /** What a Spectrum screen is, and so the shape to reserve before an image has arrived. */
  private static final int SCREEN_WIDTH = 256;
  private static final int SCREEN_HEIGHT = 192;
  private static final int GAP = 10;

  private final BufferedImage[] shots;
  private final boolean[] coming;
  private boolean anyFailed;

  public Screenshots(MouseAdapter mouseAdapter, String... urls) {
    setOpaque(false);
    shots = new BufferedImage[Math.max(1, urls.length)];
    coming = new boolean[shots.length];
    if (mouseAdapter != null) addMouseListener(mouseAdapter);
    for (int slot = 0; slot < urls.length; slot++) {
      show(slot, urls[slot]);
    }
  }

  /** Puts a picture in one of the slots, which for a game found on disk arrives after the tile. */
  public void show(int slot, String url) {
    if (url == null) {
      return;
    }
    coming[slot] = true;
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
    int available = getParent() == null
        ? SCREEN_WIDTH * shots.length + GAP * (shots.length - 1) : getParent().getWidth();
    return new Dimension(available, heightOf(widthOfEach(available)));
  }

  private int widthOfEach(int available) {
    return Math.max(1, (available - GAP * (shots.length - 1)) / shots.length);
  }

  private int heightOf(int width) {
    for (BufferedImage sample : shots) {
      if (sample != null) {
        return width * sample.getHeight() / sample.getWidth();
      }
    }
    return width * SCREEN_HEIGHT / SCREEN_WIDTH;
  }

  @Override
  protected void paintComponent(Graphics g) {
    int each = widthOfEach(getWidth());

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
    // Saying "loading..." about a game nobody is fetching a picture for is a wait that never ends.
    int slot = Math.min(coming.length - 1, x / Math.max(1, width + GAP));
    String message = coming[slot] && !anyFailed ? "loading..." : "no screenshot";
    g2.drawString(message, x + 8, height / 2);
  }
}
