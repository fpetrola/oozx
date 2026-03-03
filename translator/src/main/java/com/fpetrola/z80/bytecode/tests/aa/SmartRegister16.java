package com.fpetrola.z80.bytecode.tests.aa;

public class SmartRegister16 {
  private final String name;
  private int value16 = 0;
  private SmartRegister8 high;
  private SmartRegister8 low;
  private Mode currentMode = Mode.MODE_16;

  public SmartRegister16(String name) {
    this.name = name;
    high = new SmartRegister8(name.substring(0, 1));
    low = new SmartRegister8(name.substring(1, 2));
  }

  public void set16(int val) {
    value16 = val & 0xFFFF;
    currentMode = Mode.MODE_16;
    high.setValue((value16 >> 8) & 0xFF, currentMode);
    low.setValue(value16 & 0xFF, currentMode);

    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "set16", false, null);
  }

  public int get16() {
    boolean conv = false;
    String type = null;
    if (currentMode == Mode.MODE_8) {
      value16 = (high.getValue() << 8) | (low.getValue() & 0xFF);
      currentMode = Mode.MODE_16;
      conv = true;
      // Registrar el currentMode de cada parte
      String highSrc = high.getCurrentMode() == Mode.MODE_16 ? "16" : "8";
      String lowSrc = low.getCurrentMode() == Mode.MODE_16 ? "16" : "8";
      type = "8to16_high_from_" + highSrc + "_low_from_" + lowSrc;
    }
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "get16", conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value16;
  }

  public void setHigh(int val) {
    boolean conv = false;
    String type = null;
    high.setValue(val, Mode.MODE_8);
    currentMode = Mode.MODE_8;
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "setHigh", conv, type);
  }

  public int getHigh() {
    boolean conv = false;
    String type = null;
    int value = high.getValue();

    if (high.getCurrentMode() == Mode.MODE_16) {
      conv = true;
      type = "16to8";
      value = value16 >> 8 & 0xFF;
    }
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "getHigh", conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value;
  }

  public void setLow(int val) {
    boolean conv = false;
    String type = null;
    low.setValue(val, Mode.MODE_8);
    currentMode = Mode.MODE_8;
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "setLow", conv, type);
  }

  public int getLow() {
    boolean conv = false;
    String type = null;
    int value = low.getValue();

    if (low.getCurrentMode() == Mode.MODE_16) {
      conv = true;
      type = "16to8";
      value = value16 & 0xFF;
    }
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "getLow", conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value;
  }

  public Mode getCurrentMode() {
    return currentMode;
  }
}
