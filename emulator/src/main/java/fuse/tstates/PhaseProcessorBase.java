/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package fuse.tstates;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.ExecutionListener;
import fuse.tstates.Contention.Base;
import fuse.tstates.Contention.Kind;
import fuse.tstates.Contention.Moment;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.fpetrola.z80.registers.RegisterName.*;
import static fuse.tstates.Contention.Kind.*;
import static fuse.tstates.Contention.Moment.*;

/**
 * The contention of an instruction is a list of values, worked out once by visiting it, and
 * applied while that instruction executes: before it, after each of its reads, before each of
 * its writes, and after it. Nothing of it lives in the instruction.
 */
public abstract class PhaseProcessorBase implements InstructionVisitor<java.lang.Integer>, ExecutionListener {
  protected final State state;
  protected final Register registerI;
  protected final Register registerR;
  protected final Register registerIR;
  protected final Register registerSP;
  protected final Register registerPC;
  protected final Register registerDE;
  protected final Register registerBC;
  protected final Register registerHL;
  private final Map<Instruction, Contention[]> plans = new IdentityHashMap<>();
  private final List<Contention> planning = new ArrayList<>();
  private Contention[] current;
  private int reads;
  private int writes;
  private int lastAccess;

  public PhaseProcessorBase(State state) {
    this.state = state;
    registerI = getRegister(I);
    registerR = getRegister(R);
    registerIR = getRegister(IR);
    registerSP = getRegister(SP);
    registerPC = getRegister(PC);
    registerDE = getRegister(DE);
    registerBC = getRegister(BC);
    registerHL = getRegister(HL);
  }

  private Register getRegister(RegisterName registerName) {
    return state.getRegister(registerName);
  }

  public void beforeExecution(Instruction instruction) {
    current = plans.computeIfAbsent(instruction, this::plan);
    reads = 0;
    writes = 0;
    apply(BEFORE_EXECUTION, 0);
  }

  public void afterExecution(Instruction instruction) {
    boolean jumped = instruction instanceof JumpInstruction jump && jump.getNextPC() != -1;
    for (Contention contention : current)
      if (contention.moment() == AFTER_EXECUTION || contention.moment() == (jumped ? AFTER_EXECUTION_IF_JUMPED : AFTER_EXECUTION_IF_NOT_JUMPED))
        contend(contention);
    current = null;
  }

  public void afterRead(int address) {
    lastAccess = address;
    reads++;
    apply(AFTER_READ, reads);
  }

  public void beforeWrite(int address) {
    writes++;
    apply(BEFORE_WRITE, writes);
    lastAccess = address;
  }

  private Contention[] plan(Instruction instruction) {
    planning.clear();
    instruction.accept(this);
    return planning.toArray(new Contention[0]);
  }

  private void apply(Moment moment, int ordinal) {
    if (current != null)
      for (Contention contention : current)
        if (contention.at(moment, ordinal))
          contend(contention);
  }

  private void contend(Contention contention) {
    int base = switch (contention.base()) {
      case IR -> registerIR.read();
      case PC -> registerPC.read();
      case HL -> registerHL.read();
      case BC -> registerBC.read();
      case DE -> registerDE.read();
      case SP -> registerSP.read();
      case LAST_ACCESS -> lastAccess;
    };
    contend((base + contention.delta()) & 0xFFFF, contention.times(), contention.tstates(), contention.kind());
  }

  public abstract void contend(int address, int times, int tstates, Kind kind);

  protected void before(int times, Base base) {
    add(BEFORE_EXECUTION, Contention.ANY, base, 0, times, 1, READ_NO_MREQ);
  }

  protected void before(int times, Base base, int delta) {
    add(BEFORE_EXECUTION, Contention.ANY, base, delta, times, 1, READ_NO_MREQ);
  }

  protected void after(int times, Base base, int delta, Kind kind) {
    add(AFTER_EXECUTION, Contention.ANY, base, delta, times, 1, kind);
  }

  protected void ifJumped(int times, Base base, int delta, Kind kind) {
    add(AFTER_EXECUTION_IF_JUMPED, Contention.ANY, base, delta, times, 1, kind);
  }

  protected void ifNotJumped(int times, Base base, int delta, int tstates, Kind kind) {
    add(AFTER_EXECUTION_IF_NOT_JUMPED, Contention.ANY, base, delta, times, tstates, kind);
  }

  protected void onRead(int ordinal, int times, Base base) {
    add(AFTER_READ, ordinal, base, 0, times, 1, READ_NO_MREQ);
  }

  protected void onRead(int ordinal, int times, Base base, int delta) {
    add(AFTER_READ, ordinal, base, delta, times, 1, READ_NO_MREQ);
  }

  protected void onWrite(int ordinal, int times, Base base, int delta) {
    add(BEFORE_WRITE, ordinal, base, delta, times, 1, READ_NO_MREQ);
  }

  private void add(Moment moment, int ordinal, Base base, int delta, int times, int tstates, Kind kind) {
    planning.add(new Contention(moment, ordinal, base, delta, times, tstates, kind));
  }
}
