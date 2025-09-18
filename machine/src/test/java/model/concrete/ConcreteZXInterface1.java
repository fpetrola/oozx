package model.concrete;

import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZXInterface1;

import java.util.ArrayList;
import java.util.List;

public class ConcreteZXInterface1 implements IZXInterface1 {
  private ISpectrumBus bus;
  private List<IMicrodrive> microdrives = new ArrayList<>();
  private boolean[] flipFlops = new boolean[8];
  private boolean commsData;
  private boolean commsClock;
  private boolean previousClock;
  private boolean eraseEnabled;
  private boolean writeMode;
  private boolean ctsSet;
  private boolean waitSet;
  private boolean busy;
  private boolean dtrActive = true;
  public boolean gapDetected;
  public boolean syncDetected;
  private boolean romPagedIn;
  private byte rxData;
  private byte txData;
  private byte[] rom = new byte[8192];
  private boolean writeProtected;

  public ConcreteZXInterface1() {
    for (int i = 0; i < rom.length; i++) {
      rom[i] = (byte) ((i + 0xAA) & 0xFF);
    }
  }

  @Override
  public void connectToBus(ISpectrumBus bus) {
    this.bus = bus;
  }

  @Override
  public void disconnectFromBus() {
    this.bus = null;
  }

  @Override
  public boolean handlesPortRead(int port) {
    return port == 0xEF || port == 0xF7 || port == 0xFF;
  }

  @Override
  public byte handlePortRead(int port) {
    if (port == 0xEF) {
      byte status = 0;
      if (busy) status |= 0x80;
      if (dtrActive) status |= 0x40;
      if (gapDetected) status |= 0x02;
      if (syncDetected) status |= 0x01;
      if (isWriteProtected()) status |= 0x04;
      return status;
    } else if (port == 0xF7 || port == 0xFF) {
      return rxData;
    }
    return (byte) 0xFF;
  }

  @Override
  public boolean handlesPortWrite(int port) {
    return port == 0xEF || port == 0xE7 || port == 0xF7;
  }

  @Override
  public void handlePortWrite(int port, byte value) {
    if (port == 0xEF) {
      commsData = (value & 0x01) != 0;
      boolean newClock = (value & 0x02) != 0;
      eraseEnabled = (value & 0x04) != 0;
      writeMode = (value & 0x08) != 0;
      ctsSet = (value & 0x10) != 0;
      waitSet = (value & 0x20) != 0;

      if (newClock && !previousClock) {
        for (int i = 7; i > 0; i--) {
          flipFlops[i] = flipFlops[i - 1];
        }
        flipFlops[0] = commsData;
        busy = false;
        for (boolean f : flipFlops) {
          if (f) busy = true;
        }
        for (int i = 0; i < microdrives.size(); i++) {
          ConcreteMicrodrive drive = (ConcreteMicrodrive) microdrives.get(i);
          drive.setSelected(flipFlops[i]);
          if (drive.isSelected()) {
            drive.setMotorRunning(true);
            drive.setLEDOn(true);
            drive.setEraseCurrentOn(eraseEnabled);
            drive.setWriteMode(writeMode);
            writeProtected = drive.getWriteProtect();
          } else {
            drive.setMotorRunning(false);
            drive.setLEDOn(false);
            drive.setEraseCurrentOn(false);
          }
        }
      }
      previousClock = newClock;
    } else if (port == 0xE7) {
      int selected = getSelectedMicrodrive();
      if (selected != -1) {
        microdrives.get(selected).writeData(value);
      }
    } else if (port == 0xF7) {
      txData = value;
      rxData = value;
    }
  }

  @Override
  public boolean isROMPagedIn() {
    return romPagedIn;
  }

  @Override
  public boolean isBusy() {
    return busy;
  }

  @Override
  public boolean isDTRActive() {
    return dtrActive;
  }

  @Override
  public boolean isGapDetected() {
    return gapDetected;
  }

  @Override
  public boolean isSyncDetected() {
    return syncDetected;
  }

  @Override
  public boolean isWriteProtected() {
    return writeProtected;
  }

  @Override
  public boolean isEraseEnabled() {
    return eraseEnabled;
  }

  @Override
  public boolean isWriteMode() {
    return writeMode;
  }

  @Override
  public boolean isCommsClockHigh() {
    return commsClock;
  }

  @Override
  public boolean isCommsDataHigh() {
    return commsData;
  }

  @Override
  public boolean isCTSSet() {
    return ctsSet;
  }

  @Override
  public boolean isWaitSet() {
    return waitSet;
  }

  @Override
  public boolean isNetworkMode() {
    if (busy) return false;
    return !commsData;
  }

  @Override
  public int getSelectedMicrodrive() {
    for (int i = 0; i < 8; i++) {
      if (flipFlops[i]) return i;
    }
    return -1;
  }

  @Override
  public byte getRxData() {
    return rxData;
  }

  @Override
  public void setTxData(byte data) {
    txData = data;
    rxData = data;
  }

  @Override
  public void connectMicrodrive(IMicrodrive microdrive) {
    if (microdrives.size() < 8) {
      microdrives.add(microdrive);
      microdrive.connectToBus(bus);
    }
  }

  @Override
  public List<IMicrodrive> getConnectedMicrodrives() {
    return new ArrayList<>(microdrives);
  }

  @Override
  public byte[] getROM() {
    return rom.clone();
  }

  @Override
  public void pageROMIn() {
    romPagedIn = true;
    bus.pageInROM(rom);
  }

  @Override
  public void pageROMOut() {
    romPagedIn = false;
    bus.pageInROM(new ConcreteMemory().getROM());
  }

  public boolean isDtrActive() {
    return dtrActive;
  }

  @Override
  public void setDtrActive(boolean dtrActive) {
    this.dtrActive = dtrActive;
  }
}
