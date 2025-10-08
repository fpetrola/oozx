/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse;

import java.util.Arrays;
import java.util.Objects;

import static com.fpetrola.oozx.fuse.BlipBuffer.BLIP_BUFFER_ACCURACY;

public class BlipSynth {
    // Constants
    public static final int BLIP_PHASE_BITS = 6;
    public static final int BLIP_RES = 1 << BLIP_PHASE_BITS;
    public static final int BLIP_WIDEST_IMPULSE = 16;
    public static final int BLIP_SYNTH_QUALITY = 12; // BLIP_GOOD_QUALITY
    public static final int BLIP_SYNTH_RANGE = 65535;
    public static final int BLIP_SYNTH_WIDTH = BLIP_SYNTH_QUALITY;
    public static final int BLIP_SAMPLE_BITS = 30;

    // Fields
    private short[] impulses;
    private final BlipSynthImpl impl;

    // Constructor
    public BlipSynth() {
        impl = new BlipSynthImpl();
        impulses = new short[(BLIP_RES * (BLIP_SYNTH_WIDTH / 2) + 1) * 4];
        impl.init(impulses);
    }

    // Cleanup
    public void delete() {
        impulses = null;
    }

    // Set volume
    public void setVolume(double volume) {
        impl.volumeUnit(volume * (1.0 / BLIP_SYNTH_RANGE));
    }

    // Set output buffer
    public void setOutput(BlipBuffer buffer) {
        impl.buf = buffer;
        impl.lastAmp = 0;
    }

    // Set treble equalization
    public void setTrebleEq(double treble) {
        BlipEq eq = new BlipEq(treble, 0, 44100, 0);
        impl.trebleEq(eq);
    }

    // Update amplitude
    public void update(long time, int amplitude) {
        int delta = amplitude - impl.lastAmp;
        impl.lastAmp = amplitude;
        offsetResampled(time * impl.buf.factor + impl.buf.offset, delta, impl.buf);
    }

    // Offset resampled
    public void offsetResampled(long time, int delta, BlipBuffer blipBuf) {
        delta *= impl.deltaFactor;
        int phase = (int) ((time >> (BLIP_BUFFER_ACCURACY - BLIP_PHASE_BITS)) & (BLIP_RES - 1));
        short[] imp = Arrays.copyOfRange(impulses, BLIP_RES - phase, impulses.length);
        long[] buf = blipBuf.buffer;
        int bufIndex = (int) (time >> BLIP_BUFFER_ACCURACY);
        long i0 = imp[0];

        int fwd = (BLIP_WIDEST_IMPULSE - BLIP_SYNTH_WIDTH) / 2;
        int rev = fwd + BLIP_SYNTH_WIDTH - 2;

        // Forward impulses
        long t0 = i0 * delta + buf[fwd];
        long t1 = imp[BLIP_RES] * delta + buf[fwd + 1];
        i0 = imp[BLIP_RES * 2];
        buf[fwd] = t0;
        buf[fwd + 1] = t1;

        if (BLIP_SYNTH_WIDTH > 8) {
            t0 = i0 * delta + buf[fwd + 2];
            t1 = imp[BLIP_RES * 3] * delta + buf[fwd + 3];
            i0 = imp[BLIP_RES * 4];
            buf[fwd + 2] = t0;
            buf[fwd + 3] = t1;
        }

        int mid = BLIP_SYNTH_WIDTH / 2 - 1;
        t0 = i0 * delta + buf[fwd + mid - 1];
        t1 = imp[BLIP_RES * mid] * delta + buf[fwd + mid];
        imp = Arrays.copyOfRange(impulses, phase, impulses.length);
        i0 = imp[BLIP_RES * mid];
        buf[fwd + mid - 1] = t0;
        buf[fwd + mid] = t1;

        if (BLIP_SYNTH_WIDTH > 12) {
            t0 = i0 * delta + buf[rev - 6];
            t1 = imp[BLIP_RES * 6] * delta + buf[rev - 5];
            i0 = imp[BLIP_RES * 5];
            buf[rev - 6] = t0;
            buf[rev - 5] = t1;
        }
        if (BLIP_SYNTH_WIDTH > 8) {
            t0 = i0 * delta + buf[rev - 4];
            t1 = imp[BLIP_RES * 4] * delta + buf[rev - 3];
            i0 = imp[BLIP_RES * 3];
            buf[rev - 4] = t0;
            buf[rev - 3] = t1;
        }

        t0 = i0 * delta + buf[rev - 2];
        t1 = imp[BLIP_RES * 2] * delta + buf[rev - 1];
        i0 = imp[BLIP_RES];
        buf[rev - 2] = t0;
        buf[rev - 1] = t1;

        t0 = i0 * delta + buf[rev];
        t1 = imp[0] * delta + buf[rev + 1];
        buf[rev] = t0;
        buf[rev + 1] = t1;
    }

    // Inner implementation class
    private static class BlipSynthImpl {
        double volumeUnit;
        short[] impulses;
        long kernelUnit;
        BlipBuffer buf;
        int lastAmp;
        int deltaFactor;

        void init(short[] impulses) {
            this.impulses = impulses;
            volumeUnit = 0.0;
            kernelUnit = 0;
            buf = null;
            lastAmp = 0;
            deltaFactor = 0;
        }

        int impulsesSize() {
            return BLIP_RES / 2 * BLIP_SYNTH_WIDTH + 1;
        }

        void adjustImpulse() {
            int size = impulsesSize();
            for (int p = BLIP_RES - 1; p >= BLIP_RES / 2; p--) {
                int error = (int) kernelUnit;
                int p2 = BLIP_RES - 2 - p;
                for (int i = 1; i < size; i += BLIP_RES) {
                    error -= impulses[i + p];
                    error -= impulses[i + p2];
                }
                if (p == p2) {
                    error /= 2;
                }
                impulses[size - BLIP_RES + p] += error;
            }
        }

        void trebleEq(BlipEq eq) {
            float[] fimpulse = new float[BLIP_RES / 2 * (BLIP_WIDEST_IMPULSE - 1) + BLIP_RES * 2];
            int halfSize = BLIP_RES / 2 * (BLIP_SYNTH_WIDTH - 1);
            generate(eq, fimpulse, halfSize);

            for (int i = 0; i < BLIP_RES; i++) {
                fimpulse[BLIP_RES + halfSize + i] = fimpulse[BLIP_RES + halfSize - 1 - i];
            }
            Arrays.fill(fimpulse, 0, BLIP_RES, 0.0f);

            double total = 0.0;
            for (int i = 0; i < halfSize; i++) {
                total += fimpulse[BLIP_RES + i];
            }

            double baseUnit = 32768.0;
            double rescale = baseUnit / 2 / total;
            kernelUnit = (long) baseUnit;

            double sum = 0.0, next = 0.0;
            int impulsesSize = impulsesSize();
            for (int i = 0; i < impulsesSize; i++) {
                impulses[i] = (short) Math.floor((next - sum) * rescale + 0.5);
                sum += fimpulse[i];
                next += fimpulse[i + BLIP_RES];
            }

            adjustImpulse();

            double vol = volumeUnit;
            if (vol != 0) {
                volumeUnit = 0.0;
                volumeUnit(vol);
            }
        }

        void volumeUnit(double newUnit) {
            if (newUnit != volumeUnit) {
                if (kernelUnit == 0) {
                    trebleEq(new BlipEq(-8.0, 0, 44100, 0));
                }
                volumeUnit = newUnit;
                double factor = newUnit * (1L << BLIP_SAMPLE_BITS) / kernelUnit;
                if (factor > 0.0) {
                    int shift = 0;
                    while (factor < 2.0) {
                        shift++;
                        factor *= 2.0;
                    }
                    if (shift > 0) {
                        long offset = 0x8000 + (1 << (shift - 1));
                        long offset2 = 0x8000 >> shift;
                        kernelUnit >>= shift;
                        for (int i = impulsesSize(); i-- > 0; ) {
                            impulses[i] = (short) (((impulses[i] + offset) >> shift) - offset2);
                        }
                        adjustImpulse();
                    }
                    deltaFactor = (int) Math.floor(factor + 0.5);
                }
            }
        }

        private void generate(BlipEq eq, float[] out, int count) {
            double oversample = BLIP_RES * 2.25 / count + 0.85;
            double halfRate = eq.sampleRate * 0.5;
            if (eq.cutoffFreq != 0) {
                oversample = halfRate / eq.cutoffFreq;
            }
            double cutoff = eq.rolloffFreq * oversample / halfRate;
            genSinc(out, count, BLIP_RES * oversample, eq.treble, cutoff);

            double toFraction = Math.PI / (count - 1);
            for (int i = count; i-- > 0; ) {
                out[i] *= 0.54 - 0.46 * Math.cos(i * toFraction);
            }
        }

        private void genSinc(float[] out, int count, double oversample, double treble, double cutoff) {
            if (cutoff > 0.999) cutoff = 0.999;
            if (treble < -300.0) treble = -300.0;
            if (treble > 5.0) treble = 5.0;

            double maxh = 4096.0;
            double rolloff = Math.pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
            double powAN = Math.pow(rolloff, maxh - maxh * cutoff);
            double toAngle = Math.PI / 2 / maxh / oversample;

            for (int i = 0; i < count; i++) {
                double angle = ((i - count) * 2 + 1) * toAngle;
                double c = rolloff * Math.cos((maxh - 1.0) * angle) - Math.cos(maxh * angle);
                double cosNcAngle = Math.cos(maxh * cutoff * angle);
                double cosNc1Angle = Math.cos((maxh * cutoff - 1.0) * angle);
                double cosAngle = Math.cos(angle);
                c = c * powAN - rolloff * cosNc1Angle + cosNcAngle;
                double d = 1.0 + rolloff * (rolloff - cosAngle - cosAngle);
                double b = 2.0 - cosAngle - cosAngle;
                double a = 1.0 - cosAngle - cosNcAngle + cosNc1Angle;
                out[i] = (float) ((a * d + c * b) / (b * d));
            }
        }
    }
}

// Equalization parameters
final class BlipEq {
  public final double treble;
  public final long rolloffFreq;
  public final long sampleRate;
  public final long cutoffFreq;

  BlipEq(double treble, long rolloffFreq, long sampleRate, long cutoffFreq) {
    this.treble = treble;
    this.rolloffFreq = rolloffFreq;
    this.sampleRate = sampleRate;
    this.cutoffFreq = cutoffFreq;
  }

  public double treble() {
    return treble;
  }

  public long rolloffFreq() {
    return rolloffFreq;
  }

  public long sampleRate() {
    return sampleRate;
  }

  public long cutoffFreq() {
    return cutoffFreq;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (BlipEq) obj;
    return Double.doubleToLongBits(this.treble) == Double.doubleToLongBits(that.treble) &&
        this.rolloffFreq == that.rolloffFreq &&
        this.sampleRate == that.sampleRate &&
        this.cutoffFreq == that.cutoffFreq;
  }

  @Override
  public int hashCode() {
    return Objects.hash(treble, rolloffFreq, sampleRate, cutoffFreq);
  }

  @Override
  public String toString() {
    return "BlipEq[" +
        "treble=" + treble + ", " +
        "rolloffFreq=" + rolloffFreq + ", " +
        "sampleRate=" + sampleRate + ", " +
        "cutoffFreq=" + cutoffFreq + ']';
  }
}