/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse.bridge;

import com.fpetrola.oozx.fuse.FuseLibretroConnector;
import com.fpetrola.oozx.fuse.KVPair;
import com.fpetrola.oozx.fuse.LibretroCore;
import com.fpetrola.oozx.fuse.TStateUpdate;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

public class GetTStatesHistory implements EmulatorCommand<List<TStateUpdate>> {
  public static List<TStateUpdate> tstatesUpdates = new ArrayList<>();

  private static List<TStateUpdate> getTstatesUpdates() {
    if (FuseLibretroConnector.noTest)
      tstatesUpdates.clear();
    return tstatesUpdates;
  }

  public static void setTstatesUpdates(List<TStateUpdate> tstatesUpdates) {
    GetTStatesHistory.tstatesUpdates = tstatesUpdates;
  }

  public static void addTStateUpdate(byte tstatesToAdd, String description, int tstates) {
    if (!FuseLibretroConnector.noTest) {
//      int pc = z80.ooz80.getState().getPc().read().intValue();
      int pc = 0;
//    if (tstates == 20 && pc == 50758) {
//      System.out.println("addTStateUpdate");
//    }

      boolean a = description.startsWith("uidisplay_plot8:");
//a= true;
      if (!a) {
        if (tstatesToAdd != 0)
          getTstatesUpdates().add(new TStateUpdate(tstates, tstatesToAdd & 0Xff, description, pc));
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
