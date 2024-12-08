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

import com.fpetrola.z80.cpu.MemorySetter;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;

import java.util.function.Function;

public class EmulatedMiniZX<T extends WordNumber> {
  private OOZ80<T> ooz80;

  public static void main(String[] args) {
    new EmulatedMiniZX().start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80(MiniZXIO io) {
    DefaultRegisterBankFactory registerBankFactory = new DefaultRegisterBankFactory() {
      @Override
      protected Register create8BitRegister(RegisterName registerName) {
        return new Plain8BitRegister(registerName.name()) {
          public void write(WordNumber value) {
            super.write(value);
          }
        };
      }

    };
    var state = new State(io, registerBankFactory.createBank(), new MockedMemory(true));
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory<T>(state)));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(WordNumber.createValue(index), 0).intValue();
  }

  private void start() {
    MiniZXIO io = new MiniZXIO();
    ooz80 = createOOZ80(io);
    MiniZX.createScreen(io.miniZXKeyboard, this.getMemFunction());
    final byte[] rom = MiniZX.createROM();

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState()) {
      public VirtualRegisterFactory getVirtualRegisterFactory() {
        return new VirtualRegisterFactory(null, null, null);
      }
    };

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/detodo/desarrollo/m/zx/zx/jsw.z80");
//    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/dynamitedan1.z80");
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, new MemorySetter(ooz80.getState().getMemory(), rom));

    new Thread(() -> emulate()).start();
  }

  public void emulate() {
    int i = 0;
    while (true) {
      if (i++ % 100000 == 0) this.ooz80.getState().setINTLine(true);
      else if (i % 2 == 0) {
        this.ooz80.execute();
      }
    }
  }
}
