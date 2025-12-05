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

package model.tests;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.OOSpectrumConnector;
import com.fpetrola.oozx.fuse.bridge.FuseBaseForTests;
import com.fpetrola.oozx.fuse.sound.JavaSoundDevice;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotSaver;
import org.junit.jupiter.api.*;
import snapshots.SpectrumState;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGameExecution extends FuseBaseForTests {

  @BeforeAll
  public static void beforeall() {
  }

  @BeforeEach
  void setUp() {
  }


  @AfterAll
  static void tearDown() {
  }

  @Test
  void test48KExecuteEmlyn() {
    OOSpectrumConnector.noTest = true;
    createFuse();
    fuse.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int len) {
      }
    });
    fuse.init();
    fuse.uiDisplay.active = false;

    fuse.settings.current.emulationSpeed = 1000000;

    SnapshotSaver snapshotSaver = new SnapshotSaver();
//    String snapshotAsUnicodePacked = snapshotSaver.getSnapshotAsUnicodePacked(new RegistersBase(fuse.z80.ooz80.getState()), fuse.z80.ooz80.getState());

//    JFrame jFrame = new JFrame();
//    JComponent panel = fuse.z80.mockCore.getPanel();
//    jFrame.setContentPane(panel);
//    jFrame.pack();
//    jFrame.setVisible(true);

    String emlyn = loadFromResourceAsString("g.compressed");
    SpectrumState spectrumState = snapshotSaver.loadSnapshotFromUnicodePacked(emlyn);
    fuse.z80.loadSnap(spectrumState);
    extracted(fuse);
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

  private void extracted(Fuse fuse) {
    long states = 0;
    fuse.z80.bridgeCommand = (a, b) -> null;
    String lastX = "";
    String expected = "Score: 4 - 2";
    String x = "";

    while (states < 61936) {
      states++;
      fuse.z80.doOpcodes();
      fuse.eventManager.eventDoEvents();
      int localGoals = fuse.memory.readByteInternal(0x9253);
      int visitGoals = fuse.memory.readByteInternal(0x9254);

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