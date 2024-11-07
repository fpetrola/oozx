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

public abstract class DefaultWordNumberOperation implements WordNumberOperation {
  protected TraceableWordNumber traceableWordNumber;

  @Override
  public int getI() {
    return i;
  }

  public int i;

  @Override
  public ExecutionPoint getExecutionPoint() {
    return executionPoint;
  }

  private ExecutionPoint executionPoint;

  public DefaultWordNumberOperation(TraceableWordNumber traceableWordNumber, int i) {
    this.traceableWordNumber = traceableWordNumber;
    this.i = i;
  }

  @Override
  public void setExecutionPoint(ExecutionPoint executionPoint) {
    this.executionPoint = executionPoint;
  }

  public abstract int execute();

  public String toString() {
    return getClass().getSimpleName() + ": " + i;
  }
}
