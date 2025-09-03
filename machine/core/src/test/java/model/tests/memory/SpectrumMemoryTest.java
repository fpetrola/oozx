package model.tests.memory;

import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryContention;
import com.fpetrola.oozx.speccy.modules.memory.DecodedMemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Registers;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One fact about a Spectrum's memory per test, asked of the bus alone, with no machine running.
 * Every one of them runs against the bus as defined and against the bus that remembers, which
 * must agree; the map here is a 128K's, made by hand, because making it is the machine's job and
 * these tests are not about a machine.
 * <p>
 * They came from prototypes/tdd, where they were written one at a time and each forced the piece
 * of the model that came after it.
 */
class SpectrumMemoryTest {
  static Stream<MemoryBus> buses() {
    return Stream.of(new MemoryBus(), new DecodedMemoryBus());
  }

  private static byte[] image(int size, int filledWith) {
    byte[] image = new byte[size];
    java.util.Arrays.fill(image, (byte) filledWith);
    return image;
  }

  private static Rom romOf(int size, int filledWith) {
    Rom rom = new Rom(size);
    rom.fill(image(size, filledWith));
    return rom;
  }

  /** The map a 128K starts with: what the machine does with the memories it has, done here by hand. */
  private static SpectrumMemory machine(MemoryBus bus) {
    return machineDrawnBy(bus, offset -> { });
  }

  private static SpectrumMemory machineDrawnBy(MemoryBus bus, IntConsumer screen) {
    SpectrumMemory banks = new SpectrumMemory(new Rom.Protection());
    banks.rom(0).fill(image(0x4000, 0x11));
    banks.rom(1).fill(image(0x4000, 0x22));
    for (int bank = 1; bank < 8; bank += 2) {
      banks.ram(bank).contended = true;
    }
    bus.slot(0x4000, banks.ram(5));
    bus.slot(0x8000, banks.ram(2));
    page(bus, banks, 0, 0, 5, screen);
    return banks;
  }

  /** What an OUT to 0x7ffd does: the ROM and the top bank chosen, and which bank is the picture. */
  private static void page(MemoryBus bus, SpectrumMemory banks, int romNumber, int bank, int screenBank, IntConsumer screen) {
    bus.slot(0x0000, banks.rom(romNumber));
    bus.slot(0xc000, banks.ram(bank));
    banks.show(banks.ram(screenBank), screen);
  }

  private static MemoryContention waitingIn(List<String> waits) {
    return new MemoryContention() {
      public void beforeRead() {
        waits.add("read");
      }

      public void beforeWrite() {
        waits.add("write");
      }
    };
  }

  private static Registers controller(List<String> log) {
    return new Registers(0x800) {
      byte status = (byte) 0x80;

      public int read(int offset) {
        log.add("read " + (offset & 3));
        return status & 0xff;
      }

      public void write(int offset, byte value) {
        log.add("write " + (offset & 3) + "=" + (value & 0xff));
        status = value;
      }
    };
  }

  @ParameterizedTest
  @MethodSource("buses")
  void ramGivesBackTheByteThatWasWritten(MemoryBus bus) {
    machine(bus);
    bus.write(0x4000, (byte) 0x55);
    assertEquals(0x55, bus.read(0x4000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aWriteToTheBottom16kChangesNothing(MemoryBus bus) {
    machine(bus);
    bus.write(0x0000, (byte) 0x99);
    assertEquals(0x11, bus.read(0x0000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void theBottom16kAnswersFromTheRomTheMachineWasBuiltWith(MemoryBus bus) {
    machine(bus);
    assertEquals(0x11, bus.read(0x0000));
    assertEquals(0x11, bus.read(0x3fff));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aBankPagedAtTheTopIsTheSameMemoryItAlreadyWasLowerDown(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    bus.write(0x8000, (byte) 0x77);
    page(bus, banks, 0, 2, 5, offset -> { });
    assertEquals(0x77, bus.read(0xc000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void pagingChoosesWhichOfTheTwoRomsIsAtTheBottom(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    assertEquals(0x11, bus.read(0x0000));
    page(bus, banks, 1, 0, 5, offset -> { });
    assertEquals(0x22, bus.read(0x0000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aDeviceHoldingRomcsCoversTheMachinesRomUntilItLetsGo(MemoryBus bus) {
    machine(bus);
    MappedMemory deviceRom = new MappedMemory(0x0000, romOf(0x4000, 0x33));
    assertEquals(0x11, bus.read(0x0000));
    bus.plug(deviceRom);
    assertEquals(0x33, bus.read(0x0000));
    bus.unplug(deviceRom);
    assertEquals(0x11, bus.read(0x0000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void anEightKRomAnswersAtTwoAddressesFromItsOwnFirstByteAtEach(MemoryBus bus) {
    machine(bus);
    byte[] contents = image(0x2000, 0x33);
    contents[0x1fff] = 0x44;
    Rom rom = new Rom(0x2000);
    rom.fill(contents);
    bus.plug(new MappedMemory(0x0000, rom), new MappedMemory(0x2000, rom));
    assertEquals(0x33, bus.read(0x0000));
    assertEquals(0x44, bus.read(0x1fff), "the last byte of its 8K");
    assertEquals(0x33, bus.read(0x2000), "and its first byte again from 0x2000");
    assertEquals(0x44, bus.read(0x3fff));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void theLastDevicePluggedInAnswersOverTheEarlierOnes(MemoryBus bus) {
    machine(bus);
    MappedMemory first = new MappedMemory(0x0000, romOf(0x2000, 0x33));
    MappedMemory second = new MappedMemory(0x0000, romOf(0x2000, 0x44));
    bus.plug(first);
    bus.plug(second);
    assertEquals(0x44, bus.read(0x0000));
    bus.unplug(second);
    assertEquals(0x33, bus.read(0x0000), "the one underneath was still there");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void lettingGoOfRomcsLeavesTheBusAsIfTheDeviceHadNeverTakenIt(MemoryBus bus) {
    machine(bus);
    MappedMemory first = new MappedMemory(0x0000, romOf(0x2000, 0x33));
    MappedMemory second = new MappedMemory(0x0000, romOf(0x2000, 0x44));
    bus.plug(first);
    bus.plug(second);
    bus.plug(first);
    assertEquals(0x33, bus.read(0x0000), "taking it again puts it back on top");
    bus.unplug(first);
    assertEquals(0x44, bus.read(0x0000), "and letting go takes it off for good");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aControllersRegistersAreMemoryAndEveryAccessReachesTheDevice(MemoryBus bus) {
    machine(bus);
    List<String> log = new ArrayList<>();
    bus.plug(new MappedMemory(0x3800, controller(log)));
    assertEquals(0x80, bus.read(0x3802));
    bus.write(0x3801, (byte) 0x7f);
    assertEquals(0x7f, bus.read(0x3ff0), "and it answers to the end of its 2K");
    assertEquals(List.of("read 2", "write 1=127", "read 0"), log);
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aDeviceShowsOneWindowOfAMemoryBiggerThanTheWindow(MemoryBus bus) {
    machine(bus);
    Ram card = new Ram(0x8000);
    bus.plug(new MappedMemory(0x2000, card, 0x2000, 0x2000));
    bus.write(0x2000, (byte) 0x66);
    assertEquals(0x66, card.read(0x2000), "the write landed in the page shown");
    assertEquals(0x00, card.read(0x0000), "not at the card's own start");

    bus.plug(new MappedMemory(0x2000, card, 0x4000, 0x2000));
    assertEquals(0x00, bus.read(0x2000), "another page of the same card in the same window");
    bus.write(0x2000, (byte) 0x77);
    assertEquals(0x77, card.read(0x4000));
    assertEquals(0x66, card.read(0x2000), "and the page that was there is untouched");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void contentionFollowsTheBankAndNotTheAddress(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    assertTrue(bus.reading(0x4000).memory().contended, "bank 5 is shared with the ULA");
    assertFalse(bus.reading(0x8000).memory().contended, "bank 2 is not");
    page(bus, banks, 0, 1, 5, offset -> { });
    assertTrue(bus.reading(0xc000).memory().contended, "and bank 1 takes it with it to the top");
    page(bus, banks, 0, 2, 5, offset -> { });
    assertFalse(bus.reading(0xc000).memory().contended);
  }

  @ParameterizedTest
  @MethodSource("buses")
  void theShownBankReportsEveryByteOfThePictureThatChanges(MemoryBus bus) {
    List<Integer> changed = new ArrayList<>();
    machineDrawnBy(bus, changed::add);
    bus.write(0x4000, (byte) 0x55);
    assertEquals(List.of(0), changed);
    bus.write(0x4000, (byte) 0x55);
    assertEquals(List.of(0), changed, "writing the byte that was already there changes no picture");
    bus.write(0x4000 + 0x1b00, (byte) 0x55);
    assertEquals(List.of(0), changed, "and above the picture there is nothing to draw");
    bus.write(0x8000, (byte) 0x55);
    assertEquals(List.of(0), changed, "nor is a bank that is not the screen");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void pagingTheScreenToAnotherBankMovesWhoReports(MemoryBus bus) {
    List<Integer> changed = new ArrayList<>();
    SpectrumMemory banks = machineDrawnBy(bus, changed::add);
    page(bus, banks, 0, 7, 7, changed::add);
    bus.write(0x4000, (byte) 0x55);
    assertEquals(List.of(), changed, "bank 5 is not the picture any more");
    bus.write(0xc000, (byte) 0x55);
    assertEquals(List.of(0), changed, "bank 7 is");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aRamWithItsWriteProtectionOnKeepsWhatItHasAndStillReads(MemoryBus bus) {
    machine(bus);
    Ram shadow = new Ram(0x2000);
    bus.plug(new MappedMemory(0x2000, shadow));
    bus.write(0x2000, (byte) 0x66);
    shadow.writeProtected = true;
    bus.write(0x2000, (byte) 0x77);
    assertEquals(0x66, bus.read(0x2000));
    shadow.writeProtected = false;
    bus.write(0x2000, (byte) 0x77);
    assertEquals(0x77, bus.read(0x2000));
  }

  @ParameterizedTest
  @MethodSource("buses")
  void anInterfacePutsAllOfItselfOnTheBusAtOnceAndTakesItAllOffAtOnce(MemoryBus bus) {
    machine(bus);
    Rom rom = romOf(0x2000, 0x33);
    MappedMemory[] held = {new MappedMemory(0x0000, rom), new MappedMemory(0x2000, rom), new MappedMemory(0x3800, controller(new ArrayList<>()))};

    bus.plug(held);
    assertEquals(0x33, bus.read(0x0000));
    assertEquals(0x33, bus.read(0x2000), "its 8K ROM again from 0x2000");
    assertEquals(0x80, bus.read(0x3800), "and its controller over the top of that");

    bus.unplug(held);
    assertEquals(0x11, bus.read(0x0000));
    assertEquals(0x11, bus.read(0x3800), "all of it went at once");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void anAccessToSharedMemoryTellsWhoeverSharesItFirst(MemoryBus bus) {
    List<String> waits = new ArrayList<>();
    bus.contention = waitingIn(waits);
    machine(bus);
    bus.read(0x4000);
    bus.write(0x4000, (byte) 0x55);
    assertEquals(List.of("read", "write"), waits, "bank 5 is shared with the ULA");
    bus.read(0x8000);
    bus.write(0x8000, (byte) 0x55);
    assertEquals(List.of("read", "write"), waits, "bank 2 is not");
    bus.read(0x0000);
    assertEquals(List.of("read", "write"), waits, "nor is the ROM");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void puttingASnapshotBackMakesNobodyWaitBecauseTheMachineIsNotRunning(MemoryBus bus) {
    List<String> waits = new ArrayList<>();
    bus.contention = waitingIn(waits);
    machine(bus);
    bus.poke(0x4000, (byte) 0x55);
    assertEquals(0x55, bus.peek(0x4000));
    assertEquals(List.of(), waits, "shared or not, no beam is waiting for this");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aMemoryIsOnTheBusBeforeItHasAnythingInIt(MemoryBus bus) {
    SpectrumMemory banks = new SpectrumMemory(new Rom.Protection());
    bus.slot(0x0000, banks.rom(0));
    assertEquals(0x00, bus.read(0x0000), "the ROM is in its slot, and empty");
    banks.rom(0).fill(image(0x4000, 0x11));
    assertEquals(0x11, bus.read(0x0000), "and filling it is not paging it in again");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void everyMemorySaysWhichPageItIsSoASnapshotKnowsWhereItGoes(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    page(bus, banks, 1, 7, 5, offset -> { });
    assertEquals(7, bus.reading(0xc000).memory().pageNum, "bank 7 at the top");
    assertEquals(5, bus.reading(0x4000).memory().pageNum);
    assertEquals(2, bus.reading(0x8000).memory().pageNum);
    assertEquals(1, bus.reading(0x0000).memory().pageNum, "and the ROM says which ROM it is");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aDeviceAnswersReadsFromOneMemoryAndTakesWritesInAnother(MemoryBus bus) {
    machine(bus);
    Rom eeprom = romOf(0x2000, 0x33);
    Ram behind = new Ram(0x2000);
    bus.plug(new MappedMemory(0x0000, eeprom, 0, 0x2000, true, false),
        new MappedMemory(0x0000, behind, 0, 0x2000, false, true));
    assertEquals(0x33, bus.read(0x0000), "the reads come from the EEPROM");
    bus.write(0x0000, (byte) 0x66);
    assertEquals(0x33, bus.read(0x0000), "which did not take the write");
    assertEquals(0x66, behind.read(0), "the RAM behind it did");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aDeviceTakesOffTheRangeItNamedAndNotTheObjectItKept(MemoryBus bus) {
    machine(bus);
    Rom rom = romOf(0x2000, 0x33);
    bus.plug(new MappedMemory(0x0000, rom));
    assertEquals(0x33, bus.read(0x0000));
    bus.unplug(new MappedMemory(0x0000, rom));
    assertEquals(0x11, bus.read(0x0000), "the same memory from the same address is the same range");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void pagingTheSameBankIntoTheSameSlotIsTheRangeItAlreadyWas(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    MappedMemory top = bus.reading(0xc000);
    page(bus, banks, 0, 1, 5, offset -> { });
    page(bus, banks, 0, 0, 5, offset -> { });
    assertSame(top, bus.reading(0xc000), "the same memory at the same address is one range, not a new one per OUT");
  }

  @ParameterizedTest
  @MethodSource("buses")
  void aResetPutsBackTheMapTheMachineStartsWith(MemoryBus bus) {
    SpectrumMemory banks = machine(bus);
    page(bus, banks, 1, 7, 7, offset -> { });
    bus.write(0xc000, (byte) 0x55);
    machineDrawnBy(bus, offset -> { });
    assertEquals(0x11, bus.read(0x0000), "ROM 0 again");
    assertEquals(0x00, bus.read(0xc000), "and bank 0 at the top, not the one that was there");
    assertEquals(0x55, banks.ram(7).read(0), "what bank 7 holds is not a reset's business");
  }

  @Test
  void theBusThatRemembersFindsTheBytesOfAMemoryThatIsBytes() {
    DecodedMemoryBus bus = new DecodedMemoryBus();
    machine(bus);
    bus.plug(new MappedMemory(0x3800, controller(new ArrayList<>())));
    assertNotNull(bus.readingAt(0x4000).bytes, "a bank is read where it lies, with nothing asked of it");
    assertNull(bus.readingAt(0x3800).bytes, "a device's registers have no bytes, so they are asked");
  }

  @Test
  void theBusThatRemembersAnswersTheSameAsTheOneThatAsksOnEveryByte() {
    MemoryBus asking = new MemoryBus(), remembering = new DecodedMemoryBus();
    for (MemoryBus bus : List.of(asking, remembering)) {
      machine(bus);
      Rom rom = romOf(0x2000, 0x33);
      bus.plug(new MappedMemory(0x0000, rom), new MappedMemory(0x2000, rom), new MappedMemory(0x3800, controller(new ArrayList<>())));
    }
    for (int address = 0; address < 0x10000; address++) {
      assertEquals(asking.read(address), remembering.read(address), "at " + Integer.toHexString(address));
    }
  }
}
