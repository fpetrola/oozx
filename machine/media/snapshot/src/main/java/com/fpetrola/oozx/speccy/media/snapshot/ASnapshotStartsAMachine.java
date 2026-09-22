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
 */package com.fpetrola.oozx.speccy.media.snapshot;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.StartsAMachineOn;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;

import java.io.File;

/**
 * A machine put back as it was: a snapshot is a machine that already ran, so there is nothing
 * to load and nothing to type. It reads the formats there are, which is whatever this build
 * knows plus whatever arrived.
 */
public class ASnapshotStartsAMachine implements StartsAMachineOn {

  @Override
  public boolean handles(File file) {
    return com.fpetrola.emulation.helpers.snapshots.SnapshotFactory.getSnapshot(file) != null;
  }

  @Override
  public Going start(Speccy machine, File file) {
    // It comes up as fast as it can and then runs at whatever it was told to: a snapshot is not
    // watched while it loads, because there is nothing to watch.
    machine.speed.emulation = 10000;
    Snapshots.of(machine).load(file.getAbsolutePath());
    return null;
  }
}
