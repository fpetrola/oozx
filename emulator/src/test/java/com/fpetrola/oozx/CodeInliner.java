package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.nio.file.Path;
import java.util.*;

/**
 * Genera clases Java con código inlineado extrayendo dinámicamente el código
 * de las instrucciones y referencias usando JavaParser para hacer inlining recursivo.
 */
public class CodeInliner {
  private final InstructionAnalyzer analyzer;
  private final Path sourcePath;
  private final MethodCodeExtractor extractor;

  public CodeInliner(InstructionAnalyzer analyzer, Path sourcePath) {
    this.analyzer = analyzer;
    this.sourcePath = sourcePath;
    this.extractor = new MethodCodeExtractor(sourcePath);
  }

  public GeneratedCode inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  public GeneratedCode inlineLd(Ld ld) {
    return inlineInstruction(ld);
  }

  public GeneratedCode inlineXor(Xor xor) {
    return inlineInstruction(xor);
  }

  public GeneratedCode inlineOr(Or or) {
    return inlineInstruction(or);
  }

  private GeneratedCode generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    StringBuilder sb = new StringBuilder();

    String className = getClassName(instruction, operationName);

    sb.append("public class ").append(className)
        .append(" extends TargetSourceInstruction<ImmutableOpcodeReference> {\n");

    // Get analyzed variables in order
    Map<String, InstructionAnalyzer.VariableInfo> requiredVars = analyzer.getRequiredVariables();
    
    // Orden deseado: source registers, target registers, memory, pc
    OpcodeReference target = analyzer.getTarget();
    printFieldsInOrder(sb, requiredVars, target);

    sb.append("\n");

    // Add constructor
    List<String> constructorParams = buildConstructorParams(target);
    
    sb.append("    public ").append(className).append("(");
    sb.append(String.join(", ", constructorParams));
    sb.append(") {\n");
    
    for (String param : constructorParams) {
      String paramName = param.split(" ")[1];
      sb.append("        this.").append(paramName).append("= ").append(paramName).append(";\n");
    }
    sb.append("    }\n");

    sb.append("\n");

    // Add execute method with inlined code
    sb.append("    public void execute() {\n");
    String executeBody = generateExecuteBody(instruction, operationName, target);
    sb.append(executeBody);
    sb.append("    }\n");

    sb.append("}\n");

    return new GeneratedCode(sb.toString());
  }

  private void printFieldsInOrder(StringBuilder sb, Map<String, InstructionAnalyzer.VariableInfo> vars, OpcodeReference target) {
    // Excluir: F (flag register), Q
    Set<String> excluded = Set.of("F", "Q");
    
    // Determinar si necesitamos blank line antes de pc
    boolean needsBlankLineBeforePc = false;
    if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Memory16BitReference) {
        needsBlankLineBeforePc = true;
      }
    }
    
    // 1. Imprimir variables en orden manteniendo el de inserción
    boolean printedAnything = false;
    for (String name : vars.keySet()) {
      InstructionAnalyzer.VariableInfo var = vars.get(name);
      
      // Saltar si es excluido o si es pc/memory (los hacemos aparte)
      if (excluded.contains(name) || "memory".equals(name) || "pc".equals(name)) {
        continue;
      }
      
      sb.append("    ").append(var.type).append(" ").append(var.name).append(";\n");
      printedAnything = true;
    }
    
    // 2. Memory
    if (vars.containsKey("memory")) {
      InstructionAnalyzer.VariableInfo var = vars.get("memory");
      sb.append("    ").append(var.type).append(" ").append(var.name).append(";\n");
    }
    
    // 3. PC con blank line antes si es Memory16BitReference
    if (vars.containsKey("pc")) {
      if (needsBlankLineBeforePc) {
        sb.append("\n");
        sb.append("     ").append("Register").append(" ").append("pc").append(";\n");
      } else if (target instanceof MemoryPlusRegister8BitReference) {
        sb.append("    ").append("Register").append(" ").append("pc").append(";\n");
      }
    }
  }

  private List<String> buildConstructorParams(OpcodeReference target) {
    List<String> params = new ArrayList<>();
    
    if (target instanceof MemoryPlusRegister8BitReference || target instanceof IndirectMemory8BitReference) {
      params.add("Memory memory");
    }
    
    if (target instanceof MemoryPlusRegister8BitReference) {
      params.add("Register pc");
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Memory16BitReference) {
        params.add("Register pc");
      }
    }
    
    return params;
  }

  private String getClassName(TargetSourceInstruction instruction, String operationName) {
    OpcodeReference target = analyzer.getTarget();
    int suffix = 1;
    if (target instanceof MemoryPlusRegister8BitReference) {
      suffix = 1;
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Register) {
        suffix = 2;
      } else if (indMem.getTarget() instanceof Memory16BitReference) {
        suffix = 3;
      }
    }
    return operationName + suffix;
  }

  private String generateExecuteBody(TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    ImmutableOpcodeReference source = analyzer.getSource();
    StringBuilder code = new StringBuilder();
    
    boolean isAluOperation = isAluOperation(instruction);

    if (target instanceof MemoryPlusRegister8BitReference memPlusReg) {
      String sourceExpr = getSourceExpression(source);
      String targetRegName = getRegisterName(memPlusReg.getTarget());
      int valueDelta = memPlusReg.getValueDelta();

      code.append("          int dd = (byte) memory.read((pc.read() + ")
          .append(valueDelta)
          .append(") & 0xFFFF, 0);\n");

      if (isAluOperation) {
        code.append("          int value = memory.read((")
            .append(targetRegName)
            .append(" + dd) & 0xFFFF, 0);\n");
        code.append(extractAndInlineAluOperation(instruction, "          "));
      } else {
        code.append("          memory.write((")
            .append(targetRegName)
            .append(" + dd) & 0xFFFF, ")
            .append(sourceExpr)
            .append(");\n");
      }
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      String sourceExpr = getSourceExpression(source);
      ImmutableOpcodeReference targetRef = indMem.getTarget();

      if (targetRef instanceof Register targetReg) {
        if (isAluOperation) {
          code.append("        int value = memory.read(")
              .append(targetReg.getName())
              .append(", 0);\n");
          code.append(extractAndInlineAluOperation(instruction, "        "));
        } else {
          code.append("        memory.write(")
              .append(targetReg.getName())
              .append(", ")
              .append(sourceExpr)
              .append(");\n");
        }
      } else if (targetRef instanceof Memory16BitReference) {
        code.append("        int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);\n");
        if (isAluOperation) {
          code.append("        int value = memory.read(address, 0);\n");
          code.append(extractAndInlineAluOperation(instruction, "        "));
        } else {
          code.append("        memory.write(address, ")
              .append(sourceExpr)
              .append(");\n");
        }
      }
    }

    return code.toString();
  }

  /**
   * Detecta si la instrucción es una operación ALU (basada en campos xorTableAluOperation, orTableAluOperation, etc).
   */
  private boolean isAluOperation(TargetSourceInstruction instruction) {
    Class<?> clazz = instruction.getClass();
    try {
      return clazz.getDeclaredField(getAluOperationFieldName(clazz.getSimpleName())) != null;
    } catch (NoSuchFieldException e) {
      return false;
    }
  }

  /**
   * Obtiene el nombre del campo ALU basado en el nombre de la clase de instrucción.
   * Ej: "Xor" -> "xorTableAluOperation", "Or" -> "orTableAluOperation"
   */
  private String getAluOperationFieldName(String instructionClassName) {
    return instructionClassName.toLowerCase() + "TableAluOperation";
  }

  /**
   * Extrae y hace inline del código de la operación ALU de forma dinámica.
   */
  private String extractAndInlineAluOperation(TargetSourceInstruction instruction, String indent) {
    String instructionName = instruction.getClass().getSimpleName();
    String operationField = getAluOperationFieldName(instructionName);
    String defaultOperation = "A " + getDefaultAluOperator(instructionName) + "= value;";
    
    // Extraer el código de la operación, excluyendo variables F y Q
    String operationCode = extractor.extractAnonymousOperationMethodBody(
        instruction.getClass().getName(), 
        operationField, 
        "execute",
        "F", "Q"  // Excluir asignaciones a F y Q
    );
    
    if (operationCode != null && !operationCode.isEmpty()) {
      // Hacer inline del código con indentación correcta
      String[] lines = operationCode.split("\n");
      StringBuilder result = new StringBuilder();
      for (String line : lines) {
        result.append(indent).append(line).append("\n");
      }
      return result.toString();
    }
    
    // Fallback: usar la operación por defecto
    return indent + defaultOperation + "\n";
  }

  /**
   * Obtiene el operador ALU por defecto basado en el nombre de la instrucción.
   * Ej: "Xor" -> "^", "Or" -> "|", "And" -> "&"
   */
  private String getDefaultAluOperator(String instructionClassName) {
    return switch (instructionClassName) {
      case "Xor" -> "^";
      case "Or" -> "|";
      case "And" -> "&";
      case "Add" -> "+";
      case "Sub" -> "-";
      case "Sbc" -> "-";  // Substract with carry
      case "Adc" -> "+";  // Add with carry
      default -> "^";  // Default a XOR
    };
  }

  private String getSourceExpression(ImmutableOpcodeReference source) {
    if (source instanceof Register reg) {
      return reg.getName();
    }
    return "source.read()";
  }

  private String getRegisterName(ImmutableOpcodeReference ref) {
    if (ref instanceof Register reg) {
      return reg.getName();
    }
    return "register";
  }

  public static class GeneratedCode {
    private final String code;

    public GeneratedCode(String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return code;
    }
  }
}
