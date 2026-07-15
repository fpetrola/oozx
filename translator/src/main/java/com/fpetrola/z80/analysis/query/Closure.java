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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Forward closure over the dynamic CFG with the two guards the analyzers need: stay
 * inside a site set (one routine) and do not expand through stop nodes (loop heads —
 * without this, a while(true) sweep wraps the closure around and dissolves arm
 * exclusivity). {@link #exclusive} is the handler of one branch arm: its closure minus
 * its sibling's.
 */
public final class Closure {
  private Closure() {
  }

  public static Set<Integer> cfg(AnalysisDB db, int start, Set<Integer> within,
                                 Set<Integer> stopAt, int limit) {
    Set<Integer> out = new HashSet<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(start);
    while (!queue.isEmpty() && out.size() < limit) {
      int pc = queue.poll();
      if (!within.contains(pc) || !out.add(pc))
        continue;
      if (stopAt.contains(pc))
        continue;
      for (AnalysisDB.Edge e : db.cfgOut.getOrDefault(pc, java.util.List.of()))
        queue.add(e.dst());
    }
    return out;
  }

  public static Set<Integer> exclusive(AnalysisDB db, int arm, int sibling,
                                       Set<Integer> within, Set<Integer> stopAt, int limit) {
    Set<Integer> a = cfg(db, arm, within, stopAt, limit);
    a.removeAll(cfg(db, sibling, within, stopAt, limit));
    return a;
  }
}
