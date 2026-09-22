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

/**
 * What each of these machines is made with, which travels with the machines themselves.
 * <p>
 * The emulator used to carry this, back when it carried the machines: it knew that a Pentagon
 * runs on a 128's ROM and where a Timex's own images are, for machines it no longer has. A jar
 * that brings a machine brings what the machine is made of, or it is not a machine anybody can
 * start.
 * <p>
 * The images that may be given away are in this jar's own {@code /roms}; the rest are named and
 * fetched with a yes, the same as the emulator's own.
 */
public class ClonesRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Pentagon", List.of("128-0.rom", "128-1.rom")),
        Map.entry("Pentagon1024", List.of("128-0.rom", "128-1.rom")),
        Map.entry("Tc2048", List.of("tc2048.rom")),
        Map.entry("Tc2068", List.of("tc2068-0.rom", "tc2068-1.rom")),
        Map.entry("Ts2068", List.of("ts2068-0.rom", "ts2068-1.rom")),
        Map.entry("SpecSe", List.of("se-0.rom", "se-1.rom")),
        Map.entry("Scorpion", List.of("scorpion-0.rom", "scorpion-1.rom", "scorpion-2.rom")),
        Map.entry("Tk90x", List.of("tk90x.rom")),
        Map.entry("Tk95", List.of("tk95.rom")),
        Map.entry("CzSpectrum", List.of("48.rom")),
        Map.entry("CzSpectrumPlus", List.of("inves.rom")),
        Map.entry("Inves", List.of("inves.rom")),
        Map.entry("Chrome", List.of("chrome-0.rom", "chrome-1.rom", "chrome-2.rom", "chrome-3.rom")));
  }

  @Override
  public Map<String, Map<String, List<String>>> sets() {
    Map<String, Map<String, List<String>>> sets = new LinkedHashMap<>();
    Map<String, List<String>> tk90x = new LinkedHashMap<>();
    tk90x.put("Portuguese", List.of("tk90x.rom"));
    tk90x.put("Spanish", List.of("tk90x-spanish.rom"));
    sets.put("Tk90x", tk90x);
    Map<String, List<String>> tk95 = new LinkedHashMap<>();
    tk95.put("Portuguese", List.of("tk95.rom"));
    tk95.put("Spanish", List.of("tk95-spanish.rom"));
    sets.put("Tk95", tk95);
    return sets;
  }
}
