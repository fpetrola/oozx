package model.concrete;

import model.interfaces.IMicrodriveCartridge;

import java.util.ArrayList;
import java.util.List;

public class ConcreteMicrodriveCartridge implements IMicrodriveCartridge {
  private boolean writeProtected;

  @Override
  public boolean isWriteProtected() {
    return writeProtected;
  }

  @Override
  public int getCapacity() {
    return 100000;
  }

  @Override
  public byte[] read(String filename) {
    return new byte[0];
  }

  @Override
  public void write(String filename, byte[] data) {
  }

  @Override
  public void erase(String filename) {
  }

  @Override
  public List<String> listFiles() {
    return new ArrayList<>();
  }

  @Override
  public void format() {
  }
}
