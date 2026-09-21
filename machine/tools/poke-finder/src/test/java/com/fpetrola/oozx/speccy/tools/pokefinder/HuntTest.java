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


package com.fpetrola.oozx.speccy.tools.pokefinder;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.pokes.PokInstruction;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A game with three lives at a known address, played until it loses them. */
class HuntTest extends MachineTest {

  private static final int LIVES = 0x9c40;
  private static final int SOMETHING_ELSE = 0x9c41;

  private static void lives(Speccy speccy, int many) {
    speccy.memory.poke(LIVES, (byte) many);
  }

  @Test
  void whatWentDownWhenTheLifeWentIsWhatIsLeft() {
    Speccy speccy = silentMachine();
    lives(speccy, 3);
    speccy.memory.poke(SOMETHING_ELSE, (byte) 50);
    Hunt hunt = new Hunt(speccy);
    hunt.startOver();

    lives(speccy, 2);
    speccy.memory.poke(SOMETHING_ELSE, (byte) 90);
    hunt.narrow(Hunt.Change.LESS);

    assertTrue(hunt.shortlist(0x10000).stream().anyMatch(at -> at.address() == LIVES),
        "the lives went down, so they are still in the running");
    assertFalse(hunt.shortlist(0x10000).stream().anyMatch(at -> at.address() == SOMETHING_ELSE),
        "that one went up");
  }

  /**
   * The whole method in one test: rounds the player can answer, narrowing tens of thousands of
   * addresses down to a handful without anybody knowing anything about the game.
   */
  @Test
  void afterAFewRoundsThereIsAlmostNothingLeft() {
    Speccy speccy = silentMachine();
    lives(speccy, 3);
    Hunt hunt = new Hunt(speccy);
    hunt.startOver();
    int atFirst = hunt.left();

    lives(speccy, 2);
    hunt.narrow(Hunt.Change.LESS);
    int afterOne = hunt.left();
    hunt.narrow(Hunt.Change.SAME);
    lives(speccy, 1);
    hunt.narrow(Hunt.Change.LESS);

    assertEquals(0xc000, atFirst, "it starts with all the RAM and none of the ROM");
    assertTrue(afterOne < atFirst / 2, "one round throws away most of it: " + afterOne);
    assertTrue(hunt.left() < 200, "and three rounds leave a handful: " + hunt.left());
    assertTrue(hunt.shortlist(0x10000).stream().anyMatch(at -> at.address() == LIVES),
        "the lives survived every round, because they did what the player said");
    assertEquals(3, hunt.rounds());
  }

  @Test
  void theRomIsNeverACandidate() {
    Speccy speccy = silentMachine();
    Hunt hunt = new Hunt(speccy);

    hunt.startOver();

    assertTrue(hunt.shortlist(0x10000).stream().allMatch(at -> at.address() >= 0x4000),
        "the ROM is not the game");
  }

  /** Holding is the poke tried out: the game writes, and whoever is holding says it again. */
  @Test
  void aHeldAddressWinsAgainstTheGameWritingToIt() {
    Speccy speccy = silentMachine();
    lives(speccy, 3);
    Hunt hunt = new Hunt(speccy);
    hunt.hold(LIVES, 9);

    speccy.memory.poke(LIVES, (byte) 1);
    hunt.keepHeld();

    assertEquals(9, speccy.memory.peek(LIVES) & 0xff);

    hunt.release(LIVES);
    speccy.memory.poke(LIVES, (byte) 1);
    hunt.keepHeld();

    assertEquals(1, speccy.memory.peek(LIVES) & 0xff, "let go, the game keeps what it wrote");
  }

  /**
   * What is found leaves as something publishable: the line this writes is a line the
   * emulator's own .pok parser reads back as the same poke.
   */
  @Test
  void whatItFoundIsWrittenAsALineOfAPokFile() {
    Speccy speccy = silentMachine();
    lives(speccy, 3);
    Hunt hunt = new Hunt(speccy);

    String line = hunt.asPokeLine(LIVES, 99);

    PokInstruction.MemoryWriteInstruction poke =
        assertInstanceOf(PokInstruction.MemoryWriteInstruction.class, PokInstruction.parse(line));
    assertEquals(LIVES, poke.getAddress());
    assertEquals(99, poke.getValue());
    assertEquals(Hunt.BANK, poke.getBank());
  }
}
