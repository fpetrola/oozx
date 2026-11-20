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

public interface Z80Cpu {
  void reset();

  void execute();

  void interruption();


  InstructionFetcher getInstructionFetcher();

  State getState();

  InstructionExecutor getInstructionExecutor();

  /**
   * Runs instructions, each through the given one, until the processor has made at least that many fetches, counted by
   * R. LD R,A writes R instead of bumping it, so it is its own two fetches; it is only looked for when R came out equal
   * to A, which it always leaves. Answers instructions << 32 | fetches.
   */
  default long runFetches(int budget, Runnable instruction) {
    State state = getState();
    var r = state.getRegisterR();
    var a = state.getRegister(com.fpetrola.z80.registers.RegisterName.A);
    var memory = state.getMemory();
    int done = 0, previous = r.read() & 0x7F;
    long executed = 0;
    while (done < budget) {
      int pc = state.getPc().read();
      instruction.run();
      executed++;
      int now = r.read() & 0x7F;
      done += now == (a.read() & 0x7F) && memory.peek(pc) == 0xED && memory.peek(pc + 1 & 0xFFFF) == 0x4F ? 2 : (now - previous) & 0x7F;
      previous = now;
    }
    return executed << 32 | done;
  }
}
