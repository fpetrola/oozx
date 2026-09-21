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

package com.fpetrola.oozx.speccy.media;

import com.fpetrola.oozx.api.ZxInfoApiHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
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

  /**
   * The file to load out of whatever is at a URL, brought down into the working directory. Not
   * every one of them is a zip: ZXDB hands out .tzx.zip, the TOSEC set at archive.org hands out
   * the .z80 itself, and reading the second as a zip finds nothing in it.
   */
  public Path unzip(String url) {
    try {
      Path file = fetch(url, TMP_DIR.resolve("zxinfo_extracted"));
      if (file == null) {
        throw new IOException("nothing this emulator can open came down from " + url);
      }
      return file;
    } catch (IOException failure) {
      throw new RuntimeException(failure.getMessage(), failure);
    }
  }

  /**
   * Fetches whatever is at a URL into a directory and returns what to load: a zip is unpacked
   * and the entry worth loading chosen, anything else is written as it comes. The RZX Archive
   * offers both - 3188 recordings as plain files and 859 inside zips.
   */
  public static Path fetch(String url, Path directory) throws IOException {
    return chooseLoadable(fetchAll(url, directory));
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
    String name = nameOf(url);
    Path file = directory.resolve(name.isEmpty() ? "download" : name);
    Files.write(file, downloadFile(new URL(url)));
    return List.of(file);
  }

  private static List<Path> unzipAll(String zipUrl, Path extractTo) throws IOException {
    List<Path> result= new ArrayList<>();
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
   * Picks the file to load out of a zip's entries, or out of a folder left by an earlier run. The
   * first entry is not it: a zip often holds several variants, and which one comes first is
   * whatever order the archive happens to have. Human Killing Machine lists its 128K tape first,
   * so taking entry zero handed a 128K tape to an emulator that boots a 48K machine, and the load
   * died with the machine back in the ROM. Neither is the first entry the answer when none of them
   * is loadable: Pac-Man Emulator is published as a zip of its own build, and its Makefile was
   * being handed to the snapshot reader.
   */
  public static Path chooseLoadable(List<Path> entries) {
    return preferred(entries, entry -> entry.getFileName().toString());
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
    return bestScore <= UNLOADABLE ? null : best;
  }

  /**
   * The same candidates, best first, so somebody can be offered the choice in the order this
   * would have made it. One rule decides both, which is the point: the list a person sees and
   * the one picked for them cannot disagree.
   */
  public static <T> List<T> byPreference(List<T> candidates, Function<T, String> nameOf) {
    List<T> ordered = new ArrayList<>(candidates);
    ordered.sort((one, other) -> Integer.compare(scoreOf(nameOf.apply(other)),
        scoreOf(nameOf.apply(one))));
    return ordered;
  }

  /** Whether this is something the emulator can open at all, by the one rule that decides it. */
  public static boolean loadable(String fileName) {
    return scoreOf(fileName) != UNLOADABLE;
  }

  private static final int UNLOADABLE = Integer.MIN_VALUE + 1;

  private static int scoreOf(String fileName) {
    // A URL carries TOSEC's brackets escaped, and the rules below read the name, not the escaping.
    String path = fileName.toLowerCase().replace("%5b", "[");
    // ZXDB puts what it is not allowed to hand out under /denied/. Such a file is still the kind
    // of thing that could be loaded, so it is not rejected here - it is simply the last resort,
    // behind anything that will actually come down.
    int denied = ZxInfoApiHandler.denied(path) ? 50 : 0;
    String name = path.substring(path.lastIndexOf('/') + 1);
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
      return UNLOADABLE; // not something the emulator can load at all
    }
    score -= denied;

    // The emulator boots a 48K machine, so a 48K variant beats a 128K one.
    if (name.contains("128")) {
      score -= 5;
    }
    if (name.contains("48")) {
      score += 5;
    }
    // Not the plain dump: ZXDB says so in words, TOSEC in brackets - [a2], [h Byte Rus], [tr ru],
    // [m tzxtools]. What somebody translated or hung a trainer on is another program and not
    // another file of the same one, so it outweighs any preference between formats.
    if (name.contains("different") || name.contains("alternate") || name.contains("[")) {
      score -= 25;
    }
    return score;
  }

  /**
   * A download that did not happen, carrying a sentence worth showing someone.
   * <p>
   * The reason a file will not come down is nearly always about the file - the archive is not
   * allowed to distribute it, or has not got it any more - and that is worth saying plainly.
   * What was said instead came from {@link URL#openStream()}, which answers a refusal with
   * "Server returned HTTP response code: 403 for URL: https://..." - the whole URL, the class
   * name in front of it, and nothing about what to do.
   */
  public static class DownloadFailed extends IOException {
    public DownloadFailed(String message) {
      super(message);
    }
  }

  private static byte[] downloadFile(URL url) throws IOException {
    URLConnection connection = url.openConnection();
    connection.setConnectTimeout(20_000);
    connection.setReadTimeout(60_000);
    if (connection instanceof HttpURLConnection http) {
      http.setInstanceFollowRedirects(true);
      int status;
      try {
        status = http.getResponseCode();
      } catch (IOException unreachable) {
        throw new DownloadFailed(cannotReach(url, unreachable));
      }
      if (status / 100 != 2) {
        throw new DownloadFailed(refusal(url, status));
      }
    }
    try (InputStream in = connection.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } catch (java.net.SocketTimeoutException timeout) {
      throw new DownloadFailed(url.getHost() + " stopped sending " + nameOf(url) + " partway through");
    }
  }

  /**
   * Why a file did not come down, in words rather than in numbers.
   * <p>
   * The status code is left out of the ordinary answers. Whether the archive said 403 or 404 is
   * a distinction about the archive, not about anything the person can do: either way the file is
   * not available and that is the whole of it. It stays on the odd ones, where a number is the
   * only clue anybody has.
   */
  private static String refusal(URL url, int status) {
    String file = nameOf(url);
    String host = url.getHost();
    return switch (status) {
      case 401, 403 -> file + " is not available: " + host + " is not allowed to hand it out";
      case 404, 410 -> file + " is not available from " + host + " any more";
      case 429 -> host + " is asking for fewer requests just now - worth another go in a minute";
      default -> status / 100 == 5
          ? host + " has a problem of its own and could not send " + file
          : host + " answered HTTP " + status + " for " + file;
    };
  }

  /**
   * Whether this one can be expected to come down at all. ZXDB keeps what it may not distribute
   * under /denied/, so this is known before anything is tried; an entry left with only those is
   * offered its TOSEC files instead.
   */
  public static boolean available(String fileName) {
    return fileName != null && !ZxInfoApiHandler.denied(fileName);
  }

  private static String cannotReach(URL url, IOException failure) {
    if (failure instanceof java.net.UnknownHostException) {
      return "there is no answer for " + url.getHost() + " - the name does not resolve, so this looks like being offline";
    }
    if (failure instanceof java.net.SocketTimeoutException) {
      return url.getHost() + " did not answer in time";
    }
    return url.getHost() + " could not be reached: " + failure.getMessage();
  }

  /**
   * What the thing at a URL or a path is called, as it reads and not as a URL spells it: a TOSEC
   * file arrives with its spaces and brackets escaped, and that escaping belongs neither in the
   * name of a file kept on disk nor in a menu somebody reads.
   */
  public static String nameOf(String urlOrPath) {
    String path = urlOrPath;
    try {
      String decoded = new URI(urlOrPath).getPath();
      path = decoded == null ? urlOrPath : decoded;
    } catch (URISyntaxException aPathAndNotAUrl) {
      // A name with a space or a backslash in it is a path, and already reads as it is.
    }
    return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(File.separatorChar)) + 1);
  }

  private static String nameOf(URL url) {
    String name = nameOf(url.toString());
    return name.isEmpty() ? "the file" : name;
  }
}