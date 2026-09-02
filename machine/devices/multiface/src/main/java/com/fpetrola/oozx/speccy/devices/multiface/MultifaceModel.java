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
package com.fpetrola.oozx.speccy.devices.multiface;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The three Multifaces, and what tells them apart: which machine each was sold for, which port
 * pages it in, and which way round A7 goes - the 3 has it backwards from the other two.
 */
public enum MultifaceModel {
  ONE("Multiface One", 0x0012, MultifaceOnePeripheral.class,
      settings -> settings.romMultiface1, (settings, rom) -> settings.romMultiface1 = rom),
  M128("Multiface 128", 0x0032, Multiface128Peripheral.class,
      settings -> settings.romMultiface128, (settings, rom) -> settings.romMultiface128 = rom),
  M3("Multiface 3", 0x0032, Multiface3Peripheral.class,
      settings -> settings.romMultiface3, (settings, rom) -> settings.romMultiface3 = rom);

  public final String title;
  final int portValue;
  final Class<? extends MultifacePeripheral> peripheral;
  private final Function<Settings.SettingsInfo, String> rom;
  private final java.util.function.BiConsumer<Settings.SettingsInfo, String> chooseRom;

  MultifaceModel(String title, int portValue, Class<? extends MultifacePeripheral> peripheral,
                 Function<Settings.SettingsInfo, String> rom,
                 java.util.function.BiConsumer<Settings.SettingsInfo, String> chooseRom) {
    this.title = title;
    this.portValue = portValue;
    this.peripheral = peripheral;
    this.rom = rom;
    this.chooseRom = chooseRom;
  }

  public String rom(Settings settings) {
    return rom.apply(settings.current);
  }

  public String defaultRom(Settings settings) {
    return rom.apply(settings.defaults);
  }

  public void chooseRom(Settings settings, String path) {
    chooseRom.accept(settings.current, path);
  }

  /** The One was for a 48K; the 128 for a 128 and what came before it; the 3 for the Amstrad ones. */
  public boolean fitsOn(SpectrumMachine machine) {
    return switch (this) {
      case ONE -> !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
      case M128 -> !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
      case M3 -> machine.has(MachineCapability.PLUS3_MEMORY);
    };
  }
}
