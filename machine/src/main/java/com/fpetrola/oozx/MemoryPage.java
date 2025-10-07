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

package com.fpetrola.oozx;

import org.checkerframework.checker.units.qual.A;

// Supporting classes and enums
public class MemoryPage {
//  private byte[] page; // The data for this page
  boolean writable; // Can we write to this data?
  boolean contended; // Are reads/writes to this page contended?
  int source; // Where did this page come from?
  boolean saveToSnapshot; // Should this page be saved to snapshots?
  int pageNum; // Which page from the source
  int offset; // How far into the page this chunk starts
  private ArrayPointer arrayPointer;

  public MemoryPage() {
  }

  public ArrayPointer getPage() {
    return arrayPointer;
  }

  public void setPage(byte[] page) {
//    this.page = page;
    arrayPointer = new ArrayPointer(page);
  }

  public void setPage(byte[][] ram, int i, int j) {
    arrayPointer = new ArrayPointer(ram, i, j);
  }
}
