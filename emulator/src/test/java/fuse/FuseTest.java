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

package fuse;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.*;

public class FuseTest<T extends WordNumber> {
  public final String registers;
  public final String state;
  public final String memory;
  public final String testId;
  public Z80Cpu<T> cpu;
  private String name = "";

  public FuseTest(String testId, String registers, String state, String memory, Z80Cpu<T> z80Cpu) {
    this.testId = testId;
    this.registers = registers;
    this.state = state;
    this.memory = memory;
    cpu = z80Cpu;
  }

  public void initCpu() {
    cpu.reset();

    List<Integer> registersArray = Arrays.stream(registers.split(" "))
        .filter(s -> !s.isEmpty()).map(s -> Integer.parseInt(s, 16)).toList();

    RegisterName[] registerNames = {AF, BC, DE, HL, AFx, BCx, DEx, HLx, IX, IY, SP, PC};

    IntStream.range(0, registerNames.length)
        .forEach(i -> cpu.getState().getRegister(registerNames[i])
            .write(createValue(registersArray.get(i))));

    List<Integer> stateArray = Arrays.stream(state.split(" ")).filter(s -> !s.isEmpty()).map(s -> Integer.parseInt(s, 16)).toList();

    cpu.getState().getRegister(I).write(createValue(stateArray.get(0).byteValue()));
    cpu.getState().getRegister(R).write(createValue(stateArray.get(1).byteValue()));
    cpu.getState().setIff1(stateArray.get(2) != 0);
    cpu.getState().setIff2(stateArray.get(3) != 0);
    cpu.getState().setIntMode(State.InterruptionMode.values()[stateArray.get(4).intValue()]);
    cpu.getState().setHalted(false);

    boolean isHalted = stateArray.get(5) != 0;
    if (isHalted) {
      throw new UnsupportedOperationException("Halted state is not supported.");
    }

    for (String memoryLine : memory.split("\n")) {
      if (!memoryLine.isBlank()) {
        List<Integer> memoryArray = Arrays.stream(memoryLine.split(" ")).filter(s -> !s.equals("-1")).map(s -> Integer.parseInt(s, 16)).collect(Collectors.toList());

        int addr = memoryArray.get(0);
        for (int value : memoryArray.subList(1, memoryArray.size())) {
          cpu.getState().getMemory().getData()[addr++] = createValue(value);
        }
      }
    }
  }

  @Override
  public String toString() {
    return name + "   [id='" + testId + '\'' +
        ", memory='" + memory.replace(" -1", " | ") + '\'' +
        ']';
  }

  public boolean run(int expectedPc) {
    int ticks = 0;
    int i;
    do {
      cpu.execute();
      i = cpu.getState().getPc().read().intValue();
//      System.out.println("pc: " + i + " - expected: " + expectedPc + " - ticks: " + ticks);
    } while (i < expectedPc && ticks++ < 65536);
    return ticks < 65536;
  }

  public void run() {
    cpu.execute();
    cpu.getState().getPc().write(createValue(0));
    name = ((FuseTestParser.MyDefaultInstructionFetcher) cpu.getInstructionFetcher()).getLastInstruction().toString();
  }
}
