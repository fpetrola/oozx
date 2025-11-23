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

package com.fpetrola.oozx.fuse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
  private List<WindowState> openWindows = new ArrayList<>();
  private WindowState mainWindowState; // Estado de la ventana principal
  private Map<String, String> snapshots = new HashMap<>(); // Mapa centralizado de snapshots: id -> data
  private static final int MAX_RECENT_FILES = 10;
  private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".oozx";
  private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";

  private static final ObjectMapper mapper = new ObjectMapper();

  public OOZxConfiguration() {
    // Constructor vacío para Jackson
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

  // Utilidades de compresión
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

  // Clase para almacenar estado de ventanas
  @JsonPropertyOrder({
      "type", "x", "y", "width", "height", "zOrder",
      "filePath", "snapshotName", "searchQuery", "turboMode", "muted", "paused",
      "snapshotId" // Referencia al snapshot en el mapa centralizado
  })
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
  }
}
