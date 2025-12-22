package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

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
  private final InstructionHandlerRegistry handlerRegistry;

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
    this.handlerRegistry = new InstructionHandlerRegistry(registerValueResolver, memoryAccessHandler);
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
        mms[0] = addingMethod(cm, methodName);
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
    
    MethodMaker mm = addingMethod(cm, methodName);
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

  private MethodMaker addingMethod(ClassMaker cm, String methodName) {
    if (methodName.equals("executeExImr16Sp"))
      System.out.println("adgadgd");
    return cm.addMethod(void.class, methodName);
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
    * Agrega un método execute genérico para instrucciones manejadas por el registry
    * Retorna true si fue procesado exitosamente, false si no pudo ser procesado
    */
   public boolean addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                          String operationName, Set<String> generatedMethods) {
     // Verificar que hay handler registrado antes de crear el método
     if (!handlerRegistry.hasHandler(instruction)) {
       // Sin handler registrado, no procesamos - simplemente retornamos false
       return false;
     }
     
     // Generar el nombre del método según el tipo de instrucción
     // Esto debe coincidir con lo que genera InstructionProcessorHandler.generateRegistryMethodName()
     String methodName = generateGenericMethodName(instruction, operationName);
     
     // Si generatedMethods está disponible y el método ya existe, no lo agreguemos de nuevo
     if (generatedMethods != null && generatedMethods.contains(methodName)) {
       return true;  // Ya fue procesado
     }
     
     // Primero, intentar procesar sin crear el método aún
     try {
       // Intentar procesar con el registry - ya verificamos que existe handler
       // Creamos un MethodMaker temporal para que el handler pueda generar código
       MethodMaker tempMm = addingMethod(cm, methodName);
       tempMm.public_();
       
       boolean handled = handlerRegistry.tryHandle(cm, instruction, tempMm, operationName, generatedMethods);
       
       if (!handled) {
         // Si el handler no pudo procesar, el método se quedó vacío
         // Agregamos un return_() para que el método sea válido de todas formas,
         // aunque esté vacío (sin código útil)
         tempMm.return_();
         // NO agregamos a generatedMethods ya que no fue procesado exitosamente
         return false;  // No fue procesado
       }
       
       // El handler procesó exitosamente, retornamos
       tempMm.return_();
       
       // Agregar a generatedMethods si fue procesado exitosamente
       if (generatedMethods != null) {
         generatedMethods.add(methodName);
       }
       
       return true;  // Procesado exitosamente
       
     } catch (Exception e) {
       // Si hay error durante el procesamiento, continuamos sin lanzar
       System.err.println("Warning: No se pudo procesar instrucción " + instruction.getClass().getSimpleName() + 
                         " con handler registrado: " + e.getMessage());
       e.printStackTrace();
       return false;  // No fue procesado
     }
   }

  /**
    * Genera el nombre del método para una instrucción genérica
    * Delega a MethodNameGenerator para instrucciones TargetSource
    */
  private String generateGenericMethodName(Instruction instruction, String operationName) {
    // No agregar sufijo para instrucciones de flag (SCF, CCF)
    if (instruction instanceof SCF || instruction instanceof CCF) {
      return "execute" + operationName.toLowerCase();
    }
    
    // Para TargetSourceInstructions, usar el generador de nombres principal
    if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
      OpcodeReference target = ((com.fpetrola.z80.instructions.types.DefaultTargetInstruction) targetSourceInstruction).getTarget();
      return nameGenerator.generateUniquMethodName(targetSourceInstruction, operationName, target);
    }
    
    // Para Push y Pop, agregar el sufijo del registro
    StringBuilder methodName = new StringBuilder("execute").append(operationName);
    if (instruction instanceof Push pushInstr) {
      OpcodeReference target = pushInstr.getTarget();
      methodName.append(nameGenerator.getReferenceSuffix(target));
    } else if (instruction instanceof Pop popInstr) {
      OpcodeReference target = popInstr.getTarget();
      methodName.append(nameGenerator.getReferenceSuffix(target));
    } else if (instruction instanceof com.fpetrola.z80.instructions.types.BitOperation bitOp) {
      // Para BitOperation (RES, SET, BIT), incluir el número del bit en el nombre
      OpcodeReference target = bitOp.getTarget();
      methodName.append("Bit").append(bitOp.getN());
      methodName.append(nameGenerator.getReferenceSuffix(target));
    } else if (instruction instanceof com.fpetrola.z80.instructions.types.DefaultTargetInstruction defaultTargetInstr) {
      OpcodeReference target = defaultTargetInstr.getTarget();
      methodName.append(nameGenerator.getReferenceSuffix(target));
    }
    
    return methodName.toString();
  }
}
