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

  private void assertAllRam(int slot0, int slot1, int slot2, int slot3) {
    assertAllRam(speccy, slot0, slot1, slot2, slot3);
  }

  private int screen() {
    return shownPage(speccy);
  }

  @ParameterizedTest
  @MethodSource("unpaged")
  void a48KHasOneMapAndWritingThePagingPortChangesNothing(String model) {
    on(model);
    assertMap(0, 5, 2, 0);
    out(0x7ffd, 0x17);
    assertMap(0, 5, 2, 0);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void a128KPutsThePageNamedByTheLowThreeBitsAt0xc000(String model) {
    on(model);
    assertMap(0, 5, 2, 0);
    out(0x7ffd, 3);
    assertMap(0, 5, 2, 3);
    out(0x7ffd, 7);
    assertMap(0, 5, 2, 7);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void bit3ShowsTheScreenFromPage7WithoutMovingAnyPage(String model) {
    on(model);
    assertEquals(5, screen());
    out(0x7ffd, 0x08);
    assertEquals(7, screen());
    assertMap(0, 5, 2, 0);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void bit4PutsTheSecondRomAtTheBottom(String model) {
    on(model);
    out(0x7ffd, 0x10);
    assertMap(1, 5, 2, 0);
    out(0x7ffd, 0x00);
    assertMap(0, 5, 2, 0);
  }

  @ParameterizedTest
  @MethodSource("paged")
  void bit5LocksThePagingAsItIsAndEveryLaterWriteIsIgnored(String model) {
    on(model);
    out(0x7ffd, 0x20 | 0x10 | 0x08 | 3);
    assertMap(1, 5, 2, 3);
    out(0x7ffd, 0x00);
    assertMap(1, 5, 2, 3);
    assertEquals(7, screen());
  }

  @ParameterizedTest
  @MethodSource("paged")
  void aResetUndoesEverythingTheLockIncluded(String model) {
    on(model);
    out(0x7ffd, 0x20 | 0x10 | 0x08 | 3);
    speccy.machine.reset(true);
    assertMap(0, 5, 2, 0);
    assertEquals(5, screen());
    out(0x7ffd, 6);
    assertMap(0, 5, 2, 6);
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
    assertMap(1, 5, 2, 3);
    out(0x7ffd, 0x00);
    assertMap(1, 5, 2, 3);
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
    assertMap(0, 5, 2, 0);
  }

  /** A 128 decodes only address lines A15/A1, so 0x1ffd and 0x7ffd are the same port there. */
  @ParameterizedTest
  @MethodSource("pagedLikeA128")
  void a128KHasNo0x1ffdSoWritingItIsWriting0x7ffd(String model) {
    on(model);
    out(0x1ffd, 0x05);
    assertMap(0, 5, 2, 5);
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
    assertMap(0, 5, 2, 0);

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
