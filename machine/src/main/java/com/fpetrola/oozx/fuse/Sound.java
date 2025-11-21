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
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.ZxModule;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.oozx.fuse.sound.JavaSoundDevice;
import com.fpetrola.oozx.fuse.sound.blip.BlipBuffer;
import com.fpetrola.oozx.fuse.sound.blip.BlipSynth;

import java.util.Arrays;

public class Sound implements ZxModule, MachineChangeListener {

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

  public final Settings settings;
  // Constants
  public final int AMPL_BEEPER = 50 * 256;
  public final int AMPL_TAPE = 2 * 256;
  private int[] beeperAmpl = new int[]{0, AMPL_TAPE, AMPL_BEEPER, AMPL_BEEPER + AMPL_TAPE};
  public static final int AMPL_AY_TONE = 24 * 256;
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

  public Sound(Settings settings, Movie movie, IPeriph periph, Tape tape, SpectrumZ80Clock clock) {
    this.settings = settings;
//    this.timer = timer;
    this.movie = movie;
    this.periph = periph;
    this.tape = tape;
    this.clock = clock;
  }

  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }

  private static final int AY_CLOCK_DIVISOR = 16;

  private static final int AY_CLOCK_RATIO = 2;
  private static final int[] AY_TONE_LEVELS = {
      0x0000, 0x0385, 0x053D, 0x0770, 0x0AD7, 0x0FD5, 0x15B0, 0x230C,
      0x2B4C, 0x43C1, 0x5A4B, 0x732F, 0x9204, 0xAFF1, 0xD921, 0xFFFF
  };

  private static final int[] ayToneLevelsScaled = new int[16];

  static {
    for (int i = 0; i < 16; i++) {
      ayToneLevelsScaled[i] = (AY_TONE_LEVELS[i] * AMPL_AY_TONE + 0x8000) / 0xFFFF;
    }
  }

  // ========================================================================
  // Estado del sonido
  // ========================================================================

  public boolean soundEnabled = false;
  private int soundChannels = 2;
  private int soundFrameSize;
  private int[] outputSamples;

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
  // ========================================================================
  // Estado del chip AY
  // ========================================================================

  private final byte[] ayRegisters = new byte[16];
  private int ayToneTick[] = new int[3];
  private int ayToneHigh[] = new int[3];
  private int ayTonePeriod[] = new int[3];
  private int ayNoiseTick, ayNoisePeriod;
  private int ayEnvTick, ayEnvInternalTick, ayEnvPeriod;
  private int ayToneCycles, ayEnvCycles;

  private static class AyChange {

    long tstates;
    int reg, val;
  }

  private final AyChange[] ayChanges = new AyChange[AY_CHANGE_MAX];

  private int ayChangeCount = 0;
  private int rng = 1;

  private boolean noiseToggle = false;
  // ========================================================================
  // Configuración actual (simulación de settings_current)
  // ========================================================================

  public int soundFreq = 44100;
  public int volumeBeeper = 100;
  public int volumeAY = 100;
  public int volumeSpecdrum = 100;
  public int volumeCovox = 100;
  public int stereoAY = 0; // 0=none, 1=ABC, 2=ACB
  public int speakerType = 0; // 0=Small, 1=Large TV, 2=None
  private final double[] speakerTreble = {-37.0, -67.0, 0.0};

  private final int[] speakerBass = {200, 1000, 0};

  public int init(Object initContext) {
    String soundDevice = settings.current.soundDevice;
    int frameTstates = spectrumMachine.getTimings().tstatesPerFrame;

    initSound(3500000, frameTstates);
    return 0;
  }

  public void end() {
//    lowlevelEnd();
  }

  // ========================================================================
  // Inicialización del sonido
  // ========================================================================

  public boolean initSound(long cpuFrequency, int tstatesPerFrame) {
//    if (soundEnabled) return true;

//    if (settings.current.emulationSpeed < 2 || settings.current.emulationSpeed > 500) return false;

//    rightBuf = new BlipBuffer();

    long effectiveSpeed = cpuFrequency * settings.current.emulationSpeed / 100 * 2;

    int[] ints = {0};
    int[] soundFreqArray = {soundFreq};

    String device = "buffer=8192,frames=8";
    boolean b = lowlevelInit(device, soundFreqArray, ints);

//    if (stereoAY != 0) {
//      rightBuf.setSampleRate(soundFreq, 1000);
//      rightBuf.clockRate(effectiveSpeed);
//    }

    double treble = speakerTreble[speakerType];
    int bass = speakerBass[speakerType];
    double volume = getVolume(volumeBeeper);
    leftBeeperSynth = new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, soundFreq, 1000, effectiveSpeed, bass, volume, treble);

//    if (stereoAY != 0) {
//      rightBeeperSynth = new BlipSynth(BlipBuffer.BLIP_GOOD_QUALITY, 32768);
//      rightBeeperSynth.volume(getVolume(volumeBeeper));
//      rightBeeperSynth.trebleEq(new BlipEq(treble));
//      rightBeeperSynth.output(rightBuf);
//    }
//
//    ayASynth = createAySynth(treble, leftBuf);
//    ayBSynth = createAySynth(treble, leftBuf);
//    ayCSynth = createAySynth(treble, leftBuf);
//
//    if (stereoAY != 0) {
//      switch (stereoAY) {
//        case 1: // ABC
//          ayASynth.output(leftBuf);
//          ayBSynth.output(leftBuf);
//          ayCSynth.output(rightBuf);
//          ayBSynthR = createAySynth(treble, rightBuf);
//          break;
//        case 2: // ACB
//          ayASynth.output(leftBuf);
//          ayCSynth.output(leftBuf);
//          ayBSynth.output(rightBuf);
//          ayCSynthR = createAySynth(treble, rightBuf);
//          break;
//      }
//    }
//
//    leftSpecdrumSynth = createAySynth(treble, leftBuf);
//    leftCovoxSynth = createAySynth(treble, leftBuf);
//    if (stereoAY != 0) {
//      rightSpecdrumSynth = createAySynth(treble, rightBuf);
//      rightCovoxSynth = createAySynth(treble, rightBuf);
//    }

    // Calcular tamaño de frame de audio
    double hz = (double) effectiveSpeed / tstatesPerFrame;
    soundFrameSize = (int) (soundFreq / hz) + 1;
    outputSamples = new int[soundFrameSize * 2];

    soundEnabled = true;
    ayReset();
    return true;
  }

//  private BlipSynth createAySynth(double treble, BlipBuffer buf) {
//    BlipSynth synth = new BlipSynth(BlipBuffer.BLIP_SYNTH_QUALITY, soundFreq, 1000, effectiveSpeed);
//    synth.volume(getVolume(volumeAY));
//    synth.trebleEq(new BlipEq(treble));
//    synth.output(buf);
//    return synth;
//  }

  private double getVolume(int volume) {
    if (volume < 0) volume = 0;
    else if (volume > 100) volume = 100;

    return volume / 100.0;
  }


  public void beeper(long tstates, int on, int value) {
    if (!soundEnabled) {
      return;
    }
    if (tape.isTapePlaying()) {
      if (!settings.current.soundLoad || spectrumMachine.isTimex()) {
        on &= 0x02;
      }
    } else {
      if (on == 1)
        on = 0;
    }
    int val = beeperAmpl[on];
    lastVal = val;
    leftBeeperSynth.update(tstates, val);
    if (rightBeeperSynth != null) {
      rightBeeperSynth.update(tstates, val);
    }
  }

  // ========================================================================
  // AY-3-8912
  // ========================================================================

  public void ayWrite(int reg, int val, long tstates) {
    if (ayChangeCount < AY_CHANGE_MAX) {
      if (ayChanges[ayChangeCount] == null) ayChanges[ayChangeCount] = new AyChange();
      AyChange ch = ayChanges[ayChangeCount++];
      ch.tstates = tstates;
      ch.reg = reg & 15;
      ch.val = val;
    }
  }

  public void ayReset() {
    ayChangeCount = 0;
    Arrays.fill(ayRegisters, (byte) 0);
    Arrays.fill(ayTonePeriod, 1);
    Arrays.fill(ayToneTick, 0);
    Arrays.fill(ayToneHigh, 0);
    ayNoisePeriod = ayNoiseTick = 0;
    ayEnvPeriod = ayEnvTick = ayEnvInternalTick = 0;
    ayToneCycles = ayEnvCycles = 0;
    rng = 1;
    noiseToggle = false;
  }

  // ========================================================================
  // Generación de frame de sonido (llamar al final de cada frame)
  // ========================================================================

  public void frame() {
    int frameTstates = spectrumMachine.getTimings().tstatesPerFrame;
    if (!soundEnabled) return;

//    ayOverlay(frameTstates);

    leftBeeperSynth.endFrame(frameTstates);
//    if (rightBuf != null)
//      rightBuf.endFrame(frameTstates);

    int count = 0;
    if (stereoAY != 0) {
//      count = (int) leftBuf.readSamples(outputSamples, soundFrameSize, true);
//      rightBuf.readSamples(outputSamples, count, true); // interleave
//      count *= 2;
    } else {
      count = leftBeeperSynth.readSamples(outputSamples, soundFrameSize, true);
      for (int i = count - 1; i >= 0; i--) {
        outputSamples[i * 2 + 1] = outputSamples[i * 2];
      }
      count *= 2;
    }

    if (settings.current.sound) {
      lowlevelFrame(outputSamples, (int) count);
    }
    if (movie.recording) {
      movie.addSound(outputSamples, (int) count);
    }

    ayChangeCount = 0;
  }

  private boolean lowlevelInit(String device, int[] freqPtr, int[] stereoPtr) {
    return javaSoundDevice.sound_lowlevel_init(device, freqPtr, stereoPtr) != 0;
  }

  private void lowlevelEnd() {
    javaSoundDevice.sound_lowlevel_end();
  }

  private void lowlevelFrame(int[] data, int len) {
//    double[] data1 = new double[len];
//    for (int i = 0; i < len; i++) {
//      data1[i]= data[i];
////      OOSpectrumConnector.sendData(data1[i]);
//    }
//    OOSpectrumConnector.sendData(data1);

    javaSoundDevice.sound_lowlevel_frame(data, len);
  }

  // ========================================================================
  // AY overlay - el corazón del sonido AY
  // ========================================================================

  private void ayOverlay(long frameTstates) {
    if (!hasAY()) return;

    int changesLeft = ayChangeCount;
    int changeIdx = 0;
    int envCounter = 15;
    boolean envFirst = true;
    boolean envRev = false;
    int envShape = 0;

    int lastA = 0, lastB = 0, lastC = 0;

    for (long f = 0; f < frameTstates; f += AY_CLOCK_DIVISOR * AY_CLOCK_RATIO) {

      // Aplicar cambios de registros pendientes
      while (changesLeft > 0 && ayChanges[changeIdx].tstates <= f) {
        AyChange ch = ayChanges[changeIdx++];
        int reg = ch.reg;
        ayRegisters[reg] = (byte) ch.val;
        changesLeft--;

        switch (reg) {
          case 0, 1, 2, 3, 4, 5 -> {
            int r = reg >> 1;
            int period = (ayRegisters[reg & ~1] & 0xFF) | ((ayRegisters[reg | 1] & 0x0F) << 8);
            ayTonePeriod[r] = period == 0 ? 1 : period;
            if (ayToneTick[r] >= ayTonePeriod[r] * 2) {
              ayToneTick[r] %= ayTonePeriod[r] * 2;
            }
          }
          case 6 -> ayNoisePeriod = ayRegisters[6] & 31;
          case 11, 12 -> ayEnvPeriod = (ayRegisters[11] & 0xFF) | ((ayRegisters[12] & 0xFF) << 8);
          case 13 -> {
            ayEnvTick = ayEnvInternalTick = ayEnvCycles = 0;
            envFirst = true;
            envRev = false;
            envCounter = (ayRegisters[13] & 4) != 0 ? 0 : 15;
            envShape = ayRegisters[13] & 0x0F;
          }
        }
      }

      // Envelope
      ayEnvCycles += AY_CLOCK_DIVISOR;
      int noiseCount = 0;
      while (ayEnvCycles >= 16) {
        ayEnvCycles -= 16;
        noiseCount++;
        ayEnvTick++;
        while (ayEnvTick >= ayEnvPeriod && ayEnvPeriod > 0) {
          ayEnvTick -= ayEnvPeriod;
          if (envFirst || ((envShape & 8) != 0 && (envShape & 1) == 0)) {
            int step = (envShape & 4) != 0 ? 1 : -1;
            envCounter += envRev ? -step : step;
            envCounter = Math.clamp(envCounter, 0, 15);
          }
          ayEnvInternalTick++;
          while (ayEnvInternalTick >= 16) {
            ayEnvInternalTick -= 16;
            if ((envShape & 8) == 0) envCounter = 0;
            else if ((envShape & 1) != 0) {
              if (envFirst && (envShape & 2) != 0) {
                envCounter = envCounter == 0 ? 15 : 0;
              }
            } else {
              if ((envShape & 2) != 0) envRev = !envRev;
              else envCounter = (envShape & 4) != 0 ? 0 : 15;
            }
            envFirst = false;
          }
          if (ayEnvPeriod == 0) break;
        }
      }

      // Generar tonos
      int[] toneLevel = new int[3];
      for (int i = 0; i < 3; i++) {
        int vol = ayRegisters[8 + i] & 15;
        toneLevel[i] = (ayRegisters[8 + i] & 16) != 0 ? ayToneLevelsScaled[envCounter] : ayToneLevelsScaled[vol];
      }

      int mixer = ayRegisters[7] & 0xFF;
      ayToneCycles += AY_CLOCK_DIVISOR;
      int toneCount = ayToneCycles >> 3;
      ayToneCycles &= 7;

      int chanA = toneLevel[0];
      int chanB = toneLevel[1];
      int chanC = toneLevel[2];

      if ((mixer & 1) == 0) ayDoTone(toneCount, 0, toneLevel[0], chanA);
      if ((mixer & 8) == 0 && noiseToggle) chanA = 0;

      if ((mixer & 2) == 0) ayDoTone(toneCount, 1, toneLevel[1], chanB);
      if ((mixer & 16) == 0 && noiseToggle) chanB = 0;

      if ((mixer & 4) == 0) ayDoTone(toneCount, 2, toneLevel[2], chanC);
      if ((mixer & 32) == 0 && noiseToggle) chanC = 0;

      if (lastA != chanA) {
        ayASynth.update(f, chanA);
        if (ayASynthR != null) ayASynthR.update(f, chanA);
        lastA = chanA;
      }
      if (lastB != chanB) {
        ayBSynth.update(f, chanB);
        if (ayBSynthR != null) ayBSynthR.update(f, chanB);
        lastB = chanB;
      }
      if (lastC != chanC) {
        ayCSynth.update(f, chanC);
        if (ayCSynthR != null) ayCSynthR.update(f, chanC);
        lastC = chanC;
      }

      // Ruido
      ayNoiseTick += noiseCount;
      while (ayNoiseTick >= ayNoisePeriod && ayNoisePeriod > 0) {
        ayNoiseTick -= ayNoisePeriod;
        boolean feedback = ((rng & 1) ^ ((rng & 2) != 0 ? 1 : 0)) != 0;
        if (feedback) noiseToggle = !noiseToggle;
        if ((rng & 1) != 0) rng ^= 0x24000;
        rng >>= 1;
        if (ayNoisePeriod == 0) break;
      }
    }
  }

  private void ayDoTone(int count, int chan, int level, int current) {
    ayToneTick[chan] += count;
    while (ayToneTick[chan] >= ayTonePeriod[chan]) {
      ayToneTick[chan] -= ayTonePeriod[chan];
      ayToneHigh[chan] = -ayToneHigh[chan];
    }
    current = level != 0 && ayToneHigh[chan] != 0 ? level : 0;
  }

  // ========================================================================
  // Periféricos DAC: SpecDrum, Covox
  // ========================================================================

  public void specdrumWrite(long tstates, int value) {
    if (!hasSpecdrum()) return;
    int sample = (value - 128) * 128;
    leftSpecdrumSynth.update(tstates, sample);
    if (rightSpecdrumSynth != null) rightSpecdrumSynth.update(tstates, sample);
  }

  public void covoxWrite(long tstates, int value) {
    if (!hasCovox()) return;
    int sample = value * 128;
    leftCovoxSynth.update(tstates, sample);
    if (rightCovoxSynth != null) rightCovoxSynth.update(tstates, sample);
  }

  // Métodos de ayuda (simulación de máquina actual)

  private boolean hasAY() {
    return true;
  } // simplificado

  private boolean hasSpecdrum() {
    return leftSpecdrumSynth != null;
  }

  private boolean hasCovox() {
    return leftCovoxSynth != null;
  }

  private boolean tapeIsPlaying() {
    return false;
  }

  private boolean soundLoadEnabled() {
    return true;
  }

  private boolean isTimexMachine() {
    return false;
  }

  // ========================================================================
  // Limpieza
  // ========================================================================

  public void close() {
    if (soundEnabled) {
      leftBeeperSynth.close();
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
