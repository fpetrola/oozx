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

package model.harness;

import com.fpetrola.oozx.speccy.bridge.LibretroCore;
import com.fpetrola.oozx.speccy.bridge.EmulatorCommand;

public class GetUlaContention implements EmulatorCommand {
  private final int i;

  public GetUlaContention(int i) {
    this.i = i;
  }

  public Object execute(LibretroCore core) {
    return core.retro_get_ula_contention(i);
  }
}
