package fuse;// Code authored by Dean Edis (DeanTheCoder).
// Anyone is free to copy, modify, use, compile, or distribute this software,
// either in source code form or as a compiled binary, for any non-commercial
// purpose.
//
// If you modify the code, please retain this copyright header,
// and consider contributing back to the repository or letting us know
// about your modifications. Your contributions are valued!
//
// THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND.

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
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

  public FuseTest(String testId, String registers, String state, String memory) {
    this.testId = testId;
    this.registers = registers;
    this.state = state;
    this.memory = memory;
  }

  public void initCpu(Z80Cpu<T> cpu) {
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

    boolean isHalted = stateArray.get(5) != 0;
    if (isHalted) {
      throw new UnsupportedOperationException("Halted state is not supported.");
    }

    // Load memory
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
    return "FuseTest{" +
        "id='" + testId + '\'' +
        ", memory='" + memory + '\'' +
        ", state='" + state + '\'' +
        ", registers='" + registers + '\'' +
        '}';
  }

  public boolean run(Z80Cpu<WordNumber> cpu, int expectedPc) {
    int ticks = 0;
    do {
      cpu.execute();
    } while (cpu.getState().getPc().read().intValue() < expectedPc && ticks++ < 65536);

    return ticks < 65536;
  }
}
