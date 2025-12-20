package com.fpetrola.oozx.inliner.strategies;

import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

/**
 * Factory para crear la estrategia apropiada para un tipo de referencia
 */
public class OpcodeReferenceStrategyFactory {

  public static OpcodeReferenceStrategy create(ImmutableOpcodeReference reference) {
    return switch(reference) {
      case Register reg -> 
        new RegisterStrategy(reg);
      
      case IndirectMemory8BitReference ref -> 
        new IndirectMemory8BitStrategy(ref);
      
      case IndirectMemory16BitReference ref -> 
        new IndirectMemory16BitStrategy(ref);
      
      case MemoryPlusRegister8BitReference ref -> 
        new MemoryPlusRegister8BitStrategy(ref);
      
      case Memory8BitReference ref -> 
        new Memory8BitStrategy(ref);
      
      case Memory16BitReference ref -> 
        new Memory16BitStrategy(ref);
      
      default -> 
        throw new IllegalArgumentException("Unknown reference type: " + reference.getClass().getName());
    };
  }
}
