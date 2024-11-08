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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.blocks.spy.RoutineGrouperSpy;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.graph.GraphFrame;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.ReadOnlyMemoryImplementation;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.TraceableWordNumber;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.spy.ComplexInstructionSpy;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.spy.SpyRegisterBankFactory;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RegisterNameBuilder;
import com.fpetrola.z80.transformations.TransformerInstructionExecutor;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;
import machine.Clock;
import z80core.IZ80;
import z80core.MemIoOps;
import z80core.Timer;

import java.io.File;

import static com.fpetrola.z80.registers.RegisterName.*;

public class Z80B extends RegistersBase implements IZ80 {
  public static final String FILE = "console2A.txt";
  private MemIoOps memIoImpl;
  public OOZ80 z80;
  private Timer timer;
  private final Clock clock;
  private volatile boolean executing;
  private ComplexInstructionSpy spy;

  @Override
  public VirtualRegisterFactory getVirtualRegisterFactory() {
    return virtualRegisterFactory;
  }

  public VirtualRegisterFactory virtualRegisterFactory;

  public Z80B(MemIoOps memIoOps, GraphFrame graphFrame) {
    super();
    this.clock = Clock.getInstance();
    this.memIoImpl = memIoOps;
    // spy = new SyncInstructionSpy();
    spy = new NullInstructionSpy();
    spy = new RoutineGrouperSpy(graphFrame);
    z80 = createCompleteZ80(memIoOps, FILE.equals("console2A.txt"), spy);
    State state = z80.getState();
    setState(state);
    spy.setState(state);

    // Z80Cpu z802 = createCompleteZ80(memIoOps, false, new NullInstructionSpy());
    //z802 = createMutationsZ80(memory, io, instructionExecutor);
//    z802.reset();
//    spy.setSecondZ80(z802);
    reset();

    timer = new Timer("Z80");
  }

  public int getRegisterValue(RegisterName registerName) {
    Register<WordNumber> register = z80.getState().getRegister(registerName);
    int result;

    if (register instanceof RegisterPair<WordNumber>) {
      RegisterPair<WordNumber> wordNumberRegisterPair = (RegisterPair<WordNumber>) register;
      result = ((getValueH(wordNumberRegisterPair.getHigh()) & 0xff) << 8) | (getValueH(wordNumberRegisterPair.getLow()) & 0xff);
    } else {
      WordNumber o = (WordNumber) virtualRegisterFactory.lastValues.get(register);
      if (o == null)
        return register.read().intValue();
      else
        return o.intValue();
//      VirtualRegister<WordNumber> l = (VirtualRegister) virtualRegisterFactory.lastVirtualRegisters.get(register);
//      if (l != null) {
//        WordNumber read = l.read();
//        if (read == null)
//          System.out.println("sdgdgdgg1111");
//        result = read.intValue();
//      } else {
//        result = register.read().intValue();
//      }
    }
    return result;
  }

  private int getValueH(Register<WordNumber> high) {
    WordNumber o = (WordNumber) virtualRegisterFactory.lastValues.get(high);
    if (o == null) {
      o = high.read();
    }
    return o.intValue();
//    VirtualRegister<WordNumber> h = (VirtualRegister) virtualRegisterFactory.lastVirtualRegisters.get(high);
//    return h != null ? h.read().intValue() : high.read().intValue();
  }

  private OOZ80 createCompleteZ80(MemIoOps memIoOps, boolean traditional, ComplexInstructionSpy spy1) {
    TraceableWordNumber.instructionSpy = spy1;
    MemoryImplementation memory = new MemoryImplementation(memIoOps, spy1);
    IOImplementation io = new IOImplementation(memIoOps);

    State state = new State(io, new SpyRegisterBankFactory(spy1).createBank(), spy1.wrapMemory(memory));
    InstructionExecutor instructionExecutor = new SpyInstructionExecutor(getSpy());

    TransformerInstructionExecutor transformerInstructionExecutor = createInstructionTransformer(state, instructionExecutor);
    InstructionExecutor instructionExecutor1 = traditional ? instructionExecutor : transformerInstructionExecutor;
    return createZ80(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), instructionExecutor1);
  }

  private TransformerInstructionExecutor createInstructionTransformer(State state, InstructionExecutor instructionExecutor) {
    DefaultInstructionFactory instructionFactory = new DefaultInstructionFactory(state);
    virtualRegisterFactory = new VirtualRegisterFactory(instructionExecutor, new RegisterNameBuilder());
    InstructionTransformer instructionTransformer = new InstructionTransformer(instructionFactory, virtualRegisterFactory);
    TransformerInstructionExecutor transformerInstructionExecutor = new TransformerInstructionExecutor(state.getPc(), instructionExecutor, false, instructionTransformer);
    return transformerInstructionExecutor;
  }

  private Z80Cpu createMutationsZ80(MemoryImplementation memory, IOImplementation io, InstructionExecutor instructionExecutor) {
    final ReadOnlyMemoryImplementation memory1 = new ReadOnlyMemoryImplementation(memory);
    State state2 = new State(new ReadOnlyIOImplementation(io), new SpyRegisterBankFactory(spy).createBank(), spy.wrapMemory(memory1));
    Z80Cpu z802 = createZ80(state2, new MutableOpcodeConditions(state2, (instruction, x, state) -> true), instructionExecutor);
    return z802;
  }

  private OOZ80 createZ80(State state, OpcodeConditions opcodeConditions, InstructionExecutor instructionExecutor1) {
    return new OOZ80(state, new DefaultInstructionFetcher<>(state, opcodeConditions, new FetchNextOpcodeInstructionFactory(getSpy(), state), instructionExecutor1, new DefaultInstructionFactory(state)));
  }

  public void execute(int statesLimit) {
    executing = true;
    while (clock.getTstates() < statesLimit) {
//      timer.start();
      clock.addTstates(1);

      z80.execute();

//      if (System.currentTimeMillis() - start > 1000)
//        MemIoImpl.poke8(16384, 255);
//      start = System.currentTimeMillis();
    }
    executing = false;
  }

  public void readRegisters() {
    getRegisterValue(PC);
    getRegisterValue(SP);
    getRegisterValue(BC);
    getRegisterValue(BCx);
    getRegisterValue(DE);
    getRegisterValue(DEx);
    getRegisterValue(HL);
    getRegisterValue(HLx);
    getRegisterValue(A);
    getRegisterValue(Ax);
    getRegisterValue(AF);
    getRegisterValue(AFx);
    getRegisterValue(F);
    getRegisterValue(Fx);
    getRegisterValue(R);
    getRegisterValue(IX);
    getRegisterValue(IY);
  }

  public void setBreakpoint(int address, boolean state) {
  }

  public void reset() {
    z80.reset();
    readRegisters();
  }

  public void update() {
    for (int i = 0; i < 0xFFFF; i++) {
      int peek8 = memIoImpl.peek83(i);
      memIoImpl.poke82(i, peek8);
    }
    z80.update();
    spy.reset(getState());
    timer.reset();
    readRegisters();
  }

  public void enableSpy(boolean b) {
    getSpy().enable(b);
  }

  public void setSpritesArray(boolean[] bitsWritten) {
    getSpy().setSpritesArray(bitsWritten);
  }

  public synchronized boolean isExecuting() {
    return executing;
  }

  @Override
  public void setLoadedFile(File fileSnapshot) {
    spy.setGameName(fileSnapshot.getName());
  }

  public ComplexInstructionSpy getSpy() {
    return spy;
  }
}