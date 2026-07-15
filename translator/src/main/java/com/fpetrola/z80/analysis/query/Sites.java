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

import java.util.*;
import java.util.function.Predicate;

/**
 * Fluent filter over site statistics — the vocabulary every analyzer repeats by hand:
 * "read sites inside this range", "single dynamic cell", "sweeping more than one
 * address", "values that fit in a byte code". Immutable: each step returns a new view.
 */
public final class Sites {
  private final AnalysisDB db;
  private final List<AnalysisDB.Stat> stats;

  private Sites(AnalysisDB db, List<AnalysisDB.Stat> stats) {
    this.db = db;
    this.stats = stats;
  }

  public static Sites reads(AnalysisDB db) {
    return new Sites(db, new ArrayList<>(db.reads.values()));
  }

  public static Sites writes(AnalysisDB db) {
    return new Sites(db, new ArrayList<>(db.writes.values()));
  }

  /** the read stats of the given pcs (sites without a read stat are skipped). */
  public static Sites reads(AnalysisDB db, Collection<Integer> pcs) {
    List<AnalysisDB.Stat> out = new ArrayList<>();
    for (int pc : pcs) {
      AnalysisDB.Stat s = db.reads.get(pc);
      if (s != null)
        out.add(s);
    }
    return new Sites(db, out);
  }

  public Sites where(Predicate<AnalysisDB.Stat> p) {
    return new Sites(db, stats.stream().filter(p).toList());
  }

  /** observed range fully inside [lo..hi]. */
  public Sites within(int lo, int hi) {
    return where(s -> s.addrMin() >= lo && s.addrMax() <= hi);
  }

  public Sites intersecting(int lo, int hi) {
    return where(s -> s.addrMax() >= lo && s.addrMin() <= hi);
  }

  /** always the same single address. */
  public Sites singleCell() {
    return where(s -> s.addrMin() == s.addrMax());
  }

  /** more than one address observed (an array/sweep, not a variable). */
  public Sites sweeping() {
    return where(s -> s.addrMax() > s.addrMin());
  }

  public Sites spanningAtLeast(int bytes) {
    return where(s -> s.addrMax() - s.addrMin() + 1 >= bytes);
  }

  public Sites spanningAtMost(int bytes) {
    return where(s -> s.addrMax() - s.addrMin() + 1 <= bytes);
  }

  public Sites valuesAtMost(int max) {
    return where(s -> s.valMax() <= max);
  }

  /** the cell is rewritten by some small-range writer (not just wide clears). */
  public Sites mutableCell() {
    return where(s -> {
      for (AnalysisDB.Stat w : db.writes.values())
        if (w.addrMin() <= s.addrMin() && s.addrMin() <= w.addrMax()
            && w.addrMax() - w.addrMin() <= 4)
          return true;
      return false;
    });
  }

  public List<AnalysisDB.Stat> list() {
    return stats;
  }

  public List<Integer> pcs() {
    return stats.stream().map(AnalysisDB.Stat::pc).toList();
  }

  public boolean isEmpty() {
    return stats.isEmpty();
  }

  public Optional<AnalysisDB.Stat> heaviest() {
    return stats.stream().max(Comparator.comparingLong(AnalysisDB.Stat::count));
  }
}
