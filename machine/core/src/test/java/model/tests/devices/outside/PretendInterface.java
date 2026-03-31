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

package model.tests.devices.outside;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
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
    ports(Wired.at(0xffff, PORT, new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        return BusAnswer.of(ANSWER);
      }
    }));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    switchedOnFor = machine;
  }

  /** A 48K box: it says so itself, and that is the only thing deciding where it appears. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd();
  }

  public SpectrumMachine switchedOnFor() {
    return switchedOnFor;
  }
}
