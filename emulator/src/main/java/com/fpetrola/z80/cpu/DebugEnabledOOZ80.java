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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.DefaultInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.OpCodeDecoder;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.spy.SpyRegisterBankFactory;

import static com.fpetrola.z80.registers.RegisterName.B;

public class DebugEnabledOOZ80<T extends WordNumber> extends OOZ80<T> {
  protected OpCodeDecoder opCodeHandler2;
  protected volatile boolean continueExecution = true;
  protected volatile int till = 0xFFFFFFF;
  protected volatile boolean step;
  public RegisterBank registerBank;

  public DebugEnabledOOZ80(State aState, InstructionSpy spy) {
    super(aState, new DefaultInstructionFetcher(aState, new FetchNextOpcodeInstructionFactory(spy, aState), new SpyInstructionExecutor(spy), new DefaultInstructionFactory(aState)));
    opCodeHandler2 = createOpCodeHandler(aState);
  }

  protected OpCodeDecoder createOpCodeHandler(State aState) {
    NullInstructionSpy spy = new NullInstructionSpy();

    registerBank = new SpyRegisterBankFactory(spy).createBank();
    State state2 = new State(aState.getIo(), registerBank, spy.wrapMemory(aState.getMemory()));
    OpCodeDecoder decoder1 = new TableBasedOpCodeDecoder<T>(state2, new OpcodeConditions(state2.getFlag(), state2.getRegister(B)), new FetchNextOpcodeInstructionFactory(spy, state2), new DefaultInstructionFactory(state2));
//    new ByExtensionOpCodeDecoder(state2, spy2).compareOpcodesGenerators(state2, spy2, decoder1);

    return decoder1;
  }

  public void execute() {
    try {

      if (state.getPc().read().intValue() == till)
        continueExecution = false;

      if (state.isActiveNMI()) {
        state.setActiveNMI(false);
        return;
      }

      if (continueExecution) {
        if (state.isIntLine()) {
          if (state.isIff1() && !state.isPendingEI()) {
            interruption();
          }
        }

        execute(1);

        if (state.isPendingEI()) {
          state.setPendingEI(false);
          endInterruption();
        }

        if (step) {
          continueExecution = false;
          step = false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String decodeAt(int pc2) {
//    String value = "";
//    Plain16BitRegister<T> tempPC = new Plain16BitRegister<T>(PC);
//    T value = WordNumber.createValue(pc2);
//    tempPC.write(value);
//    int i = state.getMemory().read(tempPC.read()).intValue();
//    Instruction<T> opcode1 = getOpCodeHandler().getOpcodeLookupTable()[i];
//    tempPC.increment(1);
//    int length = opcode1.getLength();
//    tempPC.write(value.plus(1));
//
//
//    for (int j = 0; j < length; j++) {
//      int opcodePart = state.getMemory().read(value.plus(j)).intValue();
//      String convertToHex = Helper.convertToHex(opcodePart);
//      value += convertToHex + " ";
//    }
//
//    String format = String.format("%-16s %s", value, opcode1);
//    return format;
    return "";
  }

  public int getLenghtAt(int pc2) {
    int i = state.getMemory().read(WordNumber.createValue(pc2)).intValue();
    Instruction<T> opcode1 = createOpCodeHandler(state).getOpcodeLookupTable()[i];
    int length = opcode1.getLength();
    return length;
  }

  public void continueExecution() {
    continueExecution = true;
  }

  public void step() {
    step = true;
    continueExecution = true;
  }

  public void stop() {
    continueExecution = false;
    till = 0xFFFFFF;
  }

  public void till(int address) {
    this.till = address;
  }

  public OpCodeDecoder getOpCodeHandler() {
    return opCodeHandler2;
  }
}
