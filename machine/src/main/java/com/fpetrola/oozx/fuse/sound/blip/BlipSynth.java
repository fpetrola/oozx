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

package com.fpetrola.oozx.fuse.sound.blip;

import static com.fpetrola.oozx.fuse.sound.blip.BlipBuffer.*;
import static java.lang.Math.floor;

public class BlipSynth {
  public static final int BLIP_SYNTH_WIDTH = BLIP_HIGH_QUALITY;
  public static final int BLIP_SYNTH_RANGE = BLIP_UNSCALED;
  final short[] impulses;
  final int quality;
  final BlipBuffer blipBuffer;

  double volumeUnit = 0.0;
  long kernelUnit;
  int lastAmp;
  int deltaFactor;

  public BlipSynth(int quality, long soundFreq, int msec, long effectiveSpeed, int bass, double volume, double treble) {
    this.impulses = new short[BLIP_RES * (quality / 2) + 1];
    this.quality = quality;

    BlipBuffer blipBuffer = new BlipBuffer();
    blipBuffer.setSampleRate(soundFreq, msec);
    blipBuffer.clockRate(effectiveSpeed);

    volume(volume);

    this.blipBuffer = blipBuffer;
    lastAmp = 0;
    blipBuffer.bassFreq(bass);
    trebleEq(new BlipEq(treble));
  }

  public void endFrame(int frameTstates) {
    blipBuffer.endFrame(frameTstates);
  }

  public int readSamples(int[] outputSamples, int soundFrameSize, boolean stereo) {
    return blipBuffer.readSamples(outputSamples, soundFrameSize, stereo);
  }

  public void volume(double v) {
    volumeUnit(v * (1.0 / (BLIP_SYNTH_RANGE < 0 ? -(BLIP_SYNTH_RANGE) : BLIP_SYNTH_RANGE)));
  }

  public void close() {
    lastAmp = 0;
  }

  private void trebleEq(BlipEq eq) {
    float[] fImpulse = new float[BLIP_RES / 2 * (BLIP_WIDEST_IMPULSE - 1) + BLIP_RES * 2];
    int half_size = BLIP_RES / 2 * (BLIP_SYNTH_WIDTH - 1);

    eq.generate(half_size, fImpulse, BLIP_RES);

    for (int i = BLIP_RES; i-- != 0; )
      fImpulse[BLIP_RES + half_size + i] =
          fImpulse[BLIP_RES + half_size - 1 - i];

    for (int i = 0; i < BLIP_RES; i++)
      fImpulse[i] = 0.0f;

    double total = 0.0;
    for (int i = 0; i < half_size; i++)
      total += fImpulse[BLIP_RES + i];

    double base_unit = 32768.0;          /*  necessary for blip_unscaled to work */
    double rescale = base_unit / 2 / total;
    kernelUnit = (long) base_unit;

    double sum = 0.0;
    double next = 0.0;
    int impulses_size = impulsesSize();

    for (int i = 0; i < impulses_size; i++) {
      impulses[i] = (short) floor((next - sum) * rescale + 0.5);
      sum += fImpulse[i];
      next += fImpulse[i + BLIP_RES];
    }

    adjustImpulse();

    /* volume might require rescaling */
    double vol = volumeUnit;
    if (vol != 0.0) {
      volumeUnit = 0.0;
      volumeUnit(vol);
    }
  }

  public void update(long time, int amplitude) {
    int delta = amplitude - lastAmp;
    lastAmp = amplitude;
    offsetResampled(time * blipBuffer.factor + blipBuffer.offset, delta);
  }

  private void volumeUnit(double newUnit) {
    if (newUnit == volumeUnit) return;
    if (kernelUnit == 0) trebleEq(new BlipEq(-8.0));

    volumeUnit = newUnit;
    double factor = newUnit * (1L << BLIP_SAMPLE_BITS) / kernelUnit;
    if (factor > 0.0) {
      int shift = 0;

      /* if unit is really small, might need to attenuate kernel */
      while (factor < 2.0) {
        shift++;
        factor *= 2.0;
      }

      if (shift != 0) {
        /* keep values positive to avoid round-towards-zero of sign-preserving
         * right shift for negative values */
        long offset = 0x8000 + (1L << (shift - 1));
        long offset2 = 0x8000 >> shift;

        int i;

        kernelUnit >>= shift;

        for (i = impulsesSize(); i-- != 0; ) {
          long i2 = impulses[i] + offset;
          short i1 = (short) ((i2 >> shift) - offset2);
          impulses[i] = i1;
        }

        adjustImpulse();
      }
    }
    deltaFactor = (int) floor(factor + 0.5);
  }

  private int impulsesSize() {
    return BLIP_RES / 2 * BLIP_SYNTH_WIDTH + 1;
  }

  private void adjustImpulse() {
    int size = impulsesSize();
    for (int p = BLIP_RES; p-- >= BLIP_RES / 2; ) {
      int p2 = BLIP_RES - 2 - p;
      int error = (int) kernelUnit;
      for (int i = 1; i < size; i += BLIP_RES) {
        error -= impulses[i + p];
        error -= impulses[i + p2];
      }
      if (p == p2) error /= 2;
      impulses[size - BLIP_RES + p] += (short) error;
    }
  }

  private void offsetResampled(long time, int delta) {
    delta *= deltaFactor;
    int phase = (int) ((time >> (BLIP_BUFFER_ACCURACY - BLIP_PHASE_BITS)) & (BLIP_RES - 1));
    int[] buffer = this.blipBuffer.getBuffer();
    int intArrayIndex = (int) (time >> BLIP_BUFFER_ACCURACY);

    int fwd = (BLIP_WIDEST_IMPULSE - quality) / 2;
    int rev = fwd + quality - 2;

    int impulsesBaseIndex = BLIP_RES - phase;
    int i0 = impulses[impulsesBaseIndex];
    i0 = blipFwd(buffer, delta, 0, fwd, intArrayIndex, impulsesBaseIndex, i0);
    if (quality > 8) i0 = blipFwd(buffer, delta, 2, fwd, intArrayIndex, impulsesBaseIndex, i0);
    if (quality > 12) i0 = blipFwd(buffer, delta, 4, fwd, intArrayIndex, impulsesBaseIndex, i0);

    int mid = quality / 2 - 1;
    int t0 = i0 * delta + buffer[intArrayIndex + fwd + mid - 1];
    int t1 = impulses[impulsesBaseIndex + BLIP_RES * mid] * delta + buffer[intArrayIndex + fwd + mid];

    impulsesBaseIndex = phase;

    i0 = impulses[impulsesBaseIndex + BLIP_RES * mid];
    buffer[intArrayIndex + fwd + mid - 1] = t0;
    buffer[intArrayIndex + fwd + mid] = t1;

    if (quality > 12) i0 = blipRev(buffer, delta, 6, rev, intArrayIndex, impulsesBaseIndex, i0);
    if (quality > 8) i0 = blipRev(buffer, delta, 4, rev, intArrayIndex, impulsesBaseIndex, i0);
    i0 = blipRev(buffer, delta, 2, rev, intArrayIndex, impulsesBaseIndex, i0);

    buffer[intArrayIndex + rev] = i0 * delta + buffer[intArrayIndex + rev];
    buffer[intArrayIndex + rev + 1] = impulses[impulsesBaseIndex] * delta + buffer[intArrayIndex + rev + 1];
  }

  private int blipFwd(int[] buffer, int delta, int i, int fwd, int bufferBaseIndex, int impulsesBaseIndex, int i0) {
    buffer[bufferBaseIndex + fwd + i] = i0 * delta + buffer[bufferBaseIndex + fwd + i];
    buffer[bufferBaseIndex + fwd + 1 + i] = impulses[impulsesBaseIndex + BLIP_RES * (i + 1)] * delta + buffer[bufferBaseIndex + fwd + 1 + i];
    i0 = impulses[impulsesBaseIndex + BLIP_RES * (i + 2)];
    return i0;
  }

  private int blipRev(int[] buffer, int delta, int r, int rev, int bufferBaseIndex, int impulsesBaseIndex, int i0) {
    buffer[bufferBaseIndex + rev - r] = i0 * delta + buffer[bufferBaseIndex + rev - r];
    buffer[bufferBaseIndex + rev + 1 - r] = impulses[impulsesBaseIndex + BLIP_RES * r] * delta + buffer[bufferBaseIndex + rev + 1 - r];
    i0 = impulses[impulsesBaseIndex + BLIP_RES * (r - 1)];
    return i0;
  }
}
