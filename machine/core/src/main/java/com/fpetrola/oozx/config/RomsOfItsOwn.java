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

package com.fpetrola.oozx.config;

import dev.crystal.plugins.api.RoleInterface;


import java.util.List;
import java.util.Map;

/**
 * The ROMs a jar's own machines and boards are made with.
 * <p>
 * What a machine runs on is knowledge, not a setting: the build knows a 48K is made with
 * 48.rom, and somebody may point that name at a file of their own. A machine that arrives in a
 * jar brings the first half with it - otherwise the emulator would have to have been told about
 * a machine it has never heard of, which is the thing the jar was supposed to avoid.
 * <p>
 * The images themselves travel the same way when they may be given away: a jar's own
 * {@code /roms} is looked in like the build's.
 */
@RoleInterface
public interface RomsOfItsOwn {

  /**
   * The bytes of one of the files this brings, out of the jar it came in.
   * <p>
   * Asked of whoever declares the ROM rather than of one loader over everything: each plugin is
   * loaded on its own, and the only one that can read a file inside a jar is something that came
   * in that jar. A build that carries its ROMs answers the same way.
   */
  default java.io.InputStream open(String rom) {
    return getClass().getResourceAsStream("/roms/" + rom);
  }

  /** Which files each machine or board is made with, under the name it is known by. */
  Map<String, List<String>> files();

  /** The sets somebody can pick between - a Spanish ROM and an English one - if there are any. */
  default Map<String, Map<String, List<String>>> sets() {
    return Map.of();
  }

  /**
   * Where an image that cannot be given away is published, for whoever says yes to fetching it:
   * the jar keeps it as data next to where the image would be, {@code /roms/<file>.json}.
   */
  default Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new java.util.LinkedHashMap<>();
    java.util.stream.Stream.concat(files().values().stream(), sets().values().stream().flatMap(set -> set.values().stream()))
        .flatMap(List::stream).distinct().forEach(rom -> {
          try (java.io.InputStream source = open(rom + ".json")) {
            if (source != null) published.put(rom, new com.fasterxml.jackson.databind.ObjectMapper().readValue(source, RomFiles.Source.class));
          } catch (java.io.IOException unreadable) {
            throw new java.io.UncheckedIOException(unreadable);
          }
        });
    return published;
  }
}
