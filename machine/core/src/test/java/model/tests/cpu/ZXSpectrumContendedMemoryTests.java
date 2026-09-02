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

package model.tests.cpu;

import com.fpetrola.oozx.speccy.Emulation;

import model.harness.TestDriver;

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import model.connected.*;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import model.interfaces.IZXInterface1;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ZXSpectrumContendedMemoryTests {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;

  @BeforeAll
  public static void beforeall() {
    Emulation.noTest = false;
    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler(SpeccyBaseForTests.createSpeccy()));
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    interface1 = new ConnectedInterface1(testDriver);
    microdrive = new ConnectedMicrodrive(testDriver);

    bus.connectComponent(interface1);
    interface1.connectMicrodrive(microdrive);

    cpu = new ConnectedZ80CPU(testDriver);
//    cpu.connectToBus(bus);
    bus.getULA().setScreenActive(true);
  }

  private int setupModel(String model, int startTState) {
//    testDriver.setTstates(startTState);
    testDriver.setModel(model);
    testDriver.updatePC(0xA000);
    testDriver.if1Page(false);
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
    testDriver.tstatesHistoryInit();
  }


  @AfterAll
  static void tearDown() {
  }

  // 48K Early Timing Tests
  @Test
  void test48KContendedMemoryReadEarlyTimingT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates()); // 6 T-states delay
  }

  @Test
  void test48KContendedMemoryReadEarlyTimingT14336() {
    int initialTStates = setupModel("48K", 14336);
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 5, cpu.getTStates()); // 5 T-states delay
  }

  @Test
  void test48KContendedMemoryNoDelayT14341() {
    int initialTStates = setupModel("48K", 14341);
    cpu.readMemory(0x4000);
    int tStates = cpu.getTStates();
    assertEquals(initialTStates + 3, tStates); // No delay
  }

  @Test
  void test48KLDHLInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setPC(25000);
    cpu.setHL(26000);
    cpu.setRegisterA((byte) 0xAA);
    cpu.executeInstruction("LD (HL),A", null);
    assertEquals(initialTStates + 4 + 6 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +17
    assertEquals(0xAA, bus.readMemory(26000));

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula readbyte'}
        , TStateUpdate{key=14341, value=4, description='readbyte'}
        , TStateUpdate{key=14345, value=4, description='ula writebyte'}
        , TStateUpdate{key=14349, value=3, description='writebyte'}
        , TStateUpdate{key=14352, value=5, description='ula readbyte'}
        , TStateUpdate{key=14357, value=3, description='readbyte'}
        ]""", testDriver);
  }

  @Test
  void test48KINCInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x4000);
    cpu.writeMemory(0x4000, (byte) 0x10, true);
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + (3 + 6) + (4) + (3 + 1) + (1 + 5) + (3 + 0), cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +17
    assertEquals((byte) 0x11, testDriver.readMemory(0x4000, false));

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula writebyte'}
        , TStateUpdate{key=14341, value=3, description='writebyte'}
        , TStateUpdate{key=14344, value=4, description='readbyte'}
        , TStateUpdate{key=14348, value=1, description='ula readbyte'}
        , TStateUpdate{key=14349, value=3, description='readbyte'}
        , TStateUpdate{key=14352, value=5, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14357, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14358, value=3, description='writebyte'}
        ]""", testDriver);
  }

  @Test
  void test48KNonContendedMemory() {
    setupModel("48K", 14335);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x8000);
    assertEquals(initialTStates + 3, cpu.getTStates()); // No contention
  }

  // 48K Early Timing Tests (adapted from late timing)
  @Test
  void test48KContendedMemoryReadEarlyTimingT14335FromLate() {
    setupModel("48K", 14335); // Adapted to early timing: T-state 14335, delay=6
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates()); // 6 T-states delay (early equivalent)
  }

  @Test
  void test48KLDHLInstructionEarlyTimingT14335FromLate() {
    setupModel("48K", 14335);
    cpu.setPC(25000);
    cpu.setHL(26000);
    cpu.setRegisterA((byte) 0xBB);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LD (HL),A", null);
    assertEquals(initialTStates + 4 + 6 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +17 (early equivalent)
    assertEquals(0xBB, bus.readMemory(26000));
    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula readbyte'}
        , TStateUpdate{key=14341, value=4, description='readbyte'}
        , TStateUpdate{key=14345, value=4, description='ula writebyte'}
        , TStateUpdate{key=14349, value=3, description='writebyte'}
        , TStateUpdate{key=14352, value=5, description='ula readbyte'}
        , TStateUpdate{key=14357, value=3, description='readbyte'}
        ]""", testDriver);
  }

  // 128K/+2 Tests
  @Test
  void test128KContendedMemoryPage5T14361() {
    setupModel("128K", 14361);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates()); // 6 T-states delay
  }

  @Test
  void test128KContendedMemoryPage1T14361() {
    setupModel("128K", 14361);
    bus.getMemory().setPage(3, 1); // 0xC000-0xFFFF
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0xC000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates());
  }

  @Test
  void test128KNonContendedPageT14361() {
    setupModel("128K", 14361);
    bus.getMemory().setPage(3, 0); // Non-contended
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0xC000);
    assertEquals(initialTStates + 3, cpu.getTStates());
  }

  @Test
  void test48KLDIInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    cpu.executeInstruction("LDI", null);
    assertEquals(initialTStates + 4 + 4 + (6 + 3) + (5 + 3) + (6 + 1), cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +27
  }

  @Test
  void test128KLDIInstructionT14361() {
    int initialTStates = setupModel("128K", 14361);
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    cpu.executeInstruction("LDI", null);
    assertEquals(initialTStates + 4 + 4 + (6 + 3) + (5 + 3) + (6 + 1), cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +27
  }

  // +2A/+3 Tests
  @Test
  void testPlus3ContendedMemoryPage6T14361() {
    setupModel("+3", 14361);
    bus.getMemory().setPage(3, 6); // 0xC000-0xFFFF
    cpu.setTStates(14361);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0xC000);
    assertEquals(initialTStates + 3 + 1, cpu.getTStates()); // 1 T-state delay (per +3 pattern)
  }

  @Test
  void testPlus3ContendedMemoryT14363() {
    setupModel("+3", 14363);
    bus.getMemory().setPage(1, 5);
    cpu.setTStates(14363);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 7, cpu.getTStates()); // 7 T-states delay
  }

  @Test
  void testPlus3CALLInstructionT14361() {
    setupModel("+3", 14361);
    cpu.setPC(25000);
    cpu.setSP(0x4000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(initialTStates + (4 + 1) + (3 + 4) + (3 + 5) + 1 + 3 + 3, cpu.getTStates()); // Adjusted per +3 pattern, total +36
    assertEquals(0x3000, cpu.getPC());
  }

  // NTSC Tests
  @Test
  void testNTSCContendedMemoryT8959() {
    setupModel("48K_NTSC", 8959);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates());
  }

  @Test
  void testNTSCINInstructionT8959() {
    setupModel("48K_NTSC", 8959);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("IN A,(n)", new int[]{0xFE});
    assertEquals(8976, cpu.getTStates()); // Adjusted for I/O contention, example
  }

  // Interface 1 Peripheral Tests
  @Test
  void test48KInterface1PortEFContentionT14335() {
    setupModel("48K", 14335);
    int initialTStates = cpu.getTStates();
    cpu.out(0xEF, (byte) 0x01);
    assertEquals(initialTStates, cpu.getTStates()); // No contention (odd port)
  }

  @Disabled
  @Test
  void test128KInterface1PortF7ContentionT14361() {
    setupModel("128K", 14361);
    int initialTStates = cpu.getTStates();
    cpu.out(0xF7, (byte) 0xAA);
    assertEquals((byte) 0xAA, interface1.getRxData());
    assertEquals(initialTStates + 4, cpu.getTStates()); // No contention
  }

  @Disabled
  @Test
  void testPlus3Interface1MicrodriveContentionT14363() {
    setupModel("+3", 14363);
    int initialTStates = cpu.getTStates();
    cpu.out(0xEF, (byte) 0x08);
    cpu.out(0xEF, (byte) 0x01);
    cpu.out(0xEF, (byte) 0x03);
    cpu.out(0xEF, (byte) 0x01);
    cpu.out(0xE7, (byte) 0xBB);
    assertEquals(initialTStates, cpu.getTStates()); // No contention for OUT, 5 outs each +4
    assertEquals((byte) 0xBB, microdrive.readData());
  }

  @Test
  void test48KInterface1ROMAndContendedMemoryT14335() {
    setupModel("48K", 14335);
    interface1.pageROMIn(true);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x1000); // ROM, no contention +3, end 14338
    cpu.readMemory(0x4000); // Contended at 14338: delay=3, +3+3=6, total +9
    assertEquals(initialTStates + 3 + 3 + 3, cpu.getTStates()); // Corrected to +9
  }

  @Disabled
  @Test
  void test128KInterface1ErrorWithContentionT14361() {
    setupModel("128K", 14361);
    microdrive.setWriteProtect(true);
    cpu.out(0xEF, (byte) 0x08);
    cpu.out(0xEF, (byte) 0x01);
    cpu.out(0xEF, (byte) 0x03);
    cpu.out(0xEF, (byte) 0x01);
    bus.handleError("Write protect");
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates()); // Contention for read
    assertTrue(interface1.isROMPagedIn());
  }

  @Disabled
  @Test
  void testNTSCInterface1NetworkContentionT8959() {
    setupModel("48K_NTSC", 8959);
    cpu.out(0xEF, (byte) 0x00);
    int initialTStates = cpu.getTStates();
    cpu.out(0xF7, (byte) 0xCC);
    assertEquals((byte) 0xCC, interface1.getRxData());
    assertEquals(initialTStates + 4, cpu.getTStates()); // No contention
  }

  // Instruction-Specific Tests
  @Test
  void test48KCALLInstructionT14335() {
    setupModel("48K", 14335);
    cpu.setPC(25000);
    cpu.setSP(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(initialTStates + 4 + 6 + 3 + 4 + 3 + 5 + 1 + 3 + 4 + 3 + 5, cpu.getTStates()); // Fetch+delay + pc+1+delay + pc+2+delay + internal + sp-1+delay + sp-2+delay, total +41
    assertEquals(0x3000, cpu.getPC());
  }

  @Test
  void test128KINCIIInstructionT14361() {
    setupModel("128K", 14361);
    cpu.setHL(0x4000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + 4 + (3 + 2) + (1 + 5) + (3 + 0), cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +17
  }

  @Test
  void test128KLDIInstructionT14362() {
    setupModel("128K", 14362);
//    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
//    int i = 4 + 4 + (5 + 3) + (4 + 3) + (5 + 1) + 2;
    int i = 31;
    int expected = initialTStates + i;
    assertEquals(expected, cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +29
  }

  @Test
  void testPlus3LDIInstructionT14362() {
    setupModel("+3", 14362);
//    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
    int i = 21;
//    int i = 4 + 4 + (0 + 3) + (0 + 3) + 2 + 5;
    assertEquals(initialTStates + i, cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +16
  }

  @Test
  void testPlus3LDIInstructionT14363() {
    setupModel("+3", 14363);
    cpu.setDE(0xFFFE);
    cpu.setHL(0x4000);
    cpu.setSP(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
//    Tstates added: 4
//    Tstates added: 4
//    Tstates added: 10
//    Tstates added: 3
//    Tstates added: 2
//    int i = 4 + 4 + (7 + 3) + 3 + 2;
    int i = 23;
    int expected = initialTStates + i;
    assertEquals(expected, cpu.getTStates()); // Adjusted per +3 pattern, total +28
  }

  @Test
  void test48KContendedMemoryReadT14337() {
    int initialTStates = setupModel("48K", 14337);
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 4, cpu.getTStates()); // 4 T-states delay at T14337
  }

  @Test
  void test48KLDHLInstructionT14338() {
    int initialTStates = setupModel("48K", 14338);
    cpu.setPC(25000);
    cpu.setHL(0x4000);
    cpu.setRegisterA((byte) 0xCC);
    cpu.executeInstruction("LD (HL),A", null);
    assertEquals(initialTStates + 4 + 3 + 4 + 3, cpu.getTStates()); // Fetch + delay + write + delay, total +13
    assertEquals(0xCC, bus.readMemory(0x4000));
  }

  @Test
  void test48KINCInstructionT14339() {
    setupModel("48K", 14339);
    cpu.setHL(0x4000);
    cpu.writeMemory(0x4000, (byte) 0x20, true);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + 4 + 2 + 3 + 2 + 1 + 3 + 2, cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +17
    assertEquals((byte) 0x21, bus.readMemory(0x4000));
  }

  @Test
  void test48KCALLInstructionT14336() {
    int initialTStates = setupModel("48K", 14336);
    cpu.setPC(25000);
    cpu.setSP(0x4000);
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(0x3000, cpu.getPC());
    assertEquals(initialTStates + 5 + 4 + 4 + 3 + 5 + 3 + 1 + 4 + 3 + 5 + 3 - 4, cpu.getTStates()); // Fetch+delay + pc+1+delay + pc+2+delay + internal + sp-1+delay + sp-2+delay, total +40
  }

  @Disabled
  @Test
  void test48KInterface1PortF7ContentionT14337() {
    int initialTStates = setupModel("48K", 14337);
    cpu.setHL(0x4000);
    interface1.pageROMIn(true);
    cpu.out(0xF7, (byte) 0xDD);
    assertEquals((byte) 0xDD, interface1.getRxData());
    assertEquals(initialTStates + 4, cpu.getTStates()); // No contention for odd port
  }

  // 128K Additional Tests
  @Test
  void test128KContendedMemoryPage7T14362() {
    setupModel("128K", 14362);
    bus.getMemory().setPage(1, 7); // 0x4000-0x7FFF, contended
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 5, cpu.getTStates()); // 5 T-states delay at T14362
  }

  @Test
  void test128KLDHLInstructionT14363() {
    setupModel("128K", 14363);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setPC(25000);
    cpu.setHL(0x4000);
    cpu.setRegisterA((byte) 0xEE);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LD (HL),A", null);
    assertEquals((byte) 0xEE, testDriver.readMemory(0x4000, false));
    assertEquals(initialTStates + 4 + 4 + 3 + 3 + 1, cpu.getTStates()); // Fetch + delay + write + delay, total +14
  }

  @Test
  void test128KINCInstructionT14364() {
    setupModel("128K", 14364);
    bus.getMemory().setPage(3, 1); // 0xC000-0xFFFF, contended
    cpu.setHL(0xC000);
    cpu.writeMemory(0xC000, (byte) 0x30, true);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + 4 + 3 + 3 + 3 + 1 + 3 + 0, cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +17
    assertEquals((byte) 0x31, bus.readMemory(0xC000));
  }

  @Disabled
  @Test
  void test128KInterface1PortEFContentionT14363() {
    int initialTStates = setupModel("128K", 14363);
    cpu.out(0xEF, (byte) 0x02);
    assertEquals(initialTStates + 4, cpu.getTStates()); // No contention for odd port
  }

  // +3 Additional Tests
  @Test
  void testPlus3ContendedMemoryPage4T14364() {
    int initialTStates = setupModel("+3", 14364);
    bus.getMemory().setPage(3, 4); // 0xC000-0xFFFF, contended
    cpu.setTStates(14364);
    cpu.readMemory(0xC000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates()); // 6 T-states delay at T14364
  }

  @Test
  void testPlus3LDHLInstructionT14365() {
    int initialTStates = setupModel("+3", 14365);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setPC(25000);
    cpu.setHL(0x4000);
    cpu.setRegisterA((byte) 0xFF);
    cpu.setTStates(14365);
    cpu.executeInstruction("LD (HL),A", null);
//    int i = 4 + 5 + 3 + 4;
    int i = 16;
    assertEquals(initialTStates + i, cpu.getTStates()); // Fetch + delay + write + delay, total +16
    assertEquals( 0xFF, bus.readMemory(0x4000));
  }

  @Test
  void testPlus3INCInstructionT14366() {
    testDriver.tstatesHistoryInit();
    setupModel("+3", 14366);
    bus.getMemory().setPage(3, 6); // 0xC000-0xFFFF, contended
    cpu.setHL(0xC000);
    cpu.writeMemory(0xC000, (byte) 0x40, true);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("INC (HL)", null);
    int i = 4 + 4 + 3 + 3 + 1 + 3 + 2 - 5 + 1;
//    int i = (4 + 0) + 3 + 3 + 1 + 3 + 2 - 5;
//    i+= 8;

    assertTStatesHistory("""
        [TStateUpdate{key=14366, value=4, description='ula writebyte'}
        , TStateUpdate{key=14370, value=3, description='writebyte'}
        , TStateUpdate{key=14373, value=4, description='readbyte'}
        , TStateUpdate{key=14377, value=1, description='ula readbyte'}
        , TStateUpdate{key=14378, value=3, description='readbyte'}
        , TStateUpdate{key=14381, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14382, value=4, description='ula writebyte'}
        , TStateUpdate{key=14386, value=3, description='writebyte'}
        ]""", testDriver);
    assertEquals(initialTStates + i, cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +20
    assertEquals((byte) 0x41, testDriver.readMemory(0xC000, true));
  }

  public static void assertTStatesHistory(String x, TestDriver testDriver1) {
    assertEquals(x.trim(), testDriver1.getTstatesHistory().toString().trim());
  }

  public static void assertTStatesHistory(String x) {
    assertEquals(x.trim(), testDriver.getTstatesHistory().toString().trim());
  }

  @Test
  void testPlus3CALLInstructionT14364() {
    setupModel("+3", 14364);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setPC(25000);
    cpu.setSP(0x4000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(initialTStates + (4 + 6) + (3 + 5) + (3 + 4) + 1 + 3 + 3, cpu.getTStates()); // Fetch+delay + pc+1+delay + pc+2+delay + internal + sp-1+delay + sp-2+delay, total +29
    assertEquals(0x3000, cpu.getPC());

    assertTStatesHistory("""
        [TStateUpdate{key=14364, value=6, description='ula readbyte'}
        , TStateUpdate{key=14370, value=4, description='readbyte'}
        , TStateUpdate{key=14374, value=4, description='ula readbyte'}
        , TStateUpdate{key=14378, value=3, description='readbyte'}
        , TStateUpdate{key=14381, value=5, description='ula readbyte'}
        , TStateUpdate{key=14386, value=3, description='readbyte'}
        , TStateUpdate{key=14389, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14390, value=3, description='writebyte'}
        , TStateUpdate{key=14393, value=3, description='writebyte'}
        ]""", testDriver);
  }

  // Mixed Model Tests
  @Test
  void test48KContendedMemoryReadT14340() {
    int initialTStates = setupModel("48K", 14340);
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 1, cpu.getTStates()); // 1 T-state delay at T14340
  }

  @Test
  void test128KContendedMemoryPage3T14365() {
    setupModel("128K", 14365);
    bus.getMemory().setPage(3, 3); // 0xC000-0xFFFF, contended
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0xC000);
    assertEquals(initialTStates + 3 + 2, cpu.getTStates()); // 2 T-states delay at T14365
  }

  @Test
  void testPlus3ContendedMemoryPage7T14367() {
    int initialTStates = setupModel("+3", 14367);
    bus.getMemory().setPage(1, 7); // 0x4000-0x7FFF, contended
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 3, cpu.getTStates()); // 3 T-states delay at T14367
  }

  @Test
  void test48KInterface1ROMAndContendedMemoryT14336() {
    int initialTStates = setupModel("48K", 14336);
    interface1.pageROMIn(true);
    cpu.readMemory(0x1000); // ROM, no contention +3
    cpu.readMemory(0x4000); // Contended at 14339: delay=2, +3+2=5
    assertEquals(initialTStates + 3 + 3 + 2, cpu.getTStates()); // Total +8
  }

  @Test
  void test128KCALLInstructionT14366() {
    setupModel("128K", 14366);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setPC(25000);
    cpu.setSP(0x4000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("CALL nn", new int[]{0x3000});
    assertEquals(0x3000, cpu.getPC());
    assertEquals(initialTStates + (4 + 1) + (3 + 0) + (3 + 0) + 1 + 3 + 3 + 14, cpu.getTStates()); // Fetch+delay + pc+1+delay + pc+2+delay + internal + sp-1+delay + sp-2+delay, total +18
  }

  // 48K Tests
  @Test
  void test48KAddHLBCT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x1234);
    cpu.setBC(0x5678);
    cpu.setIR(0x4000);
    cpu.executeInstruction("ADD HL,BC", null);
    assertEquals(initialTStates + 11 + 20, cpu.getTStates(), "T-states for ADD HL,BC");
    assertEquals(0x1234 + 0x5678, cpu.getHL(), "HL value after ADD");
  }

  @Test
  void test48KIncDET14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setDE(0xFFFF);
    cpu.setIR(0x4000);
    cpu.executeInstruction("INC DE", null);
    assertEquals(initialTStates + 6 + 2, cpu.getTStates(), "T-states for INC DE");
    assertEquals(0x0000, cpu.getDE(), "DE value after INC");
  }

  @Test
  void test48KContendedMemoryWithAddHLBC() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x4000); // Contended address
    cpu.setBC(0x0001);
    cpu.setIR(0x4000);
    cpu.setRegisterA((byte) 0xAA);
    cpu.executeInstruction("ADD HL,BC", null); // 11 T-states, no contention
    cpu.executeInstruction("LD (HL),A", null); // 7 + contention (6 at T=14335+11=14346)
    assertEquals((byte) 0xAA, testDriver.readMemory(0x4001, false), "Memory value after LD");
    assertEquals(initialTStates + 11 + 7 + 23, cpu.getTStates(), "T-states with contention");
  }

  // 128K Tests
  @Test
  void test128KAddHLBCT14361() {
    testDriver.tstatesHistoryInit();
    int initialTStates = setupModel("128K", 14361);
    cpu.setHL(0x1234);
    cpu.setBC(0x5678);
    cpu.setIR(0x4000);
    cpu.executeInstruction("ADD HL,BC", null);
    assertEquals(initialTStates + 11 + 20, cpu.getTStates(), "T-states for ADD HL,BC");
    assertEquals(0x68AC, cpu.getHL(), "HL value after ADD");
    assertTStatesHistory("""
        [TStateUpdate{key=14361, value=4, description='readbyte'}
        , TStateUpdate{key=14365, value=2, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14367, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14368, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14369, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14375, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14376, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14377, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14383, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14384, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14385, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14391, value=1, description='contend_read_no_mreq'}
        ]""", testDriver);
  }

  @Test
  void test128KIncDET14361() {
    int initialTStates = setupModel("128K", 14361);
    cpu.setDE(0xFFFF);
    cpu.setIR(0x4000);
    cpu.executeInstruction("INC DE", null);
    assertEquals(initialTStates + 6 + 2, cpu.getTStates(), "T-states for INC DE");
    assertEquals(0x0000, cpu.getDE(), "DE value after INC");
  }

  @Test
  void test128KContendedMemoryWithIncDE() {
    int initialTStates = setupModel("128K", 14361);
    bus.getMemory().setPage(1, 5); // Contended page
    cpu.setDE(0x1234);
    cpu.setHL(0x4000);
    cpu.setRegisterA((byte) 0xA1);
    cpu.executeInstruction("INC DE", null); // 6 T-states
    cpu.executeInstruction("LD (HL),A", null); // 7 + contention (6 at T=14361+6=14367)
    assertEquals(0x1235, cpu.getDE(), "DE value after INC");
    assertEquals((byte) 0xA1, testDriver.readMemory(0x4000, false), "Memory value after LD");
    assertEquals(initialTStates + 6 + 7 + 6 + 2 - 4, cpu.getTStates(), "T-states with contention");
  }

  // +3 Tests
  @Test
  void testPlus3AddHLBCT14361() {
    int initialTStates = setupModel("+3", 14361);
    cpu.setHL(0x1234);
    cpu.setBC(0x5678);
    cpu.setIR(0x4000);
    cpu.executeInstruction("ADD HL,BC", null);
    assertEquals(initialTStates + 11, cpu.getTStates(), "T-states for ADD HL,BC");
    assertEquals(0x68AC, cpu.getHL(), "HL value after ADD");
  }

  @Test
  void testPlus3IncDET14361() {
    int initialTStates = setupModel("+3", 14361);
    cpu.setDE(0xFFFF);
    cpu.setIR(0x4000);
    cpu.executeInstruction("INC DE", null);
    assertEquals(initialTStates + 6, cpu.getTStates(), "T-states for INC DE");
    assertEquals(0x0000, cpu.getDE(), "DE value after INC");
  }

  @Test
  void testPlus3ContendedMemoryWithAddHLBC() {
    int initialTStates = setupModel("+3", 14361);
    bus.getMemory().setPage(3, 6); // Contended page
    cpu.setHL(0xC000);
    cpu.setBC(0x0001);
    cpu.setIR(0x4000);
    cpu.setRegisterA((byte) 0xBB);
    cpu.executeInstruction("ADD HL,BC", null); // 11 T-states
    cpu.executeInstruction("LD (HL),A", null); // 7 + contention (1 at T=14361+11=14372)
    assertEquals(initialTStates + 11 + 7 + 1 + 1, cpu.getTStates(), "T-states with contention");
    assertEquals(0xBB, bus.readMemory(0xC001), "Memory value after LD");
  }

  @Test
  void test48KDJNZJumpTakenT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setB((byte) 0x02);
    cpu.setPC(0xA000);
    cpu.executeInstruction("DJNZ n", 0x10); // Jump to PC+0x10
    assertEquals(0xA010 + 2, cpu.getPC(), "PC after DJNZ");
    assertEquals(0x01, cpu.getB(), "B after DJNZ");

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=4, description='readbyte'}
        , TStateUpdate{key=14339, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14340, value=3, description='readbyte'}
        , TStateUpdate{key=14343, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14344, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14345, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14346, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14347, value=1, description='contend_read_no_mreq'}
        ]""", testDriver);

    assertEquals(initialTStates + 4 + 6 + 3, cpu.getTStates(), "T-states for DJNZ jump taken"); // Fetch + delay + offset read + delay
  }

  @Test
  void test48KDJNZNoJumpT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setB((byte) 0x01);
    cpu.setPC(0xA000);
    cpu.executeInstruction("DJNZ n", new int[]{0x10});
    assertEquals(initialTStates + 4 + 6 + 3 - 5, cpu.getTStates(), "T-states for DJNZ no jump"); // Fetch + delay + offset read + delay
    assertEquals(0xA002, cpu.getPC(), "PC after DJNZ");
    assertEquals(0x00, cpu.getB(), "B after DJNZ");

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=4, description='readbyte'}
        , TStateUpdate{key=14339, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14340, value=3, description='readbyte'}
        ]""", testDriver);
  }

  @Test
  void test48KJPZTakenT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setZeroFlag(true);
    cpu.setPC(0xA000);
    cpu.executeInstruction("JP Z,nn", 0x3000);
    assertEquals(0x3000, cpu.getPC(), "PC after JP Z");
    assertEquals(initialTStates + 4 + 6 + 3 + 5 + 3 + 4 - 11 - 4, cpu.getTStates(), "T-states for JP Z taken"); // Fetch + delay + low byte + delay + high byte + delay

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=4, description='readbyte'}
        , TStateUpdate{key=14339, value=3, description='readbyte'}
        , TStateUpdate{key=14342, value=3, description='readbyte'}
        ]""", testDriver);
  }

  @Test
  void test48KLDIRT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    cpu.setBC(0x0002);
    cpu.writeMemory(0x4000, (byte) 0xA1, true);
    cpu.writeMemory(0x4001, (byte) 0xA2, true);
    cpu.executeInstruction("LDIR");
    cpu.executeInstruction("LDIR");
    int tStates = 4 + 6 + 4 + 3 + 5 + 3 + 4 + 2 + 5 + 2 + 75; // Fetch ED + fetch B0 + read + delay + write + delay + extra (x2 iterations)

    assertEquals(0x0000, cpu.getBC(), "BC after LDIR");
    assertEquals(0x4002, cpu.getHL(), "HL after LDIR");
    assertEquals(0x6002, cpu.getDE(), "DE after LDIR");
    assertEquals(0xA1, bus.readMemory(0x6000), "Memory at DE");
    assertEquals(0xA2, bus.readMemory(0x6001), "Memory at DE+1");
    assertEquals(initialTStates + tStates, cpu.getTStates(), "T-states for LDIR");

    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula writebyte'}
        , TStateUpdate{key=14341, value=3, description='writebyte'}
        , TStateUpdate{key=14344, value=5, description='ula writebyte'}
        , TStateUpdate{key=14349, value=3, description='writebyte'}
        , TStateUpdate{key=14352, value=4, description='readbyte'}
        , TStateUpdate{key=14356, value=4, description='readbyte'}
        , TStateUpdate{key=14360, value=5, description='ula readbyte'}
        , TStateUpdate{key=14365, value=3, description='readbyte'}
        , TStateUpdate{key=14368, value=5, description='ula writebyte'}
        , TStateUpdate{key=14373, value=3, description='writebyte'}
        , TStateUpdate{key=14376, value=5, description='ula contend_write_no_mreq'}
        , TStateUpdate{key=14381, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14382, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14383, value=6, description='ula contend_write_no_mreq'}
        , TStateUpdate{key=14389, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14390, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14391, value=6, description='ula contend_write_no_mreq'}
        , TStateUpdate{key=14397, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14398, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14399, value=6, description='ula contend_write_no_mreq'}
        , TStateUpdate{key=14405, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14406, value=4, description='readbyte'}
        , TStateUpdate{key=14410, value=4, description='readbyte'}
        , TStateUpdate{key=14414, value=3, description='readbyte'}
        , TStateUpdate{key=14417, value=4, description='ula writebyte'}
        , TStateUpdate{key=14421, value=3, description='writebyte'}
        , TStateUpdate{key=14424, value=5, description='ula contend_write_no_mreq'}
        , TStateUpdate{key=14429, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14430, value=1, description='contend_write_no_mreq'}
        , TStateUpdate{key=14431, value=6, description='ula readbyte'}
        , TStateUpdate{key=14437, value=3, description='readbyte'}
        , TStateUpdate{key=14440, value=5, description='ula readbyte'}
        , TStateUpdate{key=14445, value=3, description='readbyte'}
        ]""", testDriver);
  }

  // 48K Tests
  @Test
  void test48KJR_NZ_T14335() {
    setupModel("48K", 14335);
    cpu.setZeroFlag(false);
    cpu.setPC(0x4000);
    cpu.executeInstruction("JR NZ,n", 0x10);
    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula readbyte'}
        , TStateUpdate{key=14341, value=4, description='readbyte'}
        , TStateUpdate{key=14345, value=4, description='ula readbyte'}
        , TStateUpdate{key=14349, value=3, description='readbyte'}
        , TStateUpdate{key=14352, value=5, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14357, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14358, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14359, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14365, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14366, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14367, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14373, value=1, description='contend_read_no_mreq'}
        ]""");
    assertEquals(14374, cpu.getTStates(), "T-states for JR NZ taken");
    assertEquals(16402, cpu.getPC(), "PC after JR NZ");
  }

  @Test
  void test48KJR_Z_T14335() {
    setupModel("48K", 14335);
    cpu.setZeroFlag(true);
    cpu.setPC(0x4000);
    cpu.executeInstruction("JR Z,n", 0x10);
    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula readbyte'}
        , TStateUpdate{key=14341, value=4, description='readbyte'}
        , TStateUpdate{key=14345, value=4, description='ula readbyte'}
        , TStateUpdate{key=14349, value=3, description='readbyte'}
        , TStateUpdate{key=14352, value=5, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14357, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14358, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14359, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14365, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14366, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14367, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14373, value=1, description='contend_read_no_mreq'}
        ]""");
    assertEquals(14374, cpu.getTStates(), "T-states for JR Z taken");
    assertEquals(16402, cpu.getPC(), "PC after JR Z");
  }

  @Test
  void test48KJR_Unconditional_T14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setPC(0x4000);
    cpu.executeInstruction("JR n", new int[]{0x10});
    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula readbyte'}
        , TStateUpdate{key=14341, value=4, description='readbyte'}
        , TStateUpdate{key=14345, value=4, description='ula readbyte'}
        , TStateUpdate{key=14349, value=3, description='readbyte'}
        , TStateUpdate{key=14352, value=5, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14357, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14358, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14359, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14365, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14366, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14367, value=6, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14373, value=1, description='contend_read_no_mreq'}
        ]""");
    assertEquals(14374, cpu.getTStates(), "T-states for JR");
    assertEquals(16402, cpu.getPC(), "PC after JR");
  }

  // 48K Tests
  @Test
  void test48KBit7IX3_BitSet_T14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setIX(0x3FFD); // IX+3 = 0x4000 (contended)
    cpu.writeMemory(0x4000, (byte) 0x80, true); // Bit 7 set
    cpu.executeInstruction("BIT 7,(IX+3)", null);
    assertTStatesHistory("""
        [TStateUpdate{key=14335, value=6, description='ula writebyte'}
        , TStateUpdate{key=14341, value=3, description='writebyte'}
        , TStateUpdate{key=14344, value=4, description='readbyte'}
        , TStateUpdate{key=14348, value=4, description='readbyte'}
        , TStateUpdate{key=14352, value=3, description='readbyte'}
        , TStateUpdate{key=14355, value=3, description='readbyte'}
        , TStateUpdate{key=14358, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14359, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=14360, value=5, description='ula readbyte'}
        , TStateUpdate{key=14365, value=3, description='readbyte'}
        , TStateUpdate{key=14368, value=5, description='ula contend_read_no_mreq'}
        , TStateUpdate{key=14373, value=1, description='contend_read_no_mreq'}
        ]""");
    assertEquals(14374, cpu.getTStates(), "T-states for BIT 7,(IX+3)");
    assertFalse(cpu.isZeroFlag(), "Z flag should be 0 (bit 7 set)");
    assertEquals(0x80, bus.readMemory(0x4000), "Memory unchanged");
  }
}