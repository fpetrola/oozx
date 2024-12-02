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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.fpetrola.z80.helpers.Helper.formatAddress;
import static java.util.Arrays.asList;

@SuppressWarnings("ALL")
public class Routine {
  public List<Block> blocks;

  public boolean finished;
  public Map<Integer, Integer> virtualPop = new HashMap<>();
  private List<Instruction> instructions = new ArrayList<>();

  public List<Routine> getInnerRoutines() {
    return innerRoutines;
  }

  public List<Routine> innerRoutines = new ArrayList<>();
  private RoutineManager routineManager;
  public MultiValuedMap<Integer, Integer> returnPoints = new HashSetValuedHashMap<>();

  public Set<String> parameters = new HashSet<>();
  public Set<String> returnValues = new HashSet<>();
  private boolean callable = true;

  public List<Routine> getAllRoutines() {
    List<Routine> flat = new ArrayList<>();
    flat.add(this);

    List<Routine> flat2 = innerRoutines.stream().map(ir -> ir.getAllRoutines())
        .flatMap(List::stream)
        .collect(Collectors.toList());

    flat.addAll(flat2);
    return flat;
  }

  public List<Routine> getAllInnerRoutines() {

    List<Routine> flat2 = innerRoutines.stream().map(ir -> ir.getAllRoutines())
        .flatMap(List::stream)
        .collect(Collectors.toList());

    return flat2;
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  public void setCallable(boolean callable) {
    this.callable = callable;
  }

  public Routine() {
  }

  public Routine(Block block) {
    this(new ArrayList<>(asList(block)));
  }

  public Routine(List<Block> blocks) {
    this.blocks = blocks;
  }

  public void addInstruction(Instruction instruction) {
    instructions.add(instruction);
  }

  public boolean contains(int address) {
    Helper.breakInStackOverflow();
    boolean b1 = blocks.stream().anyMatch(b -> b.contains(address));
    boolean b = innerRoutines.stream().anyMatch(i -> i.contains(address));
    return b1 | b;
  }

  public void addInnerRoutine(Routine routine) {
    if (routine == this)
      throw new RuntimeException("cannot add it to self");
    if (routine == null)
      throw new RuntimeException("null inner routine");

    boolean b1 = routine.overlap(this);
    if (b1)
      System.out.println("overlapped");

    boolean b = routine.getAllInnerRoutines().stream().anyMatch(i -> i.containsInner(this));
    if (b)
      throw new RuntimeException("cannot add it to inner");

    if (routineManager.getRoutines().contains(routine))
      System.out.println("already in routinemanager");

    List<Block> innerBlocks = routine.getBlocks();
    blocks.removeAll(innerBlocks);
    innerRoutines.add(routine);
    routineManager.removeRoutine(routine);
  }

  private boolean overlap(Routine routine) {
    List<Block> allBlocksInDepth = getAllBlocksInDepth(getAllRoutines());
    List<Routine> allRoutines = routine.getAllRoutines();
    List<Block> allBlocksInDepth1 = getAllBlocksInDepth(allRoutines);

    allBlocksInDepth.addAll(allBlocksInDepth1);

    boolean overlap = false;
    for (int i = 0; i < allBlocksInDepth.size(); i++) {
      for (int j = 0; j < allBlocksInDepth.size(); j++) {
        if (i != j && allBlocksInDepth.get(i).overlap(allBlocksInDepth.get(j)))
          overlap = true;
      }
    }

    return overlap;
  }

  private List<Block> getAllBlocksInDepth(List<Routine> routine) {
    return routine.stream().map(r -> r.getBlocks()).flatMap(List::stream).collect(Collectors.toList());
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
    return "{" + formatAddress(getStartAddress()) + ":" + formatAddress(getEndAddress()) + "} -> " + blocks.toString();
  }

  public void addBlock(Block block) {
    if (blocks.contains(block)) {
      System.out.print("");
      // throw new RuntimeException("block already added");
    } else
      blocks.add(block);
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
            ArrayList<Routine> inner = new ArrayList<>(innerRoutines);
            if (inner.isEmpty() || (isNotInner(previousBlock) && isNotInner(block))) {
              previousBlock.join(block);
              blocks.remove(block);
            }
          }
      });

      innerRoutines.stream().forEach(ir -> {
        if (ir != null)
          parameters.addAll(ir.parameters);
        else
          System.out.println("ir is null");
      });
    }
  }

  private boolean isNotInner(Block block) {
    return innerRoutines.stream().noneMatch(i -> i != null && i.contains(block));
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
        Routine routine = new Routine(split);
        blocks.remove(split);
        addInnerRoutine(routine);
        result[0] = routine;
      });
      result[0].setRoutineManager(routineManager);
    } else {
      Routine routine = new Routine(first.get());
      result[0] = routine;
      blocks.remove(first.get());
      addInnerRoutine(result[0]);
      result[0].setRoutineManager(routineManager);
    }
    return result[0];
  }

  void addInstructionAt(Instruction instruction, int pcValue) {
    if (!finished) {
      Block currentBlock = routineManager.blocksManager.findBlockAt(pcValue);
      if (currentBlock.getBlockType() instanceof UnknownBlockType) {
        currentBlock.split(pcValue + instruction.getLength() - 1);
        Block blockAt2 = currentBlock.split(pcValue - 1);
        blockAt2.setType(new CodeBlockType());
        addBlock(blockAt2);
      } else {
        Routine routineAt = routineManager.findRoutineAt(pcValue);
        if (routineAt != this && !this.overlap(routineAt)) {
          routineAt.blocks.forEach(this::addBlock);
          blocks.removeAll(routineAt.getBlocks());
          routineManager.removeRoutine(routineAt);
          addInnerRoutine(routineAt);
        }
      }
    } else if (contains(pcValue))
      System.out.hashCode();
    else
      System.out.hashCode();

    detectInputAndOutput(instruction);
  }

  private void detectInputAndOutput(Instruction instruction) {
    instruction.accept(new RegisterFinderInstructionVisitor() {
      public boolean visitRegister(Register register) {
        if (register instanceof VirtualRegister<?> virtualRegister) {
          addParameter(virtualRegister);
          addReturnValue(virtualRegister);
//        addReturnValues(virtualRegister);
//        addParameters(virtualRegister);
        }
        return super.visitRegister(register);
      }

      private void addParameters(VirtualRegister<?> virtualRegister) {
        boolean isParameter = virtualRegister.getPreviousVersions().stream().anyMatch(previous -> routineManager.findRoutineAt(previous.getRegisterLine()) != Routine.this);
        if (isParameter)
          addParameter(virtualRegister);
      }

      private void addReturnValues(VirtualRegister<?> virtualRegister) {
        boolean isReturnValue = virtualRegister.getDependants().stream().anyMatch(dependantRegister -> {
          boolean[] isReturnValue2 = new boolean[]{false};
          if (routineManager.findRoutineAt(dependantRegister.getRegisterLine()) != routineManager.findRoutineAt(virtualRegister.getRegisterLine())) {
            if (dependantRegister instanceof Virtual8BitsRegister<?> dependantVirtual8BitsRegister) {
              checkReturn(virtualRegister, dependantVirtual8BitsRegister, isReturnValue2);
            } else if (dependantRegister instanceof VirtualComposed16BitRegister<?> virtualComposed16BitRegister) {
              checkReturn((VirtualRegister<?>) virtualRegister, (Virtual8BitsRegister<?>) virtualComposed16BitRegister.getHigh(), isReturnValue2);
              checkReturn((VirtualRegister<?>) virtualRegister, (Virtual8BitsRegister<?>) virtualComposed16BitRegister.getLow(), isReturnValue2);
            } else
              System.out.println();
          }
          return isReturnValue2[0];
        });
        if (isReturnValue)
          addReturnValue(virtualRegister);
      }
    });
  }

  private void checkReturn(VirtualRegister<?> virtualRegister, Virtual8BitsRegister<?> dependantVirtual8BitsRegister, boolean[] isReturnValue2) {
    Instruction<?> instruction = dependantVirtual8BitsRegister.instruction;
    instruction.accept(new RegisterFinderInstructionVisitor() {
      public boolean visitRegister(Register register) {
        boolean sameInitial = false;
        if (isSource) {
          sameInitial = getFirstVersion(virtualRegister) == getFirstVersion((VirtualRegister) register);
          if (sameInitial) {
            isReturnValue2[0] = sameInitial;
          }
        }
        return sameInitial;
      }
    });
  }

  private VirtualRegister getFirstVersion(VirtualRegister virtualRegister) {
    return (VirtualRegister) virtualRegister.getVersionHandler().versions.getFirst();
  }

  private void addParameter(VirtualRegister register) {
    parameters.add(getFirstVersion(register).getName());
  }

  private void addReturnValue(VirtualRegister register) {
    returnValues.add(getFirstVersion(register).getName());
  }

  public void setRoutineManager(RoutineManager routineManager) {
    this.routineManager = routineManager;
  }

  public int getStartAddress() {
    return blocks.stream().map(b -> b.getRangeHandler().getStartAddress()).min(Comparator.comparingInt(b -> b)).get();
  }

  public int getEndAddress() {
    List<Block> flat = getAllBlocksInDepth(getAllRoutines());
    return flat.stream().map(b -> b.getRangeHandler().getEndAddress()).max(Comparator.comparingInt(b -> b)).get();
  }

  public void addReturnPoint(int returnAddress, int pc) {
    returnPoints.put(returnAddress, pc);
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
      Instruction instruction = routineManager.getRandomAccessInstructionFetcher().getInstructionAt(i);
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
        Block split = blocksBetween.getLast().split(endAddress);
        Block split3 = blocksBetween.getFirst().split(startAddress - 1);
        List<Block> blocksBetween2 = blocksManager.getBlocksBetween(startAddress, endAddress);

        if (blocksBetween2.size() > 2)
          System.out.println("dddddddddddddd");
        Routine routine = new Routine(blocksBetween2);
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

  public boolean containsInner(Routine routine) {
    return getAllInnerRoutines().stream().anyMatch(i -> i == routine);
  }

  public Routine findRoutineAt(int address) {
    Optional<Block> b1 = blocks.stream().filter(i -> i != null && i.contains(address)).findFirst();

    if (b1.isPresent())
      return this;
    else {
      Optional<Routine> b = innerRoutines.stream().filter(i -> i.findRoutineAt(address) != null).findFirst();
      if (b.isPresent())
        return b.get().findRoutineAt(address);
    }
    return null;
  }
}
