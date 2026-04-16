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
