package model.tests;

import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.fuse.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZXSpectrumContendedMemoryTests2 {
  static private TestDriver testDriver1;
  static private TestDriver testDriver2;
  private static LocalLibretroCore localLibretroCore;
  private static LibretroCore remoteCore;

  @BeforeAll
  public static void beforeall() {
    localLibretroCore = new LocalLibretroCore();
    remoteCore = FuseLibretroConnector.core;
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
  }


  @AfterAll
  static void tearDown() {
  }

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
      for (int j = 0; j < 100; j++) {
        testDriver1.step();
        testDriver2.step();

//        int r1 = testDriver1.getRegister("R") & 0x7F;
//        int r2 = testDriver2.getRegister("R") & 0x7F;
//        assertEquals(r1, r2, "R differs at step " + i + ", substep " + j);


        int pc1 = testDriver1.getRegister("PC");
        int pc2 = testDriver2.getRegister("PC");
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

    // Iterate through lists in chunks
    for (int i = 0; i < expected.size(); i += chunkSize) {
      int endIndex = Math.min(i + chunkSize, expected.size());
      List<T> expectedChunk = expected.subList(i, endIndex);
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

  public static <T> void assertListsEqualInChunks2(List<T> expected, List<T> actual, int chunkSize) {
//    // Check if lists have the same size
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

      // Use assertEquals with a descriptive message
      assertEquals(
          expectedChunk,
          actualChunk,
          String.format("Difference found in chunk starting at index %d to %d", i, endIndex - 1)
      );
    }
  }

}