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

package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.base.DriverConfigurator;
import com.fpetrola.z80.bytecode.DefaultRegistersSetter;
import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.bytecode.examples.SnapshotHelper;
import com.fpetrola.z80.cpu.MemorySetter;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.InstructionSpy;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class JSWBytecodeCreationTests<T extends WordNumber> {
  private final RealCodeBytecodeCreationBase<T> realCodeBytecodeCreationBase;
  private final RoutinesDriverConfigurator driverConfigurator;

  @Inject
  public JSWBytecodeCreationTests(RoutinesDriverConfigurator driverConfigurator) {
    realCodeBytecodeCreationBase = driverConfigurator.getRealCodeBytecodeCreationBase();
    this.driverConfigurator = driverConfigurator;
  }

  @Ignore
  @Test
  public void testJSWMoveWilly() {
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(35090);

    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "JetSetWilly");
    actual = RemoteZ80Translator.improveSource(actual);
    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();

    Assert.assertEquals("""
        """, actual);
  }

  @Ignore
  @Test
  public void testTranslateWillyToJava() {
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(35090);
    translateToJava("JetSetWilly", base64Memory, "$34762");
  }

  private String getMemoryInBase64FromFile(String url) {
    String first = Helper.getSnapshotFile(url);
    SnapshotLoader.setupStateWithSnapshot(getDefaultRegistersSetter(), first, new MemorySetter(realCodeBytecodeCreationBase.getState().getMemory()));
    return SnapshotHelper.getBase64Memory(realCodeBytecodeCreationBase.getState());
  }

  protected void stepUntilComplete(int startAddress) {
    realCodeBytecodeCreationBase.stepUntilComplete(startAddress);
  }

  public RoutineManager getRoutineManager() {
    return realCodeBytecodeCreationBase.getRoutineManager();
  }

  public String generateAndDecompile() {
    return realCodeBytecodeCreationBase.generateAndDecompile();
  }

  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className) {
    return realCodeBytecodeCreationBase.generateAndDecompile(base64Memory, routines, targetFolder, className);
  }

  public void translateToJava(String className, String memoryInBase64, String startMethod) {
    realCodeBytecodeCreationBase.translateToJava(className, memoryInBase64, startMethod);
  }

  protected DefaultRegistersSetter<T> getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getDefaultRegistersSetter();
  }
}
