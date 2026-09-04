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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
public class RzxPlayerInternalFrame extends AttachedFrame {

  /** A Spectrum frame, so a paced replay runs at the speed it was recorded at. */
  private static final int FRAME_MILLIS = 20;
  private static final int REFRESH_MILLIS = 100;

  private enum Mode { EMPTY, STOPPED, PLAYING, TAKEN_OVER, FINISHED }

  /**
   * Both of these are asked of whoever owns the desktop, and both are given this window, because
   * there can be several players open at once and the answer differs per player: which file this
   * one is opening, and which machine window belongs to this one rather than to its neighbour.
   */
  private final Function<RzxPlayerInternalFrame, File> chooseRecording;
  private final BiConsumer<RzxPlayerInternalFrame, RzxSession> showMachine;

  /**
   * Which player this is. It goes in this window's title and in its machine's, so that with four
   * of them open it is possible to tell at a glance which controls drive which picture.
   */
  private final int number;

  private File file;
  /** Where the open recording came from: a URL when fetched, the path when opened locally. */
  private String sourceUrl;
  /** Which file inside the archive it was, when the source was a zip. */
  private String sourceEntry;
  private String pendingUrl;
  private String pendingEntry;
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
  private final JToggleButton loopButton =
      EmulatorInternalFrame.iconToggle("1F501.svg", "Loop", "Start again when the recording ends");
  private final PartsTableModel model = new PartsTableModel();
  private final JTable table = new JTable(model);
  /** The list of parts, which is everything the compact form hides. */
  private final JScrollPane parts = new JScrollPane(table);


  public RzxPlayerInternalFrame(int number, Function<RzxPlayerInternalFrame, File> chooseRecording,
                                BiConsumer<RzxPlayerInternalFrame, RzxSession> showMachine) {
    super("RZX #" + number);
    this.number = number;
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

    // The controls panel is a field now: the compact form is measured from it.
    // Same trimmed buttons as every other toolbar; without this they keep the default padding
    // and this window's buttons look bigger than the rest of the application's.
    controls.add(openButton);
    controls.add(playButton);
    controls.add(pauseButton);
    controls.add(stopButton);
    controls.add(takeOverButton);
    loopButton.setSelected(true);
    controls.add(loopButton);

    JButton favoriteButton =
        EmulatorInternalFrame.iconButton("2B50.svg", "Favorite", "Keep this recording");
    favoriteButton.addActionListener(e -> {
      if (onFavorite != null) onFavorite.run();
    });
    controls.add(favoriteButton);

    table.setRowHeight(22);
    table.getColumnModel().getColumn(0).setPreferredWidth(130);
    table.getColumnModel().getColumn(1).setPreferredWidth(400);
    table.getColumnModel().getColumn(2).setPreferredWidth(140);
    table.getColumnModel().getColumn(2).setCellRenderer(new ProgressRenderer());

    assemble(parts);

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
  @Override
  protected void machineClosed() {
    stopThread();
    session = null;
    mode = Mode.EMPTY;
    model.fireTableDataChanged();
    refresh();
    super.machineClosed();
  }

  @Override
  protected String expandTip() {
    return "Show what is in the recording, or just the controls";
  }

  @Override
  protected String attachTip() {
    return "Keep this under the machine's window, the same width as it";
  }

  /** Loads a recording, builds its machine and hands that machine over to be shown. */
  /**
   * The window's name doubles as its status line. There was a row under the toolbar saying what
   * the player was doing, which cost a strip of the window on every frame to say what the title
   * bar had room for anyway.
   */
  private String title(String name, String state) {
    return "RZX #" + number + (name.isEmpty() ? "" : ": " + name)
        + (state == null || state.isEmpty() ? "" : " - " + state);
  }

  /** Whether this player has a recording open, or is a blank window waiting for one. */
  public boolean hasRecording() {
    return session != null || file != null;
  }

  /** Which player this is, for whoever names the machine's window to match. */
  public int getNumber() {
    return number;
  }


  public void setOnFavorite(Runnable onFavorite) {
    this.onFavorite = onFavorite;
  }

  /**
   * Where the recording about to be opened really came from, for whoever knows more than the
   * file does. A recording out of an archive — fetched or picked off the disk — is unpacked
   * into a temporary directory, so the file being played is not somewhere worth remembering:
   * what lasts is the archive it came out of and which entry it was.
   */
  public void setPendingSource(String url, String entry) {
    this.pendingUrl = url;
    this.pendingEntry = entry;
  }

  public String getSourceUrl() {
    return sourceUrl;
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
    // Taken once and cleared, so a recording opened after another does not inherit where the
    // previous one came from. With nothing pending, a plain file is its own lasting source.
    sourceUrl = pendingUrl != null ? pendingUrl : recording.getAbsolutePath();
    sourceEntry = pendingEntry;
    pendingUrl = null;
    pendingEntry = null;
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
    mode = Mode.PLAYING;
    model.fireTableDataChanged();
    // The window the previous recording was on is let go of before it is closed: closed while
    // still held, it reads as the person closing the machine, which drops the recording just opened.
    JInternalFrame previous = getMachineWindow();
    setMachineWindow(null);
    if (previous != null && !previous.isClosed()) previous.dispose();
    showMachine.accept(this, session);
    startThread();
    refresh();
  }

  /** The recording being played: for one out of an archive, the entry that was unpacked. */
  public File getRecordingFile() {
    return file;
  }

  private void open() {
    // Anything thrown inside a listener is printed to the console by Swing and nowhere else,
    // which looks exactly like the button doing nothing. Say so in front of the person instead.
    try {
      File chosen = chooseRecording.apply(this);
      if (chosen != null) {
        openRecording(chosen);
      }
    } catch (RuntimeException e) {
      setBusy(null);
      JOptionPane.showMessageDialog(this, "Could not open a recording.\n\n"
          + ZXSpectrumDesktopApp.reason(e), "Open recording", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void play() {
    if (session != null && mode != Mode.TAKEN_OVER && mode != Mode.FINISHED) {
      mode = Mode.PLAYING;
    }
  }

  /** Back to the beginning, which for a recording means building its machine again. */
  /**
   * Back to the first frame, on the machine already on screen. Stopping used to reopen the file,
   * which built a whole new machine and so a new window, because a session could not be wound
   * back; it can now, so the window someone was watching stays where it is and Play runs the
   * recording again from the start.
   */
  private void stop() {
    if (session != null) {
      session.rewind();
      mode = Mode.STOPPED;
      refresh();
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
            if (loopButton.isSelected()) current.rewind(); else mode = Mode.FINISHED;
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
  private long nextFrameAt;

  private void pace(RzxSession current) {
    int speed = Math.max(1, current.getSpeccy().settings.current.emulationSpeed);
    // A frame's share of the time, kept to the nanosecond and owed forward: a sleep is whole
    // milliseconds, and at anything past 2000% a frame is less than one, so sleeping per frame
    // slept nothing at all. Sleep when a millisecond is owed; after a stall, start over.
    long frameNanos = FRAME_MILLIS * 1_000_000L * 100 / speed;
    long now = System.nanoTime();
    if (nextFrameAt < now - 100_000_000L) nextFrameAt = now;
    nextFrameAt += frameNanos;
    long owed = nextFrameAt - now;
    if (owed >= 1_000_000L) {
      sleep((int) (owed / 1_000_000L));
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
      setTitle(title("", busy));
      return;
    }
    if (!loaded) {
      setTitle(title("", "no recording - use Open Recording"));
      return;
    }
    RzxPlayback playback = session.getPlayback();
    showProgress(playback.getFrameCount() == 0 ? 0
        : (double) playback.getFrameIndex() / playback.getFrameCount());
    String where = playback.getFrameIndex() + " of " + playback.getFrameCount() + " frames";
    String state = switch (mode) {
      case PLAYING -> "Playing";
      case TAKEN_OVER -> "Taken over - the machine is yours, playing on from here";
      case FINISHED -> "Recording finished";
      default -> "Stopped";
    };
    // The model is worth showing: it is not a setting anyone chose, it is what the recording
    // said it was made on, and the machine became that to replay it.
    setTitle(title(file == null ? "" : file.getName(),
        state + " - " + where + " - " + session.getMachineName()));
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
