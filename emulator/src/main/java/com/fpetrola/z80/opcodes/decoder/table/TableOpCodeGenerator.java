/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.opcodes.decoder.table;

import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.fpetrola.z80.registers.Flags.*;
import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings("ALL")
public abstract class TableOpCodeGenerator<T> extends OpcodeTargets<T> {

  protected OpcodeConditions opc;

  protected abstract Instruction<T> getOpcode();

  protected OpcodeReference[] r;
  protected Register[] rp;
  protected OpcodeReference[] rp2;
  protected Condition[] cc;
  protected Instruction<T>[][] bli;
  protected List<Function<ImmutableOpcodeReference, Instruction<T>>> alu;
  protected List<RotFactory> rot;
  protected State s;
  protected int[] im;
  protected int x;
  protected int y;
  protected int z;
  protected int p;
  protected int q;
  protected RegisterName mainHigh8BitRegister;
  protected RegisterName mainLow8BitRegister;
  protected RegisterName main16BitRegister;
  DefaultInstructionFactory i;

  public TableOpCodeGenerator(State state, RegisterName main16BitRegister, RegisterName mainHigh8BitRegister, RegisterName mainLow8BitRegister, OpcodeReference main16BitRegisterReference, OpcodeConditions opcodeConditions, DefaultInstructionFactory instructionFactory) {
    super(state);
    this.i = instructionFactory;

    this.s = state;
    this.main16BitRegister = main16BitRegister;
    this.mainHigh8BitRegister = mainHigh8BitRegister;
    this.mainLow8BitRegister = mainLow8BitRegister;
    this.opc = opcodeConditions;

    r = new OpcodeReference[]{r(B), r(C), r(D), r(E), r(mainHigh8BitRegister), r(mainLow8BitRegister), main16BitRegisterReference, r(A)};
    rp = new Register[]{r(BC), r(DE), r(main16BitRegister), r(SP)};
    rp2 = new OpcodeReference[]{r(BC), r(DE), r(main16BitRegister), r(AF)};
    cc = new Condition[]{opc.nf(ZERO_FLAG), opc.f(ZERO_FLAG), opc.nf(CARRY_FLAG), opc.f(CARRY_FLAG), opc.nf(PARITY_FLAG), opc.f(PARITY_FLAG), opc.nf(SIGNIFICANT_FLAG), opc.f(SIGNIFICANT_FLAG)};
    im = new int[]{0, 0, 1, 2, 0, 0, 1, 2};
    createALUTable(state);
    createROTTable(state);
    createBLITable(state);
  }

  protected void createBLITable(State state) {
    bli = new Instruction[8][4];
    bli[4][0] = i.Ldi();
    bli[4][1] = i.Cpi();
    bli[4][2] = i.Ini();
    bli[4][3] = i.Outi();

    bli[5][0] = i.Ldd();
    bli[5][1] = i.Cpd();
    bli[5][2] = i.Ind();
    bli[5][3] = i.Outd();

    bli[6][0] = i.Ldir();
    bli[6][1] = i.Cpir();
    bli[6][2] = i.Inir();
    bli[6][3] = i.Outir();

    bli[7][0] = i.Lddr();
    bli[7][1] = i.Cpdr();
    bli[7][2] = i.Indr();
    bli[7][3] = i.Outdr();
  }

  protected void createALUTable(State state) {
    alu = new ArrayList<>();
    alu.add(r -> i.Add(r(A), r));
    alu.add(r -> i.Adc(r(A), r));
    alu.add(r -> i.Sub(r));
    alu.add(r -> i.Sbc(r(A), r));
    alu.add(r -> i.And(r));
    alu.add(r -> i.Xor(r));
    alu.add(r -> i.Or(r));
    alu.add(r -> i.Cp(r));
  }

  protected void createROTTable(State state) {
    rot = new ArrayList<>();
    rot.add((r, valueDelta) -> i.RLC(r));
    rot.add((r, valueDelta) -> i.RRC(r));
    rot.add((r, valueDelta) -> i.RL(r));
    rot.add((r, valueDelta) -> i.RR(r));
    rot.add((r, valueDelta) -> i.SLA(r));
    rot.add((r, valueDelta) -> i.SRA(r));
    rot.add((r, valueDelta) -> i.SLL(r));
    rot.add((r, valueDelta) -> i.SRL(r));
  }

  public Instruction[] getOpcodesTable() {
    Instruction[] opcodes = new Instruction[0x100];
    for (int i = 0; i < 0x100; i++) {
      x = i >> 6;
      y = (i & 0x38) >> 3;
      z = (i & 0x07);
      p = (i & 0x30) >> 4;
      q = (i & 0x08) >> 3;

      Instruction<T> opcode = getOpcode();
      opcodes[i] = opcode;
    }
    return opcodes;
  }

  protected Instruction<T>[] select(Instruction<T>... opcodes) {
    return opcodes;
  }

}