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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.HashMap;
import java.util.Map;

import static com.fpetrola.z80.helpers.Helper.formatAddress;
import static com.fpetrola.z80.registers.RegisterName.PC;

public class StateDelta<T extends WordNumber> {
  private final Map<Integer, Integer> memoryChanges = new HashMap<>();

  private final Map<String, Integer> registerOldValues = new HashMap<>();

  private final Map<String, Integer> registerNewValues = new HashMap<>();

  private OOZ80<T> ooz80;

  private Memory<T> memory;
  private Instruction<T> instruction;

  public Map<String, Integer> getRegisterNewValues() {
    return registerNewValues;
  }

  public StateDelta(OOZ80<T> ooz80) {
    this.ooz80 = ooz80;
    memory = ooz80.getState().getMemory();
  }

  public Map<String, Integer> getRegisterChanges() {
    return registerOldValues;
  }

  public void setInstruction(Instruction<T> instruction) {
    this.instruction = instruction;
  }

  public void addMemoryChange(T a, T v) {
    T read = memory.read(a, 0);
    int value = v.intValue();
    memoryChanges.put(a.intValue(), read.intValue());
  }

  public void addRegisterChange(Register<T> r, T v, boolean i) {
    if (!registerOldValues.containsKey(r.getName())) {
      registerOldValues.put(r.getName(), r.read().intValue());
    }
    if (!registerNewValues.containsKey(r.getName())) {
      registerNewValues.put(r.getName(), v.intValue());
    }
  }

  public void applyReverse() {
    for (Map.Entry<Integer, Integer> entry : memoryChanges.entrySet()) {
      ooz80.getState().getMemory().write(WordNumber.createValue(entry.getKey()), WordNumber.createValue(entry.getValue()));
    }

    for (Map.Entry<String, Integer> entry : registerOldValues.entrySet()) {
      ooz80.getState().getRegisterBank().get(entry.getKey()).write(WordNumber.createValue(entry.getValue()));
    }

    if (instruction != null) {
      int pc = ooz80.getState().getPc().read().intValue();
      ooz80.getState().getPc().write(WordNumber.createValue(pc - instruction.getLength()));
    }
  }

  public Instruction<T> getInstruction() {
    return instruction;
  }

  public String toString() {
//    Integer i1 = registerNewValues.get("PC");
//    if (registerNewValues.containsKey("PC") && i1.intValue() == 0xF27C)
//      System.out.println("gola");
//
//    Register<T> pc = ooz80.getState().getPc();
//    T i = pc.read();
//    pc.write(WordNumber.createValue(i1));
    String toString = new ToStringInstructionVisitor<T>().createToString(instruction);
//    pc.write(i);

    return "%s: %s".formatted(formatAddress(registerNewValues.get(PC.name())), toString);
  }

  public int getPc() {
    Integer i = registerNewValues.get("PC");
    return i != null ? i : -1;
  }
}
