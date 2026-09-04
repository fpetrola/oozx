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

package com.fpetrola.oozx.speccy.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fpetrola.emulation.SnapshotUnicodePacker;

import java.beans.Transient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class OOZxConfiguration {
  private String lastOpenDirectory = System.getProperty("user.home");
  private String lastLoadStateDirectory = System.getProperty("user.home");
  private String lastSaveStateDirectory = System.getProperty("user.home");
  private List<String> recentFiles = new ArrayList<>();
  private List<Favorite> favorites = new ArrayList<>();
  /** The theme chosen from the Look&Feel menu, by its name, or null for whatever starts up. */
  private String lookAndFeel;
  /**
   * What a newly opened emulator starts its picture with, by knob key. Kept as plain text so a
   * file written by another version still opens: an unknown key is ignored and a missing one
   * leaves that knob alone.
   */
  private Map<String, String> screenDefaults = new LinkedHashMap<>();
  private boolean turboByDefault;
  /**
   * Looks someone saved, by name. A map of plain text rather than the profile objects
   * themselves, for the same reason the defaults are: the file is read by versions that did not
   * write it, and a knob that has been renamed should cost that knob and not the whole profile.
   */
  private Map<String, Map<String, String>> keptScreenProfiles = new LinkedHashMap<>();
  private List<WindowState> openWindows = new ArrayList<>();
  private WindowState mainWindowState; // Estado de la ventana principal
  private Map<String, String> snapshots = new HashMap<>(); // Mapa centralizado de snapshots: id -> data
  private Map<String, SnapshotHistoryEntry> snapshotHistory = new LinkedHashMap<>(); // Historial de snapshots cargados
  private static final int MAX_RECENT_FILES = 10;
  private static final int MAX_SNAPSHOT_HISTORY = 20;
  private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".oozx";
  private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";

  /**
   * Unknown fields are ignored rather than refused. A configuration file outlives the versions
   * that write it: a setting dropped from the code would otherwise make the whole file
   * unreadable, and because load() treats that as an ordinary read failure the emulator would
   * start with an empty configuration — losing the favourites, the recent files and the window
   * positions to say nothing of the setting that was removed.
   */
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(com.fasterxml.jackson.databind.DeserializationFeature
          .FAIL_ON_UNKNOWN_PROPERTIES, false);
  
  // Callback para notificar cambios en el historial
  private Runnable onHistoryChanged;

  public OOZxConfiguration() {
    // Constructor vacío para Jackson
  }

  /**
   * Reads one from text with the same mapper the file goes through, so what a test proves about
   * an older file is true of the real thing rather than of a mapper the test made itself.
   */
  public static OOZxConfiguration read(String json) throws IOException {
    return mapper.readValue(json, OOZxConfiguration.class);
  }

  public static OOZxConfiguration load() {
    try {
      File configFile = new File(CONFIG_FILE);
      if (configFile.exists()) {
        return mapper.readValue(configFile, OOZxConfiguration.class);
      }
    } catch (IOException e) {
      System.err.println("Error cargando configuración: " + e.getMessage());
    }
    return new OOZxConfiguration();
  }

  public void save() {
    // Limpiar snapshots huérfanos antes de guardar
    cleanOrphanSnapshots();
    
    try {
      File configDir = new File(CONFIG_DIR);
      if (!configDir.exists()) {
        configDir.mkdirs();
      }
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(CONFIG_FILE), this);
    } catch (IOException e) {
      System.err.println("Error guardando configuración: " + e.getMessage());
    }
  }

  public void addRecentFile(String filePath) {
    // Remover si ya existe para evitar duplicados
    recentFiles.remove(filePath);
    // Agregar al inicio
    recentFiles.add(0, filePath);
    // Mantener solo los últimos N archivos
    if (recentFiles.size() > MAX_RECENT_FILES) {
      recentFiles = new ArrayList<>(recentFiles.subList(0, MAX_RECENT_FILES));
    }
    save();
  }

  // Getters y Setters
  public String getLastOpenDirectory() {
    return lastOpenDirectory;
  }

  public void setLastOpenDirectory(String lastOpenDirectory) {
    this.lastOpenDirectory = lastOpenDirectory;
  }

  public String getLastLoadStateDirectory() {
    return lastLoadStateDirectory;
  }

  public void setLastLoadStateDirectory(String lastLoadStateDirectory) {
    this.lastLoadStateDirectory = lastLoadStateDirectory;
  }

  public String getLastSaveStateDirectory() {
    return lastSaveStateDirectory;
  }

  public void setLastSaveStateDirectory(String lastSaveStateDirectory) {
    this.lastSaveStateDirectory = lastSaveStateDirectory;
  }

  public Map<String, String> getScreenDefaults() {
    return screenDefaults;
  }

  public void setScreenDefaults(Map<String, String> screenDefaults) {
    this.screenDefaults = screenDefaults;
  }

  public boolean isTurboByDefault() {
    return turboByDefault;
  }

  public void setTurboByDefault(boolean turboByDefault) {
    this.turboByDefault = turboByDefault;
  }

  public Map<String, Map<String, String>> getKeptScreenProfiles() {
    return keptScreenProfiles;
  }

  public void setKeptScreenProfiles(Map<String, Map<String, String>> keptScreenProfiles) {
    this.keptScreenProfiles = keptScreenProfiles;
  }

  public String getLookAndFeel() {
    return lookAndFeel;
  }

  public void setLookAndFeel(String lookAndFeel) {
    this.lookAndFeel = lookAndFeel;
  }

  public List<Favorite> getFavorites() {
    return favorites;
  }

  public void setFavorites(List<Favorite> favorites) {
    this.favorites = favorites;
  }

  /** Keeps one entry per source, so favouriting the same game twice does not list it twice. */
  public boolean addFavorite(Favorite favorite) {
    if (favorites.stream().anyMatch(f -> f.getSource().equals(favorite.getSource()))) return false;
    favorites.add(favorite);
    save();
    return true;
  }

  public void removeFavorite(String source) {
    favorites.removeIf(f -> f.getSource().equals(source));
    save();
  }

  public boolean isFavorite(String source) {
    return source != null && favorites.stream().anyMatch(f -> f.getSource().equals(source));
  }

  public List<String> getRecentFiles() {
    return recentFiles;
  }

  public void setRecentFiles(List<String> recentFiles) {
    this.recentFiles = recentFiles;
  }

  public List<WindowState> getOpenWindows() {
    return openWindows;
  }

  public void setOpenWindows(List<WindowState> openWindows) {
    this.openWindows = openWindows;
  }

  public WindowState getMainWindowState() {
    return mainWindowState;
  }

  public void setMainWindowState(WindowState mainWindowState) {
    this.mainWindowState = mainWindowState;
  }

  public Map<String, String> getSnapshots() {
    return snapshots;
  }

  public void setSnapshots(Map<String, String> snapshots) {
    this.snapshots = snapshots;
  }

  public Map<String, SnapshotHistoryEntry> getSnapshotHistory() {
    return snapshotHistory;
  }

  public void setSnapshotHistory(Map<String, SnapshotHistoryEntry> snapshotHistory) {
    this.snapshotHistory = snapshotHistory;
  }

  public void setOnHistoryChanged(Runnable callback) {
    this.onHistoryChanged = callback;
  }

  /**
   * Agrega un snapshot al historial con estado inicial
   */
  public void addToSnapshotHistory(String filePath, String gameName, String initialStateData) {
    String key = new File(filePath).getAbsolutePath();
    
    // Guardar el estado inicial
    String stateId = saveSnapshot(initialStateData);
    
    SnapshotHistoryEntry entry = new SnapshotHistoryEntry(gameName, filePath, System.currentTimeMillis(), stateId);
    snapshotHistory.put(key, entry);
    
    // Mantener solo los últimos N snapshots
    if (snapshotHistory.size() > MAX_SNAPSHOT_HISTORY) {
      String oldestKey = snapshotHistory.keySet().iterator().next();
      SnapshotHistoryEntry oldEntry = snapshotHistory.remove(oldestKey);
      // Limpiar el estado guardado del snapshots map
      if (oldEntry.getInitialStateId() != null) {
        snapshots.remove(oldEntry.getInitialStateId());
      }
    }
    save();
    
    // Notificar cambios en el historial
    if (onHistoryChanged != null) {
      onHistoryChanged.run();
    }
  }

  /**
   * Versión sin estado inicial (compatible con código existente)
   */
  public void addToSnapshotHistory(String filePath, String gameName) {
    addToSnapshotHistory(filePath, gameName, null);
  }

  /**
   * Limpia snapshots huérfanos (no referenciados en el historial o ventanas abiertas)
   * Se llamará automáticamente antes de guardar
   */
  private void cleanOrphanSnapshots() {
    java.util.Set<String> referencedIds = new java.util.HashSet<>();
    
    // Recolectar IDs referenciados en el historial
    for (SnapshotHistoryEntry entry : snapshotHistory.values()) {
      if (entry.getInitialStateId() != null) {
        referencedIds.add(entry.getInitialStateId());
      }
    }
    
    // Recolectar IDs referenciados en ventanas abiertas
    for (WindowState window : openWindows) {
      if (window.getSnapshotId() != null) {
        referencedIds.add(window.getSnapshotId());
      }
    }
    
    // Eliminar snapshots no referenciados
    java.util.Set<String> orphanIds = new java.util.HashSet<>(snapshots.keySet());
    orphanIds.removeAll(referencedIds);
    
    for (String orphanId : orphanIds) {
      snapshots.remove(orphanId);
    }
  }

  /**
   * Guarda un snapshot en el mapa centralizado y retorna su ID
   */
  public String saveSnapshot(String snapshotData) {
    String snapshotId = "snapshot_" + UUID.randomUUID().toString();
    snapshots.put(snapshotId, snapshotData);
    return snapshotId;
  }

  /**
   * Obtiene un snapshot del mapa centralizado usando su ID
   */
  public String getSnapshot(String snapshotId) {
    return snapshots.get(snapshotId);
  }

  // Utilidades de empaquetado usando SnapshotUnicodePacker
  public static String packSnapshot(byte[] data) {
    try {
      return SnapshotUnicodePacker.packToUnicodeString(data);
    } catch (Exception e) {
      System.err.println("Error empaquetando snapshot: " + e.getMessage());
      return null;
    }
  }

  public static byte[] unpackSnapshot(String packed) {
    try {
      return SnapshotUnicodePacker.unpackFromUnicodeString(packed);
    } catch (Exception e) {
      System.err.println("Error desempaquetando snapshot: " + e.getMessage());
      return null;
    }
  }

  // Utilidades de compresión (legacy - para mantener compatibilidad)
  public static String compressAndEncode(byte[] data) {
    try {
      Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
      deflater.setInput(data);
      deflater.finish();

      byte[] compressedData = new byte[data.length];
      int compressedSize = deflater.deflate(compressedData);
      deflater.end();

      byte[] finalData = new byte[compressedSize];
      System.arraycopy(compressedData, 0, finalData, 0, compressedSize);

      return Base64.getEncoder().encodeToString(finalData);
    } catch (Exception e) {
      System.err.println("Error comprimiendo datos: " + e.getMessage());
      return null;
    }
  }

  public static byte[] decodeAndDecompress(String encoded) {
    try {
      byte[] compressedData = Base64.getDecoder().decode(encoded);

      Inflater inflater = new Inflater();
      inflater.setInput(compressedData);

      byte[] decompressedData = new byte[compressedData.length * 10]; // Aproximado
      int decompressedSize = inflater.inflate(decompressedData);
      inflater.end();

      byte[] finalData = new byte[decompressedSize];
      System.arraycopy(decompressedData, 0, finalData, 0, decompressedSize);

      return finalData;
    } catch (Exception e) {
      System.err.println("Error descomprimiendo datos: " + e.getMessage());
      return null;
    }
  }

  // Clase para almacenar una entrada del historial de snapshots
  public static class SnapshotHistoryEntry {
    private String gameName;          // Nombre del juego
    private String filePath;          // Ruta del archivo
    private long loadedTime;          // Timestamp cuando se cargó
    private String initialStateId;    // ID del snapshot del estado inicial guardado en la config

    public SnapshotHistoryEntry() {
    }

    public SnapshotHistoryEntry(String gameName, String filePath, long loadedTime) {
      this.gameName = gameName;
      this.filePath = filePath;
      this.loadedTime = loadedTime;
    }

    public SnapshotHistoryEntry(String gameName, String filePath, long loadedTime, String initialStateId) {
      this.gameName = gameName;
      this.filePath = filePath;
      this.loadedTime = loadedTime;
      this.initialStateId = initialStateId;
    }

    public String getGameName() {
      return gameName;
    }

    public void setGameName(String gameName) {
      this.gameName = gameName;
    }

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public long getLoadedTime() {
      return loadedTime;
    }

    public void setLoadedTime(long loadedTime) {
      this.loadedTime = loadedTime;
    }

    public String getInitialStateId() {
      return initialStateId;
    }

    public void setInitialStateId(String initialStateId) {
      this.initialStateId = initialStateId;
    }

    @Transient
    public String getDisplayName() {
      return gameName + " (" + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(loadedTime)) + ")";
    }
  }

  // Clase para almacenar estado de ventanas
  @JsonPropertyOrder({
      "type", "x", "y", "width", "height", "zOrder",
      "filePath", "snapshotName", "searchQuery", "turboMode", "muted", "paused",
      "snapshotId", "appliedPokes" // Referencia al snapshot en el mapa centralizado y pokes aplicados
  })
  /**
   * A game kept to play again, and enough to play it with.
   * <p>
   * The source is whatever the launcher already knows how to open: a URL or a local path, a
   * tape, a snapshot or a recording. Storing that rather than the game's page is the difference
   * between an entry that can be launched and one that can only be looked at.
   */
  public static class Favorite {
    /** What the launcher opens: a URL or a local path. */
    private String source;
    private String title;
    /** GAME goes to the emulator, RECORDING to the RZX player. */
    private String kind = "GAME";
    /** The ZXInfo id when it came from a search, so the entry can be looked up again. */
    private String gameId;
    /**
     * Which file inside the archive was the one played, when the source is a zip. Recordings
     * often come several to a file, and without this a favourite would come back asking again
     * which part was meant — or silently opening a different one.
     */
    private String entry;

    public Favorite() {
    }

    public Favorite(String source, String title, String kind, String gameId) {
      this(source, title, kind, gameId, null);
    }

    public Favorite(String source, String title, String kind, String gameId, String entry) {
      this.source = source;
      this.title = title;
      this.kind = kind;
      this.gameId = gameId;
      this.entry = entry;
    }

    /**
     * Not a property: it reads the kind rather than holding anything, and Jackson would write
     * it out as one and then refuse to read the file back, having no setter to put it in.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isRecording() {
      return "RECORDING".equals(kind);
    }

    public String getSource() {
      return source;
    }

    public void setSource(String source) {
      this.source = source;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getKind() {
      return kind;
    }

    public void setKind(String kind) {
      this.kind = kind;
    }

    public String getEntry() {
      return entry;
    }

    public void setEntry(String entry) {
      this.entry = entry;
    }

    public String getGameId() {
      return gameId;
    }

    public void setGameId(String gameId) {
      this.gameId = gameId;
    }
  }

  public static class WindowState {
    private String type; // "EMULATOR", "GAME_BROWSER"
    private int x;
    private int y;
    private int width;
    private int height;
    private int zOrder; // Orden de profundidad de la ventana (0 = al frente)
    private String filePath; // Para emuladores
    private String snapshotName; // Nombre legible del archivo/snapshot cargado
    private String searchQuery; // Para game browser
    private boolean turboMode;
    private boolean muted;
    private boolean paused; // Estado de pausa del emulador
    private String snapshotId; // Referencia al snapshot en el mapa centralizado
    private List<PokModState> appliedPokes = new ArrayList<>(); // Pokes aplicados en el emulador

    public WindowState() {
    }

    public WindowState(String type, int x, int y, int width, int height) {
      this.type = type;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

    // Getters and Setters
    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public int getX() {
      return x;
    }

    public void setX(int x) {
      this.x = x;
    }

    public int getY() {
      return y;
    }

    public void setY(int y) {
      this.y = y;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }

    public int getZOrder() {
      return zOrder;
    }

    public void setZOrder(int zOrder) {
      this.zOrder = zOrder;
    }

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public String getSnapshotName() {
      return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
      this.snapshotName = snapshotName;
    }

    public String getSearchQuery() {
      return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
      this.searchQuery = searchQuery;
    }

    public boolean isTurboMode() {
      return turboMode;
    }

    public void setTurboMode(boolean turboMode) {
      this.turboMode = turboMode;
    }

    public boolean isMuted() {
      return muted;
    }

    public void setMuted(boolean muted) {
      this.muted = muted;
    }

    public boolean isPaused() {
      return paused;
    }

    public void setPaused(boolean paused) {
      this.paused = paused;
    }

    public String getSnapshotId() {
      return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
      this.snapshotId = snapshotId;
    }

    public List<PokModState> getAppliedPokes() {
      return appliedPokes;
    }

    public void setAppliedPokes(List<PokModState> appliedPokes) {
      this.appliedPokes = appliedPokes;
    }
  }

  /**
   * Representa un poke aplicado de forma serializable
   * Almacena información para poder revertir el poke posteriormente
   */
  public static class PokModState {
    private String name;                    // Nombre del poke (ej: "Infinite Lives")
    private String rawInstruction;          // Instrucción raw (ej: "M65280,255")
    private String pokFileName;             // Nombre del archivo .pok (ej: "JetPac (1983)(Ultimate)")
    private String gameName;                // Nombre del juego para identificación
    private String instructionType;         // Tipo de instrucción parseado
    private String description;             // Descripción del poke
    private Integer previousValue;          // Valor anterior (para revertir)
    private Integer previousBank;           // Banco anterior (para revertir)
    private Integer previousAddress;        // Dirección anterior (para revertir)

    public PokModState() {
    }

    public PokModState(String name, String rawInstruction) {
      this.name = name;
      this.rawInstruction = rawInstruction;
    }

    public PokModState(String name, String rawInstruction, String pokFileName, String gameName, 
                       String instructionType, String description) {
      this.name = name;
      this.rawInstruction = rawInstruction;
      this.pokFileName = pokFileName;
      this.gameName = gameName;
      this.instructionType = instructionType;
      this.description = description;
    }

    public PokModState(String name, String rawInstruction, String pokFileName, String gameName, 
                       String instructionType, String description, Integer previousValue,
                       Integer previousBank, Integer previousAddress) {
      this(name, rawInstruction, pokFileName, gameName, instructionType, description);
      this.previousValue = previousValue;
      this.previousBank = previousBank;
      this.previousAddress = previousAddress;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRawInstruction() {
      return rawInstruction;
    }

    public void setRawInstruction(String rawInstruction) {
      this.rawInstruction = rawInstruction;
    }

    public String getPokFileName() {
      return pokFileName;
    }

    public void setPokFileName(String pokFileName) {
      this.pokFileName = pokFileName;
    }

    public String getGameName() {
      return gameName;
    }

    public void setGameName(String gameName) {
      this.gameName = gameName;
    }

    public String getInstructionType() {
      return instructionType;
    }

    public void setInstructionType(String instructionType) {
      this.instructionType = instructionType;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getPreviousValue() {
      return previousValue;
    }

    public void setPreviousValue(Integer previousValue) {
      this.previousValue = previousValue;
    }

    public Integer getPreviousBank() {
      return previousBank;
    }

    public void setPreviousBank(Integer previousBank) {
      this.previousBank = previousBank;
    }

    public Integer getPreviousAddress() {
      return previousAddress;
    }

    public void setPreviousAddress(Integer previousAddress) {
      this.previousAddress = previousAddress;
    }

    @Override
    public String toString() {
      return "PokModState{" +
          "name='" + name + '\'' +
          ", instructionType='" + instructionType + '\'' +
          ", pokFileName='" + pokFileName + '\'' +
          '}';
    }
  }
}
