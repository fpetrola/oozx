package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.*;

/**
 * Maneja el procesamiento de instrucciones: análisis de prefijos, clasificación
 * y generación de métodos execute correspondientes.
 */
public class InstructionProcessorHandler {
  private final InstructionClassifier classifier;
  private final MethodNameGenerator nameGenerator;
  private final InstructionAnalyzer analyzer;
  private final DispatchMethodGenerator dispatchGenerator;
  private final InstructionHandlerRegistry handlerRegistry;

  public InstructionProcessorHandler(InstructionClassifier classifier, MethodNameGenerator nameGenerator, 
                                     InstructionAnalyzer analyzer, DispatchMethodGenerator dispatchGenerator,
                                     InstructionHandlerRegistry handlerRegistry) {
    this.classifier = classifier;
    this.nameGenerator = nameGenerator;
    this.analyzer = analyzer;
    this.dispatchGenerator = dispatchGenerator;
    this.handlerRegistry = handlerRegistry;
  }

  /**
    * Procesa todas las instrucciones y genera los métodos correspondientes
    * Retorna un objeto con los mapeos necesarios para el dispatch
    */
  public BytecodeInliner.InstructionProcessingResult processInstructions(
      ClassMaker cm, Map<Integer, Instruction> instructions,
      IInstructionMethodGenerator methodGenerator) {
    
    Set<String> generatedMethods = methodGenerator.getByteocdeInliner().getGeneratedMethods();
    
    Map<Integer, String> opcodeToMethodName = new LinkedHashMap<>();
    Map<Integer, String> prefixOpcodes = new LinkedHashMap<>();
    Map<Integer, Map<Integer, String>> prefixedInstructions = new LinkedHashMap<>();

    for (Map.Entry<Integer, Instruction> entry : instructions.entrySet()) {
      Integer opcode = entry.getKey();
      Instruction instruction = entry.getValue();

      if (instruction == null || classifier.isUnsupportedInstruction(instruction)) {
        continue;
      }

      if (instruction instanceof DefaultFetchNextOpcodeInstruction prefixInstruction) {
        prefixOpcodes.put(opcode, prefixInstruction.getClass().getSimpleName());
        prefixedInstructions.put(opcode, new LinkedHashMap<>());
      } else if (classifier.isPrefixedOpcode(opcode, instructions)) {
        processPrefixedInstruction(cm, instruction, opcode, instructions, prefixedInstructions, 
                                  methodGenerator);
      } else {
        processNonPrefixedInstruction(cm, instruction, opcode, opcodeToMethodName, 
                                     methodGenerator);
      }
    }

    // Agregar métodos dispatch para prefijos
    for (Map.Entry<Integer, Map<Integer, String>> prefixEntry : prefixedInstructions.entrySet()) {
      Integer prefixOpcode = prefixEntry.getKey();
      Map<Integer, String> prefixMethods = prefixEntry.getValue();
      if (!prefixMethods.isEmpty()) {
        String dispatchMethodName = dispatchGenerator.generatePrefixDispatchMethodName(prefixOpcode);
        methodGenerator.addPrefixDispatchMethod(cm, dispatchMethodName, prefixMethods);
        opcodeToMethodName.put(prefixOpcode, dispatchMethodName);
      }
    }

    return new BytecodeInliner.InstructionProcessingResult(opcodeToMethodName, prefixOpcodes);
  }

  /**
    * Procesa una instrucción prefijada (ej: instrucciones CB, DD, FD)
    */
   private void processPrefixedInstruction(
       ClassMaker cm, Instruction instruction, Integer opcode,
       Map<Integer, Instruction> instructions,
       Map<Integer, Map<Integer, String>> prefixedInstructions,
       IInstructionMethodGenerator methodGenerator) {
     Set<String> generatedMethods = methodGenerator.getByteocdeInliner().getGeneratedMethods();
    
    int prefixByte = (opcode >> 8) & 0xFF;
    int nextOpcode = opcode & 0xFF;

    if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
      // Pero si tiene handler ESPECÍFICO (como Ex), usarlo primero
      if (handlerRegistry.hasHandler(instruction) && 
          (instruction instanceof Ex)) {  // Solo para instrucciones que sabemos tienen handlers
        String methodName = processRegistryInstruction(cm, instruction, methodGenerator);
        if (methodName != null) {
          prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
          return;
        }
      }
      
      String methodName = processTargetSourceInstruction(cm, targetSourceInstruction, 
                                                         methodGenerator);
      if (methodName != null) {
        prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
      }
    } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
      String methodName = processUnaryInstructionPrefixed(cm, unaryInstruction, 
                                                         prefixByte, nextOpcode, methodGenerator);
      if (methodName != null) {
        prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
      }
    } else {
      // Intentar procesar como instrucción del registry (Push, Pop, etc.)
      if (handlerRegistry.hasHandler(instruction)) {
        String methodName = processRegistryInstruction(cm, instruction, methodGenerator);
        if (methodName != null) {
          prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
          return;  // Si el handler procesó, no continuar
        }
      }
    }
  }

  /**
   * Procesa una ParameterizedUnaryAluInstruction prefijada con logging
   */
  private String processUnaryInstructionPrefixed(
      ClassMaker cm, ParameterizedUnaryAluInstruction instruction,
      int prefixByte, int nextOpcode,
      IInstructionMethodGenerator methodGenerator) {
    
    try {
      return processUnaryInstruction(cm, instruction, methodGenerator);
    } catch (Exception e) {
      System.err.println("Warning: No se pudo procesar instrucción unaria prefijada 0x" +
        String.format("%02X%02X", prefixByte, nextOpcode) +
        " (" + instruction.getClass().getSimpleName() + "): " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
    * Procesa una instrucción no prefijada
    */
   private void processNonPrefixedInstruction(
      ClassMaker cm, Instruction instruction, Integer opcode,
      Map<Integer, String> opcodeToMethodName,
      IInstructionMethodGenerator methodGenerator) {
     Set<String> generatedMethods = methodGenerator.getByteocdeInliner().getGeneratedMethods();
   
   if (instruction == null) {
     // Ignorar instrucciones nulas
     return;
   }
   
   // Si es TargetSourceInstruction, procesar como tal (no con handlers genéricos que fallarían)
   if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
     // Pero si tiene handler ESPECÍFICO (como Ex), usarlo primero
     if (handlerRegistry.hasHandler(instruction) && 
         (instruction instanceof Ex)) {  // Solo para instrucciones que sabemos tienen handlers
       String methodName = processRegistryInstruction(cm, instruction, methodGenerator);
       if (methodName != null) {
         opcodeToMethodName.put(opcode, methodName);
         return;
       }
     }
     
     String methodName = processTargetSourceInstruction(cm, targetSourceInstruction, 
                                                       methodGenerator);
     if (methodName != null) {
       opcodeToMethodName.put(opcode, methodName);
     }
   } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
     String methodName = processUnaryInstruction(cm, unaryInstruction, 
                                                methodGenerator);
     if (methodName != null) {
       opcodeToMethodName.put(opcode, methodName);
     }
   } else {
     // Intentar procesar con handlers registrados (Push, Dec16, Inc16, etc.)
     if (handlerRegistry.hasHandler(instruction)) {
       String methodName = processRegistryInstruction(cm, instruction, methodGenerator);
       if (methodName != null) {
         opcodeToMethodName.put(opcode, methodName);
         return;  // Si el handler procesó, no continuar
       }
     }
   }
  }

  /**
    * Procesa una TargetSourceInstruction: análisis, generación de nombre y creación del método
    */
   private String processTargetSourceInstruction(
       ClassMaker cm, TargetSourceInstruction<?> instruction,
       IInstructionMethodGenerator methodGenerator) {
     
     try {
       String operationName = instruction.getClass().getSimpleName();
       
       // Procesar como TargetSourceInstruction normal
       analyzer.analyze(instruction);
       OpcodeReference target = analyzer.getTarget();
       String methodName = nameGenerator.generateUniquMethodName(instruction, operationName, target);

       try {
         methodGenerator.addExecuteMethod(cm, instruction, operationName, target);
       } catch (ClassFormatError e) {
         // Si el método ya existe (puede ocurrir con instrucciones duplicadas en diferentes prefijos),
         // simplemente reutilizamos el nombre que ya existe sin intentar volver a agregarlo
         if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
           // El método ya fue agregado, continuamos
         } else {
           throw e;
         }
       }
       return methodName;
     } catch (Exception e) {
       System.err.println("DEBUG: Exception in processTargetSourceInstruction for " + instruction.getClass().getSimpleName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
       e.printStackTrace();
       return null;
     }
   }

  /**
    * Procesa una ParameterizedUnaryAluInstruction: generación de nombre y creación del método
    */
   private String processUnaryInstruction(
       ClassMaker cm, ParameterizedUnaryAluInstruction instruction,
       IInstructionMethodGenerator methodGenerator) {
     
     try {
       String operationName = instruction.getClass().getSimpleName();
       String methodName = nameGenerator.generateUnaryMethodName(instruction, operationName);

       try {
         methodGenerator.addExecuteUnaryMethod(cm, instruction, operationName);
         return methodName;
       } catch (ClassFormatError e) {
         // Si el método ya existe (puede ocurrir con instrucciones duplicadas en diferentes prefijos),
         // simplemente reutilizamos el nombre que ya existe sin intentar volver a agregarlo
         if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
           return methodName;
         } else {
           throw e;
         }
       }
     } catch (Exception e) {
       System.err.println("DEBUG: Exception in processUnaryInstruction for " + instruction.getClass().getSimpleName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
       return null;
     }
   }

  /**
    * Procesa una instrucción con handler registrado (Push, Dec16, Inc16, etc.)
    * Si no hay handler, simplemente retorna null sin procesar
    */
   private String processRegistryInstruction(
       ClassMaker cm, Instruction instruction,
       IInstructionMethodGenerator methodGenerator) {
     
     // Si no hay handler registrado para esta instrucción, no la procesamos
     // Necesitamos verificar antes de intentar generar el método
     if (!handlerRegistry.hasHandler(instruction)) {
       return null;
     }
     
     String operationName = instruction.getClass().getSimpleName();
     
     try {
       // Intentar procesar con el handler del registry
       boolean processed = methodGenerator.addExecuteGenericMethod(cm, instruction, operationName);
       
       // Solo agregar al mapping si fue procesado exitosamente
       if (!processed) {
         return null;  // No fue procesado, no agregamos al switch
       }
       
       // Generar el nombre del método para el mapping
       String methodName = generateRegistryMethodName(instruction, operationName);
       return methodName;
       
     } catch (ClassFormatError e) {
       // Manejo especial para métodos duplicados - reutilizamos el nombre existente
       if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
         String methodName = generateRegistryMethodName(instruction, operationName);
         return methodName;
       } else {
         // Otros errores de formato de clase - continuamos sin la instrucción
         System.err.println("Warning: ClassFormatError al procesar " + operationName + ": " + e.getMessage());
         return null;
       }
     } catch (Exception e) {
       // Cualquier otra excepción - simplemente log y continua
       System.err.println("Warning: No se pudo procesar instrucción del registry " + operationName + 
                         ": " + e.getClass().getSimpleName());
       return null;
     }
   }

   /**
     * Genera el nombre del método para instrucciones del registry
     * Delega a MethodNameGenerator para instrucciones TargetSource
     */
    private String generateRegistryMethodName(Instruction instruction, String operationName) {
      // No agregar sufijo para instrucciones de flag (SCF, CCF)
      if (instruction instanceof SCF || instruction instanceof CCF) {
        return "execute" + operationName.toLowerCase();
      }
      
      // Para TargetSourceInstructions, usar el generador de nombres principal
      if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
        OpcodeReference target = ((com.fpetrola.z80.instructions.types.DefaultTargetInstruction) targetSourceInstruction).getTarget();
        return nameGenerator.generateUniquMethodName(targetSourceInstruction, operationName, target);
      }
      
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

  /**
     * Interfaz para abstracción de generación de métodos
     */
   public interface IInstructionMethodGenerator {
     void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                          String operationName, OpcodeReference target);
     
     void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                               String operationName);
     
     boolean addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                    String operationName);
     
     void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                 Map<Integer, String> prefixMethods);
     
     BytecodeInliner getByteocdeInliner();
   }
}
