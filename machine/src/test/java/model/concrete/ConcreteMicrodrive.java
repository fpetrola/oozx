package model.concrete;

import model.interfaces.IMicrodrive;
import model.interfaces.IMicrodriveCartridge;
import model.interfaces.ISpectrumBus;

public class ConcreteMicrodrive implements IMicrodrive {
  private boolean selected;
  private boolean motorRunning;
  private boolean ledOn;
  private boolean eraseCurrentOn;
  private boolean writeMode;
  private boolean writeProtect;
  private IMicrodriveCartridge cartridge;
  private byte data;

  @Override
  public void connectToBus(ISpectrumBus bus) {
  }

  @Override
  public void disconnectFromBus() {
  }

  @Override
  public boolean handlesPortRead(int port) {
    return false;
  }

  @Override
  public byte handlePortRead(int port) {
    return 0;
  }

  @Override
  public boolean handlesPortWrite(int port) {
    return false;
  }

  @Override
  public void handlePortWrite(int port, byte value) {
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override
  public boolean isSelected() {
    return selected;
  }

  public void setMotorRunning(boolean motorRunning) {
    this.motorRunning = motorRunning;
  }

  @Override
  public boolean isMotorRunning() {
    return motorRunning;
  }

  public void setLEDOn(boolean ledOn) {
    this.ledOn = ledOn;
  }

  @Override
  public boolean isLEDOn() {
    return ledOn;
  }

  public void setEraseCurrentOn(boolean eraseCurrentOn) {
    this.eraseCurrentOn = eraseCurrentOn;
  }

  @Override
  public boolean isEraseCurrentOn() {
    return eraseCurrentOn;
  }

  public void setWriteMode(boolean writeMode) {
    this.writeMode = writeMode;
  }

  @Override
  public boolean isWriteMode() {
    return writeMode;
  }

  @Override
  public boolean getWriteProtect() {
    return writeProtect;
  }

  @Override
  public void setWriteProtect(boolean writeProtect) {
    this.writeProtect = writeProtect;
  }

  @Override
  public void setCartridge(IMicrodriveCartridge cartridge) {
    this.cartridge = cartridge;
  }

  @Override
  public IMicrodriveCartridge getCartridge() {
    return cartridge;
  }

  @Override
  public byte readData() {
    return data;
  }

  @Override
  public void writeData(byte data) {
    this.data = data;
  }
}
