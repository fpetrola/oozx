package model.interfaces;

import java.util.List;

public interface IZXInterface1 extends IPeripheral {
  boolean isROMPagedIn();

  boolean isBusy();

  boolean isDTRActive();

  boolean isGapDetected();

  boolean isSyncDetected();

  boolean isWriteProtected();

  boolean isEraseEnabled();

  boolean isWriteMode();

  boolean isCommsClockHigh();

  boolean isCommsDataHigh();

  boolean isCTSSet();

  boolean isWaitSet();

  boolean isNetworkMode();

  int getSelectedMicrodrive();

  byte getRxData();

  void setTxData(byte data);

  void connectMicrodrive(IMicrodrive microdrive);

  List<IMicrodrive> getConnectedMicrodrives();

  byte[] getROM();

  void pageROMIn(boolean in);

  void pageROMOut();

  void setDtrActive(boolean dtrActive);
}
