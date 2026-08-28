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
import com.fpetrola.oozx.speccy.modules.z80.PeripheralIO;
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
 * <p>
 * The replacement is a switch rather than a substitution: {@link #release()} puts the real ports
 * back, and from then on the machine is an ordinary one that reads its keyboard, carrying whatever
 * state the recording left it in. That is the point of playing a recording in a window like any
 * other emulator's - stop watching at an interesting moment and take over.
 */
public class RzxSession {

  private final RzxFile recording;
  private final Speccy speccy;
  private final RzxPlayback playback;
  private final SwitchableIO ports;

  private RzxSession(RzxFile recording, Speccy speccy, RzxPlayback playback, SwitchableIO ports) {
    this.recording = recording;
    this.speccy = speccy;
    this.playback = playback;
    this.ports = ports;
  }

  /**
   * The machine's ports, which start out reading the recording and can be handed back to the
   * hardware. Nothing else is switched: the machine goes on from wherever the recording left it.
   */
  private static class SwitchableIO implements IO {
    private volatile IO delegate;

    SwitchableIO(IO delegate) {
      this.delegate = delegate;
    }

    public int in(int port) {
      return delegate.in(port);
    }

    public void out(int port, int value) {
      delegate.out(port, value);
    }
  }

  public static RzxSession open(File file) {
    RzxFile recording = new RzxParser().parseFile(file.getAbsolutePath());

    RZXPlayerIO.stop = false;
    RZXPlayerIO player = new RZXPlayerIO();

    SwitchableIO ports = new SwitchableIO(player);
    com.google.inject.Injector injector = Guice.createInjector(
        Modules.override(new EmulatorModule(new SpectrumZ80Clock()))
            .with(new AbstractModule() {
              @Override
              protected void configure() {
                bind(IO.class).toInstance(ports);
              }
            }));
    Speccy speccy = injector.getInstance(Speccy.class);
    speccy.init();
    speccy.z80.bridgeCommand = (command, data) -> null;
    speccy.z80.loadSnap(snapshotFileOf(recording).toAbsolutePath().toString());

    RzxSession session =
        new RzxSession(recording, speccy, new RzxPlayback(speccy.z80.ooz80, player, recording), ports);
    session.realPorts = injector.getInstance(PeripheralIO.class);
    return session;
  }

  private PeripheralIO realPorts;

  /**
   * Plays one frame of the recording and shows it.
   * <p>
   * Driving the processor means the machine's own loop never runs, and that loop is what asks the
   * sound and the display for a frame, so both are asked here. Without the sound one a replay is
   * silent, which is not the emulator being quiet - it is nobody telling it a frame went by.
   *
   * @return false at the end of the recording
   */
  public boolean playFrame() {
    if (!playback.playFrame()) {
      return false;
    }
    if (speccy.sound.soundEnabled) {
      speccy.sound.frame();
    }
    speccy.display.frame();
    return true;
  }

  /** Hands the ports back to the hardware: the machine carries on as an ordinary emulator. */
  public void release() {
    ports.delegate = realPorts;
  }

  public boolean isReleased() {
    return ports.delegate == realPorts;
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
