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

package model.interfaces;

public interface ISpectrumBus {
  void connectComponent(IComponent component);

  byte readPort(int port);

  void writePort(int port, byte value);

  int readMemory(int address);

  void writeMemory(int address, byte value);

  void pageInROM(byte[] romData);

  void handleError(String errorMessage);

  IULA getULA();

  IMemory getMemory();

  int mergeFloatingBus(int i, int i1, int i2);
}
