
package z80.jspeccy;

import com.fpetrola.z80.blocks.spy.RoutineGrouperSpy;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.graph.GraphFrame;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.IOImplementation;
import com.fpetrola.z80.jspeccy.MemoryImplementation;
import com.fpetrola.z80.jspeccy.RegistersBase;
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
import com.fpetrola.z80.spy.InstructionSpy;
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

  public Z80B(MemIoOps memIoOps, GraphFrame graphFrame) {
    super();
    this.clock = Clock.getInstance();
    this.memIoImpl = memIoOps;
    // spy = new SyncInstructionSpy();
    spy = new NullInstructionSpy();
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

  private OOZ80 createCompleteZ80(MemIoOps memIoOps, boolean traditional, ComplexInstructionSpy spy1) {
    MemoryImplementation memory = new MemoryImplementation(memIoOps, spy1);
    IOImplementation io = new IOImplementation(memIoOps);

    State state = new State(io, new SpyRegisterBankFactory(spy1).createBank(), spy1.wrapMemory(memory));
    InstructionExecutor instructionExecutor = new SpyInstructionExecutor(spy, state);

    InstructionExecutor instructionExecutor1 = instructionExecutor ;
    return createZ80(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), instructionExecutor1);
  }

  private OOZ80 createZ80(State state, OpcodeConditions opcodeConditions, InstructionExecutor instructionExecutor1) {
    OOZ80 ooz80;
    DefaultInstructionFactory defaultInstructionFactory = new DefaultInstructionFactory(state);
    FetchNextOpcodeInstructionFactory fetchNextOpcodeInstructionFactory = new FetchNextOpcodeInstructionFactory(getSpy(), state);
    DefaultInstructionFetcher instructionFetcher = new DefaultInstructionFetcher<>(state, fetchNextOpcodeInstructionFactory, instructionExecutor1, defaultInstructionFactory, false);
    ooz80 = new OOZ80(state, instructionFetcher);
//    ooz80= new DebugEnabledOOZ80(state, spy);
    return ooz80;
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
    getRegister(PC);
    getRegister(SP);
    getRegister(BC);
    getRegister(BCx);
    getRegister(DE);
    getRegister(DEx);
    getRegister(HL);
    getRegister(HLx);
    getRegister(A);
    getRegister(Ax);
    getRegister(AF);
    getRegister(AFx);
    getRegister(F);
    getRegister(Fx);
    getRegister(R);
    getRegister(IX);
    getRegister(IY);
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

  @Override
  public ComplexInstructionSpy getSpy() {
    return spy;
  }
}