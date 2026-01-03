package com.fpetrola.z80.t2;

import javax.sound.sampled.*;
import java.io.*;

class WavIO {

    static WavData read(String path) throws Exception {

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
        AudioFormat f = ais.getFormat();

        if (f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            throw new IllegalArgumentException("Solo PCM signed");

        byte[] bytes = ais.readAllBytes();
        int samples = bytes.length / 2;

        float[] data = new float[samples];
        for (int i = 0; i < samples; i++) {
            int lo = bytes[2*i] & 0xff;
            int hi = bytes[2*i+1];
            short v = (short) (hi << 8 | lo);
            data[i] = v / 32768f;
        }

        return new WavData(data, f.getSampleRate());
    }

    static void write(String path, float[] samples, float sr) throws Exception {

        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) (Math.max(-1, Math.min(1, samples[i])) * 32767);
            bytes[2*i] = (byte) v;
            bytes[2*i+1] = (byte) (v >> 8);
        }

        AudioFormat format = new AudioFormat(
                sr, 16, 1, true, false
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        AudioInputStream ais = new AudioInputStream(bais, format, samples.length);

        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(path));
    }
}

class WavData {
    final float[] samples;
    final float sampleRate;

    WavData(float[] s, float sr) {
        samples = s;
        sampleRate = sr;
    }
}
