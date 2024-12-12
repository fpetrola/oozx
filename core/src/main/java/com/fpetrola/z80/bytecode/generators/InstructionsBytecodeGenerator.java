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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.DefaultBlock;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.bytecode.generators.helpers.BytecodeGenerationContext;
import com.fpetrola.z80.bytecode.generators.helpers.PendingFlagUpdate;
import com.fpetrola.z80.bytecode.generators.helpers.SmartComposed16BitRegisterVariable;
import com.fpetrola.z80.bytecode.generators.helpers.WriteArrayVariable;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.ConditionFlag;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineVisitor;
import com.fpetrola.z80.se.DynamicJPData;
import com.fpetrola.z80.se.SEInstructionFactory;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public class InstructionsBytecodeGenerator<T extends WordNumber> implements InstructionVisitor<T, T> {
  private final MethodMaker methodMaker;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;
  private final int address;
  public PendingFlagUpdate pendingFlag;
  public PendingFlagUpdate previousPendingFlag;
  public boolean incPopsAdded;

  public InstructionsBytecodeGenerator(MethodMaker methodMaker, int label, RoutineBytecodeGenerator routineByteCodeGenerator, int address, PendingFlagUpdate previousPendingFlag) {
    this.methodMaker = methodMaker;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
    this.address = address;
    this.previousPendingFlag = previousPendingFlag;
  }

  @Override
  public void visitPush(Push push) {
    Register<T> target = (Register<T>) push.getTarget();
    // VirtualRegister top = byteCodeGenerator.getTop(target);
    //Variable var = methodMaker.var(int.class);
    //var.name("last_" + top.getName());
    //var.set(byteCodeGenerator.getExistingVariable(target).get());
    methodMaker.invoke("push", routineByteCodeGenerator.getExistingVariable2(target).get());
  }

  @Override
  public void visitingPop(Pop pop) {
    Register<T> target = (Register<T>) pop.getTarget();
//    VirtualRegister top = byteCodeGenerator.getTop(target);
//    Variable var = methodMaker.var(int.class);
//    var.name("last_" + top.getName());
//    byteCodeGenerator.getExistingVariable(target).get().set(var);
    routineByteCodeGenerator.getExistingVariable2(target).set(methodMaker.invoke("pop"));

//    if (pop instanceof SymbolicExecutionAdapter.PopReturnAddress popReturnAddress) {
//      ReturnAddressWordNumber returnAddress = popReturnAddress.getReturnAddress();
//      if (returnAddress != null) {
//        methodMaker.invoke("incPops");
//      }
//    }
  }

  @Override
  public void visitEx(Ex ex) {
    Register<T> source = (Register<T>) ex.getSource();
    Register<T> target = (Register<T>) ex.getTarget();
    // VirtualRegister top = byteCodeGenerator.getTop(target);
    //Variable var = methodMaker.var(int.class);
    //var.name("last_" + top.getName());
    //var.set(byteCodeGenerator.getExistingVariable(target).get());
    if (routineByteCodeGenerator.getTop2(source).getName().startsWith("AF")) {
      Variable variable = routineByteCodeGenerator.variables.get("AF");
      if (routineByteCodeGenerator.bytecodeGenerationContext.useFields) {
        Variable invoke = methodMaker.invoke("exAF", RoutineBytecodeGenerator.getRealVariable(variable));
      } else if (variable instanceof SmartComposed16BitRegisterVariable existingVariable) {
        existingVariable.setRegister(target);
//      Variable existingVariable = byteCodeGenerator.getExistingVariable("AF");
        Variable invoke = methodMaker.invoke("exAF", RoutineBytecodeGenerator.getRealVariable(existingVariable));
        existingVariable.set(invoke);
      }

//      Single8BitRegisterVariable variableA = (Single8BitRegisterVariable) byteCodeGenerator.getExistingVariable("A");
//      variableA.directSet();

    } else
      methodMaker.invoke("exHLDE");
  }

  @Override
  public boolean visitingBit(BIT bit) {
    bit.accept(new VariableHandlingInstructionVisitor((s, t) -> processFlag(bit, () -> t.and(1 << bit.getN())), routineByteCodeGenerator));
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
    Instruction rlca1 = rlca;
    invokeRotationInstruction(rlca1, "rlc");
    return true;
  }

  private void invokeRlc(Variable t, Variable variable, String functionName) {
    Variable f = getF();
    Variable invoke = methodMaker.invoke(functionName, variable, f);
    t.set(invoke.aget(0));
    f.set(invoke.aget(1));
  }

  @Override
  public boolean visitingRrca(RRCA rrca) {
    rrca.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      if (routineByteCodeGenerator.bytecodeGenerationContext.useFields)
        t.set(methodMaker.invoke("rrc", t.get()));
      else
        t.set(methodMaker.invoke("rrc", t.get()));
    }, routineByteCodeGenerator));
    return true;
  }

  @Override
  public void visitIn(In in) {
    in.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Object realVariable = RoutineBytecodeGenerator.getRealVariable(s);
      if (realVariable instanceof Integer integer)
        realVariable = routineByteCodeGenerator.variables.get("A").shl(8).or(integer);

      t.set(methodMaker.invoke("in", realVariable, routineByteCodeGenerator.bytecodeGenerationContext.pc.read().intValue()));
    }, routineByteCodeGenerator));
  }

  @Override
  public boolean visitingRlc(RLC rlc) {
    invokeRotationInstruction(rlc, "rlc");
    return true;
  }

  @Override
  public boolean visitingSrl(SRL srl) {
    invokeRotationInstruction(srl, "sr");
    return true;
  }

  private void invokeRotationInstruction(Instruction instruction, String name) {
    instruction.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      if (routineByteCodeGenerator.bytecodeGenerationContext.useFields)
        t.set(methodMaker.invoke(name, t.get()));
      else
        invokeRlc(t, t.get(), name);
    }, routineByteCodeGenerator));
  }

  @Override
  public void visitingScf(SCF scf) {
    Supplier<Variable> f = () -> routineByteCodeGenerator.getField("F").or(1);
    processFlag(scf, f);
  }

  @Override
  public boolean visitingRl(RL rl) {
    if (previousPendingFlag != null)
      previousPendingFlag.update(true);

    invokeRotationInstruction(rl, "rl");
    return true;
  }

  @Override
  public boolean visitingRla(RLA rla) {
    if (previousPendingFlag != null)
      previousPendingFlag.update(true);

    invokeRotationInstruction(rla, "rl");
    return true;
  }

  @Override
  public boolean visitRLD(RLD rld) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean visitingSll(SLL sll) {
    if (previousPendingFlag != null)
      previousPendingFlag.update(true);

    invokeRotationInstruction(sll, "sl");
    return true;
  }

  @Override
  public boolean visitingSla(SLA sla) {
    if (previousPendingFlag != null)
      previousPendingFlag.update(true);

    invokeRotationInstruction(sla, "sl");
    return true;
  }

  @Override
  public boolean visitingRr(RR rrc) {
    rrc.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Variable variable = t.get();
      if (variable != null)
        t.set(methodMaker.invoke("rr", variable));
    }, routineByteCodeGenerator));
    return true;
  }

  @Override
  public boolean visitingRra(RRA rra) {
    rra.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Variable variable = t.get();
      processFlag(rra, () -> getF());
      if (variable != null)
        t.set(methodMaker.invoke("rr", variable));
    }, routineByteCodeGenerator));

    return true;
  }

  @Override
  public boolean visitingRrc(RRC rrc) {
    rrc.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Variable variable = t.get();
      if (variable != null)
        t.set(methodMaker.invoke("rrc", variable));
    }, routineByteCodeGenerator));
    return true;
  }

  @Override
  public boolean visitingSra(SRA sra) {
    sra.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      t.set(t.shr(1).or(t.and(0x80)));
    }, routineByteCodeGenerator));
    return true;
  }

  @Override
  public void visitingBitOperation(BitOperation bit) {
//    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.and(bit.getN())), byteCodeGenerator);
//    bit.accept(visitor);
//    processFlag(bit, visitor);

    if (bit instanceof BIT<?>) {
      OpcodeReferenceVisitor instructionVisitor2 = new OpcodeReferenceVisitor(true, routineByteCodeGenerator);
      bit.getFlag().accept(instructionVisitor2);
      Variable flag = (Variable) instructionVisitor2.getResult();
      bit.accept(new VariableHandlingInstructionVisitor((s, t) -> flag.set(t.and(1 << bit.getN())), routineByteCodeGenerator));
    }
//    tBitOperation.accept(new VariableHandlingInstructionVisitor((s, t) -> t.and(tBitOperation.getN()), byteCodeGenerator));
  }

  public boolean visitingInc(Inc inc) {
    final Variable[] and = new Variable[1];
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
      and[0] = t.add(1).and(0xff);
      t.set(and[0]);
    }, routineByteCodeGenerator);
    inc.accept(visitor);
    processFlag(inc, () -> and[0]);
    return false;
  }

  public void visitingXor(Xor xor) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> xorAndSet(s, t), routineByteCodeGenerator);
    xor.accept(visitor);
    processFlag(xor, () -> visitor.targetVariable.shl(1));
  }

  public boolean visitingCpl(CPL cpl) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.com()), routineByteCodeGenerator);
    cpl.accept(visitor);
    processFlag(cpl, () -> visitor.targetVariable);
    return false;
  }

  @Override
  public void visitingOr(Or or) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> orAndSet(s, t), routineByteCodeGenerator);
    or.accept(visitor);
    processFlag(or, () -> visitor.targetVariable.shl(1));
  }

  @Override
  public void visitingAnd(And and) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> andAndSet(s, t), routineByteCodeGenerator);
    and.accept(visitor);
    processFlag(and, () -> visitor.targetVariable.shl(1));
  }

  public boolean visitingAdd16(Add16 add16) {
    add16.accept(new VariableHandlingInstructionVisitor((s, t) -> getSet(s, t, 0xffff), routineByteCodeGenerator));
    return false;
  }

  private void getSet(Object s, Variable t, int mask) {
    if (s != null && t != null) {
      Variable value = t == s ? t.mul(2) : t.add(s);
      t.set(value.and(mask));
    }
  }

  private void orAndSet(Object s, Variable t) {
    if (RoutineBytecodeGenerator.getRealVariable(s) != RoutineBytecodeGenerator.getRealVariable(t)) {
      t.set(t.or(s));
    }
  }

  private void andAndSet(Object s, Variable t) {
    if (RoutineBytecodeGenerator.getRealVariable(s) != RoutineBytecodeGenerator.getRealVariable(t)) {
      t.set(t.and(s));
    }
  }

  private void xorAndSet(Object s, Variable t) {
    if (RoutineBytecodeGenerator.getRealVariable(s) != RoutineBytecodeGenerator.getRealVariable(t)) {
      t.set(t.xor(s));
    } else
      t.set(0);
  }

  public void visitingInc16(Inc16 inc16) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.add(1).and(0xffff)), routineByteCodeGenerator);
    inc16.accept(visitor);
  }

  @Override
  public void visitingDec16(Dec16 dec16) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.sub(1).and(0xffff)), routineByteCodeGenerator);
    dec16.accept(visitor);
  }

  public boolean visitingAdd(Add add) {
    Variable[] add1 = new Variable[1];
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
      add1[0] = t.add(s);
      t.set(add1[0].and(0xFF));
    }, routineByteCodeGenerator);
    add.accept(visitor);
    processFlag(add, () -> add1[0]);
    return false;
  }

  @Override
  public void visitingAdc(Adc adc) { //TODO: revisar
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.add(s).add(methodMaker.invoke("carry", getF()).and(255))), routineByteCodeGenerator);
    adc.accept(visitor);
    processFlag(adc, () -> visitor.targetVariable);
  }

  private Variable getF() {
    return routineByteCodeGenerator.getExistingVariable("F");
  }

  public void visitingSub(Sub sub) {
    Variable[] sub1 = new Variable[1];
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
      sub1[0] = t.sub(s);
      t.set(sub1[0].and(0xFF));
    }, routineByteCodeGenerator);
    sub.accept(visitor);
    processFlag(sub, () -> sub1[0]);
  }

  @Override
  public void visitingSbc(Sbc sbc) { //TODO: revisar
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.sub(s).and(0xff)), routineByteCodeGenerator);
    sbc.accept(visitor);
    processFlag(sbc, () -> visitor.targetVariable);
  }

  @Override
  public boolean visitingSbc16(Sbc16 sbc16) {
    sbc16.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.sub(s).and(0xffff)), routineByteCodeGenerator));
    return false;
  }


  @Override
  public boolean visitingAdc16(Adc16 adc16) {
    adc16.accept(new VariableHandlingInstructionVisitor((s, t) -> getSet(s, t, 0xffff), routineByteCodeGenerator));
    return false;
  }

  public boolean visitingDec(Dec dec) {
    final Variable[] and = new Variable[1];
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
      and[0] = t.sub(1).and(0xff);
      t.set(and[0]);
    }, routineByteCodeGenerator);
    dec.accept(visitor);
    processFlag(dec, () -> and[0]);
    return false;
  }

  @Override
  public void visitingNeg(Neg neg) {
    neg.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.neg().and(0xff)), routineByteCodeGenerator));
  }

  private void processFlag(DefaultTargetFlagInstruction targetFlagInstruction, Supplier<Variable> targetVariable) {
    Variable value = targetVariable.get();
    setFlagPreservingCarry(value);

    routineByteCodeGenerator.lastTargetFlagInstruction = targetFlagInstruction;

//    pendingFlag = new PendingFlagUpdate(targetVariable, targetFlagInstruction, routineByteCodeGenerator, address);
  }

  private void setFlagPreservingCarry(Variable value) {
    getF().set(processWriteArray(value));
  }

  private Variable processWriteArray(Variable value) {
    if (value instanceof WriteArrayVariable)
      value = value.get();
    return value;
  }

  private void processFlag(DefaultTargetFlagInstruction targetFlagInstruction, Supplier<Variable> targetVariable, Supplier<Object> sourceVariable) {
    Variable value = targetVariable.get();
    setFlagPreservingCarry(value);
    routineByteCodeGenerator.lastTargetFlagInstruction = targetFlagInstruction;

//    pendingFlag = new PendingFlagUpdate(targetVariable, targetFlagInstruction, routineByteCodeGenerator, address, sourceVariable);
  }

  public void visitingLd(Ld ld) {
    ld.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(s), routineByteCodeGenerator));
  }


  public void visitingCp(Cp cp) {
    cp.accept(new VariableHandlingInstructionVisitor((s, t) -> processFlag(cp, () -> t.sub(s), () -> s), routineByteCodeGenerator));
  }

  public boolean visitingRet(Ret ret) {
    createIfs(ret, () -> routineByteCodeGenerator.returnFromMethod());
    return true;
  }

  public boolean visitingCall(Call call) {
    int jumpLabel = call.getJumpAddress().intValue();
    if (routineByteCodeGenerator.getMethod(jumpLabel) != null)
      createIfs(call, () -> {
        routineByteCodeGenerator.invokeTransformedMethod(jumpLabel);
        List<Integer> i = routineByteCodeGenerator.routine.getReturnPoints().get(address).stream().toList();
        i.forEach(ga -> {
          Variable isNextPC = methodMaker.invoke("isNextPC", ga);
          isNextPC.ifTrue(() -> {
            Label label1 = routineByteCodeGenerator.getLabel(ga);
            if (label1 != null) {
              label1.goto_();
            } else {
              Variable nextAddress = routineByteCodeGenerator.getField("nextAddress");
              nextAddress.set(ga + 1);
              routineByteCodeGenerator.returnFromMethod();
            }
          });
        });
      });

    return true;
  }

  private void createIfs(Instruction instruction, Runnable runnable) {
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    if (instruction instanceof DJNZ<?> djnz) {
      processDjnz(runnable, djnz, opcodeReferenceVisitor);
    } else if (instruction instanceof ConditionalInstruction conditionalInstruction && conditionalInstruction.getCondition() instanceof ConditionFlag conditionFlag)
      processExistingCondition(runnable, conditionalInstruction, conditionFlag, opcodeReferenceVisitor);
    else {
      if (previousPendingFlag != null) {
        previousPendingFlag.update(false);
      }
      runnable.run();
    }
  }

  private void processDjnz(Runnable runnable, DJNZ<?> djnz, OpcodeReferenceVisitor opcodeReferenceVisitor) {
    Variable result = opcodeReferenceVisitor.process((Register) djnz.getCondition().getB());
    Variable and = result.sub(1).and(0xFF);
    result.set(and);
    result.ifNe(0, runnable);
  }

  private void processExistingCondition(Runnable runnable, ConditionalInstruction conditionalInstruction, ConditionFlag conditionFlag, OpcodeReferenceVisitor opcodeReferenceVisitor) {
    if (routineByteCodeGenerator.bytecodeGenerationContext.pc.read().intValue() == 0xD9AC)
      System.out.println("break");
    Variable f = opcodeReferenceVisitor.process((Register) conditionFlag.getRegister());
    String string = conditionalInstruction.getCondition().toString();
    Object source;
    Variable targetVariable = null;
    if (previousPendingFlag != null) {
      FlagInstruction targetFlagInstruction = previousPendingFlag.targetFlagInstruction;
      routineByteCodeGenerator.lastMemPc.write(WordNumber.createValue(previousPendingFlag.address));

//        if (routineByteCodeGenerator.bytecodeGenerationContext.pc.read().intValue() == 38370)
//          System.out.println("sdsdg");
//        routineByteCodeGenerator.lastMemPc.write(WordNumber.createValue(previousPendingFlag.address));
//        if (targetFlagInstruction instanceof TargetInstruction<?> cp1) {
//          if (cp1.getTarget().read() instanceof DirectAccessWordNumber && (instruction instanceof JR || instruction instanceof JP)) {
//            createIfMethod(instruction, conditionalInstruction);
//          }
//        }

      if (targetFlagInstruction instanceof Cp<?> cp) {
        ImmutableOpcodeReference<WordNumber> source1 = (ImmutableOpcodeReference<WordNumber>) cp.getSource();
        if (previousPendingFlag.sourceVariableSupplier == null) {
          OpcodeReferenceVisitor opcodeReferenceVisitor2 = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
          source1.accept(opcodeReferenceVisitor2);
          source = opcodeReferenceVisitor2.getResult();
        } else {
          source = previousPendingFlag.sourceVariableSupplier.get();
        }
        if (targetFlagInstruction instanceof TargetInstruction<?> targetInstruction) {
          OpcodeReferenceVisitor<WordNumber> variableAdapter = new OpcodeReferenceVisitor<>(true, routineByteCodeGenerator);
          targetInstruction.getTarget().accept(variableAdapter);
          targetVariable = (Variable) variableAdapter.getResult();
        }
        previousPendingFlag.processed = true;
        pendingFlag = previousPendingFlag;
      } else {
        targetVariable = (Variable) previousPendingFlag.targetVariableSupplier.get();
        source = 0;
        if (isRotationInstruction(targetFlagInstruction))
          if (targetVariable != null) {
            if (string.equals("NZ")) targetVariable.ifNe(source, runnable);
            else if (string.equals("Z")) targetVariable.ifEq(source, runnable);
            else if (string.equals("NC")) targetVariable.ifEq(source, runnable);
            else if (string.equals("C")) targetVariable.ifNe(source, runnable);
            else if (string.equals("NS")) targetVariable.ifGe(source, runnable);
            else if (string.equals("S")) targetVariable.ifLt(source, runnable);
            return;
          }
      }
    } else {
      source = 0;
      targetVariable = f;
    }
    if (targetVariable != null) {
      if (isRotationInstruction(routineByteCodeGenerator.lastTargetFlagInstruction))
        if (targetVariable != null) {
          Variable invoke = methodMaker.invoke("getCarry");
          if (string.equals("NZ")) invoke.ifNe(source, runnable);
          else if (string.equals("Z")) invoke.ifEq(source, runnable);
          else if (string.equals("NC")) invoke.ifEq(source, runnable);
          else if (string.equals("C")) invoke.ifNe(source, runnable);
          else if (string.equals("NS")) invoke.ifGe(source, runnable);
          else if (string.equals("S")) invoke.ifLt(source, runnable);
          return;
        }

      executeCondition(runnable, string, targetVariable, source);
    }
  }

  private boolean isRotationInstruction(FlagInstruction targetFlagInstruction) {
    return targetFlagInstruction instanceof RR ||
        targetFlagInstruction instanceof RRA ||
        targetFlagInstruction instanceof RRC ||
        targetFlagInstruction instanceof RRCA ||
        targetFlagInstruction instanceof RL ||
        targetFlagInstruction instanceof RLA ||
        targetFlagInstruction instanceof RLC ||
        targetFlagInstruction instanceof RLCA ||
        targetFlagInstruction instanceof SLA ||
        targetFlagInstruction instanceof SLL ||
        targetFlagInstruction instanceof SRA ||
        targetFlagInstruction instanceof SRL;
  }

  private void createIfMethod(Instruction instruction, ConditionalInstruction conditionalInstruction) {
    BytecodeGenerationContext bytecodeGenerationContext = routineByteCodeGenerator.bytecodeGenerationContext;
    BlocksManager blocksManager = bytecodeGenerationContext.routineManager.blocksManager;
    int startAddress = bytecodeGenerationContext.pc.read().intValue() + instruction.getLength();
    int endAddress = conditionalInstruction.getJumpAddress().intValue() - 1;
    if (startAddress < endAddress) {
      int i = bytecodeGenerationContext.pc.read().intValue();
      Routine routine = new Routine(new DefaultBlock(startAddress, endAddress, new BlocksManager(new NullBlockChangesListener(), true)), startAddress, true);
      routine.setRoutineManager(bytecodeGenerationContext.routineManager);
      final boolean[] notContained = new boolean[1];

      routine.accept(new RoutineVisitor<Object>() {
        public void visitInstruction(int address, Instruction instruction) {
          if (instruction instanceof ConditionalInstruction<?, ?> conditionalInstruction1) {
            if (!routine.contains(conditionalInstruction1.getJumpAddress().intValue())) {
              notContained[0] |= true;
            }
          }
        }
      });

      if (true || !notContained[0]) {
        Routine innerRoutineBetween = routineByteCodeGenerator.routine.createInnerRoutineBetween(startAddress, endAddress);
        if (innerRoutineBetween != null) {
          innerRoutineBetween.setCallable(false);
          routineByteCodeGenerator.findOrCreateMethodAt(startAddress);
          RoutineBytecodeGenerator innerRoutineBytecodeGenerator = new RoutineBytecodeGenerator(bytecodeGenerationContext, innerRoutineBetween);
          innerRoutineBytecodeGenerator.generate();
//        String methodName = routineBytecodeGenerator.createLabelName(address);
//        MethodMaker methodMaker = bytecodeGenerationContext.methods.get(methodName);
//        if (methodMaker == null) {
//          if (startAddress == 38115)
//            System.out.println("");
//        }
        }
        bytecodeGenerationContext.pc.write(WordNumber.createValue(i));
      }
    }
  }

  private void executeCondition(Runnable runnable, String conditionString, Variable target, Object source) {
    if (conditionString.equals("NZ")) target.ifNe(source, runnable);
    else if (conditionString.equals("Z")) target.ifEq(source, runnable);
    else if (conditionString.equals("NC")) target.ifGe(source, runnable);
    else if (conditionString.equals("C")) target.ifLt(source, runnable);
    else if (conditionString.equals("NS")) target.ifGe(source, runnable);
    else if (conditionString.equals("S")) target.ifLt(source, runnable);
  }

  @Override
  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();

    int i = conditionalInstruction.getJumpAddress().intValue();
    Label label1 = routineByteCodeGenerator.getLabel(i);
    if (label1 != null)
      createIfs(conditionalInstruction, () -> label1.goto_());
    else {
//      byteCodeGenerator.getMethod(i);
//      createIfs(conditionalInstruction, () -> methodMaker.invoke(ByteCodeGenerator.createLabelName(i)));
      createIfs(conditionalInstruction, () -> {
        if (routineByteCodeGenerator.routine.getVirtualPop().containsKey(address)) {
          routineByteCodeGenerator.getField("nextAddress").set(routineByteCodeGenerator.routine.getVirtualPop().get(address) + 1);
          incPopsAdded = true;
        } else {
          routineByteCodeGenerator.invokeTransformedMethod(i);
          methodMaker.return_();
        }
        routineByteCodeGenerator.returnFromMethod();
      });
    }
  }

  public void visitExx(Exx exx) {
    if (routineByteCodeGenerator.bytecodeGenerationContext.useFields) {
      methodMaker.invoke("exx");
    } else
      throw new RuntimeException("not implemented");
  }

  @Override
  public void visitLdir(Ldir ldir) {
    String methodName = ((RepeatingInstruction) ldir).getClass().getSimpleName().toLowerCase();
    if (routineByteCodeGenerator.bytecodeGenerationContext.useFields) {
      methodMaker.invoke(methodName);
    } else {
      Variable invoke = methodMaker.invoke(methodName, routineByteCodeGenerator.getExistingVariable("HL"), routineByteCodeGenerator.getExistingVariable("DE"), routineByteCodeGenerator.getExistingVariable("BC"));
      routineByteCodeGenerator.getExistingVariable("HL").set(invoke.aget(0));
      routineByteCodeGenerator.getExistingVariable("DE").set(invoke.aget(1));
      routineByteCodeGenerator.getExistingVariable("BC").set(invoke.aget(2));
    }
  }

  @Override
  public boolean visitLddr(Lddr lddr) {
    callRepeatingInstruction(lddr);
    return false;
  }

  @Override
  public boolean visitCpir(Cpir cpir) {
    String methodName = ((RepeatingInstruction) cpir).getClass().getSimpleName().toLowerCase();
    if (routineByteCodeGenerator.bytecodeGenerationContext.useFields) {
      methodMaker.invoke(methodName);
    } else {
      Variable invoke = methodMaker.invoke(methodName, routineByteCodeGenerator.getExistingVariable("HL"), routineByteCodeGenerator.getExistingVariable("BC"), routineByteCodeGenerator.getExistingVariable("A"));
      routineByteCodeGenerator.getExistingVariable("HL").set(invoke.aget(0));
      routineByteCodeGenerator.getExistingVariable("BC").set(invoke.aget(1));
    }
    return false;
  }

  @Override
  public void visitCpdr(Cpdr cpdr) {
    callRepeatingInstruction(cpdr);
  }

  private void callRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
    String methodName = repeatingInstruction.getClass().getSimpleName().toLowerCase();
    methodMaker.invoke(methodName);
  }

  @Override
  public boolean visitingJP(JP<T> jp) {
    if (jp.getPositionOpcodeReference() instanceof Register<T> register) {
      Map<Integer, DynamicJPData> dynamicJP = SEInstructionFactory.dynamicJP;
      dynamicJP.forEach((djpc, dj) -> {
        if (djpc == routineByteCodeGenerator.bytecodeGenerationContext.pc.read().intValue()) {
          dj.cases.forEach(c -> {
            Variable existingVariable = routineByteCodeGenerator.getExistingVariable2(register);
            existingVariable.ifEq(c, () -> {
              Label label = routineByteCodeGenerator.getLabel(c);
              if (label != null) {
                methodMaker.goto_(label);
              } else {
                routineByteCodeGenerator.invokeTransformedMethod(c);
                methodMaker.return_();
              }
            });
          });
        }
      });
      return true;
    } else
      return false;
  }

  public boolean visitLdAR(LdAR tLdAR) {
    Variable existingVariable = routineByteCodeGenerator.getExistingVariable("A");
    existingVariable.set(methodMaker.invoke("R"));
    return true;
  }

  @Override
  public void visitingCcf(CCF ccf) {
    methodMaker.invoke("ccf");
  }
}
