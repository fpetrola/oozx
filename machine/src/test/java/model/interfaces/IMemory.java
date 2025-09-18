package model.interfaces;

public interface IMemory {
  byte read(int address);

  void write(int address, byte value);

  void pageInROM(byte[] romData);

  byte[] getROM();

  byte[] getRAM();

  boolean isContended(int address, int page);

  int getContentionDelay(int address, int tStates, String model);

  void setPage(int slot, int bank); // For 128K/+2/+3 paging

  int getPage(int bank);
}
