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
  /**
   * Runtime keeps the cap tight (64): in a compositing chain the deep side is stale
   * history and dropping it preserves the fresh sprite. DISCOVERY wants the opposite —
   * old leaves are exactly what it is looking for — so {@code -Dtaint.depth} raises it
   * (TaintDiscover defaults to 512).
   */
  private final int maxDepth;
  /**
   * Which side survives the depth cap. The VALUE plane keeps the SHALLOW side (the fresh
   * sprite of the blit; accumulated history is stale compositing). The DIRECTION plane
   * (doc/BASE-SEMANTICA.md §2.2) inverts it: the deep side is the chain that reaches the
   * entity's state variable, which is the identity being tracked — and keeping it also
   * FREEZES the chain at the cap (the returned node is an existing one, so further unions
   * are memo hits instead of new nodes).
   */
  private final boolean keepDeep;

  /** taint of every game memory byte / every Tracer register slot. */
  public final int[] mem = new int[0x10000];
  public final int[] reg = new int[32];

  /**
   * Per-BIT refinement of the same ownership question, for engines that composite with a
   * mask ({@code fondo AND mascara OR sprite}). The node graph answers "which addresses is
   * this byte made of", and under masking that union names the sprite forever: once the
   * player has passed over a byte, background painted on top through the same masked
   * routine still carries the sprite's addresses, so {@link #spriteOf} keeps calling it the
   * player and the sprite smears a trail behind it. Freshness cannot tell those apart —
   * the trail byte was just written too.
   *
   * <p>These masks say WHICH BITS of a byte actually came from a sprite bitmap, which the
   * union cannot express. A byte whose mask is 0 belongs to no sprite however its origins
   * read. See doc/DETECCION-SPRITES-3D.md §5.1.
   */
  public final byte[] bits = new byte[0x10000];
  public final int[] regBits = new int[32];

  /**
   * Sprite bits of a value read from {@code addr}: reading a sprite bitmap SEEDS the mask
   * (every set bit of it is sprite ink), anywhere else the byte carries whatever mask it
   * was last written with.
   */
  public int readBits(int addr, int value) {
    return catalogBase[addr & 0xffff] != 0 ? value & 0xff : bits[addr & 0xffff] & 0xff;
  }

  private int[] ua = new int[1 << 14], ub = new int[1 << 14];
  private int[] uDepth = new int[1 << 14];
  private int[] uSprite = new int[1 << 14];
  private int[] uTile = new int[1 << 14];
  private int unions;
  /**
   * Memo de uniones por par (a,b). Mapa primitivo y no HashMap por PESO: el plano dir junta
   * >10M de nodos en una corrida entera y el boxing de HashMap son ~50 bytes por entrada
   * (~1GB él solo) contra ~12 acá — en una máquina de 7GB es la diferencia entre correr el
   * RZX completo y colgar la sesión en swap.
   */
  private final LongIntMap memo = new LongIntMap(1 << 15);

  /** open addressing lineal, claves long ≠ 0 (los pares de union: a ≥ 1), 0 = vacío. */
  private static final class LongIntMap {
    private long[] keys;
    private int[] vals;
    private int size, mask;

    LongIntMap(int capacity) {
      int c = 16;
      while (c < capacity)
        c <<= 1;
      keys = new long[c];
      vals = new int[c];
      mask = c - 1;
    }

    private static int mix(long k) {
      k *= 0x9E3779B97F4A7C15L;
      return (int) (k ^ (k >>> 32));
    }

    int get(long k) {
      int i = mix(k) & mask;
      while (keys[i] != 0) {
        if (keys[i] == k)
          return vals[i];
        i = (i + 1) & mask;
      }
      return 0;
    }

    void put(long k, int v) {
      if (size * 5 >= keys.length * 3)
        grow();
      int i = mix(k) & mask;
      while (keys[i] != 0) {
        if (keys[i] == k) {
          vals[i] = v;
          return;
        }
        i = (i + 1) & mask;
      }
      keys[i] = k;
      vals[i] = v;
      size++;
    }

    private void grow() {
      long[] ok = keys;
      int[] ov = vals;
      keys = new long[ok.length * 2];
      vals = new int[ok.length * 2];
      mask = keys.length - 1;
      size = 0;
      for (int j = 0; j < ok.length; j++)
        if (ok[j] != 0)
          put(ok[j], ov[j]);
    }
  }

  /** addr -> sprite base + 1 when addr belongs to a catalog sprite, else 0. */
  private final int[] catalogBase;
  /** addr in a tile-template zone: the leaf address itself identifies the tile bitmap. */
  private final boolean[] tileZone;

  public OriginTaint(int[] catalogBase, boolean[] tileZone) {
    this(catalogBase, tileZone, Integer.getInteger("taint.depth", 64), false);
  }

  /** a plane with its own policy: the dir plane passes a high cap and {@code keepDeep}. */
  public OriginTaint(int[] catalogBase, boolean[] tileZone, int maxDepth, boolean keepDeep) {
    this.catalogBase = catalogBase;
    this.tileZone = tileZone;
    this.maxDepth = maxDepth;
    this.keepDeep = keepDeep;
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
    int hit = memo.get(key);
    if (hit != 0)
      return hit;
    int da = depthOf(a), db = depthOf(b);
    // accumulated history (the deep side) is stale compositing; the fresh operand is the
    // content that matters. Keeping the shallow side preserves the sprite through a blit.
    // The dir plane keeps the DEEP side instead: see keepDeep.
    if (Math.max(da, db) + 1 > maxDepth)
      return keepDeep ? (da >= db ? a : b) : (da <= db ? a : b);
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

  /**
   * Discovery-grade leaves: the FULL sorted leaf-address set of {@code node}, memoized
   * bottom-up in {@code memo} (union nodes are immutable, so entries stay valid across
   * frames — each node is computed once ever, which is what makes scanning every screen
   * byte per sampled frame affordable). A set that would exceed {@code cap} is stored as
   * SATURATED ({@code null}): a byte mixing that many origins identifies nothing anyway,
   * and the caller skips it.
   */
  public int[] leavesSorted(int node, int cap, Map<Integer, int[]> memo) {
    if (node == NONE)
      return EMPTY;
    if (node < FIRST_UNION)
      return new int[]{node - 1}; // single origin: not worth a memo entry
    if (memo.containsKey(node))
      return memo.get(node);
    int[] la = leavesSorted(ua[node - FIRST_UNION], cap, memo);
    int[] lb = leavesSorted(la == null ? NONE : ub[node - FIRST_UNION], cap, memo);
    int[] merged = la == null || lb == null ? null : mergeSorted(la, lb, cap);
    memo.put(node, merged);
    return merged;
  }

  private static final int[] EMPTY = new int[0];

  /**
   * Re-ancla la profundidad de un nodo re-internándolo por su CONJUNTO de hojas.
   *
   * <p>Por qué existe (medido en JSW, plano dir): una variable de estado actualizada por
   * RMW ({@code y += dy}) gana +1 de profundidad POR FRAME aunque su conjunto de hojas
   * converja en dos entradas; a ~500 frames toca el cap y desde ahí {@code keepDeep}
   * devuelve siempre el nodo viejo, descartando el operando fresco de cada unión — el
   * plano se fosiliza: los orígenes de gráficos, slots y tablas desaparecen de las hojas.
   * Aplanar en el STORE (donde viven las variables de estado) corta ese crecimiento:
   * el historial de construcción no converge, el conjunto sí.
   *
   * <p>Un nodo saturado (más hojas que {@code leafCap}) queda como está: no identifica
   * nada y, al tocar el cap de profundidad, sus uniones se congelan en memo hits sin
   * crear nodos. El intern por hash FNV-1a de 64 bits del set asume no-colisión.
   */
  public int flatten(int node, int leafCap) {
    if (node < FIRST_UNION || depthOf(node) <= FLATTEN_DEPTH)
      return node;
    Integer hit = flatMemo.get(node);
    if (hit != null)
      return hit;
    // los memos de arrays de hojas son lo que más pesa (hasta leafCap ints por nodo);
    // podarlos cuesta recomputar un rato y salva a una máquina de 7GB del swap
    if (flatLeaves.size() > 400_000)
      flatLeaves.clear();
    if (flatMemo.size() > 2_000_000)
      flatMemo.clear();
    int[] lv = leavesSorted(node, leafCap, flatLeaves);
    int out = node;
    if (lv != null) {
      long h = 1469598103934665603L;
      for (int a : lv) {
        h ^= a;
        h *= 1099511628211L;
      }
      Integer canon = setMemo.get(h);
      if (canon == null) {
        int n = NONE;
        for (int a : lv)
          n = union(n, origin(a));
        setMemo.put(h, canon = n);
      }
      out = canon;
    }
    flatMemo.put(node, out);
    return out;
  }

  /** por encima de esto, un store re-interna por conjunto; el canónico (≤ leafCap hojas en
   *  fold izquierdo) queda siempre por debajo, así que aplanar es idempotente. */
  private static final int FLATTEN_DEPTH = 128;
  private final Map<Integer, Integer> flatMemo = new HashMap<>();
  private final Map<Integer, int[]> flatLeaves = new HashMap<>();
  private final Map<Long, Integer> setMemo = new HashMap<>();

  /** sorted-unique merge, or {@code null} (saturated) past {@code cap} elements. */
  private static int[] mergeSorted(int[] a, int[] b, int cap) {
    int[] out = new int[Math.min(a.length + b.length, cap)];
    int i = 0, j = 0, n = 0;
    while (i < a.length || j < b.length) {
      int v;
      if (i == a.length)
        v = b[j++];
      else if (j == b.length)
        v = a[i++];
      else if (a[i] < b[j])
        v = a[i++];
      else if (a[i] > b[j])
        v = b[j++];
      else {
        v = a[i++];
        j++;
      }
      if (n == cap)
        return null;
      out[n++] = v;
    }
    return n == out.length ? out : java.util.Arrays.copyOf(out, n);
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
