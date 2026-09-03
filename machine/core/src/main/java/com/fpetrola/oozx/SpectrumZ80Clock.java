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

package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.DefaultZ80Clock;

/**
 * Adding T-states is the most repeated thing the machine does - half a dozen times per
 * instruction - so it is the one addition it inherits and nothing else: what used to wait on the
 * clock here, the tape, waits on an event now, the way everything else that wants a future
 * T-state does.
 */
public class SpectrumZ80Clock extends DefaultZ80Clock {

  /** The description is for the test clock, which records what each addition was for. */
  public void addTStates(int tStatesToAdd, String description) {
    addTStates(tStatesToAdd);
  }

  /** Moves the clock without counting the move as time that has passed: a speed change repositions it. */
  public void rebaseTStates(int newTStates) {
    this.tStates = newTStates;
  }

  public long getAbsTstates() {
    return tStates;
  }
}
