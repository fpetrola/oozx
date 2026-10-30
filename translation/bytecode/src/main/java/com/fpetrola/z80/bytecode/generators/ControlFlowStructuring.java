package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.routines.RoutineManager.Outlined.Continuation;
import com.fpetrola.z80.transformations.StackAnalyzer;
import org.apache.commons.collections4.MultiValuedMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loops a routine enters in more than one place ("No More Gotos", Yakdan et al., NDSS 2015): each such region keeps one header, and every
 * other entry is reached by going to the header with the structuring variable (nextAddress) set to it, the header dispatching on it.
 */
public class ControlFlowStructuring {
  private final Map<Integer, Set<Integer>> successors = new HashMap<>();
  /** Header of each region entered in more than one place, with the other entries and the nodes inside; the regions each entry belongs to, outermost first. */
  private final Map<Integer, Region> regions = new HashMap<>();
  private final Map<Integer, List<Region>> regionsOfEntry = new HashMap<>();

  public record Region(int header, Set<Integer> entries, Set<Integer> nodes) {}

  private final Set<Integer> nodes = new LinkedHashSet<>(), leaving = new HashSet<>();
  private final Map<Integer, Routine> parts = new TreeMap<>();
  private final List<Integer> roots = new ArrayList<>();
  /** Where each node goes on outside the routine, and where the calls in it come back to when a return unwinds to this routine. */
  private final Map<Integer, Set<Integer>> outside = new HashMap<>();
  private final MultiValuedMap<Integer, Integer> catchPoints;
  private final RoutineManager routineManager;
  private final int entry;

  /** The nodes go on where {@link RoutineManager#goesOnAt} says, and at the catch points of their calls; the parts of the routine, and those of its parent, are nodes of their own that go on where the part does. */
  public ControlFlowStructuring(Routine routine, RoutineManager routineManager, Collection<Integer> entries, StackAnalyzer stackAnalyzer) {
    this.routineManager = routineManager;
    entry = routine.getEntryPoint();
    catchPoints = routineManager.catchPointsOfCallsIn(routine);
    RoutineManager.Outlined mine = routineManager.outlined.get(routine);
    routineManager.outlined.forEach((piece, part) -> {
      if (piece != routine && (part.parent() == routine || mine != null && part.parent() == mine.parent()))
        parts.put(piece.getEntryPoint(), piece);
    });
    for (int address = routine.getStartAddress(); address <= routine.getEndAddress(); address++)
      if (routine.contains(address) && routineManager.getInstructionAt(address) != null)
        nodes.add(address);
    nodes.addAll(parts.keySet());
    for (int node : nodes) {
      Set<Integer> next = new LinkedHashSet<>();
      RoutineManager.Outlined part = parts.containsKey(node) ? routineManager.outlined.get(parts.get(node)) : null;
      if (part != null) {
        part.exits().forEach(exit -> next.add(exit.to()));
        if (part.leaves())
          leaving.add(node);
      } else {
        next.addAll(routineManager.goesOnAt(node, routine, stackAnalyzer, false));
        next.addAll(catchPoints.get(node));
      }
      next.stream().filter(to -> !nodes.contains(to)).forEach(to -> outside.computeIfAbsent(node, k -> new LinkedHashSet<>()).add(to));
      successors.put(node, next.stream().map(this::nodeOf).filter(nodes::contains).collect(Collectors.toCollection(LinkedHashSet::new)));
    }
    roots.add(routine.getEntryPoint());
    entries.stream().filter(nodes::contains).filter(at -> at != routine.getEntryPoint()).sorted().forEach(roots::add);
    structure(new LinkedHashSet<>(nodes), roots, new HashSet<>(roots));
  }

  /** A part of the routine to take out as a called method: entered only at {@code entry}, it goes on at one of {@code exits} outside it, in the routine or not (its method says which when there is more than one outcome), and {@code leaves} says whether it can end the routine instead with a RET. */
  public record Outline(int entry, Set<Integer> nodes, List<Continuation> exits, boolean leaves) {}

  private static final int LEAST_PART_BYTES = 64;

  /** Parts to take out as called methods until at most {@code keep} bytes of the routine remain: each time the biggest region of at most {@code keep} bytes that one address dominates, so it is entered only there and what remains is a sequence of calls; where a chosen part goes on counts as entered from the parent, which dispatches it. */
  public List<Outline> outlines(int keep) {
    List<Outline> chosen = new ArrayList<>();
    Set<Integer> remaining = new LinkedHashSet<>(nodes);
    while (bytes(remaining) > keep) {
      List<Integer> dispatched = Stream.concat(roots.stream(), chosen.stream().flatMap(part -> part.exits().stream().map(Continuation::to))).distinct().filter(remaining::contains).toList();
      Map<Integer, Integer> idom = immediateDominators(remaining, dispatched);
      Map<Integer, List<Integer>> children = new HashMap<>();
      idom.forEach((node, dominator) -> children.computeIfAbsent(dominator, k -> new ArrayList<>()).add(node));
      Map<Integer, Integer> dominatedBytes = dominatedBytes(idom, children);
      Optional<Outline> best = remaining.stream().filter(entry -> !roots.contains(entry) && idom.containsKey(entry) && dominatedBytes.get(entry) <= keep && dominatedBytes.get(entry) >= LEAST_PART_BYTES)
          .sorted(Comparator.comparingInt(dominatedBytes::get).reversed())
          .map(entry -> outline(entry, dominated(entry, children))).flatMap(Optional::stream).findFirst();
      if (best.isEmpty())
        break;
      chosen.add(best.get());
      remaining.removeAll(best.get().nodes());
    }
    return chosen;
  }

  private Optional<Outline> outline(int entry, Set<Integer> region) {
    return region.stream().anyMatch(node -> node != entry && roots.contains(node)) ? Optional.empty() : Optional.of(exitsOf(entry, region));
  }

  /** The whole routine as a part: where it goes on outside itself, and whether it can end with a RET. */
  public Outline whole() {
    return exitsOf(entry, nodes);
  }

  /** The node an address is at: itself, or the part it is inside. */
  private int nodeOf(int address) {
    return nodes.contains(address) ? address : parts.entrySet().stream().filter(part -> part.getValue().contains(address)).mapToInt(Map.Entry::getKey).findFirst().orElse(address);
  }

  private Outline exitsOf(int entry, Set<Integer> region) {
    List<Continuation> exits = new ArrayList<>();
    boolean leaves = false;
    for (int node : region) {
      Set<Integer> next = successors.getOrDefault(node, Set.of());
      leaves |= next.isEmpty() || leaving.contains(node) || !parts.containsKey(node) && routineManager.getInstructionAt(node) instanceof Ret;
      Stream.of(next, outside.getOrDefault(node, Set.of())).flatMap(Collection::stream).filter(to -> !region.contains(nodeOf(to))).map(to -> new Continuation(to, node)).filter(exit -> !exits.contains(exit)).forEach(exits::add);
    }
    return new Outline(entry, region, exits, leaves);
  }

  private int bytes(Set<Integer> addresses) {
    return addresses.stream().mapToInt(this::bytesOf).sum();
  }

  private int bytesOf(int node) {
    return parts.containsKey(node) ? 0 : routineManager.getInstructionAt(node).getLength();
  }

  /** The bytes each node's subtree of the dominator tree adds up to. */
  private Map<Integer, Integer> dominatedBytes(Map<Integer, Integer> idom, Map<Integer, List<Integer>> children) {
    Deque<Integer> pending = new ArrayDeque<>(children.getOrDefault(-1, List.of())), innerFirst = new ArrayDeque<>();
    while (!pending.isEmpty()) {
      int node = pending.pop();
      innerFirst.push(node);
      pending.addAll(children.getOrDefault(node, List.of()));
    }
    Map<Integer, Integer> total = new HashMap<>();
    for (int node : innerFirst)
      total.merge(idom.get(node), total.merge(node, bytesOf(node), Integer::sum), Integer::sum);
    return total;
  }

  /** The immediate dominator of each node reachable in the subgraph from these entries (Cooper, Harvey and Kennedy); the entries hang from a virtual root, -1. */
  private Map<Integer, Integer> immediateDominators(Set<Integer> subgraph, List<Integer> entries) {
    Map<Integer, Set<Integer>> predecessors = new HashMap<>();
    subgraph.forEach(node -> successors.getOrDefault(node, Set.of()).stream().filter(subgraph::contains).forEach(to -> predecessors.computeIfAbsent(to, k -> new HashSet<>()).add(node)));
    List<Integer> order = new ArrayList<>(preorder(subgraph, entries).entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList());
    Map<Integer, Integer> position = new HashMap<>(), idom = new HashMap<>();
    for (int i = 0; i < order.size(); i++)
      position.put(order.get(i), i);
    position.put(-1, -1);
    entries.forEach(root -> idom.put(root, -1));
    for (boolean changed = true; changed; ) {
      changed = false;
      for (int node : order) {
        if (entries.contains(node))
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
        region.entries().forEach(entry -> regionsOfEntry.computeIfAbsent(entry, k -> new ArrayList<>()).add(region));
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

  /** The header a jump from {@code from} to {@code to} has to go through, or -1 when it can go straight there: that of the outermost region around {@code to} that {@code from} is not in; the header dispatches on to the next one in. */
  public int headerFor(int from, int to) {
    return regionsOfEntry.getOrDefault(to, List.of()).stream().filter(region -> !region.nodes().contains(from)).mapToInt(Region::header).findFirst().orElse(-1);
  }
}
