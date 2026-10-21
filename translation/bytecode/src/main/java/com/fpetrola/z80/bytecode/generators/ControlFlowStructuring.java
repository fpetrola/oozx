package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;

import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * Loops a routine enters in more than one place ("No More Gotos", Yakdan et al., NDSS 2015): each such region keeps one header, and every
 * other entry is reached by going to the header with the structuring variable (nextAddress) set to it, the header dispatching on it.
 */
public class ControlFlowStructuring {
  private final Map<Integer, Set<Integer>> successors = new HashMap<>();
  /** Header of each region entered in more than one place, with the other entries and the nodes inside. */
  private final Map<Integer, Region> regions = new HashMap<>();
  private final Map<Integer, Region> regionOfEntry = new HashMap<>();

  public record Region(int header, Set<Integer> entries, Set<Integer> nodes) {}

  /** {@code continuation}: where the code goes on after the instruction at an address that falls through. */
  public ControlFlowStructuring(Routine routine, RoutineManager routineManager, Collection<Integer> entries, IntUnaryOperator continuation) {
    List<Integer> nodes = new ArrayList<>();
    for (int address = routine.getStartAddress(); address <= routine.getEndAddress(); address++)
      if (routine.contains(address) && routineManager.getInstructionAt(address) != null)
        nodes.add(address);
    for (int node : nodes) {
      Instruction instruction = routineManager.getInstructionAt(node);
      Set<Integer> next = successors.computeIfAbsent(node, k -> new LinkedHashSet<>());
      if (RoutineManager.fallsThrough(instruction))
        next.add(continuation.applyAsInt(node));
      if (!(instruction instanceof Call) && RoutineManager.fixedJumpTarget(instruction) != -1)
        next.add(RoutineManager.fixedJumpTarget(instruction));
      next.removeIf(to -> !successors.containsKey(to) && !nodes.contains(to));
    }
    List<Integer> roots = new ArrayList<>();
    roots.add(routine.getEntryPoint());
    entries.stream().filter(nodes::contains).filter(at -> at != routine.getEntryPoint()).sorted().forEach(roots::add);
    structure(new LinkedHashSet<>(nodes), roots, new HashSet<>(roots));
  }

  private void structure(Set<Integer> nodes, List<Integer> roots, Set<Integer> enteredFromOutside) {
    Map<Integer, Integer> order = preorder(nodes, roots);
    for (Set<Integer> component : components(nodes, order)) {
      if (component.size() == 1 && !successors.getOrDefault(component.iterator().next(), Set.of()).contains(component.iterator().next()))
        continue;
      Set<Integer> entries = new TreeSet<>(Comparator.comparingInt(order::get));
      component.stream().filter(enteredFromOutside::contains).forEach(entries::add);
      nodes.stream().filter(from -> !component.contains(from)).forEach(from -> successors.get(from).stream().filter(component::contains).forEach(entries::add));
      if (entries.isEmpty())
        continue;
      int header = entries.iterator().next();
      if (entries.size() > 1) {
        Region region = new Region(header, new LinkedHashSet<>(entries.stream().filter(at -> at != header).toList()), component);
        regions.put(header, region);
        region.entries().forEach(entry -> regionOfEntry.put(entry, region));
      }
      Set<Integer> inner = new LinkedHashSet<>(component);
      inner.remove(header);
      structure(inner, successors.get(header).stream().filter(inner::contains).toList(), new HashSet<>(entries));
    }
  }

  private Map<Integer, Integer> preorder(Set<Integer> nodes, List<Integer> roots) {
    Map<Integer, Integer> order = new HashMap<>();
    Deque<Integer> pending = new ArrayDeque<>();
    for (int root : roots) {
      pending.push(root);
      while (!pending.isEmpty()) {
        int node = pending.pop();
        if (!nodes.contains(node) || order.containsKey(node))
          continue;
        order.put(node, order.size());
        List<Integer> next = new ArrayList<>(successors.getOrDefault(node, Set.of()));
        Collections.reverse(next);
        next.forEach(pending::push);
      }
    }
    nodes.stream().filter(n -> !order.containsKey(n)).forEach(n -> order.put(n, order.size()));
    return order;
  }

  /** Strongly connected components (Tarjan, iterative) of the subgraph on these nodes. */
  private List<Set<Integer>> components(Set<Integer> nodes, Map<Integer, Integer> order) {
    Map<Integer, Integer> index = new HashMap<>(), low = new HashMap<>();
    Deque<Integer> stack = new ArrayDeque<>();
    Set<Integer> onStack = new HashSet<>();
    List<Set<Integer>> result = new ArrayList<>();
    List<Integer> sorted = nodes.stream().sorted(Comparator.comparingInt(order::get)).toList();
    for (int start : sorted) {
      if (index.containsKey(start))
        continue;
      Deque<int[]> work = new ArrayDeque<>();
      work.push(new int[]{start, 0});
      while (!work.isEmpty()) {
        int[] frame = work.peek();
        int node = frame[0];
        if (frame[1] == 0) {
          index.put(node, index.size());
          low.put(node, index.get(node));
          stack.push(node);
          onStack.add(node);
        }
        List<Integer> next = successors.getOrDefault(node, Set.of()).stream().filter(nodes::contains).toList();
        if (frame[1] < next.size()) {
          int to = next.get(frame[1]++);
          if (!index.containsKey(to))
            work.push(new int[]{to, 0});
          else if (onStack.contains(to))
            low.put(node, Math.min(low.get(node), index.get(to)));
          continue;
        }
        work.pop();
        if (!work.isEmpty())
          low.put(work.peek()[0], Math.min(low.get(work.peek()[0]), low.get(node)));
        if (low.get(node).equals(index.get(node))) {
          Set<Integer> component = new LinkedHashSet<>();
          int member;
          do {
            member = stack.pop();
            onStack.remove(member);
            component.add(member);
          } while (member != node);
          result.add(component);
        }
      }
    }
    return result;
  }

  public Map<Integer, Region> regions() {
    return regions;
  }

  /** The header a jump from {@code from} to {@code to} has to go through, or -1 when it can go straight there. */
  public int headerFor(int from, int to) {
    Region region = regionOfEntry.get(to);
    return region == null || region.nodes().contains(from) ? -1 : region.header();
  }
}
