package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Ex;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Handler para la instrucción EX que intercambia valores entre dos referencias (registros o memoria).
 */
public class ExOperationHandler {
  
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;

  public ExOperationHandler(RegisterValueResolver registerValueResolver, MemoryAccessHandler memoryAccessHandler) {
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
  }

  /**
   * Genera bytecode para la instrucción EX (Exchange/Swap)
   * EX intercambia el contenido de dos referencias
   */
  public void executeEx(MethodMaker mm, Ex instruction) {
    OpcodeReference target = instruction.getTarget();
    OpcodeReference source = instruction.getSource();

    // Caso 1: Ambos son registros
    if (target instanceof Register targetReg && source instanceof Register sourceReg) {
      String targetName = targetReg.getName();
      String sourceName = sourceReg.getName();
      executeRegisterToRegisterExchange(mm, targetName, sourceName);
      return;
    }

    // Caso 2: Target es registro, source es memoria (HL, (SP) por ejemplo)
    if (target instanceof Register targetReg && source instanceof IndirectMemory16BitReference indMem16) {
      String targetName = targetReg.getName();
      executeRegisterToMemoryExchange16Bit(mm, targetName, indMem16);
      return;
    }

    // Caso 3: Target es registro, source es memoria indirecta 8 bits
    if (target instanceof Register targetReg && source instanceof IndirectMemory8BitReference indMem) {
      String targetName = targetReg.getName();
      executeRegisterToMemoryExchange8Bit(mm, targetName, indMem);
      return;
    }

    // Caso 4: Memoria a memoria (ej: (HL), (DE))
    if (source instanceof IndirectMemory16BitReference sourceMem && target instanceof IndirectMemory16BitReference targetMem) {
      executeMemoryToMemoryExchange16Bit(mm, targetMem, sourceMem);
      return;
    }

    if (source instanceof Register register && target instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
      Variable memory = mm.field("memory");
      Variable targetAddr = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm, indirectMemory16BitReference);
      Variable sourceAddr = memoryAccessHandler.resolveSourceMemoryAddress(mm, register);

      // temp = memory[targetAddr]
      Variable temp = mm.var(int.class);
      temp.set(memory.invoke("read16Bits", targetAddr));

      memory.invoke("write16Bits", targetAddr, sourceAddr);

      registerValueResolver.assignRegisterValue(mm, register.getName(), temp);
      return;
    }


    throw new UnsupportedOperationException("EX entre " + target.getClass().getSimpleName() + " y " + 
                                           source.getClass().getSimpleName() + " no soportado");
  }

  /**
   * Intercambia dos registros: LD r1, r2; LD r2, r1 (pero con temp)
   */
  private void executeRegisterToRegisterExchange(MethodMaker mm, String targetName, String sourceName) {
    // temp = target
    Variable temp = mm.var(int.class);
    temp.set(registerValueResolver.resolveRegisterValueByName(mm, targetName));
    
    // target = source
    Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm, sourceName);
    registerValueResolver.assignRegisterValue(mm, targetName, sourceValue);
    
    // source = temp
    registerValueResolver.assignRegisterValue(mm, sourceName, temp);
  }

  /**
   * Intercambia un registro con memoria (16 bits)
   * Ej: EX HL, (SP) - intercambia HL con los 2 bytes en (SP)
   */
  private void executeRegisterToMemoryExchange16Bit(MethodMaker mm, String registerName, 
                                                     IndirectMemory16BitReference memRef) {
    Variable memory = mm.field("memory");
    Variable address = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm, memRef);
    
    // temp = memory[address]
    Variable temp = mm.var(int.class);
    temp.set(memory.invoke("read16Bits", address));
    
    // memory[address] = register
    Variable registerValue = registerValueResolver.resolveRegisterValueByName(mm, registerName);
    memory.invoke("write16Bits", address, registerValue);
    
    // register = temp
    registerValueResolver.assignRegisterValue(mm, registerName, temp);
  }

  /**
   * Intercambia un registro con memoria (8 bits)
   */
  private void executeRegisterToMemoryExchange8Bit(MethodMaker mm, String registerName, 
                                                    IndirectMemory8BitReference memRef) {
    Variable memory = mm.field("memory");
    Variable address = memoryAccessHandler.resolveIndirectMemoryAddress(mm, memRef);
    
    // temp = memory[address]
    Variable temp = mm.var(int.class);
    temp.set(memory.invoke("read", address, 0));
    
    // memory[address] = register
    Variable registerValue = registerValueResolver.resolveRegisterValueByName(mm, registerName);
    memory.invoke("write", address, registerValue);
    
    // register = temp
    registerValueResolver.assignRegisterValue(mm, registerName, temp);
  }

  /**
   * Intercambia dos áreas de memoria (16 bits cada una)
   * Ej: EX (HL), (DE) - intercambia 2 bytes en HL con 2 bytes en DE
   */
  private void executeMemoryToMemoryExchange16Bit(MethodMaker mm, 
                                                   IndirectMemory16BitReference targetMem,
                                                   IndirectMemory16BitReference sourceMem) {
    Variable memory = mm.field("memory");
    Variable targetAddr = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm, targetMem);
    Variable sourceAddr = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm, sourceMem);
    
    // temp = memory[targetAddr]
    Variable temp = mm.var(int.class);
    temp.set(memory.invoke("read16Bits", targetAddr));
    
    // memory[targetAddr] = memory[sourceAddr]
    Variable sourceValue = memory.invoke("read16Bits", sourceAddr);
    memory.invoke("write16Bits", targetAddr, sourceValue);
    
    // memory[sourceAddr] = temp
    memory.invoke("write16Bits", sourceAddr, temp);
  }
}
