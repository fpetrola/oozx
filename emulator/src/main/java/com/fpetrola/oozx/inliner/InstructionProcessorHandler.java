package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
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
    
    Map<Integer, String> opcodeToMethodName = new LinkedHashMap<>();
    Map<Integer, String> prefixOpcodes = new LinkedHashMap<>();
    Map<Integer, Map<Integer, String>> prefixedInstructions = new LinkedHashMap<>();
    Set<String> generatedMethods = new HashSet<>();

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

      if (!generatedMethods.contains(methodName)) {
        methodGenerator.addExecuteMethod(cm, instruction, operationName, target);
        generatedMethods.add(methodName);
      }
      return methodName;
    } catch (Exception e) {
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

      if (!generatedMethods.contains(methodName)) {
        methodGenerator.addExecuteUnaryMethod(cm, instruction, operationName);
        generatedMethods.add(methodName);
      }
      return methodName;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Interfaz para abstracción de generación de métodos
   */
  public interface IInstructionMethodGenerator {
    void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                         String operationName, OpcodeReference target);
    
    void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                              String operationName);
    
    void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                Map<Integer, String> prefixMethods);
  }
}
