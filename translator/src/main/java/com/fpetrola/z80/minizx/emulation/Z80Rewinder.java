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
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.spy.ObservableRegister;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static com.fpetrola.z80.registers.RegisterName.IX;
import static com.fpetrola.z80.registers.RegisterName.IY;

public class Z80Rewinder<T extends WordNumber> {
  private final OOZ80<T> ooz80;
  public LinkedList<StateDelta<T>> deltas = new LinkedList<>();
  public LinkedList<StateDelta<T>> ixdeltas = new LinkedList<>();
  public LinkedList<StateDelta<T>> iydeltas = new LinkedList<>();

  StateDelta<T> currentDelta = null;

  private boolean listening = true;

  public Z80Rewinder(OOZ80<T> ooz80) {
    this.ooz80 = ooz80;
    createStateDelta(ooz80);

    ooz80.getInstructionExecutor().addExecutionListener(new ExecutionListener<T>() {
      public void beforeExecution(Instruction<T> instruction) {
        if (listening)
          currentDelta.setInstruction(instruction);
      }

      public void afterExecution(Instruction<T> instruction) {
        createStateDelta(ooz80);
      }
    });

    ooz80.getState().getMemory().addMemoryWriteListener((a, v) -> {
      if (listening)
        currentDelta.addMemoryChange(a, v);
    });

    List<Register<T>> all = ooz80.getState().getRegisterBank().getAll();
    all.forEach(r -> {
      ObservableRegister<T> observableRegister = (ObservableRegister<T>) r;
      observableRegister.addRegisterWriteListener((v, i) -> {
        if (listening)
          currentDelta.addRegisterChange(r, v, i);
      });
      observableRegister.setListening(true);
    });
  }

  private void createStateDelta(OOZ80<T> ooz80) {
    if (listening) {
      if (currentDelta != null) {
        if (currentDelta.getRegisterChanges().containsKey(IX.name()))
          ixdeltas.offer(currentDelta);
        if (currentDelta.getRegisterChanges().containsKey(IY.name()))
          iydeltas.offer(currentDelta);
      }
      currentDelta = new StateDelta<>(ooz80);
      if (deltas.size() > 100000)
        deltas.pollFirst();
      deltas.offer(currentDelta);
    }
  }

  public StateDelta<T> getCurrentDelta() {
    return currentDelta;
  }

  public void rewind(int steps) {
    dontListen();

    if (steps <= 0 || deltas.isEmpty()) {
      return;
    }

    steps = Math.min(steps, deltas.size());

    for (int i = 0; i < steps; i++) {
      StateDelta<T> delta = deltas.pollLast();
      if (delta != null)
        delta.applyReverse();
    }

    currentDelta = deltas.peekLast();
    listen();
  }

  private void listen() {
    listening = true;
//
//    memory.enableWriteListener();
//    all.forEach(r -> ((ObservableRegister) r).setListening(true));
  }

  private void dontListen() {
    listening = false;

//    List<Register<T>> all = ooz80.getState().getRegisterBank().getAll();
//    all.forEach(r -> ((ObservableRegister) r).setListening(false));

//    Memory<T> memory = ooz80.getState().getMemory();
//    memory.canDisable(true);
//    memory.disableWriteListener();
  }

  public void backPathUntil(Predicate<StateDelta<T>> untilCondition) {
    backPathUntil(untilCondition, deltas);
  }

  public void backPathUntil(Predicate<StateDelta<T>> untilCondition, LinkedList<StateDelta<T>> deltas1) {
    int currentIndex = deltas1.size() - 2;
    while (currentIndex > 1) {
      if (currentIndex < 10)
        System.out.println("afasgasg");
      StateDelta<T> t = deltas1.get(currentIndex);
      if (!untilCondition.test(t))
        break;
      currentIndex--;
    }
  }

  public StateDelta<T> getLastDelta() {
    return deltas.get(deltas.size() - 2);
  }
}
