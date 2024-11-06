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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VirtualRegisterVersionHandler<T extends WordNumber> {
  public List<VirtualRegister<T>> versions = new ArrayList<>();

  public void addVersion(VirtualRegister<T> virtualRegister) {
    versions.add(virtualRegister);
  }

  public VirtualRegister<T> getBiggestScopeFor(VirtualRegister<T> register) {
    if (versions.isEmpty())
      return register;
    else {
      VirtualRegister<T> biggerScope = versions.stream().filter(r -> r.getScope().isIncluding(register)).sorted((o1, o2) -> o1.getRegisterLine() - o2.getRegisterLine()).max(Comparator.comparingInt(r -> r.getScope().size())).get();
      return biggerScope;
    }
  }
}
