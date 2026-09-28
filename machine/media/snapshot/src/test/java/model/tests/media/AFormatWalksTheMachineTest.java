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

package model.tests.media;

import com.fpetrola.emulation.helpers.snapshots.SnapshotException;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.formats.SnapshotFormat;
import com.fpetrola.oozx.speccy.modules.display.Border;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.z80.bytecode.RegistersBase;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A format that arrives as a way in is asked before the old readers, and reads and writes by walking the machine. */
class AFormatWalksTheMachineTest extends MachineTest {

  /** The border colour and A, in two bytes: a format as small as one can be, found on the path. */
  public static class TwoBytes implements SnapshotFormat {
    public boolean reads(File file) {
      return file.getName().endsWith(".two");
    }

    public boolean writes(File file) {
      return reads(file);
    }

    public void read(byte[] file, Speccy machine, Consumer<String> notes) throws SnapshotException {
      if (file.length != 2) throw new SnapshotException("two bytes, not " + file.length);
      machine.accept(part -> {
        if (part instanceof Border border) border.becomes(file[0]);
        if (part instanceof Cpu cpu) new RegistersBase(cpu.getOoz80().getState()).setRegA(file[1] & 0xff);
      });
      notes.accept("read two bytes");
    }

    public byte[] write(Speccy machine, Consumer<String> notes) {
      byte[] file = new byte[2];
      machine.accept(part -> {
        if (part instanceof Border border) file[0] = (byte) border.colour();
        if (part instanceof Cpu cpu) file[1] = (byte) new RegistersBase(cpu.getOoz80().getState()).getRegA();
      });
      return file;
    }
  }

  @TempDir
  Path folder;

  @Test
  void itReadsAFileIntoTheMachineItWalks() throws Exception {
    Speccy speccy = silentMachine();
    Path file = Files.write(folder.resolve("game.two"), new byte[]{3, 0x42});
    Snapshots.of(speccy).load(file.toString());
    assertEquals(3, speccy.display.border.colour());
    assertEquals(0x42, new RegistersBase(speccy.cpu.getOoz80().getState()).getRegA());
    assertEquals(List.of("read two bytes"), Snapshots.of(speccy).notes());
  }

  @Test
  void itWritesTheMachineItWalks() throws Exception {
    Speccy speccy = silentMachine();
    speccy.display.border.becomes(6);
    new RegistersBase(speccy.cpu.getOoz80().getState()).setRegA(0x17);
    Path file = folder.resolve("saved.two");
    Snapshots.of(speccy).save(file.toString());
    assertArrayEquals(new byte[]{6, 0x17}, Files.readAllBytes(file));
  }

  @Test
  void aFileItRefusesIsRefused() throws Exception {
    Speccy speccy = silentMachine();
    Path file = Files.write(folder.resolve("cut.two"), new byte[]{3});
    assertThrows(RuntimeException.class, () -> Snapshots.of(speccy).load(file.toString()));
  }
}
