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

package com.fpetrola.oozx.speccy.tools.rzx;

import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.ide.rzx.RzxWriter;
import model.tags.Slow;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A recording that reaches its end starts again, which is the loop button being on by default.
 * <p>
 * Tried on a recording cut to forty frames, because the point is what happens at the end and a
 * full one takes minutes to get there.
 */
class ARecordingThatStartsOverTest {

  private static File recording() {
    try {
      return model.harness.TestFiles.testFile("/rzx/jsw-full.rzx");
    } catch (Exception missing) {
      throw new IllegalStateException("the jsw-full.rzx test resource is missing", missing);
    }
  }

  @Slow
  @Test
  void itStartsOverWhenTheRecordingEnds() throws Exception {
    Path shortOne = Files.createTempFile("forty-frames", ".rzx");
    RzxWriter.writeExtended(new RzxParser().parseFile(recording().getPath()), 40, List.of(),
        shortOne, RzxWriter.Mode.CONTINUE_BLOCK);
    RzxFrame player = new RzxFrame();
    SwingUtilities.invokeAndWait(() -> player.open(shortOne.toFile()));
    int furthest = 0;
    boolean wentBack = false;
    for (long until = System.currentTimeMillis() + 10_000;
         !wentBack && System.currentTimeMillis() < until; Thread.sleep(20)) {
      int at = frameShown(player);
      wentBack = at < furthest;
      furthest = Math.max(furthest, at);
    }
    assertTrue(furthest >= 20, "the recording never got going: " + player.getTitle());
    assertTrue(wentBack, "reached the end and did not start over: " + player.getTitle());
    assertTrue(player.getTitle().contains("Playing"), "started over but stopped: " + player.getTitle());
    SwingUtilities.invokeAndWait(player::dispose);
  }

  private static int frameShown(RzxFrame player) {
    Matcher shown = Pattern.compile("(\\d+) of \\d+ frames").matcher(player.getTitle());
    return shown.find() ? Integer.parseInt(shown.group(1)) : 0;
  }
}
