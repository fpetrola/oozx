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

package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.speccy.rzx.RzxSession;
import com.fpetrola.z80.ide.rzx.CreatorInfo;
import com.fpetrola.z80.minizx.RzxPlayback;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.function.Supplier;

/**
 * Plays a recording and shows it running.
 * <p>
 * A recording brings its own machine: it starts from the state it was made at and its ports read
 * the recorded input, so it does not go into an emulator that is already running something. The
 * picture is that machine's own screen, put in this window.
 * <p>
 * Playing runs on its own thread, since the recording drives the processor rather than the
 * machine's clock, and it is paced to fifty frames a second so it can be watched. Unpaced it runs
 * about ten times that.
 */
public class RzxPlayerInternalFrame extends JInternalFrame {

  /** A Spectrum frame, so a paced replay runs at the speed it was recorded at. */
  private static final int FRAME_MILLIS = 20;
  private static final int REFRESH_MILLIS = 100;

  private final Supplier<File> chooseRecording;

  private File file;
  private RzxSession session;
  private Thread playing;
  private volatile boolean running;

  private final JButton openButton = new JButton("Open Recording...");
  private final JButton playButton = new JButton("Play");
  private final JButton pauseButton = new JButton("Pause");
  private final JButton stopButton = new JButton("Stop");
  private final JCheckBox fullSpeed = new JCheckBox("Full speed");
  private final JLabel status = new JLabel();
  private final JProgressBar progress = new JProgressBar(0, 1);
  private final JPanel screenHolder = new JPanel(new BorderLayout());

  public RzxPlayerInternalFrame(Supplier<File> chooseRecording) {
    super("RZX Player", true, true, true, true);
    this.chooseRecording = chooseRecording;
    setSize(700, 620);
    setLocation(120, 60);

    openButton.addActionListener(e -> open());
    playButton.addActionListener(e -> play());
    pauseButton.addActionListener(e -> pause());
    stopButton.addActionListener(e -> stop());

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    controls.add(openButton);
    controls.add(playButton);
    controls.add(pauseButton);
    controls.add(stopButton);
    controls.add(fullSpeed);

    progress.setStringPainted(true);
    JPanel top = new JPanel(new BorderLayout());
    top.add(controls, BorderLayout.NORTH);
    top.add(progress, BorderLayout.CENTER);
    top.add(status, BorderLayout.SOUTH);

    screenHolder.setBackground(Color.BLACK);
    setLayout(new BorderLayout());
    add(top, BorderLayout.NORTH);
    add(screenHolder, BorderLayout.CENTER);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
    refresh.start();
    addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
        refresh.stop();
        stopThread();
      }
    });
    refresh();
  }

  /** Loads a recording and builds the machine that will play it. */
  public void openRecording(File recording) {
    stopThread();
    file = recording;
    try {
      session = RzxSession.open(recording);
    } catch (RuntimeException e) {
      session = null;
      JOptionPane.showMessageDialog(this, "Could not read " + recording.getName() + ": " + e,
          "Open recording", JOptionPane.ERROR_MESSAGE);
      return;
    }

    screenHolder.removeAll();
    screenHolder.add(session.getSpeccy().z80.mockCore.getPanel(), BorderLayout.CENTER);
    screenHolder.revalidate();
    screenHolder.repaint();

    progress.setMaximum(session.getPlayback().getFrameCount());
    setTitle("RZX Player - " + recording.getName());
    refresh();
  }

  private void open() {
    File chosen = chooseRecording.get();
    if (chosen != null) {
      openRecording(chosen);
    }
  }

  private void play() {
    if (session == null || running) {
      return;
    }
    running = true;
    playing = new Thread(this::replay, "rzx-playback");
    playing.setDaemon(true);
    playing.start();
  }

  private void replay() {
    RzxPlayback playback = session.getPlayback();
    while (running && playback.playFrame()) {
      // The recording drives the processor, so nothing else advances the picture: render the
      // frame here, where the machine's own frame event would have.
      session.getSpeccy().display.frame();
      if (!fullSpeed.isSelected()) {
        try {
          Thread.sleep(FRAME_MILLIS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
    running = false;
  }

  private void pause() {
    stopThread();
  }

  /** Back to the beginning, which for a recording means building its machine again. */
  private void stop() {
    stopThread();
    if (file != null) {
      openRecording(file);
    }
  }

  private void stopThread() {
    running = false;
    Thread thread = playing;
    playing = null;
    if (thread != null && thread != Thread.currentThread()) {
      try {
        thread.join(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void refresh() {
    boolean loaded = session != null;
    openButton.setEnabled(true);
    playButton.setEnabled(loaded && !running);
    pauseButton.setEnabled(loaded && running);
    stopButton.setEnabled(loaded);

    if (!loaded) {
      status.setText(" No recording - use Open Recording");
      progress.setValue(0);
      progress.setString("");
      return;
    }

    RzxPlayback playback = session.getPlayback();
    progress.setValue(playback.getFrameIndex());
    progress.setString(playback.getFrameIndex() + " / " + playback.getFrameCount() + " frames");

    String state = playback.isFinished() ? "Finished" : running ? "Playing" : "Stopped";
    status.setText(" " + state + " - " + describe());
  }

  private String describe() {
    CreatorInfo creator = session.getRecording().getCreatorInfo();
    String by = creator == null || creator.creatorId == null ? "unknown" : creator.creatorId.trim();
    int seconds = session.getPlayback().getFrameCount() / 50;
    return String.format("recorded by %s, %d:%02d long, snapshot %s",
        by, seconds / 60, seconds % 60,
        session.getRecording().getSnapshotBlock().getSnapshotExtension());
  }
}
