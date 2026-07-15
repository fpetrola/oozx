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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fluent builder for the ordered {@code Map<String, Object>} findings the detectors emit and
 * Gson serialises to JSON. It replaces the {@code new LinkedHashMap<>()} + repeated
 * {@code m.put(...)} blocks — where optional fields forced a separate {@code if (c) m.put(...)}
 * statement — with a single chained expression whose insertion order (hence JSON key order) is
 * exactly the call order:
 *
 * <pre>{@code
 *   return Rec.of("range", List.of(lo, hi))
 *       .put("size", size)
 *       .putIf(!consumers.isEmpty(), "read_by", consumers)
 *       .putIf(isPrivate, "private", true)
 *       .map();
 * }</pre>
 *
 * The backing map is a {@link LinkedHashMap}, so {@link #map()} preserves insertion order.
 */
public final class Rec {
  private final Map<String, Object> m = new LinkedHashMap<>();

  private Rec() {
  }

  /** an empty record. */
  public static Rec of() {
    return new Rec();
  }

  /** a record with its first field. */
  public static Rec of(String key, Object value) {
    return new Rec().put(key, value);
  }

  /** add a field, keeping insertion (= JSON) order; returns this for chaining. */
  public Rec put(String key, Object value) {
    m.put(key, value);
    return this;
  }

  /** add a field only when {@code cond} holds — replaces {@code if (cond) m.put(k, v)}. */
  public Rec putIf(boolean cond, String key, Object value) {
    if (cond)
      m.put(key, value);
    return this;
  }

  /** the built map, typed as {@code Map<String, Object>} for the call site. */
  public Map<String, Object> map() {
    return m;
  }
}
