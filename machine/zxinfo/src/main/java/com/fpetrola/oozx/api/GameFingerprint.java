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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * What a game image is, reduced to a set of numbers, so that two copies of the same game can be
 * recognised as the same game without being the same file.
 * <p>
 * The image is cut where its own bytes say to cut, by a rolling hash over what has been read so
 * far, and each piece becomes one 64-bit hash. Cutting by content instead of by position is the
 * whole trick: a trainer, a different loader, a TZX carrying the blocks a TAP carries bare, or a
 * snapshot holding the same code at another address, all damage only the pieces they touch, and
 * the cuts fall back into step a few bytes later. Nothing here knows what a TZX or a Z80 is: it
 * takes bytes, so {@link com.fpetrola.oozx.api.GameFingerprint.Index} works the same for a tape,
 * a snapshot, or the region of memory somebody chose to hand it.
 * <p>
 * What it cannot see is memory that is in a snapshot without being the game. Two 128K snapshots
 * of unrelated games - Silk Worm and The Addams Family - were measured sharing a contiguous block
 * of 16257 identical bytes and several more besides, enough for one to claim the other at 0.55.
 * Whatever left that memory behind, it is not the game, and no amount of counting chunks can tell
 * the difference: only handing the fingerprint the game's own bytes can.
 * <p>
 * This is the fuzzy half of identifying a file. The exact half, a straight MD5 against ZXInfo's
 * catalogue, is {@link ZxInfoApiHandler#identifyFile}, needs the network, and answers nothing at
 * all when the copy is not byte-identical to a catalogued one.
 */
public record GameFingerprint(Set<Integer> chunks) {
  private static final int CUT_MASK = 0x7F, MIN_CHUNK = 32, MAX_CHUNK = 1024;
  /**
   * Only one chunk in four is kept, chosen by its own hash so that both copies of a game keep the
   * same ones. Sampling a fixed FRACTION rather than a fixed NUMBER of chunks is what keeps the
   * containment unbiased between images of very different sizes, which is the whole point of
   * comparing a 48K snapshot against a 200K tape.
   */
  private static final int SAMPLE_MASK = 0x3;
  private static final long[] GEAR = gear();

  public static GameFingerprint of(byte[] data) {
    Set<Integer> chunks = new HashSet<>();
    int start = 0;
    long rolling = 0;
    for (int i = 0; i < data.length; i++) {
      rolling = (rolling << 1) + GEAR[data[i] & 0xFF];
      int length = i - start + 1;
      if (length >= MIN_CHUNK && (length >= MAX_CHUNK || ((rolling >>> 40) & CUT_MASK) == 0)) {
        addChunk(chunks, data, start, length);
        start = i + 1;
        rolling = 0;
      }
    }
    addChunk(chunks, data, start, data.length - start);
    return new GameFingerprint(chunks);
  }

  public static GameFingerprint of(Path file) throws IOException {
    return of(Files.readAllBytes(file));
  }

  /** Fraction of the smaller of the two fingerprints that the other one also has. */
  public double containment(GameFingerprint other) {
    int smaller = Math.min(chunks.size(), other.chunks.size());
    return smaller == 0 ? 0 : (double) chunks.stream().filter(other.chunks::contains).count() / smaller;
  }

  /** A run of one repeated byte says nothing about which game it came from, and every game has them. */
  private static void addChunk(Set<Integer> chunks, byte[] data, int start, int length) {
    if (length < MIN_CHUNK || isUniform(data, start, length)) {
      return;
    }
    long hash = 0xCBF29CE484222325L;
    for (int i = start; i < start + length; i++) {
      hash = (hash ^ (data[i] & 0xFF)) * 0x100000001B3L;
    }
    if ((hash & SAMPLE_MASK) == 0) {
      chunks.add((int) (hash >>> 32));
    }
  }

  private static boolean isUniform(byte[] data, int start, int length) {
    for (int i = start + 1; i < start + length; i++) {
      if (data[i] != data[start]) {
        return false;
      }
    }
    return true;
  }

  private static long[] gear() {
    long[] table = new long[256];
    Random random = new Random(0x5ECC1L);
    Arrays.setAll(table, i -> random.nextLong());
    return table;
  }

  public record Match(GameSummary game, double score) {
  }

  /**
   * One game of the catalogue: what it is, what it looks like, what the entry carries besides the
   * game, and what its image fingerprints to.
   * <p>
   * The picture and the map are in here rather than being asked for when needed because the point
   * of a catalogue that ships is that it answers with no network. A wall of tiles used to cost one
   * request per game to find out what each one looked like.
   */
  public record Known(GameSummary game, String screenshot, boolean hasMap, GameFingerprint fingerprint) {
    public Known(GameSummary game, GameFingerprint fingerprint) {
      this(game, null, false, fingerprint);
    }
  }

  /**
   * The catalogue: the fingerprint of every known game, indexed by chunk, so that identifying an
   * image is counting votes instead of comparing against every entry. It carries the ZXInfo
   * summary of each game, so a match answers with the title, the year and the publisher, and its
   * id is the key to ask the API for everything else.
   */
  public static class Index {
    /** Above this share of the catalogue, a chunk is something everybody has, and it only adds noise. */
    private static final double TOO_COMMON = 0.1;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SHIPPED = "/catalogue.json";

    private final Map<Integer, List<String>> idsByChunk = new HashMap<>();
    private final Map<String, Known> byId = new LinkedHashMap<>();

    public void add(GameSummary game, GameFingerprint fingerprint) {
      add(new Known(game, fingerprint));
    }

    public void add(Known known) {
      byId.put(known.game().id, known);
      known.fingerprint().chunks()
          .forEach(chunk -> idsByChunk.computeIfAbsent(chunk, c -> new ArrayList<>()).add(known.game().id));
    }

    /**
     * What this catalogue is, as one number, so that a library can tell it was identified against
     * another one. Built from the games and the size of each fingerprint: what changes an answer
     * is which games are in here and what their images look like.
     */
    public int stamp() {
      return byId.entrySet().stream()
          .mapToInt(entry -> entry.getKey().hashCode() * 31 + entry.getValue().fingerprint().chunks().size())
          .sum();
    }

    /** Everything the catalogue holds about one game, or null if it holds none. */
    public Known known(String id) {
      return byId.get(id);
    }

    public List<Known> all() {
      return List.copyOf(byId.values());
    }

    /** Every file in the directory tree, each one standing for a game named after it. */
    public void addAll(Path directory) throws IOException {
      try (Stream<Path> files = Files.walk(directory)) {
        for (Path file : files.filter(Files::isRegularFile).toList()) {
          add(named(file.toString(), file.getFileName().toString().replaceFirst("\\.[^.]+$", "")), of(file));
        }
      }
    }

    public Match identify(byte[] data) {
      return identify(of(data));
    }

    /** The best match, or null when nothing in the catalogue shares a chunk with it. */
    public Match identify(GameFingerprint unknown) {
      Map<String, Integer> votes = new HashMap<>();
      for (int chunk : unknown.chunks()) {
        List<String> ids = idsByChunk.getOrDefault(chunk, List.of());
        if (ids.size() > 2 && ids.size() > byId.size() * TOO_COMMON) {
          continue;
        }
        ids.forEach(id -> votes.merge(id, 1, Integer::sum));
      }
      return votes.entrySet().stream()
          .map(vote -> new Match(byId.get(vote.getKey()).game(), (double) vote.getValue()
              / Math.min(unknown.chunks().size(), byId.get(vote.getKey()).fingerprint().chunks().size())))
          .max(Comparator.comparingDouble(Match::score))
          .orElse(null);
    }

    public boolean knows(String id) {
      return byId.containsKey(id);
    }

    public int size() {
      return byId.size();
    }

    public void save(Path file) throws IOException {
      JSON.writeValue(file.toFile(), byId.values());
    }

    public void load(Path file) throws IOException {
      try (InputStream json = Files.newInputStream(file)) {
        load(json);
      }
    }

    public void load(InputStream json) throws IOException {
      for (Known known : JSON.readValue(json, Known[].class)) {
        add(known);
      }
    }

    /** The catalogue that ships with the emulator, which is what identifies a game with no network. */
    public static Index shipped() {
      Index index = new Index();
      try (InputStream json = Index.class.getResourceAsStream(SHIPPED)) {
        if (json == null) {
          throw new IOException(SHIPPED + " is not on the classpath");
        }
        index.load(json);
      } catch (IOException notThere) {
        throw new IllegalStateException("the shipped catalogue could not be read", notThere);
      }
      return index;
    }
  }

  public static GameSummary named(String id, String title) {
    GameSummary game = new GameSummary();
    game.id = id;
    game.title = title;
    return game;
  }
}
