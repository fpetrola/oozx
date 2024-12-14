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

package com.fpetrola.z80.se;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class RoutineExecutorHandler<T extends WordNumber> {
  private final Register<T> pc;
  private Stack<Object> stackFrames = new Stack<>();
  private Map<Integer, RoutineExecution<T>> routineExecutions = new HashMap<>();

  public RoutineExecutorHandler(Register<T> pc) {
    this.pc = pc;
  }

  public void createRoutineExecution(int jumpAddress) {
    // if (jumpAddress == 35211) System.out.println("start routine: " + jumpAddress);
    if (jumpAddress == 0xCFD9)
      System.out.println("");
    stackFrames.push(jumpAddress);
    RoutineExecution<T> routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution<>(this, jumpAddress));
    } else
      System.err.print("");
  }

  public Object popRoutineExecution() {
    return stackFrames.pop();
  }

  public void reset() {
    stackFrames.clear();
    routineExecutions.clear();
  }

  public boolean isEmpty() {
    return stackFrames.isEmpty();
  }

  public RoutineExecution<T> getCurrentRoutineExecution() {
    return routineExecutions.get(stackFrames.peek());
  }

  public RoutineExecution<T> getCallerRoutineExecution() {
    return routineExecutions.get(stackFrames.get(stackFrames.size() - 2));
  }

  public HashMap<Integer, RoutineExecution> getCopyListOfRoutineExecutions() {
    return new HashMap<>(routineExecutions);
  }

  public Register<T> getPc() {
    return pc;
  }
}
