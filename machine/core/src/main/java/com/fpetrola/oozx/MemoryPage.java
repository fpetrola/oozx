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
  private boolean writable; // Can we write to this data?
  public boolean contended; // Are reads/writes to this page contended?
  public int source; // Where did this page come from?
  boolean saveToSnapshot; // Should this page be saved to snapshots?
  private int pageNum; // Which page from the source
  public int offset; // How far into the page this chunk starts
  private int[] page;

  public MemoryPage() {
  }

  public int get(final int index) {
    return page[offset + index];
  }

  public void set(final int index, final byte value) {
    page[offset + index] = value & 0xff;
  }

  public void setPage(int[] page) {
    this.page = page;
  }

  public void setPage(int[][] ram, int i) {
    setPage(ram[i]);
  }

  public int getSource() {
    return source;
  }

  public int getPageNum() {
    return pageNum;
  }

  public int[] getPage() {
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
