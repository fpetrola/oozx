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

package com.fpetrola.z80.bytecode.impl;

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.DefaultBlock;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.instructions.*;
import com.fpetrola.z80.instructions.base.*;
import com.fpetrola.z80.opcodes.references.ConditionFlag;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineVisitor;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public class InstructionsBytecodeGenerator implements InstructionVisitor {
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
    VirtualRegister<?> target = (VirtualRegister<?>) push.getTarget();
    // VirtualRegister top = byteCodeGenerator.getTop(target);
    //Variable var = methodMaker.var(int.class);
    //var.name("last_" + top.getName());
    //var.set(byteCodeGenerator.getExistingVariable(target).get());
    methodMaker.invoke("push", routineByteCodeGenerator.getExistingVariable(target).get());
  }

  @Override
  public void visitingPop(Pop pop) {
    VirtualRegister<?> target = (VirtualRegister<?>) pop.getTarget();
//    VirtualRegister top = byteCodeGenerator.getTop(target);
//    Variable var = methodMaker.var(int.class);
//    var.name("last_" + top.getName());
//    byteCodeGenerator.getExistingVariable(target).get().set(var);
    routineByteCodeGenerator.getExistingVariable(target).set(methodMaker.invoke("pop"));

//    if (pop instanceof SymbolicExecutionAdapter.PopReturnAddress popReturnAddress) {
//      ReturnAddressWordNumber returnAddress = popReturnAddress.getReturnAddress();
//      if (returnAddress != null) {
//        methodMaker.invoke("incPops");
//      }
//    }
  }

  @Override
  public void visitEx(Ex ex) {
    VirtualRegister<?> source = (VirtualRegister<?>) ex.getSource();
    VirtualRegister<?> target = (VirtualRegister<?>) ex.getTarget();
    // VirtualRegister top = byteCodeGenerator.getTop(target);
    //Variable var = methodMaker.var(int.class);
    //var.name("last_" + top.getName());
    //var.set(byteCodeGenerator.getExistingVariable(target).get());
    if (routineByteCodeGenerator.getTop(source).getName().startsWith("AF")) {
      Variable variable = routineByteCodeGenerator.variables.get("AF");
      if (variable instanceof SmartComposed16BitRegisterVariable existingVariable) {
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
      t.set(methodMaker.invoke("in", RoutineBytecodeGenerator.getRealVariable(s)));
    }, routineByteCodeGenerator));
  }

  @Override
  public boolean visitingRlc(RLC rlc) {
    invokeRotationInstruction(rlc, "rlc");
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
    pendingFlag = new PendingFlagUpdate(f, scf, routineByteCodeGenerator, address);
  }

  @Override
  public boolean visitingRl(RL rl) {
    if (previousPendingFlag != null)
      previousPendingFlag.update(true);

    invokeRotationInstruction(rl, "rl");
    return true;
  }

  @Override
  public boolean visitingRr(RR rrc) {
    rrc.accept(new VariableHandlingInstructionVisitor((s, t) -> {
      Variable variable = t.get();
      if (variable != null)
        t.set(methodMaker.invoke("rrc", variable));
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
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.add(1).and(0xff)), routineByteCodeGenerator);
    inc.accept(visitor);
    processFlag(inc, () -> visitor.targetVariable);
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

  public void visitingAdd16(Add16 add16) {
    add16.accept(new VariableHandlingInstructionVisitor((s, t) -> getSet(s, t, 0xffff), routineByteCodeGenerator));
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

  public void visitingAdd(Add add) {
    Variable[] add1 = new Variable[1];
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> {
      add1[0] = t.add(s);
      t.set(add1[0].and(0xFF));
    }, routineByteCodeGenerator);
    add.accept(visitor);
    processFlag(add, () -> add1[0]);
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
  public void visitingSbc16(Sbc16 sbc16) {
    sbc16.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.sub(s).and(0xffff)), routineByteCodeGenerator));
  }


  @Override
  public void visitingAdc16(Adc16 adc16) {
    adc16.accept(new VariableHandlingInstructionVisitor((s, t) -> getSet(s, t, 0xffff), routineByteCodeGenerator));
  }

  public boolean visitingDec(Dec dec) {
    VariableHandlingInstructionVisitor visitor = new VariableHandlingInstructionVisitor((s, t) -> t.set(t.sub(1).and(0xff)), routineByteCodeGenerator);
    dec.accept(visitor);
    processFlag(dec, () -> visitor.targetVariable);
    return false;
  }

  @Override
  public void visitingNeg(Neg neg) {
    neg.accept(new VariableHandlingInstructionVisitor((s, t) -> t.set(t.neg().and(0xff)), routineByteCodeGenerator));
  }

  private void processFlag(DefaultTargetFlagInstruction targetFlagInstruction, Supplier<Variable> targetVariable) {
    pendingFlag = new PendingFlagUpdate(targetVariable, targetFlagInstruction, routineByteCodeGenerator, address);
  }

  private void processFlag(DefaultTargetFlagInstruction targetFlagInstruction, Supplier<Variable> targetVariable, Supplier<Object> sourceVariable) {
    pendingFlag = new PendingFlagUpdate(targetVariable, targetFlagInstruction, routineByteCodeGenerator, address, sourceVariable);
  }

  public void visitingLd(Ld ld) {
    ld.accept(new VariableHandlingInstructionVisitor((sourceVariable, targetVariable) -> {
      Class<?> aClass = targetVariable.classType();
      if (!aClass.equals(int.class))
        targetVariable.aset(0, sourceVariable);
      else if (targetVariable instanceof WriteArrayVariable writeArrayVariable)
        writeArrayVariable.set(sourceVariable);
      else if (sourceVariable instanceof Variable variable) {
        //targetVariable.set(sourceVariable);
      }
    }, routineByteCodeGenerator) {
      public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
        super.visitingSource(source, targetSourceInstruction);
        createInitializer = (x) -> sourceVariable;
      }
    });
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
        List<Integer> i = routineByteCodeGenerator.routine.returnPoints.get(address).stream().toList();
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
      Variable result = opcodeReferenceVisitor.process((VirtualRegister) djnz.getCondition().getB());
      Variable and = result.sub(1).and(0xFF);
      result.set(and);
      result.ifNe(0, runnable);
    } else if (instruction instanceof ConditionalInstruction conditionalInstruction && conditionalInstruction.getCondition() instanceof ConditionFlag conditionFlag) {
      Variable f = opcodeReferenceVisitor.process((VirtualRegister) conditionFlag.getRegister());
      String string = conditionalInstruction.getCondition().toString();
      Object source;
      Variable targetVariable = null;
      if (previousPendingFlag != null) {
        FlagInstruction targetFlagInstruction = previousPendingFlag.targetFlagInstruction;

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
          } else
            source = previousPendingFlag.sourceVariableSupplier.get();

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
        }
      } else {
        source = 0;
        targetVariable = f;
      }
      if (targetVariable != null)
        executeCondition(runnable, string, targetVariable, source);
    } else {
      if (previousPendingFlag != null) {
        previousPendingFlag.update(false);
      }
      runnable.run();
    }
  }

  private void createIfMethod(Instruction instruction, ConditionalInstruction conditionalInstruction) {
    BytecodeGenerationContext bytecodeGenerationContext = routineByteCodeGenerator.bytecodeGenerationContext;
    BlocksManager blocksManager = bytecodeGenerationContext.routineManager.blocksManager;
    int startAddress = bytecodeGenerationContext.pc.read().intValue() + instruction.getLength();
    int endAddress = conditionalInstruction.getJumpAddress().intValue() - 1;
    if (startAddress < endAddress) {
      int i = bytecodeGenerationContext.pc.read().intValue();
      Routine routine = new Routine(new DefaultBlock(startAddress, endAddress, new BlocksManager(new NullBlockChangesListener(), true)));
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
        if (routineByteCodeGenerator.routine.virtualPop.containsKey(address)) {
          routineByteCodeGenerator.getField("nextAddress").set(routineByteCodeGenerator.routine.virtualPop.get(address) + 1);
          incPopsAdded = true;
        }
        routineByteCodeGenerator.returnFromMethod();
      });
    }
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
  public void visitLddr(Lddr lddr) {
    callRepeatingInstruction(lddr);
  }

  @Override
  public void visitCpir(Cpir cpir) {
    String methodName = ((RepeatingInstruction) cpir).getClass().getSimpleName().toLowerCase();
    if (routineByteCodeGenerator.bytecodeGenerationContext.useFields) {
      methodMaker.invoke(methodName);
    } else {
      Variable invoke = methodMaker.invoke(methodName, routineByteCodeGenerator.getExistingVariable("HL"), routineByteCodeGenerator.getExistingVariable("BC"), routineByteCodeGenerator.getExistingVariable("A"));
      routineByteCodeGenerator.getExistingVariable("HL").set(invoke.aget(0));
      routineByteCodeGenerator.getExistingVariable("BC").set(invoke.aget(1));
    }
  }

  @Override
  public void visitCpdr(Cpdr cpdr) {
    callRepeatingInstruction(cpdr);
  }

  private void callRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
    String methodName = repeatingInstruction.getClass().getSimpleName().toLowerCase();
    methodMaker.invoke(methodName);
  }
}
