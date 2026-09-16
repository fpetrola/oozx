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
package com.fpetrola.oozx.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * What is on this machine, said in the same terms as what is on the net: each file that can be
 * loaded, and which game of the catalogue it turned out to be.
 * <p>
 * Identifying it once and writing it down is the point. A file called RENE256.SNA is Renegade for
 * anything that asks afterwards - its pokes, its details, its map - instead of being a name that
 * matched nothing. What could not be identified stays in the library as unknown rather than being
 * dropped, because it is still a game somebody has.
 * <p>
 * Where the library is kept, which files count, and how to get the game's bytes out of one are the
 * caller's to decide: this end is handed them, so that the list of formats the emulator knows and
 * the readers that unpack a snapshot stay where they already live.
 */
public class GameLibrary {
  private static final ObjectMapper JSON = new ObjectMapper();

  private final Map<String, Copy> byPath = new LinkedHashMap<>();
  private final GameFingerprint.Index catalogue;
  private final Predicate<Path> loadable;
  private final Payload payload;

  /** The bytes of the game inside a file, which for a snapshot is not the file itself. */
  public interface Payload {
    byte[] of(Path file) throws IOException;
  }

  public GameLibrary(GameFingerprint.Index catalogue, Predicate<Path> loadable, Payload payload) {
    this.catalogue = catalogue;
    this.loadable = loadable;
    this.payload = payload;
  }

  /** One game as it sits on this machine. A null game is one the catalogue did not recognise. */
  public record Copy(String path, long size, long modified, GameSummary game, double score) {
    public boolean identified() {
      return game != null;
    }

    public String title() {
      return identified() ? game.title : Path.of(path).getFileName().toString();
    }

    @Override
    public String toString() {
      return identified() ? game.toString() : title() + " (unknown)";
    }
  }

  /**
   * Walks a directory and identifies what is new in it. A file already in the library with the same
   * size and date is left alone, so a rescan costs a listing rather than a fingerprint of every
   * game. Returns how many files were looked at.
   */
  public int scan(Path directory, double certainty) throws IOException {
    List<Path> files;
    try (Stream<Path> walk = Files.walk(directory)) {
      files = walk.filter(Files::isRegularFile).filter(loadable).toList();
    }
    for (Path file : files) {
      identify(file, certainty);
    }
    return files.size();
  }

  /**
   * Who one file is, worked out once and written down. A file already in the library with the same
   * size and date is answered from what was written, which is what makes asking cheap enough to do
   * every time a game is loaded.
   */
  public Copy identify(Path file, double certainty) throws IOException {
    long size = Files.size(file);
    long modified = Files.getLastModifiedTime(file).toMillis();
    Copy known = byPath.get(file.toString());
    if (known != null && known.size() == size && known.modified() == modified) {
      return known;
    }
    GameFingerprint.Match match = catalogue.identify(payload.of(file));
    boolean sure = match != null && match.score() >= certainty;
    Copy copy = new Copy(file.toString(), size, modified,
        sure ? match.game() : null, match == null ? 0 : match.score());
    byPath.put(file.toString(), copy);
    return copy;
  }

  public List<Copy> games() {
    return byPath.values().stream().sorted(Comparator.comparing(Copy::title, String.CASE_INSENSITIVE_ORDER)).toList();
  }

  public Copy of(Path file) {
    return byPath.get(file.toString());
  }

  public List<Copy> unknown() {
    return byPath.values().stream().filter(copy -> !copy.identified()).toList();
  }

  public void forget(Path file) {
    byPath.remove(file.toString());
  }

  /** Drops what is no longer on disk, which is what makes a moved or deleted game disappear. */
  public int forgetMissing() {
    List<String> gone = new ArrayList<>(byPath.keySet()).stream()
        .filter(path -> !Files.exists(Path.of(path))).toList();
    gone.forEach(byPath::remove);
    return gone.size();
  }

  public void save(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    JSON.writeValue(file.toFile(), byPath.values());
  }

  public void load(Path file) throws IOException {
    if (!Files.exists(file)) {
      return;
    }
    for (Copy copy : JSON.readValue(file.toFile(), Copy[].class)) {
      byPath.put(copy.path(), copy);
    }
  }
}
