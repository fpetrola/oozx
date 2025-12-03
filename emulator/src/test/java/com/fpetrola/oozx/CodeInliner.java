package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.nio.file.Path;
import java.util.*;

public class CodeInliner {
  private final InstructionAnalyzer analyzer;
  private final Path path;

  public CodeInliner(InstructionAnalyzer analyzer, Path path) {
    this.analyzer = analyzer;
    this.path = path;
  }

  public GeneratedCode inlineLd(Ld ld) {
    StringBuilder sb = new StringBuilder();
    sb.append("public class Ld extends TargetSourceInstruction<ImmutableOpcodeReference> {\n");

    // Add fields in specific order
    ImmutableOpcodeReference source = analyzer.getSource();
    OpcodeReference target = analyzer.getTarget();

    // Add source field
    if (source instanceof Register sourceReg) {
      sb.append("int ").append(sourceReg.getName()).append(";\n");
    }

    // Add target register field
    if (target instanceof MemoryPlusRegister8BitReference memPlusReg) {
      ImmutableOpcodeReference targetRef = memPlusReg.getTarget();
      if (targetRef instanceof Register targetReg) {
        sb.append("int ").append(targetReg.getName()).append(";\n");
      }
    }

    // Add memory field if needed
    if (target instanceof MemoryPlusRegister8BitReference || target instanceof IndirectMemory8BitReference) {
      sb.append("Memory memory;\n");
    }

    // Add pc field if needed
    if (target instanceof MemoryPlusRegister8BitReference) {
      sb.append("Register pc;\n");
    }

    // Add constructor
    sb.append("    public Ld(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {\n");
    sb.append("        super(target, source, flag);\n");
    sb.append("    }\n");

    // Add execute method
    sb.append("    public void execute() {\n");
    sb.append(generateExecuteBody(ld));
    sb.append("    }\n");

    // Add accept method
    sb.append("    public void accept(InstructionVisitor visitor) {\n");
    sb.append("        super.accept(visitor);\n");
    sb.append("        visitor.visitingLd(this);\n");
    sb.append("    }\n");

    sb.append("}\n");

    return new GeneratedCode(sb.toString());
  }

  private String generateExecuteBody(Ld ld) {
    OpcodeReference target = analyzer.getTarget();
    ImmutableOpcodeReference source = analyzer.getSource();

    StringBuilder code = new StringBuilder();
    code.append("        {\n");

    // Load source value
    String sourceExpr = getSourceExpression(source);
    code.append("         int value= ").append(sourceExpr).append(";\n");

    // Handle target operations
    if (target instanceof MemoryPlusRegister8BitReference memPlusReg) {
      String targetName = getRegisterName(memPlusReg.getTarget());
      int valueDelta = memPlusReg.getValueDelta();

      code.append("         int valueDelta= ").append(valueDelta).append(";\n");
      code.append("            int dd = (byte) memory.read((pc.read() + ").append(valueDelta).append(") & 0xFFFF, 0);\n");
      code.append("    memory.write((").append(targetName).append(" + dd) & 0xFFFF, value);\n");
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      String targetExpr = getTargetExpression(indMem.getTarget());
      code.append("     memory.write(").append(targetExpr).append(", value);\n");
    }

    code.append("        }\n");
    return code.toString();
  }

  private String getSourceExpression(ImmutableOpcodeReference source) {
    if (source instanceof Register reg) {
      return reg.getName();
    }
    return "source.read()";
  }

  private String getTargetExpression(ImmutableOpcodeReference target) {
    if (target instanceof Register reg) {
      return reg.getName();
    } else if (target instanceof Memory16BitReference mem) {
      return "memory.read(pc.read() + 3, 0)";
    }
    return "target.read()";
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
