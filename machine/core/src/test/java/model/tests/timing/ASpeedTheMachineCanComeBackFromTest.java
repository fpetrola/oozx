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
