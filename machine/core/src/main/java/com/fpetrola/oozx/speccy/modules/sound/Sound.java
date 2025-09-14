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

package com.fpetrola.oozx.speccy.modules.sound;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipBuffer;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Everything making a noise on the machine, mixed once a frame and handed to the card.
 * <p>
 * The seam is the frame: a source is asked once a frame for what it made, and the mix is the
 * sum. A frame of output is the frame's T-states at the card's rate, sized for the machine's own
 * clock and the speed it runs at, so that at three times real time the same frame is a third as
 * many samples and a note comes out three times as high - sped up, as a tape would be.
 */
@Singleton
public class Sound implements AudioOutput {
  /** The speakers a Spectrum's sound comes out of, and what each does to it. */
  public enum Speakers {
    SMALL(new Colouring(200, -37.0)), LARGE_TV(new Colouring(1000, -67.0)), NONE(new Colouring(0, 0.0));

    final Colouring colouring;

    Speakers(Colouring colouring) {
      this.colouring = colouring;
    }
  }

  /** Handed out because whether sound is on is this module's to say, and windows and tests ask it. */
  public final Output output;
  private final Speed speed;
  private final Supplier<Machine> machine;
  private SoundCard card;
  private final int sampleRate = 44100;
  private final List<AudioSource> sources = new ArrayList<>();
  private Colouring speaker = Speakers.SMALL.colouring;
  /** How loud the whole machine is, in percent; the sources keep their own balance under it. */
  private int volume = 100;
  /** The machine's clock at the speed it runs at: what a frame's T-states are turned into samples by. */
  private long clock;
  private int frameSize;
  /** The frame of the machine {@link #frameSize} was worked out for. */
  private int sizedFor;
  private int[] mix = new int[0];

  @Inject
  public Sound(Output output, Speed speed, SoundCard card, Provider<Machine> machine) {
    this.machine = Suppliers.memoize(machine::get);
    this.card = card;
    this.output = output;
    this.speed = speed;
  }

  /** A new machine: its sources arrive as its peripherals are switched on, and the card is opened again for its frame. */
  public void init() {
    sources.clear();
    card.close();
    sizeFor(machinesFrame());
  }

  /** Only the speed changed: the same sources go on playing, through a synth for the speed that is running now. */
  public void rebuildOutput() {
    sizeFor(machinesFrame());
  }

  private int machinesFrame() {
    return machine.get().current.getTimings().tstatesPerFrame();
  }

  /**
   * Sized for a frame of that length at the machine's clock and the speed. The card is opened
   * for a machine, not for a speed: a speed slid along its slider changes the frame at every
   * notch, and opening the card costs most of a second each time. Below real time the card,
   * waiting for room, is what holds the machine to the speed; above it the Timer holds it and
   * the card is told to drop what it has no room for, so that it never holds the machine back.
   */
  public void sizeFor(int frameTstates) {
    clock = machine.get().current.getTimings().processorSpeed() * speed.emulation / 100;
    frameSize = BlipBuffer.samplesInAFrame(sampleRate, clock, frameTstates);
    sizedFor = frameTstates;
    mix = new int[frameSize * 2];
    if (!card.isOpen()) {
      card.open(output.device, new int[]{sampleRate}, new int[]{1});
    }
    card.dropWhenAhead(speed.emulation > 100);
    sources.forEach(source -> source.takeOutputFrom(this));
  }

  public int volume() {
    return volume;
  }

  public void setVolume(int percent) {
    volume = Math.max(0, Math.min(100, percent));
  }

  public void speaker(Speakers which) {
    speaker = which.colouring;
    sources.forEach(source -> source.takeOutputFrom(this));
  }

  /** It has stopped making a noise here - unplugged, or the machine it belonged to is gone. */
  public void remove(AudioSource source) {
    sources.remove(source);
  }

  /** Something that makes a noise on this machine, from now until the machine changes. */
  public <T extends AudioSource> T add(T source) {
    sources.add(source);
    return source;
  }

  /**
   * A synth wired for this output, through the speaker. Its buffer holds a second: at two per
   * cent of real time a frame of the machine is most of a second of audio. A write past the end
   * of the frame is kept for the next only as far as the buffer's margin, a few samples, which
   * is as far past as a source is ever handed the clock.
   */
  @Override
  public BlipSynth newSynth(int volumePercent) {
    return new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, sampleRate, 1000, clock, speaker, loudness(volumePercent));
  }

  @Override
  public BlipSynth newFlatSynth(int volumePercent) {
    return new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, sampleRate, 1000, clock, speaker.flat(), loudness(volumePercent));
  }

  private static double loudness(int percent) {
    return Math.max(0, Math.min(100, percent)) / 100.0;
  }

  @Override
  public int frameSize() {
    return frameSize;
  }

  /**
   * The machine's frame is over: what was made in it goes to the card, whether or not the
   * output is on - the sources are emptied either way, or what they made piles up and comes out
   * later all at once. The frame that fills is the frame that empties: a machine that became a
   * +2A after the output was sized for a 48K, which is what loading a recording's snapshot does,
   * has a frame 1020 T-states longer, and keeping the size would leave the difference behind
   * every frame until the buffer overflowed.
   */
  public void frame() {
    int frameTstates = machinesFrame();
    if (frameTstates != sizedFor) rebuildOutput();
    Arrays.fill(mix, 0);
    int frames = sources.isEmpty() ? frameSize : 0;
    for (AudioSource source : sources) {
      source.endFrame(frameTstates);
      frames = Math.max(frames, source.mixInto(mix, frameSize));
    }
    int count = frames * 2;
    if (volume < 100) {
      for (int i = 0; i < count; i++) mix[i] = mix[i] * volume / 100;
    }
    if (output.enabled) card.play(mix, count);
  }

  /** The card is let go of: nothing plays until it is asked for again. */
  public void pause() {
    card.close();
  }

  public void unpause() {
    if (!card.isOpen()) card.open(output.device, new int[]{sampleRate}, new int[]{1});
  }

  public void end() {
    card.close();
  }

  public void close() {
    sources.forEach(AudioSource::close);
    card.close();
  }

  public SoundCard card() {
    return card;
  }

  public void setCard(SoundCard card) {
    this.card = card;
  }

  /** What comes out of the machine's speaker, and how the platform's audio is opened for it. */
  @Singleton
  public static class Output {
    public boolean enabled;
    /** How much the platform's audio layer buffers, in the terms that layer understands. */
    public String device;
    /** Whether the tape is heard while it loads, as it is on a real machine. */
    public boolean whileLoading;
  }
}
