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

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = "oozx (ZX Spectrum emulator)";

  private static final String THE_TAGS_THAT_ARE_DEVICES = "device-";

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
    return Files.exists(Plugins.folder().resolve(board.jar())) && had != null && had == board.asset();
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
      if (!tag.startsWith(THE_TAGS_THAT_ARE_DEVICES)) continue;
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
    them.brought.put(board.jar(), board.asset());
    if (them.configuration != null) them.configuration.save();

    Plugins.add(here);
    return here;
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
