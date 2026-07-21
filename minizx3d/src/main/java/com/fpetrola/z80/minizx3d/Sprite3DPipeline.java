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
  private boolean auto;
  private final int capacity;
  private final Map<Long, Model> cache;
  private final Map<Integer, SpriteFeatures> featureCache = new LinkedHashMap<>();
  /** evicted meshes waiting for a moment when nothing references them. */
  private final java.util.List<Model> retired = new java.util.ArrayList<>();
  /** technique chosen per animation strip, so every frame of a character agrees. */
  private final Map<Integer, Sprite3DConfig> groupConfig = new LinkedHashMap<>();
  private int groupSize = Integer.getInteger("sprite3d.group", 256);
  private java.util.function.IntFunction<java.util.List<SpriteBitmap>> groupFrames;
  private long hits, misses, degraded, fellBack;

  public Sprite3DPipeline(String game, int capacity) {
    this.store = new Sprite3DConfigStore(game);
    this.capacity = capacity;
    // ON by default: the geometric-primitive rules ARE the default mechanism now.
    // -Dsprite3d.auto=false falls everything back to the viewer's plain inflation.
    this.auto = !"false".equals(System.getProperty("sprite3d.auto"));
    String rules = System.getProperty("sprite3d.rules");
    this.selector = new TechniqueSelector(rules != null ? Gdx.files.absolute(rules)
        : Gdx.files.internal("sprite3d-rules.json"));
    this.cache = new LinkedHashMap<>(64, .75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Long, Model> eldest) {
        if (size() <= Sprite3DPipeline.this.capacity)
          return false;
        // NOT disposed here: an instance built last frame may still point at it, and
        // rendering a disposed mesh dies as "No buffer allocated!" far from the cause.
        // The caller drains this once the instance lists are empty.
        retired.add(eldest.getValue());
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

  /**
   * Live toggle of the automatic technique selection (the TAB menu). Clearing the per-strip
   * decisions makes the change visible on the next baked frame: with auto off everything
   * falls back to plain inflation, with it on the rules re-vote per character.
   */
  public void setAuto(boolean on) {
    if (on != auto) {
      auto = on;
      groupConfig.clear();
    }
  }

  /** live change of the animation-strip window; re-votes each character on the next frame. */
  public void setGroupSize(int size) {
    if (size != groupSize) {
      groupSize = Math.max(1, size);
      groupConfig.clear();
    }
  }

  /**
   * The animation strip a base belongs to. Every frame of a character is its OWN catalog
   * entry (JSW keeps 8 frames of 32 bytes in one 256-byte page), so selecting per base let
   * two frames of the same guardian land on different techniques and the thing jumped
   * shape as it walked. Grouping by page makes the choice a property of the CHARACTER.
   * {@code -Dsprite3d.group} resizes the page; 1 goes back to per-frame.
   */
  private int groupOf(int base) {
    return base < 0 ? -1 : base & ~(Math.max(1, groupSize) - 1);
  }

  /** the config that WILL be used for this sprite, without baking anything. */
  public Sprite3DConfig configFor(SpriteBitmap b, Sprite3DConfig viewerDefault) {
    int g = groupOf(b.base);
    if (g >= 0) {
      // an override on any frame configures the whole character, which is what "configure
      // this sprite" has to mean; an exact-base entry still wins for hand-curated cases
      Sprite3DConfig ov = store.get(b.base);
      if (ov == null)
        ov = store.get(g);
      if (ov != null)
        return ov;
    }
    if (!auto || selector.isEmpty())
      return viewerDefault;
    // The SHAPE is resolved once per character and reused by every frame (same
    // transformation always), but the finish is re-applied on every call: caching it baked
    // in would mean a knob moved live never reached the sprite.
    Sprite3DConfig cached = g >= 0 ? groupConfig.get(g) : null;
    if (cached != null)
      return finish(cached, viewerDefault);
    Sprite3DConfig chosen = vote(g, b, viewerDefault);
    if (g >= 0)
      groupConfig.put(g, chosen);
    return finish(chosen, viewerDefault);
  }

  /**
   * The technique for a character, decided by MAJORITY over all the frames of its strip.
   *
   * <p>Frames of one character measure very differently — a walking guardian can read as a
   * humanoid in one pose, as thin lettering in another and as an angular shape in a third —
   * so taking whichever frame happened to be drawn first both looked arbitrary and often
   * picked a flat technique for something that is plainly volumetric. Voting also makes the
   * result independent of the order frames appear in, which per-frame selection was not.
   */
  private Sprite3DConfig vote(int group, SpriteBitmap seen, Sprite3DConfig viewerDefault) {
    java.util.List<SpriteBitmap> frames = group >= 0 && groupFrames != null
        ? groupFrames.apply(group) : null;
    if (frames == null || frames.isEmpty())
      return selector.select(features(seen), viewerDefault);
    Map<String, Integer> tally = new LinkedHashMap<>();
    Map<String, Sprite3DConfig> byKey = new LinkedHashMap<>();
    for (SpriteBitmap f : frames) {
      if (f.litPixels() == 0)
        continue;
      Sprite3DConfig c = selector.select(SpriteAnalyzer.analyze(f), viewerDefault);
      String k = c.technique + "/" + c.primitive;
      tally.merge(k, 1, Integer::sum);
      byKey.putIfAbsent(k, c);
    }
    String best = null;
    int bestN = -1;
    for (Map.Entry<String, Integer> e : tally.entrySet()) // ties -> first in address order
      if (e.getValue() > bestN) {
        bestN = e.getValue();
        best = e.getKey();
      }
    return best == null ? selector.select(features(seen), viewerDefault) : byKey.get(best);
  }

  /**
   * A rule decides the SHAPE (technique + primitive); the live knobs decide the FINISH.
   * Without this split a rule that also pinned smoothing or depth made those sliders inert,
   * which is precisely how the smoothing dial came to look broken.
   */
  private Sprite3DConfig finish(Sprite3DConfig chosen, Sprite3DConfig live) {
    Sprite3DConfig c = chosen.copy();
    c.depth = live.depth;
    c.smoothing = live.smoothing;
    c.voxelFill = live.voxelFill;
    c.epx = live.epx;
    c.voxelLook = live.voxelLook;
    c.smoothLevel = live.smoothLevel;
    return c;
  }

  /** supplies every frame of an animation strip, so {@link #vote} can see them all. */
  public void setGroupFrames(java.util.function.IntFunction<java.util.List<SpriteBitmap>> f) {
    this.groupFrames = f;
  }

  public SpriteFeatures features(SpriteBitmap b) {
    int g = groupOf(b.base);
    if (g < 0)
      return SpriteAnalyzer.analyze(b);
    // features describe the CHARACTER, so they are measured once per strip: re-measuring
    // every animation frame is both wasted work and the source of the per-frame jumping
    return featureCache.computeIfAbsent(g, k -> SpriteAnalyzer.analyze(b));
  }

  /** the mesh for this sprite under the resolved config, baking on first sight. */
  public Model model(SpriteBitmap b, Sprite3DConfig viewerDefault) {
    return bake(b, configFor(b, viewerDefault));
  }

  /**
   * The mesh under EXACTLY this config: no rules, no overrides, no auto.
   *
   * <p>{@link #model} takes its argument as a DEFAULT — the store and the automatic selector
   * outrank it — which is right for a sprite the viewer is guessing at and wrong for one a
   * person named by hand and told how to render. Saying "this object is an ovoid" has to
   * mean it even when auto is on, which is exactly when it was being ignored.
   */
  public Model modelForced(SpriteBitmap b, Sprite3DConfig cfg) {
    return bake(b, cfg);
  }

  private Model bake(SpriteBitmap b, Sprite3DConfig cfg) {
    // An explicit depth belongs to THIS sprite, but the builders read the cap as a global
    // (SpriteFx.MAX_DEPTH) and threading a config through every one of them would touch
    // paths validated in four games. Swapped around the bake and restored after: baking is
    // synchronous and on one thread, so the scope is exactly this call.
    float saved = SpriteFx.MAX_DEPTH;
    if (cfg.maxDepth > 0)
      SpriteFx.MAX_DEPTH = cfg.maxDepth;
    try {
      return bakeInner(b, cfg);
    } finally {
      SpriteFx.MAX_DEPTH = saved;
    }
  }

  private Model bakeInner(SpriteBitmap b, Sprite3DConfig cfg) {
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
    // A technique can legitimately produce nothing — surface nets finds no isosurface when
    // the blur swallows a thin frame — and dropping the sprite there made single frames of
    // a walking guardian BLINK OUT while its other frames rendered. Never leave a lit
    // sprite unrendered: fall back to techniques that always yield geometry.
    if (m == null)
      for (Sprite3DConfig.Technique alt : new Sprite3DConfig.Technique[]{
          Sprite3DConfig.Technique.INFLATE, Sprite3DConfig.Technique.VOXELS}) {
        if (alt == cfg.technique)
          continue;
        Sprite3DConfig fb = cfg.copy();
        fb.technique = alt;
        MeshBakingStrategy fs = MeshBakingStrategy.of(alt);
        if (fs.vertexEstimate(b, fb) > MAX_VERTICES)
          continue;
        m = fs.bake(b, fb);
        if (m != null) {
          fellBack++;
          key = b.hash * 31L + fb.hash();
          break;
        }
      }
    if (m != null)
      cache.put(key, m);
    return m;
  }

  public String stats() {
    return "sprite3d: " + cache.size() + " mallas, " + hits + " hits, " + misses
        + " bakes, " + degraded + " degradadas, " + fellBack + " fallback, " + store.size() + " overrides"
        + (auto ? ", auto ON" : "");
  }

  /** meshes evicted since the last call — the caller frees them when it is safe. */
  public java.util.List<Model> drainRetired() {
    if (retired.isEmpty())
      return java.util.Collections.emptyList();
    java.util.List<Model> out = new java.util.ArrayList<>(retired);
    retired.clear();
    return out;
  }

  public void clear() {
    retired.addAll(cache.values());
    cache.clear();
    groupConfig.clear(); // sliders changed: let the rules resolve again
  }

  public void dispose() {
    cache.values().forEach(Model::dispose);
    cache.clear();
    retired.forEach(Model::dispose);
    retired.clear();
  }
}
