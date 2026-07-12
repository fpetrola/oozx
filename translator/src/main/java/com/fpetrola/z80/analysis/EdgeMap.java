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
 * Open-addressing hash map from (srcSite&lt;&lt;16 | dstSite) to a count.
 * No boxing, no allocation per increment — safe for the capture hot path.
 */
public final class EdgeMap {
  private long[] keys;
  private long[] counts;
  private int mask;
  private int size;
  private static final long EMPTY = -1;

  public EdgeMap(int initialCapacity) {
    int cap = Integer.highestOneBit(Math.max(initialCapacity, 16) * 2 - 1) << 1;
    keys = new long[cap];
    counts = new long[cap];
    java.util.Arrays.fill(keys, EMPTY);
    mask = cap - 1;
  }

  public static long key(int src, int dst) {
    return ((long) (src & 0xFFFF) << 16) | (dst & 0xFFFF);
  }

  public void increment(int src, int dst) {
    increment(key(src, dst), 1);
  }

  public void increment(long key, long delta) {
    int i = (int) (mix(key) & mask);
    while (true) {
      long k = keys[i];
      if (k == key) {
        counts[i] += delta;
        return;
      }
      if (k == EMPTY) {
        keys[i] = key;
        counts[i] = delta;
        if (++size * 4 > keys.length * 3)
          rehash();
        return;
      }
      i = (i + 1) & mask;
    }
  }

  public int size() {
    return size;
  }

  public void clear() {
    java.util.Arrays.fill(keys, EMPTY);
    java.util.Arrays.fill(counts, 0);
    size = 0;
  }

  /** visits each entry as (key, count) packed calls: consumer receives index i; use keyAt/countAt. */
  public void forEach(EntryConsumer consumer) {
    for (int i = 0; i < keys.length; i++)
      if (keys[i] != EMPTY)
        consumer.accept((int) (keys[i] >>> 16), (int) (keys[i] & 0xFFFF), counts[i]);
  }

  public interface EntryConsumer {
    void accept(int src, int dst, long count);
  }

  private void rehash() {
    long[] ok = keys, oc = counts;
    int cap = keys.length << 1;
    keys = new long[cap];
    counts = new long[cap];
    java.util.Arrays.fill(keys, EMPTY);
    mask = cap - 1;
    size = 0;
    for (int i = 0; i < ok.length; i++)
      if (ok[i] != EMPTY)
        increment(ok[i], oc[i]);
  }

  private static long mix(long x) {
    x ^= x >>> 33;
    x *= 0xff51afd7ed558ccdL;
    x ^= x >>> 33;
    return x;
  }
}
