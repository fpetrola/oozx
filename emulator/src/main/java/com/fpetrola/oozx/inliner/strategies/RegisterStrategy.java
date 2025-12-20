package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.RegisterUtils;
import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Estrategia para manejar referencias de registros (A, B, C, etc.)
 */
public class RegisterStrategy implements OpcodeReferenceStrategy {

  private final Register register;

  public RegisterStrategy(Register register) {
    this.register = register;
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // Los registros no tienen "dirección" por sí solos
    // Retornar el valor del registro
    String regName = register.getName();
    if (RegisterUtils.is16BitCompositeRegister(regName)) {
      String getter = RegisterUtils.getCompositeRegisterGetterName(regName);
      Variable result = mm.var(int.class);
      result.set(mm.invoke(getter));
      return result;
    }
    return mm.field(regName);
  }

  @Override
  public String generateNameSuffix() {
    // "A" → "A", "BC" → "Bc"
    return RegisterUtils.capitalizeFirstLetter(register.getName());
  }

  @Override
  public String getClassName(String operationName) {
    // ExecuteLdA, ExecuteXorB, etc.
    return "Execute" + operationName + generateNameSuffix();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    // Los registros no leen de memoria
    throw new UnsupportedOperationException("Los registros no leen de memoria");
  }

  @Override
  public String getReferencetype() {
    return "Register:" + register.getName();
  }
}
