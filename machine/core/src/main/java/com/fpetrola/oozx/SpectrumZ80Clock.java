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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.modules.tape.Log1;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.z80.cpu.DefaultZ80Clock;
import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.emulation.helpers.machine.ClockTimeoutListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpectrumZ80Clock extends DefaultZ80Clock {
  protected Consumer<Integer> timeoutProcessor;
  private int timeout;
  private CollectionHandler<ClockTimeoutListener> clockListeners = new CollectionHandler<>();

  public void addTStates(int tStatesToAdd) {
    this.tStates += tStatesToAdd;
    if (timeoutProcessor != null)
      timeoutProcessor.accept(tStatesToAdd);
  }

  public void addTStates(int tStatesToAdd, String description) {
    this.tStates += tStatesToAdd;
    if (timeoutProcessor != null)
      timeoutProcessor.accept(tStatesToAdd);
  }

  public void addTStates(int tStatesToAdd, Supplier<String> description) {
    addTStates(tStatesToAdd);
  }

  private void timeOutProcess(int tStatesToAdd) {
    if (timeout > 0 && tStatesToAdd >= 0) {
      timeout -= tStatesToAdd;

//      if (timeout > 60000)
//        System.out.println("max1");
      if (timeout <= 0) {
        int res = timeout;
        clockListeners.forAll(ClockTimeoutListener::clockTimeout);

        if (timeout > 0) {
          new Log1().trace("Timeout: {}, res: {}", timeout, res);
          timeout += res;
        }
      }
    }
  }

  /**
   * Moves the clock to a new position without counting the move as time that has passed.
   * <p>
   * A speed change repositions the clock, which is bookkeeping rather than elapsed time. Doing it
   * with addTStates fed the difference to the timeout countdown, and since the difference is
   * positive whenever the clock sits below the target - most of a frame - it ate up to 60000
   * T-states of whatever was waiting. The tape waits that way, a pulse at a time, so changing
   * speed while a tape was loading skipped it past dozens of pulses and the loader lost sync.
   */
  public void rebaseTStates(int newTStates) {
    this.tStates = newTStates;
  }

  public void setTimeout(int ntstates) {
    if (this.timeout <= 0) {
      this.timeout = Math.max(ntstates, 10);
      timeoutProcessor = this::timeOutProcess;
    }
  }

  public void addClockTimeoutListener(Tape tape) {
    clockListeners.add(tape);
  }

  public void removeClockTimeoutListener(Tape tape) {
    clockListeners.remove(tape);
  }

  public long getAbsTstates() {
    return tStates;
  }
}
