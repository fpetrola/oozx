/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.fuse.ports.PortHandler;
import com.fpetrola.oozx.fuse.ports.PortHandlerAdapter;

import java.util.Arrays;
import java.util.List;

// Structure for peripheral information
public class Peripheral {
  public boolean[] option; // Preferences option controlling this peripheral
  public List<? extends PortHandler> ports; // List of ports this peripheral responds to
  public boolean hardReset; // Hard reset required when added/removed
  public ActivateFunction activate; // Function called when peripheral is activated

  public Peripheral(Port... ports) {
    this(Arrays.stream(ports).map(PortHandlerAdapter::new).toList());
  }

  public Peripheral(boolean[] option, List<Port> ports, boolean hardReset, ActivateFunction activate) {
    this(option, hardReset, activate);
    this.ports = ports.stream().map(PortHandlerAdapter::new).toList();
  }

  public Peripheral(boolean[] option, PortHandler... ports) {
    this(option, false, null);
    this.ports = List.of(ports);
  }

  public Peripheral(boolean[] option, boolean hardReset, ActivateFunction activate) {
    this.option = option;
    this.hardReset = hardReset;
    this.activate = activate;
  }

  public Peripheral(List<? extends PortHandler> ports) {
    this(null, false, null);
    this.ports = ports;
  }

  public Peripheral(PortHandler... ports) {
    this(null, false, null);
    this.ports = Arrays.stream(ports).toList();
  }

  @FunctionalInterface
  public interface ActivateFunction {
    void apply();
  }
}
