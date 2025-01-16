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

package com.fpetrola.z80.se.actions;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.Arrays;

public class ExecutionStackStorage<T extends WordNumber> {
  private T[] savedStack;
  private final State<T> state;
  private int savedSP;
  private boolean enabled = true;
  private int lastSP = 123;

  public ExecutionStackStorage(State<T> state) {
    this.state = state;
  }

  void save() {
    if (savedStack == null && enabled) {
      savedStack = createStackCopy();
      printStack(savedSP, savedStack, "saving ");
    }
//    else
//      throw new RuntimeException("already stored");
  }

  private void printStack(int savedSP1, T[] savedStack1, String prefix) {
//    System.out.printf(prefix + "stack: SP: %04X -> %s%n", savedSP1, printStack(savedStack1));
  }

  public void printStack() {
    printStack(state.getRegisterSP().read().intValue(), createStackCopy(), "PC: %s ".formatted(Helper.formatAddress(state.getPc().read().intValue())));
  }

  public void restore() {
    Memory<T> memory = state.getMemory();
    if (savedStack != null && enabled) {
//      WordNumber[] currentStack = createStackCopy();

//      if (Arrays.compare(savedStack, currentStack) != 0) {
//        System.out.println("dsgsddshB");
//      }

      for (int i = 0; i < savedStack.length; i++) {
        if ((savedSP + i) <= 65535)
          memory.getData()[savedSP + i] = (T) savedStack[i];
      }

      printStack(savedSP, savedStack, "restoring ");

//      T[] savedStack3 = Arrays.copyOfRange(memory.getData(), savedSP, savedSP + 40);

      state.getRegisterSP().write(WordNumber.createValue(savedSP));
    }
  }

  public T[] createStackCopy() {
    Memory<T> memory = state.getMemory();
    savedSP = state.getRegisterSP().read().intValue();
    int i = savedSP + 40;
    return Arrays.copyOfRange(memory.getData(), savedSP, Math.min(i, 65536));
  }

  private String printStack(T[] savedStack1) {
    StringBuilder result = new StringBuilder();
    result.append("[ ");
    for (int i = 0; i < savedStack1.length; i += 2) {
      if (i + 1 < savedStack1.length) {
        int i1 = (savedStack1[i + 1].intValue() * 256) + savedStack1[i].intValue();
        result.append("%04X".formatted(i1));
        result.append(", ");
      }
    }
    result.append(" ]");

//    if (!result.toString().contains("C807"))
//      System.out.println("que?");
    return result.toString();
  }

  public ExecutionStackStorage<T> create() {
    return new ExecutionStackStorage<>(state);
  }

  public void disable() {
    enabled = false;
  }

  public void changingSP(int value) {
    disable();
    lastSP = state.getRegisterSP().read().intValue();
  }

  public void enable() {
    enabled = true;
  }

  public void restoringSP(int i) {
    enable();
    lastSP = 123;
  }
}
