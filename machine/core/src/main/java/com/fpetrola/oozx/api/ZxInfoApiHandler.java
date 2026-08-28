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

// src/main/java/com/example/Main.java
package com.fpetrola.oozx.api;

import com.fpetrola.oozx.speccy.peripherals.t.GameBrowserInternalFrame;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.List;

public class ZxInfoApiHandler {
  private final String BASE_URL = "https://api.zxinfo.dk";

  public static void main(String[] args) {
    new ZxInfoApiHandler().search("everyone wally");
  }

  /** The filter values the search endpoint accepts, for building a filter bar. */
  public Metadata getMetadata() {
    return withClient(ZxInfoClient::getMetadata);
  }

  /**
   * Search narrowed by the filters the server can apply itself. Pass null for a filter to
   * leave it out; the ones the server cannot do, like whether an entry has an RZX recording,
   * have to be applied to the results.
   */
  public List<Hit> search(String query, String machineType, String genreType) {
    SearchResponse response = withClient(zxClient -> zxClient.searchGames(query, 150, "0",
        ZxInfoClient.MODE_COMPACT, null, null, null, null, null, genreType, null, machineType,
        null, null, null, null, null, null, null, null));
    return response.hits.hits;
  }

  public List<Hit> search(String everyoneWally) {
    Client client = null;
    client = ClientBuilder.newClient();
    ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
    ZxInfoClient zxClient = target.proxy(ZxInfoClient.class);
    SearchResponse response = zxClient.searchGames(everyoneWally, 150, "0", ZxInfoClient.MODE_COMPACT,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    client.close();

    return response.hits.hits;
  }

  /**
   * Fetches game details from the API by game ID and converts to GameDetail
   * @param gameId the game ID to fetch
   * @return GameDetail with all available information from the API
   */
  public GameDetail fetchGameDetails(String gameId) {
    Client client = null;
    try {
      client = ClientBuilder.newClient();
      ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
      ZxInfoClient zxClient = target.proxy(ZxInfoClient.class);
      GameResponse response = zxClient.getGameDetails(gameId, ZxInfoClient.MODE_FULL);
      GameEntry gameEntry = response.getGameEntry();

      return convertGameEntryToDetail(gameEntry, gameId);
    } catch (Exception e) {
      System.err.println("Error fetching game details: " + e.getMessage());
      e.printStackTrace();
      return null;
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }

  /**
   * Converts GameEntry from API to GameDetail for UI display
   */
  private GameDetail convertGameEntryToDetail(GameEntry entry, String gameId) {
    GameDetail detail = new GameDetail();

    detail.id = gameId;
    detail.title = entry.title;
    detail.yearOfRelease = entry.originalYearOfRelease != null ? entry.originalYearOfRelease.toString() : null;
    detail.originalMonthOfRelease = entry.originalMonthOfRelease;
    detail.originalDayOfRelease = entry.originalDayOfRelease;
    detail.machineType = entry.machineType;
    detail.genre = entry.genre;
    detail.genreType = entry.genreType;
    detail.genreSubType = entry.genreSubType;
    detail.availability = entry.availability;
    detail.isbn = entry.isbn;
    detail.xrated = entry.xrated;
    detail.contentType = entry.contentType;
    detail.zxinfoVersion = entry.zxinfoVersion;

    // Handle score
    if (entry.score != null) {
      detail.score = entry.score.score != null ? entry.score.score.doubleValue() : null;
    }

    // Handle publishers
    if (entry.publishers != null && !entry.publishers.isEmpty()) {
      detail.publisher = entry.publishers.get(0).name;
      detail.publishers = new java.util.ArrayList<>();
      for (Publisher pub : entry.publishers) {
        detail.publishers.add(pub.name);
      }
    }

    // Handle authors
    if (entry.authors != null && !entry.authors.isEmpty()) {
      detail.authors = new java.util.ArrayList<>();
      for (Author author : entry.authors) {
        if (author.name != null) {
          detail.authors.add(author.name);
        }
      }
    }

    // Handle screenshots
    if (entry.screens != null && !entry.screens.isEmpty()) {
      detail.screenshots = new java.util.ArrayList<>();
      for (Object screenMap : entry.screens) {
        Screen screen = GameBrowserInternalFrame.getScreen(screenMap);
        if (screen != null) {
          String screenshotUrl = null;
          // Try to use URL if available
          if (screen.url != null && !screen.url.isEmpty()) {
            screenshotUrl = screen.url;
          } else if (screen.scrUrl != null && !screen.scrUrl.isEmpty()) {
            screenshotUrl = screen.scrUrl;
          } else if (screen.filename != null && !screen.filename.isEmpty()) {
            // Construct full URL from filename
            screenshotUrl = "https://media.zxinfo.dk/media/" + screen.filename;
          }
          if (screenshotUrl != null) {
            detail.screenshots.add(screenshotUrl);
          }
        }
      }
    }

    // Handle additional downloads
    if (entry.additionalDownloads != null && !entry.additionalDownloads.isEmpty()) {
      detail.additionalDownloads = new java.util.ArrayList<>(entry.additionalDownloads);
      detail.gameMaps = extractGameMaps(entry.additionalDownloads);
    }

    // Handle releases. LinkedHashMap keeps the column order stable across rows,
    // which is what the details table relies on when it derives its columns from row 0.
    if (entry.releases != null && !entry.releases.isEmpty()) {
      detail.releases = new java.util.ArrayList<>();
      for (Release release : entry.releases) {
        java.util.Map<String, String> releaseMap = new java.util.LinkedHashMap<>();
        releaseMap.put("Title", joinTitles(release.releaseTitles, entry.title));
        releaseMap.put("Year", release.yearOfRelease != null ? release.yearOfRelease.toString() : "N/A");
        releaseMap.put("Publisher", firstPublisherName(release.publishers));
        releaseMap.put("Price", formatPrice(release.releasePrice));
        releaseMap.put("Code", release.code != null ? release.code : "");
        releaseMap.put("Barcode", release.barcode != null ? release.barcode : "");
        releaseMap.put("Files", String.valueOf(release.files != null ? release.files.size() : 0));
        detail.releases.add(releaseMap);
      }
    }

    return detail;
  }

  /**
   * Suggestions for a search box, covering titles, publishers and authors.
   */
  public List<Suggestion> suggest(String term) {
    return withClient(zxClient -> zxClient.getSuggestions(term));
  }

  public List<Suggestion> suggestAuthor(String term) {
    return withClient(zxClient -> zxClient.getSuggestionsAuthor(term));
  }

  public List<Suggestion> suggestPublisher(String term) {
    return withClient(zxClient -> zxClient.getSuggestionsPublisher(term));
  }

  /**
   * Identifies a tape/disk image by its MD5 (32 chars) or SHA512 (128 chars) hash.
   * Returns null when ZXInfo knows no entry for it.
   */
  public FileCheckResult identifyFile(String hash) {
    try {
      return withClient(zxClient -> zxClient.getFileByHash(hash));
    } catch (jakarta.ws.rs.NotFoundException e) {
      return null;
    }
  }

  /**
   * Identifies a local image file by hashing it. Note ZXInfo hashes the image itself
   * (the .tap/.tzx/.z80), not the .zip it is distributed in, so unzip before calling.
   */
  public FileCheckResult identifyFile(java.io.File file) {
    try {
      return identifyFile(md5Of(file));
    } catch (Exception e) {
      System.err.println("Error hashing " + file + ": " + e.getMessage());
      return null;
    }
  }

  private static String md5Of(java.io.File file) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
    try (java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    StringBuilder hash = new StringBuilder();
    for (byte b : digest.digest()) {
      hash.append(String.format("%02x", b));
    }
    return hash.toString();
  }

  /** Runs a call against a freshly built proxy and always closes the client. */
  private <T> T withClient(java.util.function.Function<ZxInfoClient, T> call) {
    Client client = ClientBuilder.newClient();
    try {
      ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
      return call.apply(target.proxy(ZxInfoClient.class));
    } finally {
      client.close();
    }
  }

  /** ZXInfo's own label for map downloads inside additionalDownloads. */
  public static final String GAME_MAP_TYPE = "Game map";

  /**
   * Picks the "Game map" entries out of additionalDownloads. These are scanned or fan-drawn
   * map images (JPG/PNG), not structured map data - ZXInfo exposes nothing else for maps.
   */
  public static List<AdditionalDownload> extractGameMaps(List<AdditionalDownload> downloads) {
    List<AdditionalDownload> maps = new java.util.ArrayList<>();
    if (downloads != null) {
      for (AdditionalDownload download : downloads) {
        if (download != null && GAME_MAP_TYPE.equalsIgnoreCase(download.type)) {
          maps.add(download);
        }
      }
    }
    return maps;
  }

  private static String firstPublisherName(List<Publisher> publishers) {
    if (publishers != null) {
      for (Publisher publisher : publishers) {
        if (publisher != null && publisher.name != null) {
          return publisher.name;
        }
      }
    }
    return "N/A";
  }

  private static String joinTitles(List<String> titles, String fallback) {
    if (titles == null || titles.isEmpty()) {
      return fallback != null ? fallback : "N/A";
    }
    return String.join(" / ", titles);
  }

  private static String formatPrice(GameEntry.Price price) {
    if (price == null || price.amount == null) {
      return "";
    }
    String currency = price.currency != null ? price.currency : "";
    return (price.prefix != null && price.prefix == 1) ? currency + price.amount : price.amount + currency;
  }
}
