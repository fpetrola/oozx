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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.machine.Options;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.ZxModule;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.oozx.fuse.sound.BlipBuffer;
import com.fpetrola.oozx.fuse.sound.BlipSynth;
import com.fpetrola.oozx.fuse.sound.JavaSoundDevice;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public class Sound implements ZxModule, MachineChangeListener {

  public final Settings settings;
  // Constants
  public final int AMPL_BEEPER = 50 * 256;
  public final int AMPL_TAPE = 2 * 256;
  public final int AMPL_AY_TONE = 24 * 256;
  public final int AY_CHANGE_MAX = 8000;
  public final int MIN_SPEED_PERCENTAGE = 2; // Adjusted for non-Win32
  public final int MAX_SPEED_PERCENTAGE = 500;
  //  private Timer timer;
  private Movie movie;
  private IPeriph periph;
  private Tape tape;
  private final SpectrumZ80Clock clock;
  private JavaSoundDevice javaSoundDevice = new JavaSoundDevice();
  private SpectrumMachine spectrumMachine;
  private int lastVal;
  private Audio audio;
  private AY8912Type ayConf;
  private AY8912 ay8912;

  public Sound(Settings settings, Movie movie, IPeriph periph, Tape tape, SpectrumZ80Clock clock) {
    this.settings = settings;
//    this.timer = timer;
    this.movie = movie;
    this.periph = periph;
    this.tape = tape;
    this.clock = clock;
    ayConf = new AY8912Type();
    audio = new Audio(ayConf);
    ay8912 = new AY8912();
  }

  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }

  public void sendFrame() {
    audio.sendAudioFrame();
  }

  // Stereo separation types
  public enum SoundStereoAY {
    SOUND_STEREO_AY_NONE,
    SOUND_STEREO_AY_ACB,
    SOUND_STEREO_AY_ABC
  }

  //  fields
  public boolean soundEnabled = false;
  public boolean soundEnabledEver = false;
  public int soundStereoAY = SoundStereoAY.SOUND_STEREO_AY_NONE.ordinal();
  public int soundFramesiz;
  public int soundChannels;

  private final int[] ayToneLevels = new int[16];
  private final int[] ayToneTick = new int[3];
  private final boolean[] ayToneHigh = new boolean[3];
  private int ayNoiseTick;
  private int ayToneCycles;
  private int ayEnvCycles;
  private int ayEnvInternalTick;
  private int ayEnvTick;
  private final int[] ayTonePeriod = new int[3];
  private int ayNoisePeriod;
  private int ayEnvPeriod;
  private final byte[] soundAYRegisters = new byte[16];
  private final SoundAYChange[] ayChange = new SoundAYChange[AY_CHANGE_MAX];
  private int ayChangeCount;

  private BlipBuffer leftBuf;
  private BlipBuffer rightBuf;
  private int[] samples;
  private BlipSynth leftBeeperSynth;
  private BlipSynth rightBeeperSynth;
  private BlipSynth ayASynth;
  private BlipSynth ayBSynth;
  private BlipSynth ayCSynth;
  private BlipSynth ayASynthR;
  private BlipSynth ayBSynthR;
  private BlipSynth ayCSynthR;
  private BlipSynth leftSpecdrumSynth;
  private BlipSynth rightSpecdrumSynth;
  private BlipSynth leftCovoxSynth;
  private BlipSynth rightCovoxSynth;

  // Speaker types
  private final SpeakerType[] speakerType = {
      new SpeakerType(200, -37.0),
      new SpeakerType(1000, -67.0),
      new SpeakerType(0, 0.0)
  };

  // AY change record
  private record SoundAYChange(long tstates, byte reg, byte val) {
  }

  // Initialize sound
  public void init(String device) {
    if (!(settings.current.sound && !soundEnabled && isInSoundEnabledRange())) {
      return;
    }

    soundStereoAY = Options.enumerateSoundStereoAY();
    Supplier<Integer> getSoundFreq = settings.current::getSoundFreq;
    int[] soundFreq = {getSoundFreq.get()};
    int[] ints = {soundStereoAY};
    if (settings.current.sound) {
      boolean b = lowlevelInit(device, soundFreq, ints);
      settings.current.soundFreq = soundFreq[0];
      soundStereoAY = ints[0];
      if (b) {
        return;
      }
    }

    leftBuf = new BlipBuffer();
    leftBeeperSynth = new BlipSynth();
    if (!initBlip(leftBuf, leftBeeperSynth)) {
      return;
    }

    if (soundStereoAY != SoundStereoAY.SOUND_STEREO_AY_NONE.ordinal()) {
      rightBuf = new BlipBuffer();
      rightBeeperSynth = new BlipSynth();
      if (!initBlip(rightBuf, rightBeeperSynth)) {
        return;
      }
    }

    double treble = speakerType[Options.enumerateSoundSpeakerType()].treble;

    ayASynth = new BlipSynth();
    ayASynth.setVolume(getVolume(settings.current.volumeAY));
    ayASynth.setTrebleEq(treble);

    ayBSynth = new BlipSynth();
    ayBSynth.setVolume(getVolume(settings.current.volumeAY));
    ayBSynth.setTrebleEq(treble);

    ayCSynth = new BlipSynth();
    ayCSynth.setVolume(getVolume(settings.current.volumeAY));
    ayCSynth.setTrebleEq(treble);

    leftSpecdrumSynth = new BlipSynth();
    leftSpecdrumSynth.setVolume(getVolume(settings.current.volumeSpecdrum));
    leftSpecdrumSynth.setOutput(leftBuf);
    leftSpecdrumSynth.setTrebleEq(treble);

    leftCovoxSynth = new BlipSynth();
    leftCovoxSynth.setVolume(getVolume(settings.current.volumeCovox));
    leftCovoxSynth.setOutput(leftBuf);
    leftCovoxSynth.setTrebleEq(treble);

    BlipSynth ayLeftSynth, ayMidSynth, ayRightSynth;
    BlipSynth ayMidSynthR;
    ayASynthR = null;
    ayBSynthR = null;
    ayCSynthR = null;

    if (soundStereoAY != SoundStereoAY.SOUND_STEREO_AY_NONE.ordinal()) {
      if (soundStereoAY == SoundStereoAY.SOUND_STEREO_AY_ACB.ordinal()) {
        ayLeftSynth = ayASynth;
        ayMidSynth = ayCSynth;
        ayMidSynthR = new BlipSynth();
        ayRightSynth = ayBSynth;
      } else if (soundStereoAY == SoundStereoAY.SOUND_STEREO_AY_ABC.ordinal()) {
        ayLeftSynth = ayASynth;
        ayMidSynth = ayBSynth;
        ayMidSynthR = new BlipSynth();
        ayRightSynth = ayCSynth;
      } else {
        Ui.error(UIErrorLevel.UI_ERROR_ERROR, "unknown AY stereo separation type: %d", soundStereoAY);
        throw new RuntimeException("Unknown AY stereo separation type");
      }

      ayLeftSynth.setOutput(leftBuf);
      ayMidSynth.setOutput(leftBuf);
      ayRightSynth.setOutput(rightBuf);
      ayMidSynthR.setVolume(getVolume(settings.current.volumeAY));
      ayMidSynthR.setOutput(rightBuf);
      ayMidSynthR.setTrebleEq(treble);

      rightSpecdrumSynth = new BlipSynth();
      rightSpecdrumSynth.setVolume(getVolume(settings.current.volumeSpecdrum));
      rightSpecdrumSynth.setOutput(rightBuf);
      rightSpecdrumSynth.setTrebleEq(treble);

      rightCovoxSynth = new BlipSynth();
      rightCovoxSynth.setVolume(getVolume(settings.current.volumeCovox));
      rightCovoxSynth.setOutput(rightBuf);
      rightCovoxSynth.setTrebleEq(treble);
    } else {
      ayASynth.setOutput(leftBuf);
      ayBSynth.setOutput(leftBuf);
      ayCSynth.setOutput(leftBuf);
    }

    soundEnabled = soundEnabledEver = true;
    soundChannels = 2;

    float hz = (float) getEffectiveProcessorSpeed() / spectrumMachine.getTimings().tstatesPerFrame;
    soundFramesiz = (int) (settings.current.soundFreq / hz) + 1;
    soundFramesiz/= 100;
    samples = new int[soundFramesiz * soundChannels];
    movie.initSound(settings.current.soundFreq, soundStereoAY);

    ayInit();
  }

  // Pause sound
  public void pause() {
    if (soundEnabled) {
      end();
    }
  }

  // Unpause sound
  public void unpause() {
//    if (settings.current.fastload && timer.fastloadingActive()) {
//      return;
//    }
    init(settings.current.soundDevice);
  }

  @Override
  public int init(Object initContext) {
    return 0;
  }

  // End sound
  public void end() {
    if (soundEnabled) {
//      leftBeeperSynth.delete();
//      rightBeeperSynth.delete();
//      ayASynth.delete();
//      ayBSynth.delete();
//      ayCSynth.delete();
//      if (ayASynthR != null) ayASynthR.delete();
//      if (ayBSynthR != null) ayBSynthR.delete();
//      if (ayCSynthR != null) ayCSynthR.delete();
//      leftSpecdrumSynth.delete();
//      rightSpecdrumSynth.delete();
//      leftCovoxSynth.delete();
//      rightCovoxSynth.delete();
//      leftBuf.delete();
//      rightBuf.delete();
      if (settings.current.sound) {
        lowlevelEnd();
      }
      samples = null;
      soundEnabled = false;
    }
  }

  // AY initialization
  private void ayInit() {
    final int[] levels = {
        0x0000, 0x0385, 0x053D, 0x0770, 0x0AD7, 0x0FD5, 0x15B0, 0x230C,
        0x2B4C, 0x43C1, 0x5A4B, 0x732F, 0x9204, 0xAFF1, 0xD921, 0xFFFF
    };
    for (int f = 0; f < 16; f++) {
      ayToneLevels[f] = (levels[f] * AMPL_AY_TONE + 0x8000) / 0xffff;
    }
    ayNoiseTick = ayNoisePeriod = 0;
    ayEnvInternalTick = ayEnvTick = ayEnvPeriod = 0;
    ayToneCycles = ayEnvCycles = 0;
    Arrays.fill(ayToneTick, 0);
    Arrays.fill(ayToneHigh, false);
    Arrays.fill(ayTonePeriod, 1);
    ayChangeCount = 0;
  }

  // AY write
  public void ayWrite(int reg, int val, long now) {
    if (ayChangeCount < AY_CHANGE_MAX) {
      ayChange[ayChangeCount] = new SoundAYChange(now, (byte) (reg & 15), (byte) val);
      ayChangeCount++;
    }
  }

  // AY reset
  public void ayReset() {
    ayInit();
    ayChangeCount = 0;
    for (int f = 0; f < 16; f++) {
      ayWrite(f, 0, 0);
    }
    Arrays.fill(ayToneHigh, false);
    ayToneCycles = ayEnvCycles = 0;
  }

  // Process sound frame
  public void frame() {
//    audio.updateAudio(clock.getTStates(), speaker);
//    audio.endFrame();
    if (!soundEnabled) {
      return;
    }

//    ayOverlay();
    leftBuf.endFrame(spectrumMachine.getTimings().tstatesPerFrame);
    long count;
    if (soundStereoAY != SoundStereoAY.SOUND_STEREO_AY_NONE.ordinal()) {
      rightBuf.endFrame(spectrumMachine.getTimings().tstatesPerFrame);
      count = leftBuf.readSamples(samples, soundFramesiz, true);
//      rightBuf.readSamples(samples, 1, count, true);
      count <<= 1;
    } else {
      count = leftBuf.readSamples(samples, soundFramesiz, true);
    }

    if (settings.current.sound) {
      lowlevelFrame(samples, (int) count);
    }
    if (movie.recording) {
      movie.addSound(samples, (int) count);
    }
    ayChangeCount = 0;
  }

  static final int SPEAKER_VOLUME = -32700; // 6300;
  public int speaker;
  private static int sp_volt[];

  static {
    sp_volt = new int[4];
    setvol();
  }

  static void setvol() {
    sp_volt[0] = 0; //(int) -SPEAKER_VOLUME;
    sp_volt[1] = (int) (SPEAKER_VOLUME * 0.04f); // (int) -(SPEAKER_VOLUME * 1.4);
    sp_volt[2] = (int) (SPEAKER_VOLUME * 0.96f);
    sp_volt[3] = SPEAKER_VOLUME;
  }

  // Beeper sound
  public void beeper(long atTstates, int on, int value) {
    final int[] beeperAmpl = {0, AMPL_TAPE, AMPL_BEEPER, AMPL_BEEPER + AMPL_TAPE};
    if (!soundEnabled) {
      return;
    }
    if (tape.isTapePlaying()) {
      if (!settings.current.soundLoad || spectrumMachine.isTimex()) {
        on &= 0x02;
      }
    } else {
      if (on == 1) {
        on = 0;
      }
    }
    int val = beeperAmpl[on];
//    if (val != lastVal)
//      System.out.println("val: " + val);

    lastVal = val;
    leftBeeperSynth.update(atTstates, val);
    if (soundStereoAY != SoundStereoAY.SOUND_STEREO_AY_NONE.ordinal()) {
      rightBeeperSynth.update(atTstates, val);
    }
  }

  // Get effective processor speed
  public long getEffectiveProcessorSpeed() {
    return spectrumMachine.getTimings().tstatesPerFrame * settings.current.emulationSpeed / 100;
  }

  // Helper methods
  private boolean isInSoundEnabledRange() {
    return settings.current.emulationSpeed >= MIN_SPEED_PERCENTAGE &&
        settings.current.emulationSpeed <= MAX_SPEED_PERCENTAGE;
  }

  private double getVolume(int volume) {
    if (volume < 0) volume = 0;
    else if (volume > 100) volume = 100;
    return volume / 100.0;
  }

  private boolean initBlip(BlipBuffer buf, BlipSynth synth) {
    buf.setClockRate(getEffectiveProcessorSpeed());
    if (buf.setSampleRate(settings.current.soundFreq, 1000) != null) {
      end();
      Ui.error(UIErrorLevel.UI_ERROR_ERROR, "out of memory for sound buffer");
      return false;
    }
    synth.setVolume(getVolume(settings.current.volumeBeeper));
    synth.setOutput(buf);
    int speakerTypeIdx = Options.enumerateSoundSpeakerType();
    buf.setBassFreq(speakerType[speakerTypeIdx].bass);
    synth.setTrebleEq(speakerType[speakerTypeIdx].treble);
    return true;
  }

  private void ayOverlay() {
    if (!(periph.isActive(Periph.Type.FULLER) ||
        periph.isActive(Periph.Type.MELODIK) ||
        (spectrumMachine.getCapabilities() & Libspectrum.MachineCapability.AY) != 0)) {
      return;
    }

    int rng = 1;
    boolean noiseToggle = false;
    boolean envFirst = true, envRev = false;
    int envCounter = 15;
    int[] toneLevel = new int[3];
    int lastChan1 = 0, lastChan2 = 0, lastChan3 = 0;

    for (long f = 0; f < spectrumMachine.getTimings().tstatesPerFrame; f += 16 * 2) {
      int changesLeft = ayChangeCount;
      int changeIdx = 0;
      while (changesLeft > 0 && f >= ayChange[changeIdx].tstates) {
        int reg = ayChange[changeIdx].reg;
        soundAYRegisters[reg] = ayChange[changeIdx].val;
        changeIdx++;
        changesLeft--;

        switch (reg) {
          case 0:
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
            int r = reg >> 1;
            ayTonePeriod[r] = (soundAYRegisters[reg & ~1] | (soundAYRegisters[reg | 1] & 15) << 8);
            if (ayTonePeriod[r] == 0) ayTonePeriod[r] = 1;
            if (ayToneTick[r] >= ayTonePeriod[r] * 2) {
              ayToneTick[r] %= ayTonePeriod[r] * 2;
            }
            break;
          case 6:
            ayNoiseTick = 0;
            ayNoisePeriod = soundAYRegisters[reg] & 31;
            break;
          case 11:
          case 12:
            ayEnvPeriod = soundAYRegisters[11] | (soundAYRegisters[12] << 8);
            break;
          case 13:
            ayEnvInternalTick = ayEnvTick = ayEnvCycles = 0;
            envFirst = true;
            envRev = false;
            envCounter = (soundAYRegisters[13] & 8) != 0 ? 0 : 15;
            break;
        }
      }

      for (int g = 0; g < 3; g++) {
        toneLevel[g] = ayToneLevels[soundAYRegisters[8 + g] & 15];
      }

      int envShape = soundAYRegisters[13];
      int level = ayToneLevels[envCounter];
      for (int g = 0; g < 3; g++) {
        if ((soundAYRegisters[8 + g] & 16) != 0) {
          toneLevel[g] = level;
        }
      }

      ayEnvCycles += 16;
      int noiseCount = 0;
      while (ayEnvCycles >= 16) {
        ayEnvCycles -= 16;
        noiseCount++;
        ayEnvTick++;
        while (ayEnvTick >= ayEnvPeriod) {
          ayEnvTick -= ayEnvPeriod;

          if (envFirst || ((envShape & 8) != 0 && (envShape & 1) == 0)) {
            if (envRev) {
              envCounter -= (envShape & 4) != 0 ? 1 : -1;
            } else {
              envCounter += (envShape & 4) != 0 ? 1 : -1;
            }
            envCounter = Math.clamp(envCounter, 0, 15);
          }

          ayEnvInternalTick++;
          while (ayEnvInternalTick >= 16) {
            ayEnvInternalTick -= 16;

            if ((envShape & 8) == 0) {
              envCounter = 0;
            } else if ((envShape & 1) != 0) {
              if (envFirst && (envShape & 2) != 0) {
                envCounter = envCounter != 0 ? 0 : 15;
              }
            } else {
              if ((envShape & 2) != 0) {
                envRev = !envRev;
              } else {
                envCounter = (envShape & 4) != 0 ? 0 : 15;
              }
            }
            envFirst = false;
          }

          if (ayEnvPeriod == 0) break;
        }
      }

      int chan1 = toneLevel[0], chan2 = toneLevel[1], chan3 = toneLevel[2];
      int mixer = soundAYRegisters[7];

      ayToneCycles += 16;
      int toneCount = ayToneCycles >> 3;
      ayToneCycles &= 7;

      if ((mixer & 1) == 0) {
        level = chan1;
        ayDoTone(level, toneCount, chan1, 0);
      }
      if ((mixer & 0x08) == 0 && noiseToggle) {
        chan1 = 0;
      }

      if ((mixer & 2) == 0) {
        level = chan2;
        ayDoTone(level, toneCount, chan2, 1);
      }
      if ((mixer & 0x10) == 0 && noiseToggle) {
        chan2 = 0;
      }

      if ((mixer & 4) == 0) {
        level = chan3;
        ayDoTone(level, toneCount, chan3, 2);
      }
      if ((mixer & 0x20) == 0 && noiseToggle) {
        chan3 = 0;
      }

      if (lastChan1 != chan1) {
        ayASynth.update(f, chan1);
        if (ayASynthR != null) ayASynthR.update(f, chan1);
        lastChan1 = chan1;
      }
      if (lastChan2 != chan2) {
        ayBSynth.update(f, chan2);
        if (ayBSynthR != null) ayBSynthR.update(f, chan2);
        lastChan2 = chan2;
      }
      if (lastChan3 != chan3) {
        ayCSynth.update(f, chan3);
        if (ayCSynthR != null) ayCSynthR.update(f, chan3);
        lastChan3 = chan3;
      }

      ayNoiseTick += noiseCount;
      while (ayNoiseTick >= ayNoisePeriod) {
        ayNoiseTick -= ayNoisePeriod;

        int i = (rng & 1) ^ ((rng & 2) != 0 ? 1 : 0);
        if (i != 0) {
          noiseToggle = !noiseToggle;
        }

        if ((rng & 1) != 0) {
          rng ^= 0x24000;
        }
        rng >>= 1;

        if (ayNoisePeriod == 0) break;
      }
    }
  }

  private void ayDoTone(int level, int toneCount, int chan, int channel) {
    int var = 0;
    ayToneTick[channel] += toneCount;
    if (ayToneTick[channel] >= ayTonePeriod[channel]) {
      ayToneTick[channel] -= ayTonePeriod[channel];
      ayToneHigh[channel] = !ayToneHigh[channel];
    }
    if (level != 0 && ayToneHigh[channel]) {
      var = level;
    }
    chan = var;
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
}


// Speaker type record
final class SpeakerType {
  public final int bass;
  public final double treble;

  SpeakerType(int bass, double treble) {
    this.bass = bass;
    this.treble = treble;
  }

  public int bass() {
    return bass;
  }

  public double treble() {
    return treble;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (SpeakerType) obj;
    return this.bass == that.bass &&
        Double.doubleToLongBits(this.treble) == Double.doubleToLongBits(that.treble);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bass, treble);
  }

  @Override
  public String toString() {
    return "SpeakerType[" +
        "bass=" + bass + ", " +
        "treble=" + treble + ']';
  }

}