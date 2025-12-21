package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.OpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  public static final String FLAG = "F";
  private final InstructionAnalyzer analyzer;
  private final InstructionClassifier classifier;
  private final DispatchMethodGenerator dispatchGenerator;
  private final MethodNameGenerator nameGenerator;
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;
  private final AluOperationHandler aluOperationHandler;
  private final InstructionProcessorHandler processorHandler;
  private final ExecuteMethodGenerator executeMethodGenerator;
  private final ClassGenerationHandler classGenerationHandler;
  private final FieldManagementHandler fieldManagementHandler;
  private final OpCodeDecoder opcodeDecoder;
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();

  public BytecodeInliner(InstructionAnalyzer analyzer, OpCodeDecoder opcodeDecoder) {
    this.analyzer = analyzer;
    this.opcodeDecoder = opcodeDecoder;
    this.classifier = new InstructionClassifier();
    this.dispatchGenerator = new DispatchMethodGenerator();
    this.nameGenerator = new MethodNameGenerator();
    this.registerValueResolver = new RegisterValueResolver();
    this.memoryAccessHandler = new MemoryAccessHandler();
    this.aluOperationHandler = new AluOperationHandler(registerValueResolver);
    this.processorHandler = new InstructionProcessorHandler(classifier, nameGenerator, analyzer, dispatchGenerator);
    this.executeMethodGenerator = new ExecuteMethodGenerator(analyzer, classifier, registerValueResolver, 
                                                            memoryAccessHandler, aluOperationHandler, nameGenerator);
    this.classGenerationHandler = new ClassGenerationHandler(generatedBytecodes);
    this.fieldManagementHandler = new FieldManagementHandler(analyzer, classifier, aluOperationHandler);
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
    
    // Cada procesamiento obtiene su propio conjunto de métodos para evitar duplicados
    // dentro del MISMO procesamiento
    Set<String> generatedMethods = new HashSet<>();
    
    InstructionProcessingResult result = processorHandler.processInstructions(cm, instructions, 
                                                                             createMethodGenerator(), generatedMethods);
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
                                   String operationName, OpcodeReference target, Set<String> generatedMethods) {
        executeMethodGenerator.addExecuteMethod(cm, instruction, operationName, target, generatedMethods);
      }

      @Override
      public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                        String operationName, Set<String> generatedMethods) {
        executeMethodGenerator.addExecuteUnaryMethod(cm, instruction, operationName, generatedMethods);
      }

      @Override
      public void addExecuteGenericMethod(ClassMaker cm, Instruction instruction, 
                                         String operationName, Set<String> generatedMethods) {
        executeMethodGenerator.addExecuteGenericMethod(cm, instruction, operationName, generatedMethods);
      }

      @Override
      public void addPrefixDispatchMethod(ClassMaker cm, String dispatchMethodName, 
                                          Map<Integer, String> prefixMethods) {
        dispatchGenerator.addPrefixDispatchMethod(cm, dispatchMethodName, prefixMethods);
      }
    };
  }



  /**
   * Retorna el bytecode de la última clase generada
   */
  public byte[] getLastGeneratedBytecode() {
    return classGenerationHandler.getLastGeneratedBytecode();
  }

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
    // Cada procesamiento obtiene su propio conjunto de métodos para evitar duplicados
    // dentro del MISMO procesamiento
    Set<String> generatedMethods = new HashSet<>();
    
    return classGenerationHandler.generateAndLoadMultipleInstructions(className, createMethodGenerator(), 
                                                                      processorHandler, dispatchGenerator, instructions, generatedMethods);
  }

  private String generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = getClassName(instruction, operationName);
    className = className.replace("-", "_");

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

  private void addAluOperationField(ClassMaker cm, TargetSourceInstruction instruction) {
    fieldManagementHandler.addAluOperationField(cm, instruction);
  }

  private void addFieldsInOrder(ClassMaker cm, Map<String, InstructionAnalyzer.VariableInfo> vars, OpcodeReference target) {
    fieldManagementHandler.addFieldsInOrder(cm, vars, target);
  }






  private String getClassName(TargetSourceInstruction instruction, String operationName) {
    // Generar nombre sin sufijo (o agregar sufijo si necesitas múltiples variantes)
    return operationName + "Bytecode";
  }

  /**
   * Extrae todas las instrucciones del OpCodeDecoder incluyendo instrucciones con prefijo
   * (CB, ED, etc.)
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

    // Agregar instrucciones prefijadas con 0xCB si existe
    if (opcodeLookupTable[0xCB] instanceof DefaultFetchNextOpcodeInstruction cbInstruction) {
      Instruction[] cbTable = cbInstruction.getTable();
      for (int idx = 0; idx < cbTable.length; idx++) {
        Instruction instruction = cbTable[idx];
        // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
        int prefixedOpcode = (0xCB << 8) | idx;
        instructions.put(prefixedOpcode, instruction);
      }
    }

    // Agregar instrucciones prefijadas con 0xED si existe
    if (opcodeLookupTable[0xED] instanceof DefaultFetchNextOpcodeInstruction edInstruction) {
      Instruction[] edTable = edInstruction.getTable();
      for (int idx = 0; idx < edTable.length; idx++) {
        Instruction instruction = edTable[idx];
        // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
        int prefixedOpcode = (0xED << 8) | idx;
        if (instruction != null) {
          instructions.put(prefixedOpcode, instruction);
        }
      }
    }

    // Agregar instrucciones prefijadas con 0xED si existe
    if (opcodeLookupTable[0xDD] instanceof DefaultFetchNextOpcodeInstruction edInstruction) {
      Instruction[] edTable = edInstruction.getTable();
      for (int idx = 0; idx < edTable.length; idx++) {
        Instruction instruction = edTable[idx];
        // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
        int prefixedOpcode = (0xDD << 8) | idx;
        if (instruction != null) {
          instructions.put(prefixedOpcode, instruction);
        }
      }
    }

    // Agregar instrucciones prefijadas con 0xED si existe
    if (opcodeLookupTable[0xFD] instanceof DefaultFetchNextOpcodeInstruction edInstruction) {
      Instruction[] edTable = edInstruction.getTable();
      for (int idx = 0; idx < edTable.length; idx++) {
        Instruction instruction = edTable[idx];
        // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
        int prefixedOpcode = (0xFD << 8) | idx;
        if (instruction != null) {
          instructions.put(prefixedOpcode, instruction);
        }
      }
    }

    return instructions;
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
