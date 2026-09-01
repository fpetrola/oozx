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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.speccy.bridge.LibretroCore;

public class WritePortCommand implements EmulatorCommand<Object> {
  public final int port;
  public final int value;
  public final boolean contended;

  public WritePortCommand(int port, int value, boolean contended) {
    this.port = port;
    this.value = value;
    this.contended = contended;
  }

  public Object execute(LibretroCore core) {
    if (contended) {
      core.retro_write_port(port, value);
    } else
      core.retro_write_port(port, value);

    return null;
  }
}
