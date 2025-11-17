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

public class BlipBufferWaveTest {

    private static final int SAMPLE_RATE = 44100;
    private static final int CLOCK_RATE = 1789773; // NES clock
    private static final int FRAME_CLOCKS = 29780; // ~60 FPS

    public static void main(String[] args) throws LineUnavailableException, InterruptedException {
        // === Configurar BlipBuffer ===
        BlipBuffer blip = new BlipBuffer();
        String err = blip.setSampleRate(SAMPLE_RATE, 200); // 200 ms buffer
        if (err != null) {
            System.err.println("Error: " + err);
            return;
        }
        blip.setClockRate(CLOCK_RATE);

        // === Configurar BlipSynth ===
        BlipSynth synth = new BlipSynth();
        synth.setOutput(blip);
        synth.setVolume(0.6);
        synth.setTrebleEq(-2.0);
        blip.setBassFreq(150);

        // === Configurar audio ===
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, SAMPLE_RATE * 2);
        line.start();

        System.out.println("Reproduciendo: 2s cuadrada + 3s senoidal (440 Hz)");

        long time = 0;
        int amplitude = 0;
        int freq = 440;
        int period = CLOCK_RATE / freq;
        int duty = period / 2;

//        // === FASE 1: Onda cuadrada - 2 segundos ===
//        System.out.println("→ Onda cuadrada (2 segundos)...");
//        long endTime1 = time + CLOCK_RATE * 2; // 2 segundos
//
//        while (time < endTime1) {
//            for (int i = 0; i < FRAME_CLOCKS && time < endTime1; i++) {
//                int phase = (int) (time % period);
//                int newAmp = (phase < duty) ? 16384 : 0;
//                if (newAmp != amplitude) {
//                    synth.update(time, newAmp);
//                    amplitude = newAmp;
//                }
//                time++;
//            }
//            blip.endFrame(Math.min(FRAME_CLOCKS, endTime1 - (time - FRAME_CLOCKS)));
//            drainBuffer(blip, line);
//        }

        // === FASE 2: Onda senoidal - 3 segundos ===
        System.out.println("→ Onda senoidal (3 segundos)...");
        long endTime2 = time + CLOCK_RATE * 3; // 3 segundos

        while (time < endTime2) {
            for (int i = 0; i < FRAME_CLOCKS && time < endTime2; i++) {
                double t = (time % period) / (double) period;
                int newAmp = (int) (Math.sin(t * 2 * Math.PI) * 16383);
                if (Math.abs(newAmp - amplitude) > 64) { // evitar ruido de cuantización
                    synth.update(time, newAmp);
                    amplitude = newAmp;
                }
                time++;
            }
            blip.endFrame(Math.min(FRAME_CLOCKS, endTime2 - (time - FRAME_CLOCKS)));
            drainBuffer(blip, line);
        }

        // === Drenar buffer restante ===
        System.out.println("Finalizando...");
        while (blip.samplesAvail() > 0) {
            drainBuffer(blip, line);
            Thread.sleep(10);
        }

        line.drain();
        line.close();
        System.out.println("¡Listo! 5 segundos reproducidos.");
    }

    // === Extraer muestras y enviar al audio ===
    private static void drainBuffer(BlipBuffer blip, SourceDataLine line) {
        int[] samples = new int[512];
        long read = blip.readSamples(samples, samples.length, false);
        if (read > 0) {
            byte[] audio = new byte[(int) read * 2];
            for (int i = 0; i < read; i++) {
                audio[i * 2] = (byte) (samples[i] & 0xFF);
                audio[i * 2 + 1] = (byte) (samples[i] >> 8);
            }
            line.write(audio, 0, audio.length);
        }
    }
}