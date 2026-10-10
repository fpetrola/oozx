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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.bytecode.generators.helpers.Composed16BitRegisterVariable;
import com.fpetrola.z80.bytecode.generators.helpers.VariableDelegator;
import com.fpetrola.z80.bytecode.generators.helpers.WriteArrayVariable;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.minizx.StackException;
import com.fpetrola.z80.opcodes.references.ConditionFlag;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.StackAnalyzer;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public class InstructionsBytecodeGenerator implements InstructionVisitor<Object> {
  protected final MethodMaker methodMaker;
  protected final RoutineBytecodeGenerator routineByteCodeGenerator;
  private final int address;
  public boolean incPopsAdded;

  public InstructionsBytecodeGenerator(MethodMaker methodMaker, RoutineBytecodeGenerator routineByteCodeGenerator, int address) {
    this.methodMaker = methodMaker;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
    this.address = address;
  }

  @Override
  public void visitPush(Push push) {
    Register target = (Register) push.getTarget();
    if (routineByteCodeGenerator.plantedContinuation(address) == null)
      methodMaker.invoke("push", routineByteCodeGenerator.getExistingVariable(target).get());
  }

  @Override
  public void visitingPop(Pop pop) {
    Register target = (Register) pop.getTarget();
    routineByteCodeGenerator.getExistingVariable(target).set(methodMaker.invoke("pop"));
  }

  @Override
  public void visitEx(Ex ex) {
    Register source = (Register) ex.getSource();
    String sourceName = source.getName();
    if (ex.getTarget() instanceof Register target) {
      if (sourceName.startsWith("AF")) {
        exAF(target);
      } else if (!sourceName.startsWith("DE")) {
        methodMaker.invoke("exHLDE");
      }
    } else
      ex_iSP_Reg(ex, sourceName);
  }

  private void exAF(Register target) {
    Variable variable = routineByteCodeGenerator.variables.get("AF");
    invokeExAF(target, variable);
  }

  protected void invokeExAF(Register target, Variable variable) {
    Variable invoke = methodMaker.invoke("exAF", RoutineBytecodeGenerator.getRealVariable(variable));
  }

  private void ex_iSP_Reg(Ex ex, String name1) {
    OpcodeReferenceVisitor instructionVisitor = new OpcodeReferenceVisitor(true, routineByteCodeGenerator);
    ex.getTarget().accept(instructionVisitor);

    Object result = instructionVisitor.getResult();
    String name = name1;
    Variable variable = routineByteCodeGenerator.variables.get(name);
    Variable realVariable = RoutineBytecodeGenerator.getRealVariable(variable);
    Variable invoke = methodMaker.invoke("ex_iSP_REG", realVariable);
    variable.set(invoke);
  }

  @Override
  public boolean visitingBit(BIT bit) {
    bit.accept(new VariableHandlingInstructionVisitor((s, t) -> methodMaker.invoke("bit", bit.getN(), t.get()), routineByteCodeGenerator));
    return true;
  }

  @Override
  public boolean visitingSet(SET set) {
    set.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.or(1 << set.getN())), routineByteCodeGenerator));
    return true;
  }

  @Override
  public boolean visitingRes(RES res) {
    res.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.and(~(1 << res.getN()))), routineByteCodeGenerator));
    return true;
  }

  @Override
  public boolean visitingRlca(RLCA rlca) {
    unary(rlca, "rlca");
    return true;
  }


  @Override
  public boolean visitingRrca(RRCA rrca) {
    unary(rrca, "rrca");
    return true;
  }

  @Override
  public void visitOut(Out out) {
    Object portValue = valueOf(((Out.OutPortOpcodeReference) out.getTarget()).target);
    if (portValue instanceof java.lang.Integer low)
      portValue = routineByteCodeGenerator.variables.get("A").shl(8).or(low);
    methodMaker.invoke("out", portValue, valueOf(out.getSource()));
  }

  private Object valueOf(ImmutableOpcodeReference reference) {
    OpcodeReferenceVisitor visitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    reference.accept(visitor);
    return RoutineBytecodeGenerator.getRealVariable(visitor.getResult());
  }

  @Override
  public void visitIn(In in) {
    in.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Object realVariable = RoutineBytecodeGenerator.getRealVariable(s);
      String operation = realVariable instanceof java.lang.Integer ? "in" : "inC";
      if (realVariable instanceof java.lang.Integer integer)
        realVariable = routineByteCodeGenerator.variables.get("A").shl(8).or(integer);

      t.set(methodMaker.invoke(operation, realVariable, routineByteCodeGenerator.context.pc.read()));
    }, routineByteCodeGenerator));
  }

  @Override
  public boolean visitingRlc(RLC rlc) {
    unary(rlc, "rlc");
    return true;
  }

  @Override
  public boolean visitingSrl(SRL srl) {
    unary(srl, "srl");
    return true;
  }



  @Override
  public void visitingScf(SCF scf) {
    methodMaker.invoke("alu", "scf", routineByteCodeGenerator.variables.get("A").get());
  }

  @Override
  public boolean visitingRl(RL rl) {
    unary(rl, "rl");
    return true;
  }

  @Override
  public boolean visitingRla(RLA rla) {
    unary(rla, "rla");
    return true;
  }

  @Override
  public boolean visitRLD(RLD rld) {
    methodMaker.invoke("rld");
    return true;
  }

  @Override
  public boolean visitRRD(RRD rrd) {
    methodMaker.invoke("rrd");
    return true;
  }

  @Override
  public boolean visitingSll(SLL sll) {
    unary(sll, "sll");
    return true;
  }

  @Override
  public boolean visitingSla(SLA sla) {
    unary(sla, "sla");
    return true;
  }

  @Override
  public boolean visitingRr(RR rrc) {
    unary(rrc, "rr");
    return true;
  }

  @Override
  public boolean visitingRra(RRA rra) {
    unary(rra, "rra");
    return true;
  }

  @Override
  public boolean visitingRrc(RRC rrc) {
    unary(rrc, "rrc");
    return true;
  }

  @Override
  public boolean visitingSra(SRA sra) {
    unary(sra, "sra");
    return true;
  }

  @Override
  public boolean visitingBitOperation(BitOperation bit) {
    return false;
  }

  @Override
  public boolean visitingInc(Inc inc) {
    unary(inc, "inc");
    return true;
  }

  @Override
  public void visitingXor(Xor xor) {
    binary(xor, "xor");
  }


  @Override
  public boolean visitingCpl(CPL cpl) {
    unary(cpl, "cpl");
    return true;
  }

  @Override
  public void visitingOr(Or or) {
    binary(or, "or");
  }

  @Override
  public void visitingAnd(And and) {
    binary(and, "and");
  }

  @Override
  public boolean visitingAdd16(Add16 add16) {
    binary(add16, "add16");
    return true;
  }





  public void visitingInc16(Inc16 inc16) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
//      t.set(t.add(1).and(0xffff));
      t.set(methodMaker.invoke("inc16", t.get()));
    }, routineByteCodeGenerator);
    inc16.accept(visitor);
  }

  @Override
  public void visitingDec16(Dec16 dec16) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
//      t.set(t.sub(1).and(0xffff));
      t.set(methodMaker.invoke("dec16", t.get()));
    }, routineByteCodeGenerator);
    dec16.accept(visitor);
  }

  @Override
  public boolean visitingAdd(Add add) {
    binary(add, "add");
    return true;
  }

  @Override
  public void visitingAdc(Adc adc) {
    binary(adc, "adc");
  }


  @Override
  public void visitingSub(Sub sub) {
    binary(sub, "sub");
  }

  @Override
  public void visitingSbc(Sbc sbc) {
    binary(sbc, "sbc");
  }

  @Override
  public boolean visitingSbc16(Sbc16 sbc16) {
    binary(sbc16, "sbc16");
    return true;
  }


  @Override
  public boolean visitingAdc16(Adc16 adc16) {
    binary(adc16, "adc16");
    return true;
  }

  @Override
  public boolean visitingDec(Dec dec) {
    unary(dec, "dec");
    return true;
  }

  @Override
  public void visitingNeg(Neg neg) {
    unary(neg, "neg");
  }





  public void visitingLd(Ld ld) {
    if (routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer().stackSwitches.containsValue(routineByteCodeGenerator.context.pc.read()))
      methodMaker.invoke("leavingStack");
    if (ld.getSource() instanceof Register source && source.getName().equals(RegisterName.I.name())) {
      routineByteCodeGenerator.getExistingVariable("A").set(methodMaker.invoke("ldAI"));
      return;
    }
//    if (ld.getTarget() instanceof Register register && register.getName().equals("SP"))
//      return;
//
//    if (ld.getSource() instanceof Register register && register.getName().equals("SP"))
//      return;

    ld.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(s), routineByteCodeGenerator));
  }


  public void visitingCp(Cp cp) {
    cp.accept(new VariableHandlingInstructionVisitor((s, t) -> methodMaker.invoke("alu", "cp", t.get(), s), routineByteCodeGenerator));
  }

  public boolean visitingRet(Ret ret) {
    StackAnalyzer stackAnalyzer = routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer();
    int pcValue = routineByteCodeGenerator.context.pc.read();
    List<Integer> ownPushes = routineByteCodeGenerator.ownPushesConsumedAt(pcValue);
    Set<Integer> invocationsSet = stackAnalyzer.getInvocationsSet(pcValue);
    boolean returnsToACaller = stackAnalyzer.returnsConsumedBy.get(pcValue).stream().anyMatch(callSite -> callSite != -1);
    boolean consumesData = stackAnalyzer.dataConsumedBy.containsKey(pcValue) ? !ownPushes.isEmpty() && !returnsToACaller : !invocationsSet.isEmpty();
    Integer continuation = ownPushes.size() == 1 ? routineByteCodeGenerator.plantedContinuation(ownPushes.get(0)) : null;
    createIfs(ret, () -> {
      if (stackAnalyzer.stackSwitches.containsKey(pcValue))
        methodMaker.invoke("switchStack");
      else if (routineByteCodeGenerator.context.routineManager.nonLocalReturns.containsKey(pcValue))
        routineByteCodeGenerator.throwStackException(methodMaker.invoke("pop"), StackException.class);
      else if (continuation != null)
        routineByteCodeGenerator.invokeTransformedMethod(continuation);
      else if (consumesData) {
        Variable poppedValue = methodMaker.invoke("pop");
        if (!stackAnalyzer.shiftedReturns.containsKey(pcValue)) {
          invokeDynamicCall(invocationsSet, poppedValue);
          methodMaker.invoke("jump", poppedValue);
        }
      }
      routineByteCodeGenerator.returnFromMethod();
    });
    return true;
  }

  public boolean visitingCall(Call call) {
    int jumpLabel = call.getJumpAddress();
    StackAnalyzer stackAnalyzer = routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer();
    RegisterName trampoline = stackAnalyzer.trampolineRegister(jumpLabel);
    int callSite = routineByteCodeGenerator.context.pc.read();
    int returnAddress = routineByteCodeGenerator.context.routineManager.addressAfter(callSite);
    createIfs(call, () -> {
      boolean pushes = routineByteCodeGenerator.pushesReturnAddress(callSite);
      if (pushes)
        methodMaker.invoke("push", returnAddress);
      Variable pushedAt = pushes ? methodMaker.invoke("SP") : null;
      routineByteCodeGenerator.inPagedBank(jumpLabel, address -> {
        if (routineByteCodeGenerator.context.routineManager.findRoutineAt(address) != null) {
          routineByteCodeGenerator.invokeTransformedMethod(address);
          if (pushes)
            returnedFrom(pushedAt, callSite);
        }
        else if (trampoline != null)
          routineByteCodeGenerator.throughTrampoline(jumpLabel, trampoline, stackAnalyzer.calledThrough.get(callSite));
        else
          methodMaker.invoke("untranslated", jumpLabel);
      });
    });
    return true;
  }

  /** After a call that pushed its return address: still there, the callee returned normally; taken without a shifted return, the callee's RET went back to our caller. */
  private void returnedFrom(Variable pushedAt, int callSite) {
    Variable sp = methodMaker.invoke("SP");
    boolean shifted = routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer().callContinuations.containsKey(callSite);
    sp.ifEq(pushedAt, () -> methodMaker.invoke("pop"), () -> {
      if (!shifted)
        sp.ifEq(pushedAt.add(2).and(0xffff), routineByteCodeGenerator::returnFromMethod);
    });
  }

  public void visitingRst(RST rst) {
    if (routineByteCodeGenerator.context.routineManager.findRoutineAt(rst.getP()) != null)
      routineByteCodeGenerator.invokeTransformedMethod(rst.getP());
    else
      methodMaker.invoke("untranslated", rst.getP());
  }

  private void createIfs(Instruction instruction, Runnable runnable) {
    int[] cost = routineByteCodeGenerator.costOf(routineByteCodeGenerator.context.pc.read());
    Runnable taken = () -> {
      routineByteCodeGenerator.chargeTstates(cost[1] - cost[0]);
      runnable.run();
    };
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    if (instruction instanceof DJNZ djnz) {
      processDjnz(taken, djnz, opcodeReferenceVisitor);
    } else if (instruction instanceof ConditionalInstruction conditionalInstruction && conditionalInstruction.getCondition() instanceof ConditionFlag conditionFlag)
      processExistingCondition(taken, conditionalInstruction, conditionFlag, opcodeReferenceVisitor);
    else {
      runnable.run();
    }
  }

  private void processDjnz(Runnable runnable, DJNZ djnz, OpcodeReferenceVisitor opcodeReferenceVisitor) {
    Variable result = opcodeReferenceVisitor.process((Register) djnz.getCondition().getB());
    Variable and = result.sub(1).and(0xFF);
    result.set(and);
    result.ifNe(0, runnable);
  }

  private void processExistingCondition(Runnable runnable, ConditionalInstruction conditionalInstruction, ConditionFlag conditionFlag, OpcodeReferenceVisitor opcodeReferenceVisitor) {
    methodMaker.invoke("flag", conditionFlag.getFlag(), conditionFlag.isNegate()).ifTrue(runnable);
  }



  @Override
  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();

    int i = conditionalInstruction.getJumpAddress();
    Label label1 = routineByteCodeGenerator.getLabel(i);
    if (label1 != null)
      createIfs(conditionalInstruction, () -> label1.goto_());
    else {
//      byteCodeGenerator.getMethod(i);
//      createIfs(conditionalInstruction, () -> methodMaker.invoke(ByteCodeGenerator.createLabelName(i)));
      createIfs(conditionalInstruction, () -> {
        if (routineByteCodeGenerator.virtualPopOnBranch(address)) {
          routineByteCodeGenerator.throwAfterVirtualPop(address);
//          routineByteCodeGenerator.getField("nextAddress").set(nextAddress);
          incPopsAdded = true;
        } else
          routineByteCodeGenerator.tailJump(i, address);
        routineByteCodeGenerator.returnFromMethod();
      });
    }
  }

  public void visitingIm(IM im) {
    methodMaker.invoke("im", im.getMode());
  }

  public void visitingHalt(Halt halt) {
    methodMaker.invoke("halt", address);
  }

  public void visitEI(EI ei) {
    methodMaker.invoke("ei");
  }

  public void visitDI(DI di) {
    methodMaker.invoke("di");
  }

  public void visitExx(Exx exx) {
    methodMaker.invoke("exx");
  }

  public boolean visitLdi(Ldi tLdi) {
    methodMaker.invoke("ldi");
    return true;
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    ldOperation.getInstruction().accept(this);
    routineByteCodeGenerator.getExistingVariable((Register) ldOperation.getTarget()).set(valueOf(((TargetInstruction) ldOperation.getInstruction()).getTarget()));
    return true;
  }

  public boolean visitOuti(Outi outi) {
    methodMaker.invoke("outi");
    return true;
  }

  public boolean visitOutd(Outd outd) {
    methodMaker.invoke("outd");
    return true;
  }

  public boolean visitLdd(Ldd ldd) {
    methodMaker.invoke("ldd");
    return true;
  }

  public boolean visitCpd(Cpd cpd) {
    methodMaker.invoke("cpd");
    return true;
  }

  public boolean visitCpi(Cpi cpi) {
    methodMaker.invoke("cpi");
    return true;
  }

  @Override
  public boolean visitLdir(Ldir ldir) {
    callRepeatingInstruction(ldir);
    return false;
  }

  @Override
  public boolean visitLddr(Lddr lddr) {
    callRepeatingInstruction(lddr);
    return false;
  }

  @Override
  public boolean visitCpir(Cpir cpir) {
    callRepeatingInstruction(cpir);
    return false;
  }

  @Override
  public boolean visitCpdr(Cpdr cpdr) {
    callRepeatingInstruction(cpdr);
    return false;
  }

  private void callRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
    String methodName = repeatingInstruction.getClass().getSimpleName().toLowerCase();
    methodMaker.invoke(methodName, routineByteCodeGenerator.context.routineManager.originalAddress(address));
  }

  @Override
  public boolean visitingJP(JP jp) {
    StackAnalyzer stackAnalyzer = routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer();
    if (jp.getPositionOpcodeReference() instanceof Register register) {
      int pcValue1 = routineByteCodeGenerator.context.pc.read();
      Set<Integer> invocationsSet = stackAnalyzer.getInvocationsSet(pcValue1);
      Variable target = routineByteCodeGenerator.getExistingVariable(register);
      routineByteCodeGenerator.context.routineManager.nonLocalReturns.get(pcValue1).forEach(continuation -> target.ifEq(continuation, () -> routineByteCodeGenerator.throwStackException(continuation, StackException.class)));
      if (!invokeDynamicCall(invocationsSet, target)) {
        if (!stackAnalyzer.shiftedReturns.containsKey(pcValue1))
          methodMaker.invoke("jump", methodMaker.invoke(register.getName()));
        routineByteCodeGenerator.returnFromMethod();
      }
      return true;
    } else
      return false;
  }

  private boolean invokeDynamicCall(Set<Integer> invocationsSet, Variable existingVariable) {
    int pcValue1 = routineByteCodeGenerator.context.pc.read();
    boolean isSimulatedCall = routineByteCodeGenerator.context.symbolicExecutionAdapter.getStackAnalyzer().getSimulatedCallsPcs().contains(pcValue1) && !routineByteCodeGenerator.leavesAPlantedContinuation(pcValue1);
    invocationsSet.forEach(c -> {
      existingVariable.ifEq(c, () -> {
        Label label = routineByteCodeGenerator.getLabel(c);
        if (label != null) {
          if (isSimulatedCall)
            methodMaker.invoke("pop");
          methodMaker.goto_(label);
        } else if (isSimulatedCall) {
          Variable sp = methodMaker.invoke("SP");
          routineByteCodeGenerator.jumpInto(c);
          methodMaker.invoke("SP").ifEq(sp, () -> routineByteCodeGenerator.resumeAtPushed(pcValue1, methodMaker.invoke("pop")));
        } else
          routineByteCodeGenerator.tailJump(c, pcValue1);
      });
    });
    return isSimulatedCall;
  }

  public boolean visitLdAR(LdAR tLdAR) {
    routineByteCodeGenerator.getExistingVariable("A").set(methodMaker.invoke("ldAR"));
    return true;
  }

  @Override
  public void visitingCcf(CCF ccf) {
    methodMaker.invoke("alu", "ccf", routineByteCodeGenerator.variables.get("A").get());
  }

  @Override
  public boolean visitingDaa(DAA daa) {
    unary(daa, "daa");
    return true;
  }

  private void unary(Instruction instruction, String operation) {
    instruction.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(methodMaker.invoke("alu", operation, t.get())), routineByteCodeGenerator));
  }

  private void binary(Instruction instruction, String operation) {
    instruction.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(methodMaker.invoke("alu", operation, t.get(), s)), routineByteCodeGenerator));
  }
}
