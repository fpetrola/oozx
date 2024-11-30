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

import com.fpetrola.z80.instructions.types.Instruction;

public class AddressAction {
  private Instruction instruction;
  protected boolean alwaysTrue;
  private int count;
  private RoutineExecution routineExecution;
  protected final SymbolicExecutionAdapter symbolicExecutionAdapter;
  private boolean state;

  public AddressAction(int pcValue, RoutineExecution routineExecution, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    this(symbolicExecutionAdapter, pcValue);

    this.routineExecution = routineExecution;
  }

  @Override
  public String toString() {
    return "AddressAction{" +
        "address=" + address +
        ", pending=" + pending +
        '}';
  }

  public int address;
  protected boolean pending;

  public AddressAction(int pcValue, boolean b, RoutineExecution routineExecution, SymbolicExecutionAdapter symbolicExecutionAdapter, Instruction instruction, boolean alwaysTrue) {
    this(symbolicExecutionAdapter, pcValue, b);

    this.routineExecution = routineExecution;
    this.instruction = instruction;
    this.alwaysTrue = alwaysTrue;
  }

  public AddressAction(SymbolicExecutionAdapter symbolicExecutionAdapter, int address) {
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.address = address;
  }

  public AddressAction(SymbolicExecutionAdapter symbolicExecutionAdapter, int address, boolean pending) {
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.address = address;
    this.pending = pending;
  }

  public boolean processBranch(boolean doBranch, Instruction instruction) {
    if (pending)
      pending = false;
    return true;
  }

  protected boolean getDoBranch() {
    boolean result = state;
    state = !state;
    result = alwaysTrue || result;
    return result;
  }

  public int getNext(int next, int pcValue) {
    return pcValue;
  }

  public boolean isPending() {
    return pending;
  }

  public void setPending(boolean pending) {
    this.pending = pending;
  }

  protected int genericGetNext(int next, int pcValue) {
    int result = pcValue;
    if (pending) {
      pending = false;
    }
    if (routineExecution.retInstruction == next && routineExecution.hasPendingPoints())
      result = routineExecution.getNextPending().address;
    return result;
  }

  public int getNextPC() {
    return address;
  }

  void setPendingAfterStep(SymbolicExecutionAdapter symbolicExecutionAdapter) {
    setPending(false);
  }

  protected void updatePending() {
    count++;
    pending = count == 1;
  }
}
