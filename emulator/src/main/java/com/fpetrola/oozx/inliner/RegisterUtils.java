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
    * Registros de 16 bits compuestos que tienen getters/setters en UnrolledRegisterBank (BC, DE, HL, AF)
    */
  public static final Set<String> COMPOSITE_16BIT_REGISTERS = 
    Set.of("BC", "DE", "HL", "AF");

  /**
   * Registros de 16 bits que son campos directos en UnrolledRegisterBank (IX, IY, PC, SP, I, R, MEMPTR)
   */
  public static final Set<String> DIRECT_FIELD_REGISTERS = 
    Set.of("IX", "IY", "PC", "SP", "I", "R", "MEMPTR");

  /**
   * Registros de 8 bits que son parte de registros de 16 bits
   * IXH, IXL -> IX
   * IYH, IYL -> IY
   */
  public static final java.util.Map<String, String> REGISTER_MAPPING = 
    java.util.Map.ofEntries(
      java.util.Map.entry("IXH", "IX"),
      java.util.Map.entry("IXL", "IX"),
      java.util.Map.entry("IYH", "IY"),
      java.util.Map.entry("IYL", "IY")
    );

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

  /**
   * Verifica si un registro es un campo directo en UnrolledRegisterBank
   */
  public static boolean isDirectFieldRegister(String regName) {
    return DIRECT_FIELD_REGISTERS.contains(regName);
  }

  /**
   * Verifica si un registro es un sub-registro de 8 bits de un registro de 16 bits
   */
  public static boolean isSubRegister(String regName) {
    return REGISTER_MAPPING.containsKey(regName);
  }

  /**
   * Obtiene el registro padre de un sub-registro
   * Ej: "IYH" -> "IY", "IXL" -> "IX"
   */
  public static String getParentRegister(String regName) {
    return REGISTER_MAPPING.get(regName);
  }

  /**
   * Verifica si un registro es el byte alto (H) de un registro de 16 bits
   */
  public static boolean isHighByte(String regName) {
    return regName.endsWith("H") && REGISTER_MAPPING.containsKey(regName);
  }

  /**
   * Capitaliza la primera letra de una cadena
   * Preserva el caso del resto si es una constante numérica (como M16R)
   */
  public static String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    // Si la cadena tiene dígitos (como M16R), devolverla tal cual
    if (str.matches(".*\\d.*")) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
  }
