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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.actions.ExecutionStackStorage;
import com.fpetrola.z80.transformations.StackAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class RoutineExecutorHandler<T extends WordNumber> {
  private final Register<T> pc;
  private Stack<Integer> stackFrames = new Stack<>();
  private Map<Integer, RoutineExecution<T>> routineExecutions = new HashMap<>();

  private final State<T> state;

  private ExecutionStackStorage<T> executionStackStorage;

  private final DataflowService<T> dataflowService;

  public StackAnalyzer getStackAnalyzer() {
    return stackAnalyzer;
  }

  private final StackAnalyzer stackAnalyzer;

  public RoutineExecutorHandler(State<T> state, ExecutionStackStorage executionStackStorage, DataflowService dataflowService, StackAnalyzer stackAnalyzer) {
    this.pc = state.getPc();
    this.state = state;
    this.executionStackStorage = executionStackStorage;
    this.dataflowService = dataflowService;
    this.stackAnalyzer = stackAnalyzer;
  }

  public State<T> getState() {
    return state;
  }

  public DataflowService<T> getDataflowService() {
    return dataflowService;
  }

  public RoutineExecution<T> findRoutineExecutionAt(int address) {
    return routineExecutions.get(address);
  }

  public RoutineExecution<T> findRoutineExecutionContaining(int address) {
    return routineExecutions.values().stream().filter(r -> r.contains(address)).findFirst().get();
  }

  public RoutineExecution<T> createRoutineExecution(int jumpAddress) {
    RoutineExecution<T> currentRoutineExecution = getCurrentRoutineExecution();

    System.out.println("Push frame: " + formatAddress(jumpAddress));

    stackFrames.push(jumpAddress);
    RoutineExecution<T> routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution<>(this, jumpAddress));
    } else
      System.err.print("");

    if (currentRoutineExecution != null)
      currentRoutineExecution.addCallee(routineExecution);

    return routineExecution;
  }

  public Stack<Integer> getStackFrames() {
    return stackFrames;
  }

  public int popRoutineExecution() {
    T t = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
    Integer pop = stackFrames.pop();
    System.out.printf("Pop frame: %s, ret: %s%n", formatAddress(pop), formatAddress(t.intValue()));
    return pop;
  }

  public void reset() {
    stackFrames.clear();
    routineExecutions.clear();
  }

  public boolean isEmpty() {
    boolean empty = stackFrames.isEmpty();
    if (empty)
      System.out.println("empty");
    return empty;
  }

  public RoutineExecution<T> getCurrentRoutineExecution() {
    if (stackFrames.isEmpty())
      return null;
    else
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

  public ExecutionStackStorage getExecutionStackStorage() {
    return executionStackStorage;
  }

  public void pushRoutineExecution(RoutineExecution<T> routineExecution) {
    stackFrames.push(routineExecution.getStart());
  }
}
