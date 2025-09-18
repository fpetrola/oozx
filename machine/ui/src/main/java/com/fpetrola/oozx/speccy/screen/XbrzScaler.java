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
 * IT IS NOT UNDER THIS PROJECT'S LICENCE. The algorithm is Zenju's, under the GNU General Public
 * License v3; this uses Stanio's Java port of it, whose linking exception is what lets an Apache
 * 2.0 project depend on it. Which means: it stays an unmodified dependency and is never copied
 * into this tree, a modification to it would be under the GPL, and whatever ships from here
 * carries the library's licence and attribution along. The scalers in {@link Scalers} are this
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
