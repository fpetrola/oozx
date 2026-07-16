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

/**
 * Entry point for exploring the capture:
 * <pre>
 *   AnalysisCLI slice <addrLo> <addrHi> [depth] [fanout] [addr|val|cond]   backward slice de un rango
 *   AnalysisCLI sprites                                     rutinas de dibujado + datos graficos
 *   AnalysisCLI regions [totalFrames]                       variables / tablas / buffers / branches
 *   AnalysisCLI copychains                                  pipelines de bulk copies
 *   AnalysisCLI site <pc>                                   detalle de un site
 *   AnalysisCLI equations [metodo | pcLo pcHi]              listado de ecuaciones normalizadas
 *   AnalysisCLI track [rzxPath]                             pipeline automatico completo de posiciones
 *   AnalysisCLI z80run [rzxPath]                            pasada de agregados desde el EMULADOR Z80
 *                                                           (cualquier juego, sin conversion a Java)
 *   AnalysisCLI z80track [rzxPath]                          pipeline de posiciones desde el emulador
 *   AnalysisCLI positions <frameLo> [frameHi]               sprites dibujados + celdas-coordinate por frame
 *   AnalysisCLI explain <lo> <hi> [depth] [fanout]          narrativa recursiva: quien escribe el rango,
 *                                                           direccion/valor/condicion, hasta INIT/IO
 *   AnalysisCLI map [rzxPath]                               mapa completo: buffers, rutinas, graficos,
 *                                                           tablas, estructura de entidades, variables
 *   AnalysisCLI export [salida.json]                        modelo del juego consolidado en JSON
 *   AnalysisCLI segments                                    particion de la memoria por conjunto-de-acceso:
 *                                                           segmentos privados (encapsulables) + fugas
 *   AnalysisCLI screen                                      geometria de pantalla: tabla de filas ->
 *                                                           rectangulo (x,y), transformada T y su inversa
 *   AnalysisCLI coverage                                    corre todos los detectores y clasifica cada uno
 *                                                           (disparo/baja-confianza/vacio) + alarmas SMC/128K
 *   AnalysisCLI structs [metodo]                            estructuras de datos por rutina: campos,
 *                                                           geometria del arreglo, semantica y variantes
 *   AnalysisCLI rebuilds                                    variables selectoras (pantalla/nivel actual) y
 *                                                           el cluster de reconstruccion que disparan
 *   AnalysisCLI records                                     campos del registro singleton reconstruido
 *                                                           (layout, tiles, punteros, enlaces, huecos)
 * </pre>
 * Los comandos que pueden necesitar una captura (track, map, export) eligen el productor
 * ({@link CaptureSource}) con {@code -Danalysis.source=java|z80}; z80run/z80track fuerzan el
 * emulador. La DB sale con {@code -Danalysis.db=<path>}.
 */
public class AnalysisCLI {
  /**
   * which producer a command that may need a capture should use: {@code -Danalysis.source=z80}
   * runs the ORIGINAL game on the emulator (any game), the default runs the converted Java one.
   * The z80* commands pick their source explicitly and ignore the property.
   */
  private static CaptureSource source() {
    return "z80".equals(System.getProperty("analysis.source", "java"))
        ? Z80AnalysisRunner.source() : RZXAnalysisRunner.source();
  }

  public static void main(String[] args) throws Exception {
    String dbPath = System.getProperty("analysis.db", "analysis/analysis.db");
    String cmd = args.length > 0 ? args[0] : "sprites";
    String rzx = args.length > 1 ? args[1] : RzxBootstrap.DEFAULT_RZX;
    switch (cmd) {
      case "track" -> {
        SpriteTracker.track(RZXAnalysisRunner.source(), dbPath, rzx);
        System.exit(0);
      }
      case "z80run" -> {
        Z80AnalysisRunner.source().capture(rzx, dbPath);
        System.exit(0);
      }
      case "z80track" -> {
        SpriteTracker.track(Z80AnalysisRunner.source(), dbPath, rzx);
        System.exit(0);
      }
      case "positions" -> {
        int lo = Integer.parseInt(args[1]);
        PositionReport.print(dbPath, lo, args.length > 2 ? Integer.parseInt(args[2]) : lo);
        return;
      }
      case "map" -> {
        GameMapper.run(source(), dbPath, rzx);
        System.exit(0);
      }
      case "export" -> {
        GameMapper.ensureTracked(source(), dbPath, RzxBootstrap.DEFAULT_RZX);
        new GameModelExporter(new AnalysisDB(dbPath), dbPath)
            .export(args.length > 1 ? args[1] : "analysis/game-model.json");
        System.exit(0);
      }
    }
    AnalysisDB db = new AnalysisDB(dbPath);
    switch (cmd) {
      case "slice" -> {
        int lo = Integer.parseInt(args[1]), hi = Integer.parseInt(args[2]);
        int depth = args.length > 3 ? Integer.parseInt(args[3]) : 4;
        int fanout = args.length > 4 ? Integer.parseInt(args[4]) : 4;
        String role = args.length > 5 ? args[5].toUpperCase() : null;
        new BackwardSlicer(db).slice(lo, hi, depth, fanout, role);
      }
      case "equations" -> {
        String methodFilter = null;
        int lo = 0, hi = 0xFFFF;
        if (args.length == 2)
          methodFilter = args[1];
        else if (args.length >= 3) {
          lo = Integer.parseInt(args[1]);
          hi = Integer.parseInt(args[2]);
        }
        new EquationLister(db).report(methodFilter, lo, hi);
      }
      case "explain" -> {
        int lo = Integer.parseInt(args[1]), hi = Integer.parseInt(args[2]);
        int depth = args.length > 3 ? Integer.parseInt(args[3]) : 5;
        int fanout = args.length > 4 ? Integer.parseInt(args[4]) : 3;
        new Explainer(db, dbPath).explain(lo, hi, depth, fanout);
      }
      case "segments" -> new SegmentFinder(db).report();
      case "screen" -> new ScreenMapper(db).report();
      case "coverage" -> new CoverageReport(db, dbPath).report();
      case "structs" -> new StructFinder(db, dbPath).report(args.length > 1 ? args[1] : null);
      case "rebuilds" -> new RebuildFinder(db, dbPath).report();
      case "records" -> new RecordFinder(db, dbPath).report();
      case "texts" -> new TextFinder(db, dbPath).report();
      case "sprites" -> new SpriteFinder(db).report();
      case "regions" -> new RegionClassifier(db, args.length > 1 ? Integer.parseInt(args[1]) : 20583).report();
      case "copychains" -> new CopyChainFinder(db).report();
      case "site" -> {
        int pc = Integer.parseInt(args[1]);
        System.out.println(db.describe(pc));
        System.out.println("in-edges:");
        db.edgesIn.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  <- x" + e.count() + " [" + e.label() + "] " + db.describe(e.src())));
        System.out.println("out-edges:");
        db.edgesOut.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  -> x" + e.count() + " [" + e.label() + "] " + db.describe(e.dst())));
        System.out.println("cfg-out:");
        db.cfgOut.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  -> " + e.dst() + " x" + e.count()));
      }
      default -> System.out.println("comando desconocido: " + cmd);
    }
  }
}
