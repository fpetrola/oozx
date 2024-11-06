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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.base.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.MemoryReadListener;
import com.fpetrola.z80.jspeccy.MemoryWriteListener;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulation;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.mmu.IO;
import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.NullInstructionSpy;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Semaphore;

public class DefaultSyncChecker implements SyncChecker {
  static Semaphore semaphore = new Semaphore(2);
  volatile int checking;
  volatile int checkingEmu;
  volatile static Stack<StateSync> stateSync = new Stack();
  MiniZXWithEmulation miniZXWithEmulation;
  static OOZ80<WordNumber> ooz80;
  private SpectrumApplication spectrumApplication;
  private Map<String, Integer> writtenRegisters = new HashMap<>();

  public <T extends WordNumber> OOZ80<T> createOOZ80(IO io) {
    DefaultRegisterBankFactory registerBankFactory = new DefaultRegisterBankFactory() {
      @Override
      protected Register create8BitRegister(RegisterName registerName) {
        return new Plain8BitRegister(registerName.name()) {
          public void write(WordNumber value) {
            super.write(value);
            writtenRegisters.put(getName(), value.intValue());
          }
        };
      }

    };
    var state = new State(io, registerBankFactory.createBank(), new MockedMemory());
    return new OOZ80(state, DefaultInstructionFetcher.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory<T>(state)));
  }

  public DefaultSyncChecker() {
    this.ooz80 = createOOZ80(SpectrumApplication.io);
  }

  @Override
  public int getByteFromEmu(Integer index) {
    WordNumber datum = ooz80.getState().getMemory().getData()[index];
    if (datum == null)
      datum = WordNumber.createValue(0);
    return datum.intValue();
  }

  @Override
  public void init(SpectrumApplication spectrumApplication) {
    this.spectrumApplication = spectrumApplication;
    Register<WordNumber> pc = ooz80.getState().getPc();
    Memory<WordNumber> memory = ooz80.getState().getMemory();
    memory.addMemoryWriteListener((MemoryWriteListener<WordNumber>) (address, value) -> {
      checkSyncEmu(address.intValue(), value.intValue(), pc.read().intValue(), true);
    });
    memory.addMemoryReadListener((MemoryReadListener<WordNumber>) (address, value) -> {
      checkSyncEmu(address.intValue(), value.intValue(), pc.read().intValue(), false);
    });

    miniZXWithEmulation = new MiniZXWithEmulation(ooz80, this.spectrumApplication);
    miniZXWithEmulation.copyStateBackToEmulation();
    pc.write(WordNumber.createValue(34762));
    new Thread(() -> miniZXWithEmulation.emulate()).start();
  }

  @Override
  public void checkSyncEmu(int address, int value, int pc, boolean write) {
    System.out.println("sync emu: " + pc);
    while (checking == 0) ;
    if (checking != pc)
      System.out.print("");
    else {
      checkMatching(pc, address, write);
      checking = 0;
    }
  }

  @Override
  public void checkSyncJava(int address, int value, int pc) {
    System.out.println("sync java: " + pc);

    checking = pc;
    while (checking != 0) ;
  }

  @Override
  public void checkMatching(int pc, int address, boolean write) {
    if (!miniZXWithEmulation.stateIsMatching(writtenRegisters, address, write)) {
      System.out.println("not matching at: " + pc);
    } else {
      System.out.println("ok at: " + pc);
    }
    stateSync.clear();
  }
}