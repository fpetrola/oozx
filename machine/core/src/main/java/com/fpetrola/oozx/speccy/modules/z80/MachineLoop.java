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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Running the machine: instructions until the next thing is due, whether it is stopped, and the
 * work another thread asked for.
 * <p>
 * Not the {@link Cpu}, which knows how to do one instruction and nothing about being driven, and
 * not the Timer, which decides how fast this is allowed to go rather than whether it goes at all.
 */
@Singleton
public class MachineLoop {
  private final Scheduler scheduler;
  private final SpectrumZ80Clock zxClock;
  private Cpu cpu;

  private volatile boolean paused;

  /**
   * Work asked for from another thread - the window, a menu - and done here, between instructions.
   * Selecting a machine rebuilds the port list and changing speed hands every sound source a new
   * synth; neither can happen while this thread is in the middle of a frame, and doing it from the
   * event thread is what used to hang the emulator on a fast enough double click.
   */
  private final ConcurrentLinkedQueue<Runnable> pending = new ConcurrentLinkedQueue<>();

  @Inject
  public MachineLoop(Scheduler scheduler, SpectrumZ80Clock zxClock) {
    this.scheduler = scheduler;
    this.zxClock = zxClock;
  }

  /**
   * The machine this drives. Handed in from the Z80's constructor rather than injected, so that
   * the two do not have to be built at the same time.
   */
  public void drive(Cpu cpu) {
    this.cpu = cpu;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public void doOpcodes() {
    while (scheduler.nextIsAfter(zxClock.getTStates())) {
      while (paused) Thread.onSpinWait();
      try {
        cpu.step();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    applyWhatWasDeferred();
  }

  /**
   * Does whatever was asked for with {@link #later}, which every loop that advances this machine
   * has to call.
   * <p>
   * It used to happen at the end of doOpcodes and nowhere else, which was the same thing while
   * doOpcodes was the only way a machine ran. A recording drives its machine frame by frame
   * instead, through RzxSession, so while one played nothing was ever applied: changing the
   * speed, changing the machine, plugging a device in - all queued and none of it happening,
   * with no sign that anything had been asked for.
   */
  public void applyWhatWasDeferred() {
    for (Runnable work = pending.poll(); work != null; work = pending.poll()) {
      work.run();
    }
  }

  /** Ask for something to be done on the emulator's own thread, as soon as it is between instructions. */
  public void later(Runnable work) {
    pending.add(work);
  }
}
