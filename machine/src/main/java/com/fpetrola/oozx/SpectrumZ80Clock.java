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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;
import com.fpetrola.oozx.fuse.modules.tape.Log1;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.z80.cpu.DefaultZ80Clock;
import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.z80.registers.Register;
import machine.ClockTimeoutListener;

import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpectrumZ80Clock extends DefaultZ80Clock {
  private Consumer<java.lang.Integer> timeoutProcessor = (tStates) -> {
  };
  private int timeout;
  private CollectionHandler<ClockTimeoutListener> clockListeners = new CollectionHandler<>();

  private Register pc;

  public void setTStates(int tStates) {
    super.setTStates(tStates);
//    timeout = 0;
  }

  public void addTStates(int tStatesToAdd) {
    super.addTStates(tStatesToAdd);
    timeoutProcessor.accept(tStatesToAdd);
  }

  public void addTStates(int tStatesToAdd, String description) {
    log(() -> description, (byte) tStatesToAdd);
    addTStates(tStatesToAdd);
  }

  public void addTStates(int tStatesToAdd, Supplier<String> description) {
    log(description, (byte) tStatesToAdd);
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

  public void log(Supplier<String> description, byte data) {
    GetTStatesHistory.addTStateUpdate(data, description, tStates, pc);
  }

  public void setPc(Register pc) {
    this.pc = pc;
  }

  public void setTimeout(int ntstates) {
    if (this.timeout > 0) {
      throw new ConcurrentModificationException("A timeout is in progress. Can't set another timeout!");
    } else{
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
