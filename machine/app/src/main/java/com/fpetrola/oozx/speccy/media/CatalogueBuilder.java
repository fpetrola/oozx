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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.emulation.helpers.snapshots.SnapshotFactory;
import com.fpetrola.oozx.api.GameFingerprint;
import com.fpetrola.oozx.api.GameSummary;
import com.fpetrola.oozx.api.ZxInfoApiHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Builds the fingerprint catalogue: asks ZXInfo for the games people actually voted for, downloads
 * one image of each, and writes what they look like next to what they are.
 * <p>
 * It sits beside {@link DownloadAndUnzip} rather than in the zxinfo module because it is the one
 * that downloads; that module answers questions about the catalogue and knows nothing about files
 * on disk. It is a main rather than a test: it runs once against somebody else's server, takes
 * hours, and leaves an artefact behind.
 */
public class CatalogueBuilder {
  private static final long POLITE_PAUSE = 400;
  private static final int MOST_VOTED_KEPT = 5000;

  /** Args: the directory to keep the downloads and the catalogue in, and how many games to take. */
  public static void main(String[] args) throws Exception {
    build(Path.of(args.length > 0 ? args[0] : "games"), args.length > 1 ? Integer.parseInt(args[1]) : 1000);
  }

  public static void build(Path directory, int howMany) throws Exception {
    Path catalogue = directory.resolve("catalogue.json");
    GameFingerprint.Index index = new GameFingerprint.Index();
    if (Files.exists(catalogue)) {
      index.load(catalogue);
    }
    ZxInfoApiHandler api = new ZxInfoApiHandler();
    List<GameSummary> wanted = mostVoted(api, directory).stream().limit(howMany).toList();
    System.out.println("juegos a fichar: " + wanted.size() + ", ya en el catalogo: " + index.size());
    int done = 0, failed = 0;
    for (GameSummary game : wanted) {
      if (index.knows(game.id)) {
        continue;
      }
      try {
        Path image = imageOf(api, game, directory.resolve(game.id));
        if (image == null) {
          failed++;
          continue;
        }
        index.add(game, GameFingerprint.of(SnapshotFactory.payloadOf(image.toFile())));
        index.save(catalogue);
        done++;
        System.out.println(done + "/" + wanted.size() + "  " + game + "  <- " + image.getFileName());
      } catch (IOException | RuntimeException failure) {
        failed++;
        System.out.println("  sin bajar: " + game + " (" + failure.getMessage() + ")");
      }
      Thread.sleep(POLITE_PAUSE);
    }
    System.out.println("fichados: " + done + ", sin bajar: " + failed + ", catalogo: " + index.size());
  }

  /**
   * The ranking, kept on disk: walking the whole catalogue for it takes far longer than the
   * downloads it feeds, and it does not change between runs. Asks for many more than any one run
   * needs, so that raising the count later costs downloads and not another walk.
   */
  private static List<GameSummary> mostVoted(ZxInfoApiHandler api, Path directory) throws IOException {
    Path ranking = directory.resolve("most-voted.json");
    ObjectMapper json = new ObjectMapper();
    if (Files.exists(ranking)) {
      return List.of(json.readValue(ranking.toFile(), GameSummary[].class));
    }
    List<GameSummary> voted = api.mostVoted(MOST_VOTED_KEPT);
    Files.createDirectories(directory);
    json.writeValue(ranking.toFile(), voted);
    return voted;
  }

  /** The one already on disk from an earlier run, or the best download the entry offers. */
  private static Path imageOf(ZxInfoApiHandler api, GameSummary game, Path directory) throws IOException {
    if (Files.isDirectory(directory)) {
      try (var kept = Files.walk(directory)) {
        Path already = DownloadAndUnzip.chooseLoadable(kept.filter(Files::isRegularFile).toList());
        if (already != null) {
          return already;
        }
      }
    }
    Map<String, String> offers = ZxInfoApiHandler.filesOf(api.game(game.id));
    List<String> loadable = DownloadAndUnzip.byPreference(
        offers.keySet().stream().filter(DownloadAndUnzip::loadable).toList(), url -> url);
    for (String url : loadable) {
      if (DownloadAndUnzip.available(url)) {
        return DownloadAndUnzip.downloadAndUnzip(url, directory);
      }
    }
    return null;
  }
}
