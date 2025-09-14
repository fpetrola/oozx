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

import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;

/**
 * Where a source gets what it needs to make samples: a synth wired for the machine and the speed
 * that are running, and how much of a frame there is to fill.
 * <p>
 * Two methods, so that a thing making a noise depends on somewhere to make it and not on the whole
 * mixer. A peripheral holding one of these can be plugged in and pulled out without the rest of
 * the emulator having anything to say about it.
 */
public interface AudioOutput {

  /**
   * @param volumePercent how loud this source wants to be, which is the source's own business
   */
  BlipSynth newSynth(int volumePercent);

  /** The same, with no speaker's colouring: a DAC goes to the mixer as it is. */
  BlipSynth newFlatSynth(int volumePercent);

  /** How many stereo frames one frame of output holds. */
  int frameSize();
}
