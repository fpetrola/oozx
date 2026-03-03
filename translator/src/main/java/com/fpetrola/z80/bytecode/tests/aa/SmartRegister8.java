package com.fpetrola.z80.bytecode.tests.aa;

public class SmartRegister8 {
  private final String name;
  private int value = 0;
  private Mode currentMode;

  public SmartRegister8(String name) {
    this.name = name;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value, Mode currentMode) {
    this.value = value;
    this.currentMode = currentMode;
  }

  public Mode getCurrentMode() {
    return currentMode;
  }

  public String getName() {
    return name;
  }
}
