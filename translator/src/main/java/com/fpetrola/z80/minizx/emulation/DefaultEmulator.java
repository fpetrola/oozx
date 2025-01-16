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
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ObservableRegister;

public class DefaultEmulator<T extends WordNumber> implements Emulator<T> {

  private int fetchCounter;

  public void emulate(OOZ80 ooz80, int emulateUntil, int pause) {
    Register<T> pc = ooz80.getState().getPc();
    int i = 0;

//    ObservableRegister<T> registerR = (ObservableRegister<T>) ooz80.getState().getRegisterR();
//    registerR.addIncrementWriteListener(value -> {
//      fetchCounter++;
//    });
//    registerR.listening(true);

    while (pc.read().intValue() != emulateUntil) {
      if (fetchCounter > emulateUntil)
        break;
      if ((i++ % (pause * 1000000)) == 0) {
        ooz80.getState().setINTLine(true);
      } else {
        if (i % pause == 0) {
          ooz80.execute();
          ooz80.getState().setINTLine(false);
        }
      }
    }
  }
}
