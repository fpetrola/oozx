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
 * A DAC on a port: whatever byte the program last wrote, as a level in the mix. The Covox and
 * the SpecDrum are one of these each, and the Currah uSpeech's chip feeds one at its own pace.
 * Flat, like Fuse's three: no speaker between it and the mixer.
 */
public class Dac implements AudioSource {

  private final int volume;
  private BlipSynth synth;
  private int[] scratch;
  private int level;

  public Dac(AudioOutput output, int volumePercent) {
    this.volume = volumePercent;
    takeOutputFrom(output);
  }

  @Override
  public void takeOutputFrom(AudioOutput output) {
    synth = output.newFlatSynth(volume);
    scratch = new int[output.frameSize() * 2];
  }

  /** The level from now on, in the synth's units: a byte times 128, centred or not as the board has it. */
  public void write(long tstates, int level) {
    this.level = level;
    synth.update(tstates, level);
  }

  /** What it is putting out, for a meter to show. */
  public int level() {
    return level;
  }

  @Override
  public void endFrame(int frameTstates) {
    synth.endFrame(frameTstates);
  }

  @Override
  public int mixInto(int[] samples, int frames) {
    int count = synth.readSamples(scratch, frames, true);
    for (int i = 0; i < count; i++) {
      samples[i * 2] += scratch[i * 2];
      samples[i * 2 + 1] += scratch[i * 2];
    }
    return count;
  }

  @Override
  public void close() {
    synth.close();
  }
}
