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

  public SpriteCatalog(String dbPath, int maxSpriteBytes) throws Exception {
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
        if (base < 16384)
          continue;
        // curated background stamps ("udg w=N"): TILE zone no matter the size, and the
        // stride tells the slab builder how far apart a cell's rows really sit
        if (methods != null && methods.startsWith("udg")) {
          // "inv": a multi-block UDG stores its rows bottom-up, so a cell's rows sit
          // stride bytes apart DOWNWARD in memory — a negative stride walks them
          int s = Math.max(1, strideOf.getOrDefault(base, 1));
          if (methods.contains("inv"))
            s = -s;
          for (int a = base; a <= last && a < 0x10000; a++) {
            tileZone[a] = true;
            tileStrideAt[a] = (byte) s;
          }
          tileTemplates++;
          continue;
        }
        if (size <= maxSpriteBytes) {
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
    }
    if (Boolean.getBoolean("log"))
      System.out.println("SpriteCatalog: " + sprites + " sprites <= " + maxSpriteBytes
          + " bytes, " + tileTemplates + " tile templates from " + dbPath);
  }
}
