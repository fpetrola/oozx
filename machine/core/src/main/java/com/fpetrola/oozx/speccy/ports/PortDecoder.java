/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
