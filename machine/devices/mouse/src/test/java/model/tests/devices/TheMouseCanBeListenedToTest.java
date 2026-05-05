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

import com.fpetrola.oozx.speccy.devices.mouse.KempstonMouse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both ends of the wire, which is the only place the answer lives.
 * <p>
 * A pointer that jumps about while the hand moves steadily is a program reading the counter
 * less often than the counter moves: it works out how far to go from the difference between two
 * readings, as a signed byte, so more than 127 counts between two readings comes out as a large
 * move backwards. Neither end shows this on its own - the counter cannot show a difference
 * bigger than 127, which is exactly the case that has gone wrong - so what the hand did has to
 * be known too.
 */
class TheMouseCanBeListenedToTest {

  private static class Heard implements KempstonMouse.Watcher {
    final List<String> reads = new ArrayList<>();
    int sinceItLooked;
    int biggest;

    public void handMoved(int dx, int dy, int x, int y) {
      sinceItLooked += Math.abs(dx);
    }

    public void programRead(String which, int value) {
      reads.add(which + "=" + value);
      if ("x".equals(which)) {
        biggest = Math.max(biggest, sinceItLooked);
        sinceItLooked = 0;
      }
    }
  }

  @Test
  void what_the_hand_did_and_what_the_program_asked_for_are_both_heard() {
    KempstonMouse mouse = new KempstonMouse();
    Heard heard = new Heard();
    mouse.watch(heard);

    mouse.moved(10, 0);
    mouse.x();
    mouse.buttons();

    assertEquals(List.of("x=10", "buttons=255"), heard.reads,
        "the listener must hear what the program asked for and what it got");
  }

  @Test
  void a_hand_faster_than_the_program_looks_is_handed_over_in_readable_pieces() {
    KempstonMouse mouse = new KempstonMouse();

    // A steady hand that gets two hundred counts ahead before the program looks at all.
    for (int step = 0; step < 200; step++) {
      mouse.moved(1, 0);
    }

    assertEquals(127, mouse.x(),
        "the most a reading can carry, because 200 would be read as going backwards");
    assertEquals(200, mouse.x(),
        "and the next reading receives the rest, so the hand arrives in full, just later");
    assertEquals(0, mouse.owed(), "with nothing left owing and nothing thrown away");
  }

  @Test
  void the_difference_a_program_sees_is_never_more_than_it_can_read() {
    KempstonMouse mouse = new KempstonMouse();

    int before = mouse.x();
    for (int step = 0; step < 1000; step++) {
      mouse.moved(9, 0);
      int now = mouse.x();
      int asTheProgramReadsIt = (now - before + 128 & 0xFF) - 128;
      assertTrue(asTheProgramReadsIt >= 0,
          "a hand going one way must never read as going the other: " + asTheProgramReadsIt);
      before = now;
    }
  }

  @Test
  void listening_can_be_stopped_without_the_mouse_minding() {
    KempstonMouse mouse = new KempstonMouse();
    Heard heard = new Heard();
    mouse.watch(heard);
    mouse.watch(null);

    mouse.moved(5, 0);
    mouse.x();

    assertTrue(heard.reads.isEmpty(), "nobody is listening any more");
    assertEquals(5, mouse.x(), "and the mouse goes on working");
  }
}
