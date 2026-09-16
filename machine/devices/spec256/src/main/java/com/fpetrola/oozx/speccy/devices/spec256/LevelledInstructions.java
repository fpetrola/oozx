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
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.impl.And;
import com.fpetrola.z80.instructions.impl.Cpd;
import com.fpetrola.z80.instructions.impl.Cpi;
import com.fpetrola.z80.instructions.impl.Ind;
import com.fpetrola.z80.instructions.impl.Ini;
import com.fpetrola.z80.instructions.impl.Ldd;
import com.fpetrola.z80.instructions.impl.Ldi;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.impl.Outd;
import com.fpetrola.z80.instructions.impl.Outi;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeTargets;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;

import static com.fpetrola.z80.registers.RegisterName.A;
import static com.fpetrola.z80.registers.RegisterName.BC;
import static com.fpetrola.z80.registers.RegisterName.DE;
import static com.fpetrola.z80.registers.RegisterName.HL;

/**
 * The instructions a follower runs, where a game asked for the three logical ones to work on
 * colours rather than on bits.
 * <p>
 * On a plane a byte is not a mask, it is one bit of the colour of each of eight pixels; but the
 * eight of them together are a level, and a game drawing something over something else means the
 * brighter of the two rather than their bits laid on top of each other. So {@code OR} becomes the
 * higher of the two, {@code AND} the lower, and {@code XOR} the higher as well - except
 * {@code XOR A}, which a program writes to mean nothing at all, and which still means nothing.
 * <p>
 * The flags are left as the ordinary operation sets them: what changes is only what is written.
 */
final class LevelledInstructions extends DefaultInstructionFactory {
  private final Rules rules;
  private final State followed;
  private final Alignment alignment;

  LevelledInstructions(State state, Rules rules, State followed, Alignment alignment) {
    super(state);
    this.rules = rules;
    this.followed = followed;
    this.alignment = alignment;
  }

  /**
   * The registers a block instruction walks memory and counts with, taken from the machine: a
   * colour that walked into one of them would send this follower reading and writing where the
   * machine never went, and would make it repeat a different number of times.
   */
  private Register walking(RegisterName name) {
    return alignment.addressing(followed.getRegister(name), state.getRegister(name));
  }

  private RegisterPair walkingPair(RegisterName name) {
    return (RegisterPair) walking(name);
  }

  /** Every address a reference takes from a register is one this follower walks with the machine. */
  @Override
  public OpcodeTargets targets(State state, Memory memoryForOpcodes) {
    return new OpcodeTargets(state, memoryForOpcodes) {
      @Override
      public Register address(RegisterName name) {
        return walking(name);
      }
    };
  }

  @Override
  public Ldi Ldi() {
    return new Ldi(walking(DE), walkingPair(BC), walkingPair(HL), flag, memory, state.getIo(), state.getRegister(A));
  }

  @Override
  public Ldd Ldd() {
    return new Ldd(walking(DE), walkingPair(BC), walkingPair(HL), flag, memory, state.getIo(), state.getRegister(A));
  }

  @Override
  public Cpi Cpi() {
    return new Cpi(state.getRegister(A), flag, walkingPair(BC), walkingPair(HL), memory, state.getIo());
  }

  @Override
  public Cpd Cpd() {
    return new Cpd(state.getRegister(A), flag, walkingPair(BC), walkingPair(HL), memory, state.getIo());
  }

  @Override
  public Ini Ini() {
    return new Ini(walkingPair(BC), walkingPair(HL), flag, memory, state.getIo());
  }

  @Override
  public Ind Ind() {
    return new Ind(walkingPair(BC), walkingPair(HL), flag, memory, state.getIo());
  }

  @Override
  public Outi Outi() {
    return new Outi(walkingPair(BC), walkingPair(HL), flag, memory, state.getIo());
  }

  @Override
  public Outd Outd() {
    return new Outd(walkingPair(BC), walkingPair(HL), flag, memory, state.getIo());
  }

  @Override
  public And And(ImmutableOpcodeReference source) {
    And ordinary = super.And(source);
    return rules.levelledAnd ? new Lower(ordinary.getTarget(), source, flag) : ordinary;
  }

  @Override
  public Or Or(ImmutableOpcodeReference source) {
    Or ordinary = super.Or(source);
    return rules.levelledOr ? new Higher(ordinary.getTarget(), source, flag) : ordinary;
  }

  @Override
  public Xor Xor(ImmutableOpcodeReference source) {
    Xor ordinary = super.Xor(source);
    if (!rules.levelledXor || source == ordinary.getTarget()) return ordinary;
    return new HigherStill(ordinary.getTarget(), source, flag);
  }

  private static final class Lower extends And {
    Lower(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
      super(target, source, flag);
    }

    @Override
    protected int doExecute(int sourceValue, int targetValue) {
      super.doExecute(sourceValue, targetValue);
      return Math.min(sourceValue, targetValue);
    }
  }

  private static final class Higher extends Or {
    Higher(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
      super(target, source, flag);
    }

    @Override
    protected int doExecute(int sourceValue, int targetValue) {
      super.doExecute(sourceValue, targetValue);
      return Math.max(sourceValue, targetValue);
    }
  }

  private static final class HigherStill extends Xor {
    HigherStill(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
      super(target, source, flag);
    }

    @Override
    protected int doExecute(int sourceValue, int targetValue) {
      super.doExecute(sourceValue, targetValue);
      return Math.max(sourceValue, targetValue);
    }
  }
}
