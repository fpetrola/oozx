/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.z80.analysis;

import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * F5 (targeted) — the "track" command: end-to-end automatic sprite position extraction.
 * No step needs human interpretation:
 * <ol>
 *   <li>{@link CoordinateFinder} derives draw methods + coordinate-cell candidates from
 *       the aggregate DB (runs the aggregate pass first if the DB is missing);</li>
 *   <li>the RZX is re-run with {@link TrackLog} recording, per instance, every draw
 *       write, every draw-routine entry and every watched-cell change, in order;</li>
 *   <li>the offline replay clusters writes per routine invocation and decodes each
 *       cluster's top-left (x, y) from the ZX screen layout (backbuffers included via
 *       the region deltas) — table {@code sprite_draws};</li>
 *   <li>an affine-voting correlator decides which watched cell is the X / Y of which
 *       sprite ({@code coord_cells}), and groups them into strided record tables
 *       ({@code coord_tables}). Per-frame cell values land in {@code frame_cells}.</li>
 * </ol>
 * The run self-verifies: its per-frame memory hashes must be IDENTICAL to the aggregate
 * run's.
 */
public class SpriteTracker {

  /** aggregate capture (inherited) + targeted per-instance logging. */
  static class TrackRunner extends RZXAnalysisRunner {
    TrackRunner(RZXPlayerIO<WordNumber> io, Predicate<Integer> cond, String rzxPath) {
      super(io, cond, rzxPath);
    }

    @Override
    public void wMem(int address, int value, int pc) {
      super.wMem(address, value, pc);
      if (TrackLog.writeSites[pc & 0xFFFF])
        TrackLog.write(pc, address);
    }

    @Override
    public int mem(int address, int pc) {
      int v = super.mem(address, pc);
      if (TrackLog.readSites[pc & 0xFFFF])
        TrackLog.read(pc, address);
      return v;
    }

    @Override
    public void pc(int address, int rdelta) {
      super.pc(address, rdelta);
      if (address >= 0) {
        if (TrackLog.entrySites[address])
          TrackLog.entry(address, mem);
        TrackLog.pcHash(address);
      }
    }
  }

  /**
   * one routine invocation that touched the screen: its top-left position and size, the
   * graphics source it read (sprite identity) and its execution-path signature.
   */
  public record Draw(int frame, int methodEntry, char kind, int x, int y, int w, int h,
                     int nWrites, int buffer, int gfx, int gfxHi, int path, byte[] cells) {
  }

  public record CoordMatch(int addr, char axis, String transform, int off,
                           int matched, int frames, double rate) {
  }

  public record CoordTable(int stride, int slots, int x0, String xTransform, int xOff,
                           int y0, String yTransform, int yOff) {
  }

  /** two neighbour cells whose values, at the same instant, place THE SAME cluster. */
  public record CoordPair(int xAddr, String xTransform, int xOff, int yAddr, String yTransform, int yOff,
                          int joint, int frames, double rate) {
  }

  public record Correlation(List<CoordMatch> matches, List<CoordPair> pairs) {
  }

  static final String[] TRANSFORMS = {"v", "v>>1", "v<<1", "(v&31)*8", "(v&15)*8", "v&248", "(v&240)>>1"};

  public static int applyTransform(String name, int v) {
    return switch (name) {
      case "v" -> v;
      case "v>>1" -> v >> 1;
      case "v<<1" -> (v << 1) & 0x1FF;
      case "(v&31)*8" -> (v & 31) << 3;
      case "(v&15)*8" -> (v & 15) << 3;
      case "v&248" -> v & 248;
      case "(v&240)>>1" -> (v & 240) >> 1;
      default -> throw new IllegalArgumentException(name);
    };
  }

  public static void run(String dbPath, String rzxPath) throws Exception {
    System.setProperty("minizx.headless", "true");
    if (!Files.exists(Path.of(dbPath))) {
      System.out.println("No existe " + dbPath + ": corriendo primero la pasada de agregados...\n");
      RZXAnalysisRunner.runAggregate(rzxPath);
    }
    AnalysisDB db = new AnalysisDB(dbPath);
    CoordinateFinder.Plan plan = new CoordinateFinder(db).find();
    printPlan(plan);

    TrackLog.reset();
    TrackLog.configure(plan.watchCells());
    for (int s : plan.drawWriteSites())
      TrackLog.writeSites[s] = true;
    for (int e : plan.drawMethods().keySet())
      if (e >= 0)
        TrackLog.entrySites[e] = true;
    // sprite identity: log the reads over the static graphics zones (VAL side)
    Explainer explainer = new Explainer(db, dbPath);
    Set<Integer> valReads = GameMapper.roleReads(db, plan, "VAL");
    List<int[]> gfxRegions = GameMapper.mergeRanges(valReads.stream()
        .map(db.reads::get)
        .filter(r -> {
          String c = explainer.classifyRange(r.addrMin(), r.addrMax());
          return c.startsWith("ESTATICA") || c.startsWith("mayormente") || c.startsWith("MIXTA");
        })
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 64)
        .stream().filter(g -> g[1] - g[0] + 1 >= 1024).toList();
    int gfxSites = 0;
    for (int s : valReads) {
      AnalysisDB.Stat r = db.reads.get(s);
      // a site that always reads the same single cell is a lookup byte, not sprite data
      // (sprite reads sweep a range across invocations)
      if (r.addrMax() > r.addrMin()
          && gfxRegions.stream().anyMatch(g -> r.addrMax() >= g[0] && r.addrMin() <= g[1])) {
        TrackLog.readSites[s] = true;
        gfxSites++;
      }
    }
    System.out.print("Zonas de graficos a identificar por dibujo (" + gfxSites + " read-sites): ");
    gfxRegions.forEach(g -> System.out.print("[" + g[0] + ".." + g[1] + "] "));
    System.out.println();

    System.out.println("\n=== Re-corrida con tracking dirigido ===");
    RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
    TrackRunner game = new TrackRunner(io, io.getInterruptionCondition(), rzxPath);
    long start = System.currentTimeMillis();
    try {
      game.$34463();
    } catch (RuntimeException e) {
      System.out.println("Run ended: " + e.getMessage());
    }
    System.out.println("Track run: " + (System.currentTimeMillis() - start) / 1000 + "s, "
        + TrackLog.size() + " eventos");
    TrackLog.enabled = false;

    game.bootstrap.hasher.dump("analysis/track-hashes.txt");
    if (Files.exists(Path.of("analysis/instrumented-hashes.txt")))
      FrameHasher.compare("analysis/instrumented-hashes.txt", "analysis/track-hashes.txt");

    List<Draw> draws = cluster(plan);
    long distinctGfx = draws.stream().mapToInt(Draw::gfx).filter(g -> g >= 0).distinct().count();
    System.out.println(draws.size() + " dibujados (clusters) reconstruidos, "
        + distinctGfx + " origenes graficos distintos (identidad de sprite)");

    try {
      System.out.println("correlacionando...");
      Correlation corr = correlate(plan, draws);
      List<CoordTable> tables = detectTables(plan, corr.matches());
      System.out.println("volcando tablas...");
      dumpTables(dbPath, draws, corr, tables);
      spritesFound(draws, plan.drawMethods(), dbPath);
      report(corr, tables);
      System.out.println("analizando episodios...");
      episodes(db, plan, corr, draws, dbPath);
    } catch (Throwable t) {
      System.out.println("FALLO en el analisis post-corrida: " + t);
      t.printStackTrace(System.out);
      throw t;
    }

    // muestra automática: un frame con varios sprites, ya anotado
    draws.stream().filter(d -> d.kind() == 'P')
        .collect(java.util.stream.Collectors.groupingBy(Draw::frame, TreeMap::new, java.util.stream.Collectors.counting()))
        .entrySet().stream().filter(e -> e.getValue() >= 3).skip(200).findFirst()
        .ifPresent(e -> {
          try {
            System.out.println("\n=== Muestra (frame " + e.getKey() + ") ===");
            PositionReport.print(dbPath, e.getKey(), e.getKey());
          } catch (SQLException ex) {
            System.out.println("muestra fallida: " + ex);
          }
        });
  }

  private static void printPlan(CoordinateFinder.Plan plan) {
    System.out.println("=== Plan automático (derivado de la DB de agregados) ===");
    System.out.println("Regiones tipo-pantalla:");
    for (CoordinateFinder.Region r : plan.regions())
      System.out.printf("  [%d..%d] delta %+d%n", r.lo(), r.hi(), r.delta());
    System.out.println("Métodos de dibujado: " + String.join(" ", plan.drawMethods().values())
        + "  (" + plan.drawWriteSites().size() + " write-sites)");
    System.out.print("Celdas observadas (" + plan.watchCells().length + "): ");
    plan.watchRanges().forEach(rg -> System.out.print("[" + rg[0] + ".." + rg[1] + "] "));
    System.out.println();
    System.out.print("Tablas de consulta estáticas: ");
    plan.lookupTables().forEach(rg -> System.out.print("[" + rg[0] + ".." + rg[1] + "] "));
    System.out.println();
  }

  // ==================== clustering ====================

  private static final class Cluster {
    final int frame, methodEntry;
    final byte[] cells;
    final int[] minX = {Integer.MAX_VALUE, Integer.MAX_VALUE}, minY = {Integer.MAX_VALUE, Integer.MAX_VALUE};
    final int[] maxX = {Integer.MIN_VALUE, Integer.MIN_VALUE}, maxY = {Integer.MIN_VALUE, Integer.MIN_VALUE};
    final int[] n = {0, 0}, buffer = {-1, -1};
    int gfxMin = Integer.MAX_VALUE, gfxMax = -1;
    int path;

    Cluster(int frame, int methodEntry, byte[] cells) {
      this.frame = frame;
      this.methodEntry = methodEntry;
      this.cells = cells;
    }

    void add(int kindIdx, int x, int y, int regionLo) {
      if (x < minX[kindIdx]) minX[kindIdx] = x;
      if (x > maxX[kindIdx]) maxX[kindIdx] = x;
      if (y < minY[kindIdx]) minY[kindIdx] = y;
      if (y > maxY[kindIdx]) maxY[kindIdx] = y;
      n[kindIdx]++;
      buffer[kindIdx] = regionLo;
    }

    void close(List<Draw> out) {
      for (int k = 0; k < 2; k++)
        if (n[k] > 0)
          out.add(new Draw(frame, methodEntry, k == 0 ? 'P' : 'A', minX[k], minY[k],
              maxX[k] - minX[k] + 8, maxY[k] - minY[k] + (k == 0 ? 1 : 8), n[k], buffer[k],
              gfxMin == Integer.MAX_VALUE ? -1 : gfxMin, gfxMax, path, cells));
    }
  }

  /** replays the event log: cell values evolve, entries open clusters, writes decode to (x, y). */
  static List<Draw> cluster(CoordinateFinder.Plan plan) {
    int[] cells = plan.watchCells();
    Map<Integer, Integer> cellIdx = new HashMap<>();
    for (int i = 0; i < cells.length; i++)
      cellIdx.put(cells[i], i);
    List<CoordinateFinder.Region> regions = plan.regions();
    Map<Integer, Integer> siteEntry = plan.siteMethodEntry();

    int[] vals = new int[cells.length];
    List<Draw> out = new ArrayList<>();
    Cluster cur = null;
    // snapshots are SHARED between clusters until a cell changes: with millions of
    // clusters per run, copying 170 bytes per cluster would not fit in memory
    byte[] curSnap = snapshot(vals);
    boolean dirty = false;
    for (int i = 0; i < TrackLog.size(); i++) {
      long ev = TrackLog.event(i);
      int frame = TrackLog.frame(ev), a = TrackLog.a(ev), b = TrackLog.b(ev);
      switch (TrackLog.type(ev)) {
        case TrackLog.EV_CELL -> {
          Integer ci = cellIdx.get(a);
          if (ci != null) {
            vals[ci] = b;
            dirty = true;
          }
        }
        case TrackLog.EV_FRAME -> {
          if (cur != null)
            cur.close(out);
          cur = null;
        }
        case TrackLog.EV_ENTRY -> {
          if (cur != null)
            cur.close(out);
          if (dirty) {
            curSnap = snapshot(vals);
            dirty = false;
          }
          cur = new Cluster(frame, a, curSnap);
        }
        case TrackLog.EV_READ -> {
          if (cur != null) {
            if (b < cur.gfxMin)
              cur.gfxMin = b;
            if (b > cur.gfxMax)
              cur.gfxMax = b;
          }
        }
        case TrackLog.EV_PATH -> {
          if (cur != null)
            cur.path = (a << 16) | b;
        }
        case TrackLog.EV_WRITE -> {
          CoordinateFinder.Region reg = null;
          for (CoordinateFinder.Region r : regions)
            if (r.contains(b)) {
              reg = r;
              break;
            }
          if (reg == null)
            continue;
          int sa = b + reg.delta();
          int kindIdx, x, y;
          if (sa >= 16384 && sa <= 22527) {          // pixel area
            int off = sa - 16384;
            y = ((off >> 11) & 3) * 64 + ((off >> 5) & 7) * 8 + ((off >> 8) & 7);
            x = (off & 31) * 8;
            kindIdx = 0;
          } else if (sa >= 22528 && sa <= 23295) {   // attribute area
            int rel = sa - 22528;
            y = (rel >> 5) * 8;
            x = (rel & 31) * 8;
            kindIdx = 1;
          } else
            continue;
          if (cur == null || cur.frame != frame)  {
            if (cur != null)
              cur.close(out);
            if (dirty) {
              curSnap = snapshot(vals);
              dirty = false;
            }
            cur = new Cluster(frame, siteEntry.getOrDefault(a, -1), curSnap);
          }
          cur.add(kindIdx, x, y, reg.lo());
        }
      }
    }
    if (cur != null)
      cur.close(out);
    return out;
  }

  private static byte[] snapshot(int[] vals) {
    byte[] s = new byte[vals.length];
    for (int i = 0; i < vals.length; i++)
      s[i] = (byte) vals[i];
    return s;
  }

  // ==================== correlation ====================

  /**
   * affine voting: a cell is the X (or Y) of a sprite when, frame after frame, some
   * cluster of a method sits exactly at transform(cellValue) + constant. The constant is
   * learnt as the mode of the differences; support is measured in frames (several sprites
   * per frame belong to different cells, so per-cluster support would dilute).
   * <p>
   * Per-axis matches carry coincidences (with ~20 draws/frame and 32 columns, "some
   * cluster matched" is cheap), so the decisive validation is joint: an (X, Y) pair of
   * neighbour cells is real when THE SAME cluster satisfies both at once — chance level
   * for that is ~1%. Singles survive only with very high per-axis support (the player's
   * coordinates, always matched, would survive even without a neighbour).
   */
  static Correlation correlate(CoordinateFinder.Plan plan, List<Draw> draws) {
    int[] cells = plan.watchCells();
    Map<Integer, List<Draw>> byMethod = new TreeMap<>();
    for (Draw d : draws)
      byMethod.computeIfAbsent(d.methodEntry(), k -> new ArrayList<>()).add(d);
    // correlation is statistical: on very long runs a uniform sample per method keeps
    // the quadratic loops inside the time/memory budget without moving the rates
    for (Map.Entry<Integer, List<Draw>> me : byMethod.entrySet()) {
      List<Draw> ds = me.getValue();
      if (ds.size() > 400_000) {
        int k = (ds.size() + 399_999) / 400_000;
        List<Draw> sampled = new ArrayList<>(ds.size() / k + 1);
        for (int i = 0; i < ds.size(); i += k)
          sampled.add(ds.get(i));
        me.setValue(sampled);
      }
    }

    // prefilter: a coordinate must actually vary
    System.out.println("  prefiltro de celdas variables...");
    int[] distinct = new int[cells.length];
    {
      BitSet[] seen = new BitSet[cells.length];
      for (int i = 0; i < cells.length; i++)
        seen[i] = new BitSet(256);
      for (Draw d : draws)
        for (int i = 0; i < cells.length; i++)
          seen[i].set(d.cells()[i] & 0xFF);
      for (int i = 0; i < cells.length; i++)
        distinct[i] = seen[i].cardinality();
    }

    Map<Integer, Integer> framesByMethod = new HashMap<>();
    for (Map.Entry<Integer, List<Draw>> me : byMethod.entrySet()) {
      int n = 0, lastF = -1;
      for (Draw d : me.getValue())
        if (d.frame() != lastF) {
          n++;
          lastF = d.frame();
        }
      framesByMethod.put(me.getKey(), n);
    }

    // (cellIdx, axis) -> top candidates by matched count; the joint stage tries the
    // combinations, so a slot whose best single-axis transform is a lucky mask still
    // gets validated through its true transform
    Map<Long, List<CoordMatch>> cand = new HashMap<>();
    for (Map.Entry<Integer, List<Draw>> me : byMethod.entrySet()) {
      List<Draw> ds = me.getValue();
      int framesWithDraws = framesByMethod.get(me.getKey());
      if (framesWithDraws < 50)
        continue;
      System.out.println("  votacion $" + me.getKey() + " (" + ds.size() + " draws)...");
      for (int ci = 0; ci < cells.length; ci++) {
        if (distinct[ci] < 6)
          continue;
        for (char axis : new char[]{'X', 'Y'}) {
          for (String t : TRANSFORMS) {
            int[] hist = new int[1024];
            for (Draw d : ds) {
              int v = d.cells()[ci] & 0xFF;
              int diff = (axis == 'X' ? d.x() : d.y()) - applyTransform(t, v) + 512;
              if (diff >= 0 && diff < 1024)
                hist[diff]++;
            }
            int mode = 0;
            for (int k = 1; k < 1024; k++)
              if (hist[k] > hist[mode])
                mode = k;
            if (hist[mode] < 50)
              continue;
            int off = mode - 512;
            int matched = 0, lastMatchedF = -1;
            BitSet matchedPos = new BitSet(512);
            for (Draw d : ds) {
              int v = d.cells()[ci] & 0xFF;
              int pos = axis == 'X' ? d.x() : d.y();
              if (pos - applyTransform(t, v) == off && d.frame() != lastMatchedF) {
                matched++;
                lastMatchedF = d.frame();
                matchedPos.set(pos & 511);
              }
            }
            // a real coordinate moves: coincidences (a masked transform hitting a fixed
            // floor row, say) only ever cover a handful of distinct positions
            if (matchedPos.cardinality() < 10)
              continue;
            if (matched < 200)
              continue;
            double rate = matched / (double) framesWithDraws;
            List<CoordMatch> top = cand.computeIfAbsent(((long) ci << 8) | axis, k -> new ArrayList<>());
            top.add(new CoordMatch(cells[ci], axis, t, off, matched, framesWithDraws, rate));
            top.sort(Comparator.comparingInt((CoordMatch m) -> -m.matched()));
            if (top.size() > 3)
              top.remove(3);
          }
        }
      }
    }
    // joint X-Y validation over neighbour candidate cells, trying transform combinations
    System.out.println("  validacion conjunta X-Y (" + cand.size() + " candidatos)...");
    record PairCand(CoordMatch mx, CoordMatch my, int joint, int frames) {
    }
    List<PairCand> rawPairs = new ArrayList<>();
    for (Map.Entry<Long, List<CoordMatch>> ex : cand.entrySet()) {
      if ((char) (ex.getKey() & 0xFF) != 'X')
        continue;
      int cxi = (int) (ex.getKey() >> 8);
      for (Map.Entry<Long, List<CoordMatch>> ey : cand.entrySet()) {
        if ((char) (ey.getKey() & 0xFF) != 'Y')
          continue;
        int cyi = (int) (ey.getKey() >> 8);
        int ax = ex.getValue().get(0).addr(), ay = ey.getValue().get(0).addr();
        if (ax == ay || Math.abs(ax - ay) > 8)
          continue;
        PairCand bestPair = null;
        for (CoordMatch mx : ex.getValue())
          for (CoordMatch my : ey.getValue()) {
            int bestJoint = 0, bestFrames = 0;
            for (Map.Entry<Integer, List<Draw>> me : byMethod.entrySet()) {
              int fw = framesByMethod.get(me.getKey());
              if (fw < 50)
                continue;
              int joint = 0, lastF = -1;
              for (Draw d : me.getValue()) {
                int vx = d.cells()[cxi] & 0xFF, vy = d.cells()[cyi] & 0xFF;
                if (d.x() - applyTransform(mx.transform(), vx) == mx.off()
                    && d.y() - applyTransform(my.transform(), vy) == my.off()
                    && d.frame() != lastF) {
                  joint++;
                  lastF = d.frame();
                }
              }
              if (joint > bestJoint) {
                bestJoint = joint;
                bestFrames = fw;
              }
            }
            // chance level for a joint hit is ~1% per frame: low relative bar so slots
            // only active in a few rooms still show up
            if (bestJoint >= 120 && bestJoint >= 0.02 * bestFrames
                && (bestPair == null || bestJoint > bestPair.joint()))
              bestPair = new PairCand(mx, my, bestJoint, bestFrames);
          }
        if (bestPair != null)
          rawPairs.add(bestPair);
      }
    }

    // greedy exclusivity: a cell is one variable — it belongs to its highest-joint pair only
    rawPairs.sort(Comparator.comparingInt((PairCand p) -> -p.joint()));
    List<CoordPair> pairs = new ArrayList<>();
    List<CoordMatch> matches = new ArrayList<>();
    Set<Integer> usedCells = new HashSet<>();
    for (PairCand p : rawPairs) {
      if (usedCells.contains(p.mx().addr()) || usedCells.contains(p.my().addr()))
        continue;
      usedCells.add(p.mx().addr());
      usedCells.add(p.my().addr());
      pairs.add(new CoordPair(p.mx().addr(), p.mx().transform(), p.mx().off(),
          p.my().addr(), p.my().transform(), p.my().off(),
          p.joint(), p.frames(), p.joint() / (double) p.frames()));
      matches.add(p.mx());
      matches.add(p.my());
    }
    // singles: only overwhelming per-axis support survives without a validated neighbour
    for (List<CoordMatch> top : cand.values()) {
      CoordMatch m = top.get(0);
      if (m.rate() >= 0.60 && !usedCells.contains(m.addr()))
        matches.add(m);
    }
    matches.sort(Comparator.comparingInt((CoordMatch m) -> -m.matched()));
    return new Correlation(matches, pairs);
  }

  /**
   * groups matched cells of the same axis into strided record tables. Slots may have
   * settled on different (but compatible) transforms, so runs are detected on addresses
   * only and the majority transform of the members labels the table.
   */
  static List<CoordTable> detectTables(CoordinateFinder.Plan plan, List<CoordMatch> matches) {
    Map<Character, Map<Integer, CoordMatch>> byAxis = new TreeMap<>();
    for (CoordMatch m : matches)
      byAxis.computeIfAbsent(m.axis(), k -> new TreeMap<>()).putIfAbsent(m.addr(), m);

    record Run(char axis, String transform, int off, int base, int stride, int count) {
    }
    List<Run> runs = new ArrayList<>();
    for (Map.Entry<Character, Map<Integer, CoordMatch>> ge : byAxis.entrySet()) {
      Map<Integer, CoordMatch> g = ge.getValue();
      List<Integer> addrs = new ArrayList<>(g.keySet());
      Set<Integer> used = new HashSet<>();
      for (int addr : addrs) {
        if (used.contains(addr))
          continue;
        int bestLen = 1, bestStride = 0;
        for (int stride = 2; stride <= 32; stride++) {
          int len = 1;
          while (g.containsKey(addr + len * stride) && !used.contains(addr + len * stride))
            len++;
          if (len > bestLen) {
            bestLen = len;
            bestStride = stride;
          }
        }
        if (bestLen >= 3) {
          Map<String, Integer> votes = new HashMap<>();
          for (int k = 0; k < bestLen; k++) {
            used.add(addr + k * bestStride);
            CoordMatch m = g.get(addr + k * bestStride);
            votes.merge(m.transform() + "|" + m.off(), 1, Integer::sum);
          }
          String majority = Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey();
          String[] parts = majority.split("\\|");
          runs.add(new Run(ge.getKey(), parts[0], Integer.parseInt(parts[1]), addr, bestStride, bestLen));
        }
      }
    }

    List<CoordTable> tables = new ArrayList<>();
    for (Run rx : runs) {
      if (rx.axis() != 'X')
        continue;
      for (Run ry : runs) {
        if (ry.axis() != 'Y' || ry.stride() != rx.stride() || Math.abs(ry.base() - rx.base()) >= rx.stride())
          continue;
        tables.add(new CoordTable(rx.stride(), Math.min(rx.count(), ry.count()),
            rx.base(), rx.transform(), rx.off(), ry.base(), ry.transform(), ry.off()));
      }
    }
    return tables;
  }

  // ==================== output ====================

  static void dumpTables(String dbPath, List<Draw> draws, Correlation corr,
                         List<CoordTable> tables) throws SQLException {
    List<CoordMatch> matches = corr.matches();
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      c.setAutoCommit(false);
      try (Statement st = c.createStatement()) {
        for (String t : new String[]{"sprite_draws", "frame_cells", "coord_cells", "coord_tables",
            "coord_pairs", "episodes", "sprites_found"})
          st.execute("DROP TABLE IF EXISTS " + t);
        st.execute("CREATE TABLE sprite_draws(frame INT, method INT, kind TEXT, x INT, y INT," +
            " w INT, h INT, nwrites INT, buffer INT, gfx INT, gfx_hi INT, path INT)");
        st.execute("CREATE TABLE frame_cells(frame INT, addr INT, val INT)");
        st.execute("CREATE TABLE coord_cells(addr INT, axis TEXT, transform TEXT, off INT," +
            " matched INT, frames INT, rate REAL)");
        st.execute("CREATE TABLE coord_tables(stride INT, slots INT, x0 INT, x_transform TEXT," +
            " x_off INT, y0 INT, y_transform TEXT, y_off INT)");
        st.execute("CREATE TABLE coord_pairs(x_addr INT, x_transform TEXT, x_off INT," +
            " y_addr INT, y_transform TEXT, y_off INT, joint INT, frames INT, rate REAL)");
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO sprite_draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")) {
        int pending = 0;
        for (Draw d : draws) {
          ps.setInt(1, d.frame());
          ps.setInt(2, d.methodEntry());
          ps.setString(3, String.valueOf(d.kind()));
          ps.setInt(4, d.x());
          ps.setInt(5, d.y());
          ps.setInt(6, d.w());
          ps.setInt(7, d.h());
          ps.setInt(8, d.nWrites());
          ps.setInt(9, d.buffer());
          ps.setInt(10, d.gfx());
          ps.setInt(11, d.gfxHi());
          ps.setInt(12, d.path());
          ps.addBatch();
          if (++pending % 100_000 == 0)
            ps.executeBatch();
        }
        ps.executeBatch();
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO frame_cells VALUES(?,?,?)")) {
        int pending = 0;
        for (int i = 0; i < TrackLog.size(); i++) {
          long ev = TrackLog.event(i);
          if (TrackLog.type(ev) != TrackLog.EV_CELL)
            continue;
          ps.setInt(1, TrackLog.frame(ev));
          ps.setInt(2, TrackLog.a(ev));
          ps.setInt(3, TrackLog.b(ev));
          ps.addBatch();
          if (++pending % 100_000 == 0)
            ps.executeBatch();
        }
        ps.executeBatch();
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO coord_cells VALUES(?,?,?,?,?,?,?)")) {
        for (CoordMatch m : matches) {
          ps.setInt(1, m.addr());
          ps.setString(2, String.valueOf(m.axis()));
          ps.setString(3, m.transform());
          ps.setInt(4, m.off());
          ps.setInt(5, m.matched());
          ps.setInt(6, m.frames());
          ps.setDouble(7, m.rate());
          ps.addBatch();
        }
        ps.executeBatch();
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO coord_tables VALUES(?,?,?,?,?,?,?,?)")) {
        for (CoordTable t : tables) {
          ps.setInt(1, t.stride());
          ps.setInt(2, t.slots());
          ps.setInt(3, t.x0());
          ps.setString(4, t.xTransform());
          ps.setInt(5, t.xOff());
          ps.setInt(6, t.y0());
          ps.setString(7, t.yTransform());
          ps.setInt(8, t.yOff());
          ps.addBatch();
        }
        ps.executeBatch();
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO coord_pairs VALUES(?,?,?,?,?,?,?,?,?)")) {
        for (CoordPair p : corr.pairs()) {
          ps.setInt(1, p.xAddr());
          ps.setString(2, p.xTransform());
          ps.setInt(3, p.xOff());
          ps.setInt(4, p.yAddr());
          ps.setString(5, p.yTransform());
          ps.setInt(6, p.yOff());
          ps.setInt(7, p.joint());
          ps.setInt(8, p.frames());
          ps.setDouble(9, p.rate());
          ps.addBatch();
        }
        ps.executeBatch();
      }
      try (Statement st = c.createStatement()) {
        st.execute("CREATE INDEX i_draws_frame ON sprite_draws(frame)");
        st.execute("CREATE INDEX i_fcells_frame ON frame_cells(frame)");
      }
      c.commit();
    }
    System.out.println("Tablas sprite_draws / frame_cells / coord_cells / coord_pairs / coord_tables -> " + dbPath);
  }

  static void report(Correlation corr, List<CoordTable> tables) {
    List<CoordMatch> matches = corr.matches();
    System.out.println("\n=== Pares (X,Y) validados conjuntamente (mismo sprite, mismo instante) ===");
    corr.pairs().forEach(p ->
        System.out.printf("  X=mem[%d] (%s%+d)  Y=mem[%d] (%s%+d)  joint %d/%d frames (%.0f%%)%n",
            p.xAddr(), p.xTransform(), p.xOff(), p.yAddr(), p.yTransform(), p.yOff(),
            p.joint(), p.frames(), p.rate() * 100));
    System.out.println("\n=== Celdas-coordenada detectadas (correlación automática) ===");
    matches.stream().limit(40).forEach(m ->
        System.out.printf("  mem[%d] = %c  pos = %s%+d  match %d/%d frames (%.0f%%)%n",
            m.addr(), m.axis(), m.transform().replace("v", "mem[" + m.addr() + "]"), m.off(),
            m.matched(), m.frames(), m.rate() * 100));
    if (matches.size() > 40)
      System.out.println("  ... (" + (matches.size() - 40) + " más, ver tabla coord_cells)");
    for (CoordTable t : tables)
      System.out.printf("%nTABLA de registros: stride %d, %d slots -> X = mem[%d + %d*k] via %s%+d," +
              " Y = mem[%d + %d*k] via %s%+d%n",
          t.stride(), t.slots(), t.x0(), t.stride(), t.xTransform(), t.xOff(),
          t.y0(), t.stride(), t.yTransform(), t.yOff());
  }

  /**
   * per-invocation episode analysis: the same execution paths repeat massively (the data
   * universe fits in 32K), so a handful of (path, graphics, record-condition) episodes
   * explains the whole run. For draws matched to an entity record via the validated
   * pairs, derives the record byte/mask that selects each path (e.g. the type field
   * that distinguishes a vertical from a horizontal guardian).
   */
  static void episodes(AnalysisDB db, CoordinateFinder.Plan plan, Correlation corr,
                       List<Draw> draws, String dbPath) throws SQLException {
    // record geometry from the strong pairs + the copies that load the table
    List<CoordPair> strong = corr.pairs().stream().filter(p -> p.rate() >= 0.10)
        .sorted(Comparator.comparingInt(CoordPair::xAddr)).toList();
    int stride = 0;
    for (int i = 1; i < strong.size(); i++) {
      int diff = strong.get(i).xAddr() - strong.get(i - 1).xAddr();
      if (diff >= 2 && diff <= 32) {
        stride = diff;
        break;
      }
    }
    int xOffRec = 0;
    if (stride > 0) {
      int x0 = strong.get(0).xAddr(), tableBase = x0;
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi >= x0 && b.dstMin() <= x0)
          tableBase = Math.min(tableBase, b.dstMin());
      }
      xOffRec = (x0 - tableBase) % stride;
    }

    int[] cells = plan.watchCells();
    Map<Integer, Integer> cellIdx = new HashMap<>();
    for (int i = 0; i < cells.length; i++)
      cellIdx.put(cells[i], i);

    Map<Long, List<Draw>> groups = new HashMap<>();
    for (Draw d : draws)
      groups.computeIfAbsent(((long) d.methodEntry() << 32) | (d.path() & 0xFFFFFFFFL),
          k -> new ArrayList<>()).add(d);

    System.out.println("\n=== EPISODIOS: caminos de ejecucion por rutina (se repiten; pocos explican todo) ===");
    int[] masks = {255, 240, 15, 7, 3, 1};
    List<Object[]> dbRows = new ArrayList<>();
    for (Map.Entry<Integer, String> me : plan.drawMethods().entrySet()) {
      List<Map.Entry<Long, List<Draw>>> paths = groups.entrySet().stream()
          .filter(g -> (int) (g.getKey() >> 32) == me.getKey().intValue())
          .sorted(Comparator.comparingInt(g -> -g.getValue().size()))
          .toList();
      if (paths.isEmpty())
        continue;
      long total = paths.stream().mapToLong(g -> g.getValue().size()).sum();
      List<Map.Entry<Long, List<Draw>>> top = paths.stream()
          .filter(g -> g.getValue().size() >= 100).limit(8).toList();
      if (top.isEmpty())
        continue;
      System.out.println(me.getValue() + ": " + paths.size() + " caminos sobre " + total + " invocaciones");

      // per-path constants over the entity record (offset -> "&mask=val"); the record
      // of each draw is inferred here per path (a run-wide map would not fit in memory)
      List<Map<Integer, String>> consts = new ArrayList<>();
      for (Map.Entry<Long, List<Draw>> g : top) {
        List<Draw> ds = g.getValue();
        int[] recs = new int[ds.size()];
        for (int di = 0; di < ds.size(); di++) {
          Draw d = ds.get(di);
          recs[di] = -1;
          if (stride > 0)
            for (CoordPair p : strong) {
              Integer xi = cellIdx.get(p.xAddr()), yi = cellIdx.get(p.yAddr());
              if (xi == null || yi == null)
                continue;
              if (applyTransform(p.xTransform(), d.cells()[xi] & 0xFF) + p.xOff() == d.x()
                  && applyTransform(p.yTransform(), d.cells()[yi] & 0xFF) + p.yOff() == d.y()) {
                recs[di] = p.xAddr() - xOffRec;
                break;
              }
            }
        }
        Map<Integer, String> cs = new TreeMap<>();
        for (int o = 0; o < stride; o++)
          for (int mask : masks) {
            int val = -1, slotted = 0;
            boolean constant = true;
            for (int di = 0; di < ds.size(); di++) {
              if (recs[di] < 0)
                continue;
              Integer ci = cellIdx.get(recs[di] + o);
              if (ci == null) {
                constant = false;
                break;
              }
              int v = (ds.get(di).cells()[ci] & 0xFF) & mask;
              slotted++;
              if (val < 0)
                val = v;
              else if (v != val) {
                constant = false;
                break;
              }
            }
            if (constant && slotted >= 30 && val >= 0) {
              cs.put(o, "&" + mask + "=" + val);
              break;
            }
          }
        consts.add(cs);
      }
      // offsets whose constant DIFFERS between paths = the fields that select the path
      Set<Integer> discr = new TreeSet<>();
      for (int i = 0; i < consts.size(); i++)
        for (int j = i + 1; j < consts.size(); j++)
          for (int o : consts.get(i).keySet())
            if (consts.get(j).containsKey(o) && !consts.get(j).get(o).equals(consts.get(i).get(o)))
              discr.add(o);

      for (int i = 0; i < top.size(); i++) {
        List<Draw> ds = top.get(i).getValue();
        int gfxLo = Integer.MAX_VALUE, gfxHi = -1;
        for (Draw d : ds)
          if (d.gfx() >= 0) {
            gfxLo = Math.min(gfxLo, d.gfx());
            gfxHi = Math.max(gfxHi, d.gfx());
          }
        StringBuilder cond = new StringBuilder();
        for (int o : discr)
          if (consts.get(i).containsKey(o))
            cond.append(cond.length() > 0 ? " " : "").append("rec+").append(o).append(consts.get(i).get(o));
        System.out.printf("  camino %08x x%d%s%s%n", (int) (long) top.get(i).getKey(), ds.size(),
            gfxHi >= 0 ? "  gfx[" + gfxLo + ".." + gfxHi + "]" : "",
            cond.length() > 0 ? "  cuando " + cond : "");
        dbRows.add(new Object[]{me.getKey(), (int) (long) top.get(i).getKey(), ds.size(),
            gfxHi >= 0 ? gfxLo : -1, gfxHi, cond.toString()});
      }
      long rest = total - top.stream().mapToLong(g -> g.getValue().size()).sum();
      if (rest > 0)
        System.out.println("  ... y " + (paths.size() - top.size()) + " caminos menos frecuentes (x" + rest + ")");
    }

    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      c.setAutoCommit(false);
      try (Statement st = c.createStatement()) {
        st.execute("CREATE TABLE episodes(method INT, path INT, count INT, gfx_lo INT, gfx_hi INT, cond TEXT)");
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO episodes VALUES(?,?,?,?,?,?)")) {
        for (Object[] r : dbRows) {
          for (int i = 0; i < 5; i++)
            ps.setInt(i + 1, (int) r[i]);
          ps.setString(6, (String) r[5]);
          ps.addBatch();
        }
        ps.executeBatch();
      }
      c.commit();
    }
    System.out.println("Tabla episodes -> " + dbPath);
  }

  /**
   * groups the graphics addresses belonging to each sprite: the per-invocation read
   * intervals of the same (frame, method, x) coalesce when contiguous (Willy's 16
   * row-blits of 2 bytes become one 32-byte sprite), instances then aggregate by base
   * address, and bases falling inside another sprite's extent (partial reads when the
   * sprite is clipped at a screen edge) get absorbed into it. Result: table
   * {@code sprites_found(base, last, size, veces, frame_first, frame_last, methods)}.
   */
  static void spritesFound(List<Draw> draws, SortedMap<Integer, String> methodNames, String dbPath)
      throws SQLException {
    // grouping is streamed frame by frame (draws come in temporal order): a global map
    // keyed by (frame, method, x) would not fit in memory on a full-game run
    Map<Integer, long[]> byBase = new TreeMap<>(); // base -> {veces, modeHi, frameFirst, frameLast}
    Map<Integer, Map<Integer, Integer>> hiHist = new HashMap<>();
    Map<Integer, Set<Integer>> methodsOf = new HashMap<>();
    Map<Long, List<int[]>> frameBuf = new HashMap<>(); // (method<<16|x) -> intervals of this frame
    int curFrame = -1;
    for (Draw d : draws) {
      if (d.gfx() < 0)
        continue;
      if (d.frame() != curFrame) {
        flushSpriteFrame(frameBuf, curFrame, byBase, hiHist, methodsOf);
        curFrame = d.frame();
      }
      frameBuf.computeIfAbsent(((long) d.methodEntry() << 16) | d.x(), k -> new ArrayList<>())
          .add(new int[]{d.gfx(), d.gfxHi(), d.methodEntry()});
    }
    flushSpriteFrame(frameBuf, curFrame, byBase, hiHist, methodsOf);

    // per base: the sprite's extent is the MODE of the observed ends, not the max —
    // a rare fusion of two neighbours must not poison the whole sprite
    byBase.forEach((base, agg) -> agg[1] = hiHist.get(base).entrySet().stream()
        .max(Map.Entry.comparingByValue()).get().getKey());
    // absorb only what looks like a CLIPPED read of the covering sprite: it starts inside
    // it and ends exactly at its end (a neighbour sprite ends elsewhere and stays apart)
    Integer prev = null;
    for (int base : new ArrayList<>(byBase.keySet())) {
      if (prev != null && base <= byBase.get(prev)[1] && byBase.get(base)[1] <= byBase.get(prev)[1]) {
        long[] into = byBase.get(prev), from = byBase.remove(base);
        into[0] += from[0];
        into[2] = Math.min(into[2], from[2]);
        into[3] = Math.max(into[3], from[3]);
        methodsOf.get(prev).addAll(methodsOf.remove(base));
      } else
        prev = base;
    }

    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      c.setAutoCommit(false);
      try (Statement st = c.createStatement()) {
        st.execute("CREATE TABLE sprites_found(base INT, last INT, size INT, veces INT," +
            " frame_first INT, frame_last INT, methods TEXT)");
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO sprites_found VALUES(?,?,?,?,?,?,?)")) {
        for (Map.Entry<Integer, long[]> e : byBase.entrySet()) {
          long[] a = e.getValue();
          ps.setInt(1, e.getKey());
          ps.setInt(2, (int) a[1]);
          ps.setInt(3, (int) a[1] - e.getKey() + 1);
          ps.setInt(4, (int) a[0]);
          ps.setInt(5, (int) a[2]);
          ps.setInt(6, (int) a[3]);
          ps.setString(7, methodsOf.get(e.getKey()).stream()
              .map(m -> methodNames.getOrDefault(m, "$" + m))
              .reduce((x, y) -> x + " " + y).orElse(""));
          ps.addBatch();
        }
        ps.executeBatch();
      }
      c.commit();
    }
    System.out.println("\n=== SPRITES ENCONTRADOS: " + byBase.size() + " (tabla sprites_found) ===");
    byBase.entrySet().stream()
        .sorted(Comparator.comparingLong(e -> -e.getValue()[0]))
        .limit(12)
        .forEach(e -> System.out.printf("  sprite [%d..%d] (%d bytes) x%d  frames %d..%d  por %s%n",
            e.getKey(), e.getValue()[1], e.getValue()[1] - e.getKey() + 1, e.getValue()[0],
            e.getValue()[2], e.getValue()[3], methodsOf.get(e.getKey()).stream()
                .map(m -> methodNames.getOrDefault(m, "$" + m))
                .reduce((x, y) -> x + " " + y).orElse("")));
  }

  /**
   * one frame's intervals: coalesce ONLY small fragments (row-by-row blits like Willy's
   * 16 stripes of 2 bytes) — two complete neighbour sprites must never fuse — and
   * aggregate the resulting instances by base address.
   */
  private static void flushSpriteFrame(Map<Long, List<int[]>> frameBuf, int frame,
                                       Map<Integer, long[]> byBase,
                                       Map<Integer, Map<Integer, Integer>> hiHist,
                                       Map<Integer, Set<Integer>> methodsOf) {
    for (List<int[]> list : frameBuf.values()) {
      list.sort(Comparator.comparingInt(a -> a[0]));
      int lo = list.get(0)[0], hi = list.get(0)[1], method = list.get(0)[2];
      boolean fragments = hi - lo + 1 <= 4;
      for (int i = 1; i < list.size(); i++) {
        int[] r = list.get(i);
        if (fragments && r[0] <= hi + 2 && r[1] - r[0] + 1 <= 4)
          hi = Math.max(hi, r[1]);
        else {
          spriteInstance(lo, hi, method, frame, byBase, hiHist, methodsOf);
          lo = r[0];
          hi = r[1];
          fragments = hi - lo + 1 <= 4;
        }
      }
      spriteInstance(lo, hi, method, frame, byBase, hiHist, methodsOf);
    }
    frameBuf.clear();
  }

  private static void spriteInstance(int lo, int hi, int method, int frame,
                                     Map<Integer, long[]> byBase,
                                     Map<Integer, Map<Integer, Integer>> hiHist,
                                     Map<Integer, Set<Integer>> methodsOf) {
    long[] agg = byBase.computeIfAbsent(lo, k -> new long[]{0, -1, Integer.MAX_VALUE, -1});
    agg[0]++;
    agg[2] = Math.min(agg[2], frame);
    agg[3] = Math.max(agg[3], frame);
    hiHist.computeIfAbsent(lo, k -> new HashMap<>()).merge(hi, 1, Integer::sum);
    methodsOf.computeIfAbsent(lo, k -> new TreeSet<>()).add(method);
  }

  public static void main(String[] args) throws Exception {
    run(System.getProperty("analysis.db", "analysis/analysis.db"),
        args.length > 0 ? args[0] : RzxBootstrap.DEFAULT_RZX);
    System.exit(0);
  }
}
