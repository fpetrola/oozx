package com.fpetrola.z80.bytecode.tests.aa;

public class SmartRegister {
    private final String name;
    private int value16 = 0;
    private int high = 0;
    private int low = 0;
    private Mode currentMode = Mode.MODE_16;

    public SmartRegister(String name) {
        this.name = name;
    }

    public void set16(int val) {
        value16 = val & 0xFFFF;
        currentMode = Mode.MODE_16;
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "set16", false, null);
    }

    public int get16() {
        boolean conv = false;
        String type = null;
        if (currentMode == Mode.MODE_8) {
            value16 = (high << 8) | (low & 0xFF);
            currentMode = Mode.MODE_16;
            conv = true;
            type = "8to16";
        }
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "get16", conv, type);
        return value16;
    }

    public void setHigh(int val) {
        boolean conv = false;
        String type = null;
        if (currentMode == Mode.MODE_16) {
            high = (value16 >> 8) & 0xFF;
            low = value16 & 0xFF;
            currentMode = Mode.MODE_8;
            conv = true;
            type = "16to8";
        }
        high = val & 0xFF;
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "setHigh", conv, type);
    }

    public int getHigh() {
        boolean conv = false;
        String type = null;
        if (currentMode == Mode.MODE_16) {
            high = (value16 >> 8) & 0xFF;
            low = value16 & 0xFF;
            currentMode = Mode.MODE_8;
            conv = true;
            type = "16to8";
        }
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "getHigh", conv, type);
        return high;
    }

    public void setLow(int val) {
        boolean conv = false;
        String type = null;
        if (currentMode == Mode.MODE_16) {
            high = (value16 >> 8) & 0xFF;
            low = value16 & 0xFF;
            currentMode = Mode.MODE_8;
            conv = true;
            type = "16to8";
        }
        low = val & 0xFF;
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "setLow", conv, type);
    }

    public int getLow() {
        boolean conv = false;
        String type = null;
        if (currentMode == Mode.MODE_16) {
            high = (value16 >> 8) & 0xFF;
            low = value16 & 0xFF;
            currentMode = Mode.MODE_8;
            conv = true;
            type = "16to8";
        }
        Z80Registers.recordAccess(name, Z80Registers.getCurrentPC(), "getLow", conv, type);
        return low;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }
}
