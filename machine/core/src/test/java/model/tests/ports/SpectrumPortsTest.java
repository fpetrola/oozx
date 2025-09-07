package model.tests.ports;

import com.fpetrola.oozx.speccy.ports.Backplane;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.DecodedPortBus;
import com.fpetrola.oozx.speccy.ports.PortBus;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.Wiring;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One fact about a Spectrum's ports per test, asked of the bus alone. They come from
 * prototypes/tdd/ports, where they were derived from the hardware without reading this model
 * first, and they are in the order they were written there.
 */
class SpectrumPortsTest {
  private final Backplane backplane = new Backplane();

  private PortBus bus() {
    return bus(port -> (byte) 0xff);
  }

  private PortBus bus(DecodedPortBus.FloatingBus floating) {
    return new DecodedPortBus(backplane, floating);
  }

  private void attach(Wired... wired) {
    backplane.attach(wired, false);
  }

  private static Wired answering(int mask, int value, int answer) {
    return Wired.at(mask, value, new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        return BusAnswer.of(answer);
      }
    });
  }

  private static Wired listening(int mask, int value, String name, List<String> log) {
    return Wired.at(mask, value, new DefaultPortHandler(false, true) {
      public void write(int port, byte value) {
        log.add(name);
      }
    });
  }

  @Test
  void anOutReachesTheDeviceThatAnswersThatPort() {
    List<String> log = new ArrayList<>();
    attach(listening(0xffff, 0x00fe, "out", log));
    bus().write(0x00fe, (byte) 7);
    assertEquals(List.of("out"), log);
  }

  @Test
  void anInGivesBackWhatTheDevicePutsOnTheBus() {
    attach(answering(0xffff, 0x00fe, 0xbf));
    assertEquals(0xbf, bus().read(0x00fe) & 0xff);
  }

  @Test
  void aPortNobodyAnswersReadsAllOnes() {
    assertEquals(0xff, bus().read(0x1234) & 0xff);
  }

  @Test
  void theUlaAnswersEveryEvenPortBecauseItOnlyLooksAtA0() {
    attach(answering(0x0001, 0x0000, 0xbf));
    PortBus bus = bus();
    assertEquals(0xbf, bus.read(0x00fe) & 0xff, "the port the ULA is named after");
    assertEquals(0xbf, bus.read(0x7ffe) & 0xff, "and every other even one");
    assertEquals(0xbf, bus.read(0x1234) & 0xff);
    assertEquals(0xff, bus.read(0x1235) & 0xff, "an odd port is nobody's");
  }

  /** 0x00de has A0 low, so the ULA takes it, and A5 low, so a Kempston takes it too. */
  @Test
  void anOutReachesEveryDeviceThatDecodesThatPort() {
    List<String> log = new ArrayList<>();
    attach(listening(0x0001, 0x0000, "ula", log), listening(0x0020, 0x0000, "kempston", log));
    PortBus bus = bus();

    bus.write(0x00de, (byte) 7);
    assertEquals(List.of("ula", "kempston"), log, "both are wired to the same bus and both see it");

    log.clear();
    bus.write(0x00fe, (byte) 7);
    assertEquals(List.of("ula"), log, "A5 is high there, so the Kempston is not listening");
  }

  /**
   * Nothing drives the bus high: a device pulls the lines it wants low and lets the rest float up.
   * So two devices answering the same IN give the AND of what each puts on, and neither of them
   * alone - which is why a Kempston and the keyboard interfere on ports they both decode.
   */
  @Test
  void twoDevicesAnsweringTheSameInPullTheirLinesLowTogether() {
    attach(answering(0x0001, 0x0000, 0xbf), answering(0x0020, 0x0000, 0x7e));
    PortBus bus = bus();

    assertEquals(0x3e, bus.read(0x00de) & 0xff, "0xbf and 0x7e, which is neither of them");
    assertEquals(0xbf, bus.read(0x00fe) & 0xff, "only the ULA is there");
  }

  @Test
  void aPortNobodyAnswersReadsWhateverIsFloatingOnTheBus() {
    PortBus bus = bus(port -> (byte) 0x5c);
    assertEquals(0x5c, bus.read(0x1235) & 0xff);

    attach(answering(0x0001, 0x0000, 0xbf));
    assertEquals(0xbf, bus.read(0x1234) & 0xff, "where something does drive the lines, that is what is read");
  }

  /**
   * The keyboard is eight half rows, and A8 to A15 pick which ones are read: a line held low
   * selects its half row, and several at once come back combined.
   */
  @Test
  void theKeyboardReadsEveryHalfRowWhoseLineIsLow() {
    int[] halfRows = new int[8];
    Arrays.fill(halfRows, 0xff);
    halfRows[0] &= ~1;
    halfRows[7] &= ~2;
    attach(Wired.at(0x0001, 0x0000, new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        int value = 0xff;
        for (int halfRow = 0; halfRow < halfRows.length; halfRow++) {
          if ((port & (0x100 << halfRow)) == 0) {
            value &= halfRows[halfRow];
          }
        }
        return BusAnswer.of(value);
      }
    }));
    PortBus bus = bus();

    assertEquals(0xfe, bus.read(0xfefe) & 0xff, "A8 low picks half row 0, whose key 0 is down");
    assertEquals(0xfd, bus.read(0x7ffe) & 0xff, "A15 low picks half row 7, whose key 1 is down");
    assertEquals(0xff, bus.read(0xfdfe) & 0xff, "half row 1 has nothing down");
    assertEquals(0xfc, bus.read(0x00fe) & 0xff, "every line low reads every half row at once");
  }

  @Test
  void anInterfaceUnpluggedLeavesItsPortsToWhoeverIsLeft() {
    Wired kempston = answering(0x0020, 0x0000, 0x1f);
    attach(answering(0x0001, 0x0000, 0xbf));
    attach(kempston);
    PortBus bus = bus();
    assertEquals(0xbf & 0x1f, bus.read(0x00de) & 0xff, "both decode it");

    backplane.detach(new Wired[]{kempston});
    assertEquals(0xbf, bus.read(0x00de) & 0xff, "and now only the ULA does");
    assertEquals(0xff, bus.read(0x001f) & 0xff, "where nothing else was listening, nothing answers");
  }

  /**
   * Decoding a port and driving it are not the same thing: a device can be wired to a port and
   * still leave the lines alone. Only if nobody drove them does what floats stand.
   */
  @Test
  void aDeviceThatDecodesAPortWithoutDrivingItLeavesTheLinesAlone() {
    PortBus bus = bus(port -> (byte) 0x5c);
    attach(Wired.at(0x0001, 0x0000, new DefaultPortHandler(true, false) {
    }));
    assertEquals(0x5c, bus.read(0x00fe) & 0xff, "wired to it, and drove nothing");

    attach(answering(0x0020, 0x0000, 0xbf));
    assertEquals(0xbf, bus.read(0x00de) & 0xff, "once something drives it, the floating value is gone");
  }

  /**
   * The 128's paging latch does not look at the line that says whether this is a read or a write:
   * it decodes the address and takes whatever is on the data bus. So an IN to 0x7ffd pages the
   * machine with what was floating there.
   */
  @Test
  void aLatchThatIgnoresTheReadWriteLineIsWrittenByAnInAsWell() {
    List<String> log = new ArrayList<>();
    PortBus bus = bus(port -> (byte) 0x5c);
    attach(Wired.at(0xc002, 0x4000, new DefaultPortHandler(false, true) {
      public boolean ignoresReadWriteLine() {
        return true;
      }

      public void write(int port, byte value) {
        log.add("paged " + Integer.toHexString(value & 0xff));
      }
    }));
    attach(listening(0x0001, 0x0000, "ula", log));

    bus.read(0x7ffd);
    assertEquals(List.of("paged 5c"), log, "the read wrote what was floating into the latch");

    log.clear();
    bus.read(0x00fe);
    assertEquals(List.of(), log, "a device that does look at that line is not written by a read");
  }

  /**
   * A latch has no way onto the data bus at all, so asking it is meaningless. Nothing observable
   * changes either way, which is why this is asked of a stub that counts having been asked.
   */
  @Test
  void aChipWithNoWayOntoTheDataBusIsNotAskedOnARead() {
    int[] asked = {0};
    attach(Wired.at(0xc002, 0x4000, new DefaultPortHandler(false, true) {
      public BusAnswer read(int port) {
        asked[0]++;
        return BusAnswer.NONE;
      }
    }));
    attach(answering(0x0001, 0x0000, 0xbf));
    PortBus bus = bus();

    assertEquals(0xff, bus.read(0x7ffd) & 0xff, "nothing drives it and nothing floats");
    assertEquals(0, asked[0], "and the latch was never asked");
    assertEquals(0xbf, bus.read(0x00fe) & 0xff, "while a device that does answer still does");
  }

  /** The other half of the same fact: a joystick has nothing to take a write with. */
  @Test
  void aChipThatOnlyReadsIsNotHandedAWrite() {
    int[] told = {0};
    attach(Wired.at(0x0020, 0x0000, new DefaultPortHandler(true, false) {
      public void write(int port, byte value) {
        told[0]++;
      }
    }));
    bus().write(0x001f, (byte) 7);
    assertEquals(0, told[0]);
  }

  /**
   * Remembering who answers what must not mean working out all sixty-five thousand ports every
   * time something is plugged in: a machine sets its devices up one at a time at boot. Only a stub
   * counting the questions can say so.
   */
  @Test
  void pluggingSomethingInDoesNotWorkOutEveryPortThereIs() {
    int[] asked = {0};
    Wiring counted = port -> {
      asked[0]++;
      return (port & 0x0001) == 0x0000;
    };
    attach(new Wired(counted, new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        return BusAnswer.of(0xbf);
      }
    }));

    PortBus bus = bus();
    assertEquals(0, asked[0], "plugging in decodes nothing by itself");

    bus.read(0x00fe);
    int afterTheFirst = asked[0];
    assertTrue(afterTheFirst > 0, "the first access to a port has to work it out");

    bus.read(0x00fe);
    bus.read(0x00fe);
    assertEquals(afterTheFirst, asked[0], "and it is not worked out again");

    attach(answering(0x0020, 0x0000, 0x7e));
    bus.read(0x00fe);
    assertTrue(asked[0] > afterTheFirst, "until the wiring changes, and then it is");
  }

  /** Switching model is not unplugging its devices one by one: the whole backplane goes. */
  @Test
  void everythingComesOffTheBusAtOnce() {
    attach(answering(0x0001, 0x0000, 0xbf), answering(0x0020, 0x0000, 0x7e));
    PortBus bus = bus();
    assertEquals(0x3e, bus.read(0x00de) & 0xff);

    backplane.clear();
    assertEquals(0xff, bus.read(0x00de) & 0xff, "nothing is on the bus any more");
  }

  /**
   * Interfaces stack on the back of the machine and the order is real: one plugged in front is
   * nearer the Spectrum and sees an access first.
   */
  @Test
  void somethingPluggedInFrontOfTheRestSeesAnAccessFirst() {
    List<String> order = new ArrayList<>();
    attach(listening(0x0001, 0x0000, "the one already there", order));
    backplane.attach(new Wired[]{listening(0x0001, 0x0000, "the one in front", order)}, true);

    bus().write(0x00fe, (byte) 7);
    assertEquals(List.of("the one in front", "the one already there"), order);
  }

  /**
   * A chip does not know what it was soldered to: a Kempston is only on 0x1f because of how it was
   * wired, and the same one in another machine answers elsewhere. So the wiring is given when it
   * is plugged in, and the same handler object can be on two buses on different ports at once.
   */
  @Test
  void theSameChipCanBeWiredToDifferentPortsWithoutTouchingIt() {
    PortHandler chip = new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        return BusAnswer.of(0xbf);
      }
    };

    backplane.attach(new Wired[]{new Wired(Wiring.lines(0x0020, 0x0000), chip)}, false);
    Backplane elsewhere = new Backplane();
    elsewhere.attach(new Wired[]{new Wired(Wiring.lines(0x0001, 0x0000), chip)}, false);

    assertEquals(0xbf, bus().read(0x001f) & 0xff, "wired to A5 low here");
    assertEquals(0xff, bus().read(0x00fe) & 0xff, "and not to the even ports");

    PortBus other = new DecodedPortBus(elsewhere, port -> (byte) 0xff);
    assertEquals(0xbf, other.read(0x00fe) & 0xff, "the same chip, wired to A0 low over there");
    assertEquals(0xff, other.read(0x001f) & 0xff);
  }

  /**
   * The bus that remembers answers the same as one that has just been asked, on every one of the
   * 65536 ports, after things have been plugged in and pulled out: what it remembered before the
   * change is not what it answers after it.
   */
  @Test
  void theBusThatRemembersAnswersTheSameAsAFreshOneOnEveryPort() {
    PortBus remembering = bus();
    for (int port = 0; port < 0x10000; port++) remembering.read(port);

    attach(answering(0x0001, 0x0000, 0xbf), answering(0x0020, 0x0000, 0x7e), answering(0xc002, 0xc000, 0xfd));
    Wired gone = answering(0x0080, 0x0000, 0x00);
    attach(gone);
    backplane.detach(new Wired[]{gone});

    PortBus fresh = bus();
    for (int port = 0; port < 0x10000; port++) {
      assertEquals(fresh.read(port), remembering.read(port), "at " + Integer.toHexString(port));
    }
  }
}
