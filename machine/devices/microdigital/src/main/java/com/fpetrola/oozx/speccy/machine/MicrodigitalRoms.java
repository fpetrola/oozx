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
public class MicrodigitalRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Tk90x", List.of("tk90x.rom")),
        Map.entry("Tk95", List.of("tk95.rom")));
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

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source tk90xrom = new RomFiles.Source();
    tk90xrom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/tk90x.rom";
    tk90xrom.sha256 = "f3892b4140901f8289b962f5e097cf330c6997260052baac351026639012beeb";
    tk90xrom.at = 0;
    tk90xrom.length = 0;
    published.put("tk90x.rom", tk90xrom);
    RomFiles.Source tk90xspanishrom = new RomFiles.Source();
    tk90xspanishrom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/tk90xs.rom";
    tk90xspanishrom.sha256 = "9eff5867aa1123e84fb366f1e7f68a484666f845c6b33f67a62f6e6b7f1fcb4b";
    tk90xspanishrom.at = 0;
    tk90xspanishrom.length = 0;
    published.put("tk90x-spanish.rom", tk90xspanishrom);
    RomFiles.Source tk95rom = new RomFiles.Source();
    tk95rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/tk95.rom";
    tk95rom.sha256 = "c0f7c0a7b2dd838488189cda052e414cd3474f3a718c1fde0a6cef3e8e0f4776";
    tk95rom.at = 0;
    tk95rom.length = 0;
    published.put("tk95.rom", tk95rom);
    RomFiles.Source tk95spanishrom = new RomFiles.Source();
    tk95spanishrom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/tk95es.rom";
    tk95spanishrom.sha256 = "4aa2421e29487917bfd183012b114a3caacbf10ff46f3686c87e94ba5c492819";
    tk95spanishrom.at = 0;
    tk95spanishrom.length = 0;
    published.put("tk95-spanish.rom", tk95spanishrom);
    return published;
  }
}
