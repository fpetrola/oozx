package com.fpetrola.z80.t2;


import org.jtransforms.fft.DoubleFFT_1D;

import static java.lang.Math.*;

class NoiseReducer {

    private final int fftSize, hop;
    private final double[] window;
    private final DoubleFFT_1D fft;

    private final double strength = 0.75;
    private final double minGain = 0.1;
    private final double eps = 1e-9;

    NoiseReducer(int size) {
        fftSize = size;
        hop = size / 2;
        window = hann(size);
        fft = new DoubleFFT_1D(size);
    }

    float[] process(float[] in, double[] env) {

        double[] out = new double[in.length + fftSize];
        double[] buf = new double[fftSize * 2];

        for (int p = 0; p + fftSize < in.length; p += hop) {

            for (int i = 0; i < fftSize; i++) {
                buf[2*i] = in[p+i] * window[i];
                buf[2*i+1] = 0;
            }

            fft.complexForward(buf);

            for (int f = 0; f < fftSize/2; f++) {

                int re = 2*f, im = re+1;
                double mag = Math.hypot(buf[re], buf[im]);
                double ph = Math.atan2(buf[im], buf[re]);

                double ratio = mag / (env[f] + eps);
                double gain = ratio > 1
                        ? 1 - strength / ratio
                        : minGain;

                double magDb = 20 * log10(mag + eps);
                double noiseDb = 20 * log10(env[f] + eps);

                double reducedDb = magDb - strength * max(0, noiseDb - magDb);
                mag = pow(10, reducedDb / 20);

//                mag *= gain;
                buf[re] = mag * Math.cos(ph);
                buf[im] = mag * Math.sin(ph);
            }

            fft.complexInverse(buf, true);

            for (int i = 0; i < fftSize; i++)
                out[p+i] += buf[2*i] * window[i];
        }

        float[] res = new float[in.length];
        for (int i = 0; i < res.length; i++)
            res[i] = (float) out[i];

        return res;
    }

    private static double[] hann(int n) {
        double[] w = new double[n];
        for (int i = 0; i < n; i++)
            w[i] = 0.5 * (1 - Math.cos(2*Math.PI*i/(n-1)));
        return w;
    }
}
