package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.opcodes.references.*;
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
    if (source instanceof IndirectMemory8BitReference indMem) {
      return resolveIndirectMemoryAddress(mm, indMem);
    } else if (source instanceof IndirectMemory16BitReference indMem16) {
      return resolveIndirectMemory16BitAddress(mm, indMem16);
    } else if (source instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
      return ctx.address;
    } else if (source instanceof Memory8BitReference memory8BitReference) {
      return mm.field("PC").add(memory8BitReference.getDelta()).and(0xFFFF);
    } else if (source instanceof Memory16BitReference memory16BitReference) {
      return mm.field("PC").add(memory16BitReference.getDelta()).and(0xFFFF);
    }
    return null;
  }

  /**
   * Resuelve la dirección para IndirectMemory8BitReference
   */
  public Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof com.fpetrola.z80.registers.Register reg) {
      String regName = reg.getName();
      // Si es un registro de 16 bits compuesto que tiene getters (BC, DE, HL, AF)
      if (is16BitCompositeRegister(regName)) {
        String getterMethodName = "get" + regName;
        Variable result = mm.var(int.class);
        result.set(mm.invoke(getterMethodName));
        return result;
      }
      return mm.field(regName);
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
      String regName = reg.getName();
      // Si es un registro de 16 bits compuesto que tiene getters (BC, DE, HL, AF)
      if (is16BitCompositeRegister(regName)) {
        String getterMethodName = "get" + regName;
        Variable result = mm.var(int.class);
        result.set(mm.invoke(getterMethodName));
        return result;
      }
      return mm.field(regName);
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  public MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
    Variable pcPlusDelta = mm.field("PC").add(memRef.getValueDelta()).and(0xFFFF);
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    dd.set(memory.invoke("read", pcPlusDelta, 0));

    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    // Obtener el nombre del registro de forma genérica (puede ser IX, IY, etc.)
    ImmutableOpcodeReference target = memRef.getTarget();
    String registerName = getRegisterName(target);
    Variable targetReg = mm.field(registerName);
    Variable regPlusDd = targetReg.add(dd);
    Variable address = mm.var(int.class);
    address.set(regPlusDd.and(0xFFFF));

    return new MemoryPlusRegisterContext(memory, address);
  }

  /**
   * Verifica si es un registro de 16 bits compuesto que tiene getters/setters (BC, DE, HL, AF)
   */
  private boolean is16BitCompositeRegister(String regName) {
    return (regName.equals("BC") || regName.equals("DE") || regName.equals("HL") || regName.equals("AF"));
  }

  /**
   * Obtiene el nombre del registro desde una referencia
   */
  private String getRegisterName(ImmutableOpcodeReference ref) {
    if (ref instanceof com.fpetrola.z80.registers.Register reg) {
      return reg.getName();
    }
    return "register";
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
