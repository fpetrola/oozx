package model.concrete;

import model.interfaces.IMemory;

class ConcreteMemory implements IMemory {
  private byte[] rom = new byte[16384];
  private byte[] ram = new byte[131072]; // 128K total for 8 pages
  private int[] pages = new int[4]; // Banks 0-3 (0x0000-0xFFFF)

  public ConcreteMemory() {
    for (int i = 0; i < rom.length; i++) {
      rom[i] = (byte) (i & 0xFF);
    }
    pages[0] = -1; // ROM
    pages[1] = 5; // Default 128K paging
    pages[2] = 2;
    pages[3] = 0;
  }

  @Override
  public byte read(int address) {
    int bank = address >> 14;
    if (bank == 0 && pages[0] == -1) return rom[address];
    int page = pages[bank];
    return ram[(page << 14) + (address & 0x3FFF)];
  }

  @Override
  public void write(int address, byte value) {
    int bank = address >> 14;
    if (bank == 0 && pages[0] == -1) return; // ROM write protected
    int page = pages[bank];
    ram[(page << 14) + (address & 0x3FFF)] = value;
  }

  @Override
  public void pageInROM(byte[] romData) {
    System.arraycopy(romData, 0, rom, 0, Math.min(romData.length, rom.length));
  }

  @Override
  public byte[] getROM() {
    return rom.clone();
  }

  @Override
  public byte[] getRAM() {
    return ram.clone();
  }

  @Override
  public boolean isContended(int address, int page) {
    if (address < 0x4000) return false;
    if (address >= 0x4000 && address <= 0x7FFF) return true; // 48K always contended
    if (page == 1 || page == 3 || page == 5 || page == 7) return true; // 128K/+2
    if (page == 4 || page == 5 || page == 6 || page == 7) return true; // +2A/+3
    return false;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    int frameLength = model.equals("128K") ? 70908 : 69888;
    int startTState = model.equals("NTSC") ? 8959 : model.equals("128K") ? 14361 : 14335;
    if (!isContended(address, pages[address >> 14]) || tStates >= startTState + 192 * (model.equals("128K") ? 228 : 224))
      return 0;

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

  @Override
  public void setPage(int slot, int bank) {
    pages[slot] = bank;
  }

  @Override
  public int getPage(int bank) {
    return pages[bank];
  }
}
