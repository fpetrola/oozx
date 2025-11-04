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

package net.emustudio.plugins.cpu.zilogZ80;

import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.memory.Memory;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.runtime.helpers.NumberUtils;

public class MyByteMemoryStub extends ByteMemoryStub {

  private Memory memory;

  public MyByteMemoryStub() {
    super(NumberUtils.Strategy.LITTLE_ENDIAN);
  }

  public void init(Memory memory1) {
    memory = memory1;
    memory1.addMemoryWriteListener(new MemoryWriteListener() {
      public void writtingMemoryAt(int address, int value) {
        MyByteMemoryStub.super.write(address, (byte) value);
      }
    });
  }

  @Override
  public void setMemory(Byte[] memory) {
    super.setMemory(memory);
  }

  @Override
  public void setMemory(byte[] memory) {
    super.setMemory(memory);
  }

  @Override
  public void setMemory(short[] memory) {
    super.setMemory(memory);
    for (int i = 0; i < 0x10000; i++) {
      getMemory().write(i, memory[i]);
    }
  }

  private Memory getMemory() {
    return memory;
  }

  @Override
  public void write(int memoryPosition, Byte value) {
    getMemory().write(memoryPosition, (int) value);
    super.write(memoryPosition, value);
  }

  @Override
  public void write(int memoryPosition, Byte[] values) {
    super.write(memoryPosition, values);
  }

  @Override
  public void write(int memoryPosition, Byte[] cells, int count) {
    super.write(memoryPosition, cells, count);
  }

  @Override
  public Byte read(int memoryPosition) {
    Integer read = getMemory().read(memoryPosition, 0);
    if (read == null) {
      return 0;
    } else {
      return (byte) (int)read;
    }
  }
}
