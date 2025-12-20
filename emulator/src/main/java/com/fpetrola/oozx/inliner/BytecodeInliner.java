package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.HashMap;
import java.util.Map;

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
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();

  public BytecodeInliner(InstructionAnalyzer analyzer) {
    this.analyzer = analyzer;
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

  public String inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  /**
   * Genera una clase con múltiples métodos execute a partir de varias instrucciones
   */
  public String inlineMultipleInstructions(String className, Map<Integer, Instruction> instructions) {
    className = className.replace("-", "_");
    ClassMaker cm = classGenerationHandler.createBaseClass(className);
    
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
        executeMethodGenerator.addExecuteMethod(cm, instruction, operationName, target);
      }

      @Override
      public void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, 
                                        String operationName) {
        executeMethodGenerator.addExecuteUnaryMethod(cm, instruction, operationName);
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
   * Genera una clase con múltiples instrucciones y la carga en memoria
   * Retorna el Class<?> directamente usable usando finish()
   */
  public Class<?> generateAndLoadMultipleInstructions(String className, Map<Integer, Instruction> instructions) {
    return classGenerationHandler.generateAndLoadMultipleInstructions(className, createMethodGenerator(), 
                                                                     processorHandler, dispatchGenerator, instructions);
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
