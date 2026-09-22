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
public class InvesRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Inves", List.of("inves.rom")));
  }

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source invesrom = new RomFiles.Source();
    invesrom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/inves.rom";
    invesrom.sha256 = "b237d9facdc7ecb1b5e277f79870b1d8cba943b05e1b7fb1769a7e43a71742de";
    invesrom.at = 0;
    invesrom.length = 0;
    published.put("inves.rom", invesrom);
    return published;
  }
}
