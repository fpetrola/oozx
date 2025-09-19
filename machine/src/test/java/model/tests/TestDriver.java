package model.tests;

import com.fpetrola.oozx.fuse.*;
import machine.MachineTypes;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestDriver {
  public volatile boolean finished = false;
  private Queue<SintheticInstruction> instructions = new LinkedBlockingQueue<>();
  private boolean skipFetch = true;
  private boolean executingInstruction= false;
  private final CommandHandler commandHandler;

  public TestDriver(CommandHandler commandHandler) {
    this.commandHandler = commandHandler;
  }

  public Integer getOpcode(int address) {
    Integer result = null;
    if (finished) {
      System.out.println("Last executed instruction: " + address);
      throw new RuntimeException("Test complete");
    } else {
      if (!instructions.isEmpty()) {
        address= (int) commandHandler.executeCommand(new GetRegisterValue("PC"));
        SintheticInstruction instruction = instructions.element();
        executingInstruction= true;
        int writeIndex = address;
        for (byte b : instruction.bytes) {
          addOpcodeAt(writeIndex++, b);
        }

        System.out.println("Providing opcode at " + String.format("%04X", address) + ": " + String.format("%02X", instruction.bytes[0]) + (instruction.bytes.length > 1 ? String.format(" %02X", instruction.bytes[1]) : "") + (instruction.bytes.length > 2 ? String.format(" %02X", instruction.bytes[2]) : ""));
        result = ((int) instruction.bytes[0]) & 0xFF;
      } else
        result = -1;
    }
    return result;
  }

  public void addOpcodeAt(int address, byte value) {
    writeMemory(address, value, false);
  }

  public void writeMemory(int address, int value, boolean contended) {
    commandHandler.addNoResultCommand(new WriteMemoryCommand(address, value, contended));
  }

  public void updatePC(int value) {
    setRegister("PC", value);
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public void addInstruction(byte... bytes) {
//    System.out.println("tstates before add instruction: " + tstates);
//    instructions.offer(new SintheticInstruction(bytes));

    int writeIndex = getRegister("PC");
    for (byte b : bytes) {
      writeMemory(writeIndex++, b, false);
    }
  }

  public void waitExecution() {
    int i = (int) commandHandler.executeCommand(new ContinueExecutionCommand());
//
//    while (!instructions.isEmpty()) {
//      try {
//        Thread.sleep(2);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//    }
//    System.out.println("tstates after wait: " + tstates);
  }

  public void clearInstructions() {
    instructions.clear();
  }

  public void setSkipFetch(boolean skipFetch) {
    this.skipFetch = skipFetch;
  }

  public boolean isSkipFetch() {
    return skipFetch;
  }

  public void reset() {
    commandHandler.reset();
    clearInstructions();
    setFinished(false);
    skipFetch = true;
  }

  public void execDone() {
    executingInstruction= false;
    instructions.poll();
  }

  public boolean isExecuting() {
    return executingInstruction;
  }

  public byte readMemory(int address, boolean contended) {
    return (byte) commandHandler.executeCommand(new ReadMemoryCommand(address, contended));
  }

  public int getRegister(String name) {
    return (int) commandHandler.executeCommand(new GetRegisterValue(name));
  }

  public void setRegister(String name, int value) {
    commandHandler.addNoResultCommand(new SetRegisterValue(name, value));
  }

  public void setConnectedLEC(boolean b) {

  }

  public boolean isConnectedLEC() {
    return false;
  }

  public void setLECMemorySize(int i) {

  }

  public int getTstates() {
    int i = (int) commandHandler.executeCommand(new GetRegisterValue("tstates"));
    return i;
  }

  public void setTstates(int tStates) {
    commandHandler.addNoResultCommand(new SetRegisterValue("tstates", tStates));
  }

  public void resetZ80() {

  }

  public void selectHardwareModel(MachineTypes machineTypes) {

  }

  public void writePort(int port, int value) {
    commandHandler.addNoResultCommand(new WritePortCommand(port, value, false));
  }

  public void setModel(String model) {
    commandHandler.addNoResultCommand(new SetMachineModel(model));
  }

  public void if1Page(boolean in) {
    commandHandler.addNoResultCommand(new If1Page(in));
  }

  public byte readLanPort() {
    return (byte) commandHandler.executeCommand(new ReadLanPortCommand());
  }

  public int getBeamX() {
    return (int) commandHandler.executeCommand(new GetBeamX());
  }

  public int getBeamY() {
    return (int) commandHandler.executeCommand(new GetBeamY());
  }
}
