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
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.minizx.emulation.finders.MemoryRangesFinder;
import com.fpetrola.z80.minizx.emulation.finders.MultimapAdapter;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.google.gson.Gson;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static com.fpetrola.z80.helpers.Helper.createMD5;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class GameBytecodeCreationTests<T extends WordNumber> {
  protected final RealCodeBytecodeCreationBase<T> realCodeBytecodeCreationBase;
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

  @Test
  public void testTranslateWallyToJava() {
    int address = 0x8184;
    int emulateUntil = address;
//    emulateUntil = 184100;
////    emulateUntil = 10000;
//    EmulatedMiniZX.rzxFile= "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
//    StackAnalyzer.collecting= true;
    String memoryInBase64FromFile = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "http://torinak.com/qaop/bin/wally");
//    StackAnalyzer.collecting= false;

    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();
    addDynamicInvocations(stackAnalyzer, "{60160=[60161, 60835, 60870, 60919, 60840, 60281, 60604, 60175], 60130=[60386, 60851, 60356, 60468, 60309, 60459, 60397, 60414], 61170=[62723, 63302, 62473, 62217, 62027, 62379, 63212, 62799, 62961, 62834, 62675, 63347, 60691, 62260, 63092, 62198, 62524, 62621, 62333], 43400=[43872, 43698, 43843, 43814, 43785, 43931, 43741]}");
    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    testTranslateGame(memoryInBase64FromFile, 0x8185);
  }

  private void addDynamicInvocations(StackAnalyzer stackAnalyzer, String json) {
    ((Map<String, List<Double>>) new Gson().fromJson(json, Map.class)).entrySet()
        .forEach(e -> e.getValue().forEach(v -> stackAnalyzer.dynamicInvocation.put(Integer.parseInt(e.getKey()), Double.valueOf(v).intValue())));
  }

  @Ignore
  @Test
  public void testTranslateSamCruiseToJava() {
    testTranslateGame(getMemoryInBase64FromFile("file:///home/fernando/Downloads/samcruise.z80"), 61483);
  }

  @Test
  public void testTranslateEmlynToJava() {
    Helper.hex = true;
    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();
    addDynamicInvocations(stackAnalyzer, "{24992=[26125], 29217=[27660, 25762, 27675], 23942=[25378], 25606=[26550], 26662=[23744], 46923=[47657, 38058, 47475, 47047], 5676=[23744, 25251, 25238, 32759, 28122, 25372, 25468, 26685], 38222=[28174], 38257=[38282, 38323, 38307, 38332, 38316], 24979=[26125], 1012=[26457, 26498, 28515, 25532, 26509, 28037], 23893=[26125], 51063=[51648, 51840, 51265, 52417, 55011, 52211, 51653, 51878, 51705, 52332], 25080=[26125], 23801=[26125], 29212=[26141, 25278, 25302], 24988=[26125], 38110=[56880, 44643, 46020, 45288, 43866, 48939, 43964, 49758]}");

    String base64Memory = getMemoryInBase64FromFile("file:////home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");
    stepUntilComplete(0xb542);

    List<Routine> routines = getRoutineManager().getRoutines();
    String actual = generateAndDecompile(base64Memory, routines, ".", "ZxGame1");

    Assert.assertEquals("""
        """, actual);
    translateToJava("emlyn", base64Memory, "$b542");
//    testTranslateGame(getMemoryInBase64FromFile("file:////home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80"), 0xb542);
  }

  @Test
  public void testTranslateEmlynToJava2() {
    int emulateUntil = 0xC804;
    EmulatedMiniZX.setRzxFile("/home/fernando/detodo/desarrollo/m/zx/roms/recordings/emlyn/emlyn3.rzx");
    StackAnalyzer.collecting= true;
    emulateUntil= 23451;
    String base64Memory = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "file:////home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");
    StackAnalyzer.collecting= false;

    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();

    addDynamicInvocations(stackAnalyzer, "{52931=[52961, 53111], 55965=[56008, 55966, 56058], 111=[51200], 59839=[59867]}");

    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    stepUntilComplete(0xC804);


    Helper.hex = true;
    addDynamicInvocations(stackAnalyzer, "{38257=[38282, 38307, 38323, 38332, 38316], 51063=[51648, 52056, 51265, 52417, 55011, 51653], 46923=[47657, 38058, 47475, 47047], 38110=[56880, 44643, 46020, 45288, 43866, 48939, 43964, 49758], 38222=[56880]}");

     base64Memory = getMemoryInBase64FromFile("file:////home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");
    stepUntilComplete(0xb542);

    List<Routine> routines = getRoutineManager().getRoutines();
//    String actual = generateAndDecompile(base64Memory, routines, ".", "ZxGame1");

//    Assert.assertEquals("""
//        """, actual);
    translateToJava("emlyn", base64Memory, "$b542");
//    testTranslateGame(getMemoryInBase64FromFile("file:////home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80"), 0xb542);
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
    int emulateUntil = 0xC804;
//    EmulatedMiniZX.rzxFile= "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx";
//    StackAnalyzer.collecting= true;
//    emulateUntil= 52879;
    String base64Memory = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "http://torinak.com/qaop/bin/dynamitedan");
//    StackAnalyzer.collecting= false;

    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();

    addDynamicInvocations(stackAnalyzer, "{52931=[52961, 53111], 55965=[56008, 55966, 56058], 111=[51200], 59839=[59867]}");

    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    stepUntilComplete(0xC804);

//    translateToJava("ZxGame1", base64Memory, "$C804");
    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "ZxGame1");
    actual = RemoteZ80Translator.improveSource(actual);

    Assert.assertEquals("", actual);
    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();

    Assert.assertEquals("296abb94c428f5a9593c2ec4dafe5e1a", createMD5(actual));
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

    Assert.assertEquals("", actual);
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


  @Test
  public void testWillyCheckingRoutinesAndGameData() {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(35090);

    GameData gameData = MemoryRangesFinder.loadFromJson("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/jsw-game-data.json", MultimapAdapter.getGson());

    realCodeBytecodeCreationBase.setGameData(gameData);
    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "JetSetWilly");
    actual = RemoteZ80Translator.improveSource(actual);

    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();

    String routinesString = getRoutinesString(routines);
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
