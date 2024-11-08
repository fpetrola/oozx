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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.TraceableWordNumber;
import com.fpetrola.z80.opcodes.references.TraceableWordNumber.ReadOperation;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ComplexInstructionSpy;
import z80core.MemIoOps;

import java.util.ArrayList;
import java.util.List;

public class MemoryImplementation<T extends WordNumber> implements Memory<T> {
  private MemIoOps memory;
  private final ComplexInstructionSpy spy;
  int[] data = new int[0x10000];

  WordNumber[] traces = new WordNumber[0x10000];

  private List<MemoryReadListener> memoryReadListeners = new ArrayList<>();
  private List<MemoryWriteListener> memoryWriteListeners = new ArrayList<>();

  public MemoryImplementation(MemIoOps memory2, ComplexInstructionSpy spy) {
    this.memory = memory2;
    this.spy = spy;

//    JFrame frame = new JFrame("ZX Spectrum Bouncing Ball Example");
//    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    frame.setContentPane(new ZXScreen(data));
//    frame.pack();
//    frame.setVisible(true);
  }

  public void update() {
    for (int i = 0; i < 0xFFFF; i++) {
      int j = memory.peek82(i) & 0xFF;
      data[i] = j;
    }
  }

  public T read(T address) {
//    if (address.intValue() == 24686) System.out.println("dgadg");

    int i = address.intValue() & 0xFFFF;
    WordNumber value = WordNumber.createValue(data[i] & 0xff);

    WordNumber trace = traces[i];
    if (trace != null) {
      value= trace.readOperation(address, WordNumber.createValue(data[i] & 0xff));
    } else {
      value = value.readOperation(address, value);
    }

    if (!spy.wasFetched(i)) {
      WordNumber finalValue = value;
      new ArrayList<>(memoryReadListeners).forEach(l -> l.readingMemoryAt(address, finalValue));
    }
    return (T) value;
  }

  @Override
  public void write(T address, T value) {
//    if (address.intValue() < 14000)
//      System.out.println("adds11111");

    byte b = (byte) (value.intValue() & 0xFF);
    if (memoryWriteListeners != null) {
      memoryWriteListeners.forEach(l-> l.writtingMemoryAt(address, value));
    }

    int a = address.intValue() & 0xffff;
    data[a] = b;
    memory.poke8(a, value.intValue());
    checkTrace(value, a);
  }

  private void checkTrace(T value, int a) {
    if (value instanceof TraceableWordNumber) {
      TraceableWordNumber traceableWordNumber = ((TraceableWordNumber) value);

      WordNumber trace = traces[a];
      traces[a] = value;

      if (value instanceof TraceableWordNumber wordNumber)
        wordNumber.purgeTooOlderPrevious();

      if (spy.getBitsWritten() != null)
        if (a >= 0x4000 && a < 0x5800) {
          List<ReadOperation> readOperations = traceableWordNumber.getFirstReadOperation();

          for (ReadOperation readOperation : readOperations) {
            int r = readOperation.address.intValue();
//        if (r >= 0x8000 && r <= 0xC000) {
            for (int k = 0; k < 8; k++) {
//            if (r < 49000 & r > 43000)
//              System.out.println("AAAAA");
              spy.getBitsWritten()[r * 8 + k] = true;
            }
            int a1 = 1;
//        }
          }
        }
    }
  }

  public boolean compare() {
    for (int i = 0; i < 0xFFFF; i++) {
      int j = memory.peek82(i) & 0xFF;
      if (data[i] != j) return false;
    }
    return true;
  }

  @Override
  public void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    this.memoryWriteListeners.add(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    this.memoryWriteListeners.remove(memoryWriteListener);
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {
    this.memoryReadListeners.add(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {
    this.memoryReadListeners.remove(memoryReadListener);
  }

  @Override
  public void reset() {
    traces = new WordNumber[0x10000];
  }
}