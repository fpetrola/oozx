package model.tests;

import com.fpetrola.oozx.fuse.CommandHandler;
import model.connected.*;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZXInterface1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ZXInterface1Tests {

    static private ISpectrumBus bus;
    static private IZXInterface1 interface1;
    static private IMicrodrive microdrive1;
    static private IMicrodrive microdrive2;
    static private TestDriver testDriver;


    @BeforeAll
    public static void beforeall() {
        testDriver = new TestDriver(CommandHandler.createCommandHandler());
        bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
        interface1 = new ConnectedInterface1(testDriver);
        microdrive1 = new ConnectedMicrodrive(testDriver);
        microdrive2 = new ConnectedMicrodrive(testDriver);

        bus.connectComponent(interface1);
        interface1.connectMicrodrive(microdrive1);
        interface1.connectMicrodrive(microdrive2);
    }

    @BeforeEach
    void setUp() {
        interface1.reset();
    }


//    @AfterEach
//    void tearDown() {
//       testDriver.setFinished(true);
//    }

    // Tests for Microdrive selection and control via ports

    @Test
    void testSelectMicrodrive1() {
        // Sequence to select drive 1: clock in 1 for first, 0 for others
        // Set comms data = 1, clock low -> high -> low
        bus.writePort(0xEF, (byte) 0x00); // Clock low, data 0
        bus.writePort(0xEF, (byte) 0x01); // Data 1, clock low
        bus.writePort(0xEF, (byte) 0x03); // Clock high, data 1 - rising edge, shift 1 into ff0
        bus.writePort(0xEF, (byte) 0x01); // Clock low, data 1

        // For other drives, clock 0
        bus.writePort(0xEF, (byte) 0x00); // Data 0, clock low
        bus.writePort(0xEF, (byte) 0x02); // Clock high - shift 0 into ff0, previous to ff1
        bus.writePort(0xEF, (byte) 0x00); // Clock low

        assertEquals(0, interface1.getSelectedMicrodrive()); // 0-based
        assertTrue(microdrive1.isSelected());
        assertFalse(microdrive2.isSelected());
        assertTrue(interface1.isBusy());
        assertTrue(microdrive1.isMotorRunning());
        assertTrue(microdrive1.isLEDOn());
    }

    @Test
    void testSelectMicrodrive2() {
        // Sequence for drive 2: clock 0 for first, 1 for second
        // First clock 0
        bus.writePort(0xEF, (byte) 0x00); // Data 0, clock low
        bus.writePort(0xEF, (byte) 0x02); // Clock high, shift 0
        bus.writePort(0xEF, (byte) 0x00); // Clock low

        // Second clock 1
        bus.writePort(0xEF, (byte) 0x01); // Data 1, clock low
        bus.writePort(0xEF, (byte) 0x03); // Clock high, shift 1 into ff0, previous 0 to ff1
        bus.writePort(0xEF, (byte) 0x01); // Clock low

        assertEquals(1, interface1.getSelectedMicrodrive());
        assertFalse(microdrive1.isSelected());
        assertTrue(microdrive2.isSelected());
        assertTrue(microdrive2.isMotorRunning());
        assertTrue(interface1.isBusy());
    }

    @Disabled
    @Test
    void testDeselectAllMicrodrives() {
        // Select first, then deselect by shifting 0s
        testSelectMicrodrive1();
        // Clock 0 multiple times
        for (int i = 0; i < 8; i++) {
            bus.writePort(0xEF, (byte) 0x00); // Data 0, clock low
            bus.writePort(0xEF, (byte) 0x02); // Clock high
            bus.writePort(0xEF, (byte) 0x00); // Clock low
        }

        assertEquals(-1, interface1.getSelectedMicrodrive());
        assertFalse(interface1.isBusy());
        assertFalse(microdrive1.isMotorRunning());
        assertFalse(microdrive1.isLEDOn());
    }

    @Disabled
    @Test
    void testEnableEraseOnSelectedDrive() {
        testSelectMicrodrive1();
        // Set erase bit
        bus.writePort(0xEF, (byte) 0x04); // Erase on, clock low
        bus.writePort(0xEF, (byte) 0x06); // Clock high (but no shift since data change but clock edge with same selection)
        assertTrue(interface1.isEraseEnabled());
        assertTrue(microdrive1.isEraseCurrentOn());
    }

    @Disabled
    @Test
    void testSetWriteModeOnSelectedDrive() {
        testSelectMicrodrive1();
        // Set R/W bit to 1 (write)
        bus.writePort(0xEF, (byte) 0x08); // R/W on
        assertTrue(interface1.isWriteMode());
        assertTrue(microdrive1.isWriteMode());
    }

    @Disabled
    @Test
    void testReadWriteProtectFromSelectedDrive() {
        microdrive1.setWriteProtect(true);
        testSelectMicrodrive1();
        byte status = bus.readPort(0xEF);
        assertTrue((status & 0x04) != 0); // Bit 2
        assertTrue(interface1.isWriteProtected());
    }

    @Disabled
    @Test
    void testBusyStatusAfterSelection() {
        assertFalse(interface1.isBusy());
        testSelectMicrodrive1();
        byte status = bus.readPort(0xEF);
        assertTrue((status & 0x80) != 0); // Bit 7
        assertTrue(interface1.isBusy());
    }

    @Disabled
    @Test
    void testWriteDataToSelectedMicrodrive() {
        testSelectMicrodrive1();
        bus.writePort(0xE7, (byte) 0xAA); // Data port
        assertEquals((byte) 0xAA, microdrive1.readData());
    }

    @Test
    void testNoDataWriteIfNotSelected() {
        bus.writePort(0xE7, (byte) 0xBB);
        assertNotEquals((byte) 0xBB, microdrive1.readData());
    }

    // Tests for RS232

    @Disabled
    @Test
    void testSetCTSForRS232() {
        bus.writePort(0xEF, (byte) 0x10); // CTS bit 4
        assertTrue(interface1.isCTSSet());
    }

    @Disabled
    @Test
    void testReadDTRForRS232() {
        // Stub DTR active
        byte status = bus.readPort(0xEF);
        assertTrue((status & 0x40) != 0); // Bit 6
        assertTrue(interface1.isDTRActive());
    }

    @Test
    void testSendTxDataRS232() {
        // Set to RS232 mode (comms data high when idle)
        bus.writePort(0xEF, (byte) 0x01); // Data high, no clock
        assertFalse(interface1.isNetworkMode());
        bus.writePort(0xF7, (byte) 0xCC); // Write to F7 for TX
        assertEquals((byte) 0xCC, interface1.getRxData()); // Stub, assume loopback for test
    }

    @Disabled
    @Test
    void testReceiveRxDataRS232() {
        interface1.setTxData((byte) 0xDD); // Set stub
        byte data = bus.readPort(0xF7); // Read from F7
        assertEquals((byte) 0xDD, data);
    }

    // Tests for ZX Network

    @Disabled
    @Test
    void testSwitchToNetworkMode() {
        // Comms data low when idle
        bus.writePort(0xEF, (byte) 0x00); // Data low
        assertTrue(interface1.isNetworkMode());
    }

    @Disabled
    @Test
    void testSendDataNetwork() {
        testSwitchToNetworkMode();
        bus.writePort(0xF7, (byte) 0xEE); // Write net output
        // Verify state
        assertEquals((byte) 0xEE, interface1.getRxData()); // Stub loopback
    }

    @Disabled
    @Test
    void testReadNetInput() {
        interface1.setTxData((byte) 0xFF);
        byte data = bus.readPort(0xF7);
        assertEquals((byte) 0xFF, data);
    }

    // Tests for ROM paging and error handling

    @Test
    void testROMPagingOnError() {
        interface1.pageROMIn(true);
        bus.handleError("Test error");
        assertTrue(interface1.isROMPagedIn());
    }

    @Test
    void testPageROMOut() {
        testROMPagingOnError();
        interface1.pageROMOut();
        assertFalse(interface1.isROMPagedIn());
    }

    // Edge cases and multiple interactions

    @Test
    void testMaxMicrodrivesConnected() {
        for (int i = 0; i < 6; i++) { // Already 2, add 6 more
            interface1.connectMicrodrive(new ConnectedMicrodrive(testDriver));
        }
        assertEquals(8, interface1.getConnectedMicrodrives().size());
    }

    @Disabled
    @Test
    void testNoBusyIfNoSelection() {
        byte status = bus.readPort(0xEF);
        assertEquals(0, status & 0x80);
    }

    @Disabled
    @Test
    void testGapAndSyncStub() {
        // Assume set by internal logic, test read
        testSelectMicrodrive1();
        // Stub set
//        ((ConcreteZXInterface1) interface1).gapDetected = true;
//        ((ConcreteZXInterface1) interface1).syncDetected = true;
        byte status = bus.readPort(0xEF);
        assertTrue((status & 0x02) != 0); // Gap bit 1
        assertTrue((status & 0x01) != 0); // Sync bit 0
    }

    @Disabled
    @Test
    void testWaitBitSet() {
        bus.writePort(0xEF, (byte) 0x20); // Wait bit 5
        assertTrue(interface1.isWaitSet());
    }

    @Disabled
    @Test
    void testCommsBits() {
        bus.writePort(0xEF, (byte) 0x01); // Data high
        assertTrue(interface1.isCommsDataHigh());
        bus.writePort(0xEF, (byte) 0x02); // Clock high
        assertTrue(interface1.isCommsClockHigh());
    }

    @Disabled
    @Test
    void testNetworkModeOnlyWhenIdle() {
        testSelectMicrodrive1();
        assertFalse(interface1.isNetworkMode()); // Busy with microdrive
    }

    @Disabled
    @Test
    void testWriteProtectOnlyWhenSelected() {
//        ((ConcreteMicrodrive) microdrive1).setWriteProtect(true);
        byte status = bus.readPort(0xEF); // No select
        assertFalse((status & 0x04) != 0);
        testSelectMicrodrive1();
        status = bus.readPort(0xEF);
        assertTrue((status & 0x04) != 0);
    }

    @Disabled
    @Test
    void testMultipleSelectionsShift() {
        // Shift multiple 1s, but typically only one selected, but test state
        bus.writePort(0xEF, (byte) 0x01);
        bus.writePort(0xEF, (byte) 0x03); // Shift 1 to ff0
        bus.writePort(0xEF, (byte) 0x01);
        bus.writePort(0xEF, (byte) 0x03); // Shift another 1, now ff0=1, ff1=1
        assertTrue(microdrive1.isSelected());
        assertTrue(microdrive2.isSelected());
        assertTrue(interface1.isBusy());
    }

    @Disabled
    @Test
    void testDTRChange() {
        interface1.setDtrActive(false);
        byte status = bus.readPort(0xEF);
        assertFalse((status & 0x40) != 0);
    }

    // Add more tests if needed for additional scenarios
}