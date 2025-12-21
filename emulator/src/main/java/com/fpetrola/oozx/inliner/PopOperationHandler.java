package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Pop;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la generación de bytecode para instrucciones POP.
 * POP lee un valor de 16 bits desde memoria en la dirección SP e incrementa SP en 2.
 */
public class PopOperationHandler {
  
  private final RegisterValueResolver registerValueResolver;

  public PopOperationHandler(RegisterValueResolver registerValueResolver) {
    this.registerValueResolver = registerValueResolver;
  }

  /**
   * Ejecuta POP para un registro específico
   */
  public void executePopWithRegister(MethodMaker mm, String registerName) {
    Variable sp = mm.field("SP");
    Variable memory = mm.field("memory");
    
    // Leer valor de 16 bits desde SP: value = memory.read16Bits(SP)
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read16Bits", sp));
    
    // Escribir el valor al registro target
    registerValueResolver.assignRegisterValue(mm, registerName, value);
    
    // Incrementar SP por 2: SP = (SP + 2) & 0xFFFF
    Variable newSp = mm.var(int.class);
    newSp.set(sp.add(2).and(0xFFFF));
    sp.set(newSp);
  }
}
