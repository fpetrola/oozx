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

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import net.emustudio.cpu.testsuite.CpuRunner;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.plugins.cpu.zilogZ80.CpuImpl;
import net.emustudio.plugins.cpu.zilogZ80.FakeByteDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

public class CpuRunnerImpl extends CpuRunner<CpuImpl> {
  private final CpuImpl cpuImpl;
  private final List<FakeByteDevice> devices;

  public CpuRunnerImpl(CpuImpl cpu, ByteMemoryStub memoryStub, List<FakeByteDevice> devices) {
    super(cpu, memoryStub);
    this.cpuImpl = cpu;
    this.devices = List.copyOf(Objects.requireNonNull(devices));
  }


  @Override
  public void setByte(int address, int value) {
//    ((MyByteMemoryStub) memoryStub).write(address, (byte) value);
    super.setByte(address, value);
  }

  @Override
  public void setRegister(int register, int value) {
    value &= 0xff;

    switch (register) {
      case REG_B:
        cpuImpl.ooz80.getState().getRegister(RegisterName.B).write((Integer) value);
        break;

      case REG_C:
        cpuImpl.ooz80.getState().getRegister(RegisterName.C).write((Integer) value);
        break;

      case REG_D:
        cpuImpl.ooz80.getState().getRegister(RegisterName.D).write((Integer) value);
        break;
      case REG_E:
        cpuImpl.ooz80.getState().getRegister(RegisterName.E).write((Integer) value);
        break;
      case REG_H:
        cpuImpl.ooz80.getState().getRegister(RegisterName.H).write((Integer) value);
        break;
      case REG_L:
        cpuImpl.ooz80.getState().getRegister(RegisterName.L).write((Integer) value);
        break;
      case REG_A:
        cpuImpl.ooz80.getState().getRegister(RegisterName.A).write((Integer) value);
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegister2(int register, int value) {

    switch (register) {
      case REG_B:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Bx).write((Integer) value);
        break;

      case REG_C:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Cx).write((Integer) value);
        break;

      case REG_D:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Dx).write((Integer) value);
        break;
      case REG_E:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Ex).write((Integer) value);
        break;
      case REG_H:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Hx).write((Integer) value);
        break;
      case REG_L:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Lx).write((Integer) value);
        break;
      case REG_A:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Ax).write((Integer) value);
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegisterPair(int registerPair, int value) {
    switch (registerPair) {
      case 0:
        cpuImpl.ooz80.getState().getRegister(RegisterName.BC).write((Integer) value);
        break;
      case 1:
        cpuImpl.ooz80.getState().getRegister(RegisterName.DE).write((Integer) value);
        break;
      case 2:
        cpuImpl.ooz80.getState().getRegister(RegisterName.HL).write((Integer) value);
        break;
      case 3:
        cpuImpl.ooz80.getState().getRegister(RegisterName.SP).write((Integer) value);
        return;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegisterPairPSW(int registerPair, int value) {
    if (registerPair < 3) {
      setRegisterPair(registerPair, value);
    } else if (registerPair == 3) {
      setRegister(REG_A, (value >>> 8) & 0xFF);
      setFlags(value & 0xFF);
    } else {
      throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegisterPair2(int registerPair, int value) {
    switch (registerPair) {
      case 0:
        cpuImpl.ooz80.getState().getRegister(RegisterName.BCx).write((Integer) value);
        break;
      case 1:
        cpuImpl.ooz80.getState().getRegister(RegisterName.DEx).write((Integer) value);
        break;
      case 2:
        cpuImpl.ooz80.getState().getRegister(RegisterName.HLx).write((Integer) value);
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,2> !");
    }
  }

  public boolean getIFF(int index) {
    return cpu.getEngine().IFF[index];
  }

  public FakeByteDevice getDevice(int port) {
    return devices.get(port);
  }

  @Override
  public int getPC() {
    Register pc = cpuImpl.ooz80.getState().getPc();
    return pc.read();
    //return cpu.getEngine().PC;
  }

  @Override
  public int getSP() {
    Register sp = cpuImpl.ooz80.getState().getRegisterSP();
    return sp.read();
  }

  public void setFlags2(int mask) {
    Register flag = cpuImpl.ooz80.getState().getRegister(RegisterName.AFx);
    Integer wordNumber = flag.read();
    flag.write((wordNumber | mask) & 0xFFFF);
  }

  public void resetFlags() {
    Register flag = cpuImpl.ooz80.getState().getFlag();
    flag.write(0);
    //cpu.getEngine().flags = 0;
  }

  public void resetFlags2() {
    Register flag = cpuImpl.ooz80.getState().getRegister(RegisterName.AFx);
    flag.write(0);
  }

  @Override
  public int getFlags() {
    return getFlagsStatic(cpuImpl);
  }

  public static int getFlagsStatic(CpuImpl cpuImpl1) {
    Register flag = cpuImpl1.ooz80.getState().getFlag();
    return flag.read();
  }

  public int getFlags2() {
    return getFlagsStatic2(cpuImpl);
  }

  public static int getFlagsStatic2(CpuImpl cpuImpl1) {
    Register flag = cpuImpl1.ooz80.getState().getRegister(RegisterName.Fx);
    return flag.read();
  }

  @Override
  public void setFlags(int mask) {
    Register flag = cpuImpl.ooz80.getState().getFlag();
    Integer wordNumber = flag.read();
    flag.write((wordNumber | mask) & 0xFFFF);
    //cpu.getEngine().flags |= mask;
  }

  public void setIX(int ix) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.IX).write((Integer) ix);
  }

  public void setI(int value) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.I).write((Integer) value & 0xFF);
  }

  public void setR(int value) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.R).write((Integer)value & 0xFF);
  }

  public void setIY(int iy) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.IY).write(iy);
  }

  public void setSP(int sp) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.SP).write(sp);
  }

  public void enableIFF2() {
    cpu.ooz80.getState().setIff2(true);
  }

  public void disableIFF1() {
    cpu.ooz80.getState().setIff1(false);
  }

  public void setIntMode(byte intMode) {
    cpu.ooz80.getState().setIntMode(State.InterruptionMode.values()[intMode]);
  }

  public static int getRegister(int register, CpuImpl cpuImpl1) {
    Integer result;

    OOZ80 ooz80 = cpuImpl1.ooz80;
    switch (register) {
      case REG_B:
        result = ooz80.getState().getRegister(RegisterName.B).read();
        break;
      case REG_C:
        result = ooz80.getState().getRegister(RegisterName.C).read();
        break;
      case REG_D:
        result = ooz80.getState().getRegister(RegisterName.D).read();
        break;
      case REG_E:
        result = ooz80.getState().getRegister(RegisterName.E).read();
        break;
      case REG_H:
        result = ooz80.getState().getRegister(RegisterName.H).read();
        break;
      case REG_L:
        result = ooz80.getState().getRegister(RegisterName.L).read();
        break;
      case 6:
        result = ooz80.getState().getRegister(RegisterName.F).read();
        break;
      case REG_A:
        result = ooz80.getState().getRegister(RegisterName.A).read();
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }

    return result & 0xff;
  }


  public static int getRegister2(int register, CpuImpl cpuImpl1) {
    Integer result;

    OOZ80 ooz80 = cpuImpl1.ooz80;
    switch (register) {
      case REG_B:
        result = ooz80.getState().getRegister(RegisterName.Bx).read();
        break;
      case REG_C:
        result = ooz80.getState().getRegister(RegisterName.Cx).read();
        break;
      case REG_D:
        result = ooz80.getState().getRegister(RegisterName.Dx).read();
        break;
      case REG_E:
        result = ooz80.getState().getRegister(RegisterName.Ex).read();
        break;
      case REG_H:
        result = ooz80.getState().getRegister(RegisterName.Hx).read();
        break;
      case REG_L:
        result = ooz80.getState().getRegister(RegisterName.Lx).read();
        break;
      case 6:
        result = ooz80.getState().getRegister(RegisterName.Fx).read();
        break;
      case REG_A:
        result = ooz80.getState().getRegister(RegisterName.Ax).read();
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }

    return result & 0xff;
  }

  @Override
  public List<java.lang.Integer> getRegisters() {
    List<java.lang.Integer> registers = new ArrayList<>();
    for (int i = 0; i < 8; i++)
      registers.add(getRegister(i, cpuImpl));

    //registers.add(getRegister(7, cpuImpl));

    return registers;
  }
}
