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

import io.github.stanio.xbrz.Xbrz;

import java.awt.image.BufferedImage;

/**
 * xBRZ, which is the one that actually rounds a diagonal off.
 * <p>
 * The scalers written here choose among the colours already on the screen, which keeps a picture
 * honest and leaves a diagonal a staircase of smaller steps. xBRZ looks at a wider neighbourhood,
 * works out which way an edge is running, and draws the slope - so a diagonal comes out a line
 * and a curve comes out curved. Measured on a frame of Ping Pong it turned nine colours into
 * sixty-eight, which is the cost of doing that, and took 2.3 ms a frame against Scale2x's 2.1.
 * <p>
 * IT IS SOMEBODY ELSE'S WORK, under this project's own licence. The algorithm is Zenju's,
 * under the GNU General Public License v3, and this uses Stanio's Java port of it, so the
 * GPL's terms are all that depending on it needs. Which means: it stays an unmodified
 * dependency and is never copied into this tree, and whatever ships from here carries the
 * library's licence and attribution along. The scalers in {@link Scalers} are this
 * project's own and carry none of that, and are staying.
 */
public class XbrzScaler implements Scaler {

  private final int factor;
  private final Xbrz xbrz;
  private int[] enlarged = new int[0];

  public XbrzScaler(int factor) {
    this.factor = factor;
    // No alpha: the pictures here come from a machine with fifteen opaque colours, and telling
    // it so is one less thing for it to work out per pixel.
    this.xbrz = new Xbrz(factor, false);
  }

  @Override
  public String label() {
    return "xBRZ " + factor + "x";
  }

  @Override
  public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
    int sourceWidth = picture.getWidth(), sourceHeight = picture.getHeight();
    int[] source = ScreenContext.readablePixels(picture);
    int needed = sourceWidth * factor * sourceHeight * factor;
    if (enlarged.length != needed) {
      enlarged = new int[needed];
    }
    xbrz.scaleImage(source, enlarged, sourceWidth, sourceHeight);

    BufferedImage out = context.buffer("scaled", width, height);
    // The same last step the other whole-number scalers take: what xBRZ decided on is the shape,
    // and averaging it down to the window would undo the deciding.
    Scaler.repeat(enlarged, sourceWidth * factor, sourceHeight * factor,
        ScreenContext.pixelsOf(out), width, height);
    return out;
  }
}
