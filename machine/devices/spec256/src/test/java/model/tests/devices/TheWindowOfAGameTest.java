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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Peripheral;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/** The window a desk can clip onto a machine, and the switch it offers. */
class TheWindowOfAGameTest extends MachineTest {
  private static final int LINE = 40, COLUMN = 3;

  @TempDir
  Path where;

  private Speccy speccy;

  private static int addressOf(int line, int column) {
    return Planes.RAM + ((line & 0xc0) << 5) + ((line & 0x07) << 8) + ((line & 0x38) << 2) + column;
  }

  private Spec256Peripheral started() throws IOException {
    speccy = silentMachine();
    select(speccy, speccy.machine.model(Spec48.class));
    speccy.picture.active = true;
    byte[] colours = new byte[Planes.LENGTH];
    for (int pixel = 0; pixel < 8; pixel++) {
      colours[(addressOf(LINE, COLUMN) - Planes.RAM) * 8 + (7 - pixel)] = (byte) 100;
    }
    Files.write(where.resolve("game.GFX"), colours);
    byte[] sna = new byte[27 + 0xc000];
    sna[24] = 0x40;
    sna[27 + 0x1800 + (LINE / 8) * 32 + COLUMN] = 0x07;
    sna[27 + addressOf(LINE, COLUMN) - Planes.RAM] = (byte) 0xff;
    Path snapshot = where.resolve("game.sna");
    Files.write(snapshot, sna);
    Snapshots.of(speccy).load(snapshot.toString());
    speccy.loop.applyWhatWasDeferred();
    paint();
    return (Spec256Peripheral) speccy.peripheralRegistry.find(Spec256Peripheral.class);
  }

  private void paint() {
    speccy.display.refreshAll();
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
  }

  private int firstPixel() {
    return speccy.picture.pixels[(Display.BORDER_HEIGHT + LINE) * Picture.STRIDE
        + (Display.BORDER_WIDTH_COLS + COLUMN) * 8];
  }

  @Test
  void theDeskIsOfferedItWithoutKnowingWhatItIs() {
    Equipment offered = ServiceLoader.load(Equipment.class).stream()
        .map(ServiceLoader.Provider::get)
        .filter(kind -> kind.name().equals("Spec256"))
        .findFirst().orElse(null);

    assertNotNull(offered, "nothing on this classpath offers a game in 256 colours");
  }

  @Test
  void theSwitchGoesBackAndForthBetweenTheGamesColoursAndTheMachinesOwn() throws IOException {
    Spec256Peripheral game = started();
    assertTrue(game.inItsColours());
    assertEquals(speccy.picture.palette[100], firstPixel(), "the game's colour, to begin with");

    game.inItsColours(false);
    paint();
    assertEquals(Picture.SINCLAIR[7], firstPixel(), "the byte and its attribute, as the machine has them");
    assertEquals("Spec256", speccy.processors.current(), "and the followers are still running");

    game.inItsColours(true);
    paint();
    assertEquals(speccy.picture.palette[100], firstPixel(), "and back, with no reloading of anything");
  }
}
