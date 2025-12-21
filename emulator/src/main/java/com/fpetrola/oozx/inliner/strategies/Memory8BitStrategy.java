package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.Memory8BitReference;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Estrategia para manejar referencias de memoria de 8 bits: (PC + delta)
 */
public class Memory8BitStrategy implements OpcodeReferenceStrategy {

  private final Memory8BitReference reference;

  public Memory8BitStrategy(Memory8BitReference reference) {
    this.reference = reference;
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // (PC + delta) & 0xFFFF
    return mm.field("PC").and(0xFFFF);
  }

  @Override
  public String generateNameSuffix() {
    return "M8R";
  }

  @Override
  public String getClassName(String operationName) {
    return "Execute" + operationName + "M8R";
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    Variable result = mm.var(int.class);
    result.set(memory.invoke("read", address, 0));
    return result;
  }

  @Override
  public String getReferencetype() {
    return "Memory8Bit[delta=" + reference.getDelta() + "]";
  }
}
