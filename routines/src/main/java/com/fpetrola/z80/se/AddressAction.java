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
  private int count;
  @Override
  public String toString() {
    return "AddressAction{" +
        "address=" + address +
        ", pending=" + pending +
        '}';
  }

  public int address;
  protected boolean pending;

  public AddressAction() {
  }

  public AddressAction(int address) {
    this.address = address;
  }

  public AddressAction(int address, boolean pending) {
    this.address = address;
    this.pending = pending;
  }

  public boolean processBranch(boolean doBranch, Instruction instruction, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    if (pending)
      pending = false;
    return true;
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
