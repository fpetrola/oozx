package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.oozx.inliner.strategies.OpcodeReferenceStrategyFactory;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja todas las operaciones de acceso a memoria,
 * resolviendo direcciones desde diferentes tipos de referencias de memoria.
 */
public class MemoryAccessHandler {

  /**
   * Lee una dirección de 16 bits desde (pc + delta) en formato little-endian
   */
  public Variable readAddress16Bit(MethodMaker mm, Memory16BitReference mem16Ref) {
    Variable pc = mm.field("PC");
    Variable read16 = mm.invoke("read16", pc);
    return read16;
  }

  /**
   * Resuelve la dirección de memoria desde una referencia
   */
  public Variable resolveSourceMemoryAddress(MethodMaker mm, ImmutableOpcodeReference source) {
    var strategy = OpcodeReferenceStrategyFactory.create(source);
    return strategy.resolveAddress(mm, this);
  }

  /**
   * Resuelve la dirección para IndirectMemory8BitReference
   */
  public Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof com.fpetrola.z80.registers.Register reg) {
      return resolveRegisterValue(mm, reg.getName());
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Resuelve la dirección para IndirectMemory16BitReference
   */
  public Variable resolveIndirectMemory16BitAddress(MethodMaker mm, IndirectMemory16BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof com.fpetrola.z80.registers.Register reg) {
      return resolveRegisterValue(mm, reg.getName());
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Resuelve el valor de un registro, manejando registros de 16 bits compuestos
   */
  private Variable resolveRegisterValue(MethodMaker mm, String regName) {
    if (RegisterUtils.is16BitCompositeRegister(regName)) {
      String getterMethodName = RegisterUtils.getCompositeRegisterGetterName(regName);
      Variable result = mm.var(int.class);
      result.set(mm.invoke(getterMethodName));
      return result;
    }
    return mm.field(regName);
  }

  /**
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  public MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
     Variable pcPlusDelta = mm.field("PC").and(0xFFFF);
     Variable dd = mm.var(byte.class);
     Variable memory = mm.field("memory");
     dd.set(memory.invoke("read", pcPlusDelta, 0).cast(byte.class));

    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    // Obtener el nombre del registro de forma genérica (puede ser IX, IY, etc.)
    ImmutableOpcodeReference target = memRef.getTarget();
    String registerName = RegisterUtils.getRegisterName(target);
    Variable targetReg = mm.field(registerName);
    Variable regPlusDd = targetReg.add(dd);
    Variable address = mm.var(int.class);
    address.set(regPlusDd.and(0xFFFF));

    return new MemoryPlusRegisterContext(memory, address);
  }



  /**
   * Contexto para operaciones de MemoryPlusRegister8BitReference
   */
  public static class MemoryPlusRegisterContext {
    public final Variable memory;
    public final Variable address;

    public MemoryPlusRegisterContext(Variable memory, Variable address) {
      this.memory = memory;
      this.address = address;
    }
  }
}
