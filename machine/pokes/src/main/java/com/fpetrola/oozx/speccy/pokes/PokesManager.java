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

package com.fpetrola.oozx.speccy.pokes;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Every poke there is, keyed by the game it is for.
 * <p>
 * They used to be 3683 .pok files under a folder of one letter each, indexed by the text before
 * the first bracket of the file's name. That name is what a poke was found by, so a game loaded
 * from RENE256.SNA had none, and two games that happen to share a title shared each other's -
 * 3683 files went in and 3612 names came out, so seventy-odd were quietly eaten. They are now one
 * file, and each game carries the id ZXInfo uses, which is what the emulator asks by.
 */
public class PokesManager {
  private static final String POKES = "/pokes.json";

  private final Map<String, List<PokFile>> byName = new ConcurrentHashMap<>();
  private final Map<String, List<PokFile>> byEntry = new ConcurrentHashMap<>();
  private boolean initialized = false;

  /** One game's pokes as they are written down, with the entry they were matched to. */
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record Pokes(String id, String title, Integer year, String publisher, String file,
                      List<Poke> pokes) {
  }

  public record Poke(String name, List<String> lines) {
  }

  public PokesManager() {
    initializePokes();
  }

  public void initializePokes() {
    if (initialized) return;

    new Thread(() -> {
      try (java.io.InputStream json = PokesManager.class.getResourceAsStream(POKES)) {
        if (json == null) {
          System.err.println("Pokes resource not found: " + POKES);
          return;
        }
        for (Pokes game : new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(json, Pokes[].class)) {
          PokFile pokes = new PokFile(game.file(), game.pokes().stream()
              .flatMap(poke -> poke.lines().stream()
                  .map(line -> new PokFile.PokeMod(poke.name(), line, game.file(), game.title())))
              .toList());
          byName.computeIfAbsent(game.title().toLowerCase(), title -> new ArrayList<>()).add(pokes);
          if (game.id() != null) {
            byEntry.computeIfAbsent(game.id(), id -> new ArrayList<>()).add(pokes);
          }
        }
        initialized = true;
        System.out.println("Pokes initialized: " + byName.size() + " games, " + byEntry.size()
            + " of them by id");
      } catch (Exception e) {
        System.err.println("Error initializing pokes: " + e.getMessage());
      }
    }).start();
  }

  /**
   * The pokes for a game the catalogue recognised. Nothing is guessed here: the id was matched to
   * the .pok file when this file was built, against ZXDB, which is where both come from.
   */
  public List<PokFile> findPokesForEntry(String entryId) {
    return entryId == null ? List.of() : byEntry.getOrDefault(entryId, List.of());
  }

  public List<PokFile> findPokesForGame(String gameName) {
    if (!initialized) {
      System.out.println("Pokes not yet initialized");
      return new ArrayList<>();
    }

    String searchName = gameName.toLowerCase();

    if (byName.containsKey(searchName)) {
      return byName.get(searchName);
    }

    List<PokFile> results = new ArrayList<>();
    for (Map.Entry<String, List<PokFile>> entry : byName.entrySet()) {
      if (isSimilar(searchName, entry.getKey())) {
        results.addAll(entry.getValue());
      }
    }
    
    return results;
  }
  
  public List<PokFile> searchPokFilesByName(String searchTerm) {
    if (!initialized) {
      System.out.println("Pokes not yet initialized");
      return new ArrayList<>();
    }

    List<PokFile> results = new ArrayList<>();
    String lowerSearch = searchTerm.toLowerCase();

    for (Map.Entry<String, List<PokFile>> gameEntry : byName.entrySet()) {
      for (PokFile pokFile : gameEntry.getValue()) {
        if (pokFile.getName().toLowerCase().contains(lowerSearch) ||
            gameEntry.getKey().toLowerCase().contains(lowerSearch) ||
            isSimilar(lowerSearch, pokFile.getName().toLowerCase())) {
          results.add(pokFile);
        }
      }
    }
    
    return results;
  }

  private boolean isSimilar(String name1, String name2) {
    String clean1 = name1.replaceAll("[^a-z0-9]", "").toLowerCase();
    String clean2 = name2.replaceAll("[^a-z0-9]", "").toLowerCase();

    if (clean1.contains(clean2) || clean2.contains(clean1)) {
      return true;
    }

    int distance = levenshteinDistance(clean1, clean2);
    int maxLen = Math.max(clean1.length(), clean2.length());
    double similarity = 1.0 - ((double) distance / maxLen);

    return similarity > 0.7;
  }

  private int levenshteinDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    
    for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
    for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
    
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), 
                           dp[i - 1][j - 1] + cost);
      }
    }
    
    return dp[a.length()][b.length()];
  }

  public Set<String> getAllGameNames() {
    return byName.keySet();
  }

  public int getTotalGamesIndexed() {
    return byName.size();
  }

  public boolean isInitialized() {
    return initialized;
  }

  public static String getPokesDirectory() {
    return POKES;
  }
}
