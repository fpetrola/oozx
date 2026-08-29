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
import com.fpetrola.z80.ide.rzx.InputRecordingBlock;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxHeader;
import com.fpetrola.z80.ide.rzx.SnapshotBlock;
import com.fpetrola.z80.minizx.RzxPlayback;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Drives a recording: what is in the file, how far along it is, and play, pause and take over.
 * <p>
 * The picture is not here. A recording brings its own machine, and that machine goes into an
 * ordinary emulator window like any other, which is what makes {@link RzxSession#release() taking
 * over} mean something: stop the recording at an interesting moment and the same machine carries
 * on reading the keyboard, with the game exactly where the recording left it. This window is the
 * controls and the contents, the way the cassette browser is for a tape.
 * <p>
 * The machine runs on this window's thread either way, because while a recording plays it is the
 * recording that drives the processor, not the machine's clock. After taking over, the same
 * thread runs the ordinary loop instead.
 * <p>
 * How fast it plays comes from the machine's own speed setting, so the emulator window's speed
 * control works on a replay as it does on anything else.
 */
public class RzxPlayerInternalFrame extends JInternalFrame {

  /** A Spectrum frame, so a paced replay runs at the speed it was recorded at. */
  private static final int FRAME_MILLIS = 20;
  private static final int REFRESH_MILLIS = 100;

  private enum Mode { EMPTY, STOPPED, PLAYING, TAKEN_OVER, FINISHED }

  private final Supplier<File> chooseRecording;
  private final Consumer<RzxSession> showMachine;

  private File file;
  /** Where the open recording came from: a URL when fetched, the path when opened locally. */
  private String sourceUrl;
  /** Which file inside the archive it was, when the source was a zip. */
  private String sourceEntry;
  private Runnable onFavorite;
  private RzxSession session;
  private volatile Mode mode = Mode.EMPTY;
  private Thread thread;
  private volatile boolean alive;

  private final JButton openButton =
      EmulatorInternalFrame.iconButton("1F39E.svg", "Open Recording...", "Open a recording");
  private final JButton playButton =
      EmulatorInternalFrame.iconButton("25B6.svg", "Play", "Play the recording");
  private final JButton pauseButton =
      EmulatorInternalFrame.iconButton("23F8.svg", "Pause", "Pause the recording");
  private final JButton stopButton =
      EmulatorInternalFrame.iconButton("23F9.svg", "Stop", "Stop and go back to the start");
  private final JButton takeOverButton =
      EmulatorInternalFrame.iconButton("1F579.svg", "Take Over", null);
  private final JLabel status = new JLabel();
  private final PartsTableModel model = new PartsTableModel();
  private final JTable table = new JTable(model);

  public RzxPlayerInternalFrame(Supplier<File> chooseRecording, Consumer<RzxSession> showMachine) {
    super("RZX Player", true, true, true, true);
    this.chooseRecording = chooseRecording;
    this.showMachine = showMachine;
    setSize(720, 300);
    setLocation(120, 60);

    openButton.addActionListener(e -> open());
    playButton.addActionListener(e -> play());
    pauseButton.addActionListener(e -> mode = Mode.STOPPED);
    stopButton.addActionListener(e -> stop());
    takeOverButton.addActionListener(e -> takeOver());
    takeOverButton.setToolTipText(
        "Stops the recording and leaves the machine playable, exactly where the recording is");

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    // Same trimmed buttons as every other toolbar; without this they keep the default padding
    // and this window's buttons look bigger than the rest of the application's.
    controls.add(openButton);
    controls.add(playButton);
    controls.add(pauseButton);
    controls.add(stopButton);
    controls.add(takeOverButton);

    JButton favoriteButton =
        EmulatorInternalFrame.iconButton("2B50.svg", "Favorite", "Keep this recording");
    favoriteButton.addActionListener(e -> {
      if (onFavorite != null) onFavorite.run();
    });
    controls.add(favoriteButton);
    EmulatorInternalFrame.tighten(controls);

    table.setRowHeight(22);
    table.getColumnModel().getColumn(0).setPreferredWidth(130);
    table.getColumnModel().getColumn(1).setPreferredWidth(400);
    table.getColumnModel().getColumn(2).setPreferredWidth(140);
    table.getColumnModel().getColumn(2).setCellRenderer(new ProgressRenderer());

    JPanel top = new JPanel(new BorderLayout());
    top.add(controls, BorderLayout.NORTH);
    top.add(status, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(top, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
    refresh.start();
    addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
        refresh.stop();
        alive = false;
      }
    });
    refresh();
  }

  private String busy;

  /** Says something is being fetched, so a click that takes a while does not look ignored. */
  public void setBusy(String message) {
    busy = message;
    refresh();
  }

  /** The emulator window showing this recording's machine was closed: stop driving it. */
  public void machineClosed() {
    stopThread();
    session = null;
    mode = Mode.EMPTY;
    setTitle("RZX Player");
    model.fireTableDataChanged();
    refresh();
  }

  /** Loads a recording, builds its machine and hands that machine over to be shown. */
  public void setOnFavorite(Runnable onFavorite) {
    this.onFavorite = onFavorite;
  }

  /** Remembers what was fetched, so the same file can be found again inside the same archive. */
  public void setSource(String url, String entry) {
    this.sourceUrl = url;
    this.sourceEntry = entry;
  }

  public String getSourceUrl() {
    return sourceUrl != null ? sourceUrl : (file == null ? null : file.getAbsolutePath());
  }

  public String getSourceEntry() {
    return sourceEntry;
  }

  public String getRecordingName() {
    return file == null ? null : file.getName();
  }

  public void openRecording(File recording) {
    stopThread();
    file = recording;
    // Reading a recording happens on the event thread, and a long one can take a noticeable
    // while, during which the window would otherwise look like it had ignored the file.
    setBusy("Opening " + recording.getName() + "...");
    try {
      session = RzxSession.open(recording);
    } catch (RuntimeException e) {
      session = null;
      busy = null;
      mode = Mode.EMPTY;
      JOptionPane.showMessageDialog(this,
          "Could not read " + recording.getName() + ".\n\n" + ZXSpectrumDesktopApp.reason(e),
          "Open recording", JOptionPane.ERROR_MESSAGE);
      return;
    }

    busy = null;
    mode = Mode.STOPPED;
    setTitle("RZX Player - " + recording.getName());
    model.fireTableDataChanged();
    showMachine.accept(session);
    startThread();
    refresh();
  }

  /** The recording being played: for one out of an archive, the entry that was unpacked. */
  public File getRecordingFile() {
    return file;
  }

  private void open() {
    File chosen = chooseRecording.get();
    if (chosen != null) {
      openRecording(chosen);
    }
  }

  private void play() {
    if (session != null && mode != Mode.TAKEN_OVER && mode != Mode.FINISHED) {
      mode = Mode.PLAYING;
    }
  }

  /** Back to the beginning, which for a recording means building its machine again. */
  private void stop() {
    if (file != null) {
      openRecording(file);
    }
  }

  private void takeOver() {
    if (session == null || mode == Mode.TAKEN_OVER) {
      return;
    }
    session.release();
    mode = Mode.TAKEN_OVER;
  }

  private void startThread() {
    alive = true;
    thread = new Thread(this::run, "rzx-machine");
    thread.setDaemon(true);
    thread.start();
  }

  private void stopThread() {
    alive = false;
    Thread running = thread;
    thread = null;
    if (running != null && running != Thread.currentThread()) {
      try {
        running.join(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void run() {
    while (alive) {
      RzxSession current = session;
      if (current == null) {
        sleep(REFRESH_MILLIS);
        continue;
      }
      switch (mode) {
        case PLAYING -> {
          if (!current.playFrame()) {
            mode = Mode.FINISHED;
          } else {
            pace(current);
          }
        }
        // Taken over: the machine is an ordinary one again, so its own loop runs it.
        case TAKEN_OVER -> {
          current.getSpeccy().z80.doOpcodes();
          current.getSpeccy().eventManager.eventDoEvents();
        }
        default -> sleep(REFRESH_MILLIS);
      }
    }
  }

  /**
   * Holds the replay to the machine's own speed setting, so the emulator window's speed control
   * governs it like it governs everything else. This window used to have a Full speed box of its
   * own, which meant two controls for one thing and neither of them right.
   */
  private void pace(RzxSession current) {
    int speed = Math.max(1, current.getSpeccy().settings.current.emulationSpeed);
    long millis = FRAME_MILLIS * 100L / speed;
    if (millis > 0) {
      sleep((int) millis);
    }
  }

  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void refresh() {
    boolean loaded = session != null;
    playButton.setEnabled(loaded && mode == Mode.STOPPED);
    pauseButton.setEnabled(mode == Mode.PLAYING);
    stopButton.setEnabled(loaded);
    takeOverButton.setEnabled(loaded && mode != Mode.TAKEN_OVER);

    if (busy != null) {
      status.setText(" " + busy);
      return;
    }
    if (!loaded) {
      status.setText(" No recording - use Open Recording");
      return;
    }
    RzxPlayback playback = session.getPlayback();
    String where = playback.getFrameIndex() + " of " + playback.getFrameCount() + " frames";
    String state = switch (mode) {
      case PLAYING -> "Playing";
      case TAKEN_OVER -> "Taken over - the machine is yours, playing on from here";
      case FINISHED -> "Recording finished";
      default -> "Stopped";
    };
    // The model is worth showing: it is not a setting anyone chose, it is what the recording
    // said it was made on, and the machine became that to replay it.
    status.setText(" " + state + " - " + where + " - " + session.getMachineName());
    model.fireTableRowsUpdated(0, Math.max(0, model.getRowCount() - 1));
  }

  /** One row per part of the file, the way the cassette browser lists a tape's blocks. */
  private class PartsTableModel extends AbstractTableModel {
    private final String[] columns = {"Part", "Details", "Progress"};

    public int getRowCount() {
      return session == null ? 0 : 4;
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return false; // a listing, not a form
    }

    public Object getValueAt(int row, int column) {
      RzxFile recording = session.getRecording();
      if (column == 2) {
        return row == 3 ? progressOfRecording() : 100;
      }
      return switch (row) {
        case 0 -> column == 0 ? "Header" : headerOf(recording.getHeader());
        case 1 -> column == 0 ? "Creator" : creatorOf(recording.getCreatorInfo());
        case 2 -> column == 0 ? "Snapshot" : snapshotOf(recording.getSnapshotBlock());
        default -> column == 0 ? "Input recording" : inputOf(recording.getInputRecordingBlock());
      };
    }

    private int progressOfRecording() {
      RzxPlayback playback = session.getPlayback();
      return playback.getFrameCount() == 0 ? 0
          : playback.getFrameIndex() * 100 / playback.getFrameCount();
    }

    private String headerOf(RzxHeader header) {
      return header == null ? "" : String.format("%s, version %d.%d",
          header.signature == null ? "RZX" : header.signature.trim(),
          header.majorRevision, header.minorRevision);
    }

    private String creatorOf(CreatorInfo creator) {
      return creator == null ? "unknown" : String.format("%s %d.%d",
          creator.creatorId == null ? "unknown" : creator.creatorId.trim(),
          creator.majorVersion, creator.minorVersion);
    }

    private String snapshotOf(SnapshotBlock snapshot) {
      if (snapshot == null) {
        return "none - the recording starts from whatever is in the machine";
      }
      return String.format("%s, %d bytes%s", snapshot.getSnapshotExtension(),
          snapshot.getSnapshotData() == null ? 0 : snapshot.getSnapshotData().length,
          snapshot.isCompressed() ? ", stored compressed" : "");
    }

    private String inputOf(InputRecordingBlock block) {
      if (block == null) {
        return "none";
      }
      long reads = 0;
      for (InputRecordingBlock.Frame frame : block.frames) {
        reads += frame.inCounter;
      }
      int seconds = block.frames.size() / 50;
      return String.format("%d frames, %d:%02d long, %d port reads%s",
          block.frames.size(), seconds / 60, seconds % 60, reads,
          block.isCompressed ? ", stored compressed" : "");
    }
  }

  private static class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    ProgressRenderer() {
      super(0, 100);
      setStringPainted(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                   boolean focused, int row, int column) {
      int progress = value instanceof Integer ? (Integer) value : 0;
      setValue(progress);
      setString(progress + "%");
      return this;
    }
  }
}
