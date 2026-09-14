/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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

  /**
   * The ROM sets a machine can be run with, by the name each one goes by - a language, a revision.
   * Keyed like {@link #files}, and the build's knowledge rather than a setting: which of them is
   * running is what {@code files} holds, and that one is the person's.
   */
  public Map<String, Map<String, List<String>>> sets = new LinkedHashMap<>();

  public static final class Source {
    public String url;
    public String sha256;
    /**
     * Where this ROM starts inside what is published, and how long it is, for the archives that
     * publish a machine's ROMs as the one image its chips were read out into. Left out when what
     * is published is the ROM itself.
     */
    public int at;
    public int length;
  }

  /**
   * Asked before anything is fetched, and answered by whoever is in front of the machine. The
   * default answer is no, so a build, a test or a headless run never reaches the network on its own.
   */
  public interface Consent {
    boolean toDownload(String rom, String from);

    /**
     * How much of it has arrived so far, told often enough to be watched. The length is what the
     * other end said it would send, or -1 when it would not say, which is what a bar that cannot
     * show a fraction has to live with.
     */
    default void arriving(String rom, long soFar, long length) {
    }

    /** Nothing more is coming, whether because it all arrived or because it stopped. */
    default void arrived(String rom) {
    }
  }

  private static Consent consent = (rom, from) -> false;

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = "oozx (ZX Spectrum emulator)";

  public static void askingFirst(Consent asking) {
    consent = asking == null ? (rom, from) -> false : asking;
  }

  /** This same section as the build carries it, with nobody's saved settings over it. */
  private static RomFiles ofTheBuild;

  private static RomFiles theBuild() {
    if (ofTheBuild == null) ofTheBuild = Configuration.shipped().of(RomFiles.class);
    return ofTheBuild;
  }

  /**
   * What this build knows about something, and only then what the person's own file says about it.
   * That way round for everything that is knowledge rather than a setting: a run that saved the
   * file while the build knew something else would otherwise go on telling every later run what
   * this build has already stopped believing.
   */
  private static <K> K whatTheBuildKnows(Map<String, K> itsOwn, Map<String, K> theirs, String key) {
    K known = itsOwn.get(key);
    return known != null ? known : theirs.get(key);
  }

  /** Fallback used when the chosen file for a key can't be read. */
  private static List<String> shipped(String key) {
    return theBuild().files.get(key);
  }

  /** Where a ROM is published and what it has to be. */
  public Source sourceFor(String filename) {
    return whatTheBuildKnows(theBuild().sources, sources, filename);
  }

  /** The ROM sets this machine can be run with, by the name each one goes by. */
  public Map<String, List<String>> setsFor(Object device) {
    Map<String, List<String>> known = whatTheBuildKnows(theBuild().sets, sets, keyOf(device));
    return known == null ? Map.of() : known;
  }

  /** The ROMs this machine is running, whether they came from a set or from somebody's own file. */
  public List<String> running(Object device) {
    return files.getOrDefault(keyOf(device), List.of());
  }

  public void runOn(Object device, List<String> roms) {
    files.put(keyOf(device), List.copyOf(roms));
  }

  /**
   * Runs this machine on the set that goes by that name. A machine that shares its ROMs with
   * another - a +2A with a +3 - is choosing for both, which is what sharing them means.
   */
  public void chooseSet(Object device, String name) {
    Map<String, List<String>> known = setsFor(device);
    List<String> set = known.get(name);
    if (set == null)
      throw new IllegalArgumentException("there is no ROM set called '" + name + "' for "
          + device.getClass().getSimpleName() + "; there is " + known.keySet());
    runOn(device, set);
  }

  /** Which of them it is running on, or nothing when it was pointed at a file of somebody's own. */
  public String chosenSet(Object device) {
    List<String> running = running(device);
    for (Map.Entry<String, List<String>> set : setsFor(device).entrySet())
      if (set.getValue().equals(running)) return set.getKey();
    return null;
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
    if (shipped == null || page >= shipped.size()) throw new RomNotLoadedException("nothing was chosen for " + key + ", which has no ROM of its own", null);
    return image(shipped.get(page), length);
  }

  /**
   * The classpath first, for the ROMs that may be shipped; then the file the person chose; then the
   * copy kept from an earlier download. Never the network: this is called while a machine is being
   * built, and a machine being built is no place to ask a question or wait on an answer from far
   * away. What is not here yet is brought by {@link #bring}, before anybody starts a machine.
   */
  private byte[] image(String filename, int length) {
    byte[] image = packaged(filename);
    if (image == null) image = bytesOf(new File(filename));
    if (image == null) image = keptIfItIsStillTheRightOne(filename);
    if (image == null) throw new RomNotLoadedException("couldn't find ROM '" + filename + "'", filename);
    if (image.length != length) throw new RomNotLoadedException("ROM '" + filename + "' is " + image.length + " bytes long; expected " + length, filename);
    return image;
  }

  /** Which of the ROMs this machine or device asks for are not here yet, in the order it asks. */
  public List<String> missingFor(Object device) {
    List<String> missing = new java.util.ArrayList<>();
    for (String filename : files.getOrDefault(keyOf(device), List.of())) {
      if (!here(filename)) missing.add(filename);
    }
    return missing;
  }

  private boolean here(String filename) {
    return RomFiles.class.getResource("/roms/" + filename) != null
        || new File(filename).isFile() || keptIfItIsStillTheRightOne(filename) != null;
  }

  /**
   * The copy kept from an earlier fetch, unless this build has since come to expect different
   * bytes under that name - which happens when a better image of the same ROM is found. Kept
   * copies are not settings: one that no longer matches is fetched again rather than believed.
   */
  private byte[] keptIfItIsStillTheRightOne(String filename) {
    byte[] kept = bytesOf(kept(filename));
    Source source = sourceFor(filename);
    if (kept == null || source == null || source.sha256 == null) return kept;
    return source.sha256.equalsIgnoreCase(digestOf(kept)) ? kept : null;
  }

  /**
   * Brings a ROM that is not here from wherever it is published, and keeps it. Says whether it is
   * here now. Called by whoever is about to start a machine, and never by the machine itself.
   */
  public boolean bring(String filename) {
    return fetched(filename) != null;
  }

  private static byte[] packaged(String filename) {
    try (InputStream packaged = RomFiles.class.getResourceAsStream("/roms/" + filename)) {
      return packaged == null ? null : packaged.readAllBytes();
    } catch (IOException cannot) {
      throw new RomNotLoadedException("ROM '" + filename + "' cannot be read: " + cannot, filename);
    }
  }

  private static byte[] bytesOf(File file) {
    try {
      return file.isFile() ? org.apache.commons.io.FileUtils.readFileToByteArray(file) : null;
    } catch (IOException cannot) {
      throw new RomNotLoadedException("ROM '" + file + "' cannot be read: " + cannot, file.getName());
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
    Source source = sourceFor(filename);
    if (source == null || source.url == null || !consent.toDownload(filename, source.url)) return null;
    byte[] image;
    try {
      image = download(filename, source.url, consent);
    } finally {
      consent.arrived(filename);
    }
    if (source.length > 0) {
      if (image.length < source.at + source.length)
        throw new RomNotLoadedException("ROM '" + filename + "' should be " + source.length + " bytes at " + source.at
            + " of " + source.url + ", which is only " + image.length + " bytes long", filename);
      image = java.util.Arrays.copyOfRange(image, source.at, source.at + source.length);
    }
    mustBe(filename, image, source.sha256);
    try {
      org.apache.commons.io.FileUtils.writeByteArrayToFile(kept(filename), image);
    } catch (IOException couldNotKeepIt) {
      // Not being able to keep it costs a download next time and nothing else.
    }
    return image;
  }

  private static byte[] download(String filename, String from, Consent watching) {
    java.net.URI where = java.net.URI.create(from);
    if ("file".equals(where.getScheme())) {
      byte[] published = bytesOf(new File(where));
      if (published == null) throw new RomNotLoadedException("ROM '" + filename + "' was not at " + from, filename);
      return published;
    }
    try {
      // Saying who is asking, and saying it as this emulator: the archives that publish these ROMs
      // hang up on a request that does not introduce itself, and one of them hangs up on the name
      // this toolkit gives itself when nobody sets one.
      java.net.http.HttpResponse<InputStream> answer = java.net.http.HttpClient.newBuilder()
          .version(java.net.http.HttpClient.Version.HTTP_1_1)
          .connectTimeout(java.time.Duration.ofSeconds(20)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build()
          .send(java.net.http.HttpRequest.newBuilder(where).header("User-Agent", WHO_IS_ASKING)
              .timeout(java.time.Duration.ofSeconds(60)).build(),
              java.net.http.HttpResponse.BodyHandlers.ofInputStream());
      if (answer.statusCode() != 200) throw new RomNotLoadedException("ROM '" + filename + "' was not at " + from + ": " + answer.statusCode(), filename);
      // Read in pieces rather than in one go, which is the only way anybody can be told how it is going.
      long length = answer.headers().firstValueAsLong("content-length").orElse(-1);
      java.io.ByteArrayOutputStream arriving = new java.io.ByteArrayOutputStream();
      byte[] piece = new byte[8192];
      try (InputStream coming = answer.body()) {
        for (int read; (read = coming.read(piece)) > 0; ) {
          arriving.write(piece, 0, read);
          watching.arriving(filename, arriving.size(), length);
        }
      }
      return arriving.toByteArray();
    } catch (IOException | InterruptedException didNotArrive) {
      if (didNotArrive instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new RomNotLoadedException("ROM '" + filename + "' could not be fetched from " + from + ": " + didNotArrive, filename);
    }
  }

  /**
   * What arrived has to be what was expected, to the byte. A ROM is the one thing a machine cannot
   * work around being wrong about: wrong bytes boot into nonsense far from where the mistake was.
   */
  private static void mustBe(String filename, byte[] image, String sha256) {
    if (sha256 == null) throw new RomNotLoadedException("ROM '" + filename + "' says where it comes from but not what it should be", filename);
    String digest = digestOf(image);
    if (!digest.equalsIgnoreCase(sha256))
      throw new RomNotLoadedException("ROM '" + filename + "' is not the one expected: " + digest + " arrived, " + sha256 + " was asked for", filename);
  }

  private static String digestOf(byte[] image) {
    StringBuilder digest = new StringBuilder();
    try {
      for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(image)) digest.append(String.format("%02x", b));
    } catch (java.security.NoSuchAlgorithmException everyJavaHasIt) {
      throw new IllegalStateException(everyJavaHasIt);
    }
    return digest.toString();
  }
}
