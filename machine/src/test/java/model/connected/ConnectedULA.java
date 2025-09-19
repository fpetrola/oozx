package model.connected;

import model.interfaces.IULA;
import model.tests.TestDriver;

public class ConnectedULA implements IULA {
  private final TestDriver testDriver;

  public ConnectedULA(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public void setScreenActive(boolean active) {

  }

  @Override
  public byte readKeyboard(int port) {
    return 0;
  }

  @Override
  public void setBorder(int color) {

  }

  @Override
  public void generateInterrupt() {

  }

  @Override
  public void renderScreen() {

  }

  @Override
  public void beep(int duration) {

  }

  @Override
  public boolean isScreenActive() {
    return false;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    return 0;
  }

  @Override
  public int getIOContentionDelay(int port, int tStates, String model) {
    return 0;
  }

  @Override
  public int getVerticalPosition() {
    return testDriver.getBeamY();
  }

  @Override
  public int getHorizontalPosition() {
    return testDriver.getBeamX();
  }

  @Override
  public int getBorderColor() {
    return 0;
  }

  @Override
  public int getBeeperState() {
    return 0;
  }

  @Override
  public void setKeyboardRow(byte b, byte b1) {

  }

  @Override
  public boolean isInterruptActive() {
    return false;
  }
}
