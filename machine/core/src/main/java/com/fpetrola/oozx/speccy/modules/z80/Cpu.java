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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.peripherals.EmulatorControl;

/**
 * All a Spectrum model needs from the processor it drives.
 * <p>
 * The machines used to hold a concrete {@link Z80}, which drags in Swing, snapshot loading and
 * poke files along with it. What they actually use is this: raise an interrupt at the end of a
 * frame, read the clock to place events within one, and keep the CPU's own time base aligned
 * when the frame boundary moves the clock back.
 */
public interface Cpu {

  /** The clock the CPU drives. Machines read it to place events within a frame. */
  SpectrumZ80Clock getClock();

  void interrupt();

  /**
   * A frame boundary just moved the clock back by {@code frameLength}; move the CPU's own
   * bookkeeping with it, so the moment interrupts became enabled stays comparable to the clock.
   */
  void rebaseInterruptWindow(int frameLength);

  /**
   * Only here because {@code Ui} is static and reaches through the CPU to find the core it
   * should update. It belongs on the user interface, and moves there when Ui becomes an
   * injected {@code UserInterface} rather than a class full of statics.
   */
  EmulatorControl getEmulatorCore();
}
