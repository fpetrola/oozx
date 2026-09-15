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
import com.fpetrola.z80.instructions.impl.Add16;
import com.fpetrola.z80.instructions.impl.And;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

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

  LevelledInstructions(State state, Rules rules, State followed) {
    super(state);
    this.rules = rules;
    this.followed = followed;
  }

  @Override
  public Add16 Add16(OpcodeReference target, ImmutableOpcodeReference source) {
    Register into = machines(target), what = machines(source);
    return into == null || what == null ? super.Add16(target, source) : new AddedUp(target, source, flag, into, what, rules);
  }

  /** The machine's register of the same name, for a reference that is one. */
  private Register machines(Object reference) {
    return reference instanceof Register named ? followed.getRegister(RegisterName.valueOf(named.getName())) : null;
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

  /**
   * An address a follower worked out by adding: the sum is the one the machine is about to make
   * with its own two registers, because a register that carried a colour into the addition would
   * send this one to read and write where the machine never went. A pointer a follower was given
   * rather than added up is still its own, so a table indexed by a colour keeps working.
   */
  private static final class AddedUp extends Add16 {
    private final Register into, what;
    private final Rules rules;

    AddedUp(OpcodeReference target, ImmutableOpcodeReference source, Register flag, Register into, Register what, Rules rules) {
      super(target, source, flag);
      this.into = into;
      this.what = what;
      this.rules = rules;
    }

    @Override
    protected int doExecute(int sourceValue, int targetValue) {
      int sum = super.doExecute(sourceValue, targetValue);
      return rules.addressesAddedUpByTheMachine ? (into.read() + what.read()) & 0xffff : sum;
    }
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
