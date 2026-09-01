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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;

public interface PeripheralBusDelegate extends PeripheralBus {
  PeripheralBus getPeripherals();

  default void register(Peripheral peripheral) {
    getPeripherals().register(peripheral);
  }

  default boolean activateType(Class<? extends Peripheral> type, boolean active) {
    return getPeripherals().activateType(type, active);
  }

  default Peripheral find(Class<? extends Peripheral> peripheralClass) {
    return getPeripherals().find(peripheralClass);
  }

  default boolean isActive(Class<? extends Peripheral> peripheralClass) {
    return getPeripherals().isActive(peripheralClass);
  }

  default void clear() {
    getPeripherals().clear();
  }

  default void end() {
    getPeripherals().end();
  }

  default byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
    return getPeripherals().mergeFloatingBus(value, attached, floatingBus);
  }

  default void writePortInternal(int port, byte b) {
    getPeripherals().writePortInternal(port, b);
  }

  default boolean update() {
    return getPeripherals().update();
  }

  default void postHook() {
    getPeripherals().postHook();
  }

  default boolean postCheck() {
    return getPeripherals().postCheck();
  }
}
