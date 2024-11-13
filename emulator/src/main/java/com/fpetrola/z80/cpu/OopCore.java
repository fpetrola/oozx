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
package com.fpetrola.z80.cpu;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;
import com.fpetrola.z80.tstates.PhaseProcessor;

/** The OOP core: the model's instructions, fetched and executed one at a time, with MEMPTR and the contention as listeners on the executor. */
public class OopCore implements Core {
  public String name() {
    return "OOP";
  }

  public RegisterBank bank(Memory memory, IO io) {
    return new DefaultRegisterBankFactory().createBank();
  }

  public OOZ80 cpu(State state, PhaseProcessor contention) {
    OOZ80 cpu = new OOZ80(state, new DefaultInstructionFetcher(state, false, false), new DefaultInstructionExecutor(state, false));
    new MemptrUpdateInstructionSpy(state).addExecutionListeners(cpu.getInstructionExecutor());
    cpu.getInstructionExecutor().setExecutionListener(contention);
    return cpu;
  }

  public boolean countsItsOwnContention() {
    return false;
  }
}
