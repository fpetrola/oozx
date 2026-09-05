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

package model.connected;

import model.interfaces.IMicrodrive;
import model.interfaces.IMicrodriveCartridge;
import model.interfaces.ISpectrumBus;
import model.harness.TestDriver;

public class ConnectedMicrodrive implements IMicrodrive {
  private final TestDriver testDriver;
  private int index;
  private boolean writeProtect;

  public ConnectedMicrodrive(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public boolean isSelected() {
//    return testDriver.spectrum.getInterface1().isDriveRunning(index);
    return false;
  }

  @Override
  public boolean isMotorRunning() {
    return true;
  }

  @Override
  public boolean isLEDOn() {
    return true;
  }

  @Override
  public boolean isEraseCurrentOn() {
    return false;
  }

  @Override
  public boolean isWriteMode() {
    return writeProtect;
  }

  @Override
  public boolean getWriteProtect() {
    return false;
  }

  @Override
  public void setCartridge(IMicrodriveCartridge cartridge) {

  }

  @Override
  public IMicrodriveCartridge getCartridge() {
    return null;
  }

  @Override
  public byte readData() {
//    return (byte) testDriver.spectrum.getInterface1().readDataPort();
    return 123;
  }

  @Override
  public void writeData(byte data) {

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

  @Override
  public void connect() {
//    Interface1 interface1 = testDriver.spectrum.getInterface1();
//    Microdrive[] microdrive = interface1.microdrive;
//    for (int i = 0; i < microdrive.length; i++) {
//      if (!microdrive[i].isCartridge()) {
//        index = i;
//        interface1.insertNew(index);
//        break;
//      }
//    }
  }

  @Override
  public void setWriteProtect(boolean writeProtect) {
//    testDriver.spectrum.getInterface1().setWriteProtected(0, true);
  }
}
