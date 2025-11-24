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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Representa un archivo .pok con sus modificaciones de memoria
 */
public class PokFile {
  private String name;                    // Nombre del archivo sin extensión
  private Path filePath;
  private List<PokeMod> mods = new ArrayList<>();
  private String rawContent;

  public PokFile(String name, Path filePath) {
    this.name = name;
    this.filePath = filePath;
  }

  public void parseContent() throws IOException {
    rawContent = new String(Files.readAllBytes(filePath));
    String[] lines = rawContent.split("\n");
    
    // Extraer el nombre del juego del nombre del archivo .pok
    String gameName = extractGameName(name);
    
    String currentModName = null;
    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) continue;
      
      if (line.startsWith("N")) {
        // Nombre del poke - validar que sea un nombre válido
        String modName = line.substring(1).trim();
        if (isValidModName(modName)) {
          currentModName = modName;
        } else {
          currentModName = null;
        }
      } else if (line.equals("Y")) {
        // Fin del poke actual
        currentModName = null;
      } else if (isInstructionLine(line)) {
        // Modificación de memoria (M, Z, A, X, etc.)
        if (currentModName != null) {
          PokeMod mod = new PokeMod(currentModName, line, name, gameName);
          mods.add(mod);
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
   * Valida si un nombre de mod es válido (descarta líneas de basura como "Press W...")
   */
  private boolean isValidModName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    
    // Rechazar líneas que contengan palabras clave típicas de mensajes/instrucciones
    String lower = name.toLowerCase();
    if (lower.contains("press ") || 
        lower.contains("attachments") ||
        lower.contains("get all") ||
        lower.contains("fuse") ||
        lower.contains("http") ||
        lower.contains("www")) {
      return false;
    }
    
    // El nombre debe tener al menos 2 caracteres significativos
    return name.length() >= 2;
  }

  /**
   * Verifica si una línea es una instrucción de poke
   */
  private boolean isInstructionLine(String line) {
    if (line.isEmpty()) return false;
    char firstChar = line.charAt(0);
    return firstChar == 'M' || firstChar == 'Z' || firstChar == 'A' || 
           firstChar == 'X' || firstChar == 'Y' || firstChar == 'B' || 
           firstChar == 'C' || firstChar == 'D' || firstChar == 'E';
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return name.replace(".pok", "").replace("(1989)(Melbourne House)", "").trim();
  }

  public Path getFilePath() {
    return filePath;
  }

  public List<PokeMod> getMods() {
    return mods;
  }

  public String getRawContent() {
    return rawContent;
  }

  /**
    * Representa una modificación individual en un archivo .pok
    */
  public static class PokeMod {
    private String name;
    private String rawInstruction;
    private PokInstruction parsedInstruction;
    private String pokFileName;             // Nombre del archivo .pok (ej: "JetPac (1983)(Ultimate)")
    private String gameName;                // Nombre del juego para identificación

    public PokeMod(String name, String rawInstruction) {
      this.name = name;
      this.rawInstruction = rawInstruction;
      // Parsear la instrucción al crear el objeto
      try {
        this.parsedInstruction = PokInstruction.parse(rawInstruction);
      } catch (IllegalArgumentException e) {
        System.err.println("Error parsing instruction '" + rawInstruction + "': " + e.getMessage());
        this.parsedInstruction = new PokInstruction.GenericInstruction(rawInstruction);
      }
    }

    public PokeMod(String name, String rawInstruction, String pokFileName, String gameName) {
      this(name, rawInstruction);
      this.pokFileName = pokFileName;
      this.gameName = gameName;
    }

    public String getName() {
      return name;
    }

    public String getRawInstruction() {
      return rawInstruction;
    }

    public PokInstruction getParsedInstruction() {
      return parsedInstruction;
    }

    public String getInstructionType() {
      return parsedInstruction.getInstructionType();
    }

    public String getDescription() {
      return parsedInstruction.getDescription();
    }

    public boolean isApplied() {
      return parsedInstruction.isApplied();
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

    @Override
    public String toString() {
      return name;
    }
  }
}
