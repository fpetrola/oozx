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
    WordNumber read = getMemory().read(WordNumber.createValue(memoryPosition));
    return read == null ? 0 : (byte) read.intValue();
  }
}
