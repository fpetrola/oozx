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

package com.fpetrola.z80.analysis.sprites;

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ExecutionStep;

public class AddressRange<T extends WordNumber> {
  private ExecutionStep<T>lastStep;
  private int firstAddress = Integer.MAX_VALUE;
  private int lastAddress = 0;
  int distance = 100;

  public AddressRange() {
  }

  public AddressRange(int address, ExecutionStep<T> firstStep) {
    add(address, firstStep);
  }

  public String getName() {
    return "[" + Helper.formatAddress(firstAddress) + "-" + Helper.formatAddress(lastAddress) + "]";
  }

  public boolean canAdd(int address, ExecutionStep<T> step) {
    if (lastStep == null)
      return true;
    else if (isInside(address))
      return true;
    else return Math.abs(firstAddress - address) < distance || Math.abs(lastAddress - address) < distance;
  }

  public void add(int address, ExecutionStep<T> step) {
    lastStep = step;
    if (address < firstAddress)
      firstAddress = address;

    if (address > lastAddress)
      lastAddress = address;
  }

  public boolean mergeIfRequired(AddressRange<T> b) {
    boolean merged = isInside(b.firstAddress) || isInside(b.lastAddress);

    if (merged) {
      firstAddress = Math.min(firstAddress, b.firstAddress);
      lastAddress = Math.min(lastAddress, b.lastAddress);
    }

    return merged;
  }

  private boolean isInside(int address) {
    return (address >= firstAddress && address <= lastAddress);
  }

}
