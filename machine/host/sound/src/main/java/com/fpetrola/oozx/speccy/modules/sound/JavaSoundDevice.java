/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.modules.sound;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The system's audio line, through javax.sound.sampled: the same on every platform.
 */
public class JavaSoundDevice implements SoundCard {

  private SourceDataLine line;
  private int channels;
  private int frameSize;
  private boolean verbose;
  private boolean firstOpen = true;
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
   * @param device     device name, may be null or "default"
   * @param freqPtr    sample rate; overwritten with the rate actually opened if unsupported
   * @param stereoPtr  1 = stereo, 0 = mono; overwritten if the requested one is unsupported
   * @return 0 on success, 1 on failure
   */
  public int open(String device, int[] freqPtr, int[] stereoPtr) {
    LineOptions options = new LineOptions(device);
    verbose = options.verbose();
    if (verbose && firstOpen) {
      System.out.println("JavaSoundDevice: Iniciando con device='" + device + "'");
    }
    try {
      return open(options, freqPtr, stereoPtr);
    } catch (LineUnavailableException e) {
      System.err.println("No se pudo abrir la línea de audio: " + e.getMessage());
      return 1;
    } catch (RuntimeException e) {
      System.err.println("Error inicializando JavaSound: " + e.getMessage());
      e.printStackTrace();
      return 1;
    }
  }

  private int open(LineOptions options, int[] freqPtr, int[] stereoPtr) throws LineUnavailableException {
    int freq = freqPtr[0];
    int numPeriods = options.number("frames", 3);
    int bufferFrames = options.number("buffer", 0);
    int periodSize = bufferFrames > 0 ? bufferFrames / numPeriods : freq / 100; // ALSA also caps latency at 100 Hz
    int bufferSizeInFrames = periodSize * numPeriods;
    Optional<AudioFormat> supported = supportedFormat(freq, stereoPtr, bufferSizeInFrames);
    if (supported.isEmpty()) {
      System.err.println("Formato no soportado: " + freq + " Hz, " + (stereoPtr[0] != 0 ? "stereo" : "mono"));
      return 1;
    }
    AudioFormat format = supported.get();
    int bufferSizeInBytes = bufferSizeInFrames * frameSize;
    // The line this replaces is let go of first. Opening a second one over the field left the
    // first one open, and a machine change opens one every time.
    close();
    line = AudioSystem.getSourceDataLine(format);
    line.open(format, bufferSizeInBytes);
    line.start();
    float actualRate = line.getFormat().getSampleRate();
    if (firstOpen && Math.abs(actualRate - freq) > 1) {
      System.out.printf("Frecuencia %d Hz no soportada. Usando %.0f Hz.\n", freq, actualRate);
      freqPtr[0] = (int) actualRate;
    }
    if (verbose && firstOpen) {
      System.out.printf("JavaSound: %d Hz, %s, buffer=%d bytes (%d frames, %d periods)\n",
          (int) actualRate, channels == 2 ? "stereo" : "mono", bufferSizeInBytes, bufferSizeInFrames, numPeriods);
    }
    firstOpen = false;
    return 0;
  }

  /** The format asked for, or the other channel count when the line takes only that; empty when neither. */
  private Optional<AudioFormat> supportedFormat(int freq, int[] stereoPtr, int bufferSizeInFrames) {
    int asked = stereoPtr[0] != 0 ? 2 : 1;
    for (int tried : new int[] {asked, 3 - asked}) {
      channels = tried;
      frameSize = channels * 2;
      AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, freq, 16, channels, frameSize, freq, false);
      if (AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format, bufferSizeInFrames * frameSize))) {
        stereoPtr[0] = channels - 1;
        return Optional.of(format);
      }
    }
    return Optional.empty();
  }

  /** What the device string carries besides a name: "buffer=", "frames=" and "verbose", comma separated. */
  private static final class LineOptions {
    private final Map<String, String> values = new HashMap<>();
    private boolean verbose;

    LineOptions(String device) {
      String text = device == null ? "" : device;
      for (String part : text.split(",")) {
        String[] pair = part.trim().split("=", 2);
        verbose |= pair[0].equals("verbose");
        values.put(pair[0], pair.length == 2 ? pair[1] : "");
      }
    }

    boolean verbose() {
      return verbose;
    }

    int number(String name, int fallback) {
      String value = values.getOrDefault(name, "");
      try {
        return value.isEmpty() ? fallback : Integer.parseInt(value);
      } catch (NumberFormatException e) {
        System.err.println("Bad ALSA " + name + " '" + value + "' (" + e.getMessage() + "), using " + fallback);
        return fallback;
      }
    }
  }

  /**
   * @param data samples as short[] values, interleaved L,R,L,R... for stereo, or mono
   * @param len  sample count, not frame count (frames = len / channels)
   */
  public void play(int[] data, int len) {
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

  public void close() {
    if (line != null) {
      line.drain();
      line.close();
    }
  }

  public int getBufferSize() {
    return line != null ? line.getBufferSize() : 0;
  }

  public boolean isOpen() {
    return line != null && line.isOpen();
  }
}