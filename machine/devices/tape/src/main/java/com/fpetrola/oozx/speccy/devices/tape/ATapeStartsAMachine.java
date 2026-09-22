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
 */package com.fpetrola.oozx.speccy.devices.tape;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.StartsAMachineOn;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.tape.TapeAutoLoader;
import com.fpetrola.oozx.speccy.modules.tape.TapeHardware;

import java.io.File;

/**
 * A machine given a tape: the one the tape was made for, with the tape in it, typing LOAD "".
 * <p>
 * The emulator used to do this itself, which meant it had to know what a tape is to start at
 * all. It is the deck's, and the deck is a jar.
 */
public class ATapeStartsAMachine implements StartsAMachineOn {

  @Override
  public boolean handles(File file) {
    return Tape.isATape(file.getName());
  }

  /** What the tape says it was made for, or a 128 when its own name says so. */
  @Override
  public String machineFor(File file) {
    return TapeHardware.bestMachineFor(file)
        .orElseGet(() -> file.getName().toLowerCase().contains("128") ? "Spectrum 128K" : null);
  }

  @Override
  public Going start(Speccy machine, File file) {
    TapeAutoLoader loading = new TapeAutoLoader(machine, file);
    return new Going() {
      public boolean done() {
        return loading.isDone();
      }

      public void step() {
        loading.step();
      }

      public String wrong() {
        return loading.getError();
      }
    };
  }
}
