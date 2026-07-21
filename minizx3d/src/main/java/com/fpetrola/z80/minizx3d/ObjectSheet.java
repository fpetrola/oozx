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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The objects marked by hand, as a PNG that is also their DEFINITION: every pixel carries,
 * invisibly, which graphic it came from.
 *
 * <p>A picture and a table of addresses beside it drift apart the first time somebody edits
 * one of them. Here they cannot: the sheet shows the shapes as the game drew them, and the
 * address of the piece each pixel belongs to travels in that same pixel, in the low two bits
 * of R, G and B — a change of at most 3/255 per channel, which on this palette is invisible.
 *
 * <p>Those six bits hold an INDEX, not the address: 16 bits per pixel would take five bits
 * of a channel and start to show. The index is resolved through a legend written the same
 * way along the TOP ROW of the image, three pixels per entry (6+6+4 bits), on a row that is
 * background everywhere else. So the file is self-contained — no sidecar, no metadata chunk
 * a converter can drop — and {@link #read} gives back, for every pixel, the graphic behind
 * it.
 */
public final class ObjectSheet {
  /** one object as the editor captured it: name, size, and one address per pixel (0 = none). */
  public record Obj(String name, int x, int y, int w, int h, java.util.Set<Integer> parts) {
  }

  private static final int MAX_PARTS = 63; // six bits, minus 0 = "no piece here"

  /**
   * @param cells  per object: {x, y, w, h} of where it was laid out
   * @param pixels per object: ARGB per pixel, 0 where nothing was selected
   * @param owner  per object: the graphic address per pixel, -1 where nothing
   */
  public static void write(String path, List<String> names, List<int[]> cells,
      List<int[]> pixels, List<int[]> owner) throws Exception {
    int w = 0, h = 1; // row 0 is the legend
    for (int[] c : cells) {
      w = Math.max(w, c[0] + c[2]);
      h = Math.max(h, c[1] + c[3]);
    }
    w = Math.max(w, 32);
    int[] out = new int[w * h];
    java.util.Arrays.fill(out, 0xff000000);
    // one index per distinct graphic, in first-seen order: that is what the pixels carry
    Map<Integer, Integer> index = new LinkedHashMap<>();
    for (int[] o : owner)
      for (int a : o)
        if (a >= 0 && !index.containsKey(a) && index.size() < MAX_PARTS)
          index.put(a, index.size() + 1);
    for (int n = 0; n < cells.size(); n++) {
      int[] c = cells.get(n), px = pixels.get(n), ow = owner.get(n);
      for (int y = 0; y < c[3]; y++)
        for (int x = 0; x < c[2]; x++) {
          int argb = px[y * c[2] + x];
          if ((argb >>> 24) == 0)
            continue;
          int code = index.getOrDefault(ow[y * c[2] + x], 0);
          out[(c[1] + y) * w + c[0] + x] = 0xff000000 | embed(argb & 0xffffff, code);
        }
    }
    // the legend, along row 0: three pixels per entry, entry i at 3*(i-1)
    for (Map.Entry<Integer, Integer> e : index.entrySet()) {
      int at = (e.getValue() - 1) * 3, addr = e.getKey();
      if (at + 2 >= w)
        break;
      out[at] = 0xff000000 | embed(0, addr & 0x3f);
      out[at + 1] = 0xff000000 | embed(0, (addr >> 6) & 0x3f);
      out[at + 2] = 0xff000000 | embed(0, (addr >> 12) & 0xf);
    }
    java.awt.image.BufferedImage img =
        new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        img.setRGB(x, y, out[y * w + x]);
    java.nio.file.Path p = java.nio.file.Path.of(path);
    if (p.getParent() != null)
      java.nio.file.Files.createDirectories(p.getParent());
    javax.imageio.ImageIO.write(img, "png", p.toFile());
  }

  /** six bits of payload into the low two bits of R, G and B. */
  private static int embed(int rgb, int code) {
    int r = ((rgb >> 16) & 0xff & ~3) | (code & 3);
    int g = ((rgb >> 8) & 0xff & ~3) | ((code >> 2) & 3);
    int b = (rgb & 0xff & ~3) | ((code >> 4) & 3);
    return (r << 16) | (g << 8) | b;
  }

  private static int extract(int rgb) {
    return ((rgb >> 16) & 3) | (((rgb >> 8) & 3) << 2) | ((rgb & 3) << 4);
  }

  /**
   * Reads the sheet back: the graphic address behind every pixel, {@code -1} where there is
   * none. The point of the format is that this needs nothing but the file.
   */
  public static int[][] read(String path) throws Exception {
    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.File(path));
    int w = img.getWidth(), h = img.getHeight();
    List<Integer> legend = new ArrayList<>();
    for (int at = 0; at + 2 < w; at += 3) {
      int lo = extract(img.getRGB(at, 0)), mid = extract(img.getRGB(at + 1, 0));
      int hi = extract(img.getRGB(at + 2, 0)) & 0xf;
      int addr = lo | (mid << 6) | (hi << 12);
      if (addr == 0)
        break;
      legend.add(addr);
    }
    int[][] out = new int[h][w];
    for (int y = 1; y < h; y++)
      for (int x = 0; x < w; x++) {
        int code = extract(img.getRGB(x, y));
        out[y][x] = code == 0 || code > legend.size() ? -1 : legend.get(code - 1);
      }
    return out;
  }

  private ObjectSheet() {
  }
}
