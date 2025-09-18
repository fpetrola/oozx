package model.interfaces;

import java.util.List;

public interface IMicrodriveCartridge {
  boolean isWriteProtected();

  int getCapacity();

  byte[] read(String filename);

  void write(String filename, byte[] data);

  void erase(String filename);

  List<String> listFiles();

  void format();
}
