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

package com.fpetrola.oozx.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Section;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The boards that are published, and bringing one in.
 * <p>
 * Each is a release of its own with one jar in it, so the list is a question to an archive and
 * taking one is a download. Nothing here decides to do either: somebody asks for the list and
 * picks what to add, which is the only consent a download needs.
 */
@Section("plugins")
public class PluginReleases implements Configuration.Saves {

  /** Where the boards are published: the releases of this repository whose tag names a device. */
  public String repository = "fpetrola/oozx";

  /** Which asset each jar here came from, so that what is published again is seen to be newer. */
  public Map<String, Long> brought = new LinkedHashMap<>();

  /**
   * The ones taken out while the emulator was running. The file stays where it is until the next
   * start, because the loader holds it open and a jar pulled out from under it makes every later
   * look at the services throw; it simply stops counting from the moment it is listed here.
   */
  public java.util.Set<String> takenOut = new java.util.LinkedHashSet<>();

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = "oozx (ZX Spectrum emulator)";

  /** What a tag or a jar is called when it is something to plug in: a board, or a tool over one. */
  private static final List<String> WHAT_PLUGS_IN = List.of("device-", "tool-");

  private static boolean plugsIn(String named) {
    return WHAT_PLUGS_IN.stream().anyMatch(named::startsWith);
  }

  private static final Duration PATIENCE = Duration.ofSeconds(15);

  private Configuration configuration;

  @Override
  public void savedBy(Configuration configuration) {
    this.configuration = configuration;
  }

  private static PluginReleases theOne() {
    return Configuration.shared().of(PluginReleases.class);
  }

  /** One published board: what it is called, which file it is, where it is, and how big. */
  public record Board(String name, String jar, String from, long asset, long size) {
  }

  /** Where the boards come from, for a window to say so. */
  public static String publishedAt() {
    return theOne().repository;
  }

  /** Whether this board is already here, and the copy is the one that is published. */
  public static boolean isHere(Board board) {
    PluginReleases them = theOne();
    Long had = them.brought.get(board.jar());
    return !them.takenOut.contains(board.jar())
        && Files.exists(Plugins.folder().resolve(board.jar())) && had != null && had == board.asset();
  }

  /** The releases of the repository, reduced to the ones that are a device and carry a jar. */
  public static List<Board> published() throws IOException, InterruptedException {
    String repository = theOne().repository;
    HttpResponse<String> answer = client().send(
        asking("https://api.github.com/repos/" + repository + "/releases?per_page=100")
            .header("Accept", "application/vnd.github+json").build(),
        HttpResponse.BodyHandlers.ofString());
    if (answer.statusCode() != 200)
      throw new IOException(repository + " answered " + answer.statusCode());

    List<Board> boards = new ArrayList<>();
    for (JsonNode release : new ObjectMapper().readTree(answer.body())) {
      String tag = release.path("tag_name").asText("");
      if (!plugsIn(tag)) continue;
      for (JsonNode asset : release.path("assets")) {
        String jar = asset.path("name").asText("");
        if (!jar.endsWith(".jar")) continue;
        String named = release.path("name").asText("");
        boards.add(new Board(named.isBlank() ? tag : named, jar,
            asset.path("browser_download_url").asText(), asset.path("id").asLong(),
            asset.path("size").asLong()));
        break;
      }
    }
    boards.sort(java.util.Comparator.comparing(Board::name));
    return boards;
  }

  /**
   * Brings one in and puts it where the emulator will find it, which is the same as plugging it
   * in: from here on it is one more thing the machine can be built with.
   */
  public static Path bring(Board board) throws IOException, InterruptedException {
    Files.createDirectories(Plugins.folder());
    // Written beside itself and moved into place, so a download cut in half is never loaded.
    Path half = Plugins.folder().resolve(board.jar() + ".part");
    HttpResponse<Path> answer = client().send(asking(board.from()).build(),
        HttpResponse.BodyHandlers.ofFile(half));
    if (answer.statusCode() != 200) {
      Files.deleteIfExists(half);
      throw new IOException(board.from() + " answered " + answer.statusCode());
    }
    Path here = Plugins.folder().resolve(board.jar());
    Files.move(half, here, StandardCopyOption.REPLACE_EXISTING);

    PluginReleases them = theOne();
    them.takenOut.remove(board.jar());
    them.brought.put(board.jar(), board.asset());
    if (them.configuration != null) them.configuration.save();

    Plugins.add(here);
    return here;
  }

  /**
   * Takes one out: written down as out rather than deleted, and deleted when the emulator starts
   * again. The file has to stay where it is while this runs - the loader holds it open, and one
   * pulled out from under it makes every later look at the services throw - so being out is a
   * fact about the list and not about the disk.
   * <p>
   * What was already loaded stays loaded: a class cannot be unloaded from a machine that is using
   * it, so a board taken out now is one that machine keeps until the emulator starts again.
   */
  public static void takeOut(Board board) {
    PluginReleases them = theOne();
    them.brought.remove(board.jar());
    them.takenOut.add(board.jar());
    if (them.configuration != null) them.configuration.save();
  }

  /** Whether this jar was taken out and is only waiting for the next start to go. */
  public static boolean isOut(String jar) {
    return theOne().takenOut.contains(jar);
  }

  /** The ones taken out, thrown away now that nothing has them open. Called once, on the way up. */
  public static void sweep() {
    PluginReleases them = theOne();
    if (them.takenOut.isEmpty()) return;
    for (String jar : them.takenOut) {
      try {
        Files.deleteIfExists(Plugins.folder().resolve(jar));
      } catch (IOException wouldNotGo) {
        System.err.println("oozx: " + jar + " could not be thrown away: " + wouldNotGo.getMessage());
      }
    }
    them.takenOut.clear();
    if (them.configuration != null) them.configuration.save();
  }

  /**
   * The other boards this one was built against, read from its own manifest: a board that uses
   * another's code says so in the jar, so bringing one can bring what it cannot work without.
   */
  public static List<String> needs(Path jar) {
    List<String> boards = new ArrayList<>();
    try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar.toFile())) {
      java.util.jar.Manifest manifest = opened.getManifest();
      if (manifest == null) return boards;
      String classpath = manifest.getMainAttributes().getValue("Class-Path");
      if (classpath == null) return boards;
      for (String named : classpath.split("\\s+")) {
        if (plugsIn(named) && named.endsWith(".jar")) boards.add(named);
      }
    } catch (IOException cannotBeRead) {
      System.err.println("oozx: " + jar.getFileName() + " does not say what it needs: " + cannotBeRead.getMessage());
    }
    return boards;
  }

  private static HttpClient client() {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(PATIENCE)
        .build();
  }

  /** Saying who is asking, because an archive that publishes these hangs up on a request that does not. */
  private static HttpRequest.Builder asking(String url) {
    return HttpRequest.newBuilder(URI.create(url)).timeout(PATIENCE).header("User-Agent", WHO_IS_ASKING);
  }
}
