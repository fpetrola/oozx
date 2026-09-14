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
 */
@Singleton
public final class Alignment {
  /** What is taken when a game says nothing: the stack pointer and every flag but the carry. */
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

  public Alignment() {
    says(BY_DEFAULT);
  }

  /** Reading a game's own letters, where anything it does not know is a mistake worth hearing about. */
  public void says(String letters) {
    java.util.LinkedHashSet<RegisterName> wanted = new java.util.LinkedHashSet<>();
    wanted.add(RegisterName.PC);
    flags = alternateFlags = false;
    for (char letter : letters.toCharArray()) {
      if (letter == 'T') continue;
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
