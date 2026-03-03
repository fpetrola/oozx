package com.fpetrola.z80.bytecode.tests.aa;

public class SmartRegister16 {
  private final String name;
  private int value16 = 0;
  private SmartRegister8 high;
  private SmartRegister8 low;
  private Mode currentMode = Mode.MODE_16;

  public SmartRegister16(String name) {
    this.name = name;
    high = new SmartRegister8(getHighRegisterName(name));
    low = new SmartRegister8(getLowRegisterName(name));
  }

  private String getLowRegisterName(String name) {
    if (name.startsWith("I")) {
      return name + "L";
    } else
      return name.substring(1, 2);
  }

  private String getHighRegisterName(String name) {
    if (name.startsWith("I")) {
      return name + "H";
    } else
      return name.substring(0, 1);
  }

  public void set16(int val) {
    value16 = val & 0xFFFF;
    currentMode = Mode.MODE_16;
    high.setValue((value16 >> 8) & 0xFF, currentMode);
    low.setValue(value16 & 0xFF, currentMode);
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "set" + name, false, null);
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
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "get" + name, conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value16;
  }

  public void setHigh(int val) {
    high.setValue(val, Mode.MODE_8);
    currentMode = Mode.MODE_8;
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "set" + high.getName(), false, null);
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
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "get" + high.getName(), conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value;
  }

  public void setLow(int val) {
    low.setValue(val, Mode.MODE_8);
    currentMode = Mode.MODE_8;
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "set" + low.getName(), false, null);
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
    Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "get" + low.getName(), conv, type);
    Z80Registers.recordMode(name, Z80Registers.getCurrentPC(), currentMode);
    return value;
  }

  public Mode getCurrentMode() {
    return currentMode;
  }
}
