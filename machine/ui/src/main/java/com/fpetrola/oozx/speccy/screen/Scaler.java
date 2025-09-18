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

import java.awt.image.BufferedImage;

/**
 * How a picture of 256 by 192 becomes one that fills a window.
 * <p>
 * There is no neutral way to do it. Repeating pixels keeps every edge exactly where the machine
 * put it and makes a mess of any scale that is not a whole number; averaging them is smooth at
 * any scale and turns eight-pixel letters to mush; the pixel-art scalers guess at the shapes the
 * pixels were drawn to suggest, and are right often enough to be worth it and wrong in ways that
 * are their own. Which of those someone wants is a matter of taste, so all of them are here and
 * each one does its own thing behind the same call.
 */
public interface Scaler extends ScreenEffect {

  /**
   * The picture at the size it is going to be shown.
   * <p>
   * The target is whatever the window makes room for, aspect already worked out by the caller, so
   * a scaler that works in whole steps takes as many as fit and hands the rest to a plain resize.
   */
  BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context);

  @Override
  default BufferedImage apply(BufferedImage picture, ScreenContext context) {
    return scale(picture, context.targetWidth(), context.targetHeight(), context);
  }

  /**
   * Copies a picture into a bigger one by repeating pixels, whole numbers of them.
   * <p>
   * The step the pixel-art scalers are built out of, and a scaler in its own right.
   */
  static void repeat(int[] source, int sourceWidth, int sourceHeight,
                     int[] target, int targetWidth, int targetHeight) {
    for (int y = 0; y < targetHeight; y++) {
      int from = (int) ((long) y * sourceHeight / targetHeight) * sourceWidth;
      int to = y * targetWidth;
      for (int x = 0; x < targetWidth; x++) {
        target[to + x] = source[from + (int) ((long) x * sourceWidth / targetWidth)];
      }
    }
  }

  /** A pixel of a picture, with the edges standing in for what is past them. */
  static int at(int[] pixels, int width, int height, int x, int y) {
    int cx = x < 0 ? 0 : x >= width ? width - 1 : x;
    int cy = y < 0 ? 0 : y >= height ? height - 1 : y;
    return pixels[cy * width + cx];
  }
}
