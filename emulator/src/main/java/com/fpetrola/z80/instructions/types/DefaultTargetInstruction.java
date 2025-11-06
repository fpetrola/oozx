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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.opcodes.references.OpcodeReference;

public abstract class DefaultTargetInstruction extends AbstractInstruction implements TargetInstruction {
  final protected OpcodeReference target;

  public DefaultTargetInstruction(OpcodeReference target) {
    this.target = target;
  }

  public OpcodeReference getTarget() {
    return target;
  }

  public String toString() {
    return super.toString() + " " + getTarget().getClass().getSimpleName().toString();
  }
}
