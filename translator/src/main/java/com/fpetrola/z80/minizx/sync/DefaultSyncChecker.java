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

package com.fpetrola.z80.minizx.sync;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulation;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.*;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class DefaultSyncChecker implements SyncChecker {
  public static final int maxwait = 100;
  volatile int checking;
  volatile int checkingEmu;
  volatile Stack<StateSync> stateSync = new Stack();
  MiniZXWithEmulation miniZXWithEmulation;
  OOZ80<WordNumber> ooz80;
  private SpectrumApplication spectrumApplication;
  private final Map<String, Integer> writtenRegisters = new HashMap<>();
  private int syncEmuCounter;
  private int syncJavaCounter;
  private int port;
  private int pc;
  private MiniZXIO io;
  private List<Integer> rValues = Collections.synchronizedList(new ArrayList<>());

  public <T extends WordNumber> OOZ80<T> createOOZ80(MiniZXIO io) {
    this.io = io;
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

      public Register<T> createRRegister() {
        return new RRegister<T>() {
          public T read() {
            T read = super.read();
            int e = read.intValue();
            System.out.println("emu R: " + e);
            rValues.add(e);
            return read;
          }
        };
      }
    };
    var state = new State(io, registerBankFactory.createBank(), new MockedMemory(true));
    io.setPc(state.getPc());
    return Z80Factory.createOOZ80(state);
  }

  public DefaultSyncChecker() {
    com.fpetrola.z80.helpers.Helper.hex = true;
    MiniZXIO io = (MiniZXIO) SpectrumApplication.io;
    SpectrumApplication.io = io;
    ooz80 = createOOZ80(io);
    ooz80.getState().getMemory().canDisable(true);
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
    memory.addMemoryWriteListener((address, value) -> {
      checkSyncEmu(address.intValue(), value.intValue(), pc.read().intValue(), true);
    });
    memory.addMemoryReadListener((address, value, delta, fetching) -> {
      if (address.intValue() >= 0) {
        if (fetching == 0) {
//          System.out.println("read memory at: " + com.fpetrola.z80.helpers.Helper.formatAddress(address.intValue()));
          checkSyncEmu(address.intValue(), value.intValue(), pc.read().intValue(), false);
        }
      }
    });

    miniZXWithEmulation = new MiniZXWithEmulation(ooz80, this.spectrumApplication);
    miniZXWithEmulation.copyStateBackToEmulation();
    pc.write(WordNumber.createValue(0xC804));
    new Thread(() -> miniZXWithEmulation.emulate()).start();
  }

  @Override
  public void checkSyncEmu(int address, int value, int pc, boolean write) {
    System.out.println("sync emu: " + formatAddress(pc));
    syncEmuCounter++;
    while (checking == 0 || syncEmuCounter > maxwait) ;
    if (checking != pc)
      System.out.print("");
    else {
      checkMatching(pc, address, write);
      checking = 0;
    }
  }

  @Override
  public void checkSyncJava(int address, int value, int pc) {
    System.out.println("sync java: " + formatAddress(pc));
    syncEmuCounter++;
    checking = pc;
    while (checking != 0 || syncJavaCounter > maxwait) ;
  }

  @Override
  public void checkMatching(int pc, int address, boolean write) {
    if (!miniZXWithEmulation.stateIsMatching(writtenRegisters, address, write)) {
      System.out.println("not matching at: " + formatAddress(pc));
    } else {
      syncEmuCounter = 0;
      syncJavaCounter = 0;
      System.out.println("ok at: " + formatAddress(pc));
    }
    stateSync.clear();
  }

  @Override
  public void checkSyncInJava(int port, int pc) {
    this.port = port;
    this.pc = pc;
    io.javaPC = pc;
  }

  public int getR() {
    while (rValues.isEmpty()) ;
    Integer e = rValues.get(rValues.size() - 1);
    rValues.remove(rValues.size() - 1);
    System.out.println("java R: " + e);

    return e;
//    return ooz80.getState().getRegister(R).read().intValue();
  }
}