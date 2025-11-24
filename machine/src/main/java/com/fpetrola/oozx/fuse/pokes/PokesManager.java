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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Gestiona la descarga, indexación y búsqueda de archivos .pok
 */
public class PokesManager {
  private static final String POKES_REPO_URL = "https://github.com/ladyeklipse/all-tipshop-pokes.git";
  private static final String POKES_DIR = System.getProperty("user.home") + File.separator + ".oozx" + File.separator + "pokes";
  
  private Map<String, List<PokFile>> pokIndex = new ConcurrentHashMap<>();
  private boolean initialized = false;

  public PokesManager() {
    initializePokes();
  }

  /**
   * Inicializa el gestor de pokes
   */
  public void initializePokes() {
    if (initialized) return;
    
    new Thread(() -> {
      try {
        downloadPokesIfNeeded();
        indexPokes();
        initialized = true;
        System.out.println("Pokes initialized: " + pokIndex.size() + " games with pokes");
      } catch (Exception e) {
        System.err.println("Error initializing pokes: " + e.getMessage());
        e.printStackTrace();
      }
    }).start();
  }

  /**
   * Descarga el repositorio de pokes si no existe o está desactualizado
   */
  private void downloadPokesIfNeeded() throws Exception {
    Path pokesPath = Paths.get(POKES_DIR);
    
    // Verificar si ya existe
    if (Files.exists(pokesPath)) {
      // Verificar si es antiguo (más de 7 días)
      long lastModified = Files.getLastModifiedTime(pokesPath).toMillis();
      long now = System.currentTimeMillis();
      long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
      
      if (now - lastModified < sevenDaysMs) {
        System.out.println("Pokes already downloaded and fresh");
        return;
      }
      
      // Actualizar repositorio
      System.out.println("Updating pokes repository...");
      updateRepository(pokesPath);
    } else {
      // Clonar repositorio
      System.out.println("Downloading pokes repository...");
      Files.createDirectories(pokesPath.getParent());
      cloneRepository(pokesPath);
    }
  }

  /**
   * Clona el repositorio de pokes
   */
  private void cloneRepository(Path targetPath) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(
        "git", "clone", "--depth", "1", POKES_REPO_URL, targetPath.toString()
    );
    pb.directory(new File(POKES_DIR).getParentFile());
    pb.redirectErrorStream(true);
    
    Process process = pb.start();
    int exitCode = process.waitFor();
    
    if (exitCode != 0) {
      throw new Exception("Failed to clone pokes repository");
    }
  }

  /**
   * Actualiza el repositorio existente
   */
  private void updateRepository(Path repositoryPath) throws Exception {
    ProcessBuilder pb = new ProcessBuilder("git", "pull", "--depth", "1");
    pb.directory(repositoryPath.toFile());
    pb.redirectErrorStream(true);
    
    Process process = pb.start();
    int exitCode = process.waitFor();
    
    if (exitCode != 0) {
      System.err.println("Failed to update pokes repository, using cached version");
    }
  }

  /**
   * Indexa todos los archivos .pok disponibles
   */
  private void indexPokes() throws Exception {
    Path pokesPath = Paths.get(POKES_DIR);
    
    if (!Files.exists(pokesPath)) {
      System.out.println("Pokes directory not found");
      return;
    }

    // Buscar todos los archivos .pok
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
    return POKES_DIR;
  }
}
