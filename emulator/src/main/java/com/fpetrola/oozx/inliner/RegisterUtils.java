package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;
import java.util.Set;

/**
 * Utilidades centralizadas para manipulación de registros Z80.
 * Contiene lógica reutilizable para validación y extracción de registros.
 */
public class RegisterUtils {

  /**
   * Registros de 16 bits compuestos que tienen getters/setters (BC, DE, HL, AF)
   */
  public static final Set<String> COMPOSITE_16BIT_REGISTERS = 
    Set.of("BC", "DE", "HL", "AF");

  /**
   * Verifica si un registro es de 16 bits compuesto que tiene getters/setters
   */
  public static boolean is16BitCompositeRegister(String regName) {
    return COMPOSITE_16BIT_REGISTERS.contains(regName);
  }

  /**
   * Extrae el nombre del registro desde una referencia de opcode
   */
  public static String getRegisterName(ImmutableOpcodeReference ref) {
    if (ref instanceof Register reg) {
      return reg.getName();
    }
    return "register";
  }

  /**
   * Obtiene el nombre del getter para un registro de 16 bits compuesto
   * Ej: "BC" -> "getBC"
   */
  public static String getCompositeRegisterGetterName(String regName) {
    if (is16BitCompositeRegister(regName)) {
      return "get" + regName;
    }
    throw new IllegalArgumentException("No es un registro compuesto de 16 bits: " + regName);
  }

  /**
   * Obtiene el nombre del setter para un registro de 16 bits compuesto
   * Ej: "BC" -> "setBC"
   */
  public static String getCompositeRegisterSetterName(String regName) {
    if (is16BitCompositeRegister(regName)) {
      return "set" + regName;
    }
    throw new IllegalArgumentException("No es un registro compuesto de 16 bits: " + regName);
  }
}
