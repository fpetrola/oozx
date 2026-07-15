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

import java.util.*;

/**
 * The FORMAL layer of the game model ("model" in game-model.json): the same deductions
 * that the human-readable findings describe, organised the way a skoolkit disassembly
 * organises them — every address range TYPED and NAMED, every variable/structure/record
 * with its fields, every routine with what it reads and writes — so that a later pass
 * can READ this JSON and TRANSFORM the generated Java source: rename things, turn the
 * entity union into classes with subtypes, replace mem[..] with named properties, split
 * the flat memory into segments only visible to the code that touches them, extract
 * graphics to resource files. Sections:
 * <ul>
 *   <li><b>address_space</b>: the typed memory map (screen, buffers, graphics,
 *       lookup tables, structure arrays, singleton records, template catalogues,
 *       variables), each with a stable proposed name;</li>
 *   <li><b>variables</b>: named cells (selectors, protagonist coordinates) with who
 *       reads/writes them;</li>
 *   <li><b>estructuras</b>: the canonical records with their discriminated types
 *       (the input for entity classes + polymorphism);</li>
 *   <li><b>records</b>: singleton records with their field layout (the input for
 *       "memory range → class with named properties");</li>
 *   <li><b>rutinas</b>: role classification + which named regions each one reads and
 *       writes (the input for moving code into the classes it belongs to);</li>
 *   <li><b>segmentos</b>: the inverse view — per region, the routines that touch it,
 *       with an encapsulation proposal when few routines see it;</li>
 *   <li><b>refactor_suggestions</b>: mechanical hints derived from the above.</li>
 * </ul>
 * Names are role-based and generic (nothing game-specific); the evidence behind each
 * element stays in the "findings"/"evidence" layers.
 */
class ModelAssembler {
  private final AnalysisDB db;
  private final CoordinateFinder.Plan plan;
  private final List<int[]> gfxRegions, lookupTables;

  ModelAssembler(AnalysisDB db, CoordinateFinder.Plan plan,
                 List<int[]> gfxRegions, List<int[]> lookupTables) {
    this.db = db;
    this.plan = plan;
    this.gfxRegions = gfxRegions;
    this.lookupTables = lookupTables;
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> assemble(List<Map<String, Object>> canonicos,
                               List<Map<String, Object>> records,
                               List<Map<String, Object>> rebuilds,
                               List<Object> rutinasClasificadas,
                               Map<String, Object> fuente,
                               Map<String, Object> protagonista) {
    List<Map<String, Object>> espacio = new ArrayList<>();

    // screen and buffers, named by where their content lands
    Set<String> seen = new HashSet<>();
    int nBufPix = 0, nBufAttr = 0;
    for (CoordinateFinder.Region r : plan.regions()) {
      if (!seen.add(r.lo() + ".." + r.hi()))
        continue;
      boolean atributos = r.lo() + r.delta() >= 22528;
      String nombre;
      if (r.delta() == 0)
        nombre = r.hi() >= 22528 && r.lo() < 22528 ? "screen"
            : atributos ? "screen_attributes" : "screen_pixels";
      else
        nombre = (atributos ? "attribute_buffer_" + ++nBufAttr : "pixel_buffer_" + ++nBufPix);
      Map<String, Object> e = entrada(espacio, nombre, r.lo(), r.hi(),
          r.delta() == 0 ? "screen_hardware" : "work_buffer", "screen");
      if (r.delta() != 0)
        e.put("delta_to_screen", r.delta());
    }
    if (fuente != null) {
      List<Integer> fr = (List<Integer>) fuente.get("range");
      entrada(espacio, "text_font", fr.get(0), fr.get(1), "graphics_data", "font");
    }
    int n = 0;
    for (int[] g : gfxRegions)
      entrada(espacio, "graphics_" + ++n, g[0], g[1], "graphics_data", "sprites");
    n = 0;
    for (int[] t : lookupTables)
      entrada(espacio, "lookup_table_" + ++n, t[0], t[1], "lookup_table", "lookup-tables");

    // structure arrays (canonical records); the discriminated one is "entities"
    List<Map<String, Object>> estructuras = new ArrayList<>();
    n = 0;
    for (Map<String, Object> c : canonicos) {
      List<Integer> r = (List<Integer>) c.get("range");
      String nombre = c.containsKey("types") ? "entities" : "record_array_" + ++n;
      Map<String, Object> e = entrada(espacio, nombre, r.get(0), r.get(1),
          "record_array", "entity-table");
      e.put("record_bytes", c.get("record_bytes"));
      Map<String, Object> est = new LinkedHashMap<>();
      est.put("name", nombre);
      est.putAll(c);
      estructuras.add(est);
    }

    // singleton records + their template catalogue + their selector variable
    List<Map<String, Object>> vars = new ArrayList<>();
    n = 0;
    for (Map<String, Object> rec : records) {
      n++;
      List<Integer> r = (List<Integer>) rec.get("range");
      entrada(espacio, "current_record_" + n, r.get(0), r.get(1),
          "singleton_record", "rebuilt-records");
      Map<String, Object> t = (Map<String, Object>) rec.get("template");
      List<Integer> tr = (List<Integer>) t.get("range");
      Map<String, Object> cat = entrada(espacio, "record_catalog_" + n, tr.get(0), tr.get(1),
          "static_catalog", "selector-rebuild");
      cat.put("record_bytes", t.get("record_bytes"));
      cat.put("records", t.get("used_records"));
      int cell = (int) rec.get("selector");
      entrada(espacio, "record_selector_" + n, cell, cell, "variable", "selector-rebuild");
      Map<String, Object> v = new LinkedHashMap<>();
      v.put("name", "record_selector_" + n);
      v.put("addr", cell);
      v.put("bytes", 1);
      v.put("role", "picks which record of the record_catalog_" + n
          + " gets copied to current_record_" + n);
      v.put("written_by", rebuildWriters(rebuilds, cell));
      vars.add(v);
      rec.put("name", "current_record_" + n);
    }
    if (protagonista != null) {
      for (String eje : List.of("x", "y")) {
        int addr = ((Number) protagonista.get(eje + "_addr")).intValue();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("name", "protagonist_" + eje);
        v.put("addr", addr);
        v.put("bytes", 1);
        v.put("role", "coordinate " + eje.toUpperCase() + " of the individual sprite; pixel = "
            + protagonista.get(eje + "_formula"));
        vars.add(v);
        entrada(espacio, "protagonist_" + eje, addr, addr, "variable", "protagonist");
      }
    }

    espacio.sort(Comparator.comparingInt(e -> (int) ((List<Integer>) e.get("range")).get(0)));

    // routines: role + which named regions each one reads/writes
    Map<String, String> rolPorRutina = new HashMap<>();
    for (Object ro : rutinasClasificadas) {
      Map<String, Object> rr = (Map<String, Object>) ro;
      rolPorRutina.put((String) rr.get("name"), (String) rr.get("type"));
    }
    Map<String, Map<String, long[]>> porRutina = accesosPorRutina(espacio);
    List<Map<String, Object>> rutinas = new ArrayList<>();
    for (Map.Entry<String, Map<String, long[]>> me : porRutina.entrySet()) {
      Map<String, Object> rt = new LinkedHashMap<>();
      rt.put("name", me.getKey());
      if (rolPorRutina.containsKey(me.getKey()))
        rt.put("role", rolPorRutina.get(me.getKey()));
      List<String> lee = new ArrayList<>(), escribe = new ArrayList<>();
      me.getValue().entrySet().stream()
          .sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
          .forEach(e -> {
            if (e.getValue()[0] > 0)
              lee.add(e.getKey());
            if (e.getValue()[1] > 0)
              escribe.add(e.getKey());
          });
      rt.put("reads_from", lee);
      rt.put("writes_into", escribe);
      rutinas.add(rt);
    }

    // segments: the inverse view, with an encapsulation proposal
    List<Map<String, Object>> segmentos = new ArrayList<>();
    for (Map<String, Object> e : espacio) {
      String nombre = (String) e.get("name");
      List<String> leen = new ArrayList<>(), escriben = new ArrayList<>();
      porRutina.forEach((rutina, acc) -> {
        long[] c = acc.get(nombre);
        if (c == null)
          return;
        if (c[0] > 0)
          leen.add(rutina);
        if (c[1] > 0)
          escriben.add(rutina);
      });
      Map<String, Object> s = new LinkedHashMap<>();
      s.put("segment", nombre);
      s.put("range", e.get("range"));
      s.put("read_by", leen);
      s.put("written_by", escriben);
      Set<String> todas = new TreeSet<>(leen);
      todas.addAll(escriben);
      if (todas.size() > 0 && todas.size() <= 4)
        s.put("encapsulatable", "only " + todas.size() + " routines see it: " + String.join(" ", todas));
      segmentos.add(s);
    }

    Map<String, Object> modelo = new LinkedHashMap<>();
    modelo.put("version", 1);
    modelo.put("purpose", "formal layer of the model, meant to be read to transform the generated Java"
        + " source: renames, entity classes with subtypes, mem[..] -> named properties,"
        + " memory segmentation by visibility, extraction of data to resources");
    modelo.put("address_space", espacio);
    modelo.put("variables", vars);
    modelo.put("structures", estructuras);
    modelo.put("records", records);
    modelo.put("routines", rutinas);
    modelo.put("segments", segmentos);
    modelo.put("refactor_suggestions", sugerencias(estructuras, records, vars, segmentos));
    return modelo;
  }

  private Map<String, Object> entrada(List<Map<String, Object>> espacio, String nombre,
                                      int lo, int hi, String tipo, String evidencia) {
    Map<String, Object> e = new LinkedHashMap<>();
    e.put("name", nombre);
    e.put("range", List.of(lo, hi));
    e.put("type", tipo);
    e.put("finding", evidencia);
    espacio.add(e);
    return e;
  }

  @SuppressWarnings("unchecked")
  private List<String> rebuildWriters(List<Map<String, Object>> rebuilds, int cell) {
    for (Map<String, Object> f : rebuilds) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      if ((Integer) sel.get("cell") != cell)
        continue;
      return ((List<Object>) sel.get("written_by")).stream()
          .map(wo -> (String) ((Map<String, Object>) wo).get("routine"))
          .distinct().toList();
    }
    return List.of();
  }

  /** per routine, per named region: {reads, writes} summed over its sites. */
  @SuppressWarnings("unchecked")
  private Map<String, Map<String, long[]>> accesosPorRutina(List<Map<String, Object>> espacio) {
    Map<String, Map<String, long[]>> out = new TreeMap<>();
    for (Map<Integer, AnalysisDB.Stat> side : List.of(db.reads, db.writes)) {
      boolean isWrite = side == db.writes;
      for (AnalysisDB.Stat s : side.values()) {
        String region = regionDe(espacio, s.addrMin(), s.addrMax());
        if (region == null)
          continue;
        String rutina = db.method.getOrDefault(s.pc(), "?");
        long[] acc = out.computeIfAbsent(rutina, k -> new TreeMap<>())
            .computeIfAbsent(region, k -> new long[2]);
        acc[isWrite ? 1 : 0] += s.count();
      }
    }
    return out;
  }

  /** smallest named region containing the access range (aggregated ranges may span zones). */
  @SuppressWarnings("unchecked")
  private String regionDe(List<Map<String, Object>> espacio, int lo, int hi) {
    String best = null;
    int bestSize = Integer.MAX_VALUE;
    for (Map<String, Object> e : espacio) {
      List<Integer> r = (List<Integer>) e.get("range");
      if (r.get(0) <= lo && hi <= r.get(1) && r.get(1) - r.get(0) < bestSize) {
        best = (String) e.get("name");
        bestSize = r.get(1) - r.get(0);
      }
    }
    return best;
  }

  /** mechanical refactor hints derived from the model (what a transformer pass would do). */
  @SuppressWarnings("unchecked")
  private List<Object> sugerencias(List<Map<String, Object>> estructuras,
                                   List<Map<String, Object>> records,
                                   List<Map<String, Object>> vars,
                                   List<Map<String, Object>> segmentos) {
    List<Object> out = new ArrayList<>();
    for (Map<String, Object> est : estructuras) {
      if (!est.containsKey("types"))
        continue;
      Map<String, Object> disc = (Map<String, Object>) est.get("discriminant");
      out.add(Map.of(
          "target", est.get("name"),
          "refactor", "class with polymorphic subtypes",
          "detail", "a base class with the common fields and one subclass per value of "
              + disc.get("field") + " & " + disc.get("mask")
              + "; the type-ladder ifs become polymorphic methods"));
    }
    for (Map<String, Object> rec : records)
      out.add(Map.of(
          "target", rec.get("name"),
          "refactor", "class with named properties",
          "detail", "each field of the record becomes a property; the catalog is an array of"
              + " instances and the selector a reference to the current instance"));
    for (Map<String, Object> v : vars)
      out.add(Map.of(
          "target", v.get("name"),
          "refactor", "named property",
          "detail", "replace mem[" + v.get("addr") + "] with the property " + v.get("name")));
    long encapsulables = segmentos.stream().filter(s -> s.containsKey("encapsulatable")).count();
    if (encapsulables > 0)
      out.add(Map.of(
          "target", "segments",
          "refactor", "restricted visibility",
          "detail", encapsulables + " segments are seen by <=4 routines: move each one into its routines'"
              + " class and take the data out of the global mem[] array"));
    out.add(Map.of(
        "target", "graphics_data",
        "refactor", "external resources",
        "detail", "the graphics_data zones and static catalogs can be extracted to"
            + " files and loaded at initialization"));
    return out;
  }
}
