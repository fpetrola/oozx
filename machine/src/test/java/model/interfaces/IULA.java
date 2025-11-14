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

package model.interfaces;

public interface IULA {
  void setScreenActive(boolean active);

  byte readKeyboard(int port);

  void setBorder(int color);

  void generateInterrupt();

  void renderScreen();

  void beep(int duration);

  boolean isScreenActive();

  int getContentionDelay(int address, int tStates, String model);

  int getIOContentionDelay(int port, int tStates, String model);

  int getVerticalPosition();

  int getHorizontalPosition();

  int getBorderColor();

  int getBeeperState();

  void setKeyboardRow(byte b, byte b1);

  boolean isInterruptActive();
}
