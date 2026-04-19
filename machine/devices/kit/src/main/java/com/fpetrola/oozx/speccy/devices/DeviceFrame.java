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

package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * The window of a device somebody plugs in: clipping it onto a machine is what plugs the device
 * into that machine, and taking it off unplugs it.
 * <p>
 * Which machine it is clipped to is {@link MachineFrame}'s business. What this adds is the
 * device: found on that machine, switched on from the emulator's own thread - switching a device
 * on rebuilds the machine's ports - and switched off again when the window is unclipped or the
 * machine closes. What is shown while it is plugged in is the subclass's business, told through
 * {@link #plugged}.
 */
public abstract class DeviceFrame<P extends Peripheral & Pluggable> extends MachineFrame {

  private final Class<? extends P> kind;
  private P device;

  protected DeviceFrame(String title, Class<? extends P> kind) {
    super(title);
    this.kind = kind;
    // Closing the window takes the device out of the machine: it was the device.
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        connect(machine(), device, false);
        device = null;
        plugged(null);
      }
    });
  }

  /** The device this window shows, or null while it is not clipped onto a machine that has one. */
  protected P device() {
    return device;
  }

  /** Which device on that machine this window is for: the one of its kind, unless a subclass has a better answer. */
  protected P find(Speccy machine) {
    return kind.cast(machine.peripheralRegistry.find(kind));
  }

  /** The device changed hands: this one, on another machine, or none at all. */
  protected void plugged(P device) {
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    connect(was, device, false);
    device = now == null ? null : find(now);
    connect(now, device, true);
    plugged(device);
  }

  private void connect(Speccy into, P wired, boolean connected) {
    if (into == null || wired == null) {
      return;
    }
    into.loop.later(() -> {
      wired.plugIn(connected);
      // A device that changes the machine's memory map - a ROM, RAM of its own - only arrives at
      // a reset, which is what the hardware needed too: you did not plug a Multiface into a
      // running Spectrum.
      if (into.peripheralRegistry.update()) {
        into.machine.reset(true);
      }
    });
  }
}
