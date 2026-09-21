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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finding the byte that holds the lives, by playing.
 * <p>
 * Nobody can say which address it is by looking at 48K of memory. But whoever is playing knows
 * something the memory does not say: that a life was just lost. So the search is not of the
 * memory, it is of the player - take the memory down, play until something you can name
 * happens, say what happened, and throw away every address that disagrees. Three or four
 * rounds of that and the tens of thousands left are a handful.
 * <p>
 * It watches nothing and hooks nothing: peeking costs the machine no cycles, so a search is
 * just looking at the same 48K again and comparing. Which is why this is the whole of what a
 * poke finder needs to be.
 */
public class Hunt {

  /** The bank a 48K poke is written against in a .pok file. */
  public static final int BANK = 8;

  /** Where a game's own memory starts: below this is the ROM, and the ROM is not the game. */
  private static final int FROM = 0x4000;

  /** What the player says happened to the number they are hunting for. */
  public enum Change {
    LESS("went down"), MORE("went up"), SAME("did not change"), DIFFERENT("changed");

    private final String said;

    Change(String said) {
      this.said = said;
    }

    public String said() {
      return said;
    }

    boolean holds(int before, int now) {
      return switch (this) {
        case LESS -> now < before;
        case MORE -> now > before;
        case SAME -> now == before;
        case DIFFERENT -> now != before;
      };
    }
  }

  /** An address still in the running: what it held when the last round began, and what it holds. */
  public record Candidate(int address, int before, int now) {
  }

  private final Speccy machine;
  private final int[] before = new int[0x10000];
  private final Map<Integer, Integer> held = new LinkedHashMap<>();
  private List<Integer> candidates = new ArrayList<>();
  private int rounds;

  public Hunt(Speccy machine) {
    this.machine = machine;
  }

  /** Every address is a candidate again, and what they hold now is what the next round compares against. */
  public void startOver() {
    candidates = new ArrayList<>(0x10000 - FROM);
    for (int address = FROM; address < 0x10000; address++) {
      candidates.add(address);
    }
    rounds = 0;
    takeItDown();
  }

  /** Keeps the addresses that did what the player says the number did, and starts the next round. */
  public void narrow(Change change) {
    List<Integer> still = new ArrayList<>();
    for (int address : candidates) {
      if (change.holds(before[address], now(address))) {
        still.add(address);
      }
    }
    candidates = still;
    rounds++;
    takeItDown();
  }

  /** Keeps only the addresses holding exactly that, which is what somebody who can see the number does. */
  public void narrowTo(int value) {
    List<Integer> still = new ArrayList<>();
    for (int address : candidates) {
      if (now(address) == (value & 0xff)) {
        still.add(address);
      }
    }
    candidates = still;
    rounds++;
    takeItDown();
  }

  private void takeItDown() {
    for (int address : candidates) {
      before[address] = now(address);
    }
  }

  private int now(int address) {
    return machine.memory.peek(address) & 0xff;
  }

  public int left() {
    return candidates.size();
  }

  public int rounds() {
    return rounds;
  }

  /** The first few that are left, which is all anybody reads: the rest are for narrowing further. */
  public List<Candidate> shortlist(int most) {
    List<Candidate> shortlist = new ArrayList<>();
    for (int i = 0; i < Math.min(most, candidates.size()); i++) {
      int address = candidates.get(i);
      shortlist.add(new Candidate(address, before[address], now(address)));
    }
    return shortlist;
  }

  /**
   * Holds an address at a value: the poke, tried out before anybody writes it down. Whoever is
   * holding it has to keep saying so - {@link #keepHeld} - because the game writes there too,
   * and the whole point is to win the argument.
   */
  public void hold(int address, int value) {
    held.put(address & 0xffff, value & 0xff);
  }

  public void release(int address) {
    held.remove(address & 0xffff);
  }

  public Map<Integer, Integer> held() {
    return Map.copyOf(held);
  }

  /** Says it again. Called as often as the window ticks, which is far more often than a game writes. */
  public void keepHeld() {
    held.forEach((address, value) -> machine.memory.poke(address, (byte) (int) value));
  }

  /**
   * What was found, as a line of a .pok file: the format every other emulator and every poke
   * site already speaks, so a find leaves here as something publishable rather than as a note.
   */
  public String asPokeLine(int address, int value) {
    return "M %d %d %d %d".formatted(BANK, address & 0xffff, value & 0xff, now(address));
  }
}
