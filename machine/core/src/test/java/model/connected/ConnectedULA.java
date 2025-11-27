/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package model.connected;

import model.interfaces.IULA;
import model.tests.TestDriver;

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
