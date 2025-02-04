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
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class MemoryRangesFinder<T extends WordNumber> {
  private final OOZ80<T> ooz80;
  private final GameData gameData;
  private Register<T> pc;
  private final StructureFinder structureFinder;

  public MemoryRangesFinder(OOZ80<T> ooz80, StructureFinder structureFinder, GameData gameData) {
    this.ooz80 = ooz80;
    pc = ooz80.getState().getPc();
    this.structureFinder = structureFinder;
    this.gameData = gameData;
  }

  public void init() {
    this.ooz80.getInstructionExecutor().addExecutionListener(new ExecutionListener<T>() {
      public void afterExecution(Instruction<T> instruction) {
        int pcValue = pc.read().intValue();
        if (instruction instanceof TargetSourceInstruction<T, ?> targetSourceInstruction) {
          if (isMemoryAccess(targetSourceInstruction.getSource())) {
            gameData.memoryAccesses.put(pcValue, getAccessedAddress(targetSourceInstruction.getSource()));
            gameData.invertedMemoryAccesses.put(getAccessedAddress(targetSourceInstruction.getSource()), pcValue);
          }
          if (isMemoryAccess(targetSourceInstruction.getTarget())) {
            gameData.memoryAccesses.put(pcValue, getAccessedAddress(targetSourceInstruction.getTarget()));
            gameData.invertedMemoryAccesses.put(getAccessedAddress(targetSourceInstruction.getTarget()), pcValue);
          }
        }
      }

      private int getAccessedAddress(ImmutableOpcodeReference<T> source) {
        if (source instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
          Object read = indirectMemory8BitReference.read();
          return indirectMemory8BitReference.address.intValue();
        } else if (source instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
          Object read = indirectMemory16BitReference.read();
          return indirectMemory16BitReference.address.intValue();
        } else if (source instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
          Object read = memoryPlusRegister8BitReference.read();
          return memoryPlusRegister8BitReference.address.intValue();
        }
        return -1;
      }
    });
  }

  public boolean isMemoryAccess(ImmutableOpcodeReference<?> reference) {
    return reference instanceof MemoryPlusRegister8BitReference ||
        reference instanceof IndirectMemory16BitReference ||
        reference instanceof IndirectMemory8BitReference;
  }

  private void findPaths(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
  }


  public void persist() {
    SetMultimap<Integer, Integer> memoryAccesses = TreeMultimap.create(gameData.memoryAccesses);
    SetMultimap<Integer, Integer> invertedMemoryAccesses = TreeMultimap.create(gameData.invertedMemoryAccesses);
    Set<Integer> pcs = new HashSet<>(memoryAccesses.keySet());

    pcs.forEach(pc -> {
      if (!memoryAccesses.get(pc).isEmpty()) {
        LocalMemory localMemory = new LocalMemory();
        gameData.localMemoryList.add(localMemory);
        addOthers(pc, localMemory, memoryAccesses, invertedMemoryAccesses);
      }
    });


    for (int i = 0; i < gameData.localMemoryList.size(); i++) {
      for (int j = 0; j < gameData.localMemoryList.size(); j++) {
        if (i != j) {

          LocalMemory localMemory1 = gameData.localMemoryList.get(i);
          LocalMemory localMemory2 = gameData.localMemoryList.get(j);
          Collection<Integer> intersection1 = CollectionUtils.intersection(localMemory1.addresses, localMemory2.addresses);
          Set<Integer> referers = localMemory1.referers;
          Set<Integer> referers1 = localMemory2.referers;
          Set<Integer> origins1 = getOrigins(referers);
          Set<Integer> origins2 = getOrigins(referers1);
          if (!CollectionUtils.intersection(origins1, origins2).isEmpty()) {
            localMemory2.referers.forEach(localMemory1::addReferer);

//            ArrayList<Integer> list = new ArrayList<>(localMemory1.referers);
//            Collections.sort(list);
//            localMemory1.referers= new HashSet<>(list);

            localMemory2.addresses.forEach(localMemory1::addAddress);

//            ArrayList<Integer> list2 = new ArrayList<>(localMemory1.addresses);
//            Collections.sort(list2);
//            localMemory1.addresses= new HashSet<>(list2);
            localMemory2.addresses.clear();
            localMemory2.referers.clear();
            gameData.localMemoryList.remove(localMemory2);
          }
          Collection<Integer> intersection2 = CollectionUtils.intersection(referers, referers1);
          if (!intersection1.isEmpty() || !intersection2.isEmpty()) {
            System.out.println("dfadgadg");
          }
        }
      }
    }

//    saveFile("ranges.json", gameData.memoryAccesses);
//    saveFile("inverted-ranges.json", gameData.invertedMemoryAccesses);
  }

  private Set<Integer> getOrigins(Set<Integer> referers) {
    List<Integer> list = referers.stream().map(r -> (Integer) structureFinder.origins.get(r)).filter(Objects::nonNull).toList();
    return new HashSet<>(list);
  }

  private void addOthers(Integer pc, LocalMemory localMemory, SetMultimap<Integer, Integer> memoryAccesses, SetMultimap<Integer, Integer> invertedMemoryAccesses) {
    Collection<Integer> integers = memoryAccesses.get(pc);
    Collection<Integer> addresses = new ArrayList<>(integers);
    integers.clear();
    localMemory.addReferer(pc);

    addresses.forEach(address -> {
      localMemory.addAddress(address);

      Collection<Integer> pcs2 = invertedMemoryAccesses.get(address);
      pcs2.forEach(pc2 -> addOthers(pc2, localMemory, memoryAccesses, invertedMemoryAccesses));
    });
  }

  static public void saveToJson(String fileName, Gson gson, GameData gameData) {
    String json1 = gson.toJson(gameData);
    try {
      FileWriter fileWriter = new FileWriter(fileName);
      fileWriter.write(json1);
      fileWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static public GameData loadFromJson(String fileName, Gson gson) {
    try {
      FileReader fileReader = new FileReader(fileName);
      JsonReader reader = new JsonReader(fileReader);
      GameData result = gson.fromJson(reader, GameData.class);
      fileReader.close();
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}



