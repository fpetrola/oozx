package com.fpetrola.z80.registers.flag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test parametrizado que verifica la compatibilidad de TODAS las operaciones TableAluOperation.
 *
 * Lee la configuración desde table_alu_operations_config.json y automáticamente:
 * 1. Carga cada operación usando reflexión
 * 2. Compara su tabla con el JSON baseline correspondiente
 * 3. Reporta diferencias
 *
 * NO requiere código específico para cada instrucción.
 * Solo agregar la operación al JSON de configuración para que se pruebe automáticamente.
 */
@DisplayName("All Table ALU Operations - Compatibility Test")
class AllTableAluOperationsCompatibilityTest {

  /**
   * Proporciona configuraciones de todas las operaciones ALU.
   */
  static Collection<TableAluOperationRegistry.OperationConfig> provideAllOperations()
      throws IOException {
    List<TableAluOperationRegistry.OperationConfig> configs =
        TableAluOperationRegistry.getOperations();
    System.out.println("\n=== Testing " + configs.size() + " Table ALU Operations ===\n");
    return configs;
  }

  /**
   * Test parametrizado que prueba cada operación contra su baseline JSON.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllOperations")
  @DisplayName("Operation table matches baseline")
  void testOperationTableCompatibility(
      TableAluOperationRegistry.OperationConfig operationConfig) throws Exception {
    // Obtener la operación
    TableAluOperation operation = TableAluOperationRegistry.getOperation(operationConfig);
    assertNotNull(operation, "Operation " + operationConfig.name + " should not be null");

    // Generar el nombre del archivo esperado
    String baselineJsonPath =
        "src/test/resources/alu_operations/" + operationConfig.name.toLowerCase() + "_table.json";

    // Si el archivo no existe, skip con mensaje informativo
    if (!Files.exists(Paths.get(baselineJsonPath))) {
      System.out.println(
          "⊘ Skipped " + operationConfig.name + " - baseline not found: " + baselineJsonPath);
      System.out.println("  Run: mvn test -Dtest=AllTableAluOperationsCompatibilityTest#generateAllBaselines");
      return;
    }

    // Cargar baseline
    TableAluOperationExporter.TableExport savedExport =
        TableAluOperationExporter.loadFromJson(baselineJsonPath);

    // Obtener tabla actual
    int[] currentTable = TableAluOperationRegistry.getTable(operation);

    assertNotNull(currentTable, "Table for " + operationConfig.name + " should not be null");

    // Comparar
    List<String> differences =
        TableAluOperationExporter.compareTables(currentTable, savedExport);

    if (!differences.isEmpty()) {
      System.out.println(
          "✗ " + operationConfig.name + " has " + differences.size() + " differences:");
      differences.stream().limit(5).forEach(d -> System.out.println("  " + d));
      if (differences.size() > 5) {
        System.out.println("  ... and " + (differences.size() - 5) + " more");
      }
    } else {
      System.out.println("✓ " + operationConfig.name);
    }

    assertEquals(
        0,
        differences.size(),
        () ->
            operationConfig.name
                + " has "
                + differences.size()
                + " differences from baseline");
  }

//  /**
//   * Test que exporta baselines para todas las operaciones.
//   * Ejecutar una sola vez: mvn test -Dtest=AllTableAluOperationsCompatibilityTest#generateAllBaselines
//   */
//  @Test
//  @DisplayName("Generate baselines for all operations")
//  void generateAllBaselines() throws Exception {
//    String outputDir = "src/test/resources/alu_operations";
//    System.out.println("\nGenerating baselines in: " + outputDir);
//
//    TableAluOperationRegistry.exportAllOperations(outputDir);
//
//    System.out.println("✓ Baselines generated successfully");
//    System.out.println("Next: mvn test -Dtest=AllTableAluOperationsCompatibilityTest");
//  }

  /**
   * Test de validación: verifica que todas las operaciones están inicializadas.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllOperations")
  @DisplayName("Operation table is initialized")
  void testOperationTableInitialized(
      TableAluOperationRegistry.OperationConfig operationConfig) throws Exception {
    TableAluOperation operation = TableAluOperationRegistry.getOperation(operationConfig);
    int[] table = TableAluOperationRegistry.getTable(operation);

    assertTrue(table.length > 0, operationConfig.name + " table should not be empty");

    long nonZeroCount = java.util.Arrays.stream(table).filter(v -> v != 0).count();
    assertTrue(
        nonZeroCount > 0,
        operationConfig.name + " table should have non-zero values, found: " + nonZeroCount);
  }

  /**
   * Test informativo: muestra qué operaciones tienen baselines y cuáles no.
   */
  @Test
  @DisplayName("Baseline status report")
  void baselineStatusReport() throws IOException {
    List<TableAluOperationRegistry.OperationConfig> configs =
        TableAluOperationRegistry.getOperations();
    int withBaseline = 0;
    int withoutBaseline = 0;

    System.out.println("\n=== Baseline Status Report ===\n");

    for (TableAluOperationRegistry.OperationConfig config : configs) {
      String path = "src/test/resources/alu_operations/" + config.name.toLowerCase() + "_table.json";
      boolean exists = Files.exists(Paths.get(path));
      if (exists) {
        withBaseline++;
        System.out.println("✓ " + config.name);
      } else {
        withoutBaseline++;
        System.out.println("⊘ " + config.name);
      }
    }

    System.out.println(
        "\nTotal: " + configs.size() + " | With baseline: " + withBaseline + " | Without: " + withoutBaseline);

    if (withoutBaseline > 0) {
      System.out.println("\nTo generate missing baselines:");
      System.out.println("  mvn test -Dtest=AllTableAluOperationsCompatibilityTest#generateAllBaselines");
    }
  }
}
