package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la generación de bytecode para operaciones binarias (ADD, XOR, OR, etc.).
 * Responsable de compilar todas las variantes de operaciones ALU binarias.
 */
public class BinaryOperationHandler {

  private final InstructionClassifier classifier;
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;
  private final AluOperationHandler aluOperationHandler;

  public BinaryOperationHandler(InstructionClassifier classifier,
                                RegisterValueResolver registerValueResolver,
                                MemoryAccessHandler memoryAccessHandler,
                                AluOperationHandler aluOperationHandler) {
    this.classifier = classifier;
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
    this.aluOperationHandler = aluOperationHandler;
  }

  /**
   * Ejecuta operaciones ALU entre dos registros (register-to-register)
   */
  public void executeRegisterToRegisterAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                                    String sourceRegName, String targetRegName) {
    if (instruction instanceof com.fpetrola.z80.instructions.impl.Binary16BitsOperation bin16) {
      executeBinary16BitsOperation(mm, bin16, sourceRegName, targetRegName);
      return;
    }

    Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field("F");

    Class<?> aluOperationClass = aluOperationHandler.getAluOperationClass(instruction);
    String fieldName = aluOperationHandler.getAluOperationFieldName(instruction.getClass().getSimpleName());

    Variable aluOp = mm.var(aluOperationClass);
    aluOp.set(mm.field(fieldName));

    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, sourceValue, flag));

    registerValueResolver.assignRegisterValue(mm, targetRegName, result);

    Variable flagField = mm.field("F");
    flagField.set(aluOp.field("F"));
  }

  /**
   * Ejecuta una operación Binary16BitsOperation (Add16, Adc16, Sbc16)
   */
  public void executeBinary16BitsOperation(MethodMaker mm, com.fpetrola.z80.instructions.impl.Binary16BitsOperation instruction,
                                           String sourceRegName, String targetRegName) {
    Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field("F");

    Class<?> aluOperationClass = aluOperationHandler.getAluOperationClass(instruction);
    String fieldName = aluOperationHandler.getAluOperationFieldName(instruction.getClass().getSimpleName());

    Variable aluOp = mm.var(aluOperationClass);
    aluOp.set(mm.field(fieldName));

    Variable result = mm.var(int.class);
    if (instruction instanceof com.fpetrola.z80.instructions.impl.Add16) {
      result.set(targetValue.add(sourceValue));
    } else if (instruction instanceof com.fpetrola.z80.instructions.impl.Adc16) {
      result.set(targetValue.add(sourceValue).add(flag.and(0x01)));
    } else if (instruction instanceof com.fpetrola.z80.instructions.impl.Sbc16) {
      result.set(targetValue.sub(sourceValue).sub(flag.and(0x01)));
    } else {
      throw new UnsupportedOperationException("Operación Binary16BitsOperation no soportada: " + instruction.getClass().getSimpleName());
    }

    Variable maskedResultValue = result.and(0xFFFF);
    aluOp.invoke("calculateOriginal", targetValue, sourceValue, result, maskedResultValue);

    registerValueResolver.assignRegisterValue(mm, targetRegName, result.and(0xFFFF));

    Variable flagField = mm.field("F");
    flagField.set(aluOp.field("F"));
  }

  /**
   * Ejecuta operaciones ALU cuando el source es memoria: target = target op (memoria)
   */
  public void executeAluOperationFromMemory(MethodMaker mm, TargetSourceInstruction instruction,
                                            ImmutableOpcodeReference source, String targetRegName) {
    Variable memory = mm.field("memory");

    if (source instanceof IndirectMemory8BitReference indMem) {
      Variable address = memoryAccessHandler.resolveIndirectMemoryAddress(mm, indMem);
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read", address, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof IndirectMemory16BitReference indMem16) {
      Variable address = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm, indMem16);
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read16Bits", address));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryAccessHandler.MemoryPlusRegisterContext ctx = memoryAccessHandler.readOffsetAndCalculateAddress(mm, memRef);
      Variable value = mm.var(int.class);
      value.set(ctx.memory.invoke("read", ctx.address, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof Memory8BitReference memory8BitReference) {
      Variable pcPlusDelta = mm.field("PC").add(memory8BitReference.getDelta()).and(0xFFFF);
      Variable dd = mm.var(int.class);
      dd.set(memory.invoke("read", pcPlusDelta, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, dd);
    } else if (source instanceof Memory16BitReference memory16BitReference) {
      Variable pcPlusDelta = mm.field("PC").add(memory16BitReference.getDelta()).and(0xFFFF);
      Variable dd = mm.var(int.class);
      dd.set(memory.invoke("read16Bits", pcPlusDelta, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, dd);
    }
  }

  /**
   * Ejecuta la operación ALU con un valor leído de memoria
   */
  private void executeAluWithMemoryValue(MethodMaker mm, TargetSourceInstruction instruction,
                                        String targetRegName, Variable memoryValue) {
    Variable targetValue = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);

    if (classifier.isAluOperation(instruction)) {
      Class<?> aluOperationClass = aluOperationHandler.getAluOperationClass(instruction);
      String fieldName = aluOperationHandler.getAluOperationFieldName(instruction.getClass().getSimpleName());

      Variable aluOp = mm.var(aluOperationClass);
      aluOp.set(mm.field(fieldName));
      Variable flag = mm.field("F");

      Variable result = mm.var(int.class);
      result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, memoryValue, flag));

      registerValueResolver.assignRegisterValue(mm, targetRegName, result);

      Variable flagField = mm.field("F");
      flagField.set(aluOp.field("F"));
    }
  }
}
