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

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class KVPair extends Structure {
  public KVPair(Pointer pData) {
    super(pData);
  }

  public static class ByReference extends KVPair implements Structure.ByReference {
    public ByReference(Pointer pData) {
      super(pData);
    }
  }

  public static class ByValue extends KVPair implements Structure.ByValue {
    public ByValue(Pointer pData) {
      super(pData);
    }
  }

  public String description;
  public int key;
  public int value;
  public int pc;

  @Override
  protected List<String> getFieldOrder() {
    return Arrays.asList("description", "key", "value", "pc");
  }
}
