package com.fpetrola.oozx.t2;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;

public final class IndirectMemory8BitReference implements OpcodeReference {
  private final ImmutableOpcodeReference target;
  private final Memory memory;
  public int address;

  public IndirectMemory8BitReference(ImmutableOpcodeReference target, Memory memory) {
    this.target = target;
    this.memory = memory;
  }

  public int read() {
    address = target.read();
    return memory.read(address, 0);
  }

  public void write(int value) {
    address = target.read();
    memory.write(address, value);
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