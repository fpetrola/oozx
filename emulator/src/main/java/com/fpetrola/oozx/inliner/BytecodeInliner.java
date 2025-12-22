package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.OpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  private final InstructionAnalyzer analyzer;
  private final DispatchMethodGenerator dispatchGenerator;
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;
  private final AluOperationHandler aluOperationHandler;
  private final InstructionProcessorHandler processorHandler;
  private final ExecuteMethodGenerator executeMethodGenerator;
  private final ClassGenerationHandler classGenerationHandler;
  private final OpCodeDecoder opcodeDecoder;
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();
  private InstructionProcessingContext processingContext;

  public BytecodeInliner(InstructionAnalyzer analyzer, OpCodeDecoder opcodeDecoder) {
    this.analyzer = analyzer;
    this.opcodeDecoder = opcodeDecoder;
    InstructionClassifier classifier = new InstructionClassifier();
    MethodNameGenerator nameGenerator = new MethodNameGenerator();
    this.dispatchGenerator = new DispatchMethodGenerator();
    this.registerValueResolver = new RegisterValueResolver();
    this.memoryAccessHandler = new MemoryAccessHandler();
    this.aluOperationHandler = new AluOperationHandler(registerValueResolver);
    this.executeMethodGenerator = new ExecuteMethodGenerator(analyzer, classifier, registerValueResolver, 
                                                            memoryAccessHandler, aluOperationHandler, nameGenerator);
    // Crear el handler registry que se usa en processorHandler
    InstructionHandlerRegistry handlerRegistry = new InstructionHandlerRegistry(registerValueResolver, memoryAccessHandler);
    this.processorHandler = new InstructionProcessorHandler(classifier, nameGenerator, analyzer, dispatchGenerator, handlerRegistry);
    this.classGenerationHandler = new ClassGenerationHandler(generatedBytecodes);
    this.processingContext = new InstructionProcessingContext();
  }

  /**
   * Constructor alternativo para mantener compatibilidad con código que no proporciona OpCodeDecoder
   */
  public BytecodeInliner(InstructionAnalyzer analyzer) {
    this(analyzer, null);
  }

  public String inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  /**
    * Genera bytecode para una instrucción usando el registry de handlers
    */
  public String inlineInstructionGeneric(Instruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedGenericClass(instruction, operationName);
  }

  /**
    * Genera una clase con múltiples métodos execute a partir del OpCodeDecoder
    */
  public String inlineMultipleInstructions(String className) {
    if (opcodeDecoder == null) {
      throw new IllegalStateException("OpCodeDecoder no fue proporcionado en el constructor");
    }
    Map<Integer, Instruction> instructions = extractInstructionsFromDecoder();
    return inlineMultipleInstructionsInternal(className, instructions);
  }

  /**
    * Genera una clase con múltiples métodos execute a partir de varias instrucciones (uso interno)
    */
  private String inlineMultipleInstructionsInternal(String className, Map<Integer, Instruction> instructions) {
    className = className.replace("-", "_");
    ClassMaker cm = classGenerationHandler.createBaseClass(className);
    processingContext.clear();

    InstructionProcessingResult result = processorHandler.processInstructions(cm, instructions, 
                                                                              createMethodGenerator());
    dispatchGenerator.addDispatchMethodWithOpcodes(cm, result.opcodeToMethodName, result.prefixOpcodes);
    
    return classGenerationHandler.finializeClass(className, cm);
  }

  /**
    * Crea una implementación anónima de IInstructionMethodGenerator
    */
  private InstructionProcessorHandler.IInstructionMethodGenerator createMethodGenerator() {
    return new InstructionProcessorHandler.IInstructionMethodGenerator() {
      @Override
      public void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, 
                                   String operationName, OpcodeReference target) {
        executeMethodGenerator.addExecuteMethod(cm, instruction, operationName, target, processingContext);
      }

      @Override
      public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                        String operationName) {
        executeMethodGenerator.addExecuteUnaryMethod(cm, instruction, operationName, processingContext);
      }

      @Override
      public boolean addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                            String operationName) {
        return executeMethodGenerator.addExecuteGenericMethod(cm, instruction, operationName, processingContext);
      }

      @Override
      public void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                          Map<Integer, String> prefixMethods) {
        dispatchGenerator.addPrefixDispatchMethod(cm, dispatchMethodName, prefixMethods);
      }

    };
  }


  /**
    * Retorna el conjunto de métodos generados en la clase actual (getter por compatibilidad)
    */
  /**
   * Genera una clase con múltiples instrucciones desde el OpCodeDecoder y la carga en memoria
   * Retorna el Class<?> directamente usable usando finish()
   */
  public Class<?> generateAndLoadMultipleInstructions(String className) {
   if (opcodeDecoder == null) {
     throw new IllegalStateException("OpCodeDecoder no fue proporcionado en el constructor");
   }
   Map<Integer, Instruction> instructions = extractInstructionsFromDecoder();
   return generateAndLoadMultipleInstructionsInternal(className, instructions);
  }

  /**
    * Genera una clase con múltiples instrucciones y la carga en memoria (uso interno)
    * Retorna el Class<?> directamente usable usando finish()
    */
  private Class<?> generateAndLoadMultipleInstructionsInternal(String className, Map<Integer, Instruction> instructions) {
    processingContext.clear();
    return classGenerationHandler.generateAndLoadMultipleInstructions(className, createMethodGenerator(),
                                                                      processorHandler, dispatchGenerator, instructions);
  }

  private String generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = (operationName + "Bytecode").replace("-", "_");

    ClassMaker cm = classGenerationHandler.createBaseClass(className);
    OpcodeReference target = analyzer.getTarget();

    // Add execute method with inlined code
    executeMethodGenerator.addExecuteMethod(cm, instruction, operationName, target);

    return classGenerationHandler.finializeClass(className, cm);
  }

  private String generateInlinedGenericClass(Instruction instruction, String operationName) {
    String className = operationName + "Bytecode";
    className = className.replace("-", "_");

    ClassMaker cm = classGenerationHandler.createBaseClass(className);

    // Add execute method using the registry
    executeMethodGenerator.addExecuteGenericMethod(cm, instruction, operationName, null);

    return classGenerationHandler.finializeClass(className, cm);
  }


  /**
     * Extrae todas las instrucciones del OpCodeDecoder incluyendo instrucciones con prefijo
     * (CB, ED, DD, FD, etc.)
     * @return Mapa de opcode a instrucción
     */
   private Map<Integer, Instruction> extractInstructionsFromDecoder() {
     Map<Integer, Instruction> instructions = new TreeMap<>();

     Instruction[] opcodeLookupTable = opcodeDecoder.getOpcodeLookupTable();

     // Agregar instrucciones principales (sin prefijo)
     for (int idx = 0; idx < opcodeLookupTable.length; idx++) {
       Instruction instruction = opcodeLookupTable[idx];
       instructions.put(idx, instruction);
     }

     // Agregar instrucciones con prefijo
     // Soportados: CB (0xCB), ED (0xED), DD (0xDD), FD (0xFD)
     int[] prefixes = {0xCB, 0xED, 0xDD, 0xFD};
     for (int prefix : prefixes) {
       if (opcodeLookupTable[prefix] instanceof DefaultFetchNextOpcodeInstruction prefixInstruction) {
         addPrefixedInstructions(instructions, prefix, prefixInstruction.getTable());
       }
     }

     return instructions;
   }

   /**
    * Agrega instrucciones prefijadas al mapa
    * @param instructions Mapa donde agregar las instrucciones
    * @param prefix Byte de prefijo (0xCB, 0xED, 0xDD, 0xFD)
    * @param prefixTable Tabla de instrucciones para este prefijo
    */
   private void addPrefixedInstructions(Map<Integer, Instruction> instructions, int prefix, 
                                       Instruction[] prefixTable) {
     for (int idx = 0; idx < prefixTable.length; idx++) {
       Instruction instruction = prefixTable[idx];
       if (instruction != null) {
         // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
         int prefixedOpcode = (prefix << 8) | idx;
         instructions.put(prefixedOpcode, instruction);
       }
     }
   }



  /**
    * Contenedor para los resultados del procesamiento de instrucciones
    */
  public static class InstructionProcessingResult {
    public final Map<Integer, String> opcodeToMethodName;
    public final Map<Integer, String> prefixOpcodes;

    public InstructionProcessingResult(Map<Integer, String> opcodeToMethodName, Map<Integer, String> prefixOpcodes) {
      this.opcodeToMethodName = opcodeToMethodName;
      this.prefixOpcodes = prefixOpcodes;
    }
  }
}
