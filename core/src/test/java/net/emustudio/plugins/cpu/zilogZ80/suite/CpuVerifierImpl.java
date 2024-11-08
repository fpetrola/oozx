/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.emustudio.plugins.cpu.zilogZ80.suite;

import com.fpetrola.z80.registers.RegisterName;
import net.emustudio.cpu.testsuite.CpuVerifier;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.plugins.cpu.zilogZ80.CpuImpl;
import net.emustudio.plugins.cpu.zilogZ80.FakeByteDevice;

import java.util.List;
import java.util.Objects;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static org.junit.Assert.*;

public class CpuVerifierImpl extends CpuVerifier {
  private final CpuImpl cpu;
  private final List<FakeByteDevice> devices;

  public CpuVerifierImpl(CpuImpl cpu, ByteMemoryStub memoryStub, List<FakeByteDevice> devices) {
    super(memoryStub);
    this.cpu = Objects.requireNonNull(cpu);
    this.devices = List.copyOf(Objects.requireNonNull(devices));
  }

  public static String intToFlags(int flags) {
    String flagsString = "";
    if ((flags & FLAG_S) == FLAG_S) {
      flagsString += "S";
    }
    if ((flags & FLAG_Z) == FLAG_Z) {
      flagsString += "Z";
    }
    if ((flags & FLAG_Y) == FLAG_Y) {
      flagsString += "Y";
    }
    if ((flags & FLAG_H) == FLAG_H) {
      flagsString += "H";
    }
    if ((flags & FLAG_X) == FLAG_X) {
      flagsString += "X";
    }
    if ((flags & FLAG_PV) == FLAG_PV) {
      flagsString += "P";
    }
    if ((flags & FLAG_N) == FLAG_N) {
      flagsString += "N";
    }
    if ((flags & FLAG_C) == FLAG_C) {
      flagsString += "C";
    }
    return flagsString;
  }

  public void checkRegister(int register, int value) {
    value &= 0xFF;
    int register1 = CpuRunnerImpl.getRegister(register, cpu);
    assertEquals(
        String.format("Expected reg[%02x]=%02x, but was %02x", register, value, register1),
        value, register1
    );
  }

  public void checkRegisterPair(int registerPair, int value) {
    value &= 0xFFFF;
    int realValue;

    switch (registerPair) {
      case 0:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.BC).read().intValue();
        break;
      case 1:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.DE).read().intValue();
        break;
      case 2:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.HL).read().intValue();
        break;
      case 3:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.SP).read().intValue();
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }

    assertEquals(
        String.format("Expected regPair[%02x]=%04x, but was %04x", registerPair, value, realValue),
        value, realValue
    );
  }

  public void checkRegisterPair2(int registerPair, int value) {
    value &= 0xFFFF;
    int realValue;

    switch (registerPair) {
      case 0:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.BCx).read().intValue();
        break;
      case 1:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.DEx).read().intValue();
        break;
      case 2:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.HLx).read().intValue();
        break;
      case 3:
        realValue = cpu.ooz80.getState().getRegister(RegisterName.SP).read().intValue();
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }

    assertEquals(
        String.format("Expected regPair2[%02x]=%04x, but was %04x", registerPair, value, realValue),
        value, realValue
    );
  }

  public void checkIX(int value) {
    value &= 0xFFFF;

    int ix = cpu.ooz80.getState().getRegister(RegisterName.IX).read().intValue();
    assertEquals(
        String.format("Expected IX=%x, but was %x", value, ix),
        value, ix
    );
  }

  public void checkIY(int value) {
    value &= 0xFFFF;
    int iy = cpu.ooz80.getState().getRegister(RegisterName.IY).read().intValue();

    assertEquals(
        String.format("Expected IY=%x, but was %x", value, iy),
        value, iy
    );
  }

  public void checkRegisterPairPSW(int registerPair, int value) {
    if (registerPair < 3) {
      checkRegisterPair(registerPair, value);
    } else if (registerPair == 3) {
      int realValue = (CpuRunnerImpl.getRegister(REG_A, cpu) << 8) | CpuRunnerImpl.getFlagsStatic(cpu);
      assertEquals(
          String.format("Expected regPair[%02x]=%04x, but was %04x", registerPair, value, realValue),
          value, realValue
      );
    } else {
      throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void checkPC(int PC) {
    int pc = cpu.ooz80.getState().getPc().read().intValue();
    assertEquals(
        String.format("Expected PC=%04x, but was %04x", PC, pc),
        PC, pc
    );
  }

  public void checkInterruptsAreEnabled(int set) {
    if (set == 0)
      assertTrue(cpu.ooz80.getState().isIff1());
    else
      assertTrue(cpu.ooz80.getState().isIff2());
  }

  public void checkInterruptsAreDisabled(int set) {
    if (set == 0)
      assertFalse(cpu.ooz80.getState().isIff1());
    else
      assertFalse(cpu.ooz80.getState().isIff2());
  }

  public void checkIntMode(int mode) {
    assertEquals(mode, cpu.ooz80.getState().getInterruptionMode().ordinal());
  }

  @Override
  public void checkFlags(int mask) {
    int flags = CpuRunnerImpl.getFlagsStatic(cpu);
    assertEquals(String.format("Expected flags=%s, but was %s",
        intToFlags(mask), intToFlags(flags)), mask, (flags & mask));
  }

  @Override
  public void checkNotFlags(int mask) {
    int flags = CpuRunnerImpl.getFlagsStatic(cpu);
    assertEquals(String.format("Expected NOT flags=%s, but was %s",
        intToFlags(mask), intToFlags(flags)), 0, (flags & mask));
  }

  public void checkI(int value) {
    int i = cpu.ooz80.getState().getRegister(RegisterName.I).read().intValue();
    assertEquals(
        String.format("Expected I=%02x, but was %02x", value, i),
        value, i
    );
  }

  public void checkR(int value) {
    int r = cpu.ooz80.getState().getRegister(RegisterName.R).read().intValue();
    assertEquals(
        String.format("Expected R=%02x, but was %02x", value, r),
        value, r
    );
  }

  public void checkAF(int value) {

    int af = (CpuRunnerImpl.getRegister(REG_A, cpu) << 8) | CpuRunnerImpl.getFlagsStatic(cpu);

    assertEquals(
        String.format("Expected AF=%04x, but was %04x", value, af),
        value, af
    );
  }


  public void checkAF2(int value) {
    int af = (CpuRunnerImpl.getRegister2(REG_A, cpu) << 8) | CpuRunnerImpl.getFlagsStatic2(cpu);

    assertEquals(
        String.format("Expected AF2=%04x, but was %04x", value, af),
        value, af
    );
  }

  public void checkDeviceValue(int port, int expected) {
    int value = devices.get(port & 0xFF).getValue() & 0xFF;
    assertEquals(
        String.format("Expected device[%02x]=%02x, but was %02x", port, expected, value),
        expected, value
    );
  }
}
