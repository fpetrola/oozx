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
package com.fpetrola.oozx.speccy.devices.zxmmc;

import com.fpetrola.oozx.speccy.devices.ide.MmcBoard;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The ZXMMC: a card slot and nothing else, with the select at 0x1f and the card's byte at 0x3f.
 */
@Singleton
public class ZxmmcPeripheral extends MmcBoard {

  @Inject
  public ZxmmcPeripheral() {
    super(0x001f, 0x003f);
  }
}
