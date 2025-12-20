package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.function.Supplier;

/**
 * Genera bytecode para métodos execute (binarios y unarios).
 * Responsable de compilar instrucciones en código ejecutable.
 */
public class ExecuteMethodGenerator {
  public static final String FLAG = "F";
  
  private final InstructionAnalyzer analyzer;
  private final InstructionClassifier classifier;
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;
  private final AluOperationHandler aluOperationHandler;
  private final MethodNameGenerator nameGenerator;

  public ExecuteMethodGenerator(InstructionAnalyzer analyzer, InstructionClassifier classifier,
                                RegisterValueResolver registerValueResolver, MemoryAccessHandler memoryAccessHandler,
                                AluOperationHandler aluOperationHandler, MethodNameGenerator nameGenerator) {
    this.analyzer = analyzer;
    this.classifier = classifier;
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
    this.aluOperationHandler = aluOperationHandler;
    this.nameGenerator = nameGenerator;
  }

  /**
   * Agrega un método execute para una TargetSourceInstruction
   */
  public void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                               String operationName, OpcodeReference target) {
    String methodName = nameGenerator.generateUniquMethodName(instruction, operationName, target);
    MethodMaker[] mms = new MethodMaker[]{null};

    Supplier<MethodMaker> methodMakerSupplier = () -> {
      if (mms[0] == null) {
        mms[0] = cm.addMethod(void.class, methodName);
        mms[0].public_();
      }
      return mms[0];
    };

    generateExecute(methodMakerSupplier, instruction, target);
    mms[0].return_();
  }

  /**
   * Agrega un método execute para una ParameterizedUnaryAluInstruction
   */
  public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                    String operationName) {
    String methodName = nameGenerator.generateUnaryMethodName(instruction, operationName);
    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    
    generateUnaryExecute(mm, instruction);
    mm.return_();
  }

  /**
   * Genera código de ejecución para instrucciones binarias (LD, XOR, OR, etc.)
   */
  private void generateExecute(Supplier<MethodMaker> mm, TargetSourceInstruction ld, OpcodeReference target) {
    ImmutableOpcodeReference source = ld.getSource();

    // Caso 1: source es un Register
    if (source instanceof Register sourceReg) {
      String sourceRegName = sourceReg.getName();
      // Si target es también un Register (register-to-register)
      if (target instanceof Register targetReg) {
        String targetRegName = targetReg.getName();
        if (ld instanceof Ld) {
          Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm.get(), sourceRegName);
          registerValueResolver.assignRegisterValue(mm.get(), targetRegName, sourceValue);
        } else if (classifier.isAluOperation(ld)) {
          executeRegisterToRegisterAluOperation(mm.get(), ld, sourceRegName, targetRegName);
        } else {
          throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + ld.getClass());
        }
        return;
      }
      generateAluExecute(mm, ld, target, sourceRegName);
    }
    // Caso 2: source es una referencia de memoria pero target es un Register
    else if (target instanceof Register targetReg) {
      String targetRegName = targetReg.getName();
      if (ld instanceof Ld) {
        Variable address = memoryAccessHandler.resolveSourceMemoryAddress(mm.get(), source);
        if (address != null) {
          Variable memory = mm.get().field("memory");
          Variable value = mm.get().var(int.class);
          if (ld.getSource() instanceof Memory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else if (ld.getSource() instanceof IndirectMemory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else {
            value.set(memory.invoke("read", address, 0));
          }
          registerValueResolver.assignRegisterValue(mm.get(), targetRegName, value);
        }
      } else if (classifier.isAluOperation(ld)) {
        executeAluOperationFromMemory(mm.get(), ld, source, targetRegName);
      }
    }
    // Caso 3: source es memoria y target también es memoria
    else {
      if (ld instanceof Ld && source instanceof IndirectMemory8BitReference) {
        Variable sourceAddr = memoryAccessHandler.resolveSourceMemoryAddress(mm.get(), source);
        if (sourceAddr != null && target instanceof IndirectMemory8BitReference targetMem) {
          Variable targetAddr = memoryAccessHandler.resolveIndirectMemoryAddress(mm.get(), targetMem);
          if (targetAddr != null) {
            Variable memory = mm.get().field("memory");
            Variable value = mm.get().var(int.class);
            value.set(memory.invoke("read", sourceAddr, 0));
            memory.invoke("write", targetAddr, value);
          }
        }
      } else {
        throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + ld.getClass());
      }
    }
  }

  /**
   * Ejecuta operaciones ALU cuando el source es memoria: target = target op (memoria)
   */
  private void executeAluOperationFromMemory(MethodMaker mm, TargetSourceInstruction instruction,
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
      Variable flag = mm.field(FLAG);

      Variable result = mm.var(int.class);
      result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, memoryValue, flag));

      registerValueResolver.assignRegisterValue(mm, targetRegName, result);

      Variable flagField = mm.field(FLAG);
      flagField.set(aluOp.field(FLAG));
    }
  }

  /**
   * Ejecuta operaciones ALU entre dos registros (register-to-register)
   */
  private void executeRegisterToRegisterAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                                    String sourceRegName, String targetRegName) {
    if (instruction instanceof com.fpetrola.z80.instructions.impl.Binary16BitsOperation bin16) {
      executeBinary16BitsOperation(mm, bin16, sourceRegName, targetRegName);
      return;
    }

    Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field(FLAG);

    Class<?> aluOperationClass = aluOperationHandler.getAluOperationClass(instruction);
    String fieldName = aluOperationHandler.getAluOperationFieldName(instruction.getClass().getSimpleName());

    Variable aluOp = mm.var(aluOperationClass);
    aluOp.set(mm.field(fieldName));

    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, sourceValue, flag));

    registerValueResolver.assignRegisterValue(mm, targetRegName, result);

    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
  }

  /**
   * Ejecuta una operación Binary16BitsOperation
   */
  private void executeBinary16BitsOperation(MethodMaker mm, com.fpetrola.z80.instructions.impl.Binary16BitsOperation instruction,
                                           String sourceRegName, String targetRegName) {
    Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field(FLAG);

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

    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
  }

  /**
   * Genera código de ejecución para instrucciones ALU con target en memoria
   */
  private void generateAluExecute(Supplier<MethodMaker> mm, TargetSourceInstruction instruction,
                                 OpcodeReference target, String sourceRegName) {
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryAccessHandler.MemoryPlusRegisterContext ctx = memoryAccessHandler.readOffsetAndCalculateAddress(mm.get(), memRef);
      aluOperationHandler.executeAluOperation(mm.get(), instruction, ctx.memory, ctx.address, sourceRegName);
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      Variable address = memoryAccessHandler.resolveIndirectMemoryAddress(mm.get(), indMem);
      Variable memory = mm.get().field("memory");
      aluOperationHandler.executeAluOperation(mm.get(), instruction, memory, address, sourceRegName);
    } else if (target instanceof IndirectMemory16BitReference indMem16) {
      Variable address = memoryAccessHandler.resolveIndirectMemory16BitAddress(mm.get(), indMem16);
      Variable memory = mm.get().field("memory");
      aluOperationHandler.executeAluOperation16Bit(mm.get(), instruction, memory, address, sourceRegName);
    } else if (target instanceof Register targetReg) {
      String targetRegName = targetReg.getName();
      executeRegisterToRegisterAluOperation(mm.get(), instruction, sourceRegName, targetRegName);
    } else {
      throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + instruction.getClass());
    }
  }

  /**
   * Genera código para instrucciones unarias (Inc, Dec, etc.)
   */
  private void generateUnaryExecute(MethodMaker mm, ParameterizedUnaryAluInstruction instruction) {
    try {
      OpcodeReference target = nameGenerator.getTargetFromUnaryInstruction(instruction);
      if (target == null) {
        throw new RuntimeException("No se pudo obtener target de la instrucción unaria");
      }

      Class<?> aluOperationClass = getAluOperationClassFromUnaryInstruction(instruction);

      if (target instanceof Register targetReg) {
        String targetRegName = targetReg.getName();
        executeUnaryRegisterOperation(mm, instruction, targetRegName, aluOperationClass);
      } else if (target instanceof IndirectMemory8BitReference indMem) {
        Variable address = memoryAccessHandler.resolveIndirectMemoryAddress(mm, indMem);
        if (address == null) {
          throw new RuntimeException("No se pudo resolver dirección para IndirectMemory8BitReference en " + instruction.getClass().getSimpleName());
        }
        Variable memory = mm.field("memory");
        executeUnaryMemoryOperation(mm, instruction, memory, address, aluOperationClass);
      } else if (target instanceof MemoryPlusRegister8BitReference memRef) {
        MemoryAccessHandler.MemoryPlusRegisterContext ctx = memoryAccessHandler.readOffsetAndCalculateAddress(mm, memRef);
        executeUnaryMemoryOperation(mm, instruction, ctx.memory, ctx.address, aluOperationClass);
      } else {
        throw new RuntimeException("Tipo de target no soportado para instrucción unaria: " + target.getClass().getSimpleName() + " en " + instruction.getClass().getSimpleName());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error generando código para instrucción unaria (" + instruction.getClass().getSimpleName() + "): " + e.getMessage(), e);
    }
  }

  /**
   * Obtiene la clase de la operación ALU desde una instrucción unaria
   */
  private Class<?> getAluOperationClassFromUnaryInstruction(ParameterizedUnaryAluInstruction instruction) throws Exception {
    Class<?> clazz = instruction.getClass();

    while (clazz != null && clazz != Object.class) {
      try {
        java.lang.reflect.Field aluOpField = clazz.getDeclaredField("aluOperation");
        aluOpField.setAccessible(true);
        Object aluOp = aluOpField.get(instruction);
        return aluOp.getClass();
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }

    throw new RuntimeException("No se pudo obtener aluOperation");
  }

  /**
   * Ejecuta operación unaria en un registro: reg = alu.execute(reg, flag)
   */
  private void executeUnaryRegisterOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                            String targetRegName, Class<?> aluOperationClass) {
    Variable value = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable result = aluOperationHandler.executeUnaryAluOperation(mm, instruction, value);
    registerValueResolver.assignRegisterValue(mm, targetRegName, result);
  }

  /**
   * Ejecuta operación unaria en memoria: (addr) = alu.execute((addr), flag)
   */
  private void executeUnaryMemoryOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                          Variable memory, Variable address, Class<?> aluOperationClass) {
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable result = aluOperationHandler.executeUnaryAluOperation(mm, instruction, value);
    memory.invoke("write", address, result);
  }
}
