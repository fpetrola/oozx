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

package com.fpetrola.oozx.speccy.modules.memory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * MemoryBus caching, per 2K page, which range covers it and its base offset, invalidated only via {@link #changed}.
 */
@Singleton
public final class DecodedMemoryBus extends MemoryBus {
  private static final int PAGE = 11;
  private static final int MASK = (1 << PAGE) - 1;

  /** Cached per-page view of a range: reused across remaps so covering a page never allocates. */
  public static final class Covering {
    public MappedMemory range;
    public MemoryPart memory;
    public int base;
    public boolean contended;
    public byte[] bytes;

    void cover(MappedMemory range, int page) {
      this.range = range;
      memory = range.memory();
      base = range.offset(page << PAGE);
      contended = memory.contended;
      bytes = memory instanceof Storage held ? held.bytes : null;
    }
  }

  private final Covering[] reading = new Covering[1 << (16 - PAGE)];
  private final Covering[] writing = new Covering[1 << (16 - PAGE)];

  @Inject
  public DecodedMemoryBus() {
    for (int page = 0; page < reading.length; page++) {
      reading[page] = new Covering();
      writing[page] = new Covering();
    }
  }

  @Override
  public MappedMemory reading(int address) {
    return reading[address >>> PAGE].range;
  }

  @Override
  public MappedMemory writing(int address) {
    return writing[address >>> PAGE].range;
  }

  public Covering readingAt(int address) {
    return reading[address >>> PAGE];
  }

  public Covering writingAt(int address) {
    return writing[address >>> PAGE];
  }

  @Override
  public boolean contended(int address) {
    return readingAt(address).contended;
  }

  @Override
  public int read(int address) {
    Covering covering = readingAt(address);
    if (covering.contended) {
      contention.beforeRead();
    }
    return peek(covering, address);
  }

  @Override
  public int peek(int address) {
    return peek(readingAt(address), address);
  }

  private static int peek(Covering covering, int address) {
    int value;
    if (covering.bytes != null) {
      value = covering.bytes[covering.base + (address & MASK)] & 0xff;
    } else {
      value = covering.memory.read(covering.base + (address & MASK));
    }
    return value;
  }

  @Override
  public void write(int address, byte value) {
    Covering covering = writingAt(address);
    if (covering.contended) {
      contention.beforeWrite();
    }
    covering.memory.write(covering.base + (address & MASK), value);
  }

  @Override
  public void poke(int address, byte value) {
    Covering covering = writingAt(address);
    covering.memory.write(covering.base + (address & MASK), value);
  }

  /** Recomputes covering only for the pages the change spans, leaving the rest cached. */
  @Override
  protected void changed(int address, int length) {
    for (int page = address >>> PAGE; page < (address + length + MASK) >>> PAGE; page++) {
      MappedMemory reader = super.reading(page << PAGE), writer = super.writing(page << PAGE);
      if (reader != null) reading[page].cover(reader, page);
      if (writer != null) writing[page].cover(writer, page);
    }
  }
}
