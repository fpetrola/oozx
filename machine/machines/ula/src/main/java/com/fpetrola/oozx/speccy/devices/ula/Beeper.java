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

package com.fpetrola.oozx.speccy.devices.ula;

import com.fpetrola.oozx.speccy.modules.sound.AudioOutput;
import com.fpetrola.oozx.speccy.modules.sound.AudioSource;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;

/**
 * Port 0xFE bit 4: the beeper every Spectrum model shares. Driven by both program writes and
 * tape playback (making loading audible); while a tape plays, only its signal passes through,
 * except on a Timex, whose port keeps the two separate.
 */
public class Beeper implements AudioSource {

  private static final int AMPL_BEEPER = 50 * 256;
  private static final int AMPL_TAPE = 2 * 256;

  /** Lookup table indexed by the combined tape-bit/program-bit value. */
  private static final int[] LEVELS = {0, AMPL_TAPE, AMPL_BEEPER, AMPL_BEEPER + AMPL_TAPE};

  private final Tape tape;
  private final Sound.Output soundOutput;
  private BlipSynth synth;
  private int[] scratch;

  /** Output volume, owned entirely by this class. */
  private int volume = 100;

  public Beeper(AudioOutput output, Tape tape, Sound.Output soundOutput) {
    this.tape = tape;
    this.soundOutput = soundOutput;
    takeOutputFrom(output);
  }

  @Override
  public void takeOutputFrom(AudioOutput output) {
    synth = output.newSynth(volume);
    scratch = new int[output.frameSize() * 2];
  }

  /**
   * @param bits  combined tape and program bits as passed by the ULA
   * @param separateTapeBit whether this model's port keeps tape and speaker bits distinct
   */
  public void write(long tstates, int bits, boolean separateTapeBit) {
    if (tape.isTapePlaying()) {
      if (!soundOutput.whileLoading || !separateTapeBit) {
        bits &= 0x02;
      }
    } else if (bits == 1) {
      bits = 0;
    }
    synth.update(tstates, LEVELS[bits]);
  }

  @Override
  public void endFrame(int frameTstates) {
    synth.endFrame(frameTstates);
  }

  @Override
  public int mixInto(int[] samples, int frames) {
    int count = synth.readSamples(scratch, frames, true);
    for (int i = 0; i < count; i++) {
      // Mono output duplicated to both stereo channels.
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
