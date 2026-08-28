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

package com.fpetrola.oozx.speccy.rzx;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.RzxPlayback;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A machine set up to replay a recording: its ports read from the recording rather than from the
 * hardware, and it starts from the state the recording was made at.
 * <p>
 * The ports are replaced by overriding the binding of {@link IO}, which is why the machine itself
 * needs no say in it. The recording carries the machine's state at the moment recording began, as
 * a snapshot the parser hands over already expanded, so it is written out and loaded the same way
 * any snapshot is.
 */
public class RzxSession {

  private final RzxFile recording;
  private final Speccy speccy;
  private final RzxPlayback playback;

  private RzxSession(RzxFile recording, Speccy speccy, RzxPlayback playback) {
    this.recording = recording;
    this.speccy = speccy;
    this.playback = playback;
  }

  public static RzxSession open(File file) {
    RzxFile recording = new RzxParser().parseFile(file.getAbsolutePath());

    RZXPlayerIO.stop = false;
    RZXPlayerIO player = new RZXPlayerIO();

    Speccy speccy = Guice.createInjector(
        Modules.override(new EmulatorModule(new SpectrumZ80Clock()))
            .with(new AbstractModule() {
              @Override
              protected void configure() {
                bind(IO.class).toInstance(player);
              }
            })).getInstance(Speccy.class);
    speccy.init();
    speccy.z80.bridgeCommand = (command, data) -> null;
    speccy.z80.loadSnap(snapshotFileOf(recording).toAbsolutePath().toString());

    return new RzxSession(recording, speccy, new RzxPlayback(speccy.z80.ooz80, player, recording));
  }

  /**
   * The state the recording starts from, on disk. It is loaded through the ordinary snapshot
   * path rather than decoded here: the format reader deliberately knows nothing about snapshots.
   */
  private static Path snapshotFileOf(RzxFile recording) {
    try {
      String extension = recording.getSnapshotBlock().getSnapshotExtension();
      Path file = Files.createTempFile("rzx-snapshot", "." + (extension == null ? "z80" : extension));
      file.toFile().deleteOnExit();
      Files.write(file, recording.getSnapshotBlock().getSnapshotData());
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException("could not write the recording's snapshot", e);
    }
  }

  public Speccy getSpeccy() {
    return speccy;
  }

  public RzxPlayback getPlayback() {
    return playback;
  }

  public RzxFile getRecording() {
    return recording;
  }
}
