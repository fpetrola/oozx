// Written by the build from the model, not by hand. Model: 1b50889c6ce37375

package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.DecodedMemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.DecodedMemoryBus.Covering;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.memory.Registers;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.memory.Storage;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.State.InterruptionMode;
import com.fpetrola.z80.generate.GeneratedCore;
import com.fpetrola.z80.registers.UnrolledRegisterBank;
import com.fpetrola.z80.tstates.Contention.Kind;

/** Generated from the OOP model by the generator: the instructions, MEMPTR and the contention, flattened. Do not edit; regenerate. */
public class GeneratedSpectrumZ80 extends UnrolledRegisterBank implements GeneratedCore {
  protected final DecodedMemoryBus ram;
  protected final Ula ula;
  protected final SpectrumZ80Clock clock;
  protected final Display display;
  protected final IO io;
  protected final byte[] noMreqRun2;
  protected final byte[] noMreqRun3;
  protected final byte[] noMreqRun4;
  protected final byte[] noMreqRun5;
  protected final byte[] noMreqRun6;
  protected final byte[] noMreqRun7;
  protected final Covering[] reading;
  protected final Covering[] writing;
  protected State state;

  static final int FLAG_C = 0x0001;
  static final int FLAG_N = 0x0002;
  static final int FLAG_P = 0x0004;
  static final int FLAG_V = 0x0004;
  static final int FLAG_3 = 0x0008;
  static final int FLAG_H = 0x0010;
  static final int FLAG_5 = 0x0020;
  static final int FLAG_Z = 0x0040;
  static final int FLAG_S = 0x0080;
  /**
 * Indexed by the three carry bits an addition or a subtraction leaves.
 */
static final int[] HALF_CARRY_ADD = { 0, FLAG_H, FLAG_H, FLAG_H, 0, 0, 0, FLAG_H };
  static final int[] HALF_CARRY_SUB = { 0, 0, FLAG_H, 0, FLAG_H, 0, FLAG_H, FLAG_H };
  static final int[] OVERFLOW_ADD = { 0, 0, 0, FLAG_V, FLAG_V, 0, 0, 0 };
  static final int[] OVERFLOW_SUB = { 0, FLAG_V, 0, 0, 0, 0, FLAG_V, 0 };
  /**
 * One entry per byte: the sign and the two undocumented bits, the parity, and both together.
 * Zero is not in them - it is asked of the whole value, because a caller can hand this a 16 bit
 * result whose low byte happens to be zero and that is not a zero result.
 */
static final int[] SZ53 = new int[0x100];
  static final int[] PARITY = new int[0x100];
  static final int[] SZ53P = new int[0x100];
  static {
      for (int i = 0; i < 0x100; i++) {
          int bits = 0;
          for (int bit = i; bit != 0; bit >>= 1) {
              bits ^= bit & 1;
          }
          PARITY[i] = bits != 0 ? 0 : FLAG_P;
          SZ53[i] = i & (FLAG_3 | FLAG_5 | FLAG_S);
          SZ53P[i] = SZ53[i] | PARITY[i];
      }
  }

  private int _nextPC159 = -1;
  private int _nextPC871 = -1;
  private int _nextPC2223 = -1;

  public GeneratedSpectrumZ80(DecodedMemoryBus ram, Ula ula, SpectrumZ80Clock clock, Display display, IO io, byte[] noMreqRun2, byte[] noMreqRun3, byte[] noMreqRun4, byte[] noMreqRun5, byte[] noMreqRun6, byte[] noMreqRun7, Covering[] reading, Covering[] writing) {
    this.ram = ram;
    this.ula = ula;
    this.clock = clock;
    this.display = display;
    this.io = io;
    this.noMreqRun2 = noMreqRun2;
    this.noMreqRun3 = noMreqRun3;
    this.noMreqRun4 = noMreqRun4;
    this.noMreqRun5 = noMreqRun5;
    this.noMreqRun6 = noMreqRun6;
    this.noMreqRun7 = noMreqRun7;
    this.reading = reading;
    this.writing = writing;
  }

  public void attach(State state) {
    this.state = state;
  }

  private int read(int address, int fetching) {
      Covering covering_6401 = reading[address >>> 11];
      if (covering_6401.contended) {
          int tStates_6402 = clock.getTStates();
          clock.contend(Kind.READ, ula.contention.delay[(tStates_6402 > 0x45FFF ? 0x45FFF : tStates_6402)]);
      }
      int value_6403;
      if (covering_6401.bytes != null) {
          value_6403 = covering_6401.bytes[covering_6401.base + (address & 0x7FF)] & 0xff;
      } else {
          int read_6404;
          if (covering_6401.memory instanceof Storage) {
              read_6404 = (((Storage) covering_6401.memory).bytes[(covering_6401.base + (address & 0x7FF))] & 0xff);
          } else {
              read_6404 = ((Registers) covering_6401.memory).read(covering_6401.base + (address & 0x7FF));
          }
          value_6403 = read_6404;
      }
      clock.addTStates(fetching == 1 ? 4 : 3);
      return value_6403;
  }

  private void write(int address, int value) {
      Covering covering_6405 = writing[address >>> 11];
      if (covering_6405.contended) {
          int tStates_6406 = clock.getTStates();
          clock.contend(Kind.WRITE, ula.contention.delay[(tStates_6406 > 0x45FFF ? 0x45FFF : tStates_6406)]);
      }
      if (covering_6405.memory instanceof Rom) {
          if (((Rom) covering_6405.memory).protection != null && ((Rom) covering_6405.memory).protection.writableRoms) {
              ((Rom) covering_6405.memory).bytes[(covering_6405.base + (address & 0x7FF))] = ((byte) (value & 0xff));
          }
      } else if (covering_6405.memory instanceof Ram) {
          int offset_6407 = covering_6405.base + (address & 0x7FF);
          byte value_6408 = ((byte) (value & 0xff));
          done_6409: {
              if (((Ram) covering_6405.memory).writeProtected) {
                  break done_6409;
              }
              if (((Ram) covering_6405.memory).shownTo != null && offset_6407 < 0x1B00 && ((Ram) covering_6405.memory).bytes[offset_6407] != value_6408) {
                  ((Ram) covering_6405.memory).shownTo.accept(offset_6407);
              }
              ((Ram) covering_6405.memory).bytes[offset_6407] = value_6408;
          }
      } else {
          ((Registers) covering_6405.memory).write(covering_6405.base + (address & 0x7FF), ((byte) (value & 0xff)));
      }
      clock.addTStates(3);
  }

  private void contend2x1(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6410 = clock.getTStates();
          clock.addTStates(noMreqRun2[(tStates_6410 > 0x45FFF ? 0x45FFF : tStates_6410)]);
      } else {
          clock.addTStates(2);
      }
  }

  private void contend7x1(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6411 = clock.getTStates();
          clock.addTStates(noMreqRun7[(tStates_6411 > 0x45FFF ? 0x45FFF : tStates_6411)]);
      } else {
          clock.addTStates(7);
      }
  }

  private void contend1x1(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6412 = clock.getTStates();
          clock.contend(com.fpetrola.z80.tstates.Contention.Kind.READ_NO_MREQ, ula.contention.delayNoMreq[(tStates_6412 > 0x45FFF ? 0x45FFF : tStates_6412)] + 1);
      } else {
          clock.addTStates(1);
      }
  }

  private void contend5x1(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6413 = clock.getTStates();
          clock.addTStates(noMreqRun5[(tStates_6413 > 0x45FFF ? 0x45FFF : tStates_6413)]);
      } else {
          clock.addTStates(5);
      }
  }

  private void contend1x3(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6414 = clock.getTStates();
          clock.contend(com.fpetrola.z80.tstates.Contention.Kind.READ_NO_MREQ, ula.contention.delayNoMreq[(tStates_6414 > 0x45FFF ? 0x45FFF : tStates_6414)] + 3);
      } else {
          clock.addTStates(3);
      }
  }

  private void contend4x1(int address) {
      if ((reading[address >>> 11].contended)) {
          int tStates_6415 = clock.getTStates();
          clock.addTStates(noMreqRun4[(tStates_6415 > 0x45FFF ? 0x45FFF : tStates_6415)]);
      } else {
          clock.addTStates(4);
      }
  }

  public void step() {
      R = (R + 1 & 0x7f) | regRBit7;
      decode(read(PC, 1));
  }

  private void decode(int opcode) {
    switch (opcode >> 3) {
      case 0: decode_0(opcode);
        break;
      case 1: decode_1(opcode);
        break;
      case 2: decode_2(opcode);
        break;
      case 3: decode_3(opcode);
        break;
      case 4: decode_4(opcode);
        break;
      case 5: decode_5(opcode);
        break;
      case 6: decode_6(opcode);
        break;
      case 7: decode_7(opcode);
        break;
      case 8: decode_8(opcode);
        break;
      case 9: decode_9(opcode);
        break;
      case 10: decode_10(opcode);
        break;
      case 11: decode_11(opcode);
        break;
      case 12: decode_12(opcode);
        break;
      case 13: decode_13(opcode);
        break;
      case 14: decode_14(opcode);
        break;
      case 15: decode_15(opcode);
        break;
      case 16: decode_16(opcode);
        break;
      case 17: decode_17(opcode);
        break;
      case 18: decode_18(opcode);
        break;
      case 19: decode_19(opcode);
        break;
      case 20: decode_20(opcode);
        break;
      case 21: decode_21(opcode);
        break;
      case 22: decode_22(opcode);
        break;
      case 23: decode_23(opcode);
        break;
      case 24: decode_24(opcode);
        break;
      case 25: decode_25(opcode);
        break;
      case 26: decode_26(opcode);
        break;
      case 27: decode_27(opcode);
        break;
      case 28: decode_28(opcode);
        break;
      case 29: decode_29(opcode);
        break;
      case 30: decode_30(opcode);
        break;
      case 31: decode_31(opcode);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_0(int opcode) {
    switch (opcode) {
      case 0x00: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x01: {
          int address_1 = (PC + 1) & 0xFFFF;
          int operand_3 = read(address_1, 0);
          int operand_5 = read((address_1 + 1) & 0xFFFF, 0);
          int value_6 = ((operand_5 << 8) | operand_3);
          B = (value_6 >>> 8);
          C = value_6 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _address3 = (B << 8) | C;
          write(_address3, A);
          MEMPTR = ((A << 8) | ((_address3 + 1) & 0xff));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x03: {
          int read_7 = ((B << 8) | C);
          int value_8 = (read_7 + 1) & 0xFFFF;
          B = (value_8 >>> 8);
          C = value_8 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F6;
          int value1_9 = B;
          int value2_10 = F;
          _F6 = value2_10;
          value1_9++;
          value1_9 &= 0xff;
          _F6 = (_F6 & 1) | (value1_9 == 0x80 ? 4 : 0) | ((value1_9 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_9 & 0xff] | (value1_9 == 0 ? 0x40 : 0));
          int result_12 = value1_9 & 0xFF;
          F = (_F6 & 0xFF);
          B = result_12;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F8;
          int value1_13 = B;
          int value2_14 = F;
          _F8 = value2_14;
          _F8 = (_F8 & 1) | ((value1_13 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_13--;
          value1_13 &= 0xff;
          _F8 |= (value1_13 == 0x7f ? 4 : 0) | (SZ53[value1_13 & 0xff] | (value1_13 == 0 ? 0x40 : 0));
          int result_16 = value1_13 & 0xFF;
          F = (_F8 & 0xFF);
          B = result_16;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x06: {
          int operand_17 = read((PC + 1) & 0xFFFF, 0);
          B = operand_17;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F11;
          int value1_18 = A;
          int value2_19 = F;
          _F11 = value2_19;
          value1_18 = (value1_18 << 1) | (value1_18 >> 7);
          _F11 = (_F11 & 0xC4) | (value1_18 & 0x29);
          int result_21 = value1_18 & 0xFF;
          F = _F11;
          A = result_21;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_1(int opcode) {
    switch (opcode) {
      case 0x08: {
          int v1_22 = ((A << 8) | F);
          int v2_23 = _AF;
          A = (v2_23 >>> 8);
          F = v2_23 & 0xFF;
          _AF = v1_22;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F14;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_24 = ((H << 8) | L);
          int b_25 = ((B << 8) | C);
          int result_26 = (a_24 + b_25);
          int value1_27 = ((a_24 & 0x0800) >> 4 | result_26 >> 11);
          int value2_28 = F;
          int value3_29 = (b_25 >> 11) & 1;
          _F14 = value2_28;
          int add16temp_30 = value1_27 << 11;
          int lookup_31 = (((value1_27 << 4) & 0x0800) >> 11) | ((value3_29 << 11) >> 10) | ((add16temp_30 & 0x0800) >> 9);
          _F14 = (_F14 & 0xC4) | ((add16temp_30 & 0x10000) != 0 ? 1 : 0) | ((add16temp_30 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_31];
          F = (_F14 & 0xFF);
          int value_33 = (result_26 & 0xffff);
          H = (value_33 >>> 8);
          L = value_33 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _address16 = (B << 8) | C;
          int value_34 = read(_address16, 0);
          A = value_34;
          MEMPTR = ((_address16 + 1) & 0xFFFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int value_35 = (((B << 8) | C) - 1) & 0xFFFF;
          B = (value_35 >>> 8);
          C = value_35 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F19;
          int value1_36 = C;
          int value2_37 = F;
          _F19 = value2_37;
          value1_36++;
          value1_36 &= 0xff;
          _F19 = (_F19 & 1) | (value1_36 == 0x80 ? 4 : 0) | ((value1_36 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_36 & 0xff] | (value1_36 == 0 ? 0x40 : 0));
          int result_39 = value1_36 & 0xFF;
          F = (_F19 & 0xFF);
          C = result_39;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F21;
          int value1_40 = C;
          int value2_41 = F;
          _F21 = value2_41;
          _F21 = (_F21 & 1) | ((value1_40 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_40--;
          value1_40 &= 0xff;
          _F21 |= (value1_40 == 0x7f ? 4 : 0) | (SZ53[value1_40 & 0xff] | (value1_40 == 0 ? 0x40 : 0));
          int result_43 = value1_40 & 0xFF;
          F = (_F21 & 0xFF);
          C = result_43;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int operand_44 = read((PC + 1) & 0xFFFF, 0);
          C = operand_44;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F24;
          int value1_45 = A;
          int value2_46 = F;
          _F24 = value2_46;
          _F24 = (_F24 & 0xC4) | (value1_45 & 1);
          value1_45 = (value1_45 >> 1) | (value1_45 << 7);
          _F24 |= (value1_45 & 0x28);
          int result_48 = (value1_45 & 0xff);
          F = (_F24 & 0xFF);
          A = result_48;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_2(int opcode) {
    switch (opcode) {
      case 0x10: {
          contend1x1(((I << 8) | R));
          B = (B - 1) & 0xFF;
          if ((B != 0)) {
              int operand_50 = read((PC + 1) & 0xFFFF, 0);
              int jumpAddress2_49 = ((PC + 2 + (byte) operand_50) & 0xFFFF);
              MEMPTR = jumpAddress2_49;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_49;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0x11: {
          int address_52 = (PC + 1) & 0xFFFF;
          int operand_54 = read(address_52, 0);
          int operand_56 = read((address_52 + 1) & 0xFFFF, 0);
          int value_57 = ((operand_56 << 8) | operand_54);
          D = (value_57 >>> 8);
          E = value_57 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _address28 = (D << 8) | E;
          write(_address28, A);
          MEMPTR = ((A << 8) | ((_address28 + 1) & 0xff));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x13: {
          int read_58 = ((D << 8) | E);
          int value_59 = (read_58 + 1) & 0xFFFF;
          D = (value_59 >>> 8);
          E = value_59 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F31;
          int value1_60 = D;
          int value2_61 = F;
          _F31 = value2_61;
          value1_60++;
          value1_60 &= 0xff;
          _F31 = (_F31 & 1) | (value1_60 == 0x80 ? 4 : 0) | ((value1_60 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_60 & 0xff] | (value1_60 == 0 ? 0x40 : 0));
          int result_63 = value1_60 & 0xFF;
          F = (_F31 & 0xFF);
          D = result_63;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F33;
          int value1_64 = D;
          int value2_65 = F;
          _F33 = value2_65;
          _F33 = (_F33 & 1) | ((value1_64 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_64--;
          value1_64 &= 0xff;
          _F33 |= (value1_64 == 0x7f ? 4 : 0) | (SZ53[value1_64 & 0xff] | (value1_64 == 0 ? 0x40 : 0));
          int result_67 = value1_64 & 0xFF;
          F = (_F33 & 0xFF);
          D = result_67;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x16: {
          int operand_68 = read((PC + 1) & 0xFFFF, 0);
          D = operand_68;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F36;
          int value1_69 = A;
          int value2_70 = F;
          _F36 = value2_70;
          int bytetemp_72 = value1_69;
          value1_69 = (value1_69 << 1) | (_F36 & 1);
          _F36 = (_F36 & 0xC4) | (value1_69 & 0x28) | (bytetemp_72 >> 7);
          int result_73 = value1_69 & 0xFF;
          F = (_F36 & 0xFF);
          A = result_73;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_3(int opcode) {
    switch (opcode) {
      case 0x18: {
          int _nextPC38;
          int operand_75 = read((PC + 1) & 0xFFFF, 0);
          int jumpAddress2_74 = ((PC + 2 + (byte) operand_75) & 0xFFFF);
          _nextPC38 = jumpAddress2_74;
          MEMPTR = _nextPC38;
          contend5x1((PC + 1) & 0xFFFF);
          PC = _nextPC38;
          break;
      }
      case 0x19: {
          int _F39;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_77 = ((H << 8) | L);
          int b_78 = ((D << 8) | E);
          int result_79 = (a_77 + b_78);
          int value1_80 = ((a_77 & 0x0800) >> 4 | result_79 >> 11);
          int value2_81 = F;
          int value3_82 = (b_78 >> 11) & 1;
          _F39 = value2_81;
          int add16temp_83 = value1_80 << 11;
          int lookup_84 = (((value1_80 << 4) & 0x0800) >> 11) | ((value3_82 << 11) >> 10) | ((add16temp_83 & 0x0800) >> 9);
          _F39 = (_F39 & 0xC4) | ((add16temp_83 & 0x10000) != 0 ? 1 : 0) | ((add16temp_83 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_84];
          F = (_F39 & 0xFF);
          int value_86 = (result_79 & 0xffff);
          H = (value_86 >>> 8);
          L = value_86 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _address41 = (D << 8) | E;
          int value_87 = read(_address41, 0);
          A = value_87;
          MEMPTR = ((_address41 + 1) & 0xFFFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int value_88 = (((D << 8) | E) - 1) & 0xFFFF;
          D = (value_88 >>> 8);
          E = value_88 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F44;
          int value1_89 = E;
          int value2_90 = F;
          _F44 = value2_90;
          value1_89++;
          value1_89 &= 0xff;
          _F44 = (_F44 & 1) | (value1_89 == 0x80 ? 4 : 0) | ((value1_89 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_89 & 0xff] | (value1_89 == 0 ? 0x40 : 0));
          int result_92 = value1_89 & 0xFF;
          F = (_F44 & 0xFF);
          E = result_92;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F46;
          int value1_93 = E;
          int value2_94 = F;
          _F46 = value2_94;
          _F46 = (_F46 & 1) | ((value1_93 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_93--;
          value1_93 &= 0xff;
          _F46 |= (value1_93 == 0x7f ? 4 : 0) | (SZ53[value1_93 & 0xff] | (value1_93 == 0 ? 0x40 : 0));
          int result_96 = value1_93 & 0xFF;
          F = (_F46 & 0xFF);
          E = result_96;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int operand_97 = read((PC + 1) & 0xFFFF, 0);
          E = operand_97;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F49;
          int value1_98 = A;
          int value2_99 = F;
          _F49 = value2_99;
          int A_101 = value1_98;
          int bytetemp_102 = A_101;
          A_101 = (A_101 >> 1) | (_F49 << 7);
          _F49 = (_F49 & 0xC4) | (A_101 & 0x28) | (bytetemp_102 & 1);
          int result_103 = A_101 & 0xFF;
          F = _F49;
          A = result_103;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_4(int opcode) {
    switch (opcode) {
      case 0x20: {
          if ((!((F & 0x40) == 0x40))) {
              int operand_105 = read((PC + 1) & 0xFFFF, 0);
              int jumpAddress2_104 = ((PC + 2 + (byte) operand_105) & 0xFFFF);
              MEMPTR = jumpAddress2_104;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_104;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0x21: {
          int address_107 = (PC + 1) & 0xFFFF;
          int operand_109 = read(address_107, 0);
          int operand_111 = read((address_107 + 1) & 0xFFFF, 0);
          int value_112 = ((operand_111 << 8) | operand_109);
          H = (value_112 >>> 8);
          L = value_112 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _address53;
          int address_113 = (PC + 1) & 0xFFFF;
          int operand_115 = read(address_113, 0);
          int operand_117 = read((address_113 + 1) & 0xFFFF, 0);
          _address53 = (operand_117 << 8) | operand_115;
          int value_118 = ((H << 8) | L);
          write(_address53, (value_118 & 0xFF));
          write((_address53 + 1) & 0xFFFF, (value_118 >>> 8));
          MEMPTR = ((_address53 + 1) & 0xFFFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x23: {
          int read_119 = ((H << 8) | L);
          int value_120 = (read_119 + 1) & 0xFFFF;
          H = (value_120 >>> 8);
          L = value_120 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F56;
          int value1_121 = H;
          int value2_122 = F;
          _F56 = value2_122;
          value1_121++;
          value1_121 &= 0xff;
          _F56 = (_F56 & 1) | (value1_121 == 0x80 ? 4 : 0) | ((value1_121 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_121 & 0xff] | (value1_121 == 0 ? 0x40 : 0));
          int result_124 = value1_121 & 0xFF;
          F = (_F56 & 0xFF);
          H = result_124;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F58;
          int value1_125 = H;
          int value2_126 = F;
          _F58 = value2_126;
          _F58 = (_F58 & 1) | ((value1_125 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_125--;
          value1_125 &= 0xff;
          _F58 |= (value1_125 == 0x7f ? 4 : 0) | (SZ53[value1_125 & 0xff] | (value1_125 == 0 ? 0x40 : 0));
          int result_128 = value1_125 & 0xFF;
          F = (_F58 & 0xFF);
          H = result_128;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x26: {
          int operand_129 = read((PC + 1) & 0xFFFF, 0);
          H = operand_129;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F64 = 0;
          int _F63 = 0;
          int _data62 = 0;
          int _F61;
          int value1_130 = A;
          int value2_131 = F;
          _F61 = value2_131;
          value1_130 &= 0xff;
          int add_133 = 0;
          int carry_134 = (_F61 & 1);
          if (((_F61 & 0x10) != 0) || ((value1_130 & 0x0f) > 9)) {
              add_133 = 6;
          }
          if (carry_134 != 0 || (value1_130 > 0x99)) {
              add_133 |= 0x60;
          }
          if (value1_130 > 0x99) {
              carry_134 = 1;
          }
          int and_136 = _F61 & 0xff;
          _data62 = and_136;
          if ((_F61 & 2) != 0) {
              int value1_137 = value1_130;
              int subtemp_140 = value1_137 - add_133;
              int lookup_141 = ((value1_137 & 0x88) >> 3) | ((add_133 & 0x88) >> 2) | ((subtemp_140 & 0x88) >> 1);
              value1_137 = subtemp_140 & 0xff;
              _F63 = ((subtemp_140 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_141 & 0x07)] | OVERFLOW_SUB[(lookup_141 >> 4)] | (SZ53[value1_137 & 0xff] | (value1_137 == 0 ? 0x40 : 0));
              int result_142 = value1_137 & 0xFF;
              int and_143 = (_F63 & 0xFF);
              _data62 = and_143;
              value1_130 = result_142;
          } else {
              int value2_145 = value1_130;
              int addtemp_147 = value2_145 + add_133;
              int lookup_148 = ((value2_145 & 0x88) >> 3) | ((add_133 & 0x88) >> 2) | ((addtemp_147 & 0x88) >> 1);
              value2_145 = addtemp_147 & 0xff;
              _F64 = ((addtemp_147 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_148 & 0x07)] | OVERFLOW_ADD[(lookup_148 >> 4)] | (SZ53[value2_145 & 0xff] | (value2_145 == 0 ? 0x40 : 0));
              int result_149 = value2_145 & 0xFF;
              int and_150 = (_F64 & 0xFF);
              _data62 = and_150;
              value1_130 = result_149;
          }
          _F61 = _data62;
          _F61 = (_F61 & -6) | carry_134 | PARITY[value1_130 & 0xff];
          int result_151 = value1_130 & 0xFF;
          F = (_F61 & 0xFF);
          A = result_151;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_5(int opcode) {
    switch (opcode) {
      case 0x28: {
          if (((F & 0x40) == 0x40)) {
              int operand_153 = read((PC + 1) & 0xFFFF, 0);
              int jumpAddress2_152 = ((PC + 2 + (byte) operand_153) & 0xFFFF);
              MEMPTR = jumpAddress2_152;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_152;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0x29: {
          int _F67;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_155 = ((H << 8) | L);
          int b_156 = ((H << 8) | L);
          int result_157 = (a_155 + b_156);
          int value1_158 = ((a_155 & 0x0800) >> 4 | result_157 >> 11);
          int value2_159 = F;
          int value3_160 = (b_156 >> 11) & 1;
          _F67 = value2_159;
          int add16temp_161 = value1_158 << 11;
          int lookup_162 = (((value1_158 << 4) & 0x0800) >> 11) | ((value3_160 << 11) >> 10) | ((add16temp_161 & 0x0800) >> 9);
          _F67 = (_F67 & 0xC4) | ((add16temp_161 & 0x10000) != 0 ? 1 : 0) | ((add16temp_161 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_162];
          F = (_F67 & 0xFF);
          int value_164 = (result_157 & 0xffff);
          H = (value_164 >>> 8);
          L = value_164 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _address69;
          int address_165 = (PC + 1) & 0xFFFF;
          int operand_167 = read(address_165, 0);
          int operand_169 = read((address_165 + 1) & 0xFFFF, 0);
          _address69 = (operand_169 << 8) | operand_167;
          int wordNumber1_170 = read(_address69, 0);
          int wordNumber_171 = read((_address69 + 1) & 0xFFFF, 0);
          int value_172 = ((wordNumber_171 << 8) | wordNumber1_170);
          H = (value_172 >>> 8);
          L = value_172 & 0xFF;
          MEMPTR = ((_address69 + 1) & 0xFFFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x2B: {
          int value_173 = (((H << 8) | L) - 1) & 0xFFFF;
          H = (value_173 >>> 8);
          L = value_173 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F72;
          int value1_174 = L;
          int value2_175 = F;
          _F72 = value2_175;
          value1_174++;
          value1_174 &= 0xff;
          _F72 = (_F72 & 1) | (value1_174 == 0x80 ? 4 : 0) | ((value1_174 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_174 & 0xff] | (value1_174 == 0 ? 0x40 : 0));
          int result_177 = value1_174 & 0xFF;
          F = (_F72 & 0xFF);
          L = result_177;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F74;
          int value1_178 = L;
          int value2_179 = F;
          _F74 = value2_179;
          _F74 = (_F74 & 1) | ((value1_178 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_178--;
          value1_178 &= 0xff;
          _F74 |= (value1_178 == 0x7f ? 4 : 0) | (SZ53[value1_178 & 0xff] | (value1_178 == 0 ? 0x40 : 0));
          int result_181 = value1_178 & 0xFF;
          F = (_F74 & 0xFF);
          L = result_181;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int operand_182 = read((PC + 1) & 0xFFFF, 0);
          L = operand_182;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F77;
          int value1_183 = A;
          int value2_184 = F;
          _F77 = value2_184;
          value1_183 ^= 0xff;
          _F77 = (_F77 & 0xC5) | (value1_183 & 0x28) | 0x12;
          int result_186 = value1_183 & 0xFF;
          F = _F77;
          A = result_186;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_6(int opcode) {
    switch (opcode) {
      case 0x30: {
          if ((!((F & 1) == 1))) {
              int operand_188 = read((PC + 1) & 0xFFFF, 0);
              int jumpAddress2_187 = ((PC + 2 + (byte) operand_188) & 0xFFFF);
              MEMPTR = jumpAddress2_187;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_187;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0x31: {
          int address_190 = (PC + 1) & 0xFFFF;
          int operand_192 = read(address_190, 0);
          int operand_194 = read((address_190 + 1) & 0xFFFF, 0);
          SP = ((operand_194 << 8) | operand_192);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _address81;
          int address_195 = (PC + 1) & 0xFFFF;
          int operand_197 = read(address_195, 0);
          int operand_199 = read((address_195 + 1) & 0xFFFF, 0);
          _address81 = (operand_199 << 8) | operand_197;
          write(_address81, A);
          MEMPTR = ((A << 8) | ((_address81 + 1) & 0xff));
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x33: {
          int read_200 = SP;
          SP = ((read_200 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F85;
          int _address84 = (H << 8) | L;
          int value1_201 = read(_address84, 0);
          int value2_202 = F;
          _F85 = value2_202;
          value1_201++;
          value1_201 &= 0xff;
          _F85 = (_F85 & 1) | (value1_201 == 0x80 ? 4 : 0) | ((value1_201 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_201 & 0xff] | (value1_201 == 0 ? 0x40 : 0));
          int result_204 = value1_201 & 0xFF;
          F = (_F85 & 0xFF);
          _address84 = (H << 8) | L;
          contend1x1(((H << 8) | L));
          write(_address84, result_204);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F87;
          int _address84 = (H << 8) | L;
          int value1_205 = read(_address84, 0);
          int value2_206 = F;
          _F87 = value2_206;
          _F87 = (_F87 & 1) | ((value1_205 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_205--;
          value1_205 &= 0xff;
          _F87 |= (value1_205 == 0x7f ? 4 : 0) | (SZ53[value1_205 & 0xff] | (value1_205 == 0 ? 0x40 : 0));
          int result_208 = value1_205 & 0xFF;
          F = (_F87 & 0xFF);
          _address84 = (H << 8) | L;
          contend1x1(((H << 8) | L));
          write(_address84, result_208);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _address84;
          int operand_209 = read((PC + 1) & 0xFFFF, 0);
          _address84 = (H << 8) | L;
          write(_address84, operand_209);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F90;
          int value2_211 = F;
          _F90 = value2_211;
          _F90 = _F90 & 0xC4 | A & 0x28 | 1;
          F = _F90;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_7(int opcode) {
    switch (opcode) {
      case 0x38: {
          if (((F & 1) == 1)) {
              int operand_215 = read((PC + 1) & 0xFFFF, 0);
              int jumpAddress2_214 = ((PC + 2 + (byte) operand_215) & 0xFFFF);
              MEMPTR = jumpAddress2_214;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_214;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0x39: {
          int _F93;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_217 = ((H << 8) | L);
          int result_218 = (a_217 + SP);
          int value1_219 = ((a_217 & 0x0800) >> 4 | result_218 >> 11);
          int value2_220 = F;
          int value3_221 = (SP >> 11) & 1;
          _F93 = value2_220;
          int add16temp_222 = value1_219 << 11;
          int lookup_223 = (((value1_219 << 4) & 0x0800) >> 11) | ((value3_221 << 11) >> 10) | ((add16temp_222 & 0x0800) >> 9);
          _F93 = (_F93 & 0xC4) | ((add16temp_222 & 0x10000) != 0 ? 1 : 0) | ((add16temp_222 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_223];
          F = (_F93 & 0xFF);
          int value_225 = (result_218 & 0xffff);
          H = (value_225 >>> 8);
          L = value_225 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _address95;
          int address_226 = (PC + 1) & 0xFFFF;
          int operand_228 = read(address_226, 0);
          int operand_230 = read((address_226 + 1) & 0xFFFF, 0);
          _address95 = (operand_230 << 8) | operand_228;
          int value_231 = read(_address95, 0);
          A = value_231;
          MEMPTR = ((_address95 + 1) & 0xFFFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x3B: {
          SP = ((SP - 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F98;
          int value1_232 = A;
          int value2_233 = F;
          _F98 = value2_233;
          value1_232++;
          value1_232 &= 0xff;
          _F98 = (_F98 & 1) | (value1_232 == 0x80 ? 4 : 0) | ((value1_232 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_232 & 0xff] | (value1_232 == 0 ? 0x40 : 0));
          int result_235 = value1_232 & 0xFF;
          F = (_F98 & 0xFF);
          A = result_235;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F100;
          int value1_236 = A;
          int value2_237 = F;
          _F100 = value2_237;
          _F100 = (_F100 & 1) | ((value1_236 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_236--;
          value1_236 &= 0xff;
          _F100 |= (value1_236 == 0x7f ? 4 : 0) | (SZ53[value1_236 & 0xff] | (value1_236 == 0 ? 0x40 : 0));
          int result_239 = value1_236 & 0xFF;
          F = (_F100 & 0xFF);
          A = result_239;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int operand_240 = read((PC + 1) & 0xFFFF, 0);
          A = operand_240;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F103;
          int value2_242 = F;
          _F103 = value2_242;
          _F103 = _F103 & 0xC4 | ((_F103 & 1) != 0 ? 0x10 : 1) | A & 0x28;
          F = _F103;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_8(int opcode) {
    switch (opcode) {
      case 0x40: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x41: {
          B = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x42: {
          B = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x43: {
          B = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x44: {
          B = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x45: {
          B = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _address84 = (H << 8) | L;
          int value_245 = read(_address84, 0);
          B = value_245;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x47: {
          B = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_9(int opcode) {
    switch (opcode) {
      case 0x48: {
          C = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x49: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4A: {
          C = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4B: {
          C = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4C: {
          C = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4D: {
          C = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _address84 = (H << 8) | L;
          int value_246 = read(_address84, 0);
          C = value_246;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x4F: {
          C = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_10(int opcode) {
    switch (opcode) {
      case 0x50: {
          D = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x51: {
          D = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x52: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x53: {
          D = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x54: {
          D = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x55: {
          D = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _address84 = (H << 8) | L;
          int value_247 = read(_address84, 0);
          D = value_247;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x57: {
          D = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_11(int opcode) {
    switch (opcode) {
      case 0x58: {
          E = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x59: {
          E = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5A: {
          E = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5B: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5C: {
          E = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5D: {
          E = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _address84 = (H << 8) | L;
          int value_248 = read(_address84, 0);
          E = value_248;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x5F: {
          E = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_12(int opcode) {
    switch (opcode) {
      case 0x60: {
          H = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x61: {
          H = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x62: {
          H = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x63: {
          H = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x64: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x65: {
          H = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _address84 = (H << 8) | L;
          int value_249 = read(_address84, 0);
          H = value_249;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x67: {
          H = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_13(int opcode) {
    switch (opcode) {
      case 0x68: {
          L = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x69: {
          L = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6A: {
          L = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6B: {
          L = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6C: {
          L = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6D: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _address84 = (H << 8) | L;
          int value_250 = read(_address84, 0);
          L = value_250;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x6F: {
          L = A;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_14(int opcode) {
    switch (opcode) {
      case 0x70: {
          int _address84 = (H << 8) | L;
          write(_address84, B);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _address84 = (H << 8) | L;
          write(_address84, C);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _address84 = (H << 8) | L;
          write(_address84, D);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address84 = (H << 8) | L;
          write(_address84, E);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _address84 = (H << 8) | L;
          write(_address84, H);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _address84 = (H << 8) | L;
          write(_address84, L);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x76: {
          if (!state.isHalted()) {
              state.setHalted(true);
              _nextPC159 = PC;
          }
          PC = _nextPC159 == -1 ? (PC + 1) & 0xFFFF : _nextPC159;
          break;
      }
      case 0x77: {
          int _address84 = (H << 8) | L;
          write(_address84, A);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_15(int opcode) {
    switch (opcode) {
      case 0x78: {
          A = B;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x79: {
          A = C;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7A: {
          A = D;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7B: {
          A = E;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7C: {
          A = H;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7D: {
          A = L;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _address84 = (H << 8) | L;
          int value_251 = read(_address84, 0);
          A = value_251;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x7F: {
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_16(int opcode) {
    switch (opcode) {
      case 0x80: {
          int _F169;
          int value1_252 = A;
          int value2_253 = B;
          int addtemp_255 = value2_253 + value1_252;
          int lookup_256 = ((value2_253 & 0x88) >> 3) | ((value1_252 & 0x88) >> 2) | ((addtemp_255 & 0x88) >> 1);
          value2_253 = addtemp_255 & 0xff;
          _F169 = ((addtemp_255 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_256 & 0x07)] | OVERFLOW_ADD[(lookup_256 >> 4)] | (SZ53[value2_253] | (value2_253 == 0 ? 0x40 : 0));
          F = (_F169 & 0xFF);
          A = value2_253;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _F171;
          int value1_258 = A;
          int value2_259 = C;
          int addtemp_261 = value2_259 + value1_258;
          int lookup_262 = ((value2_259 & 0x88) >> 3) | ((value1_258 & 0x88) >> 2) | ((addtemp_261 & 0x88) >> 1);
          value2_259 = addtemp_261 & 0xff;
          _F171 = ((addtemp_261 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_262 & 0x07)] | OVERFLOW_ADD[(lookup_262 >> 4)] | (SZ53[value2_259] | (value2_259 == 0 ? 0x40 : 0));
          F = (_F171 & 0xFF);
          A = value2_259;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _F173;
          int value1_264 = A;
          int value2_265 = D;
          int addtemp_267 = value2_265 + value1_264;
          int lookup_268 = ((value2_265 & 0x88) >> 3) | ((value1_264 & 0x88) >> 2) | ((addtemp_267 & 0x88) >> 1);
          value2_265 = addtemp_267 & 0xff;
          _F173 = ((addtemp_267 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_268 & 0x07)] | OVERFLOW_ADD[(lookup_268 >> 4)] | (SZ53[value2_265] | (value2_265 == 0 ? 0x40 : 0));
          F = (_F173 & 0xFF);
          A = value2_265;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _F175;
          int value1_270 = A;
          int value2_271 = E;
          int addtemp_273 = value2_271 + value1_270;
          int lookup_274 = ((value2_271 & 0x88) >> 3) | ((value1_270 & 0x88) >> 2) | ((addtemp_273 & 0x88) >> 1);
          value2_271 = addtemp_273 & 0xff;
          _F175 = ((addtemp_273 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_274 & 0x07)] | OVERFLOW_ADD[(lookup_274 >> 4)] | (SZ53[value2_271] | (value2_271 == 0 ? 0x40 : 0));
          F = (_F175 & 0xFF);
          A = value2_271;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _F177;
          int value1_276 = A;
          int value2_277 = H;
          int addtemp_279 = value2_277 + value1_276;
          int lookup_280 = ((value2_277 & 0x88) >> 3) | ((value1_276 & 0x88) >> 2) | ((addtemp_279 & 0x88) >> 1);
          value2_277 = addtemp_279 & 0xff;
          _F177 = ((addtemp_279 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_280 & 0x07)] | OVERFLOW_ADD[(lookup_280 >> 4)] | (SZ53[value2_277] | (value2_277 == 0 ? 0x40 : 0));
          F = (_F177 & 0xFF);
          A = value2_277;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _F179;
          int value1_282 = A;
          int value2_283 = L;
          int addtemp_285 = value2_283 + value1_282;
          int lookup_286 = ((value2_283 & 0x88) >> 3) | ((value1_282 & 0x88) >> 2) | ((addtemp_285 & 0x88) >> 1);
          value2_283 = addtemp_285 & 0xff;
          _F179 = ((addtemp_285 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_286 & 0x07)] | OVERFLOW_ADD[(lookup_286 >> 4)] | (SZ53[value2_283] | (value2_283 == 0 ? 0x40 : 0));
          F = (_F179 & 0xFF);
          A = value2_283;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _F181;
          int _address84 = (H << 8) | L;
          int sourceValue_288 = read(_address84, 0);
          int value1_289 = A;
          int value2_290 = sourceValue_288;
          int addtemp_292 = value2_290 + value1_289;
          int lookup_293 = ((value2_290 & 0x88) >> 3) | ((value1_289 & 0x88) >> 2) | ((addtemp_292 & 0x88) >> 1);
          value2_290 = addtemp_292 & 0xff;
          _F181 = ((addtemp_292 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_293 & 0x07)] | OVERFLOW_ADD[(lookup_293 >> 4)] | (SZ53[value2_290] | (value2_290 == 0 ? 0x40 : 0));
          F = (_F181 & 0xFF);
          A = value2_290;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _F183;
          int value1_295 = A;
          int value2_296 = A;
          int addtemp_298 = value2_296 + value1_295;
          int lookup_299 = ((value2_296 & 0x88) >> 3) | ((value1_295 & 0x88) >> 2) | ((addtemp_298 & 0x88) >> 1);
          value2_296 = addtemp_298 & 0xff;
          _F183 = ((addtemp_298 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_299 & 0x07)] | OVERFLOW_ADD[(lookup_299 >> 4)] | (SZ53[value2_296] | (value2_296 == 0 ? 0x40 : 0));
          F = (_F183 & 0xFF);
          A = value2_296;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_17(int opcode) {
    switch (opcode) {
      case 0x88: {
          int _F185;
          int value1_301 = A;
          int value3_303 = F & 1;
          _F185 = value3_303;
          int adctemp_304 = value1_301 + B + (_F185 & 1);
          int lookup_305 = ((value1_301 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((adctemp_304 & 0x88) >> 1);
          value1_301 = adctemp_304 & 0xff;
          _F185 = ((adctemp_304 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_305 & 0x07)] | OVERFLOW_ADD[(lookup_305 >> 4)] | (SZ53[value1_301] | (value1_301 == 0 ? 0x40 : 0));
          F = (_F185 & 0xFF);
          A = value1_301;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _F187;
          int value1_307 = A;
          int value3_309 = F & 1;
          _F187 = value3_309;
          int adctemp_310 = value1_307 + C + (_F187 & 1);
          int lookup_311 = ((value1_307 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((adctemp_310 & 0x88) >> 1);
          value1_307 = adctemp_310 & 0xff;
          _F187 = ((adctemp_310 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_311 & 0x07)] | OVERFLOW_ADD[(lookup_311 >> 4)] | (SZ53[value1_307] | (value1_307 == 0 ? 0x40 : 0));
          F = (_F187 & 0xFF);
          A = value1_307;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _F189;
          int value1_313 = A;
          int value3_315 = F & 1;
          _F189 = value3_315;
          int adctemp_316 = value1_313 + D + (_F189 & 1);
          int lookup_317 = ((value1_313 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((adctemp_316 & 0x88) >> 1);
          value1_313 = adctemp_316 & 0xff;
          _F189 = ((adctemp_316 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_317 & 0x07)] | OVERFLOW_ADD[(lookup_317 >> 4)] | (SZ53[value1_313] | (value1_313 == 0 ? 0x40 : 0));
          F = (_F189 & 0xFF);
          A = value1_313;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _F191;
          int value1_319 = A;
          int value3_321 = F & 1;
          _F191 = value3_321;
          int adctemp_322 = value1_319 + E + (_F191 & 1);
          int lookup_323 = ((value1_319 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((adctemp_322 & 0x88) >> 1);
          value1_319 = adctemp_322 & 0xff;
          _F191 = ((adctemp_322 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_323 & 0x07)] | OVERFLOW_ADD[(lookup_323 >> 4)] | (SZ53[value1_319] | (value1_319 == 0 ? 0x40 : 0));
          F = (_F191 & 0xFF);
          A = value1_319;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _F193;
          int value1_325 = A;
          int value3_327 = F & 1;
          _F193 = value3_327;
          int adctemp_328 = value1_325 + H + (_F193 & 1);
          int lookup_329 = ((value1_325 & 0x88) >> 3) | ((H & 0x88) >> 2) | ((adctemp_328 & 0x88) >> 1);
          value1_325 = adctemp_328 & 0xff;
          _F193 = ((adctemp_328 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_329 & 0x07)] | OVERFLOW_ADD[(lookup_329 >> 4)] | (SZ53[value1_325] | (value1_325 == 0 ? 0x40 : 0));
          F = (_F193 & 0xFF);
          A = value1_325;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _F195;
          int value1_331 = A;
          int value3_333 = F & 1;
          _F195 = value3_333;
          int adctemp_334 = value1_331 + L + (_F195 & 1);
          int lookup_335 = ((value1_331 & 0x88) >> 3) | ((L & 0x88) >> 2) | ((adctemp_334 & 0x88) >> 1);
          value1_331 = adctemp_334 & 0xff;
          _F195 = ((adctemp_334 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_335 & 0x07)] | OVERFLOW_ADD[(lookup_335 >> 4)] | (SZ53[value1_331] | (value1_331 == 0 ? 0x40 : 0));
          F = (_F195 & 0xFF);
          A = value1_331;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _F197;
          int _address84 = (H << 8) | L;
          int sourceValue_337 = read(_address84, 0);
          int value1_338 = A;
          int value3_340 = F & 1;
          _F197 = value3_340;
          int adctemp_341 = value1_338 + sourceValue_337 + (_F197 & 1);
          int lookup_342 = ((value1_338 & 0x88) >> 3) | ((sourceValue_337 & 0x88) >> 2) | ((adctemp_341 & 0x88) >> 1);
          value1_338 = adctemp_341 & 0xff;
          _F197 = ((adctemp_341 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_342 & 0x07)] | OVERFLOW_ADD[(lookup_342 >> 4)] | (SZ53[value1_338] | (value1_338 == 0 ? 0x40 : 0));
          F = (_F197 & 0xFF);
          A = value1_338;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _F199;
          int value1_344 = A;
          int value2_345 = A;
          int value3_346 = F & 1;
          _F199 = value3_346;
          int adctemp_347 = value1_344 + value2_345 + (_F199 & 1);
          int lookup_348 = ((value1_344 & 0x88) >> 3) | ((value2_345 & 0x88) >> 2) | ((adctemp_347 & 0x88) >> 1);
          value1_344 = adctemp_347 & 0xff;
          _F199 = ((adctemp_347 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_348 & 0x07)] | OVERFLOW_ADD[(lookup_348 >> 4)] | (SZ53[value1_344] | (value1_344 == 0 ? 0x40 : 0));
          F = (_F199 & 0xFF);
          A = value1_344;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_18(int opcode) {
    switch (opcode) {
      case 0x90: {
          int _F201;
          int value1_350 = A;
          int subtemp_353 = value1_350 - B;
          int lookup_354 = ((value1_350 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((subtemp_353 & 0x88) >> 1);
          value1_350 = subtemp_353 & 0xff;
          _F201 = ((subtemp_353 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_354 & 0x07)] | OVERFLOW_SUB[(lookup_354 >> 4)] | (SZ53[value1_350] | (value1_350 == 0 ? 0x40 : 0));
          F = (_F201 & 0xFF);
          A = value1_350;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _F203;
          int value1_356 = A;
          int subtemp_359 = value1_356 - C;
          int lookup_360 = ((value1_356 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((subtemp_359 & 0x88) >> 1);
          value1_356 = subtemp_359 & 0xff;
          _F203 = ((subtemp_359 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_360 & 0x07)] | OVERFLOW_SUB[(lookup_360 >> 4)] | (SZ53[value1_356] | (value1_356 == 0 ? 0x40 : 0));
          F = (_F203 & 0xFF);
          A = value1_356;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _F205;
          int value1_362 = A;
          int subtemp_365 = value1_362 - D;
          int lookup_366 = ((value1_362 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((subtemp_365 & 0x88) >> 1);
          value1_362 = subtemp_365 & 0xff;
          _F205 = ((subtemp_365 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_366 & 0x07)] | OVERFLOW_SUB[(lookup_366 >> 4)] | (SZ53[value1_362] | (value1_362 == 0 ? 0x40 : 0));
          F = (_F205 & 0xFF);
          A = value1_362;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _F207;
          int value1_368 = A;
          int subtemp_371 = value1_368 - E;
          int lookup_372 = ((value1_368 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((subtemp_371 & 0x88) >> 1);
          value1_368 = subtemp_371 & 0xff;
          _F207 = ((subtemp_371 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_372 & 0x07)] | OVERFLOW_SUB[(lookup_372 >> 4)] | (SZ53[value1_368] | (value1_368 == 0 ? 0x40 : 0));
          F = (_F207 & 0xFF);
          A = value1_368;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _F209;
          int value1_374 = A;
          int subtemp_377 = value1_374 - H;
          int lookup_378 = ((value1_374 & 0x88) >> 3) | ((H & 0x88) >> 2) | ((subtemp_377 & 0x88) >> 1);
          value1_374 = subtemp_377 & 0xff;
          _F209 = ((subtemp_377 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_378 & 0x07)] | OVERFLOW_SUB[(lookup_378 >> 4)] | (SZ53[value1_374] | (value1_374 == 0 ? 0x40 : 0));
          F = (_F209 & 0xFF);
          A = value1_374;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _F211;
          int value1_380 = A;
          int subtemp_383 = value1_380 - L;
          int lookup_384 = ((value1_380 & 0x88) >> 3) | ((L & 0x88) >> 2) | ((subtemp_383 & 0x88) >> 1);
          value1_380 = subtemp_383 & 0xff;
          _F211 = ((subtemp_383 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_384 & 0x07)] | OVERFLOW_SUB[(lookup_384 >> 4)] | (SZ53[value1_380] | (value1_380 == 0 ? 0x40 : 0));
          F = (_F211 & 0xFF);
          A = value1_380;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _F213;
          int _address84 = (H << 8) | L;
          int sourceValue_386 = read(_address84, 0);
          int value1_387 = A;
          int subtemp_390 = value1_387 - sourceValue_386;
          int lookup_391 = ((value1_387 & 0x88) >> 3) | ((sourceValue_386 & 0x88) >> 2) | ((subtemp_390 & 0x88) >> 1);
          value1_387 = subtemp_390 & 0xff;
          _F213 = ((subtemp_390 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_391 & 0x07)] | OVERFLOW_SUB[(lookup_391 >> 4)] | (SZ53[value1_387] | (value1_387 == 0 ? 0x40 : 0));
          F = (_F213 & 0xFF);
          A = value1_387;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _F215;
          int value1_393 = A;
          int value2_394 = A;
          int subtemp_396 = value1_393 - value2_394;
          int lookup_397 = ((value1_393 & 0x88) >> 3) | ((value2_394 & 0x88) >> 2) | ((subtemp_396 & 0x88) >> 1);
          value1_393 = subtemp_396 & 0xff;
          _F215 = ((subtemp_396 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_397 & 0x07)] | OVERFLOW_SUB[(lookup_397 >> 4)] | (SZ53[value1_393] | (value1_393 == 0 ? 0x40 : 0));
          F = (_F215 & 0xFF);
          A = value1_393;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_19(int opcode) {
    switch (opcode) {
      case 0x98: {
          int _F217;
          int value1_399 = A;
          int value3_401 = F & 1;
          _F217 = value3_401;
          int sbctemp_402 = value1_399 - B - (_F217 & 1);
          int lookup_403 = ((value1_399 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((sbctemp_402 & 0x88) >> 1);
          value1_399 = sbctemp_402 & 0xff;
          _F217 = ((sbctemp_402 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_403 & 0x07)] | OVERFLOW_SUB[(lookup_403 >> 4)] | (SZ53[value1_399] | (value1_399 == 0 ? 0x40 : 0));
          F = (_F217 & 0xFF);
          A = value1_399;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _F219;
          int value1_405 = A;
          int value3_407 = F & 1;
          _F219 = value3_407;
          int sbctemp_408 = value1_405 - C - (_F219 & 1);
          int lookup_409 = ((value1_405 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((sbctemp_408 & 0x88) >> 1);
          value1_405 = sbctemp_408 & 0xff;
          _F219 = ((sbctemp_408 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_409 & 0x07)] | OVERFLOW_SUB[(lookup_409 >> 4)] | (SZ53[value1_405] | (value1_405 == 0 ? 0x40 : 0));
          F = (_F219 & 0xFF);
          A = value1_405;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _F221;
          int value1_411 = A;
          int value3_413 = F & 1;
          _F221 = value3_413;
          int sbctemp_414 = value1_411 - D - (_F221 & 1);
          int lookup_415 = ((value1_411 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((sbctemp_414 & 0x88) >> 1);
          value1_411 = sbctemp_414 & 0xff;
          _F221 = ((sbctemp_414 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_415 & 0x07)] | OVERFLOW_SUB[(lookup_415 >> 4)] | (SZ53[value1_411] | (value1_411 == 0 ? 0x40 : 0));
          F = (_F221 & 0xFF);
          A = value1_411;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _F223;
          int value1_417 = A;
          int value3_419 = F & 1;
          _F223 = value3_419;
          int sbctemp_420 = value1_417 - E - (_F223 & 1);
          int lookup_421 = ((value1_417 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((sbctemp_420 & 0x88) >> 1);
          value1_417 = sbctemp_420 & 0xff;
          _F223 = ((sbctemp_420 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_421 & 0x07)] | OVERFLOW_SUB[(lookup_421 >> 4)] | (SZ53[value1_417] | (value1_417 == 0 ? 0x40 : 0));
          F = (_F223 & 0xFF);
          A = value1_417;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _F225;
          int value1_423 = A;
          int value3_425 = F & 1;
          _F225 = value3_425;
          int sbctemp_426 = value1_423 - H - (_F225 & 1);
          int lookup_427 = ((value1_423 & 0x88) >> 3) | ((H & 0x88) >> 2) | ((sbctemp_426 & 0x88) >> 1);
          value1_423 = sbctemp_426 & 0xff;
          _F225 = ((sbctemp_426 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_427 & 0x07)] | OVERFLOW_SUB[(lookup_427 >> 4)] | (SZ53[value1_423] | (value1_423 == 0 ? 0x40 : 0));
          F = (_F225 & 0xFF);
          A = value1_423;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _F227;
          int value1_429 = A;
          int value3_431 = F & 1;
          _F227 = value3_431;
          int sbctemp_432 = value1_429 - L - (_F227 & 1);
          int lookup_433 = ((value1_429 & 0x88) >> 3) | ((L & 0x88) >> 2) | ((sbctemp_432 & 0x88) >> 1);
          value1_429 = sbctemp_432 & 0xff;
          _F227 = ((sbctemp_432 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_433 & 0x07)] | OVERFLOW_SUB[(lookup_433 >> 4)] | (SZ53[value1_429] | (value1_429 == 0 ? 0x40 : 0));
          F = (_F227 & 0xFF);
          A = value1_429;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _F229;
          int _address84 = (H << 8) | L;
          int sourceValue_435 = read(_address84, 0);
          int value1_436 = A;
          int value3_438 = F & 1;
          _F229 = value3_438;
          int sbctemp_439 = value1_436 - sourceValue_435 - (_F229 & 1);
          int lookup_440 = ((value1_436 & 0x88) >> 3) | ((sourceValue_435 & 0x88) >> 2) | ((sbctemp_439 & 0x88) >> 1);
          value1_436 = sbctemp_439 & 0xff;
          _F229 = ((sbctemp_439 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_440 & 0x07)] | OVERFLOW_SUB[(lookup_440 >> 4)] | (SZ53[value1_436] | (value1_436 == 0 ? 0x40 : 0));
          F = (_F229 & 0xFF);
          A = value1_436;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _F231;
          int value1_442 = A;
          int value2_443 = A;
          int value3_444 = F & 1;
          _F231 = value3_444;
          int sbctemp_445 = value1_442 - value2_443 - (_F231 & 1);
          int lookup_446 = ((value1_442 & 0x88) >> 3) | ((value2_443 & 0x88) >> 2) | ((sbctemp_445 & 0x88) >> 1);
          value1_442 = sbctemp_445 & 0xff;
          _F231 = ((sbctemp_445 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_446 & 0x07)] | OVERFLOW_SUB[(lookup_446 >> 4)] | (SZ53[value1_442] | (value1_442 == 0 ? 0x40 : 0));
          F = (_F231 & 0xFF);
          A = value1_442;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_20(int opcode) {
    switch (opcode) {
      case 0xA0: {
          int _F233;
          int value1_448 = A;
          int value2_449 = B;
          value2_449 &= value1_448;
          _F233 = 0x10 | (SZ53P[value2_449 & 0xff] | (value2_449 == 0 ? 0x40 : 0));
          int result_451 = value2_449 & 0xFF;
          F = (_F233 & 0xFF);
          A = result_451;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F235;
          int value1_452 = A;
          int value2_453 = C;
          value2_453 &= value1_452;
          _F235 = 0x10 | (SZ53P[value2_453 & 0xff] | (value2_453 == 0 ? 0x40 : 0));
          int result_455 = value2_453 & 0xFF;
          F = (_F235 & 0xFF);
          A = result_455;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F237;
          int value1_456 = A;
          int value2_457 = D;
          value2_457 &= value1_456;
          _F237 = 0x10 | (SZ53P[value2_457 & 0xff] | (value2_457 == 0 ? 0x40 : 0));
          int result_459 = value2_457 & 0xFF;
          F = (_F237 & 0xFF);
          A = result_459;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F239;
          int value1_460 = A;
          int value2_461 = E;
          value2_461 &= value1_460;
          _F239 = 0x10 | (SZ53P[value2_461 & 0xff] | (value2_461 == 0 ? 0x40 : 0));
          int result_463 = value2_461 & 0xFF;
          F = (_F239 & 0xFF);
          A = result_463;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _F241;
          int value1_464 = A;
          int value2_465 = H;
          value2_465 &= value1_464;
          _F241 = 0x10 | (SZ53P[value2_465 & 0xff] | (value2_465 == 0 ? 0x40 : 0));
          int result_467 = value2_465 & 0xFF;
          F = (_F241 & 0xFF);
          A = result_467;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _F243;
          int value1_468 = A;
          int value2_469 = L;
          value2_469 &= value1_468;
          _F243 = 0x10 | (SZ53P[value2_469 & 0xff] | (value2_469 == 0 ? 0x40 : 0));
          int result_471 = value2_469 & 0xFF;
          F = (_F243 & 0xFF);
          A = result_471;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _F245;
          int _address84 = (H << 8) | L;
          int sourceValue_472 = read(_address84, 0);
          int value1_473 = A;
          int value2_474 = sourceValue_472;
          value2_474 &= value1_473;
          _F245 = 0x10 | (SZ53P[value2_474 & 0xff] | (value2_474 == 0 ? 0x40 : 0));
          int result_476 = value2_474 & 0xFF;
          F = (_F245 & 0xFF);
          A = result_476;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _F247;
          int value1_477 = A;
          int value2_478 = A;
          value2_478 &= value1_477;
          _F247 = 0x10 | (SZ53P[value2_478 & 0xff] | (value2_478 == 0 ? 0x40 : 0));
          int result_480 = value2_478 & 0xFF;
          F = (_F247 & 0xFF);
          A = result_480;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_21(int opcode) {
    switch (opcode) {
      case 0xA8: {
          int _F249;
          int value1_481 = A;
          int value2_482 = B;
          value2_482 ^= value1_481;
          _F249 = SZ53P[value2_482 & 0xff] | (value2_482 == 0 ? 0x40 : 0);
          int result_484 = value2_482 & 0xFF;
          F = (_F249 & 0xFF);
          A = result_484;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F251;
          int value1_485 = A;
          int value2_486 = C;
          value2_486 ^= value1_485;
          _F251 = SZ53P[value2_486 & 0xff] | (value2_486 == 0 ? 0x40 : 0);
          int result_488 = value2_486 & 0xFF;
          F = (_F251 & 0xFF);
          A = result_488;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F253;
          int value1_489 = A;
          int value2_490 = D;
          value2_490 ^= value1_489;
          _F253 = SZ53P[value2_490 & 0xff] | (value2_490 == 0 ? 0x40 : 0);
          int result_492 = value2_490 & 0xFF;
          F = (_F253 & 0xFF);
          A = result_492;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F255;
          int value1_493 = A;
          int value2_494 = E;
          value2_494 ^= value1_493;
          _F255 = SZ53P[value2_494 & 0xff] | (value2_494 == 0 ? 0x40 : 0);
          int result_496 = value2_494 & 0xFF;
          F = (_F255 & 0xFF);
          A = result_496;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _F257;
          int value1_497 = A;
          int value2_498 = H;
          value2_498 ^= value1_497;
          _F257 = SZ53P[value2_498 & 0xff] | (value2_498 == 0 ? 0x40 : 0);
          int result_500 = value2_498 & 0xFF;
          F = (_F257 & 0xFF);
          A = result_500;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _F259;
          int value1_501 = A;
          int value2_502 = L;
          value2_502 ^= value1_501;
          _F259 = SZ53P[value2_502 & 0xff] | (value2_502 == 0 ? 0x40 : 0);
          int result_504 = value2_502 & 0xFF;
          F = (_F259 & 0xFF);
          A = result_504;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _F261;
          int _address84 = (H << 8) | L;
          int sourceValue_505 = read(_address84, 0);
          int value1_506 = A;
          int value2_507 = sourceValue_505;
          value2_507 ^= value1_506;
          _F261 = SZ53P[value2_507 & 0xff] | (value2_507 == 0 ? 0x40 : 0);
          int result_509 = value2_507 & 0xFF;
          F = (_F261 & 0xFF);
          A = result_509;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _F263;
          int value1_510 = A;
          int value2_511 = A;
          value2_511 ^= value1_510;
          _F263 = SZ53P[value2_511 & 0xff] | (value2_511 == 0 ? 0x40 : 0);
          int result_513 = value2_511 & 0xFF;
          F = (_F263 & 0xFF);
          A = result_513;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_22(int opcode) {
    switch (opcode) {
      case 0xB0: {
          int _F265;
          int value1_514 = A;
          int value2_515 = B;
          value2_515 |= value1_514;
          _F265 = SZ53P[value2_515 & 0xff] | (value2_515 == 0 ? 0x40 : 0);
          int result_517 = value2_515 & 0xFF;
          F = (_F265 & 0xFF);
          A = result_517;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _F267;
          int value1_518 = A;
          int value2_519 = C;
          value2_519 |= value1_518;
          _F267 = SZ53P[value2_519 & 0xff] | (value2_519 == 0 ? 0x40 : 0);
          int result_521 = value2_519 & 0xFF;
          F = (_F267 & 0xFF);
          A = result_521;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _F269;
          int value1_522 = A;
          int value2_523 = D;
          value2_523 |= value1_522;
          _F269 = SZ53P[value2_523 & 0xff] | (value2_523 == 0 ? 0x40 : 0);
          int result_525 = value2_523 & 0xFF;
          F = (_F269 & 0xFF);
          A = result_525;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _F271;
          int value1_526 = A;
          int value2_527 = E;
          value2_527 |= value1_526;
          _F271 = SZ53P[value2_527 & 0xff] | (value2_527 == 0 ? 0x40 : 0);
          int result_529 = value2_527 & 0xFF;
          F = (_F271 & 0xFF);
          A = result_529;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _F273;
          int value1_530 = A;
          int value2_531 = H;
          value2_531 |= value1_530;
          _F273 = SZ53P[value2_531 & 0xff] | (value2_531 == 0 ? 0x40 : 0);
          int result_533 = value2_531 & 0xFF;
          F = (_F273 & 0xFF);
          A = result_533;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _F275;
          int value1_534 = A;
          int value2_535 = L;
          value2_535 |= value1_534;
          _F275 = SZ53P[value2_535 & 0xff] | (value2_535 == 0 ? 0x40 : 0);
          int result_537 = value2_535 & 0xFF;
          F = (_F275 & 0xFF);
          A = result_537;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _F277;
          int _address84 = (H << 8) | L;
          int sourceValue_538 = read(_address84, 0);
          int value1_539 = A;
          int value2_540 = sourceValue_538;
          value2_540 |= value1_539;
          _F277 = SZ53P[value2_540 & 0xff] | (value2_540 == 0 ? 0x40 : 0);
          int result_542 = value2_540 & 0xFF;
          F = (_F277 & 0xFF);
          A = result_542;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _F279;
          int value1_543 = A;
          int value2_544 = A;
          value2_544 |= value1_543;
          _F279 = SZ53P[value2_544 & 0xff] | (value2_544 == 0 ? 0x40 : 0);
          int result_546 = value2_544 & 0xFF;
          F = (_F279 & 0xFF);
          A = result_546;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_23(int opcode) {
    switch (opcode) {
      case 0xB8: {
          int _F281;
          int cptemp_550 = A - B;
          int lookup_551 = ((A & 0x88) >> 3) | ((B & 0x88) >> 2) | ((cptemp_550 & 0x88) >> 1);
          _F281 = ((cptemp_550 & 0x100) != 0 ? 1 : (cptemp_550 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_551 & 0x07)] | OVERFLOW_SUB[(lookup_551 >> 4)] | (B & 0x28) | (cptemp_550 & 0x80);
          F = (_F281 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _F283;
          int cptemp_556 = A - C;
          int lookup_557 = ((A & 0x88) >> 3) | ((C & 0x88) >> 2) | ((cptemp_556 & 0x88) >> 1);
          _F283 = ((cptemp_556 & 0x100) != 0 ? 1 : (cptemp_556 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_557 & 0x07)] | OVERFLOW_SUB[(lookup_557 >> 4)] | (C & 0x28) | (cptemp_556 & 0x80);
          F = (_F283 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _F285;
          int cptemp_562 = A - D;
          int lookup_563 = ((A & 0x88) >> 3) | ((D & 0x88) >> 2) | ((cptemp_562 & 0x88) >> 1);
          _F285 = ((cptemp_562 & 0x100) != 0 ? 1 : (cptemp_562 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_563 & 0x07)] | OVERFLOW_SUB[(lookup_563 >> 4)] | (D & 0x28) | (cptemp_562 & 0x80);
          F = (_F285 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _F287;
          int cptemp_568 = A - E;
          int lookup_569 = ((A & 0x88) >> 3) | ((E & 0x88) >> 2) | ((cptemp_568 & 0x88) >> 1);
          _F287 = ((cptemp_568 & 0x100) != 0 ? 1 : (cptemp_568 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_569 & 0x07)] | OVERFLOW_SUB[(lookup_569 >> 4)] | (E & 0x28) | (cptemp_568 & 0x80);
          F = (_F287 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _F289;
          int cptemp_574 = A - H;
          int lookup_575 = ((A & 0x88) >> 3) | ((H & 0x88) >> 2) | ((cptemp_574 & 0x88) >> 1);
          _F289 = ((cptemp_574 & 0x100) != 0 ? 1 : (cptemp_574 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_575 & 0x07)] | OVERFLOW_SUB[(lookup_575 >> 4)] | (H & 0x28) | (cptemp_574 & 0x80);
          F = (_F289 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _F291;
          int cptemp_580 = A - L;
          int lookup_581 = ((A & 0x88) >> 3) | ((L & 0x88) >> 2) | ((cptemp_580 & 0x88) >> 1);
          _F291 = ((cptemp_580 & 0x100) != 0 ? 1 : (cptemp_580 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_581 & 0x07)] | OVERFLOW_SUB[(lookup_581 >> 4)] | (L & 0x28) | (cptemp_580 & 0x80);
          F = (_F291 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _F293;
          int _address84 = (H << 8) | L;
          int sourceValue_583 = read(_address84, 0);
          int cptemp_587 = A - sourceValue_583;
          int lookup_588 = ((A & 0x88) >> 3) | ((sourceValue_583 & 0x88) >> 2) | ((cptemp_587 & 0x88) >> 1);
          _F293 = ((cptemp_587 & 0x100) != 0 ? 1 : (cptemp_587 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_588 & 0x07)] | OVERFLOW_SUB[(lookup_588 >> 4)] | (sourceValue_583 & 0x28) | (cptemp_587 & 0x80);
          F = (_F293 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _F295;
          int cptemp_593 = A - A;
          int lookup_594 = ((A & 0x88) >> 3) | ((A & 0x88) >> 2) | ((cptemp_593 & 0x88) >> 1);
          _F295 = ((cptemp_593 & 0x100) != 0 ? 1 : (cptemp_593 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_594 & 0x07)] | OVERFLOW_SUB[(lookup_594 >> 4)] | (A & 0x28) | (cptemp_593 & 0x80);
          F = (_F295 & 0xFF);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_24(int opcode) {
    switch (opcode) {
      case 0xC0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_596 = SP;
          if ((!((F & 0x40) == 0x40))) {
              int wordNumber1_598 = read(SP, 0);
              int wordNumber_599 = read((SP + 1) & 0xFFFF, 0);
              int value_597 = ((wordNumber_599 << 8) | wordNumber1_598);
              int wordNumber_600 = SP;
              SP = ((wordNumber_600 + 2) & 0xFFFF);
              jumpAddress2_596 = value_597;
              MEMPTR = jumpAddress2_596;
              PC = jumpAddress2_596;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xC1: {
          int wordNumber1_604 = read(SP, 0);
          int wordNumber_605 = read((SP + 1) & 0xFFFF, 0);
          int value_603 = ((wordNumber_605 << 8) | wordNumber1_604);
          int wordNumber_606 = SP;
          SP = ((wordNumber_606 + 2) & 0xFFFF);
          B = (value_603 >>> 8);
          C = value_603 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _jumpAddress299 = 0;
          int address_608 = (PC + 1) & 0xFFFF;
          int operand_610 = read(address_608, 0);
          int operand_612 = read((address_608 + 1) & 0xFFFF, 0);
          int jumpAddress2_607 = (_jumpAddress299 = (operand_612 << 8) | operand_610);
          if ((!((F & 0x40) == 0x40))) {
              _jumpAddress299 = jumpAddress2_607;
              MEMPTR = _jumpAddress299;
              PC = jumpAddress2_607;
              break;
          } else {
              MEMPTR = _jumpAddress299;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xC3: {
          int _nextPC300;
          int _jumpAddress300;
          int address_615 = (PC + 1) & 0xFFFF;
          int operand_617 = read(address_615, 0);
          int operand_619 = read((address_615 + 1) & 0xFFFF, 0);
          int jumpAddress2_614 = (_jumpAddress300 = (operand_619 << 8) | operand_617);
          _jumpAddress300 = jumpAddress2_614;
          _nextPC300 = jumpAddress2_614;
          MEMPTR = _jumpAddress300;
          PC = _nextPC300;
          break;
      }
      case 0xC4: {
          int _jumpAddress301 = 0;
          int address_621 = (PC + 1) & 0xFFFF;
          int operand_623 = read(address_621, 0);
          int operand_625 = read((address_621 + 1) & 0xFFFF, 0);
          int value_626 = (_jumpAddress301 = (operand_625 << 8) | operand_623);
          MEMPTR = value_626;
          int jumpAddress2_627 = (_jumpAddress301 = (operand_625 << 8) | operand_623);
          if ((!((F & 0x40) == 0x40))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_631 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_631 >>> 8));
              write(SP, (value_631 & 0xFF));
              _jumpAddress301 = jumpAddress2_627;
              MEMPTR = _jumpAddress301;
              PC = jumpAddress2_627;
              break;
          } else {
              MEMPTR = _jumpAddress301;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xC5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_633 = ((B << 8) | C);
          write((SP + 1) & 0xFFFF, (value_633 >>> 8));
          write(SP, (value_633 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _F303;
          int operand_634 = read((PC + 1) & 0xFFFF, 0);
          int value1_635 = A;
          int value2_636 = operand_634;
          int addtemp_638 = value2_636 + value1_635;
          int lookup_639 = ((value2_636 & 0x88) >> 3) | ((value1_635 & 0x88) >> 2) | ((addtemp_638 & 0x88) >> 1);
          value2_636 = addtemp_638 & 0xff;
          _F303 = ((addtemp_638 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_639 & 0x07)] | OVERFLOW_ADD[(lookup_639 >> 4)] | (SZ53[value2_636] | (value2_636 == 0 ? 0x40 : 0));
          F = (_F303 & 0xFF);
          A = value2_636;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _nextPC305;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_641 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_641 >>> 8));
          write(SP, (value_641 & 0xFF));
          _nextPC305 = 0;
          MEMPTR = _nextPC305 & 0xFFFF;
          PC = _nextPC305 == -1 ? (PC + 1) & 0xFFFF : _nextPC305;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_25(int opcode) {
    switch (opcode) {
      case 0xC8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_642 = SP;
          if (((F & 0x40) == 0x40)) {
              int wordNumber1_644 = read(SP, 0);
              int wordNumber_645 = read((SP + 1) & 0xFFFF, 0);
              int value_643 = ((wordNumber_645 << 8) | wordNumber1_644);
              int wordNumber_646 = SP;
              SP = ((wordNumber_646 + 2) & 0xFFFF);
              jumpAddress2_642 = value_643;
              MEMPTR = jumpAddress2_642;
              PC = jumpAddress2_642;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xC9: {
          int _nextPC307;
          int jumpAddress2_648;
          int wordNumber1_650 = read(SP, 0);
          int wordNumber_651 = read((SP + 1) & 0xFFFF, 0);
          int value_649 = ((wordNumber_651 << 8) | wordNumber1_650);
          int wordNumber_652 = SP;
          SP = ((wordNumber_652 + 2) & 0xFFFF);
          jumpAddress2_648 = value_649;
          _nextPC307 = jumpAddress2_648;
          MEMPTR = _nextPC307;
          PC = _nextPC307;
          break;
      }
      case 0xCA: {
          int _jumpAddress308 = 0;
          int address_655 = (PC + 1) & 0xFFFF;
          int operand_657 = read(address_655, 0);
          int operand_659 = read((address_655 + 1) & 0xFFFF, 0);
          int jumpAddress2_654 = (_jumpAddress308 = (operand_659 << 8) | operand_657);
          if (((F & 0x40) == 0x40)) {
              _jumpAddress308 = jumpAddress2_654;
              MEMPTR = _jumpAddress308;
              PC = jumpAddress2_654;
              break;
          } else {
              MEMPTR = _jumpAddress308;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xCB: {
          R = (R + 1 & 0x7f) | regRBit7;
          decodeCB(read((PC + 1) & 0xFFFF, 1));
          break;
      }
      case 0xCC: {
          int _jumpAddress693 = 0;
          int address_1333 = (PC + 1) & 0xFFFF;
          int operand_1335 = read(address_1333, 0);
          int operand_1337 = read((address_1333 + 1) & 0xFFFF, 0);
          int value_1338 = (_jumpAddress693 = (operand_1337 << 8) | operand_1335);
          MEMPTR = value_1338;
          int jumpAddress2_1339 = (_jumpAddress693 = (operand_1337 << 8) | operand_1335);
          if (((F & 0x40) == 0x40)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_1343 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_1343 >>> 8));
              write(SP, (value_1343 & 0xFF));
              _jumpAddress693 = jumpAddress2_1339;
              MEMPTR = _jumpAddress693;
              PC = jumpAddress2_1339;
              break;
          } else {
              MEMPTR = _jumpAddress693;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xCD: {
          int _nextPC694;
          int _jumpAddress694;
          int address_1345 = (PC + 1) & 0xFFFF;
          int operand_1347 = read(address_1345, 0);
          int operand_1349 = read((address_1345 + 1) & 0xFFFF, 0);
          int value_1350 = (_jumpAddress694 = (operand_1349 << 8) | operand_1347);
          int jumpAddress2_1351 = (_jumpAddress694 = (operand_1349 << 8) | operand_1347);
          SP = ((SP - 2) & 0xFFFF);
          int value_1355 = ((PC + 3) & 0xFFFF);
          contend1x1((PC + 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_1355 >>> 8));
          write(SP, (value_1355 & 0xFF));
          _jumpAddress694 = jumpAddress2_1351;
          _nextPC694 = jumpAddress2_1351;
          MEMPTR = _jumpAddress694;
          PC = _nextPC694;
          break;
      }
      case 0xCE: {
          int _F695;
          int operand_1357 = read((PC + 1) & 0xFFFF, 0);
          int value1_1358 = A;
          int value3_1360 = F & 1;
          _F695 = value3_1360;
          int adctemp_1361 = value1_1358 + operand_1357 + (_F695 & 1);
          int lookup_1362 = ((value1_1358 & 0x88) >> 3) | ((operand_1357 & 0x88) >> 2) | ((adctemp_1361 & 0x88) >> 1);
          value1_1358 = adctemp_1361 & 0xff;
          _F695 = ((adctemp_1361 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1362 & 0x07)] | OVERFLOW_ADD[(lookup_1362 >> 4)] | (SZ53[value1_1358] | (value1_1358 == 0 ? 0x40 : 0));
          F = (_F695 & 0xFF);
          A = value1_1358;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _nextPC697;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_1364 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_1364 >>> 8));
          write(SP, (value_1364 & 0xFF));
          _nextPC697 = 8;
          MEMPTR = _nextPC697;
          PC = _nextPC697;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_26(int opcode) {
    switch (opcode) {
      case 0xD0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_1365 = SP;
          if ((!((F & 1) == 1))) {
              int wordNumber1_1367 = read(SP, 0);
              int wordNumber_1368 = read((SP + 1) & 0xFFFF, 0);
              int value_1366 = ((wordNumber_1368 << 8) | wordNumber1_1367);
              int wordNumber_1369 = SP;
              SP = ((wordNumber_1369 + 2) & 0xFFFF);
              jumpAddress2_1365 = value_1366;
              MEMPTR = jumpAddress2_1365;
              PC = jumpAddress2_1365;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xD1: {
          int wordNumber1_1373 = read(SP, 0);
          int wordNumber_1374 = read((SP + 1) & 0xFFFF, 0);
          int value_1372 = ((wordNumber_1374 << 8) | wordNumber1_1373);
          int wordNumber_1375 = SP;
          SP = ((wordNumber_1375 + 2) & 0xFFFF);
          D = (value_1372 >>> 8);
          E = value_1372 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _jumpAddress700 = 0;
          int address_1377 = (PC + 1) & 0xFFFF;
          int operand_1379 = read(address_1377, 0);
          int operand_1381 = read((address_1377 + 1) & 0xFFFF, 0);
          int jumpAddress2_1376 = (_jumpAddress700 = (operand_1381 << 8) | operand_1379);
          if ((!((F & 1) == 1))) {
              _jumpAddress700 = jumpAddress2_1376;
              MEMPTR = _jumpAddress700;
              PC = jumpAddress2_1376;
              break;
          } else {
              MEMPTR = _jumpAddress700;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xD3: {
          int operand_1384 = read((PC + 1) & 0xFFFF, 0);
          int read_1383 = operand_1384;
          read_1383 = (read_1383 | A << 8);
          io.out(read_1383, A);
          MEMPTR = (A << 8);
          int read_1385 = operand_1384;
          read_1385 = (read_1385 | A << 8);
          MEMPTR = (MEMPTR | ((read_1385 + 1) & 0xff));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _jumpAddress702 = 0;
          int address_1386 = (PC + 1) & 0xFFFF;
          int operand_1388 = read(address_1386, 0);
          int operand_1390 = read((address_1386 + 1) & 0xFFFF, 0);
          int value_1391 = (_jumpAddress702 = (operand_1390 << 8) | operand_1388);
          MEMPTR = value_1391;
          int jumpAddress2_1392 = (_jumpAddress702 = (operand_1390 << 8) | operand_1388);
          if ((!((F & 1) == 1))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_1396 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_1396 >>> 8));
              write(SP, (value_1396 & 0xFF));
              _jumpAddress702 = jumpAddress2_1392;
              MEMPTR = _jumpAddress702;
              PC = jumpAddress2_1392;
              break;
          } else {
              MEMPTR = _jumpAddress702;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xD5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_1398 = ((D << 8) | E);
          write((SP + 1) & 0xFFFF, (value_1398 >>> 8));
          write(SP, (value_1398 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _F704;
          int operand_1399 = read((PC + 1) & 0xFFFF, 0);
          int value1_1400 = A;
          int subtemp_1403 = value1_1400 - operand_1399;
          int lookup_1404 = ((value1_1400 & 0x88) >> 3) | ((operand_1399 & 0x88) >> 2) | ((subtemp_1403 & 0x88) >> 1);
          value1_1400 = subtemp_1403 & 0xff;
          _F704 = ((subtemp_1403 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1404 & 0x07)] | OVERFLOW_SUB[(lookup_1404 >> 4)] | (SZ53[value1_1400] | (value1_1400 == 0 ? 0x40 : 0));
          F = (_F704 & 0xFF);
          A = value1_1400;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _nextPC706;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_1406 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_1406 >>> 8));
          write(SP, (value_1406 & 0xFF));
          _nextPC706 = 0x10;
          MEMPTR = _nextPC706;
          PC = _nextPC706;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_27(int opcode) {
    switch (opcode) {
      case 0xD8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_1407 = SP;
          if (((F & 1) == 1)) {
              int wordNumber1_1409 = read(SP, 0);
              int wordNumber_1410 = read((SP + 1) & 0xFFFF, 0);
              int value_1408 = ((wordNumber_1410 << 8) | wordNumber1_1409);
              int wordNumber_1411 = SP;
              SP = ((wordNumber_1411 + 2) & 0xFFFF);
              jumpAddress2_1407 = value_1408;
              MEMPTR = jumpAddress2_1407;
              PC = jumpAddress2_1407;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xD9: {
          int v1_1413 = ((B << 8) | C);
          B = (_BC >>> 8);
          C = _BC & 0xFF;
          _BC = v1_1413;
          v1_1413 = (D << 8) | E;
          D = (_DE >>> 8);
          E = _DE & 0xFF;
          _DE = v1_1413;
          v1_1413 = (H << 8) | L;
          H = (_HL >>> 8);
          L = _HL & 0xFF;
          _HL = v1_1413;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _jumpAddress709 = 0;
          int address_1415 = (PC + 1) & 0xFFFF;
          int operand_1417 = read(address_1415, 0);
          int operand_1419 = read((address_1415 + 1) & 0xFFFF, 0);
          int jumpAddress2_1414 = (_jumpAddress709 = (operand_1419 << 8) | operand_1417);
          if (((F & 1) == 1)) {
              _jumpAddress709 = jumpAddress2_1414;
              MEMPTR = _jumpAddress709;
              PC = jumpAddress2_1414;
              break;
          } else {
              MEMPTR = _jumpAddress709;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xDB: {
          int operand_1422 = read((PC + 1) & 0xFFFF, 0);
          MEMPTR = (((operand_1422 | A << 8) + 1) & 0xFFFF);
          int port_1423 = (operand_1422 | A << 8);
          int value_1425 = io.in(port_1423);
          A = value_1425 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress711 = 0;
          int address_1426 = (PC + 1) & 0xFFFF;
          int operand_1428 = read(address_1426, 0);
          int operand_1430 = read((address_1426 + 1) & 0xFFFF, 0);
          int value_1431 = (_jumpAddress711 = (operand_1430 << 8) | operand_1428);
          MEMPTR = value_1431;
          int jumpAddress2_1432 = (_jumpAddress711 = (operand_1430 << 8) | operand_1428);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_1436 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_1436 >>> 8));
              write(SP, (value_1436 & 0xFF));
              _jumpAddress711 = jumpAddress2_1432;
              MEMPTR = _jumpAddress711;
              PC = jumpAddress2_1432;
              break;
          } else {
              MEMPTR = _jumpAddress711;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xDD: {
          R = (R + 1 & 0x7f) | regRBit7;
          decodeDD(read((PC + 1) & 0xFFFF, 1));
          break;
      }
      case 0xDE: {
          int _F1721;
          int operand_3577 = read((PC + 1) & 0xFFFF, 0);
          int value1_3578 = A;
          int value3_3580 = F & 1;
          _F1721 = value3_3580;
          int sbctemp_3581 = value1_3578 - operand_3577 - (_F1721 & 1);
          int lookup_3582 = ((value1_3578 & 0x88) >> 3) | ((operand_3577 & 0x88) >> 2) | ((sbctemp_3581 & 0x88) >> 1);
          value1_3578 = sbctemp_3581 & 0xff;
          _F1721 = ((sbctemp_3581 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3582 & 0x07)] | OVERFLOW_SUB[(lookup_3582 >> 4)] | (SZ53[value1_3578] | (value1_3578 == 0 ? 0x40 : 0));
          F = (_F1721 & 0xFF);
          A = value1_3578;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC1723;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3584 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3584 >>> 8));
          write(SP, (value_3584 & 0xFF));
          _nextPC1723 = 0x18;
          MEMPTR = _nextPC1723;
          PC = _nextPC1723;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_28(int opcode) {
    switch (opcode) {
      case 0xE0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3585 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_3587 = read(SP, 0);
              int wordNumber_3588 = read((SP + 1) & 0xFFFF, 0);
              int value_3586 = ((wordNumber_3588 << 8) | wordNumber1_3587);
              int wordNumber_3589 = SP;
              SP = ((wordNumber_3589 + 2) & 0xFFFF);
              jumpAddress2_3585 = value_3586;
              MEMPTR = jumpAddress2_3585;
              PC = jumpAddress2_3585;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_3593 = read(SP, 0);
          int wordNumber_3594 = read((SP + 1) & 0xFFFF, 0);
          int value_3592 = ((wordNumber_3594 << 8) | wordNumber1_3593);
          int wordNumber_3595 = SP;
          SP = ((wordNumber_3595 + 2) & 0xFFFF);
          H = (value_3592 >>> 8);
          L = value_3592 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress1726 = 0;
          int address_3597 = (PC + 1) & 0xFFFF;
          int operand_3599 = read(address_3597, 0);
          int operand_3601 = read((address_3597 + 1) & 0xFFFF, 0);
          int jumpAddress2_3596 = (_jumpAddress1726 = (operand_3601 << 8) | operand_3599);
          if ((!((F & 4) == 4))) {
              _jumpAddress1726 = jumpAddress2_3596;
              MEMPTR = _jumpAddress1726;
              PC = jumpAddress2_3596;
              break;
          } else {
              MEMPTR = _jumpAddress1726;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address1727 = SP;
          int wordNumber1_3604 = read(_address1727, 0);
          int wordNumber_3605 = read((_address1727 + 1) & 0xFFFF, 0);
          int v1_3603 = ((wordNumber_3605 << 8) | wordNumber1_3604);
          int v2_3606 = ((H << 8) | L);
          _address1727 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address1727 + 1) & 0xFFFF, (v2_3606 >>> 8));
          write(_address1727, (v2_3606 & 0xFF));
          H = (v1_3603 >>> 8);
          L = v1_3603 & 0xFF;
          MEMPTR = ((H << 8) | L);
          contend2x1(SP);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress1729 = 0;
          int address_3607 = (PC + 1) & 0xFFFF;
          int operand_3609 = read(address_3607, 0);
          int operand_3611 = read((address_3607 + 1) & 0xFFFF, 0);
          int value_3612 = (_jumpAddress1729 = (operand_3611 << 8) | operand_3609);
          MEMPTR = value_3612;
          int jumpAddress2_3613 = (_jumpAddress1729 = (operand_3611 << 8) | operand_3609);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3617 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3617 >>> 8));
              write(SP, (value_3617 & 0xFF));
              _jumpAddress1729 = jumpAddress2_3613;
              MEMPTR = _jumpAddress1729;
              PC = jumpAddress2_3613;
              break;
          } else {
              MEMPTR = _jumpAddress1729;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xE5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3619 = ((H << 8) | L);
          write((SP + 1) & 0xFFFF, (value_3619 >>> 8));
          write(SP, (value_3619 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _F1731;
          int operand_3620 = read((PC + 1) & 0xFFFF, 0);
          int value1_3621 = A;
          int value2_3622 = operand_3620;
          value2_3622 &= value1_3621;
          _F1731 = 0x10 | (SZ53P[value2_3622 & 0xff] | (value2_3622 == 0 ? 0x40 : 0));
          int result_3624 = value2_3622 & 0xFF;
          F = (_F1731 & 0xFF);
          A = result_3624;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC1733;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3625 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3625 >>> 8));
          write(SP, (value_3625 & 0xFF));
          _nextPC1733 = 0x20;
          MEMPTR = _nextPC1733;
          PC = _nextPC1733;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_29(int opcode) {
    switch (opcode) {
      case 0xE8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3626 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_3628 = read(SP, 0);
              int wordNumber_3629 = read((SP + 1) & 0xFFFF, 0);
              int value_3627 = ((wordNumber_3629 << 8) | wordNumber1_3628);
              int wordNumber_3630 = SP;
              SP = ((wordNumber_3630 + 2) & 0xFFFF);
              jumpAddress2_3626 = value_3627;
              MEMPTR = jumpAddress2_3626;
              PC = jumpAddress2_3626;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xE9: {
          int _nextPC1735;
          int jumpAddress2_3632 = ((H << 8) | L);
          _nextPC1735 = jumpAddress2_3632;
          MEMPTR = 0;
          PC = _nextPC1735;
          break;
      }
      case 0xEA: {
          int _jumpAddress1736 = 0;
          int address_3635 = (PC + 1) & 0xFFFF;
          int operand_3637 = read(address_3635, 0);
          int operand_3639 = read((address_3635 + 1) & 0xFFFF, 0);
          int jumpAddress2_3634 = (_jumpAddress1736 = (operand_3639 << 8) | operand_3637);
          if (((F & 4) == 4)) {
              _jumpAddress1736 = jumpAddress2_3634;
              MEMPTR = _jumpAddress1736;
              PC = jumpAddress2_3634;
              break;
          } else {
              MEMPTR = _jumpAddress1736;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_3641 = ((D << 8) | E);
          int v2_3642 = ((H << 8) | L);
          D = (v2_3642 >>> 8);
          E = v2_3642 & 0xFF;
          H = (v1_3641 >>> 8);
          L = v1_3641 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress1738 = 0;
          int address_3643 = (PC + 1) & 0xFFFF;
          int operand_3645 = read(address_3643, 0);
          int operand_3647 = read((address_3643 + 1) & 0xFFFF, 0);
          int value_3648 = (_jumpAddress1738 = (operand_3647 << 8) | operand_3645);
          MEMPTR = value_3648;
          int jumpAddress2_3649 = (_jumpAddress1738 = (operand_3647 << 8) | operand_3645);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3653 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3653 >>> 8));
              write(SP, (value_3653 & 0xFF));
              _jumpAddress1738 = jumpAddress2_3649;
              MEMPTR = _jumpAddress1738;
              PC = jumpAddress2_3649;
              break;
          } else {
              MEMPTR = _jumpAddress1738;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xED: {
          R = (R + 1 & 0x7f) | regRBit7;
          decodeED(read((PC + 1) & 0xFFFF, 1));
          break;
      }
      case 0xEE: {
          int _F2047;
          int operand_4185 = read((PC + 1) & 0xFFFF, 0);
          int value1_4186 = A;
          int value2_4187 = operand_4185;
          value2_4187 ^= value1_4186;
          _F2047 = SZ53P[value2_4187 & 0xff] | (value2_4187 == 0 ? 0x40 : 0);
          int result_4189 = value2_4187 & 0xFF;
          F = (_F2047 & 0xFF);
          A = result_4189;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC2049;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4190 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4190 >>> 8));
          write(SP, (value_4190 & 0xFF));
          _nextPC2049 = 0x28;
          MEMPTR = _nextPC2049;
          PC = _nextPC2049;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_30(int opcode) {
    switch (opcode) {
      case 0xF0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_4191 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_4193 = read(SP, 0);
              int wordNumber_4194 = read((SP + 1) & 0xFFFF, 0);
              int value_4192 = ((wordNumber_4194 << 8) | wordNumber1_4193);
              int wordNumber_4195 = SP;
              SP = ((wordNumber_4195 + 2) & 0xFFFF);
              jumpAddress2_4191 = value_4192;
              MEMPTR = jumpAddress2_4191;
              PC = jumpAddress2_4191;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_4199 = read(SP, 0);
          int wordNumber_4200 = read((SP + 1) & 0xFFFF, 0);
          int value_4198 = ((wordNumber_4200 << 8) | wordNumber1_4199);
          int wordNumber_4201 = SP;
          SP = ((wordNumber_4201 + 2) & 0xFFFF);
          A = (value_4198 >>> 8);
          F = value_4198 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress2052 = 0;
          int address_4203 = (PC + 1) & 0xFFFF;
          int operand_4205 = read(address_4203, 0);
          int operand_4207 = read((address_4203 + 1) & 0xFFFF, 0);
          int jumpAddress2_4202 = (_jumpAddress2052 = (operand_4207 << 8) | operand_4205);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress2052 = jumpAddress2_4202;
              MEMPTR = _jumpAddress2052;
              PC = jumpAddress2_4202;
              break;
          } else {
              MEMPTR = _jumpAddress2052;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xF3: {
          state.resetInterrupt();
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _jumpAddress2054 = 0;
          int address_4209 = (PC + 1) & 0xFFFF;
          int operand_4211 = read(address_4209, 0);
          int operand_4213 = read((address_4209 + 1) & 0xFFFF, 0);
          int value_4214 = (_jumpAddress2054 = (operand_4213 << 8) | operand_4211);
          MEMPTR = value_4214;
          int jumpAddress2_4215 = (_jumpAddress2054 = (operand_4213 << 8) | operand_4211);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4219 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4219 >>> 8));
              write(SP, (value_4219 & 0xFF));
              _jumpAddress2054 = jumpAddress2_4215;
              MEMPTR = _jumpAddress2054;
              PC = jumpAddress2_4215;
              break;
          } else {
              MEMPTR = _jumpAddress2054;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xF5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4221 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_4221 >>> 8));
          write(SP, (value_4221 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F2056;
          int operand_4222 = read((PC + 1) & 0xFFFF, 0);
          int value1_4223 = A;
          int value2_4224 = operand_4222;
          value2_4224 |= value1_4223;
          _F2056 = SZ53P[value2_4224 & 0xff] | (value2_4224 == 0 ? 0x40 : 0);
          int result_4226 = value2_4224 & 0xFF;
          F = (_F2056 & 0xFF);
          A = result_4226;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC2058;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4227 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4227 >>> 8));
          write(SP, (value_4227 & 0xFF));
          _nextPC2058 = 0x30;
          MEMPTR = _nextPC2058;
          PC = _nextPC2058;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decode_31(int opcode) {
    switch (opcode) {
      case 0xF8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_4228 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_4230 = read(SP, 0);
              int wordNumber_4231 = read((SP + 1) & 0xFFFF, 0);
              int value_4229 = ((wordNumber_4231 << 8) | wordNumber1_4230);
              int wordNumber_4232 = SP;
              SP = ((wordNumber_4232 + 2) & 0xFFFF);
              jumpAddress2_4228 = value_4229;
              MEMPTR = jumpAddress2_4228;
              PC = jumpAddress2_4228;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xF9: {
          contend2x1(((I << 8) | R));
          SP = ((H << 8) | L);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _jumpAddress2061 = 0;
          int address_4235 = (PC + 1) & 0xFFFF;
          int operand_4237 = read(address_4235, 0);
          int operand_4239 = read((address_4235 + 1) & 0xFFFF, 0);
          int jumpAddress2_4234 = (_jumpAddress2061 = (operand_4239 << 8) | operand_4237);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress2061 = jumpAddress2_4234;
              MEMPTR = _jumpAddress2061;
              PC = jumpAddress2_4234;
              break;
          } else {
              MEMPTR = _jumpAddress2061;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xFB: {
          state.enableInterrupt();
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _jumpAddress2063 = 0;
          int address_4241 = (PC + 1) & 0xFFFF;
          int operand_4243 = read(address_4241, 0);
          int operand_4245 = read((address_4241 + 1) & 0xFFFF, 0);
          int value_4246 = (_jumpAddress2063 = (operand_4245 << 8) | operand_4243);
          MEMPTR = value_4246;
          int jumpAddress2_4247 = (_jumpAddress2063 = (operand_4245 << 8) | operand_4243);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4251 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4251 >>> 8));
              write(SP, (value_4251 & 0xFF));
              _jumpAddress2063 = jumpAddress2_4247;
              MEMPTR = _jumpAddress2063;
              PC = jumpAddress2_4247;
              break;
          } else {
              MEMPTR = _jumpAddress2063;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xFD: {
          R = (R + 1 & 0x7f) | regRBit7;
          decodeFD(read((PC + 1) & 0xFFFF, 1));
          break;
      }
      case 0xFE: {
          int _F3073;
          int operand_6392 = read((PC + 1) & 0xFFFF, 0);
          int cptemp_6396 = A - operand_6392;
          int lookup_6397 = ((A & 0x88) >> 3) | ((operand_6392 & 0x88) >> 2) | ((cptemp_6396 & 0x88) >> 1);
          _F3073 = ((cptemp_6396 & 0x100) != 0 ? 1 : (cptemp_6396 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_6397 & 0x07)] | OVERFLOW_SUB[(lookup_6397 >> 4)] | (operand_6392 & 0x28) | (cptemp_6396 & 0x80);
          F = (_F3073 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC3075;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6399 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6399 >>> 8));
          write(SP, (value_6399 & 0xFF));
          _nextPC3075 = 0x38;
          MEMPTR = _nextPC3075;
          PC = _nextPC3075;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decode");
    }
  }

  private void decodeCB(int opcode) {
    switch (opcode >> 3) {
      case 0: decodeCB_0(opcode);
        break;
      case 1: decodeCB_1(opcode);
        break;
      case 2: decodeCB_2(opcode);
        break;
      case 3: decodeCB_3(opcode);
        break;
      case 4: decodeCB_4(opcode);
        break;
      case 5: decodeCB_5(opcode);
        break;
      case 6: decodeCB_6(opcode);
        break;
      case 7: decodeCB_7(opcode);
        break;
      case 8: decodeCB_8(opcode);
        break;
      case 9: decodeCB_9(opcode);
        break;
      case 10: decodeCB_10(opcode);
        break;
      case 11: decodeCB_11(opcode);
        break;
      case 12: decodeCB_12(opcode);
        break;
      case 13: decodeCB_13(opcode);
        break;
      case 14: decodeCB_14(opcode);
        break;
      case 15: decodeCB_15(opcode);
        break;
      case 16: decodeCB_16(opcode);
        break;
      case 17: decodeCB_17(opcode);
        break;
      case 18: decodeCB_18(opcode);
        break;
      case 19: decodeCB_19(opcode);
        break;
      case 20: decodeCB_20(opcode);
        break;
      case 21: decodeCB_21(opcode);
        break;
      case 22: decodeCB_22(opcode);
        break;
      case 23: decodeCB_23(opcode);
        break;
      case 24: decodeCB_24(opcode);
        break;
      case 25: decodeCB_25(opcode);
        break;
      case 26: decodeCB_26(opcode);
        break;
      case 27: decodeCB_27(opcode);
        break;
      case 28: decodeCB_28(opcode);
        break;
      case 29: decodeCB_29(opcode);
        break;
      case 30: decodeCB_30(opcode);
        break;
      case 31: decodeCB_31(opcode);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_0(int opcode) {
    switch (opcode) {
      case 0x00: {
          int _F309;
          int value1_661 = B;
          value1_661 = (value1_661 << 1 | value1_661 >> 7) & 0xff;
          _F309 = (value1_661 & 1) | (SZ53P[value1_661] | (value1_661 == 0 ? 0x40 : 0));
          F = (_F309 & 0xFF);
          B = value1_661;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x01: {
          int _F311;
          int value1_665 = C;
          value1_665 = (value1_665 << 1 | value1_665 >> 7) & 0xff;
          _F311 = (value1_665 & 1) | (SZ53P[value1_665] | (value1_665 == 0 ? 0x40 : 0));
          F = (_F311 & 0xFF);
          C = value1_665;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _F313;
          int value1_669 = D;
          value1_669 = (value1_669 << 1 | value1_669 >> 7) & 0xff;
          _F313 = (value1_669 & 1) | (SZ53P[value1_669] | (value1_669 == 0 ? 0x40 : 0));
          F = (_F313 & 0xFF);
          D = value1_669;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x03: {
          int _F315;
          int value1_673 = E;
          value1_673 = (value1_673 << 1 | value1_673 >> 7) & 0xff;
          _F315 = (value1_673 & 1) | (SZ53P[value1_673] | (value1_673 == 0 ? 0x40 : 0));
          F = (_F315 & 0xFF);
          E = value1_673;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F317;
          int value1_677 = H;
          value1_677 = (value1_677 << 1 | value1_677 >> 7) & 0xff;
          _F317 = (value1_677 & 1) | (SZ53P[value1_677] | (value1_677 == 0 ? 0x40 : 0));
          F = (_F317 & 0xFF);
          H = value1_677;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F319;
          int value1_681 = L;
          value1_681 = (value1_681 << 1 | value1_681 >> 7) & 0xff;
          _F319 = (value1_681 & 1) | (SZ53P[value1_681] | (value1_681 == 0 ? 0x40 : 0));
          F = (_F319 & 0xFF);
          L = value1_681;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          int _F321;
          int _address84 = (H << 8) | L;
          int value1_685 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          value1_685 = (value1_685 << 1 | value1_685 >> 7) & 0xff;
          _F321 = (value1_685 & 1) | (SZ53P[value1_685] | (value1_685 == 0 ? 0x40 : 0));
          F = (_F321 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, value1_685);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F323;
          int value1_689 = A;
          value1_689 = (value1_689 << 1 | value1_689 >> 7) & 0xff;
          _F323 = (value1_689 & 1) | (SZ53P[value1_689] | (value1_689 == 0 ? 0x40 : 0));
          F = (_F323 & 0xFF);
          A = value1_689;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_1(int opcode) {
    switch (opcode) {
      case 0x08: {
          int _F325;
          int value1_693 = B;
          _F325 = value1_693 & 1;
          value1_693 = (value1_693 >> 1) | (value1_693 << 7);
          value1_693 &= 0xff;
          _F325 |= (SZ53P[value1_693 & 0xff] | (value1_693 == 0 ? 0x40 : 0));
          int result_696 = value1_693 & 0xFF;
          F = (_F325 & 0xFF);
          B = result_696;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F327;
          int value1_697 = C;
          _F327 = value1_697 & 1;
          value1_697 = (value1_697 >> 1) | (value1_697 << 7);
          value1_697 &= 0xff;
          _F327 |= (SZ53P[value1_697 & 0xff] | (value1_697 == 0 ? 0x40 : 0));
          int result_700 = value1_697 & 0xFF;
          F = (_F327 & 0xFF);
          C = result_700;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _F329;
          int value1_701 = D;
          _F329 = value1_701 & 1;
          value1_701 = (value1_701 >> 1) | (value1_701 << 7);
          value1_701 &= 0xff;
          _F329 |= (SZ53P[value1_701 & 0xff] | (value1_701 == 0 ? 0x40 : 0));
          int result_704 = value1_701 & 0xFF;
          F = (_F329 & 0xFF);
          D = result_704;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int _F331;
          int value1_705 = E;
          _F331 = value1_705 & 1;
          value1_705 = (value1_705 >> 1) | (value1_705 << 7);
          value1_705 &= 0xff;
          _F331 |= (SZ53P[value1_705 & 0xff] | (value1_705 == 0 ? 0x40 : 0));
          int result_708 = value1_705 & 0xFF;
          F = (_F331 & 0xFF);
          E = result_708;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F333;
          int value1_709 = H;
          _F333 = value1_709 & 1;
          value1_709 = (value1_709 >> 1) | (value1_709 << 7);
          value1_709 &= 0xff;
          _F333 |= (SZ53P[value1_709 & 0xff] | (value1_709 == 0 ? 0x40 : 0));
          int result_712 = value1_709 & 0xFF;
          F = (_F333 & 0xFF);
          H = result_712;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F335;
          int value1_713 = L;
          _F335 = value1_713 & 1;
          value1_713 = (value1_713 >> 1) | (value1_713 << 7);
          value1_713 &= 0xff;
          _F335 |= (SZ53P[value1_713 & 0xff] | (value1_713 == 0 ? 0x40 : 0));
          int result_716 = value1_713 & 0xFF;
          F = (_F335 & 0xFF);
          L = result_716;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int _F337;
          int _address84 = (H << 8) | L;
          int value1_717 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          _F337 = value1_717 & 1;
          value1_717 = (value1_717 >> 1) | (value1_717 << 7);
          value1_717 &= 0xff;
          _F337 |= (SZ53P[value1_717 & 0xff] | (value1_717 == 0 ? 0x40 : 0));
          int result_720 = value1_717 & 0xFF;
          F = (_F337 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_720);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F339;
          int value1_721 = A;
          _F339 = value1_721 & 1;
          value1_721 = (value1_721 >> 1) | (value1_721 << 7);
          value1_721 &= 0xff;
          _F339 |= (SZ53P[value1_721 & 0xff] | (value1_721 == 0 ? 0x40 : 0));
          int result_724 = value1_721 & 0xFF;
          F = (_F339 & 0xFF);
          A = result_724;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_2(int opcode) {
    switch (opcode) {
      case 0x10: {
          int _F341;
          int value1_725 = B;
          int value2_726 = F;
          _F341 = value2_726;
          int rltemp_728 = value1_725;
          value1_725 = (value1_725 << 1) | (_F341 & 1);
          value1_725 &= 0xff;
          _F341 = (rltemp_728 >> 7) | (SZ53P[value1_725 & 0xff] | (value1_725 == 0 ? 0x40 : 0));
          int result_729 = value1_725 & 0xFF;
          F = (_F341 & 0xFF);
          B = result_729;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x11: {
          int _F343;
          int value1_730 = C;
          int value2_731 = F;
          _F343 = value2_731;
          int rltemp_733 = value1_730;
          value1_730 = (value1_730 << 1) | (_F343 & 1);
          value1_730 &= 0xff;
          _F343 = (rltemp_733 >> 7) | (SZ53P[value1_730 & 0xff] | (value1_730 == 0 ? 0x40 : 0));
          int result_734 = value1_730 & 0xFF;
          F = (_F343 & 0xFF);
          C = result_734;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _F345;
          int value1_735 = D;
          int value2_736 = F;
          _F345 = value2_736;
          int rltemp_738 = value1_735;
          value1_735 = (value1_735 << 1) | (_F345 & 1);
          value1_735 &= 0xff;
          _F345 = (rltemp_738 >> 7) | (SZ53P[value1_735 & 0xff] | (value1_735 == 0 ? 0x40 : 0));
          int result_739 = value1_735 & 0xFF;
          F = (_F345 & 0xFF);
          D = result_739;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x13: {
          int _F347;
          int value1_740 = E;
          int value2_741 = F;
          _F347 = value2_741;
          int rltemp_743 = value1_740;
          value1_740 = (value1_740 << 1) | (_F347 & 1);
          value1_740 &= 0xff;
          _F347 = (rltemp_743 >> 7) | (SZ53P[value1_740 & 0xff] | (value1_740 == 0 ? 0x40 : 0));
          int result_744 = value1_740 & 0xFF;
          F = (_F347 & 0xFF);
          E = result_744;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F349;
          int value1_745 = H;
          int value2_746 = F;
          _F349 = value2_746;
          int rltemp_748 = value1_745;
          value1_745 = (value1_745 << 1) | (_F349 & 1);
          value1_745 &= 0xff;
          _F349 = (rltemp_748 >> 7) | (SZ53P[value1_745 & 0xff] | (value1_745 == 0 ? 0x40 : 0));
          int result_749 = value1_745 & 0xFF;
          F = (_F349 & 0xFF);
          H = result_749;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F351;
          int value1_750 = L;
          int value2_751 = F;
          _F351 = value2_751;
          int rltemp_753 = value1_750;
          value1_750 = (value1_750 << 1) | (_F351 & 1);
          value1_750 &= 0xff;
          _F351 = (rltemp_753 >> 7) | (SZ53P[value1_750 & 0xff] | (value1_750 == 0 ? 0x40 : 0));
          int result_754 = value1_750 & 0xFF;
          F = (_F351 & 0xFF);
          L = result_754;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          int _F353;
          int _address84 = (H << 8) | L;
          int value1_755 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value2_756 = F;
          _F353 = value2_756;
          int rltemp_758 = value1_755;
          value1_755 = (value1_755 << 1) | (_F353 & 1);
          value1_755 &= 0xff;
          _F353 = (rltemp_758 >> 7) | (SZ53P[value1_755 & 0xff] | (value1_755 == 0 ? 0x40 : 0));
          int result_759 = value1_755 & 0xFF;
          F = (_F353 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_759);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F355;
          int value1_760 = A;
          int value2_761 = F;
          _F355 = value2_761;
          int rltemp_763 = value1_760;
          value1_760 = (value1_760 << 1) | (_F355 & 1);
          value1_760 &= 0xff;
          _F355 = (rltemp_763 >> 7) | (SZ53P[value1_760 & 0xff] | (value1_760 == 0 ? 0x40 : 0));
          int result_764 = value1_760 & 0xFF;
          F = (_F355 & 0xFF);
          A = result_764;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_3(int opcode) {
    switch (opcode) {
      case 0x18: {
          int _F357;
          int value1_765 = B;
          int value2_766 = F;
          _F357 = value2_766;
          int rrtemp_768 = value1_765;
          value1_765 = (value1_765 >> 1) | (_F357 << 7);
          value1_765 &= 0xff;
          _F357 = (rrtemp_768 & 1) | (SZ53P[value1_765 & 0xff] | (value1_765 == 0 ? 0x40 : 0));
          int result_769 = value1_765 & 0xFF;
          F = (_F357 & 0xFF);
          B = result_769;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x19: {
          int _F359;
          int value1_770 = C;
          int value2_771 = F;
          _F359 = value2_771;
          int rrtemp_773 = value1_770;
          value1_770 = (value1_770 >> 1) | (_F359 << 7);
          value1_770 &= 0xff;
          _F359 = (rrtemp_773 & 1) | (SZ53P[value1_770 & 0xff] | (value1_770 == 0 ? 0x40 : 0));
          int result_774 = value1_770 & 0xFF;
          F = (_F359 & 0xFF);
          C = result_774;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _F361;
          int value1_775 = D;
          int value2_776 = F;
          _F361 = value2_776;
          int rrtemp_778 = value1_775;
          value1_775 = (value1_775 >> 1) | (_F361 << 7);
          value1_775 &= 0xff;
          _F361 = (rrtemp_778 & 1) | (SZ53P[value1_775 & 0xff] | (value1_775 == 0 ? 0x40 : 0));
          int result_779 = value1_775 & 0xFF;
          F = (_F361 & 0xFF);
          D = result_779;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int _F363;
          int value1_780 = E;
          int value2_781 = F;
          _F363 = value2_781;
          int rrtemp_783 = value1_780;
          value1_780 = (value1_780 >> 1) | (_F363 << 7);
          value1_780 &= 0xff;
          _F363 = (rrtemp_783 & 1) | (SZ53P[value1_780 & 0xff] | (value1_780 == 0 ? 0x40 : 0));
          int result_784 = value1_780 & 0xFF;
          F = (_F363 & 0xFF);
          E = result_784;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F365;
          int value1_785 = H;
          int value2_786 = F;
          _F365 = value2_786;
          int rrtemp_788 = value1_785;
          value1_785 = (value1_785 >> 1) | (_F365 << 7);
          value1_785 &= 0xff;
          _F365 = (rrtemp_788 & 1) | (SZ53P[value1_785 & 0xff] | (value1_785 == 0 ? 0x40 : 0));
          int result_789 = value1_785 & 0xFF;
          F = (_F365 & 0xFF);
          H = result_789;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F367;
          int value1_790 = L;
          int value2_791 = F;
          _F367 = value2_791;
          int rrtemp_793 = value1_790;
          value1_790 = (value1_790 >> 1) | (_F367 << 7);
          value1_790 &= 0xff;
          _F367 = (rrtemp_793 & 1) | (SZ53P[value1_790 & 0xff] | (value1_790 == 0 ? 0x40 : 0));
          int result_794 = value1_790 & 0xFF;
          F = (_F367 & 0xFF);
          L = result_794;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int _F369;
          int _address84 = (H << 8) | L;
          int value1_795 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value2_796 = F;
          _F369 = value2_796;
          int rrtemp_798 = value1_795;
          value1_795 = (value1_795 >> 1) | (_F369 << 7);
          value1_795 &= 0xff;
          _F369 = (rrtemp_798 & 1) | (SZ53P[value1_795 & 0xff] | (value1_795 == 0 ? 0x40 : 0));
          int result_799 = value1_795 & 0xFF;
          F = (_F369 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_799);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F371;
          int value1_800 = A;
          int value2_801 = F;
          _F371 = value2_801;
          int rrtemp_803 = value1_800;
          value1_800 = (value1_800 >> 1) | (_F371 << 7);
          value1_800 &= 0xff;
          _F371 = (rrtemp_803 & 1) | (SZ53P[value1_800 & 0xff] | (value1_800 == 0 ? 0x40 : 0));
          int result_804 = value1_800 & 0xFF;
          F = (_F371 & 0xFF);
          A = result_804;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_4(int opcode) {
    switch (opcode) {
      case 0x20: {
          int _F373;
          int value1_805 = B;
          _F373 = value1_805 >> 7;
          value1_805 <<= 1;
          value1_805 &= 0xff;
          _F373 |= (SZ53P[value1_805 & 0xff] | (value1_805 == 0 ? 0x40 : 0));
          int result_808 = value1_805 & 0xFF;
          F = (_F373 & 0xFF);
          B = result_808;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x21: {
          int _F375;
          int value1_809 = C;
          _F375 = value1_809 >> 7;
          value1_809 <<= 1;
          value1_809 &= 0xff;
          _F375 |= (SZ53P[value1_809 & 0xff] | (value1_809 == 0 ? 0x40 : 0));
          int result_812 = value1_809 & 0xFF;
          F = (_F375 & 0xFF);
          C = result_812;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _F377;
          int value1_813 = D;
          _F377 = value1_813 >> 7;
          value1_813 <<= 1;
          value1_813 &= 0xff;
          _F377 |= (SZ53P[value1_813 & 0xff] | (value1_813 == 0 ? 0x40 : 0));
          int result_816 = value1_813 & 0xFF;
          F = (_F377 & 0xFF);
          D = result_816;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x23: {
          int _F379;
          int value1_817 = E;
          _F379 = value1_817 >> 7;
          value1_817 <<= 1;
          value1_817 &= 0xff;
          _F379 |= (SZ53P[value1_817 & 0xff] | (value1_817 == 0 ? 0x40 : 0));
          int result_820 = value1_817 & 0xFF;
          F = (_F379 & 0xFF);
          E = result_820;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F381;
          int value1_821 = H;
          _F381 = value1_821 >> 7;
          value1_821 <<= 1;
          value1_821 &= 0xff;
          _F381 |= (SZ53P[value1_821 & 0xff] | (value1_821 == 0 ? 0x40 : 0));
          int result_824 = value1_821 & 0xFF;
          F = (_F381 & 0xFF);
          H = result_824;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F383;
          int value1_825 = L;
          _F383 = value1_825 >> 7;
          value1_825 <<= 1;
          value1_825 &= 0xff;
          _F383 |= (SZ53P[value1_825 & 0xff] | (value1_825 == 0 ? 0x40 : 0));
          int result_828 = value1_825 & 0xFF;
          F = (_F383 & 0xFF);
          L = result_828;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          int _F385;
          int _address84 = (H << 8) | L;
          int value1_829 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          _F385 = value1_829 >> 7;
          value1_829 <<= 1;
          value1_829 &= 0xff;
          _F385 |= (SZ53P[value1_829 & 0xff] | (value1_829 == 0 ? 0x40 : 0));
          int result_832 = value1_829 & 0xFF;
          F = (_F385 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_832);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F387;
          int value1_833 = A;
          _F387 = value1_833 >> 7;
          value1_833 <<= 1;
          value1_833 &= 0xff;
          _F387 |= (SZ53P[value1_833 & 0xff] | (value1_833 == 0 ? 0x40 : 0));
          int result_836 = value1_833 & 0xFF;
          F = (_F387 & 0xFF);
          A = result_836;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_5(int opcode) {
    switch (opcode) {
      case 0x28: {
          int _F389;
          int value1_837 = B;
          _F389 = value1_837 & 1;
          value1_837 = (value1_837 & 0x80) | (value1_837 >> 1);
          value1_837 &= 0xff;
          _F389 |= (SZ53P[value1_837 & 0xff] | (value1_837 == 0 ? 0x40 : 0));
          int result_840 = value1_837 & 0xFF;
          F = (_F389 & 0xFF);
          B = result_840;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x29: {
          int _F391;
          int value1_841 = C;
          _F391 = value1_841 & 1;
          value1_841 = (value1_841 & 0x80) | (value1_841 >> 1);
          value1_841 &= 0xff;
          _F391 |= (SZ53P[value1_841 & 0xff] | (value1_841 == 0 ? 0x40 : 0));
          int result_844 = value1_841 & 0xFF;
          F = (_F391 & 0xFF);
          C = result_844;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _F393;
          int value1_845 = D;
          _F393 = value1_845 & 1;
          value1_845 = (value1_845 & 0x80) | (value1_845 >> 1);
          value1_845 &= 0xff;
          _F393 |= (SZ53P[value1_845 & 0xff] | (value1_845 == 0 ? 0x40 : 0));
          int result_848 = value1_845 & 0xFF;
          F = (_F393 & 0xFF);
          D = result_848;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2B: {
          int _F395;
          int value1_849 = E;
          _F395 = value1_849 & 1;
          value1_849 = (value1_849 & 0x80) | (value1_849 >> 1);
          value1_849 &= 0xff;
          _F395 |= (SZ53P[value1_849 & 0xff] | (value1_849 == 0 ? 0x40 : 0));
          int result_852 = value1_849 & 0xFF;
          F = (_F395 & 0xFF);
          E = result_852;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F397;
          int value1_853 = H;
          _F397 = value1_853 & 1;
          value1_853 = (value1_853 & 0x80) | (value1_853 >> 1);
          value1_853 &= 0xff;
          _F397 |= (SZ53P[value1_853 & 0xff] | (value1_853 == 0 ? 0x40 : 0));
          int result_856 = value1_853 & 0xFF;
          F = (_F397 & 0xFF);
          H = result_856;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F399;
          int value1_857 = L;
          _F399 = value1_857 & 1;
          value1_857 = (value1_857 & 0x80) | (value1_857 >> 1);
          value1_857 &= 0xff;
          _F399 |= (SZ53P[value1_857 & 0xff] | (value1_857 == 0 ? 0x40 : 0));
          int result_860 = value1_857 & 0xFF;
          F = (_F399 & 0xFF);
          L = result_860;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int _F401;
          int _address84 = (H << 8) | L;
          int value1_861 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          _F401 = value1_861 & 1;
          value1_861 = (value1_861 & 0x80) | (value1_861 >> 1);
          value1_861 &= 0xff;
          _F401 |= (SZ53P[value1_861 & 0xff] | (value1_861 == 0 ? 0x40 : 0));
          int result_864 = value1_861 & 0xFF;
          F = (_F401 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_864);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F403;
          int value1_865 = A;
          _F403 = value1_865 & 1;
          value1_865 = (value1_865 & 0x80) | (value1_865 >> 1);
          value1_865 &= 0xff;
          _F403 |= (SZ53P[value1_865 & 0xff] | (value1_865 == 0 ? 0x40 : 0));
          int result_868 = value1_865 & 0xFF;
          F = (_F403 & 0xFF);
          A = result_868;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_6(int opcode) {
    switch (opcode) {
      case 0x30: {
          int _F405;
          int value1_869 = B;
          _F405 = value1_869 >> 7;
          value1_869 = (value1_869 << 1) | 0x01;
          value1_869 &= 0xff;
          _F405 |= (SZ53P[value1_869 & 0xff] | (value1_869 == 0 ? 0x40 : 0));
          int result_872 = value1_869 & 0xFF;
          F = (_F405 & 0xFF);
          B = result_872;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x31: {
          int _F407;
          int value1_873 = C;
          _F407 = value1_873 >> 7;
          value1_873 = (value1_873 << 1) | 0x01;
          value1_873 &= 0xff;
          _F407 |= (SZ53P[value1_873 & 0xff] | (value1_873 == 0 ? 0x40 : 0));
          int result_876 = value1_873 & 0xFF;
          F = (_F407 & 0xFF);
          C = result_876;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _F409;
          int value1_877 = D;
          _F409 = value1_877 >> 7;
          value1_877 = (value1_877 << 1) | 0x01;
          value1_877 &= 0xff;
          _F409 |= (SZ53P[value1_877 & 0xff] | (value1_877 == 0 ? 0x40 : 0));
          int result_880 = value1_877 & 0xFF;
          F = (_F409 & 0xFF);
          D = result_880;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x33: {
          int _F411;
          int value1_881 = E;
          _F411 = value1_881 >> 7;
          value1_881 = (value1_881 << 1) | 0x01;
          value1_881 &= 0xff;
          _F411 |= (SZ53P[value1_881 & 0xff] | (value1_881 == 0 ? 0x40 : 0));
          int result_884 = value1_881 & 0xFF;
          F = (_F411 & 0xFF);
          E = result_884;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F413;
          int value1_885 = H;
          _F413 = value1_885 >> 7;
          value1_885 = (value1_885 << 1) | 0x01;
          value1_885 &= 0xff;
          _F413 |= (SZ53P[value1_885 & 0xff] | (value1_885 == 0 ? 0x40 : 0));
          int result_888 = value1_885 & 0xFF;
          F = (_F413 & 0xFF);
          H = result_888;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F415;
          int value1_889 = L;
          _F415 = value1_889 >> 7;
          value1_889 = (value1_889 << 1) | 0x01;
          value1_889 &= 0xff;
          _F415 |= (SZ53P[value1_889 & 0xff] | (value1_889 == 0 ? 0x40 : 0));
          int result_892 = value1_889 & 0xFF;
          F = (_F415 & 0xFF);
          L = result_892;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _F417;
          int _address84 = (H << 8) | L;
          int value1_893 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          _F417 = value1_893 >> 7;
          value1_893 = (value1_893 << 1) | 0x01;
          value1_893 &= 0xff;
          _F417 |= (SZ53P[value1_893 & 0xff] | (value1_893 == 0 ? 0x40 : 0));
          int result_896 = value1_893 & 0xFF;
          F = (_F417 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_896);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F419;
          int value1_897 = A;
          _F419 = value1_897 >> 7;
          value1_897 = (value1_897 << 1) | 0x01;
          value1_897 &= 0xff;
          _F419 |= (SZ53P[value1_897 & 0xff] | (value1_897 == 0 ? 0x40 : 0));
          int result_900 = value1_897 & 0xFF;
          F = (_F419 & 0xFF);
          A = result_900;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_7(int opcode) {
    switch (opcode) {
      case 0x38: {
          int _F421;
          int value1_901 = B;
          _F421 = value1_901 & 1;
          value1_901 >>= 1;
          value1_901 &= 0xff;
          _F421 |= (SZ53P[value1_901 & 0xff] | (value1_901 == 0 ? 0x40 : 0));
          int result_904 = value1_901 & 0xFF;
          F = (_F421 & 0xFF);
          B = result_904;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x39: {
          int _F423;
          int value1_905 = C;
          _F423 = value1_905 & 1;
          value1_905 >>= 1;
          value1_905 &= 0xff;
          _F423 |= (SZ53P[value1_905 & 0xff] | (value1_905 == 0 ? 0x40 : 0));
          int result_908 = value1_905 & 0xFF;
          F = (_F423 & 0xFF);
          C = result_908;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _F425;
          int value1_909 = D;
          _F425 = value1_909 & 1;
          value1_909 >>= 1;
          value1_909 &= 0xff;
          _F425 |= (SZ53P[value1_909 & 0xff] | (value1_909 == 0 ? 0x40 : 0));
          int result_912 = value1_909 & 0xFF;
          F = (_F425 & 0xFF);
          D = result_912;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3B: {
          int _F427;
          int value1_913 = E;
          _F427 = value1_913 & 1;
          value1_913 >>= 1;
          value1_913 &= 0xff;
          _F427 |= (SZ53P[value1_913 & 0xff] | (value1_913 == 0 ? 0x40 : 0));
          int result_916 = value1_913 & 0xFF;
          F = (_F427 & 0xFF);
          E = result_916;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F429;
          int value1_917 = H;
          _F429 = value1_917 & 1;
          value1_917 >>= 1;
          value1_917 &= 0xff;
          _F429 |= (SZ53P[value1_917 & 0xff] | (value1_917 == 0 ? 0x40 : 0));
          int result_920 = value1_917 & 0xFF;
          F = (_F429 & 0xFF);
          H = result_920;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F431;
          int value1_921 = L;
          _F431 = value1_921 & 1;
          value1_921 >>= 1;
          value1_921 &= 0xff;
          _F431 |= (SZ53P[value1_921 & 0xff] | (value1_921 == 0 ? 0x40 : 0));
          int result_924 = value1_921 & 0xFF;
          F = (_F431 & 0xFF);
          L = result_924;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int _F433;
          int _address84 = (H << 8) | L;
          int value1_925 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          _F433 = value1_925 & 1;
          value1_925 >>= 1;
          value1_925 &= 0xff;
          _F433 |= (SZ53P[value1_925 & 0xff] | (value1_925 == 0 ? 0x40 : 0));
          int result_928 = value1_925 & 0xFF;
          F = (_F433 & 0xFF);
          _address84 = (H << 8) | L;
          write(_address84, result_928);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F435;
          int value1_929 = A;
          _F435 = value1_929 & 1;
          value1_929 >>= 1;
          value1_929 &= 0xff;
          _F435 |= (SZ53P[value1_929 & 0xff] | (value1_929 == 0 ? 0x40 : 0));
          int result_932 = value1_929 & 0xFF;
          F = (_F435 & 0xFF);
          A = result_932;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_8(int opcode) {
    switch (opcode) {
      case 0x40: {
          int _F437;
          int address_933;
          address_933 = B;
          int nAndCarry_934 = F & 1;
          int value3_937 = nAndCarry_934;
          _F437 = value3_937;
          value3_937 = value3_937 >>> 1;
          _F437 = (_F437 & 1) | 0x10 | (address_933 & 0x28);
          if ((B & (0x01 << value3_937)) == 0) {
              _F437 |= 0x44;
          }
          if (value3_937 == 7 && (B & 0x80) != 0) {
              _F437 |= 0x80;
          }
          F = (_F437 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x41: {
          int _F439;
          int address_939;
          address_939 = C;
          int nAndCarry_940 = F & 1;
          int value3_943 = nAndCarry_940;
          _F439 = value3_943;
          value3_943 = value3_943 >>> 1;
          _F439 = (_F439 & 1) | 0x10 | (address_939 & 0x28);
          if ((C & (0x01 << value3_943)) == 0) {
              _F439 |= 0x44;
          }
          if (value3_943 == 7 && (C & 0x80) != 0) {
              _F439 |= 0x80;
          }
          F = (_F439 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x42: {
          int _F441;
          int address_945;
          address_945 = D;
          int nAndCarry_946 = F & 1;
          int value3_949 = nAndCarry_946;
          _F441 = value3_949;
          value3_949 = value3_949 >>> 1;
          _F441 = (_F441 & 1) | 0x10 | (address_945 & 0x28);
          if ((D & (0x01 << value3_949)) == 0) {
              _F441 |= 0x44;
          }
          if (value3_949 == 7 && (D & 0x80) != 0) {
              _F441 |= 0x80;
          }
          F = (_F441 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x43: {
          int _F443;
          int address_951;
          address_951 = E;
          int nAndCarry_952 = F & 1;
          int value3_955 = nAndCarry_952;
          _F443 = value3_955;
          value3_955 = value3_955 >>> 1;
          _F443 = (_F443 & 1) | 0x10 | (address_951 & 0x28);
          if ((E & (0x01 << value3_955)) == 0) {
              _F443 |= 0x44;
          }
          if (value3_955 == 7 && (E & 0x80) != 0) {
              _F443 |= 0x80;
          }
          F = (_F443 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x44: {
          int _F445;
          int address_957;
          address_957 = H;
          int nAndCarry_958 = F & 1;
          int value3_961 = nAndCarry_958;
          _F445 = value3_961;
          value3_961 = value3_961 >>> 1;
          _F445 = (_F445 & 1) | 0x10 | (address_957 & 0x28);
          if ((H & (0x01 << value3_961)) == 0) {
              _F445 |= 0x44;
          }
          if (value3_961 == 7 && (H & 0x80) != 0) {
              _F445 |= 0x80;
          }
          F = (_F445 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x45: {
          int _F447;
          int address_963;
          address_963 = L;
          int nAndCarry_964 = F & 1;
          int value3_967 = nAndCarry_964;
          _F447 = value3_967;
          value3_967 = value3_967 >>> 1;
          _F447 = (_F447 & 1) | 0x10 | (address_963 & 0x28);
          if ((L & (0x01 << value3_967)) == 0) {
              _F447 |= 0x44;
          }
          if (value3_967 == 7 && (L & 0x80) != 0) {
              _F447 |= 0x80;
          }
          F = (_F447 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _F449;
          int _address84;
          int address_969;
          address_969 = MEMPTR >>> 8;
          int nAndCarry_970 = F & 1;
          _address84 = (H << 8) | L;
          int value2_972 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_973 = nAndCarry_970;
          _F449 = value3_973;
          value3_973 = value3_973 >>> 1;
          _F449 = (_F449 & 1) | 0x10 | (address_969 & 0x28);
          if ((value2_972 & (0x01 << value3_973)) == 0) {
              _F449 |= 0x44;
          }
          if (value3_973 == 7 && (value2_972 & 0x80) != 0) {
              _F449 |= 0x80;
          }
          F = (_F449 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x47: {
          int _F451;
          int address_975;
          address_975 = A;
          int nAndCarry_976 = F & 1;
          int value3_979 = nAndCarry_976;
          _F451 = value3_979;
          value3_979 = value3_979 >>> 1;
          _F451 = (_F451 & 1) | 0x10 | (address_975 & 0x28);
          if ((A & (0x01 << value3_979)) == 0) {
              _F451 |= 0x44;
          }
          if (value3_979 == 7 && (A & 0x80) != 0) {
              _F451 |= 0x80;
          }
          F = (_F451 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_9(int opcode) {
    switch (opcode) {
      case 0x48: {
          int _F453;
          int address_981;
          address_981 = B;
          int nAndCarry_982 = 2 | F & 1;
          int value3_985 = nAndCarry_982;
          _F453 = value3_985 & 1;
          value3_985 = value3_985 >>> 1;
          _F453 = (_F453 & 1) | 0x10 | (address_981 & 0x28);
          if ((B & (0x01 << value3_985)) == 0) {
              _F453 |= 0x44;
          }
          if (value3_985 == 7 && (B & 0x80) != 0) {
              _F453 |= 0x80;
          }
          F = (_F453 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x49: {
          int _F455;
          int address_987;
          address_987 = C;
          int nAndCarry_988 = 2 | F & 1;
          int value3_991 = nAndCarry_988;
          _F455 = value3_991 & 1;
          value3_991 = value3_991 >>> 1;
          _F455 = (_F455 & 1) | 0x10 | (address_987 & 0x28);
          if ((C & (0x01 << value3_991)) == 0) {
              _F455 |= 0x44;
          }
          if (value3_991 == 7 && (C & 0x80) != 0) {
              _F455 |= 0x80;
          }
          F = (_F455 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4A: {
          int _F457;
          int address_993;
          address_993 = D;
          int nAndCarry_994 = 2 | F & 1;
          int value3_997 = nAndCarry_994;
          _F457 = value3_997 & 1;
          value3_997 = value3_997 >>> 1;
          _F457 = (_F457 & 1) | 0x10 | (address_993 & 0x28);
          if ((D & (0x01 << value3_997)) == 0) {
              _F457 |= 0x44;
          }
          if (value3_997 == 7 && (D & 0x80) != 0) {
              _F457 |= 0x80;
          }
          F = (_F457 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4B: {
          int _F459;
          int address_999;
          address_999 = E;
          int nAndCarry_1000 = 2 | F & 1;
          int value3_1003 = nAndCarry_1000;
          _F459 = value3_1003 & 1;
          value3_1003 = value3_1003 >>> 1;
          _F459 = (_F459 & 1) | 0x10 | (address_999 & 0x28);
          if ((E & (0x01 << value3_1003)) == 0) {
              _F459 |= 0x44;
          }
          if (value3_1003 == 7 && (E & 0x80) != 0) {
              _F459 |= 0x80;
          }
          F = (_F459 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4C: {
          int _F461;
          int address_1005;
          address_1005 = H;
          int nAndCarry_1006 = 2 | F & 1;
          int value3_1009 = nAndCarry_1006;
          _F461 = value3_1009 & 1;
          value3_1009 = value3_1009 >>> 1;
          _F461 = (_F461 & 1) | 0x10 | (address_1005 & 0x28);
          if ((H & (0x01 << value3_1009)) == 0) {
              _F461 |= 0x44;
          }
          if (value3_1009 == 7 && (H & 0x80) != 0) {
              _F461 |= 0x80;
          }
          F = (_F461 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4D: {
          int _F463;
          int address_1011;
          address_1011 = L;
          int nAndCarry_1012 = 2 | F & 1;
          int value3_1015 = nAndCarry_1012;
          _F463 = value3_1015 & 1;
          value3_1015 = value3_1015 >>> 1;
          _F463 = (_F463 & 1) | 0x10 | (address_1011 & 0x28);
          if ((L & (0x01 << value3_1015)) == 0) {
              _F463 |= 0x44;
          }
          if (value3_1015 == 7 && (L & 0x80) != 0) {
              _F463 |= 0x80;
          }
          F = (_F463 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _F465;
          int _address84;
          int address_1017;
          address_1017 = MEMPTR >>> 8;
          int nAndCarry_1018 = 2 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1020 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1021 = nAndCarry_1018;
          _F465 = value3_1021 & 1;
          value3_1021 = value3_1021 >>> 1;
          _F465 = (_F465 & 1) | 0x10 | (address_1017 & 0x28);
          if ((value2_1020 & (0x01 << value3_1021)) == 0) {
              _F465 |= 0x44;
          }
          if (value3_1021 == 7 && (value2_1020 & 0x80) != 0) {
              _F465 |= 0x80;
          }
          F = (_F465 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4F: {
          int _F467;
          int address_1023;
          address_1023 = A;
          int nAndCarry_1024 = 2 | F & 1;
          int value3_1027 = nAndCarry_1024;
          _F467 = value3_1027 & 1;
          value3_1027 = value3_1027 >>> 1;
          _F467 = (_F467 & 1) | 0x10 | (address_1023 & 0x28);
          if ((A & (0x01 << value3_1027)) == 0) {
              _F467 |= 0x44;
          }
          if (value3_1027 == 7 && (A & 0x80) != 0) {
              _F467 |= 0x80;
          }
          F = (_F467 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_10(int opcode) {
    switch (opcode) {
      case 0x50: {
          int _F469;
          int address_1029;
          address_1029 = B;
          int nAndCarry_1030 = 4 | F & 1;
          int value3_1033 = nAndCarry_1030;
          _F469 = value3_1033 & 1;
          value3_1033 = value3_1033 >>> 1;
          _F469 = (_F469 & 1) | 0x10 | (address_1029 & 0x28);
          if ((B & (0x01 << value3_1033)) == 0) {
              _F469 |= 0x44;
          }
          if (value3_1033 == 7 && (B & 0x80) != 0) {
              _F469 |= 0x80;
          }
          F = (_F469 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x51: {
          int _F471;
          int address_1035;
          address_1035 = C;
          int nAndCarry_1036 = 4 | F & 1;
          int value3_1039 = nAndCarry_1036;
          _F471 = value3_1039 & 1;
          value3_1039 = value3_1039 >>> 1;
          _F471 = (_F471 & 1) | 0x10 | (address_1035 & 0x28);
          if ((C & (0x01 << value3_1039)) == 0) {
              _F471 |= 0x44;
          }
          if (value3_1039 == 7 && (C & 0x80) != 0) {
              _F471 |= 0x80;
          }
          F = (_F471 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x52: {
          int _F473;
          int address_1041;
          address_1041 = D;
          int nAndCarry_1042 = 4 | F & 1;
          int value3_1045 = nAndCarry_1042;
          _F473 = value3_1045 & 1;
          value3_1045 = value3_1045 >>> 1;
          _F473 = (_F473 & 1) | 0x10 | (address_1041 & 0x28);
          if ((D & (0x01 << value3_1045)) == 0) {
              _F473 |= 0x44;
          }
          if (value3_1045 == 7 && (D & 0x80) != 0) {
              _F473 |= 0x80;
          }
          F = (_F473 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x53: {
          int _F475;
          int address_1047;
          address_1047 = E;
          int nAndCarry_1048 = 4 | F & 1;
          int value3_1051 = nAndCarry_1048;
          _F475 = value3_1051 & 1;
          value3_1051 = value3_1051 >>> 1;
          _F475 = (_F475 & 1) | 0x10 | (address_1047 & 0x28);
          if ((E & (0x01 << value3_1051)) == 0) {
              _F475 |= 0x44;
          }
          if (value3_1051 == 7 && (E & 0x80) != 0) {
              _F475 |= 0x80;
          }
          F = (_F475 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x54: {
          int _F477;
          int address_1053;
          address_1053 = H;
          int nAndCarry_1054 = 4 | F & 1;
          int value3_1057 = nAndCarry_1054;
          _F477 = value3_1057 & 1;
          value3_1057 = value3_1057 >>> 1;
          _F477 = (_F477 & 1) | 0x10 | (address_1053 & 0x28);
          if ((H & (0x01 << value3_1057)) == 0) {
              _F477 |= 0x44;
          }
          if (value3_1057 == 7 && (H & 0x80) != 0) {
              _F477 |= 0x80;
          }
          F = (_F477 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x55: {
          int _F479;
          int address_1059;
          address_1059 = L;
          int nAndCarry_1060 = 4 | F & 1;
          int value3_1063 = nAndCarry_1060;
          _F479 = value3_1063 & 1;
          value3_1063 = value3_1063 >>> 1;
          _F479 = (_F479 & 1) | 0x10 | (address_1059 & 0x28);
          if ((L & (0x01 << value3_1063)) == 0) {
              _F479 |= 0x44;
          }
          if (value3_1063 == 7 && (L & 0x80) != 0) {
              _F479 |= 0x80;
          }
          F = (_F479 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _F481;
          int _address84;
          int address_1065;
          address_1065 = MEMPTR >>> 8;
          int nAndCarry_1066 = 4 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1068 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1069 = nAndCarry_1066;
          _F481 = value3_1069 & 1;
          value3_1069 = value3_1069 >>> 1;
          _F481 = (_F481 & 1) | 0x10 | (address_1065 & 0x28);
          if ((value2_1068 & (0x01 << value3_1069)) == 0) {
              _F481 |= 0x44;
          }
          if (value3_1069 == 7 && (value2_1068 & 0x80) != 0) {
              _F481 |= 0x80;
          }
          F = (_F481 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x57: {
          int _F483;
          int address_1071;
          address_1071 = A;
          int nAndCarry_1072 = 4 | F & 1;
          int value3_1075 = nAndCarry_1072;
          _F483 = value3_1075 & 1;
          value3_1075 = value3_1075 >>> 1;
          _F483 = (_F483 & 1) | 0x10 | (address_1071 & 0x28);
          if ((A & (0x01 << value3_1075)) == 0) {
              _F483 |= 0x44;
          }
          if (value3_1075 == 7 && (A & 0x80) != 0) {
              _F483 |= 0x80;
          }
          F = (_F483 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_11(int opcode) {
    switch (opcode) {
      case 0x58: {
          int _F485;
          int address_1077;
          address_1077 = B;
          int nAndCarry_1078 = 6 | F & 1;
          int value3_1081 = nAndCarry_1078;
          _F485 = value3_1081 & 1;
          value3_1081 = value3_1081 >>> 1;
          _F485 = (_F485 & 1) | 0x10 | (address_1077 & 0x28);
          if ((B & (0x01 << value3_1081)) == 0) {
              _F485 |= 0x44;
          }
          if (value3_1081 == 7 && (B & 0x80) != 0) {
              _F485 |= 0x80;
          }
          F = (_F485 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x59: {
          int _F487;
          int address_1083;
          address_1083 = C;
          int nAndCarry_1084 = 6 | F & 1;
          int value3_1087 = nAndCarry_1084;
          _F487 = value3_1087 & 1;
          value3_1087 = value3_1087 >>> 1;
          _F487 = (_F487 & 1) | 0x10 | (address_1083 & 0x28);
          if ((C & (0x01 << value3_1087)) == 0) {
              _F487 |= 0x44;
          }
          if (value3_1087 == 7 && (C & 0x80) != 0) {
              _F487 |= 0x80;
          }
          F = (_F487 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5A: {
          int _F489;
          int address_1089;
          address_1089 = D;
          int nAndCarry_1090 = 6 | F & 1;
          int value3_1093 = nAndCarry_1090;
          _F489 = value3_1093 & 1;
          value3_1093 = value3_1093 >>> 1;
          _F489 = (_F489 & 1) | 0x10 | (address_1089 & 0x28);
          if ((D & (0x01 << value3_1093)) == 0) {
              _F489 |= 0x44;
          }
          if (value3_1093 == 7 && (D & 0x80) != 0) {
              _F489 |= 0x80;
          }
          F = (_F489 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5B: {
          int _F491;
          int address_1095;
          address_1095 = E;
          int nAndCarry_1096 = 6 | F & 1;
          int value3_1099 = nAndCarry_1096;
          _F491 = value3_1099 & 1;
          value3_1099 = value3_1099 >>> 1;
          _F491 = (_F491 & 1) | 0x10 | (address_1095 & 0x28);
          if ((E & (0x01 << value3_1099)) == 0) {
              _F491 |= 0x44;
          }
          if (value3_1099 == 7 && (E & 0x80) != 0) {
              _F491 |= 0x80;
          }
          F = (_F491 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5C: {
          int _F493;
          int address_1101;
          address_1101 = H;
          int nAndCarry_1102 = 6 | F & 1;
          int value3_1105 = nAndCarry_1102;
          _F493 = value3_1105 & 1;
          value3_1105 = value3_1105 >>> 1;
          _F493 = (_F493 & 1) | 0x10 | (address_1101 & 0x28);
          if ((H & (0x01 << value3_1105)) == 0) {
              _F493 |= 0x44;
          }
          if (value3_1105 == 7 && (H & 0x80) != 0) {
              _F493 |= 0x80;
          }
          F = (_F493 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5D: {
          int _F495;
          int address_1107;
          address_1107 = L;
          int nAndCarry_1108 = 6 | F & 1;
          int value3_1111 = nAndCarry_1108;
          _F495 = value3_1111 & 1;
          value3_1111 = value3_1111 >>> 1;
          _F495 = (_F495 & 1) | 0x10 | (address_1107 & 0x28);
          if ((L & (0x01 << value3_1111)) == 0) {
              _F495 |= 0x44;
          }
          if (value3_1111 == 7 && (L & 0x80) != 0) {
              _F495 |= 0x80;
          }
          F = (_F495 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _F497;
          int _address84;
          int address_1113;
          address_1113 = MEMPTR >>> 8;
          int nAndCarry_1114 = 6 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1116 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1117 = nAndCarry_1114;
          _F497 = value3_1117 & 1;
          value3_1117 = value3_1117 >>> 1;
          _F497 = (_F497 & 1) | 0x10 | (address_1113 & 0x28);
          if ((value2_1116 & (0x01 << value3_1117)) == 0) {
              _F497 |= 0x44;
          }
          if (value3_1117 == 7 && (value2_1116 & 0x80) != 0) {
              _F497 |= 0x80;
          }
          F = (_F497 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5F: {
          int _F499;
          int address_1119;
          address_1119 = A;
          int nAndCarry_1120 = 6 | F & 1;
          int value3_1123 = nAndCarry_1120;
          _F499 = value3_1123 & 1;
          value3_1123 = value3_1123 >>> 1;
          _F499 = (_F499 & 1) | 0x10 | (address_1119 & 0x28);
          if ((A & (0x01 << value3_1123)) == 0) {
              _F499 |= 0x44;
          }
          if (value3_1123 == 7 && (A & 0x80) != 0) {
              _F499 |= 0x80;
          }
          F = (_F499 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_12(int opcode) {
    switch (opcode) {
      case 0x60: {
          int _F501;
          int address_1125;
          address_1125 = B;
          int nAndCarry_1126 = 8 | F & 1;
          int value3_1129 = nAndCarry_1126;
          _F501 = value3_1129 & 1;
          value3_1129 = value3_1129 >>> 1;
          _F501 = (_F501 & 1) | 0x10 | (address_1125 & 0x28);
          if ((B & (0x01 << value3_1129)) == 0) {
              _F501 |= 0x44;
          }
          if (value3_1129 == 7 && (B & 0x80) != 0) {
              _F501 |= 0x80;
          }
          F = (_F501 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x61: {
          int _F503;
          int address_1131;
          address_1131 = C;
          int nAndCarry_1132 = 8 | F & 1;
          int value3_1135 = nAndCarry_1132;
          _F503 = value3_1135 & 1;
          value3_1135 = value3_1135 >>> 1;
          _F503 = (_F503 & 1) | 0x10 | (address_1131 & 0x28);
          if ((C & (0x01 << value3_1135)) == 0) {
              _F503 |= 0x44;
          }
          if (value3_1135 == 7 && (C & 0x80) != 0) {
              _F503 |= 0x80;
          }
          F = (_F503 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x62: {
          int _F505;
          int address_1137;
          address_1137 = D;
          int nAndCarry_1138 = 8 | F & 1;
          int value3_1141 = nAndCarry_1138;
          _F505 = value3_1141 & 1;
          value3_1141 = value3_1141 >>> 1;
          _F505 = (_F505 & 1) | 0x10 | (address_1137 & 0x28);
          if ((D & (0x01 << value3_1141)) == 0) {
              _F505 |= 0x44;
          }
          if (value3_1141 == 7 && (D & 0x80) != 0) {
              _F505 |= 0x80;
          }
          F = (_F505 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x63: {
          int _F507;
          int address_1143;
          address_1143 = E;
          int nAndCarry_1144 = 8 | F & 1;
          int value3_1147 = nAndCarry_1144;
          _F507 = value3_1147 & 1;
          value3_1147 = value3_1147 >>> 1;
          _F507 = (_F507 & 1) | 0x10 | (address_1143 & 0x28);
          if ((E & (0x01 << value3_1147)) == 0) {
              _F507 |= 0x44;
          }
          if (value3_1147 == 7 && (E & 0x80) != 0) {
              _F507 |= 0x80;
          }
          F = (_F507 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x64: {
          int _F509;
          int address_1149;
          address_1149 = H;
          int nAndCarry_1150 = 8 | F & 1;
          int value3_1153 = nAndCarry_1150;
          _F509 = value3_1153 & 1;
          value3_1153 = value3_1153 >>> 1;
          _F509 = (_F509 & 1) | 0x10 | (address_1149 & 0x28);
          if ((H & (0x01 << value3_1153)) == 0) {
              _F509 |= 0x44;
          }
          if (value3_1153 == 7 && (H & 0x80) != 0) {
              _F509 |= 0x80;
          }
          F = (_F509 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x65: {
          int _F511;
          int address_1155;
          address_1155 = L;
          int nAndCarry_1156 = 8 | F & 1;
          int value3_1159 = nAndCarry_1156;
          _F511 = value3_1159 & 1;
          value3_1159 = value3_1159 >>> 1;
          _F511 = (_F511 & 1) | 0x10 | (address_1155 & 0x28);
          if ((L & (0x01 << value3_1159)) == 0) {
              _F511 |= 0x44;
          }
          if (value3_1159 == 7 && (L & 0x80) != 0) {
              _F511 |= 0x80;
          }
          F = (_F511 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _F513;
          int _address84;
          int address_1161;
          address_1161 = MEMPTR >>> 8;
          int nAndCarry_1162 = 8 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1164 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1165 = nAndCarry_1162;
          _F513 = value3_1165 & 1;
          value3_1165 = value3_1165 >>> 1;
          _F513 = (_F513 & 1) | 0x10 | (address_1161 & 0x28);
          if ((value2_1164 & (0x01 << value3_1165)) == 0) {
              _F513 |= 0x44;
          }
          if (value3_1165 == 7 && (value2_1164 & 0x80) != 0) {
              _F513 |= 0x80;
          }
          F = (_F513 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x67: {
          int _F515;
          int address_1167;
          address_1167 = A;
          int nAndCarry_1168 = 8 | F & 1;
          int value3_1171 = nAndCarry_1168;
          _F515 = value3_1171 & 1;
          value3_1171 = value3_1171 >>> 1;
          _F515 = (_F515 & 1) | 0x10 | (address_1167 & 0x28);
          if ((A & (0x01 << value3_1171)) == 0) {
              _F515 |= 0x44;
          }
          if (value3_1171 == 7 && (A & 0x80) != 0) {
              _F515 |= 0x80;
          }
          F = (_F515 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_13(int opcode) {
    switch (opcode) {
      case 0x68: {
          int _F517;
          int address_1173;
          address_1173 = B;
          int nAndCarry_1174 = 10 | F & 1;
          int value3_1177 = nAndCarry_1174;
          _F517 = value3_1177 & 1;
          value3_1177 = value3_1177 >>> 1;
          _F517 = (_F517 & 1) | 0x10 | (address_1173 & 0x28);
          if ((B & (0x01 << value3_1177)) == 0) {
              _F517 |= 0x44;
          }
          if (value3_1177 == 7 && (B & 0x80) != 0) {
              _F517 |= 0x80;
          }
          F = (_F517 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x69: {
          int _F519;
          int address_1179;
          address_1179 = C;
          int nAndCarry_1180 = 10 | F & 1;
          int value3_1183 = nAndCarry_1180;
          _F519 = value3_1183 & 1;
          value3_1183 = value3_1183 >>> 1;
          _F519 = (_F519 & 1) | 0x10 | (address_1179 & 0x28);
          if ((C & (0x01 << value3_1183)) == 0) {
              _F519 |= 0x44;
          }
          if (value3_1183 == 7 && (C & 0x80) != 0) {
              _F519 |= 0x80;
          }
          F = (_F519 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6A: {
          int _F521;
          int address_1185;
          address_1185 = D;
          int nAndCarry_1186 = 10 | F & 1;
          int value3_1189 = nAndCarry_1186;
          _F521 = value3_1189 & 1;
          value3_1189 = value3_1189 >>> 1;
          _F521 = (_F521 & 1) | 0x10 | (address_1185 & 0x28);
          if ((D & (0x01 << value3_1189)) == 0) {
              _F521 |= 0x44;
          }
          if (value3_1189 == 7 && (D & 0x80) != 0) {
              _F521 |= 0x80;
          }
          F = (_F521 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6B: {
          int _F523;
          int address_1191;
          address_1191 = E;
          int nAndCarry_1192 = 10 | F & 1;
          int value3_1195 = nAndCarry_1192;
          _F523 = value3_1195 & 1;
          value3_1195 = value3_1195 >>> 1;
          _F523 = (_F523 & 1) | 0x10 | (address_1191 & 0x28);
          if ((E & (0x01 << value3_1195)) == 0) {
              _F523 |= 0x44;
          }
          if (value3_1195 == 7 && (E & 0x80) != 0) {
              _F523 |= 0x80;
          }
          F = (_F523 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6C: {
          int _F525;
          int address_1197;
          address_1197 = H;
          int nAndCarry_1198 = 10 | F & 1;
          int value3_1201 = nAndCarry_1198;
          _F525 = value3_1201 & 1;
          value3_1201 = value3_1201 >>> 1;
          _F525 = (_F525 & 1) | 0x10 | (address_1197 & 0x28);
          if ((H & (0x01 << value3_1201)) == 0) {
              _F525 |= 0x44;
          }
          if (value3_1201 == 7 && (H & 0x80) != 0) {
              _F525 |= 0x80;
          }
          F = (_F525 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6D: {
          int _F527;
          int address_1203;
          address_1203 = L;
          int nAndCarry_1204 = 10 | F & 1;
          int value3_1207 = nAndCarry_1204;
          _F527 = value3_1207 & 1;
          value3_1207 = value3_1207 >>> 1;
          _F527 = (_F527 & 1) | 0x10 | (address_1203 & 0x28);
          if ((L & (0x01 << value3_1207)) == 0) {
              _F527 |= 0x44;
          }
          if (value3_1207 == 7 && (L & 0x80) != 0) {
              _F527 |= 0x80;
          }
          F = (_F527 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _F529;
          int _address84;
          int address_1209;
          address_1209 = MEMPTR >>> 8;
          int nAndCarry_1210 = 10 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1212 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1213 = nAndCarry_1210;
          _F529 = value3_1213 & 1;
          value3_1213 = value3_1213 >>> 1;
          _F529 = (_F529 & 1) | 0x10 | (address_1209 & 0x28);
          if ((value2_1212 & (0x01 << value3_1213)) == 0) {
              _F529 |= 0x44;
          }
          if (value3_1213 == 7 && (value2_1212 & 0x80) != 0) {
              _F529 |= 0x80;
          }
          F = (_F529 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6F: {
          int _F531;
          int address_1215;
          address_1215 = A;
          int nAndCarry_1216 = 10 | F & 1;
          int value3_1219 = nAndCarry_1216;
          _F531 = value3_1219 & 1;
          value3_1219 = value3_1219 >>> 1;
          _F531 = (_F531 & 1) | 0x10 | (address_1215 & 0x28);
          if ((A & (0x01 << value3_1219)) == 0) {
              _F531 |= 0x44;
          }
          if (value3_1219 == 7 && (A & 0x80) != 0) {
              _F531 |= 0x80;
          }
          F = (_F531 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_14(int opcode) {
    switch (opcode) {
      case 0x70: {
          int _F533;
          int address_1221;
          address_1221 = B;
          int nAndCarry_1222 = 12 | F & 1;
          int value3_1225 = nAndCarry_1222;
          _F533 = value3_1225 & 1;
          value3_1225 = value3_1225 >>> 1;
          _F533 = (_F533 & 1) | 0x10 | (address_1221 & 0x28);
          if ((B & (0x01 << value3_1225)) == 0) {
              _F533 |= 0x44;
          }
          if (value3_1225 == 7 && (B & 0x80) != 0) {
              _F533 |= 0x80;
          }
          F = (_F533 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _F535;
          int address_1227;
          address_1227 = C;
          int nAndCarry_1228 = 12 | F & 1;
          int value3_1231 = nAndCarry_1228;
          _F535 = value3_1231 & 1;
          value3_1231 = value3_1231 >>> 1;
          _F535 = (_F535 & 1) | 0x10 | (address_1227 & 0x28);
          if ((C & (0x01 << value3_1231)) == 0) {
              _F535 |= 0x44;
          }
          if (value3_1231 == 7 && (C & 0x80) != 0) {
              _F535 |= 0x80;
          }
          F = (_F535 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _F537;
          int address_1233;
          address_1233 = D;
          int nAndCarry_1234 = 12 | F & 1;
          int value3_1237 = nAndCarry_1234;
          _F537 = value3_1237 & 1;
          value3_1237 = value3_1237 >>> 1;
          _F537 = (_F537 & 1) | 0x10 | (address_1233 & 0x28);
          if ((D & (0x01 << value3_1237)) == 0) {
              _F537 |= 0x44;
          }
          if (value3_1237 == 7 && (D & 0x80) != 0) {
              _F537 |= 0x80;
          }
          F = (_F537 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _F539;
          int address_1239;
          address_1239 = E;
          int nAndCarry_1240 = 12 | F & 1;
          int value3_1243 = nAndCarry_1240;
          _F539 = value3_1243 & 1;
          value3_1243 = value3_1243 >>> 1;
          _F539 = (_F539 & 1) | 0x10 | (address_1239 & 0x28);
          if ((E & (0x01 << value3_1243)) == 0) {
              _F539 |= 0x44;
          }
          if (value3_1243 == 7 && (E & 0x80) != 0) {
              _F539 |= 0x80;
          }
          F = (_F539 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _F541;
          int address_1245;
          address_1245 = H;
          int nAndCarry_1246 = 12 | F & 1;
          int value3_1249 = nAndCarry_1246;
          _F541 = value3_1249 & 1;
          value3_1249 = value3_1249 >>> 1;
          _F541 = (_F541 & 1) | 0x10 | (address_1245 & 0x28);
          if ((H & (0x01 << value3_1249)) == 0) {
              _F541 |= 0x44;
          }
          if (value3_1249 == 7 && (H & 0x80) != 0) {
              _F541 |= 0x80;
          }
          F = (_F541 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _F543;
          int address_1251;
          address_1251 = L;
          int nAndCarry_1252 = 12 | F & 1;
          int value3_1255 = nAndCarry_1252;
          _F543 = value3_1255 & 1;
          value3_1255 = value3_1255 >>> 1;
          _F543 = (_F543 & 1) | 0x10 | (address_1251 & 0x28);
          if ((L & (0x01 << value3_1255)) == 0) {
              _F543 |= 0x44;
          }
          if (value3_1255 == 7 && (L & 0x80) != 0) {
              _F543 |= 0x80;
          }
          F = (_F543 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x76: {
          int _F545;
          int _address84;
          int address_1257;
          address_1257 = MEMPTR >>> 8;
          int nAndCarry_1258 = 12 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1260 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1261 = nAndCarry_1258;
          _F545 = value3_1261 & 1;
          value3_1261 = value3_1261 >>> 1;
          _F545 = (_F545 & 1) | 0x10 | (address_1257 & 0x28);
          if ((value2_1260 & (0x01 << value3_1261)) == 0) {
              _F545 |= 0x44;
          }
          if (value3_1261 == 7 && (value2_1260 & 0x80) != 0) {
              _F545 |= 0x80;
          }
          F = (_F545 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x77: {
          int _F547;
          int address_1263;
          address_1263 = A;
          int nAndCarry_1264 = 12 | F & 1;
          int value3_1267 = nAndCarry_1264;
          _F547 = value3_1267 & 1;
          value3_1267 = value3_1267 >>> 1;
          _F547 = (_F547 & 1) | 0x10 | (address_1263 & 0x28);
          if ((A & (0x01 << value3_1267)) == 0) {
              _F547 |= 0x44;
          }
          if (value3_1267 == 7 && (A & 0x80) != 0) {
              _F547 |= 0x80;
          }
          F = (_F547 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_15(int opcode) {
    switch (opcode) {
      case 0x78: {
          int _F549;
          int address_1269;
          address_1269 = B;
          int nAndCarry_1270 = 14 | F & 1;
          int value3_1273 = nAndCarry_1270;
          _F549 = value3_1273 & 1;
          value3_1273 = value3_1273 >>> 1;
          _F549 = (_F549 & 1) | 0x10 | (address_1269 & 0x28);
          if ((B & (0x01 << value3_1273)) == 0) {
              _F549 |= 0x44;
          }
          if (value3_1273 == 7 && (B & 0x80) != 0) {
              _F549 |= 0x80;
          }
          F = (_F549 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x79: {
          int _F551;
          int address_1275;
          address_1275 = C;
          int nAndCarry_1276 = 14 | F & 1;
          int value3_1279 = nAndCarry_1276;
          _F551 = value3_1279 & 1;
          value3_1279 = value3_1279 >>> 1;
          _F551 = (_F551 & 1) | 0x10 | (address_1275 & 0x28);
          if ((C & (0x01 << value3_1279)) == 0) {
              _F551 |= 0x44;
          }
          if (value3_1279 == 7 && (C & 0x80) != 0) {
              _F551 |= 0x80;
          }
          F = (_F551 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7A: {
          int _F553;
          int address_1281;
          address_1281 = D;
          int nAndCarry_1282 = 14 | F & 1;
          int value3_1285 = nAndCarry_1282;
          _F553 = value3_1285 & 1;
          value3_1285 = value3_1285 >>> 1;
          _F553 = (_F553 & 1) | 0x10 | (address_1281 & 0x28);
          if ((D & (0x01 << value3_1285)) == 0) {
              _F553 |= 0x44;
          }
          if (value3_1285 == 7 && (D & 0x80) != 0) {
              _F553 |= 0x80;
          }
          F = (_F553 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7B: {
          int _F555;
          int address_1287;
          address_1287 = E;
          int nAndCarry_1288 = 14 | F & 1;
          int value3_1291 = nAndCarry_1288;
          _F555 = value3_1291 & 1;
          value3_1291 = value3_1291 >>> 1;
          _F555 = (_F555 & 1) | 0x10 | (address_1287 & 0x28);
          if ((E & (0x01 << value3_1291)) == 0) {
              _F555 |= 0x44;
          }
          if (value3_1291 == 7 && (E & 0x80) != 0) {
              _F555 |= 0x80;
          }
          F = (_F555 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7C: {
          int _F557;
          int address_1293;
          address_1293 = H;
          int nAndCarry_1294 = 14 | F & 1;
          int value3_1297 = nAndCarry_1294;
          _F557 = value3_1297 & 1;
          value3_1297 = value3_1297 >>> 1;
          _F557 = (_F557 & 1) | 0x10 | (address_1293 & 0x28);
          if ((H & (0x01 << value3_1297)) == 0) {
              _F557 |= 0x44;
          }
          if (value3_1297 == 7 && (H & 0x80) != 0) {
              _F557 |= 0x80;
          }
          F = (_F557 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7D: {
          int _F559;
          int address_1299;
          address_1299 = L;
          int nAndCarry_1300 = 14 | F & 1;
          int value3_1303 = nAndCarry_1300;
          _F559 = value3_1303 & 1;
          value3_1303 = value3_1303 >>> 1;
          _F559 = (_F559 & 1) | 0x10 | (address_1299 & 0x28);
          if ((L & (0x01 << value3_1303)) == 0) {
              _F559 |= 0x44;
          }
          if (value3_1303 == 7 && (L & 0x80) != 0) {
              _F559 |= 0x80;
          }
          F = (_F559 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _F561;
          int _address84;
          int address_1305;
          address_1305 = MEMPTR >>> 8;
          int nAndCarry_1306 = 14 | F & 1;
          _address84 = (H << 8) | L;
          int value2_1308 = read(_address84, 0);
          contend1x1(((H << 8) | L));
          int value3_1309 = nAndCarry_1306;
          _F561 = value3_1309 & 1;
          value3_1309 = value3_1309 >>> 1;
          _F561 = (_F561 & 1) | 0x10 | (address_1305 & 0x28);
          if ((value2_1308 & (0x01 << value3_1309)) == 0) {
              _F561 |= 0x44;
          }
          if (value3_1309 == 7 && (value2_1308 & 0x80) != 0) {
              _F561 |= 0x80;
          }
          F = (_F561 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7F: {
          int _F563;
          int address_1311;
          address_1311 = A;
          int nAndCarry_1312 = 14 | F & 1;
          int value3_1315 = nAndCarry_1312;
          _F563 = value3_1315 & 1;
          value3_1315 = value3_1315 >>> 1;
          _F563 = (_F563 & 1) | 0x10 | (address_1311 & 0x28);
          if ((A & (0x01 << value3_1315)) == 0) {
              _F563 |= 0x44;
          }
          if (value3_1315 == 7 && (A & 0x80) != 0) {
              _F563 |= 0x80;
          }
          F = (_F563 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_16(int opcode) {
    switch (opcode) {
      case 0x80: {
          B = (B & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          C = (C & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          D = (D & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          E = (E & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          H = (H & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          L = (L & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _address84 = (H << 8) | L;
          int value_1317 = (read(_address84, 0) & -2);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1317);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x87: {
          A = (A & -2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_17(int opcode) {
    switch (opcode) {
      case 0x88: {
          B = (B & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          C = (C & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          D = (D & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          E = (E & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          H = (H & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          L = (L & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _address84 = (H << 8) | L;
          int value_1318 = (read(_address84, 0) & -3);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1318);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8F: {
          A = (A & -3);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_18(int opcode) {
    switch (opcode) {
      case 0x90: {
          B = (B & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          C = (C & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          D = (D & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          E = (E & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          H = (H & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          L = (L & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _address84 = (H << 8) | L;
          int value_1319 = (read(_address84, 0) & -5);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1319);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x97: {
          A = (A & -5);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_19(int opcode) {
    switch (opcode) {
      case 0x98: {
          B = (B & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          C = (C & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          D = (D & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          E = (E & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          H = (H & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          L = (L & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _address84 = (H << 8) | L;
          int value_1320 = (read(_address84, 0) & -9);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1320);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9F: {
          A = (A & -9);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_20(int opcode) {
    switch (opcode) {
      case 0xA0: {
          B = (B & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          C = (C & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          D = (D & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          E = (E & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          H = (H & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          L = (L & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _address84 = (H << 8) | L;
          int value_1321 = (read(_address84, 0) & -17);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1321);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA7: {
          A = (A & -17);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_21(int opcode) {
    switch (opcode) {
      case 0xA8: {
          B = (B & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          C = (C & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          D = (D & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          E = (E & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          H = (H & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          L = (L & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _address84 = (H << 8) | L;
          int value_1322 = (read(_address84, 0) & -33);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1322);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAF: {
          A = (A & -33);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_22(int opcode) {
    switch (opcode) {
      case 0xB0: {
          B = (B & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB1: {
          C = (C & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB2: {
          D = (D & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB3: {
          E = (E & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB4: {
          H = (H & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          L = (L & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _address84 = (H << 8) | L;
          int value_1323 = (read(_address84, 0) & -65);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1323);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB7: {
          A = (A & -65);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_23(int opcode) {
    switch (opcode) {
      case 0xB8: {
          B = (B & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB9: {
          C = (C & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBA: {
          D = (D & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBB: {
          E = (E & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBC: {
          H = (H & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          L = (L & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _address84 = (H << 8) | L;
          int value_1324 = (read(_address84, 0) & -129);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1324);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBF: {
          A = (A & -129);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_24(int opcode) {
    switch (opcode) {
      case 0xC0: {
          B = (B | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC1: {
          C = (C | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          D = (D | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC3: {
          E = (E | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC4: {
          H = (H | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC5: {
          L = (L | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _address84 = (H << 8) | L;
          int value_1325 = (read(_address84, 0) | 1);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1325);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC7: {
          A = (A | 1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_25(int opcode) {
    switch (opcode) {
      case 0xC8: {
          B = (B | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC9: {
          C = (C | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCA: {
          D = (D | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCB: {
          E = (E | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCC: {
          H = (H | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCD: {
          L = (L | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCE: {
          int _address84 = (H << 8) | L;
          int value_1326 = (read(_address84, 0) | 2);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1326);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCF: {
          A = (A | 2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_26(int opcode) {
    switch (opcode) {
      case 0xD0: {
          B = (B | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD1: {
          C = (C | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          D = (D | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD3: {
          E = (E | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD4: {
          H = (H | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD5: {
          L = (L | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _address84 = (H << 8) | L;
          int value_1327 = (read(_address84, 0) | 4);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1327);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD7: {
          A = (A | 4);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_27(int opcode) {
    switch (opcode) {
      case 0xD8: {
          B = (B | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD9: {
          C = (C | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          D = (D | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDB: {
          E = (E | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDC: {
          H = (H | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDD: {
          L = (L | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _address84 = (H << 8) | L;
          int value_1328 = (read(_address84, 0) | 8);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1328);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDF: {
          A = (A | 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_28(int opcode) {
    switch (opcode) {
      case 0xE0: {
          B = (B | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE1: {
          C = (C | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          D = (D | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE3: {
          E = (E | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          H = (H | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE5: {
          L = (L | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _address84 = (H << 8) | L;
          int value_1329 = (read(_address84, 0) | 0x10);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1329);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE7: {
          A = (A | 0x10);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_29(int opcode) {
    switch (opcode) {
      case 0xE8: {
          B = (B | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE9: {
          C = (C | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEA: {
          D = (D | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEB: {
          E = (E | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          H = (H | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xED: {
          L = (L | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _address84 = (H << 8) | L;
          int value_1330 = (read(_address84, 0) | 0x20);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1330);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEF: {
          A = (A | 0x20);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_30(int opcode) {
    switch (opcode) {
      case 0xF0: {
          B = (B | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF1: {
          C = (C | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          D = (D | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF3: {
          E = (E | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF4: {
          H = (H | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF5: {
          L = (L | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _address84 = (H << 8) | L;
          int value_1331 = (read(_address84, 0) | 0x40);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1331);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF7: {
          A = (A | 0x40);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeCB_31(int opcode) {
    switch (opcode) {
      case 0xF8: {
          B = (B | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF9: {
          C = (C | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFA: {
          D = (D | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFB: {
          E = (E | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFC: {
          H = (H | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFD: {
          L = (L | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _address84 = (H << 8) | L;
          int value_1332 = (read(_address84, 0) | 0x80);
          contend1x1(((H << 8) | L));
          _address84 = (H << 8) | L;
          write(_address84, value_1332);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFF: {
          A = (A | 0x80);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeCB");
    }
  }

  private void decodeDD(int opcode) {
    switch (opcode >> 3) {
      case 0: decodeDD_0(opcode);
        break;
      case 1: decodeDD_1(opcode);
        break;
      case 2: decodeDD_2(opcode);
        break;
      case 3: decodeDD_3(opcode);
        break;
      case 4: decodeDD_4(opcode);
        break;
      case 5: decodeDD_5(opcode);
        break;
      case 6: decodeDD_6(opcode);
        break;
      case 7: decodeDD_7(opcode);
        break;
      case 8: decodeDD_8(opcode);
        break;
      case 9: decodeDD_9(opcode);
        break;
      case 10: decodeDD_10(opcode);
        break;
      case 11: decodeDD_11(opcode);
        break;
      case 12: decodeDD_12(opcode);
        break;
      case 13: decodeDD_13(opcode);
        break;
      case 14: decodeDD_14(opcode);
        break;
      case 15: decodeDD_15(opcode);
        break;
      case 16: decodeDD_16(opcode);
        break;
      case 17: decodeDD_17(opcode);
        break;
      case 18: decodeDD_18(opcode);
        break;
      case 19: decodeDD_19(opcode);
        break;
      case 20: decodeDD_20(opcode);
        break;
      case 21: decodeDD_21(opcode);
        break;
      case 22: decodeDD_22(opcode);
        break;
      case 23: decodeDD_23(opcode);
        break;
      case 24: decodeDD_24(opcode);
        break;
      case 25: decodeDD_25(opcode);
        break;
      case 26: decodeDD_26(opcode);
        break;
      case 27: decodeDD_27(opcode);
        break;
      case 28: decodeDD_28(opcode);
        break;
      case 29: decodeDD_29(opcode);
        break;
      case 30: decodeDD_30(opcode);
        break;
      case 31: decodeDD_31(opcode);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_0(int opcode) {
    switch (opcode) {
      case 0x00: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x01: {
          int address_1438 = (PC + 2) & 0xFFFF;
          int operand_1440 = read(address_1438, 0);
          int operand_1442 = read((address_1438 + 1) & 0xFFFF, 0);
          int value_1443 = ((operand_1442 << 8) | operand_1440);
          B = (value_1443 >>> 8);
          C = value_1443 & 0xFF;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _address714 = (B << 8) | C;
          write(_address714, A);
          MEMPTR = ((A << 8) | ((_address714 + 1) & 0xff));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x03: {
          int read_1444 = ((B << 8) | C);
          int value_1445 = (read_1444 + 1) & 0xFFFF;
          B = (value_1445 >>> 8);
          C = value_1445 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F717;
          int value1_1446 = B;
          int value2_1447 = F;
          _F717 = value2_1447;
          value1_1446++;
          value1_1446 &= 0xff;
          _F717 = (_F717 & 1) | (value1_1446 == 0x80 ? 4 : 0) | ((value1_1446 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1446 & 0xff] | (value1_1446 == 0 ? 0x40 : 0));
          int result_1449 = value1_1446 & 0xFF;
          F = (_F717 & 0xFF);
          B = result_1449;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F719;
          int value1_1450 = B;
          int value2_1451 = F;
          _F719 = value2_1451;
          _F719 = (_F719 & 1) | ((value1_1450 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1450--;
          value1_1450 &= 0xff;
          _F719 |= (value1_1450 == 0x7f ? 4 : 0) | (SZ53[value1_1450 & 0xff] | (value1_1450 == 0 ? 0x40 : 0));
          int result_1453 = value1_1450 & 0xFF;
          F = (_F719 & 0xFF);
          B = result_1453;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          int operand_1454 = read((PC + 2) & 0xFFFF, 0);
          B = operand_1454;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F722;
          int value1_1455 = A;
          int value2_1456 = F;
          _F722 = value2_1456;
          value1_1455 = (value1_1455 << 1) | (value1_1455 >> 7);
          _F722 = (_F722 & 0xC4) | (value1_1455 & 0x29);
          int result_1458 = value1_1455 & 0xFF;
          F = _F722;
          A = result_1458;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_1(int opcode) {
    switch (opcode) {
      case 0x08: {
          int v1_1459 = ((A << 8) | F);
          int v2_1460 = _AF;
          A = (v2_1460 >>> 8);
          F = v2_1460 & 0xFF;
          _AF = v1_1459;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F725;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int b_1461 = ((B << 8) | C);
          int result_1462 = (IX + b_1461);
          int value1_1463 = ((IX & 0x0800) >> 4 | result_1462 >> 11);
          int value2_1464 = F;
          int value3_1465 = (b_1461 >> 11) & 1;
          _F725 = value2_1464;
          int add16temp_1466 = value1_1463 << 11;
          int lookup_1467 = (((value1_1463 << 4) & 0x0800) >> 11) | ((value3_1465 << 11) >> 10) | ((add16temp_1466 & 0x0800) >> 9);
          _F725 = (_F725 & 0xC4) | ((add16temp_1466 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1466 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1467];
          F = (_F725 & 0xFF);
          IX = (result_1462 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _address727 = (B << 8) | C;
          int value_1469 = read(_address727, 0);
          A = value_1469;
          MEMPTR = ((_address727 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int value_1470 = (((B << 8) | C) - 1) & 0xFFFF;
          B = (value_1470 >>> 8);
          C = value_1470 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F730;
          int value1_1471 = C;
          int value2_1472 = F;
          _F730 = value2_1472;
          value1_1471++;
          value1_1471 &= 0xff;
          _F730 = (_F730 & 1) | (value1_1471 == 0x80 ? 4 : 0) | ((value1_1471 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1471 & 0xff] | (value1_1471 == 0 ? 0x40 : 0));
          int result_1474 = value1_1471 & 0xFF;
          F = (_F730 & 0xFF);
          C = result_1474;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F732;
          int value1_1475 = C;
          int value2_1476 = F;
          _F732 = value2_1476;
          _F732 = (_F732 & 1) | ((value1_1475 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1475--;
          value1_1475 &= 0xff;
          _F732 |= (value1_1475 == 0x7f ? 4 : 0) | (SZ53[value1_1475 & 0xff] | (value1_1475 == 0 ? 0x40 : 0));
          int result_1478 = value1_1475 & 0xFF;
          F = (_F732 & 0xFF);
          C = result_1478;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int operand_1479 = read((PC + 2) & 0xFFFF, 0);
          C = operand_1479;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F735;
          int value1_1480 = A;
          int value2_1481 = F;
          _F735 = value2_1481;
          _F735 = (_F735 & 0xC4) | (value1_1480 & 1);
          value1_1480 = (value1_1480 >> 1) | (value1_1480 << 7);
          _F735 |= (value1_1480 & 0x28);
          int result_1483 = (value1_1480 & 0xff);
          F = (_F735 & 0xFF);
          A = result_1483;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_2(int opcode) {
    switch (opcode) {
      case 0x10: {
          contend1x1(((I << 8) | R));
          B = (B - 1) & 0xFF;
          if ((B != 0)) {
              int operand_1485 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1484 = ((PC + 3 + (byte) operand_1485) & 0xFFFF);
              MEMPTR = jumpAddress2_1484;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1484;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x11: {
          int address_1487 = (PC + 2) & 0xFFFF;
          int operand_1489 = read(address_1487, 0);
          int operand_1491 = read((address_1487 + 1) & 0xFFFF, 0);
          int value_1492 = ((operand_1491 << 8) | operand_1489);
          D = (value_1492 >>> 8);
          E = value_1492 & 0xFF;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _address739 = (D << 8) | E;
          write(_address739, A);
          MEMPTR = ((A << 8) | ((_address739 + 1) & 0xff));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x13: {
          int read_1493 = ((D << 8) | E);
          int value_1494 = (read_1493 + 1) & 0xFFFF;
          D = (value_1494 >>> 8);
          E = value_1494 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F742;
          int value1_1495 = D;
          int value2_1496 = F;
          _F742 = value2_1496;
          value1_1495++;
          value1_1495 &= 0xff;
          _F742 = (_F742 & 1) | (value1_1495 == 0x80 ? 4 : 0) | ((value1_1495 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1495 & 0xff] | (value1_1495 == 0 ? 0x40 : 0));
          int result_1498 = value1_1495 & 0xFF;
          F = (_F742 & 0xFF);
          D = result_1498;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F744;
          int value1_1499 = D;
          int value2_1500 = F;
          _F744 = value2_1500;
          _F744 = (_F744 & 1) | ((value1_1499 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1499--;
          value1_1499 &= 0xff;
          _F744 |= (value1_1499 == 0x7f ? 4 : 0) | (SZ53[value1_1499 & 0xff] | (value1_1499 == 0 ? 0x40 : 0));
          int result_1502 = value1_1499 & 0xFF;
          F = (_F744 & 0xFF);
          D = result_1502;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          int operand_1503 = read((PC + 2) & 0xFFFF, 0);
          D = operand_1503;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F747;
          int value1_1504 = A;
          int value2_1505 = F;
          _F747 = value2_1505;
          int bytetemp_1507 = value1_1504;
          value1_1504 = (value1_1504 << 1) | (_F747 & 1);
          _F747 = (_F747 & 0xC4) | (value1_1504 & 0x28) | (bytetemp_1507 >> 7);
          int result_1508 = value1_1504 & 0xFF;
          F = (_F747 & 0xFF);
          A = result_1508;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_3(int opcode) {
    switch (opcode) {
      case 0x18: {
          int _nextPC749;
          int operand_1510 = read((PC + 2) & 0xFFFF, 0);
          int jumpAddress2_1509 = ((PC + 3 + (byte) operand_1510) & 0xFFFF);
          _nextPC749 = jumpAddress2_1509;
          MEMPTR = _nextPC749;
          contend5x1((PC + 1) & 0xFFFF);
          PC = _nextPC749;
          break;
      }
      case 0x19: {
          int _F750;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int b_1512 = ((D << 8) | E);
          int result_1513 = (IX + b_1512);
          int value1_1514 = ((IX & 0x0800) >> 4 | result_1513 >> 11);
          int value2_1515 = F;
          int value3_1516 = (b_1512 >> 11) & 1;
          _F750 = value2_1515;
          int add16temp_1517 = value1_1514 << 11;
          int lookup_1518 = (((value1_1514 << 4) & 0x0800) >> 11) | ((value3_1516 << 11) >> 10) | ((add16temp_1517 & 0x0800) >> 9);
          _F750 = (_F750 & 0xC4) | ((add16temp_1517 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1517 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1518];
          F = (_F750 & 0xFF);
          IX = (result_1513 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _address752 = (D << 8) | E;
          int value_1520 = read(_address752, 0);
          A = value_1520;
          MEMPTR = ((_address752 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int value_1521 = (((D << 8) | E) - 1) & 0xFFFF;
          D = (value_1521 >>> 8);
          E = value_1521 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F755;
          int value1_1522 = E;
          int value2_1523 = F;
          _F755 = value2_1523;
          value1_1522++;
          value1_1522 &= 0xff;
          _F755 = (_F755 & 1) | (value1_1522 == 0x80 ? 4 : 0) | ((value1_1522 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1522 & 0xff] | (value1_1522 == 0 ? 0x40 : 0));
          int result_1525 = value1_1522 & 0xFF;
          F = (_F755 & 0xFF);
          E = result_1525;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F757;
          int value1_1526 = E;
          int value2_1527 = F;
          _F757 = value2_1527;
          _F757 = (_F757 & 1) | ((value1_1526 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1526--;
          value1_1526 &= 0xff;
          _F757 |= (value1_1526 == 0x7f ? 4 : 0) | (SZ53[value1_1526 & 0xff] | (value1_1526 == 0 ? 0x40 : 0));
          int result_1529 = value1_1526 & 0xFF;
          F = (_F757 & 0xFF);
          E = result_1529;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int operand_1530 = read((PC + 2) & 0xFFFF, 0);
          E = operand_1530;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F760;
          int value1_1531 = A;
          int value2_1532 = F;
          _F760 = value2_1532;
          int A_1534 = value1_1531;
          int bytetemp_1535 = A_1534;
          A_1534 = (A_1534 >> 1) | (_F760 << 7);
          _F760 = (_F760 & 0xC4) | (A_1534 & 0x28) | (bytetemp_1535 & 1);
          int result_1536 = A_1534 & 0xFF;
          F = _F760;
          A = result_1536;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_4(int opcode) {
    switch (opcode) {
      case 0x20: {
          if ((!((F & 0x40) == 0x40))) {
              int operand_1538 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1537 = ((PC + 3 + (byte) operand_1538) & 0xFFFF);
              MEMPTR = jumpAddress2_1537;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1537;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x21: {
          int address_1540 = (PC + 2) & 0xFFFF;
          int operand_1542 = read(address_1540, 0);
          int operand_1544 = read((address_1540 + 1) & 0xFFFF, 0);
          IX = ((operand_1544 << 8) | operand_1542);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _address764;
          int address_1545 = (PC + 2) & 0xFFFF;
          int operand_1547 = read(address_1545, 0);
          int operand_1549 = read((address_1545 + 1) & 0xFFFF, 0);
          _address764 = (operand_1549 << 8) | operand_1547;
          write(_address764, (IX & 0xFF));
          write((_address764 + 1) & 0xFFFF, (IX >>> 8));
          MEMPTR = ((_address764 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int read_1550 = IX;
          IX = ((read_1550 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F767;
          int value1_1551 = (IX >> 8);
          int value2_1552 = F;
          _F767 = value2_1552;
          value1_1551++;
          value1_1551 &= 0xff;
          _F767 = (_F767 & 1) | (value1_1551 == 0x80 ? 4 : 0) | ((value1_1551 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1551 & 0xff] | (value1_1551 == 0 ? 0x40 : 0));
          int result_1554 = value1_1551 & 0xFF;
          F = (_F767 & 0xFF);
          IX = (IX & 0x00FF) | (result_1554 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F769;
          int value1_1555 = (IX >> 8);
          int value2_1556 = F;
          _F769 = value2_1556;
          _F769 = (_F769 & 1) | ((value1_1555 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1555--;
          value1_1555 &= 0xff;
          _F769 |= (value1_1555 == 0x7f ? 4 : 0) | (SZ53[value1_1555 & 0xff] | (value1_1555 == 0 ? 0x40 : 0));
          int result_1558 = value1_1555 & 0xFF;
          F = (_F769 & 0xFF);
          IX = (IX & 0x00FF) | (result_1558 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          int operand_1559 = read((PC + 2) & 0xFFFF, 0);
          IX = (IX & 0x00FF) | (operand_1559 << 8);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F775 = 0;
          int _F774 = 0;
          int _data773 = 0;
          int _F772;
          int value1_1560 = A;
          int value2_1561 = F;
          _F772 = value2_1561;
          value1_1560 &= 0xff;
          int add_1563 = 0;
          int carry_1564 = (_F772 & 1);
          if (((_F772 & 0x10) != 0) || ((value1_1560 & 0x0f) > 9)) {
              add_1563 = 6;
          }
          if (carry_1564 != 0 || (value1_1560 > 0x99)) {
              add_1563 |= 0x60;
          }
          if (value1_1560 > 0x99) {
              carry_1564 = 1;
          }
          int and_1566 = _F772 & 0xff;
          _data773 = and_1566;
          if ((_F772 & 2) != 0) {
              int value1_1567 = value1_1560;
              int subtemp_1570 = value1_1567 - add_1563;
              int lookup_1571 = ((value1_1567 & 0x88) >> 3) | ((add_1563 & 0x88) >> 2) | ((subtemp_1570 & 0x88) >> 1);
              value1_1567 = subtemp_1570 & 0xff;
              _F774 = ((subtemp_1570 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1571 & 0x07)] | OVERFLOW_SUB[(lookup_1571 >> 4)] | (SZ53[value1_1567 & 0xff] | (value1_1567 == 0 ? 0x40 : 0));
              int result_1572 = value1_1567 & 0xFF;
              int and_1573 = (_F774 & 0xFF);
              _data773 = and_1573;
              value1_1560 = result_1572;
          } else {
              int value2_1575 = value1_1560;
              int addtemp_1577 = value2_1575 + add_1563;
              int lookup_1578 = ((value2_1575 & 0x88) >> 3) | ((add_1563 & 0x88) >> 2) | ((addtemp_1577 & 0x88) >> 1);
              value2_1575 = addtemp_1577 & 0xff;
              _F775 = ((addtemp_1577 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1578 & 0x07)] | OVERFLOW_ADD[(lookup_1578 >> 4)] | (SZ53[value2_1575 & 0xff] | (value2_1575 == 0 ? 0x40 : 0));
              int result_1579 = value2_1575 & 0xFF;
              int and_1580 = (_F775 & 0xFF);
              _data773 = and_1580;
              value1_1560 = result_1579;
          }
          _F772 = _data773;
          _F772 = (_F772 & -6) | carry_1564 | PARITY[value1_1560 & 0xff];
          int result_1581 = value1_1560 & 0xFF;
          F = (_F772 & 0xFF);
          A = result_1581;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_5(int opcode) {
    switch (opcode) {
      case 0x28: {
          if (((F & 0x40) == 0x40)) {
              int operand_1583 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1582 = ((PC + 3 + (byte) operand_1583) & 0xFFFF);
              MEMPTR = jumpAddress2_1582;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1582;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x29: {
          int _F778;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int result_1585 = (IX + IX);
          int value1_1586 = ((IX & 0x0800) >> 4 | result_1585 >> 11);
          int value2_1587 = F;
          int value3_1588 = (IX >> 11) & 1;
          _F778 = value2_1587;
          int add16temp_1589 = value1_1586 << 11;
          int lookup_1590 = (((value1_1586 << 4) & 0x0800) >> 11) | ((value3_1588 << 11) >> 10) | ((add16temp_1589 & 0x0800) >> 9);
          _F778 = (_F778 & 0xC4) | ((add16temp_1589 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1589 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1590];
          F = (_F778 & 0xFF);
          IX = (result_1585 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _address780;
          int address_1592 = (PC + 2) & 0xFFFF;
          int operand_1594 = read(address_1592, 0);
          int operand_1596 = read((address_1592 + 1) & 0xFFFF, 0);
          _address780 = (operand_1596 << 8) | operand_1594;
          int wordNumber1_1597 = read(_address780, 0);
          int wordNumber_1598 = read((_address780 + 1) & 0xFFFF, 0);
          IX = ((wordNumber_1598 << 8) | wordNumber1_1597);
          MEMPTR = ((_address780 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2B: {
          IX = ((IX - 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F783;
          int value1_1599 = (IX & 0xFF);
          int value2_1600 = F;
          _F783 = value2_1600;
          value1_1599++;
          value1_1599 &= 0xff;
          _F783 = (_F783 & 1) | (value1_1599 == 0x80 ? 4 : 0) | ((value1_1599 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1599 & 0xff] | (value1_1599 == 0 ? 0x40 : 0));
          int result_1602 = value1_1599 & 0xFF;
          F = (_F783 & 0xFF);
          IX = (IX & 0xFF00) | result_1602;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F785;
          int value1_1603 = (IX & 0xFF);
          int value2_1604 = F;
          _F785 = value2_1604;
          _F785 = (_F785 & 1) | ((value1_1603 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1603--;
          value1_1603 &= 0xff;
          _F785 |= (value1_1603 == 0x7f ? 4 : 0) | (SZ53[value1_1603 & 0xff] | (value1_1603 == 0 ? 0x40 : 0));
          int result_1606 = value1_1603 & 0xFF;
          F = (_F785 & 0xFF);
          IX = (IX & 0xFF00) | result_1606;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int operand_1607 = read((PC + 2) & 0xFFFF, 0);
          IX = (IX & 0xFF00) | operand_1607;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F788;
          int value1_1608 = A;
          int value2_1609 = F;
          _F788 = value2_1609;
          value1_1608 ^= 0xff;
          _F788 = (_F788 & 0xC5) | (value1_1608 & 0x28) | 0x12;
          int result_1611 = value1_1608 & 0xFF;
          F = _F788;
          A = result_1611;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_6(int opcode) {
    switch (opcode) {
      case 0x30: {
          if ((!((F & 1) == 1))) {
              int operand_1613 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1612 = ((PC + 3 + (byte) operand_1613) & 0xFFFF);
              MEMPTR = jumpAddress2_1612;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1612;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x31: {
          int address_1615 = (PC + 2) & 0xFFFF;
          int operand_1617 = read(address_1615, 0);
          int operand_1619 = read((address_1615 + 1) & 0xFFFF, 0);
          SP = ((operand_1619 << 8) | operand_1617);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _address792;
          int address_1620 = (PC + 2) & 0xFFFF;
          int operand_1622 = read(address_1620, 0);
          int operand_1624 = read((address_1620 + 1) & 0xFFFF, 0);
          _address792 = (operand_1624 << 8) | operand_1622;
          write(_address792, A);
          MEMPTR = ((A << 8) | ((_address792 + 1) & 0xff));
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int read_1625 = SP;
          SP = ((read_1625 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F796;
          int _value795;
          int _address795;
          int operand_1626 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1626)) & 0xFFFF;
          int operand_1627 = read(_address795, 0);
          contend1x1(_address795);
          _value795 = operand_1627;
          int value1_1628 = _value795;
          int value2_1629 = F;
          _F796 = value2_1629;
          value1_1628++;
          value1_1628 &= 0xff;
          _F796 = (_F796 & 1) | (value1_1628 == 0x80 ? 4 : 0) | ((value1_1628 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1628 & 0xff] | (value1_1628 == 0 ? 0x40 : 0));
          int result_1631 = value1_1628 & 0xFF;
          F = (_F796 & 0xFF);
          _address795 = (IX + (int) ((byte) operand_1626)) & 0xFFFF;
          _value795 = result_1631;
          write(_address795, result_1631);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F798;
          int _value795;
          int _address795;
          int operand_1632 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1632)) & 0xFFFF;
          int operand_1633 = read(_address795, 0);
          contend1x1(_address795);
          _value795 = operand_1633;
          int value1_1634 = _value795;
          int value2_1635 = F;
          _F798 = value2_1635;
          _F798 = (_F798 & 1) | ((value1_1634 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1634--;
          value1_1634 &= 0xff;
          _F798 |= (value1_1634 == 0x7f ? 4 : 0) | (SZ53[value1_1634 & 0xff] | (value1_1634 == 0 ? 0x40 : 0));
          int result_1637 = value1_1634 & 0xFF;
          F = (_F798 & 0xFF);
          _address795 = (IX + (int) ((byte) operand_1632)) & 0xFFFF;
          _value795 = result_1637;
          write(_address795, result_1637);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _address800;
          int operand_1638 = read((PC + 2) & 0xFFFF, 0);
          int operand_1639 = read((PC + 3) & 0xFFFF, 0);
          _address800 = (IX + (int) ((byte) operand_1638)) & 0xFFFF;
          contend2x1((PC + 3) & 0xFFFF);
          write(_address800, operand_1639);
          MEMPTR = _address800;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F802;
          int value2_1641 = F;
          _F802 = value2_1641;
          _F802 = _F802 & 0xC4 | A & 0x28 | 1;
          F = _F802;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_7(int opcode) {
    switch (opcode) {
      case 0x38: {
          if (((F & 1) == 1)) {
              int operand_1645 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1644 = ((PC + 3 + (byte) operand_1645) & 0xFFFF);
              MEMPTR = jumpAddress2_1644;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1644;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x39: {
          int _F805;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int result_1647 = (IX + SP);
          int value1_1648 = ((IX & 0x0800) >> 4 | result_1647 >> 11);
          int value2_1649 = F;
          int value3_1650 = (SP >> 11) & 1;
          _F805 = value2_1649;
          int add16temp_1651 = value1_1648 << 11;
          int lookup_1652 = (((value1_1648 << 4) & 0x0800) >> 11) | ((value3_1650 << 11) >> 10) | ((add16temp_1651 & 0x0800) >> 9);
          _F805 = (_F805 & 0xC4) | ((add16temp_1651 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1651 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1652];
          F = (_F805 & 0xFF);
          IX = (result_1647 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _address807;
          int address_1654 = (PC + 2) & 0xFFFF;
          int operand_1656 = read(address_1654, 0);
          int operand_1658 = read((address_1654 + 1) & 0xFFFF, 0);
          _address807 = (operand_1658 << 8) | operand_1656;
          int value_1659 = read(_address807, 0);
          A = value_1659;
          MEMPTR = ((_address807 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3B: {
          SP = ((SP - 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F810;
          int value1_1660 = A;
          int value2_1661 = F;
          _F810 = value2_1661;
          value1_1660++;
          value1_1660 &= 0xff;
          _F810 = (_F810 & 1) | (value1_1660 == 0x80 ? 4 : 0) | ((value1_1660 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1660 & 0xff] | (value1_1660 == 0 ? 0x40 : 0));
          int result_1663 = value1_1660 & 0xFF;
          F = (_F810 & 0xFF);
          A = result_1663;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F812;
          int value1_1664 = A;
          int value2_1665 = F;
          _F812 = value2_1665;
          _F812 = (_F812 & 1) | ((value1_1664 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1664--;
          value1_1664 &= 0xff;
          _F812 |= (value1_1664 == 0x7f ? 4 : 0) | (SZ53[value1_1664 & 0xff] | (value1_1664 == 0 ? 0x40 : 0));
          int result_1667 = value1_1664 & 0xFF;
          F = (_F812 & 0xFF);
          A = result_1667;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int operand_1668 = read((PC + 2) & 0xFFFF, 0);
          A = operand_1668;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F815;
          int value2_1670 = F;
          _F815 = value2_1670;
          _F815 = _F815 & 0xC4 | ((_F815 & 1) != 0 ? 0x10 : 1) | A & 0x28;
          F = _F815;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_8(int opcode) {
    switch (opcode) {
      case 0x40: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x41: {
          B = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x42: {
          B = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x43: {
          B = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x44: {
          B = (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x45: {
          B = (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _value795;
          int _address795;
          int operand_1673 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1673)) & 0xFFFF;
          int operand_1674 = read(_address795, 0);
          _value795 = operand_1674;
          B = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x47: {
          B = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_9(int opcode) {
    switch (opcode) {
      case 0x48: {
          C = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x49: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4A: {
          C = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4B: {
          C = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4C: {
          C = (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4D: {
          C = (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _value795;
          int _address795;
          int operand_1675 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1675)) & 0xFFFF;
          int operand_1676 = read(_address795, 0);
          _value795 = operand_1676;
          C = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x4F: {
          C = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_10(int opcode) {
    switch (opcode) {
      case 0x50: {
          D = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x51: {
          D = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x52: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x53: {
          D = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x54: {
          D = (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x55: {
          D = (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _value795;
          int _address795;
          int operand_1677 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1677)) & 0xFFFF;
          int operand_1678 = read(_address795, 0);
          _value795 = operand_1678;
          D = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x57: {
          D = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_11(int opcode) {
    switch (opcode) {
      case 0x58: {
          E = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x59: {
          E = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5A: {
          E = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5C: {
          E = (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5D: {
          E = (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _value795;
          int _address795;
          int operand_1679 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1679)) & 0xFFFF;
          int operand_1680 = read(_address795, 0);
          _value795 = operand_1680;
          E = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x5F: {
          E = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_12(int opcode) {
    switch (opcode) {
      case 0x60: {
          IX = (IX & 0x00FF) | (B << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x61: {
          IX = (IX & 0x00FF) | (C << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x62: {
          IX = (IX & 0x00FF) | (D << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x63: {
          IX = (IX & 0x00FF) | (E << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x64: {
          IX = (IX & 0x00FF) | ((IX >> 8) << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x65: {
          IX = (IX & 0x00FF) | ((IX & 0xFF) << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _value795;
          int _address795;
          int operand_1681 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1681)) & 0xFFFF;
          int operand_1682 = read(_address795, 0);
          _value795 = operand_1682;
          H = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x67: {
          IX = (IX & 0x00FF) | (A << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_13(int opcode) {
    switch (opcode) {
      case 0x68: {
          IX = (IX & 0xFF00) | B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x69: {
          IX = (IX & 0xFF00) | C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6A: {
          IX = (IX & 0xFF00) | D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6B: {
          IX = (IX & 0xFF00) | E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6C: {
          IX = (IX & 0xFF00) | (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6D: {
          IX = (IX & 0xFF00) | (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _value795;
          int _address795;
          int operand_1683 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1683)) & 0xFFFF;
          int operand_1684 = read(_address795, 0);
          _value795 = operand_1684;
          L = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x6F: {
          IX = (IX & 0xFF00) | A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_14(int opcode) {
    switch (opcode) {
      case 0x70: {
          int _address795;
          int operand_1685 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1685)) & 0xFFFF;
          write(_address795, B);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _address795;
          int operand_1686 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1686)) & 0xFFFF;
          write(_address795, C);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _address795;
          int operand_1687 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1687)) & 0xFFFF;
          write(_address795, D);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address795;
          int operand_1688 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1688)) & 0xFFFF;
          write(_address795, E);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _address795;
          int operand_1689 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1689)) & 0xFFFF;
          write(_address795, H);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _address795;
          int operand_1690 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1690)) & 0xFFFF;
          write(_address795, L);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x76: {
          if (!state.isHalted()) {
              state.setHalted(true);
              _nextPC871 = PC;
          }
          PC = _nextPC871 == -1 ? (PC + 2) & 0xFFFF : _nextPC871;
          break;
      }
      case 0x77: {
          int _address795;
          int operand_1691 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1691)) & 0xFFFF;
          write(_address795, A);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_15(int opcode) {
    switch (opcode) {
      case 0x78: {
          A = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x79: {
          A = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7A: {
          A = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7B: {
          A = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7C: {
          A = (IX >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7D: {
          A = (IX & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _value795;
          int _address795;
          int operand_1692 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1692)) & 0xFFFF;
          int operand_1693 = read(_address795, 0);
          _value795 = operand_1693;
          A = _value795;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x7F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_16(int opcode) {
    switch (opcode) {
      case 0x80: {
          int _F881;
          int value1_1694 = A;
          int value2_1695 = B;
          int addtemp_1697 = value2_1695 + value1_1694;
          int lookup_1698 = ((value2_1695 & 0x88) >> 3) | ((value1_1694 & 0x88) >> 2) | ((addtemp_1697 & 0x88) >> 1);
          value2_1695 = addtemp_1697 & 0xff;
          _F881 = ((addtemp_1697 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1698 & 0x07)] | OVERFLOW_ADD[(lookup_1698 >> 4)] | (SZ53[value2_1695] | (value2_1695 == 0 ? 0x40 : 0));
          F = (_F881 & 0xFF);
          A = value2_1695;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _F883;
          int value1_1700 = A;
          int value2_1701 = C;
          int addtemp_1703 = value2_1701 + value1_1700;
          int lookup_1704 = ((value2_1701 & 0x88) >> 3) | ((value1_1700 & 0x88) >> 2) | ((addtemp_1703 & 0x88) >> 1);
          value2_1701 = addtemp_1703 & 0xff;
          _F883 = ((addtemp_1703 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1704 & 0x07)] | OVERFLOW_ADD[(lookup_1704 >> 4)] | (SZ53[value2_1701] | (value2_1701 == 0 ? 0x40 : 0));
          F = (_F883 & 0xFF);
          A = value2_1701;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _F885;
          int value1_1706 = A;
          int value2_1707 = D;
          int addtemp_1709 = value2_1707 + value1_1706;
          int lookup_1710 = ((value2_1707 & 0x88) >> 3) | ((value1_1706 & 0x88) >> 2) | ((addtemp_1709 & 0x88) >> 1);
          value2_1707 = addtemp_1709 & 0xff;
          _F885 = ((addtemp_1709 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1710 & 0x07)] | OVERFLOW_ADD[(lookup_1710 >> 4)] | (SZ53[value2_1707] | (value2_1707 == 0 ? 0x40 : 0));
          F = (_F885 & 0xFF);
          A = value2_1707;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _F887;
          int value1_1712 = A;
          int value2_1713 = E;
          int addtemp_1715 = value2_1713 + value1_1712;
          int lookup_1716 = ((value2_1713 & 0x88) >> 3) | ((value1_1712 & 0x88) >> 2) | ((addtemp_1715 & 0x88) >> 1);
          value2_1713 = addtemp_1715 & 0xff;
          _F887 = ((addtemp_1715 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1716 & 0x07)] | OVERFLOW_ADD[(lookup_1716 >> 4)] | (SZ53[value2_1713] | (value2_1713 == 0 ? 0x40 : 0));
          F = (_F887 & 0xFF);
          A = value2_1713;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _F889;
          int value1_1718 = A;
          int value2_1719 = (IX >> 8);
          int addtemp_1721 = value2_1719 + value1_1718;
          int lookup_1722 = ((value2_1719 & 0x88) >> 3) | ((value1_1718 & 0x88) >> 2) | ((addtemp_1721 & 0x88) >> 1);
          value2_1719 = addtemp_1721 & 0xff;
          _F889 = ((addtemp_1721 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1722 & 0x07)] | OVERFLOW_ADD[(lookup_1722 >> 4)] | (SZ53[value2_1719] | (value2_1719 == 0 ? 0x40 : 0));
          F = (_F889 & 0xFF);
          A = value2_1719;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _F891;
          int value1_1724 = A;
          int value2_1725 = (IX & 0xFF);
          int addtemp_1727 = value2_1725 + value1_1724;
          int lookup_1728 = ((value2_1725 & 0x88) >> 3) | ((value1_1724 & 0x88) >> 2) | ((addtemp_1727 & 0x88) >> 1);
          value2_1725 = addtemp_1727 & 0xff;
          _F891 = ((addtemp_1727 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1728 & 0x07)] | OVERFLOW_ADD[(lookup_1728 >> 4)] | (SZ53[value2_1725] | (value2_1725 == 0 ? 0x40 : 0));
          F = (_F891 & 0xFF);
          A = value2_1725;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _F893;
          int _value795;
          int _address795;
          int operand_1730 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1730)) & 0xFFFF;
          int operand_1731 = read(_address795, 0);
          _value795 = operand_1731;
          int value1_1732 = A;
          int value2_1733 = _value795;
          int addtemp_1735 = value2_1733 + value1_1732;
          int lookup_1736 = ((value2_1733 & 0x88) >> 3) | ((value1_1732 & 0x88) >> 2) | ((addtemp_1735 & 0x88) >> 1);
          value2_1733 = addtemp_1735 & 0xff;
          _F893 = ((addtemp_1735 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1736 & 0x07)] | OVERFLOW_ADD[(lookup_1736 >> 4)] | (SZ53[value2_1733] | (value2_1733 == 0 ? 0x40 : 0));
          F = (_F893 & 0xFF);
          A = value2_1733;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _F895;
          int value1_1738 = A;
          int value2_1739 = A;
          int addtemp_1741 = value2_1739 + value1_1738;
          int lookup_1742 = ((value2_1739 & 0x88) >> 3) | ((value1_1738 & 0x88) >> 2) | ((addtemp_1741 & 0x88) >> 1);
          value2_1739 = addtemp_1741 & 0xff;
          _F895 = ((addtemp_1741 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1742 & 0x07)] | OVERFLOW_ADD[(lookup_1742 >> 4)] | (SZ53[value2_1739] | (value2_1739 == 0 ? 0x40 : 0));
          F = (_F895 & 0xFF);
          A = value2_1739;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_17(int opcode) {
    switch (opcode) {
      case 0x88: {
          int _F897;
          int value1_1744 = A;
          int value3_1746 = F & 1;
          _F897 = value3_1746;
          int adctemp_1747 = value1_1744 + B + (_F897 & 1);
          int lookup_1748 = ((value1_1744 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((adctemp_1747 & 0x88) >> 1);
          value1_1744 = adctemp_1747 & 0xff;
          _F897 = ((adctemp_1747 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1748 & 0x07)] | OVERFLOW_ADD[(lookup_1748 >> 4)] | (SZ53[value1_1744] | (value1_1744 == 0 ? 0x40 : 0));
          F = (_F897 & 0xFF);
          A = value1_1744;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _F899;
          int value1_1750 = A;
          int value3_1752 = F & 1;
          _F899 = value3_1752;
          int adctemp_1753 = value1_1750 + C + (_F899 & 1);
          int lookup_1754 = ((value1_1750 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((adctemp_1753 & 0x88) >> 1);
          value1_1750 = adctemp_1753 & 0xff;
          _F899 = ((adctemp_1753 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1754 & 0x07)] | OVERFLOW_ADD[(lookup_1754 >> 4)] | (SZ53[value1_1750] | (value1_1750 == 0 ? 0x40 : 0));
          F = (_F899 & 0xFF);
          A = value1_1750;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _F901;
          int value1_1756 = A;
          int value3_1758 = F & 1;
          _F901 = value3_1758;
          int adctemp_1759 = value1_1756 + D + (_F901 & 1);
          int lookup_1760 = ((value1_1756 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((adctemp_1759 & 0x88) >> 1);
          value1_1756 = adctemp_1759 & 0xff;
          _F901 = ((adctemp_1759 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1760 & 0x07)] | OVERFLOW_ADD[(lookup_1760 >> 4)] | (SZ53[value1_1756] | (value1_1756 == 0 ? 0x40 : 0));
          F = (_F901 & 0xFF);
          A = value1_1756;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _F903;
          int value1_1762 = A;
          int value3_1764 = F & 1;
          _F903 = value3_1764;
          int adctemp_1765 = value1_1762 + E + (_F903 & 1);
          int lookup_1766 = ((value1_1762 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((adctemp_1765 & 0x88) >> 1);
          value1_1762 = adctemp_1765 & 0xff;
          _F903 = ((adctemp_1765 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1766 & 0x07)] | OVERFLOW_ADD[(lookup_1766 >> 4)] | (SZ53[value1_1762] | (value1_1762 == 0 ? 0x40 : 0));
          F = (_F903 & 0xFF);
          A = value1_1762;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _F905;
          int value1_1768 = A;
          int value2_1769 = (IX >> 8);
          int value3_1770 = F & 1;
          _F905 = value3_1770;
          int adctemp_1771 = value1_1768 + value2_1769 + (_F905 & 1);
          int lookup_1772 = ((value1_1768 & 0x88) >> 3) | ((value2_1769 & 0x88) >> 2) | ((adctemp_1771 & 0x88) >> 1);
          value1_1768 = adctemp_1771 & 0xff;
          _F905 = ((adctemp_1771 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1772 & 0x07)] | OVERFLOW_ADD[(lookup_1772 >> 4)] | (SZ53[value1_1768] | (value1_1768 == 0 ? 0x40 : 0));
          F = (_F905 & 0xFF);
          A = value1_1768;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _F907;
          int value1_1774 = A;
          int value2_1775 = (IX & 0xFF);
          int value3_1776 = F & 1;
          _F907 = value3_1776;
          int adctemp_1777 = value1_1774 + value2_1775 + (_F907 & 1);
          int lookup_1778 = ((value1_1774 & 0x88) >> 3) | ((value2_1775 & 0x88) >> 2) | ((adctemp_1777 & 0x88) >> 1);
          value1_1774 = adctemp_1777 & 0xff;
          _F907 = ((adctemp_1777 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1778 & 0x07)] | OVERFLOW_ADD[(lookup_1778 >> 4)] | (SZ53[value1_1774] | (value1_1774 == 0 ? 0x40 : 0));
          F = (_F907 & 0xFF);
          A = value1_1774;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _F909;
          int _value795;
          int _address795;
          int operand_1780 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1780)) & 0xFFFF;
          int operand_1781 = read(_address795, 0);
          _value795 = operand_1781;
          int value1_1782 = A;
          int value3_1784 = F & 1;
          _F909 = value3_1784;
          int adctemp_1785 = value1_1782 + _value795 + (_F909 & 1);
          int lookup_1786 = ((value1_1782 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((adctemp_1785 & 0x88) >> 1);
          value1_1782 = adctemp_1785 & 0xff;
          _F909 = ((adctemp_1785 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1786 & 0x07)] | OVERFLOW_ADD[(lookup_1786 >> 4)] | (SZ53[value1_1782] | (value1_1782 == 0 ? 0x40 : 0));
          F = (_F909 & 0xFF);
          A = value1_1782;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _F911;
          int value1_1788 = A;
          int value2_1789 = A;
          int value3_1790 = F & 1;
          _F911 = value3_1790;
          int adctemp_1791 = value1_1788 + value2_1789 + (_F911 & 1);
          int lookup_1792 = ((value1_1788 & 0x88) >> 3) | ((value2_1789 & 0x88) >> 2) | ((adctemp_1791 & 0x88) >> 1);
          value1_1788 = adctemp_1791 & 0xff;
          _F911 = ((adctemp_1791 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1792 & 0x07)] | OVERFLOW_ADD[(lookup_1792 >> 4)] | (SZ53[value1_1788] | (value1_1788 == 0 ? 0x40 : 0));
          F = (_F911 & 0xFF);
          A = value1_1788;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_18(int opcode) {
    switch (opcode) {
      case 0x90: {
          int _F913;
          int value1_1794 = A;
          int subtemp_1797 = value1_1794 - B;
          int lookup_1798 = ((value1_1794 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((subtemp_1797 & 0x88) >> 1);
          value1_1794 = subtemp_1797 & 0xff;
          _F913 = ((subtemp_1797 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1798 & 0x07)] | OVERFLOW_SUB[(lookup_1798 >> 4)] | (SZ53[value1_1794] | (value1_1794 == 0 ? 0x40 : 0));
          F = (_F913 & 0xFF);
          A = value1_1794;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _F915;
          int value1_1800 = A;
          int subtemp_1803 = value1_1800 - C;
          int lookup_1804 = ((value1_1800 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((subtemp_1803 & 0x88) >> 1);
          value1_1800 = subtemp_1803 & 0xff;
          _F915 = ((subtemp_1803 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1804 & 0x07)] | OVERFLOW_SUB[(lookup_1804 >> 4)] | (SZ53[value1_1800] | (value1_1800 == 0 ? 0x40 : 0));
          F = (_F915 & 0xFF);
          A = value1_1800;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _F917;
          int value1_1806 = A;
          int subtemp_1809 = value1_1806 - D;
          int lookup_1810 = ((value1_1806 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((subtemp_1809 & 0x88) >> 1);
          value1_1806 = subtemp_1809 & 0xff;
          _F917 = ((subtemp_1809 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1810 & 0x07)] | OVERFLOW_SUB[(lookup_1810 >> 4)] | (SZ53[value1_1806] | (value1_1806 == 0 ? 0x40 : 0));
          F = (_F917 & 0xFF);
          A = value1_1806;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _F919;
          int value1_1812 = A;
          int subtemp_1815 = value1_1812 - E;
          int lookup_1816 = ((value1_1812 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((subtemp_1815 & 0x88) >> 1);
          value1_1812 = subtemp_1815 & 0xff;
          _F919 = ((subtemp_1815 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1816 & 0x07)] | OVERFLOW_SUB[(lookup_1816 >> 4)] | (SZ53[value1_1812] | (value1_1812 == 0 ? 0x40 : 0));
          F = (_F919 & 0xFF);
          A = value1_1812;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _F921;
          int value1_1818 = A;
          int value2_1819 = (IX >> 8);
          int subtemp_1821 = value1_1818 - value2_1819;
          int lookup_1822 = ((value1_1818 & 0x88) >> 3) | ((value2_1819 & 0x88) >> 2) | ((subtemp_1821 & 0x88) >> 1);
          value1_1818 = subtemp_1821 & 0xff;
          _F921 = ((subtemp_1821 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1822 & 0x07)] | OVERFLOW_SUB[(lookup_1822 >> 4)] | (SZ53[value1_1818] | (value1_1818 == 0 ? 0x40 : 0));
          F = (_F921 & 0xFF);
          A = value1_1818;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _F923;
          int value1_1824 = A;
          int value2_1825 = (IX & 0xFF);
          int subtemp_1827 = value1_1824 - value2_1825;
          int lookup_1828 = ((value1_1824 & 0x88) >> 3) | ((value2_1825 & 0x88) >> 2) | ((subtemp_1827 & 0x88) >> 1);
          value1_1824 = subtemp_1827 & 0xff;
          _F923 = ((subtemp_1827 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1828 & 0x07)] | OVERFLOW_SUB[(lookup_1828 >> 4)] | (SZ53[value1_1824] | (value1_1824 == 0 ? 0x40 : 0));
          F = (_F923 & 0xFF);
          A = value1_1824;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _F925;
          int _value795;
          int _address795;
          int operand_1830 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1830)) & 0xFFFF;
          int operand_1831 = read(_address795, 0);
          _value795 = operand_1831;
          int value1_1832 = A;
          int subtemp_1835 = value1_1832 - _value795;
          int lookup_1836 = ((value1_1832 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((subtemp_1835 & 0x88) >> 1);
          value1_1832 = subtemp_1835 & 0xff;
          _F925 = ((subtemp_1835 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1836 & 0x07)] | OVERFLOW_SUB[(lookup_1836 >> 4)] | (SZ53[value1_1832] | (value1_1832 == 0 ? 0x40 : 0));
          F = (_F925 & 0xFF);
          A = value1_1832;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _F927;
          int value1_1838 = A;
          int value2_1839 = A;
          int subtemp_1841 = value1_1838 - value2_1839;
          int lookup_1842 = ((value1_1838 & 0x88) >> 3) | ((value2_1839 & 0x88) >> 2) | ((subtemp_1841 & 0x88) >> 1);
          value1_1838 = subtemp_1841 & 0xff;
          _F927 = ((subtemp_1841 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1842 & 0x07)] | OVERFLOW_SUB[(lookup_1842 >> 4)] | (SZ53[value1_1838] | (value1_1838 == 0 ? 0x40 : 0));
          F = (_F927 & 0xFF);
          A = value1_1838;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_19(int opcode) {
    switch (opcode) {
      case 0x98: {
          int _F929;
          int value1_1844 = A;
          int value3_1846 = F & 1;
          _F929 = value3_1846;
          int sbctemp_1847 = value1_1844 - B - (_F929 & 1);
          int lookup_1848 = ((value1_1844 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((sbctemp_1847 & 0x88) >> 1);
          value1_1844 = sbctemp_1847 & 0xff;
          _F929 = ((sbctemp_1847 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1848 & 0x07)] | OVERFLOW_SUB[(lookup_1848 >> 4)] | (SZ53[value1_1844] | (value1_1844 == 0 ? 0x40 : 0));
          F = (_F929 & 0xFF);
          A = value1_1844;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _F931;
          int value1_1850 = A;
          int value3_1852 = F & 1;
          _F931 = value3_1852;
          int sbctemp_1853 = value1_1850 - C - (_F931 & 1);
          int lookup_1854 = ((value1_1850 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((sbctemp_1853 & 0x88) >> 1);
          value1_1850 = sbctemp_1853 & 0xff;
          _F931 = ((sbctemp_1853 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1854 & 0x07)] | OVERFLOW_SUB[(lookup_1854 >> 4)] | (SZ53[value1_1850] | (value1_1850 == 0 ? 0x40 : 0));
          F = (_F931 & 0xFF);
          A = value1_1850;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _F933;
          int value1_1856 = A;
          int value3_1858 = F & 1;
          _F933 = value3_1858;
          int sbctemp_1859 = value1_1856 - D - (_F933 & 1);
          int lookup_1860 = ((value1_1856 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((sbctemp_1859 & 0x88) >> 1);
          value1_1856 = sbctemp_1859 & 0xff;
          _F933 = ((sbctemp_1859 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1860 & 0x07)] | OVERFLOW_SUB[(lookup_1860 >> 4)] | (SZ53[value1_1856] | (value1_1856 == 0 ? 0x40 : 0));
          F = (_F933 & 0xFF);
          A = value1_1856;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _F935;
          int value1_1862 = A;
          int value3_1864 = F & 1;
          _F935 = value3_1864;
          int sbctemp_1865 = value1_1862 - E - (_F935 & 1);
          int lookup_1866 = ((value1_1862 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((sbctemp_1865 & 0x88) >> 1);
          value1_1862 = sbctemp_1865 & 0xff;
          _F935 = ((sbctemp_1865 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1866 & 0x07)] | OVERFLOW_SUB[(lookup_1866 >> 4)] | (SZ53[value1_1862] | (value1_1862 == 0 ? 0x40 : 0));
          F = (_F935 & 0xFF);
          A = value1_1862;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _F937;
          int value1_1868 = A;
          int value2_1869 = (IX >> 8);
          int value3_1870 = F & 1;
          _F937 = value3_1870;
          int sbctemp_1871 = value1_1868 - value2_1869 - (_F937 & 1);
          int lookup_1872 = ((value1_1868 & 0x88) >> 3) | ((value2_1869 & 0x88) >> 2) | ((sbctemp_1871 & 0x88) >> 1);
          value1_1868 = sbctemp_1871 & 0xff;
          _F937 = ((sbctemp_1871 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1872 & 0x07)] | OVERFLOW_SUB[(lookup_1872 >> 4)] | (SZ53[value1_1868] | (value1_1868 == 0 ? 0x40 : 0));
          F = (_F937 & 0xFF);
          A = value1_1868;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _F939;
          int value1_1874 = A;
          int value2_1875 = (IX & 0xFF);
          int value3_1876 = F & 1;
          _F939 = value3_1876;
          int sbctemp_1877 = value1_1874 - value2_1875 - (_F939 & 1);
          int lookup_1878 = ((value1_1874 & 0x88) >> 3) | ((value2_1875 & 0x88) >> 2) | ((sbctemp_1877 & 0x88) >> 1);
          value1_1874 = sbctemp_1877 & 0xff;
          _F939 = ((sbctemp_1877 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1878 & 0x07)] | OVERFLOW_SUB[(lookup_1878 >> 4)] | (SZ53[value1_1874] | (value1_1874 == 0 ? 0x40 : 0));
          F = (_F939 & 0xFF);
          A = value1_1874;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _F941;
          int _value795;
          int _address795;
          int operand_1880 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1880)) & 0xFFFF;
          int operand_1881 = read(_address795, 0);
          _value795 = operand_1881;
          int value1_1882 = A;
          int value3_1884 = F & 1;
          _F941 = value3_1884;
          int sbctemp_1885 = value1_1882 - _value795 - (_F941 & 1);
          int lookup_1886 = ((value1_1882 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((sbctemp_1885 & 0x88) >> 1);
          value1_1882 = sbctemp_1885 & 0xff;
          _F941 = ((sbctemp_1885 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1886 & 0x07)] | OVERFLOW_SUB[(lookup_1886 >> 4)] | (SZ53[value1_1882] | (value1_1882 == 0 ? 0x40 : 0));
          F = (_F941 & 0xFF);
          A = value1_1882;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _F943;
          int value1_1888 = A;
          int value2_1889 = A;
          int value3_1890 = F & 1;
          _F943 = value3_1890;
          int sbctemp_1891 = value1_1888 - value2_1889 - (_F943 & 1);
          int lookup_1892 = ((value1_1888 & 0x88) >> 3) | ((value2_1889 & 0x88) >> 2) | ((sbctemp_1891 & 0x88) >> 1);
          value1_1888 = sbctemp_1891 & 0xff;
          _F943 = ((sbctemp_1891 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1892 & 0x07)] | OVERFLOW_SUB[(lookup_1892 >> 4)] | (SZ53[value1_1888] | (value1_1888 == 0 ? 0x40 : 0));
          F = (_F943 & 0xFF);
          A = value1_1888;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_20(int opcode) {
    switch (opcode) {
      case 0xA0: {
          int _F945;
          int value1_1894 = A;
          int value2_1895 = B;
          value2_1895 &= value1_1894;
          _F945 = 0x10 | (SZ53P[value2_1895 & 0xff] | (value2_1895 == 0 ? 0x40 : 0));
          int result_1897 = value2_1895 & 0xFF;
          F = (_F945 & 0xFF);
          A = result_1897;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F947;
          int value1_1898 = A;
          int value2_1899 = C;
          value2_1899 &= value1_1898;
          _F947 = 0x10 | (SZ53P[value2_1899 & 0xff] | (value2_1899 == 0 ? 0x40 : 0));
          int result_1901 = value2_1899 & 0xFF;
          F = (_F947 & 0xFF);
          A = result_1901;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F949;
          int value1_1902 = A;
          int value2_1903 = D;
          value2_1903 &= value1_1902;
          _F949 = 0x10 | (SZ53P[value2_1903 & 0xff] | (value2_1903 == 0 ? 0x40 : 0));
          int result_1905 = value2_1903 & 0xFF;
          F = (_F949 & 0xFF);
          A = result_1905;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F951;
          int value1_1906 = A;
          int value2_1907 = E;
          value2_1907 &= value1_1906;
          _F951 = 0x10 | (SZ53P[value2_1907 & 0xff] | (value2_1907 == 0 ? 0x40 : 0));
          int result_1909 = value2_1907 & 0xFF;
          F = (_F951 & 0xFF);
          A = result_1909;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _F953;
          int value1_1910 = A;
          int value2_1911 = (IX >> 8);
          value2_1911 &= value1_1910;
          _F953 = 0x10 | (SZ53P[value2_1911 & 0xff] | (value2_1911 == 0 ? 0x40 : 0));
          int result_1913 = value2_1911 & 0xFF;
          F = (_F953 & 0xFF);
          A = result_1913;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _F955;
          int value1_1914 = A;
          int value2_1915 = (IX & 0xFF);
          value2_1915 &= value1_1914;
          _F955 = 0x10 | (SZ53P[value2_1915 & 0xff] | (value2_1915 == 0 ? 0x40 : 0));
          int result_1917 = value2_1915 & 0xFF;
          F = (_F955 & 0xFF);
          A = result_1917;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _F957;
          int _value795;
          int _address795;
          int operand_1918 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1918)) & 0xFFFF;
          int operand_1919 = read(_address795, 0);
          _value795 = operand_1919;
          int value1_1920 = A;
          int value2_1921 = _value795;
          value2_1921 &= value1_1920;
          _F957 = 0x10 | (SZ53P[value2_1921 & 0xff] | (value2_1921 == 0 ? 0x40 : 0));
          int result_1923 = value2_1921 & 0xFF;
          F = (_F957 & 0xFF);
          A = result_1923;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _F959;
          int value1_1924 = A;
          int value2_1925 = A;
          value2_1925 &= value1_1924;
          _F959 = 0x10 | (SZ53P[value2_1925 & 0xff] | (value2_1925 == 0 ? 0x40 : 0));
          int result_1927 = value2_1925 & 0xFF;
          F = (_F959 & 0xFF);
          A = result_1927;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_21(int opcode) {
    switch (opcode) {
      case 0xA8: {
          int _F961;
          int value1_1928 = A;
          int value2_1929 = B;
          value2_1929 ^= value1_1928;
          _F961 = SZ53P[value2_1929 & 0xff] | (value2_1929 == 0 ? 0x40 : 0);
          int result_1931 = value2_1929 & 0xFF;
          F = (_F961 & 0xFF);
          A = result_1931;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F963;
          int value1_1932 = A;
          int value2_1933 = C;
          value2_1933 ^= value1_1932;
          _F963 = SZ53P[value2_1933 & 0xff] | (value2_1933 == 0 ? 0x40 : 0);
          int result_1935 = value2_1933 & 0xFF;
          F = (_F963 & 0xFF);
          A = result_1935;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F965;
          int value1_1936 = A;
          int value2_1937 = D;
          value2_1937 ^= value1_1936;
          _F965 = SZ53P[value2_1937 & 0xff] | (value2_1937 == 0 ? 0x40 : 0);
          int result_1939 = value2_1937 & 0xFF;
          F = (_F965 & 0xFF);
          A = result_1939;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F967;
          int value1_1940 = A;
          int value2_1941 = E;
          value2_1941 ^= value1_1940;
          _F967 = SZ53P[value2_1941 & 0xff] | (value2_1941 == 0 ? 0x40 : 0);
          int result_1943 = value2_1941 & 0xFF;
          F = (_F967 & 0xFF);
          A = result_1943;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _F969;
          int value1_1944 = A;
          int value2_1945 = (IX >> 8);
          value2_1945 ^= value1_1944;
          _F969 = SZ53P[value2_1945 & 0xff] | (value2_1945 == 0 ? 0x40 : 0);
          int result_1947 = value2_1945 & 0xFF;
          F = (_F969 & 0xFF);
          A = result_1947;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _F971;
          int value1_1948 = A;
          int value2_1949 = (IX & 0xFF);
          value2_1949 ^= value1_1948;
          _F971 = SZ53P[value2_1949 & 0xff] | (value2_1949 == 0 ? 0x40 : 0);
          int result_1951 = value2_1949 & 0xFF;
          F = (_F971 & 0xFF);
          A = result_1951;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _F973;
          int _value795;
          int _address795;
          int operand_1952 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1952)) & 0xFFFF;
          int operand_1953 = read(_address795, 0);
          _value795 = operand_1953;
          int value1_1954 = A;
          int value2_1955 = _value795;
          value2_1955 ^= value1_1954;
          _F973 = SZ53P[value2_1955 & 0xff] | (value2_1955 == 0 ? 0x40 : 0);
          int result_1957 = value2_1955 & 0xFF;
          F = (_F973 & 0xFF);
          A = result_1957;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _F975;
          int value1_1958 = A;
          int value2_1959 = A;
          value2_1959 ^= value1_1958;
          _F975 = SZ53P[value2_1959 & 0xff] | (value2_1959 == 0 ? 0x40 : 0);
          int result_1961 = value2_1959 & 0xFF;
          F = (_F975 & 0xFF);
          A = result_1961;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_22(int opcode) {
    switch (opcode) {
      case 0xB0: {
          int _F977;
          int value1_1962 = A;
          int value2_1963 = B;
          value2_1963 |= value1_1962;
          _F977 = SZ53P[value2_1963 & 0xff] | (value2_1963 == 0 ? 0x40 : 0);
          int result_1965 = value2_1963 & 0xFF;
          F = (_F977 & 0xFF);
          A = result_1965;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _F979;
          int value1_1966 = A;
          int value2_1967 = C;
          value2_1967 |= value1_1966;
          _F979 = SZ53P[value2_1967 & 0xff] | (value2_1967 == 0 ? 0x40 : 0);
          int result_1969 = value2_1967 & 0xFF;
          F = (_F979 & 0xFF);
          A = result_1969;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _F981;
          int value1_1970 = A;
          int value2_1971 = D;
          value2_1971 |= value1_1970;
          _F981 = SZ53P[value2_1971 & 0xff] | (value2_1971 == 0 ? 0x40 : 0);
          int result_1973 = value2_1971 & 0xFF;
          F = (_F981 & 0xFF);
          A = result_1973;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _F983;
          int value1_1974 = A;
          int value2_1975 = E;
          value2_1975 |= value1_1974;
          _F983 = SZ53P[value2_1975 & 0xff] | (value2_1975 == 0 ? 0x40 : 0);
          int result_1977 = value2_1975 & 0xFF;
          F = (_F983 & 0xFF);
          A = result_1977;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _F985;
          int value1_1978 = A;
          int value2_1979 = (IX >> 8);
          value2_1979 |= value1_1978;
          _F985 = SZ53P[value2_1979 & 0xff] | (value2_1979 == 0 ? 0x40 : 0);
          int result_1981 = value2_1979 & 0xFF;
          F = (_F985 & 0xFF);
          A = result_1981;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _F987;
          int value1_1982 = A;
          int value2_1983 = (IX & 0xFF);
          value2_1983 |= value1_1982;
          _F987 = SZ53P[value2_1983 & 0xff] | (value2_1983 == 0 ? 0x40 : 0);
          int result_1985 = value2_1983 & 0xFF;
          F = (_F987 & 0xFF);
          A = result_1985;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _F989;
          int _value795;
          int _address795;
          int operand_1986 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1986)) & 0xFFFF;
          int operand_1987 = read(_address795, 0);
          _value795 = operand_1987;
          int value1_1988 = A;
          int value2_1989 = _value795;
          value2_1989 |= value1_1988;
          _F989 = SZ53P[value2_1989 & 0xff] | (value2_1989 == 0 ? 0x40 : 0);
          int result_1991 = value2_1989 & 0xFF;
          F = (_F989 & 0xFF);
          A = result_1991;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _F991;
          int value1_1992 = A;
          int value2_1993 = A;
          value2_1993 |= value1_1992;
          _F991 = SZ53P[value2_1993 & 0xff] | (value2_1993 == 0 ? 0x40 : 0);
          int result_1995 = value2_1993 & 0xFF;
          F = (_F991 & 0xFF);
          A = result_1995;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_23(int opcode) {
    switch (opcode) {
      case 0xB8: {
          int _F993;
          int cptemp_1999 = A - B;
          int lookup_2000 = ((A & 0x88) >> 3) | ((B & 0x88) >> 2) | ((cptemp_1999 & 0x88) >> 1);
          _F993 = ((cptemp_1999 & 0x100) != 0 ? 1 : (cptemp_1999 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2000 & 0x07)] | OVERFLOW_SUB[(lookup_2000 >> 4)] | (B & 0x28) | (cptemp_1999 & 0x80);
          F = (_F993 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _F995;
          int cptemp_2005 = A - C;
          int lookup_2006 = ((A & 0x88) >> 3) | ((C & 0x88) >> 2) | ((cptemp_2005 & 0x88) >> 1);
          _F995 = ((cptemp_2005 & 0x100) != 0 ? 1 : (cptemp_2005 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2006 & 0x07)] | OVERFLOW_SUB[(lookup_2006 >> 4)] | (C & 0x28) | (cptemp_2005 & 0x80);
          F = (_F995 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _F997;
          int cptemp_2011 = A - D;
          int lookup_2012 = ((A & 0x88) >> 3) | ((D & 0x88) >> 2) | ((cptemp_2011 & 0x88) >> 1);
          _F997 = ((cptemp_2011 & 0x100) != 0 ? 1 : (cptemp_2011 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2012 & 0x07)] | OVERFLOW_SUB[(lookup_2012 >> 4)] | (D & 0x28) | (cptemp_2011 & 0x80);
          F = (_F997 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _F999;
          int cptemp_2017 = A - E;
          int lookup_2018 = ((A & 0x88) >> 3) | ((E & 0x88) >> 2) | ((cptemp_2017 & 0x88) >> 1);
          _F999 = ((cptemp_2017 & 0x100) != 0 ? 1 : (cptemp_2017 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2018 & 0x07)] | OVERFLOW_SUB[(lookup_2018 >> 4)] | (E & 0x28) | (cptemp_2017 & 0x80);
          F = (_F999 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _F1001;
          int value2_2021 = (IX >> 8);
          int cptemp_2023 = A - value2_2021;
          int lookup_2024 = ((A & 0x88) >> 3) | ((value2_2021 & 0x88) >> 2) | ((cptemp_2023 & 0x88) >> 1);
          _F1001 = ((cptemp_2023 & 0x100) != 0 ? 1 : (cptemp_2023 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2024 & 0x07)] | OVERFLOW_SUB[(lookup_2024 >> 4)] | (value2_2021 & 0x28) | (cptemp_2023 & 0x80);
          F = (_F1001 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _F1003;
          int value2_2027 = (IX & 0xFF);
          int cptemp_2029 = A - value2_2027;
          int lookup_2030 = ((A & 0x88) >> 3) | ((value2_2027 & 0x88) >> 2) | ((cptemp_2029 & 0x88) >> 1);
          _F1003 = ((cptemp_2029 & 0x100) != 0 ? 1 : (cptemp_2029 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2030 & 0x07)] | OVERFLOW_SUB[(lookup_2030 >> 4)] | (value2_2027 & 0x28) | (cptemp_2029 & 0x80);
          F = (_F1003 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _F1005;
          int _value795;
          int _address795;
          int operand_2032 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_2032)) & 0xFFFF;
          int operand_2033 = read(_address795, 0);
          _value795 = operand_2033;
          int cptemp_2037 = A - _value795;
          int lookup_2038 = ((A & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((cptemp_2037 & 0x88) >> 1);
          _F1005 = ((cptemp_2037 & 0x100) != 0 ? 1 : (cptemp_2037 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2038 & 0x07)] | OVERFLOW_SUB[(lookup_2038 >> 4)] | (_value795 & 0x28) | (cptemp_2037 & 0x80);
          F = (_F1005 & 0xFF);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _F1007;
          int cptemp_2043 = A - A;
          int lookup_2044 = ((A & 0x88) >> 3) | ((A & 0x88) >> 2) | ((cptemp_2043 & 0x88) >> 1);
          _F1007 = ((cptemp_2043 & 0x100) != 0 ? 1 : (cptemp_2043 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2044 & 0x07)] | OVERFLOW_SUB[(lookup_2044 >> 4)] | (A & 0x28) | (cptemp_2043 & 0x80);
          F = (_F1007 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_24(int opcode) {
    switch (opcode) {
      case 0xC0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_2046 = SP;
          if ((!((F & 0x40) == 0x40))) {
              int wordNumber1_2048 = read(SP, 0);
              int wordNumber_2049 = read((SP + 1) & 0xFFFF, 0);
              int value_2047 = ((wordNumber_2049 << 8) | wordNumber1_2048);
              int wordNumber_2050 = SP;
              SP = ((wordNumber_2050 + 2) & 0xFFFF);
              jumpAddress2_2046 = value_2047;
              MEMPTR = jumpAddress2_2046;
              PC = jumpAddress2_2046;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC1: {
          int wordNumber1_2054 = read(SP, 0);
          int wordNumber_2055 = read((SP + 1) & 0xFFFF, 0);
          int value_2053 = ((wordNumber_2055 << 8) | wordNumber1_2054);
          int wordNumber_2056 = SP;
          SP = ((wordNumber_2056 + 2) & 0xFFFF);
          B = (value_2053 >>> 8);
          C = value_2053 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _jumpAddress1011 = 0;
          int address_2058 = (PC + 2) & 0xFFFF;
          int operand_2060 = read(address_2058, 0);
          int operand_2062 = read((address_2058 + 1) & 0xFFFF, 0);
          int jumpAddress2_2057 = (_jumpAddress1011 = (operand_2062 << 8) | operand_2060);
          if ((!((F & 0x40) == 0x40))) {
              _jumpAddress1011 = jumpAddress2_2057;
              MEMPTR = _jumpAddress1011;
              PC = jumpAddress2_2057;
              break;
          } else {
              MEMPTR = _jumpAddress1011;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xC3: {
          int _nextPC1012;
          int _jumpAddress1012;
          int address_2065 = (PC + 2) & 0xFFFF;
          int operand_2067 = read(address_2065, 0);
          int operand_2069 = read((address_2065 + 1) & 0xFFFF, 0);
          int jumpAddress2_2064 = (_jumpAddress1012 = (operand_2069 << 8) | operand_2067);
          _jumpAddress1012 = jumpAddress2_2064;
          _nextPC1012 = jumpAddress2_2064;
          MEMPTR = _jumpAddress1012;
          PC = _nextPC1012;
          break;
      }
      case 0xC4: {
          int _jumpAddress1013 = 0;
          int address_2071 = (PC + 2) & 0xFFFF;
          int operand_2073 = read(address_2071, 0);
          int operand_2075 = read((address_2071 + 1) & 0xFFFF, 0);
          int value_2076 = (_jumpAddress1013 = (operand_2075 << 8) | operand_2073);
          MEMPTR = value_2076;
          int jumpAddress2_2077 = (_jumpAddress1013 = (operand_2075 << 8) | operand_2073);
          if ((!((F & 0x40) == 0x40))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_2081 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_2081 >>> 8));
              write(SP, (value_2081 & 0xFF));
              _jumpAddress1013 = jumpAddress2_2077;
              MEMPTR = _jumpAddress1013;
              PC = jumpAddress2_2077;
              break;
          } else {
              MEMPTR = _jumpAddress1013;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xC5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_2083 = ((B << 8) | C);
          write((SP + 1) & 0xFFFF, (value_2083 >>> 8));
          write(SP, (value_2083 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _F1015;
          int operand_2084 = read((PC + 2) & 0xFFFF, 0);
          int value1_2085 = A;
          int value2_2086 = operand_2084;
          int addtemp_2088 = value2_2086 + value1_2085;
          int lookup_2089 = ((value2_2086 & 0x88) >> 3) | ((value1_2085 & 0x88) >> 2) | ((addtemp_2088 & 0x88) >> 1);
          value2_2086 = addtemp_2088 & 0xff;
          _F1015 = ((addtemp_2088 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_2089 & 0x07)] | OVERFLOW_ADD[(lookup_2089 >> 4)] | (SZ53[value2_2086] | (value2_2086 == 0 ? 0x40 : 0));
          F = (_F1015 & 0xFF);
          A = value2_2086;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _nextPC1017;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_2091 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_2091 >>> 8));
          write(SP, (value_2091 & 0xFF));
          _nextPC1017 = 0;
          MEMPTR = _nextPC1017 & 0xFFFF;
          PC = _nextPC1017 == -1 ? (PC + 2) & 0xFFFF : _nextPC1017;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_25(int opcode) {
    switch (opcode) {
      case 0xC8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_2092 = SP;
          if (((F & 0x40) == 0x40)) {
              int wordNumber1_2094 = read(SP, 0);
              int wordNumber_2095 = read((SP + 1) & 0xFFFF, 0);
              int value_2093 = ((wordNumber_2095 << 8) | wordNumber1_2094);
              int wordNumber_2096 = SP;
              SP = ((wordNumber_2096 + 2) & 0xFFFF);
              jumpAddress2_2092 = value_2093;
              MEMPTR = jumpAddress2_2092;
              PC = jumpAddress2_2092;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC9: {
          int _nextPC1019;
          int jumpAddress2_2098;
          int wordNumber1_2100 = read(SP, 0);
          int wordNumber_2101 = read((SP + 1) & 0xFFFF, 0);
          int value_2099 = ((wordNumber_2101 << 8) | wordNumber1_2100);
          int wordNumber_2102 = SP;
          SP = ((wordNumber_2102 + 2) & 0xFFFF);
          jumpAddress2_2098 = value_2099;
          _nextPC1019 = jumpAddress2_2098;
          MEMPTR = _nextPC1019;
          PC = _nextPC1019;
          break;
      }
      case 0xCA: {
          int _jumpAddress1020 = 0;
          int address_2105 = (PC + 2) & 0xFFFF;
          int operand_2107 = read(address_2105, 0);
          int operand_2109 = read((address_2105 + 1) & 0xFFFF, 0);
          int jumpAddress2_2104 = (_jumpAddress1020 = (operand_2109 << 8) | operand_2107);
          if (((F & 0x40) == 0x40)) {
              _jumpAddress1020 = jumpAddress2_2104;
              MEMPTR = _jumpAddress1020;
              PC = jumpAddress2_2104;
              break;
          } else {
              MEMPTR = _jumpAddress1020;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xCB: {
          int displacement = read((PC + 2) & 0xFFFF, 0);
          decodeDDCB(read((PC + 3) & 0xFFFF, 2), displacement);
          break;
      }
      case 0xCC: {
          int _jumpAddress1661 = 0;
          int address_3319 = (PC + 2) & 0xFFFF;
          int operand_3321 = read(address_3319, 0);
          int operand_3323 = read((address_3319 + 1) & 0xFFFF, 0);
          int value_3324 = (_jumpAddress1661 = (operand_3323 << 8) | operand_3321);
          MEMPTR = value_3324;
          int jumpAddress2_3325 = (_jumpAddress1661 = (operand_3323 << 8) | operand_3321);
          if (((F & 0x40) == 0x40)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3329 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3329 >>> 8));
              write(SP, (value_3329 & 0xFF));
              _jumpAddress1661 = jumpAddress2_3325;
              MEMPTR = _jumpAddress1661;
              PC = jumpAddress2_3325;
              break;
          } else {
              MEMPTR = _jumpAddress1661;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xCD: {
          int _nextPC1662;
          int _jumpAddress1662;
          int address_3331 = (PC + 2) & 0xFFFF;
          int operand_3333 = read(address_3331, 0);
          int operand_3335 = read((address_3331 + 1) & 0xFFFF, 0);
          int value_3336 = (_jumpAddress1662 = (operand_3335 << 8) | operand_3333);
          int jumpAddress2_3337 = (_jumpAddress1662 = (operand_3335 << 8) | operand_3333);
          SP = ((SP - 2) & 0xFFFF);
          int value_3341 = ((PC + 4) & 0xFFFF);
          contend1x1((PC + 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3341 >>> 8));
          write(SP, (value_3341 & 0xFF));
          _jumpAddress1662 = jumpAddress2_3337;
          _nextPC1662 = jumpAddress2_3337;
          MEMPTR = _jumpAddress1662;
          PC = _nextPC1662;
          break;
      }
      case 0xCE: {
          int _F1663;
          int operand_3343 = read((PC + 2) & 0xFFFF, 0);
          int value1_3344 = A;
          int value3_3346 = F & 1;
          _F1663 = value3_3346;
          int adctemp_3347 = value1_3344 + operand_3343 + (_F1663 & 1);
          int lookup_3348 = ((value1_3344 & 0x88) >> 3) | ((operand_3343 & 0x88) >> 2) | ((adctemp_3347 & 0x88) >> 1);
          value1_3344 = adctemp_3347 & 0xff;
          _F1663 = ((adctemp_3347 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_3348 & 0x07)] | OVERFLOW_ADD[(lookup_3348 >> 4)] | (SZ53[value1_3344] | (value1_3344 == 0 ? 0x40 : 0));
          F = (_F1663 & 0xFF);
          A = value1_3344;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _nextPC1665;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3350 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3350 >>> 8));
          write(SP, (value_3350 & 0xFF));
          _nextPC1665 = 8;
          MEMPTR = _nextPC1665;
          PC = _nextPC1665;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_26(int opcode) {
    switch (opcode) {
      case 0xD0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3351 = SP;
          if ((!((F & 1) == 1))) {
              int wordNumber1_3353 = read(SP, 0);
              int wordNumber_3354 = read((SP + 1) & 0xFFFF, 0);
              int value_3352 = ((wordNumber_3354 << 8) | wordNumber1_3353);
              int wordNumber_3355 = SP;
              SP = ((wordNumber_3355 + 2) & 0xFFFF);
              jumpAddress2_3351 = value_3352;
              MEMPTR = jumpAddress2_3351;
              PC = jumpAddress2_3351;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD1: {
          int wordNumber1_3359 = read(SP, 0);
          int wordNumber_3360 = read((SP + 1) & 0xFFFF, 0);
          int value_3358 = ((wordNumber_3360 << 8) | wordNumber1_3359);
          int wordNumber_3361 = SP;
          SP = ((wordNumber_3361 + 2) & 0xFFFF);
          D = (value_3358 >>> 8);
          E = value_3358 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _jumpAddress1668 = 0;
          int address_3363 = (PC + 2) & 0xFFFF;
          int operand_3365 = read(address_3363, 0);
          int operand_3367 = read((address_3363 + 1) & 0xFFFF, 0);
          int jumpAddress2_3362 = (_jumpAddress1668 = (operand_3367 << 8) | operand_3365);
          if ((!((F & 1) == 1))) {
              _jumpAddress1668 = jumpAddress2_3362;
              MEMPTR = _jumpAddress1668;
              PC = jumpAddress2_3362;
              break;
          } else {
              MEMPTR = _jumpAddress1668;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD3: {
          int operand_3370 = read((PC + 2) & 0xFFFF, 0);
          int read_3369 = operand_3370;
          read_3369 = (read_3369 | A << 8);
          io.out(read_3369, A);
          MEMPTR = (A << 8);
          int read_3371 = operand_3370;
          read_3371 = (read_3371 | A << 8);
          MEMPTR = (MEMPTR | ((read_3371 + 1) & 0xff));
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _jumpAddress1670 = 0;
          int address_3372 = (PC + 2) & 0xFFFF;
          int operand_3374 = read(address_3372, 0);
          int operand_3376 = read((address_3372 + 1) & 0xFFFF, 0);
          int value_3377 = (_jumpAddress1670 = (operand_3376 << 8) | operand_3374);
          MEMPTR = value_3377;
          int jumpAddress2_3378 = (_jumpAddress1670 = (operand_3376 << 8) | operand_3374);
          if ((!((F & 1) == 1))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3382 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3382 >>> 8));
              write(SP, (value_3382 & 0xFF));
              _jumpAddress1670 = jumpAddress2_3378;
              MEMPTR = _jumpAddress1670;
              PC = jumpAddress2_3378;
              break;
          } else {
              MEMPTR = _jumpAddress1670;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3384 = ((D << 8) | E);
          write((SP + 1) & 0xFFFF, (value_3384 >>> 8));
          write(SP, (value_3384 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _F1672;
          int operand_3385 = read((PC + 2) & 0xFFFF, 0);
          int value1_3386 = A;
          int subtemp_3389 = value1_3386 - operand_3385;
          int lookup_3390 = ((value1_3386 & 0x88) >> 3) | ((operand_3385 & 0x88) >> 2) | ((subtemp_3389 & 0x88) >> 1);
          value1_3386 = subtemp_3389 & 0xff;
          _F1672 = ((subtemp_3389 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3390 & 0x07)] | OVERFLOW_SUB[(lookup_3390 >> 4)] | (SZ53[value1_3386] | (value1_3386 == 0 ? 0x40 : 0));
          F = (_F1672 & 0xFF);
          A = value1_3386;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _nextPC1674;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3392 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3392 >>> 8));
          write(SP, (value_3392 & 0xFF));
          _nextPC1674 = 0x10;
          MEMPTR = _nextPC1674;
          PC = _nextPC1674;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_27(int opcode) {
    switch (opcode) {
      case 0xD8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3393 = SP;
          if (((F & 1) == 1)) {
              int wordNumber1_3395 = read(SP, 0);
              int wordNumber_3396 = read((SP + 1) & 0xFFFF, 0);
              int value_3394 = ((wordNumber_3396 << 8) | wordNumber1_3395);
              int wordNumber_3397 = SP;
              SP = ((wordNumber_3397 + 2) & 0xFFFF);
              jumpAddress2_3393 = value_3394;
              MEMPTR = jumpAddress2_3393;
              PC = jumpAddress2_3393;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD9: {
          int v1_3399 = ((B << 8) | C);
          B = (_BC >>> 8);
          C = _BC & 0xFF;
          _BC = v1_3399;
          v1_3399 = (D << 8) | E;
          D = (_DE >>> 8);
          E = _DE & 0xFF;
          _DE = v1_3399;
          v1_3399 = (H << 8) | L;
          H = (_HL >>> 8);
          L = _HL & 0xFF;
          _HL = v1_3399;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _jumpAddress1677 = 0;
          int address_3401 = (PC + 2) & 0xFFFF;
          int operand_3403 = read(address_3401, 0);
          int operand_3405 = read((address_3401 + 1) & 0xFFFF, 0);
          int jumpAddress2_3400 = (_jumpAddress1677 = (operand_3405 << 8) | operand_3403);
          if (((F & 1) == 1)) {
              _jumpAddress1677 = jumpAddress2_3400;
              MEMPTR = _jumpAddress1677;
              PC = jumpAddress2_3400;
              break;
          } else {
              MEMPTR = _jumpAddress1677;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDB: {
          int operand_3408 = read((PC + 2) & 0xFFFF, 0);
          MEMPTR = (((operand_3408 | A << 8) + 1) & 0xFFFF);
          int port_3409 = (operand_3408 | A << 8);
          int value_3411 = io.in(port_3409);
          A = value_3411 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress1679 = 0;
          int address_3412 = (PC + 2) & 0xFFFF;
          int operand_3414 = read(address_3412, 0);
          int operand_3416 = read((address_3412 + 1) & 0xFFFF, 0);
          int value_3417 = (_jumpAddress1679 = (operand_3416 << 8) | operand_3414);
          MEMPTR = value_3417;
          int jumpAddress2_3418 = (_jumpAddress1679 = (operand_3416 << 8) | operand_3414);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3422 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3422 >>> 8));
              write(SP, (value_3422 & 0xFF));
              _jumpAddress1679 = jumpAddress2_3418;
              MEMPTR = _jumpAddress1679;
              PC = jumpAddress2_3418;
              break;
          } else {
              MEMPTR = _jumpAddress1679;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _F1681;
          int operand_3424 = read((PC + 2) & 0xFFFF, 0);
          int value1_3425 = A;
          int value3_3427 = F & 1;
          _F1681 = value3_3427;
          int sbctemp_3428 = value1_3425 - operand_3424 - (_F1681 & 1);
          int lookup_3429 = ((value1_3425 & 0x88) >> 3) | ((operand_3424 & 0x88) >> 2) | ((sbctemp_3428 & 0x88) >> 1);
          value1_3425 = sbctemp_3428 & 0xff;
          _F1681 = ((sbctemp_3428 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3429 & 0x07)] | OVERFLOW_SUB[(lookup_3429 >> 4)] | (SZ53[value1_3425] | (value1_3425 == 0 ? 0x40 : 0));
          F = (_F1681 & 0xFF);
          A = value1_3425;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC1683;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3431 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3431 >>> 8));
          write(SP, (value_3431 & 0xFF));
          _nextPC1683 = 0x18;
          MEMPTR = _nextPC1683;
          PC = _nextPC1683;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_28(int opcode) {
    switch (opcode) {
      case 0xE0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3432 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_3434 = read(SP, 0);
              int wordNumber_3435 = read((SP + 1) & 0xFFFF, 0);
              int value_3433 = ((wordNumber_3435 << 8) | wordNumber1_3434);
              int wordNumber_3436 = SP;
              SP = ((wordNumber_3436 + 2) & 0xFFFF);
              jumpAddress2_3432 = value_3433;
              MEMPTR = jumpAddress2_3432;
              PC = jumpAddress2_3432;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_3440 = read(SP, 0);
          int wordNumber_3441 = read((SP + 1) & 0xFFFF, 0);
          int value_3439 = ((wordNumber_3441 << 8) | wordNumber1_3440);
          int wordNumber_3442 = SP;
          SP = ((wordNumber_3442 + 2) & 0xFFFF);
          IX = value_3439;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress1686 = 0;
          int address_3444 = (PC + 2) & 0xFFFF;
          int operand_3446 = read(address_3444, 0);
          int operand_3448 = read((address_3444 + 1) & 0xFFFF, 0);
          int jumpAddress2_3443 = (_jumpAddress1686 = (operand_3448 << 8) | operand_3446);
          if ((!((F & 4) == 4))) {
              _jumpAddress1686 = jumpAddress2_3443;
              MEMPTR = _jumpAddress1686;
              PC = jumpAddress2_3443;
              break;
          } else {
              MEMPTR = _jumpAddress1686;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address1687 = SP;
          int wordNumber1_3451 = read(_address1687, 0);
          int wordNumber_3452 = read((_address1687 + 1) & 0xFFFF, 0);
          int v1_3450 = ((wordNumber_3452 << 8) | wordNumber1_3451);
          int v2_3453 = IX;
          _address1687 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address1687 + 1) & 0xFFFF, (v2_3453 >>> 8));
          write(_address1687, (v2_3453 & 0xFF));
          IX = v1_3450;
          MEMPTR = IX;
          contend2x1(SP);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress1689 = 0;
          int address_3454 = (PC + 2) & 0xFFFF;
          int operand_3456 = read(address_3454, 0);
          int operand_3458 = read((address_3454 + 1) & 0xFFFF, 0);
          int value_3459 = (_jumpAddress1689 = (operand_3458 << 8) | operand_3456);
          MEMPTR = value_3459;
          int jumpAddress2_3460 = (_jumpAddress1689 = (operand_3458 << 8) | operand_3456);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3464 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3464 >>> 8));
              write(SP, (value_3464 & 0xFF));
              _jumpAddress1689 = jumpAddress2_3460;
              MEMPTR = _jumpAddress1689;
              PC = jumpAddress2_3460;
              break;
          } else {
              MEMPTR = _jumpAddress1689;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (IX >>> 8));
          write(SP, (IX & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _F1691;
          int operand_3466 = read((PC + 2) & 0xFFFF, 0);
          int value1_3467 = A;
          int value2_3468 = operand_3466;
          value2_3468 &= value1_3467;
          _F1691 = 0x10 | (SZ53P[value2_3468 & 0xff] | (value2_3468 == 0 ? 0x40 : 0));
          int result_3470 = value2_3468 & 0xFF;
          F = (_F1691 & 0xFF);
          A = result_3470;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC1693;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3471 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3471 >>> 8));
          write(SP, (value_3471 & 0xFF));
          _nextPC1693 = 0x20;
          MEMPTR = _nextPC1693;
          PC = _nextPC1693;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_29(int opcode) {
    switch (opcode) {
      case 0xE8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3472 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_3474 = read(SP, 0);
              int wordNumber_3475 = read((SP + 1) & 0xFFFF, 0);
              int value_3473 = ((wordNumber_3475 << 8) | wordNumber1_3474);
              int wordNumber_3476 = SP;
              SP = ((wordNumber_3476 + 2) & 0xFFFF);
              jumpAddress2_3472 = value_3473;
              MEMPTR = jumpAddress2_3472;
              PC = jumpAddress2_3472;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE9: {
          MEMPTR = 0;
          PC = IX;
          break;
      }
      case 0xEA: {
          int _jumpAddress1696 = 0;
          int address_3481 = (PC + 2) & 0xFFFF;
          int operand_3483 = read(address_3481, 0);
          int operand_3485 = read((address_3481 + 1) & 0xFFFF, 0);
          int jumpAddress2_3480 = (_jumpAddress1696 = (operand_3485 << 8) | operand_3483);
          if (((F & 4) == 4)) {
              _jumpAddress1696 = jumpAddress2_3480;
              MEMPTR = _jumpAddress1696;
              PC = jumpAddress2_3480;
              break;
          } else {
              MEMPTR = _jumpAddress1696;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_3487 = ((D << 8) | E);
          int v2_3488 = ((H << 8) | L);
          D = (v2_3488 >>> 8);
          E = v2_3488 & 0xFF;
          H = (v1_3487 >>> 8);
          L = v1_3487 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress1698 = 0;
          int address_3489 = (PC + 2) & 0xFFFF;
          int operand_3491 = read(address_3489, 0);
          int operand_3493 = read((address_3489 + 1) & 0xFFFF, 0);
          int value_3494 = (_jumpAddress1698 = (operand_3493 << 8) | operand_3491);
          MEMPTR = value_3494;
          int jumpAddress2_3495 = (_jumpAddress1698 = (operand_3493 << 8) | operand_3491);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3499 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3499 >>> 8));
              write(SP, (value_3499 & 0xFF));
              _jumpAddress1698 = jumpAddress2_3495;
              MEMPTR = _jumpAddress1698;
              PC = jumpAddress2_3495;
              break;
          } else {
              MEMPTR = _jumpAddress1698;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xED: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _F1700;
          int operand_3501 = read((PC + 2) & 0xFFFF, 0);
          int value1_3502 = A;
          int value2_3503 = operand_3501;
          value2_3503 ^= value1_3502;
          _F1700 = SZ53P[value2_3503 & 0xff] | (value2_3503 == 0 ? 0x40 : 0);
          int result_3505 = value2_3503 & 0xFF;
          F = (_F1700 & 0xFF);
          A = result_3505;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC1702;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3506 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3506 >>> 8));
          write(SP, (value_3506 & 0xFF));
          _nextPC1702 = 0x28;
          MEMPTR = _nextPC1702;
          PC = _nextPC1702;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_30(int opcode) {
    switch (opcode) {
      case 0xF0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3507 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_3509 = read(SP, 0);
              int wordNumber_3510 = read((SP + 1) & 0xFFFF, 0);
              int value_3508 = ((wordNumber_3510 << 8) | wordNumber1_3509);
              int wordNumber_3511 = SP;
              SP = ((wordNumber_3511 + 2) & 0xFFFF);
              jumpAddress2_3507 = value_3508;
              MEMPTR = jumpAddress2_3507;
              PC = jumpAddress2_3507;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_3515 = read(SP, 0);
          int wordNumber_3516 = read((SP + 1) & 0xFFFF, 0);
          int value_3514 = ((wordNumber_3516 << 8) | wordNumber1_3515);
          int wordNumber_3517 = SP;
          SP = ((wordNumber_3517 + 2) & 0xFFFF);
          A = (value_3514 >>> 8);
          F = value_3514 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress1705 = 0;
          int address_3519 = (PC + 2) & 0xFFFF;
          int operand_3521 = read(address_3519, 0);
          int operand_3523 = read((address_3519 + 1) & 0xFFFF, 0);
          int jumpAddress2_3518 = (_jumpAddress1705 = (operand_3523 << 8) | operand_3521);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress1705 = jumpAddress2_3518;
              MEMPTR = _jumpAddress1705;
              PC = jumpAddress2_3518;
              break;
          } else {
              MEMPTR = _jumpAddress1705;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xF3: {
          state.resetInterrupt();
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _jumpAddress1707 = 0;
          int address_3525 = (PC + 2) & 0xFFFF;
          int operand_3527 = read(address_3525, 0);
          int operand_3529 = read((address_3525 + 1) & 0xFFFF, 0);
          int value_3530 = (_jumpAddress1707 = (operand_3529 << 8) | operand_3527);
          MEMPTR = value_3530;
          int jumpAddress2_3531 = (_jumpAddress1707 = (operand_3529 << 8) | operand_3527);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3535 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3535 >>> 8));
              write(SP, (value_3535 & 0xFF));
              _jumpAddress1707 = jumpAddress2_3531;
              MEMPTR = _jumpAddress1707;
              PC = jumpAddress2_3531;
              break;
          } else {
              MEMPTR = _jumpAddress1707;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xF5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3537 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_3537 >>> 8));
          write(SP, (value_3537 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F1709;
          int operand_3538 = read((PC + 2) & 0xFFFF, 0);
          int value1_3539 = A;
          int value2_3540 = operand_3538;
          value2_3540 |= value1_3539;
          _F1709 = SZ53P[value2_3540 & 0xff] | (value2_3540 == 0 ? 0x40 : 0);
          int result_3542 = value2_3540 & 0xFF;
          F = (_F1709 & 0xFF);
          A = result_3542;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC1711;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3543 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3543 >>> 8));
          write(SP, (value_3543 & 0xFF));
          _nextPC1711 = 0x30;
          MEMPTR = _nextPC1711;
          PC = _nextPC1711;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDD_31(int opcode) {
    switch (opcode) {
      case 0xF8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_3544 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_3546 = read(SP, 0);
              int wordNumber_3547 = read((SP + 1) & 0xFFFF, 0);
              int value_3545 = ((wordNumber_3547 << 8) | wordNumber1_3546);
              int wordNumber_3548 = SP;
              SP = ((wordNumber_3548 + 2) & 0xFFFF);
              jumpAddress2_3544 = value_3545;
              MEMPTR = jumpAddress2_3544;
              PC = jumpAddress2_3544;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF9: {
          contend2x1(((I << 8) | R));
          SP = IX;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _jumpAddress1714 = 0;
          int address_3551 = (PC + 2) & 0xFFFF;
          int operand_3553 = read(address_3551, 0);
          int operand_3555 = read((address_3551 + 1) & 0xFFFF, 0);
          int jumpAddress2_3550 = (_jumpAddress1714 = (operand_3555 << 8) | operand_3553);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress1714 = jumpAddress2_3550;
              MEMPTR = _jumpAddress1714;
              PC = jumpAddress2_3550;
              break;
          } else {
              MEMPTR = _jumpAddress1714;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xFB: {
          state.enableInterrupt();
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _jumpAddress1716 = 0;
          int address_3557 = (PC + 2) & 0xFFFF;
          int operand_3559 = read(address_3557, 0);
          int operand_3561 = read((address_3557 + 1) & 0xFFFF, 0);
          int value_3562 = (_jumpAddress1716 = (operand_3561 << 8) | operand_3559);
          MEMPTR = value_3562;
          int jumpAddress2_3563 = (_jumpAddress1716 = (operand_3561 << 8) | operand_3559);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3567 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3567 >>> 8));
              write(SP, (value_3567 & 0xFF));
              _jumpAddress1716 = jumpAddress2_3563;
              MEMPTR = _jumpAddress1716;
              PC = jumpAddress2_3563;
              break;
          } else {
              MEMPTR = _jumpAddress1716;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xFD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _F1718;
          int operand_3569 = read((PC + 2) & 0xFFFF, 0);
          int cptemp_3573 = A - operand_3569;
          int lookup_3574 = ((A & 0x88) >> 3) | ((operand_3569 & 0x88) >> 2) | ((cptemp_3573 & 0x88) >> 1);
          _F1718 = ((cptemp_3573 & 0x100) != 0 ? 1 : (cptemp_3573 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_3574 & 0x07)] | OVERFLOW_SUB[(lookup_3574 >> 4)] | (operand_3569 & 0x28) | (cptemp_3573 & 0x80);
          F = (_F1718 & 0xFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC1720;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3576 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3576 >>> 8));
          write(SP, (value_3576 & 0xFF));
          _nextPC1720 = 0x38;
          MEMPTR = _nextPC1720;
          PC = _nextPC1720;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDD");
    }
  }

  private void decodeDDCB(int opcode, int displacement) {
    switch (opcode >> 3) {
      case 0: decodeDDCB_0(opcode, displacement);
        break;
      case 1: decodeDDCB_1(opcode, displacement);
        break;
      case 2: decodeDDCB_2(opcode, displacement);
        break;
      case 3: decodeDDCB_3(opcode, displacement);
        break;
      case 4: decodeDDCB_4(opcode, displacement);
        break;
      case 5: decodeDDCB_5(opcode, displacement);
        break;
      case 6: decodeDDCB_6(opcode, displacement);
        break;
      case 7: decodeDDCB_7(opcode, displacement);
        break;
      case 8: decodeDDCB_8(opcode, displacement);
        break;
      case 9: decodeDDCB_9(opcode, displacement);
        break;
      case 10: decodeDDCB_10(opcode, displacement);
        break;
      case 11: decodeDDCB_11(opcode, displacement);
        break;
      case 12: decodeDDCB_12(opcode, displacement);
        break;
      case 13: decodeDDCB_13(opcode, displacement);
        break;
      case 14: decodeDDCB_14(opcode, displacement);
        break;
      case 15: decodeDDCB_15(opcode, displacement);
        break;
      case 16: decodeDDCB_16(opcode, displacement);
        break;
      case 17: decodeDDCB_17(opcode, displacement);
        break;
      case 18: decodeDDCB_18(opcode, displacement);
        break;
      case 19: decodeDDCB_19(opcode, displacement);
        break;
      case 20: decodeDDCB_20(opcode, displacement);
        break;
      case 21: decodeDDCB_21(opcode, displacement);
        break;
      case 22: decodeDDCB_22(opcode, displacement);
        break;
      case 23: decodeDDCB_23(opcode, displacement);
        break;
      case 24: decodeDDCB_24(opcode, displacement);
        break;
      case 25: decodeDDCB_25(opcode, displacement);
        break;
      case 26: decodeDDCB_26(opcode, displacement);
        break;
      case 27: decodeDDCB_27(opcode, displacement);
        break;
      case 28: decodeDDCB_28(opcode, displacement);
        break;
      case 29: decodeDDCB_29(opcode, displacement);
        break;
      case 30: decodeDDCB_30(opcode, displacement);
        break;
      case 31: decodeDDCB_31(opcode, displacement);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_0(int opcode, int displacement) {
    switch (opcode) {
      case 0x00: {
          int _F1022;
          int _value1021;
          int _address1021;
          contend2x1((PC + 3) & 0xFFFF);
          _address1021 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2111 = read(_address1021, 0);
          contend1x1(_address1021);
          _value1021 = operand_2111;
          int value1_2112 = _value1021;
          value1_2112 = (value1_2112 << 1 | value1_2112 >> 7) & 0xff;
          _F1022 = (value1_2112 & 1) | (SZ53P[value1_2112] | (value1_2112 == 0 ? 0x40 : 0));
          F = (_F1022 & 0xFF);
          _address1021 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1021 = value1_2112;
          write(_address1021, value1_2112);
          int read_2116;
          read_2116 = _value1021;
          B = read_2116;
          MEMPTR = _address1021;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x01: {
          int _F1025;
          int _value1024;
          int _address1024;
          contend2x1((PC + 3) & 0xFFFF);
          _address1024 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2117 = read(_address1024, 0);
          contend1x1(_address1024);
          _value1024 = operand_2117;
          int value1_2118 = _value1024;
          value1_2118 = (value1_2118 << 1 | value1_2118 >> 7) & 0xff;
          _F1025 = (value1_2118 & 1) | (SZ53P[value1_2118] | (value1_2118 == 0 ? 0x40 : 0));
          F = (_F1025 & 0xFF);
          _address1024 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1024 = value1_2118;
          write(_address1024, value1_2118);
          int read_2122;
          read_2122 = _value1024;
          C = read_2122;
          MEMPTR = _address1024;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _F1028;
          int _value1027;
          int _address1027;
          contend2x1((PC + 3) & 0xFFFF);
          _address1027 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2123 = read(_address1027, 0);
          contend1x1(_address1027);
          _value1027 = operand_2123;
          int value1_2124 = _value1027;
          value1_2124 = (value1_2124 << 1 | value1_2124 >> 7) & 0xff;
          _F1028 = (value1_2124 & 1) | (SZ53P[value1_2124] | (value1_2124 == 0 ? 0x40 : 0));
          F = (_F1028 & 0xFF);
          _address1027 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1027 = value1_2124;
          write(_address1027, value1_2124);
          int read_2128;
          read_2128 = _value1027;
          D = read_2128;
          MEMPTR = _address1027;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x03: {
          int _F1031;
          int _value1030;
          int _address1030;
          contend2x1((PC + 3) & 0xFFFF);
          _address1030 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2129 = read(_address1030, 0);
          contend1x1(_address1030);
          _value1030 = operand_2129;
          int value1_2130 = _value1030;
          value1_2130 = (value1_2130 << 1 | value1_2130 >> 7) & 0xff;
          _F1031 = (value1_2130 & 1) | (SZ53P[value1_2130] | (value1_2130 == 0 ? 0x40 : 0));
          F = (_F1031 & 0xFF);
          _address1030 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1030 = value1_2130;
          write(_address1030, value1_2130);
          int read_2134;
          read_2134 = _value1030;
          E = read_2134;
          MEMPTR = _address1030;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F1034;
          int _value1033;
          int _address1033;
          contend2x1((PC + 3) & 0xFFFF);
          _address1033 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2135 = read(_address1033, 0);
          contend1x1(_address1033);
          _value1033 = operand_2135;
          int value1_2136 = _value1033;
          value1_2136 = (value1_2136 << 1 | value1_2136 >> 7) & 0xff;
          _F1034 = (value1_2136 & 1) | (SZ53P[value1_2136] | (value1_2136 == 0 ? 0x40 : 0));
          F = (_F1034 & 0xFF);
          _address1033 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1033 = value1_2136;
          write(_address1033, value1_2136);
          int read_2140;
          read_2140 = _value1033;
          H = read_2140;
          MEMPTR = _address1033;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F1037;
          int _value1036;
          int _address1036;
          contend2x1((PC + 3) & 0xFFFF);
          _address1036 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2141 = read(_address1036, 0);
          contend1x1(_address1036);
          _value1036 = operand_2141;
          int value1_2142 = _value1036;
          value1_2142 = (value1_2142 << 1 | value1_2142 >> 7) & 0xff;
          _F1037 = (value1_2142 & 1) | (SZ53P[value1_2142] | (value1_2142 == 0 ? 0x40 : 0));
          F = (_F1037 & 0xFF);
          _address1036 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1036 = value1_2142;
          write(_address1036, value1_2142);
          int read_2146;
          read_2146 = _value1036;
          L = read_2146;
          MEMPTR = _address1036;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x06: {
          int _F1040;
          int _value1039;
          int _address1039;
          contend2x1((PC + 3) & 0xFFFF);
          _address1039 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2147 = read(_address1039, 0);
          contend1x1(_address1039);
          _value1039 = operand_2147;
          int value1_2148 = _value1039;
          value1_2148 = (value1_2148 << 1 | value1_2148 >> 7) & 0xff;
          _F1040 = (value1_2148 & 1) | (SZ53P[value1_2148] | (value1_2148 == 0 ? 0x40 : 0));
          F = (_F1040 & 0xFF);
          _address1039 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1039 = value1_2148;
          write(_address1039, value1_2148);
          MEMPTR = _address1039;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F1043;
          int _value1042;
          int _address1042;
          contend2x1((PC + 3) & 0xFFFF);
          _address1042 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2152 = read(_address1042, 0);
          contend1x1(_address1042);
          _value1042 = operand_2152;
          int value1_2153 = _value1042;
          value1_2153 = (value1_2153 << 1 | value1_2153 >> 7) & 0xff;
          _F1043 = (value1_2153 & 1) | (SZ53P[value1_2153] | (value1_2153 == 0 ? 0x40 : 0));
          F = (_F1043 & 0xFF);
          _address1042 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1042 = value1_2153;
          write(_address1042, value1_2153);
          int read_2157;
          read_2157 = _value1042;
          A = read_2157;
          MEMPTR = _address1042;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_1(int opcode, int displacement) {
    switch (opcode) {
      case 0x08: {
          int _F1046;
          int _value1045;
          int _address1045;
          contend2x1((PC + 3) & 0xFFFF);
          _address1045 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2158 = read(_address1045, 0);
          contend1x1(_address1045);
          _value1045 = operand_2158;
          int value1_2159 = _value1045;
          _F1046 = value1_2159 & 1;
          value1_2159 = (value1_2159 >> 1) | (value1_2159 << 7);
          value1_2159 &= 0xff;
          _F1046 |= (SZ53P[value1_2159 & 0xff] | (value1_2159 == 0 ? 0x40 : 0));
          int result_2162 = value1_2159 & 0xFF;
          F = (_F1046 & 0xFF);
          _address1045 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1045 = result_2162;
          write(_address1045, result_2162);
          int read_2163;
          read_2163 = _value1045;
          B = read_2163;
          MEMPTR = _address1045;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F1049;
          int _value1048;
          int _address1048;
          contend2x1((PC + 3) & 0xFFFF);
          _address1048 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2164 = read(_address1048, 0);
          contend1x1(_address1048);
          _value1048 = operand_2164;
          int value1_2165 = _value1048;
          _F1049 = value1_2165 & 1;
          value1_2165 = (value1_2165 >> 1) | (value1_2165 << 7);
          value1_2165 &= 0xff;
          _F1049 |= (SZ53P[value1_2165 & 0xff] | (value1_2165 == 0 ? 0x40 : 0));
          int result_2168 = value1_2165 & 0xFF;
          F = (_F1049 & 0xFF);
          _address1048 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1048 = result_2168;
          write(_address1048, result_2168);
          int read_2169;
          read_2169 = _value1048;
          C = read_2169;
          MEMPTR = _address1048;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _F1052;
          int _value1051;
          int _address1051;
          contend2x1((PC + 3) & 0xFFFF);
          _address1051 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2170 = read(_address1051, 0);
          contend1x1(_address1051);
          _value1051 = operand_2170;
          int value1_2171 = _value1051;
          _F1052 = value1_2171 & 1;
          value1_2171 = (value1_2171 >> 1) | (value1_2171 << 7);
          value1_2171 &= 0xff;
          _F1052 |= (SZ53P[value1_2171 & 0xff] | (value1_2171 == 0 ? 0x40 : 0));
          int result_2174 = value1_2171 & 0xFF;
          F = (_F1052 & 0xFF);
          _address1051 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1051 = result_2174;
          write(_address1051, result_2174);
          int read_2175;
          read_2175 = _value1051;
          D = read_2175;
          MEMPTR = _address1051;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int _F1055;
          int _value1054;
          int _address1054;
          contend2x1((PC + 3) & 0xFFFF);
          _address1054 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2176 = read(_address1054, 0);
          contend1x1(_address1054);
          _value1054 = operand_2176;
          int value1_2177 = _value1054;
          _F1055 = value1_2177 & 1;
          value1_2177 = (value1_2177 >> 1) | (value1_2177 << 7);
          value1_2177 &= 0xff;
          _F1055 |= (SZ53P[value1_2177 & 0xff] | (value1_2177 == 0 ? 0x40 : 0));
          int result_2180 = value1_2177 & 0xFF;
          F = (_F1055 & 0xFF);
          _address1054 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1054 = result_2180;
          write(_address1054, result_2180);
          int read_2181;
          read_2181 = _value1054;
          E = read_2181;
          MEMPTR = _address1054;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F1058;
          int _value1057;
          int _address1057;
          contend2x1((PC + 3) & 0xFFFF);
          _address1057 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2182 = read(_address1057, 0);
          contend1x1(_address1057);
          _value1057 = operand_2182;
          int value1_2183 = _value1057;
          _F1058 = value1_2183 & 1;
          value1_2183 = (value1_2183 >> 1) | (value1_2183 << 7);
          value1_2183 &= 0xff;
          _F1058 |= (SZ53P[value1_2183 & 0xff] | (value1_2183 == 0 ? 0x40 : 0));
          int result_2186 = value1_2183 & 0xFF;
          F = (_F1058 & 0xFF);
          _address1057 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1057 = result_2186;
          write(_address1057, result_2186);
          int read_2187;
          read_2187 = _value1057;
          H = read_2187;
          MEMPTR = _address1057;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F1061;
          int _value1060;
          int _address1060;
          contend2x1((PC + 3) & 0xFFFF);
          _address1060 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2188 = read(_address1060, 0);
          contend1x1(_address1060);
          _value1060 = operand_2188;
          int value1_2189 = _value1060;
          _F1061 = value1_2189 & 1;
          value1_2189 = (value1_2189 >> 1) | (value1_2189 << 7);
          value1_2189 &= 0xff;
          _F1061 |= (SZ53P[value1_2189 & 0xff] | (value1_2189 == 0 ? 0x40 : 0));
          int result_2192 = value1_2189 & 0xFF;
          F = (_F1061 & 0xFF);
          _address1060 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1060 = result_2192;
          write(_address1060, result_2192);
          int read_2193;
          read_2193 = _value1060;
          L = read_2193;
          MEMPTR = _address1060;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int _F1064;
          int _value1063;
          int _address1063;
          contend2x1((PC + 3) & 0xFFFF);
          _address1063 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2194 = read(_address1063, 0);
          contend1x1(_address1063);
          _value1063 = operand_2194;
          int value1_2195 = _value1063;
          _F1064 = value1_2195 & 1;
          value1_2195 = (value1_2195 >> 1) | (value1_2195 << 7);
          value1_2195 &= 0xff;
          _F1064 |= (SZ53P[value1_2195 & 0xff] | (value1_2195 == 0 ? 0x40 : 0));
          int result_2198 = value1_2195 & 0xFF;
          F = (_F1064 & 0xFF);
          _address1063 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1063 = result_2198;
          write(_address1063, result_2198);
          MEMPTR = _address1063;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F1067;
          int _value1066;
          int _address1066;
          contend2x1((PC + 3) & 0xFFFF);
          _address1066 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2199 = read(_address1066, 0);
          contend1x1(_address1066);
          _value1066 = operand_2199;
          int value1_2200 = _value1066;
          _F1067 = value1_2200 & 1;
          value1_2200 = (value1_2200 >> 1) | (value1_2200 << 7);
          value1_2200 &= 0xff;
          _F1067 |= (SZ53P[value1_2200 & 0xff] | (value1_2200 == 0 ? 0x40 : 0));
          int result_2203 = value1_2200 & 0xFF;
          F = (_F1067 & 0xFF);
          _address1066 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1066 = result_2203;
          write(_address1066, result_2203);
          int read_2204;
          read_2204 = _value1066;
          A = read_2204;
          MEMPTR = _address1066;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_2(int opcode, int displacement) {
    switch (opcode) {
      case 0x10: {
          int _F1070;
          int _value1069;
          int _address1069;
          contend2x1((PC + 3) & 0xFFFF);
          _address1069 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2205 = read(_address1069, 0);
          contend1x1(_address1069);
          _value1069 = operand_2205;
          int value1_2206 = _value1069;
          int value2_2207 = F;
          _F1070 = value2_2207;
          int rltemp_2209 = value1_2206;
          value1_2206 = (value1_2206 << 1) | (_F1070 & 1);
          value1_2206 &= 0xff;
          _F1070 = (rltemp_2209 >> 7) | (SZ53P[value1_2206 & 0xff] | (value1_2206 == 0 ? 0x40 : 0));
          int result_2210 = value1_2206 & 0xFF;
          F = (_F1070 & 0xFF);
          _address1069 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1069 = result_2210;
          write(_address1069, result_2210);
          int read_2211;
          read_2211 = _value1069;
          B = read_2211;
          MEMPTR = _address1069;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x11: {
          int _F1073;
          int _value1072;
          int _address1072;
          contend2x1((PC + 3) & 0xFFFF);
          _address1072 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2212 = read(_address1072, 0);
          contend1x1(_address1072);
          _value1072 = operand_2212;
          int value1_2213 = _value1072;
          int value2_2214 = F;
          _F1073 = value2_2214;
          int rltemp_2216 = value1_2213;
          value1_2213 = (value1_2213 << 1) | (_F1073 & 1);
          value1_2213 &= 0xff;
          _F1073 = (rltemp_2216 >> 7) | (SZ53P[value1_2213 & 0xff] | (value1_2213 == 0 ? 0x40 : 0));
          int result_2217 = value1_2213 & 0xFF;
          F = (_F1073 & 0xFF);
          _address1072 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1072 = result_2217;
          write(_address1072, result_2217);
          int read_2218;
          read_2218 = _value1072;
          C = read_2218;
          MEMPTR = _address1072;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _F1076;
          int _value1075;
          int _address1075;
          contend2x1((PC + 3) & 0xFFFF);
          _address1075 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2219 = read(_address1075, 0);
          contend1x1(_address1075);
          _value1075 = operand_2219;
          int value1_2220 = _value1075;
          int value2_2221 = F;
          _F1076 = value2_2221;
          int rltemp_2223 = value1_2220;
          value1_2220 = (value1_2220 << 1) | (_F1076 & 1);
          value1_2220 &= 0xff;
          _F1076 = (rltemp_2223 >> 7) | (SZ53P[value1_2220 & 0xff] | (value1_2220 == 0 ? 0x40 : 0));
          int result_2224 = value1_2220 & 0xFF;
          F = (_F1076 & 0xFF);
          _address1075 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1075 = result_2224;
          write(_address1075, result_2224);
          int read_2225;
          read_2225 = _value1075;
          D = read_2225;
          MEMPTR = _address1075;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x13: {
          int _F1079;
          int _value1078;
          int _address1078;
          contend2x1((PC + 3) & 0xFFFF);
          _address1078 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2226 = read(_address1078, 0);
          contend1x1(_address1078);
          _value1078 = operand_2226;
          int value1_2227 = _value1078;
          int value2_2228 = F;
          _F1079 = value2_2228;
          int rltemp_2230 = value1_2227;
          value1_2227 = (value1_2227 << 1) | (_F1079 & 1);
          value1_2227 &= 0xff;
          _F1079 = (rltemp_2230 >> 7) | (SZ53P[value1_2227 & 0xff] | (value1_2227 == 0 ? 0x40 : 0));
          int result_2231 = value1_2227 & 0xFF;
          F = (_F1079 & 0xFF);
          _address1078 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1078 = result_2231;
          write(_address1078, result_2231);
          int read_2232;
          read_2232 = _value1078;
          E = read_2232;
          MEMPTR = _address1078;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F1082;
          int _value1081;
          int _address1081;
          contend2x1((PC + 3) & 0xFFFF);
          _address1081 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2233 = read(_address1081, 0);
          contend1x1(_address1081);
          _value1081 = operand_2233;
          int value1_2234 = _value1081;
          int value2_2235 = F;
          _F1082 = value2_2235;
          int rltemp_2237 = value1_2234;
          value1_2234 = (value1_2234 << 1) | (_F1082 & 1);
          value1_2234 &= 0xff;
          _F1082 = (rltemp_2237 >> 7) | (SZ53P[value1_2234 & 0xff] | (value1_2234 == 0 ? 0x40 : 0));
          int result_2238 = value1_2234 & 0xFF;
          F = (_F1082 & 0xFF);
          _address1081 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1081 = result_2238;
          write(_address1081, result_2238);
          int read_2239;
          read_2239 = _value1081;
          H = read_2239;
          MEMPTR = _address1081;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F1085;
          int _value1084;
          int _address1084;
          contend2x1((PC + 3) & 0xFFFF);
          _address1084 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2240 = read(_address1084, 0);
          contend1x1(_address1084);
          _value1084 = operand_2240;
          int value1_2241 = _value1084;
          int value2_2242 = F;
          _F1085 = value2_2242;
          int rltemp_2244 = value1_2241;
          value1_2241 = (value1_2241 << 1) | (_F1085 & 1);
          value1_2241 &= 0xff;
          _F1085 = (rltemp_2244 >> 7) | (SZ53P[value1_2241 & 0xff] | (value1_2241 == 0 ? 0x40 : 0));
          int result_2245 = value1_2241 & 0xFF;
          F = (_F1085 & 0xFF);
          _address1084 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1084 = result_2245;
          write(_address1084, result_2245);
          int read_2246;
          read_2246 = _value1084;
          L = read_2246;
          MEMPTR = _address1084;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x16: {
          int _F1088;
          int _value1087;
          int _address1087;
          contend2x1((PC + 3) & 0xFFFF);
          _address1087 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2247 = read(_address1087, 0);
          contend1x1(_address1087);
          _value1087 = operand_2247;
          int value1_2248 = _value1087;
          int value2_2249 = F;
          _F1088 = value2_2249;
          int rltemp_2251 = value1_2248;
          value1_2248 = (value1_2248 << 1) | (_F1088 & 1);
          value1_2248 &= 0xff;
          _F1088 = (rltemp_2251 >> 7) | (SZ53P[value1_2248 & 0xff] | (value1_2248 == 0 ? 0x40 : 0));
          int result_2252 = value1_2248 & 0xFF;
          F = (_F1088 & 0xFF);
          _address1087 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1087 = result_2252;
          write(_address1087, result_2252);
          MEMPTR = _address1087;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F1091;
          int _value1090;
          int _address1090;
          contend2x1((PC + 3) & 0xFFFF);
          _address1090 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2253 = read(_address1090, 0);
          contend1x1(_address1090);
          _value1090 = operand_2253;
          int value1_2254 = _value1090;
          int value2_2255 = F;
          _F1091 = value2_2255;
          int rltemp_2257 = value1_2254;
          value1_2254 = (value1_2254 << 1) | (_F1091 & 1);
          value1_2254 &= 0xff;
          _F1091 = (rltemp_2257 >> 7) | (SZ53P[value1_2254 & 0xff] | (value1_2254 == 0 ? 0x40 : 0));
          int result_2258 = value1_2254 & 0xFF;
          F = (_F1091 & 0xFF);
          _address1090 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1090 = result_2258;
          write(_address1090, result_2258);
          int read_2259;
          read_2259 = _value1090;
          A = read_2259;
          MEMPTR = _address1090;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_3(int opcode, int displacement) {
    switch (opcode) {
      case 0x18: {
          int _F1094;
          int _value1093;
          int _address1093;
          contend2x1((PC + 3) & 0xFFFF);
          _address1093 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2260 = read(_address1093, 0);
          contend1x1(_address1093);
          _value1093 = operand_2260;
          int value1_2261 = _value1093;
          int value2_2262 = F;
          _F1094 = value2_2262;
          int rrtemp_2264 = value1_2261;
          value1_2261 = (value1_2261 >> 1) | (_F1094 << 7);
          value1_2261 &= 0xff;
          _F1094 = (rrtemp_2264 & 1) | (SZ53P[value1_2261 & 0xff] | (value1_2261 == 0 ? 0x40 : 0));
          int result_2265 = value1_2261 & 0xFF;
          F = (_F1094 & 0xFF);
          _address1093 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1093 = result_2265;
          write(_address1093, result_2265);
          int read_2266;
          read_2266 = _value1093;
          B = read_2266;
          MEMPTR = _address1093;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x19: {
          int _F1097;
          int _value1096;
          int _address1096;
          contend2x1((PC + 3) & 0xFFFF);
          _address1096 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2267 = read(_address1096, 0);
          contend1x1(_address1096);
          _value1096 = operand_2267;
          int value1_2268 = _value1096;
          int value2_2269 = F;
          _F1097 = value2_2269;
          int rrtemp_2271 = value1_2268;
          value1_2268 = (value1_2268 >> 1) | (_F1097 << 7);
          value1_2268 &= 0xff;
          _F1097 = (rrtemp_2271 & 1) | (SZ53P[value1_2268 & 0xff] | (value1_2268 == 0 ? 0x40 : 0));
          int result_2272 = value1_2268 & 0xFF;
          F = (_F1097 & 0xFF);
          _address1096 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1096 = result_2272;
          write(_address1096, result_2272);
          int read_2273;
          read_2273 = _value1096;
          C = read_2273;
          MEMPTR = _address1096;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _F1100;
          int _value1099;
          int _address1099;
          contend2x1((PC + 3) & 0xFFFF);
          _address1099 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2274 = read(_address1099, 0);
          contend1x1(_address1099);
          _value1099 = operand_2274;
          int value1_2275 = _value1099;
          int value2_2276 = F;
          _F1100 = value2_2276;
          int rrtemp_2278 = value1_2275;
          value1_2275 = (value1_2275 >> 1) | (_F1100 << 7);
          value1_2275 &= 0xff;
          _F1100 = (rrtemp_2278 & 1) | (SZ53P[value1_2275 & 0xff] | (value1_2275 == 0 ? 0x40 : 0));
          int result_2279 = value1_2275 & 0xFF;
          F = (_F1100 & 0xFF);
          _address1099 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1099 = result_2279;
          write(_address1099, result_2279);
          int read_2280;
          read_2280 = _value1099;
          D = read_2280;
          MEMPTR = _address1099;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int _F1103;
          int _value1102;
          int _address1102;
          contend2x1((PC + 3) & 0xFFFF);
          _address1102 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2281 = read(_address1102, 0);
          contend1x1(_address1102);
          _value1102 = operand_2281;
          int value1_2282 = _value1102;
          int value2_2283 = F;
          _F1103 = value2_2283;
          int rrtemp_2285 = value1_2282;
          value1_2282 = (value1_2282 >> 1) | (_F1103 << 7);
          value1_2282 &= 0xff;
          _F1103 = (rrtemp_2285 & 1) | (SZ53P[value1_2282 & 0xff] | (value1_2282 == 0 ? 0x40 : 0));
          int result_2286 = value1_2282 & 0xFF;
          F = (_F1103 & 0xFF);
          _address1102 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1102 = result_2286;
          write(_address1102, result_2286);
          int read_2287;
          read_2287 = _value1102;
          E = read_2287;
          MEMPTR = _address1102;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F1106;
          int _value1105;
          int _address1105;
          contend2x1((PC + 3) & 0xFFFF);
          _address1105 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2288 = read(_address1105, 0);
          contend1x1(_address1105);
          _value1105 = operand_2288;
          int value1_2289 = _value1105;
          int value2_2290 = F;
          _F1106 = value2_2290;
          int rrtemp_2292 = value1_2289;
          value1_2289 = (value1_2289 >> 1) | (_F1106 << 7);
          value1_2289 &= 0xff;
          _F1106 = (rrtemp_2292 & 1) | (SZ53P[value1_2289 & 0xff] | (value1_2289 == 0 ? 0x40 : 0));
          int result_2293 = value1_2289 & 0xFF;
          F = (_F1106 & 0xFF);
          _address1105 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1105 = result_2293;
          write(_address1105, result_2293);
          int read_2294;
          read_2294 = _value1105;
          H = read_2294;
          MEMPTR = _address1105;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F1109;
          int _value1108;
          int _address1108;
          contend2x1((PC + 3) & 0xFFFF);
          _address1108 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2295 = read(_address1108, 0);
          contend1x1(_address1108);
          _value1108 = operand_2295;
          int value1_2296 = _value1108;
          int value2_2297 = F;
          _F1109 = value2_2297;
          int rrtemp_2299 = value1_2296;
          value1_2296 = (value1_2296 >> 1) | (_F1109 << 7);
          value1_2296 &= 0xff;
          _F1109 = (rrtemp_2299 & 1) | (SZ53P[value1_2296 & 0xff] | (value1_2296 == 0 ? 0x40 : 0));
          int result_2300 = value1_2296 & 0xFF;
          F = (_F1109 & 0xFF);
          _address1108 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1108 = result_2300;
          write(_address1108, result_2300);
          int read_2301;
          read_2301 = _value1108;
          L = read_2301;
          MEMPTR = _address1108;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int _F1112;
          int _value1111;
          int _address1111;
          contend2x1((PC + 3) & 0xFFFF);
          _address1111 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2302 = read(_address1111, 0);
          contend1x1(_address1111);
          _value1111 = operand_2302;
          int value1_2303 = _value1111;
          int value2_2304 = F;
          _F1112 = value2_2304;
          int rrtemp_2306 = value1_2303;
          value1_2303 = (value1_2303 >> 1) | (_F1112 << 7);
          value1_2303 &= 0xff;
          _F1112 = (rrtemp_2306 & 1) | (SZ53P[value1_2303 & 0xff] | (value1_2303 == 0 ? 0x40 : 0));
          int result_2307 = value1_2303 & 0xFF;
          F = (_F1112 & 0xFF);
          _address1111 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1111 = result_2307;
          write(_address1111, result_2307);
          MEMPTR = _address1111;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F1115;
          int _value1114;
          int _address1114;
          contend2x1((PC + 3) & 0xFFFF);
          _address1114 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2308 = read(_address1114, 0);
          contend1x1(_address1114);
          _value1114 = operand_2308;
          int value1_2309 = _value1114;
          int value2_2310 = F;
          _F1115 = value2_2310;
          int rrtemp_2312 = value1_2309;
          value1_2309 = (value1_2309 >> 1) | (_F1115 << 7);
          value1_2309 &= 0xff;
          _F1115 = (rrtemp_2312 & 1) | (SZ53P[value1_2309 & 0xff] | (value1_2309 == 0 ? 0x40 : 0));
          int result_2313 = value1_2309 & 0xFF;
          F = (_F1115 & 0xFF);
          _address1114 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1114 = result_2313;
          write(_address1114, result_2313);
          int read_2314;
          read_2314 = _value1114;
          A = read_2314;
          MEMPTR = _address1114;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_4(int opcode, int displacement) {
    switch (opcode) {
      case 0x20: {
          int _F1118;
          int _value1117;
          int _address1117;
          contend2x1((PC + 3) & 0xFFFF);
          _address1117 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2315 = read(_address1117, 0);
          contend1x1(_address1117);
          _value1117 = operand_2315;
          int value1_2316 = _value1117;
          _F1118 = value1_2316 >> 7;
          value1_2316 <<= 1;
          value1_2316 &= 0xff;
          _F1118 |= (SZ53P[value1_2316 & 0xff] | (value1_2316 == 0 ? 0x40 : 0));
          int result_2319 = value1_2316 & 0xFF;
          F = (_F1118 & 0xFF);
          _address1117 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1117 = result_2319;
          write(_address1117, result_2319);
          int read_2320;
          read_2320 = _value1117;
          B = read_2320;
          MEMPTR = _address1117;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x21: {
          int _F1121;
          int _value1120;
          int _address1120;
          contend2x1((PC + 3) & 0xFFFF);
          _address1120 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2321 = read(_address1120, 0);
          contend1x1(_address1120);
          _value1120 = operand_2321;
          int value1_2322 = _value1120;
          _F1121 = value1_2322 >> 7;
          value1_2322 <<= 1;
          value1_2322 &= 0xff;
          _F1121 |= (SZ53P[value1_2322 & 0xff] | (value1_2322 == 0 ? 0x40 : 0));
          int result_2325 = value1_2322 & 0xFF;
          F = (_F1121 & 0xFF);
          _address1120 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1120 = result_2325;
          write(_address1120, result_2325);
          int read_2326;
          read_2326 = _value1120;
          C = read_2326;
          MEMPTR = _address1120;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _F1124;
          int _value1123;
          int _address1123;
          contend2x1((PC + 3) & 0xFFFF);
          _address1123 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2327 = read(_address1123, 0);
          contend1x1(_address1123);
          _value1123 = operand_2327;
          int value1_2328 = _value1123;
          _F1124 = value1_2328 >> 7;
          value1_2328 <<= 1;
          value1_2328 &= 0xff;
          _F1124 |= (SZ53P[value1_2328 & 0xff] | (value1_2328 == 0 ? 0x40 : 0));
          int result_2331 = value1_2328 & 0xFF;
          F = (_F1124 & 0xFF);
          _address1123 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1123 = result_2331;
          write(_address1123, result_2331);
          int read_2332;
          read_2332 = _value1123;
          D = read_2332;
          MEMPTR = _address1123;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int _F1127;
          int _value1126;
          int _address1126;
          contend2x1((PC + 3) & 0xFFFF);
          _address1126 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2333 = read(_address1126, 0);
          contend1x1(_address1126);
          _value1126 = operand_2333;
          int value1_2334 = _value1126;
          _F1127 = value1_2334 >> 7;
          value1_2334 <<= 1;
          value1_2334 &= 0xff;
          _F1127 |= (SZ53P[value1_2334 & 0xff] | (value1_2334 == 0 ? 0x40 : 0));
          int result_2337 = value1_2334 & 0xFF;
          F = (_F1127 & 0xFF);
          _address1126 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1126 = result_2337;
          write(_address1126, result_2337);
          int read_2338;
          read_2338 = _value1126;
          E = read_2338;
          MEMPTR = _address1126;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F1130;
          int _value1129;
          int _address1129;
          contend2x1((PC + 3) & 0xFFFF);
          _address1129 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2339 = read(_address1129, 0);
          contend1x1(_address1129);
          _value1129 = operand_2339;
          int value1_2340 = _value1129;
          _F1130 = value1_2340 >> 7;
          value1_2340 <<= 1;
          value1_2340 &= 0xff;
          _F1130 |= (SZ53P[value1_2340 & 0xff] | (value1_2340 == 0 ? 0x40 : 0));
          int result_2343 = value1_2340 & 0xFF;
          F = (_F1130 & 0xFF);
          _address1129 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1129 = result_2343;
          write(_address1129, result_2343);
          int read_2344;
          read_2344 = _value1129;
          H = read_2344;
          MEMPTR = _address1129;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F1133;
          int _value1132;
          int _address1132;
          contend2x1((PC + 3) & 0xFFFF);
          _address1132 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2345 = read(_address1132, 0);
          contend1x1(_address1132);
          _value1132 = operand_2345;
          int value1_2346 = _value1132;
          _F1133 = value1_2346 >> 7;
          value1_2346 <<= 1;
          value1_2346 &= 0xff;
          _F1133 |= (SZ53P[value1_2346 & 0xff] | (value1_2346 == 0 ? 0x40 : 0));
          int result_2349 = value1_2346 & 0xFF;
          F = (_F1133 & 0xFF);
          _address1132 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1132 = result_2349;
          write(_address1132, result_2349);
          int read_2350;
          read_2350 = _value1132;
          L = read_2350;
          MEMPTR = _address1132;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x26: {
          int _F1136;
          int _value1135;
          int _address1135;
          contend2x1((PC + 3) & 0xFFFF);
          _address1135 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2351 = read(_address1135, 0);
          contend1x1(_address1135);
          _value1135 = operand_2351;
          int value1_2352 = _value1135;
          _F1136 = value1_2352 >> 7;
          value1_2352 <<= 1;
          value1_2352 &= 0xff;
          _F1136 |= (SZ53P[value1_2352 & 0xff] | (value1_2352 == 0 ? 0x40 : 0));
          int result_2355 = value1_2352 & 0xFF;
          F = (_F1136 & 0xFF);
          _address1135 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1135 = result_2355;
          write(_address1135, result_2355);
          MEMPTR = _address1135;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F1139;
          int _value1138;
          int _address1138;
          contend2x1((PC + 3) & 0xFFFF);
          _address1138 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2356 = read(_address1138, 0);
          contend1x1(_address1138);
          _value1138 = operand_2356;
          int value1_2357 = _value1138;
          _F1139 = value1_2357 >> 7;
          value1_2357 <<= 1;
          value1_2357 &= 0xff;
          _F1139 |= (SZ53P[value1_2357 & 0xff] | (value1_2357 == 0 ? 0x40 : 0));
          int result_2360 = value1_2357 & 0xFF;
          F = (_F1139 & 0xFF);
          _address1138 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1138 = result_2360;
          write(_address1138, result_2360);
          int read_2361;
          read_2361 = _value1138;
          A = read_2361;
          MEMPTR = _address1138;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_5(int opcode, int displacement) {
    switch (opcode) {
      case 0x28: {
          int _F1142;
          int _value1141;
          int _address1141;
          contend2x1((PC + 3) & 0xFFFF);
          _address1141 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2362 = read(_address1141, 0);
          contend1x1(_address1141);
          _value1141 = operand_2362;
          int value1_2363 = _value1141;
          _F1142 = value1_2363 & 1;
          value1_2363 = (value1_2363 & 0x80) | (value1_2363 >> 1);
          value1_2363 &= 0xff;
          _F1142 |= (SZ53P[value1_2363 & 0xff] | (value1_2363 == 0 ? 0x40 : 0));
          int result_2366 = value1_2363 & 0xFF;
          F = (_F1142 & 0xFF);
          _address1141 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1141 = result_2366;
          write(_address1141, result_2366);
          int read_2367;
          read_2367 = _value1141;
          B = read_2367;
          MEMPTR = _address1141;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x29: {
          int _F1145;
          int _value1144;
          int _address1144;
          contend2x1((PC + 3) & 0xFFFF);
          _address1144 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2368 = read(_address1144, 0);
          contend1x1(_address1144);
          _value1144 = operand_2368;
          int value1_2369 = _value1144;
          _F1145 = value1_2369 & 1;
          value1_2369 = (value1_2369 & 0x80) | (value1_2369 >> 1);
          value1_2369 &= 0xff;
          _F1145 |= (SZ53P[value1_2369 & 0xff] | (value1_2369 == 0 ? 0x40 : 0));
          int result_2372 = value1_2369 & 0xFF;
          F = (_F1145 & 0xFF);
          _address1144 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1144 = result_2372;
          write(_address1144, result_2372);
          int read_2373;
          read_2373 = _value1144;
          C = read_2373;
          MEMPTR = _address1144;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _F1148;
          int _value1147;
          int _address1147;
          contend2x1((PC + 3) & 0xFFFF);
          _address1147 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2374 = read(_address1147, 0);
          contend1x1(_address1147);
          _value1147 = operand_2374;
          int value1_2375 = _value1147;
          _F1148 = value1_2375 & 1;
          value1_2375 = (value1_2375 & 0x80) | (value1_2375 >> 1);
          value1_2375 &= 0xff;
          _F1148 |= (SZ53P[value1_2375 & 0xff] | (value1_2375 == 0 ? 0x40 : 0));
          int result_2378 = value1_2375 & 0xFF;
          F = (_F1148 & 0xFF);
          _address1147 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1147 = result_2378;
          write(_address1147, result_2378);
          int read_2379;
          read_2379 = _value1147;
          D = read_2379;
          MEMPTR = _address1147;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2B: {
          int _F1151;
          int _value1150;
          int _address1150;
          contend2x1((PC + 3) & 0xFFFF);
          _address1150 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2380 = read(_address1150, 0);
          contend1x1(_address1150);
          _value1150 = operand_2380;
          int value1_2381 = _value1150;
          _F1151 = value1_2381 & 1;
          value1_2381 = (value1_2381 & 0x80) | (value1_2381 >> 1);
          value1_2381 &= 0xff;
          _F1151 |= (SZ53P[value1_2381 & 0xff] | (value1_2381 == 0 ? 0x40 : 0));
          int result_2384 = value1_2381 & 0xFF;
          F = (_F1151 & 0xFF);
          _address1150 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1150 = result_2384;
          write(_address1150, result_2384);
          int read_2385;
          read_2385 = _value1150;
          E = read_2385;
          MEMPTR = _address1150;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F1154;
          int _value1153;
          int _address1153;
          contend2x1((PC + 3) & 0xFFFF);
          _address1153 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2386 = read(_address1153, 0);
          contend1x1(_address1153);
          _value1153 = operand_2386;
          int value1_2387 = _value1153;
          _F1154 = value1_2387 & 1;
          value1_2387 = (value1_2387 & 0x80) | (value1_2387 >> 1);
          value1_2387 &= 0xff;
          _F1154 |= (SZ53P[value1_2387 & 0xff] | (value1_2387 == 0 ? 0x40 : 0));
          int result_2390 = value1_2387 & 0xFF;
          F = (_F1154 & 0xFF);
          _address1153 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1153 = result_2390;
          write(_address1153, result_2390);
          int read_2391;
          read_2391 = _value1153;
          H = read_2391;
          MEMPTR = _address1153;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F1157;
          int _value1156;
          int _address1156;
          contend2x1((PC + 3) & 0xFFFF);
          _address1156 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2392 = read(_address1156, 0);
          contend1x1(_address1156);
          _value1156 = operand_2392;
          int value1_2393 = _value1156;
          _F1157 = value1_2393 & 1;
          value1_2393 = (value1_2393 & 0x80) | (value1_2393 >> 1);
          value1_2393 &= 0xff;
          _F1157 |= (SZ53P[value1_2393 & 0xff] | (value1_2393 == 0 ? 0x40 : 0));
          int result_2396 = value1_2393 & 0xFF;
          F = (_F1157 & 0xFF);
          _address1156 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1156 = result_2396;
          write(_address1156, result_2396);
          int read_2397;
          read_2397 = _value1156;
          L = read_2397;
          MEMPTR = _address1156;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int _F1160;
          int _value1159;
          int _address1159;
          contend2x1((PC + 3) & 0xFFFF);
          _address1159 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2398 = read(_address1159, 0);
          contend1x1(_address1159);
          _value1159 = operand_2398;
          int value1_2399 = _value1159;
          _F1160 = value1_2399 & 1;
          value1_2399 = (value1_2399 & 0x80) | (value1_2399 >> 1);
          value1_2399 &= 0xff;
          _F1160 |= (SZ53P[value1_2399 & 0xff] | (value1_2399 == 0 ? 0x40 : 0));
          int result_2402 = value1_2399 & 0xFF;
          F = (_F1160 & 0xFF);
          _address1159 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1159 = result_2402;
          write(_address1159, result_2402);
          MEMPTR = _address1159;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F1163;
          int _value1162;
          int _address1162;
          contend2x1((PC + 3) & 0xFFFF);
          _address1162 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2403 = read(_address1162, 0);
          contend1x1(_address1162);
          _value1162 = operand_2403;
          int value1_2404 = _value1162;
          _F1163 = value1_2404 & 1;
          value1_2404 = (value1_2404 & 0x80) | (value1_2404 >> 1);
          value1_2404 &= 0xff;
          _F1163 |= (SZ53P[value1_2404 & 0xff] | (value1_2404 == 0 ? 0x40 : 0));
          int result_2407 = value1_2404 & 0xFF;
          F = (_F1163 & 0xFF);
          _address1162 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1162 = result_2407;
          write(_address1162, result_2407);
          int read_2408;
          read_2408 = _value1162;
          A = read_2408;
          MEMPTR = _address1162;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_6(int opcode, int displacement) {
    switch (opcode) {
      case 0x30: {
          int _F1166;
          int _value1165;
          int _address1165;
          contend2x1((PC + 3) & 0xFFFF);
          _address1165 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2409 = read(_address1165, 0);
          contend1x1(_address1165);
          _value1165 = operand_2409;
          int value1_2410 = _value1165;
          _F1166 = value1_2410 >> 7;
          value1_2410 = (value1_2410 << 1) | 0x01;
          value1_2410 &= 0xff;
          _F1166 |= (SZ53P[value1_2410 & 0xff] | (value1_2410 == 0 ? 0x40 : 0));
          int result_2413 = value1_2410 & 0xFF;
          F = (_F1166 & 0xFF);
          _address1165 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1165 = result_2413;
          write(_address1165, result_2413);
          int read_2414;
          read_2414 = _value1165;
          B = read_2414;
          MEMPTR = _address1165;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x31: {
          int _F1169;
          int _value1168;
          int _address1168;
          contend2x1((PC + 3) & 0xFFFF);
          _address1168 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2415 = read(_address1168, 0);
          contend1x1(_address1168);
          _value1168 = operand_2415;
          int value1_2416 = _value1168;
          _F1169 = value1_2416 >> 7;
          value1_2416 = (value1_2416 << 1) | 0x01;
          value1_2416 &= 0xff;
          _F1169 |= (SZ53P[value1_2416 & 0xff] | (value1_2416 == 0 ? 0x40 : 0));
          int result_2419 = value1_2416 & 0xFF;
          F = (_F1169 & 0xFF);
          _address1168 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1168 = result_2419;
          write(_address1168, result_2419);
          int read_2420;
          read_2420 = _value1168;
          C = read_2420;
          MEMPTR = _address1168;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _F1172;
          int _value1171;
          int _address1171;
          contend2x1((PC + 3) & 0xFFFF);
          _address1171 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2421 = read(_address1171, 0);
          contend1x1(_address1171);
          _value1171 = operand_2421;
          int value1_2422 = _value1171;
          _F1172 = value1_2422 >> 7;
          value1_2422 = (value1_2422 << 1) | 0x01;
          value1_2422 &= 0xff;
          _F1172 |= (SZ53P[value1_2422 & 0xff] | (value1_2422 == 0 ? 0x40 : 0));
          int result_2425 = value1_2422 & 0xFF;
          F = (_F1172 & 0xFF);
          _address1171 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1171 = result_2425;
          write(_address1171, result_2425);
          int read_2426;
          read_2426 = _value1171;
          D = read_2426;
          MEMPTR = _address1171;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int _F1175;
          int _value1174;
          int _address1174;
          contend2x1((PC + 3) & 0xFFFF);
          _address1174 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2427 = read(_address1174, 0);
          contend1x1(_address1174);
          _value1174 = operand_2427;
          int value1_2428 = _value1174;
          _F1175 = value1_2428 >> 7;
          value1_2428 = (value1_2428 << 1) | 0x01;
          value1_2428 &= 0xff;
          _F1175 |= (SZ53P[value1_2428 & 0xff] | (value1_2428 == 0 ? 0x40 : 0));
          int result_2431 = value1_2428 & 0xFF;
          F = (_F1175 & 0xFF);
          _address1174 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1174 = result_2431;
          write(_address1174, result_2431);
          int read_2432;
          read_2432 = _value1174;
          E = read_2432;
          MEMPTR = _address1174;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F1178;
          int _value1177;
          int _address1177;
          contend2x1((PC + 3) & 0xFFFF);
          _address1177 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2433 = read(_address1177, 0);
          contend1x1(_address1177);
          _value1177 = operand_2433;
          int value1_2434 = _value1177;
          _F1178 = value1_2434 >> 7;
          value1_2434 = (value1_2434 << 1) | 0x01;
          value1_2434 &= 0xff;
          _F1178 |= (SZ53P[value1_2434 & 0xff] | (value1_2434 == 0 ? 0x40 : 0));
          int result_2437 = value1_2434 & 0xFF;
          F = (_F1178 & 0xFF);
          _address1177 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1177 = result_2437;
          write(_address1177, result_2437);
          int read_2438;
          read_2438 = _value1177;
          H = read_2438;
          MEMPTR = _address1177;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F1181;
          int _value1180;
          int _address1180;
          contend2x1((PC + 3) & 0xFFFF);
          _address1180 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2439 = read(_address1180, 0);
          contend1x1(_address1180);
          _value1180 = operand_2439;
          int value1_2440 = _value1180;
          _F1181 = value1_2440 >> 7;
          value1_2440 = (value1_2440 << 1) | 0x01;
          value1_2440 &= 0xff;
          _F1181 |= (SZ53P[value1_2440 & 0xff] | (value1_2440 == 0 ? 0x40 : 0));
          int result_2443 = value1_2440 & 0xFF;
          F = (_F1181 & 0xFF);
          _address1180 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1180 = result_2443;
          write(_address1180, result_2443);
          int read_2444;
          read_2444 = _value1180;
          L = read_2444;
          MEMPTR = _address1180;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _F1184;
          int _value1183;
          int _address1183;
          contend2x1((PC + 3) & 0xFFFF);
          _address1183 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2445 = read(_address1183, 0);
          contend1x1(_address1183);
          _value1183 = operand_2445;
          int value1_2446 = _value1183;
          _F1184 = value1_2446 >> 7;
          value1_2446 = (value1_2446 << 1) | 0x01;
          value1_2446 &= 0xff;
          _F1184 |= (SZ53P[value1_2446 & 0xff] | (value1_2446 == 0 ? 0x40 : 0));
          int result_2449 = value1_2446 & 0xFF;
          F = (_F1184 & 0xFF);
          _address1183 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1183 = result_2449;
          write(_address1183, result_2449);
          MEMPTR = _address1183;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F1187;
          int _value1186;
          int _address1186;
          contend2x1((PC + 3) & 0xFFFF);
          _address1186 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2450 = read(_address1186, 0);
          contend1x1(_address1186);
          _value1186 = operand_2450;
          int value1_2451 = _value1186;
          _F1187 = value1_2451 >> 7;
          value1_2451 = (value1_2451 << 1) | 0x01;
          value1_2451 &= 0xff;
          _F1187 |= (SZ53P[value1_2451 & 0xff] | (value1_2451 == 0 ? 0x40 : 0));
          int result_2454 = value1_2451 & 0xFF;
          F = (_F1187 & 0xFF);
          _address1186 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1186 = result_2454;
          write(_address1186, result_2454);
          int read_2455;
          read_2455 = _value1186;
          A = read_2455;
          MEMPTR = _address1186;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_7(int opcode, int displacement) {
    switch (opcode) {
      case 0x38: {
          int _F1190;
          int _value1189;
          int _address1189;
          contend2x1((PC + 3) & 0xFFFF);
          _address1189 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2456 = read(_address1189, 0);
          contend1x1(_address1189);
          _value1189 = operand_2456;
          int value1_2457 = _value1189;
          _F1190 = value1_2457 & 1;
          value1_2457 >>= 1;
          value1_2457 &= 0xff;
          _F1190 |= (SZ53P[value1_2457 & 0xff] | (value1_2457 == 0 ? 0x40 : 0));
          int result_2460 = value1_2457 & 0xFF;
          F = (_F1190 & 0xFF);
          _address1189 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1189 = result_2460;
          write(_address1189, result_2460);
          int read_2461;
          read_2461 = _value1189;
          B = read_2461;
          MEMPTR = _address1189;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x39: {
          int _F1193;
          int _value1192;
          int _address1192;
          contend2x1((PC + 3) & 0xFFFF);
          _address1192 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2462 = read(_address1192, 0);
          contend1x1(_address1192);
          _value1192 = operand_2462;
          int value1_2463 = _value1192;
          _F1193 = value1_2463 & 1;
          value1_2463 >>= 1;
          value1_2463 &= 0xff;
          _F1193 |= (SZ53P[value1_2463 & 0xff] | (value1_2463 == 0 ? 0x40 : 0));
          int result_2466 = value1_2463 & 0xFF;
          F = (_F1193 & 0xFF);
          _address1192 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1192 = result_2466;
          write(_address1192, result_2466);
          int read_2467;
          read_2467 = _value1192;
          C = read_2467;
          MEMPTR = _address1192;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _F1196;
          int _value1195;
          int _address1195;
          contend2x1((PC + 3) & 0xFFFF);
          _address1195 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2468 = read(_address1195, 0);
          contend1x1(_address1195);
          _value1195 = operand_2468;
          int value1_2469 = _value1195;
          _F1196 = value1_2469 & 1;
          value1_2469 >>= 1;
          value1_2469 &= 0xff;
          _F1196 |= (SZ53P[value1_2469 & 0xff] | (value1_2469 == 0 ? 0x40 : 0));
          int result_2472 = value1_2469 & 0xFF;
          F = (_F1196 & 0xFF);
          _address1195 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1195 = result_2472;
          write(_address1195, result_2472);
          int read_2473;
          read_2473 = _value1195;
          D = read_2473;
          MEMPTR = _address1195;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3B: {
          int _F1199;
          int _value1198;
          int _address1198;
          contend2x1((PC + 3) & 0xFFFF);
          _address1198 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2474 = read(_address1198, 0);
          contend1x1(_address1198);
          _value1198 = operand_2474;
          int value1_2475 = _value1198;
          _F1199 = value1_2475 & 1;
          value1_2475 >>= 1;
          value1_2475 &= 0xff;
          _F1199 |= (SZ53P[value1_2475 & 0xff] | (value1_2475 == 0 ? 0x40 : 0));
          int result_2478 = value1_2475 & 0xFF;
          F = (_F1199 & 0xFF);
          _address1198 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1198 = result_2478;
          write(_address1198, result_2478);
          int read_2479;
          read_2479 = _value1198;
          E = read_2479;
          MEMPTR = _address1198;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F1202;
          int _value1201;
          int _address1201;
          contend2x1((PC + 3) & 0xFFFF);
          _address1201 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2480 = read(_address1201, 0);
          contend1x1(_address1201);
          _value1201 = operand_2480;
          int value1_2481 = _value1201;
          _F1202 = value1_2481 & 1;
          value1_2481 >>= 1;
          value1_2481 &= 0xff;
          _F1202 |= (SZ53P[value1_2481 & 0xff] | (value1_2481 == 0 ? 0x40 : 0));
          int result_2484 = value1_2481 & 0xFF;
          F = (_F1202 & 0xFF);
          _address1201 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1201 = result_2484;
          write(_address1201, result_2484);
          int read_2485;
          read_2485 = _value1201;
          H = read_2485;
          MEMPTR = _address1201;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F1205;
          int _value1204;
          int _address1204;
          contend2x1((PC + 3) & 0xFFFF);
          _address1204 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2486 = read(_address1204, 0);
          contend1x1(_address1204);
          _value1204 = operand_2486;
          int value1_2487 = _value1204;
          _F1205 = value1_2487 & 1;
          value1_2487 >>= 1;
          value1_2487 &= 0xff;
          _F1205 |= (SZ53P[value1_2487 & 0xff] | (value1_2487 == 0 ? 0x40 : 0));
          int result_2490 = value1_2487 & 0xFF;
          F = (_F1205 & 0xFF);
          _address1204 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1204 = result_2490;
          write(_address1204, result_2490);
          int read_2491;
          read_2491 = _value1204;
          L = read_2491;
          MEMPTR = _address1204;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int _F1208;
          int _value1207;
          int _address1207;
          contend2x1((PC + 3) & 0xFFFF);
          _address1207 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2492 = read(_address1207, 0);
          contend1x1(_address1207);
          _value1207 = operand_2492;
          int value1_2493 = _value1207;
          _F1208 = value1_2493 & 1;
          value1_2493 >>= 1;
          value1_2493 &= 0xff;
          _F1208 |= (SZ53P[value1_2493 & 0xff] | (value1_2493 == 0 ? 0x40 : 0));
          int result_2496 = value1_2493 & 0xFF;
          F = (_F1208 & 0xFF);
          _address1207 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1207 = result_2496;
          write(_address1207, result_2496);
          MEMPTR = _address1207;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F1211;
          int _value1210;
          int _address1210;
          contend2x1((PC + 3) & 0xFFFF);
          _address1210 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2497 = read(_address1210, 0);
          contend1x1(_address1210);
          _value1210 = operand_2497;
          int value1_2498 = _value1210;
          _F1211 = value1_2498 & 1;
          value1_2498 >>= 1;
          value1_2498 &= 0xff;
          _F1211 |= (SZ53P[value1_2498 & 0xff] | (value1_2498 == 0 ? 0x40 : 0));
          int result_2501 = value1_2498 & 0xFF;
          F = (_F1211 & 0xFF);
          _address1210 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1210 = result_2501;
          write(_address1210, result_2501);
          int read_2502;
          read_2502 = _value1210;
          A = read_2502;
          MEMPTR = _address1210;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_8(int opcode, int displacement) {
    switch (opcode) {
      case 0x40: {
          int _F1214;
          int _value1213;
          int _address1213;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2503;
          address_2503 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2504 = F & 1;
          _address1213 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2505 = read(_address1213, 0);
          contend1x1(_address1213);
          _value1213 = operand_2505;
          int value3_2508 = nAndCarry_2504;
          _F1214 = value3_2508;
          value3_2508 = value3_2508 >>> 1;
          _F1214 = (_F1214 & 1) | 0x10 | (address_2503 & 0x28);
          if ((_value1213 & (0x01 << value3_2508)) == 0) {
              _F1214 |= 0x44;
          }
          if (value3_2508 == 7 && (_value1213 & 0x80) != 0) {
              _F1214 |= 0x80;
          }
          F = (_F1214 & 0xFF);
          MEMPTR = _address1213;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x41: {
          int _F1217;
          int _value1216;
          int _address1216;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2510;
          address_2510 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2511 = F & 1;
          _address1216 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2512 = read(_address1216, 0);
          contend1x1(_address1216);
          _value1216 = operand_2512;
          int value3_2515 = nAndCarry_2511;
          _F1217 = value3_2515;
          value3_2515 = value3_2515 >>> 1;
          _F1217 = (_F1217 & 1) | 0x10 | (address_2510 & 0x28);
          if ((_value1216 & (0x01 << value3_2515)) == 0) {
              _F1217 |= 0x44;
          }
          if (value3_2515 == 7 && (_value1216 & 0x80) != 0) {
              _F1217 |= 0x80;
          }
          F = (_F1217 & 0xFF);
          MEMPTR = _address1216;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x42: {
          int _F1220;
          int _value1219;
          int _address1219;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2517;
          address_2517 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2518 = F & 1;
          _address1219 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2519 = read(_address1219, 0);
          contend1x1(_address1219);
          _value1219 = operand_2519;
          int value3_2522 = nAndCarry_2518;
          _F1220 = value3_2522;
          value3_2522 = value3_2522 >>> 1;
          _F1220 = (_F1220 & 1) | 0x10 | (address_2517 & 0x28);
          if ((_value1219 & (0x01 << value3_2522)) == 0) {
              _F1220 |= 0x44;
          }
          if (value3_2522 == 7 && (_value1219 & 0x80) != 0) {
              _F1220 |= 0x80;
          }
          F = (_F1220 & 0xFF);
          MEMPTR = _address1219;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x43: {
          int _F1223;
          int _value1222;
          int _address1222;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2524;
          address_2524 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2525 = F & 1;
          _address1222 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2526 = read(_address1222, 0);
          contend1x1(_address1222);
          _value1222 = operand_2526;
          int value3_2529 = nAndCarry_2525;
          _F1223 = value3_2529;
          value3_2529 = value3_2529 >>> 1;
          _F1223 = (_F1223 & 1) | 0x10 | (address_2524 & 0x28);
          if ((_value1222 & (0x01 << value3_2529)) == 0) {
              _F1223 |= 0x44;
          }
          if (value3_2529 == 7 && (_value1222 & 0x80) != 0) {
              _F1223 |= 0x80;
          }
          F = (_F1223 & 0xFF);
          MEMPTR = _address1222;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x44: {
          int _F1226;
          int _value1225;
          int _address1225;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2531;
          address_2531 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2532 = F & 1;
          _address1225 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2533 = read(_address1225, 0);
          contend1x1(_address1225);
          _value1225 = operand_2533;
          int value3_2536 = nAndCarry_2532;
          _F1226 = value3_2536;
          value3_2536 = value3_2536 >>> 1;
          _F1226 = (_F1226 & 1) | 0x10 | (address_2531 & 0x28);
          if ((_value1225 & (0x01 << value3_2536)) == 0) {
              _F1226 |= 0x44;
          }
          if (value3_2536 == 7 && (_value1225 & 0x80) != 0) {
              _F1226 |= 0x80;
          }
          F = (_F1226 & 0xFF);
          MEMPTR = _address1225;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x45: {
          int _F1229;
          int _value1228;
          int _address1228;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2538;
          address_2538 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2539 = F & 1;
          _address1228 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2540 = read(_address1228, 0);
          contend1x1(_address1228);
          _value1228 = operand_2540;
          int value3_2543 = nAndCarry_2539;
          _F1229 = value3_2543;
          value3_2543 = value3_2543 >>> 1;
          _F1229 = (_F1229 & 1) | 0x10 | (address_2538 & 0x28);
          if ((_value1228 & (0x01 << value3_2543)) == 0) {
              _F1229 |= 0x44;
          }
          if (value3_2543 == 7 && (_value1228 & 0x80) != 0) {
              _F1229 |= 0x80;
          }
          F = (_F1229 & 0xFF);
          MEMPTR = _address1228;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _F1232;
          int _value1231;
          int _address1231;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2545;
          address_2545 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2546 = F & 1;
          _address1231 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2547 = read(_address1231, 0);
          contend1x1(_address1231);
          _value1231 = operand_2547;
          int value3_2550 = nAndCarry_2546;
          _F1232 = value3_2550;
          value3_2550 = value3_2550 >>> 1;
          _F1232 = (_F1232 & 1) | 0x10 | (address_2545 & 0x28);
          if ((_value1231 & (0x01 << value3_2550)) == 0) {
              _F1232 |= 0x44;
          }
          if (value3_2550 == 7 && (_value1231 & 0x80) != 0) {
              _F1232 |= 0x80;
          }
          F = (_F1232 & 0xFF);
          MEMPTR = _address1231;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x47: {
          int _F1235;
          int _value1234;
          int _address1234;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2552;
          address_2552 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2553 = F & 1;
          _address1234 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2554 = read(_address1234, 0);
          contend1x1(_address1234);
          _value1234 = operand_2554;
          int value3_2557 = nAndCarry_2553;
          _F1235 = value3_2557;
          value3_2557 = value3_2557 >>> 1;
          _F1235 = (_F1235 & 1) | 0x10 | (address_2552 & 0x28);
          if ((_value1234 & (0x01 << value3_2557)) == 0) {
              _F1235 |= 0x44;
          }
          if (value3_2557 == 7 && (_value1234 & 0x80) != 0) {
              _F1235 |= 0x80;
          }
          F = (_F1235 & 0xFF);
          MEMPTR = _address1234;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_9(int opcode, int displacement) {
    switch (opcode) {
      case 0x48: {
          int _F1238;
          int _value1237;
          int _address1237;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2559;
          address_2559 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2560 = 2 | F & 1;
          _address1237 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2561 = read(_address1237, 0);
          contend1x1(_address1237);
          _value1237 = operand_2561;
          int value3_2564 = nAndCarry_2560;
          _F1238 = value3_2564 & 1;
          value3_2564 = value3_2564 >>> 1;
          _F1238 = (_F1238 & 1) | 0x10 | (address_2559 & 0x28);
          if ((_value1237 & (0x01 << value3_2564)) == 0) {
              _F1238 |= 0x44;
          }
          if (value3_2564 == 7 && (_value1237 & 0x80) != 0) {
              _F1238 |= 0x80;
          }
          F = (_F1238 & 0xFF);
          MEMPTR = _address1237;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x49: {
          int _F1241;
          int _value1240;
          int _address1240;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2566;
          address_2566 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2567 = 2 | F & 1;
          _address1240 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2568 = read(_address1240, 0);
          contend1x1(_address1240);
          _value1240 = operand_2568;
          int value3_2571 = nAndCarry_2567;
          _F1241 = value3_2571 & 1;
          value3_2571 = value3_2571 >>> 1;
          _F1241 = (_F1241 & 1) | 0x10 | (address_2566 & 0x28);
          if ((_value1240 & (0x01 << value3_2571)) == 0) {
              _F1241 |= 0x44;
          }
          if (value3_2571 == 7 && (_value1240 & 0x80) != 0) {
              _F1241 |= 0x80;
          }
          F = (_F1241 & 0xFF);
          MEMPTR = _address1240;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4A: {
          int _F1244;
          int _value1243;
          int _address1243;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2573;
          address_2573 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2574 = 2 | F & 1;
          _address1243 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2575 = read(_address1243, 0);
          contend1x1(_address1243);
          _value1243 = operand_2575;
          int value3_2578 = nAndCarry_2574;
          _F1244 = value3_2578 & 1;
          value3_2578 = value3_2578 >>> 1;
          _F1244 = (_F1244 & 1) | 0x10 | (address_2573 & 0x28);
          if ((_value1243 & (0x01 << value3_2578)) == 0) {
              _F1244 |= 0x44;
          }
          if (value3_2578 == 7 && (_value1243 & 0x80) != 0) {
              _F1244 |= 0x80;
          }
          F = (_F1244 & 0xFF);
          MEMPTR = _address1243;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4B: {
          int _F1247;
          int _value1246;
          int _address1246;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2580;
          address_2580 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2581 = 2 | F & 1;
          _address1246 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2582 = read(_address1246, 0);
          contend1x1(_address1246);
          _value1246 = operand_2582;
          int value3_2585 = nAndCarry_2581;
          _F1247 = value3_2585 & 1;
          value3_2585 = value3_2585 >>> 1;
          _F1247 = (_F1247 & 1) | 0x10 | (address_2580 & 0x28);
          if ((_value1246 & (0x01 << value3_2585)) == 0) {
              _F1247 |= 0x44;
          }
          if (value3_2585 == 7 && (_value1246 & 0x80) != 0) {
              _F1247 |= 0x80;
          }
          F = (_F1247 & 0xFF);
          MEMPTR = _address1246;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4C: {
          int _F1250;
          int _value1249;
          int _address1249;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2587;
          address_2587 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2588 = 2 | F & 1;
          _address1249 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2589 = read(_address1249, 0);
          contend1x1(_address1249);
          _value1249 = operand_2589;
          int value3_2592 = nAndCarry_2588;
          _F1250 = value3_2592 & 1;
          value3_2592 = value3_2592 >>> 1;
          _F1250 = (_F1250 & 1) | 0x10 | (address_2587 & 0x28);
          if ((_value1249 & (0x01 << value3_2592)) == 0) {
              _F1250 |= 0x44;
          }
          if (value3_2592 == 7 && (_value1249 & 0x80) != 0) {
              _F1250 |= 0x80;
          }
          F = (_F1250 & 0xFF);
          MEMPTR = _address1249;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4D: {
          int _F1253;
          int _value1252;
          int _address1252;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2594;
          address_2594 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2595 = 2 | F & 1;
          _address1252 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2596 = read(_address1252, 0);
          contend1x1(_address1252);
          _value1252 = operand_2596;
          int value3_2599 = nAndCarry_2595;
          _F1253 = value3_2599 & 1;
          value3_2599 = value3_2599 >>> 1;
          _F1253 = (_F1253 & 1) | 0x10 | (address_2594 & 0x28);
          if ((_value1252 & (0x01 << value3_2599)) == 0) {
              _F1253 |= 0x44;
          }
          if (value3_2599 == 7 && (_value1252 & 0x80) != 0) {
              _F1253 |= 0x80;
          }
          F = (_F1253 & 0xFF);
          MEMPTR = _address1252;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _F1256;
          int _value1255;
          int _address1255;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2601;
          address_2601 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2602 = 2 | F & 1;
          _address1255 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2603 = read(_address1255, 0);
          contend1x1(_address1255);
          _value1255 = operand_2603;
          int value3_2606 = nAndCarry_2602;
          _F1256 = value3_2606 & 1;
          value3_2606 = value3_2606 >>> 1;
          _F1256 = (_F1256 & 1) | 0x10 | (address_2601 & 0x28);
          if ((_value1255 & (0x01 << value3_2606)) == 0) {
              _F1256 |= 0x44;
          }
          if (value3_2606 == 7 && (_value1255 & 0x80) != 0) {
              _F1256 |= 0x80;
          }
          F = (_F1256 & 0xFF);
          MEMPTR = _address1255;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4F: {
          int _F1259;
          int _value1258;
          int _address1258;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2608;
          address_2608 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2609 = 2 | F & 1;
          _address1258 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2610 = read(_address1258, 0);
          contend1x1(_address1258);
          _value1258 = operand_2610;
          int value3_2613 = nAndCarry_2609;
          _F1259 = value3_2613 & 1;
          value3_2613 = value3_2613 >>> 1;
          _F1259 = (_F1259 & 1) | 0x10 | (address_2608 & 0x28);
          if ((_value1258 & (0x01 << value3_2613)) == 0) {
              _F1259 |= 0x44;
          }
          if (value3_2613 == 7 && (_value1258 & 0x80) != 0) {
              _F1259 |= 0x80;
          }
          F = (_F1259 & 0xFF);
          MEMPTR = _address1258;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_10(int opcode, int displacement) {
    switch (opcode) {
      case 0x50: {
          int _F1262;
          int _value1261;
          int _address1261;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2615;
          address_2615 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2616 = 4 | F & 1;
          _address1261 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2617 = read(_address1261, 0);
          contend1x1(_address1261);
          _value1261 = operand_2617;
          int value3_2620 = nAndCarry_2616;
          _F1262 = value3_2620 & 1;
          value3_2620 = value3_2620 >>> 1;
          _F1262 = (_F1262 & 1) | 0x10 | (address_2615 & 0x28);
          if ((_value1261 & (0x01 << value3_2620)) == 0) {
              _F1262 |= 0x44;
          }
          if (value3_2620 == 7 && (_value1261 & 0x80) != 0) {
              _F1262 |= 0x80;
          }
          F = (_F1262 & 0xFF);
          MEMPTR = _address1261;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x51: {
          int _F1265;
          int _value1264;
          int _address1264;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2622;
          address_2622 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2623 = 4 | F & 1;
          _address1264 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2624 = read(_address1264, 0);
          contend1x1(_address1264);
          _value1264 = operand_2624;
          int value3_2627 = nAndCarry_2623;
          _F1265 = value3_2627 & 1;
          value3_2627 = value3_2627 >>> 1;
          _F1265 = (_F1265 & 1) | 0x10 | (address_2622 & 0x28);
          if ((_value1264 & (0x01 << value3_2627)) == 0) {
              _F1265 |= 0x44;
          }
          if (value3_2627 == 7 && (_value1264 & 0x80) != 0) {
              _F1265 |= 0x80;
          }
          F = (_F1265 & 0xFF);
          MEMPTR = _address1264;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x52: {
          int _F1268;
          int _value1267;
          int _address1267;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2629;
          address_2629 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2630 = 4 | F & 1;
          _address1267 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2631 = read(_address1267, 0);
          contend1x1(_address1267);
          _value1267 = operand_2631;
          int value3_2634 = nAndCarry_2630;
          _F1268 = value3_2634 & 1;
          value3_2634 = value3_2634 >>> 1;
          _F1268 = (_F1268 & 1) | 0x10 | (address_2629 & 0x28);
          if ((_value1267 & (0x01 << value3_2634)) == 0) {
              _F1268 |= 0x44;
          }
          if (value3_2634 == 7 && (_value1267 & 0x80) != 0) {
              _F1268 |= 0x80;
          }
          F = (_F1268 & 0xFF);
          MEMPTR = _address1267;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x53: {
          int _F1271;
          int _value1270;
          int _address1270;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2636;
          address_2636 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2637 = 4 | F & 1;
          _address1270 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2638 = read(_address1270, 0);
          contend1x1(_address1270);
          _value1270 = operand_2638;
          int value3_2641 = nAndCarry_2637;
          _F1271 = value3_2641 & 1;
          value3_2641 = value3_2641 >>> 1;
          _F1271 = (_F1271 & 1) | 0x10 | (address_2636 & 0x28);
          if ((_value1270 & (0x01 << value3_2641)) == 0) {
              _F1271 |= 0x44;
          }
          if (value3_2641 == 7 && (_value1270 & 0x80) != 0) {
              _F1271 |= 0x80;
          }
          F = (_F1271 & 0xFF);
          MEMPTR = _address1270;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x54: {
          int _F1274;
          int _value1273;
          int _address1273;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2643;
          address_2643 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2644 = 4 | F & 1;
          _address1273 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2645 = read(_address1273, 0);
          contend1x1(_address1273);
          _value1273 = operand_2645;
          int value3_2648 = nAndCarry_2644;
          _F1274 = value3_2648 & 1;
          value3_2648 = value3_2648 >>> 1;
          _F1274 = (_F1274 & 1) | 0x10 | (address_2643 & 0x28);
          if ((_value1273 & (0x01 << value3_2648)) == 0) {
              _F1274 |= 0x44;
          }
          if (value3_2648 == 7 && (_value1273 & 0x80) != 0) {
              _F1274 |= 0x80;
          }
          F = (_F1274 & 0xFF);
          MEMPTR = _address1273;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x55: {
          int _F1277;
          int _value1276;
          int _address1276;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2650;
          address_2650 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2651 = 4 | F & 1;
          _address1276 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2652 = read(_address1276, 0);
          contend1x1(_address1276);
          _value1276 = operand_2652;
          int value3_2655 = nAndCarry_2651;
          _F1277 = value3_2655 & 1;
          value3_2655 = value3_2655 >>> 1;
          _F1277 = (_F1277 & 1) | 0x10 | (address_2650 & 0x28);
          if ((_value1276 & (0x01 << value3_2655)) == 0) {
              _F1277 |= 0x44;
          }
          if (value3_2655 == 7 && (_value1276 & 0x80) != 0) {
              _F1277 |= 0x80;
          }
          F = (_F1277 & 0xFF);
          MEMPTR = _address1276;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _F1280;
          int _value1279;
          int _address1279;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2657;
          address_2657 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2658 = 4 | F & 1;
          _address1279 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2659 = read(_address1279, 0);
          contend1x1(_address1279);
          _value1279 = operand_2659;
          int value3_2662 = nAndCarry_2658;
          _F1280 = value3_2662 & 1;
          value3_2662 = value3_2662 >>> 1;
          _F1280 = (_F1280 & 1) | 0x10 | (address_2657 & 0x28);
          if ((_value1279 & (0x01 << value3_2662)) == 0) {
              _F1280 |= 0x44;
          }
          if (value3_2662 == 7 && (_value1279 & 0x80) != 0) {
              _F1280 |= 0x80;
          }
          F = (_F1280 & 0xFF);
          MEMPTR = _address1279;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x57: {
          int _F1283;
          int _value1282;
          int _address1282;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2664;
          address_2664 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2665 = 4 | F & 1;
          _address1282 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2666 = read(_address1282, 0);
          contend1x1(_address1282);
          _value1282 = operand_2666;
          int value3_2669 = nAndCarry_2665;
          _F1283 = value3_2669 & 1;
          value3_2669 = value3_2669 >>> 1;
          _F1283 = (_F1283 & 1) | 0x10 | (address_2664 & 0x28);
          if ((_value1282 & (0x01 << value3_2669)) == 0) {
              _F1283 |= 0x44;
          }
          if (value3_2669 == 7 && (_value1282 & 0x80) != 0) {
              _F1283 |= 0x80;
          }
          F = (_F1283 & 0xFF);
          MEMPTR = _address1282;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_11(int opcode, int displacement) {
    switch (opcode) {
      case 0x58: {
          int _F1286;
          int _value1285;
          int _address1285;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2671;
          address_2671 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2672 = 6 | F & 1;
          _address1285 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2673 = read(_address1285, 0);
          contend1x1(_address1285);
          _value1285 = operand_2673;
          int value3_2676 = nAndCarry_2672;
          _F1286 = value3_2676 & 1;
          value3_2676 = value3_2676 >>> 1;
          _F1286 = (_F1286 & 1) | 0x10 | (address_2671 & 0x28);
          if ((_value1285 & (0x01 << value3_2676)) == 0) {
              _F1286 |= 0x44;
          }
          if (value3_2676 == 7 && (_value1285 & 0x80) != 0) {
              _F1286 |= 0x80;
          }
          F = (_F1286 & 0xFF);
          MEMPTR = _address1285;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x59: {
          int _F1289;
          int _value1288;
          int _address1288;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2678;
          address_2678 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2679 = 6 | F & 1;
          _address1288 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2680 = read(_address1288, 0);
          contend1x1(_address1288);
          _value1288 = operand_2680;
          int value3_2683 = nAndCarry_2679;
          _F1289 = value3_2683 & 1;
          value3_2683 = value3_2683 >>> 1;
          _F1289 = (_F1289 & 1) | 0x10 | (address_2678 & 0x28);
          if ((_value1288 & (0x01 << value3_2683)) == 0) {
              _F1289 |= 0x44;
          }
          if (value3_2683 == 7 && (_value1288 & 0x80) != 0) {
              _F1289 |= 0x80;
          }
          F = (_F1289 & 0xFF);
          MEMPTR = _address1288;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5A: {
          int _F1292;
          int _value1291;
          int _address1291;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2685;
          address_2685 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2686 = 6 | F & 1;
          _address1291 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2687 = read(_address1291, 0);
          contend1x1(_address1291);
          _value1291 = operand_2687;
          int value3_2690 = nAndCarry_2686;
          _F1292 = value3_2690 & 1;
          value3_2690 = value3_2690 >>> 1;
          _F1292 = (_F1292 & 1) | 0x10 | (address_2685 & 0x28);
          if ((_value1291 & (0x01 << value3_2690)) == 0) {
              _F1292 |= 0x44;
          }
          if (value3_2690 == 7 && (_value1291 & 0x80) != 0) {
              _F1292 |= 0x80;
          }
          F = (_F1292 & 0xFF);
          MEMPTR = _address1291;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5B: {
          int _F1295;
          int _value1294;
          int _address1294;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2692;
          address_2692 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2693 = 6 | F & 1;
          _address1294 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2694 = read(_address1294, 0);
          contend1x1(_address1294);
          _value1294 = operand_2694;
          int value3_2697 = nAndCarry_2693;
          _F1295 = value3_2697 & 1;
          value3_2697 = value3_2697 >>> 1;
          _F1295 = (_F1295 & 1) | 0x10 | (address_2692 & 0x28);
          if ((_value1294 & (0x01 << value3_2697)) == 0) {
              _F1295 |= 0x44;
          }
          if (value3_2697 == 7 && (_value1294 & 0x80) != 0) {
              _F1295 |= 0x80;
          }
          F = (_F1295 & 0xFF);
          MEMPTR = _address1294;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5C: {
          int _F1298;
          int _value1297;
          int _address1297;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2699;
          address_2699 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2700 = 6 | F & 1;
          _address1297 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2701 = read(_address1297, 0);
          contend1x1(_address1297);
          _value1297 = operand_2701;
          int value3_2704 = nAndCarry_2700;
          _F1298 = value3_2704 & 1;
          value3_2704 = value3_2704 >>> 1;
          _F1298 = (_F1298 & 1) | 0x10 | (address_2699 & 0x28);
          if ((_value1297 & (0x01 << value3_2704)) == 0) {
              _F1298 |= 0x44;
          }
          if (value3_2704 == 7 && (_value1297 & 0x80) != 0) {
              _F1298 |= 0x80;
          }
          F = (_F1298 & 0xFF);
          MEMPTR = _address1297;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5D: {
          int _F1301;
          int _value1300;
          int _address1300;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2706;
          address_2706 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2707 = 6 | F & 1;
          _address1300 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2708 = read(_address1300, 0);
          contend1x1(_address1300);
          _value1300 = operand_2708;
          int value3_2711 = nAndCarry_2707;
          _F1301 = value3_2711 & 1;
          value3_2711 = value3_2711 >>> 1;
          _F1301 = (_F1301 & 1) | 0x10 | (address_2706 & 0x28);
          if ((_value1300 & (0x01 << value3_2711)) == 0) {
              _F1301 |= 0x44;
          }
          if (value3_2711 == 7 && (_value1300 & 0x80) != 0) {
              _F1301 |= 0x80;
          }
          F = (_F1301 & 0xFF);
          MEMPTR = _address1300;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _F1304;
          int _value1303;
          int _address1303;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2713;
          address_2713 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2714 = 6 | F & 1;
          _address1303 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2715 = read(_address1303, 0);
          contend1x1(_address1303);
          _value1303 = operand_2715;
          int value3_2718 = nAndCarry_2714;
          _F1304 = value3_2718 & 1;
          value3_2718 = value3_2718 >>> 1;
          _F1304 = (_F1304 & 1) | 0x10 | (address_2713 & 0x28);
          if ((_value1303 & (0x01 << value3_2718)) == 0) {
              _F1304 |= 0x44;
          }
          if (value3_2718 == 7 && (_value1303 & 0x80) != 0) {
              _F1304 |= 0x80;
          }
          F = (_F1304 & 0xFF);
          MEMPTR = _address1303;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5F: {
          int _F1307;
          int _value1306;
          int _address1306;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2720;
          address_2720 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2721 = 6 | F & 1;
          _address1306 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2722 = read(_address1306, 0);
          contend1x1(_address1306);
          _value1306 = operand_2722;
          int value3_2725 = nAndCarry_2721;
          _F1307 = value3_2725 & 1;
          value3_2725 = value3_2725 >>> 1;
          _F1307 = (_F1307 & 1) | 0x10 | (address_2720 & 0x28);
          if ((_value1306 & (0x01 << value3_2725)) == 0) {
              _F1307 |= 0x44;
          }
          if (value3_2725 == 7 && (_value1306 & 0x80) != 0) {
              _F1307 |= 0x80;
          }
          F = (_F1307 & 0xFF);
          MEMPTR = _address1306;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_12(int opcode, int displacement) {
    switch (opcode) {
      case 0x60: {
          int _F1310;
          int _value1309;
          int _address1309;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2727;
          address_2727 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2728 = 8 | F & 1;
          _address1309 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2729 = read(_address1309, 0);
          contend1x1(_address1309);
          _value1309 = operand_2729;
          int value3_2732 = nAndCarry_2728;
          _F1310 = value3_2732 & 1;
          value3_2732 = value3_2732 >>> 1;
          _F1310 = (_F1310 & 1) | 0x10 | (address_2727 & 0x28);
          if ((_value1309 & (0x01 << value3_2732)) == 0) {
              _F1310 |= 0x44;
          }
          if (value3_2732 == 7 && (_value1309 & 0x80) != 0) {
              _F1310 |= 0x80;
          }
          F = (_F1310 & 0xFF);
          MEMPTR = _address1309;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x61: {
          int _F1313;
          int _value1312;
          int _address1312;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2734;
          address_2734 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2735 = 8 | F & 1;
          _address1312 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2736 = read(_address1312, 0);
          contend1x1(_address1312);
          _value1312 = operand_2736;
          int value3_2739 = nAndCarry_2735;
          _F1313 = value3_2739 & 1;
          value3_2739 = value3_2739 >>> 1;
          _F1313 = (_F1313 & 1) | 0x10 | (address_2734 & 0x28);
          if ((_value1312 & (0x01 << value3_2739)) == 0) {
              _F1313 |= 0x44;
          }
          if (value3_2739 == 7 && (_value1312 & 0x80) != 0) {
              _F1313 |= 0x80;
          }
          F = (_F1313 & 0xFF);
          MEMPTR = _address1312;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x62: {
          int _F1316;
          int _value1315;
          int _address1315;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2741;
          address_2741 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2742 = 8 | F & 1;
          _address1315 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2743 = read(_address1315, 0);
          contend1x1(_address1315);
          _value1315 = operand_2743;
          int value3_2746 = nAndCarry_2742;
          _F1316 = value3_2746 & 1;
          value3_2746 = value3_2746 >>> 1;
          _F1316 = (_F1316 & 1) | 0x10 | (address_2741 & 0x28);
          if ((_value1315 & (0x01 << value3_2746)) == 0) {
              _F1316 |= 0x44;
          }
          if (value3_2746 == 7 && (_value1315 & 0x80) != 0) {
              _F1316 |= 0x80;
          }
          F = (_F1316 & 0xFF);
          MEMPTR = _address1315;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x63: {
          int _F1319;
          int _value1318;
          int _address1318;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2748;
          address_2748 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2749 = 8 | F & 1;
          _address1318 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2750 = read(_address1318, 0);
          contend1x1(_address1318);
          _value1318 = operand_2750;
          int value3_2753 = nAndCarry_2749;
          _F1319 = value3_2753 & 1;
          value3_2753 = value3_2753 >>> 1;
          _F1319 = (_F1319 & 1) | 0x10 | (address_2748 & 0x28);
          if ((_value1318 & (0x01 << value3_2753)) == 0) {
              _F1319 |= 0x44;
          }
          if (value3_2753 == 7 && (_value1318 & 0x80) != 0) {
              _F1319 |= 0x80;
          }
          F = (_F1319 & 0xFF);
          MEMPTR = _address1318;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x64: {
          int _F1322;
          int _value1321;
          int _address1321;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2755;
          address_2755 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2756 = 8 | F & 1;
          _address1321 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2757 = read(_address1321, 0);
          contend1x1(_address1321);
          _value1321 = operand_2757;
          int value3_2760 = nAndCarry_2756;
          _F1322 = value3_2760 & 1;
          value3_2760 = value3_2760 >>> 1;
          _F1322 = (_F1322 & 1) | 0x10 | (address_2755 & 0x28);
          if ((_value1321 & (0x01 << value3_2760)) == 0) {
              _F1322 |= 0x44;
          }
          if (value3_2760 == 7 && (_value1321 & 0x80) != 0) {
              _F1322 |= 0x80;
          }
          F = (_F1322 & 0xFF);
          MEMPTR = _address1321;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x65: {
          int _F1325;
          int _value1324;
          int _address1324;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2762;
          address_2762 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2763 = 8 | F & 1;
          _address1324 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2764 = read(_address1324, 0);
          contend1x1(_address1324);
          _value1324 = operand_2764;
          int value3_2767 = nAndCarry_2763;
          _F1325 = value3_2767 & 1;
          value3_2767 = value3_2767 >>> 1;
          _F1325 = (_F1325 & 1) | 0x10 | (address_2762 & 0x28);
          if ((_value1324 & (0x01 << value3_2767)) == 0) {
              _F1325 |= 0x44;
          }
          if (value3_2767 == 7 && (_value1324 & 0x80) != 0) {
              _F1325 |= 0x80;
          }
          F = (_F1325 & 0xFF);
          MEMPTR = _address1324;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _F1328;
          int _value1327;
          int _address1327;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2769;
          address_2769 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2770 = 8 | F & 1;
          _address1327 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2771 = read(_address1327, 0);
          contend1x1(_address1327);
          _value1327 = operand_2771;
          int value3_2774 = nAndCarry_2770;
          _F1328 = value3_2774 & 1;
          value3_2774 = value3_2774 >>> 1;
          _F1328 = (_F1328 & 1) | 0x10 | (address_2769 & 0x28);
          if ((_value1327 & (0x01 << value3_2774)) == 0) {
              _F1328 |= 0x44;
          }
          if (value3_2774 == 7 && (_value1327 & 0x80) != 0) {
              _F1328 |= 0x80;
          }
          F = (_F1328 & 0xFF);
          MEMPTR = _address1327;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x67: {
          int _F1331;
          int _value1330;
          int _address1330;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2776;
          address_2776 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2777 = 8 | F & 1;
          _address1330 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2778 = read(_address1330, 0);
          contend1x1(_address1330);
          _value1330 = operand_2778;
          int value3_2781 = nAndCarry_2777;
          _F1331 = value3_2781 & 1;
          value3_2781 = value3_2781 >>> 1;
          _F1331 = (_F1331 & 1) | 0x10 | (address_2776 & 0x28);
          if ((_value1330 & (0x01 << value3_2781)) == 0) {
              _F1331 |= 0x44;
          }
          if (value3_2781 == 7 && (_value1330 & 0x80) != 0) {
              _F1331 |= 0x80;
          }
          F = (_F1331 & 0xFF);
          MEMPTR = _address1330;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_13(int opcode, int displacement) {
    switch (opcode) {
      case 0x68: {
          int _F1334;
          int _value1333;
          int _address1333;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2783;
          address_2783 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2784 = 10 | F & 1;
          _address1333 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2785 = read(_address1333, 0);
          contend1x1(_address1333);
          _value1333 = operand_2785;
          int value3_2788 = nAndCarry_2784;
          _F1334 = value3_2788 & 1;
          value3_2788 = value3_2788 >>> 1;
          _F1334 = (_F1334 & 1) | 0x10 | (address_2783 & 0x28);
          if ((_value1333 & (0x01 << value3_2788)) == 0) {
              _F1334 |= 0x44;
          }
          if (value3_2788 == 7 && (_value1333 & 0x80) != 0) {
              _F1334 |= 0x80;
          }
          F = (_F1334 & 0xFF);
          MEMPTR = _address1333;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x69: {
          int _F1337;
          int _value1336;
          int _address1336;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2790;
          address_2790 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2791 = 10 | F & 1;
          _address1336 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2792 = read(_address1336, 0);
          contend1x1(_address1336);
          _value1336 = operand_2792;
          int value3_2795 = nAndCarry_2791;
          _F1337 = value3_2795 & 1;
          value3_2795 = value3_2795 >>> 1;
          _F1337 = (_F1337 & 1) | 0x10 | (address_2790 & 0x28);
          if ((_value1336 & (0x01 << value3_2795)) == 0) {
              _F1337 |= 0x44;
          }
          if (value3_2795 == 7 && (_value1336 & 0x80) != 0) {
              _F1337 |= 0x80;
          }
          F = (_F1337 & 0xFF);
          MEMPTR = _address1336;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6A: {
          int _F1340;
          int _value1339;
          int _address1339;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2797;
          address_2797 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2798 = 10 | F & 1;
          _address1339 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2799 = read(_address1339, 0);
          contend1x1(_address1339);
          _value1339 = operand_2799;
          int value3_2802 = nAndCarry_2798;
          _F1340 = value3_2802 & 1;
          value3_2802 = value3_2802 >>> 1;
          _F1340 = (_F1340 & 1) | 0x10 | (address_2797 & 0x28);
          if ((_value1339 & (0x01 << value3_2802)) == 0) {
              _F1340 |= 0x44;
          }
          if (value3_2802 == 7 && (_value1339 & 0x80) != 0) {
              _F1340 |= 0x80;
          }
          F = (_F1340 & 0xFF);
          MEMPTR = _address1339;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6B: {
          int _F1343;
          int _value1342;
          int _address1342;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2804;
          address_2804 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2805 = 10 | F & 1;
          _address1342 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2806 = read(_address1342, 0);
          contend1x1(_address1342);
          _value1342 = operand_2806;
          int value3_2809 = nAndCarry_2805;
          _F1343 = value3_2809 & 1;
          value3_2809 = value3_2809 >>> 1;
          _F1343 = (_F1343 & 1) | 0x10 | (address_2804 & 0x28);
          if ((_value1342 & (0x01 << value3_2809)) == 0) {
              _F1343 |= 0x44;
          }
          if (value3_2809 == 7 && (_value1342 & 0x80) != 0) {
              _F1343 |= 0x80;
          }
          F = (_F1343 & 0xFF);
          MEMPTR = _address1342;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6C: {
          int _F1346;
          int _value1345;
          int _address1345;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2811;
          address_2811 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2812 = 10 | F & 1;
          _address1345 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2813 = read(_address1345, 0);
          contend1x1(_address1345);
          _value1345 = operand_2813;
          int value3_2816 = nAndCarry_2812;
          _F1346 = value3_2816 & 1;
          value3_2816 = value3_2816 >>> 1;
          _F1346 = (_F1346 & 1) | 0x10 | (address_2811 & 0x28);
          if ((_value1345 & (0x01 << value3_2816)) == 0) {
              _F1346 |= 0x44;
          }
          if (value3_2816 == 7 && (_value1345 & 0x80) != 0) {
              _F1346 |= 0x80;
          }
          F = (_F1346 & 0xFF);
          MEMPTR = _address1345;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6D: {
          int _F1349;
          int _value1348;
          int _address1348;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2818;
          address_2818 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2819 = 10 | F & 1;
          _address1348 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2820 = read(_address1348, 0);
          contend1x1(_address1348);
          _value1348 = operand_2820;
          int value3_2823 = nAndCarry_2819;
          _F1349 = value3_2823 & 1;
          value3_2823 = value3_2823 >>> 1;
          _F1349 = (_F1349 & 1) | 0x10 | (address_2818 & 0x28);
          if ((_value1348 & (0x01 << value3_2823)) == 0) {
              _F1349 |= 0x44;
          }
          if (value3_2823 == 7 && (_value1348 & 0x80) != 0) {
              _F1349 |= 0x80;
          }
          F = (_F1349 & 0xFF);
          MEMPTR = _address1348;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _F1352;
          int _value1351;
          int _address1351;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2825;
          address_2825 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2826 = 10 | F & 1;
          _address1351 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2827 = read(_address1351, 0);
          contend1x1(_address1351);
          _value1351 = operand_2827;
          int value3_2830 = nAndCarry_2826;
          _F1352 = value3_2830 & 1;
          value3_2830 = value3_2830 >>> 1;
          _F1352 = (_F1352 & 1) | 0x10 | (address_2825 & 0x28);
          if ((_value1351 & (0x01 << value3_2830)) == 0) {
              _F1352 |= 0x44;
          }
          if (value3_2830 == 7 && (_value1351 & 0x80) != 0) {
              _F1352 |= 0x80;
          }
          F = (_F1352 & 0xFF);
          MEMPTR = _address1351;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6F: {
          int _F1355;
          int _value1354;
          int _address1354;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2832;
          address_2832 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2833 = 10 | F & 1;
          _address1354 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2834 = read(_address1354, 0);
          contend1x1(_address1354);
          _value1354 = operand_2834;
          int value3_2837 = nAndCarry_2833;
          _F1355 = value3_2837 & 1;
          value3_2837 = value3_2837 >>> 1;
          _F1355 = (_F1355 & 1) | 0x10 | (address_2832 & 0x28);
          if ((_value1354 & (0x01 << value3_2837)) == 0) {
              _F1355 |= 0x44;
          }
          if (value3_2837 == 7 && (_value1354 & 0x80) != 0) {
              _F1355 |= 0x80;
          }
          F = (_F1355 & 0xFF);
          MEMPTR = _address1354;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_14(int opcode, int displacement) {
    switch (opcode) {
      case 0x70: {
          int _F1358;
          int _value1357;
          int _address1357;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2839;
          address_2839 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2840 = 12 | F & 1;
          _address1357 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2841 = read(_address1357, 0);
          contend1x1(_address1357);
          _value1357 = operand_2841;
          int value3_2844 = nAndCarry_2840;
          _F1358 = value3_2844 & 1;
          value3_2844 = value3_2844 >>> 1;
          _F1358 = (_F1358 & 1) | 0x10 | (address_2839 & 0x28);
          if ((_value1357 & (0x01 << value3_2844)) == 0) {
              _F1358 |= 0x44;
          }
          if (value3_2844 == 7 && (_value1357 & 0x80) != 0) {
              _F1358 |= 0x80;
          }
          F = (_F1358 & 0xFF);
          MEMPTR = _address1357;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _F1361;
          int _value1360;
          int _address1360;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2846;
          address_2846 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2847 = 12 | F & 1;
          _address1360 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2848 = read(_address1360, 0);
          contend1x1(_address1360);
          _value1360 = operand_2848;
          int value3_2851 = nAndCarry_2847;
          _F1361 = value3_2851 & 1;
          value3_2851 = value3_2851 >>> 1;
          _F1361 = (_F1361 & 1) | 0x10 | (address_2846 & 0x28);
          if ((_value1360 & (0x01 << value3_2851)) == 0) {
              _F1361 |= 0x44;
          }
          if (value3_2851 == 7 && (_value1360 & 0x80) != 0) {
              _F1361 |= 0x80;
          }
          F = (_F1361 & 0xFF);
          MEMPTR = _address1360;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _F1364;
          int _value1363;
          int _address1363;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2853;
          address_2853 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2854 = 12 | F & 1;
          _address1363 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2855 = read(_address1363, 0);
          contend1x1(_address1363);
          _value1363 = operand_2855;
          int value3_2858 = nAndCarry_2854;
          _F1364 = value3_2858 & 1;
          value3_2858 = value3_2858 >>> 1;
          _F1364 = (_F1364 & 1) | 0x10 | (address_2853 & 0x28);
          if ((_value1363 & (0x01 << value3_2858)) == 0) {
              _F1364 |= 0x44;
          }
          if (value3_2858 == 7 && (_value1363 & 0x80) != 0) {
              _F1364 |= 0x80;
          }
          F = (_F1364 & 0xFF);
          MEMPTR = _address1363;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _F1367;
          int _value1366;
          int _address1366;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2860;
          address_2860 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2861 = 12 | F & 1;
          _address1366 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2862 = read(_address1366, 0);
          contend1x1(_address1366);
          _value1366 = operand_2862;
          int value3_2865 = nAndCarry_2861;
          _F1367 = value3_2865 & 1;
          value3_2865 = value3_2865 >>> 1;
          _F1367 = (_F1367 & 1) | 0x10 | (address_2860 & 0x28);
          if ((_value1366 & (0x01 << value3_2865)) == 0) {
              _F1367 |= 0x44;
          }
          if (value3_2865 == 7 && (_value1366 & 0x80) != 0) {
              _F1367 |= 0x80;
          }
          F = (_F1367 & 0xFF);
          MEMPTR = _address1366;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _F1370;
          int _value1369;
          int _address1369;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2867;
          address_2867 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2868 = 12 | F & 1;
          _address1369 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2869 = read(_address1369, 0);
          contend1x1(_address1369);
          _value1369 = operand_2869;
          int value3_2872 = nAndCarry_2868;
          _F1370 = value3_2872 & 1;
          value3_2872 = value3_2872 >>> 1;
          _F1370 = (_F1370 & 1) | 0x10 | (address_2867 & 0x28);
          if ((_value1369 & (0x01 << value3_2872)) == 0) {
              _F1370 |= 0x44;
          }
          if (value3_2872 == 7 && (_value1369 & 0x80) != 0) {
              _F1370 |= 0x80;
          }
          F = (_F1370 & 0xFF);
          MEMPTR = _address1369;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _F1373;
          int _value1372;
          int _address1372;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2874;
          address_2874 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2875 = 12 | F & 1;
          _address1372 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2876 = read(_address1372, 0);
          contend1x1(_address1372);
          _value1372 = operand_2876;
          int value3_2879 = nAndCarry_2875;
          _F1373 = value3_2879 & 1;
          value3_2879 = value3_2879 >>> 1;
          _F1373 = (_F1373 & 1) | 0x10 | (address_2874 & 0x28);
          if ((_value1372 & (0x01 << value3_2879)) == 0) {
              _F1373 |= 0x44;
          }
          if (value3_2879 == 7 && (_value1372 & 0x80) != 0) {
              _F1373 |= 0x80;
          }
          F = (_F1373 & 0xFF);
          MEMPTR = _address1372;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x76: {
          int _F1376;
          int _value1375;
          int _address1375;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2881;
          address_2881 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2882 = 12 | F & 1;
          _address1375 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2883 = read(_address1375, 0);
          contend1x1(_address1375);
          _value1375 = operand_2883;
          int value3_2886 = nAndCarry_2882;
          _F1376 = value3_2886 & 1;
          value3_2886 = value3_2886 >>> 1;
          _F1376 = (_F1376 & 1) | 0x10 | (address_2881 & 0x28);
          if ((_value1375 & (0x01 << value3_2886)) == 0) {
              _F1376 |= 0x44;
          }
          if (value3_2886 == 7 && (_value1375 & 0x80) != 0) {
              _F1376 |= 0x80;
          }
          F = (_F1376 & 0xFF);
          MEMPTR = _address1375;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x77: {
          int _F1379;
          int _value1378;
          int _address1378;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2888;
          address_2888 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2889 = 12 | F & 1;
          _address1378 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2890 = read(_address1378, 0);
          contend1x1(_address1378);
          _value1378 = operand_2890;
          int value3_2893 = nAndCarry_2889;
          _F1379 = value3_2893 & 1;
          value3_2893 = value3_2893 >>> 1;
          _F1379 = (_F1379 & 1) | 0x10 | (address_2888 & 0x28);
          if ((_value1378 & (0x01 << value3_2893)) == 0) {
              _F1379 |= 0x44;
          }
          if (value3_2893 == 7 && (_value1378 & 0x80) != 0) {
              _F1379 |= 0x80;
          }
          F = (_F1379 & 0xFF);
          MEMPTR = _address1378;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_15(int opcode, int displacement) {
    switch (opcode) {
      case 0x78: {
          int _F1382;
          int _value1381;
          int _address1381;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2895;
          address_2895 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2896 = 14 | F & 1;
          _address1381 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2897 = read(_address1381, 0);
          contend1x1(_address1381);
          _value1381 = operand_2897;
          int value3_2900 = nAndCarry_2896;
          _F1382 = value3_2900 & 1;
          value3_2900 = value3_2900 >>> 1;
          _F1382 = (_F1382 & 1) | 0x10 | (address_2895 & 0x28);
          if ((_value1381 & (0x01 << value3_2900)) == 0) {
              _F1382 |= 0x44;
          }
          if (value3_2900 == 7 && (_value1381 & 0x80) != 0) {
              _F1382 |= 0x80;
          }
          F = (_F1382 & 0xFF);
          MEMPTR = _address1381;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x79: {
          int _F1385;
          int _value1384;
          int _address1384;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2902;
          address_2902 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2903 = 14 | F & 1;
          _address1384 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2904 = read(_address1384, 0);
          contend1x1(_address1384);
          _value1384 = operand_2904;
          int value3_2907 = nAndCarry_2903;
          _F1385 = value3_2907 & 1;
          value3_2907 = value3_2907 >>> 1;
          _F1385 = (_F1385 & 1) | 0x10 | (address_2902 & 0x28);
          if ((_value1384 & (0x01 << value3_2907)) == 0) {
              _F1385 |= 0x44;
          }
          if (value3_2907 == 7 && (_value1384 & 0x80) != 0) {
              _F1385 |= 0x80;
          }
          F = (_F1385 & 0xFF);
          MEMPTR = _address1384;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7A: {
          int _F1388;
          int _value1387;
          int _address1387;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2909;
          address_2909 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2910 = 14 | F & 1;
          _address1387 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2911 = read(_address1387, 0);
          contend1x1(_address1387);
          _value1387 = operand_2911;
          int value3_2914 = nAndCarry_2910;
          _F1388 = value3_2914 & 1;
          value3_2914 = value3_2914 >>> 1;
          _F1388 = (_F1388 & 1) | 0x10 | (address_2909 & 0x28);
          if ((_value1387 & (0x01 << value3_2914)) == 0) {
              _F1388 |= 0x44;
          }
          if (value3_2914 == 7 && (_value1387 & 0x80) != 0) {
              _F1388 |= 0x80;
          }
          F = (_F1388 & 0xFF);
          MEMPTR = _address1387;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7B: {
          int _F1391;
          int _value1390;
          int _address1390;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2916;
          address_2916 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2917 = 14 | F & 1;
          _address1390 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2918 = read(_address1390, 0);
          contend1x1(_address1390);
          _value1390 = operand_2918;
          int value3_2921 = nAndCarry_2917;
          _F1391 = value3_2921 & 1;
          value3_2921 = value3_2921 >>> 1;
          _F1391 = (_F1391 & 1) | 0x10 | (address_2916 & 0x28);
          if ((_value1390 & (0x01 << value3_2921)) == 0) {
              _F1391 |= 0x44;
          }
          if (value3_2921 == 7 && (_value1390 & 0x80) != 0) {
              _F1391 |= 0x80;
          }
          F = (_F1391 & 0xFF);
          MEMPTR = _address1390;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7C: {
          int _F1394;
          int _value1393;
          int _address1393;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2923;
          address_2923 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2924 = 14 | F & 1;
          _address1393 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2925 = read(_address1393, 0);
          contend1x1(_address1393);
          _value1393 = operand_2925;
          int value3_2928 = nAndCarry_2924;
          _F1394 = value3_2928 & 1;
          value3_2928 = value3_2928 >>> 1;
          _F1394 = (_F1394 & 1) | 0x10 | (address_2923 & 0x28);
          if ((_value1393 & (0x01 << value3_2928)) == 0) {
              _F1394 |= 0x44;
          }
          if (value3_2928 == 7 && (_value1393 & 0x80) != 0) {
              _F1394 |= 0x80;
          }
          F = (_F1394 & 0xFF);
          MEMPTR = _address1393;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7D: {
          int _F1397;
          int _value1396;
          int _address1396;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2930;
          address_2930 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2931 = 14 | F & 1;
          _address1396 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2932 = read(_address1396, 0);
          contend1x1(_address1396);
          _value1396 = operand_2932;
          int value3_2935 = nAndCarry_2931;
          _F1397 = value3_2935 & 1;
          value3_2935 = value3_2935 >>> 1;
          _F1397 = (_F1397 & 1) | 0x10 | (address_2930 & 0x28);
          if ((_value1396 & (0x01 << value3_2935)) == 0) {
              _F1397 |= 0x44;
          }
          if (value3_2935 == 7 && (_value1396 & 0x80) != 0) {
              _F1397 |= 0x80;
          }
          F = (_F1397 & 0xFF);
          MEMPTR = _address1396;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _F1400;
          int _value1399;
          int _address1399;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2937;
          address_2937 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2938 = 14 | F & 1;
          _address1399 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2939 = read(_address1399, 0);
          contend1x1(_address1399);
          _value1399 = operand_2939;
          int value3_2942 = nAndCarry_2938;
          _F1400 = value3_2942 & 1;
          value3_2942 = value3_2942 >>> 1;
          _F1400 = (_F1400 & 1) | 0x10 | (address_2937 & 0x28);
          if ((_value1399 & (0x01 << value3_2942)) == 0) {
              _F1400 |= 0x44;
          }
          if (value3_2942 == 7 && (_value1399 & 0x80) != 0) {
              _F1400 |= 0x80;
          }
          F = (_F1400 & 0xFF);
          MEMPTR = _address1399;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7F: {
          int _F1403;
          int _value1402;
          int _address1402;
          contend2x1((PC + 3) & 0xFFFF);
          int address_2944;
          address_2944 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2945 = 14 | F & 1;
          _address1402 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2946 = read(_address1402, 0);
          contend1x1(_address1402);
          _value1402 = operand_2946;
          int value3_2949 = nAndCarry_2945;
          _F1403 = value3_2949 & 1;
          value3_2949 = value3_2949 >>> 1;
          _F1403 = (_F1403 & 1) | 0x10 | (address_2944 & 0x28);
          if ((_value1402 & (0x01 << value3_2949)) == 0) {
              _F1403 |= 0x44;
          }
          if (value3_2949 == 7 && (_value1402 & 0x80) != 0) {
              _F1403 |= 0x80;
          }
          F = (_F1403 & 0xFF);
          MEMPTR = _address1402;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_16(int opcode, int displacement) {
    switch (opcode) {
      case 0x80: {
          int _value1405;
          int _address1405;
          contend2x1((PC + 3) & 0xFFFF);
          _address1405 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2951 = read(_address1405, 0);
          contend1x1(_address1405);
          _value1405 = operand_2951;
          int value_2952 = (_value1405 & -2);
          _address1405 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1405 = value_2952;
          write(_address1405, value_2952);
          int read_2953;
          read_2953 = _value1405;
          B = read_2953;
          MEMPTR = _address1405;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _value1407;
          int _address1407;
          contend2x1((PC + 3) & 0xFFFF);
          _address1407 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2954 = read(_address1407, 0);
          contend1x1(_address1407);
          _value1407 = operand_2954;
          int value_2955 = (_value1407 & -2);
          _address1407 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1407 = value_2955;
          write(_address1407, value_2955);
          int read_2956;
          read_2956 = _value1407;
          C = read_2956;
          MEMPTR = _address1407;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _value1409;
          int _address1409;
          contend2x1((PC + 3) & 0xFFFF);
          _address1409 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2957 = read(_address1409, 0);
          contend1x1(_address1409);
          _value1409 = operand_2957;
          int value_2958 = (_value1409 & -2);
          _address1409 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1409 = value_2958;
          write(_address1409, value_2958);
          int read_2959;
          read_2959 = _value1409;
          D = read_2959;
          MEMPTR = _address1409;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _value1411;
          int _address1411;
          contend2x1((PC + 3) & 0xFFFF);
          _address1411 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2960 = read(_address1411, 0);
          contend1x1(_address1411);
          _value1411 = operand_2960;
          int value_2961 = (_value1411 & -2);
          _address1411 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1411 = value_2961;
          write(_address1411, value_2961);
          int read_2962;
          read_2962 = _value1411;
          E = read_2962;
          MEMPTR = _address1411;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _value1413;
          int _address1413;
          contend2x1((PC + 3) & 0xFFFF);
          _address1413 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2963 = read(_address1413, 0);
          contend1x1(_address1413);
          _value1413 = operand_2963;
          int value_2964 = (_value1413 & -2);
          _address1413 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1413 = value_2964;
          write(_address1413, value_2964);
          int read_2965;
          read_2965 = _value1413;
          H = read_2965;
          MEMPTR = _address1413;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _value1415;
          int _address1415;
          contend2x1((PC + 3) & 0xFFFF);
          _address1415 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2966 = read(_address1415, 0);
          contend1x1(_address1415);
          _value1415 = operand_2966;
          int value_2967 = (_value1415 & -2);
          _address1415 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1415 = value_2967;
          write(_address1415, value_2967);
          int read_2968;
          read_2968 = _value1415;
          L = read_2968;
          MEMPTR = _address1415;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _value1417;
          int _address1417;
          contend2x1((PC + 3) & 0xFFFF);
          _address1417 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2969 = read(_address1417, 0);
          contend1x1(_address1417);
          _value1417 = operand_2969;
          int value_2970 = (_value1417 & -2);
          _address1417 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1417 = value_2970;
          write(_address1417, value_2970);
          MEMPTR = _address1417;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _value1419;
          int _address1419;
          contend2x1((PC + 3) & 0xFFFF);
          _address1419 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2971 = read(_address1419, 0);
          contend1x1(_address1419);
          _value1419 = operand_2971;
          int value_2972 = (_value1419 & -2);
          _address1419 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1419 = value_2972;
          write(_address1419, value_2972);
          int read_2973;
          read_2973 = _value1419;
          A = read_2973;
          MEMPTR = _address1419;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_17(int opcode, int displacement) {
    switch (opcode) {
      case 0x88: {
          int _value1421;
          int _address1421;
          contend2x1((PC + 3) & 0xFFFF);
          _address1421 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2974 = read(_address1421, 0);
          contend1x1(_address1421);
          _value1421 = operand_2974;
          int value_2975 = (_value1421 & -3);
          _address1421 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1421 = value_2975;
          write(_address1421, value_2975);
          int read_2976;
          read_2976 = _value1421;
          B = read_2976;
          MEMPTR = _address1421;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _value1423;
          int _address1423;
          contend2x1((PC + 3) & 0xFFFF);
          _address1423 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2977 = read(_address1423, 0);
          contend1x1(_address1423);
          _value1423 = operand_2977;
          int value_2978 = (_value1423 & -3);
          _address1423 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1423 = value_2978;
          write(_address1423, value_2978);
          int read_2979;
          read_2979 = _value1423;
          C = read_2979;
          MEMPTR = _address1423;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _value1425;
          int _address1425;
          contend2x1((PC + 3) & 0xFFFF);
          _address1425 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2980 = read(_address1425, 0);
          contend1x1(_address1425);
          _value1425 = operand_2980;
          int value_2981 = (_value1425 & -3);
          _address1425 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1425 = value_2981;
          write(_address1425, value_2981);
          int read_2982;
          read_2982 = _value1425;
          D = read_2982;
          MEMPTR = _address1425;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _value1427;
          int _address1427;
          contend2x1((PC + 3) & 0xFFFF);
          _address1427 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2983 = read(_address1427, 0);
          contend1x1(_address1427);
          _value1427 = operand_2983;
          int value_2984 = (_value1427 & -3);
          _address1427 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1427 = value_2984;
          write(_address1427, value_2984);
          int read_2985;
          read_2985 = _value1427;
          E = read_2985;
          MEMPTR = _address1427;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _value1429;
          int _address1429;
          contend2x1((PC + 3) & 0xFFFF);
          _address1429 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2986 = read(_address1429, 0);
          contend1x1(_address1429);
          _value1429 = operand_2986;
          int value_2987 = (_value1429 & -3);
          _address1429 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1429 = value_2987;
          write(_address1429, value_2987);
          int read_2988;
          read_2988 = _value1429;
          H = read_2988;
          MEMPTR = _address1429;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _value1431;
          int _address1431;
          contend2x1((PC + 3) & 0xFFFF);
          _address1431 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2989 = read(_address1431, 0);
          contend1x1(_address1431);
          _value1431 = operand_2989;
          int value_2990 = (_value1431 & -3);
          _address1431 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1431 = value_2990;
          write(_address1431, value_2990);
          int read_2991;
          read_2991 = _value1431;
          L = read_2991;
          MEMPTR = _address1431;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _value1433;
          int _address1433;
          contend2x1((PC + 3) & 0xFFFF);
          _address1433 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2992 = read(_address1433, 0);
          contend1x1(_address1433);
          _value1433 = operand_2992;
          int value_2993 = (_value1433 & -3);
          _address1433 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1433 = value_2993;
          write(_address1433, value_2993);
          MEMPTR = _address1433;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _value1435;
          int _address1435;
          contend2x1((PC + 3) & 0xFFFF);
          _address1435 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2994 = read(_address1435, 0);
          contend1x1(_address1435);
          _value1435 = operand_2994;
          int value_2995 = (_value1435 & -3);
          _address1435 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1435 = value_2995;
          write(_address1435, value_2995);
          int read_2996;
          read_2996 = _value1435;
          A = read_2996;
          MEMPTR = _address1435;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_18(int opcode, int displacement) {
    switch (opcode) {
      case 0x90: {
          int _value1437;
          int _address1437;
          contend2x1((PC + 3) & 0xFFFF);
          _address1437 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2997 = read(_address1437, 0);
          contend1x1(_address1437);
          _value1437 = operand_2997;
          int value_2998 = (_value1437 & -5);
          _address1437 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1437 = value_2998;
          write(_address1437, value_2998);
          int read_2999;
          read_2999 = _value1437;
          B = read_2999;
          MEMPTR = _address1437;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _value1439;
          int _address1439;
          contend2x1((PC + 3) & 0xFFFF);
          _address1439 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3000 = read(_address1439, 0);
          contend1x1(_address1439);
          _value1439 = operand_3000;
          int value_3001 = (_value1439 & -5);
          _address1439 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1439 = value_3001;
          write(_address1439, value_3001);
          int read_3002;
          read_3002 = _value1439;
          C = read_3002;
          MEMPTR = _address1439;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _value1441;
          int _address1441;
          contend2x1((PC + 3) & 0xFFFF);
          _address1441 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3003 = read(_address1441, 0);
          contend1x1(_address1441);
          _value1441 = operand_3003;
          int value_3004 = (_value1441 & -5);
          _address1441 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1441 = value_3004;
          write(_address1441, value_3004);
          int read_3005;
          read_3005 = _value1441;
          D = read_3005;
          MEMPTR = _address1441;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _value1443;
          int _address1443;
          contend2x1((PC + 3) & 0xFFFF);
          _address1443 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3006 = read(_address1443, 0);
          contend1x1(_address1443);
          _value1443 = operand_3006;
          int value_3007 = (_value1443 & -5);
          _address1443 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1443 = value_3007;
          write(_address1443, value_3007);
          int read_3008;
          read_3008 = _value1443;
          E = read_3008;
          MEMPTR = _address1443;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _value1445;
          int _address1445;
          contend2x1((PC + 3) & 0xFFFF);
          _address1445 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3009 = read(_address1445, 0);
          contend1x1(_address1445);
          _value1445 = operand_3009;
          int value_3010 = (_value1445 & -5);
          _address1445 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1445 = value_3010;
          write(_address1445, value_3010);
          int read_3011;
          read_3011 = _value1445;
          H = read_3011;
          MEMPTR = _address1445;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _value1447;
          int _address1447;
          contend2x1((PC + 3) & 0xFFFF);
          _address1447 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3012 = read(_address1447, 0);
          contend1x1(_address1447);
          _value1447 = operand_3012;
          int value_3013 = (_value1447 & -5);
          _address1447 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1447 = value_3013;
          write(_address1447, value_3013);
          int read_3014;
          read_3014 = _value1447;
          L = read_3014;
          MEMPTR = _address1447;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _value1449;
          int _address1449;
          contend2x1((PC + 3) & 0xFFFF);
          _address1449 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3015 = read(_address1449, 0);
          contend1x1(_address1449);
          _value1449 = operand_3015;
          int value_3016 = (_value1449 & -5);
          _address1449 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1449 = value_3016;
          write(_address1449, value_3016);
          MEMPTR = _address1449;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _value1451;
          int _address1451;
          contend2x1((PC + 3) & 0xFFFF);
          _address1451 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3017 = read(_address1451, 0);
          contend1x1(_address1451);
          _value1451 = operand_3017;
          int value_3018 = (_value1451 & -5);
          _address1451 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1451 = value_3018;
          write(_address1451, value_3018);
          int read_3019;
          read_3019 = _value1451;
          A = read_3019;
          MEMPTR = _address1451;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_19(int opcode, int displacement) {
    switch (opcode) {
      case 0x98: {
          int _value1453;
          int _address1453;
          contend2x1((PC + 3) & 0xFFFF);
          _address1453 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3020 = read(_address1453, 0);
          contend1x1(_address1453);
          _value1453 = operand_3020;
          int value_3021 = (_value1453 & -9);
          _address1453 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1453 = value_3021;
          write(_address1453, value_3021);
          int read_3022;
          read_3022 = _value1453;
          B = read_3022;
          MEMPTR = _address1453;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _value1455;
          int _address1455;
          contend2x1((PC + 3) & 0xFFFF);
          _address1455 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3023 = read(_address1455, 0);
          contend1x1(_address1455);
          _value1455 = operand_3023;
          int value_3024 = (_value1455 & -9);
          _address1455 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1455 = value_3024;
          write(_address1455, value_3024);
          int read_3025;
          read_3025 = _value1455;
          C = read_3025;
          MEMPTR = _address1455;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _value1457;
          int _address1457;
          contend2x1((PC + 3) & 0xFFFF);
          _address1457 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3026 = read(_address1457, 0);
          contend1x1(_address1457);
          _value1457 = operand_3026;
          int value_3027 = (_value1457 & -9);
          _address1457 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1457 = value_3027;
          write(_address1457, value_3027);
          int read_3028;
          read_3028 = _value1457;
          D = read_3028;
          MEMPTR = _address1457;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _value1459;
          int _address1459;
          contend2x1((PC + 3) & 0xFFFF);
          _address1459 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3029 = read(_address1459, 0);
          contend1x1(_address1459);
          _value1459 = operand_3029;
          int value_3030 = (_value1459 & -9);
          _address1459 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1459 = value_3030;
          write(_address1459, value_3030);
          int read_3031;
          read_3031 = _value1459;
          E = read_3031;
          MEMPTR = _address1459;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _value1461;
          int _address1461;
          contend2x1((PC + 3) & 0xFFFF);
          _address1461 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3032 = read(_address1461, 0);
          contend1x1(_address1461);
          _value1461 = operand_3032;
          int value_3033 = (_value1461 & -9);
          _address1461 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1461 = value_3033;
          write(_address1461, value_3033);
          int read_3034;
          read_3034 = _value1461;
          H = read_3034;
          MEMPTR = _address1461;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _value1463;
          int _address1463;
          contend2x1((PC + 3) & 0xFFFF);
          _address1463 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3035 = read(_address1463, 0);
          contend1x1(_address1463);
          _value1463 = operand_3035;
          int value_3036 = (_value1463 & -9);
          _address1463 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1463 = value_3036;
          write(_address1463, value_3036);
          int read_3037;
          read_3037 = _value1463;
          L = read_3037;
          MEMPTR = _address1463;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _value1465;
          int _address1465;
          contend2x1((PC + 3) & 0xFFFF);
          _address1465 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3038 = read(_address1465, 0);
          contend1x1(_address1465);
          _value1465 = operand_3038;
          int value_3039 = (_value1465 & -9);
          _address1465 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1465 = value_3039;
          write(_address1465, value_3039);
          MEMPTR = _address1465;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _value1467;
          int _address1467;
          contend2x1((PC + 3) & 0xFFFF);
          _address1467 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3040 = read(_address1467, 0);
          contend1x1(_address1467);
          _value1467 = operand_3040;
          int value_3041 = (_value1467 & -9);
          _address1467 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1467 = value_3041;
          write(_address1467, value_3041);
          int read_3042;
          read_3042 = _value1467;
          A = read_3042;
          MEMPTR = _address1467;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_20(int opcode, int displacement) {
    switch (opcode) {
      case 0xA0: {
          int _value1469;
          int _address1469;
          contend2x1((PC + 3) & 0xFFFF);
          _address1469 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3043 = read(_address1469, 0);
          contend1x1(_address1469);
          _value1469 = operand_3043;
          int value_3044 = (_value1469 & -17);
          _address1469 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1469 = value_3044;
          write(_address1469, value_3044);
          int read_3045;
          read_3045 = _value1469;
          B = read_3045;
          MEMPTR = _address1469;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _value1471;
          int _address1471;
          contend2x1((PC + 3) & 0xFFFF);
          _address1471 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3046 = read(_address1471, 0);
          contend1x1(_address1471);
          _value1471 = operand_3046;
          int value_3047 = (_value1471 & -17);
          _address1471 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1471 = value_3047;
          write(_address1471, value_3047);
          int read_3048;
          read_3048 = _value1471;
          C = read_3048;
          MEMPTR = _address1471;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _value1473;
          int _address1473;
          contend2x1((PC + 3) & 0xFFFF);
          _address1473 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3049 = read(_address1473, 0);
          contend1x1(_address1473);
          _value1473 = operand_3049;
          int value_3050 = (_value1473 & -17);
          _address1473 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1473 = value_3050;
          write(_address1473, value_3050);
          int read_3051;
          read_3051 = _value1473;
          D = read_3051;
          MEMPTR = _address1473;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _value1475;
          int _address1475;
          contend2x1((PC + 3) & 0xFFFF);
          _address1475 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3052 = read(_address1475, 0);
          contend1x1(_address1475);
          _value1475 = operand_3052;
          int value_3053 = (_value1475 & -17);
          _address1475 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1475 = value_3053;
          write(_address1475, value_3053);
          int read_3054;
          read_3054 = _value1475;
          E = read_3054;
          MEMPTR = _address1475;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _value1477;
          int _address1477;
          contend2x1((PC + 3) & 0xFFFF);
          _address1477 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3055 = read(_address1477, 0);
          contend1x1(_address1477);
          _value1477 = operand_3055;
          int value_3056 = (_value1477 & -17);
          _address1477 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1477 = value_3056;
          write(_address1477, value_3056);
          int read_3057;
          read_3057 = _value1477;
          H = read_3057;
          MEMPTR = _address1477;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _value1479;
          int _address1479;
          contend2x1((PC + 3) & 0xFFFF);
          _address1479 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3058 = read(_address1479, 0);
          contend1x1(_address1479);
          _value1479 = operand_3058;
          int value_3059 = (_value1479 & -17);
          _address1479 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1479 = value_3059;
          write(_address1479, value_3059);
          int read_3060;
          read_3060 = _value1479;
          L = read_3060;
          MEMPTR = _address1479;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _value1481;
          int _address1481;
          contend2x1((PC + 3) & 0xFFFF);
          _address1481 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3061 = read(_address1481, 0);
          contend1x1(_address1481);
          _value1481 = operand_3061;
          int value_3062 = (_value1481 & -17);
          _address1481 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1481 = value_3062;
          write(_address1481, value_3062);
          MEMPTR = _address1481;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _value1483;
          int _address1483;
          contend2x1((PC + 3) & 0xFFFF);
          _address1483 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3063 = read(_address1483, 0);
          contend1x1(_address1483);
          _value1483 = operand_3063;
          int value_3064 = (_value1483 & -17);
          _address1483 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1483 = value_3064;
          write(_address1483, value_3064);
          int read_3065;
          read_3065 = _value1483;
          A = read_3065;
          MEMPTR = _address1483;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_21(int opcode, int displacement) {
    switch (opcode) {
      case 0xA8: {
          int _value1485;
          int _address1485;
          contend2x1((PC + 3) & 0xFFFF);
          _address1485 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3066 = read(_address1485, 0);
          contend1x1(_address1485);
          _value1485 = operand_3066;
          int value_3067 = (_value1485 & -33);
          _address1485 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1485 = value_3067;
          write(_address1485, value_3067);
          int read_3068;
          read_3068 = _value1485;
          B = read_3068;
          MEMPTR = _address1485;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _value1487;
          int _address1487;
          contend2x1((PC + 3) & 0xFFFF);
          _address1487 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3069 = read(_address1487, 0);
          contend1x1(_address1487);
          _value1487 = operand_3069;
          int value_3070 = (_value1487 & -33);
          _address1487 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1487 = value_3070;
          write(_address1487, value_3070);
          int read_3071;
          read_3071 = _value1487;
          C = read_3071;
          MEMPTR = _address1487;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _value1489;
          int _address1489;
          contend2x1((PC + 3) & 0xFFFF);
          _address1489 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3072 = read(_address1489, 0);
          contend1x1(_address1489);
          _value1489 = operand_3072;
          int value_3073 = (_value1489 & -33);
          _address1489 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1489 = value_3073;
          write(_address1489, value_3073);
          int read_3074;
          read_3074 = _value1489;
          D = read_3074;
          MEMPTR = _address1489;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _value1491;
          int _address1491;
          contend2x1((PC + 3) & 0xFFFF);
          _address1491 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3075 = read(_address1491, 0);
          contend1x1(_address1491);
          _value1491 = operand_3075;
          int value_3076 = (_value1491 & -33);
          _address1491 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1491 = value_3076;
          write(_address1491, value_3076);
          int read_3077;
          read_3077 = _value1491;
          E = read_3077;
          MEMPTR = _address1491;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _value1493;
          int _address1493;
          contend2x1((PC + 3) & 0xFFFF);
          _address1493 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3078 = read(_address1493, 0);
          contend1x1(_address1493);
          _value1493 = operand_3078;
          int value_3079 = (_value1493 & -33);
          _address1493 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1493 = value_3079;
          write(_address1493, value_3079);
          int read_3080;
          read_3080 = _value1493;
          H = read_3080;
          MEMPTR = _address1493;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _value1495;
          int _address1495;
          contend2x1((PC + 3) & 0xFFFF);
          _address1495 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3081 = read(_address1495, 0);
          contend1x1(_address1495);
          _value1495 = operand_3081;
          int value_3082 = (_value1495 & -33);
          _address1495 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1495 = value_3082;
          write(_address1495, value_3082);
          int read_3083;
          read_3083 = _value1495;
          L = read_3083;
          MEMPTR = _address1495;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _value1497;
          int _address1497;
          contend2x1((PC + 3) & 0xFFFF);
          _address1497 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3084 = read(_address1497, 0);
          contend1x1(_address1497);
          _value1497 = operand_3084;
          int value_3085 = (_value1497 & -33);
          _address1497 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1497 = value_3085;
          write(_address1497, value_3085);
          MEMPTR = _address1497;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _value1499;
          int _address1499;
          contend2x1((PC + 3) & 0xFFFF);
          _address1499 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3086 = read(_address1499, 0);
          contend1x1(_address1499);
          _value1499 = operand_3086;
          int value_3087 = (_value1499 & -33);
          _address1499 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1499 = value_3087;
          write(_address1499, value_3087);
          int read_3088;
          read_3088 = _value1499;
          A = read_3088;
          MEMPTR = _address1499;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_22(int opcode, int displacement) {
    switch (opcode) {
      case 0xB0: {
          int _value1501;
          int _address1501;
          contend2x1((PC + 3) & 0xFFFF);
          _address1501 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3089 = read(_address1501, 0);
          contend1x1(_address1501);
          _value1501 = operand_3089;
          int value_3090 = (_value1501 & -65);
          _address1501 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1501 = value_3090;
          write(_address1501, value_3090);
          int read_3091;
          read_3091 = _value1501;
          B = read_3091;
          MEMPTR = _address1501;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _value1503;
          int _address1503;
          contend2x1((PC + 3) & 0xFFFF);
          _address1503 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3092 = read(_address1503, 0);
          contend1x1(_address1503);
          _value1503 = operand_3092;
          int value_3093 = (_value1503 & -65);
          _address1503 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1503 = value_3093;
          write(_address1503, value_3093);
          int read_3094;
          read_3094 = _value1503;
          C = read_3094;
          MEMPTR = _address1503;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _value1505;
          int _address1505;
          contend2x1((PC + 3) & 0xFFFF);
          _address1505 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3095 = read(_address1505, 0);
          contend1x1(_address1505);
          _value1505 = operand_3095;
          int value_3096 = (_value1505 & -65);
          _address1505 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1505 = value_3096;
          write(_address1505, value_3096);
          int read_3097;
          read_3097 = _value1505;
          D = read_3097;
          MEMPTR = _address1505;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _value1507;
          int _address1507;
          contend2x1((PC + 3) & 0xFFFF);
          _address1507 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3098 = read(_address1507, 0);
          contend1x1(_address1507);
          _value1507 = operand_3098;
          int value_3099 = (_value1507 & -65);
          _address1507 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1507 = value_3099;
          write(_address1507, value_3099);
          int read_3100;
          read_3100 = _value1507;
          E = read_3100;
          MEMPTR = _address1507;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _value1509;
          int _address1509;
          contend2x1((PC + 3) & 0xFFFF);
          _address1509 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3101 = read(_address1509, 0);
          contend1x1(_address1509);
          _value1509 = operand_3101;
          int value_3102 = (_value1509 & -65);
          _address1509 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1509 = value_3102;
          write(_address1509, value_3102);
          int read_3103;
          read_3103 = _value1509;
          H = read_3103;
          MEMPTR = _address1509;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _value1511;
          int _address1511;
          contend2x1((PC + 3) & 0xFFFF);
          _address1511 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3104 = read(_address1511, 0);
          contend1x1(_address1511);
          _value1511 = operand_3104;
          int value_3105 = (_value1511 & -65);
          _address1511 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1511 = value_3105;
          write(_address1511, value_3105);
          int read_3106;
          read_3106 = _value1511;
          L = read_3106;
          MEMPTR = _address1511;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _value1513;
          int _address1513;
          contend2x1((PC + 3) & 0xFFFF);
          _address1513 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3107 = read(_address1513, 0);
          contend1x1(_address1513);
          _value1513 = operand_3107;
          int value_3108 = (_value1513 & -65);
          _address1513 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1513 = value_3108;
          write(_address1513, value_3108);
          MEMPTR = _address1513;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _value1515;
          int _address1515;
          contend2x1((PC + 3) & 0xFFFF);
          _address1515 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3109 = read(_address1515, 0);
          contend1x1(_address1515);
          _value1515 = operand_3109;
          int value_3110 = (_value1515 & -65);
          _address1515 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1515 = value_3110;
          write(_address1515, value_3110);
          int read_3111;
          read_3111 = _value1515;
          A = read_3111;
          MEMPTR = _address1515;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_23(int opcode, int displacement) {
    switch (opcode) {
      case 0xB8: {
          int _value1517;
          int _address1517;
          contend2x1((PC + 3) & 0xFFFF);
          _address1517 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3112 = read(_address1517, 0);
          contend1x1(_address1517);
          _value1517 = operand_3112;
          int value_3113 = (_value1517 & -129);
          _address1517 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1517 = value_3113;
          write(_address1517, value_3113);
          int read_3114;
          read_3114 = _value1517;
          B = read_3114;
          MEMPTR = _address1517;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _value1519;
          int _address1519;
          contend2x1((PC + 3) & 0xFFFF);
          _address1519 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3115 = read(_address1519, 0);
          contend1x1(_address1519);
          _value1519 = operand_3115;
          int value_3116 = (_value1519 & -129);
          _address1519 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1519 = value_3116;
          write(_address1519, value_3116);
          int read_3117;
          read_3117 = _value1519;
          C = read_3117;
          MEMPTR = _address1519;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _value1521;
          int _address1521;
          contend2x1((PC + 3) & 0xFFFF);
          _address1521 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3118 = read(_address1521, 0);
          contend1x1(_address1521);
          _value1521 = operand_3118;
          int value_3119 = (_value1521 & -129);
          _address1521 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1521 = value_3119;
          write(_address1521, value_3119);
          int read_3120;
          read_3120 = _value1521;
          D = read_3120;
          MEMPTR = _address1521;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _value1523;
          int _address1523;
          contend2x1((PC + 3) & 0xFFFF);
          _address1523 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3121 = read(_address1523, 0);
          contend1x1(_address1523);
          _value1523 = operand_3121;
          int value_3122 = (_value1523 & -129);
          _address1523 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1523 = value_3122;
          write(_address1523, value_3122);
          int read_3123;
          read_3123 = _value1523;
          E = read_3123;
          MEMPTR = _address1523;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _value1525;
          int _address1525;
          contend2x1((PC + 3) & 0xFFFF);
          _address1525 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3124 = read(_address1525, 0);
          contend1x1(_address1525);
          _value1525 = operand_3124;
          int value_3125 = (_value1525 & -129);
          _address1525 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1525 = value_3125;
          write(_address1525, value_3125);
          int read_3126;
          read_3126 = _value1525;
          H = read_3126;
          MEMPTR = _address1525;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _value1527;
          int _address1527;
          contend2x1((PC + 3) & 0xFFFF);
          _address1527 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3127 = read(_address1527, 0);
          contend1x1(_address1527);
          _value1527 = operand_3127;
          int value_3128 = (_value1527 & -129);
          _address1527 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1527 = value_3128;
          write(_address1527, value_3128);
          int read_3129;
          read_3129 = _value1527;
          L = read_3129;
          MEMPTR = _address1527;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _value1529;
          int _address1529;
          contend2x1((PC + 3) & 0xFFFF);
          _address1529 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3130 = read(_address1529, 0);
          contend1x1(_address1529);
          _value1529 = operand_3130;
          int value_3131 = (_value1529 & -129);
          _address1529 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1529 = value_3131;
          write(_address1529, value_3131);
          MEMPTR = _address1529;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _value1531;
          int _address1531;
          contend2x1((PC + 3) & 0xFFFF);
          _address1531 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3132 = read(_address1531, 0);
          contend1x1(_address1531);
          _value1531 = operand_3132;
          int value_3133 = (_value1531 & -129);
          _address1531 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1531 = value_3133;
          write(_address1531, value_3133);
          int read_3134;
          read_3134 = _value1531;
          A = read_3134;
          MEMPTR = _address1531;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_24(int opcode, int displacement) {
    switch (opcode) {
      case 0xC0: {
          int _value1533;
          int _address1533;
          contend2x1((PC + 3) & 0xFFFF);
          _address1533 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3135 = read(_address1533, 0);
          contend1x1(_address1533);
          _value1533 = operand_3135;
          int value_3136 = (_value1533 | 1);
          _address1533 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1533 = value_3136;
          write(_address1533, value_3136);
          int read_3137;
          read_3137 = _value1533;
          B = read_3137;
          MEMPTR = _address1533;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC1: {
          int _value1535;
          int _address1535;
          contend2x1((PC + 3) & 0xFFFF);
          _address1535 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3138 = read(_address1535, 0);
          contend1x1(_address1535);
          _value1535 = operand_3138;
          int value_3139 = (_value1535 | 1);
          _address1535 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1535 = value_3139;
          write(_address1535, value_3139);
          int read_3140;
          read_3140 = _value1535;
          C = read_3140;
          MEMPTR = _address1535;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _value1537;
          int _address1537;
          contend2x1((PC + 3) & 0xFFFF);
          _address1537 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3141 = read(_address1537, 0);
          contend1x1(_address1537);
          _value1537 = operand_3141;
          int value_3142 = (_value1537 | 1);
          _address1537 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1537 = value_3142;
          write(_address1537, value_3142);
          int read_3143;
          read_3143 = _value1537;
          D = read_3143;
          MEMPTR = _address1537;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC3: {
          int _value1539;
          int _address1539;
          contend2x1((PC + 3) & 0xFFFF);
          _address1539 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3144 = read(_address1539, 0);
          contend1x1(_address1539);
          _value1539 = operand_3144;
          int value_3145 = (_value1539 | 1);
          _address1539 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1539 = value_3145;
          write(_address1539, value_3145);
          int read_3146;
          read_3146 = _value1539;
          E = read_3146;
          MEMPTR = _address1539;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC4: {
          int _value1541;
          int _address1541;
          contend2x1((PC + 3) & 0xFFFF);
          _address1541 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3147 = read(_address1541, 0);
          contend1x1(_address1541);
          _value1541 = operand_3147;
          int value_3148 = (_value1541 | 1);
          _address1541 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1541 = value_3148;
          write(_address1541, value_3148);
          int read_3149;
          read_3149 = _value1541;
          H = read_3149;
          MEMPTR = _address1541;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC5: {
          int _value1543;
          int _address1543;
          contend2x1((PC + 3) & 0xFFFF);
          _address1543 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3150 = read(_address1543, 0);
          contend1x1(_address1543);
          _value1543 = operand_3150;
          int value_3151 = (_value1543 | 1);
          _address1543 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1543 = value_3151;
          write(_address1543, value_3151);
          int read_3152;
          read_3152 = _value1543;
          L = read_3152;
          MEMPTR = _address1543;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _value1545;
          int _address1545;
          contend2x1((PC + 3) & 0xFFFF);
          _address1545 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3153 = read(_address1545, 0);
          contend1x1(_address1545);
          _value1545 = operand_3153;
          int value_3154 = (_value1545 | 1);
          _address1545 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1545 = value_3154;
          write(_address1545, value_3154);
          MEMPTR = _address1545;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _value1547;
          int _address1547;
          contend2x1((PC + 3) & 0xFFFF);
          _address1547 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3155 = read(_address1547, 0);
          contend1x1(_address1547);
          _value1547 = operand_3155;
          int value_3156 = (_value1547 | 1);
          _address1547 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1547 = value_3156;
          write(_address1547, value_3156);
          int read_3157;
          read_3157 = _value1547;
          A = read_3157;
          MEMPTR = _address1547;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_25(int opcode, int displacement) {
    switch (opcode) {
      case 0xC8: {
          int _value1549;
          int _address1549;
          contend2x1((PC + 3) & 0xFFFF);
          _address1549 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3158 = read(_address1549, 0);
          contend1x1(_address1549);
          _value1549 = operand_3158;
          int value_3159 = (_value1549 | 2);
          _address1549 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1549 = value_3159;
          write(_address1549, value_3159);
          int read_3160;
          read_3160 = _value1549;
          B = read_3160;
          MEMPTR = _address1549;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC9: {
          int _value1551;
          int _address1551;
          contend2x1((PC + 3) & 0xFFFF);
          _address1551 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3161 = read(_address1551, 0);
          contend1x1(_address1551);
          _value1551 = operand_3161;
          int value_3162 = (_value1551 | 2);
          _address1551 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1551 = value_3162;
          write(_address1551, value_3162);
          int read_3163;
          read_3163 = _value1551;
          C = read_3163;
          MEMPTR = _address1551;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCA: {
          int _value1553;
          int _address1553;
          contend2x1((PC + 3) & 0xFFFF);
          _address1553 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3164 = read(_address1553, 0);
          contend1x1(_address1553);
          _value1553 = operand_3164;
          int value_3165 = (_value1553 | 2);
          _address1553 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1553 = value_3165;
          write(_address1553, value_3165);
          int read_3166;
          read_3166 = _value1553;
          D = read_3166;
          MEMPTR = _address1553;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCB: {
          int _value1555;
          int _address1555;
          contend2x1((PC + 3) & 0xFFFF);
          _address1555 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3167 = read(_address1555, 0);
          contend1x1(_address1555);
          _value1555 = operand_3167;
          int value_3168 = (_value1555 | 2);
          _address1555 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1555 = value_3168;
          write(_address1555, value_3168);
          int read_3169;
          read_3169 = _value1555;
          E = read_3169;
          MEMPTR = _address1555;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCC: {
          int _value1557;
          int _address1557;
          contend2x1((PC + 3) & 0xFFFF);
          _address1557 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3170 = read(_address1557, 0);
          contend1x1(_address1557);
          _value1557 = operand_3170;
          int value_3171 = (_value1557 | 2);
          _address1557 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1557 = value_3171;
          write(_address1557, value_3171);
          int read_3172;
          read_3172 = _value1557;
          H = read_3172;
          MEMPTR = _address1557;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCD: {
          int _value1559;
          int _address1559;
          contend2x1((PC + 3) & 0xFFFF);
          _address1559 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3173 = read(_address1559, 0);
          contend1x1(_address1559);
          _value1559 = operand_3173;
          int value_3174 = (_value1559 | 2);
          _address1559 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1559 = value_3174;
          write(_address1559, value_3174);
          int read_3175;
          read_3175 = _value1559;
          L = read_3175;
          MEMPTR = _address1559;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCE: {
          int _value1561;
          int _address1561;
          contend2x1((PC + 3) & 0xFFFF);
          _address1561 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3176 = read(_address1561, 0);
          contend1x1(_address1561);
          _value1561 = operand_3176;
          int value_3177 = (_value1561 | 2);
          _address1561 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1561 = value_3177;
          write(_address1561, value_3177);
          MEMPTR = _address1561;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _value1563;
          int _address1563;
          contend2x1((PC + 3) & 0xFFFF);
          _address1563 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3178 = read(_address1563, 0);
          contend1x1(_address1563);
          _value1563 = operand_3178;
          int value_3179 = (_value1563 | 2);
          _address1563 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1563 = value_3179;
          write(_address1563, value_3179);
          int read_3180;
          read_3180 = _value1563;
          A = read_3180;
          MEMPTR = _address1563;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_26(int opcode, int displacement) {
    switch (opcode) {
      case 0xD0: {
          int _value1565;
          int _address1565;
          contend2x1((PC + 3) & 0xFFFF);
          _address1565 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3181 = read(_address1565, 0);
          contend1x1(_address1565);
          _value1565 = operand_3181;
          int value_3182 = (_value1565 | 4);
          _address1565 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1565 = value_3182;
          write(_address1565, value_3182);
          int read_3183;
          read_3183 = _value1565;
          B = read_3183;
          MEMPTR = _address1565;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD1: {
          int _value1567;
          int _address1567;
          contend2x1((PC + 3) & 0xFFFF);
          _address1567 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3184 = read(_address1567, 0);
          contend1x1(_address1567);
          _value1567 = operand_3184;
          int value_3185 = (_value1567 | 4);
          _address1567 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1567 = value_3185;
          write(_address1567, value_3185);
          int read_3186;
          read_3186 = _value1567;
          C = read_3186;
          MEMPTR = _address1567;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _value1569;
          int _address1569;
          contend2x1((PC + 3) & 0xFFFF);
          _address1569 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3187 = read(_address1569, 0);
          contend1x1(_address1569);
          _value1569 = operand_3187;
          int value_3188 = (_value1569 | 4);
          _address1569 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1569 = value_3188;
          write(_address1569, value_3188);
          int read_3189;
          read_3189 = _value1569;
          D = read_3189;
          MEMPTR = _address1569;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD3: {
          int _value1571;
          int _address1571;
          contend2x1((PC + 3) & 0xFFFF);
          _address1571 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3190 = read(_address1571, 0);
          contend1x1(_address1571);
          _value1571 = operand_3190;
          int value_3191 = (_value1571 | 4);
          _address1571 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1571 = value_3191;
          write(_address1571, value_3191);
          int read_3192;
          read_3192 = _value1571;
          E = read_3192;
          MEMPTR = _address1571;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _value1573;
          int _address1573;
          contend2x1((PC + 3) & 0xFFFF);
          _address1573 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3193 = read(_address1573, 0);
          contend1x1(_address1573);
          _value1573 = operand_3193;
          int value_3194 = (_value1573 | 4);
          _address1573 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1573 = value_3194;
          write(_address1573, value_3194);
          int read_3195;
          read_3195 = _value1573;
          H = read_3195;
          MEMPTR = _address1573;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD5: {
          int _value1575;
          int _address1575;
          contend2x1((PC + 3) & 0xFFFF);
          _address1575 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3196 = read(_address1575, 0);
          contend1x1(_address1575);
          _value1575 = operand_3196;
          int value_3197 = (_value1575 | 4);
          _address1575 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1575 = value_3197;
          write(_address1575, value_3197);
          int read_3198;
          read_3198 = _value1575;
          L = read_3198;
          MEMPTR = _address1575;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _value1577;
          int _address1577;
          contend2x1((PC + 3) & 0xFFFF);
          _address1577 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3199 = read(_address1577, 0);
          contend1x1(_address1577);
          _value1577 = operand_3199;
          int value_3200 = (_value1577 | 4);
          _address1577 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1577 = value_3200;
          write(_address1577, value_3200);
          MEMPTR = _address1577;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _value1579;
          int _address1579;
          contend2x1((PC + 3) & 0xFFFF);
          _address1579 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3201 = read(_address1579, 0);
          contend1x1(_address1579);
          _value1579 = operand_3201;
          int value_3202 = (_value1579 | 4);
          _address1579 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1579 = value_3202;
          write(_address1579, value_3202);
          int read_3203;
          read_3203 = _value1579;
          A = read_3203;
          MEMPTR = _address1579;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_27(int opcode, int displacement) {
    switch (opcode) {
      case 0xD8: {
          int _value1581;
          int _address1581;
          contend2x1((PC + 3) & 0xFFFF);
          _address1581 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3204 = read(_address1581, 0);
          contend1x1(_address1581);
          _value1581 = operand_3204;
          int value_3205 = (_value1581 | 8);
          _address1581 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1581 = value_3205;
          write(_address1581, value_3205);
          int read_3206;
          read_3206 = _value1581;
          B = read_3206;
          MEMPTR = _address1581;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD9: {
          int _value1583;
          int _address1583;
          contend2x1((PC + 3) & 0xFFFF);
          _address1583 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3207 = read(_address1583, 0);
          contend1x1(_address1583);
          _value1583 = operand_3207;
          int value_3208 = (_value1583 | 8);
          _address1583 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1583 = value_3208;
          write(_address1583, value_3208);
          int read_3209;
          read_3209 = _value1583;
          C = read_3209;
          MEMPTR = _address1583;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _value1585;
          int _address1585;
          contend2x1((PC + 3) & 0xFFFF);
          _address1585 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3210 = read(_address1585, 0);
          contend1x1(_address1585);
          _value1585 = operand_3210;
          int value_3211 = (_value1585 | 8);
          _address1585 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1585 = value_3211;
          write(_address1585, value_3211);
          int read_3212;
          read_3212 = _value1585;
          D = read_3212;
          MEMPTR = _address1585;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDB: {
          int _value1587;
          int _address1587;
          contend2x1((PC + 3) & 0xFFFF);
          _address1587 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3213 = read(_address1587, 0);
          contend1x1(_address1587);
          _value1587 = operand_3213;
          int value_3214 = (_value1587 | 8);
          _address1587 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1587 = value_3214;
          write(_address1587, value_3214);
          int read_3215;
          read_3215 = _value1587;
          E = read_3215;
          MEMPTR = _address1587;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _value1589;
          int _address1589;
          contend2x1((PC + 3) & 0xFFFF);
          _address1589 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3216 = read(_address1589, 0);
          contend1x1(_address1589);
          _value1589 = operand_3216;
          int value_3217 = (_value1589 | 8);
          _address1589 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1589 = value_3217;
          write(_address1589, value_3217);
          int read_3218;
          read_3218 = _value1589;
          H = read_3218;
          MEMPTR = _address1589;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDD: {
          int _value1591;
          int _address1591;
          contend2x1((PC + 3) & 0xFFFF);
          _address1591 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3219 = read(_address1591, 0);
          contend1x1(_address1591);
          _value1591 = operand_3219;
          int value_3220 = (_value1591 | 8);
          _address1591 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1591 = value_3220;
          write(_address1591, value_3220);
          int read_3221;
          read_3221 = _value1591;
          L = read_3221;
          MEMPTR = _address1591;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _value1593;
          int _address1593;
          contend2x1((PC + 3) & 0xFFFF);
          _address1593 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3222 = read(_address1593, 0);
          contend1x1(_address1593);
          _value1593 = operand_3222;
          int value_3223 = (_value1593 | 8);
          _address1593 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1593 = value_3223;
          write(_address1593, value_3223);
          MEMPTR = _address1593;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _value1595;
          int _address1595;
          contend2x1((PC + 3) & 0xFFFF);
          _address1595 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3224 = read(_address1595, 0);
          contend1x1(_address1595);
          _value1595 = operand_3224;
          int value_3225 = (_value1595 | 8);
          _address1595 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1595 = value_3225;
          write(_address1595, value_3225);
          int read_3226;
          read_3226 = _value1595;
          A = read_3226;
          MEMPTR = _address1595;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_28(int opcode, int displacement) {
    switch (opcode) {
      case 0xE0: {
          int _value1597;
          int _address1597;
          contend2x1((PC + 3) & 0xFFFF);
          _address1597 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3227 = read(_address1597, 0);
          contend1x1(_address1597);
          _value1597 = operand_3227;
          int value_3228 = (_value1597 | 0x10);
          _address1597 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1597 = value_3228;
          write(_address1597, value_3228);
          int read_3229;
          read_3229 = _value1597;
          B = read_3229;
          MEMPTR = _address1597;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE1: {
          int _value1599;
          int _address1599;
          contend2x1((PC + 3) & 0xFFFF);
          _address1599 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3230 = read(_address1599, 0);
          contend1x1(_address1599);
          _value1599 = operand_3230;
          int value_3231 = (_value1599 | 0x10);
          _address1599 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1599 = value_3231;
          write(_address1599, value_3231);
          int read_3232;
          read_3232 = _value1599;
          C = read_3232;
          MEMPTR = _address1599;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _value1601;
          int _address1601;
          contend2x1((PC + 3) & 0xFFFF);
          _address1601 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3233 = read(_address1601, 0);
          contend1x1(_address1601);
          _value1601 = operand_3233;
          int value_3234 = (_value1601 | 0x10);
          _address1601 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1601 = value_3234;
          write(_address1601, value_3234);
          int read_3235;
          read_3235 = _value1601;
          D = read_3235;
          MEMPTR = _address1601;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE3: {
          int _value1603;
          int _address1603;
          contend2x1((PC + 3) & 0xFFFF);
          _address1603 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3236 = read(_address1603, 0);
          contend1x1(_address1603);
          _value1603 = operand_3236;
          int value_3237 = (_value1603 | 0x10);
          _address1603 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1603 = value_3237;
          write(_address1603, value_3237);
          int read_3238;
          read_3238 = _value1603;
          E = read_3238;
          MEMPTR = _address1603;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _value1605;
          int _address1605;
          contend2x1((PC + 3) & 0xFFFF);
          _address1605 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3239 = read(_address1605, 0);
          contend1x1(_address1605);
          _value1605 = operand_3239;
          int value_3240 = (_value1605 | 0x10);
          _address1605 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1605 = value_3240;
          write(_address1605, value_3240);
          int read_3241;
          read_3241 = _value1605;
          H = read_3241;
          MEMPTR = _address1605;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE5: {
          int _value1607;
          int _address1607;
          contend2x1((PC + 3) & 0xFFFF);
          _address1607 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3242 = read(_address1607, 0);
          contend1x1(_address1607);
          _value1607 = operand_3242;
          int value_3243 = (_value1607 | 0x10);
          _address1607 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1607 = value_3243;
          write(_address1607, value_3243);
          int read_3244;
          read_3244 = _value1607;
          L = read_3244;
          MEMPTR = _address1607;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _value1609;
          int _address1609;
          contend2x1((PC + 3) & 0xFFFF);
          _address1609 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3245 = read(_address1609, 0);
          contend1x1(_address1609);
          _value1609 = operand_3245;
          int value_3246 = (_value1609 | 0x10);
          _address1609 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1609 = value_3246;
          write(_address1609, value_3246);
          MEMPTR = _address1609;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _value1611;
          int _address1611;
          contend2x1((PC + 3) & 0xFFFF);
          _address1611 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3247 = read(_address1611, 0);
          contend1x1(_address1611);
          _value1611 = operand_3247;
          int value_3248 = (_value1611 | 0x10);
          _address1611 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1611 = value_3248;
          write(_address1611, value_3248);
          int read_3249;
          read_3249 = _value1611;
          A = read_3249;
          MEMPTR = _address1611;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_29(int opcode, int displacement) {
    switch (opcode) {
      case 0xE8: {
          int _value1613;
          int _address1613;
          contend2x1((PC + 3) & 0xFFFF);
          _address1613 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3250 = read(_address1613, 0);
          contend1x1(_address1613);
          _value1613 = operand_3250;
          int value_3251 = (_value1613 | 0x20);
          _address1613 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1613 = value_3251;
          write(_address1613, value_3251);
          int read_3252;
          read_3252 = _value1613;
          B = read_3252;
          MEMPTR = _address1613;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE9: {
          int _value1615;
          int _address1615;
          contend2x1((PC + 3) & 0xFFFF);
          _address1615 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3253 = read(_address1615, 0);
          contend1x1(_address1615);
          _value1615 = operand_3253;
          int value_3254 = (_value1615 | 0x20);
          _address1615 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1615 = value_3254;
          write(_address1615, value_3254);
          int read_3255;
          read_3255 = _value1615;
          C = read_3255;
          MEMPTR = _address1615;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEA: {
          int _value1617;
          int _address1617;
          contend2x1((PC + 3) & 0xFFFF);
          _address1617 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3256 = read(_address1617, 0);
          contend1x1(_address1617);
          _value1617 = operand_3256;
          int value_3257 = (_value1617 | 0x20);
          _address1617 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1617 = value_3257;
          write(_address1617, value_3257);
          int read_3258;
          read_3258 = _value1617;
          D = read_3258;
          MEMPTR = _address1617;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEB: {
          int _value1619;
          int _address1619;
          contend2x1((PC + 3) & 0xFFFF);
          _address1619 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3259 = read(_address1619, 0);
          contend1x1(_address1619);
          _value1619 = operand_3259;
          int value_3260 = (_value1619 | 0x20);
          _address1619 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1619 = value_3260;
          write(_address1619, value_3260);
          int read_3261;
          read_3261 = _value1619;
          E = read_3261;
          MEMPTR = _address1619;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _value1621;
          int _address1621;
          contend2x1((PC + 3) & 0xFFFF);
          _address1621 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3262 = read(_address1621, 0);
          contend1x1(_address1621);
          _value1621 = operand_3262;
          int value_3263 = (_value1621 | 0x20);
          _address1621 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1621 = value_3263;
          write(_address1621, value_3263);
          int read_3264;
          read_3264 = _value1621;
          H = read_3264;
          MEMPTR = _address1621;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xED: {
          int _value1623;
          int _address1623;
          contend2x1((PC + 3) & 0xFFFF);
          _address1623 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3265 = read(_address1623, 0);
          contend1x1(_address1623);
          _value1623 = operand_3265;
          int value_3266 = (_value1623 | 0x20);
          _address1623 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1623 = value_3266;
          write(_address1623, value_3266);
          int read_3267;
          read_3267 = _value1623;
          L = read_3267;
          MEMPTR = _address1623;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _value1625;
          int _address1625;
          contend2x1((PC + 3) & 0xFFFF);
          _address1625 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3268 = read(_address1625, 0);
          contend1x1(_address1625);
          _value1625 = operand_3268;
          int value_3269 = (_value1625 | 0x20);
          _address1625 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1625 = value_3269;
          write(_address1625, value_3269);
          MEMPTR = _address1625;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _value1627;
          int _address1627;
          contend2x1((PC + 3) & 0xFFFF);
          _address1627 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3270 = read(_address1627, 0);
          contend1x1(_address1627);
          _value1627 = operand_3270;
          int value_3271 = (_value1627 | 0x20);
          _address1627 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1627 = value_3271;
          write(_address1627, value_3271);
          int read_3272;
          read_3272 = _value1627;
          A = read_3272;
          MEMPTR = _address1627;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_30(int opcode, int displacement) {
    switch (opcode) {
      case 0xF0: {
          int _value1629;
          int _address1629;
          contend2x1((PC + 3) & 0xFFFF);
          _address1629 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3273 = read(_address1629, 0);
          contend1x1(_address1629);
          _value1629 = operand_3273;
          int value_3274 = (_value1629 | 0x40);
          _address1629 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1629 = value_3274;
          write(_address1629, value_3274);
          int read_3275;
          read_3275 = _value1629;
          B = read_3275;
          MEMPTR = _address1629;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF1: {
          int _value1631;
          int _address1631;
          contend2x1((PC + 3) & 0xFFFF);
          _address1631 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3276 = read(_address1631, 0);
          contend1x1(_address1631);
          _value1631 = operand_3276;
          int value_3277 = (_value1631 | 0x40);
          _address1631 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1631 = value_3277;
          write(_address1631, value_3277);
          int read_3278;
          read_3278 = _value1631;
          C = read_3278;
          MEMPTR = _address1631;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _value1633;
          int _address1633;
          contend2x1((PC + 3) & 0xFFFF);
          _address1633 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3279 = read(_address1633, 0);
          contend1x1(_address1633);
          _value1633 = operand_3279;
          int value_3280 = (_value1633 | 0x40);
          _address1633 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1633 = value_3280;
          write(_address1633, value_3280);
          int read_3281;
          read_3281 = _value1633;
          D = read_3281;
          MEMPTR = _address1633;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF3: {
          int _value1635;
          int _address1635;
          contend2x1((PC + 3) & 0xFFFF);
          _address1635 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3282 = read(_address1635, 0);
          contend1x1(_address1635);
          _value1635 = operand_3282;
          int value_3283 = (_value1635 | 0x40);
          _address1635 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1635 = value_3283;
          write(_address1635, value_3283);
          int read_3284;
          read_3284 = _value1635;
          E = read_3284;
          MEMPTR = _address1635;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _value1637;
          int _address1637;
          contend2x1((PC + 3) & 0xFFFF);
          _address1637 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3285 = read(_address1637, 0);
          contend1x1(_address1637);
          _value1637 = operand_3285;
          int value_3286 = (_value1637 | 0x40);
          _address1637 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1637 = value_3286;
          write(_address1637, value_3286);
          int read_3287;
          read_3287 = _value1637;
          H = read_3287;
          MEMPTR = _address1637;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF5: {
          int _value1639;
          int _address1639;
          contend2x1((PC + 3) & 0xFFFF);
          _address1639 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3288 = read(_address1639, 0);
          contend1x1(_address1639);
          _value1639 = operand_3288;
          int value_3289 = (_value1639 | 0x40);
          _address1639 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1639 = value_3289;
          write(_address1639, value_3289);
          int read_3290;
          read_3290 = _value1639;
          L = read_3290;
          MEMPTR = _address1639;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _value1641;
          int _address1641;
          contend2x1((PC + 3) & 0xFFFF);
          _address1641 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3291 = read(_address1641, 0);
          contend1x1(_address1641);
          _value1641 = operand_3291;
          int value_3292 = (_value1641 | 0x40);
          _address1641 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1641 = value_3292;
          write(_address1641, value_3292);
          MEMPTR = _address1641;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _value1643;
          int _address1643;
          contend2x1((PC + 3) & 0xFFFF);
          _address1643 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3293 = read(_address1643, 0);
          contend1x1(_address1643);
          _value1643 = operand_3293;
          int value_3294 = (_value1643 | 0x40);
          _address1643 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1643 = value_3294;
          write(_address1643, value_3294);
          int read_3295;
          read_3295 = _value1643;
          A = read_3295;
          MEMPTR = _address1643;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeDDCB_31(int opcode, int displacement) {
    switch (opcode) {
      case 0xF8: {
          int _value1645;
          int _address1645;
          contend2x1((PC + 3) & 0xFFFF);
          _address1645 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3296 = read(_address1645, 0);
          contend1x1(_address1645);
          _value1645 = operand_3296;
          int value_3297 = (_value1645 | 0x80);
          _address1645 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1645 = value_3297;
          write(_address1645, value_3297);
          int read_3298;
          read_3298 = _value1645;
          B = read_3298;
          MEMPTR = _address1645;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF9: {
          int _value1647;
          int _address1647;
          contend2x1((PC + 3) & 0xFFFF);
          _address1647 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3299 = read(_address1647, 0);
          contend1x1(_address1647);
          _value1647 = operand_3299;
          int value_3300 = (_value1647 | 0x80);
          _address1647 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1647 = value_3300;
          write(_address1647, value_3300);
          int read_3301;
          read_3301 = _value1647;
          C = read_3301;
          MEMPTR = _address1647;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _value1649;
          int _address1649;
          contend2x1((PC + 3) & 0xFFFF);
          _address1649 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3302 = read(_address1649, 0);
          contend1x1(_address1649);
          _value1649 = operand_3302;
          int value_3303 = (_value1649 | 0x80);
          _address1649 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1649 = value_3303;
          write(_address1649, value_3303);
          int read_3304;
          read_3304 = _value1649;
          D = read_3304;
          MEMPTR = _address1649;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFB: {
          int _value1651;
          int _address1651;
          contend2x1((PC + 3) & 0xFFFF);
          _address1651 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3305 = read(_address1651, 0);
          contend1x1(_address1651);
          _value1651 = operand_3305;
          int value_3306 = (_value1651 | 0x80);
          _address1651 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1651 = value_3306;
          write(_address1651, value_3306);
          int read_3307;
          read_3307 = _value1651;
          E = read_3307;
          MEMPTR = _address1651;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _value1653;
          int _address1653;
          contend2x1((PC + 3) & 0xFFFF);
          _address1653 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3308 = read(_address1653, 0);
          contend1x1(_address1653);
          _value1653 = operand_3308;
          int value_3309 = (_value1653 | 0x80);
          _address1653 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1653 = value_3309;
          write(_address1653, value_3309);
          int read_3310;
          read_3310 = _value1653;
          H = read_3310;
          MEMPTR = _address1653;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFD: {
          int _value1655;
          int _address1655;
          contend2x1((PC + 3) & 0xFFFF);
          _address1655 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3311 = read(_address1655, 0);
          contend1x1(_address1655);
          _value1655 = operand_3311;
          int value_3312 = (_value1655 | 0x80);
          _address1655 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1655 = value_3312;
          write(_address1655, value_3312);
          int read_3313;
          read_3313 = _value1655;
          L = read_3313;
          MEMPTR = _address1655;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _value1657;
          int _address1657;
          contend2x1((PC + 3) & 0xFFFF);
          _address1657 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3314 = read(_address1657, 0);
          contend1x1(_address1657);
          _value1657 = operand_3314;
          int value_3315 = (_value1657 | 0x80);
          _address1657 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1657 = value_3315;
          write(_address1657, value_3315);
          MEMPTR = _address1657;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _value1659;
          int _address1659;
          contend2x1((PC + 3) & 0xFFFF);
          _address1659 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3316 = read(_address1659, 0);
          contend1x1(_address1659);
          _value1659 = operand_3316;
          int value_3317 = (_value1659 | 0x80);
          _address1659 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1659 = value_3317;
          write(_address1659, value_3317);
          int read_3318;
          read_3318 = _value1659;
          A = read_3318;
          MEMPTR = _address1659;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeDDCB");
    }
  }

  private void decodeED(int opcode) {
    switch (opcode >> 3) {
      case 0: decodeED_0(opcode);
        break;
      case 1: decodeED_1(opcode);
        break;
      case 2: decodeED_2(opcode);
        break;
      case 3: decodeED_3(opcode);
        break;
      case 4: decodeED_4(opcode);
        break;
      case 5: decodeED_5(opcode);
        break;
      case 6: decodeED_6(opcode);
        break;
      case 7: decodeED_7(opcode);
        break;
      case 8: decodeED_8(opcode);
        break;
      case 9: decodeED_9(opcode);
        break;
      case 10: decodeED_10(opcode);
        break;
      case 11: decodeED_11(opcode);
        break;
      case 12: decodeED_12(opcode);
        break;
      case 13: decodeED_13(opcode);
        break;
      case 14: decodeED_14(opcode);
        break;
      case 15: decodeED_15(opcode);
        break;
      case 16: decodeED_16(opcode);
        break;
      case 17: decodeED_17(opcode);
        break;
      case 18: decodeED_18(opcode);
        break;
      case 19: decodeED_19(opcode);
        break;
      case 20: decodeED_20(opcode);
        break;
      case 21: decodeED_21(opcode);
        break;
      case 22: decodeED_22(opcode);
        break;
      case 23: decodeED_23(opcode);
        break;
      case 24: decodeED_24(opcode);
        break;
      case 25: decodeED_25(opcode);
        break;
      case 26: decodeED_26(opcode);
        break;
      case 27: decodeED_27(opcode);
        break;
      case 28: decodeED_28(opcode);
        break;
      case 29: decodeED_29(opcode);
        break;
      case 30: decodeED_30(opcode);
        break;
      case 31: decodeED_31(opcode);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_0(int opcode) {
    switch (opcode) {
      case 0x00: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x01: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x02: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x03: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x07: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_1(int opcode) {
    switch (opcode) {
      case 0x08: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_2(int opcode) {
    switch (opcode) {
      case 0x10: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x11: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x12: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x13: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x17: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_3(int opcode) {
    switch (opcode) {
      case 0x18: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x19: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_4(int opcode) {
    switch (opcode) {
      case 0x20: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x21: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x22: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x23: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x27: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_5(int opcode) {
    switch (opcode) {
      case 0x28: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x29: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_6(int opcode) {
    switch (opcode) {
      case 0x30: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x31: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x32: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x33: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x35: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x36: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x37: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_7(int opcode) {
    switch (opcode) {
      case 0x38: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x39: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_8(int opcode) {
    switch (opcode) {
      case 0x40: {
          int _F1803;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3656 = ((B << 8) | C);
          int value_3658 = io.in(port_3656);
          B = value_3658 & 0xFF;
          int value1_3659 = value_3658 & 0xFF;
          int value2_3660 = F;
          _F1803 = value2_3660;
          _F1803 = (_F1803 & 1) | (SZ53P[value1_3659] | (value1_3659 == 0 ? 0x40 : 0));
          F = (_F1803 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x41: {
          int read_3663 = ((B << 8) | C);
          io.out(read_3663, B);
          int read_3664 = ((B << 8) | C);
          MEMPTR = ((read_3664 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x42: {
          int _F1806;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3665 = ((H << 8) | L);
          int b_3666 = ((B << 8) | C);
          int result_3667 = (a_3665 - b_3666 - (F & 1));
          int value1_3668 = ((result_3667 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3669 = (((a_3665 & 0x8800 | (b_3666 & 0x8800) >> 1) | ((result_3667 & 0x18800) | ((result_3667 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3671 = value2_3669 & 0x33;
          i_3671 |= i_3671 << 1 & 0x04;
          int result1_3672 = i_3671 << 11 & 0x1A800;
          int lookup_3673 = (value2_3669 << 8 & 0x8800) >> 11 | (value2_3669 << 9 & 0x8800) >> 10 | (result1_3672 & 0x8800) >> 9;
          _F1806 = ((result1_3672 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3673 >> 4)] | (result1_3672 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3673 & 0x07)] | (value1_3668 != 0 ? 0 : 0x40);
          F = (_F1806 & 0xFF);
          int value_3675 = (result_3667 & 0xffff);
          H = (value_3675 >>> 8);
          L = value_3675 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x43: {
          int _address1808;
          int address_3676 = (PC + 2) & 0xFFFF;
          int operand_3678 = read(address_3676, 0);
          int operand_3680 = read((address_3676 + 1) & 0xFFFF, 0);
          _address1808 = (operand_3680 << 8) | operand_3678;
          int value_3681 = ((B << 8) | C);
          write(_address1808, (value_3681 & 0xFF));
          write((_address1808 + 1) & 0xFFFF, (value_3681 >>> 8));
          MEMPTR = ((_address1808 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x44: {
          int _F1810;
          int value1_3682 = A;
          int value2_3683 = 0;
          int subtemp_3685 = value2_3683 - value1_3682;
          int lookup_3686 = ((value2_3683 & 0x88) >> 3) | ((value1_3682 & 0x88) >> 2) | ((subtemp_3685 & 0x88) >> 1);
          value2_3683 = subtemp_3685 & 0xff;
          _F1810 = ((subtemp_3685 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3686 & 0x07)] | OVERFLOW_SUB[(lookup_3686 >> 4)] | (SZ53[value2_3683] | (value2_3683 == 0 ? 0x40 : 0));
          F = (_F1810 & 0xFF);
          A = value2_3683;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x45: {
          int _nextPC1812;
          state.setIff1(state.isIff2());
          int jumpAddress2_3688;
          int wordNumber1_3690 = read(SP, 0);
          int wordNumber_3691 = read((SP + 1) & 0xFFFF, 0);
          int value_3689 = ((wordNumber_3691 << 8) | wordNumber1_3690);
          int wordNumber_3692 = SP;
          SP = ((wordNumber_3692 + 2) & 0xFFFF);
          jumpAddress2_3688 = value_3689;
          _nextPC1812 = jumpAddress2_3688;
          MEMPTR = _nextPC1812;
          PC = _nextPC1812;
          break;
      }
      case 0x46: {
          state.setIntMode(InterruptionMode.IM0);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x47: {
          contend1x1(((I << 8) | R));
          I = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_9(int opcode) {
    switch (opcode) {
      case 0x48: {
          int _F1815;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3695 = ((B << 8) | C);
          int value_3697 = io.in(port_3695);
          C = value_3697 & 0xFF;
          int value1_3698 = value_3697 & 0xFF;
          int value2_3699 = F;
          _F1815 = value2_3699;
          _F1815 = (_F1815 & 1) | (SZ53P[value1_3698] | (value1_3698 == 0 ? 0x40 : 0));
          F = (_F1815 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x49: {
          int read_3702 = ((B << 8) | C);
          io.out(read_3702, C);
          int read_3703 = ((B << 8) | C);
          MEMPTR = ((read_3703 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4A: {
          int _F1818;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3704 = ((B << 8) | C);
          int b_3705 = ((H << 8) | L);
          int result_3706 = (a_3704 + b_3705 + (F & 1));
          int value1_3707 = ((result_3706 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3708 = (((a_3704 & 0x8800 | (b_3705 & 0x8800) >> 1) | ((result_3706 & 0x18800) | ((result_3706 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3710 = value2_3708 & 0x33;
          i_3710 |= (i_3710 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3711 = (i_3710 << 11) & 0x1A800;
          int lookup_3712 = (value2_3708 << 8 & 0x8800) >> 11 | (value2_3708 << 9 & 0x8800) >> 10 | (result1_3711 & 0x8800) >> 9;
          _F1818 = ((result1_3711 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3712 >> 4)] | (result1_3711 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3712 & 0x07)] | (value1_3707 == 1 ? 0 : 0x40);
          F = (_F1818 & 0xFF);
          int value_3714 = (result_3706 & 0xffff);
          H = (value_3714 >>> 8);
          L = value_3714 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4B: {
          int _address1820;
          int address_3715 = (PC + 2) & 0xFFFF;
          int operand_3717 = read(address_3715, 0);
          int operand_3719 = read((address_3715 + 1) & 0xFFFF, 0);
          _address1820 = (operand_3719 << 8) | operand_3717;
          int wordNumber1_3720 = read(_address1820, 0);
          int wordNumber_3721 = read((_address1820 + 1) & 0xFFFF, 0);
          int value_3722 = ((wordNumber_3721 << 8) | wordNumber1_3720);
          B = (value_3722 >>> 8);
          C = value_3722 & 0xFF;
          MEMPTR = ((_address1820 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4C: {
          int _F1822;
          int value1_3723 = A;
          int value2_3724 = 0;
          int subtemp_3726 = value2_3724 - value1_3723;
          int lookup_3727 = ((value2_3724 & 0x88) >> 3) | ((value1_3723 & 0x88) >> 2) | ((subtemp_3726 & 0x88) >> 1);
          value2_3724 = subtemp_3726 & 0xff;
          _F1822 = ((subtemp_3726 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3727 & 0x07)] | OVERFLOW_SUB[(lookup_3727 >> 4)] | (SZ53[value2_3724] | (value2_3724 == 0 ? 0x40 : 0));
          F = (_F1822 & 0xFF);
          A = value2_3724;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4D: {
          int _nextPC1824;
          state.setIff1(state.isIff2());
          int jumpAddress2_3729;
          int wordNumber1_3731 = read(SP, 0);
          int wordNumber_3732 = read((SP + 1) & 0xFFFF, 0);
          int value_3730 = ((wordNumber_3732 << 8) | wordNumber1_3731);
          int wordNumber_3733 = SP;
          SP = ((wordNumber_3733 + 2) & 0xFFFF);
          jumpAddress2_3729 = value_3730;
          _nextPC1824 = jumpAddress2_3729;
          MEMPTR = _nextPC1824;
          PC = _nextPC1824;
          break;
      }
      case 0x4E: {
          state.setIntMode(InterruptionMode.IM0);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4F: {
          contend1x1(((I << 8) | R));
          if ((A > 0x7f)) {
              regRBit7 = 0x80;
              R = (A & 0x7f) | 0x80;
              PC = (PC + 2) & 0xFFFF;
              break;
          } else {
              regRBit7 = 0;
              R = (A & 0x7f);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_10(int opcode) {
    switch (opcode) {
      case 0x50: {
          int _F1827;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3736 = ((B << 8) | C);
          int value_3738 = io.in(port_3736);
          D = value_3738 & 0xFF;
          int value1_3739 = value_3738 & 0xFF;
          int value2_3740 = F;
          _F1827 = value2_3740;
          _F1827 = (_F1827 & 1) | (SZ53P[value1_3739] | (value1_3739 == 0 ? 0x40 : 0));
          F = (_F1827 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x51: {
          int read_3743 = ((B << 8) | C);
          io.out(read_3743, D);
          int read_3744 = ((B << 8) | C);
          MEMPTR = ((read_3744 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x52: {
          int _F1830;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3745 = ((H << 8) | L);
          int b_3746 = ((D << 8) | E);
          int result_3747 = (a_3745 - b_3746 - (F & 1));
          int value1_3748 = ((result_3747 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3749 = (((a_3745 & 0x8800 | (b_3746 & 0x8800) >> 1) | ((result_3747 & 0x18800) | ((result_3747 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3751 = value2_3749 & 0x33;
          i_3751 |= i_3751 << 1 & 0x04;
          int result1_3752 = i_3751 << 11 & 0x1A800;
          int lookup_3753 = (value2_3749 << 8 & 0x8800) >> 11 | (value2_3749 << 9 & 0x8800) >> 10 | (result1_3752 & 0x8800) >> 9;
          _F1830 = ((result1_3752 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3753 >> 4)] | (result1_3752 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3753 & 0x07)] | (value1_3748 != 0 ? 0 : 0x40);
          F = (_F1830 & 0xFF);
          int value_3755 = (result_3747 & 0xffff);
          H = (value_3755 >>> 8);
          L = value_3755 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x53: {
          int _address1832;
          int address_3756 = (PC + 2) & 0xFFFF;
          int operand_3758 = read(address_3756, 0);
          int operand_3760 = read((address_3756 + 1) & 0xFFFF, 0);
          _address1832 = (operand_3760 << 8) | operand_3758;
          int value_3761 = ((D << 8) | E);
          write(_address1832, (value_3761 & 0xFF));
          write((_address1832 + 1) & 0xFFFF, (value_3761 >>> 8));
          MEMPTR = ((_address1832 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x54: {
          int _F1834;
          int value1_3762 = A;
          int value2_3763 = 0;
          int subtemp_3765 = value2_3763 - value1_3762;
          int lookup_3766 = ((value2_3763 & 0x88) >> 3) | ((value1_3762 & 0x88) >> 2) | ((subtemp_3765 & 0x88) >> 1);
          value2_3763 = subtemp_3765 & 0xff;
          _F1834 = ((subtemp_3765 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3766 & 0x07)] | OVERFLOW_SUB[(lookup_3766 >> 4)] | (SZ53[value2_3763] | (value2_3763 == 0 ? 0x40 : 0));
          F = (_F1834 & 0xFF);
          A = value2_3763;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x55: {
          int _nextPC1836;
          state.setIff1(state.isIff2());
          int jumpAddress2_3768;
          int wordNumber1_3770 = read(SP, 0);
          int wordNumber_3771 = read((SP + 1) & 0xFFFF, 0);
          int value_3769 = ((wordNumber_3771 << 8) | wordNumber1_3770);
          int wordNumber_3772 = SP;
          SP = ((wordNumber_3772 + 2) & 0xFFFF);
          jumpAddress2_3768 = value_3769;
          _nextPC1836 = jumpAddress2_3768;
          MEMPTR = _nextPC1836;
          PC = _nextPC1836;
          break;
      }
      case 0x56: {
          state.setIntMode(InterruptionMode.IM1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x57: {
          int _F1838;
          contend1x1(((I << 8) | R));
          boolean iff2_3775 = state.isIff2();
          int value2_3777 = F;
          int value3_3778 = (iff2_3775 ? 1 : 0);
          _F1838 = value2_3777;
          _F1838 = (_F1838 & 1) | (SZ53[I] | (I == 0 ? 0x40 : 0)) | (value3_3778 != 0 ? 4 : 0);
          F = (_F1838 & 0xFF);
          A = I;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_11(int opcode) {
    switch (opcode) {
      case 0x58: {
          int _F1840;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3782 = ((B << 8) | C);
          int value_3784 = io.in(port_3782);
          E = value_3784 & 0xFF;
          int value1_3785 = value_3784 & 0xFF;
          int value2_3786 = F;
          _F1840 = value2_3786;
          _F1840 = (_F1840 & 1) | (SZ53P[value1_3785] | (value1_3785 == 0 ? 0x40 : 0));
          F = (_F1840 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x59: {
          int read_3789 = ((B << 8) | C);
          io.out(read_3789, E);
          int read_3790 = ((B << 8) | C);
          MEMPTR = ((read_3790 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5A: {
          int _F1843;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3791 = ((D << 8) | E);
          int b_3792 = ((H << 8) | L);
          int result_3793 = (a_3791 + b_3792 + (F & 1));
          int value1_3794 = ((result_3793 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3795 = (((a_3791 & 0x8800 | (b_3792 & 0x8800) >> 1) | ((result_3793 & 0x18800) | ((result_3793 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3797 = value2_3795 & 0x33;
          i_3797 |= (i_3797 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3798 = (i_3797 << 11) & 0x1A800;
          int lookup_3799 = (value2_3795 << 8 & 0x8800) >> 11 | (value2_3795 << 9 & 0x8800) >> 10 | (result1_3798 & 0x8800) >> 9;
          _F1843 = ((result1_3798 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3799 >> 4)] | (result1_3798 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3799 & 0x07)] | (value1_3794 == 1 ? 0 : 0x40);
          F = (_F1843 & 0xFF);
          int value_3801 = (result_3793 & 0xffff);
          H = (value_3801 >>> 8);
          L = value_3801 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5B: {
          int _address1845;
          int address_3802 = (PC + 2) & 0xFFFF;
          int operand_3804 = read(address_3802, 0);
          int operand_3806 = read((address_3802 + 1) & 0xFFFF, 0);
          _address1845 = (operand_3806 << 8) | operand_3804;
          int wordNumber1_3807 = read(_address1845, 0);
          int wordNumber_3808 = read((_address1845 + 1) & 0xFFFF, 0);
          int value_3809 = ((wordNumber_3808 << 8) | wordNumber1_3807);
          D = (value_3809 >>> 8);
          E = value_3809 & 0xFF;
          MEMPTR = ((_address1845 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5C: {
          int _F1847;
          int value1_3810 = A;
          int value2_3811 = 0;
          int subtemp_3813 = value2_3811 - value1_3810;
          int lookup_3814 = ((value2_3811 & 0x88) >> 3) | ((value1_3810 & 0x88) >> 2) | ((subtemp_3813 & 0x88) >> 1);
          value2_3811 = subtemp_3813 & 0xff;
          _F1847 = ((subtemp_3813 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3814 & 0x07)] | OVERFLOW_SUB[(lookup_3814 >> 4)] | (SZ53[value2_3811] | (value2_3811 == 0 ? 0x40 : 0));
          F = (_F1847 & 0xFF);
          A = value2_3811;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5D: {
          int _nextPC1849;
          state.setIff1(state.isIff2());
          int jumpAddress2_3816;
          int wordNumber1_3818 = read(SP, 0);
          int wordNumber_3819 = read((SP + 1) & 0xFFFF, 0);
          int value_3817 = ((wordNumber_3819 << 8) | wordNumber1_3818);
          int wordNumber_3820 = SP;
          SP = ((wordNumber_3820 + 2) & 0xFFFF);
          jumpAddress2_3816 = value_3817;
          _nextPC1849 = jumpAddress2_3816;
          MEMPTR = _nextPC1849;
          PC = _nextPC1849;
          break;
      }
      case 0x5E: {
          state.setIntMode(InterruptionMode.IM2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5F: {
          int _F1851;
          contend1x1(((I << 8) | R));
          int value2_3825 = F;
          int value3_3826 = (state.isIff2() ? 1 : 0);
          _F1851 = value2_3825;
          _F1851 = (_F1851 & 1) | (SZ53[R] | (R == 0 ? 0x40 : 0)) | (value3_3826 != 0 ? 4 : 0);
          int result_3828 = _F1851 & 0xFF;
          F = result_3828;
          A = R;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_12(int opcode) {
    switch (opcode) {
      case 0x60: {
          int _F1853;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3830 = ((B << 8) | C);
          int value_3832 = io.in(port_3830);
          H = value_3832 & 0xFF;
          int value1_3833 = value_3832 & 0xFF;
          int value2_3834 = F;
          _F1853 = value2_3834;
          _F1853 = (_F1853 & 1) | (SZ53P[value1_3833] | (value1_3833 == 0 ? 0x40 : 0));
          F = (_F1853 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x61: {
          int read_3837 = ((B << 8) | C);
          io.out(read_3837, H);
          int read_3838 = ((B << 8) | C);
          MEMPTR = ((read_3838 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x62: {
          int _F1856;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3839 = ((H << 8) | L);
          int b_3840 = ((H << 8) | L);
          int result_3841 = (a_3839 - b_3840 - (F & 1));
          int value1_3842 = ((result_3841 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3843 = (((a_3839 & 0x8800 | (b_3840 & 0x8800) >> 1) | ((result_3841 & 0x18800) | ((result_3841 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3845 = value2_3843 & 0x33;
          i_3845 |= i_3845 << 1 & 0x04;
          int result1_3846 = i_3845 << 11 & 0x1A800;
          int lookup_3847 = (value2_3843 << 8 & 0x8800) >> 11 | (value2_3843 << 9 & 0x8800) >> 10 | (result1_3846 & 0x8800) >> 9;
          _F1856 = ((result1_3846 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3847 >> 4)] | (result1_3846 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3847 & 0x07)] | (value1_3842 != 0 ? 0 : 0x40);
          F = (_F1856 & 0xFF);
          int value_3849 = (result_3841 & 0xffff);
          H = (value_3849 >>> 8);
          L = value_3849 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x63: {
          int _address1858;
          int address_3850 = (PC + 2) & 0xFFFF;
          int operand_3852 = read(address_3850, 0);
          int operand_3854 = read((address_3850 + 1) & 0xFFFF, 0);
          _address1858 = (operand_3854 << 8) | operand_3852;
          int value_3855 = ((H << 8) | L);
          write(_address1858, (value_3855 & 0xFF));
          write((_address1858 + 1) & 0xFFFF, (value_3855 >>> 8));
          MEMPTR = ((_address1858 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x64: {
          int _F1860;
          int value1_3856 = A;
          int value2_3857 = 0;
          int subtemp_3859 = value2_3857 - value1_3856;
          int lookup_3860 = ((value2_3857 & 0x88) >> 3) | ((value1_3856 & 0x88) >> 2) | ((subtemp_3859 & 0x88) >> 1);
          value2_3857 = subtemp_3859 & 0xff;
          _F1860 = ((subtemp_3859 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3860 & 0x07)] | OVERFLOW_SUB[(lookup_3860 >> 4)] | (SZ53[value2_3857] | (value2_3857 == 0 ? 0x40 : 0));
          F = (_F1860 & 0xFF);
          A = value2_3857;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x65: {
          int _nextPC1862;
          state.setIff1(state.isIff2());
          int jumpAddress2_3862;
          int wordNumber1_3864 = read(SP, 0);
          int wordNumber_3865 = read((SP + 1) & 0xFFFF, 0);
          int value_3863 = ((wordNumber_3865 << 8) | wordNumber1_3864);
          int wordNumber_3866 = SP;
          SP = ((wordNumber_3866 + 2) & 0xFFFF);
          jumpAddress2_3862 = value_3863;
          _nextPC1862 = jumpAddress2_3862;
          MEMPTR = _nextPC1862;
          PC = _nextPC1862;
          break;
      }
      case 0x66: {
          state.setIntMode(InterruptionMode.IM0);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x67: {
          int _F1864;
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int reg_A_3868 = A;
          int nibble1_3869 = (reg_A_3868 & 0x00F0) >> 4;
          int nibble2_3870 = reg_A_3868 & 0x000F;
          int temp_3871 = read(((H << 8) | L), 0);
          contend4x1(((H << 8) | L));
          int nibble3_3872 = (temp_3871 & 0x00F0) >> 4;
          int nibble4_3873 = temp_3871 & 0x000F;
          write(((H << 8) | L), ((nibble2_3870 << 4) | nibble3_3872));
          int value_3874 = ((nibble1_3869 << 4) | nibble4_3873);
          int value2_3876 = reg_A_3868;
          int value3_3877 = F & 1;
          _F1864 = value3_3877;
          value2_3876 = (value2_3876 & 0xf0) | (temp_3871 & 0x0f);
          _F1864 = (_F1864 & 1) | (SZ53P[value2_3876] | (value2_3876 == 0 ? 0x40 : 0));
          F = (_F1864 & 0xFF);
          A = value_3874;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_13(int opcode) {
    switch (opcode) {
      case 0x68: {
          int _F1866;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3880 = ((B << 8) | C);
          int value_3882 = io.in(port_3880);
          L = value_3882 & 0xFF;
          int value1_3883 = value_3882 & 0xFF;
          int value2_3884 = F;
          _F1866 = value2_3884;
          _F1866 = (_F1866 & 1) | (SZ53P[value1_3883] | (value1_3883 == 0 ? 0x40 : 0));
          F = (_F1866 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x69: {
          int read_3887 = ((B << 8) | C);
          io.out(read_3887, L);
          int read_3888 = ((B << 8) | C);
          MEMPTR = ((read_3888 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6A: {
          int _F1869;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3889 = ((H << 8) | L);
          int b_3890 = ((H << 8) | L);
          int result_3891 = (a_3889 + b_3890 + (F & 1));
          int value1_3892 = ((result_3891 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3893 = (((a_3889 & 0x8800 | (b_3890 & 0x8800) >> 1) | ((result_3891 & 0x18800) | ((result_3891 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3895 = value2_3893 & 0x33;
          i_3895 |= (i_3895 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3896 = (i_3895 << 11) & 0x1A800;
          int lookup_3897 = (value2_3893 << 8 & 0x8800) >> 11 | (value2_3893 << 9 & 0x8800) >> 10 | (result1_3896 & 0x8800) >> 9;
          _F1869 = ((result1_3896 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3897 >> 4)] | (result1_3896 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3897 & 0x07)] | (value1_3892 == 1 ? 0 : 0x40);
          F = (_F1869 & 0xFF);
          int value_3899 = (result_3891 & 0xffff);
          H = (value_3899 >>> 8);
          L = value_3899 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6B: {
          int _address1871;
          int address_3900 = (PC + 2) & 0xFFFF;
          int operand_3902 = read(address_3900, 0);
          int operand_3904 = read((address_3900 + 1) & 0xFFFF, 0);
          _address1871 = (operand_3904 << 8) | operand_3902;
          int wordNumber1_3905 = read(_address1871, 0);
          int wordNumber_3906 = read((_address1871 + 1) & 0xFFFF, 0);
          int value_3907 = ((wordNumber_3906 << 8) | wordNumber1_3905);
          H = (value_3907 >>> 8);
          L = value_3907 & 0xFF;
          MEMPTR = ((_address1871 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6C: {
          int _F1873;
          int value1_3908 = A;
          int value2_3909 = 0;
          int subtemp_3911 = value2_3909 - value1_3908;
          int lookup_3912 = ((value2_3909 & 0x88) >> 3) | ((value1_3908 & 0x88) >> 2) | ((subtemp_3911 & 0x88) >> 1);
          value2_3909 = subtemp_3911 & 0xff;
          _F1873 = ((subtemp_3911 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3912 & 0x07)] | OVERFLOW_SUB[(lookup_3912 >> 4)] | (SZ53[value2_3909] | (value2_3909 == 0 ? 0x40 : 0));
          F = (_F1873 & 0xFF);
          A = value2_3909;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6D: {
          int _nextPC1875;
          state.setIff1(state.isIff2());
          int jumpAddress2_3914;
          int wordNumber1_3916 = read(SP, 0);
          int wordNumber_3917 = read((SP + 1) & 0xFFFF, 0);
          int value_3915 = ((wordNumber_3917 << 8) | wordNumber1_3916);
          int wordNumber_3918 = SP;
          SP = ((wordNumber_3918 + 2) & 0xFFFF);
          jumpAddress2_3914 = value_3915;
          _nextPC1875 = jumpAddress2_3914;
          MEMPTR = _nextPC1875;
          PC = _nextPC1875;
          break;
      }
      case 0x6E: {
          state.setIntMode(InterruptionMode.IM0);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6F: {
          int _F1877;
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int reg_A_3920 = A;
          int nibble1_3921 = (reg_A_3920 & 0x00F0) >> 4;
          int nibble2_3922 = reg_A_3920 & 0x000F;
          int temp_3923 = read(((H << 8) | L), 0);
          contend4x1(((H << 8) | L));
          int nibble3_3924 = (temp_3923 & 0x00F0) >> 4;
          int nibble4_3925 = temp_3923 & 0x000F;
          write(((H << 8) | L), ((nibble4_3925 << 4) | nibble2_3922));
          int value_3926 = ((nibble1_3921 << 4) | nibble3_3924);
          int value2_3928 = reg_A_3920;
          int value3_3929 = F & 1;
          _F1877 = value3_3929;
          value2_3928 = (value2_3928 & 0xf0) | (temp_3923 >> 4);
          _F1877 = (_F1877 & 1) | (SZ53P[value2_3928] | (value2_3928 == 0 ? 0x40 : 0));
          F = (_F1877 & 0xFF);
          A = value_3926;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_14(int opcode) {
    switch (opcode) {
      case 0x70: {
          int _F1879;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3932 = ((B << 8) | C);
          int value_3934 = io.in(port_3932);
          int value1_3935 = value_3934 & 0xFF;
          int value2_3936 = F;
          _F1879 = value2_3936;
          _F1879 = (_F1879 & 1) | (SZ53P[value1_3935] | (value1_3935 == 0 ? 0x40 : 0));
          F = (_F1879 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x71: {
          int read_3939 = ((B << 8) | C);
          io.out(read_3939, 0);
          int read_3940 = ((B << 8) | C);
          MEMPTR = ((read_3940 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _F1882;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3941 = ((H << 8) | L);
          int result_3942 = (a_3941 - SP - (F & 1));
          int value1_3943 = ((result_3942 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3944 = (((a_3941 & 0x8800 | (SP & 0x8800) >> 1) | ((result_3942 & 0x18800) | ((result_3942 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3946 = value2_3944 & 0x33;
          i_3946 |= i_3946 << 1 & 0x04;
          int result1_3947 = i_3946 << 11 & 0x1A800;
          int lookup_3948 = (value2_3944 << 8 & 0x8800) >> 11 | (value2_3944 << 9 & 0x8800) >> 10 | (result1_3947 & 0x8800) >> 9;
          _F1882 = ((result1_3947 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3948 >> 4)] | (result1_3947 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3948 & 0x07)] | (value1_3943 != 0 ? 0 : 0x40);
          F = (_F1882 & 0xFF);
          int value_3950 = (result_3942 & 0xffff);
          H = (value_3950 >>> 8);
          L = value_3950 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address1884;
          int address_3951 = (PC + 2) & 0xFFFF;
          int operand_3953 = read(address_3951, 0);
          int operand_3955 = read((address_3951 + 1) & 0xFFFF, 0);
          _address1884 = (operand_3955 << 8) | operand_3953;
          write(_address1884, (SP & 0xFF));
          write((_address1884 + 1) & 0xFFFF, (SP >>> 8));
          MEMPTR = ((_address1884 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _F1886;
          int value1_3956 = A;
          int value2_3957 = 0;
          int subtemp_3959 = value2_3957 - value1_3956;
          int lookup_3960 = ((value2_3957 & 0x88) >> 3) | ((value1_3956 & 0x88) >> 2) | ((subtemp_3959 & 0x88) >> 1);
          value2_3957 = subtemp_3959 & 0xff;
          _F1886 = ((subtemp_3959 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3960 & 0x07)] | OVERFLOW_SUB[(lookup_3960 >> 4)] | (SZ53[value2_3957] | (value2_3957 == 0 ? 0x40 : 0));
          F = (_F1886 & 0xFF);
          A = value2_3957;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _nextPC1888;
          state.setIff1(state.isIff2());
          int jumpAddress2_3962;
          int wordNumber1_3964 = read(SP, 0);
          int wordNumber_3965 = read((SP + 1) & 0xFFFF, 0);
          int value_3963 = ((wordNumber_3965 << 8) | wordNumber1_3964);
          int wordNumber_3966 = SP;
          SP = ((wordNumber_3966 + 2) & 0xFFFF);
          jumpAddress2_3962 = value_3963;
          _nextPC1888 = jumpAddress2_3962;
          MEMPTR = _nextPC1888;
          PC = _nextPC1888;
          break;
      }
      case 0x76: {
          state.setIntMode(InterruptionMode.IM1);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x77: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_15(int opcode) {
    switch (opcode) {
      case 0x78: {
          int _F1891;
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_3969 = ((B << 8) | C);
          int value_3971 = io.in(port_3969);
          A = value_3971 & 0xFF;
          int value1_3972 = value_3971 & 0xFF;
          int value2_3973 = F;
          _F1891 = value2_3973;
          _F1891 = (_F1891 & 1) | (SZ53P[value1_3972] | (value1_3972 == 0 ? 0x40 : 0));
          F = (_F1891 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x79: {
          int read_3976 = ((B << 8) | C);
          io.out(read_3976, A);
          int read_3977 = ((B << 8) | C);
          MEMPTR = ((read_3977 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7A: {
          int _F1894;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int b_3978 = ((H << 8) | L);
          int result_3979 = (SP + b_3978 + (F & 1));
          int value1_3980 = ((result_3979 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3981 = (((SP & 0x8800 | (b_3978 & 0x8800) >> 1) | ((result_3979 & 0x18800) | ((result_3979 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3983 = value2_3981 & 0x33;
          i_3983 |= (i_3983 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3984 = (i_3983 << 11) & 0x1A800;
          int lookup_3985 = (value2_3981 << 8 & 0x8800) >> 11 | (value2_3981 << 9 & 0x8800) >> 10 | (result1_3984 & 0x8800) >> 9;
          _F1894 = ((result1_3984 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3985 >> 4)] | (result1_3984 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3985 & 0x07)] | (value1_3980 == 1 ? 0 : 0x40);
          F = (_F1894 & 0xFF);
          int value_3987 = (result_3979 & 0xffff);
          H = (value_3987 >>> 8);
          L = value_3987 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7B: {
          int _address1896;
          int address_3988 = (PC + 2) & 0xFFFF;
          int operand_3990 = read(address_3988, 0);
          int operand_3992 = read((address_3988 + 1) & 0xFFFF, 0);
          _address1896 = (operand_3992 << 8) | operand_3990;
          int wordNumber1_3993 = read(_address1896, 0);
          int wordNumber_3994 = read((_address1896 + 1) & 0xFFFF, 0);
          SP = ((wordNumber_3994 << 8) | wordNumber1_3993);
          MEMPTR = ((_address1896 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7C: {
          int _F1898;
          int value1_3995 = A;
          int value2_3996 = 0;
          int subtemp_3998 = value2_3996 - value1_3995;
          int lookup_3999 = ((value2_3996 & 0x88) >> 3) | ((value1_3995 & 0x88) >> 2) | ((subtemp_3998 & 0x88) >> 1);
          value2_3996 = subtemp_3998 & 0xff;
          _F1898 = ((subtemp_3998 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3999 & 0x07)] | OVERFLOW_SUB[(lookup_3999 >> 4)] | (SZ53[value2_3996] | (value2_3996 == 0 ? 0x40 : 0));
          F = (_F1898 & 0xFF);
          A = value2_3996;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7D: {
          int _nextPC1900;
          state.setIff1(state.isIff2());
          int jumpAddress2_4001;
          int wordNumber1_4003 = read(SP, 0);
          int wordNumber_4004 = read((SP + 1) & 0xFFFF, 0);
          int value_4002 = ((wordNumber_4004 << 8) | wordNumber1_4003);
          int wordNumber_4005 = SP;
          SP = ((wordNumber_4005 + 2) & 0xFFFF);
          jumpAddress2_4001 = value_4002;
          _nextPC1900 = jumpAddress2_4001;
          MEMPTR = _nextPC1900;
          PC = _nextPC1900;
          break;
      }
      case 0x7E: {
          state.setIntMode(InterruptionMode.IM2);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_16(int opcode) {
    switch (opcode) {
      case 0x80: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x87: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_17(int opcode) {
    switch (opcode) {
      case 0x88: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_18(int opcode) {
    switch (opcode) {
      case 0x90: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x97: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_19(int opcode) {
    switch (opcode) {
      case 0x98: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_20(int opcode) {
    switch (opcode) {
      case 0xA0: {
          int _F1935 = 0;
          int read_4007 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4007);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          E = (E + 1) & 0xFF;
          if (E == 0) {
              D = (D + 1) & 0xFF;
          }
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int byteTemp_4008 = read_4007 + A;
          int value1_4009 = F;
          int value2_4010 = byteTemp_4008 & 0xFF;
          int value3_4011 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1935 = value1_4009;
          _F1935 = (_F1935 & 0xC1) | (value3_4011 != 0 ? 4 : 0) | (value2_4010 & 8) | ((value2_4010 & 0x02) != 0 ? 0x20 : 0);
          F = _F1935;
          contend2x1((((D << 8) | E) - 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F1937 = 0;
          MEMPTR = (MEMPTR + 1) & 0xFFFF;
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int value_4015 = read(((H << 8) | L), 0);
          int carry_4017 = F & 1;
          int value3_4020 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1937 = 0;
          int bytetemp_4021 = A - value_4015;
          int lookup_4022 = ((A & 0x08) >> 3) | ((value_4015 & 0x08) >> 2) | ((bytetemp_4021 & 0x08) >> 1);
          _F1937 = (_F1937 & 1) | (value3_4020 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4022] | (bytetemp_4021 != 0 ? 0 : 0x40) | (bytetemp_4021 & 0x80);
          if ((_F1937 & 0x10) != 0) {
              bytetemp_4021--;
          }
          _F1937 |= (bytetemp_4021 & 8) | ((bytetemp_4021 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1937 & 0xFF);
          F = ((F & -2) | carry_4017);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          contend5x1((((H << 8) | L) - 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F1939 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_4024 = ((B << 8) | C);
          int in_4025 = io.in(port_4024);
          int hlValue_4027 = ((H << 8) | L);
          write(hlValue_4027, in_4025);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4033 = in_4025 & 0xff;
          int initemp2_4034 = (initemp_4033 + C + 1) & 0xff;
          _F1939 = ((initemp_4033 & 0x80) != 0 ? 2 : 0) | ((initemp2_4034 < initemp_4033) ? 0x11 : 0) | (PARITY[((initemp2_4034 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1939 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F1941 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4039 = ((H << 8) | L);
          int valueFromHL_4041 = read(hlValue_4039, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4041);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          int outitemp2_4048 = (valueFromHL_4041 + L) & 0xff;
          _F1941 = ((valueFromHL_4041 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4048 < valueFromHL_4041) ? 0x11 : 0) | (PARITY[((outitemp2_4048 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1941 & 0xFF);
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_21(int opcode) {
    switch (opcode) {
      case 0xA8: {
          int _F1947 = 0;
          int read_4050 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4050);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          E = (E - 1) & 0xFF;
          if (E == 0xFF) {
              D = (D - 1) & 0xFF;
          }
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int byteTemp_4051 = read_4050 + A;
          int value1_4052 = F;
          int value2_4053 = byteTemp_4051 & 0xFF;
          int value3_4054 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1947 = value1_4052;
          _F1947 = (_F1947 & 0xC1) | (value3_4054 != 0 ? 4 : 0) | (value2_4053 & 8) | ((value2_4053 & 0x02) != 0 ? 0x20 : 0);
          F = _F1947;
          contend2x1((((D << 8) | E) + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F1949 = 0;
          MEMPTR = (MEMPTR - 1) & 0xFFFF;
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int lastCarry_4058 = F & 1;
          int value1_4059 = read(((H << 8) | L), 0);
          int value3_4061 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1949 = A;
          int bytetemp_4062 = A - value1_4059;
          int lookup_4063 = ((A & 0x08) >> 3) | ((value1_4059 & 0x08) >> 2) | ((bytetemp_4062 & 0x08) >> 1);
          _F1949 = (_F1949 & 1) | (value3_4061 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4063] | (bytetemp_4062 != 0 ? 0 : 0x40) | (bytetemp_4062 & 0x80);
          if ((_F1949 & 0x10) != 0) {
              bytetemp_4062--;
          }
          _F1949 |= (bytetemp_4062 & 8) | ((bytetemp_4062 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1949 & 0xFF);
          F = (F | lastCarry_4058);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          contend5x1((((H << 8) | L) + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F1951 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
          int port_4065 = ((B << 8) | C);
          int in_4066 = io.in(port_4065);
          int hlValue_4067 = ((H << 8) | L);
          write(hlValue_4067, in_4066);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4073 = in_4066 & 0xff;
          int initemp2_4074 = (initemp_4073 + C + -1) & 0xff;
          _F1951 = ((initemp_4073 & 0x80) != 0 ? 2 : 0) | ((initemp2_4074 < initemp_4073) ? 0x11 : 0) | (PARITY[((initemp2_4074 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1951 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F1953 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4079 = ((H << 8) | L);
          int valueFromHL_4081 = read(hlValue_4079, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4081);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          int outitemp2_4088 = (valueFromHL_4081 + L) & 0xff;
          _F1953 = ((valueFromHL_4081 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4088 < valueFromHL_4081) ? 0x11 : 0) | (PARITY[((outitemp2_4088 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1953 & 0xFF);
          MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_22(int opcode) {
    switch (opcode) {
      case 0xB0: {
          int _F1959 = 0;
          int read_4090 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4090);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          E = (E + 1) & 0xFF;
          if (E == 0) {
              D = (D + 1) & 0xFF;
          }
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int byteTemp_4091 = read_4090 + A;
          int value1_4092 = F;
          int value2_4093 = byteTemp_4091 & 0xFF;
          int value3_4094 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1959 = value1_4092;
          _F1959 = (_F1959 & 0xC1) | (value3_4094 != 0 ? 4 : 0) | (value2_4093 & 8) | ((value2_4093 & 0x02) != 0 ? 0x20 : 0);
          F = _F1959;
          if ((((B << 8) | C) != 0)) {
              int nextPC_4098 = PC;
              int newValue_4099;
              if (nextPC_4098 != -1) {
                  newValue_4099 = (nextPC_4098 + 1) & 0xFFFF;
              } else {
                  newValue_4099 = MEMPTR;
              }
              MEMPTR = newValue_4099;
              contend2x1((((D << 8) | E) - 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((D << 8) | E) - 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4099;
              newValue_4099 = MEMPTR;
              MEMPTR = newValue_4099;
              contend2x1((((D << 8) | E) - 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB1: {
          int _F1961 = 0;
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int value_4101 = read(((H << 8) | L), 0);
          int carry_4103 = F & 1;
          int value3_4106 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1961 = 0;
          int bytetemp_4107 = A - value_4101;
          int lookup_4108 = ((A & 0x08) >> 3) | ((value_4101 & 0x08) >> 2) | ((bytetemp_4107 & 0x08) >> 1);
          _F1961 = (_F1961 & 1) | (value3_4106 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4108] | (bytetemp_4107 != 0 ? 0 : 0x40) | (bytetemp_4107 & 0x80);
          if ((_F1961 & 0x10) != 0) {
              bytetemp_4107--;
          }
          _F1961 |= (bytetemp_4107 & 8) | ((bytetemp_4107 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1961 & 0xFF);
          F = ((F & -2) | carry_4103);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          if (((F & 0x40) == 0 && ((B << 8) | C) != 0)) {
              int nextPC_4110 = PC;
              int newValue_4111;
              if (nextPC_4110 != -1) {
                  newValue_4111 = (nextPC_4110 + 1) & 0xFFFF;
              } else {
                  newValue_4111 = (MEMPTR + 1) & 0xFFFF;
              }
              MEMPTR = newValue_4111;
              contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4111;
              newValue_4111 = (MEMPTR + 1) & 0xFFFF;
              MEMPTR = newValue_4111;
              contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB2: {
          int _F1963 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_4113 = ((B << 8) | C);
          int in_4114 = io.in(port_4113);
          int hlValue_4116 = ((H << 8) | L);
          write(hlValue_4116, in_4114);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4122 = in_4114 & 0xff;
          int initemp2_4123 = (initemp_4122 + C + 1) & 0xff;
          _F1963 = ((initemp_4122 & 0x80) != 0 ? 2 : 0) | ((initemp2_4123 < initemp_4122) ? 0x11 : 0) | (PARITY[((initemp2_4123 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1963 & 0xFF);
          if ((B != 0)) {
              if (PC != -1)
                  contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB3: {
          int _F1965 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4128 = ((H << 8) | L);
          int valueFromHL_4130 = read(hlValue_4128, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4130);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          int outitemp2_4137 = (valueFromHL_4130 + L) & 0xff;
          _F1965 = ((valueFromHL_4130 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4137 < valueFromHL_4130) ? 0x11 : 0) | (PARITY[((outitemp2_4137 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1965 & 0xFF);
          if ((B != 0)) {
              MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1(((B << 8) | C));
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_23(int opcode) {
    switch (opcode) {
      case 0xB8: {
          int _F1971 = 0;
          int read_4139 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4139);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          E = (E - 1) & 0xFF;
          if (E == 0xFF) {
              D = (D - 1) & 0xFF;
          }
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int byteTemp_4140 = read_4139 + A;
          int value1_4141 = F;
          int value2_4142 = byteTemp_4140 & 0xFF;
          int value3_4143 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1971 = value1_4141;
          _F1971 = (_F1971 & 0xC1) | (value3_4143 != 0 ? 4 : 0) | (value2_4142 & 8) | ((value2_4142 & 0x02) != 0 ? 0x20 : 0);
          F = _F1971;
          if ((((B << 8) | C) != 0)) {
              int nextPC_4147 = PC;
              int newValue_4148;
              if (nextPC_4147 != -1) {
                  newValue_4148 = (nextPC_4147 + 1) & 0xFFFF;
              } else {
                  newValue_4148 = MEMPTR;
              }
              MEMPTR = newValue_4148;
              contend2x1((((D << 8) | E) + 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((D << 8) | E) + 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4148;
              newValue_4148 = MEMPTR;
              MEMPTR = newValue_4148;
              contend2x1((((D << 8) | E) + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB9: {
          int _F1973 = 0;
          C = (C - 1) & 0xFF;
          if (C == 0xFF) {
              B = (B - 1) & 0xFF;
          }
          int lastCarry_4150 = F & 1;
          int value1_4151 = read(((H << 8) | L), 0);
          int value3_4153 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1973 = A;
          int bytetemp_4154 = A - value1_4151;
          int lookup_4155 = ((A & 0x08) >> 3) | ((value1_4151 & 0x08) >> 2) | ((bytetemp_4154 & 0x08) >> 1);
          _F1973 = (_F1973 & 1) | (value3_4153 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4155] | (bytetemp_4154 != 0 ? 0 : 0x40) | (bytetemp_4154 & 0x80);
          if ((_F1973 & 0x10) != 0) {
              bytetemp_4154--;
          }
          _F1973 |= (bytetemp_4154 & 8) | ((bytetemp_4154 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1973 & 0xFF);
          F = (F | lastCarry_4150);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          if (((F & 0x40) == 0 && ((B << 8) | C) != 0)) {
              int nextPC_4157 = PC;
              int newValue_4158;
              if (nextPC_4157 != -1) {
                  newValue_4158 = (nextPC_4157 + 1) & 0xFFFF;
              } else {
                  newValue_4158 = (MEMPTR + -1) & 0xFFFF;
              }
              MEMPTR = newValue_4158;
              contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4158;
              newValue_4158 = (MEMPTR + -1) & 0xFFFF;
              MEMPTR = newValue_4158;
              contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xBA: {
          int _F1975 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
          int port_4160 = ((B << 8) | C);
          int in_4161 = io.in(port_4160);
          int hlValue_4162 = ((H << 8) | L);
          write(hlValue_4162, in_4161);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4168 = in_4161 & 0xff;
          int initemp2_4169 = (initemp_4168 + C + -1) & 0xff;
          _F1975 = ((initemp_4168 & 0x80) != 0 ? 2 : 0) | ((initemp2_4169 < initemp_4168) ? 0x11 : 0) | (PARITY[((initemp2_4169 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1975 & 0xFF);
          if ((B != 0)) {
              if (PC != -1)
                  contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xBB: {
          int _F1977 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4174 = ((H << 8) | L);
          int valueFromHL_4176 = read(hlValue_4174, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4176);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          int outitemp2_4183 = (valueFromHL_4176 + L) & 0xff;
          _F1977 = ((valueFromHL_4176 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4183 < valueFromHL_4176) ? 0x11 : 0) | (PARITY[((outitemp2_4183 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1977 & 0xFF);
          if ((B != 0)) {
              MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
              if (PC != -1)
                  contend5x1(((B << 8) | C));
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xBC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_24(int opcode) {
    switch (opcode) {
      case 0xC0: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC1: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC3: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_25(int opcode) {
    switch (opcode) {
      case 0xC8: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC9: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCA: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCB: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xCF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_26(int opcode) {
    switch (opcode) {
      case 0xD0: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD1: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD3: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_27(int opcode) {
    switch (opcode) {
      case 0xD8: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD9: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDB: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_28(int opcode) {
    switch (opcode) {
      case 0xE0: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE1: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE3: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_29(int opcode) {
    switch (opcode) {
      case 0xE8: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE9: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEA: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEB: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xED: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_30(int opcode) {
    switch (opcode) {
      case 0xF0: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF1: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF3: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF4: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF5: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF7: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeED_31(int opcode) {
    switch (opcode) {
      case 0xF8: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF9: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFA: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFB: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFC: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFE: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFF: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeED");
    }
  }

  private void decodeFD(int opcode) {
    switch (opcode >> 3) {
      case 0: decodeFD_0(opcode);
        break;
      case 1: decodeFD_1(opcode);
        break;
      case 2: decodeFD_2(opcode);
        break;
      case 3: decodeFD_3(opcode);
        break;
      case 4: decodeFD_4(opcode);
        break;
      case 5: decodeFD_5(opcode);
        break;
      case 6: decodeFD_6(opcode);
        break;
      case 7: decodeFD_7(opcode);
        break;
      case 8: decodeFD_8(opcode);
        break;
      case 9: decodeFD_9(opcode);
        break;
      case 10: decodeFD_10(opcode);
        break;
      case 11: decodeFD_11(opcode);
        break;
      case 12: decodeFD_12(opcode);
        break;
      case 13: decodeFD_13(opcode);
        break;
      case 14: decodeFD_14(opcode);
        break;
      case 15: decodeFD_15(opcode);
        break;
      case 16: decodeFD_16(opcode);
        break;
      case 17: decodeFD_17(opcode);
        break;
      case 18: decodeFD_18(opcode);
        break;
      case 19: decodeFD_19(opcode);
        break;
      case 20: decodeFD_20(opcode);
        break;
      case 21: decodeFD_21(opcode);
        break;
      case 22: decodeFD_22(opcode);
        break;
      case 23: decodeFD_23(opcode);
        break;
      case 24: decodeFD_24(opcode);
        break;
      case 25: decodeFD_25(opcode);
        break;
      case 26: decodeFD_26(opcode);
        break;
      case 27: decodeFD_27(opcode);
        break;
      case 28: decodeFD_28(opcode);
        break;
      case 29: decodeFD_29(opcode);
        break;
      case 30: decodeFD_30(opcode);
        break;
      case 31: decodeFD_31(opcode);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_0(int opcode) {
    switch (opcode) {
      case 0x00: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x01: {
          int address_4253 = (PC + 2) & 0xFFFF;
          int operand_4255 = read(address_4253, 0);
          int operand_4257 = read((address_4253 + 1) & 0xFFFF, 0);
          int value_4258 = ((operand_4257 << 8) | operand_4255);
          B = (value_4258 >>> 8);
          C = value_4258 & 0xFF;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _address2066 = (B << 8) | C;
          write(_address2066, A);
          MEMPTR = ((A << 8) | ((_address2066 + 1) & 0xff));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x03: {
          int read_4259 = ((B << 8) | C);
          int value_4260 = (read_4259 + 1) & 0xFFFF;
          B = (value_4260 >>> 8);
          C = value_4260 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F2069;
          int value1_4261 = B;
          int value2_4262 = F;
          _F2069 = value2_4262;
          value1_4261++;
          value1_4261 &= 0xff;
          _F2069 = (_F2069 & 1) | (value1_4261 == 0x80 ? 4 : 0) | ((value1_4261 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4261 & 0xff] | (value1_4261 == 0 ? 0x40 : 0));
          int result_4264 = value1_4261 & 0xFF;
          F = (_F2069 & 0xFF);
          B = result_4264;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F2071;
          int value1_4265 = B;
          int value2_4266 = F;
          _F2071 = value2_4266;
          _F2071 = (_F2071 & 1) | ((value1_4265 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4265--;
          value1_4265 &= 0xff;
          _F2071 |= (value1_4265 == 0x7f ? 4 : 0) | (SZ53[value1_4265 & 0xff] | (value1_4265 == 0 ? 0x40 : 0));
          int result_4268 = value1_4265 & 0xFF;
          F = (_F2071 & 0xFF);
          B = result_4268;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          int operand_4269 = read((PC + 2) & 0xFFFF, 0);
          B = operand_4269;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F2074;
          int value1_4270 = A;
          int value2_4271 = F;
          _F2074 = value2_4271;
          value1_4270 = (value1_4270 << 1) | (value1_4270 >> 7);
          _F2074 = (_F2074 & 0xC4) | (value1_4270 & 0x29);
          int result_4273 = value1_4270 & 0xFF;
          F = _F2074;
          A = result_4273;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_1(int opcode) {
    switch (opcode) {
      case 0x08: {
          int v1_4274 = ((A << 8) | F);
          int v2_4275 = _AF;
          A = (v2_4275 >>> 8);
          F = v2_4275 & 0xFF;
          _AF = v1_4274;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F2077;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int b_4276 = ((B << 8) | C);
          int result_4277 = (IY + b_4276);
          int value1_4278 = ((IY & 0x0800) >> 4 | result_4277 >> 11);
          int value2_4279 = F;
          int value3_4280 = (b_4276 >> 11) & 1;
          _F2077 = value2_4279;
          int add16temp_4281 = value1_4278 << 11;
          int lookup_4282 = (((value1_4278 << 4) & 0x0800) >> 11) | ((value3_4280 << 11) >> 10) | ((add16temp_4281 & 0x0800) >> 9);
          _F2077 = (_F2077 & 0xC4) | ((add16temp_4281 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4281 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4282];
          F = (_F2077 & 0xFF);
          IY = (result_4277 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _address2079 = (B << 8) | C;
          int value_4284 = read(_address2079, 0);
          A = value_4284;
          MEMPTR = ((_address2079 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int value_4285 = (((B << 8) | C) - 1) & 0xFFFF;
          B = (value_4285 >>> 8);
          C = value_4285 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F2082;
          int value1_4286 = C;
          int value2_4287 = F;
          _F2082 = value2_4287;
          value1_4286++;
          value1_4286 &= 0xff;
          _F2082 = (_F2082 & 1) | (value1_4286 == 0x80 ? 4 : 0) | ((value1_4286 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4286 & 0xff] | (value1_4286 == 0 ? 0x40 : 0));
          int result_4289 = value1_4286 & 0xFF;
          F = (_F2082 & 0xFF);
          C = result_4289;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F2084;
          int value1_4290 = C;
          int value2_4291 = F;
          _F2084 = value2_4291;
          _F2084 = (_F2084 & 1) | ((value1_4290 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4290--;
          value1_4290 &= 0xff;
          _F2084 |= (value1_4290 == 0x7f ? 4 : 0) | (SZ53[value1_4290 & 0xff] | (value1_4290 == 0 ? 0x40 : 0));
          int result_4293 = value1_4290 & 0xFF;
          F = (_F2084 & 0xFF);
          C = result_4293;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int operand_4294 = read((PC + 2) & 0xFFFF, 0);
          C = operand_4294;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F2087;
          int value1_4295 = A;
          int value2_4296 = F;
          _F2087 = value2_4296;
          _F2087 = (_F2087 & 0xC4) | (value1_4295 & 1);
          value1_4295 = (value1_4295 >> 1) | (value1_4295 << 7);
          _F2087 |= (value1_4295 & 0x28);
          int result_4298 = (value1_4295 & 0xff);
          F = (_F2087 & 0xFF);
          A = result_4298;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_2(int opcode) {
    switch (opcode) {
      case 0x10: {
          contend1x1(((I << 8) | R));
          B = (B - 1) & 0xFF;
          if ((B != 0)) {
              int operand_4300 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4299 = ((PC + 3 + (byte) operand_4300) & 0xFFFF);
              MEMPTR = jumpAddress2_4299;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4299;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x11: {
          int address_4302 = (PC + 2) & 0xFFFF;
          int operand_4304 = read(address_4302, 0);
          int operand_4306 = read((address_4302 + 1) & 0xFFFF, 0);
          int value_4307 = ((operand_4306 << 8) | operand_4304);
          D = (value_4307 >>> 8);
          E = value_4307 & 0xFF;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _address2091 = (D << 8) | E;
          write(_address2091, A);
          MEMPTR = ((A << 8) | ((_address2091 + 1) & 0xff));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x13: {
          int read_4308 = ((D << 8) | E);
          int value_4309 = (read_4308 + 1) & 0xFFFF;
          D = (value_4309 >>> 8);
          E = value_4309 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F2094;
          int value1_4310 = D;
          int value2_4311 = F;
          _F2094 = value2_4311;
          value1_4310++;
          value1_4310 &= 0xff;
          _F2094 = (_F2094 & 1) | (value1_4310 == 0x80 ? 4 : 0) | ((value1_4310 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4310 & 0xff] | (value1_4310 == 0 ? 0x40 : 0));
          int result_4313 = value1_4310 & 0xFF;
          F = (_F2094 & 0xFF);
          D = result_4313;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F2096;
          int value1_4314 = D;
          int value2_4315 = F;
          _F2096 = value2_4315;
          _F2096 = (_F2096 & 1) | ((value1_4314 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4314--;
          value1_4314 &= 0xff;
          _F2096 |= (value1_4314 == 0x7f ? 4 : 0) | (SZ53[value1_4314 & 0xff] | (value1_4314 == 0 ? 0x40 : 0));
          int result_4317 = value1_4314 & 0xFF;
          F = (_F2096 & 0xFF);
          D = result_4317;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          int operand_4318 = read((PC + 2) & 0xFFFF, 0);
          D = operand_4318;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F2099;
          int value1_4319 = A;
          int value2_4320 = F;
          _F2099 = value2_4320;
          int bytetemp_4322 = value1_4319;
          value1_4319 = (value1_4319 << 1) | (_F2099 & 1);
          _F2099 = (_F2099 & 0xC4) | (value1_4319 & 0x28) | (bytetemp_4322 >> 7);
          int result_4323 = value1_4319 & 0xFF;
          F = (_F2099 & 0xFF);
          A = result_4323;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_3(int opcode) {
    switch (opcode) {
      case 0x18: {
          int _nextPC2101;
          int operand_4325 = read((PC + 2) & 0xFFFF, 0);
          int jumpAddress2_4324 = ((PC + 3 + (byte) operand_4325) & 0xFFFF);
          _nextPC2101 = jumpAddress2_4324;
          MEMPTR = _nextPC2101;
          contend5x1((PC + 1) & 0xFFFF);
          PC = _nextPC2101;
          break;
      }
      case 0x19: {
          int _F2102;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int b_4327 = ((D << 8) | E);
          int result_4328 = (IY + b_4327);
          int value1_4329 = ((IY & 0x0800) >> 4 | result_4328 >> 11);
          int value2_4330 = F;
          int value3_4331 = (b_4327 >> 11) & 1;
          _F2102 = value2_4330;
          int add16temp_4332 = value1_4329 << 11;
          int lookup_4333 = (((value1_4329 << 4) & 0x0800) >> 11) | ((value3_4331 << 11) >> 10) | ((add16temp_4332 & 0x0800) >> 9);
          _F2102 = (_F2102 & 0xC4) | ((add16temp_4332 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4332 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4333];
          F = (_F2102 & 0xFF);
          IY = (result_4328 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _address2104 = (D << 8) | E;
          int value_4335 = read(_address2104, 0);
          A = value_4335;
          MEMPTR = ((_address2104 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int value_4336 = (((D << 8) | E) - 1) & 0xFFFF;
          D = (value_4336 >>> 8);
          E = value_4336 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F2107;
          int value1_4337 = E;
          int value2_4338 = F;
          _F2107 = value2_4338;
          value1_4337++;
          value1_4337 &= 0xff;
          _F2107 = (_F2107 & 1) | (value1_4337 == 0x80 ? 4 : 0) | ((value1_4337 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4337 & 0xff] | (value1_4337 == 0 ? 0x40 : 0));
          int result_4340 = value1_4337 & 0xFF;
          F = (_F2107 & 0xFF);
          E = result_4340;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F2109;
          int value1_4341 = E;
          int value2_4342 = F;
          _F2109 = value2_4342;
          _F2109 = (_F2109 & 1) | ((value1_4341 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4341--;
          value1_4341 &= 0xff;
          _F2109 |= (value1_4341 == 0x7f ? 4 : 0) | (SZ53[value1_4341 & 0xff] | (value1_4341 == 0 ? 0x40 : 0));
          int result_4344 = value1_4341 & 0xFF;
          F = (_F2109 & 0xFF);
          E = result_4344;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int operand_4345 = read((PC + 2) & 0xFFFF, 0);
          E = operand_4345;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F2112;
          int value1_4346 = A;
          int value2_4347 = F;
          _F2112 = value2_4347;
          int A_4349 = value1_4346;
          int bytetemp_4350 = A_4349;
          A_4349 = (A_4349 >> 1) | (_F2112 << 7);
          _F2112 = (_F2112 & 0xC4) | (A_4349 & 0x28) | (bytetemp_4350 & 1);
          int result_4351 = A_4349 & 0xFF;
          F = _F2112;
          A = result_4351;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_4(int opcode) {
    switch (opcode) {
      case 0x20: {
          if ((!((F & 0x40) == 0x40))) {
              int operand_4353 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4352 = ((PC + 3 + (byte) operand_4353) & 0xFFFF);
              MEMPTR = jumpAddress2_4352;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4352;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x21: {
          int address_4355 = (PC + 2) & 0xFFFF;
          int operand_4357 = read(address_4355, 0);
          int operand_4359 = read((address_4355 + 1) & 0xFFFF, 0);
          IY = ((operand_4359 << 8) | operand_4357);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _address2116;
          int address_4360 = (PC + 2) & 0xFFFF;
          int operand_4362 = read(address_4360, 0);
          int operand_4364 = read((address_4360 + 1) & 0xFFFF, 0);
          _address2116 = (operand_4364 << 8) | operand_4362;
          write(_address2116, (IY & 0xFF));
          write((_address2116 + 1) & 0xFFFF, (IY >>> 8));
          MEMPTR = ((_address2116 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int read_4365 = IY;
          IY = ((read_4365 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F2119;
          int value1_4366 = (IY >> 8);
          int value2_4367 = F;
          _F2119 = value2_4367;
          value1_4366++;
          value1_4366 &= 0xff;
          _F2119 = (_F2119 & 1) | (value1_4366 == 0x80 ? 4 : 0) | ((value1_4366 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4366 & 0xff] | (value1_4366 == 0 ? 0x40 : 0));
          int result_4369 = value1_4366 & 0xFF;
          F = (_F2119 & 0xFF);
          IY = (IY & 0x00FF) | (result_4369 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F2121;
          int value1_4370 = (IY >> 8);
          int value2_4371 = F;
          _F2121 = value2_4371;
          _F2121 = (_F2121 & 1) | ((value1_4370 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4370--;
          value1_4370 &= 0xff;
          _F2121 |= (value1_4370 == 0x7f ? 4 : 0) | (SZ53[value1_4370 & 0xff] | (value1_4370 == 0 ? 0x40 : 0));
          int result_4373 = value1_4370 & 0xFF;
          F = (_F2121 & 0xFF);
          IY = (IY & 0x00FF) | (result_4373 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          int operand_4374 = read((PC + 2) & 0xFFFF, 0);
          IY = (IY & 0x00FF) | (operand_4374 << 8);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F2127 = 0;
          int _F2126 = 0;
          int _data2125 = 0;
          int _F2124;
          int value1_4375 = A;
          int value2_4376 = F;
          _F2124 = value2_4376;
          value1_4375 &= 0xff;
          int add_4378 = 0;
          int carry_4379 = (_F2124 & 1);
          if (((_F2124 & 0x10) != 0) || ((value1_4375 & 0x0f) > 9)) {
              add_4378 = 6;
          }
          if (carry_4379 != 0 || (value1_4375 > 0x99)) {
              add_4378 |= 0x60;
          }
          if (value1_4375 > 0x99) {
              carry_4379 = 1;
          }
          int and_4381 = _F2124 & 0xff;
          _data2125 = and_4381;
          if ((_F2124 & 2) != 0) {
              int value1_4382 = value1_4375;
              int subtemp_4385 = value1_4382 - add_4378;
              int lookup_4386 = ((value1_4382 & 0x88) >> 3) | ((add_4378 & 0x88) >> 2) | ((subtemp_4385 & 0x88) >> 1);
              value1_4382 = subtemp_4385 & 0xff;
              _F2126 = ((subtemp_4385 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4386 & 0x07)] | OVERFLOW_SUB[(lookup_4386 >> 4)] | (SZ53[value1_4382 & 0xff] | (value1_4382 == 0 ? 0x40 : 0));
              int result_4387 = value1_4382 & 0xFF;
              int and_4388 = (_F2126 & 0xFF);
              _data2125 = and_4388;
              value1_4375 = result_4387;
          } else {
              int value2_4390 = value1_4375;
              int addtemp_4392 = value2_4390 + add_4378;
              int lookup_4393 = ((value2_4390 & 0x88) >> 3) | ((add_4378 & 0x88) >> 2) | ((addtemp_4392 & 0x88) >> 1);
              value2_4390 = addtemp_4392 & 0xff;
              _F2127 = ((addtemp_4392 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4393 & 0x07)] | OVERFLOW_ADD[(lookup_4393 >> 4)] | (SZ53[value2_4390 & 0xff] | (value2_4390 == 0 ? 0x40 : 0));
              int result_4394 = value2_4390 & 0xFF;
              int and_4395 = (_F2127 & 0xFF);
              _data2125 = and_4395;
              value1_4375 = result_4394;
          }
          _F2124 = _data2125;
          _F2124 = (_F2124 & -6) | carry_4379 | PARITY[value1_4375 & 0xff];
          int result_4396 = value1_4375 & 0xFF;
          F = (_F2124 & 0xFF);
          A = result_4396;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_5(int opcode) {
    switch (opcode) {
      case 0x28: {
          if (((F & 0x40) == 0x40)) {
              int operand_4398 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4397 = ((PC + 3 + (byte) operand_4398) & 0xFFFF);
              MEMPTR = jumpAddress2_4397;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4397;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x29: {
          int _F2130;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int result_4400 = (IY + IY);
          int value1_4401 = ((IY & 0x0800) >> 4 | result_4400 >> 11);
          int value2_4402 = F;
          int value3_4403 = (IY >> 11) & 1;
          _F2130 = value2_4402;
          int add16temp_4404 = value1_4401 << 11;
          int lookup_4405 = (((value1_4401 << 4) & 0x0800) >> 11) | ((value3_4403 << 11) >> 10) | ((add16temp_4404 & 0x0800) >> 9);
          _F2130 = (_F2130 & 0xC4) | ((add16temp_4404 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4404 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4405];
          F = (_F2130 & 0xFF);
          IY = (result_4400 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _address2132;
          int address_4407 = (PC + 2) & 0xFFFF;
          int operand_4409 = read(address_4407, 0);
          int operand_4411 = read((address_4407 + 1) & 0xFFFF, 0);
          _address2132 = (operand_4411 << 8) | operand_4409;
          int wordNumber1_4412 = read(_address2132, 0);
          int wordNumber_4413 = read((_address2132 + 1) & 0xFFFF, 0);
          IY = ((wordNumber_4413 << 8) | wordNumber1_4412);
          MEMPTR = ((_address2132 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2B: {
          IY = ((IY - 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F2135;
          int value1_4414 = (IY & 0xFF);
          int value2_4415 = F;
          _F2135 = value2_4415;
          value1_4414++;
          value1_4414 &= 0xff;
          _F2135 = (_F2135 & 1) | (value1_4414 == 0x80 ? 4 : 0) | ((value1_4414 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4414 & 0xff] | (value1_4414 == 0 ? 0x40 : 0));
          int result_4417 = value1_4414 & 0xFF;
          F = (_F2135 & 0xFF);
          IY = (IY & 0xFF00) | result_4417;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F2137;
          int value1_4418 = (IY & 0xFF);
          int value2_4419 = F;
          _F2137 = value2_4419;
          _F2137 = (_F2137 & 1) | ((value1_4418 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4418--;
          value1_4418 &= 0xff;
          _F2137 |= (value1_4418 == 0x7f ? 4 : 0) | (SZ53[value1_4418 & 0xff] | (value1_4418 == 0 ? 0x40 : 0));
          int result_4421 = value1_4418 & 0xFF;
          F = (_F2137 & 0xFF);
          IY = (IY & 0xFF00) | result_4421;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int operand_4422 = read((PC + 2) & 0xFFFF, 0);
          IY = (IY & 0xFF00) | operand_4422;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F2140;
          int value1_4423 = A;
          int value2_4424 = F;
          _F2140 = value2_4424;
          value1_4423 ^= 0xff;
          _F2140 = (_F2140 & 0xC5) | (value1_4423 & 0x28) | 0x12;
          int result_4426 = value1_4423 & 0xFF;
          F = _F2140;
          A = result_4426;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_6(int opcode) {
    switch (opcode) {
      case 0x30: {
          if ((!((F & 1) == 1))) {
              int operand_4428 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4427 = ((PC + 3 + (byte) operand_4428) & 0xFFFF);
              MEMPTR = jumpAddress2_4427;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4427;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x31: {
          int address_4430 = (PC + 2) & 0xFFFF;
          int operand_4432 = read(address_4430, 0);
          int operand_4434 = read((address_4430 + 1) & 0xFFFF, 0);
          SP = ((operand_4434 << 8) | operand_4432);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _address2144;
          int address_4435 = (PC + 2) & 0xFFFF;
          int operand_4437 = read(address_4435, 0);
          int operand_4439 = read((address_4435 + 1) & 0xFFFF, 0);
          _address2144 = (operand_4439 << 8) | operand_4437;
          write(_address2144, A);
          MEMPTR = ((A << 8) | ((_address2144 + 1) & 0xff));
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int read_4440 = SP;
          SP = ((read_4440 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F2148;
          int _value2147;
          int _address2147;
          int operand_4441 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4441)) & 0xFFFF;
          int operand_4442 = read(_address2147, 0);
          contend1x1(_address2147);
          _value2147 = operand_4442;
          int value1_4443 = _value2147;
          int value2_4444 = F;
          _F2148 = value2_4444;
          value1_4443++;
          value1_4443 &= 0xff;
          _F2148 = (_F2148 & 1) | (value1_4443 == 0x80 ? 4 : 0) | ((value1_4443 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4443 & 0xff] | (value1_4443 == 0 ? 0x40 : 0));
          int result_4446 = value1_4443 & 0xFF;
          F = (_F2148 & 0xFF);
          _address2147 = (IY + (int) ((byte) operand_4441)) & 0xFFFF;
          _value2147 = result_4446;
          write(_address2147, result_4446);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F2150;
          int _value2147;
          int _address2147;
          int operand_4447 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4447)) & 0xFFFF;
          int operand_4448 = read(_address2147, 0);
          contend1x1(_address2147);
          _value2147 = operand_4448;
          int value1_4449 = _value2147;
          int value2_4450 = F;
          _F2150 = value2_4450;
          _F2150 = (_F2150 & 1) | ((value1_4449 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4449--;
          value1_4449 &= 0xff;
          _F2150 |= (value1_4449 == 0x7f ? 4 : 0) | (SZ53[value1_4449 & 0xff] | (value1_4449 == 0 ? 0x40 : 0));
          int result_4452 = value1_4449 & 0xFF;
          F = (_F2150 & 0xFF);
          _address2147 = (IY + (int) ((byte) operand_4447)) & 0xFFFF;
          _value2147 = result_4452;
          write(_address2147, result_4452);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _address2152;
          int operand_4453 = read((PC + 2) & 0xFFFF, 0);
          int operand_4454 = read((PC + 3) & 0xFFFF, 0);
          _address2152 = (IY + (int) ((byte) operand_4453)) & 0xFFFF;
          contend2x1((PC + 3) & 0xFFFF);
          write(_address2152, operand_4454);
          MEMPTR = _address2152;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F2154;
          int value2_4456 = F;
          _F2154 = value2_4456;
          _F2154 = _F2154 & 0xC4 | A & 0x28 | 1;
          F = _F2154;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_7(int opcode) {
    switch (opcode) {
      case 0x38: {
          if (((F & 1) == 1)) {
              int operand_4460 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4459 = ((PC + 3 + (byte) operand_4460) & 0xFFFF);
              MEMPTR = jumpAddress2_4459;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4459;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x39: {
          int _F2157;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int result_4462 = (IY + SP);
          int value1_4463 = ((IY & 0x0800) >> 4 | result_4462 >> 11);
          int value2_4464 = F;
          int value3_4465 = (SP >> 11) & 1;
          _F2157 = value2_4464;
          int add16temp_4466 = value1_4463 << 11;
          int lookup_4467 = (((value1_4463 << 4) & 0x0800) >> 11) | ((value3_4465 << 11) >> 10) | ((add16temp_4466 & 0x0800) >> 9);
          _F2157 = (_F2157 & 0xC4) | ((add16temp_4466 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4466 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4467];
          F = (_F2157 & 0xFF);
          IY = (result_4462 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _address2159;
          int address_4469 = (PC + 2) & 0xFFFF;
          int operand_4471 = read(address_4469, 0);
          int operand_4473 = read((address_4469 + 1) & 0xFFFF, 0);
          _address2159 = (operand_4473 << 8) | operand_4471;
          int value_4474 = read(_address2159, 0);
          A = value_4474;
          MEMPTR = ((_address2159 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3B: {
          SP = ((SP - 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F2162;
          int value1_4475 = A;
          int value2_4476 = F;
          _F2162 = value2_4476;
          value1_4475++;
          value1_4475 &= 0xff;
          _F2162 = (_F2162 & 1) | (value1_4475 == 0x80 ? 4 : 0) | ((value1_4475 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4475 & 0xff] | (value1_4475 == 0 ? 0x40 : 0));
          int result_4478 = value1_4475 & 0xFF;
          F = (_F2162 & 0xFF);
          A = result_4478;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F2164;
          int value1_4479 = A;
          int value2_4480 = F;
          _F2164 = value2_4480;
          _F2164 = (_F2164 & 1) | ((value1_4479 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4479--;
          value1_4479 &= 0xff;
          _F2164 |= (value1_4479 == 0x7f ? 4 : 0) | (SZ53[value1_4479 & 0xff] | (value1_4479 == 0 ? 0x40 : 0));
          int result_4482 = value1_4479 & 0xFF;
          F = (_F2164 & 0xFF);
          A = result_4482;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int operand_4483 = read((PC + 2) & 0xFFFF, 0);
          A = operand_4483;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F2167;
          int value2_4485 = F;
          _F2167 = value2_4485;
          _F2167 = _F2167 & 0xC4 | ((_F2167 & 1) != 0 ? 0x10 : 1) | A & 0x28;
          F = _F2167;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_8(int opcode) {
    switch (opcode) {
      case 0x40: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x41: {
          B = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x42: {
          B = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x43: {
          B = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x44: {
          B = (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x45: {
          B = (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _value2147;
          int _address2147;
          int operand_4488 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4488)) & 0xFFFF;
          int operand_4489 = read(_address2147, 0);
          _value2147 = operand_4489;
          B = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x47: {
          B = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_9(int opcode) {
    switch (opcode) {
      case 0x48: {
          C = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x49: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4A: {
          C = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4B: {
          C = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4C: {
          C = (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4D: {
          C = (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _value2147;
          int _address2147;
          int operand_4490 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4490)) & 0xFFFF;
          int operand_4491 = read(_address2147, 0);
          _value2147 = operand_4491;
          C = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x4F: {
          C = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_10(int opcode) {
    switch (opcode) {
      case 0x50: {
          D = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x51: {
          D = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x52: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x53: {
          D = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x54: {
          D = (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x55: {
          D = (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _value2147;
          int _address2147;
          int operand_4492 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4492)) & 0xFFFF;
          int operand_4493 = read(_address2147, 0);
          _value2147 = operand_4493;
          D = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x57: {
          D = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_11(int opcode) {
    switch (opcode) {
      case 0x58: {
          E = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x59: {
          E = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5A: {
          E = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5B: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5C: {
          E = (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5D: {
          E = (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _value2147;
          int _address2147;
          int operand_4494 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4494)) & 0xFFFF;
          int operand_4495 = read(_address2147, 0);
          _value2147 = operand_4495;
          E = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x5F: {
          E = A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_12(int opcode) {
    switch (opcode) {
      case 0x60: {
          IY = (IY & 0x00FF) | (B << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x61: {
          IY = (IY & 0x00FF) | (C << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x62: {
          IY = (IY & 0x00FF) | (D << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x63: {
          IY = (IY & 0x00FF) | (E << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x64: {
          IY = (IY & 0x00FF) | ((IY >> 8) << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x65: {
          IY = (IY & 0x00FF) | ((IY & 0xFF) << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _value2147;
          int _address2147;
          int operand_4496 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4496)) & 0xFFFF;
          int operand_4497 = read(_address2147, 0);
          _value2147 = operand_4497;
          H = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x67: {
          IY = (IY & 0x00FF) | (A << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_13(int opcode) {
    switch (opcode) {
      case 0x68: {
          IY = (IY & 0xFF00) | B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x69: {
          IY = (IY & 0xFF00) | C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6A: {
          IY = (IY & 0xFF00) | D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6B: {
          IY = (IY & 0xFF00) | E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6C: {
          IY = (IY & 0xFF00) | (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6D: {
          IY = (IY & 0xFF00) | (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _value2147;
          int _address2147;
          int operand_4498 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4498)) & 0xFFFF;
          int operand_4499 = read(_address2147, 0);
          _value2147 = operand_4499;
          L = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x6F: {
          IY = (IY & 0xFF00) | A;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_14(int opcode) {
    switch (opcode) {
      case 0x70: {
          int _address2147;
          int operand_4500 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4500)) & 0xFFFF;
          write(_address2147, B);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _address2147;
          int operand_4501 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4501)) & 0xFFFF;
          write(_address2147, C);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _address2147;
          int operand_4502 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4502)) & 0xFFFF;
          write(_address2147, D);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address2147;
          int operand_4503 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4503)) & 0xFFFF;
          write(_address2147, E);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _address2147;
          int operand_4504 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4504)) & 0xFFFF;
          write(_address2147, H);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _address2147;
          int operand_4505 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4505)) & 0xFFFF;
          write(_address2147, L);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x76: {
          if (!state.isHalted()) {
              state.setHalted(true);
              _nextPC2223 = PC;
          }
          PC = _nextPC2223 == -1 ? (PC + 2) & 0xFFFF : _nextPC2223;
          break;
      }
      case 0x77: {
          int _address2147;
          int operand_4506 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4506)) & 0xFFFF;
          write(_address2147, A);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_15(int opcode) {
    switch (opcode) {
      case 0x78: {
          A = B;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x79: {
          A = C;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7A: {
          A = D;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7B: {
          A = E;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7C: {
          A = (IY >> 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7D: {
          A = (IY & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _value2147;
          int _address2147;
          int operand_4507 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4507)) & 0xFFFF;
          int operand_4508 = read(_address2147, 0);
          _value2147 = operand_4508;
          A = _value2147;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x7F: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_16(int opcode) {
    switch (opcode) {
      case 0x80: {
          int _F2233;
          int value1_4509 = A;
          int value2_4510 = B;
          int addtemp_4512 = value2_4510 + value1_4509;
          int lookup_4513 = ((value2_4510 & 0x88) >> 3) | ((value1_4509 & 0x88) >> 2) | ((addtemp_4512 & 0x88) >> 1);
          value2_4510 = addtemp_4512 & 0xff;
          _F2233 = ((addtemp_4512 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4513 & 0x07)] | OVERFLOW_ADD[(lookup_4513 >> 4)] | (SZ53[value2_4510] | (value2_4510 == 0 ? 0x40 : 0));
          F = (_F2233 & 0xFF);
          A = value2_4510;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _F2235;
          int value1_4515 = A;
          int value2_4516 = C;
          int addtemp_4518 = value2_4516 + value1_4515;
          int lookup_4519 = ((value2_4516 & 0x88) >> 3) | ((value1_4515 & 0x88) >> 2) | ((addtemp_4518 & 0x88) >> 1);
          value2_4516 = addtemp_4518 & 0xff;
          _F2235 = ((addtemp_4518 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4519 & 0x07)] | OVERFLOW_ADD[(lookup_4519 >> 4)] | (SZ53[value2_4516] | (value2_4516 == 0 ? 0x40 : 0));
          F = (_F2235 & 0xFF);
          A = value2_4516;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _F2237;
          int value1_4521 = A;
          int value2_4522 = D;
          int addtemp_4524 = value2_4522 + value1_4521;
          int lookup_4525 = ((value2_4522 & 0x88) >> 3) | ((value1_4521 & 0x88) >> 2) | ((addtemp_4524 & 0x88) >> 1);
          value2_4522 = addtemp_4524 & 0xff;
          _F2237 = ((addtemp_4524 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4525 & 0x07)] | OVERFLOW_ADD[(lookup_4525 >> 4)] | (SZ53[value2_4522] | (value2_4522 == 0 ? 0x40 : 0));
          F = (_F2237 & 0xFF);
          A = value2_4522;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _F2239;
          int value1_4527 = A;
          int value2_4528 = E;
          int addtemp_4530 = value2_4528 + value1_4527;
          int lookup_4531 = ((value2_4528 & 0x88) >> 3) | ((value1_4527 & 0x88) >> 2) | ((addtemp_4530 & 0x88) >> 1);
          value2_4528 = addtemp_4530 & 0xff;
          _F2239 = ((addtemp_4530 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4531 & 0x07)] | OVERFLOW_ADD[(lookup_4531 >> 4)] | (SZ53[value2_4528] | (value2_4528 == 0 ? 0x40 : 0));
          F = (_F2239 & 0xFF);
          A = value2_4528;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _F2241;
          int value1_4533 = A;
          int value2_4534 = (IY >> 8);
          int addtemp_4536 = value2_4534 + value1_4533;
          int lookup_4537 = ((value2_4534 & 0x88) >> 3) | ((value1_4533 & 0x88) >> 2) | ((addtemp_4536 & 0x88) >> 1);
          value2_4534 = addtemp_4536 & 0xff;
          _F2241 = ((addtemp_4536 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4537 & 0x07)] | OVERFLOW_ADD[(lookup_4537 >> 4)] | (SZ53[value2_4534] | (value2_4534 == 0 ? 0x40 : 0));
          F = (_F2241 & 0xFF);
          A = value2_4534;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _F2243;
          int value1_4539 = A;
          int value2_4540 = (IY & 0xFF);
          int addtemp_4542 = value2_4540 + value1_4539;
          int lookup_4543 = ((value2_4540 & 0x88) >> 3) | ((value1_4539 & 0x88) >> 2) | ((addtemp_4542 & 0x88) >> 1);
          value2_4540 = addtemp_4542 & 0xff;
          _F2243 = ((addtemp_4542 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4543 & 0x07)] | OVERFLOW_ADD[(lookup_4543 >> 4)] | (SZ53[value2_4540] | (value2_4540 == 0 ? 0x40 : 0));
          F = (_F2243 & 0xFF);
          A = value2_4540;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _F2245;
          int _value2147;
          int _address2147;
          int operand_4545 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4545)) & 0xFFFF;
          int operand_4546 = read(_address2147, 0);
          _value2147 = operand_4546;
          int value1_4547 = A;
          int value2_4548 = _value2147;
          int addtemp_4550 = value2_4548 + value1_4547;
          int lookup_4551 = ((value2_4548 & 0x88) >> 3) | ((value1_4547 & 0x88) >> 2) | ((addtemp_4550 & 0x88) >> 1);
          value2_4548 = addtemp_4550 & 0xff;
          _F2245 = ((addtemp_4550 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4551 & 0x07)] | OVERFLOW_ADD[(lookup_4551 >> 4)] | (SZ53[value2_4548] | (value2_4548 == 0 ? 0x40 : 0));
          F = (_F2245 & 0xFF);
          A = value2_4548;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _F2247;
          int value1_4553 = A;
          int value2_4554 = A;
          int addtemp_4556 = value2_4554 + value1_4553;
          int lookup_4557 = ((value2_4554 & 0x88) >> 3) | ((value1_4553 & 0x88) >> 2) | ((addtemp_4556 & 0x88) >> 1);
          value2_4554 = addtemp_4556 & 0xff;
          _F2247 = ((addtemp_4556 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4557 & 0x07)] | OVERFLOW_ADD[(lookup_4557 >> 4)] | (SZ53[value2_4554] | (value2_4554 == 0 ? 0x40 : 0));
          F = (_F2247 & 0xFF);
          A = value2_4554;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_17(int opcode) {
    switch (opcode) {
      case 0x88: {
          int _F2249;
          int value1_4559 = A;
          int value3_4561 = F & 1;
          _F2249 = value3_4561;
          int adctemp_4562 = value1_4559 + B + (_F2249 & 1);
          int lookup_4563 = ((value1_4559 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((adctemp_4562 & 0x88) >> 1);
          value1_4559 = adctemp_4562 & 0xff;
          _F2249 = ((adctemp_4562 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4563 & 0x07)] | OVERFLOW_ADD[(lookup_4563 >> 4)] | (SZ53[value1_4559] | (value1_4559 == 0 ? 0x40 : 0));
          F = (_F2249 & 0xFF);
          A = value1_4559;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _F2251;
          int value1_4565 = A;
          int value3_4567 = F & 1;
          _F2251 = value3_4567;
          int adctemp_4568 = value1_4565 + C + (_F2251 & 1);
          int lookup_4569 = ((value1_4565 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((adctemp_4568 & 0x88) >> 1);
          value1_4565 = adctemp_4568 & 0xff;
          _F2251 = ((adctemp_4568 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4569 & 0x07)] | OVERFLOW_ADD[(lookup_4569 >> 4)] | (SZ53[value1_4565] | (value1_4565 == 0 ? 0x40 : 0));
          F = (_F2251 & 0xFF);
          A = value1_4565;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _F2253;
          int value1_4571 = A;
          int value3_4573 = F & 1;
          _F2253 = value3_4573;
          int adctemp_4574 = value1_4571 + D + (_F2253 & 1);
          int lookup_4575 = ((value1_4571 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((adctemp_4574 & 0x88) >> 1);
          value1_4571 = adctemp_4574 & 0xff;
          _F2253 = ((adctemp_4574 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4575 & 0x07)] | OVERFLOW_ADD[(lookup_4575 >> 4)] | (SZ53[value1_4571] | (value1_4571 == 0 ? 0x40 : 0));
          F = (_F2253 & 0xFF);
          A = value1_4571;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _F2255;
          int value1_4577 = A;
          int value3_4579 = F & 1;
          _F2255 = value3_4579;
          int adctemp_4580 = value1_4577 + E + (_F2255 & 1);
          int lookup_4581 = ((value1_4577 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((adctemp_4580 & 0x88) >> 1);
          value1_4577 = adctemp_4580 & 0xff;
          _F2255 = ((adctemp_4580 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4581 & 0x07)] | OVERFLOW_ADD[(lookup_4581 >> 4)] | (SZ53[value1_4577] | (value1_4577 == 0 ? 0x40 : 0));
          F = (_F2255 & 0xFF);
          A = value1_4577;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _F2257;
          int value1_4583 = A;
          int value2_4584 = (IY >> 8);
          int value3_4585 = F & 1;
          _F2257 = value3_4585;
          int adctemp_4586 = value1_4583 + value2_4584 + (_F2257 & 1);
          int lookup_4587 = ((value1_4583 & 0x88) >> 3) | ((value2_4584 & 0x88) >> 2) | ((adctemp_4586 & 0x88) >> 1);
          value1_4583 = adctemp_4586 & 0xff;
          _F2257 = ((adctemp_4586 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4587 & 0x07)] | OVERFLOW_ADD[(lookup_4587 >> 4)] | (SZ53[value1_4583] | (value1_4583 == 0 ? 0x40 : 0));
          F = (_F2257 & 0xFF);
          A = value1_4583;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _F2259;
          int value1_4589 = A;
          int value2_4590 = (IY & 0xFF);
          int value3_4591 = F & 1;
          _F2259 = value3_4591;
          int adctemp_4592 = value1_4589 + value2_4590 + (_F2259 & 1);
          int lookup_4593 = ((value1_4589 & 0x88) >> 3) | ((value2_4590 & 0x88) >> 2) | ((adctemp_4592 & 0x88) >> 1);
          value1_4589 = adctemp_4592 & 0xff;
          _F2259 = ((adctemp_4592 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4593 & 0x07)] | OVERFLOW_ADD[(lookup_4593 >> 4)] | (SZ53[value1_4589] | (value1_4589 == 0 ? 0x40 : 0));
          F = (_F2259 & 0xFF);
          A = value1_4589;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _F2261;
          int _value2147;
          int _address2147;
          int operand_4595 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4595)) & 0xFFFF;
          int operand_4596 = read(_address2147, 0);
          _value2147 = operand_4596;
          int value1_4597 = A;
          int value3_4599 = F & 1;
          _F2261 = value3_4599;
          int adctemp_4600 = value1_4597 + _value2147 + (_F2261 & 1);
          int lookup_4601 = ((value1_4597 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((adctemp_4600 & 0x88) >> 1);
          value1_4597 = adctemp_4600 & 0xff;
          _F2261 = ((adctemp_4600 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4601 & 0x07)] | OVERFLOW_ADD[(lookup_4601 >> 4)] | (SZ53[value1_4597] | (value1_4597 == 0 ? 0x40 : 0));
          F = (_F2261 & 0xFF);
          A = value1_4597;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _F2263;
          int value1_4603 = A;
          int value2_4604 = A;
          int value3_4605 = F & 1;
          _F2263 = value3_4605;
          int adctemp_4606 = value1_4603 + value2_4604 + (_F2263 & 1);
          int lookup_4607 = ((value1_4603 & 0x88) >> 3) | ((value2_4604 & 0x88) >> 2) | ((adctemp_4606 & 0x88) >> 1);
          value1_4603 = adctemp_4606 & 0xff;
          _F2263 = ((adctemp_4606 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4607 & 0x07)] | OVERFLOW_ADD[(lookup_4607 >> 4)] | (SZ53[value1_4603] | (value1_4603 == 0 ? 0x40 : 0));
          F = (_F2263 & 0xFF);
          A = value1_4603;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_18(int opcode) {
    switch (opcode) {
      case 0x90: {
          int _F2265;
          int value1_4609 = A;
          int subtemp_4612 = value1_4609 - B;
          int lookup_4613 = ((value1_4609 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((subtemp_4612 & 0x88) >> 1);
          value1_4609 = subtemp_4612 & 0xff;
          _F2265 = ((subtemp_4612 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4613 & 0x07)] | OVERFLOW_SUB[(lookup_4613 >> 4)] | (SZ53[value1_4609] | (value1_4609 == 0 ? 0x40 : 0));
          F = (_F2265 & 0xFF);
          A = value1_4609;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _F2267;
          int value1_4615 = A;
          int subtemp_4618 = value1_4615 - C;
          int lookup_4619 = ((value1_4615 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((subtemp_4618 & 0x88) >> 1);
          value1_4615 = subtemp_4618 & 0xff;
          _F2267 = ((subtemp_4618 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4619 & 0x07)] | OVERFLOW_SUB[(lookup_4619 >> 4)] | (SZ53[value1_4615] | (value1_4615 == 0 ? 0x40 : 0));
          F = (_F2267 & 0xFF);
          A = value1_4615;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _F2269;
          int value1_4621 = A;
          int subtemp_4624 = value1_4621 - D;
          int lookup_4625 = ((value1_4621 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((subtemp_4624 & 0x88) >> 1);
          value1_4621 = subtemp_4624 & 0xff;
          _F2269 = ((subtemp_4624 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4625 & 0x07)] | OVERFLOW_SUB[(lookup_4625 >> 4)] | (SZ53[value1_4621] | (value1_4621 == 0 ? 0x40 : 0));
          F = (_F2269 & 0xFF);
          A = value1_4621;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _F2271;
          int value1_4627 = A;
          int subtemp_4630 = value1_4627 - E;
          int lookup_4631 = ((value1_4627 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((subtemp_4630 & 0x88) >> 1);
          value1_4627 = subtemp_4630 & 0xff;
          _F2271 = ((subtemp_4630 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4631 & 0x07)] | OVERFLOW_SUB[(lookup_4631 >> 4)] | (SZ53[value1_4627] | (value1_4627 == 0 ? 0x40 : 0));
          F = (_F2271 & 0xFF);
          A = value1_4627;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _F2273;
          int value1_4633 = A;
          int value2_4634 = (IY >> 8);
          int subtemp_4636 = value1_4633 - value2_4634;
          int lookup_4637 = ((value1_4633 & 0x88) >> 3) | ((value2_4634 & 0x88) >> 2) | ((subtemp_4636 & 0x88) >> 1);
          value1_4633 = subtemp_4636 & 0xff;
          _F2273 = ((subtemp_4636 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4637 & 0x07)] | OVERFLOW_SUB[(lookup_4637 >> 4)] | (SZ53[value1_4633] | (value1_4633 == 0 ? 0x40 : 0));
          F = (_F2273 & 0xFF);
          A = value1_4633;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _F2275;
          int value1_4639 = A;
          int value2_4640 = (IY & 0xFF);
          int subtemp_4642 = value1_4639 - value2_4640;
          int lookup_4643 = ((value1_4639 & 0x88) >> 3) | ((value2_4640 & 0x88) >> 2) | ((subtemp_4642 & 0x88) >> 1);
          value1_4639 = subtemp_4642 & 0xff;
          _F2275 = ((subtemp_4642 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4643 & 0x07)] | OVERFLOW_SUB[(lookup_4643 >> 4)] | (SZ53[value1_4639] | (value1_4639 == 0 ? 0x40 : 0));
          F = (_F2275 & 0xFF);
          A = value1_4639;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _F2277;
          int _value2147;
          int _address2147;
          int operand_4645 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4645)) & 0xFFFF;
          int operand_4646 = read(_address2147, 0);
          _value2147 = operand_4646;
          int value1_4647 = A;
          int subtemp_4650 = value1_4647 - _value2147;
          int lookup_4651 = ((value1_4647 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((subtemp_4650 & 0x88) >> 1);
          value1_4647 = subtemp_4650 & 0xff;
          _F2277 = ((subtemp_4650 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4651 & 0x07)] | OVERFLOW_SUB[(lookup_4651 >> 4)] | (SZ53[value1_4647] | (value1_4647 == 0 ? 0x40 : 0));
          F = (_F2277 & 0xFF);
          A = value1_4647;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _F2279;
          int value1_4653 = A;
          int value2_4654 = A;
          int subtemp_4656 = value1_4653 - value2_4654;
          int lookup_4657 = ((value1_4653 & 0x88) >> 3) | ((value2_4654 & 0x88) >> 2) | ((subtemp_4656 & 0x88) >> 1);
          value1_4653 = subtemp_4656 & 0xff;
          _F2279 = ((subtemp_4656 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4657 & 0x07)] | OVERFLOW_SUB[(lookup_4657 >> 4)] | (SZ53[value1_4653] | (value1_4653 == 0 ? 0x40 : 0));
          F = (_F2279 & 0xFF);
          A = value1_4653;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_19(int opcode) {
    switch (opcode) {
      case 0x98: {
          int _F2281;
          int value1_4659 = A;
          int value3_4661 = F & 1;
          _F2281 = value3_4661;
          int sbctemp_4662 = value1_4659 - B - (_F2281 & 1);
          int lookup_4663 = ((value1_4659 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((sbctemp_4662 & 0x88) >> 1);
          value1_4659 = sbctemp_4662 & 0xff;
          _F2281 = ((sbctemp_4662 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4663 & 0x07)] | OVERFLOW_SUB[(lookup_4663 >> 4)] | (SZ53[value1_4659] | (value1_4659 == 0 ? 0x40 : 0));
          F = (_F2281 & 0xFF);
          A = value1_4659;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _F2283;
          int value1_4665 = A;
          int value3_4667 = F & 1;
          _F2283 = value3_4667;
          int sbctemp_4668 = value1_4665 - C - (_F2283 & 1);
          int lookup_4669 = ((value1_4665 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((sbctemp_4668 & 0x88) >> 1);
          value1_4665 = sbctemp_4668 & 0xff;
          _F2283 = ((sbctemp_4668 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4669 & 0x07)] | OVERFLOW_SUB[(lookup_4669 >> 4)] | (SZ53[value1_4665] | (value1_4665 == 0 ? 0x40 : 0));
          F = (_F2283 & 0xFF);
          A = value1_4665;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _F2285;
          int value1_4671 = A;
          int value3_4673 = F & 1;
          _F2285 = value3_4673;
          int sbctemp_4674 = value1_4671 - D - (_F2285 & 1);
          int lookup_4675 = ((value1_4671 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((sbctemp_4674 & 0x88) >> 1);
          value1_4671 = sbctemp_4674 & 0xff;
          _F2285 = ((sbctemp_4674 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4675 & 0x07)] | OVERFLOW_SUB[(lookup_4675 >> 4)] | (SZ53[value1_4671] | (value1_4671 == 0 ? 0x40 : 0));
          F = (_F2285 & 0xFF);
          A = value1_4671;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _F2287;
          int value1_4677 = A;
          int value3_4679 = F & 1;
          _F2287 = value3_4679;
          int sbctemp_4680 = value1_4677 - E - (_F2287 & 1);
          int lookup_4681 = ((value1_4677 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((sbctemp_4680 & 0x88) >> 1);
          value1_4677 = sbctemp_4680 & 0xff;
          _F2287 = ((sbctemp_4680 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4681 & 0x07)] | OVERFLOW_SUB[(lookup_4681 >> 4)] | (SZ53[value1_4677] | (value1_4677 == 0 ? 0x40 : 0));
          F = (_F2287 & 0xFF);
          A = value1_4677;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _F2289;
          int value1_4683 = A;
          int value2_4684 = (IY >> 8);
          int value3_4685 = F & 1;
          _F2289 = value3_4685;
          int sbctemp_4686 = value1_4683 - value2_4684 - (_F2289 & 1);
          int lookup_4687 = ((value1_4683 & 0x88) >> 3) | ((value2_4684 & 0x88) >> 2) | ((sbctemp_4686 & 0x88) >> 1);
          value1_4683 = sbctemp_4686 & 0xff;
          _F2289 = ((sbctemp_4686 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4687 & 0x07)] | OVERFLOW_SUB[(lookup_4687 >> 4)] | (SZ53[value1_4683] | (value1_4683 == 0 ? 0x40 : 0));
          F = (_F2289 & 0xFF);
          A = value1_4683;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _F2291;
          int value1_4689 = A;
          int value2_4690 = (IY & 0xFF);
          int value3_4691 = F & 1;
          _F2291 = value3_4691;
          int sbctemp_4692 = value1_4689 - value2_4690 - (_F2291 & 1);
          int lookup_4693 = ((value1_4689 & 0x88) >> 3) | ((value2_4690 & 0x88) >> 2) | ((sbctemp_4692 & 0x88) >> 1);
          value1_4689 = sbctemp_4692 & 0xff;
          _F2291 = ((sbctemp_4692 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4693 & 0x07)] | OVERFLOW_SUB[(lookup_4693 >> 4)] | (SZ53[value1_4689] | (value1_4689 == 0 ? 0x40 : 0));
          F = (_F2291 & 0xFF);
          A = value1_4689;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _F2293;
          int _value2147;
          int _address2147;
          int operand_4695 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4695)) & 0xFFFF;
          int operand_4696 = read(_address2147, 0);
          _value2147 = operand_4696;
          int value1_4697 = A;
          int value3_4699 = F & 1;
          _F2293 = value3_4699;
          int sbctemp_4700 = value1_4697 - _value2147 - (_F2293 & 1);
          int lookup_4701 = ((value1_4697 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((sbctemp_4700 & 0x88) >> 1);
          value1_4697 = sbctemp_4700 & 0xff;
          _F2293 = ((sbctemp_4700 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4701 & 0x07)] | OVERFLOW_SUB[(lookup_4701 >> 4)] | (SZ53[value1_4697] | (value1_4697 == 0 ? 0x40 : 0));
          F = (_F2293 & 0xFF);
          A = value1_4697;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _F2295;
          int value1_4703 = A;
          int value2_4704 = A;
          int value3_4705 = F & 1;
          _F2295 = value3_4705;
          int sbctemp_4706 = value1_4703 - value2_4704 - (_F2295 & 1);
          int lookup_4707 = ((value1_4703 & 0x88) >> 3) | ((value2_4704 & 0x88) >> 2) | ((sbctemp_4706 & 0x88) >> 1);
          value1_4703 = sbctemp_4706 & 0xff;
          _F2295 = ((sbctemp_4706 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4707 & 0x07)] | OVERFLOW_SUB[(lookup_4707 >> 4)] | (SZ53[value1_4703] | (value1_4703 == 0 ? 0x40 : 0));
          F = (_F2295 & 0xFF);
          A = value1_4703;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_20(int opcode) {
    switch (opcode) {
      case 0xA0: {
          int _F2297;
          int value1_4709 = A;
          int value2_4710 = B;
          value2_4710 &= value1_4709;
          _F2297 = 0x10 | (SZ53P[value2_4710 & 0xff] | (value2_4710 == 0 ? 0x40 : 0));
          int result_4712 = value2_4710 & 0xFF;
          F = (_F2297 & 0xFF);
          A = result_4712;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F2299;
          int value1_4713 = A;
          int value2_4714 = C;
          value2_4714 &= value1_4713;
          _F2299 = 0x10 | (SZ53P[value2_4714 & 0xff] | (value2_4714 == 0 ? 0x40 : 0));
          int result_4716 = value2_4714 & 0xFF;
          F = (_F2299 & 0xFF);
          A = result_4716;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F2301;
          int value1_4717 = A;
          int value2_4718 = D;
          value2_4718 &= value1_4717;
          _F2301 = 0x10 | (SZ53P[value2_4718 & 0xff] | (value2_4718 == 0 ? 0x40 : 0));
          int result_4720 = value2_4718 & 0xFF;
          F = (_F2301 & 0xFF);
          A = result_4720;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F2303;
          int value1_4721 = A;
          int value2_4722 = E;
          value2_4722 &= value1_4721;
          _F2303 = 0x10 | (SZ53P[value2_4722 & 0xff] | (value2_4722 == 0 ? 0x40 : 0));
          int result_4724 = value2_4722 & 0xFF;
          F = (_F2303 & 0xFF);
          A = result_4724;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _F2305;
          int value1_4725 = A;
          int value2_4726 = (IY >> 8);
          value2_4726 &= value1_4725;
          _F2305 = 0x10 | (SZ53P[value2_4726 & 0xff] | (value2_4726 == 0 ? 0x40 : 0));
          int result_4728 = value2_4726 & 0xFF;
          F = (_F2305 & 0xFF);
          A = result_4728;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _F2307;
          int value1_4729 = A;
          int value2_4730 = (IY & 0xFF);
          value2_4730 &= value1_4729;
          _F2307 = 0x10 | (SZ53P[value2_4730 & 0xff] | (value2_4730 == 0 ? 0x40 : 0));
          int result_4732 = value2_4730 & 0xFF;
          F = (_F2307 & 0xFF);
          A = result_4732;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _F2309;
          int _value2147;
          int _address2147;
          int operand_4733 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4733)) & 0xFFFF;
          int operand_4734 = read(_address2147, 0);
          _value2147 = operand_4734;
          int value1_4735 = A;
          int value2_4736 = _value2147;
          value2_4736 &= value1_4735;
          _F2309 = 0x10 | (SZ53P[value2_4736 & 0xff] | (value2_4736 == 0 ? 0x40 : 0));
          int result_4738 = value2_4736 & 0xFF;
          F = (_F2309 & 0xFF);
          A = result_4738;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _F2311;
          int value1_4739 = A;
          int value2_4740 = A;
          value2_4740 &= value1_4739;
          _F2311 = 0x10 | (SZ53P[value2_4740 & 0xff] | (value2_4740 == 0 ? 0x40 : 0));
          int result_4742 = value2_4740 & 0xFF;
          F = (_F2311 & 0xFF);
          A = result_4742;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_21(int opcode) {
    switch (opcode) {
      case 0xA8: {
          int _F2313;
          int value1_4743 = A;
          int value2_4744 = B;
          value2_4744 ^= value1_4743;
          _F2313 = SZ53P[value2_4744 & 0xff] | (value2_4744 == 0 ? 0x40 : 0);
          int result_4746 = value2_4744 & 0xFF;
          F = (_F2313 & 0xFF);
          A = result_4746;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F2315;
          int value1_4747 = A;
          int value2_4748 = C;
          value2_4748 ^= value1_4747;
          _F2315 = SZ53P[value2_4748 & 0xff] | (value2_4748 == 0 ? 0x40 : 0);
          int result_4750 = value2_4748 & 0xFF;
          F = (_F2315 & 0xFF);
          A = result_4750;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F2317;
          int value1_4751 = A;
          int value2_4752 = D;
          value2_4752 ^= value1_4751;
          _F2317 = SZ53P[value2_4752 & 0xff] | (value2_4752 == 0 ? 0x40 : 0);
          int result_4754 = value2_4752 & 0xFF;
          F = (_F2317 & 0xFF);
          A = result_4754;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F2319;
          int value1_4755 = A;
          int value2_4756 = E;
          value2_4756 ^= value1_4755;
          _F2319 = SZ53P[value2_4756 & 0xff] | (value2_4756 == 0 ? 0x40 : 0);
          int result_4758 = value2_4756 & 0xFF;
          F = (_F2319 & 0xFF);
          A = result_4758;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _F2321;
          int value1_4759 = A;
          int value2_4760 = (IY >> 8);
          value2_4760 ^= value1_4759;
          _F2321 = SZ53P[value2_4760 & 0xff] | (value2_4760 == 0 ? 0x40 : 0);
          int result_4762 = value2_4760 & 0xFF;
          F = (_F2321 & 0xFF);
          A = result_4762;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _F2323;
          int value1_4763 = A;
          int value2_4764 = (IY & 0xFF);
          value2_4764 ^= value1_4763;
          _F2323 = SZ53P[value2_4764 & 0xff] | (value2_4764 == 0 ? 0x40 : 0);
          int result_4766 = value2_4764 & 0xFF;
          F = (_F2323 & 0xFF);
          A = result_4766;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _F2325;
          int _value2147;
          int _address2147;
          int operand_4767 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4767)) & 0xFFFF;
          int operand_4768 = read(_address2147, 0);
          _value2147 = operand_4768;
          int value1_4769 = A;
          int value2_4770 = _value2147;
          value2_4770 ^= value1_4769;
          _F2325 = SZ53P[value2_4770 & 0xff] | (value2_4770 == 0 ? 0x40 : 0);
          int result_4772 = value2_4770 & 0xFF;
          F = (_F2325 & 0xFF);
          A = result_4772;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _F2327;
          int value1_4773 = A;
          int value2_4774 = A;
          value2_4774 ^= value1_4773;
          _F2327 = SZ53P[value2_4774 & 0xff] | (value2_4774 == 0 ? 0x40 : 0);
          int result_4776 = value2_4774 & 0xFF;
          F = (_F2327 & 0xFF);
          A = result_4776;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_22(int opcode) {
    switch (opcode) {
      case 0xB0: {
          int _F2329;
          int value1_4777 = A;
          int value2_4778 = B;
          value2_4778 |= value1_4777;
          _F2329 = SZ53P[value2_4778 & 0xff] | (value2_4778 == 0 ? 0x40 : 0);
          int result_4780 = value2_4778 & 0xFF;
          F = (_F2329 & 0xFF);
          A = result_4780;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _F2331;
          int value1_4781 = A;
          int value2_4782 = C;
          value2_4782 |= value1_4781;
          _F2331 = SZ53P[value2_4782 & 0xff] | (value2_4782 == 0 ? 0x40 : 0);
          int result_4784 = value2_4782 & 0xFF;
          F = (_F2331 & 0xFF);
          A = result_4784;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _F2333;
          int value1_4785 = A;
          int value2_4786 = D;
          value2_4786 |= value1_4785;
          _F2333 = SZ53P[value2_4786 & 0xff] | (value2_4786 == 0 ? 0x40 : 0);
          int result_4788 = value2_4786 & 0xFF;
          F = (_F2333 & 0xFF);
          A = result_4788;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _F2335;
          int value1_4789 = A;
          int value2_4790 = E;
          value2_4790 |= value1_4789;
          _F2335 = SZ53P[value2_4790 & 0xff] | (value2_4790 == 0 ? 0x40 : 0);
          int result_4792 = value2_4790 & 0xFF;
          F = (_F2335 & 0xFF);
          A = result_4792;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _F2337;
          int value1_4793 = A;
          int value2_4794 = (IY >> 8);
          value2_4794 |= value1_4793;
          _F2337 = SZ53P[value2_4794 & 0xff] | (value2_4794 == 0 ? 0x40 : 0);
          int result_4796 = value2_4794 & 0xFF;
          F = (_F2337 & 0xFF);
          A = result_4796;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _F2339;
          int value1_4797 = A;
          int value2_4798 = (IY & 0xFF);
          value2_4798 |= value1_4797;
          _F2339 = SZ53P[value2_4798 & 0xff] | (value2_4798 == 0 ? 0x40 : 0);
          int result_4800 = value2_4798 & 0xFF;
          F = (_F2339 & 0xFF);
          A = result_4800;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _F2341;
          int _value2147;
          int _address2147;
          int operand_4801 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4801)) & 0xFFFF;
          int operand_4802 = read(_address2147, 0);
          _value2147 = operand_4802;
          int value1_4803 = A;
          int value2_4804 = _value2147;
          value2_4804 |= value1_4803;
          _F2341 = SZ53P[value2_4804 & 0xff] | (value2_4804 == 0 ? 0x40 : 0);
          int result_4806 = value2_4804 & 0xFF;
          F = (_F2341 & 0xFF);
          A = result_4806;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _F2343;
          int value1_4807 = A;
          int value2_4808 = A;
          value2_4808 |= value1_4807;
          _F2343 = SZ53P[value2_4808 & 0xff] | (value2_4808 == 0 ? 0x40 : 0);
          int result_4810 = value2_4808 & 0xFF;
          F = (_F2343 & 0xFF);
          A = result_4810;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_23(int opcode) {
    switch (opcode) {
      case 0xB8: {
          int _F2345;
          int cptemp_4814 = A - B;
          int lookup_4815 = ((A & 0x88) >> 3) | ((B & 0x88) >> 2) | ((cptemp_4814 & 0x88) >> 1);
          _F2345 = ((cptemp_4814 & 0x100) != 0 ? 1 : (cptemp_4814 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4815 & 0x07)] | OVERFLOW_SUB[(lookup_4815 >> 4)] | (B & 0x28) | (cptemp_4814 & 0x80);
          F = (_F2345 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _F2347;
          int cptemp_4820 = A - C;
          int lookup_4821 = ((A & 0x88) >> 3) | ((C & 0x88) >> 2) | ((cptemp_4820 & 0x88) >> 1);
          _F2347 = ((cptemp_4820 & 0x100) != 0 ? 1 : (cptemp_4820 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4821 & 0x07)] | OVERFLOW_SUB[(lookup_4821 >> 4)] | (C & 0x28) | (cptemp_4820 & 0x80);
          F = (_F2347 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _F2349;
          int cptemp_4826 = A - D;
          int lookup_4827 = ((A & 0x88) >> 3) | ((D & 0x88) >> 2) | ((cptemp_4826 & 0x88) >> 1);
          _F2349 = ((cptemp_4826 & 0x100) != 0 ? 1 : (cptemp_4826 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4827 & 0x07)] | OVERFLOW_SUB[(lookup_4827 >> 4)] | (D & 0x28) | (cptemp_4826 & 0x80);
          F = (_F2349 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _F2351;
          int cptemp_4832 = A - E;
          int lookup_4833 = ((A & 0x88) >> 3) | ((E & 0x88) >> 2) | ((cptemp_4832 & 0x88) >> 1);
          _F2351 = ((cptemp_4832 & 0x100) != 0 ? 1 : (cptemp_4832 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4833 & 0x07)] | OVERFLOW_SUB[(lookup_4833 >> 4)] | (E & 0x28) | (cptemp_4832 & 0x80);
          F = (_F2351 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _F2353;
          int value2_4836 = (IY >> 8);
          int cptemp_4838 = A - value2_4836;
          int lookup_4839 = ((A & 0x88) >> 3) | ((value2_4836 & 0x88) >> 2) | ((cptemp_4838 & 0x88) >> 1);
          _F2353 = ((cptemp_4838 & 0x100) != 0 ? 1 : (cptemp_4838 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4839 & 0x07)] | OVERFLOW_SUB[(lookup_4839 >> 4)] | (value2_4836 & 0x28) | (cptemp_4838 & 0x80);
          F = (_F2353 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _F2355;
          int value2_4842 = (IY & 0xFF);
          int cptemp_4844 = A - value2_4842;
          int lookup_4845 = ((A & 0x88) >> 3) | ((value2_4842 & 0x88) >> 2) | ((cptemp_4844 & 0x88) >> 1);
          _F2355 = ((cptemp_4844 & 0x100) != 0 ? 1 : (cptemp_4844 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4845 & 0x07)] | OVERFLOW_SUB[(lookup_4845 >> 4)] | (value2_4842 & 0x28) | (cptemp_4844 & 0x80);
          F = (_F2355 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _F2357;
          int _value2147;
          int _address2147;
          int operand_4847 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4847)) & 0xFFFF;
          int operand_4848 = read(_address2147, 0);
          _value2147 = operand_4848;
          int cptemp_4852 = A - _value2147;
          int lookup_4853 = ((A & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((cptemp_4852 & 0x88) >> 1);
          _F2357 = ((cptemp_4852 & 0x100) != 0 ? 1 : (cptemp_4852 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4853 & 0x07)] | OVERFLOW_SUB[(lookup_4853 >> 4)] | (_value2147 & 0x28) | (cptemp_4852 & 0x80);
          F = (_F2357 & 0xFF);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _F2359;
          int cptemp_4858 = A - A;
          int lookup_4859 = ((A & 0x88) >> 3) | ((A & 0x88) >> 2) | ((cptemp_4858 & 0x88) >> 1);
          _F2359 = ((cptemp_4858 & 0x100) != 0 ? 1 : (cptemp_4858 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4859 & 0x07)] | OVERFLOW_SUB[(lookup_4859 >> 4)] | (A & 0x28) | (cptemp_4858 & 0x80);
          F = (_F2359 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_24(int opcode) {
    switch (opcode) {
      case 0xC0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_4861 = SP;
          if ((!((F & 0x40) == 0x40))) {
              int wordNumber1_4863 = read(SP, 0);
              int wordNumber_4864 = read((SP + 1) & 0xFFFF, 0);
              int value_4862 = ((wordNumber_4864 << 8) | wordNumber1_4863);
              int wordNumber_4865 = SP;
              SP = ((wordNumber_4865 + 2) & 0xFFFF);
              jumpAddress2_4861 = value_4862;
              MEMPTR = jumpAddress2_4861;
              PC = jumpAddress2_4861;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC1: {
          int wordNumber1_4869 = read(SP, 0);
          int wordNumber_4870 = read((SP + 1) & 0xFFFF, 0);
          int value_4868 = ((wordNumber_4870 << 8) | wordNumber1_4869);
          int wordNumber_4871 = SP;
          SP = ((wordNumber_4871 + 2) & 0xFFFF);
          B = (value_4868 >>> 8);
          C = value_4868 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _jumpAddress2363 = 0;
          int address_4873 = (PC + 2) & 0xFFFF;
          int operand_4875 = read(address_4873, 0);
          int operand_4877 = read((address_4873 + 1) & 0xFFFF, 0);
          int jumpAddress2_4872 = (_jumpAddress2363 = (operand_4877 << 8) | operand_4875);
          if ((!((F & 0x40) == 0x40))) {
              _jumpAddress2363 = jumpAddress2_4872;
              MEMPTR = _jumpAddress2363;
              PC = jumpAddress2_4872;
              break;
          } else {
              MEMPTR = _jumpAddress2363;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xC3: {
          int _nextPC2364;
          int _jumpAddress2364;
          int address_4880 = (PC + 2) & 0xFFFF;
          int operand_4882 = read(address_4880, 0);
          int operand_4884 = read((address_4880 + 1) & 0xFFFF, 0);
          int jumpAddress2_4879 = (_jumpAddress2364 = (operand_4884 << 8) | operand_4882);
          _jumpAddress2364 = jumpAddress2_4879;
          _nextPC2364 = jumpAddress2_4879;
          MEMPTR = _jumpAddress2364;
          PC = _nextPC2364;
          break;
      }
      case 0xC4: {
          int _jumpAddress2365 = 0;
          int address_4886 = (PC + 2) & 0xFFFF;
          int operand_4888 = read(address_4886, 0);
          int operand_4890 = read((address_4886 + 1) & 0xFFFF, 0);
          int value_4891 = (_jumpAddress2365 = (operand_4890 << 8) | operand_4888);
          MEMPTR = value_4891;
          int jumpAddress2_4892 = (_jumpAddress2365 = (operand_4890 << 8) | operand_4888);
          if ((!((F & 0x40) == 0x40))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4896 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4896 >>> 8));
              write(SP, (value_4896 & 0xFF));
              _jumpAddress2365 = jumpAddress2_4892;
              MEMPTR = _jumpAddress2365;
              PC = jumpAddress2_4892;
              break;
          } else {
              MEMPTR = _jumpAddress2365;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xC5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4898 = ((B << 8) | C);
          write((SP + 1) & 0xFFFF, (value_4898 >>> 8));
          write(SP, (value_4898 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _F2367;
          int operand_4899 = read((PC + 2) & 0xFFFF, 0);
          int value1_4900 = A;
          int value2_4901 = operand_4899;
          int addtemp_4903 = value2_4901 + value1_4900;
          int lookup_4904 = ((value2_4901 & 0x88) >> 3) | ((value1_4900 & 0x88) >> 2) | ((addtemp_4903 & 0x88) >> 1);
          value2_4901 = addtemp_4903 & 0xff;
          _F2367 = ((addtemp_4903 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4904 & 0x07)] | OVERFLOW_ADD[(lookup_4904 >> 4)] | (SZ53[value2_4901] | (value2_4901 == 0 ? 0x40 : 0));
          F = (_F2367 & 0xFF);
          A = value2_4901;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _nextPC2369;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4906 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4906 >>> 8));
          write(SP, (value_4906 & 0xFF));
          _nextPC2369 = 0;
          MEMPTR = _nextPC2369 & 0xFFFF;
          PC = _nextPC2369 == -1 ? (PC + 2) & 0xFFFF : _nextPC2369;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_25(int opcode) {
    switch (opcode) {
      case 0xC8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_4907 = SP;
          if (((F & 0x40) == 0x40)) {
              int wordNumber1_4909 = read(SP, 0);
              int wordNumber_4910 = read((SP + 1) & 0xFFFF, 0);
              int value_4908 = ((wordNumber_4910 << 8) | wordNumber1_4909);
              int wordNumber_4911 = SP;
              SP = ((wordNumber_4911 + 2) & 0xFFFF);
              jumpAddress2_4907 = value_4908;
              MEMPTR = jumpAddress2_4907;
              PC = jumpAddress2_4907;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC9: {
          int _nextPC2371;
          int jumpAddress2_4913;
          int wordNumber1_4915 = read(SP, 0);
          int wordNumber_4916 = read((SP + 1) & 0xFFFF, 0);
          int value_4914 = ((wordNumber_4916 << 8) | wordNumber1_4915);
          int wordNumber_4917 = SP;
          SP = ((wordNumber_4917 + 2) & 0xFFFF);
          jumpAddress2_4913 = value_4914;
          _nextPC2371 = jumpAddress2_4913;
          MEMPTR = _nextPC2371;
          PC = _nextPC2371;
          break;
      }
      case 0xCA: {
          int _jumpAddress2372 = 0;
          int address_4920 = (PC + 2) & 0xFFFF;
          int operand_4922 = read(address_4920, 0);
          int operand_4924 = read((address_4920 + 1) & 0xFFFF, 0);
          int jumpAddress2_4919 = (_jumpAddress2372 = (operand_4924 << 8) | operand_4922);
          if (((F & 0x40) == 0x40)) {
              _jumpAddress2372 = jumpAddress2_4919;
              MEMPTR = _jumpAddress2372;
              PC = jumpAddress2_4919;
              break;
          } else {
              MEMPTR = _jumpAddress2372;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xCB: {
          int displacement = read((PC + 2) & 0xFFFF, 0);
          decodeFDCB(read((PC + 3) & 0xFFFF, 2), displacement);
          break;
      }
      case 0xCC: {
          int _jumpAddress3013 = 0;
          int address_6134 = (PC + 2) & 0xFFFF;
          int operand_6136 = read(address_6134, 0);
          int operand_6138 = read((address_6134 + 1) & 0xFFFF, 0);
          int value_6139 = (_jumpAddress3013 = (operand_6138 << 8) | operand_6136);
          MEMPTR = value_6139;
          int jumpAddress2_6140 = (_jumpAddress3013 = (operand_6138 << 8) | operand_6136);
          if (((F & 0x40) == 0x40)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6144 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6144 >>> 8));
              write(SP, (value_6144 & 0xFF));
              _jumpAddress3013 = jumpAddress2_6140;
              MEMPTR = _jumpAddress3013;
              PC = jumpAddress2_6140;
              break;
          } else {
              MEMPTR = _jumpAddress3013;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xCD: {
          int _nextPC3014;
          int _jumpAddress3014;
          int address_6146 = (PC + 2) & 0xFFFF;
          int operand_6148 = read(address_6146, 0);
          int operand_6150 = read((address_6146 + 1) & 0xFFFF, 0);
          int value_6151 = (_jumpAddress3014 = (operand_6150 << 8) | operand_6148);
          int jumpAddress2_6152 = (_jumpAddress3014 = (operand_6150 << 8) | operand_6148);
          SP = ((SP - 2) & 0xFFFF);
          int value_6156 = ((PC + 4) & 0xFFFF);
          contend1x1((PC + 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6156 >>> 8));
          write(SP, (value_6156 & 0xFF));
          _jumpAddress3014 = jumpAddress2_6152;
          _nextPC3014 = jumpAddress2_6152;
          MEMPTR = _jumpAddress3014;
          PC = _nextPC3014;
          break;
      }
      case 0xCE: {
          int _F3015;
          int operand_6158 = read((PC + 2) & 0xFFFF, 0);
          int value1_6159 = A;
          int value3_6161 = F & 1;
          _F3015 = value3_6161;
          int adctemp_6162 = value1_6159 + operand_6158 + (_F3015 & 1);
          int lookup_6163 = ((value1_6159 & 0x88) >> 3) | ((operand_6158 & 0x88) >> 2) | ((adctemp_6162 & 0x88) >> 1);
          value1_6159 = adctemp_6162 & 0xff;
          _F3015 = ((adctemp_6162 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_6163 & 0x07)] | OVERFLOW_ADD[(lookup_6163 >> 4)] | (SZ53[value1_6159] | (value1_6159 == 0 ? 0x40 : 0));
          F = (_F3015 & 0xFF);
          A = value1_6159;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _nextPC3017;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6165 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6165 >>> 8));
          write(SP, (value_6165 & 0xFF));
          _nextPC3017 = 8;
          MEMPTR = _nextPC3017;
          PC = _nextPC3017;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_26(int opcode) {
    switch (opcode) {
      case 0xD0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6166 = SP;
          if ((!((F & 1) == 1))) {
              int wordNumber1_6168 = read(SP, 0);
              int wordNumber_6169 = read((SP + 1) & 0xFFFF, 0);
              int value_6167 = ((wordNumber_6169 << 8) | wordNumber1_6168);
              int wordNumber_6170 = SP;
              SP = ((wordNumber_6170 + 2) & 0xFFFF);
              jumpAddress2_6166 = value_6167;
              MEMPTR = jumpAddress2_6166;
              PC = jumpAddress2_6166;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD1: {
          int wordNumber1_6174 = read(SP, 0);
          int wordNumber_6175 = read((SP + 1) & 0xFFFF, 0);
          int value_6173 = ((wordNumber_6175 << 8) | wordNumber1_6174);
          int wordNumber_6176 = SP;
          SP = ((wordNumber_6176 + 2) & 0xFFFF);
          D = (value_6173 >>> 8);
          E = value_6173 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _jumpAddress3020 = 0;
          int address_6178 = (PC + 2) & 0xFFFF;
          int operand_6180 = read(address_6178, 0);
          int operand_6182 = read((address_6178 + 1) & 0xFFFF, 0);
          int jumpAddress2_6177 = (_jumpAddress3020 = (operand_6182 << 8) | operand_6180);
          if ((!((F & 1) == 1))) {
              _jumpAddress3020 = jumpAddress2_6177;
              MEMPTR = _jumpAddress3020;
              PC = jumpAddress2_6177;
              break;
          } else {
              MEMPTR = _jumpAddress3020;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD3: {
          int operand_6185 = read((PC + 2) & 0xFFFF, 0);
          int read_6184 = operand_6185;
          read_6184 = (read_6184 | A << 8);
          io.out(read_6184, A);
          MEMPTR = (A << 8);
          int read_6186 = operand_6185;
          read_6186 = (read_6186 | A << 8);
          MEMPTR = (MEMPTR | ((read_6186 + 1) & 0xff));
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _jumpAddress3022 = 0;
          int address_6187 = (PC + 2) & 0xFFFF;
          int operand_6189 = read(address_6187, 0);
          int operand_6191 = read((address_6187 + 1) & 0xFFFF, 0);
          int value_6192 = (_jumpAddress3022 = (operand_6191 << 8) | operand_6189);
          MEMPTR = value_6192;
          int jumpAddress2_6193 = (_jumpAddress3022 = (operand_6191 << 8) | operand_6189);
          if ((!((F & 1) == 1))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6197 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6197 >>> 8));
              write(SP, (value_6197 & 0xFF));
              _jumpAddress3022 = jumpAddress2_6193;
              MEMPTR = _jumpAddress3022;
              PC = jumpAddress2_6193;
              break;
          } else {
              MEMPTR = _jumpAddress3022;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6199 = ((D << 8) | E);
          write((SP + 1) & 0xFFFF, (value_6199 >>> 8));
          write(SP, (value_6199 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _F3024;
          int operand_6200 = read((PC + 2) & 0xFFFF, 0);
          int value1_6201 = A;
          int subtemp_6204 = value1_6201 - operand_6200;
          int lookup_6205 = ((value1_6201 & 0x88) >> 3) | ((operand_6200 & 0x88) >> 2) | ((subtemp_6204 & 0x88) >> 1);
          value1_6201 = subtemp_6204 & 0xff;
          _F3024 = ((subtemp_6204 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_6205 & 0x07)] | OVERFLOW_SUB[(lookup_6205 >> 4)] | (SZ53[value1_6201] | (value1_6201 == 0 ? 0x40 : 0));
          F = (_F3024 & 0xFF);
          A = value1_6201;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _nextPC3026;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6207 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6207 >>> 8));
          write(SP, (value_6207 & 0xFF));
          _nextPC3026 = 0x10;
          MEMPTR = _nextPC3026;
          PC = _nextPC3026;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_27(int opcode) {
    switch (opcode) {
      case 0xD8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6208 = SP;
          if (((F & 1) == 1)) {
              int wordNumber1_6210 = read(SP, 0);
              int wordNumber_6211 = read((SP + 1) & 0xFFFF, 0);
              int value_6209 = ((wordNumber_6211 << 8) | wordNumber1_6210);
              int wordNumber_6212 = SP;
              SP = ((wordNumber_6212 + 2) & 0xFFFF);
              jumpAddress2_6208 = value_6209;
              MEMPTR = jumpAddress2_6208;
              PC = jumpAddress2_6208;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD9: {
          int v1_6214 = ((B << 8) | C);
          B = (_BC >>> 8);
          C = _BC & 0xFF;
          _BC = v1_6214;
          v1_6214 = (D << 8) | E;
          D = (_DE >>> 8);
          E = _DE & 0xFF;
          _DE = v1_6214;
          v1_6214 = (H << 8) | L;
          H = (_HL >>> 8);
          L = _HL & 0xFF;
          _HL = v1_6214;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _jumpAddress3029 = 0;
          int address_6216 = (PC + 2) & 0xFFFF;
          int operand_6218 = read(address_6216, 0);
          int operand_6220 = read((address_6216 + 1) & 0xFFFF, 0);
          int jumpAddress2_6215 = (_jumpAddress3029 = (operand_6220 << 8) | operand_6218);
          if (((F & 1) == 1)) {
              _jumpAddress3029 = jumpAddress2_6215;
              MEMPTR = _jumpAddress3029;
              PC = jumpAddress2_6215;
              break;
          } else {
              MEMPTR = _jumpAddress3029;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDB: {
          int operand_6223 = read((PC + 2) & 0xFFFF, 0);
          MEMPTR = (((operand_6223 | A << 8) + 1) & 0xFFFF);
          int port_6224 = (operand_6223 | A << 8);
          int value_6226 = io.in(port_6224);
          A = value_6226 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress3031 = 0;
          int address_6227 = (PC + 2) & 0xFFFF;
          int operand_6229 = read(address_6227, 0);
          int operand_6231 = read((address_6227 + 1) & 0xFFFF, 0);
          int value_6232 = (_jumpAddress3031 = (operand_6231 << 8) | operand_6229);
          MEMPTR = value_6232;
          int jumpAddress2_6233 = (_jumpAddress3031 = (operand_6231 << 8) | operand_6229);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6237 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6237 >>> 8));
              write(SP, (value_6237 & 0xFF));
              _jumpAddress3031 = jumpAddress2_6233;
              MEMPTR = _jumpAddress3031;
              PC = jumpAddress2_6233;
              break;
          } else {
              MEMPTR = _jumpAddress3031;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _F3033;
          int operand_6239 = read((PC + 2) & 0xFFFF, 0);
          int value1_6240 = A;
          int value3_6242 = F & 1;
          _F3033 = value3_6242;
          int sbctemp_6243 = value1_6240 - operand_6239 - (_F3033 & 1);
          int lookup_6244 = ((value1_6240 & 0x88) >> 3) | ((operand_6239 & 0x88) >> 2) | ((sbctemp_6243 & 0x88) >> 1);
          value1_6240 = sbctemp_6243 & 0xff;
          _F3033 = ((sbctemp_6243 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_6244 & 0x07)] | OVERFLOW_SUB[(lookup_6244 >> 4)] | (SZ53[value1_6240] | (value1_6240 == 0 ? 0x40 : 0));
          F = (_F3033 & 0xFF);
          A = value1_6240;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC3035;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6246 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6246 >>> 8));
          write(SP, (value_6246 & 0xFF));
          _nextPC3035 = 0x18;
          MEMPTR = _nextPC3035;
          PC = _nextPC3035;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_28(int opcode) {
    switch (opcode) {
      case 0xE0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6247 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_6249 = read(SP, 0);
              int wordNumber_6250 = read((SP + 1) & 0xFFFF, 0);
              int value_6248 = ((wordNumber_6250 << 8) | wordNumber1_6249);
              int wordNumber_6251 = SP;
              SP = ((wordNumber_6251 + 2) & 0xFFFF);
              jumpAddress2_6247 = value_6248;
              MEMPTR = jumpAddress2_6247;
              PC = jumpAddress2_6247;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_6255 = read(SP, 0);
          int wordNumber_6256 = read((SP + 1) & 0xFFFF, 0);
          int value_6254 = ((wordNumber_6256 << 8) | wordNumber1_6255);
          int wordNumber_6257 = SP;
          SP = ((wordNumber_6257 + 2) & 0xFFFF);
          IY = value_6254;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress3038 = 0;
          int address_6259 = (PC + 2) & 0xFFFF;
          int operand_6261 = read(address_6259, 0);
          int operand_6263 = read((address_6259 + 1) & 0xFFFF, 0);
          int jumpAddress2_6258 = (_jumpAddress3038 = (operand_6263 << 8) | operand_6261);
          if ((!((F & 4) == 4))) {
              _jumpAddress3038 = jumpAddress2_6258;
              MEMPTR = _jumpAddress3038;
              PC = jumpAddress2_6258;
              break;
          } else {
              MEMPTR = _jumpAddress3038;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address3039 = SP;
          int wordNumber1_6266 = read(_address3039, 0);
          int wordNumber_6267 = read((_address3039 + 1) & 0xFFFF, 0);
          int v1_6265 = ((wordNumber_6267 << 8) | wordNumber1_6266);
          int v2_6268 = IY;
          _address3039 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address3039 + 1) & 0xFFFF, (v2_6268 >>> 8));
          write(_address3039, (v2_6268 & 0xFF));
          IY = v1_6265;
          MEMPTR = IY;
          contend2x1(SP);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress3041 = 0;
          int address_6269 = (PC + 2) & 0xFFFF;
          int operand_6271 = read(address_6269, 0);
          int operand_6273 = read((address_6269 + 1) & 0xFFFF, 0);
          int value_6274 = (_jumpAddress3041 = (operand_6273 << 8) | operand_6271);
          MEMPTR = value_6274;
          int jumpAddress2_6275 = (_jumpAddress3041 = (operand_6273 << 8) | operand_6271);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6279 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6279 >>> 8));
              write(SP, (value_6279 & 0xFF));
              _jumpAddress3041 = jumpAddress2_6275;
              MEMPTR = _jumpAddress3041;
              PC = jumpAddress2_6275;
              break;
          } else {
              MEMPTR = _jumpAddress3041;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (IY >>> 8));
          write(SP, (IY & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _F3043;
          int operand_6281 = read((PC + 2) & 0xFFFF, 0);
          int value1_6282 = A;
          int value2_6283 = operand_6281;
          value2_6283 &= value1_6282;
          _F3043 = 0x10 | (SZ53P[value2_6283 & 0xff] | (value2_6283 == 0 ? 0x40 : 0));
          int result_6285 = value2_6283 & 0xFF;
          F = (_F3043 & 0xFF);
          A = result_6285;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC3045;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6286 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6286 >>> 8));
          write(SP, (value_6286 & 0xFF));
          _nextPC3045 = 0x20;
          MEMPTR = _nextPC3045;
          PC = _nextPC3045;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_29(int opcode) {
    switch (opcode) {
      case 0xE8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6287 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_6289 = read(SP, 0);
              int wordNumber_6290 = read((SP + 1) & 0xFFFF, 0);
              int value_6288 = ((wordNumber_6290 << 8) | wordNumber1_6289);
              int wordNumber_6291 = SP;
              SP = ((wordNumber_6291 + 2) & 0xFFFF);
              jumpAddress2_6287 = value_6288;
              MEMPTR = jumpAddress2_6287;
              PC = jumpAddress2_6287;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE9: {
          MEMPTR = 0;
          PC = IY;
          break;
      }
      case 0xEA: {
          int _jumpAddress3048 = 0;
          int address_6296 = (PC + 2) & 0xFFFF;
          int operand_6298 = read(address_6296, 0);
          int operand_6300 = read((address_6296 + 1) & 0xFFFF, 0);
          int jumpAddress2_6295 = (_jumpAddress3048 = (operand_6300 << 8) | operand_6298);
          if (((F & 4) == 4)) {
              _jumpAddress3048 = jumpAddress2_6295;
              MEMPTR = _jumpAddress3048;
              PC = jumpAddress2_6295;
              break;
          } else {
              MEMPTR = _jumpAddress3048;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_6302 = ((D << 8) | E);
          int v2_6303 = ((H << 8) | L);
          D = (v2_6303 >>> 8);
          E = v2_6303 & 0xFF;
          H = (v1_6302 >>> 8);
          L = v1_6302 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress3050 = 0;
          int address_6304 = (PC + 2) & 0xFFFF;
          int operand_6306 = read(address_6304, 0);
          int operand_6308 = read((address_6304 + 1) & 0xFFFF, 0);
          int value_6309 = (_jumpAddress3050 = (operand_6308 << 8) | operand_6306);
          MEMPTR = value_6309;
          int jumpAddress2_6310 = (_jumpAddress3050 = (operand_6308 << 8) | operand_6306);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6314 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6314 >>> 8));
              write(SP, (value_6314 & 0xFF));
              _jumpAddress3050 = jumpAddress2_6310;
              MEMPTR = _jumpAddress3050;
              PC = jumpAddress2_6310;
              break;
          } else {
              MEMPTR = _jumpAddress3050;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xED: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _F3052;
          int operand_6316 = read((PC + 2) & 0xFFFF, 0);
          int value1_6317 = A;
          int value2_6318 = operand_6316;
          value2_6318 ^= value1_6317;
          _F3052 = SZ53P[value2_6318 & 0xff] | (value2_6318 == 0 ? 0x40 : 0);
          int result_6320 = value2_6318 & 0xFF;
          F = (_F3052 & 0xFF);
          A = result_6320;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC3054;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6321 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6321 >>> 8));
          write(SP, (value_6321 & 0xFF));
          _nextPC3054 = 0x28;
          MEMPTR = _nextPC3054;
          PC = _nextPC3054;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_30(int opcode) {
    switch (opcode) {
      case 0xF0: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6322 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_6324 = read(SP, 0);
              int wordNumber_6325 = read((SP + 1) & 0xFFFF, 0);
              int value_6323 = ((wordNumber_6325 << 8) | wordNumber1_6324);
              int wordNumber_6326 = SP;
              SP = ((wordNumber_6326 + 2) & 0xFFFF);
              jumpAddress2_6322 = value_6323;
              MEMPTR = jumpAddress2_6322;
              PC = jumpAddress2_6322;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_6330 = read(SP, 0);
          int wordNumber_6331 = read((SP + 1) & 0xFFFF, 0);
          int value_6329 = ((wordNumber_6331 << 8) | wordNumber1_6330);
          int wordNumber_6332 = SP;
          SP = ((wordNumber_6332 + 2) & 0xFFFF);
          A = (value_6329 >>> 8);
          F = value_6329 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress3057 = 0;
          int address_6334 = (PC + 2) & 0xFFFF;
          int operand_6336 = read(address_6334, 0);
          int operand_6338 = read((address_6334 + 1) & 0xFFFF, 0);
          int jumpAddress2_6333 = (_jumpAddress3057 = (operand_6338 << 8) | operand_6336);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress3057 = jumpAddress2_6333;
              MEMPTR = _jumpAddress3057;
              PC = jumpAddress2_6333;
              break;
          } else {
              MEMPTR = _jumpAddress3057;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xF3: {
          state.resetInterrupt();
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _jumpAddress3059 = 0;
          int address_6340 = (PC + 2) & 0xFFFF;
          int operand_6342 = read(address_6340, 0);
          int operand_6344 = read((address_6340 + 1) & 0xFFFF, 0);
          int value_6345 = (_jumpAddress3059 = (operand_6344 << 8) | operand_6342);
          MEMPTR = value_6345;
          int jumpAddress2_6346 = (_jumpAddress3059 = (operand_6344 << 8) | operand_6342);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6350 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6350 >>> 8));
              write(SP, (value_6350 & 0xFF));
              _jumpAddress3059 = jumpAddress2_6346;
              MEMPTR = _jumpAddress3059;
              PC = jumpAddress2_6346;
              break;
          } else {
              MEMPTR = _jumpAddress3059;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xF5: {
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6352 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_6352 >>> 8));
          write(SP, (value_6352 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F3061;
          int operand_6353 = read((PC + 2) & 0xFFFF, 0);
          int value1_6354 = A;
          int value2_6355 = operand_6353;
          value2_6355 |= value1_6354;
          _F3061 = SZ53P[value2_6355 & 0xff] | (value2_6355 == 0 ? 0x40 : 0);
          int result_6357 = value2_6355 & 0xFF;
          F = (_F3061 & 0xFF);
          A = result_6357;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC3063;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6358 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6358 >>> 8));
          write(SP, (value_6358 & 0xFF));
          _nextPC3063 = 0x30;
          MEMPTR = _nextPC3063;
          PC = _nextPC3063;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFD_31(int opcode) {
    switch (opcode) {
      case 0xF8: {
          contend1x1(((I << 8) | R));
          int jumpAddress2_6359 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_6361 = read(SP, 0);
              int wordNumber_6362 = read((SP + 1) & 0xFFFF, 0);
              int value_6360 = ((wordNumber_6362 << 8) | wordNumber1_6361);
              int wordNumber_6363 = SP;
              SP = ((wordNumber_6363 + 2) & 0xFFFF);
              jumpAddress2_6359 = value_6360;
              MEMPTR = jumpAddress2_6359;
              PC = jumpAddress2_6359;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF9: {
          contend2x1(((I << 8) | R));
          SP = IY;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _jumpAddress3066 = 0;
          int address_6366 = (PC + 2) & 0xFFFF;
          int operand_6368 = read(address_6366, 0);
          int operand_6370 = read((address_6366 + 1) & 0xFFFF, 0);
          int jumpAddress2_6365 = (_jumpAddress3066 = (operand_6370 << 8) | operand_6368);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress3066 = jumpAddress2_6365;
              MEMPTR = _jumpAddress3066;
              PC = jumpAddress2_6365;
              break;
          } else {
              MEMPTR = _jumpAddress3066;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xFB: {
          state.enableInterrupt();
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _jumpAddress3068 = 0;
          int address_6372 = (PC + 2) & 0xFFFF;
          int operand_6374 = read(address_6372, 0);
          int operand_6376 = read((address_6372 + 1) & 0xFFFF, 0);
          int value_6377 = (_jumpAddress3068 = (operand_6376 << 8) | operand_6374);
          MEMPTR = value_6377;
          int jumpAddress2_6378 = (_jumpAddress3068 = (operand_6376 << 8) | operand_6374);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6382 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6382 >>> 8));
              write(SP, (value_6382 & 0xFF));
              _jumpAddress3068 = jumpAddress2_6378;
              MEMPTR = _jumpAddress3068;
              PC = jumpAddress2_6378;
              break;
          } else {
              MEMPTR = _jumpAddress3068;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xFD: {
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _F3070;
          int operand_6384 = read((PC + 2) & 0xFFFF, 0);
          int cptemp_6388 = A - operand_6384;
          int lookup_6389 = ((A & 0x88) >> 3) | ((operand_6384 & 0x88) >> 2) | ((cptemp_6388 & 0x88) >> 1);
          _F3070 = ((cptemp_6388 & 0x100) != 0 ? 1 : (cptemp_6388 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_6389 & 0x07)] | OVERFLOW_SUB[(lookup_6389 >> 4)] | (operand_6384 & 0x28) | (cptemp_6388 & 0x80);
          F = (_F3070 & 0xFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC3072;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6391 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6391 >>> 8));
          write(SP, (value_6391 & 0xFF));
          _nextPC3072 = 0x38;
          MEMPTR = _nextPC3072;
          PC = _nextPC3072;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFD");
    }
  }

  private void decodeFDCB(int opcode, int displacement) {
    switch (opcode >> 3) {
      case 0: decodeFDCB_0(opcode, displacement);
        break;
      case 1: decodeFDCB_1(opcode, displacement);
        break;
      case 2: decodeFDCB_2(opcode, displacement);
        break;
      case 3: decodeFDCB_3(opcode, displacement);
        break;
      case 4: decodeFDCB_4(opcode, displacement);
        break;
      case 5: decodeFDCB_5(opcode, displacement);
        break;
      case 6: decodeFDCB_6(opcode, displacement);
        break;
      case 7: decodeFDCB_7(opcode, displacement);
        break;
      case 8: decodeFDCB_8(opcode, displacement);
        break;
      case 9: decodeFDCB_9(opcode, displacement);
        break;
      case 10: decodeFDCB_10(opcode, displacement);
        break;
      case 11: decodeFDCB_11(opcode, displacement);
        break;
      case 12: decodeFDCB_12(opcode, displacement);
        break;
      case 13: decodeFDCB_13(opcode, displacement);
        break;
      case 14: decodeFDCB_14(opcode, displacement);
        break;
      case 15: decodeFDCB_15(opcode, displacement);
        break;
      case 16: decodeFDCB_16(opcode, displacement);
        break;
      case 17: decodeFDCB_17(opcode, displacement);
        break;
      case 18: decodeFDCB_18(opcode, displacement);
        break;
      case 19: decodeFDCB_19(opcode, displacement);
        break;
      case 20: decodeFDCB_20(opcode, displacement);
        break;
      case 21: decodeFDCB_21(opcode, displacement);
        break;
      case 22: decodeFDCB_22(opcode, displacement);
        break;
      case 23: decodeFDCB_23(opcode, displacement);
        break;
      case 24: decodeFDCB_24(opcode, displacement);
        break;
      case 25: decodeFDCB_25(opcode, displacement);
        break;
      case 26: decodeFDCB_26(opcode, displacement);
        break;
      case 27: decodeFDCB_27(opcode, displacement);
        break;
      case 28: decodeFDCB_28(opcode, displacement);
        break;
      case 29: decodeFDCB_29(opcode, displacement);
        break;
      case 30: decodeFDCB_30(opcode, displacement);
        break;
      case 31: decodeFDCB_31(opcode, displacement);
        break;
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_0(int opcode, int displacement) {
    switch (opcode) {
      case 0x00: {
          int _F2374;
          int _value2373;
          int _address2373;
          contend2x1((PC + 3) & 0xFFFF);
          _address2373 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4926 = read(_address2373, 0);
          contend1x1(_address2373);
          _value2373 = operand_4926;
          int value1_4927 = _value2373;
          value1_4927 = (value1_4927 << 1 | value1_4927 >> 7) & 0xff;
          _F2374 = (value1_4927 & 1) | (SZ53P[value1_4927] | (value1_4927 == 0 ? 0x40 : 0));
          F = (_F2374 & 0xFF);
          _address2373 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2373 = value1_4927;
          write(_address2373, value1_4927);
          int read_4931;
          read_4931 = _value2373;
          B = read_4931;
          MEMPTR = _address2373;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x01: {
          int _F2377;
          int _value2376;
          int _address2376;
          contend2x1((PC + 3) & 0xFFFF);
          _address2376 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4932 = read(_address2376, 0);
          contend1x1(_address2376);
          _value2376 = operand_4932;
          int value1_4933 = _value2376;
          value1_4933 = (value1_4933 << 1 | value1_4933 >> 7) & 0xff;
          _F2377 = (value1_4933 & 1) | (SZ53P[value1_4933] | (value1_4933 == 0 ? 0x40 : 0));
          F = (_F2377 & 0xFF);
          _address2376 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2376 = value1_4933;
          write(_address2376, value1_4933);
          int read_4937;
          read_4937 = _value2376;
          C = read_4937;
          MEMPTR = _address2376;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x02: {
          int _F2380;
          int _value2379;
          int _address2379;
          contend2x1((PC + 3) & 0xFFFF);
          _address2379 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4938 = read(_address2379, 0);
          contend1x1(_address2379);
          _value2379 = operand_4938;
          int value1_4939 = _value2379;
          value1_4939 = (value1_4939 << 1 | value1_4939 >> 7) & 0xff;
          _F2380 = (value1_4939 & 1) | (SZ53P[value1_4939] | (value1_4939 == 0 ? 0x40 : 0));
          F = (_F2380 & 0xFF);
          _address2379 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2379 = value1_4939;
          write(_address2379, value1_4939);
          int read_4943;
          read_4943 = _value2379;
          D = read_4943;
          MEMPTR = _address2379;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x03: {
          int _F2383;
          int _value2382;
          int _address2382;
          contend2x1((PC + 3) & 0xFFFF);
          _address2382 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4944 = read(_address2382, 0);
          contend1x1(_address2382);
          _value2382 = operand_4944;
          int value1_4945 = _value2382;
          value1_4945 = (value1_4945 << 1 | value1_4945 >> 7) & 0xff;
          _F2383 = (value1_4945 & 1) | (SZ53P[value1_4945] | (value1_4945 == 0 ? 0x40 : 0));
          F = (_F2383 & 0xFF);
          _address2382 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2382 = value1_4945;
          write(_address2382, value1_4945);
          int read_4949;
          read_4949 = _value2382;
          E = read_4949;
          MEMPTR = _address2382;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F2386;
          int _value2385;
          int _address2385;
          contend2x1((PC + 3) & 0xFFFF);
          _address2385 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4950 = read(_address2385, 0);
          contend1x1(_address2385);
          _value2385 = operand_4950;
          int value1_4951 = _value2385;
          value1_4951 = (value1_4951 << 1 | value1_4951 >> 7) & 0xff;
          _F2386 = (value1_4951 & 1) | (SZ53P[value1_4951] | (value1_4951 == 0 ? 0x40 : 0));
          F = (_F2386 & 0xFF);
          _address2385 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2385 = value1_4951;
          write(_address2385, value1_4951);
          int read_4955;
          read_4955 = _value2385;
          H = read_4955;
          MEMPTR = _address2385;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F2389;
          int _value2388;
          int _address2388;
          contend2x1((PC + 3) & 0xFFFF);
          _address2388 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4956 = read(_address2388, 0);
          contend1x1(_address2388);
          _value2388 = operand_4956;
          int value1_4957 = _value2388;
          value1_4957 = (value1_4957 << 1 | value1_4957 >> 7) & 0xff;
          _F2389 = (value1_4957 & 1) | (SZ53P[value1_4957] | (value1_4957 == 0 ? 0x40 : 0));
          F = (_F2389 & 0xFF);
          _address2388 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2388 = value1_4957;
          write(_address2388, value1_4957);
          int read_4961;
          read_4961 = _value2388;
          L = read_4961;
          MEMPTR = _address2388;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x06: {
          int _F2392;
          int _value2391;
          int _address2391;
          contend2x1((PC + 3) & 0xFFFF);
          _address2391 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4962 = read(_address2391, 0);
          contend1x1(_address2391);
          _value2391 = operand_4962;
          int value1_4963 = _value2391;
          value1_4963 = (value1_4963 << 1 | value1_4963 >> 7) & 0xff;
          _F2392 = (value1_4963 & 1) | (SZ53P[value1_4963] | (value1_4963 == 0 ? 0x40 : 0));
          F = (_F2392 & 0xFF);
          _address2391 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2391 = value1_4963;
          write(_address2391, value1_4963);
          MEMPTR = _address2391;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F2395;
          int _value2394;
          int _address2394;
          contend2x1((PC + 3) & 0xFFFF);
          _address2394 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4967 = read(_address2394, 0);
          contend1x1(_address2394);
          _value2394 = operand_4967;
          int value1_4968 = _value2394;
          value1_4968 = (value1_4968 << 1 | value1_4968 >> 7) & 0xff;
          _F2395 = (value1_4968 & 1) | (SZ53P[value1_4968] | (value1_4968 == 0 ? 0x40 : 0));
          F = (_F2395 & 0xFF);
          _address2394 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2394 = value1_4968;
          write(_address2394, value1_4968);
          int read_4972;
          read_4972 = _value2394;
          A = read_4972;
          MEMPTR = _address2394;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_1(int opcode, int displacement) {
    switch (opcode) {
      case 0x08: {
          int _F2398;
          int _value2397;
          int _address2397;
          contend2x1((PC + 3) & 0xFFFF);
          _address2397 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4973 = read(_address2397, 0);
          contend1x1(_address2397);
          _value2397 = operand_4973;
          int value1_4974 = _value2397;
          _F2398 = value1_4974 & 1;
          value1_4974 = (value1_4974 >> 1) | (value1_4974 << 7);
          value1_4974 &= 0xff;
          _F2398 |= (SZ53P[value1_4974 & 0xff] | (value1_4974 == 0 ? 0x40 : 0));
          int result_4977 = value1_4974 & 0xFF;
          F = (_F2398 & 0xFF);
          _address2397 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2397 = result_4977;
          write(_address2397, result_4977);
          int read_4978;
          read_4978 = _value2397;
          B = read_4978;
          MEMPTR = _address2397;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F2401;
          int _value2400;
          int _address2400;
          contend2x1((PC + 3) & 0xFFFF);
          _address2400 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4979 = read(_address2400, 0);
          contend1x1(_address2400);
          _value2400 = operand_4979;
          int value1_4980 = _value2400;
          _F2401 = value1_4980 & 1;
          value1_4980 = (value1_4980 >> 1) | (value1_4980 << 7);
          value1_4980 &= 0xff;
          _F2401 |= (SZ53P[value1_4980 & 0xff] | (value1_4980 == 0 ? 0x40 : 0));
          int result_4983 = value1_4980 & 0xFF;
          F = (_F2401 & 0xFF);
          _address2400 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2400 = result_4983;
          write(_address2400, result_4983);
          int read_4984;
          read_4984 = _value2400;
          C = read_4984;
          MEMPTR = _address2400;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _F2404;
          int _value2403;
          int _address2403;
          contend2x1((PC + 3) & 0xFFFF);
          _address2403 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4985 = read(_address2403, 0);
          contend1x1(_address2403);
          _value2403 = operand_4985;
          int value1_4986 = _value2403;
          _F2404 = value1_4986 & 1;
          value1_4986 = (value1_4986 >> 1) | (value1_4986 << 7);
          value1_4986 &= 0xff;
          _F2404 |= (SZ53P[value1_4986 & 0xff] | (value1_4986 == 0 ? 0x40 : 0));
          int result_4989 = value1_4986 & 0xFF;
          F = (_F2404 & 0xFF);
          _address2403 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2403 = result_4989;
          write(_address2403, result_4989);
          int read_4990;
          read_4990 = _value2403;
          D = read_4990;
          MEMPTR = _address2403;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int _F2407;
          int _value2406;
          int _address2406;
          contend2x1((PC + 3) & 0xFFFF);
          _address2406 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4991 = read(_address2406, 0);
          contend1x1(_address2406);
          _value2406 = operand_4991;
          int value1_4992 = _value2406;
          _F2407 = value1_4992 & 1;
          value1_4992 = (value1_4992 >> 1) | (value1_4992 << 7);
          value1_4992 &= 0xff;
          _F2407 |= (SZ53P[value1_4992 & 0xff] | (value1_4992 == 0 ? 0x40 : 0));
          int result_4995 = value1_4992 & 0xFF;
          F = (_F2407 & 0xFF);
          _address2406 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2406 = result_4995;
          write(_address2406, result_4995);
          int read_4996;
          read_4996 = _value2406;
          E = read_4996;
          MEMPTR = _address2406;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F2410;
          int _value2409;
          int _address2409;
          contend2x1((PC + 3) & 0xFFFF);
          _address2409 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_4997 = read(_address2409, 0);
          contend1x1(_address2409);
          _value2409 = operand_4997;
          int value1_4998 = _value2409;
          _F2410 = value1_4998 & 1;
          value1_4998 = (value1_4998 >> 1) | (value1_4998 << 7);
          value1_4998 &= 0xff;
          _F2410 |= (SZ53P[value1_4998 & 0xff] | (value1_4998 == 0 ? 0x40 : 0));
          int result_5001 = value1_4998 & 0xFF;
          F = (_F2410 & 0xFF);
          _address2409 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2409 = result_5001;
          write(_address2409, result_5001);
          int read_5002;
          read_5002 = _value2409;
          H = read_5002;
          MEMPTR = _address2409;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F2413;
          int _value2412;
          int _address2412;
          contend2x1((PC + 3) & 0xFFFF);
          _address2412 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5003 = read(_address2412, 0);
          contend1x1(_address2412);
          _value2412 = operand_5003;
          int value1_5004 = _value2412;
          _F2413 = value1_5004 & 1;
          value1_5004 = (value1_5004 >> 1) | (value1_5004 << 7);
          value1_5004 &= 0xff;
          _F2413 |= (SZ53P[value1_5004 & 0xff] | (value1_5004 == 0 ? 0x40 : 0));
          int result_5007 = value1_5004 & 0xFF;
          F = (_F2413 & 0xFF);
          _address2412 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2412 = result_5007;
          write(_address2412, result_5007);
          int read_5008;
          read_5008 = _value2412;
          L = read_5008;
          MEMPTR = _address2412;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int _F2416;
          int _value2415;
          int _address2415;
          contend2x1((PC + 3) & 0xFFFF);
          _address2415 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5009 = read(_address2415, 0);
          contend1x1(_address2415);
          _value2415 = operand_5009;
          int value1_5010 = _value2415;
          _F2416 = value1_5010 & 1;
          value1_5010 = (value1_5010 >> 1) | (value1_5010 << 7);
          value1_5010 &= 0xff;
          _F2416 |= (SZ53P[value1_5010 & 0xff] | (value1_5010 == 0 ? 0x40 : 0));
          int result_5013 = value1_5010 & 0xFF;
          F = (_F2416 & 0xFF);
          _address2415 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2415 = result_5013;
          write(_address2415, result_5013);
          MEMPTR = _address2415;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F2419;
          int _value2418;
          int _address2418;
          contend2x1((PC + 3) & 0xFFFF);
          _address2418 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5014 = read(_address2418, 0);
          contend1x1(_address2418);
          _value2418 = operand_5014;
          int value1_5015 = _value2418;
          _F2419 = value1_5015 & 1;
          value1_5015 = (value1_5015 >> 1) | (value1_5015 << 7);
          value1_5015 &= 0xff;
          _F2419 |= (SZ53P[value1_5015 & 0xff] | (value1_5015 == 0 ? 0x40 : 0));
          int result_5018 = value1_5015 & 0xFF;
          F = (_F2419 & 0xFF);
          _address2418 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2418 = result_5018;
          write(_address2418, result_5018);
          int read_5019;
          read_5019 = _value2418;
          A = read_5019;
          MEMPTR = _address2418;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_2(int opcode, int displacement) {
    switch (opcode) {
      case 0x10: {
          int _F2422;
          int _value2421;
          int _address2421;
          contend2x1((PC + 3) & 0xFFFF);
          _address2421 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5020 = read(_address2421, 0);
          contend1x1(_address2421);
          _value2421 = operand_5020;
          int value1_5021 = _value2421;
          int value2_5022 = F;
          _F2422 = value2_5022;
          int rltemp_5024 = value1_5021;
          value1_5021 = (value1_5021 << 1) | (_F2422 & 1);
          value1_5021 &= 0xff;
          _F2422 = (rltemp_5024 >> 7) | (SZ53P[value1_5021 & 0xff] | (value1_5021 == 0 ? 0x40 : 0));
          int result_5025 = value1_5021 & 0xFF;
          F = (_F2422 & 0xFF);
          _address2421 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2421 = result_5025;
          write(_address2421, result_5025);
          int read_5026;
          read_5026 = _value2421;
          B = read_5026;
          MEMPTR = _address2421;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x11: {
          int _F2425;
          int _value2424;
          int _address2424;
          contend2x1((PC + 3) & 0xFFFF);
          _address2424 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5027 = read(_address2424, 0);
          contend1x1(_address2424);
          _value2424 = operand_5027;
          int value1_5028 = _value2424;
          int value2_5029 = F;
          _F2425 = value2_5029;
          int rltemp_5031 = value1_5028;
          value1_5028 = (value1_5028 << 1) | (_F2425 & 1);
          value1_5028 &= 0xff;
          _F2425 = (rltemp_5031 >> 7) | (SZ53P[value1_5028 & 0xff] | (value1_5028 == 0 ? 0x40 : 0));
          int result_5032 = value1_5028 & 0xFF;
          F = (_F2425 & 0xFF);
          _address2424 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2424 = result_5032;
          write(_address2424, result_5032);
          int read_5033;
          read_5033 = _value2424;
          C = read_5033;
          MEMPTR = _address2424;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x12: {
          int _F2428;
          int _value2427;
          int _address2427;
          contend2x1((PC + 3) & 0xFFFF);
          _address2427 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5034 = read(_address2427, 0);
          contend1x1(_address2427);
          _value2427 = operand_5034;
          int value1_5035 = _value2427;
          int value2_5036 = F;
          _F2428 = value2_5036;
          int rltemp_5038 = value1_5035;
          value1_5035 = (value1_5035 << 1) | (_F2428 & 1);
          value1_5035 &= 0xff;
          _F2428 = (rltemp_5038 >> 7) | (SZ53P[value1_5035 & 0xff] | (value1_5035 == 0 ? 0x40 : 0));
          int result_5039 = value1_5035 & 0xFF;
          F = (_F2428 & 0xFF);
          _address2427 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2427 = result_5039;
          write(_address2427, result_5039);
          int read_5040;
          read_5040 = _value2427;
          D = read_5040;
          MEMPTR = _address2427;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x13: {
          int _F2431;
          int _value2430;
          int _address2430;
          contend2x1((PC + 3) & 0xFFFF);
          _address2430 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5041 = read(_address2430, 0);
          contend1x1(_address2430);
          _value2430 = operand_5041;
          int value1_5042 = _value2430;
          int value2_5043 = F;
          _F2431 = value2_5043;
          int rltemp_5045 = value1_5042;
          value1_5042 = (value1_5042 << 1) | (_F2431 & 1);
          value1_5042 &= 0xff;
          _F2431 = (rltemp_5045 >> 7) | (SZ53P[value1_5042 & 0xff] | (value1_5042 == 0 ? 0x40 : 0));
          int result_5046 = value1_5042 & 0xFF;
          F = (_F2431 & 0xFF);
          _address2430 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2430 = result_5046;
          write(_address2430, result_5046);
          int read_5047;
          read_5047 = _value2430;
          E = read_5047;
          MEMPTR = _address2430;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F2434;
          int _value2433;
          int _address2433;
          contend2x1((PC + 3) & 0xFFFF);
          _address2433 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5048 = read(_address2433, 0);
          contend1x1(_address2433);
          _value2433 = operand_5048;
          int value1_5049 = _value2433;
          int value2_5050 = F;
          _F2434 = value2_5050;
          int rltemp_5052 = value1_5049;
          value1_5049 = (value1_5049 << 1) | (_F2434 & 1);
          value1_5049 &= 0xff;
          _F2434 = (rltemp_5052 >> 7) | (SZ53P[value1_5049 & 0xff] | (value1_5049 == 0 ? 0x40 : 0));
          int result_5053 = value1_5049 & 0xFF;
          F = (_F2434 & 0xFF);
          _address2433 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2433 = result_5053;
          write(_address2433, result_5053);
          int read_5054;
          read_5054 = _value2433;
          H = read_5054;
          MEMPTR = _address2433;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F2437;
          int _value2436;
          int _address2436;
          contend2x1((PC + 3) & 0xFFFF);
          _address2436 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5055 = read(_address2436, 0);
          contend1x1(_address2436);
          _value2436 = operand_5055;
          int value1_5056 = _value2436;
          int value2_5057 = F;
          _F2437 = value2_5057;
          int rltemp_5059 = value1_5056;
          value1_5056 = (value1_5056 << 1) | (_F2437 & 1);
          value1_5056 &= 0xff;
          _F2437 = (rltemp_5059 >> 7) | (SZ53P[value1_5056 & 0xff] | (value1_5056 == 0 ? 0x40 : 0));
          int result_5060 = value1_5056 & 0xFF;
          F = (_F2437 & 0xFF);
          _address2436 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2436 = result_5060;
          write(_address2436, result_5060);
          int read_5061;
          read_5061 = _value2436;
          L = read_5061;
          MEMPTR = _address2436;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x16: {
          int _F2440;
          int _value2439;
          int _address2439;
          contend2x1((PC + 3) & 0xFFFF);
          _address2439 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5062 = read(_address2439, 0);
          contend1x1(_address2439);
          _value2439 = operand_5062;
          int value1_5063 = _value2439;
          int value2_5064 = F;
          _F2440 = value2_5064;
          int rltemp_5066 = value1_5063;
          value1_5063 = (value1_5063 << 1) | (_F2440 & 1);
          value1_5063 &= 0xff;
          _F2440 = (rltemp_5066 >> 7) | (SZ53P[value1_5063 & 0xff] | (value1_5063 == 0 ? 0x40 : 0));
          int result_5067 = value1_5063 & 0xFF;
          F = (_F2440 & 0xFF);
          _address2439 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2439 = result_5067;
          write(_address2439, result_5067);
          MEMPTR = _address2439;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F2443;
          int _value2442;
          int _address2442;
          contend2x1((PC + 3) & 0xFFFF);
          _address2442 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5068 = read(_address2442, 0);
          contend1x1(_address2442);
          _value2442 = operand_5068;
          int value1_5069 = _value2442;
          int value2_5070 = F;
          _F2443 = value2_5070;
          int rltemp_5072 = value1_5069;
          value1_5069 = (value1_5069 << 1) | (_F2443 & 1);
          value1_5069 &= 0xff;
          _F2443 = (rltemp_5072 >> 7) | (SZ53P[value1_5069 & 0xff] | (value1_5069 == 0 ? 0x40 : 0));
          int result_5073 = value1_5069 & 0xFF;
          F = (_F2443 & 0xFF);
          _address2442 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2442 = result_5073;
          write(_address2442, result_5073);
          int read_5074;
          read_5074 = _value2442;
          A = read_5074;
          MEMPTR = _address2442;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_3(int opcode, int displacement) {
    switch (opcode) {
      case 0x18: {
          int _F2446;
          int _value2445;
          int _address2445;
          contend2x1((PC + 3) & 0xFFFF);
          _address2445 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5075 = read(_address2445, 0);
          contend1x1(_address2445);
          _value2445 = operand_5075;
          int value1_5076 = _value2445;
          int value2_5077 = F;
          _F2446 = value2_5077;
          int rrtemp_5079 = value1_5076;
          value1_5076 = (value1_5076 >> 1) | (_F2446 << 7);
          value1_5076 &= 0xff;
          _F2446 = (rrtemp_5079 & 1) | (SZ53P[value1_5076 & 0xff] | (value1_5076 == 0 ? 0x40 : 0));
          int result_5080 = value1_5076 & 0xFF;
          F = (_F2446 & 0xFF);
          _address2445 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2445 = result_5080;
          write(_address2445, result_5080);
          int read_5081;
          read_5081 = _value2445;
          B = read_5081;
          MEMPTR = _address2445;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x19: {
          int _F2449;
          int _value2448;
          int _address2448;
          contend2x1((PC + 3) & 0xFFFF);
          _address2448 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5082 = read(_address2448, 0);
          contend1x1(_address2448);
          _value2448 = operand_5082;
          int value1_5083 = _value2448;
          int value2_5084 = F;
          _F2449 = value2_5084;
          int rrtemp_5086 = value1_5083;
          value1_5083 = (value1_5083 >> 1) | (_F2449 << 7);
          value1_5083 &= 0xff;
          _F2449 = (rrtemp_5086 & 1) | (SZ53P[value1_5083 & 0xff] | (value1_5083 == 0 ? 0x40 : 0));
          int result_5087 = value1_5083 & 0xFF;
          F = (_F2449 & 0xFF);
          _address2448 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2448 = result_5087;
          write(_address2448, result_5087);
          int read_5088;
          read_5088 = _value2448;
          C = read_5088;
          MEMPTR = _address2448;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _F2452;
          int _value2451;
          int _address2451;
          contend2x1((PC + 3) & 0xFFFF);
          _address2451 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5089 = read(_address2451, 0);
          contend1x1(_address2451);
          _value2451 = operand_5089;
          int value1_5090 = _value2451;
          int value2_5091 = F;
          _F2452 = value2_5091;
          int rrtemp_5093 = value1_5090;
          value1_5090 = (value1_5090 >> 1) | (_F2452 << 7);
          value1_5090 &= 0xff;
          _F2452 = (rrtemp_5093 & 1) | (SZ53P[value1_5090 & 0xff] | (value1_5090 == 0 ? 0x40 : 0));
          int result_5094 = value1_5090 & 0xFF;
          F = (_F2452 & 0xFF);
          _address2451 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2451 = result_5094;
          write(_address2451, result_5094);
          int read_5095;
          read_5095 = _value2451;
          D = read_5095;
          MEMPTR = _address2451;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int _F2455;
          int _value2454;
          int _address2454;
          contend2x1((PC + 3) & 0xFFFF);
          _address2454 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5096 = read(_address2454, 0);
          contend1x1(_address2454);
          _value2454 = operand_5096;
          int value1_5097 = _value2454;
          int value2_5098 = F;
          _F2455 = value2_5098;
          int rrtemp_5100 = value1_5097;
          value1_5097 = (value1_5097 >> 1) | (_F2455 << 7);
          value1_5097 &= 0xff;
          _F2455 = (rrtemp_5100 & 1) | (SZ53P[value1_5097 & 0xff] | (value1_5097 == 0 ? 0x40 : 0));
          int result_5101 = value1_5097 & 0xFF;
          F = (_F2455 & 0xFF);
          _address2454 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2454 = result_5101;
          write(_address2454, result_5101);
          int read_5102;
          read_5102 = _value2454;
          E = read_5102;
          MEMPTR = _address2454;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F2458;
          int _value2457;
          int _address2457;
          contend2x1((PC + 3) & 0xFFFF);
          _address2457 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5103 = read(_address2457, 0);
          contend1x1(_address2457);
          _value2457 = operand_5103;
          int value1_5104 = _value2457;
          int value2_5105 = F;
          _F2458 = value2_5105;
          int rrtemp_5107 = value1_5104;
          value1_5104 = (value1_5104 >> 1) | (_F2458 << 7);
          value1_5104 &= 0xff;
          _F2458 = (rrtemp_5107 & 1) | (SZ53P[value1_5104 & 0xff] | (value1_5104 == 0 ? 0x40 : 0));
          int result_5108 = value1_5104 & 0xFF;
          F = (_F2458 & 0xFF);
          _address2457 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2457 = result_5108;
          write(_address2457, result_5108);
          int read_5109;
          read_5109 = _value2457;
          H = read_5109;
          MEMPTR = _address2457;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F2461;
          int _value2460;
          int _address2460;
          contend2x1((PC + 3) & 0xFFFF);
          _address2460 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5110 = read(_address2460, 0);
          contend1x1(_address2460);
          _value2460 = operand_5110;
          int value1_5111 = _value2460;
          int value2_5112 = F;
          _F2461 = value2_5112;
          int rrtemp_5114 = value1_5111;
          value1_5111 = (value1_5111 >> 1) | (_F2461 << 7);
          value1_5111 &= 0xff;
          _F2461 = (rrtemp_5114 & 1) | (SZ53P[value1_5111 & 0xff] | (value1_5111 == 0 ? 0x40 : 0));
          int result_5115 = value1_5111 & 0xFF;
          F = (_F2461 & 0xFF);
          _address2460 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2460 = result_5115;
          write(_address2460, result_5115);
          int read_5116;
          read_5116 = _value2460;
          L = read_5116;
          MEMPTR = _address2460;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int _F2464;
          int _value2463;
          int _address2463;
          contend2x1((PC + 3) & 0xFFFF);
          _address2463 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5117 = read(_address2463, 0);
          contend1x1(_address2463);
          _value2463 = operand_5117;
          int value1_5118 = _value2463;
          int value2_5119 = F;
          _F2464 = value2_5119;
          int rrtemp_5121 = value1_5118;
          value1_5118 = (value1_5118 >> 1) | (_F2464 << 7);
          value1_5118 &= 0xff;
          _F2464 = (rrtemp_5121 & 1) | (SZ53P[value1_5118 & 0xff] | (value1_5118 == 0 ? 0x40 : 0));
          int result_5122 = value1_5118 & 0xFF;
          F = (_F2464 & 0xFF);
          _address2463 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2463 = result_5122;
          write(_address2463, result_5122);
          MEMPTR = _address2463;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F2467;
          int _value2466;
          int _address2466;
          contend2x1((PC + 3) & 0xFFFF);
          _address2466 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5123 = read(_address2466, 0);
          contend1x1(_address2466);
          _value2466 = operand_5123;
          int value1_5124 = _value2466;
          int value2_5125 = F;
          _F2467 = value2_5125;
          int rrtemp_5127 = value1_5124;
          value1_5124 = (value1_5124 >> 1) | (_F2467 << 7);
          value1_5124 &= 0xff;
          _F2467 = (rrtemp_5127 & 1) | (SZ53P[value1_5124 & 0xff] | (value1_5124 == 0 ? 0x40 : 0));
          int result_5128 = value1_5124 & 0xFF;
          F = (_F2467 & 0xFF);
          _address2466 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2466 = result_5128;
          write(_address2466, result_5128);
          int read_5129;
          read_5129 = _value2466;
          A = read_5129;
          MEMPTR = _address2466;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_4(int opcode, int displacement) {
    switch (opcode) {
      case 0x20: {
          int _F2470;
          int _value2469;
          int _address2469;
          contend2x1((PC + 3) & 0xFFFF);
          _address2469 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5130 = read(_address2469, 0);
          contend1x1(_address2469);
          _value2469 = operand_5130;
          int value1_5131 = _value2469;
          _F2470 = value1_5131 >> 7;
          value1_5131 <<= 1;
          value1_5131 &= 0xff;
          _F2470 |= (SZ53P[value1_5131 & 0xff] | (value1_5131 == 0 ? 0x40 : 0));
          int result_5134 = value1_5131 & 0xFF;
          F = (_F2470 & 0xFF);
          _address2469 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2469 = result_5134;
          write(_address2469, result_5134);
          int read_5135;
          read_5135 = _value2469;
          B = read_5135;
          MEMPTR = _address2469;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x21: {
          int _F2473;
          int _value2472;
          int _address2472;
          contend2x1((PC + 3) & 0xFFFF);
          _address2472 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5136 = read(_address2472, 0);
          contend1x1(_address2472);
          _value2472 = operand_5136;
          int value1_5137 = _value2472;
          _F2473 = value1_5137 >> 7;
          value1_5137 <<= 1;
          value1_5137 &= 0xff;
          _F2473 |= (SZ53P[value1_5137 & 0xff] | (value1_5137 == 0 ? 0x40 : 0));
          int result_5140 = value1_5137 & 0xFF;
          F = (_F2473 & 0xFF);
          _address2472 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2472 = result_5140;
          write(_address2472, result_5140);
          int read_5141;
          read_5141 = _value2472;
          C = read_5141;
          MEMPTR = _address2472;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _F2476;
          int _value2475;
          int _address2475;
          contend2x1((PC + 3) & 0xFFFF);
          _address2475 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5142 = read(_address2475, 0);
          contend1x1(_address2475);
          _value2475 = operand_5142;
          int value1_5143 = _value2475;
          _F2476 = value1_5143 >> 7;
          value1_5143 <<= 1;
          value1_5143 &= 0xff;
          _F2476 |= (SZ53P[value1_5143 & 0xff] | (value1_5143 == 0 ? 0x40 : 0));
          int result_5146 = value1_5143 & 0xFF;
          F = (_F2476 & 0xFF);
          _address2475 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2475 = result_5146;
          write(_address2475, result_5146);
          int read_5147;
          read_5147 = _value2475;
          D = read_5147;
          MEMPTR = _address2475;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int _F2479;
          int _value2478;
          int _address2478;
          contend2x1((PC + 3) & 0xFFFF);
          _address2478 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5148 = read(_address2478, 0);
          contend1x1(_address2478);
          _value2478 = operand_5148;
          int value1_5149 = _value2478;
          _F2479 = value1_5149 >> 7;
          value1_5149 <<= 1;
          value1_5149 &= 0xff;
          _F2479 |= (SZ53P[value1_5149 & 0xff] | (value1_5149 == 0 ? 0x40 : 0));
          int result_5152 = value1_5149 & 0xFF;
          F = (_F2479 & 0xFF);
          _address2478 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2478 = result_5152;
          write(_address2478, result_5152);
          int read_5153;
          read_5153 = _value2478;
          E = read_5153;
          MEMPTR = _address2478;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F2482;
          int _value2481;
          int _address2481;
          contend2x1((PC + 3) & 0xFFFF);
          _address2481 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5154 = read(_address2481, 0);
          contend1x1(_address2481);
          _value2481 = operand_5154;
          int value1_5155 = _value2481;
          _F2482 = value1_5155 >> 7;
          value1_5155 <<= 1;
          value1_5155 &= 0xff;
          _F2482 |= (SZ53P[value1_5155 & 0xff] | (value1_5155 == 0 ? 0x40 : 0));
          int result_5158 = value1_5155 & 0xFF;
          F = (_F2482 & 0xFF);
          _address2481 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2481 = result_5158;
          write(_address2481, result_5158);
          int read_5159;
          read_5159 = _value2481;
          H = read_5159;
          MEMPTR = _address2481;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F2485;
          int _value2484;
          int _address2484;
          contend2x1((PC + 3) & 0xFFFF);
          _address2484 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5160 = read(_address2484, 0);
          contend1x1(_address2484);
          _value2484 = operand_5160;
          int value1_5161 = _value2484;
          _F2485 = value1_5161 >> 7;
          value1_5161 <<= 1;
          value1_5161 &= 0xff;
          _F2485 |= (SZ53P[value1_5161 & 0xff] | (value1_5161 == 0 ? 0x40 : 0));
          int result_5164 = value1_5161 & 0xFF;
          F = (_F2485 & 0xFF);
          _address2484 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2484 = result_5164;
          write(_address2484, result_5164);
          int read_5165;
          read_5165 = _value2484;
          L = read_5165;
          MEMPTR = _address2484;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x26: {
          int _F2488;
          int _value2487;
          int _address2487;
          contend2x1((PC + 3) & 0xFFFF);
          _address2487 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5166 = read(_address2487, 0);
          contend1x1(_address2487);
          _value2487 = operand_5166;
          int value1_5167 = _value2487;
          _F2488 = value1_5167 >> 7;
          value1_5167 <<= 1;
          value1_5167 &= 0xff;
          _F2488 |= (SZ53P[value1_5167 & 0xff] | (value1_5167 == 0 ? 0x40 : 0));
          int result_5170 = value1_5167 & 0xFF;
          F = (_F2488 & 0xFF);
          _address2487 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2487 = result_5170;
          write(_address2487, result_5170);
          MEMPTR = _address2487;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F2491;
          int _value2490;
          int _address2490;
          contend2x1((PC + 3) & 0xFFFF);
          _address2490 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5171 = read(_address2490, 0);
          contend1x1(_address2490);
          _value2490 = operand_5171;
          int value1_5172 = _value2490;
          _F2491 = value1_5172 >> 7;
          value1_5172 <<= 1;
          value1_5172 &= 0xff;
          _F2491 |= (SZ53P[value1_5172 & 0xff] | (value1_5172 == 0 ? 0x40 : 0));
          int result_5175 = value1_5172 & 0xFF;
          F = (_F2491 & 0xFF);
          _address2490 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2490 = result_5175;
          write(_address2490, result_5175);
          int read_5176;
          read_5176 = _value2490;
          A = read_5176;
          MEMPTR = _address2490;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_5(int opcode, int displacement) {
    switch (opcode) {
      case 0x28: {
          int _F2494;
          int _value2493;
          int _address2493;
          contend2x1((PC + 3) & 0xFFFF);
          _address2493 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5177 = read(_address2493, 0);
          contend1x1(_address2493);
          _value2493 = operand_5177;
          int value1_5178 = _value2493;
          _F2494 = value1_5178 & 1;
          value1_5178 = (value1_5178 & 0x80) | (value1_5178 >> 1);
          value1_5178 &= 0xff;
          _F2494 |= (SZ53P[value1_5178 & 0xff] | (value1_5178 == 0 ? 0x40 : 0));
          int result_5181 = value1_5178 & 0xFF;
          F = (_F2494 & 0xFF);
          _address2493 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2493 = result_5181;
          write(_address2493, result_5181);
          int read_5182;
          read_5182 = _value2493;
          B = read_5182;
          MEMPTR = _address2493;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x29: {
          int _F2497;
          int _value2496;
          int _address2496;
          contend2x1((PC + 3) & 0xFFFF);
          _address2496 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5183 = read(_address2496, 0);
          contend1x1(_address2496);
          _value2496 = operand_5183;
          int value1_5184 = _value2496;
          _F2497 = value1_5184 & 1;
          value1_5184 = (value1_5184 & 0x80) | (value1_5184 >> 1);
          value1_5184 &= 0xff;
          _F2497 |= (SZ53P[value1_5184 & 0xff] | (value1_5184 == 0 ? 0x40 : 0));
          int result_5187 = value1_5184 & 0xFF;
          F = (_F2497 & 0xFF);
          _address2496 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2496 = result_5187;
          write(_address2496, result_5187);
          int read_5188;
          read_5188 = _value2496;
          C = read_5188;
          MEMPTR = _address2496;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _F2500;
          int _value2499;
          int _address2499;
          contend2x1((PC + 3) & 0xFFFF);
          _address2499 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5189 = read(_address2499, 0);
          contend1x1(_address2499);
          _value2499 = operand_5189;
          int value1_5190 = _value2499;
          _F2500 = value1_5190 & 1;
          value1_5190 = (value1_5190 & 0x80) | (value1_5190 >> 1);
          value1_5190 &= 0xff;
          _F2500 |= (SZ53P[value1_5190 & 0xff] | (value1_5190 == 0 ? 0x40 : 0));
          int result_5193 = value1_5190 & 0xFF;
          F = (_F2500 & 0xFF);
          _address2499 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2499 = result_5193;
          write(_address2499, result_5193);
          int read_5194;
          read_5194 = _value2499;
          D = read_5194;
          MEMPTR = _address2499;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2B: {
          int _F2503;
          int _value2502;
          int _address2502;
          contend2x1((PC + 3) & 0xFFFF);
          _address2502 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5195 = read(_address2502, 0);
          contend1x1(_address2502);
          _value2502 = operand_5195;
          int value1_5196 = _value2502;
          _F2503 = value1_5196 & 1;
          value1_5196 = (value1_5196 & 0x80) | (value1_5196 >> 1);
          value1_5196 &= 0xff;
          _F2503 |= (SZ53P[value1_5196 & 0xff] | (value1_5196 == 0 ? 0x40 : 0));
          int result_5199 = value1_5196 & 0xFF;
          F = (_F2503 & 0xFF);
          _address2502 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2502 = result_5199;
          write(_address2502, result_5199);
          int read_5200;
          read_5200 = _value2502;
          E = read_5200;
          MEMPTR = _address2502;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2C: {
          int _F2506;
          int _value2505;
          int _address2505;
          contend2x1((PC + 3) & 0xFFFF);
          _address2505 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5201 = read(_address2505, 0);
          contend1x1(_address2505);
          _value2505 = operand_5201;
          int value1_5202 = _value2505;
          _F2506 = value1_5202 & 1;
          value1_5202 = (value1_5202 & 0x80) | (value1_5202 >> 1);
          value1_5202 &= 0xff;
          _F2506 |= (SZ53P[value1_5202 & 0xff] | (value1_5202 == 0 ? 0x40 : 0));
          int result_5205 = value1_5202 & 0xFF;
          F = (_F2506 & 0xFF);
          _address2505 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2505 = result_5205;
          write(_address2505, result_5205);
          int read_5206;
          read_5206 = _value2505;
          H = read_5206;
          MEMPTR = _address2505;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F2509;
          int _value2508;
          int _address2508;
          contend2x1((PC + 3) & 0xFFFF);
          _address2508 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5207 = read(_address2508, 0);
          contend1x1(_address2508);
          _value2508 = operand_5207;
          int value1_5208 = _value2508;
          _F2509 = value1_5208 & 1;
          value1_5208 = (value1_5208 & 0x80) | (value1_5208 >> 1);
          value1_5208 &= 0xff;
          _F2509 |= (SZ53P[value1_5208 & 0xff] | (value1_5208 == 0 ? 0x40 : 0));
          int result_5211 = value1_5208 & 0xFF;
          F = (_F2509 & 0xFF);
          _address2508 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2508 = result_5211;
          write(_address2508, result_5211);
          int read_5212;
          read_5212 = _value2508;
          L = read_5212;
          MEMPTR = _address2508;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int _F2512;
          int _value2511;
          int _address2511;
          contend2x1((PC + 3) & 0xFFFF);
          _address2511 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5213 = read(_address2511, 0);
          contend1x1(_address2511);
          _value2511 = operand_5213;
          int value1_5214 = _value2511;
          _F2512 = value1_5214 & 1;
          value1_5214 = (value1_5214 & 0x80) | (value1_5214 >> 1);
          value1_5214 &= 0xff;
          _F2512 |= (SZ53P[value1_5214 & 0xff] | (value1_5214 == 0 ? 0x40 : 0));
          int result_5217 = value1_5214 & 0xFF;
          F = (_F2512 & 0xFF);
          _address2511 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2511 = result_5217;
          write(_address2511, result_5217);
          MEMPTR = _address2511;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F2515;
          int _value2514;
          int _address2514;
          contend2x1((PC + 3) & 0xFFFF);
          _address2514 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5218 = read(_address2514, 0);
          contend1x1(_address2514);
          _value2514 = operand_5218;
          int value1_5219 = _value2514;
          _F2515 = value1_5219 & 1;
          value1_5219 = (value1_5219 & 0x80) | (value1_5219 >> 1);
          value1_5219 &= 0xff;
          _F2515 |= (SZ53P[value1_5219 & 0xff] | (value1_5219 == 0 ? 0x40 : 0));
          int result_5222 = value1_5219 & 0xFF;
          F = (_F2515 & 0xFF);
          _address2514 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2514 = result_5222;
          write(_address2514, result_5222);
          int read_5223;
          read_5223 = _value2514;
          A = read_5223;
          MEMPTR = _address2514;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_6(int opcode, int displacement) {
    switch (opcode) {
      case 0x30: {
          int _F2518;
          int _value2517;
          int _address2517;
          contend2x1((PC + 3) & 0xFFFF);
          _address2517 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5224 = read(_address2517, 0);
          contend1x1(_address2517);
          _value2517 = operand_5224;
          int value1_5225 = _value2517;
          _F2518 = value1_5225 >> 7;
          value1_5225 = (value1_5225 << 1) | 0x01;
          value1_5225 &= 0xff;
          _F2518 |= (SZ53P[value1_5225 & 0xff] | (value1_5225 == 0 ? 0x40 : 0));
          int result_5228 = value1_5225 & 0xFF;
          F = (_F2518 & 0xFF);
          _address2517 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2517 = result_5228;
          write(_address2517, result_5228);
          int read_5229;
          read_5229 = _value2517;
          B = read_5229;
          MEMPTR = _address2517;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x31: {
          int _F2521;
          int _value2520;
          int _address2520;
          contend2x1((PC + 3) & 0xFFFF);
          _address2520 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5230 = read(_address2520, 0);
          contend1x1(_address2520);
          _value2520 = operand_5230;
          int value1_5231 = _value2520;
          _F2521 = value1_5231 >> 7;
          value1_5231 = (value1_5231 << 1) | 0x01;
          value1_5231 &= 0xff;
          _F2521 |= (SZ53P[value1_5231 & 0xff] | (value1_5231 == 0 ? 0x40 : 0));
          int result_5234 = value1_5231 & 0xFF;
          F = (_F2521 & 0xFF);
          _address2520 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2520 = result_5234;
          write(_address2520, result_5234);
          int read_5235;
          read_5235 = _value2520;
          C = read_5235;
          MEMPTR = _address2520;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _F2524;
          int _value2523;
          int _address2523;
          contend2x1((PC + 3) & 0xFFFF);
          _address2523 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5236 = read(_address2523, 0);
          contend1x1(_address2523);
          _value2523 = operand_5236;
          int value1_5237 = _value2523;
          _F2524 = value1_5237 >> 7;
          value1_5237 = (value1_5237 << 1) | 0x01;
          value1_5237 &= 0xff;
          _F2524 |= (SZ53P[value1_5237 & 0xff] | (value1_5237 == 0 ? 0x40 : 0));
          int result_5240 = value1_5237 & 0xFF;
          F = (_F2524 & 0xFF);
          _address2523 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2523 = result_5240;
          write(_address2523, result_5240);
          int read_5241;
          read_5241 = _value2523;
          D = read_5241;
          MEMPTR = _address2523;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int _F2527;
          int _value2526;
          int _address2526;
          contend2x1((PC + 3) & 0xFFFF);
          _address2526 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5242 = read(_address2526, 0);
          contend1x1(_address2526);
          _value2526 = operand_5242;
          int value1_5243 = _value2526;
          _F2527 = value1_5243 >> 7;
          value1_5243 = (value1_5243 << 1) | 0x01;
          value1_5243 &= 0xff;
          _F2527 |= (SZ53P[value1_5243 & 0xff] | (value1_5243 == 0 ? 0x40 : 0));
          int result_5246 = value1_5243 & 0xFF;
          F = (_F2527 & 0xFF);
          _address2526 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2526 = result_5246;
          write(_address2526, result_5246);
          int read_5247;
          read_5247 = _value2526;
          E = read_5247;
          MEMPTR = _address2526;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F2530;
          int _value2529;
          int _address2529;
          contend2x1((PC + 3) & 0xFFFF);
          _address2529 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5248 = read(_address2529, 0);
          contend1x1(_address2529);
          _value2529 = operand_5248;
          int value1_5249 = _value2529;
          _F2530 = value1_5249 >> 7;
          value1_5249 = (value1_5249 << 1) | 0x01;
          value1_5249 &= 0xff;
          _F2530 |= (SZ53P[value1_5249 & 0xff] | (value1_5249 == 0 ? 0x40 : 0));
          int result_5252 = value1_5249 & 0xFF;
          F = (_F2530 & 0xFF);
          _address2529 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2529 = result_5252;
          write(_address2529, result_5252);
          int read_5253;
          read_5253 = _value2529;
          H = read_5253;
          MEMPTR = _address2529;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F2533;
          int _value2532;
          int _address2532;
          contend2x1((PC + 3) & 0xFFFF);
          _address2532 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5254 = read(_address2532, 0);
          contend1x1(_address2532);
          _value2532 = operand_5254;
          int value1_5255 = _value2532;
          _F2533 = value1_5255 >> 7;
          value1_5255 = (value1_5255 << 1) | 0x01;
          value1_5255 &= 0xff;
          _F2533 |= (SZ53P[value1_5255 & 0xff] | (value1_5255 == 0 ? 0x40 : 0));
          int result_5258 = value1_5255 & 0xFF;
          F = (_F2533 & 0xFF);
          _address2532 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2532 = result_5258;
          write(_address2532, result_5258);
          int read_5259;
          read_5259 = _value2532;
          L = read_5259;
          MEMPTR = _address2532;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _F2536;
          int _value2535;
          int _address2535;
          contend2x1((PC + 3) & 0xFFFF);
          _address2535 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5260 = read(_address2535, 0);
          contend1x1(_address2535);
          _value2535 = operand_5260;
          int value1_5261 = _value2535;
          _F2536 = value1_5261 >> 7;
          value1_5261 = (value1_5261 << 1) | 0x01;
          value1_5261 &= 0xff;
          _F2536 |= (SZ53P[value1_5261 & 0xff] | (value1_5261 == 0 ? 0x40 : 0));
          int result_5264 = value1_5261 & 0xFF;
          F = (_F2536 & 0xFF);
          _address2535 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2535 = result_5264;
          write(_address2535, result_5264);
          MEMPTR = _address2535;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F2539;
          int _value2538;
          int _address2538;
          contend2x1((PC + 3) & 0xFFFF);
          _address2538 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5265 = read(_address2538, 0);
          contend1x1(_address2538);
          _value2538 = operand_5265;
          int value1_5266 = _value2538;
          _F2539 = value1_5266 >> 7;
          value1_5266 = (value1_5266 << 1) | 0x01;
          value1_5266 &= 0xff;
          _F2539 |= (SZ53P[value1_5266 & 0xff] | (value1_5266 == 0 ? 0x40 : 0));
          int result_5269 = value1_5266 & 0xFF;
          F = (_F2539 & 0xFF);
          _address2538 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2538 = result_5269;
          write(_address2538, result_5269);
          int read_5270;
          read_5270 = _value2538;
          A = read_5270;
          MEMPTR = _address2538;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_7(int opcode, int displacement) {
    switch (opcode) {
      case 0x38: {
          int _F2542;
          int _value2541;
          int _address2541;
          contend2x1((PC + 3) & 0xFFFF);
          _address2541 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5271 = read(_address2541, 0);
          contend1x1(_address2541);
          _value2541 = operand_5271;
          int value1_5272 = _value2541;
          _F2542 = value1_5272 & 1;
          value1_5272 >>= 1;
          value1_5272 &= 0xff;
          _F2542 |= (SZ53P[value1_5272 & 0xff] | (value1_5272 == 0 ? 0x40 : 0));
          int result_5275 = value1_5272 & 0xFF;
          F = (_F2542 & 0xFF);
          _address2541 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2541 = result_5275;
          write(_address2541, result_5275);
          int read_5276;
          read_5276 = _value2541;
          B = read_5276;
          MEMPTR = _address2541;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x39: {
          int _F2545;
          int _value2544;
          int _address2544;
          contend2x1((PC + 3) & 0xFFFF);
          _address2544 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5277 = read(_address2544, 0);
          contend1x1(_address2544);
          _value2544 = operand_5277;
          int value1_5278 = _value2544;
          _F2545 = value1_5278 & 1;
          value1_5278 >>= 1;
          value1_5278 &= 0xff;
          _F2545 |= (SZ53P[value1_5278 & 0xff] | (value1_5278 == 0 ? 0x40 : 0));
          int result_5281 = value1_5278 & 0xFF;
          F = (_F2545 & 0xFF);
          _address2544 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2544 = result_5281;
          write(_address2544, result_5281);
          int read_5282;
          read_5282 = _value2544;
          C = read_5282;
          MEMPTR = _address2544;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _F2548;
          int _value2547;
          int _address2547;
          contend2x1((PC + 3) & 0xFFFF);
          _address2547 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5283 = read(_address2547, 0);
          contend1x1(_address2547);
          _value2547 = operand_5283;
          int value1_5284 = _value2547;
          _F2548 = value1_5284 & 1;
          value1_5284 >>= 1;
          value1_5284 &= 0xff;
          _F2548 |= (SZ53P[value1_5284 & 0xff] | (value1_5284 == 0 ? 0x40 : 0));
          int result_5287 = value1_5284 & 0xFF;
          F = (_F2548 & 0xFF);
          _address2547 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2547 = result_5287;
          write(_address2547, result_5287);
          int read_5288;
          read_5288 = _value2547;
          D = read_5288;
          MEMPTR = _address2547;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3B: {
          int _F2551;
          int _value2550;
          int _address2550;
          contend2x1((PC + 3) & 0xFFFF);
          _address2550 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5289 = read(_address2550, 0);
          contend1x1(_address2550);
          _value2550 = operand_5289;
          int value1_5290 = _value2550;
          _F2551 = value1_5290 & 1;
          value1_5290 >>= 1;
          value1_5290 &= 0xff;
          _F2551 |= (SZ53P[value1_5290 & 0xff] | (value1_5290 == 0 ? 0x40 : 0));
          int result_5293 = value1_5290 & 0xFF;
          F = (_F2551 & 0xFF);
          _address2550 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2550 = result_5293;
          write(_address2550, result_5293);
          int read_5294;
          read_5294 = _value2550;
          E = read_5294;
          MEMPTR = _address2550;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3C: {
          int _F2554;
          int _value2553;
          int _address2553;
          contend2x1((PC + 3) & 0xFFFF);
          _address2553 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5295 = read(_address2553, 0);
          contend1x1(_address2553);
          _value2553 = operand_5295;
          int value1_5296 = _value2553;
          _F2554 = value1_5296 & 1;
          value1_5296 >>= 1;
          value1_5296 &= 0xff;
          _F2554 |= (SZ53P[value1_5296 & 0xff] | (value1_5296 == 0 ? 0x40 : 0));
          int result_5299 = value1_5296 & 0xFF;
          F = (_F2554 & 0xFF);
          _address2553 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2553 = result_5299;
          write(_address2553, result_5299);
          int read_5300;
          read_5300 = _value2553;
          H = read_5300;
          MEMPTR = _address2553;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F2557;
          int _value2556;
          int _address2556;
          contend2x1((PC + 3) & 0xFFFF);
          _address2556 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5301 = read(_address2556, 0);
          contend1x1(_address2556);
          _value2556 = operand_5301;
          int value1_5302 = _value2556;
          _F2557 = value1_5302 & 1;
          value1_5302 >>= 1;
          value1_5302 &= 0xff;
          _F2557 |= (SZ53P[value1_5302 & 0xff] | (value1_5302 == 0 ? 0x40 : 0));
          int result_5305 = value1_5302 & 0xFF;
          F = (_F2557 & 0xFF);
          _address2556 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2556 = result_5305;
          write(_address2556, result_5305);
          int read_5306;
          read_5306 = _value2556;
          L = read_5306;
          MEMPTR = _address2556;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int _F2560;
          int _value2559;
          int _address2559;
          contend2x1((PC + 3) & 0xFFFF);
          _address2559 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5307 = read(_address2559, 0);
          contend1x1(_address2559);
          _value2559 = operand_5307;
          int value1_5308 = _value2559;
          _F2560 = value1_5308 & 1;
          value1_5308 >>= 1;
          value1_5308 &= 0xff;
          _F2560 |= (SZ53P[value1_5308 & 0xff] | (value1_5308 == 0 ? 0x40 : 0));
          int result_5311 = value1_5308 & 0xFF;
          F = (_F2560 & 0xFF);
          _address2559 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2559 = result_5311;
          write(_address2559, result_5311);
          MEMPTR = _address2559;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F2563;
          int _value2562;
          int _address2562;
          contend2x1((PC + 3) & 0xFFFF);
          _address2562 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5312 = read(_address2562, 0);
          contend1x1(_address2562);
          _value2562 = operand_5312;
          int value1_5313 = _value2562;
          _F2563 = value1_5313 & 1;
          value1_5313 >>= 1;
          value1_5313 &= 0xff;
          _F2563 |= (SZ53P[value1_5313 & 0xff] | (value1_5313 == 0 ? 0x40 : 0));
          int result_5316 = value1_5313 & 0xFF;
          F = (_F2563 & 0xFF);
          _address2562 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2562 = result_5316;
          write(_address2562, result_5316);
          int read_5317;
          read_5317 = _value2562;
          A = read_5317;
          MEMPTR = _address2562;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_8(int opcode, int displacement) {
    switch (opcode) {
      case 0x40: {
          int _F2566;
          int _value2565;
          int _address2565;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5318;
          address_5318 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5319 = F & 1;
          _address2565 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5320 = read(_address2565, 0);
          contend1x1(_address2565);
          _value2565 = operand_5320;
          int value3_5323 = nAndCarry_5319;
          _F2566 = value3_5323;
          value3_5323 = value3_5323 >>> 1;
          _F2566 = (_F2566 & 1) | 0x10 | (address_5318 & 0x28);
          if ((_value2565 & (0x01 << value3_5323)) == 0) {
              _F2566 |= 0x44;
          }
          if (value3_5323 == 7 && (_value2565 & 0x80) != 0) {
              _F2566 |= 0x80;
          }
          F = (_F2566 & 0xFF);
          MEMPTR = _address2565;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x41: {
          int _F2569;
          int _value2568;
          int _address2568;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5325;
          address_5325 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5326 = F & 1;
          _address2568 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5327 = read(_address2568, 0);
          contend1x1(_address2568);
          _value2568 = operand_5327;
          int value3_5330 = nAndCarry_5326;
          _F2569 = value3_5330;
          value3_5330 = value3_5330 >>> 1;
          _F2569 = (_F2569 & 1) | 0x10 | (address_5325 & 0x28);
          if ((_value2568 & (0x01 << value3_5330)) == 0) {
              _F2569 |= 0x44;
          }
          if (value3_5330 == 7 && (_value2568 & 0x80) != 0) {
              _F2569 |= 0x80;
          }
          F = (_F2569 & 0xFF);
          MEMPTR = _address2568;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x42: {
          int _F2572;
          int _value2571;
          int _address2571;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5332;
          address_5332 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5333 = F & 1;
          _address2571 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5334 = read(_address2571, 0);
          contend1x1(_address2571);
          _value2571 = operand_5334;
          int value3_5337 = nAndCarry_5333;
          _F2572 = value3_5337;
          value3_5337 = value3_5337 >>> 1;
          _F2572 = (_F2572 & 1) | 0x10 | (address_5332 & 0x28);
          if ((_value2571 & (0x01 << value3_5337)) == 0) {
              _F2572 |= 0x44;
          }
          if (value3_5337 == 7 && (_value2571 & 0x80) != 0) {
              _F2572 |= 0x80;
          }
          F = (_F2572 & 0xFF);
          MEMPTR = _address2571;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x43: {
          int _F2575;
          int _value2574;
          int _address2574;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5339;
          address_5339 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5340 = F & 1;
          _address2574 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5341 = read(_address2574, 0);
          contend1x1(_address2574);
          _value2574 = operand_5341;
          int value3_5344 = nAndCarry_5340;
          _F2575 = value3_5344;
          value3_5344 = value3_5344 >>> 1;
          _F2575 = (_F2575 & 1) | 0x10 | (address_5339 & 0x28);
          if ((_value2574 & (0x01 << value3_5344)) == 0) {
              _F2575 |= 0x44;
          }
          if (value3_5344 == 7 && (_value2574 & 0x80) != 0) {
              _F2575 |= 0x80;
          }
          F = (_F2575 & 0xFF);
          MEMPTR = _address2574;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x44: {
          int _F2578;
          int _value2577;
          int _address2577;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5346;
          address_5346 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5347 = F & 1;
          _address2577 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5348 = read(_address2577, 0);
          contend1x1(_address2577);
          _value2577 = operand_5348;
          int value3_5351 = nAndCarry_5347;
          _F2578 = value3_5351;
          value3_5351 = value3_5351 >>> 1;
          _F2578 = (_F2578 & 1) | 0x10 | (address_5346 & 0x28);
          if ((_value2577 & (0x01 << value3_5351)) == 0) {
              _F2578 |= 0x44;
          }
          if (value3_5351 == 7 && (_value2577 & 0x80) != 0) {
              _F2578 |= 0x80;
          }
          F = (_F2578 & 0xFF);
          MEMPTR = _address2577;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x45: {
          int _F2581;
          int _value2580;
          int _address2580;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5353;
          address_5353 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5354 = F & 1;
          _address2580 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5355 = read(_address2580, 0);
          contend1x1(_address2580);
          _value2580 = operand_5355;
          int value3_5358 = nAndCarry_5354;
          _F2581 = value3_5358;
          value3_5358 = value3_5358 >>> 1;
          _F2581 = (_F2581 & 1) | 0x10 | (address_5353 & 0x28);
          if ((_value2580 & (0x01 << value3_5358)) == 0) {
              _F2581 |= 0x44;
          }
          if (value3_5358 == 7 && (_value2580 & 0x80) != 0) {
              _F2581 |= 0x80;
          }
          F = (_F2581 & 0xFF);
          MEMPTR = _address2580;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x46: {
          int _F2584;
          int _value2583;
          int _address2583;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5360;
          address_5360 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5361 = F & 1;
          _address2583 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5362 = read(_address2583, 0);
          contend1x1(_address2583);
          _value2583 = operand_5362;
          int value3_5365 = nAndCarry_5361;
          _F2584 = value3_5365;
          value3_5365 = value3_5365 >>> 1;
          _F2584 = (_F2584 & 1) | 0x10 | (address_5360 & 0x28);
          if ((_value2583 & (0x01 << value3_5365)) == 0) {
              _F2584 |= 0x44;
          }
          if (value3_5365 == 7 && (_value2583 & 0x80) != 0) {
              _F2584 |= 0x80;
          }
          F = (_F2584 & 0xFF);
          MEMPTR = _address2583;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x47: {
          int _F2587;
          int _value2586;
          int _address2586;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5367;
          address_5367 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5368 = F & 1;
          _address2586 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5369 = read(_address2586, 0);
          contend1x1(_address2586);
          _value2586 = operand_5369;
          int value3_5372 = nAndCarry_5368;
          _F2587 = value3_5372;
          value3_5372 = value3_5372 >>> 1;
          _F2587 = (_F2587 & 1) | 0x10 | (address_5367 & 0x28);
          if ((_value2586 & (0x01 << value3_5372)) == 0) {
              _F2587 |= 0x44;
          }
          if (value3_5372 == 7 && (_value2586 & 0x80) != 0) {
              _F2587 |= 0x80;
          }
          F = (_F2587 & 0xFF);
          MEMPTR = _address2586;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_9(int opcode, int displacement) {
    switch (opcode) {
      case 0x48: {
          int _F2590;
          int _value2589;
          int _address2589;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5374;
          address_5374 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5375 = 2 | F & 1;
          _address2589 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5376 = read(_address2589, 0);
          contend1x1(_address2589);
          _value2589 = operand_5376;
          int value3_5379 = nAndCarry_5375;
          _F2590 = value3_5379 & 1;
          value3_5379 = value3_5379 >>> 1;
          _F2590 = (_F2590 & 1) | 0x10 | (address_5374 & 0x28);
          if ((_value2589 & (0x01 << value3_5379)) == 0) {
              _F2590 |= 0x44;
          }
          if (value3_5379 == 7 && (_value2589 & 0x80) != 0) {
              _F2590 |= 0x80;
          }
          F = (_F2590 & 0xFF);
          MEMPTR = _address2589;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x49: {
          int _F2593;
          int _value2592;
          int _address2592;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5381;
          address_5381 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5382 = 2 | F & 1;
          _address2592 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5383 = read(_address2592, 0);
          contend1x1(_address2592);
          _value2592 = operand_5383;
          int value3_5386 = nAndCarry_5382;
          _F2593 = value3_5386 & 1;
          value3_5386 = value3_5386 >>> 1;
          _F2593 = (_F2593 & 1) | 0x10 | (address_5381 & 0x28);
          if ((_value2592 & (0x01 << value3_5386)) == 0) {
              _F2593 |= 0x44;
          }
          if (value3_5386 == 7 && (_value2592 & 0x80) != 0) {
              _F2593 |= 0x80;
          }
          F = (_F2593 & 0xFF);
          MEMPTR = _address2592;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4A: {
          int _F2596;
          int _value2595;
          int _address2595;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5388;
          address_5388 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5389 = 2 | F & 1;
          _address2595 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5390 = read(_address2595, 0);
          contend1x1(_address2595);
          _value2595 = operand_5390;
          int value3_5393 = nAndCarry_5389;
          _F2596 = value3_5393 & 1;
          value3_5393 = value3_5393 >>> 1;
          _F2596 = (_F2596 & 1) | 0x10 | (address_5388 & 0x28);
          if ((_value2595 & (0x01 << value3_5393)) == 0) {
              _F2596 |= 0x44;
          }
          if (value3_5393 == 7 && (_value2595 & 0x80) != 0) {
              _F2596 |= 0x80;
          }
          F = (_F2596 & 0xFF);
          MEMPTR = _address2595;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4B: {
          int _F2599;
          int _value2598;
          int _address2598;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5395;
          address_5395 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5396 = 2 | F & 1;
          _address2598 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5397 = read(_address2598, 0);
          contend1x1(_address2598);
          _value2598 = operand_5397;
          int value3_5400 = nAndCarry_5396;
          _F2599 = value3_5400 & 1;
          value3_5400 = value3_5400 >>> 1;
          _F2599 = (_F2599 & 1) | 0x10 | (address_5395 & 0x28);
          if ((_value2598 & (0x01 << value3_5400)) == 0) {
              _F2599 |= 0x44;
          }
          if (value3_5400 == 7 && (_value2598 & 0x80) != 0) {
              _F2599 |= 0x80;
          }
          F = (_F2599 & 0xFF);
          MEMPTR = _address2598;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4C: {
          int _F2602;
          int _value2601;
          int _address2601;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5402;
          address_5402 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5403 = 2 | F & 1;
          _address2601 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5404 = read(_address2601, 0);
          contend1x1(_address2601);
          _value2601 = operand_5404;
          int value3_5407 = nAndCarry_5403;
          _F2602 = value3_5407 & 1;
          value3_5407 = value3_5407 >>> 1;
          _F2602 = (_F2602 & 1) | 0x10 | (address_5402 & 0x28);
          if ((_value2601 & (0x01 << value3_5407)) == 0) {
              _F2602 |= 0x44;
          }
          if (value3_5407 == 7 && (_value2601 & 0x80) != 0) {
              _F2602 |= 0x80;
          }
          F = (_F2602 & 0xFF);
          MEMPTR = _address2601;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4D: {
          int _F2605;
          int _value2604;
          int _address2604;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5409;
          address_5409 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5410 = 2 | F & 1;
          _address2604 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5411 = read(_address2604, 0);
          contend1x1(_address2604);
          _value2604 = operand_5411;
          int value3_5414 = nAndCarry_5410;
          _F2605 = value3_5414 & 1;
          value3_5414 = value3_5414 >>> 1;
          _F2605 = (_F2605 & 1) | 0x10 | (address_5409 & 0x28);
          if ((_value2604 & (0x01 << value3_5414)) == 0) {
              _F2605 |= 0x44;
          }
          if (value3_5414 == 7 && (_value2604 & 0x80) != 0) {
              _F2605 |= 0x80;
          }
          F = (_F2605 & 0xFF);
          MEMPTR = _address2604;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4E: {
          int _F2608;
          int _value2607;
          int _address2607;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5416;
          address_5416 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5417 = 2 | F & 1;
          _address2607 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5418 = read(_address2607, 0);
          contend1x1(_address2607);
          _value2607 = operand_5418;
          int value3_5421 = nAndCarry_5417;
          _F2608 = value3_5421 & 1;
          value3_5421 = value3_5421 >>> 1;
          _F2608 = (_F2608 & 1) | 0x10 | (address_5416 & 0x28);
          if ((_value2607 & (0x01 << value3_5421)) == 0) {
              _F2608 |= 0x44;
          }
          if (value3_5421 == 7 && (_value2607 & 0x80) != 0) {
              _F2608 |= 0x80;
          }
          F = (_F2608 & 0xFF);
          MEMPTR = _address2607;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4F: {
          int _F2611;
          int _value2610;
          int _address2610;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5423;
          address_5423 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5424 = 2 | F & 1;
          _address2610 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5425 = read(_address2610, 0);
          contend1x1(_address2610);
          _value2610 = operand_5425;
          int value3_5428 = nAndCarry_5424;
          _F2611 = value3_5428 & 1;
          value3_5428 = value3_5428 >>> 1;
          _F2611 = (_F2611 & 1) | 0x10 | (address_5423 & 0x28);
          if ((_value2610 & (0x01 << value3_5428)) == 0) {
              _F2611 |= 0x44;
          }
          if (value3_5428 == 7 && (_value2610 & 0x80) != 0) {
              _F2611 |= 0x80;
          }
          F = (_F2611 & 0xFF);
          MEMPTR = _address2610;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_10(int opcode, int displacement) {
    switch (opcode) {
      case 0x50: {
          int _F2614;
          int _value2613;
          int _address2613;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5430;
          address_5430 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5431 = 4 | F & 1;
          _address2613 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5432 = read(_address2613, 0);
          contend1x1(_address2613);
          _value2613 = operand_5432;
          int value3_5435 = nAndCarry_5431;
          _F2614 = value3_5435 & 1;
          value3_5435 = value3_5435 >>> 1;
          _F2614 = (_F2614 & 1) | 0x10 | (address_5430 & 0x28);
          if ((_value2613 & (0x01 << value3_5435)) == 0) {
              _F2614 |= 0x44;
          }
          if (value3_5435 == 7 && (_value2613 & 0x80) != 0) {
              _F2614 |= 0x80;
          }
          F = (_F2614 & 0xFF);
          MEMPTR = _address2613;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x51: {
          int _F2617;
          int _value2616;
          int _address2616;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5437;
          address_5437 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5438 = 4 | F & 1;
          _address2616 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5439 = read(_address2616, 0);
          contend1x1(_address2616);
          _value2616 = operand_5439;
          int value3_5442 = nAndCarry_5438;
          _F2617 = value3_5442 & 1;
          value3_5442 = value3_5442 >>> 1;
          _F2617 = (_F2617 & 1) | 0x10 | (address_5437 & 0x28);
          if ((_value2616 & (0x01 << value3_5442)) == 0) {
              _F2617 |= 0x44;
          }
          if (value3_5442 == 7 && (_value2616 & 0x80) != 0) {
              _F2617 |= 0x80;
          }
          F = (_F2617 & 0xFF);
          MEMPTR = _address2616;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x52: {
          int _F2620;
          int _value2619;
          int _address2619;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5444;
          address_5444 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5445 = 4 | F & 1;
          _address2619 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5446 = read(_address2619, 0);
          contend1x1(_address2619);
          _value2619 = operand_5446;
          int value3_5449 = nAndCarry_5445;
          _F2620 = value3_5449 & 1;
          value3_5449 = value3_5449 >>> 1;
          _F2620 = (_F2620 & 1) | 0x10 | (address_5444 & 0x28);
          if ((_value2619 & (0x01 << value3_5449)) == 0) {
              _F2620 |= 0x44;
          }
          if (value3_5449 == 7 && (_value2619 & 0x80) != 0) {
              _F2620 |= 0x80;
          }
          F = (_F2620 & 0xFF);
          MEMPTR = _address2619;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x53: {
          int _F2623;
          int _value2622;
          int _address2622;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5451;
          address_5451 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5452 = 4 | F & 1;
          _address2622 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5453 = read(_address2622, 0);
          contend1x1(_address2622);
          _value2622 = operand_5453;
          int value3_5456 = nAndCarry_5452;
          _F2623 = value3_5456 & 1;
          value3_5456 = value3_5456 >>> 1;
          _F2623 = (_F2623 & 1) | 0x10 | (address_5451 & 0x28);
          if ((_value2622 & (0x01 << value3_5456)) == 0) {
              _F2623 |= 0x44;
          }
          if (value3_5456 == 7 && (_value2622 & 0x80) != 0) {
              _F2623 |= 0x80;
          }
          F = (_F2623 & 0xFF);
          MEMPTR = _address2622;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x54: {
          int _F2626;
          int _value2625;
          int _address2625;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5458;
          address_5458 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5459 = 4 | F & 1;
          _address2625 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5460 = read(_address2625, 0);
          contend1x1(_address2625);
          _value2625 = operand_5460;
          int value3_5463 = nAndCarry_5459;
          _F2626 = value3_5463 & 1;
          value3_5463 = value3_5463 >>> 1;
          _F2626 = (_F2626 & 1) | 0x10 | (address_5458 & 0x28);
          if ((_value2625 & (0x01 << value3_5463)) == 0) {
              _F2626 |= 0x44;
          }
          if (value3_5463 == 7 && (_value2625 & 0x80) != 0) {
              _F2626 |= 0x80;
          }
          F = (_F2626 & 0xFF);
          MEMPTR = _address2625;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x55: {
          int _F2629;
          int _value2628;
          int _address2628;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5465;
          address_5465 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5466 = 4 | F & 1;
          _address2628 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5467 = read(_address2628, 0);
          contend1x1(_address2628);
          _value2628 = operand_5467;
          int value3_5470 = nAndCarry_5466;
          _F2629 = value3_5470 & 1;
          value3_5470 = value3_5470 >>> 1;
          _F2629 = (_F2629 & 1) | 0x10 | (address_5465 & 0x28);
          if ((_value2628 & (0x01 << value3_5470)) == 0) {
              _F2629 |= 0x44;
          }
          if (value3_5470 == 7 && (_value2628 & 0x80) != 0) {
              _F2629 |= 0x80;
          }
          F = (_F2629 & 0xFF);
          MEMPTR = _address2628;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x56: {
          int _F2632;
          int _value2631;
          int _address2631;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5472;
          address_5472 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5473 = 4 | F & 1;
          _address2631 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5474 = read(_address2631, 0);
          contend1x1(_address2631);
          _value2631 = operand_5474;
          int value3_5477 = nAndCarry_5473;
          _F2632 = value3_5477 & 1;
          value3_5477 = value3_5477 >>> 1;
          _F2632 = (_F2632 & 1) | 0x10 | (address_5472 & 0x28);
          if ((_value2631 & (0x01 << value3_5477)) == 0) {
              _F2632 |= 0x44;
          }
          if (value3_5477 == 7 && (_value2631 & 0x80) != 0) {
              _F2632 |= 0x80;
          }
          F = (_F2632 & 0xFF);
          MEMPTR = _address2631;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x57: {
          int _F2635;
          int _value2634;
          int _address2634;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5479;
          address_5479 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5480 = 4 | F & 1;
          _address2634 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5481 = read(_address2634, 0);
          contend1x1(_address2634);
          _value2634 = operand_5481;
          int value3_5484 = nAndCarry_5480;
          _F2635 = value3_5484 & 1;
          value3_5484 = value3_5484 >>> 1;
          _F2635 = (_F2635 & 1) | 0x10 | (address_5479 & 0x28);
          if ((_value2634 & (0x01 << value3_5484)) == 0) {
              _F2635 |= 0x44;
          }
          if (value3_5484 == 7 && (_value2634 & 0x80) != 0) {
              _F2635 |= 0x80;
          }
          F = (_F2635 & 0xFF);
          MEMPTR = _address2634;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_11(int opcode, int displacement) {
    switch (opcode) {
      case 0x58: {
          int _F2638;
          int _value2637;
          int _address2637;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5486;
          address_5486 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5487 = 6 | F & 1;
          _address2637 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5488 = read(_address2637, 0);
          contend1x1(_address2637);
          _value2637 = operand_5488;
          int value3_5491 = nAndCarry_5487;
          _F2638 = value3_5491 & 1;
          value3_5491 = value3_5491 >>> 1;
          _F2638 = (_F2638 & 1) | 0x10 | (address_5486 & 0x28);
          if ((_value2637 & (0x01 << value3_5491)) == 0) {
              _F2638 |= 0x44;
          }
          if (value3_5491 == 7 && (_value2637 & 0x80) != 0) {
              _F2638 |= 0x80;
          }
          F = (_F2638 & 0xFF);
          MEMPTR = _address2637;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x59: {
          int _F2641;
          int _value2640;
          int _address2640;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5493;
          address_5493 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5494 = 6 | F & 1;
          _address2640 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5495 = read(_address2640, 0);
          contend1x1(_address2640);
          _value2640 = operand_5495;
          int value3_5498 = nAndCarry_5494;
          _F2641 = value3_5498 & 1;
          value3_5498 = value3_5498 >>> 1;
          _F2641 = (_F2641 & 1) | 0x10 | (address_5493 & 0x28);
          if ((_value2640 & (0x01 << value3_5498)) == 0) {
              _F2641 |= 0x44;
          }
          if (value3_5498 == 7 && (_value2640 & 0x80) != 0) {
              _F2641 |= 0x80;
          }
          F = (_F2641 & 0xFF);
          MEMPTR = _address2640;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5A: {
          int _F2644;
          int _value2643;
          int _address2643;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5500;
          address_5500 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5501 = 6 | F & 1;
          _address2643 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5502 = read(_address2643, 0);
          contend1x1(_address2643);
          _value2643 = operand_5502;
          int value3_5505 = nAndCarry_5501;
          _F2644 = value3_5505 & 1;
          value3_5505 = value3_5505 >>> 1;
          _F2644 = (_F2644 & 1) | 0x10 | (address_5500 & 0x28);
          if ((_value2643 & (0x01 << value3_5505)) == 0) {
              _F2644 |= 0x44;
          }
          if (value3_5505 == 7 && (_value2643 & 0x80) != 0) {
              _F2644 |= 0x80;
          }
          F = (_F2644 & 0xFF);
          MEMPTR = _address2643;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5B: {
          int _F2647;
          int _value2646;
          int _address2646;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5507;
          address_5507 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5508 = 6 | F & 1;
          _address2646 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5509 = read(_address2646, 0);
          contend1x1(_address2646);
          _value2646 = operand_5509;
          int value3_5512 = nAndCarry_5508;
          _F2647 = value3_5512 & 1;
          value3_5512 = value3_5512 >>> 1;
          _F2647 = (_F2647 & 1) | 0x10 | (address_5507 & 0x28);
          if ((_value2646 & (0x01 << value3_5512)) == 0) {
              _F2647 |= 0x44;
          }
          if (value3_5512 == 7 && (_value2646 & 0x80) != 0) {
              _F2647 |= 0x80;
          }
          F = (_F2647 & 0xFF);
          MEMPTR = _address2646;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5C: {
          int _F2650;
          int _value2649;
          int _address2649;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5514;
          address_5514 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5515 = 6 | F & 1;
          _address2649 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5516 = read(_address2649, 0);
          contend1x1(_address2649);
          _value2649 = operand_5516;
          int value3_5519 = nAndCarry_5515;
          _F2650 = value3_5519 & 1;
          value3_5519 = value3_5519 >>> 1;
          _F2650 = (_F2650 & 1) | 0x10 | (address_5514 & 0x28);
          if ((_value2649 & (0x01 << value3_5519)) == 0) {
              _F2650 |= 0x44;
          }
          if (value3_5519 == 7 && (_value2649 & 0x80) != 0) {
              _F2650 |= 0x80;
          }
          F = (_F2650 & 0xFF);
          MEMPTR = _address2649;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5D: {
          int _F2653;
          int _value2652;
          int _address2652;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5521;
          address_5521 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5522 = 6 | F & 1;
          _address2652 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5523 = read(_address2652, 0);
          contend1x1(_address2652);
          _value2652 = operand_5523;
          int value3_5526 = nAndCarry_5522;
          _F2653 = value3_5526 & 1;
          value3_5526 = value3_5526 >>> 1;
          _F2653 = (_F2653 & 1) | 0x10 | (address_5521 & 0x28);
          if ((_value2652 & (0x01 << value3_5526)) == 0) {
              _F2653 |= 0x44;
          }
          if (value3_5526 == 7 && (_value2652 & 0x80) != 0) {
              _F2653 |= 0x80;
          }
          F = (_F2653 & 0xFF);
          MEMPTR = _address2652;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5E: {
          int _F2656;
          int _value2655;
          int _address2655;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5528;
          address_5528 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5529 = 6 | F & 1;
          _address2655 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5530 = read(_address2655, 0);
          contend1x1(_address2655);
          _value2655 = operand_5530;
          int value3_5533 = nAndCarry_5529;
          _F2656 = value3_5533 & 1;
          value3_5533 = value3_5533 >>> 1;
          _F2656 = (_F2656 & 1) | 0x10 | (address_5528 & 0x28);
          if ((_value2655 & (0x01 << value3_5533)) == 0) {
              _F2656 |= 0x44;
          }
          if (value3_5533 == 7 && (_value2655 & 0x80) != 0) {
              _F2656 |= 0x80;
          }
          F = (_F2656 & 0xFF);
          MEMPTR = _address2655;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5F: {
          int _F2659;
          int _value2658;
          int _address2658;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5535;
          address_5535 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5536 = 6 | F & 1;
          _address2658 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5537 = read(_address2658, 0);
          contend1x1(_address2658);
          _value2658 = operand_5537;
          int value3_5540 = nAndCarry_5536;
          _F2659 = value3_5540 & 1;
          value3_5540 = value3_5540 >>> 1;
          _F2659 = (_F2659 & 1) | 0x10 | (address_5535 & 0x28);
          if ((_value2658 & (0x01 << value3_5540)) == 0) {
              _F2659 |= 0x44;
          }
          if (value3_5540 == 7 && (_value2658 & 0x80) != 0) {
              _F2659 |= 0x80;
          }
          F = (_F2659 & 0xFF);
          MEMPTR = _address2658;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_12(int opcode, int displacement) {
    switch (opcode) {
      case 0x60: {
          int _F2662;
          int _value2661;
          int _address2661;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5542;
          address_5542 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5543 = 8 | F & 1;
          _address2661 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5544 = read(_address2661, 0);
          contend1x1(_address2661);
          _value2661 = operand_5544;
          int value3_5547 = nAndCarry_5543;
          _F2662 = value3_5547 & 1;
          value3_5547 = value3_5547 >>> 1;
          _F2662 = (_F2662 & 1) | 0x10 | (address_5542 & 0x28);
          if ((_value2661 & (0x01 << value3_5547)) == 0) {
              _F2662 |= 0x44;
          }
          if (value3_5547 == 7 && (_value2661 & 0x80) != 0) {
              _F2662 |= 0x80;
          }
          F = (_F2662 & 0xFF);
          MEMPTR = _address2661;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x61: {
          int _F2665;
          int _value2664;
          int _address2664;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5549;
          address_5549 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5550 = 8 | F & 1;
          _address2664 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5551 = read(_address2664, 0);
          contend1x1(_address2664);
          _value2664 = operand_5551;
          int value3_5554 = nAndCarry_5550;
          _F2665 = value3_5554 & 1;
          value3_5554 = value3_5554 >>> 1;
          _F2665 = (_F2665 & 1) | 0x10 | (address_5549 & 0x28);
          if ((_value2664 & (0x01 << value3_5554)) == 0) {
              _F2665 |= 0x44;
          }
          if (value3_5554 == 7 && (_value2664 & 0x80) != 0) {
              _F2665 |= 0x80;
          }
          F = (_F2665 & 0xFF);
          MEMPTR = _address2664;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x62: {
          int _F2668;
          int _value2667;
          int _address2667;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5556;
          address_5556 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5557 = 8 | F & 1;
          _address2667 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5558 = read(_address2667, 0);
          contend1x1(_address2667);
          _value2667 = operand_5558;
          int value3_5561 = nAndCarry_5557;
          _F2668 = value3_5561 & 1;
          value3_5561 = value3_5561 >>> 1;
          _F2668 = (_F2668 & 1) | 0x10 | (address_5556 & 0x28);
          if ((_value2667 & (0x01 << value3_5561)) == 0) {
              _F2668 |= 0x44;
          }
          if (value3_5561 == 7 && (_value2667 & 0x80) != 0) {
              _F2668 |= 0x80;
          }
          F = (_F2668 & 0xFF);
          MEMPTR = _address2667;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x63: {
          int _F2671;
          int _value2670;
          int _address2670;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5563;
          address_5563 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5564 = 8 | F & 1;
          _address2670 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5565 = read(_address2670, 0);
          contend1x1(_address2670);
          _value2670 = operand_5565;
          int value3_5568 = nAndCarry_5564;
          _F2671 = value3_5568 & 1;
          value3_5568 = value3_5568 >>> 1;
          _F2671 = (_F2671 & 1) | 0x10 | (address_5563 & 0x28);
          if ((_value2670 & (0x01 << value3_5568)) == 0) {
              _F2671 |= 0x44;
          }
          if (value3_5568 == 7 && (_value2670 & 0x80) != 0) {
              _F2671 |= 0x80;
          }
          F = (_F2671 & 0xFF);
          MEMPTR = _address2670;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x64: {
          int _F2674;
          int _value2673;
          int _address2673;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5570;
          address_5570 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5571 = 8 | F & 1;
          _address2673 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5572 = read(_address2673, 0);
          contend1x1(_address2673);
          _value2673 = operand_5572;
          int value3_5575 = nAndCarry_5571;
          _F2674 = value3_5575 & 1;
          value3_5575 = value3_5575 >>> 1;
          _F2674 = (_F2674 & 1) | 0x10 | (address_5570 & 0x28);
          if ((_value2673 & (0x01 << value3_5575)) == 0) {
              _F2674 |= 0x44;
          }
          if (value3_5575 == 7 && (_value2673 & 0x80) != 0) {
              _F2674 |= 0x80;
          }
          F = (_F2674 & 0xFF);
          MEMPTR = _address2673;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x65: {
          int _F2677;
          int _value2676;
          int _address2676;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5577;
          address_5577 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5578 = 8 | F & 1;
          _address2676 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5579 = read(_address2676, 0);
          contend1x1(_address2676);
          _value2676 = operand_5579;
          int value3_5582 = nAndCarry_5578;
          _F2677 = value3_5582 & 1;
          value3_5582 = value3_5582 >>> 1;
          _F2677 = (_F2677 & 1) | 0x10 | (address_5577 & 0x28);
          if ((_value2676 & (0x01 << value3_5582)) == 0) {
              _F2677 |= 0x44;
          }
          if (value3_5582 == 7 && (_value2676 & 0x80) != 0) {
              _F2677 |= 0x80;
          }
          F = (_F2677 & 0xFF);
          MEMPTR = _address2676;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x66: {
          int _F2680;
          int _value2679;
          int _address2679;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5584;
          address_5584 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5585 = 8 | F & 1;
          _address2679 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5586 = read(_address2679, 0);
          contend1x1(_address2679);
          _value2679 = operand_5586;
          int value3_5589 = nAndCarry_5585;
          _F2680 = value3_5589 & 1;
          value3_5589 = value3_5589 >>> 1;
          _F2680 = (_F2680 & 1) | 0x10 | (address_5584 & 0x28);
          if ((_value2679 & (0x01 << value3_5589)) == 0) {
              _F2680 |= 0x44;
          }
          if (value3_5589 == 7 && (_value2679 & 0x80) != 0) {
              _F2680 |= 0x80;
          }
          F = (_F2680 & 0xFF);
          MEMPTR = _address2679;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x67: {
          int _F2683;
          int _value2682;
          int _address2682;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5591;
          address_5591 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5592 = 8 | F & 1;
          _address2682 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5593 = read(_address2682, 0);
          contend1x1(_address2682);
          _value2682 = operand_5593;
          int value3_5596 = nAndCarry_5592;
          _F2683 = value3_5596 & 1;
          value3_5596 = value3_5596 >>> 1;
          _F2683 = (_F2683 & 1) | 0x10 | (address_5591 & 0x28);
          if ((_value2682 & (0x01 << value3_5596)) == 0) {
              _F2683 |= 0x44;
          }
          if (value3_5596 == 7 && (_value2682 & 0x80) != 0) {
              _F2683 |= 0x80;
          }
          F = (_F2683 & 0xFF);
          MEMPTR = _address2682;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_13(int opcode, int displacement) {
    switch (opcode) {
      case 0x68: {
          int _F2686;
          int _value2685;
          int _address2685;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5598;
          address_5598 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5599 = 10 | F & 1;
          _address2685 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5600 = read(_address2685, 0);
          contend1x1(_address2685);
          _value2685 = operand_5600;
          int value3_5603 = nAndCarry_5599;
          _F2686 = value3_5603 & 1;
          value3_5603 = value3_5603 >>> 1;
          _F2686 = (_F2686 & 1) | 0x10 | (address_5598 & 0x28);
          if ((_value2685 & (0x01 << value3_5603)) == 0) {
              _F2686 |= 0x44;
          }
          if (value3_5603 == 7 && (_value2685 & 0x80) != 0) {
              _F2686 |= 0x80;
          }
          F = (_F2686 & 0xFF);
          MEMPTR = _address2685;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x69: {
          int _F2689;
          int _value2688;
          int _address2688;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5605;
          address_5605 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5606 = 10 | F & 1;
          _address2688 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5607 = read(_address2688, 0);
          contend1x1(_address2688);
          _value2688 = operand_5607;
          int value3_5610 = nAndCarry_5606;
          _F2689 = value3_5610 & 1;
          value3_5610 = value3_5610 >>> 1;
          _F2689 = (_F2689 & 1) | 0x10 | (address_5605 & 0x28);
          if ((_value2688 & (0x01 << value3_5610)) == 0) {
              _F2689 |= 0x44;
          }
          if (value3_5610 == 7 && (_value2688 & 0x80) != 0) {
              _F2689 |= 0x80;
          }
          F = (_F2689 & 0xFF);
          MEMPTR = _address2688;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6A: {
          int _F2692;
          int _value2691;
          int _address2691;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5612;
          address_5612 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5613 = 10 | F & 1;
          _address2691 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5614 = read(_address2691, 0);
          contend1x1(_address2691);
          _value2691 = operand_5614;
          int value3_5617 = nAndCarry_5613;
          _F2692 = value3_5617 & 1;
          value3_5617 = value3_5617 >>> 1;
          _F2692 = (_F2692 & 1) | 0x10 | (address_5612 & 0x28);
          if ((_value2691 & (0x01 << value3_5617)) == 0) {
              _F2692 |= 0x44;
          }
          if (value3_5617 == 7 && (_value2691 & 0x80) != 0) {
              _F2692 |= 0x80;
          }
          F = (_F2692 & 0xFF);
          MEMPTR = _address2691;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6B: {
          int _F2695;
          int _value2694;
          int _address2694;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5619;
          address_5619 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5620 = 10 | F & 1;
          _address2694 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5621 = read(_address2694, 0);
          contend1x1(_address2694);
          _value2694 = operand_5621;
          int value3_5624 = nAndCarry_5620;
          _F2695 = value3_5624 & 1;
          value3_5624 = value3_5624 >>> 1;
          _F2695 = (_F2695 & 1) | 0x10 | (address_5619 & 0x28);
          if ((_value2694 & (0x01 << value3_5624)) == 0) {
              _F2695 |= 0x44;
          }
          if (value3_5624 == 7 && (_value2694 & 0x80) != 0) {
              _F2695 |= 0x80;
          }
          F = (_F2695 & 0xFF);
          MEMPTR = _address2694;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6C: {
          int _F2698;
          int _value2697;
          int _address2697;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5626;
          address_5626 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5627 = 10 | F & 1;
          _address2697 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5628 = read(_address2697, 0);
          contend1x1(_address2697);
          _value2697 = operand_5628;
          int value3_5631 = nAndCarry_5627;
          _F2698 = value3_5631 & 1;
          value3_5631 = value3_5631 >>> 1;
          _F2698 = (_F2698 & 1) | 0x10 | (address_5626 & 0x28);
          if ((_value2697 & (0x01 << value3_5631)) == 0) {
              _F2698 |= 0x44;
          }
          if (value3_5631 == 7 && (_value2697 & 0x80) != 0) {
              _F2698 |= 0x80;
          }
          F = (_F2698 & 0xFF);
          MEMPTR = _address2697;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6D: {
          int _F2701;
          int _value2700;
          int _address2700;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5633;
          address_5633 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5634 = 10 | F & 1;
          _address2700 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5635 = read(_address2700, 0);
          contend1x1(_address2700);
          _value2700 = operand_5635;
          int value3_5638 = nAndCarry_5634;
          _F2701 = value3_5638 & 1;
          value3_5638 = value3_5638 >>> 1;
          _F2701 = (_F2701 & 1) | 0x10 | (address_5633 & 0x28);
          if ((_value2700 & (0x01 << value3_5638)) == 0) {
              _F2701 |= 0x44;
          }
          if (value3_5638 == 7 && (_value2700 & 0x80) != 0) {
              _F2701 |= 0x80;
          }
          F = (_F2701 & 0xFF);
          MEMPTR = _address2700;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6E: {
          int _F2704;
          int _value2703;
          int _address2703;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5640;
          address_5640 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5641 = 10 | F & 1;
          _address2703 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5642 = read(_address2703, 0);
          contend1x1(_address2703);
          _value2703 = operand_5642;
          int value3_5645 = nAndCarry_5641;
          _F2704 = value3_5645 & 1;
          value3_5645 = value3_5645 >>> 1;
          _F2704 = (_F2704 & 1) | 0x10 | (address_5640 & 0x28);
          if ((_value2703 & (0x01 << value3_5645)) == 0) {
              _F2704 |= 0x44;
          }
          if (value3_5645 == 7 && (_value2703 & 0x80) != 0) {
              _F2704 |= 0x80;
          }
          F = (_F2704 & 0xFF);
          MEMPTR = _address2703;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6F: {
          int _F2707;
          int _value2706;
          int _address2706;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5647;
          address_5647 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5648 = 10 | F & 1;
          _address2706 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5649 = read(_address2706, 0);
          contend1x1(_address2706);
          _value2706 = operand_5649;
          int value3_5652 = nAndCarry_5648;
          _F2707 = value3_5652 & 1;
          value3_5652 = value3_5652 >>> 1;
          _F2707 = (_F2707 & 1) | 0x10 | (address_5647 & 0x28);
          if ((_value2706 & (0x01 << value3_5652)) == 0) {
              _F2707 |= 0x44;
          }
          if (value3_5652 == 7 && (_value2706 & 0x80) != 0) {
              _F2707 |= 0x80;
          }
          F = (_F2707 & 0xFF);
          MEMPTR = _address2706;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_14(int opcode, int displacement) {
    switch (opcode) {
      case 0x70: {
          int _F2710;
          int _value2709;
          int _address2709;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5654;
          address_5654 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5655 = 12 | F & 1;
          _address2709 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5656 = read(_address2709, 0);
          contend1x1(_address2709);
          _value2709 = operand_5656;
          int value3_5659 = nAndCarry_5655;
          _F2710 = value3_5659 & 1;
          value3_5659 = value3_5659 >>> 1;
          _F2710 = (_F2710 & 1) | 0x10 | (address_5654 & 0x28);
          if ((_value2709 & (0x01 << value3_5659)) == 0) {
              _F2710 |= 0x44;
          }
          if (value3_5659 == 7 && (_value2709 & 0x80) != 0) {
              _F2710 |= 0x80;
          }
          F = (_F2710 & 0xFF);
          MEMPTR = _address2709;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _F2713;
          int _value2712;
          int _address2712;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5661;
          address_5661 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5662 = 12 | F & 1;
          _address2712 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5663 = read(_address2712, 0);
          contend1x1(_address2712);
          _value2712 = operand_5663;
          int value3_5666 = nAndCarry_5662;
          _F2713 = value3_5666 & 1;
          value3_5666 = value3_5666 >>> 1;
          _F2713 = (_F2713 & 1) | 0x10 | (address_5661 & 0x28);
          if ((_value2712 & (0x01 << value3_5666)) == 0) {
              _F2713 |= 0x44;
          }
          if (value3_5666 == 7 && (_value2712 & 0x80) != 0) {
              _F2713 |= 0x80;
          }
          F = (_F2713 & 0xFF);
          MEMPTR = _address2712;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _F2716;
          int _value2715;
          int _address2715;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5668;
          address_5668 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5669 = 12 | F & 1;
          _address2715 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5670 = read(_address2715, 0);
          contend1x1(_address2715);
          _value2715 = operand_5670;
          int value3_5673 = nAndCarry_5669;
          _F2716 = value3_5673 & 1;
          value3_5673 = value3_5673 >>> 1;
          _F2716 = (_F2716 & 1) | 0x10 | (address_5668 & 0x28);
          if ((_value2715 & (0x01 << value3_5673)) == 0) {
              _F2716 |= 0x44;
          }
          if (value3_5673 == 7 && (_value2715 & 0x80) != 0) {
              _F2716 |= 0x80;
          }
          F = (_F2716 & 0xFF);
          MEMPTR = _address2715;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _F2719;
          int _value2718;
          int _address2718;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5675;
          address_5675 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5676 = 12 | F & 1;
          _address2718 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5677 = read(_address2718, 0);
          contend1x1(_address2718);
          _value2718 = operand_5677;
          int value3_5680 = nAndCarry_5676;
          _F2719 = value3_5680 & 1;
          value3_5680 = value3_5680 >>> 1;
          _F2719 = (_F2719 & 1) | 0x10 | (address_5675 & 0x28);
          if ((_value2718 & (0x01 << value3_5680)) == 0) {
              _F2719 |= 0x44;
          }
          if (value3_5680 == 7 && (_value2718 & 0x80) != 0) {
              _F2719 |= 0x80;
          }
          F = (_F2719 & 0xFF);
          MEMPTR = _address2718;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _F2722;
          int _value2721;
          int _address2721;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5682;
          address_5682 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5683 = 12 | F & 1;
          _address2721 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5684 = read(_address2721, 0);
          contend1x1(_address2721);
          _value2721 = operand_5684;
          int value3_5687 = nAndCarry_5683;
          _F2722 = value3_5687 & 1;
          value3_5687 = value3_5687 >>> 1;
          _F2722 = (_F2722 & 1) | 0x10 | (address_5682 & 0x28);
          if ((_value2721 & (0x01 << value3_5687)) == 0) {
              _F2722 |= 0x44;
          }
          if (value3_5687 == 7 && (_value2721 & 0x80) != 0) {
              _F2722 |= 0x80;
          }
          F = (_F2722 & 0xFF);
          MEMPTR = _address2721;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _F2725;
          int _value2724;
          int _address2724;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5689;
          address_5689 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5690 = 12 | F & 1;
          _address2724 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5691 = read(_address2724, 0);
          contend1x1(_address2724);
          _value2724 = operand_5691;
          int value3_5694 = nAndCarry_5690;
          _F2725 = value3_5694 & 1;
          value3_5694 = value3_5694 >>> 1;
          _F2725 = (_F2725 & 1) | 0x10 | (address_5689 & 0x28);
          if ((_value2724 & (0x01 << value3_5694)) == 0) {
              _F2725 |= 0x44;
          }
          if (value3_5694 == 7 && (_value2724 & 0x80) != 0) {
              _F2725 |= 0x80;
          }
          F = (_F2725 & 0xFF);
          MEMPTR = _address2724;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x76: {
          int _F2728;
          int _value2727;
          int _address2727;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5696;
          address_5696 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5697 = 12 | F & 1;
          _address2727 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5698 = read(_address2727, 0);
          contend1x1(_address2727);
          _value2727 = operand_5698;
          int value3_5701 = nAndCarry_5697;
          _F2728 = value3_5701 & 1;
          value3_5701 = value3_5701 >>> 1;
          _F2728 = (_F2728 & 1) | 0x10 | (address_5696 & 0x28);
          if ((_value2727 & (0x01 << value3_5701)) == 0) {
              _F2728 |= 0x44;
          }
          if (value3_5701 == 7 && (_value2727 & 0x80) != 0) {
              _F2728 |= 0x80;
          }
          F = (_F2728 & 0xFF);
          MEMPTR = _address2727;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x77: {
          int _F2731;
          int _value2730;
          int _address2730;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5703;
          address_5703 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5704 = 12 | F & 1;
          _address2730 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5705 = read(_address2730, 0);
          contend1x1(_address2730);
          _value2730 = operand_5705;
          int value3_5708 = nAndCarry_5704;
          _F2731 = value3_5708 & 1;
          value3_5708 = value3_5708 >>> 1;
          _F2731 = (_F2731 & 1) | 0x10 | (address_5703 & 0x28);
          if ((_value2730 & (0x01 << value3_5708)) == 0) {
              _F2731 |= 0x44;
          }
          if (value3_5708 == 7 && (_value2730 & 0x80) != 0) {
              _F2731 |= 0x80;
          }
          F = (_F2731 & 0xFF);
          MEMPTR = _address2730;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_15(int opcode, int displacement) {
    switch (opcode) {
      case 0x78: {
          int _F2734;
          int _value2733;
          int _address2733;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5710;
          address_5710 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5711 = 14 | F & 1;
          _address2733 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5712 = read(_address2733, 0);
          contend1x1(_address2733);
          _value2733 = operand_5712;
          int value3_5715 = nAndCarry_5711;
          _F2734 = value3_5715 & 1;
          value3_5715 = value3_5715 >>> 1;
          _F2734 = (_F2734 & 1) | 0x10 | (address_5710 & 0x28);
          if ((_value2733 & (0x01 << value3_5715)) == 0) {
              _F2734 |= 0x44;
          }
          if (value3_5715 == 7 && (_value2733 & 0x80) != 0) {
              _F2734 |= 0x80;
          }
          F = (_F2734 & 0xFF);
          MEMPTR = _address2733;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x79: {
          int _F2737;
          int _value2736;
          int _address2736;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5717;
          address_5717 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5718 = 14 | F & 1;
          _address2736 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5719 = read(_address2736, 0);
          contend1x1(_address2736);
          _value2736 = operand_5719;
          int value3_5722 = nAndCarry_5718;
          _F2737 = value3_5722 & 1;
          value3_5722 = value3_5722 >>> 1;
          _F2737 = (_F2737 & 1) | 0x10 | (address_5717 & 0x28);
          if ((_value2736 & (0x01 << value3_5722)) == 0) {
              _F2737 |= 0x44;
          }
          if (value3_5722 == 7 && (_value2736 & 0x80) != 0) {
              _F2737 |= 0x80;
          }
          F = (_F2737 & 0xFF);
          MEMPTR = _address2736;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7A: {
          int _F2740;
          int _value2739;
          int _address2739;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5724;
          address_5724 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5725 = 14 | F & 1;
          _address2739 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5726 = read(_address2739, 0);
          contend1x1(_address2739);
          _value2739 = operand_5726;
          int value3_5729 = nAndCarry_5725;
          _F2740 = value3_5729 & 1;
          value3_5729 = value3_5729 >>> 1;
          _F2740 = (_F2740 & 1) | 0x10 | (address_5724 & 0x28);
          if ((_value2739 & (0x01 << value3_5729)) == 0) {
              _F2740 |= 0x44;
          }
          if (value3_5729 == 7 && (_value2739 & 0x80) != 0) {
              _F2740 |= 0x80;
          }
          F = (_F2740 & 0xFF);
          MEMPTR = _address2739;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7B: {
          int _F2743;
          int _value2742;
          int _address2742;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5731;
          address_5731 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5732 = 14 | F & 1;
          _address2742 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5733 = read(_address2742, 0);
          contend1x1(_address2742);
          _value2742 = operand_5733;
          int value3_5736 = nAndCarry_5732;
          _F2743 = value3_5736 & 1;
          value3_5736 = value3_5736 >>> 1;
          _F2743 = (_F2743 & 1) | 0x10 | (address_5731 & 0x28);
          if ((_value2742 & (0x01 << value3_5736)) == 0) {
              _F2743 |= 0x44;
          }
          if (value3_5736 == 7 && (_value2742 & 0x80) != 0) {
              _F2743 |= 0x80;
          }
          F = (_F2743 & 0xFF);
          MEMPTR = _address2742;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7C: {
          int _F2746;
          int _value2745;
          int _address2745;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5738;
          address_5738 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5739 = 14 | F & 1;
          _address2745 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5740 = read(_address2745, 0);
          contend1x1(_address2745);
          _value2745 = operand_5740;
          int value3_5743 = nAndCarry_5739;
          _F2746 = value3_5743 & 1;
          value3_5743 = value3_5743 >>> 1;
          _F2746 = (_F2746 & 1) | 0x10 | (address_5738 & 0x28);
          if ((_value2745 & (0x01 << value3_5743)) == 0) {
              _F2746 |= 0x44;
          }
          if (value3_5743 == 7 && (_value2745 & 0x80) != 0) {
              _F2746 |= 0x80;
          }
          F = (_F2746 & 0xFF);
          MEMPTR = _address2745;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7D: {
          int _F2749;
          int _value2748;
          int _address2748;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5745;
          address_5745 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5746 = 14 | F & 1;
          _address2748 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5747 = read(_address2748, 0);
          contend1x1(_address2748);
          _value2748 = operand_5747;
          int value3_5750 = nAndCarry_5746;
          _F2749 = value3_5750 & 1;
          value3_5750 = value3_5750 >>> 1;
          _F2749 = (_F2749 & 1) | 0x10 | (address_5745 & 0x28);
          if ((_value2748 & (0x01 << value3_5750)) == 0) {
              _F2749 |= 0x44;
          }
          if (value3_5750 == 7 && (_value2748 & 0x80) != 0) {
              _F2749 |= 0x80;
          }
          F = (_F2749 & 0xFF);
          MEMPTR = _address2748;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7E: {
          int _F2752;
          int _value2751;
          int _address2751;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5752;
          address_5752 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5753 = 14 | F & 1;
          _address2751 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5754 = read(_address2751, 0);
          contend1x1(_address2751);
          _value2751 = operand_5754;
          int value3_5757 = nAndCarry_5753;
          _F2752 = value3_5757 & 1;
          value3_5757 = value3_5757 >>> 1;
          _F2752 = (_F2752 & 1) | 0x10 | (address_5752 & 0x28);
          if ((_value2751 & (0x01 << value3_5757)) == 0) {
              _F2752 |= 0x44;
          }
          if (value3_5757 == 7 && (_value2751 & 0x80) != 0) {
              _F2752 |= 0x80;
          }
          F = (_F2752 & 0xFF);
          MEMPTR = _address2751;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7F: {
          int _F2755;
          int _value2754;
          int _address2754;
          contend2x1((PC + 3) & 0xFFFF);
          int address_5759;
          address_5759 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5760 = 14 | F & 1;
          _address2754 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5761 = read(_address2754, 0);
          contend1x1(_address2754);
          _value2754 = operand_5761;
          int value3_5764 = nAndCarry_5760;
          _F2755 = value3_5764 & 1;
          value3_5764 = value3_5764 >>> 1;
          _F2755 = (_F2755 & 1) | 0x10 | (address_5759 & 0x28);
          if ((_value2754 & (0x01 << value3_5764)) == 0) {
              _F2755 |= 0x44;
          }
          if (value3_5764 == 7 && (_value2754 & 0x80) != 0) {
              _F2755 |= 0x80;
          }
          F = (_F2755 & 0xFF);
          MEMPTR = _address2754;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_16(int opcode, int displacement) {
    switch (opcode) {
      case 0x80: {
          int _value2757;
          int _address2757;
          contend2x1((PC + 3) & 0xFFFF);
          _address2757 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5766 = read(_address2757, 0);
          contend1x1(_address2757);
          _value2757 = operand_5766;
          int value_5767 = (_value2757 & -2);
          _address2757 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2757 = value_5767;
          write(_address2757, value_5767);
          int read_5768;
          read_5768 = _value2757;
          B = read_5768;
          MEMPTR = _address2757;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _value2759;
          int _address2759;
          contend2x1((PC + 3) & 0xFFFF);
          _address2759 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5769 = read(_address2759, 0);
          contend1x1(_address2759);
          _value2759 = operand_5769;
          int value_5770 = (_value2759 & -2);
          _address2759 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2759 = value_5770;
          write(_address2759, value_5770);
          int read_5771;
          read_5771 = _value2759;
          C = read_5771;
          MEMPTR = _address2759;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _value2761;
          int _address2761;
          contend2x1((PC + 3) & 0xFFFF);
          _address2761 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5772 = read(_address2761, 0);
          contend1x1(_address2761);
          _value2761 = operand_5772;
          int value_5773 = (_value2761 & -2);
          _address2761 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2761 = value_5773;
          write(_address2761, value_5773);
          int read_5774;
          read_5774 = _value2761;
          D = read_5774;
          MEMPTR = _address2761;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _value2763;
          int _address2763;
          contend2x1((PC + 3) & 0xFFFF);
          _address2763 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5775 = read(_address2763, 0);
          contend1x1(_address2763);
          _value2763 = operand_5775;
          int value_5776 = (_value2763 & -2);
          _address2763 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2763 = value_5776;
          write(_address2763, value_5776);
          int read_5777;
          read_5777 = _value2763;
          E = read_5777;
          MEMPTR = _address2763;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _value2765;
          int _address2765;
          contend2x1((PC + 3) & 0xFFFF);
          _address2765 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5778 = read(_address2765, 0);
          contend1x1(_address2765);
          _value2765 = operand_5778;
          int value_5779 = (_value2765 & -2);
          _address2765 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2765 = value_5779;
          write(_address2765, value_5779);
          int read_5780;
          read_5780 = _value2765;
          H = read_5780;
          MEMPTR = _address2765;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _value2767;
          int _address2767;
          contend2x1((PC + 3) & 0xFFFF);
          _address2767 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5781 = read(_address2767, 0);
          contend1x1(_address2767);
          _value2767 = operand_5781;
          int value_5782 = (_value2767 & -2);
          _address2767 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2767 = value_5782;
          write(_address2767, value_5782);
          int read_5783;
          read_5783 = _value2767;
          L = read_5783;
          MEMPTR = _address2767;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _value2769;
          int _address2769;
          contend2x1((PC + 3) & 0xFFFF);
          _address2769 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5784 = read(_address2769, 0);
          contend1x1(_address2769);
          _value2769 = operand_5784;
          int value_5785 = (_value2769 & -2);
          _address2769 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2769 = value_5785;
          write(_address2769, value_5785);
          MEMPTR = _address2769;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _value2771;
          int _address2771;
          contend2x1((PC + 3) & 0xFFFF);
          _address2771 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5786 = read(_address2771, 0);
          contend1x1(_address2771);
          _value2771 = operand_5786;
          int value_5787 = (_value2771 & -2);
          _address2771 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2771 = value_5787;
          write(_address2771, value_5787);
          int read_5788;
          read_5788 = _value2771;
          A = read_5788;
          MEMPTR = _address2771;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_17(int opcode, int displacement) {
    switch (opcode) {
      case 0x88: {
          int _value2773;
          int _address2773;
          contend2x1((PC + 3) & 0xFFFF);
          _address2773 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5789 = read(_address2773, 0);
          contend1x1(_address2773);
          _value2773 = operand_5789;
          int value_5790 = (_value2773 & -3);
          _address2773 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2773 = value_5790;
          write(_address2773, value_5790);
          int read_5791;
          read_5791 = _value2773;
          B = read_5791;
          MEMPTR = _address2773;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _value2775;
          int _address2775;
          contend2x1((PC + 3) & 0xFFFF);
          _address2775 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5792 = read(_address2775, 0);
          contend1x1(_address2775);
          _value2775 = operand_5792;
          int value_5793 = (_value2775 & -3);
          _address2775 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2775 = value_5793;
          write(_address2775, value_5793);
          int read_5794;
          read_5794 = _value2775;
          C = read_5794;
          MEMPTR = _address2775;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _value2777;
          int _address2777;
          contend2x1((PC + 3) & 0xFFFF);
          _address2777 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5795 = read(_address2777, 0);
          contend1x1(_address2777);
          _value2777 = operand_5795;
          int value_5796 = (_value2777 & -3);
          _address2777 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2777 = value_5796;
          write(_address2777, value_5796);
          int read_5797;
          read_5797 = _value2777;
          D = read_5797;
          MEMPTR = _address2777;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _value2779;
          int _address2779;
          contend2x1((PC + 3) & 0xFFFF);
          _address2779 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5798 = read(_address2779, 0);
          contend1x1(_address2779);
          _value2779 = operand_5798;
          int value_5799 = (_value2779 & -3);
          _address2779 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2779 = value_5799;
          write(_address2779, value_5799);
          int read_5800;
          read_5800 = _value2779;
          E = read_5800;
          MEMPTR = _address2779;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _value2781;
          int _address2781;
          contend2x1((PC + 3) & 0xFFFF);
          _address2781 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5801 = read(_address2781, 0);
          contend1x1(_address2781);
          _value2781 = operand_5801;
          int value_5802 = (_value2781 & -3);
          _address2781 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2781 = value_5802;
          write(_address2781, value_5802);
          int read_5803;
          read_5803 = _value2781;
          H = read_5803;
          MEMPTR = _address2781;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _value2783;
          int _address2783;
          contend2x1((PC + 3) & 0xFFFF);
          _address2783 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5804 = read(_address2783, 0);
          contend1x1(_address2783);
          _value2783 = operand_5804;
          int value_5805 = (_value2783 & -3);
          _address2783 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2783 = value_5805;
          write(_address2783, value_5805);
          int read_5806;
          read_5806 = _value2783;
          L = read_5806;
          MEMPTR = _address2783;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _value2785;
          int _address2785;
          contend2x1((PC + 3) & 0xFFFF);
          _address2785 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5807 = read(_address2785, 0);
          contend1x1(_address2785);
          _value2785 = operand_5807;
          int value_5808 = (_value2785 & -3);
          _address2785 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2785 = value_5808;
          write(_address2785, value_5808);
          MEMPTR = _address2785;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _value2787;
          int _address2787;
          contend2x1((PC + 3) & 0xFFFF);
          _address2787 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5809 = read(_address2787, 0);
          contend1x1(_address2787);
          _value2787 = operand_5809;
          int value_5810 = (_value2787 & -3);
          _address2787 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2787 = value_5810;
          write(_address2787, value_5810);
          int read_5811;
          read_5811 = _value2787;
          A = read_5811;
          MEMPTR = _address2787;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_18(int opcode, int displacement) {
    switch (opcode) {
      case 0x90: {
          int _value2789;
          int _address2789;
          contend2x1((PC + 3) & 0xFFFF);
          _address2789 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5812 = read(_address2789, 0);
          contend1x1(_address2789);
          _value2789 = operand_5812;
          int value_5813 = (_value2789 & -5);
          _address2789 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2789 = value_5813;
          write(_address2789, value_5813);
          int read_5814;
          read_5814 = _value2789;
          B = read_5814;
          MEMPTR = _address2789;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _value2791;
          int _address2791;
          contend2x1((PC + 3) & 0xFFFF);
          _address2791 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5815 = read(_address2791, 0);
          contend1x1(_address2791);
          _value2791 = operand_5815;
          int value_5816 = (_value2791 & -5);
          _address2791 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2791 = value_5816;
          write(_address2791, value_5816);
          int read_5817;
          read_5817 = _value2791;
          C = read_5817;
          MEMPTR = _address2791;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _value2793;
          int _address2793;
          contend2x1((PC + 3) & 0xFFFF);
          _address2793 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5818 = read(_address2793, 0);
          contend1x1(_address2793);
          _value2793 = operand_5818;
          int value_5819 = (_value2793 & -5);
          _address2793 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2793 = value_5819;
          write(_address2793, value_5819);
          int read_5820;
          read_5820 = _value2793;
          D = read_5820;
          MEMPTR = _address2793;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _value2795;
          int _address2795;
          contend2x1((PC + 3) & 0xFFFF);
          _address2795 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5821 = read(_address2795, 0);
          contend1x1(_address2795);
          _value2795 = operand_5821;
          int value_5822 = (_value2795 & -5);
          _address2795 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2795 = value_5822;
          write(_address2795, value_5822);
          int read_5823;
          read_5823 = _value2795;
          E = read_5823;
          MEMPTR = _address2795;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _value2797;
          int _address2797;
          contend2x1((PC + 3) & 0xFFFF);
          _address2797 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5824 = read(_address2797, 0);
          contend1x1(_address2797);
          _value2797 = operand_5824;
          int value_5825 = (_value2797 & -5);
          _address2797 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2797 = value_5825;
          write(_address2797, value_5825);
          int read_5826;
          read_5826 = _value2797;
          H = read_5826;
          MEMPTR = _address2797;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _value2799;
          int _address2799;
          contend2x1((PC + 3) & 0xFFFF);
          _address2799 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5827 = read(_address2799, 0);
          contend1x1(_address2799);
          _value2799 = operand_5827;
          int value_5828 = (_value2799 & -5);
          _address2799 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2799 = value_5828;
          write(_address2799, value_5828);
          int read_5829;
          read_5829 = _value2799;
          L = read_5829;
          MEMPTR = _address2799;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _value2801;
          int _address2801;
          contend2x1((PC + 3) & 0xFFFF);
          _address2801 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5830 = read(_address2801, 0);
          contend1x1(_address2801);
          _value2801 = operand_5830;
          int value_5831 = (_value2801 & -5);
          _address2801 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2801 = value_5831;
          write(_address2801, value_5831);
          MEMPTR = _address2801;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _value2803;
          int _address2803;
          contend2x1((PC + 3) & 0xFFFF);
          _address2803 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5832 = read(_address2803, 0);
          contend1x1(_address2803);
          _value2803 = operand_5832;
          int value_5833 = (_value2803 & -5);
          _address2803 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2803 = value_5833;
          write(_address2803, value_5833);
          int read_5834;
          read_5834 = _value2803;
          A = read_5834;
          MEMPTR = _address2803;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_19(int opcode, int displacement) {
    switch (opcode) {
      case 0x98: {
          int _value2805;
          int _address2805;
          contend2x1((PC + 3) & 0xFFFF);
          _address2805 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5835 = read(_address2805, 0);
          contend1x1(_address2805);
          _value2805 = operand_5835;
          int value_5836 = (_value2805 & -9);
          _address2805 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2805 = value_5836;
          write(_address2805, value_5836);
          int read_5837;
          read_5837 = _value2805;
          B = read_5837;
          MEMPTR = _address2805;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _value2807;
          int _address2807;
          contend2x1((PC + 3) & 0xFFFF);
          _address2807 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5838 = read(_address2807, 0);
          contend1x1(_address2807);
          _value2807 = operand_5838;
          int value_5839 = (_value2807 & -9);
          _address2807 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2807 = value_5839;
          write(_address2807, value_5839);
          int read_5840;
          read_5840 = _value2807;
          C = read_5840;
          MEMPTR = _address2807;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _value2809;
          int _address2809;
          contend2x1((PC + 3) & 0xFFFF);
          _address2809 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5841 = read(_address2809, 0);
          contend1x1(_address2809);
          _value2809 = operand_5841;
          int value_5842 = (_value2809 & -9);
          _address2809 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2809 = value_5842;
          write(_address2809, value_5842);
          int read_5843;
          read_5843 = _value2809;
          D = read_5843;
          MEMPTR = _address2809;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _value2811;
          int _address2811;
          contend2x1((PC + 3) & 0xFFFF);
          _address2811 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5844 = read(_address2811, 0);
          contend1x1(_address2811);
          _value2811 = operand_5844;
          int value_5845 = (_value2811 & -9);
          _address2811 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2811 = value_5845;
          write(_address2811, value_5845);
          int read_5846;
          read_5846 = _value2811;
          E = read_5846;
          MEMPTR = _address2811;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _value2813;
          int _address2813;
          contend2x1((PC + 3) & 0xFFFF);
          _address2813 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5847 = read(_address2813, 0);
          contend1x1(_address2813);
          _value2813 = operand_5847;
          int value_5848 = (_value2813 & -9);
          _address2813 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2813 = value_5848;
          write(_address2813, value_5848);
          int read_5849;
          read_5849 = _value2813;
          H = read_5849;
          MEMPTR = _address2813;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _value2815;
          int _address2815;
          contend2x1((PC + 3) & 0xFFFF);
          _address2815 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5850 = read(_address2815, 0);
          contend1x1(_address2815);
          _value2815 = operand_5850;
          int value_5851 = (_value2815 & -9);
          _address2815 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2815 = value_5851;
          write(_address2815, value_5851);
          int read_5852;
          read_5852 = _value2815;
          L = read_5852;
          MEMPTR = _address2815;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _value2817;
          int _address2817;
          contend2x1((PC + 3) & 0xFFFF);
          _address2817 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5853 = read(_address2817, 0);
          contend1x1(_address2817);
          _value2817 = operand_5853;
          int value_5854 = (_value2817 & -9);
          _address2817 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2817 = value_5854;
          write(_address2817, value_5854);
          MEMPTR = _address2817;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _value2819;
          int _address2819;
          contend2x1((PC + 3) & 0xFFFF);
          _address2819 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5855 = read(_address2819, 0);
          contend1x1(_address2819);
          _value2819 = operand_5855;
          int value_5856 = (_value2819 & -9);
          _address2819 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2819 = value_5856;
          write(_address2819, value_5856);
          int read_5857;
          read_5857 = _value2819;
          A = read_5857;
          MEMPTR = _address2819;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_20(int opcode, int displacement) {
    switch (opcode) {
      case 0xA0: {
          int _value2821;
          int _address2821;
          contend2x1((PC + 3) & 0xFFFF);
          _address2821 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5858 = read(_address2821, 0);
          contend1x1(_address2821);
          _value2821 = operand_5858;
          int value_5859 = (_value2821 & -17);
          _address2821 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2821 = value_5859;
          write(_address2821, value_5859);
          int read_5860;
          read_5860 = _value2821;
          B = read_5860;
          MEMPTR = _address2821;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _value2823;
          int _address2823;
          contend2x1((PC + 3) & 0xFFFF);
          _address2823 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5861 = read(_address2823, 0);
          contend1x1(_address2823);
          _value2823 = operand_5861;
          int value_5862 = (_value2823 & -17);
          _address2823 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2823 = value_5862;
          write(_address2823, value_5862);
          int read_5863;
          read_5863 = _value2823;
          C = read_5863;
          MEMPTR = _address2823;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _value2825;
          int _address2825;
          contend2x1((PC + 3) & 0xFFFF);
          _address2825 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5864 = read(_address2825, 0);
          contend1x1(_address2825);
          _value2825 = operand_5864;
          int value_5865 = (_value2825 & -17);
          _address2825 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2825 = value_5865;
          write(_address2825, value_5865);
          int read_5866;
          read_5866 = _value2825;
          D = read_5866;
          MEMPTR = _address2825;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _value2827;
          int _address2827;
          contend2x1((PC + 3) & 0xFFFF);
          _address2827 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5867 = read(_address2827, 0);
          contend1x1(_address2827);
          _value2827 = operand_5867;
          int value_5868 = (_value2827 & -17);
          _address2827 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2827 = value_5868;
          write(_address2827, value_5868);
          int read_5869;
          read_5869 = _value2827;
          E = read_5869;
          MEMPTR = _address2827;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _value2829;
          int _address2829;
          contend2x1((PC + 3) & 0xFFFF);
          _address2829 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5870 = read(_address2829, 0);
          contend1x1(_address2829);
          _value2829 = operand_5870;
          int value_5871 = (_value2829 & -17);
          _address2829 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2829 = value_5871;
          write(_address2829, value_5871);
          int read_5872;
          read_5872 = _value2829;
          H = read_5872;
          MEMPTR = _address2829;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _value2831;
          int _address2831;
          contend2x1((PC + 3) & 0xFFFF);
          _address2831 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5873 = read(_address2831, 0);
          contend1x1(_address2831);
          _value2831 = operand_5873;
          int value_5874 = (_value2831 & -17);
          _address2831 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2831 = value_5874;
          write(_address2831, value_5874);
          int read_5875;
          read_5875 = _value2831;
          L = read_5875;
          MEMPTR = _address2831;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _value2833;
          int _address2833;
          contend2x1((PC + 3) & 0xFFFF);
          _address2833 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5876 = read(_address2833, 0);
          contend1x1(_address2833);
          _value2833 = operand_5876;
          int value_5877 = (_value2833 & -17);
          _address2833 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2833 = value_5877;
          write(_address2833, value_5877);
          MEMPTR = _address2833;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _value2835;
          int _address2835;
          contend2x1((PC + 3) & 0xFFFF);
          _address2835 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5878 = read(_address2835, 0);
          contend1x1(_address2835);
          _value2835 = operand_5878;
          int value_5879 = (_value2835 & -17);
          _address2835 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2835 = value_5879;
          write(_address2835, value_5879);
          int read_5880;
          read_5880 = _value2835;
          A = read_5880;
          MEMPTR = _address2835;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_21(int opcode, int displacement) {
    switch (opcode) {
      case 0xA8: {
          int _value2837;
          int _address2837;
          contend2x1((PC + 3) & 0xFFFF);
          _address2837 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5881 = read(_address2837, 0);
          contend1x1(_address2837);
          _value2837 = operand_5881;
          int value_5882 = (_value2837 & -33);
          _address2837 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2837 = value_5882;
          write(_address2837, value_5882);
          int read_5883;
          read_5883 = _value2837;
          B = read_5883;
          MEMPTR = _address2837;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _value2839;
          int _address2839;
          contend2x1((PC + 3) & 0xFFFF);
          _address2839 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5884 = read(_address2839, 0);
          contend1x1(_address2839);
          _value2839 = operand_5884;
          int value_5885 = (_value2839 & -33);
          _address2839 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2839 = value_5885;
          write(_address2839, value_5885);
          int read_5886;
          read_5886 = _value2839;
          C = read_5886;
          MEMPTR = _address2839;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _value2841;
          int _address2841;
          contend2x1((PC + 3) & 0xFFFF);
          _address2841 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5887 = read(_address2841, 0);
          contend1x1(_address2841);
          _value2841 = operand_5887;
          int value_5888 = (_value2841 & -33);
          _address2841 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2841 = value_5888;
          write(_address2841, value_5888);
          int read_5889;
          read_5889 = _value2841;
          D = read_5889;
          MEMPTR = _address2841;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _value2843;
          int _address2843;
          contend2x1((PC + 3) & 0xFFFF);
          _address2843 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5890 = read(_address2843, 0);
          contend1x1(_address2843);
          _value2843 = operand_5890;
          int value_5891 = (_value2843 & -33);
          _address2843 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2843 = value_5891;
          write(_address2843, value_5891);
          int read_5892;
          read_5892 = _value2843;
          E = read_5892;
          MEMPTR = _address2843;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _value2845;
          int _address2845;
          contend2x1((PC + 3) & 0xFFFF);
          _address2845 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5893 = read(_address2845, 0);
          contend1x1(_address2845);
          _value2845 = operand_5893;
          int value_5894 = (_value2845 & -33);
          _address2845 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2845 = value_5894;
          write(_address2845, value_5894);
          int read_5895;
          read_5895 = _value2845;
          H = read_5895;
          MEMPTR = _address2845;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _value2847;
          int _address2847;
          contend2x1((PC + 3) & 0xFFFF);
          _address2847 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5896 = read(_address2847, 0);
          contend1x1(_address2847);
          _value2847 = operand_5896;
          int value_5897 = (_value2847 & -33);
          _address2847 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2847 = value_5897;
          write(_address2847, value_5897);
          int read_5898;
          read_5898 = _value2847;
          L = read_5898;
          MEMPTR = _address2847;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _value2849;
          int _address2849;
          contend2x1((PC + 3) & 0xFFFF);
          _address2849 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5899 = read(_address2849, 0);
          contend1x1(_address2849);
          _value2849 = operand_5899;
          int value_5900 = (_value2849 & -33);
          _address2849 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2849 = value_5900;
          write(_address2849, value_5900);
          MEMPTR = _address2849;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _value2851;
          int _address2851;
          contend2x1((PC + 3) & 0xFFFF);
          _address2851 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5901 = read(_address2851, 0);
          contend1x1(_address2851);
          _value2851 = operand_5901;
          int value_5902 = (_value2851 & -33);
          _address2851 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2851 = value_5902;
          write(_address2851, value_5902);
          int read_5903;
          read_5903 = _value2851;
          A = read_5903;
          MEMPTR = _address2851;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_22(int opcode, int displacement) {
    switch (opcode) {
      case 0xB0: {
          int _value2853;
          int _address2853;
          contend2x1((PC + 3) & 0xFFFF);
          _address2853 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5904 = read(_address2853, 0);
          contend1x1(_address2853);
          _value2853 = operand_5904;
          int value_5905 = (_value2853 & -65);
          _address2853 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2853 = value_5905;
          write(_address2853, value_5905);
          int read_5906;
          read_5906 = _value2853;
          B = read_5906;
          MEMPTR = _address2853;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _value2855;
          int _address2855;
          contend2x1((PC + 3) & 0xFFFF);
          _address2855 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5907 = read(_address2855, 0);
          contend1x1(_address2855);
          _value2855 = operand_5907;
          int value_5908 = (_value2855 & -65);
          _address2855 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2855 = value_5908;
          write(_address2855, value_5908);
          int read_5909;
          read_5909 = _value2855;
          C = read_5909;
          MEMPTR = _address2855;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _value2857;
          int _address2857;
          contend2x1((PC + 3) & 0xFFFF);
          _address2857 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5910 = read(_address2857, 0);
          contend1x1(_address2857);
          _value2857 = operand_5910;
          int value_5911 = (_value2857 & -65);
          _address2857 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2857 = value_5911;
          write(_address2857, value_5911);
          int read_5912;
          read_5912 = _value2857;
          D = read_5912;
          MEMPTR = _address2857;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _value2859;
          int _address2859;
          contend2x1((PC + 3) & 0xFFFF);
          _address2859 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5913 = read(_address2859, 0);
          contend1x1(_address2859);
          _value2859 = operand_5913;
          int value_5914 = (_value2859 & -65);
          _address2859 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2859 = value_5914;
          write(_address2859, value_5914);
          int read_5915;
          read_5915 = _value2859;
          E = read_5915;
          MEMPTR = _address2859;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _value2861;
          int _address2861;
          contend2x1((PC + 3) & 0xFFFF);
          _address2861 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5916 = read(_address2861, 0);
          contend1x1(_address2861);
          _value2861 = operand_5916;
          int value_5917 = (_value2861 & -65);
          _address2861 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2861 = value_5917;
          write(_address2861, value_5917);
          int read_5918;
          read_5918 = _value2861;
          H = read_5918;
          MEMPTR = _address2861;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _value2863;
          int _address2863;
          contend2x1((PC + 3) & 0xFFFF);
          _address2863 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5919 = read(_address2863, 0);
          contend1x1(_address2863);
          _value2863 = operand_5919;
          int value_5920 = (_value2863 & -65);
          _address2863 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2863 = value_5920;
          write(_address2863, value_5920);
          int read_5921;
          read_5921 = _value2863;
          L = read_5921;
          MEMPTR = _address2863;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _value2865;
          int _address2865;
          contend2x1((PC + 3) & 0xFFFF);
          _address2865 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5922 = read(_address2865, 0);
          contend1x1(_address2865);
          _value2865 = operand_5922;
          int value_5923 = (_value2865 & -65);
          _address2865 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2865 = value_5923;
          write(_address2865, value_5923);
          MEMPTR = _address2865;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _value2867;
          int _address2867;
          contend2x1((PC + 3) & 0xFFFF);
          _address2867 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5924 = read(_address2867, 0);
          contend1x1(_address2867);
          _value2867 = operand_5924;
          int value_5925 = (_value2867 & -65);
          _address2867 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2867 = value_5925;
          write(_address2867, value_5925);
          int read_5926;
          read_5926 = _value2867;
          A = read_5926;
          MEMPTR = _address2867;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_23(int opcode, int displacement) {
    switch (opcode) {
      case 0xB8: {
          int _value2869;
          int _address2869;
          contend2x1((PC + 3) & 0xFFFF);
          _address2869 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5927 = read(_address2869, 0);
          contend1x1(_address2869);
          _value2869 = operand_5927;
          int value_5928 = (_value2869 & -129);
          _address2869 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2869 = value_5928;
          write(_address2869, value_5928);
          int read_5929;
          read_5929 = _value2869;
          B = read_5929;
          MEMPTR = _address2869;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _value2871;
          int _address2871;
          contend2x1((PC + 3) & 0xFFFF);
          _address2871 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5930 = read(_address2871, 0);
          contend1x1(_address2871);
          _value2871 = operand_5930;
          int value_5931 = (_value2871 & -129);
          _address2871 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2871 = value_5931;
          write(_address2871, value_5931);
          int read_5932;
          read_5932 = _value2871;
          C = read_5932;
          MEMPTR = _address2871;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _value2873;
          int _address2873;
          contend2x1((PC + 3) & 0xFFFF);
          _address2873 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5933 = read(_address2873, 0);
          contend1x1(_address2873);
          _value2873 = operand_5933;
          int value_5934 = (_value2873 & -129);
          _address2873 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2873 = value_5934;
          write(_address2873, value_5934);
          int read_5935;
          read_5935 = _value2873;
          D = read_5935;
          MEMPTR = _address2873;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _value2875;
          int _address2875;
          contend2x1((PC + 3) & 0xFFFF);
          _address2875 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5936 = read(_address2875, 0);
          contend1x1(_address2875);
          _value2875 = operand_5936;
          int value_5937 = (_value2875 & -129);
          _address2875 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2875 = value_5937;
          write(_address2875, value_5937);
          int read_5938;
          read_5938 = _value2875;
          E = read_5938;
          MEMPTR = _address2875;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _value2877;
          int _address2877;
          contend2x1((PC + 3) & 0xFFFF);
          _address2877 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5939 = read(_address2877, 0);
          contend1x1(_address2877);
          _value2877 = operand_5939;
          int value_5940 = (_value2877 & -129);
          _address2877 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2877 = value_5940;
          write(_address2877, value_5940);
          int read_5941;
          read_5941 = _value2877;
          H = read_5941;
          MEMPTR = _address2877;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _value2879;
          int _address2879;
          contend2x1((PC + 3) & 0xFFFF);
          _address2879 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5942 = read(_address2879, 0);
          contend1x1(_address2879);
          _value2879 = operand_5942;
          int value_5943 = (_value2879 & -129);
          _address2879 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2879 = value_5943;
          write(_address2879, value_5943);
          int read_5944;
          read_5944 = _value2879;
          L = read_5944;
          MEMPTR = _address2879;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _value2881;
          int _address2881;
          contend2x1((PC + 3) & 0xFFFF);
          _address2881 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5945 = read(_address2881, 0);
          contend1x1(_address2881);
          _value2881 = operand_5945;
          int value_5946 = (_value2881 & -129);
          _address2881 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2881 = value_5946;
          write(_address2881, value_5946);
          MEMPTR = _address2881;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _value2883;
          int _address2883;
          contend2x1((PC + 3) & 0xFFFF);
          _address2883 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5947 = read(_address2883, 0);
          contend1x1(_address2883);
          _value2883 = operand_5947;
          int value_5948 = (_value2883 & -129);
          _address2883 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2883 = value_5948;
          write(_address2883, value_5948);
          int read_5949;
          read_5949 = _value2883;
          A = read_5949;
          MEMPTR = _address2883;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_24(int opcode, int displacement) {
    switch (opcode) {
      case 0xC0: {
          int _value2885;
          int _address2885;
          contend2x1((PC + 3) & 0xFFFF);
          _address2885 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5950 = read(_address2885, 0);
          contend1x1(_address2885);
          _value2885 = operand_5950;
          int value_5951 = (_value2885 | 1);
          _address2885 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2885 = value_5951;
          write(_address2885, value_5951);
          int read_5952;
          read_5952 = _value2885;
          B = read_5952;
          MEMPTR = _address2885;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC1: {
          int _value2887;
          int _address2887;
          contend2x1((PC + 3) & 0xFFFF);
          _address2887 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5953 = read(_address2887, 0);
          contend1x1(_address2887);
          _value2887 = operand_5953;
          int value_5954 = (_value2887 | 1);
          _address2887 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2887 = value_5954;
          write(_address2887, value_5954);
          int read_5955;
          read_5955 = _value2887;
          C = read_5955;
          MEMPTR = _address2887;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _value2889;
          int _address2889;
          contend2x1((PC + 3) & 0xFFFF);
          _address2889 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5956 = read(_address2889, 0);
          contend1x1(_address2889);
          _value2889 = operand_5956;
          int value_5957 = (_value2889 | 1);
          _address2889 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2889 = value_5957;
          write(_address2889, value_5957);
          int read_5958;
          read_5958 = _value2889;
          D = read_5958;
          MEMPTR = _address2889;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC3: {
          int _value2891;
          int _address2891;
          contend2x1((PC + 3) & 0xFFFF);
          _address2891 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5959 = read(_address2891, 0);
          contend1x1(_address2891);
          _value2891 = operand_5959;
          int value_5960 = (_value2891 | 1);
          _address2891 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2891 = value_5960;
          write(_address2891, value_5960);
          int read_5961;
          read_5961 = _value2891;
          E = read_5961;
          MEMPTR = _address2891;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC4: {
          int _value2893;
          int _address2893;
          contend2x1((PC + 3) & 0xFFFF);
          _address2893 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5962 = read(_address2893, 0);
          contend1x1(_address2893);
          _value2893 = operand_5962;
          int value_5963 = (_value2893 | 1);
          _address2893 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2893 = value_5963;
          write(_address2893, value_5963);
          int read_5964;
          read_5964 = _value2893;
          H = read_5964;
          MEMPTR = _address2893;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC5: {
          int _value2895;
          int _address2895;
          contend2x1((PC + 3) & 0xFFFF);
          _address2895 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5965 = read(_address2895, 0);
          contend1x1(_address2895);
          _value2895 = operand_5965;
          int value_5966 = (_value2895 | 1);
          _address2895 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2895 = value_5966;
          write(_address2895, value_5966);
          int read_5967;
          read_5967 = _value2895;
          L = read_5967;
          MEMPTR = _address2895;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _value2897;
          int _address2897;
          contend2x1((PC + 3) & 0xFFFF);
          _address2897 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5968 = read(_address2897, 0);
          contend1x1(_address2897);
          _value2897 = operand_5968;
          int value_5969 = (_value2897 | 1);
          _address2897 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2897 = value_5969;
          write(_address2897, value_5969);
          MEMPTR = _address2897;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _value2899;
          int _address2899;
          contend2x1((PC + 3) & 0xFFFF);
          _address2899 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5970 = read(_address2899, 0);
          contend1x1(_address2899);
          _value2899 = operand_5970;
          int value_5971 = (_value2899 | 1);
          _address2899 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2899 = value_5971;
          write(_address2899, value_5971);
          int read_5972;
          read_5972 = _value2899;
          A = read_5972;
          MEMPTR = _address2899;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_25(int opcode, int displacement) {
    switch (opcode) {
      case 0xC8: {
          int _value2901;
          int _address2901;
          contend2x1((PC + 3) & 0xFFFF);
          _address2901 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5973 = read(_address2901, 0);
          contend1x1(_address2901);
          _value2901 = operand_5973;
          int value_5974 = (_value2901 | 2);
          _address2901 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2901 = value_5974;
          write(_address2901, value_5974);
          int read_5975;
          read_5975 = _value2901;
          B = read_5975;
          MEMPTR = _address2901;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC9: {
          int _value2903;
          int _address2903;
          contend2x1((PC + 3) & 0xFFFF);
          _address2903 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5976 = read(_address2903, 0);
          contend1x1(_address2903);
          _value2903 = operand_5976;
          int value_5977 = (_value2903 | 2);
          _address2903 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2903 = value_5977;
          write(_address2903, value_5977);
          int read_5978;
          read_5978 = _value2903;
          C = read_5978;
          MEMPTR = _address2903;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCA: {
          int _value2905;
          int _address2905;
          contend2x1((PC + 3) & 0xFFFF);
          _address2905 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5979 = read(_address2905, 0);
          contend1x1(_address2905);
          _value2905 = operand_5979;
          int value_5980 = (_value2905 | 2);
          _address2905 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2905 = value_5980;
          write(_address2905, value_5980);
          int read_5981;
          read_5981 = _value2905;
          D = read_5981;
          MEMPTR = _address2905;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCB: {
          int _value2907;
          int _address2907;
          contend2x1((PC + 3) & 0xFFFF);
          _address2907 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5982 = read(_address2907, 0);
          contend1x1(_address2907);
          _value2907 = operand_5982;
          int value_5983 = (_value2907 | 2);
          _address2907 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2907 = value_5983;
          write(_address2907, value_5983);
          int read_5984;
          read_5984 = _value2907;
          E = read_5984;
          MEMPTR = _address2907;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCC: {
          int _value2909;
          int _address2909;
          contend2x1((PC + 3) & 0xFFFF);
          _address2909 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5985 = read(_address2909, 0);
          contend1x1(_address2909);
          _value2909 = operand_5985;
          int value_5986 = (_value2909 | 2);
          _address2909 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2909 = value_5986;
          write(_address2909, value_5986);
          int read_5987;
          read_5987 = _value2909;
          H = read_5987;
          MEMPTR = _address2909;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCD: {
          int _value2911;
          int _address2911;
          contend2x1((PC + 3) & 0xFFFF);
          _address2911 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5988 = read(_address2911, 0);
          contend1x1(_address2911);
          _value2911 = operand_5988;
          int value_5989 = (_value2911 | 2);
          _address2911 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2911 = value_5989;
          write(_address2911, value_5989);
          int read_5990;
          read_5990 = _value2911;
          L = read_5990;
          MEMPTR = _address2911;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCE: {
          int _value2913;
          int _address2913;
          contend2x1((PC + 3) & 0xFFFF);
          _address2913 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5991 = read(_address2913, 0);
          contend1x1(_address2913);
          _value2913 = operand_5991;
          int value_5992 = (_value2913 | 2);
          _address2913 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2913 = value_5992;
          write(_address2913, value_5992);
          MEMPTR = _address2913;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _value2915;
          int _address2915;
          contend2x1((PC + 3) & 0xFFFF);
          _address2915 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5993 = read(_address2915, 0);
          contend1x1(_address2915);
          _value2915 = operand_5993;
          int value_5994 = (_value2915 | 2);
          _address2915 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2915 = value_5994;
          write(_address2915, value_5994);
          int read_5995;
          read_5995 = _value2915;
          A = read_5995;
          MEMPTR = _address2915;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_26(int opcode, int displacement) {
    switch (opcode) {
      case 0xD0: {
          int _value2917;
          int _address2917;
          contend2x1((PC + 3) & 0xFFFF);
          _address2917 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5996 = read(_address2917, 0);
          contend1x1(_address2917);
          _value2917 = operand_5996;
          int value_5997 = (_value2917 | 4);
          _address2917 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2917 = value_5997;
          write(_address2917, value_5997);
          int read_5998;
          read_5998 = _value2917;
          B = read_5998;
          MEMPTR = _address2917;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD1: {
          int _value2919;
          int _address2919;
          contend2x1((PC + 3) & 0xFFFF);
          _address2919 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5999 = read(_address2919, 0);
          contend1x1(_address2919);
          _value2919 = operand_5999;
          int value_6000 = (_value2919 | 4);
          _address2919 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2919 = value_6000;
          write(_address2919, value_6000);
          int read_6001;
          read_6001 = _value2919;
          C = read_6001;
          MEMPTR = _address2919;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _value2921;
          int _address2921;
          contend2x1((PC + 3) & 0xFFFF);
          _address2921 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6002 = read(_address2921, 0);
          contend1x1(_address2921);
          _value2921 = operand_6002;
          int value_6003 = (_value2921 | 4);
          _address2921 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2921 = value_6003;
          write(_address2921, value_6003);
          int read_6004;
          read_6004 = _value2921;
          D = read_6004;
          MEMPTR = _address2921;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD3: {
          int _value2923;
          int _address2923;
          contend2x1((PC + 3) & 0xFFFF);
          _address2923 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6005 = read(_address2923, 0);
          contend1x1(_address2923);
          _value2923 = operand_6005;
          int value_6006 = (_value2923 | 4);
          _address2923 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2923 = value_6006;
          write(_address2923, value_6006);
          int read_6007;
          read_6007 = _value2923;
          E = read_6007;
          MEMPTR = _address2923;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _value2925;
          int _address2925;
          contend2x1((PC + 3) & 0xFFFF);
          _address2925 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6008 = read(_address2925, 0);
          contend1x1(_address2925);
          _value2925 = operand_6008;
          int value_6009 = (_value2925 | 4);
          _address2925 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2925 = value_6009;
          write(_address2925, value_6009);
          int read_6010;
          read_6010 = _value2925;
          H = read_6010;
          MEMPTR = _address2925;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD5: {
          int _value2927;
          int _address2927;
          contend2x1((PC + 3) & 0xFFFF);
          _address2927 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6011 = read(_address2927, 0);
          contend1x1(_address2927);
          _value2927 = operand_6011;
          int value_6012 = (_value2927 | 4);
          _address2927 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2927 = value_6012;
          write(_address2927, value_6012);
          int read_6013;
          read_6013 = _value2927;
          L = read_6013;
          MEMPTR = _address2927;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _value2929;
          int _address2929;
          contend2x1((PC + 3) & 0xFFFF);
          _address2929 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6014 = read(_address2929, 0);
          contend1x1(_address2929);
          _value2929 = operand_6014;
          int value_6015 = (_value2929 | 4);
          _address2929 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2929 = value_6015;
          write(_address2929, value_6015);
          MEMPTR = _address2929;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _value2931;
          int _address2931;
          contend2x1((PC + 3) & 0xFFFF);
          _address2931 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6016 = read(_address2931, 0);
          contend1x1(_address2931);
          _value2931 = operand_6016;
          int value_6017 = (_value2931 | 4);
          _address2931 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2931 = value_6017;
          write(_address2931, value_6017);
          int read_6018;
          read_6018 = _value2931;
          A = read_6018;
          MEMPTR = _address2931;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_27(int opcode, int displacement) {
    switch (opcode) {
      case 0xD8: {
          int _value2933;
          int _address2933;
          contend2x1((PC + 3) & 0xFFFF);
          _address2933 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6019 = read(_address2933, 0);
          contend1x1(_address2933);
          _value2933 = operand_6019;
          int value_6020 = (_value2933 | 8);
          _address2933 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2933 = value_6020;
          write(_address2933, value_6020);
          int read_6021;
          read_6021 = _value2933;
          B = read_6021;
          MEMPTR = _address2933;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD9: {
          int _value2935;
          int _address2935;
          contend2x1((PC + 3) & 0xFFFF);
          _address2935 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6022 = read(_address2935, 0);
          contend1x1(_address2935);
          _value2935 = operand_6022;
          int value_6023 = (_value2935 | 8);
          _address2935 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2935 = value_6023;
          write(_address2935, value_6023);
          int read_6024;
          read_6024 = _value2935;
          C = read_6024;
          MEMPTR = _address2935;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _value2937;
          int _address2937;
          contend2x1((PC + 3) & 0xFFFF);
          _address2937 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6025 = read(_address2937, 0);
          contend1x1(_address2937);
          _value2937 = operand_6025;
          int value_6026 = (_value2937 | 8);
          _address2937 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2937 = value_6026;
          write(_address2937, value_6026);
          int read_6027;
          read_6027 = _value2937;
          D = read_6027;
          MEMPTR = _address2937;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDB: {
          int _value2939;
          int _address2939;
          contend2x1((PC + 3) & 0xFFFF);
          _address2939 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6028 = read(_address2939, 0);
          contend1x1(_address2939);
          _value2939 = operand_6028;
          int value_6029 = (_value2939 | 8);
          _address2939 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2939 = value_6029;
          write(_address2939, value_6029);
          int read_6030;
          read_6030 = _value2939;
          E = read_6030;
          MEMPTR = _address2939;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _value2941;
          int _address2941;
          contend2x1((PC + 3) & 0xFFFF);
          _address2941 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6031 = read(_address2941, 0);
          contend1x1(_address2941);
          _value2941 = operand_6031;
          int value_6032 = (_value2941 | 8);
          _address2941 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2941 = value_6032;
          write(_address2941, value_6032);
          int read_6033;
          read_6033 = _value2941;
          H = read_6033;
          MEMPTR = _address2941;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDD: {
          int _value2943;
          int _address2943;
          contend2x1((PC + 3) & 0xFFFF);
          _address2943 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6034 = read(_address2943, 0);
          contend1x1(_address2943);
          _value2943 = operand_6034;
          int value_6035 = (_value2943 | 8);
          _address2943 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2943 = value_6035;
          write(_address2943, value_6035);
          int read_6036;
          read_6036 = _value2943;
          L = read_6036;
          MEMPTR = _address2943;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _value2945;
          int _address2945;
          contend2x1((PC + 3) & 0xFFFF);
          _address2945 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6037 = read(_address2945, 0);
          contend1x1(_address2945);
          _value2945 = operand_6037;
          int value_6038 = (_value2945 | 8);
          _address2945 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2945 = value_6038;
          write(_address2945, value_6038);
          MEMPTR = _address2945;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _value2947;
          int _address2947;
          contend2x1((PC + 3) & 0xFFFF);
          _address2947 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6039 = read(_address2947, 0);
          contend1x1(_address2947);
          _value2947 = operand_6039;
          int value_6040 = (_value2947 | 8);
          _address2947 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2947 = value_6040;
          write(_address2947, value_6040);
          int read_6041;
          read_6041 = _value2947;
          A = read_6041;
          MEMPTR = _address2947;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_28(int opcode, int displacement) {
    switch (opcode) {
      case 0xE0: {
          int _value2949;
          int _address2949;
          contend2x1((PC + 3) & 0xFFFF);
          _address2949 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6042 = read(_address2949, 0);
          contend1x1(_address2949);
          _value2949 = operand_6042;
          int value_6043 = (_value2949 | 0x10);
          _address2949 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2949 = value_6043;
          write(_address2949, value_6043);
          int read_6044;
          read_6044 = _value2949;
          B = read_6044;
          MEMPTR = _address2949;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE1: {
          int _value2951;
          int _address2951;
          contend2x1((PC + 3) & 0xFFFF);
          _address2951 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6045 = read(_address2951, 0);
          contend1x1(_address2951);
          _value2951 = operand_6045;
          int value_6046 = (_value2951 | 0x10);
          _address2951 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2951 = value_6046;
          write(_address2951, value_6046);
          int read_6047;
          read_6047 = _value2951;
          C = read_6047;
          MEMPTR = _address2951;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _value2953;
          int _address2953;
          contend2x1((PC + 3) & 0xFFFF);
          _address2953 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6048 = read(_address2953, 0);
          contend1x1(_address2953);
          _value2953 = operand_6048;
          int value_6049 = (_value2953 | 0x10);
          _address2953 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2953 = value_6049;
          write(_address2953, value_6049);
          int read_6050;
          read_6050 = _value2953;
          D = read_6050;
          MEMPTR = _address2953;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE3: {
          int _value2955;
          int _address2955;
          contend2x1((PC + 3) & 0xFFFF);
          _address2955 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6051 = read(_address2955, 0);
          contend1x1(_address2955);
          _value2955 = operand_6051;
          int value_6052 = (_value2955 | 0x10);
          _address2955 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2955 = value_6052;
          write(_address2955, value_6052);
          int read_6053;
          read_6053 = _value2955;
          E = read_6053;
          MEMPTR = _address2955;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _value2957;
          int _address2957;
          contend2x1((PC + 3) & 0xFFFF);
          _address2957 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6054 = read(_address2957, 0);
          contend1x1(_address2957);
          _value2957 = operand_6054;
          int value_6055 = (_value2957 | 0x10);
          _address2957 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2957 = value_6055;
          write(_address2957, value_6055);
          int read_6056;
          read_6056 = _value2957;
          H = read_6056;
          MEMPTR = _address2957;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE5: {
          int _value2959;
          int _address2959;
          contend2x1((PC + 3) & 0xFFFF);
          _address2959 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6057 = read(_address2959, 0);
          contend1x1(_address2959);
          _value2959 = operand_6057;
          int value_6058 = (_value2959 | 0x10);
          _address2959 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2959 = value_6058;
          write(_address2959, value_6058);
          int read_6059;
          read_6059 = _value2959;
          L = read_6059;
          MEMPTR = _address2959;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _value2961;
          int _address2961;
          contend2x1((PC + 3) & 0xFFFF);
          _address2961 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6060 = read(_address2961, 0);
          contend1x1(_address2961);
          _value2961 = operand_6060;
          int value_6061 = (_value2961 | 0x10);
          _address2961 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2961 = value_6061;
          write(_address2961, value_6061);
          MEMPTR = _address2961;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _value2963;
          int _address2963;
          contend2x1((PC + 3) & 0xFFFF);
          _address2963 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6062 = read(_address2963, 0);
          contend1x1(_address2963);
          _value2963 = operand_6062;
          int value_6063 = (_value2963 | 0x10);
          _address2963 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2963 = value_6063;
          write(_address2963, value_6063);
          int read_6064;
          read_6064 = _value2963;
          A = read_6064;
          MEMPTR = _address2963;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_29(int opcode, int displacement) {
    switch (opcode) {
      case 0xE8: {
          int _value2965;
          int _address2965;
          contend2x1((PC + 3) & 0xFFFF);
          _address2965 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6065 = read(_address2965, 0);
          contend1x1(_address2965);
          _value2965 = operand_6065;
          int value_6066 = (_value2965 | 0x20);
          _address2965 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2965 = value_6066;
          write(_address2965, value_6066);
          int read_6067;
          read_6067 = _value2965;
          B = read_6067;
          MEMPTR = _address2965;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE9: {
          int _value2967;
          int _address2967;
          contend2x1((PC + 3) & 0xFFFF);
          _address2967 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6068 = read(_address2967, 0);
          contend1x1(_address2967);
          _value2967 = operand_6068;
          int value_6069 = (_value2967 | 0x20);
          _address2967 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2967 = value_6069;
          write(_address2967, value_6069);
          int read_6070;
          read_6070 = _value2967;
          C = read_6070;
          MEMPTR = _address2967;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEA: {
          int _value2969;
          int _address2969;
          contend2x1((PC + 3) & 0xFFFF);
          _address2969 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6071 = read(_address2969, 0);
          contend1x1(_address2969);
          _value2969 = operand_6071;
          int value_6072 = (_value2969 | 0x20);
          _address2969 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2969 = value_6072;
          write(_address2969, value_6072);
          int read_6073;
          read_6073 = _value2969;
          D = read_6073;
          MEMPTR = _address2969;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEB: {
          int _value2971;
          int _address2971;
          contend2x1((PC + 3) & 0xFFFF);
          _address2971 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6074 = read(_address2971, 0);
          contend1x1(_address2971);
          _value2971 = operand_6074;
          int value_6075 = (_value2971 | 0x20);
          _address2971 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2971 = value_6075;
          write(_address2971, value_6075);
          int read_6076;
          read_6076 = _value2971;
          E = read_6076;
          MEMPTR = _address2971;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _value2973;
          int _address2973;
          contend2x1((PC + 3) & 0xFFFF);
          _address2973 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6077 = read(_address2973, 0);
          contend1x1(_address2973);
          _value2973 = operand_6077;
          int value_6078 = (_value2973 | 0x20);
          _address2973 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2973 = value_6078;
          write(_address2973, value_6078);
          int read_6079;
          read_6079 = _value2973;
          H = read_6079;
          MEMPTR = _address2973;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xED: {
          int _value2975;
          int _address2975;
          contend2x1((PC + 3) & 0xFFFF);
          _address2975 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6080 = read(_address2975, 0);
          contend1x1(_address2975);
          _value2975 = operand_6080;
          int value_6081 = (_value2975 | 0x20);
          _address2975 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2975 = value_6081;
          write(_address2975, value_6081);
          int read_6082;
          read_6082 = _value2975;
          L = read_6082;
          MEMPTR = _address2975;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _value2977;
          int _address2977;
          contend2x1((PC + 3) & 0xFFFF);
          _address2977 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6083 = read(_address2977, 0);
          contend1x1(_address2977);
          _value2977 = operand_6083;
          int value_6084 = (_value2977 | 0x20);
          _address2977 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2977 = value_6084;
          write(_address2977, value_6084);
          MEMPTR = _address2977;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _value2979;
          int _address2979;
          contend2x1((PC + 3) & 0xFFFF);
          _address2979 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6085 = read(_address2979, 0);
          contend1x1(_address2979);
          _value2979 = operand_6085;
          int value_6086 = (_value2979 | 0x20);
          _address2979 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2979 = value_6086;
          write(_address2979, value_6086);
          int read_6087;
          read_6087 = _value2979;
          A = read_6087;
          MEMPTR = _address2979;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_30(int opcode, int displacement) {
    switch (opcode) {
      case 0xF0: {
          int _value2981;
          int _address2981;
          contend2x1((PC + 3) & 0xFFFF);
          _address2981 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6088 = read(_address2981, 0);
          contend1x1(_address2981);
          _value2981 = operand_6088;
          int value_6089 = (_value2981 | 0x40);
          _address2981 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2981 = value_6089;
          write(_address2981, value_6089);
          int read_6090;
          read_6090 = _value2981;
          B = read_6090;
          MEMPTR = _address2981;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF1: {
          int _value2983;
          int _address2983;
          contend2x1((PC + 3) & 0xFFFF);
          _address2983 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6091 = read(_address2983, 0);
          contend1x1(_address2983);
          _value2983 = operand_6091;
          int value_6092 = (_value2983 | 0x40);
          _address2983 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2983 = value_6092;
          write(_address2983, value_6092);
          int read_6093;
          read_6093 = _value2983;
          C = read_6093;
          MEMPTR = _address2983;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _value2985;
          int _address2985;
          contend2x1((PC + 3) & 0xFFFF);
          _address2985 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6094 = read(_address2985, 0);
          contend1x1(_address2985);
          _value2985 = operand_6094;
          int value_6095 = (_value2985 | 0x40);
          _address2985 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2985 = value_6095;
          write(_address2985, value_6095);
          int read_6096;
          read_6096 = _value2985;
          D = read_6096;
          MEMPTR = _address2985;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF3: {
          int _value2987;
          int _address2987;
          contend2x1((PC + 3) & 0xFFFF);
          _address2987 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6097 = read(_address2987, 0);
          contend1x1(_address2987);
          _value2987 = operand_6097;
          int value_6098 = (_value2987 | 0x40);
          _address2987 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2987 = value_6098;
          write(_address2987, value_6098);
          int read_6099;
          read_6099 = _value2987;
          E = read_6099;
          MEMPTR = _address2987;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _value2989;
          int _address2989;
          contend2x1((PC + 3) & 0xFFFF);
          _address2989 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6100 = read(_address2989, 0);
          contend1x1(_address2989);
          _value2989 = operand_6100;
          int value_6101 = (_value2989 | 0x40);
          _address2989 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2989 = value_6101;
          write(_address2989, value_6101);
          int read_6102;
          read_6102 = _value2989;
          H = read_6102;
          MEMPTR = _address2989;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF5: {
          int _value2991;
          int _address2991;
          contend2x1((PC + 3) & 0xFFFF);
          _address2991 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6103 = read(_address2991, 0);
          contend1x1(_address2991);
          _value2991 = operand_6103;
          int value_6104 = (_value2991 | 0x40);
          _address2991 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2991 = value_6104;
          write(_address2991, value_6104);
          int read_6105;
          read_6105 = _value2991;
          L = read_6105;
          MEMPTR = _address2991;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _value2993;
          int _address2993;
          contend2x1((PC + 3) & 0xFFFF);
          _address2993 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6106 = read(_address2993, 0);
          contend1x1(_address2993);
          _value2993 = operand_6106;
          int value_6107 = (_value2993 | 0x40);
          _address2993 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2993 = value_6107;
          write(_address2993, value_6107);
          MEMPTR = _address2993;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _value2995;
          int _address2995;
          contend2x1((PC + 3) & 0xFFFF);
          _address2995 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6108 = read(_address2995, 0);
          contend1x1(_address2995);
          _value2995 = operand_6108;
          int value_6109 = (_value2995 | 0x40);
          _address2995 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2995 = value_6109;
          write(_address2995, value_6109);
          int read_6110;
          read_6110 = _value2995;
          A = read_6110;
          MEMPTR = _address2995;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

  private void decodeFDCB_31(int opcode, int displacement) {
    switch (opcode) {
      case 0xF8: {
          int _value2997;
          int _address2997;
          contend2x1((PC + 3) & 0xFFFF);
          _address2997 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6111 = read(_address2997, 0);
          contend1x1(_address2997);
          _value2997 = operand_6111;
          int value_6112 = (_value2997 | 0x80);
          _address2997 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2997 = value_6112;
          write(_address2997, value_6112);
          int read_6113;
          read_6113 = _value2997;
          B = read_6113;
          MEMPTR = _address2997;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF9: {
          int _value2999;
          int _address2999;
          contend2x1((PC + 3) & 0xFFFF);
          _address2999 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6114 = read(_address2999, 0);
          contend1x1(_address2999);
          _value2999 = operand_6114;
          int value_6115 = (_value2999 | 0x80);
          _address2999 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2999 = value_6115;
          write(_address2999, value_6115);
          int read_6116;
          read_6116 = _value2999;
          C = read_6116;
          MEMPTR = _address2999;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _value3001;
          int _address3001;
          contend2x1((PC + 3) & 0xFFFF);
          _address3001 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6117 = read(_address3001, 0);
          contend1x1(_address3001);
          _value3001 = operand_6117;
          int value_6118 = (_value3001 | 0x80);
          _address3001 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3001 = value_6118;
          write(_address3001, value_6118);
          int read_6119;
          read_6119 = _value3001;
          D = read_6119;
          MEMPTR = _address3001;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFB: {
          int _value3003;
          int _address3003;
          contend2x1((PC + 3) & 0xFFFF);
          _address3003 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6120 = read(_address3003, 0);
          contend1x1(_address3003);
          _value3003 = operand_6120;
          int value_6121 = (_value3003 | 0x80);
          _address3003 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3003 = value_6121;
          write(_address3003, value_6121);
          int read_6122;
          read_6122 = _value3003;
          E = read_6122;
          MEMPTR = _address3003;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _value3005;
          int _address3005;
          contend2x1((PC + 3) & 0xFFFF);
          _address3005 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6123 = read(_address3005, 0);
          contend1x1(_address3005);
          _value3005 = operand_6123;
          int value_6124 = (_value3005 | 0x80);
          _address3005 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3005 = value_6124;
          write(_address3005, value_6124);
          int read_6125;
          read_6125 = _value3005;
          H = read_6125;
          MEMPTR = _address3005;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFD: {
          int _value3007;
          int _address3007;
          contend2x1((PC + 3) & 0xFFFF);
          _address3007 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6126 = read(_address3007, 0);
          contend1x1(_address3007);
          _value3007 = operand_6126;
          int value_6127 = (_value3007 | 0x80);
          _address3007 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3007 = value_6127;
          write(_address3007, value_6127);
          int read_6128;
          read_6128 = _value3007;
          L = read_6128;
          MEMPTR = _address3007;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _value3009;
          int _address3009;
          contend2x1((PC + 3) & 0xFFFF);
          _address3009 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6129 = read(_address3009, 0);
          contend1x1(_address3009);
          _value3009 = operand_6129;
          int value_6130 = (_value3009 | 0x80);
          _address3009 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3009 = value_6130;
          write(_address3009, value_6130);
          MEMPTR = _address3009;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _value3011;
          int _address3011;
          contend2x1((PC + 3) & 0xFFFF);
          _address3011 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6131 = read(_address3011, 0);
          contend1x1(_address3011);
          _value3011 = operand_6131;
          int value_6132 = (_value3011 | 0x80);
          _address3011 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3011 = value_6132;
          write(_address3011, value_6132);
          int read_6133;
          read_6133 = _value3011;
          A = read_6133;
          MEMPTR = _address3011;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

}
