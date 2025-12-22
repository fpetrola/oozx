package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import com.fpetrola.z80.opcodes.references.ConstantOpcodeReference;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Estrategia para manejar referencias constantes (immediate values)
 */
public class ConstantOpcodeStrategy implements OpcodeReferenceStrategy {

  private final ConstantOpcodeReference constant;

  public ConstantOpcodeStrategy(ConstantOpcodeReference constant) {
    this.constant = constant;
  }

  @Override
  public Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler) {
    // Las constantes no tienen dirección
    throw new UnsupportedOperationException("Las constantes no tienen dirección");
  }

  @Override
  public String generateNameSuffix() {
    // "0x06" o similar
    return "C" + constant.read();
  }

  @Override
  public String getClassName(String operationName) {
    // ExecuteLdC6, ExecuteXorC20, etc.
    return "Execute" + operationName + generateNameSuffix();
  }

  @Override
  public Variable readValue(MethodMaker mm, Variable address, Variable memory) {
    // Las constantes no leen de memoria
    throw new UnsupportedOperationException("Las constantes no leen de memoria");
  }

  @Override
  public String getReferencetype() {
    return "Constant:" + constant.read();
  }
}
