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

package com.fpetrola.oozx.fuse.peripherals.t;

import com.fpetrola.oozx.fuse.modules.tape.Tape;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shows what is on a tape and lets it be driven by hand: the blocks and their details, which one
 * is being read, how far into it the player has got, and play, pause and stop.
 * <p>
 * The cassette belongs to the window, not to a machine: open a tape here and its composition can
 * be read with no emulator running at all. Playing it needs one, and it goes into whichever
 * emulator is in front at that moment - opening one afterwards, or switching to another, and
 * pressing play again puts the same cassette into that one.
 * <p>
 * The deck is driven with {@code play(true)}, the manual mode, so a "stop the tape" block is
 * honoured here as it should be: the person watching is the one who decides when it starts again.
 * An automatic load runs through those instead, because there is nobody to press anything.
 */
public class TapeBrowserInternalFrame extends JInternalFrame {

  /** How often the progress column is refreshed. The tape moves on the emulation thread. */
  private static final int REFRESH_MILLIS = 100;

  private final Supplier<Tape> activeTape;
  private final Runnable openTapeChooser;
  private final Consumer<File> loadInNewEmulator;
  private final BlockTableModel model;
  private final JTable table;
  private final JLabel status;
  private final JButton playButton;
  private final JButton pauseButton;
  private final JButton stopButton;
  private final JButton insertButton;

  /** The cassette this window holds, which needs no emulator to be looked at. */
  private File tapeFile;
  private List<TapeBlock> blocks = List.of();

  /** The deck the cassette was last played into, or null if it has not been played yet. */
  private Tape deck;

  /** The block the player last reported starting, which is the one being read. */
  private volatile int currentBlock = -1;
  private boolean paused;

  /**
   * @param activeTape      supplies the deck of whatever machine is in front, or null if there is none
   * @param openTapeChooser   asks the user for a tape file and calls back {@link #openTape}
   * @param loadInNewEmulator opens a machine on a tape and lets it load itself from the start
   */
  public TapeBrowserInternalFrame(Supplier<Tape> activeTape, Runnable openTapeChooser,
                                  Consumer<File> loadInNewEmulator) {
    super("Cassette", true, true, true, true);
    this.activeTape = activeTape;
    this.openTapeChooser = openTapeChooser;
    this.loadInNewEmulator = loadInNewEmulator;

    setSize(720, 420);
    setLocation(80, 80);

    model = new BlockTableModel();
    table = new JTable(model);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowHeight(22);
    table.getColumnModel().getColumn(0).setPreferredWidth(40);
    table.getColumnModel().getColumn(1).setPreferredWidth(150);
    table.getColumnModel().getColumn(2).setPreferredWidth(320);
    table.getColumnModel().getColumn(3).setPreferredWidth(70);
    table.getColumnModel().getColumn(4).setPreferredWidth(120);
    table.getColumnModel().getColumn(4).setCellRenderer(new ProgressRenderer());
    table.setDefaultRenderer(Object.class, new CurrentBlockRenderer());

    playButton = new JButton("Play");
    pauseButton = new JButton("Pause");
    stopButton = new JButton("Stop");
    insertButton = new JButton("Open Tape...");
    playButton.addActionListener(e -> play());
    pauseButton.addActionListener(e -> pause());
    stopButton.addActionListener(e -> stop());
    insertButton.addActionListener(e -> openTapeChooser.run());
    pauseButton.setToolTipText("Stops the tape where it is; playing again restarts the current block");

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    controls.add(playButton);
    controls.add(pauseButton);
    controls.add(stopButton);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(insertButton);
    status = new JLabel();
    controls.add(Box.createHorizontalStrut(15));
    controls.add(status);

    setLayout(new BorderLayout());
    add(controls, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
    refresh.start();
    addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
        refresh.stop();
      }
    });

    refresh();
  }

  /** Puts the cassette in this window into the machine in front and plays it. */
  private void play() {
    if (tapeFile == null) {
      return;
    }
    Tape active = activeTape.get();
    if (active == null) {
      // Nothing to play into: open a machine on this cassette and let it load itself from the
      // start, which is what clicking a game in the game browser does. The window is handed the
      // deck of that machine as it comes up, so it goes on showing the load.
      status.setText("Opening an emulator for " + tapeFile.getName() + "...");
      loadInNewEmulator.accept(tapeFile);
      return;
    }

    // A different machine, or the same one with something else in it: load the cassette there.
    if (active != deck || !tapeFile.equals(active.getTapeFilename())) {
      active.stop();
      active.eject();
      if (!active.insert(tapeFile)) {
        JOptionPane.showMessageDialog(this, "The deck could not read " + tapeFile.getName() + ".",
            "Play", JOptionPane.ERROR_MESSAGE);
        return;
      }
      active.addTapeBlockListener(block -> currentBlock = block);
      deck = active;
      paused = false;
    }

    int selected = table.getSelectedRow();
    if (!paused && selected >= 0) {
      deck.setSelectedBlock(selected); // ignored while playing, which is what we want
    }
    paused = false;
    deck.play(true);
  }

  private void pause() {
    if (deck == null) {
      return;
    }
    paused = deck.isTapePlaying();
    deck.stop();
  }

  private void stop() {
    if (deck == null) {
      return;
    }
    paused = false;
    deck.stop();
    deck.setSelectedBlock(0);
    currentBlock = -1;
  }

  /** Loads a cassette into this window. No emulator is needed to look at what is on it. */
  public void openTape(File file) {
    tapeFile = file;
    blocks = TapeBlock.read(file);
    deck = null;
    currentBlock = -1;
    paused = false;
    setTitle("Cassette - " + file.getName());
    model.fireTableDataChanged();
    refresh();
  }

  /** Adopts a cassette already loaded and running in a machine, as the game browser does. */
  public void adopt(File file, Tape playingDeck) {
    openTape(file);
    deck = playingDeck;
    playingDeck.addTapeBlockListener(block -> currentBlock = block);
  }

  private void refresh() {
    boolean hasTape = !blocks.isEmpty();
    boolean playing = hasTape && deck != null && deck.isTapePlaying();
    playButton.setEnabled(hasTape && !playing);
    pauseButton.setEnabled(playing);
    stopButton.setEnabled(hasTape && (playing || paused));

    if (!hasTape) {
      status.setText("No cassette - use Open Tape");
      return;
    }

    String where = currentBlock >= 0 && currentBlock < blocks.size()
        ? "block " + (currentBlock + 1) + " of " + blocks.size() + ", " + blocks.get(currentBlock).type()
        : blocks.size() + " blocks";
    String state = playing ? "Playing" : paused ? "Paused" : deck == null
        ? (activeTape.get() == null ? "Not loaded - no emulator open" : "Not loaded") : "Stopped";
    status.setText(state + " - " + where);

    model.fireProgressChanged();
    if (playing && currentBlock >= 0 && currentBlock < table.getRowCount()) {
      table.scrollRectToVisible(table.getCellRect(currentBlock, 0, true));
    }
  }

  /**
   * How far the player is into a block, 0 to 100. Blocks already behind it read full and ones
   * ahead read empty, so the column doubles as a position along the whole tape.
   */
  private int progressOf(int row) {
    if (deck == null || currentBlock < 0 || row > currentBlock) {
      return 0;
    }
    if (row < currentBlock) {
      return 100;
    }

    TapeBlock block = blocks.get(row);
    int length = block.length();
    if (length <= 0) {
      return 100;
    }
    int played = deck.getTapePosition() - block.start();
    return Math.max(0, Math.min(100, played * 100 / length));
  }

  private class BlockTableModel extends AbstractTableModel {
    private final String[] columns = {"#", "Type", "Details", "Bytes", "Progress"};

    public int getRowCount() {
      return blocks.size();
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    public Object getValueAt(int row, int column) {
      TapeBlock block = blocks.get(row);
      return switch (column) {
        case 0 -> row + 1;
        case 1 -> block.type();
        case 2 -> block.details();
        case 3 -> block.length();
        case 4 -> progressOf(row);
        default -> "";
      };
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return false; // a listing, not a form
    }

    void fireProgressChanged() {
      fireTableRowsUpdated(0, Math.max(0, blocks.size() - 1));
    }
  }

  /** Draws the progress column as a bar rather than a number. */
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

  /** Marks the block being read, so it can be picked out without reading the progress column. */
  private class CurrentBlockRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                   boolean focused, int row, int column) {
      Component component =
          super.getTableCellRendererComponent(table, value, selected, focused, row, column);
      Font font = component.getFont();
      component.setFont(row == currentBlock ? font.deriveFont(Font.BOLD) : font.deriveFont(Font.PLAIN));
      return component;
    }
  }
}
