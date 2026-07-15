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

  /**
   * Two layers: {@code hallazgos} are FINAL, readable conclusions (already grouped and
   * interpreted, only the most important items inline); {@code evidencia} holds the raw
   * supporting data, keyed by the finding's id.
   */
  public void export(String outPath) throws Exception {
    Map<String, Object> evEnt = entidades();
    StructFinder structFinder = new StructFinder(db, dbPath);
    List<Map<String, Object>> structs = structFinder.analyze(null);
    List<Map<String, Object>> canonicos = structFinder.canonical(structs);
    List<Map<String, Object>> rebuilds = new RebuildFinder(db, dbPath).analyze();
    List<Map<String, Object>> registros = new RecordFinder(db, dbPath).analyze();
    List<Object> rutinasFull = rutinas();

    List<Object> hallazgos = new ArrayList<>();
    Map<String, Object> evidencia = new LinkedHashMap<>();

    hallazgoPantalla(hallazgos, evidencia);
    Map<String, Object> mapa = hallazgo(hallazgos, evidencia, "mapa-memoria",
        "Mapa de memoria: que hay en cada rango", null);
    mapa.put("zonas", zonas(evEnt));
    hallazgoReconstruccion(hallazgos, evidencia, rebuilds);
    hallazgoRegistros(hallazgos, evidencia, registros);
    hallazgoEntidades(hallazgos, evidencia, evEnt, structs, canonicos);
    hallazgoProtagonista(hallazgos, evidencia, evEnt);
    hallazgoSprites(hallazgos, evidencia);
    hallazgoFuente(hallazgos, evidencia);
    hallazgoTablas(hallazgos, evidencia);
    hallazgoVariables(hallazgos, evidencia);
    hallazgoRutinas(hallazgos, evidencia, rutinasFull);
    hallazgoEstructuras(hallazgos, evidencia, structs, evEnt);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("meta", meta());
    root.put("hallazgos", hallazgos);
    root.put("modelo", new ModelAssembler(db, plan, gfxRegions, lookupTables)
        .assemble(canonicos, registros, rebuilds, rutinasFull, fuente(), mejorProtagonista(evEnt)));
    root.put("evidencia", evidencia);
    String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root);
    Files.writeString(Path.of(outPath), json);
    System.out.println("Modelo del juego -> " + outPath + " (" + json.length() / 1024 + " KB, "
        + hallazgos.size() + " hallazgos)");
  }

  private Map<String, Object> hallazgo(List<Object> hallazgos, Map<String, Object> evidencia,
                                       String id, String titulo, Object evidence) {
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("id", id);
    h.put("titulo", titulo);
    hallazgos.add(h);
    if (evidence != null) {
      evidencia.put(id, evidence);
      h.put("evidencia", id);
    }
    return h;
  }

  // ---------- hallazgo: pantalla y composicion ----------
  private void hallazgoPantalla(List<Object> hallazgos, Map<String, Object> evidencia) {
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "pantalla",
        "La pantalla se compone por etapas de buffers (doble buffer)", buffers());
    h.put("pixels", pipeline(16384, 22527));
    h.put("colores", pipeline(22528, 23295));
  }

  /** chain of copies ending at the given screen slice, rendered as one readable line. */
  private String pipeline(int lo, int hi) {
    StringBuilder sb = new StringBuilder("pantalla [" + lo + ".." + hi + "]");
    int curLo = lo, curHi = hi;
    for (int hop = 0; hop < 4; hop++) {
      AnalysisDB.Bulk best = null;
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi < curLo || b.dstMin() > curHi || b.srcMin() >= curLo && b.srcMin() <= curHi)
          continue;
        if (best == null || b.count() > best.count())
          best = b;
      }
      if (best == null)
        break;
      int srcHi = best.srcMax() + best.lenMax() - 1;
      sb.insert(0, "[" + best.srcMin() + ".." + srcHi + "] --copia x" + best.count() + "--> ");
      curLo = best.srcMin();
      curHi = srcHi;
    }
    return sb.toString();
  }

  // ---------- hallazgo: variables selectoras y su cluster de reconstruccion ----------
  @SuppressWarnings("unchecked")
  private void hallazgoReconstruccion(List<Object> hallazgos, Map<String, Object> evidencia,
                                      List<Map<String, Object>> all) {
    if (all.isEmpty())
      return;
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "reconstruccion-por-selector",
        "Variables selectoras: eligen que contenido se construye (pantalla/nivel actual) "
            + "y disparan el cluster que lo reconstruye", all);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> f : all) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      Map<String, Object> main = (Map<String, Object>)
          ((List<Object>) f.get("copias_indexadas_por_el_selector")).get(0);
      Map<String, Object> tabla = (Map<String, Object>) main.get("tabla_indexada");
      List<Integer> dst = (List<Integer>) main.get("destino");
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("selector", "mem[" + sel.get("celda") + "]"
          + (sel.containsKey("valores_distintos")
              ? " (" + sel.get("valores_distintos") + " valores distintos)" : ""));
      item.put("que_hace", "elige 1 de " + tabla.get("registros_usados") + " registros de "
          + tabla.get("registro_bytes") + " bytes en " + tabla.get("rango")
          + " y lo copia a [" + dst.get(0) + ".." + dst.get(1) + "]");
      item.put("formula", main.get("formula"));
      item.put("disparado_por", ((List<Object>) sel.get("escrito_por")).stream()
          .map(wo -> {
            Map<String, Object> w = (Map<String, Object>) wo;
            return w.get("rutina") + " x" + w.get("veces");
          }).toList());
      if (f.containsKey("nota_disparo"))
        item.put("nota", f.get("nota_disparo"));
      if (f.containsKey("corren_con_la_misma_cadencia"))
        item.put("reconstruye_ademas", ((List<Object>) f.get("corren_con_la_misma_cadencia")).stream()
            .map(co -> {
              Map<String, Object> c = (Map<String, Object>) co;
              return "[" + ((List<Integer>) c.get("origen")).get(0) + ".."
                  + ((List<Integer>) c.get("origen")).get(1) + "] -> ["
                  + ((List<Integer>) c.get("destino")).get(0) + ".."
                  + ((List<Integer>) c.get("destino")).get(1) + "] (" + c.get("por_reconstruccion")
                  + "x por reconstruccion" + (c.containsKey("nota") ? "; " + c.get("nota") : "") + ")";
            }).toList());
      Map<String, Object> lectores = new LinkedHashMap<>();
      ((Map<String, Object>) f.get("destino_leido_por")).forEach((rango, rs) ->
          lectores.put(rango, ((List<Object>) rs).stream()
              .map(ro -> (String) ((Map<String, Object>) ro).get("rutina").toString()).toList()));
      item.put("contenido_consumido_despues_por", lectores);
      lista.add(item);
    }
    h.put("selectores", lista);
  }

  // ---------- hallazgo: el registro singleton reconstruido, campo por campo ----------
  @SuppressWarnings("unchecked")
  private void hallazgoRegistros(List<Object> hallazgos, Map<String, Object> evidencia,
                                 List<Map<String, Object>> registros) {
    if (registros.isEmpty())
      return;
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "registros-reconstruidos",
        "El registro que el selector reconstruye, campo por campo (layout del contenido actual)",
        registros);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> rec : registros) {
      Map<String, Object> item = new LinkedHashMap<>();
      List<Integer> r = (List<Integer>) rec.get("rango");
      item.put("rango", r);
      item.put("selector", "mem[" + rec.get("selector") + "]");
      Map<String, String> campos = new LinkedHashMap<>();
      for (Map<String, Object> f : (List<Map<String, Object>>) rec.get("campos")) {
        List<Integer> fr = (List<Integer>) f.get("rango");
        StringBuilder sb = new StringBuilder();
        if (f.containsKey("nombre_propuesto"))
          sb.append(f.get("nombre_propuesto"));
        if (f.containsKey("etiquetas"))
          sb.append(sb.isEmpty() ? "" : " — ")
              .append(String.join("; ", (List<String>) f.get("etiquetas")));
        if (sb.isEmpty())
          sb.append("leido por ").append(f.getOrDefault("leido_por", "?"));
        campos.put("[" + fr.get(0) + ".." + fr.get(1) + "]", sb.toString());
      }
      for (Map<String, Object> g : (List<Map<String, Object>>) rec.get("huecos")) {
        List<Integer> gr = (List<Integer>) g.get("rango");
        campos.put("[" + gr.get(0) + ".." + gr.get(1) + "]", "HUECO sin acceso tipado"
            + (g.containsKey("lectores_genericos")
                ? "; lo leen rutinas genericas: " + g.get("lectores_genericos") : ""));
      }
      item.put("campos", campos);
      lista.add(item);
    }
    h.put("registros", lista);
  }

  // ---------- hallazgo: tabla de entidades ----------
  @SuppressWarnings("unchecked")
  private void hallazgoEntidades(List<Object> hallazgos, Map<String, Object> evidencia,
                                 Map<String, Object> evEnt, List<Map<String, Object>> structs,
                                 List<Map<String, Object>> canonicos) {
    Object tablaObj = evEnt.get("tabla");
    if (!(tablaObj instanceof Map<?, ?> tabla))
      return;
    int base = ((Number) tabla.get("base")).intValue();
    int stride = ((Number) tabla.get("registro_bytes")).intValue();
    int slots = ((Number) tabla.get("slots")).intValue();

    Map<String, Object> h = hallazgo(hallazgos, evidencia, "tabla-entidades",
        String.format("Tabla de %d entidades moviles en [%d..%d], registros de %d bytes",
            slots, base, base + stride * slots - 1, base), null);
    h.put("titulo", String.format("Tabla de %d entidades moviles en [%d..%d], registros de %d bytes",
        slots, base, base + stride * slots - 1, stride));

    // consolidated field meanings
    Map<Integer, List<String>> notas = new TreeMap<>();
    Map<String, Object> campos = (Map<String, Object>) tabla.get("campos");
    for (Map.Entry<String, Object> ce : campos.entrySet()) {
      Map<String, Object> c = (Map<String, Object>) ce.getValue();
      int off = ((Number) c.get("offset")).intValue();
      if (ce.getKey().equals("x"))
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("coordenada X en pantalla: pixel = " + c.get("formula"));
      else if (ce.getKey().equals("y"))
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("coordenada Y en pantalla: pixel = " + c.get("formula"));
      else
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("participa en la seleccion del grafico/frame del sprite");
    }
    // variant behaviour from the updater routine's structure
    List<Object> variantes = new ArrayList<>();
    for (Map<String, Object> st : structs) {
      if (((Number) st.get("base")).intValue() != base
          || ((Number) st.get("registro_bytes")).intValue() != stride)
        continue;
      Integer xOff = offsetOf(campos, "x"), yOff = offsetOf(campos, "y");
      for (Object vo : (List<Object>) st.get("variantes")) {
        Map<String, Object> v = (Map<String, Object>) vo;
        String cond = (String) v.get("condicion");
        List<Map<String, Object>> ramas = (List<Map<String, Object>>) (List<?>) v.get("ramas");
        long total = ramas.stream().mapToLong(r -> ((Number) r.get("veces")).longValue()).sum();
        if (!cond.contains("==") || total < 2000)
          continue;
        if (cond.endsWith("== 255")) {
          notas.computeIfAbsent(fieldOfCond(cond), k -> new ArrayList<>())
              .add("el valor 255 marca el FIN de la tabla");
          continue;
        }
        Map<String, Object> ve = new LinkedHashMap<>();
        ve.put("cuando", cond + " (en " + st.get("rutina") + ")");
        for (Map<String, Object> rama : ramas) {
          List<Integer> excl = (List<Integer>) rama.get("campos_exclusivos");
          ve.put(ve.containsKey("un_camino") ? "otro_camino" : "un_camino", armDescription(excl, xOff, yOff));
        }
        variantes.add(ve);
        notas.computeIfAbsent(fieldOfCond(cond), k -> new ArrayList<>())
            .add("sus bits eligen la variante de comportamiento");
      }
    }
    // proposed names from the relation analysis of the structures over this table
    for (Map<String, Object> st : structs) {
      if (((Number) st.get("base")).intValue() != base)
        continue;
      for (Object fo : (List<Object>) st.get("campos")) {
        Map<String, Object> f = (Map<String, Object>) fo;
        if (f.containsKey("nombre_propuesto")) {
          int off = ((Number) f.get("offset")).intValue() % stride;
          List<String> ns = notas.computeIfAbsent(off, k -> new ArrayList<>());
          String nombre = "nombre sugerido: " + f.get("nombre_propuesto");
          if (ns.stream().noneMatch(s -> s.startsWith("nombre sugerido")))
            ns.add(nombre);
        }
      }
    }
    Map<String, String> camposFinales = new LinkedHashMap<>();
    notas.forEach((off, ns) -> camposFinales.put("+" + off, String.join("; ", new LinkedHashSet<>(ns))));

    // the canonical record with the discriminated types (the final, skoolkit-like view)
    Map<String, Object> canon = canonicos.stream()
        .filter(r -> ((Number) r.get("base")).intValue() == base
            && ((Number) r.get("registro_bytes")).intValue() == stride)
        .findFirst().orElse(null);
    if (canon != null && canon.containsKey("tipos")) {
      Map<String, Object> disc = (Map<String, Object>) canon.get("discriminante");
      List<Object> tipos = (List<Object>) canon.get("tipos");
      int slotsObs = canon.containsKey("slots_con_datos_observados")
          ? ((Number) canon.get("slots_con_datos_observados")).intValue() : slots;
      h.put("titulo", String.format(
          "Tabla de entidades en [%d..%d]: %d slots de %d bytes, %d tipos de entidad",
          base, base + stride * Math.max(slots, slotsObs) - 1, Math.max(slots, slotsObs),
          stride, tipos.size()));
      if (canon.containsKey("slots_con_datos_observados"))
        h.put("slots_con_datos_observados", canon.get("slots_con_datos_observados"));
      if (canon.containsKey("terminador"))
        h.put("terminador", "el valor "
            + ((Map<String, Object>) canon.get("terminador")).get("valor") + " marca el fin de la tabla");
      h.put("tipo_de_entidad", "campo " + disc.get("campo") + " & " + disc.get("mascara")
          + " (" + disc.get("bits") + "); valores observados: "
          + ((List<Integer>) disc.get("valores_observados")).stream()
              .map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse(""));
      h.put("tipos", tipos.stream().map(to -> fichaTipo((Map<String, Object>) to)).toList());
      h.put("campos_comunes", camposFinales);
    } else {
      h.put("campos", camposFinales);
      if (!variantes.isEmpty())
        h.put("variantes_de_comportamiento", variantes.stream().distinct().limit(5).toList());
    }
    h.put("cargada_desde", tabla.get("cargada_por"));
    h.put("actualizada_por", tabla.get("actualizada_por"));
    h.put("evidencia", "tabla-entidades");
    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("deteccion", evEnt);
    ev.put("estructuras", structs.stream()
        .filter(st -> ((Number) st.get("base")).intValue() == base).toList());
    if (canon != null)
      ev.put("registro_canonico", canon);
    evidencia.put("tabla-entidades", ev);
  }

  /** one entity type rendered for the final layer: fields as readable one-liners. */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> fichaTipo(Map<String, Object> tipo) {
    Map<String, Object> tf = new LinkedHashMap<>();
    tf.put("valor", tipo.get("valor"));
    tf.put("nombre", tipo.get("nombre_propuesto"));
    if (tipo.containsKey("ocupa_registros"))
      tf.put("ocupa_registros", tipo.get("ocupa_registros"));
    if (tipo.containsKey("frames_observados"))
      tf.put("frames_observados", tipo.get("frames_observados"));
    tf.put("seleccionado_en", tipo.get("seleccionado_en"));
    Map<String, String> campos = new LinkedHashMap<>();
    for (Object fo : (List<Object>) tipo.get("campos")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("valores");
      StringBuilder sb = new StringBuilder();
      sb.append(f.getOrDefault("nombre_propuesto", "(sin nombre)"));
      sb.append("; val [").append(val.get(0)).append("..").append(val.get(1)).append("]");
      if (f.containsKey("descomposicion_bits"))
        sb.append("; sub-campos: ").append(String.join(", ", (List<String>) f.get("descomposicion_bits")));
      if (f.containsKey("campo_del_registro_siguiente"))
        sb.append("; es el +").append(f.get("campo_del_registro_siguiente"))
            .append(" del registro SIGUIENTE (registro extendido)");
      campos.put("+" + f.get("offset"), sb.toString());
    }
    tf.put("campos", campos);
    return tf;
  }

  @SuppressWarnings("unchecked")
  private static Integer offsetOf(Map<String, Object> campos, String key) {
    Object c = campos.get(key);
    return c instanceof Map<?, ?> m ? ((Number) ((Map<String, Object>) m).get("offset")).intValue() : null;
  }

  private static int fieldOfCond(String cond) {
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("campo \\+(\\d+)").matcher(cond);
    return m.find() ? Integer.parseInt(m.group(1)) : 0;
  }

  private static String armDescription(List<Integer> excl, Integer xOff, Integer yOff) {
    boolean x = xOff != null && excl.contains(xOff), y = yOff != null && excl.contains(yOff);
    if (y && !x)
      return "modifica la coordenada Y (movimiento vertical); usa +"
          + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
    if (x && !y)
      return "modifica la coordenada X (movimiento horizontal); usa +"
          + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
    return "usa exclusivamente los campos +"
        + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
  }

  // ---------- hallazgo: protagonista ----------
  @SuppressWarnings("unchecked")
  private static Map<String, Object> mejorProtagonista(Map<String, Object> evEnt) {
    List<Object> indiv = (List<Object>) evEnt.get("individuales");
    Map<String, Object> best = null;
    for (Object io : indiv) {
      Map<String, Object> i = (Map<String, Object>) io;
      if (((Number) i.get("confianza")).doubleValue() >= 0.6
          && (best == null || ((Number) i.get("confianza")).doubleValue() > ((Number) best.get("confianza")).doubleValue()))
        best = i;
    }
    return best;
  }

  private void hallazgoProtagonista(List<Object> hallazgos, Map<String, Object> evidencia,
                                    Map<String, Object> evEnt) throws SQLException {
    List<Object> indiv = (List<Object>) evEnt.get("individuales");
    Map<String, Object> best = mejorProtagonista(evEnt);
    if (best == null)
      return;
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "protagonista",
        "Sprite individual (protagonista): posicion fuera de la tabla de entidades", indiv);
    h.put("x", "mem[" + best.get("x_addr") + "], pixel = " + best.get("x_formula"));
    h.put("y", "mem[" + best.get("y_addr") + "], pixel = " + best.get("y_formula"));
    h.put("confianza", best.get("confianza"));
    // its main animation: the biggest contiguous sprite group drawn by the row renderer
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         ResultSet rs = c.createStatement().executeQuery(
             "SELECT base, last, veces FROM sprites_found ORDER BY base")) {
      int gLo = -1, gHi = -1;
      long gVeces = 0, bLo = -1, bHi = -1, bVeces = -1;
      while (rs.next()) {
        if (rs.getInt(1) == gHi + 1) {
          gHi = rs.getInt(2);
          gVeces += rs.getLong(3);
        } else {
          if (gVeces > bVeces) {
            bLo = gLo;
            bHi = gHi;
            bVeces = gVeces;
          }
          gLo = rs.getInt(1);
          gHi = rs.getInt(2);
          gVeces = rs.getLong(3);
        }
      }
      if (gVeces > bVeces) {
        bLo = gLo;
        bHi = gHi;
        bVeces = gVeces;
      }
      if (bLo >= 0)
        h.put("animacion_principal", "sprites contiguos [" + bLo + ".." + bHi + "] ("
            + ((bHi - bLo + 1) / 32) + " frames de 32 bytes, dibujados x" + bVeces + ")");
    }
  }

  // ---------- hallazgo: sprites ----------
  private void hallazgoSprites(List<Object> hallazgos, Map<String, Object> evidencia) throws SQLException {
    List<Object> full = sprites();
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "sprites",
        "Catalogo de sprites (datos graficos del cassette realmente dibujados)", full);
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      h.put("total", scalar(c, "SELECT COUNT(*) FROM sprites_found"));
      h.put("tamanio_tipico_bytes", scalar(c,
          "SELECT size FROM sprites_found GROUP BY size ORDER BY COUNT(*) DESC LIMIT 1"));
      List<Object> top = new ArrayList<>();
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT base, last, size, veces, methods FROM sprites_found WHERE size >= 8 ORDER BY veces DESC LIMIT 8")) {
        while (rs.next())
          top.add(Map.of("rango", List.of(rs.getInt(1), rs.getInt(2)), "bytes", rs.getInt(3),
              "veces_dibujado", rs.getInt(4), "rutinas", rs.getString(5)));
      }
      h.put("mas_dibujados", top);
      List<Object> items = new ArrayList<>();
      int[] itemZone = {Integer.MAX_VALUE, -1};
      long nItems = 0;
      try (ResultSet rs = c.createStatement().executeQuery(
          "SELECT base, last FROM sprites_found WHERE size <= 2")) {
        while (rs.next()) {
          itemZone[0] = Math.min(itemZone[0], rs.getInt(1));
          itemZone[1] = Math.max(itemZone[1], rs.getInt(2));
          nItems++;
        }
      }
      if (nItems > 0)
        h.put("items", "ademas hay " + nItems + " graficos chicos (1-2 bytes) en ["
            + itemZone[0] + ".." + itemZone[1] + "] (objetos/decoraciones)");
    }
  }

  // ---------- hallazgos: fuente, tablas, variables, rutinas, otras estructuras ----------
  private void hallazgoFuente(List<Object> hallazgos, Map<String, Object> evidencia) {
    Map<String, Object> f = fuente();
    if (f == null)
      return;
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "fuente",
        "Fuente de caracteres: la ROM del Spectrum, usada por la rutina de texto", f);
    h.put("rango", f.get("rango"));
    h.put("rutina_de_texto", f.get("rutina"));
  }

  private void hallazgoTablas(List<Object> hallazgos, Map<String, Object> evidencia) {
    List<Object> tablas = new ArrayList<>();
    for (int[] t : lookupTables)
      tablas.add(Map.of("rango", List.of(t[0], t[1]),
          "usada_por", methodsReading(addrReads, t[0], t[1])));
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "tablas-consulta",
        "Tablas de consulta estaticas (traducen indices a direcciones, ej. filas de pantalla)", tablas);
    h.put("tablas", tablas);
  }

  @SuppressWarnings("unchecked")
  private void hallazgoVariables(List<Object> hallazgos, Map<String, Object> evidencia) throws SQLException {
    List<Object> full = variables();
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "variables",
        "Variables dinamicas que gobiernan el dibujado (serie temporal completa en frame_cells)", full);
    List<Object> top = new ArrayList<>();
    for (Object zo : full)
      for (Object co : (List<Object>) ((Map<String, Object>) zo).get("celdas")) {
        Map<String, Object> cell = (Map<String, Object>) co;
        if (!cell.containsKey("coordenada") && ((Number) cell.get("valores_distintos")).intValue() >= 5)
          top.add(cell);
      }
    top.sort(Comparator.comparingInt(cm -> -((Number) ((Map<String, Object>) cm).get("valores_distintos")).intValue()));
    h.put("mas_activas", top.stream().limit(10).toList());
    h.put("nota", "las celdas de coordenadas estan en tabla-entidades y protagonista");
  }

  @SuppressWarnings("unchecked")
  private void hallazgoRutinas(List<Object> hallazgos, Map<String, Object> evidencia,
                               List<Object> full) throws SQLException {
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "rutinas",
        "Rutinas del juego clasificadas por lo que hacen", full);
    Map<String, String> frases = Map.of(
        "texto", "imprime texto con la fuente ROM",
        "render_fondo", "renderiza el fondo de la habitacion al buffer",
        "dibujo_sprites", "dibuja sprites de entidades",
        "dibujo_sprites_por_filas", "dibuja un sprite fila por fila (el protagonista)",
        "dibujo_atributos", "pinta colores (atributos)",
        "logica_estado", "logica de juego: actualiza variables y entidades",
        "dibujo", "dibuja en pantalla");
    List<Object> lista = new ArrayList<>();
    for (Object ro : full) {
      Map<String, Object> r = (Map<String, Object>) ro;
      lista.add(Map.of("rutina", r.get("nombre"),
          "hace", frases.getOrDefault((String) r.get("tipo"), (String) r.get("tipo"))));
    }
    h.put("lista", lista);
  }

  @SuppressWarnings("unchecked")
  private void hallazgoEstructuras(List<Object> hallazgos, Map<String, Object> evidencia,
                                   List<Map<String, Object>> structs, Map<String, Object> evEnt) {
    Object tabla = evEnt.get("tabla");
    int entBase = tabla instanceof Map<?, ?> tm ? ((Number) ((Map<String, Object>) tm).get("base")).intValue() : -1;
    List<Map<String, Object>> otras = structs.stream()
        .filter(st -> ((Number) st.get("base")).intValue() != entBase).toList();
    if (otras.isEmpty())
      return;
    Map<String, Object> h = hallazgo(hallazgos, evidencia, "otras-estructuras",
        "Otros arreglos de registros que las rutinas recorren", otras);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> st : otras)
      lista.add(Map.of("rutina", st.get("rutina"), "cursor", st.get("cursor"),
          "resumen", String.format("arreglo en [%d..%d], registros de %d bytes, %d campos",
              ((List<Number>) st.get("rango")).get(0).intValue(),
              ((List<Number>) st.get("rango")).get(1).intValue(),
              ((Number) st.get("registro_bytes")).intValue(),
              ((List<?>) st.get("campos")).size())));
    h.put("lista", lista);
  }

  private Map<String, Object> meta() throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("generado", LocalDateTime.now().toString());
    m.put("descripcion", "Modelo del juego deducido automaticamente del replay RZX instrumentado."
        + " Dos capas: 'hallazgos' son las conclusiones finales, agrupadas e interpretadas"
        + " (lo mas importante de cada cosa encontrada); 'evidencia' tiene los datos crudos"
        + " que las respaldan, asociados por el id del hallazgo (ver doc/MANUAL-ANALISIS.md)");
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
