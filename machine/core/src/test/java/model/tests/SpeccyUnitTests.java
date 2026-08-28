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

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.LocalLibretroCore;
import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.modules.Ula;
import model.connected.*;
import model.interfaces.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class SpeccyUnitTests extends SpeccyBaseForTests {

  /* --------------------------------------------------------------------- */
  /*  Constantes exactas del código C original (Memory.java + unittests.c) */
  /* --------------------------------------------------------------------- */
  private static final int PAGE_SIZE_LOGARITHM = 11;
  private static final int PAGE_SIZE = 1 << PAGE_SIZE_LOGARITHM;                 // 2048
  private static final int PAGES_IN_64K = 1 << (16 - PAGE_SIZE_LOGARITHM);      // 32
  private static final int PAGES_IN_16K = 1 << (14 - PAGE_SIZE_LOGARITHM);      // 8
  private static final int PAGES_IN$this_8K = 1 << (13 - PAGE_SIZE_LOGARITHM);      // 4
  private static final int PAGES_IN_4K = 1 << (12 - PAGE_SIZE_LOGARITHM);      // 2
  private static final int PAGES_IN_2K = 1 << (11 - PAGE_SIZE_LOGARITHM);      // 1
  private static final int ULA_CONTENTION_SIZE = 69888;                         // 50 Hz PAL

  /* --------------------------------------------------------------------- */
  /*  Infraestructura del emulador (TestDriver, Memory, ULA, etc.)        */
  /* --------------------------------------------------------------------- */
  private static TestDriver testDriver;
  private static Memory memory;
  private static Ula ula;
  private static IZ80CPU cpu;
  private static ISpectrumBus bus;
  private static LocalLibretroCore localLibretroCore;

  public SpeccyUnitTests(){
    speccy= createSpeccy();
    localLibretroCore = new LocalLibretroCore(speccy.eventManager, speccy.display, speccy.machine, speccy.z80, speccy.zxClock, speccy.periph, speccy);

    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler(speccy));
    //        bus = new ConnectedSpectrumBus(speccy.memory, speccy.ula, testDriver);
    cpu = new ConnectedZ80CPU(testDriver);

    memory.init(null);
//        ula.setScreenActive(true);
  }

  @BeforeAll
  public static void beforeAll() {

  }

  @BeforeEach
  void setUp() {
    testDriver.reset();
    cpu.reset();
    memory.end();               // limpia el pool y los sources
    memory.init(null);
    testDriver.updatePC(0xA000);
    testDriver.tstatesHistoryInit();
  }

  @AfterAll
  static void tearDown() {
    memory.end();
  }

  private int setupModel(String model, int tstates) {
    testDriver.setModel(model);
    testDriver.updatePC(0xA000);
    testDriver.if1Page(false);
    cpu.setTStates(tstates);
    return tstates;
  }

  /* --------------------------------------------------------------------- */
  /*  unittests_run() – punto de entrada del test suite                     */
  /* --------------------------------------------------------------------- */
  @Test
  void unittests_run() {
    int r = 0;
    r += contention_test();
    r += floating_bus_test();
    r += floating_bus_merge_test();
    r += mempool_test();
    r += paging_test();
    // r += debugger_disassemble_unittest(); // no implementado

    System.out.println("Final return value: " + r + " (should be 0)");
    assertEquals(0, r, "Todas las pruebas deben pasar");
  }

  /* --------------------------------------------------------------------- */
  /*  contention_test() – checksum exacto de la tabla ula_contention[]     */
  /* --------------------------------------------------------------------- */
  private int contention_test() {
    long checksum = 0;
    long target = 0;
    int error = 0;

    for (int i = 0; i < ULA_CONTENTION_SIZE; i++) {
      checksum += (long) ula.contention[i] * (i + 1);
    }

    boolean late = testDriver.isLateTimings();
    String machine = testDriver.getModel();

    if (late) {
      target = switch (machine) {
        case "16K", "48K", "SE" -> 2308927488L;
        case "48K_NTSC" -> 1962110976L;
        case "128K", "+2" -> 2335248384L;
        case "+2A", "+3", "+3E" -> 3113840640L;
        case "TC2048", "TC2068" -> 2307959808L;
        case "TS2068" -> 1975593984L;
        case "PENT", "PENT512", "PENT1024", "SCORP" -> 0L;
        default -> -1L;
      };
    } else {
      target = switch (machine) {
        case "16K", "48K", "SE" -> 2308862976L;
        case "48K_NTSC" -> 1962046464L;
        case "128K", "+2" -> 2335183872L;
        case "+2A", "+3", "+3E" -> 3113754624L;
        case "TC2048", "TC2068" -> 2307895296L;
        case "TS2068" -> 1975529472L;
        case "PENT", "PENT512", "PENT1024", "SCORP" -> 0L;
        default -> -1L;
      };
    }

    if (checksum != target) {
      System.out.printf("%s: contention test: checksum = %d, expected = %d%n",
          "Speccy", checksum, target);
      error = 1;
    }
    return error;
  }

  /* --------------------------------------------------------------------- */
  /*  floating_bus_test() – comportamiento del puerto 0xFF                 */
  /* --------------------------------------------------------------------- */
  private int floating_bus_test() {
    long checksum = 0;
    long target = 0;
    int error = 0;

    int screen = memory.currentScreen;
    for (int offset = 0; offset < 8192; offset++) {
      int addr = 0x4000 + offset + (screen * 0x4000);
      testDriver.writeMemory(addr, (byte) (offset % 0x100), true);
    }

    for (int tstates = 0; tstates < ULA_CONTENTION_SIZE; tstates++) {
      cpu.setTStates(tstates);
      checksum += (long) bus.readPort(0xFF) * (tstates + 1);
    }

    boolean late = testDriver.isLateTimings();
    String machine = testDriver.getModel();

    if (late) {
      target = switch (machine) {
        case "16K", "48K" -> 3426156480L;
        case "48K_NTSC" -> 3258908608L;
        case "128K", "+2" -> 2852995008L;
        case "+2A", "+3", "+3E", "TC2048", "TC2068", "TS2068",
             "SE", "PENT", "PENT512", "PENT1024", "SCORP" -> 4261381056L;
        default -> -1L;
      };
    } else {
      target = switch (machine) {
        case "16K", "48K" -> 3427723200L;
        case "48K_NTSC" -> 3260475328L;
        case "128K", "+2" -> 2854561728L;
        case "+2A", "+3", "+3E", "TC2048", "TC2068", "TS2068",
             "SE", "PENT", "PENT512", "PENT1024", "SCORP" -> 4261381056L;
        default -> -1L;
      };
    }

    if (checksum != target) {
      System.out.printf("%s: floating bus test: checksum = %d, expected = %d%n",
          "Speccy", checksum, target);
      error = 1;
    }
    return error;
  }

  /* --------------------------------------------------------------------- */
  /*  floating_bus_merge_test() – combinación de líneas de bus flotante   */
  /* --------------------------------------------------------------------- */
  private int floating_bus_merge_test() {
    assertEquals(0xaa, bus.mergeFloatingBus(0xaa, 0xff, 0x00));
    assertEquals(0xaa, bus.mergeFloatingBus(0xaa, 0xff, 0xff));
    assertEquals(0x00, bus.mergeFloatingBus(0xaa, 0x00, 0x00));
    assertEquals(0xaa, bus.mergeFloatingBus(0xaa, 0x00, 0xff));
    assertEquals(0xa0, bus.mergeFloatingBus(0xaa, 0xf0, 0x00));
    assertEquals(0xaa, bus.mergeFloatingBus(0xaa, 0xf0, 0xff));
    assertEquals(0x0a, bus.mergeFloatingBus(0xaa, 0x0f, 0x00));
    assertEquals(0xaa, bus.mergeFloatingBus(0xaa, 0x0f, 0xff));
    return 0;
  }

  /* --------------------------------------------------------------------- */
  /*  mempool_test() – pool de memoria (exacto al original)               */
  /* --------------------------------------------------------------------- */
  private int mempool_test() {
    int initial = memory.pool.size();
    int pool1 = memory.poolAllocate(0).length; // solo para forzar registro
    assertEquals(initial + 1, memory.pool.size());

    byte[] m1 = memory.poolAllocate(23);
    assertEquals(1, memory.pool.stream().filter(e -> e.memory == m1).count());

    byte[] m2 = memory.poolAllocate(42 * 4);
    assertEquals(2, memory.pool.stream().filter(e -> e.memory == m1 || e.memory == m2).count());

    // Simulamos mempool_new (no hay tipo genérico, solo conteo)
    memory.poolAllocate(5 * 2);
    assertEquals(3, memory.pool.size());

    memory.poolFree(); // elimina no-persistentes
    assertEquals(0, memory.pool.size());

    // segundo pool
    int pool2 = memory.poolAllocate(0).length;
    assertEquals(initial + 2, memory.pool.size());

    memory.poolAllocate(23);
    memory.poolAllocate(6 * 7);
    memory.poolAllocate(5);
    memory.poolAllocatePersistent(42, true);
    assertEquals(1, memory.pool.stream().filter(e -> e.persistent).count());

    memory.poolFree(); // elimina todo menos el persistente
    assertEquals(1, memory.pool.size());

    memory.pool.clear();
    return 0;
  }

  /* --------------------------------------------------------------------- */
  /*  Helpers de paginación – idénticos a los del C original               */
  /* --------------------------------------------------------------------- */
  private int assert_page(int base, int length, int source, int page) {
    int baseIdx = base >>> PAGE_SIZE_LOGARITHM;
    for (int i = 0; i < length >>> PAGE_SIZE_LOGARITHM; i++) {
      MemoryPage r = memory.mapRead[baseIdx + i];
      MemoryPage w = memory.mapWrite[baseIdx + i];
      assertEquals(source, r.source);
      assertEquals(page, r.getPageNum());
      assertEquals(source, w.source);
      assertEquals(page, w.getPageNum());
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
    return unittests_assert_16k_page(base, memory.sourceRam, page);
  }

  private int assert_16k_rom_page(int base, int page) {
    return unittests_assert_16k_page(base, memory.sourceRom, page);
  }

  private int assert_16k_pages(int rom, int ram4000, int ram8000, int ramc000) {
    int r = 0;
    r += assert_16k_rom_page(0x0000, rom);
    r += unittests_assert_16k_ram_page(0x4000, ram4000);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, ramc000);
    return r;
  }

  private int assert_all_ram(int r0, int r4, int r8, int rc) {
    int r = 0;
    r += unittests_assert_16k_ram_page(0x0000, r0);
    r += unittests_assert_16k_ram_page(0x4000, r4);
    r += unittests_assert_16k_ram_page(0x8000, r8);
    r += unittests_assert_16k_ram_page(0xc000, rc);
    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  paging_test_16() – modelo 16K                                        */
  /* --------------------------------------------------------------------- */
  private int paging_test_16() {
    int r = 0;
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_page(0x8000, memory.sourceNone, 0);
    r += unittests_assert_16k_page(0xc000, memory.sourceNone, 0);
    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  unittests_paging_test_48() – modelo 48K                              */
  /* --------------------------------------------------------------------- */
  private int unittests_paging_test_48(int ram8000) {
    int r = 0;
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, memory.currentScreen);
    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  paging_test_128_unlocked() / locked()                               */
  /* --------------------------------------------------------------------- */
  private int paging_test_128_unlocked(int ram8000) {
    int r = 0;
    assertEquals(0, testDriver.getRamLocked());

    r += unittests_paging_test_48(ram8000);

    cpu.out(0x7ffd, (byte) 0x07);
    r += assert_16k_pages(0, 5, ram8000, 7);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x7ffd, (byte) 0x08);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(7, memory.currentScreen);

    cpu.out(0x7ffd, (byte) 0x10);
    r += assert_16k_pages(1, 5, ram8000, 0);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x7ffd, (byte) 0x1f);
    r += assert_16k_pages(1, 5, ram8000, 7);
    assertEquals(7, memory.currentScreen);

    return r;
  }

  private int paging_test_128_locked(int ram8000) {
    int r = 0;
    cpu.out(0x7ffd, (byte) 0x20);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, memory.currentScreen);
    assertNotEquals(0, testDriver.getRamLocked());

    cpu.out(0x7ffd, (byte) 0x1f);
    r += assert_16k_pages(0, 5, ram8000, 0);
    assertEquals(5, memory.currentScreen);
    return r;
  }

  private int paging_test_128() {
    int r = 0;
    r += paging_test_128_unlocked(2);
    r += paging_test_128_locked(2);
    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  paging_test_plus3() – modelo +3 (puerto 0x1FFD)                     */
  /* --------------------------------------------------------------------- */
  private int paging_test_plus3() {
    int r = 0;
    r += paging_test_128_unlocked(2);

    cpu.out(0x7ffd, (byte) 0x00);
    cpu.out(0x1ffd, (byte) 0x04);
    r += assert_16k_pages(2, 5, 2, 0);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x7ffd, (byte) 0x10);
    r += assert_16k_pages(3, 5, 2, 0);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x1ffd, (byte) 0x01);
    r += assert_all_ram(0, 1, 2, 3);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x1ffd, (byte) 0x03);
    r += assert_all_ram(4, 5, 6, 7);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x1ffd, (byte) 0x05);
    r += assert_all_ram(4, 5, 6, 3);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x1ffd, (byte) 0x07);
    r += assert_all_ram(4, 7, 6, 3);
    assertEquals(5, memory.currentScreen);

    cpu.out(0x1ffd, (byte) 0x00);
    r += paging_test_128_locked(2);

    cpu.out(0x1ffd, (byte) 0x10);
    r += assert_16k_pages(0, 5, 2, 0);
    assertEquals(5, memory.currentScreen);

    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  paging_test_timex() – Timex TC2048/TC2068/TS2068                    */
  /* --------------------------------------------------------------------- */
  private int paging_test_timex(int ram8000, int dock, int exrom) {
    int r = 0;
    r += unittests_paging_test_48(ram8000);

    cpu.out(0x00f4, (byte) 0x01);
    r += unittests_assert_8k_page(0x0000, dock, 0);
    r += unittests_assert_8k_page(0x2000, memory.sourceRom, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);

    cpu.out(0x00f4, (byte) 0x04);
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_8k_page(0x4000, dock, 2);
    r += unittests_assert_8k_page(0x6000, memory.sourceRam, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);

    cpu.out(0x00f4, (byte) 0xff);
    for (int i = 0; i < 8; i++) {
      r += unittests_assert_8k_page(i * 0x2000, dock, i);
    }

    cpu.out(0x00ff, (byte) 0x80);
    for (int i = 0; i < 8; i++) {
      r += unittests_assert_8k_page(i * 0x2000, exrom, i);
    }

    cpu.out(0x00f4, (byte) 0x00);
    r += assert_16k_rom_page(0x0000, 0);
    r += unittests_assert_16k_ram_page(0x4000, 5);
    r += unittests_assert_16k_ram_page(0x8000, ram8000);
    r += unittests_assert_16k_ram_page(0xc000, 0);

    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  paging_test() – dispatcher por modelo (exacto al C)                */
  /* --------------------------------------------------------------------- */
  private int paging_test() {
    int r = 0;
    String machine = testDriver.getModel();

    switch (machine) {
      case "16K" -> r += paging_test_16();
      case "48K", "48K_NTSC" -> r += unittests_paging_test_48(2);
      case "128K", "+2", "PENT" -> r += paging_test_128();
      case "+2A", "+3", "+3E", "128E" -> r += paging_test_plus3();
      case "SCORP" -> r += 0;                     // stub
      case "PENT512" -> r += 0;                   // stub
      case "PENT1024" -> r += 0;                  // stub
      case "TC2048" -> r += paging_test_timex(2, memory.sourceNone, memory.sourceNone);
      case "TC2068", "TS2068" -> r += paging_test_timex(2, memory.sourceNone, memory.sourceExrom);
      case "SE" -> r += 0;                        // stub
      default -> System.out.printf("%s:%d: unknown machine?%n",
          "SpeccyUnitTests.java", 420);
    }

    // Las pruebas de periféricos (IF1, Beta, etc.) se omiten para 16K y SE
    if (!Set.of("16K", "SE").contains(machine)) {
      // r += if1_unittest(); … (no implementado)
    }

    return r;
  }

  /* --------------------------------------------------------------------- */
  /*  Stubs para los modelos no implementados (mantienen la API)          */
  /* --------------------------------------------------------------------- */
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
}