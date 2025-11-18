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

import javax.sound.sampled.*;
import java.nio.ByteBuffer;

// ================================================================
// DEMO COMPLETO: Sonido band-limited tipo NES con Java Sound
// ================================================================

public class BlipBufferDemo {

    // Configuración de audio
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_MS = 100;  // 100 ms de buffer (baja latencia)

    public static void main(String[] args) throws Exception {
        new BlipBufferDemo().run();
    }

    void run() throws Exception {
        // 1. Crear BlipBuffer
        BlipBuffer blip = new BlipBuffer();
        String err = blip.setSampleRate(SAMPLE_RATE, BUFFER_MS);
        if (err != null) {
            System.err.println("Error: " + err);
            return;
        }
        blip.clockRate(1789773); // Frecuencia típica de NES / APU

        // 2. Crear sintetizador de alta calidad (calidad "good" = 12)
        BlipSynth synth = new BlipSynth(BlipBuffer.BLIP_GOOD_QUALITY, 15); // rango típico de onda cuadrada NES
        synth.output(blip);
        synth.volume(0.7);
        synth.trebleEq(new BlipEq(-13.0)); // Sonido clásico de NES (un poco oscuro)

        // 3. Abrir línea de audio
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, SAMPLE_RATE * 2 * 2); // buffer interno de ~90 ms
        line.start();

        System.out.println("Reproduciendo tonos tipo NES... (presiona Ctrl+C para salir)");

        long clock = 0;
        long frameClocks = blip.clockRate() / 60; // 60 FPS

        // Frecuencias de notas (aprox. escala de Do mayor)
        int[] notes = { 262, 294, 330, 349, 392, 440, 494, 523 }; // C4 a C5

        int noteIndex = 0;
        boolean goingUp = true;

        short[] audioBuffer = new short[2048];

        while (true) {
            // --- Generar una nota cada medio segundo ---
            if ((clock % (frameClocks * 30)) == 0) { // cada 0.5 segundos
                int freq = notes[noteIndex];
                long period = blip.clockRate() / freq / 2; // onda cuadrada 50% duty

                // Subir nota
                synth.update(clock, 16384);
                clock += period;
                synth.update(clock, 0);
                clock += period;

                // Cambiar nota
                if (goingUp) {
                    noteIndex++;
                    if (noteIndex == notes.length - 1) goingUp = false;
                } else {
                    noteIndex--;
                    if (noteIndex == 0) goingUp = true;
                }
            }

            // Avanzar un frame de video (60 Hz)
            blip.endFrame(frameClocks);
            clock += frameClocks;

            // Leer muestras generadas y enviar al altavoz
            while (blip.samplesAvail() > 0) {
                long read = blip.readSamples(audioBuffer, 0, audioBuffer.length, false);
                if (read > 0) {
                    byte[] byteBuf = new byte[(int) read * 2];
                    ByteBuffer.wrap(byteBuf).asShortBuffer().put(audioBuffer, 0, (int) read);
                    line.write(byteBuf, 0, byteBuf.length);
                }
            }

            // Pequeña pausa para no saturar CPU
            Thread.sleep(1);
        }
    }
}