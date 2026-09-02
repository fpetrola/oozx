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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import static fuse.tstates.Contention.ANY;
import static fuse.tstates.Contention.Base.*;
import static fuse.tstates.Contention.Kind.*;

/**
 * Which contention each shape of instruction has, as Fuse has it. The only knowledge of the
 * Spectrum's timing in the emulator; the Z80 model does not know this class exists.
 */
public abstract class PhaseProcessor extends PhaseProcessorBase {
  public PhaseProcessor(State state) {
    super(state);
  }

  private boolean isImmediate(ImmutableOpcodeReference reference) {
    return reference instanceof Memory8BitReference;
  }

  private boolean isIndexed(OpcodeReferenceBase reference) {
    return reference instanceof MemoryPlusRegister8BitReference;
  }

  private boolean isLdSP(Ld ld) {
    return ld.getTarget().equals(registerSP) && ld.getSource() instanceof Register;
  }

  private boolean isIndirectHL(TargetInstruction instruction) {
    return instruction.getTarget() instanceof IndirectMemory8BitReference indirect && indirect.getTarget() instanceof Register register && register.getName().equals(RegisterName.HL.name());
  }

  public void visitingRst(RST rst) {
    before(1, IR);
  }

  public boolean visitingRet(Ret ret) {
    if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
      before(1, IR);
    return false;
  }

  public boolean visiting16BitsOperation(Binary16BitsOperation operation) {
    before(7, IR);
    return true;
  }

  public void visitingInc16(Inc16 inc16) {
    after(2, IR, 0, READ_NO_MREQ);
  }

  public void visitingDec16(Dec16 dec16) {
    after(2, IR, 0, READ_NO_MREQ);
  }

  public void visitPush(Push push) {
    before(1, IR);
  }

  public void visitingLd(Ld ld) {
    if (isLdSP(ld))
      before(2, IR);
    else if (ld.getTarget().equals(registerI) || ld.getTarget().equals(registerR) || ld instanceof LdAI || ld instanceof LdAR)
      before(1, IR);
    if (isIndexed(ld.getTarget()) && isImmediate(ld.getSource()))
      onWrite(ANY, 2, PC, 3);
    if (!isImmediate(ld.getSource()) && (isIndexed(ld.getSource()) || isIndexed(ld.getTarget())))
      onRead(1, 5, IR);
  }

  public void visitingJR(JR jr) {
    relativeJump();
  }

  public boolean visitingDjnz(DJNZ djnz) {
    before(1, IR);
    relativeJump();
    return false;
  }

  /** Not taken, the displacement is contended for the three T-states of a read but not read. */
  private void relativeJump() {
    ifJumped(5, PC, 1, READ_NO_MREQ);
    ifNotJumped(1, PC, 1, 3, READ);
  }

  public void visitEx(Ex ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference) {
      after(2, SP, 0, WRITE_NO_MREQ);
      onWrite(1, 1, SP, 1);
    }
  }

  public boolean visitOutir(Outir outir) {
    before(1, IR);
    ifJumped(5, BC, 0, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitOutdr(Outdr outdr) {
    before(1, IR);
    ifJumped(5, BC, 0, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitInir(Inir inir) {
    before(1, IR);
    ifJumped(5, HL, -1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitIndr(Indr indr) {
    before(1, IR);
    ifJumped(5, HL, 1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitCpir(Cpir cpir) {
    after(5, HL, -1, WRITE_NO_MREQ);
    ifJumped(5, HL, -1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitCpdr(Cpdr cpdr) {
    after(5, HL, 1, WRITE_NO_MREQ);
    ifJumped(5, HL, 1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitLdir(Ldir ldir) {
    after(2, DE, -1, WRITE_NO_MREQ);
    ifJumped(5, DE, -1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitLddr(Lddr lddr) {
    after(2, DE, 1, WRITE_NO_MREQ);
    ifJumped(5, DE, 1, WRITE_NO_MREQ);
    return true;
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    before(1, IR);
  }

  public boolean visitLdi(Ldi ldi) {
    after(2, DE, -1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitLdd(Ldd ldd) {
    after(2, DE, 1, WRITE_NO_MREQ);
    return true;
  }

  public boolean visitCpi(Cpi cpi) {
    after(5, HL, -1, READ_NO_MREQ);
    return true;
  }

  public boolean visitCpd(Cpd cpd) {
    after(5, HL, 1, READ_NO_MREQ);
    return true;
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    indexedOrIndirectHL((TargetInstruction) ldOperation.getInstruction());
    return true;
  }

  public boolean visitingBitOperation(BitOperation bitOperation) {
    indexedOrIndirectHL(bitOperation);
    return true;
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction instruction) {
    indexedOrIndirectHL(instruction);
    return true;
  }

  private void indexedOrIndirectHL(TargetInstruction instruction) {
    if (isIndexed(instruction.getTarget())) {
      before(2, PC, 3);
      onRead(1, 1, LAST_ACCESS);
    }
    if (isIndirectHL(instruction))
      onRead(1, 1, HL);
  }

  public boolean visitingInc(Inc inc) {
    incDec(inc);
    return true;
  }

  public boolean visitingDec(Dec dec) {
    incDec(dec);
    return true;
  }

  private void incDec(TargetInstruction instruction) {
    if (isIndexed(instruction.getTarget())) {
      onRead(1, 5, PC, 2);
      onRead(2, 1, LAST_ACCESS);
    }
    if (isIndirectHL(instruction))
      onWrite(ANY, 1, HL, 0);
  }

  public boolean visitRLD(RLD rld) {
    onRead(ANY, 4, HL);
    return false;
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction instruction) {
    if (isIndexed(instruction.getSource()))
      onRead(1, 5, PC, 2);
  }

  public boolean visitingCall(Call call) {
    onWrite(1, 1, PC, 2);
    return false;
  }
}
