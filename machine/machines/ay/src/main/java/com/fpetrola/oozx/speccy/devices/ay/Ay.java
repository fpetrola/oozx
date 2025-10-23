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

package com.fpetrola.oozx.speccy.devices.ay;

import com.fpetrola.oozx.speccy.modules.sound.AudioOutput;

import com.fpetrola.oozx.speccy.modules.sound.AudioSource;

import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;

import java.util.Arrays;

/**
 * AY-3-8912 sound generator, present on the 128K and absent on the 48K. Register writes are
 * timestamped and queued; once a frame the queue is replayed against the tone/noise/envelope
 * clocks to build samples. One shared synth buffer is enough because all three channels mix
 * to mono here; stereo panning would need per-channel synths instead.
 */
class Ay implements AudioSource {

  public static final int AMPL_AY_TONE = 24 * 256;

  private static final int AY_CHANGE_MAX = 8000;
  private static final int AY_CLOCK_DIVISOR = 16;
  private static final int AY_CLOCK_RATIO = 2;
  /** Machine T-states per internal chip step. */
  private static final int STEP = AY_CLOCK_DIVISOR * AY_CLOCK_RATIO;
  /** Sentinel for quietSteps meaning no queued write is pending. */
  private static final long NO_CHANGE = Long.MAX_VALUE;

  /** Manufacturer's volume table, rescaled to a 16-bit range. */
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

  /** Write count, used elsewhere to detect that this chip is actually in use. */
  public long writes;

  /** Output volume, owned by this class rather than the mixer. */
  private int volume = 100;

  public Ay(AudioOutput output) {
    takeOutputFrom(output);
    reset();
  }

  @Override
  public void takeOutputFrom(AudioOutput output) {
    synth = output.newSynth(volume);
    scratch = new int[output.frameSize() * 2];
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
    // Cleared by the reader of the queue, not the writer, once this frame's changes are used.
    ayChangeCount = 0;
  }

  @Override
  public int mixInto(int[] samples, int frames) {
    int count = synth.readSamples(scratch, frames, true);
    for (int i = 0; i < count; i++) {
      // Mono output duplicated to both channels; real stereo panning would need separate synths.
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

    boolean noiseTicked = false;
    for (long f = 0; f < frameTstates; f += STEP) {
      // Between events (a tone edge, an envelope/noise tick, a register write, frame end)
      // nothing audible changes, so quietSteps below fast-forwards counters instead of
      // iterating every step one at a time; result is identical, just faster. A muted
      // channel's edge is likewise not an event, since crossing it changes nothing audible.
      int audible = 0;
      for (int chan = 0; chan < 3; chan++) {
        int level = (ayRegisters[8 + chan] & 16) != 0 ? ayToneLevelsScaled[envCounter] : ayToneLevelsScaled[ayRegisters[8 + chan] & 15];
        if (level != 0) audible |= 1 << chan;
      }
      int quiet = noiseTicked ? 0 : quietSteps(f, frameTstates, changesLeft > 0 ? ayChanges[changeIdx].tstates : NO_CHANGE, audible);
      if (quiet > 0) {
        int on = ayRegisters[7] & 0xFF;
        for (int chan = 0; chan < 3; chan++) {
          if ((on & (1 << chan)) != 0) continue;
          int ticks = ayToneTick[chan] + 2 * quiet;
          if ((audible & (1 << chan)) == 0 && ticks >= ayTonePeriod[chan]) {
            ayToneHigh[chan] ^= (ticks / ayTonePeriod[chan]) & 1;
            ticks %= ayTonePeriod[chan];
          }
          ayToneTick[chan] = ticks;
        }
        ayEnvTick += quiet;
        ayNoiseTick += quiet;
        f += (long) STEP * quiet;
      }

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

      // Noise updates after mixing, so its effect is heard starting next step, not this one.
      ayNoiseTick += noiseCount;
      noiseTicked = false;
      while (ayNoiseTick >= ayNoisePeriod && ayNoisePeriod > 0) {
        noiseTicked = true;
        ayNoiseTick -= ayNoisePeriod;
        boolean feedback = ((rng & 1) ^ ((rng & 2) != 0 ? 1 : 0)) != 0;
        if (feedback) noiseToggle = !noiseToggle;
        if ((rng & 1) != 0) rng ^= 0x24000;
        rng >>= 1;
        if (ayNoisePeriod == 0) break;
      }
    }
  }

  /** Steps until the next audible event: two ticks/step for an active tone, one for
   * envelope/noise, or the step a queued write falls on, whichever comes first. */
  int quietSteps(long f, long frameTstates, long nextChange, int audible) {
    // Step 0 always runs, to emit the frame's starting level even with no change yet.
    if (f == 0) return 0;
    long quiet = (frameTstates - f + STEP - 1) / STEP - 1;
    if (nextChange <= f) return 0;
    if (nextChange != NO_CHANGE) quiet = Math.min(quiet, (nextChange - f + STEP - 1) / STEP);
    int on = ayRegisters[7] & 0xFF;
    for (int chan = 0; chan < 3; chan++) {
      if ((on & (1 << chan)) == 0 && (audible & (1 << chan)) != 0) {
        quiet = Math.min(quiet, Math.max(0, (ayTonePeriod[chan] - ayToneTick[chan] + 1) / 2 - 1));
      }
    }
    if (ayEnvPeriod > 0) quiet = Math.min(quiet, Math.max(0, ayEnvPeriod - ayEnvTick - 1));
    if (ayNoisePeriod > 0) quiet = Math.min(quiet, Math.max(0, ayNoisePeriod - ayNoiseTick - 1));
    return (int) quiet;
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
