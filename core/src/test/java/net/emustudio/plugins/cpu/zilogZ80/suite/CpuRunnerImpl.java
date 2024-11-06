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
package net.emustudio.plugins.cpu.zilogZ80.suite;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
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
        cpuImpl.ooz80.getState().getRegister(RegisterName.B).write(WordNumber.createValue(value));
        break;

      case REG_C:
        cpuImpl.ooz80.getState().getRegister(RegisterName.C).write(WordNumber.createValue(value));
        break;

      case REG_D:
        cpuImpl.ooz80.getState().getRegister(RegisterName.D).write(WordNumber.createValue(value));
        break;
      case REG_E:
        cpuImpl.ooz80.getState().getRegister(RegisterName.E).write(WordNumber.createValue(value));
        break;
      case REG_H:
        cpuImpl.ooz80.getState().getRegister(RegisterName.H).write(WordNumber.createValue(value));
        break;
      case REG_L:
        cpuImpl.ooz80.getState().getRegister(RegisterName.L).write(WordNumber.createValue(value));
        break;
      case REG_A:
        cpuImpl.ooz80.getState().getRegister(RegisterName.A).write(WordNumber.createValue(value));
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegister2(int register, int value) {

    switch (register) {
      case REG_B:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Bx).write(WordNumber.createValue(value));
        break;

      case REG_C:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Cx).write(WordNumber.createValue(value));
        break;

      case REG_D:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Dx).write(WordNumber.createValue(value));
        break;
      case REG_E:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Ex).write(WordNumber.createValue(value));
        break;
      case REG_H:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Hx).write(WordNumber.createValue(value));
        break;
      case REG_L:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Lx).write(WordNumber.createValue(value));
        break;
      case REG_A:
        cpuImpl.ooz80.getState().getRegister(RegisterName.Ax).write(WordNumber.createValue(value));
        break;
      default:
        throw new IllegalArgumentException("Expected value between <0,3> !");
    }
  }

  public void setRegisterPair(int registerPair, int value) {
    switch (registerPair) {
      case 0:
        cpuImpl.ooz80.getState().getRegister(RegisterName.BC).write(WordNumber.createValue(value));
        break;
      case 1:
        cpuImpl.ooz80.getState().getRegister(RegisterName.DE).write(WordNumber.createValue(value));
        break;
      case 2:
        cpuImpl.ooz80.getState().getRegister(RegisterName.HL).write(WordNumber.createValue(value));
        break;
      case 3:
        cpuImpl.ooz80.getState().getRegister(RegisterName.SP).write(WordNumber.createValue(value));
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
        cpuImpl.ooz80.getState().getRegister(RegisterName.BCx).write(WordNumber.createValue(value));
        break;
      case 1:
        cpuImpl.ooz80.getState().getRegister(RegisterName.DEx).write(WordNumber.createValue(value));
        break;
      case 2:
        cpuImpl.ooz80.getState().getRegister(RegisterName.HLx).write(WordNumber.createValue(value));
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
    Register<WordNumber> pc = cpuImpl.ooz80.getState().getPc();
    return pc.read().intValue();
    //return cpu.getEngine().PC;
  }

  @Override
  public int getSP() {
    Register<WordNumber> sp = cpuImpl.ooz80.getState().getRegisterSP();
    return sp.read().intValue();
  }

  public void setFlags2(int mask) {
    Register<WordNumber> flag = cpuImpl.ooz80.getState().getRegister(RegisterName.AFx);
    flag.write(flag.read().or(mask));
  }

  public void resetFlags() {
    Register<WordNumber> flag = cpuImpl.ooz80.getState().getFlag();
    flag.write(WordNumber.createValue(0));
    //cpu.getEngine().flags = 0;
  }

  public void resetFlags2() {
    Register<WordNumber> flag = cpuImpl.ooz80.getState().getRegister(RegisterName.AFx);
    flag.write(WordNumber.createValue(0));
  }

  @Override
  public int getFlags() {
    return getFlagsStatic(cpuImpl);
  }

  public static int getFlagsStatic(CpuImpl cpuImpl1) {
    Register<WordNumber> flag = cpuImpl1.ooz80.getState().getFlag();
    return flag.read().intValue();
  }

  public int getFlags2() {
    return getFlagsStatic2(cpuImpl);
  }

  public static int getFlagsStatic2(CpuImpl cpuImpl1) {
    Register<WordNumber> flag = cpuImpl1.ooz80.getState().getRegister(RegisterName.Fx);
    return flag.read().intValue();
  }

  @Override
  public void setFlags(int mask) {
    Register<WordNumber> flag = cpuImpl.ooz80.getState().getFlag();
    flag.write(flag.read().or(mask));
    //cpu.getEngine().flags |= mask;
  }

  public void setIX(int ix) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.IX).write(WordNumber.createValue(ix));
  }

  public void setI(int value) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.I).write(WordNumber.createValue(value & 0xFF));
  }

  public void setR(int value) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.R).write(WordNumber.createValue(value & 0xFF));
  }

  public void setIY(int iy) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.IY).write(WordNumber.createValue(iy));
  }

  public void setSP(int sp) {
    cpuImpl.ooz80.getState().getRegister(RegisterName.SP).write(WordNumber.createValue(sp));
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
    WordNumber result;

    OOZ80<WordNumber> ooz80 = cpuImpl1.ooz80;
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

    return result.intValue() & 0xff;
  }


  public static int getRegister2(int register, CpuImpl cpuImpl1) {
    WordNumber result;

    OOZ80<WordNumber> ooz80 = cpuImpl1.ooz80;
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

    return result.intValue() & 0xff;
  }

  @Override
  public List<Integer> getRegisters() {
    List<Integer> registers = new ArrayList<>();
    for (int i = 0; i < 8; i++)
      registers.add(getRegister(i, cpuImpl));

    //registers.add(getRegister(7, cpuImpl));

    return registers;
  }
}
