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
    
    String currentModName = null;
    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty() || line.equals("Y")) continue;
      
      if (line.startsWith("N")) {
        // Nombre del poke
        currentModName = line.substring(1).trim();
      } else if (line.startsWith("M") || line.startsWith("Z")) {
        // Modificación de memoria
        // Formato: [M|Z] bank address value [...]
        if (currentModName != null) {
          PokeMod mod = new PokeMod(currentModName, line);
          mods.add(mod);
        }
      }
    }
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
    private String instruction;

    public PokeMod(String name, String instruction) {
      this.name = name;
      this.instruction = instruction;
    }

    public String getName() {
      return name;
    }

    public String getInstruction() {
      return instruction;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
