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
import com.fpetrola.emulation.helpers.snapshots.SnapshotLoader;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.minizx.emulation.finders.MemoryRangesFinder;
import com.fpetrola.z80.minizx.emulation.finders.MultimapAdapter;
import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.Base64Utils;
import com.fpetrola.z80.transformations.StackAnalyzer;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.Map;

import static com.fpetrola.z80.helpers.Helper.createMD5;
@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class GameBytecodeCreationTests {
  protected final RealCodeBytecodeCreationBase realCodeBytecodeCreationBase;
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


  @Ignore("pacman-memory.txt is not in the repo")
  @Test
  public void testPacman() {
    Helper.hex = true;

    try {
      String content = Files.readString(Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/pacman-memory.txt"));
      byte[] bytes = Base64Utils.gzipDecompressFromBase64(content);


      State state = realCodeBytecodeCreationBase.getState();

      int[] data = state.getMemory().getData();

      for (int i = 0; i < data.length; i++) {
        int aByte = bytes[i] & 0xff;
        data[i]= aByte;
      }

      int i = 12288;
      state.getPc().write(i);

      stepUntilComplete(i);

      String actual = generateAndDecompile(content, getRoutineManager().getRoutines(), ".", "JetSetWilly");
      actual = RemoteZ80Translator.improveSource(actual);

      } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testTranslateWallyToJava() {
    translateRecording("Wally", "/home/fernando/detodo/spectrum/eawally/eawallyCsabaComplete.rzx", 0x8185);
  }

  @Test
  public void testTranslateMontyToJava() {
    translateRecording("MontyOnTheRun", "/home/fernando/detodo/spectrum/montyontherun.rzx", 0xAAC5);
  }

  @Test
  public void testTranslateGreatEscapeToJava() {
    translateRecording("GreatEscape", "/home/fernando/detodo/spectrum/rzx-top/2125/greatescape_moneybagending.rzx", 0xF510);
  }

  @Test
  public void testTranslateZynapsToJava() {
    translateRecording("Zynaps", "/home/fernando/detodo/spectrum/rzx-top/5890/zynaps.rzx", 0xA9E4);
  }

  @Test
  public void testTranslateDynamiteDan2ToJava() {
    translateRecording("DynamiteDan2", "/home/fernando/detodo/spectrum/rzx-top/1553/dynamitedan2.rzx", 0x6B94);
  }

  @Test
  public void testTranslateManicMinerToJava() {
    translateRecording("ManicMiner", "/home/fernando/detodo/spectrum/rzx-top/3012/manicnoliveslost.rzx", 0x92FB);
  }

  @Test
  public void testTranslateExolonToJava() {
    translateRecording("Exolon", "/home/fernando/detodo/spectrum/rzx-top/1686/exolon.rzx", 0x8057);
  }

  @Test
  public void testTranslateRenegadeToJava() {
    translateRecording("Renegade", "/home/fernando/detodo/spectrum/rzx-top/4082/renegade48-random.rzx", 0xF280);
  }

  @Test
  public void testTranslateRenegade128ToJava() {
    translateRecording("Renegade128", "/home/fernando/detodo/spectrum/rzx-top/renegade.rzx", 0x6004);
  }

  @Test
  public void testTranslateTargetRenegadeToJava() {
    translateRecording("TargetRenegade", "/home/fernando/detodo/spectrum/rzx-top/4087/target.rzx", 0xBFBF);
  }

  @Test
  public void testTranslateFairlightToJava() {
    translateRecording("Fairlight", "/home/fernando/detodo/spectrum/rzx-top/1712/fairlight48.rzx", 0xF0DC);
  }

  @Test
  public void testTranslateRTypeToJava() {
    translateRecording("RType", "/home/fernando/detodo/spectrum/rzx-top/4256/rtype-random.rzx", 0xBF60);
  }

  @Ignore
  @Test
  public void testTranslateSamCruiseToJava() {
    testTranslateGame("SamCruise", getMemoryInBase64FromFile("file:///home/fernando/Downloads/samcruise.z80"), 61483);
  }

  @Test
  public void testTranslateEmlynToJava() {
    String base64Memory = RemoteZ80Translator.emulateRecordingUntil(realCodeBytecodeCreationBase, "/home/fernando/detodo/spectrum/emlyn_r4.rzx", 0xFE65);
    List<String> recordings = Stream.of("emlyn_r3.rzx", "emlyn_r4.rzx").map(recording -> "/home/fernando/detodo/spectrum/" + recording).toList();
    realCodeBytecodeCreationBase.exploreRecording(RemoteZ80Translator.Footprint.combine(recordings.stream().map(recording -> RemoteZ80Translator.footprint(recording, 0xFE65)).toList()), 0xFE65, 0x963E);
    CodeVersions versions = realCodeBytecodeCreationBase.getStackAnalyzer().codeVersions;
    int[][] templates = {{0x9BBF, 0x9C1D, 0xE000}, {0x9AF7, 0x9B1C, 0xE300}};
    Stream.of(templates).forEach(template -> versions.mergeBlockRegions(template[0], template[1]));
    recordings.forEach(recording -> RemoteZ80Translator.recordBlockContents(EmulatedMiniZX.ofRecording(recording, -1, null), 0xFE65, versions));
    Stream.of(templates).forEach(template -> realCodeBytecodeCreationBase.translateCodeVariants(template[0], template[1], template[2], versions));
    writeTranslation("Emlyn", base64Memory);
  }

  @Test
  public void testTranslateDizzyToJava() {
    translateRecording("Dizzy", "/home/fernando/detodo/spectrum/dizzy/Dizzy RZX - The Long Way.rzx", 0xF85B, 0xF85A);
  }

  @Test
  public void testTranslateEquinoxToJava() {
    translateRecording("Equinox", "/home/fernando/detodo/spectrum/equinox/equinox.rzx", 0x5B8D);
  }

  private void translateRecording(String name, String recording, int start, int... entries) {
    String base64Memory = RemoteZ80Translator.emulateRecordingUntil(realCodeBytecodeCreationBase, recording, start);
    realCodeBytecodeCreationBase.exploreRecording(RemoteZ80Translator.footprint(recording, start), start, entries);
    writeTranslation(name, base64Memory);
  }

  private void testTranslateGame(String name, String MemoryInBase64FromFile, int startAddress, int... reachedByTheRecording) {
    exploreGame(startAddress, reachedByTheRecording);
    writeTranslation(name, MemoryInBase64FromFile);
  }

  private void exploreGame(int startAddress, int... reachedByTheRecording) {
    Helper.hex = true;
    getRoutineManager().externalEntries.add(startAddress);
    stepUntilComplete(startAddress);
    for (int address : reachedByTheRecording)
      stepUntilComplete(address);
  }

  private void writeTranslation(String name, String base64Memory) {
    List<Routine> routines = getRoutineManager().getRoutines();
    try {
      Files.writeString(Path.of("target/game-routines.txt"), getRoutinesString(routines));
      Files.writeString(Path.of("target/" + name + ".java"), String.valueOf(generateAndDecompile(base64Memory, routines, ".", name)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  public void testTranslateDynamite() throws Exception {
    String recording = "/home/fernando/detodo/spectrum/dynamitedan/dynamitedan.rzx";
    int start = 0xC804;
    String base64Memory = RemoteZ80Translator.emulateRecordingUntil(realCodeBytecodeCreationBase, recording, start);
    realCodeBytecodeCreationBase.exploreRecording(RemoteZ80Translator.footprint(recording, start), start);
    Files.writeString(Path.of("target/dd-routines.txt"), getRoutinesString(getRoutineManager().getRoutines()));
    Files.writeString(Path.of("target/DynamiteDan.java"), RemoteZ80Translator.improveSource(generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "DynamiteDan")));
  }

  @Ignore
  @Test
  public void testTranslateWillyToJava() {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(34762);
    realCodeBytecodeCreationBase.translatedProgram("JetSetWilly", base64Memory).run(34762);

  }

  @Test
  public void testTranslateWillyFromSnapshot() throws Exception {
    String snapshot = System.getenv("JSW_SNAPSHOT");
    Assume.assumeNotNull(snapshot);
    translateWilly(snapshot);
  }

  private String translateWilly(String snapshot) throws Exception {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile(Path.of(snapshot).toUri().toString());
    stepUntilComplete(34463);
    String actual = RemoteZ80Translator.improveSource(generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "JetSetWilly"));
    String routinesString = getRoutinesString(driverConfigurator.getRoutineManager().getRoutines());
    Files.writeString(Path.of("target/jsw-routines.txt"), routinesString);
    Files.writeString(Path.of("target/JetSetWilly.java"), actual);
    assertRunsHeadless("JetSetWilly", "$34463");
    return routinesString;
  }

  @Test
  public void testWillyCheckingRoutines() throws Exception {
    String routinesString = translateWilly("../../doc/jsw/jsw.z80");

    Assert.assertEquals("""
        {34463:38136} -> [34463 : 34498, 34762 : 35210, 35245 : 35562, 35591 : 36146, 37048 : 37055, 38043 : 38045, 38061 : 38063, 38095 : 38097, 38134 : 38136]
        {34499:34692} -> [34499 : 34619, 34687 : 34692]
        {34620:34761} -> [34620 : 34685, 34693 : 34761]
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


  @Ignore("jsw-game-data.json is not in the repo")
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

  private void assertRunsHeadless(String className, String startMethod) throws Exception {
    class BudgetSpent extends RuntimeException {
    }
    System.setProperty("minizx.headless", "true");
    SpectrumApplication.io = new DefaultMiniZXIO();
    Class<?> game = new URLClassLoader(new URL[]{Path.of(".").toUri().toURL()}, getClass().getClassLoader()).loadClass(className);
    MiniZX instance = (MiniZX) game.getConstructor().newInstance();
    instance.setInterruptionCondition(fetches -> {
      if (fetches > 1_000_000)
        throw new BudgetSpent();
      return false;
    });
    try {
      game.getMethod(startMethod).invoke(instance);
      Assert.fail("the game returned after " + instance.fetchCounter + " fetches");
    } catch (InvocationTargetException e) {
      if (!(e.getCause() instanceof BudgetSpent))
        throw e;
    }
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
    State state = realCodeBytecodeCreationBase.getState();
    SnapshotLoader.setupStateWithSnapshot(getDefaultRegistersSetter(), first, state);
//    int[] data = realCodeBytecodeCreationBase.getState().getMemory().getData();
//    data[34498] = 34476 & 0xff;
//    data[34497] = (34476 >> 8) & 0xff;
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

  protected RegistersSetter getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getRegistersSetter();
  }
}
