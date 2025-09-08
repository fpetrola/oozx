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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Who, of one kind - readers, writers, listeners - answers each of the 65536 ports.
 * <p>
 * A program asks the same few dozen ports thousands of times a frame, and what decodes a port
 * changes only when something is registry in or out: so a port is worked out the first time it is
 * asked and kept until the ports change, which they say themselves.
 */
final class PortDecoder {
  private static final PortHandler[] NOBODY = new PortHandler[0];

  private final Backplane backplane;
  private final Predicate<PortHandler> ofThisKind;
  private final PortHandler[][] at = new PortHandler[0x10000][];
  private int seen = -1;

  PortDecoder(Backplane backplane, Predicate<PortHandler> ofThisKind) {
    this.backplane = backplane;
    this.ofThisKind = ofThisKind;
  }

  PortHandler[] at(int port) {
    if (seen != backplane.version()) {
      java.util.Arrays.fill(at, null);
      seen = backplane.version();
    }
    PortHandler[] answering = at[port];
    return answering != null ? answering : (at[port] = decode(port));
  }

  private PortHandler[] decode(int port) {
    List<PortHandler> answering = new ArrayList<>();
    for (Wired each : backplane.wired()) {
      if (each.wiring().answers(port) && ofThisKind.test(each.handler())) {
        answering.add(each.handler());
      }
    }
    return answering.isEmpty() ? NOBODY : answering.toArray(NOBODY);
  }
}
