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

package com.fpetrola.oozx.fuse.sound.p2;

public final class BlipBuffer {

    // === Configuration ===
    public static final int BLIP_BUFFER_ACCURACY = 16;
    public static final int BLIP_PHASE_BITS = 6;
    private static final int BLIP_RES = 1 << BLIP_PHASE_BITS;  // 64
    private static final int BLIP_WIDEST_IMPULSE = 16;
    public static final int BLIP_SAMPLE_BITS = 30;
    private static final int BUFFER_EXTRA = BLIP_WIDEST_IMPULSE + 2;

    public static final int BLIP_MED_QUALITY  = 8;
    public static final int BLIP_GOOD_QUALITY = 12;
    public static final int BLIP_HIGH_QUALITY = 16;

    // === Public state ===
    public long factor;
    public long offset;
    public int[] buffer;
    private long bufferSize;
    private long sampleRate;
    private long clockRate;
    private int bassFreq = 16;
    private int bassShift;
    private long readerAccum;
    private int length;

    public BlipBuffer() {
        clear(true);
    }

    public String setSampleRate(long samplesPerSec, int msecLength) {
        if (msecLength == 0) msecLength = 250;

        long newSize = (Long.MAX_VALUE >>> BLIP_BUFFER_ACCURACY) - BUFFER_EXTRA - 64;
        if (msecLength != 0) {
            long s = (samplesPerSec * (msecLength + 1) + 999) / 1000;
            if (s < newSize) newSize = s;
        }

        if (bufferSize != newSize || buffer == null) {
            buffer = new int[(int)(newSize + BUFFER_EXTRA)];
            bufferSize = newSize;
        }

        sampleRate = samplesPerSec;
        length = (int)(newSize * 1000 / samplesPerSec - 1);
        if (clockRate != 0) clockRate(clockRate);
        bassFreq(bassFreq);
        clear(true);
        return null;
    }

    public void clockRate(long cps) {
        factor = clockRateFactor(clockRate = cps);
    }

    public void endFrame(long t) {
        offset += t * factor;
    }

    public long samplesAvail() { return offset >>> BLIP_BUFFER_ACCURACY; }

    public long readSamples(short[] out, int outOffset, long maxSamples, boolean stereo) {
        long count = samplesAvail();
        if (count > maxSamples) count = maxSamples;
        if (count == 0) return 0;

        final int sampleShift = BLIP_SAMPLE_BITS - 16;
        long accum = readerAccum;
        int pos = (int)(offset >>> BLIP_BUFFER_ACCURACY);

        if (!stereo) {
            for (long n = count; n > 0; n--) {
                long s = accum >> sampleShift;
                accum += buffer[pos++] - (accum >> bassShift);
                if ((short)s != s)
                    s = (s >> 24) > 0 ? Short.MIN_VALUE : Short.MAX_VALUE;
                out[outOffset++] = (short)s;
            }
        } else {
            for (long n = count; n > 0; n--) {
                long s = accum >> sampleShift;
                accum += buffer[pos++] - (accum >> bassShift);
                if ((short)s != s)
                    s = (s >> 24) > 0 ? Short.MIN_VALUE : Short.MAX_VALUE;
                out[outOffset] = (short)s;
                outOffset += 2;
            }
        }

        readerAccum = accum;
        removeSamples(count);
        return count;
    }

    public void clear(boolean entireBuffer) {
        offset = 0;
        readerAccum = 0;
        if (buffer != null) {
            int count = entireBuffer ? buffer.length : (int)(samplesAvail() + BUFFER_EXTRA);
            java.util.Arrays.fill(buffer, 0, count, 0);
        }
    }

    public void removeSamples(long count) {
        if (count > 0) {
            removeSilence(count);
            long remain = samplesAvail() + BUFFER_EXTRA;
            System.arraycopy(buffer, (int)count, buffer, 0, (int)remain);
            java.util.Arrays.fill(buffer, (int)remain, (int)(remain + count), 0);
        }
    }

    public void removeSilence(long count) {
        offset -= count << BLIP_BUFFER_ACCURACY;
    }

    public void bassFreq(int freq) {
        bassFreq = freq;
        int shift = 31;
        if (freq > 0) {
            shift = 13;
            long f = (freq << 16) / sampleRate;
            while ((f >>= 1) != 0) shift--;
        }
        bassShift = shift;
    }

    public void mixSamples(short[] in, int inOffset, long count) {
        int pos = (int)((offset >>> BLIP_BUFFER_ACCURACY) + (BLIP_WIDEST_IMPULSE / 2));
        int prev = 0;
        for (long i = 0; i < count; i++) {
            int s = in[inOffset++] << (BLIP_SAMPLE_BITS - 16);
            buffer[pos] += s - prev;
            prev = s;
            pos++;
        }
        buffer[pos] -= prev;
    }

    // Getters
    public long sampleRate() { return sampleRate; }
    public long clockRate()  { return clockRate; }
    public int  length()     { return length; }
    public int  outputLatency() { return BLIP_WIDEST_IMPULSE / 2; }

    private long clockRateFactor(long rate) {
        double ratio = sampleRate / (double)rate;
        return (long)(ratio * (1L << BLIP_BUFFER_ACCURACY) + 0.5);
    }

    // Internal resampled time
    long resampledTime(long t) { return t * factor + offset; }
    long resampledDuration(int t) { return (long)t * factor; }
}

// ===================================================================
// Full high-quality BlipSynth with sinc impulse generation
// ===================================================================

class BlipEq {
    final double treble;
    final long rolloffFreq;
    final long sampleRate;
    final long nyquistFreq;

    public BlipEq(double trebleDb) {
        this(trebleDb, 0, 44100, 0);
    }

    public BlipEq(double trebleDb, long rolloffFreq, long sampleRate, long cutoffFreq) {
        this.treble = trebleDb;
        this.rolloffFreq = rolloffFreq != 0 ? rolloffFreq : sampleRate / 2;
        this.sampleRate = sampleRate;
        this.nyquistFreq = cutoffFreq != 0 ? cutoffFreq : sampleRate / 2;
    }

    void generate(float[] out, int count) {
        double oversample = BlipSynth.BLIP_RES * 2.25 / count + 0.85;
        double halfRate = sampleRate * 0.5;
        if (nyquistFreq > 0)
            oversample = halfRate / nyquistFreq;

        double cutoff = rolloffFreq * oversample / halfRate;
        if (cutoff >= 0.999) cutoff = 0.999;

        double maxh = 4096.0;
        double rolloff = Math.pow(10.0, treble / (maxh * 20.0) / (1.0 - cutoff));
        double pow_a_n = Math.pow(rolloff, maxh - maxh * cutoff);
        double to_angle = Math.PI / 2 / maxh / oversample;

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
            out[i] = (float)((a * d + c * b) / (b * d));
        }

        // Apply hamming window
        double to_fraction = Math.PI / (count - 1);
        for (int i = 0; i < count; i++) {
            out[i] *= 0.54f - 0.46f * Math.cos(i * to_fraction);
        }
    }
}

final class BlipSynth {
    public static final int BLIP_RES = 64;
    private static final int BLIP_WIDEST_IMPULSE = 16;

    private final short[] impulses;
    private final int width;
    private final int impulsesSize;

    private BlipBuffer buf;
    private int lastAmp;
    private int deltaFactor;
    private long kernelUnit;
    private double volumeUnit;

    public BlipSynth(int quality, int range) {
        if (range < 0) range = -range;
        width = quality;
        impulsesSize = BLIP_RES / 2 * quality + 1;
        impulses = new short[impulsesSize];

        // Default reasonable treble EQ
        trebleEq(new BlipEq(-8.0));
        volume(1.0 / range);
    }

    public void output(BlipBuffer b) {
        buf = b;
        lastAmp = 0;
    }

    public void volume(double v) {
        volumeUnit = v;
        updateVolume();
    }

    public void trebleEq(BlipEq eq) {
        float[] fimpulse = new float[BLIP_RES * 8 + BLIP_RES * 2];
        int halfSize = BLIP_RES / 2 * (width - 1);

        // Generate half kernel
        eq.generate(fimpulse, BLIP_RES + halfSize);

        // Mirror the rest
        for (int i = 0; i < BLIP_RES; i++) {
            fimpulse[BLIP_RES + halfSize + i] = fimpulse[BLIP_RES + halfSize - 1 - i];
        }

        // Find scaling factor
        double total = 0.0;
        for (int i = 0; i < halfSize; i++) {
            total += fimpulse[BLIP_RES + i];
        }
        double baseUnit = 32768.0;
        double rescale = baseUnit / 2 / total;
        kernelUnit = (long) baseUnit;

        // Integrate, difference, rescale
        double sum = 0.0;
        double next = 0.0;
        for (int i = 0; i < impulsesSize; i++) {
            next += fimpulse[i + BLIP_RES];
            impulses[i] = (short) Math.floor((next - sum) * rescale + 0.5);
            sum += fimpulse[i];
        }

        adjustImpulse();
        updateVolume();
    }

    private void adjustImpulse() {
        int size = impulsesSize();
        for (int p = BLIP_RES; p-- >= BLIP_RES / 2; ) {
            int p2 = BLIP_RES - 2 - p;
            long error = kernelUnit;
            for (int i = 1; i < size; i += BLIP_RES) {
                error -= impulses[i + p];
                error -= impulses[i + p2];
            }
            if (p == p2) error /= 2;
            impulses[size - BLIP_RES + p] += (short)error;
        }
    }

    private void updateVolume() {
        double factor = volumeUnit * (1L << BlipBuffer.BLIP_SAMPLE_BITS) / kernelUnit;
        int shift = 0;
        while (factor < 2.0 && factor > 0) {
            factor *= 2;
            shift++;
        }
        if (shift > 0) {
            kernelUnit >>= shift;
            long offset = 0x8000 + (1L << (shift - 1));
            long offset2 = 0x8000 >> shift;
            for (int i = 0; i < impulsesSize; i++) {
                long v = (impulses[i] + offset) >> shift;
                impulses[i] = (short)(v - offset2);
            }
            adjustImpulse();
        }
        deltaFactor = (int)(factor + 0.5);
    }

    public void update(long time, int amp) {
        int delta = amp - lastAmp;
        lastAmp = amp;
        offset(time, delta);
    }

    public void offset(long time, int delta) {
        if (buf == null || delta == 0) return;
        offsetResampled(time * buf.factor + buf.offset, delta);
    }

    private void offsetResampled(long time, int delta) {
        delta *= deltaFactor;
        int phase = (int)((time >> (BlipBuffer.BLIP_BUFFER_ACCURACY - BlipBuffer.BLIP_PHASE_BITS)) & (BLIP_RES - 1));
        int impPhase = BLIP_RES - 1 - phase;

        int[] buf = this.buf.buffer;
        int pos = (int)(time >> BlipBuffer.BLIP_BUFFER_ACCURACY);
        int fwd = (BLIP_WIDEST_IMPULSE - width) / 2;
        int rev = fwd + width - 2;

        long i0 = impulses[impPhase];
        impPhase += BLIP_RES;

        // Forward direction
        for (int i = 0; i < width; i += 4) {
            if (i + 4 <= width) {
                long t0 = i0 * delta + buf[pos + fwd + i];
                long t1 = impulses[impPhase++] * delta + buf[pos + fwd + i + 1];
                long t2 = impulses[impPhase++] * delta + buf[pos + fwd + i + 2];
                long t3 = impulses[impPhase++] * delta + buf[pos + fwd + i + 3];
                i0 = impulses[impPhase++];
                buf[pos + fwd + i]     = (int)t0;
                buf[pos + fwd + i + 1] = (int)t1;
                buf[pos + fwd + i + 2] = (int)t2;
                buf[pos + fwd + i + 3] = (int)t3;
            }
        }

        // Reverse direction
        impPhase = phase;
        for (int i = width - 1; i >= 0; i -= 4) {
            if (i - 3 >= 0) {
                long t0 = impulses[impPhase--] * delta + buf[pos + rev - i];
                long t1 = impulses[impPhase--] * delta + buf[pos + rev + 1 - i];
                long t2 = impulses[impPhase--] * delta + buf[pos + rev + 2 - i];
                long t3 = impulses[impPhase--] * delta + buf[pos + rev + 3 - i];
                buf[pos + rev - i]     = (int)t0;
                buf[pos + rev + 1 - i] = (int)t1;
                buf[pos + rev + 2 - i] = (int)t2;
                buf[pos + rev + 3 - i] = (int)t3;
            }
        }
    }

    private int impulsesSize() { return impulsesSize; }
}