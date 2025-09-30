package model.tests;

import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.fuse.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZXSpectrumContendedMemoryTests2 {
  static private TestDriver testDriver1;
  static private TestDriver testDriver2;
  private static LocalLibretroCore localLibretroCore;
  private static LibretroCore remoteCore;

  @BeforeAll
  public static void beforeall() {
    localLibretroCore = new LocalLibretroCore();
    remoteCore = FuseLibretroConnector.core;
    CommandHandler commandHandler1 = DefaultCommandHandler.createCommandHandler(localLibretroCore);
    CommandHandler commandHandler2 = DefaultCommandHandler.createCommandHandler(remoteCore);

    testDriver1 = new TestDriver(commandHandler1);
    testDriver2 = new TestDriver(commandHandler2);
  }

  @BeforeEach
  void setUp() {
    testDriver1.reset();
    testDriver1.tstatesHistoryInit();
    testDriver2.reset();
    testDriver2.tstatesHistoryInit();
  }


  @AfterAll
  static void tearDown() {
  }

  @Test
  void test48KExecuteGame() {
    testDriver1.setModel("48K");
    testDriver1.loadSnapshot("/home/fernando/detodo/desarrollo/m/zx/roms/jsw3.z80");

    testDriver2.setModel("48K");
    testDriver2.loadSnapshot("/home/fernando/detodo/desarrollo/m/zx/roms/jsw3.z80");

    List<TStateUpdate> localtStateUpdates = new ArrayList<>();
    List<TStateUpdate> remotetStateUpdates = new ArrayList<>();


    for (int i = 0; i < 2000; i++) {
      testDriver1.tstatesHistoryInit();
      testDriver2.tstatesHistoryInit();
      for (int j = 0; j < 10; j++) {
        testDriver1.step();
        testDriver2.step();
//        if (Spectrum.tstates > 62080 && Spectrum.tstates < 62087 ) {
//          System.out.println("sdsdgsdgdg");
//        }
      }

      List<TStateUpdate> tStateUpdates = GetTStatesHistory.getLocalTStateUpdates(localLibretroCore);
      List<TStateUpdate> tStateUpdates2 = GetTStatesHistory.getRemoteTStateUpdates2(remoteCore);

      localtStateUpdates.addAll(tStateUpdates);
      remotetStateUpdates.addAll(tStateUpdates2);

      if (tStateUpdates.size() != tStateUpdates2.size()) {
        System.out.println("Different size at step " + i);
        System.out.println("Local: " + tStateUpdates);
        System.out.println("Remote: " + tStateUpdates2);
//        assertEquals(tStateUpdates2, tStateUpdates);
      }
//      assertEquals(tStateUpdates2.size(), tStateUpdates.size());

//      for (int j = 0; j < tStateUpdates.size(); j++) {
//        assertEquals(tStateUpdates2.get(j), tStateUpdates.get(j));
//      }
//      assertEquals(tStateUpdates.toString(), tStateUpdates2.toString());
//      System.out.println(tStateUpdates.get(0).pc);
//      assertEquals(tStateUpdates2, tStateUpdates);
      int key = tStateUpdates.get(tStateUpdates.size() - 1).key;
      System.out.println(key);
    }
//    assertEquals(initialTStates + 4 + 6 + 3 + 4, cpu.getTStates()); // Fetch + delay + write + delay, total +17
//    assertEquals((byte) 0xAA, bus.readMemory(26000));
      assertEquals(localtStateUpdates, remotetStateUpdates);


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
                 ]""", testDriver1);
  }

}