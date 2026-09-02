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

package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.peripherals.t.AttachedFrame;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * The window of a device somebody plugs in: clipping it onto a machine is what plugs the device
 * into that machine, and taking it off unplugs it.
 * <p>
 * The window finds its device on the machine it was clipped to, switches it on from the
 * emulator's own thread - switching a device on rebuilds the machine's ports - and switches it off
 * again when it is unclipped or the machine closes. What is shown while it is plugged in is the
 * subclass's business, told through {@link #plugged}.
 */
public abstract class DeviceFrame<P extends Peripheral & Pluggable> extends AttachedFrame {

  private final Class<? extends P> kind;
  private Speccy machine;
  private P device;

  protected DeviceFrame(String title, Class<? extends P> kind) {
    super(title);
    this.kind = kind;
    // Closing the window takes the device out of the machine: it was the device.
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        unplug();
      }
    });
  }

  /** The device this window shows, or null while it is not clipped onto a machine that has one. */
  protected P device() {
    return device;
  }

  /** The machine the device is plugged into, or null. */
  protected Speccy machine() {
    return machine;
  }

  /** Which device on that machine this window is for: the one of its kind, unless a subclass has a better answer. */
  protected P find(Speccy machine) {
    return kind.cast(machine.peripherals.find(kind));
  }

  /** The device changed hands: this one, on another machine, or none at all. */
  protected void plugged(P device) {
  }

  @Override
  protected void attachmentChanged() {
    Speccy attached = isAttached() && getMachineWindow() instanceof EmulatorWindow window
        ? window.machine() : null;
    if (attached == machine) {
      return;
    }
    connect(false);
    machine = attached;
    device = machine == null ? null : find(machine);
    connect(true);
    plugged(device);
  }

  /** Attached, the window goes with the machine, and the device comes out of it first. */
  @Override
  protected void machineClosed() {
    unplug();
    super.machineClosed();
  }

  private void unplug() {
    connect(false);
    machine = null;
    device = null;
    plugged(null);
  }

  private void connect(boolean connected) {
    if (machine == null || device == null) {
      return;
    }
    P wired = device;
    Speccy into = machine;
    into.z80.later(() -> {
      wired.plugIn(connected);
      // A device that changes the machine's memory map - a ROM, RAM of its own - only arrives at
      // a reset, which is what the hardware needed too: you did not plug a Multiface into a
      // running Spectrum.
      if (into.peripherals.update()) {
        into.machine.reset(true);
      }
    });
  }
}
