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

package com.fpetrola.oozx.speccy.modules.z80;

/** What goes through the processor's ports, told to whoever listens, whichever processor is running. */
public interface PortTraffic {
  void whenRead(Processors.ReadListener listener);

  void stopTellingAboutReads(Processors.ReadListener listener);

  void whenWritten(Processors.WriteListener listener);

  void stopTellingAboutWrites(Processors.WriteListener listener);
}
