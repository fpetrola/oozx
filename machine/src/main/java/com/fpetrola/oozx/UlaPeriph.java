/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import com.fpetrola.oozx.fuse.modules.Ula;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.peripherals.ZxPeripheral;

public class UlaPeriph implements IPeriph {
  private final Ula ula;
  private ZxClock zxClock;

  @Override
  public void register(ZxPeripheral zxPeripheral) {
    periph.register(zxPeripheral);
  }

  @Override
  public void setPresent(Periph.Type type, Periph.Present present) {
    periph.setPresent(type, present);
  }

  @Override
  public void setPresent(Class<? extends ZxPeripheral> zxPeripheralClass, Periph.Present present) {
    periph.setPresent(zxPeripheralClass, present);
  }

  @Override
  public boolean activateType(Class<? extends ZxPeripheral> type, boolean active) {
    return periph.activateType(type, active);
  }

  @Override
  public boolean isActive(Periph.Type type) {
    return periph.isActive(type);
  }

  @Override
  public void clear() {
    periph.clear();
  }

  @Override
  public void end() {
    periph.end();
  }

  public byte readPort(int port) {
    ula.contendPortEarly(port);
    ula.contendPortLate(port);
    return periph.readPort(port);
  }

  @Override
  public byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
    return periph.mergeFloatingBus(value, attached, floatingBus);
  }

  @Override
  public void writePort(int port, byte b) {
    ula.contendPortEarly(port);
    writePortInternal(port, b);
    ula.contendPortLate(port);
    zxClock.addTstates(1);
  }

  @Override
  public void writePortInternal(int port, byte b) {
    periph.writePortInternal(port, b);
  }

  //    }

//    // Disable optional peripherals
//    public  void disableOptional() {
//        if (Ui.mousePresent && Ui.mouseGrabbed) {
//            Ui.mouseGrabbed = Ui.mouseRelease(true);
//        }
//
//        peripherals.forEach((type, privatePeriph) -> {
//            if (privatePeriph.present == Present.NEVER || privatePeriph.present == Present.OPTIONAL) {
//                if (privatePeriph.peripheral.hasOption()) {
//                    privatePeriph.peripheral.getOption()[0] = false;
//                }
//            }
//        });
//
//        updatePeripheralsStatus();
//    }

  // Update peripherals and determine if a hard reset is needed
  @Override
  public boolean update() {
    return periph.update();
  }

  @Override
  public void postHook() {
    periph.postHook();
  }

  @Override
  public boolean postCheck() {
    return periph.postCheck();
  }

  private final IPeriph periph;

  public UlaPeriph(Ula ula, ZxClock zxClock, IPeriph periph) {
    this.ula = ula;
    this.zxClock = zxClock;
    this.periph = periph;
  }
}
