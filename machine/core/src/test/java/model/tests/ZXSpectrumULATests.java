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

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import model.connected.*;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import model.interfaces.IZXInterface1;
import model.interfaces.IULA;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class ZXSpectrumULATests {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;
  static private IULA ula;

  @BeforeAll
  public static void beforeall() {
    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler(SpeccyBaseForTests.createSpeccy()));
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    interface1 = new ConnectedInterface1(testDriver);
    microdrive = new ConnectedMicrodrive(testDriver);
    ula = bus.getULA();

    bus.connectComponent(interface1);
    interface1.connectMicrodrive(microdrive);

    cpu = new ConnectedZ80CPU(testDriver);
//    cpu.connectToBus(bus);
    ula.setScreenActive(true);
  }

  private int setupModel(String model, int startTState) {
//    testDriver.setTstates(startTState);
    testDriver.setModel(model);
    cpu.setTStates(startTState);
    return startTState;
  }

  @BeforeEach
  void setUp() {
    interface1.reset();
    testDriver.reset();
    cpu.reset();
//    cpu.setPC(0xA000);
//    testDriver.spectrum.z80.setExecDone(true);
    testDriver.updatePC(0xA000);
  }

  @AfterAll
  static void tearDown() {
  }

  // Beam Position Tests
  @Test
  void testBeamPositionAtT0() {
    setupModel("48K", 0);
    assertEquals(0, ula.getVerticalPosition());
    assertEquals(0, ula.getHorizontalPosition());
  }

  @Test
  void testBeamPositionAfterOneLine() {
    setupModel("48K", 0);
    for (int i = 0; i < 20000; i++) {
      cpu.setTStates(i);
      System.out.println("tstates: " + cpu.getTStates() + " - vpos: " + ula.getVerticalPosition() + " - hpos: " + ula.getHorizontalPosition());
    }

    cpu.setTStates(224);
    assertEquals(1, ula.getVerticalPosition());
    assertEquals(0, ula.getHorizontalPosition());
  }

  @Test
  void testBeamPositionAtDisplayStartT14336() {
    setupModel("48K", 14336);
    assertEquals(64, ula.getVerticalPosition());
    assertEquals(0, ula.getHorizontalPosition());
  }

  @Test
  void testBeamPositionAtContentionStartT14335() {
    setupModel("48K", 14335);
    assertEquals(63, ula.getVerticalPosition());
    assertEquals(223, ula.getHorizontalPosition());
  }

  @Test
  void testBeamPositionAfterMemoryReadT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.readMemory(0x4000); // Adds 3 + 6 = 9 t-states
    assertEquals(64, ula.getVerticalPosition());
    assertEquals(0, ula.getHorizontalPosition()); // 14335 + 9 = 14344, 14344 % 224 = 8, but adjust if contention affects
  }

  // Border State Tests
  @Test
  void testBorderChange() {
    setupModel("48K", 0);
    cpu.out(0xFE, (byte) 0x02); // Border red (bits 0-2 = 2)
    assertEquals(2, ula.getBorderColor());
  }

  @Test
  void testBorderChangeDuringInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.out(0xFE, (byte) 0x03); // Border magenta
    assertEquals(3, ula.getBorderColor());
    assertEquals(initialTStates + 4 + 6, cpu.getTStates()); // OUT with contention example
  }

  // Audio (Beeper) Tests
  @Test
  void testBeeperToggle() {
    setupModel("48K", 0);
    cpu.out(0xFE, (byte) 0x10); // Beeper on (bit 4)
    assertEquals(1, ula.getBeeperState());
    cpu.out(0xFE, (byte) 0x00); // Beeper off
    assertEquals(0, ula.getBeeperState());
  }

  @Test
  void testBeeperWithLDInstruction() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setRegisterA((byte) 0x10);
    cpu.executeInstruction("LD A,0x10", null); // Dummy, but then OUT
    cpu.out(0xFE, cpu.getRegisterA());
    assertEquals(1, ula.getBeeperState());
  }

  // Keyboard Tests (Assuming setKeyboardRow method exists)
  @Test
  void testKeyboardReadSingleKey() {
    setupModel("48K", 0);
    ula.setKeyboardRow((byte) 0xFE, (byte) 0x01); // Press SHIFT (bit 0)
    byte keys = cpu.in(0xFEFE);
    assertEquals((byte) 0x1E, keys); // 0b11110
  }

  @Test
  void testKeyboardReadMultipleKeys() {
    setupModel("48K", 0);
    ula.setKeyboardRow((byte) 0xFE, (byte) 0x03); // Press SHIFT and Z (bits 0 and 1)
    byte keys = cpu.in(0xFEFE);
    assertEquals((byte) 0x1C, keys); // 0b11100
  }

  @Test
  void testKeyboardDuringINCInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    ula.setKeyboardRow((byte) 0xFE, (byte) 0x01);
    cpu.setHL(0x4000);
    cpu.executeInstruction("INC (HL)", null); // Execute with contention
    byte keys = cpu.in(0xFEFE);
    assertEquals((byte) 0x1E, keys);
  }

  // Interrupt Tests
  @Test
  void testInterruptAtFrameEnd() {
    setupModel("48K", 69887);
    assertFalse(ula.isInterruptActive());
    cpu.setTStates(1);
    assertTrue(ula.isInterruptActive());
  }

  @Test
  void testNoInterruptMidFrame() {
    setupModel("48K", 14335);
    assertFalse(ula.isInterruptActive());
  }

  @Test
  void testInterruptDuration() {
    setupModel("48K", 69888);
    assertTrue(ula.isInterruptActive());
    cpu.setTStates(32);
    assertFalse(ula.isInterruptActive()); // Assuming active for 32 t-states
  }

  // Combined Tests with Instructions
  @Test
  void testBorderAndBeamWithCALLInstructionT14335() {
    setupModel("48K", 14335);
    cpu.setPC(25000);
    cpu.setSP(0x6000);
    cpu.out(0xFE, (byte) 0x04); // Set border green before CALL
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(4, ula.getBorderColor());
    // Assert beam advanced based on t-states added (41 from previous calculation)
    assertEquals(64, ula.getVerticalPosition()); // Approximate, adjust based on total t
  }

  @Test
  void testBeeperAndKeyboardWithLDIInstructionT14361() {
    setupModel("128K", 14361);
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    cpu.out(0xFE, (byte) 0x10); // Beeper on
    ula.setKeyboardRow((byte) 0xFE, (byte) 0x01);
    cpu.executeInstruction("LDI", null);
    assertEquals(1, ula.getBeeperState());
    assertEquals((byte) 0x1E, cpu.in(0xFEFE));
  }
}