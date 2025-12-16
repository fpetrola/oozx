package com.fpetrola.oozx.t2;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;

public class Memory8BitReference implements ImmutableOpcodeReference {
  private final Memory memory;
  private final int delta;
  private final Register pc;

  public Memory8BitReference(Memory memory, Register pc, int delta) {
    this.memory = memory;
    this.pc = pc;
    this.delta = delta;
  }

  final public int read() {
    return memory.read((pc.read() + delta) & 0xFFFF, 0);
  }

  final public void write(int value) {
    memory.write(pc.read(), value);
  }

  public int getLength() {
    return 1;
  }
}