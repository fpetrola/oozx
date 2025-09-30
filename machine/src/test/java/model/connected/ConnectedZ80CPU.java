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
  public void setB(int b) {
    setHighRegister(b, "BC");
  }

  @Override
  public int getB() {
    return getHigh("BC");
  }

  @Override
  public void setZeroFlag(boolean b) {
//    if (b)
//      executeInstruction("XOR A");
//    else {
      int af = testDriver.getRegister("AF");
      testDriver.setRegister("AF", b ? (af | 64) : af & ~64);
//    }
  }

  @Override
  public int getBC() {
    return testDriver.getRegister("BC");
  }

  @Override
  public void step() {
    testDriver.step();
  }

  private int getHigh(String bc) {
    return (testDriver.getRegister(bc) & 0xffff) >> 8;
  }

  private void setHighRegister(int value8, String register16BitsName) {
    int value16 = testDriver.getRegister(register16BitsName);
    testDriver.setRegister(register16BitsName, (value8 & 0xff) << 8 | (value16 & 0xff));
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
  public void executeInstruction(String opcode, int... operands) {
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
        testDriver.addInstruction((byte) 0xCD,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "ADD HL,BC":
        testDriver.addInstruction((byte) 0x09);
        break;
      case "INC DE":
        testDriver.addInstruction((byte) 0x13);
        break;
      case "DJNZ n":
        testDriver.addInstruction((byte) 0x10, (byte) operands[0]);
        break;
      case "XOR A":
        testDriver.addInstruction((byte) 0xAF);
        break;
      case "DEC A":
        testDriver.addInstruction((byte) 0x3D);
        break;
      case "JP Z,nn":
        testDriver.addInstruction((byte) 0xCA,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "LDIR":
        testDriver.addInstruction((byte) 0xED, (byte) 0xB0);
        break;

      // === JR family ===
      case "JR n":
        testDriver.addInstruction((byte) 0x18, (byte) operands[0]);
        break;
      case "JR NZ,n":
        testDriver.addInstruction((byte) 0x20, (byte) operands[0]);
        break;
      case "JR Z,n":
        testDriver.addInstruction((byte) 0x28, (byte) operands[0]);
        break;
      case "JR NC,n":
        testDriver.addInstruction((byte) 0x30, (byte) operands[0]);
        break;
      case "JR C,n":
        testDriver.addInstruction((byte) 0x38, (byte) operands[0]);
        break;

      // === JP family ===
      case "JP nn":
        testDriver.addInstruction((byte) 0xC3,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "JP NZ,nn":
        testDriver.addInstruction((byte) 0xC2,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "JP NC,nn":
        testDriver.addInstruction((byte) 0xD2,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "JP C,nn":
        testDriver.addInstruction((byte) 0xDA,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;

      // === CALL family ===
      case "CALL NZ,nn":
        testDriver.addInstruction((byte) 0xC4,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "CALL Z,nn":
        testDriver.addInstruction((byte) 0xCC,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "CALL NC,nn":
        testDriver.addInstruction((byte) 0xD4,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;
      case "CALL C,nn":
        testDriver.addInstruction((byte) 0xDC,
            (byte) (operands[0] & 0xFF),
            (byte) ((operands[0] >> 8) & 0xFF));
        break;

      default:
        throw new RuntimeException("instruction not found: " + opcode);
    }

    testDriver.waitExecution();
  }
}

