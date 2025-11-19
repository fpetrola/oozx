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

package com.fpetrola.oozx.fuse.sound.p3;

import static java.lang.Long.MAX_VALUE;

/**
 * Blip_Buffer.java - Band-limited sound synthesis and buffering
 * Ported from Blip_Buffer 0.4.0 by Shay Green (blargg)
 * Java port by [tu nombre] - 2025
 */
public class BlipBuffer {

  // ========================================================================
  // Constantes
  // ========================================================================
  public static final int BLIP_BUFFER_ACCURACY = 16;
  public static final int BLIP_PHASE_BITS = 6;
  public static final int BLIP_RES = 1 << BLIP_PHASE_BITS;         // 64
  public static final int BLIP_WIDEST_IMPULSE = 16;
  public static final int BLIP_SAMPLE_MAX = 32767;
  public static final int BLIP_SAMPLE_BITS = 30;

  public static final int BLIP_MED_QUALITY = 8;
  public static final int BLIP_GOOD_QUALITY = 12;
  public static final int BLIP_HIGH_QUALITY = 16;


  public static final int BLIP_UNSCALED = 65535;
  public static final int BLIP_MAX_LENGTH = 0;

  public static final int BLIP_SYNTH_QUALITY = BLIP_GOOD_QUALITY;
  public static final int BLIP_SYNTH_RANGE = BLIP_UNSCALED;
  public static final int BLIP_SYNTH_WIDTH = BLIP_SYNTH_QUALITY;

  private static final int BUFFER_EXTRA = BLIP_WIDEST_IMPULSE + 2;

  // ========================================================================
  // Campos internos
  // ========================================================================
  public long factor;
  public long offset;
  public int[] buffer;          // buffer interno (32-bit)
  private int bufferSize;

  private long readerAccum;
  private int bassShift;

  private long sampleRate;
  private long clockRate;
  private int bassFreq = 16;
  private int lengthMs;

  public BlipBuffer() {
    factor = MAX_VALUE;
    clear(true);
  }

  public boolean setSampleRate(long new_rate, int msec) {
    long newSize = ((MAX_VALUE >>> BLIP_BUFFER_ACCURACY) - BUFFER_EXTRA - 64);
    if (msec != BLIP_MAX_LENGTH) {
      long s = (new_rate * (msec + 1) + 999) / 1000;
      if (s < newSize) newSize = (int) s;
    }

    if (bufferSize != newSize) {
      buffer = new int[(int) (newSize + BUFFER_EXTRA)];
      bufferSize = (int) newSize;
    }

    sampleRate = new_rate;
    lengthMs = (int) (newSize * 1000L / new_rate - 1);

    if (clockRate != 0) clockRate(clockRate);
    bassFreq(bassFreq);
    clear(true);
    return true;
  }

  public void clockRate(long cps) {
    factor = clockRateFactor(clockRate = cps);
  }

  public void endFrame(long t) {
    offset += t * factor;
    assert samplesAvail() <= bufferSize;
  }

  public long readSamples(int[] out, int maxSamples, boolean stereo) {
    long count = samplesAvail();

    if (count > maxSamples)
      count = maxSamples;

    if (count != 0) {
      int sample_shift = BLIP_SAMPLE_BITS - 16;

      int my_bass_shift = bassShift;

      long accum = readerAccum;

      int[] in = buffer;
      int inPos = 0;
      int outPos = 0;

      if (!stereo) {
        for (int n = (int) count; n-- != 0; ) {
          long s = accum >> sample_shift;

          accum -= accum >> my_bass_shift;
          accum += in[inPos++];
          out[outPos++] = (int) s;

          /* clamp sample */
          if ((int) s != s)
            out[outPos - 1] = (int) (0x7FFF - (s >> 24));
        }
      } else {
        for (int n = (int) count; n-- != 0; ) {
          long s = accum >> sample_shift;

          accum -= accum >> my_bass_shift;
          accum += in[inPos++];
          out[outPos] = (int) s;
          outPos += 2;

          /* clamp sample */
          if ((short) s != s)
            out[outPos - 2] = (int) (0x7FFF - (s >> 24));
        }
      }

      readerAccum = accum;
      removeSamples(count);
    }

    return count;
  }

  public void clear(boolean entireBuffer) {
    offset = 0;
    readerAccum = 0;
    if (buffer != null) {
      int count = entireBuffer ? bufferSize : (int) samplesAvail();
      java.util.Arrays.fill(buffer, 0, count + BUFFER_EXTRA, 0);
    }
  }

  public void bassFreq(int freq) {
    bassFreq = freq;
    int shift = 31;
    if (freq > 0) {
      shift = 13;
      long f = (freq << 16) / sampleRate;
      while ((f >>= 1) != 0 && --shift != 0) ;
    }
    bassShift = shift;
  }

  public void mixSamples(short[] in, int count) {
    int pos = (int) (offset >>> BLIP_BUFFER_ACCURACY) + BLIP_WIDEST_IMPULSE / 2;
    int prev = 0;
    int sampleShift = BLIP_SAMPLE_BITS - 16;

    for (int i = 0; i < count; i++) {
      int s = in[i] << sampleShift;
      buffer[pos] += s - prev;
      prev = s;
      pos++;
    }
    buffer[pos] -= prev;
  }

  public long samplesAvail() {
    return offset >>> BLIP_BUFFER_ACCURACY;
  }

  public long sampleRate() {
    return sampleRate;
  }

  public long clockRate() {
    return clockRate;
  }

  public int length() {
    return lengthMs;
  }

  public int outputLatency() {
    return BLIP_WIDEST_IMPULSE / 2;
  }

  private long clockRateFactor(long clockRate) {
    double ratio = (double) sampleRate / clockRate;
    long factor = (long) (ratio * (1L << BLIP_BUFFER_ACCURACY) + 0.5);
    assert factor > 0 || sampleRate == 0;
    return factor;
  }

  public void removeSamples(long count) {
    if (count > 0) {
      int remain = (int) (samplesAvail() + BUFFER_EXTRA);
      System.arraycopy(buffer, (int) count, buffer, 0, remain);
      java.util.Arrays.fill(buffer, remain, remain + (int) count, 0);
      offset -= count << BLIP_BUFFER_ACCURACY;
    }
  }

  public long countSamples(long duration) {
    long last = (offset + duration * factor) >>> BLIP_BUFFER_ACCURACY;
    long first = offset >>> BLIP_BUFFER_ACCURACY;
    return last - first;
  }
}