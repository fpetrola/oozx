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
import com.fpetrola.oozx.api.GameEntry;
import com.fpetrola.oozx.api.Hit;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.speccy.pokes.PokFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Turns the thousands of .pok files into one file of pokes keyed by the game they are for.
 * <p>
 * Today a poke is found by the name of the file the game was loaded from, so a game called
 * RENE256.SNA has no pokes and two games that share a title share each other's. The id ZXInfo uses
 * is what fixes that, and it does not have to be guessed: ZXDB is where these files come from, it
 * lists a "POK pokes file" against the entry it belongs to, and the file is named after the entry's
 * title, year and publisher.
 * <p>
 * The walk of the whole catalogue is the long part, so it is written down slice by slice as it
 * goes: run this again after it dies and it asks only for what is missing. Matching and writing
 * cost nothing and are done from scratch every time.
 */
public class PokesBuilder {
  private static final ObjectMapper JSON = new ObjectMapper();

  /** What ZXInfo knows about one game, which is all that is needed to recognise a .pok by name. */
  public record Entry(String id, String title, Integer year, String publisher) {
  }

  /** The walk so far: which slices are done, and what they turned up. */
  public record Walk(Set<String> done, Map<String, Entry> entries) {
    public Walk() {
      this(java.util.concurrent.ConcurrentHashMap.newKeySet(), new java.util.concurrent.ConcurrentHashMap<>());
    }

    /** What was read back from disk arrives in plain collections, and the walk writes from several threads. */
    public Walk copy() {
      Walk shared = new Walk();
      shared.done().addAll(done());
      shared.entries().putAll(entries());
      return shared;
    }
  }

  /** One game's pokes, with the entry they were matched to when there was one. */
  public record Pokes(String id, String title, Integer year, String publisher, String file,
                      List<Poke> pokes) {
  }

  public record Poke(String name, List<String> lines) {
  }

  /**
   * Args: the folder of .pok files, and where to keep the walk and write the result.
   * <p>
   * The .pok files are not in this repository any more - they are what this produced pokes.json
   * from. They come from ZXDB, and the ones that were here are in the commit that replaced them.
   */
  public static void main(String[] args) throws Exception {
    Path pokes = Path.of(args[0]);
    Path into = Path.of(args.length > 1 ? args[1] : "pokes-build");
    Files.createDirectories(into);

    Walk walk = walkTheCatalogue(into.resolve("zxdb-index.json"));
    List<Pokes> matched = match(pokes, walk.entries());
    JSON.writeValue(into.resolve("pokes.json").toFile(), matched);

    long named = matched.stream().filter(one -> one.id() != null).count();
    System.out.printf("pokes: %d juegos, %d con id (%.0f%%), %d sin resolver%n",
        matched.size(), named, 100.0 * named / matched.size(), matched.size() - named);
    Files.write(into.resolve("sin-id.txt"), matched.stream()
        .filter(one -> one.id() == null).map(Pokes::file).sorted().toList());
  }

  /**
   * The whole catalogue as id, title, year and publisher, kept in a file as it is read. A slice
   * already in it is not asked for again, so this can be stopped and started at will.
   */
  private static Walk walkTheCatalogue(Path kept) throws IOException {
    Walk walk = (Files.exists(kept) ? JSON.readValue(kept.toFile(), Walk.class) : new Walk()).copy();
    System.out.println("recorrido hasta ahora: " + walk.done().size() + " pedazos, "
        + walk.entries().size() + " juegos");
    int[] sinceSaved = {0};
    new ZxInfoApiHandler().walk(slice -> walk.done().contains(slice.key()), (slice, found) -> {
      for (Hit hit : found) {
        GameEntry entry = hit.getSource();
        walk.entries().put(hit.getId(), new Entry(hit.getId(), entry.title, entry.originalYearOfRelease,
            entry.publishers == null || entry.publishers.isEmpty() ? null : entry.publishers.get(0).name));
      }
      walk.done().add(slice.key());
      // Written from several threads at once, and the file is the thing that must not be half a
      // walk, so the saving is what gets serialised rather than the reading.
      synchronized (PokesBuilder.class) {
        if (++sinceSaved[0] % 10 == 0) {
          save(kept, walk);
          System.out.println("  " + slice.key() + " -> " + walk.entries().size() + " juegos");
        }
      }
    });
    save(kept, walk);
    System.out.println("recorrido completo: " + walk.entries().size() + " juegos del catalogo");
    return walk;
  }

  private static void save(Path kept, Walk walk) {
    try {
      JSON.writeValue(kept.toFile(), walk);
    } catch (IOException notWritten) {
      System.err.println("no se pudo guardar el recorrido: " + notWritten.getMessage());
    }
  }

  /**
   * Each .pok file against the catalogue, by the title in its name. The year and the publisher are
   * in the name too and are what decides between games that share a title; with neither agreeing,
   * a single candidate is taken and several candidates are left unresolved rather than guessed.
   */
  private static List<Pokes> match(Path folder, Map<String, Entry> entries) throws IOException {
    Map<String, List<Entry>> byTitle = new LinkedHashMap<>();
    entries.values().forEach(entry ->
        byTitle.computeIfAbsent(plainly(entry.title()), title -> new ArrayList<>()).add(entry));

    List<Pokes> all = new ArrayList<>();
    try (Stream<Path> files = Files.walk(folder)) {
      for (Path file : files.filter(one -> one.toString().toLowerCase().endsWith(".pok")).sorted().toList()) {
        String name = file.getFileName().toString().replaceFirst("(?i)\\.pok$", "");
        Entry entry = bestOf(byTitle.getOrDefault(plainly(titleIn(name)), List.of()), yearIn(name), publisherIn(name));
        all.add(new Pokes(entry == null ? null : entry.id(),
            entry == null ? titleIn(name) : entry.title(),
            entry == null ? yearIn(name) : entry.year(),
            entry == null ? publisherIn(name) : entry.publisher(),
            name, pokesIn(file, name)));
      }
    }
    return all;
  }

  private static Entry bestOf(List<Entry> candidates, Integer year, String publisher) {
    if (candidates.isEmpty()) {
      return null;
    }
    List<Entry> sameYear = candidates.stream()
        .filter(entry -> year != null && year.equals(entry.year())).toList();
    List<Entry> narrowed = sameYear.isEmpty() ? candidates : sameYear;
    if (narrowed.size() > 1 && publisher != null) {
      List<Entry> samePublisher = narrowed.stream().filter(entry -> entry.publisher() != null
          && plainly(entry.publisher()).startsWith(plainly(publisher))).toList();
      if (!samePublisher.isEmpty()) {
        narrowed = samePublisher;
      }
    }
    // Several left means the name does not say which, and a poke on the wrong game is worse than
    // a poke that has to be looked up by name as before.
    return narrowed.size() == 1 ? narrowed.get(0) : null;
  }

  private static List<Poke> pokesIn(Path file, String name) throws IOException {
    PokFile parsed = new PokFile(name, file);
    parsed.parseContent();
    Map<String, List<String>> byName = new LinkedHashMap<>();
    // One line at a time is how they are parsed, and a poke of several lines is several of them
    // under the one name; here they go back together in the order they were written.
    parsed.getMods().forEach(mod -> byName.computeIfAbsent(mod.getName(), poke -> new ArrayList<>())
        .add(mod.getRawInstruction()));
    return byName.entrySet().stream().map(poke -> new Poke(poke.getKey(), poke.getValue()))
        .sorted(Comparator.comparing(Poke::name)).toList();
  }

  /** "Cybernoid II - The Revenge (1988)(Hewson Consultants)" without what is in brackets. */
  private static String titleIn(String name) {
    int bracket = name.indexOf('(');
    return (bracket > 0 ? name.substring(0, bracket) : name).trim();
  }

  private static Integer yearIn(String name) {
    java.util.regex.Matcher year = java.util.regex.Pattern.compile("\\((\\d{4})\\)").matcher(name);
    return year.find() ? Integer.valueOf(year.group(1)) : null;
  }

  private static String publisherIn(String name) {
    java.util.regex.Matcher fields = java.util.regex.Pattern.compile("\\(([^)]*)\\)").matcher(name);
    String last = null;
    while (fields.find()) {
      if (!fields.group(1).matches("\\d{4}")) {
        last = fields.group(1);
      }
    }
    return last;
  }

  /**
   * A title as the eye reads it, so that two spellings of one name meet. Case and punctuation are
   * the easy half: ZXDB writes "Cybernoid II The Revenge" where the file says "Cybernoid II - The
   * Revenge". The other half is the article, which these file names move to the end - "Abadia del
   * Crimen, La" is ZXDB's "La Abadia del Crimen" - and which is not always English: leaving the
   * Spanish and French ones out of the list left thirty-six games unmatched that this finds.
   */
  private static final String ARTICLE = "(the|a|la|el|los|las|le|les|l|lo|il|der|die|das)";

  private static String plainly(String title) {
    String plain = title.toLowerCase()
        .replaceAll(",\\s*" + ARTICLE + "\\b", "")
        .replaceAll("[‘’']", "")
        .replaceAll("[^a-z0-9]+", " ").trim();
    return plain.replaceAll("^" + ARTICLE + " ", "").replaceAll(" " + ARTICLE + "$", "");
  }
}
