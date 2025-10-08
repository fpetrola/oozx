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

import java.util.List;

// Structure for peripheral information
public class Peripheral {
  public boolean[] option; // Preferences option controlling this peripheral
  public List<Port> ports; // List of ports this peripheral responds to
  public boolean hardReset; // Hard reset required when added/removed
  public ActivateFunction activate; // Function called when peripheral is activated

  public Peripheral(boolean[] option, List<Port> ports, boolean hardReset, ActivateFunction activate) {
    this.option = option;
    this.ports = ports;
    this.hardReset = hardReset;
    this.activate = activate;
  }

  @FunctionalInterface
  public interface ActivateFunction {
      void apply();
  }
}
