package com.fpetrola.oozx.proto.inflate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuts a sheet of sprites into sprites.
 * <p>
 * Measured rather than assumed, because a sheet found on the internet is drawn to be looked at and
 * not to be read by a program: the cells are laid on a coloured ground, they are often drawn at
 * two or three times size, and the grid does not necessarily start where you would expect. Every
 * one of those is found here by looking at the picture.
 * <p>
 * The doubling matters more than it sounds. xBRZ is for art drawn one pixel at a time, and handing
 * it a picture whose pixels are already in pairs makes it round the pairs - it smooths the
 * enlargement instead of the drawing, and the result is worse than not scaling at all. The
 * enlargement has to be undone first, which means knowing exactly what it was.
 */
public final class SpriteSheet {

  private SpriteSheet() {
  }

  /** Every sprite on the sheet, in reading order, each at its original size with a clear ground. */
  public static List<BufferedImage> slice(File file) throws Exception {
    BufferedImage sheet = ImageIO.read(file);
    int width = sheet.getWidth(), height = sheet.getHeight();
    // The commonest colour in the WHOLE sheet, which is the one the cells are laid out on and
    // therefore the one separating them. Taken from the top row instead - which sounds like the
    // background and is not - this sheet answers with the grey of its outer margin, the blue
    // panel inside counts as content, and every cell on it joins into one island the size of the
    // page. Nothing then complains: the cell is simply enormous, and the local thickness, which
    // is quadratic, quietly goes away for the rest of the week.
    int ground = commonest(sheet, 0, 0, width, height);

    List<int[]> cells = cells(sheet, ground);
    if (cells.isEmpty()) {
      throw new IllegalStateException(file + " has nothing on it that looks like a grid of cells");
    }
    // If the cells came out a quarter of the page each, the grid was not found, and saying so
    // is worth more than inflating whatever it was.
    int[] first = cells.get(0);
    if ((long) first[2] * first[3] * 20 > (long) width * height || cells.size() < 2) {
      throw new IllegalStateException(file.getName() + ": no grid of cells here - the biggest "
          + "group of same-sized shapes is " + cells.size() + " of " + first[2] + "x" + first[3]
          + " on a " + width + "x" + height + " sheet, which is the page and not a sprite.");
    }
    int[] drawn = doubling(sheet, cells);
    System.out.printf("%s: %d cells of %dx%d, drawn at %dx from offset %d%n",
        file.getName(), cells.size(), cells.get(0)[2], cells.get(0)[3], drawn[0], drawn[1]);

    List<BufferedImage> sprites = new ArrayList<>();
    for (int[] cell : cells) {
      sprites.add(cut(sheet, cell, drawn[0], drawn[1]));
    }
    return sprites;
  }

  /**
   * The cells: islands of anything-but-the-ground, kept only where they are all the same size.
   * <p>
   * The size filter is what throws away the sheet's title. Letters are islands too, and every one
   * of them a different shape; the cells are a hundred and fifty of one size, so the commonest
   * size IS the cell and everything else is decoration.
   */
  private static List<int[]> cells(BufferedImage sheet, int ground) {
    int width = sheet.getWidth(), height = sheet.getHeight();
    boolean[] seen = new boolean[width * height];
    List<int[]> islands = new ArrayList<>();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (seen[y * width + x] || same(sheet.getRGB(x, y), ground)) {
          continue;
        }
        int minX = x, maxX = x, minY = y, maxY = y;
        Deque<int[]> queue = new ArrayDeque<>();
        seen[y * width + x] = true;
        queue.add(new int[]{x, y});
        while (!queue.isEmpty()) {
          int[] at = queue.poll();
          minX = Math.min(minX, at[0]);
          maxX = Math.max(maxX, at[0]);
          minY = Math.min(minY, at[1]);
          maxY = Math.max(maxY, at[1]);
          for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int nx = at[0] + step[0], ny = at[1] + step[1];
            if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
            if (seen[ny * width + nx] || same(sheet.getRGB(nx, ny), ground)) continue;
            seen[ny * width + nx] = true;
            queue.add(new int[]{nx, ny});
          }
        }
        islands.add(new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1});
      }
    }

    Map<Long, Integer> sizes = new HashMap<>();
    for (int[] island : islands) {
      sizes.merge((long) island[2] << 32 | island[3], 1, Integer::sum);
    }
    long commonest = sizes.entrySet().stream()
        .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    List<int[]> cells = new ArrayList<>();
    for (int[] island : islands) {
      if (((long) island[2] << 32 | island[3]) == commonest) {
        cells.add(island);
      }
    }
    // Reading order, with a tolerance so a row whose cells are a pixel apart stays one row.
    cells.sort((one, other) -> Math.abs(one[1] - other[1]) > one[3] / 2
        ? Integer.compare(one[1], other[1]) : Integer.compare(one[0], other[0]));
    return cells;
  }

  /**
   * How many times over the sheet was enlarged, found by looking for the size of block at which
   * every block is one colour - and at which OFFSET, since a grid line can push it over by one.
   */
  private static int[] doubling(BufferedImage sheet, List<int[]> cells) {
    for (int scale = 4; scale >= 2; scale--) {
      for (int offset = 0; offset < scale; offset++) {
        if (uniform(sheet, cells, scale, offset)) {
          return new int[]{scale, offset};
        }
      }
    }
    return new int[]{1, 0};
  }

  private static boolean uniform(BufferedImage sheet, List<int[]> cells, int scale, int offset) {
    for (int[] cell : cells) {
      int across = (cell[2] - offset) / scale, down = (cell[3] - offset) / scale;
      if (across < 1 || down < 1) {
        return false;
      }
      for (int by = 0; by < down; by++) {
        for (int bx = 0; bx < across; bx++) {
          int first = sheet.getRGB(cell[0] + offset + bx * scale, cell[1] + offset + by * scale);
          for (int dy = 0; dy < scale; dy++) {
            for (int dx = 0; dx < scale; dx++) {
              if (!same(first,
                  sheet.getRGB(cell[0] + offset + bx * scale + dx,
                      cell[1] + offset + by * scale + dy))) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * One cell as a sprite: back to its original size, and with the cell's own background made
   * clear, since what is wanted is the figure and not the black square it was shown on.
   */
  private static BufferedImage cut(BufferedImage sheet, int[] cell, int scale, int offset) {
    // The offset is not decoration. The blocks of this sheet start one pixel in, so sampling from
    // the corner of the cell reads the last pixel of the block BEFORE the one wanted: every
    // sprite comes out shifted by a pixel with a column of grid line down its left edge.
    int across = (cell[2] - offset) / scale, down = (cell[3] - offset) / scale;
    int back = around(sheet, cell);
    BufferedImage sprite = new BufferedImage(across, down, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < down; y++) {
      for (int x = 0; x < across; x++) {
        int rgb = sheet.getRGB(cell[0] + offset + x * scale, cell[1] + offset + y * scale);
        sprite.setRGB(x, y, same(rgb, back) ? 0 : 0xFF000000 | rgb);
      }
    }
    return sprite;
  }

  /**
   * The background of a cell, taken from the ring of pixels around its edge rather than from the
   * cell as a whole.
   * <p>
   * The commonest colour in the cell is the background only while the figure leaves most of the
   * cell empty. Several of these guardians do not - a full moon, a row of teeth - and for those
   * the commonest colour IS the figure, so it was the figure that came out transparent and the
   * background that got inflated. They were plain to see: a dozen cells among the hundred and
   * fifty came out inside out. What is reliably background is the border of the cell.
   */
  private static int around(BufferedImage sheet, int[] cell) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int x = 0; x < cell[2]; x++) {
      counts.merge(sheet.getRGB(cell[0] + x, cell[1]) & 0xFFFFFF, 1, Integer::sum);
      counts.merge(sheet.getRGB(cell[0] + x, cell[1] + cell[3] - 1) & 0xFFFFFF, 1, Integer::sum);
    }
    for (int y = 0; y < cell[3]; y++) {
      counts.merge(sheet.getRGB(cell[0], cell[1] + y) & 0xFFFFFF, 1, Integer::sum);
      counts.merge(sheet.getRGB(cell[0] + cell[2] - 1, cell[1] + y) & 0xFFFFFF, 1, Integer::sum);
    }
    return counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
  }

  private static int commonest(BufferedImage image, int x0, int y0, int width, int height) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int y = y0; y < y0 + height && y < image.getHeight(); y++) {
      for (int x = x0; x < x0 + width && x < image.getWidth(); x++) {
        counts.merge(image.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
      }
    }
    return counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
  }

  private static boolean same(int one, int other) {
    return (one & 0xFFFFFF) == (other & 0xFFFFFF);
  }

  /** Writes every sprite out on its own, which is how the cutting gets checked by eye. */
  public static void main(String[] args) throws Exception {
    File out = new File(args.length > 1 ? args[1] : "target/sprites");
    out.mkdirs();
    List<BufferedImage> sprites = slice(new File(args[0]));
    for (int i = 0; i < sprites.size(); i++) {
      ImageIO.write(sprites.get(i), "png", new File(out, String.format("%03d.png", i)));
    }
    // And all of them together, enlarged, on one page.
    int across = 16, side = sprites.get(0).getWidth(), zoom = 3;
    BufferedImage all = new BufferedImage(across * side * zoom,
        ((sprites.size() + across - 1) / across) * side * zoom, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D pen = all.createGraphics();
    pen.setColor(new java.awt.Color(0x202028));
    pen.fillRect(0, 0, all.getWidth(), all.getHeight());
    for (int i = 0; i < sprites.size(); i++) {
      pen.drawImage(sprites.get(i), (i % across) * side * zoom, (i / across) * side * zoom,
          side * zoom, side * zoom, null);
    }
    pen.dispose();
    ImageIO.write(all, "png", new File(out, "all.png"));
    System.out.println("wrote " + sprites.size() + " sprites to " + out);
  }
}
