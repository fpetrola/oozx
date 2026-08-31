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

package model.tests.devices;

import model.tags.NeedsNativeCore;

import com.fpetrola.oozx.speccy.OOSpectrumConnector;

import model.harness.TestDriver;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.*;
import com.fpetrola.oozx.speccy.bridge.CommandHandler;
import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.bridge.GetTStatesHistory;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.z80.blocks.Block;
import model.connected.ConnectedZ80CPU;
import org.junit.jupiter.api.*;

import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NeedsNativeCore
public class TestSpeccySound extends SpeccyBaseForTests {
  static private TestDriver testDriver1;
  static private TestDriver testDriver2;
  private static LocalLibretroCore localLibretroCore;
  private static LibretroCore remoteCore;
  private static List<int[]> datas = new ArrayList<>();

  @BeforeAll
  public static void beforeall() {
    OOSpectrumConnector.noTest = false;
    Speccy speccy = createSpeccy();
    speccy.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int len) {
        datas.add(data);
      }
    });
    localLibretroCore = new LocalLibretroCore(speccy.eventManager, speccy.display, speccy.machine, speccy.z80, speccy.zxClock, speccy.periph, speccy);
    remoteCore = OOSpectrumConnector.core;
    CommandHandler commandHandler1 = DefaultCommandHandler.createCommandHandler(localLibretroCore);
    CommandHandler commandHandler2 = DefaultCommandHandler.createCommandHandler(remoteCore);

    testDriver1 = new TestDriver(commandHandler1);
    testDriver2 = new TestDriver(commandHandler2);
  }

  @BeforeEach
  void setUp() {
    testDriver1.reset();
    testDriver1.tstatesHistoryInit();
    testDriver2.reset();
    testDriver2.tstatesHistoryInit();
    datas.clear();
  }


  @AfterAll
  static void tearDown() {
  }

  @Disabled
  @Test
  void test48KExecuteGame() {
    String model = "48K";
    String fileName = "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80";

    testDriver1.setModel(model);
    testDriver1.loadSnapshot(fileName);

    testDriver2.setModel(model);
    testDriver2.loadSnapshot(fileName);

    List<TStateUpdate> localtStateUpdates = new ArrayList<>();
    List<TStateUpdate> remotetStateUpdates = new ArrayList<>();


    for (int i = 0; i < 1368000; i++) {
      testDriver1.tstatesHistoryInit();
      testDriver2.tstatesHistoryInit();
      for (int j = 0; j < 20; j++) {
        testDriver1.step();
        testDriver2.step();

//        int r1 = testDriver1.getRegister("R") & 0x7F;
//        int r2 = testDriver2.getRegister("R") & 0x7F;
//        assertEquals(r1, r2, "R differs at step " + i + ", substep " + j);


        int pc1 = testDriver1.getRegister("PC");
        int pc2 = testDriver2.getRegister("PC");
//        if (i4++ % 1000 == 0)
//        System.out.printf("%H %n", pc1);
        if (pc1 != pc2) {
//          List<TStateUpdate> tStateUpdates = GetTStatesHistory.getLocalTStateUpdates(localLibretroCore);
//          List<TStateUpdate> tStateUpdates2 = GetTStatesHistory.getRemoteTStateUpdates2(remoteCore);
//
//          localtStateUpdates.addAll(tStateUpdates);
//          remotetStateUpdates.addAll(tStateUpdates2);
          assertListsEqualInChunks(localtStateUpdates, remotetStateUpdates, 20, u -> u.key, "key");
          assertEquals(pc1, pc2, "PC differs at step " + i + ", substep " + j + ", PC=" + pc1 + "/" + pc2);
        }
        int a1 = testDriver1.getRegister("AF");
        int a2 = testDriver2.getRegister("AF");
        if (a1 != a2) {
          System.out.println("Difference at step " + i + ", substep " + j + ", PC=" + pc1 + "/" + pc2 + ", AF=" + a1 + "/" + a2);
          System.out.println("Local TStates updates: " + GetTStatesHistory.getLocalTStateUpdates(localLibretroCore));
          System.out.println("Remote TStates updates: " + GetTStatesHistory.getRemoteTStateUpdates2(remoteCore));
        }
//
//        int hl1 = testDriver1.getRegister("HL");
//        int hl2 = testDriver2.getRegister("HL");
//        assertEquals(hl1, hl2, "HL differs at step " + i + ", substep " + j + ", PC=" + pc1 + "/" + pc2);
//        assertEquals(a1, a2, "A differs at step " + i + ", substep " + j + ", PC=" + pc1 + "/" + pc2);
////        if (Spectrum.tstates > 62080 && Spectrum.tstates < 62087 ) {
//          System.out.println("sdsdgsdgdg");
//        }
      }

      List<TStateUpdate> tStateUpdates = GetTStatesHistory.getLocalTStateUpdates(localLibretroCore);
      List<TStateUpdate> tStateUpdates2 = GetTStatesHistory.getRemoteTStateUpdates2(remoteCore);

      if (!tStateUpdates2.isEmpty() || !tStateUpdates.isEmpty()) {
        System.out.println(" Step " + i + ": PC=" + tStateUpdates.get(0).pc + ", TStates=" + tStateUpdates.get(0).key + " -> " + tStateUpdates.get(tStateUpdates.size() - 1).key + ", updates=" + tStateUpdates.size());
      }
      localtStateUpdates.addAll(tStateUpdates);
      remotetStateUpdates.addAll(tStateUpdates2);

//      if (tStateUpdates.size() != tStateUpdates2.size()) {
//        System.out.println("Different size at step " + i);
//        System.out.println("Local: " + tStateUpdates);
//        System.out.println("Remote: " + tStateUpdates2);
////        assertEquals(tStateUpdates2, tStateUpdates);
//      }
//      assertEquals(tStateUpdates2.size(), tStateUpdates.size());

//      for (int j = 0; j < tStateUpdates.size(); j++) {
//        assertEquals(tStateUpdates2.get(j), tStateUpdates.get(j));
//      }
//      assertEquals(tStateUpdates.toString(), tStateUpdates2.toString());
//      System.out.println(tStateUpdates.get(0).pc);
//      assertEquals(tStateUpdates2, tStateUpdates);
//      int key = tStateUpdates.get(tStateUpdates.size() - 1).key;
//      System.out.println(key);
      assertListsEqualInChunks(tStateUpdates, tStateUpdates2, 20, u -> u.key, "key");
    }
//    assertEquals(initialTStates + 4 + 6 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +17
//    assertEquals((byte) 0xAA, bus.readMemory(26000));
    assertListsEqualInChunks(localtStateUpdates, remotetStateUpdates, 20, u -> u.key, "key");
  }

  public static <T, P> void assertListsEqualInChunks(
      List<T> expected,
      List<T> actual,
      int chunkSize,
      Function<T, P> propertyExtractor,
      String propertyDescription
  ) {
    // Check if lists have the same size
//    assertEquals(expected.size(), actual.size(), "Lists have different sizes");

    // Handle empty lists
    if (expected.isEmpty() && actual.isEmpty()) {
      return;
    }

    if (expected.size() != actual.size()) {
      String format = String.format(
          "Lists have different sizes: expected %d, but was %d",
          expected.size(), actual.size()
      );
      System.out.println(format);
    }

    // Iterate through lists in chunks
    for (int i = 0; i < expected.size(); i += chunkSize) {
      int endIndex = Math.min(i + chunkSize, expected.size());
      List<T> expectedChunk = expected.subList(i, endIndex);

      if (endIndex > actual.size()) {
        String format = String.format(
            "Actual list is shorter than expected at chunk starting index %d to %d: expected size %d, but was %d",
            i, endIndex - 1, expected.size(), actual.size()
        );
        System.out.println(format);
      }
      List<T> actualChunk = actual.subList(i, endIndex);

      // Check if chunks are equal
      if (!expectedChunk.equals(actualChunk)) {
        // Find the first differing element
        for (int j = 0; j < expectedChunk.size(); j++) {
          T expectedElement = expectedChunk.get(j);
          T actualElement = actualChunk.get(j);
          int globalIndex = i + j;

          if (!expectedElement.equals(actualElement)) {
            // Get the property value of the differing element in the expected list
            P expectedPropertyValue = propertyExtractor.apply(expectedElement);

            // Count elements before globalIndex with the same property value
            int matchingCount = 0;
            for (int k = 0; k < globalIndex; k++) {
              T expectedPrior = expected.get(k);
              T actualPrior = actual.get(k);
              // Count only if elements are equal and property matches
              if (expectedPrior.equals(actualPrior)) {
                P priorPropertyValue = propertyExtractor.apply(expectedPrior);
                if ((expectedPropertyValue == null && priorPropertyValue == null) ||
                    (expectedPropertyValue != null && expectedPropertyValue.equals(priorPropertyValue))) {
                  matchingCount++;
                }
              }
            }
            String format = String.format(
                "First difference found at index %d (chunk starting at %d): expected %s, but was %s. "
                + "Number of prior elements where %s is %s: %d\n PC=%d",
                globalIndex, i, expectedElement, actualElement,
                propertyDescription, expectedPropertyValue, matchingCount, ((TStateUpdate) expectedElement).pc
            );

            System.out.println(format);

            assertEquals(
                expectedChunk,
                actualChunk,
                String.format("Difference found in chunk starting at index %d to %d", i, endIndex - 1)
            );
          }
        }
      }
    }
  }

  public static <T> void assertListsEqualInChunks(List<T> expected, List<T> actual, int chunkSize) {
    // Check if lists have the same size
//    assertEquals(expected.size(), actual.size(), "Lists have different sizes");

    // Handle empty lists
    if (expected.isEmpty() && actual.isEmpty()) {
      return;
    }

    // Iterate through lists in chunks
    for (int i = 0; i < expected.size(); i += chunkSize) {
      int endIndex = Math.min(i + chunkSize, expected.size());
      List<T> expectedChunk = expected.subList(i, endIndex);
      List<T> actualChunk = actual.subList(i, endIndex);

      // Check if chunks are equal
      if (!expectedChunk.equals(actualChunk)) {
        assertEquals(
            expectedChunk,
            actualChunk,
            String.format("Difference found in chunk starting at index %d to %d", i, endIndex - 1)
        );
        // Find the first differing element
        for (int j = 0; j < expectedChunk.size(); j++) {
          T expectedElement = expectedChunk.get(j);
          T actualElement = actualChunk.get(j);
          int globalIndex = i + j;

          assertEquals(
              expectedElement,
              actualElement,
              String.format(
                  "First difference found at index %d (chunk starting at %d): expected %s, but was %s",
                  globalIndex, i, expectedElement, actualElement
              )
          );
        }
      }
    }
  }

  public static void assertListsEqualInChunks2(List expected, List actual, int chunkSize) {
//    // Check if lists have the same size
//    assertEquals(expected.size(), actual.size(), "Lists have different sizes");

    // Handle empty lists
    if (expected.isEmpty() && actual.isEmpty()) {
      return;
    }

    // Iterate through lists in chunks
    for (int i = 0; i < expected.size(); i += chunkSize) {
      int endIndex = Math.min(i + chunkSize, expected.size());
      List expectedChunk = expected.subList(i, endIndex);
      List actualChunk = actual.subList(i, endIndex);

      // Use assertEquals with a descriptive message
      assertEquals(
          expectedChunk,
          actualChunk,
          String.format("Difference found in chunk starting at index %d to %d", i, endIndex - 1)
      );
    }
  }

//  @Test
//  void test48KExecuteGame2() {
//    String model = "48K";
//    String fileName = "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80";
//
//    testDriver1.setModel(model);
//    testDriver1.loadSnapshot(fileName);
//
//    for (int i = 0; i < 1368000; i++) {
//      testDriver1.tstatesHistoryInit();
//      testDriver2.tstatesHistoryInit();
//      for (int j = 0; j < 100; j++) {
//        testDriver1.step();
//

  ////        int pc1 = testDriver1.getRegister("PC");
  ////        if (pc1 == 0) {
  ////          System.out.println("sdsdgsdgdg");
  ////        }
//      }
//    }
//  }


// Interface 1 Peripheral Tests
  @Test
  void test48KInterface1PortEFContentionT14335() {
    testDriver1.setModel("48K");
    int initialTStates = testDriver1.getTstates();
    testDriver1.writePort(0x4000, 1);
    assertEquals(initialTStates, testDriver1.getTstates());
  }

  public static void assertShortArrayListEquals(List<double[]> expectedList, List<double[]> actualList) {
    if (expectedList.size() != actualList.size()) {
      System.out.println("size");
    }
    assertEquals(expectedList.size(), actualList.size(),
        () -> "Tamaño de listas diferente: esperado " + expectedList.size() +
              ", actual " + actualList.size());

    for (int i = 0; i < expectedList.size(); i++) {
      double[] expected = expectedList.get(i);
      double[] actual = actualList.get(i);

      int finalI = i;
      Assertions.assertArrayEquals(expected, actual,
          () -> "Fila " + finalI + " diferente:\n" +
                "  Esperado: " + Arrays.toString(expected) + "\n" +
                "  Actual:   " + Arrays.toString(actual));
    }
  }

  @Test
  void testBeepPipOnSpeakerPortFE_A() {
//    ArrayList<int[]> ints = test1(testDriver2);
    ArrayList<int[]> ints1 = test1(testDriver1);

    List<double[]> localData = OOSpectrumConnector.localData;
//    List<double[]> remoteData = OOSpectrumConnector.remoteData;
//    assertShortArrayListEquals(localData, remoteData);
  }

  @Disabled
  @Test
  void testBeepPipOnSpeakerPortFE_B() {
    test1(testDriver2);
  }

  private ArrayList<int[]> test1(TestDriver testDriver3) {
    ConnectedZ80CPU cpu = new ConnectedZ80CPU(testDriver3);

    testDriver3.setModel("48K");
    testDriver3.setRegister("PC", 0xF000);

    testDriver3.writePort(0xFE, 0b0001_1000);  // = 24 decimal → altavoz ON + borde

    int i1 = 3500 * 5;
    testDriver3.setRegister("BC", 200);
    testDriver3.setRegister("HL", 0);
    testDriver3.setRegister("DE", 1);
    boolean active = false;
    for (int i = 0; i < i1; i++) {
      cpu.executeInstruction("LDIR");  // = 24 decimal → altavoz ON + borde
      testDriver3.writePort(0xFE, active ? 0b0001_1000 : 0);
      active = !active;
    }

    testDriver3.writePort(0xFE, 0b0000_0000);  // altavoz OFF
    return new ArrayList<>(datas);
  }


  @Test
  void test48KExecuteGame2() {
    String model = "48K";
    String fileName = "/home/fernando/detodo/desarrollo/m/zx/roms/jsw2.z80";


//    testDriver2.setModel(model);
//    testDriver2.loadSnapshot(fileName);
    testDriver1.setModel(model);
    testDriver1.loadSnapshot(fileName);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    int lastSize = 0;
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < 100000; i++) {
      for (int j = 0; j < 1; j++) {
//        testDriver2.step();
        testDriver1.step();
      }

      ArrayList<int[]> ints = new ArrayList<>(datas);
      List<double[]> localData = OOSpectrumConnector.localData;
      if (ints.size() != lastSize) {
        lastSize = ints.size();
        String string = Arrays.toString(datas.get(0));
        result.append(string).append("\n");
      }
//      assertTrue(ints.isEmpty());
//      List<double[]> remoteData = OOSpectrumConnector.remoteData;
//      assertShortArrayListEquals(localData, remoteData);
      localData.clear();
//      remoteData.clear();
    }


    String a = """
        [0, 0, 0, 0, 0, 0]
        [0, 0, 0, 0, 0, 0]
        [0, 0, 3, 3, 0, 0]
        [21, 21, 69, 69, 0, 0]
        [186, 186, 455, 455, 0, 0]
        [1058, 1058, 2365, 2365, 0, 0]
        [4885, 4885, 7995, 7995, 0, 0]
        [10134, 10134, 10999, 10999, 11183, 11183]
        [11034, 11034, 10712, 10712, 11183, 11183]
        [10235, 10235, 9553, 9553, 11183, 11183]
        [8536, 8536, 7970, 7970, 11183, 11183]
        [8354, 8354, 8681, 8681, 11183, 11183]
        [8662, 8662, 8506, 8506, 11183, 11183]
        [8256, 8256, 7927, 7927, 11183, 11183]
        [7483, 7483, 6796, 6796, 11183, 11183]
        [5840, 5840, 5546, 5546, 6001, 6001]
        [6156, 6156, 5838, 5838, 6001, 6001]
        [5128, 5128, 4831, 4831, 6001, 6001]
        [5216, 5216, 5372, 5372, 6001, 6001]
        [5112, 5112, 4419, 4419, 6001, 6001]
        [3861, 3861, 4197, 4197, 6001, 6001]
        [4608, 4608, 4571, 4571, 6001, 6001]
        [4166, 4166, 3712, 3712, 3821, 3821]
        [4039, 4039, 3896, 3896, 3821, 3821]
        [3351, 3351, 2659, 2659, 3821, 3821]
        [2780, 2780, 3310, 3310, 3821, 3821]
        """;

    assertEquals(a, result.toString());
  }
}