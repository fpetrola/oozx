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
  private final BinaryOperationHandler binaryOperationHandler;
  private final UnaryOperationHandler unaryOperationHandler;

  public ExecuteMethodGenerator(InstructionAnalyzer analyzer, InstructionClassifier classifier,
                                RegisterValueResolver registerValueResolver, MemoryAccessHandler memoryAccessHandler,
                                AluOperationHandler aluOperationHandler, MethodNameGenerator nameGenerator) {
    this.analyzer = analyzer;
    this.classifier = classifier;
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
    this.aluOperationHandler = aluOperationHandler;
    this.nameGenerator = nameGenerator;
    this.binaryOperationHandler = new BinaryOperationHandler(classifier, registerValueResolver, memoryAccessHandler, aluOperationHandler);
    this.unaryOperationHandler = new UnaryOperationHandler(registerValueResolver, memoryAccessHandler, aluOperationHandler, nameGenerator);
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
          binaryOperationHandler.executeRegisterToRegisterAluOperation(mm.get(), ld, sourceRegName, targetRegName);
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
        binaryOperationHandler.executeAluOperationFromMemory(mm.get(), ld, source, targetRegName);
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
      binaryOperationHandler.executeRegisterToRegisterAluOperation(mm.get(), instruction, sourceRegName, targetRegName);
    } else {
      throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + instruction.getClass());
    }
  }

  /**
   * Genera código para instrucciones unarias (Inc, Dec, etc.)
   */
  private void generateUnaryExecute(MethodMaker mm, ParameterizedUnaryAluInstruction instruction) {
    unaryOperationHandler.generateUnaryExecute(mm, instruction);
  }
}
