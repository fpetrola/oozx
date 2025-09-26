package model.tests;

import com.fpetrola.oozx.fuse.*;

import java.util.List;

public class TestDriver {
  private final CommandHandler commandHandler;

  public TestDriver(CommandHandler commandHandler) {
    this.commandHandler = commandHandler;
  }

  public void writeMemory(int address, int value, boolean contended) {
    commandHandler.addNoResultCommand(new WriteMemoryCommand(address, value, contended));
  }

  public void updatePC(int value) {
    setRegister("PC", value);
  }

  public void addInstruction(byte... bytes) {
    int writeIndex = getRegister("PC");
    for (byte b : bytes) {
      writeMemory(writeIndex++, b, false);
    }
  }

  public void waitExecution() {
    commandHandler.executeCommand(new ContinueExecutionCommand());
  }

  public void reset() {
    commandHandler.reset();
  }

  public byte readMemory(int address, boolean contended) {
    return (byte) (int) commandHandler.executeCommand(new ReadMemoryCommand(address, contended));
  }

  public int getRegister(String name) {
    return (int) commandHandler.executeCommand(new GetRegisterValue(name)) & 0xffff;
  }

  public List<TStateUpdate> getTstatesHistory() {
    return (List<TStateUpdate>) commandHandler.executeCommand(new GetTStatesHistory());
  }

  public void tstatesHistoryInit() {
    commandHandler.addNoResultCommand(new TStatesHistoryInit());
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
    return (int) commandHandler.executeCommand(new GetRegisterValue("tstates"));
  }

  public void setTstates(int tStates) {
    commandHandler.addNoResultCommand(new SetRegisterValue("tstates", tStates));
  }

  public void resetZ80() {

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
    return (byte) (int) commandHandler.executeCommand(new ReadLanPortCommand());
  }

  public int getBeamX() {
    return (int) commandHandler.executeCommand(new GetBeamX());
  }

  public int getBeamY() {
    return (int) commandHandler.executeCommand(new GetBeamY());
  }

  public void loadSnapshot(String fileName) {
    commandHandler.addNoResultCommand(new LoadSnapshot(fileName));
  }

  public void step() {
    commandHandler.executeCommand(new ContinueExecutionCommand());

//    commandHandler.addNoResultCommand(new StepCommand());
  }
}
