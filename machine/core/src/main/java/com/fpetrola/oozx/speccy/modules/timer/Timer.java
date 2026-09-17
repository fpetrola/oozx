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

package com.fpetrola.oozx.speccy.modules.timer;

import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.google.inject.Provider;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;

import java.util.Arrays;

@Singleton
public class Timer {
  private final Scheduler scheduler;
  private final Sound sound;

  private final double[] storedTimes = new double[10];
  private int nextStoredTime = 0;
  private int framesUntilUpdate = 0;
  private int samples = 0;
  private float currentSpeed = 100.0f;
  private double startTime = 0.0;
  private Task tick;
  private static final int TEN_MS = 10;
  private final Sound.Output soundOutput;
  private final Speed speed;
  /** Whether the machine is loading, which is when it may run flat out: said by whoever is feeding it. */
  private java.util.function.BooleanSupplier loading = () -> false;
  private boolean changeRequested = false;
  private final SpectrumZ80Clock clock;
  private final Supplier<Machine> machine;
  private final java.util.List<java.util.function.DoubleConsumer> speedListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

  @Inject
  public Timer(Scheduler scheduler, Sound sound, Sound.Output soundOutput, Speed speed, SpectrumZ80Clock clock, Provider<Machine> machine) {
    this.scheduler = scheduler;
    this.machine = Suppliers.memoize(machine::get);
    this.clock = clock;
    this.sound = sound;
    this.soundOutput = soundOutput;
    this.speed = speed;
    startTime = getTime();
    if (startTime < 0) {
      throw new IllegalStateException("the clock went backwards before the timer started");
    }
    tick = scheduler.register(new Tick());
    addEvent();
    estimateReset();
  }

  public void addEvent() {
    scheduler.schedule(tick, 0);
  }

  /** Nothing is timing this machine any more. */
  public void end() {
    scheduler.cancel(tick);
  }

  /** Told the speed the machine is actually managing, whenever it is worked out again. */
  public void onSpeed(java.util.function.DoubleConsumer listener) {
    speedListeners.add(listener);
  }

  public void estimateSpeed() {
    if (framesUntilUpdate-- > 0) {
      return;
    }

    double currentTime = getTime();
    if (currentTime < 0) {
      return;
    }

    if (samples < 10) {
      currentSpeed = speed.emulation;
    } else {
      currentSpeed = (float) (10 * 100.0 / (currentTime - storedTimes[nextStoredTime]));
    }

    // An indexed walk and a primitive: forEach with the speed captured allocates a lambda on
    // every frame, and a Consumer<Float> boxes it. This is called from inside the frame.
    for (int listener = 0; listener < speedListeners.size(); listener++) {
      speedListeners.get(listener).accept(currentSpeed);
    }

    storedTimes[nextStoredTime] = currentTime;
    nextStoredTime = (nextStoredTime + 1) % 10;
    framesUntilUpdate = (int) (machine.get().current.getTimings().processorSpeed() / machine.get().current.getTimings().tstatesPerFrame()) - 1;
    samples++;
  }

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

  public void loading(java.util.function.BooleanSupplier loading) {
    this.loading = loading;
  }

  private final class Tick extends Task {
    public void run(long lastTstates) {
      if (changeRequested) {
        changeRequested= false;
        estimateReset();
      }
      // At real time or below the sound is the clock: the card takes a frame of audio in a frame's
      // time and the machine waits on it. Above real time the card drops what it has no room for
      // and waits on nothing, so the pacing is done here.
      if (soundPaces()) {
        frameCallbackSound(lastTstates);
        return;
      }

      if (speed.fastLoading && loading.getAsBoolean()) {
        long nextCheckTime = lastTstates + machine.get().current.getTimings().tstatesPerFrame();
        scheduler.schedule(this, nextCheckTime);
      } else {
        float factor = Math.max(speed.emulation, 1) / 100.0f;
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
        // Clamped: at an unlimited speed the grant for one tick overflowed an int and came out
        // negative, which fired the timer at once and again, and the machine stood still.
        int tstates = (int) Math.min(((difference + TEN_MS / 1000.0) * machine.get().current.getTimings().processorSpeed()) * factor + 0.5, Integer.MAX_VALUE / 2);
        scheduler.schedule(this, lastTstates + tstates);
        startTime = currentTime + TEN_MS / 1000.0;
      }
    }
  }

  /** Whether the card is what holds the machine to its speed, so nothing else should. */
  public boolean soundPaces() {
    return soundOutput.enabled && speed.emulation <= 100;
  }

  private void frameCallbackSound(long lastTstates) {
    scheduler.schedule(tick, lastTstates + machine.get().current.getTimings().tstatesPerFrame());
  }

  private double getTime() {
    return System.nanoTime() / 1_000_000_000.0;
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Where the clock is put when the speed changes, which is near the end of a frame. */
  private static final int REBASED_TO = 60000;

  /**
   * How fast the machine is asked to run from now on. The clock is rebased because the pacing is
   * worked out from a T-state count that a speed change makes meaningless, and the sound is
   * rebuilt because a frame's worth of samples is sized for the speed it is played at.
   */
  public void changeSpeed(int emulationSpeed) {
    speed.emulation = emulationSpeed;
    // Through the scheduler, because everything waiting has to move with the clock. Moving the
    // clock alone left a tape edge due in two hundred T-states due fifty thousand later, or already
    // past and fired at once, depending on where in the frame the speed was changed - and a loader
    // told that a pulse lasted a frame stops loading.
    scheduler.moveClockTo(REBASED_TO);
    this.changeRequested = true;
    scheduler.schedule(tick, REBASED_TO + 10000);
    sound.rebuildOutput();
  }

}
