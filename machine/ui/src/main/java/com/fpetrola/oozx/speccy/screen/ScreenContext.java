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

package com.fpetrola.oozx.speccy.screen;

import com.fpetrola.oozx.speccy.screen.TvScreen;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

/**
 * The room a frame is worked on in, kept between frames and belonging to one window.
 * <p>
 * Fifty frames a second, each a quarter of a megabyte, is not a thing to ask a garbage collector
 * to absorb when the same buffers will do. Every effect that needs somewhere to put its result
 * asks here for one of the right size and gets the same one back next frame.
 */
public class ScreenContext {

  private final Map<String, BufferedImage> buffers = new HashMap<>();
  private final TvScreen.Scratch tvScratch = new TvScreen.Scratch();
  private long frame;

  /** Where the picture is going, so an effect can know the size it is being scaled to. */
  private int targetWidth = 1;
  private int targetHeight = 1;

  public void beginFrame(int targetWidth, int targetHeight) {
    this.targetWidth = Math.max(1, targetWidth);
    this.targetHeight = Math.max(1, targetHeight);
    frame++;
  }

  public int targetWidth() {
    return targetWidth;
  }

  public int targetHeight() {
    return targetHeight;
  }

  /** Which frame this is, for the effects that remember - persistence, and snow. */
  public long frame() {
    return frame;
  }

  public TvScreen.Scratch tvScratch() {
    return tvScratch;
  }

  /**
   * A buffer of the given size, the same one every frame.
   * <p>
   * Named, because an effect that reads one buffer and writes another needs the two to be
   * different buffers, and both to still be there next frame.
   */
  public BufferedImage buffer(String name, int width, int height) {
    BufferedImage held = buffers.get(name);
    if (held == null || held.getWidth() != width || held.getHeight() != height) {
      held = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
      buffers.put(name, held);
    }
    return held;
  }

  /**
   * An image's own pixels, to be written through.
   * <p>
   * Only for the buffers handed out above, which are all of a type that keeps its pixels as ints.
   */
  public static int[] pixelsOf(BufferedImage image) {
    return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
  }

  /**
   * An image's pixels to read, whatever it is made of.
   * <p>
   * The pictures inside the pipeline are all ours and keep their pixels as ints, so this hands
   * back the array itself and costs nothing. One from somewhere else - a file, a screenshot, a
   * test - is copied out instead, which is slower and does not throw, and a scaler asked to
   * enlarge a picture should not care where it came from.
   */
  public static int[] readablePixels(BufferedImage image) {
    if (image.getRaster().getDataBuffer() instanceof DataBufferInt ints
        && (image.getType() == BufferedImage.TYPE_INT_RGB
        || image.getType() == BufferedImage.TYPE_INT_ARGB)) {
      return ints.getData();
    }
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }
}
