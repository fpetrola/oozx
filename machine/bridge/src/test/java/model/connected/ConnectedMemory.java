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

import model.interfaces.IMemory;
import model.harness.TestDriver;

public class ConnectedMemory implements IMemory {
  private final TestDriver testDriver;

  public ConnectedMemory(TestDriver testDriver) {
    this.testDriver = testDriver;
  }

  @Override
  public byte read(int address) {
    return testDriver.readMemory(address, true);
  }

  @Override
  public void write(int address, byte value) {
    testDriver.writeMemory(address, value, true);
  }

  @Override
  public void pageInROM(byte[] romData) {

  }

  @Override
  public byte[] getROM() {
    return new byte[0];
  }

  @Override
  public byte[] getRAM() {
    return new byte[0];
  }

  @Override
  public boolean isContended(int address, int page) {
    return false;
  }

  @Override
  public int getContentionDelay(int address, int tStates, String model) {
    return 0;
  }

  @Override
  public void setPage(int slot, int bank) {
    testDriver.writePort(0x7FFD, bank);
//    int regBC = testDriver.getRegister("BC");
//    int regA = testDriver.getRegister("A");
//    testDriver.setRegister("BC", 0x7FFD);
//    testDriver.setRegister("A", bank);
//    testDriver.addInstruction((byte) 0xED, (byte) 0x79); // OUT (C),A
//    testDriver.waitExecution();
//    testDriver.setRegister("BC", regBC);
//    testDriver.setRegister("A", regA);
  }

  @Override
  public int getPage(int bank) {
    return 0;
  }

  @Override
  public int getROMBank() {
    return 0;
  }

  public void enableLEC(boolean b) {
    testDriver.setConnectedLEC(b);
  }

  public boolean isLECEnabled() {
    return testDriver.isConnectedLEC();
  }

  public void setLECMemorySize(int i) {
    testDriver.setLECMemorySize(i);
  }
}
