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

package com.fpetrola.oozx;

// Supporting classes and enums
public class MemoryPage {
  //  private byte[] page; // The data for this page
  public boolean writable; // Can we write to this data?
  public boolean contended; // Are reads/writes to this page contended?
  public int source; // Where did this page come from?
  boolean saveToSnapshot; // Should this page be saved to snapshots?
  public int pageNum; // Which page from the source
  public int offset; // How far into the page this chunk starts
  /**
   * How much of this chunk is the screen being shown: all of it, none, or the attributes' tail.
   * Decided when the screen page changes, so a write asks one question instead of four.
   */
  public int screenBytes;
  /**
   * Public like the rest of what a page is: the generated core reads it without going through get.
   * Bytes, because that is what they are: as ints a 128K's RAM was half a megabyte, four times
   * what fits in the cache alongside everything else a frame touches.
   */
  public byte[] page;

  public MemoryPage() {
  }

  public int get(final int index) {
    return page[offset + index] & 0xff;
  }

  /** Whether the byte there is already this one: a write that changes nothing dirties nothing. */
  public boolean holds(final int index, final byte value) {
    return page[offset + index] == value;
  }

  public void set(final int index, final byte value) {
    page[offset + index] = value;
  }

  public void setPage(byte[] page) {
    this.page = page;
  }

  public void setPage(byte[][] ram, int i) {
    setPage(ram[i]);
  }

  public int getSource() {
    return source;
  }

  public int getPageNum() {
    return pageNum;
  }

  public byte[] getPage() {
    return page;
  }

  public boolean isWritable() {
    return writable;
  }

  public void setWritable(boolean writable) {
    this.writable = writable;
  }

  public void setPageNum(int pageNum) {
    this.pageNum = pageNum;
  }
}
