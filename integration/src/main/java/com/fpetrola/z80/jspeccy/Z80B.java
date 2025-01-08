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

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.spy.RoutineGrouperSpy;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.ReadOnlyMemoryImplementation;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.spy.ComplexInstructionSpy;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.spy.SpyRegisterBankFactory;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RegisterNameBuilder;
import com.fpetrola.z80.transformations.TransformerInstructionExecutor;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;
import com.mxgraph.view.mxGraph;
import machine.Clock;
import z80core.IZ80;
import z80core.MemIoOps;
import z80core.Timer;

import java.io.File;

import static com.fpetrola.z80.registers.RegisterName.*;

public class Z80B extends RegistersBase<WordNumber> implements IZ80 {
  public static final String FILE = "console2A.txt";
  private final MemIoOps memIoImpl;
  public OOZ80 z80;
  private final Timer timer;
  private final Clock clock;
  private volatile boolean executing;
  private ComplexInstructionSpy spy;
  private BlocksManager blockManager;

  public VirtualRegisterFactory getVirtualRegisterFactory() {
    return virtualRegisterFactory;
  }

  public VirtualRegisterFactory virtualRegisterFactory;

  public Z80B(MemIoOps memIoOps, mxGraph graphFrame, DataflowService dataflowService, RoutineFinder routineFinder1) {
    super();
    this.clock = Clock.getInstance();
    this.memIoImpl = memIoOps;
    // spy = new SyncInstructionSpy();
    spy = new NullInstructionSpy();
    spy = new RoutineGrouperSpy(graphFrame, dataflowService, routineFinder1);
    final IOImplementation io = new IOImplementation(memIoOps);
    final MemoryImplementation memory = new MemoryImplementation(memIoOps, spy);
    z80 = createCompleteZ80(FILE.equals("console2A.txt"), spy, blockManager, new State(io, new SpyRegisterBankFactory(spy).createBank(), spy.wrapMemory(memory)));
    State state = z80.getState();
    io.setPc(state.getPc());
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

    boolean b = registerName.name().length() > 1;

    if (register instanceof RegisterPair<WordNumber> wordNumberRegisterPair) {
      result = ((getValueH(wordNumberRegisterPair.getHigh()) & 0xff) << 8) | (getValueH(wordNumberRegisterPair.getLow()) & 0xff);
    } else {
      return b ? register.read().intValue() & 0xFFFF : register.read().intValue() & 0xFF;
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
    if (virtualRegisterFactory != null) {
      WordNumber o = (WordNumber) virtualRegisterFactory.lastValues.get(high);
      if (o == null) {
        o = high.read();
      }

      return o.intValue();
    } else
      return high.read().intValue();
//    VirtualRegister<WordNumber> h = (VirtualRegister) virtualRegisterFactory.lastVirtualRegisters.get(high);
//    return h != null ? h.read().intValue() : high.read().intValue();
  }

  public static OOZ80 createCompleteZ80(boolean traditional, InstructionSpy spy1, BlocksManager blockManager1, State state) {
//    TraceableWordNumber.instructionSpy = spy1;

    InstructionExecutor instructionExecutor = DefaultInstructionExecutor.createSpyInstructionExecutor(spy1, state);

    InstructionExecutor instructionExecutor1 = traditional ? instructionExecutor : createInstructionTransformer(state, instructionExecutor, blockManager1);
    return createZ80(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), instructionExecutor1, spy1);
  }

  private static TransformerInstructionExecutor createInstructionTransformer(State state, InstructionExecutor instructionExecutor, BlocksManager blockManager1) {
    DefaultInstructionFactory instructionFactory = new DefaultInstructionFactory(state);
    var virtualRegisterFactory = new VirtualRegisterFactory(instructionExecutor, new RegisterNameBuilder(), blockManager1);
    InstructionTransformer instructionTransformer = new InstructionTransformer(instructionFactory, virtualRegisterFactory);
    TransformerInstructionExecutor transformerInstructionExecutor = new TransformerInstructionExecutor(state.getPc(), instructionExecutor, false, instructionTransformer);
    return transformerInstructionExecutor;
  }

  private Z80Cpu createMutationsZ80(MemoryImplementation memory, IOImplementation io, InstructionExecutor instructionExecutor) {
    final ReadOnlyMemoryImplementation memory1 = new ReadOnlyMemoryImplementation(memory);
    State state2 = new State(new ReadOnlyIOImplementation(io), new SpyRegisterBankFactory(spy).createBank(), spy.wrapMemory(memory1));
    Z80Cpu z802 = createZ80(state2, new MutableOpcodeConditions(state2, (instruction, x, state) -> true), instructionExecutor, getSpy());
    return z802;
  }

  private static OOZ80 createZ80(State state, OpcodeConditions opcodeConditions, InstructionExecutor instructionExecutor1, InstructionSpy spy1) {
    return new OOZ80(state, new DefaultInstructionFetcher<>(state, opcodeConditions, new FetchNextOpcodeInstructionFactory(spy1, state), instructionExecutor1, new DefaultInstructionFactory(state), false, false));
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