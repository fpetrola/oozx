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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ObservableRegister;

import java.util.function.Predicate;

public class DefaultEmulator<T extends WordNumber> implements Emulator<T> {
  private int fetchCounter;
  private OOZ80<T> ooz80;
  private int emulateUntil;
  private int pause;
  private Predicate<Integer> continueEmulationCondition;
  private Predicate<Integer> interruptionCondition;

  public void emulate() {
    int i = 0;
    while (continueEmulationCondition.test(i++)) {
      if (interruptionCondition.test(fetchCounter)) {
        ooz80.getState().setINTLine(true);
        ooz80.execute();
        ooz80.getState().setINTLine(false);
      } else {
        ooz80.execute();
      }
    }
  }

  public void setup(OOZ80<T> ooz80, int emulateUntil1, int pause1, Predicate<Integer> continueEmulation, Predicate<Integer> interruptionCondition) {
    this.ooz80 = ooz80;
    this.emulateUntil = emulateUntil1;
    this.pause = pause1;
    this.continueEmulationCondition = continueEmulation;
    this.interruptionCondition = interruptionCondition;
    ObservableRegister<T> registerR = (ObservableRegister<T>) ooz80.getState().getRegisterR();
    registerR.addRegisterWriteListener((value, isIncrement) -> {
      if (isIncrement)
        fetchCounter++;
    });
    registerR.setListening(true);
  }
}
