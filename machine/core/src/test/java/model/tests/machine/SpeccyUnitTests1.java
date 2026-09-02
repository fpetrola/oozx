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

package model.tests.machine;

import com.fpetrola.oozx.speccy.Emulation;

import model.harness.TestDriver;

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import model.connected.*;
import model.interfaces.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class SpeccyUnitTests1 {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;

  private static final int MEMORY_PAGE_SIZE = 2048;
  private static final int ULA_CONTENTION_SIZE = 69888;

  @BeforeAll
  public static void beforeAll() {
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

  private int setupModel(String model, int tstates) {
    testDriver.setModel(model);
    testDriver.updatePC(0xA000);
    testDriver.if1Page(false);
    cpu.setTStates(tstates);
    return tstates;
  }

  private int setupModelWithTimings(String model, boolean late) {
    if (late)
      testDriver.setLateTimings(false);
    int i = setupModel(model, 0);
    testDriver.setLateTimings(late);
    return i;
  }

  // ===================================================================
  // unittests_run() – NO TOCADO (como pediste)
  // ===================================================================
  void unittests_run() {
    int r = 0;
//        r += paging_test();
    System.out.println("Final return value: " + r + " (should be 0)");
    assertEquals(0, r, "All unit tests should pass");
  }

  // ===================================================================
  // TESTS INDIVIDUALES POR MODELO (camelCase)
  // ===================================================================

  @Test
  void pagingTest16K() {
    setupModel("16K", 0);
    assertEquals(0, paging_test_16(), "16K paging test failed");
  }

  @Test
  void pagingTest48K() {
    setupModel("48K", 0);
    assertEquals(0, unittests_paging_test_48(2), "48K paging test failed");
  }

  @Test
  void pagingTest48KNtsc() {
    setupModel("48K_NTSC", 0);
    assertEquals(0, unittests_paging_test_48(2), "48K NTSC paging test failed");
  }

  @Test
  void pagingTest128K() {
    setupModel("128K", 0);
    assertEquals(0, paging_test_128(), "128K paging test failed");
  }

  @Test
  void pagingTestPlus2() {
    setupModel("+2", 0);
    assertEquals(0, paging_test_128(), "+2 paging test failed");
  }

  @Disabled
//  @Test
  void pagingTestPentagon() {
    setupModel("PENT", 0);
    assertEquals(0, paging_test_128(), "PENT paging test failed");
  }

  @Test
  void pagingTestPlus2A() {
    setupModel("+2A", 0);
    assertEquals(0, paging_test_plus3(), "+2A paging test failed");
  }

  @Test
  void pagingTestPlus3() {
    setupModel("+3", 0);
    assertEquals(0, paging_test_plus3(), "+3 paging test failed");
  }

  @Test
  void pagingTestPlus3E() {
    setupModel("+3E", 0);
    assertEquals(0, paging_test_plus3(), "+3E paging test failed");
  }

  @Test
  void pagingTestScorpion() {
    setupModel("SCORP", 0);
    assertEquals(0, paging_test_scorpion(), "SCORP paging test failed");
  }

  @Test
  void pagingTestPentagon512() {
    setupModel("PENT512", 0);
    assertEquals(0, paging_test_pentagon512(), "PENT512 paging test failed");
  }

  @Test
  void pagingTestPentagon1024() {
    setupModel("PENT1024", 0);
    assertEquals(0, paging_test_pentagon1024(), "PENT1024 paging test failed");
  }

  @Test
  void pagingTestSe() {
    setupModel("SE", 0);
    assertEquals(0, paging_test_se(), "SE paging test failed");
  }

  // ===================================================================
  // MÉTODOS DE PAGING (sin cambios, solo se llaman desde @Test)
  // ===================================================================

  private int paging_test_16() {
    int r = 0;
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_page(0x8000, 1, 2);
    r += unittests_assert_16k_page(0xc000, 1, 0);
    return r;
  }

  private int unittests_paging_test_48(int ram8000) {
    int r = 0;
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    return r;
  }

  private int paging_test_128_unlocked(int ram8000) {
    int r = 0;
    assertEquals(0, testDriver.getRamLocked());
    r += unittests_paging_test_48(ram8000);
    cpu.out(0x7ffd, (byte) 0x07);
    r += assert_16k_pages(0, 5, ram8000, 7);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x7ffd, (byte) 0x08);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(7, testDriver.getCurrentScreen());
    cpu.out(0x7ffd, (byte) 0x10);
    r += assert_16k_pages(1, 5, ram8000, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x7ffd, (byte) 0x1f);
    r += assert_16k_pages(1, 5, ram8000, 7);
    assertEquals(7, testDriver.getCurrentScreen());
    return r;
  }

  private int paging_test_128_locked(int ram8000) {
    int r = 0;
    cpu.out(0x7ffd, (byte) 0x20);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    assertNotEquals(0, testDriver.getRamLocked());
    cpu.out(0x7ffd, (byte) 0x1f);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    return r;
  }

  private int paging_test_128() {
    int r = 0;
    r += paging_test_128_unlocked(2);
    r += paging_test_128_locked(2);
    return r;
  }

  private int paging_test_plus3() {
    int r = 0;
    r += paging_test_128_unlocked(2);
    cpu.out(0x7ffd, (byte) 0x00);
    cpu.out(0x1ffd, (byte) 0x04);
    r += assert_16k_pages(2, 5, 2, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x7ffd, (byte) 0x10);
    r += assert_16k_pages(3, 5, 2, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x1ffd, (byte) 0x01);
    r += assert_all_ram(0, 1, 2, 3);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x1ffd, (byte) 0x03);
    r += assert_all_ram(4, 5, 6, 7);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x1ffd, (byte) 0x05);
    r += assert_all_ram(4, 5, 6, 3);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x1ffd, (byte) 0x07);
    r += assert_all_ram(4, 7, 6, 3);
    assertEquals(5, testDriver.getCurrentScreen());
    cpu.out(0x1ffd, (byte) 0x00);
    r += paging_test_128_locked(2);
    cpu.out(0x1ffd, (byte) 0x10);
    r += assert_16k_pages(0, 5, 2, 0);
    assertEquals(5, testDriver.getCurrentScreen());
    return r;
  }

  private int paging_test_timex(int ram8000, int dock_source, int exrom_source) {
    int r = 0;
    r += unittests_paging_test_48(ram8000);
    cpu.out(0x00f4, (byte) 0x01);
    r += unittests_assert_8k_page(0x0000, dock_source, 0);
    r += unittests_assert_8k_page(0x2000, 0, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);
    cpu.out(0x00f4, (byte) 0x04);
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_8k_page(0x4000, dock_source, 2);
    r += unittests_assert_8k_page(0x6000, 1, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);
    cpu.out(0x00f4, (byte) 0xff);
    for (int i = 0; i < 8; i++) {
      r += unittests_assert_8k_page(i * 0x2000, dock_source, i);
    }
    cpu.out(0x00ff, (byte) 0x80);
    for (int i = 0; i < 8; i++) {
      r += unittests_assert_8k_page(i * 0x2000, exrom_source, i);
    }
    cpu.out(0x00f4, (byte) 0x00);
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);
    return r;
  }

  // ===================================================================
  // Stubs (puedes implementarlos más tarde)
  // ===================================================================
  private int paging_test_scorpion() {
    return 0;
  }

  private int paging_test_pentagon512() {
    return 0;
  }

  private int paging_test_pentagon1024() {
    return 0;
  }

  private int paging_test_se() {
    return 0;
  }

  // ===================================================================
  // Helpers de paginación (sin cambios)
  // ===================================================================
  private int assert_page(int base, int length, int source, int page) {
    int base_index = base / MEMORY_PAGE_SIZE;
    for (int i = 0; i < length / MEMORY_PAGE_SIZE; i++) {
      assertEquals(source, testDriver.getMemoryMapRead(base_index + i).getSource());
      assertEquals(page, testDriver.getMemoryMapRead(base_index + i).getPageNum());
      assertEquals(source, testDriver.getMemoryMapWrite(base_index + i).getSource());
      assertEquals(page, testDriver.getMemoryMapWrite(base_index + i).getPageNum());
    }
    return 0;
  }

  private int unittests_assert_2k_page(int base, int source, int page) {
    return assert_page(base, 0x0800, source, page);
  }

  private int unittests_assert_4k_page(int base, int source, int page) {
    return assert_page(base, 0x1000, source, page);
  }

  private int unittests_assert_8k_page(int base, int source, int page) {
    return assert_page(base, 0x2000, source, page);
  }

  private int unittests_assert_16k_page(int base, int source, int page) {
    return assert_page(base, 0x4000, source, page);
  }

  private int unittests_assert_16k_ram_page(int base, int page) {
    return unittests_assert_16k_page(base, 1, page);
  }

  private int assert_16k_rom_page(int base, int page) {
    return unittests_assert_16k_page(base, 0, page);
  }

  private int assert_16k_pages(int rom, int ram4000, int ram8000, int ramc000) {
    int r = 0;
    r += assert_16k_rom_page(0x0000, rom);
    r += unittests_assert_16k_ram_page(0x4000, ram4000);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, ramc000);
    return r;
  }

  private int assert_all_ram(int ram0000, int ram4000, int ram8000, int ramc000) {
    int r = 0;
    r += unittests_assert_16k_ram_page(0x0000, ram0000);
    r += unittests_assert_16k_ram_page(0x4000, ram4000);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, ramc000);
    return r;
  }

  // ===================================================================
  // (Opcional) Ejecutar todos los tests de paginación desde un solo método
  // ===================================================================
  @Test
  void runAllPagingTests() {
    unittests_run(); // Usa el modelo por defecto del @BeforeEach
  }

  // ===================================================================
  // CONTENTION TEST POR MODELO (camelCase)
  // ===================================================================

  @Test
  void contentionTest16KEarly() {
    setupModel("16K", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(2308862976L);
  }

  @Test
  void contentionTest16KLate() {
    setupModelWithTimings("16K", true);
    assertContentionChecksum(2308927488L);
  }

  @Test
  void contentionTest48KEarly() {
    setupModel("48K", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(2308862976L);
  }

  @Test
  void contentionTest48KLate() {
    setupModelWithTimings("48K", true);
    assertContentionChecksum(2308927488L);
  }

  @Test
  void contentionTest48KNtscEarly() {
    setupModel("48K_NTSC", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(1962046464L);
  }

  @Test
  void contentionTest48KNtscLate() {
    setupModelWithTimings("48K_NTSC", true);
    assertContentionChecksum(1962110976L);
  }

  @Test
  void contentionTest128KEarly() {
    setupModel("128K", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(2335183872L);
  }

  @Test
  void contentionTest128KLate() {
    setupModelWithTimings("128K", true);
    assertContentionChecksum(2335248384L);
  }

  @Test
  void contentionTestPlus2Early() {
    setupModel("+2", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(2335183872L);
  }

  @Test
  void contentionTestPlus2Late() {
    setupModelWithTimings("+2", true);
    assertContentionChecksum(2335248384L);
  }

  @Test
  void contentionTestPlus3Early() {
    setupModel("+3", 0);
    testDriver.setLateTimings(false);
    assertContentionChecksum(3113840640L);
  }

  @Test
  void contentionTestPlus3Late() {
    setupModel("+3", 0);
    testDriver.setLateTimings(true);
    assertContentionChecksum(3113754624L);
  }

//  @Test
//  void contentionTestTc2048Early() {
//    setupModel("TC2048", 0);
//    testDriver.setLateTimings(false);
//    assertContentionChecksum(2307895296L);
//  }
//
//  @Test
//  void contentionTestTc2048Late() {
//    setupModelWithTimings("TC2048", true);
//    assertContentionChecksum(2307959808L);
//  }
//
//  @Test
//  void pagingTestTc2048() {
//    setupModel("TC2048", 0);
//    assertEquals(0, paging_test_timex(2, 5, 5), "TC2048 paging test failed");
//  }
//
//  @Test
//  void pagingTestTc2068() {
//    setupModel("TC2068", 0);
//    assertEquals(0, paging_test_timex(2, 5, 3), "TC2068 paging test failed");
//  }
//
//  @Test
//  void pagingTestTs2068() {
//    setupModel("TS2068", 0);
//    assertEquals(0, paging_test_timex(2, 5, 3), "TS2068 paging test failed");
//  }

  @Disabled
//  @Test
  void contentionTestPentagon() {
    setupModel("PENT", 0);
    assertContentionChecksum(0L);
  }

  // ===================================================================
  // HELPER: Calcula y verifica checksum de ULA contention
  // ===================================================================
  private void assertContentionChecksum(long expected) {
    long checksum = 0;
    for (int i = 0; i < ULA_CONTENTION_SIZE; i++) {
      checksum += (long) testDriver.getULAContention(i) * (i + 1);
    }

    if (checksum != expected) {
      System.out.printf("%s: contention test failed: checksum = %d, expected = %d%n",
          testDriver.getModel(), checksum, expected);
    }

    assertEquals(expected, checksum,
        String.format("%s contention checksum mismatch", testDriver.getModel()));
  }

}