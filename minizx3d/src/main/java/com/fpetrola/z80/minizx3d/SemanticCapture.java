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
import java.util.HashMap;
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

  public static void main(String[] args) throws Exception {
    System.setProperty("dir.plane", "true");
    GameProfile profile = GameProfile.resolve(new String[0]);
    OracleVerify.Oracle oracle = OracleVerify.Oracle.load();
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
    int bufBase = oracle.root.get("pantalla").getInt("buffer");

    SpriteCatalog catalog = new SpriteCatalog(catalogPath, 128);
    System.out.println("SemanticCapture: " + rzx + " -> " + dbPath
        + " (catalogo " + catalogPath + ", canvas $4000 + $" + Integer.toHexString(bufBase) + ")");

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
      st.execute("CREATE INDEX de_frame ON draw_events(frame_first)");
      st.execute("CREATE INDEX ot_frame ON oracle_truth(frame)");
    }
    PreparedStatement insSet = conn.prepareStatement(
        "INSERT INTO read_sets(id, n, hash, addrs) VALUES (?,?,?,?)");
    PreparedStatement insEv = conn.prepareStatement(
        "INSERT INTO draw_events VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    PreparedStatement insTruth = conn.prepareStatement(
        "INSERT INTO oracle_truth VALUES (?,?,?,?,?)");

    Map<Long, Integer> setIds = new HashMap<>();
    int[] nextSetId = {1};
    Map<Integer, int[]> leafMemo = new HashMap<>();
    Map<Integer, Ev> open = new HashMap<>();
    Map<Long, Agg> aggOpen = new HashMap<>();
    long[] evRows = {0}, evFlushed = {0}, evBig = {0}, evSat = {0};
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
        if (frame % truthEvery == 0) {
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
    replay.observer = (addr, frame, node, routine, order, valNode, dirNode) -> {
      int idx;
      if (addr >= DISPLAY && addr < DISPLAY + CANVAS_BYTES)
        idx = addr - DISPLAY;
      else if (addr >= bufBase && addr < bufBase + CANVAS_BYTES)
        idx = addr - bufBase;
      else
        return;
      TaintReplay r = holder[0];
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
        + " %d con bytes saturados, %d read_sets%n",
        (System.currentTimeMillis() - t0) / 1000, evFlushed[0], evRows[0],
        evFlushed[0] / (double) Math.max(1, evRows[0]), evBig[0], evSat[0], setIds.size());
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
