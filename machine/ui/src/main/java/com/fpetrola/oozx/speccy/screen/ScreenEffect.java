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

import com.fpetrola.oozx.plugins.Plugin;

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
 * <p>
 * One that arrives in a jar is built with nothing - it is found, not configured - so whatever it
 * needs to know it reads for itself, and it says with {@link #when} which side of the scaler it
 * belongs on. The ones this build carries are made from the knobs instead.
 */
@Plugin("effect")
@dev.crystal.plugins.api.RoleInterface
public interface ScreenEffect {

  /**
   * Which side of the scaler this belongs on, which is the only thing about the order that is
   * the effect's own: what happens to the picture the machine drew, and what happens to the
   * screen it lands on. A tint is the first, a shadow mask the second.
   */
  enum When {
    ON_THE_PICTURE, ON_THE_SCREEN
  }

  /** Where this goes. On the screen, unless it is about the picture itself. */
  default When when() {
    return When.ON_THE_SCREEN;
  }

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

  /**
   * The knobs this effect brings with it, which the settings window offers beside its own.
   * <p>
   * An effect that arrived is built with nothing, so this is how it is configured at all: what
   * it reads and writes is its own, and being a knob is what makes it appear in the window, be
   * written down with the rest and come back tomorrow.
   */
  default java.util.List<Knob> knobs() {
    return java.util.List.of();
  }

  /** Whether this is doing anything at all, so a pipeline can leave it out rather than run it. */
  default boolean isTransparent() {
    return false;
  }
}
