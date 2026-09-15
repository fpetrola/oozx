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
  /**
   * What is taken when a game says nothing: the stack pointer, every flag but the carry, and
   * going where the machine goes. The last one costs nothing measurable and is what keeps a
   * follower's colours from landing where nothing asked for them.
   */
  public static final String BY_DEFAULT = "1PSsT";
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

    public int read() {
      return (addressesFromTheOneFollowed ? machines : mine).read();
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
