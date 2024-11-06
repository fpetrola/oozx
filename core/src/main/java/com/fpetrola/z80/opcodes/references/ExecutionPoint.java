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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.instructions.base.Instruction;

import java.util.Objects;

public class ExecutionPoint implements Comparable {
  public long executionNumber;
  public Instruction instruction;
  public int pc;
  public int cycle;

  public ExecutionPoint(long executionNumber, Instruction instruction, int pc) {
    this.executionNumber = executionNumber;
    this.instruction = instruction;
    this.pc = pc;
  }

  @Override
  public String toString() {
    return "ExecutionPoint{" +
        "executionNumber=" + executionNumber +
        ", instruction=" + instruction +
        '}';
  }

  @Override
  public int compareTo(Object o) {
    return (int) (executionNumber - ((ExecutionPoint) o).executionNumber);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExecutionPoint that = (ExecutionPoint) o;
    return executionNumber == that.executionNumber;
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionNumber);
  }

  public void setCycle(int cycle) {
    this.cycle = cycle;
  }
}
