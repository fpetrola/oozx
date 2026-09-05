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
