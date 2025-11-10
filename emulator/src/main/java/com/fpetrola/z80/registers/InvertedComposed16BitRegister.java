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

package com.fpetrola.z80.registers;

public class InvertedComposed16BitRegister implements RegisterPair {
  private final Register high;
  private final Register low;
  private final String name;
  protected int data;

  public InvertedComposed16BitRegister(String name, RegisterName registerH, RegisterName registerL) {
    this.name = name;
    this.low = new Register() {
      public int read() {
        return data & 0xFF;
      }

      public int getLength() {
        return 0;
      }

      public void write(int value) {
        data = (data & 0xFF00) | (value & 0xFF);
      }

      public void increment() {
        data = (data + 1) & 0xFFFF;
      }

      public void decrement() {
        data = (data - 1) & 0xFFFF;
      }

      public String getName() {
        return registerL.name();
      }
    };

    this.high = new Register() {
      public int read() {
        return data >> 8;
      }

      public void write(int value) {
        data = (data & 0x00FF) | ((value & 0xFF) << 8);
      }

      public void increment() {
        data = (data + 0x100) & 0xFFFF;
      }

      public void decrement() {
        data = (data - 0x100) & 0xFFFF;
      }

      public String getName() {
        return registerH.name();
      }

      public int getLength() {
        return 0;
      }
    };
  }

  public Register getHigh() {
    return high;
  }

  public Register getLow() {
    return low;
  }

  public void increment() {
    data = (data + 1) & 0xFFFF;
  }

  public void decrement() {
    data = (data - 1) & 0xFFFF;
  }

  public String getName() {
    return name;
  }

  public int read() {
    return data;
  }

  public int getLength() {
    return 0;
  }

  public void write(int value) {
    this.data = value & 0xFFFF;
  }
}
