package model.connected;

import model.interfaces.IMemory;
import model.tests.TestDriver;

public class ConnectedMemory implements IMemory {
  private final TestDriver testDriver;

  public ConnectedMemory(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public byte read(int address) {
    return testDriver.readMemory(address);
  }

  @Override
  public void write(int address, byte value) {
    testDriver.writeMemory(address, value);
  }

  @Override
  public void pageInROM(byte[] romData) {

  }

  @Override
  public byte[] getROM() {
    return new byte[0];
  }

  @Override
  public byte[] getRAM() {
    return new byte[0];
  }

  @Override
  public boolean isContended(int address, int page) {
    return false;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    return 0;
  }

  @Override
  public void setPage(int slot, int bank) {
    int regBC = testDriver.getRegister("BC");
    int regA = testDriver.getRegister("A");
    testDriver.setRegister("BC", 0x7FFD);
    testDriver.setRegister("A", bank);
    testDriver.addInstruction((byte) 0xED, (byte) 0x79); // OUT (C),A
    testDriver.waitExecution();
    testDriver.setRegister("BC", regBC);
    testDriver.setRegister("A", regA);
  }

  @Override
  public int getPage(int bank) {
    return 0;
  }

  public void enableLEC(boolean b) {
    testDriver.setConnectedLEC(b);
  }

  public boolean isLECEnabled() {
    return testDriver.isConnectedLEC();
  }

  public void setLECMemorySize(int i) {
    testDriver.setLECMemorySize(i);
  }
}
