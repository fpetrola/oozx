package model.connected;

import machine.MachineTypes;
import model.interfaces.IZ80CPU;
import model.tests.TestDriver;

// Concrete implementations for testing
public class ConnectedZ80CPU implements IZ80CPU {
  private final TestDriver testDriver;

  public ConnectedZ80CPU(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public void setModel(String model) {
    switch (model) {
      case "48K":
        testDriver.selectHardwareModel(MachineTypes.SPECTRUM48K);
        break;
      case "128K":
        testDriver.selectHardwareModel(MachineTypes.SPECTRUM128K);
        break;
      case "+3":
        testDriver.selectHardwareModel(MachineTypes.SPECTRUMPLUS3);
        break;
      default:
        throw new IllegalArgumentException("Unsupported model: " + model);
    }
  }

  @Override
  public void reset() {
    testDriver.resetZ80();
  }

  @Override
  public void executeCycle() {
  }

  @Override
  public int getPC() {
    return testDriver.getRegister("PC");
  }

  @Override
  public void setPC(int pc) {
    testDriver.setRegister("PC", pc);
  }

  @Override
  public byte getRegisterA() {
    return (byte) testDriver.getRegister("A");
  }

  public void setRegisterA(int value) {
    testDriver.setRegister("A", value);
  }

  public void setHL(int hl) {
    testDriver.setRegister("HL", hl);
  }

  public void setSP(int sp) {
    testDriver.setRegister("SP", sp);
  }

  @Override
  public void setDE(int i) {
    testDriver.setRegister("DE", i);
  }

  @Override
  public void setBC(int i) {
    testDriver.setRegister("BC", i);
  }

  @Override
  public int getHL() {
    return testDriver.getRegister("HL");
  }

  @Override
  public int getDE() {
    return testDriver.getRegister("DE");
  }

  @Override
  public void setIR(int i) {
    testDriver.setRegister("IR", i);
  }

  @Override
  public byte in(int port) {
    return -1;
  }

  @Override
  public void out(int port, byte value) {
    testDriver.writePort(port, value);
//
//    // OUT (c),A
//    int regBC = getRegBC();
//    int regA = getRegA();
//    setRegBC(port);
//    setRegisterA(value);
//    testDriver.addInstruction((byte) 0xED, (byte) 0x79); // OUT (C),A
//    testDriver.waitExecution();
//    setRegBC(regBC);
//    setRegisterA(regA);
  }

  private void setRegBC(int port) {
    testDriver.setRegister("BC", port);
  }

  private int getRegA() {
    return (byte) testDriver.getRegister("A");
  }

  private int getRegBC() {
    return (byte) testDriver.getRegister("BC");
  }

  @Override
  public byte readMemory(int address) {
    return testDriver.readMemory(address, true);
//    int regHL = getRegHL();
//    setHL(address);
//    testDriver.addInstruction((byte) 0x7E); // LD A,(HL)
//    testDriver.waitExecution();
//    setHL(regHL);
//    return (byte) getRegA();
//    return testDriver.spectrum.getMemory().readByte(address);
  }

  private int getRegHL() {
    return (byte) testDriver.getRegister("HL");
  }

  @Override
  public void writeMemory(int address, byte value, boolean contended) {
    testDriver.writeMemory(address, value, contended);

//    int regHL = getRegHL();
//    int regA = getRegA();
//    setHL(address);
//    setRegisterA(value);
//    testDriver.addInstruction((byte) 0x77); // LD (HL),A
//    testDriver.waitExecution();
//    setHL(regHL);
//    setRegisterA(regA);
  }

  @Override
  public int getTStates() {
    return testDriver.getTstates();
  }

  @Override
  public void setTStates(int tStates) {
    testDriver.setTstates(tStates);
  }

  @Override
  public void addTStates2(int tStates) {
    testDriver.setTstates(getTStates() + tStates);
  }

  @Override
  public void executeInstruction(String opcode, int[] operands) {
    switch (opcode) {
      case "LD (HL),A":
        testDriver.addInstruction((byte) 0x77);
        break;
      case "INC (HL)":
        testDriver.addInstruction((byte) 0x34);
        break;
      case "LDI":
        testDriver.addInstruction((byte) 0xED, (byte) 0xA0);
        break;
      case "IN A,(n)":
        testDriver.addInstruction((byte) 0xDB, (byte) operands[0]);
        break;
      case "CALL nn":
        testDriver.addInstruction((byte) 0xCD, (byte) (operands[0] & 0xFF), (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "ADD HL,BC":
        testDriver.addInstruction((byte) 0x09);
        break;
      case "INC DE":
        testDriver.addInstruction((byte) 0x13);
        break;
    }

    testDriver.waitExecution();
  }
}

