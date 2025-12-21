package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.DefaultTargetInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Handler genérico para instrucciones que solo tienen un target (como Inc16, Dec16).
 * Estructura: value = target.read(); target.write(operation(value)); 
 */
public class DefaultTargetInstructionHandler {

  private final RegisterValueResolver registerValueResolver;

  public DefaultTargetInstructionHandler(RegisterValueResolver registerValueResolver) {
    this.registerValueResolver = registerValueResolver;
  }

  /**
   * Genera código para instrucciones unarias sobre un target (Inc16, Dec16, etc.)
   * El patrón es: 
   *   value = target.read()
   *   target.write(operation(value))
   */
  public void executeDefaultTargetInstruction(MethodMaker mm, DefaultTargetInstruction instruction,
                                             String operationName) {
    OpcodeReference target = instruction.getTarget();
    
    // Leer el valor actual del target
    Variable currentValue = resolveTargetValue(mm, target);
    
    // Aplicar operación según el nombre
    Variable newValue = applyOperation(mm, operationName, currentValue);
    
    // Escribir el nuevo valor de vuelta al target
    writeTargetValue(mm, target, newValue);
  }

  private Variable resolveTargetValue(MethodMaker mm, OpcodeReference target) {
    if (target instanceof com.fpetrola.z80.registers.Register reg) {
      return registerValueResolver.resolveRegisterValueByName(mm, reg.getName());
    }
    throw new UnsupportedOperationException("Target no soportado: " + target.getClass().getSimpleName());
  }

  private Variable applyOperation(MethodMaker mm, String operationName, Variable value) {
    Variable result = mm.var(int.class);
    
    // Soportar operaciones comunes
    if (operationName.contains("Inc")) {
      result.set(value.add(1).and(0xFFFF));
    } else if (operationName.contains("Dec")) {
      result.set(value.sub(1).and(0xFFFF));
    } else {
      throw new UnsupportedOperationException("Operación no soportada: " + operationName);
    }
    
    return result;
  }

  private void writeTargetValue(MethodMaker mm, OpcodeReference target, Variable value) {
    if (target instanceof com.fpetrola.z80.registers.Register reg) {
      registerValueResolver.assignRegisterValue(mm, reg.getName(), value);
    } else {
      throw new UnsupportedOperationException("No se puede escribir a: " + target.getClass().getSimpleName());
    }
  }
}
