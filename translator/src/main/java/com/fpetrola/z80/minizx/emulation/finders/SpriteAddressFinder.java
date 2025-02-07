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

package com.fpetrola.z80.minizx.emulation.finders;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.DirectAccessWordNumber;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class SpriteAddressFinder<T extends WordNumber> {
  public static final int SCREEN_START_ADDRESS = 16384;
  public static final int ATTRIBUTES_START_ADDRESS = 22528;
  public static final int ATTRIBUTES_END_ADDRESS = 23296;
  private final OOZ80<T> ooz80;
  private final GameData gameData;
  private final Z80Rewinder z80Rewinder;

  public SpriteAddressFinder(OOZ80<T> ooz80, GameData gameData, Z80Rewinder z80Rewinder) {
    this.ooz80 = ooz80;
    this.gameData = gameData;
    this.z80Rewinder = z80Rewinder;
  }

  public void init() {
    Memory<T> memory = ooz80.getState().getMemory();
    T[] data = memory.getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = (T) new DirectAccessWordNumber(data[i].intValue(), -1, i);
    }

    memory.addMemoryWriteListener(((address, value) -> {
      int addressValue = address.intValue();

//      if (addressValue == 33026)
//        System.out.println("sssss!1111!!!");

//      if (addressValue >= 24576 && addressValue < 	28672)
      if (addressValue > ATTRIBUTES_END_ADDRESS)
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          T read = memory.read(address, 0);
          T value1 = (T) new DirectAccessWordNumber(value.intValue(), directAccessWordNumber.pc, addressValue);
          value = value1.process(value);
        }

      if (addressValue >= 28672 && addressValue < 28672 + 4096) {
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          List<Integer> list = directAccessWordNumber.getAddressesSupplier().stream().filter(i -> i > ATTRIBUTES_END_ADDRESS).toList();
          if (list.stream().anyMatch(a -> a > 43776 && a < 49152))
            System.out.println("dgadg");

          gameData.spriteAddresses.addAll(list);
        } else {
          int notFound = 1;
        }
      }

      if (addressValue >= SCREEN_START_ADDRESS && addressValue < ATTRIBUTES_START_ADDRESS) {
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          List<Integer> list = directAccessWordNumber.getAddressesSupplier().stream().filter(i -> i > ATTRIBUTES_END_ADDRESS).toList();
          if (!list.isEmpty())
            if (list.stream().anyMatch(a -> a > 43776 && a < 49152)) {

              Collection<Integer> integers = directAccessWordNumber.getAllSupplier();
//              LinkedHashSet<Integer> integers1 = directAccessWordNumber.getAddressesSupplier();
//              Collection<Integer> integers2 = CollectionUtils.removeAll(integers, integers1);
//              if (!directAccessWordNumber.getOriginSupplier().isEmpty()) {
//                int v = 1;
//              }
              int a = 1;
            }
          gameData.spriteAddresses.addAll(list);
        } else {
          int notFound = 1;
        }
      }
      if (addressValue >= ATTRIBUTES_START_ADDRESS && addressValue < ATTRIBUTES_END_ADDRESS) {
        if (value instanceof DirectAccessWordNumber directAccessWordNumber) {
          List<Integer> list = directAccessWordNumber.getAddressesSupplier().stream().filter(i -> i > ATTRIBUTES_END_ADDRESS).toList();
          if (!list.isEmpty()) {
//            Collection<Integer> integers = directAccessWordNumber.getAllSupplier();
//            int a= 1;
          }
          gameData.attributesAddresses.addAll(list);
        }
      }
      return value;
    }));
  }

}
