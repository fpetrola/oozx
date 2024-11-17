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

import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.cpu.Event;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.fpetrola.z80.registers.RegisterName.*;

public class FuseResult<T extends WordNumber> {
  private final int[] registers;
  private final List<Event> events;
  private final int[] state;
  private final String memory;
  private final int tStates;
  private final String testId;

  public FuseResult(String testId, String registers, String state, String memory, List<Event> events) {
    this.registers = Arrays.stream(registers.split(" "))
        .filter(s -> !s.isEmpty())
        .mapToInt(s -> Integer.parseInt(s, 16))
        .toArray();
    this.events = events;

    List<Integer> stateList = Arrays.stream(state.split(" "))
        .filter(s -> !s.isEmpty())
        .map(s -> Integer.parseInt(s, 16))
        .collect(Collectors.toList());

    this.state = stateList.subList(0, stateList.size() - 1).stream().mapToInt(Integer::intValue).toArray();
    this.tStates = stateList.get(stateList.size() - 1);
    this.memory = memory;
    this.testId = testId;
  }

  public String getTestId() {
    return testId;
  }

  public int getExpectedPC() {
    return registers[11];
  }

  public void verify(Z80Cpu<T> cpu) {
    Assertions.assertAll(
        () -> verifyEvents(cpu),
        () -> verifyRegisters(cpu),
        () -> verifyCpuState(cpu),
        () -> verifyMemory(cpu)
    );
  }

  private void verifyEvents(Z80Cpu<T> cpu) {
    List<Event> eventsFromCpu= cpu.getState().getEvents();
//    Assertions.assertEquals(events, eventsFromCpu, "Events mismatch");
  }

  private void verifyMemory(Z80Cpu<T> cpu) {
    for (String memoryLine : memory.split("\n")) {
      if (!memoryLine.isBlank()) {
        int[] memoryData = Arrays.stream(memoryLine.split(" "))
            .filter(s -> !s.equals("-1"))
            .mapToInt(s -> Integer.parseInt(s, 16))
            .toArray();

        int addr = memoryData[0];
        for (int value : Arrays.copyOfRange(memoryData, 1, memoryData.length)) {
          int actual = cpu.getState().getMemory().getData()[addr++].intValue();
          Assertions.assertEquals(value, actual, "Memory mismatch at address " + Integer.toHexString(addr - 1));
        }
      }
    }
  }

  private void verifyCpuState(Z80Cpu<T> cpu) {
    Assertions.assertEquals(state[0], getRegisterValue(cpu, I), "Register mismatch: I");
    Assertions.assertEquals(state[1], getRegisterValue(cpu, R), "Register mismatch: R");
    Assertions.assertEquals(state[2] != 0, cpu.getState().isIff1(), "Register mismatch: IFF1");
    Assertions.assertEquals(state[3] != 0, cpu.getState().isIff2(), "Register mismatch: IFF2");
    Assertions.assertEquals(state[4], cpu.getState().getInterruptionMode().ordinal(), "Register mismatch: IM");
//    Assertions.assertEquals(tStates, cpu.getState().getTStatesSinceCpuStart(), "Mismatch in T-states");
  }

  private int getRegisterValue(Z80Cpu<T> cpu, RegisterName registerName) {
    return cpu.getState().getRegister(registerName).read().intValue();
  }

  private void verifyRegisters(Z80Cpu<T> cpu) {
    int[] regs = registers;

    Assertions.assertEquals((regs[0] & 0xff00) >> 8, getRegisterValue(cpu, A), "Register mismatch: A");

    int mainF = getRegisterValue(cpu, F);
    int expectedF = regs[0] & 0x00ff;
    Assertions.assertEquals(expectedF, mainF, String.format("Flags (F): SZ5H3PNC%nActual:    %s%nExpected:  %s",
        String.format("%8s", Integer.toBinaryString(mainF)).replace(' ', '0'),
        String.format("%8s", Integer.toBinaryString(expectedF)).replace(' ', '0')
    ));

    Assertions.assertEquals(regs[1], getRegisterValue(cpu, BC), "Register mismatch: BC");
    Assertions.assertEquals(regs[2], getRegisterValue(cpu, DE), "Register mismatch: DE");
    Assertions.assertEquals(regs[3], getRegisterValue(cpu, HL), "Register mismatch: HL");

    Assertions.assertEquals((regs[4] & 0xff00) >> 8, getRegisterValue(cpu, Ax), "Register mismatch: A'");

    int altF = getRegisterValue(cpu, Fx);
    expectedF = regs[4] & 0x00ff;
    Assertions.assertEquals(expectedF, altF, String.format("Flags (F'): SZ5H3PNC%nActual:     %s%nExpected:   %s",
        String.format("%8s", Integer.toBinaryString(altF)).replace(' ', '0'),
        String.format("%8s", Integer.toBinaryString(expectedF)).replace(' ', '0')
    ));

    Assertions.assertEquals(regs[5], getRegisterValue(cpu, BCx), "Register mismatch: BC'");
    Assertions.assertEquals(regs[6], getRegisterValue(cpu, DEx), "Register mismatch: DE'");
    Assertions.assertEquals(regs[7], getRegisterValue(cpu, HLx), "Register mismatch: HL'");
    Assertions.assertEquals(regs[8], getRegisterValue(cpu, IX), "Register mismatch: IX");
    Assertions.assertEquals(regs[9], getRegisterValue(cpu, IY), "Register mismatch: IY");
    Assertions.assertEquals(regs[10], getRegisterValue(cpu, SP), "Register mismatch: SP");
    Assertions.assertEquals(regs[11], getRegisterValue(cpu, PC), "Register mismatch: PC");
    Assertions.assertEquals(regs[12], getRegisterValue(cpu, MEMPTR), "Register mismatch: MEMPTR");
  }
}
