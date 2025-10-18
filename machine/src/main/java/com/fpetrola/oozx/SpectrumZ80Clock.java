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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;
import com.fpetrola.z80.cpu.DefaultZ80Clock;

import java.util.function.Supplier;

public class SpectrumZ80Clock extends DefaultZ80Clock {
  public void addTStates(int tStatesToAdd, String description) {
    log(() -> description, (byte) tStatesToAdd);
    addTStates(tStatesToAdd);
  }

  public void addTStates(int tStatesToAdd, Supplier<String> description) {
    log(description, (byte) tStatesToAdd);
    addTStates(tStatesToAdd);
  }

  public void log(Supplier<String> description, byte data) {
    GetTStatesHistory.addTStateUpdate(data, description, tStates);
  }
}
