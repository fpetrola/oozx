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

package com.fpetrola.z80.blocks.references;

import java.util.Objects;

public class ReferenceVersion {
  public final int cycle;
  public final long executionNumber;

  public ReferenceVersion(int cycle, long executionNumber) {
    this.cycle = cycle;
    this.executionNumber = executionNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReferenceVersion that = (ReferenceVersion) o;
    return cycle == that.cycle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cycle);
  }

  @Override
  public String toString() {
    return "ReferenceVersion{" +
        "cycle=" + cycle +
        ", executionNumber=" + executionNumber +
        '}';
  }
}
