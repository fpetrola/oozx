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


package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Peripheral;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A session: colours in a file beside a snapshot put the machine on the nine processors, and
 * everything that is not that game puts it back where it was.
 * <p>
 * No game's files here. A 48K snapshot is twenty-seven bytes of registers and the machine's RAM,
 * and colours are eight bytes for each of those, so both are written on the spot.
 */
class TheSessionOfAGameTest extends MachineTest {
  @TempDir
  Path where;

  private Speccy speccy;

  private Speccy machine() {
    speccy = silentMachine();
    select(speccy, speccy.machine.model(Spec48.class));
    return speccy;
  }

  private String snapshot(String name) throws IOException {
    byte[] sna = new byte[27 + 0xc000];
    sna[23] = 0x00;
    sna[24] = 0x40;                                  // the stack points somewhere harmless
    Path file = where.resolve(name + ".sna");
    Files.write(file, sna);
    return file.toString();
  }

  private void coloursFor(String name, int length) throws IOException {
    Files.write(where.resolve(name + ".GFX"), new byte[length]);
  }

  private void load(String url) {
    Snapshots.of(speccy).load(url);
    speccy.loop.applyWhatWasDeferred();
  }

  private Spec256Peripheral session() {
    return (Spec256Peripheral) speccy.peripheralRegistry.find(Spec256Peripheral.class);
  }

  @Test
  void aSnapshotWithItsColoursBesideItPutsTheMachineOnTheNine() throws IOException {
    machine();
    String was = speccy.processors.current();
    coloursFor("game", Planes.LENGTH);

    load(snapshot("game"));

    assertEquals("Spec256", speccy.processors.current(), "the colours are there, so the machine carries them");
    assertEquals("game.GFX", session().playing(), "and it says which game's they are");
    assertNotEquals("Spec256", was, "which is not where it started");
  }

  @Test
  void aSnapshotThatBringsNoColoursLeavesTheMachineWhereItWas() throws IOException {
    machine();
    String was = speccy.processors.current();

    load(snapshot("plain"));

    assertEquals(was, speccy.processors.current());
    assertNull(session().playing());
  }

  @Test
  void theMachineGoesBackToTheProcessorItWasOnWhenTheNextSnapshotBringsNothing() throws IOException {
    machine();
    String was = speccy.processors.current();
    coloursFor("game", Planes.LENGTH);
    load(snapshot("game"));

    load(snapshot("plain"));

    assertEquals(was, speccy.processors.current(), "back where it came from, not on some default");
    assertNull(session().playing());
  }

  @Test
  void aResetEndsTheSession() throws IOException {
    machine();
    String was = speccy.processors.current();
    coloursFor("game", Planes.LENGTH);
    load(snapshot("game"));

    speccy.machine.reset(true);
    speccy.loop.applyWhatWasDeferred();

    assertEquals(was, speccy.processors.current());
    assertNull(session().playing());
  }

  @Test
  void aChangeOfMachineEndsTheSession() throws IOException {
    machine();
    String was = speccy.processors.current();
    coloursFor("game", Planes.LENGTH);
    load(snapshot("game"));

    select(speccy, speccy.machine.model(com.fpetrola.oozx.speccy.machine.Spec128.class));
    speccy.loop.applyWhatWasDeferred();

    assertEquals(was, speccy.processors.current());
    assertNull(session().playing());
  }

  @Test
  void aFileOfTheWrongSizeBesideASnapshotIsNotAGamesColours() throws IOException {
    machine();
    String was = speccy.processors.current();
    coloursFor("game", Planes.LENGTH - 1);

    load(snapshot("game"));

    assertEquals(was, speccy.processors.current(), "a machine that would not paint right does not start");
    assertNull(session().playing());
  }
}
