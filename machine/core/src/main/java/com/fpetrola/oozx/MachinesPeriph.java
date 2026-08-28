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

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.fuse.peripherals.*;

@Singleton
public class MachinesPeriph {

  private PeriphDelegate periph;

@Inject
  public MachinesPeriph(PeriphDelegate periph) {
    this.periph = periph;
  }

  // Base peripherals available on all machines
  private void basePeripherals() {
    periph.setPresent(Periph.Type.DIVIDE, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.DIVMMC, Periph.Present.OPTIONAL);
    periph.setPresent(KempstonStrictPeripheral.class, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.KEMPSTON_MOUSE, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.SIMPLEIDE, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.SPECCYBOOT, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.SPECTRANET, Periph.Present.OPTIONAL);
    periph.setPresent(UlaPeripheral.class, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.ZXATASP, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.ZXCF, Periph.Present.OPTIONAL);
  }

  // Base peripherals for 48K and 128K machines
  private void basePeripherals48128() {
    basePeripherals();
    periph.setPresent(Periph.Type.BETA128, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.INTERFACE1, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.INTERFACE2, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.MULTIFACE_128, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.OPUS, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.PLUSD, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.SPECDRUM, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.USOURCE, Periph.Present.OPTIONAL);
  }

  // Peripherals for 48K and similar machines
  public void machinesPeriph48() {
    basePeripherals48128();
    periph.setPresent(Periph.Type.FULLER, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.MELODIK, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.MULTIFACE_1, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.TTX2000S, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.ZXPRINTER, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.DIDAKTIK80, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.DISCIPLE, Periph.Present.OPTIONAL);
  }

  // Peripherals for 128K and similar machines
  public void machinesPeriph128() {
    basePeripherals48128();
    periph.setPresent(Periph.Type.AY, Periph.Present.ALWAYS);
    periph.setPresent(Spec128MemoryPeripheral.class, Periph.Present.ALWAYS);
  }

  // Peripherals for +3 and similar machines
  public void machinesPeriphPlus3() {
    basePeripherals();
    periph.setPresent(Periph.Type.AY_PLUS3, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.MULTIFACE_3, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.PARALLEL_PRINTER, Periph.Present.OPTIONAL);
    periph.setPresent(SpecPlus3MemoryPeripheral.class, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.ZXMMC, Periph.Present.OPTIONAL);
  }

  // Peripherals for TC2068 and TS2068
  public void machinesPeriphTimex() {
    basePeripherals();
    periph.setPresent(UlaPeripheral.class, Periph.Present.NEVER);
    periph.setPresent(UlaFullDecodePeripheral.class, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.SCLD, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.AY_TIMEX_WITH_JOYSTICK, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.INTERFACE2, Periph.Present.OPTIONAL);
    periph.setPresent(Periph.Type.ZXPRINTER_FULL_DECODE, Periph.Present.OPTIONAL);
  }

  // Peripherals for Pentagon and Scorpion
  public void machinesPeriphPentagon() {
    basePeripherals();
    periph.setPresent(Spec128MemoryPeripheral.class, Periph.Present.ALWAYS);
    periph.setPresent(Periph.Type.AY, Periph.Present.ALWAYS);
    periph.setPresent(UlaPeripheral.class, Periph.Present.NEVER);
    periph.setPresent(UlaFullDecodePeripheral.class, Periph.Present.ALWAYS);
    periph.setPresent(KempstonStrictPeripheral.class, Periph.Present.NEVER);
  }
}

