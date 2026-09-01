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

import com.fpetrola.oozx.speccy.bridge.KVPair;
import com.fpetrola.oozx.speccy.bridge.LibretroCore;
import com.fpetrola.oozx.speccy.bridge.TStateUpdate;
import com.fpetrola.z80.registers.Register;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GetTStatesHistory implements EmulatorCommand<List<TStateUpdate>> {
  public static List<TStateUpdate> tstatesUpdates = new ArrayList<>();

  private static List<TStateUpdate> getTstatesUpdates() {
    return tstatesUpdates;
  }

  public static void setTstatesUpdates(List<TStateUpdate> tstatesUpdates) {
    GetTStatesHistory.tstatesUpdates = tstatesUpdates;
  }

  public static void addTStateUpdate(int tstatesToAdd, Supplier<String> description, long tstates, Register pcRegister) {
    {
      int pc = pcRegister.read();

      String s = description.get();
      boolean a = s.startsWith("uidisplay_plot8:");
//a= true;
      if (!a) {
        if (tstatesToAdd != 0)
          getTstatesUpdates().add(new TStateUpdate(tstates, tstatesToAdd & 0Xff, s, pc));
      }
    }
  }

  public List<TStateUpdate> execute(LibretroCore core) {
    return getTStateUpdates(core);
  }

  public static List<TStateUpdate> getTStateUpdates(LibretroCore core) {
    Pointer pData = core.retro_tstates_history();
    if (pData == null) {
      return getLocalTStateUpdates(core);
    } else {
      return getRemoteTStateUpdates(core, pData);
    }
  }

  public static List<TStateUpdate> getLocalTStateUpdates(LibretroCore core) {
    List<TStateUpdate> out = new ArrayList<>();
    out.addAll(getTstatesUpdates());
    return out;
  }

  public static List<TStateUpdate> getRemoteTStateUpdates(LibretroCore core, Pointer pData) {
    if (pData != null)
      return getRemoteTStateUpdates2(core);
    else
      return new ArrayList<>();
  }

  public static List<TStateUpdate> getRemoteTStateUpdates2(LibretroCore core) {
    Pointer pData1 = core.retro_tstates_history();
    KVPair first = new KVPair(pData1);
    KVPair[] pairs = (KVPair[]) first.toArray(2000);

    List<TStateUpdate> out = new ArrayList<>();
    for (KVPair kv : pairs) {
      kv.read();
      String description = kv.description;
      if (description != null && !"empty".equals(description))
        out.add(new TStateUpdate(kv.key, kv.value, description, kv.pc));
    }

    return out;
  }
}
