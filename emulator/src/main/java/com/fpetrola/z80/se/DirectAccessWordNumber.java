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

import com.fpetrola.z80.opcodes.references.IntegerWordNumber;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.*;

public class DirectAccessWordNumber extends IntegerWordNumber {
  public final int pc;
  private boolean readOnly;

  public Set<Integer> addresses = new LinkedHashSet<>();

  public DirectAccessWordNumber(int i, int pc, int address) {
    super(i);
    this.pc = pc;
    this.addresses.add(address);
  }

  public DirectAccessWordNumber(int i, int pc, Set<Integer> addresses) {
    super(i);
    this.pc = pc;
    this.addresses = addresses;
  }

  public DirectAccessWordNumber(int i, int pc, int address, boolean readOnly) {
    this(i, pc, Set.of(address));
    this.readOnly = readOnly;
  }

  public IntegerWordNumber createInstance(int value) {
    return new DirectAccessWordNumber(value & 0xFFFF, pc, new LinkedHashSet<>(addresses));
  }

  @Override
  public <T extends WordNumber> T process(T execute) {
//    LinkedHashSet<Integer> integers = new LinkedHashSet<>(addresses);
//    if (execute instanceof DirectAccessWordNumber directAccessWordNumber)
//      integers.addAll(directAccessWordNumber.addresses);
//    return (T) new DirectAccessWordNumber(value, pc, integers);
    return (T) this;
  }

  public <T extends WordNumber> T processOrigin(T execute) {
    LinkedHashSet<Integer> integers = new LinkedHashSet<>(addresses);
    if (execute instanceof DirectAccessWordNumber directAccessWordNumber)
      integers.addAll(directAccessWordNumber.addresses);
    return (T) new DirectAccessWordNumber(value, pc, integers);
  }

  public Collection<Integer> getAddressesSupplier() {
    return addresses;
  }

  public Collection<Integer> getOriginSupplier() {
    Collection<Integer> integers = new LinkedList<>();
    return integers;
  }

  public Collection<Integer> getAllSupplier() {
    Collection<Integer> integers = new HashSet<>();
    return integers;
  }
}

