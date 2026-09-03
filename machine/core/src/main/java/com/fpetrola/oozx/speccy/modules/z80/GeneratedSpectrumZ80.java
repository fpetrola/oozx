package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.Ula;
import com.fpetrola.z80.cpu.GeneratedCore;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.State.InterruptionMode;
import com.fpetrola.z80.registers.UnrolledRegisterBank;
import fuse.tstates.Contention;

/** Generated from the OOP model by the generator: the instructions, MEMPTR and the contention, flattened. Do not edit; regenerate. */
public abstract class GeneratedSpectrumZ80 extends UnrolledRegisterBank implements GeneratedCore {
  protected final Memory ram;
  protected final MemoryPage[] mapRead;
  protected final MemoryPage[] mapWrite;
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
 * Indexed by the three carry bits an addition or a subtraction leaves, as Fuse indexes them.
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

  public GeneratedSpectrumZ80(Memory ram, MemoryPage[] mapRead, MemoryPage[] mapWrite, Ula ula, SpectrumZ80Clock clock, Display display, IO io, byte[] noMreqRun2, byte[] noMreqRun3, byte[] noMreqRun4, byte[] noMreqRun5, byte[] noMreqRun6, byte[] noMreqRun7) {
    this.ram = ram;
    this.mapRead = mapRead;
    this.mapWrite = mapWrite;
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
  }

  public void attach(State state) {
    this.state = state;
  }

  public abstract void contend(int address, int times, int tstates, Contention.Kind kind);

  private int read(int address, int fetching) {
      MemoryPage mapping_6382 = mapRead[address >>> 11];
      if (mapping_6382.contended) {
          clock.addTStates(ula.contention[clock.getTStates()], "ula readbyte");
      }
      int value_6381 = mapping_6382.page[mapping_6382.offset + (address & 0x7FF)];
      clock.addTStates(fetching == 1 ? 4 : 3);
      return value_6381;
  }

  private void write(int address, int value) {
      MemoryPage memoryPage_6383 = mapWrite[address >>> 11];
      if (memoryPage_6383.contended) {
          clock.addTStates(ula.contention[clock.getTStates()], "ula writebyte");
      }
      byte value_6384 = ((byte) (value & 0xff));
      if ((memoryPage_6383.writable) || (memoryPage_6383.source != ram.sourceNone && ram.settings.current.writableRoms)) {
          int addressMasked_6385 = address & 0x7FF;
          int offset2_6386 = addressMasked_6385 + memoryPage_6383.offset;
          if (memoryPage_6383.source == ram.sourceRam && (memoryPage_6383.pageNum) == ram.currentScreen && (offset2_6386 & ram.screenMask) < 0x1b00 && memoryPage_6383.page[memoryPage_6383.offset + addressMasked_6385] != value_6384) {
              display.dirtySinclair(offset2_6386);
          }
          memoryPage_6383.page[memoryPage_6383.offset + addressMasked_6385] = value_6384 & 0xff;
      }
      clock.addTStates(3);
  }

  private void contend2x1(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(noMreqRun2[clock.getTStates()]);
      } else {
          clock.addTStates(2);
      }
  }

  private void contend7x1(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(noMreqRun7[clock.getTStates()]);
      } else {
          clock.addTStates(7);
      }
  }

  private void contend1x1(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(ula.contentionNoMreq[clock.getTStates()] + 1);
      } else {
          clock.addTStates(1);
      }
  }

  private void contend5x1(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(noMreqRun5[clock.getTStates()]);
      } else {
          clock.addTStates(5);
      }
  }

  private void contend1x3(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(ula.contentionNoMreq[clock.getTStates()] + 3);
      } else {
          clock.addTStates(3);
      }
  }

  private void contend4x1(int address) {
      if (mapRead[address >>> 11].contended) {
          clock.addTStates(noMreqRun4[clock.getTStates()]);
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
          int wordNumber1_1421 = (((Integer) (A << 8)) | operand_1422);
          MEMPTR = ((wordNumber1_1421 + 1) & 0xFFFF);
          int port_1423 = operand_1422;
          port_1423 = (port_1423 | A << 8);
          int value_1424 = io.in(port_1423);
          A = value_1424 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress711 = 0;
          int address_1425 = (PC + 1) & 0xFFFF;
          int operand_1427 = read(address_1425, 0);
          int operand_1429 = read((address_1425 + 1) & 0xFFFF, 0);
          int value_1430 = (_jumpAddress711 = (operand_1429 << 8) | operand_1427);
          MEMPTR = value_1430;
          int jumpAddress2_1431 = (_jumpAddress711 = (operand_1429 << 8) | operand_1427);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_1435 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_1435 >>> 8));
              write(SP, (value_1435 & 0xFF));
              _jumpAddress711 = jumpAddress2_1431;
              MEMPTR = _jumpAddress711;
              PC = jumpAddress2_1431;
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
          int operand_3575 = read((PC + 1) & 0xFFFF, 0);
          int value1_3576 = A;
          int value3_3578 = F & 1;
          _F1721 = value3_3578;
          int sbctemp_3579 = value1_3576 - operand_3575 - (_F1721 & 1);
          int lookup_3580 = ((value1_3576 & 0x88) >> 3) | ((operand_3575 & 0x88) >> 2) | ((sbctemp_3579 & 0x88) >> 1);
          value1_3576 = sbctemp_3579 & 0xff;
          _F1721 = ((sbctemp_3579 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3580 & 0x07)] | OVERFLOW_SUB[(lookup_3580 >> 4)] | (SZ53[value1_3576] | (value1_3576 == 0 ? 0x40 : 0));
          F = (_F1721 & 0xFF);
          A = value1_3576;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC1723;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3582 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3582 >>> 8));
          write(SP, (value_3582 & 0xFF));
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
          int jumpAddress2_3583 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_3585 = read(SP, 0);
              int wordNumber_3586 = read((SP + 1) & 0xFFFF, 0);
              int value_3584 = ((wordNumber_3586 << 8) | wordNumber1_3585);
              int wordNumber_3587 = SP;
              SP = ((wordNumber_3587 + 2) & 0xFFFF);
              jumpAddress2_3583 = value_3584;
              MEMPTR = jumpAddress2_3583;
              PC = jumpAddress2_3583;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_3591 = read(SP, 0);
          int wordNumber_3592 = read((SP + 1) & 0xFFFF, 0);
          int value_3590 = ((wordNumber_3592 << 8) | wordNumber1_3591);
          int wordNumber_3593 = SP;
          SP = ((wordNumber_3593 + 2) & 0xFFFF);
          H = (value_3590 >>> 8);
          L = value_3590 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress1726 = 0;
          int address_3595 = (PC + 1) & 0xFFFF;
          int operand_3597 = read(address_3595, 0);
          int operand_3599 = read((address_3595 + 1) & 0xFFFF, 0);
          int jumpAddress2_3594 = (_jumpAddress1726 = (operand_3599 << 8) | operand_3597);
          if ((!((F & 4) == 4))) {
              _jumpAddress1726 = jumpAddress2_3594;
              MEMPTR = _jumpAddress1726;
              PC = jumpAddress2_3594;
              break;
          } else {
              MEMPTR = _jumpAddress1726;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address1727 = SP;
          int wordNumber1_3602 = read(_address1727, 0);
          int wordNumber_3603 = read((_address1727 + 1) & 0xFFFF, 0);
          int v1_3601 = ((wordNumber_3603 << 8) | wordNumber1_3602);
          int v2_3604 = ((H << 8) | L);
          _address1727 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address1727 + 1) & 0xFFFF, (v2_3604 >>> 8));
          write(_address1727, (v2_3604 & 0xFF));
          H = (v1_3601 >>> 8);
          L = v1_3601 & 0xFF;
          MEMPTR = ((H << 8) | L);
          contend2x1(SP);
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress1729 = 0;
          int address_3605 = (PC + 1) & 0xFFFF;
          int operand_3607 = read(address_3605, 0);
          int operand_3609 = read((address_3605 + 1) & 0xFFFF, 0);
          int value_3610 = (_jumpAddress1729 = (operand_3609 << 8) | operand_3607);
          MEMPTR = value_3610;
          int jumpAddress2_3611 = (_jumpAddress1729 = (operand_3609 << 8) | operand_3607);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3615 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3615 >>> 8));
              write(SP, (value_3615 & 0xFF));
              _jumpAddress1729 = jumpAddress2_3611;
              MEMPTR = _jumpAddress1729;
              PC = jumpAddress2_3611;
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
          int value_3617 = ((H << 8) | L);
          write((SP + 1) & 0xFFFF, (value_3617 >>> 8));
          write(SP, (value_3617 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _F1731;
          int operand_3618 = read((PC + 1) & 0xFFFF, 0);
          int value1_3619 = A;
          int value2_3620 = operand_3618;
          value2_3620 &= value1_3619;
          _F1731 = 0x10 | (SZ53P[value2_3620 & 0xff] | (value2_3620 == 0 ? 0x40 : 0));
          int result_3622 = value2_3620 & 0xFF;
          F = (_F1731 & 0xFF);
          A = result_3622;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC1733;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3623 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3623 >>> 8));
          write(SP, (value_3623 & 0xFF));
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
          int jumpAddress2_3624 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_3626 = read(SP, 0);
              int wordNumber_3627 = read((SP + 1) & 0xFFFF, 0);
              int value_3625 = ((wordNumber_3627 << 8) | wordNumber1_3626);
              int wordNumber_3628 = SP;
              SP = ((wordNumber_3628 + 2) & 0xFFFF);
              jumpAddress2_3624 = value_3625;
              MEMPTR = jumpAddress2_3624;
              PC = jumpAddress2_3624;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xE9: {
          int _nextPC1735;
          int jumpAddress2_3630 = ((H << 8) | L);
          _nextPC1735 = jumpAddress2_3630;
          MEMPTR = 0;
          PC = _nextPC1735;
          break;
      }
      case 0xEA: {
          int _jumpAddress1736 = 0;
          int address_3633 = (PC + 1) & 0xFFFF;
          int operand_3635 = read(address_3633, 0);
          int operand_3637 = read((address_3633 + 1) & 0xFFFF, 0);
          int jumpAddress2_3632 = (_jumpAddress1736 = (operand_3637 << 8) | operand_3635);
          if (((F & 4) == 4)) {
              _jumpAddress1736 = jumpAddress2_3632;
              MEMPTR = _jumpAddress1736;
              PC = jumpAddress2_3632;
              break;
          } else {
              MEMPTR = _jumpAddress1736;
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_3639 = ((D << 8) | E);
          int v2_3640 = ((H << 8) | L);
          D = (v2_3640 >>> 8);
          E = v2_3640 & 0xFF;
          H = (v1_3639 >>> 8);
          L = v1_3639 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress1738 = 0;
          int address_3641 = (PC + 1) & 0xFFFF;
          int operand_3643 = read(address_3641, 0);
          int operand_3645 = read((address_3641 + 1) & 0xFFFF, 0);
          int value_3646 = (_jumpAddress1738 = (operand_3645 << 8) | operand_3643);
          MEMPTR = value_3646;
          int jumpAddress2_3647 = (_jumpAddress1738 = (operand_3645 << 8) | operand_3643);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3651 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3651 >>> 8));
              write(SP, (value_3651 & 0xFF));
              _jumpAddress1738 = jumpAddress2_3647;
              MEMPTR = _jumpAddress1738;
              PC = jumpAddress2_3647;
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
          int operand_4167 = read((PC + 1) & 0xFFFF, 0);
          int value1_4168 = A;
          int value2_4169 = operand_4167;
          value2_4169 ^= value1_4168;
          _F2047 = SZ53P[value2_4169 & 0xff] | (value2_4169 == 0 ? 0x40 : 0);
          int result_4171 = value2_4169 & 0xFF;
          F = (_F2047 & 0xFF);
          A = result_4171;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC2049;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4172 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4172 >>> 8));
          write(SP, (value_4172 & 0xFF));
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
          int jumpAddress2_4173 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_4175 = read(SP, 0);
              int wordNumber_4176 = read((SP + 1) & 0xFFFF, 0);
              int value_4174 = ((wordNumber_4176 << 8) | wordNumber1_4175);
              int wordNumber_4177 = SP;
              SP = ((wordNumber_4177 + 2) & 0xFFFF);
              jumpAddress2_4173 = value_4174;
              MEMPTR = jumpAddress2_4173;
              PC = jumpAddress2_4173;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 1) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_4181 = read(SP, 0);
          int wordNumber_4182 = read((SP + 1) & 0xFFFF, 0);
          int value_4180 = ((wordNumber_4182 << 8) | wordNumber1_4181);
          int wordNumber_4183 = SP;
          SP = ((wordNumber_4183 + 2) & 0xFFFF);
          A = (value_4180 >>> 8);
          F = value_4180 & 0xFF;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress2052 = 0;
          int address_4185 = (PC + 1) & 0xFFFF;
          int operand_4187 = read(address_4185, 0);
          int operand_4189 = read((address_4185 + 1) & 0xFFFF, 0);
          int jumpAddress2_4184 = (_jumpAddress2052 = (operand_4189 << 8) | operand_4187);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress2052 = jumpAddress2_4184;
              MEMPTR = _jumpAddress2052;
              PC = jumpAddress2_4184;
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
          int address_4191 = (PC + 1) & 0xFFFF;
          int operand_4193 = read(address_4191, 0);
          int operand_4195 = read((address_4191 + 1) & 0xFFFF, 0);
          int value_4196 = (_jumpAddress2054 = (operand_4195 << 8) | operand_4193);
          MEMPTR = value_4196;
          int jumpAddress2_4197 = (_jumpAddress2054 = (operand_4195 << 8) | operand_4193);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4201 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4201 >>> 8));
              write(SP, (value_4201 & 0xFF));
              _jumpAddress2054 = jumpAddress2_4197;
              MEMPTR = _jumpAddress2054;
              PC = jumpAddress2_4197;
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
          int value_4203 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_4203 >>> 8));
          write(SP, (value_4203 & 0xFF));
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F2056;
          int operand_4204 = read((PC + 1) & 0xFFFF, 0);
          int value1_4205 = A;
          int value2_4206 = operand_4204;
          value2_4206 |= value1_4205;
          _F2056 = SZ53P[value2_4206 & 0xff] | (value2_4206 == 0 ? 0x40 : 0);
          int result_4208 = value2_4206 & 0xFF;
          F = (_F2056 & 0xFF);
          A = result_4208;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC2058;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4209 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4209 >>> 8));
          write(SP, (value_4209 & 0xFF));
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
          int jumpAddress2_4210 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_4212 = read(SP, 0);
              int wordNumber_4213 = read((SP + 1) & 0xFFFF, 0);
              int value_4211 = ((wordNumber_4213 << 8) | wordNumber1_4212);
              int wordNumber_4214 = SP;
              SP = ((wordNumber_4214 + 2) & 0xFFFF);
              jumpAddress2_4210 = value_4211;
              MEMPTR = jumpAddress2_4210;
              PC = jumpAddress2_4210;
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
          int address_4217 = (PC + 1) & 0xFFFF;
          int operand_4219 = read(address_4217, 0);
          int operand_4221 = read((address_4217 + 1) & 0xFFFF, 0);
          int jumpAddress2_4216 = (_jumpAddress2061 = (operand_4221 << 8) | operand_4219);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress2061 = jumpAddress2_4216;
              MEMPTR = _jumpAddress2061;
              PC = jumpAddress2_4216;
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
          int address_4223 = (PC + 1) & 0xFFFF;
          int operand_4225 = read(address_4223, 0);
          int operand_4227 = read((address_4223 + 1) & 0xFFFF, 0);
          int value_4228 = (_jumpAddress2063 = (operand_4227 << 8) | operand_4225);
          MEMPTR = value_4228;
          int jumpAddress2_4229 = (_jumpAddress2063 = (operand_4227 << 8) | operand_4225);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4233 = ((PC + 3) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4233 >>> 8));
              write(SP, (value_4233 & 0xFF));
              _jumpAddress2063 = jumpAddress2_4229;
              MEMPTR = _jumpAddress2063;
              PC = jumpAddress2_4229;
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
          int operand_6373 = read((PC + 1) & 0xFFFF, 0);
          int cptemp_6377 = A - operand_6373;
          int lookup_6378 = ((A & 0x88) >> 3) | ((operand_6373 & 0x88) >> 2) | ((cptemp_6377 & 0x88) >> 1);
          _F3073 = ((cptemp_6377 & 0x100) != 0 ? 1 : (cptemp_6377 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_6378 & 0x07)] | OVERFLOW_SUB[(lookup_6378 >> 4)] | (operand_6373 & 0x28) | (cptemp_6377 & 0x80);
          F = (_F3073 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC3075;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6380 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6380 >>> 8));
          write(SP, (value_6380 & 0xFF));
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
          int address_1437 = (PC + 2) & 0xFFFF;
          int operand_1439 = read(address_1437, 0);
          int operand_1441 = read((address_1437 + 1) & 0xFFFF, 0);
          int value_1442 = ((operand_1441 << 8) | operand_1439);
          B = (value_1442 >>> 8);
          C = value_1442 & 0xFF;
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
          int read_1443 = ((B << 8) | C);
          int value_1444 = (read_1443 + 1) & 0xFFFF;
          B = (value_1444 >>> 8);
          C = value_1444 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F717;
          int value1_1445 = B;
          int value2_1446 = F;
          _F717 = value2_1446;
          value1_1445++;
          value1_1445 &= 0xff;
          _F717 = (_F717 & 1) | (value1_1445 == 0x80 ? 4 : 0) | ((value1_1445 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1445 & 0xff] | (value1_1445 == 0 ? 0x40 : 0));
          int result_1448 = value1_1445 & 0xFF;
          F = (_F717 & 0xFF);
          B = result_1448;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F719;
          int value1_1449 = B;
          int value2_1450 = F;
          _F719 = value2_1450;
          _F719 = (_F719 & 1) | ((value1_1449 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1449--;
          value1_1449 &= 0xff;
          _F719 |= (value1_1449 == 0x7f ? 4 : 0) | (SZ53[value1_1449 & 0xff] | (value1_1449 == 0 ? 0x40 : 0));
          int result_1452 = value1_1449 & 0xFF;
          F = (_F719 & 0xFF);
          B = result_1452;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          int operand_1453 = read((PC + 2) & 0xFFFF, 0);
          B = operand_1453;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F722;
          int value1_1454 = A;
          int value2_1455 = F;
          _F722 = value2_1455;
          value1_1454 = (value1_1454 << 1) | (value1_1454 >> 7);
          _F722 = (_F722 & 0xC4) | (value1_1454 & 0x29);
          int result_1457 = value1_1454 & 0xFF;
          F = _F722;
          A = result_1457;
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
          int v1_1458 = ((A << 8) | F);
          int v2_1459 = _AF;
          A = (v2_1459 >>> 8);
          F = v2_1459 & 0xFF;
          _AF = v1_1458;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F725;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int b_1460 = ((B << 8) | C);
          int result_1461 = (IX + b_1460);
          int value1_1462 = ((IX & 0x0800) >> 4 | result_1461 >> 11);
          int value2_1463 = F;
          int value3_1464 = (b_1460 >> 11) & 1;
          _F725 = value2_1463;
          int add16temp_1465 = value1_1462 << 11;
          int lookup_1466 = (((value1_1462 << 4) & 0x0800) >> 11) | ((value3_1464 << 11) >> 10) | ((add16temp_1465 & 0x0800) >> 9);
          _F725 = (_F725 & 0xC4) | ((add16temp_1465 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1465 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1466];
          F = (_F725 & 0xFF);
          IX = (result_1461 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _address727 = (B << 8) | C;
          int value_1468 = read(_address727, 0);
          A = value_1468;
          MEMPTR = ((_address727 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int value_1469 = (((B << 8) | C) - 1) & 0xFFFF;
          B = (value_1469 >>> 8);
          C = value_1469 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F730;
          int value1_1470 = C;
          int value2_1471 = F;
          _F730 = value2_1471;
          value1_1470++;
          value1_1470 &= 0xff;
          _F730 = (_F730 & 1) | (value1_1470 == 0x80 ? 4 : 0) | ((value1_1470 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1470 & 0xff] | (value1_1470 == 0 ? 0x40 : 0));
          int result_1473 = value1_1470 & 0xFF;
          F = (_F730 & 0xFF);
          C = result_1473;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F732;
          int value1_1474 = C;
          int value2_1475 = F;
          _F732 = value2_1475;
          _F732 = (_F732 & 1) | ((value1_1474 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1474--;
          value1_1474 &= 0xff;
          _F732 |= (value1_1474 == 0x7f ? 4 : 0) | (SZ53[value1_1474 & 0xff] | (value1_1474 == 0 ? 0x40 : 0));
          int result_1477 = value1_1474 & 0xFF;
          F = (_F732 & 0xFF);
          C = result_1477;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int operand_1478 = read((PC + 2) & 0xFFFF, 0);
          C = operand_1478;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F735;
          int value1_1479 = A;
          int value2_1480 = F;
          _F735 = value2_1480;
          _F735 = (_F735 & 0xC4) | (value1_1479 & 1);
          value1_1479 = (value1_1479 >> 1) | (value1_1479 << 7);
          _F735 |= (value1_1479 & 0x28);
          int result_1482 = (value1_1479 & 0xff);
          F = (_F735 & 0xFF);
          A = result_1482;
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
              int operand_1484 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1483 = ((PC + 3 + (byte) operand_1484) & 0xFFFF);
              MEMPTR = jumpAddress2_1483;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1483;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x11: {
          int address_1486 = (PC + 2) & 0xFFFF;
          int operand_1488 = read(address_1486, 0);
          int operand_1490 = read((address_1486 + 1) & 0xFFFF, 0);
          int value_1491 = ((operand_1490 << 8) | operand_1488);
          D = (value_1491 >>> 8);
          E = value_1491 & 0xFF;
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
          int read_1492 = ((D << 8) | E);
          int value_1493 = (read_1492 + 1) & 0xFFFF;
          D = (value_1493 >>> 8);
          E = value_1493 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F742;
          int value1_1494 = D;
          int value2_1495 = F;
          _F742 = value2_1495;
          value1_1494++;
          value1_1494 &= 0xff;
          _F742 = (_F742 & 1) | (value1_1494 == 0x80 ? 4 : 0) | ((value1_1494 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1494 & 0xff] | (value1_1494 == 0 ? 0x40 : 0));
          int result_1497 = value1_1494 & 0xFF;
          F = (_F742 & 0xFF);
          D = result_1497;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F744;
          int value1_1498 = D;
          int value2_1499 = F;
          _F744 = value2_1499;
          _F744 = (_F744 & 1) | ((value1_1498 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1498--;
          value1_1498 &= 0xff;
          _F744 |= (value1_1498 == 0x7f ? 4 : 0) | (SZ53[value1_1498 & 0xff] | (value1_1498 == 0 ? 0x40 : 0));
          int result_1501 = value1_1498 & 0xFF;
          F = (_F744 & 0xFF);
          D = result_1501;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          int operand_1502 = read((PC + 2) & 0xFFFF, 0);
          D = operand_1502;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F747;
          int value1_1503 = A;
          int value2_1504 = F;
          _F747 = value2_1504;
          int bytetemp_1506 = value1_1503;
          value1_1503 = (value1_1503 << 1) | (_F747 & 1);
          _F747 = (_F747 & 0xC4) | (value1_1503 & 0x28) | (bytetemp_1506 >> 7);
          int result_1507 = value1_1503 & 0xFF;
          F = (_F747 & 0xFF);
          A = result_1507;
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
          int operand_1509 = read((PC + 2) & 0xFFFF, 0);
          int jumpAddress2_1508 = ((PC + 3 + (byte) operand_1509) & 0xFFFF);
          _nextPC749 = jumpAddress2_1508;
          MEMPTR = _nextPC749;
          contend5x1((PC + 1) & 0xFFFF);
          PC = _nextPC749;
          break;
      }
      case 0x19: {
          int _F750;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IX + 1) & 0xFFFF);
          int b_1511 = ((D << 8) | E);
          int result_1512 = (IX + b_1511);
          int value1_1513 = ((IX & 0x0800) >> 4 | result_1512 >> 11);
          int value2_1514 = F;
          int value3_1515 = (b_1511 >> 11) & 1;
          _F750 = value2_1514;
          int add16temp_1516 = value1_1513 << 11;
          int lookup_1517 = (((value1_1513 << 4) & 0x0800) >> 11) | ((value3_1515 << 11) >> 10) | ((add16temp_1516 & 0x0800) >> 9);
          _F750 = (_F750 & 0xC4) | ((add16temp_1516 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1516 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1517];
          F = (_F750 & 0xFF);
          IX = (result_1512 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _address752 = (D << 8) | E;
          int value_1519 = read(_address752, 0);
          A = value_1519;
          MEMPTR = ((_address752 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int value_1520 = (((D << 8) | E) - 1) & 0xFFFF;
          D = (value_1520 >>> 8);
          E = value_1520 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F755;
          int value1_1521 = E;
          int value2_1522 = F;
          _F755 = value2_1522;
          value1_1521++;
          value1_1521 &= 0xff;
          _F755 = (_F755 & 1) | (value1_1521 == 0x80 ? 4 : 0) | ((value1_1521 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1521 & 0xff] | (value1_1521 == 0 ? 0x40 : 0));
          int result_1524 = value1_1521 & 0xFF;
          F = (_F755 & 0xFF);
          E = result_1524;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F757;
          int value1_1525 = E;
          int value2_1526 = F;
          _F757 = value2_1526;
          _F757 = (_F757 & 1) | ((value1_1525 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1525--;
          value1_1525 &= 0xff;
          _F757 |= (value1_1525 == 0x7f ? 4 : 0) | (SZ53[value1_1525 & 0xff] | (value1_1525 == 0 ? 0x40 : 0));
          int result_1528 = value1_1525 & 0xFF;
          F = (_F757 & 0xFF);
          E = result_1528;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int operand_1529 = read((PC + 2) & 0xFFFF, 0);
          E = operand_1529;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F760;
          int value1_1530 = A;
          int value2_1531 = F;
          _F760 = value2_1531;
          int A_1533 = value1_1530;
          int bytetemp_1534 = A_1533;
          A_1533 = (A_1533 >> 1) | (_F760 << 7);
          _F760 = (_F760 & 0xC4) | (A_1533 & 0x28) | (bytetemp_1534 & 1);
          int result_1535 = A_1533 & 0xFF;
          F = _F760;
          A = result_1535;
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
              int operand_1537 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1536 = ((PC + 3 + (byte) operand_1537) & 0xFFFF);
              MEMPTR = jumpAddress2_1536;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1536;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x21: {
          int address_1539 = (PC + 2) & 0xFFFF;
          int operand_1541 = read(address_1539, 0);
          int operand_1543 = read((address_1539 + 1) & 0xFFFF, 0);
          IX = ((operand_1543 << 8) | operand_1541);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _address764;
          int address_1544 = (PC + 2) & 0xFFFF;
          int operand_1546 = read(address_1544, 0);
          int operand_1548 = read((address_1544 + 1) & 0xFFFF, 0);
          _address764 = (operand_1548 << 8) | operand_1546;
          write(_address764, (IX & 0xFF));
          write((_address764 + 1) & 0xFFFF, (IX >>> 8));
          MEMPTR = ((_address764 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int read_1549 = IX;
          IX = ((read_1549 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F767;
          int value1_1550 = (IX >> 8);
          int value2_1551 = F;
          _F767 = value2_1551;
          value1_1550++;
          value1_1550 &= 0xff;
          _F767 = (_F767 & 1) | (value1_1550 == 0x80 ? 4 : 0) | ((value1_1550 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1550 & 0xff] | (value1_1550 == 0 ? 0x40 : 0));
          int result_1553 = value1_1550 & 0xFF;
          F = (_F767 & 0xFF);
          IX = (IX & 0x00FF) | (result_1553 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F769;
          int value1_1554 = (IX >> 8);
          int value2_1555 = F;
          _F769 = value2_1555;
          _F769 = (_F769 & 1) | ((value1_1554 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1554--;
          value1_1554 &= 0xff;
          _F769 |= (value1_1554 == 0x7f ? 4 : 0) | (SZ53[value1_1554 & 0xff] | (value1_1554 == 0 ? 0x40 : 0));
          int result_1557 = value1_1554 & 0xFF;
          F = (_F769 & 0xFF);
          IX = (IX & 0x00FF) | (result_1557 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          int operand_1558 = read((PC + 2) & 0xFFFF, 0);
          IX = (IX & 0x00FF) | (operand_1558 << 8);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F775 = 0;
          int _F774 = 0;
          int _data773 = 0;
          int _F772;
          int value1_1559 = A;
          int value2_1560 = F;
          _F772 = value2_1560;
          value1_1559 &= 0xff;
          int add_1562 = 0;
          int carry_1563 = (_F772 & 1);
          if (((_F772 & 0x10) != 0) || ((value1_1559 & 0x0f) > 9)) {
              add_1562 = 6;
          }
          if (carry_1563 != 0 || (value1_1559 > 0x99)) {
              add_1562 |= 0x60;
          }
          if (value1_1559 > 0x99) {
              carry_1563 = 1;
          }
          int and_1565 = _F772 & 0xff;
          _data773 = and_1565;
          if ((_F772 & 2) != 0) {
              int value1_1566 = value1_1559;
              int subtemp_1569 = value1_1566 - add_1562;
              int lookup_1570 = ((value1_1566 & 0x88) >> 3) | ((add_1562 & 0x88) >> 2) | ((subtemp_1569 & 0x88) >> 1);
              value1_1566 = subtemp_1569 & 0xff;
              _F774 = ((subtemp_1569 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1570 & 0x07)] | OVERFLOW_SUB[(lookup_1570 >> 4)] | (SZ53[value1_1566 & 0xff] | (value1_1566 == 0 ? 0x40 : 0));
              int result_1571 = value1_1566 & 0xFF;
              int and_1572 = (_F774 & 0xFF);
              _data773 = and_1572;
              value1_1559 = result_1571;
          } else {
              int value2_1574 = value1_1559;
              int addtemp_1576 = value2_1574 + add_1562;
              int lookup_1577 = ((value2_1574 & 0x88) >> 3) | ((add_1562 & 0x88) >> 2) | ((addtemp_1576 & 0x88) >> 1);
              value2_1574 = addtemp_1576 & 0xff;
              _F775 = ((addtemp_1576 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1577 & 0x07)] | OVERFLOW_ADD[(lookup_1577 >> 4)] | (SZ53[value2_1574 & 0xff] | (value2_1574 == 0 ? 0x40 : 0));
              int result_1578 = value2_1574 & 0xFF;
              int and_1579 = (_F775 & 0xFF);
              _data773 = and_1579;
              value1_1559 = result_1578;
          }
          _F772 = _data773;
          _F772 = (_F772 & -6) | carry_1563 | PARITY[value1_1559 & 0xff];
          int result_1580 = value1_1559 & 0xFF;
          F = (_F772 & 0xFF);
          A = result_1580;
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
              int operand_1582 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1581 = ((PC + 3 + (byte) operand_1582) & 0xFFFF);
              MEMPTR = jumpAddress2_1581;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1581;
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
          int result_1584 = (IX + IX);
          int value1_1585 = ((IX & 0x0800) >> 4 | result_1584 >> 11);
          int value2_1586 = F;
          int value3_1587 = (IX >> 11) & 1;
          _F778 = value2_1586;
          int add16temp_1588 = value1_1585 << 11;
          int lookup_1589 = (((value1_1585 << 4) & 0x0800) >> 11) | ((value3_1587 << 11) >> 10) | ((add16temp_1588 & 0x0800) >> 9);
          _F778 = (_F778 & 0xC4) | ((add16temp_1588 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1588 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1589];
          F = (_F778 & 0xFF);
          IX = (result_1584 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _address780;
          int address_1591 = (PC + 2) & 0xFFFF;
          int operand_1593 = read(address_1591, 0);
          int operand_1595 = read((address_1591 + 1) & 0xFFFF, 0);
          _address780 = (operand_1595 << 8) | operand_1593;
          int wordNumber1_1596 = read(_address780, 0);
          int wordNumber_1597 = read((_address780 + 1) & 0xFFFF, 0);
          IX = ((wordNumber_1597 << 8) | wordNumber1_1596);
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
          int value1_1598 = (IX & 0xFF);
          int value2_1599 = F;
          _F783 = value2_1599;
          value1_1598++;
          value1_1598 &= 0xff;
          _F783 = (_F783 & 1) | (value1_1598 == 0x80 ? 4 : 0) | ((value1_1598 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1598 & 0xff] | (value1_1598 == 0 ? 0x40 : 0));
          int result_1601 = value1_1598 & 0xFF;
          F = (_F783 & 0xFF);
          IX = (IX & 0xFF00) | result_1601;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F785;
          int value1_1602 = (IX & 0xFF);
          int value2_1603 = F;
          _F785 = value2_1603;
          _F785 = (_F785 & 1) | ((value1_1602 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1602--;
          value1_1602 &= 0xff;
          _F785 |= (value1_1602 == 0x7f ? 4 : 0) | (SZ53[value1_1602 & 0xff] | (value1_1602 == 0 ? 0x40 : 0));
          int result_1605 = value1_1602 & 0xFF;
          F = (_F785 & 0xFF);
          IX = (IX & 0xFF00) | result_1605;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int operand_1606 = read((PC + 2) & 0xFFFF, 0);
          IX = (IX & 0xFF00) | operand_1606;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F788;
          int value1_1607 = A;
          int value2_1608 = F;
          _F788 = value2_1608;
          value1_1607 ^= 0xff;
          _F788 = (_F788 & 0xC5) | (value1_1607 & 0x28) | 0x12;
          int result_1610 = value1_1607 & 0xFF;
          F = _F788;
          A = result_1610;
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
              int operand_1612 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1611 = ((PC + 3 + (byte) operand_1612) & 0xFFFF);
              MEMPTR = jumpAddress2_1611;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1611;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x31: {
          int address_1614 = (PC + 2) & 0xFFFF;
          int operand_1616 = read(address_1614, 0);
          int operand_1618 = read((address_1614 + 1) & 0xFFFF, 0);
          SP = ((operand_1618 << 8) | operand_1616);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _address792;
          int address_1619 = (PC + 2) & 0xFFFF;
          int operand_1621 = read(address_1619, 0);
          int operand_1623 = read((address_1619 + 1) & 0xFFFF, 0);
          _address792 = (operand_1623 << 8) | operand_1621;
          write(_address792, A);
          MEMPTR = ((A << 8) | ((_address792 + 1) & 0xff));
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int read_1624 = SP;
          SP = ((read_1624 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F796;
          int _value795;
          int _address795;
          int operand_1625 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1625)) & 0xFFFF;
          int operand_1626 = read(_address795, 0);
          contend1x1(_address795);
          _value795 = operand_1626;
          int value1_1627 = _value795;
          int value2_1628 = F;
          _F796 = value2_1628;
          value1_1627++;
          value1_1627 &= 0xff;
          _F796 = (_F796 & 1) | (value1_1627 == 0x80 ? 4 : 0) | ((value1_1627 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1627 & 0xff] | (value1_1627 == 0 ? 0x40 : 0));
          int result_1630 = value1_1627 & 0xFF;
          F = (_F796 & 0xFF);
          _address795 = (IX + (int) ((byte) operand_1625)) & 0xFFFF;
          _value795 = result_1630;
          write(_address795, result_1630);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F798;
          int _value795;
          int _address795;
          int operand_1631 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1631)) & 0xFFFF;
          int operand_1632 = read(_address795, 0);
          contend1x1(_address795);
          _value795 = operand_1632;
          int value1_1633 = _value795;
          int value2_1634 = F;
          _F798 = value2_1634;
          _F798 = (_F798 & 1) | ((value1_1633 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1633--;
          value1_1633 &= 0xff;
          _F798 |= (value1_1633 == 0x7f ? 4 : 0) | (SZ53[value1_1633 & 0xff] | (value1_1633 == 0 ? 0x40 : 0));
          int result_1636 = value1_1633 & 0xFF;
          F = (_F798 & 0xFF);
          _address795 = (IX + (int) ((byte) operand_1631)) & 0xFFFF;
          _value795 = result_1636;
          write(_address795, result_1636);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _address800;
          int operand_1637 = read((PC + 2) & 0xFFFF, 0);
          int operand_1638 = read((PC + 3) & 0xFFFF, 0);
          _address800 = (IX + (int) ((byte) operand_1637)) & 0xFFFF;
          contend2x1((PC + 3) & 0xFFFF);
          write(_address800, operand_1638);
          MEMPTR = _address800;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F802;
          int value2_1640 = F;
          _F802 = value2_1640;
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
              int operand_1644 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_1643 = ((PC + 3 + (byte) operand_1644) & 0xFFFF);
              MEMPTR = jumpAddress2_1643;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_1643;
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
          int result_1646 = (IX + SP);
          int value1_1647 = ((IX & 0x0800) >> 4 | result_1646 >> 11);
          int value2_1648 = F;
          int value3_1649 = (SP >> 11) & 1;
          _F805 = value2_1648;
          int add16temp_1650 = value1_1647 << 11;
          int lookup_1651 = (((value1_1647 << 4) & 0x0800) >> 11) | ((value3_1649 << 11) >> 10) | ((add16temp_1650 & 0x0800) >> 9);
          _F805 = (_F805 & 0xC4) | ((add16temp_1650 & 0x10000) != 0 ? 1 : 0) | ((add16temp_1650 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_1651];
          F = (_F805 & 0xFF);
          IX = (result_1646 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _address807;
          int address_1653 = (PC + 2) & 0xFFFF;
          int operand_1655 = read(address_1653, 0);
          int operand_1657 = read((address_1653 + 1) & 0xFFFF, 0);
          _address807 = (operand_1657 << 8) | operand_1655;
          int value_1658 = read(_address807, 0);
          A = value_1658;
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
          int value1_1659 = A;
          int value2_1660 = F;
          _F810 = value2_1660;
          value1_1659++;
          value1_1659 &= 0xff;
          _F810 = (_F810 & 1) | (value1_1659 == 0x80 ? 4 : 0) | ((value1_1659 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_1659 & 0xff] | (value1_1659 == 0 ? 0x40 : 0));
          int result_1662 = value1_1659 & 0xFF;
          F = (_F810 & 0xFF);
          A = result_1662;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F812;
          int value1_1663 = A;
          int value2_1664 = F;
          _F812 = value2_1664;
          _F812 = (_F812 & 1) | ((value1_1663 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_1663--;
          value1_1663 &= 0xff;
          _F812 |= (value1_1663 == 0x7f ? 4 : 0) | (SZ53[value1_1663 & 0xff] | (value1_1663 == 0 ? 0x40 : 0));
          int result_1666 = value1_1663 & 0xFF;
          F = (_F812 & 0xFF);
          A = result_1666;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int operand_1667 = read((PC + 2) & 0xFFFF, 0);
          A = operand_1667;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F815;
          int value2_1669 = F;
          _F815 = value2_1669;
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
          int operand_1672 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1672)) & 0xFFFF;
          int operand_1673 = read(_address795, 0);
          _value795 = operand_1673;
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
          int operand_1674 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1674)) & 0xFFFF;
          int operand_1675 = read(_address795, 0);
          _value795 = operand_1675;
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
          int operand_1676 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1676)) & 0xFFFF;
          int operand_1677 = read(_address795, 0);
          _value795 = operand_1677;
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
          int operand_1678 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1678)) & 0xFFFF;
          int operand_1679 = read(_address795, 0);
          _value795 = operand_1679;
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
          int operand_1680 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1680)) & 0xFFFF;
          int operand_1681 = read(_address795, 0);
          _value795 = operand_1681;
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
          int operand_1682 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1682)) & 0xFFFF;
          int operand_1683 = read(_address795, 0);
          _value795 = operand_1683;
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
          int operand_1684 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1684)) & 0xFFFF;
          write(_address795, B);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _address795;
          int operand_1685 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1685)) & 0xFFFF;
          write(_address795, C);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _address795;
          int operand_1686 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1686)) & 0xFFFF;
          write(_address795, D);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address795;
          int operand_1687 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1687)) & 0xFFFF;
          write(_address795, E);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _address795;
          int operand_1688 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1688)) & 0xFFFF;
          write(_address795, H);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _address795;
          int operand_1689 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1689)) & 0xFFFF;
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
          int operand_1690 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1690)) & 0xFFFF;
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
          int operand_1691 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address795 = (IX + (int) ((byte) operand_1691)) & 0xFFFF;
          int operand_1692 = read(_address795, 0);
          _value795 = operand_1692;
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
          int value1_1693 = A;
          int value2_1694 = B;
          int addtemp_1696 = value2_1694 + value1_1693;
          int lookup_1697 = ((value2_1694 & 0x88) >> 3) | ((value1_1693 & 0x88) >> 2) | ((addtemp_1696 & 0x88) >> 1);
          value2_1694 = addtemp_1696 & 0xff;
          _F881 = ((addtemp_1696 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1697 & 0x07)] | OVERFLOW_ADD[(lookup_1697 >> 4)] | (SZ53[value2_1694] | (value2_1694 == 0 ? 0x40 : 0));
          F = (_F881 & 0xFF);
          A = value2_1694;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _F883;
          int value1_1699 = A;
          int value2_1700 = C;
          int addtemp_1702 = value2_1700 + value1_1699;
          int lookup_1703 = ((value2_1700 & 0x88) >> 3) | ((value1_1699 & 0x88) >> 2) | ((addtemp_1702 & 0x88) >> 1);
          value2_1700 = addtemp_1702 & 0xff;
          _F883 = ((addtemp_1702 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1703 & 0x07)] | OVERFLOW_ADD[(lookup_1703 >> 4)] | (SZ53[value2_1700] | (value2_1700 == 0 ? 0x40 : 0));
          F = (_F883 & 0xFF);
          A = value2_1700;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _F885;
          int value1_1705 = A;
          int value2_1706 = D;
          int addtemp_1708 = value2_1706 + value1_1705;
          int lookup_1709 = ((value2_1706 & 0x88) >> 3) | ((value1_1705 & 0x88) >> 2) | ((addtemp_1708 & 0x88) >> 1);
          value2_1706 = addtemp_1708 & 0xff;
          _F885 = ((addtemp_1708 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1709 & 0x07)] | OVERFLOW_ADD[(lookup_1709 >> 4)] | (SZ53[value2_1706] | (value2_1706 == 0 ? 0x40 : 0));
          F = (_F885 & 0xFF);
          A = value2_1706;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _F887;
          int value1_1711 = A;
          int value2_1712 = E;
          int addtemp_1714 = value2_1712 + value1_1711;
          int lookup_1715 = ((value2_1712 & 0x88) >> 3) | ((value1_1711 & 0x88) >> 2) | ((addtemp_1714 & 0x88) >> 1);
          value2_1712 = addtemp_1714 & 0xff;
          _F887 = ((addtemp_1714 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1715 & 0x07)] | OVERFLOW_ADD[(lookup_1715 >> 4)] | (SZ53[value2_1712] | (value2_1712 == 0 ? 0x40 : 0));
          F = (_F887 & 0xFF);
          A = value2_1712;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _F889;
          int value1_1717 = A;
          int value2_1718 = (IX >> 8);
          int addtemp_1720 = value2_1718 + value1_1717;
          int lookup_1721 = ((value2_1718 & 0x88) >> 3) | ((value1_1717 & 0x88) >> 2) | ((addtemp_1720 & 0x88) >> 1);
          value2_1718 = addtemp_1720 & 0xff;
          _F889 = ((addtemp_1720 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1721 & 0x07)] | OVERFLOW_ADD[(lookup_1721 >> 4)] | (SZ53[value2_1718] | (value2_1718 == 0 ? 0x40 : 0));
          F = (_F889 & 0xFF);
          A = value2_1718;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _F891;
          int value1_1723 = A;
          int value2_1724 = (IX & 0xFF);
          int addtemp_1726 = value2_1724 + value1_1723;
          int lookup_1727 = ((value2_1724 & 0x88) >> 3) | ((value1_1723 & 0x88) >> 2) | ((addtemp_1726 & 0x88) >> 1);
          value2_1724 = addtemp_1726 & 0xff;
          _F891 = ((addtemp_1726 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1727 & 0x07)] | OVERFLOW_ADD[(lookup_1727 >> 4)] | (SZ53[value2_1724] | (value2_1724 == 0 ? 0x40 : 0));
          F = (_F891 & 0xFF);
          A = value2_1724;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _F893;
          int _value795;
          int _address795;
          int operand_1729 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1729)) & 0xFFFF;
          int operand_1730 = read(_address795, 0);
          _value795 = operand_1730;
          int value1_1731 = A;
          int value2_1732 = _value795;
          int addtemp_1734 = value2_1732 + value1_1731;
          int lookup_1735 = ((value2_1732 & 0x88) >> 3) | ((value1_1731 & 0x88) >> 2) | ((addtemp_1734 & 0x88) >> 1);
          value2_1732 = addtemp_1734 & 0xff;
          _F893 = ((addtemp_1734 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1735 & 0x07)] | OVERFLOW_ADD[(lookup_1735 >> 4)] | (SZ53[value2_1732] | (value2_1732 == 0 ? 0x40 : 0));
          F = (_F893 & 0xFF);
          A = value2_1732;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _F895;
          int value1_1737 = A;
          int value2_1738 = A;
          int addtemp_1740 = value2_1738 + value1_1737;
          int lookup_1741 = ((value2_1738 & 0x88) >> 3) | ((value1_1737 & 0x88) >> 2) | ((addtemp_1740 & 0x88) >> 1);
          value2_1738 = addtemp_1740 & 0xff;
          _F895 = ((addtemp_1740 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1741 & 0x07)] | OVERFLOW_ADD[(lookup_1741 >> 4)] | (SZ53[value2_1738] | (value2_1738 == 0 ? 0x40 : 0));
          F = (_F895 & 0xFF);
          A = value2_1738;
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
          int value1_1743 = A;
          int value3_1745 = F & 1;
          _F897 = value3_1745;
          int adctemp_1746 = value1_1743 + B + (_F897 & 1);
          int lookup_1747 = ((value1_1743 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((adctemp_1746 & 0x88) >> 1);
          value1_1743 = adctemp_1746 & 0xff;
          _F897 = ((adctemp_1746 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1747 & 0x07)] | OVERFLOW_ADD[(lookup_1747 >> 4)] | (SZ53[value1_1743] | (value1_1743 == 0 ? 0x40 : 0));
          F = (_F897 & 0xFF);
          A = value1_1743;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _F899;
          int value1_1749 = A;
          int value3_1751 = F & 1;
          _F899 = value3_1751;
          int adctemp_1752 = value1_1749 + C + (_F899 & 1);
          int lookup_1753 = ((value1_1749 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((adctemp_1752 & 0x88) >> 1);
          value1_1749 = adctemp_1752 & 0xff;
          _F899 = ((adctemp_1752 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1753 & 0x07)] | OVERFLOW_ADD[(lookup_1753 >> 4)] | (SZ53[value1_1749] | (value1_1749 == 0 ? 0x40 : 0));
          F = (_F899 & 0xFF);
          A = value1_1749;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _F901;
          int value1_1755 = A;
          int value3_1757 = F & 1;
          _F901 = value3_1757;
          int adctemp_1758 = value1_1755 + D + (_F901 & 1);
          int lookup_1759 = ((value1_1755 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((adctemp_1758 & 0x88) >> 1);
          value1_1755 = adctemp_1758 & 0xff;
          _F901 = ((adctemp_1758 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1759 & 0x07)] | OVERFLOW_ADD[(lookup_1759 >> 4)] | (SZ53[value1_1755] | (value1_1755 == 0 ? 0x40 : 0));
          F = (_F901 & 0xFF);
          A = value1_1755;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _F903;
          int value1_1761 = A;
          int value3_1763 = F & 1;
          _F903 = value3_1763;
          int adctemp_1764 = value1_1761 + E + (_F903 & 1);
          int lookup_1765 = ((value1_1761 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((adctemp_1764 & 0x88) >> 1);
          value1_1761 = adctemp_1764 & 0xff;
          _F903 = ((adctemp_1764 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1765 & 0x07)] | OVERFLOW_ADD[(lookup_1765 >> 4)] | (SZ53[value1_1761] | (value1_1761 == 0 ? 0x40 : 0));
          F = (_F903 & 0xFF);
          A = value1_1761;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _F905;
          int value1_1767 = A;
          int value2_1768 = (IX >> 8);
          int value3_1769 = F & 1;
          _F905 = value3_1769;
          int adctemp_1770 = value1_1767 + value2_1768 + (_F905 & 1);
          int lookup_1771 = ((value1_1767 & 0x88) >> 3) | ((value2_1768 & 0x88) >> 2) | ((adctemp_1770 & 0x88) >> 1);
          value1_1767 = adctemp_1770 & 0xff;
          _F905 = ((adctemp_1770 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1771 & 0x07)] | OVERFLOW_ADD[(lookup_1771 >> 4)] | (SZ53[value1_1767] | (value1_1767 == 0 ? 0x40 : 0));
          F = (_F905 & 0xFF);
          A = value1_1767;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _F907;
          int value1_1773 = A;
          int value2_1774 = (IX & 0xFF);
          int value3_1775 = F & 1;
          _F907 = value3_1775;
          int adctemp_1776 = value1_1773 + value2_1774 + (_F907 & 1);
          int lookup_1777 = ((value1_1773 & 0x88) >> 3) | ((value2_1774 & 0x88) >> 2) | ((adctemp_1776 & 0x88) >> 1);
          value1_1773 = adctemp_1776 & 0xff;
          _F907 = ((adctemp_1776 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1777 & 0x07)] | OVERFLOW_ADD[(lookup_1777 >> 4)] | (SZ53[value1_1773] | (value1_1773 == 0 ? 0x40 : 0));
          F = (_F907 & 0xFF);
          A = value1_1773;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _F909;
          int _value795;
          int _address795;
          int operand_1779 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1779)) & 0xFFFF;
          int operand_1780 = read(_address795, 0);
          _value795 = operand_1780;
          int value1_1781 = A;
          int value3_1783 = F & 1;
          _F909 = value3_1783;
          int adctemp_1784 = value1_1781 + _value795 + (_F909 & 1);
          int lookup_1785 = ((value1_1781 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((adctemp_1784 & 0x88) >> 1);
          value1_1781 = adctemp_1784 & 0xff;
          _F909 = ((adctemp_1784 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1785 & 0x07)] | OVERFLOW_ADD[(lookup_1785 >> 4)] | (SZ53[value1_1781] | (value1_1781 == 0 ? 0x40 : 0));
          F = (_F909 & 0xFF);
          A = value1_1781;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _F911;
          int value1_1787 = A;
          int value2_1788 = A;
          int value3_1789 = F & 1;
          _F911 = value3_1789;
          int adctemp_1790 = value1_1787 + value2_1788 + (_F911 & 1);
          int lookup_1791 = ((value1_1787 & 0x88) >> 3) | ((value2_1788 & 0x88) >> 2) | ((adctemp_1790 & 0x88) >> 1);
          value1_1787 = adctemp_1790 & 0xff;
          _F911 = ((adctemp_1790 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_1791 & 0x07)] | OVERFLOW_ADD[(lookup_1791 >> 4)] | (SZ53[value1_1787] | (value1_1787 == 0 ? 0x40 : 0));
          F = (_F911 & 0xFF);
          A = value1_1787;
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
          int value1_1793 = A;
          int subtemp_1796 = value1_1793 - B;
          int lookup_1797 = ((value1_1793 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((subtemp_1796 & 0x88) >> 1);
          value1_1793 = subtemp_1796 & 0xff;
          _F913 = ((subtemp_1796 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1797 & 0x07)] | OVERFLOW_SUB[(lookup_1797 >> 4)] | (SZ53[value1_1793] | (value1_1793 == 0 ? 0x40 : 0));
          F = (_F913 & 0xFF);
          A = value1_1793;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _F915;
          int value1_1799 = A;
          int subtemp_1802 = value1_1799 - C;
          int lookup_1803 = ((value1_1799 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((subtemp_1802 & 0x88) >> 1);
          value1_1799 = subtemp_1802 & 0xff;
          _F915 = ((subtemp_1802 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1803 & 0x07)] | OVERFLOW_SUB[(lookup_1803 >> 4)] | (SZ53[value1_1799] | (value1_1799 == 0 ? 0x40 : 0));
          F = (_F915 & 0xFF);
          A = value1_1799;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _F917;
          int value1_1805 = A;
          int subtemp_1808 = value1_1805 - D;
          int lookup_1809 = ((value1_1805 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((subtemp_1808 & 0x88) >> 1);
          value1_1805 = subtemp_1808 & 0xff;
          _F917 = ((subtemp_1808 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1809 & 0x07)] | OVERFLOW_SUB[(lookup_1809 >> 4)] | (SZ53[value1_1805] | (value1_1805 == 0 ? 0x40 : 0));
          F = (_F917 & 0xFF);
          A = value1_1805;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _F919;
          int value1_1811 = A;
          int subtemp_1814 = value1_1811 - E;
          int lookup_1815 = ((value1_1811 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((subtemp_1814 & 0x88) >> 1);
          value1_1811 = subtemp_1814 & 0xff;
          _F919 = ((subtemp_1814 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1815 & 0x07)] | OVERFLOW_SUB[(lookup_1815 >> 4)] | (SZ53[value1_1811] | (value1_1811 == 0 ? 0x40 : 0));
          F = (_F919 & 0xFF);
          A = value1_1811;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _F921;
          int value1_1817 = A;
          int value2_1818 = (IX >> 8);
          int subtemp_1820 = value1_1817 - value2_1818;
          int lookup_1821 = ((value1_1817 & 0x88) >> 3) | ((value2_1818 & 0x88) >> 2) | ((subtemp_1820 & 0x88) >> 1);
          value1_1817 = subtemp_1820 & 0xff;
          _F921 = ((subtemp_1820 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1821 & 0x07)] | OVERFLOW_SUB[(lookup_1821 >> 4)] | (SZ53[value1_1817] | (value1_1817 == 0 ? 0x40 : 0));
          F = (_F921 & 0xFF);
          A = value1_1817;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _F923;
          int value1_1823 = A;
          int value2_1824 = (IX & 0xFF);
          int subtemp_1826 = value1_1823 - value2_1824;
          int lookup_1827 = ((value1_1823 & 0x88) >> 3) | ((value2_1824 & 0x88) >> 2) | ((subtemp_1826 & 0x88) >> 1);
          value1_1823 = subtemp_1826 & 0xff;
          _F923 = ((subtemp_1826 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1827 & 0x07)] | OVERFLOW_SUB[(lookup_1827 >> 4)] | (SZ53[value1_1823] | (value1_1823 == 0 ? 0x40 : 0));
          F = (_F923 & 0xFF);
          A = value1_1823;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _F925;
          int _value795;
          int _address795;
          int operand_1829 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1829)) & 0xFFFF;
          int operand_1830 = read(_address795, 0);
          _value795 = operand_1830;
          int value1_1831 = A;
          int subtemp_1834 = value1_1831 - _value795;
          int lookup_1835 = ((value1_1831 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((subtemp_1834 & 0x88) >> 1);
          value1_1831 = subtemp_1834 & 0xff;
          _F925 = ((subtemp_1834 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1835 & 0x07)] | OVERFLOW_SUB[(lookup_1835 >> 4)] | (SZ53[value1_1831] | (value1_1831 == 0 ? 0x40 : 0));
          F = (_F925 & 0xFF);
          A = value1_1831;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _F927;
          int value1_1837 = A;
          int value2_1838 = A;
          int subtemp_1840 = value1_1837 - value2_1838;
          int lookup_1841 = ((value1_1837 & 0x88) >> 3) | ((value2_1838 & 0x88) >> 2) | ((subtemp_1840 & 0x88) >> 1);
          value1_1837 = subtemp_1840 & 0xff;
          _F927 = ((subtemp_1840 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1841 & 0x07)] | OVERFLOW_SUB[(lookup_1841 >> 4)] | (SZ53[value1_1837] | (value1_1837 == 0 ? 0x40 : 0));
          F = (_F927 & 0xFF);
          A = value1_1837;
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
          int value1_1843 = A;
          int value3_1845 = F & 1;
          _F929 = value3_1845;
          int sbctemp_1846 = value1_1843 - B - (_F929 & 1);
          int lookup_1847 = ((value1_1843 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((sbctemp_1846 & 0x88) >> 1);
          value1_1843 = sbctemp_1846 & 0xff;
          _F929 = ((sbctemp_1846 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1847 & 0x07)] | OVERFLOW_SUB[(lookup_1847 >> 4)] | (SZ53[value1_1843] | (value1_1843 == 0 ? 0x40 : 0));
          F = (_F929 & 0xFF);
          A = value1_1843;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _F931;
          int value1_1849 = A;
          int value3_1851 = F & 1;
          _F931 = value3_1851;
          int sbctemp_1852 = value1_1849 - C - (_F931 & 1);
          int lookup_1853 = ((value1_1849 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((sbctemp_1852 & 0x88) >> 1);
          value1_1849 = sbctemp_1852 & 0xff;
          _F931 = ((sbctemp_1852 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1853 & 0x07)] | OVERFLOW_SUB[(lookup_1853 >> 4)] | (SZ53[value1_1849] | (value1_1849 == 0 ? 0x40 : 0));
          F = (_F931 & 0xFF);
          A = value1_1849;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _F933;
          int value1_1855 = A;
          int value3_1857 = F & 1;
          _F933 = value3_1857;
          int sbctemp_1858 = value1_1855 - D - (_F933 & 1);
          int lookup_1859 = ((value1_1855 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((sbctemp_1858 & 0x88) >> 1);
          value1_1855 = sbctemp_1858 & 0xff;
          _F933 = ((sbctemp_1858 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1859 & 0x07)] | OVERFLOW_SUB[(lookup_1859 >> 4)] | (SZ53[value1_1855] | (value1_1855 == 0 ? 0x40 : 0));
          F = (_F933 & 0xFF);
          A = value1_1855;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _F935;
          int value1_1861 = A;
          int value3_1863 = F & 1;
          _F935 = value3_1863;
          int sbctemp_1864 = value1_1861 - E - (_F935 & 1);
          int lookup_1865 = ((value1_1861 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((sbctemp_1864 & 0x88) >> 1);
          value1_1861 = sbctemp_1864 & 0xff;
          _F935 = ((sbctemp_1864 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1865 & 0x07)] | OVERFLOW_SUB[(lookup_1865 >> 4)] | (SZ53[value1_1861] | (value1_1861 == 0 ? 0x40 : 0));
          F = (_F935 & 0xFF);
          A = value1_1861;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _F937;
          int value1_1867 = A;
          int value2_1868 = (IX >> 8);
          int value3_1869 = F & 1;
          _F937 = value3_1869;
          int sbctemp_1870 = value1_1867 - value2_1868 - (_F937 & 1);
          int lookup_1871 = ((value1_1867 & 0x88) >> 3) | ((value2_1868 & 0x88) >> 2) | ((sbctemp_1870 & 0x88) >> 1);
          value1_1867 = sbctemp_1870 & 0xff;
          _F937 = ((sbctemp_1870 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1871 & 0x07)] | OVERFLOW_SUB[(lookup_1871 >> 4)] | (SZ53[value1_1867] | (value1_1867 == 0 ? 0x40 : 0));
          F = (_F937 & 0xFF);
          A = value1_1867;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _F939;
          int value1_1873 = A;
          int value2_1874 = (IX & 0xFF);
          int value3_1875 = F & 1;
          _F939 = value3_1875;
          int sbctemp_1876 = value1_1873 - value2_1874 - (_F939 & 1);
          int lookup_1877 = ((value1_1873 & 0x88) >> 3) | ((value2_1874 & 0x88) >> 2) | ((sbctemp_1876 & 0x88) >> 1);
          value1_1873 = sbctemp_1876 & 0xff;
          _F939 = ((sbctemp_1876 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1877 & 0x07)] | OVERFLOW_SUB[(lookup_1877 >> 4)] | (SZ53[value1_1873] | (value1_1873 == 0 ? 0x40 : 0));
          F = (_F939 & 0xFF);
          A = value1_1873;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _F941;
          int _value795;
          int _address795;
          int operand_1879 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1879)) & 0xFFFF;
          int operand_1880 = read(_address795, 0);
          _value795 = operand_1880;
          int value1_1881 = A;
          int value3_1883 = F & 1;
          _F941 = value3_1883;
          int sbctemp_1884 = value1_1881 - _value795 - (_F941 & 1);
          int lookup_1885 = ((value1_1881 & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((sbctemp_1884 & 0x88) >> 1);
          value1_1881 = sbctemp_1884 & 0xff;
          _F941 = ((sbctemp_1884 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1885 & 0x07)] | OVERFLOW_SUB[(lookup_1885 >> 4)] | (SZ53[value1_1881] | (value1_1881 == 0 ? 0x40 : 0));
          F = (_F941 & 0xFF);
          A = value1_1881;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _F943;
          int value1_1887 = A;
          int value2_1888 = A;
          int value3_1889 = F & 1;
          _F943 = value3_1889;
          int sbctemp_1890 = value1_1887 - value2_1888 - (_F943 & 1);
          int lookup_1891 = ((value1_1887 & 0x88) >> 3) | ((value2_1888 & 0x88) >> 2) | ((sbctemp_1890 & 0x88) >> 1);
          value1_1887 = sbctemp_1890 & 0xff;
          _F943 = ((sbctemp_1890 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_1891 & 0x07)] | OVERFLOW_SUB[(lookup_1891 >> 4)] | (SZ53[value1_1887] | (value1_1887 == 0 ? 0x40 : 0));
          F = (_F943 & 0xFF);
          A = value1_1887;
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
          int value1_1893 = A;
          int value2_1894 = B;
          value2_1894 &= value1_1893;
          _F945 = 0x10 | (SZ53P[value2_1894 & 0xff] | (value2_1894 == 0 ? 0x40 : 0));
          int result_1896 = value2_1894 & 0xFF;
          F = (_F945 & 0xFF);
          A = result_1896;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F947;
          int value1_1897 = A;
          int value2_1898 = C;
          value2_1898 &= value1_1897;
          _F947 = 0x10 | (SZ53P[value2_1898 & 0xff] | (value2_1898 == 0 ? 0x40 : 0));
          int result_1900 = value2_1898 & 0xFF;
          F = (_F947 & 0xFF);
          A = result_1900;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F949;
          int value1_1901 = A;
          int value2_1902 = D;
          value2_1902 &= value1_1901;
          _F949 = 0x10 | (SZ53P[value2_1902 & 0xff] | (value2_1902 == 0 ? 0x40 : 0));
          int result_1904 = value2_1902 & 0xFF;
          F = (_F949 & 0xFF);
          A = result_1904;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F951;
          int value1_1905 = A;
          int value2_1906 = E;
          value2_1906 &= value1_1905;
          _F951 = 0x10 | (SZ53P[value2_1906 & 0xff] | (value2_1906 == 0 ? 0x40 : 0));
          int result_1908 = value2_1906 & 0xFF;
          F = (_F951 & 0xFF);
          A = result_1908;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _F953;
          int value1_1909 = A;
          int value2_1910 = (IX >> 8);
          value2_1910 &= value1_1909;
          _F953 = 0x10 | (SZ53P[value2_1910 & 0xff] | (value2_1910 == 0 ? 0x40 : 0));
          int result_1912 = value2_1910 & 0xFF;
          F = (_F953 & 0xFF);
          A = result_1912;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _F955;
          int value1_1913 = A;
          int value2_1914 = (IX & 0xFF);
          value2_1914 &= value1_1913;
          _F955 = 0x10 | (SZ53P[value2_1914 & 0xff] | (value2_1914 == 0 ? 0x40 : 0));
          int result_1916 = value2_1914 & 0xFF;
          F = (_F955 & 0xFF);
          A = result_1916;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _F957;
          int _value795;
          int _address795;
          int operand_1917 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1917)) & 0xFFFF;
          int operand_1918 = read(_address795, 0);
          _value795 = operand_1918;
          int value1_1919 = A;
          int value2_1920 = _value795;
          value2_1920 &= value1_1919;
          _F957 = 0x10 | (SZ53P[value2_1920 & 0xff] | (value2_1920 == 0 ? 0x40 : 0));
          int result_1922 = value2_1920 & 0xFF;
          F = (_F957 & 0xFF);
          A = result_1922;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _F959;
          int value1_1923 = A;
          int value2_1924 = A;
          value2_1924 &= value1_1923;
          _F959 = 0x10 | (SZ53P[value2_1924 & 0xff] | (value2_1924 == 0 ? 0x40 : 0));
          int result_1926 = value2_1924 & 0xFF;
          F = (_F959 & 0xFF);
          A = result_1926;
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
          int value1_1927 = A;
          int value2_1928 = B;
          value2_1928 ^= value1_1927;
          _F961 = SZ53P[value2_1928 & 0xff] | (value2_1928 == 0 ? 0x40 : 0);
          int result_1930 = value2_1928 & 0xFF;
          F = (_F961 & 0xFF);
          A = result_1930;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F963;
          int value1_1931 = A;
          int value2_1932 = C;
          value2_1932 ^= value1_1931;
          _F963 = SZ53P[value2_1932 & 0xff] | (value2_1932 == 0 ? 0x40 : 0);
          int result_1934 = value2_1932 & 0xFF;
          F = (_F963 & 0xFF);
          A = result_1934;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F965;
          int value1_1935 = A;
          int value2_1936 = D;
          value2_1936 ^= value1_1935;
          _F965 = SZ53P[value2_1936 & 0xff] | (value2_1936 == 0 ? 0x40 : 0);
          int result_1938 = value2_1936 & 0xFF;
          F = (_F965 & 0xFF);
          A = result_1938;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F967;
          int value1_1939 = A;
          int value2_1940 = E;
          value2_1940 ^= value1_1939;
          _F967 = SZ53P[value2_1940 & 0xff] | (value2_1940 == 0 ? 0x40 : 0);
          int result_1942 = value2_1940 & 0xFF;
          F = (_F967 & 0xFF);
          A = result_1942;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _F969;
          int value1_1943 = A;
          int value2_1944 = (IX >> 8);
          value2_1944 ^= value1_1943;
          _F969 = SZ53P[value2_1944 & 0xff] | (value2_1944 == 0 ? 0x40 : 0);
          int result_1946 = value2_1944 & 0xFF;
          F = (_F969 & 0xFF);
          A = result_1946;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _F971;
          int value1_1947 = A;
          int value2_1948 = (IX & 0xFF);
          value2_1948 ^= value1_1947;
          _F971 = SZ53P[value2_1948 & 0xff] | (value2_1948 == 0 ? 0x40 : 0);
          int result_1950 = value2_1948 & 0xFF;
          F = (_F971 & 0xFF);
          A = result_1950;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _F973;
          int _value795;
          int _address795;
          int operand_1951 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1951)) & 0xFFFF;
          int operand_1952 = read(_address795, 0);
          _value795 = operand_1952;
          int value1_1953 = A;
          int value2_1954 = _value795;
          value2_1954 ^= value1_1953;
          _F973 = SZ53P[value2_1954 & 0xff] | (value2_1954 == 0 ? 0x40 : 0);
          int result_1956 = value2_1954 & 0xFF;
          F = (_F973 & 0xFF);
          A = result_1956;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _F975;
          int value1_1957 = A;
          int value2_1958 = A;
          value2_1958 ^= value1_1957;
          _F975 = SZ53P[value2_1958 & 0xff] | (value2_1958 == 0 ? 0x40 : 0);
          int result_1960 = value2_1958 & 0xFF;
          F = (_F975 & 0xFF);
          A = result_1960;
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
          int value1_1961 = A;
          int value2_1962 = B;
          value2_1962 |= value1_1961;
          _F977 = SZ53P[value2_1962 & 0xff] | (value2_1962 == 0 ? 0x40 : 0);
          int result_1964 = value2_1962 & 0xFF;
          F = (_F977 & 0xFF);
          A = result_1964;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _F979;
          int value1_1965 = A;
          int value2_1966 = C;
          value2_1966 |= value1_1965;
          _F979 = SZ53P[value2_1966 & 0xff] | (value2_1966 == 0 ? 0x40 : 0);
          int result_1968 = value2_1966 & 0xFF;
          F = (_F979 & 0xFF);
          A = result_1968;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _F981;
          int value1_1969 = A;
          int value2_1970 = D;
          value2_1970 |= value1_1969;
          _F981 = SZ53P[value2_1970 & 0xff] | (value2_1970 == 0 ? 0x40 : 0);
          int result_1972 = value2_1970 & 0xFF;
          F = (_F981 & 0xFF);
          A = result_1972;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _F983;
          int value1_1973 = A;
          int value2_1974 = E;
          value2_1974 |= value1_1973;
          _F983 = SZ53P[value2_1974 & 0xff] | (value2_1974 == 0 ? 0x40 : 0);
          int result_1976 = value2_1974 & 0xFF;
          F = (_F983 & 0xFF);
          A = result_1976;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _F985;
          int value1_1977 = A;
          int value2_1978 = (IX >> 8);
          value2_1978 |= value1_1977;
          _F985 = SZ53P[value2_1978 & 0xff] | (value2_1978 == 0 ? 0x40 : 0);
          int result_1980 = value2_1978 & 0xFF;
          F = (_F985 & 0xFF);
          A = result_1980;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _F987;
          int value1_1981 = A;
          int value2_1982 = (IX & 0xFF);
          value2_1982 |= value1_1981;
          _F987 = SZ53P[value2_1982 & 0xff] | (value2_1982 == 0 ? 0x40 : 0);
          int result_1984 = value2_1982 & 0xFF;
          F = (_F987 & 0xFF);
          A = result_1984;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _F989;
          int _value795;
          int _address795;
          int operand_1985 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_1985)) & 0xFFFF;
          int operand_1986 = read(_address795, 0);
          _value795 = operand_1986;
          int value1_1987 = A;
          int value2_1988 = _value795;
          value2_1988 |= value1_1987;
          _F989 = SZ53P[value2_1988 & 0xff] | (value2_1988 == 0 ? 0x40 : 0);
          int result_1990 = value2_1988 & 0xFF;
          F = (_F989 & 0xFF);
          A = result_1990;
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _F991;
          int value1_1991 = A;
          int value2_1992 = A;
          value2_1992 |= value1_1991;
          _F991 = SZ53P[value2_1992 & 0xff] | (value2_1992 == 0 ? 0x40 : 0);
          int result_1994 = value2_1992 & 0xFF;
          F = (_F991 & 0xFF);
          A = result_1994;
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
          int cptemp_1998 = A - B;
          int lookup_1999 = ((A & 0x88) >> 3) | ((B & 0x88) >> 2) | ((cptemp_1998 & 0x88) >> 1);
          _F993 = ((cptemp_1998 & 0x100) != 0 ? 1 : (cptemp_1998 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_1999 & 0x07)] | OVERFLOW_SUB[(lookup_1999 >> 4)] | (B & 0x28) | (cptemp_1998 & 0x80);
          F = (_F993 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _F995;
          int cptemp_2004 = A - C;
          int lookup_2005 = ((A & 0x88) >> 3) | ((C & 0x88) >> 2) | ((cptemp_2004 & 0x88) >> 1);
          _F995 = ((cptemp_2004 & 0x100) != 0 ? 1 : (cptemp_2004 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2005 & 0x07)] | OVERFLOW_SUB[(lookup_2005 >> 4)] | (C & 0x28) | (cptemp_2004 & 0x80);
          F = (_F995 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _F997;
          int cptemp_2010 = A - D;
          int lookup_2011 = ((A & 0x88) >> 3) | ((D & 0x88) >> 2) | ((cptemp_2010 & 0x88) >> 1);
          _F997 = ((cptemp_2010 & 0x100) != 0 ? 1 : (cptemp_2010 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2011 & 0x07)] | OVERFLOW_SUB[(lookup_2011 >> 4)] | (D & 0x28) | (cptemp_2010 & 0x80);
          F = (_F997 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _F999;
          int cptemp_2016 = A - E;
          int lookup_2017 = ((A & 0x88) >> 3) | ((E & 0x88) >> 2) | ((cptemp_2016 & 0x88) >> 1);
          _F999 = ((cptemp_2016 & 0x100) != 0 ? 1 : (cptemp_2016 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2017 & 0x07)] | OVERFLOW_SUB[(lookup_2017 >> 4)] | (E & 0x28) | (cptemp_2016 & 0x80);
          F = (_F999 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _F1001;
          int value2_2020 = (IX >> 8);
          int cptemp_2022 = A - value2_2020;
          int lookup_2023 = ((A & 0x88) >> 3) | ((value2_2020 & 0x88) >> 2) | ((cptemp_2022 & 0x88) >> 1);
          _F1001 = ((cptemp_2022 & 0x100) != 0 ? 1 : (cptemp_2022 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2023 & 0x07)] | OVERFLOW_SUB[(lookup_2023 >> 4)] | (value2_2020 & 0x28) | (cptemp_2022 & 0x80);
          F = (_F1001 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _F1003;
          int value2_2026 = (IX & 0xFF);
          int cptemp_2028 = A - value2_2026;
          int lookup_2029 = ((A & 0x88) >> 3) | ((value2_2026 & 0x88) >> 2) | ((cptemp_2028 & 0x88) >> 1);
          _F1003 = ((cptemp_2028 & 0x100) != 0 ? 1 : (cptemp_2028 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2029 & 0x07)] | OVERFLOW_SUB[(lookup_2029 >> 4)] | (value2_2026 & 0x28) | (cptemp_2028 & 0x80);
          F = (_F1003 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _F1005;
          int _value795;
          int _address795;
          int operand_2031 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address795 = (IX + (int) ((byte) operand_2031)) & 0xFFFF;
          int operand_2032 = read(_address795, 0);
          _value795 = operand_2032;
          int cptemp_2036 = A - _value795;
          int lookup_2037 = ((A & 0x88) >> 3) | ((_value795 & 0x88) >> 2) | ((cptemp_2036 & 0x88) >> 1);
          _F1005 = ((cptemp_2036 & 0x100) != 0 ? 1 : (cptemp_2036 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2037 & 0x07)] | OVERFLOW_SUB[(lookup_2037 >> 4)] | (_value795 & 0x28) | (cptemp_2036 & 0x80);
          F = (_F1005 & 0xFF);
          MEMPTR = _address795;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _F1007;
          int cptemp_2042 = A - A;
          int lookup_2043 = ((A & 0x88) >> 3) | ((A & 0x88) >> 2) | ((cptemp_2042 & 0x88) >> 1);
          _F1007 = ((cptemp_2042 & 0x100) != 0 ? 1 : (cptemp_2042 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_2043 & 0x07)] | OVERFLOW_SUB[(lookup_2043 >> 4)] | (A & 0x28) | (cptemp_2042 & 0x80);
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
          int jumpAddress2_2045 = SP;
          if ((!((F & 0x40) == 0x40))) {
              int wordNumber1_2047 = read(SP, 0);
              int wordNumber_2048 = read((SP + 1) & 0xFFFF, 0);
              int value_2046 = ((wordNumber_2048 << 8) | wordNumber1_2047);
              int wordNumber_2049 = SP;
              SP = ((wordNumber_2049 + 2) & 0xFFFF);
              jumpAddress2_2045 = value_2046;
              MEMPTR = jumpAddress2_2045;
              PC = jumpAddress2_2045;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC1: {
          int wordNumber1_2053 = read(SP, 0);
          int wordNumber_2054 = read((SP + 1) & 0xFFFF, 0);
          int value_2052 = ((wordNumber_2054 << 8) | wordNumber1_2053);
          int wordNumber_2055 = SP;
          SP = ((wordNumber_2055 + 2) & 0xFFFF);
          B = (value_2052 >>> 8);
          C = value_2052 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _jumpAddress1011 = 0;
          int address_2057 = (PC + 2) & 0xFFFF;
          int operand_2059 = read(address_2057, 0);
          int operand_2061 = read((address_2057 + 1) & 0xFFFF, 0);
          int jumpAddress2_2056 = (_jumpAddress1011 = (operand_2061 << 8) | operand_2059);
          if ((!((F & 0x40) == 0x40))) {
              _jumpAddress1011 = jumpAddress2_2056;
              MEMPTR = _jumpAddress1011;
              PC = jumpAddress2_2056;
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
          int address_2064 = (PC + 2) & 0xFFFF;
          int operand_2066 = read(address_2064, 0);
          int operand_2068 = read((address_2064 + 1) & 0xFFFF, 0);
          int jumpAddress2_2063 = (_jumpAddress1012 = (operand_2068 << 8) | operand_2066);
          _jumpAddress1012 = jumpAddress2_2063;
          _nextPC1012 = jumpAddress2_2063;
          MEMPTR = _jumpAddress1012;
          PC = _nextPC1012;
          break;
      }
      case 0xC4: {
          int _jumpAddress1013 = 0;
          int address_2070 = (PC + 2) & 0xFFFF;
          int operand_2072 = read(address_2070, 0);
          int operand_2074 = read((address_2070 + 1) & 0xFFFF, 0);
          int value_2075 = (_jumpAddress1013 = (operand_2074 << 8) | operand_2072);
          MEMPTR = value_2075;
          int jumpAddress2_2076 = (_jumpAddress1013 = (operand_2074 << 8) | operand_2072);
          if ((!((F & 0x40) == 0x40))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_2080 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_2080 >>> 8));
              write(SP, (value_2080 & 0xFF));
              _jumpAddress1013 = jumpAddress2_2076;
              MEMPTR = _jumpAddress1013;
              PC = jumpAddress2_2076;
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
          int value_2082 = ((B << 8) | C);
          write((SP + 1) & 0xFFFF, (value_2082 >>> 8));
          write(SP, (value_2082 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _F1015;
          int operand_2083 = read((PC + 2) & 0xFFFF, 0);
          int value1_2084 = A;
          int value2_2085 = operand_2083;
          int addtemp_2087 = value2_2085 + value1_2084;
          int lookup_2088 = ((value2_2085 & 0x88) >> 3) | ((value1_2084 & 0x88) >> 2) | ((addtemp_2087 & 0x88) >> 1);
          value2_2085 = addtemp_2087 & 0xff;
          _F1015 = ((addtemp_2087 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_2088 & 0x07)] | OVERFLOW_ADD[(lookup_2088 >> 4)] | (SZ53[value2_2085] | (value2_2085 == 0 ? 0x40 : 0));
          F = (_F1015 & 0xFF);
          A = value2_2085;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _nextPC1017;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_2090 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_2090 >>> 8));
          write(SP, (value_2090 & 0xFF));
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
          int jumpAddress2_2091 = SP;
          if (((F & 0x40) == 0x40)) {
              int wordNumber1_2093 = read(SP, 0);
              int wordNumber_2094 = read((SP + 1) & 0xFFFF, 0);
              int value_2092 = ((wordNumber_2094 << 8) | wordNumber1_2093);
              int wordNumber_2095 = SP;
              SP = ((wordNumber_2095 + 2) & 0xFFFF);
              jumpAddress2_2091 = value_2092;
              MEMPTR = jumpAddress2_2091;
              PC = jumpAddress2_2091;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC9: {
          int _nextPC1019;
          int jumpAddress2_2097;
          int wordNumber1_2099 = read(SP, 0);
          int wordNumber_2100 = read((SP + 1) & 0xFFFF, 0);
          int value_2098 = ((wordNumber_2100 << 8) | wordNumber1_2099);
          int wordNumber_2101 = SP;
          SP = ((wordNumber_2101 + 2) & 0xFFFF);
          jumpAddress2_2097 = value_2098;
          _nextPC1019 = jumpAddress2_2097;
          MEMPTR = _nextPC1019;
          PC = _nextPC1019;
          break;
      }
      case 0xCA: {
          int _jumpAddress1020 = 0;
          int address_2104 = (PC + 2) & 0xFFFF;
          int operand_2106 = read(address_2104, 0);
          int operand_2108 = read((address_2104 + 1) & 0xFFFF, 0);
          int jumpAddress2_2103 = (_jumpAddress1020 = (operand_2108 << 8) | operand_2106);
          if (((F & 0x40) == 0x40)) {
              _jumpAddress1020 = jumpAddress2_2103;
              MEMPTR = _jumpAddress1020;
              PC = jumpAddress2_2103;
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
          int address_3318 = (PC + 2) & 0xFFFF;
          int operand_3320 = read(address_3318, 0);
          int operand_3322 = read((address_3318 + 1) & 0xFFFF, 0);
          int value_3323 = (_jumpAddress1661 = (operand_3322 << 8) | operand_3320);
          MEMPTR = value_3323;
          int jumpAddress2_3324 = (_jumpAddress1661 = (operand_3322 << 8) | operand_3320);
          if (((F & 0x40) == 0x40)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3328 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3328 >>> 8));
              write(SP, (value_3328 & 0xFF));
              _jumpAddress1661 = jumpAddress2_3324;
              MEMPTR = _jumpAddress1661;
              PC = jumpAddress2_3324;
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
          int address_3330 = (PC + 2) & 0xFFFF;
          int operand_3332 = read(address_3330, 0);
          int operand_3334 = read((address_3330 + 1) & 0xFFFF, 0);
          int value_3335 = (_jumpAddress1662 = (operand_3334 << 8) | operand_3332);
          int jumpAddress2_3336 = (_jumpAddress1662 = (operand_3334 << 8) | operand_3332);
          SP = ((SP - 2) & 0xFFFF);
          int value_3340 = ((PC + 4) & 0xFFFF);
          contend1x1((PC + 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3340 >>> 8));
          write(SP, (value_3340 & 0xFF));
          _jumpAddress1662 = jumpAddress2_3336;
          _nextPC1662 = jumpAddress2_3336;
          MEMPTR = _jumpAddress1662;
          PC = _nextPC1662;
          break;
      }
      case 0xCE: {
          int _F1663;
          int operand_3342 = read((PC + 2) & 0xFFFF, 0);
          int value1_3343 = A;
          int value3_3345 = F & 1;
          _F1663 = value3_3345;
          int adctemp_3346 = value1_3343 + operand_3342 + (_F1663 & 1);
          int lookup_3347 = ((value1_3343 & 0x88) >> 3) | ((operand_3342 & 0x88) >> 2) | ((adctemp_3346 & 0x88) >> 1);
          value1_3343 = adctemp_3346 & 0xff;
          _F1663 = ((adctemp_3346 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_3347 & 0x07)] | OVERFLOW_ADD[(lookup_3347 >> 4)] | (SZ53[value1_3343] | (value1_3343 == 0 ? 0x40 : 0));
          F = (_F1663 & 0xFF);
          A = value1_3343;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _nextPC1665;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3349 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3349 >>> 8));
          write(SP, (value_3349 & 0xFF));
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
          int jumpAddress2_3350 = SP;
          if ((!((F & 1) == 1))) {
              int wordNumber1_3352 = read(SP, 0);
              int wordNumber_3353 = read((SP + 1) & 0xFFFF, 0);
              int value_3351 = ((wordNumber_3353 << 8) | wordNumber1_3352);
              int wordNumber_3354 = SP;
              SP = ((wordNumber_3354 + 2) & 0xFFFF);
              jumpAddress2_3350 = value_3351;
              MEMPTR = jumpAddress2_3350;
              PC = jumpAddress2_3350;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD1: {
          int wordNumber1_3358 = read(SP, 0);
          int wordNumber_3359 = read((SP + 1) & 0xFFFF, 0);
          int value_3357 = ((wordNumber_3359 << 8) | wordNumber1_3358);
          int wordNumber_3360 = SP;
          SP = ((wordNumber_3360 + 2) & 0xFFFF);
          D = (value_3357 >>> 8);
          E = value_3357 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _jumpAddress1668 = 0;
          int address_3362 = (PC + 2) & 0xFFFF;
          int operand_3364 = read(address_3362, 0);
          int operand_3366 = read((address_3362 + 1) & 0xFFFF, 0);
          int jumpAddress2_3361 = (_jumpAddress1668 = (operand_3366 << 8) | operand_3364);
          if ((!((F & 1) == 1))) {
              _jumpAddress1668 = jumpAddress2_3361;
              MEMPTR = _jumpAddress1668;
              PC = jumpAddress2_3361;
              break;
          } else {
              MEMPTR = _jumpAddress1668;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD3: {
          int operand_3369 = read((PC + 2) & 0xFFFF, 0);
          int read_3368 = operand_3369;
          read_3368 = (read_3368 | A << 8);
          io.out(read_3368, A);
          MEMPTR = (A << 8);
          int read_3370 = operand_3369;
          read_3370 = (read_3370 | A << 8);
          MEMPTR = (MEMPTR | ((read_3370 + 1) & 0xff));
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _jumpAddress1670 = 0;
          int address_3371 = (PC + 2) & 0xFFFF;
          int operand_3373 = read(address_3371, 0);
          int operand_3375 = read((address_3371 + 1) & 0xFFFF, 0);
          int value_3376 = (_jumpAddress1670 = (operand_3375 << 8) | operand_3373);
          MEMPTR = value_3376;
          int jumpAddress2_3377 = (_jumpAddress1670 = (operand_3375 << 8) | operand_3373);
          if ((!((F & 1) == 1))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3381 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3381 >>> 8));
              write(SP, (value_3381 & 0xFF));
              _jumpAddress1670 = jumpAddress2_3377;
              MEMPTR = _jumpAddress1670;
              PC = jumpAddress2_3377;
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
          int value_3383 = ((D << 8) | E);
          write((SP + 1) & 0xFFFF, (value_3383 >>> 8));
          write(SP, (value_3383 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _F1672;
          int operand_3384 = read((PC + 2) & 0xFFFF, 0);
          int value1_3385 = A;
          int subtemp_3388 = value1_3385 - operand_3384;
          int lookup_3389 = ((value1_3385 & 0x88) >> 3) | ((operand_3384 & 0x88) >> 2) | ((subtemp_3388 & 0x88) >> 1);
          value1_3385 = subtemp_3388 & 0xff;
          _F1672 = ((subtemp_3388 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3389 & 0x07)] | OVERFLOW_SUB[(lookup_3389 >> 4)] | (SZ53[value1_3385] | (value1_3385 == 0 ? 0x40 : 0));
          F = (_F1672 & 0xFF);
          A = value1_3385;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _nextPC1674;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3391 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3391 >>> 8));
          write(SP, (value_3391 & 0xFF));
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
          int jumpAddress2_3392 = SP;
          if (((F & 1) == 1)) {
              int wordNumber1_3394 = read(SP, 0);
              int wordNumber_3395 = read((SP + 1) & 0xFFFF, 0);
              int value_3393 = ((wordNumber_3395 << 8) | wordNumber1_3394);
              int wordNumber_3396 = SP;
              SP = ((wordNumber_3396 + 2) & 0xFFFF);
              jumpAddress2_3392 = value_3393;
              MEMPTR = jumpAddress2_3392;
              PC = jumpAddress2_3392;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD9: {
          int v1_3398 = ((B << 8) | C);
          B = (_BC >>> 8);
          C = _BC & 0xFF;
          _BC = v1_3398;
          v1_3398 = (D << 8) | E;
          D = (_DE >>> 8);
          E = _DE & 0xFF;
          _DE = v1_3398;
          v1_3398 = (H << 8) | L;
          H = (_HL >>> 8);
          L = _HL & 0xFF;
          _HL = v1_3398;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _jumpAddress1677 = 0;
          int address_3400 = (PC + 2) & 0xFFFF;
          int operand_3402 = read(address_3400, 0);
          int operand_3404 = read((address_3400 + 1) & 0xFFFF, 0);
          int jumpAddress2_3399 = (_jumpAddress1677 = (operand_3404 << 8) | operand_3402);
          if (((F & 1) == 1)) {
              _jumpAddress1677 = jumpAddress2_3399;
              MEMPTR = _jumpAddress1677;
              PC = jumpAddress2_3399;
              break;
          } else {
              MEMPTR = _jumpAddress1677;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDB: {
          int operand_3407 = read((PC + 2) & 0xFFFF, 0);
          int wordNumber1_3406 = (((Integer) (A << 8)) | operand_3407);
          MEMPTR = ((wordNumber1_3406 + 1) & 0xFFFF);
          int port_3408 = operand_3407;
          port_3408 = (port_3408 | A << 8);
          int value_3409 = io.in(port_3408);
          A = value_3409 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress1679 = 0;
          int address_3410 = (PC + 2) & 0xFFFF;
          int operand_3412 = read(address_3410, 0);
          int operand_3414 = read((address_3410 + 1) & 0xFFFF, 0);
          int value_3415 = (_jumpAddress1679 = (operand_3414 << 8) | operand_3412);
          MEMPTR = value_3415;
          int jumpAddress2_3416 = (_jumpAddress1679 = (operand_3414 << 8) | operand_3412);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3420 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3420 >>> 8));
              write(SP, (value_3420 & 0xFF));
              _jumpAddress1679 = jumpAddress2_3416;
              MEMPTR = _jumpAddress1679;
              PC = jumpAddress2_3416;
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
          int operand_3422 = read((PC + 2) & 0xFFFF, 0);
          int value1_3423 = A;
          int value3_3425 = F & 1;
          _F1681 = value3_3425;
          int sbctemp_3426 = value1_3423 - operand_3422 - (_F1681 & 1);
          int lookup_3427 = ((value1_3423 & 0x88) >> 3) | ((operand_3422 & 0x88) >> 2) | ((sbctemp_3426 & 0x88) >> 1);
          value1_3423 = sbctemp_3426 & 0xff;
          _F1681 = ((sbctemp_3426 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3427 & 0x07)] | OVERFLOW_SUB[(lookup_3427 >> 4)] | (SZ53[value1_3423] | (value1_3423 == 0 ? 0x40 : 0));
          F = (_F1681 & 0xFF);
          A = value1_3423;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC1683;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3429 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3429 >>> 8));
          write(SP, (value_3429 & 0xFF));
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
          int jumpAddress2_3430 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_3432 = read(SP, 0);
              int wordNumber_3433 = read((SP + 1) & 0xFFFF, 0);
              int value_3431 = ((wordNumber_3433 << 8) | wordNumber1_3432);
              int wordNumber_3434 = SP;
              SP = ((wordNumber_3434 + 2) & 0xFFFF);
              jumpAddress2_3430 = value_3431;
              MEMPTR = jumpAddress2_3430;
              PC = jumpAddress2_3430;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_3438 = read(SP, 0);
          int wordNumber_3439 = read((SP + 1) & 0xFFFF, 0);
          int value_3437 = ((wordNumber_3439 << 8) | wordNumber1_3438);
          int wordNumber_3440 = SP;
          SP = ((wordNumber_3440 + 2) & 0xFFFF);
          IX = value_3437;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress1686 = 0;
          int address_3442 = (PC + 2) & 0xFFFF;
          int operand_3444 = read(address_3442, 0);
          int operand_3446 = read((address_3442 + 1) & 0xFFFF, 0);
          int jumpAddress2_3441 = (_jumpAddress1686 = (operand_3446 << 8) | operand_3444);
          if ((!((F & 4) == 4))) {
              _jumpAddress1686 = jumpAddress2_3441;
              MEMPTR = _jumpAddress1686;
              PC = jumpAddress2_3441;
              break;
          } else {
              MEMPTR = _jumpAddress1686;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address1687 = SP;
          int wordNumber1_3449 = read(_address1687, 0);
          int wordNumber_3450 = read((_address1687 + 1) & 0xFFFF, 0);
          int v1_3448 = ((wordNumber_3450 << 8) | wordNumber1_3449);
          int v2_3451 = IX;
          _address1687 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address1687 + 1) & 0xFFFF, (v2_3451 >>> 8));
          write(_address1687, (v2_3451 & 0xFF));
          IX = v1_3448;
          MEMPTR = IX;
          contend2x1(SP);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress1689 = 0;
          int address_3452 = (PC + 2) & 0xFFFF;
          int operand_3454 = read(address_3452, 0);
          int operand_3456 = read((address_3452 + 1) & 0xFFFF, 0);
          int value_3457 = (_jumpAddress1689 = (operand_3456 << 8) | operand_3454);
          MEMPTR = value_3457;
          int jumpAddress2_3458 = (_jumpAddress1689 = (operand_3456 << 8) | operand_3454);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3462 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3462 >>> 8));
              write(SP, (value_3462 & 0xFF));
              _jumpAddress1689 = jumpAddress2_3458;
              MEMPTR = _jumpAddress1689;
              PC = jumpAddress2_3458;
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
          int operand_3464 = read((PC + 2) & 0xFFFF, 0);
          int value1_3465 = A;
          int value2_3466 = operand_3464;
          value2_3466 &= value1_3465;
          _F1691 = 0x10 | (SZ53P[value2_3466 & 0xff] | (value2_3466 == 0 ? 0x40 : 0));
          int result_3468 = value2_3466 & 0xFF;
          F = (_F1691 & 0xFF);
          A = result_3468;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC1693;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3469 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3469 >>> 8));
          write(SP, (value_3469 & 0xFF));
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
          int jumpAddress2_3470 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_3472 = read(SP, 0);
              int wordNumber_3473 = read((SP + 1) & 0xFFFF, 0);
              int value_3471 = ((wordNumber_3473 << 8) | wordNumber1_3472);
              int wordNumber_3474 = SP;
              SP = ((wordNumber_3474 + 2) & 0xFFFF);
              jumpAddress2_3470 = value_3471;
              MEMPTR = jumpAddress2_3470;
              PC = jumpAddress2_3470;
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
          int address_3479 = (PC + 2) & 0xFFFF;
          int operand_3481 = read(address_3479, 0);
          int operand_3483 = read((address_3479 + 1) & 0xFFFF, 0);
          int jumpAddress2_3478 = (_jumpAddress1696 = (operand_3483 << 8) | operand_3481);
          if (((F & 4) == 4)) {
              _jumpAddress1696 = jumpAddress2_3478;
              MEMPTR = _jumpAddress1696;
              PC = jumpAddress2_3478;
              break;
          } else {
              MEMPTR = _jumpAddress1696;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_3485 = ((D << 8) | E);
          int v2_3486 = ((H << 8) | L);
          D = (v2_3486 >>> 8);
          E = v2_3486 & 0xFF;
          H = (v1_3485 >>> 8);
          L = v1_3485 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress1698 = 0;
          int address_3487 = (PC + 2) & 0xFFFF;
          int operand_3489 = read(address_3487, 0);
          int operand_3491 = read((address_3487 + 1) & 0xFFFF, 0);
          int value_3492 = (_jumpAddress1698 = (operand_3491 << 8) | operand_3489);
          MEMPTR = value_3492;
          int jumpAddress2_3493 = (_jumpAddress1698 = (operand_3491 << 8) | operand_3489);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3497 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3497 >>> 8));
              write(SP, (value_3497 & 0xFF));
              _jumpAddress1698 = jumpAddress2_3493;
              MEMPTR = _jumpAddress1698;
              PC = jumpAddress2_3493;
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
          int operand_3499 = read((PC + 2) & 0xFFFF, 0);
          int value1_3500 = A;
          int value2_3501 = operand_3499;
          value2_3501 ^= value1_3500;
          _F1700 = SZ53P[value2_3501 & 0xff] | (value2_3501 == 0 ? 0x40 : 0);
          int result_3503 = value2_3501 & 0xFF;
          F = (_F1700 & 0xFF);
          A = result_3503;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC1702;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3504 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3504 >>> 8));
          write(SP, (value_3504 & 0xFF));
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
          int jumpAddress2_3505 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_3507 = read(SP, 0);
              int wordNumber_3508 = read((SP + 1) & 0xFFFF, 0);
              int value_3506 = ((wordNumber_3508 << 8) | wordNumber1_3507);
              int wordNumber_3509 = SP;
              SP = ((wordNumber_3509 + 2) & 0xFFFF);
              jumpAddress2_3505 = value_3506;
              MEMPTR = jumpAddress2_3505;
              PC = jumpAddress2_3505;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_3513 = read(SP, 0);
          int wordNumber_3514 = read((SP + 1) & 0xFFFF, 0);
          int value_3512 = ((wordNumber_3514 << 8) | wordNumber1_3513);
          int wordNumber_3515 = SP;
          SP = ((wordNumber_3515 + 2) & 0xFFFF);
          A = (value_3512 >>> 8);
          F = value_3512 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress1705 = 0;
          int address_3517 = (PC + 2) & 0xFFFF;
          int operand_3519 = read(address_3517, 0);
          int operand_3521 = read((address_3517 + 1) & 0xFFFF, 0);
          int jumpAddress2_3516 = (_jumpAddress1705 = (operand_3521 << 8) | operand_3519);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress1705 = jumpAddress2_3516;
              MEMPTR = _jumpAddress1705;
              PC = jumpAddress2_3516;
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
          int address_3523 = (PC + 2) & 0xFFFF;
          int operand_3525 = read(address_3523, 0);
          int operand_3527 = read((address_3523 + 1) & 0xFFFF, 0);
          int value_3528 = (_jumpAddress1707 = (operand_3527 << 8) | operand_3525);
          MEMPTR = value_3528;
          int jumpAddress2_3529 = (_jumpAddress1707 = (operand_3527 << 8) | operand_3525);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3533 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3533 >>> 8));
              write(SP, (value_3533 & 0xFF));
              _jumpAddress1707 = jumpAddress2_3529;
              MEMPTR = _jumpAddress1707;
              PC = jumpAddress2_3529;
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
          int value_3535 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_3535 >>> 8));
          write(SP, (value_3535 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F1709;
          int operand_3536 = read((PC + 2) & 0xFFFF, 0);
          int value1_3537 = A;
          int value2_3538 = operand_3536;
          value2_3538 |= value1_3537;
          _F1709 = SZ53P[value2_3538 & 0xff] | (value2_3538 == 0 ? 0x40 : 0);
          int result_3540 = value2_3538 & 0xFF;
          F = (_F1709 & 0xFF);
          A = result_3540;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC1711;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3541 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3541 >>> 8));
          write(SP, (value_3541 & 0xFF));
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
          int jumpAddress2_3542 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_3544 = read(SP, 0);
              int wordNumber_3545 = read((SP + 1) & 0xFFFF, 0);
              int value_3543 = ((wordNumber_3545 << 8) | wordNumber1_3544);
              int wordNumber_3546 = SP;
              SP = ((wordNumber_3546 + 2) & 0xFFFF);
              jumpAddress2_3542 = value_3543;
              MEMPTR = jumpAddress2_3542;
              PC = jumpAddress2_3542;
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
          int address_3549 = (PC + 2) & 0xFFFF;
          int operand_3551 = read(address_3549, 0);
          int operand_3553 = read((address_3549 + 1) & 0xFFFF, 0);
          int jumpAddress2_3548 = (_jumpAddress1714 = (operand_3553 << 8) | operand_3551);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress1714 = jumpAddress2_3548;
              MEMPTR = _jumpAddress1714;
              PC = jumpAddress2_3548;
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
          int address_3555 = (PC + 2) & 0xFFFF;
          int operand_3557 = read(address_3555, 0);
          int operand_3559 = read((address_3555 + 1) & 0xFFFF, 0);
          int value_3560 = (_jumpAddress1716 = (operand_3559 << 8) | operand_3557);
          MEMPTR = value_3560;
          int jumpAddress2_3561 = (_jumpAddress1716 = (operand_3559 << 8) | operand_3557);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_3565 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_3565 >>> 8));
              write(SP, (value_3565 & 0xFF));
              _jumpAddress1716 = jumpAddress2_3561;
              MEMPTR = _jumpAddress1716;
              PC = jumpAddress2_3561;
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
          int operand_3567 = read((PC + 2) & 0xFFFF, 0);
          int cptemp_3571 = A - operand_3567;
          int lookup_3572 = ((A & 0x88) >> 3) | ((operand_3567 & 0x88) >> 2) | ((cptemp_3571 & 0x88) >> 1);
          _F1718 = ((cptemp_3571 & 0x100) != 0 ? 1 : (cptemp_3571 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_3572 & 0x07)] | OVERFLOW_SUB[(lookup_3572 >> 4)] | (operand_3567 & 0x28) | (cptemp_3571 & 0x80);
          F = (_F1718 & 0xFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC1720;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_3574 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_3574 >>> 8));
          write(SP, (value_3574 & 0xFF));
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
          int operand_2110 = read(_address1021, 0);
          contend1x1(_address1021);
          _value1021 = operand_2110;
          int value1_2111 = _value1021;
          value1_2111 = (value1_2111 << 1 | value1_2111 >> 7) & 0xff;
          _F1022 = (value1_2111 & 1) | (SZ53P[value1_2111] | (value1_2111 == 0 ? 0x40 : 0));
          F = (_F1022 & 0xFF);
          _address1021 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1021 = value1_2111;
          write(_address1021, value1_2111);
          int read_2115;
          read_2115 = _value1021;
          B = read_2115;
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
          int operand_2116 = read(_address1024, 0);
          contend1x1(_address1024);
          _value1024 = operand_2116;
          int value1_2117 = _value1024;
          value1_2117 = (value1_2117 << 1 | value1_2117 >> 7) & 0xff;
          _F1025 = (value1_2117 & 1) | (SZ53P[value1_2117] | (value1_2117 == 0 ? 0x40 : 0));
          F = (_F1025 & 0xFF);
          _address1024 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1024 = value1_2117;
          write(_address1024, value1_2117);
          int read_2121;
          read_2121 = _value1024;
          C = read_2121;
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
          int operand_2122 = read(_address1027, 0);
          contend1x1(_address1027);
          _value1027 = operand_2122;
          int value1_2123 = _value1027;
          value1_2123 = (value1_2123 << 1 | value1_2123 >> 7) & 0xff;
          _F1028 = (value1_2123 & 1) | (SZ53P[value1_2123] | (value1_2123 == 0 ? 0x40 : 0));
          F = (_F1028 & 0xFF);
          _address1027 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1027 = value1_2123;
          write(_address1027, value1_2123);
          int read_2127;
          read_2127 = _value1027;
          D = read_2127;
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
          int operand_2128 = read(_address1030, 0);
          contend1x1(_address1030);
          _value1030 = operand_2128;
          int value1_2129 = _value1030;
          value1_2129 = (value1_2129 << 1 | value1_2129 >> 7) & 0xff;
          _F1031 = (value1_2129 & 1) | (SZ53P[value1_2129] | (value1_2129 == 0 ? 0x40 : 0));
          F = (_F1031 & 0xFF);
          _address1030 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1030 = value1_2129;
          write(_address1030, value1_2129);
          int read_2133;
          read_2133 = _value1030;
          E = read_2133;
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
          int operand_2134 = read(_address1033, 0);
          contend1x1(_address1033);
          _value1033 = operand_2134;
          int value1_2135 = _value1033;
          value1_2135 = (value1_2135 << 1 | value1_2135 >> 7) & 0xff;
          _F1034 = (value1_2135 & 1) | (SZ53P[value1_2135] | (value1_2135 == 0 ? 0x40 : 0));
          F = (_F1034 & 0xFF);
          _address1033 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1033 = value1_2135;
          write(_address1033, value1_2135);
          int read_2139;
          read_2139 = _value1033;
          H = read_2139;
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
          int operand_2140 = read(_address1036, 0);
          contend1x1(_address1036);
          _value1036 = operand_2140;
          int value1_2141 = _value1036;
          value1_2141 = (value1_2141 << 1 | value1_2141 >> 7) & 0xff;
          _F1037 = (value1_2141 & 1) | (SZ53P[value1_2141] | (value1_2141 == 0 ? 0x40 : 0));
          F = (_F1037 & 0xFF);
          _address1036 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1036 = value1_2141;
          write(_address1036, value1_2141);
          int read_2145;
          read_2145 = _value1036;
          L = read_2145;
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
          int operand_2146 = read(_address1039, 0);
          contend1x1(_address1039);
          _value1039 = operand_2146;
          int value1_2147 = _value1039;
          value1_2147 = (value1_2147 << 1 | value1_2147 >> 7) & 0xff;
          _F1040 = (value1_2147 & 1) | (SZ53P[value1_2147] | (value1_2147 == 0 ? 0x40 : 0));
          F = (_F1040 & 0xFF);
          _address1039 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1039 = value1_2147;
          write(_address1039, value1_2147);
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
          int operand_2151 = read(_address1042, 0);
          contend1x1(_address1042);
          _value1042 = operand_2151;
          int value1_2152 = _value1042;
          value1_2152 = (value1_2152 << 1 | value1_2152 >> 7) & 0xff;
          _F1043 = (value1_2152 & 1) | (SZ53P[value1_2152] | (value1_2152 == 0 ? 0x40 : 0));
          F = (_F1043 & 0xFF);
          _address1042 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1042 = value1_2152;
          write(_address1042, value1_2152);
          int read_2156;
          read_2156 = _value1042;
          A = read_2156;
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
          int operand_2157 = read(_address1045, 0);
          contend1x1(_address1045);
          _value1045 = operand_2157;
          int value1_2158 = _value1045;
          _F1046 = value1_2158 & 1;
          value1_2158 = (value1_2158 >> 1) | (value1_2158 << 7);
          value1_2158 &= 0xff;
          _F1046 |= (SZ53P[value1_2158 & 0xff] | (value1_2158 == 0 ? 0x40 : 0));
          int result_2161 = value1_2158 & 0xFF;
          F = (_F1046 & 0xFF);
          _address1045 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1045 = result_2161;
          write(_address1045, result_2161);
          int read_2162;
          read_2162 = _value1045;
          B = read_2162;
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
          int operand_2163 = read(_address1048, 0);
          contend1x1(_address1048);
          _value1048 = operand_2163;
          int value1_2164 = _value1048;
          _F1049 = value1_2164 & 1;
          value1_2164 = (value1_2164 >> 1) | (value1_2164 << 7);
          value1_2164 &= 0xff;
          _F1049 |= (SZ53P[value1_2164 & 0xff] | (value1_2164 == 0 ? 0x40 : 0));
          int result_2167 = value1_2164 & 0xFF;
          F = (_F1049 & 0xFF);
          _address1048 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1048 = result_2167;
          write(_address1048, result_2167);
          int read_2168;
          read_2168 = _value1048;
          C = read_2168;
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
          int operand_2169 = read(_address1051, 0);
          contend1x1(_address1051);
          _value1051 = operand_2169;
          int value1_2170 = _value1051;
          _F1052 = value1_2170 & 1;
          value1_2170 = (value1_2170 >> 1) | (value1_2170 << 7);
          value1_2170 &= 0xff;
          _F1052 |= (SZ53P[value1_2170 & 0xff] | (value1_2170 == 0 ? 0x40 : 0));
          int result_2173 = value1_2170 & 0xFF;
          F = (_F1052 & 0xFF);
          _address1051 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1051 = result_2173;
          write(_address1051, result_2173);
          int read_2174;
          read_2174 = _value1051;
          D = read_2174;
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
          int operand_2175 = read(_address1054, 0);
          contend1x1(_address1054);
          _value1054 = operand_2175;
          int value1_2176 = _value1054;
          _F1055 = value1_2176 & 1;
          value1_2176 = (value1_2176 >> 1) | (value1_2176 << 7);
          value1_2176 &= 0xff;
          _F1055 |= (SZ53P[value1_2176 & 0xff] | (value1_2176 == 0 ? 0x40 : 0));
          int result_2179 = value1_2176 & 0xFF;
          F = (_F1055 & 0xFF);
          _address1054 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1054 = result_2179;
          write(_address1054, result_2179);
          int read_2180;
          read_2180 = _value1054;
          E = read_2180;
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
          int operand_2181 = read(_address1057, 0);
          contend1x1(_address1057);
          _value1057 = operand_2181;
          int value1_2182 = _value1057;
          _F1058 = value1_2182 & 1;
          value1_2182 = (value1_2182 >> 1) | (value1_2182 << 7);
          value1_2182 &= 0xff;
          _F1058 |= (SZ53P[value1_2182 & 0xff] | (value1_2182 == 0 ? 0x40 : 0));
          int result_2185 = value1_2182 & 0xFF;
          F = (_F1058 & 0xFF);
          _address1057 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1057 = result_2185;
          write(_address1057, result_2185);
          int read_2186;
          read_2186 = _value1057;
          H = read_2186;
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
          int operand_2187 = read(_address1060, 0);
          contend1x1(_address1060);
          _value1060 = operand_2187;
          int value1_2188 = _value1060;
          _F1061 = value1_2188 & 1;
          value1_2188 = (value1_2188 >> 1) | (value1_2188 << 7);
          value1_2188 &= 0xff;
          _F1061 |= (SZ53P[value1_2188 & 0xff] | (value1_2188 == 0 ? 0x40 : 0));
          int result_2191 = value1_2188 & 0xFF;
          F = (_F1061 & 0xFF);
          _address1060 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1060 = result_2191;
          write(_address1060, result_2191);
          int read_2192;
          read_2192 = _value1060;
          L = read_2192;
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
          int operand_2193 = read(_address1063, 0);
          contend1x1(_address1063);
          _value1063 = operand_2193;
          int value1_2194 = _value1063;
          _F1064 = value1_2194 & 1;
          value1_2194 = (value1_2194 >> 1) | (value1_2194 << 7);
          value1_2194 &= 0xff;
          _F1064 |= (SZ53P[value1_2194 & 0xff] | (value1_2194 == 0 ? 0x40 : 0));
          int result_2197 = value1_2194 & 0xFF;
          F = (_F1064 & 0xFF);
          _address1063 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1063 = result_2197;
          write(_address1063, result_2197);
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
          int operand_2198 = read(_address1066, 0);
          contend1x1(_address1066);
          _value1066 = operand_2198;
          int value1_2199 = _value1066;
          _F1067 = value1_2199 & 1;
          value1_2199 = (value1_2199 >> 1) | (value1_2199 << 7);
          value1_2199 &= 0xff;
          _F1067 |= (SZ53P[value1_2199 & 0xff] | (value1_2199 == 0 ? 0x40 : 0));
          int result_2202 = value1_2199 & 0xFF;
          F = (_F1067 & 0xFF);
          _address1066 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1066 = result_2202;
          write(_address1066, result_2202);
          int read_2203;
          read_2203 = _value1066;
          A = read_2203;
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
          int operand_2204 = read(_address1069, 0);
          contend1x1(_address1069);
          _value1069 = operand_2204;
          int value1_2205 = _value1069;
          int value2_2206 = F;
          _F1070 = value2_2206;
          int rltemp_2208 = value1_2205;
          value1_2205 = (value1_2205 << 1) | (_F1070 & 1);
          value1_2205 &= 0xff;
          _F1070 = (rltemp_2208 >> 7) | (SZ53P[value1_2205 & 0xff] | (value1_2205 == 0 ? 0x40 : 0));
          int result_2209 = value1_2205 & 0xFF;
          F = (_F1070 & 0xFF);
          _address1069 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1069 = result_2209;
          write(_address1069, result_2209);
          int read_2210;
          read_2210 = _value1069;
          B = read_2210;
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
          int operand_2211 = read(_address1072, 0);
          contend1x1(_address1072);
          _value1072 = operand_2211;
          int value1_2212 = _value1072;
          int value2_2213 = F;
          _F1073 = value2_2213;
          int rltemp_2215 = value1_2212;
          value1_2212 = (value1_2212 << 1) | (_F1073 & 1);
          value1_2212 &= 0xff;
          _F1073 = (rltemp_2215 >> 7) | (SZ53P[value1_2212 & 0xff] | (value1_2212 == 0 ? 0x40 : 0));
          int result_2216 = value1_2212 & 0xFF;
          F = (_F1073 & 0xFF);
          _address1072 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1072 = result_2216;
          write(_address1072, result_2216);
          int read_2217;
          read_2217 = _value1072;
          C = read_2217;
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
          int operand_2218 = read(_address1075, 0);
          contend1x1(_address1075);
          _value1075 = operand_2218;
          int value1_2219 = _value1075;
          int value2_2220 = F;
          _F1076 = value2_2220;
          int rltemp_2222 = value1_2219;
          value1_2219 = (value1_2219 << 1) | (_F1076 & 1);
          value1_2219 &= 0xff;
          _F1076 = (rltemp_2222 >> 7) | (SZ53P[value1_2219 & 0xff] | (value1_2219 == 0 ? 0x40 : 0));
          int result_2223 = value1_2219 & 0xFF;
          F = (_F1076 & 0xFF);
          _address1075 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1075 = result_2223;
          write(_address1075, result_2223);
          int read_2224;
          read_2224 = _value1075;
          D = read_2224;
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
          int operand_2225 = read(_address1078, 0);
          contend1x1(_address1078);
          _value1078 = operand_2225;
          int value1_2226 = _value1078;
          int value2_2227 = F;
          _F1079 = value2_2227;
          int rltemp_2229 = value1_2226;
          value1_2226 = (value1_2226 << 1) | (_F1079 & 1);
          value1_2226 &= 0xff;
          _F1079 = (rltemp_2229 >> 7) | (SZ53P[value1_2226 & 0xff] | (value1_2226 == 0 ? 0x40 : 0));
          int result_2230 = value1_2226 & 0xFF;
          F = (_F1079 & 0xFF);
          _address1078 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1078 = result_2230;
          write(_address1078, result_2230);
          int read_2231;
          read_2231 = _value1078;
          E = read_2231;
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
          int operand_2232 = read(_address1081, 0);
          contend1x1(_address1081);
          _value1081 = operand_2232;
          int value1_2233 = _value1081;
          int value2_2234 = F;
          _F1082 = value2_2234;
          int rltemp_2236 = value1_2233;
          value1_2233 = (value1_2233 << 1) | (_F1082 & 1);
          value1_2233 &= 0xff;
          _F1082 = (rltemp_2236 >> 7) | (SZ53P[value1_2233 & 0xff] | (value1_2233 == 0 ? 0x40 : 0));
          int result_2237 = value1_2233 & 0xFF;
          F = (_F1082 & 0xFF);
          _address1081 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1081 = result_2237;
          write(_address1081, result_2237);
          int read_2238;
          read_2238 = _value1081;
          H = read_2238;
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
          int operand_2239 = read(_address1084, 0);
          contend1x1(_address1084);
          _value1084 = operand_2239;
          int value1_2240 = _value1084;
          int value2_2241 = F;
          _F1085 = value2_2241;
          int rltemp_2243 = value1_2240;
          value1_2240 = (value1_2240 << 1) | (_F1085 & 1);
          value1_2240 &= 0xff;
          _F1085 = (rltemp_2243 >> 7) | (SZ53P[value1_2240 & 0xff] | (value1_2240 == 0 ? 0x40 : 0));
          int result_2244 = value1_2240 & 0xFF;
          F = (_F1085 & 0xFF);
          _address1084 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1084 = result_2244;
          write(_address1084, result_2244);
          int read_2245;
          read_2245 = _value1084;
          L = read_2245;
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
          int operand_2246 = read(_address1087, 0);
          contend1x1(_address1087);
          _value1087 = operand_2246;
          int value1_2247 = _value1087;
          int value2_2248 = F;
          _F1088 = value2_2248;
          int rltemp_2250 = value1_2247;
          value1_2247 = (value1_2247 << 1) | (_F1088 & 1);
          value1_2247 &= 0xff;
          _F1088 = (rltemp_2250 >> 7) | (SZ53P[value1_2247 & 0xff] | (value1_2247 == 0 ? 0x40 : 0));
          int result_2251 = value1_2247 & 0xFF;
          F = (_F1088 & 0xFF);
          _address1087 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1087 = result_2251;
          write(_address1087, result_2251);
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
          int operand_2252 = read(_address1090, 0);
          contend1x1(_address1090);
          _value1090 = operand_2252;
          int value1_2253 = _value1090;
          int value2_2254 = F;
          _F1091 = value2_2254;
          int rltemp_2256 = value1_2253;
          value1_2253 = (value1_2253 << 1) | (_F1091 & 1);
          value1_2253 &= 0xff;
          _F1091 = (rltemp_2256 >> 7) | (SZ53P[value1_2253 & 0xff] | (value1_2253 == 0 ? 0x40 : 0));
          int result_2257 = value1_2253 & 0xFF;
          F = (_F1091 & 0xFF);
          _address1090 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1090 = result_2257;
          write(_address1090, result_2257);
          int read_2258;
          read_2258 = _value1090;
          A = read_2258;
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
          int operand_2259 = read(_address1093, 0);
          contend1x1(_address1093);
          _value1093 = operand_2259;
          int value1_2260 = _value1093;
          int value2_2261 = F;
          _F1094 = value2_2261;
          int rrtemp_2263 = value1_2260;
          value1_2260 = (value1_2260 >> 1) | (_F1094 << 7);
          value1_2260 &= 0xff;
          _F1094 = (rrtemp_2263 & 1) | (SZ53P[value1_2260 & 0xff] | (value1_2260 == 0 ? 0x40 : 0));
          int result_2264 = value1_2260 & 0xFF;
          F = (_F1094 & 0xFF);
          _address1093 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1093 = result_2264;
          write(_address1093, result_2264);
          int read_2265;
          read_2265 = _value1093;
          B = read_2265;
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
          int operand_2266 = read(_address1096, 0);
          contend1x1(_address1096);
          _value1096 = operand_2266;
          int value1_2267 = _value1096;
          int value2_2268 = F;
          _F1097 = value2_2268;
          int rrtemp_2270 = value1_2267;
          value1_2267 = (value1_2267 >> 1) | (_F1097 << 7);
          value1_2267 &= 0xff;
          _F1097 = (rrtemp_2270 & 1) | (SZ53P[value1_2267 & 0xff] | (value1_2267 == 0 ? 0x40 : 0));
          int result_2271 = value1_2267 & 0xFF;
          F = (_F1097 & 0xFF);
          _address1096 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1096 = result_2271;
          write(_address1096, result_2271);
          int read_2272;
          read_2272 = _value1096;
          C = read_2272;
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
          int operand_2273 = read(_address1099, 0);
          contend1x1(_address1099);
          _value1099 = operand_2273;
          int value1_2274 = _value1099;
          int value2_2275 = F;
          _F1100 = value2_2275;
          int rrtemp_2277 = value1_2274;
          value1_2274 = (value1_2274 >> 1) | (_F1100 << 7);
          value1_2274 &= 0xff;
          _F1100 = (rrtemp_2277 & 1) | (SZ53P[value1_2274 & 0xff] | (value1_2274 == 0 ? 0x40 : 0));
          int result_2278 = value1_2274 & 0xFF;
          F = (_F1100 & 0xFF);
          _address1099 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1099 = result_2278;
          write(_address1099, result_2278);
          int read_2279;
          read_2279 = _value1099;
          D = read_2279;
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
          int operand_2280 = read(_address1102, 0);
          contend1x1(_address1102);
          _value1102 = operand_2280;
          int value1_2281 = _value1102;
          int value2_2282 = F;
          _F1103 = value2_2282;
          int rrtemp_2284 = value1_2281;
          value1_2281 = (value1_2281 >> 1) | (_F1103 << 7);
          value1_2281 &= 0xff;
          _F1103 = (rrtemp_2284 & 1) | (SZ53P[value1_2281 & 0xff] | (value1_2281 == 0 ? 0x40 : 0));
          int result_2285 = value1_2281 & 0xFF;
          F = (_F1103 & 0xFF);
          _address1102 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1102 = result_2285;
          write(_address1102, result_2285);
          int read_2286;
          read_2286 = _value1102;
          E = read_2286;
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
          int operand_2287 = read(_address1105, 0);
          contend1x1(_address1105);
          _value1105 = operand_2287;
          int value1_2288 = _value1105;
          int value2_2289 = F;
          _F1106 = value2_2289;
          int rrtemp_2291 = value1_2288;
          value1_2288 = (value1_2288 >> 1) | (_F1106 << 7);
          value1_2288 &= 0xff;
          _F1106 = (rrtemp_2291 & 1) | (SZ53P[value1_2288 & 0xff] | (value1_2288 == 0 ? 0x40 : 0));
          int result_2292 = value1_2288 & 0xFF;
          F = (_F1106 & 0xFF);
          _address1105 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1105 = result_2292;
          write(_address1105, result_2292);
          int read_2293;
          read_2293 = _value1105;
          H = read_2293;
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
          int operand_2294 = read(_address1108, 0);
          contend1x1(_address1108);
          _value1108 = operand_2294;
          int value1_2295 = _value1108;
          int value2_2296 = F;
          _F1109 = value2_2296;
          int rrtemp_2298 = value1_2295;
          value1_2295 = (value1_2295 >> 1) | (_F1109 << 7);
          value1_2295 &= 0xff;
          _F1109 = (rrtemp_2298 & 1) | (SZ53P[value1_2295 & 0xff] | (value1_2295 == 0 ? 0x40 : 0));
          int result_2299 = value1_2295 & 0xFF;
          F = (_F1109 & 0xFF);
          _address1108 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1108 = result_2299;
          write(_address1108, result_2299);
          int read_2300;
          read_2300 = _value1108;
          L = read_2300;
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
          int operand_2301 = read(_address1111, 0);
          contend1x1(_address1111);
          _value1111 = operand_2301;
          int value1_2302 = _value1111;
          int value2_2303 = F;
          _F1112 = value2_2303;
          int rrtemp_2305 = value1_2302;
          value1_2302 = (value1_2302 >> 1) | (_F1112 << 7);
          value1_2302 &= 0xff;
          _F1112 = (rrtemp_2305 & 1) | (SZ53P[value1_2302 & 0xff] | (value1_2302 == 0 ? 0x40 : 0));
          int result_2306 = value1_2302 & 0xFF;
          F = (_F1112 & 0xFF);
          _address1111 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1111 = result_2306;
          write(_address1111, result_2306);
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
          int operand_2307 = read(_address1114, 0);
          contend1x1(_address1114);
          _value1114 = operand_2307;
          int value1_2308 = _value1114;
          int value2_2309 = F;
          _F1115 = value2_2309;
          int rrtemp_2311 = value1_2308;
          value1_2308 = (value1_2308 >> 1) | (_F1115 << 7);
          value1_2308 &= 0xff;
          _F1115 = (rrtemp_2311 & 1) | (SZ53P[value1_2308 & 0xff] | (value1_2308 == 0 ? 0x40 : 0));
          int result_2312 = value1_2308 & 0xFF;
          F = (_F1115 & 0xFF);
          _address1114 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1114 = result_2312;
          write(_address1114, result_2312);
          int read_2313;
          read_2313 = _value1114;
          A = read_2313;
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
          int operand_2314 = read(_address1117, 0);
          contend1x1(_address1117);
          _value1117 = operand_2314;
          int value1_2315 = _value1117;
          _F1118 = value1_2315 >> 7;
          value1_2315 <<= 1;
          value1_2315 &= 0xff;
          _F1118 |= (SZ53P[value1_2315 & 0xff] | (value1_2315 == 0 ? 0x40 : 0));
          int result_2318 = value1_2315 & 0xFF;
          F = (_F1118 & 0xFF);
          _address1117 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1117 = result_2318;
          write(_address1117, result_2318);
          int read_2319;
          read_2319 = _value1117;
          B = read_2319;
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
          int operand_2320 = read(_address1120, 0);
          contend1x1(_address1120);
          _value1120 = operand_2320;
          int value1_2321 = _value1120;
          _F1121 = value1_2321 >> 7;
          value1_2321 <<= 1;
          value1_2321 &= 0xff;
          _F1121 |= (SZ53P[value1_2321 & 0xff] | (value1_2321 == 0 ? 0x40 : 0));
          int result_2324 = value1_2321 & 0xFF;
          F = (_F1121 & 0xFF);
          _address1120 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1120 = result_2324;
          write(_address1120, result_2324);
          int read_2325;
          read_2325 = _value1120;
          C = read_2325;
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
          int operand_2326 = read(_address1123, 0);
          contend1x1(_address1123);
          _value1123 = operand_2326;
          int value1_2327 = _value1123;
          _F1124 = value1_2327 >> 7;
          value1_2327 <<= 1;
          value1_2327 &= 0xff;
          _F1124 |= (SZ53P[value1_2327 & 0xff] | (value1_2327 == 0 ? 0x40 : 0));
          int result_2330 = value1_2327 & 0xFF;
          F = (_F1124 & 0xFF);
          _address1123 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1123 = result_2330;
          write(_address1123, result_2330);
          int read_2331;
          read_2331 = _value1123;
          D = read_2331;
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
          int operand_2332 = read(_address1126, 0);
          contend1x1(_address1126);
          _value1126 = operand_2332;
          int value1_2333 = _value1126;
          _F1127 = value1_2333 >> 7;
          value1_2333 <<= 1;
          value1_2333 &= 0xff;
          _F1127 |= (SZ53P[value1_2333 & 0xff] | (value1_2333 == 0 ? 0x40 : 0));
          int result_2336 = value1_2333 & 0xFF;
          F = (_F1127 & 0xFF);
          _address1126 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1126 = result_2336;
          write(_address1126, result_2336);
          int read_2337;
          read_2337 = _value1126;
          E = read_2337;
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
          int operand_2338 = read(_address1129, 0);
          contend1x1(_address1129);
          _value1129 = operand_2338;
          int value1_2339 = _value1129;
          _F1130 = value1_2339 >> 7;
          value1_2339 <<= 1;
          value1_2339 &= 0xff;
          _F1130 |= (SZ53P[value1_2339 & 0xff] | (value1_2339 == 0 ? 0x40 : 0));
          int result_2342 = value1_2339 & 0xFF;
          F = (_F1130 & 0xFF);
          _address1129 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1129 = result_2342;
          write(_address1129, result_2342);
          int read_2343;
          read_2343 = _value1129;
          H = read_2343;
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
          int operand_2344 = read(_address1132, 0);
          contend1x1(_address1132);
          _value1132 = operand_2344;
          int value1_2345 = _value1132;
          _F1133 = value1_2345 >> 7;
          value1_2345 <<= 1;
          value1_2345 &= 0xff;
          _F1133 |= (SZ53P[value1_2345 & 0xff] | (value1_2345 == 0 ? 0x40 : 0));
          int result_2348 = value1_2345 & 0xFF;
          F = (_F1133 & 0xFF);
          _address1132 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1132 = result_2348;
          write(_address1132, result_2348);
          int read_2349;
          read_2349 = _value1132;
          L = read_2349;
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
          int operand_2350 = read(_address1135, 0);
          contend1x1(_address1135);
          _value1135 = operand_2350;
          int value1_2351 = _value1135;
          _F1136 = value1_2351 >> 7;
          value1_2351 <<= 1;
          value1_2351 &= 0xff;
          _F1136 |= (SZ53P[value1_2351 & 0xff] | (value1_2351 == 0 ? 0x40 : 0));
          int result_2354 = value1_2351 & 0xFF;
          F = (_F1136 & 0xFF);
          _address1135 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1135 = result_2354;
          write(_address1135, result_2354);
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
          int operand_2355 = read(_address1138, 0);
          contend1x1(_address1138);
          _value1138 = operand_2355;
          int value1_2356 = _value1138;
          _F1139 = value1_2356 >> 7;
          value1_2356 <<= 1;
          value1_2356 &= 0xff;
          _F1139 |= (SZ53P[value1_2356 & 0xff] | (value1_2356 == 0 ? 0x40 : 0));
          int result_2359 = value1_2356 & 0xFF;
          F = (_F1139 & 0xFF);
          _address1138 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1138 = result_2359;
          write(_address1138, result_2359);
          int read_2360;
          read_2360 = _value1138;
          A = read_2360;
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
          int operand_2361 = read(_address1141, 0);
          contend1x1(_address1141);
          _value1141 = operand_2361;
          int value1_2362 = _value1141;
          _F1142 = value1_2362 & 1;
          value1_2362 = (value1_2362 & 0x80) | (value1_2362 >> 1);
          value1_2362 &= 0xff;
          _F1142 |= (SZ53P[value1_2362 & 0xff] | (value1_2362 == 0 ? 0x40 : 0));
          int result_2365 = value1_2362 & 0xFF;
          F = (_F1142 & 0xFF);
          _address1141 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1141 = result_2365;
          write(_address1141, result_2365);
          int read_2366;
          read_2366 = _value1141;
          B = read_2366;
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
          int operand_2367 = read(_address1144, 0);
          contend1x1(_address1144);
          _value1144 = operand_2367;
          int value1_2368 = _value1144;
          _F1145 = value1_2368 & 1;
          value1_2368 = (value1_2368 & 0x80) | (value1_2368 >> 1);
          value1_2368 &= 0xff;
          _F1145 |= (SZ53P[value1_2368 & 0xff] | (value1_2368 == 0 ? 0x40 : 0));
          int result_2371 = value1_2368 & 0xFF;
          F = (_F1145 & 0xFF);
          _address1144 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1144 = result_2371;
          write(_address1144, result_2371);
          int read_2372;
          read_2372 = _value1144;
          C = read_2372;
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
          int operand_2373 = read(_address1147, 0);
          contend1x1(_address1147);
          _value1147 = operand_2373;
          int value1_2374 = _value1147;
          _F1148 = value1_2374 & 1;
          value1_2374 = (value1_2374 & 0x80) | (value1_2374 >> 1);
          value1_2374 &= 0xff;
          _F1148 |= (SZ53P[value1_2374 & 0xff] | (value1_2374 == 0 ? 0x40 : 0));
          int result_2377 = value1_2374 & 0xFF;
          F = (_F1148 & 0xFF);
          _address1147 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1147 = result_2377;
          write(_address1147, result_2377);
          int read_2378;
          read_2378 = _value1147;
          D = read_2378;
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
          int operand_2379 = read(_address1150, 0);
          contend1x1(_address1150);
          _value1150 = operand_2379;
          int value1_2380 = _value1150;
          _F1151 = value1_2380 & 1;
          value1_2380 = (value1_2380 & 0x80) | (value1_2380 >> 1);
          value1_2380 &= 0xff;
          _F1151 |= (SZ53P[value1_2380 & 0xff] | (value1_2380 == 0 ? 0x40 : 0));
          int result_2383 = value1_2380 & 0xFF;
          F = (_F1151 & 0xFF);
          _address1150 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1150 = result_2383;
          write(_address1150, result_2383);
          int read_2384;
          read_2384 = _value1150;
          E = read_2384;
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
          int operand_2385 = read(_address1153, 0);
          contend1x1(_address1153);
          _value1153 = operand_2385;
          int value1_2386 = _value1153;
          _F1154 = value1_2386 & 1;
          value1_2386 = (value1_2386 & 0x80) | (value1_2386 >> 1);
          value1_2386 &= 0xff;
          _F1154 |= (SZ53P[value1_2386 & 0xff] | (value1_2386 == 0 ? 0x40 : 0));
          int result_2389 = value1_2386 & 0xFF;
          F = (_F1154 & 0xFF);
          _address1153 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1153 = result_2389;
          write(_address1153, result_2389);
          int read_2390;
          read_2390 = _value1153;
          H = read_2390;
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
          int operand_2391 = read(_address1156, 0);
          contend1x1(_address1156);
          _value1156 = operand_2391;
          int value1_2392 = _value1156;
          _F1157 = value1_2392 & 1;
          value1_2392 = (value1_2392 & 0x80) | (value1_2392 >> 1);
          value1_2392 &= 0xff;
          _F1157 |= (SZ53P[value1_2392 & 0xff] | (value1_2392 == 0 ? 0x40 : 0));
          int result_2395 = value1_2392 & 0xFF;
          F = (_F1157 & 0xFF);
          _address1156 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1156 = result_2395;
          write(_address1156, result_2395);
          int read_2396;
          read_2396 = _value1156;
          L = read_2396;
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
          int operand_2397 = read(_address1159, 0);
          contend1x1(_address1159);
          _value1159 = operand_2397;
          int value1_2398 = _value1159;
          _F1160 = value1_2398 & 1;
          value1_2398 = (value1_2398 & 0x80) | (value1_2398 >> 1);
          value1_2398 &= 0xff;
          _F1160 |= (SZ53P[value1_2398 & 0xff] | (value1_2398 == 0 ? 0x40 : 0));
          int result_2401 = value1_2398 & 0xFF;
          F = (_F1160 & 0xFF);
          _address1159 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1159 = result_2401;
          write(_address1159, result_2401);
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
          int operand_2402 = read(_address1162, 0);
          contend1x1(_address1162);
          _value1162 = operand_2402;
          int value1_2403 = _value1162;
          _F1163 = value1_2403 & 1;
          value1_2403 = (value1_2403 & 0x80) | (value1_2403 >> 1);
          value1_2403 &= 0xff;
          _F1163 |= (SZ53P[value1_2403 & 0xff] | (value1_2403 == 0 ? 0x40 : 0));
          int result_2406 = value1_2403 & 0xFF;
          F = (_F1163 & 0xFF);
          _address1162 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1162 = result_2406;
          write(_address1162, result_2406);
          int read_2407;
          read_2407 = _value1162;
          A = read_2407;
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
          int operand_2408 = read(_address1165, 0);
          contend1x1(_address1165);
          _value1165 = operand_2408;
          int value1_2409 = _value1165;
          _F1166 = value1_2409 >> 7;
          value1_2409 = (value1_2409 << 1) | 0x01;
          value1_2409 &= 0xff;
          _F1166 |= (SZ53P[value1_2409 & 0xff] | (value1_2409 == 0 ? 0x40 : 0));
          int result_2412 = value1_2409 & 0xFF;
          F = (_F1166 & 0xFF);
          _address1165 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1165 = result_2412;
          write(_address1165, result_2412);
          int read_2413;
          read_2413 = _value1165;
          B = read_2413;
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
          int operand_2414 = read(_address1168, 0);
          contend1x1(_address1168);
          _value1168 = operand_2414;
          int value1_2415 = _value1168;
          _F1169 = value1_2415 >> 7;
          value1_2415 = (value1_2415 << 1) | 0x01;
          value1_2415 &= 0xff;
          _F1169 |= (SZ53P[value1_2415 & 0xff] | (value1_2415 == 0 ? 0x40 : 0));
          int result_2418 = value1_2415 & 0xFF;
          F = (_F1169 & 0xFF);
          _address1168 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1168 = result_2418;
          write(_address1168, result_2418);
          int read_2419;
          read_2419 = _value1168;
          C = read_2419;
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
          int operand_2420 = read(_address1171, 0);
          contend1x1(_address1171);
          _value1171 = operand_2420;
          int value1_2421 = _value1171;
          _F1172 = value1_2421 >> 7;
          value1_2421 = (value1_2421 << 1) | 0x01;
          value1_2421 &= 0xff;
          _F1172 |= (SZ53P[value1_2421 & 0xff] | (value1_2421 == 0 ? 0x40 : 0));
          int result_2424 = value1_2421 & 0xFF;
          F = (_F1172 & 0xFF);
          _address1171 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1171 = result_2424;
          write(_address1171, result_2424);
          int read_2425;
          read_2425 = _value1171;
          D = read_2425;
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
          int operand_2426 = read(_address1174, 0);
          contend1x1(_address1174);
          _value1174 = operand_2426;
          int value1_2427 = _value1174;
          _F1175 = value1_2427 >> 7;
          value1_2427 = (value1_2427 << 1) | 0x01;
          value1_2427 &= 0xff;
          _F1175 |= (SZ53P[value1_2427 & 0xff] | (value1_2427 == 0 ? 0x40 : 0));
          int result_2430 = value1_2427 & 0xFF;
          F = (_F1175 & 0xFF);
          _address1174 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1174 = result_2430;
          write(_address1174, result_2430);
          int read_2431;
          read_2431 = _value1174;
          E = read_2431;
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
          int operand_2432 = read(_address1177, 0);
          contend1x1(_address1177);
          _value1177 = operand_2432;
          int value1_2433 = _value1177;
          _F1178 = value1_2433 >> 7;
          value1_2433 = (value1_2433 << 1) | 0x01;
          value1_2433 &= 0xff;
          _F1178 |= (SZ53P[value1_2433 & 0xff] | (value1_2433 == 0 ? 0x40 : 0));
          int result_2436 = value1_2433 & 0xFF;
          F = (_F1178 & 0xFF);
          _address1177 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1177 = result_2436;
          write(_address1177, result_2436);
          int read_2437;
          read_2437 = _value1177;
          H = read_2437;
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
          int operand_2438 = read(_address1180, 0);
          contend1x1(_address1180);
          _value1180 = operand_2438;
          int value1_2439 = _value1180;
          _F1181 = value1_2439 >> 7;
          value1_2439 = (value1_2439 << 1) | 0x01;
          value1_2439 &= 0xff;
          _F1181 |= (SZ53P[value1_2439 & 0xff] | (value1_2439 == 0 ? 0x40 : 0));
          int result_2442 = value1_2439 & 0xFF;
          F = (_F1181 & 0xFF);
          _address1180 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1180 = result_2442;
          write(_address1180, result_2442);
          int read_2443;
          read_2443 = _value1180;
          L = read_2443;
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
          int operand_2444 = read(_address1183, 0);
          contend1x1(_address1183);
          _value1183 = operand_2444;
          int value1_2445 = _value1183;
          _F1184 = value1_2445 >> 7;
          value1_2445 = (value1_2445 << 1) | 0x01;
          value1_2445 &= 0xff;
          _F1184 |= (SZ53P[value1_2445 & 0xff] | (value1_2445 == 0 ? 0x40 : 0));
          int result_2448 = value1_2445 & 0xFF;
          F = (_F1184 & 0xFF);
          _address1183 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1183 = result_2448;
          write(_address1183, result_2448);
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
          int operand_2449 = read(_address1186, 0);
          contend1x1(_address1186);
          _value1186 = operand_2449;
          int value1_2450 = _value1186;
          _F1187 = value1_2450 >> 7;
          value1_2450 = (value1_2450 << 1) | 0x01;
          value1_2450 &= 0xff;
          _F1187 |= (SZ53P[value1_2450 & 0xff] | (value1_2450 == 0 ? 0x40 : 0));
          int result_2453 = value1_2450 & 0xFF;
          F = (_F1187 & 0xFF);
          _address1186 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1186 = result_2453;
          write(_address1186, result_2453);
          int read_2454;
          read_2454 = _value1186;
          A = read_2454;
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
          int operand_2455 = read(_address1189, 0);
          contend1x1(_address1189);
          _value1189 = operand_2455;
          int value1_2456 = _value1189;
          _F1190 = value1_2456 & 1;
          value1_2456 >>= 1;
          value1_2456 &= 0xff;
          _F1190 |= (SZ53P[value1_2456 & 0xff] | (value1_2456 == 0 ? 0x40 : 0));
          int result_2459 = value1_2456 & 0xFF;
          F = (_F1190 & 0xFF);
          _address1189 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1189 = result_2459;
          write(_address1189, result_2459);
          int read_2460;
          read_2460 = _value1189;
          B = read_2460;
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
          int operand_2461 = read(_address1192, 0);
          contend1x1(_address1192);
          _value1192 = operand_2461;
          int value1_2462 = _value1192;
          _F1193 = value1_2462 & 1;
          value1_2462 >>= 1;
          value1_2462 &= 0xff;
          _F1193 |= (SZ53P[value1_2462 & 0xff] | (value1_2462 == 0 ? 0x40 : 0));
          int result_2465 = value1_2462 & 0xFF;
          F = (_F1193 & 0xFF);
          _address1192 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1192 = result_2465;
          write(_address1192, result_2465);
          int read_2466;
          read_2466 = _value1192;
          C = read_2466;
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
          int operand_2467 = read(_address1195, 0);
          contend1x1(_address1195);
          _value1195 = operand_2467;
          int value1_2468 = _value1195;
          _F1196 = value1_2468 & 1;
          value1_2468 >>= 1;
          value1_2468 &= 0xff;
          _F1196 |= (SZ53P[value1_2468 & 0xff] | (value1_2468 == 0 ? 0x40 : 0));
          int result_2471 = value1_2468 & 0xFF;
          F = (_F1196 & 0xFF);
          _address1195 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1195 = result_2471;
          write(_address1195, result_2471);
          int read_2472;
          read_2472 = _value1195;
          D = read_2472;
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
          int operand_2473 = read(_address1198, 0);
          contend1x1(_address1198);
          _value1198 = operand_2473;
          int value1_2474 = _value1198;
          _F1199 = value1_2474 & 1;
          value1_2474 >>= 1;
          value1_2474 &= 0xff;
          _F1199 |= (SZ53P[value1_2474 & 0xff] | (value1_2474 == 0 ? 0x40 : 0));
          int result_2477 = value1_2474 & 0xFF;
          F = (_F1199 & 0xFF);
          _address1198 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1198 = result_2477;
          write(_address1198, result_2477);
          int read_2478;
          read_2478 = _value1198;
          E = read_2478;
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
          int operand_2479 = read(_address1201, 0);
          contend1x1(_address1201);
          _value1201 = operand_2479;
          int value1_2480 = _value1201;
          _F1202 = value1_2480 & 1;
          value1_2480 >>= 1;
          value1_2480 &= 0xff;
          _F1202 |= (SZ53P[value1_2480 & 0xff] | (value1_2480 == 0 ? 0x40 : 0));
          int result_2483 = value1_2480 & 0xFF;
          F = (_F1202 & 0xFF);
          _address1201 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1201 = result_2483;
          write(_address1201, result_2483);
          int read_2484;
          read_2484 = _value1201;
          H = read_2484;
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
          int operand_2485 = read(_address1204, 0);
          contend1x1(_address1204);
          _value1204 = operand_2485;
          int value1_2486 = _value1204;
          _F1205 = value1_2486 & 1;
          value1_2486 >>= 1;
          value1_2486 &= 0xff;
          _F1205 |= (SZ53P[value1_2486 & 0xff] | (value1_2486 == 0 ? 0x40 : 0));
          int result_2489 = value1_2486 & 0xFF;
          F = (_F1205 & 0xFF);
          _address1204 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1204 = result_2489;
          write(_address1204, result_2489);
          int read_2490;
          read_2490 = _value1204;
          L = read_2490;
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
          int operand_2491 = read(_address1207, 0);
          contend1x1(_address1207);
          _value1207 = operand_2491;
          int value1_2492 = _value1207;
          _F1208 = value1_2492 & 1;
          value1_2492 >>= 1;
          value1_2492 &= 0xff;
          _F1208 |= (SZ53P[value1_2492 & 0xff] | (value1_2492 == 0 ? 0x40 : 0));
          int result_2495 = value1_2492 & 0xFF;
          F = (_F1208 & 0xFF);
          _address1207 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1207 = result_2495;
          write(_address1207, result_2495);
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
          int operand_2496 = read(_address1210, 0);
          contend1x1(_address1210);
          _value1210 = operand_2496;
          int value1_2497 = _value1210;
          _F1211 = value1_2497 & 1;
          value1_2497 >>= 1;
          value1_2497 &= 0xff;
          _F1211 |= (SZ53P[value1_2497 & 0xff] | (value1_2497 == 0 ? 0x40 : 0));
          int result_2500 = value1_2497 & 0xFF;
          F = (_F1211 & 0xFF);
          _address1210 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1210 = result_2500;
          write(_address1210, result_2500);
          int read_2501;
          read_2501 = _value1210;
          A = read_2501;
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
          int address_2502;
          address_2502 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2503 = F & 1;
          _address1213 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2504 = read(_address1213, 0);
          contend1x1(_address1213);
          _value1213 = operand_2504;
          int value3_2507 = nAndCarry_2503;
          _F1214 = value3_2507;
          value3_2507 = value3_2507 >>> 1;
          _F1214 = (_F1214 & 1) | 0x10 | (address_2502 & 0x28);
          if ((_value1213 & (0x01 << value3_2507)) == 0) {
              _F1214 |= 0x44;
          }
          if (value3_2507 == 7 && (_value1213 & 0x80) != 0) {
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
          int address_2509;
          address_2509 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2510 = F & 1;
          _address1216 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2511 = read(_address1216, 0);
          contend1x1(_address1216);
          _value1216 = operand_2511;
          int value3_2514 = nAndCarry_2510;
          _F1217 = value3_2514;
          value3_2514 = value3_2514 >>> 1;
          _F1217 = (_F1217 & 1) | 0x10 | (address_2509 & 0x28);
          if ((_value1216 & (0x01 << value3_2514)) == 0) {
              _F1217 |= 0x44;
          }
          if (value3_2514 == 7 && (_value1216 & 0x80) != 0) {
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
          int address_2516;
          address_2516 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2517 = F & 1;
          _address1219 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2518 = read(_address1219, 0);
          contend1x1(_address1219);
          _value1219 = operand_2518;
          int value3_2521 = nAndCarry_2517;
          _F1220 = value3_2521;
          value3_2521 = value3_2521 >>> 1;
          _F1220 = (_F1220 & 1) | 0x10 | (address_2516 & 0x28);
          if ((_value1219 & (0x01 << value3_2521)) == 0) {
              _F1220 |= 0x44;
          }
          if (value3_2521 == 7 && (_value1219 & 0x80) != 0) {
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
          int address_2523;
          address_2523 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2524 = F & 1;
          _address1222 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2525 = read(_address1222, 0);
          contend1x1(_address1222);
          _value1222 = operand_2525;
          int value3_2528 = nAndCarry_2524;
          _F1223 = value3_2528;
          value3_2528 = value3_2528 >>> 1;
          _F1223 = (_F1223 & 1) | 0x10 | (address_2523 & 0x28);
          if ((_value1222 & (0x01 << value3_2528)) == 0) {
              _F1223 |= 0x44;
          }
          if (value3_2528 == 7 && (_value1222 & 0x80) != 0) {
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
          int address_2530;
          address_2530 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2531 = F & 1;
          _address1225 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2532 = read(_address1225, 0);
          contend1x1(_address1225);
          _value1225 = operand_2532;
          int value3_2535 = nAndCarry_2531;
          _F1226 = value3_2535;
          value3_2535 = value3_2535 >>> 1;
          _F1226 = (_F1226 & 1) | 0x10 | (address_2530 & 0x28);
          if ((_value1225 & (0x01 << value3_2535)) == 0) {
              _F1226 |= 0x44;
          }
          if (value3_2535 == 7 && (_value1225 & 0x80) != 0) {
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
          int address_2537;
          address_2537 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2538 = F & 1;
          _address1228 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2539 = read(_address1228, 0);
          contend1x1(_address1228);
          _value1228 = operand_2539;
          int value3_2542 = nAndCarry_2538;
          _F1229 = value3_2542;
          value3_2542 = value3_2542 >>> 1;
          _F1229 = (_F1229 & 1) | 0x10 | (address_2537 & 0x28);
          if ((_value1228 & (0x01 << value3_2542)) == 0) {
              _F1229 |= 0x44;
          }
          if (value3_2542 == 7 && (_value1228 & 0x80) != 0) {
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
          int address_2544;
          address_2544 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2545 = F & 1;
          _address1231 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2546 = read(_address1231, 0);
          contend1x1(_address1231);
          _value1231 = operand_2546;
          int value3_2549 = nAndCarry_2545;
          _F1232 = value3_2549;
          value3_2549 = value3_2549 >>> 1;
          _F1232 = (_F1232 & 1) | 0x10 | (address_2544 & 0x28);
          if ((_value1231 & (0x01 << value3_2549)) == 0) {
              _F1232 |= 0x44;
          }
          if (value3_2549 == 7 && (_value1231 & 0x80) != 0) {
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
          int address_2551;
          address_2551 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2552 = F & 1;
          _address1234 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2553 = read(_address1234, 0);
          contend1x1(_address1234);
          _value1234 = operand_2553;
          int value3_2556 = nAndCarry_2552;
          _F1235 = value3_2556;
          value3_2556 = value3_2556 >>> 1;
          _F1235 = (_F1235 & 1) | 0x10 | (address_2551 & 0x28);
          if ((_value1234 & (0x01 << value3_2556)) == 0) {
              _F1235 |= 0x44;
          }
          if (value3_2556 == 7 && (_value1234 & 0x80) != 0) {
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
          int address_2558;
          address_2558 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2559 = 2 | F & 1;
          _address1237 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2560 = read(_address1237, 0);
          contend1x1(_address1237);
          _value1237 = operand_2560;
          int value3_2563 = nAndCarry_2559;
          _F1238 = value3_2563 & 1;
          value3_2563 = value3_2563 >>> 1;
          _F1238 = (_F1238 & 1) | 0x10 | (address_2558 & 0x28);
          if ((_value1237 & (0x01 << value3_2563)) == 0) {
              _F1238 |= 0x44;
          }
          if (value3_2563 == 7 && (_value1237 & 0x80) != 0) {
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
          int address_2565;
          address_2565 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2566 = 2 | F & 1;
          _address1240 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2567 = read(_address1240, 0);
          contend1x1(_address1240);
          _value1240 = operand_2567;
          int value3_2570 = nAndCarry_2566;
          _F1241 = value3_2570 & 1;
          value3_2570 = value3_2570 >>> 1;
          _F1241 = (_F1241 & 1) | 0x10 | (address_2565 & 0x28);
          if ((_value1240 & (0x01 << value3_2570)) == 0) {
              _F1241 |= 0x44;
          }
          if (value3_2570 == 7 && (_value1240 & 0x80) != 0) {
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
          int address_2572;
          address_2572 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2573 = 2 | F & 1;
          _address1243 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2574 = read(_address1243, 0);
          contend1x1(_address1243);
          _value1243 = operand_2574;
          int value3_2577 = nAndCarry_2573;
          _F1244 = value3_2577 & 1;
          value3_2577 = value3_2577 >>> 1;
          _F1244 = (_F1244 & 1) | 0x10 | (address_2572 & 0x28);
          if ((_value1243 & (0x01 << value3_2577)) == 0) {
              _F1244 |= 0x44;
          }
          if (value3_2577 == 7 && (_value1243 & 0x80) != 0) {
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
          int address_2579;
          address_2579 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2580 = 2 | F & 1;
          _address1246 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2581 = read(_address1246, 0);
          contend1x1(_address1246);
          _value1246 = operand_2581;
          int value3_2584 = nAndCarry_2580;
          _F1247 = value3_2584 & 1;
          value3_2584 = value3_2584 >>> 1;
          _F1247 = (_F1247 & 1) | 0x10 | (address_2579 & 0x28);
          if ((_value1246 & (0x01 << value3_2584)) == 0) {
              _F1247 |= 0x44;
          }
          if (value3_2584 == 7 && (_value1246 & 0x80) != 0) {
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
          int address_2586;
          address_2586 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2587 = 2 | F & 1;
          _address1249 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2588 = read(_address1249, 0);
          contend1x1(_address1249);
          _value1249 = operand_2588;
          int value3_2591 = nAndCarry_2587;
          _F1250 = value3_2591 & 1;
          value3_2591 = value3_2591 >>> 1;
          _F1250 = (_F1250 & 1) | 0x10 | (address_2586 & 0x28);
          if ((_value1249 & (0x01 << value3_2591)) == 0) {
              _F1250 |= 0x44;
          }
          if (value3_2591 == 7 && (_value1249 & 0x80) != 0) {
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
          int address_2593;
          address_2593 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2594 = 2 | F & 1;
          _address1252 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2595 = read(_address1252, 0);
          contend1x1(_address1252);
          _value1252 = operand_2595;
          int value3_2598 = nAndCarry_2594;
          _F1253 = value3_2598 & 1;
          value3_2598 = value3_2598 >>> 1;
          _F1253 = (_F1253 & 1) | 0x10 | (address_2593 & 0x28);
          if ((_value1252 & (0x01 << value3_2598)) == 0) {
              _F1253 |= 0x44;
          }
          if (value3_2598 == 7 && (_value1252 & 0x80) != 0) {
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
          int address_2600;
          address_2600 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2601 = 2 | F & 1;
          _address1255 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2602 = read(_address1255, 0);
          contend1x1(_address1255);
          _value1255 = operand_2602;
          int value3_2605 = nAndCarry_2601;
          _F1256 = value3_2605 & 1;
          value3_2605 = value3_2605 >>> 1;
          _F1256 = (_F1256 & 1) | 0x10 | (address_2600 & 0x28);
          if ((_value1255 & (0x01 << value3_2605)) == 0) {
              _F1256 |= 0x44;
          }
          if (value3_2605 == 7 && (_value1255 & 0x80) != 0) {
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
          int address_2607;
          address_2607 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2608 = 2 | F & 1;
          _address1258 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2609 = read(_address1258, 0);
          contend1x1(_address1258);
          _value1258 = operand_2609;
          int value3_2612 = nAndCarry_2608;
          _F1259 = value3_2612 & 1;
          value3_2612 = value3_2612 >>> 1;
          _F1259 = (_F1259 & 1) | 0x10 | (address_2607 & 0x28);
          if ((_value1258 & (0x01 << value3_2612)) == 0) {
              _F1259 |= 0x44;
          }
          if (value3_2612 == 7 && (_value1258 & 0x80) != 0) {
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
          int address_2614;
          address_2614 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2615 = 4 | F & 1;
          _address1261 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2616 = read(_address1261, 0);
          contend1x1(_address1261);
          _value1261 = operand_2616;
          int value3_2619 = nAndCarry_2615;
          _F1262 = value3_2619 & 1;
          value3_2619 = value3_2619 >>> 1;
          _F1262 = (_F1262 & 1) | 0x10 | (address_2614 & 0x28);
          if ((_value1261 & (0x01 << value3_2619)) == 0) {
              _F1262 |= 0x44;
          }
          if (value3_2619 == 7 && (_value1261 & 0x80) != 0) {
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
          int address_2621;
          address_2621 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2622 = 4 | F & 1;
          _address1264 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2623 = read(_address1264, 0);
          contend1x1(_address1264);
          _value1264 = operand_2623;
          int value3_2626 = nAndCarry_2622;
          _F1265 = value3_2626 & 1;
          value3_2626 = value3_2626 >>> 1;
          _F1265 = (_F1265 & 1) | 0x10 | (address_2621 & 0x28);
          if ((_value1264 & (0x01 << value3_2626)) == 0) {
              _F1265 |= 0x44;
          }
          if (value3_2626 == 7 && (_value1264 & 0x80) != 0) {
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
          int address_2628;
          address_2628 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2629 = 4 | F & 1;
          _address1267 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2630 = read(_address1267, 0);
          contend1x1(_address1267);
          _value1267 = operand_2630;
          int value3_2633 = nAndCarry_2629;
          _F1268 = value3_2633 & 1;
          value3_2633 = value3_2633 >>> 1;
          _F1268 = (_F1268 & 1) | 0x10 | (address_2628 & 0x28);
          if ((_value1267 & (0x01 << value3_2633)) == 0) {
              _F1268 |= 0x44;
          }
          if (value3_2633 == 7 && (_value1267 & 0x80) != 0) {
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
          int address_2635;
          address_2635 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2636 = 4 | F & 1;
          _address1270 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2637 = read(_address1270, 0);
          contend1x1(_address1270);
          _value1270 = operand_2637;
          int value3_2640 = nAndCarry_2636;
          _F1271 = value3_2640 & 1;
          value3_2640 = value3_2640 >>> 1;
          _F1271 = (_F1271 & 1) | 0x10 | (address_2635 & 0x28);
          if ((_value1270 & (0x01 << value3_2640)) == 0) {
              _F1271 |= 0x44;
          }
          if (value3_2640 == 7 && (_value1270 & 0x80) != 0) {
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
          int address_2642;
          address_2642 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2643 = 4 | F & 1;
          _address1273 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2644 = read(_address1273, 0);
          contend1x1(_address1273);
          _value1273 = operand_2644;
          int value3_2647 = nAndCarry_2643;
          _F1274 = value3_2647 & 1;
          value3_2647 = value3_2647 >>> 1;
          _F1274 = (_F1274 & 1) | 0x10 | (address_2642 & 0x28);
          if ((_value1273 & (0x01 << value3_2647)) == 0) {
              _F1274 |= 0x44;
          }
          if (value3_2647 == 7 && (_value1273 & 0x80) != 0) {
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
          int address_2649;
          address_2649 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2650 = 4 | F & 1;
          _address1276 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2651 = read(_address1276, 0);
          contend1x1(_address1276);
          _value1276 = operand_2651;
          int value3_2654 = nAndCarry_2650;
          _F1277 = value3_2654 & 1;
          value3_2654 = value3_2654 >>> 1;
          _F1277 = (_F1277 & 1) | 0x10 | (address_2649 & 0x28);
          if ((_value1276 & (0x01 << value3_2654)) == 0) {
              _F1277 |= 0x44;
          }
          if (value3_2654 == 7 && (_value1276 & 0x80) != 0) {
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
          int address_2656;
          address_2656 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2657 = 4 | F & 1;
          _address1279 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2658 = read(_address1279, 0);
          contend1x1(_address1279);
          _value1279 = operand_2658;
          int value3_2661 = nAndCarry_2657;
          _F1280 = value3_2661 & 1;
          value3_2661 = value3_2661 >>> 1;
          _F1280 = (_F1280 & 1) | 0x10 | (address_2656 & 0x28);
          if ((_value1279 & (0x01 << value3_2661)) == 0) {
              _F1280 |= 0x44;
          }
          if (value3_2661 == 7 && (_value1279 & 0x80) != 0) {
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
          int address_2663;
          address_2663 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2664 = 4 | F & 1;
          _address1282 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2665 = read(_address1282, 0);
          contend1x1(_address1282);
          _value1282 = operand_2665;
          int value3_2668 = nAndCarry_2664;
          _F1283 = value3_2668 & 1;
          value3_2668 = value3_2668 >>> 1;
          _F1283 = (_F1283 & 1) | 0x10 | (address_2663 & 0x28);
          if ((_value1282 & (0x01 << value3_2668)) == 0) {
              _F1283 |= 0x44;
          }
          if (value3_2668 == 7 && (_value1282 & 0x80) != 0) {
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
          int address_2670;
          address_2670 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2671 = 6 | F & 1;
          _address1285 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2672 = read(_address1285, 0);
          contend1x1(_address1285);
          _value1285 = operand_2672;
          int value3_2675 = nAndCarry_2671;
          _F1286 = value3_2675 & 1;
          value3_2675 = value3_2675 >>> 1;
          _F1286 = (_F1286 & 1) | 0x10 | (address_2670 & 0x28);
          if ((_value1285 & (0x01 << value3_2675)) == 0) {
              _F1286 |= 0x44;
          }
          if (value3_2675 == 7 && (_value1285 & 0x80) != 0) {
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
          int address_2677;
          address_2677 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2678 = 6 | F & 1;
          _address1288 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2679 = read(_address1288, 0);
          contend1x1(_address1288);
          _value1288 = operand_2679;
          int value3_2682 = nAndCarry_2678;
          _F1289 = value3_2682 & 1;
          value3_2682 = value3_2682 >>> 1;
          _F1289 = (_F1289 & 1) | 0x10 | (address_2677 & 0x28);
          if ((_value1288 & (0x01 << value3_2682)) == 0) {
              _F1289 |= 0x44;
          }
          if (value3_2682 == 7 && (_value1288 & 0x80) != 0) {
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
          int address_2684;
          address_2684 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2685 = 6 | F & 1;
          _address1291 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2686 = read(_address1291, 0);
          contend1x1(_address1291);
          _value1291 = operand_2686;
          int value3_2689 = nAndCarry_2685;
          _F1292 = value3_2689 & 1;
          value3_2689 = value3_2689 >>> 1;
          _F1292 = (_F1292 & 1) | 0x10 | (address_2684 & 0x28);
          if ((_value1291 & (0x01 << value3_2689)) == 0) {
              _F1292 |= 0x44;
          }
          if (value3_2689 == 7 && (_value1291 & 0x80) != 0) {
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
          int address_2691;
          address_2691 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2692 = 6 | F & 1;
          _address1294 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2693 = read(_address1294, 0);
          contend1x1(_address1294);
          _value1294 = operand_2693;
          int value3_2696 = nAndCarry_2692;
          _F1295 = value3_2696 & 1;
          value3_2696 = value3_2696 >>> 1;
          _F1295 = (_F1295 & 1) | 0x10 | (address_2691 & 0x28);
          if ((_value1294 & (0x01 << value3_2696)) == 0) {
              _F1295 |= 0x44;
          }
          if (value3_2696 == 7 && (_value1294 & 0x80) != 0) {
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
          int address_2698;
          address_2698 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2699 = 6 | F & 1;
          _address1297 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2700 = read(_address1297, 0);
          contend1x1(_address1297);
          _value1297 = operand_2700;
          int value3_2703 = nAndCarry_2699;
          _F1298 = value3_2703 & 1;
          value3_2703 = value3_2703 >>> 1;
          _F1298 = (_F1298 & 1) | 0x10 | (address_2698 & 0x28);
          if ((_value1297 & (0x01 << value3_2703)) == 0) {
              _F1298 |= 0x44;
          }
          if (value3_2703 == 7 && (_value1297 & 0x80) != 0) {
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
          int address_2705;
          address_2705 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2706 = 6 | F & 1;
          _address1300 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2707 = read(_address1300, 0);
          contend1x1(_address1300);
          _value1300 = operand_2707;
          int value3_2710 = nAndCarry_2706;
          _F1301 = value3_2710 & 1;
          value3_2710 = value3_2710 >>> 1;
          _F1301 = (_F1301 & 1) | 0x10 | (address_2705 & 0x28);
          if ((_value1300 & (0x01 << value3_2710)) == 0) {
              _F1301 |= 0x44;
          }
          if (value3_2710 == 7 && (_value1300 & 0x80) != 0) {
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
          int address_2712;
          address_2712 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2713 = 6 | F & 1;
          _address1303 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2714 = read(_address1303, 0);
          contend1x1(_address1303);
          _value1303 = operand_2714;
          int value3_2717 = nAndCarry_2713;
          _F1304 = value3_2717 & 1;
          value3_2717 = value3_2717 >>> 1;
          _F1304 = (_F1304 & 1) | 0x10 | (address_2712 & 0x28);
          if ((_value1303 & (0x01 << value3_2717)) == 0) {
              _F1304 |= 0x44;
          }
          if (value3_2717 == 7 && (_value1303 & 0x80) != 0) {
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
          int address_2719;
          address_2719 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2720 = 6 | F & 1;
          _address1306 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2721 = read(_address1306, 0);
          contend1x1(_address1306);
          _value1306 = operand_2721;
          int value3_2724 = nAndCarry_2720;
          _F1307 = value3_2724 & 1;
          value3_2724 = value3_2724 >>> 1;
          _F1307 = (_F1307 & 1) | 0x10 | (address_2719 & 0x28);
          if ((_value1306 & (0x01 << value3_2724)) == 0) {
              _F1307 |= 0x44;
          }
          if (value3_2724 == 7 && (_value1306 & 0x80) != 0) {
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
          int address_2726;
          address_2726 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2727 = 8 | F & 1;
          _address1309 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2728 = read(_address1309, 0);
          contend1x1(_address1309);
          _value1309 = operand_2728;
          int value3_2731 = nAndCarry_2727;
          _F1310 = value3_2731 & 1;
          value3_2731 = value3_2731 >>> 1;
          _F1310 = (_F1310 & 1) | 0x10 | (address_2726 & 0x28);
          if ((_value1309 & (0x01 << value3_2731)) == 0) {
              _F1310 |= 0x44;
          }
          if (value3_2731 == 7 && (_value1309 & 0x80) != 0) {
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
          int address_2733;
          address_2733 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2734 = 8 | F & 1;
          _address1312 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2735 = read(_address1312, 0);
          contend1x1(_address1312);
          _value1312 = operand_2735;
          int value3_2738 = nAndCarry_2734;
          _F1313 = value3_2738 & 1;
          value3_2738 = value3_2738 >>> 1;
          _F1313 = (_F1313 & 1) | 0x10 | (address_2733 & 0x28);
          if ((_value1312 & (0x01 << value3_2738)) == 0) {
              _F1313 |= 0x44;
          }
          if (value3_2738 == 7 && (_value1312 & 0x80) != 0) {
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
          int address_2740;
          address_2740 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2741 = 8 | F & 1;
          _address1315 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2742 = read(_address1315, 0);
          contend1x1(_address1315);
          _value1315 = operand_2742;
          int value3_2745 = nAndCarry_2741;
          _F1316 = value3_2745 & 1;
          value3_2745 = value3_2745 >>> 1;
          _F1316 = (_F1316 & 1) | 0x10 | (address_2740 & 0x28);
          if ((_value1315 & (0x01 << value3_2745)) == 0) {
              _F1316 |= 0x44;
          }
          if (value3_2745 == 7 && (_value1315 & 0x80) != 0) {
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
          int address_2747;
          address_2747 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2748 = 8 | F & 1;
          _address1318 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2749 = read(_address1318, 0);
          contend1x1(_address1318);
          _value1318 = operand_2749;
          int value3_2752 = nAndCarry_2748;
          _F1319 = value3_2752 & 1;
          value3_2752 = value3_2752 >>> 1;
          _F1319 = (_F1319 & 1) | 0x10 | (address_2747 & 0x28);
          if ((_value1318 & (0x01 << value3_2752)) == 0) {
              _F1319 |= 0x44;
          }
          if (value3_2752 == 7 && (_value1318 & 0x80) != 0) {
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
          int address_2754;
          address_2754 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2755 = 8 | F & 1;
          _address1321 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2756 = read(_address1321, 0);
          contend1x1(_address1321);
          _value1321 = operand_2756;
          int value3_2759 = nAndCarry_2755;
          _F1322 = value3_2759 & 1;
          value3_2759 = value3_2759 >>> 1;
          _F1322 = (_F1322 & 1) | 0x10 | (address_2754 & 0x28);
          if ((_value1321 & (0x01 << value3_2759)) == 0) {
              _F1322 |= 0x44;
          }
          if (value3_2759 == 7 && (_value1321 & 0x80) != 0) {
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
          int address_2761;
          address_2761 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2762 = 8 | F & 1;
          _address1324 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2763 = read(_address1324, 0);
          contend1x1(_address1324);
          _value1324 = operand_2763;
          int value3_2766 = nAndCarry_2762;
          _F1325 = value3_2766 & 1;
          value3_2766 = value3_2766 >>> 1;
          _F1325 = (_F1325 & 1) | 0x10 | (address_2761 & 0x28);
          if ((_value1324 & (0x01 << value3_2766)) == 0) {
              _F1325 |= 0x44;
          }
          if (value3_2766 == 7 && (_value1324 & 0x80) != 0) {
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
          int address_2768;
          address_2768 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2769 = 8 | F & 1;
          _address1327 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2770 = read(_address1327, 0);
          contend1x1(_address1327);
          _value1327 = operand_2770;
          int value3_2773 = nAndCarry_2769;
          _F1328 = value3_2773 & 1;
          value3_2773 = value3_2773 >>> 1;
          _F1328 = (_F1328 & 1) | 0x10 | (address_2768 & 0x28);
          if ((_value1327 & (0x01 << value3_2773)) == 0) {
              _F1328 |= 0x44;
          }
          if (value3_2773 == 7 && (_value1327 & 0x80) != 0) {
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
          int address_2775;
          address_2775 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2776 = 8 | F & 1;
          _address1330 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2777 = read(_address1330, 0);
          contend1x1(_address1330);
          _value1330 = operand_2777;
          int value3_2780 = nAndCarry_2776;
          _F1331 = value3_2780 & 1;
          value3_2780 = value3_2780 >>> 1;
          _F1331 = (_F1331 & 1) | 0x10 | (address_2775 & 0x28);
          if ((_value1330 & (0x01 << value3_2780)) == 0) {
              _F1331 |= 0x44;
          }
          if (value3_2780 == 7 && (_value1330 & 0x80) != 0) {
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
          int address_2782;
          address_2782 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2783 = 10 | F & 1;
          _address1333 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2784 = read(_address1333, 0);
          contend1x1(_address1333);
          _value1333 = operand_2784;
          int value3_2787 = nAndCarry_2783;
          _F1334 = value3_2787 & 1;
          value3_2787 = value3_2787 >>> 1;
          _F1334 = (_F1334 & 1) | 0x10 | (address_2782 & 0x28);
          if ((_value1333 & (0x01 << value3_2787)) == 0) {
              _F1334 |= 0x44;
          }
          if (value3_2787 == 7 && (_value1333 & 0x80) != 0) {
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
          int address_2789;
          address_2789 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2790 = 10 | F & 1;
          _address1336 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2791 = read(_address1336, 0);
          contend1x1(_address1336);
          _value1336 = operand_2791;
          int value3_2794 = nAndCarry_2790;
          _F1337 = value3_2794 & 1;
          value3_2794 = value3_2794 >>> 1;
          _F1337 = (_F1337 & 1) | 0x10 | (address_2789 & 0x28);
          if ((_value1336 & (0x01 << value3_2794)) == 0) {
              _F1337 |= 0x44;
          }
          if (value3_2794 == 7 && (_value1336 & 0x80) != 0) {
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
          int address_2796;
          address_2796 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2797 = 10 | F & 1;
          _address1339 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2798 = read(_address1339, 0);
          contend1x1(_address1339);
          _value1339 = operand_2798;
          int value3_2801 = nAndCarry_2797;
          _F1340 = value3_2801 & 1;
          value3_2801 = value3_2801 >>> 1;
          _F1340 = (_F1340 & 1) | 0x10 | (address_2796 & 0x28);
          if ((_value1339 & (0x01 << value3_2801)) == 0) {
              _F1340 |= 0x44;
          }
          if (value3_2801 == 7 && (_value1339 & 0x80) != 0) {
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
          int address_2803;
          address_2803 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2804 = 10 | F & 1;
          _address1342 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2805 = read(_address1342, 0);
          contend1x1(_address1342);
          _value1342 = operand_2805;
          int value3_2808 = nAndCarry_2804;
          _F1343 = value3_2808 & 1;
          value3_2808 = value3_2808 >>> 1;
          _F1343 = (_F1343 & 1) | 0x10 | (address_2803 & 0x28);
          if ((_value1342 & (0x01 << value3_2808)) == 0) {
              _F1343 |= 0x44;
          }
          if (value3_2808 == 7 && (_value1342 & 0x80) != 0) {
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
          int address_2810;
          address_2810 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2811 = 10 | F & 1;
          _address1345 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2812 = read(_address1345, 0);
          contend1x1(_address1345);
          _value1345 = operand_2812;
          int value3_2815 = nAndCarry_2811;
          _F1346 = value3_2815 & 1;
          value3_2815 = value3_2815 >>> 1;
          _F1346 = (_F1346 & 1) | 0x10 | (address_2810 & 0x28);
          if ((_value1345 & (0x01 << value3_2815)) == 0) {
              _F1346 |= 0x44;
          }
          if (value3_2815 == 7 && (_value1345 & 0x80) != 0) {
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
          int address_2817;
          address_2817 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2818 = 10 | F & 1;
          _address1348 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2819 = read(_address1348, 0);
          contend1x1(_address1348);
          _value1348 = operand_2819;
          int value3_2822 = nAndCarry_2818;
          _F1349 = value3_2822 & 1;
          value3_2822 = value3_2822 >>> 1;
          _F1349 = (_F1349 & 1) | 0x10 | (address_2817 & 0x28);
          if ((_value1348 & (0x01 << value3_2822)) == 0) {
              _F1349 |= 0x44;
          }
          if (value3_2822 == 7 && (_value1348 & 0x80) != 0) {
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
          int address_2824;
          address_2824 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2825 = 10 | F & 1;
          _address1351 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2826 = read(_address1351, 0);
          contend1x1(_address1351);
          _value1351 = operand_2826;
          int value3_2829 = nAndCarry_2825;
          _F1352 = value3_2829 & 1;
          value3_2829 = value3_2829 >>> 1;
          _F1352 = (_F1352 & 1) | 0x10 | (address_2824 & 0x28);
          if ((_value1351 & (0x01 << value3_2829)) == 0) {
              _F1352 |= 0x44;
          }
          if (value3_2829 == 7 && (_value1351 & 0x80) != 0) {
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
          int address_2831;
          address_2831 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2832 = 10 | F & 1;
          _address1354 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2833 = read(_address1354, 0);
          contend1x1(_address1354);
          _value1354 = operand_2833;
          int value3_2836 = nAndCarry_2832;
          _F1355 = value3_2836 & 1;
          value3_2836 = value3_2836 >>> 1;
          _F1355 = (_F1355 & 1) | 0x10 | (address_2831 & 0x28);
          if ((_value1354 & (0x01 << value3_2836)) == 0) {
              _F1355 |= 0x44;
          }
          if (value3_2836 == 7 && (_value1354 & 0x80) != 0) {
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
          int address_2838;
          address_2838 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2839 = 12 | F & 1;
          _address1357 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2840 = read(_address1357, 0);
          contend1x1(_address1357);
          _value1357 = operand_2840;
          int value3_2843 = nAndCarry_2839;
          _F1358 = value3_2843 & 1;
          value3_2843 = value3_2843 >>> 1;
          _F1358 = (_F1358 & 1) | 0x10 | (address_2838 & 0x28);
          if ((_value1357 & (0x01 << value3_2843)) == 0) {
              _F1358 |= 0x44;
          }
          if (value3_2843 == 7 && (_value1357 & 0x80) != 0) {
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
          int address_2845;
          address_2845 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2846 = 12 | F & 1;
          _address1360 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2847 = read(_address1360, 0);
          contend1x1(_address1360);
          _value1360 = operand_2847;
          int value3_2850 = nAndCarry_2846;
          _F1361 = value3_2850 & 1;
          value3_2850 = value3_2850 >>> 1;
          _F1361 = (_F1361 & 1) | 0x10 | (address_2845 & 0x28);
          if ((_value1360 & (0x01 << value3_2850)) == 0) {
              _F1361 |= 0x44;
          }
          if (value3_2850 == 7 && (_value1360 & 0x80) != 0) {
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
          int address_2852;
          address_2852 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2853 = 12 | F & 1;
          _address1363 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2854 = read(_address1363, 0);
          contend1x1(_address1363);
          _value1363 = operand_2854;
          int value3_2857 = nAndCarry_2853;
          _F1364 = value3_2857 & 1;
          value3_2857 = value3_2857 >>> 1;
          _F1364 = (_F1364 & 1) | 0x10 | (address_2852 & 0x28);
          if ((_value1363 & (0x01 << value3_2857)) == 0) {
              _F1364 |= 0x44;
          }
          if (value3_2857 == 7 && (_value1363 & 0x80) != 0) {
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
          int address_2859;
          address_2859 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2860 = 12 | F & 1;
          _address1366 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2861 = read(_address1366, 0);
          contend1x1(_address1366);
          _value1366 = operand_2861;
          int value3_2864 = nAndCarry_2860;
          _F1367 = value3_2864 & 1;
          value3_2864 = value3_2864 >>> 1;
          _F1367 = (_F1367 & 1) | 0x10 | (address_2859 & 0x28);
          if ((_value1366 & (0x01 << value3_2864)) == 0) {
              _F1367 |= 0x44;
          }
          if (value3_2864 == 7 && (_value1366 & 0x80) != 0) {
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
          int address_2866;
          address_2866 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2867 = 12 | F & 1;
          _address1369 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2868 = read(_address1369, 0);
          contend1x1(_address1369);
          _value1369 = operand_2868;
          int value3_2871 = nAndCarry_2867;
          _F1370 = value3_2871 & 1;
          value3_2871 = value3_2871 >>> 1;
          _F1370 = (_F1370 & 1) | 0x10 | (address_2866 & 0x28);
          if ((_value1369 & (0x01 << value3_2871)) == 0) {
              _F1370 |= 0x44;
          }
          if (value3_2871 == 7 && (_value1369 & 0x80) != 0) {
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
          int address_2873;
          address_2873 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2874 = 12 | F & 1;
          _address1372 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2875 = read(_address1372, 0);
          contend1x1(_address1372);
          _value1372 = operand_2875;
          int value3_2878 = nAndCarry_2874;
          _F1373 = value3_2878 & 1;
          value3_2878 = value3_2878 >>> 1;
          _F1373 = (_F1373 & 1) | 0x10 | (address_2873 & 0x28);
          if ((_value1372 & (0x01 << value3_2878)) == 0) {
              _F1373 |= 0x44;
          }
          if (value3_2878 == 7 && (_value1372 & 0x80) != 0) {
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
          int address_2880;
          address_2880 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2881 = 12 | F & 1;
          _address1375 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2882 = read(_address1375, 0);
          contend1x1(_address1375);
          _value1375 = operand_2882;
          int value3_2885 = nAndCarry_2881;
          _F1376 = value3_2885 & 1;
          value3_2885 = value3_2885 >>> 1;
          _F1376 = (_F1376 & 1) | 0x10 | (address_2880 & 0x28);
          if ((_value1375 & (0x01 << value3_2885)) == 0) {
              _F1376 |= 0x44;
          }
          if (value3_2885 == 7 && (_value1375 & 0x80) != 0) {
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
          int address_2887;
          address_2887 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2888 = 12 | F & 1;
          _address1378 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2889 = read(_address1378, 0);
          contend1x1(_address1378);
          _value1378 = operand_2889;
          int value3_2892 = nAndCarry_2888;
          _F1379 = value3_2892 & 1;
          value3_2892 = value3_2892 >>> 1;
          _F1379 = (_F1379 & 1) | 0x10 | (address_2887 & 0x28);
          if ((_value1378 & (0x01 << value3_2892)) == 0) {
              _F1379 |= 0x44;
          }
          if (value3_2892 == 7 && (_value1378 & 0x80) != 0) {
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
          int address_2894;
          address_2894 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2895 = 14 | F & 1;
          _address1381 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2896 = read(_address1381, 0);
          contend1x1(_address1381);
          _value1381 = operand_2896;
          int value3_2899 = nAndCarry_2895;
          _F1382 = value3_2899 & 1;
          value3_2899 = value3_2899 >>> 1;
          _F1382 = (_F1382 & 1) | 0x10 | (address_2894 & 0x28);
          if ((_value1381 & (0x01 << value3_2899)) == 0) {
              _F1382 |= 0x44;
          }
          if (value3_2899 == 7 && (_value1381 & 0x80) != 0) {
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
          int address_2901;
          address_2901 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2902 = 14 | F & 1;
          _address1384 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2903 = read(_address1384, 0);
          contend1x1(_address1384);
          _value1384 = operand_2903;
          int value3_2906 = nAndCarry_2902;
          _F1385 = value3_2906 & 1;
          value3_2906 = value3_2906 >>> 1;
          _F1385 = (_F1385 & 1) | 0x10 | (address_2901 & 0x28);
          if ((_value1384 & (0x01 << value3_2906)) == 0) {
              _F1385 |= 0x44;
          }
          if (value3_2906 == 7 && (_value1384 & 0x80) != 0) {
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
          int address_2908;
          address_2908 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2909 = 14 | F & 1;
          _address1387 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2910 = read(_address1387, 0);
          contend1x1(_address1387);
          _value1387 = operand_2910;
          int value3_2913 = nAndCarry_2909;
          _F1388 = value3_2913 & 1;
          value3_2913 = value3_2913 >>> 1;
          _F1388 = (_F1388 & 1) | 0x10 | (address_2908 & 0x28);
          if ((_value1387 & (0x01 << value3_2913)) == 0) {
              _F1388 |= 0x44;
          }
          if (value3_2913 == 7 && (_value1387 & 0x80) != 0) {
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
          int address_2915;
          address_2915 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2916 = 14 | F & 1;
          _address1390 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2917 = read(_address1390, 0);
          contend1x1(_address1390);
          _value1390 = operand_2917;
          int value3_2920 = nAndCarry_2916;
          _F1391 = value3_2920 & 1;
          value3_2920 = value3_2920 >>> 1;
          _F1391 = (_F1391 & 1) | 0x10 | (address_2915 & 0x28);
          if ((_value1390 & (0x01 << value3_2920)) == 0) {
              _F1391 |= 0x44;
          }
          if (value3_2920 == 7 && (_value1390 & 0x80) != 0) {
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
          int address_2922;
          address_2922 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2923 = 14 | F & 1;
          _address1393 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2924 = read(_address1393, 0);
          contend1x1(_address1393);
          _value1393 = operand_2924;
          int value3_2927 = nAndCarry_2923;
          _F1394 = value3_2927 & 1;
          value3_2927 = value3_2927 >>> 1;
          _F1394 = (_F1394 & 1) | 0x10 | (address_2922 & 0x28);
          if ((_value1393 & (0x01 << value3_2927)) == 0) {
              _F1394 |= 0x44;
          }
          if (value3_2927 == 7 && (_value1393 & 0x80) != 0) {
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
          int address_2929;
          address_2929 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2930 = 14 | F & 1;
          _address1396 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2931 = read(_address1396, 0);
          contend1x1(_address1396);
          _value1396 = operand_2931;
          int value3_2934 = nAndCarry_2930;
          _F1397 = value3_2934 & 1;
          value3_2934 = value3_2934 >>> 1;
          _F1397 = (_F1397 & 1) | 0x10 | (address_2929 & 0x28);
          if ((_value1396 & (0x01 << value3_2934)) == 0) {
              _F1397 |= 0x44;
          }
          if (value3_2934 == 7 && (_value1396 & 0x80) != 0) {
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
          int address_2936;
          address_2936 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2937 = 14 | F & 1;
          _address1399 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2938 = read(_address1399, 0);
          contend1x1(_address1399);
          _value1399 = operand_2938;
          int value3_2941 = nAndCarry_2937;
          _F1400 = value3_2941 & 1;
          value3_2941 = value3_2941 >>> 1;
          _F1400 = (_F1400 & 1) | 0x10 | (address_2936 & 0x28);
          if ((_value1399 & (0x01 << value3_2941)) == 0) {
              _F1400 |= 0x44;
          }
          if (value3_2941 == 7 && (_value1399 & 0x80) != 0) {
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
          int address_2943;
          address_2943 = ((IX + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_2944 = 14 | F & 1;
          _address1402 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2945 = read(_address1402, 0);
          contend1x1(_address1402);
          _value1402 = operand_2945;
          int value3_2948 = nAndCarry_2944;
          _F1403 = value3_2948 & 1;
          value3_2948 = value3_2948 >>> 1;
          _F1403 = (_F1403 & 1) | 0x10 | (address_2943 & 0x28);
          if ((_value1402 & (0x01 << value3_2948)) == 0) {
              _F1403 |= 0x44;
          }
          if (value3_2948 == 7 && (_value1402 & 0x80) != 0) {
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
          int operand_2950 = read(_address1405, 0);
          contend1x1(_address1405);
          _value1405 = operand_2950;
          int value_2951 = (_value1405 & -2);
          _address1405 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1405 = value_2951;
          write(_address1405, value_2951);
          int read_2952;
          read_2952 = _value1405;
          B = read_2952;
          MEMPTR = _address1405;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _value1407;
          int _address1407;
          contend2x1((PC + 3) & 0xFFFF);
          _address1407 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2953 = read(_address1407, 0);
          contend1x1(_address1407);
          _value1407 = operand_2953;
          int value_2954 = (_value1407 & -2);
          _address1407 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1407 = value_2954;
          write(_address1407, value_2954);
          int read_2955;
          read_2955 = _value1407;
          C = read_2955;
          MEMPTR = _address1407;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _value1409;
          int _address1409;
          contend2x1((PC + 3) & 0xFFFF);
          _address1409 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2956 = read(_address1409, 0);
          contend1x1(_address1409);
          _value1409 = operand_2956;
          int value_2957 = (_value1409 & -2);
          _address1409 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1409 = value_2957;
          write(_address1409, value_2957);
          int read_2958;
          read_2958 = _value1409;
          D = read_2958;
          MEMPTR = _address1409;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _value1411;
          int _address1411;
          contend2x1((PC + 3) & 0xFFFF);
          _address1411 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2959 = read(_address1411, 0);
          contend1x1(_address1411);
          _value1411 = operand_2959;
          int value_2960 = (_value1411 & -2);
          _address1411 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1411 = value_2960;
          write(_address1411, value_2960);
          int read_2961;
          read_2961 = _value1411;
          E = read_2961;
          MEMPTR = _address1411;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _value1413;
          int _address1413;
          contend2x1((PC + 3) & 0xFFFF);
          _address1413 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2962 = read(_address1413, 0);
          contend1x1(_address1413);
          _value1413 = operand_2962;
          int value_2963 = (_value1413 & -2);
          _address1413 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1413 = value_2963;
          write(_address1413, value_2963);
          int read_2964;
          read_2964 = _value1413;
          H = read_2964;
          MEMPTR = _address1413;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _value1415;
          int _address1415;
          contend2x1((PC + 3) & 0xFFFF);
          _address1415 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2965 = read(_address1415, 0);
          contend1x1(_address1415);
          _value1415 = operand_2965;
          int value_2966 = (_value1415 & -2);
          _address1415 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1415 = value_2966;
          write(_address1415, value_2966);
          int read_2967;
          read_2967 = _value1415;
          L = read_2967;
          MEMPTR = _address1415;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _value1417;
          int _address1417;
          contend2x1((PC + 3) & 0xFFFF);
          _address1417 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2968 = read(_address1417, 0);
          contend1x1(_address1417);
          _value1417 = operand_2968;
          int value_2969 = (_value1417 & -2);
          _address1417 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1417 = value_2969;
          write(_address1417, value_2969);
          MEMPTR = _address1417;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _value1419;
          int _address1419;
          contend2x1((PC + 3) & 0xFFFF);
          _address1419 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2970 = read(_address1419, 0);
          contend1x1(_address1419);
          _value1419 = operand_2970;
          int value_2971 = (_value1419 & -2);
          _address1419 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1419 = value_2971;
          write(_address1419, value_2971);
          int read_2972;
          read_2972 = _value1419;
          A = read_2972;
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
          int operand_2973 = read(_address1421, 0);
          contend1x1(_address1421);
          _value1421 = operand_2973;
          int value_2974 = (_value1421 & -3);
          _address1421 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1421 = value_2974;
          write(_address1421, value_2974);
          int read_2975;
          read_2975 = _value1421;
          B = read_2975;
          MEMPTR = _address1421;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _value1423;
          int _address1423;
          contend2x1((PC + 3) & 0xFFFF);
          _address1423 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2976 = read(_address1423, 0);
          contend1x1(_address1423);
          _value1423 = operand_2976;
          int value_2977 = (_value1423 & -3);
          _address1423 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1423 = value_2977;
          write(_address1423, value_2977);
          int read_2978;
          read_2978 = _value1423;
          C = read_2978;
          MEMPTR = _address1423;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _value1425;
          int _address1425;
          contend2x1((PC + 3) & 0xFFFF);
          _address1425 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2979 = read(_address1425, 0);
          contend1x1(_address1425);
          _value1425 = operand_2979;
          int value_2980 = (_value1425 & -3);
          _address1425 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1425 = value_2980;
          write(_address1425, value_2980);
          int read_2981;
          read_2981 = _value1425;
          D = read_2981;
          MEMPTR = _address1425;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _value1427;
          int _address1427;
          contend2x1((PC + 3) & 0xFFFF);
          _address1427 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2982 = read(_address1427, 0);
          contend1x1(_address1427);
          _value1427 = operand_2982;
          int value_2983 = (_value1427 & -3);
          _address1427 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1427 = value_2983;
          write(_address1427, value_2983);
          int read_2984;
          read_2984 = _value1427;
          E = read_2984;
          MEMPTR = _address1427;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _value1429;
          int _address1429;
          contend2x1((PC + 3) & 0xFFFF);
          _address1429 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2985 = read(_address1429, 0);
          contend1x1(_address1429);
          _value1429 = operand_2985;
          int value_2986 = (_value1429 & -3);
          _address1429 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1429 = value_2986;
          write(_address1429, value_2986);
          int read_2987;
          read_2987 = _value1429;
          H = read_2987;
          MEMPTR = _address1429;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _value1431;
          int _address1431;
          contend2x1((PC + 3) & 0xFFFF);
          _address1431 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2988 = read(_address1431, 0);
          contend1x1(_address1431);
          _value1431 = operand_2988;
          int value_2989 = (_value1431 & -3);
          _address1431 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1431 = value_2989;
          write(_address1431, value_2989);
          int read_2990;
          read_2990 = _value1431;
          L = read_2990;
          MEMPTR = _address1431;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _value1433;
          int _address1433;
          contend2x1((PC + 3) & 0xFFFF);
          _address1433 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2991 = read(_address1433, 0);
          contend1x1(_address1433);
          _value1433 = operand_2991;
          int value_2992 = (_value1433 & -3);
          _address1433 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1433 = value_2992;
          write(_address1433, value_2992);
          MEMPTR = _address1433;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _value1435;
          int _address1435;
          contend2x1((PC + 3) & 0xFFFF);
          _address1435 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2993 = read(_address1435, 0);
          contend1x1(_address1435);
          _value1435 = operand_2993;
          int value_2994 = (_value1435 & -3);
          _address1435 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1435 = value_2994;
          write(_address1435, value_2994);
          int read_2995;
          read_2995 = _value1435;
          A = read_2995;
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
          int operand_2996 = read(_address1437, 0);
          contend1x1(_address1437);
          _value1437 = operand_2996;
          int value_2997 = (_value1437 & -5);
          _address1437 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1437 = value_2997;
          write(_address1437, value_2997);
          int read_2998;
          read_2998 = _value1437;
          B = read_2998;
          MEMPTR = _address1437;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _value1439;
          int _address1439;
          contend2x1((PC + 3) & 0xFFFF);
          _address1439 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_2999 = read(_address1439, 0);
          contend1x1(_address1439);
          _value1439 = operand_2999;
          int value_3000 = (_value1439 & -5);
          _address1439 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1439 = value_3000;
          write(_address1439, value_3000);
          int read_3001;
          read_3001 = _value1439;
          C = read_3001;
          MEMPTR = _address1439;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _value1441;
          int _address1441;
          contend2x1((PC + 3) & 0xFFFF);
          _address1441 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3002 = read(_address1441, 0);
          contend1x1(_address1441);
          _value1441 = operand_3002;
          int value_3003 = (_value1441 & -5);
          _address1441 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1441 = value_3003;
          write(_address1441, value_3003);
          int read_3004;
          read_3004 = _value1441;
          D = read_3004;
          MEMPTR = _address1441;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _value1443;
          int _address1443;
          contend2x1((PC + 3) & 0xFFFF);
          _address1443 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3005 = read(_address1443, 0);
          contend1x1(_address1443);
          _value1443 = operand_3005;
          int value_3006 = (_value1443 & -5);
          _address1443 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1443 = value_3006;
          write(_address1443, value_3006);
          int read_3007;
          read_3007 = _value1443;
          E = read_3007;
          MEMPTR = _address1443;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _value1445;
          int _address1445;
          contend2x1((PC + 3) & 0xFFFF);
          _address1445 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3008 = read(_address1445, 0);
          contend1x1(_address1445);
          _value1445 = operand_3008;
          int value_3009 = (_value1445 & -5);
          _address1445 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1445 = value_3009;
          write(_address1445, value_3009);
          int read_3010;
          read_3010 = _value1445;
          H = read_3010;
          MEMPTR = _address1445;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _value1447;
          int _address1447;
          contend2x1((PC + 3) & 0xFFFF);
          _address1447 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3011 = read(_address1447, 0);
          contend1x1(_address1447);
          _value1447 = operand_3011;
          int value_3012 = (_value1447 & -5);
          _address1447 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1447 = value_3012;
          write(_address1447, value_3012);
          int read_3013;
          read_3013 = _value1447;
          L = read_3013;
          MEMPTR = _address1447;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _value1449;
          int _address1449;
          contend2x1((PC + 3) & 0xFFFF);
          _address1449 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3014 = read(_address1449, 0);
          contend1x1(_address1449);
          _value1449 = operand_3014;
          int value_3015 = (_value1449 & -5);
          _address1449 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1449 = value_3015;
          write(_address1449, value_3015);
          MEMPTR = _address1449;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _value1451;
          int _address1451;
          contend2x1((PC + 3) & 0xFFFF);
          _address1451 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3016 = read(_address1451, 0);
          contend1x1(_address1451);
          _value1451 = operand_3016;
          int value_3017 = (_value1451 & -5);
          _address1451 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1451 = value_3017;
          write(_address1451, value_3017);
          int read_3018;
          read_3018 = _value1451;
          A = read_3018;
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
          int operand_3019 = read(_address1453, 0);
          contend1x1(_address1453);
          _value1453 = operand_3019;
          int value_3020 = (_value1453 & -9);
          _address1453 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1453 = value_3020;
          write(_address1453, value_3020);
          int read_3021;
          read_3021 = _value1453;
          B = read_3021;
          MEMPTR = _address1453;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _value1455;
          int _address1455;
          contend2x1((PC + 3) & 0xFFFF);
          _address1455 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3022 = read(_address1455, 0);
          contend1x1(_address1455);
          _value1455 = operand_3022;
          int value_3023 = (_value1455 & -9);
          _address1455 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1455 = value_3023;
          write(_address1455, value_3023);
          int read_3024;
          read_3024 = _value1455;
          C = read_3024;
          MEMPTR = _address1455;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _value1457;
          int _address1457;
          contend2x1((PC + 3) & 0xFFFF);
          _address1457 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3025 = read(_address1457, 0);
          contend1x1(_address1457);
          _value1457 = operand_3025;
          int value_3026 = (_value1457 & -9);
          _address1457 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1457 = value_3026;
          write(_address1457, value_3026);
          int read_3027;
          read_3027 = _value1457;
          D = read_3027;
          MEMPTR = _address1457;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _value1459;
          int _address1459;
          contend2x1((PC + 3) & 0xFFFF);
          _address1459 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3028 = read(_address1459, 0);
          contend1x1(_address1459);
          _value1459 = operand_3028;
          int value_3029 = (_value1459 & -9);
          _address1459 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1459 = value_3029;
          write(_address1459, value_3029);
          int read_3030;
          read_3030 = _value1459;
          E = read_3030;
          MEMPTR = _address1459;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _value1461;
          int _address1461;
          contend2x1((PC + 3) & 0xFFFF);
          _address1461 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3031 = read(_address1461, 0);
          contend1x1(_address1461);
          _value1461 = operand_3031;
          int value_3032 = (_value1461 & -9);
          _address1461 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1461 = value_3032;
          write(_address1461, value_3032);
          int read_3033;
          read_3033 = _value1461;
          H = read_3033;
          MEMPTR = _address1461;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _value1463;
          int _address1463;
          contend2x1((PC + 3) & 0xFFFF);
          _address1463 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3034 = read(_address1463, 0);
          contend1x1(_address1463);
          _value1463 = operand_3034;
          int value_3035 = (_value1463 & -9);
          _address1463 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1463 = value_3035;
          write(_address1463, value_3035);
          int read_3036;
          read_3036 = _value1463;
          L = read_3036;
          MEMPTR = _address1463;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _value1465;
          int _address1465;
          contend2x1((PC + 3) & 0xFFFF);
          _address1465 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3037 = read(_address1465, 0);
          contend1x1(_address1465);
          _value1465 = operand_3037;
          int value_3038 = (_value1465 & -9);
          _address1465 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1465 = value_3038;
          write(_address1465, value_3038);
          MEMPTR = _address1465;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _value1467;
          int _address1467;
          contend2x1((PC + 3) & 0xFFFF);
          _address1467 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3039 = read(_address1467, 0);
          contend1x1(_address1467);
          _value1467 = operand_3039;
          int value_3040 = (_value1467 & -9);
          _address1467 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1467 = value_3040;
          write(_address1467, value_3040);
          int read_3041;
          read_3041 = _value1467;
          A = read_3041;
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
          int operand_3042 = read(_address1469, 0);
          contend1x1(_address1469);
          _value1469 = operand_3042;
          int value_3043 = (_value1469 & -17);
          _address1469 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1469 = value_3043;
          write(_address1469, value_3043);
          int read_3044;
          read_3044 = _value1469;
          B = read_3044;
          MEMPTR = _address1469;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _value1471;
          int _address1471;
          contend2x1((PC + 3) & 0xFFFF);
          _address1471 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3045 = read(_address1471, 0);
          contend1x1(_address1471);
          _value1471 = operand_3045;
          int value_3046 = (_value1471 & -17);
          _address1471 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1471 = value_3046;
          write(_address1471, value_3046);
          int read_3047;
          read_3047 = _value1471;
          C = read_3047;
          MEMPTR = _address1471;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _value1473;
          int _address1473;
          contend2x1((PC + 3) & 0xFFFF);
          _address1473 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3048 = read(_address1473, 0);
          contend1x1(_address1473);
          _value1473 = operand_3048;
          int value_3049 = (_value1473 & -17);
          _address1473 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1473 = value_3049;
          write(_address1473, value_3049);
          int read_3050;
          read_3050 = _value1473;
          D = read_3050;
          MEMPTR = _address1473;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _value1475;
          int _address1475;
          contend2x1((PC + 3) & 0xFFFF);
          _address1475 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3051 = read(_address1475, 0);
          contend1x1(_address1475);
          _value1475 = operand_3051;
          int value_3052 = (_value1475 & -17);
          _address1475 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1475 = value_3052;
          write(_address1475, value_3052);
          int read_3053;
          read_3053 = _value1475;
          E = read_3053;
          MEMPTR = _address1475;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _value1477;
          int _address1477;
          contend2x1((PC + 3) & 0xFFFF);
          _address1477 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3054 = read(_address1477, 0);
          contend1x1(_address1477);
          _value1477 = operand_3054;
          int value_3055 = (_value1477 & -17);
          _address1477 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1477 = value_3055;
          write(_address1477, value_3055);
          int read_3056;
          read_3056 = _value1477;
          H = read_3056;
          MEMPTR = _address1477;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _value1479;
          int _address1479;
          contend2x1((PC + 3) & 0xFFFF);
          _address1479 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3057 = read(_address1479, 0);
          contend1x1(_address1479);
          _value1479 = operand_3057;
          int value_3058 = (_value1479 & -17);
          _address1479 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1479 = value_3058;
          write(_address1479, value_3058);
          int read_3059;
          read_3059 = _value1479;
          L = read_3059;
          MEMPTR = _address1479;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _value1481;
          int _address1481;
          contend2x1((PC + 3) & 0xFFFF);
          _address1481 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3060 = read(_address1481, 0);
          contend1x1(_address1481);
          _value1481 = operand_3060;
          int value_3061 = (_value1481 & -17);
          _address1481 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1481 = value_3061;
          write(_address1481, value_3061);
          MEMPTR = _address1481;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _value1483;
          int _address1483;
          contend2x1((PC + 3) & 0xFFFF);
          _address1483 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3062 = read(_address1483, 0);
          contend1x1(_address1483);
          _value1483 = operand_3062;
          int value_3063 = (_value1483 & -17);
          _address1483 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1483 = value_3063;
          write(_address1483, value_3063);
          int read_3064;
          read_3064 = _value1483;
          A = read_3064;
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
          int operand_3065 = read(_address1485, 0);
          contend1x1(_address1485);
          _value1485 = operand_3065;
          int value_3066 = (_value1485 & -33);
          _address1485 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1485 = value_3066;
          write(_address1485, value_3066);
          int read_3067;
          read_3067 = _value1485;
          B = read_3067;
          MEMPTR = _address1485;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _value1487;
          int _address1487;
          contend2x1((PC + 3) & 0xFFFF);
          _address1487 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3068 = read(_address1487, 0);
          contend1x1(_address1487);
          _value1487 = operand_3068;
          int value_3069 = (_value1487 & -33);
          _address1487 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1487 = value_3069;
          write(_address1487, value_3069);
          int read_3070;
          read_3070 = _value1487;
          C = read_3070;
          MEMPTR = _address1487;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _value1489;
          int _address1489;
          contend2x1((PC + 3) & 0xFFFF);
          _address1489 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3071 = read(_address1489, 0);
          contend1x1(_address1489);
          _value1489 = operand_3071;
          int value_3072 = (_value1489 & -33);
          _address1489 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1489 = value_3072;
          write(_address1489, value_3072);
          int read_3073;
          read_3073 = _value1489;
          D = read_3073;
          MEMPTR = _address1489;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _value1491;
          int _address1491;
          contend2x1((PC + 3) & 0xFFFF);
          _address1491 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3074 = read(_address1491, 0);
          contend1x1(_address1491);
          _value1491 = operand_3074;
          int value_3075 = (_value1491 & -33);
          _address1491 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1491 = value_3075;
          write(_address1491, value_3075);
          int read_3076;
          read_3076 = _value1491;
          E = read_3076;
          MEMPTR = _address1491;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _value1493;
          int _address1493;
          contend2x1((PC + 3) & 0xFFFF);
          _address1493 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3077 = read(_address1493, 0);
          contend1x1(_address1493);
          _value1493 = operand_3077;
          int value_3078 = (_value1493 & -33);
          _address1493 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1493 = value_3078;
          write(_address1493, value_3078);
          int read_3079;
          read_3079 = _value1493;
          H = read_3079;
          MEMPTR = _address1493;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _value1495;
          int _address1495;
          contend2x1((PC + 3) & 0xFFFF);
          _address1495 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3080 = read(_address1495, 0);
          contend1x1(_address1495);
          _value1495 = operand_3080;
          int value_3081 = (_value1495 & -33);
          _address1495 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1495 = value_3081;
          write(_address1495, value_3081);
          int read_3082;
          read_3082 = _value1495;
          L = read_3082;
          MEMPTR = _address1495;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _value1497;
          int _address1497;
          contend2x1((PC + 3) & 0xFFFF);
          _address1497 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3083 = read(_address1497, 0);
          contend1x1(_address1497);
          _value1497 = operand_3083;
          int value_3084 = (_value1497 & -33);
          _address1497 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1497 = value_3084;
          write(_address1497, value_3084);
          MEMPTR = _address1497;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _value1499;
          int _address1499;
          contend2x1((PC + 3) & 0xFFFF);
          _address1499 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3085 = read(_address1499, 0);
          contend1x1(_address1499);
          _value1499 = operand_3085;
          int value_3086 = (_value1499 & -33);
          _address1499 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1499 = value_3086;
          write(_address1499, value_3086);
          int read_3087;
          read_3087 = _value1499;
          A = read_3087;
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
          int operand_3088 = read(_address1501, 0);
          contend1x1(_address1501);
          _value1501 = operand_3088;
          int value_3089 = (_value1501 & -65);
          _address1501 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1501 = value_3089;
          write(_address1501, value_3089);
          int read_3090;
          read_3090 = _value1501;
          B = read_3090;
          MEMPTR = _address1501;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _value1503;
          int _address1503;
          contend2x1((PC + 3) & 0xFFFF);
          _address1503 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3091 = read(_address1503, 0);
          contend1x1(_address1503);
          _value1503 = operand_3091;
          int value_3092 = (_value1503 & -65);
          _address1503 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1503 = value_3092;
          write(_address1503, value_3092);
          int read_3093;
          read_3093 = _value1503;
          C = read_3093;
          MEMPTR = _address1503;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _value1505;
          int _address1505;
          contend2x1((PC + 3) & 0xFFFF);
          _address1505 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3094 = read(_address1505, 0);
          contend1x1(_address1505);
          _value1505 = operand_3094;
          int value_3095 = (_value1505 & -65);
          _address1505 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1505 = value_3095;
          write(_address1505, value_3095);
          int read_3096;
          read_3096 = _value1505;
          D = read_3096;
          MEMPTR = _address1505;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _value1507;
          int _address1507;
          contend2x1((PC + 3) & 0xFFFF);
          _address1507 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3097 = read(_address1507, 0);
          contend1x1(_address1507);
          _value1507 = operand_3097;
          int value_3098 = (_value1507 & -65);
          _address1507 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1507 = value_3098;
          write(_address1507, value_3098);
          int read_3099;
          read_3099 = _value1507;
          E = read_3099;
          MEMPTR = _address1507;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _value1509;
          int _address1509;
          contend2x1((PC + 3) & 0xFFFF);
          _address1509 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3100 = read(_address1509, 0);
          contend1x1(_address1509);
          _value1509 = operand_3100;
          int value_3101 = (_value1509 & -65);
          _address1509 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1509 = value_3101;
          write(_address1509, value_3101);
          int read_3102;
          read_3102 = _value1509;
          H = read_3102;
          MEMPTR = _address1509;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _value1511;
          int _address1511;
          contend2x1((PC + 3) & 0xFFFF);
          _address1511 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3103 = read(_address1511, 0);
          contend1x1(_address1511);
          _value1511 = operand_3103;
          int value_3104 = (_value1511 & -65);
          _address1511 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1511 = value_3104;
          write(_address1511, value_3104);
          int read_3105;
          read_3105 = _value1511;
          L = read_3105;
          MEMPTR = _address1511;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _value1513;
          int _address1513;
          contend2x1((PC + 3) & 0xFFFF);
          _address1513 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3106 = read(_address1513, 0);
          contend1x1(_address1513);
          _value1513 = operand_3106;
          int value_3107 = (_value1513 & -65);
          _address1513 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1513 = value_3107;
          write(_address1513, value_3107);
          MEMPTR = _address1513;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _value1515;
          int _address1515;
          contend2x1((PC + 3) & 0xFFFF);
          _address1515 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3108 = read(_address1515, 0);
          contend1x1(_address1515);
          _value1515 = operand_3108;
          int value_3109 = (_value1515 & -65);
          _address1515 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1515 = value_3109;
          write(_address1515, value_3109);
          int read_3110;
          read_3110 = _value1515;
          A = read_3110;
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
          int operand_3111 = read(_address1517, 0);
          contend1x1(_address1517);
          _value1517 = operand_3111;
          int value_3112 = (_value1517 & -129);
          _address1517 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1517 = value_3112;
          write(_address1517, value_3112);
          int read_3113;
          read_3113 = _value1517;
          B = read_3113;
          MEMPTR = _address1517;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _value1519;
          int _address1519;
          contend2x1((PC + 3) & 0xFFFF);
          _address1519 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3114 = read(_address1519, 0);
          contend1x1(_address1519);
          _value1519 = operand_3114;
          int value_3115 = (_value1519 & -129);
          _address1519 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1519 = value_3115;
          write(_address1519, value_3115);
          int read_3116;
          read_3116 = _value1519;
          C = read_3116;
          MEMPTR = _address1519;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _value1521;
          int _address1521;
          contend2x1((PC + 3) & 0xFFFF);
          _address1521 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3117 = read(_address1521, 0);
          contend1x1(_address1521);
          _value1521 = operand_3117;
          int value_3118 = (_value1521 & -129);
          _address1521 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1521 = value_3118;
          write(_address1521, value_3118);
          int read_3119;
          read_3119 = _value1521;
          D = read_3119;
          MEMPTR = _address1521;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _value1523;
          int _address1523;
          contend2x1((PC + 3) & 0xFFFF);
          _address1523 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3120 = read(_address1523, 0);
          contend1x1(_address1523);
          _value1523 = operand_3120;
          int value_3121 = (_value1523 & -129);
          _address1523 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1523 = value_3121;
          write(_address1523, value_3121);
          int read_3122;
          read_3122 = _value1523;
          E = read_3122;
          MEMPTR = _address1523;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _value1525;
          int _address1525;
          contend2x1((PC + 3) & 0xFFFF);
          _address1525 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3123 = read(_address1525, 0);
          contend1x1(_address1525);
          _value1525 = operand_3123;
          int value_3124 = (_value1525 & -129);
          _address1525 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1525 = value_3124;
          write(_address1525, value_3124);
          int read_3125;
          read_3125 = _value1525;
          H = read_3125;
          MEMPTR = _address1525;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _value1527;
          int _address1527;
          contend2x1((PC + 3) & 0xFFFF);
          _address1527 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3126 = read(_address1527, 0);
          contend1x1(_address1527);
          _value1527 = operand_3126;
          int value_3127 = (_value1527 & -129);
          _address1527 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1527 = value_3127;
          write(_address1527, value_3127);
          int read_3128;
          read_3128 = _value1527;
          L = read_3128;
          MEMPTR = _address1527;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _value1529;
          int _address1529;
          contend2x1((PC + 3) & 0xFFFF);
          _address1529 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3129 = read(_address1529, 0);
          contend1x1(_address1529);
          _value1529 = operand_3129;
          int value_3130 = (_value1529 & -129);
          _address1529 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1529 = value_3130;
          write(_address1529, value_3130);
          MEMPTR = _address1529;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _value1531;
          int _address1531;
          contend2x1((PC + 3) & 0xFFFF);
          _address1531 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3131 = read(_address1531, 0);
          contend1x1(_address1531);
          _value1531 = operand_3131;
          int value_3132 = (_value1531 & -129);
          _address1531 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1531 = value_3132;
          write(_address1531, value_3132);
          int read_3133;
          read_3133 = _value1531;
          A = read_3133;
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
          int operand_3134 = read(_address1533, 0);
          contend1x1(_address1533);
          _value1533 = operand_3134;
          int value_3135 = (_value1533 | 1);
          _address1533 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1533 = value_3135;
          write(_address1533, value_3135);
          int read_3136;
          read_3136 = _value1533;
          B = read_3136;
          MEMPTR = _address1533;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC1: {
          int _value1535;
          int _address1535;
          contend2x1((PC + 3) & 0xFFFF);
          _address1535 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3137 = read(_address1535, 0);
          contend1x1(_address1535);
          _value1535 = operand_3137;
          int value_3138 = (_value1535 | 1);
          _address1535 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1535 = value_3138;
          write(_address1535, value_3138);
          int read_3139;
          read_3139 = _value1535;
          C = read_3139;
          MEMPTR = _address1535;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _value1537;
          int _address1537;
          contend2x1((PC + 3) & 0xFFFF);
          _address1537 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3140 = read(_address1537, 0);
          contend1x1(_address1537);
          _value1537 = operand_3140;
          int value_3141 = (_value1537 | 1);
          _address1537 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1537 = value_3141;
          write(_address1537, value_3141);
          int read_3142;
          read_3142 = _value1537;
          D = read_3142;
          MEMPTR = _address1537;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC3: {
          int _value1539;
          int _address1539;
          contend2x1((PC + 3) & 0xFFFF);
          _address1539 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3143 = read(_address1539, 0);
          contend1x1(_address1539);
          _value1539 = operand_3143;
          int value_3144 = (_value1539 | 1);
          _address1539 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1539 = value_3144;
          write(_address1539, value_3144);
          int read_3145;
          read_3145 = _value1539;
          E = read_3145;
          MEMPTR = _address1539;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC4: {
          int _value1541;
          int _address1541;
          contend2x1((PC + 3) & 0xFFFF);
          _address1541 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3146 = read(_address1541, 0);
          contend1x1(_address1541);
          _value1541 = operand_3146;
          int value_3147 = (_value1541 | 1);
          _address1541 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1541 = value_3147;
          write(_address1541, value_3147);
          int read_3148;
          read_3148 = _value1541;
          H = read_3148;
          MEMPTR = _address1541;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC5: {
          int _value1543;
          int _address1543;
          contend2x1((PC + 3) & 0xFFFF);
          _address1543 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3149 = read(_address1543, 0);
          contend1x1(_address1543);
          _value1543 = operand_3149;
          int value_3150 = (_value1543 | 1);
          _address1543 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1543 = value_3150;
          write(_address1543, value_3150);
          int read_3151;
          read_3151 = _value1543;
          L = read_3151;
          MEMPTR = _address1543;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _value1545;
          int _address1545;
          contend2x1((PC + 3) & 0xFFFF);
          _address1545 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3152 = read(_address1545, 0);
          contend1x1(_address1545);
          _value1545 = operand_3152;
          int value_3153 = (_value1545 | 1);
          _address1545 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1545 = value_3153;
          write(_address1545, value_3153);
          MEMPTR = _address1545;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _value1547;
          int _address1547;
          contend2x1((PC + 3) & 0xFFFF);
          _address1547 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3154 = read(_address1547, 0);
          contend1x1(_address1547);
          _value1547 = operand_3154;
          int value_3155 = (_value1547 | 1);
          _address1547 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1547 = value_3155;
          write(_address1547, value_3155);
          int read_3156;
          read_3156 = _value1547;
          A = read_3156;
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
          int operand_3157 = read(_address1549, 0);
          contend1x1(_address1549);
          _value1549 = operand_3157;
          int value_3158 = (_value1549 | 2);
          _address1549 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1549 = value_3158;
          write(_address1549, value_3158);
          int read_3159;
          read_3159 = _value1549;
          B = read_3159;
          MEMPTR = _address1549;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC9: {
          int _value1551;
          int _address1551;
          contend2x1((PC + 3) & 0xFFFF);
          _address1551 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3160 = read(_address1551, 0);
          contend1x1(_address1551);
          _value1551 = operand_3160;
          int value_3161 = (_value1551 | 2);
          _address1551 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1551 = value_3161;
          write(_address1551, value_3161);
          int read_3162;
          read_3162 = _value1551;
          C = read_3162;
          MEMPTR = _address1551;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCA: {
          int _value1553;
          int _address1553;
          contend2x1((PC + 3) & 0xFFFF);
          _address1553 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3163 = read(_address1553, 0);
          contend1x1(_address1553);
          _value1553 = operand_3163;
          int value_3164 = (_value1553 | 2);
          _address1553 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1553 = value_3164;
          write(_address1553, value_3164);
          int read_3165;
          read_3165 = _value1553;
          D = read_3165;
          MEMPTR = _address1553;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCB: {
          int _value1555;
          int _address1555;
          contend2x1((PC + 3) & 0xFFFF);
          _address1555 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3166 = read(_address1555, 0);
          contend1x1(_address1555);
          _value1555 = operand_3166;
          int value_3167 = (_value1555 | 2);
          _address1555 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1555 = value_3167;
          write(_address1555, value_3167);
          int read_3168;
          read_3168 = _value1555;
          E = read_3168;
          MEMPTR = _address1555;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCC: {
          int _value1557;
          int _address1557;
          contend2x1((PC + 3) & 0xFFFF);
          _address1557 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3169 = read(_address1557, 0);
          contend1x1(_address1557);
          _value1557 = operand_3169;
          int value_3170 = (_value1557 | 2);
          _address1557 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1557 = value_3170;
          write(_address1557, value_3170);
          int read_3171;
          read_3171 = _value1557;
          H = read_3171;
          MEMPTR = _address1557;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCD: {
          int _value1559;
          int _address1559;
          contend2x1((PC + 3) & 0xFFFF);
          _address1559 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3172 = read(_address1559, 0);
          contend1x1(_address1559);
          _value1559 = operand_3172;
          int value_3173 = (_value1559 | 2);
          _address1559 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1559 = value_3173;
          write(_address1559, value_3173);
          int read_3174;
          read_3174 = _value1559;
          L = read_3174;
          MEMPTR = _address1559;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCE: {
          int _value1561;
          int _address1561;
          contend2x1((PC + 3) & 0xFFFF);
          _address1561 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3175 = read(_address1561, 0);
          contend1x1(_address1561);
          _value1561 = operand_3175;
          int value_3176 = (_value1561 | 2);
          _address1561 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1561 = value_3176;
          write(_address1561, value_3176);
          MEMPTR = _address1561;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _value1563;
          int _address1563;
          contend2x1((PC + 3) & 0xFFFF);
          _address1563 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3177 = read(_address1563, 0);
          contend1x1(_address1563);
          _value1563 = operand_3177;
          int value_3178 = (_value1563 | 2);
          _address1563 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1563 = value_3178;
          write(_address1563, value_3178);
          int read_3179;
          read_3179 = _value1563;
          A = read_3179;
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
          int operand_3180 = read(_address1565, 0);
          contend1x1(_address1565);
          _value1565 = operand_3180;
          int value_3181 = (_value1565 | 4);
          _address1565 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1565 = value_3181;
          write(_address1565, value_3181);
          int read_3182;
          read_3182 = _value1565;
          B = read_3182;
          MEMPTR = _address1565;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD1: {
          int _value1567;
          int _address1567;
          contend2x1((PC + 3) & 0xFFFF);
          _address1567 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3183 = read(_address1567, 0);
          contend1x1(_address1567);
          _value1567 = operand_3183;
          int value_3184 = (_value1567 | 4);
          _address1567 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1567 = value_3184;
          write(_address1567, value_3184);
          int read_3185;
          read_3185 = _value1567;
          C = read_3185;
          MEMPTR = _address1567;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _value1569;
          int _address1569;
          contend2x1((PC + 3) & 0xFFFF);
          _address1569 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3186 = read(_address1569, 0);
          contend1x1(_address1569);
          _value1569 = operand_3186;
          int value_3187 = (_value1569 | 4);
          _address1569 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1569 = value_3187;
          write(_address1569, value_3187);
          int read_3188;
          read_3188 = _value1569;
          D = read_3188;
          MEMPTR = _address1569;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD3: {
          int _value1571;
          int _address1571;
          contend2x1((PC + 3) & 0xFFFF);
          _address1571 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3189 = read(_address1571, 0);
          contend1x1(_address1571);
          _value1571 = operand_3189;
          int value_3190 = (_value1571 | 4);
          _address1571 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1571 = value_3190;
          write(_address1571, value_3190);
          int read_3191;
          read_3191 = _value1571;
          E = read_3191;
          MEMPTR = _address1571;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _value1573;
          int _address1573;
          contend2x1((PC + 3) & 0xFFFF);
          _address1573 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3192 = read(_address1573, 0);
          contend1x1(_address1573);
          _value1573 = operand_3192;
          int value_3193 = (_value1573 | 4);
          _address1573 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1573 = value_3193;
          write(_address1573, value_3193);
          int read_3194;
          read_3194 = _value1573;
          H = read_3194;
          MEMPTR = _address1573;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD5: {
          int _value1575;
          int _address1575;
          contend2x1((PC + 3) & 0xFFFF);
          _address1575 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3195 = read(_address1575, 0);
          contend1x1(_address1575);
          _value1575 = operand_3195;
          int value_3196 = (_value1575 | 4);
          _address1575 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1575 = value_3196;
          write(_address1575, value_3196);
          int read_3197;
          read_3197 = _value1575;
          L = read_3197;
          MEMPTR = _address1575;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _value1577;
          int _address1577;
          contend2x1((PC + 3) & 0xFFFF);
          _address1577 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3198 = read(_address1577, 0);
          contend1x1(_address1577);
          _value1577 = operand_3198;
          int value_3199 = (_value1577 | 4);
          _address1577 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1577 = value_3199;
          write(_address1577, value_3199);
          MEMPTR = _address1577;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _value1579;
          int _address1579;
          contend2x1((PC + 3) & 0xFFFF);
          _address1579 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3200 = read(_address1579, 0);
          contend1x1(_address1579);
          _value1579 = operand_3200;
          int value_3201 = (_value1579 | 4);
          _address1579 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1579 = value_3201;
          write(_address1579, value_3201);
          int read_3202;
          read_3202 = _value1579;
          A = read_3202;
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
          int operand_3203 = read(_address1581, 0);
          contend1x1(_address1581);
          _value1581 = operand_3203;
          int value_3204 = (_value1581 | 8);
          _address1581 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1581 = value_3204;
          write(_address1581, value_3204);
          int read_3205;
          read_3205 = _value1581;
          B = read_3205;
          MEMPTR = _address1581;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD9: {
          int _value1583;
          int _address1583;
          contend2x1((PC + 3) & 0xFFFF);
          _address1583 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3206 = read(_address1583, 0);
          contend1x1(_address1583);
          _value1583 = operand_3206;
          int value_3207 = (_value1583 | 8);
          _address1583 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1583 = value_3207;
          write(_address1583, value_3207);
          int read_3208;
          read_3208 = _value1583;
          C = read_3208;
          MEMPTR = _address1583;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _value1585;
          int _address1585;
          contend2x1((PC + 3) & 0xFFFF);
          _address1585 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3209 = read(_address1585, 0);
          contend1x1(_address1585);
          _value1585 = operand_3209;
          int value_3210 = (_value1585 | 8);
          _address1585 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1585 = value_3210;
          write(_address1585, value_3210);
          int read_3211;
          read_3211 = _value1585;
          D = read_3211;
          MEMPTR = _address1585;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDB: {
          int _value1587;
          int _address1587;
          contend2x1((PC + 3) & 0xFFFF);
          _address1587 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3212 = read(_address1587, 0);
          contend1x1(_address1587);
          _value1587 = operand_3212;
          int value_3213 = (_value1587 | 8);
          _address1587 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1587 = value_3213;
          write(_address1587, value_3213);
          int read_3214;
          read_3214 = _value1587;
          E = read_3214;
          MEMPTR = _address1587;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _value1589;
          int _address1589;
          contend2x1((PC + 3) & 0xFFFF);
          _address1589 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3215 = read(_address1589, 0);
          contend1x1(_address1589);
          _value1589 = operand_3215;
          int value_3216 = (_value1589 | 8);
          _address1589 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1589 = value_3216;
          write(_address1589, value_3216);
          int read_3217;
          read_3217 = _value1589;
          H = read_3217;
          MEMPTR = _address1589;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDD: {
          int _value1591;
          int _address1591;
          contend2x1((PC + 3) & 0xFFFF);
          _address1591 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3218 = read(_address1591, 0);
          contend1x1(_address1591);
          _value1591 = operand_3218;
          int value_3219 = (_value1591 | 8);
          _address1591 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1591 = value_3219;
          write(_address1591, value_3219);
          int read_3220;
          read_3220 = _value1591;
          L = read_3220;
          MEMPTR = _address1591;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _value1593;
          int _address1593;
          contend2x1((PC + 3) & 0xFFFF);
          _address1593 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3221 = read(_address1593, 0);
          contend1x1(_address1593);
          _value1593 = operand_3221;
          int value_3222 = (_value1593 | 8);
          _address1593 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1593 = value_3222;
          write(_address1593, value_3222);
          MEMPTR = _address1593;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _value1595;
          int _address1595;
          contend2x1((PC + 3) & 0xFFFF);
          _address1595 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3223 = read(_address1595, 0);
          contend1x1(_address1595);
          _value1595 = operand_3223;
          int value_3224 = (_value1595 | 8);
          _address1595 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1595 = value_3224;
          write(_address1595, value_3224);
          int read_3225;
          read_3225 = _value1595;
          A = read_3225;
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
          int operand_3226 = read(_address1597, 0);
          contend1x1(_address1597);
          _value1597 = operand_3226;
          int value_3227 = (_value1597 | 0x10);
          _address1597 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1597 = value_3227;
          write(_address1597, value_3227);
          int read_3228;
          read_3228 = _value1597;
          B = read_3228;
          MEMPTR = _address1597;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE1: {
          int _value1599;
          int _address1599;
          contend2x1((PC + 3) & 0xFFFF);
          _address1599 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3229 = read(_address1599, 0);
          contend1x1(_address1599);
          _value1599 = operand_3229;
          int value_3230 = (_value1599 | 0x10);
          _address1599 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1599 = value_3230;
          write(_address1599, value_3230);
          int read_3231;
          read_3231 = _value1599;
          C = read_3231;
          MEMPTR = _address1599;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _value1601;
          int _address1601;
          contend2x1((PC + 3) & 0xFFFF);
          _address1601 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3232 = read(_address1601, 0);
          contend1x1(_address1601);
          _value1601 = operand_3232;
          int value_3233 = (_value1601 | 0x10);
          _address1601 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1601 = value_3233;
          write(_address1601, value_3233);
          int read_3234;
          read_3234 = _value1601;
          D = read_3234;
          MEMPTR = _address1601;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE3: {
          int _value1603;
          int _address1603;
          contend2x1((PC + 3) & 0xFFFF);
          _address1603 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3235 = read(_address1603, 0);
          contend1x1(_address1603);
          _value1603 = operand_3235;
          int value_3236 = (_value1603 | 0x10);
          _address1603 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1603 = value_3236;
          write(_address1603, value_3236);
          int read_3237;
          read_3237 = _value1603;
          E = read_3237;
          MEMPTR = _address1603;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _value1605;
          int _address1605;
          contend2x1((PC + 3) & 0xFFFF);
          _address1605 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3238 = read(_address1605, 0);
          contend1x1(_address1605);
          _value1605 = operand_3238;
          int value_3239 = (_value1605 | 0x10);
          _address1605 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1605 = value_3239;
          write(_address1605, value_3239);
          int read_3240;
          read_3240 = _value1605;
          H = read_3240;
          MEMPTR = _address1605;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE5: {
          int _value1607;
          int _address1607;
          contend2x1((PC + 3) & 0xFFFF);
          _address1607 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3241 = read(_address1607, 0);
          contend1x1(_address1607);
          _value1607 = operand_3241;
          int value_3242 = (_value1607 | 0x10);
          _address1607 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1607 = value_3242;
          write(_address1607, value_3242);
          int read_3243;
          read_3243 = _value1607;
          L = read_3243;
          MEMPTR = _address1607;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _value1609;
          int _address1609;
          contend2x1((PC + 3) & 0xFFFF);
          _address1609 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3244 = read(_address1609, 0);
          contend1x1(_address1609);
          _value1609 = operand_3244;
          int value_3245 = (_value1609 | 0x10);
          _address1609 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1609 = value_3245;
          write(_address1609, value_3245);
          MEMPTR = _address1609;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _value1611;
          int _address1611;
          contend2x1((PC + 3) & 0xFFFF);
          _address1611 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3246 = read(_address1611, 0);
          contend1x1(_address1611);
          _value1611 = operand_3246;
          int value_3247 = (_value1611 | 0x10);
          _address1611 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1611 = value_3247;
          write(_address1611, value_3247);
          int read_3248;
          read_3248 = _value1611;
          A = read_3248;
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
          int operand_3249 = read(_address1613, 0);
          contend1x1(_address1613);
          _value1613 = operand_3249;
          int value_3250 = (_value1613 | 0x20);
          _address1613 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1613 = value_3250;
          write(_address1613, value_3250);
          int read_3251;
          read_3251 = _value1613;
          B = read_3251;
          MEMPTR = _address1613;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE9: {
          int _value1615;
          int _address1615;
          contend2x1((PC + 3) & 0xFFFF);
          _address1615 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3252 = read(_address1615, 0);
          contend1x1(_address1615);
          _value1615 = operand_3252;
          int value_3253 = (_value1615 | 0x20);
          _address1615 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1615 = value_3253;
          write(_address1615, value_3253);
          int read_3254;
          read_3254 = _value1615;
          C = read_3254;
          MEMPTR = _address1615;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEA: {
          int _value1617;
          int _address1617;
          contend2x1((PC + 3) & 0xFFFF);
          _address1617 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3255 = read(_address1617, 0);
          contend1x1(_address1617);
          _value1617 = operand_3255;
          int value_3256 = (_value1617 | 0x20);
          _address1617 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1617 = value_3256;
          write(_address1617, value_3256);
          int read_3257;
          read_3257 = _value1617;
          D = read_3257;
          MEMPTR = _address1617;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEB: {
          int _value1619;
          int _address1619;
          contend2x1((PC + 3) & 0xFFFF);
          _address1619 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3258 = read(_address1619, 0);
          contend1x1(_address1619);
          _value1619 = operand_3258;
          int value_3259 = (_value1619 | 0x20);
          _address1619 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1619 = value_3259;
          write(_address1619, value_3259);
          int read_3260;
          read_3260 = _value1619;
          E = read_3260;
          MEMPTR = _address1619;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _value1621;
          int _address1621;
          contend2x1((PC + 3) & 0xFFFF);
          _address1621 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3261 = read(_address1621, 0);
          contend1x1(_address1621);
          _value1621 = operand_3261;
          int value_3262 = (_value1621 | 0x20);
          _address1621 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1621 = value_3262;
          write(_address1621, value_3262);
          int read_3263;
          read_3263 = _value1621;
          H = read_3263;
          MEMPTR = _address1621;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xED: {
          int _value1623;
          int _address1623;
          contend2x1((PC + 3) & 0xFFFF);
          _address1623 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3264 = read(_address1623, 0);
          contend1x1(_address1623);
          _value1623 = operand_3264;
          int value_3265 = (_value1623 | 0x20);
          _address1623 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1623 = value_3265;
          write(_address1623, value_3265);
          int read_3266;
          read_3266 = _value1623;
          L = read_3266;
          MEMPTR = _address1623;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _value1625;
          int _address1625;
          contend2x1((PC + 3) & 0xFFFF);
          _address1625 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3267 = read(_address1625, 0);
          contend1x1(_address1625);
          _value1625 = operand_3267;
          int value_3268 = (_value1625 | 0x20);
          _address1625 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1625 = value_3268;
          write(_address1625, value_3268);
          MEMPTR = _address1625;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _value1627;
          int _address1627;
          contend2x1((PC + 3) & 0xFFFF);
          _address1627 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3269 = read(_address1627, 0);
          contend1x1(_address1627);
          _value1627 = operand_3269;
          int value_3270 = (_value1627 | 0x20);
          _address1627 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1627 = value_3270;
          write(_address1627, value_3270);
          int read_3271;
          read_3271 = _value1627;
          A = read_3271;
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
          int operand_3272 = read(_address1629, 0);
          contend1x1(_address1629);
          _value1629 = operand_3272;
          int value_3273 = (_value1629 | 0x40);
          _address1629 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1629 = value_3273;
          write(_address1629, value_3273);
          int read_3274;
          read_3274 = _value1629;
          B = read_3274;
          MEMPTR = _address1629;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF1: {
          int _value1631;
          int _address1631;
          contend2x1((PC + 3) & 0xFFFF);
          _address1631 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3275 = read(_address1631, 0);
          contend1x1(_address1631);
          _value1631 = operand_3275;
          int value_3276 = (_value1631 | 0x40);
          _address1631 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1631 = value_3276;
          write(_address1631, value_3276);
          int read_3277;
          read_3277 = _value1631;
          C = read_3277;
          MEMPTR = _address1631;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _value1633;
          int _address1633;
          contend2x1((PC + 3) & 0xFFFF);
          _address1633 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3278 = read(_address1633, 0);
          contend1x1(_address1633);
          _value1633 = operand_3278;
          int value_3279 = (_value1633 | 0x40);
          _address1633 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1633 = value_3279;
          write(_address1633, value_3279);
          int read_3280;
          read_3280 = _value1633;
          D = read_3280;
          MEMPTR = _address1633;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF3: {
          int _value1635;
          int _address1635;
          contend2x1((PC + 3) & 0xFFFF);
          _address1635 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3281 = read(_address1635, 0);
          contend1x1(_address1635);
          _value1635 = operand_3281;
          int value_3282 = (_value1635 | 0x40);
          _address1635 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1635 = value_3282;
          write(_address1635, value_3282);
          int read_3283;
          read_3283 = _value1635;
          E = read_3283;
          MEMPTR = _address1635;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _value1637;
          int _address1637;
          contend2x1((PC + 3) & 0xFFFF);
          _address1637 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3284 = read(_address1637, 0);
          contend1x1(_address1637);
          _value1637 = operand_3284;
          int value_3285 = (_value1637 | 0x40);
          _address1637 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1637 = value_3285;
          write(_address1637, value_3285);
          int read_3286;
          read_3286 = _value1637;
          H = read_3286;
          MEMPTR = _address1637;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF5: {
          int _value1639;
          int _address1639;
          contend2x1((PC + 3) & 0xFFFF);
          _address1639 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3287 = read(_address1639, 0);
          contend1x1(_address1639);
          _value1639 = operand_3287;
          int value_3288 = (_value1639 | 0x40);
          _address1639 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1639 = value_3288;
          write(_address1639, value_3288);
          int read_3289;
          read_3289 = _value1639;
          L = read_3289;
          MEMPTR = _address1639;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _value1641;
          int _address1641;
          contend2x1((PC + 3) & 0xFFFF);
          _address1641 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3290 = read(_address1641, 0);
          contend1x1(_address1641);
          _value1641 = operand_3290;
          int value_3291 = (_value1641 | 0x40);
          _address1641 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1641 = value_3291;
          write(_address1641, value_3291);
          MEMPTR = _address1641;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _value1643;
          int _address1643;
          contend2x1((PC + 3) & 0xFFFF);
          _address1643 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3292 = read(_address1643, 0);
          contend1x1(_address1643);
          _value1643 = operand_3292;
          int value_3293 = (_value1643 | 0x40);
          _address1643 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1643 = value_3293;
          write(_address1643, value_3293);
          int read_3294;
          read_3294 = _value1643;
          A = read_3294;
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
          int operand_3295 = read(_address1645, 0);
          contend1x1(_address1645);
          _value1645 = operand_3295;
          int value_3296 = (_value1645 | 0x80);
          _address1645 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1645 = value_3296;
          write(_address1645, value_3296);
          int read_3297;
          read_3297 = _value1645;
          B = read_3297;
          MEMPTR = _address1645;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF9: {
          int _value1647;
          int _address1647;
          contend2x1((PC + 3) & 0xFFFF);
          _address1647 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3298 = read(_address1647, 0);
          contend1x1(_address1647);
          _value1647 = operand_3298;
          int value_3299 = (_value1647 | 0x80);
          _address1647 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1647 = value_3299;
          write(_address1647, value_3299);
          int read_3300;
          read_3300 = _value1647;
          C = read_3300;
          MEMPTR = _address1647;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _value1649;
          int _address1649;
          contend2x1((PC + 3) & 0xFFFF);
          _address1649 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3301 = read(_address1649, 0);
          contend1x1(_address1649);
          _value1649 = operand_3301;
          int value_3302 = (_value1649 | 0x80);
          _address1649 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1649 = value_3302;
          write(_address1649, value_3302);
          int read_3303;
          read_3303 = _value1649;
          D = read_3303;
          MEMPTR = _address1649;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFB: {
          int _value1651;
          int _address1651;
          contend2x1((PC + 3) & 0xFFFF);
          _address1651 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3304 = read(_address1651, 0);
          contend1x1(_address1651);
          _value1651 = operand_3304;
          int value_3305 = (_value1651 | 0x80);
          _address1651 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1651 = value_3305;
          write(_address1651, value_3305);
          int read_3306;
          read_3306 = _value1651;
          E = read_3306;
          MEMPTR = _address1651;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _value1653;
          int _address1653;
          contend2x1((PC + 3) & 0xFFFF);
          _address1653 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3307 = read(_address1653, 0);
          contend1x1(_address1653);
          _value1653 = operand_3307;
          int value_3308 = (_value1653 | 0x80);
          _address1653 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1653 = value_3308;
          write(_address1653, value_3308);
          int read_3309;
          read_3309 = _value1653;
          H = read_3309;
          MEMPTR = _address1653;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFD: {
          int _value1655;
          int _address1655;
          contend2x1((PC + 3) & 0xFFFF);
          _address1655 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3310 = read(_address1655, 0);
          contend1x1(_address1655);
          _value1655 = operand_3310;
          int value_3311 = (_value1655 | 0x80);
          _address1655 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1655 = value_3311;
          write(_address1655, value_3311);
          int read_3312;
          read_3312 = _value1655;
          L = read_3312;
          MEMPTR = _address1655;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _value1657;
          int _address1657;
          contend2x1((PC + 3) & 0xFFFF);
          _address1657 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3313 = read(_address1657, 0);
          contend1x1(_address1657);
          _value1657 = operand_3313;
          int value_3314 = (_value1657 | 0x80);
          _address1657 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1657 = value_3314;
          write(_address1657, value_3314);
          MEMPTR = _address1657;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _value1659;
          int _address1659;
          contend2x1((PC + 3) & 0xFFFF);
          _address1659 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          int operand_3315 = read(_address1659, 0);
          contend1x1(_address1659);
          _value1659 = operand_3315;
          int value_3316 = (_value1659 | 0x80);
          _address1659 = (IX + (int) ((byte) displacement)) & 0xFFFF;
          _value1659 = value_3316;
          write(_address1659, value_3316);
          int read_3317;
          read_3317 = _value1659;
          A = read_3317;
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
          int port_3653 = (B << 8) | C;
          int value_3654 = io.in(port_3653);
          B = value_3654 & 0xFF;
          int value1_3655 = value_3654 & 0xFF;
          int value2_3656 = F;
          _F1803 = value2_3656;
          _F1803 = (_F1803 & 1) | (SZ53P[value1_3655] | (value1_3655 == 0 ? 0x40 : 0));
          F = (_F1803 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x41: {
          int read_3659 = ((B << 8) | C);
          io.out(read_3659, B);
          int read_3660 = ((B << 8) | C);
          MEMPTR = ((read_3660 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x42: {
          int _F1806;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3661 = ((H << 8) | L);
          int b_3662 = ((B << 8) | C);
          int result_3663 = (a_3661 - b_3662 - (F & 1));
          int value1_3664 = ((result_3663 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3665 = (((a_3661 & 0x8800 | (b_3662 & 0x8800) >> 1) | ((result_3663 & 0x18800) | ((result_3663 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3667 = value2_3665 & 0x33;
          i_3667 |= i_3667 << 1 & 0x04;
          int result1_3668 = i_3667 << 11 & 0x1A800;
          int lookup_3669 = (value2_3665 << 8 & 0x8800) >> 11 | (value2_3665 << 9 & 0x8800) >> 10 | (result1_3668 & 0x8800) >> 9;
          _F1806 = ((result1_3668 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3669 >> 4)] | (result1_3668 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3669 & 0x07)] | (value1_3664 != 0 ? 0 : 0x40);
          F = (_F1806 & 0xFF);
          int value_3671 = (result_3663 & 0xffff);
          H = (value_3671 >>> 8);
          L = value_3671 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x43: {
          int _address1808;
          int address_3672 = (PC + 2) & 0xFFFF;
          int operand_3674 = read(address_3672, 0);
          int operand_3676 = read((address_3672 + 1) & 0xFFFF, 0);
          _address1808 = (operand_3676 << 8) | operand_3674;
          int value_3677 = ((B << 8) | C);
          write(_address1808, (value_3677 & 0xFF));
          write((_address1808 + 1) & 0xFFFF, (value_3677 >>> 8));
          MEMPTR = ((_address1808 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x44: {
          int _F1810;
          int value1_3678 = A;
          int value2_3679 = 0;
          int subtemp_3681 = value2_3679 - value1_3678;
          int lookup_3682 = ((value2_3679 & 0x88) >> 3) | ((value1_3678 & 0x88) >> 2) | ((subtemp_3681 & 0x88) >> 1);
          value2_3679 = subtemp_3681 & 0xff;
          _F1810 = ((subtemp_3681 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3682 & 0x07)] | OVERFLOW_SUB[(lookup_3682 >> 4)] | (SZ53[value2_3679] | (value2_3679 == 0 ? 0x40 : 0));
          F = (_F1810 & 0xFF);
          A = value2_3679;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x45: {
          int _nextPC1812;
          state.setIff1(state.isIff2());
          int jumpAddress2_3684;
          int wordNumber1_3686 = read(SP, 0);
          int wordNumber_3687 = read((SP + 1) & 0xFFFF, 0);
          int value_3685 = ((wordNumber_3687 << 8) | wordNumber1_3686);
          int wordNumber_3688 = SP;
          SP = ((wordNumber_3688 + 2) & 0xFFFF);
          jumpAddress2_3684 = value_3685;
          _nextPC1812 = jumpAddress2_3684;
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
          int port_3690 = (B << 8) | C;
          int value_3691 = io.in(port_3690);
          C = value_3691 & 0xFF;
          int value1_3692 = value_3691 & 0xFF;
          int value2_3693 = F;
          _F1815 = value2_3693;
          _F1815 = (_F1815 & 1) | (SZ53P[value1_3692] | (value1_3692 == 0 ? 0x40 : 0));
          F = (_F1815 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x49: {
          int read_3696 = ((B << 8) | C);
          io.out(read_3696, C);
          int read_3697 = ((B << 8) | C);
          MEMPTR = ((read_3697 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4A: {
          int _F1818;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3698 = ((B << 8) | C);
          int b_3699 = ((H << 8) | L);
          int result_3700 = (a_3698 + b_3699 + (F & 1));
          int value1_3701 = ((result_3700 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3702 = (((a_3698 & 0x8800 | (b_3699 & 0x8800) >> 1) | ((result_3700 & 0x18800) | ((result_3700 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3704 = value2_3702 & 0x33;
          i_3704 |= (i_3704 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3705 = (i_3704 << 11) & 0x1A800;
          int lookup_3706 = (value2_3702 << 8 & 0x8800) >> 11 | (value2_3702 << 9 & 0x8800) >> 10 | (result1_3705 & 0x8800) >> 9;
          _F1818 = ((result1_3705 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3706 >> 4)] | (result1_3705 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3706 & 0x07)] | (value1_3701 == 1 ? 0 : 0x40);
          F = (_F1818 & 0xFF);
          int value_3708 = (result_3700 & 0xffff);
          H = (value_3708 >>> 8);
          L = value_3708 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4B: {
          int _address1820;
          int address_3709 = (PC + 2) & 0xFFFF;
          int operand_3711 = read(address_3709, 0);
          int operand_3713 = read((address_3709 + 1) & 0xFFFF, 0);
          _address1820 = (operand_3713 << 8) | operand_3711;
          int wordNumber1_3714 = read(_address1820, 0);
          int wordNumber_3715 = read((_address1820 + 1) & 0xFFFF, 0);
          int value_3716 = ((wordNumber_3715 << 8) | wordNumber1_3714);
          B = (value_3716 >>> 8);
          C = value_3716 & 0xFF;
          MEMPTR = ((_address1820 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x4C: {
          int _F1822;
          int value1_3717 = A;
          int value2_3718 = 0;
          int subtemp_3720 = value2_3718 - value1_3717;
          int lookup_3721 = ((value2_3718 & 0x88) >> 3) | ((value1_3717 & 0x88) >> 2) | ((subtemp_3720 & 0x88) >> 1);
          value2_3718 = subtemp_3720 & 0xff;
          _F1822 = ((subtemp_3720 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3721 & 0x07)] | OVERFLOW_SUB[(lookup_3721 >> 4)] | (SZ53[value2_3718] | (value2_3718 == 0 ? 0x40 : 0));
          F = (_F1822 & 0xFF);
          A = value2_3718;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x4D: {
          int _nextPC1824;
          state.setIff1(state.isIff2());
          int jumpAddress2_3723;
          int wordNumber1_3725 = read(SP, 0);
          int wordNumber_3726 = read((SP + 1) & 0xFFFF, 0);
          int value_3724 = ((wordNumber_3726 << 8) | wordNumber1_3725);
          int wordNumber_3727 = SP;
          SP = ((wordNumber_3727 + 2) & 0xFFFF);
          jumpAddress2_3723 = value_3724;
          _nextPC1824 = jumpAddress2_3723;
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
          int port_3729 = (B << 8) | C;
          int value_3730 = io.in(port_3729);
          D = value_3730 & 0xFF;
          int value1_3731 = value_3730 & 0xFF;
          int value2_3732 = F;
          _F1827 = value2_3732;
          _F1827 = (_F1827 & 1) | (SZ53P[value1_3731] | (value1_3731 == 0 ? 0x40 : 0));
          F = (_F1827 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x51: {
          int read_3735 = ((B << 8) | C);
          io.out(read_3735, D);
          int read_3736 = ((B << 8) | C);
          MEMPTR = ((read_3736 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x52: {
          int _F1830;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3737 = ((H << 8) | L);
          int b_3738 = ((D << 8) | E);
          int result_3739 = (a_3737 - b_3738 - (F & 1));
          int value1_3740 = ((result_3739 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3741 = (((a_3737 & 0x8800 | (b_3738 & 0x8800) >> 1) | ((result_3739 & 0x18800) | ((result_3739 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3743 = value2_3741 & 0x33;
          i_3743 |= i_3743 << 1 & 0x04;
          int result1_3744 = i_3743 << 11 & 0x1A800;
          int lookup_3745 = (value2_3741 << 8 & 0x8800) >> 11 | (value2_3741 << 9 & 0x8800) >> 10 | (result1_3744 & 0x8800) >> 9;
          _F1830 = ((result1_3744 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3745 >> 4)] | (result1_3744 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3745 & 0x07)] | (value1_3740 != 0 ? 0 : 0x40);
          F = (_F1830 & 0xFF);
          int value_3747 = (result_3739 & 0xffff);
          H = (value_3747 >>> 8);
          L = value_3747 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x53: {
          int _address1832;
          int address_3748 = (PC + 2) & 0xFFFF;
          int operand_3750 = read(address_3748, 0);
          int operand_3752 = read((address_3748 + 1) & 0xFFFF, 0);
          _address1832 = (operand_3752 << 8) | operand_3750;
          int value_3753 = ((D << 8) | E);
          write(_address1832, (value_3753 & 0xFF));
          write((_address1832 + 1) & 0xFFFF, (value_3753 >>> 8));
          MEMPTR = ((_address1832 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x54: {
          int _F1834;
          int value1_3754 = A;
          int value2_3755 = 0;
          int subtemp_3757 = value2_3755 - value1_3754;
          int lookup_3758 = ((value2_3755 & 0x88) >> 3) | ((value1_3754 & 0x88) >> 2) | ((subtemp_3757 & 0x88) >> 1);
          value2_3755 = subtemp_3757 & 0xff;
          _F1834 = ((subtemp_3757 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3758 & 0x07)] | OVERFLOW_SUB[(lookup_3758 >> 4)] | (SZ53[value2_3755] | (value2_3755 == 0 ? 0x40 : 0));
          F = (_F1834 & 0xFF);
          A = value2_3755;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x55: {
          int _nextPC1836;
          state.setIff1(state.isIff2());
          int jumpAddress2_3760;
          int wordNumber1_3762 = read(SP, 0);
          int wordNumber_3763 = read((SP + 1) & 0xFFFF, 0);
          int value_3761 = ((wordNumber_3763 << 8) | wordNumber1_3762);
          int wordNumber_3764 = SP;
          SP = ((wordNumber_3764 + 2) & 0xFFFF);
          jumpAddress2_3760 = value_3761;
          _nextPC1836 = jumpAddress2_3760;
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
          boolean iff2_3767 = state.isIff2();
          int value2_3769 = F;
          int value3_3770 = (iff2_3767 ? 1 : 0);
          _F1838 = value2_3769;
          _F1838 = (_F1838 & 1) | (SZ53[I] | (I == 0 ? 0x40 : 0)) | (value3_3770 != 0 ? 4 : 0);
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
          int port_3773 = (B << 8) | C;
          int value_3774 = io.in(port_3773);
          E = value_3774 & 0xFF;
          int value1_3775 = value_3774 & 0xFF;
          int value2_3776 = F;
          _F1840 = value2_3776;
          _F1840 = (_F1840 & 1) | (SZ53P[value1_3775] | (value1_3775 == 0 ? 0x40 : 0));
          F = (_F1840 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x59: {
          int read_3779 = ((B << 8) | C);
          io.out(read_3779, E);
          int read_3780 = ((B << 8) | C);
          MEMPTR = ((read_3780 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5A: {
          int _F1843;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3781 = ((D << 8) | E);
          int b_3782 = ((H << 8) | L);
          int result_3783 = (a_3781 + b_3782 + (F & 1));
          int value1_3784 = ((result_3783 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3785 = (((a_3781 & 0x8800 | (b_3782 & 0x8800) >> 1) | ((result_3783 & 0x18800) | ((result_3783 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3787 = value2_3785 & 0x33;
          i_3787 |= (i_3787 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3788 = (i_3787 << 11) & 0x1A800;
          int lookup_3789 = (value2_3785 << 8 & 0x8800) >> 11 | (value2_3785 << 9 & 0x8800) >> 10 | (result1_3788 & 0x8800) >> 9;
          _F1843 = ((result1_3788 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3789 >> 4)] | (result1_3788 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3789 & 0x07)] | (value1_3784 == 1 ? 0 : 0x40);
          F = (_F1843 & 0xFF);
          int value_3791 = (result_3783 & 0xffff);
          H = (value_3791 >>> 8);
          L = value_3791 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5B: {
          int _address1845;
          int address_3792 = (PC + 2) & 0xFFFF;
          int operand_3794 = read(address_3792, 0);
          int operand_3796 = read((address_3792 + 1) & 0xFFFF, 0);
          _address1845 = (operand_3796 << 8) | operand_3794;
          int wordNumber1_3797 = read(_address1845, 0);
          int wordNumber_3798 = read((_address1845 + 1) & 0xFFFF, 0);
          int value_3799 = ((wordNumber_3798 << 8) | wordNumber1_3797);
          D = (value_3799 >>> 8);
          E = value_3799 & 0xFF;
          MEMPTR = ((_address1845 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x5C: {
          int _F1847;
          int value1_3800 = A;
          int value2_3801 = 0;
          int subtemp_3803 = value2_3801 - value1_3800;
          int lookup_3804 = ((value2_3801 & 0x88) >> 3) | ((value1_3800 & 0x88) >> 2) | ((subtemp_3803 & 0x88) >> 1);
          value2_3801 = subtemp_3803 & 0xff;
          _F1847 = ((subtemp_3803 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3804 & 0x07)] | OVERFLOW_SUB[(lookup_3804 >> 4)] | (SZ53[value2_3801] | (value2_3801 == 0 ? 0x40 : 0));
          F = (_F1847 & 0xFF);
          A = value2_3801;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x5D: {
          int _nextPC1849;
          state.setIff1(state.isIff2());
          int jumpAddress2_3806;
          int wordNumber1_3808 = read(SP, 0);
          int wordNumber_3809 = read((SP + 1) & 0xFFFF, 0);
          int value_3807 = ((wordNumber_3809 << 8) | wordNumber1_3808);
          int wordNumber_3810 = SP;
          SP = ((wordNumber_3810 + 2) & 0xFFFF);
          jumpAddress2_3806 = value_3807;
          _nextPC1849 = jumpAddress2_3806;
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
          int value2_3815 = F;
          int value3_3816 = (state.isIff2() ? 1 : 0);
          _F1851 = value2_3815;
          _F1851 = (_F1851 & 1) | (SZ53[R] | (R == 0 ? 0x40 : 0)) | (value3_3816 != 0 ? 4 : 0);
          int result_3818 = _F1851 & 0xFF;
          F = result_3818;
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
          int port_3819 = (B << 8) | C;
          int value_3820 = io.in(port_3819);
          H = value_3820 & 0xFF;
          int value1_3821 = value_3820 & 0xFF;
          int value2_3822 = F;
          _F1853 = value2_3822;
          _F1853 = (_F1853 & 1) | (SZ53P[value1_3821] | (value1_3821 == 0 ? 0x40 : 0));
          F = (_F1853 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x61: {
          int read_3825 = ((B << 8) | C);
          io.out(read_3825, H);
          int read_3826 = ((B << 8) | C);
          MEMPTR = ((read_3826 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x62: {
          int _F1856;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3827 = ((H << 8) | L);
          int b_3828 = ((H << 8) | L);
          int result_3829 = (a_3827 - b_3828 - (F & 1));
          int value1_3830 = ((result_3829 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3831 = (((a_3827 & 0x8800 | (b_3828 & 0x8800) >> 1) | ((result_3829 & 0x18800) | ((result_3829 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3833 = value2_3831 & 0x33;
          i_3833 |= i_3833 << 1 & 0x04;
          int result1_3834 = i_3833 << 11 & 0x1A800;
          int lookup_3835 = (value2_3831 << 8 & 0x8800) >> 11 | (value2_3831 << 9 & 0x8800) >> 10 | (result1_3834 & 0x8800) >> 9;
          _F1856 = ((result1_3834 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3835 >> 4)] | (result1_3834 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3835 & 0x07)] | (value1_3830 != 0 ? 0 : 0x40);
          F = (_F1856 & 0xFF);
          int value_3837 = (result_3829 & 0xffff);
          H = (value_3837 >>> 8);
          L = value_3837 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x63: {
          int _address1858;
          int address_3838 = (PC + 2) & 0xFFFF;
          int operand_3840 = read(address_3838, 0);
          int operand_3842 = read((address_3838 + 1) & 0xFFFF, 0);
          _address1858 = (operand_3842 << 8) | operand_3840;
          int value_3843 = ((H << 8) | L);
          write(_address1858, (value_3843 & 0xFF));
          write((_address1858 + 1) & 0xFFFF, (value_3843 >>> 8));
          MEMPTR = ((_address1858 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x64: {
          int _F1860;
          int value1_3844 = A;
          int value2_3845 = 0;
          int subtemp_3847 = value2_3845 - value1_3844;
          int lookup_3848 = ((value2_3845 & 0x88) >> 3) | ((value1_3844 & 0x88) >> 2) | ((subtemp_3847 & 0x88) >> 1);
          value2_3845 = subtemp_3847 & 0xff;
          _F1860 = ((subtemp_3847 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3848 & 0x07)] | OVERFLOW_SUB[(lookup_3848 >> 4)] | (SZ53[value2_3845] | (value2_3845 == 0 ? 0x40 : 0));
          F = (_F1860 & 0xFF);
          A = value2_3845;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x65: {
          int _nextPC1862;
          state.setIff1(state.isIff2());
          int jumpAddress2_3850;
          int wordNumber1_3852 = read(SP, 0);
          int wordNumber_3853 = read((SP + 1) & 0xFFFF, 0);
          int value_3851 = ((wordNumber_3853 << 8) | wordNumber1_3852);
          int wordNumber_3854 = SP;
          SP = ((wordNumber_3854 + 2) & 0xFFFF);
          jumpAddress2_3850 = value_3851;
          _nextPC1862 = jumpAddress2_3850;
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
          int reg_A_3856 = A;
          int nibble1_3857 = (reg_A_3856 & 0x00F0) >> 4;
          int nibble2_3858 = reg_A_3856 & 0x000F;
          int temp_3859 = read(((H << 8) | L), 0);
          contend4x1(((H << 8) | L));
          int nibble3_3860 = (temp_3859 & 0x00F0) >> 4;
          int nibble4_3861 = temp_3859 & 0x000F;
          write(((H << 8) | L), ((nibble2_3858 << 4) | nibble3_3860));
          int value_3862 = ((nibble1_3857 << 4) | nibble4_3861);
          int value2_3864 = reg_A_3856;
          int value3_3865 = F & 1;
          _F1864 = value3_3865;
          value2_3864 = (value2_3864 & 0xf0) | (temp_3859 & 0x0f);
          _F1864 = (_F1864 & 1) | (SZ53P[value2_3864] | (value2_3864 == 0 ? 0x40 : 0));
          F = (_F1864 & 0xFF);
          A = value_3862;
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
          int port_3867 = (B << 8) | C;
          int value_3868 = io.in(port_3867);
          L = value_3868 & 0xFF;
          int value1_3869 = value_3868 & 0xFF;
          int value2_3870 = F;
          _F1866 = value2_3870;
          _F1866 = (_F1866 & 1) | (SZ53P[value1_3869] | (value1_3869 == 0 ? 0x40 : 0));
          F = (_F1866 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x69: {
          int read_3873 = ((B << 8) | C);
          io.out(read_3873, L);
          int read_3874 = ((B << 8) | C);
          MEMPTR = ((read_3874 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6A: {
          int _F1869;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3875 = ((H << 8) | L);
          int b_3876 = ((H << 8) | L);
          int result_3877 = (a_3875 + b_3876 + (F & 1));
          int value1_3878 = ((result_3877 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3879 = (((a_3875 & 0x8800 | (b_3876 & 0x8800) >> 1) | ((result_3877 & 0x18800) | ((result_3877 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3881 = value2_3879 & 0x33;
          i_3881 |= (i_3881 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3882 = (i_3881 << 11) & 0x1A800;
          int lookup_3883 = (value2_3879 << 8 & 0x8800) >> 11 | (value2_3879 << 9 & 0x8800) >> 10 | (result1_3882 & 0x8800) >> 9;
          _F1869 = ((result1_3882 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3883 >> 4)] | (result1_3882 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3883 & 0x07)] | (value1_3878 == 1 ? 0 : 0x40);
          F = (_F1869 & 0xFF);
          int value_3885 = (result_3877 & 0xffff);
          H = (value_3885 >>> 8);
          L = value_3885 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6B: {
          int _address1871;
          int address_3886 = (PC + 2) & 0xFFFF;
          int operand_3888 = read(address_3886, 0);
          int operand_3890 = read((address_3886 + 1) & 0xFFFF, 0);
          _address1871 = (operand_3890 << 8) | operand_3888;
          int wordNumber1_3891 = read(_address1871, 0);
          int wordNumber_3892 = read((_address1871 + 1) & 0xFFFF, 0);
          int value_3893 = ((wordNumber_3892 << 8) | wordNumber1_3891);
          H = (value_3893 >>> 8);
          L = value_3893 & 0xFF;
          MEMPTR = ((_address1871 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x6C: {
          int _F1873;
          int value1_3894 = A;
          int value2_3895 = 0;
          int subtemp_3897 = value2_3895 - value1_3894;
          int lookup_3898 = ((value2_3895 & 0x88) >> 3) | ((value1_3894 & 0x88) >> 2) | ((subtemp_3897 & 0x88) >> 1);
          value2_3895 = subtemp_3897 & 0xff;
          _F1873 = ((subtemp_3897 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3898 & 0x07)] | OVERFLOW_SUB[(lookup_3898 >> 4)] | (SZ53[value2_3895] | (value2_3895 == 0 ? 0x40 : 0));
          F = (_F1873 & 0xFF);
          A = value2_3895;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x6D: {
          int _nextPC1875;
          state.setIff1(state.isIff2());
          int jumpAddress2_3900;
          int wordNumber1_3902 = read(SP, 0);
          int wordNumber_3903 = read((SP + 1) & 0xFFFF, 0);
          int value_3901 = ((wordNumber_3903 << 8) | wordNumber1_3902);
          int wordNumber_3904 = SP;
          SP = ((wordNumber_3904 + 2) & 0xFFFF);
          jumpAddress2_3900 = value_3901;
          _nextPC1875 = jumpAddress2_3900;
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
          int reg_A_3906 = A;
          int nibble1_3907 = (reg_A_3906 & 0x00F0) >> 4;
          int nibble2_3908 = reg_A_3906 & 0x000F;
          int temp_3909 = read(((H << 8) | L), 0);
          contend4x1(((H << 8) | L));
          int nibble3_3910 = (temp_3909 & 0x00F0) >> 4;
          int nibble4_3911 = temp_3909 & 0x000F;
          write(((H << 8) | L), ((nibble4_3911 << 4) | nibble2_3908));
          int value_3912 = ((nibble1_3907 << 4) | nibble3_3910);
          int value2_3914 = reg_A_3906;
          int value3_3915 = F & 1;
          _F1877 = value3_3915;
          value2_3914 = (value2_3914 & 0xf0) | (temp_3909 >> 4);
          _F1877 = (_F1877 & 1) | (SZ53P[value2_3914] | (value2_3914 == 0 ? 0x40 : 0));
          F = (_F1877 & 0xFF);
          A = value_3912;
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
          int port_3917 = (B << 8) | C;
          int value_3918 = io.in(port_3917);
          int value1_3919 = value_3918 & 0xFF;
          int value2_3920 = F;
          _F1879 = value2_3920;
          _F1879 = (_F1879 & 1) | (SZ53P[value1_3919] | (value1_3919 == 0 ? 0x40 : 0));
          F = (_F1879 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x71: {
          int read_3923 = ((B << 8) | C);
          io.out(read_3923, 0);
          int read_3924 = ((B << 8) | C);
          MEMPTR = ((read_3924 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _F1882;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int a_3925 = ((H << 8) | L);
          int result_3926 = (a_3925 - SP - (F & 1));
          int value1_3927 = ((result_3926 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3928 = (((a_3925 & 0x8800 | (SP & 0x8800) >> 1) | ((result_3926 & 0x18800) | ((result_3926 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3930 = value2_3928 & 0x33;
          i_3930 |= i_3930 << 1 & 0x04;
          int result1_3931 = i_3930 << 11 & 0x1A800;
          int lookup_3932 = (value2_3928 << 8 & 0x8800) >> 11 | (value2_3928 << 9 & 0x8800) >> 10 | (result1_3931 & 0x8800) >> 9;
          _F1882 = ((result1_3931 & 0x10000) != 0 ? 1 : 0) | 2 | OVERFLOW_SUB[(lookup_3932 >> 4)] | (result1_3931 >> 8 & 0xA8) | HALF_CARRY_SUB[(lookup_3932 & 0x07)] | (value1_3927 != 0 ? 0 : 0x40);
          F = (_F1882 & 0xFF);
          int value_3934 = (result_3926 & 0xffff);
          H = (value_3934 >>> 8);
          L = value_3934 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address1884;
          int address_3935 = (PC + 2) & 0xFFFF;
          int operand_3937 = read(address_3935, 0);
          int operand_3939 = read((address_3935 + 1) & 0xFFFF, 0);
          _address1884 = (operand_3939 << 8) | operand_3937;
          write(_address1884, (SP & 0xFF));
          write((_address1884 + 1) & 0xFFFF, (SP >>> 8));
          MEMPTR = ((_address1884 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _F1886;
          int value1_3940 = A;
          int value2_3941 = 0;
          int subtemp_3943 = value2_3941 - value1_3940;
          int lookup_3944 = ((value2_3941 & 0x88) >> 3) | ((value1_3940 & 0x88) >> 2) | ((subtemp_3943 & 0x88) >> 1);
          value2_3941 = subtemp_3943 & 0xff;
          _F1886 = ((subtemp_3943 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3944 & 0x07)] | OVERFLOW_SUB[(lookup_3944 >> 4)] | (SZ53[value2_3941] | (value2_3941 == 0 ? 0x40 : 0));
          F = (_F1886 & 0xFF);
          A = value2_3941;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _nextPC1888;
          state.setIff1(state.isIff2());
          int jumpAddress2_3946;
          int wordNumber1_3948 = read(SP, 0);
          int wordNumber_3949 = read((SP + 1) & 0xFFFF, 0);
          int value_3947 = ((wordNumber_3949 << 8) | wordNumber1_3948);
          int wordNumber_3950 = SP;
          SP = ((wordNumber_3950 + 2) & 0xFFFF);
          jumpAddress2_3946 = value_3947;
          _nextPC1888 = jumpAddress2_3946;
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
          int port_3952 = (B << 8) | C;
          int value_3953 = io.in(port_3952);
          A = value_3953 & 0xFF;
          int value1_3954 = value_3953 & 0xFF;
          int value2_3955 = F;
          _F1891 = value2_3955;
          _F1891 = (_F1891 & 1) | (SZ53P[value1_3954] | (value1_3954 == 0 ? 0x40 : 0));
          F = (_F1891 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x79: {
          int read_3958 = ((B << 8) | C);
          io.out(read_3958, A);
          int read_3959 = ((B << 8) | C);
          MEMPTR = ((read_3959 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7A: {
          int _F1894;
          contend7x1(((I << 8) | R));
          MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
          int b_3960 = ((H << 8) | L);
          int result_3961 = (SP + b_3960 + (F & 1));
          int value1_3962 = ((result_3961 & 0xFFFF) != 0 ? 1 : 0);
          int value2_3963 = (((SP & 0x8800 | (b_3960 & 0x8800) >> 1) | ((result_3961 & 0x18800) | ((result_3961 & 0x2000) >> 1)) >> 3) >> 8);
          int i_3965 = value2_3963 & 0x33;
          i_3965 |= (i_3965 & 0x02) != 0 ? 0x04 : 0x00;
          int result1_3966 = (i_3965 << 11) & 0x1A800;
          int lookup_3967 = (value2_3963 << 8 & 0x8800) >> 11 | (value2_3963 << 9 & 0x8800) >> 10 | (result1_3966 & 0x8800) >> 9;
          _F1894 = ((result1_3966 & 0x10000) != 0 ? 1 : 0) | OVERFLOW_ADD[(lookup_3967 >> 4)] | (result1_3966 >> 8 & 0xA8) | HALF_CARRY_ADD[(lookup_3967 & 0x07)] | (value1_3962 == 1 ? 0 : 0x40);
          F = (_F1894 & 0xFF);
          int value_3969 = (result_3961 & 0xffff);
          H = (value_3969 >>> 8);
          L = value_3969 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7B: {
          int _address1896;
          int address_3970 = (PC + 2) & 0xFFFF;
          int operand_3972 = read(address_3970, 0);
          int operand_3974 = read((address_3970 + 1) & 0xFFFF, 0);
          _address1896 = (operand_3974 << 8) | operand_3972;
          int wordNumber1_3975 = read(_address1896, 0);
          int wordNumber_3976 = read((_address1896 + 1) & 0xFFFF, 0);
          SP = ((wordNumber_3976 << 8) | wordNumber1_3975);
          MEMPTR = ((_address1896 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x7C: {
          int _F1898;
          int value1_3977 = A;
          int value2_3978 = 0;
          int subtemp_3980 = value2_3978 - value1_3977;
          int lookup_3981 = ((value2_3978 & 0x88) >> 3) | ((value1_3977 & 0x88) >> 2) | ((subtemp_3980 & 0x88) >> 1);
          value2_3978 = subtemp_3980 & 0xff;
          _F1898 = ((subtemp_3980 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_3981 & 0x07)] | OVERFLOW_SUB[(lookup_3981 >> 4)] | (SZ53[value2_3978] | (value2_3978 == 0 ? 0x40 : 0));
          F = (_F1898 & 0xFF);
          A = value2_3978;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x7D: {
          int _nextPC1900;
          state.setIff1(state.isIff2());
          int jumpAddress2_3983;
          int wordNumber1_3985 = read(SP, 0);
          int wordNumber_3986 = read((SP + 1) & 0xFFFF, 0);
          int value_3984 = ((wordNumber_3986 << 8) | wordNumber1_3985);
          int wordNumber_3987 = SP;
          SP = ((wordNumber_3987 + 2) & 0xFFFF);
          jumpAddress2_3983 = value_3984;
          _nextPC1900 = jumpAddress2_3983;
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
          int read_3989 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_3989);
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
          int byteTemp_3990 = read_3989 + A;
          int value1_3991 = F;
          int value2_3992 = byteTemp_3990 & 0xFF;
          int value3_3993 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1935 = value1_3991;
          _F1935 = (_F1935 & 0xC1) | (value3_3993 != 0 ? 4 : 0) | (value2_3992 & 8) | ((value2_3992 & 0x02) != 0 ? 0x20 : 0);
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
          int value_3997 = read(((H << 8) | L), 0);
          int carry_3999 = F & 1;
          int value3_4002 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1937 = 0;
          int bytetemp_4003 = A - value_3997;
          int lookup_4004 = ((A & 0x08) >> 3) | ((value_3997 & 0x08) >> 2) | ((bytetemp_4003 & 0x08) >> 1);
          _F1937 = (_F1937 & 1) | (value3_4002 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4004] | (bytetemp_4003 != 0 ? 0 : 0x40) | (bytetemp_4003 & 0x80);
          if ((_F1937 & 0x10) != 0) {
              bytetemp_4003--;
          }
          _F1937 |= (bytetemp_4003 & 8) | ((bytetemp_4003 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1937 & 0xFF);
          F = ((F & -2) | carry_3999);
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
          int port_4006 = ((B << 8) | C);
          int in_4007 = io.in(port_4006);
          int hlValue_4009 = ((H << 8) | L);
          write(hlValue_4009, in_4007);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4015 = in_4007 & 0xff;
          int initemp2_4016 = (initemp_4015 + C + 1) & 0xff;
          _F1939 = ((initemp_4015 & 0x80) != 0 ? 2 : 0) | ((initemp2_4016 < initemp_4015) ? 0x11 : 0) | (PARITY[((initemp2_4016 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1939 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F1941 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4021 = ((H << 8) | L);
          int valueFromHL_4023 = read(hlValue_4021, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4023);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          int outitemp2_4030 = (valueFromHL_4023 + L) & 0xff;
          _F1941 = ((valueFromHL_4023 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4030 < valueFromHL_4023) ? 0x11 : 0) | (PARITY[((outitemp2_4030 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int read_4032 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4032);
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
          int byteTemp_4033 = read_4032 + A;
          int value1_4034 = F;
          int value2_4035 = byteTemp_4033 & 0xFF;
          int value3_4036 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1947 = value1_4034;
          _F1947 = (_F1947 & 0xC1) | (value3_4036 != 0 ? 4 : 0) | (value2_4035 & 8) | ((value2_4035 & 0x02) != 0 ? 0x20 : 0);
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
          int lastCarry_4040 = F & 1;
          int value1_4041 = read(((H << 8) | L), 0);
          int value3_4043 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1949 = A;
          int bytetemp_4044 = A - value1_4041;
          int lookup_4045 = ((A & 0x08) >> 3) | ((value1_4041 & 0x08) >> 2) | ((bytetemp_4044 & 0x08) >> 1);
          _F1949 = (_F1949 & 1) | (value3_4043 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4045] | (bytetemp_4044 != 0 ? 0 : 0x40) | (bytetemp_4044 & 0x80);
          if ((_F1949 & 0x10) != 0) {
              bytetemp_4044--;
          }
          _F1949 |= (bytetemp_4044 & 8) | ((bytetemp_4044 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1949 & 0xFF);
          F = (F | lastCarry_4040);
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
          int port_4047 = ((B << 8) | C);
          int in_4048 = io.in(port_4047);
          int hlValue_4049 = ((H << 8) | L);
          write(hlValue_4049, in_4048);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4055 = in_4048 & 0xff;
          int initemp2_4056 = (initemp_4055 + C + -1) & 0xff;
          _F1951 = ((initemp_4055 & 0x80) != 0 ? 2 : 0) | ((initemp2_4056 < initemp_4055) ? 0x11 : 0) | (PARITY[((initemp2_4056 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
          F = (_F1951 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F1953 = 0;
          contend1x1(((I << 8) | R));
          int hlValue_4061 = ((H << 8) | L);
          int valueFromHL_4063 = read(hlValue_4061, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4063);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          int outitemp2_4070 = (valueFromHL_4063 + L) & 0xff;
          _F1953 = ((valueFromHL_4063 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4070 < valueFromHL_4063) ? 0x11 : 0) | (PARITY[((outitemp2_4070 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int read_4072 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4072);
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
          int byteTemp_4073 = read_4072 + A;
          int value1_4074 = F;
          int value2_4075 = byteTemp_4073 & 0xFF;
          int value3_4076 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1959 = value1_4074;
          _F1959 = (_F1959 & 0xC1) | (value3_4076 != 0 ? 4 : 0) | (value2_4075 & 8) | ((value2_4075 & 0x02) != 0 ? 0x20 : 0);
          F = _F1959;
          if ((((B << 8) | C) != 0)) {
              int nextPC_4080 = PC;
              int newValue_4081;
              if (nextPC_4080 != -1) {
                  newValue_4081 = (nextPC_4080 + 1) & 0xFFFF;
              } else {
                  newValue_4081 = MEMPTR;
              }
              MEMPTR = newValue_4081;
              contend2x1((((D << 8) | E) - 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((D << 8) | E) - 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4081;
              newValue_4081 = MEMPTR;
              MEMPTR = newValue_4081;
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
          int value_4083 = read(((H << 8) | L), 0);
          int carry_4085 = F & 1;
          int value3_4088 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1961 = 0;
          int bytetemp_4089 = A - value_4083;
          int lookup_4090 = ((A & 0x08) >> 3) | ((value_4083 & 0x08) >> 2) | ((bytetemp_4089 & 0x08) >> 1);
          _F1961 = (_F1961 & 1) | (value3_4088 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4090] | (bytetemp_4089 != 0 ? 0 : 0x40) | (bytetemp_4089 & 0x80);
          if ((_F1961 & 0x10) != 0) {
              bytetemp_4089--;
          }
          _F1961 |= (bytetemp_4089 & 8) | ((bytetemp_4089 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1961 & 0xFF);
          F = ((F & -2) | carry_4085);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          if (((F & 0x40) == 0 && ((B << 8) | C) != 0)) {
              int nextPC_4092 = PC;
              int newValue_4093;
              if (nextPC_4092 != -1) {
                  newValue_4093 = (nextPC_4092 + 1) & 0xFFFF;
              } else {
                  newValue_4093 = (MEMPTR + 1) & 0xFFFF;
              }
              MEMPTR = newValue_4093;
              contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4093;
              newValue_4093 = (MEMPTR + 1) & 0xFFFF;
              MEMPTR = newValue_4093;
              contend5x1((((H << 8) | L) - 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xB2: {
          int _F1963 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + 1) & 0xFFFF);
          int port_4095 = ((B << 8) | C);
          int in_4096 = io.in(port_4095);
          int hlValue_4098 = ((H << 8) | L);
          write(hlValue_4098, in_4096);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4104 = in_4096 & 0xff;
          int initemp2_4105 = (initemp_4104 + C + 1) & 0xff;
          _F1963 = ((initemp_4104 & 0x80) != 0 ? 2 : 0) | ((initemp2_4105 < initemp_4104) ? 0x11 : 0) | (PARITY[((initemp2_4105 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int hlValue_4110 = ((H << 8) | L);
          int valueFromHL_4112 = read(hlValue_4110, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4112);
          L = (L + 1) & 0xFF;
          if (L == 0) {
              H = (H + 1) & 0xFF;
          }
          int outitemp2_4119 = (valueFromHL_4112 + L) & 0xff;
          _F1965 = ((valueFromHL_4112 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4119 < valueFromHL_4112) ? 0x11 : 0) | (PARITY[((outitemp2_4119 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int read_4121 = read(((H << 8) | L), 0);
          write(((D << 8) | E), read_4121);
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
          int byteTemp_4122 = read_4121 + A;
          int value1_4123 = F;
          int value2_4124 = byteTemp_4122 & 0xFF;
          int value3_4125 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1971 = value1_4123;
          _F1971 = (_F1971 & 0xC1) | (value3_4125 != 0 ? 4 : 0) | (value2_4124 & 8) | ((value2_4124 & 0x02) != 0 ? 0x20 : 0);
          F = _F1971;
          if ((((B << 8) | C) != 0)) {
              int nextPC_4129 = PC;
              int newValue_4130;
              if (nextPC_4129 != -1) {
                  newValue_4130 = (nextPC_4129 + 1) & 0xFFFF;
              } else {
                  newValue_4130 = MEMPTR;
              }
              MEMPTR = newValue_4130;
              contend2x1((((D << 8) | E) + 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((D << 8) | E) + 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4130;
              newValue_4130 = MEMPTR;
              MEMPTR = newValue_4130;
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
          int lastCarry_4132 = F & 1;
          int value1_4133 = read(((H << 8) | L), 0);
          int value3_4135 = (((B << 8) | C) != 0 ? 1 : 0);
          _F1973 = A;
          int bytetemp_4136 = A - value1_4133;
          int lookup_4137 = ((A & 0x08) >> 3) | ((value1_4133 & 0x08) >> 2) | ((bytetemp_4136 & 0x08) >> 1);
          _F1973 = (_F1973 & 1) | (value3_4135 != 0 ? 6 : 2) | HALF_CARRY_SUB[lookup_4137] | (bytetemp_4136 != 0 ? 0 : 0x40) | (bytetemp_4136 & 0x80);
          if ((_F1973 & 0x10) != 0) {
              bytetemp_4136--;
          }
          _F1973 |= (bytetemp_4136 & 8) | ((bytetemp_4136 & 0x02) != 0 ? 0x20 : 0);
          F = (_F1973 & 0xFF);
          F = (F | lastCarry_4132);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          if (((F & 0x40) == 0 && ((B << 8) | C) != 0)) {
              int nextPC_4139 = PC;
              int newValue_4140;
              if (nextPC_4139 != -1) {
                  newValue_4140 = (nextPC_4139 + 1) & 0xFFFF;
              } else {
                  newValue_4140 = (MEMPTR + -1) & 0xFFFF;
              }
              MEMPTR = newValue_4140;
              contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              if (PC != -1)
                  contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              PC = PC == -1 ? (PC + 2) & 0xFFFF : PC;
              break;
          } else {
              int newValue_4140;
              newValue_4140 = (MEMPTR + -1) & 0xFFFF;
              MEMPTR = newValue_4140;
              contend5x1((((H << 8) | L) + 1) & 0xFFFF);
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xBA: {
          int _F1975 = 0;
          contend1x1(((I << 8) | R));
          MEMPTR = ((((B << 8) | C) + -1) & 0xFFFF);
          int port_4142 = ((B << 8) | C);
          int in_4143 = io.in(port_4142);
          int hlValue_4144 = ((H << 8) | L);
          write(hlValue_4144, in_4143);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          B = (B - 1) & 0xFF;
          int initemp_4150 = in_4143 & 0xff;
          int initemp2_4151 = (initemp_4150 + C + -1) & 0xff;
          _F1975 = ((initemp_4150 & 0x80) != 0 ? 2 : 0) | ((initemp2_4151 < initemp_4150) ? 0x11 : 0) | (PARITY[((initemp2_4151 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int hlValue_4156 = ((H << 8) | L);
          int valueFromHL_4158 = read(hlValue_4156, 0);
          B = (B - 1) & 0xFF;
          io.out(((B << 8) | C), valueFromHL_4158);
          L = (L - 1) & 0xFF;
          if (L == 0xFF) {
              H = (H - 1) & 0xFF;
          }
          int outitemp2_4165 = (valueFromHL_4158 + L) & 0xff;
          _F1977 = ((valueFromHL_4158 & 0x80) != 0 ? 2 : 0) | ((outitemp2_4165 < valueFromHL_4158) ? 0x11 : 0) | (PARITY[((outitemp2_4165 & 0x07) ^ B)] != 0 ? 4 : 0) | (SZ53[B] | (B == 0 ? 0x40 : 0));
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
          int address_4235 = (PC + 2) & 0xFFFF;
          int operand_4237 = read(address_4235, 0);
          int operand_4239 = read((address_4235 + 1) & 0xFFFF, 0);
          int value_4240 = ((operand_4239 << 8) | operand_4237);
          B = (value_4240 >>> 8);
          C = value_4240 & 0xFF;
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
          int read_4241 = ((B << 8) | C);
          int value_4242 = (read_4241 + 1) & 0xFFFF;
          B = (value_4242 >>> 8);
          C = value_4242 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x04: {
          int _F2069;
          int value1_4243 = B;
          int value2_4244 = F;
          _F2069 = value2_4244;
          value1_4243++;
          value1_4243 &= 0xff;
          _F2069 = (_F2069 & 1) | (value1_4243 == 0x80 ? 4 : 0) | ((value1_4243 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4243 & 0xff] | (value1_4243 == 0 ? 0x40 : 0));
          int result_4246 = value1_4243 & 0xFF;
          F = (_F2069 & 0xFF);
          B = result_4246;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x05: {
          int _F2071;
          int value1_4247 = B;
          int value2_4248 = F;
          _F2071 = value2_4248;
          _F2071 = (_F2071 & 1) | ((value1_4247 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4247--;
          value1_4247 &= 0xff;
          _F2071 |= (value1_4247 == 0x7f ? 4 : 0) | (SZ53[value1_4247 & 0xff] | (value1_4247 == 0 ? 0x40 : 0));
          int result_4250 = value1_4247 & 0xFF;
          F = (_F2071 & 0xFF);
          B = result_4250;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x06: {
          int operand_4251 = read((PC + 2) & 0xFFFF, 0);
          B = operand_4251;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x07: {
          int _F2074;
          int value1_4252 = A;
          int value2_4253 = F;
          _F2074 = value2_4253;
          value1_4252 = (value1_4252 << 1) | (value1_4252 >> 7);
          _F2074 = (_F2074 & 0xC4) | (value1_4252 & 0x29);
          int result_4255 = value1_4252 & 0xFF;
          F = _F2074;
          A = result_4255;
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
          int v1_4256 = ((A << 8) | F);
          int v2_4257 = _AF;
          A = (v2_4257 >>> 8);
          F = v2_4257 & 0xFF;
          _AF = v1_4256;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x09: {
          int _F2077;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int b_4258 = ((B << 8) | C);
          int result_4259 = (IY + b_4258);
          int value1_4260 = ((IY & 0x0800) >> 4 | result_4259 >> 11);
          int value2_4261 = F;
          int value3_4262 = (b_4258 >> 11) & 1;
          _F2077 = value2_4261;
          int add16temp_4263 = value1_4260 << 11;
          int lookup_4264 = (((value1_4260 << 4) & 0x0800) >> 11) | ((value3_4262 << 11) >> 10) | ((add16temp_4263 & 0x0800) >> 9);
          _F2077 = (_F2077 & 0xC4) | ((add16temp_4263 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4263 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4264];
          F = (_F2077 & 0xFF);
          IY = (result_4259 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0A: {
          int _address2079 = (B << 8) | C;
          int value_4266 = read(_address2079, 0);
          A = value_4266;
          MEMPTR = ((_address2079 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0B: {
          int value_4267 = (((B << 8) | C) - 1) & 0xFFFF;
          B = (value_4267 >>> 8);
          C = value_4267 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0C: {
          int _F2082;
          int value1_4268 = C;
          int value2_4269 = F;
          _F2082 = value2_4269;
          value1_4268++;
          value1_4268 &= 0xff;
          _F2082 = (_F2082 & 1) | (value1_4268 == 0x80 ? 4 : 0) | ((value1_4268 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4268 & 0xff] | (value1_4268 == 0 ? 0x40 : 0));
          int result_4271 = value1_4268 & 0xFF;
          F = (_F2082 & 0xFF);
          C = result_4271;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0D: {
          int _F2084;
          int value1_4272 = C;
          int value2_4273 = F;
          _F2084 = value2_4273;
          _F2084 = (_F2084 & 1) | ((value1_4272 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4272--;
          value1_4272 &= 0xff;
          _F2084 |= (value1_4272 == 0x7f ? 4 : 0) | (SZ53[value1_4272 & 0xff] | (value1_4272 == 0 ? 0x40 : 0));
          int result_4275 = value1_4272 & 0xFF;
          F = (_F2084 & 0xFF);
          C = result_4275;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x0E: {
          int operand_4276 = read((PC + 2) & 0xFFFF, 0);
          C = operand_4276;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x0F: {
          int _F2087;
          int value1_4277 = A;
          int value2_4278 = F;
          _F2087 = value2_4278;
          _F2087 = (_F2087 & 0xC4) | (value1_4277 & 1);
          value1_4277 = (value1_4277 >> 1) | (value1_4277 << 7);
          _F2087 |= (value1_4277 & 0x28);
          int result_4280 = (value1_4277 & 0xff);
          F = (_F2087 & 0xFF);
          A = result_4280;
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
              int operand_4282 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4281 = ((PC + 3 + (byte) operand_4282) & 0xFFFF);
              MEMPTR = jumpAddress2_4281;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4281;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x11: {
          int address_4284 = (PC + 2) & 0xFFFF;
          int operand_4286 = read(address_4284, 0);
          int operand_4288 = read((address_4284 + 1) & 0xFFFF, 0);
          int value_4289 = ((operand_4288 << 8) | operand_4286);
          D = (value_4289 >>> 8);
          E = value_4289 & 0xFF;
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
          int read_4290 = ((D << 8) | E);
          int value_4291 = (read_4290 + 1) & 0xFFFF;
          D = (value_4291 >>> 8);
          E = value_4291 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x14: {
          int _F2094;
          int value1_4292 = D;
          int value2_4293 = F;
          _F2094 = value2_4293;
          value1_4292++;
          value1_4292 &= 0xff;
          _F2094 = (_F2094 & 1) | (value1_4292 == 0x80 ? 4 : 0) | ((value1_4292 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4292 & 0xff] | (value1_4292 == 0 ? 0x40 : 0));
          int result_4295 = value1_4292 & 0xFF;
          F = (_F2094 & 0xFF);
          D = result_4295;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x15: {
          int _F2096;
          int value1_4296 = D;
          int value2_4297 = F;
          _F2096 = value2_4297;
          _F2096 = (_F2096 & 1) | ((value1_4296 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4296--;
          value1_4296 &= 0xff;
          _F2096 |= (value1_4296 == 0x7f ? 4 : 0) | (SZ53[value1_4296 & 0xff] | (value1_4296 == 0 ? 0x40 : 0));
          int result_4299 = value1_4296 & 0xFF;
          F = (_F2096 & 0xFF);
          D = result_4299;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x16: {
          int operand_4300 = read((PC + 2) & 0xFFFF, 0);
          D = operand_4300;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x17: {
          int _F2099;
          int value1_4301 = A;
          int value2_4302 = F;
          _F2099 = value2_4302;
          int bytetemp_4304 = value1_4301;
          value1_4301 = (value1_4301 << 1) | (_F2099 & 1);
          _F2099 = (_F2099 & 0xC4) | (value1_4301 & 0x28) | (bytetemp_4304 >> 7);
          int result_4305 = value1_4301 & 0xFF;
          F = (_F2099 & 0xFF);
          A = result_4305;
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
          int operand_4307 = read((PC + 2) & 0xFFFF, 0);
          int jumpAddress2_4306 = ((PC + 3 + (byte) operand_4307) & 0xFFFF);
          _nextPC2101 = jumpAddress2_4306;
          MEMPTR = _nextPC2101;
          contend5x1((PC + 1) & 0xFFFF);
          PC = _nextPC2101;
          break;
      }
      case 0x19: {
          int _F2102;
          contend7x1(((I << 8) | R));
          MEMPTR = ((IY + 1) & 0xFFFF);
          int b_4309 = ((D << 8) | E);
          int result_4310 = (IY + b_4309);
          int value1_4311 = ((IY & 0x0800) >> 4 | result_4310 >> 11);
          int value2_4312 = F;
          int value3_4313 = (b_4309 >> 11) & 1;
          _F2102 = value2_4312;
          int add16temp_4314 = value1_4311 << 11;
          int lookup_4315 = (((value1_4311 << 4) & 0x0800) >> 11) | ((value3_4313 << 11) >> 10) | ((add16temp_4314 & 0x0800) >> 9);
          _F2102 = (_F2102 & 0xC4) | ((add16temp_4314 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4314 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4315];
          F = (_F2102 & 0xFF);
          IY = (result_4310 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1A: {
          int _address2104 = (D << 8) | E;
          int value_4317 = read(_address2104, 0);
          A = value_4317;
          MEMPTR = ((_address2104 + 1) & 0xFFFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1B: {
          int value_4318 = (((D << 8) | E) - 1) & 0xFFFF;
          D = (value_4318 >>> 8);
          E = value_4318 & 0xFF;
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1C: {
          int _F2107;
          int value1_4319 = E;
          int value2_4320 = F;
          _F2107 = value2_4320;
          value1_4319++;
          value1_4319 &= 0xff;
          _F2107 = (_F2107 & 1) | (value1_4319 == 0x80 ? 4 : 0) | ((value1_4319 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4319 & 0xff] | (value1_4319 == 0 ? 0x40 : 0));
          int result_4322 = value1_4319 & 0xFF;
          F = (_F2107 & 0xFF);
          E = result_4322;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1D: {
          int _F2109;
          int value1_4323 = E;
          int value2_4324 = F;
          _F2109 = value2_4324;
          _F2109 = (_F2109 & 1) | ((value1_4323 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4323--;
          value1_4323 &= 0xff;
          _F2109 |= (value1_4323 == 0x7f ? 4 : 0) | (SZ53[value1_4323 & 0xff] | (value1_4323 == 0 ? 0x40 : 0));
          int result_4326 = value1_4323 & 0xFF;
          F = (_F2109 & 0xFF);
          E = result_4326;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x1E: {
          int operand_4327 = read((PC + 2) & 0xFFFF, 0);
          E = operand_4327;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x1F: {
          int _F2112;
          int value1_4328 = A;
          int value2_4329 = F;
          _F2112 = value2_4329;
          int A_4331 = value1_4328;
          int bytetemp_4332 = A_4331;
          A_4331 = (A_4331 >> 1) | (_F2112 << 7);
          _F2112 = (_F2112 & 0xC4) | (A_4331 & 0x28) | (bytetemp_4332 & 1);
          int result_4333 = A_4331 & 0xFF;
          F = _F2112;
          A = result_4333;
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
              int operand_4335 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4334 = ((PC + 3 + (byte) operand_4335) & 0xFFFF);
              MEMPTR = jumpAddress2_4334;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4334;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x21: {
          int address_4337 = (PC + 2) & 0xFFFF;
          int operand_4339 = read(address_4337, 0);
          int operand_4341 = read((address_4337 + 1) & 0xFFFF, 0);
          IY = ((operand_4341 << 8) | operand_4339);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x22: {
          int _address2116;
          int address_4342 = (PC + 2) & 0xFFFF;
          int operand_4344 = read(address_4342, 0);
          int operand_4346 = read((address_4342 + 1) & 0xFFFF, 0);
          _address2116 = (operand_4346 << 8) | operand_4344;
          write(_address2116, (IY & 0xFF));
          write((_address2116 + 1) & 0xFFFF, (IY >>> 8));
          MEMPTR = ((_address2116 + 1) & 0xFFFF);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x23: {
          int read_4347 = IY;
          IY = ((read_4347 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x24: {
          int _F2119;
          int value1_4348 = (IY >> 8);
          int value2_4349 = F;
          _F2119 = value2_4349;
          value1_4348++;
          value1_4348 &= 0xff;
          _F2119 = (_F2119 & 1) | (value1_4348 == 0x80 ? 4 : 0) | ((value1_4348 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4348 & 0xff] | (value1_4348 == 0 ? 0x40 : 0));
          int result_4351 = value1_4348 & 0xFF;
          F = (_F2119 & 0xFF);
          IY = (IY & 0x00FF) | (result_4351 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x25: {
          int _F2121;
          int value1_4352 = (IY >> 8);
          int value2_4353 = F;
          _F2121 = value2_4353;
          _F2121 = (_F2121 & 1) | ((value1_4352 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4352--;
          value1_4352 &= 0xff;
          _F2121 |= (value1_4352 == 0x7f ? 4 : 0) | (SZ53[value1_4352 & 0xff] | (value1_4352 == 0 ? 0x40 : 0));
          int result_4355 = value1_4352 & 0xFF;
          F = (_F2121 & 0xFF);
          IY = (IY & 0x00FF) | (result_4355 << 8);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x26: {
          int operand_4356 = read((PC + 2) & 0xFFFF, 0);
          IY = (IY & 0x00FF) | (operand_4356 << 8);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x27: {
          int _F2127 = 0;
          int _F2126 = 0;
          int _data2125 = 0;
          int _F2124;
          int value1_4357 = A;
          int value2_4358 = F;
          _F2124 = value2_4358;
          value1_4357 &= 0xff;
          int add_4360 = 0;
          int carry_4361 = (_F2124 & 1);
          if (((_F2124 & 0x10) != 0) || ((value1_4357 & 0x0f) > 9)) {
              add_4360 = 6;
          }
          if (carry_4361 != 0 || (value1_4357 > 0x99)) {
              add_4360 |= 0x60;
          }
          if (value1_4357 > 0x99) {
              carry_4361 = 1;
          }
          int and_4363 = _F2124 & 0xff;
          _data2125 = and_4363;
          if ((_F2124 & 2) != 0) {
              int value1_4364 = value1_4357;
              int subtemp_4367 = value1_4364 - add_4360;
              int lookup_4368 = ((value1_4364 & 0x88) >> 3) | ((add_4360 & 0x88) >> 2) | ((subtemp_4367 & 0x88) >> 1);
              value1_4364 = subtemp_4367 & 0xff;
              _F2126 = ((subtemp_4367 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4368 & 0x07)] | OVERFLOW_SUB[(lookup_4368 >> 4)] | (SZ53[value1_4364 & 0xff] | (value1_4364 == 0 ? 0x40 : 0));
              int result_4369 = value1_4364 & 0xFF;
              int and_4370 = (_F2126 & 0xFF);
              _data2125 = and_4370;
              value1_4357 = result_4369;
          } else {
              int value2_4372 = value1_4357;
              int addtemp_4374 = value2_4372 + add_4360;
              int lookup_4375 = ((value2_4372 & 0x88) >> 3) | ((add_4360 & 0x88) >> 2) | ((addtemp_4374 & 0x88) >> 1);
              value2_4372 = addtemp_4374 & 0xff;
              _F2127 = ((addtemp_4374 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4375 & 0x07)] | OVERFLOW_ADD[(lookup_4375 >> 4)] | (SZ53[value2_4372 & 0xff] | (value2_4372 == 0 ? 0x40 : 0));
              int result_4376 = value2_4372 & 0xFF;
              int and_4377 = (_F2127 & 0xFF);
              _data2125 = and_4377;
              value1_4357 = result_4376;
          }
          _F2124 = _data2125;
          _F2124 = (_F2124 & -6) | carry_4361 | PARITY[value1_4357 & 0xff];
          int result_4378 = value1_4357 & 0xFF;
          F = (_F2124 & 0xFF);
          A = result_4378;
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
              int operand_4380 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4379 = ((PC + 3 + (byte) operand_4380) & 0xFFFF);
              MEMPTR = jumpAddress2_4379;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4379;
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
          int result_4382 = (IY + IY);
          int value1_4383 = ((IY & 0x0800) >> 4 | result_4382 >> 11);
          int value2_4384 = F;
          int value3_4385 = (IY >> 11) & 1;
          _F2130 = value2_4384;
          int add16temp_4386 = value1_4383 << 11;
          int lookup_4387 = (((value1_4383 << 4) & 0x0800) >> 11) | ((value3_4385 << 11) >> 10) | ((add16temp_4386 & 0x0800) >> 9);
          _F2130 = (_F2130 & 0xC4) | ((add16temp_4386 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4386 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4387];
          F = (_F2130 & 0xFF);
          IY = (result_4382 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2A: {
          int _address2132;
          int address_4389 = (PC + 2) & 0xFFFF;
          int operand_4391 = read(address_4389, 0);
          int operand_4393 = read((address_4389 + 1) & 0xFFFF, 0);
          _address2132 = (operand_4393 << 8) | operand_4391;
          int wordNumber1_4394 = read(_address2132, 0);
          int wordNumber_4395 = read((_address2132 + 1) & 0xFFFF, 0);
          IY = ((wordNumber_4395 << 8) | wordNumber1_4394);
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
          int value1_4396 = (IY & 0xFF);
          int value2_4397 = F;
          _F2135 = value2_4397;
          value1_4396++;
          value1_4396 &= 0xff;
          _F2135 = (_F2135 & 1) | (value1_4396 == 0x80 ? 4 : 0) | ((value1_4396 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4396 & 0xff] | (value1_4396 == 0 ? 0x40 : 0));
          int result_4399 = value1_4396 & 0xFF;
          F = (_F2135 & 0xFF);
          IY = (IY & 0xFF00) | result_4399;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2D: {
          int _F2137;
          int value1_4400 = (IY & 0xFF);
          int value2_4401 = F;
          _F2137 = value2_4401;
          _F2137 = (_F2137 & 1) | ((value1_4400 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4400--;
          value1_4400 &= 0xff;
          _F2137 |= (value1_4400 == 0x7f ? 4 : 0) | (SZ53[value1_4400 & 0xff] | (value1_4400 == 0 ? 0x40 : 0));
          int result_4403 = value1_4400 & 0xFF;
          F = (_F2137 & 0xFF);
          IY = (IY & 0xFF00) | result_4403;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x2E: {
          int operand_4404 = read((PC + 2) & 0xFFFF, 0);
          IY = (IY & 0xFF00) | operand_4404;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x2F: {
          int _F2140;
          int value1_4405 = A;
          int value2_4406 = F;
          _F2140 = value2_4406;
          value1_4405 ^= 0xff;
          _F2140 = (_F2140 & 0xC5) | (value1_4405 & 0x28) | 0x12;
          int result_4408 = value1_4405 & 0xFF;
          F = _F2140;
          A = result_4408;
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
              int operand_4410 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4409 = ((PC + 3 + (byte) operand_4410) & 0xFFFF);
              MEMPTR = jumpAddress2_4409;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4409;
              break;
          } else {
              MEMPTR = 0;
              contend1x3((PC + 1) & 0xFFFF);
              PC = (PC + 3) & 0xFFFF;
              break;
          }
      }
      case 0x31: {
          int address_4412 = (PC + 2) & 0xFFFF;
          int operand_4414 = read(address_4412, 0);
          int operand_4416 = read((address_4412 + 1) & 0xFFFF, 0);
          SP = ((operand_4416 << 8) | operand_4414);
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x32: {
          int _address2144;
          int address_4417 = (PC + 2) & 0xFFFF;
          int operand_4419 = read(address_4417, 0);
          int operand_4421 = read((address_4417 + 1) & 0xFFFF, 0);
          _address2144 = (operand_4421 << 8) | operand_4419;
          write(_address2144, A);
          MEMPTR = ((A << 8) | ((_address2144 + 1) & 0xff));
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x33: {
          int read_4422 = SP;
          SP = ((read_4422 + 1) & 0xFFFF);
          contend2x1(((I << 8) | R));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x34: {
          int _F2148;
          int _value2147;
          int _address2147;
          int operand_4423 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4423)) & 0xFFFF;
          int operand_4424 = read(_address2147, 0);
          contend1x1(_address2147);
          _value2147 = operand_4424;
          int value1_4425 = _value2147;
          int value2_4426 = F;
          _F2148 = value2_4426;
          value1_4425++;
          value1_4425 &= 0xff;
          _F2148 = (_F2148 & 1) | (value1_4425 == 0x80 ? 4 : 0) | ((value1_4425 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4425 & 0xff] | (value1_4425 == 0 ? 0x40 : 0));
          int result_4428 = value1_4425 & 0xFF;
          F = (_F2148 & 0xFF);
          _address2147 = (IY + (int) ((byte) operand_4423)) & 0xFFFF;
          _value2147 = result_4428;
          write(_address2147, result_4428);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x35: {
          int _F2150;
          int _value2147;
          int _address2147;
          int operand_4429 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4429)) & 0xFFFF;
          int operand_4430 = read(_address2147, 0);
          contend1x1(_address2147);
          _value2147 = operand_4430;
          int value1_4431 = _value2147;
          int value2_4432 = F;
          _F2150 = value2_4432;
          _F2150 = (_F2150 & 1) | ((value1_4431 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4431--;
          value1_4431 &= 0xff;
          _F2150 |= (value1_4431 == 0x7f ? 4 : 0) | (SZ53[value1_4431 & 0xff] | (value1_4431 == 0 ? 0x40 : 0));
          int result_4434 = value1_4431 & 0xFF;
          F = (_F2150 & 0xFF);
          _address2147 = (IY + (int) ((byte) operand_4429)) & 0xFFFF;
          _value2147 = result_4434;
          write(_address2147, result_4434);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x36: {
          int _address2152;
          int operand_4435 = read((PC + 2) & 0xFFFF, 0);
          int operand_4436 = read((PC + 3) & 0xFFFF, 0);
          _address2152 = (IY + (int) ((byte) operand_4435)) & 0xFFFF;
          contend2x1((PC + 3) & 0xFFFF);
          write(_address2152, operand_4436);
          MEMPTR = _address2152;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x37: {
          int _F2154;
          int value2_4438 = F;
          _F2154 = value2_4438;
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
              int operand_4442 = read((PC + 2) & 0xFFFF, 0);
              int jumpAddress2_4441 = ((PC + 3 + (byte) operand_4442) & 0xFFFF);
              MEMPTR = jumpAddress2_4441;
              contend5x1((PC + 1) & 0xFFFF);
              PC = jumpAddress2_4441;
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
          int result_4444 = (IY + SP);
          int value1_4445 = ((IY & 0x0800) >> 4 | result_4444 >> 11);
          int value2_4446 = F;
          int value3_4447 = (SP >> 11) & 1;
          _F2157 = value2_4446;
          int add16temp_4448 = value1_4445 << 11;
          int lookup_4449 = (((value1_4445 << 4) & 0x0800) >> 11) | ((value3_4447 << 11) >> 10) | ((add16temp_4448 & 0x0800) >> 9);
          _F2157 = (_F2157 & 0xC4) | ((add16temp_4448 & 0x10000) != 0 ? 1 : 0) | ((add16temp_4448 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_4449];
          F = (_F2157 & 0xFF);
          IY = (result_4444 & 0xffff);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3A: {
          int _address2159;
          int address_4451 = (PC + 2) & 0xFFFF;
          int operand_4453 = read(address_4451, 0);
          int operand_4455 = read((address_4451 + 1) & 0xFFFF, 0);
          _address2159 = (operand_4455 << 8) | operand_4453;
          int value_4456 = read(_address2159, 0);
          A = value_4456;
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
          int value1_4457 = A;
          int value2_4458 = F;
          _F2162 = value2_4458;
          value1_4457++;
          value1_4457 &= 0xff;
          _F2162 = (_F2162 & 1) | (value1_4457 == 0x80 ? 4 : 0) | ((value1_4457 & 0x0f) != 0 ? 0 : 0x10) | (SZ53[value1_4457 & 0xff] | (value1_4457 == 0 ? 0x40 : 0));
          int result_4460 = value1_4457 & 0xFF;
          F = (_F2162 & 0xFF);
          A = result_4460;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3D: {
          int _F2164;
          int value1_4461 = A;
          int value2_4462 = F;
          _F2164 = value2_4462;
          _F2164 = (_F2164 & 1) | ((value1_4461 & 0x0f) != 0 ? 0 : 0x10) | 2;
          value1_4461--;
          value1_4461 &= 0xff;
          _F2164 |= (value1_4461 == 0x7f ? 4 : 0) | (SZ53[value1_4461 & 0xff] | (value1_4461 == 0 ? 0x40 : 0));
          int result_4464 = value1_4461 & 0xFF;
          F = (_F2164 & 0xFF);
          A = result_4464;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x3E: {
          int operand_4465 = read((PC + 2) & 0xFFFF, 0);
          A = operand_4465;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x3F: {
          int _F2167;
          int value2_4467 = F;
          _F2167 = value2_4467;
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
          int operand_4470 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4470)) & 0xFFFF;
          int operand_4471 = read(_address2147, 0);
          _value2147 = operand_4471;
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
          int operand_4472 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4472)) & 0xFFFF;
          int operand_4473 = read(_address2147, 0);
          _value2147 = operand_4473;
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
          int operand_4474 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4474)) & 0xFFFF;
          int operand_4475 = read(_address2147, 0);
          _value2147 = operand_4475;
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
          int operand_4476 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4476)) & 0xFFFF;
          int operand_4477 = read(_address2147, 0);
          _value2147 = operand_4477;
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
          int operand_4478 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4478)) & 0xFFFF;
          int operand_4479 = read(_address2147, 0);
          _value2147 = operand_4479;
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
          int operand_4480 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4480)) & 0xFFFF;
          int operand_4481 = read(_address2147, 0);
          _value2147 = operand_4481;
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
          int operand_4482 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4482)) & 0xFFFF;
          write(_address2147, B);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x71: {
          int _address2147;
          int operand_4483 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4483)) & 0xFFFF;
          write(_address2147, C);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x72: {
          int _address2147;
          int operand_4484 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4484)) & 0xFFFF;
          write(_address2147, D);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x73: {
          int _address2147;
          int operand_4485 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4485)) & 0xFFFF;
          write(_address2147, E);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x74: {
          int _address2147;
          int operand_4486 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4486)) & 0xFFFF;
          write(_address2147, H);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x75: {
          int _address2147;
          int operand_4487 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4487)) & 0xFFFF;
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
          int operand_4488 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4488)) & 0xFFFF;
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
          int operand_4489 = read((PC + 2) & 0xFFFF, 0);
          contend5x1(((I << 8) | R));
          _address2147 = (IY + (int) ((byte) operand_4489)) & 0xFFFF;
          int operand_4490 = read(_address2147, 0);
          _value2147 = operand_4490;
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
          int value1_4491 = A;
          int value2_4492 = B;
          int addtemp_4494 = value2_4492 + value1_4491;
          int lookup_4495 = ((value2_4492 & 0x88) >> 3) | ((value1_4491 & 0x88) >> 2) | ((addtemp_4494 & 0x88) >> 1);
          value2_4492 = addtemp_4494 & 0xff;
          _F2233 = ((addtemp_4494 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4495 & 0x07)] | OVERFLOW_ADD[(lookup_4495 >> 4)] | (SZ53[value2_4492] | (value2_4492 == 0 ? 0x40 : 0));
          F = (_F2233 & 0xFF);
          A = value2_4492;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _F2235;
          int value1_4497 = A;
          int value2_4498 = C;
          int addtemp_4500 = value2_4498 + value1_4497;
          int lookup_4501 = ((value2_4498 & 0x88) >> 3) | ((value1_4497 & 0x88) >> 2) | ((addtemp_4500 & 0x88) >> 1);
          value2_4498 = addtemp_4500 & 0xff;
          _F2235 = ((addtemp_4500 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4501 & 0x07)] | OVERFLOW_ADD[(lookup_4501 >> 4)] | (SZ53[value2_4498] | (value2_4498 == 0 ? 0x40 : 0));
          F = (_F2235 & 0xFF);
          A = value2_4498;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _F2237;
          int value1_4503 = A;
          int value2_4504 = D;
          int addtemp_4506 = value2_4504 + value1_4503;
          int lookup_4507 = ((value2_4504 & 0x88) >> 3) | ((value1_4503 & 0x88) >> 2) | ((addtemp_4506 & 0x88) >> 1);
          value2_4504 = addtemp_4506 & 0xff;
          _F2237 = ((addtemp_4506 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4507 & 0x07)] | OVERFLOW_ADD[(lookup_4507 >> 4)] | (SZ53[value2_4504] | (value2_4504 == 0 ? 0x40 : 0));
          F = (_F2237 & 0xFF);
          A = value2_4504;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _F2239;
          int value1_4509 = A;
          int value2_4510 = E;
          int addtemp_4512 = value2_4510 + value1_4509;
          int lookup_4513 = ((value2_4510 & 0x88) >> 3) | ((value1_4509 & 0x88) >> 2) | ((addtemp_4512 & 0x88) >> 1);
          value2_4510 = addtemp_4512 & 0xff;
          _F2239 = ((addtemp_4512 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4513 & 0x07)] | OVERFLOW_ADD[(lookup_4513 >> 4)] | (SZ53[value2_4510] | (value2_4510 == 0 ? 0x40 : 0));
          F = (_F2239 & 0xFF);
          A = value2_4510;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _F2241;
          int value1_4515 = A;
          int value2_4516 = (IY >> 8);
          int addtemp_4518 = value2_4516 + value1_4515;
          int lookup_4519 = ((value2_4516 & 0x88) >> 3) | ((value1_4515 & 0x88) >> 2) | ((addtemp_4518 & 0x88) >> 1);
          value2_4516 = addtemp_4518 & 0xff;
          _F2241 = ((addtemp_4518 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4519 & 0x07)] | OVERFLOW_ADD[(lookup_4519 >> 4)] | (SZ53[value2_4516] | (value2_4516 == 0 ? 0x40 : 0));
          F = (_F2241 & 0xFF);
          A = value2_4516;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _F2243;
          int value1_4521 = A;
          int value2_4522 = (IY & 0xFF);
          int addtemp_4524 = value2_4522 + value1_4521;
          int lookup_4525 = ((value2_4522 & 0x88) >> 3) | ((value1_4521 & 0x88) >> 2) | ((addtemp_4524 & 0x88) >> 1);
          value2_4522 = addtemp_4524 & 0xff;
          _F2243 = ((addtemp_4524 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4525 & 0x07)] | OVERFLOW_ADD[(lookup_4525 >> 4)] | (SZ53[value2_4522] | (value2_4522 == 0 ? 0x40 : 0));
          F = (_F2243 & 0xFF);
          A = value2_4522;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _F2245;
          int _value2147;
          int _address2147;
          int operand_4527 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4527)) & 0xFFFF;
          int operand_4528 = read(_address2147, 0);
          _value2147 = operand_4528;
          int value1_4529 = A;
          int value2_4530 = _value2147;
          int addtemp_4532 = value2_4530 + value1_4529;
          int lookup_4533 = ((value2_4530 & 0x88) >> 3) | ((value1_4529 & 0x88) >> 2) | ((addtemp_4532 & 0x88) >> 1);
          value2_4530 = addtemp_4532 & 0xff;
          _F2245 = ((addtemp_4532 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4533 & 0x07)] | OVERFLOW_ADD[(lookup_4533 >> 4)] | (SZ53[value2_4530] | (value2_4530 == 0 ? 0x40 : 0));
          F = (_F2245 & 0xFF);
          A = value2_4530;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _F2247;
          int value1_4535 = A;
          int value2_4536 = A;
          int addtemp_4538 = value2_4536 + value1_4535;
          int lookup_4539 = ((value2_4536 & 0x88) >> 3) | ((value1_4535 & 0x88) >> 2) | ((addtemp_4538 & 0x88) >> 1);
          value2_4536 = addtemp_4538 & 0xff;
          _F2247 = ((addtemp_4538 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4539 & 0x07)] | OVERFLOW_ADD[(lookup_4539 >> 4)] | (SZ53[value2_4536] | (value2_4536 == 0 ? 0x40 : 0));
          F = (_F2247 & 0xFF);
          A = value2_4536;
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
          int value1_4541 = A;
          int value3_4543 = F & 1;
          _F2249 = value3_4543;
          int adctemp_4544 = value1_4541 + B + (_F2249 & 1);
          int lookup_4545 = ((value1_4541 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((adctemp_4544 & 0x88) >> 1);
          value1_4541 = adctemp_4544 & 0xff;
          _F2249 = ((adctemp_4544 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4545 & 0x07)] | OVERFLOW_ADD[(lookup_4545 >> 4)] | (SZ53[value1_4541] | (value1_4541 == 0 ? 0x40 : 0));
          F = (_F2249 & 0xFF);
          A = value1_4541;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _F2251;
          int value1_4547 = A;
          int value3_4549 = F & 1;
          _F2251 = value3_4549;
          int adctemp_4550 = value1_4547 + C + (_F2251 & 1);
          int lookup_4551 = ((value1_4547 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((adctemp_4550 & 0x88) >> 1);
          value1_4547 = adctemp_4550 & 0xff;
          _F2251 = ((adctemp_4550 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4551 & 0x07)] | OVERFLOW_ADD[(lookup_4551 >> 4)] | (SZ53[value1_4547] | (value1_4547 == 0 ? 0x40 : 0));
          F = (_F2251 & 0xFF);
          A = value1_4547;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _F2253;
          int value1_4553 = A;
          int value3_4555 = F & 1;
          _F2253 = value3_4555;
          int adctemp_4556 = value1_4553 + D + (_F2253 & 1);
          int lookup_4557 = ((value1_4553 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((adctemp_4556 & 0x88) >> 1);
          value1_4553 = adctemp_4556 & 0xff;
          _F2253 = ((adctemp_4556 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4557 & 0x07)] | OVERFLOW_ADD[(lookup_4557 >> 4)] | (SZ53[value1_4553] | (value1_4553 == 0 ? 0x40 : 0));
          F = (_F2253 & 0xFF);
          A = value1_4553;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _F2255;
          int value1_4559 = A;
          int value3_4561 = F & 1;
          _F2255 = value3_4561;
          int adctemp_4562 = value1_4559 + E + (_F2255 & 1);
          int lookup_4563 = ((value1_4559 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((adctemp_4562 & 0x88) >> 1);
          value1_4559 = adctemp_4562 & 0xff;
          _F2255 = ((adctemp_4562 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4563 & 0x07)] | OVERFLOW_ADD[(lookup_4563 >> 4)] | (SZ53[value1_4559] | (value1_4559 == 0 ? 0x40 : 0));
          F = (_F2255 & 0xFF);
          A = value1_4559;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _F2257;
          int value1_4565 = A;
          int value2_4566 = (IY >> 8);
          int value3_4567 = F & 1;
          _F2257 = value3_4567;
          int adctemp_4568 = value1_4565 + value2_4566 + (_F2257 & 1);
          int lookup_4569 = ((value1_4565 & 0x88) >> 3) | ((value2_4566 & 0x88) >> 2) | ((adctemp_4568 & 0x88) >> 1);
          value1_4565 = adctemp_4568 & 0xff;
          _F2257 = ((adctemp_4568 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4569 & 0x07)] | OVERFLOW_ADD[(lookup_4569 >> 4)] | (SZ53[value1_4565] | (value1_4565 == 0 ? 0x40 : 0));
          F = (_F2257 & 0xFF);
          A = value1_4565;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _F2259;
          int value1_4571 = A;
          int value2_4572 = (IY & 0xFF);
          int value3_4573 = F & 1;
          _F2259 = value3_4573;
          int adctemp_4574 = value1_4571 + value2_4572 + (_F2259 & 1);
          int lookup_4575 = ((value1_4571 & 0x88) >> 3) | ((value2_4572 & 0x88) >> 2) | ((adctemp_4574 & 0x88) >> 1);
          value1_4571 = adctemp_4574 & 0xff;
          _F2259 = ((adctemp_4574 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4575 & 0x07)] | OVERFLOW_ADD[(lookup_4575 >> 4)] | (SZ53[value1_4571] | (value1_4571 == 0 ? 0x40 : 0));
          F = (_F2259 & 0xFF);
          A = value1_4571;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _F2261;
          int _value2147;
          int _address2147;
          int operand_4577 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4577)) & 0xFFFF;
          int operand_4578 = read(_address2147, 0);
          _value2147 = operand_4578;
          int value1_4579 = A;
          int value3_4581 = F & 1;
          _F2261 = value3_4581;
          int adctemp_4582 = value1_4579 + _value2147 + (_F2261 & 1);
          int lookup_4583 = ((value1_4579 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((adctemp_4582 & 0x88) >> 1);
          value1_4579 = adctemp_4582 & 0xff;
          _F2261 = ((adctemp_4582 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4583 & 0x07)] | OVERFLOW_ADD[(lookup_4583 >> 4)] | (SZ53[value1_4579] | (value1_4579 == 0 ? 0x40 : 0));
          F = (_F2261 & 0xFF);
          A = value1_4579;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _F2263;
          int value1_4585 = A;
          int value2_4586 = A;
          int value3_4587 = F & 1;
          _F2263 = value3_4587;
          int adctemp_4588 = value1_4585 + value2_4586 + (_F2263 & 1);
          int lookup_4589 = ((value1_4585 & 0x88) >> 3) | ((value2_4586 & 0x88) >> 2) | ((adctemp_4588 & 0x88) >> 1);
          value1_4585 = adctemp_4588 & 0xff;
          _F2263 = ((adctemp_4588 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4589 & 0x07)] | OVERFLOW_ADD[(lookup_4589 >> 4)] | (SZ53[value1_4585] | (value1_4585 == 0 ? 0x40 : 0));
          F = (_F2263 & 0xFF);
          A = value1_4585;
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
          int value1_4591 = A;
          int subtemp_4594 = value1_4591 - B;
          int lookup_4595 = ((value1_4591 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((subtemp_4594 & 0x88) >> 1);
          value1_4591 = subtemp_4594 & 0xff;
          _F2265 = ((subtemp_4594 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4595 & 0x07)] | OVERFLOW_SUB[(lookup_4595 >> 4)] | (SZ53[value1_4591] | (value1_4591 == 0 ? 0x40 : 0));
          F = (_F2265 & 0xFF);
          A = value1_4591;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _F2267;
          int value1_4597 = A;
          int subtemp_4600 = value1_4597 - C;
          int lookup_4601 = ((value1_4597 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((subtemp_4600 & 0x88) >> 1);
          value1_4597 = subtemp_4600 & 0xff;
          _F2267 = ((subtemp_4600 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4601 & 0x07)] | OVERFLOW_SUB[(lookup_4601 >> 4)] | (SZ53[value1_4597] | (value1_4597 == 0 ? 0x40 : 0));
          F = (_F2267 & 0xFF);
          A = value1_4597;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _F2269;
          int value1_4603 = A;
          int subtemp_4606 = value1_4603 - D;
          int lookup_4607 = ((value1_4603 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((subtemp_4606 & 0x88) >> 1);
          value1_4603 = subtemp_4606 & 0xff;
          _F2269 = ((subtemp_4606 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4607 & 0x07)] | OVERFLOW_SUB[(lookup_4607 >> 4)] | (SZ53[value1_4603] | (value1_4603 == 0 ? 0x40 : 0));
          F = (_F2269 & 0xFF);
          A = value1_4603;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _F2271;
          int value1_4609 = A;
          int subtemp_4612 = value1_4609 - E;
          int lookup_4613 = ((value1_4609 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((subtemp_4612 & 0x88) >> 1);
          value1_4609 = subtemp_4612 & 0xff;
          _F2271 = ((subtemp_4612 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4613 & 0x07)] | OVERFLOW_SUB[(lookup_4613 >> 4)] | (SZ53[value1_4609] | (value1_4609 == 0 ? 0x40 : 0));
          F = (_F2271 & 0xFF);
          A = value1_4609;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _F2273;
          int value1_4615 = A;
          int value2_4616 = (IY >> 8);
          int subtemp_4618 = value1_4615 - value2_4616;
          int lookup_4619 = ((value1_4615 & 0x88) >> 3) | ((value2_4616 & 0x88) >> 2) | ((subtemp_4618 & 0x88) >> 1);
          value1_4615 = subtemp_4618 & 0xff;
          _F2273 = ((subtemp_4618 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4619 & 0x07)] | OVERFLOW_SUB[(lookup_4619 >> 4)] | (SZ53[value1_4615] | (value1_4615 == 0 ? 0x40 : 0));
          F = (_F2273 & 0xFF);
          A = value1_4615;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _F2275;
          int value1_4621 = A;
          int value2_4622 = (IY & 0xFF);
          int subtemp_4624 = value1_4621 - value2_4622;
          int lookup_4625 = ((value1_4621 & 0x88) >> 3) | ((value2_4622 & 0x88) >> 2) | ((subtemp_4624 & 0x88) >> 1);
          value1_4621 = subtemp_4624 & 0xff;
          _F2275 = ((subtemp_4624 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4625 & 0x07)] | OVERFLOW_SUB[(lookup_4625 >> 4)] | (SZ53[value1_4621] | (value1_4621 == 0 ? 0x40 : 0));
          F = (_F2275 & 0xFF);
          A = value1_4621;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _F2277;
          int _value2147;
          int _address2147;
          int operand_4627 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4627)) & 0xFFFF;
          int operand_4628 = read(_address2147, 0);
          _value2147 = operand_4628;
          int value1_4629 = A;
          int subtemp_4632 = value1_4629 - _value2147;
          int lookup_4633 = ((value1_4629 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((subtemp_4632 & 0x88) >> 1);
          value1_4629 = subtemp_4632 & 0xff;
          _F2277 = ((subtemp_4632 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4633 & 0x07)] | OVERFLOW_SUB[(lookup_4633 >> 4)] | (SZ53[value1_4629] | (value1_4629 == 0 ? 0x40 : 0));
          F = (_F2277 & 0xFF);
          A = value1_4629;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _F2279;
          int value1_4635 = A;
          int value2_4636 = A;
          int subtemp_4638 = value1_4635 - value2_4636;
          int lookup_4639 = ((value1_4635 & 0x88) >> 3) | ((value2_4636 & 0x88) >> 2) | ((subtemp_4638 & 0x88) >> 1);
          value1_4635 = subtemp_4638 & 0xff;
          _F2279 = ((subtemp_4638 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4639 & 0x07)] | OVERFLOW_SUB[(lookup_4639 >> 4)] | (SZ53[value1_4635] | (value1_4635 == 0 ? 0x40 : 0));
          F = (_F2279 & 0xFF);
          A = value1_4635;
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
          int value1_4641 = A;
          int value3_4643 = F & 1;
          _F2281 = value3_4643;
          int sbctemp_4644 = value1_4641 - B - (_F2281 & 1);
          int lookup_4645 = ((value1_4641 & 0x88) >> 3) | ((B & 0x88) >> 2) | ((sbctemp_4644 & 0x88) >> 1);
          value1_4641 = sbctemp_4644 & 0xff;
          _F2281 = ((sbctemp_4644 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4645 & 0x07)] | OVERFLOW_SUB[(lookup_4645 >> 4)] | (SZ53[value1_4641] | (value1_4641 == 0 ? 0x40 : 0));
          F = (_F2281 & 0xFF);
          A = value1_4641;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _F2283;
          int value1_4647 = A;
          int value3_4649 = F & 1;
          _F2283 = value3_4649;
          int sbctemp_4650 = value1_4647 - C - (_F2283 & 1);
          int lookup_4651 = ((value1_4647 & 0x88) >> 3) | ((C & 0x88) >> 2) | ((sbctemp_4650 & 0x88) >> 1);
          value1_4647 = sbctemp_4650 & 0xff;
          _F2283 = ((sbctemp_4650 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4651 & 0x07)] | OVERFLOW_SUB[(lookup_4651 >> 4)] | (SZ53[value1_4647] | (value1_4647 == 0 ? 0x40 : 0));
          F = (_F2283 & 0xFF);
          A = value1_4647;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _F2285;
          int value1_4653 = A;
          int value3_4655 = F & 1;
          _F2285 = value3_4655;
          int sbctemp_4656 = value1_4653 - D - (_F2285 & 1);
          int lookup_4657 = ((value1_4653 & 0x88) >> 3) | ((D & 0x88) >> 2) | ((sbctemp_4656 & 0x88) >> 1);
          value1_4653 = sbctemp_4656 & 0xff;
          _F2285 = ((sbctemp_4656 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4657 & 0x07)] | OVERFLOW_SUB[(lookup_4657 >> 4)] | (SZ53[value1_4653] | (value1_4653 == 0 ? 0x40 : 0));
          F = (_F2285 & 0xFF);
          A = value1_4653;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _F2287;
          int value1_4659 = A;
          int value3_4661 = F & 1;
          _F2287 = value3_4661;
          int sbctemp_4662 = value1_4659 - E - (_F2287 & 1);
          int lookup_4663 = ((value1_4659 & 0x88) >> 3) | ((E & 0x88) >> 2) | ((sbctemp_4662 & 0x88) >> 1);
          value1_4659 = sbctemp_4662 & 0xff;
          _F2287 = ((sbctemp_4662 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4663 & 0x07)] | OVERFLOW_SUB[(lookup_4663 >> 4)] | (SZ53[value1_4659] | (value1_4659 == 0 ? 0x40 : 0));
          F = (_F2287 & 0xFF);
          A = value1_4659;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _F2289;
          int value1_4665 = A;
          int value2_4666 = (IY >> 8);
          int value3_4667 = F & 1;
          _F2289 = value3_4667;
          int sbctemp_4668 = value1_4665 - value2_4666 - (_F2289 & 1);
          int lookup_4669 = ((value1_4665 & 0x88) >> 3) | ((value2_4666 & 0x88) >> 2) | ((sbctemp_4668 & 0x88) >> 1);
          value1_4665 = sbctemp_4668 & 0xff;
          _F2289 = ((sbctemp_4668 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4669 & 0x07)] | OVERFLOW_SUB[(lookup_4669 >> 4)] | (SZ53[value1_4665] | (value1_4665 == 0 ? 0x40 : 0));
          F = (_F2289 & 0xFF);
          A = value1_4665;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _F2291;
          int value1_4671 = A;
          int value2_4672 = (IY & 0xFF);
          int value3_4673 = F & 1;
          _F2291 = value3_4673;
          int sbctemp_4674 = value1_4671 - value2_4672 - (_F2291 & 1);
          int lookup_4675 = ((value1_4671 & 0x88) >> 3) | ((value2_4672 & 0x88) >> 2) | ((sbctemp_4674 & 0x88) >> 1);
          value1_4671 = sbctemp_4674 & 0xff;
          _F2291 = ((sbctemp_4674 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4675 & 0x07)] | OVERFLOW_SUB[(lookup_4675 >> 4)] | (SZ53[value1_4671] | (value1_4671 == 0 ? 0x40 : 0));
          F = (_F2291 & 0xFF);
          A = value1_4671;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _F2293;
          int _value2147;
          int _address2147;
          int operand_4677 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4677)) & 0xFFFF;
          int operand_4678 = read(_address2147, 0);
          _value2147 = operand_4678;
          int value1_4679 = A;
          int value3_4681 = F & 1;
          _F2293 = value3_4681;
          int sbctemp_4682 = value1_4679 - _value2147 - (_F2293 & 1);
          int lookup_4683 = ((value1_4679 & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((sbctemp_4682 & 0x88) >> 1);
          value1_4679 = sbctemp_4682 & 0xff;
          _F2293 = ((sbctemp_4682 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4683 & 0x07)] | OVERFLOW_SUB[(lookup_4683 >> 4)] | (SZ53[value1_4679] | (value1_4679 == 0 ? 0x40 : 0));
          F = (_F2293 & 0xFF);
          A = value1_4679;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _F2295;
          int value1_4685 = A;
          int value2_4686 = A;
          int value3_4687 = F & 1;
          _F2295 = value3_4687;
          int sbctemp_4688 = value1_4685 - value2_4686 - (_F2295 & 1);
          int lookup_4689 = ((value1_4685 & 0x88) >> 3) | ((value2_4686 & 0x88) >> 2) | ((sbctemp_4688 & 0x88) >> 1);
          value1_4685 = sbctemp_4688 & 0xff;
          _F2295 = ((sbctemp_4688 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_4689 & 0x07)] | OVERFLOW_SUB[(lookup_4689 >> 4)] | (SZ53[value1_4685] | (value1_4685 == 0 ? 0x40 : 0));
          F = (_F2295 & 0xFF);
          A = value1_4685;
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
          int value1_4691 = A;
          int value2_4692 = B;
          value2_4692 &= value1_4691;
          _F2297 = 0x10 | (SZ53P[value2_4692 & 0xff] | (value2_4692 == 0 ? 0x40 : 0));
          int result_4694 = value2_4692 & 0xFF;
          F = (_F2297 & 0xFF);
          A = result_4694;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _F2299;
          int value1_4695 = A;
          int value2_4696 = C;
          value2_4696 &= value1_4695;
          _F2299 = 0x10 | (SZ53P[value2_4696 & 0xff] | (value2_4696 == 0 ? 0x40 : 0));
          int result_4698 = value2_4696 & 0xFF;
          F = (_F2299 & 0xFF);
          A = result_4698;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _F2301;
          int value1_4699 = A;
          int value2_4700 = D;
          value2_4700 &= value1_4699;
          _F2301 = 0x10 | (SZ53P[value2_4700 & 0xff] | (value2_4700 == 0 ? 0x40 : 0));
          int result_4702 = value2_4700 & 0xFF;
          F = (_F2301 & 0xFF);
          A = result_4702;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _F2303;
          int value1_4703 = A;
          int value2_4704 = E;
          value2_4704 &= value1_4703;
          _F2303 = 0x10 | (SZ53P[value2_4704 & 0xff] | (value2_4704 == 0 ? 0x40 : 0));
          int result_4706 = value2_4704 & 0xFF;
          F = (_F2303 & 0xFF);
          A = result_4706;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _F2305;
          int value1_4707 = A;
          int value2_4708 = (IY >> 8);
          value2_4708 &= value1_4707;
          _F2305 = 0x10 | (SZ53P[value2_4708 & 0xff] | (value2_4708 == 0 ? 0x40 : 0));
          int result_4710 = value2_4708 & 0xFF;
          F = (_F2305 & 0xFF);
          A = result_4710;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _F2307;
          int value1_4711 = A;
          int value2_4712 = (IY & 0xFF);
          value2_4712 &= value1_4711;
          _F2307 = 0x10 | (SZ53P[value2_4712 & 0xff] | (value2_4712 == 0 ? 0x40 : 0));
          int result_4714 = value2_4712 & 0xFF;
          F = (_F2307 & 0xFF);
          A = result_4714;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _F2309;
          int _value2147;
          int _address2147;
          int operand_4715 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4715)) & 0xFFFF;
          int operand_4716 = read(_address2147, 0);
          _value2147 = operand_4716;
          int value1_4717 = A;
          int value2_4718 = _value2147;
          value2_4718 &= value1_4717;
          _F2309 = 0x10 | (SZ53P[value2_4718 & 0xff] | (value2_4718 == 0 ? 0x40 : 0));
          int result_4720 = value2_4718 & 0xFF;
          F = (_F2309 & 0xFF);
          A = result_4720;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _F2311;
          int value1_4721 = A;
          int value2_4722 = A;
          value2_4722 &= value1_4721;
          _F2311 = 0x10 | (SZ53P[value2_4722 & 0xff] | (value2_4722 == 0 ? 0x40 : 0));
          int result_4724 = value2_4722 & 0xFF;
          F = (_F2311 & 0xFF);
          A = result_4724;
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
          int value1_4725 = A;
          int value2_4726 = B;
          value2_4726 ^= value1_4725;
          _F2313 = SZ53P[value2_4726 & 0xff] | (value2_4726 == 0 ? 0x40 : 0);
          int result_4728 = value2_4726 & 0xFF;
          F = (_F2313 & 0xFF);
          A = result_4728;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _F2315;
          int value1_4729 = A;
          int value2_4730 = C;
          value2_4730 ^= value1_4729;
          _F2315 = SZ53P[value2_4730 & 0xff] | (value2_4730 == 0 ? 0x40 : 0);
          int result_4732 = value2_4730 & 0xFF;
          F = (_F2315 & 0xFF);
          A = result_4732;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _F2317;
          int value1_4733 = A;
          int value2_4734 = D;
          value2_4734 ^= value1_4733;
          _F2317 = SZ53P[value2_4734 & 0xff] | (value2_4734 == 0 ? 0x40 : 0);
          int result_4736 = value2_4734 & 0xFF;
          F = (_F2317 & 0xFF);
          A = result_4736;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _F2319;
          int value1_4737 = A;
          int value2_4738 = E;
          value2_4738 ^= value1_4737;
          _F2319 = SZ53P[value2_4738 & 0xff] | (value2_4738 == 0 ? 0x40 : 0);
          int result_4740 = value2_4738 & 0xFF;
          F = (_F2319 & 0xFF);
          A = result_4740;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _F2321;
          int value1_4741 = A;
          int value2_4742 = (IY >> 8);
          value2_4742 ^= value1_4741;
          _F2321 = SZ53P[value2_4742 & 0xff] | (value2_4742 == 0 ? 0x40 : 0);
          int result_4744 = value2_4742 & 0xFF;
          F = (_F2321 & 0xFF);
          A = result_4744;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _F2323;
          int value1_4745 = A;
          int value2_4746 = (IY & 0xFF);
          value2_4746 ^= value1_4745;
          _F2323 = SZ53P[value2_4746 & 0xff] | (value2_4746 == 0 ? 0x40 : 0);
          int result_4748 = value2_4746 & 0xFF;
          F = (_F2323 & 0xFF);
          A = result_4748;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _F2325;
          int _value2147;
          int _address2147;
          int operand_4749 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4749)) & 0xFFFF;
          int operand_4750 = read(_address2147, 0);
          _value2147 = operand_4750;
          int value1_4751 = A;
          int value2_4752 = _value2147;
          value2_4752 ^= value1_4751;
          _F2325 = SZ53P[value2_4752 & 0xff] | (value2_4752 == 0 ? 0x40 : 0);
          int result_4754 = value2_4752 & 0xFF;
          F = (_F2325 & 0xFF);
          A = result_4754;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _F2327;
          int value1_4755 = A;
          int value2_4756 = A;
          value2_4756 ^= value1_4755;
          _F2327 = SZ53P[value2_4756 & 0xff] | (value2_4756 == 0 ? 0x40 : 0);
          int result_4758 = value2_4756 & 0xFF;
          F = (_F2327 & 0xFF);
          A = result_4758;
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
          int value1_4759 = A;
          int value2_4760 = B;
          value2_4760 |= value1_4759;
          _F2329 = SZ53P[value2_4760 & 0xff] | (value2_4760 == 0 ? 0x40 : 0);
          int result_4762 = value2_4760 & 0xFF;
          F = (_F2329 & 0xFF);
          A = result_4762;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _F2331;
          int value1_4763 = A;
          int value2_4764 = C;
          value2_4764 |= value1_4763;
          _F2331 = SZ53P[value2_4764 & 0xff] | (value2_4764 == 0 ? 0x40 : 0);
          int result_4766 = value2_4764 & 0xFF;
          F = (_F2331 & 0xFF);
          A = result_4766;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _F2333;
          int value1_4767 = A;
          int value2_4768 = D;
          value2_4768 |= value1_4767;
          _F2333 = SZ53P[value2_4768 & 0xff] | (value2_4768 == 0 ? 0x40 : 0);
          int result_4770 = value2_4768 & 0xFF;
          F = (_F2333 & 0xFF);
          A = result_4770;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _F2335;
          int value1_4771 = A;
          int value2_4772 = E;
          value2_4772 |= value1_4771;
          _F2335 = SZ53P[value2_4772 & 0xff] | (value2_4772 == 0 ? 0x40 : 0);
          int result_4774 = value2_4772 & 0xFF;
          F = (_F2335 & 0xFF);
          A = result_4774;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _F2337;
          int value1_4775 = A;
          int value2_4776 = (IY >> 8);
          value2_4776 |= value1_4775;
          _F2337 = SZ53P[value2_4776 & 0xff] | (value2_4776 == 0 ? 0x40 : 0);
          int result_4778 = value2_4776 & 0xFF;
          F = (_F2337 & 0xFF);
          A = result_4778;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _F2339;
          int value1_4779 = A;
          int value2_4780 = (IY & 0xFF);
          value2_4780 |= value1_4779;
          _F2339 = SZ53P[value2_4780 & 0xff] | (value2_4780 == 0 ? 0x40 : 0);
          int result_4782 = value2_4780 & 0xFF;
          F = (_F2339 & 0xFF);
          A = result_4782;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _F2341;
          int _value2147;
          int _address2147;
          int operand_4783 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4783)) & 0xFFFF;
          int operand_4784 = read(_address2147, 0);
          _value2147 = operand_4784;
          int value1_4785 = A;
          int value2_4786 = _value2147;
          value2_4786 |= value1_4785;
          _F2341 = SZ53P[value2_4786 & 0xff] | (value2_4786 == 0 ? 0x40 : 0);
          int result_4788 = value2_4786 & 0xFF;
          F = (_F2341 & 0xFF);
          A = result_4788;
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _F2343;
          int value1_4789 = A;
          int value2_4790 = A;
          value2_4790 |= value1_4789;
          _F2343 = SZ53P[value2_4790 & 0xff] | (value2_4790 == 0 ? 0x40 : 0);
          int result_4792 = value2_4790 & 0xFF;
          F = (_F2343 & 0xFF);
          A = result_4792;
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
          int cptemp_4796 = A - B;
          int lookup_4797 = ((A & 0x88) >> 3) | ((B & 0x88) >> 2) | ((cptemp_4796 & 0x88) >> 1);
          _F2345 = ((cptemp_4796 & 0x100) != 0 ? 1 : (cptemp_4796 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4797 & 0x07)] | OVERFLOW_SUB[(lookup_4797 >> 4)] | (B & 0x28) | (cptemp_4796 & 0x80);
          F = (_F2345 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _F2347;
          int cptemp_4802 = A - C;
          int lookup_4803 = ((A & 0x88) >> 3) | ((C & 0x88) >> 2) | ((cptemp_4802 & 0x88) >> 1);
          _F2347 = ((cptemp_4802 & 0x100) != 0 ? 1 : (cptemp_4802 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4803 & 0x07)] | OVERFLOW_SUB[(lookup_4803 >> 4)] | (C & 0x28) | (cptemp_4802 & 0x80);
          F = (_F2347 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _F2349;
          int cptemp_4808 = A - D;
          int lookup_4809 = ((A & 0x88) >> 3) | ((D & 0x88) >> 2) | ((cptemp_4808 & 0x88) >> 1);
          _F2349 = ((cptemp_4808 & 0x100) != 0 ? 1 : (cptemp_4808 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4809 & 0x07)] | OVERFLOW_SUB[(lookup_4809 >> 4)] | (D & 0x28) | (cptemp_4808 & 0x80);
          F = (_F2349 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _F2351;
          int cptemp_4814 = A - E;
          int lookup_4815 = ((A & 0x88) >> 3) | ((E & 0x88) >> 2) | ((cptemp_4814 & 0x88) >> 1);
          _F2351 = ((cptemp_4814 & 0x100) != 0 ? 1 : (cptemp_4814 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4815 & 0x07)] | OVERFLOW_SUB[(lookup_4815 >> 4)] | (E & 0x28) | (cptemp_4814 & 0x80);
          F = (_F2351 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _F2353;
          int value2_4818 = (IY >> 8);
          int cptemp_4820 = A - value2_4818;
          int lookup_4821 = ((A & 0x88) >> 3) | ((value2_4818 & 0x88) >> 2) | ((cptemp_4820 & 0x88) >> 1);
          _F2353 = ((cptemp_4820 & 0x100) != 0 ? 1 : (cptemp_4820 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4821 & 0x07)] | OVERFLOW_SUB[(lookup_4821 >> 4)] | (value2_4818 & 0x28) | (cptemp_4820 & 0x80);
          F = (_F2353 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _F2355;
          int value2_4824 = (IY & 0xFF);
          int cptemp_4826 = A - value2_4824;
          int lookup_4827 = ((A & 0x88) >> 3) | ((value2_4824 & 0x88) >> 2) | ((cptemp_4826 & 0x88) >> 1);
          _F2355 = ((cptemp_4826 & 0x100) != 0 ? 1 : (cptemp_4826 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4827 & 0x07)] | OVERFLOW_SUB[(lookup_4827 >> 4)] | (value2_4824 & 0x28) | (cptemp_4826 & 0x80);
          F = (_F2355 & 0xFF);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _F2357;
          int _value2147;
          int _address2147;
          int operand_4829 = read((PC + 2) & 0xFFFF, 0);
          contend5x1((PC + 2) & 0xFFFF);
          _address2147 = (IY + (int) ((byte) operand_4829)) & 0xFFFF;
          int operand_4830 = read(_address2147, 0);
          _value2147 = operand_4830;
          int cptemp_4834 = A - _value2147;
          int lookup_4835 = ((A & 0x88) >> 3) | ((_value2147 & 0x88) >> 2) | ((cptemp_4834 & 0x88) >> 1);
          _F2357 = ((cptemp_4834 & 0x100) != 0 ? 1 : (cptemp_4834 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4835 & 0x07)] | OVERFLOW_SUB[(lookup_4835 >> 4)] | (_value2147 & 0x28) | (cptemp_4834 & 0x80);
          F = (_F2357 & 0xFF);
          MEMPTR = _address2147;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _F2359;
          int cptemp_4840 = A - A;
          int lookup_4841 = ((A & 0x88) >> 3) | ((A & 0x88) >> 2) | ((cptemp_4840 & 0x88) >> 1);
          _F2359 = ((cptemp_4840 & 0x100) != 0 ? 1 : (cptemp_4840 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_4841 & 0x07)] | OVERFLOW_SUB[(lookup_4841 >> 4)] | (A & 0x28) | (cptemp_4840 & 0x80);
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
          int jumpAddress2_4843 = SP;
          if ((!((F & 0x40) == 0x40))) {
              int wordNumber1_4845 = read(SP, 0);
              int wordNumber_4846 = read((SP + 1) & 0xFFFF, 0);
              int value_4844 = ((wordNumber_4846 << 8) | wordNumber1_4845);
              int wordNumber_4847 = SP;
              SP = ((wordNumber_4847 + 2) & 0xFFFF);
              jumpAddress2_4843 = value_4844;
              MEMPTR = jumpAddress2_4843;
              PC = jumpAddress2_4843;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC1: {
          int wordNumber1_4851 = read(SP, 0);
          int wordNumber_4852 = read((SP + 1) & 0xFFFF, 0);
          int value_4850 = ((wordNumber_4852 << 8) | wordNumber1_4851);
          int wordNumber_4853 = SP;
          SP = ((wordNumber_4853 + 2) & 0xFFFF);
          B = (value_4850 >>> 8);
          C = value_4850 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _jumpAddress2363 = 0;
          int address_4855 = (PC + 2) & 0xFFFF;
          int operand_4857 = read(address_4855, 0);
          int operand_4859 = read((address_4855 + 1) & 0xFFFF, 0);
          int jumpAddress2_4854 = (_jumpAddress2363 = (operand_4859 << 8) | operand_4857);
          if ((!((F & 0x40) == 0x40))) {
              _jumpAddress2363 = jumpAddress2_4854;
              MEMPTR = _jumpAddress2363;
              PC = jumpAddress2_4854;
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
          int address_4862 = (PC + 2) & 0xFFFF;
          int operand_4864 = read(address_4862, 0);
          int operand_4866 = read((address_4862 + 1) & 0xFFFF, 0);
          int jumpAddress2_4861 = (_jumpAddress2364 = (operand_4866 << 8) | operand_4864);
          _jumpAddress2364 = jumpAddress2_4861;
          _nextPC2364 = jumpAddress2_4861;
          MEMPTR = _jumpAddress2364;
          PC = _nextPC2364;
          break;
      }
      case 0xC4: {
          int _jumpAddress2365 = 0;
          int address_4868 = (PC + 2) & 0xFFFF;
          int operand_4870 = read(address_4868, 0);
          int operand_4872 = read((address_4868 + 1) & 0xFFFF, 0);
          int value_4873 = (_jumpAddress2365 = (operand_4872 << 8) | operand_4870);
          MEMPTR = value_4873;
          int jumpAddress2_4874 = (_jumpAddress2365 = (operand_4872 << 8) | operand_4870);
          if ((!((F & 0x40) == 0x40))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_4878 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_4878 >>> 8));
              write(SP, (value_4878 & 0xFF));
              _jumpAddress2365 = jumpAddress2_4874;
              MEMPTR = _jumpAddress2365;
              PC = jumpAddress2_4874;
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
          int value_4880 = ((B << 8) | C);
          write((SP + 1) & 0xFFFF, (value_4880 >>> 8));
          write(SP, (value_4880 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _F2367;
          int operand_4881 = read((PC + 2) & 0xFFFF, 0);
          int value1_4882 = A;
          int value2_4883 = operand_4881;
          int addtemp_4885 = value2_4883 + value1_4882;
          int lookup_4886 = ((value2_4883 & 0x88) >> 3) | ((value1_4882 & 0x88) >> 2) | ((addtemp_4885 & 0x88) >> 1);
          value2_4883 = addtemp_4885 & 0xff;
          _F2367 = ((addtemp_4885 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_4886 & 0x07)] | OVERFLOW_ADD[(lookup_4886 >> 4)] | (SZ53[value2_4883] | (value2_4883 == 0 ? 0x40 : 0));
          F = (_F2367 & 0xFF);
          A = value2_4883;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _nextPC2369;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_4888 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_4888 >>> 8));
          write(SP, (value_4888 & 0xFF));
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
          int jumpAddress2_4889 = SP;
          if (((F & 0x40) == 0x40)) {
              int wordNumber1_4891 = read(SP, 0);
              int wordNumber_4892 = read((SP + 1) & 0xFFFF, 0);
              int value_4890 = ((wordNumber_4892 << 8) | wordNumber1_4891);
              int wordNumber_4893 = SP;
              SP = ((wordNumber_4893 + 2) & 0xFFFF);
              jumpAddress2_4889 = value_4890;
              MEMPTR = jumpAddress2_4889;
              PC = jumpAddress2_4889;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xC9: {
          int _nextPC2371;
          int jumpAddress2_4895;
          int wordNumber1_4897 = read(SP, 0);
          int wordNumber_4898 = read((SP + 1) & 0xFFFF, 0);
          int value_4896 = ((wordNumber_4898 << 8) | wordNumber1_4897);
          int wordNumber_4899 = SP;
          SP = ((wordNumber_4899 + 2) & 0xFFFF);
          jumpAddress2_4895 = value_4896;
          _nextPC2371 = jumpAddress2_4895;
          MEMPTR = _nextPC2371;
          PC = _nextPC2371;
          break;
      }
      case 0xCA: {
          int _jumpAddress2372 = 0;
          int address_4902 = (PC + 2) & 0xFFFF;
          int operand_4904 = read(address_4902, 0);
          int operand_4906 = read((address_4902 + 1) & 0xFFFF, 0);
          int jumpAddress2_4901 = (_jumpAddress2372 = (operand_4906 << 8) | operand_4904);
          if (((F & 0x40) == 0x40)) {
              _jumpAddress2372 = jumpAddress2_4901;
              MEMPTR = _jumpAddress2372;
              PC = jumpAddress2_4901;
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
          int address_6116 = (PC + 2) & 0xFFFF;
          int operand_6118 = read(address_6116, 0);
          int operand_6120 = read((address_6116 + 1) & 0xFFFF, 0);
          int value_6121 = (_jumpAddress3013 = (operand_6120 << 8) | operand_6118);
          MEMPTR = value_6121;
          int jumpAddress2_6122 = (_jumpAddress3013 = (operand_6120 << 8) | operand_6118);
          if (((F & 0x40) == 0x40)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6126 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6126 >>> 8));
              write(SP, (value_6126 & 0xFF));
              _jumpAddress3013 = jumpAddress2_6122;
              MEMPTR = _jumpAddress3013;
              PC = jumpAddress2_6122;
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
          int address_6128 = (PC + 2) & 0xFFFF;
          int operand_6130 = read(address_6128, 0);
          int operand_6132 = read((address_6128 + 1) & 0xFFFF, 0);
          int value_6133 = (_jumpAddress3014 = (operand_6132 << 8) | operand_6130);
          int jumpAddress2_6134 = (_jumpAddress3014 = (operand_6132 << 8) | operand_6130);
          SP = ((SP - 2) & 0xFFFF);
          int value_6138 = ((PC + 4) & 0xFFFF);
          contend1x1((PC + 2) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6138 >>> 8));
          write(SP, (value_6138 & 0xFF));
          _jumpAddress3014 = jumpAddress2_6134;
          _nextPC3014 = jumpAddress2_6134;
          MEMPTR = _jumpAddress3014;
          PC = _nextPC3014;
          break;
      }
      case 0xCE: {
          int _F3015;
          int operand_6140 = read((PC + 2) & 0xFFFF, 0);
          int value1_6141 = A;
          int value3_6143 = F & 1;
          _F3015 = value3_6143;
          int adctemp_6144 = value1_6141 + operand_6140 + (_F3015 & 1);
          int lookup_6145 = ((value1_6141 & 0x88) >> 3) | ((operand_6140 & 0x88) >> 2) | ((adctemp_6144 & 0x88) >> 1);
          value1_6141 = adctemp_6144 & 0xff;
          _F3015 = ((adctemp_6144 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_6145 & 0x07)] | OVERFLOW_ADD[(lookup_6145 >> 4)] | (SZ53[value1_6141] | (value1_6141 == 0 ? 0x40 : 0));
          F = (_F3015 & 0xFF);
          A = value1_6141;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _nextPC3017;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6147 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6147 >>> 8));
          write(SP, (value_6147 & 0xFF));
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
          int jumpAddress2_6148 = SP;
          if ((!((F & 1) == 1))) {
              int wordNumber1_6150 = read(SP, 0);
              int wordNumber_6151 = read((SP + 1) & 0xFFFF, 0);
              int value_6149 = ((wordNumber_6151 << 8) | wordNumber1_6150);
              int wordNumber_6152 = SP;
              SP = ((wordNumber_6152 + 2) & 0xFFFF);
              jumpAddress2_6148 = value_6149;
              MEMPTR = jumpAddress2_6148;
              PC = jumpAddress2_6148;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD1: {
          int wordNumber1_6156 = read(SP, 0);
          int wordNumber_6157 = read((SP + 1) & 0xFFFF, 0);
          int value_6155 = ((wordNumber_6157 << 8) | wordNumber1_6156);
          int wordNumber_6158 = SP;
          SP = ((wordNumber_6158 + 2) & 0xFFFF);
          D = (value_6155 >>> 8);
          E = value_6155 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _jumpAddress3020 = 0;
          int address_6160 = (PC + 2) & 0xFFFF;
          int operand_6162 = read(address_6160, 0);
          int operand_6164 = read((address_6160 + 1) & 0xFFFF, 0);
          int jumpAddress2_6159 = (_jumpAddress3020 = (operand_6164 << 8) | operand_6162);
          if ((!((F & 1) == 1))) {
              _jumpAddress3020 = jumpAddress2_6159;
              MEMPTR = _jumpAddress3020;
              PC = jumpAddress2_6159;
              break;
          } else {
              MEMPTR = _jumpAddress3020;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xD3: {
          int operand_6167 = read((PC + 2) & 0xFFFF, 0);
          int read_6166 = operand_6167;
          read_6166 = (read_6166 | A << 8);
          io.out(read_6166, A);
          MEMPTR = (A << 8);
          int read_6168 = operand_6167;
          read_6168 = (read_6168 | A << 8);
          MEMPTR = (MEMPTR | ((read_6168 + 1) & 0xff));
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _jumpAddress3022 = 0;
          int address_6169 = (PC + 2) & 0xFFFF;
          int operand_6171 = read(address_6169, 0);
          int operand_6173 = read((address_6169 + 1) & 0xFFFF, 0);
          int value_6174 = (_jumpAddress3022 = (operand_6173 << 8) | operand_6171);
          MEMPTR = value_6174;
          int jumpAddress2_6175 = (_jumpAddress3022 = (operand_6173 << 8) | operand_6171);
          if ((!((F & 1) == 1))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6179 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6179 >>> 8));
              write(SP, (value_6179 & 0xFF));
              _jumpAddress3022 = jumpAddress2_6175;
              MEMPTR = _jumpAddress3022;
              PC = jumpAddress2_6175;
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
          int value_6181 = ((D << 8) | E);
          write((SP + 1) & 0xFFFF, (value_6181 >>> 8));
          write(SP, (value_6181 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _F3024;
          int operand_6182 = read((PC + 2) & 0xFFFF, 0);
          int value1_6183 = A;
          int subtemp_6186 = value1_6183 - operand_6182;
          int lookup_6187 = ((value1_6183 & 0x88) >> 3) | ((operand_6182 & 0x88) >> 2) | ((subtemp_6186 & 0x88) >> 1);
          value1_6183 = subtemp_6186 & 0xff;
          _F3024 = ((subtemp_6186 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_6187 & 0x07)] | OVERFLOW_SUB[(lookup_6187 >> 4)] | (SZ53[value1_6183] | (value1_6183 == 0 ? 0x40 : 0));
          F = (_F3024 & 0xFF);
          A = value1_6183;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _nextPC3026;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6189 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6189 >>> 8));
          write(SP, (value_6189 & 0xFF));
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
          int jumpAddress2_6190 = SP;
          if (((F & 1) == 1)) {
              int wordNumber1_6192 = read(SP, 0);
              int wordNumber_6193 = read((SP + 1) & 0xFFFF, 0);
              int value_6191 = ((wordNumber_6193 << 8) | wordNumber1_6192);
              int wordNumber_6194 = SP;
              SP = ((wordNumber_6194 + 2) & 0xFFFF);
              jumpAddress2_6190 = value_6191;
              MEMPTR = jumpAddress2_6190;
              PC = jumpAddress2_6190;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xD9: {
          int v1_6196 = ((B << 8) | C);
          B = (_BC >>> 8);
          C = _BC & 0xFF;
          _BC = v1_6196;
          v1_6196 = (D << 8) | E;
          D = (_DE >>> 8);
          E = _DE & 0xFF;
          _DE = v1_6196;
          v1_6196 = (H << 8) | L;
          H = (_HL >>> 8);
          L = _HL & 0xFF;
          _HL = v1_6196;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _jumpAddress3029 = 0;
          int address_6198 = (PC + 2) & 0xFFFF;
          int operand_6200 = read(address_6198, 0);
          int operand_6202 = read((address_6198 + 1) & 0xFFFF, 0);
          int jumpAddress2_6197 = (_jumpAddress3029 = (operand_6202 << 8) | operand_6200);
          if (((F & 1) == 1)) {
              _jumpAddress3029 = jumpAddress2_6197;
              MEMPTR = _jumpAddress3029;
              PC = jumpAddress2_6197;
              break;
          } else {
              MEMPTR = _jumpAddress3029;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xDB: {
          int operand_6205 = read((PC + 2) & 0xFFFF, 0);
          int wordNumber1_6204 = (((Integer) (A << 8)) | operand_6205);
          MEMPTR = ((wordNumber1_6204 + 1) & 0xFFFF);
          int port_6206 = operand_6205;
          port_6206 = (port_6206 | A << 8);
          int value_6207 = io.in(port_6206);
          A = value_6207 & 0xFF;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _jumpAddress3031 = 0;
          int address_6208 = (PC + 2) & 0xFFFF;
          int operand_6210 = read(address_6208, 0);
          int operand_6212 = read((address_6208 + 1) & 0xFFFF, 0);
          int value_6213 = (_jumpAddress3031 = (operand_6212 << 8) | operand_6210);
          MEMPTR = value_6213;
          int jumpAddress2_6214 = (_jumpAddress3031 = (operand_6212 << 8) | operand_6210);
          if (((F & 1) == 1)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6218 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6218 >>> 8));
              write(SP, (value_6218 & 0xFF));
              _jumpAddress3031 = jumpAddress2_6214;
              MEMPTR = _jumpAddress3031;
              PC = jumpAddress2_6214;
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
          int operand_6220 = read((PC + 2) & 0xFFFF, 0);
          int value1_6221 = A;
          int value3_6223 = F & 1;
          _F3033 = value3_6223;
          int sbctemp_6224 = value1_6221 - operand_6220 - (_F3033 & 1);
          int lookup_6225 = ((value1_6221 & 0x88) >> 3) | ((operand_6220 & 0x88) >> 2) | ((sbctemp_6224 & 0x88) >> 1);
          value1_6221 = sbctemp_6224 & 0xff;
          _F3033 = ((sbctemp_6224 & 0x100) != 0 ? 1 : 0) | 2 | HALF_CARRY_SUB[(lookup_6225 & 0x07)] | OVERFLOW_SUB[(lookup_6225 >> 4)] | (SZ53[value1_6221] | (value1_6221 == 0 ? 0x40 : 0));
          F = (_F3033 & 0xFF);
          A = value1_6221;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _nextPC3035;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6227 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6227 >>> 8));
          write(SP, (value_6227 & 0xFF));
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
          int jumpAddress2_6228 = SP;
          if ((!((F & 4) == 4))) {
              int wordNumber1_6230 = read(SP, 0);
              int wordNumber_6231 = read((SP + 1) & 0xFFFF, 0);
              int value_6229 = ((wordNumber_6231 << 8) | wordNumber1_6230);
              int wordNumber_6232 = SP;
              SP = ((wordNumber_6232 + 2) & 0xFFFF);
              jumpAddress2_6228 = value_6229;
              MEMPTR = jumpAddress2_6228;
              PC = jumpAddress2_6228;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xE1: {
          int wordNumber1_6236 = read(SP, 0);
          int wordNumber_6237 = read((SP + 1) & 0xFFFF, 0);
          int value_6235 = ((wordNumber_6237 << 8) | wordNumber1_6236);
          int wordNumber_6238 = SP;
          SP = ((wordNumber_6238 + 2) & 0xFFFF);
          IY = value_6235;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _jumpAddress3038 = 0;
          int address_6240 = (PC + 2) & 0xFFFF;
          int operand_6242 = read(address_6240, 0);
          int operand_6244 = read((address_6240 + 1) & 0xFFFF, 0);
          int jumpAddress2_6239 = (_jumpAddress3038 = (operand_6244 << 8) | operand_6242);
          if ((!((F & 4) == 4))) {
              _jumpAddress3038 = jumpAddress2_6239;
              MEMPTR = _jumpAddress3038;
              PC = jumpAddress2_6239;
              break;
          } else {
              MEMPTR = _jumpAddress3038;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xE3: {
          int _address3039 = SP;
          int wordNumber1_6247 = read(_address3039, 0);
          int wordNumber_6248 = read((_address3039 + 1) & 0xFFFF, 0);
          int v1_6246 = ((wordNumber_6248 << 8) | wordNumber1_6247);
          int v2_6249 = IY;
          _address3039 = SP;
          contend1x1((SP + 1) & 0xFFFF);
          write((_address3039 + 1) & 0xFFFF, (v2_6249 >>> 8));
          write(_address3039, (v2_6249 & 0xFF));
          IY = v1_6246;
          MEMPTR = IY;
          contend2x1(SP);
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _jumpAddress3041 = 0;
          int address_6250 = (PC + 2) & 0xFFFF;
          int operand_6252 = read(address_6250, 0);
          int operand_6254 = read((address_6250 + 1) & 0xFFFF, 0);
          int value_6255 = (_jumpAddress3041 = (operand_6254 << 8) | operand_6252);
          MEMPTR = value_6255;
          int jumpAddress2_6256 = (_jumpAddress3041 = (operand_6254 << 8) | operand_6252);
          if ((!((F & 4) == 4))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6260 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6260 >>> 8));
              write(SP, (value_6260 & 0xFF));
              _jumpAddress3041 = jumpAddress2_6256;
              MEMPTR = _jumpAddress3041;
              PC = jumpAddress2_6256;
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
          int operand_6262 = read((PC + 2) & 0xFFFF, 0);
          int value1_6263 = A;
          int value2_6264 = operand_6262;
          value2_6264 &= value1_6263;
          _F3043 = 0x10 | (SZ53P[value2_6264 & 0xff] | (value2_6264 == 0 ? 0x40 : 0));
          int result_6266 = value2_6264 & 0xFF;
          F = (_F3043 & 0xFF);
          A = result_6266;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _nextPC3045;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6267 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6267 >>> 8));
          write(SP, (value_6267 & 0xFF));
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
          int jumpAddress2_6268 = SP;
          if (((F & 4) == 4)) {
              int wordNumber1_6270 = read(SP, 0);
              int wordNumber_6271 = read((SP + 1) & 0xFFFF, 0);
              int value_6269 = ((wordNumber_6271 << 8) | wordNumber1_6270);
              int wordNumber_6272 = SP;
              SP = ((wordNumber_6272 + 2) & 0xFFFF);
              jumpAddress2_6268 = value_6269;
              MEMPTR = jumpAddress2_6268;
              PC = jumpAddress2_6268;
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
          int address_6277 = (PC + 2) & 0xFFFF;
          int operand_6279 = read(address_6277, 0);
          int operand_6281 = read((address_6277 + 1) & 0xFFFF, 0);
          int jumpAddress2_6276 = (_jumpAddress3048 = (operand_6281 << 8) | operand_6279);
          if (((F & 4) == 4)) {
              _jumpAddress3048 = jumpAddress2_6276;
              MEMPTR = _jumpAddress3048;
              PC = jumpAddress2_6276;
              break;
          } else {
              MEMPTR = _jumpAddress3048;
              PC = (PC + 4) & 0xFFFF;
              break;
          }
      }
      case 0xEB: {
          int v1_6283 = ((D << 8) | E);
          int v2_6284 = ((H << 8) | L);
          D = (v2_6284 >>> 8);
          E = v2_6284 & 0xFF;
          H = (v1_6283 >>> 8);
          L = v1_6283 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _jumpAddress3050 = 0;
          int address_6285 = (PC + 2) & 0xFFFF;
          int operand_6287 = read(address_6285, 0);
          int operand_6289 = read((address_6285 + 1) & 0xFFFF, 0);
          int value_6290 = (_jumpAddress3050 = (operand_6289 << 8) | operand_6287);
          MEMPTR = value_6290;
          int jumpAddress2_6291 = (_jumpAddress3050 = (operand_6289 << 8) | operand_6287);
          if (((F & 4) == 4)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6295 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6295 >>> 8));
              write(SP, (value_6295 & 0xFF));
              _jumpAddress3050 = jumpAddress2_6291;
              MEMPTR = _jumpAddress3050;
              PC = jumpAddress2_6291;
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
          int operand_6297 = read((PC + 2) & 0xFFFF, 0);
          int value1_6298 = A;
          int value2_6299 = operand_6297;
          value2_6299 ^= value1_6298;
          _F3052 = SZ53P[value2_6299 & 0xff] | (value2_6299 == 0 ? 0x40 : 0);
          int result_6301 = value2_6299 & 0xFF;
          F = (_F3052 & 0xFF);
          A = result_6301;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _nextPC3054;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6302 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6302 >>> 8));
          write(SP, (value_6302 & 0xFF));
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
          int jumpAddress2_6303 = SP;
          if ((!((F & 0x80) == 0x80))) {
              int wordNumber1_6305 = read(SP, 0);
              int wordNumber_6306 = read((SP + 1) & 0xFFFF, 0);
              int value_6304 = ((wordNumber_6306 << 8) | wordNumber1_6305);
              int wordNumber_6307 = SP;
              SP = ((wordNumber_6307 + 2) & 0xFFFF);
              jumpAddress2_6303 = value_6304;
              MEMPTR = jumpAddress2_6303;
              PC = jumpAddress2_6303;
              break;
          } else {
              MEMPTR = 0;
              PC = (PC + 2) & 0xFFFF;
              break;
          }
      }
      case 0xF1: {
          int wordNumber1_6311 = read(SP, 0);
          int wordNumber_6312 = read((SP + 1) & 0xFFFF, 0);
          int value_6310 = ((wordNumber_6312 << 8) | wordNumber1_6311);
          int wordNumber_6313 = SP;
          SP = ((wordNumber_6313 + 2) & 0xFFFF);
          A = (value_6310 >>> 8);
          F = value_6310 & 0xFF;
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _jumpAddress3057 = 0;
          int address_6315 = (PC + 2) & 0xFFFF;
          int operand_6317 = read(address_6315, 0);
          int operand_6319 = read((address_6315 + 1) & 0xFFFF, 0);
          int jumpAddress2_6314 = (_jumpAddress3057 = (operand_6319 << 8) | operand_6317);
          if ((!((F & 0x80) == 0x80))) {
              _jumpAddress3057 = jumpAddress2_6314;
              MEMPTR = _jumpAddress3057;
              PC = jumpAddress2_6314;
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
          int address_6321 = (PC + 2) & 0xFFFF;
          int operand_6323 = read(address_6321, 0);
          int operand_6325 = read((address_6321 + 1) & 0xFFFF, 0);
          int value_6326 = (_jumpAddress3059 = (operand_6325 << 8) | operand_6323);
          MEMPTR = value_6326;
          int jumpAddress2_6327 = (_jumpAddress3059 = (operand_6325 << 8) | operand_6323);
          if ((!((F & 0x80) == 0x80))) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6331 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6331 >>> 8));
              write(SP, (value_6331 & 0xFF));
              _jumpAddress3059 = jumpAddress2_6327;
              MEMPTR = _jumpAddress3059;
              PC = jumpAddress2_6327;
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
          int value_6333 = ((A << 8) | F);
          write((SP + 1) & 0xFFFF, (value_6333 >>> 8));
          write(SP, (value_6333 & 0xFF));
          PC = (PC + 2) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _F3061;
          int operand_6334 = read((PC + 2) & 0xFFFF, 0);
          int value1_6335 = A;
          int value2_6336 = operand_6334;
          value2_6336 |= value1_6335;
          _F3061 = SZ53P[value2_6336 & 0xff] | (value2_6336 == 0 ? 0x40 : 0);
          int result_6338 = value2_6336 & 0xFF;
          F = (_F3061 & 0xFF);
          A = result_6338;
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _nextPC3063;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6339 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6339 >>> 8));
          write(SP, (value_6339 & 0xFF));
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
          int jumpAddress2_6340 = SP;
          if (((F & 0x80) == 0x80)) {
              int wordNumber1_6342 = read(SP, 0);
              int wordNumber_6343 = read((SP + 1) & 0xFFFF, 0);
              int value_6341 = ((wordNumber_6343 << 8) | wordNumber1_6342);
              int wordNumber_6344 = SP;
              SP = ((wordNumber_6344 + 2) & 0xFFFF);
              jumpAddress2_6340 = value_6341;
              MEMPTR = jumpAddress2_6340;
              PC = jumpAddress2_6340;
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
          int address_6347 = (PC + 2) & 0xFFFF;
          int operand_6349 = read(address_6347, 0);
          int operand_6351 = read((address_6347 + 1) & 0xFFFF, 0);
          int jumpAddress2_6346 = (_jumpAddress3066 = (operand_6351 << 8) | operand_6349);
          if (((F & 0x80) == 0x80)) {
              _jumpAddress3066 = jumpAddress2_6346;
              MEMPTR = _jumpAddress3066;
              PC = jumpAddress2_6346;
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
          int address_6353 = (PC + 2) & 0xFFFF;
          int operand_6355 = read(address_6353, 0);
          int operand_6357 = read((address_6353 + 1) & 0xFFFF, 0);
          int value_6358 = (_jumpAddress3068 = (operand_6357 << 8) | operand_6355);
          MEMPTR = value_6358;
          int jumpAddress2_6359 = (_jumpAddress3068 = (operand_6357 << 8) | operand_6355);
          if (((F & 0x80) == 0x80)) {
              SP = ((SP - 2) & 0xFFFF);
              int value_6363 = ((PC + 4) & 0xFFFF);
              contend1x1((PC + 2) & 0xFFFF);
              write((SP + 1) & 0xFFFF, (value_6363 >>> 8));
              write(SP, (value_6363 & 0xFF));
              _jumpAddress3068 = jumpAddress2_6359;
              MEMPTR = _jumpAddress3068;
              PC = jumpAddress2_6359;
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
          int operand_6365 = read((PC + 2) & 0xFFFF, 0);
          int cptemp_6369 = A - operand_6365;
          int lookup_6370 = ((A & 0x88) >> 3) | ((operand_6365 & 0x88) >> 2) | ((cptemp_6369 & 0x88) >> 1);
          _F3070 = ((cptemp_6369 & 0x100) != 0 ? 1 : (cptemp_6369 != 0 ? 0 : 0x40)) | 2 | HALF_CARRY_SUB[(lookup_6370 & 0x07)] | OVERFLOW_SUB[(lookup_6370 >> 4)] | (operand_6365 & 0x28) | (cptemp_6369 & 0x80);
          F = (_F3070 & 0xFF);
          PC = (PC + 3) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _nextPC3072;
          contend1x1(((I << 8) | R));
          SP = ((SP - 2) & 0xFFFF);
          int value_6372 = ((PC + 1) & 0xFFFF);
          write((SP + 1) & 0xFFFF, (value_6372 >>> 8));
          write(SP, (value_6372 & 0xFF));
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
          int operand_4908 = read(_address2373, 0);
          contend1x1(_address2373);
          _value2373 = operand_4908;
          int value1_4909 = _value2373;
          value1_4909 = (value1_4909 << 1 | value1_4909 >> 7) & 0xff;
          _F2374 = (value1_4909 & 1) | (SZ53P[value1_4909] | (value1_4909 == 0 ? 0x40 : 0));
          F = (_F2374 & 0xFF);
          _address2373 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2373 = value1_4909;
          write(_address2373, value1_4909);
          int read_4913;
          read_4913 = _value2373;
          B = read_4913;
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
          int operand_4914 = read(_address2376, 0);
          contend1x1(_address2376);
          _value2376 = operand_4914;
          int value1_4915 = _value2376;
          value1_4915 = (value1_4915 << 1 | value1_4915 >> 7) & 0xff;
          _F2377 = (value1_4915 & 1) | (SZ53P[value1_4915] | (value1_4915 == 0 ? 0x40 : 0));
          F = (_F2377 & 0xFF);
          _address2376 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2376 = value1_4915;
          write(_address2376, value1_4915);
          int read_4919;
          read_4919 = _value2376;
          C = read_4919;
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
          int operand_4920 = read(_address2379, 0);
          contend1x1(_address2379);
          _value2379 = operand_4920;
          int value1_4921 = _value2379;
          value1_4921 = (value1_4921 << 1 | value1_4921 >> 7) & 0xff;
          _F2380 = (value1_4921 & 1) | (SZ53P[value1_4921] | (value1_4921 == 0 ? 0x40 : 0));
          F = (_F2380 & 0xFF);
          _address2379 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2379 = value1_4921;
          write(_address2379, value1_4921);
          int read_4925;
          read_4925 = _value2379;
          D = read_4925;
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
          int operand_4926 = read(_address2382, 0);
          contend1x1(_address2382);
          _value2382 = operand_4926;
          int value1_4927 = _value2382;
          value1_4927 = (value1_4927 << 1 | value1_4927 >> 7) & 0xff;
          _F2383 = (value1_4927 & 1) | (SZ53P[value1_4927] | (value1_4927 == 0 ? 0x40 : 0));
          F = (_F2383 & 0xFF);
          _address2382 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2382 = value1_4927;
          write(_address2382, value1_4927);
          int read_4931;
          read_4931 = _value2382;
          E = read_4931;
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
          int operand_4932 = read(_address2385, 0);
          contend1x1(_address2385);
          _value2385 = operand_4932;
          int value1_4933 = _value2385;
          value1_4933 = (value1_4933 << 1 | value1_4933 >> 7) & 0xff;
          _F2386 = (value1_4933 & 1) | (SZ53P[value1_4933] | (value1_4933 == 0 ? 0x40 : 0));
          F = (_F2386 & 0xFF);
          _address2385 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2385 = value1_4933;
          write(_address2385, value1_4933);
          int read_4937;
          read_4937 = _value2385;
          H = read_4937;
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
          int operand_4938 = read(_address2388, 0);
          contend1x1(_address2388);
          _value2388 = operand_4938;
          int value1_4939 = _value2388;
          value1_4939 = (value1_4939 << 1 | value1_4939 >> 7) & 0xff;
          _F2389 = (value1_4939 & 1) | (SZ53P[value1_4939] | (value1_4939 == 0 ? 0x40 : 0));
          F = (_F2389 & 0xFF);
          _address2388 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2388 = value1_4939;
          write(_address2388, value1_4939);
          int read_4943;
          read_4943 = _value2388;
          L = read_4943;
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
          int operand_4944 = read(_address2391, 0);
          contend1x1(_address2391);
          _value2391 = operand_4944;
          int value1_4945 = _value2391;
          value1_4945 = (value1_4945 << 1 | value1_4945 >> 7) & 0xff;
          _F2392 = (value1_4945 & 1) | (SZ53P[value1_4945] | (value1_4945 == 0 ? 0x40 : 0));
          F = (_F2392 & 0xFF);
          _address2391 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2391 = value1_4945;
          write(_address2391, value1_4945);
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
          int operand_4949 = read(_address2394, 0);
          contend1x1(_address2394);
          _value2394 = operand_4949;
          int value1_4950 = _value2394;
          value1_4950 = (value1_4950 << 1 | value1_4950 >> 7) & 0xff;
          _F2395 = (value1_4950 & 1) | (SZ53P[value1_4950] | (value1_4950 == 0 ? 0x40 : 0));
          F = (_F2395 & 0xFF);
          _address2394 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2394 = value1_4950;
          write(_address2394, value1_4950);
          int read_4954;
          read_4954 = _value2394;
          A = read_4954;
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
          int operand_4955 = read(_address2397, 0);
          contend1x1(_address2397);
          _value2397 = operand_4955;
          int value1_4956 = _value2397;
          _F2398 = value1_4956 & 1;
          value1_4956 = (value1_4956 >> 1) | (value1_4956 << 7);
          value1_4956 &= 0xff;
          _F2398 |= (SZ53P[value1_4956 & 0xff] | (value1_4956 == 0 ? 0x40 : 0));
          int result_4959 = value1_4956 & 0xFF;
          F = (_F2398 & 0xFF);
          _address2397 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2397 = result_4959;
          write(_address2397, result_4959);
          int read_4960;
          read_4960 = _value2397;
          B = read_4960;
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
          int operand_4961 = read(_address2400, 0);
          contend1x1(_address2400);
          _value2400 = operand_4961;
          int value1_4962 = _value2400;
          _F2401 = value1_4962 & 1;
          value1_4962 = (value1_4962 >> 1) | (value1_4962 << 7);
          value1_4962 &= 0xff;
          _F2401 |= (SZ53P[value1_4962 & 0xff] | (value1_4962 == 0 ? 0x40 : 0));
          int result_4965 = value1_4962 & 0xFF;
          F = (_F2401 & 0xFF);
          _address2400 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2400 = result_4965;
          write(_address2400, result_4965);
          int read_4966;
          read_4966 = _value2400;
          C = read_4966;
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
          int operand_4967 = read(_address2403, 0);
          contend1x1(_address2403);
          _value2403 = operand_4967;
          int value1_4968 = _value2403;
          _F2404 = value1_4968 & 1;
          value1_4968 = (value1_4968 >> 1) | (value1_4968 << 7);
          value1_4968 &= 0xff;
          _F2404 |= (SZ53P[value1_4968 & 0xff] | (value1_4968 == 0 ? 0x40 : 0));
          int result_4971 = value1_4968 & 0xFF;
          F = (_F2404 & 0xFF);
          _address2403 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2403 = result_4971;
          write(_address2403, result_4971);
          int read_4972;
          read_4972 = _value2403;
          D = read_4972;
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
          int operand_4973 = read(_address2406, 0);
          contend1x1(_address2406);
          _value2406 = operand_4973;
          int value1_4974 = _value2406;
          _F2407 = value1_4974 & 1;
          value1_4974 = (value1_4974 >> 1) | (value1_4974 << 7);
          value1_4974 &= 0xff;
          _F2407 |= (SZ53P[value1_4974 & 0xff] | (value1_4974 == 0 ? 0x40 : 0));
          int result_4977 = value1_4974 & 0xFF;
          F = (_F2407 & 0xFF);
          _address2406 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2406 = result_4977;
          write(_address2406, result_4977);
          int read_4978;
          read_4978 = _value2406;
          E = read_4978;
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
          int operand_4979 = read(_address2409, 0);
          contend1x1(_address2409);
          _value2409 = operand_4979;
          int value1_4980 = _value2409;
          _F2410 = value1_4980 & 1;
          value1_4980 = (value1_4980 >> 1) | (value1_4980 << 7);
          value1_4980 &= 0xff;
          _F2410 |= (SZ53P[value1_4980 & 0xff] | (value1_4980 == 0 ? 0x40 : 0));
          int result_4983 = value1_4980 & 0xFF;
          F = (_F2410 & 0xFF);
          _address2409 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2409 = result_4983;
          write(_address2409, result_4983);
          int read_4984;
          read_4984 = _value2409;
          H = read_4984;
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
          int operand_4985 = read(_address2412, 0);
          contend1x1(_address2412);
          _value2412 = operand_4985;
          int value1_4986 = _value2412;
          _F2413 = value1_4986 & 1;
          value1_4986 = (value1_4986 >> 1) | (value1_4986 << 7);
          value1_4986 &= 0xff;
          _F2413 |= (SZ53P[value1_4986 & 0xff] | (value1_4986 == 0 ? 0x40 : 0));
          int result_4989 = value1_4986 & 0xFF;
          F = (_F2413 & 0xFF);
          _address2412 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2412 = result_4989;
          write(_address2412, result_4989);
          int read_4990;
          read_4990 = _value2412;
          L = read_4990;
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
          int operand_4991 = read(_address2415, 0);
          contend1x1(_address2415);
          _value2415 = operand_4991;
          int value1_4992 = _value2415;
          _F2416 = value1_4992 & 1;
          value1_4992 = (value1_4992 >> 1) | (value1_4992 << 7);
          value1_4992 &= 0xff;
          _F2416 |= (SZ53P[value1_4992 & 0xff] | (value1_4992 == 0 ? 0x40 : 0));
          int result_4995 = value1_4992 & 0xFF;
          F = (_F2416 & 0xFF);
          _address2415 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2415 = result_4995;
          write(_address2415, result_4995);
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
          int operand_4996 = read(_address2418, 0);
          contend1x1(_address2418);
          _value2418 = operand_4996;
          int value1_4997 = _value2418;
          _F2419 = value1_4997 & 1;
          value1_4997 = (value1_4997 >> 1) | (value1_4997 << 7);
          value1_4997 &= 0xff;
          _F2419 |= (SZ53P[value1_4997 & 0xff] | (value1_4997 == 0 ? 0x40 : 0));
          int result_5000 = value1_4997 & 0xFF;
          F = (_F2419 & 0xFF);
          _address2418 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2418 = result_5000;
          write(_address2418, result_5000);
          int read_5001;
          read_5001 = _value2418;
          A = read_5001;
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
          int operand_5002 = read(_address2421, 0);
          contend1x1(_address2421);
          _value2421 = operand_5002;
          int value1_5003 = _value2421;
          int value2_5004 = F;
          _F2422 = value2_5004;
          int rltemp_5006 = value1_5003;
          value1_5003 = (value1_5003 << 1) | (_F2422 & 1);
          value1_5003 &= 0xff;
          _F2422 = (rltemp_5006 >> 7) | (SZ53P[value1_5003 & 0xff] | (value1_5003 == 0 ? 0x40 : 0));
          int result_5007 = value1_5003 & 0xFF;
          F = (_F2422 & 0xFF);
          _address2421 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2421 = result_5007;
          write(_address2421, result_5007);
          int read_5008;
          read_5008 = _value2421;
          B = read_5008;
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
          int operand_5009 = read(_address2424, 0);
          contend1x1(_address2424);
          _value2424 = operand_5009;
          int value1_5010 = _value2424;
          int value2_5011 = F;
          _F2425 = value2_5011;
          int rltemp_5013 = value1_5010;
          value1_5010 = (value1_5010 << 1) | (_F2425 & 1);
          value1_5010 &= 0xff;
          _F2425 = (rltemp_5013 >> 7) | (SZ53P[value1_5010 & 0xff] | (value1_5010 == 0 ? 0x40 : 0));
          int result_5014 = value1_5010 & 0xFF;
          F = (_F2425 & 0xFF);
          _address2424 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2424 = result_5014;
          write(_address2424, result_5014);
          int read_5015;
          read_5015 = _value2424;
          C = read_5015;
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
          int operand_5016 = read(_address2427, 0);
          contend1x1(_address2427);
          _value2427 = operand_5016;
          int value1_5017 = _value2427;
          int value2_5018 = F;
          _F2428 = value2_5018;
          int rltemp_5020 = value1_5017;
          value1_5017 = (value1_5017 << 1) | (_F2428 & 1);
          value1_5017 &= 0xff;
          _F2428 = (rltemp_5020 >> 7) | (SZ53P[value1_5017 & 0xff] | (value1_5017 == 0 ? 0x40 : 0));
          int result_5021 = value1_5017 & 0xFF;
          F = (_F2428 & 0xFF);
          _address2427 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2427 = result_5021;
          write(_address2427, result_5021);
          int read_5022;
          read_5022 = _value2427;
          D = read_5022;
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
          int operand_5023 = read(_address2430, 0);
          contend1x1(_address2430);
          _value2430 = operand_5023;
          int value1_5024 = _value2430;
          int value2_5025 = F;
          _F2431 = value2_5025;
          int rltemp_5027 = value1_5024;
          value1_5024 = (value1_5024 << 1) | (_F2431 & 1);
          value1_5024 &= 0xff;
          _F2431 = (rltemp_5027 >> 7) | (SZ53P[value1_5024 & 0xff] | (value1_5024 == 0 ? 0x40 : 0));
          int result_5028 = value1_5024 & 0xFF;
          F = (_F2431 & 0xFF);
          _address2430 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2430 = result_5028;
          write(_address2430, result_5028);
          int read_5029;
          read_5029 = _value2430;
          E = read_5029;
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
          int operand_5030 = read(_address2433, 0);
          contend1x1(_address2433);
          _value2433 = operand_5030;
          int value1_5031 = _value2433;
          int value2_5032 = F;
          _F2434 = value2_5032;
          int rltemp_5034 = value1_5031;
          value1_5031 = (value1_5031 << 1) | (_F2434 & 1);
          value1_5031 &= 0xff;
          _F2434 = (rltemp_5034 >> 7) | (SZ53P[value1_5031 & 0xff] | (value1_5031 == 0 ? 0x40 : 0));
          int result_5035 = value1_5031 & 0xFF;
          F = (_F2434 & 0xFF);
          _address2433 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2433 = result_5035;
          write(_address2433, result_5035);
          int read_5036;
          read_5036 = _value2433;
          H = read_5036;
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
          int operand_5037 = read(_address2436, 0);
          contend1x1(_address2436);
          _value2436 = operand_5037;
          int value1_5038 = _value2436;
          int value2_5039 = F;
          _F2437 = value2_5039;
          int rltemp_5041 = value1_5038;
          value1_5038 = (value1_5038 << 1) | (_F2437 & 1);
          value1_5038 &= 0xff;
          _F2437 = (rltemp_5041 >> 7) | (SZ53P[value1_5038 & 0xff] | (value1_5038 == 0 ? 0x40 : 0));
          int result_5042 = value1_5038 & 0xFF;
          F = (_F2437 & 0xFF);
          _address2436 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2436 = result_5042;
          write(_address2436, result_5042);
          int read_5043;
          read_5043 = _value2436;
          L = read_5043;
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
          int operand_5044 = read(_address2439, 0);
          contend1x1(_address2439);
          _value2439 = operand_5044;
          int value1_5045 = _value2439;
          int value2_5046 = F;
          _F2440 = value2_5046;
          int rltemp_5048 = value1_5045;
          value1_5045 = (value1_5045 << 1) | (_F2440 & 1);
          value1_5045 &= 0xff;
          _F2440 = (rltemp_5048 >> 7) | (SZ53P[value1_5045 & 0xff] | (value1_5045 == 0 ? 0x40 : 0));
          int result_5049 = value1_5045 & 0xFF;
          F = (_F2440 & 0xFF);
          _address2439 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2439 = result_5049;
          write(_address2439, result_5049);
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
          int operand_5050 = read(_address2442, 0);
          contend1x1(_address2442);
          _value2442 = operand_5050;
          int value1_5051 = _value2442;
          int value2_5052 = F;
          _F2443 = value2_5052;
          int rltemp_5054 = value1_5051;
          value1_5051 = (value1_5051 << 1) | (_F2443 & 1);
          value1_5051 &= 0xff;
          _F2443 = (rltemp_5054 >> 7) | (SZ53P[value1_5051 & 0xff] | (value1_5051 == 0 ? 0x40 : 0));
          int result_5055 = value1_5051 & 0xFF;
          F = (_F2443 & 0xFF);
          _address2442 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2442 = result_5055;
          write(_address2442, result_5055);
          int read_5056;
          read_5056 = _value2442;
          A = read_5056;
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
          int operand_5057 = read(_address2445, 0);
          contend1x1(_address2445);
          _value2445 = operand_5057;
          int value1_5058 = _value2445;
          int value2_5059 = F;
          _F2446 = value2_5059;
          int rrtemp_5061 = value1_5058;
          value1_5058 = (value1_5058 >> 1) | (_F2446 << 7);
          value1_5058 &= 0xff;
          _F2446 = (rrtemp_5061 & 1) | (SZ53P[value1_5058 & 0xff] | (value1_5058 == 0 ? 0x40 : 0));
          int result_5062 = value1_5058 & 0xFF;
          F = (_F2446 & 0xFF);
          _address2445 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2445 = result_5062;
          write(_address2445, result_5062);
          int read_5063;
          read_5063 = _value2445;
          B = read_5063;
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
          int operand_5064 = read(_address2448, 0);
          contend1x1(_address2448);
          _value2448 = operand_5064;
          int value1_5065 = _value2448;
          int value2_5066 = F;
          _F2449 = value2_5066;
          int rrtemp_5068 = value1_5065;
          value1_5065 = (value1_5065 >> 1) | (_F2449 << 7);
          value1_5065 &= 0xff;
          _F2449 = (rrtemp_5068 & 1) | (SZ53P[value1_5065 & 0xff] | (value1_5065 == 0 ? 0x40 : 0));
          int result_5069 = value1_5065 & 0xFF;
          F = (_F2449 & 0xFF);
          _address2448 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2448 = result_5069;
          write(_address2448, result_5069);
          int read_5070;
          read_5070 = _value2448;
          C = read_5070;
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
          int operand_5071 = read(_address2451, 0);
          contend1x1(_address2451);
          _value2451 = operand_5071;
          int value1_5072 = _value2451;
          int value2_5073 = F;
          _F2452 = value2_5073;
          int rrtemp_5075 = value1_5072;
          value1_5072 = (value1_5072 >> 1) | (_F2452 << 7);
          value1_5072 &= 0xff;
          _F2452 = (rrtemp_5075 & 1) | (SZ53P[value1_5072 & 0xff] | (value1_5072 == 0 ? 0x40 : 0));
          int result_5076 = value1_5072 & 0xFF;
          F = (_F2452 & 0xFF);
          _address2451 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2451 = result_5076;
          write(_address2451, result_5076);
          int read_5077;
          read_5077 = _value2451;
          D = read_5077;
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
          int operand_5078 = read(_address2454, 0);
          contend1x1(_address2454);
          _value2454 = operand_5078;
          int value1_5079 = _value2454;
          int value2_5080 = F;
          _F2455 = value2_5080;
          int rrtemp_5082 = value1_5079;
          value1_5079 = (value1_5079 >> 1) | (_F2455 << 7);
          value1_5079 &= 0xff;
          _F2455 = (rrtemp_5082 & 1) | (SZ53P[value1_5079 & 0xff] | (value1_5079 == 0 ? 0x40 : 0));
          int result_5083 = value1_5079 & 0xFF;
          F = (_F2455 & 0xFF);
          _address2454 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2454 = result_5083;
          write(_address2454, result_5083);
          int read_5084;
          read_5084 = _value2454;
          E = read_5084;
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
          int operand_5085 = read(_address2457, 0);
          contend1x1(_address2457);
          _value2457 = operand_5085;
          int value1_5086 = _value2457;
          int value2_5087 = F;
          _F2458 = value2_5087;
          int rrtemp_5089 = value1_5086;
          value1_5086 = (value1_5086 >> 1) | (_F2458 << 7);
          value1_5086 &= 0xff;
          _F2458 = (rrtemp_5089 & 1) | (SZ53P[value1_5086 & 0xff] | (value1_5086 == 0 ? 0x40 : 0));
          int result_5090 = value1_5086 & 0xFF;
          F = (_F2458 & 0xFF);
          _address2457 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2457 = result_5090;
          write(_address2457, result_5090);
          int read_5091;
          read_5091 = _value2457;
          H = read_5091;
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
          int operand_5092 = read(_address2460, 0);
          contend1x1(_address2460);
          _value2460 = operand_5092;
          int value1_5093 = _value2460;
          int value2_5094 = F;
          _F2461 = value2_5094;
          int rrtemp_5096 = value1_5093;
          value1_5093 = (value1_5093 >> 1) | (_F2461 << 7);
          value1_5093 &= 0xff;
          _F2461 = (rrtemp_5096 & 1) | (SZ53P[value1_5093 & 0xff] | (value1_5093 == 0 ? 0x40 : 0));
          int result_5097 = value1_5093 & 0xFF;
          F = (_F2461 & 0xFF);
          _address2460 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2460 = result_5097;
          write(_address2460, result_5097);
          int read_5098;
          read_5098 = _value2460;
          L = read_5098;
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
          int operand_5099 = read(_address2463, 0);
          contend1x1(_address2463);
          _value2463 = operand_5099;
          int value1_5100 = _value2463;
          int value2_5101 = F;
          _F2464 = value2_5101;
          int rrtemp_5103 = value1_5100;
          value1_5100 = (value1_5100 >> 1) | (_F2464 << 7);
          value1_5100 &= 0xff;
          _F2464 = (rrtemp_5103 & 1) | (SZ53P[value1_5100 & 0xff] | (value1_5100 == 0 ? 0x40 : 0));
          int result_5104 = value1_5100 & 0xFF;
          F = (_F2464 & 0xFF);
          _address2463 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2463 = result_5104;
          write(_address2463, result_5104);
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
          int operand_5105 = read(_address2466, 0);
          contend1x1(_address2466);
          _value2466 = operand_5105;
          int value1_5106 = _value2466;
          int value2_5107 = F;
          _F2467 = value2_5107;
          int rrtemp_5109 = value1_5106;
          value1_5106 = (value1_5106 >> 1) | (_F2467 << 7);
          value1_5106 &= 0xff;
          _F2467 = (rrtemp_5109 & 1) | (SZ53P[value1_5106 & 0xff] | (value1_5106 == 0 ? 0x40 : 0));
          int result_5110 = value1_5106 & 0xFF;
          F = (_F2467 & 0xFF);
          _address2466 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2466 = result_5110;
          write(_address2466, result_5110);
          int read_5111;
          read_5111 = _value2466;
          A = read_5111;
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
          int operand_5112 = read(_address2469, 0);
          contend1x1(_address2469);
          _value2469 = operand_5112;
          int value1_5113 = _value2469;
          _F2470 = value1_5113 >> 7;
          value1_5113 <<= 1;
          value1_5113 &= 0xff;
          _F2470 |= (SZ53P[value1_5113 & 0xff] | (value1_5113 == 0 ? 0x40 : 0));
          int result_5116 = value1_5113 & 0xFF;
          F = (_F2470 & 0xFF);
          _address2469 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2469 = result_5116;
          write(_address2469, result_5116);
          int read_5117;
          read_5117 = _value2469;
          B = read_5117;
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
          int operand_5118 = read(_address2472, 0);
          contend1x1(_address2472);
          _value2472 = operand_5118;
          int value1_5119 = _value2472;
          _F2473 = value1_5119 >> 7;
          value1_5119 <<= 1;
          value1_5119 &= 0xff;
          _F2473 |= (SZ53P[value1_5119 & 0xff] | (value1_5119 == 0 ? 0x40 : 0));
          int result_5122 = value1_5119 & 0xFF;
          F = (_F2473 & 0xFF);
          _address2472 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2472 = result_5122;
          write(_address2472, result_5122);
          int read_5123;
          read_5123 = _value2472;
          C = read_5123;
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
          int operand_5124 = read(_address2475, 0);
          contend1x1(_address2475);
          _value2475 = operand_5124;
          int value1_5125 = _value2475;
          _F2476 = value1_5125 >> 7;
          value1_5125 <<= 1;
          value1_5125 &= 0xff;
          _F2476 |= (SZ53P[value1_5125 & 0xff] | (value1_5125 == 0 ? 0x40 : 0));
          int result_5128 = value1_5125 & 0xFF;
          F = (_F2476 & 0xFF);
          _address2475 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2475 = result_5128;
          write(_address2475, result_5128);
          int read_5129;
          read_5129 = _value2475;
          D = read_5129;
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
          int operand_5130 = read(_address2478, 0);
          contend1x1(_address2478);
          _value2478 = operand_5130;
          int value1_5131 = _value2478;
          _F2479 = value1_5131 >> 7;
          value1_5131 <<= 1;
          value1_5131 &= 0xff;
          _F2479 |= (SZ53P[value1_5131 & 0xff] | (value1_5131 == 0 ? 0x40 : 0));
          int result_5134 = value1_5131 & 0xFF;
          F = (_F2479 & 0xFF);
          _address2478 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2478 = result_5134;
          write(_address2478, result_5134);
          int read_5135;
          read_5135 = _value2478;
          E = read_5135;
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
          int operand_5136 = read(_address2481, 0);
          contend1x1(_address2481);
          _value2481 = operand_5136;
          int value1_5137 = _value2481;
          _F2482 = value1_5137 >> 7;
          value1_5137 <<= 1;
          value1_5137 &= 0xff;
          _F2482 |= (SZ53P[value1_5137 & 0xff] | (value1_5137 == 0 ? 0x40 : 0));
          int result_5140 = value1_5137 & 0xFF;
          F = (_F2482 & 0xFF);
          _address2481 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2481 = result_5140;
          write(_address2481, result_5140);
          int read_5141;
          read_5141 = _value2481;
          H = read_5141;
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
          int operand_5142 = read(_address2484, 0);
          contend1x1(_address2484);
          _value2484 = operand_5142;
          int value1_5143 = _value2484;
          _F2485 = value1_5143 >> 7;
          value1_5143 <<= 1;
          value1_5143 &= 0xff;
          _F2485 |= (SZ53P[value1_5143 & 0xff] | (value1_5143 == 0 ? 0x40 : 0));
          int result_5146 = value1_5143 & 0xFF;
          F = (_F2485 & 0xFF);
          _address2484 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2484 = result_5146;
          write(_address2484, result_5146);
          int read_5147;
          read_5147 = _value2484;
          L = read_5147;
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
          int operand_5148 = read(_address2487, 0);
          contend1x1(_address2487);
          _value2487 = operand_5148;
          int value1_5149 = _value2487;
          _F2488 = value1_5149 >> 7;
          value1_5149 <<= 1;
          value1_5149 &= 0xff;
          _F2488 |= (SZ53P[value1_5149 & 0xff] | (value1_5149 == 0 ? 0x40 : 0));
          int result_5152 = value1_5149 & 0xFF;
          F = (_F2488 & 0xFF);
          _address2487 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2487 = result_5152;
          write(_address2487, result_5152);
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
          int operand_5153 = read(_address2490, 0);
          contend1x1(_address2490);
          _value2490 = operand_5153;
          int value1_5154 = _value2490;
          _F2491 = value1_5154 >> 7;
          value1_5154 <<= 1;
          value1_5154 &= 0xff;
          _F2491 |= (SZ53P[value1_5154 & 0xff] | (value1_5154 == 0 ? 0x40 : 0));
          int result_5157 = value1_5154 & 0xFF;
          F = (_F2491 & 0xFF);
          _address2490 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2490 = result_5157;
          write(_address2490, result_5157);
          int read_5158;
          read_5158 = _value2490;
          A = read_5158;
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
          int operand_5159 = read(_address2493, 0);
          contend1x1(_address2493);
          _value2493 = operand_5159;
          int value1_5160 = _value2493;
          _F2494 = value1_5160 & 1;
          value1_5160 = (value1_5160 & 0x80) | (value1_5160 >> 1);
          value1_5160 &= 0xff;
          _F2494 |= (SZ53P[value1_5160 & 0xff] | (value1_5160 == 0 ? 0x40 : 0));
          int result_5163 = value1_5160 & 0xFF;
          F = (_F2494 & 0xFF);
          _address2493 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2493 = result_5163;
          write(_address2493, result_5163);
          int read_5164;
          read_5164 = _value2493;
          B = read_5164;
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
          int operand_5165 = read(_address2496, 0);
          contend1x1(_address2496);
          _value2496 = operand_5165;
          int value1_5166 = _value2496;
          _F2497 = value1_5166 & 1;
          value1_5166 = (value1_5166 & 0x80) | (value1_5166 >> 1);
          value1_5166 &= 0xff;
          _F2497 |= (SZ53P[value1_5166 & 0xff] | (value1_5166 == 0 ? 0x40 : 0));
          int result_5169 = value1_5166 & 0xFF;
          F = (_F2497 & 0xFF);
          _address2496 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2496 = result_5169;
          write(_address2496, result_5169);
          int read_5170;
          read_5170 = _value2496;
          C = read_5170;
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
          int operand_5171 = read(_address2499, 0);
          contend1x1(_address2499);
          _value2499 = operand_5171;
          int value1_5172 = _value2499;
          _F2500 = value1_5172 & 1;
          value1_5172 = (value1_5172 & 0x80) | (value1_5172 >> 1);
          value1_5172 &= 0xff;
          _F2500 |= (SZ53P[value1_5172 & 0xff] | (value1_5172 == 0 ? 0x40 : 0));
          int result_5175 = value1_5172 & 0xFF;
          F = (_F2500 & 0xFF);
          _address2499 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2499 = result_5175;
          write(_address2499, result_5175);
          int read_5176;
          read_5176 = _value2499;
          D = read_5176;
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
          int operand_5177 = read(_address2502, 0);
          contend1x1(_address2502);
          _value2502 = operand_5177;
          int value1_5178 = _value2502;
          _F2503 = value1_5178 & 1;
          value1_5178 = (value1_5178 & 0x80) | (value1_5178 >> 1);
          value1_5178 &= 0xff;
          _F2503 |= (SZ53P[value1_5178 & 0xff] | (value1_5178 == 0 ? 0x40 : 0));
          int result_5181 = value1_5178 & 0xFF;
          F = (_F2503 & 0xFF);
          _address2502 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2502 = result_5181;
          write(_address2502, result_5181);
          int read_5182;
          read_5182 = _value2502;
          E = read_5182;
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
          int operand_5183 = read(_address2505, 0);
          contend1x1(_address2505);
          _value2505 = operand_5183;
          int value1_5184 = _value2505;
          _F2506 = value1_5184 & 1;
          value1_5184 = (value1_5184 & 0x80) | (value1_5184 >> 1);
          value1_5184 &= 0xff;
          _F2506 |= (SZ53P[value1_5184 & 0xff] | (value1_5184 == 0 ? 0x40 : 0));
          int result_5187 = value1_5184 & 0xFF;
          F = (_F2506 & 0xFF);
          _address2505 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2505 = result_5187;
          write(_address2505, result_5187);
          int read_5188;
          read_5188 = _value2505;
          H = read_5188;
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
          int operand_5189 = read(_address2508, 0);
          contend1x1(_address2508);
          _value2508 = operand_5189;
          int value1_5190 = _value2508;
          _F2509 = value1_5190 & 1;
          value1_5190 = (value1_5190 & 0x80) | (value1_5190 >> 1);
          value1_5190 &= 0xff;
          _F2509 |= (SZ53P[value1_5190 & 0xff] | (value1_5190 == 0 ? 0x40 : 0));
          int result_5193 = value1_5190 & 0xFF;
          F = (_F2509 & 0xFF);
          _address2508 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2508 = result_5193;
          write(_address2508, result_5193);
          int read_5194;
          read_5194 = _value2508;
          L = read_5194;
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
          int operand_5195 = read(_address2511, 0);
          contend1x1(_address2511);
          _value2511 = operand_5195;
          int value1_5196 = _value2511;
          _F2512 = value1_5196 & 1;
          value1_5196 = (value1_5196 & 0x80) | (value1_5196 >> 1);
          value1_5196 &= 0xff;
          _F2512 |= (SZ53P[value1_5196 & 0xff] | (value1_5196 == 0 ? 0x40 : 0));
          int result_5199 = value1_5196 & 0xFF;
          F = (_F2512 & 0xFF);
          _address2511 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2511 = result_5199;
          write(_address2511, result_5199);
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
          int operand_5200 = read(_address2514, 0);
          contend1x1(_address2514);
          _value2514 = operand_5200;
          int value1_5201 = _value2514;
          _F2515 = value1_5201 & 1;
          value1_5201 = (value1_5201 & 0x80) | (value1_5201 >> 1);
          value1_5201 &= 0xff;
          _F2515 |= (SZ53P[value1_5201 & 0xff] | (value1_5201 == 0 ? 0x40 : 0));
          int result_5204 = value1_5201 & 0xFF;
          F = (_F2515 & 0xFF);
          _address2514 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2514 = result_5204;
          write(_address2514, result_5204);
          int read_5205;
          read_5205 = _value2514;
          A = read_5205;
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
          int operand_5206 = read(_address2517, 0);
          contend1x1(_address2517);
          _value2517 = operand_5206;
          int value1_5207 = _value2517;
          _F2518 = value1_5207 >> 7;
          value1_5207 = (value1_5207 << 1) | 0x01;
          value1_5207 &= 0xff;
          _F2518 |= (SZ53P[value1_5207 & 0xff] | (value1_5207 == 0 ? 0x40 : 0));
          int result_5210 = value1_5207 & 0xFF;
          F = (_F2518 & 0xFF);
          _address2517 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2517 = result_5210;
          write(_address2517, result_5210);
          int read_5211;
          read_5211 = _value2517;
          B = read_5211;
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
          int operand_5212 = read(_address2520, 0);
          contend1x1(_address2520);
          _value2520 = operand_5212;
          int value1_5213 = _value2520;
          _F2521 = value1_5213 >> 7;
          value1_5213 = (value1_5213 << 1) | 0x01;
          value1_5213 &= 0xff;
          _F2521 |= (SZ53P[value1_5213 & 0xff] | (value1_5213 == 0 ? 0x40 : 0));
          int result_5216 = value1_5213 & 0xFF;
          F = (_F2521 & 0xFF);
          _address2520 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2520 = result_5216;
          write(_address2520, result_5216);
          int read_5217;
          read_5217 = _value2520;
          C = read_5217;
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
          int operand_5218 = read(_address2523, 0);
          contend1x1(_address2523);
          _value2523 = operand_5218;
          int value1_5219 = _value2523;
          _F2524 = value1_5219 >> 7;
          value1_5219 = (value1_5219 << 1) | 0x01;
          value1_5219 &= 0xff;
          _F2524 |= (SZ53P[value1_5219 & 0xff] | (value1_5219 == 0 ? 0x40 : 0));
          int result_5222 = value1_5219 & 0xFF;
          F = (_F2524 & 0xFF);
          _address2523 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2523 = result_5222;
          write(_address2523, result_5222);
          int read_5223;
          read_5223 = _value2523;
          D = read_5223;
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
          int operand_5224 = read(_address2526, 0);
          contend1x1(_address2526);
          _value2526 = operand_5224;
          int value1_5225 = _value2526;
          _F2527 = value1_5225 >> 7;
          value1_5225 = (value1_5225 << 1) | 0x01;
          value1_5225 &= 0xff;
          _F2527 |= (SZ53P[value1_5225 & 0xff] | (value1_5225 == 0 ? 0x40 : 0));
          int result_5228 = value1_5225 & 0xFF;
          F = (_F2527 & 0xFF);
          _address2526 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2526 = result_5228;
          write(_address2526, result_5228);
          int read_5229;
          read_5229 = _value2526;
          E = read_5229;
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
          int operand_5230 = read(_address2529, 0);
          contend1x1(_address2529);
          _value2529 = operand_5230;
          int value1_5231 = _value2529;
          _F2530 = value1_5231 >> 7;
          value1_5231 = (value1_5231 << 1) | 0x01;
          value1_5231 &= 0xff;
          _F2530 |= (SZ53P[value1_5231 & 0xff] | (value1_5231 == 0 ? 0x40 : 0));
          int result_5234 = value1_5231 & 0xFF;
          F = (_F2530 & 0xFF);
          _address2529 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2529 = result_5234;
          write(_address2529, result_5234);
          int read_5235;
          read_5235 = _value2529;
          H = read_5235;
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
          int operand_5236 = read(_address2532, 0);
          contend1x1(_address2532);
          _value2532 = operand_5236;
          int value1_5237 = _value2532;
          _F2533 = value1_5237 >> 7;
          value1_5237 = (value1_5237 << 1) | 0x01;
          value1_5237 &= 0xff;
          _F2533 |= (SZ53P[value1_5237 & 0xff] | (value1_5237 == 0 ? 0x40 : 0));
          int result_5240 = value1_5237 & 0xFF;
          F = (_F2533 & 0xFF);
          _address2532 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2532 = result_5240;
          write(_address2532, result_5240);
          int read_5241;
          read_5241 = _value2532;
          L = read_5241;
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
          int operand_5242 = read(_address2535, 0);
          contend1x1(_address2535);
          _value2535 = operand_5242;
          int value1_5243 = _value2535;
          _F2536 = value1_5243 >> 7;
          value1_5243 = (value1_5243 << 1) | 0x01;
          value1_5243 &= 0xff;
          _F2536 |= (SZ53P[value1_5243 & 0xff] | (value1_5243 == 0 ? 0x40 : 0));
          int result_5246 = value1_5243 & 0xFF;
          F = (_F2536 & 0xFF);
          _address2535 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2535 = result_5246;
          write(_address2535, result_5246);
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
          int operand_5247 = read(_address2538, 0);
          contend1x1(_address2538);
          _value2538 = operand_5247;
          int value1_5248 = _value2538;
          _F2539 = value1_5248 >> 7;
          value1_5248 = (value1_5248 << 1) | 0x01;
          value1_5248 &= 0xff;
          _F2539 |= (SZ53P[value1_5248 & 0xff] | (value1_5248 == 0 ? 0x40 : 0));
          int result_5251 = value1_5248 & 0xFF;
          F = (_F2539 & 0xFF);
          _address2538 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2538 = result_5251;
          write(_address2538, result_5251);
          int read_5252;
          read_5252 = _value2538;
          A = read_5252;
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
          int operand_5253 = read(_address2541, 0);
          contend1x1(_address2541);
          _value2541 = operand_5253;
          int value1_5254 = _value2541;
          _F2542 = value1_5254 & 1;
          value1_5254 >>= 1;
          value1_5254 &= 0xff;
          _F2542 |= (SZ53P[value1_5254 & 0xff] | (value1_5254 == 0 ? 0x40 : 0));
          int result_5257 = value1_5254 & 0xFF;
          F = (_F2542 & 0xFF);
          _address2541 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2541 = result_5257;
          write(_address2541, result_5257);
          int read_5258;
          read_5258 = _value2541;
          B = read_5258;
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
          int operand_5259 = read(_address2544, 0);
          contend1x1(_address2544);
          _value2544 = operand_5259;
          int value1_5260 = _value2544;
          _F2545 = value1_5260 & 1;
          value1_5260 >>= 1;
          value1_5260 &= 0xff;
          _F2545 |= (SZ53P[value1_5260 & 0xff] | (value1_5260 == 0 ? 0x40 : 0));
          int result_5263 = value1_5260 & 0xFF;
          F = (_F2545 & 0xFF);
          _address2544 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2544 = result_5263;
          write(_address2544, result_5263);
          int read_5264;
          read_5264 = _value2544;
          C = read_5264;
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
          int operand_5265 = read(_address2547, 0);
          contend1x1(_address2547);
          _value2547 = operand_5265;
          int value1_5266 = _value2547;
          _F2548 = value1_5266 & 1;
          value1_5266 >>= 1;
          value1_5266 &= 0xff;
          _F2548 |= (SZ53P[value1_5266 & 0xff] | (value1_5266 == 0 ? 0x40 : 0));
          int result_5269 = value1_5266 & 0xFF;
          F = (_F2548 & 0xFF);
          _address2547 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2547 = result_5269;
          write(_address2547, result_5269);
          int read_5270;
          read_5270 = _value2547;
          D = read_5270;
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
          int operand_5271 = read(_address2550, 0);
          contend1x1(_address2550);
          _value2550 = operand_5271;
          int value1_5272 = _value2550;
          _F2551 = value1_5272 & 1;
          value1_5272 >>= 1;
          value1_5272 &= 0xff;
          _F2551 |= (SZ53P[value1_5272 & 0xff] | (value1_5272 == 0 ? 0x40 : 0));
          int result_5275 = value1_5272 & 0xFF;
          F = (_F2551 & 0xFF);
          _address2550 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2550 = result_5275;
          write(_address2550, result_5275);
          int read_5276;
          read_5276 = _value2550;
          E = read_5276;
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
          int operand_5277 = read(_address2553, 0);
          contend1x1(_address2553);
          _value2553 = operand_5277;
          int value1_5278 = _value2553;
          _F2554 = value1_5278 & 1;
          value1_5278 >>= 1;
          value1_5278 &= 0xff;
          _F2554 |= (SZ53P[value1_5278 & 0xff] | (value1_5278 == 0 ? 0x40 : 0));
          int result_5281 = value1_5278 & 0xFF;
          F = (_F2554 & 0xFF);
          _address2553 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2553 = result_5281;
          write(_address2553, result_5281);
          int read_5282;
          read_5282 = _value2553;
          H = read_5282;
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
          int operand_5283 = read(_address2556, 0);
          contend1x1(_address2556);
          _value2556 = operand_5283;
          int value1_5284 = _value2556;
          _F2557 = value1_5284 & 1;
          value1_5284 >>= 1;
          value1_5284 &= 0xff;
          _F2557 |= (SZ53P[value1_5284 & 0xff] | (value1_5284 == 0 ? 0x40 : 0));
          int result_5287 = value1_5284 & 0xFF;
          F = (_F2557 & 0xFF);
          _address2556 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2556 = result_5287;
          write(_address2556, result_5287);
          int read_5288;
          read_5288 = _value2556;
          L = read_5288;
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
          int operand_5289 = read(_address2559, 0);
          contend1x1(_address2559);
          _value2559 = operand_5289;
          int value1_5290 = _value2559;
          _F2560 = value1_5290 & 1;
          value1_5290 >>= 1;
          value1_5290 &= 0xff;
          _F2560 |= (SZ53P[value1_5290 & 0xff] | (value1_5290 == 0 ? 0x40 : 0));
          int result_5293 = value1_5290 & 0xFF;
          F = (_F2560 & 0xFF);
          _address2559 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2559 = result_5293;
          write(_address2559, result_5293);
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
          int operand_5294 = read(_address2562, 0);
          contend1x1(_address2562);
          _value2562 = operand_5294;
          int value1_5295 = _value2562;
          _F2563 = value1_5295 & 1;
          value1_5295 >>= 1;
          value1_5295 &= 0xff;
          _F2563 |= (SZ53P[value1_5295 & 0xff] | (value1_5295 == 0 ? 0x40 : 0));
          int result_5298 = value1_5295 & 0xFF;
          F = (_F2563 & 0xFF);
          _address2562 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2562 = result_5298;
          write(_address2562, result_5298);
          int read_5299;
          read_5299 = _value2562;
          A = read_5299;
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
          int address_5300;
          address_5300 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5301 = F & 1;
          _address2565 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5302 = read(_address2565, 0);
          contend1x1(_address2565);
          _value2565 = operand_5302;
          int value3_5305 = nAndCarry_5301;
          _F2566 = value3_5305;
          value3_5305 = value3_5305 >>> 1;
          _F2566 = (_F2566 & 1) | 0x10 | (address_5300 & 0x28);
          if ((_value2565 & (0x01 << value3_5305)) == 0) {
              _F2566 |= 0x44;
          }
          if (value3_5305 == 7 && (_value2565 & 0x80) != 0) {
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
          int address_5307;
          address_5307 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5308 = F & 1;
          _address2568 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5309 = read(_address2568, 0);
          contend1x1(_address2568);
          _value2568 = operand_5309;
          int value3_5312 = nAndCarry_5308;
          _F2569 = value3_5312;
          value3_5312 = value3_5312 >>> 1;
          _F2569 = (_F2569 & 1) | 0x10 | (address_5307 & 0x28);
          if ((_value2568 & (0x01 << value3_5312)) == 0) {
              _F2569 |= 0x44;
          }
          if (value3_5312 == 7 && (_value2568 & 0x80) != 0) {
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
          int address_5314;
          address_5314 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5315 = F & 1;
          _address2571 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5316 = read(_address2571, 0);
          contend1x1(_address2571);
          _value2571 = operand_5316;
          int value3_5319 = nAndCarry_5315;
          _F2572 = value3_5319;
          value3_5319 = value3_5319 >>> 1;
          _F2572 = (_F2572 & 1) | 0x10 | (address_5314 & 0x28);
          if ((_value2571 & (0x01 << value3_5319)) == 0) {
              _F2572 |= 0x44;
          }
          if (value3_5319 == 7 && (_value2571 & 0x80) != 0) {
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
          int address_5321;
          address_5321 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5322 = F & 1;
          _address2574 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5323 = read(_address2574, 0);
          contend1x1(_address2574);
          _value2574 = operand_5323;
          int value3_5326 = nAndCarry_5322;
          _F2575 = value3_5326;
          value3_5326 = value3_5326 >>> 1;
          _F2575 = (_F2575 & 1) | 0x10 | (address_5321 & 0x28);
          if ((_value2574 & (0x01 << value3_5326)) == 0) {
              _F2575 |= 0x44;
          }
          if (value3_5326 == 7 && (_value2574 & 0x80) != 0) {
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
          int address_5328;
          address_5328 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5329 = F & 1;
          _address2577 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5330 = read(_address2577, 0);
          contend1x1(_address2577);
          _value2577 = operand_5330;
          int value3_5333 = nAndCarry_5329;
          _F2578 = value3_5333;
          value3_5333 = value3_5333 >>> 1;
          _F2578 = (_F2578 & 1) | 0x10 | (address_5328 & 0x28);
          if ((_value2577 & (0x01 << value3_5333)) == 0) {
              _F2578 |= 0x44;
          }
          if (value3_5333 == 7 && (_value2577 & 0x80) != 0) {
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
          int address_5335;
          address_5335 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5336 = F & 1;
          _address2580 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5337 = read(_address2580, 0);
          contend1x1(_address2580);
          _value2580 = operand_5337;
          int value3_5340 = nAndCarry_5336;
          _F2581 = value3_5340;
          value3_5340 = value3_5340 >>> 1;
          _F2581 = (_F2581 & 1) | 0x10 | (address_5335 & 0x28);
          if ((_value2580 & (0x01 << value3_5340)) == 0) {
              _F2581 |= 0x44;
          }
          if (value3_5340 == 7 && (_value2580 & 0x80) != 0) {
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
          int address_5342;
          address_5342 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5343 = F & 1;
          _address2583 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5344 = read(_address2583, 0);
          contend1x1(_address2583);
          _value2583 = operand_5344;
          int value3_5347 = nAndCarry_5343;
          _F2584 = value3_5347;
          value3_5347 = value3_5347 >>> 1;
          _F2584 = (_F2584 & 1) | 0x10 | (address_5342 & 0x28);
          if ((_value2583 & (0x01 << value3_5347)) == 0) {
              _F2584 |= 0x44;
          }
          if (value3_5347 == 7 && (_value2583 & 0x80) != 0) {
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
          int address_5349;
          address_5349 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5350 = F & 1;
          _address2586 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5351 = read(_address2586, 0);
          contend1x1(_address2586);
          _value2586 = operand_5351;
          int value3_5354 = nAndCarry_5350;
          _F2587 = value3_5354;
          value3_5354 = value3_5354 >>> 1;
          _F2587 = (_F2587 & 1) | 0x10 | (address_5349 & 0x28);
          if ((_value2586 & (0x01 << value3_5354)) == 0) {
              _F2587 |= 0x44;
          }
          if (value3_5354 == 7 && (_value2586 & 0x80) != 0) {
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
          int address_5356;
          address_5356 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5357 = 2 | F & 1;
          _address2589 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5358 = read(_address2589, 0);
          contend1x1(_address2589);
          _value2589 = operand_5358;
          int value3_5361 = nAndCarry_5357;
          _F2590 = value3_5361 & 1;
          value3_5361 = value3_5361 >>> 1;
          _F2590 = (_F2590 & 1) | 0x10 | (address_5356 & 0x28);
          if ((_value2589 & (0x01 << value3_5361)) == 0) {
              _F2590 |= 0x44;
          }
          if (value3_5361 == 7 && (_value2589 & 0x80) != 0) {
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
          int address_5363;
          address_5363 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5364 = 2 | F & 1;
          _address2592 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5365 = read(_address2592, 0);
          contend1x1(_address2592);
          _value2592 = operand_5365;
          int value3_5368 = nAndCarry_5364;
          _F2593 = value3_5368 & 1;
          value3_5368 = value3_5368 >>> 1;
          _F2593 = (_F2593 & 1) | 0x10 | (address_5363 & 0x28);
          if ((_value2592 & (0x01 << value3_5368)) == 0) {
              _F2593 |= 0x44;
          }
          if (value3_5368 == 7 && (_value2592 & 0x80) != 0) {
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
          int address_5370;
          address_5370 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5371 = 2 | F & 1;
          _address2595 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5372 = read(_address2595, 0);
          contend1x1(_address2595);
          _value2595 = operand_5372;
          int value3_5375 = nAndCarry_5371;
          _F2596 = value3_5375 & 1;
          value3_5375 = value3_5375 >>> 1;
          _F2596 = (_F2596 & 1) | 0x10 | (address_5370 & 0x28);
          if ((_value2595 & (0x01 << value3_5375)) == 0) {
              _F2596 |= 0x44;
          }
          if (value3_5375 == 7 && (_value2595 & 0x80) != 0) {
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
          int address_5377;
          address_5377 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5378 = 2 | F & 1;
          _address2598 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5379 = read(_address2598, 0);
          contend1x1(_address2598);
          _value2598 = operand_5379;
          int value3_5382 = nAndCarry_5378;
          _F2599 = value3_5382 & 1;
          value3_5382 = value3_5382 >>> 1;
          _F2599 = (_F2599 & 1) | 0x10 | (address_5377 & 0x28);
          if ((_value2598 & (0x01 << value3_5382)) == 0) {
              _F2599 |= 0x44;
          }
          if (value3_5382 == 7 && (_value2598 & 0x80) != 0) {
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
          int address_5384;
          address_5384 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5385 = 2 | F & 1;
          _address2601 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5386 = read(_address2601, 0);
          contend1x1(_address2601);
          _value2601 = operand_5386;
          int value3_5389 = nAndCarry_5385;
          _F2602 = value3_5389 & 1;
          value3_5389 = value3_5389 >>> 1;
          _F2602 = (_F2602 & 1) | 0x10 | (address_5384 & 0x28);
          if ((_value2601 & (0x01 << value3_5389)) == 0) {
              _F2602 |= 0x44;
          }
          if (value3_5389 == 7 && (_value2601 & 0x80) != 0) {
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
          int address_5391;
          address_5391 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5392 = 2 | F & 1;
          _address2604 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5393 = read(_address2604, 0);
          contend1x1(_address2604);
          _value2604 = operand_5393;
          int value3_5396 = nAndCarry_5392;
          _F2605 = value3_5396 & 1;
          value3_5396 = value3_5396 >>> 1;
          _F2605 = (_F2605 & 1) | 0x10 | (address_5391 & 0x28);
          if ((_value2604 & (0x01 << value3_5396)) == 0) {
              _F2605 |= 0x44;
          }
          if (value3_5396 == 7 && (_value2604 & 0x80) != 0) {
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
          int address_5398;
          address_5398 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5399 = 2 | F & 1;
          _address2607 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5400 = read(_address2607, 0);
          contend1x1(_address2607);
          _value2607 = operand_5400;
          int value3_5403 = nAndCarry_5399;
          _F2608 = value3_5403 & 1;
          value3_5403 = value3_5403 >>> 1;
          _F2608 = (_F2608 & 1) | 0x10 | (address_5398 & 0x28);
          if ((_value2607 & (0x01 << value3_5403)) == 0) {
              _F2608 |= 0x44;
          }
          if (value3_5403 == 7 && (_value2607 & 0x80) != 0) {
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
          int address_5405;
          address_5405 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5406 = 2 | F & 1;
          _address2610 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5407 = read(_address2610, 0);
          contend1x1(_address2610);
          _value2610 = operand_5407;
          int value3_5410 = nAndCarry_5406;
          _F2611 = value3_5410 & 1;
          value3_5410 = value3_5410 >>> 1;
          _F2611 = (_F2611 & 1) | 0x10 | (address_5405 & 0x28);
          if ((_value2610 & (0x01 << value3_5410)) == 0) {
              _F2611 |= 0x44;
          }
          if (value3_5410 == 7 && (_value2610 & 0x80) != 0) {
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
          int address_5412;
          address_5412 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5413 = 4 | F & 1;
          _address2613 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5414 = read(_address2613, 0);
          contend1x1(_address2613);
          _value2613 = operand_5414;
          int value3_5417 = nAndCarry_5413;
          _F2614 = value3_5417 & 1;
          value3_5417 = value3_5417 >>> 1;
          _F2614 = (_F2614 & 1) | 0x10 | (address_5412 & 0x28);
          if ((_value2613 & (0x01 << value3_5417)) == 0) {
              _F2614 |= 0x44;
          }
          if (value3_5417 == 7 && (_value2613 & 0x80) != 0) {
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
          int address_5419;
          address_5419 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5420 = 4 | F & 1;
          _address2616 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5421 = read(_address2616, 0);
          contend1x1(_address2616);
          _value2616 = operand_5421;
          int value3_5424 = nAndCarry_5420;
          _F2617 = value3_5424 & 1;
          value3_5424 = value3_5424 >>> 1;
          _F2617 = (_F2617 & 1) | 0x10 | (address_5419 & 0x28);
          if ((_value2616 & (0x01 << value3_5424)) == 0) {
              _F2617 |= 0x44;
          }
          if (value3_5424 == 7 && (_value2616 & 0x80) != 0) {
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
          int address_5426;
          address_5426 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5427 = 4 | F & 1;
          _address2619 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5428 = read(_address2619, 0);
          contend1x1(_address2619);
          _value2619 = operand_5428;
          int value3_5431 = nAndCarry_5427;
          _F2620 = value3_5431 & 1;
          value3_5431 = value3_5431 >>> 1;
          _F2620 = (_F2620 & 1) | 0x10 | (address_5426 & 0x28);
          if ((_value2619 & (0x01 << value3_5431)) == 0) {
              _F2620 |= 0x44;
          }
          if (value3_5431 == 7 && (_value2619 & 0x80) != 0) {
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
          int address_5433;
          address_5433 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5434 = 4 | F & 1;
          _address2622 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5435 = read(_address2622, 0);
          contend1x1(_address2622);
          _value2622 = operand_5435;
          int value3_5438 = nAndCarry_5434;
          _F2623 = value3_5438 & 1;
          value3_5438 = value3_5438 >>> 1;
          _F2623 = (_F2623 & 1) | 0x10 | (address_5433 & 0x28);
          if ((_value2622 & (0x01 << value3_5438)) == 0) {
              _F2623 |= 0x44;
          }
          if (value3_5438 == 7 && (_value2622 & 0x80) != 0) {
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
          int address_5440;
          address_5440 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5441 = 4 | F & 1;
          _address2625 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5442 = read(_address2625, 0);
          contend1x1(_address2625);
          _value2625 = operand_5442;
          int value3_5445 = nAndCarry_5441;
          _F2626 = value3_5445 & 1;
          value3_5445 = value3_5445 >>> 1;
          _F2626 = (_F2626 & 1) | 0x10 | (address_5440 & 0x28);
          if ((_value2625 & (0x01 << value3_5445)) == 0) {
              _F2626 |= 0x44;
          }
          if (value3_5445 == 7 && (_value2625 & 0x80) != 0) {
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
          int address_5447;
          address_5447 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5448 = 4 | F & 1;
          _address2628 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5449 = read(_address2628, 0);
          contend1x1(_address2628);
          _value2628 = operand_5449;
          int value3_5452 = nAndCarry_5448;
          _F2629 = value3_5452 & 1;
          value3_5452 = value3_5452 >>> 1;
          _F2629 = (_F2629 & 1) | 0x10 | (address_5447 & 0x28);
          if ((_value2628 & (0x01 << value3_5452)) == 0) {
              _F2629 |= 0x44;
          }
          if (value3_5452 == 7 && (_value2628 & 0x80) != 0) {
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
          int address_5454;
          address_5454 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5455 = 4 | F & 1;
          _address2631 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5456 = read(_address2631, 0);
          contend1x1(_address2631);
          _value2631 = operand_5456;
          int value3_5459 = nAndCarry_5455;
          _F2632 = value3_5459 & 1;
          value3_5459 = value3_5459 >>> 1;
          _F2632 = (_F2632 & 1) | 0x10 | (address_5454 & 0x28);
          if ((_value2631 & (0x01 << value3_5459)) == 0) {
              _F2632 |= 0x44;
          }
          if (value3_5459 == 7 && (_value2631 & 0x80) != 0) {
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
          int address_5461;
          address_5461 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5462 = 4 | F & 1;
          _address2634 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5463 = read(_address2634, 0);
          contend1x1(_address2634);
          _value2634 = operand_5463;
          int value3_5466 = nAndCarry_5462;
          _F2635 = value3_5466 & 1;
          value3_5466 = value3_5466 >>> 1;
          _F2635 = (_F2635 & 1) | 0x10 | (address_5461 & 0x28);
          if ((_value2634 & (0x01 << value3_5466)) == 0) {
              _F2635 |= 0x44;
          }
          if (value3_5466 == 7 && (_value2634 & 0x80) != 0) {
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
          int address_5468;
          address_5468 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5469 = 6 | F & 1;
          _address2637 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5470 = read(_address2637, 0);
          contend1x1(_address2637);
          _value2637 = operand_5470;
          int value3_5473 = nAndCarry_5469;
          _F2638 = value3_5473 & 1;
          value3_5473 = value3_5473 >>> 1;
          _F2638 = (_F2638 & 1) | 0x10 | (address_5468 & 0x28);
          if ((_value2637 & (0x01 << value3_5473)) == 0) {
              _F2638 |= 0x44;
          }
          if (value3_5473 == 7 && (_value2637 & 0x80) != 0) {
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
          int address_5475;
          address_5475 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5476 = 6 | F & 1;
          _address2640 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5477 = read(_address2640, 0);
          contend1x1(_address2640);
          _value2640 = operand_5477;
          int value3_5480 = nAndCarry_5476;
          _F2641 = value3_5480 & 1;
          value3_5480 = value3_5480 >>> 1;
          _F2641 = (_F2641 & 1) | 0x10 | (address_5475 & 0x28);
          if ((_value2640 & (0x01 << value3_5480)) == 0) {
              _F2641 |= 0x44;
          }
          if (value3_5480 == 7 && (_value2640 & 0x80) != 0) {
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
          int address_5482;
          address_5482 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5483 = 6 | F & 1;
          _address2643 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5484 = read(_address2643, 0);
          contend1x1(_address2643);
          _value2643 = operand_5484;
          int value3_5487 = nAndCarry_5483;
          _F2644 = value3_5487 & 1;
          value3_5487 = value3_5487 >>> 1;
          _F2644 = (_F2644 & 1) | 0x10 | (address_5482 & 0x28);
          if ((_value2643 & (0x01 << value3_5487)) == 0) {
              _F2644 |= 0x44;
          }
          if (value3_5487 == 7 && (_value2643 & 0x80) != 0) {
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
          int address_5489;
          address_5489 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5490 = 6 | F & 1;
          _address2646 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5491 = read(_address2646, 0);
          contend1x1(_address2646);
          _value2646 = operand_5491;
          int value3_5494 = nAndCarry_5490;
          _F2647 = value3_5494 & 1;
          value3_5494 = value3_5494 >>> 1;
          _F2647 = (_F2647 & 1) | 0x10 | (address_5489 & 0x28);
          if ((_value2646 & (0x01 << value3_5494)) == 0) {
              _F2647 |= 0x44;
          }
          if (value3_5494 == 7 && (_value2646 & 0x80) != 0) {
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
          int address_5496;
          address_5496 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5497 = 6 | F & 1;
          _address2649 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5498 = read(_address2649, 0);
          contend1x1(_address2649);
          _value2649 = operand_5498;
          int value3_5501 = nAndCarry_5497;
          _F2650 = value3_5501 & 1;
          value3_5501 = value3_5501 >>> 1;
          _F2650 = (_F2650 & 1) | 0x10 | (address_5496 & 0x28);
          if ((_value2649 & (0x01 << value3_5501)) == 0) {
              _F2650 |= 0x44;
          }
          if (value3_5501 == 7 && (_value2649 & 0x80) != 0) {
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
          int address_5503;
          address_5503 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5504 = 6 | F & 1;
          _address2652 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5505 = read(_address2652, 0);
          contend1x1(_address2652);
          _value2652 = operand_5505;
          int value3_5508 = nAndCarry_5504;
          _F2653 = value3_5508 & 1;
          value3_5508 = value3_5508 >>> 1;
          _F2653 = (_F2653 & 1) | 0x10 | (address_5503 & 0x28);
          if ((_value2652 & (0x01 << value3_5508)) == 0) {
              _F2653 |= 0x44;
          }
          if (value3_5508 == 7 && (_value2652 & 0x80) != 0) {
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
          int address_5510;
          address_5510 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5511 = 6 | F & 1;
          _address2655 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5512 = read(_address2655, 0);
          contend1x1(_address2655);
          _value2655 = operand_5512;
          int value3_5515 = nAndCarry_5511;
          _F2656 = value3_5515 & 1;
          value3_5515 = value3_5515 >>> 1;
          _F2656 = (_F2656 & 1) | 0x10 | (address_5510 & 0x28);
          if ((_value2655 & (0x01 << value3_5515)) == 0) {
              _F2656 |= 0x44;
          }
          if (value3_5515 == 7 && (_value2655 & 0x80) != 0) {
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
          int address_5517;
          address_5517 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5518 = 6 | F & 1;
          _address2658 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5519 = read(_address2658, 0);
          contend1x1(_address2658);
          _value2658 = operand_5519;
          int value3_5522 = nAndCarry_5518;
          _F2659 = value3_5522 & 1;
          value3_5522 = value3_5522 >>> 1;
          _F2659 = (_F2659 & 1) | 0x10 | (address_5517 & 0x28);
          if ((_value2658 & (0x01 << value3_5522)) == 0) {
              _F2659 |= 0x44;
          }
          if (value3_5522 == 7 && (_value2658 & 0x80) != 0) {
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
          int address_5524;
          address_5524 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5525 = 8 | F & 1;
          _address2661 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5526 = read(_address2661, 0);
          contend1x1(_address2661);
          _value2661 = operand_5526;
          int value3_5529 = nAndCarry_5525;
          _F2662 = value3_5529 & 1;
          value3_5529 = value3_5529 >>> 1;
          _F2662 = (_F2662 & 1) | 0x10 | (address_5524 & 0x28);
          if ((_value2661 & (0x01 << value3_5529)) == 0) {
              _F2662 |= 0x44;
          }
          if (value3_5529 == 7 && (_value2661 & 0x80) != 0) {
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
          int address_5531;
          address_5531 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5532 = 8 | F & 1;
          _address2664 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5533 = read(_address2664, 0);
          contend1x1(_address2664);
          _value2664 = operand_5533;
          int value3_5536 = nAndCarry_5532;
          _F2665 = value3_5536 & 1;
          value3_5536 = value3_5536 >>> 1;
          _F2665 = (_F2665 & 1) | 0x10 | (address_5531 & 0x28);
          if ((_value2664 & (0x01 << value3_5536)) == 0) {
              _F2665 |= 0x44;
          }
          if (value3_5536 == 7 && (_value2664 & 0x80) != 0) {
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
          int address_5538;
          address_5538 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5539 = 8 | F & 1;
          _address2667 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5540 = read(_address2667, 0);
          contend1x1(_address2667);
          _value2667 = operand_5540;
          int value3_5543 = nAndCarry_5539;
          _F2668 = value3_5543 & 1;
          value3_5543 = value3_5543 >>> 1;
          _F2668 = (_F2668 & 1) | 0x10 | (address_5538 & 0x28);
          if ((_value2667 & (0x01 << value3_5543)) == 0) {
              _F2668 |= 0x44;
          }
          if (value3_5543 == 7 && (_value2667 & 0x80) != 0) {
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
          int address_5545;
          address_5545 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5546 = 8 | F & 1;
          _address2670 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5547 = read(_address2670, 0);
          contend1x1(_address2670);
          _value2670 = operand_5547;
          int value3_5550 = nAndCarry_5546;
          _F2671 = value3_5550 & 1;
          value3_5550 = value3_5550 >>> 1;
          _F2671 = (_F2671 & 1) | 0x10 | (address_5545 & 0x28);
          if ((_value2670 & (0x01 << value3_5550)) == 0) {
              _F2671 |= 0x44;
          }
          if (value3_5550 == 7 && (_value2670 & 0x80) != 0) {
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
          int address_5552;
          address_5552 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5553 = 8 | F & 1;
          _address2673 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5554 = read(_address2673, 0);
          contend1x1(_address2673);
          _value2673 = operand_5554;
          int value3_5557 = nAndCarry_5553;
          _F2674 = value3_5557 & 1;
          value3_5557 = value3_5557 >>> 1;
          _F2674 = (_F2674 & 1) | 0x10 | (address_5552 & 0x28);
          if ((_value2673 & (0x01 << value3_5557)) == 0) {
              _F2674 |= 0x44;
          }
          if (value3_5557 == 7 && (_value2673 & 0x80) != 0) {
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
          int address_5559;
          address_5559 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5560 = 8 | F & 1;
          _address2676 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5561 = read(_address2676, 0);
          contend1x1(_address2676);
          _value2676 = operand_5561;
          int value3_5564 = nAndCarry_5560;
          _F2677 = value3_5564 & 1;
          value3_5564 = value3_5564 >>> 1;
          _F2677 = (_F2677 & 1) | 0x10 | (address_5559 & 0x28);
          if ((_value2676 & (0x01 << value3_5564)) == 0) {
              _F2677 |= 0x44;
          }
          if (value3_5564 == 7 && (_value2676 & 0x80) != 0) {
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
          int address_5566;
          address_5566 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5567 = 8 | F & 1;
          _address2679 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5568 = read(_address2679, 0);
          contend1x1(_address2679);
          _value2679 = operand_5568;
          int value3_5571 = nAndCarry_5567;
          _F2680 = value3_5571 & 1;
          value3_5571 = value3_5571 >>> 1;
          _F2680 = (_F2680 & 1) | 0x10 | (address_5566 & 0x28);
          if ((_value2679 & (0x01 << value3_5571)) == 0) {
              _F2680 |= 0x44;
          }
          if (value3_5571 == 7 && (_value2679 & 0x80) != 0) {
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
          int address_5573;
          address_5573 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5574 = 8 | F & 1;
          _address2682 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5575 = read(_address2682, 0);
          contend1x1(_address2682);
          _value2682 = operand_5575;
          int value3_5578 = nAndCarry_5574;
          _F2683 = value3_5578 & 1;
          value3_5578 = value3_5578 >>> 1;
          _F2683 = (_F2683 & 1) | 0x10 | (address_5573 & 0x28);
          if ((_value2682 & (0x01 << value3_5578)) == 0) {
              _F2683 |= 0x44;
          }
          if (value3_5578 == 7 && (_value2682 & 0x80) != 0) {
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
          int address_5580;
          address_5580 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5581 = 10 | F & 1;
          _address2685 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5582 = read(_address2685, 0);
          contend1x1(_address2685);
          _value2685 = operand_5582;
          int value3_5585 = nAndCarry_5581;
          _F2686 = value3_5585 & 1;
          value3_5585 = value3_5585 >>> 1;
          _F2686 = (_F2686 & 1) | 0x10 | (address_5580 & 0x28);
          if ((_value2685 & (0x01 << value3_5585)) == 0) {
              _F2686 |= 0x44;
          }
          if (value3_5585 == 7 && (_value2685 & 0x80) != 0) {
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
          int address_5587;
          address_5587 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5588 = 10 | F & 1;
          _address2688 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5589 = read(_address2688, 0);
          contend1x1(_address2688);
          _value2688 = operand_5589;
          int value3_5592 = nAndCarry_5588;
          _F2689 = value3_5592 & 1;
          value3_5592 = value3_5592 >>> 1;
          _F2689 = (_F2689 & 1) | 0x10 | (address_5587 & 0x28);
          if ((_value2688 & (0x01 << value3_5592)) == 0) {
              _F2689 |= 0x44;
          }
          if (value3_5592 == 7 && (_value2688 & 0x80) != 0) {
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
          int address_5594;
          address_5594 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5595 = 10 | F & 1;
          _address2691 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5596 = read(_address2691, 0);
          contend1x1(_address2691);
          _value2691 = operand_5596;
          int value3_5599 = nAndCarry_5595;
          _F2692 = value3_5599 & 1;
          value3_5599 = value3_5599 >>> 1;
          _F2692 = (_F2692 & 1) | 0x10 | (address_5594 & 0x28);
          if ((_value2691 & (0x01 << value3_5599)) == 0) {
              _F2692 |= 0x44;
          }
          if (value3_5599 == 7 && (_value2691 & 0x80) != 0) {
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
          int address_5601;
          address_5601 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5602 = 10 | F & 1;
          _address2694 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5603 = read(_address2694, 0);
          contend1x1(_address2694);
          _value2694 = operand_5603;
          int value3_5606 = nAndCarry_5602;
          _F2695 = value3_5606 & 1;
          value3_5606 = value3_5606 >>> 1;
          _F2695 = (_F2695 & 1) | 0x10 | (address_5601 & 0x28);
          if ((_value2694 & (0x01 << value3_5606)) == 0) {
              _F2695 |= 0x44;
          }
          if (value3_5606 == 7 && (_value2694 & 0x80) != 0) {
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
          int address_5608;
          address_5608 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5609 = 10 | F & 1;
          _address2697 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5610 = read(_address2697, 0);
          contend1x1(_address2697);
          _value2697 = operand_5610;
          int value3_5613 = nAndCarry_5609;
          _F2698 = value3_5613 & 1;
          value3_5613 = value3_5613 >>> 1;
          _F2698 = (_F2698 & 1) | 0x10 | (address_5608 & 0x28);
          if ((_value2697 & (0x01 << value3_5613)) == 0) {
              _F2698 |= 0x44;
          }
          if (value3_5613 == 7 && (_value2697 & 0x80) != 0) {
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
          int address_5615;
          address_5615 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5616 = 10 | F & 1;
          _address2700 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5617 = read(_address2700, 0);
          contend1x1(_address2700);
          _value2700 = operand_5617;
          int value3_5620 = nAndCarry_5616;
          _F2701 = value3_5620 & 1;
          value3_5620 = value3_5620 >>> 1;
          _F2701 = (_F2701 & 1) | 0x10 | (address_5615 & 0x28);
          if ((_value2700 & (0x01 << value3_5620)) == 0) {
              _F2701 |= 0x44;
          }
          if (value3_5620 == 7 && (_value2700 & 0x80) != 0) {
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
          int address_5622;
          address_5622 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5623 = 10 | F & 1;
          _address2703 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5624 = read(_address2703, 0);
          contend1x1(_address2703);
          _value2703 = operand_5624;
          int value3_5627 = nAndCarry_5623;
          _F2704 = value3_5627 & 1;
          value3_5627 = value3_5627 >>> 1;
          _F2704 = (_F2704 & 1) | 0x10 | (address_5622 & 0x28);
          if ((_value2703 & (0x01 << value3_5627)) == 0) {
              _F2704 |= 0x44;
          }
          if (value3_5627 == 7 && (_value2703 & 0x80) != 0) {
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
          int address_5629;
          address_5629 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5630 = 10 | F & 1;
          _address2706 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5631 = read(_address2706, 0);
          contend1x1(_address2706);
          _value2706 = operand_5631;
          int value3_5634 = nAndCarry_5630;
          _F2707 = value3_5634 & 1;
          value3_5634 = value3_5634 >>> 1;
          _F2707 = (_F2707 & 1) | 0x10 | (address_5629 & 0x28);
          if ((_value2706 & (0x01 << value3_5634)) == 0) {
              _F2707 |= 0x44;
          }
          if (value3_5634 == 7 && (_value2706 & 0x80) != 0) {
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
          int address_5636;
          address_5636 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5637 = 12 | F & 1;
          _address2709 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5638 = read(_address2709, 0);
          contend1x1(_address2709);
          _value2709 = operand_5638;
          int value3_5641 = nAndCarry_5637;
          _F2710 = value3_5641 & 1;
          value3_5641 = value3_5641 >>> 1;
          _F2710 = (_F2710 & 1) | 0x10 | (address_5636 & 0x28);
          if ((_value2709 & (0x01 << value3_5641)) == 0) {
              _F2710 |= 0x44;
          }
          if (value3_5641 == 7 && (_value2709 & 0x80) != 0) {
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
          int address_5643;
          address_5643 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5644 = 12 | F & 1;
          _address2712 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5645 = read(_address2712, 0);
          contend1x1(_address2712);
          _value2712 = operand_5645;
          int value3_5648 = nAndCarry_5644;
          _F2713 = value3_5648 & 1;
          value3_5648 = value3_5648 >>> 1;
          _F2713 = (_F2713 & 1) | 0x10 | (address_5643 & 0x28);
          if ((_value2712 & (0x01 << value3_5648)) == 0) {
              _F2713 |= 0x44;
          }
          if (value3_5648 == 7 && (_value2712 & 0x80) != 0) {
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
          int address_5650;
          address_5650 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5651 = 12 | F & 1;
          _address2715 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5652 = read(_address2715, 0);
          contend1x1(_address2715);
          _value2715 = operand_5652;
          int value3_5655 = nAndCarry_5651;
          _F2716 = value3_5655 & 1;
          value3_5655 = value3_5655 >>> 1;
          _F2716 = (_F2716 & 1) | 0x10 | (address_5650 & 0x28);
          if ((_value2715 & (0x01 << value3_5655)) == 0) {
              _F2716 |= 0x44;
          }
          if (value3_5655 == 7 && (_value2715 & 0x80) != 0) {
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
          int address_5657;
          address_5657 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5658 = 12 | F & 1;
          _address2718 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5659 = read(_address2718, 0);
          contend1x1(_address2718);
          _value2718 = operand_5659;
          int value3_5662 = nAndCarry_5658;
          _F2719 = value3_5662 & 1;
          value3_5662 = value3_5662 >>> 1;
          _F2719 = (_F2719 & 1) | 0x10 | (address_5657 & 0x28);
          if ((_value2718 & (0x01 << value3_5662)) == 0) {
              _F2719 |= 0x44;
          }
          if (value3_5662 == 7 && (_value2718 & 0x80) != 0) {
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
          int address_5664;
          address_5664 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5665 = 12 | F & 1;
          _address2721 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5666 = read(_address2721, 0);
          contend1x1(_address2721);
          _value2721 = operand_5666;
          int value3_5669 = nAndCarry_5665;
          _F2722 = value3_5669 & 1;
          value3_5669 = value3_5669 >>> 1;
          _F2722 = (_F2722 & 1) | 0x10 | (address_5664 & 0x28);
          if ((_value2721 & (0x01 << value3_5669)) == 0) {
              _F2722 |= 0x44;
          }
          if (value3_5669 == 7 && (_value2721 & 0x80) != 0) {
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
          int address_5671;
          address_5671 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5672 = 12 | F & 1;
          _address2724 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5673 = read(_address2724, 0);
          contend1x1(_address2724);
          _value2724 = operand_5673;
          int value3_5676 = nAndCarry_5672;
          _F2725 = value3_5676 & 1;
          value3_5676 = value3_5676 >>> 1;
          _F2725 = (_F2725 & 1) | 0x10 | (address_5671 & 0x28);
          if ((_value2724 & (0x01 << value3_5676)) == 0) {
              _F2725 |= 0x44;
          }
          if (value3_5676 == 7 && (_value2724 & 0x80) != 0) {
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
          int address_5678;
          address_5678 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5679 = 12 | F & 1;
          _address2727 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5680 = read(_address2727, 0);
          contend1x1(_address2727);
          _value2727 = operand_5680;
          int value3_5683 = nAndCarry_5679;
          _F2728 = value3_5683 & 1;
          value3_5683 = value3_5683 >>> 1;
          _F2728 = (_F2728 & 1) | 0x10 | (address_5678 & 0x28);
          if ((_value2727 & (0x01 << value3_5683)) == 0) {
              _F2728 |= 0x44;
          }
          if (value3_5683 == 7 && (_value2727 & 0x80) != 0) {
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
          int address_5685;
          address_5685 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5686 = 12 | F & 1;
          _address2730 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5687 = read(_address2730, 0);
          contend1x1(_address2730);
          _value2730 = operand_5687;
          int value3_5690 = nAndCarry_5686;
          _F2731 = value3_5690 & 1;
          value3_5690 = value3_5690 >>> 1;
          _F2731 = (_F2731 & 1) | 0x10 | (address_5685 & 0x28);
          if ((_value2730 & (0x01 << value3_5690)) == 0) {
              _F2731 |= 0x44;
          }
          if (value3_5690 == 7 && (_value2730 & 0x80) != 0) {
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
          int address_5692;
          address_5692 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5693 = 14 | F & 1;
          _address2733 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5694 = read(_address2733, 0);
          contend1x1(_address2733);
          _value2733 = operand_5694;
          int value3_5697 = nAndCarry_5693;
          _F2734 = value3_5697 & 1;
          value3_5697 = value3_5697 >>> 1;
          _F2734 = (_F2734 & 1) | 0x10 | (address_5692 & 0x28);
          if ((_value2733 & (0x01 << value3_5697)) == 0) {
              _F2734 |= 0x44;
          }
          if (value3_5697 == 7 && (_value2733 & 0x80) != 0) {
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
          int address_5699;
          address_5699 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5700 = 14 | F & 1;
          _address2736 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5701 = read(_address2736, 0);
          contend1x1(_address2736);
          _value2736 = operand_5701;
          int value3_5704 = nAndCarry_5700;
          _F2737 = value3_5704 & 1;
          value3_5704 = value3_5704 >>> 1;
          _F2737 = (_F2737 & 1) | 0x10 | (address_5699 & 0x28);
          if ((_value2736 & (0x01 << value3_5704)) == 0) {
              _F2737 |= 0x44;
          }
          if (value3_5704 == 7 && (_value2736 & 0x80) != 0) {
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
          int address_5706;
          address_5706 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5707 = 14 | F & 1;
          _address2739 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5708 = read(_address2739, 0);
          contend1x1(_address2739);
          _value2739 = operand_5708;
          int value3_5711 = nAndCarry_5707;
          _F2740 = value3_5711 & 1;
          value3_5711 = value3_5711 >>> 1;
          _F2740 = (_F2740 & 1) | 0x10 | (address_5706 & 0x28);
          if ((_value2739 & (0x01 << value3_5711)) == 0) {
              _F2740 |= 0x44;
          }
          if (value3_5711 == 7 && (_value2739 & 0x80) != 0) {
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
          int address_5713;
          address_5713 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5714 = 14 | F & 1;
          _address2742 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5715 = read(_address2742, 0);
          contend1x1(_address2742);
          _value2742 = operand_5715;
          int value3_5718 = nAndCarry_5714;
          _F2743 = value3_5718 & 1;
          value3_5718 = value3_5718 >>> 1;
          _F2743 = (_F2743 & 1) | 0x10 | (address_5713 & 0x28);
          if ((_value2742 & (0x01 << value3_5718)) == 0) {
              _F2743 |= 0x44;
          }
          if (value3_5718 == 7 && (_value2742 & 0x80) != 0) {
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
          int address_5720;
          address_5720 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5721 = 14 | F & 1;
          _address2745 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5722 = read(_address2745, 0);
          contend1x1(_address2745);
          _value2745 = operand_5722;
          int value3_5725 = nAndCarry_5721;
          _F2746 = value3_5725 & 1;
          value3_5725 = value3_5725 >>> 1;
          _F2746 = (_F2746 & 1) | 0x10 | (address_5720 & 0x28);
          if ((_value2745 & (0x01 << value3_5725)) == 0) {
              _F2746 |= 0x44;
          }
          if (value3_5725 == 7 && (_value2745 & 0x80) != 0) {
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
          int address_5727;
          address_5727 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5728 = 14 | F & 1;
          _address2748 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5729 = read(_address2748, 0);
          contend1x1(_address2748);
          _value2748 = operand_5729;
          int value3_5732 = nAndCarry_5728;
          _F2749 = value3_5732 & 1;
          value3_5732 = value3_5732 >>> 1;
          _F2749 = (_F2749 & 1) | 0x10 | (address_5727 & 0x28);
          if ((_value2748 & (0x01 << value3_5732)) == 0) {
              _F2749 |= 0x44;
          }
          if (value3_5732 == 7 && (_value2748 & 0x80) != 0) {
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
          int address_5734;
          address_5734 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5735 = 14 | F & 1;
          _address2751 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5736 = read(_address2751, 0);
          contend1x1(_address2751);
          _value2751 = operand_5736;
          int value3_5739 = nAndCarry_5735;
          _F2752 = value3_5739 & 1;
          value3_5739 = value3_5739 >>> 1;
          _F2752 = (_F2752 & 1) | 0x10 | (address_5734 & 0x28);
          if ((_value2751 & (0x01 << value3_5739)) == 0) {
              _F2752 |= 0x44;
          }
          if (value3_5739 == 7 && (_value2751 & 0x80) != 0) {
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
          int address_5741;
          address_5741 = ((IY + (int) ((byte) displacement)) & 0xFFFF) >> 8;
          int nAndCarry_5742 = 14 | F & 1;
          _address2754 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5743 = read(_address2754, 0);
          contend1x1(_address2754);
          _value2754 = operand_5743;
          int value3_5746 = nAndCarry_5742;
          _F2755 = value3_5746 & 1;
          value3_5746 = value3_5746 >>> 1;
          _F2755 = (_F2755 & 1) | 0x10 | (address_5741 & 0x28);
          if ((_value2754 & (0x01 << value3_5746)) == 0) {
              _F2755 |= 0x44;
          }
          if (value3_5746 == 7 && (_value2754 & 0x80) != 0) {
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
          int operand_5748 = read(_address2757, 0);
          contend1x1(_address2757);
          _value2757 = operand_5748;
          int value_5749 = (_value2757 & -2);
          _address2757 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2757 = value_5749;
          write(_address2757, value_5749);
          int read_5750;
          read_5750 = _value2757;
          B = read_5750;
          MEMPTR = _address2757;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x81: {
          int _value2759;
          int _address2759;
          contend2x1((PC + 3) & 0xFFFF);
          _address2759 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5751 = read(_address2759, 0);
          contend1x1(_address2759);
          _value2759 = operand_5751;
          int value_5752 = (_value2759 & -2);
          _address2759 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2759 = value_5752;
          write(_address2759, value_5752);
          int read_5753;
          read_5753 = _value2759;
          C = read_5753;
          MEMPTR = _address2759;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x82: {
          int _value2761;
          int _address2761;
          contend2x1((PC + 3) & 0xFFFF);
          _address2761 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5754 = read(_address2761, 0);
          contend1x1(_address2761);
          _value2761 = operand_5754;
          int value_5755 = (_value2761 & -2);
          _address2761 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2761 = value_5755;
          write(_address2761, value_5755);
          int read_5756;
          read_5756 = _value2761;
          D = read_5756;
          MEMPTR = _address2761;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x83: {
          int _value2763;
          int _address2763;
          contend2x1((PC + 3) & 0xFFFF);
          _address2763 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5757 = read(_address2763, 0);
          contend1x1(_address2763);
          _value2763 = operand_5757;
          int value_5758 = (_value2763 & -2);
          _address2763 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2763 = value_5758;
          write(_address2763, value_5758);
          int read_5759;
          read_5759 = _value2763;
          E = read_5759;
          MEMPTR = _address2763;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x84: {
          int _value2765;
          int _address2765;
          contend2x1((PC + 3) & 0xFFFF);
          _address2765 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5760 = read(_address2765, 0);
          contend1x1(_address2765);
          _value2765 = operand_5760;
          int value_5761 = (_value2765 & -2);
          _address2765 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2765 = value_5761;
          write(_address2765, value_5761);
          int read_5762;
          read_5762 = _value2765;
          H = read_5762;
          MEMPTR = _address2765;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x85: {
          int _value2767;
          int _address2767;
          contend2x1((PC + 3) & 0xFFFF);
          _address2767 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5763 = read(_address2767, 0);
          contend1x1(_address2767);
          _value2767 = operand_5763;
          int value_5764 = (_value2767 & -2);
          _address2767 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2767 = value_5764;
          write(_address2767, value_5764);
          int read_5765;
          read_5765 = _value2767;
          L = read_5765;
          MEMPTR = _address2767;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x86: {
          int _value2769;
          int _address2769;
          contend2x1((PC + 3) & 0xFFFF);
          _address2769 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5766 = read(_address2769, 0);
          contend1x1(_address2769);
          _value2769 = operand_5766;
          int value_5767 = (_value2769 & -2);
          _address2769 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2769 = value_5767;
          write(_address2769, value_5767);
          MEMPTR = _address2769;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x87: {
          int _value2771;
          int _address2771;
          contend2x1((PC + 3) & 0xFFFF);
          _address2771 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5768 = read(_address2771, 0);
          contend1x1(_address2771);
          _value2771 = operand_5768;
          int value_5769 = (_value2771 & -2);
          _address2771 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2771 = value_5769;
          write(_address2771, value_5769);
          int read_5770;
          read_5770 = _value2771;
          A = read_5770;
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
          int operand_5771 = read(_address2773, 0);
          contend1x1(_address2773);
          _value2773 = operand_5771;
          int value_5772 = (_value2773 & -3);
          _address2773 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2773 = value_5772;
          write(_address2773, value_5772);
          int read_5773;
          read_5773 = _value2773;
          B = read_5773;
          MEMPTR = _address2773;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x89: {
          int _value2775;
          int _address2775;
          contend2x1((PC + 3) & 0xFFFF);
          _address2775 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5774 = read(_address2775, 0);
          contend1x1(_address2775);
          _value2775 = operand_5774;
          int value_5775 = (_value2775 & -3);
          _address2775 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2775 = value_5775;
          write(_address2775, value_5775);
          int read_5776;
          read_5776 = _value2775;
          C = read_5776;
          MEMPTR = _address2775;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8A: {
          int _value2777;
          int _address2777;
          contend2x1((PC + 3) & 0xFFFF);
          _address2777 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5777 = read(_address2777, 0);
          contend1x1(_address2777);
          _value2777 = operand_5777;
          int value_5778 = (_value2777 & -3);
          _address2777 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2777 = value_5778;
          write(_address2777, value_5778);
          int read_5779;
          read_5779 = _value2777;
          D = read_5779;
          MEMPTR = _address2777;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8B: {
          int _value2779;
          int _address2779;
          contend2x1((PC + 3) & 0xFFFF);
          _address2779 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5780 = read(_address2779, 0);
          contend1x1(_address2779);
          _value2779 = operand_5780;
          int value_5781 = (_value2779 & -3);
          _address2779 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2779 = value_5781;
          write(_address2779, value_5781);
          int read_5782;
          read_5782 = _value2779;
          E = read_5782;
          MEMPTR = _address2779;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8C: {
          int _value2781;
          int _address2781;
          contend2x1((PC + 3) & 0xFFFF);
          _address2781 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5783 = read(_address2781, 0);
          contend1x1(_address2781);
          _value2781 = operand_5783;
          int value_5784 = (_value2781 & -3);
          _address2781 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2781 = value_5784;
          write(_address2781, value_5784);
          int read_5785;
          read_5785 = _value2781;
          H = read_5785;
          MEMPTR = _address2781;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8D: {
          int _value2783;
          int _address2783;
          contend2x1((PC + 3) & 0xFFFF);
          _address2783 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5786 = read(_address2783, 0);
          contend1x1(_address2783);
          _value2783 = operand_5786;
          int value_5787 = (_value2783 & -3);
          _address2783 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2783 = value_5787;
          write(_address2783, value_5787);
          int read_5788;
          read_5788 = _value2783;
          L = read_5788;
          MEMPTR = _address2783;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8E: {
          int _value2785;
          int _address2785;
          contend2x1((PC + 3) & 0xFFFF);
          _address2785 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5789 = read(_address2785, 0);
          contend1x1(_address2785);
          _value2785 = operand_5789;
          int value_5790 = (_value2785 & -3);
          _address2785 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2785 = value_5790;
          write(_address2785, value_5790);
          MEMPTR = _address2785;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x8F: {
          int _value2787;
          int _address2787;
          contend2x1((PC + 3) & 0xFFFF);
          _address2787 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5791 = read(_address2787, 0);
          contend1x1(_address2787);
          _value2787 = operand_5791;
          int value_5792 = (_value2787 & -3);
          _address2787 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2787 = value_5792;
          write(_address2787, value_5792);
          int read_5793;
          read_5793 = _value2787;
          A = read_5793;
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
          int operand_5794 = read(_address2789, 0);
          contend1x1(_address2789);
          _value2789 = operand_5794;
          int value_5795 = (_value2789 & -5);
          _address2789 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2789 = value_5795;
          write(_address2789, value_5795);
          int read_5796;
          read_5796 = _value2789;
          B = read_5796;
          MEMPTR = _address2789;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x91: {
          int _value2791;
          int _address2791;
          contend2x1((PC + 3) & 0xFFFF);
          _address2791 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5797 = read(_address2791, 0);
          contend1x1(_address2791);
          _value2791 = operand_5797;
          int value_5798 = (_value2791 & -5);
          _address2791 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2791 = value_5798;
          write(_address2791, value_5798);
          int read_5799;
          read_5799 = _value2791;
          C = read_5799;
          MEMPTR = _address2791;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x92: {
          int _value2793;
          int _address2793;
          contend2x1((PC + 3) & 0xFFFF);
          _address2793 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5800 = read(_address2793, 0);
          contend1x1(_address2793);
          _value2793 = operand_5800;
          int value_5801 = (_value2793 & -5);
          _address2793 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2793 = value_5801;
          write(_address2793, value_5801);
          int read_5802;
          read_5802 = _value2793;
          D = read_5802;
          MEMPTR = _address2793;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x93: {
          int _value2795;
          int _address2795;
          contend2x1((PC + 3) & 0xFFFF);
          _address2795 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5803 = read(_address2795, 0);
          contend1x1(_address2795);
          _value2795 = operand_5803;
          int value_5804 = (_value2795 & -5);
          _address2795 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2795 = value_5804;
          write(_address2795, value_5804);
          int read_5805;
          read_5805 = _value2795;
          E = read_5805;
          MEMPTR = _address2795;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x94: {
          int _value2797;
          int _address2797;
          contend2x1((PC + 3) & 0xFFFF);
          _address2797 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5806 = read(_address2797, 0);
          contend1x1(_address2797);
          _value2797 = operand_5806;
          int value_5807 = (_value2797 & -5);
          _address2797 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2797 = value_5807;
          write(_address2797, value_5807);
          int read_5808;
          read_5808 = _value2797;
          H = read_5808;
          MEMPTR = _address2797;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x95: {
          int _value2799;
          int _address2799;
          contend2x1((PC + 3) & 0xFFFF);
          _address2799 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5809 = read(_address2799, 0);
          contend1x1(_address2799);
          _value2799 = operand_5809;
          int value_5810 = (_value2799 & -5);
          _address2799 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2799 = value_5810;
          write(_address2799, value_5810);
          int read_5811;
          read_5811 = _value2799;
          L = read_5811;
          MEMPTR = _address2799;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x96: {
          int _value2801;
          int _address2801;
          contend2x1((PC + 3) & 0xFFFF);
          _address2801 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5812 = read(_address2801, 0);
          contend1x1(_address2801);
          _value2801 = operand_5812;
          int value_5813 = (_value2801 & -5);
          _address2801 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2801 = value_5813;
          write(_address2801, value_5813);
          MEMPTR = _address2801;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x97: {
          int _value2803;
          int _address2803;
          contend2x1((PC + 3) & 0xFFFF);
          _address2803 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5814 = read(_address2803, 0);
          contend1x1(_address2803);
          _value2803 = operand_5814;
          int value_5815 = (_value2803 & -5);
          _address2803 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2803 = value_5815;
          write(_address2803, value_5815);
          int read_5816;
          read_5816 = _value2803;
          A = read_5816;
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
          int operand_5817 = read(_address2805, 0);
          contend1x1(_address2805);
          _value2805 = operand_5817;
          int value_5818 = (_value2805 & -9);
          _address2805 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2805 = value_5818;
          write(_address2805, value_5818);
          int read_5819;
          read_5819 = _value2805;
          B = read_5819;
          MEMPTR = _address2805;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x99: {
          int _value2807;
          int _address2807;
          contend2x1((PC + 3) & 0xFFFF);
          _address2807 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5820 = read(_address2807, 0);
          contend1x1(_address2807);
          _value2807 = operand_5820;
          int value_5821 = (_value2807 & -9);
          _address2807 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2807 = value_5821;
          write(_address2807, value_5821);
          int read_5822;
          read_5822 = _value2807;
          C = read_5822;
          MEMPTR = _address2807;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9A: {
          int _value2809;
          int _address2809;
          contend2x1((PC + 3) & 0xFFFF);
          _address2809 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5823 = read(_address2809, 0);
          contend1x1(_address2809);
          _value2809 = operand_5823;
          int value_5824 = (_value2809 & -9);
          _address2809 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2809 = value_5824;
          write(_address2809, value_5824);
          int read_5825;
          read_5825 = _value2809;
          D = read_5825;
          MEMPTR = _address2809;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9B: {
          int _value2811;
          int _address2811;
          contend2x1((PC + 3) & 0xFFFF);
          _address2811 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5826 = read(_address2811, 0);
          contend1x1(_address2811);
          _value2811 = operand_5826;
          int value_5827 = (_value2811 & -9);
          _address2811 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2811 = value_5827;
          write(_address2811, value_5827);
          int read_5828;
          read_5828 = _value2811;
          E = read_5828;
          MEMPTR = _address2811;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9C: {
          int _value2813;
          int _address2813;
          contend2x1((PC + 3) & 0xFFFF);
          _address2813 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5829 = read(_address2813, 0);
          contend1x1(_address2813);
          _value2813 = operand_5829;
          int value_5830 = (_value2813 & -9);
          _address2813 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2813 = value_5830;
          write(_address2813, value_5830);
          int read_5831;
          read_5831 = _value2813;
          H = read_5831;
          MEMPTR = _address2813;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9D: {
          int _value2815;
          int _address2815;
          contend2x1((PC + 3) & 0xFFFF);
          _address2815 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5832 = read(_address2815, 0);
          contend1x1(_address2815);
          _value2815 = operand_5832;
          int value_5833 = (_value2815 & -9);
          _address2815 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2815 = value_5833;
          write(_address2815, value_5833);
          int read_5834;
          read_5834 = _value2815;
          L = read_5834;
          MEMPTR = _address2815;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9E: {
          int _value2817;
          int _address2817;
          contend2x1((PC + 3) & 0xFFFF);
          _address2817 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5835 = read(_address2817, 0);
          contend1x1(_address2817);
          _value2817 = operand_5835;
          int value_5836 = (_value2817 & -9);
          _address2817 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2817 = value_5836;
          write(_address2817, value_5836);
          MEMPTR = _address2817;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0x9F: {
          int _value2819;
          int _address2819;
          contend2x1((PC + 3) & 0xFFFF);
          _address2819 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5837 = read(_address2819, 0);
          contend1x1(_address2819);
          _value2819 = operand_5837;
          int value_5838 = (_value2819 & -9);
          _address2819 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2819 = value_5838;
          write(_address2819, value_5838);
          int read_5839;
          read_5839 = _value2819;
          A = read_5839;
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
          int operand_5840 = read(_address2821, 0);
          contend1x1(_address2821);
          _value2821 = operand_5840;
          int value_5841 = (_value2821 & -17);
          _address2821 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2821 = value_5841;
          write(_address2821, value_5841);
          int read_5842;
          read_5842 = _value2821;
          B = read_5842;
          MEMPTR = _address2821;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA1: {
          int _value2823;
          int _address2823;
          contend2x1((PC + 3) & 0xFFFF);
          _address2823 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5843 = read(_address2823, 0);
          contend1x1(_address2823);
          _value2823 = operand_5843;
          int value_5844 = (_value2823 & -17);
          _address2823 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2823 = value_5844;
          write(_address2823, value_5844);
          int read_5845;
          read_5845 = _value2823;
          C = read_5845;
          MEMPTR = _address2823;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA2: {
          int _value2825;
          int _address2825;
          contend2x1((PC + 3) & 0xFFFF);
          _address2825 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5846 = read(_address2825, 0);
          contend1x1(_address2825);
          _value2825 = operand_5846;
          int value_5847 = (_value2825 & -17);
          _address2825 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2825 = value_5847;
          write(_address2825, value_5847);
          int read_5848;
          read_5848 = _value2825;
          D = read_5848;
          MEMPTR = _address2825;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA3: {
          int _value2827;
          int _address2827;
          contend2x1((PC + 3) & 0xFFFF);
          _address2827 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5849 = read(_address2827, 0);
          contend1x1(_address2827);
          _value2827 = operand_5849;
          int value_5850 = (_value2827 & -17);
          _address2827 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2827 = value_5850;
          write(_address2827, value_5850);
          int read_5851;
          read_5851 = _value2827;
          E = read_5851;
          MEMPTR = _address2827;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA4: {
          int _value2829;
          int _address2829;
          contend2x1((PC + 3) & 0xFFFF);
          _address2829 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5852 = read(_address2829, 0);
          contend1x1(_address2829);
          _value2829 = operand_5852;
          int value_5853 = (_value2829 & -17);
          _address2829 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2829 = value_5853;
          write(_address2829, value_5853);
          int read_5854;
          read_5854 = _value2829;
          H = read_5854;
          MEMPTR = _address2829;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA5: {
          int _value2831;
          int _address2831;
          contend2x1((PC + 3) & 0xFFFF);
          _address2831 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5855 = read(_address2831, 0);
          contend1x1(_address2831);
          _value2831 = operand_5855;
          int value_5856 = (_value2831 & -17);
          _address2831 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2831 = value_5856;
          write(_address2831, value_5856);
          int read_5857;
          read_5857 = _value2831;
          L = read_5857;
          MEMPTR = _address2831;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA6: {
          int _value2833;
          int _address2833;
          contend2x1((PC + 3) & 0xFFFF);
          _address2833 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5858 = read(_address2833, 0);
          contend1x1(_address2833);
          _value2833 = operand_5858;
          int value_5859 = (_value2833 & -17);
          _address2833 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2833 = value_5859;
          write(_address2833, value_5859);
          MEMPTR = _address2833;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA7: {
          int _value2835;
          int _address2835;
          contend2x1((PC + 3) & 0xFFFF);
          _address2835 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5860 = read(_address2835, 0);
          contend1x1(_address2835);
          _value2835 = operand_5860;
          int value_5861 = (_value2835 & -17);
          _address2835 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2835 = value_5861;
          write(_address2835, value_5861);
          int read_5862;
          read_5862 = _value2835;
          A = read_5862;
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
          int operand_5863 = read(_address2837, 0);
          contend1x1(_address2837);
          _value2837 = operand_5863;
          int value_5864 = (_value2837 & -33);
          _address2837 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2837 = value_5864;
          write(_address2837, value_5864);
          int read_5865;
          read_5865 = _value2837;
          B = read_5865;
          MEMPTR = _address2837;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xA9: {
          int _value2839;
          int _address2839;
          contend2x1((PC + 3) & 0xFFFF);
          _address2839 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5866 = read(_address2839, 0);
          contend1x1(_address2839);
          _value2839 = operand_5866;
          int value_5867 = (_value2839 & -33);
          _address2839 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2839 = value_5867;
          write(_address2839, value_5867);
          int read_5868;
          read_5868 = _value2839;
          C = read_5868;
          MEMPTR = _address2839;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAA: {
          int _value2841;
          int _address2841;
          contend2x1((PC + 3) & 0xFFFF);
          _address2841 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5869 = read(_address2841, 0);
          contend1x1(_address2841);
          _value2841 = operand_5869;
          int value_5870 = (_value2841 & -33);
          _address2841 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2841 = value_5870;
          write(_address2841, value_5870);
          int read_5871;
          read_5871 = _value2841;
          D = read_5871;
          MEMPTR = _address2841;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAB: {
          int _value2843;
          int _address2843;
          contend2x1((PC + 3) & 0xFFFF);
          _address2843 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5872 = read(_address2843, 0);
          contend1x1(_address2843);
          _value2843 = operand_5872;
          int value_5873 = (_value2843 & -33);
          _address2843 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2843 = value_5873;
          write(_address2843, value_5873);
          int read_5874;
          read_5874 = _value2843;
          E = read_5874;
          MEMPTR = _address2843;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAC: {
          int _value2845;
          int _address2845;
          contend2x1((PC + 3) & 0xFFFF);
          _address2845 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5875 = read(_address2845, 0);
          contend1x1(_address2845);
          _value2845 = operand_5875;
          int value_5876 = (_value2845 & -33);
          _address2845 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2845 = value_5876;
          write(_address2845, value_5876);
          int read_5877;
          read_5877 = _value2845;
          H = read_5877;
          MEMPTR = _address2845;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAD: {
          int _value2847;
          int _address2847;
          contend2x1((PC + 3) & 0xFFFF);
          _address2847 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5878 = read(_address2847, 0);
          contend1x1(_address2847);
          _value2847 = operand_5878;
          int value_5879 = (_value2847 & -33);
          _address2847 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2847 = value_5879;
          write(_address2847, value_5879);
          int read_5880;
          read_5880 = _value2847;
          L = read_5880;
          MEMPTR = _address2847;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAE: {
          int _value2849;
          int _address2849;
          contend2x1((PC + 3) & 0xFFFF);
          _address2849 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5881 = read(_address2849, 0);
          contend1x1(_address2849);
          _value2849 = operand_5881;
          int value_5882 = (_value2849 & -33);
          _address2849 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2849 = value_5882;
          write(_address2849, value_5882);
          MEMPTR = _address2849;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xAF: {
          int _value2851;
          int _address2851;
          contend2x1((PC + 3) & 0xFFFF);
          _address2851 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5883 = read(_address2851, 0);
          contend1x1(_address2851);
          _value2851 = operand_5883;
          int value_5884 = (_value2851 & -33);
          _address2851 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2851 = value_5884;
          write(_address2851, value_5884);
          int read_5885;
          read_5885 = _value2851;
          A = read_5885;
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
          int operand_5886 = read(_address2853, 0);
          contend1x1(_address2853);
          _value2853 = operand_5886;
          int value_5887 = (_value2853 & -65);
          _address2853 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2853 = value_5887;
          write(_address2853, value_5887);
          int read_5888;
          read_5888 = _value2853;
          B = read_5888;
          MEMPTR = _address2853;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB1: {
          int _value2855;
          int _address2855;
          contend2x1((PC + 3) & 0xFFFF);
          _address2855 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5889 = read(_address2855, 0);
          contend1x1(_address2855);
          _value2855 = operand_5889;
          int value_5890 = (_value2855 & -65);
          _address2855 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2855 = value_5890;
          write(_address2855, value_5890);
          int read_5891;
          read_5891 = _value2855;
          C = read_5891;
          MEMPTR = _address2855;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB2: {
          int _value2857;
          int _address2857;
          contend2x1((PC + 3) & 0xFFFF);
          _address2857 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5892 = read(_address2857, 0);
          contend1x1(_address2857);
          _value2857 = operand_5892;
          int value_5893 = (_value2857 & -65);
          _address2857 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2857 = value_5893;
          write(_address2857, value_5893);
          int read_5894;
          read_5894 = _value2857;
          D = read_5894;
          MEMPTR = _address2857;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB3: {
          int _value2859;
          int _address2859;
          contend2x1((PC + 3) & 0xFFFF);
          _address2859 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5895 = read(_address2859, 0);
          contend1x1(_address2859);
          _value2859 = operand_5895;
          int value_5896 = (_value2859 & -65);
          _address2859 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2859 = value_5896;
          write(_address2859, value_5896);
          int read_5897;
          read_5897 = _value2859;
          E = read_5897;
          MEMPTR = _address2859;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB4: {
          int _value2861;
          int _address2861;
          contend2x1((PC + 3) & 0xFFFF);
          _address2861 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5898 = read(_address2861, 0);
          contend1x1(_address2861);
          _value2861 = operand_5898;
          int value_5899 = (_value2861 & -65);
          _address2861 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2861 = value_5899;
          write(_address2861, value_5899);
          int read_5900;
          read_5900 = _value2861;
          H = read_5900;
          MEMPTR = _address2861;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB5: {
          int _value2863;
          int _address2863;
          contend2x1((PC + 3) & 0xFFFF);
          _address2863 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5901 = read(_address2863, 0);
          contend1x1(_address2863);
          _value2863 = operand_5901;
          int value_5902 = (_value2863 & -65);
          _address2863 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2863 = value_5902;
          write(_address2863, value_5902);
          int read_5903;
          read_5903 = _value2863;
          L = read_5903;
          MEMPTR = _address2863;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB6: {
          int _value2865;
          int _address2865;
          contend2x1((PC + 3) & 0xFFFF);
          _address2865 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5904 = read(_address2865, 0);
          contend1x1(_address2865);
          _value2865 = operand_5904;
          int value_5905 = (_value2865 & -65);
          _address2865 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2865 = value_5905;
          write(_address2865, value_5905);
          MEMPTR = _address2865;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB7: {
          int _value2867;
          int _address2867;
          contend2x1((PC + 3) & 0xFFFF);
          _address2867 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5906 = read(_address2867, 0);
          contend1x1(_address2867);
          _value2867 = operand_5906;
          int value_5907 = (_value2867 & -65);
          _address2867 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2867 = value_5907;
          write(_address2867, value_5907);
          int read_5908;
          read_5908 = _value2867;
          A = read_5908;
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
          int operand_5909 = read(_address2869, 0);
          contend1x1(_address2869);
          _value2869 = operand_5909;
          int value_5910 = (_value2869 & -129);
          _address2869 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2869 = value_5910;
          write(_address2869, value_5910);
          int read_5911;
          read_5911 = _value2869;
          B = read_5911;
          MEMPTR = _address2869;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xB9: {
          int _value2871;
          int _address2871;
          contend2x1((PC + 3) & 0xFFFF);
          _address2871 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5912 = read(_address2871, 0);
          contend1x1(_address2871);
          _value2871 = operand_5912;
          int value_5913 = (_value2871 & -129);
          _address2871 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2871 = value_5913;
          write(_address2871, value_5913);
          int read_5914;
          read_5914 = _value2871;
          C = read_5914;
          MEMPTR = _address2871;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBA: {
          int _value2873;
          int _address2873;
          contend2x1((PC + 3) & 0xFFFF);
          _address2873 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5915 = read(_address2873, 0);
          contend1x1(_address2873);
          _value2873 = operand_5915;
          int value_5916 = (_value2873 & -129);
          _address2873 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2873 = value_5916;
          write(_address2873, value_5916);
          int read_5917;
          read_5917 = _value2873;
          D = read_5917;
          MEMPTR = _address2873;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBB: {
          int _value2875;
          int _address2875;
          contend2x1((PC + 3) & 0xFFFF);
          _address2875 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5918 = read(_address2875, 0);
          contend1x1(_address2875);
          _value2875 = operand_5918;
          int value_5919 = (_value2875 & -129);
          _address2875 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2875 = value_5919;
          write(_address2875, value_5919);
          int read_5920;
          read_5920 = _value2875;
          E = read_5920;
          MEMPTR = _address2875;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBC: {
          int _value2877;
          int _address2877;
          contend2x1((PC + 3) & 0xFFFF);
          _address2877 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5921 = read(_address2877, 0);
          contend1x1(_address2877);
          _value2877 = operand_5921;
          int value_5922 = (_value2877 & -129);
          _address2877 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2877 = value_5922;
          write(_address2877, value_5922);
          int read_5923;
          read_5923 = _value2877;
          H = read_5923;
          MEMPTR = _address2877;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBD: {
          int _value2879;
          int _address2879;
          contend2x1((PC + 3) & 0xFFFF);
          _address2879 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5924 = read(_address2879, 0);
          contend1x1(_address2879);
          _value2879 = operand_5924;
          int value_5925 = (_value2879 & -129);
          _address2879 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2879 = value_5925;
          write(_address2879, value_5925);
          int read_5926;
          read_5926 = _value2879;
          L = read_5926;
          MEMPTR = _address2879;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBE: {
          int _value2881;
          int _address2881;
          contend2x1((PC + 3) & 0xFFFF);
          _address2881 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5927 = read(_address2881, 0);
          contend1x1(_address2881);
          _value2881 = operand_5927;
          int value_5928 = (_value2881 & -129);
          _address2881 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2881 = value_5928;
          write(_address2881, value_5928);
          MEMPTR = _address2881;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xBF: {
          int _value2883;
          int _address2883;
          contend2x1((PC + 3) & 0xFFFF);
          _address2883 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5929 = read(_address2883, 0);
          contend1x1(_address2883);
          _value2883 = operand_5929;
          int value_5930 = (_value2883 & -129);
          _address2883 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2883 = value_5930;
          write(_address2883, value_5930);
          int read_5931;
          read_5931 = _value2883;
          A = read_5931;
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
          int operand_5932 = read(_address2885, 0);
          contend1x1(_address2885);
          _value2885 = operand_5932;
          int value_5933 = (_value2885 | 1);
          _address2885 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2885 = value_5933;
          write(_address2885, value_5933);
          int read_5934;
          read_5934 = _value2885;
          B = read_5934;
          MEMPTR = _address2885;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC1: {
          int _value2887;
          int _address2887;
          contend2x1((PC + 3) & 0xFFFF);
          _address2887 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5935 = read(_address2887, 0);
          contend1x1(_address2887);
          _value2887 = operand_5935;
          int value_5936 = (_value2887 | 1);
          _address2887 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2887 = value_5936;
          write(_address2887, value_5936);
          int read_5937;
          read_5937 = _value2887;
          C = read_5937;
          MEMPTR = _address2887;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC2: {
          int _value2889;
          int _address2889;
          contend2x1((PC + 3) & 0xFFFF);
          _address2889 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5938 = read(_address2889, 0);
          contend1x1(_address2889);
          _value2889 = operand_5938;
          int value_5939 = (_value2889 | 1);
          _address2889 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2889 = value_5939;
          write(_address2889, value_5939);
          int read_5940;
          read_5940 = _value2889;
          D = read_5940;
          MEMPTR = _address2889;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC3: {
          int _value2891;
          int _address2891;
          contend2x1((PC + 3) & 0xFFFF);
          _address2891 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5941 = read(_address2891, 0);
          contend1x1(_address2891);
          _value2891 = operand_5941;
          int value_5942 = (_value2891 | 1);
          _address2891 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2891 = value_5942;
          write(_address2891, value_5942);
          int read_5943;
          read_5943 = _value2891;
          E = read_5943;
          MEMPTR = _address2891;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC4: {
          int _value2893;
          int _address2893;
          contend2x1((PC + 3) & 0xFFFF);
          _address2893 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5944 = read(_address2893, 0);
          contend1x1(_address2893);
          _value2893 = operand_5944;
          int value_5945 = (_value2893 | 1);
          _address2893 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2893 = value_5945;
          write(_address2893, value_5945);
          int read_5946;
          read_5946 = _value2893;
          H = read_5946;
          MEMPTR = _address2893;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC5: {
          int _value2895;
          int _address2895;
          contend2x1((PC + 3) & 0xFFFF);
          _address2895 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5947 = read(_address2895, 0);
          contend1x1(_address2895);
          _value2895 = operand_5947;
          int value_5948 = (_value2895 | 1);
          _address2895 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2895 = value_5948;
          write(_address2895, value_5948);
          int read_5949;
          read_5949 = _value2895;
          L = read_5949;
          MEMPTR = _address2895;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC6: {
          int _value2897;
          int _address2897;
          contend2x1((PC + 3) & 0xFFFF);
          _address2897 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5950 = read(_address2897, 0);
          contend1x1(_address2897);
          _value2897 = operand_5950;
          int value_5951 = (_value2897 | 1);
          _address2897 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2897 = value_5951;
          write(_address2897, value_5951);
          MEMPTR = _address2897;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC7: {
          int _value2899;
          int _address2899;
          contend2x1((PC + 3) & 0xFFFF);
          _address2899 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5952 = read(_address2899, 0);
          contend1x1(_address2899);
          _value2899 = operand_5952;
          int value_5953 = (_value2899 | 1);
          _address2899 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2899 = value_5953;
          write(_address2899, value_5953);
          int read_5954;
          read_5954 = _value2899;
          A = read_5954;
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
          int operand_5955 = read(_address2901, 0);
          contend1x1(_address2901);
          _value2901 = operand_5955;
          int value_5956 = (_value2901 | 2);
          _address2901 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2901 = value_5956;
          write(_address2901, value_5956);
          int read_5957;
          read_5957 = _value2901;
          B = read_5957;
          MEMPTR = _address2901;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xC9: {
          int _value2903;
          int _address2903;
          contend2x1((PC + 3) & 0xFFFF);
          _address2903 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5958 = read(_address2903, 0);
          contend1x1(_address2903);
          _value2903 = operand_5958;
          int value_5959 = (_value2903 | 2);
          _address2903 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2903 = value_5959;
          write(_address2903, value_5959);
          int read_5960;
          read_5960 = _value2903;
          C = read_5960;
          MEMPTR = _address2903;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCA: {
          int _value2905;
          int _address2905;
          contend2x1((PC + 3) & 0xFFFF);
          _address2905 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5961 = read(_address2905, 0);
          contend1x1(_address2905);
          _value2905 = operand_5961;
          int value_5962 = (_value2905 | 2);
          _address2905 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2905 = value_5962;
          write(_address2905, value_5962);
          int read_5963;
          read_5963 = _value2905;
          D = read_5963;
          MEMPTR = _address2905;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCB: {
          int _value2907;
          int _address2907;
          contend2x1((PC + 3) & 0xFFFF);
          _address2907 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5964 = read(_address2907, 0);
          contend1x1(_address2907);
          _value2907 = operand_5964;
          int value_5965 = (_value2907 | 2);
          _address2907 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2907 = value_5965;
          write(_address2907, value_5965);
          int read_5966;
          read_5966 = _value2907;
          E = read_5966;
          MEMPTR = _address2907;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCC: {
          int _value2909;
          int _address2909;
          contend2x1((PC + 3) & 0xFFFF);
          _address2909 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5967 = read(_address2909, 0);
          contend1x1(_address2909);
          _value2909 = operand_5967;
          int value_5968 = (_value2909 | 2);
          _address2909 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2909 = value_5968;
          write(_address2909, value_5968);
          int read_5969;
          read_5969 = _value2909;
          H = read_5969;
          MEMPTR = _address2909;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCD: {
          int _value2911;
          int _address2911;
          contend2x1((PC + 3) & 0xFFFF);
          _address2911 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5970 = read(_address2911, 0);
          contend1x1(_address2911);
          _value2911 = operand_5970;
          int value_5971 = (_value2911 | 2);
          _address2911 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2911 = value_5971;
          write(_address2911, value_5971);
          int read_5972;
          read_5972 = _value2911;
          L = read_5972;
          MEMPTR = _address2911;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCE: {
          int _value2913;
          int _address2913;
          contend2x1((PC + 3) & 0xFFFF);
          _address2913 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5973 = read(_address2913, 0);
          contend1x1(_address2913);
          _value2913 = operand_5973;
          int value_5974 = (_value2913 | 2);
          _address2913 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2913 = value_5974;
          write(_address2913, value_5974);
          MEMPTR = _address2913;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xCF: {
          int _value2915;
          int _address2915;
          contend2x1((PC + 3) & 0xFFFF);
          _address2915 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5975 = read(_address2915, 0);
          contend1x1(_address2915);
          _value2915 = operand_5975;
          int value_5976 = (_value2915 | 2);
          _address2915 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2915 = value_5976;
          write(_address2915, value_5976);
          int read_5977;
          read_5977 = _value2915;
          A = read_5977;
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
          int operand_5978 = read(_address2917, 0);
          contend1x1(_address2917);
          _value2917 = operand_5978;
          int value_5979 = (_value2917 | 4);
          _address2917 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2917 = value_5979;
          write(_address2917, value_5979);
          int read_5980;
          read_5980 = _value2917;
          B = read_5980;
          MEMPTR = _address2917;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD1: {
          int _value2919;
          int _address2919;
          contend2x1((PC + 3) & 0xFFFF);
          _address2919 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5981 = read(_address2919, 0);
          contend1x1(_address2919);
          _value2919 = operand_5981;
          int value_5982 = (_value2919 | 4);
          _address2919 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2919 = value_5982;
          write(_address2919, value_5982);
          int read_5983;
          read_5983 = _value2919;
          C = read_5983;
          MEMPTR = _address2919;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD2: {
          int _value2921;
          int _address2921;
          contend2x1((PC + 3) & 0xFFFF);
          _address2921 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5984 = read(_address2921, 0);
          contend1x1(_address2921);
          _value2921 = operand_5984;
          int value_5985 = (_value2921 | 4);
          _address2921 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2921 = value_5985;
          write(_address2921, value_5985);
          int read_5986;
          read_5986 = _value2921;
          D = read_5986;
          MEMPTR = _address2921;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD3: {
          int _value2923;
          int _address2923;
          contend2x1((PC + 3) & 0xFFFF);
          _address2923 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5987 = read(_address2923, 0);
          contend1x1(_address2923);
          _value2923 = operand_5987;
          int value_5988 = (_value2923 | 4);
          _address2923 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2923 = value_5988;
          write(_address2923, value_5988);
          int read_5989;
          read_5989 = _value2923;
          E = read_5989;
          MEMPTR = _address2923;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD4: {
          int _value2925;
          int _address2925;
          contend2x1((PC + 3) & 0xFFFF);
          _address2925 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5990 = read(_address2925, 0);
          contend1x1(_address2925);
          _value2925 = operand_5990;
          int value_5991 = (_value2925 | 4);
          _address2925 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2925 = value_5991;
          write(_address2925, value_5991);
          int read_5992;
          read_5992 = _value2925;
          H = read_5992;
          MEMPTR = _address2925;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD5: {
          int _value2927;
          int _address2927;
          contend2x1((PC + 3) & 0xFFFF);
          _address2927 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5993 = read(_address2927, 0);
          contend1x1(_address2927);
          _value2927 = operand_5993;
          int value_5994 = (_value2927 | 4);
          _address2927 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2927 = value_5994;
          write(_address2927, value_5994);
          int read_5995;
          read_5995 = _value2927;
          L = read_5995;
          MEMPTR = _address2927;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD6: {
          int _value2929;
          int _address2929;
          contend2x1((PC + 3) & 0xFFFF);
          _address2929 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5996 = read(_address2929, 0);
          contend1x1(_address2929);
          _value2929 = operand_5996;
          int value_5997 = (_value2929 | 4);
          _address2929 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2929 = value_5997;
          write(_address2929, value_5997);
          MEMPTR = _address2929;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD7: {
          int _value2931;
          int _address2931;
          contend2x1((PC + 3) & 0xFFFF);
          _address2931 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_5998 = read(_address2931, 0);
          contend1x1(_address2931);
          _value2931 = operand_5998;
          int value_5999 = (_value2931 | 4);
          _address2931 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2931 = value_5999;
          write(_address2931, value_5999);
          int read_6000;
          read_6000 = _value2931;
          A = read_6000;
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
          int operand_6001 = read(_address2933, 0);
          contend1x1(_address2933);
          _value2933 = operand_6001;
          int value_6002 = (_value2933 | 8);
          _address2933 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2933 = value_6002;
          write(_address2933, value_6002);
          int read_6003;
          read_6003 = _value2933;
          B = read_6003;
          MEMPTR = _address2933;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xD9: {
          int _value2935;
          int _address2935;
          contend2x1((PC + 3) & 0xFFFF);
          _address2935 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6004 = read(_address2935, 0);
          contend1x1(_address2935);
          _value2935 = operand_6004;
          int value_6005 = (_value2935 | 8);
          _address2935 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2935 = value_6005;
          write(_address2935, value_6005);
          int read_6006;
          read_6006 = _value2935;
          C = read_6006;
          MEMPTR = _address2935;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDA: {
          int _value2937;
          int _address2937;
          contend2x1((PC + 3) & 0xFFFF);
          _address2937 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6007 = read(_address2937, 0);
          contend1x1(_address2937);
          _value2937 = operand_6007;
          int value_6008 = (_value2937 | 8);
          _address2937 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2937 = value_6008;
          write(_address2937, value_6008);
          int read_6009;
          read_6009 = _value2937;
          D = read_6009;
          MEMPTR = _address2937;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDB: {
          int _value2939;
          int _address2939;
          contend2x1((PC + 3) & 0xFFFF);
          _address2939 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6010 = read(_address2939, 0);
          contend1x1(_address2939);
          _value2939 = operand_6010;
          int value_6011 = (_value2939 | 8);
          _address2939 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2939 = value_6011;
          write(_address2939, value_6011);
          int read_6012;
          read_6012 = _value2939;
          E = read_6012;
          MEMPTR = _address2939;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDC: {
          int _value2941;
          int _address2941;
          contend2x1((PC + 3) & 0xFFFF);
          _address2941 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6013 = read(_address2941, 0);
          contend1x1(_address2941);
          _value2941 = operand_6013;
          int value_6014 = (_value2941 | 8);
          _address2941 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2941 = value_6014;
          write(_address2941, value_6014);
          int read_6015;
          read_6015 = _value2941;
          H = read_6015;
          MEMPTR = _address2941;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDD: {
          int _value2943;
          int _address2943;
          contend2x1((PC + 3) & 0xFFFF);
          _address2943 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6016 = read(_address2943, 0);
          contend1x1(_address2943);
          _value2943 = operand_6016;
          int value_6017 = (_value2943 | 8);
          _address2943 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2943 = value_6017;
          write(_address2943, value_6017);
          int read_6018;
          read_6018 = _value2943;
          L = read_6018;
          MEMPTR = _address2943;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDE: {
          int _value2945;
          int _address2945;
          contend2x1((PC + 3) & 0xFFFF);
          _address2945 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6019 = read(_address2945, 0);
          contend1x1(_address2945);
          _value2945 = operand_6019;
          int value_6020 = (_value2945 | 8);
          _address2945 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2945 = value_6020;
          write(_address2945, value_6020);
          MEMPTR = _address2945;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xDF: {
          int _value2947;
          int _address2947;
          contend2x1((PC + 3) & 0xFFFF);
          _address2947 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6021 = read(_address2947, 0);
          contend1x1(_address2947);
          _value2947 = operand_6021;
          int value_6022 = (_value2947 | 8);
          _address2947 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2947 = value_6022;
          write(_address2947, value_6022);
          int read_6023;
          read_6023 = _value2947;
          A = read_6023;
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
          int operand_6024 = read(_address2949, 0);
          contend1x1(_address2949);
          _value2949 = operand_6024;
          int value_6025 = (_value2949 | 0x10);
          _address2949 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2949 = value_6025;
          write(_address2949, value_6025);
          int read_6026;
          read_6026 = _value2949;
          B = read_6026;
          MEMPTR = _address2949;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE1: {
          int _value2951;
          int _address2951;
          contend2x1((PC + 3) & 0xFFFF);
          _address2951 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6027 = read(_address2951, 0);
          contend1x1(_address2951);
          _value2951 = operand_6027;
          int value_6028 = (_value2951 | 0x10);
          _address2951 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2951 = value_6028;
          write(_address2951, value_6028);
          int read_6029;
          read_6029 = _value2951;
          C = read_6029;
          MEMPTR = _address2951;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE2: {
          int _value2953;
          int _address2953;
          contend2x1((PC + 3) & 0xFFFF);
          _address2953 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6030 = read(_address2953, 0);
          contend1x1(_address2953);
          _value2953 = operand_6030;
          int value_6031 = (_value2953 | 0x10);
          _address2953 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2953 = value_6031;
          write(_address2953, value_6031);
          int read_6032;
          read_6032 = _value2953;
          D = read_6032;
          MEMPTR = _address2953;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE3: {
          int _value2955;
          int _address2955;
          contend2x1((PC + 3) & 0xFFFF);
          _address2955 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6033 = read(_address2955, 0);
          contend1x1(_address2955);
          _value2955 = operand_6033;
          int value_6034 = (_value2955 | 0x10);
          _address2955 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2955 = value_6034;
          write(_address2955, value_6034);
          int read_6035;
          read_6035 = _value2955;
          E = read_6035;
          MEMPTR = _address2955;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE4: {
          int _value2957;
          int _address2957;
          contend2x1((PC + 3) & 0xFFFF);
          _address2957 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6036 = read(_address2957, 0);
          contend1x1(_address2957);
          _value2957 = operand_6036;
          int value_6037 = (_value2957 | 0x10);
          _address2957 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2957 = value_6037;
          write(_address2957, value_6037);
          int read_6038;
          read_6038 = _value2957;
          H = read_6038;
          MEMPTR = _address2957;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE5: {
          int _value2959;
          int _address2959;
          contend2x1((PC + 3) & 0xFFFF);
          _address2959 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6039 = read(_address2959, 0);
          contend1x1(_address2959);
          _value2959 = operand_6039;
          int value_6040 = (_value2959 | 0x10);
          _address2959 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2959 = value_6040;
          write(_address2959, value_6040);
          int read_6041;
          read_6041 = _value2959;
          L = read_6041;
          MEMPTR = _address2959;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE6: {
          int _value2961;
          int _address2961;
          contend2x1((PC + 3) & 0xFFFF);
          _address2961 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6042 = read(_address2961, 0);
          contend1x1(_address2961);
          _value2961 = operand_6042;
          int value_6043 = (_value2961 | 0x10);
          _address2961 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2961 = value_6043;
          write(_address2961, value_6043);
          MEMPTR = _address2961;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE7: {
          int _value2963;
          int _address2963;
          contend2x1((PC + 3) & 0xFFFF);
          _address2963 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6044 = read(_address2963, 0);
          contend1x1(_address2963);
          _value2963 = operand_6044;
          int value_6045 = (_value2963 | 0x10);
          _address2963 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2963 = value_6045;
          write(_address2963, value_6045);
          int read_6046;
          read_6046 = _value2963;
          A = read_6046;
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
          int operand_6047 = read(_address2965, 0);
          contend1x1(_address2965);
          _value2965 = operand_6047;
          int value_6048 = (_value2965 | 0x20);
          _address2965 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2965 = value_6048;
          write(_address2965, value_6048);
          int read_6049;
          read_6049 = _value2965;
          B = read_6049;
          MEMPTR = _address2965;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xE9: {
          int _value2967;
          int _address2967;
          contend2x1((PC + 3) & 0xFFFF);
          _address2967 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6050 = read(_address2967, 0);
          contend1x1(_address2967);
          _value2967 = operand_6050;
          int value_6051 = (_value2967 | 0x20);
          _address2967 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2967 = value_6051;
          write(_address2967, value_6051);
          int read_6052;
          read_6052 = _value2967;
          C = read_6052;
          MEMPTR = _address2967;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEA: {
          int _value2969;
          int _address2969;
          contend2x1((PC + 3) & 0xFFFF);
          _address2969 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6053 = read(_address2969, 0);
          contend1x1(_address2969);
          _value2969 = operand_6053;
          int value_6054 = (_value2969 | 0x20);
          _address2969 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2969 = value_6054;
          write(_address2969, value_6054);
          int read_6055;
          read_6055 = _value2969;
          D = read_6055;
          MEMPTR = _address2969;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEB: {
          int _value2971;
          int _address2971;
          contend2x1((PC + 3) & 0xFFFF);
          _address2971 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6056 = read(_address2971, 0);
          contend1x1(_address2971);
          _value2971 = operand_6056;
          int value_6057 = (_value2971 | 0x20);
          _address2971 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2971 = value_6057;
          write(_address2971, value_6057);
          int read_6058;
          read_6058 = _value2971;
          E = read_6058;
          MEMPTR = _address2971;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEC: {
          int _value2973;
          int _address2973;
          contend2x1((PC + 3) & 0xFFFF);
          _address2973 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6059 = read(_address2973, 0);
          contend1x1(_address2973);
          _value2973 = operand_6059;
          int value_6060 = (_value2973 | 0x20);
          _address2973 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2973 = value_6060;
          write(_address2973, value_6060);
          int read_6061;
          read_6061 = _value2973;
          H = read_6061;
          MEMPTR = _address2973;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xED: {
          int _value2975;
          int _address2975;
          contend2x1((PC + 3) & 0xFFFF);
          _address2975 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6062 = read(_address2975, 0);
          contend1x1(_address2975);
          _value2975 = operand_6062;
          int value_6063 = (_value2975 | 0x20);
          _address2975 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2975 = value_6063;
          write(_address2975, value_6063);
          int read_6064;
          read_6064 = _value2975;
          L = read_6064;
          MEMPTR = _address2975;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEE: {
          int _value2977;
          int _address2977;
          contend2x1((PC + 3) & 0xFFFF);
          _address2977 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6065 = read(_address2977, 0);
          contend1x1(_address2977);
          _value2977 = operand_6065;
          int value_6066 = (_value2977 | 0x20);
          _address2977 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2977 = value_6066;
          write(_address2977, value_6066);
          MEMPTR = _address2977;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xEF: {
          int _value2979;
          int _address2979;
          contend2x1((PC + 3) & 0xFFFF);
          _address2979 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6067 = read(_address2979, 0);
          contend1x1(_address2979);
          _value2979 = operand_6067;
          int value_6068 = (_value2979 | 0x20);
          _address2979 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2979 = value_6068;
          write(_address2979, value_6068);
          int read_6069;
          read_6069 = _value2979;
          A = read_6069;
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
          int operand_6070 = read(_address2981, 0);
          contend1x1(_address2981);
          _value2981 = operand_6070;
          int value_6071 = (_value2981 | 0x40);
          _address2981 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2981 = value_6071;
          write(_address2981, value_6071);
          int read_6072;
          read_6072 = _value2981;
          B = read_6072;
          MEMPTR = _address2981;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF1: {
          int _value2983;
          int _address2983;
          contend2x1((PC + 3) & 0xFFFF);
          _address2983 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6073 = read(_address2983, 0);
          contend1x1(_address2983);
          _value2983 = operand_6073;
          int value_6074 = (_value2983 | 0x40);
          _address2983 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2983 = value_6074;
          write(_address2983, value_6074);
          int read_6075;
          read_6075 = _value2983;
          C = read_6075;
          MEMPTR = _address2983;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF2: {
          int _value2985;
          int _address2985;
          contend2x1((PC + 3) & 0xFFFF);
          _address2985 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6076 = read(_address2985, 0);
          contend1x1(_address2985);
          _value2985 = operand_6076;
          int value_6077 = (_value2985 | 0x40);
          _address2985 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2985 = value_6077;
          write(_address2985, value_6077);
          int read_6078;
          read_6078 = _value2985;
          D = read_6078;
          MEMPTR = _address2985;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF3: {
          int _value2987;
          int _address2987;
          contend2x1((PC + 3) & 0xFFFF);
          _address2987 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6079 = read(_address2987, 0);
          contend1x1(_address2987);
          _value2987 = operand_6079;
          int value_6080 = (_value2987 | 0x40);
          _address2987 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2987 = value_6080;
          write(_address2987, value_6080);
          int read_6081;
          read_6081 = _value2987;
          E = read_6081;
          MEMPTR = _address2987;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF4: {
          int _value2989;
          int _address2989;
          contend2x1((PC + 3) & 0xFFFF);
          _address2989 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6082 = read(_address2989, 0);
          contend1x1(_address2989);
          _value2989 = operand_6082;
          int value_6083 = (_value2989 | 0x40);
          _address2989 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2989 = value_6083;
          write(_address2989, value_6083);
          int read_6084;
          read_6084 = _value2989;
          H = read_6084;
          MEMPTR = _address2989;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF5: {
          int _value2991;
          int _address2991;
          contend2x1((PC + 3) & 0xFFFF);
          _address2991 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6085 = read(_address2991, 0);
          contend1x1(_address2991);
          _value2991 = operand_6085;
          int value_6086 = (_value2991 | 0x40);
          _address2991 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2991 = value_6086;
          write(_address2991, value_6086);
          int read_6087;
          read_6087 = _value2991;
          L = read_6087;
          MEMPTR = _address2991;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF6: {
          int _value2993;
          int _address2993;
          contend2x1((PC + 3) & 0xFFFF);
          _address2993 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6088 = read(_address2993, 0);
          contend1x1(_address2993);
          _value2993 = operand_6088;
          int value_6089 = (_value2993 | 0x40);
          _address2993 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2993 = value_6089;
          write(_address2993, value_6089);
          MEMPTR = _address2993;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF7: {
          int _value2995;
          int _address2995;
          contend2x1((PC + 3) & 0xFFFF);
          _address2995 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6090 = read(_address2995, 0);
          contend1x1(_address2995);
          _value2995 = operand_6090;
          int value_6091 = (_value2995 | 0x40);
          _address2995 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2995 = value_6091;
          write(_address2995, value_6091);
          int read_6092;
          read_6092 = _value2995;
          A = read_6092;
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
          int operand_6093 = read(_address2997, 0);
          contend1x1(_address2997);
          _value2997 = operand_6093;
          int value_6094 = (_value2997 | 0x80);
          _address2997 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2997 = value_6094;
          write(_address2997, value_6094);
          int read_6095;
          read_6095 = _value2997;
          B = read_6095;
          MEMPTR = _address2997;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xF9: {
          int _value2999;
          int _address2999;
          contend2x1((PC + 3) & 0xFFFF);
          _address2999 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6096 = read(_address2999, 0);
          contend1x1(_address2999);
          _value2999 = operand_6096;
          int value_6097 = (_value2999 | 0x80);
          _address2999 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value2999 = value_6097;
          write(_address2999, value_6097);
          int read_6098;
          read_6098 = _value2999;
          C = read_6098;
          MEMPTR = _address2999;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFA: {
          int _value3001;
          int _address3001;
          contend2x1((PC + 3) & 0xFFFF);
          _address3001 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6099 = read(_address3001, 0);
          contend1x1(_address3001);
          _value3001 = operand_6099;
          int value_6100 = (_value3001 | 0x80);
          _address3001 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3001 = value_6100;
          write(_address3001, value_6100);
          int read_6101;
          read_6101 = _value3001;
          D = read_6101;
          MEMPTR = _address3001;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFB: {
          int _value3003;
          int _address3003;
          contend2x1((PC + 3) & 0xFFFF);
          _address3003 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6102 = read(_address3003, 0);
          contend1x1(_address3003);
          _value3003 = operand_6102;
          int value_6103 = (_value3003 | 0x80);
          _address3003 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3003 = value_6103;
          write(_address3003, value_6103);
          int read_6104;
          read_6104 = _value3003;
          E = read_6104;
          MEMPTR = _address3003;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFC: {
          int _value3005;
          int _address3005;
          contend2x1((PC + 3) & 0xFFFF);
          _address3005 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6105 = read(_address3005, 0);
          contend1x1(_address3005);
          _value3005 = operand_6105;
          int value_6106 = (_value3005 | 0x80);
          _address3005 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3005 = value_6106;
          write(_address3005, value_6106);
          int read_6107;
          read_6107 = _value3005;
          H = read_6107;
          MEMPTR = _address3005;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFD: {
          int _value3007;
          int _address3007;
          contend2x1((PC + 3) & 0xFFFF);
          _address3007 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6108 = read(_address3007, 0);
          contend1x1(_address3007);
          _value3007 = operand_6108;
          int value_6109 = (_value3007 | 0x80);
          _address3007 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3007 = value_6109;
          write(_address3007, value_6109);
          int read_6110;
          read_6110 = _value3007;
          L = read_6110;
          MEMPTR = _address3007;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFE: {
          int _value3009;
          int _address3009;
          contend2x1((PC + 3) & 0xFFFF);
          _address3009 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6111 = read(_address3009, 0);
          contend1x1(_address3009);
          _value3009 = operand_6111;
          int value_6112 = (_value3009 | 0x80);
          _address3009 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3009 = value_6112;
          write(_address3009, value_6112);
          MEMPTR = _address3009;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      case 0xFF: {
          int _value3011;
          int _address3011;
          contend2x1((PC + 3) & 0xFFFF);
          _address3011 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          int operand_6113 = read(_address3011, 0);
          contend1x1(_address3011);
          _value3011 = operand_6113;
          int value_6114 = (_value3011 | 0x80);
          _address3011 = (IY + (int) ((byte) displacement)) & 0xFFFF;
          _value3011 = value_6114;
          write(_address3011, value_6114);
          int read_6115;
          read_6115 = _value3011;
          A = read_6115;
          MEMPTR = _address3011;
          PC = (PC + 4) & 0xFFFF;
          break;
      }
      default:
        throw new IllegalStateException("undefined opcode " + opcode + " in decodeFDCB");
    }
  }

}
