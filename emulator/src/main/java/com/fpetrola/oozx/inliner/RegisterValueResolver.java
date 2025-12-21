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
    // Si es un sub-registro de 8 bits (IYH, IYL, IXH, IXL)
    if (RegisterUtils.isSubRegister(regName)) {
      String parentReg = RegisterUtils.getParentRegister(regName);
      
      Variable parentValue = mm.var(int.class);
      if (RegisterUtils.isDirectFieldRegister(parentReg)) {
        // IX, IY, PC, SP, etc. son campos directos
        parentValue.set(mm.field(parentReg));
      } else {
        // BC, DE, HL, AF usan getters normales
        String getterMethodName = RegisterUtils.getCompositeRegisterGetterName(parentReg);
        parentValue.set(mm.invoke(getterMethodName));
      }
      
      // Extraer el byte alto (H) o bajo (L)
      Variable result = mm.var(int.class);
      if (RegisterUtils.isHighByte(regName)) {
        // Byte alto: (value >> 8) & 0xFF
        result.set(parentValue.shr(8).and(0xFF));
      } else {
        // Byte bajo: value & 0xFF
        result.set(parentValue.and(0xFF));
      }
      return result;
    }
    
    // Si es un registro de 16 bits compuesto que tiene getters (BC, DE, HL, AF)
    if (RegisterUtils.is16BitCompositeRegister(regName)) {
      String getterMethodName = RegisterUtils.getCompositeRegisterGetterName(regName);
      Variable result = mm.var(int.class);
      result.set(mm.invoke(getterMethodName));
      return result;
    }

    // Para otros registros (A, F, IX, IY, PC, SP, I, R, MEMPTR, etc.), acceder directamente
    return mm.field(regName);
  }

  /**
    * Asigna un valor a un registro, manejando registros de 16 bits compuestos (BC, DE, HL, AF) y campos directos (IX, IY, PC, SP, I, R)
    */
  public void assignRegisterValue(MethodMaker mm, String regName, Variable value) {
    // Si es un sub-registro de 8 bits (IYH, IYL, IXH, IXL)
    if (RegisterUtils.isSubRegister(regName)) {
      String parentReg = RegisterUtils.getParentRegister(regName);
      
      Variable parentValue = mm.var(int.class);
      
      if (RegisterUtils.isDirectFieldRegister(parentReg)) {
        // IX, IY, PC, SP, etc. son campos directos
        parentValue.set(mm.field(parentReg));
      } else {
        // BC, DE, HL, AF usan getters normales
        String getterMethodName = RegisterUtils.getCompositeRegisterGetterName(parentReg);
        parentValue.set(mm.invoke(getterMethodName));
      }
      
      // Modificar el byte correspondiente
      Variable newValue = mm.var(int.class);
      if (RegisterUtils.isHighByte(regName)) {
        // Byte alto: (parentValue & 0xFF) | (value << 8)
        newValue.set(parentValue.and(0xFF).or(value.and(0xFF).shl(8)));
      } else {
        // Byte bajo: (parentValue & 0xFF00) | (value & 0xFF)
        newValue.set(parentValue.and(0xFF00).or(value.and(0xFF)));
      }
      
      // Asignar el nuevo valor al registro padre
      if (RegisterUtils.isDirectFieldRegister(parentReg)) {
        mm.field(parentReg).set(newValue);
      } else {
        String setterMethodName = RegisterUtils.getCompositeRegisterSetterName(parentReg);
        mm.invoke(setterMethodName, newValue);
      }
    } else if (RegisterUtils.is16BitCompositeRegister(regName)) {
      // Para registros de 16 bits compuestos, usar el setter correspondiente
      String setterMethodName = RegisterUtils.getCompositeRegisterSetterName(regName);
      mm.invoke(setterMethodName, value);
    } else {
      // Para registros de 8 bits o campos directos, asignar directamente
      mm.field(regName).set(value);
    }
  }
}
