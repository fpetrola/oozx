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

package com.fpetrola.oozx.speccy.tools.cassette;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.EmulatorWindow;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cassette in this window plays into the machine it is clipped to and no other.
 * <p>
 * The deck used to be handed to the window by the application, which had to pair the two by
 * hand whenever they were built in either order. The deck belongs to the machine, so being
 * clipped on is the whole of it: this is what that replaced.
 */
class ClippedOnItPlaysIntoThatMachineTest {

  @TempDir
  Path folder;

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  private JInternalFrame windowOf(Speccy speccy) {
    class Machine extends JInternalFrame implements EmulatorWindow {
      public JComponent picture() {
        return this;
      }

      public Speccy machine() {
        return speccy;
      }
    }
    return new Machine();
  }

  /** A header block and a data block, which is the least that reads as a cassette. */
  private File aTape() throws IOException {
    byte[] header = new byte[21];
    header[0] = 19;
    header[3] = 0;
    for (int letter = 0; letter < 10; letter++) {
      header[5 + letter] = ' ';
    }
    byte[] data = {4, 0, (byte) 0xFF, 1, 2, 3};
    byte[] tape = new byte[header.length + data.length];
    System.arraycopy(header, 0, tape, 0, header.length);
    System.arraycopy(data, 0, tape, header.length, data.length);
    Path file = folder.resolve("a.tap");
    Files.write(file, tape);
    return file.toFile();
  }

  @Test
  void clippedOnItFindsThatMachinesDeckWithNobodyHandingItOver() throws IOException {
    Speccy speccy = speccy();
    CassetteFrame window = new CassetteFrame();
    window.open(aTape());

    window.setMachineWindow(windowOf(speccy));

    assertTrue(window.isAttached(), "should arrive clipped onto the machine");
    assertSame(Tape.of(speccy), window.deck(), "the deck it plays into is that machine's own");

    window.setMachineWindow(null);
    assertNull(window.deck(), "carried away, it plays into nothing");
  }
}
