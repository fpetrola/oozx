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

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hand overrides per sprite, persisted per game in
 * {@code ~/.jsw3d-sprite3d-<game>.json} — same convention as the live config the viewer
 * already keeps ({@code ~/.jsw3d-config-<game>.json}).
 *
 * <p>Keyed by the graphic's CATALOG BASE, not by the bitmap hash. Every animation frame of
 * one character has its own bitmap and therefore its own hash, so hashing would mean
 * configuring a character once per frame — and again for every frame you had not seen yet.
 * The base address is the same for the whole animation, which is what "configure this
 * sprite" has to mean. The bitmap hash stays what it should be: the MESH cache key.
 */
public final class Sprite3DConfigStore {
  private final Map<Integer, Sprite3DConfig> overrides = new LinkedHashMap<>();
  private final String game;

  public Sprite3DConfigStore(String game) {
    this.game = game;
    load();
  }

  public Sprite3DConfig get(int base) {
    return overrides.get(base);
  }

  public boolean has(int base) {
    return overrides.containsKey(base);
  }

  public void put(int base, Sprite3DConfig cfg) {
    overrides.put(base, cfg.copy());
    save();
  }

  /** live edit: takes effect on the next baked frame but is NOT written to disk yet. */
  public void putTransient(int base, Sprite3DConfig cfg) {
    overrides.put(base, cfg.copy());
  }

  public void persist() {
    save();
  }

  public void remove(int base) {
    if (overrides.remove(base) != null)
      save();
  }

  public int size() {
    return overrides.size();
  }

  private Path file() {
    String p = System.getProperty("sprite3d.file");
    return Paths.get(p != null ? p
        : System.getProperty("user.home") + "/.jsw3d-sprite3d-" + game + ".json");
  }

  private void load() {
    try {
      Path f = file();
      if (!Files.exists(f))
        return;
      JsonValue root = new JsonReader().parse(Files.readString(f));
      for (JsonValue v = root.child; v != null; v = v.next) {
        Sprite3DConfig c = new Sprite3DConfig();
        TechniqueSelector.apply(v, c); // same field mapping the rules use
        overrides.put(Integer.parseInt(v.name.replace("$", ""), 16), c);
      }
      if (!overrides.isEmpty())
        System.out.println("Sprite3D: " + overrides.size() + " OVERRIDES a mano desde " + f
            + " — le ganan a las reglas automaticas: " + overrides.keySet().stream()
            .map(k -> "$" + Integer.toHexString(k)).toList());
    } catch (Exception e) {
      System.out.println("Sprite3D: no se pudo leer overrides: " + e);
    }
  }

  private void save() {
    StringBuilder sb = new StringBuilder("{\n");
    int i = 0;
    for (Map.Entry<Integer, Sprite3DConfig> e : overrides.entrySet()) {
      Sprite3DConfig c = e.getValue();
      sb.append(String.format("  \"$%04x\": { \"technique\": \"%s\", \"primitive\": \"%s\","
              + " \"colorMode\": \"%s\", \"depth\": %s, \"roundness\": %s,"
              + " \"doubleSided\": %s, \"epx\": %d, \"smoothLevel\": %d, \"smoothing\": %s,"
              + " \"voxelFill\": %s, \"stackLayers\": %d, \"voxelLook\": %s }%s%n",
          e.getKey(), c.technique, c.primitive, c.colorMode, c.depth, c.roundness,
          c.doubleSided, c.epx, c.smoothLevel, c.smoothing, c.voxelFill, c.stackLayers,
          ++i < overrides.size() ? "," : ""));
    }
    sb.append("}\n");
    try {
      Files.writeString(file(), sb.toString());
    } catch (Exception e) {
      System.out.println("Sprite3D: no se pudo guardar overrides: " + e);
    }
  }
}
