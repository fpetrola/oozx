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
