package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.RegisterUtils;
import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Estrategia para manejar referencias de memoria más registro de 8 bits: (IX+d), (IY+d)
 */
public class MemoryPlusRegister8BitStrategy implements OpcodeReferenceStrategy {

  private final MemoryPlusRegister8BitReference reference;
  private final String registerName;

  public MemoryPlusRegister8BitStrategy(MemoryPlusRegister8BitReference reference) {
    this.reference = reference;
    this.registerName = RegisterUtils.getRegisterName(reference.getTarget());
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // (IX+d), (IY+d) - requiere offset
    // Calcular: (registerValue + offset) & 0xFFFF
    Variable pcPlusDelta = mm.field("PC").and(0xFFFF);
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    dd.set(memory.invoke("read", pcPlusDelta, 0));

    // Obtener el valor del registro
    Variable targetReg = mm.field(registerName);
    Variable regPlusDd = targetReg.add(dd);
    Variable address = mm.var(int.class);
    address.set(regPlusDd.and(0xFFFF));
    
    return address;
  }

  @Override
  public String generateNameSuffix() {
    return "Mprf" + RegisterUtils.capitalizeFirstLetter(registerName);
  }

  @Override
  public String getClassName(String operationName) {
    return "Execute" + operationName + generateNameSuffix();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    Variable result = mm.var(int.class);
    result.set(memory.invoke("read", address, 0));
    return result;
  }

  @Override
  public String getReferencetype() {
    return "MemoryPlusRegister8Bit[" + registerName + "]";
  }
}
