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

package com.fpetrola.oozx;

/**
 * Where a word for the person goes when whoever has it is nowhere near a window: a snapshot that
 * asks for a machine this build does not carry, a processor that cannot run here, a plugin that
 * could not be read. These used to go to the console, where the person running a window never
 * looks, and so the emulator quietly did something other than what was asked.
 * <p>
 * Whoever has a place to show them says so; until then, and in a build with no screen at all,
 * they are printed as before.
 */
public final class TellsThePerson {

  public interface Listening {
    void about(String what);

    /**
     * The same, for something the person asked for and did not get. Worth stopping them for.
     * <p>
     * What is named in {@code thatNeeds} is what would make it work - so far, the machine a
     * snapshot was taken on - so that whoever shows this can offer to bring it rather than
     * leave the person to find out which plugin that is.
     */
    default void aboutNotDoingIt(String what, String thatNeeds) {
      about(what);
    }
  }

  private static final java.util.List<Listening> who = new java.util.concurrent.CopyOnWriteArrayList<>();

  public static void listens(Listening one) {
    who.add(one);
  }

  public static void stops(Listening one) {
    who.remove(one);
  }

  /** A remark: the emulator did what was asked, and this is something about how. */
  public static void that(String what) {
    if (who.isEmpty()) System.out.println("oozx: " + what);
    else who.forEach(one -> one.about(what));
  }

  /**
   * What was asked for did not happen, and the person is better off knowing now: a snapshot on a
   * machine this build does not carry ends in a screen that never comes up, and nothing about it
   * says why.
   */
  public static void thisBuildCannot(String what) {
    thisBuildCannot(what, null);
  }

  /** The same, naming the machine that would make it work, for whoever can go and bring it. */
  public static void thisBuildCannot(String what, String thatNeeds) {
    if (who.isEmpty()) System.out.println("oozx: " + what);
    else who.forEach(one -> one.aboutNotDoingIt(what, thatNeeds));
  }

  private TellsThePerson() {
  }
}
