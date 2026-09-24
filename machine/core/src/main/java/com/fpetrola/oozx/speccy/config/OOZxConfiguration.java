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

package com.fpetrola.oozx.speccy.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fpetrola.emulation.SnapshotUnicodePacker;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Section;

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

@Section(value = "desktop", wasTheWholeFile = true)
public class OOZxConfiguration implements Configuration.Saves {
  /**
   * Where a file chooser last was. Null until one has been, and answered as the home directory,
   * which is not a default a configuration file should carry: it is a fact about the machine it
   * was written on, and a file that carries it is a file that cannot be copied to another.
   */
  @JsonProperty
  private String lastOpenDirectory;
  @JsonProperty
  private String lastLoadStateDirectory;
  @JsonProperty
  private String lastSaveStateDirectory;
  private List<String> recentFiles = new ArrayList<>();
  private List<Favorite> favorites = new ArrayList<>();
  /** The folders this machine's games are looked for in. */
  @JsonProperty
  private List<String> gameFolders = new ArrayList<>();
  /** The ones inside those that are not being taken into account, which is usually none. */
  @JsonProperty
  private List<String> skippedFolders = new ArrayList<>();
  /** The theme chosen from the Look&Feel menu, by its name, or null for whatever starts up. */
  private String lookAndFeel;
  /**
   * What a newly opened emulator starts its picture with, by knob key. Kept as plain text so a
   * file written by another version still opens: an unknown key is ignored and a missing one
   * leaves that knob alone.
   */
  private Map<String, String> screenDefaults = new LinkedHashMap<>();
  private boolean turboByDefault;
  /** The processor a machine starts on, by name, or null for whichever one this build prefers. */
  /**
   * Looks someone saved, by name. A map of plain text rather than the profile objects
   * themselves, for the same reason the defaults are: the file is read by versions that did not
   * write it, and a knob that has been renamed should cost that knob and not the whole profile.
   */
  private Map<String, Map<String, String>> keptScreenProfiles = new LinkedHashMap<>();
  private List<WindowState> openWindows = new ArrayList<>();
  private WindowState mainWindowState;
  private Map<String, String> snapshots = new HashMap<>();
  private Map<String, SnapshotHistoryEntry> snapshotHistory = new LinkedHashMap<>();
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
  
  private Runnable onHistoryChanged;
  private Runnable onFavoritesChanged = () -> { };

  public OOZxConfiguration() {
  }

  /**
   * Reads one from text with the same mapper the file goes through, so what a test proves about
   * an older file is true of the real thing rather than of a mapper the test made itself.
   */
  public static OOZxConfiguration read(String json) throws IOException {
    return mapper.readValue(json, OOZxConfiguration.class);
  }

  /**
   * Where this section came from, so that saving it saves the file it is one section of rather
   * than the whole file it used to be. Set when the machine's configuration hands it out.
   */
  @JsonIgnore
  public Configuration configuration;

  public void savedBy(Configuration configuration) {
    this.configuration = configuration;
  }

  public void save() {
    cleanOrphanSnapshots();
    if (configuration != null)
      configuration.save();
  }

  public void addRecentFile(String filePath) {
    recentFiles.remove(filePath);
    recentFiles.add(0, filePath);
    if (recentFiles.size() > MAX_RECENT_FILES) {
      recentFiles = new ArrayList<>(recentFiles.subList(0, MAX_RECENT_FILES));
    }
    save();
  }

  @JsonIgnore
  public String getLastOpenDirectory() {
    return lastOpenDirectory == null ? System.getProperty("user.home") : lastOpenDirectory;
  }

  public void setLastOpenDirectory(String lastOpenDirectory) {
    this.lastOpenDirectory = lastOpenDirectory;
  }

  @JsonIgnore
  public String getLastLoadStateDirectory() {
    return lastLoadStateDirectory == null ? System.getProperty("user.home") : lastLoadStateDirectory;
  }

  public void setLastLoadStateDirectory(String lastLoadStateDirectory) {
    this.lastLoadStateDirectory = lastLoadStateDirectory;
  }

  @JsonIgnore
  public String getLastSaveStateDirectory() {
    return lastSaveStateDirectory == null ? System.getProperty("user.home") : lastSaveStateDirectory;
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

  public List<String> getGameFolders() {
    return gameFolders;
  }

  public void setGameFolders(List<String> gameFolders) {
    this.gameFolders = gameFolders;
  }

  /** Keeps one entry per folder, and drops one that is inside another already being looked at. */
  public boolean addGameFolder(String folder) {
    if (gameFolders.stream().anyMatch(kept -> folder.equals(kept) || folder.startsWith(kept + File.separator))) {
      return false;
    }
    gameFolders.removeIf(kept -> kept.startsWith(folder + File.separator));
    gameFolders.add(folder);
    save();
    return true;
  }

  public void removeGameFolder(String folder) {
    if (gameFolders.remove(folder)) {
      save();
    }
  }

  public List<String> getSkippedFolders() {
    return skippedFolders;
  }

  public void setSkippedFolders(List<String> skippedFolders) {
    this.skippedFolders = skippedFolders;
  }

  /** Whether a folder counts, which it does unless it or one it is inside was turned off. */
  public boolean takesIntoAccount(String folder) {
    return skippedFolders.stream()
        .noneMatch(skipped -> folder.equals(skipped) || folder.startsWith(skipped + File.separator));
  }

  public void takeIntoAccount(String folder, boolean taken) {
    skippedFolders.remove(folder);
    if (!taken) {
      skippedFolders.add(folder);
    } else {
      // Turning a folder back on turns on whatever was turned off inside it, which is the only
      // reading of the box that matches what it then shows.
      skippedFolders.removeIf(skipped -> skipped.startsWith(folder + File.separator));
    }
    save();
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
    onFavoritesChanged.run();
    return true;
  }

  public void removeFavorite(String source) {
    favorites.removeIf(f -> f.getSource().equals(source));
    save();
    onFavoritesChanged.run();
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

  public void setOnFavoritesChanged(Runnable callback) {
    this.onFavoritesChanged = callback == null ? () -> { } : callback;
  }

  public void setOnHistoryChanged(Runnable callback) {
    this.onHistoryChanged = callback;
  }

  public void addToSnapshotHistory(String filePath, String gameName, String initialStateData) {
    String key = new File(filePath).getAbsolutePath();
    String stateId = saveSnapshot(initialStateData);

    SnapshotHistoryEntry entry = new SnapshotHistoryEntry(gameName, filePath, System.currentTimeMillis(), stateId);
    snapshotHistory.put(key, entry);

    if (snapshotHistory.size() > MAX_SNAPSHOT_HISTORY) {
      String oldestKey = snapshotHistory.keySet().iterator().next();
      SnapshotHistoryEntry oldEntry = snapshotHistory.remove(oldestKey);
      if (oldEntry.getInitialStateId() != null) {
        snapshots.remove(oldEntry.getInitialStateId());
      }
    }
    save();

    if (onHistoryChanged != null) {
      onHistoryChanged.run();
    }
  }

  public void addToSnapshotHistory(String filePath, String gameName) {
    addToSnapshotHistory(filePath, gameName, null);
  }

  /** Runs before every save so a file removed elsewhere does not linger in the snapshot map forever. */
  private void cleanOrphanSnapshots() {
    java.util.Set<String> referencedIds = new java.util.HashSet<>();

    for (SnapshotHistoryEntry entry : snapshotHistory.values()) {
      if (entry.getInitialStateId() != null) {
        referencedIds.add(entry.getInitialStateId());
      }
    }

    for (WindowState window : openWindows) {
      if (window.getSnapshotId() != null) {
        referencedIds.add(window.getSnapshotId());
      }
    }

    java.util.Set<String> orphanIds = new java.util.HashSet<>(snapshots.keySet());
    orphanIds.removeAll(referencedIds);

    for (String orphanId : orphanIds) {
      snapshots.remove(orphanId);
    }
  }

  public String saveSnapshot(String snapshotData) {
    String snapshotId = "snapshot_" + UUID.randomUUID().toString();
    snapshots.put(snapshotId, snapshotData);
    return snapshotId;
  }

  public String getSnapshot(String snapshotId) {
    return snapshots.get(snapshotId);
  }

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

      byte[] decompressedData = new byte[compressedData.length * 10];
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

  public static class SnapshotHistoryEntry {
    private String gameName;
    private String filePath;
    private long loadedTime;
    private String initialStateId;

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

  @JsonPropertyOrder({
      "type", "x", "y", "width", "height", "zOrder",
      "filePath", "snapshotName", "searchQuery", "turboMode", "muted", "paused",
      "snapshotId", "appliedPokes"
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
    @JsonIgnore
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
    private String type;
    private int x;
    private int y;
    private int width;
    private int height;
    private int zOrder;
    private String filePath;
    private String snapshotName;
    private String searchQuery;
    private boolean turboMode;
    private boolean muted;
    private boolean paused;
    private String snapshotId;
    private List<PokModState> appliedPokes = new ArrayList<>();

    public WindowState() {
    }

    public WindowState(String type, int x, int y, int width, int height) {
      this.type = type;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

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

  /** A poke mod, kept in a form Jackson can write and read back to reverse it later. */
  public static class PokModState {
    private String name;
    private String rawInstruction;
    private String pokFileName;
    private String gameName;
    private String instructionType;
    private String description;
    private Integer previousValue;
    private Integer previousBank;
    private Integer previousAddress;

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
