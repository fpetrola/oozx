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
 *   AnalysisCLI slice <addrLo> <addrHi> [depth] [fanout]   backward slice de un rango
 *   AnalysisCLI sprites                                     rutinas de dibujado + datos graficos
 *   AnalysisCLI regions [totalFrames]                       variables / tablas / buffers / branches
 *   AnalysisCLI copychains                                  pipelines de bulk copies
 *   AnalysisCLI site <pc>                                   detalle de un site
 * </pre>
 */
public class AnalysisCLI {
  public static void main(String[] args) throws Exception {
    String dbPath = System.getProperty("analysis.db", "analysis/analysis.db");
    AnalysisDB db = new AnalysisDB(dbPath);
    String cmd = args.length > 0 ? args[0] : "sprites";
    switch (cmd) {
      case "slice" -> {
        int lo = Integer.parseInt(args[1]), hi = Integer.parseInt(args[2]);
        int depth = args.length > 3 ? Integer.parseInt(args[3]) : 4;
        int fanout = args.length > 4 ? Integer.parseInt(args[4]) : 4;
        new BackwardSlicer(db).slice(lo, hi, depth, fanout);
      }
      case "sprites" -> new SpriteFinder(db).report();
      case "regions" -> new RegionClassifier(db, args.length > 1 ? Integer.parseInt(args[1]) : 20583).report();
      case "copychains" -> new CopyChainFinder(db).report();
      case "site" -> {
        int pc = Integer.parseInt(args[1]);
        System.out.println(db.describe(pc));
        System.out.println("in-edges:");
        db.edgesIn.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  <- x" + e.count() + " " + db.describe(e.src())));
        System.out.println("out-edges:");
        db.edgesOut.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  -> x" + e.count() + " " + db.describe(e.dst())));
        System.out.println("cfg-out:");
        db.cfgOut.getOrDefault(pc, java.util.List.of())
            .forEach(e -> System.out.println("  -> " + e.dst() + " x" + e.count()));
      }
      default -> System.out.println("comando desconocido: " + cmd);
    }
  }
}
