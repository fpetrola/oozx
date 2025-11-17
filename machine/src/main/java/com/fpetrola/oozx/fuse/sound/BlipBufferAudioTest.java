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

package com.fpetrola.oozx.fuse.sound;

import javax.sound.sampled.*;
import java.util.Arrays;

public class BlipBufferAudioTest {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_MSEC = 100; // 100ms de buffer (~4 frames)
    private static final int FRAME_CLOCKS = 29780; // ~60 FPS @ 1.789773 MHz (NES)
    private static final int CLOCK_RATE = 1789773; // NES CPU clock

    public static void main(String[] args) throws LineUnavailableException, InterruptedException {
        // === 1. Configurar BlipBuffer ===
        BlipBuffer blip = new BlipBuffer();
        String error = blip.setSampleRate(SAMPLE_RATE, BUFFER_MSEC);
        if (error != null) {
            System.err.println("Error: " + error);
            return;
        }
        blip.setClockRate(CLOCK_RATE);

        // === 2. Configurar BlipSynth (onda cuadrada) ===
        BlipSynth synth = new BlipSynth();
        synth.setOutput(blip);
        synth.setVolume(0.9);           // 70% volumen
        synth.setTrebleEq(-3.0);        // -3 dB treble (suave)
        blip.setBassFreq(200);          // Filtro paso bajo a 200 Hz

        // === 3. Configurar línea de audio ===
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format, SAMPLE_RATE * 2); // 2 segundos de buffer
        line.start();

        System.out.println("Reproduciendo onda cuadrada (440 Hz) con BlipBuffer... (presiona Ctrl+C para salir)");

        int time = 0;
        int amplitude = 0;
        int period = CLOCK_RATE / 1440; // 440 Hz
        int duty = period / 2;

        while (true) {
            // === Simular un frame de 1/60 seg ===
            for (int i = 0; i < FRAME_CLOCKS; i++) {
                int phase = time % period;
                int newAmp = (phase < duty) ? 16384 : 0; // 50% duty cycle
                if (newAmp != amplitude) {
                    synth.update(time, newAmp);
                    amplitude = newAmp;
                }
                time++;
            }

            blip.endFrame(FRAME_CLOCKS);

            // === Leer muestras y enviar al audio ===
            int[] samples = new int[1024];
            long read = blip.readSamples(samples, samples.length, false);

            if (read > 0) {
                byte[] audioBytes = new byte[(int) read * 2];
                for (int i = 0; i < read; i++) {
                    audioBytes[i * 2] = (byte) (samples[i] & 0xFF);
                    audioBytes[i * 2 + 1] = (byte) (samples[i] >> 8);
                }
                line.write(audioBytes, 0, audioBytes.length);
            }

            // Pequeña pausa para no saturar CPU
            Thread.sleep(1);
        }
    }
}