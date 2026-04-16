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
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

/**
 * A card slot on two ports: one chooses the card, both its lines being active low, and every
 * access to the other clocks a byte each way. The ZXMMC and the DivMMC differ only in which
 * ports they are, so this is the slot and each board says where it sits.
 */
public class MmcSlot {

  private final MmcCard card = new MmcCard();
  private final int selectPort;
  private final int dataPort;
  private boolean selected;

  public MmcSlot(int selectPort, int dataPort) {
    this.selectPort = selectPort;
    this.dataPort = dataPort;
  }

  public MmcCard card() {
    return card;
  }

  public Wired selectPort() {
    return Wired.at(0x00ff, selectPort, new DefaultPortHandler(false, true) {
      public void write(int port, byte value) {
        selected = (value & 0x03) == 0x02;
      }
    });
  }

  public Wired dataPort() {
    return Wired.at(0x00ff, dataPort, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of((selected ? card.read() : 0xff));
      }

      public void write(int port, byte value) {
        if (selected) card.write(value & 0xff);
      }
    });
  }
}
