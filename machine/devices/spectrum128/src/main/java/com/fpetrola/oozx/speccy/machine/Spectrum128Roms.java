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
public class Spectrum128Roms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Spec128", List.of("128-0.rom", "128-1.rom")),
        Map.entry("SpecPlus2", List.of("plus2-0.rom", "plus2-1.rom")));
  }

  @Override
  public Map<String, Map<String, List<String>>> sets() {
    Map<String, Map<String, List<String>>> sets = new LinkedHashMap<>();
    Map<String, List<String>> spec128 = new LinkedHashMap<>();
    spec128.put("English", List.of("128-0.rom", "128-1.rom"));
    spec128.put("Spanish", List.of("128-spanish-0.rom", "128-spanish-1.rom"));
    sets.put("Spec128", spec128);
    Map<String, List<String>> specplus2 = new LinkedHashMap<>();
    specplus2.put("English", List.of("plus2-0.rom", "plus2-1.rom"));
    specplus2.put("French", List.of("plus2-french-0.rom", "plus2-french-1.rom"));
    specplus2.put("Spanish", List.of("plus2-spanish-0.rom", "plus2-spanish-1.rom"));
    sets.put("SpecPlus2", specplus2);
    return sets;
  }
}
