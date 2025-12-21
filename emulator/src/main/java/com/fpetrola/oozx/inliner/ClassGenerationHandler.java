package com.fpetrola.oozx.inliner;

import com.fpetrola.oozx.Z80UnRolled;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.cojen.maker.ClassMaker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maneja el ciclo de vida completo de generación de clases:
 * creación, compilación, almacenamiento en memoria.
 */
public class ClassGenerationHandler {
  
  private byte[] lastGeneratedBytecode;
  public Map<String, byte[]> generatedBytecodes;

  public ClassGenerationHandler(Map<String, byte[]> generatedBytecodes) {
    this.generatedBytecodes = generatedBytecodes;
  }

  /**
   * Crea la clase base que extiende Z80UnRolled
   */
  public ClassMaker createBaseClass(String className) {
    ClassMaker cm = ClassMaker.beginExternal(className);
    cm.public_();
    cm.extend(Z80UnRolled.class);
    return cm;
  }

  /**
   * Compila la clase y guarda el bytecode
   */
  public String finializeClass(String className, ClassMaker cm) {
    byte[] bytecodeBytes = cm.finishBytes();
    lastGeneratedBytecode = bytecodeBytes;
    generatedBytecodes.put(className, bytecodeBytes);
    return className;
  }

  /**
    * Genera una clase con múltiples instrucciones y la carga en memoria
    * Retorna el Class<?> directamente usable usando finish()
    */
  public Class<?> generateAndLoadMultipleInstructions(String className, 
                                                      InstructionProcessorHandler.IInstructionMethodGenerator methodGenerator,
                                                      InstructionProcessorHandler processorHandler,
                                                      DispatchMethodGenerator dispatchGenerator,
                                                      Map<Integer, Instruction> instructions) {
    className = className.replace("-", "_");
    ClassMaker cm = createBaseClass(className);

    // Crear constructor
    var constructorMaker = cm.addConstructor();
    constructorMaker.invokeSuperConstructor();
    constructorMaker.public_();
    constructorMaker.return_();

    // Procesar instrucciones
    BytecodeInliner.InstructionProcessingResult result = processorHandler.processInstructions(cm, instructions, 
                                                                                                methodGenerator);
    dispatchGenerator.addDispatchMethodWithOpcodes(cm, result.opcodeToMethodName, result.prefixOpcodes);

    // Usar finish() para cargar la clase directamente en memoria
    return cm.finish();
  }

  /**
   * Retorna el bytecode de la última clase generada
   */
  public byte[] getLastGeneratedBytecode() {
    return lastGeneratedBytecode;
  }

  /**
   * Extrae el bytecode de una clase compilada usando el ClassLoader
   */
  public byte[] extractBytecodeFromCompiledClass(Class<?> clazz) {
    try {
      String resourcePath = clazz.getName().replace('.', '/') + ".class";
      var resourceStream = clazz.getClassLoader().getResourceAsStream(resourcePath);

      if (resourceStream != null) {
        return resourceStream.readAllBytes();
      }
    } catch (Exception e) {
      // Ignorar
    }

    return null;
  }
}
