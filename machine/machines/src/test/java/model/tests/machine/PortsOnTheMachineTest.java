package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The facts about a Spectrum's ports that need the machine around the bus: when in the
 * processor's cycle the bus is asked, and that a port nobody answers is held up like any other.
 * The rest are core's SpectrumPortsTest, against the bus alone.
 */
class PortsOnTheMachineTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  /** A probe on a port, writing down the T-state at which it was reached. */
  private static class Probe extends AbstractPeripheral {
    final List<String> reached = new ArrayList<>();

    Probe(Speccy speccy, int mask, int value) {
      super(List.of());
      ports(Wired.at(mask, value, new DefaultPortHandler(true, true) {
        public BusAnswer read(int port) {
          reached.add("read at " + speccy.zxClock.getTStates());
          return BusAnswer.of(0xbf);
        }

        public void write(int port, byte value) {
          reached.add("write at " + speccy.zxClock.getTStates());
        }
      }));
    }

    public boolean fitsOn(SpectrumMachine machine) {
      return true;
    }
  }

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  private Probe plug(int mask, int value) {
    Probe probe = new Probe(speccy, mask, value);
    speccy.peripheralRegistry.register(probe);
    speccy.peripheralRegistry.update();
    return probe;
  }

  /** One instruction at 0x8000, which is uncontended on every model, started at that T-state; what it cost. */
  private long costOf(int opcode, int operand, int highByte, int at) {
    speccy.memory.poke(0x8000, (byte) opcode);
    speccy.memory.poke(0x8001, (byte) operand);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).write(highByte);
    speccy.zxClock.setTStates(at);
    speccy.cpu.jump(0x8000);
    long before = speccy.zxClock.getTStates();
    speccy.cpu.step();
    return speccy.zxClock.getTStates() - before;
  }

  private static final int IN_A_N = 0xDB;
  private static final int OUT_N_A = 0xD3;

  /**
   * An IN waits twice before taking the value - once before the address is on the bus and
   * once before the data is - and an OUT puts its value in between. Off the picture nothing
   * holds the processor up, so the two waits are the cycle's own one and two T-states: the IN
   * reads at the seventh T-state after its two fetches plus three, the OUT writes at plus one,
   * and either costs eleven.
   */
  @Test
  void anInWaitsTwiceBeforeTakingTheValueAndAnOutPutsItInBetween() {
    on(Spec48.class);
    Probe probe = plug(0xffff, 0x80ff);

    assertEquals(11, costOf(IN_A_N, 0xff, 0x80, 1000));
    assertEquals(List.of("read at " + (1000 + 7 + 3)), probe.reached);

    probe.reached.clear();
    assertEquals(11, costOf(OUT_N_A, 0xff, 0x80, 1000));
    assertEquals(List.of("write at " + (1000 + 7 + 1)), probe.reached);
  }

  /**
   * Every port access tells whoever may hold the processor up, and a port nobody answers is
   * held up like any other: on a 128, whose ULA holds up an I/O cycle whose address looks like
   * page five, an IN and an OUT to a port nobody answers both cost more over the picture than
   * off it. The wait is the ULA's and the bus is not asked whether anyone is there.
   */
  @Test
  void aPortNobodyAnswersIsHeldUpLikeAnyOther() {
    on(Spec128.class);
    int overThePicture = (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + 32;

    long quietIn = costOf(IN_A_N, 0xff, 0x7f, 1000);
    long quietOut = costOf(OUT_N_A, 0xff, 0x7f, 1000);
    long heldIn = 0, heldOut = 0;
    for (int t = overThePicture; t < overThePicture + 8; t++) {
      heldIn = Math.max(heldIn, costOf(IN_A_N, 0xff, 0x7f, t));
      heldOut = Math.max(heldOut, costOf(OUT_N_A, 0xff, 0x7f, t));
    }

    assertEquals(11, quietIn);
    assertTrue(heldIn > quietIn, "an IN to 0x7fff was never held up over the picture");
    assertTrue(heldOut > quietOut, "nor an OUT");
  }

  /**
   * The value an IN takes is the bus at the moment it is taken, which is after both waits: over
   * the picture on a 48K a port nobody answers reads what the ULA has just fetched, and which
   * byte that is says when the read happened.
   */
  @Test
  void anInTakesWhatIsOnTheBusAfterBothWaits() {
    on(Spec48.class);
    for (int column = 0; column < 4; column++) {
      speccy.memory.poke(0x4000 + column, (byte) (0x10 | column));
      speccy.memory.poke(0x5800 + column, (byte) (0x20 | column));
    }
    int firstPixel = (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + speccy.display.BORDER_WIDTH_COLS * 4;
    int start = firstPixel - 7 - 3 + 3;

    costOf(IN_A_N, 0xff, 0x80, start);

    int read = speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).read() & 0xff;
    assertEquals(0x20, read, "the attribute of column 0, which the bus carries at the first pixel plus three");
  }
}
