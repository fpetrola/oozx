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


    for (int i = 0; i < 18000; i++) {
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
  }

}