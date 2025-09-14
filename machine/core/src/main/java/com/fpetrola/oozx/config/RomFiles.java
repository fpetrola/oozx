/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps each machine or device to the ROM file(s) it reads; callers ask by class, never by filename. */
@Section("roms")
public final class RomFiles implements Roms {
  /** Keyed by simple class name, since device modules aren't importable here; an absent key means no ROM chosen yet. */
  public Map<String, List<String>> files = new LinkedHashMap<>();

  /** Fallback used when the chosen file for a key can't be read. */
  private static Map<String, List<String>> shipped;

  private static List<String> shipped(String key) {
    if (shipped == null) shipped = Configuration.shipped().of(RomFiles.class).files;
    return shipped.get(key);
  }

  public String nameOf(Object device) {
    List<String> chosen = files.get(keyOf(device));
    return chosen == null || chosen.isEmpty() ? "none" : chosen.get(0);
  }

  public void choose(Object device, String file) {
    choose(keyOf(device), file);
  }

  public void choose(String key, String file) {
    if (file == null) files.remove(key); else files.put(key, List.of(file));
  }

  public byte[] of(Spectrum machine, int page, int length) {
    return read(keyOf(machine), page, length);
  }

  public byte[] of(Peripheral device, int length) {
    return read(keyOf(device), 0, length);
  }

  /** Walks up to the nearest superclass that has an entry, since a device may be held by its interface elsewhere. */
  private String keyOf(Object device) {
    for (Class<?> c = device.getClass(); c != null; c = c.getSuperclass())
      if (files.containsKey(c.getSimpleName())) return c.getSimpleName();
    return device.getClass().getSimpleName();
  }

  private byte[] read(String key, int page, int length) {
    List<String> chosen = files.get(key);
    List<String> shipped = shipped(key);
    if (chosen != null && page < chosen.size()) {
      try {
        return image(chosen.get(page), length);
      } catch (RomNotLoadedException chosenOneIsNoGood) {
        if (shipped == null || shipped.equals(chosen)) throw chosenOneIsNoGood;
      }
    }
    if (shipped == null || page >= shipped.size()) throw new RomNotLoadedException("nothing was chosen for " + key + ", which has no ROM of its own");
    return image(shipped.get(page), length);
  }

  /** Tries the classpath first (shippable ROMs), then disk (for ROMs that can't be redistributed). */
  private static byte[] image(String filename, int length) {
    byte[] image;
    try (InputStream packaged = RomFiles.class.getResourceAsStream("/roms/" + filename)) {
      File onDisk = new File(filename);
      image = packaged != null ? packaged.readAllBytes() : onDisk.isFile() ? org.apache.commons.io.FileUtils.readFileToByteArray(onDisk) : null;
    } catch (IOException cannot) {
      throw new RomNotLoadedException("ROM '" + filename + "' cannot be read: " + cannot);
    }
    if (image == null) throw new RomNotLoadedException("couldn't find ROM '" + filename + "'");
    if (image.length != length) throw new RomNotLoadedException("ROM '" + filename + "' is " + image.length + " bytes long; expected " + length);
    return image;
  }
}
