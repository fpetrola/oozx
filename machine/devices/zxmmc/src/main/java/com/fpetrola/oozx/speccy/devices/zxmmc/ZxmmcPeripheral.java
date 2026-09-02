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
package com.fpetrola.oozx.speccy.devices.zxmmc;

import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.devices.ide.MmcBoard;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The ZXMMC: a card slot and nothing else, with the select at 0x1f and the card's byte at 0x3f.
 * Ported from Fuse's zxmmc.c.
 */
@Singleton
public class ZxmmcPeripheral extends MmcBoard {

  @Inject
  public ZxmmcPeripheral(Module module) {
    super(module, 0x001f, 0x003f);
  }
}
