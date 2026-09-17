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

// src/main/java/com/example/Main.java
package com.fpetrola.oozx.api;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ZxInfoApiHandler {
  private static final int FIRST_YEAR = 1980, MOST_AT_ONCE = 1000, RETRIES = 5, AT_ONCE = 4;
  private static final long BACKOFF = 2000, BETWEEN_PAGES = 150;
  private static final String TOSEC = "TOSEC", TOSEC_SET = "/download/zx_spectrum_tosec_set_september_2023/";

  private Metadata metadata;

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
    try {
      return convertGameEntryToDetail(game(gameId), gameId);
    } catch (Exception e) {
      System.err.println("Error fetching game details: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /** The whole entry, which is where the files it can be downloaded from are listed. */
  public GameEntry game(String gameId) {
    return withClient(zxClient -> zxClient.getGameDetails(gameId, ZxInfoClient.MODE_FULL)).getGameEntry();
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
        Screen screen = Screen.from(screenMap);
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
   * The games most people voted for at World of Spectrum, which is the closest thing the catalogue
   * has to "games somebody has heard of": a demo nobody saw has no votes at all.
   * <p>
   * The scan goes year by year because the search cannot sort by votes - its sort=score_desc
   * actually orders by id - and because paging past ten thousand results is refused, which no
   * single year reaches.
   */
  public List<GameSummary> mostVoted(int howMany) {
    List<Hit> voted = new ArrayList<>();
    walk(slice -> false, (slice, found) -> found.stream().filter(hit -> votesOf(hit) > 0).forEach(voted::add));
    return voted.stream()
        .sorted(Comparator.comparingInt(ZxInfoApiHandler::votesOf).reversed())
        .limit(howMany)
        .map(ZxInfoApiHandler::summaryOf)
        .toList();
  }

  /** One narrowing of the catalogue: a year, and the genre and machine it had to be cut down by. */
  public record Slice(int year, String genre, String machine) {
    public String key() {
      return year + "|" + (genre == null ? "" : genre) + "|" + (machine == null ? "" : machine);
    }
  }

  /**
   * Every entry of the catalogue, handed over a slice at a time. The slice comes with them so that
   * a walk of forty thousand entries can be written down as it goes and taken up where it stopped:
   * {@code skip} is asked before each one, and a slice already done costs nothing.
   * <p>
   * Several slices are read at once, so {@code skip} and {@code found} are called from more than
   * one thread and whatever they write to has to expect that.
   */
  public void walk(java.util.function.Predicate<Slice> skip,
      java.util.function.BiConsumer<Slice, List<Hit>> found) {
    // A handful of years at a time: each request takes a second or two and there are hundreds of
    // slices, so the walk is nearly all waiting. Not many more than a handful, because this API
    // answers 503 when it is asked too fast and then the retries eat what the threads won.
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(AT_ONCE);
    try {
      List<java.util.concurrent.Future<?>> years = new ArrayList<>();
      for (int year = FIRST_YEAR; year <= java.time.Year.now().getValue(); year++) {
        int reading = year;
        years.add(pool.submit(() -> collect(new Slice(reading, null, null), skip, found)));
      }
      for (java.util.concurrent.Future<?> year : years) {
        year.get();
      }
    } catch (InterruptedException stopped) {
      Thread.currentThread().interrupt();
    } catch (java.util.concurrent.ExecutionException failed) {
      throw new IllegalStateException("the catalogue could not be read: " + failed.getCause(), failed.getCause());
    } finally {
      pool.shutdownNow();
    }
  }

  /**
   * Reads a slice of the catalogue, splitting it until it fits in one request.
   * <p>
   * There is no paging to lean on: the numeric offset answers 503 past a few hundred, and the
   * cursor - the sort values of the last hit - cycles between two pages instead of advancing, so
   * a walk with it reads the same two hundred entries forever. What does work is asking for a
   * thousand at once, so the slice is narrowed by year, then by genre, then by machine until the
   * count comes back under that.
   */
  private void collect(Slice slice, java.util.function.Predicate<Slice> skip,
      java.util.function.BiConsumer<Slice, List<Hit>> found) {
    if (skip.test(slice)) {
      return;
    }
    SearchResponse response = withClient(zxClient -> zxClient.searchGames("*", MOST_AT_ONCE, "0",
        ZxInfoClient.MODE_COMPACT, null, null, "SOFTWARE", slice.year(), null, slice.genre(), null,
        slice.machine(), null, null, null, null, null, null, null, null));
    if (response.hits.total.value > MOST_AT_ONCE) {
      // A slice that does not fit is not done: it is cut finer and its pieces are what get written
      // down, so taking the walk up again re-asks only this one question.
      for (String narrower : slice.genre() == null
          ? valuesOf(metadata().genretypes) : valuesOf(metadata().machinetypes)) {
        collect(slice.genre() == null ? new Slice(slice.year(), narrower, null)
            : new Slice(slice.year(), slice.genre(), narrower), skip, found);
      }
      return;
    }
    found.accept(slice, response.hits.hits);
    sleep(BETWEEN_PAGES);
  }

  private static List<String> valuesOf(Metadata.Facet facet) {
    return facet.values.stream().map(Metadata.Value::name).toList();
  }

  private synchronized Metadata metadata() {
    if (metadata == null) {
      metadata = getMetadata();
    }
    return metadata;
  }

  private static int votesOf(Hit hit) {
    GameEntry entry = hit.getSource();
    return entry.score == null || entry.score.votes == null ? 0 : entry.score.votes;
  }

  /** Where ZXDB's own paths are actually served from, which is three different hosts. */
  public static String mediaUrl(String path) {
    if (path.startsWith("/zxscreens")) {
      return "https://zxinfo.dk/media" + path;
    }
    return path.startsWith("/zxdb") ? "https://spectrumcomputing.co.uk" + path : "https://worldofspectrum.net" + path;
  }

  /** Whether ZXDB keeps this one where it may not hand it out, which is what /denied/ means. */
  public static boolean denied(String path) {
    return path != null && path.toLowerCase().contains("/denied/");
  }

  /**
   * Where a TOSEC path is served from: archive.org keeps the set as one zip per category and hands
   * out a single file from inside it. The path has to be the entry's own - TOSEC writes "(Ocean)"
   * where ZXDB writes "Ocean Software", so a name built from the entry's fields does not exist.
   */
  public static String tosecUrl(String path) {
    try {
      return new java.net.URI("https", "archive.org",
          TOSEC_SET + path.substring(1, path.indexOf('/', 1)) + ".zip" + path, null).toASCIIString();
    } catch (java.net.URISyntaxException notAPath) {
      throw new IllegalArgumentException(path, notAPath);
    }
  }

   /**
   * Every file the entry offers, as URLs, paired with the format each one is in. What it may not
   * hand out ZXDB lists anyway, under /denied/, and the TOSEC set stands in for that: for Knight
   * Lore or Dizzy Collection it is the only place left, and the entry names its TOSEC files whether
   * or not it is allowed to serve its own. It takes one withheld file and not all of them: Enduro
   * Racer lists two magazine scans it may serve beside the tape it may not.
   */
  public static java.util.Map<String, String> filesOf(GameEntry game) {
    java.util.Map<String, String> formatByUrl = new java.util.LinkedHashMap<>();
    game.releases.forEach(release -> release.files.stream()
        .filter(file -> file.format != null)
        .forEach(file -> formatByUrl.put(mediaUrl(file.path), file.format)));
    if (withheld(formatByUrl) && game.tosec != null) {
      game.tosec.forEach(file -> formatByUrl.put(tosecUrl(file.path), TOSEC));
    }
    return formatByUrl;
  }

  /** Whether this is one of the TOSEC files, which is not ZXDB's own copy but one found for it. */
  public static boolean fromTosec(String url) {
    return url != null && url.contains(TOSEC_SET);
  }

  /** Whether ZXDB is keeping any of what it lists for itself, which is what TOSEC stands in for. */
  private static boolean withheld(java.util.Map<String, String> files) {
    return files.keySet().stream().anyMatch(ZxInfoApiHandler::denied);
  }

  /**
   * The entry with what a compact search leaves out. A search answers without the TOSEC paths, so
   * an entry ZXDB withholds arrives looking like one with nothing to download at all, and stays
   * that way until the whole entry is asked for. Only those are asked for: one call per hit would
   * be 150 of them for a single search, and for "r-type" exactly one of the 138 needs it.
   */
  public GameEntry withTosecFiles(String id, GameEntry game) {
    if (game.tosec != null || !withheld(filesOf(game))) {
      return game;
    }
    try {
      return game(id);
    } catch (RuntimeException outOfReach) {
      return game;
    }
  }

  public static GameSummary summaryOf(Hit hit) {
    GameEntry entry = hit.getSource();
    GameSummary summary = new GameSummary();
    summary.id = hit.getId();
    summary.title = entry.title;
    summary.yearOfRelease = String.valueOf(entry.originalYearOfRelease);
    summary.publisher = entry.publishers == null || entry.publishers.isEmpty() ? null : entry.publishers.get(0).name;
    return summary;
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
  /**
   * A 503 from this API means asked too fast, not gone: a few hundred requests in a row bring it
   * on, and the same call answers when it is given a moment. Anything else is passed straight out.
   */
  private <T> T withClient(java.util.function.Function<ZxInfoClient, T> call) {
    for (int attempt = 1; ; attempt++) {
      Client client = ClientBuilder.newClient();
      try {
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
        return call.apply(target.proxy(ZxInfoClient.class));
      } catch (jakarta.ws.rs.ServiceUnavailableException tooFast) {
        if (attempt == RETRIES) {
          throw tooFast;
        }
        sleep(attempt * BACKOFF);
      } finally {
        client.close();
      }
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
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
