package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Push;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la generación de bytecode para instrucciones PUSH.
 * PUSH decrementa SP en 2 y escribe un valor de 16 bits en memoria en la nueva dirección SP.
 */
public class PushOperationHandler {
  
  private final RegisterValueResolver registerValueResolver;

  public PushOperationHandler(RegisterValueResolver registerValueResolver) {
    this.registerValueResolver = registerValueResolver;
  }

  /**
   * Versión simplificada que asume el registro está disponible
   */
  public void executePushWithRegister(MethodMaker mm, String registerName) {
    Variable sp = mm.field("SP");
    Variable memory = mm.field("memory");
    Variable registerValue = registerValueResolver.resolveRegisterValueByName(mm, registerName);

    // SP = (SP - 2) & 0xFFFF
    Variable newSP = mm.var(int.class);
    newSP.set(sp.sub(2).and(0xFFFF));
    sp.set(newSP);

    // memory.write16Bits(registerValue, SP)
    memory.invoke("write16Bits", registerValue, sp);
  }
}
