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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PokFile {
  private String name;
  private Path filePath;
  private List<PokeMod> mods = new ArrayList<>();
  private String rawContent;

  public PokFile(String name, Path filePath) {
    this.name = name;
    this.filePath = filePath;
  }

  /** One that was written down rather than read from a .pok, which is how they ship now. */
  public PokFile(String name, List<PokeMod> mods) {
    this.name = name;
    this.mods = new ArrayList<>(mods);
  }

  public void parseContent() throws IOException {
    rawContent = new String(Files.readAllBytes(filePath));
    String[] lines = rawContent.split("\n");
    
    String gameName = extractGameName(name);
    
    String currentModName = null;
    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) continue;
      
      if (line.startsWith("N")) {
        String modName = line.substring(1).trim();
        if (isValidModName(modName)) {
          currentModName = modName;
        } else {
          currentModName = null;
        }
      } else if (line.equals("Y")) {
        currentModName = null;
      } else if (isInstructionLine(line)) {
        if (currentModName != null) {
          PokeMod mod = new PokeMod(currentModName, line, name, gameName);
          mods.add(mod);
        }
      }
    }
  }

  private String extractGameName(String pokFileName) {
    int parenIndex = pokFileName.indexOf('(');
    if (parenIndex > 0) {
      return pokFileName.substring(0, parenIndex).trim();
    }
    return pokFileName;
  }
  
  /** Filters out prose lines some .pok files carry (instructions, credits, URLs) that look like a name line but are not one. */
  private boolean isValidModName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }

    String lower = name.toLowerCase();
    if (lower.contains("press ") || 
        lower.contains("attachments") ||
        lower.contains("get all") ||
        lower.contains("speccy") ||
        lower.contains("http") ||
        lower.contains("www")) {
      return false;
    }

    return name.length() >= 2;
  }

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

  public static class PokeMod {
    private String name;
    private String rawInstruction;
    private PokInstruction parsedInstruction;
    private String pokFileName;
    private String gameName;

    public PokeMod(String name, String rawInstruction) {
      this.name = name;
      this.rawInstruction = rawInstruction;
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
