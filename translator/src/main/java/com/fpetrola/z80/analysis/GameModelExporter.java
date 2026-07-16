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
import com.fpetrola.z80.analysis.query.Db;

import java.sql.SQLException;
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
    return c.startsWith("STATIC") || c.startsWith("mostly") || c.startsWith("MIXED");
  }

  /**
   * Two layers: {@code hallazgos} are FINAL, readable conclusions (already grouped and
   * interpreted, only the most important items inline); {@code evidencia} holds the raw
   * supporting data, keyed by the finding's id.
   */
  public void export(String outPath) throws Exception {
    Map<String, Object> evEnt = entities();
    StructFinder structFinder = new StructFinder(db, dbPath);
    List<Map<String, Object>> structs = structFinder.analyze(null);
    List<Map<String, Object>> canonicos = structFinder.canonical(structs);
    RebuildFinder rebuildFinder = new RebuildFinder(db, dbPath);
    List<Map<String, Object>> rebuilds = rebuildFinder.analyze();
    List<Map<String, Object>> drawingRebuilds = rebuildFinder.analyzeDrawing();
    List<Map<String, Object>> records = new RecordFinder(db, dbPath).analyze();
    List<Map<String, Object>> texts = new TextFinder(db, dbPath).analyze();
    Map<String, Object> segments = new SegmentFinder(db).data();
    List<Object> rutinasFull = routines();

    List<Object> findings = new ArrayList<>();
    Map<String, Object> evidence = new LinkedHashMap<>();

    findingScreen(findings, evidence);
    Map<String, Object> mapa = finding(findings, evidence, "memory-map",
        "Memory map: what lives in each range", null);
    mapa.put("zones", zones(evEnt));
    findingRebuild(findings, evidence, rebuilds, drawingRebuilds);
    findingRecords(findings, evidence, records);
    findingTexts(findings, evidence, texts);
    findingSegments(findings, evidence, segments);
    findingEntities(findings, evidence, evEnt, structs, canonicos);
    findingProtagonist(findings, evidence, evEnt);
    findingSprites(findings, evidence);
    findingFont(findings, evidence);
    findingTables(findings, evidence);
    findingVariables(findings, evidence);
    findingRoutines(findings, evidence, rutinasFull);
    findingStructures(findings, evidence, structs, evEnt);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("meta", meta());
    root.put("findings", findings);
    root.put("model", new ModelAssembler(db, plan, gfxRegions, lookupTables)
        .assemble(canonicos, records, rebuilds, rutinasFull, font(), bestProtagonist(evEnt), texts, segments));
    root.put("evidence", evidence);
    String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root);
    Files.writeString(Path.of(outPath), json);
    System.out.println("Game model -> " + outPath + " (" + json.length() / 1024 + " KB, "
        + findings.size() + " findings)");
  }

  private Map<String, Object> finding(List<Object> findings, Map<String, Object> evidence,
                                      String id, String title, Object evidenceData) {
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("id", id);
    h.put("title", title);
    findings.add(h);
    if (evidenceData != null) {
      evidence.put(id, evidenceData);
      h.put("evidence", id);
    }
    return h;
  }

  // ---------- hallazgo: pantalla y composicion ----------
  private void findingScreen(List<Object> findings, Map<String, Object> evidence) {
    Map<String, Object> h = finding(findings, evidence, "screen",
        "The screen is composed through buffer stages (double buffering)", buffers());
    h.put("pixels", pipeline(16384, 22527));
    h.put("colors", pipeline(22528, 23295));
  }

  /** chain of copies ending at the given screen slice, rendered as one readable line. */
  private String pipeline(int lo, int hi) {
    StringBuilder sb = new StringBuilder("screen [" + lo + ".." + hi + "]");
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
      sb.insert(0, "[" + best.srcMin() + ".." + srcHi + "] --copy x" + best.count() + "--> ");
      curLo = best.srcMin();
      curHi = srcHi;
    }
    return sb.toString();
  }

  // ---------- hallazgo: variables selectoras y su cluster de reconstruccion ----------
  @SuppressWarnings("unchecked")
  private void findingRebuild(List<Object> findings, Map<String, Object> evidence,
                                      List<Map<String, Object>> all,
                                      List<Map<String, Object>> drawing) {
    if (all.isEmpty() && drawing.isEmpty())
      return;
    List<Map<String, Object>> both = new ArrayList<>(all);
    both.addAll(drawing);
    Map<String, Object> h = finding(findings, evidence, "selector-rebuild",
        "Selector variables: they pick which content gets built (current screen/level) "
            + "and trigger the cluster that rebuilds it", both);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> f : drawing) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("selector", "mem[" + sel.get("cell") + "]"
          + (sel.containsKey("distinct_values")
              ? " (" + sel.get("distinct_values") + " valores distintos)" : ""));
      item.put("what_it_does", "rebuilds by DRAWING: indexes the static tables "
          + ((List<Object>) f.get("indexed_tables")).stream()
              .map(to -> String.valueOf(((Map<String, Object>) to).get("table_range"))).toList()
          + " and the consumers redraw walking them"
          + (f.containsKey("walked_data") ? " over " + f.get("walked_data") : ""));
      item.put("triggered_by", ((List<Object>) sel.get("written_by")).stream()
          .map(wo -> {
            Map<String, Object> w = (Map<String, Object>) wo;
            return w.get("routine") + " x" + w.get("times");
          }).toList());
      lista.add(item);
    }
    for (Map<String, Object> f : all) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      Map<String, Object> main = (Map<String, Object>)
          ((List<Object>) f.get("copies_indexed_by_selector")).get(0);
      Map<String, Object> tabla = (Map<String, Object>) main.get("indexed_table");
      List<Integer> dst = (List<Integer>) main.get("destination");
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("selector", "mem[" + sel.get("cell") + "]"
          + (sel.containsKey("distinct_values")
              ? " (" + sel.get("distinct_values") + " valores distintos)" : ""));
      item.put("what_it_does", "picks 1 of " + tabla.get("used_records") + " records of "
          + tabla.get("record_bytes") + " bytes at " + tabla.get("range")
          + " and copies it to [" + dst.get(0) + ".." + dst.get(1) + "]");
      item.put("formula", main.get("formula"));
      item.put("triggered_by", ((List<Object>) sel.get("written_by")).stream()
          .map(wo -> {
            Map<String, Object> w = (Map<String, Object>) wo;
            return w.get("routine") + " x" + w.get("times");
          }).toList());
      if (f.containsKey("trigger_note"))
        item.put("note", f.get("trigger_note"));
      if (f.containsKey("same_cadence_copies"))
        item.put("also_rebuilds", ((List<Object>) f.get("same_cadence_copies")).stream()
            .map(co -> {
              Map<String, Object> c = (Map<String, Object>) co;
              return "[" + ((List<Integer>) c.get("source")).get(0) + ".."
                  + ((List<Integer>) c.get("source")).get(1) + "] -> ["
                  + ((List<Integer>) c.get("destination")).get(0) + ".."
                  + ((List<Integer>) c.get("destination")).get(1) + "] (" + c.get("per_rebuild")
                  + "x per rebuild" + (c.containsKey("note") ? "; " + c.get("note") : "") + ")";
            }).toList());
      Map<String, Object> lectores = new LinkedHashMap<>();
      ((Map<String, Object>) f.get("destination_read_by")).forEach((rango, rs) ->
          lectores.put(rango, ((List<Object>) rs).stream()
              .map(ro -> (String) ((Map<String, Object>) ro).get("routine").toString()).toList()));
      item.put("content_consumed_later_by", lectores);
      lista.add(item);
    }
    h.put("selectors", lista);
  }

  // ---------- hallazgo: el registro singleton reconstruido, campo por campo ----------
  @SuppressWarnings("unchecked")
  private void findingRecords(List<Object> findings, Map<String, Object> evidence,
                                 List<Map<String, Object>> records) {
    if (records.isEmpty())
      return;
    Map<String, Object> h = finding(findings, evidence, "rebuilt-records",
        "The record the selector rebuilds, field by field (layout of the current content)",
        records);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> rec : records) {
      Map<String, Object> item = new LinkedHashMap<>();
      List<Integer> r = (List<Integer>) rec.get("range");
      item.put("range", r);
      item.put("selector", "mem[" + rec.get("selector") + "]");
      Map<String, String> campos = new LinkedHashMap<>();
      for (Map<String, Object> f : (List<Map<String, Object>>) rec.get("fields")) {
        List<Integer> fr = (List<Integer>) f.get("range");
        StringBuilder sb = new StringBuilder();
        if (f.containsKey("proposed_name"))
          sb.append(f.get("proposed_name"));
        if (f.containsKey("tags"))
          sb.append(sb.isEmpty() ? "" : " — ")
              .append(String.join("; ", (List<String>) f.get("tags")));
        if (sb.isEmpty())
          sb.append("read by ").append(f.getOrDefault("read_by", "?"));
        campos.put("[" + fr.get(0) + ".." + fr.get(1) + "]", sb.toString());
      }
      for (Map<String, Object> g : (List<Map<String, Object>>) rec.get("gaps")) {
        List<Integer> gr = (List<Integer>) g.get("range");
        campos.put("[" + gr.get(0) + ".." + gr.get(1) + "]", "GAP without typed access"
            + (g.containsKey("generic_readers")
                ? "; read by generic routines: " + g.get("generic_readers") : ""));
      }
      item.put("fields", campos);
      lista.add(item);
    }
    h.put("records", lista);
  }

  // ---------- hallazgo: el texto del juego (fuente + cadenas + nombres por registro) ----------
  @SuppressWarnings("unchecked")
  private void findingTexts(List<Object> findings, Map<String, Object> evidence,
                            List<Map<String, Object>> texts) {
    if (texts.isEmpty())
      return;
    Map<String, Object> h = finding(findings, evidence, "texts",
        "The game's text: the glyph table (font), where the strings live and what they say",
        texts);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> t : texts) {
      Map<String, Object> font = (Map<String, Object>) t.get("font");
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("font", "glyphs at " + font.get("range") + " read by " + font.get("read_by"));
      item.put("char_sources", t.get("char_sources"));
      if (t.containsKey("strings"))
        item.put("strings", ((List<Map<String, Object>>) t.get("strings")).stream()
            .map(s -> "@" + s.get("address") + " \"" + s.get("text") + "\"").toList());
      if (t.containsKey("record_texts")) {
        List<Object> rts = new ArrayList<>();
        for (Map<String, Object> rt : (List<Map<String, Object>>) t.get("record_texts")) {
          Map<String, Object> r = new LinkedHashMap<>();
          r.put("what", "field " + rt.get("field_offset") + " of the " + rt.get("record_bytes")
              + "-byte records at " + rt.get("template_base") + " is text (printed by "
              + rt.get("printed_by") + ")");
          r.put("per_record", ((List<Map<String, Object>>) rt.get("texts")).stream()
              .map(x -> "record " + x.get("record") + ": \"" + x.get("text") + "\"").toList());
          rts.add(r);
        }
        item.put("record_texts", rts);
      }
      if (t.containsKey("note"))
        item.put("note", t.get("note"));
      lista.add(item);
    }
    h.put("texts", lista);
  }

  // ---------- hallazgo: segmentacion de memoria por conjunto-de-acceso ----------
  @SuppressWarnings("unchecked")
  private void findingSegments(List<Object> findings, Map<String, Object> evidence,
                               Map<String, Object> seg) {
    List<Map<String, Object>> segs = (List<Map<String, Object>>) seg.get("segments");
    if (segs == null || segs.isEmpty())
      return;
    Map<String, Object> h = finding(findings, evidence, "memory-segments",
        String.format("Memory partitioned by which routines use it: %d segments, %d private "
                + "(candidates to become a class with a 0-indexed local array)",
            seg.get("segments_total"), seg.get("private")), seg);

    // owned buffers/structures: private read-write segments >1 byte, the encapsulation targets
    List<Object> owned = new ArrayList<>();
    List<Object> data = new ArrayList<>();
    for (Map<String, Object> s : segs) {
      List<Integer> r = (List<Integer>) s.get("range");
      if (!Boolean.TRUE.equals(s.get("private")))
        continue;
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("range", r);
      item.put("owner", String.join(" ", (List<String>) s.get("owners")));
      if (s.containsKey("read_by"))
        item.put("read_by", s.get("read_by"));
      if ("read-only".equals(s.get("kind")))
        data.add(item);
      else if (r.get(1) - r.get(0) >= 1)
        owned.add(item);
    }
    h.put("owned_buffers", owned);
    h.put("shared_data_tables", data);
    // what blocks clean encapsulation
    h.put("data_pipes", ((List<Object>) seg.get("pipes")).stream().limit(12).toList());
    h.put("shared_helpers", ((List<Object>) seg.get("spanners")).stream().limit(10).toList());
  }

  // ---------- hallazgo: tabla de entidades ----------
  @SuppressWarnings("unchecked")
  private void findingEntities(List<Object> findings, Map<String, Object> evidence,
                                 Map<String, Object> evEnt, List<Map<String, Object>> structs,
                                 List<Map<String, Object>> canonicos) {
    Object tablaObj = evEnt.get("table");
    if (!(tablaObj instanceof Map<?, ?> tabla))
      return;
    int base = ((Number) tabla.get("base")).intValue();
    int stride = ((Number) tabla.get("record_bytes")).intValue();
    int slots = ((Number) tabla.get("slots")).intValue();

    Map<String, Object> h = finding(findings, evidence, "entity-table",
        String.format("Table of %d mobile entities at [%d..%d], records of %d bytes",
            slots, base, base + stride * slots - 1, base), null);
    h.put("title", String.format("Table of %d mobile entities at [%d..%d], records of %d bytes",
        slots, base, base + stride * slots - 1, stride));

    // consolidated field meanings
    Map<Integer, List<String>> notas = new TreeMap<>();
    Map<String, Object> campos = (Map<String, Object>) tabla.get("fields");
    for (Map.Entry<String, Object> ce : campos.entrySet()) {
      Map<String, Object> c = (Map<String, Object>) ce.getValue();
      int off = ((Number) c.get("offset")).intValue();
      if (ce.getKey().equals("x"))
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("X coordinate on screen: pixel = " + c.get("formula"));
      else if (ce.getKey().equals("y"))
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("Y coordinate on screen: pixel = " + c.get("formula"));
      else
        notas.computeIfAbsent(off, k -> new ArrayList<>())
            .add("takes part in choosing the sprite graphic/frame");
    }
    // variant behaviour from the updater routine's structure
    List<Object> variantes = new ArrayList<>();
    for (Map<String, Object> st : structs) {
      if (((Number) st.get("base")).intValue() != base
          || ((Number) st.get("record_bytes")).intValue() != stride)
        continue;
      Integer xOff = offsetOf(campos, "x"), yOff = offsetOf(campos, "y");
      for (Object vo : (List<Object>) st.get("variants")) {
        Map<String, Object> v = (Map<String, Object>) vo;
        String cond = (String) v.get("condition");
        List<Map<String, Object>> ramas = (List<Map<String, Object>>) (List<?>) v.get("arms");
        long total = ramas.stream().mapToLong(r -> ((Number) r.get("times")).longValue()).sum();
        if (!cond.contains("==") || total < 2000)
          continue;
        if (cond.endsWith("== 255")) {
          notas.computeIfAbsent(fieldOfCond(cond), k -> new ArrayList<>())
              .add("value 255 marks the END of the table");
          continue;
        }
        Map<String, Object> ve = new LinkedHashMap<>();
        ve.put("cuando", cond + " (en " + st.get("routine") + ")");
        for (Map<String, Object> rama : ramas) {
          List<Integer> excl = (List<Integer>) rama.get("exclusive_fields");
          ve.put(ve.containsKey("un_camino") ? "otro_camino" : "un_camino", armDescription(excl, xOff, yOff));
        }
        variantes.add(ve);
        notas.computeIfAbsent(fieldOfCond(cond), k -> new ArrayList<>())
            .add("its bits choose the behavior variant");
      }
    }
    // proposed names from the relation analysis of the structures over this table
    for (Map<String, Object> st : structs) {
      if (((Number) st.get("base")).intValue() != base)
        continue;
      for (Object fo : (List<Object>) st.get("fields")) {
        Map<String, Object> f = (Map<String, Object>) fo;
        if (f.containsKey("proposed_name")) {
          int off = ((Number) f.get("offset")).intValue() % stride;
          List<String> ns = notas.computeIfAbsent(off, k -> new ArrayList<>());
          String nombre = "suggested name: " + f.get("proposed_name");
          if (ns.stream().noneMatch(s -> s.startsWith("suggested name")))
            ns.add(nombre);
        }
      }
    }
    Map<String, String> camposFinales = new LinkedHashMap<>();
    notas.forEach((off, ns) -> camposFinales.put("+" + off, String.join("; ", new LinkedHashSet<>(ns))));

    // the canonical record with the discriminated types (the final, skoolkit-like view)
    Map<String, Object> canon = canonicos.stream()
        .filter(r -> ((Number) r.get("base")).intValue() == base
            && ((Number) r.get("record_bytes")).intValue() == stride)
        .findFirst().orElse(null);
    if (canon != null && canon.containsKey("types")) {
      Map<String, Object> disc = (Map<String, Object>) canon.get("discriminant");
      List<Object> tipos = (List<Object>) canon.get("types");
      int slotsObs = canon.containsKey("slots_with_observed_data")
          ? ((Number) canon.get("slots_with_observed_data")).intValue() : slots;
      h.put("title", String.format(
          "Entity table at [%d..%d]: %d slots of %d bytes, %d entity types",
          base, base + stride * Math.max(slots, slotsObs) - 1, Math.max(slots, slotsObs),
          stride, tipos.size()));
      if (canon.containsKey("slots_with_observed_data"))
        h.put("slots_with_observed_data", canon.get("slots_with_observed_data"));
      if (canon.containsKey("terminator"))
        h.put("terminator", "value "
            + ((Map<String, Object>) canon.get("terminator")).get("value") + " marks the end of the table");
      h.put("entity_type_field", "campo " + disc.get("field") + " & " + disc.get("mask")
          + " (" + disc.get("bits") + "); valores observados: "
          + ((List<Integer>) disc.get("observed_values")).stream()
              .map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse(""));
      h.put("types", tipos.stream().map(to -> fichaTipo((Map<String, Object>) to)).toList());
      h.put("common_fields", camposFinales);
    } else {
      h.put("fields", camposFinales);
      if (!variantes.isEmpty())
        h.put("behavior_variants", variantes.stream().distinct().limit(5).toList());
    }
    h.put("loaded_from", tabla.get("loaded_by"));
    h.put("updated_by", tabla.get("updated_by"));
    h.put("evidence", "entity-table");
    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("detection", evEnt);
    ev.put("structures", structs.stream()
        .filter(st -> ((Number) st.get("base")).intValue() == base).toList());
    if (canon != null)
      ev.put("canonical_record", canon);
    evidence.put("entity-table", ev);
  }

  /** one entity type rendered for the final layer: fields as readable one-liners. */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> fichaTipo(Map<String, Object> tipo) {
    Map<String, Object> tf = new LinkedHashMap<>();
    tf.put("value", tipo.get("value"));
    tf.put("name", tipo.get("proposed_name"));
    if (tipo.containsKey("spans_records"))
      tf.put("spans_records", tipo.get("spans_records"));
    if (tipo.containsKey("observed_frames"))
      tf.put("observed_frames", tipo.get("observed_frames"));
    tf.put("selected_in", tipo.get("selected_in"));
    Map<String, String> campos = new LinkedHashMap<>();
    for (Object fo : (List<Object>) tipo.get("fields")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("values");
      StringBuilder sb = new StringBuilder();
      sb.append(f.getOrDefault("proposed_name", "(unnamed)"));
      sb.append("; val [").append(val.get(0)).append("..").append(val.get(1)).append("]");
      if (f.containsKey("bit_decomposition"))
        sb.append("; sub-fields: ").append(String.join(", ", (List<String>) f.get("bit_decomposition")));
      if (f.containsKey("field_of_next_record"))
        sb.append("; it is +").append(f.get("field_of_next_record"))
            .append(" of the NEXT record (extended record)");
      campos.put("+" + f.get("offset"), sb.toString());
    }
    tf.put("fields", campos);
    return tf;
  }

  @SuppressWarnings("unchecked")
  private static Integer offsetOf(Map<String, Object> campos, String key) {
    Object c = campos.get(key);
    return c instanceof Map<?, ?> m ? ((Number) ((Map<String, Object>) m).get("offset")).intValue() : null;
  }

  private static int fieldOfCond(String cond) {
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("field \\+(\\d+)").matcher(cond);
    return m.find() ? Integer.parseInt(m.group(1)) : 0;
  }

  private static String armDescription(List<Integer> excl, Integer xOff, Integer yOff) {
    boolean x = xOff != null && excl.contains(xOff), y = yOff != null && excl.contains(yOff);
    if (y && !x)
      return "modifies the Y coordinate (vertical movement); uses +"
          + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
    if (x && !y)
      return "modifies the X coordinate (horizontal movement); uses +"
          + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
    return "uses only fields +"
        + excl.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse("");
  }

  // ---------- hallazgo: protagonista ----------
  @SuppressWarnings("unchecked")
  private static Map<String, Object> bestProtagonist(Map<String, Object> evEnt) {
    List<Object> indiv = (List<Object>) evEnt.get("individual");
    Map<String, Object> best = null;
    for (Object io : indiv) {
      Map<String, Object> i = (Map<String, Object>) io;
      // the pair must place the drawn cluster in the MAJORITY of its routine's frames
      if (((Number) i.get("confidence")).doubleValue() >= 0.5
          && (best == null || ((Number) i.get("confidence")).doubleValue() > ((Number) best.get("confidence")).doubleValue()))
        best = i;
    }
    return best;
  }

  private void findingProtagonist(List<Object> findings, Map<String, Object> evidence,
                                    Map<String, Object> evEnt) throws SQLException {
    List<Object> indiv = (List<Object>) evEnt.get("individual");
    Map<String, Object> best = bestProtagonist(evEnt);
    if (best == null)
      return;
    Map<String, Object> h = finding(findings, evidence, "protagonist",
        "Individual sprite (protagonist): position outside the entity table", indiv);
    h.put("x", "mem[" + best.get("x_addr") + "], pixel = " + best.get("x_formula"));
    h.put("y", "mem[" + best.get("y_addr") + "], pixel = " + best.get("y_formula"));
    h.put("confidence", best.get("confidence"));
    // its main animation: the biggest contiguous sprite group drawn by the row renderer
    try (Db q = new Db(dbPath)) {
      long[] g = {-1, -1, 0};   // current contiguous group: lo, hi, veces
      long[] bg = {-1, -1, -1}; // best group so far: lo, hi, veces
      q.forEach("SELECT base, last, veces FROM sprites_found ORDER BY base", rs -> {
        if (rs.getInt(1) == g[1] + 1) {
          g[1] = rs.getInt(2);
          g[2] += rs.getLong(3);
        } else {
          if (g[2] > bg[2]) {
            bg[0] = g[0];
            bg[1] = g[1];
            bg[2] = g[2];
          }
          g[0] = rs.getInt(1);
          g[1] = rs.getInt(2);
          g[2] = rs.getLong(3);
        }
      });
      if (g[2] > bg[2]) {
        bg[0] = g[0];
        bg[1] = g[1];
        bg[2] = g[2];
      }
      if (bg[0] >= 0)
        h.put("main_animation", "contiguous sprites [" + bg[0] + ".." + bg[1] + "] ("
            + ((bg[1] - bg[0] + 1) / 32) + " frames of 32 bytes, drawn x" + bg[2] + ")");
    }
  }

  // ---------- hallazgo: sprites ----------
  private void findingSprites(List<Object> findings, Map<String, Object> evidence) throws SQLException {
    List<Object> full = sprites();
    Map<String, Object> h = finding(findings, evidence, "sprites",
        "Sprite catalog (cassette graphics data actually drawn)", full);
    try (Db q = new Db(dbPath)) {
      h.put("total", q.scalar("SELECT COUNT(*) FROM sprites_found", -1));
      h.put("typical_size_bytes", q.scalar(
          "SELECT size FROM sprites_found GROUP BY size ORDER BY COUNT(*) DESC LIMIT 1", -1));
      h.put("most_drawn", q.query(
          "SELECT base, last, size, veces, methods FROM sprites_found WHERE size >= 8 ORDER BY veces DESC LIMIT 8",
          rs -> (Object) Map.of("range", List.of(rs.getInt(1), rs.getInt(2)), "bytes", rs.getInt(3),
              "times_drawn", rs.getInt(4), "routines", rs.getString(5))));
      int[] itemZone = {Integer.MAX_VALUE, -1};
      long[] nItems = {0};
      q.forEach("SELECT base, last FROM sprites_found WHERE size <= 2", rs -> {
        itemZone[0] = Math.min(itemZone[0], rs.getInt(1));
        itemZone[1] = Math.max(itemZone[1], rs.getInt(2));
        nItems[0]++;
      });
      if (nItems[0] > 0)
        h.put("items", "there are also " + nItems[0] + " small graphics (1-2 bytes) at ["
            + itemZone[0] + ".." + itemZone[1] + "] (objects/decorations)");
    }
  }

  // ---------- hallazgos: fuente, tablas, variables, rutinas, otras estructuras ----------
  private void findingFont(List<Object> findings, Map<String, Object> evidence) {
    Map<String, Object> f = font();
    if (f == null)
      return;
    Map<String, Object> h = finding(findings, evidence, "font",
        "Character font: the Spectrum ROM, used by the text routine", f);
    h.put("range", f.get("range"));
    h.put("text_routine", f.get("routine"));
  }

  private void findingTables(List<Object> findings, Map<String, Object> evidence) {
    List<Object> tablas = new ArrayList<>();
    for (int[] t : lookupTables)
      tablas.add(Map.of("range", List.of(t[0], t[1]),
          "used_by", methodsReading(addrReads, t[0], t[1])));
    Map<String, Object> h = finding(findings, evidence, "lookup-tables",
        "Static lookup tables (they translate indices to addresses, e.g. screen rows)", tablas);
    h.put("tablas", tablas);
  }

  @SuppressWarnings("unchecked")
  private void findingVariables(List<Object> findings, Map<String, Object> evidence) throws SQLException {
    List<Object> full = variables();
    Map<String, Object> h = finding(findings, evidence, "variables",
        "Dynamic variables that drive the drawing (full time series in frame_cells)", full);
    List<Object> top = new ArrayList<>();
    for (Object zo : full)
      for (Object co : (List<Object>) ((Map<String, Object>) zo).get("cells")) {
        Map<String, Object> cell = (Map<String, Object>) co;
        if (!cell.containsKey("coordinate") && ((Number) cell.get("distinct_values")).intValue() >= 5)
          top.add(cell);
      }
    top.sort(Comparator.comparingInt(cm -> -((Number) ((Map<String, Object>) cm).get("distinct_values")).intValue()));
    h.put("most_active", top.stream().limit(10).toList());
    h.put("note", "the coordinate cells live in entity-table and protagonist");
  }

  @SuppressWarnings("unchecked")
  private void findingRoutines(List<Object> findings, Map<String, Object> evidence,
                               List<Object> full) throws SQLException {
    Map<String, Object> h = finding(findings, evidence, "routines",
        "Game routines classified by what they do", full);
    Map<String, String> frases = Map.of(
        "text", "prints text with the ROM font",
        "background_render", "renders the room background into the buffer",
        "sprite_drawing", "draws entity sprites",
        "row_by_row_sprite_drawing", "draws a sprite row by row (the protagonist)",
        "attribute_drawing", "paints colors (attributes)",
        "state_logic", "game logic: updates variables and entities",
        "drawing", "draws to the screen");
    List<Object> lista = new ArrayList<>();
    for (Object ro : full) {
      Map<String, Object> r = (Map<String, Object>) ro;
      lista.add(Map.of("routine", r.get("name"),
          "does", frases.getOrDefault((String) r.get("type"), (String) r.get("type"))));
    }
    h.put("list", lista);
  }

  @SuppressWarnings("unchecked")
  private void findingStructures(List<Object> findings, Map<String, Object> evidence,
                                   List<Map<String, Object>> structs, Map<String, Object> evEnt) {
    Object tabla = evEnt.get("table");
    int entBase = tabla instanceof Map<?, ?> tm ? ((Number) ((Map<String, Object>) tm).get("base")).intValue() : -1;
    List<Map<String, Object>> otras = structs.stream()
        .filter(st -> ((Number) st.get("base")).intValue() != entBase).toList();
    if (otras.isEmpty())
      return;
    Map<String, Object> h = finding(findings, evidence, "other-structures",
        "Other record arrays the routines walk", otras);
    List<Object> lista = new ArrayList<>();
    for (Map<String, Object> st : otras)
      lista.add(Map.of("routine", st.get("routine"), "cursor", st.get("cursor"),
          "summary", String.format("array at [%d..%d], records of %d bytes, %d fields",
              ((List<Number>) st.get("range")).get(0).intValue(),
              ((List<Number>) st.get("range")).get(1).intValue(),
              ((Number) st.get("record_bytes")).intValue(),
              ((List<?>) st.get("fields")).size())));
    h.put("list", lista);
  }

  private Map<String, Object> meta() throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("generated", LocalDateTime.now().toString());
    m.put("description", "Game model deduced automatically from the instrumented RZX replay."
        + " Layers: 'findings' are the final, interpreted conclusions (the most important"
        + " part of each discovery); 'model' is the formal machine-readable layer meant to"
        + " drive source transformations; 'evidence' holds the raw data backing them,"
        + " keyed by finding id (see doc/MANUAL-ANALISIS.md)");
    try (Db q = new Db(dbPath)) {
      m.put("draws", q.scalar("SELECT COUNT(*) FROM sprite_draws", -1));
      m.put("frames_with_draws", q.scalar("SELECT COUNT(DISTINCT frame) FROM sprite_draws", -1));
      m.put("sprites_found", q.scalar("SELECT COUNT(*) FROM sprites_found", -1));
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
      b.put("range", List.of(r.lo(), r.hi()));
      b.put("delta_to_screen", r.delta());
      boolean estatica = explainer.classifyRange(r.lo(), r.hi()).startsWith("STATIC");
      b.put("type", estatica ? "static_data_copied_to_screen" : "buffer");
      List<Object> feeds = new ArrayList<>();
      for (AnalysisDB.Bulk bk : db.bulks.values()) {
        int dstHi = bk.dstMax() + Math.max(0, bk.lenMax() - 1);
        if (dstHi < r.lo() || bk.dstMin() > r.hi() || bk.srcMin() >= r.lo() && bk.srcMin() <= r.hi())
          continue;
        feeds.add(Map.of("copia_desde", List.of(bk.srcMin(), bk.srcMax() + bk.lenMax() - 1),
            "times", bk.count(), "site", bk.pc()));
      }
      if (!feeds.isEmpty())
        b.put("alimentado_por", feeds);
      out.add(b);
    }
    return out;
  }

  private List<Object> zones(Map<String, Object> entidades) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (int[] g : gfxRegions) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("range", List.of(g[0], g[1]));
      z.put("type", "sprite_graphics");
      z.put("classification", explainer.classifyRange(g[0], g[1]));
      z.put("read_by", methodsReading(valReads, g[0], g[1]));
      out.add(z);
    }
    for (int[] t : lookupTables) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("range", List.of(t[0], t[1]));
      z.put("type", "address_lookup_table");
      z.put("used_by", methodsReading(addrReads, t[0], t[1]));
      out.add(z);
    }
    Map<String, Object> f = font();
    if (f != null) {
      Map<String, Object> z = new LinkedHashMap<>(f);
      z.put("type", "rom_text_font");
      out.add(z);
    }
    Object tabla = entidades.get("table");
    if (tabla instanceof Map<?, ?> tm) {
      Map<String, Object> z = new LinkedHashMap<>();
      int base = ((Number) tm.get("base")).intValue();
      int fin = base + ((Number) tm.get("record_bytes")).intValue() * ((Number) tm.get("slots")).intValue() - 1;
      z.put("range", List.of(base, fin));
      z.put("type", "entity_table");
      out.add(z);
    }
    for (int[] rg : plan.watchRanges()) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("range", List.of(rg[0], rg[1]));
      z.put("type", "dynamic_variables");
      z.put("note", "feed drawing addresses; time series in frame_cells");
      out.add(z);
    }
    out.sort(Comparator.comparingInt(z -> ((Number) ((List<?>) z.get("range")).get(0)).intValue()));
    return new ArrayList<>(out);
  }

  private List<String> methodsReading(Set<Integer> readSites, int lo, int hi) {
    Set<String> ms = new TreeSet<>();
    for (int s : readSites) {
      AnalysisDB.Stat r = db.reads.get(s);
      if (r.addrMax() >= lo && r.addrMin() <= hi)
        ms.add(db.nameOf(s));
    }
    return new ArrayList<>(ms);
  }

  private List<Object> sprites() throws SQLException {
    // typical drawn size per sprite: vote over (gfx, w, h) of pixel draws
    List<int[]> bases = new ArrayList<>();
    List<Object> out = new ArrayList<>();
    Map<Integer, Map<String, Integer>> sizeVotes = new HashMap<>();
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT base, last FROM sprites_found ORDER BY base",
          rs -> bases.add(new int[]{rs.getInt(1), rs.getInt(2)}));
      q.forEach("SELECT gfx, w, h, COUNT(*) FROM sprite_draws WHERE kind='P' AND gfx>=0 GROUP BY gfx, w, h", rs -> {
        int[] sp = spriteContaining(bases, rs.getInt(1));
        if (sp != null)
          sizeVotes.computeIfAbsent(sp[0], k -> new HashMap<>())
              .merge(rs.getInt(2) + "x" + rs.getInt(3), rs.getInt(4), Integer::sum);
      });
      q.forEach("SELECT base, last, size, veces, frame_first, frame_last, methods FROM sprites_found ORDER BY base", rs -> {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("base", rs.getInt(1));
        s.put("end", rs.getInt(2));
        s.put("bytes", rs.getInt(3));
        s.put("times_drawn", rs.getInt(4));
        s.put("frames", List.of(rs.getInt(5), rs.getInt(6)));
        s.put("routines", Arrays.asList(rs.getString(7).split(" ")));
        Map<String, Integer> votes = sizeVotes.get(rs.getInt(1));
        if (votes != null)
          s.put("typical_draw", Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey());
        out.add(s);
      });
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
  private Map<String, Object> font() {
    for (int s : valReads) {
      AnalysisDB.Stat r = db.reads.get(s);
      if (r.addrMin() < 16384) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("range", List.of(r.addrMin(), Math.min(16383, r.addrMax())));
        f.put("routine", db.nameOf(s));
        f.put("note", "the site's aggregated range may mix zones; the <16384 part is ROM");
        return f;
      }
    }
    return null;
  }

  private Map<String, Object> entities() throws SQLException {
    Map<String, Object> out = new LinkedHashMap<>();
    record Pair(int xAddr, String xT, int xOff, int yAddr, String yT, int yOff, double rate) {
    }
    List<Pair> strong = new ArrayList<>(), weak = new ArrayList<>();
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT x_addr,x_transform,x_off,y_addr,y_transform,y_off,rate FROM coord_pairs ORDER BY x_addr", rs -> {
        Pair p = new Pair(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
            rs.getString(5), rs.getInt(6), rs.getDouble(7));
        (p.rate() >= 0.10 ? strong : weak).add(p);
      });
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
    // two points always form a degenerate "run" with some stride: a credible table needs
    // at least 3 validated slots — otherwise the pairs are individuals (e.g. the protagonist
    // record's own fields would masquerade as a table and swallow the protagonist finding)
    if (stride > 0 && runLen >= 3) {
      int base = runStart, slots = runLen;
      List<Object> loaders = new ArrayList<>();
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi >= runStart && b.dstMin() <= runStart + (runLen - 1) * stride) {
          base = Math.min(base, b.dstMin());
          slots = Math.max(slots, (dstHi - base + 1) / stride);
          loaders.add(Map.of("from", List.of(b.srcMin(), b.srcMax() + b.lenMax() - 1),
              "times", b.count(), "site", b.pc()));
        }
      }
      Map<String, Object> tabla = new LinkedHashMap<>();
      tabla.put("base", base);
      tabla.put("record_bytes", stride);
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
        slotList.add(Map.of("x_addr", p.xAddr(), "y_addr", p.yAddr(), "confidence", p.rate()));
      }
      // graphics selector: dynamic cells feeding the ADDRESS of the sprite-data reads
      for (int off : selectorOffsets(base, stride, slots))
        campos.put("graphic_selector_offset_" + off, Map.of("offset", off,
            "note", "picks which sprite/frame is drawn"));
      tabla.put("fields", campos);
      tabla.put("validated_slots", slotList);
      if (!loaders.isEmpty())
        tabla.put("loaded_by", loaders);
      List<String> updaters = new ArrayList<>(new TreeSet<>(
          db.writersIntersecting(base, base + slots * stride - 1).stream()
              .map(w -> db.nameOf(w.pc())).toList()));
      tabla.put("updated_by", updaters);
      out.put("table", tabla);
    }
    List<Object> individuales = new ArrayList<>();
    for (Pair p : strong)
      if (!inRun.contains(p.xAddr()))
        individuales.add(Map.of("x_addr", p.xAddr(), "x_formula", p.xT() + (p.xOff() != 0 ? "+" + p.xOff() : ""),
            "y_addr", p.yAddr(), "y_formula", p.yT() + (p.yOff() != 0 ? "+" + p.yOff() : ""),
            "confidence", p.rate()));
    out.put("individual", individuales);
    if (!weak.isEmpty())
      out.put("weak_pairs_possible_noise", weak.stream()
          .map(p -> Map.of("x_addr", p.xAddr(), "y_addr", p.yAddr(), "confidence", p.rate()))
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
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT addr, axis FROM coord_cells",
          rs -> coordAxis.putIfAbsent(rs.getInt(1), rs.getString(2)));
      q.forEach("SELECT addr, MIN(val), MAX(val), COUNT(DISTINCT val) FROM frame_cells GROUP BY addr",
          rs -> stats.put(rs.getInt(1), new int[]{rs.getInt(2), rs.getInt(3), rs.getInt(4)}));
    }
    List<Object> out = new ArrayList<>();
    for (int[] rg : plan.watchRanges()) {
      Map<String, Object> z = new LinkedHashMap<>();
      z.put("range", List.of(rg[0], rg[1]));
      List<Object> celdas = new ArrayList<>();
      for (int a = rg[0]; a <= rg[1]; a++) {
        int[] st = stats.get(a);
        if (st == null || st[2] < 2)
          continue; // constante toda la partida: sin interes
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("addr", a);
        cell.put("min", st[0]);
        cell.put("max", st[1]);
        cell.put("distinct_values", st[2]);
        if (coordAxis.containsKey(a))
          cell.put("coordinate", coordAxis.get(a));
        celdas.add(cell);
      }
      z.put("cells", celdas);
      out.add(z);
    }
    return out;
  }

  private List<Object> routines() throws SQLException {
    // typical pixel/attr cluster per method + path counts, from sprite_draws
    Map<Integer, Map<String, long[]>> byMethodKind = new HashMap<>(); // m -> kind -> {bestCount, w, h, total}
    Map<Integer, Integer> pathsOf = new HashMap<>();
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT method, kind, w, h, COUNT(*) FROM sprite_draws GROUP BY 1,2,3,4", rs -> {
        long n = rs.getLong(5);
        long[] cur = byMethodKind.computeIfAbsent(rs.getInt(1), k -> new HashMap<>())
            .computeIfAbsent(rs.getString(2), k -> new long[]{0, 0, 0, 0});
        cur[3] += n;
        if (n > cur[0]) {
          cur[0] = n;
          cur[1] = rs.getInt(3);
          cur[2] = rs.getInt(4);
        }
      });
      q.forEach("SELECT method, COUNT(DISTINCT path) FROM sprite_draws GROUP BY 1",
          rs -> pathsOf.put(rs.getInt(1), rs.getInt(2)));
    }
    // which methods read the ROM font (text renderers)
    Set<String> romReaders = new HashSet<>();
    for (int s : valReads)
      if (db.reads.get(s).addrMin() < 16384)
        romReaders.add(db.nameOf(s));

    List<Object> out = new ArrayList<>();
    for (Map.Entry<Integer, String> me : plan.drawMethods().entrySet()) {
      Map<String, Object> r = new LinkedHashMap<>();
      r.put("entry", me.getKey());
      r.put("name", me.getValue());
      Map<String, long[]> kinds = byMethodKind.getOrDefault(me.getKey(), Map.of());
      long[] p = kinds.get("P"), a = kinds.get("A");
      String tipo;
      if (p != null && romReaders.contains(me.getValue()))
        tipo = "text";
      else if (p != null && p[2] == 1)
        tipo = "row_by_row_sprite_drawing";
      else if (p != null && p[1] >= 128)
        tipo = "background_render";
      else if (p != null)
        tipo = "sprite_drawing";
      else if (a != null)
        tipo = "attribute_drawing";
      else
        tipo = "drawing";
      r.put("type", tipo);
      Map<String, Object> ev = new LinkedHashMap<>();
      if (p != null) {
        ev.put("pixel_draws", p[3]);
        ev.put("typical_pixels", p[1] + "x" + p[2]);
      }
      if (a != null) {
        ev.put("attribute_draws", a[3]);
        ev.put("typical_attributes", a[1] + "x" + a[2]);
      }
      Integer paths = pathsOf.get(me.getKey());
      if (paths != null)
        ev.put("execution_paths", paths);
      r.put("evidence", ev);
      out.add(r);
    }
    // entity-table updaters that are not draw methods = game logic
    Set<String> drawNames = new HashSet<>(plan.drawMethods().values());
    Map<String, Long> logic = new TreeMap<>();
    for (int[] rg : plan.watchRanges())
      for (AnalysisDB.Stat w : db.writersIntersecting(rg[0], rg[1])) {
        String m = db.nameOf(w.pc());
        if (!drawNames.contains(m))
          logic.merge(m, w.count(), Long::sum);
      }
    logic.forEach((m, n) -> out.add(Map.of("name", m, "type", "state_logic",
        "evidence", Map.of("writes_to_variables", n))));
    return out;
  }

  private List<Object> episodes() throws SQLException {
    try (Db q = new Db(dbPath)) {
      return q.query("SELECT method, path, count, gfx_lo, gfx_hi, cond FROM episodes ORDER BY count DESC", rs -> {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("routine", "$" + rs.getInt(1));
        e.put("path", String.format("%08x", rs.getInt(2)));
        e.put("times", rs.getInt(3));
        if (rs.getInt(4) >= 0)
          e.put("gfx", List.of(rs.getInt(4), rs.getInt(5)));
        String cond = rs.getString(6);
        if (cond != null && !cond.isEmpty())
          e.put("condition", cond);
        return (Object) e;
      });
    }
  }

}
