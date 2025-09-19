package model.tests;

import com.fpetrola.oozx.fuse.CommandHandler;
import model.connected.*;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import model.interfaces.IZXInterface1;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZXSpectrumContendedMemoryTests1 {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;

  @BeforeAll
  public static void beforeall() {
    testDriver = new TestDriver(new CommandHandler());
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
    bus.setModel(model);
    cpu.setModel(model);
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
  }


  @AfterAll
  static void tearDown() {
    testDriver.setFinished(true);
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
    assertEquals((byte) 0xAA, bus.readMemory(26000));
  }

  @Test
  void test48KINCInstructionT14335() {
    int initialTStates = setupModel("48K", 14335);
    cpu.setHL(0x4000);
    cpu.writeMemory(0x4000, (byte) 0x10, true);
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + (3 + 6) + (4) + (3 + 1) + (1 + 5) + (3 + 0), cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +17
    assertEquals((byte) 0x11, bus.readMemory(0x4000));
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
    assertEquals((byte) 0xBB, bus.readMemory(26000));
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
  @Disabled
  @Test
  void testNTSCContendedMemoryT8959() {
    setupModel("NTSC", 8959);
    int initialTStates = cpu.getTStates();
    cpu.readMemory(0x4000);
    assertEquals(initialTStates + 3 + 6, cpu.getTStates());
  }

  @Disabled
  @Test
  void testNTSCINInstructionT8959() {
    setupModel("NTSC", 8959);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("IN A,(n)", new int[]{0xFE});
    assertEquals(initialTStates + 4 + 3 + 6 + 4 + 24, cpu.getTStates()); // Adjusted for I/O contention, example
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
    setupModel("NTSC", 8959);
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
  void testPlus3LDIInstructionT14363() {
    setupModel("+3", 14363);
    cpu.setHL(0x4000);
    cpu.setSP(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
    assertEquals(initialTStates + 4 + 4 + (7 + 3) + 3 + 2, cpu.getTStates()); // Adjusted per +3 pattern, total +28
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
    assertEquals((byte) 0xCC, bus.readMemory(0x4000));
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
    assertEquals(initialTStates + 3 + 5 , cpu.getTStates()); // 5 T-states delay at T14362
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

  @Test
  void test128KLDIInstructionT14362() {
    setupModel("128K", 14362);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
    assertEquals(initialTStates + 4 + 4 + (5 + 3) + (4 + 3) + (5 + 1) + 2, cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +29
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
    cpu.executeInstruction("LD (HL),A", null);
    assertEquals(initialTStates + 4 + 5 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +16
    assertEquals((byte) 0xFF, bus.readMemory(0x4000));
  }

  @Test
  void testPlus3INCInstructionT14366() {
    setupModel("+3", 14366);
    bus.getMemory().setPage(3, 6); // 0xC000-0xFFFF, contended
    cpu.setHL(0xC000);
    cpu.writeMemory(0xC000, (byte) 0x40, true);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("INC (HL)", null);
    assertEquals(initialTStates + 4 + 4 + 3 + 3 + 1 + 3 + 2 - 4, cpu.getTStates()); // Fetch + delay + read + delay + modify + write + delay, total +20
    assertEquals((byte) 0x41, bus.readMemory(0xC000));
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
  }

  @Test
  void testPlus3LDIInstructionT14362() {
    setupModel("+3", 14362);
    bus.getMemory().setPage(1, 5); // 0x4000-0x7FFF
    cpu.setHL(0x4000);
    cpu.setDE(0x6000);
    int initialTStates = cpu.getTStates();
    cpu.executeInstruction("LDI", null);
    assertEquals(initialTStates + 4 + 4 + (0 + 3) + (0 + 3) + 2 + 5, cpu.getTStates()); // Fetch ED + fetch A0 + delay + read + delay + write + extra, total +16
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
    assertEquals(initialTStates + (4 + 1) + (3 + 0) + (3 + 0) + 1 + 3 + 3+14, cpu.getTStates()); // Fetch+delay + pc+1+delay + pc+2+delay + internal + sp-1+delay + sp-2+delay, total +18
  }
}