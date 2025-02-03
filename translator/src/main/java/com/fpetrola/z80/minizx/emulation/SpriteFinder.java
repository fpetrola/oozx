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

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.DirectAccessWordNumber;
import com.fpetrola.z80.spy.ExecutionListener;

import java.util.*;

public class SpriteFinder<T extends WordNumber> {
  public static final int SCREEN_START_ADDRESS = 16384;
  public static final int ATTRIBUTES_START_ADDRESS = 22528;
  public static final int ATTRIBUTES_END_ADDRESS = 23296;
  private final OOZ80<T> ooz80;
  private Set<Integer> spriteAddresses = new HashSet<>();
  private Set<Integer> attributesAddresses = new HashSet<>();
  private Set<Integer> borderAddresses = new HashSet<>();
  private Set<Integer> soundAddresses = new HashSet<>();
  private int last254 = 0;
  private List<Integer> outPcs = new ArrayList<>();

  public SpriteFinder(OOZ80<T> ooz80) {
    this.ooz80 = ooz80;
  }

  public void init() {
    Memory<T> memory = ooz80.getState().getMemory();
    T[] data = memory.getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = (T) new DirectAccessWordNumber(data[i].intValue(), -1, i);
    }

    memory.addMemoryWriteListener(((address, value) -> {
      int addressValue = address.intValue();
      if (addressValue >= SCREEN_START_ADDRESS && addressValue < ATTRIBUTES_START_ADDRESS) {
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          List<Integer> list = directAccessWordNumber.addresses.stream().filter(i -> i > ATTRIBUTES_END_ADDRESS).toList();
          spriteAddresses.addAll(list);
        }
      }
      if (addressValue >= ATTRIBUTES_START_ADDRESS && addressValue < ATTRIBUTES_END_ADDRESS) {
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          List<Integer> list = directAccessWordNumber.addresses.stream().filter(i -> i > ATTRIBUTES_END_ADDRESS).toList();
          attributesAddresses.addAll(list);
        }
      }
    }));

    IO<T> io = ooz80.getState().getIo();

    io.addOutListener((port, value) -> {
      int r = 0;
      if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
        if ((port.intValue() & 0xFF) == 0xFE) {
//            if ((value.intValue() & 0x10) != 0) {
//              List<Integer> list = directAccessWordNumber.addresses.stream().toList();
//              soundAddresses.addAll(list);
//            }
//            if ((value.intValue() & 0x07) != 0) {
//              List<Integer> list = directAccessWordNumber.addresses.stream().toList();
//              borderAddresses.addAll(list);
//            }

          List<Integer> list = directAccessWordNumber.addresses.stream().toList();
          soundAddresses.addAll(list);
          last254 = value.intValue();
        }
      }
    });

//    memory.addMemoryReadListener((address, value, delta, fetching) -> {
//      if (address.intValue() >= 43776 && address.intValue() < 49152) {
////        System.out.println("dgffffff");
//      }
//    });

    this.ooz80.getInstructionExecutor().addExecutionListener(new ExecutionListener<T>() {
      public void afterExecution(Instruction<T> instruction) {

      }
    });
  }

}
