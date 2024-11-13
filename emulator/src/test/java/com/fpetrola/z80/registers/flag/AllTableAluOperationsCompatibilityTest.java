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
 * Regression test: for each ALU operation, MD5-hashes its precomputed int[] table by reflection
 * and compares it against the baseline in {@code table_alu_operations_config.json}. Discovers
 * operations automatically (class {@code com.fpetrola.z80.instructions.impl.{name}}, its static
 * {@code TableAluOperation} field), so new operations need no test-specific code.
 *
 * <p>A failure means the table changed since the baseline was recorded. If the change is
 * intentional, regenerate it with {@code mvn test -Dtest=UpdateMd5Test -pl emulator} and commit
 * the new MD5s; otherwise it points at an accidental change to the ALU's flag-calculation logic.
 */
@DisplayName("All Table ALU Operations - Regression Test (MD5 Baseline Verification)")
class AllTableAluOperationsCompatibilityTest {

  /**
   * Every ALU operation, as a case.
   */
  static Collection<TableAluOperationRegistry.OperationConfig> provideAllOperations()
      throws IOException {
    List<TableAluOperationRegistry.OperationConfig> configs =
        TableAluOperationRegistry.getOperations();
    System.out.println("\n=== Testing " + configs.size() + " Table ALU Operations ===\n");
    return configs;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllOperations")
  @DisplayName("Table matches baseline MD5")
  void testOperationTableCompatibility(
      TableAluOperationRegistry.OperationConfig operationConfig) throws Exception {


    AluOperation operation = TableAluOperationRegistry.getOperation(operationConfig);
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

  /** Prints this suite's purpose and usage; run with
   *  {@code mvn test -Dtest=AllTableAluOperationsCompatibilityTest#displayTestPurpose -pl emulator}. */
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
