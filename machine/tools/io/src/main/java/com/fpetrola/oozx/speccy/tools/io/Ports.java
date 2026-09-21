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


package com.fpetrola.oozx.speccy.tools.io;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.z80.registers.RegisterName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every port the machine touched, counted - and this is a device, not a hook.
 * <p>
 * It plugs into the backplane like any board and is wired to all sixteen address lines at once,
 * but it never drives the data lines: a read it hears is answered with nothing, so whatever
 * really answers that port still wins the bus and the machine cannot tell this is listening.
 * Which is how a logic analyser is put on a bus, and the reason nothing in the emulator had to
 * grow a way of being listened to.
 */
public class Ports extends AbstractPeripheral {

  /** What a port was asked for and told, and who did the asking. */
  public static final class Port {
    private final int port;
    private long reads;
    private long writes;
    private int lastWritten = -1;
    private final Map<Integer, Integer> from = new LinkedHashMap<>();

    Port(int port) {
      this.port = port;
    }

    public int port() {
      return port;
    }

    public long reads() {
      return reads;
    }

    public long writes() {
      return writes;
    }

    /** The last byte written here, or -1 if it has only ever been read. */
    public int lastWritten() {
      return lastWritten;
    }

    /** The code that touched this port, and how often each place did. */
    public Map<Integer, Integer> from() {
      return Map.copyOf(from);
    }
  }

  private final Speccy machine;
  private final Map<Integer, Port> ports = new LinkedHashMap<>();

  public Ports(Speccy machine) {
    super(List.of());
    this.machine = machine;
    // Wired to every line and needing nothing on them, which is every port there is.
    ports(Wired.at(0x0000, 0x0000, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        touched(port, false, 0);
        return BusAnswer.NONE;
      }

      public void write(int port, byte value) {
        touched(port, true, value & 0xff);
      }
    }));
    machine.peripheralRegistry.register(this);
    machine.peripheralRegistry.activateType(getClass(), true);
  }

  /**
   * On every machine and always wanted: this is not a board somebody chose in the settings, it
   * is an instrument somebody clipped on, and it goes away when the window does.
   */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }

  private synchronized void touched(int port, boolean written, int value) {
    Port seen = ports.computeIfAbsent(port & 0xffff, Port::new);
    if (written) {
      seen.writes++;
      seen.lastWritten = value;
    } else {
      seen.reads++;
    }
    // Where the machine is while the port is answered, which for an IN or an OUT is the
    // instruction doing it: the one thing a table of ports cannot be read without.
    seen.from.merge(machine.cpu.getOoz80().getState().getRegister(RegisterName.PC).read(),
        1, Integer::sum);
  }

  /** Every port that was touched, the busiest first. */
  public synchronized List<Port> touched() {
    List<Port> seen = new ArrayList<>(ports.values());
    seen.sort((one, other) ->
        Long.compare(other.reads + other.writes, one.reads + one.writes));
    return seen;
  }

  public synchronized void forget() {
    ports.clear();
  }

  /** Unplugs itself, which is the whole of letting the machine go. */
  public void close() {
    machine.peripheralRegistry.activateType(getClass(), false);
  }
}
