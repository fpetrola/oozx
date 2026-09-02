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

import static model.tests.cpu.ZXSpectrumContendedMemoryTests.assertTStatesHistory;
import static org.junit.jupiter.api.Assertions.*;

public class ZXSpectrumInterruptionTests {
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
    bus.getULA().setScreenActive(true);
  }

  private int setupModel(String model, int startTState) {
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
    testDriver.updatePC(0xA000);
    testDriver.tstatesHistoryInit();
  }


  @AfterAll
  static void tearDown() {
  }

  // Test 1: 48K EI with Immediate IM 1 Interrupt
  @Test
  void test48KEI_ImmediateInterrupt_IM1_T69886() {
    testDriver.tstatesHistoryInit();
    int initialTStates = setupModel("48K", 69886); // 2 T-states before interrupt
    cpu.executeInstruction("LD SP,nn", 0x6000); // 10 T-states
    cpu.executeInstruction("IM 1"); // 8 T-states
    cpu.executeInstruction("EI"); // 4 T-states
    cpu.executeInstruction("NOP"); // 4 T-states, interrupt after
//    assertTrue(cpu.getInterruptEnable(), "Interrupts enabled");
    assertEquals(0x0038, cpu.getPC(), "PC jumps to IM 1 ISR");
    assertEquals(0x5FFE, cpu.getSP(), "SP after stack push");
    assertEquals(0xA0, bus.readMemory(0x5FFF), "High byte of return address");
    assertEquals(0x07, bus.readMemory(0x5FFE), "Low byte of return address");
    assertTStatesHistory("""
        [TStateUpdate{key=69886, value=4, description='readbyte'}
        , TStateUpdate{key=69890, value=3, description='readbyte'}
        , TStateUpdate{key=69893, value=3, description='readbyte'}
        , TStateUpdate{key=8, value=4, description='readbyte'}
        , TStateUpdate{key=12, value=4, description='readbyte'}
        , TStateUpdate{key=16, value=4, description='readbyte'}
        , TStateUpdate{key=20, value=4, description='readbyte'}
        , TStateUpdate{key=24, value=7, description='interrupt'}
        , TStateUpdate{key=31, value=3, description='writebyte'}
        , TStateUpdate{key=34, value=3, description='writebyte'}
        , TStateUpdate{key=37, value=3, description='readbyte'}
        , TStateUpdate{key=40, value=3, description='readbyte'}
        ]""", testDriver);
    assertEquals(43, cpu.getTStates(), "T-states for IM 1 interrupt");
  }

  // Test 2: 48K DI Preventing Interrupt
  @Test
  void test48KDI_NoInterrupt_T14335() {
    int initialTStates = setupModel("48K", 69886);
    cpu.executeInstruction("LD SP,nn", 0x4000); // 10 T-states + contention
    cpu.executeInstruction("IM 1"); // 8 T-states + contention
    cpu.executeInstruction("DI"); // 4 T-states + contention
    cpu.executeInstruction("NOP"); // 4 T-states, interrupt after
//    assertFalse(cpu.getInterruptEnable(), "Interrupts disabled");
    assertEquals(0xA007, cpu.getPC(), "PC after instructions");
    assertEquals(0x4000, cpu.getSP(), "SP unchanged");
    assertTStatesHistory("""
        [TStateUpdate{key=69886, value=4, description='readbyte'}
        , TStateUpdate{key=69890, value=3, description='readbyte'}
        , TStateUpdate{key=69893, value=3, description='readbyte'}
        , TStateUpdate{key=8, value=4, description='readbyte'}
        , TStateUpdate{key=12, value=4, description='readbyte'}
        , TStateUpdate{key=16, value=4, description='readbyte'}
        , TStateUpdate{key=20, value=4, description='readbyte'}
        ]""", testDriver);
    assertEquals(24, cpu.getTStates(), "T-states for DI");
  }

  // Test 3: 48K IM 2 with Custom ISR
  @Test
  void test48KIM2_CustomISR_T69836() {
    int initialTStates = setupModel("48K", 69836);
    cpu.executeInstruction("LD A,n", 0x80); // 7 T-states
    cpu.executeInstruction("LD I,A"); // 9 T-states
    cpu.executeInstruction("LD SP,nn", 0x6000); // 10 T-states
    cpu.executeInstruction("IM 2"); // 8 T-states
    cpu.executeInstruction("LD A,n", 0x40); // 7 T-states
    cpu.executeInstruction("LD (nn),A", 0x80FF); // 13 T-states
    cpu.executeInstruction("LD A,n", 0x20); // 7 T-states
    cpu.executeInstruction("LD (nn),A", 0x8100); // 13 T-states
    cpu.executeInstruction("EI"); // 4 T-states
    cpu.executeInstruction("NOP"); // 4 T-states
//    assertTrue(cpu.getInterruptEnable(), "Interrupts enabled");

    assertTStatesHistory("""
        [TStateUpdate{key=69836, value=4, description='readbyte'}
        , TStateUpdate{key=69840, value=3, description='readbyte'}
        , TStateUpdate{key=69843, value=4, description='readbyte'}
        , TStateUpdate{key=69847, value=4, description='readbyte'}
        , TStateUpdate{key=69851, value=1, description='contend_read_no_mreq'}
        , TStateUpdate{key=69852, value=4, description='readbyte'}
        , TStateUpdate{key=69856, value=3, description='readbyte'}
        , TStateUpdate{key=69859, value=3, description='readbyte'}
        , TStateUpdate{key=69862, value=4, description='readbyte'}
        , TStateUpdate{key=69866, value=4, description='readbyte'}
        , TStateUpdate{key=69870, value=4, description='readbyte'}
        , TStateUpdate{key=69874, value=3, description='readbyte'}
        , TStateUpdate{key=69877, value=4, description='readbyte'}
        , TStateUpdate{key=69881, value=3, description='readbyte'}
        , TStateUpdate{key=69884, value=3, description='readbyte'}
        , TStateUpdate{key=69887, value=3, description='writebyte'}
        , TStateUpdate{key=2, value=4, description='readbyte'}
        , TStateUpdate{key=6, value=3, description='readbyte'}
        , TStateUpdate{key=9, value=4, description='readbyte'}
        , TStateUpdate{key=13, value=3, description='readbyte'}
        , TStateUpdate{key=16, value=3, description='readbyte'}
        , TStateUpdate{key=19, value=3, description='writebyte'}
        , TStateUpdate{key=22, value=4, description='readbyte'}
        , TStateUpdate{key=26, value=4, description='readbyte'}
        , TStateUpdate{key=30, value=7, description='interrupt'}
        , TStateUpdate{key=37, value=3, description='writebyte'}
        , TStateUpdate{key=40, value=3, description='writebyte'}
        , TStateUpdate{key=43, value=3, description='readbyte'}
        , TStateUpdate{key=46, value=3, description='readbyte'}
        ]""", testDriver);
    assertEquals(0x2040, cpu.getPC(), "PC jumps to ISR");
    assertEquals(0x5FFE, cpu.getSP(), "SP after stack push");
    assertEquals(0xA0, bus.readMemory(0x5FFF), "High byte of return address");
    assertEquals(0x15, bus.readMemory(0x5FFE), "Low byte of return address");
    assertEquals(55, cpu.getTStates(), "T-states for IM 2 interrupt");
  }
}