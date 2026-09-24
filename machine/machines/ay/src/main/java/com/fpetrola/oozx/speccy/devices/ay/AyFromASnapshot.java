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

package com.fpetrola.oozx.speccy.devices.ay;

import com.fpetrola.emulation.helpers.snapshots.AY8912State;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.speccy.modules.snapshot.RestoredFromASnapshot;
import com.google.inject.Inject;

/**
 * El estado del AY que trae un snapshot, puesto de vuelta en el chip.
 * <p>
 * Un snapshot se toma a mitad de una melodia y trae el estado del chip; sin esto lo que se
 * restauraba era una maquina tocando lo ultimo que se le hubiera escrito al chip, hasta que el
 * juego reescribiera cada registro - que para una nota sostenida no es pronto, y para una
 * grabacion es nunca, porque la grabacion repite las escrituras que vinieron despues.
 * <p>
 * Por los puertos y no adentro de la sintesis, para que lo que la maquina puede leer de vuelta y
 * lo que suena no puedan contradecirse.
 */
public class AyFromASnapshot implements RestoredFromASnapshot {

  private final com.fpetrola.oozx.speccy.modules.machine.Machine machine;
  private final com.fpetrola.z80.cpu.IO io;

  @Inject
  public AyFromASnapshot(com.fpetrola.oozx.speccy.modules.machine.Machine machine,
                         com.fpetrola.z80.cpu.IO io) {
    this.machine = machine;
    this.io = io;
  }

  @Override
  public void restore(SpectrumState snapshot) {
    if (machine.current == null || !machine.current.hasOnBoard(AyPeripheral.class)) {
      return;
    }
    AY8912State chip = snapshot.getAY8912State();
    if (chip == null || chip.getRegAY() == null) {
      return;
    }
    int[] registers = chip.getRegAY();
    for (int register = 0; register < 16 && register < registers.length; register++) {
      io.out(0xfffd, register);
      io.out(0xbffd, registers[register] & 0xff);
    }
    io.out(0xfffd, chip.getAddressLatch() & 0x0f);
  }
}
