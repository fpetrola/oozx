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

import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The "export" command: consolidates EVERYTHING the analyses deduced about the game
 * into one machine-readable JSON (game model): memory zones with their data type,
 * every sprite with its address/size/usage, the text font, the entity structure,
 * dynamic variables, routines classified by evidence, and the execution episodes.
 * Requires the track tables (runs the pipeline first if missing). Every field is
 * backed by observed evidence — nothing is guessed from game knowledge.
 */
public class GameModelExporter {
  private final AnalysisDB db;
  private final String dbPath;
  private final CoordinateFinder.Plan plan;
  private final Explainer explainer;
  private final Set<Integer> valReads, addrReads;
  private final List<int[]> gfxRegions, lookupTables;

  public GameModelExporter(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
    this.plan = new CoordinateFinder(db).find();
    this.explainer = new Explainer(db, dbPath);
    this.valReads = GameMapper.roleReads(db, plan, "VAL");
    this.addrReads = GameMapper.roleReads(db, plan, "ADDR");
    this.lookupTables = GameMapper.mergeRanges(addrReads.stream()
        .map(db.reads::get)
        .filter(r -> isStaticish(r.addrMin(), r.addrMax()))
        .filter(r -> r.addrMax() - r.addrMin() + 1 < 1024)
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 16);
    this.gfxRegions = GameMapper.mergeRanges(valReads.stream()
        .map(db.reads::get)
        .filter(r -> r.addrMax() - r.addrMin() + 1 >= 16 && isStaticish(r.addrMin(), r.addrMax()))
        .filter(r -> lookupTables.stream().noneMatch(t -> t[1] - t[0] + 1 < 1024
            && r.addrMin() >= t[0] - 16 && r.addrMax() <= t[1] + 16))
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 64)
        .stream().filter(g -> g[1] - g[0] + 1 >= 1024).toList();
  }

  private boolean isStaticish(int lo, int hi) {
    String c = explainer.classifyRange(lo, hi);
    return c.startsWith("ESTATICA") || c.startsWith("mayormente") || c.startsWith("MIXTA");
  }

  public void export(String outPath) throws Exception {
    Map<String, Object> root = new LinkedHashMap<>();
    Map<String, Object> entidades = entidades();
    root.put("meta", meta());
    root.put("pantalla", Map.of("pixels", List.of(16384, 22527), "atributos", List.of(22528, 23295)));
    root.put("buffers", buffers());
    root.put("zonas_memoria", zonas(entidades));
    root.put("sprites", sprites());
    root.put("fuente_texto", fuente());
    root.put("entidades", entidades);
    root.put("variables", variables());
    root.put("rutinas", rutinas());
    root.put("episodios", episodios());
    String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root);
    Files.writeString(Path.of(outPath), json);
    System.out.println("Modelo del juego -> " + outPath + " (" + json.length() / 1024 + " KB)");
  }

  private Map<String, Object> meta() throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("generado", LocalDateTime.now().toString());
    m.put("descripcion", "Modelo del juego deducido automaticamente del replay RZX instrumentado;"
        + " toda afirmacion esta respaldada por evidencia observada (ver doc/MANUAL-ANALISIS.md)");
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      m.put("dibujados", scalar(c, "SELECT COUNT(*) FROM sprite_draws"));
      m.put("frames_con_dibujos", scalar(c, "SELECT COUNT(DISTINCT frame) FROM sprite_draws"));
      m.put("sprites_encontrados", scalar(c, "SELECT COUNT(*) FROM sprites_found"));
    }
    return m;
  }

  private List<Object> buffers() {
    List<Object> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (CoordinateFinder.Region r : plan.regions()) {
      if (r.delta() % 256 != 0 || r.delta() == 0 || !seen.add(r.lo() + ".." + r.hi()))
        continue;
      Map<String, Object> b = new LinkedHashMap<>();
      b.put("rango", List.of(r.lo(), r.hi()));
      b.put("delta_a_pantalla", r.delta());
      boolean estatica = explainer.classifyRange(r.lo(), r.hi()).startsWith("ESTATICA");
      b.put("tipo", estatica ? "datos_estaticos_copiados_a_pantalla" : "buffer");
      List<Object> feeds = new ArrayList<>();
      for (AnalysisDB.Bulk bk : db.bulks.values()) {
        int dstHi = bk.dstMax() + Math.max(0, bk.lenMax() - 1);
        if (dstHi < r.lo() || bk.dstMin() > r.hi() || bk.srcMin() >= r.lo() && bk.srcMin() <= r.hi())
          continue;
        feeds.add(Map.of("copia_desde", List.of(bk.srcMin(), bk.srcMax() + bk.lenMax() - 1),
            "veces", bk.count(), "site", bk.pc()));
      }
      if (!feeds.isEmpty())
        b.put("alimentado_por", feeds);
      out.add(b);
    }
    return out;
  }

  private List<Object> zonas(Map<String, Object> entidades) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (int[] g : gfxRegions) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("rango", List.of(g[0], g[1]));
      z.put("tipo", "graficos_sprites");
      z.put("clasificacion", explainer.classifyRange(g[0], g[1]));
      z.put("leida_por", methodsReading(valReads, g[0], g[1]));
      out.add(z);
    }
    for (int[] t : lookupTables) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("rango", List.of(t[0], t[1]));
      z.put("tipo", "tabla_consulta_direcciones");
      z.put("usada_por", methodsReading(addrReads, t[0], t[1]));
      out.add(z);
    }
    Map<String, Object> f = fuente();
    if (f != null) {
      Map<String, Object> z = new LinkedHashMap<>(f);
      z.put("tipo", "fuente_texto_rom");
      out.add(z);
    }
    Object tabla = entidades.get("tabla");
    if (tabla instanceof Map<?, ?> tm) {
      Map<String, Object> z = new LinkedHashMap<>();
      int base = ((Number) tm.get("base")).intValue();
      int fin = base + ((Number) tm.get("registro_bytes")).intValue() * ((Number) tm.get("slots")).intValue() - 1;
      z.put("rango", List.of(base, fin));
      z.put("tipo", "tabla_entidades");
      out.add(z);
    }
    for (int[] rg : plan.watchRanges()) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("rango", List.of(rg[0], rg[1]));
      z.put("tipo", "variables_dinamicas");
      z.put("nota", "alimentan direcciones de dibujo; serie temporal en frame_cells");
      out.add(z);
    }
    out.sort(Comparator.comparingInt(z -> ((Number) ((List<?>) z.get("rango")).get(0)).intValue()));
    return new ArrayList<>(out);
  }

  private List<String> methodsReading(Set<Integer> readSites, int lo, int hi) {
    Set<String> ms = new TreeSet<>();
    for (int s : readSites) {
      AnalysisDB.Stat r = db.reads.get(s);
      if (r.addrMax() >= lo && r.addrMin() <= hi)
        ms.add(db.method.getOrDefault(s, "?"));
    }
    return new ArrayList<>(ms);
  }

  private List<Object> sprites() throws SQLException {
    // typical drawn size per sprite: vote over (gfx, w, h) of pixel draws
    List<int[]> bases = new ArrayList<>();
    List<Object> out = new ArrayList<>();
    Map<Integer, Map<String, Integer>> sizeVotes = new HashMap<>();
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT base, last FROM sprites_found ORDER BY base")) {
        while (rs.next())
          bases.add(new int[]{rs.getInt(1), rs.getInt(2)});
      }
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT gfx, w, h, COUNT(*) FROM sprite_draws WHERE kind='P' AND gfx>=0 GROUP BY gfx, w, h")) {
        while (rs.next()) {
          int gfx = rs.getInt(1);
          int[] sp = spriteContaining(bases, gfx);
          if (sp != null)
            sizeVotes.computeIfAbsent(sp[0], k -> new HashMap<>())
                .merge(rs.getInt(2) + "x" + rs.getInt(3), rs.getInt(4), Integer::sum);
        }
      }
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT base, last, size, veces, frame_first, frame_last, methods FROM sprites_found ORDER BY base")) {
        while (rs.next()) {
          Map<String, Object> s = new LinkedHashMap<>();
          s.put("base", rs.getInt(1));
          s.put("fin", rs.getInt(2));
          s.put("bytes", rs.getInt(3));
          s.put("veces_dibujado", rs.getInt(4));
          s.put("frames", List.of(rs.getInt(5), rs.getInt(6)));
          s.put("rutinas", Arrays.asList(rs.getString(7).split(" ")));
          Map<String, Integer> votes = sizeVotes.get(rs.getInt(1));
          if (votes != null)
            s.put("dibujo_tipico", Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey());
          out.add(s);
        }
      }
    }
    return out;
  }

  private static int[] spriteContaining(List<int[]> sorted, int addr) {
    int loIdx = 0, hiIdx = sorted.size() - 1;
    while (loIdx <= hiIdx) {
      int mid = (loIdx + hiIdx) >>> 1;
      int[] s = sorted.get(mid);
      if (addr < s[0])
        hiIdx = mid - 1;
      else if (addr > s[1])
        loIdx = mid + 1;
      else
        return s;
    }
    return null;
  }

  /** VAL reads of a draw method that land in ROM (<16384) = the character font. */
  private Map<String, Object> fuente() {
    for (int s : valReads) {
      AnalysisDB.Stat r = db.reads.get(s);
      if (r.addrMin() < 16384) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("rango", List.of(r.addrMin(), Math.min(16383, r.addrMax())));
        f.put("rutina", db.method.getOrDefault(s, "?"));
        f.put("nota", "el rango agregado del site puede mezclar zonas; el tramo <16384 es ROM");
        return f;
      }
    }
    return null;
  }

  private Map<String, Object> entidades() throws SQLException {
    Map<String, Object> out = new LinkedHashMap<>();
    record Pair(int xAddr, String xT, int xOff, int yAddr, String yT, int yOff, double rate) {
    }
    List<Pair> strong = new ArrayList<>(), weak = new ArrayList<>();
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         ResultSet rs = c.createStatement().executeQuery(
             "SELECT x_addr,x_transform,x_off,y_addr,y_transform,y_off,rate FROM coord_pairs ORDER BY x_addr")) {
      while (rs.next()) {
        Pair p = new Pair(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
            rs.getString(5), rs.getInt(6), rs.getDouble(7));
        (p.rate() >= 0.10 ? strong : weak).add(p);
      }
    }
    // best confidence-weighted stride run over the strong pair X addresses
    List<Integer> xs = strong.stream().map(Pair::xAddr).sorted().distinct().toList();
    Map<Integer, Double> weights = new HashMap<>();
    for (Pair p : strong)
      weights.merge(p.xAddr(), p.rate(), Double::sum);
    int[] run = GameMapper.bestStrideRun(xs, weights);
    int stride = run != null ? run[1] : 0, runStart = run != null ? run[0] : -1,
        runLen = run != null ? run[2] : 0;
    Set<Integer> inRun = new HashSet<>();
    if (stride > 0) {
      int base = runStart, slots = runLen;
      List<Object> loaders = new ArrayList<>();
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi >= runStart && b.dstMin() <= runStart + (runLen - 1) * stride) {
          base = Math.min(base, b.dstMin());
          slots = Math.max(slots, (dstHi - base + 1) / stride);
          loaders.add(Map.of("desde", List.of(b.srcMin(), b.srcMax() + b.lenMax() - 1),
              "veces", b.count(), "site", b.pc()));
        }
      }
      Map<String, Object> tabla = new LinkedHashMap<>();
      tabla.put("base", base);
      tabla.put("registro_bytes", stride);
      tabla.put("slots", slots);
      Map<String, Object> campos = new LinkedHashMap<>();
      List<Object> slotList = new ArrayList<>();
      for (int k = 0; k < runLen; k++) {
        int xa = runStart + k * stride;
        inRun.add(xa);
        Pair p = strong.stream().filter(q -> q.xAddr() == xa).findFirst().orElse(null);
        if (p == null)
          continue;
        campos.putIfAbsent("x", Map.of("offset", (p.xAddr() - base) % stride, "formula", p.xT()));
        campos.putIfAbsent("y", Map.of("offset", (p.yAddr() - base) % stride, "formula", p.yT()));
        slotList.add(Map.of("x_addr", p.xAddr(), "y_addr", p.yAddr(), "confianza", p.rate()));
      }
      // graphics selector: dynamic cells feeding the ADDRESS of the sprite-data reads
      for (int off : selectorOffsets(base, stride, slots))
        campos.put("selector_grafico_offset_" + off, Map.of("offset", off,
            "nota", "elige que sprite/frame se dibuja"));
      tabla.put("campos", campos);
      tabla.put("slots_validados", slotList);
      if (!loaders.isEmpty())
        tabla.put("cargada_por", loaders);
      List<String> updaters = new ArrayList<>(new TreeSet<>(
          db.writersIntersecting(base, base + slots * stride - 1).stream()
              .map(w -> db.method.getOrDefault(w.pc(), "?")).toList()));
      tabla.put("actualizada_por", updaters);
      out.put("tabla", tabla);
    }
    List<Object> individuales = new ArrayList<>();
    for (Pair p : strong)
      if (!inRun.contains(p.xAddr()))
        individuales.add(Map.of("x_addr", p.xAddr(), "x_formula", p.xT() + (p.xOff() != 0 ? "+" + p.xOff() : ""),
            "y_addr", p.yAddr(), "y_formula", p.yT() + (p.yOff() != 0 ? "+" + p.yOff() : ""),
            "confianza", p.rate()));
    out.put("individuales", individuales);
    if (!weak.isEmpty())
      out.put("pares_debiles_posible_ruido", weak.stream()
          .map(p -> Map.of("x_addr", p.xAddr(), "y_addr", p.yAddr(), "confianza", p.rate()))
          .toList());
    return out;
  }

  /** offsets within the entity record of the cells that feed sprite-data read addresses. */
  private List<Integer> selectorOffsets(int base, int stride, int slots) {
    Set<Integer> gfxReadSites = new TreeSet<>();
    for (int s : valReads) {
      AnalysisDB.Stat r = db.reads.get(s);
      if (r.addrMax() > r.addrMin()
          && gfxRegions.stream().anyMatch(g -> r.addrMax() >= g[0] && r.addrMin() <= g[1]))
        gfxReadSites.add(s);
    }
    Set<Integer> offsets = new TreeSet<>();
    for (int rs : gfxReadSites) {
      Set<Integer> seen = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(rs, List.of()))
        if (e.src() != 0 && e.role() != null && e.role().contains("ADDR") && seen.add(e.src()))
          queue.add(new int[]{e.src(), 1});
      while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        AnalysisDB.Stat r = db.reads.get(cur[0]);
        // a site is the selector of ONE record field only if every address it reads is
        // congruent to the same offset (mod stride); sites sweeping whole records skip
        if (r != null && r.addrMin() >= base && r.addrMax() <= base + stride * (slots + 1)
            && (r.addrMin() - base) % stride == (r.addrMax() - base) % stride
            && (r.addrMax() - r.addrMin()) % stride == 0)
          offsets.add((r.addrMin() - base) % stride);
        if (cur[1] >= 6)
          continue;
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(cur[0], List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(new int[]{e.src(), cur[1] + 1});
      }
    }
    return new ArrayList<>(offsets);
  }

  private List<Object> variables() throws SQLException {
    Map<Integer, String> coordAxis = new HashMap<>();
    Map<Integer, int[]> stats = new HashMap<>(); // addr -> {min, max, distintos}
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (ResultSet rs = c.createStatement().executeQuery("SELECT addr, axis FROM coord_cells")) {
        while (rs.next())
          coordAxis.putIfAbsent(rs.getInt(1), rs.getString(2));
      }
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT addr, MIN(val), MAX(val), COUNT(DISTINCT val) FROM frame_cells GROUP BY addr")) {
        while (rs.next())
          stats.put(rs.getInt(1), new int[]{rs.getInt(2), rs.getInt(3), rs.getInt(4)});
      }
    }
    List<Object> out = new ArrayList<>();
    for (int[] rg : plan.watchRanges()) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("rango", List.of(rg[0], rg[1]));
      List<Object> celdas = new ArrayList<>();
      for (int a = rg[0]; a <= rg[1]; a++) {
        int[] st = stats.get(a);
        if (st == null || st[2] < 2)
          continue; // constante toda la partida: sin interes
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("addr", a);
        cell.put("min", st[0]);
        cell.put("max", st[1]);
        cell.put("valores_distintos", st[2]);
        if (coordAxis.containsKey(a))
          cell.put("coordenada", coordAxis.get(a));
        celdas.add(cell);
      }
      z.put("celdas", celdas);
      out.add(z);
    }
    return out;
  }

  private List<Object> rutinas() throws SQLException {
    // typical pixel/attr cluster per method + path counts, from sprite_draws
    Map<Integer, Map<String, long[]>> byMethodKind = new HashMap<>(); // m -> kind -> {bestCount, w, h, total}
    Map<Integer, Integer> pathsOf = new HashMap<>();
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT method, kind, w, h, COUNT(*) FROM sprite_draws GROUP BY 1,2,3,4")) {
        while (rs.next()) {
          long n = rs.getLong(5);
          long[] cur = byMethodKind.computeIfAbsent(rs.getInt(1), k -> new HashMap<>())
              .computeIfAbsent(rs.getString(2), k -> new long[]{0, 0, 0, 0});
          cur[3] += n;
          if (n > cur[0]) {
            cur[0] = n;
            cur[1] = rs.getInt(3);
            cur[2] = rs.getInt(4);
          }
        }
      }
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT method, COUNT(DISTINCT path) FROM sprite_draws GROUP BY 1")) {
        while (rs.next())
          pathsOf.put(rs.getInt(1), rs.getInt(2));
      }
    }
    // which methods read the ROM font (text renderers)
    Set<String> romReaders = new HashSet<>();
    for (int s : valReads)
      if (db.reads.get(s).addrMin() < 16384)
        romReaders.add(db.method.getOrDefault(s, "?"));

    List<Object> out = new ArrayList<>();
    for (Map.Entry<Integer, String> me : plan.drawMethods().entrySet()) {
      Map<String, Object> r = new LinkedHashMap<>();
      r.put("entry", me.getKey());
      r.put("nombre", me.getValue());
      Map<String, long[]> kinds = byMethodKind.getOrDefault(me.getKey(), Map.of());
      long[] p = kinds.get("P"), a = kinds.get("A");
      String tipo;
      if (p != null && romReaders.contains(me.getValue()))
        tipo = "texto";
      else if (p != null && p[2] == 1)
        tipo = "dibujo_sprites_por_filas";
      else if (p != null && p[1] >= 128)
        tipo = "render_fondo";
      else if (p != null)
        tipo = "dibujo_sprites";
      else if (a != null)
        tipo = "dibujo_atributos";
      else
        tipo = "dibujo";
      r.put("tipo", tipo);
      Map<String, Object> ev = new LinkedHashMap<>();
      if (p != null) {
        ev.put("dibujos_pixels", p[3]);
        ev.put("tipico_pixels", p[1] + "x" + p[2]);
      }
      if (a != null) {
        ev.put("dibujos_atributos", a[3]);
        ev.put("tipico_atributos", a[1] + "x" + a[2]);
      }
      Integer paths = pathsOf.get(me.getKey());
      if (paths != null)
        ev.put("caminos_de_ejecucion", paths);
      r.put("evidencia", ev);
      out.add(r);
    }
    // entity-table updaters that are not draw methods = game logic
    Set<String> drawNames = new HashSet<>(plan.drawMethods().values());
    Map<String, Long> logic = new TreeMap<>();
    for (int[] rg : plan.watchRanges())
      for (AnalysisDB.Stat w : db.writersIntersecting(rg[0], rg[1])) {
        String m = db.method.getOrDefault(w.pc(), "?");
        if (!drawNames.contains(m))
          logic.merge(m, w.count(), Long::sum);
      }
    logic.forEach((m, n) -> out.add(Map.of("nombre", m, "tipo", "logica_estado",
        "evidencia", Map.of("escrituras_a_variables", n))));
    return out;
  }

  private List<Object> episodios() throws SQLException {
    List<Object> out = new ArrayList<>();
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         ResultSet rs = c.createStatement().executeQuery(
             "SELECT method, path, count, gfx_lo, gfx_hi, cond FROM episodes ORDER BY count DESC")) {
      while (rs.next()) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("rutina", "$" + rs.getInt(1));
        e.put("camino", String.format("%08x", rs.getInt(2)));
        e.put("veces", rs.getInt(3));
        if (rs.getInt(4) >= 0)
          e.put("gfx", List.of(rs.getInt(4), rs.getInt(5)));
        String cond = rs.getString(6);
        if (cond != null && !cond.isEmpty())
          e.put("condicion", cond);
        out.add(e);
      }
    }
    return out;
  }

  private static long scalar(Connection c, String sql) throws SQLException {
    try (ResultSet rs = c.createStatement().executeQuery(sql)) {
      return rs.next() ? rs.getLong(1) : -1;
    }
  }
}
