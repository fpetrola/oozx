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

package com.fpetrola.oozx.rzx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The RZX Archive's 4291 recordings, indexed by the game they are of.
 * <p>
 * The archive has no API and stopped being updated, so it ships as data rather than being
 * queried; {@code tools/scrape_rzx_archive.py} is what produced it. Each recording carries the
 * game's Spectrum Computing id, which is the id the ZXInfo API uses too — so a search result
 * the browser already has can be handed straight to {@link #recordingsFor(int)} with no name
 * matching.
 * <p>
 * Loaded on first use, not at startup: it is a megabyte and a half of JSON that a session which
 * never asks about recordings should not pay for.
 */
@Singleton
public class RzxArchive {

  private static final String RESOURCE = "/rzx/rzx-archive.json";

  private Map<Integer, List<RzxRecording>> byGame;

  /**
   * Every recording of a game, newest listing order preserved, or empty if the archive has
   * none. The eleven recordings the archive lists without an id are unreachable this way,
   * which is the trade for keying on the join.
   */
  public List<RzxRecording> recordingsFor(int spectrumComputingId) {
    return byGame().getOrDefault(spectrumComputingId, List.of());
  }

  public boolean hasRecordings(int spectrumComputingId) {
    return !recordingsFor(spectrumComputingId).isEmpty();
  }

  public synchronized Map<Integer, List<RzxRecording>> byGame() {
    if (byGame == null) byGame = load();
    return byGame;
  }

  private Map<Integer, List<RzxRecording>> load() {
    try (InputStream json = RzxArchive.class.getResourceAsStream(RESOURCE)) {
      if (json == null) {
        // Missing catalogue is not worth failing an emulator over: no recordings is a fine
        // answer, and the browser will simply offer none.
        System.err.println("RZX catalogue not on the classpath at " + RESOURCE);
        return Map.of();
      }

      JsonNode entries = new ObjectMapper().readTree(json).path("entries");
      Map<Integer, List<RzxRecording>> index = new HashMap<>();
      for (JsonNode entry : entries) {
        RzxRecording recording = read(entry);
        if (recording.spectrumComputingId() == null) continue;
        index.computeIfAbsent(recording.spectrumComputingId(), k -> new ArrayList<>())
            .add(recording);
      }
      index.replaceAll((game, list) -> Collections.unmodifiableList(list));
      return Collections.unmodifiableMap(index);
    } catch (Exception e) {
      throw new IllegalStateException("cannot read the RZX catalogue at " + RESOURCE, e);
    }
  }

  private RzxRecording read(JsonNode entry) {
    List<RzxRecording.RzxDownload> downloads = new ArrayList<>();
    for (JsonNode download : entry.path("downloads")) {
      downloads.add(new RzxRecording.RzxDownload(download.path("url").asText(),
          download.path("sizeKb").asInt()));
    }

    return new RzxRecording(
        text(entry, "id"),
        text(entry, "title"),
        text(entry, "submitter"),
        text(entry, "note"),
        entry.hasNonNull("spectrumComputingId") ? entry.get("spectrumComputingId").asInt() : null,
        text(entry, "sourceUrl"),
        text(entry, "worldOfSpectrumUrl"),
        text(entry, "videoUrl"),
        Collections.unmodifiableList(downloads),
        entry.path("distributionDenied").asBoolean(false));
  }

  /** The catalogue leaves absent fields out rather than writing nulls, to stay small. */
  private String text(JsonNode entry, String field) {
    return entry.hasNonNull(field) ? entry.get(field).asText() : null;
  }
}
