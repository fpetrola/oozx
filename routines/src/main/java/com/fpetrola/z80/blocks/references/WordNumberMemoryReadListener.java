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

package com.fpetrola.z80.blocks.references;

import com.fpetrola.z80.blocks.spy.RoutineGrouperSpy;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.metadata.DataStructure;
import com.fpetrola.z80.opcodes.references.ExecutionPoint;
import com.fpetrola.z80.opcodes.references.TraceableWordNumber;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.*;

public class WordNumberMemoryReadListener<T extends WordNumber> implements MemoryReadListener<T> {
  private final ReferencesHandler referencesHandler;
  private final RoutineGrouperSpy spy;
  private final List<ExecutionPoint> processedLasts = new ArrayList<>();

  public WordNumberMemoryReadListener(ReferencesHandler referencesHandler, RoutineGrouperSpy spy) {
    this.referencesHandler = referencesHandler;
    this.spy = spy;
  }

  @Override
  public void readingMemoryAt(T address, T value) {
    if (referencesHandler.isDataBlock(address.intValue())) {
      if (referencesHandler.blocksManager.findBlockAt(spy.getLastExecutionPoint().pc) == referencesHandler.associatedBlock) {
        Collection<Map.Entry<Integer, BlockRelation>> entries1 = referencesHandler.relationsBySourceAddress.entries();

        Set<BlockRelation> blockRelations = referencesHandler.getBlockRelations();
        for (BlockRelation blockRelation : blockRelations) {
          if (blockRelation.getTargetAddress() == address.intValue()) {

            Collection<BlockRelation> blockRelations1 = referencesHandler.relationsBySourceAddress.get(blockRelation.getSourceAddress());
            if (blockRelations1.size() < 100) {
              if (address instanceof TraceableWordNumber) {
                TraceableWordNumber traceableWordNumber = (TraceableWordNumber) address;
                TreeSet<ExecutionPoint> operationsAddresses = traceableWordNumber.getOperationsAddresses();

                ExecutionPoint first = findFirst(operationsAddresses);

                DataStructure dataStructure = referencesHandler.dataStructures.get(first.executionNumber);
                if (dataStructure == null)
                  referencesHandler.dataStructures.put(first.executionNumber, dataStructure = new DataStructure());

                Iterator<ExecutionPoint> executionPointIterator = operationsAddresses.descendingIterator();
                ExecutionPoint last = executionPointIterator.next();


                boolean invalid = false;// processedLasts.stream().anyMatch(l -> operationsAddresses.contains(l));

                if (!invalid) {
//                    System.out.println("---------------------------------------");
//                    operationsAddresses.forEach(e-> System.out.println(e));
//                    System.out.println("---------------------------------------");
                  LinkedList<ExecutionPoint> spyExecutionPoints = spy.getExecutionPoints();

                  if (!executionPointIterator.hasNext()) {
                    dataStructure.getInstance(0).addAddress(address.intValue());
                  } else
                    addAddressToInstance(address, executionPointIterator, spyExecutionPoints, first, dataStructure);

                  processedLasts.add(last);
                } else
                  System.out.println("hola!: ");
              }
            }
            return;
          }
        }
      }
    }
  }

  private ExecutionPoint findFirst(TreeSet<ExecutionPoint> operationsAddresses) {
    ExecutionPoint first = operationsAddresses.iterator().next();

    Iterator<ExecutionPoint> executionPointIterator1 = operationsAddresses.descendingIterator();
    while (executionPointIterator1.hasNext()) {
      ExecutionPoint temp = executionPointIterator1.next();
      if (processedLasts.contains(temp))
        first = temp;
    }
    return first;
  }

  private <T extends WordNumber> void addAddressToInstance(T address, Iterator<ExecutionPoint> executionPointIterator, LinkedList<ExecutionPoint> spyExecutionPoints, ExecutionPoint first, DataStructure dataStructure) {
    ExecutionPoint last2 = executionPointIterator.next();

    int firstIndex = spyExecutionPoints.indexOf(first);
    int lastIndex = spyExecutionPoints.indexOf(last2);

    if (firstIndex != -1 && lastIndex != -1) {
      int instanceNumber = 0;

      if (firstIndex != lastIndex) {
        instanceNumber++;
        for (int i = firstIndex; i < lastIndex; i++) {
          if (spyExecutionPoints.get(i).pc == last2.pc) {
            instanceNumber++;
          }
        }
      }

      dataStructure.getInstance(instanceNumber).addAddress(address.intValue());
    } else {
      System.out.println("hola!: ");
    }
  }
}
