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

package model.tests.devices.outside;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * A device the emulator was not built with: it lives in the tests, is announced in their own
 * service file, and nothing in the main sources mentions it. If it answers a port, a peripheral
 * really is something that can arrive from outside.
 * <p>
 * It asks for the Sound module in its constructor for no reason other than to prove that a
 * discovered device is wired by the injector like any other, and it answers on an odd port no
 * machine here uses, so a running emulator is unaffected by it being switched on.
 */
@Singleton
public class PretendInterface extends AbstractPeripheral {
  public static final int PORT = 0x1235;
  public static final byte ANSWER = 0x5a;

  private SpectrumMachine switchedOnFor;

  @Inject
  public PretendInterface(Sound sound) {
    super(List.of());
    if (sound == null) throw new IllegalStateException("a discovered device was not wired");
    ports(new DefaultPortHandler(0xffff, PORT, true, false) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return ANSWER;
      }
    });
  }

  @Override
  public void activate(SpectrumMachine machine) {
    switchedOnFor = machine;
  }

  /** A 48K box: it says so itself, and that is the only thing deciding where it appears. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128);
  }

  public SpectrumMachine switchedOnFor() {
    return switchedOnFor;
  }
}
