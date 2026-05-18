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
package com.fpetrola.oozx.speccy.devices.multiface;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The three Multifaces, and what tells them apart: which machine each was sold for, which port
 * pages it in, and which way round A7 goes - the 3 has it backwards from the other two.
 */
public enum MultifaceModel {
  ONE("Multiface One", 0x0012, MultifaceOnePeripheral.class),
  M128("Multiface 128", 0x0032, Multiface128Peripheral.class),
  M3("Multiface 3", 0x0032, Multiface3Peripheral.class);

  public final String title;
  final int portValue;
  final Class<? extends MultifacePeripheral> peripheral;

  MultifaceModel(String title, int portValue, Class<? extends MultifacePeripheral> peripheral) {
    this.title = title;
    this.portValue = portValue;
    this.peripheral = peripheral;
  }

  /** The One was for a 48K; the 128 for a 128 and what came before it; the 3 for the Amstrad ones. */
  public boolean fitsOn(SpectrumMachine machine) {
    return switch (this) {
      case ONE -> !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
      case M128 -> !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
      case M3 -> machine.pagesThrough1ffd();
    };
  }
}
