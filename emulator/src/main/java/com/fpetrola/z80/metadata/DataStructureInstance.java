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

package com.fpetrola.z80.metadata;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DataStructureInstance {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataStructureInstance that = (DataStructureInstance) o;
    return Objects.equals(addresses, that.addresses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addresses);
  }

  public Set<Integer> addresses = new HashSet<>();

  public void addAddress(int address) {
  if (addresses.contains(33024) && address == 33032)
    System.out.println("oh!!");

    if (!addresses.isEmpty() && address - addresses.iterator().next().intValue() > 100)
      System.out.println("mucha distancia!");
    addresses.add(address);
  }
}
