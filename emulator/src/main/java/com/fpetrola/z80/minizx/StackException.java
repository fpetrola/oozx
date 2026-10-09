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

package com.fpetrola.z80.minizx;

public class StackException extends RuntimeException {
  public void setNextPC(int nextPC) {
    this.nextPC = nextPC;
  }

  private int nextPC;
  private String poppedInto = "";

  public StackException(int nextPC) {
    this.nextPC = nextPC;
  }

  public StackException(int nextPC, String poppedInto) {
    this.nextPC = nextPC;
    this.poppedInto = poppedInto;
  }

  /** The register a virtual pop loads, for when no translated caller owns the return address it takes. */
  public String getPoppedInto() {
    return poppedInto;
  }

  public int getNextPC() {
    return nextPC;
  }
}
