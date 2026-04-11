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

package model.tests.regression;

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.tags.Slow;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.emulation.helpers.snapshots.SnapshotSaver;
import org.junit.jupiter.api.*;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slow
public class TestGameExecution {


  @AfterAll
  static void tearDown() {
  }

  @Tag("slow")
  @Test
  void test48KExecuteEmlyn() {
    Speccy speccy = MachineTest.silentMachine();

    speccy.speed.emulation = 1000000;

    String emlyn = loadFromResourceAsString("g.compressed");
    SpectrumState spectrumState = SnapshotSaver.loadSnapshotFromUnicodePacked(emlyn);
    Snapshots.of(speccy).load(spectrumState);
    extracted(speccy);
  }

  private String loadFromResourceAsString(String s) {
    try (var inputStream = getClass().getResourceAsStream("/" + s)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Resource not found: " + s);
      }
      StringBuilder contentBuilder = new StringBuilder();
      try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          contentBuilder.append(line).append("\n");
        }
      }
        return contentBuilder.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return s;
  }

  private void extracted(Speccy speccy) {
    long states = 0;
    String lastX = "";
    String expected = "Score: 4 - 2";
    String x = "";

    while (states < 61936) {
      states++;
      MachineTest.step(speccy);
      int localGoals = speccy.memory.peek(0x9253);
      int visitGoals = speccy.memory.peek(0x9254);

      x = "Score: " + localGoals + " - " + visitGoals;
      if (!x.equals(lastX)) {
        System.out.println(x);
      }
      lastX = x;

      if (x.equals(expected))
        break;
    }

    assertEquals(expected, x);
  }
}