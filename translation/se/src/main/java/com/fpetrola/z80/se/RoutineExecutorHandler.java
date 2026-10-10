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

package com.fpetrola.z80.se;

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.actions.ExecutionStackStorage;
import com.fpetrola.z80.transformations.StackAnalyzer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map;
import java.util.Stack;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class RoutineExecutorHandler {
  private final Register pc;
  private Stack<java.lang.Integer> stackFrames = new Stack<>();
  private Map<java.lang.Integer, RoutineExecution> routineExecutions = new HashMap<>();
  private final Map<java.lang.Integer, LinkedList<java.lang.Integer>> unexploredJumpTargets = new HashMap<>();

  private final State state;
  private final RoutineManager routineManager;
  private int exploration;

  private ExecutionStackStorage executionStackStorage;

  private final DataflowService dataflowService;

  public StackAnalyzer getStackAnalyzer() {
    return stackAnalyzer;
  }

  private final StackAnalyzer stackAnalyzer;

  public RoutineExecutorHandler(State state, RoutineManager routineManager, ExecutionStackStorage executionStackStorage, DataflowService dataflowService, StackAnalyzer stackAnalyzer) {
    this.routineManager = routineManager;
    this.pc = state.getPc();
    this.state = state;
    this.executionStackStorage = executionStackStorage;
    this.dataflowService = dataflowService;
    this.stackAnalyzer = stackAnalyzer;
  }

  public State getState() {
    return state;
  }

  public RoutineManager getRoutineManager() {
    return routineManager;
  }


  public DataflowService getDataflowService() {
    return dataflowService;
  }

  public RoutineExecution findRoutineExecutionAt(int address) {
    return routineExecutions.get(address);
  }

  public RoutineExecution findCallerOf(int callSite) {
    int callee = routineManager.getInstructionAt(callSite) instanceof Call call ? stackFrames.lastIndexOf(call.getJumpAddress()) : -1;
    return callee > 0 ? routineExecutions.get(stackFrames.get(callee - 1)) : stackFrames.reversed().stream().map(routineExecutions::get).filter(r -> r.contains(callSite)).findFirst().orElse(null);
  }

  public LinkedList<java.lang.Integer> unexploredJumpTargets(int address, Set<java.lang.Integer> targets) {
    return unexploredJumpTargets.computeIfAbsent(address, a -> new LinkedList<>(targets));
  }

  public RoutineExecution createRoutineExecution(int jumpAddress) {
    RoutineExecution currentRoutineExecution = getCurrentRoutineExecution();

    System.out.println("Push frame: " + formatAddress(jumpAddress));

    if (stackFrames.isEmpty())
      routineExecutions.values().forEach(RoutineExecution::invalidate);
    stackFrames.push(jumpAddress);
    RoutineExecution routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution(this, jumpAddress));
    } else
      routineExecution.invalidate();

    if (currentRoutineExecution != null)
      currentRoutineExecution.addCallee(routineExecution);

    return routineExecution;
  }

  public Stack<Integer> getStackFrames() {
    return stackFrames;
  }

  public int popRoutineExecution() {
    int t = state.getMemory().read16Bits(state.getRegisterSP().read());
    java.lang.Integer pop = stackFrames.pop();
    System.out.printf("Pop frame: %s, ret: %s%n", formatAddress(pop), formatAddress(t));
    if (stackFrames.isEmpty())
      clearStackFrames();
    else
      routineExecutions.get(pop).invalidate();
    return pop;
  }

  public void newExploration() {
    exploration++;
  }

  public int exploration() {
    return exploration;
  }

  public void forgetExecutions(int from, int to) {
    routineExecutions.keySet().removeIf(start -> start >= from && start < to);
    routineExecutions.values().forEach(RoutineExecution::invalidate);
  }

  public void clearStackFrames() {
    stackFrames.clear();
    routineExecutions.values().forEach(RoutineExecution::invalidate);
  }

  public void reset() {
    clearStackFrames();
    routineExecutions.clear();
    unexploredJumpTargets.clear();
  }

  public boolean isEmpty() {
    boolean empty = stackFrames.isEmpty();
    if (empty)
      System.out.println("empty");
    return empty;
  }

  public RoutineExecution getCurrentRoutineExecution() {
    if (stackFrames.isEmpty())
      return null;
    else
      return routineExecutions.get(stackFrames.peek());
  }

  public RoutineExecution getCallerRoutineExecution() {
    return routineExecutions.get(stackFrames.get(stackFrames.size() - 2));
  }

  public HashMap<java.lang.Integer, RoutineExecution> getCopyListOfRoutineExecutions() {
    return new HashMap<>(routineExecutions);
  }

  public Register getPc() {
    return pc;
  }

  public ExecutionStackStorage getExecutionStackStorage() {
    return executionStackStorage;
  }

  public void pushRoutineExecution(RoutineExecution routineExecution) {
    stackFrames.push(routineExecution.getStart());
  }
}
