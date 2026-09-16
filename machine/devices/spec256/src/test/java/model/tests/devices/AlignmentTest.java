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

import com.fpetrola.oozx.speccy.devices.spec256.Alignment;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What a processor that follows another takes from it, and what it is left to work out itself. */
class AlignmentTest {
  private final Alignment alignment = new Alignment();
  private final State followed = state();
  private final State follower = state();

  private static State state() {
    return new State(new IO() {
      public int in(int port) {
        return 0xff;
      }

      public void out(int port, int value) {
      }
    }, new DefaultRegisterBankFactory().createBank(), new Memory() {
      public int read(int address, int fetching) {
        return 0;
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    });
  }

  private void set(State state, RegisterName name, int value) {
    state.getRegister(name).write(value);
  }

  private int of(State state, RegisterName name) {
    return state.getRegister(name).read();
  }

  @Test
  void whereverTheOneFollowedIsIsWhereTheFollowerGoesNext() {
    set(followed, RegisterName.PC, 0x1234);
    set(follower, RegisterName.PC, 0x9999);
    alignment.from(followed, follower);

    assertEquals(0x1234, of(follower, RegisterName.PC), "a follower cannot end up anywhere else");
  }

  @Test
  void whatAFollowerMovedStaysItsOwn() {
    set(followed, RegisterName.HL, 0x1111);
    set(followed, RegisterName.A, 0x11);
    set(follower, RegisterName.HL, 0x2222);
    set(follower, RegisterName.A, 0x22);
    alignment.from(followed, follower);

    assertEquals(0x2222, of(follower, RegisterName.HL), "its data registers carry colours, and they are untouched");
    assertEquals(0x22, of(follower, RegisterName.A));
  }

  @Test
  void aGameCanAskForTheOnesItNeeds() {
    alignment.says("1PSsHL");
    set(followed, RegisterName.HL, 0x1111);
    set(followed, RegisterName.A, 0x11);
    set(follower, RegisterName.HL, 0x2222);
    set(follower, RegisterName.A, 0x22);
    alignment.from(followed, follower);

    assertEquals(0x1111, of(follower, RegisterName.HL), "named, it is taken");
    assertEquals(0x22, of(follower, RegisterName.A), "unnamed, it is not");
  }

  @Test
  void everyFlagButTheCarryIsTaken() {
    set(followed, RegisterName.F, 0b11000000);
    set(follower, RegisterName.F, 0b00000001);
    alignment.from(followed, follower);

    assertEquals(0b11000001, of(follower, RegisterName.F), "the carry is the one thing a follower works out for itself");
  }

  @Test
  void theLatchesThatAreTheProcessorsWorkingsAreTakenWhateverTheGameSays() {
    alignment.says("P");
    followed.setIff1(true);
    followed.setIff2(true);
    followed.setIntMode(State.InterruptionMode.IM2);
    followed.setHalted(true);
    set(followed, RegisterName.I, 0x3f);
    alignment.from(followed, follower);

    assertTrue(follower.isIff1());
    assertTrue(follower.isIff2());
    assertEquals(State.InterruptionMode.IM2, follower.getInterruptionMode());
    assertTrue(follower.isHalted());
    assertEquals(0x3f, of(follower, RegisterName.I), "the interrupt register says where its vectors are, and they are the same ones");
  }

  @Test
  void aLetterNobodyKnowsIsWorthHearingAbout() {
    assertThrows(IllegalArgumentException.class, () -> alignment.says("1PSsQ"));
    assertEquals(Alignment.BY_DEFAULT, alignment.said(), "and what it was told before still stands");
  }
}
