package com.fpetrola.oozx;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;

import java.util.*;

public class InstructionAnalyzer implements InstructionVisitor<Void> {
  private Map<String, VariableInfo> requiredVariables = new HashMap<>();
  private OpcodeReference currentTarget;
  private ImmutableOpcodeReference currentSource;
  private Register currentFlag;
  private Set<Object> referencedInstances = new HashSet<>();
  
  public void analyze(Ld ld) {
    requiredVariables.clear();
    referencedInstances.clear();
    ld.accept(this);
  }

  @Override
  public void visitingFlag(Register flag, com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction targetSourceInstruction) {
    this.currentFlag = flag;
    addVariable(flag.getName(), "Register", "pc");
    referencedInstances.add(flag);
  }

  @Override
  public void visitingTarget(OpcodeReference target, com.fpetrola.z80.instructions.types.TargetInstruction targetInstruction) {
    this.currentTarget = target;
    referencedInstances.add(target);
    target.accept(this);
  }

  @Override
  public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
    this.currentSource = source;
    referencedInstances.add(source);
    source.accept(this);
  }

  @Override
  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    var target = memoryPlusRegister8BitReference.getTarget();
    referencedInstances.add(target);
    referencedInstances.add(memoryPlusRegister8BitReference.getMemory());
    referencedInstances.add(memoryPlusRegister8BitReference.getPc());
    
    if (target instanceof Register reg) {
      addVariable(reg.getName(), "int", null);
    }
    addVariable("memory", "Memory", null);
    addVariable("pc", "Register", null);
  }

  @Override
  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    var target = indirectMemory8BitReference.getTarget();
    referencedInstances.add(target);
    referencedInstances.add(indirectMemory8BitReference.getMemory());
    
    addVariable("memory", "Memory", null);
    target.accept(this);
  }

  @Override
  public boolean visitRegister(Register register) {
    referencedInstances.add(register);
    addVariable(register.getName(), "int", null);
    return true;
  }

  @Override
  public boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
    referencedInstances.add(memory16BitReference);
    addVariable("memory", "Memory", null);
    addVariable("pc", "Register", null);
    return true;
  }

  @Override
  public void visitingTargetSourceInstruction(TargetSourceInstruction targetSourceInstruction) {
  }

  private void addVariable(String name, String type, String value) {
    if (!requiredVariables.containsKey(name)) {
      requiredVariables.put(name, new VariableInfo(name, type, value));
    }
  }

  public Map<String, VariableInfo> getRequiredVariables() {
    return requiredVariables;
  }

  public OpcodeReference getTarget() {
    return currentTarget;
  }

  public ImmutableOpcodeReference getSource() {
    return currentSource;
  }

  public Register getFlag() {
    return currentFlag;
  }

  public Set<Object> getReferencedInstances() {
    return referencedInstances;
  }

  public static class VariableInfo {
    public final String name;
    public final String type;
    public final String initialValue;

    public VariableInfo(String name, String type, String initialValue) {
      this.name = name;
      this.type = type;
      this.initialValue = initialValue;
    }
  }
}
