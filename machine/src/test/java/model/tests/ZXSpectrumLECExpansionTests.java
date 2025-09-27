package model.tests;

import com.fpetrola.oozx.fuse.DefaultCommandHandler;
import model.connected.ConnectedMemory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZXSpectrumLECExpansionTests {
    static private TestDriver testDriver;
    static private ConnectedMemory memory;

    @BeforeAll
    public static void beforeall() {
        testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler());
        memory = new ConnectedMemory(testDriver);
    }

    @BeforeEach
    void setUp() {
        testDriver.reset();
    }

    @AfterAll
    static void tearDown() {
    }

    @Test
    void testLECExpansionEnable() {
        memory.enableLEC(true);
        assertTrue(memory.isLECEnabled());
    }

//    @Test
//    void testLECMemorySize528KB() {
//        memory.enableLEC(true);
//        memory.setLECMemorySize(528);
//        assertEquals(528 * 1024, memory.getTotalRAM());
//    }
//
//    @Test
//    void testLECCPMBoot() {
//        memory.enableLEC(true);
//        testDriver.loadCPM("lec.cpm");
//        assertTrue(testDriver.isCPMLoaded());
//    }
//
//    @Test
//    void testLECWithMicrodrive() {
//        memory.enableLEC(true);
//        testDriver.connectMicrodrive();
//        testDriver.loadMDV("cpm.mdv");
//        assertTrue(testDriver.isMDVUsedForCPM());
//    }
//
//    @Test
//    void testLECPaging() {
//        memory.enableLEC(true);
//        memory.setLECPaging(0x10);
//        assertEquals(0x10, memory.getLECPagingRegister());
//    }
//
//    @Test
//    void testLECReadBeyondStandard() {
//        memory.enableLEC(true);
//        memory.writeLEC(0x10000, (byte) 0xAA);
//        assertEquals((byte) 0xAA, memory.readLEC(0x10000));
//    }
//
//    @Test
//    void testLECDisable() {
//        memory.enableLEC(false);
//        assertEquals(48 * 1024, memory.getTotalRAM()); // Fall back to 48K
//    }
//
//    @Test
//    void testLECWithSnapshot() {
//        memory.enableLEC(true);
//        testDriver.saveSnapshot("lec.szx");
//        testDriver.loadSnapshot("lec.szx");
//        assertTrue(memory.isLECEnabled());
//    }
//
//    @Test
//    void testLECCPMVersion() {
//        memory.enableLEC(true);
//        testDriver.loadCPM("lec.cpm");
//        assertEquals("2.2", testDriver.getCPMVersion());
//    }
//
//    @Test
//    void testLECExpansionIn128K() {
//        setupModel("128K", 0);
//        memory.enableLEC(true);
//        assertFalse(memory.isLECCompatible()); // May conflict with 128K paging
//    }
}