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

package com.fpetrola.oozx.speccy.machine;



import com.fpetrola.oozx.speccy.modules.display.Display;

public abstract class AbstractSpectrumMachine implements SpectrumMachine {
  /** The T-state of the first displayed line: every other line is a line's length further on. */
  private long firstLine;
  protected final Paging paging = new Paging();


  public Paging paging() {
    return paging;
  }

  public long lineStart(int line) {
    return firstLine + (long) line * getTimings().tstatesPerLine();
  }

  public void firstLineAt(long tState) {
    firstLine = tState;
  }
}
