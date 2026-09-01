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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.MachineChangeListener;

public interface IPeriph extends MachineChangeListener {
  void register(ZxPeripheral zxPeripheral);

  void setPresent(Periph.Type type, Periph.Present present);

  void setPresent(Class<? extends ZxPeripheral> zxPeripheralClass, Periph.Present present);

  boolean activateType(Class<? extends ZxPeripheral> type, boolean active);

  boolean isActive(Periph.Type type);

  boolean isActive(Class<? extends ZxPeripheral> zxPeripheralClass);

  /** The registered peripheral of a kind, or null if this build has none. */
  ZxPeripheral find(Periph.Type type);

  void clear();

  void end();

  byte readPort(int port);

  byte mergeFloatingBus(byte value, byte attached, byte floatingBus);

  void writePort(int port, byte b);

  void writePortInternal(int port, byte b);

  boolean update();

  void postHook();

  boolean postCheck();
}
