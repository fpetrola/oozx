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


  public static final int BLIP_SYNTH_QUALITY = BLIP_GOOD_QUALITY;

  private static final int BUFFER_EXTRA = BLIP_WIDEST_IMPULSE + 2;

  // ========================================================================
  // Campos internos
  // ========================================================================
  private long factor;
  private long offset;
  private int[] buffer;          // buffer interno (32-bit)
  private int bufferSize;

  private long readerAccum;
  private int bassShift;

  private long sampleRate;
  private long clockRate;
  private int bassFreq = 16;
  private int lengthMs;

  // ========================================================================
  // Constructor / Destructor
  // ========================================================================
  public BlipBuffer() {
    factor = Long.MAX_VALUE;
    clear(true);
  }

  // ========================================================================
  // API pública
  // ========================================================================
  public boolean setSampleRate(long samplesPerSec, int msecLength) {
    long newSize = ((0xfffffffffffffffL >>> BLIP_BUFFER_ACCURACY) - BUFFER_EXTRA - 64);
    if (msecLength != 0) {
      long s = (samplesPerSec * (msecLength + 1) + 999) / 1000;
      if (s < newSize) newSize = (int) s;
    }

    if (bufferSize != newSize) {
      buffer = new int[(int) (newSize + BUFFER_EXTRA)];
      if (buffer == null) return false;
      bufferSize = (int) newSize;
    }

    sampleRate = samplesPerSec;
    lengthMs = (int) (newSize * 1000L / samplesPerSec - 1);
    if (msecLength != 0) assert lengthMs == msecLength;

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
    long count = Math.min(samplesAvail(), maxSamples);
    if (count == 0) return 0;

    int sampleShift = BLIP_SAMPLE_BITS - 16;
    int bass = bassShift;
    long accum = readerAccum;
    int pos = 0;

    int outIdx = 0;
    for (int n = 0; n < count; n++) {
      long s = accum >> sampleShift;
      accum -= accum >> bass;
      accum += buffer[pos++];

      if ((short) s != s) {
        s = (s >> 24) == 0 ? Short.MAX_VALUE : Short.MIN_VALUE + 1;
      }
      out[outIdx] = (short) s;
      outIdx += stereo ? 2 : 1;
    }

    readerAccum = accum;
    removeSamples(count);
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

  // ========================================================================
  // Getters
  // ========================================================================
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

  // ========================================================================
  // Utilidades internas
  // ========================================================================
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

  // ========================================================================
  // Clases auxiliares
  // ========================================================================

  public static class BlipEq {
    public final double treble;
    public final long rolloffFreq;
    public final long sampleRate;
    public final long cutoffFreq;

    public BlipEq(double trebleDb) {
      this(trebleDb, 0, 44100, 0);
    }

    public BlipEq(double treble, long rolloffFreq, long sampleRate, long cutoffFreq) {
      this.treble = treble;
      this.rolloffFreq = rolloffFreq;
      this.sampleRate = sampleRate;
      this.cutoffFreq = cutoffFreq;
    }
  }

  // ------------------------------------------------------------------------
  // BlipSynth - versión genérica como en el original C++
  // ------------------------------------------------------------------------
  public static class BlipSynth {
    private final BlipSynthImpl impl;

    public BlipSynth(int quality, int range) {
      int absRange = range < 0 ? -range : range;
      short[] impulses = new short[BLIP_RES * (quality / 2) + 1];
      impl = new BlipSynthImpl(impulses, quality);
      volume(1.0 / absRange);
    }

    public void volume(double v) {
      impl.volumeUnit(v);
    }

    public void trebleEq(BlipEq eq) {
      impl.trebleEq(eq);
    }

    public void output(BlipBuffer buf) {
      impl.buf = buf;
      impl.lastAmp = 0;
    }

    public BlipBuffer output() {
      return impl.buf;
    }

    public void update(long time, int amplitude) {
      int delta = amplitude - impl.lastAmp;
      impl.lastAmp = amplitude;
      offset(time, delta);
    }

    public void offset(long time, int delta) {
      offsetResampled(time * impl.buf.factor + impl.buf.offset, delta);
    }

    public void offsetResampled(long time, int delta) {
      impl.offsetResampled(time, delta);
    }

    // Versión inline rápida
    public void offsetInline(long time, int delta) {
      offsetResampled(time * impl.buf.factor + impl.buf.offset, delta);
    }
  }

  // ------------------------------------------------------------------------
  // Implementación interna del sintetizador (equivalente a Blip_Synth_)
  // ------------------------------------------------------------------------
  private static class BlipSynthImpl {
    double volumeUnit = 0.0;
    final short[] impulses;
    final int width;
    long kernelUnit;
    BlipBuffer buf;
    int lastAmp;
    int deltaFactor;

    BlipSynthImpl(short[] impulses, int width) {
      this.impulses = impulses;
      this.width = width;
    }

    void volumeUnit(double newUnit) {
      if (newUnit == volumeUnit) return;
      if (kernelUnit == 0) trebleEq(new BlipEq(-8.0));

      volumeUnit = newUnit;
      double factor = newUnit * (1L << BLIP_SAMPLE_BITS) / kernelUnit;
      if (factor <= 0) return;

      int shift = 0;
      while (factor < 2.0 && shift < 31) {
        shift++;
        factor *= 2;
      }

      if (shift != 0) {
        kernelUnit >>= shift;
        long offset = 0x8000 + (1 << (shift - 1));
        long offset2 = 0x8000 >> shift;
        for (int i = impulses.length - 1; i >= 0; i--) {
          long v = (impulses[i] & 0xFFFFL) + offset;
          short i1 = (short) ((v >> shift) - offset2);
          impulses[i] = i1;
        }
        adjustImpulse();
      }
      deltaFactor = (int) (factor + 0.5);
    }

    // ====================================================================
    // trebleEq - IMPLEMENTACIÓN COMPLETA (original blargg)
    // ====================================================================
    void trebleEq(BlipEq eq) {
      float[] fimpulse = new float[BLIP_RES / 2 * (BLIP_WIDEST_IMPULSE - 1) + BLIP_RES * 2];
      int halfSize = BLIP_RES / 2 * (width - 1);

      // generate sinc
      generateSinc(fimpulse, BLIP_RES, halfSize, eq);

      // lower cutoff for narrow kernels
      double oversample = BLIP_RES * 2.25 / (halfSize / (BLIP_RES / 2.0)) + 0.85;
      double halfRate = eq.sampleRate * 0.5;
      if (eq.cutoffFreq != 0) oversample = halfRate / eq.cutoffFreq;
      double cutoff = eq.rolloffFreq * oversample / halfRate;

      genSinc(fimpulse, BLIP_RES, halfSize, eq.treble, cutoff);

      // apply Hamming window
      double toFraction = Math.PI / (halfSize * 2);
      for (int i = halfSize; i-- > 0; ) {
        fimpulse[BLIP_RES + i] *= 0.54f - 0.46f * Math.cos(i * toFraction * 2);
      }

      // integrate and rescale
      double total = 0.0;
      for (int i = 0; i < halfSize; i++) total += fimpulse[BLIP_RES + i];

      double baseUnit = 32768.0; // para compatibilidad con blip_unscaled
      double rescale = baseUnit / 2 / total;
      kernelUnit = (long)baseUnit;

      double sum = 0.0, next = 0.0;
      int size = impulses.length;
      for (int i = 0; i < size; i++) {
        sum  += fimpulse[i];
        next += fimpulse[i + BLIP_RES];
        impulses[i] = (short)Math.floor((next - sum) * rescale + 0.5);
      }

      adjustImpulse();

      // reapply volume si ya estaba seteado
      if (volumeUnit != 0.0) {
        double v = volumeUnit;
        volumeUnit = 0.0;
        volumeUnit(v);
      }
    }

    private void generateSinc(float[] out, int start, int count, BlipEq eq) {
      double treble = eq.treble;
      double cutoff = eq.cutoffFreq != 0 ? eq.cutoffFreq / (eq.sampleRate * 0.5) : 0.999;
      if (cutoff >= 0.999) cutoff = 0.999;
      if (treble < -300.0) treble = -300.0;
      if (treble > 5.0) treble = 5.0;

      double maxh = 4096.0;
      double rolloff = Math.pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
      double pow_a_n = Math.pow(rolloff, maxh - maxh * cutoff);
      double to_angle = Math.PI / 2 / maxh / (BLIP_RES * 2.25 / count + 0.85);

      for (int i = 0; i < count; i++) {
        double angle = ((i - count) * 2 + 1) * to_angle;
        double c = rolloff * Math.cos((maxh - 1.0) * angle) - Math.cos(maxh * angle);
        double cos_nc_angle = Math.cos(maxh * cutoff * angle);
        double cos_nc1_angle = Math.cos((maxh * cutoff - 1.0) * angle);
        double cos_angle = Math.cos(angle);
        c = c * pow_a_n - rolloff * cos_nc1_angle + cos_nc_angle;
        double d = 1.0 + rolloff * (rolloff - cos_angle - cos_angle);
        double b = 2.0 - cos_angle - cos_angle;
        double a = 1.0 - cos_angle - cos_nc_angle + cos_nc1_angle;
        out[start + i] = (float)((a * d + c * b) / (b * d));
      }
    }

    // gen_sinc port exacto
    private void genSinc(float[] out, int start, int count, double treble, double cutoff) {
      if (cutoff >= 0.999) cutoff = 0.999;
      if (treble < -300.0) treble = -300.0;
      if (treble > 5.0) treble = 5.0;

      double maxh = 4096.0;
      double rolloff = Math.pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
      double pow_a_n = Math.pow(rolloff, maxh - maxh * cutoff);
      double to_angle = Math.PI / 2 / maxh / (BLIP_RES * 2.25 / count + 0.85);

      for (int i = 0; i < count; i++) {
        double angle = ((i - count) * 2 + 1) * to_angle;
        double c = rolloff * Math.cos((maxh - 1.0) * angle) - Math.cos(maxh * angle);
        double cos_nc_angle = Math.cos(maxh * cutoff * angle);
        double cos_nc1_angle = Math.cos((maxh * cutoff - 1.0) * angle);
        double cos_angle = Math.cos(angle);
        c = c * pow_a_n - rolloff * cos_nc1_angle + cos_nc_angle;
        double d = 1.0 + rolloff * (rolloff - cos_angle - cos_angle);
        double b = 2.0 - cos_angle - cos_angle;
        double a = 1.0 - cos_angle - cos_nc_angle + cos_nc1_angle;
        out[start + i] = (float)((a * d + c * b) / (b * d));
      }
    }

    void adjustImpulse() {
      int size = impulses.length;
      for (int p = BLIP_RES; p-- >= BLIP_RES / 2; ) {
        int p2 = BLIP_RES - 2 - p;
        long error = kernelUnit;
        for (int i = 1; i < size; i += BLIP_RES) {
          error -= impulses[i + p] & 0xFFFF;
          error -= impulses[i + p2] & 0xFFFF;
        }
        if (p == p2) error /= 2;
        impulses[size - BLIP_RES + p] += (short) error;
      }
    }

    // ====================================================================
    // offsetResampled - versión Java de la macro BLIP_FWD/BLIP_REV
    // ====================================================================
    void offsetResampled(long time, int delta) {
//      if (time > buf.length())
//        return;
      delta *= deltaFactor;
      int phase = (int) ((time >> (BLIP_BUFFER_ACCURACY - BLIP_PHASE_BITS)) & (BLIP_RES - 1));
      int[] buf = this.buf.buffer;

      ArrayHandler imp = new ArrayHandler(impulses, BLIP_RES - phase);

      int fwd = (BLIP_WIDEST_IMPULSE - BLIP_SYNTH_QUALITY) / 2;
      int rev = fwd + BLIP_SYNTH_QUALITY - 2;

      long[] i0 = {imp.get()};
      blipFwd(buf, imp, i0, delta, 0, fwd);
      if (BLIP_SYNTH_QUALITY > 8) blipFwd(buf, imp, i0, delta, 2, fwd);
      if (BLIP_SYNTH_QUALITY > 12) blipFwd(buf, imp, i0, delta, 4, fwd);

      int mid = BLIP_SYNTH_QUALITY / 2 - 1;
      long t0 = i0[0] * delta + buf[fwd + mid - 1];
      long t1 = (long) imp.get(BLIP_RES * mid) * delta + buf[fwd + mid];
      imp = new ArrayHandler(impulses, phase);
      i0[0] = imp.get(BLIP_RES * mid);
      buf[fwd + mid - 1] = (int) t0;
      buf[fwd + mid] = (int) t1;

      if (BLIP_SYNTH_QUALITY > 12) blipRev(buf, imp, i0, delta, 6, rev);
      if (BLIP_SYNTH_QUALITY > 8) blipRev(buf, imp, i0, delta, 4, rev);
      blipRev(buf, imp, i0, delta, 2, rev);

      t0 = i0[0] * delta + buf[rev];
      t1 = (long) imp.get() * delta + buf[rev + 1];
      buf[rev] = (int) t0;
      buf[rev + 1] = (int) t1;
    }

    // Métodos que reemplazan las macros BLIP_FWD y BLIP_REV
    private void blipFwd(int[] buf, ArrayHandler imp, long[] i0, int delta, int i, int fwd) {
      int t0 = (int) (i0[0] * delta + buf[fwd + i]);
      int t1 = (int) (imp.get(BLIP_RES * (i + 1)) * delta + buf[fwd + 1 + i]);
      i0[0] = imp.get(BLIP_RES * (i + 2));
      buf[fwd + i] = t0;
      buf[fwd + 1 + i] = t1;

//      if (baseIdx + offset < buf.length) {
//        long t0 = i0[0] * delta + buf[baseIdx + offset];
//        long t1 = (imp.get(BLIP_RES * (offset + 1)) & 0xFFFFL) * delta + buf[baseIdx + 1 + offset];
//        i0[0] = (int) (imp.get(BLIP_RES * (offset + 2)) & 0xFFFFL);
//        buf[baseIdx + offset] = (int) t0;
//        buf[baseIdx + 1 + offset] = (int) t1;
//        // i0 se pasa por referencia simulada (se ignora aquí porque se recalcula en el llamador)
    }

    private void blipRev(int[] buf, ArrayHandler imp, long[] i0, int delta, int r, int rev) {
      int t0 = (int) (i0[0] * delta + buf[rev - r]);
      int t1 = imp.get(BLIP_RES * r) * delta + buf[rev + 1 - r];
      i0[0] = imp.get(BLIP_RES * (r - 1));
      buf[rev - r] = t0;
      buf[rev + 1 - r] = t1;

//    long t0 = i0[0] * delta + buf[baseIdx - r];
//    long t1 = (imp.get(BLIP_RES * r) & 0xFFFFL) * delta + buf[baseIdx + 1 - r];
//    i0[0] = ((int) (imp.get(BLIP_RES * (r - 1)) & 0xFFFFL));
//    buf[baseIdx - r] = (int) t0;
//    buf[baseIdx + 1 - r] = (int) t1;
    }
  }

}