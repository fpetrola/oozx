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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.bytecode.tests.ZxObject;
import com.fpetrola.z80.minizx.sync.SyncChecker;
import com.fpetrola.z80.opcodes.references.WordNumber;

public abstract class SyncSpectrumApplication<T> extends SpectrumApplication<T> {
  public SyncChecker syncChecker = new DummySyncChecker();
  protected ZxObject[] objectMemory = new ZxObject[0x10000];

  public SyncSpectrumApplication() {
    io = new MiniZXIO();
  }

  public int mem(int address, int pc) {
    syncChecker.checkSyncJava(address, 0, pc);
    return getMem()[address] & 0xff;
  }

  public void wMem(int address, int value, int pc) {
    syncChecker.checkSyncJava(address, value, pc);
//    System.out.println("pc: " + pc);
    wMem(address, value);
  }

  public void wMem16(int address, int value, int pc) {
    syncChecker.checkSyncJava(address, value, pc);
    getMem()[address] = value & 0xFF;
    syncChecker.checkSyncJava(address + 1, value, pc);
    getMem()[address + 1] = value >> 8;
    if (address == 32985) {
      System.out.println();
    }
  }

  public int mem16(int address, int pc) {
    syncChecker.checkSyncJava(address, 0, pc);
    return mem(address + 1) * 256 + mem(address);
  }

  public void wMem(int address, int value) {
//    long start = System.nanoTime();
//    while (start + 4000 >= System.nanoTime()) ;
    getMem()[address] = value & 0xff;
    objectMemory[address] = new ZxObject(value);
    replaceWithObject(address, value);
  }

  @Override
  public int in(int port, int pc) {
    syncChecker.checkSyncInJava(port, pc);
    return ((MiniZXIO)io).in2(WordNumber.createValue(port)).intValue();
  }

  @Override
  public int in(int port) {
    syncChecker.checkSyncInJava(port, -1);
    return ((MiniZXIO)io).in2(WordNumber.createValue(port)).intValue();
  }

  protected void replaceWithObject(int address, int value) {

  }

  public void setSyncChecker(SyncChecker syncChecker) {
    this.syncChecker = syncChecker;
    syncChecker.init(this);
  }

  public class DummySyncChecker implements SyncChecker {
    public int getByteFromEmu(Integer index) {
      return getMem()[index];
    }
  }

  @Override
  public int R() {
    return syncChecker.getR();
  }
}
