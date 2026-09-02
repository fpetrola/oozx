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

import com.fpetrola.oozx.speccy.sound.blip.BlipSynth;

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
