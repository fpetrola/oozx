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

import model.interfaces.IULA;
import model.harness.TestDriver;

public class ConnectedULA implements IULA {
  private final TestDriver testDriver;

  public ConnectedULA(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public void setScreenActive(boolean active) {

  }

  @Override
  public byte readKeyboard(int port) {
    return 0;
  }

  @Override
  public void setBorder(int color) {

  }

  @Override
  public void generateInterrupt() {

  }

  @Override
  public void renderScreen() {

  }

  @Override
  public void beep(int duration) {

  }

  @Override
  public boolean isScreenActive() {
    return false;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    return 0;
  }

  @Override
  public int getIOContentionDelay(int port, int tStates, String model) {
    return 0;
  }

  @Override
  public int getVerticalPosition() {
    return testDriver.getBeamY();
  }

  @Override
  public int getHorizontalPosition() {
    return testDriver.getBeamX();
  }

  @Override
  public int getBorderColor() {
    return 0;
  }

  @Override
  public int getBeeperState() {
    return 0;
  }

  @Override
  public void setKeyboardRow(byte b, byte b1) {

  }

  @Override
  public boolean isInterruptActive() {
    return false;
  }
}
