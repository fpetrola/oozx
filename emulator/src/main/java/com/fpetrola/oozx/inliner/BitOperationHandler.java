package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.RES;
import com.fpetrola.z80.instructions.impl.SET;
import com.fpetrola.z80.instructions.impl.BIT;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Handler para instrucciones de bit (BIT, RES, SET).
 * Estas instrucciones manipulan bits individuales de un valor.
 */
public class BitOperationHandler {

  private final RegisterValueResolver registerValueResolver;

  public BitOperationHandler(RegisterValueResolver registerValueResolver) {
    this.registerValueResolver = registerValueResolver;
  }

  /**
   * Genera código para instrucciones de bit (RES, SET).
   * RES: limpia un bit (target & ~(1 << n))
   * SET: establece un bit (target | (1 << n))
   */
  public void executeBitOperation(MethodMaker mm, BitOperation instruction) {
    OpcodeReference target = instruction.getTarget();
    int bitPosition = instruction.getN();

    // Leer el valor actual del target
    Variable currentValue = resolveTargetValue(mm, target);

    // Aplicar operación según el tipo de instrucción
    Variable newValue = applyBitOperation(mm, instruction, currentValue, bitPosition);

    // Escribir el nuevo valor de vuelta al target
    writeTargetValue(mm, target, newValue);
  }

  private Variable resolveTargetValue(MethodMaker mm, OpcodeReference target) {
    if (target instanceof Register reg) {
      return registerValueResolver.resolveRegisterValueByName(mm, reg.getName());
    }
    // Para referencias de memoria, sería más complejo; por ahora solo soportamos registros
    throw new UnsupportedOperationException("Target no soportado para BitOperation: " + target.getClass().getSimpleName());
  }

  private Variable applyBitOperation(MethodMaker mm, BitOperation instruction, Variable value, int bitPosition) {
    Variable result = mm.var(int.class);

    if (instruction instanceof RES) {
      // RES: limpia el bit (AND con máscara invertida)
      // target = (target & ~(1 << n)) & 0xFFFF
      int mask = 1 << bitPosition;
      result.set(value.and(~mask).and(0xFFFF));
    } else if (instruction instanceof SET) {
      // SET: establece el bit (OR con máscara)
      // target = (target | (1 << n)) & 0xFFFF
      int mask = 1 << bitPosition;
      result.set(value.or(mask).and(0xFFFF));
    } else if (instruction instanceof BIT) {
      // BIT: solo verifica el bit (no modifica el valor)
      // Para simplificar, devolvemos el valor sin cambios
      // El manejo de flags sería más complejo
      result.set(value);
    } else {
      throw new UnsupportedOperationException("Instrucción BitOperation no soportada: " + instruction.getClass().getSimpleName());
    }

    return result;
  }

  private void writeTargetValue(MethodMaker mm, OpcodeReference target, Variable value) {
    if (target instanceof Register reg) {
      registerValueResolver.assignRegisterValue(mm, reg.getName(), value);
    } else {
      throw new UnsupportedOperationException("No se puede escribir a: " + target.getClass().getSimpleName());
    }
  }
}
