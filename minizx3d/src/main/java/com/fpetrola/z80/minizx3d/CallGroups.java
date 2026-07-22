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

package com.fpetrola.z80.minizx3d;

import com.badlogic.gdx.utils.IntArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which DRAWING each screen byte belongs to, decided by the call tree.
 *
 * <p>A game that draws a composite object does it from one routine, or from several called
 * together by one that decided to draw the thing, so every byte of it hangs off one node.
 * Grouping on the screen instead — adjacency, colour, write order, a time window: all four
 * were tried and measured — cannot work, because the screen does not say where an object
 * ends: a capsule and the player standing in front of it are one connected patch drawn in one
 * frame, and there is no pixel that says otherwise.
 *
 * <p>Computed HERE, on the replay thread, and carried in the snapshot. The tree is rebuilt
 * every frame and the viewer renders whenever it can, so by the time it looks, the ids mean
 * something else entirely; the grouping has to be taken while it is still true.
 */
public final class CallGroups {
  private CallGroups() {
  }

  /**
   * The object is the HIGHEST call that still looks like one: a node counts if its box is the
   * size of a thing and its parent's is not. A fixed level cannot do it — one level above the
   * pixel is the object for a missile and the whole room for the routine that repaints the
   * screen (measured: fixed levels gave "objects" of 128x96 px, which is half the screen).
   * Density decides what a box alone cannot: half a screen fits any cap generous enough for a
   * big object, but it is nowhere near as full as a drawing of one thing.
   *
   * @return per screen byte, the id of the drawing it belongs to (0 = none)
   */
  public static int[] compute(byte[] pixels, int[] lastWrite, int frame, int[] writeNode,
      IntArray nodeParent, int maxCols, int maxRows, float minFill) {
    int[] out = new int[TaintReplay.PIXEL_BYTES];
    Map<Integer, List<Integer>> byNode = new HashMap<>();
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      if ((pixels[i] & 0xff) == 0 || lastWrite[i] != frame)
        continue;
      int n = writeNode[i] >= 0 && writeNode[i] < nodeParent.size ? writeNode[i] : 0;
      for (int a = n; ; a = nodeParent.get(a)) { // the byte belongs to every ancestor too
        byNode.computeIfAbsent(a, k -> new ArrayList<>()).add(i);
        if (a == 0)
          break;
      }
    }
    List<Integer> nodes = new ArrayList<>(byNode.keySet());
    nodes.sort((a, b) -> byNode.get(b).size() - byNode.get(a).size());
    for (int n : nodes) {
      List<Integer> painted = byNode.get(n);
      if (!fits(painted, maxCols, maxRows, minFill))
        continue;
      int parent = n == 0 ? -1 : nodeParent.get(n);
      if (parent >= 0 && byNode.containsKey(parent)
          && fits(byNode.get(parent), maxCols, maxRows, minFill))
        continue; // its caller still looks like one object: this is a piece of it, not it
      for (int i : painted)
        if (out[i] == 0)
          out[i] = n + 1;
    }
    return out;
  }

  private static boolean fits(List<Integer> painted, int maxCols, int maxRows, float minFill) {
    int minC = 31, maxC = 0, minR = 191, maxR = 0;
    for (int i : painted) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      minC = Math.min(minC, c);
      maxC = Math.max(maxC, c);
      minR = Math.min(minR, y);
      maxR = Math.max(maxR, y);
    }
    int w = maxC - minC + 1, h = maxR - minR + 1;
    return w <= maxCols && h <= maxRows && painted.size() >= minFill * w * h;
  }
}
