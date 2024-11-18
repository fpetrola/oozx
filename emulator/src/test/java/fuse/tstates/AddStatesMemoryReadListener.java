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

package fuse.tstates;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.fpetrola.z80.registers.RegisterName.*;

public class AddStatesMemoryReadListener<T extends WordNumber> implements MemoryReadListener<T> {
  private Runnable lastEvents;
  private Z80Cpu<T> cpu;
  private final State<T> state;
  private final DefaultInstructionFetcher instructionFetcher;
  private StatesAdder<T, Integer> afterFetchAdder;
  private StatesAdder<T, List<StatesAddition>> afterExecutionAdder;

  public AddStatesMemoryReadListener(Z80Cpu cpu) {
    this.cpu = cpu;
    this.state = cpu.getState();
    this.instructionFetcher = (DefaultInstructionFetcher) cpu.getInstructionFetcher();
    afterExecutionAdder = new AfterExecutionAdder<>(cpu);
    afterFetchAdder = new AfterFetchAdder<>(cpu);
  }

  public static <T extends WordNumber, R> R addMcWith(StatesAdder<T, R> afterFetchAdder1, R result, Consumer<R> action, DefaultInstructionFetcher instructionFetcher1) {
    afterFetchAdder1.setResult(result);
    var instruction = instructionFetcher1.instruction2;
    instruction.accept(afterFetchAdder1);

    if (afterFetchAdder1.getResult() != null)
      action.accept(afterFetchAdder1.getResult());

    return afterFetchAdder1.getResult();
  }

  public static <T extends WordNumber> void addMc(int x, Z80Cpu<T> cpu, int time1, int delta, int i1) {
    for (int i = 0; i < x; i++) {
      cpu.getState().addEvent(new Event(time1, "MC", i1 + delta, null));
    }
  }

  public boolean addIfIndirectHL(TargetInstruction tBitOperation) {
    if (AfterExecutionAdder.isIndirectHL(tBitOperation)) {
      if (cpu.getState().getTStatesSinceCpuStart() == 11) {
        addMc(1, cpu, 1, 0, cpu.getState().getRegister(HL).read().intValue());
        return true;
      }

    }
    return false;
  }

  public <T extends WordNumber> void addMc14Or19(T address) {
    if (cpu.getState().getTStatesSinceCpuStart() == 14) {
      addMc(2, cpu, 1, 3, cpu.getState().getRegister(PC).read().intValue());
    } else if (cpu.getState().getTStatesSinceCpuStart() == 19) {
      addMc(1, cpu, 1, 0, address.intValue());
    }
  }

  @Override
  public void readingMemoryAt(T address, T value, int fetching) {
    if (address.intValue() == -1) {
      addMcWith(afterFetchAdder, null,
          (result) -> addMc(result, cpu, 1, 0, cpu.getState().getRegister(IR).read().intValue()), instructionFetcher);
    } else if (address.intValue() == -2) {
      addMcWith(afterExecutionAdder, new ArrayList<>(),
          (result) -> result.forEach(add -> addMc(add.time(), cpu, 1, add.delta(), cpu.getState().getRegister(add.registerName()).read().intValue())), instructionFetcher);

    } else {
      boolean fetching1 = fetching != 0;
      boolean pendingEvent = lastEvents != null;

      Runnable lastEvents1 = () -> {
        int time1;
        if (fetching1)
          time1 = 4 - (fetching == 2 ? 1 : 0);
        else
          time1 = 3;


        addMc(1, cpu, time1, 0, address.intValue());
        cpu.getState().addEvent(new Event(0, "MR", address.intValue(), value.intValue()));

        addAfterMR(address, cpu, state, instructionFetcher, time1);
      };

      if (fetching != 2) {
        lastEvents1.run();
      }

      if (pendingEvent) {
        lastEvents.run();
        lastEvents = null;
      }

      if (fetching == 2)
        lastEvents = lastEvents1;
    }
  }

  private void addAfterMR(T address, Z80Cpu<T> cpu, State<T> state, DefaultInstructionFetcher instructionFetcher, int time1) {
    InstructionVisitor<T, ?> instructionVisitor = new AfterMRAdder(this, cpu, address, state);
    var instruction = instructionFetcher.instruction2;
    if (instruction != null)
      instruction.accept(instructionVisitor);
  }

}
