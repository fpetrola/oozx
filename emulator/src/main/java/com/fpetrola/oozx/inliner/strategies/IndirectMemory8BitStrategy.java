package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.RegisterUtils;
import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Estrategia para manejar referencias de memoria indirecta de 8 bits: (HL), (BC), (DE), etc.
 */
public class IndirectMemory8BitStrategy implements OpcodeReferenceStrategy {

  private final IndirectMemory8BitReference reference;
  private final ImmutableOpcodeReference innerTarget;

  public IndirectMemory8BitStrategy(IndirectMemory8BitReference reference) {
    this.reference = reference;
    this.innerTarget = reference.getTarget();
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // (HL), (BC), (DE), etc. - delegar al handler
    if (innerTarget instanceof Register reg) {
      String regName = reg.getName();
      if (RegisterUtils.is16BitCompositeRegister(regName)) {
        String getter = RegisterUtils.getCompositeRegisterGetterName(regName);
        Variable result = mm.var(int.class);
        result.set(mm.invoke(getter));
        return result;
      }
      return mm.field(regName);
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return memoryAccessHandler.readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  @Override
  public String generateNameSuffix() {
    // "Imr" + "Hl", "Imr" + "Bc", "Imr" + "M16R", etc.
    if (innerTarget instanceof Register reg) {
      return "Imr" + RegisterUtils.capitalizeFirstLetter(reg.getName());
    } else if (innerTarget instanceof Memory16BitReference) {
      return "ImrM16R";
    }
    return "ImrUnknown";
  }

  @Override
  public String getClassName(String operationName) {
    return "Execute" + operationName + generateNameSuffix();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    // Leer 8 bits desde memoria[address]
    Variable result = mm.var(int.class);
    result.set(memory.invoke("read", address, 0));
    return result;
  }

  @Override
  public String getReferencetype() {
    if (innerTarget instanceof Register reg) {
      return "IndirectMemory8Bit[" + reg.getName() + "]";
    } else if (innerTarget instanceof Memory16BitReference) {
      return "IndirectMemory8Bit[M16R]";
    }
    return "IndirectMemory8Bit[Unknown]";
  }
}
