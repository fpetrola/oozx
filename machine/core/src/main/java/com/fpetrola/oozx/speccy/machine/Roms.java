/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;

/**
 * The ROM images a machine and its devices are made with, asked for by what they are: a machine
 * says which of its pages it wants, a device says it wants its own. Nothing here is a file: which
 * bytes stand in for a 128K's second page, and where they are kept, is decided outside the machine.
 */
public interface Roms {
  byte[] of(Spectrum machine, int page, int length);

  byte[] of(Peripheral device, int length);
}
