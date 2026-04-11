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

//    JFrame jFrame = new JFrame();
//    JComponent panel = speccy.control.getPanel();
//    jFrame.setContentPane(panel);
//    jFrame.pack();
//    jFrame.setVisible(true);

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