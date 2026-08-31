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

package com.fpetrola.oozx.speccy.sound;

/**
 * Something a machine makes a noise with, asked once a frame.
 * <p>
 * The seam is the frame and not the sample, deliberately. Fifty frames a second over a handful of
 * sources is a couple of hundred calls; inside the chip's tick loop the same call would run a
 * hundred and ten thousand times a second, and a measured nine hundredths of one per cent would
 * stop being free.
 */
public interface AudioSource {

  /** Close this source's frame at the machine's frame length. */
  void endFrame(int frameTstates);

  /**
   * Adds what this source made into the mix, which is already zeroed and may already hold others.
   * <p>
   * The mix is interleaved - left at even indices, right at odd - and a source writes both. Most
   * write the same to each. A sound chip whose channels are placed left and right does not, and
   * that placement is inside the chip, where nothing else can see the channels it is placing.
   *
   * @return how many stereo frames it wrote
   */
  int mixInto(int[] samples, int frames);

  default void close() {
  }
}
