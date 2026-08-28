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
import java.util.function.Supplier;

/**
 * Shows what is on a tape and lets it be driven by hand: the blocks and their details, which one
 * is being read, how far into it the player has got, and play, pause and stop.
 * <p>
 * It is not bound to one machine. On every refresh it asks the app which emulator is in front and
 * drives that one's deck, so a tape can be opened into a running emulator, played block by block
 * while that machine sits at LOAD "", and the window keeps working when the emulator is closed and
 * another opened. With no emulator, or none with a tape in it, it says so and the controls go dead.
 * <p>
 * The deck is driven with {@code play(true)}, the manual mode, so a "stop the tape" block is
 * honoured here as it should be: the person watching is the one who decides when it starts again.
 * An automatic load runs through those instead, because there is nobody to press anything.
 */
public class TapeBrowserInternalFrame extends JInternalFrame {

  /** How often the progress column is refreshed. The tape moves on the emulation thread. */
  private static final int REFRESH_MILLIS = 100;

  private final Supplier<Tape> activeTape;
  private final Runnable insertTape;
  private final BlockTableModel model;
  private final JTable table;
  private final JLabel status;
  private final JButton playButton;
  private final JButton pauseButton;
  private final JButton stopButton;
  private final JButton insertButton;

  /** The deck being driven, and the tape in it, as of the last refresh. */
  private Tape tape;
  private File tapeFile;
  private List<TapeBlock> blocks = List.of();

  /** The block the player last reported starting, which is the one being read. */
  private volatile int currentBlock = -1;
  private boolean paused;

  /**
   * @param activeTape supplies the deck of whatever machine is in front, or null when there is none
   * @param insertTape asks for a tape to be put into that machine
   */
  public TapeBrowserInternalFrame(Supplier<Tape> activeTape, Runnable insertTape) {
    super("Cassette", true, true, true, true);
    this.activeTape = activeTape;
    this.insertTape = insertTape;

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
    insertButton = new JButton("Insert Tape...");
    playButton.addActionListener(e -> play());
    pauseButton.addActionListener(e -> pause());
    stopButton.addActionListener(e -> stop());
    insertButton.addActionListener(e -> insertTape.run());
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

  /** Plays from the selected block, or from wherever the tape already is if nothing is selected. */
  private void play() {
    if (tape == null) {
      return;
    }
    int selected = table.getSelectedRow();
    if (!paused && selected >= 0) {
      tape.setSelectedBlock(selected); // ignored while playing, which is what we want
    }
    paused = false;
    tape.play(true);
  }

  private void pause() {
    if (tape == null) {
      return;
    }
    paused = tape.isTapePlaying();
    tape.stop();
  }

  private void stop() {
    if (tape == null) {
      return;
    }
    paused = false;
    tape.stop();
    tape.setSelectedBlock(0);
    currentBlock = -1;
  }

  private void refresh() {
    followActiveEmulator();

    boolean hasTape = tape != null && !blocks.isEmpty();
    boolean playing = hasTape && tape.isTapePlaying();
    playButton.setEnabled(hasTape && !playing);
    pauseButton.setEnabled(playing);
    stopButton.setEnabled(hasTape && (playing || paused));

    if (!hasTape) {
      status.setText(activeTape.get() == null
          ? "No emulator in front - open one to drive its tape"
          : "No tape in the machine in front - use Insert Tape");
      model.fireProgressChanged();
      return;
    }

    String where = currentBlock >= 0 && currentBlock < blocks.size()
        ? "block " + (currentBlock + 1) + " of " + blocks.size() + ", " + blocks.get(currentBlock).type()
        : blocks.size() + " blocks";
    status.setText((playing ? "Playing - " : paused ? "Paused - " : "Stopped - ") + where);

    model.fireProgressChanged();
    if (playing && currentBlock >= 0 && currentBlock < table.getRowCount()) {
      table.scrollRectToVisible(table.getCellRect(currentBlock, 0, true));
    }
  }

  /**
   * Points at whatever emulator is in front. Rereads the block list when the deck or the tape in
   * it changes, which is what makes the window survive switching or reopening emulators.
   */
  private void followActiveEmulator() {
    Tape active = activeTape.get();
    File file = active != null && active.isTapeInserted() ? active.getTapeFilename() : null;

    boolean sameDeck = active == tape;
    boolean sameTape = file == null ? tapeFile == null : file.equals(tapeFile);
    if (sameDeck && sameTape) {
      return;
    }

    if (active != tape && active != null) {
      // The player reports the block it is starting; that is what "being read" means here.
      active.addTapeBlockListener(block -> currentBlock = block);
    }

    tape = active;
    tapeFile = file;
    blocks = file != null ? TapeBlock.read(file) : List.of();
    currentBlock = -1;
    paused = false;
    setTitle(file != null ? "Cassette - " + file.getName() : "Cassette");
    model.fireTableDataChanged();
  }

  /**
   * How far the player is into a block, 0 to 100. Blocks already behind it read full and ones
   * ahead read empty, so the column doubles as a position along the whole tape.
   */
  private int progressOf(int row) {
    if (currentBlock < 0 || row > currentBlock) {
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
    int played = tape.getTapePosition() - block.start();
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
