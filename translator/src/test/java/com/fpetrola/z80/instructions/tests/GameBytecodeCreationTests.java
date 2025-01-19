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

import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.bytecode.examples.SnapshotHelper;
import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.StackAnalyzer;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static com.fpetrola.z80.helpers.Helper.createMD5;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class GameBytecodeCreationTests<T extends WordNumber> {
  private final RealCodeBytecodeCreationBase<T> realCodeBytecodeCreationBase;
  private final RoutinesDriverConfigurator driverConfigurator;

  @Before
  public void setUp() throws Exception {
    Helper.hex = true;
  }

  @After
  public void tearDown() throws Exception {
    Helper.hex = false;
  }

  @Inject
  public GameBytecodeCreationTests(RoutinesDriverConfigurator driverConfigurator) {
    realCodeBytecodeCreationBase = driverConfigurator.getRealCodeBytecodeCreationBase();
    this.driverConfigurator = driverConfigurator;
  }

  @Ignore
  @Test
  public void testTranslateWallyToJava() {
    int address = 0x8184;
    int emulateUntil= 4000;
    EmulatedMiniZX.rzxFile= "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
    StackAnalyzer.collecting= true;
    String memoryInBase64FromFile = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "http://torinak.com/qaop/bin/wally");
    StackAnalyzer.collecting= false;
    realCodeBytecodeCreationBase.getStackAnalyzer().reset(realCodeBytecodeCreationBase.getState());
    testTranslateGame(memoryInBase64FromFile, 0x8185);
  }

  @Ignore
  @Test
  public void testTranslateSamCruiseToJava() {
    testTranslateGame(getMemoryInBase64FromFile("file:///home/fernando/Downloads/samcruise.z80"), 61483);
  }

  @Ignore
  @Test
  public void testTranslateEmlynToJava() {
    testTranslateGame(getMemoryInBase64FromFile("file:////home/fernando/detodo/desarrollo/m/zx/zx/emlyn.z80"), 0xb542);
  }

  private void testTranslateGame(String MemoryInBase64FromFile, int startAddress) {
    Helper.hex = true;
    String base64Memory = MemoryInBase64FromFile;
    stepUntilComplete(startAddress);
//    translateToJava("ZxGame1", base64Memory, "$61483");

    List<Routine> routines = getRoutineManager().getRoutines();
    String actual = generateAndDecompile(base64Memory, routines, ".", "ZxGame1");

    String routinesString = getRoutinesString(routines);

    Assert.assertEquals(""" 
        """, actual);
  }

  @Test
  public void testTranslateDynamite() {
    EmulatedMiniZX.rzxFile= "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx";
    StackAnalyzer.collecting= true;
    int emulateUntil = 0xC804;
    emulateUntil= 40000;
    String base64Memory = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "http://torinak.com/qaop/bin/dynamitedan");
    StackAnalyzer.collecting= false;
    realCodeBytecodeCreationBase.getStackAnalyzer().reset(realCodeBytecodeCreationBase.getState());
    stepUntilComplete(0xC804);

//    translateToJava("ZxGame1", base64Memory, "$C804");
    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "ZxGame1");
    actual = RemoteZ80Translator.improveSource(actual);

    Assert.assertEquals("", actual);
    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();

    Assert.assertEquals("316054b2f1a45816f1410048afd34a77", createMD5(actual));
  }

  @Ignore
  @Test
  public void testTranslateWillyToJava() {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(34762);
    translateToJava("JetSetWilly", base64Memory, "$34762");
  }

  @Test
  public void testWillyCheckingRoutines() {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(35090);

    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "JetSetWilly");
    actual = RemoteZ80Translator.improveSource(actual);

//    Assert.assertEquals("", actual);
    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();

    Assert.assertEquals("b58b3dee93626aa2e7598615f79bfd25", createMD5(actual));

    String routinesString = getRoutinesString(routines);

    Assert.assertEquals("""
        {34762:38136} -> [34762 : 35210, 35245 : 35562, 35591 : 36146, 37048 : 37055, 38043 : 38045, 38061 : 38063, 38095 : 38097, 38134 : 38136]
        {35211:35244} -> [35211 : 35244]
        {35563:35590} -> [35563 : 35590]
        {36147:36170} -> [36147 : 36170]
        {36171:36202} -> [36171 : 36202]
        {36203:36287} -> [36203 : 36287]
        {36288:36306} -> [36288 : 36306]
        {36307:38132} -> [36307 : 36507, 36528 : 37045, 38026 : 38041, 38046 : 38059, 38098 : 38132]
        {36508:36527} -> [36508 : 36527]
        {37056:37309} -> [37056 : 37309]
        {37310:37818} -> [37310 : 37818]
        {37841:37973} -> [37841 : 37973]
        {37974:38025} -> [37974 : 38025]
        {38064:38093} -> [38064 : 38093]
        {38137:38195} -> [38137 : 38195]
        {38196:38343} -> [38196 : 38275, 38298 : 38343]
        {38276:38297} -> [38276 : 38297]
        {38344:38503} -> [38344 : 38429, 38455 : 38503]
        {38430:38454} -> [38430 : 38454]
        {38504:38527} -> [38504 : 38527]
        {38528:38544} -> [38528 : 38544]
        {38545:38554} -> [38545 : 38554]
        {38555:38561} -> [38555 : 38561]
        {38562:38600} -> [38562 : 38600]
        {38601:38621} -> [38601 : 38621]
        {38622:38643} -> [38622 : 38643]
        """, routinesString);
  }

  private String getRoutinesString(List<Routine> routines) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out);

    routines.forEach(r -> {
      printStream.println(r);
    });

    String string = out.toString();
    return string;
  }

  private String getMemoryInBase64FromFile(String url) {
    String first = Helper.getSnapshotFile(url);
    State<T> state = realCodeBytecodeCreationBase.getState();
    SnapshotLoader.setupStateWithSnapshot(getDefaultRegistersSetter(), first, state);
    return SnapshotHelper.getBase64Memory(realCodeBytecodeCreationBase.getState());
  }

  protected void stepUntilComplete(int startAddress) {
    realCodeBytecodeCreationBase.stepUntilComplete(startAddress);
//    getRoutineManager().optimizeAll();
  }

  public RoutineManager getRoutineManager() {
    return realCodeBytecodeCreationBase.getRoutineManager();
  }

  public String generateAndDecompile() {
    return realCodeBytecodeCreationBase.generateAndDecompile();
  }

  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className) {
    return realCodeBytecodeCreationBase.generateAndDecompile(base64Memory, routines, targetFolder, className, realCodeBytecodeCreationBase.symbolicExecutionAdapter);
  }

  public void translateToJava(String className, String memoryInBase64, String startMethod) {
    realCodeBytecodeCreationBase.translateToJava(className, memoryInBase64, startMethod);
  }

  protected RegistersSetter<T> getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getRegistersSetter();
  }
}
