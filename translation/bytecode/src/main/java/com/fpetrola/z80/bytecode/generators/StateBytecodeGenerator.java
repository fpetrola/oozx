/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.bytecode.generators.helpers.BytecodeGenerationContext;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.minizx.emulation.LocalMemory;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import org.apache.commons.collections4.CollectionUtils;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.ClassMaker2;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import org.apache.commons.collections4.MultiValuedMap;
import java.util.stream.Collectors;

import static com.fpetrola.z80.bytecode.generators.MemoryType.*;

public class StateBytecodeGenerator {
  private final String className;
  private final RoutineManager routineManager;
  private final State state;
  private final boolean translation;
  private final Class<?> translationSuperClass;
  private final Class<?> executionSuperClass;
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  private final String base64Memory;
  private final GameData gameData;
  private Map<String, byte[]> bytecodes = new HashMap<>();

  public StateBytecodeGenerator(String className, RoutineManager routineManager, State state, boolean translation, Class<?> translationSuperClass, Class<?> executionSuperClass, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory, GameData gameData) {
    this.className = className;
    this.routineManager = routineManager;
    this.state = state;
    this.translation = translation;
    this.translationSuperClass = translationSuperClass;
    this.executionSuperClass = executionSuperClass;
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.base64Memory = base64Memory;
    this.gameData = gameData;
  }

  private ClassMaker translate() {
    ClassLoader classLoader = StateBytecodeGenerator.class.getClassLoader();
    if (!translation)
      classLoader = ClassLoader.getSystemClassLoader();
//    ClassMaker classMaker = ClassMaker.begin(className, classLoader).public_();

    ClassMaker classMaker = ClassMaker2.beginExternal(className, classLoader).public_();
    if (translation) {
      classMaker.extend(translationSuperClass);
    } else {
      classMaker.extend(executionSuperClass);
    }

    MethodMaker methodMaker = classMaker.addConstructor().public_();
    methodMaker.invokeSuperConstructor();

    //createMainMethod(classMaker);

    if (translation) {
      MethodMaker getProgramBytesMaker = classMaker.addMethod(String.class, "getProgramBytes").public_();
      Variable bytes = getProgramBytesMaker.var(String.class).set(base64Memory.substring(0, Math.min(60000, base64Memory.length())));
      for (int from = 60000; from < base64Memory.length(); from += 60000)
        bytes = bytes.invoke("concat", base64Memory.substring(from, Math.min(from + 60000, base64Memory.length())));
      getProgramBytesMaker.return_(bytes);
    }
    if (symbolicExecutionAdapter.getStackAnalyzer().layoutCallSites.contains(StackAnalyzer.INTERRUPT))
      classMaker.addMethod(boolean.class, "pushesInterruptReturns").public_().return_(true);
    List<RoutineManager.CodeVariant> bankCopies = routineManager.codeVariants.stream().filter(copy -> copy.bank() != -1).toList();
    if (!bankCopies.isEmpty()) {
      MethodMaker pagedCopy = classMaker.addMethod(int.class, "pagedCopy", int.class).protected_();
      Variable address = pagedCopy.param(0), bank = pagedCopy.invoke("bank");
      bankCopies.forEach(copy -> bank.ifEq(copy.bank(), () -> address.ifGe(copy.start(), () -> address.ifLt(copy.end(), () -> pagedCopy.return_(address.add(copy.relocatedAt() - copy.start()))))));
      pagedCopy.return_(address);
    }

    BytecodeGenerationContext bytecodeGenerationContext = new BytecodeGenerationContext(routineManager, classMaker, state.getPc(), symbolicExecutionAdapter, gameData, !translation);

//    Routine routine1 = routineManager.getRoutines().stream().filter(r -> r.getEntryPoint() == 34463).findFirst().get();
//    Block block = routine1.getBlocks().get(0);
//    routine1.removeBlock(block);
//    routine1.setEntryPoint(34762);
//    routineManager.addRoutine(new Routine(block, 34463, true));
////    routine1.split(34762);

    if (!routineManager.planned) {
      routineManager.planPoppedReturnsOfRewrittenCalls(symbolicExecutionAdapter.getStackAnalyzer());
      routineManager.planEntries(symbolicExecutionAdapter.getStackAnalyzer());
      new ArrayList<>(routineManager.getRoutines()).forEach(this::splitIfTooLargeForOneMethod);
      foldJumpCyclesIntoTheirHeads();
      routineManager.planNonLocalReturns(symbolicExecutionAdapter.getStackAnalyzer(), bytecodeGenerationContext.routinesInJumpCycles());
    }
    if (Boolean.getBoolean("translation.reportJumpCycles"))
      routineManager.jumpCycleComponents(symbolicExecutionAdapter.getStackAnalyzer()).forEach(cycle -> System.out.println("JUMP CYCLE " + cycle.members().stream().map(r -> Helper.formatAddress(r.getEntryPoint())).toList()
          + " entered by " + cycle.entries().keySet().stream().sorted().map(cause -> cause + " " + cycle.entries().get(cause).stream().sorted().map(Helper::formatAddress).toList()).collect(java.util.stream.Collectors.joining(", "))));
    List<Routine> routines = routineManager.getRoutinesInDepth();

    RoutineBytecodeGenerator routineBytecodeGenerator1 = new RoutineBytecodeGenerator(bytecodeGenerationContext, null);
    routineBytecodeGenerator1.createMethod(0);

    routines.sort(Comparator.comparingInt(Routine::getEntryPoint));

    int[] jumpMembers = bytecodeGenerationContext.routinesInJumpCycles().stream().mapToInt(Routine::getEntryPoint).sorted().toArray();
    MethodMaker runJumps = jumpMembers.length > 0 ? classMaker.addMethod(int.class, "runJumps", int.class).public_() : null;

    routines.forEach(routine -> {
      routine.optimize();
      RoutineBytecodeGenerator routineBytecodeGenerator = new RoutineBytecodeGenerator(bytecodeGenerationContext, routine);
      routineBytecodeGenerator.getMethod(routine.getEntryPoint());
    });

    routines.forEach(routine -> {
      System.out.println(routine);
      if (true || !routine.toString().contains("{7D9E:DE53}")) {
        RoutineBytecodeGenerator routineBytecodeGenerator = new RoutineBytecodeGenerator(bytecodeGenerationContext, routine);
        routineBytecodeGenerator.generate();
      }
    });

    java.util.stream.Stream.concat(routineManager.externalEntries.stream(), bytecodeGenerationContext.dispatchedEntries.stream()).distinct().forEach(entry -> {
      Routine owner = routineManager.findRoutineAt(entry);
      if (owner != null && owner.getEntryPoint() != entry && !bytecodeGenerationContext.methods.containsKey(entry)) {
        MethodMaker entryMethod = new RoutineBytecodeGenerator(bytecodeGenerationContext, owner).getMethod(entry);
        enterThrough(entryMethod, entry, bytecodeGenerationContext);
        entryMethod.return_();
      }
    });

    MethodMaker runTranslated = classMaker.addMethod(boolean.class, "runTranslated", int.class).protected_();
    int[] translated = bytecodeGenerationContext.methods.keySet().stream().filter(address -> routineManager.findRoutineAt(address) != null).mapToInt(Integer::intValue).sorted().toArray();
    Label[] entries = java.util.stream.IntStream.range(0, translated.length).mapToObj(i -> runTranslated.label()).toArray(Label[]::new);
    Label none = runTranslated.label();
    runTranslated.invoke("pagedCopy", runTranslated.param(0)).switch_(none, translated, entries);
    for (int i = 0; i < translated.length; i++) {
      entries[i].here();
      enterThrough(runTranslated, translated[i], bytecodeGenerationContext);
      runTranslated.return_(true);
    }
    none.here();
    runTranslated.return_(false);

    if (jumpMembers.length > 0) {
      Variable next = runJumps.var(int.class).set(runJumps.param(0));
      Label loop = runJumps.label().here(), exit = runJumps.label();
      Label[] members = java.util.stream.IntStream.range(0, jumpMembers.length).mapToObj(i -> runJumps.label()).toArray(Label[]::new);
      next.set(runJumps.invoke("pagedCopy", next));
      next.switch_(exit, jumpMembers, members);
      for (int i = 0; i < jumpMembers.length; i++) {
        members[i].here();
        next.set(runJumps.invoke(RoutineBytecodeGenerator.createLabelName(jumpMembers[i])));
        runJumps.invoke("unwinding").ifTrue(exit::goto_);
        loop.goto_();
      }
      exit.here();
      runJumps.return_(next);
    }

    return classMaker;
  }

  /** Entering translated code at an address: through the method of the routine that runs its loop, with the address as where to go on when it is not that method's start. */
  private void enterThrough(MethodMaker mm, int address, BytecodeGenerationContext context) {
    Routine top = routineManager.topParent(routineManager.findRoutineAt(address));
    if (address != top.getEntryPoint())
      mm.invoke("setNextAddress", address);
    if (context.routinesInJumpCycles().contains(top))
      mm.invoke("runJumps", top.getEntryPoint());
    else
      mm.invoke(RoutineBytecodeGenerator.createLabelName(top.getEntryPoint()));
  }

  private static final int MAX_ROUTINE_BYTES = Integer.getInteger("translation.methodBytes", 1500);

  /** Routines that jump to each other around a head (a member every cycle passes through) are a loop in the head's method: the others become its parts, which report where they went on, so no runJumps is needed. The head is the member that is called, or else the one entered from outside the most; a cycle with a called member besides the head, or with a jump back to an ancestor frame between members, stays as it is: those are calls and returns, not turns of a loop. */
  private void foldJumpCyclesIntoTheirHeads() {
    StackAnalyzer stackAnalyzer = symbolicExecutionAdapter.getStackAnalyzer();
    RoutineManager.AncestorJumps ancestorJumps = routineManager.ancestorJumps(stackAnalyzer);
    MultiValuedMap<Integer, Integer> callSites = routineManager.callSitesByTarget();
    routineManager.jumpCycleComponents(stackAnalyzer).forEach(cycle -> {
      if (cycle.members().stream().anyMatch(from -> cycle.members().stream().anyMatch(to -> ancestorJumps.goesBack(from, to)))) {
        if (Boolean.getBoolean("translation.reportRoutines"))
          System.out.println("LOOP kept as runJumps " + cycle.members().stream().map(m -> Helper.formatAddress(m.getEntryPoint())).toList() + " jumps back to an ancestor");
        return;
      }
      Routine head = cycle.members().stream().max(Comparator.comparingInt((Routine member) -> callSites.get(member.getEntryPoint()).size()).thenComparingLong(member -> cycle.entries().values().stream().filter(member::contains).count()).thenComparing(Comparator.comparingInt(Routine::getEntryPoint).reversed())).get();
      cycle.members().stream().filter(member -> member != head).forEach(task -> {
        ControlFlowStructuring.Outline whole = new ControlFlowStructuring(task, routineManager, List.of(), stackAnalyzer, Set.of(), node -> false).whole();
        routineManager.outlined.put(task, new RoutineManager.Outlined(head, whole.exits(), whole.leaves()));
      });
      if (Boolean.getBoolean("translation.reportRoutines"))
        System.out.println("LOOP " + Helper.formatAddress(head.getEntryPoint()) + " tasks " + cycle.members().stream().filter(m -> m != head).map(m -> Helper.formatAddress(m.getEntryPoint()) + "->" + routineManager.outlined.get(m).exits().stream().map(exit -> Helper.formatAddress(exit.to()) + "<-" + Helper.formatAddress(exit.from())).toList()).toList());
    });
  }

  private void splitIfTooLargeForOneMethod(Routine routine) {
    if (size(routine) > MAX_ROUTINE_BYTES)
      cut(routine, MAX_ROUTINE_BYTES).forEach(this::splitIfTooLargeForOneMethod);
  }

  /** When Fernflower drops the whole class (it runs out of memory on a method): the biggest routine cut into parts. */
  public boolean splitBiggestRoutine() {
    return routineManager.getRoutines().stream().max(Comparator.comparingInt(StateBytecodeGenerator::size)).map(routine -> splitRoutineAt(routine.getEntryPoint())).orElse(false);
  }

  /** For a second generation of the class, which keeps the plan of the first: the routine cut into parts, when it can be. */
  public boolean splitRoutineAt(int entryPoint) {
    routineManager.planned = true;
    Routine routine = routineManager.findRoutineAt(entryPoint);
    return routine != null && !cut(routine, size(routine) / 2).isEmpty();
  }

  /** The parts of the routine that can be methods it calls, until at most {@code keep} bytes remain; when they are not enough, its blocks as before, since a part must not end up jumped into from a block. */
  private List<Routine> cut(Routine routine, int keep) {
    StackAnalyzer stackAnalyzer = symbolicExecutionAdapter.getStackAnalyzer();
    Set<Integer> entries = routineManager.entriesFromOutside(routine);
    entries.addAll(routineManager.catchPointsOfCallsIn(routine).values());
    entries.addAll(routineManager.returnsOfCallsInTakenByNonLocalReturns(routine, stackAnalyzer));
    RoutineManager.AncestorJumps ancestorJumps = routineManager.ancestorJumps(stackAnalyzer);
    Set<Integer> landings = ancestorJumps.landings().getOrDefault(routine, Set.of());
    entries.addAll(landings);
    Set<Routine> unwinding = routineManager.routinesThatMayUnwind(stackAnalyzer, ancestorJumps);
    List<ControlFlowStructuring.Outline> outlines = new ControlFlowStructuring(routine, routineManager, entries, stackAnalyzer, landings, address -> unwinding.contains(routineManager.findRoutineAt(address))).outlines(keep);
    if (Boolean.getBoolean("translation.reportRoutines"))
      System.out.println("CUT " + routine + " keep " + keep + (routineManager.outlined.containsKey(routine) ? " part" : "") + " entries " + entries.stream().map(Helper::formatAddress).toList() + " parts " + outlines.stream().map(part -> "%s->%s %d bytes%s".formatted(Helper.formatAddress(part.entry()), part.exits().stream().map(exit -> Helper.formatAddress(exit.to())).toList(), part.nodes().size(), part.leaves() ? " leaves" : "")).toList());
    boolean inAFamily = routineManager.outlined.containsKey(routine) || routineManager.outlined.values().stream().anyMatch(part -> part.parent() == routine);
    if (outlines.isEmpty() || !inAFamily && size(routine) - outlines.stream().mapToInt(part -> part.nodes().stream().filter(routine::contains).mapToInt(address -> routineManager.getInstructionAt(address).getLength()).sum()).sum() > keep) {
      if (Boolean.getBoolean("translation.reportRoutines"))
        System.out.println("CUT " + routine + (inAFamily ? " stays" : " by blocks"));
      return inAFamily ? List.of() : split(routine);
    }
    List<Routine> pieces = new ArrayList<>(outlines.stream().map(part -> routineManager.outline(routine, part.entry(), part.nodes().stream().filter(routine::contains).collect(Collectors.toCollection(LinkedHashSet::new)), part.exits(), part.leaves())).toList());
    pieces.add(routine);
    return pieces;
  }

  private List<Routine> split(Routine routine) {
    List<Block> blocks = routine.getBlocks().stream().sorted(Comparator.comparingInt(b -> b.getRangeHandler().getStartAddress())).toList();
    if (blocks.size() > 1) {
      List<Routine> pieces = new ArrayList<>(List.of(routine));
      blocks.subList(1, blocks.size()).forEach(block -> pieces.add(routine.split(block.getRangeHandler().getStartAddress())));
      return pieces;
    }
    return cutNearestToMiddleOf(blocks.get(0)).map(target -> List.of(routine.split(target), routine)).orElse(List.of());
  }

  private Optional<Integer> cutNearestToMiddleOf(Block block) {
    int start = block.getRangeHandler().getStartAddress(), end = block.getRangeHandler().getEndAddress(), middle = (start + end) / 2;
    Comparator<Integer> nearestToMiddle = Comparator.comparingInt(target -> Math.abs(target - middle));
    return java.util.stream.Stream.concat(routineManager.callers.keySet().stream(), java.util.stream.IntStream.rangeClosed(start, end).map(address -> RoutineManager.fixedJumpTarget(routineManager.getInstructionAt(address))).boxed())
        .filter(target -> target > start && target <= end).min(nearestToMiddle)
        .or(() -> java.util.stream.IntStream.range(start + 1, end).filter(address -> routineManager.getInstructionAt(address) != null && routineManager.getInstructionAt(address) != routineManager.getInstructionAt(address - 1)).boxed().min(nearestToMiddle));
  }

  private static int size(Routine routine) {
    return routine.getBlocks().stream().mapToInt(b -> b.getRangeHandler().getEndAddress() - b.getRangeHandler().getStartAddress() + 1).sum();
  }

  public Map<String, byte[]> getBytecode() {
    ClassMaker translate = translate();
    byte[] bytes = translate.finishBytes();
    bytecodes.put(className, bytes);
    return bytecodes;
  }
}
