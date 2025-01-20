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

package com.fpetrola.z80.routines;

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.CodeBlockType;
import com.fpetrola.z80.blocks.UnknownBlockType;
import com.fpetrola.z80.instructions.types.Instruction;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.fpetrola.z80.helpers.Helper.formatAddressPlain;
import static java.util.Arrays.asList;

@SuppressWarnings("ALL")
public class Routine {
  private boolean virtual;
  private List<Block> blocks;
  private boolean finished;
  private Map<Integer, Integer> virtualPop = new HashMap<>();
  private List<Instruction> instructions = new ArrayList<>();
  private int entryPoint;
  public RoutineManager routineManager;
  private MultiValuedMap<Integer, Integer> returnPoints = new HashSetValuedHashMap<>();

  private MultiValuedMap<Integer, Integer> returnPointsDropped = new HashSetValuedHashMap<>();

  public Set<String> parameters = new HashSet<>();
  public Set<String> returnValues = new HashSet<>();
  private boolean callable = true;

  public MultiValuedMap<Integer, Integer> getReturnPointsDropped() {
    return returnPointsDropped;
  }

  public Routine(boolean virtual) {
    this.virtual = virtual;
  }

  public Routine(Block block, int entryPoint, boolean virtual) {
    this(new ArrayList<>(asList(block)), entryPoint, virtual);
  }

  public Routine(List<Block> blocks, int entryPoint, boolean virtual) {
    this.virtual = virtual;
    this.blocks = blocks;
    this.setEntryPoint(entryPoint);
  }

  public List<Routine> getAllRoutines() {
    List<Routine> flat = new ArrayList<>();
    flat.add(this);
    return flat;
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  public void setCallable(boolean callable) {
    this.callable = callable;
  }

  public void addInstruction(Instruction instruction) {
    instructions.add(instruction);
  }

  public boolean contains(int address) {
//    Helper.breakInStackOverflow();
    ArrayList<Block> blocks1 = new ArrayList<>(blocks);
    boolean b1 = blocks1.stream().anyMatch(b -> b != null && b.contains(address));
    return b1;
  }

  public void addInnerRoutine(Routine routine) {
    routineManager.addRoutine(routine);
  }

  public void removeBlocks(List<Block> innerBlocks) {
    getBlocks().removeAll(innerBlocks);
  }

  public void removeBlock(Block block) {
    removeBlocks(asList(block));
  }

  private boolean overlap(Routine routine) {
//    return Block.testOverlap(getStartAddress(), getEndAddress(), routine.getStartAddress(), routine.getEndAddress());
    return overlapByBlocks(routine);
//    return overlapByRecursive(routine);
  }

  private boolean overlapByRecursive(Routine routine) {
    boolean overlap = false;
    for (int i = 0; i < routine.getBlocks().size(); i++) {
      for (int j = 0; j < getBlocks().size(); j++) {
        Block block = routine.getBlocks().get(i);
        Block block1 = getBlocks().get(j);
        if (block.overlap(block1))
          overlap = true;
      }
    }

    return overlap;
  }

  private boolean overlapByBlocks(Routine routine) {
    List<Block> allBlocksInDepth = getAllBlocksInDepth(getAllRoutines());
    List<Routine> allRoutines = routine.getAllRoutines();
    List<Block> allBlocksInDepth1 = getAllBlocksInDepth(allRoutines);

    allBlocksInDepth.addAll(allBlocksInDepth1);

    boolean overlap = false;
    for (int i = 0; i < allBlocksInDepth.size(); i++) {
      for (int j = 0; j < allBlocksInDepth.size(); j++) {
        Block block = allBlocksInDepth.get(i);
        Block block1 = allBlocksInDepth.get(j);
        if (i != j && block.overlap(block1))
          overlap = true;
      }
    }
    return overlap;
  }

  private List<Block> getAllBlocksInDepth(List<Routine> routine) {
    return routine.stream().map(r -> new ArrayList<>(r.getBlocks())).flatMap(List::stream).collect(Collectors.toList());
  }

  public void growTo(int address, int length) {
    Block nearestBlock = findNearestBlock(address);
    nearestBlock.growBlockTo(address + length - 1);
  }

  public Block findNearestBlock(int address) {
    return blocks.stream().filter(b -> b.canTake(address)).findFirst().orElse(null);
  }

  @Override
  public String toString() {
    return "{" + formatAddressPlain(getStartAddress()) + ":" + formatAddressPlain(getEndAddress()) + "} -> " + new ArrayList<>(blocks).toString();
  }

  public void addBlock(Block block) {
    if (blocks.contains(block)) {
      System.out.print("");
      // throw new RuntimeException("block already added");
    } else
      blocks.add(block);
  }

  private static boolean splitBlocksIfRequired(Routine routineAt, Block block, int startAddress1, int startAddress2, Map<Integer, Integer> virtualPop1, MultiValuedMap<Integer, Integer> returnPointsDropped) {
    if (startAddress1 != startAddress2) {
      Block split = block.split(startAddress1 - 1);
      if (startAddress2 != routineAt.entryPoint)
        routineAt.setEntryPoint(startAddress1 - 1);
      return createRoutineFromSplit(routineAt, startAddress1, virtualPop1, split, returnPointsDropped);
    } else {
      if (routineAt.getBlocks().size() > 1) {
        routineAt.removeBlock(block);
        if (block.getRangeHandler().getStartAddress() == routineAt.entryPoint) {
          routineAt.setEntryPoint(routineAt.getBlocks().get(0).getRangeHandler().getStartAddress());
        }
        return createRoutineFromSplit(routineAt, startAddress1, virtualPop1, block, returnPointsDropped);
      }
    }
    return false;
  }

  private static boolean createRoutineFromSplit(Routine routineAt, int startAddress1, Map<Integer, Integer> virtualPop1, Block split, MultiValuedMap<Integer, Integer> returnPointsDropped) {
    Routine routine = new Routine(split, startAddress1, true);

    updateVirtualPops(routineAt, virtualPop1, routine);
    updateReturnPointsDropped(routineAt, returnPointsDropped, routine);

    routineAt.routineManager.addRoutine(routine);
    return true;
  }

  private static void updateVirtualPops(Routine routineAt, Map<Integer, Integer> virtualPops1, Routine routine) {
    routine.getVirtualPop().putAll(virtualPops1);
    Map<Integer, Integer> returnPoints1 = new HashMap<>(routine.getVirtualPop());
    returnPoints1.entrySet().forEach(e -> {
      if (!routineAt.contains(e.getKey())) {
        routineAt.getVirtualPop().remove(e.getKey(), e.getValue());
      }

      if (!routine.contains(e.getKey())) {
        routine.getVirtualPop().remove(e.getKey(), e.getValue());
      }
    });
  }

  private static void updateReturnPointsDropped(Routine routineAt, MultiValuedMap<Integer, Integer> returnPointsDropped, Routine routine) {
    routine.getReturnPointsDropped().putAll(returnPointsDropped);
    returnPointsDropped.entries().forEach(e -> {
      if (!routineAt.contains(e.getValue())) {
        routineAt.getReturnPointsDropped().removeMapping(e.getKey(), e.getValue());
      }

      if (!routine.contains(e.getValue())) {
        routine.getReturnPointsDropped().removeMapping(e.getKey(), e.getValue());
      }
    });
  }

  public void optimize() {
    {
//      if (finished) {
//        if (blocks.size() > 1)
//          System.out.println("finished");
//      }
      blocks.sort(Comparator.comparingInt(b -> b.getRangeHandler().getStartAddress()));

      List<Block> blocksInReverse = new ArrayList<>(blocks);
      Collections.reverse(blocksInReverse);
      blocksInReverse.forEach(block -> {
        Block previousBlock = block.getRangeHandler().getPreviousBlock();
        if (blocks.contains(previousBlock))
          if (previousBlock.isAdjacent(block) && previousBlock.getBlockType() instanceof CodeBlockType) {
            previousBlock.join(block);
            removeBlock(block);
          }
      });

      blocks.sort(Comparator.comparingInt(b -> b.getRangeHandler().getStartAddress()));
    }
  }

  public boolean splitVirtualRoutines() {
    final boolean[] changes = {false};

    for (int i2 = 0; i2 < blocks.size(); i2++) {
      Block block2 = blocks.get(i2);

      int startAddress = block2.getRangeHandler().getStartAddress();
      RoutineManager routineManager1 = routineManager;

      for (int address = startAddress; address <= block2.getRangeHandler().getEndAddress(); address++) {
        List<Integer> integers = routineManager1.callers.get(address);

        int finalAddress = address;
        if (integers.stream().anyMatch(call -> routineManager1.findRoutineAt(call) != routineManager1.findRoutineAt(finalAddress))) {
          changes[0] |= splitBlocksIfRequired(this, block2, address, startAddress, getVirtualPop(), getReturnPointsDropped());
        }

        List<Integer> callees = routineManager1.callees.get(address);
        for (int i = 0; i < callees.size(); i++) {
          int finalI1 = callees.get(i);
          Routine routineAt = routineManager1.findRoutineAt(finalI1);
          if (routineAt != null && routineAt != routineManager1.findRoutineAt(address)) {
            new ArrayList<Block>(routineAt.getBlocks()).forEach(block1 -> {
              if (block1.contains(finalI1)) {
                int startAddress2 = block1.getRangeHandler().getStartAddress();
                changes[0] |= splitBlocksIfRequired(routineAt, block1, finalI1, startAddress2, getVirtualPop(), getReturnPointsDropped());
              }
            });
          }
        }
      }
    }
    return changes[0];
  }

  private boolean contains(Block block) {
    return blocks.contains(block);
  }

  private boolean noneContaining(Block block, ArrayList<Routine> inner) {
    return inner.stream().allMatch(i -> !i.contains(block.getRangeHandler().getStartAddress()));
  }

  public Routine split(int address) {
    Routine[] result = new Routine[1];
    Optional<Block> first = blocks.stream().filter(b -> b.contains(address)).findFirst();
    if (first.get().getRangeHandler().getStartAddress() < address) {
      first.ifPresent(b -> {
        Block split = b.split(address - 1);
        addBlock(split);
        Routine routine = new Routine(split, address, true);
        removeBlock(split);
        addInnerRoutine(routine);
        result[0] = routine;
      });
      result[0].setRoutineManager(routineManager);
    } else {
      Routine routine = new Routine(first.get(), address, false);
      result[0] = routine;
      removeBlock(first.get());
      addInnerRoutine(result[0]);
      result[0].setRoutineManager(routineManager);
    }
    return result[0];
  }

  void addInstructionAt(Instruction instruction, int pcValue) {
    instructions.add(instruction);
    if (!finished) {
      Block currentBlock = routineManager.blocksManager.findBlockAt(pcValue);
      if (currentBlock.getBlockType() instanceof UnknownBlockType) {
        currentBlock.split(pcValue + instruction.getLength() - 1);
        Block blockAt2 = currentBlock.split(pcValue - 1);
        blockAt2.setType(new CodeBlockType());
        addBlock(blockAt2);
      } else {
        Routine routineAt = routineManager.findRoutineAt(pcValue);
        if (routineAt != null && !routineAt.virtual && routineAt != this && !this.overlap(routineAt)) {
          routineAt.getBlocks().forEach(this::addBlock);
          removeBlocks(routineAt.getBlocks());
          routineManager.removeRoutine(routineAt);
          routineAt.virtual = true;
          addInnerRoutine(routineAt);
        }
      }
    } else if (contains(pcValue))
      System.out.hashCode();
    else
      System.out.hashCode();

//    InputOutputDetector.detectInputAndOutput(this, instruction);
  }

  public void setRoutineManager(RoutineManager routineManager) {
    this.routineManager = routineManager;
  }

  public int getStartAddress() {
    List<Block> flat = getAllBlocksInDepth(getAllRoutines());
    return flat.stream().map(b -> b.getRangeHandler().getStartAddress()).min(Comparator.comparingInt(b -> b)).get();
  }

  public int getEndAddress() {
    List<Block> flat = getAllBlocksInDepth(getAllRoutines());
    return flat.stream().map(b -> b.getRangeHandler().getEndAddress()).max(Comparator.comparingInt(b -> b)).get();
  }

  public void addReturnPoint(int returnAddress, int pc) {
    returnPoints.put(returnAddress, pc);
  }

  public void addReturnPointDropped(int returnAddress, int pc) {
    returnPointsDropped.put(returnAddress, pc);
  }


  public void finish() {
    optimize();
    finished = true;
  }

  public <R> R accept(RoutineVisitor<R> routineVisitor) {
    routineVisitor.visit(this);

    List<String> allRegisters = asList("AF", "BC", "DE", "HL", "IX", "IY", "A", "F", "B", "C", "D", "E", "H", "L", "IXL", "IXH", "IYL", "IYH");
    allRegisters.forEach(routineVisitor::visitParameter);
//    parameters.stream().filter(p -> !p.contains("x")).forEach(routineVisitor::visitParameter);


//    allRegisters.forEach(routineVisitor::visitReturnValue);

    asList("IX", "F", "D").forEach(routineVisitor::visitReturnValue);
//
//    Set<String> finalParameters = new HashSet<>();
//    finalParameters.addAll(parameters);
//    finalParameters.addAll(returnValues);
//    finalParameters.add("F");
//    finalParameters.stream().filter(p -> !p.contains("x")).forEach(routineVisitor::visitReturnValue);
    Instruction[] lastInstruction = {null};

    for (int i = getStartAddress(); i <= getEndAddress(); i++) {
      Instruction instruction = routineManager.getInstructionExecutor().getInstructionAt(i);
      if (instruction != null) {
        if (instruction != lastInstruction[0]) {
          routineVisitor.visitInstruction(i, instruction);
        }
      }
      lastInstruction[0] = instruction;
    }
    return routineVisitor.getResult();
  }

  public boolean isCallable() {
    return callable;
  }

  public Routine createInnerRoutineBetween(int startAddress, int endAddress) {
    Routine[] result = new Routine[1];
    Optional<Block> first = blocks.stream().filter(b -> b.contains(startAddress)).findFirst();
    if (first.isEmpty())
      System.out.println("wow");
    else {
      Block block = first.get();
      BlocksManager blocksManager = block.getBlocksManager();
      if (block.getRangeHandler().getStartAddress() <= startAddress && block.getRangeHandler().getEndAddress() >= endAddress) {
        List<Block> blocksBetween = blocksManager.getBlocksBetween(startAddress, endAddress);
        Block split = blocksBetween.get(blocksBetween.size() - 1).split(endAddress);
        Block split3 = blocksBetween.get(0).split(startAddress - 1);
        List<Block> blocksBetween2 = blocksManager.getBlocksBetween(startAddress, endAddress);

        if (blocksBetween2.size() > 2)
          System.out.println("dddddddddddddd");
        Routine routine = new Routine(blocksBetween2, entryPoint, true);
        addInnerRoutine(routine);
        result[0] = routine;
        routineManager.addRoutine(result[0]);

      } else {
        System.out.println("multiple routines inside");
//      throw new RuntimeException("block is smaller");
      }
    }
    return result[0];
  }

  public boolean contains(Routine routine) {
    return getAllRoutines().stream().anyMatch(i -> i == routine);
  }

  public Routine findRoutineAt(int address) {
    Optional<Block> b1 = new ArrayList<>(getBlocks()).stream().filter(i -> i != null && i.contains(address)).findFirst();

    if (b1.isPresent())
      return this;
    return null;
  }

  public boolean isFinished() {
    return finished;
  }

  public Map<Integer, Integer> getVirtualPop() {
    return virtualPop;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  public MultiValuedMap<Integer, Integer> getReturnPoints() {
    return returnPoints;
  }

  public Set<String> getParameters() {
    return parameters;
  }

  public Set<String> getReturnValues() {
    return returnValues;
  }

  public Block findBlockOf(int address) {
    return blocks.stream().filter(b -> b.contains(address)).findFirst().get();
  }

  public int getEntryPoint() {
    return entryPoint;
  }

  public void setEntryPoint(int entryPoint) {
    if (entryPoint == 0xDDFF)
      System.out.println("sdfadadgaffff");
    this.entryPoint = entryPoint;
  }

  public boolean isVirtual() {
    return virtual;
  }
}
