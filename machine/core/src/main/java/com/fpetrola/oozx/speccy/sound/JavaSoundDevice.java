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


import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Implementación de bajo nivel de audio para Java usando javax.sound.sampled
 * Equivalente a alsasound.c (ALSA) pero multiplataforma.
 */
public class JavaSoundDevice {

  private SourceDataLine line;
  private AudioFormat format;
  private int channels;
  private int frameSize;
  private boolean verbose = false;
  private static boolean firstInit = true;
  private boolean dropWhenAhead;
  private byte[] audioBytes = new byte[0];

  /**
   * Whether a frame the card has no room for is dropped rather than waited on. Waiting is what
   * holds the machine to real time; dropping is what lets it run ahead and still be heard.
   */
  public void dropWhenAhead(boolean drop) {
    dropWhenAhead = drop;
  }

  /**
   * Inicializa el dispositivo de audio.
   *
   * @param device     Nombre del dispositivo (puede ser null o "default")
   * @param freqPtr    Frecuencia de muestreo (modificable si no es soportada)
   * @param stereoPtr  1 = estéreo, 0 = mono (modificable si no es soportado)
   * @return 0 si éxito, 1 si error
   */
  public int sound_lowlevel_init(String device, int[] freqPtr, int[] stereoPtr) {
    int freq = freqPtr[0];
    int stereo = stereoPtr[0];
    this.verbose = device != null && device.contains("verbose");

    if (verbose && firstInit) {
      System.out.println("JavaSoundDevice: Iniciando con device='" + device + "'");
    }

    try {
      // === 1. Parsear opciones (como en ALSA) ===
      int bufferFrames = 0;
      int numPeriods = 3; // NUM_FRAMES
      int availMin = 0;

      if (device != null && !device.isEmpty()) {
        String[] parts = device.split(",");
        for (String part : parts) {
          part = part.trim();
          if (part.startsWith("buffer=")) {
            try {
              bufferFrames = Integer.parseInt(part.substring(7));
            } catch (NumberFormatException e) {
              System.err.println("Bad ALSA buffer size, using default");
            }
          } else if (part.startsWith("frames=")) {
            try {
              numPeriods = Integer.parseInt(part.substring(7));
            } catch (NumberFormatException e) {
              System.err.println("Bad ALSA frames, using default (3)");
            }
          } else if (part.startsWith("avail=")) {
            try {
              availMin = Integer.parseInt(part.substring(6));
            } catch (NumberFormatException e) {
              System.err.println("Bad ALSA avail_min, using default");
            }
          } else if (part.equals("verbose")) {
            this.verbose = true;
          } else if (!part.isEmpty() && !part.startsWith("'")) {
            device = part; // nombre del mixer
          }
        }
      }

      // === 2. Configurar formato ===
      channels = stereo != 0 ? 2 : 1;
      frameSize = channels * 2; // 16-bit

      format = new AudioFormat(
          AudioFormat.Encoding.PCM_SIGNED,
          freq,
          16,
          channels,
          frameSize,
          freq,
          false // little-endian
      );

      // === 3. Calcular tamaño de buffer ===
      int periodSize;
      if (bufferFrames > 0) {
        periodSize = bufferFrames / numPeriods;
      } else {
        float hz = 100.0f; // como en ALSA: max 100 Hz de latencia
        periodSize = freq / (int) hz;
      }

      int bufferSizeInFrames = periodSize * numPeriods;
      int bufferSizeInBytes = bufferSizeInFrames * frameSize;

      // === 4. Abrir línea ===
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, bufferSizeInBytes);
      if (!AudioSystem.isLineSupported(info)) {
        // Intentar con mono/estéreo opuesto
        channels = channels == 2 ? 1 : 2;
        stereoPtr[0] = channels - 1;
        frameSize = channels * 2;
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, freq, 16, channels, frameSize, freq, false);
        info = new DataLine.Info(SourceDataLine.class, format, bufferSizeInBytes);
        if (!AudioSystem.isLineSupported(info)) {
          System.err.println("Formato no soportado: " + freq + " Hz, " + (stereo != 0 ? "stereo" : "mono"));
          return 1;
        }
      }

      line = (SourceDataLine) AudioSystem.getLine(info);
      line.open(format, bufferSizeInBytes);
      line.start();

      // === 5. Ajustar frecuencia si fue modificada ===
      float actualRate = line.getFormat().getSampleRate();
      if (firstInit && Math.abs(actualRate - freq) > 1) {
        System.out.printf("Frecuencia %d Hz no soportada. Usando %.0f Hz.\n", freq, actualRate);
        freqPtr[0] = (int) actualRate;
      }

      if (verbose && firstInit) {
        System.out.printf("JavaSound: %d Hz, %s, buffer=%d bytes (%d frames, %d periods)\n",
            (int) actualRate, channels == 2 ? "stereo" : "mono",
            bufferSizeInBytes, bufferSizeInFrames, numPeriods);
      }

      firstInit = false;
      return 0;

    } catch (LineUnavailableException e) {
      System.err.println("No se pudo abrir la línea de audio: " + e.getMessage());
      return 1;
    } catch (Exception e) {
      System.err.println("Error inicializando JavaSound: " + e.getMessage());
      e.printStackTrace();
      return 1;
    }
  }

  /**
   * Reproduce un frame de audio.
   *
   * @param data Datos en formato: short[] interleaved (L,R,L,R...) o mono
   * @param len  Cantidad de samples (no frames). En C: len /= ch
   */
  public void sound_lowlevel_frame(int[] data, int len) {
    if (line == null || !line.isOpen()) return;
    int frames = len / channels;
    int bytesToWrite = frames * frameSize;
    // A frame is played whole or not at all: dropping the tail of every frame would play the
    // heads one after another, which is the sped-up buzz again.
    if (dropWhenAhead && line.available() < bytesToWrite) return;
    convertToBytes(data, len);
    int written = 0;
    while (written < bytesToWrite) {
      int ret = line.write(audioBytes, written, bytesToWrite - written);
      if (ret < 0) {
        if (verbose) System.err.println("JavaSound: underrun!");
        line.flush();
        break;
      }
      written += ret;
    }
  }

  private void convertToBytes(int[] data, int len) {
    if (audioBytes.length < len * 2) audioBytes = new byte[len * 2];
    for (int i = 0; i < len; i++) {
      int idx = i * 2;
      audioBytes[idx] = (byte) (data[i] & 0xFF);
      audioBytes[idx + 1] = (byte) (data[i] >> 8 & 0xFF);
    }
  }

  /**
   * Cierra el dispositivo de audio.
   */
  public void sound_lowlevel_end() {
    if (line != null) {
      line.drain();
      line.close();
//      line = null;
    }
  }

  // === Métodos auxiliares (opcionales) ===
  public int getBufferSize() {
    return line != null ? line.getBufferSize() : 0;
  }

  public boolean isActive() {
    return line != null && line.isActive();
  }
}