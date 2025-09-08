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
package com.fpetrola.oozx.speccy.ports;

/**
 * The port bus: a read or a write of one of the 65536 ports.
 * <p>
 * One interface, three layers, each adding what the one below does not know. {@link DecodedPortBus}
 * is the wires: who answers, who wins the lines, and no clock. MachinePortBus over it is this
 * machine's: the T-state an access takes and what an unattached port reads as. ContendedPortBus
 * over that is what the CPU is given: the ULA's waits on either side. The ULA and the sound chip
 * are handed the middle one, because the ULA is what does the waiting and cannot be made to wait
 * on itself.
 */
public interface PortBus {
  byte read(int port);

  void write(int port, byte b);
}
