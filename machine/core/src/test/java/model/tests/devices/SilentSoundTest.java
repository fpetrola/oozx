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

package model.tests.devices;

import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
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
    assertInstanceOf(SilentSoundDevice.class, SpeccyBaseForTests.createSpeccy().sound.getJavaSoundDevice(),
        "tests must get the device that opens nothing");
  }
}
