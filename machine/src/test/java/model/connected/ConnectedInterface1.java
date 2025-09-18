package model.connected;

import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZXInterface1;
import model.tests.TestDriver;

import java.util.ArrayList;
import java.util.List;

public class ConnectedInterface1 implements IZXInterface1 {
  private final TestDriver testDriver;
  private boolean dtrActive;

  public ConnectedInterface1(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public void reset() {
//    testDriver.spectrum.getInterface1().reset();
  }

  @Override
  public boolean isROMPagedIn() {
//    return testDriver.spectrum.getMemory().isIF1RomPaged();
    return false;
  }

  @Override
  public boolean isBusy() {
    return true;
  }

  @Override
  public boolean isDTRActive() {
    return false;
  }

  @Override
  public boolean isGapDetected() {
    return false;
  }

  @Override
  public boolean isSyncDetected() {
    return false;
  }

  @Override
  public boolean isWriteProtected() {
    return false;
  }

  @Override
  public boolean isEraseEnabled() {
    return false;
  }

  @Override
  public boolean isWriteMode() {
    return false;
  }

  @Override
  public boolean isCommsClockHigh() {
    return false;
  }

  @Override
  public boolean isCommsDataHigh() {
    return false;
  }

  @Override
  public boolean isCTSSet() {
    return false;
  }

  @Override
  public boolean isWaitSet() {
    return false;
  }

  @Override
  public boolean isNetworkMode() {
    return false;
  }

  @Override
  public int getSelectedMicrodrive() {
//    if (getInterface1().mdrFlipFlop == 0)
//      return -1;
//    else
//      return getInterface1().getDriveRunning();
    return 0;
  }

//  private Interface1 getInterface1() {
//    return testDriver.spectrum.getInterface1();
//  }

  @Override
  public byte getRxData() {
//    return (byte) testDriver.spectrum.getInterface1().readLanPort();
    return 0;
  }

  @Override
  public void setTxData(byte data) {

  }

  @Override
  public void connectMicrodrive(IMicrodrive microdrive) {
    microdrive.connect();
  }

  @Override
  public List<IMicrodrive> getConnectedMicrodrives() {
    List<? extends IMicrodrive> list = new ArrayList<>();
//       list= Arrays.stream(getInterface1().microdrive).filter(m -> m.isCartridge()).map(m -> new ConnectedMicrodrive(testDriver)).toList();
    return (List<IMicrodrive>) list;
  }

  @Override
  public byte[] getROM() {
    return new byte[0];
  }

  @Override
  public void pageROMIn() {
//    testDriver.spectrum.getMemory().pageIF1Rom();
  }

  @Override
  public void pageROMOut() {
//    testDriver.spectrum.getMemory().unpageIF1Rom();
  }

  @Override
  public void setDtrActive(boolean dtrActive) {
    this.dtrActive = dtrActive;
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

  @Override
  public void connectToBus(ISpectrumBus bus) {

  }

  @Override
  public void disconnectFromBus() {

  }
}
