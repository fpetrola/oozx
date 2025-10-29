/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse.modules;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.z80.Z80;

import java.util.function.Supplier;

public class Timer implements ZxModule {
  private final EventManager eventManager;
  private final Supplier<SpectrumMachine> fuseMachineInfoSupplier;
  private final Sound sound;

  private final double[] storedTimes = new double[10];
  private int nextStoredTime = 0;
  private int framesUntilUpdate = 0;
  private int samples = 0;
  private float currentSpeed = 100.0f;
  private double startTime = 0.0;
  private int timerEvent = 0;
  private static final int TEN_MS = 10;
  private Settings settings;

  public Timer(EventManager eventManager, Supplier<SpectrumMachine> fuseMachineInfoSupplier, Sound sound, Settings settings) {
    this.eventManager = eventManager;
    this.fuseMachineInfoSupplier = fuseMachineInfoSupplier;
    this.sound = sound;
    this.settings = settings;
  }

  @Override
  public int init(Object context) {
    startTime = getTime();
    if (startTime < 0) {
      return 1;
    }

    timerEvent = eventManager.eventRegister(this::frame, "Timer");
    addEvent();
    return estimateReset();
  }

  public void addEvent() {
    eventManager.eventAdd(0, timerEvent);
  }

  @Override
  public void end() {
    eventManager.eventRemoveType(timerEvent);
  }

  // Estimate emulation speed
  public int estimateSpeed(Z80 z80) {
    if (framesUntilUpdate-- > 0) {
      return 0;
    }

    double currentTime = getTime();
    if (currentTime < 0) {
      return 1;
    }

    if (samples < 10) {
      currentSpeed = settings.current.emulationSpeed;
    } else {
      currentSpeed = (float) (10 * 100.0 / (currentTime - storedTimes[nextStoredTime]));
    }

    Ui.statusbarUpdateSpeed(currentSpeed, z80);

    storedTimes[nextStoredTime] = currentTime;
    nextStoredTime = (nextStoredTime + 1) % 10;
    framesUntilUpdate = (int) (getCurrent().getTimings().processorSpeed / getCurrent().getTimings().tstatesPerFrame) - 1;
    samples++;

    return 0;
  }

  // Reset speed estimation
  public int estimateReset() {
    startTime = getTime();
    if (startTime < 0) {
      return 1;
    }
    samples = 0;
    nextStoredTime = 0;
    framesUntilUpdate = 0;
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
    return Tape.isPlaying() || PhantomTypist.isActive();
  }

  // Frame handling
  public void frame(long lastTstates, int event, Object userData) {
    if (Sound.enabled && settings.current.sound) {
      frameCallbackSound(lastTstates);
      return;
    }

    if (settings.current.fastload && fastloadingActive()) {
      long nextCheckTime = lastTstates + getCurrent().getTimings().tstatesPerFrame;
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
      int tstates = (int) (((difference + TEN_MS / 1000.0) * getCurrent().getTimings().processorSpeed) * speed + 0.5);
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
    eventManager.eventAdd(lastTstates + getCurrent().getTimings().tstatesPerFrame, timerEvent);
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

  private SpectrumMachine getCurrent() {
    return fuseMachineInfoSupplier.get();
  }
}
