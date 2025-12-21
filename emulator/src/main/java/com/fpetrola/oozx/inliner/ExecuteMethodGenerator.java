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
import com.fpetrola.z80.instructions.impl.Push;

import java.util.Set;
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
  private final PushOperationHandler pushOperationHandler;

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
    this.pushOperationHandler = new PushOperationHandler(registerValueResolver);
  }

  /**
    * Agrega un método execute para una TargetSourceInstruction
    */
  public void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                               String operationName, OpcodeReference target) {
    addExecuteMethod(cm, instruction, operationName, target, null);
  }

  /**
    * Agrega un método execute para una TargetSourceInstruction con seguimiento de métodos generados
    */
  public void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                               String operationName, OpcodeReference target, Set<String> generatedMethods) {
    String methodName = nameGenerator.generateUniquMethodName(instruction, operationName, target);
    
    // Si generatedMethods está disponible y el método ya existe, no lo agreguemos de nuevo
    if (generatedMethods != null && generatedMethods.contains(methodName)) {
      return;
    }
    
    // Agregar INMEDIATAMENTE a generatedMethods para prevenir re-intentos durante procesamiento paralelo
    if (generatedMethods != null) {
      // Si otro thread/contexto ya lo agregó, no lo hacemos de nuevo
      if (!generatedMethods.add(methodName)) {
        // El método ya estaba en el conjunto, no agregamos
        return;
      }
    }
    
    MethodMaker[] mms = new MethodMaker[]{null};

    Supplier<MethodMaker> methodMakerSupplier = () -> {
      if (mms[0] == null) {
        mms[0] = cm.addMethod(void.class, methodName);
        mms[0].public_();
      }
      return mms[0];
    };

    try {
      generateExecute(methodMakerSupplier, instruction, target);
      mms[0].return_();
    } catch (Exception e) {
      // Si la generación falla, removemos del registry para permitir reintentos
      if (generatedMethods != null) {
        generatedMethods.remove(methodName);
      }
      throw e;
    }
  }

  /**
    * Agrega un método execute para una ParameterizedUnaryAluInstruction
    */
  public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                    String operationName) {
    addExecuteUnaryMethod(cm, instruction, operationName, null);
  }

  /**
    * Agrega un método execute para una ParameterizedUnaryAluInstruction con seguimiento de métodos generados
    */
  public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                    String operationName, Set<String> generatedMethods) {
    String methodName = nameGenerator.generateUnaryMethodName(instruction, operationName);
    
    // Si generatedMethods está disponible y el método ya existe, no lo agreguemos de nuevo
    if (generatedMethods != null && generatedMethods.contains(methodName)) {
      return;
    }
    
    // Agregar INMEDIATAMENTE a generatedMethods para prevenir re-intentos durante procesamiento paralelo
    if (generatedMethods != null) {
      // Si otro thread/contexto ya lo agregó, no lo hacemos de nuevo
      if (!generatedMethods.add(methodName)) {
        // El método ya estaba en el conjunto, no agregamos
        return;
      }
    }
    
    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    
    try {
      generateUnaryExecute(mm, instruction);
      mm.return_();
    } catch (Exception e) {
      // Si la generación falla, removemos del registry para permitir reintentos
      if (generatedMethods != null) {
        generatedMethods.remove(methodName);
      }
      throw e;
    }
  }

  /**
   * Genera código de ejecución para instrucciones binarias (LD, XOR, OR, etc.)
   */
  private void generateExecute(Supplier<MethodMaker> mm, TargetSourceInstruction targetSourceInstruction, OpcodeReference target) {
    ImmutableOpcodeReference source = targetSourceInstruction.getSource();

    // Caso 1: source es un Register
    if (source instanceof Register sourceReg) {
      String sourceRegName = sourceReg.getName();
      // Si target es también un Register (register-to-register)
      if (target instanceof Register targetReg) {
        String targetRegName = targetReg.getName();
        if (targetSourceInstruction instanceof Ld) {
          Variable sourceValue = registerValueResolver.resolveRegisterValueByName(mm.get(), sourceRegName);
          registerValueResolver.assignRegisterValue(mm.get(), targetRegName, sourceValue);
        } else if (classifier.isAluOperation(targetSourceInstruction)) {
          binaryOperationHandler.executeRegisterToRegisterAluOperation(mm.get(), targetSourceInstruction, sourceRegName, targetRegName);
        } else {
          throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + targetSourceInstruction.getClass());
        }
        return;
      }
      generateAluExecute(mm, targetSourceInstruction, target, sourceRegName);
    }
    // Caso 2: source es una referencia de memoria pero target es un Register
    else if (target instanceof Register targetReg) {
      String targetRegName = targetReg.getName();
      if (targetSourceInstruction instanceof Ld) {
        Variable address = memoryAccessHandler.resolveSourceMemoryAddress(mm.get(), source);
        if (address != null) {
          Variable memory = mm.get().field("memory");
          Variable value = mm.get().var(int.class);
          if (targetSourceInstruction.getSource() instanceof Memory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else if (targetSourceInstruction.getSource() instanceof IndirectMemory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else {
            value.set(memory.invoke("read", address, 0));
          }
          registerValueResolver.assignRegisterValue(mm.get(), targetRegName, value);
        }
      } else if (classifier.isAluOperation(targetSourceInstruction)) {
        binaryOperationHandler.executeAluOperationFromMemory(mm.get(), targetSourceInstruction, source, targetRegName);
      }
    }
    // Caso 3: source es memoria y target también es memoria
    else {
      if (targetSourceInstruction instanceof Ld && source instanceof IndirectMemory8BitReference) {
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
        throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + targetSourceInstruction.getClass());
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

  /**
   * Agrega un método execute para una instrucción PUSH
   */
  public void addExecutePushMethod(ClassMaker cm, Push instruction, String operationName, Set<String> generatedMethods) {
    String methodName = nameGenerator.generatePushMethodName(instruction, operationName);
    
    // Si generatedMethods está disponible y el método ya existe, no lo agreguemos de nuevo
    if (generatedMethods != null && generatedMethods.contains(methodName)) {
      return;
    }
    
    // Agregar INMEDIATAMENTE a generatedMethods para prevenir re-intentos durante procesamiento paralelo
    if (generatedMethods != null) {
      if (!generatedMethods.add(methodName)) {
        return;
      }
    }
    
    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    
    try {
      generatePushExecute(mm, instruction);
      mm.return_();
    } catch (Exception e) {
      if (generatedMethods != null) {
        generatedMethods.remove(methodName);
      }
      throw e;
    }
  }

  /**
   * Genera código de ejecución para PUSH
   */
  private void generatePushExecute(MethodMaker mm, Push instruction) {
    // Obtener el nombre del registro desde el target
    // Push.getTarget() retorna un OpcodeReference (que debería ser un Register)
    OpcodeReference target = instruction.getTarget();
    
    if (target instanceof Register targetReg) {
      String registerName = targetReg.getName();
      pushOperationHandler.executePushWithRegister(mm, registerName);
    } else {
      throw new UnsupportedOperationException("PUSH requiere un registro como target, pero se encontró: " + target.getClass().getSimpleName());
    }
  }
  }
