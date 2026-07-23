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

package com.fpetrola.z80.minizx3d;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Capa 1 de la base semántica (doc/BASE-SEMANTICA.md §7): escribe {@code read_sets},
 * {@code draw_events}, {@code mem_profile} y {@code oracle_truth} DURANTE el replay
 * instrumentado (decisión §7.2.1: el replay corre una vez, la inferencia itera barato).
 *
 * <p>Un EVENTO son los writes de canvas de UNA invocación del árbol de llamadas
 * ({@code writeNode}) — la definición ya medida de "un dibujo" (CallTreeProbe). El canvas
 * no es sólo el display file: en JSW los sprites se dibujan al buffer de composición
 * ($6000) y la raíz copia al display; los dos rangos cuentan, con el mismo layout de
 * índices. Los repintados masivos (la copia, el redibujado de sala) exceden
 * {@code semantic.maxEventBytes} y quedan con {@code dir_set} NULL — son escena, no objeto.
 *
 * <p>{@code dir_set} = unión de hojas dir del evento; {@code dir_core_set} = hojas
 * presentes en ≥80% de los bytes. La distinción es la clave de {@code deps} (etapa 3):
 * una dependencia real entra por la cadena de registros del dibujante y tiñe TODOS los
 * bytes; la contaminación por solapamiento tiñe sólo los bytes solapados.
 *
 * <p>Uso: {@code SemanticCapture <rzx> <db> [maxFrames]} con {@code -Dgame=<g>}.
 * Knobs: {@code -Dsemantic.catalog} (catálogo para gfx_base; el ancho, no el curado),
 * {@code -Dsemantic.leafmin} (hoja mínima que entra a un set; JSW 32768 = $8000, donde
 * empieza el estado del juego), {@code -Dsemantic.maxEventBytes}, {@code -Dsemantic.truth}
 * (frecuencia de oracle_truth).
 */
public final class SemanticCapture {

  private static final int DISPLAY = 16384, CANVAS_BYTES = TaintReplay.PIXEL_BYTES;
  private static final int AGG_GAP = Integer.getInteger("semantic.agggap", 4);

  /** un dibujo en curso: los writes de una invocación, byte → nodo dir del último write. */
  private static final class Ev {
    final int routine, parentRoutine, frame, firstOrder;
    int lo = Integer.MAX_VALUE, hi = -1;
    boolean big;
    int bigBytes;
    final Map<Integer, Integer> byteDir = new HashMap<>();
    final Map<Integer, Integer> gfx = new HashMap<>();

    Ev(int routine, int parentRoutine, int frame, int firstOrder) {
      this.routine = routine;
      this.parentRoutine = parentRoutine;
      this.frame = frame;
      this.firstOrder = firstOrder;
    }
  }

  /** una fila de draw_events abierta a la agregación de frames consecutivos idénticos. */
  private static final class Agg {
    int frameFirst, frameLast, count, lo, hi, bytes, gfxBase, routine, parentRoutine, order;
    Integer dirSet, coreSet;
  }

  /** los accesos de una invocación en curso (parametricidad §4): direcciones tocadas. */
  private static final class MAcc {
    final int routine, frame;
    final TreeMap<Integer, Boolean> addrs = new TreeMap<>(); // addr -> hubo write
    boolean big;
    int bigN;

    MAcc(int routine, int frame) {
      this.routine = routine;
      this.frame = frame;
    }
  }

  /** una fila de mem_accesses abierta a la agregación. */
  private static final class MAgg {
    int frameFirst, frameLast, count, routine, base, n, writes;
    Integer pattern;
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("dir.plane", "true");
    GameProfile profile = GameProfile.resolve(new String[0]);
    // el oráculo es el ASSERT de JSW; para cualquier otro juego -Dsemantic.notruth=true
    // corre sin él (modo consistencia §8.1) y el canvas se descubre solo
    boolean noTruth = Boolean.getBoolean("semantic.notruth");
    OracleVerify.Oracle oracle = noTruth ? null : OracleVerify.Oracle.load();
    String rzx = args.length > 0 ? args[0] : profile.rzx;
    String dbPath = args.length > 1 ? args[1] : "analysis/jsw-semantic.db";
    int maxFrames = args.length > 2 ? Integer.parseInt(args[2]) : Integer.MAX_VALUE;
    // el RZX entero de JSW necesita ~2.5GB de heap (los arrays del plano dir duplican
    // capacidad pasando los ~11M de nodos, frame ~60K). Con el heap default de exec:java
    // (~25% de la RAM) eso era un OOM que se llevaba la corrida entera y la DB quedaba a
    // medias — mejor capar CON AVISO que morir sin escribir mem_profile.
    long maxHeap = Runtime.getRuntime().maxMemory();
    if (args.length <= 2 && maxHeap < 2_600_000_000L) {
      maxFrames = 60000;
      System.out.println("AVISO: heap maximo " + (maxHeap >> 20) + "MB < 2.6GB -> capturo"
          + " hasta el frame 60000 (cubre todas las sogas y pares identicos del RZX de"
          + " JSW). Para el RZX entero: MAVEN_OPTS=-Xmx3g y maxFrames explicito.");
    }
    String catalogPath = System.getProperty("semantic.catalog", profile.db);
    int leafMin = Integer.getInteger("semantic.leafmin", 32768);
    int maxEventBytes = Integer.getInteger("semantic.maxEventBytes", 2048);
    int truthEvery = Integer.getInteger("semantic.truth", 1);
    // canvas: -Dsemantic.canvas=HEX lo fija; "auto" (default sin oráculo) lo descubre por
    // el delta fuente→destino de la copia al display; con oráculo, el del oráculo
    String canvasProp = System.getProperty("semantic.canvas", "");
    int[] bufBase = {-1};
    if (!canvasProp.isEmpty() && !canvasProp.equals("auto"))
      bufBase[0] = Integer.parseInt(canvasProp.replace("$", ""), 16);
    else if (oracle != null && canvasProp.isEmpty())
      bufBase[0] = oracle.root.get("pantalla").getInt("buffer");
    Map<Integer, Integer> deltaHisto = new HashMap<>();
    // -Dsemantic.scratch=lo:hi (hex): la region de scratch de compose (la cura de Exolon)
    String scratchProp = System.getProperty("semantic.scratch", "");

    SpriteCatalog catalog = new SpriteCatalog(catalogPath, 128);
    System.out.println("SemanticCapture: " + rzx + " -> " + dbPath + " (catalogo "
        + catalogPath + ", canvas $4000 + " + (bufBase[0] < 0 ? "auto"
        : "$" + Integer.toHexString(bufBase[0])) + ")");

    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    conn.setAutoCommit(false);
    try (var st = conn.createStatement()) {
      st.execute("DROP TABLE IF EXISTS read_sets");
      st.execute("DROP TABLE IF EXISTS draw_events");
      st.execute("DROP TABLE IF EXISTS mem_profile");
      st.execute("DROP TABLE IF EXISTS oracle_truth");
      st.execute("CREATE TABLE read_sets(id INTEGER PRIMARY KEY, n INT, hash INT, addrs BLOB)");
      st.execute("CREATE TABLE draw_events(frame_first INT, frame_last INT, count INT,"
          + " screen_lo INT, screen_hi INT, bytes INT, gfx_base INT, dir_set INT,"
          + " dir_core_set INT, routine INT, parent_routine INT, write_order INT)");
      st.execute("CREATE TABLE mem_profile(addr INTEGER PRIMARY KEY, writes INT)");
      st.execute("CREATE TABLE oracle_truth(frame INT, slot INT, tipo INT, x INT, y INT)");
      st.execute("DROP TABLE IF EXISTS access_patterns");
      st.execute("DROP TABLE IF EXISTS mem_accesses");
      st.execute("CREATE TABLE access_patterns(id INTEGER PRIMARY KEY, n INT, hash INT,"
          + " offsets BLOB)");
      st.execute("CREATE TABLE mem_accesses(frame_first INT, frame_last INT, count INT,"
          + " routine INT, base INT, pattern INT, n INT, writes INT)");
      st.execute("CREATE INDEX de_frame ON draw_events(frame_first)");
      st.execute("CREATE INDEX ot_frame ON oracle_truth(frame)");
      st.execute("CREATE INDEX ma_routine ON mem_accesses(routine)");
    }
    PreparedStatement insSet = conn.prepareStatement(
        "INSERT INTO read_sets(id, n, hash, addrs) VALUES (?,?,?,?)");
    PreparedStatement insEv = conn.prepareStatement(
        "INSERT INTO draw_events VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    PreparedStatement insTruth = conn.prepareStatement(
        "INSERT INTO oracle_truth VALUES (?,?,?,?,?)");
    PreparedStatement insPat = conn.prepareStatement(
        "INSERT INTO access_patterns(id, n, hash, offsets) VALUES (?,?,?,?)");
    PreparedStatement insMem = conn.prepareStatement(
        "INSERT INTO mem_accesses VALUES (?,?,?,?,?,?,?,?)");

    Map<Long, Integer> setIds = new HashMap<>();
    int[] nextSetId = {1};
    Map<Integer, int[]> leafMemo = new HashMap<>();
    Map<Integer, Ev> open = new HashMap<>();
    Map<Long, Agg> aggOpen = new HashMap<>();
    long[] evRows = {0}, evFlushed = {0}, evBig = {0}, evSat = {0};
    // parametricidad: accesos por invocación → patrón internado → fila agregada
    boolean memOn = !"false".equals(System.getProperty("semantic.mem"));
    int patCap = Integer.getInteger("semantic.patcap", 128);
    Map<Integer, MAcc> memOpen = new HashMap<>();
    Map<Long, Integer> patIds = new HashMap<>();
    int[] nextPatId = {1};
    Map<Long, MAgg> memAggOpen = new HashMap<>();
    long[] memRows = {0}, memFlushed = {0};
    TaintReplay[] holder = new TaintReplay[1];

    TaintReplay replay = new TaintReplay(rzx, catalog, snap -> {
      TaintReplay r = holder[0];
      int frame = snap.frame() - 1; // lo que está en pantalla se pintó en el frame anterior
      try {
        // tope por TAMAÑO: con leafcap 512 una entrada llega a ~2KB (ver OriginTaint.flatten)
        if (leafMemo.size() > 100_000)
          leafMemo.clear();
        for (Ev ev : open.values()) {
          evFlushed[0]++;
          flush(r, ev, leafMin, setIds, nextSetId, insSet, insEv, aggOpen, evRows, evBig,
              evSat, leafMemo);
        }
        open.clear();
        for (MAcc m : memOpen.values()) {
          memFlushed[0]++;
          flushMem(m, patCap, patIds, nextPatId, insPat, insMem, memAggOpen, memRows);
        }
        memOpen.clear();
        if (oracle != null && frame % truthEvery == 0) {
          // Willy como fila slot=-1: tipo lleva su estado de soga ($85D6, 3..32 = colgado)
          // — el ground truth contra el que se verifica la arista follower(willy->soga)
          insTruth.setInt(1, frame);
          insTruth.setInt(2, -1);
          insTruth.setInt(3, r.memByte(oracle.willySoga));
          insTruth.setInt(4, 0);
          insTruth.setInt(5, r.memByte(oracle.willyY));
          insTruth.addBatch();
          for (int s = 0; s < oracle.entSlots; s++) {
            int base = oracle.entBase + s * oracle.entStride;
            int b0 = r.memByte(base + oracle.tipoOffset);
            if (b0 == oracle.entTerm)
              break;
            int type = b0 & oracle.tipoMask;
            if (type == 0)
              continue;
            insTruth.setInt(1, frame);
            insTruth.setInt(2, s);
            insTruth.setInt(3, type);
            // la soga guarda su x en el offset 3 y no tiene y propia; el resto x en 2, y en 3
            insTruth.setInt(4, type == 3 ? r.memByte(base + 3) : r.memByte(base + 2) & 31);
            insTruth.setInt(5, type == 3 ? 0 : r.memByte(base + 3));
            insTruth.addBatch();
          }
        }
        if (frame % 2000 == 0) {
          insTruth.executeBatch();
          insEv.executeBatch();
          insMem.executeBatch();
          conn.commit();
          if (frame % 10000 == 0)
            System.out.println("  frame " + frame + ": " + evFlushed[0] + " eventos, "
                + evRows[0] + " filas, " + setIds.size() + " sets, dirNodes="
                + (r.dir == null ? 0 : r.dir.nodeCount()));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    holder[0] = replay;
    if (!scratchProp.isEmpty()) {
      String[] pp = scratchProp.split(":");
      replay.scratchLo = Integer.parseInt(pp[0].replace("$", ""), 16);
      replay.scratchHi = Integer.parseInt(pp[1].replace("$", ""), 16);
      System.out.printf("scratch de compose: $%04x..$%04x (lecturas no aportan cadena a la"
          + " variante sin-scratch)%n", replay.scratchLo, replay.scratchHi);
    }
    if (memOn)
      replay.memObserver = (addr, frame, node, routine, write) -> {
        // el canvas es asunto de draw_events; el resto de la RAM es la estructura
        if (addr < 23296 || (bufBase[0] >= 0 && addr >= bufBase[0] && addr < bufBase[0] + CANVAS_BYTES))
          return;
        MAcc m = memOpen.get(node);
        if (m == null)
          memOpen.put(node, m = new MAcc(routine, frame));
        if (m.big) {
          m.bigN++;
          return;
        }
        m.addrs.merge(addr, write, (a, b) -> a || b);
        if (m.addrs.size() > patCap * 4) {
          m.big = true;
          m.bigN = m.addrs.size();
          m.addrs.clear();
        }
      };
    replay.observer = (addr, frame, node, routine, order, valNode, dirNode) -> {
      int idx;
      TaintReplay r = holder[0];
      if (addr >= DISPLAY && addr < DISPLAY + CANVAS_BYTES) {
        idx = addr - DISPLAY;
        // canvas auto: el write al display acaba de leer su fuente — el delta dominante
        // fuente-destino ES el buffer de composicion, sea cual sea el juego
        if (bufBase[0] < 0 && frame < 3000 && r.lastReadAddr > 23296) {
          int delta = r.lastReadAddr - addr;
          if (delta != 0 && deltaHisto.merge(delta, 1, Integer::sum) == 20000) {
            long total = deltaHisto.values().stream().mapToLong(Integer::intValue).sum();
            if (20000L * 10 >= total * 4) {
              bufBase[0] = DISPLAY + delta;
              System.out.printf("canvas descubierto: buffer de composicion en $%04x"
                  + " (delta $%04x, %d%% de las copias)%n", bufBase[0], delta,
                  20000L * 100 / total);
            }
          }
        }
      } else if (bufBase[0] >= 0 && addr >= bufBase[0] && addr < bufBase[0] + CANVAS_BYTES)
        idx = addr - bufBase[0];
      else
        return;
      Ev ev = open.get(node);
      if (ev == null) {
        int parent = node > 0 && node < r.nodeParent.size ? r.nodeParent.get(node) : 0;
        int parentRoutine = parent > 0 && parent < r.nodeAddr.size ? r.nodeAddr.get(parent) : 0;
        open.put(node, ev = new Ev(routine, parentRoutine, frame, order));
      }
      ev.lo = Math.min(ev.lo, idx);
      ev.hi = Math.max(ev.hi, idx);
      int gfx = r.taint.spriteOf(valNode);
      if (gfx != 0)
        ev.gfx.merge(gfx - 1, 1, Integer::sum);
      if (ev.big) {
        ev.bigBytes++;
        return;
      }
      ev.byteDir.put(idx, dirNode);
      if (ev.byteDir.size() > maxEventBytes) {
        ev.big = true;
        ev.bigBytes = ev.byteDir.size();
        ev.byteDir.clear();
      }
    };
    replay.paced = false;
    replay.maxFrames = maxFrames;
    long t0 = System.currentTimeMillis();
    try {
      replay.run();
    } catch (RuntimeException e) {
      System.out.println("replay terminado: " + e.getMessage());
    }
    for (Agg a : aggOpen.values())
      insertAgg(a, insEv, evRows);
    for (MAgg a : memAggOpen.values())
      insertMAgg(a, insMem, memRows);
    insMem.executeBatch();
    PreparedStatement insProf = conn.prepareStatement(
        "INSERT INTO mem_profile VALUES (?,?)");
    for (int a = 0; a < 0x10000; a++)
      if (replay.memWrites[a] > 0) {
        insProf.setInt(1, a);
        insProf.setInt(2, replay.memWrites[a]);
        insProf.addBatch();
      }
    insProf.executeBatch();
    insTruth.executeBatch();
    insEv.executeBatch();
    conn.commit();
    conn.close();
    System.out.printf("listo en %ds: %d eventos -> %d filas (%.1fx agregacion), %d grandes,"
        + " %d con bytes saturados, %d read_sets · mem: %d invocaciones -> %d filas"
        + " (%.1fx), %d patrones%n",
        (System.currentTimeMillis() - t0) / 1000, evFlushed[0], evRows[0],
        evFlushed[0] / (double) Math.max(1, evRows[0]), evBig[0], evSat[0], setIds.size(),
        memFlushed[0], memRows[0], memFlushed[0] / (double) Math.max(1, memRows[0]),
        patIds.size());
  }

  private static final int REGION_GAP = Integer.getInteger("semantic.rgap", 32);

  /**
   * Cierra los accesos de una invocación. El conjunto se parte en REGIONES por hueco de
   * direcciones (> {@code semantic.rgap}), una fila por región: una invocación real toca
   * varias zonas a la vez (atributos + tabla de lookup + gráfico + estado) y un patrón que
   * las mezcla no autocorrelaciona nada — el mismo corte por gap que usa TaintDiscover
   * para las piezas.
   */
  private static void flushMem(MAcc m, int patCap, Map<Long, Integer> patIds, int[] nextPatId,
                               PreparedStatement insPat, PreparedStatement insMem,
                               Map<Long, MAgg> memAggOpen, long[] memRows) throws Exception {
    if (m.big) {
      emitRegion(m, -1, java.util.List.of(), 0, m.bigN, patCap, patIds, nextPatId, insPat,
          insMem, memAggOpen, memRows);
      return;
    }
    if (m.addrs.isEmpty())
      return;
    List<Integer> region = new ArrayList<>();
    int writes = 0, prev = Integer.MIN_VALUE, base = -1;
    for (Map.Entry<Integer, Boolean> e : m.addrs.entrySet()) {
      if (prev != Integer.MIN_VALUE && e.getKey() - prev > REGION_GAP) {
        emitRegion(m, base, region, writes, region.size(), patCap, patIds, nextPatId, insPat,
            insMem, memAggOpen, memRows);
        region = new ArrayList<>();
        writes = 0;
        base = -1;
      }
      if (base < 0)
        base = e.getKey();
      region.add(e.getKey() - base);
      if (e.getValue())
        writes++;
      prev = e.getKey();
    }
    emitRegion(m, base, region, writes, region.size(), patCap, patIds, nextPatId, insPat,
        insMem, memAggOpen, memRows);
  }

  private static void emitRegion(MAcc m, int base, List<Integer> offs, int writes, int n,
                                 int patCap, Map<Long, Integer> patIds, int[] nextPatId,
                                 PreparedStatement insPat, PreparedStatement insMem,
                                 Map<Long, MAgg> memAggOpen, long[] memRows) throws Exception {
    if (n == 0)
      return;
    Integer pattern = null;
    if (base >= 0 && n <= patCap) {
      long h = 1469598103934665603L;
      for (int o : offs) {
        h ^= o;
        h *= 1099511628211L;
      }
      Integer id = patIds.get(h);
      if (id == null) {
        id = nextPatId[0]++;
        patIds.put(h, id);
        byte[] blob = new byte[offs.size() * 2];
        for (int k = 0; k < offs.size(); k++) {
          blob[k * 2] = (byte) (int) offs.get(k);
          blob[k * 2 + 1] = (byte) (offs.get(k) >> 8);
        }
        insPat.setInt(1, id);
        insPat.setInt(2, offs.size());
        insPat.setLong(3, h);
        insPat.setBytes(4, blob);
        insPat.addBatch();
        insPat.executeBatch();
      }
      pattern = id;
    }
    long key = 1469598103934665603L;
    for (long v : new long[]{m.routine, base, pattern == null ? -1 : pattern, n}) {
      key ^= v;
      key *= 1099511628211L;
    }
    MAgg agg = memAggOpen.get(key);
    if (agg != null && agg.frameLast >= m.frame - AGG_GAP) {
      agg.frameLast = m.frame;
      agg.count++;
      return;
    }
    if (agg != null)
      insertMAgg(agg, insMem, memRows);
    agg = new MAgg();
    agg.frameFirst = agg.frameLast = m.frame;
    agg.count = 1;
    agg.routine = m.routine;
    agg.base = base;
    agg.pattern = pattern;
    agg.n = n;
    agg.writes = writes;
    memAggOpen.put(key, agg);
  }

  private static void insertMAgg(MAgg a, PreparedStatement insMem, long[] memRows)
      throws Exception {
    insMem.setInt(1, a.frameFirst);
    insMem.setInt(2, a.frameLast);
    insMem.setInt(3, a.count);
    insMem.setInt(4, a.routine);
    insMem.setInt(5, a.base);
    if (a.pattern == null)
      insMem.setNull(6, java.sql.Types.INTEGER);
    else
      insMem.setInt(6, a.pattern);
    insMem.setInt(7, a.n);
    insMem.setInt(8, a.writes);
    insMem.addBatch();
    memRows[0]++;
  }

  private static void flush(TaintReplay r, Ev ev, int leafMin, Map<Long, Integer> setIds,
                            int[] nextSetId, PreparedStatement insSet, PreparedStatement insEv,
                            Map<Long, Agg> aggOpen, long[] evRows, long[] evBig, long[] evSat,
                            Map<Integer, int[]> leafMemo) throws Exception {
    int bytes = ev.big ? ev.bigBytes : ev.byteDir.size();
    if (bytes == 0)
      return;
    Integer dirSet = null, coreSet = null;
    if (!ev.big && r.dir != null) {
      Map<Integer, Integer> cnt = new TreeMap<>();
      int sat = 0;
      for (int dirNode : ev.byteDir.values()) {
        int[] lv = r.dir.leavesSorted(dirNode, TaintReplay.DIR_LEAFCAP, leafMemo);
        if (lv == null) {
          sat++;
          continue;
        }
        for (int leaf : lv)
          if (leaf >= leafMin)
            cnt.merge(leaf, 1, Integer::sum);
      }
      int usable = ev.byteDir.size() - sat;
      if (sat > 0)
        evSat[0]++;
      if (usable > 0 && !cnt.isEmpty()) {
        int core = (int) Math.ceil(usable * 0.8);
        dirSet = intern(cnt.keySet().stream().mapToInt(Integer::intValue).toArray(),
            setIds, nextSetId, insSet);
        int[] coreLeaves = cnt.entrySet().stream().filter(e -> e.getValue() >= core)
            .mapToInt(Map.Entry::getKey).toArray();
        coreSet = coreLeaves.length == 0 ? null
            : intern(coreLeaves, setIds, nextSetId, insSet);
      }
    } else if (ev.big)
      evBig[0]++;
    int gfxBase = ev.gfx.entrySet().stream()
        .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);

    long key = 1469598103934665603L;
    for (long v : new long[]{ev.routine, ev.lo, ev.hi, gfxBase,
        dirSet == null ? -1 : dirSet, coreSet == null ? -1 : coreSet}) {
      key ^= v;
      key *= 1099511628211L;
    }
    // el tick de juego de JSW son ~4 frames RZX: un dibujo estable reaparece con gap 4,
    // no 1 — la fila de vidas y la cinta colapsan a una fila por racha con esto
    Agg agg = aggOpen.get(key);
    if (agg != null && agg.frameLast >= ev.frame - AGG_GAP) {
      agg.frameLast = ev.frame;
      agg.count++;
      return;
    }
    if (agg != null)
      insertAgg(agg, insEv, evRows);
    agg = new Agg();
    agg.frameFirst = agg.frameLast = ev.frame;
    agg.count = 1;
    agg.lo = ev.lo;
    agg.hi = ev.hi;
    agg.bytes = bytes;
    agg.gfxBase = gfxBase;
    agg.dirSet = dirSet;
    agg.coreSet = coreSet;
    agg.routine = ev.routine;
    agg.parentRoutine = ev.parentRoutine;
    agg.order = ev.firstOrder;
    aggOpen.put(key, agg);
  }

  private static void insertAgg(Agg a, PreparedStatement insEv, long[] evRows) throws Exception {
    insEv.setInt(1, a.frameFirst);
    insEv.setInt(2, a.frameLast);
    insEv.setInt(3, a.count);
    insEv.setInt(4, a.lo);
    insEv.setInt(5, a.hi);
    insEv.setInt(6, a.bytes);
    insEv.setInt(7, a.gfxBase);
    if (a.dirSet == null)
      insEv.setNull(8, java.sql.Types.INTEGER);
    else
      insEv.setInt(8, a.dirSet);
    if (a.coreSet == null)
      insEv.setNull(9, java.sql.Types.INTEGER);
    else
      insEv.setInt(9, a.coreSet);
    insEv.setInt(10, a.routine);
    insEv.setInt(11, a.parentRoutine);
    insEv.setInt(12, a.order);
    insEv.addBatch();
    evRows[0]++;
  }

  /** interna un conjunto ordenado de hojas: mismo set, mismo id (hash FNV-1a de 64 bits). */
  private static int intern(int[] sorted, Map<Long, Integer> setIds, int[] nextSetId,
                            PreparedStatement insSet) throws Exception {
    long h = 1469598103934665603L;
    for (int a : sorted) {
      h ^= a;
      h *= 1099511628211L;
    }
    Integer id = setIds.get(h);
    if (id != null)
      return id;
    id = nextSetId[0]++;
    setIds.put(h, id);
    byte[] blob = new byte[sorted.length * 2];
    for (int i = 0; i < sorted.length; i++) {
      blob[i * 2] = (byte) sorted[i];
      blob[i * 2 + 1] = (byte) (sorted[i] >> 8);
    }
    insSet.setInt(1, id);
    insSet.setInt(2, sorted.length);
    insSet.setLong(3, h);
    insSet.setBytes(4, blob);
    insSet.addBatch();
    insSet.executeBatch();
    return id;
  }

  private SemanticCapture() {
  }
}
