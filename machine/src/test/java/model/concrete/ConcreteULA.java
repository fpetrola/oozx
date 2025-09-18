package model.concrete;

import model.interfaces.IULA;

class ConcreteULA implements IULA {
  private boolean screenActive;
  private int tStates;

  @Override
  public void setScreenActive(boolean active) {
    this.screenActive = active;
  }

  @Override
  public byte readKeyboard(int port) {
    return (byte) 0xFF;
  }

  @Override
  public void setBorder(int color) {
  }

  @Override
  public void generateInterrupt() {
    tStates = 0;
  }

  @Override
  public void renderScreen() {
    screenActive = true;
  }

  @Override
  public void beep(int duration) {
  }

  @Override
  public boolean isScreenActive() {
    return screenActive;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    return 0; // Handled by IMemory
  }

  @Override
  public int getIOContentionDelay(int port, int tStates, String model) {
    if ((port & 0x0001) == 0 || model.equals("48K")) { // ULA ports or Interface 1
      int frameLength = model.equals("128K") ? 70908 : 69888;
      int startTState = model.equals("NTSC") ? 8959 : model.equals("128K") ? 14361 : 14335;
      if (tStates >= startTState + 192 * (model.equals("128K") ? 228 : 224)) return 0;
      int lineTState = (tStates - startTState) % (model.equals("128K") ? 228 : 224);
      if (model.equals("+3")) {
        if (lineTState == 0) return 1;
        if (lineTState >= 2 && lineTState <= 8) return 8 - lineTState;
        return 0;
      } else {
        if (lineTState >= 0 && lineTState <= 5) return 6 - lineTState;
        return 0;
      }
    }
    return 0;
  }
}
