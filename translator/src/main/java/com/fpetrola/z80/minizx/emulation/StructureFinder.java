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
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.ExecutionListener;

import java.util.*;

public class StructureFinder<T extends WordNumber> {
  private final OOZ80<T> ooz80;
  private final Z80Rewinder<T> z80Rewinder;
  private Map<Integer, PcVisit> visitsByPC = new HashMap<>();

  public StructureFinder(OOZ80<T> ooz80, Z80Rewinder<T> z80Rewinder) {
    this.ooz80 = ooz80;
    this.z80Rewinder = z80Rewinder;

    this.ooz80.getInstructionExecutor().addExecutionListener(new ExecutionListener<T>() {
      public void afterExecution(Instruction<T> instruction) {
        if (instruction instanceof Ld<T> ld) {
          if (ld.getSource() instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
            findPaths(memoryPlusRegister8BitReference);
          } else if (ld.getTarget() instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
            findPaths(memoryPlusRegister8BitReference);
          }
        }
      }
    });
  }

  private void findPaths(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
    StateDelta<T> lastDelta = z80Rewinder.getLastDelta();
    long ticks = ooz80.getState().getTicks();

    Integer pcValue = lastDelta.getRegisterNewValues().get(RegisterName.PC.name());
    if (pcValue != null) {
      PcVisit pcVisit = visitsByPC.get(pcValue);
      if (pcVisit == null) {
        pcVisit = new PcVisit(1, ticks);
        visitsByPC.put(pcValue, pcVisit);
      }

      if (pcVisit.valid(ticks)) {
        ImmutableOpcodeReference<T> target = memoryPlusRegister8BitReference.getTarget();
        List<StateDelta<T>> states = new ArrayList<>();

        LinkedList<StateDelta<T>> deltas = z80Rewinder.deltas;
        if (target.toString().equals("IX"))
          deltas = z80Rewinder.ixdeltas;
        if (target.toString().equals("IY"))
          deltas = z80Rewinder.iydeltas;
        z80Rewinder.backPathUntil(stateDelta -> {
          Map<String, Integer> registerChanges = stateDelta.getRegisterChanges();
          if (registerChanges.containsKey(target.toString())) {
            states.add(stateDelta);
            return !(stateDelta.getInstruction() instanceof Ld ld2 || stateDelta.getInstruction() instanceof Pop<T>);
          }
          return true;
        }, deltas);

        pcVisit.counter++;
        System.out.printf("%s:--> %s -> %s%n", ticks, lastDelta, states);
      }
    }
  }

}
