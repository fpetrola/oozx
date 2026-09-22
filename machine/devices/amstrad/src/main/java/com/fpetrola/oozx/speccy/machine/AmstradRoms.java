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

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source plus3410rom = new RomFiles.Source();
    plus3410rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2a41.rom";
    plus3410rom.sha256 = "5a15b395a60f6cae87c80d560265d7f5ce3bf99f5ee8c57598d32918c106696d";
    plus3410rom.at = 0;
    plus3410rom.length = 16384;
    published.put("plus3-41-0.rom", plus3410rom);
    RomFiles.Source plus3411rom = new RomFiles.Source();
    plus3411rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2a41.rom";
    plus3411rom.sha256 = "32e44e9c622a8e50f1c46e88a1008df7c7583244c3d3736c9d5e406b98c8ff04";
    plus3411rom.at = 16384;
    plus3411rom.length = 16384;
    published.put("plus3-41-1.rom", plus3411rom);
    RomFiles.Source plus3412rom = new RomFiles.Source();
    plus3412rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2a41.rom";
    plus3412rom.sha256 = "aa92f4e0c8ef1f324b9d565009d1f1b37079057ebc97c39a6def33209e29499e";
    plus3412rom.at = 32768;
    plus3412rom.length = 16384;
    published.put("plus3-41-2.rom", plus3412rom);
    RomFiles.Source plus3413rom = new RomFiles.Source();
    plus3413rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2a41.rom";
    plus3413rom.sha256 = "4ded16ccbcaf8043ed9acec49556dd04e79910d2e32e4bda6e5f4dbbd8c25a09";
    plus3413rom.at = 49152;
    plus3413rom.length = 16384;
    published.put("plus3-41-3.rom", plus3413rom);
    RomFiles.Source plus3spanish0rom = new RomFiles.Source();
    plus3spanish0rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2as.rom";
    plus3spanish0rom.sha256 = "25e00cb18566d9886b5996eda55ab2983d1d917bc5f097b45e4dc82321b91480";
    plus3spanish0rom.at = 0;
    plus3spanish0rom.length = 16384;
    published.put("plus3-spanish-0.rom", plus3spanish0rom);
    RomFiles.Source plus3spanish1rom = new RomFiles.Source();
    plus3spanish1rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2as.rom";
    plus3spanish1rom.sha256 = "21389868b3762028d1f209d7ce8d26022288d41bb309b628a807ad1b5829995d";
    plus3spanish1rom.at = 16384;
    plus3spanish1rom.length = 16384;
    published.put("plus3-spanish-1.rom", plus3spanish1rom);
    RomFiles.Source plus3spanish2rom = new RomFiles.Source();
    plus3spanish2rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2as.rom";
    plus3spanish2rom.sha256 = "3e44807556d6acdfba231cd16e35524b72268f805f1a247dca3621c5f0c1a454";
    plus3spanish2rom.at = 32768;
    plus3spanish2rom.length = 16384;
    published.put("plus3-spanish-2.rom", plus3spanish2rom);
    RomFiles.Source plus3spanish3rom = new RomFiles.Source();
    plus3spanish3rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/p2as.rom";
    plus3spanish3rom.sha256 = "24a9fff69995e0fe6397b6437e3e7ae60010b6ff65e1680087f578c27871a9ef";
    plus3spanish3rom.at = 49152;
    plus3spanish3rom.length = 16384;
    published.put("plus3-spanish-3.rom", plus3spanish3rom);
    return published;
  }
}
