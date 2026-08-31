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

package com.fpetrola.oozx.speccy;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.peripherals.*;
import com.fpetrola.oozx.speccy.sound.AudioSource;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.blip.BlipBuffer;
import com.fpetrola.oozx.speccy.sound.blip.BlipSynth;

import java.util.Arrays;

@Singleton
public class Sound implements ZxModule, MachineChangeListener {

  public void pause() {
    if (soundEnabled) {
      end();
    }
  }

  public void unpause() {
    init(settings.current.soundDevice);
  }

  public final Settings settings;
  private Movie movie;
  private IPeriph periph;
  private Tape tape;
  private final SpectrumZ80Clock clock;

  private JavaSoundDevice javaSoundDevice;

  private SpectrumMachine spectrumMachine;

@Inject
  public Sound(Settings settings, Movie movie, IPeriph periph, Tape tape, SpectrumZ80Clock clock,
               JavaSoundDevice javaSoundDevice) {
    this.javaSoundDevice = javaSoundDevice;
    this.settings = settings;
    this.movie = movie;
    this.periph = periph;
    this.tape = tape;
    this.clock = clock;
  }

  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }

  public boolean soundEnabled = false;
  private int soundFrameSize;
  private long effectiveSpeed;
  private int bass;
  private double treble;
  private int[] outputSamples;

  /** Everything making a noise on this machine, asked once a frame. */
  private final java.util.List<AudioSource> sources = new java.util.ArrayList<>();

  public int soundFreq = 44100;
  public int volumeBeeper = 100;
  public int volumeAY = 100;
  public int speakerType = 0; // 0=Small, 1=Large TV, 2=None
  private final double[] speakerTreble = {-37.0, -67.0, 0.0};

  private final int[] speakerBass = {200, 1000, 0};

  @Override
  public void start() {
    init(null);
  }

  /**
   * Starts the sound with a say in whether it comes out enabled. Never a lifecycle hook: Sound
   * is not registered as a startup module, and every caller passes a real argument — Boolean
   * false to come up muted, anything else to come up audible.
   */
  public void init(Object enabled) {
    int frameTstates = spectrumMachine.getTimings().tstatesPerFrame;
    initSound(3500000, frameTstates, enabled, true);
  }

  /** The emulator changed speed. The output is rebuilt for it; what is playing goes on playing. */
  public void speedChanged() {
    initSound(3500000, spectrumMachine.getTimings().tstatesPerFrame, true, false);
  }

  public void end() {
  }

  public boolean initSound(long cpuFrequency, int tstatesPerFrame, Object initContext) {
    return initSound(cpuFrequency, tstatesPerFrame, initContext, true);
  }

  public boolean initSound(long cpuFrequency, int tstatesPerFrame, Object initContext, boolean forANewMachine) {

    this.effectiveSpeed = cpuFrequency * settings.current.emulationSpeed / 100 * 2;

    int[] ints = {0};
    int[] soundFreqArray = {soundFreq};

    String device = "buffer=8192,frames=8";
    boolean b = lowlevelInit(device, soundFreqArray, ints);

    this.treble = speakerTreble[speakerType];
    this.bass = speakerBass[speakerType];
    double hz = (double) effectiveSpeed / tstatesPerFrame;
    soundFrameSize = (int) (soundFreq / hz) + 1;
    outputSamples = new int[soundFrameSize * 2];
    // A new machine brings its own sources, which arrive as its peripherals are switched on.
    if (forANewMachine) {
      sources.clear();
    } else {
      // Only the speed changed. The sources are the same ones and go on playing; what they
      // cannot keep is a synth built for the speed that is over.
      sources.forEach(source -> source.takeOutputFrom(this));
    }
    // The sound chip is in the list when the machine has one, and that is the whole question.
    // It used to be asked of a method here that answered true for every machine, so a 48K spent
    // two thousand two hundred ticks a frame synthesising a chip it has not got, into silence,
    // because its ports never reach one. A machine that has no chip now has no chip.

    if (!(initContext instanceof Boolean bool1) || bool1)
      soundEnabled = true;

    return true;
  }

  private double getVolume(int volume) {
    if (volume < 0) volume = 0;
    else if (volume > 100) volume = 100;

    return volume / 100.0;
  }


  /** Something that makes a noise on this machine, from now until the machine changes. */
  public <T extends AudioSource> T add(T source) {
    sources.add(source);
    return source;
  }

  /** A synth wired for this output, for whoever is going to make samples with it. */
  public BlipSynth newSynth(int volume) {
    return new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, soundFreq, 1000, effectiveSpeed, bass,
        getVolume(volume), treble);
  }

  public int frameSize() {
    return soundFrameSize;
  }

  public void frame() {
    int frameTstates = spectrumMachine.getTimings().tstatesPerFrame;
    if (!soundEnabled) return;

    for (AudioSource source : sources) {
      source.endFrame(frameTstates);
    }
    // Every source adds itself in, so the mix starts at nothing and none of them has to know
    // whether it went first.
    Arrays.fill(outputSamples, 0);
    int frames = 0;
    for (AudioSource source : sources) {
      frames = Math.max(frames, source.mixInto(outputSamples, soundFrameSize));
    }
    int count = frames * 2;

    if (settings.current.sound) {
      lowlevelFrame(outputSamples, (int) count);
    }
    if (movie.recording) {
      movie.addSound(outputSamples, (int) count);
    }
  }

  private boolean lowlevelInit(String device, int[] freqPtr, int[] stereoPtr) {
    return javaSoundDevice.sound_lowlevel_init(device, freqPtr, stereoPtr) != 0;
  }

  private void lowlevelEnd() {
    javaSoundDevice.sound_lowlevel_end();
  }

  private void lowlevelFrame(int[] data, int len) {

    javaSoundDevice.sound_lowlevel_frame(data, len);
  }

  /**
   * Advances one channel and answers what it is putting out.
   * <p>
   * In C this writes the level through a pointer. Translated with the pointer dropped it assigned
   * to its own parameter, which in Java is a local: the channel was advanced and the level thrown
   * away, so every channel read as whatever it had been. And the square wave was flipped by
   * negating it, which leaves nought as nought - it started low and stayed there.
   */

  public void close() {
    if (soundEnabled) {
      for (AudioSource source : sources) source.close();
      soundEnabled = false;
    }
  }

  public JavaSoundDevice getJavaSoundDevice() {
    return javaSoundDevice;
  }

  public void setJavaSoundDevice(JavaSoundDevice javaSoundDevice) {
    this.javaSoundDevice = javaSoundDevice;
  }
}
