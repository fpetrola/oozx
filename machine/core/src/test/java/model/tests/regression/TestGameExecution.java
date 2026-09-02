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

import model.tags.Slow;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.emulation.helpers.snapshots.SnapshotSaver;
import org.junit.jupiter.api.*;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slow
public class TestGameExecution extends SpeccyBaseForTests {
  public TestGameExecution() {
    speccy= createSpeccy();
  }

  @BeforeAll
  public static void beforeall() {
  }

  @BeforeEach
  void setUp() {
  }


  @AfterAll
  static void tearDown() {
    Emulation.noTest = false;
  }

  @Tag("slow")
  @Test
  void test48KExecuteEmlyn() {
    Emulation.noTest = true;
    createSpeccy();
    speccy.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int len) {
      }
    });
    speccy.init();
    speccy.uiDisplay.active = false;

    speccy.settings.current.emulationSpeed = 1000000;

    SnapshotSaver snapshotSaver = new SnapshotSaver();
//    String snapshotAsUnicodePacked = snapshotSaver.getSnapshotAsUnicodePacked(new RegistersBase(speccy.z80.ooz80.getState()), speccy.z80.ooz80.getState());

//    JFrame jFrame = new JFrame();
//    JComponent panel = speccy.z80.mockCore.getPanel();
//    jFrame.setContentPane(panel);
//    jFrame.pack();
//    jFrame.setVisible(true);

    String emlyn = loadFromResourceAsString("g.compressed");
    SpectrumState spectrumState = snapshotSaver.loadSnapshotFromUnicodePacked(emlyn);
    speccy.z80.loadSnap(spectrumState);
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
    speccy.z80.bridgeCommand = (a, b) -> null;
    String lastX = "";
    String expected = "Score: 4 - 2";
    String x = "";

    while (states < 61936) {
      states++;
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
      int localGoals = speccy.memory.readByteInternal(0x9253);
      int visitGoals = speccy.memory.readByteInternal(0x9254);

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