package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.registers.flag.TableAluOperationExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de compatibilidad para RLA TableAluOperation.
 *
 * Verifica que los valores generados por RLA coincidan exactamente con los valores guardados
 * en rla_table_export.json. Esto asegura que cambios en el código no introduzcan bugs silenciosos.
 *
 * Para generar el archivo JSON inicial:
 *   java -cp ... ExportRLATableMain
 */
@DisplayName("RLA Table Compatibility Test")
class RLATableCompatibilityTest {

  private static final String TABLE_EXPORT_PATH =
      "src/test/resources/rla_table_export.json";

  @Test
  @DisplayName("RLA table values match exported baseline")
  void testRlaTableCompatibility() throws IOException {
    // Verificar que el archivo de referencia existe
    if (!Files.exists(Paths.get(TABLE_EXPORT_PATH))) {
      System.out.println(
          "⚠ Warning: " + TABLE_EXPORT_PATH + " not found. Skipping compatibility test.");
      System.out.println("  To generate: java -cp ... ExportRLATableMain");
      return;
    }

    // Cargar tabla de referencia
    TableAluOperationExporter.TableExport savedExport =
        TableAluOperationExporter.loadFromJson(TABLE_EXPORT_PATH);

    // Obtener tabla actual usando reflexión
    java.lang.reflect.Field tableField =
        null;
    try {
      tableField = RLA.rlaTableAluOperation.getClass().getSuperclass().getDeclaredField("table");
      tableField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to access table field from RLA", e);
    }

    int[] currentTable;
    try {
      currentTable = (int[]) tableField.get(RLA.rlaTableAluOperation);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to get table value from RLA", e);
    }

    assertNotNull(currentTable, "RLA table should not be null");

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
            "RLA table has "
                + differences.size()
                + " differences from baseline:\n"
                + String.join("\n", differences));
  }

  @Test
  @DisplayName("RLA table is properly initialized")
  void testRlaTableInitialized() throws NoSuchFieldException, IllegalAccessException {
    java.lang.reflect.Field tableField =
        RLA.rlaTableAluOperation.getClass().getSuperclass().getDeclaredField("table");
    tableField.setAccessible(true);

    int[] table = (int[]) tableField.get(RLA.rlaTableAluOperation);

    // RLA usa biFunction, así que tabla de 512 entradas (256 * 2)
    assertEquals(512, table.length, "RLA table should have 512 entries (256 A values × 2 carry)");

    // Verificar que la tabla contiene valores (no toda ceros)
    long nonZeroCount =
        java.util.Arrays.stream(table).filter(v -> v != 0).count();
    assertTrue(
        nonZeroCount > 400,
        "RLA table should have many non-zero values, found: " + nonZeroCount);
  }

//  @Test
//  @DisplayName("Export RLA table on demand")
//  void exportRlaTableOnDemand() throws IOException {
//    System.out.println("Exporting current RLA table...");
//    String outputPath = "src/test/resources/rla_table_export.json";
//    TableAluOperationExporter.exportToJson(RLA.rlaTableAluOperation, "RLA", outputPath);
//
//    // Verificar que el archivo se creó
//    assertTrue(Files.exists(Paths.get(outputPath)), "Export file should be created");
//
//    // Cargar y mostrar info
//    TableAluOperationExporter.TableExport export =
//        TableAluOperationExporter.loadFromJson(outputPath);
//    System.out.println(
//        "Exported " + export.operationName + " with " + export.entries.size() + " entries");
//    System.out.println("First 5 entries:");
//    export.entries.stream().limit(5).forEach(e -> System.out.println("  " + e));
//  }
}
