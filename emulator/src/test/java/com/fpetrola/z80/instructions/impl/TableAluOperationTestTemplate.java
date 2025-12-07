package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.registers.flag.TableAluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperationExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template para crear tests de compatibilidad de TableAluOperation.
 *
 * CÓMO USAR:
 * 1. Copiar esta clase
 * 2. Cambiar:
 *    - Nombre: XXXTableCompatibilityTest (donde XXX es el nombre de la operación)
 *    - TABLE_EXPORT_PATH: "src/test/resources/xxx_table_export.json"
 *    - TABLE_OPERATION: XXX.xxxTableAluOperation
 *    - operationName: "XXX"
 * 3. Ejecutar exportRlaTableOnDemand() primero (crea el archivo JSON)
 * 4. Ejecutar el resto de tests para verificar compatibilidad
 */
@DisplayName("TableAluOperation Compatibility Test Template")
abstract class TableAluOperationTestTemplate {

  /**
   * Subclases deben implementar estos métodos.
   */
  protected abstract String getTableExportPath();

  protected abstract TableAluOperation getTableOperation();

  protected abstract String getOperationName();

  @Test
  @DisplayName("Table values match exported baseline")
  void testTableCompatibility() throws IOException {
    String path = getTableExportPath();

    // Verificar que el archivo de referencia existe
    if (!Files.exists(Paths.get(path))) {
      System.out.println("⚠ Warning: " + path + " not found. Skipping compatibility test.");
      System.out.println("  Run exportTableOnDemand() first to generate the baseline.");
      return;
    }

    // Cargar tabla de referencia
    TableAluOperationExporter.TableExport savedExport =
        TableAluOperationExporter.loadFromJson(path);

    // Obtener tabla actual usando reflexión
    java.lang.reflect.Field tableField;
    try {
      tableField = getTableOperation().getClass().getSuperclass().getDeclaredField("table");
      tableField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to access table field", e);
    }

    int[] currentTable;
    try {
      currentTable = (int[]) tableField.get(getTableOperation());
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to get table value", e);
    }

    assertNotNull(currentTable, "Table should not be null");

    // Comparar tablas
    List<String> differences =
        TableAluOperationExporter.compareTables(currentTable, savedExport);

    if (!differences.isEmpty()) {
      System.out.println("Found " + differences.size() + " differences:");
      differences.forEach(System.out::println);
    }

    assertEquals(
        0,
        differences.size(),
        () ->
            "Table has "
                + differences.size()
                + " differences from baseline:\n"
                + String.join("\n", differences));
  }

  @Test
  @DisplayName("Table is properly initialized")
  void testTableInitialized() throws NoSuchFieldException, IllegalAccessException {
    java.lang.reflect.Field tableField =
        getTableOperation().getClass().getSuperclass().getDeclaredField("table");
    tableField.setAccessible(true);

    int[] table = (int[]) tableField.get(getTableOperation());

    assertTrue(table.length > 0, "Table should not be empty");

    // Verificar que contiene valores
    long nonZeroCount =
        java.util.Arrays.stream(table).filter(v -> v != 0).count();
    assertTrue(
        nonZeroCount > (table.length * 0.5),
        "Table should have many non-zero values, found: " + nonZeroCount + " out of " + table.length);
  }

  @Test
  @DisplayName("Export table on demand")
  void exportTableOnDemand() throws IOException {
    System.out.println("Exporting current " + getOperationName() + " table...");
    String outputPath = getTableExportPath();
    TableAluOperationExporter.exportToJson(
        getTableOperation(), getOperationName(), outputPath);

    // Verificar que el archivo se creó
    assertTrue(Files.exists(Paths.get(outputPath)), "Export file should be created");

    // Cargar y mostrar info
    TableAluOperationExporter.TableExport export =
        TableAluOperationExporter.loadFromJson(outputPath);
    System.out.println(
        "Exported " + export.operationName + " with " + export.entries.size() + " entries");
    System.out.println("First 5 entries:");
    export.entries.stream().limit(5).forEach(e -> System.out.println("  " + e));
  }
}
