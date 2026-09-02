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
package com.fpetrola.z80.generate;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.Contention;
import org.junit.jupiter.api.Test;

/**
 * Instructions per second of each core on a small program that copies a block, adds it up and
 * loops: {@code mvn test -pl emulator -Dtest=CoreBenchmark}. Numbers, not assertions.
 */
public class CoreBenchmark {
  static final int[] PROGRAM = {
      0x21, 0x00, 0x40,       // LD HL,4000h
      0x11, 0x00, 0x60,       // LD DE,6000h
      0x01, 0x00, 0x08,       // LD BC,0800h
      0xED, 0xB0,             // LDIR
      0x06, 0xFF,             // LD B,FFh
      0x3C,                   // INC A
      0x86,                   // ADD A,(HL)
      0x23,                   // INC HL
      0x10, 0xFB,             // DJNZ -5
      0xDD, 0x7E, 0x02,       // LD A,(IX+2)
      0xCB, 0x11,             // RL C
      0xC3, 0x00, 0x80        // JP 8000h
  };
  static final IO NO_IO = new IO() {
    public int in(int port) {
      return 0xFF;
    }

    public void out(int port, int value) {
    }
  };

  static Z80Cpu oop() {
    State state = new State(NO_IO, new MockedMemory(true));
    DefaultInstructionFactory factory = new DefaultInstructionFactory(state);
    return new OOZ80(state, new DefaultInstructionFetcher(state, factory, false, false), new DefaultInstructionExecutor(state, false));
  }

  static Z80Cpu generated() {
    Memory memory = new MockedMemory(true);
    GeneratedZ80 core = new GeneratedZ80(memory, NO_IO) {
      public void contend(int address, int times, int tstates, Contention.Kind kind) {
      }
    };
    return new GeneratedZ80Cpu(new State(NO_IO, core, memory), core);
  }

  static void load(Z80Cpu cpu) {
    int[] data = cpu.getState().getMemory().getData();
    for (int i = 0; i < PROGRAM.length; i++)
      data[0x8000 + i] = PROGRAM[i];
    cpu.getState().getPc().write(0x8000);
    cpu.getState().getRegister(RegisterName.IX).write(0x4000);
  }

  static double run(Z80Cpu cpu, long instructions) {
    long start = System.nanoTime();
    for (long i = 0; i < instructions; i++)
      cpu.execute();
    return instructions / ((System.nanoTime() - start) / 1e9);
  }

  @Test
  public void instructionsPerSecond() {
    for (int round = 0; round < 3; round++) {
      Z80Cpu oop = oop();
      load(oop);
      run(oop, 5_000_000);
      double oopRate = run(oop, 30_000_000);
      Z80Cpu generated = generated();
      load(generated);
      run(generated, 5_000_000);
      double generatedRate = run(generated, 30_000_000);
      System.out.printf("round %d: OOP %.1f M instr/s, generated %.1f M instr/s, ratio %.2fx%n", round, oopRate / 1e6, generatedRate / 1e6, generatedRate / oopRate);
    }
  }
}
