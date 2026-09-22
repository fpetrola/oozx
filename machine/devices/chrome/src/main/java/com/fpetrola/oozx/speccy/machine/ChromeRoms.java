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
public class ChromeRoms implements RomsOfItsOwn {

  @Override
  public Map<String, List<String>> files() {
    return Map.ofEntries(
        Map.entry("Chrome", List.of("chrome-0.rom", "chrome-1.rom", "chrome-2.rom", "chrome-3.rom")));
  }

  @Override
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source chrome0rom = new RomFiles.Source();
    chrome0rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/chrome.rom";
    chrome0rom.sha256 = "8757f634823518cfd9365eed33221e8b340f9696e79039cc345d6b9ab9d35427";
    chrome0rom.at = 0;
    chrome0rom.length = 16384;
    published.put("chrome-0.rom", chrome0rom);
    RomFiles.Source chrome1rom = new RomFiles.Source();
    chrome1rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/chrome.rom";
    chrome1rom.sha256 = "d55daa439b673b0e3f5897f99ac37ecb45f974d1862b4dadb85dec34af99cb42";
    chrome1rom.at = 16384;
    chrome1rom.length = 16384;
    published.put("chrome-1.rom", chrome1rom);
    RomFiles.Source chrome2rom = new RomFiles.Source();
    chrome2rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/chrome.rom";
    chrome2rom.sha256 = "3ba308f23b9471d13d9ba30c23030059a9ce5d4b317b85b86274b132651d1425";
    chrome2rom.at = 32768;
    chrome2rom.length = 16384;
    published.put("chrome-2.rom", chrome2rom);
    RomFiles.Source chrome3rom = new RomFiles.Source();
    chrome3rom.url = "https://raw.githubusercontent.com/chernandezba/zesarux/da53a8d1755490a97fe6858866bc6eae7aead6f1/src/chrome.rom";
    chrome3rom.sha256 = "8d93c3342321e9d1e51d60afcd7d15f6a7afd978c231b43435a7c0757c60b9a3";
    chrome3rom.at = 49152;
    chrome3rom.length = 16384;
    published.put("chrome-3.rom", chrome3rom);
    return published;
  }
}
