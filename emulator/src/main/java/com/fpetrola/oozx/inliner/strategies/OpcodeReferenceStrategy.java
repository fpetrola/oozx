package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.oozx.inliner.MemoryAccessHandler;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;
import java.util.List;

/**
 * Estrategia para manejar un tipo específico de referencia de opcode.
 * Encapsula toda la lógica relacionada con ese tipo en un único lugar.
 */
public interface OpcodeReferenceStrategy {

  /**
   * Resuelve la dirección de memoria para esta referencia
   */
  Variable resolveAddress(MethodMaker mm, MemoryAccessHandler memoryAccessHandler);

  /**
   * Genera el sufijo para el nombre del método (ej: "A", "Mprf", "Imr")
   */
  String generateNameSuffix();

  /**
   * Obtiene el nombre de la clase para código generado
   */
  String getClassName(String operationName);

  /**
   * Obtiene los parámetros del constructor en orden
   */
  List<String> getConstructorParameters();

  /**
   * Lee el valor desde memoria según el tipo de referencia
   * (8-bit vs 16-bit)
   */
  Variable readValue(MethodMaker mm, Variable address, Variable memory);

  /**
   * Tipo de la referencia (para logging/debugging)
   */
  String getReferencetype();
}
