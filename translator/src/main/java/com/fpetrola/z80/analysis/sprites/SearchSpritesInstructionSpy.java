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

package com.fpetrola.z80.analysis.sprites;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.z80.graph.CustomGraph;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class SearchSpritesInstructionSpy<T extends WordNumber> extends AbstractInstructionSpy<T> implements ComplexInstructionSpy<T> {
  private static final String FILE_TRACE_JSON = "game-trace.json";

  private final class ExecutionStepAddressRange<T extends WordNumber> extends ExecutionStep<T> {
    private final AddressRange addressRange;

    private ExecutionStepAddressRange(AddressRange addressRange) {
      super(memory);
      this.addressRange = addressRange;
      description = "sprite: " + addressRange.getName();
    }

    public int hashCode() {
      return System.identityHashCode(addressRange);
    }
  }

  private Set<Integer> spritesAt = new HashSet<>();
  private State state;
  private CustomGraph customGraph;

  public SearchSpritesInstructionSpy() {
    super();
  }

  AddressRange currentRange = new AddressRange();
  protected List<AddressRange> ranges = new ArrayList<AddressRange>();
  public static final int STEP_PROCESSOR_CANCEL = -2;
  public static final int STEP_PROCESSOR_NOT_MATCHING = -1;

  protected int walkAccessReverse(ExecutionStep step, AccessProcessor accessProcessor) {
    for (int j = step.accessReferences.size() - 1; j >= 0; j--) {
      if (accessProcessor.accessMatching(step.accessReferences.get(j)))
        return j;
    }
    return STEP_PROCESSOR_NOT_MATCHING;
  }

  protected ExecutionStep walkReverse(Function<ExecutionStep, Integer> stepProcessor, ExecutionStep from) {
    for (int i = from.i - 1; i >= 0; i--) {
      ExecutionStep step = executionSteps.get(i);
      Integer apply = stepProcessor.apply(step);
      if (apply == STEP_PROCESSOR_CANCEL)
        return nullStep;
      else if (apply != STEP_PROCESSOR_NOT_MATCHING)
        return step;
    }
    return nullStep;
  }

  public void reset(State state) {
    super.reset(state);
    spritesAt.clear();
    initGraph();
  }

  private void initGraph() {
    customGraph = new CustomGraph() {
      protected String getVertexLabel(Object object) {
        if (object instanceof ExecutionStep) {
          ExecutionStep currentStep = (ExecutionStep) object;
          return Helper.formatAddress(currentStep.pcValue) + ": " + currentStep.description;
        } else
          return object + "";
      }

      protected String getVertexId(Object object) {
        if (ExecutionStepAddressRange.class.isAssignableFrom(object.getClass()))
          return object.hashCode() + "";
        else
          return getVertexLabel(object);
      }
    };
  }

  public void process() {
    spritesAt.clear();

    execute(false);

  }

  public static void main(String[] args) {
    SearchSpritesInstructionSpy searchSpritesInstructionSpy = new SearchSpritesInstructionSpy();
    searchSpritesInstructionSpy.execute(true);
  }

  private void execute(boolean replay) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      if (!replay)
        exportData(objectMapper);
      else
        importData(objectMapper);
    } catch (Exception e) {
      e.printStackTrace();
    }

    ExecutionStep last = executionSteps.get(executionSteps.size() - 1);
    findFirst(last);

    updateSpriteBrowser();

    exportGraph();
  }

  private void exportGraph() {
    while (mergeRanges() > 0)
      ;

    customGraph.exportGraph();
  }

  private int mergeRanges() {
    for (int i = 0; i < ranges.size(); i++) {
      AddressRange addressRange = ranges.get(i);
      for (int j = 0; j < ranges.size(); j++) {
        AddressRange b = ranges.get(j);
        if (b != addressRange) {
          boolean merged = addressRange.mergeIfRequired(b);
          ExecutionStep targetVertex = new ExecutionStepAddressRange(addressRange);
          ExecutionStep sourceVertex = new ExecutionStepAddressRange(b);

          if (merged) {
            customGraph.mergeVertexWith(targetVertex, sourceVertex);
            ranges.remove(b);
            return 1;
          }
        }
      }
    }

    return 0;
  }

  private void updateSpriteBrowser() {
    for (Integer address : spritesAt) {
      System.out.println("sprite at: " + address);
      for (int k = 0; k < 8; k++)
        bitsWritten[address * 8 + k] = true;
    }
  }

  private void importData(ObjectMapper objectMapper) throws IOException, StreamReadException, DatabindException {
    ResultContainer resultContainer2 = objectMapper.readValue(new File(FILE_TRACE_JSON), ResultContainer.class);

    executionSteps = resultContainer2.executionSteps;
    memorySpy = resultContainer2.memorySpy;

    for (ExecutionStep step : executionSteps) {
      step.accessReferences = new ArrayList<>();
      step.writeMemoryReferences = filterIndirect(step.writeMemoryReferences);
      step.writeReferences = filterIndirect(step.writeReferences);
      step.readMemoryReferences = filterIndirect(step.readMemoryReferences);
      step.readReferences = filterIndirect(step.readReferences);

      step.accessReferences.addAll(step.writeMemoryReferences);
      step.accessReferences.addAll(step.writeReferences);
      step.accessReferences.addAll(step.readMemoryReferences);
      step.accessReferences.addAll(step.readReferences);
      addMemoryChanges(step);
    }
    initGraph();
    bitsWritten = new boolean[0x10000 * 8];
  }

  private <T extends SpyReference> List<T> filterIndirect(List<T> writeMemoryReferences) {
//    List<T> value = writeMemoryReferences.stream().filter(r -> !r.isIndirectReference()).collect(Collectors.toList());
    return writeMemoryReferences;
  }

  private void exportData(ObjectMapper objectMapper) throws IOException, StreamWriteException, DatabindException {
    ResultContainer resultContainer = new ResultContainer();
    resultContainer.executionSteps = executionSteps;
    resultContainer.memorySpy = memorySpy;
    objectMapper.writeValue(new File(FILE_TRACE_JSON), resultContainer);
  }

  private SpyReference getSource(ExecutionStep<T> executionStep) {
    if (!executionStep.readMemoryReferences.isEmpty()) {
      if (executionStep.readMemoryReferences.size() > 1)
        System.out.println("dsgsdagds");
      return executionStep.readMemoryReferences.get(0);
    } else {
      if (executionStep.readReferences.isEmpty()) {
        return null;
      } else
        return executionStep.readReferences.get(0);
    }
  }

  private ExecutionStep findFirst(ExecutionStep last) {
    ExecutionStep result = last;

    while (last != null) {
      ExecutionStep screenWritingStep = walkReverse(step -> walkAccessReverse(step, this::isScreenWriting), last);
      if (screenWritingStep == nullStep)
        break;

      ExecutionStep screenStep = addScreenEdge(screenWritingStep);

      List<ExecutionStep> originalSteps = findOriginalSourceOf(screenWritingStep, getSource(screenWritingStep), screenStep);

      for (ExecutionStep<T> originalStep : originalSteps) {
        for (WriteMemoryReference writeMemoryReference : originalStep.writeMemoryReferences) {
          addRangeEdge(originalStep, "s0", writeMemoryReference.address.intValue());
        }
        for (ReadMemoryReference<T> readMemoryReference : originalStep.readMemoryReferences) {
          int address = readMemoryReference.address.intValue();
          boolean found = address >= 0xB900 && address <= 0xB97F;

          if (found)
            System.out.println("sdgdsg");
          addRangeEdge(originalStep, "s1", readMemoryReference.address.intValue());
        }
      }

      last = getPreviousStep(screenWritingStep);
//      last = null;
    }

    return result;
  }

  private ExecutionStep addScreenEdge(ExecutionStep screenWritingStep) {
    ExecutionStep screenStep = new ExecutionStep(memory);
    screenStep.description = "screen";
    screenStep.i = screenWritingStep.i;
    customGraph.addEdge(screenWritingStep, screenStep, "write");
    return screenStep;
  }

  protected AddressRange getAddressRangeFor(int address, ExecutionStep step) {
    Optional<AddressRange> first = ranges.stream().filter(r -> r.canAdd(address, step)).findFirst();
    first.ifPresentOrElse(r -> {
      currentRange = r;
      r.add(address, step);
    }, () -> ranges.add(currentRange = new AddressRange(address, step)));

    return currentRange;
  }

  protected void addRangeEdge(ExecutionStep originalStep, String label, int address) {
    AddressRange addressRange = getAddressRangeFor(address, originalStep);
    ExecutionStep targetVertex = new ExecutionStepAddressRange(addressRange);
    customGraph.addEdge(targetVertex, originalStep, label);
    spritesAt.add(address);
  }

  private boolean checkSource(SpyReference source) {
    if (source == null)
      return true;
    else if (source instanceof ReadMemoryReference) {
      ReadMemoryReference<WordNumber> readMemoryOpcodeReference = (ReadMemoryReference<WordNumber>) source;
      if (readMemoryOpcodeReference.address.intValue() < 0x4000)
        return true;
      else if (isSpriteAddress(readMemoryOpcodeReference.address.intValue()))
        return true;
    }
    return false;
  }

  private List<ExecutionStep> findOriginalSourceOf(ExecutionStep foundStep, SpyReference source, ExecutionStep prev) {

    List<ExecutionStep> results = new ArrayList<>();
//    if (customGraph.edges > 100000)
//      return results;

    if (checkSource(source))
      return Arrays.asList(foundStep);

    ExecutionStep currentStep = foundStep;

    while (currentStep != null) {
      if (source instanceof ReadMemoryReference) {
        ReadMemoryReference<WordNumber> readMemoryOpcodeReference = (ReadMemoryReference<WordNumber>) source;

//        if (readMemoryOpcodeReference.address == 27581) {
//          System.out.println("AAAA");
//        }

        List<ExecutionStep<T>> list = memoryChanges.get(readMemoryOpcodeReference.address.intValue());

        int currentIndex = currentStep.i;
        Optional<ExecutionStep<T>> first = list.stream().filter(step -> step.i < currentIndex).findFirst();

        if (first.isPresent()) {
//          for (int i = prev.i - 1; i > currentStep.i; i--) {
//            customGraph.addEdge(executionStepDatas.get(i), executionStepDatas.get(i + 1), "m2");
//          }

          currentStep = first.get();

          List<ExecutionStep> fromSources = findFromSources(currentStep, prev, "memory");

          return fromSources;
        }
      } else {
        if (targetIsEqual(currentStep, source)) {
          List<ExecutionStep> fromSources = findFromSources(currentStep, prev, "register");
//          for (int i = prev.i - 1; i > currentStep.i; i--) {
//            customGraph.addEdge(executionStepDatas.get(i), executionStepDatas.get(i + 1), "r2");
//          }
          return fromSources;
        }
      }

      currentStep = getPreviousStep(currentStep);
    }

    return results;
  }

  private boolean targetIsEqual(ExecutionStep<T> currentStep, SpyReference source) {
    for (WriteOpcodeReference<T> wr : currentStep.writeReferences) {
      if (wr.sameReference(source))
        return true;
    }
    return false;
  }

  private ExecutionStep getPreviousStep(ExecutionStep last) {
    int index = last.i - 1;
    ExecutionStep executionStep = index >= 0 ? executionSteps.get(index) : null;
    return executionStep;
  }

  private List<ExecutionStep> findFromSources(ExecutionStep executionStep, ExecutionStep previous, String label) {
    customGraph.addEdge(executionStep, previous, label);
    List<ExecutionStep> results = new ArrayList<>();
    addSources(executionStep, results, executionStep.readMemoryReferences);
    addSources(executionStep, results, executionStep.readReferences);

    return results;
  }

  private void addSources(ExecutionStep executionStep, List<ExecutionStep> results, List<? extends SpyReference> readMemoryReferences) {
    if (!readMemoryReferences.isEmpty()) {
      for (SpyReference readMemoryReference : readMemoryReferences) {
        if (checkSource(readMemoryReference))
          results.add(executionStep);
        else {
          boolean processChain = true;
          if (readMemoryReference instanceof ReadMemoryReference) {
            ReadMemoryReference<WordNumber> readMemoryOpcodeReference = (ReadMemoryReference<WordNumber>) readMemoryReference;
            processChain = !readMemoryOpcodeReference.indirectReference;
          }

          if (processChain)
            results.addAll(findOriginalSourceOf(getPreviousStep(executionStep), readMemoryReference, executionStep));
        }
      }
    }
  }

  private boolean isSpriteAddress(int address) {
    return memorySpy.getAddressModificationsCounter(address) <= 100;
  }

  private boolean isScreenWriting(Object accessReference) {
    if (accessReference instanceof WriteMemoryReference) {
      WriteMemoryReference<T> wr = (WriteMemoryReference) accessReference;
      if (wr.address.intValue() >= 0x4000 && wr.address.intValue() <= (0x5000))
        return true;
    }
    return false;
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  public void setGameName(String gameName) {

  }
}
