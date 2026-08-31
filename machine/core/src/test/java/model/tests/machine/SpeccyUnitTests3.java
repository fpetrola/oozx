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

import com.fpetrola.oozx.speccy.OOSpectrumConnector;

import model.harness.TestDriver;

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import model.connected.*;
import model.interfaces.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class SpeccyUnitTests3 {
    static private IZ80CPU cpu;
    static private TestDriver testDriver;
    static private ISpectrumBus bus;
    static private IZXInterface1 interface1;
    static private IMicrodrive microdrive;

    private static final int MEMORY_PAGE_SIZE = 2048;
    private static final int ULA_CONTENTION_SIZE = 69888; // 50Hz PAL: 69888 T-states/frame

    @BeforeAll
    public static void beforeAll() {
    OOSpectrumConnector.noTest = false;
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
        setupModel("+3", 0);
        interface1.reset();
        testDriver.reset();
        cpu.reset();
        testDriver.updatePC(0xA000);
        testDriver.tstatesHistoryInit();
    }

    @AfterAll
    static void tearDown() {}

    private int setupModel(String model, int tstates) {
        testDriver.setModel(model);
        testDriver.updatePC(0xA000);
        testDriver.if1Page(false);
        cpu.setTStates(tstates);
        return tstates;
    }

    // ===================================================================
    // unittests_run()
    // ===================================================================
    void unittests_run() {
        int r = 0;
        r += contention_test();
//        r += floating_bus_test();
//        r += floating_bus_merge_test();
//        r += mempool_test();
        r += paging_test();
        // r += debugger_disassemble_unittest(); // Not implemented

        System.out.println("Final return value: " + r + " (should be 0)");
        assertEquals(0, r, "All unit tests should pass");
    }

    @Test
    void unittests48K_run() {
        setupModel("48K", 0);
        unittests_run();
    }

    @Test
    void unittests128K_run() {
        setupModel("48K", 0);
        unittests_run();
    }

    @Test
    void unittestsPlus3_run() {
        setupModel("+3", 0);
        unittests_run();
    }

    // ===================================================================
    // contention_test()
    // ===================================================================
    private int contention_test() {
        long checksum = 0;
        long target = 0;
        int error = 0;

        for (int i = 0; i < ULA_CONTENTION_SIZE; i++) {
            checksum += (long) testDriver.getULAContention(i) * (i + 1);
        }

        boolean late_timings = testDriver.isLateTimings();
        String machine = testDriver.getModel();

        if (late_timings) {
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

    // ===================================================================
    // floating_bus_test()
    // ===================================================================
    private int floating_bus_test() {
        long checksum = 0;
        long target = 0;
        int error = 0;

        int screen = testDriver.getCurrentScreen();
        for (int offset = 0; offset < 8192; offset++) {
            testDriver.writeMemory(0x4000 + offset + (screen * 0x4000), (byte) (offset % 0x100), true);
        }

        for (int tstates = 0; tstates < ULA_CONTENTION_SIZE; tstates++) {
            cpu.setTStates(tstates);
            checksum += (long) bus.readPort(0xFF) * (tstates + 1);
        }

        boolean late_timings = testDriver.isLateTimings();
        String machine = testDriver.getModel();

        if (late_timings) {
            target = switch (machine) {
                case "16K", "48K" -> 3426156480L;
                case "48K_NTSC" -> 3258908608L;
                case "128K", "+2" -> 2852995008L;
                case "+2A", "+3", "+3E", "TC2048", "TC2068", "TS2068", "SE",
                     "PENT", "PENT512", "PENT1024", "SCORP" -> 4261381056L;
                default -> -1L;
            };
        } else {
            target = switch (machine) {
                case "16K", "48K" -> 3427723200L;
                case "48K_NTSC" -> 3260475328L;
                case "128K", "+2" -> 2854561728L;
                case "+2A", "+3", "+3E", "TC2048", "TC2068", "TS2068", "SE",
                     "PENT", "PENT512", "PENT1024", "SCORP" -> 4261381056L;
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

    // ===================================================================
    // floating_bus_merge_test()
    // ===================================================================
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

    // ===================================================================
    // mempool_test()
    // ===================================================================
    private int mempool_test() {
        int initial_pools = testDriver.getMempoolPools();
        int pool1 = testDriver.mempoolRegisterPool();
        assertEquals(initial_pools + 1, testDriver.getMempoolPools());
        assertEquals(0, testDriver.getMempoolPoolSize(pool1));

        testDriver.mempoolMalloc(pool1, 23);
        assertEquals(1, testDriver.getMempoolPoolSize(pool1));

        testDriver.mempoolMallocN(pool1, 42, 4);
        assertEquals(2, testDriver.getMempoolPoolSize(pool1));

        testDriver.mempoolNew(pool1, "word", 5);
        assertEquals(3, testDriver.getMempoolPoolSize(pool1));

        testDriver.mempoolFree(pool1);
        assertEquals(0, testDriver.getMempoolPoolSize(pool1));

        int pool2 = testDriver.mempoolRegisterPool();
        assertEquals(initial_pools + 2, testDriver.getMempoolPools());
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolMalloc(pool1, 23);
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolMallocN(pool1, 6, 7);
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolNew(pool1, "byte", 5);
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolMalloc(pool2, 42);
        assertEquals(1, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolFree(pool2);
        assertEquals(3, testDriver.getMempoolPoolSize(pool1));
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        testDriver.mempoolFree(pool1);
        assertEquals(0, testDriver.getMempoolPoolSize(pool1));
        assertEquals(0, testDriver.getMempoolPoolSize(pool2));

        return 0;
    }

    // ===================================================================
    // assert_page()
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

    private int unittests_assert_2k_page(int base, int source, int page) { return assert_page(base, 0x0800, source, page); }
    private int unittests_assert_4k_page(int base, int source, int page) { return assert_page(base, 0x1000, source, page); }
    private int unittests_assert_8k_page(int base, int source, int page) { return assert_page(base, 0x2000, source, page); }
    private int unittests_assert_16k_page(int base, int source, int page) { return assert_page(base, 0x4000, source, page); }
    private int unittests_assert_16k_ram_page(int base, int page) { return unittests_assert_16k_page(base, 1, page); } // memory_source_ram = 1

    private int assert_16k_rom_page(int base, int page) {
        return unittests_assert_16k_page(base, 0, page); // memory_source_rom = 0
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
    // paging_test_16()
    // ===================================================================
    private int paging_test_16() {
        int r = 0;
        r += assert_16k_rom_page(0x0000, 0);
        r += unittests_assert_16k_ram_page(0x4000, 5);
        r += unittests_assert_16k_page(0x8000, -1, 0); // memory_source_none = -1
        r += unittests_assert_16k_page(0xc000, -1, 0);
        return r;
    }

    // ===================================================================
    // unittests_paging_test_48()
    // ===================================================================
    private int unittests_paging_test_48(int ram8000) {
        int r = 0;
        r += assert_16k_pages(0, 5, ram8000, 0);
        assertEquals(5, testDriver.getCurrentScreen());
        return r;
    }

    // ===================================================================
    // paging_test_128_unlocked()
    // ===================================================================
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

    // ===================================================================
    // paging_test_128_locked()
    // ===================================================================
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

    // ===================================================================
    // paging_test_128()
    // ===================================================================
    private int paging_test_128() {
        int r = 0;
        r += paging_test_128_unlocked(2);
        r += paging_test_128_locked(2);
        return r;
    }

    // ===================================================================
    // paging_test_plus3()
    // ===================================================================
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

    // ===================================================================
    // paging_test_timex()
    // ===================================================================
    private int paging_test_timex(int ram8000, int dock_source, int exrom_source) {
        int r = 0;
        r += unittests_paging_test_48(ram8000);

        cpu.out(0x00f4, (byte) 0x01);
        r += unittests_assert_8k_page(0x0000, dock_source, 0);
        r += unittests_assert_8k_page(0x2000, 0, 0); // ROM
        r += unittests_assert_16k_ram_page(0x4000, 5);
        r += unittests_assert_16k_ram_page(0x8000, ram8000);
        r += unittests_assert_16k_ram_page(0xc000, 0);

        cpu.out(0x00f4, (byte) 0x04);
        r += assert_16k_rom_page(0x0000, 0);
        r += unittests_assert_8k_page(0x4000, dock_source, 2);
        r += unittests_assert_8k_page(0x6000, 1, 5); // RAM
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
    // paging_test()
    // ===================================================================
    private int paging_test() {
        int r = 0;
        String machine = testDriver.getModel();

        switch (machine) {
            case "16K" -> r += paging_test_16();
            case "48K", "48K_NTSC" -> r += unittests_paging_test_48(2);
            case "128K", "+2", "PENT" -> r += paging_test_128();
            case "+2A", "+3", "+3E", "128E" -> r += paging_test_plus3();
            case "SCORP" -> r += paging_test_scorpion();
            case "PENT512" -> r += paging_test_pentagon512();
            case "PENT1024" -> r += paging_test_pentagon1024();
            case "TC2048" -> r += paging_test_timex(2, -1, -1);
            case "TC2068", "TS2068" -> r += paging_test_timex(2, -1, 2); // exrom_source
            case "SE" -> r += paging_test_se();
            default -> System.out.printf("%s:%d: unknown machine?%n", "SpeccyUnitTests.java", 123);
        }

        // Peripheral tests skipped for 16K and SE
        if (!Arrays.asList("16K", "SE").contains(machine)) {
            // r += if1_unittest(); etc. (not implemented)
        }

        return r;
    }

    // Stubs para pruebas no implementadas
    private int paging_test_scorpion() { return 0; }
    private int paging_test_pentagon512() { return 0; }
    private int paging_test_pentagon1024() { return 0; }
    private int paging_test_se() { return 0; }
}