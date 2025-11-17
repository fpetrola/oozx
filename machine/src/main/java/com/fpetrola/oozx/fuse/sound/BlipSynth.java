package com.fpetrola.oozx.fuse.sound;

import static com.fpetrola.oozx.fuse.sound.BlipBuffer.*;

public class BlipSynth {
    private static final double PI = 3.1415926535897932384626433832795029;

    private short[] impulses;
    private final BlipSynthImpl impl;

    public BlipSynth() {
        int size = (BlipBuffer.BLIP_RES / 2 * BlipBuffer.BLIP_SYNTH_WIDTH + 1) * 4;
        impulses = new short[size];
        impl = new BlipSynthImpl();
        _blip_synth_init(impl, impulses);
    }

    public void setVolume(double v) {
        _blip_synth_volume_unit(impl, v * (1.0 / (BLIP_SYNTH_RANGE < 0 ? -BlipBuffer.BLIP_SYNTH_RANGE : BlipBuffer.BLIP_SYNTH_RANGE)));
    }

    public void setTrebleEq(double treble) {
        BlipEq eq = new BlipEq();
        eq.treble = treble;
        eq.sampleRate = 44100;
        _blip_synth_treble_eq(impl, eq);
    }

    public void setOutput(BlipBuffer b) {
        impl.buf = b;
        impl.lastAmp = 0;
    }

    public void update(long time, int amplitude) {
        int delta = amplitude - impl.lastAmp;
        impl.lastAmp = amplitude;
        offsetResampled(time * impl.buf.getFactor() + impl.buf.getOffset(), delta, impl.buf);
    }

    public void offsetResampled(long time, int delta, BlipBuffer blipBuf) {
        delta *= impl.deltaFactor;
        int phase = (int) ((time >> (BLIP_BUFFER_ACCURACY - BLIP_PHASE_BITS)) & (BLIP_RES - 1));
        short[] imp = impulses;
        long[] buf = blipBuf.getBuffer();
        int bufIdx = (int) (time >> BLIP_BUFFER_ACCURACY);

        // === PROTECCIÓN: evitar overflow ===
        if (bufIdx < 0 || bufIdx + BLIP_WIDEST_IMPULSE + 2 >= buf.length) {
            return; // Silenciar si está fuera del buffer
        }
        // === Fin de protección ===

        int impIdx = BLIP_RES - phase;
        long i0 = imp[impIdx];
        int fwd = (BLIP_WIDEST_IMPULSE - BLIP_SYNTH_QUALITY) / 2;
        int rev = fwd + BLIP_SYNTH_QUALITY - 2;

        // BLIP_FWD
        long t0, t1;
        t0 = i0 * delta + buf[bufIdx + fwd];
        t1 = imp[impIdx + BlipBuffer.BLIP_RES] * delta + buf[bufIdx + fwd + 1];
        i0 = imp[impIdx + 2 * BlipBuffer.BLIP_RES];
        buf[bufIdx + fwd] = t0;
        buf[bufIdx + fwd + 1] = t1;

        if (BlipBuffer.BLIP_SYNTH_QUALITY > 8) {
            t0 = i0 * delta + buf[bufIdx + fwd + 2];
            t1 = imp[impIdx + 3 * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + fwd + 3];
            i0 = imp[impIdx + 4 * BlipBuffer.BLIP_RES];
            buf[bufIdx + fwd + 2] = t0;
            buf[bufIdx + fwd + 3] = t1;
        }
        if (BlipBuffer.BLIP_SYNTH_QUALITY > 12) {
            t0 = i0 * delta + buf[bufIdx + fwd + 4];
            t1 = imp[impIdx + 5 * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + fwd + 5];
            i0 = imp[impIdx + 6 * BlipBuffer.BLIP_RES];
            buf[bufIdx + fwd + 4] = t0;
            buf[bufIdx + fwd + 5] = t1;
        }

        int mid = BlipBuffer.BLIP_SYNTH_QUALITY / 2 - 1;
        t0 = i0 * delta + buf[bufIdx + fwd + mid - 1];
        t1 = imp[impIdx + mid * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + fwd + mid];
        impIdx = phase;
        i0 = imp[impIdx + mid * BlipBuffer.BLIP_RES];
        buf[bufIdx + fwd + mid - 1] = t0;
        buf[bufIdx + fwd + mid] = t1;

        if (BlipBuffer.BLIP_SYNTH_QUALITY > 12) {
            int r = 6;
            t0 = i0 * delta + buf[bufIdx + rev - r];
            t1 = imp[impIdx + r * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + rev + 1 - r];
            i0 = imp[impIdx + (r - 1) * BlipBuffer.BLIP_RES];
            buf[bufIdx + rev - r] = t0;
            buf[bufIdx + rev + 1 - r] = t1;
        }
        if (BlipBuffer.BLIP_SYNTH_QUALITY > 8) {
            int r = 4;
            t0 = i0 * delta + buf[bufIdx + rev - r];
            t1 = imp[impIdx + r * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + rev + 1 - r];
            i0 = imp[impIdx + (r - 1) * BlipBuffer.BLIP_RES];
            buf[bufIdx + rev - r] = t0;
            buf[bufIdx + rev + 1 - r] = t1;
        }
        {
            int r = 2;
            t0 = i0 * delta + buf[bufIdx + rev - r];
            t1 = imp[impIdx + r * BlipBuffer.BLIP_RES] * delta + buf[bufIdx + rev + 1 - r];
            i0 = imp[impIdx + (r - 1) * BlipBuffer.BLIP_RES];
            buf[bufIdx + rev - r] = t0;
            buf[bufIdx + rev + 1 - r] = t1;
        }
        t0 = i0 * delta + buf[bufIdx + rev];
        t1 = imp[impIdx] * delta + buf[bufIdx + rev + 1];
        buf[bufIdx + rev] = t0;
        buf[bufIdx + rev + 1] = t1;
    }

    // --- Internas ---
    private static class BlipSynthImpl {
        double volumeUnit;
        short[] impulses;
        long kernelUnit;
        BlipBuffer buf;
        int lastAmp;
        int deltaFactor;
    }

    private void _blip_synth_init(BlipSynthImpl synth, short[] p) {
        synth.impulses = p;
        synth.volumeUnit = 0.0;
        synth.kernelUnit = 0;
        synth.buf = null;
        synth.lastAmp = 0;
        synth.deltaFactor = 0;
    }

    private int _blip_synth_impulses_size() {
        return BlipBuffer.BLIP_RES / 2 * BlipBuffer.BLIP_SYNTH_WIDTH + 1;
    }

    private void _blip_synth_adjust_impulse(BlipSynthImpl synth) {
        int size = _blip_synth_impulses_size();
        for (int p = BlipBuffer.BLIP_RES; p-- >= BlipBuffer.BLIP_RES / 2; ) {
            int error = (int) synth.kernelUnit;
            int p2 = BlipBuffer.BLIP_RES - 2 - p;
            for (int i = 1; i < size; i += BlipBuffer.BLIP_RES) {
                error -= synth.impulses[i + p];
                error -= synth.impulses[i + p2];
            }
            if (p == p2) error /= 2;
            synth.impulses[size - BlipBuffer.BLIP_RES + p] += error;
        }
    }

    private void _blip_synth_treble_eq(BlipSynthImpl synth, BlipEq eq) {
        int halfSize = BlipBuffer.BLIP_RES / 2 * (BlipBuffer.BLIP_SYNTH_WIDTH - 1);
        float[] fimpulse = new float[BlipBuffer.BLIP_RES / 2 * (BlipBuffer.BLIP_WIDEST_IMPULSE - 1) + BlipBuffer.BLIP_RES * 2];

        blip_eq_generate(eq, fimpulse, BlipBuffer.BLIP_RES, halfSize);

        // Mirror
        for (int i = 0; i < BlipBuffer.BLIP_RES; i++) {
            fimpulse[BlipBuffer.BLIP_RES + halfSize + i] = fimpulse[BlipBuffer.BLIP_RES + halfSize - 1 - i];
            fimpulse[i] = 0.0f;
        }

        double total = 0.0;
        for (int i = 0; i < halfSize; i++) {
            total += fimpulse[BlipBuffer.BLIP_RES + i];
        }

        double baseUnit = 32768.0;
        double rescale = baseUnit / 2 / total;
        synth.kernelUnit = (long) baseUnit;

        double sum = 0.0, next = 0.0;
        int impulsesSize = _blip_synth_impulses_size();
        for (int i = 0; i < impulsesSize; i++) {
            synth.impulses[i] = (short) Math.floor((next - sum) * rescale + 0.5);
            sum += fimpulse[i];
            next += fimpulse[i + BlipBuffer.BLIP_RES];
        }
        _blip_synth_adjust_impulse(synth);

        double vol = synth.volumeUnit;
        if (vol != 0.0) {
            synth.volumeUnit = 0.0;
            _blip_synth_volume_unit(synth, vol);
        }
    }

    private void _blip_synth_volume_unit(BlipSynthImpl synth, double newUnit) {
        if (newUnit == synth.volumeUnit) return;

        if (synth.kernelUnit == 0) {
            BlipEq eq = new BlipEq();
            eq.treble = -8.0;
            eq.sampleRate = 44100;
            _blip_synth_treble_eq(synth, eq);
        }

        synth.volumeUnit = newUnit;
        double factor = newUnit * (1L << BlipBuffer.BLIP_SAMPLE_BITS) / synth.kernelUnit;
        if (factor > 0.0) {
            int shift = 0;
            while (factor < 2.0 && shift < 31) {
                shift++;
                factor *= 2.0;
            }
            if (shift > 0) {
                long offset = 0x8000 + (1 << (shift - 1));
                long offset2 = 0x8000 >> shift;
                synth.kernelUnit >>= shift;
                int size = _blip_synth_impulses_size();
                for (int i = 0; i < size; i++) {
                    long val = (synth.impulses[i] + offset) >> shift;
                    synth.impulses[i] = (short) (val - offset2);
                }
                _blip_synth_adjust_impulse(synth);
            }
        }
        synth.deltaFactor = (int) Math.floor(factor + 0.5);
    }

    private void blip_eq_generate(BlipEq eq, float[] out, int start, int count) {
        double oversample = BlipBuffer.BLIP_RES * 2.25 / count + 0.85;
        double halfRate = eq.sampleRate * 0.5;
        double cutoff = eq.cutoffFreq != 0 ? halfRate / eq.cutoffFreq : eq.rolloffFreq * oversample / halfRate;
        if (cutoff > 0.999) cutoff = 0.999;

        gen_sinc(out, start, count, oversample, eq.treble, cutoff);

        double toFraction = PI / (count - 1);
        for (int i = 0; i < count; i++) {
            out[start + i] *= 0.54 - 0.46 * Math.cos(i * toFraction);
        }
    }

    private void gen_sinc(float[] out, int start, int count, double oversample, double treble, double cutoff) {
        double maxh = 4096.0;
        double rolloff = Math.pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
        double pow_a_n = Math.pow(rolloff, maxh - maxh * cutoff);
        double to_angle = PI / 2 / maxh / oversample;

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
            out[start + i] = (float) ((a * d + c * b) / (b * d));
        }
    }
}