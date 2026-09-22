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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.config.RomsOfItsOwn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What these machines are made with, which travels with the machines themselves. */
public class AmstradRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("SpecPlus3", List.of("plus3-0.rom", "plus3-1.rom", "plus3-2.rom", "plus3-3.rom")),
        Map.entry("SpecPlus3E", List.of("plus3e-0.rom", "plus3e-1.rom", "plus3e-2.rom", "plus3e-3.rom")));
  }

  @Override
  public Map<String, Map<String, List<String>>> sets() {
    Map<String, Map<String, List<String>>> sets = new LinkedHashMap<>();
    Map<String, List<String>> specplus3 = new LinkedHashMap<>();
    specplus3.put("Version 4.0", List.of("plus3-0.rom", "plus3-1.rom", "plus3-2.rom", "plus3-3.rom"));
    specplus3.put("Version 4.1", List.of("plus3-41-0.rom", "plus3-41-1.rom", "plus3-41-2.rom", "plus3-41-3.rom"));
    specplus3.put("Spanish", List.of("plus3-spanish-0.rom", "plus3-spanish-1.rom", "plus3-spanish-2.rom", "plus3-spanish-3.rom"));
    sets.put("SpecPlus3", specplus3);
    return sets;
  }
}
