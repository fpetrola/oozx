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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlockRelation {
  private int sourceAddress;
  private int targetAddress;

  private List<ReferenceVersion> versions = new ArrayList<>();

  public BlockRelation(int sourceAddress, int targetAddress) {
    this.sourceAddress = sourceAddress;
    this.targetAddress = targetAddress;
  }

  public static BlockRelation createBlockRelation(int sourceAddress, int targetAddress) {
    return new BlockRelation(sourceAddress, targetAddress);
  }

  public int getSourceAddress() {
    return sourceAddress;
  }

  public int getTargetAddress() {
    return targetAddress;
  }

  public void addInCycle(int cycle, long executionNumber) {
    if (cycle > 0)
      versions.add(new ReferenceVersion(cycle, executionNumber));
  }

  public List<ReferenceVersion> getVersions() {
    return versions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BlockRelation that = (BlockRelation) o;
    return Objects.equals(sourceAddress, that.sourceAddress) && Objects.equals(targetAddress, that.targetAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceAddress, targetAddress);
  }
}
