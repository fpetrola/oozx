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
 * Nought is not a speed. It is what pause is for, and as a number it puts the machine's clock at
 * nothing a second, which is a division by nothing in everything that sizes itself by the clock.
 * <p>
 * What actually hung a slow machine was not this: it was a sound buffer fixed at a second, which a
 * frame below about two per cent overran, taking the thread that runs the machine with it. That is
 * fixed where it was, in Sound, and this is only the one value that is not a speed.
 */
class ASpeedTheMachineCanComeBackFromTest {
  private final Speed speed = new Speed();

  @Test
  void nothingPerCentIsTheSlowestItWillGo() {
    speed.setEmulation(0);

    assertEquals(Speed.SLOWEST, speed.emulation, "nought is not a speed");
  }

  @Test
  void whatIsAboveTheSlowestIsTakenAsItIs() {
    speed.setEmulation(25);

    assertEquals(25, speed.emulation);
  }

  @Test
  void onePerCentIsAllowed() {
    speed.setEmulation(1);

    assertEquals(1, speed.emulation, "a frame a second is slow, and slow is not broken");
  }

  @Test
  void asFastAsItGoesIsStillAsFastAsItGoes() {
    speed.setEmulation(Speed.UNLIMITED);

    assertEquals(Speed.UNLIMITED, speed.emulation);
  }
}
