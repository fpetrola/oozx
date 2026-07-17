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
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;

import java.util.function.IntUnaryOperator;

/**
 * An 8x8 tile extruded into a SOLID slab: unlike the inflated sprites, a platform is a
 * block — every pixel column runs the full depth, paper pixels included, so the tile's
 * pattern shows on the front and back faces and the sides are solid wall. Ink columns
 * stand 1px proud of the paper on both faces, so the pattern also reads as relief.
 *
 * <p>Two parts with named materials ("ink"/"paper") let the caller tint each instance
 * from the cell's own attribute, exactly like the 2D screen colors it.
 */
public final class TileSlabBuilder {
  public static final String INK = "ink", PAPER = "paper";

  /** {@code depth} is the slab's full extent in z, centered on 0. */
  public static Model build(int leaf, IntUnaryOperator memByte, float depth) {
    boolean[][] mask = VoxelSpriteBuilder.mask(leaf, 8, 1, memByte);
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    // one part at a time: ModelBuilder closes the previous part when the next one starts
    MeshPartBuilder ink = mb.part(INK, GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(INK, ColorAttribute.createDiffuse(Color.WHITE)));
    boxes(ink, mask, true, depth + 2);
    MeshPartBuilder paper = mb.part(PAPER, GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(PAPER, ColorAttribute.createDiffuse(Color.WHITE)));
    boxes(paper, mask, false, depth);
    return mb.end();
  }

  private static void boxes(MeshPartBuilder part, boolean[][] mask, boolean set, float depth) {
    for (int y = 0; y < 8; y++)
      for (int x = 0; x < 8; x++)
        if (mask[y][x] == set)
          BoxShapeBuilder.build(part, x - 3.5f, 3.5f - y, 0, 1, 1, depth);
  }

  private TileSlabBuilder() {
  }
}
