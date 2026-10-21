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

package com.fpetrola.z80.minizx.emulation.finders;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.base.ToStringInstructionVisitor;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.HashMap;
import java.util.Map;

import static com.fpetrola.z80.helpers.Helper.formatAddress;
import static com.fpetrola.z80.registers.RegisterName.PC;

public class StateDelta {
  private final Map<Integer, Integer> memoryChanges = new HashMap<>();

  private final Map<String, Integer> registerOldValues = new HashMap<>();

  private final Map<String, Integer> registerNewValues = new HashMap<>();

  private OOZ80 ooz80;

  private Memory memory;
  private Instruction instruction;

  public Map<String, Integer> getRegisterNewValues() {
    return registerNewValues;
  }

  public StateDelta(OOZ80 ooz80) {
    this.ooz80 = ooz80;
    memory = ooz80.getState().getMemory();
  }

  public Map<String, Integer> getRegisterChanges() {
    return registerOldValues;
  }

  public void setInstruction(Instruction instruction) {
    this.instruction = instruction;
  }

  public void addMemoryChange(int a, int v) {
    int read = memory.read(a, 0);
    int value = v;
    memoryChanges.put(a, read);
  }

  public void addRegisterChange(Register r, int v, boolean i) {
    if (!registerOldValues.containsKey(r.getName())) {
      registerOldValues.put(r.getName(), r.read());
    }
    if (!registerNewValues.containsKey(r.getName())) {
      registerNewValues.put(r.getName(), v);
    }
  }

  public void applyReverse() {
    for (Map.Entry<Integer, Integer> entry : memoryChanges.entrySet()) {
      ooz80.getState().getMemory().write(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, Integer> entry : registerOldValues.entrySet()) {
      ooz80.getState().getRegister(RegisterName.valueOf(entry.getKey())).write(entry.getValue());
    }

    if (instruction != null) {
      int pc = ooz80.getState().getPc().read();
      ooz80.getState().getPc().write(pc - instruction.getLength());
    }
  }

  public Instruction getInstruction() {
    return instruction;
  }

  public String toString() {
//    Integer i1 = registerNewValues.get("PC");
//    if (registerNewValues.containsKey("PC") && i1 == 0xF27C)
//      System.out.println("gola");
//
//    Register pc = ooz80.getState().getPc();
//    int i = pc.read();
//    pc.write(i1);
    String toString = new ToStringInstructionVisitor().createToString(instruction);
//    pc.write(i);

    return "%s: %s".formatted(formatAddress(registerNewValues.get(PC.name())), toString);
  }

  public int getPc() {
    Integer i = registerNewValues.get("PC");
    return i != null ? i : -1;
  }
}
