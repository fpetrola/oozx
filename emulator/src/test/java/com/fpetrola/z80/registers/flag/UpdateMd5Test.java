package com.fpetrola.z80.registers.flag;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Test que actualiza los MD5 baselines de las operaciones ALU.
 * 
 * Ejecutar con: mvn test -Dtest=UpdateMd5Test -pl emulator
 */
@Disabled
@DisplayName("Update MD5 Baselines for Table ALU Operations")
class UpdateMd5Test {

  @Test
  void updateAllMd5Hashes() throws Exception {
    TableAluOperationRegistry.updateAllMd5Hashes();
  }
}
