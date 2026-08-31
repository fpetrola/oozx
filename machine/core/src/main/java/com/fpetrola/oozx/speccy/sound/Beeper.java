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

import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.sound.blip.BlipSynth;

/**
 * The one bit of one port that every Spectrum can make a noise with.
 * <p>
 * Two things reach the speaker through it: what a program writes to bit 4 of port 0xFE, and what
 * the tape is playing, which is why loading is audible. A tape drowns the program out - while one
 * is playing only its own bit is let through, unless the machine is a Timex, whose port does not
 * work that way.
 */
public class Beeper implements AudioSource {

  private static final int AMPL_BEEPER = 50 * 256;
  private static final int AMPL_TAPE = 2 * 256;

  /** Indexed by the two bits that reach it: the tape's, and the program's. */
  private static final int[] LEVELS = {0, AMPL_TAPE, AMPL_BEEPER, AMPL_BEEPER + AMPL_TAPE};

  private final Tape tape;
  private final Settings settings;
  private BlipSynth synth;
  private int[] scratch;

  /** How loud a speaker is, which is the speaker's business and nobody else's. */
  private int volume = 100;

  public Beeper(AudioOutput output, Tape tape, Settings settings) {
    this.tape = tape;
    this.settings = settings;
    takeOutputFrom(output);
  }

  @Override
  public void takeOutputFrom(AudioOutput output) {
    synth = output.newSynth(volume);
    scratch = new int[output.frameSize() * 2];
  }

  /**
   * @param bits  the tape's bit and the program's, as the ULA hands them over
   * @param timex whether this machine's port keeps the two apart, which a Timex does not
   */
  public void write(long tstates, int bits, boolean timex) {
    if (tape.isTapePlaying()) {
      if (!settings.current.soundLoad || timex) {
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
      // One speaker, so both ears get it.
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
