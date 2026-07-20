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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import java.util.EnumMap;
import java.util.Map;

/**
 * One way of turning a {@link SpriteBitmap} into a mesh. Small on purpose: the pipeline
 * picks a strategy (hand override, then the rule-based selector, then the default) and this
 * is the whole contract it needs.
 *
 * <p>{@link #vertexEstimate} is not decoration. A libGDX mesh indexes its vertices with
 * shorts, so one model tops out at 32767, and blowing that throws "Too many vertices" at
 * bake time. Asking the strategy FIRST lets the caller fall back to a cheaper technique for
 * an oversized sprite instead of crashing or dropping it.
 */
public interface MeshBakingStrategy {

  Model bake(SpriteBitmap bmp, Sprite3DConfig cfg);

  int vertexEstimate(SpriteBitmap bmp, Sprite3DConfig cfg);

  /** the registry; {@link #of} is how callers reach a technique. */
  Map<Sprite3DConfig.Technique, MeshBakingStrategy> REGISTRY = Registry.build();

  static MeshBakingStrategy of(Sprite3DConfig.Technique t) {
    return REGISTRY.get(t);
  }

  final class Registry {
    private Registry() {
    }

    static Map<Sprite3DConfig.Technique, MeshBakingStrategy> build() {
      EnumMap<Sprite3DConfig.Technique, MeshBakingStrategy> m =
          new EnumMap<>(Sprite3DConfig.Technique.class);
      m.put(Sprite3DConfig.Technique.VOXELS, new Voxels());
      m.put(Sprite3DConfig.Technique.INFLATE, new Inflate());
      m.put(Sprite3DConfig.Technique.SLAB, new Slab());
      m.put(Sprite3DConfig.Technique.BILLBOARD, new Billboard());
      m.put(Sprite3DConfig.Technique.STACK, new Stack());
      m.put(Sprite3DConfig.Technique.PRIMITIVE, new PrimitiveProfile());
      m.put(Sprite3DConfig.Technique.SURFACE_NETS, new SurfaceNets());
      return m;
    }
  }

  /** §3.3 — delegates to the existing builder, so the voxel look is bit-for-bit the same. */
  final class Voxels implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      SpriteBitmap s = SpriteFx.preprocess(b, c);
      if (c.voxelFill >= 1f && c.customDepthMap == null && c.doubleSided)
        return VoxelSpriteBuilder.build(0, s.data.length, s.wBytes, s.memView(),
            c.smoothLevel, c.depth);
      boolean[][] mask = s.mask();
      return VoxelSpriteBuilder.buildWithDepth(mask,
          SpriteFx.depthField(s, c, mask), c.voxelFill, c.doubleSided);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      return VoxelSpriteBuilder.vertexCount(b.litPixels() * (c.epx > 1 ? c.epx * c.epx : 1));
    }
  }

  /** §3.5 — the distance-transform inflation the viewer already used as its default. */
  final class Inflate implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      SpriteBitmap s = SpriteFx.preprocess(b, c);
      return SmoothSpriteBuilder.build(0, s.data.length, s.wBytes, s.memView(),
          c.smoothLevel, c.depth);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      int k = c.epx > 1 ? c.epx : 1;
      return SmoothSpriteBuilder.vertexCount(b.wBytes * k, b.rows * k);
    }
  }

  /** §3.2 — solid extrusion; the existing tile builder works on any 8x8 bitmap. */
  final class Slab implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      return TileSlabBuilder.build(0, b.memView(), c.depth, 1);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      return 24 * b.litPixels() * 2;
    }
  }

  /** §3.1 — a flat quad. Not 3D: the guaranteed fallback when nothing else fits. */
  final class Billboard implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      boolean[][] mask = b.mask();
      float[][] d = new float[mask.length][mask[0].length];
      for (float[] row : d)
        java.util.Arrays.fill(row, .5f);
      return VoxelSpriteBuilder.buildWithDepth(mask, d, 1f, true);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      return VoxelSpriteBuilder.vertexCount(b.litPixels());
    }
  }

  /** §3.8 — a few silhouette copies stacked in z: cheap volume, retro look. */
  final class Stack implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      SpriteBitmap s = SpriteFx.preprocess(b, c);
      boolean[][] mask = s.mask();
      int rows = mask.length, w = mask[0].length;
      int layers = Math.max(1, c.stackLayers);
      ModelBuilder mb = new ModelBuilder();
      mb.begin();
      MeshPartBuilder part = mb.part("stack", GL20.GL_TRIANGLES,
          Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.WHITE)));
      float step = Math.max(1f, c.depth * 2f) / layers;
      for (int l = 0; l < layers; l++) {
        float z = (l - (layers - 1) / 2f) * step;
        for (int y = 0; y < rows; y++)
          for (int x = 0; x < w; x++)
            if (mask[y][x])
              com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder.build(part,
                  x - w / 2f + .5f, rows / 2f - y - .5f, z, 1, 1, step * .9f);
      }
      return mb.end();
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      return VoxelSpriteBuilder.vertexCount(b.litPixels()) * Math.max(1, c.stackLayers);
    }
  }

  /**
   * §3.4 — thickness from a primitive surface, then SMOOTHED: the geometric shape gives the
   * volume and the usual blur passes give the finish. Voxel boxes are still reachable with
   * {@code voxelFill < 1}, which is the only reason to want the chunky version here.
   */
  final class PrimitiveProfile implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      SpriteBitmap s = SpriteFx.preprocess(b, c);
      if (c.voxelFill < 1f) {
        boolean[][] mask = s.mask();
        return VoxelSpriteBuilder.buildWithDepth(mask, SpriteFx.depthField(s, c, mask),
            c.voxelFill, c.doubleSided);
      }
      return SmoothSpriteBuilder.build(0, s.data.length, s.wBytes, s.memView(),
          c.smoothLevel, c.depth, c.primitive, c.roundness);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      int k = c.epx > 1 ? c.epx : 1;
      if (c.voxelFill < 1f)
        return VoxelSpriteBuilder.vertexCount(b.litPixels() * k * k);
      return SmoothSpriteBuilder.vertexCount(b.wBytes * k, b.rows * k);
    }
  }

  /** §3.6 — isosurface over a blurred occupancy field. */
  final class SurfaceNets implements MeshBakingStrategy {
    public Model bake(SpriteBitmap b, Sprite3DConfig c) {
      return SurfaceNetsBuilder.build(SpriteFx.preprocess(b, c), c);
    }

    public int vertexEstimate(SpriteBitmap b, Sprite3DConfig c) {
      return SurfaceNetsBuilder.vertexEstimate(b, c);
    }
  }
}
