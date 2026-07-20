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

/**
 * How one sprite is turned into a mesh. Resolution order is
 * {@code hand override > automatic selection (only when enabled) > this default}, and the
 * default is deliberately the behaviour the viewer already had: {@link Technique#INFLATE}
 * or {@link Technique#VOXELS} depending on the smooth/voxel toggle, at the global depth and
 * smoothing sliders. Turning the new subsystem on must not change a single existing game.
 */
public final class Sprite3DConfig {
  public Technique technique = Technique.INFLATE;
  public Primitive primitive = Primitive.OVOID;
  /** z scale, the per-sprite counterpart of the global depth slider (D/C). */
  public float depth = 1f;
  /** 0 = flat silhouette profile, 1 = fully rounded (primitive techniques). */
  public float roundness = 0.7f;
  /** solid volume (front + mirrored back) vs relief bulging only toward the camera. */
  public boolean doubleSided = true;
  /** EPX/Scale2x passes over the silhouette before baking: 1 = off, 2, 4. */
  public int epx = 1;
  /** silhouette smoothing, the per-sprite counterpart of the S/X slider. */
  public int smoothLevel = 2;
  /** blur radius of the scalar field, {@link Technique#SURFACE_NETS} only. */
  public float smoothing = 1f;
  /** 0..1 gap between voxels for the "Lego" look. */
  public float voxelFill = 1f;
  /** layers stacked in z, {@link Technique#STACK} only. */
  public int stackLayers = 4;
  public ColorMode colorMode = ColorMode.SINGLE;
  /** optional hand-painted depth per pixel (w*h), overrides the technique's own profile. */
  public float[] customDepthMap;

  public Sprite3DConfig copy() {
    Sprite3DConfig c = new Sprite3DConfig();
    c.technique = technique;
    c.primitive = primitive;
    c.depth = depth;
    c.roundness = roundness;
    c.doubleSided = doubleSided;
    c.epx = epx;
    c.smoothLevel = smoothLevel;
    c.smoothing = smoothing;
    c.voxelFill = voxelFill;
    c.stackLayers = stackLayers;
    c.colorMode = colorMode;
    c.customDepthMap = customDepthMap == null ? null : customDepthMap.clone();
    return c;
  }

  /**
   * Identity of the SETTINGS, so the mesh cache can key on (bitmap, config): the same
   * sprite configured two ways is two meshes, and re-tuning a slider must not hand back
   * the model baked with the old value.
   */
  public long hash() {
    long h = 1469598103934665603L;
    h = (h ^ technique.ordinal()) * 1099511628211L;
    h = (h ^ primitive.ordinal()) * 1099511628211L;
    h = (h ^ Float.floatToIntBits(depth)) * 1099511628211L;
    h = (h ^ Float.floatToIntBits(roundness)) * 1099511628211L;
    h = (h ^ (doubleSided ? 1 : 0)) * 1099511628211L;
    h = (h ^ epx) * 1099511628211L;
    h = (h ^ smoothLevel) * 1099511628211L;
    h = (h ^ Float.floatToIntBits(smoothing)) * 1099511628211L;
    h = (h ^ Float.floatToIntBits(voxelFill)) * 1099511628211L;
    h = (h ^ stackLayers) * 1099511628211L;
    h = (h ^ colorMode.ordinal()) * 1099511628211L;
    if (customDepthMap != null)
      for (float f : customDepthMap)
        h = (h ^ Float.floatToIntBits(f)) * 1099511628211L;
    return h;
  }

  public enum Technique {
    /** camera-facing quad: not 3D at all, the guaranteed cheap fallback and HUD path. */
    BILLBOARD,
    /** silhouette extruded to a fixed depth — solid block, reads as architecture. */
    SLAB,
    /** one cube per lit pixel. */
    VOXELS,
    /** depth profile from a primitive surface evaluated over the bounding box. */
    PRIMITIVE,
    /** depth from the distance transform of the silhouette: adapts to any shape. */
    INFLATE,
    /** isosurface over a blurred occupancy field — single smooth skin. */
    SURFACE_NETS,
    /** a few silhouette copies stacked in z: the cheap 2.5D illusion. */
    STACK
  }

  public enum Primitive {
    SPHERE, OVOID, CYL_V, CYL_H, CONE, PILLOW
  }

  public enum ColorMode {
    /** exact per-pixel color through a nearest-filtered texture (keeps attribute clash). */
    TEXTURE,
    VERTEX,
    /** one attribute tint for the whole blob — what the viewer does today. */
    SINGLE
  }
}
