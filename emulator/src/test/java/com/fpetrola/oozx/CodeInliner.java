package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
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

  public GeneratedCode inlineInstruction(TargetSourceInstruction instruction, String operationName) {
    return generateInlinedClass(instruction, operationName);
  }

  public GeneratedCode inlineLd(Ld ld) {
    return generateInlinedClass(ld, "Ld");
  }

  public GeneratedCode inlineXor(Xor xor) {
    return generateInlinedClass(xor, "Xor");
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

    if (target instanceof MemoryPlusRegister8BitReference memPlusReg) {
      String sourceExpr = getSourceExpression(source);
      String targetRegName = getRegisterName(memPlusReg.getTarget());
      int valueDelta = memPlusReg.getValueDelta();

      if (operationName.equals("Ld")) {
        code.append("          int dd = (byte) memory.read((pc.read() + ")
            .append(valueDelta)
            .append(") & 0xFFFF, 0);\n");
        code.append("          memory.write((")
            .append(targetRegName)
            .append(" + dd) & 0xFFFF, ")
            .append(sourceExpr)
            .append(");\n");
      } else if (operationName.equals("Xor")) {
        code.append("          int dd = (byte) memory.read((pc.read() + ")
            .append(valueDelta)
            .append(") & 0xFFFF, 0);\n");
        code.append("          int value = memory.read((")
            .append(targetRegName)
            .append(" + dd) & 0xFFFF, 0);\n");
        code.append("          A ^= value;\n");
      }
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      String sourceExpr = getSourceExpression(source);
      ImmutableOpcodeReference targetRef = indMem.getTarget();

      if (targetRef instanceof Register targetReg) {
        if (operationName.equals("Ld")) {
          code.append("        memory.write(")
              .append(targetReg.getName())
              .append(", ")
              .append(sourceExpr)
              .append(");\n");
        } else if (operationName.equals("Xor")) {
          code.append("        int value = memory.read(")
              .append(targetReg.getName())
              .append(", 0);\n");
          code.append("        A ^= value;\n");
        }
      } else if (targetRef instanceof Memory16BitReference) {
        if (operationName.equals("Ld")) {
          code.append("        int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);\n");
          code.append("        memory.write(address, ")
              .append(sourceExpr)
              .append(");\n");
        } else if (operationName.equals("Xor")) {
          code.append("        int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);\n");
          code.append("        int value = memory.read(address, 0);\n");
          code.append("        A ^= value;\n");
        }
      }
    }

    return code.toString();
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
