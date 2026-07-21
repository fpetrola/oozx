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
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * The catalogue as something a PERSON can read: a Markdown file with every discovered
 * graphic drawn in ASCII, plus a contact sheet PNG of the whole set.
 *
 * <p>And it is not a report on the side — {@link SpriteCatalog} READS this file, so what the
 * viewer runs on is the same text you are looking at. That is the point: a catalogue kept
 * only as rows in a SQLite table is invisible, so nobody notices when a re-catalogue quietly
 * changes a sprite's size or drops a character. As text it diffs in git, greps, and can be
 * corrected by hand — the data line under each heading is the source of truth, and a
 * programmer who can see that a graphic came out 8 bytes short can fix it right there.
 *
 * <p>Two characters per pixel ({@code ██} / {@code ··}) on purpose: a monospace cell is
 * about twice as tall as it is wide, so one character per pixel squashes every sprite into
 * something you cannot recognise.
 */
public final class SpriteReport {
  /** one catalogued graphic, exactly the columns the runtime needs plus what a human wants */
  public record Entry(int base, int last, int size, int veces, int frameFirst, int frameLast,
                      String kind, int stride, String metrics) {
  }

  private static final int PX_ON = 0xffffffff, PX_OFF = 0x101018ff, PX_BG = 0x000000ff;

  /**
   * Writes {@code <path>.md} and {@code <path>.png}. {@code memByte} reads the game's memory
   * as it stood at the end of the run: the graphics live there, and drawing them is the only
   * way the file can show WHAT was found rather than just where.
   */
  public static void write(String mdPath, String game, List<Entry> entries,
      IntUnaryOperator memByte) throws Exception {
    write(mdPath, game, entries, memByte, java.util.List.of());
  }

  /**
   * Same, plus the OBJECTS the game composed on screen ({@link SpriteComposites}). Those go
   * first in the file and get the colour contact sheet, because they are what the game
   * shows; the catalogue entries below them are the pieces, and what the runtime loads.
   */
  public static void write(String mdPath, String game, List<Entry> entries,
      IntUnaryOperator memByte, List<SpriteComposites.Composite> composites) throws Exception {
    List<Entry> sorted = new ArrayList<>(entries);
    sorted.sort((a, b) -> b.veces - a.veces);
    StringBuilder sb = new StringBuilder();
    sb.append("# Catálogo de gráficos — ").append(game).append("\n\n");
    sb.append("> Generado por `TaintDiscover`. **Esto no es documentación: es la fuente que")
        .append(" carga el visor** (`SpriteCatalog` lee este archivo).\n>\n")
        .append("> Cada gráfico tiene una línea de datos entre acentos graves; eso es lo que")
        .append(" se parsea. El dibujo es para vos.\n> Corregir un `stride` o un `tipo` a mano")
        .append(" acá cambia lo que se renderiza, y el `git diff` de este archivo te dice qué")
        .append(" cambió entre dos catalogaciones.\n\n");
    if (!composites.isEmpty()) {
      sb.append("## Objetos compuestos (lo que se ve en pantalla)\n\n")
          .append("Agrupados por adyacencia sobre los bytes con dueño, igual que el visor en")
          .append(" modo `blobs=adjacent`: un objeto es lo que el juego COMPUSO ahí, aunque")
          .append(" salga de varias piezas compartidas. En color en `")
          .append(name(mdPath)).append(".png`.\n\n");
      int n = 0;
      for (SpriteComposites.Composite c : composites) {
        sb.append("### objeto ").append(++n).append(" — ").append(c.wBytes * 8).append('x')
            .append(c.rows).append(" px · visto ").append(c.count).append(" veces · frames ")
            .append(c.firstFrame).append("-").append(c.lastFrame).append("\n\n");
        sb.append("compuesto por: ");
        int k = 0;
        for (Map.Entry<Integer, Integer> pe : sortedPieces(c)) {
          sb.append(k++ > 0 ? " · " : "").append("[`$").append(hex(pe.getKey()))
              .append("`](#").append(hex(pe.getKey())).append(") ").append(pe.getValue())
              .append(" B");
        }
        sb.append("\n\n```\n").append(compositeArt(c)).append("```\n\n");
      }
      sb.append("---\n\n## Piezas del catálogo (lo que carga el visor)\n\n");
    }
    sb.append("| gráfico | tipo | bytes | ancho | apariciones | frames |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (Entry e : sorted)
      sb.append("| [`$").append(hex(e.base)).append("`](#").append(hex(e.base)).append(") | ")
          .append(e.kind).append(" | ").append(e.size).append(" | ").append(e.stride * 8)
          .append("px | ").append(e.veces).append(" | ").append(e.frameFirst).append("-")
          .append(e.frameLast).append(" |\n");
    sb.append("\n---\n\n");
    for (Entry e : sorted) {
      sb.append("## <a name=\"").append(hex(e.base)).append("\"></a>$").append(hex(e.base))
          .append(" — ").append(e.kind).append(" de ").append(e.size).append(" B, ")
          .append(e.stride * 8).append("px de ancho\n\n");
      sb.append("`base=$").append(hex(e.base)).append(" last=$").append(hex(e.last))
          .append(" size=").append(e.size).append(" stride=").append(e.stride)
          .append(" tipo=").append(e.kind).append(" veces=").append(e.veces).append("`\n\n");
      if (e.metrics != null && !e.metrics.isBlank())
        sb.append("<sub>").append(e.metrics.trim()).append("</sub>\n\n");
      sb.append("```\n").append(art(e, memByte)).append("```\n\n");
    }
    java.nio.file.Path md = java.nio.file.Path.of(mdPath);
    if (md.getParent() != null)
      java.nio.file.Files.createDirectories(md.getParent());
    java.nio.file.Files.writeString(md, sb.toString());
    String stem = mdPath.replaceAll("\\.md$", "");
    if (composites.isEmpty()) {
      contactSheet(stem + ".png", sorted, memByte);
      System.out.println("catálogo legible: " + md + "  +  " + stem + ".png");
    } else {
      // the sheet a person looks at is the OBJECTS, in colour, as the game drew them; the
      // pieces get their own sheet, which is a different question ("what did it catalogue")
      compositeSheet(stem + ".png", composites);
      contactSheet(stem + "-piezas.png", sorted, memByte);
      System.out.println("catálogo legible: " + md + "  +  " + stem + ".png (objetos) + "
          + stem + "-piezas.png");
    }
  }

  private static List<Map.Entry<Integer, Integer>> sortedPieces(SpriteComposites.Composite c) {
    List<Map.Entry<Integer, Integer>> l = new ArrayList<>(c.pieces.entrySet());
    l.sort((a, b) -> b.getValue() - a.getValue());
    return l;
  }

  private static String name(String mdPath) {
    String f = mdPath.replaceAll("\\.md$", "");
    return f.contains("/") ? f.substring(f.lastIndexOf('/') + 1) : f;
  }

  /** the composed object as text, same two-characters-per-pixel as the pieces. */
  private static String compositeArt(SpriteComposites.Composite c) {
    StringBuilder out = new StringBuilder();
    for (int y = 0; y < c.rows; y++) {
      for (int b = 0; b < c.wBytes; b++) {
        int v = c.bits[y * c.wBytes + b] & 0xff;
        for (int bit = 0; bit < 8; bit++)
          out.append((v & (0x80 >> bit)) != 0 ? "██" : "··");
      }
      out.append('\n');
    }
    return out.toString();
  }

  /** the Spectrum palette, so the sheet shows the objects in the colours the game used. */
  private static final int[] PALETTE = {
      0x000000, 0x0000d7, 0xd70000, 0xd700d7, 0x00d700, 0x00d7d7, 0xd7d700, 0xd7d7d7,
      0x000000, 0x0000ff, 0xff0000, 0xff00ff, 0x00ff00, 0x00ffff, 0xffff00, 0xffffff};

  /** the objects, in colour, laid out like a sprite sheet: this is the page you look at. */
  private static void compositeSheet(String path, List<SpriteComposites.Composite> cs)
      throws Exception {
    int scale = 2, pad = 6, label = 7, cols = 8, cellW = 0, cellH = 0;
    for (SpriteComposites.Composite c : cs) {
      cellW = Math.max(cellW, c.wBytes * 8 * scale);
      cellH = Math.max(cellH, c.rows * scale);
    }
    cellW += pad;
    cellH += pad + label;
    int rows = (cs.size() + cols - 1) / cols;
    int w = cols * cellW + pad, h = Math.max(1, rows) * cellH + pad;
    int[] px = new int[w * h];
    java.util.Arrays.fill(px, PX_BG);
    for (int i = 0; i < cs.size(); i++) {
      SpriteComposites.Composite c = cs.get(i);
      int ox = pad + (i % cols) * cellW, oy = pad + (i / cols) * cellH;
      for (int y = 0; y < c.rows; y++)
        for (int b = 0; b < c.wBytes; b++) {
          int v = c.bits[y * c.wBytes + b] & 0xff;
          int inkIdx = c.ink[Math.min(c.ink.length - 1, (y >> 3) * c.cellCols + b)] & 0xf;
          int rgb = PALETTE[inkIdx] << 8 | 0xff;
          for (int bit = 0; bit < 8; bit++) {
            if ((v & (0x80 >> bit)) == 0)
              continue;
            for (int sy = 0; sy < scale; sy++)
              for (int sx = 0; sx < scale; sx++) {
                int x = ox + (b * 8 + bit) * scale + sx, yy = oy + y * scale + sy;
                if (x < w && yy < h)
                  px[yy * w + x] = rgb;
              }
          }
        }
      drawLabel(px, w, h, ox, oy + c.rows * scale + 1, Integer.toString(i + 1));
    }
    writePng(path, px, w, h);
  }

  /**
   * The graphic as text. A catalogue entry can be a whole animation STRIP (taint-discovery
   * cuts at address gaps, so frames stored back to back have nothing to cut on), so it is
   * chopped into chunks of at most 16 rows and laid out side by side: what you want to see
   * is the walk cycle, and stacked vertically it reads as one impossibly tall creature.
   */
  private static String art(Entry e, IntUnaryOperator memByte) {
    int stride = Math.max(1, e.stride), rows = Math.max(1, e.size / stride);
    int chunk = Math.min(rows, 16), frames = (rows + chunk - 1) / chunk;
    frames = Math.min(frames, 8); // a long strip: the first frames say everything
    StringBuilder out = new StringBuilder();
    for (int y = 0; y < chunk; y++) {
      for (int f = 0; f < frames; f++) {
        int row = f * chunk + y;
        if (row >= rows) {
          out.append(" ".repeat(stride * 16));
        } else
          for (int b = 0; b < stride; b++) {
            int v = memByte.applyAsInt(e.base + row * stride + b) & 0xff;
            for (int bit = 0; bit < 8; bit++)
              out.append((v & (0x80 >> bit)) != 0 ? "██" : "··");
          }
        if (f < frames - 1)
          out.append("  ");
      }
      out.append('\n');
    }
    return out.toString();
  }

  /**
   * All the graphics in one image, labelled with their address in a 3x5 font: the first
   * question about a new game is "did the catalogue find characters or garbage?", and that
   * is answered by looking, not by reading a table.
   */
  private static void contactSheet(String path, List<Entry> entries, IntUnaryOperator memByte)
      throws Exception {
    int scale = 2, pad = 4, label = 7, cols = 12;
    int cw = 16 * 2 * scale + pad, ch = 16 * scale + label + pad;
    int rows = (entries.size() + cols - 1) / cols;
    int w = cols * cw + pad, h = Math.max(1, rows) * ch + pad;
    int[] px = new int[w * h];
    java.util.Arrays.fill(px, PX_BG);
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      int ox = pad + (i % cols) * cw, oy = pad + (i / cols) * ch;
      int stride = Math.max(1, e.stride), n = Math.max(1, e.size / stride);
      for (int y = 0; y < Math.min(n, 16); y++)
        for (int b = 0; b < Math.min(stride, 4); b++) {
          int v = memByte.applyAsInt(e.base + y * stride + b) & 0xff;
          for (int bit = 0; bit < 8; bit++) {
            boolean on = (v & (0x80 >> bit)) != 0;
            for (int sy = 0; sy < scale; sy++)
              for (int sx = 0; sx < scale; sx++) {
                int x = ox + (b * 8 + bit) * scale + sx, yy = oy + y * scale + sy;
                if (x < w && yy < h)
                  px[yy * w + x] = on ? PX_ON : PX_OFF;
              }
          }
        }
      drawLabel(px, w, h, ox, oy + 16 * scale + 1, hex(e.base));
    }
    writePng(path, px, w, h);
  }

  /**
   * javax.imageio and not libGDX's Pixmap: this runs in the OFFLINE pass, which has no GL
   * context and no natives loaded, and a contact sheet is not worth booting either.
   */
  private static void writePng(String path, int[] px, int w, int h) throws Exception {
    java.awt.image.BufferedImage img =
        new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        img.setRGB(x, y, px[y * w + x] >>> 8); // RGBA -> RGB
    java.nio.file.Path p = java.nio.file.Path.of(path);
    if (p.getParent() != null)
      java.nio.file.Files.createDirectories(p.getParent());
    javax.imageio.ImageIO.write(img, "png", p.toFile());
  }

  /** a 3x5 hex font, because a contact sheet with no addresses is a poster, not a tool. */
  private static final String[] GLYPHS = {
      "111101101101111", "010110010010111", "111001111100111", "111001111001111",
      "101101111001001", "111100111001111", "111100111101111", "111001010010010",
      "111101111101111", "111101111001111", "111101111101101", "110101110101110",
      "111100100100111", "110101101101110", "111100111100111", "111100111100100"};

  private static void drawLabel(int[] px, int w, int h, int ox, int oy, String hex) {
    int x = ox;
    for (char c : hex.toCharArray()) {
      int g = Character.digit(c, 16);
      if (g >= 0) {
        String bits = GLYPHS[g];
        for (int y = 0; y < 5; y++)
          for (int b = 0; b < 3; b++)
            if (bits.charAt(y * 3 + b) == '1' && x + b < w && oy + y < h)
              px[(oy + y) * w + x + b] = 0x808080ff;
      }
      x += 4;
    }
  }

  private static String hex(int v) {
    return Integer.toHexString(v);
  }

  private SpriteReport() {
  }
}
