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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * The discrete sprites the offline analysis already catalogued ({@code sprites_found}, from
 * the track pipeline): base address and extent of each graphic the game ever drew. The taint
 * classifies a screen byte by checking whether an origin lands inside one of these.
 *
 * <p>Only small entries are 3D candidates: a 16x16 game sprite is 32 bytes. The 256-byte
 * room-template records (background identity) stay 2D — they are the part of the screen the
 * voxel sprites float in front of.
 */
public final class SpriteCatalog {
  /** addr -> sprite base + 1, 0 when the address is not part of any catalogued sprite. */
  public final int[] baseOf = new int[0x10000];
  /** base -> catalogued byte size (16x16 = 32 bytes; DD also has 16/24/48/72). */
  public final java.util.Map<Integer, Integer> sizeOf = new java.util.HashMap<>();
  /**
   * base -> bytes per row, when the catalog KNOWS it (curated entries carry "w=N" in the
   * methods column — DD's sprites are 1..3 bytes wide, decoded from the game's own sprite
   * headers via the hand-made disassembly). Default elsewhere: 2 (16px, JSW/MM).
   */
  public final java.util.Map<Integer, Integer> strideOf = new java.util.HashMap<>();
  public int sprites;

  /**
   * catalogued graphics too big to be one sprite (JSW's 256-byte room-template records):
   * their bytes are TILE bitmaps — 8x8 platforms, walls, conveyors, items — and a screen
   * byte whose origin lands here is a tile cell, identified by the leaf address itself.
   */
  public final boolean[] tileZone = new boolean[0x10000];
  public int tileTemplates;
  /**
   * bytes-per-row of the tile bitmap a leaf belongs to: 1 for JSW/MM's plain cell rows,
   * >1 for multi-cell UDG stamps (DD) where a cell's rows sit stride bytes apart.
   * Curated "udg w=N" entries fill it; 0 means default 1.
   */
  private final byte[] tileStrideAt = new byte[0x10000];

  public int tileStride(int leaf) {
    return tileStrideAt[leaf & 0xffff] == 0 ? 1 : tileStrideAt[leaf & 0xffff];
  }

  /**
   * Loads from the readable catalogue ({@code .md}, see {@link SpriteReport}) or from the
   * analysis database ({@code .db}), by extension. The Markdown is preferred wherever it
   * exists because it is the one a person can read, diff and correct — the db stays as the
   * pipeline's own output and as the fallback for games not reported yet.
   */
  public SpriteCatalog(String path, int maxSpriteBytes) throws Exception {
    if (path != null && path.endsWith(".md"))
      fromMarkdown(path, maxSpriteBytes);
    else
      fromDb(path, maxSpriteBytes);
  }

  /**
   * Reads the data lines of the report: {@code base=$edc0 last=$eddf size=32 stride=2
   * tipo=sprite veces=1234}. Everything else in the file — the heading, the drawing, the
   * table — is for the reader, and is skipped. Deliberately forgiving about spacing and
   * order so the file stays hand-editable: correcting a stride there must WORK, not throw.
   */
  private void fromMarkdown(String path, int maxSpriteBytes) throws Exception {
    int lines = 0;
    for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of(path))) {
      String t = line.trim();
      if (!t.startsWith("`base=") || !t.endsWith("`"))
        continue;
      java.util.Map<String, String> f = new java.util.HashMap<>();
      for (String tok : t.substring(1, t.length() - 1).split("\s+")) {
        int eq = tok.indexOf('=');
        if (eq > 0)
          f.put(tok.substring(0, eq), tok.substring(eq + 1));
      }
      int base = num(f.get("base")), last = num(f.get("last"));
      int size = f.containsKey("size") ? num(f.get("size")) : last - base + 1;
      int stride = f.containsKey("stride") ? num(f.get("stride")) : 0;
      String kind = f.getOrDefault("tipo", "sprite");
      if (base <= 0 || last < base)
        continue;
      lines++;
      add(base, last, size, stride, kind, maxSpriteBytes);
    }
    if (Boolean.getBoolean("log"))
      System.out.println("SpriteCatalog: " + sprites + " sprites, " + tileTemplates
          + " zonas de fondo, de " + lines + " entradas en " + path);
  }

  /** "$edc0", "0xedc0" and "60864" all mean the same address. */
  private static int num(String v) {
    if (v == null)
      return -1;
    String s = v.trim();
    try {
      if (s.startsWith("$"))
        return Integer.parseInt(s.substring(1), 16);
      if (s.startsWith("0x"))
        return Integer.parseInt(s.substring(2), 16);
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /** the one place a catalogue row becomes runtime state, whatever file it came from. */
  private void add(int base, int last, int size, int stride, String kind, int maxSpriteBytes) {
    if (base < 16384)
      return; // ROM: the system font, never game graphics (it shreds printed text)
    boolean udg = kind.startsWith("udg");
    if (stride > 0)
      strideOf.put(base, stride);
    if (udg) {
      int s = Math.max(1, stride <= 0 ? 1 : stride);
      if (kind.contains("inv"))
        s = -s; // a multi-block UDG stores its rows bottom-up
      for (int a = base; a <= last && a < 0x10000; a++) {
        tileZone[a] = true;
        tileStrideAt[a] = (byte) s;
      }
      tileTemplates++;
    } else if ("sprite".equals(kind) && size <= maxSpriteBytes) {
      for (int a = base; a <= last && a < 0x10000; a++)
        baseOf[a] = base + 1;
      sizeOf.put(base, size);
      sprites++;
    } else {
      for (int a = base; a <= last && a < 0x10000; a++)
        tileZone[a] = true;
      tileTemplates++;
    }
  }

  private void fromDb(String dbPath, int maxSpriteBytes) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         Statement st = c.createStatement();
         ResultSet rs = st.executeQuery("SELECT base, last, size, methods FROM sprites_found")) {
      while (rs.next()) {
        int base = rs.getInt(1), last = rs.getInt(2), size = rs.getInt(3);
        String methods = rs.getString(4);
        int wTag = methods == null ? -1 : methods.indexOf("w=");
        if (wTag >= 0)
          strideOf.put(base, methods.charAt(wTag + 2) - '0');
        // ROM entries are the SYSTEM's data — the character font, mostly. Game sprites
        // never live there, and treating the font as sprites shreds every printed text
        // (Manic Miner's score/name/AIR bar) into unreadable voxel blobs.
        // curated background stamps ("udg w=N") are TILE zone whatever their size; "inv"
        // means the rows run bottom-up and the slab builder needs a negative stride
        String kind = methods != null && methods.startsWith("udg")
            ? (methods.contains("inv") ? "udg,inv" : "udg")
            : size <= maxSpriteBytes ? "sprite" : "fondo";
        add(base, last, size, strideOf.getOrDefault(base, 0), kind, maxSpriteBytes);
      }
    }
    if (Boolean.getBoolean("log"))
      System.out.println("SpriteCatalog: " + sprites + " sprites <= " + maxSpriteBytes
          + " bytes, " + tileTemplates + " tile templates from " + dbPath);
  }
}
