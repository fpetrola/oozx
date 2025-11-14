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

import com.fpetrola.oozx.fuse.bridge.DefaultCommandHandler;
import model.connected.*;
import model.interfaces.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class ZXPlus3ROMBankSwitchingTests {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;

  @BeforeAll
  public static void beforeAll() {
    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler());
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    interface1 = new ConnectedInterface1(testDriver);
    microdrive = new ConnectedMicrodrive(testDriver);

    bus.connectComponent(interface1);
    interface1.connectMicrodrive(microdrive);

    cpu = new ConnectedZ80CPU(testDriver);
    bus.getULA().setScreenActive(true);
  }

  private int setupPlus3(int startTState) {
    testDriver.setModel("+3");
    testDriver.updatePC(0xA000);
    testDriver.if1Page(false);
    cpu.setTStates(startTState);
    testDriver.tstatesHistoryInit();
    return startTState;
  }

  @BeforeEach
  void setUp() {
    interface1.reset();
    testDriver.reset();
    cpu.reset();
    testDriver.updatePC(0xA000);
  }

  @AfterAll
  static void tearDown() {}

  // === 1. ROM PAGING BASICO (PORT 0x1FFD) ===
  @Test
  void testPlus3ROMBankSwitch_Port1FFD_Bank0() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x00); // ROM 0 (Editor/48K)
    assertEquals(243, bus.readMemory(0x0000), "First byte of ROM 0 (LD A,n)");
    assertEquals(0, bus.getMemory().getROMBank(), "ROM bank should be 0");
  }

  @Test
  void testPlus3ROMBankSwitch_Port1FFD_Bank1() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // ROM 1 (+3DOS)
    assertEquals(0, bus.readMemory(0x0000), "First byte of +3DOS ROM (JP)");
    assertEquals(1, bus.getMemory().getROMBank(), "ROM bank should be 1");
  }

  @Test
  void testPlus3ROMBankSwitch_Port1FFD_Bank2() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x02); // ROM 2 (Syntax checker)
    assertEquals(243, bus.readMemory(0x0000), "First byte of +3DOS ROM (JP)");
    assertEquals(2, bus.getMemory().getROMBank(), "ROM bank should be 2");
  }

  @Test
  void testPlus3ROMBankSwitch_Port1FFD_Bank3() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x03); // ROM 3 (48K BASIC)
    assertEquals( 0, bus.readMemory(0x0000), "ROM 3 starts with DI");
    assertEquals(3, bus.getMemory().getROMBank(), "ROM bank should be 3");
  }

  @Test
  void testPlus3ROMBankSwitch_Port1FFD_InvalidBank() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x04); // Invalid (bits 0-1 only)
    assertEquals(0, bus.getMemory().getROMBank(), "Invalid bank should default to 0");
  }

  // === 2. INTERACCION CON RAM PAGING (PORT 0x7FFD) ===
  @Test
  void testPlus3ROMAndRAMPage_ToggleROMWithRAMLocked() {
    setupPlus3(14361);
    cpu.out(0x7FFD, (byte) 0x10); // Lock RAM paging
    cpu.out(0x1FFD, (byte) 0x01); // Try to switch to ROM 1
    assertEquals(0, bus.getMemory().getROMBank(), "ROM paging blocked when RAM paging locked");
  }

  @Test
  void testPlus3ROMAndRAMPage_RAMUnlocked_ROMChangeAllowed() {
    setupPlus3(14361);
    cpu.out(0x7FFD, (byte) 0x00); // Unlock RAM paging
    cpu.out(0x1FFD, (byte) 0x01); // Switch to ROM 1
    assertEquals(1, bus.getMemory().getROMBank(), "ROM change allowed when RAM unlocked");
  }

  // === 3. CONTENCION EN LECTURA DE ROM ===
  @Test
  void testPlus3ROMReadContentionT14361_ROM0() {
    int initial = setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x00); // ROM 0
    cpu.readMemory(0x0000); // ROM read
    assertEquals(initial + 4, cpu.getTStates(), "ROM read has no contention (4 T-states)");
  }

  @Test
  void testPlus3ROMReadContentionT14361_ROM1() {
    int initial = setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // ROM 1
    cpu.readMemory(0x0000);
    assertEquals(initial + 4, cpu.getTStates(), "ROM read no contention regardless of bank");
  }

  // === 4. EJECUCION DE CODIGO DESDE ROM ===
  @Test
  void testPlus3ExecuteFromROM0_RST38() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x00); // ROM 0
    cpu.setPC(0x0038); // RST 38H in ROM 0
    cpu.executeInstruction("RST 38H", null);
    assertTrue(cpu.getPC() >= 0x1000 && cpu.getPC() < 0x4000, "RST 38H should jump into ROM 0 handler");
  }

  @Test
  void testPlus3ExecuteFromROM1_DOSCall() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // ROM 1 (+3DOS)
    cpu.setPC(0x0008); // RST 8 in +3DOS
    cpu.executeInstruction("RST 08H", null);
    assertTrue(cpu.getPC() >= 0x4000, "DOS call should jump to RAM routine");
  }

  // === 5. CAMBIO DE ROM DURANTE EJECUCION ===
  @Test
  void testPlus3ROMBankSwitchDuringExecution() {
    setupPlus3(14361);
    cpu.setPC(0xA000);
    cpu.writeMemory(0xA000, (byte) 0x3E, true); // LD A,n
    cpu.writeMemory(0xA001, (byte) 0x01, true);
    cpu.writeMemory(0xA002, (byte) 0xD3, true); // OUT (n),A
    cpu.writeMemory(0xA003, (byte) 0xFD, true); // Port 0x1FFD
    cpu.writeMemory(0xA004, (byte) 0x01, true); // Bank 1
    cpu.writeMemory(0xA005, (byte) 0x76, true); // HALT

    cpu.executeInstruction("LD A,1", null);
    cpu.executeInstruction("OUT (0x1FFD),A", null);
    assertEquals(1, bus.getMemory().getROMBank(), "ROM bank changed during execution");
  }

  // === 6. INTERFACE 1 ROM PAGING (PORT 0x00EF) ===
  @Test
  void testPlus3Interface1ROMPageIn_WithROM0() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x00); // ROM 0
    interface1.pageROMIn(true);
    assertTrue(interface1.isROMPagedIn(), "Interface 1 ROM paged in");
    assertEquals((byte) 0xF3, bus.readMemory(0x0000), "Interface 1 ROM starts with DI");
  }

  @Test
  void testPlus3Interface1ROMPageIn_WithROM1() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // ROM 1
    interface1.pageROMIn(true);
    assertTrue(interface1.isROMPagedIn(), "Interface 1 ROM still pages in over +3DOS");
    assertEquals((byte) 0xF3, bus.readMemory(0x0000), "Interface 1 ROM overrides +3 ROM");
  }

  @Test
  void testPlus3Interface1ROMPageOut_RestoresPlus3ROM() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // ROM 1
    interface1.pageROMIn(true);
    interface1.pageROMIn(false);
    assertEquals((byte) 0xC3, bus.readMemory(0x0000), "+3DOS ROM restored after Interface 1 paged out");
  }

  // === 7. CONTENCION EN ESCRITURA A PUERTO 0x1FFD ===
  @Test
  void testPlus3Port1FFDWriteContentionT14361() {
    int initial = setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01);
    assertEquals(initial + 4, cpu.getTStates(), "Port write to 0x1FFD has no contention (4 T-states)");
  }

  @Test
  void testPlus3Port1FFDWriteContentionT14363() {
    int initial = setupPlus3(14363);
    cpu.out(0x1FFD, (byte) 0x02);
    assertEquals(initial + 4, cpu.getTStates(), "Port 0x1FFD write no contention even in contended cycle");
  }

  // === 8. RESET Y ESTADO INICIAL ===
  @Test
  void testPlus3Reset_DefaultROMBank0() {
    setupPlus3(14361);
    cpu.reset();
    assertEquals(0, bus.getMemory().getROMBank(), "After reset, ROM bank should be 0 (Editor)");
    assertEquals((byte) 0x3E, bus.readMemory(0x0000), "ROM 0 loaded on reset");
  }

  // === 9. SECUENCIA DE CAMBIO DE BANCO + LECTURA INMEDIATA ===
  @Test
  void testPlus3ROMBankSwitchThenImmediateRead() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01); // Switch to ROM 1
    int t1 = cpu.getTStates();
    int b = bus.readMemory(0x0000); // Read immediately
    int t2 = cpu.getTStates();
    assertEquals((byte) 0xC3, b, "ROM 1 byte read correctly");
    assertEquals(t1 + 4, t2, "No extra contention on ROM read after bank switch");
  }

  // === 10. HISTORIA DE T-STATES EN CAMBIO DE BANCO ===
  @Test
  void testPlus3ROMBankSwitchTStatesHistory() {
    setupPlus3(14361);
    cpu.out(0x1FFD, (byte) 0x01);

    assertTStatesHistory("""
        [TStateUpdate{key=14361, value=4, description='writebyte'}
        ]""", testDriver);
  }

  @Test
  void testPlus3ROMBankSwitchWithInterface1PagedIn() {
    setupPlus3(14361);
    interface1.pageROMIn(true);
    cpu.out(0x1FFD, (byte) 0x01); // Try to change +3 ROM
    assertTrue(interface1.isROMPagedIn(), "Interface 1 ROM remains paged in");
    assertEquals((byte) 0xF3, bus.readMemory(0x0000), "Interface 1 ROM not affected by 0x1FFD");
  }

  // === UTILIDADES ===
  public static void assertTStatesHistory(String expected, TestDriver driver) {
    assertEquals(expected.trim(), driver.getTstatesHistory().toString().trim());
  }

  public static void assertTStatesHistory(String expected) {
    assertEquals(expected.trim(), testDriver.getTstatesHistory().toString().trim());
  }
}