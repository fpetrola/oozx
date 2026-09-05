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

public interface IMemory {
  byte read(int address);

  void write(int address, byte value);

  void pageInROM(byte[] romData);

  byte[] getROM();

  byte[] getRAM();

  boolean isContended(int address, int page);

  int getContentionDelay(int address, int tStates, String model);

  void setPage(int slot, int bank); // For 128K/+2/+3 paging

  int getPage(int bank);

  int getROMBank();
}
