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

package model.tests.video;


import model.harness.TestDriver;

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
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
    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler(SpeccyBaseForTests.createSpeccy()));
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    ula = bus.getULA();

    cpu = new ConnectedZ80CPU(testDriver);
    ula.setScreenActive(true);
  }

  private int setupModel(String model, int startTState) {
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

    // Held up by contention, the beam goes on: nine T-states is two positions and a bit, and
    // which it lands on depends on where in the four the count already was, not on the nine.
    cpu.addTStates2(9);
    assertEquals(24, ula.getVerticalPosition(), "vpos after contention");

    // Four T-states is one position, wherever it is standing. This is what a delay does to it.
    for (int delay = 1; delay <= 7; delay++) {
      setupModel(getModel(), 14335); // Reset
      int before = ula.getHorizontalPosition();
      cpu.addTStates2(4);
      assertEquals(before + 1, ula.getHorizontalPosition(), "four T-states is one position");
      cpu.addTStates2(delay);
      assertEquals(24, ula.getVerticalPosition(), "vpos with delay " + delay);
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
    }
  }
}