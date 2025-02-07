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
  private DirectAccessProcessor<Collection<Integer>> addressesSupplier;
  private DirectAccessProcessor<Collection<Integer>> originSupplier;
  private DirectAccessProcessor<Collection<Integer>> allSupplier;

  public DirectAccessWordNumber(int i, int pc, int address) {
    super(i);
    this.pc = pc;
    this.addressesSupplier = new DirectAccessProcessor<>() {
      public void process(Collection<Integer> integers, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
        integers.add(address);
      }

      public int getDepth() {
        return 1;
      }

      public boolean contains(DirectAccessWordNumber directAccessWordNumber) {
        return directAccessWordNumber == DirectAccessWordNumber.this;
      }
    };

    this.originSupplier = new DirectAccessProcessor<>() {
      public void process(Collection<Integer> integers, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
      }

      public int getDepth() {
        return 1;
      }

      public boolean contains(DirectAccessWordNumber directAccessWordNumber) {
        return directAccessWordNumber == DirectAccessWordNumber.this;
      }
    };
    this.allSupplier = new DirectAccessProcessor<>() {
      public void process(Collection<Integer> integers, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
      }

      public int getDepth() {
        return 1;
      }

      public boolean contains(DirectAccessWordNumber directAccessWordNumber) {
        return directAccessWordNumber == DirectAccessWordNumber.this;
      }
    };
  }

  public DirectAccessWordNumber(int i, int pc, DirectAccessProcessor<Collection<Integer>> addressesSupplier, DirectAccessProcessor<Collection<Integer>> originSupplier, DirectAccessProcessor<Collection<Integer>> allSupplier) {
    super(i);
    this.pc = pc;
    this.addressesSupplier = addressesSupplier;
    this.originSupplier = originSupplier;
    this.allSupplier = allSupplier;
  }

  @Override
  public <T extends WordNumber> T left(int i) {
    return super.left(i);
  }

  public IntegerWordNumber createInstance(int value) {
    return new DirectAccessWordNumber(value & 0xFFFF, pc, addressesSupplier, originSupplier, allSupplier);
  }

  @Override
  public <T extends WordNumber> T process(T execute) {
    if (execute instanceof DirectAccessWordNumber directAccessWordNumber) {
      DirectAccessProcessor<Collection<Integer>> newSupplier = new DirectAccessProcessor<>() {
        public void process(Collection<Integer> addresses, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
          addressesSupplier.process(addresses, processors);
          directAccessWordNumber.addressesSupplier.process(addresses, processors);
        }

        public int getDepth() {
          int result = execute instanceof DirectAccessWordNumber directAccessWordNumber ? directAccessWordNumber.addressesSupplier.getDepth() : 1;
          return Math.max(result, addressesSupplier.getDepth()) + 1;
        }

        public boolean contains(DirectAccessWordNumber directAccessWordNumber1) {
          return addressesSupplier.contains(directAccessWordNumber1) || execute instanceof DirectAccessWordNumber directAccessWordNumber && directAccessWordNumber.addressesSupplier.contains(directAccessWordNumber1);
        }
      };

      return (T) new DirectAccessWordNumber(value, pc, newSupplier, originSupplier, createAllSupplier(execute));
    }
    return (T) this;
  }

  private <T extends WordNumber> DirectAccessProcessor<Collection<Integer>> createAllSupplier(T execute) {
    DirectAccessProcessor<Collection<Integer>> allSupplier1 = new DirectAccessProcessor<>() {
      public void process(Collection<Integer> addresses, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
        addressesSupplier.process(addresses, processors);
        originSupplier.process(addresses, processors);

        if (execute instanceof DirectAccessWordNumber directAccessWordNumber) {
          directAccessWordNumber.allSupplier.process(addresses, processors);
        }
      }

      public int getDepth() {
        int result = execute instanceof DirectAccessWordNumber directAccessWordNumber ? directAccessWordNumber.allSupplier.getDepth() : 1;
        int max = Math.max(result, addressesSupplier.getDepth());
        max = Math.max(max, originSupplier.getDepth());
        return max + 1;
      }

      public boolean contains(DirectAccessWordNumber directAccessWordNumber1) {
        return addressesSupplier.contains(directAccessWordNumber1) || originSupplier.contains(directAccessWordNumber1) || execute instanceof DirectAccessWordNumber directAccessWordNumber && directAccessWordNumber.allSupplier.contains(directAccessWordNumber1);
      }
    };
    return allSupplier1;
  }

  public <T extends WordNumber> T processOrigin(T execute) {
    if (execute instanceof DirectAccessWordNumber directAccessWordNumber) {
      DirectAccessProcessor<Collection<Integer>> newSupplier = new DirectAccessProcessor<>() {
        public void process(Collection<Integer> addresses, Collection<DirectAccessProcessor<Collection<Integer>>> processors) {
          originSupplier.process(addresses, processors);
          directAccessWordNumber.addressesSupplier.process(addresses, processors);
        }

        public int getDepth() {
          int result = execute instanceof DirectAccessWordNumber directAccessWordNumber ? directAccessWordNumber.addressesSupplier.getDepth() : 1;
          int max = Math.max(result, originSupplier.getDepth());
          return max + 1;
        }

        public boolean contains(DirectAccessWordNumber directAccessWordNumber1) {
          return originSupplier.contains(directAccessWordNumber1) || execute instanceof DirectAccessWordNumber directAccessWordNumber && directAccessWordNumber.addressesSupplier.contains(directAccessWordNumber1);
        }
      };
      return (T) new DirectAccessWordNumber(value, pc, addressesSupplier, newSupplier, createAllSupplier(execute));
    }
    return (T) this;
  }

  public Collection<Integer> getAddressesSupplier() {
    Collection<Integer> t = new LinkedList<>();
    addressesSupplier.process(t, new HashSet<>());
    return t;
  }

  public Collection<Integer> getOriginSupplier() {
    Collection<Integer> integers = new LinkedList<>();
    originSupplier.process(integers, new HashSet<>());
    return integers;
  }

  public Collection<Integer> getAllSupplier() {
    Collection<Integer> integers = new HashSet<>();
    Collection<DirectAccessProcessor<Collection<Integer>>> processors = new HashSet<>();
    allSupplier.process(integers, processors);
//    if (processors.size() > 1000)
//      System.out.println("dagdgagd");
    return integers;
  }

  public interface DirectAccessProcessor<T> {
    void process(T t, Collection<DirectAccessProcessor<T>> processors);

    int getDepth();

    boolean contains(DirectAccessWordNumber directAccessWordNumber);
  }
}

