package model.concrete;

import model.interfaces.*;

import java.util.ArrayList;
import java.util.List;

public class ConcreteSpectrumBus implements ISpectrumBus {
  private List<IPeripheral> peripherals = new ArrayList<>();
  private IMemory memory = new ConcreteMemory();
  private IULA ula = new ConcreteULA();
  private int tStates;
  private String model = "48K";

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public void connectComponent(IComponent component) {
    component.connectToBus(this);
    if (component instanceof IPeripheral) {
      peripherals.add((IPeripheral) component);
    }
  }

  @Override
  public byte readPort(int port) {
    int delay = ula.getIOContentionDelay(port, tStates % (model.equals("128K") ? 70908 : 69888), model);
    tStates += delay;
    for (IPeripheral p : peripherals) {
      if (p.handlesPortRead(port)) {
        return p.handlePortRead(port);
      }
    }
    return (byte) 0xFF;
  }

  @Override
  public void writePort(int port, byte value) {
    int delay = ula.getIOContentionDelay(port, tStates % (model.equals("128K") ? 70908 : 69888), model);
    tStates += delay;
    for (IPeripheral p : peripherals) {
      if (p.handlesPortWrite(port)) {
        p.handlePortWrite(port, value);
      }
    }
  }

  @Override
  public byte readMemory(int address) {
    return memory.read(address);
  }

  @Override
  public void writeMemory(int address, byte value) {
    memory.write(address, value);
  }

  @Override
  public void pageInROM(byte[] romData) {
    memory.pageInROM(romData);
  }

  @Override
  public void handleError(String errorMessage) {
    for (IPeripheral p : peripherals) {
      if (p instanceof IZXInterface1) {
        ((IZXInterface1) p).pageROMIn();
      }
    }
  }

  @Override
  public IULA getULA() {
    return ula;
  }

  @Override
  public IMemory getMemory() {
    return memory;
  }
}
