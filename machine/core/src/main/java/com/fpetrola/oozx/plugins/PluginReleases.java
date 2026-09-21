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
 * The boards, brought from where they are published rather than carried in this jar.
 * <p>
 * Each one is a release of its own with one jar in it, so what arrives is a list of names and a
 * list of files, and this only downloads the ones it does not already have: the asset's number is
 * written down beside the file, and an asset that has not been published again is not fetched
 * again. With no network it says so and the machine starts on whatever was brought before.
 * <p>
 * Nothing is fetched without a yes. Somebody else's code arriving on this machine deserves at
 * least what a ROM gets, which is a question first.
 */
@Section("plugins")
public class PluginReleases implements Configuration.Saves {

  /** Where the boards are published: the releases of this repository whose tag names a device. */
  public String repository = "fpetrola/oozx";

  /** Whether to look at all. A no to the question turns it off rather than asking every time. */
  public boolean fetch = true;

  /** Said yes once. */
  public boolean agreed;

  /** Which asset each jar came from, so a later run brings only what was published again. */
  public Map<String, Long> brought = new LinkedHashMap<>();

  /** When the list was last asked for, so that starting the emulator is not a network call. */
  public long lastLooked;

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = "oozx (ZX Spectrum emulator)";

  private static final String THE_TAGS_THAT_ARE_DEVICES = "device-";

  private static final Duration PATIENCE = Duration.ofSeconds(8);

  private static final long A_DAY = 24 * 60 * 60 * 1000L;

  /** Whoever can ask the person, since this end has no windows. */
  public interface Consent {
    boolean toBring(String repository, List<String> boards);
  }

  /** Whoever wants to say what is happening while it happens. */
  public interface Watching {
    void bringing(String board, int which, int of);
  }

  private static Consent consent = (repository, boards) -> false;

  public static void askingFirst(Consent asking) {
    consent = asking == null ? (repository, boards) -> false : asking;
  }

  private Configuration configuration;

  @Override
  public void savedBy(Configuration configuration) {
    this.configuration = configuration;
  }

  /** One published jar: what it is called, where it is, and which asset it is. */
  private record Published(String board, String jar, String from, long asset) {
  }

  /**
   * Brings what is published and not here yet, and says nothing if there is nothing to do. Called
   * before anything is built: a plugin that arrives after a machine has been made is a plugin that
   * machine will not have.
   */
  public static void bringWhatIsPublished(Watching watching) {
    Configuration.shared().of(PluginReleases.class).bring(watching);
  }

  private void bring(Watching watching) {
    if (!fetch) return;
    // Asked once a day, and always when there is nothing here: starting the emulator should not
    // wait on somebody else's server to say what it already knows.
    if (!Plugins.jars().isEmpty() && System.currentTimeMillis() - lastLooked < A_DAY) return;
    lastLooked = System.currentTimeMillis();

    List<Published> published;
    try {
      published = whatIsPublished();
    } catch (IOException | InterruptedException noAnswer) {
      System.out.println("oozx: the boards could not be asked for (" + noAnswer.getMessage()
          + "); starting with what is already here");
      return;
    }

    List<Published> missing = new ArrayList<>();
    for (Published board : published) {
      Path here = Plugins.folder().resolve(board.jar());
      Long had = brought.get(board.jar());
      if (!Files.exists(here) || had == null || had != board.asset()) missing.add(board);
    }
    if (missing.isEmpty()) return;

    if (!agreed && !consent.toBring(repository, missing.stream().map(Published::board).toList())) {
      fetch = false;
      save();
      return;
    }
    agreed = true;

    int which = 0;
    for (Published board : missing) {
      which++;
      if (watching != null) watching.bringing(board.board(), which, missing.size());
      try {
        download(board);
        brought.put(board.jar(), board.asset());
      } catch (IOException | InterruptedException didNotArrive) {
        System.err.println("oozx: " + board.board() + " did not arrive: " + didNotArrive.getMessage());
      }
    }
    save();
  }

  private void save() {
    if (configuration != null) configuration.save();
  }

  /** The releases of the repository, reduced to the ones that are a device and carry a jar. */
  private List<Published> whatIsPublished() throws IOException, InterruptedException {
    HttpResponse<String> answer = client().send(
        asking("https://api.github.com/repos/" + repository + "/releases?per_page=100")
            .header("Accept", "application/vnd.github+json").build(),
        HttpResponse.BodyHandlers.ofString());
    if (answer.statusCode() != 200)
      throw new IOException(repository + " answered " + answer.statusCode());

    List<Published> published = new ArrayList<>();
    for (JsonNode release : new ObjectMapper().readTree(answer.body())) {
      String tag = release.path("tag_name").asText("");
      if (!tag.startsWith(THE_TAGS_THAT_ARE_DEVICES)) continue;
      for (JsonNode asset : release.path("assets")) {
        String name = asset.path("name").asText("");
        if (!name.endsWith(".jar")) continue;
        String board = release.path("name").asText("");
        published.add(new Published(board.isBlank() ? tag : board, name,
            asset.path("browser_download_url").asText(), asset.path("id").asLong()));
        break;
      }
    }
    return published;
  }

  /** Written beside itself and moved into place, so a download cut in half is never loaded. */
  private void download(Published board) throws IOException, InterruptedException {
    Files.createDirectories(Plugins.folder());
    Path half = Plugins.folder().resolve(board.jar() + ".part");
    HttpResponse<Path> answer = client().send(asking(board.from()).build(),
        HttpResponse.BodyHandlers.ofFile(half));
    if (answer.statusCode() != 200) {
      Files.deleteIfExists(half);
      throw new IOException(board.from() + " answered " + answer.statusCode());
    }
    Files.move(half, Plugins.folder().resolve(board.jar()), StandardCopyOption.REPLACE_EXISTING);
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
