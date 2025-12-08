package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Exporta la tabla int[] de una TableAluOperation a JSON para verificación de compatibilidad.
 * Útil para asegurar que una operación ALU no cambia su comportamiento después de modificaciones.
 */
public class TableAluOperationExporter {

  public static class TableEntry {
    public int index;
    public int value;
    public int resultA;
    public int resultFlags;

    public TableEntry() {
    }

    public TableEntry(int index, int value, int resultA, int resultFlags) {
      this.index = index;
      this.value = value;
      this.resultA = resultA;
      this.resultFlags = resultFlags;
    }

    @Override
    public String toString() {
      return String.format(
          "idx=%3d: val=0x%08X (A=0x%02X, F=0x%02X)", index, value, resultA, resultFlags);
    }
  }

  public static class TableExport {
    public String operationName;
    public String className;
    public long exportedAt;
    public int tableSize;
    public String tableMd5;
    public List<TableEntry> entries;

    public TableExport() {
      this.exportedAt = System.currentTimeMillis();
      this.entries = new ArrayList<>();
    }
  }

  /**
   * Calcula el MD5 de una tabla int[].
   */
  public static String calculateTableMd5(int[] table) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(intArrayToBytes(table));
      StringBuilder hexString = new StringBuilder();
      for (byte b : messageDigest) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Convierte un array int[] a bytes para cálculo de hash.
   */
  private static byte[] intArrayToBytes(int[] intArray) {
    byte[] bytes = new byte[intArray.length * 4];
    for (int i = 0; i < intArray.length; i++) {
      bytes[i * 4] = (byte) ((intArray[i] >> 24) & 0xff);
      bytes[i * 4 + 1] = (byte) ((intArray[i] >> 16) & 0xff);
      bytes[i * 4 + 2] = (byte) ((intArray[i] >> 8) & 0xff);
      bytes[i * 4 + 3] = (byte) (intArray[i] & 0xff);
    }
    return bytes;
  }

  /**
   * Exporta la tabla de una TableAluOperation a JSON.
   *
   * @param operation Instancia de TableAluOperation a exportar
   * @param operationName Nombre descriptivo (ej: "RLA", "SLA", "RLC")
   * @param outputPath Ruta del archivo JSON de salida
   */
  public static void exportToJson(
      AluOperation operation, String operationName, String outputPath) throws IOException {
    int[] table = TableAluOperationRegistry.getTable(operation);

    TableExport export = new TableExport();
    export.operationName = operationName;
    export.className = operation.getClass().getName();
    export.tableSize = table.length;
    export.tableMd5 = calculateTableMd5(table);

    // Convertir cada entrada de la tabla
    for (int i = 0; i < table.length; i++) {
      int tableValue = table[i];
      int resultA = tableValue >> 8;
      int resultFlags = tableValue & 0xFF;
      export.entries.add(new TableEntry(i, tableValue, resultA, resultFlags));
    }

    // Guardar como JSON
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
    Files.write(Paths.get(outputPath), json.getBytes());

    System.out.println("✓ Exported " + operationName + " table (" + table.length + " entries)");
    System.out.println("  MD5: " + export.tableMd5);
    System.out.println("  Saved to: " + outputPath);
  }

  /**
   * Carga una tabla previamente exportada desde JSON.
   */
  public static TableExport loadFromJson(String inputPath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get(inputPath)));
    return mapper.readValue(json, TableExport.class);
  }

  /**
   * Compara dos tablas usando MD5 (rápido) y retorna las diferencias encontradas.
   */
  public static List<String> compareTables(int[] currentTable, TableExport savedExport) {
    List<String> differences = new ArrayList<>();

    if (currentTable.length != savedExport.tableSize) {
      differences.add(
          String.format(
              "Table size mismatch: current=%d, saved=%d",
              currentTable.length, savedExport.tableSize));
      return differences;
    }

    // Comparar usando MD5
    String currentMd5 = calculateTableMd5(currentTable);
    if (!currentMd5.equals(savedExport.tableMd5)) {
      differences.add(
          String.format(
              "Table MD5 mismatch: current=%s, saved=%s",
              currentMd5, savedExport.tableMd5));
    }

    return differences;
  }
}
