package model.interfaces;

public interface ISpectrumBus {
  void setModel(String model);

  void connectComponent(IComponent component);

  byte readPort(int port);

  void writePort(int port, byte value);

  byte readMemory(int address);

  void writeMemory(int address, byte value);

  void pageInROM(byte[] romData);

  void handleError(String errorMessage);

  IULA getULA();

  IMemory getMemory();
}
