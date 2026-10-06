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
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.Base64Utils;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.google.gson.Gson;
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
  public void testTranslateWallyToJava() throws Exception {
    int address = 0x8184;
    int emulateUntil = address;
//    emulateUntil = 184100;
////    emulateUntil = 10000;
//    EmulatedMiniZX.rzxFile= "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
//    StackAnalyzer.collecting= true;
    String memoryInBase64FromFile = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, "http://torinak.com/qaop/bin/wally");
//    StackAnalyzer.collecting= false;

    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();
    addDynamicInvocations(stackAnalyzer, "{60160=[60161, 60835, 60870, 60919, 60840, 60281, 60604, 60175, 61383], 60130=[60386, 60851, 60356, 60468, 60309, 60459, 60397, 60414], 61170=[62723, 63302, 62473, 62217, 62027, 62379, 63212, 62799, 62961, 62834, 62675, 63347, 60691, 62260, 63092, 62198, 62524, 62621, 62333], 43400=[43872, 43698, 43843, 43814, 43785, 43931, 43741]}");
    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    testTranslateGame(memoryInBase64FromFile, 0x8185, 0xB7F9, 0xEFC7, 0xF057, 0xF0AE, 0xF0C0, 0xF526);
    Assert.assertEquals("""
        {80E5:8183} -> [80E5 : 8139, 8155 : 8183]
        {813B:8154} -> [813B : 8154]
        {8184:8184} -> [8184 : 8184]
        {8185:EEA1} -> [8185 : 81AD, EE9F : EEA1]
        {81AE:81C3} -> [81AE : 81C3]
        {81C4:81D3} -> [81C4 : 81D3]
        {81D4:81E2} -> [81D4 : 81E2]
        {81E3:81F7} -> [81E3 : 81F7]
        {A83D:A89F} -> [A83D : A89F]
        {A8A0:A920} -> [A8A0 : A920]
        {A921:A92B} -> [A921 : A92B]
        {A92C:AA2F} -> [A92C : AA2F]
        {AA30:AA7E} -> [AA30 : AA7E]
        {AA7F:AA8B} -> [AA7F : AA8B]
        {AA8C:AA9F} -> [AA8C : AA9F]
        {AAB2:AADC} -> [AAB2 : AADC]
        {AADD:AB08} -> [AADD : AB08]
        {AB09:AB25} -> [AB09 : AB25]
        {AB26:AB42} -> [AB26 : AB42]
        {AB43:AB5F} -> [AB43 : AB5F]
        {AB60:AB9A} -> [AB60 : AB9A]
        {AB9B:ABD6} -> [AB9B : ABD6]
        {ABD7:AC05} -> [ABD7 : AC05]
        {AC06:AC5E} -> [AC06 : AC5E]
        {AC5F:AC68} -> [AC5F : AC68]
        {AC69:AC6B} -> [AC69 : AC6B]
        {AC6C:AC7E} -> [AC6C : AC7E]
        {ADBA:AE2F} -> [ADBA : AE2F]
        {AE30:AE3B} -> [AE30 : AE3B]
        {AE5D:AE89} -> [AE5D : AE89]
        {AE8A:AEA6} -> [AE8A : AEA6]
        {AED7:AEE9} -> [AED7 : AEE9]
        {AF01:AF0F} -> [AF01 : AF0F]
        {AFC4:B00A} -> [AFC4 : B00A]
        {B00B:B03B} -> [B00B : B03B]
        {B03C:B06C} -> [B03C : B06C]
        {B06D:B09C} -> [B06D : B09C]
        {B09F:B0E5} -> [B09F : B0E5]
        {B0E6:B196} -> [B0E6 : B196]
        {B197:B1A5} -> [B197 : B1A5]
        {B1A6:B1B8} -> [B1A6 : B1B8]
        {B1B9:B263} -> [B1B9 : B263]
        {B288:E314} -> [B288 : B2DA, E2FC : E314]
        {B2DC:B2E1} -> [B2DC : B2E1]
        {B2E2:B2E7} -> [B2E2 : B2E7]
        {B2E8:B2EC} -> [B2E8 : B2EC]
        {B2F1:B34A} -> [B2F1 : B34A]
        {B34C:B3AA} -> [B34C : B3AA]
        {B3AB:B3BD} -> [B3AB : B3BD]
        {B3C3:F045} -> [B3C3 : B450, EABF : EB53, EB79 : EC0E, EC2B : EC3C, EC4A : EC5B, ECBC : ECF4, EDA3 : EDAB, EDB3 : EDBD, EDC6 : EE9E, EFC7 : F045]
        {B451:B470} -> [B451 : B470]
        {B471:B481} -> [B471 : B481]
        {B482:B4E6} -> [B482 : B4E6]
        {B4EB:B504} -> [B4EB : B504]
        {B505:B536} -> [B505 : B536]
        {B715:B77A} -> [B715 : B77A]
        {B77B:B7B9} -> [B77B : B7B9]
        {B7BA:B7F8} -> [B7BA : B7F8]
        {B7F9:B82A} -> [B7F9 : B82A]
        {B82B:B84A} -> [B82B : B84A]
        {B84B:B8C3} -> [B84B : B8C3]
        {B8C4:B8EC} -> [B8C4 : B8EC]
        {B8ED:B901} -> [B8ED : B901]
        {B902:B92C} -> [B902 : B92C]
        {B92D:B950} -> [B92D : B950]
        {B952:B968} -> [B952 : B968]
        {B969:B9AB} -> [B969 : B9AB]
        {B9AC:B9D5} -> [B9AC : B9D5]
        {B9D6:B9FA} -> [B9D6 : B9FA]
        {B9FB:BA24} -> [B9FB : BA24]
        {BA25:BA65} -> [BA25 : BA65]
        {BA6E:BAB2} -> [BA6E : BAB2]
        {BAB4:BACD} -> [BAB4 : BACD]
        {BACE:BAD7} -> [BACE : BAD7]
        {BAD8:BB3D} -> [BAD8 : BB3D]
        {E315:E328} -> [E315 : E328]
        {E329:E33E} -> [E329 : E33E]
        {E33F:E36C} -> [E33F : E36C]
        {E36D:E3D2} -> [E36D : E3D2]
        {E3D3:E3EB} -> [E3D3 : E3EB]
        {E3EC:E3FC} -> [E3EC : E3FC]
        {E3FD:E41C} -> [E3FD : E41C]
        {E41D:E41F} -> [E41D : E41F]
        {E420:E467} -> [E420 : E467]
        {E468:E483} -> [E468 : E483]
        {E77A:E79A} -> [E77A : E79A]
        {E79B:E7E8} -> [E79B : E7E8]
        {EB55:EB78} -> [EB55 : EB78]
        {EC1D:EC2A} -> [EC1D : EC2A]
        {EC3D:EC49} -> [EC3D : EC49]
        {ED13:F7DD} -> [ED13 : ED29, EEC1 : EEF2, F24B : F26D, F309 : F326, F334 : F374, F37D : F39F, F3AB : F3FC, F409 : F490, F49D : F4D1, F4D3 : F4FE, F503 : F525, F543 : F56D, F572 : F577, F5F1 : F64A, F674 : F6D0, F6EC : F739, F746 : F7DD]
        {ED2A:ED89} -> [ED2A : ED89]
        {ED8A:EDA2} -> [ED8A : EDA2]
        {EEA2:EEC0} -> [EEA2 : EEC0]
        {EF35:EF87} -> [EF35 : EF87]
        {EF88:EF90} -> [EF88 : EF90]
        {EF91:EFC6} -> [EF91 : EFC6]
        {F047:F056} -> [F047 : F056]
        {F057:F0B6} -> [F057 : F0B6]
        {F0C0:F103} -> [F0C0 : F103]
        {F10E:F157} -> [F10E : F157]
        {F277:F2F5} -> [F277 : F2F5]
        {F2F6:F2F6} -> [F2F6 : F2F6]
        {F526:F542} -> [F526 : F542]
        {F578:F5BD} -> [F578 : F5BD]
        {F814:F878} -> [F814 : F878]
        {F8B9:F939} -> [F8B9 : F939]
        {F93A:F9A3} -> [F93A : F9A3]
        {F9A4:F9B1} -> [F9A4 : F9B1]
        {F9C4:F9EF} -> [F9C4 : F9EF]
        {F9F5:FAE6} -> [F9F5 : FAE6]
        {FAE7:FAEE} -> [FAE7 : FAEE]
        {FAF0:FB29} -> [FAF0 : FB29]
        """, getRoutinesString(getRoutineManager().getRoutines()));
    assertRunsHeadless("ZxGame1", "$8185");
  }

  private void addDynamicInvocations(StackAnalyzer stackAnalyzer, String json) {
    ((Map<String, List<Double>>) new Gson().fromJson(json, Map.class)).entrySet()
        .forEach(e -> e.getValue().forEach(v -> stackAnalyzer.dynamicInvocation.put(Integer.parseInt(e.getKey()), v.intValue())));
  }

  @Ignore
  @Test
  public void testTranslateSamCruiseToJava() {
    testTranslateGame(getMemoryInBase64FromFile("file:///home/fernando/Downloads/samcruise.z80"), 61483);
  }

  @Test
  public void testTranslateEmlynToJava() {
    String base64Memory = RemoteZ80Translator.emulateRecordingUntil(realCodeBytecodeCreationBase, "/home/fernando/detodo/spectrum/emlyn_r4.rzx", 0xFE65);
    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();
    RemoteZ80Translator.Footprint footprint = RemoteZ80Translator.Footprint.combine(Stream.of("emlyn_r3.rzx", "emlyn_r4.rzx").map(recording -> RemoteZ80Translator.footprint("/home/fernando/detodo/spectrum/" + recording, 0xFE65)).toList());
    footprint.install(realCodeBytecodeCreationBase.getState().getMemory(), realCodeBytecodeCreationBase.getState().getRegisterSP().read());
    stackAnalyzer.learnFrom(footprint.learned());
    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    getRoutineManager().setReachable(footprint.executed());
    realCodeBytecodeCreationBase.symbolicExecutionAdapter.getMutantAddress().addAll(footprint.modifiedCode());
    getRoutineManager().externalEntries.addAll(footprint.returnAddressesOnStack());
    exploreGame(0xFE65, Stream.of(Stream.of(0x963E), footprint.returnAddressesOnStack().stream(), stackAnalyzer.dynamicInvocation.values().stream()).flatMap(s -> s).mapToInt(Integer::intValue).toArray());
    realCodeBytecodeCreationBase.translateRomRoutines(0x0038, 0x22B0, 0x0E44, 0x03F4, 0x2C8D);
    realCodeBytecodeCreationBase.translateCodeVariants(0x9BBF, 0x9C1D, 0x9BDA, 0xE000,
        "79c3df9b79652e001fcb1ccb1dc38f9c08e378c3f19b78652e001fcb1ccb1dc3979c",
        "26fd7e696e652e001fcb1ccb1dc38f9c08e326fd7e686e652e001fcb1ccb1dc3979c",
        "79c3df9b79673e00291729172917291708e378c3f19b78673e002917291729172917",
        "26fd7e696e652e00c3ea9b00cb0ecb0608e326fd7e686e652e00c3fc9b00cb0ecb06",
        "79c3df9b79652e00c3ea9b00cb0ecb0608e378c3f19b78652e00c3fc9b00cb0ecb06",
        "79c3df9b79673e0029172917c3ea9b0008e378c3f19b78673e0029172917c3fc9b00",
        "26fd7e696e673e00291729172917291708e326fd7e686e673e002917291729172917",
        "26fd7e696e673e0029172917c3ea9b0008e326fd7e686e673e0029172917c3fc9b00");
    realCodeBytecodeCreationBase.translateCodeVariants(0x9AF7, 0x9B1C, 0x9AFB, 0xE300,
        "2d3601243602243604243608243610243620",
        "36102436202436402436802d243601243602",
        "3640243680242d3601243602243604243608",
        "3610243620243640243680242d3601243602",
        "36042436082436102436202436402436802d",
        "3608243604243602243601242c3680243640",
        "2c3680243640243620243610243608243604",
        "36202436102436082436042436022436012c",
        "3602243601242c3680243640243620243610");
    writeTranslation(base64Memory);
  }

  @Test
  public void testTranslateDizzyToJava() {
    String recording = "/home/fernando/detodo/spectrum/dizzy/Dizzy RZX - The Long Way.rzx";
    int start = 0xF85B;
    String base64Memory = RemoteZ80Translator.emulateRecordingUntil(realCodeBytecodeCreationBase, recording, start);
    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();
    RemoteZ80Translator.Footprint footprint = RemoteZ80Translator.footprint(recording, start);
    footprint.install(realCodeBytecodeCreationBase.getState().getMemory(), realCodeBytecodeCreationBase.getState().getRegisterSP().read());
    stackAnalyzer.learnFrom(footprint.learned());
    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    getRoutineManager().setReachable(footprint.executed());
    realCodeBytecodeCreationBase.symbolicExecutionAdapter.getMutantAddress().addAll(footprint.modifiedCode());
    getRoutineManager().externalEntries.addAll(footprint.returnAddressesOnStack());
    stackAnalyzer.nonLocalRets.keySet().forEach(ret -> getRoutineManager().externalEntries.addAll(stackAnalyzer.dynamicInvocation.get(ret)));
    getRoutineManager().externalEntries.addAll(stackAnalyzer.calledThrough.values());
    exploreGame(start, Stream.of(Stream.of(0xF85A), footprint.returnAddressesOnStack().stream(), stackAnalyzer.dynamicInvocation.values().stream(), stackAnalyzer.calledThrough.values().stream()).flatMap(s -> s).mapToInt(Integer::intValue).toArray());
    realCodeBytecodeCreationBase.translateRomRoutines(0x0038);
    writeTranslation(base64Memory);
  }

  private void testTranslateGame(String MemoryInBase64FromFile, int startAddress, int... reachedByTheRecording) {
    exploreGame(startAddress, reachedByTheRecording);
    writeTranslation(MemoryInBase64FromFile);
  }

  private void exploreGame(int startAddress, int... reachedByTheRecording) {
    Helper.hex = true;
    getRoutineManager().externalEntries.add(startAddress);
    stepUntilComplete(startAddress);
    for (int address : reachedByTheRecording)
      stepUntilComplete(address);
  }

  private void writeTranslation(String base64Memory) {
    List<Routine> routines = getRoutineManager().getRoutines();
    try {
      Files.writeString(Path.of("target/game-routines.txt"), getRoutinesString(routines));
      Files.writeString(Path.of("target/Game.java"), String.valueOf(generateAndDecompile(base64Memory, routines, ".", "ZxGame1")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  public void testTranslateDynamite() throws Exception {
    int emulateUntil = 0xC804;
//    StackAnalyzer.collecting= true;
//    emulateUntil= 52879;
    String base64Memory = RemoteZ80Translator.emulateRecording(realCodeBytecodeCreationBase, "/home/fernando/detodo/spectrum/dynamitedan/dynamitedan.rzx", emulateUntil);
//    StackAnalyzer.collecting= false;

    StackAnalyzer stackAnalyzer = realCodeBytecodeCreationBase.getStackAnalyzer();

    addDynamicInvocations(stackAnalyzer, "{52931=[52961, 53111], 55965=[56008, 55966, 56058], 111=[51200], 59839=[59867]}");

    stackAnalyzer.reset(realCodeBytecodeCreationBase.getState());
    stepUntilComplete(0xC804);

//    translateToJava("ZxGame1", base64Memory, "$C804");
    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "ZxGame1");
    actual = RemoteZ80Translator.improveSource(actual);
    Files.writeString(Path.of("target/dd-routines.txt"), getRoutinesString(getRoutineManager().getRoutines()));
    Files.writeString(Path.of("target/DD.java"), actual);
    Assert.assertEquals("""
        {62A3:62D5} -> [62A3 : 62D5]
        {C804:E643} -> [C804 : C865, CDD5 : CE52, DC42 : DC44, E641 : E643]
        {C881:C8D1} -> [C881 : C8D1]
        {C8FF:C924} -> [C8FF : C924]
        {C92A:CA1D} -> [C92A : CA1D]
        {CA56:F2F3} -> [CA56 : CB8C, DB63 : DB82, DCF6 : DD8C, F2BE : F2F3]
        {CB8D:CB9B} -> [CB8D : CB9B]
        {CB9C:CBBC} -> [CB9C : CBBC]
        {CBBD:CC35} -> [CBBD : CC35]
        {CC36:CC5D} -> [CC36 : CC5D]
        {CC5F:CC88} -> [CC5F : CC88]
        {CC89:CD23} -> [CC89 : CD23]
        {CD24:CD2A} -> [CD24 : CD2A]
        {CD2B:CD30} -> [CD2B : CD30]
        {CD31:CD5B} -> [CD31 : CD5B]
        {CD5C:CD7D} -> [CD5C : CD7D]
        {CD8A:CD99} -> [CD8A : CD99]
        {CD9A:CDD2} -> [CD9A : CDD2]
        {CE76:CEA3} -> [CE76 : CEA3]
        {CEAD:CEE5} -> [CEAD : CEC3, CEE1 : CEE5]
        {CEF0:CF76} -> [CEF0 : CF76]
        {CF77:CFC8} -> [CF77 : CFC8]
        {CFD9:DC68} -> [CFD9 : D1B1, D316 : D318, DBEE : DC40, DC45 : DC68]
        {D1B2:D1CC} -> [D1B2 : D1CC]
        {D1CE:D2BE} -> [D1CE : D2BE]
        {D2BF:D2ED} -> [D2BF : D2ED]
        {D2EF:D2EF} -> [D2EF : D2EF]
        {D2F0:D950} -> [D2F0 : D314, D319 : D377, D895 : D950]
        {D378:D3AB} -> [D378 : D3AB]
        {D3EC:D4AE} -> [D3EC : D4AE]
        {D4AF:D4B4} -> [D4AF : D4B4]
        {D4B5:D54F} -> [D4B5 : D54F]
        {D550:D566} -> [D550 : D566]
        {D567:DE0A} -> [D567 : D606, DDF4 : DE0A]
        {D607:D61D} -> [D607 : D61D]
        {D61E:D654} -> [D61E : D654]
        {D655:D668} -> [D655 : D668]
        {D669:D674} -> [D669 : D674]
        {D677:D6BA} -> [D677 : D6BA]
        {D6BF:D72D} -> [D6BF : D72D]
        {D732:D7A1} -> [D732 : D7A1]
        {D7A2:D7B6} -> [D7A2 : D7B6]
        {D7B7:D7D5} -> [D7B7 : D7D5]
        {D7D6:D7E6} -> [D7D6 : D7E6]
        {D7E7:D812} -> [D7E7 : D812]
        {D815:D894} -> [D815 : D894]
        {D951:D9A6} -> [D951 : D9A6]
        {D9A7:D9A9} -> [D9A7 : D9A9]
        {D9AA:D9AF} -> [D9AA : D9AF]
        {D9B0:D9E7} -> [D9B0 : D9E7]
        {D9EC:DA86} -> [D9EC : DA86]
        {DA8D:DB37} -> [DA8D : DAB1, DAC8 : DAF9, DB17 : DB37]
        {DAB2:DAC7} -> [DAB2 : DAC7]
        {DAFA:DB00} -> [DAFA : DB00]
        {DB01:DB0A} -> [DB01 : DB0A]
        {DB0B:DB16} -> [DB0B : DB16]
        {DB38:DB51} -> [DB38 : DB51]
        {DB83:DB9A} -> [DB83 : DB9A]
        {DB9B:DBB7} -> [DB9B : DBB7]
        {DBB8:DBEC} -> [DBB8 : DBEC]
        {DC71:DCC0} -> [DC71 : DCC0]
        {DCC1:DCCB} -> [DCC1 : DCCB]
        {DCCC:DCE3} -> [DCCC : DCE3]
        {DCE4:DCE7} -> [DCE4 : DCE7]
        {DCE8:DCF4} -> [DCE8 : DCF4]
        {DD8D:DDDF} -> [DD8D : DDDF]
        {DDE0:DDF3} -> [DDE0 : DDF3]
        {DE0B:DE1A} -> [DE0B : DE1A]
        {DE1B:DE27} -> [DE1B : DE27]
        {DE28:DE39} -> [DE28 : DE39]
        {DE3A:DE50} -> [DE3A : DE50]
        {DE52:DE7E} -> [DE52 : DE7E]
        {DE87:DED7} -> [DE87 : DED7]
        {E544:E54C} -> [E544 : E54C]
        {E54D:E591} -> [E54D : E591]
        {E592:E59E} -> [E592 : E59E]
        {E59F:E5E7} -> [E59F : E5E7]
        {E5E8:E661} -> [E5E8 : E63F, E644 : E661]
        {E663:E6D8} -> [E663 : E6D8]
        {E6DC:E6F5} -> [E6DC : E6F5]
        {E6F6:E7D6} -> [E6F6 : E755, E782 : E7D6]
        {E756:E76B} -> [E756 : E76B]
        {E76C:E774} -> [E76C : E774]
        {E775:E781} -> [E775 : E781]
        {E7D7:E7E1} -> [E7D7 : E7E1]
        {E801:E81F} -> [E801 : E81F]
        {E820:E84D} -> [E820 : E84D]
        {E84E:E879} -> [E84E : E879]
        {E87A:E896} -> [E87A : E896]
        {E897:E8B9} -> [E897 : E8B9]
        {E8BA:E8D1} -> [E8BA : E8D1]
        {E8D2:E8E2} -> [E8D2 : E8E2]
        {E8E3:E8F0} -> [E8E3 : E8F0]
        {E8F1:E8F6} -> [E8F1 : E8F6]
        {E8F7:E908} -> [E8F7 : E908]
        {E909:E90B} -> [E909 : E90B]
        {E90C:E915} -> [E90C : E915]
        {E916:E93D} -> [E916 : E93D]
        {E93F:E96A} -> [E93F : E96A]
        {E96B:E9B9} -> [E96B : E9B9]
        {E9BC:E9F7} -> [E9BC : E9BF, E9DB : E9F7]
        {E9F8:EA04} -> [E9F8 : EA04]
        {ECA4:ECBC} -> [ECA4 : ECBC]
        {ECDD:ECF3} -> [ECDD : ECF3]
        {ECF4:ECFF} -> [ECF4 : ECFF]
        {ED00:ED05} -> [ED00 : ED05]
        {ED06:EDA0} -> [ED06 : EDA0]
        {EDA2:EDBB} -> [EDA2 : EDBB]
        {EEF1:EEF7} -> [EEF1 : EEF7]
        {EEF8:EEF8} -> [EEF8 : EEF8]
        {EEF9:EF1D} -> [EEF9 : EF1D]
        {EF1E:EF47} -> [EF1E : EF47]
        {EF7C:EFBB} -> [EF7C : EFBB]
        {F021:F051} -> [F021 : F051]
        {F2F4:F2FF} -> [F2F4 : F2FF]
        {F300:F309} -> [F300 : F309]
        {F30A:F344} -> [F30A : F344]
        {F345:F39F} -> [F345 : F39F]
        {F3A4:F3D2} -> [F3A4 : F3D2]
        {F3D3:F3DF} -> [F3D3 : F3DF]
        {F3E0:F3EB} -> [F3E0 : F3EB]
        {F3EC:F40F} -> [F3EC : F40F]
        {F470:F484} -> [F470 : F484]
        """, getRoutinesString(getRoutineManager().getRoutines()));
    assertRunsHeadless("ZxGame1", "$C804");

    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();
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
  public void testWillyCheckingRoutines() throws Exception {
    Helper.hex = false;
//    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    String base64Memory = getMemoryInBase64FromFile(Path.of("../../doc/jsw/jsw.z80").toUri().toString());

    stepUntilComplete(34463);

    String actual = generateAndDecompile(base64Memory, getRoutineManager().getRoutines(), ".", "JetSetWilly");
    actual = RemoteZ80Translator.improveSource(actual);

    List<Routine> routines = driverConfigurator.getRoutineManager().getRoutines();


    String routinesString = getRoutinesString(routines);
    Files.writeString(Path.of("target/jsw-routines.txt"), routinesString);
    Files.writeString(Path.of("target/JetSetWilly.java"), actual);
    assertRunsHeadless("JetSetWilly", "$34463");

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

  public void translateToJava(String className, String memoryInBase64, String startMethod) {
    realCodeBytecodeCreationBase.translateToJava(className, memoryInBase64, startMethod);
  }

  protected RegistersSetter getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getRegistersSetter();
  }
}
