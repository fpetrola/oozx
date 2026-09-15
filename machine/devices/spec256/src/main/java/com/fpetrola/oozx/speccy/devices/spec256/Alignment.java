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


package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.google.inject.Singleton;

import java.util.function.IntPredicate;

/**
 * What a processor that follows another takes from it before every instruction, so that it can
 * never end up anywhere else.
 * <p>
 * The program counter always, whatever else is named here, and the latches that are the
 * processor's own workings rather than its data: the interrupt flip-flops, the mode, the refresh
 * and interrupt registers, and whether it is halted. Everything not taken stays the follower's,
 * and that is where its colours are.
 * <p>
 * A game says what else it needs with the letters its own file uses: {@code A F B C D E H L} for
 * the registers, {@code X x Y y} for the halves of the index registers, {@code S s} for the stack
 * pointer, {@code P} for the program counter that is taken anyway, {@code 1} and {@code 0} for
 * the flags and the alternate flags except the carry, and the same letters in lowercase for the
 * alternate set. The carry is never taken: it is the one thing a follower computes for itself.
 * <p>
 * And one letter that takes nothing: {@code T} says a follower <em>addresses</em> memory with the
 * machine's pointers while keeping its own as numbers. Taking a pointer as a number instead is a
 * blunt version of the same thing that works and costs the colours the register was carrying.
 */
@Singleton
public final class Alignment {
  public static int PC;
  /** Whether what the machine is pointing at is a picture rather than a table every plane shares. */
  private IntPredicate aPictureIsThere = address -> false;

  public void aPictureIsThere(IntPredicate there) {
    aPictureIsThere = there;
  }

  public static final java.util.Map<String, Long> DRIFT = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * What is taken when a game says nothing: the stack pointer and every flag but the carry, and
   * not {@code T}. A follower's pointer is not always drifting - a game that mirrors a sprite
   * looks it up in a table indexed by the very byte it is mirroring, and there the follower is
   * right to go somewhere the machine did not. Which of the two a game does is the game's to say,
   * and most games need to say nothing: a colour that walks into an address is caught where it
   * walks in, by {@link Planes#writingWhereTheMachineWrote} and by
   * {@link Rules#addressesAddedUpByTheMachine}, without costing the register its colour.
   */
  public static final String BY_DEFAULT = "1PSs";
  private static final int CARRY = 0x01;
  private static final String LETTERS = "AFBCDEHLXxYy10PSsafbcdehl";
  private static final RegisterName[] MEANS = {
      RegisterName.A, RegisterName.F, RegisterName.B, RegisterName.C, RegisterName.D, RegisterName.E,
      RegisterName.H, RegisterName.L, RegisterName.IXH, RegisterName.IXL, RegisterName.IYH, RegisterName.IYL,
      null, null, RegisterName.PC, RegisterName.SP, RegisterName.SP,
      RegisterName.Ax, RegisterName.Fx, RegisterName.Bx, RegisterName.Cx, RegisterName.Dx, RegisterName.Ex,
      RegisterName.Hx, RegisterName.Lx};

  private String said;
  private RegisterName[] named;
  private boolean flags;
  private boolean alternateFlags;
  private boolean addressesFromTheOneFollowed;
  private boolean numbersFromTheOneFollowed;

  public Alignment() {
    says(BY_DEFAULT);
  }

  /** Reading a game's own letters, where anything it does not know is a mistake worth hearing about. */
  public void says(String letters) {
    java.util.LinkedHashSet<RegisterName> wanted = new java.util.LinkedHashSet<>();
    wanted.add(RegisterName.PC);
    flags = alternateFlags = addressesFromTheOneFollowed = false;
    for (char letter : letters.toCharArray()) {
      if (letter == 'T') {
        addressesFromTheOneFollowed = true;
        continue;
      }
      int at = LETTERS.indexOf(letter);
      if (at < 0) throw new IllegalArgumentException("A follower cannot be asked for '" + letter + "', only for one of " + LETTERS);
      if (letter == '1') flags = true;
      else if (letter == '0') alternateFlags = true;
      else wanted.add(MEANS[at]);
    }
    named = wanted.toArray(new RegisterName[0]);
    said = letters;
  }

  /** The letters it was last told, for whoever shows them. */
  public String said() {
    return said;
  }

  /**
   * Whether a follower goes where the machine goes: its own pointer registers carry colours, and
   * one addition on one of them would send a write where nothing asked for it.
   */
  public boolean addressesFromTheOneFollowed() {
    return addressesFromTheOneFollowed;
  }

  /**
   * Whether the numbers written into instructions come from the machine as well.
   * <p>
   * The two emulators this was read from disagree here and so do the games. GZX takes them from
   * the plane, which lets a game paint a colour into the number an instruction carries and have
   * a follower write that colour; ZX-Poly takes them from the machine, which stops a painted
   * number from sending a follower to an address the machine never went to. Army Moves wants the
   * first and Renegade the second, so neither is the answer: it is a thing a game says.
   */
  public boolean numbersFromTheOneFollowed() {
    return numbersFromTheOneFollowed;
  }

  public void numbersFromTheOneFollowed(boolean fromTheMachine) {
    numbersFromTheOneFollowed = fromTheMachine;
  }

  /**
   * One register read for an address from the machine's and written to as its own, which is the
   * whole of {@code T}: where a follower goes is the machine's business, what it carries is not.
   * It asks on every address rather than once, so that turning it on and off is something a
   * person can do while the game is running.
   */
  public Register addressing(Register machines, Register mine) {
    if (mine instanceof com.fpetrola.z80.registers.RegisterPair pair) {
      return new AddressingPair(machines, pair);
    }
    return new Addressing(machines, mine);
  }

  private class Addressing implements Register {
    protected final Register machines;
    protected final Register mine;

    Addressing(Register machines, Register mine) {
      this.machines = machines;
      this.mine = mine;
    }

    /**
     * Where this follower reads and writes: its own, which is where its colours are, unless it
     * was told to go where the machine goes, or unless its own has wandered off the picture the
     * machine is reading - a pointer that carries a colour points at a pixel that is not there.
     */
    public int read() {
      if (addressesFromTheOneFollowed) return machines.read();
      int ours = mine.read(), his = machines.read();
      return ours == his || !aPictureIsThere.test(his) ? ours : his;
    }

    public void write(int value) {
      mine.write(value);
    }

    public void increment() {
      mine.increment();
    }

    public void decrement() {
      mine.decrement();
    }

    public int getLength() {
      return mine.getLength();
    }

    public String getName() {
      return mine.getName();
    }
  }

  private final class AddressingPair extends Addressing implements com.fpetrola.z80.registers.RegisterPair {
    AddressingPair(Register machines, com.fpetrola.z80.registers.RegisterPair mine) {
      super(machines, mine);
    }

    public Register getHigh() {
      return ((com.fpetrola.z80.registers.RegisterPair) mine).getHigh();
    }

    public Register getLow() {
      return ((com.fpetrola.z80.registers.RegisterPair) mine).getLow();
    }
  }

  public void from(State followed, State follower) {
    for (RegisterName name : named) {
      follower.getRegister(name).write(followed.getRegister(name).read());
    }
    if (flags) exceptTheCarry(followed.getRegister(RegisterName.F), follower.getRegister(RegisterName.F));
    if (alternateFlags) exceptTheCarry(followed.getRegister(RegisterName.Fx), follower.getRegister(RegisterName.Fx));
    follower.getRegister(RegisterName.I).write(followed.getRegister(RegisterName.I).read());
    follower.getRegister(RegisterName.R).write(followed.getRegister(RegisterName.R).read());
    follower.setIff1(followed.isIff1());
    follower.setIff2(followed.isIff2());
    follower.setIntMode(followed.getInterruptionMode());
    follower.setHalted(followed.isHalted());
    follower.setPendingEI(followed.isPendingEI());
  }

  private void exceptTheCarry(com.fpetrola.z80.registers.Register followed, com.fpetrola.z80.registers.Register follower) {
    follower.write((follower.read() & CARRY) | (followed.read() & ~CARRY & 0xff));
  }
}
