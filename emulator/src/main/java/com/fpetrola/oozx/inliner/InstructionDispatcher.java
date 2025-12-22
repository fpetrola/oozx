package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;

import java.util.Map;

/**
 * Centraliza la lógica de procesamiento de instrucciones.
 * Todas las instrucciones pasan por aquí para determinar el flujo correcto.
 */
public class InstructionDispatcher {
  private final InstructionClassifier classifier;
  private final InstructionAnalyzer analyzer;
  private final InstructionHandlerRegistry handlerRegistry;
  private final MethodNameGenerator nameGenerator;

  public InstructionDispatcher(InstructionClassifier classifier, InstructionAnalyzer analyzer,
                              InstructionHandlerRegistry handlerRegistry, MethodNameGenerator nameGenerator) {
    this.classifier = classifier;
    this.analyzer = analyzer;
    this.handlerRegistry = handlerRegistry;
    this.nameGenerator = nameGenerator;
  }

  /**
   * Procesa una instrucción y retorna el nombre del método generado, o null si no se pudo procesar.
   * Este es el punto único de entrada para toda la lógica de procesamiento.
   */
  public String processInstruction(ClassMaker cm, Instruction instruction, 
                                   InstructionProcessorHandler.IInstructionMethodGenerator methodGenerator) {
    // Caso 1: TargetSourceInstruction
    if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
      return processTargetSourceInstruction(cm, targetSourceInstruction, methodGenerator);
    }
    
    // Caso 2: ParameterizedUnaryAluInstruction
    if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
      return processUnaryInstruction(cm, unaryInstruction, methodGenerator);
    }
    
    // Caso 3: Instrucciones del registry (Push, Pop, etc.)
    if (handlerRegistry.hasHandler(instruction)) {
      return processRegistryInstruction(cm, instruction, methodGenerator);
    }
    
    // No se pudo procesar
    return null;
  }

  /**
   * Procesa una TargetSourceInstruction.
   * Intenta primero con handlers específicos, luego con el procesamiento genérico.
   */
  private String processTargetSourceInstruction(ClassMaker cm, 
                                               TargetSourceInstruction<?> instruction,
                                               InstructionProcessorHandler.IInstructionMethodGenerator methodGenerator) {
    try {
      String operationName = instruction.getClass().getSimpleName();
      
      // Si tiene handler específico (como Ex), intentar primero
      if (handlerRegistry.hasHandler(instruction) && instruction instanceof Ex) {
        String methodName = processRegistryInstruction(cm, instruction, methodGenerator);
        if (methodName != null) {
          return methodName;
        }
      }
      
      // Procesamiento estándar para TargetSourceInstruction
      analyzer.analyze(instruction);
      OpcodeReference target = analyzer.getTarget();
      
      try {
        methodGenerator.addExecuteMethod(cm, instruction, operationName, target);
        return nameGenerator.generateUniquMethodName(instruction, operationName, target);
      } catch (ClassFormatError e) {
        // Método duplicado, reutilizar nombre
        if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
          return nameGenerator.generateUniquMethodName(instruction, operationName, target);
        }
        throw e;
      }
    } catch (Exception e) {
      InstructionProcessingLogger.logTargetSourceInstructionError(
        instruction.getClass().getSimpleName(), 
        e.getClass().getSimpleName(), 
        e.getMessage());
      InstructionProcessingLogger.logProcessingError(instruction.getClass().getSimpleName(), e);
      return null;
    }
  }

  /**
   * Procesa una ParameterizedUnaryAluInstruction.
   */
  private String processUnaryInstruction(ClassMaker cm, 
                                        ParameterizedUnaryAluInstruction instruction,
                                        InstructionProcessorHandler.IInstructionMethodGenerator methodGenerator) {
    try {
      String operationName = instruction.getClass().getSimpleName();
      
      try {
        methodGenerator.addExecuteUnaryMethod(cm, instruction, operationName);
        return nameGenerator.generateUnaryMethodName(instruction, operationName);
      } catch (ClassFormatError e) {
        // Método duplicado, reutilizar nombre
        if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
          return nameGenerator.generateUnaryMethodName(instruction, operationName);
        }
        throw e;
      }
    } catch (Exception e) {
      InstructionProcessingLogger.logUnaryInstructionError(
        instruction.getClass().getSimpleName(), 
        e.getClass().getSimpleName(), 
        e.getMessage());
      return null;
    }
  }

  /**
   * Procesa una instrucción usando handlers del registry.
   */
  private String processRegistryInstruction(ClassMaker cm, Instruction instruction,
                                           InstructionProcessorHandler.IInstructionMethodGenerator methodGenerator) {
    if (!handlerRegistry.hasHandler(instruction)) {
      return null;
    }
    
    String operationName = instruction.getClass().getSimpleName();
    
    try {
      boolean processed = methodGenerator.addExecuteGenericMethod(cm, instruction, operationName);
      if (!processed) {
        return null;
      }
      return nameGenerator.generateMethodName(instruction, operationName);
    } catch (ClassFormatError e) {
      // Manejo especial para métodos duplicados
      if (e.getMessage() != null && e.getMessage().contains("Duplicate method")) {
        return nameGenerator.generateMethodName(instruction, operationName);
      }
      InstructionProcessingLogger.logClassFormatError(operationName, e.getMessage());
      return null;
    } catch (Exception e) {
      InstructionProcessingLogger.logRegistryInstructionError(operationName, 
                                                             e.getClass().getSimpleName());
      return null;
    }
  }
}
