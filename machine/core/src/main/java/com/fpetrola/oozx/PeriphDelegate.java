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

import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.peripherals.ZxPeripheral;

public interface PeriphDelegate extends IPeriph {
  IPeriph getPeriph();

  default void register(ZxPeripheral zxPeripheral) {
    getPeriph().register(zxPeripheral);
  }

  default void setPresent(Periph.Type type, Periph.Present present) {
    getPeriph().setPresent(type, present);
  }

  default void setPresent(Class<? extends ZxPeripheral> zxPeripheralClass, Periph.Present present) {
    getPeriph().setPresent(zxPeripheralClass, present);
  }

  default boolean activateType(Class<? extends ZxPeripheral> type, boolean active) {
    return getPeriph().activateType(type, active);
  }

  default boolean isActive(Periph.Type type) {
    return getPeriph().isActive(type);
  }

  default void clear() {
    getPeriph().clear();
  }

  default void end() {
    getPeriph().end();
  }

  default byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
    return getPeriph().mergeFloatingBus(value, attached, floatingBus);
  }

  default void writePortInternal(int port, byte b) {
    getPeriph().writePortInternal(port, b);
  }

  default boolean update() {
    return getPeriph().update();
  }

  default void postHook() {
    getPeriph().postHook();
  }

  default boolean postCheck() {
    return getPeriph().postCheck();
  }
}
