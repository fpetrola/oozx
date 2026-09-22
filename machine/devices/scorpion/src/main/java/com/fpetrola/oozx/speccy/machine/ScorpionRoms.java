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

/** What these machines are made with, which travels with the machines themselves. */
public class ScorpionRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Scorpion", List.of("scorpion-0.rom", "scorpion-1.rom", "scorpion-2.rom")));
  }

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source scorpion0rom = new RomFiles.Source();
    scorpion0rom.url = "https://speccy4ever.speccy.org/rom/SC140893.rom";
    scorpion0rom.sha256 = "b6e09abc8d7acbca57761a453d26283865b045e94c67a673ed0453df6dda6474";
    scorpion0rom.at = 0;
    scorpion0rom.length = 16384;
    published.put("scorpion-0.rom", scorpion0rom);
    RomFiles.Source scorpion1rom = new RomFiles.Source();
    scorpion1rom.url = "https://speccy4ever.speccy.org/rom/SC140893.rom";
    scorpion1rom.sha256 = "badd2ef7edcf9dafd0fc5309ae8dca06dcd007d07b4885263f69405b5a6d13e3";
    scorpion1rom.at = 16384;
    scorpion1rom.length = 16384;
    published.put("scorpion-1.rom", scorpion1rom);
    RomFiles.Source scorpion2rom = new RomFiles.Source();
    scorpion2rom.url = "https://speccy4ever.speccy.org/rom/SC140893.rom";
    scorpion2rom.sha256 = "0fbba07a833d4dcfc7024eaf313661a0ba8f80a05c6d29b8801c612e10e60dee";
    scorpion2rom.at = 32768;
    scorpion2rom.length = 16384;
    published.put("scorpion-2.rom", scorpion2rom);
    return published;
  }
}
