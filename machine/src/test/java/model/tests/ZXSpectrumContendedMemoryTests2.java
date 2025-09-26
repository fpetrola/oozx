package model.tests;

import com.fpetrola.oozx.fuse.*;
import model.connected.*;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import model.interfaces.IZXInterface1;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZXSpectrumContendedMemoryTests2 {
  static private IZ80CPU cpu;
  static private TestDriver testDriver;
  static private ISpectrumBus bus;
  static private IZXInterface1 interface1;
  static private IMicrodrive microdrive;
  private static LocalLibretroCore localLibretroCore;
  private static LibretroCore remoteCore;

  @BeforeAll
  public static void beforeall() {
    localLibretroCore = new LocalLibretroCore();
    remoteCore = CommandHandler.core;
    LibretroCoreMultiplexor libretroCoreMultiplexor = new LibretroCoreMultiplexor(localLibretroCore, remoteCore);
    CommandHandler commandHandler = CommandHandler.createCommandHandler(libretroCoreMultiplexor);

    testDriver = new TestDriver(commandHandler);
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

  @Test
  void test48KExecuteGame() {
    int initialTStates = setupModel("48K", 40000);
    testDriver.loadSnapshot("/home/fernando/detodo/desarrollo/m/zx/roms/jsw3.z80");
    for (int i = 0; i < 30; i++) {
      cpu.step();
    }
//    assertEquals(initialTStates + 4 + 6 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +17
//    assertEquals((byte) 0xAA, bus.readMemory(26000));


    List<TStateUpdate> tStateUpdates = GetTStatesHistory.getLocalTStateUpdates(localLibretroCore);
    List<TStateUpdate> tStateUpdates2 = GetTStatesHistory.getRemoteTStateUpdates2(remoteCore);


    ZXSpectrumContendedMemoryTests.assertTStatesHistory("""
        [TStateUpdate{key=40000, value=3, description='writebyte'}
                 , TStateUpdate{key=40004, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40008, value=4, description='readbyte'}
                 , TStateUpdate{key=40012, value=4, description='readbyte'}
                 , TStateUpdate{key=40015, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40016, value=3, description='readbyte'}
                 , TStateUpdate{key=40017, value=3, description='readbyte'}
                 , TStateUpdate{key=40018, value=3, description='readbyte'}
                 , TStateUpdate{key=40019, value=3, description='writebyte'}
                 , TStateUpdate{key=40020, value=4, description='readbyte'}
                 , TStateUpdate{key=40023, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40027, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40031, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40034, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40037, value=3, description='readbyte'}
                 , TStateUpdate{key=40038, value=3, description='readbyte'}
                 , TStateUpdate{key=40039, value=3, description='readbyte'}
                 , TStateUpdate{key=40042, value=3, description='writebyte'}
                 , TStateUpdate{key=40046, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40050, value=4, description='readbyte'}
                 , TStateUpdate{key=40053, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40054, value=4, description='readbyte'}
                 , TStateUpdate{key=40055, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40056, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40057, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40058, value=3, description='readbyte'}
                 , TStateUpdate{key=40061, value=3, description='writebyte'}
                 , TStateUpdate{key=40065, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40068, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40072, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40075, value=4, description='readbyte'}
                 , TStateUpdate{key=40078, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40082, value=3, description='writebyte'}
                 , TStateUpdate{key=40083, value=4, description='readbyte'}
                 , TStateUpdate{key=40084, value=3, description='writebyte'}
                 , TStateUpdate{key=40085, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40086, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40087, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40088, value=1, description='contend_write_no_mreq'}
                 , TStateUpdate{key=40089, value=1, description='contend_write_no_mreq'}
                 ]""", testDriver);
  }

}