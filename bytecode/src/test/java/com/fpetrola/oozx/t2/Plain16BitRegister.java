package com.fpetrola.oozx.t2;

import com.fpetrola.z80.registers.Register;

public class Plain16BitRegister implements Register {
  protected int data;
  private final String name;

  public Plain16BitRegister(String name) {
    this.name = name;
  }

  public int read() {
    return data;
  }

  public void write(int value) {
    this.data = value;
  }

  public String toString() {
    return name;
  }

  public void increment() {
    data++;
  }

  public void decrement() {
    data--;
    data &= 0xffff;
  }

  public int getLength() {
    return 0;
  }

  public String getName() {
    return name;
  }
}
