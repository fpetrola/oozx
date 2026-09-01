/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.bridge;

import com.sun.jna.Structure;

import java.util.List;

public class MemoryPageStructure extends Structure {
  public int writable;
  public int contended;
  public int source;
  public int save_to_snapshot; // Should this page be saved to snapshots?
  public int page_num; // Which page from the source
  public int offset;

  @Override
  protected List<String> getFieldOrder() {
    return List.of("writable", "contended", "source", "save_to_snapshot", "page_num", "offset");
  }
}
