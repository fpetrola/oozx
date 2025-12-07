package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public TableEntry() {}

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
    public List<TableEntry> entries;

    public TableExport() {
      this.exportedAt = System.currentTimeMillis();
      this.entries = new ArrayList<>();
    }
  }

  /**
   * Obtiene el campo protegido 'table' de una TableAluOperation usando reflexión.
   */
  private static int[] getTableFromOperation(TableAluOperation operation) {
    try {
      java.lang.reflect.Field tableField = TableAluOperation.class.getDeclaredField("table");
      tableField.setAccessible(true);
      return (int[]) tableField.get(operation);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to access table field", e);
    }
  }

  /**
   * Exporta la tabla de una TableAluOperation a JSON.
   *
   * @param operation Instancia de TableAluOperation a exportar
   * @param operationName Nombre descriptivo (ej: "RLA", "SLA", "RLC")
   * @param outputPath Ruta del archivo JSON de salida
   */
  public static void exportToJson(
      TableAluOperation operation, String operationName, String outputPath) throws IOException {
    int[] table = getTableFromOperation(operation);

    TableExport export = new TableExport();
    export.operationName = operationName;
    export.className = operation.getClass().getName();
    export.tableSize = table.length;

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
   * Compara dos tablas y retorna las diferencias encontradas.
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

    for (int i = 0; i < Math.min(currentTable.length, savedExport.entries.size()); i++) {
      int currentValue = currentTable[i];
      TableEntry savedEntry = savedExport.entries.get(i);

      if (currentValue != savedEntry.value) {
        int currentA = currentValue >> 8;
        int currentF = currentValue & 0xFF;
        differences.add(
            String.format(
                "Mismatch at index %d: current=(A=0x%02X,F=0x%02X,val=0x%08X) vs saved=(A=0x%02X,F=0x%02X,val=0x%08X)",
                i, currentA, currentF, currentValue, savedEntry.resultA, savedEntry.resultFlags,
                savedEntry.value));
      }
    }

    return differences;
  }
}
