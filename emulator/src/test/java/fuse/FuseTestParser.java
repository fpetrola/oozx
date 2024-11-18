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

package fuse;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.cpu.Event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class FuseTestParser<T extends WordNumber> {
  private final File inFile;
  private final File expectedFile;
  private int time;
  private Runnable lastEvents;
  private Z80Cpu cpu;
  private DefaultInstructionFetcher instructionFetcher;
  private State<T> state;

  public FuseTestParser(File testDataDir) {
    this.inFile = new File(testDataDir, "tests.in");
    this.expectedFile = new File(testDataDir, "tests.expected");
  }

  public List<FuseTest> getTests() {
    try {
      List<String> inLines = Files.readAllLines(inFile.toPath()).stream()
          .filter(line -> !line.trim().isEmpty())
          .collect(Collectors.toList());

      List<FuseTest> tests = new ArrayList<>();
      Iterator<String> iterator = inLines.iterator();

      Z80Cpu z80Cpu = getZ80Cpu();

      while (iterator.hasNext()) {
        String testId = iterator.next();
        String registers = iterator.next(); // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
        String state = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

        StringBuilder memory = new StringBuilder();
        String line;
        while (!(line = iterator.next().trim()).equals("-1")) {
          memory.append("\n").append(line);
        }

        FuseTest fuseTest = new FuseTest(testId, registers, state, memory.toString(), z80Cpu);
        fuseTest.initCpu();
        fuseTest.run();
        tests.add(fuseTest);
      }
      return tests;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addMc(int x, Z80Cpu cpu, State<T> state, int time1, RegisterName registerName, int delta) {
    for (int i = 0; i < x; i++) {
      cpu.getState().addEvent(new Event(time1, "MC", state.getRegister(registerName).read().intValue() + delta, null));
    }
  }

  private Z80Cpu getZ80Cpu() {
    MockedMemory<T> memory = new MockedMemory(true);

    state = new State<T>(new IO<WordNumber>() {

      int contend_port_preio(WordNumber port) {
        if ((port.intValue() & 0xc000) == 0x4000) {
          cpu.getState().addEvent(new Event(1, "PC", port.intValue(), null));
        } else
          cpu.getState().tstates++;
        return 1;
      }

      int contend_port_postio(WordNumber port) {
        if ((port.intValue() & 0x0001) != 0) {
          if ((port.intValue() & 0xc000) == 0x4000) {
            cpu.getState().addEvent(new Event(1, "PC", port.intValue(), null));
            cpu.getState().addEvent(new Event(1, "PC", port.intValue(), null));
            cpu.getState().addEvent(new Event(1, "PC", port.intValue(), null));
            return 3;
          } else {
            cpu.getState().tstates += 3;
            return 3;
          }
        } else {
          cpu.getState().addEvent(new Event(3, "PC", port.intValue(), null));
          return 3;
        }
      }

      public WordNumber in(WordNumber port) {
        WordNumber value = createValue(port.intValue() >> 8);
        int i = contend_port_preio(port);
        cpu.getState().addEvent(new Event(0, "PR", port.intValue(), value.intValue()));
        int i1 = contend_port_postio(port);
        return value;
      }

      public void out(WordNumber port, WordNumber value) {
        int i = contend_port_preio(port);
        cpu.getState().addEvent(new Event(0, "PW", port.intValue(), value.intValue()));
        int i1 = contend_port_postio(port);
      }
    }, memory);
    NullInstructionSpy spy = new NullInstructionSpy();
    DefaultInstructionFactory instructionFactory = new DefaultInstructionFactory<WordNumber>(state);
    instructionFetcher = new MyDefaultInstructionFetcher(state, spy, instructionFactory);
    cpu = (OOZ80<WordNumber>) new OOZ80(state, instructionFetcher);

    memory.addMemoryReadListener(new WordNumberMemoryReadListener(cpu, state, instructionFetcher)
    );

    memory.addMemoryWriteListener((address, value) -> {
      processCycleStep(new InstructionVisitor<>() {
        public boolean visitingCall(Call tCall) {
          if (cpu.getState().getTStatesSinceCpuStart() == 10)
            addMc(1, cpu, state, 1, RegisterName.IR, 1);

          return InstructionVisitor.super.visitingCall(tCall);
        }

        public void visitEx(Ex<T> ex) {
          if (ex.getTarget() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
            int i = ex.getSource().equals(state.getRegister(RegisterName.HL)) && indirectMemory16BitReference.target.equals(state.getRegister(RegisterName.SP)) ? 10 : 14;
            if (cpu.getState().getTStatesSinceCpuStart() == i)
              addMc(1, cpu, state, 1, RegisterName.SP, 1);
          }

          InstructionVisitor.super.visitEx(ex);
        }
      });

      cpu.getState().addEvent(new Event(3, "MC", address.intValue(), null));
      cpu.getState().addEvent(new Event(0, "MW", address.intValue(), value.intValue()));

      processCycleStep(new InstructionVisitor<>() {
        public void visitBlockInstruction(BlockInstruction blockInstruction) {
          InstructionVisitor.super.visitBlockInstruction(blockInstruction);
        }
      });
    });
    return cpu;
  }

  private void processCycleStep(InstructionVisitor<T, Integer> instructionVisitor) {
    var instruction = instructionFetcher.instruction2;
    if (instruction != null)
      instruction.accept(instructionVisitor);
  }

  public static class MyDefaultInstructionFetcher extends DefaultInstructionFetcher {
    public MyDefaultInstructionFetcher(State state, NullInstructionSpy spy, DefaultInstructionFactory instructionFactory) {
      super(state, new OpcodeConditions(state.getFlag(), state.getRegister(RegisterName.B)), new FetchNextOpcodeInstructionFactory(spy, state), new SpyInstructionExecutor(spy, new MemptrUpdater(state.getMemptr(), state.getMemory())), instructionFactory);
    }

    public Instruction getLastInstruction() {
      return instruction;
    }

    public void reset() {
      super.reset();
      createOpcodeTables();
    }

  }


  public List<FuseResult> getResults() {
    try {
      List<String> inLines = Files.readAllLines(expectedFile.toPath()).stream()
          .filter(line -> !line.trim().isEmpty())
          .collect(Collectors.toList());

      List<FuseResult> results = new ArrayList<>();
      Iterator<String> iterator = inLines.iterator();
      List<String> eventTypes = Arrays.asList("MR", "MW", "MC", "PR", "PW", "PC");

      String next = iterator.hasNext() ? iterator.next() : "";
      while (iterator.hasNext()) {
        String testId = next;

        List<Event> events = new ArrayList<>();

        // Skip events
        while (true) {
          if (!iterator.hasNext()) {
            break;
          } else {
            next = iterator.next();
            if (eventTypes.stream().noneMatch(next::contains)) {
              break;
            }
            events.add(parseEvent(next));
          }
        }

        String registers = next; // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
        String state = next = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

        StringBuilder memory = new StringBuilder();
        while (iterator.hasNext()) {
          next = iterator.next().trim();
          if (!next.endsWith("-1")) {
            break;
          }
          memory.append("\n").append(next);
        }

        results.add(new FuseResult(testId, registers, state, memory.toString(), events));
      }
      return results;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Helper to parse event in .expected file
  private Event parseEvent(String line) {
    String[] parts = line.trim().split(" ");
    int time = Integer.parseInt(parts[0]);
    String type = parts[1];
    int address = Integer.parseInt(parts[2], 16);
    Integer data = parts.length > 3 ? Integer.parseInt(parts[3], 16) : null;
    return new Event(time, type, address, data);
  }

  private class WordNumberMemoryReadListener implements MemoryReadListener<T> {
    private final Z80Cpu cpu;
    private final State<T> state;
    private final DefaultInstructionFetcher instructionFetcher;

    public WordNumberMemoryReadListener(Z80Cpu cpu, State<T> state, DefaultInstructionFetcher instructionFetcher) {
      this.cpu = cpu;
      this.state = state;
      this.instructionFetcher = instructionFetcher;
    }

    @Override
    public void readingMemoryAt(T address, T value, int fetching) {
      if (address.intValue() == -1) {
        InstructionVisitor<T, Integer> instructionVisitor = new InstructionVisitor<>() {
          public void visitingRst(RST rst) {
            addMc(1, cpu, state, 1, RegisterName.IR, 0);
          }

          public boolean visitingRet(Ret ret) {
            if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
              addMc(1, cpu, state, 1, RegisterName.IR, 0);
            return InstructionVisitor.super.visitingRet(ret);
          }

          public boolean visitingAdc16(Adc16<T> tAdc16) {
            addMc(7, cpu, state, 1, RegisterName.IR, 0);
            return false;
          }

          public void visitingAdd16(Add16 tAdd16) {
            addMc(7, cpu, state, 1, RegisterName.IR, 0);
          }

          public void visitingInc16(Inc16 tInc16) {
            addMc(2, cpu, state, 1, RegisterName.IR, 0);
          }

          public void visitingSbc16(Sbc16<T> sbc16) {
            addMc(7, cpu, state, 1, RegisterName.IR, 0);
          }

          public void visitingDec16(Dec16 tDec16) {
            addMc(2, cpu, state, 1, RegisterName.IR, 0);
          }

          public void visitPush(Push push) {
            addMc(1, cpu, state, 1, RegisterName.IR, 0);
          }

          public void visitBlockInstruction(BlockInstruction blockInstruction) {
            if (blockInstruction instanceof Ini ||
                blockInstruction instanceof Ind ||
                blockInstruction instanceof Outi ||
                blockInstruction instanceof Outd
            )
              addMc(1, cpu, state, 1, RegisterName.IR, 0);
            InstructionVisitor.super.visitBlockInstruction(blockInstruction);
          }

          @Override
          public boolean visitRepeatingInstruction(RepeatingInstruction<T> repeatingInstruction) {
            if (repeatingInstruction instanceof Inir ||
                repeatingInstruction instanceof Indr ||
                repeatingInstruction instanceof Outir ||
                repeatingInstruction instanceof Outdr
            )
              addMc(1, cpu, state, 1, RegisterName.IR, 0);
            return InstructionVisitor.super.visitRepeatingInstruction(repeatingInstruction);
          }

          public void visitingLd(Ld<T> ld) {
            if (isLdSP(ld))
              addMc(2, cpu, state, 1, RegisterName.IR, 0);

            InstructionVisitor.super.visitingLd(ld);
          }


        };
        var instruction = instructionFetcher.instruction2;
        instruction.accept(instructionVisitor);

      } else if (address.intValue() == -2) {
        InstructionVisitor<T,?> instructionVisitor = new InstructionVisitor<>() {
          public void visitEx(Ex<T> ex) {
            if (ex.getTarget() instanceof IndirectMemory16BitReference<T>)
              addMc(2, cpu, state, 1, RegisterName.SP, 0);

            InstructionVisitor.super.visitEx(ex);
          }

          @Override
          public boolean visitingBit(BIT bit) {
            if (bit.getTarget() instanceof IndirectMemory8BitReference<?> indirectMemory8BitReference) {
              if (indirectMemory8BitReference.getTarget() instanceof Register<?> register) {
                if (register.getName().equals(RegisterName.HL.name()))
                  addMc(1, cpu, state, 1, RegisterName.HL, 0);
              }
            }

            return false;
          }

          public boolean visitOuti(Outi<T> outi) {
            if (outi.getNextPC() != null) {
              addMc(5, cpu, state, 1, RegisterName.IR, 0);
            }
            return InstructionVisitor.super.visitOuti(outi);
          }

          public boolean visitIni(Ini<T> tIni) {
            if (tIni.getNextPC() != null) {
              addMc(5, cpu, state, 1, RegisterName.IR, 0);
            }
            return InstructionVisitor.super.visitIni(tIni);
          }

          public boolean visitLdd(Ldd ldd) {
            addMc(2, cpu, state, 1, RegisterName.DE, 1);
            return true;
          }

          public void visitLdi(Ldi<T> tLdi) {
            addMc(2, cpu, state, 1, RegisterName.DE, -1);
            InstructionVisitor.super.visitLdi(tLdi);
          }

          @Override
          public void visitCpi(Cpi<T> cpi) {
            addMc(5, cpu, state, 1, RegisterName.HL, -1);
            InstructionVisitor.super.visitCpi(cpi);
          }

          @Override
          public boolean visitCpd(Cpd<T> cpd) {
            addMc(5, cpu, state, 1, RegisterName.HL, 1);
            return true;
          }

          public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
            if (tRepeatingInstruction instanceof Inir<T>) {
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.HL, -1);
              }
            } else if (tRepeatingInstruction instanceof Indr<T>) {
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.HL, +1);
              }
            } else if (tRepeatingInstruction instanceof Outdr<T>) {
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.BC, 0);
              }
            } else if (tRepeatingInstruction instanceof Outir<T>) {
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.BC, 0);
              }
            } else if (tRepeatingInstruction instanceof Ldir<T>) {
              addMc(2, cpu, state, 1, RegisterName.DE, -1);
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.DE, -1);
              }
            } else if (tRepeatingInstruction instanceof Lddr<T>) {
              addMc(2, cpu, state, 1, RegisterName.DE, 1);
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.DE, 1);
              }
            } else if (tRepeatingInstruction instanceof Cpir<T>) {
              addMc(5, cpu, state, 1, RegisterName.HL, -1);
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.HL, -1);
              }
            } else if (tRepeatingInstruction instanceof Cpdr<T>) {
              addMc(5, cpu, state, 1, RegisterName.HL, 1);
              if (tRepeatingInstruction.getNextPC() != null) {
                addMc(5, cpu, state, 1, RegisterName.HL, 1);
              }
            }
            return InstructionVisitor.super.visitRepeatingInstruction(tRepeatingInstruction);
          }

          public void visitingJR(JR jr) {
            if (jr.getNextPC() != null) {
              addMc(5, cpu, state, 1, RegisterName.HL, 1);
            } else {
              addMc(1, cpu, state, 1, RegisterName.IR, 0);
              cpu.getState().tstates += 2;
            }

            InstructionVisitor.super.visitingJR(jr);
          }
        };
        var instruction = instructionFetcher.instruction2;
        instruction.accept(instructionVisitor);
      } else {
        boolean fetching1 = fetching != 0;
        boolean pendingEvent = lastEvents != null;

        Runnable lastEvents1 = () -> {
          int time1;
          if (fetching1)
            time1 = 4 - (fetching == 2 ? 1 : 0);
          else
            time1 = 3;


          cpu.getState().addEvent(new Event(time1, "MC", address.intValue(), null));
          cpu.getState().addEvent(new Event(0, "MR", address.intValue(), value.intValue()));

          addBefore(address, cpu, state, instructionFetcher, time1);
        };

        if (fetching != 2) {
          lastEvents1.run();
        }

        if (pendingEvent) {
          lastEvents.run();
          lastEvents = null;
        }

        if (fetching == 2)
          lastEvents = lastEvents1;
      }
    }

    private void addBefore(T address, Z80Cpu cpu, State<T> state, DefaultInstructionFetcher instructionFetcher, int time1) {
      InstructionVisitor<T, ?> instructionVisitor = new InstructionVisitor<>() {
        public boolean visitRLD(RLD<T> rld) {
          if (cpu.getState().getTStatesSinceCpuStart() == 11) {
            addMc(4, cpu, state, 1, RegisterName.HL, 0);
          }
          return InstructionVisitor.super.visitRLD(rld);
        }

        public boolean visitLdOperation(LdOperation ldOperation) {
          extracted(address);
          return InstructionVisitor.super.visitLdOperation(ldOperation);
        }

        public void visitingBitOperation(BitOperation tBitOperation) {
          if (!(tBitOperation instanceof RES<?> || tBitOperation instanceof SET<?>) || !addIfIndirectHL(tBitOperation))
            extracted(address);
        }

        public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
          ifIndirecHLAddAll(parameterizedUnaryAluInstruction, address);
          return InstructionVisitor.super.visitingParameterizedUnaryAluInstruction(parameterizedUnaryAluInstruction);
        }

        public void visitingLd(Ld<T> ld) {
          if (cpu.getState().getTStatesSinceCpuStart() == 11 && (!ld.getTarget().equals(state.getRegister(RegisterName.SP)) || ld.getSource() instanceof Register<T>))
            addMc(5, cpu, state, 1, RegisterName.IR, 0);
          InstructionVisitor.super.visitingLd(ld);
        }

        public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
          if (cpu.getState().getTStatesSinceCpuStart() == 11)
            addMc(5, cpu, state, 1, RegisterName.IR, 0);
        }
      };
      var instruction = instructionFetcher.instruction2;
      if (instruction != null)
        instruction.accept(instructionVisitor);
    }

  }

  private boolean addIfIndirectHL(TargetInstruction tBitOperation) {
    if (isIndirectHL(tBitOperation)) {
      if (cpu.getState().getTStatesSinceCpuStart() == 11) {
        cpu.getState().addEvent(new Event(1, "MC", state.getRegister(RegisterName.HL).read().intValue(), null));
        return true;
      }

    }
    return false;
  }

  private void ifIndirecHLAddAll(TargetInstruction<T> rrc, T address) {
    addIfIndirectHL(rrc);
    extracted(address);
  }

  private void extracted(T address) {
    if (cpu.getState().getTStatesSinceCpuStart() == 14) {
      int address1 = state.getRegister(RegisterName.PC).read().intValue() + 3;
      cpu.getState().addEvent(new Event(1, "MC", address1, null));
      cpu.getState().addEvent(new Event(1, "MC", address1, null));
    } else if (cpu.getState().getTStatesSinceCpuStart() == 19) {
      cpu.getState().addEvent(new Event(1, "MC", address.intValue(), null));
    }
  }

  private boolean isIndirectHL(TargetInstruction tBitOperation) {
    return tBitOperation.getTarget() instanceof IndirectMemory8BitReference<?> indirectMemory8BitReference && indirectMemory8BitReference.getTarget() instanceof Register<?> register && register.getName().equals(RegisterName.HL.name());
  }

  private boolean isLdSP(Ld<T> ld) {
    return ld.getTarget().equals(state.getRegister(RegisterName.SP)) && ld.getSource() instanceof Register<T>;
  }
}

// Assuming you have classes FuseTest and FuseResult defined somewhere in your codebase.
