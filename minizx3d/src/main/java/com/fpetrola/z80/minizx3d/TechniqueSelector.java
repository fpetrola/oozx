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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a technique from a sprite's {@link SpriteFeatures} using an ORDERED list of rules,
 * first match wins. Deterministic and explainable on purpose: no model, no training, and
 * the thresholds live in data ({@code sprite3d-rules.json}) so they can be retuned without
 * recompiling — {@code -Dsprite3d.rules=<path>} points at your own file.
 *
 * <p>Rule shape: {@code when} maps a feature name to an inclusive {@code [min, max]} range
 * and ALL entries must hold; {@code then} carries the fields to overwrite on the config.
 * <pre>
 * { "name": "texto", "when": { "thinness": [0.55, 1], "components": [3, 999] },
 *   "then": { "technique": "SLAB", "depth": 0.35 } }
 * </pre>
 *
 * <p>Off unless asked for ({@code -Dsprite3d.auto=true}): with it off every sprite keeps the
 * viewer's existing default, which is what makes the whole subsystem safe to ship.
 */
public final class TechniqueSelector {
  private final List<Rule> rules = new ArrayList<>();

  private static final class Rule {
    String name;
    String[] feature;
    float[] lo, hi;
    JsonValue then;
  }

  public TechniqueSelector(FileHandle rulesFile) {
    if (rulesFile == null || !rulesFile.exists())
      return;
    JsonValue root = new JsonReader().parse(rulesFile);
    for (JsonValue r = root.get("rules") == null ? null : root.get("rules").child;
         r != null; r = r.next) {
      Rule rule = new Rule();
      rule.name = r.getString("name", "?");
      rule.then = r.get("then");
      JsonValue when = r.get("when");
      int n = when == null ? 0 : when.size;
      rule.feature = new String[n];
      rule.lo = new float[n];
      rule.hi = new float[n];
      int i = 0;
      for (JsonValue c = when == null ? null : when.child; c != null; c = c.next, i++) {
        rule.feature[i] = c.name;
        rule.lo[i] = c.getFloat(0);
        rule.hi[i] = c.size > 1 ? c.getFloat(1) : Float.MAX_VALUE;
      }
      rules.add(rule);
    }
  }

  public boolean isEmpty() {
    return rules.isEmpty();
  }

  /**
   * The config {@code base} with the first matching rule applied, or {@code base} untouched
   * when nothing matches — an unmatched sprite falls through to the default, it is never
   * forced into a technique.
   */
  public Sprite3DConfig select(SpriteFeatures f, Sprite3DConfig base) {
    for (Rule r : rules) {
      boolean ok = true;
      for (int i = 0; i < r.feature.length && ok; i++) {
        float v = f.get(r.feature[i]);
        ok = !Float.isNaN(v) && v >= r.lo[i] && v <= r.hi[i];
      }
      if (ok) {
        Sprite3DConfig c = base.copy();
        apply(r.then, c);
        return c;
      }
    }
    return base;
  }

  /** name of the rule that would fire, for the tuning overlay. */
  public String explain(SpriteFeatures f) {
    for (Rule r : rules) {
      boolean ok = true;
      for (int i = 0; i < r.feature.length && ok; i++) {
        float v = f.get(r.feature[i]);
        ok = !Float.isNaN(v) && v >= r.lo[i] && v <= r.hi[i];
      }
      if (ok)
        return r.name;
    }
    return "(default)";
  }

  static void apply(JsonValue then, Sprite3DConfig c) {
    if (then == null)
      return;
    for (JsonValue v = then.child; v != null; v = v.next)
      switch (v.name) {
        case "technique":
          c.technique = Sprite3DConfig.Technique.valueOf(v.asString());
          break;
        case "primitive":
          c.primitive = Sprite3DConfig.Primitive.valueOf(v.asString());
          break;
        case "colorMode":
          c.colorMode = Sprite3DConfig.ColorMode.valueOf(v.asString());
          break;
        case "depth":
          c.depth = v.asFloat();
          break;
        case "roundness":
          c.roundness = v.asFloat();
          break;
        case "doubleSided":
          c.doubleSided = v.asBoolean();
          break;
        case "voxelLook":
          c.voxelLook = v.asBoolean();
          break;
        case "epx":
          c.epx = v.asInt();
          break;
        case "smoothLevel":
          c.smoothLevel = v.asInt();
          break;
        case "smoothing":
          c.smoothing = v.asFloat();
          break;
        case "voxelFill":
          c.voxelFill = v.asFloat();
          break;
        case "stackLayers":
          c.stackLayers = v.asInt();
          break;
        default:
          break; // unknown key: ignored, so a newer rules file still loads on older code
      }
  }
}
