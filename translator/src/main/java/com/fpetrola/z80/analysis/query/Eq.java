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

package com.fpetrola.z80.analysis.query;

import com.fpetrola.z80.analysis.AnalysisDB;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Probes over a site's normalized equation — the regex sniffing every analyzer repeats. */
public final class Eq {
  private static final Pattern CMP = Pattern.compile("cp\\([A-Z], (\\d+)\\)");
  private static final Pattern MASK = Pattern.compile("& (\\d+)");

  private Eq() {
  }

  public static String text(AnalysisDB db, int pc) {
    return db.equation.getOrDefault(pc, "");
  }

  /** constant of a cp(REG, N) comparison, if any. */
  public static OptionalInt cmpConst(AnalysisDB db, int pc) {
    Matcher m = CMP.matcher(text(db, pc));
    return m.find() ? OptionalInt.of(Integer.parseInt(m.group(1))) : OptionalInt.empty();
  }

  /** first AND mask that is a real sub-field (& 255 is byte truncation, not a mask). */
  public static OptionalInt maskConst(AnalysisDB db, int pc) {
    Matcher m = MASK.matcher(text(db, pc));
    while (m.find()) {
      int mask = Integer.parseInt(m.group(1));
      if (mask != 255)
        return OptionalInt.of(mask);
    }
    return OptionalInt.empty();
  }

  public static boolean flagZ(AnalysisDB db, int pc) {
    return text(db, pc).contains("flagZ(");
  }

  public static boolean rotates(AnalysisDB db, int pc) {
    String eq = text(db, pc);
    return eq.contains("rlc(") || eq.contains("rrc(");
  }
}
