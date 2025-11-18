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

import static com.fpetrola.oozx.fuse.sound.p3.BlipBuffer.*;

public class BlipSynthImpl {
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
    double factor = newUnit * (1L << BlipBuffer.BLIP_SAMPLE_BITS) / kernelUnit;
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

  void trebleEq(BlipEq eq) {
    float[] fimpulse = new float[BLIP_RES / 2 * (BLIP_WIDEST_IMPULSE - 1) + BLIP_RES * 2];
    int halfSize = BLIP_RES / 2 * (BLIP_SYNTH_WIDTH - 1);

    // generate sinc
//    generateSinc(fimpulse, BLIP_RES, halfSize, eq);

    eq.generate(halfSize, fimpulse, this);

    // integrate and rescale
    double total = 0.0;
    for (int i = 0; i < halfSize; i++) total += fimpulse[BLIP_RES + i];

    double baseUnit = 32768.0; // para compatibilidad con blip_unscaled
    double rescale = baseUnit / 2 / total;
    kernelUnit = (long) baseUnit;

    double sum = 0.0, next = 0.0;
    int size = impulses.length;
    for (int i = 0; i < size; i++) {
      sum += fimpulse[i];
      next += fimpulse[i + BLIP_RES];
      impulses[i] = (short) Math.floor((next - sum) * rescale + 0.5);
    }

    adjustImpulse();

    // reapply volume si ya estaba seteado
    if (volumeUnit != 0.0) {
      double v = volumeUnit;
      volumeUnit = 0.0;
      volumeUnit(v);
    }
  }

//  private void generateSinc(float[] out, int start, int count, BlipEq eq) {
//    double treble = eq.treble;
//    double cutoff = eq.cutoffFreq != 0 ? eq.cutoffFreq / (eq.sampleRate * 0.5) : 0.999;
//    if (cutoff >= 0.999) cutoff = 0.999;
//    if (treble < -300.0) treble = -300.0;
//    if (treble > 5.0) treble = 5.0;
//
//    double maxh = 4096.0;
//    double rolloff = Math.pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
//    double pow_a_n = Math.pow(rolloff, maxh - maxh * cutoff);
//    double to_angle = Math.PI / 2 / maxh / (BLIP_RES * 2.25 / count + 0.85);
//
//    for (int i = 0; i < count; i++) {
//      double angle = ((i - count) * 2 + 1) * to_angle;
//      double c = rolloff * Math.cos((maxh - 1.0) * angle) - Math.cos(maxh * angle);
//      double cos_nc_angle = Math.cos(maxh * cutoff * angle);
//      double cos_nc1_angle = Math.cos((maxh * cutoff - 1.0) * angle);
//      double cos_angle = Math.cos(angle);
//      c = c * pow_a_n - rolloff * cos_nc1_angle + cos_nc_angle;
//      double d = 1.0 + rolloff * (rolloff - cos_angle - cos_angle);
//      double b = 2.0 - cos_angle - cos_angle;
//      double a = 1.0 - cos_angle - cos_nc_angle + cos_nc1_angle;
//      out[start + i] = (float) ((a * d + c * b) / (b * d));
//    }
//  }

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

  void offsetResampled(long time, int delta) {
//      if (time > buf.length())
//        return;
    delta *= deltaFactor;
    int phase = (int) ((time >> (BlipBuffer.BLIP_BUFFER_ACCURACY - BlipBuffer.BLIP_PHASE_BITS)) & (BLIP_RES - 1));
    int[] buf = this.buf.buffer;

    ArrayHandler imp = new ArrayHandler(impulses, BLIP_RES - phase);

    int fwd = (BLIP_WIDEST_IMPULSE - BlipBuffer.BLIP_SYNTH_QUALITY) / 2;
    int rev = fwd + BlipBuffer.BLIP_SYNTH_QUALITY - 2;

    long[] i0 = {imp.get()};
    blipFwd(buf, imp, i0, delta, 0, fwd);
    if (BlipBuffer.BLIP_SYNTH_QUALITY > 8) blipFwd(buf, imp, i0, delta, 2, fwd);
    if (BlipBuffer.BLIP_SYNTH_QUALITY > 12) blipFwd(buf, imp, i0, delta, 4, fwd);

    int mid = BlipBuffer.BLIP_SYNTH_QUALITY / 2 - 1;
    long t0 = i0[0] * delta + buf[fwd + mid - 1];
    long t1 = (long) imp.get(BLIP_RES * mid) * delta + buf[fwd + mid];
    imp = new ArrayHandler(impulses, phase);
    i0[0] = imp.get(BLIP_RES * mid);
    buf[fwd + mid - 1] = (int) t0;
    buf[fwd + mid] = (int) t1;

    if (BlipBuffer.BLIP_SYNTH_QUALITY > 12) blipRev(buf, imp, i0, delta, 6, rev);
    if (BlipBuffer.BLIP_SYNTH_QUALITY > 8) blipRev(buf, imp, i0, delta, 4, rev);
    blipRev(buf, imp, i0, delta, 2, rev);

    t0 = i0[0] * delta + buf[rev];
    t1 = (long) imp.get() * delta + buf[rev + 1];
    buf[rev] = (int) t0;
    buf[rev + 1] = (int) t1;
  }

  private void blipFwd(int[] buf, ArrayHandler imp, long[] i0, int delta, int i, int fwd) {
    int t0 = (int) (i0[0] * delta + buf[fwd + i]);
    int t1 = (int) (imp.get(BLIP_RES * (i + 1)) * delta + buf[fwd + 1 + i]);
    i0[0] = imp.get(BLIP_RES * (i + 2));
    buf[fwd + i] = t0;
    buf[fwd + 1 + i] = t1;
  }

  private void blipRev(int[] buf, ArrayHandler imp, long[] i0, int delta, int r, int rev) {
    int t0 = (int) (i0[0] * delta + buf[rev - r]);
    int t1 = imp.get(BLIP_RES * r) * delta + buf[rev + 1 - r];
    i0[0] = imp.get(BLIP_RES * (r - 1));
    buf[rev - r] = t0;
    buf[rev + 1 - r] = t1;
  }
}
