package com.fpetrola.z80.registers.flag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * REGRESSION TEST: Verifica que las tablas precalculadas de instrucciones ALU
 * no hayan sido modificadas accidentalmente.
 *
 * ======================== QUÉ ES ESTE TEST ========================
 *
 * Las operaciones TableAluOperation contienen tablas precalculadas (arrays int[])
 * con resultados precomputados para todas las combinaciones de entrada.
 * Estas tablas son críticas para el rendimiento y la compatibilidad.
 *
 * Este test asegura que:
 * 1. Las tablas en el código fuente coincidan con los valores baseline
 * 2. Ningún cambio accidental haya alterado el comportamiento
 * 3. La emulación siga siendo precisa
 *
 * ======================== CÓMO FUNCIONA ========================
 *
 * Para cada operación ALU (RLA, RRA, Add, Sub, etc.):
 * 1. Carga la clase e encuentra el campo TableAluOperation por reflection
 * 2. Obtiene su tabla int[] interna
 * 3. Calcula un MD5 de la tabla
 * 4. Compara contra el MD5 baseline del archivo de configuración
 *
 * NO requiere código específico para cada instrucción.
 * Las nuevas operaciones se descubren automáticamente.
 *
 * ======================== CUÁNDO FALLA ========================
 *
 * El test falla cuando: La tabla actual (MD5) NO coincide con la baseline
 *
 * Causas posibles:
 * ✗ Cambio accidental en el inicializador de la tabla (init method)
 * ✗ Modificación del algoritmo ALU (lógica de cálculo de flags)
 * ✗ Error en la lógica de paridad, carry, zero flags
 * ✗ Cambio en el método de conversión de tablas
 * ✗ Corrupción accidental del código
 *
 * ======================== CÓMO RESOLVER ========================
 *
 * Si el cambio es INTENCIONADO:
 *   1. Actualizar el baseline con: mvn test -Dtest=UpdateMd5Test -pl emulator
 *   2. Revisar y validar que el cambio es correcto
 *   3. Commit de los nuevos MD5s
 *
 * Si el cambio es ACCIDENTAL:
 *   1. Revisar los cambios recientes en la clase ALU
 *   2. Revertir a la versión anterior del código
 *   3. Validar con: mvn test -Dtest=AllTableAluOperationsCompatibilityTest -pl emulator
 *
 * ======================== CONFIGURACIÓN ========================
 *
 * Los baselines se almacenan en: table_alu_operations_config.json
 * Cada operación tiene: name, description, md5
 *
 * El sistema descubre automáticamente:
 * - Clase: com.fpetrola.z80.instructions.impl.{name}
 * - Campo: el campo static de tipo TableAluOperation en esa clase
 */
@DisplayName("All Table ALU Operations - Regression Test (MD5 Baseline Verification)")
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
   * Test parametrizado que verifica cada operación ALU contra su baseline MD5.
   *
   * Para cada operación:
   * 1. Carga la implementación usando reflexión
   * 2. Obtiene la tabla precalculada int[]
   * 3. Calcula su MD5
   * 4. Compara contra el baseline configurado
   *
   * FALLA si: El MD5 actual NO coincide con el baseline
   * POSIBLES CAUSAS: Ver comentarios de la clase
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllOperations")
  @DisplayName("Table matches baseline MD5")
  void testOperationTableCompatibility(
      TableAluOperationRegistry.OperationConfig operationConfig) throws Exception {

    // === STEP 1: Cargar operación ===
    TableAluOperation operation = TableAluOperationRegistry.getOperation(operationConfig);
    assertNotNull(
        operation,
        String.format(
            "[%s] No se pudo cargar la operación. Verificar:\n"
            + "  - Clase existe: com.fpetrola.z80.instructions.impl.%s\n"
            + "  - Tiene campo static de tipo TableAluOperation\n"
            + "  - Campo es accesible (public/protected static)",
            operationConfig.name, operationConfig.name));

    // === STEP 2: Verificar baseline MD5 configurado ===
    assertNotNull(
        operationConfig.md5,
        String.format(
            "[%s] Falta MD5 configurado en table_alu_operations_config.json\n"
            + "  Solución: mvn test -Dtest=UpdateMd5Test -pl emulator",
            operationConfig.name));

    // === STEP 3: Obtener tabla precalculada ===
    int[] currentTable = TableAluOperationRegistry.getTable(operation);
    assertNotNull(
        currentTable,
        String.format(
            "[%s] Tabla no inicializada.\n"
            + "  La operación no llamó a init() en su constructor.\n"
            + "  Verificar inicialización de la tabla en TableAluOperation",
            operationConfig.name));

    // === STEP 4: Calcular MD5 actual ===
    String currentMd5 = TableAluOperationExporter.calculateTableMd5(currentTable);

    // === STEP 5: Mostrar progreso ===
    if (currentMd5.equals(operationConfig.md5)) {
      System.out.println("✓ " + operationConfig.name);
    } else {
      System.out.println("✗ " + operationConfig.name + " - MD5 MISMATCH");
      System.out.println("  Expected: " + operationConfig.md5);
      System.out.println("  Current:  " + currentMd5);
    }

    // === STEP 6: Verificar MD5 ===
    assertEquals(
        operationConfig.md5,
        currentMd5,
        String.format(
            "\n[%s] TABLA MODIFICADA - MD5 No coincide\n"
            + "  Expected: %s\n"
            + "  Current:  %s\n"
            + "  Tabla tamaño: %d bytes (~%d elementos int)\n"
            + "\n  Posibles causas:\n"
            + "  ✗ Cambio en init() o lógica de cálculo de flags\n"
            + "  ✗ Error en algoritmo de paridad/carry/zero\n"
            + "  ✗ Cambio accidental en el código\n"
            + "\n  Solución si es INTENCIONADO:\n"
            + "  → mvn test -Dtest=UpdateMd5Test -pl emulator\n"
            + "\n  Solución si es ACCIDENTAL:\n"
            + "  → git diff <file>\n"
            + "  → Revertir cambios no deseados",
            operationConfig.name,
            operationConfig.md5,
            currentMd5,
            currentTable.length * 4,
            currentTable.length));
  }

  /**
   * Test informativo que explica el propósito y funcionamiento de esta suite.
   * Ejecutar para ver documentación: mvn test -Dtest=AllTableAluOperationsCompatibilityTest#displayTestPurpose -pl emulator
   */
  @Test
  @DisplayName("📋 Test Purpose Documentation")
  void displayTestPurpose() {
    String documentation = """
      ╔════════════════════════════════════════════════════════════════════════════════════╗
      ║                  TABLE ALU OPERATIONS - REGRESSION TEST SUITE                      ║
      ╚════════════════════════════════════════════════════════════════════════════════════╝

      🎯 PURPOSE:
      ───────────
      This test suite ensures that the precalculated lookup tables used by ALU operations
      have NOT been accidentally modified. These tables are critical for:
        • Performance: Avoid runtime calculation of flag results
        • Correctness: Precomputed values must match the Z80 spec exactly
        • Compatibility: Ensure emulated CPU behavior is accurate

      📊 WHAT ARE THESE TABLES:
      ────────────────────────────
      Each ALU operation (RLA, Add, Sub, etc.) contains a precalculated int[] array
      with results for ALL possible input combinations:
        • Example: RLA (Rotate Left Accumulator)
          - Input: A register (0-255) + Carry flag (0-1) = 256 * 2 = 512 combinations
          - Output: Result A + Flags (Sign, Zero, Half-carry, Parity, Carry)
          - Table size: 512 int entries

        • Example: Add (Add to Accumulator)
          - Input: A (0-255) + Value (0-255) = 256 * 256 = 65,536 combinations
          - Table size: 65,536 int entries
      """;

    System.out.println(documentation);
  }
}
