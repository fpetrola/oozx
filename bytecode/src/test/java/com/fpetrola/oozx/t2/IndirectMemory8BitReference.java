package com.fpetrola.oozx.t2;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;

public final class IndirectMemory8BitReference implements OpcodeReference {
  private final ImmutableOpcodeReference target;
  private final Memory memory;

  public IndirectMemory8BitReference(ImmutableOpcodeReference target, Memory memory) {
    this.target = target;
    this.memory = memory;
  }

  public int read() {
    return memory.read(target.read(), 0);
  }

  public void write(int value) {
    memory.write(target.read(), value);
  }

  public int getLength() {
    return target.getLength();
  }

  public Memory getMemory() {
    return memory;
  }

  public ImmutableOpcodeReference getTarget() {
    return target;
  }
}