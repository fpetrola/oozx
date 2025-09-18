package model.interfaces;

public interface IULA {
  void setScreenActive(boolean active);

  byte readKeyboard(int port);

  void setBorder(int color);

  void generateInterrupt();

  void renderScreen();

  void beep(int duration);

  boolean isScreenActive();

  int getContentionDelay(int address, int tStates, String model);

  int getIOContentionDelay(int port, int tStates, String model);
}
