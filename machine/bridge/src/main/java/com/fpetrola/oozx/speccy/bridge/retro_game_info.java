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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.List;

public class retro_game_info extends Structure {

  public String path;
  public Pointer data;
  public NativeLong size;
  public String meta;

  @Override
  protected List<String> getFieldOrder() {
    return List.of("path", "data", "size", "meta");
  }
}
