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

package fuse.tstates;

import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;

public class AddStatesIO implements IO {
  private State state;

  public AddStatesIO() {
  }

  public void setState(State state) {
    this.state = state;
  }

  void contend_port_preio(Integer port) {
    if ((port & 0xc000) == 0x4000) {
      addPCEvent(port, 1);
    } else
      getState().clock.addTStates(1);
  }

  private void addPCEvent(Integer port, int time) {
    getState().addEvent(new Event(time, "PC", port, null));
  }

  void contend_port_postio(Integer port) {
    if ((port & 0x0001) != 0) {
      if ((port & 0xc000) == 0x4000) {
        addPCEvent(port, 1);
        addPCEvent(port, 1);
        addPCEvent(port, 1);
      } else {
        getState().clock.addTStates(3);
      }
    } else {
      addPCEvent(port, 3);
    }
  }

  private State getState() {
    return state;
  }

  public int in(int port) {
    int value = port >> 8;
    contend_port_preio(port);
    getState().addEvent(new Event(0, "PR", port, value));
    contend_port_postio(port);
    return value;
  }

  public void out(int port, int value) {
    contend_port_preio(port);
    getState().addEvent(new Event(0, "PW", port, value));
    contend_port_postio(port);
  }
}
