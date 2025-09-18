package model.interfaces;

public interface IPeripheral extends IComponent {
  boolean handlesPortRead(int port);

  byte handlePortRead(int port);

  boolean handlesPortWrite(int port);

  void handlePortWrite(int port, byte value);
}
