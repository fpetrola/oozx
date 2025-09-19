/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import com.fpetrola.oozx.fuse.CommandHandler;
import model.connected.*;
import model.interfaces.IULA;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZXSpectrumBeamBatchTests {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IULA ula;

  @BeforeAll
  public static void beforeall() {
    testDriver = new TestDriver(new CommandHandler());
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    ula = bus.getULA();

    cpu = new ConnectedZ80CPU(testDriver);
    ula.setScreenActive(true);
  }

  private int setupModel(String model, int startTState) {
    bus.setModel(model);
//    cpu.setModel(model);
    testDriver.setModel(model);
    cpu.setTStates(startTState);
    return startTState;
  }

  @BeforeEach
  void setUp() {
    testDriver.reset();
    cpu.reset();
    testDriver.updatePC(0xA000);
  }

  @AfterAll
  static void tearDown() {
    testDriver.setFinished(true);
  }

  // Test for pre-frame negative positions (punctual cases)
  @Test
  void testPreFrameNegativePositions() {
    setupModel(getModel(), 0); // Reset to base
    int baseT = 8941;
    for (int offset = 0; offset < 3; offset++) {
      int t = baseT + offset;
      cpu.addTStates2(t - cpu.getTStates()); // Set to specific tstate
      assertEquals(-1, ula.getVerticalPosition(), "Failed vpos at t=" + t);
      assertEquals(-1, ula.getHorizontalPosition(), "Failed hpos at t=" + t);
    }
  }

  private String getModel() {
    return "48K";
  }

  // Test for start of frame (punctual)
  @Test
  void testStartOfFramePosition() {
    setupModel(getModel(), 8944);
    assertEquals(0, ula.getVerticalPosition());
    assertEquals(0, ula.getHorizontalPosition());
  }

  // Dynamic test for hpos increment within a line using calculations
  @Test
  void testHposIncrementInLine() {
    setupModel(getModel(), 0);
    int baseT = 8944; // Start of line 0
    int lineLength = 224;
    int hposSteps = lineLength / 4; // 56 steps (0-55)
    for (int h = 0; h < hposSteps; h++) {
      for (int sub = 0; sub < 4; sub++) { // Each hpos lasts 4 tstates
        int t = baseT + h * 4 + sub;
        cpu.addTStates2(t - cpu.getTStates());
        assertEquals(0, ula.getVerticalPosition(), "Failed vpos at t=" + t);
        assertEquals(h, ula.getHorizontalPosition(), "Failed hpos at t=" + t);
      }
    }
  }

  // Dynamic test for multiple line transitions
  @Test
  void testMultipleLineTransitions() {
    setupModel(getModel(), 0);
    int baseT = 8944; // Start of frame
    int linesToTest = 10; // Test transitions for first 10 lines
    int lineLength = 224;
    for (int v = 0; v < linesToTest; v++) {
      // Check start of line
      int tStart = baseT + v * lineLength;
      cpu.addTStates2(tStart - cpu.getTStates());
      assertEquals(v, ula.getVerticalPosition(), "Failed start vpos at line " + v);
      assertEquals(0, ula.getHorizontalPosition(), "Failed start hpos at line " + v);

      // Check end of line
      int tEnd = tStart + lineLength - 1;
      cpu.addTStates2(tEnd - cpu.getTStates());
      assertEquals(v, ula.getVerticalPosition(), "Failed end vpos at line " + v);
      assertEquals(55, ula.getHorizontalPosition(), "Failed end hpos at line " + v);
    }
  }

  // Test for end-of-frame wrap-around
  @Test
  void testEndOfFrameWrapAround() {
    setupModel(getModel(), 0);
    int frameLength = 69888;
    int endT = frameLength - 1; // End of frame
    cpu.addTStates2(endT);
    assertEquals(272, ula.getVerticalPosition(), "Failed vpos at end of frame");
    assertEquals(0, ula.getHorizontalPosition(), "Failed hpos at end of frame");

    // Next tstate wraps to new frame
    cpu.addTStates2(224);
    assertEquals(273, ula.getVerticalPosition(), "Failed wrap vpos");
    assertEquals(0, ula.getHorizontalPosition(), "Failed wrap hpos");
  }

  // Test beam advance with simulated contention (adding extra tstates)
  @Test
  void testBeamAdvanceWithContention() {
    setupModel(getModel(), 14335); // Known contention start
    assertEquals(24, ula.getVerticalPosition(), "Initial vpos at 14335");
    assertEquals(3, ula.getHorizontalPosition(), "Initial hpos at 14335");

    // Simulate reading contended memory: add 9 tstates (3 base + 6 contention)
    int extraT = 9;
    cpu.addTStates2(extraT);
    int expectedH = extraT / 4; // 9 / 4 = 2
    assertEquals(24, ula.getVerticalPosition(), "vpos after contention");
//    assertEquals(expectedH, ula.getHorizontalPosition(), "hpos after contention");

    // Test variable contention delays
    int[] delays = {1, 2, 3, 4, 5, 6, 7}; // Common contention patterns
    for (int delay : delays) {
      setupModel(getModel(), 14335); // Reset
      cpu.addTStates2(3 + delay); // Base read + variable delay
      int expH = (3 + delay) / 4;
      int expV = (3 + delay >= 1) ? 24 : 23; // Cross line if enough tstates
      assertEquals(expV, ula.getVerticalPosition(), "vpos with delay " + delay);
//      assertEquals(expH, ula.getHorizontalPosition(), "hpos with delay " + delay);
    }
  }

  // Comprehensive test covering a range of tstates with same hpos or vpos
  @Test
  void testRangesWithSameHposOrVpos() {
    setupModel(getModel(), 0);
    int baseT = 8944;
    int lineLength = 224;

    // Test multiple tstates with same hpos (each hpos spans 4 tstates)
    for (int h = 0; h < 56; h += 10) { // Sample every 10th hpos
      int tBase = baseT + h * 4;
      for (int sub = 0; sub < 4; sub++) {
        int t = tBase + sub;
        cpu.addTStates2(t - cpu.getTStates());
        assertEquals(0, ula.getVerticalPosition(), "vpos same hpos at t=" + t);
        assertEquals(h, ula.getHorizontalPosition(), "same hpos at t=" + t);
      }
    }

    // Test multiple tstates with same vpos (across lines)
    for (int v = 0; v < 312; v += 50) { // Sample every 50th line
      int tLineStart = baseT + v * lineLength;
      cpu.addTStates2(tLineStart - cpu.getTStates());
      assertEquals(v, ula.getVerticalPosition(), "same vpos start at line " + v);
      assertEquals(0, ula.getHorizontalPosition());

      // Mid-line for same vpos
      int tMid = tLineStart + 100; // Arbitrary mid
      cpu.addTStates2(tMid - cpu.getTStates());
      assertEquals(v, ula.getVerticalPosition(), "same vpos mid at line " + v);
//      int expHMid = 100 / 4;
//      assertEquals(expHMid, ula.getHorizontalPosition());
    }
  }
}