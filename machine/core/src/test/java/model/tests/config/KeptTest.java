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

package model.tests.config;

import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Kept;
import com.fpetrola.oozx.config.Keeps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What something that wants to come back as it was can count on: what it puts is read back the same
 * however the environment carried it, what it never put reads as it asked, and it never sees how.
 */
class KeptTest {

  @Test
  void whatIsPutComesBackTheSameFromTheEnvironmentsOwnForm() {
    Kept into = Configuration.fresh();
    into.put("title", "Manic Miner").put("frames", 1L << 40).put("speed", 2.5).put("paused", true)
        .put("ram", new byte[]{0, 1, (byte) 0xff});
    into.in("window").put("x", 120).put("y", -4);
    into.add("drive").put("disk", "a.dsk");
    into.add("drive").put("disk", "b.dsk");

    Kept from = Configuration.kept(Configuration.bytes(into));

    assertEquals("Manic Miner", from.text("title", ""));
    assertEquals(1L << 40, from.number("frames", 0));
    assertEquals(2.5, from.real("speed", 0));
    assertTrue(from.flag("paused", false));
    assertArrayEquals(new byte[]{0, 1, (byte) 0xff}, from.bytes("ram"));
    assertEquals(120, from.in("window").number("x", 0));
    assertEquals(-4, from.in("window").number("y", 0));
    assertEquals(List.of("a.dsk", "b.dsk"), from.all("drive").stream().map(drive -> drive.text("disk", "")).toList());
  }

  @Test
  void whatWasNeverPutReadsAsTheReaderAsks() {
    Kept nothing = Configuration.kept(Configuration.bytes(Configuration.fresh()));
    assertEquals("none", nothing.text("title", "none"));
    assertEquals(7, nothing.number("frames", 7));
    assertEquals(1.5, nothing.real("speed", 1.5));
    assertFalse(nothing.flag("paused", false));
    assertNull(nothing.bytes("ram"));
    assertEquals(List.of(), nothing.all("drive"));
    assertFalse(nothing.has("window"));
    assertEquals(3, nothing.in("window").number("x", 3), "a group that is not there reads as empty");
  }

  @Test
  void whatTheEnvironmentKeepsIsInItsFileAfterASave(@TempDir Path dir) {
    File file = dir.resolve("config.json").toFile();
    Configuration configuration = new Configuration(file);
    configuration.kept("game-browser").put("query", "manic");
    configuration.save();
    assertEquals("manic", new Configuration(file).kept("game-browser").text("query", ""));
  }

  @Test
  void aKeeperIsToldBackWhatItSaid() {
    class Counter implements Keeps {
      int count;
      String label = "";

      public String keptAs() {
        return "counter";
      }

      public void keep(Kept into) {
        into.put("count", count).put("label", label);
      }

      public void restore(Kept from) {
        count = (int) from.number("count", 0);
        label = from.text("label", "");
      }
    }
    Counter before = new Counter();
    before.count = 42;
    before.label = "lives";
    Kept kept = Configuration.fresh();
    before.keep(kept.in(before.keptAs()));

    Counter after = new Counter();
    after.restore(Configuration.kept(Configuration.bytes(kept)).in(after.keptAs()));
    assertEquals(42, after.count);
    assertEquals("lives", after.label);
  }
}
