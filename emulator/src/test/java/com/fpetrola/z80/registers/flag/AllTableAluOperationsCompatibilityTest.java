package com.fpetrola.z80.registers.flag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test parametrizado que verifica la compatibilidad de TODAS las operaciones TableAluOperation.
 *
 * Lee la configuración desde table_alu_operations_config.json y automáticamente:
 * 1. Carga cada operación usando reflexión
 * 2. Calcula el MD5 de la tabla
 * 3. Compara contra el MD5 baseline en la configuración
 * 4. Reporta diferencias
 *
 * NO requiere código específico para cada instrucción.
 * Solo agregar la operación al JSON de configuración para que se pruebe automáticamente.
 * 
 * Para actualizar los MD5 (cuando cambien las tablas):
 *   mvn test -Dtest=UpdateMd5Test -pl emulator
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
   * Test parametrizado que prueba cada operación contra el MD5 en la configuración.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllOperations")
  @DisplayName("Operation table matches baseline MD5")
  void testOperationTableCompatibility(
      TableAluOperationRegistry.OperationConfig operationConfig) throws Exception {
    // Obtener la operación
    TableAluOperation operation = TableAluOperationRegistry.getOperation(operationConfig);
    assertNotNull(operation, "Operation " + operationConfig.name + " should not be null");

    // Verificar que el MD5 esté configurado
    assertNotNull(operationConfig.md5, "Operation " + operationConfig.name + " should have MD5 configured");

    // Obtener tabla actual
    int[] currentTable = TableAluOperationRegistry.getTable(operation);
    assertNotNull(currentTable, "Table for " + operationConfig.name + " should not be null");

    // Calcular MD5 actual
    String currentMd5 = TableAluOperationExporter.calculateTableMd5(currentTable);

    // Comparar
    if (currentMd5.equals(operationConfig.md5)) {
      System.out.println("✓ " + operationConfig.name);
    } else {
      System.out.println("✗ " + operationConfig.name);
      System.out.println("  Expected MD5: " + operationConfig.md5);
      System.out.println("  Current MD5:  " + currentMd5);
    }

    assertEquals(
        operationConfig.md5,
        currentMd5,
        () -> operationConfig.name + " MD5 mismatch");
  }
}
