package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;
import java.util.List;

/**
 * Estrategia para manejar referencias de memoria de 16 bits: (PC + delta)
 */
public class Memory16BitStrategy implements OpcodeReferenceStrategy {

  private final Memory16BitReference reference;

  public Memory16BitStrategy(Memory16BitReference reference) {
    this.reference = reference;
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    return mm.field("PC").add(reference.getDelta()).and(0xFFFF);
  }

  @Override
  public String generateNameSuffix() {
    return "M16R";
  }

  @Override
  public String getClassName(String operationName) {
    return "Execute" + operationName + "M16R";
  }

  @Override
  public List<String> getConstructorParameters() {
    return List.of();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    Variable result = mm.var(int.class);
    result.set(memory.invoke("read16Bits", address));
    return result;
  }

  @Override
  public String getReferencetype() {
    return "Memory16Bit[delta=" + reference.getDelta() + "]";
  }
}
