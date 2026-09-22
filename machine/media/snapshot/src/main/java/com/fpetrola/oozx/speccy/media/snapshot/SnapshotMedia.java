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

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** Snapshots on every machine: the seam between a file and the machine it was taken on. */
public class SnapshotMedia extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(Snapshots.class);
    // Which formats there are, said here because this is the module that can look: the readers
    // live under the plugin mechanism and cannot go asking, and whoever loads a snapshot should
    // not have to be told to tell them first.
    com.fpetrola.emulation.helpers.snapshots.SnapshotFactory.alsoRead(
        com.fpetrola.oozx.plugins.Plugins.found(
            com.fpetrola.emulation.helpers.snapshots.SnapshotFile.class));
  }
}
