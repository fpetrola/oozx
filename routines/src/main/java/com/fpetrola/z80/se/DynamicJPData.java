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

package com.fpetrola.z80.se;

import com.fpetrola.z80.helpers.Helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DynamicJPData {
  private final int pc;
  private final int pointer;
  private final int pointerAddress;
  public Set<Integer> cases = new HashSet<>();

  public DynamicJPData(int pc, int pointer, int pointerAddress) {
    this.pc = pc;
    this.pointer = pointer;
    this.pointerAddress = pointerAddress;
  }

  public void addCase(int aCase) {
    System.out.println("0x" + Helper.formatAddress(pointerAddress()) + ":  " + Helper.formatAddress(aCase));
    cases.add(aCase);
  }

  public int pc() {
    return pc;
  }

  public int pointer() {
    return pointer;
  }

  public int pointerAddress() {
    return pointerAddress;
  }
}
