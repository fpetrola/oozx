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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
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
public class JSWBytecodeCreationTests<T extends WordNumber> {
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
  public JSWBytecodeCreationTests(RoutinesDriverConfigurator driverConfigurator) {
    realCodeBytecodeCreationBase = driverConfigurator.getRealCodeBytecodeCreationBase();
    this.driverConfigurator = driverConfigurator;
  }

  @Ignore
  @Test
  public void testTranslateSamCruiseToJava() {
    Helper.hex = false;
    String base64Memory = getMemoryInBase64FromFile("file:///home/fernando/Downloads/samcruise.z80");
    stepUntilComplete(61483);
    translateToJava("ZxGame1", base64Memory, "$61483");
  }

  @Test
  public void testEmulateUntil() {
    String base64Memory = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, 0xC804, "http://torinak.com/qaop/bin/dynamitedan");
    stepUntilComplete(0xC804);

    translateToJava("ZxGame1", base64Memory, "$C804");

    List<Routine> routines = getRoutineManager().getRoutines();
    String actual = generateAndDecompile(base64Memory, routines, ".", "ZxGame1");

    String routinesString = getRoutinesString(routines);

    Assert.assertEquals("""
        {C804:FFFF} -> [Code: C804 : C865, Code: CDD5 : CE52, Code: DC42 : DC44, Code: E641 : E643, Code: F485 : FFFF]
        {C881:C8D1} -> [Code: C881 : C8D1]
        {C8FF:C924} -> [Code: C8FF : C924]
        {C92A:CA1D} -> [Code: C92A : CA1D]
        {CA5B:F2F3} -> [Code: CA5B : CB8C, Code: DB63 : DB82, Code: DCF6 : DD8C, Code: F2BE : F2F3]
        {CB8D:CB9B} -> [Code: CB8D : CB9B]
        {CB9C:CBBC} -> [Code: CB9C : CBBC]
        {CBBD:CC35} -> [Code: CBBD : CC35]
        {CC36:CC5D} -> [Code: CC36 : CC5D]
        {CC5F:CC88} -> [Code: CC5F : CC88]
        {CC89:CD23} -> [Code: CC89 : CD23]
        {CD24:CD5B} -> [Code: CD24 : CD2A, Code: CD31 : CD5B]
        {CD2B:CD30} -> [Code: CD2B : CD30]
        {CD5C:CD7D} -> [Code: CD5C : CD7D]
        {CD8A:CD99} -> [Code: CD8A : CD99]
        {CD9A:CDD2} -> [Code: CD9A : CDD2]
        {CE76:CEA3} -> [Code: CE76 : CEA3]
        {CEAD:CF76} -> [Code: CEAD : CEC3, Code: CEE1 : CEE5, Code: CEF0 : CF76]
        {CFD9:DC68} -> [Code: CFD9 : D1B1, Code: D2EF : D2EF, Code: D316 : D318, Code: DBEE : DC40, Code: DC45 : DC68]
        {D1B2:D1CC} -> [Code: D1B2 : D1CC]
        {D1CE:D2BE} -> [Code: D1CE : D2BE]
        {D2BF:D2ED} -> [Code: D2BF : D2ED]
        {D2F0:D950} -> [Code: D2F0 : D314, Code: D319 : D377, Code: D895 : D950]
        {D378:D3AB} -> [Code: D378 : D3AB]
        {D3EC:D4AE} -> [Code: D3EC : D4AE]
        {D4AF:D4B4} -> [Code: D4AF : D4B4]
        {D4B5:D54F} -> [Code: D4B5 : D54F]
        {D550:D566} -> [Code: D550 : D566]
        {D567:DE0A} -> [Code: D567 : D606, Code: DDF4 : DE0A]
        {D607:D61D} -> [Code: D607 : D61D]
        {D61E:D654} -> [Code: D61E : D654]
        {D655:D668} -> [Code: D655 : D668]
        {D669:D674} -> [Code: D669 : D674]
        {D677:D6BA} -> [Code: D677 : D6BA]
        {D6BF:D72D} -> [Code: D6BF : D72D]
        {D732:D7A1} -> [Code: D732 : D7A1]
        {D7A2:D7B6} -> [Code: D7A2 : D7B6]
        {D7B7:D7D5} -> [Code: D7B7 : D7D5]
        {D7D6:D7E6} -> [Code: D7D6 : D7E6]
        {D7E7:D812} -> [Code: D7E7 : D812]
        {D815:D894} -> [Code: D815 : D894]
        {D951:D9A6} -> [Code: D951 : D9A6]
        {D9A7:D9A9} -> [Code: D9A7 : D9A9]
        {D9AA:D9AF} -> [Code: D9AA : D9AF]
        {D9B0:D9E7} -> [Code: D9B0 : D9E7]
        {D9EC:DA86} -> [Code: D9EC : DA86]
        {DA8D:DB37} -> [Code: DA8D : DA9D, Code: DAA7 : DAB1, Code: DAE5 : DAF9, Code: DB17 : DB37]
        {DA9E:DAA6} -> [Code: DA9E : DAA6]
        {DAB2:DAC7} -> [Code: DAB2 : DAC7]
        {DAC8:DAE4} -> [Code: DAC8 : DAE4]
        {DAFA:DB00} -> [Code: DAFA : DB00]
        {DB01:DB0A} -> [Code: DB01 : DB0A]
        {DB0B:DB16} -> [Code: DB0B : DB16]
        {DB38:DB51} -> [Code: DB38 : DB51]
        {DB83:DB9A} -> [Code: DB83 : DB9A]
        {DB9B:DBB7} -> [Code: DB9B : DBB7]
        {DBB8:DBEC} -> [Code: DBB8 : DBEC]
        {DC71:DCC0} -> [Code: DC71 : DCC0]
        {DCC1:DCCB} -> [Code: DCC1 : DCCB]
        {DCCC:DCE3} -> [Code: DCCC : DCE3]
        {DCE4:DCE7} -> [Code: DCE4 : DCE7]
        {DCE8:DCF4} -> [Code: DCE8 : DCF4]
        {DD8D:DDDF} -> [Code: DD8D : DDDF]
        {DDE0:DDF3} -> [Code: DDE0 : DDF3]
        {DE0B:DE1A} -> [Code: DE0B : DE1A]
        {DE1B:DE27} -> [Code: DE1B : DE27]
        {DE28:DE39} -> [Code: DE28 : DE39]
        {DE3A:DE50} -> [Code: DE3A : DE50]
        {DE52:DE7E} -> [Code: DE52 : DE7E]
        {DE87:DED7} -> [Code: DE87 : DED7]
        {E544:E54C} -> [Code: E544 : E54C]
        {E54D:E591} -> [Code: E54D : E591]
        {E592:E59E} -> [Code: E592 : E59E]
        {E59F:E5E7} -> [Code: E59F : E5E7]
        {E5E8:E661} -> [Code: E5E8 : E63F, Code: E644 : E661]
        {E663:E6D8} -> [Code: E663 : E6D8]
        {E6DC:E6F5} -> [Code: E6DC : E6F5]
        {E6F6:E755} -> [Code: E6F6 : E755]
        {E756:E76B} -> [Code: E756 : E76B]
        {E76C:E774} -> [Code: E76C : E774]
        {E775:E781} -> [Code: E775 : E781]
        {E782:E7D6} -> [Code: E782 : E7D6]
        {E7D7:E7E1} -> [Code: E7D7 : E7E1]
        {E801:E81F} -> [Code: E801 : E81F]
        {E820:E84D} -> [Code: E820 : E84D]
        {E84E:E879} -> [Code: E84E : E879]
        {E87A:E896} -> [Code: E87A : E896]
        {E897:E8B9} -> [Code: E897 : E8B9]
        {E8BA:E8D1} -> [Code: E8BA : E8D1]
        {E8D2:E8E2} -> [Code: E8D2 : E8E2]
        {E8E3:E8F0} -> [Code: E8E3 : E8F0]
        {E8F1:E8F6} -> [Code: E8F1 : E8F6]
        {E8F7:E908} -> [Code: E8F7 : E908]
        {E909:E90B} -> [Code: E909 : E90B]
        {E90C:E915} -> [Code: E90C : E915]
        {E916:E93D} -> [Code: E916 : E93D]
        {E93F:E96A} -> [Code: E93F : E96A]
        {E96B:E9B9} -> [Code: E96B : E9B9]
        {E9BC:E9DA} -> [Code: E9BC : E9DA]
        {E9DB:E9F7} -> [Code: E9DB : E9F7]
        {E9F8:EA04} -> [Code: E9F8 : EA04]
        {ECA4:ECBC} -> [Code: ECA4 : ECBC]
        {ECDD:ECF3} -> [Code: ECDD : ECF3]
        {ECF4:ECFF} -> [Code: ECF4 : ECFF]
        {ED00:ED05} -> [Code: ED00 : ED05]
        {ED06:EDA0} -> [Code: ED06 : EDA0]
        {EDA2:EDBB} -> [Code: EDA2 : EDBB]
        {EEF1:EEF7} -> [Code: EEF1 : EEF7]
        {EEF8:EEF8} -> [Code: EEF8 : EEF8]
        {EEF9:EF1D} -> [Code: EEF9 : EF1D]
        {EF1E:EF47} -> [Code: EF1E : EF47]
        {EF7C:EFBB} -> [Code: EF7C : EFBB]
        {F021:F051} -> [Code: F021 : F051]
        {F271:F2A4} -> [Code: F271 : F2A4]
        {F2F4:F2FF} -> [Code: F2F4 : F2FF]
        {F300:F309} -> [Code: F300 : F309]
        {F30A:F329} -> [Code: F30A : F329]
        {F32A:F344} -> [Code: F32A : F344]
        {F345:F39F} -> [Code: F345 : F39F]
        {F3A4:F3D2} -> [Code: F3A4 : F3D2]
        {F3D3:F3DF} -> [Code: F3D3 : F3DF]
        {F3E0:F3EB} -> [Code: F3E0 : F3EB]
        {F3EC:F40F} -> [Code: F3EC : F40F]
        {F470:F484} -> [Code: F470 : F484]
        """, routinesString);

    Assert.assertEquals("8f16af509d0ed4447f5d57355f6505af", createMD5(actual));
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

    List<Routine> routines = getRoutineManager().getRoutines();
    String actual = generateAndDecompile(base64Memory, routines, ".", "JetSetWilly");

    String routinesString = getRoutinesString(routines);

    Assert.assertEquals("""
        {34762:65535} -> [Code: 34762 : 35210, Code: 35245 : 35562, Code: 35591 : 36146, Code: 37048 : 37055, Code: 38043 : 38045, Code: 38061 : 38063, Code: 38095 : 38097, Code: 38134 : 38136, Code: 38644 : 65535]
        {35211:35244} -> [Code: 35211 : 35244]
        {35563:35590} -> [Code: 35563 : 35590]
        {36147:36170} -> [Code: 36147 : 36170]
        {36171:36202} -> [Code: 36171 : 36202]
        {36203:36287} -> [Code: 36203 : 36287]
        {36288:36306} -> [Code: 36288 : 36306]
        {36307:38132} -> [Code: 36307 : 36507, Code: 36528 : 37045, Code: 38026 : 38041, Code: 38046 : 38059, Code: 38098 : 38132]
        {36508:36527} -> [Code: 36508 : 36527]
        {37056:37309} -> [Code: 37056 : 37309]
        {37310:37818} -> [Code: 37310 : 37818]
        {37841:37973} -> [Code: 37841 : 37973]
        {37974:38025} -> [Code: 37974 : 38025]
        {38064:38093} -> [Code: 38064 : 38093]
        {38137:38195} -> [Code: 38137 : 38195]
        {38196:38343} -> [Code: 38196 : 38275, Code: 38298 : 38343]
        {38276:38297} -> [Code: 38276 : 38297]
        {38344:38503} -> [Code: 38344 : 38429, Code: 38455 : 38503]
        {38430:38454} -> [Code: 38430 : 38454]
        {38504:38527} -> [Code: 38504 : 38527]
        {38528:38544} -> [Code: 38528 : 38544]
        {38545:38554} -> [Code: 38545 : 38554]
        {38555:38561} -> [Code: 38555 : 38561]
        {38562:38600} -> [Code: 38562 : 38600]
        {38601:38621} -> [Code: 38601 : 38621]
        {38622:38643} -> [Code: 38622 : 38643]
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
