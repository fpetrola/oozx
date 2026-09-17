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
