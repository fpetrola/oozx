package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.ClassMaker;

import java.util.Map;
import java.util.Set;

/**
 * @deprecated Clase no utilizada. Los campos se manejan en otros handlers.
 */
@Deprecated
public class FieldManagementHandler {
  public static final String FLAG = "F";
  
  private final InstructionAnalyzer analyzer;
  private final InstructionClassifier classifier;
  private final AluOperationHandler aluOperationHandler;

  public FieldManagementHandler(InstructionAnalyzer analyzer, InstructionClassifier classifier,
                               AluOperationHandler aluOperationHandler) {
    this.analyzer = analyzer;
    this.classifier = classifier;
    this.aluOperationHandler = aluOperationHandler;
  }

  /**
   * @deprecated No se utiliza
   */
  @Deprecated
  public void addFieldsInOrder(ClassMaker cm, Map<String, InstructionAnalyzer.VariableInfo> vars, 
                               OpcodeReference target) {
    Set<String> excluded = Set.of("F", "Q");

    // 1. Variables en orden manteniendo el de inserción
    for (String name : vars.keySet()) {
      InstructionAnalyzer.VariableInfo var = vars.get(name);

      // Saltar si es excluido o si es pc/memory (los hacemos aparte)
      if (excluded.contains(name) || "memory".equals(name) || "pc".equals(name)) {
        continue;
      }

      Class<?> fieldType = resolveType(var.type);
      cm.addField(fieldType, var.name).private_();
    }

    // 2. Memory
    if (vars.containsKey("memory")) {
      Class<?> fieldType = Memory.class;
      cm.addField(fieldType, "memory").private_();
    }

    // 3. PC
    if (vars.containsKey("pc")) {
      cm.addField(int.class, "pc").private_();
    }
  }

  /**
   * @deprecated No se utiliza
   */
  @Deprecated
  public void addAluOperationField(ClassMaker cm, TargetSourceInstruction instruction) {
    if (classifier.isAluOperation(instruction)) {
      // Add flag field if not already added
      if (!analyzer.getRequiredVariables().containsKey(FLAG)) {
        cm.addField(Register.class, FLAG).private_();
      }

      // Add ALU operation field with the correct type
      String fieldName = aluOperationHandler.getAluOperationFieldName(instruction.getClass().getSimpleName());
      Class<?> aluOperationClass = aluOperationHandler.getAluOperationClass(instruction);
      cm.addField(aluOperationClass, fieldName).private_();
    }
  }

  /**
   * Resuelve nombres de tipo a clases Java
   */
  private Class<?> resolveType(String typeName) {
    return switch (typeName) {
      case "int" -> int.class;
      case "long" -> long.class;
      case "byte" -> byte.class;
      case "short" -> short.class;
      case "boolean" -> boolean.class;
      case "char" -> char.class;
      case "float" -> float.class;
      case "double" -> double.class;
      case "Register" -> Register.class;
      case "Memory" -> Memory.class;
      case "Plain8BitRegister" -> int.class;
      case "Plain16BitRegister" -> int.class;
      default -> Object.class;
    };
  }
}
