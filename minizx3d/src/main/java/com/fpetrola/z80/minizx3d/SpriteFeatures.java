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
 * Shape measurements of one sprite's silhouette. All deterministic, all cheap, and computed
 * once per bitmap — {@link TechniqueSelector} decides from these and nothing else, so the
 * choice of technique is reproducible and explainable rather than a guess.
 *
 * <p>This is the SECOND layer of classification and answers a different question from the
 * catalog's. The catalog (`TaintDiscover`) decides WHAT a graphic is — sprite or background.
 * These features decide HOW to render it — which is why a "sprite" that turns out to be
 * text and a "sprite" that turns out to be a humanoid can get different techniques without
 * either of them stopping being a sprite.
 */
public final class SpriteFeatures {
  /** height / width of the silhouette's bounding box. */
  public final float aspect;
  /** lit pixels over the bounding box area: how densely the shape fills its box. */
  public final float fill;
  /** lit pixels over the convex hull's area: 1 = convex blob, low = spidery/angular. */
  public final float solidity;
  /** agreement between the silhouette and its horizontal mirror, 0..1. */
  public final float symmetryV;
  /** connected groups of lit pixels (8-connectivity). */
  public final int components;
  /** enclosed background regions — eyes, windows, the hole in a donut. */
  public final int holes;
  /** share of lit pixels one step or less from the edge: lines, limbs, lettering. */
  public final float thinness;
  public final int width, height, litPixels;

  SpriteFeatures(float aspect, float fill, float solidity, float symmetryV, int components,
                 int holes, float thinness, int width, int height, int litPixels) {
    this.aspect = aspect;
    this.fill = fill;
    this.solidity = solidity;
    this.symmetryV = symmetryV;
    this.components = components;
    this.holes = holes;
    this.thinness = thinness;
    this.width = width;
    this.height = height;
    this.litPixels = litPixels;
  }

  /** feature by the name the rule files use; unknown names are a rule that never matches. */
  public float get(String name) {
    switch (name) {
      case "aspect":
        return aspect;
      case "fill":
        return fill;
      case "solidity":
        return solidity;
      case "symmetryV":
        return symmetryV;
      case "components":
        return components;
      case "holes":
        return holes;
      case "thinness":
        return thinness;
      case "width":
        return width;
      case "height":
        return height;
      case "litPixels":
        return litPixels;
      default:
        return Float.NaN;
    }
  }

  @Override
  public String toString() {
    return String.format("aspect=%.2f fill=%.2f solidity=%.2f sym=%.2f comp=%d holes=%d"
            + " thin=%.2f %dx%d lit=%d",
        aspect, fill, solidity, symmetryV, components, holes, thinness, width, height, litPixels);
  }
}
