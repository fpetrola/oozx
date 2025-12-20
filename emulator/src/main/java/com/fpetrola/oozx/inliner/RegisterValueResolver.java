package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la resolución y asignación de valores de registros,
 * incluyendo registros de 16 bits compuestos (BC, DE, HL, AF).
 */
public class RegisterValueResolver {

  /**
   * Resuelve el valor de un registro, manejando registros de 16 bits construidos a partir de 8 bits
   */
  public Variable resolveRegisterValue(MethodMaker mm, Register reg) {
    String regName = reg.getName();
    return resolveRegisterValueByName(mm, regName);
  }

  /**
   * Resuelve el valor de un registro por su nombre, manejando registros de 16 bits usando los getters de UnrolledRegisterBank
   */
  public Variable resolveRegisterValueByName(MethodMaker mm, String regName) {
    // Si es un registro de 16 bits compuesto que tiene getters (BC, DE, HL, AF)
    if (RegisterUtils.is16BitCompositeRegister(regName)) {
      String getterMethodName = RegisterUtils.getCompositeRegisterGetterName(regName);
      Variable result = mm.var(int.class);
      result.set(mm.invoke(getterMethodName));
      return result;
    }

    // Para otros registros (A, F, I, R, IX, IY, SP, PC, etc.), acceder directamente
    return mm.field(regName);
  }

  /**
   * Asigna un valor a un registro, manejando registros de 16 bits compuestos (BC, DE, HL, AF)
   */
  public void assignRegisterValue(MethodMaker mm, String regName, Variable value) {
    if (RegisterUtils.is16BitCompositeRegister(regName)) {
      // Para registros de 16 bits compuestos, usar el setter correspondiente
      String setterMethodName = RegisterUtils.getCompositeRegisterSetterName(regName);
      mm.invoke(setterMethodName, value);
    } else {
      // Para registros de 8 bits o especiales, asignar directamente
      mm.field(regName).set(value);
    }
  }
}
