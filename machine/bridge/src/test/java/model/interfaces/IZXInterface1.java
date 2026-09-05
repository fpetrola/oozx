/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
