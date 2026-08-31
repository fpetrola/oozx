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
public class RzxPlayerInternalFrame extends JInternalFrame {

  /** A Spectrum frame, so a paced replay runs at the speed it was recorded at. */
  private static final int FRAME_MILLIS = 20;
  private static final int REFRESH_MILLIS = 100;
  /**
   * How near its resting place a drag has to finish for the player to attach.
   * <p>
   * Measured from where it would sit, which is a seam inside the machine's outline, so the reach
   * has to cover that seam as well as the slack in somebody's aim.
   */
  private static final int STICKY = 40;
  /** How long the window has to stand still before a drag counts as finished. */
  private static final int SETTLE_MILLIS = 180;
  /**
   * -Drzx.dock.trace=true prints what the docking decides and why.
   * <p>
   * Here because a report of "it snaps and then does not follow" could not be reproduced from
   * the outside: the decision is right in a test that walks the same steps, so what is wanted is
   * what the running program sees rather than another guess about it.
   */
  private static final boolean TRACE = Boolean.getBoolean("rzx.dock.trace");

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

  /** The emulator window showing this player's machine, while there is one. */
  private JInternalFrame machineWindow;

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
  private final PartsTableModel model = new PartsTableModel();
  private final JTable table = new JTable(model);
  /** The list of parts, which is everything the compact form hides. */
  private final JScrollPane parts = new JScrollPane(table);
  private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

  /**
   * Where the player sits against its machine's window, if it sits against it at all.
   * <p>
   * Bottom by default because that is where it belongs: the controls for a picture go under the
   * picture, the same width, the way the transport bar of anything else does.
   */
  public enum Dock { FREE, BOTTOM, TOP, LEFT, RIGHT }

  private Dock dock = Dock.BOTTOM;
  private boolean compact = true;
  /**
   * The height somebody dragged this to, which docking has to respect.
   * <p>
   * Before this, every placement recomputed the height, so moving the window - or moving the
   * machine, which moves the window - handed back a size nobody asked for and threw away the one
   * they had set.
   */
  private int chosenHeight;
  /**
   * The last height this code set for itself.
   * <p>
   * Resize events are POSTED, not delivered on the spot, so one describing a height this code
   * chose can arrive long afterwards - after {@link #compact} has been turned off, for instance,
   * at which point the listener below reads it as "the person wants it this tall" and records the
   * compact height as their preference. Expanding then does nothing at all, about half the time.
   * An event carrying the height we just set is not somebody resizing the window.
   */
  private int placedHeight = -1;
  private final Progress progress = new Progress();
  /**
   * Set while THIS code is moving the window, so that its own placement is not mistaken for
   * somebody dragging it. Without it, docking moves the window, the move looks like a drag, the
   * drag re-docks, and the two feed each other.
   */
  private boolean placing;
  private JToggleButton dockButton;
  /** Fires once the window has stopped being dragged; see the listener that restarts it. */
  private Timer settle;
  private JToggleButton expandButton;
  /** Watches the machine's window so the player follows it about while attached. */
  private ComponentAdapter machineWatcher;

  public RzxPlayerInternalFrame(int number, Function<RzxPlayerInternalFrame, File> chooseRecording,
                                BiConsumer<RzxPlayerInternalFrame, RzxSession> showMachine) {
    super("RZX #" + number, true, true, true, true);
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

    JButton favoriteButton =
        EmulatorInternalFrame.iconButton("2B50.svg", "Favorite", "Keep this recording");
    favoriteButton.addActionListener(e -> {
      if (onFavorite != null) onFavorite.run();
    });
    controls.add(favoriteButton);

    expandButton = EmulatorInternalFrame.iconToggle("expand-panel.svg", "Expand",
        "Show what is in the recording, or just the controls");
    expandButton.addActionListener(e -> setCompact(!expandButton.isSelected()));
    controls.add(expandButton);

    dockButton = EmulatorInternalFrame.iconToggle("dock-bottom.svg", "Attach",
        "Keep this under the machine's window, the same width as it");
    dockButton.setSelected(true);
    dockButton.addActionListener(e -> {
      dock = dockButton.isSelected() ? Dock.BOTTOM : Dock.FREE;
      place();
    });
    controls.add(dockButton);
    EmulatorInternalFrame.tighten(controls);

    table.setRowHeight(22);
    table.getColumnModel().getColumn(0).setPreferredWidth(130);
    table.getColumnModel().getColumn(1).setPreferredWidth(400);
    table.getColumnModel().getColumn(2).setPreferredWidth(140);
    table.getColumnModel().getColumn(2).setCellRenderer(new ProgressRenderer());

    JPanel top = new JPanel(new BorderLayout());
    top.add(controls, BorderLayout.NORTH);
    // Under the buttons and across the whole width: while a recording plays, how far along it is
    // is the one thing worth seeing at a glance, and the compact form has no room for anything
    // that takes a row of its own.
    top.add(progress, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(top, BorderLayout.NORTH);
    add(parts, BorderLayout.CENTER);
    parts.setVisible(false);                    // compact until asked otherwise

    // Sticky edges: a drag that finishes near the machine's window attaches to whichever side it
    // finished nearest, and one that finishes away from it lets go.
    // Decided when the dragging STOPS, not at every position along the way. A drag is a stream
    // of moves, and most of them are nowhere near anything: judging each one in turn means a
    // quick drag downwards detaches the moment it clears the machine, and a drag across the
    // desktop attaches and detaches to whatever it passes. Waiting for the moves to stop asks
    // the only question that matters - where was it LEFT.
    settle = new Timer(SETTLE_MILLIS, e -> snapIfNear());
    settle.setRepeats(false);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(ComponentEvent moved) {
        if (!placing) {
          settle.restart();
        }
      }

      @Override
      public void componentResized(ComponentEvent resized) {
        // Only a resize that somebody did counts. The ones this code makes are how it gets
        // placed, and treating those as a preference is how a window slowly grows on its own.
        if (!placing && !compact && getHeight() != placedHeight) {
          chosenHeight = getHeight();
        }
      }
    });

    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
    refresh.start();
    addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
        refresh.stop();
        settle.stop();
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
    machineWindow = null;
    mode = Mode.EMPTY;
    model.fireTableDataChanged();
    refresh();
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

  /** The emulator window driven by this player, or null while there is none. */
  public JInternalFrame getMachineWindow() {
    return machineWindow;
  }

  public void setMachineWindow(JInternalFrame machineWindow) {
    if (this.machineWindow != null && machineWatcher != null) {
      this.machineWindow.removeComponentListener(machineWatcher);
    }
    this.machineWindow = machineWindow;
    if (machineWindow != null) {
      machineWatcher = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent moved) {
          if (TRACE) {
            System.out.printf("rzx dock: the machine moved to %s, dock is %s%n",
                RzxPlayerInternalFrame.this.machineWindow.getBounds(), dock);
          }
          place();
        }

        @Override
        public void componentResized(ComponentEvent resized) {
          place();
        }
      };
      machineWindow.addComponentListener(machineWatcher);
      place();
    }
  }

  /**
   * How tall this is with the list hidden: the toolbar and the title bar, and nothing else.
   * <p>
   * Asked of the frame rather than added up by hand. A JInternalFrame's insets are its border
   * alone - the title bar is a component inside it, not an inset - so adding the toolbar to the
   * insets comes out a title bar short, and the compact form loses its bottom row of buttons.
   * The layout already skips what is not visible, so with the list hidden the preferred height is
   * exactly the compact height.
   */
  private int compactHeight() {
    boolean showing = parts.isVisible();
    parts.setVisible(false);
    int tall = getPreferredSize().height;
    parts.setVisible(showing);
    return tall;
  }

  /** Puts the player against its machine, if it is attached to it. */
  private void place() {
    if (machineWindow == null || machineWindow.isClosed() || dock == Dock.FREE) {
      return;
    }
    Rectangle m = machineWindow.getBounds();
    int tall = compact ? compactHeight() : Math.max(compactHeight(), chosenHeight);
    placing = true;
    placedHeight = tall;
    try {
      switch (dock) {
        // Along the top or the bottom it takes the machine's width, which is the whole point of
        // putting it there: the controls for a picture, the width of the picture.
        // Overlapped by the two borders that meet, or the frames sit a seam apart: each draws
        // its own edge and the gap between the picture and the buttons is the sum of the two.
        case BOTTOM -> setBounds(m.x, m.y + m.height - seam(), m.width, tall);
        case TOP -> setBounds(m.x, Math.max(0, m.y - tall + seam()), m.width, tall);
        // Against a side it is only moved, never resized. Stretching it to the machine's height
        // turns a toolbar into a column of empty space, and a window that changes size because
        // it drifted near something else is a window fighting the person holding it.
        case LEFT -> setLocation(Math.max(0, m.x - getWidth() + sideSeam()), m.y);
        case RIGHT -> setLocation(m.x + m.width - sideSeam(), m.y);
        default -> { }
      }
    } finally {
      placing = false;
    }
  }

  /** The two borders that meet where the frames touch, which is what to overlap by. */
  private int seam() {
    return machineWindow == null ? 0 : machineWindow.getInsets().bottom + getInsets().top;
  }

  private int sideSeam() {
    return machineWindow == null ? 0 : machineWindow.getInsets().left + getInsets().right;
  }

  /**
   * Attaches to whichever side of the machine's window this one was left nearest, or lets go.
   * <p>
   * Only the sides it actually overlaps count: a player dragged well below and to the right of
   * the machine is near its bottom edge by one measure and near nothing by eye.
   */
  void snapIfNear() {
    if (machineWindow == null || machineWindow.isClosed()) {
      return;
    }
    Rectangle m = machineWindow.getBounds(), me = getBounds();
    Dock nearest = Dock.FREE;
    int closest = STICKY;
    // Measured against where it would SIT if attached, not against the machine's outline. The
    // two are a seam apart, and measuring to the outline meant that the moment docking placed
    // the window - a seam inside the edge - the next look found it exactly a seam away and let
    // go again. With the seam at 24 and the reach at 24, "closer than" was never true and the
    // window attached and detached in the same breath: it moved into place and never held.
    if (me.x < m.x + m.width && m.x < me.x + me.width) {
      int under = Math.abs(me.y - (m.y + m.height - seam()));
      if (under < closest) {
        closest = under;
        nearest = Dock.BOTTOM;
      }
      int over = Math.abs(me.y - (m.y - me.height + seam()));
      if (over < closest) {
        closest = over;
        nearest = Dock.TOP;
      }
    }
    if (me.y < m.y + m.height && m.y < me.y + me.height) {
      int right = Math.abs(me.x - (m.x + m.width - sideSeam()));
      if (right < closest) {
        closest = right;
        nearest = Dock.RIGHT;
      }
      int left = Math.abs(me.x - (m.x - me.width + sideSeam()));
      if (left < closest) {
        closest = left;
        nearest = Dock.LEFT;
      }
    }
    if (TRACE) {
      System.out.printf("rzx dock: machine=%s player=%s -> %s (nearest %d px)%n",
          m, me, nearest, closest);
    }
    dock = nearest;
    dockButton.setSelected(nearest != Dock.FREE);
    place();
  }

  /** Which side of the machine's window this is attached to, or FREE. */
  Dock dockedTo() {
    return dock;
  }

  /** The compact form is the controls alone; expanded adds what is in the recording. */
  void setCompact(boolean wanted) {
    if (!wanted && chosenHeight < compactHeight() + 40) {
      chosenHeight = 300;                       // never expanded before: a sensible first size
    }
    compact = wanted;
    parts.setVisible(!wanted);
    if (dock == Dock.FREE) {
      placedHeight = wanted ? compactHeight() : chosenHeight;
      setSize(getWidth(), placedHeight);
    } else {
      place();
    }
    revalidate();
    repaint();
  }

  /**
   * How far along the recording is, as a line under the buttons.
   * <p>
   * Its own component rather than a JProgressBar: a few pixels tall is the point, and a progress
   * bar with a look and feel behind it has its own opinion about how short it is allowed to be.
   */
  private static class Progress extends JComponent {

    private double howFar;

    Progress() {
      setPreferredSize(new Dimension(10, 5));
    }

    void setHowFar(double howFar) {
      double clamped = Math.max(0, Math.min(1, howFar));
      if (Math.abs(clamped - this.howFar) > 0.0005) {
        this.howFar = clamped;
        repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics pen) {
      pen.setColor(getBackground().darker());
      pen.fillRect(0, 0, getWidth(), getHeight());
      pen.setColor(new Color(0x3F7FBF));
      pen.fillRect(0, 0, (int) Math.round(getWidth() * howFar), getHeight());
    }
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
    mode = Mode.STOPPED;
    model.fireTableDataChanged();
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
      setTitle(title("", busy));
      return;
    }
    if (!loaded) {
      setTitle(title("", "no recording - use Open Recording"));
      return;
    }
    RzxPlayback playback = session.getPlayback();
    progress.setHowFar(playback.getFrameCount() == 0 ? 0
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
