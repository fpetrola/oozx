package model.interfaces;

public interface IMicrodrive extends IPeripheral {
  boolean isSelected();

  boolean isMotorRunning();

  boolean isLEDOn();

  boolean isEraseCurrentOn();

  boolean isWriteMode();

  boolean getWriteProtect();

  void setWriteProtect(boolean writeProtect);

  void setCartridge(IMicrodriveCartridge cartridge);

  IMicrodriveCartridge getCartridge();

  byte readData();

  void writeData(byte data);

  default void connect() {

  }
}
