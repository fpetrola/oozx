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
import com.fpetrola.z80.opcodes.references.WordNumber;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class AddStatesIO<T extends WordNumber> implements IO<T> {
  private State<T> state;

  public AddStatesIO() {
  }

  public void setState(State<T> state) {
    this.state = state;
  }

  void contend_port_preio(WordNumber port) {
    if ((port.intValue() & 0xc000) == 0x4000) {
      addPCEvent(port, 1);
    } else
      getState().tstates++;
  }

  private void addPCEvent(WordNumber port, int time) {
    getState().addEvent(new Event(time, "PC", port.intValue(), null));
  }

  void contend_port_postio(WordNumber port) {
    if ((port.intValue() & 0x0001) != 0) {
      if ((port.intValue() & 0xc000) == 0x4000) {
        addPCEvent(port, 1);
        addPCEvent(port, 1);
        addPCEvent(port, 1);
      } else {
        getState().tstates += 3;
      }
    } else {
      addPCEvent(port, 3);
    }
  }

  private State<T> getState() {
    return state;
  }

  public T in(T port) {
    T value = createValue(port.intValue() >> 8);
    contend_port_preio(port);
    getState().addEvent(new Event(0, "PR", port.intValue(), value.intValue()));
    contend_port_postio(port);
    return value;
  }

  public void out(T port, T value) {
    contend_port_preio(port);
    getState().addEvent(new Event(0, "PW", port.intValue(), value.intValue()));
    contend_port_postio(port);
  }
}
