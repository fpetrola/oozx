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

import com.fpetrola.z80.cpu.DefaultZ80Clock;
import com.fpetrola.z80.tstates.Contention.Kind;

/**
 * Adding T-states is the most repeated thing the machine does - half a dozen times per
 * instruction - so it is the one addition it inherits and nothing else: what used to wait on the
 * clock here, the tape, waits on an event now, the way everything else that wants a future
 * T-state does.
 */
public class SpectrumZ80Clock extends DefaultZ80Clock {

  /**
   * Time the ULA adds to that bus cycle: wait states while it owns the bus, and the port cycles
   * it times. Named, like the two below, so a clock that keeps a trace can say what each
   * addition was for without this one carrying a description.
   */
  public void contend(Kind cycle, int tStates) {
    addTStates(tStates);
  }

  /** The acknowledge cycle of an interrupt, maskable or not. */
  public void acknowledge(int tStates) {
    addTStates(tStates);
  }

  /** One phase of an instruction, the way the recorded vectors count them; only the machine under test adds time this way. */
  public void phase(Kind kind, int tStates) {
    addTStates(tStates);
  }

  /** Moves the clock without counting the move as time that has passed: a speed change repositions it. */
  public void rebaseTStates(int newTStates) {
    this.tStates = newTStates;
  }

  public long getAbsTstates() {
    return tStates;
  }
}
