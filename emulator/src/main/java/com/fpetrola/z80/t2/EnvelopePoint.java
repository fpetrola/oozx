package com.fpetrola.z80.t2;

class EnvelopePoint {
    final double hz, db;
    EnvelopePoint(double h, double d) { hz = h; db = d; }
}

class EnvelopeBuilder {

    static double[] build(
            EnvelopePoint[] shape,
            int fftSize,
            double sampleRate
    ) {
        int bins = fftSize / 2;
        double[] env = new double[bins];

        for (int i = 0; i < bins; i++) {
            double freq = i * sampleRate / fftSize;
            double db = interpolate(shape, freq);
            env[i] = Math.pow(10.0, db / 20.0);
        }
        return env;
    }

    static double interpolate(EnvelopePoint[] p, double f) {
        if (f <= p[0].hz) return p[0].db;
        if (f >= p[p.length - 1].hz) return p[p.length - 1].db;

        for (int i = 0; i < p.length - 1; i++) {
            if (f >= p[i].hz && f <= p[i+1].hz) {
                double t = (f - p[i].hz) / (p[i+1].hz - p[i].hz);
                return p[i].db + t * (p[i+1].db - p[i].db);
            }
        }
        return -100;
    }

    static void smooth(double[] e) {
        double[] t = e.clone();
        for (int i = 1; i < e.length - 1; i++)
            e[i] = (t[i-1] + t[i] + t[i+1]) / 3.0;
    }
}
