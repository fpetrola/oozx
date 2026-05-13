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
package com.fpetrola.oozx.speccy.devices.interface2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** A ROM cartridge: 16K, and the name on its label. */
public record Cartridge(String name, byte[] image, String path) {

  public static final int SIZE = 0x4000;

  public static Cartridge read(File file) throws IOException {
    byte[] image = Files.readAllBytes(file.toPath());
    if (image.length != SIZE) {
      throw new IOException(file.getName() + " is " + image.length + " bytes; a cartridge is 16K");
    }
    return new Cartridge(file.getName().replaceFirst("\\.[^.]*$", ""), image, file.getPath());
  }
}
