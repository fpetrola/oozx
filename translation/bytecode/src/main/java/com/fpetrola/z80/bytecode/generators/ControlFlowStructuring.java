package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;

import java.util.*;
import java.util.function.IntFunction;
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

  private final List<Integer> nodes = new ArrayList<>(), roots = new ArrayList<>();
  private final Set<Integer> jumpTargets = new HashSet<>(), jumpsOut = new HashSet<>();
  private final RoutineManager routineManager;

  /** {@code continuation}: where the code goes on after the instruction at an address that falls through; {@code dynamicTargets}: where a jump through a register or a RET used as a jump goes. */
  public ControlFlowStructuring(Routine routine, RoutineManager routineManager, Collection<Integer> entries, IntUnaryOperator continuation, IntFunction<Set<Integer>> dynamicTargets) {
    this.routineManager = routineManager;
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
      if (instruction instanceof Ret || instruction instanceof ConditionalInstruction<?> jump && !(jump instanceof Call) && jump.getPositionOpcodeReference() instanceof Register)
        next.addAll(dynamicTargets.apply(node));
      if (next.stream().anyMatch(to -> !successors.containsKey(to) && !nodes.contains(to)))
        jumpsOut.add(node);
      next.removeIf(to -> !successors.containsKey(to) && !nodes.contains(to));
      next.stream().filter(to -> !RoutineManager.fallsThrough(instruction) || to != continuation.applyAsInt(node)).forEach(jumpTargets::add);
    }
    roots.add(routine.getEntryPoint());
    entries.stream().filter(nodes::contains).filter(at -> at != routine.getEntryPoint()).sorted().forEach(roots::add);
    structure(new LinkedHashSet<>(nodes), roots, new HashSet<>(roots));
  }

  /** A part of the routine to take out as a called method: entered only at {@code entry}, it comes back to {@code continuation} in what remains (-1: never), and {@code leaves} says whether it can also end the routine with a RET; a part never jumps out of the routine. */
  public record Outline(int entry, Set<Integer> nodes, int continuation, boolean leaves) {}

  private static final int LEAST_PART_BYTES = 64;

  /** Parts to take out as called methods until at most {@code keep} bytes of the routine remain: each starts at a jump target and comes back to one point of what remains, directly or through another part with the same continuation; a tail that only ends the routine is not a part. Among the parts worth a method, the innermost go first, the biggest of them, so the routine keeps its own shape. */
  public List<Outline> outlines(int keep) {
    List<Outline> chosen = new ArrayList<>();
    Set<Integer> remaining = new LinkedHashSet<>(nodes);
    while (bytes(remaining) > keep) {
      Map<Integer, Integer> idom = immediateDominators(remaining);
      Map<Integer, List<Integer>> children = new HashMap<>();
      idom.forEach((node, dominator) -> children.computeIfAbsent(dominator, k -> new ArrayList<>()).add(node));
      List<Outline> candidates = remaining.stream().filter(entry -> jumpTargets.contains(entry) && !roots.contains(entry) && idom.containsKey(entry))
          .map(entry -> outline(entry, dominated(entry, children), remaining, chosen))
          .flatMap(Optional::stream).toList();
      List<Outline> worthAMethod = candidates.stream().filter(part -> bytes(part.nodes()) >= LEAST_PART_BYTES).toList();
      Optional<Outline> best = worthAMethod.stream().filter(part -> worthAMethod.stream().noneMatch(other -> other != part && part.nodes().contains(other.entry()))).max(Comparator.comparingInt(part -> bytes(part.nodes())));
      if (best.isEmpty())
        break;
      chosen.add(best.get());
      remaining.removeAll(best.get().nodes());
    }
    return chosen;
  }

  private Optional<Outline> outline(int entry, Set<Integer> region, Set<Integer> remaining, List<Outline> chosen) {
    if (region.stream().anyMatch(node -> node != entry && roots.contains(node) || jumpsOut.contains(node)))
      return Optional.empty();
    int continuation = -1;
    boolean leaves = false;
    for (int node : region) {
      Set<Integer> next = successors.getOrDefault(node, Set.of());
      leaves |= next.isEmpty();
      for (int to : next) {
        if (region.contains(to))
          continue;
        int goesOn = chosen.stream().filter(part -> part.entry() == to).mapToInt(Outline::continuation).findFirst().orElse(to);
        if (goesOn != -1 && continuation != -1 && goesOn != continuation)
          return Optional.empty();
        continuation = goesOn != -1 ? goesOn : continuation;
      }
    }
    return continuation == -1 ? Optional.empty() : Optional.of(new Outline(entry, region, continuation, leaves));
  }

  private int bytes(Set<Integer> addresses) {
    return addresses.stream().mapToInt(address -> routineManager.getInstructionAt(address).getLength()).sum();
  }

  /** The immediate dominator of each node reachable in the subgraph from its roots (Cooper, Harvey and Kennedy); the roots hang from a virtual one, -1. */
  private Map<Integer, Integer> immediateDominators(Set<Integer> subgraph) {
    Map<Integer, Set<Integer>> predecessors = new HashMap<>();
    subgraph.forEach(node -> successors.getOrDefault(node, Set.of()).stream().filter(subgraph::contains).forEach(to -> predecessors.computeIfAbsent(to, k -> new HashSet<>()).add(node)));
    List<Integer> order = new ArrayList<>(preorder(subgraph, roots.stream().filter(subgraph::contains).toList()).entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList());
    Map<Integer, Integer> position = new HashMap<>(), idom = new HashMap<>();
    for (int i = 0; i < order.size(); i++)
      position.put(order.get(i), i);
    position.put(-1, -1);
    roots.stream().filter(subgraph::contains).forEach(root -> idom.put(root, -1));
    for (boolean changed = true; changed; ) {
      changed = false;
      for (int node : order) {
        if (roots.contains(node))
          continue;
        int dominator = -2;
        for (int from : predecessors.getOrDefault(node, Set.of()))
          if (idom.containsKey(from)) {
            int a = from, b = dominator == -2 ? from : dominator;
            while (a != b)
              if (position.get(a) > position.get(b))
                a = idom.get(a);
              else
                b = idom.get(b);
            dominator = a;
          }
        if (dominator != -2 && !Integer.valueOf(dominator).equals(idom.put(node, dominator)))
          changed = true;
      }
    }
    return idom;
  }

  /** The nodes the entry dominates: its subtree in the dominator tree. */
  private static Set<Integer> dominated(int entry, Map<Integer, List<Integer>> children) {
    Set<Integer> subtree = new LinkedHashSet<>();
    Deque<Integer> pending = new ArrayDeque<>(List.of(entry));
    while (!pending.isEmpty()) {
      int node = pending.pop();
      if (subtree.add(node))
        pending.addAll(children.getOrDefault(node, List.of()));
    }
    return subtree;
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
