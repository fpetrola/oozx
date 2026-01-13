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
package com.fpetrola.oozx.speccy.modules.sound;

import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;

/**
 * A DAC on a port: whatever byte the program last wrote, as a level in the mix. The Covox and
 * the SpecDrum are one of these each, and the Currah uSpeech's chip feeds one at its own pace.
 * Flat: no speaker between it and the mixer.
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
