package com.fpetrola.z80.tstates;

import com.fpetrola.z80.cpu.DefaultInstructionExecutor;
import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.NullInstructionSpy;

/**
 * The Z80's own timing with no ULA in the way: every access and every internal cycle of an instruction's plan costs
 * what it costs, a port access the four T-states of its machine cycle, and a write the ROM ignores its three.
 */
public class UncontendedTiming {
  public static final int PORT_ACCESS = 4, INTERRUPT_ACKNOWLEDGE = 7;
  private static final int SCRATCH = 0x8000, DATA = 0x9000;
  private static OOZ80 scratch;
  private AddStatesMemoryWriteListener writes;

  public MockedMemory memory() {
    return new MockedMemory(true) {
      public void write(int address, int value) {
        if (isProtected(address))
          writes.writtingMemoryAt(address, value);
        else
          super.write(address, value);
      }
    };
  }

  public void attach(State state, DefaultInstructionExecutor executor, InstructionFetcher fetcher) {
    RecordingPhaseProcessor processor = new RecordingPhaseProcessor(state, event -> {
    }) {
      public void contend(int address, int times, int tstates, Contention.Kind kind) {
        state.clock.addTStates(times * tstates);
      }
    };
    writes = new AddStatesMemoryWriteListener(processor);
    state.getMemory().addMemoryReadListener(new AddStatesMemoryReadListener(processor));
    state.getMemory().addMemoryWriteListener(writes);
    executor.addTopExecutionListener(processor);
    fetcher.addFetchListener(new FetchListener() {
      public void interruptedTo(int vector) {
        state.clock.addTStates(INTERRUPT_ACKNOWLEDGE);
      }
    });
  }

  /** What the instruction encoded by these bytes costs on its cheapest and dearest path: not taken and taken, or ending and repeating. */
  public static synchronized int[] costOf(int... bytes) {
    if (scratch == null)
      scratch = scratchMachine();
    int cheapest = Integer.MAX_VALUE, dearest = 0;
    for (int[] registers : new int[][]{{0x0000, 0x0101}, {0xFFFF, 0x0202}, {0x0000, 0x0001}, {0xFFFF, 0x0002}}) {
      int cost = costWith(bytes, registers[0], registers[1]);
      cheapest = Math.min(cheapest, cost);
      dearest = Math.max(dearest, cost);
    }
    return new int[]{cheapest, dearest};
  }

  /** A and F all clear or all set, B and C one or two: every condition both ways, and a search never finding its byte. */
  private static int costWith(int[] bytes, int af, int bc) {
    State state = scratch.getState();
    for (int i = 0; i < bytes.length; i++)
      state.getMemory().write(SCRATCH + i, bytes[i]);
    state.getMemory().write(DATA, ~af & 0xFF);
    state.getRegister(RegisterName.AF).write(af);
    state.getRegister(RegisterName.BC).write(bc);
    for (RegisterName pointer : new RegisterName[]{RegisterName.DE, RegisterName.HL, RegisterName.IX, RegisterName.IY})
      state.getRegister(pointer).write(DATA);
    state.getRegisterSP().write(0xFF00);
    state.getPc().write(SCRATCH);
    state.setHalted(false);
    state.clock.setTStates(0);
    scratch.execute();
    return state.clock.getTStates();
  }

  private static OOZ80 scratchMachine() {
    State[] holder = new State[1];
    IO ports = new IO() {
      public int in(int port) {
        holder[0].clock.addTStates(PORT_ACCESS);
        return 0xFF;
      }

      public void out(int port, int value) {
        holder[0].clock.addTStates(PORT_ACCESS);
      }
    };
    UncontendedTiming timing = new UncontendedTiming();
    State state = holder[0] = new State(ports, new DefaultRegisterBankFactory().createBank(), timing.memory());
    DefaultInstructionExecutor executor = new DefaultInstructionExecutor(state, false);
    InstructionFetcher fetcher = Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory(state));
    timing.attach(state, executor, fetcher);
    return new OOZ80(state, fetcher, executor);
  }
}
