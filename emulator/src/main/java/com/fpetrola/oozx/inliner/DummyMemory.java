package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.memory.Memory;

public class DummyMemory implements Memory {
  @Override
  public int read(int address, int fetching) {
    return 0;
  }

  @Override
  public void write(int address, int value) {

  }

  @Override
  public void reset() {

  }
}
