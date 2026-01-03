package com.fpetrola.z80.t2;

public class NoiseReductionApp {

    public static void main(String[] args) throws Exception {

        String in = "/home/fernando/Documents/realtime/cassettes/rc0.wav";
        String out = "/home/fernando/Documents/realtime/cassettes/rc0_clean.wav";

        // 1) Leer WAV
        WavData wav = WavIO.read(in);

        // 2) Shape GoldWave (Hz, dB)
        EnvelopePoint[] shape = {
            new EnvelopePoint(20,     -20.9),
            new EnvelopePoint(96.9,    -77.2),
            new EnvelopePoint(634.8,    -56.3),
            new EnvelopePoint(6290.8,   -46.5),
            new EnvelopePoint(25081.6,  -12.8),
            new EnvelopePoint(100000, -100)
        };

        int fftSize = 11;

        // 3) Construir envelope
        double[] envelope = EnvelopeBuilder.build(
                shape,
                fftSize,
                wav.sampleRate
        );

        EnvelopeBuilder.smooth(envelope);

        // 4) Noise Reduction
        NoiseReducer nr = new NoiseReducer(fftSize);
        float[] cleaned = nr.process(wav.samples, envelope);

        // 5) Guardar WAV
        WavIO.write(out, cleaned, wav.sampleRate);

        System.out.println("Listo.");
    }
}
