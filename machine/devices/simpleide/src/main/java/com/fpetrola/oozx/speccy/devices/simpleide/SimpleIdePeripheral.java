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
package com.fpetrola.oozx.speccy.devices.simpleide;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.devices.ide.IdeBoard;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The Simple 8-bit IDE interface: one channel on the low byte of the bus and nothing else,
 * answering any port with bit 4 low, the register in bits 8, 12 and 13.
 */
@Singleton
public class SimpleIdePeripheral extends IdeBoard {

  @Inject
  public SimpleIdePeripheral() {
    super(false, 2);
    ports(Wired.at(0x0010, 0x0000, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(channel.read(register(port)));
      }

      public void write(int port, byte value) {
        channel.write(register(port), value & 0xff);
      }
    }));
  }

  private static IdeChannel.Register register(int port) {
    return IdeChannel.Register.values()[port >> 8 & 0x01 | port >> 11 & 0x06];
  }
}
