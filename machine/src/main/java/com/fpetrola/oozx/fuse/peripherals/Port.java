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

package com.fpetrola.oozx.fuse.peripherals;

// Structure for port response
public class Port {
  public int mask;
  public int value;
  public PortReadFunction read;
  public PortWriteFunction write;

  public Port(int mask, int value, PortReadFunction read, PortWriteFunction write) {
    this.mask = mask;
    this.value = value;
    this.read = read;
    this.write = write;
  }

  // Functional interfaces for port read/write
  @FunctionalInterface
  public interface PortReadFunction {
      byte apply(int port, byte[] attached);
  }

  @FunctionalInterface
  public interface PortWriteFunction {
      void apply(int port, byte data);
  }
}
