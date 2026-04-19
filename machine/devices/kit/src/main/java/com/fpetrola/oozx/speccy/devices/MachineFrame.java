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
import com.fpetrola.oozx.speccy.windows.AttachedFrame;

/**
 * A window clipped onto a machine: it knows which machine it is showing, and is told when that
 * changes.
 * <p>
 * Clipping is the only way it finds one. There is no list of open machines to search and no
 * setter: the window is put against a machine's window and that is the machine it is about,
 * which is what makes two of them, on two machines, mean two different things.
 */
public abstract class MachineFrame extends AttachedFrame {

  private Speccy machine;

  protected MachineFrame(String title) {
    super(title);
  }

  /** The machine this window is clipped to, or null while it is clipped to nothing. */
  protected Speccy machine() {
    return machine;
  }

  /** It changed hands: from one machine to another, from none to one, or from one to none. */
  protected void machineChanged(Speccy was, Speccy now) {
  }

  @Override
  protected void attachmentChanged() {
    Speccy now = isAttached() && getMachineWindow() instanceof EmulatorWindow window ? window.machine() : null;
    if (now == machine) {
      return;
    }
    Speccy was = machine;
    machine = now;
    machineChanged(was, now);
  }

  @Override
  protected void machineClosed() {
    Speccy was = machine;
    machine = null;
    if (was != null) {
      machineChanged(was, null);
    }
    super.machineClosed();
  }
}
