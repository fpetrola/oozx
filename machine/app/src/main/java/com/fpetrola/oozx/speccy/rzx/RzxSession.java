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
  private com.fpetrola.oozx.speccy.peripherals.EmulatorCore core;
  private final SwitchableIO ports;
  /** Replaced by {@link #rewind()}: a driver counts from where it started and cannot go back. */
  private RzxPlayback playback;
  /** The recording's own starting state, written out once and read again to go back to it. */
  private Path snapshot;

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
    /** Replaced on a rewind: a player holds its place in the recording and does not seek. */
    private volatile RZXPlayerIO player;
    private volatile IO hardware;
    private volatile boolean replaying = true;

    SwitchableIO(RZXPlayerIO player) {
      this.player = player;
    }

    /**
     * Only the reads come from the recording. What the machine WRITES still has to reach the
     * hardware: the border, the speaker and anything else a port drives are the machine doing
     * its job, not input being replayed. Sending writes to the player instead left the replay
     * silent and the border dead, since the ULA never saw an out at all.
     */
    public int in(int port) {
      return replaying ? player.in(port) : hardware.in(port);
    }

    public void out(int port, int value) {
      player.out(port, value); // it tracks the last value written to 0xFE
      if (hardware != null) {
        hardware.out(port, value);
      }
    }

  }

  /** The model the recording was made on, which is the one the machine became to replay it. */
  public String getMachineName() {
    return speccy.machine.current.getName();
  }

  public static RzxSession open(File file) {
    RzxFile recording = new RzxParser().parseFile(file.getAbsolutePath());
    // The parser answers null for anything it does not recognise rather than saying so, and a
    // recording without a snapshot starts from a machine state it does not carry, which this
    // cannot supply. Both were reaching the caller as a NullPointerException from somewhere else.
    if (recording == null) {
      throw new IllegalArgumentException(file.getName() + " is not a recording this can read");
    }
    if (recording.getSnapshotBlock() == null || recording.getSnapshotBlock().getSnapshotData() == null) {
      throw new IllegalArgumentException(
          file.getName() + " carries no snapshot, so there is no state to start it from");
    }

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
    // Before init, because that is when the sound works out how big a frame of audio is: at the
    // default 200x it comes to four samples a frame instead of the eight hundred odd a real one
    // has, and a replay is heard through that. Pacing is this session's own job either way.
    speccy.settings.current.emulationSpeed = 100;
    speccy.init();
    speccy.z80.bridgeCommand = (command, data) -> null;
    com.fpetrola.oozx.speccy.peripherals.EmulatorCore core =
        new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(speccy);
    speccy.z80.mockCore = core;
    // BEFORE the snapshot, because loading one WRITES: it puts back the paging the snapshot was
    // saved under by sending it through the machine's own port, and these are the ports it goes
    // through. Set afterwards, as it was, that write went to a null and was dropped without a
    // word - the machine kept whatever selecting it had left, which is bank 0 and the 128K ROM.
    // Games that never call the ROM did not notice; Bad Demo takes its interrupt into it, and
    // ran the 128K editor's handler - reading the AY and scanning the keyboard, twenty-two port
    // reads in a frame the recording says had one.
    ports.hardware = injector.getInstance(PeripheralIO.class);
    Path snapshot = snapshotFileOf(recording);
    speccy.z80.loadSnap(snapshot.toAbsolutePath().toString());

    // After loadSnap, because that is what decides which machine this is: a 128K frame is 70908
    // T-states against a 48K one's 69888, and the driver subtracts a frame's worth at every
    // boundary to keep the clock inside a frame. Handing it the wrong length leaves the clock
    // drifting by a thousand T-states a frame, which the sound and the display both read.
    RzxSession session = new RzxSession(recording, speccy,
        new RzxPlayback(speccy.z80.ooz80, player, recording, framesTStatesOf(speccy)).steppedBy(speccy.z80::step), ports);
    session.timer = injector.getInstance(com.fpetrola.oozx.speccy.modules.Timer.class);
    session.core = core;
    session.snapshot = snapshot;
    return session;
  }

  /**
   * Back to the recording's first frame, on the machine that is already running it.
   * <p>
   * Starting a recording over used to mean building the whole thing again - a new machine, and
   * so a new window for it, because a window is tied to the machine it shows. Pressing Stop
   * therefore threw away the window you were watching and put another in its place. Nothing
   * about going back to the beginning needs that: the recording carries the state it began
   * from, and putting that state back into this machine is what starting over means.
   * <p>
   * What has to be new is the pair that keeps a place: a player holds its position in the
   * recording and a driver counts fetches from where it started, and neither seeks backwards.
   * They are made again over the same machine.
   * <p>
   * The ports go back to reading the recording, so this also undoes a Take Over - which is the
   * other thing someone means by Stop.
   */
  public void rewind() {
    RZXPlayerIO player = new RZXPlayerIO();
    ports.player = player;
    ports.replaying = true;
    finished = false;
    speccy.z80.loadSnap(snapshot.toAbsolutePath().toString());
    playback = new RzxPlayback(speccy.z80.ooz80, player, recording, framesTStatesOf(speccy)).steppedBy(speccy.z80::step);
  }

  /** How long a frame is on the machine currently selected. */
  private static int framesTStatesOf(Speccy speccy) {
    return speccy.machine.current.getTimings().tstatesPerFrame;
  }

  /** What the player throws when the recording has no more frames to give. */
  private static final String END_OF_RECORDING = "rzx finished";

  private volatile boolean finished;

  /** True once the recording has been played to its end. */
  public boolean isFinished() {
    return finished || playback.isFinished();
  }

  private com.fpetrola.oozx.speccy.modules.Timer timer;

  /**
   * Plays one frame of the recording and shows it.
   * <p>
   * Driving the processor means the machine's own loop never runs, and that loop is what asks the
   * sound and the display for a frame, so both are asked here. Without the sound one a replay is
   * silent, which is not the emulator being quiet - it is nobody telling it a frame went by.
   * <p>
   * They are asked once per frame OF THE CLOCK, not once per recorded frame. A recorded frame is
   * a count of fetches and runs a little over or under a frame's worth of T-states; the sound
   * closes its frame at a fixed 69888 regardless, so asking it on the recording's rhythm hands it
   * beeper changes belonging either side of where it cuts. That comes out as an echo.
   *
   * @return false at the end of the recording
   */
  public boolean playFrame() {
    // This is how a machine driven by a recording advances, so it is where what was deferred
    // gets done - the ordinary loop does it at the end of doOpcodes, which never runs here.
    speccy.z80.applyWhatWasDeferred();
    try {
      if (!playback.playFrame()) {
        return false;
      }
    } catch (RuntimeException e) {
      // THE END OF A RECORDING ARRIVES AS AN EXCEPTION, deliberately: the player throws it from
      // the one place that can tell the tail apart from a frame that merely ran out of values,
      // and runners outside this use it as their stop signal. What it is not is a failure, and
      // letting it out of here was: it left the window's thread and took the thread with it, so
      // a recording shorter than the session sat frozen with nothing said. Three of fourteen
      // sampled from the archive end this way - they are 1885, 8259 and 13449 frames long.
      if (!END_OF_RECORDING.equals(e.getMessage())) {
        throw e;
      }
      finished = true;
      return false;
    }
    for (int frames = playback.takeElapsedMachineFrames(); frames > 0; frames--) {
      if (speccy.sound.soundEnabled) {
        speccy.sound.frame();
      }
      speccy.display.frame();
    }
    return true;
  }

  /**
   * Hands the ports back to the hardware: the machine carries on as an ordinary emulator.
   * <p>
   * The event queue has to be restarted along with them. While a recording plays, the machine's
   * loop never runs and so nothing services its events, which pile up overdue - the frame event
   * among them, and that one raises the interrupt. Handing the machine back without clearing them
   * runs every overdue event at once: the game races through whatever it had left and stops.
   */
  public void release() {
    if (isReleased()) {
      return;
    }
    speccy.eventManager.reset();
    speccy.zxClock.setTStates(0);
    // The timer's event as well as the frame's, in that order, the way selecting a machine does
    // it. Not for the pacing alone: with only one event the queue empties every frame, and an
    // empty queue puts eventNextEvent back to -1 with the same result as below.
    timer.addEvent();
    speccy.eventManager.eventAdd(framesTStatesOf(speccy), speccy.machine.current.spectrumFrameEvent);
    // reset leaves eventNextEvent at EVENT_NO_EVENTS, which is 0xffffffff held in a long: -1.
    // Adding an event does not lower it, since nothing is less than -1, so the queue reads as
    // due forever and eventDoEvents takes events that have not come round yet. eventFrame(0)
    // moves nothing and recomputes the value from the queue, which is what puts it right.
    speccy.eventManager.eventFrame(0);
    ports.replaying = false;
  }

  public boolean isReleased() {
    return !ports.replaying;
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

  /** What a window drives this recording's machine with, which the session builds because it built the machine. */
  public com.fpetrola.oozx.speccy.peripherals.EmulatorCore getCore() {
    return core;
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
