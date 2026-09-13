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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A Pentagon with half a megabyte, which needs five bits to name a page where the port has three.
 * The two it is short of are bits 6 and 7 of the same port, which a 128 does not decode at all, so
 * nothing else about the machine changes: same ROMs, same timings, same lack of contention.
 */
@Singleton
public class Pentagon512 extends Pentagon {
  @Inject
  public Pentagon512(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                     Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  @Override
  protected int pageAt(int slot) {
    if (slot != 3 || paging.special()) return super.pageAt(slot);
    int port = paging.port7ffd() & 0xff;
    return (port & 0x07) + ((port & 0xc0) >> 3);
  }

  @Override
  public String shortName() {
    return "Pentagon512";
  }

  @Override
  public String getName() {
    return "Pentagon 512K";
  }
}
