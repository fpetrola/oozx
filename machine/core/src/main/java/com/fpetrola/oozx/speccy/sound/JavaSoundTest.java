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

package com.fpetrola.oozx.speccy.sound;

public class JavaSoundTest {
    public static void main(String[] args) throws InterruptedException {
        JavaSoundDevice sound = new JavaSoundDevice();

        int[] freq = {44100};
        int[] stereo = {1}; // 1 = estéreo

        // Opciones como en ALSA: "buffer=8192,frames=4,verbose"
        if (sound.sound_lowlevel_init("buffer=8192,frames=4,verbose", freq, stereo) != 0) {
            System.err.println("Error inicializando audio");
            return;
        }

        int len = 1024;
        int[] buffer = new int[len];

        // Generar tono 440 Hz (onda cuadrada)
        int period = freq[0] / 440;
        int samples = 0;
        long start = System.currentTimeMillis();

        System.out.println("Reproduciendo 5 segundos...");

        while (System.currentTimeMillis() - start < 5000) {
            for (int i = 0; i < len-1; i += stereo[0]) {
                int phase = samples % period;
                int amp = (phase < period / 2) ? (int) 8000 : (int) -8000;
                buffer[i] = amp;
                if (stereo[0] == 1) buffer[i + 1] = amp;
                samples++;
            }
            sound.sound_lowlevel_frame(buffer, len);
        }

        sound.sound_lowlevel_end();
        System.out.println("Fin.");
    }
}