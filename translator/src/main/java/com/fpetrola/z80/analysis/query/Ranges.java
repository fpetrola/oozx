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

import java.util.ArrayList;
import java.util.List;

/** The address-interval algebra the analyzers keep re-inlining. */
public final class Ranges {
  private Ranges() {
  }

  public static boolean intersects(int lo1, int hi1, int lo2, int hi2) {
    return hi1 >= lo2 && lo1 <= hi2;
  }

  public static boolean contains(int lo, int hi, int addr) {
    return lo <= addr && addr <= hi;
  }

  /** intersection [lo..hi] of two ranges, or null when disjoint. */
  public static int[] intersection(int lo1, int hi1, int lo2, int hi2) {
    int lo = Math.max(lo1, lo2), hi = Math.min(hi1, hi2);
    return lo <= hi ? new int[]{lo, hi} : null;
  }

  /** merge sorted-or-not ranges closer than {@code gap} into maximal blocks. */
  public static List<int[]> merge(List<int[]> ranges, int gap) {
    List<int[]> sorted = new ArrayList<>(ranges);
    sorted.sort((a, b) -> Integer.compare(a[0], b[0]));
    List<int[]> out = new ArrayList<>();
    for (int[] r : sorted) {
      if (!out.isEmpty() && r[0] - out.get(out.size() - 1)[1] <= gap)
        out.get(out.size() - 1)[1] = Math.max(out.get(out.size() - 1)[1], r[1]);
      else
        out.add(new int[]{r[0], r[1]});
    }
    return out;
  }
}
