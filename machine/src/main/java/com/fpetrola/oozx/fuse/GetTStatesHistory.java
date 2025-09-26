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

package com.fpetrola.oozx.fuse;

import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

public class GetTStatesHistory implements EmulatorCommand<List<TStateUpdate>> {
  static List<TStateUpdate> tstatesUpdates = new ArrayList<>();

  public static List<TStateUpdate> getTstatesUpdates() {
    if (FuseLibretroExample.noTest)
      tstatesUpdates.clear();
    return tstatesUpdates;
  }

  public static void setTstatesUpdates(List<TStateUpdate> tstatesUpdates) {
    GetTStatesHistory.tstatesUpdates = tstatesUpdates;
  }

  public List<TStateUpdate> execute(LibretroCore core) {
    Pointer pData = core.retro_tstates_history();
    List<TStateUpdate> out = new ArrayList<>();
    if (pData == null) {
      out.addAll(getTstatesUpdates());
    } else {
      KVPair first = new KVPair(pData);
      KVPair[] pairs = (KVPair[]) first.toArray(1000);

      for (KVPair kv : pairs) {
        kv.read();
        String description = kv.description;
        if (description != null && !"empty".equals(description))
          out.add(new TStateUpdate(kv.key, kv.value, description, kv.pc));
      }
    }
    return out;
  }
}
