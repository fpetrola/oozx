package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.oozx.inliner.strategies.OpcodeReferenceStrategyFactory;
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
    
    // Set F on aluOperation before using it
    aluOp.field("F").set(flag);

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
    var strategy = OpcodeReferenceStrategyFactory.create(source);
    Variable address = strategy.resolveAddress(mm, memoryAccessHandler);
    Variable memory = mm.field("memory");
    Variable value = strategy.readValue(mm, address, memory);
    executeAluWithMemoryValue(mm, instruction, targetRegName, value);
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
