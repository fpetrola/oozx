package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
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

  public InstructionProcessorHandler(InstructionClassifier classifier, MethodNameGenerator nameGenerator, 
                                     InstructionAnalyzer analyzer, DispatchMethodGenerator dispatchGenerator) {
    this.classifier = classifier;
    this.nameGenerator = nameGenerator;
    this.analyzer = analyzer;
    this.dispatchGenerator = dispatchGenerator;
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

      if (classifier.isUnsupportedInstruction(instruction)) {
        continue;
      }

      if (instruction instanceof DefaultFetchNextOpcodeInstruction prefixInstruction) {
        prefixOpcodes.put(opcode, prefixInstruction.getClass().getSimpleName());
        prefixedInstructions.put(opcode, new LinkedHashMap<>());
      } else if (classifier.isPrefixedOpcode(opcode, instructions)) {
        processPrefixedInstruction(cm, instruction, opcode, instructions, prefixedInstructions, 
                                  generatedMethods, methodGenerator);
      } else {
        processNonPrefixedInstruction(cm, instruction, opcode, opcodeToMethodName, 
                                     generatedMethods, methodGenerator);
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
      Set<String> generatedMethods,
      IInstructionMethodGenerator methodGenerator) {
    
    int prefixByte = (opcode >> 8) & 0xFF;
    int nextOpcode = opcode & 0xFF;

    if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
      String methodName = processTargetSourceInstruction(cm, targetSourceInstruction, generatedMethods, 
                                                        methodGenerator);
      if (methodName != null) {
        prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
      }
    } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
      String methodName = processUnaryInstructionPrefixed(cm, unaryInstruction, generatedMethods, 
                                                         prefixByte, nextOpcode, methodGenerator);
      if (methodName != null) {
        prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
      }
    } else {
      // Intentar procesar como instrucción del registry (Push, Pop, etc.)
      String methodName = processRegistryInstruction(cm, instruction, generatedMethods, methodGenerator);
      if (methodName != null) {
        prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
      }
    }
  }

  /**
   * Procesa una ParameterizedUnaryAluInstruction prefijada con logging
   */
  private String processUnaryInstructionPrefixed(
      ClassMaker cm, ParameterizedUnaryAluInstruction instruction,
      Set<String> generatedMethods, int prefixByte, int nextOpcode,
      IInstructionMethodGenerator methodGenerator) {
    
    try {
      return processUnaryInstruction(cm, instruction, generatedMethods, methodGenerator);
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
     Set<String> generatedMethods,
     IInstructionMethodGenerator methodGenerator) {
   
   if (instruction == null) {
     // Ignorar instrucciones nulas
     return;
   }
   
   if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
     String methodName = processTargetSourceInstruction(cm, targetSourceInstruction, generatedMethods, 
                                                       methodGenerator);
     if (methodName != null) {
       opcodeToMethodName.put(opcode, methodName);
     }
   } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
     String methodName = processUnaryInstruction(cm, unaryInstruction, generatedMethods, 
                                                methodGenerator);
     if (methodName != null) {
       opcodeToMethodName.put(opcode, methodName);
     }
   } else {
     // Intentar procesar con handlers registrados (Push, Dec16, Inc16, etc.)
     String methodName = processRegistryInstruction(cm, instruction, generatedMethods, methodGenerator);
     if (methodName != null) {
       opcodeToMethodName.put(opcode, methodName);
     }
   }
  }

  /**
    * Procesa una TargetSourceInstruction: análisis, generación de nombre y creación del método
    */
   private String processTargetSourceInstruction(
       ClassMaker cm, TargetSourceInstruction<?> instruction,
       Set<String> generatedMethods,
       IInstructionMethodGenerator methodGenerator) {
     
     try {
       analyzer.analyze(instruction);
       String operationName = instruction.getClass().getSimpleName();
       OpcodeReference target = analyzer.getTarget();
       String methodName = nameGenerator.generateUniquMethodName(instruction, operationName, target);

       try {
         methodGenerator.addExecuteMethod(cm, instruction, operationName, target, generatedMethods);
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
       Set<String> generatedMethods,
       IInstructionMethodGenerator methodGenerator) {
     
     try {
       String operationName = instruction.getClass().getSimpleName();
       String methodName = nameGenerator.generateUnaryMethodName(instruction, operationName);

       try {
         methodGenerator.addExecuteUnaryMethod(cm, instruction, operationName, generatedMethods);
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
       Set<String> generatedMethods,
       IInstructionMethodGenerator methodGenerator) {
     
     // Si no hay handler registrado para esta instrucción, no la procesamos
     // Necesitamos verificar antes de intentar generar el método
     if (!new InstructionHandlerRegistry(null, null).hasHandler(instruction)) {
       return null;
     }
     
     String operationName = instruction.getClass().getSimpleName();
     
     try {
       // Intentar procesar con el handler del registry
       boolean processed = methodGenerator.addExecuteGenericMethod(cm, instruction, operationName, generatedMethods);
       
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
    */
   private String generateRegistryMethodName(Instruction instruction, String operationName) {
     StringBuilder methodName = new StringBuilder("execute").append(operationName);
     
     // No agregar sufijo para instrucciones de flag (SCF, CCF)
     if (instruction instanceof SCF || instruction instanceof CCF) {
       return methodName.toString().toLowerCase();
     }
     
     if (instruction instanceof Push pushInstr) {
       OpcodeReference target = pushInstr.getTarget();
       methodName.append(nameGenerator.getReferenceSuffix(target));
     } else if (instruction instanceof Pop popInstr) {
       OpcodeReference target = popInstr.getTarget();
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
                          String operationName, OpcodeReference target, Set<String> generatedMethods);
     
     void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                               String operationName, Set<String> generatedMethods);
     
     boolean addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                    String operationName, Set<String> generatedMethods);
     
     void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                 Map<Integer, String> prefixMethods);
     
     BytecodeInliner getByteocdeInliner();
   }
}
