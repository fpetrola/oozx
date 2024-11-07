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
import com.fpetrola.z80.jspeccy.MemorySetter;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@SuppressWarnings("ALL")
public class JSWBytecodeCreationTests<T extends WordNumber> extends RealCodeBytecodeCreationBase<T> {

  @Test
  public void testJSWMoveWilly() {
    String base64Memory = getMemoryInBase64FromFile("file:///home/fernando/dynamitedan1.z80");
    stepUntilComplete(0xC80A);

    String actual = generateAndDecompile(base64Memory, RealCodeBytecodeCreationBase.getRoutines(), ".", "JetSetWilly");
    actual = RemoteZ80Translator.improveSource(actual);
    List<Routine> routines = routineManager.getRoutines();

    Assert.assertEquals("""
        """, actual);
  }

  @Ignore
  @Test
  public void testTranslateWillyToJava() {
    String base64Memory = getMemoryInBase64FromFile("http://torinak.com/qaop/bin/jetsetwilly");
    stepUntilComplete(35090);
    translateToJava("JetSetWilly", base64Memory, "$34762", RealCodeBytecodeCreationBase.getRoutines());
  }

  private String getMemoryInBase64FromFile(String url) {
    String first = getSnapshotFile(url);
    SnapshotLoader.setupStateWithSnapshot(gettDefaultRegistersSetter(), first, new MemorySetter(state.getMemory()));
    String base64Memory = SnapshotHelper.getBase64Memory(state);
    return base64Memory;
  }

  private String getSnapshotFile(String url) {
    try {
      String s = url;
      String first = "/tmp/game.z80";
      Files.copy(new URL(s).openStream(), Paths.get(first), StandardCopyOption.REPLACE_EXISTING);
      return first;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
