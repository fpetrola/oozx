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
package model.tests.timing;

import com.fpetrola.oozx.speccy.modules.timer.Speed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The machine is what takes up a change of speed: a frame ends, and the tick that ends it reads
 * what the speed is now. At nothing per cent a frame never ends, so the change that would undo it
 * is never read and the emulator does not come back - which is what happened when the spinner in
 * the settings was wound down to zero.
 */
class ASpeedTheMachineCanComeBackFromTest {
  private final Speed speed = new Speed();

  @Test
  void nothingPerCentIsTheSlowestItWillGo() {
    speed.setEmulation(0);

    assertEquals(Speed.SLOWEST, speed.emulation, "a speed it could not come back from");
  }

  @Test
  void whatIsAboveTheSlowestIsTakenAsItIs() {
    speed.setEmulation(25);

    assertEquals(25, speed.emulation);
  }

  @Test
  void asFastAsItGoesIsStillAsFastAsItGoes() {
    speed.setEmulation(Speed.UNLIMITED);

    assertEquals(Speed.UNLIMITED, speed.emulation);
  }
}
