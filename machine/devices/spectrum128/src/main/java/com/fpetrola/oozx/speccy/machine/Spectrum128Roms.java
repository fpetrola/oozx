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

import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.config.RomsOfItsOwn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What these machines are made with and where the ones that cannot be given away are published,
 * which travels with the machines themselves.
 */
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

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source rom128spanish0rom = new RomFiles.Source();
    rom128spanish0rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/128s.rom";
    rom128spanish0rom.sha256 = "b6a34475bfd5d88d58b5a96f64d5c368d69e5c91a68e8f3c6b2b173b00e66565";
    rom128spanish0rom.at = 0;
    rom128spanish0rom.length = 16384;
    published.put("128-spanish-0.rom", rom128spanish0rom);
    RomFiles.Source rom128spanish1rom = new RomFiles.Source();
    rom128spanish1rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/128s.rom";
    rom128spanish1rom.sha256 = "88b351aea61427b105df32155704d176e09c36e19fe82106007987e654ec948b";
    rom128spanish1rom.at = 16384;
    rom128spanish1rom.length = 16384;
    published.put("128-spanish-1.rom", rom128spanish1rom);
    RomFiles.Source plus2french0rom = new RomFiles.Source();
    plus2french0rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2f.rom";
    plus2french0rom.sha256 = "71c4cf46a41d71df6bfc11acc28edfa234688067c69d85491455d2e293c8beac";
    plus2french0rom.at = 0;
    plus2french0rom.length = 16384;
    published.put("plus2-french-0.rom", plus2french0rom);
    RomFiles.Source plus2french1rom = new RomFiles.Source();
    plus2french1rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2f.rom";
    plus2french1rom.sha256 = "23dad83ea81d852b94bea4c8f2d3d7842ff24c1cb7371538a67be35302d0220a";
    plus2french1rom.at = 16384;
    plus2french1rom.length = 16384;
    published.put("plus2-french-1.rom", plus2french1rom);
    RomFiles.Source plus2spanish0rom = new RomFiles.Source();
    plus2spanish0rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2s.rom";
    plus2spanish0rom.sha256 = "85515a7d06b42c9fb10ed4ed0c6a15f75adbd883dc0192a8a6b77413ccf3360a";
    plus2spanish0rom.at = 0;
    plus2spanish0rom.length = 16384;
    published.put("plus2-spanish-0.rom", plus2spanish0rom);
    RomFiles.Source plus2spanish1rom = new RomFiles.Source();
    plus2spanish1rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2s.rom";
    plus2spanish1rom.sha256 = "ea1714ba42086a9753a4864b9fe03003c5134313f7230af7f3af356dcdee18bc";
    plus2spanish1rom.at = 16384;
    plus2spanish1rom.length = 16384;
    published.put("plus2-spanish-1.rom", plus2spanish1rom);
    return published;
  }
}
