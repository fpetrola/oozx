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

package model.connected;

import model.interfaces.*;
import model.harness.TestDriver;

import java.util.ArrayList;
import java.util.List;

public class ConnectedSpectrumBus implements ISpectrumBus {
  private List<IPeripheral> peripherals = new ArrayList<>();
  private final IMemory memory;
  private final IULA ula;
  private final TestDriver testDriver;
  private String model;

  public ConnectedSpectrumBus(IMemory memory1, IULA ula1, TestDriver testDriver) {
    memory = memory1;
    ula = ula1;
    this.testDriver = testDriver;
  }

  @Override
  public void connectComponent(IComponent component) {
    component.connectToBus(this);
    if (component instanceof IPeripheral) {
      peripherals.add((IPeripheral) component);
    }
  }

  @Override
  public byte readPort(int port) {
    for (IPeripheral p : peripherals) {
      if (p.handlesPortRead(port)) {
        return p.handlePortRead(port);
      }
    }
    return (byte) 0xFF;
  }

  @Override
  public void writePort(int port, byte value) {
    testDriver.addInstruction((byte) 0x3E, value);
    testDriver.waitExecution();
    testDriver.addInstruction((byte) 0xD3, (byte) port);
    testDriver.waitExecution();
//    for (IPeripheral p : peripherals) {
//      if (p.handlesPortWrite(port)) {
//        p.handlePortWrite(port, value);
//      }
//    }
  }

  @Override
  public int readMemory(int address) {
    return memory.read(address) & 0xff;
  }

  @Override
  public void writeMemory(int address, byte value) {
    memory.write(address, value);
  }

  @Override
  public void pageInROM(byte[] romData) {
    memory.pageInROM(romData);
  }

  @Override
  public void handleError(String errorMessage) {
    for (IPeripheral p : peripherals) {
      if (p instanceof IZXInterface1) {
        ((IZXInterface1) p).pageROMIn(true);
      }
    }
  }

  @Override
  public IULA getULA() {
    return ula;
  }

  @Override
  public IMemory getMemory() {
    return memory;
  }

  @Override
  public int mergeFloatingBus(int i, int i1, int i2) {
    return 0;
  }
}
