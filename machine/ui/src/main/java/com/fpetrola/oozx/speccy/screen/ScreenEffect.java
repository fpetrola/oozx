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

import java.awt.image.BufferedImage;

/**
 * One thing done to the picture on its way from the machine to the window.
 * <p>
 * A television lead, a scaler, a shadow mask, a tint: each of them takes the picture as it stands
 * and hands back the picture with its own thing done. What none of them do is know about each
 * other - the order they run in belongs to {@link ScreenPipeline}, and adding one is writing one
 * class, not editing a chain of if statements that grows a branch per effect.
 * <p>
 * An effect may hand back the image it was given, having changed it in place, or a different one
 * of a different size. Scalers do the second; most of the rest do the first, because a frame is a
 * quarter of a megabyte and there are fifty of them a second.
 */
public interface ScreenEffect {

  /** What this is called where someone picks it. */
  String label();

  /**
   * The picture with this effect done to it.
   *
   * @param picture what has been made of the frame so far
   * @param context the room to work in, and what is known about where this is going
   * @return the result, which may be the same image or a new one
   */
  BufferedImage apply(BufferedImage picture, ScreenContext context);

  /** Whether this is doing anything at all, so a pipeline can leave it out rather than run it. */
  default boolean isTransparent() {
    return false;
  }
}
