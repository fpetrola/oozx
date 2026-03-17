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

package model.tests.devices;

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * A machine built for a test must never ask the operating system for a speaker.
 * <p>
 * The real device reaches the platform's audio server through JNI, and a crash down there takes
 * the whole JVM with it: the run stops mid-class and every test after it is never reached, which
 * reads as a hang rather than as a failure. Nothing under test listens, so nothing should open.
 */
class SilentSoundTest {

  @Test
  void aMachineBuiltForATestOpensNoAudioLine() {
    assertInstanceOf(SilentSoundDevice.class, MachineTest.silentMachine().sound.card(),
        "tests must get the device that opens nothing");
  }
}
