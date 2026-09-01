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
import java.util.ArrayList;
import java.util.List;

/**
 * What is coming in through the sound card, kept as the last minute of it.
 * <p>
 * The counterpart of {@link JavaSoundDevice}, which only ever spoke: this listens, so a real
 * cassette player with its lead in the line input can be watched and, later, read.
 * <p>
 * The samples live in a ring, and positions into it are counted from the moment recording
 * started rather than from the start of the array. A view can then hold a position and go on
 * meaning the same instant of sound while the ring turns underneath it; asking for one that has
 * already been overwritten answers silence rather than the wrong noise.
 */
public class AudioIn {

  public static final int RATE = 44100;

  /** How much is kept. Older than this and a tape is past being worth looking at. */
  private static final int SECONDS = 60;

  private static final AudioFormat FORMAT =
      new AudioFormat(RATE, 16, 1, true, false);

  private final short[] ring = new short[RATE * SECONDS];
  private volatile long written;
  private TargetDataLine line;
  private volatile SourceDataLine monitor;
  private Thread reader;
  private volatile boolean listening;

  /**
   * The inputs this computer offers, the ones that record only listed first.
   * <p>
   * Java Sound will not say which of its mixers is a microphone and which is a loudspeaker
   * wearing an input's clothes: the one this machine calls its default input records happily
   * and every sample is zero. What does separate them is that the impostors can also PLAY. A
   * device that can only record is an input and nothing else, so those go to the top and the
   * first of them is what listening starts on.
   * <p>
   * It is a rule of thumb, not a fact about sound cards, which is why the list keeps the rest
   * and why {@link #peak} exists: whoever is watching can see in a second which one hears.
   */
  public static List<String> inputs() {
    DataLine.Info records = new DataLine.Info(TargetDataLine.class, FORMAT);
    DataLine.Info plays = new DataLine.Info(SourceDataLine.class, FORMAT);
    List<String> onlyRecords = new ArrayList<>();
    List<String> alsoPlays = new ArrayList<>();
    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
      Mixer mixer = AudioSystem.getMixer(info);
      if (mixer.isLineSupported(records)) {
        (mixer.isLineSupported(plays) ? alsoPlays : onlyRecords).add(info.getName());
      }
    }
    onlyRecords.addAll(alsoPlays);
    return onlyRecords;
  }

  /**
   * Starts listening to one of {@link #inputs}.
   * <p>
   * Named, not left to the default: what this computer calls its default input is a wrapper
   * round the PLAYBACK device, and it delivers samples happily - all of them zero. An input
   * that hears nothing looks exactly like a quiet room, so the first is taken instead and
   * {@link #peak} is there to say whether it is hearing anything at all.
   */
  public void open(String input) throws LineUnavailableException {
    close();
    DataLine.Info wanted = new DataLine.Info(TargetDataLine.class, FORMAT);
    List<String> available = inputs();
    if (input == null && available.isEmpty()) {
      throw new LineUnavailableException("this computer offers no input to record from");
    }
    line = (TargetDataLine) mixerNamed(input == null ? available.get(0) : input).getLine(wanted);
    line.open(FORMAT);
    line.start();
    written = 0;
    listening = true;
    reader = new Thread(this::read, "audio in");
    reader.setDaemon(true);
    reader.start();
  }

  /**
   * Plays what comes in, so a cassette can be heard as well as seen.
   * <p>
   * Without it there is no way to tell a tape that is loading from one that is not turning: the
   * picture says there is signal, and only the sound says it is the right signal. Watch out with
   * a microphone rather than a lead - the speakers feed straight back into it.
   */
  public void hear(boolean wanted) throws LineUnavailableException {
    if (wanted && monitor == null) {
      SourceDataLine out = AudioSystem.getSourceDataLine(FORMAT);
      out.open(FORMAT);
      out.start();
      monitor = out;
    } else if (!wanted && monitor != null) {
      monitor.stop();
      monitor.close();
      monitor = null;
    }
  }

  public boolean isHeard() {
    return monitor != null;
  }

  public void close() {
    listening = false;
    try {
      hear(false);
    } catch (LineUnavailableException impossible) {
      monitor = null;
    }
    if (line != null) {
      line.stop();
      line.close();
      line = null;
    }
    reader = null;
  }

  public boolean isListening() {
    return listening;
  }

  /** How many samples have come in since it started listening: the position of the head. */
  public long written() {
    return written;
  }

  /** How many of them are still held. Older ones have been written over. */
  public int held() {
    return ring.length;
  }

  /**
   * One sample, by its position counted from the start of listening.
   * <p>
   * Silence for anything not held, which is what a view showing a stretch that runs off either
   * end of the ring should draw there.
   */
  public short at(long position) {
    long head = written;
    if (position < 0 || position >= head || position < head - ring.length) {
      return 0;
    }
    return ring[(int) Math.floorMod(position, ring.length)];
  }

  /**
   * The loudest thing heard in the last stretch, 0 to 1.
   * <p>
   * Which input is the microphone and which is a dead socket cannot be told from their names,
   * so this is how somebody tells: pick one, watch the number, keep the one that moves.
   */
  public double peak(double seconds) {
    long head = written;
    long from = Math.max(0, head - (long) (seconds * RATE));
    int loudest = 0;
    for (long at = from; at < head; at++) {
      loudest = Math.max(loudest, Math.abs(at(at)));
    }
    return loudest / (double) Short.MAX_VALUE;
  }

  /**
   * Whether the signal is on the high side just now, which is what an ear line reads.
   * <p>
   * A bare zero crossing: enough to see a tape's edges arrive, not yet enough to read a noisy
   * one, which wants hysteresis around the crossing so that hiss on a quiet stretch does not
   * come out as a stream of edges.
   */
  public boolean high() {
    return at(written - 1) > 0;
  }

  private static Mixer mixerNamed(String name) throws LineUnavailableException {
    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
      if (info.getName().equals(name)) {
        return AudioSystem.getMixer(info);
      }
    }
    throw new LineUnavailableException("no input called " + name);
  }

  private void read() {
    byte[] bytes = new byte[4096];
    while (listening && line != null) {
      int got = line.read(bytes, 0, bytes.length);
      SourceDataLine out = monitor;
      // Only what fits: writing more than there is room for blocks until it has been played,
      // and a reader that waits on the speakers stops keeping up with the tape.
      if (out != null && out.available() >= got) {
        out.write(bytes, 0, got);
      }
      for (int i = 0; i + 1 < got; i += 2) {
        // Little endian, as the format above asks for: low byte first, and only the low one
        // masked - the high one carries the sign, which is the whole point of a waveform.
        ring[(int) Math.floorMod(written, ring.length)] =
            (short) ((bytes[i] & 0xFF) | (bytes[i + 1] << 8));
        written++;
      }
    }
  }
}
