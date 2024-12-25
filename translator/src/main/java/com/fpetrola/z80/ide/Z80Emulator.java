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

package com.fpetrola.z80.ide;

import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.spy.RegisterWriteListener;

import javax.swing.*;

public class Z80Emulator {
  private byte[] memory;
  private int[] registers; // Simplified registers array
  private boolean[] flags; // Z80 flags
  protected Runnable updateListener;
  protected JTable instructionTable;

  public Z80Emulator() {
    memory = new byte[65536];
    registers = new int[10];
    flags = new boolean[4]; // Z, N, H, C
    reset();
  }

  public void reset() {
    memory = new byte[65536];
    registers = new int[10];
    flags = new boolean[4];
  }

  public void loadProgram(byte[] program) {
    System.arraycopy(program, 0, memory, 0, program.length);
  }

  public void step() {
    // Simplified example: increment PC
    registers[8] = (registers[8] + 1) & 0xFFFF; // Increment PC
  }

  public void stepInto() {
    // Example logic for step into
    step();
  }

  public void continueExecution() {
    // Example logic for continue
  }

  public void pauseExecution() {
    // Example logic for pause
  }

  public void stopExecution() {
    reset();
  }

  public byte[] getMemory() {
    return memory;
  }

  public int[] getRegisters() {
    return registers;
  }

  public boolean[] getFlags() {
    return flags;
  }

  public int getPC() {
    return registers[8];
  }

  public String[] getInstructions() {
    // Simplified example of instructions
    String[] strings = {
        "NOP", "LD A, B", "ADD A, C", "JP 0x1000", "LD A, (HL)", "LD (DE), A", "INC HL",
        "DEC BC", "JP NZ, 0x2000", "CALL 0x3000", "RET", "RET Z", "LD SP, HL", "ADD HL, DE",
        "LD A, (BC)", "LD (HL), A", "CALL NZ, 0x4000", "LD A, B", "SUB C", "LD C, A",
        "OR A", "AND B", "XOR (HL)", "LD BC, 0x1234", "LD HL, 0x5678", "LD SP, 0x9ABC",
        "LD DE, 0xDEF0", "INC DE", "DEC A", "LD (C), A", "LD (HL+), A", "ADD A, (HL)",
        "JP NC, 0x2000", "LD B, (DE)", "LD (HL-), A", "RLCA", "LD A, 0xFF", "LD A, 0x00",
        "LD HL, SP+8", "POP BC", "PUSH DE", "LD A, (C)", "DEC DE", "LD B, C",
        "CALL 0x5000", "LD (BC), A", "LD A, (HL-)", "LD A, (HL+)", "LD BC, (SP)",
        "LD (SP), DE", "LD A, (BC+)", "RL (HL)", "ADD A, B", "JP Z, 0x1000",
        "LD A, C", "LD (DE), A", "DEC HL", "LD BC, A", "LD A, B", "JP 0x8000",
        "CALL Z, 0x2000", "LD (SP+4), A", "LD HL, (SP)", "JP NZ, 0x6000", "CALL 0x7000",
        "LD SP, HL", "LD DE, (HL)", "LD (DE), A", "LD A, (HL)", "DEC BC",
        "LD A, B", "LD (HL), B", "LD HL, 0x1234", "LD DE, A", "ADD A, C",
        "LD (SP), HL", "LD B, A", "LD HL, DE", "CALL 0x8000", "LD SP, 0xFF00",
        "POP DE", "LD (HL), A", "LD A, D", "ADD HL, SP", "LD HL, (SP+8)",
        "LD DE, HL", "JP 0xFFFF", "CALL 0x1234", "RET Z", "LD A, (HL)",
        "LD SP, HL", "LD A, (SP)", "LD HL, DE", "LD BC, A", "LD (HL), A",
        "DEC HL", "LD A, (BC)", "LD (DE), A", "ADD A, B", "LD BC, 0x9000",
        "CALL 0x1000", "LD HL, SP+2", "LD DE, (HL)", "LD A, 0x00", "DEC A",
        "LD (HL+), A", "LD SP, DE", "JP 0x2000", "LD HL, 0x1234", "LD A, C",
        "CALL 0x6000", "LD A, D", "SUB (HL)", "LD B, (HL)", "LD HL, SP+6",
        "LD (DE), B", "LD A, (BC)", "ADD A, D", "LD HL, 0x8000", "CALL 0x9000",
        "LD A, 0x80", "LD BC, 0x1000", "LD A, (HL)", "LD (DE), C", "LD SP, HL",
        "LD B, C", "LD A, B", "DEC HL", "LD HL, SP+4", "CALL 0x2000",
        "RET Z", "LD A, (BC)", "LD SP, HL", "CALL 0x5000", "LD (HL+), A",
        "ADD HL, BC", "LD HL, 0x4321", "LD BC, (SP+4)", "LD A, (HL)",
        "LD (DE), HL", "LD HL, 0x1000", "CALL 0x3000", "LD DE, 0x1234",
        "LD SP, 0x5678", "DEC BC", "LD (HL), 0xFF", "LD A, (HL+)",
        "LD B, (HL)", "LD HL, (SP)", "LD BC, 0x4321", "LD DE, (HL)",
        "CALL 0x7000", "LD HL, BC", "LD A, B", "LD B, (HL)", "LD A, (HL+)",
        "LD HL, 0x7FFF", "LD (SP), A", "LD DE, 0x9876", "LD BC, A",
        "LD HL, DE", "LD BC, 0x3000", "ADD A, D", "LD SP, BC", "RET NZ",
        "LD HL, SP+4", "LD (DE), A", "LD HL, (SP)", "LD BC, 0x1234",
        "LD DE, 0x5678", "LD HL, 0x8765", "LD A, B", "LD HL, BC",
        "LD BC, (SP)", "LD A, (HL)", "LD DE, 0x00FF", "LD SP, HL",
        "LD (DE), A", "LD HL, 0x1234", "LD A, (HL)", "LD BC, HL",
        "CALL 0x8000", "LD HL, (SP)", "LD SP, 0x1234", "RET Z", "LD A, B"
    };
    return strings;
  }

  public FetchListener getRegisterWriteListener() {
    return (address, instruction) -> {
    };
  }

  public void setUpdateListener(Runnable runnable) {
    this.updateListener = runnable;
  }

  public void setInstructionTableModel(JTable model) {
    this.instructionTable = model;
  }
}
