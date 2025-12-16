package com.fpetrola.oozx.t2;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;

public class MemoryPlusRegister8BitReference implements OpcodeReference {
  final private Memory memory;
  final private ImmutableOpcodeReference target;
  final protected int valueDelta;
  final private Register pc;

  public MemoryPlusRegister8BitReference(ImmutableOpcodeReference target, Memory memory, Register pc, int valueDelta) {
    this.target = target;
    this.memory = memory;
    this.pc = pc;
    this.valueDelta = valueDelta;
  }

  final public int read() {
    int address = (target.read() + (int) fetchRelative()) & 0xFFFF;
    return memory.read(address, 0);
  }

  final public void write(int value) {
    int address = (target.read() + (int) fetchRelative()) & 0xFFFF;
    memory.write(address, value);
  }

  public byte fetchRelative() {
    return (byte) memory.read((pc.read() + valueDelta) & 0xFFFF, 0);
  }

  public int getLength() {
    return 1;
  }
}
