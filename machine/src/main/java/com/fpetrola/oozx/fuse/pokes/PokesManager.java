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

package com.fpetrola.oozx.fuse.pokes;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Gestiona la indexación y búsqueda de archivos .pok desde resources
 */
public class PokesManager {
  private static final String POKES_RESOURCE_PATH = "/pokes";
  
  private Map<String, List<PokFile>> pokIndex = new ConcurrentHashMap<>();
  private boolean initialized = false;

  public PokesManager() {
    initializePokes();
  }

  /**
   * Inicializa el gestor de pokes desde resources
   */
  public void initializePokes() {
    if (initialized) return;
    
    new Thread(() -> {
      try {
        indexPokesFromResources();
        initialized = true;
        System.out.println("Pokes initialized: " + pokIndex.size() + " games with pokes");
      } catch (Exception e) {
        System.err.println("Error initializing pokes: " + e.getMessage());
        e.printStackTrace();
      }
    }).start();
  }

  /**
   * Indexa los pokes desde los recursos embebidos en la aplicación
   */
  private void indexPokesFromResources() throws Exception {
    System.out.println("Loading pokes from resources...");
    
    // Obtener la URL del recurso de pokes
    URL pokesUrl = getClass().getResource(POKES_RESOURCE_PATH);
    if (pokesUrl == null) {
      System.err.println("Pokes resource not found: " + POKES_RESOURCE_PATH);
      return;
    }
    
    // Convertir URL a Path para poder iterar directorios
    Path pokesPath = null;
    if (pokesUrl.getProtocol().equals("file")) {
      pokesPath = Paths.get(pokesUrl.toURI());
    } else if (pokesUrl.getProtocol().equals("jar")) {
      // Si está en un JAR, crear un FileSystem temporal
      String[] parts = pokesUrl.getPath().split("!");
      Path jarPath = Paths.get(parts[0].substring(5)); // Quitar "file:"
      pokesPath = Paths.get(parts[1]);
    }
    
    if (pokesPath != null && Files.exists(pokesPath)) {
      indexPokesDirectory(pokesPath);
    } else {
      System.err.println("Could not access pokes directory");
    }
  }

  /**
   * Indexa el directorio de pokes
   */
  private void indexPokesDirectory(Path pokesPath) throws Exception {
    try (DirectoryStream<Path> directories = Files.newDirectoryStream(pokesPath)) {
      for (Path dir : directories) {
        if (Files.isDirectory(dir) && !dir.getFileName().toString().startsWith(".")) {
          indexDirectory(dir);
        }
      }
    }
  }



  /**
   * Indexa una carpeta de pokes (organizada por letra)
   */
  private void indexDirectory(Path directory) throws Exception {
    try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.pok")) {
      for (Path pokPath : files) {
        try {
          String pokFileName = pokPath.getFileName().toString();
          String pokName = pokFileName.replace(".pok", "");
          
          PokFile pokFile = new PokFile(pokName, pokPath);
          pokFile.parseContent();
          
          // Extraer nombre del juego (antes del primer paréntesis)
          String gameName = extractGameName(pokName);
          
          pokIndex.computeIfAbsent(gameName.toLowerCase(), k -> new ArrayList<>()).add(pokFile);
        } catch (Exception e) {
          System.err.println("Error parsing pok file: " + pokPath + " - " + e.getMessage());
        }
      }
    }
  }

  /**
   * Extrae el nombre del juego del nombre del archivo .pok
   */
  private String extractGameName(String pokFileName) {
    int parenIndex = pokFileName.indexOf('(');
    if (parenIndex > 0) {
      return pokFileName.substring(0, parenIndex).trim();
    }
    return pokFileName;
  }

  /**
   * Busca pokes para un juego específico usando búsqueda fuzzy
   */
  public List<PokFile> findPokesForGame(String gameName) {
    if (!initialized) {
      System.out.println("Pokes not yet initialized");
      return new ArrayList<>();
    }

    String searchName = gameName.toLowerCase();
    
    // Búsqueda exacta
    if (pokIndex.containsKey(searchName)) {
      return pokIndex.get(searchName);
    }
    
    // Búsqueda fuzzy: buscar coincidencias parciales
    List<PokFile> results = new ArrayList<>();
    for (Map.Entry<String, List<PokFile>> entry : pokIndex.entrySet()) {
      if (isSimilar(searchName, entry.getKey())) {
        results.addAll(entry.getValue());
      }
    }
    
    return results;
  }
  
  /**
   * Busca archivos .pok por nombre similar (para búsquedas por patrón de nombre)
   */
  public List<PokFile> searchPokFilesByName(String searchTerm) {
    if (!initialized) {
      System.out.println("Pokes not yet initialized");
      return new ArrayList<>();
    }
    
    List<PokFile> results = new ArrayList<>();
    String lowerSearch = searchTerm.toLowerCase();
    
    // Buscar en todos los pok files indexados
    for (Map.Entry<String, List<PokFile>> gameEntry : pokIndex.entrySet()) {
      for (PokFile pokFile : gameEntry.getValue()) {
        // Comparar por nombre del archivo (sin extensión) y por nombre del juego
        if (pokFile.getName().toLowerCase().contains(lowerSearch) ||
            gameEntry.getKey().toLowerCase().contains(lowerSearch) ||
            isSimilar(lowerSearch, pokFile.getName().toLowerCase())) {
          results.add(pokFile);
        }
      }
    }
    
    return results;
  }

  /**
   * Compara dos nombres para encontrar similitud
   */
  private boolean isSimilar(String name1, String name2) {
    // Eliminar caracteres especiales y espacios
    String clean1 = name1.replaceAll("[^a-z0-9]", "").toLowerCase();
    String clean2 = name2.replaceAll("[^a-z0-9]", "").toLowerCase();
    
    // Si uno es substring del otro
    if (clean1.contains(clean2) || clean2.contains(clean1)) {
      return true;
    }
    
    // Calcular similitud Levenshtein
    int distance = levenshteinDistance(clean1, clean2);
    int maxLen = Math.max(clean1.length(), clean2.length());
    double similarity = 1.0 - ((double) distance / maxLen);
    
    return similarity > 0.7; // Al menos 70% similar
  }

  /**
   * Calcula la distancia de Levenshtein entre dos strings
   */
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

  /**
   * Obtiene todas las claves de juegos indexados
   */
  public Set<String> getAllGameNames() {
    return pokIndex.keySet();
  }

  /**
   * Obtiene todas las claves de juegos indexados
   */
  public int getTotalGamesIndexed() {
    return pokIndex.size();
  }

  public boolean isInitialized() {
    return initialized;
  }

  public static String getPokesDirectory() {
    return POKES_RESOURCE_PATH;
  }
}
