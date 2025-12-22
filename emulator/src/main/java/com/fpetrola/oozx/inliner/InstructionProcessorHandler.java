package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maneja el procesamiento de instrucciones: análisis de prefijos, clasificación
 * y generación de métodos execute correspondientes.
 */
public class InstructionProcessorHandler {
  private final InstructionClassifier classifier;
  private final DispatchMethodGenerator dispatchGenerator;
  private final InstructionDispatcher dispatcher;

  public InstructionProcessorHandler(InstructionClassifier classifier, MethodNameGenerator nameGenerator, 
                                     InstructionAnalyzer analyzer, DispatchMethodGenerator dispatchGenerator,
                                     InstructionHandlerRegistry handlerRegistry) {
    this.classifier = classifier;
    this.dispatchGenerator = dispatchGenerator;
    this.dispatcher = new InstructionDispatcher(classifier, analyzer, handlerRegistry, nameGenerator);
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
    
    int prefixByte = (opcode >> 8) & 0xFF;
    int nextOpcode = opcode & 0xFF;
    
    String methodName = dispatcher.processInstruction(cm, instruction, methodGenerator);
    if (methodName != null) {
      prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
    }
  }

  /**
    * Procesa una instrucción no prefijada
    */
   private void processNonPrefixedInstruction(
      ClassMaker cm, Instruction instruction, Integer opcode,
      Map<Integer, String> opcodeToMethodName,
      IInstructionMethodGenerator methodGenerator) {
     
   if (instruction == null) {
     return;
   }
   
   String methodName = dispatcher.processInstruction(cm, instruction, methodGenerator);
   if (methodName != null) {
     opcodeToMethodName.put(opcode, methodName);
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
     
     boolean addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                    String operationName);
     
     void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                 Map<Integer, String> prefixMethods);

  }
}
