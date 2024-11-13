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

package fuse;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.registers.RegisterName;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fpetrola.z80.registers.RegisterName.*;

public class FuseTest {
  public final String registers;
  public final String state;
  public final String memory;
  public final String testId;
  public Z80Cpu cpu;
  public final RecordedEvents events;
  private final List<String> namesLines;
  private final int lineNumber;

  public FuseTest(String testId, String registers, String state, String memory, Z80Cpu z80Cpu, RecordedEvents events, List<String> namesLines, int lineNumber) {
    this.testId = testId;
    this.events = events;
    this.registers = registers;
    this.state = state;
    this.memory = memory;
    cpu = z80Cpu;
    this.namesLines = namesLines;
    this.lineNumber = lineNumber;
  }

  public void initCpu() {
    cpu.reset();
    events.clear();

    List<java.lang.Integer> registersArray = Arrays.stream(registers.split(" "))
        .filter(s -> !s.isEmpty()).map(s -> java.lang.Integer.parseInt(s, 16)).toList();

    RegisterName[] registerNames = {AF, BC, DE, HL, AFx, BCx, DEx, HLx, IX, IY, SP, PC, MEMPTR};

    IntStream.range(0, registerNames.length)
        .forEach(i -> cpu.getState().getRegister(registerNames[i])
            .write(registersArray.get(i)));

    List<java.lang.Integer> stateArray = Arrays.stream(state.split(" ")).filter(s -> !s.isEmpty()).map(s -> java.lang.Integer.parseInt(s, 16)).toList();

    cpu.getState().getRegister(I).write(stateArray.get(0));
    cpu.getState().getRegister(R).write(stateArray.get(1));
    cpu.getState().setIff1(stateArray.get(2) != 0);
    cpu.getState().setIff2(stateArray.get(3) != 0);
    cpu.getState().setIntMode(State.InterruptionMode.values()[stateArray.get(4)]);
    cpu.getState().setHalted(false);

    cpu.getState().getMemory().reset();

    boolean isHalted = stateArray.get(5) != 0;
    if (isHalted) {
      throw new UnsupportedOperationException("Halted state is not supported.");
    }

    for (String memoryLine : memory.split("\n")) {
      if (!memoryLine.isBlank()) {
        List<java.lang.Integer> memoryArray = Arrays.stream(memoryLine.split(" ")).filter(s -> !s.equals("-1")).map(s -> java.lang.Integer.parseInt(s, 16)).collect(Collectors.toList());

        int addr = memoryArray.get(0);
        for (int value : memoryArray.subList(1, memoryArray.size())) {
          cpu.getState().getMemory().poke(addr++, value);
        }
      }
    }
  }

  @Override
  public String toString() {
    return namesLines.get(lineNumber);
  }

  public boolean run(int expectedPc) {
    int ticks = 0;
    int i;
    do {
      cpu.execute();
      i = cpu.getState().getPc().read();
//      System.out.println("pc: " + i + " - expected: " + expectedPc + " - ticks: " + ticks);
    } while (i < expectedPc && ticks++ < 65536);
    return ticks < 65536;
  }
}
