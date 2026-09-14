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

  /**
   * Where a ROM this project does not ship is published, and which bytes it has to turn out to be.
   * Keyed by filename, since a file is a file whichever machine asks for it.
   */
  public Map<String, Source> sources = new LinkedHashMap<>();

  public static final class Source {
    public String url;
    public String sha256;
  }

  /**
   * Asked before anything is fetched, and answered by whoever is in front of the machine. The
   * default answer is no, so a build, a test or a headless run never reaches the network on its own.
   */
  public interface Consent {
    boolean toDownload(String rom, String from);
  }

  private static Consent consent = (rom, from) -> false;

  public static void askingFirst(Consent asking) {
    consent = asking == null ? (rom, from) -> false : asking;
  }

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

  /**
   * The classpath first, for the ROMs that may be shipped; then the file the person chose; then the
   * copy kept from an earlier download; and only then, for a ROM that says where it is published,
   * the network. A packaged ROM is answered before any of that, so it can never reach out.
   */
  private byte[] image(String filename, int length) {
    byte[] image = packaged(filename);
    if (image == null) image = bytesOf(new File(filename));
    if (image == null) image = bytesOf(kept(filename));
    if (image == null) image = fetched(filename);
    if (image == null) throw new RomNotLoadedException("couldn't find ROM '" + filename + "'");
    if (image.length != length) throw new RomNotLoadedException("ROM '" + filename + "' is " + image.length + " bytes long; expected " + length);
    return image;
  }

  private static byte[] packaged(String filename) {
    try (InputStream packaged = RomFiles.class.getResourceAsStream("/roms/" + filename)) {
      return packaged == null ? null : packaged.readAllBytes();
    } catch (IOException cannot) {
      throw new RomNotLoadedException("ROM '" + filename + "' cannot be read: " + cannot);
    }
  }

  private static byte[] bytesOf(File file) {
    try {
      return file.isFile() ? org.apache.commons.io.FileUtils.readFileToByteArray(file) : null;
    } catch (IOException cannot) {
      throw new RomNotLoadedException("ROM '" + file + "' cannot be read: " + cannot);
    }
  }

  /** The copy of a downloaded ROM, which belongs to the person and not to the build. */
  public static File kept(String filename) {
    return new File(new File(Configuration.home(), "roms"), new File(filename).getName());
  }

  /**
   * A ROM fetched from where it is published, if it is published, if the person says so, and if the
   * bytes that arrive are the bytes that were expected. The one that arrives is kept, so it is
   * asked for once and not once a boot.
   */
  private byte[] fetched(String filename) {
    Source source = sources.get(filename);
    if (source == null || source.url == null || !consent.toDownload(filename, source.url)) return null;
    byte[] image = download(filename, source.url);
    mustBe(filename, image, source.sha256);
    try {
      org.apache.commons.io.FileUtils.writeByteArrayToFile(kept(filename), image);
    } catch (IOException couldNotKeepIt) {
      // Not being able to keep it costs a download next time and nothing else.
    }
    return image;
  }

  private static byte[] download(String filename, String from) {
    java.net.URI where = java.net.URI.create(from);
    if ("file".equals(where.getScheme())) {
      byte[] published = bytesOf(new File(where));
      if (published == null) throw new RomNotLoadedException("ROM '" + filename + "' was not at " + from);
      return published;
    }
    try {
      java.net.http.HttpResponse<byte[]> answer = java.net.http.HttpClient.newBuilder()
          .connectTimeout(java.time.Duration.ofSeconds(20)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build()
          .send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(from)).timeout(java.time.Duration.ofSeconds(60)).build(),
              java.net.http.HttpResponse.BodyHandlers.ofByteArray());
      if (answer.statusCode() != 200) throw new RomNotLoadedException("ROM '" + filename + "' was not at " + from + ": " + answer.statusCode());
      return answer.body();
    } catch (IOException | InterruptedException didNotArrive) {
      if (didNotArrive instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new RomNotLoadedException("ROM '" + filename + "' could not be fetched from " + from + ": " + didNotArrive);
    }
  }

  /**
   * What arrived has to be what was expected, to the byte. A ROM is the one thing a machine cannot
   * work around being wrong about: wrong bytes boot into nonsense far from where the mistake was.
   */
  private static void mustBe(String filename, byte[] image, String sha256) {
    if (sha256 == null) throw new RomNotLoadedException("ROM '" + filename + "' says where it comes from but not what it should be");
    StringBuilder digest = new StringBuilder();
    try {
      for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(image)) digest.append(String.format("%02x", b));
    } catch (java.security.NoSuchAlgorithmException everyJavaHasIt) {
      throw new IllegalStateException(everyJavaHasIt);
    }
    if (!digest.toString().equalsIgnoreCase(sha256))
      throw new RomNotLoadedException("ROM '" + filename + "' is not the one expected: " + digest + " arrived, " + sha256 + " was asked for");
  }
}
