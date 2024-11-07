package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import z80core.MemIoOps;

final class IOImplementation<T extends WordNumber> implements IO<T> {
  private MemIoOps memIoOps;
  private int[] ports = new int[0x10000];

  public IOImplementation(MemIoOps memory) {
    this.memIoOps = memory;
  }

  public void out(T port, T value) {
    memIoOps.outPort(port.intValue(), value.intValue());
  }

  public T in(T port) {
    T value = WordNumber.createValue(memIoOps.inPort(port.intValue()));
    //if (value.intValue() != 255 && value.intValue() != 191)
    //if (port.intValue() == 49150)

    int port1 = ports[port.intValue()];
    if (value.intValue() != port1 && port.intValue() == 31)
      System.out.println(port + "= " + value.intValue());

    ports[port.intValue()]= value.intValue();

    return value;
  }
}