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

public interface ISpectrumBus {
  void connectComponent(IComponent component);

  byte readPort(int port);

  void writePort(int port, byte value);

  int readMemory(int address);

  void writeMemory(int address, byte value);

  void pageInROM(byte[] romData);

  void handleError(String errorMessage);

  IULA getULA();

  IMemory getMemory();

  int mergeFloatingBus(int i, int i1, int i2);
}
