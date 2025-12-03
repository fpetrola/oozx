package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CodeInliner {
  private final InstructionAnalyzer analyzer;
  private final Path path;

  public CodeInliner(InstructionAnalyzer analyzer, Path path) {
    this.analyzer = analyzer;
    this.path = path;
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
    
    // Get the class name based on the instruction type
    String className = getClassName(instruction, operationName);
    
    sb.append("public class ").append(className)
        .append(" extends TargetSourceInstruction<ImmutableOpcodeReference> {\n");

    // Add fields
    ImmutableOpcodeReference source = analyzer.getSource();
    OpcodeReference target = analyzer.getTarget();

    // Add source field
    if (source instanceof Register sourceReg) {
      sb.append("    int ").append(sourceReg.getName()).append(";\n");
    }

    // Add target register field
    if (target instanceof MemoryPlusRegister8BitReference memPlusReg) {
      ImmutableOpcodeReference targetRef = memPlusReg.getTarget();
      if (targetRef instanceof Register targetReg) {
        sb.append("    int ").append(targetReg.getName()).append(";\n");
      }
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      ImmutableOpcodeReference targetRef = indMem.getTarget();
      if (targetRef instanceof Register targetReg) {
        sb.append("    int ").append(targetReg.getName()).append(";\n");
      } else if (targetRef instanceof Memory16BitReference mem16) {
        // Memory16BitReference - extract the register from it
        ImmutableOpcodeReference mem16Ref = mem16.getPc();
        if (mem16Ref instanceof Register targetReg) {
          sb.append("    int ").append(targetReg.getName()).append(";\n");
        }
      }
    }

    // Add memory field if needed
    boolean needsMemory = false;
    if (target instanceof MemoryPlusRegister8BitReference || target instanceof IndirectMemory8BitReference) {
      needsMemory = true;
      sb.append("    Memory memory;\n");
    }

    // Add pc field if needed
    boolean needsPc = false;
    if (target instanceof MemoryPlusRegister8BitReference) {
      needsPc = true;
      sb.append("    Register pc;\n");
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Memory16BitReference) {
        needsPc = true;
        sb.append("\n");  // Add blank line before pc
        sb.append("     Register pc;\n");
      }
    }

    sb.append("\n");

    // Add constructor
    sb.append("    public ").append(className).append("(");
    List<String> constructorParams = new ArrayList<>();
    if (needsMemory) {
      constructorParams.add("Memory memory");
    }
    if (needsPc) {
      constructorParams.add("Register pc");
    }
    sb.append(String.join(", ", constructorParams));
    sb.append(") {\n");
    if (needsMemory) {
      sb.append("        this.memory= memory;\n");
    }
    if (needsPc) {
      sb.append("        this.pc= pc;\n");
    }
    sb.append("    }\n");

    sb.append("\n");

    // Add execute method
    sb.append("    public void execute() {\n");
    sb.append(generateExecuteBody(instruction, operationName));
    sb.append("    }\n");

    sb.append("}\n");

    return new GeneratedCode(sb.toString());
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

  private String generateExecuteBody(TargetSourceInstruction instruction, String operationName) {
    OpcodeReference target = analyzer.getTarget();
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
