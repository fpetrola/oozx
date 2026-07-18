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

import java.util.HashMap;
import java.util.Map;

/**
 * Real-time origin taint: every byte of game memory (and every register) carries a node id
 * describing which ORIGINAL memory addresses its value was built from. A screen byte whose
 * origins fall inside the sprite catalog IS that sprite, no matter how many buffers, shifts
 * and ORs the value went through on the way — that is the whole point.
 *
 * <p>Node ids are interned (hash-consing): {@code 0} = no taint, {@code addr+1} = ORIGIN(addr),
 * higher ids = UNION nodes deduplicated by operand pair. A game repeats itself, so the id of
 * "sprite byte OR background byte" is created once and then every later frame hits the memo —
 * per-operation cost is one lookup, and the table stays small.
 *
 * <p>Each node caches the answer to the one question the renderer asks — "does any leaf fall
 * in the sprite catalog, and which sprite?" — so classification per screen byte is O(1).
 *
 * <p>Terms grow only on read-modify-write chains (OR/XOR compositing onto the same byte);
 * a store REPLACES the term. The depth cap cuts the accumulated (deep) side and keeps the
 * fresh (shallow) side, which in a blit is precisely the sprite being drawn.
 */
public final class OriginTaint {
  public static final int NONE = 0;
  private static final int FIRST_UNION = 0x10001;
  private static final int MAX_DEPTH = 64;

  /** taint of every game memory byte / every Tracer register slot. */
  public final int[] mem = new int[0x10000];
  public final int[] reg = new int[32];

  private int[] ua = new int[1 << 14], ub = new int[1 << 14];
  private int[] uDepth = new int[1 << 14];
  private int[] uSprite = new int[1 << 14];
  private int[] uTile = new int[1 << 14];
  private int unions;
  private final Map<Long, Integer> memo = new HashMap<>();

  /** addr -> sprite base + 1 when addr belongs to a catalog sprite, else 0. */
  private final int[] catalogBase;
  /** addr in a tile-template zone: the leaf address itself identifies the tile bitmap. */
  private final boolean[] tileZone;

  public OriginTaint(int[] catalogBase, boolean[] tileZone) {
    this.catalogBase = catalogBase;
    this.tileZone = tileZone;
  }

  public static int origin(int addr) {
    return addr + 1;
  }

  /** taint of reading memory at {@code addr}: its last written taint, or itself as origin. */
  public int read(int addr) {
    int t = mem[addr & 0xffff];
    return t != NONE ? t : origin(addr & 0xffff);
  }

  public int union(int a, int b) {
    if (a == b || b == NONE)
      return a;
    if (a == NONE)
      return b;
    if (a > b) {
      int t = a;
      a = b;
      b = t;
    }
    long key = ((long) a << 32) | (b & 0xffffffffL);
    Integer hit = memo.get(key);
    if (hit != null)
      return hit;
    int da = depthOf(a), db = depthOf(b);
    // accumulated history (the deep side) is stale compositing; the fresh operand is the
    // content that matters. Keeping the shallow side preserves the sprite through a blit.
    if (Math.max(da, db) + 1 > MAX_DEPTH)
      return da <= db ? a : b;
    int id = FIRST_UNION + unions;
    if (unions == ua.length)
      grow();
    ua[unions] = a;
    ub[unions] = b;
    uDepth[unions] = Math.max(da, db) + 1;
    int sa = spriteOf(a);
    uSprite[unions] = sa != 0 ? sa : spriteOf(b);
    int ta = tileOf(a);
    uTile[unions] = ta != 0 ? ta : tileOf(b);
    unions++;
    memo.put(key, id);
    return id;
  }

  /** sprite base + 1 when any leaf of {@code node} falls in the catalog, else 0. O(1). */
  public int spriteOf(int node) {
    if (node == NONE)
      return 0;
    if (node < FIRST_UNION)
      return catalogBase[node - 1];
    return uSprite[node - FIRST_UNION];
  }

  /** tile-bitmap leaf address + 1 when a leaf falls in a tile-template zone, else 0. O(1). */
  public int tileOf(int node) {
    if (node == NONE)
      return 0;
    if (node < FIRST_UNION)
      return tileZone[node - 1] ? node : 0;
    return uTile[node - FIRST_UNION];
  }

  private int depthOf(int node) {
    return node < FIRST_UNION ? 1 : uDepth[node - FIRST_UNION];
  }

  private void grow() {
    int cap = ua.length * 2;
    ua = java.util.Arrays.copyOf(ua, cap);
    ub = java.util.Arrays.copyOf(ub, cap);
    uDepth = java.util.Arrays.copyOf(uDepth, cap);
    uSprite = java.util.Arrays.copyOf(uSprite, cap);
    uTile = java.util.Arrays.copyOf(uTile, cap);
  }

  public int nodeCount() {
    return unions;
  }

  /** debug: collect up to {@code budget} distinct leaf addresses of {@code node}. */
  public java.util.Set<Integer> leaves(int node, int budget) {
    java.util.Set<Integer> out = new java.util.TreeSet<>();
    java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<>();
    stack.push(node);
    while (!stack.isEmpty() && out.size() < budget) {
      int n = stack.pop();
      if (n == NONE)
        continue;
      if (n < FIRST_UNION)
        out.add(n - 1);
      else {
        stack.push(ua[n - FIRST_UNION]);
        stack.push(ub[n - FIRST_UNION]);
      }
    }
    return out;
  }
}
