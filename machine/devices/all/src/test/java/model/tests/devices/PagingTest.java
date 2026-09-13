/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package model.tests.devices;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.emulation.helpers.snapshots.MemoryState;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.emulation.helpers.snapshots.Z80State;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * One memory-paging fact per test, verified through the machine's own ports against every
 * model it applies to; facts and ordering follow the prototypes/tdd derivation.
 */
class PagingTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  private static Stream<String> machines(Predicate<Spectrum> which) {
    return silentMachine().machine.getMachineTypes().stream().filter(which).map(Spectrum::getName);
  }

  /** Models with no 0x7ffd paging port at all (fixed memory map). */
  static Stream<String> unpaged() {
    return machines(machine -> !machine.pagesThrough7ffd());
  }

  /** Every model that decodes the 128's 0x7ffd paging port. */
  static Stream<String> paged() {
    return machines(machine -> machine.pagesThrough7ffd());
  }

  /** Models with 0x7ffd but not 0x1ffd, where the latter address aliases the former. */
  static Stream<String> pagedLikeA128() {
    return machines(machine -> machine.pagesThrough7ffd() && !machine.pagesThrough1ffd());
  }

  /**
   * The ones whose bit 5 is a lock. Named rather than asked, so the test is not confirming itself,
   * and because a machine with more memory than the port has bits may be using that bit to say so.
   */
  static Stream<String> lockedByBit5() {
    return paged().filter(name -> !name.equals("Pentagon 1024K"));
  }

  static Stream<String> pagedLikeAPlus3() {
    return machines(machine -> machine.pagesThrough1ffd());
  }

  private void on(String model) {
    speccy.machine.selectDefault();
    speccy.machine.select(speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getName().equals(model)).findFirst().orElseThrow());
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private void assertMap(int rom, int slot1, int slot2, int slot3) {
    assertMap(speccy, rom, slot1, slot2, slot3);
  }

  /**
   * The bottom and the top, which every machine that pages has in common. What sits in the two
   * middle slots is the machine's own business: a 128 keeps pages 5 and 2 there, and a machine with
   * its own idea of how much memory it has need not.
   */
  private void assertRomAndTop(int rom, int top) {
    assertSame(speccy.banks.rom(rom), speccy.memory.reading(0x0000).memory(), "rom at the bottom");
    assertSame(speccy.banks.ram(top), speccy.memory.reading(0xc000).memory(), "page at 0xc000");
  }

  private void assertAllRam(int slot0, int slot1, int slot2, int slot3) {
    assertAllRam(speccy, slot0, slot1, slot2, slot3);
  }

  private int screen() {
    return shownPage(speccy);
  }

  /**
   * Two facts were one here until a 16K existed to tell them apart: that a machine without the
   * port keeps whatever map it has, which is true of all of them, and that the map is rom, five,
   * two and zero, which is only true of the ones with 48K in them.
   */
  @ParameterizedTest
  @MethodSource("unpaged")
  void withNoPagingPortTheMapIsWhateverItWasBeforeTheWrite(String model) {
    on(model);
    var before = java.util.stream.IntStream.of(0x0000, 0x4000, 0x8000, 0xc000)
        .mapToObj(address -> speccy.memory.reading(address).memory()).toList();
    out(0x7ffd, 0x17);
    var after = java.util.stream.IntStream.of(0x0000, 0x4000, 0x8000, 0xc000)
        .mapToObj(address -> speccy.memory.reading(address).memory()).toList();
    assertEquals(before, after, "writing 0x7ffd moved something on a machine with no paging port");
  }

  /**
   * Half a megabyte needs five bits to name a page and the port has three, so a Pentagon 512 reads
   * two more out of bits 6 and 7 - which a 128 leaves undecoded, and which is why the machine needs
   * nothing else of its own. The three low bits still count first, so the pages go 0 to 7, then 8
   * to 15 with bit 6 up, and so on to 31.
   */
  @Test
  void aPentagon512ReadsTwoMorePageBitsOutOfThePortsTopTwo() {
    on("Pentagon 512K");
    out(0x7ffd, 0x03);
    assertMap(0, 5, 2, 3);
    out(0x7ffd, 0x43);
    assertMap(0, 5, 2, 11);
    out(0x7ffd, 0x83);
    assertMap(0, 5, 2, 19);
    out(0x7ffd, 0xc7);
    assertMap(0, 5, 2, 31);
  }

  /**
   * A megabyte needs six bits and the port has three. The other three are bits 5, 6 and 7 of the
   * same port, which is one more than the half-megabyte machine takes, and the one it takes extra
   * is the bit a 128 locks itself with - so on this machine, until it is told otherwise, there is
   * no lock at all.
   */
  @Test
  void aPentagon1024ReadsThreeMorePageBitsAndHasNoLockWhileItDoes() {
    on("Pentagon 1024K");
    out(0x7ffd, 0x03);
    assertRomAndTop(0, 3);
    out(0x7ffd, 0x23);
    assertRomAndTop(0, 35);
    out(0x7ffd, 0xe7);
    assertRomAndTop(0, 63);
  }

  /**
   * Its own port says how the other one is read. Told that it is the later revision, the page goes
   * back to the three low bits and the bit that was part of it becomes the lock a 128 has, so the
   * same value that named a page a moment ago now stops the machine from paging ever again.
   */
  @Test
  void theSecondPortOfAPentagon1024DecidesWhatTheFirstOneMeans() {
    on("Pentagon 1024K");
    out(0x7ffd, 0x23);
    assertRomAndTop(0, 35);

    out(0xeff7, 0x04);
    assertRomAndTop(0, 3);

    out(0x7ffd, 0x26);
    assertRomAndTop(0, 6);
    out(0x7ffd, 0x01);
    assertRomAndTop(0, 6);
  }

  /** And with another of its bits there is RAM where every other machine has its ROM. */
  @Test
  void aPentagon1024CanPutRamWhereTheRomIs() {
    on("Pentagon 1024K");
    assertSame(speccy.banks.rom(0), speccy.memory.reading(0x0000).memory());
    out(0xeff7, 0x08);
    assertSame(speccy.banks.ram(0), speccy.memory.reading(0x0000).memory(), "RAM at the bottom");
  }

  @Test
  void a48KIsRomAndThenPagesFiveTwoAndZero() {
    on("Spectrum 48K");
    assertRomAndTop(0, 0);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void a128KPutsThePageNamedByTheLowThreeBitsAt0xc000(String model) {
    on(model);
    assertRomAndTop(0, 0);
    out(0x7ffd, 3);
    assertRomAndTop(0, 3);
    out(0x7ffd, 7);
    assertRomAndTop(0, 7);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void bit3ShowsTheScreenFromPage7WithoutMovingAnyPage(String model) {
    on(model);
    assertEquals(5, screen());
    out(0x7ffd, 0x08);
    assertEquals(7, screen());
    assertRomAndTop(0, 0);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void bit4PutsTheSecondRomAtTheBottom(String model) {
    on(model);
    out(0x7ffd, 0x10);
    assertRomAndTop(1, 0);
    out(0x7ffd, 0x00);
    assertRomAndTop(0, 0);
  }

  @ParameterizedTest
  @MethodSource("lockedByBit5")
  void bit5LocksThePagingAsItIsAndEveryLaterWriteIsIgnored(String model) {
    on(model);
    out(0x7ffd, 0x20 | 0x10 | 0x08 | 3);
    assertRomAndTop(1, 3);
    out(0x7ffd, 0x00);
    assertRomAndTop(1, 3);
    assertEquals(7, screen());
  }

  @ParameterizedTest
  @MethodSource("paged")
  void aResetUndoesEverythingTheLockIncluded(String model) {
    on(model);
    out(0x7ffd, 0x20 | 0x10 | 0x08 | 3);
    speccy.machine.reset(true);
    assertRomAndTop(0, 0);
    assertEquals(5, screen());
    out(0x7ffd, 6);
    assertRomAndTop(0, 6);
  }

  /** Loading a snapshot restores the exact port byte last written, lock state included. */
  @Test
  void whatASnapshotKeepsIsTheLastByteWrittenAndLoadingItRestoresTheMapLockIncluded() {
    SpectrumState state = new SpectrumState();
    state.setSpectrumModel(MachineTypes.SPECTRUM128K);
    state.setZ80State(new Z80State());
    state.setMemoryState(new MemoryState());
    state.setPort7ffd(0x20 | 0x13);
    Snapshots.of(speccy).load(state);
    assertRomAndTop(1, 3);
    out(0x7ffd, 0x00);
    assertRomAndTop(1, 3);
  }

  @ParameterizedTest
  @MethodSource("pagedLikeAPlus3")
  void aPlus3HasFourRomsAndBit2Of0x1ffdIsTheHighBitOfTheNumber(String model) {
    on(model);
    out(0x7ffd, 0x10);
    assertMap(1, 5, 2, 0);
    out(0x1ffd, 0x04);
    assertMap(3, 5, 2, 0);
    out(0x7ffd, 0x00);
    assertMap(2, 5, 2, 0);
  }

  /** 0x1ffd bit 0 removes the ROM entirely; bits 1-2 select one of 4 all-RAM maps, ignoring 0x7ffd. */
  @ParameterizedTest
  @MethodSource("pagedLikeAPlus3")
  void aPlus3InSpecialModeIsAllRamInOneOfFourMaps(String model) {
    on(model);
    out(0x7ffd, 0x10 | 6);
    out(0x1ffd, 0x01);
    assertAllRam(0, 1, 2, 3);
    out(0x1ffd, 0x03);
    assertAllRam(4, 5, 6, 7);
    out(0x1ffd, 0x05);
    assertAllRam(4, 5, 6, 3);
    out(0x1ffd, 0x07);
    assertAllRam(4, 7, 6, 3);
    out(0x1ffd, 0x00);
    assertMap(1, 5, 2, 6);
  }

  @ParameterizedTest
  @MethodSource("pagedLikeAPlus3")
  void theLockIn0x7ffdHolds0x1ffdToo(String model) {
    on(model);
    out(0x7ffd, 0x20);
    out(0x1ffd, 0x01);
    assertRomAndTop(0, 0);
  }

  /** A 128 decodes only address lines A15/A1, so 0x1ffd and 0x7ffd are the same port there. */
  @ParameterizedTest
  @MethodSource("pagedLikeA128")
  void a128KHasNo0x1ffdSoWritingItIsWriting0x7ffd(String model) {
    on(model);
    out(0x1ffd, 0x05);
    assertRomAndTop(0, 5);
  }

  @ParameterizedTest
  @MethodSource("pagedLikeAPlus3")
  void theScreenBitStillCountsInSpecialMode(String model) {
    on(model);
    out(0x7ffd, 0x08);
    out(0x1ffd, 0x01);
    assertEquals(7, screen());
  }

  @Test
  void aResetAndASnapshotCoverTheSecondLatchToo() {
    on("Spectrum Plus 3");
    out(0x1ffd, 0x05);
    speccy.machine.reset(true);
    assertRomAndTop(0, 0);

    SpectrumState state = new SpectrumState();
    state.setSpectrumModel(MachineTypes.SPECTRUMPLUS3);
    state.setZ80State(new Z80State());
    state.setMemoryState(new MemoryState());
    state.setPort7ffd(0x20);
    state.setPort1ffd(0x05);
    Snapshots.of(speccy).load(state);
    assertAllRam(4, 5, 6, 3);
  }

  /** Contended RAM pages differ per board: 5 on 48K, odd pages on 128K, upper 4 on +3, none on Pentagon. */
  static Stream<Arguments> contendedPages() {
    return Stream.of(
        Arguments.of("Spectrum 48K", 5, true), Arguments.of("Spectrum 48K", 2, false),
        Arguments.of("Spectrum 128K", 1, true), Arguments.of("Spectrum 128K", 4, false),
        Arguments.of("Spectrum Plus 2", 1, true), Arguments.of("Spectrum Plus 2", 4, false),
        Arguments.of("Spectrum Plus 3", 4, true), Arguments.of("Spectrum Plus 3", 3, false),
        Arguments.of("Pentagon", 1, false), Arguments.of("Pentagon", 5, false));
  }

  @ParameterizedTest
  @MethodSource("contendedPages")
  void whichPagesAreContendedIsTheBoards(String model, int page, boolean contended) {
    on(model);
    int address = page == 5 ? 0x4000 : page == 2 ? 0x8000 : 0xc000;
    if (address == 0xc000) out(0x7ffd, page);
    assertEquals(contended, speccy.memory.contended(address), "page " + page + " on a " + model);
  }
}
