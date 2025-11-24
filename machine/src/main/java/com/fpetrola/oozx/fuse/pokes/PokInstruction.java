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

/**
 * Representación abstracta de una instrucción de poke
 * Las instrucciones pueden ser de diferentes tipos (Memory write, conditional, etc)
 * Guarda el valor previo para poder revertir el cambio
 */
public abstract class PokInstruction {
  protected String rawInstruction;
  protected Integer previousValue = null;  // Valor guardado antes de aplicar el poke
  protected Integer previousBank = null;   // Banco guardado (para instrucciones que lo modifican)
  protected Integer previousAddress = null; // Dirección guardada (para instrucciones que la modifican)

  public PokInstruction(String rawInstruction) {
    this.rawInstruction = rawInstruction;
  }

  public String getRawInstruction() {
    return rawInstruction;
  }

  public Integer getPreviousValue() {
    return previousValue;
  }

  public Integer getPreviousBank() {
    return previousBank;
  }

  public Integer getPreviousAddress() {
    return previousAddress;
  }

  public void setPreviousValue(Integer previousValue) {
    this.previousValue = previousValue;
  }

  public void setPreviousBank(Integer previousBank) {
    this.previousBank = previousBank;
  }

  public void setPreviousAddress(Integer previousAddress) {
    this.previousAddress = previousAddress;
  }

  public abstract String getInstructionType();

  public abstract String getDescription();

  /**
   * Aplica el poke y guarda el valor previo para poder revertir
   */
  public abstract void apply(EmulatorMemoryWriter memoryWriter);

  /**
   * Revierte el poke restaurando el valor previo
   */
  public abstract void revert(EmulatorMemoryWriter memoryWriter);

  /**
   * Retorna true si el poke fue aplicado (tiene valor previo guardado)
   */
  public boolean isApplied() {
    return previousValue != null;
  }

  /**
   * Interfaz para escribir en memoria del emulador
   */
  public interface EmulatorMemoryWriter {
    void writeMemory(int bank, int address, int value);
    int readMemory(int bank, int address);
  }

  /**
   * Parsea una línea de instrucción y retorna el tipo correcto
   */
  public static PokInstruction parse(String line) throws IllegalArgumentException {
    String[] parts = line.trim().split("\\s+");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid instruction format: " + line);
    }

    String type = parts[0];

    switch (type) {
      case "M":
        // Memory write without condition: M bank address value condition
        return new MemoryWriteInstruction(line, parts);
      case "Z":
        // Memory write, reset address to zero: Z bank address value condition
        return new MemoryResetInstruction(line, parts);
      case "A":
        // Add to address: A bank address offset condition
        return new MemoryAddInstruction(line, parts);
      case "X":
        // XOR with memory: X bank address value condition
        return new MemoryXorInstruction(line, parts);
      case "Y":
        // End of poke marker
        return new EndMarkerInstruction(line);
      default:
        // Instruction type not recognized, create generic
        return new GenericInstruction(line);
    }
  }

  /**
   * Instrucción genérica (fallback)
   */
  public static class GenericInstruction extends PokInstruction {
    public GenericInstruction(String rawInstruction) {
      super(rawInstruction);
    }

    @Override
    public String getInstructionType() {
      return "GENERIC";
    }

    @Override
    public String getDescription() {
      return "Unknown instruction: " + rawInstruction;
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      System.out.println("Cannot apply unknown instruction: " + rawInstruction);
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      System.out.println("Cannot revert unknown instruction: " + rawInstruction);
    }
  }

  /**
   * Marcador de fin de poke
   */
  public static class EndMarkerInstruction extends PokInstruction {
    public EndMarkerInstruction(String rawInstruction) {
      super(rawInstruction);
    }

    @Override
    public String getInstructionType() {
      return "END";
    }

    @Override
    public String getDescription() {
      return "End of poke marker";
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      // No action needed
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      // No action needed
    }
  }

  /**
   * Escritura de memoria: M bank address value condition
   * Escribe un valor en una dirección de memoria específica
   */
  public static class MemoryWriteInstruction extends PokInstruction {
    private int bank;
    private int address;
    private int value;
    private int condition;

    public MemoryWriteInstruction(String raw, String[] parts) throws IllegalArgumentException {
      super(raw);
      if (parts.length < 5) {
        throw new IllegalArgumentException("MemoryWrite requires: M bank address value condition");
      }
      try {
        this.bank = parseNumber(parts[1]);
        this.address = parseNumber(parts[2]);
        this.value = parseNumber(parts[3]);
        this.condition = parseNumber(parts[4]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid numbers in MemoryWrite: " + e.getMessage());
      }
    }

    public int getBank() { return bank; }
    public int getAddress() { return address; }
    public int getValue() { return value; }
    public int getCondition() { return condition; }

    @Override
    public String getInstructionType() {
      return "MEMORY_WRITE";
    }

    @Override
    public String getDescription() {
      return String.format("Write 0x%02X to memory bank %d at address 0x%04X", value, bank, address);
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      // Guardar el valor previo antes de modificar
      previousValue = memoryWriter.readMemory(bank, address);
      previousBank = bank;
      previousAddress = address;
      // Aplicar el cambio
      memoryWriter.writeMemory(bank, address, value);
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      if (previousValue != null && previousBank != null && previousAddress != null) {
        memoryWriter.writeMemory(previousBank, previousAddress, previousValue);
        // Limpiar el registro después de revertir
        previousValue = null;
        previousBank = null;
        previousAddress = null;
      }
    }
  }

  /**
   * Escritura de memoria con reset: Z bank address value condition
   * Similar a M pero con reset a cero
   */
  public static class MemoryResetInstruction extends PokInstruction {
    private int bank;
    private int address;
    private int value;
    private int condition;

    public MemoryResetInstruction(String raw, String[] parts) throws IllegalArgumentException {
      super(raw);
      if (parts.length < 5) {
        throw new IllegalArgumentException("MemoryReset requires: Z bank address value condition");
      }
      try {
        this.bank = parseNumber(parts[1]);
        this.address = parseNumber(parts[2]);
        this.value = parseNumber(parts[3]);
        this.condition = parseNumber(parts[4]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid numbers in MemoryReset: " + e.getMessage());
      }
    }

    public int getBank() { return bank; }
    public int getAddress() { return address; }
    public int getValue() { return value; }
    public int getCondition() { return condition; }

    @Override
    public String getInstructionType() {
      return "MEMORY_RESET";
    }

    @Override
    public String getDescription() {
      return String.format("Reset and write 0x%02X to memory bank %d at address 0x%04X", value, bank, address);
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      // Guardar valores previos
      previousValue = memoryWriter.readMemory(bank, address);
      previousBank = bank;
      previousAddress = address;
      // Reset address to zero and then write
      memoryWriter.writeMemory(bank, 0, 0);
      memoryWriter.writeMemory(bank, address, value);
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      if (previousValue != null && previousBank != null && previousAddress != null) {
        memoryWriter.writeMemory(previousBank, previousAddress, previousValue);
        // Limpiar el registro después de revertir
        previousValue = null;
        previousBank = null;
        previousAddress = null;
      }
    }
  }

  /**
   * Suma a memoria: A bank address offset condition
   * Suma un valor a la dirección de memoria
   */
  public static class MemoryAddInstruction extends PokInstruction {
    private int bank;
    private int address;
    private int offset;
    private int condition;

    public MemoryAddInstruction(String raw, String[] parts) throws IllegalArgumentException {
      super(raw);
      if (parts.length < 5) {
        throw new IllegalArgumentException("MemoryAdd requires: A bank address offset condition");
      }
      try {
        this.bank = parseNumber(parts[1]);
        this.address = parseNumber(parts[2]);
        this.offset = parseNumber(parts[3]);
        this.condition = parseNumber(parts[4]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid numbers in MemoryAdd: " + e.getMessage());
      }
    }

    public int getBank() { return bank; }
    public int getAddress() { return address; }
    public int getOffset() { return offset; }
    public int getCondition() { return condition; }

    @Override
    public String getInstructionType() {
      return "MEMORY_ADD";
    }

    @Override
    public String getDescription() {
      return String.format("Add 0x%02X to memory bank %d at address 0x%04X", offset, bank, address);
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      // Guardar el valor previo
      previousValue = memoryWriter.readMemory(bank, address);
      previousBank = bank;
      previousAddress = address;
      // Aplicar suma
      int newValue = (previousValue + offset) & 0xFF; // Keep as byte
      memoryWriter.writeMemory(bank, address, newValue);
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      if (previousValue != null && previousBank != null && previousAddress != null) {
        memoryWriter.writeMemory(previousBank, previousAddress, previousValue);
        // Limpiar el registro después de revertir
        previousValue = null;
        previousBank = null;
        previousAddress = null;
      }
    }
  }

  /**
   * XOR con memoria: X bank address value condition
   * Aplica XOR con un valor en la dirección de memoria
   */
  public static class MemoryXorInstruction extends PokInstruction {
    private int bank;
    private int address;
    private int value;
    private int condition;

    public MemoryXorInstruction(String raw, String[] parts) throws IllegalArgumentException {
      super(raw);
      if (parts.length < 5) {
        throw new IllegalArgumentException("MemoryXor requires: X bank address value condition");
      }
      try {
        this.bank = parseNumber(parts[1]);
        this.address = parseNumber(parts[2]);
        this.value = parseNumber(parts[3]);
        this.condition = parseNumber(parts[4]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid numbers in MemoryXor: " + e.getMessage());
      }
    }

    public int getBank() { return bank; }
    public int getAddress() { return address; }
    public int getValue() { return value; }
    public int getCondition() { return condition; }

    @Override
    public String getInstructionType() {
      return "MEMORY_XOR";
    }

    @Override
    public String getDescription() {
      return String.format("XOR memory bank %d at address 0x%04X with 0x%02X", bank, address, value);
    }

    @Override
    public void apply(EmulatorMemoryWriter memoryWriter) {
      // Guardar el valor previo
      previousValue = memoryWriter.readMemory(bank, address);
      previousBank = bank;
      previousAddress = address;
      // Aplicar XOR
      int newValue = previousValue ^ value;
      memoryWriter.writeMemory(bank, address, newValue);
    }

    @Override
    public void revert(EmulatorMemoryWriter memoryWriter) {
      if (previousValue != null && previousBank != null && previousAddress != null) {
        memoryWriter.writeMemory(previousBank, previousAddress, previousValue);
        // Limpiar el registro después de revertir
        previousValue = null;
        previousBank = null;
        previousAddress = null;
      }
    }
  }

  /**
   * Parsea un número que puede estar en decimal u hexadecimal
   */
  protected static int parseNumber(String num) throws NumberFormatException {
    if (num.startsWith("0x") || num.startsWith("0X")) {
      return Integer.parseInt(num.substring(2), 16);
    } else if (num.startsWith("$")) {
      return Integer.parseInt(num.substring(1), 16);
    } else {
      return Integer.parseInt(num);
    }
  }
}
