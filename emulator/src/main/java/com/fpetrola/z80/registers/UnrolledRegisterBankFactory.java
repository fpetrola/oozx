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

package com.fpetrola.z80.registers;

import com.fpetrola.oozx.BytecodeInliner;
import com.fpetrola.oozx.InstructionAnalyzer;
import com.fpetrola.oozx.Z80UnRolled;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.OpCodeDecoder;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;

import java.util.Map;
import java.util.TreeMap;

public class UnrolledRegisterBankFactory {

  private static int i;

  public Z80UnRolled createZ80Unrolled(OpCodeDecoder getOpcodesTables) {
    Map<Integer, Instruction> instructions = new TreeMap<>();
    Instruction[] opcodeLookupTable = getOpcodesTables.getOpcodeLookupTable();
    BytecodeInliner lastInliner = new BytecodeInliner(new InstructionAnalyzer());
    
    // Agregar instrucciones principales (sin prefijo)
    for (int idx = 0; idx < opcodeLookupTable.length; idx++) {
      Instruction instruction = opcodeLookupTable[idx];
      if (instruction instanceof TargetSourceInstruction<?> || 
          instruction instanceof ParameterizedUnaryAluInstruction ||
          instruction instanceof DefaultFetchNextOpcodeInstruction) {
        instructions.put(idx, instruction);
      }
    }
    
    // Agregar instrucciones prefijadas con 0xCB si existe
    if (opcodeLookupTable[0xCB] instanceof DefaultFetchNextOpcodeInstruction cbInstruction) {
      Instruction[] cbTable = cbInstruction.getTable();
      for (int idx = 0; idx < cbTable.length; idx++) {
        Instruction instruction = cbTable[idx];
        if (instruction instanceof TargetSourceInstruction<?> || 
            instruction instanceof ParameterizedUnaryAluInstruction) {
          // Opcode prefijado: prefijo en byte alto, siguiente byte en byte bajo
          int prefixedOpcode = (0xCB << 8) | idx;
          instructions.put(prefixedOpcode, instruction);
        }
      }
    }
    
    Class<?> instructionsSwitchClass = lastInliner.generateAndLoadMultipleInstructions("InstructionsSwitch" + i++, instructions);
    try {
      return (Z80UnRolled) instructionsSwitchClass.getDeclaredConstructors()[0].newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public RegisterBank createBank() {

    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    Z80UnRolled registerBank = createZ80Unrolled(opcodesTables);
//    UnrolledRegisterBank registerBank = new UnrolledRegisterBank();

    registerBank.registerAf = registerBank.new AFRegister(registerBank.new ARegister(), registerBank.new FRegister());
    registerBank.registerBc = registerBank.new BCRegister(registerBank.new BRegister(), registerBank.new CRegister());
    registerBank.registerDe = registerBank.new DERegister(registerBank.new DRegister(), registerBank.new ERegister());
    registerBank.registerHl = registerBank.new HLRegister(registerBank.new HRegister(), registerBank.new LRegister());
    registerBank.register_af = registerBank.new AFxRegister(registerBank.new AxRegister(), registerBank.new FxRegister());
    registerBank.register_bc = registerBank.new BCxRegister(registerBank.new BxRegister(), registerBank.new CxRegister());
    registerBank.register_de = registerBank.new DExRegister(registerBank.new DxRegister(), registerBank.new ExRegister());
    registerBank.register_hl = registerBank.new HLxRegister(registerBank.new HxRegister(), registerBank.new LxRegister());
    registerBank.registerIx = registerBank.new IXRegister(registerBank.new IXHRegister(), registerBank.new IXLRegister());
    registerBank.registerIy = registerBank.new IYRegister(registerBank.new IYHRegister(), registerBank.new IYLRegister());
    registerBank.registerIr = registerBank.new IRRegister(registerBank.new IRegister(), registerBank.new RRegister());
    registerBank.registerPc = registerBank.new PCRegister();
    registerBank.registerSp = registerBank.new SPRegister();
    registerBank.registerMemptr = registerBank.new MEMPTRRegister();
    registerBank.registerVirtual = registerBank.new VirtualRegister();

    return registerBank;
  }
}
