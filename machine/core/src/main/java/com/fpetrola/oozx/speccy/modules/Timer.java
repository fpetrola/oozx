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

package com.fpetrola.oozx.speccy.modules;

import com.fpetrola.oozx.UserInterface;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;

import java.util.Arrays;

@Singleton
public class Timer implements ZxModule, MachineChangeListener {
  private final EventManager eventManager;
  private final Sound sound;

  private final double[] storedTimes = new double[10];
  private int nextStoredTime = 0;
  private int framesUntilUpdate = 0;
  private int samples = 0;
  private float currentSpeed = 100.0f;
  private double startTime = 0.0;
  private int timerEvent = 0;
  private static final int TEN_MS = 10;
  private final Settings settings;
  private final Tape tape;
  private boolean changeRequested = false;
  private SpectrumMachine spectrumMachine;
  private final UserInterface userInterface;

  @Inject
  public Timer(EventManager eventManager, Sound sound, Settings settings, Tape tape, UserInterface userInterface) {
    this.userInterface = userInterface;
    this.eventManager = eventManager;
    this.sound = sound;
    this.settings = settings;
    this.tape = tape;
  }

  @Override
  public void start() {
    startTime = getTime();
    if (startTime < 0) {
      throw new IllegalStateException("the clock went backwards before the timer started");
    }

    timerEvent = eventManager.eventRegister(this::frame, "Timer");
    addEvent();
    estimateReset();
  }

  public void addEvent() {
    eventManager.eventAdd(0, timerEvent);
  }

  @Override
  public void end() {
    eventManager.eventRemoveType(timerEvent);
  }

  // Estimate emulation speed
  public void estimateSpeed(Cpu cpu) {
    if (framesUntilUpdate-- > 0) {
      return;
    }

    double currentTime = getTime();
    if (currentTime < 0) {
      return;
    }

    if (samples < 10) {
      currentSpeed = settings.current.emulationSpeed;
    } else {
      currentSpeed = (float) (10 * 100.0 / (currentTime - storedTimes[nextStoredTime]));
    }

    userInterface.statusbarUpdateSpeed(currentSpeed, cpu.getEmulatorCore());

    storedTimes[nextStoredTime] = currentTime;
    nextStoredTime = (nextStoredTime + 1) % 10;
    framesUntilUpdate = (int) (spectrumMachine.getTimings().processorSpeed / spectrumMachine.getTimings().tstatesPerFrame) - 1;
    samples++;
  }

  // Reset speed estimation
  public int estimateReset() {
    startTime = getTime();
    if (startTime < 0) {
      throw new IllegalStateException("the clock went backwards before the timer started");
    }
    samples = 0;
    nextStoredTime = 0;
    framesUntilUpdate = 0;
    Arrays.fill(storedTimes, 0);
    return 0;
  }

  // Start fastloading
  public void startFastloading() {
    if (settings.current.fastload) {
      sound.pause();
    }
  }

  // Stop fastloading
  public void stopFastloading() {
    if (settings.current.fastload) {
      sound.unpause();
      estimateReset();
    }
  }

  // Check if fastloading is active
  public boolean fastloadingActive() {
    return tape.isTapePlaying() || PhantomTypist.isActive();
  }

  // Frame handling
  public void frame(long lastTstates, int event, Object userData) {
    if (changeRequested) {
      changeRequested= false;
      estimateReset();
    }
    if (sound.soundEnabled && settings.current.sound) {
      frameCallbackSound(lastTstates);
      return;
    }

    if (settings.current.fastload && fastloadingActive()) {
      long nextCheckTime = lastTstates + spectrumMachine.getTimings().tstatesPerFrame;
      eventManager.eventAdd(nextCheckTime, timerEvent);
    } else {
      float speed = Math.max(settings.current.emulationSpeed, 1) / 100.0f;
      while (true) {
        double currentTime = getTime();
        if (currentTime < 0) {
          return;
        }
        double difference = currentTime - startTime;
        if (difference < 0) {
          sleep(TEN_MS);
        } else {
          break;
        }
      }

      double currentTime = getTime();
      if (currentTime < 0) {
        return;
      }
      double difference = currentTime - startTime;
      int tstates = (int) (((difference + TEN_MS / 1000.0) * spectrumMachine.getTimings().processorSpeed) * speed + 0.5);
      eventManager.eventAdd(lastTstates + tstates, timerEvent);
      startTime = currentTime + TEN_MS / 1000.0;
    }
  }

  // Sound-based frame callback
  private void frameCallbackSound(long lastTstates) {
    // Placeholder for SOUND_FIFO implementation
        /*
        for (;;) {
            if (Sfifo.space(soundFifo) < Sound.framesiz) {
                sleep(TEN_MS);
            } else {
                break;
            }
        }
        */
    eventManager.eventAdd(lastTstates + spectrumMachine.getTimings().tstatesPerFrame, timerEvent);
  }

  // Get current time in seconds
  private double getTime() {
    return System.nanoTime() / 1_000_000_000.0;
  }

  // Sleep for specified milliseconds
  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void changeSpeed(int emulationSpeed) {
    this.changeRequested = true;
    eventManager.changeEventTime(70000, timerEvent);
  }

  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }
}
