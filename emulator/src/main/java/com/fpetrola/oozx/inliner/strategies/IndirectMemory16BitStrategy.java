package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.RegisterUtils;
import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;
import java.util.List;

/**
 * Estrategia para manejar referencias de memoria indirecta de 16 bits: (HL), (BC), (DE), etc.
 */
public class IndirectMemory16BitStrategy implements OpcodeReferenceStrategy {

  private final IndirectMemory16BitReference reference;
  private final ImmutableOpcodeReference innerTarget;

  public IndirectMemory16BitStrategy(IndirectMemory16BitReference reference) {
    this.reference = reference;
    this.innerTarget = reference.getTarget();
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // Resuelve la dirección desde el registro o memoria
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
    if (innerTarget instanceof Register reg) {
      return "Imr16" + RegisterUtils.capitalizeFirstLetter(reg.getName());
    } else if (innerTarget instanceof Memory16BitReference) {
      return "Imr16M16R";
    }
    return "Imr16Unknown";
  }

  @Override
  public String getClassName(String operationName) {
    return "Execute" + operationName + generateNameSuffix();
  }

  @Override
  public List<String> getConstructorParameters() {
    return List.of();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    // Leer 16 bits desde memoria[address]
    Variable result = mm.var(int.class);
    result.set(memory.invoke("read16Bits", address));
    return result;
  }

  @Override
  public String getReferencetype() {
    if (innerTarget instanceof Register reg) {
      return "IndirectMemory16Bit[" + reg.getName() + "]";
    } else if (innerTarget instanceof Memory16BitReference) {
      return "IndirectMemory16Bit[M16R]";
    }
    return "IndirectMemory16Bit[Unknown]";
  }
}
