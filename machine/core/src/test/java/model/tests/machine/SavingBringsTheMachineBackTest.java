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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writing the machine down, which until now printed a line and did nothing.
 * <p>
 * Getting a machine into an interesting state can be most of an afternoon - a program loaded
 * from tape, set up, and left on the screen worth working with - and without this the only way
 * back to it was to do all of that again.
 */
class SavingBringsTheMachineBackTest {

  private static Speccy machine() {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    // Saving a machine to a file is asked of it the way a window asks, so the test needs the same
    // adapter a window would have; building one is what an application does at startup.
    speccy.z80.mockCore = new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(speccy);
    return speccy;
  }

  @Test
  void a_machine_can_be_written_to_a_file(@TempDir Path where) throws Exception {
    Speccy speccy = machine();
    Path file = where.resolve("state.z80");

    speccy.z80.mockCore.saveState(file.toString());

    assertTrue(Files.exists(file), "nothing was written");
    // Not measured against the size of the machine's memory: a .z80 is compressed, and a machine
    // that has just been switched on is mostly the same byte over and over, so a perfectly good
    // snapshot of one is under a kilobyte. What it holds is the next test's business.
    assertTrue(Files.size(file) > 0, "the file is empty");
  }

  @Test
  void what_was_written_can_be_read_back_into_a_machine(@TempDir Path where) throws Exception {
    Speccy saved = machine();
    // Something to recognise on the other side, put where the screen is.
    saved.z80.ooz80.getState().getMemory().write(16384 + 100, (byte) 0x5A);
    Path file = where.resolve("state.z80");
    saved.z80.mockCore.saveState(file.toString());

    Speccy reopened = machine();
    reopened.z80.loadSnap(file.toString());

    assertEquals(0x5A, reopened.memory.readByteInternal(16384 + 100) & 0xFF,
        "the machine that came back is not the machine that was written");
  }
}
