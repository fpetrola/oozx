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
import com.fpetrola.z80.opcodes.references.WordNumber;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.runtime.helpers.NumberUtils;

public class MyByteMemoryStub extends ByteMemoryStub {

  private Memory<WordNumber> memory;

  public MyByteMemoryStub() {
    super(NumberUtils.Strategy.LITTLE_ENDIAN);
  }

  public void init(Memory<WordNumber> memory1) {
    memory = memory1;
    memory1.addMemoryWriteListener(new MemoryWriteListener<WordNumber>() {
      public void writtingMemoryAt(WordNumber address, WordNumber value) {
        MyByteMemoryStub.super.write(address.intValue(), (byte) value.intValue());
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
    for (int i = 0; i < memory.length; i++) {
      getMemory().write(WordNumber.createValue(i), WordNumber.createValue(memory[i]));
    }
  }

  private Memory<WordNumber> getMemory() {
    return memory;
  }

  @Override
  public void write(int memoryPosition, Byte value) {
    getMemory().write(WordNumber.createValue(memoryPosition), WordNumber.createValue(value));
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
    WordNumber read = getMemory().read(WordNumber.createValue(memoryPosition), 0);
    return read == null ? 0 : (byte) read.intValue();
  }
}
