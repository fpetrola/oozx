/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.modules.sound;

/**
 * Something a machine makes a noise with, asked once a frame.
 * <p>
 * The seam is the frame and not the sample, deliberately. Fifty frames a second over a handful of
 * sources is a couple of hundred calls; inside the chip's tick loop the same call would run a
 * hundred and ten thousand times a second, and a measured nine hundredths of one per cent would
 * stop being free.
 */
public interface AudioSource {

  /**
   * Take a synth from this output, and room to read samples into.
   * <p>
   * Called when the output is built and again whenever it is rebuilt, which happens on a change
   * of speed: a synth is made for a speed, and one made for the old one plays at the wrong rate.
   */
  void takeOutputFrom(AudioOutput output);

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
