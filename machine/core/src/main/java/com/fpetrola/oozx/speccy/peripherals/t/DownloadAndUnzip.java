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

package com.fpetrola.oozx.speccy.peripherals.t;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.zip.*;

public class DownloadAndUnzip {

  private static final Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

  public static void main(String[] args) {
    new DownloadAndUnzip().unzip("https://zxinfo.dk/media/zxdb/sinclair/entries/0030743/BigBrother.z80.zip");
  }

  public Path unzip(String zipUrl) {
    String outputDirName = "zxinfo_extracted";

    try {
//      File tempFile = File.createTempFile("zxinfo", "tmp");
      Path extractDir = TMP_DIR.resolve(outputDirName);
      Files.createDirectories(extractDir);

      Path path = downloadAndUnzip(zipUrl, extractDir);
      System.out.println("Descomprimido en: " + extractDir.toAbsolutePath());
      return path;

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches whatever is at a URL into a directory and returns what to load: a zip is unpacked
   * and the entry worth loading chosen, anything else is written as it comes. The RZX Archive
   * offers both - 3188 recordings as plain files and 859 inside zips.
   */
  public static Path fetch(String url, Path directory) throws IOException {
    List<Path> all = fetchAll(url, directory);
    return all.isEmpty() ? null : chooseLoadable(all);
  }

  /**
   * Everything worth loading that came out of a URL, in the order the archive held it.
   * <p>
   * A zip does not always hold one thing: a recording of a game played over several sittings
   * comes as one file per part, and taking any single one of them plays a fifth of the game and
   * looks like a failure. The caller decides what to do with more than one.
   */
  public static List<Path> fetchAll(String url, Path directory) throws IOException {
    Files.createDirectories(directory);
    if (url.toLowerCase().endsWith(".zip")) {
      List<Path> entries = new ArrayList<>();
      for (Path entry : unzipAll(url, directory)) {
        if (!Files.isDirectory(entry) && scoreOf(entry.getFileName().toString()) > Integer.MIN_VALUE + 1) {
          entries.add(entry);
        }
      }
      entries.sort(Comparator.comparing(path -> path.getFileName().toString()));
      return entries;
    }
    String name = url.substring(url.lastIndexOf('/') + 1);
    Path file = directory.resolve(name.isEmpty() ? "download" : name);
    Files.write(file, downloadFile(new URL(url)));
    return List.of(file);
  }

  public static Path downloadAndUnzip(String zipUrl, Path extractTo) throws IOException {
    return chooseLoadable(unzipAll(zipUrl, extractTo));
  }

  private static List<Path> unzipAll(String zipUrl, Path extractTo) throws IOException {
    List<Path> result= new ArrayList<>();
// 1. Descargar el ZIP en memoria (o a disco si es muy grande)
    byte[] zipData = downloadFile(new URL(zipUrl));

    // 2. Descomprimir en memoria
    try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
         ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        Path filePath = extractTo.resolve(entry.getName()).normalize();
        result.add(filePath);
        // Seguridad: evitar path traversal (../)
        if (!filePath.startsWith(extractTo)) {
          throw new IOException("Entrada ZIP maliciosa: " + entry.getName());
        }

        if (entry.isDirectory()) {
          Files.createDirectories(filePath);
        } else {
          Files.createDirectories(filePath.getParent());
          try (OutputStream out = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) > 0) {
              out.write(buffer, 0, len);
            }
          }
        }
        zis.closeEntry();
      }
    }

    return result;
  }

  /**
   * Picks the file to load out of a zip's entries. The first entry is not it: a zip often holds
   * several variants, and which one comes first is whatever order the archive happens to have.
   * Human Killing Machine lists its 128K tape first, so taking entry zero handed a 128K tape to
   * an emulator that boots a 48K machine, and the load died with the machine back in the ROM.
   * <p>
   * Directories are skipped, anything that is not loadable is skipped, and between two variants
   * of the same tape the 48K one wins, since that is the machine being emulated.
   */
  public static Path chooseLoadable(List<Path> entries) {
    List<Path> files = new ArrayList<>();
    for (Path entry : entries) {
      if (!Files.isDirectory(entry)) {
        files.add(entry);
      }
    }
    Path best = preferred(files, entry -> entry.getFileName().toString());
    return best != null ? best : entries.get(0);
  }

  /**
   * Picks the best of several candidates by name, or null when none is loadable.
   * <p>
   * The same choice has to be made twice over: once among the files ZXDB lists for a game, which
   * are separate downloads, and again among the entries of the zip that comes back. Three Weeks
   * in Paradise is listed as seven files including both a 48K and a 128K tape, and taking the
   * first handed a 128K tape to a 48K machine even though the chooser inside the zip was right.
   */
  public static <T> T preferred(List<T> candidates, Function<T, String> nameOf) {
    T best = null;
    int bestScore = Integer.MIN_VALUE;
    for (T candidate : candidates) {
      int score = scoreOf(nameOf.apply(candidate));
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return bestScore == Integer.MIN_VALUE + 1 ? null : best;
  }

  private static int scoreOf(String fileName) {
    String name = fileName.toLowerCase();
    // ZXDB lists downloads as .tzx.zip while the entries inside them are plain .tzx, and the
    // same scoring serves both.
    if (name.endsWith(".zip")) {
      name = name.substring(0, name.length() - 4);
    }
    int score;
    if (name.endsWith(".rzx")) {
      score = 40; // a recording: a zip holding one holds nothing else worth loading
    } else if (name.endsWith(".z80") || name.endsWith(".sna") || name.endsWith(".szx")) {
      score = 30; // a snapshot loads instantly and cannot fail on tape timing
    } else if (name.endsWith(".tzx") || name.endsWith(".tap")) {
      score = 20;
    } else if (name.endsWith(".csw")) {
      score = 10;
    } else {
      return Integer.MIN_VALUE + 1; // not something the emulator can load at all
    }

    // The emulator boots a 48K machine, so a 48K variant beats a 128K one.
    if (name.contains("128")) {
      score -= 5;
    }
    if (name.contains("48")) {
      score += 5;
    }
    // A plain release beats one marked as an alternate or a different dump.
    if (name.contains("different") || name.contains("alternate")) {
      score -= 2;
    }
    return score;
  }

  private static byte[] downloadFile(URL url) throws IOException {
    try (InputStream in = url.openStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    }
  }
}