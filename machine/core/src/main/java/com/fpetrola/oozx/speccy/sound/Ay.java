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

import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.sound.blip.BlipSynth;

import java.util.Arrays;

/**
 * The AY-3-8912, the chip a 128K has and a 48K does not.
 * <p>
 * Writes arrive with the T-state they happened at, because a tune is the changes and also when
 * each one was made: handed over without that, every note of a frame would begin at once. They
 * queue up, and once a frame the queue is walked alongside a clock that ticks the three tone
 * channels, the noise generator and the envelope, and what comes out of the mixer is turned into
 * samples.
 * <p>
 * Fuse gives each channel its own synth because it needs them apart to put two of them left and
 * one right. In mono the three are summed anyway, and a synth here owns its buffer rather than
 * sharing one, so there is one and the channels meet in it.
 */
public class Ay implements AudioSource {

  public static final int AMPL_AY_TONE = 24 * 256;

  private static final int AY_CHANGE_MAX = 8000;
  private static final int AY_CLOCK_DIVISOR = 16;
  private static final int AY_CLOCK_RATIO = 2;

  /** As published for the chip, and scaled the way Fuse scales them. */
  private static final int[] AY_TONE_LEVELS = {
      0x0000, 0x0385, 0x053D, 0x0770, 0x0AD7, 0x0FD5, 0x15B0, 0x230C,
      0x2B4C, 0x43C1, 0x5A4B, 0x732F, 0x9204, 0xAFF1, 0xD921, 0xFFFF
  };

  private static final int[] ayToneLevelsScaled = new int[16];

  static {
    for (int i = 0; i < 16; i++) {
      ayToneLevelsScaled[i] = (AY_TONE_LEVELS[i] * AMPL_AY_TONE + 0x8000) / 0xFFFF;
    }
  }

  private static class AyChange {

    long tstates;
    int reg, val;
  }

  private BlipSynth synth;
  private int[] scratch;

  private final byte[] ayRegisters = new byte[16];
  private final AyChange[] ayChanges = new AyChange[AY_CHANGE_MAX];
  private int ayChangeCount = 0;

  private int ayToneTick[] = new int[3];
  private int ayToneHigh[] = new int[3];
  private int ayTonePeriod[] = new int[3];
  private int ayNoiseTick, ayNoisePeriod;
  private int ayEnvTick, ayEnvInternalTick, ayEnvPeriod;
  private int ayToneCycles, ayEnvCycles;
  private int rng = 1;
  private boolean noiseToggle = false;

  /** How many times the chip has been written to, which is how you tell it is wired up. */
  public long writes;

  public Ay(Sound sound) {
    takeOutputFrom(sound);
    reset();
  }

  @Override
  public void takeOutputFrom(Sound sound) {
    synth = sound.newSynth(sound.volumeAY);
    scratch = new int[sound.frameSize() * 2];
  }

  public void write(int register, int value, long tstates) {
    writes++;
    if (ayChangeCount < AY_CHANGE_MAX) {
      if (ayChanges[ayChangeCount] == null) ayChanges[ayChangeCount] = new AyChange();
      AyChange ch = ayChanges[ayChangeCount++];
      ch.tstates = tstates;
      ch.reg = register & 15;
      ch.val = value;
    }
  }

  public void reset() {
    ayChangeCount = 0;
    Arrays.fill(ayRegisters, (byte) 0);
    Arrays.fill(ayTonePeriod, 1);
    Arrays.fill(ayToneTick, 0);
    Arrays.fill(ayTonePeriod, 1);
    Arrays.fill(ayToneHigh, 0);
    ayNoisePeriod = ayNoiseTick = 0;
    ayEnvPeriod = ayEnvTick = ayEnvInternalTick = 0;
    ayToneCycles = ayEnvCycles = 0;
    rng = 1;
    noiseToggle = false;
  }

  @Override
  public void endFrame(int frameTstates) {
    synthesise(frameTstates);
    synth.endFrame(frameTstates);
    // Emptied here, where the queue is read, rather than at the end of the mix: it belongs to
    // whoever consumes it, and leaving it to the caller is how it came to be cleared in a method
    // that had nothing else to do with the chip.
    ayChangeCount = 0;
  }

  @Override
  public int mixInto(int[] samples, int frames) {
    int count = synth.readSamples(scratch, frames, true);
    for (int i = 0; i < count; i++) {
      // The same to each ear, because the three channels are summed into one synth. Placing them
      // - two left and one right, or the other pairing - is a second synth's worth of work and
      // belongs here, where what a channel is is still known.
      samples[i * 2] += scratch[i * 2];
      samples[i * 2 + 1] += scratch[i * 2];
    }
    return count;
  }

  private void synthesise(long frameTstates) {

    int changesLeft = ayChangeCount;
    int changeIdx = 0;
    int envCounter = 15;
    int lastMixed = 0;
    boolean envFirst = true;
    boolean envRev = false;
    int envShape = 0;

    int lastA = 0, lastB = 0, lastC = 0;

    for (long f = 0; f < frameTstates; f += AY_CLOCK_DIVISOR * AY_CLOCK_RATIO) {

      // Aplicar cambios de registros pendientes
      while (changesLeft > 0 && ayChanges[changeIdx].tstates <= f) {
        AyChange ch = ayChanges[changeIdx++];
        int reg = ch.reg;
        ayRegisters[reg] = (byte) ch.val;
        changesLeft--;

        switch (reg) {
          case 0, 1, 2, 3, 4, 5 -> {
            int r = reg >> 1;
            int period = (ayRegisters[reg & ~1] & 0xFF) | ((ayRegisters[reg | 1] & 0x0F) << 8);
            ayTonePeriod[r] = period == 0 ? 1 : period;
            if (ayToneTick[r] >= ayTonePeriod[r] * 2) {
              ayToneTick[r] %= ayTonePeriod[r] * 2;
            }
          }
          case 6 -> ayNoisePeriod = ayRegisters[6] & 31;
          case 11, 12 -> ayEnvPeriod = (ayRegisters[11] & 0xFF) | ((ayRegisters[12] & 0xFF) << 8);
          case 13 -> {
            ayEnvTick = ayEnvInternalTick = ayEnvCycles = 0;
            envFirst = true;
            envRev = false;
            envCounter = (ayRegisters[13] & 4) != 0 ? 0 : 15;
            envShape = ayRegisters[13] & 0x0F;
          }
        }
      }

      // Envelope
      ayEnvCycles += AY_CLOCK_DIVISOR;
      int noiseCount = 0;
      while (ayEnvCycles >= 16) {
        ayEnvCycles -= 16;
        noiseCount++;
        ayEnvTick++;
        while (ayEnvTick >= ayEnvPeriod && ayEnvPeriod > 0) {
          ayEnvTick -= ayEnvPeriod;
          if (envFirst || ((envShape & 8) != 0 && (envShape & 1) == 0)) {
            int step = (envShape & 4) != 0 ? 1 : -1;
            envCounter += envRev ? -step : step;
            envCounter = Math.clamp(envCounter, 0, 15);
          }
          ayEnvInternalTick++;
          while (ayEnvInternalTick >= 16) {
            ayEnvInternalTick -= 16;
            if ((envShape & 8) == 0) envCounter = 0;
            else if ((envShape & 1) != 0) {
              if (envFirst && (envShape & 2) != 0) {
                envCounter = envCounter == 0 ? 15 : 0;
              }
            } else {
              if ((envShape & 2) != 0) envRev = !envRev;
              else envCounter = (envShape & 4) != 0 ? 0 : 15;
            }
            envFirst = false;
          }
          if (ayEnvPeriod == 0) break;
        }
      }

      // Generar tonos
      int[] toneLevel = new int[3];
      for (int i = 0; i < 3; i++) {
        int vol = ayRegisters[8 + i] & 15;
        toneLevel[i] = (ayRegisters[8 + i] & 16) != 0 ? ayToneLevelsScaled[envCounter] : ayToneLevelsScaled[vol];
      }

      int mixer = ayRegisters[7] & 0xFF;
      ayToneCycles += AY_CLOCK_DIVISOR;
      int toneCount = ayToneCycles >> 3;
      ayToneCycles &= 7;

      int chanA = toneLevel[0];
      int chanB = toneLevel[1];
      int chanC = toneLevel[2];

      if ((mixer & 1) == 0) chanA = ayDoTone(toneCount, 0, toneLevel[0]);
      if ((mixer & 8) == 0 && noiseToggle) chanA = 0;

      if ((mixer & 2) == 0) chanB = ayDoTone(toneCount, 1, toneLevel[1]);
      if ((mixer & 16) == 0 && noiseToggle) chanB = 0;

      if ((mixer & 4) == 0) chanC = ayDoTone(toneCount, 2, toneLevel[2]);
      if ((mixer & 32) == 0 && noiseToggle) chanC = 0;

      int mixed = chanA + chanB + chanC;
      if (mixed != lastMixed) {
        synth.update(f, mixed);
        lastMixed = mixed;
      }

      // Ruido
      ayNoiseTick += noiseCount;
      while (ayNoiseTick >= ayNoisePeriod && ayNoisePeriod > 0) {
        ayNoiseTick -= ayNoisePeriod;
        boolean feedback = ((rng & 1) ^ ((rng & 2) != 0 ? 1 : 0)) != 0;
        if (feedback) noiseToggle = !noiseToggle;
        if ((rng & 1) != 0) rng ^= 0x24000;
        rng >>= 1;
        if (ayNoisePeriod == 0) break;
      }
    }
  }

  private int ayDoTone(int count, int chan, int level) {
    ayToneTick[chan] += count;
    while (ayToneTick[chan] >= ayTonePeriod[chan]) {
      ayToneTick[chan] -= ayTonePeriod[chan];
      ayToneHigh[chan] = ayToneHigh[chan] == 0 ? 1 : 0;
    }
    return level != 0 && ayToneHigh[chan] != 0 ? level : 0;
  }
}
