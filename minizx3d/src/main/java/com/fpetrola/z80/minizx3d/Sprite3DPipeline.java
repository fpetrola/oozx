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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ties the sprite-to-mesh path together: resolve the config, bake, cache.
 *
 * <p><b>Precedence</b> {@code hand override > automatic selection > viewer default}. The
 * automatic layer only runs with {@code -Dsprite3d.auto=true}, and when nothing overrides
 * and nothing is selected the config handed in is the viewer's own — so with no flags at
 * all every game renders exactly as it did before this class existed.
 *
 * <p><b>Cache</b> keyed by {@code (bitmap, config)}: a sprite retuned live must not get the
 * mesh baked with the previous slider value back. LRU with {@code dispose()} on eviction —
 * these are GPU resources, dropping the reference would leak them.
 */
public final class Sprite3DPipeline {
  /** a libGDX mesh indexes with shorts, so one model cannot exceed this. */
  private static final int MAX_VERTICES = Short.MAX_VALUE;

  private final Sprite3DConfigStore store;
  private final TechniqueSelector selector;
  private final boolean auto;
  private final int capacity;
  private final Map<Long, Model> cache;
  private final Map<Integer, SpriteFeatures> featureCache = new LinkedHashMap<>();
  private long hits, misses, degraded;

  public Sprite3DPipeline(String game, int capacity) {
    this.store = new Sprite3DConfigStore(game);
    this.capacity = capacity;
    this.auto = Boolean.getBoolean("sprite3d.auto");
    String rules = System.getProperty("sprite3d.rules");
    this.selector = new TechniqueSelector(rules != null ? Gdx.files.absolute(rules)
        : Gdx.files.internal("sprite3d-rules.json"));
    this.cache = new LinkedHashMap<>(64, .75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Long, Model> eldest) {
        if (size() <= Sprite3DPipeline.this.capacity)
          return false;
        eldest.getValue().dispose(); // GPU resource: must be released, not just dropped
        return true;
      }
    };
  }

  public Sprite3DConfigStore store() {
    return store;
  }

  public TechniqueSelector selector() {
    return selector;
  }

  public boolean autoEnabled() {
    return auto;
  }

  /** the config that WILL be used for this sprite, without baking anything. */
  public Sprite3DConfig configFor(SpriteBitmap b, Sprite3DConfig viewerDefault) {
    Sprite3DConfig ov = b.base >= 0 ? store.get(b.base) : null;
    if (ov != null) {
      // the override says technique and shape; depth/smoothing still follow the live
      // sliders unless the override changed them away from the defaults
      return ov;
    }
    if (auto && !selector.isEmpty())
      return selector.select(features(b), viewerDefault);
    return viewerDefault;
  }

  public SpriteFeatures features(SpriteBitmap b) {
    if (b.base < 0)
      return SpriteAnalyzer.analyze(b);
    // features describe the GRAPHIC, so they are cached per catalog base: measuring every
    // animation frame of every sprite, every frame, would not pay for itself
    return featureCache.computeIfAbsent(b.base, k -> SpriteAnalyzer.analyze(b));
  }

  /** the mesh for this sprite under the resolved config, baking on first sight. */
  public Model model(SpriteBitmap b, Sprite3DConfig viewerDefault) {
    Sprite3DConfig cfg = configFor(b, viewerDefault);
    long key = b.hash * 31L + cfg.hash();
    Model m = cache.get(key);
    if (m != null) {
      hits++;
      return m;
    }
    misses++;
    MeshBakingStrategy s = MeshBakingStrategy.of(cfg.technique);
    if (s == null)
      s = MeshBakingStrategy.of(Sprite3DConfig.Technique.VOXELS);
    // ask BEFORE baking: over the short-index limit the bake would throw, so step down to
    // a technique whose cost is bounded by the lit pixels instead of by the lattice
    if (s.vertexEstimate(b, cfg) > MAX_VERTICES) {
      degraded++;
      Sprite3DConfig cheap = cfg.copy();
      cheap.technique = Sprite3DConfig.Technique.VOXELS;
      cheap.epx = 1;
      MeshBakingStrategy vox = MeshBakingStrategy.of(Sprite3DConfig.Technique.VOXELS);
      if (vox.vertexEstimate(b, cheap) <= MAX_VERTICES) {
        cfg = cheap;
        s = vox;
      } else
        return null; // caller keeps it 2D; nothing here can mesh it
    }
    m = s.bake(b, cfg);
    if (m != null)
      cache.put(key, m);
    return m;
  }

  public String stats() {
    return "sprite3d: " + cache.size() + " mallas, " + hits + " hits, " + misses
        + " bakes, " + degraded + " degradadas, " + store.size() + " overrides"
        + (auto ? ", auto ON" : "");
  }

  public void clear() {
    cache.values().forEach(Model::dispose);
    cache.clear();
  }

  public void dispose() {
    clear();
  }
}
