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
 * The FORMAL layer of the game model ("modelo" in game-model.json): the same deductions
 * that the human-readable findings describe, organised the way a skoolkit disassembly
 * organises them — every address range TYPED and NAMED, every variable/structure/record
 * with its fields, every routine with what it reads and writes — so that a later pass
 * can READ this JSON and TRANSFORM the generated Java source: rename things, turn the
 * entity union into classes with subtypes, replace mem[..] with named properties, split
 * the flat memory into segments only visible to the code that touches them, extract
 * graphics to resource files. Sections:
 * <ul>
 *   <li><b>espacio_direcciones</b>: the typed memory map (screen, buffers, graphics,
 *       lookup tables, structure arrays, singleton records, template catalogues,
 *       variables), each with a stable proposed name;</li>
 *   <li><b>variables</b>: named cells (selectors, protagonist coordinates) with who
 *       reads/writes them;</li>
 *   <li><b>estructuras</b>: the canonical records with their discriminated types
 *       (the input for entity classes + polymorphism);</li>
 *   <li><b>registros</b>: singleton records with their field layout (the input for
 *       "memory range → class with named properties");</li>
 *   <li><b>rutinas</b>: role classification + which named regions each one reads and
 *       writes (the input for moving code into the classes it belongs to);</li>
 *   <li><b>segmentos</b>: the inverse view — per region, the routines that touch it,
 *       with an encapsulation proposal when few routines see it;</li>
 *   <li><b>sugerencias_de_refactor</b>: mechanical hints derived from the above.</li>
 * </ul>
 * Names are role-based and generic (nothing game-specific); the evidence behind each
 * element stays in the "hallazgos"/"evidencia" layers.
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
                               List<Map<String, Object>> registros,
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
        nombre = r.hi() >= 22528 && r.lo() < 22528 ? "pantalla"
            : atributos ? "pantalla_atributos" : "pantalla_pixels";
      else
        nombre = (atributos ? "buffer_atributos_" + ++nBufAttr : "buffer_pixels_" + ++nBufPix);
      Map<String, Object> e = entrada(espacio, nombre, r.lo(), r.hi(),
          r.delta() == 0 ? "hardware_pantalla" : "buffer_de_trabajo", "pantalla");
      if (r.delta() != 0)
        e.put("delta_a_pantalla", r.delta());
    }
    if (fuente != null) {
      List<Integer> fr = (List<Integer>) fuente.get("rango");
      entrada(espacio, "fuente_texto", fr.get(0), fr.get(1), "datos_graficos", "fuente");
    }
    int n = 0;
    for (int[] g : gfxRegions)
      entrada(espacio, "graficos_" + ++n, g[0], g[1], "datos_graficos", "graficos");
    n = 0;
    for (int[] t : lookupTables)
      entrada(espacio, "tabla_consulta_" + ++n, t[0], t[1], "tabla_consulta", "tablas-consulta");

    // structure arrays (canonical records); the discriminated one is "entidades"
    List<Map<String, Object>> estructuras = new ArrayList<>();
    n = 0;
    for (Map<String, Object> c : canonicos) {
      List<Integer> r = (List<Integer>) c.get("rango");
      String nombre = c.containsKey("tipos") ? "entidades" : "arreglo_registros_" + ++n;
      Map<String, Object> e = entrada(espacio, nombre, r.get(0), r.get(1),
          "arreglo_de_registros", "tabla-entidades");
      e.put("registro_bytes", c.get("registro_bytes"));
      Map<String, Object> est = new LinkedHashMap<>();
      est.put("nombre", nombre);
      est.putAll(c);
      estructuras.add(est);
    }

    // singleton records + their template catalogue + their selector variable
    List<Map<String, Object>> vars = new ArrayList<>();
    n = 0;
    for (Map<String, Object> rec : registros) {
      n++;
      List<Integer> r = (List<Integer>) rec.get("rango");
      entrada(espacio, "registro_actual_" + n, r.get(0), r.get(1),
          "registro_singleton", "registros-reconstruidos");
      Map<String, Object> t = (Map<String, Object>) rec.get("plantilla");
      List<Integer> tr = (List<Integer>) t.get("rango");
      Map<String, Object> cat = entrada(espacio, "catalogo_registros_" + n, tr.get(0), tr.get(1),
          "catalogo_estatico", "reconstruccion-por-selector");
      cat.put("registro_bytes", t.get("registro_bytes"));
      cat.put("registros", t.get("registros_usados"));
      int cell = (int) rec.get("selector");
      entrada(espacio, "selector_registro_" + n, cell, cell, "variable", "reconstruccion-por-selector");
      Map<String, Object> v = new LinkedHashMap<>();
      v.put("nombre", "selector_registro_" + n);
      v.put("addr", cell);
      v.put("bytes", 1);
      v.put("rol", "elige que registro del catalogo_registros_" + n
          + " se copia a registro_actual_" + n);
      v.put("escrita_por", rebuildWriters(rebuilds, cell));
      vars.add(v);
      rec.put("nombre", "registro_actual_" + n);
    }
    if (protagonista != null) {
      for (String eje : List.of("x", "y")) {
        int addr = ((Number) protagonista.get(eje + "_addr")).intValue();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("nombre", "protagonista_" + eje);
        v.put("addr", addr);
        v.put("bytes", 1);
        v.put("rol", "coordenada " + eje.toUpperCase() + " del sprite individual; pixel = "
            + protagonista.get(eje + "_formula"));
        vars.add(v);
        entrada(espacio, "protagonista_" + eje, addr, addr, "variable", "protagonista");
      }
    }

    espacio.sort(Comparator.comparingInt(e -> (int) ((List<Integer>) e.get("rango")).get(0)));

    // routines: role + which named regions each one reads/writes
    Map<String, String> rolPorRutina = new HashMap<>();
    for (Object ro : rutinasClasificadas) {
      Map<String, Object> rr = (Map<String, Object>) ro;
      rolPorRutina.put((String) rr.get("nombre"), (String) rr.get("tipo"));
    }
    Map<String, Map<String, long[]>> porRutina = accesosPorRutina(espacio);
    List<Map<String, Object>> rutinas = new ArrayList<>();
    for (Map.Entry<String, Map<String, long[]>> me : porRutina.entrySet()) {
      Map<String, Object> rt = new LinkedHashMap<>();
      rt.put("nombre", me.getKey());
      if (rolPorRutina.containsKey(me.getKey()))
        rt.put("rol", rolPorRutina.get(me.getKey()));
      List<String> lee = new ArrayList<>(), escribe = new ArrayList<>();
      me.getValue().entrySet().stream()
          .sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
          .forEach(e -> {
            if (e.getValue()[0] > 0)
              lee.add(e.getKey());
            if (e.getValue()[1] > 0)
              escribe.add(e.getKey());
          });
      rt.put("lee", lee);
      rt.put("escribe", escribe);
      rutinas.add(rt);
    }

    // segments: the inverse view, with an encapsulation proposal
    List<Map<String, Object>> segmentos = new ArrayList<>();
    for (Map<String, Object> e : espacio) {
      String nombre = (String) e.get("nombre");
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
      s.put("segmento", nombre);
      s.put("rango", e.get("rango"));
      s.put("leido_por", leen);
      s.put("escrito_por", escriben);
      Set<String> todas = new TreeSet<>(leen);
      todas.addAll(escriben);
      if (todas.size() > 0 && todas.size() <= 4)
        s.put("encapsulable", "solo " + todas.size() + " rutinas lo ven: " + String.join(" ", todas));
      segmentos.add(s);
    }

    Map<String, Object> modelo = new LinkedHashMap<>();
    modelo.put("version", 1);
    modelo.put("proposito", "capa formal del modelo, pensada para leerla y transformar el fuente"
        + " Java generado: renombres, clases de entidades con subtipos, mem[..] -> propiedades"
        + " con nombre, segmentacion de la memoria por visibilidad, extraccion de datos a recursos");
    modelo.put("espacio_direcciones", espacio);
    modelo.put("variables", vars);
    modelo.put("estructuras", estructuras);
    modelo.put("registros", registros);
    modelo.put("rutinas", rutinas);
    modelo.put("segmentos", segmentos);
    modelo.put("sugerencias_de_refactor", sugerencias(estructuras, registros, vars, segmentos));
    return modelo;
  }

  private Map<String, Object> entrada(List<Map<String, Object>> espacio, String nombre,
                                      int lo, int hi, String tipo, String evidencia) {
    Map<String, Object> e = new LinkedHashMap<>();
    e.put("nombre", nombre);
    e.put("rango", List.of(lo, hi));
    e.put("tipo", tipo);
    e.put("hallazgo", evidencia);
    espacio.add(e);
    return e;
  }

  @SuppressWarnings("unchecked")
  private List<String> rebuildWriters(List<Map<String, Object>> rebuilds, int cell) {
    for (Map<String, Object> f : rebuilds) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      if ((Integer) sel.get("celda") != cell)
        continue;
      return ((List<Object>) sel.get("escrito_por")).stream()
          .map(wo -> (String) ((Map<String, Object>) wo).get("rutina"))
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
      List<Integer> r = (List<Integer>) e.get("rango");
      if (r.get(0) <= lo && hi <= r.get(1) && r.get(1) - r.get(0) < bestSize) {
        best = (String) e.get("nombre");
        bestSize = r.get(1) - r.get(0);
      }
    }
    return best;
  }

  /** mechanical refactor hints derived from the model (what a transformer pass would do). */
  @SuppressWarnings("unchecked")
  private List<Object> sugerencias(List<Map<String, Object>> estructuras,
                                   List<Map<String, Object>> registros,
                                   List<Map<String, Object>> vars,
                                   List<Map<String, Object>> segmentos) {
    List<Object> out = new ArrayList<>();
    for (Map<String, Object> est : estructuras) {
      if (!est.containsKey("tipos"))
        continue;
      Map<String, Object> disc = (Map<String, Object>) est.get("discriminante");
      out.add(Map.of(
          "objetivo", est.get("nombre"),
          "refactor", "clase con subtipos polimorficos",
          "detalle", "una clase base con los campos comunes y una subclase por cada valor de "
              + disc.get("campo") + " & " + disc.get("mascara")
              + "; los ifs de la escalera de tipos pasan a metodos polimorficos"));
    }
    for (Map<String, Object> rec : registros)
      out.add(Map.of(
          "objetivo", rec.get("nombre"),
          "refactor", "clase con propiedades nombradas",
          "detalle", "cada campo del registro pasa a ser una propiedad; el catalogo es un"
              + " arreglo de instancias y el selector una referencia a la instancia actual"));
    for (Map<String, Object> v : vars)
      out.add(Map.of(
          "objetivo", v.get("nombre"),
          "refactor", "propiedad con nombre",
          "detalle", "reemplazar mem[" + v.get("addr") + "] por la propiedad " + v.get("nombre")));
    long encapsulables = segmentos.stream().filter(s -> s.containsKey("encapsulable")).count();
    if (encapsulables > 0)
      out.add(Map.of(
          "objetivo", "segmentos",
          "refactor", "visibilidad restringida",
          "detalle", encapsulables + " segmentos los ven <=4 rutinas: mover cada uno a la clase"
              + " de sus rutinas y sacar el dato del arreglo global mem[]"));
    out.add(Map.of(
        "objetivo", "datos_graficos",
        "refactor", "recursos externos",
        "detalle", "las zonas datos_graficos y los catalogos estaticos pueden extraerse a"
            + " archivos y cargarse en la inicializacion"));
    return out;
  }
}
