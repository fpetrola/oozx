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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OOZxConfiguration {
  private String lastOpenDirectory = System.getProperty("user.home");
  private String lastLoadStateDirectory = System.getProperty("user.home");
  private String lastSaveStateDirectory = System.getProperty("user.home");
  private List<String> recentFiles = new ArrayList<>();
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
}
