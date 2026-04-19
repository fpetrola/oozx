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
