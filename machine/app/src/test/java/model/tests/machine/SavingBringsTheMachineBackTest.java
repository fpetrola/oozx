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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
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
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    // Saving a machine to a file is asked of it the way a window asks, so the test needs the same
    // adapter a window would have; building one is what an application does at startup.
    speccy.control = new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(speccy);
    return speccy;
  }

  @Test
  void a_machine_can_be_written_to_a_file(@TempDir Path where) throws Exception {
    Speccy speccy = machine();
    Path file = where.resolve("state.z80");

    speccy.control.saveState(file.toString());

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
    saved.cpu.getOoz80().getState().getMemory().write(16384 + 100, (byte) 0x5A);
    Path file = where.resolve("state.z80");
    saved.control.saveState(file.toString());

    Speccy reopened = machine();
    Snapshots.of(reopened).load(file.toString());

    assertEquals(0x5A, reopened.memory.peek(16384 + 100) & 0xFF,
        "the machine that came back is not the machine that was written");
  }
}
