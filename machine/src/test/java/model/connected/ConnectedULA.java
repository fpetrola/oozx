package model.connected;

import model.interfaces.IULA;

public class ConnectedULA implements IULA {
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
}
