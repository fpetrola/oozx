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

package com.fpetrola.z80.base;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.MemoryAccessOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("ALL")
public abstract class TwoZ80Driver<T extends WordNumber> extends ContextDriverDelegator<T> {
  protected final IDriverConfigurator<T> driverConfigurator;
  public Z80ContextDriver<T> firstContext;
  public Z80ContextDriver<T> secondContext;
  public RoutineFinderInstructionSpy routineFinderInstructionSpy;

  public TwoZ80Driver(IDriverConfigurator<T> driverConfigurator) {
    super(null);
    this.driverConfigurator = driverConfigurator;
    driverConfigurator.reset();
    routineFinderInstructionSpy = driverConfigurator.getRoutineFinderInstructionSpy();
//    firstContext = driverConfigurator.getFirstContext();
    secondContext = driverConfigurator.getSecondContext();
    useSecond();
  }

  @Before
  public <T2 extends WordNumber> void setUp() {
    setUpMemory();
    driverConfigurator.reset();
  }

  protected void useFirst() {
    currentContext = firstContext;
  }

  protected void useSecond() {
    currentContext = secondContext;
  }

  protected void useBoth() {
    currentContext = new ContextDriverMux<T>(firstContext, secondContext);
  }

  protected <J> J assertTypeAndCast(Class<? extends J> expected, Object i1) {
    assertEquals(expected, i1.getClass());
    J ld1 = (J) i1;
    return ld1;
  }

  public void stepFirst() {
    firstContext.step();
  }

  public void stepSecond() {
    secondContext.step();
  }

  protected abstract void setUpMemory();

  protected OpcodeReference mm(ImmutableOpcodeReference<T> c) {
    return new MemoryAccessOpcodeReference(c, this.mem());
  }

  @Override
  public RoutineFinderInstructionSpy getRegisterTransformerInstructionSpy() {
    return routineFinderInstructionSpy;
  }
}
