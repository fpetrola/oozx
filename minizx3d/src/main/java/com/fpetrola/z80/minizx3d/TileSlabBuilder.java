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
  /**
   * The paper columns' DEPTH, as its own material. On a dithered scenery (Exolon: ink dots
   * over black paper) the extruded block is mostly paper, so seen from any angle a platform
   * read as a black mass instead of its own colour. Painting the whole paper part with the
   * ink colour was tried and reverted — the room turned into solid single-colour bricks and
   * the dither was lost — but the FRONT FACE is the only part that carries that texture.
   * Splitting the front skin from the depth lets the caller keep the face honest and give
   * the mass behind it the platform's own hue.
   */
  public static final String PAPER_SIDE = "paperSide";
  /** how much of the paper column's depth is the front skin that keeps the paper colour. */
  private static final float SKIN = 0.6f;

  /**
   * {@code depth} is the slab's full extent in z, centered on 0. Returns null for an EMPTY
   * bitmap: an all-paper cell is AIR — the room's background, not a block. Extruding it
   * would entomb the whole room in solid paper-colored slabs (with black paper, a black
   * screen where only the cells moving sprites clear open up as tunnels).
   */
  public static Model build(int leaf, IntUnaryOperator memByte, float depth) {
    return build(leaf, memByte, depth, 1);
  }

  /** {@code stride}: bytes between consecutive rows of this cell (1 = plain 8-byte cell,
   *  >1 = a column inside a wider UDG stamp, as in Dynamite Dan's backgrounds). */
  public static Model build(int leaf, IntUnaryOperator memByte, float depth, int stride) {
    boolean[][] mask = new boolean[8][8];
    for (int y = 0; y < 8; y++) {
      int row = memByte.applyAsInt(leaf + y * stride);
      for (int x = 0; x < 8; x++)
        mask[y][x] = (row & (0x80 >> x)) != 0;
    }
    int inkPixels = 0;
    for (int y = 0; y < 8; y++)
      for (int x = 0; x < 8; x++)
        if (mask[y][x])
          inkPixels++;
    if (inkPixels == 0)
      return null;
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    // one part at a time: ModelBuilder closes the previous part when the next one starts;
    // a part is only created when it has boxes (an empty part breaks the material lookup)
    MeshPartBuilder ink = mb.part(INK, GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(INK, ColorAttribute.createDiffuse(Color.WHITE)));
    boxes(ink, mask, true, depth + 2);
    if (inkPixels < 64) {
      MeshPartBuilder paper = mb.part(PAPER, GL20.GL_TRIANGLES,
          Usage.Position | Usage.Normal, new Material(PAPER, ColorAttribute.createDiffuse(Color.WHITE)));
      // the thin front skin, at the very front of the column, keeps the paper colour
      for (int y = 0; y < 8; y++)
        for (int x = 0; x < 8; x++)
          if (!mask[y][x])
            BoxShapeBuilder.build(paper, x - 3.5f, 3.5f - y, (depth - SKIN) / 2, 1, 1, SKIN);
      MeshPartBuilder side = mb.part(PAPER_SIDE, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
          new Material(PAPER_SIDE, ColorAttribute.createDiffuse(Color.WHITE)));
      for (int y = 0; y < 8; y++)
        for (int x = 0; x < 8; x++)
          if (!mask[y][x])
            BoxShapeBuilder.build(side, x - 3.5f, 3.5f - y, -SKIN / 2, 1, 1, depth - SKIN);
    }
    return mb.end();
  }

  /**
   * The same solid block over an ARBITRARY silhouette, not the 8x8 cell read from memory:
   * this is what an object identified by hand needs when it should read as a piece of
   * architecture — stretched back to the backdrop — instead of as an inflated body. The
   * cell version cannot do it: it always reads eight bytes and makes one 8x8 tile, so a
   * 40x24 object came out as its top-left corner.
   */
  public static Model buildMask(boolean[][] mask, float depth) {
    int rows = mask.length, w = mask[0].length;
    int inkPixels = 0;
    for (boolean[] row : mask)
      for (boolean v : row)
        if (v)
          inkPixels++;
    if (inkPixels == 0)
      return null;
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    MeshPartBuilder ink = mb.part(INK, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
        new Material(INK, ColorAttribute.createDiffuse(Color.WHITE)));
    boxesAt(ink, mask, true, depth + 2, w, rows);
    if (inkPixels < rows * w) {
      MeshPartBuilder paper = mb.part(PAPER, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
          new Material(PAPER, ColorAttribute.createDiffuse(Color.WHITE)));
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          if (!mask[y][x])
            BoxShapeBuilder.build(paper, x - w / 2f, rows / 2f - y, (depth - SKIN) / 2,
                1, 1, SKIN);
      MeshPartBuilder side = mb.part(PAPER_SIDE, GL20.GL_TRIANGLES,
          Usage.Position | Usage.Normal,
          new Material(PAPER_SIDE, ColorAttribute.createDiffuse(Color.WHITE)));
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          if (!mask[y][x])
            BoxShapeBuilder.build(side, x - w / 2f, rows / 2f - y, -SKIN / 2,
                1, 1, depth - SKIN);
    }
    return mb.end();
  }

  private static void boxesAt(MeshPartBuilder part, boolean[][] mask, boolean set, float depth,
      int w, int rows) {
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (mask[y][x] == set)
          BoxShapeBuilder.build(part, x - w / 2f, rows / 2f - y, 0, 1, 1, depth);
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
