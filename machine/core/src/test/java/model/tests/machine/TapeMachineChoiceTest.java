/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.OOSpectrumLauncher;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which machine a tape is loaded into.
 * <p>
 * A tape says nothing about the machine it was made for - a snapshot does, which is why those
 * have always arrived on the right one - so the only thing to go on is what the archive called
 * the file. Where an entry offers both, they are named for it, and loading the 128K release into
 * a 48K machine gets to the end of the tape and answers "out of memory": a 48K BASIC error, and
 * the machine saying exactly what is wrong.
 */
class TapeMachineChoiceTest {

  /** A tape of two blocks with a given name; the contents do not matter, only the name does. */
  private static File tapeCalled(String name) throws IOException {
    File file = new File(Files.createTempDirectory("tapes").toFile(), name);
    file.deleteOnExit();
    try (FileOutputStream out = new FileOutputStream(file)) {
      for (int block = 0; block < 2; block++) {
        byte[] body = new byte[19];
        body[0] = (byte) (block == 0 ? 0x00 : 0xFF);
        out.write(body.length & 0xFF);
        out.write((body.length >> 8) & 0xFF);
        out.write(body);
      }
    }
    return file;
  }

  private static String machineFor(String name) throws IOException {
    return machineFor(name, null);
  }

  private static String machineFor(String name, String chosen) throws IOException {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = new OOSpectrumLauncher()
        .createSpeccy(tapeCalled(name).getAbsolutePath(), chosen);
    return speccy.machine.current.getName();
  }

  @Test
  void a_release_named_for_128k_is_loaded_into_a_128k_machine() throws Exception {
    assertEquals("Spectrum 128K", machineFor("DarkTransit2(128K).tap"),
        "a 128K release went into a 48K machine, where it runs out of memory");
  }

  /**
   * Somebody picked one, so that is the one, whatever the file and its name have to say.
   * <p>
   * Most tapes cannot say: a game that loads once and then looks for a sound chip only finds out
   * at run time, and Marauder is one - a plain 48K load with AY music and nowhere to declare it.
   * Being able to say so from the browser is the only way to hear it.
   */
  @Test
  void a_machine_somebody_picked_beats_what_the_name_says() throws Exception {
    assertEquals("Spectrum 128K", machineFor("SomeOrdinaryGame.tap", "Spectrum 128K"),
        "the choice was made and ignored");
    assertEquals("Spectrum 48K", machineFor("DarkTransit2(128K).tap", "Spectrum 48K"),
        "and it wins over the name too, which is the point of offering it");
  }

  @Test
  void everything_else_keeps_the_48k_machine_it_always_had() throws Exception {
    assertEquals("Spectrum 48K", machineFor("DarkTransit2(48K).tap"));
    assertEquals("Spectrum 48K", machineFor("SomeOrdinaryGame.tap"));
  }
}
