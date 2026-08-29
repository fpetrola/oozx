/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.z80;

import com.fpetrola.z80.cpu.DefaultInstructionExecutor;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every arithmetic and logic instruction against the Z80 as documented, over every input.
 * <p>
 * WHY THIS EXISTS BESIDE THE FUSE SUITE. Fuse's tests are one vector per opcode, and one vector
 * cannot tell a correct instruction from one that happens to agree with it. Four real faults got
 * through it here, each wrong across most of the input space and each passing on the one vector
 * that exists for it:
 * <ul>
 *   <li>LD A,R and LD A,I took their sign and zero from the flags register instead of from the
 *       byte loaded, and the one vector has R and the incoming flags agreeing on every bit that
 *       would have told them apart.</li>
 *   <li>RRD never seeded the flags, so the carry it is supposed to preserve came out of bit 0 of
 *       the byte at (HL); the one vector arrives with that bit clear and the carry clear, which
 *       is the one case where the two are indistinguishable. Its twin RLD was right.</li>
 *   <li>CPI seeded the flags from the "BC is not zero" boolean, so the carry it must preserve
 *       came from how much of the block was left.</li>
 *   <li>ADC HL,ss and SBC HL,ss packed bit 13 of the result on top of bit 11 of the operand, so
 *       any result reaching 0x2000 reported a half carry it had not made; and they asked whether
 *       the untruncated result was zero, so 0000 + FFFF + carry was not called zero.</li>
 * </ul>
 * <p>
 * So this walks the whole space - every accumulator against every operand against both carries,
 * 131072 combinations an instruction - and compares against a reference written here from the
 * published behaviour rather than from the emulator. Undocumented bits 3 and 5 included, because
 * that is where the gaps hide and games do read them.
 * <p>
 * Through the processor, never against a flag table directly: a table takes its arguments by a
 * convention of its own, and checking one with the arguments the wrong way round is a test that
 * passes while the instruction is broken. What a game sees is A and F after the opcode.
 */
@DisplayName("Every ALU instruction against the documented Z80")
class AluReferenceTest {

  private static final int S = 0x80, Z = 0x40, F5 = 0x20, H = 0x10, F3 = 0x08, PV = 0x04,
      N = 0x02, C = 0x01;

  private OOZ80 cpu;
  private Memory memory;
  private Register pc, af, bc, de, hl;

  @BeforeEach
  void buildProcessor() {
    memory = new MockedMemory(true);
    State state = new State(new IO() {
      public int in(int port) {
        return 0xFF;
      }

      public void out(int port, int value) {
      }
    }, memory);
    DefaultInstructionFactory factory = new DefaultInstructionFactory(state);
    cpu = new OOZ80(state, new DefaultInstructionFetcher(state, factory, false, false),
        new DefaultInstructionExecutor(state, false));
    pc = state.getPc();
    af = state.getRegister(RegisterName.AF);
    bc = state.getRegister(RegisterName.BC);
    de = state.getRegister(RegisterName.DE);
    hl = state.getRegister(RegisterName.HL);
  }

  /** What an instruction should leave in the accumulator and the flags. */
  private interface Reference {
    int[] expect(int a, int operand, int carryIn);
  }

  // ------------------------------------------------------------------ the checks

  @Test
  @DisplayName("arithmetic on the accumulator")
  void arithmetic() {
    List<String> wrong = new ArrayList<>();
    overEverything(wrong, "ADD A,B", new int[]{0x80}, AluReferenceTest::add);
    overEverything(wrong, "ADC A,B", new int[]{0x88}, AluReferenceTest::adc);
    overEverything(wrong, "SUB B", new int[]{0x90}, AluReferenceTest::sub);
    overEverything(wrong, "SBC A,B", new int[]{0x98}, AluReferenceTest::sbc);
    overEverything(wrong, "CP B", new int[]{0xB8}, AluReferenceTest::cp);
    overEverything(wrong, "NEG", new int[]{0xED, 0x44}, (a, n, c) -> neg(a));
    overEverything(wrong, "INC A", new int[]{0x3C}, (a, n, c) -> inc(a, c));
    overEverything(wrong, "DEC A", new int[]{0x3D}, (a, n, c) -> dec(a, c));
    overEverything(wrong, "DAA", new int[]{0x27}, AluReferenceTest::daa);
    assertNone(wrong);
  }

  @Test
  @DisplayName("logic on the accumulator")
  void logic() {
    List<String> wrong = new ArrayList<>();
    overEverything(wrong, "AND B", new int[]{0xA0}, (a, n, c) -> logical(a & n, H));
    overEverything(wrong, "XOR B", new int[]{0xA8}, (a, n, c) -> logical(a ^ n, 0));
    overEverything(wrong, "OR B", new int[]{0xB0}, (a, n, c) -> logical(a | n, 0));
    overEverything(wrong, "CPL", new int[]{0x2F}, (a, n, c) -> cpl(a, c));
    overEverything(wrong, "SCF", new int[]{0x37}, (a, n, c) -> new int[]{a, (a & (F3 | F5)) | C});
    overEverything(wrong, "CCF", new int[]{0x3F},
        (a, n, c) -> new int[]{a, (a & (F3 | F5)) | (c != 0 ? H : C)});
    assertNone(wrong);
  }

  @Test
  @DisplayName("rotates of the accumulator")
  void accumulatorRotates() {
    List<String> wrong = new ArrayList<>();
    overEverything(wrong, "RLCA", new int[]{0x07},
        (a, n, c) -> rotatedAccumulator(((a << 1) | (a >> 7)), a & 0x80));
    overEverything(wrong, "RRCA", new int[]{0x0F},
        (a, n, c) -> rotatedAccumulator(((a >> 1) | (a << 7)), a & 1));
    overEverything(wrong, "RLA", new int[]{0x17},
        (a, n, c) -> rotatedAccumulator(((a << 1) | c), a & 0x80));
    overEverything(wrong, "RRA", new int[]{0x1F},
        (a, n, c) -> rotatedAccumulator(((a >> 1) | (c << 7)), a & 1));
    assertNone(wrong);
  }

  @Test
  @DisplayName("shifts and rotates of a register")
  void shifts() {
    List<String> wrong = new ArrayList<>();
    overRegister(wrong, "RLC B", new int[]{0xCB, 0x00}, (v, c) -> shifted((v << 1) | (v >> 7), v & 0x80));
    overRegister(wrong, "RRC B", new int[]{0xCB, 0x08}, (v, c) -> shifted((v >> 1) | (v << 7), v & 1));
    overRegister(wrong, "RL B", new int[]{0xCB, 0x10}, (v, c) -> shifted((v << 1) | c, v & 0x80));
    overRegister(wrong, "RR B", new int[]{0xCB, 0x18}, (v, c) -> shifted((v >> 1) | (c << 7), v & 1));
    overRegister(wrong, "SLA B", new int[]{0xCB, 0x20}, (v, c) -> shifted(v << 1, v & 0x80));
    overRegister(wrong, "SRA B", new int[]{0xCB, 0x28}, (v, c) -> shifted((v >> 1) | (v & 0x80), v & 1));
    overRegister(wrong, "SLL B", new int[]{0xCB, 0x30}, (v, c) -> shifted((v << 1) | 1, v & 0x80));
    overRegister(wrong, "SRL B", new int[]{0xCB, 0x38}, (v, c) -> shifted(v >> 1, v & 1));
    assertNone(wrong);
  }

  @Test
  @DisplayName("BIT, whose undocumented bits come from the operand")
  void bitTest() {
    List<String> wrong = new ArrayList<>();
    for (int bit = 0; bit < 8; bit++) {
      int which = bit;
      overRegister(wrong, "BIT " + bit + ",B", new int[]{0xCB, 0x40 + bit * 8}, (v, c) -> {
        boolean set = (v & (1 << which)) != 0;
        return new int[]{v, H | (v & (F3 | F5)) | (set ? 0 : Z | PV)
            | (set && which == 7 ? S : 0) | (c != 0 ? C : 0)};
      });
    }
    assertNone(wrong);
  }

  @Test
  @DisplayName("sixteen-bit addition, where the half carry comes from bit eleven")
  void sixteenBit() {
    List<String> wrong = new ArrayList<>();
    int[] lows = {0x00, 0x01, 0x7F, 0x80, 0xFF};
    for (int high = 0; high < 256; high++) {
      for (int otherHigh = 0; otherHigh < 256; otherHigh++) {
        for (int low : lows) {
          int left = (high << 8) | low, right = (otherHigh << 8) | (0xFF - low);
          // ADD HL,DE leaves sign, zero and overflow alone; every flag goes in set so that
          // any it wrongly clears shows up.
          checkSixteen(wrong, "ADD HL,DE", new int[]{0x19}, left, right, 0xFF, 0, () -> {
            int r = left + right;
            return new int[]{r & 0xFFFF, (0xFF & (S | Z | PV)) | ((r >> 8) & (F3 | F5))
                | (((left & 0x0FFF) + (right & 0x0FFF)) > 0x0FFF ? H : 0) | (r > 0xFFFF ? C : 0)};
          });
          for (int carry = 0; carry < 2; carry++) {
            int carryIn = carry;
            checkSixteen(wrong, "ADC HL,DE", new int[]{0xED, 0x5A}, left, right, carryIn, carryIn, () -> {
              int r = left + right + carryIn;
              return new int[]{r & 0xFFFF, (((r & 0xFFFF) >> 8) & (S | F3 | F5))
                  | ((r & 0xFFFF) == 0 ? Z : 0)
                  | (((left & 0x0FFF) + (right & 0x0FFF) + carryIn) > 0x0FFF ? H : 0)
                  | ((((left ^ ~right) & (left ^ r)) & 0x8000) != 0 ? PV : 0)
                  | (r > 0xFFFF ? C : 0)};
            });
            checkSixteen(wrong, "SBC HL,DE", new int[]{0xED, 0x52}, left, right, carryIn, carryIn, () -> {
              int r = left - right - carryIn;
              return new int[]{r & 0xFFFF, (((r & 0xFFFF) >> 8) & (S | F3 | F5))
                  | ((r & 0xFFFF) == 0 ? Z : 0) | N
                  | (((left & 0x0FFF) - (right & 0x0FFF) - carryIn) < 0 ? H : 0)
                  | ((((left ^ right) & (left ^ r)) & 0x8000) != 0 ? PV : 0)
                  | (r < 0 ? C : 0)};
            });
          }
        }
      }
    }
    assertNone(wrong);
  }

  @Test
  @DisplayName("the digit rotates, which must leave the carry where it was")
  void digitRotates() {
    List<String> wrong = new ArrayList<>();
    for (boolean left : new boolean[]{true, false}) {
      String name = left ? "RLD" : "RRD";
      int[] opcode = left ? new int[]{0xED, 0x6F} : new int[]{0xED, 0x67};
      for (int a = 0; a < 256; a++) {
        for (int atHl = 0; atHl < 256; atHl++) {
          for (int carry = 0; carry < 2; carry++) {
            hl.write(0x9000);
            memory.write(0x9000, atHl);
            int[] got = run(opcode, a, 0, carry);
            int wantA = left ? (a & 0xF0) | ((atHl >> 4) & 0x0F) : (a & 0xF0) | (atHl & 0x0F);
            int wantF = (wantA & (S | F3 | F5)) | (wantA == 0 ? Z : 0) | parity(wantA)
                | (carry != 0 ? C : 0);
            note(wrong, name, got, new int[]{wantA, wantF},
                String.format("A=%02x (HL)=%02x C=%d", a, atHl, carry));
          }
        }
      }
    }
    assertNone(wrong);
  }

  @Test
  @DisplayName("the block compare, which must leave the carry where it was")
  void blockCompare() {
    List<String> wrong = new ArrayList<>();
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int left : new int[]{1, 2}) {
          for (int carry = 0; carry < 2; carry++) {
            hl.write(0x9000);
            memory.write(0x9000, value);
            bc.write(left);
            int[] got = run(new int[]{0xED, 0xA1}, a, 0, carry);
            int r = (a - value) & 0xFF;
            boolean half = ((a & 0xF) - (value & 0xF)) < 0;
            int t = (r - (half ? 1 : 0)) & 0xFF;
            int wantF = (r & S) | (r == 0 ? Z : 0) | N | (carry != 0 ? C : 0)
                | (half ? H : 0) | ((t & 0x08) != 0 ? F3 : 0) | ((t & 0x02) != 0 ? F5 : 0)
                | (left - 1 != 0 ? PV : 0);
            // CPI leaves A alone; only the flags are at stake.
            note(wrong, "CPI", new int[]{a, got[1]}, new int[]{a, wantF},
                String.format("A=%02x (HL)=%02x BC=%d C=%d", a, value, left, carry));
          }
        }
      }
    }
    assertNone(wrong);
  }

  @Test
  @DisplayName("the block move, whose undocumented bits come from A plus what it moved")
  void blockMove() {
    List<String> wrong = new ArrayList<>();
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int left : new int[]{1, 2}) {
          hl.write(0x9000);
          de.write(0x9100);
          memory.write(0x9000, value);
          bc.write(left);
          int[] got = run(new int[]{0xED, 0xA0}, a, 0, 1);
          int n = (a + value) & 0xFF;
          int wantF = (0xFF & (S | Z | C) & (S | Z | C))
              | ((n & 0x08) != 0 ? F3 : 0) | ((n & 0x02) != 0 ? F5 : 0)
              | (left - 1 != 0 ? PV : 0);
          // Sign, zero and carry are left as they were, and they went in as carry only.
          wantF = (wantF & ~(S | Z)) | C;
          note(wrong, "LDI", new int[]{a, got[1]}, new int[]{a, wantF},
              String.format("A=%02x (HL)=%02x BC=%d", a, value, left));
        }
      }
    }
    assertNone(wrong);
  }

  // ------------------------------------------------------------------ running one

  private void overEverything(List<String> wrong, String name, int[] opcode, Reference reference) {
    for (int a = 0; a < 256; a++) {
      for (int operand = 0; operand < 256; operand++) {
        for (int carry = 0; carry < 2; carry++) {
          note(wrong, name, run(opcode, a, operand, carry), reference.expect(a, operand, carry),
              String.format("A=%02x B=%02x C=%d", a, operand, carry));
        }
      }
    }
  }

  private interface RegisterReference {
    int[] expect(int value, int carryIn);
  }

  private void overRegister(List<String> wrong, String name, int[] opcode, RegisterReference reference) {
    for (int value = 0; value < 256; value++) {
      for (int carry = 0; carry < 2; carry++) {
        int[] got = runOnB(opcode, value, carry);
        note(wrong, name, got, reference.expect(value, carry),
            String.format("B=%02x C=%d", value, carry));
      }
    }
  }

  private void checkSixteen(List<String> wrong, String name, int[] opcode, int left, int right,
                            int flagsIn, int carryIn, java.util.function.Supplier<int[]> reference) {
    load(opcode);
    af.write(flagsIn & 0xFF);
    hl.write(left);
    de.write(right);
    cpu.execute();
    int[] want = reference.get();
    note(wrong, name, new int[]{hl.read() & 0xFFFF, af.read() & 0xFF}, want,
        String.format("HL=%04x DE=%04x C=%d", left, right, carryIn));
  }

  private void note(List<String> wrong, String name, int[] got, int[] want, String inputs) {
    if (got[0] != want[0] || got[1] != want[1]) {
      if (wrong.stream().noneMatch(one -> one.startsWith(name + " "))) {
        wrong.add(String.format("%s with %s gave %02x/%02x, the Z80 gives %02x/%02x (flags differ in %s)",
            name, inputs, got[0], got[1], want[0], want[1], flagNames(got[1] ^ want[1])));
      }
    }
  }

  private void assertNone(List<String> wrong) {
    assertTrue(wrong.isEmpty(), () -> "\n  " + String.join("\n  ", wrong) + "\n");
  }

  private void load(int[] opcode) {
    int at = 0x8000;
    for (int b : opcode) {
      memory.write(at++, b);
    }
    memory.write(at, 0x00);
    pc.write(0x8000);
  }

  private int[] run(int[] opcode, int a, int operand, int carry) {
    load(opcode);
    af.write((a << 8) | (carry != 0 ? C : 0));
    bc.write((operand << 8) | (bc.read() & 0xFF));
    cpu.execute();
    return new int[]{(af.read() >> 8) & 0xFF, af.read() & 0xFF};
  }

  private int[] runOnB(int[] opcode, int value, int carry) {
    load(opcode);
    af.write(carry != 0 ? C : 0);
    bc.write((value << 8));
    cpu.execute();
    return new int[]{(bc.read() >> 8) & 0xFF, af.read() & 0xFF};
  }

  private static String flagNames(int mask) {
    String[] names = {"C", "N", "P/V", "bit3", "H", "bit5", "Z", "S"};
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      if ((mask & (1 << i)) != 0) {
        out.append(names[i]).append(" ");
      }
    }
    return out.length() == 0 ? "none - the result itself differs" : out.toString().trim();
  }

  // ------------------------------------------------------------------ the Z80, as documented

  private static int sz53(int v) {
    return (v & (F3 | F5 | S)) | (v == 0 ? Z : 0);
  }

  private static int parity(int v) {
    int set = 0;
    for (int i = 0; i < 8; i++) {
      if ((v & (1 << i)) != 0) {
        set++;
      }
    }
    return (set & 1) == 0 ? PV : 0;
  }

  private static int[] add(int a, int n, int carryIn) {
    return added(a, n, 0);
  }

  private static int[] adc(int a, int n, int carryIn) {
    return added(a, n, carryIn);
  }

  private static int[] added(int a, int n, int carry) {
    int r = a + n + carry;
    return new int[]{r & 0xFF, sz53(r & 0xFF)
        | (((a & 0xF) + (n & 0xF) + carry) > 0xF ? H : 0)
        | ((((a ^ ~n) & (a ^ r)) & 0x80) != 0 ? PV : 0)
        | (r > 0xFF ? C : 0)};
  }

  private static int[] sub(int a, int n, int carryIn) {
    return subtracted(a, n, 0);
  }

  private static int[] sbc(int a, int n, int carryIn) {
    return subtracted(a, n, carryIn);
  }

  private static int[] subtracted(int a, int n, int carry) {
    int r = a - n - carry;
    return new int[]{r & 0xFF, sz53(r & 0xFF) | N
        | (((a & 0xF) - (n & 0xF) - carry) < 0 ? H : 0)
        | ((((a ^ n) & (a ^ r)) & 0x80) != 0 ? PV : 0)
        | (r < 0 ? C : 0)};
  }

  /** CP takes its undocumented bits from the OPERAND, where everything else takes them from the result. */
  private static int[] cp(int a, int n, int carryIn) {
    int r = a - n;
    return new int[]{a, ((r & 0xFF) & S) | ((r & 0xFF) == 0 ? Z : 0) | (n & (F3 | F5)) | N
        | (((a & 0xF) - (n & 0xF)) < 0 ? H : 0)
        | ((((a ^ n) & (a ^ r)) & 0x80) != 0 ? PV : 0)
        | (r < 0 ? C : 0)};
  }

  private static int[] neg(int a) {
    int r = (0 - a) & 0xFF;
    return new int[]{r, sz53(r) | N | ((a & 0xF) != 0 ? H : 0)
        | (a == 0x80 ? PV : 0) | (a != 0 ? C : 0)};
  }

  private static int[] inc(int a, int carryIn) {
    int r = (a + 1) & 0xFF;
    return new int[]{r, sz53(r) | ((a & 0xF) == 0xF ? H : 0) | (r == 0x80 ? PV : 0)
        | (carryIn != 0 ? C : 0)};
  }

  private static int[] dec(int a, int carryIn) {
    int r = (a - 1) & 0xFF;
    return new int[]{r, sz53(r) | N | ((a & 0xF) == 0 ? H : 0) | (r == 0x7F ? PV : 0)
        | (carryIn != 0 ? C : 0)};
  }

  private static int[] logical(int r, int half) {
    return new int[]{r & 0xFF, sz53(r & 0xFF) | parity(r & 0xFF) | half};
  }

  private static int[] cpl(int a, int carryIn) {
    int r = a ^ 0xFF;
    return new int[]{r, (r & (F3 | F5)) | H | N | (carryIn != 0 ? C : 0)};
  }

  /** Entered with N and H clear, which is where it is after an addition. */
  private static int[] daa(int a, int n, int carryIn) {
    int add = 0;
    int carryOut = carryIn;
    if ((a & 0x0F) > 9) {
      add = 6;
    }
    if (carryIn != 0 || a > 0x99) {
      add |= 0x60;
      carryOut = 1;
    }
    int r = (a + add) & 0xFF;
    return new int[]{r, sz53(r) | parity(r) | ((a & 0x0F) > 9 ? H : 0) | (carryOut != 0 ? C : 0)};
  }

  /** The accumulator rotates leave sign, zero and overflow alone; here they went in clear. */
  private static int[] rotatedAccumulator(int r, int carryOut) {
    return new int[]{r & 0xFF, ((r & 0xFF) & (F3 | F5)) | (carryOut != 0 ? C : 0)};
  }

  private static int[] shifted(int r, int carryOut) {
    return new int[]{r & 0xFF, sz53(r & 0xFF) | parity(r & 0xFF) | (carryOut != 0 ? C : 0)};
  }
}
