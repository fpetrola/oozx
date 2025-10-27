package model.interfaces;

public interface ISpectrumBus {
  void connectComponent(IComponent component);

  byte readPort(int port);

  void writePort(int port, byte value);

  int readMemory(int address);

  void writeMemory(int address, byte value);

  void pageInROM(byte[] romData);

  void handleError(String errorMessage);

  IULA getULA();

  IMemory getMemory();

  int mergeFloatingBus(int i, int i1, int i2);
}
