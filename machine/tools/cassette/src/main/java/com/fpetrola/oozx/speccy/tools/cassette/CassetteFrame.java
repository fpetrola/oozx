/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.tools.cassette;

import com.fpetrola.oozx.speccy.modules.tape.TapeBlock;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.Desk;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.devices.Opens;
import com.fpetrola.oozx.speccy.windows.Widgets;
import com.fpetrola.oozx.speccy.modules.tape.Tape;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;

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
public class CassetteFrame extends MachineFrame implements Opens {

  /** How often the progress column is refreshed. The tape moves on the emulation thread. */
  private static final int REFRESH_MILLIS = 100;

  /** The deck inside a machine's window, which is what being attached to it gives this one. */
  private final BlockTableModel model;
  private final JTable table;
  private final JButton playPauseButton;
  private final JButton stopButton;
  private final JButton insertButton;

  /** The cassette this window holds, which needs no emulator to be looked at. */
  private File tapeFile;
  private List<TapeBlock> blocks = List.of();

  /**
   * The deck this cassette is plugged into: the one belonging to the machine it is clipped to.
   * <p>
   * Attaching is the cable. A cassette against a machine plays into that machine and no other,
   * so switching machines is unplugging it from one and clipping it onto the next, which is
   * what it is with a real deck and a real lead.
   */
  private Tape deck;

  /** The block the player last reported starting, which is the one being read. */
  private volatile int currentBlock = -1;

  /** One listener, moved from deck to deck, so clipping this on twice does not count twice. */
  private final com.fpetrola.oozx.speccy.modules.tape.TapeBlockListener watching = block -> currentBlock = block;
  private boolean paused;

  public CassetteFrame() {
    super("Cassette");

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

    playPauseButton = Widgets.iconButton("25B6.svg", "Play", "Play the tape");
    stopButton = Widgets.iconButton("23F9.svg", "Stop", "Stop the tape and rewind it");
    insertButton = Widgets.iconButton("1F4FC.svg", "Open Tape...", "Open a tape");
    playPauseButton.addActionListener(e -> {
      if (deck != null && deck.isTapePlaying()) {
        pause();
      } else {
        play();
      }
      refresh();
    });
    stopButton.addActionListener(e -> stop());
    insertButton.addActionListener(e -> chooseATape());

    controls.add(playPauseButton);
    controls.add(stopButton);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(insertButton);

    assemble(new JScrollPane(table));

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

  /**
   * The machine this is clipped to changed, so the lead now goes somewhere else.
   * <p>
   * Unplugged, the tape stops turning: a deck carried away from the computer is not still
   * loading it. Nothing is played by plugging in - that is what the play button is for.
   */
  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    Tape plugged = now == null ? null : Tape.of(now);
    if (plugged == deck) {
      return;
    }
    if (deck != null) {
      deck.stop();
      deck.removeTapeBlockListener(watching);
    }
    deck = plugged;
    // Clipped onto a machine that is already loading - a tape given on the command line, a game
    // from the browser - this is what makes the table follow it without anybody pressing play.
    if (deck != null) {
      deck.addTapeBlockListener(watching);
    }
    currentBlock = -1;
    paused = false;
    refresh();
  }

  @Override
  protected String expandTip() {
    return "Show what is on the cassette, or just the controls";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs it in";
  }

  /** Puts the cassette in this window into the machine it is plugged into, and plays it. */
  private void play() {
    if (tapeFile == null) {
      return;
    }
    if (deck == null) {
      // Plugged into nothing: open a machine on this cassette and let it load itself from the
      // start, which is what clicking a game in the game browser does. This window is clipped
      // onto that machine as it comes up, so it goes on showing the load.
      setTitle(title("Opening an emulator..."));
      Desk.theOne().openMachineFor(tapeFile, this);
      return;
    }

    // Something else in that machine's deck: load this cassette there instead.
    if (!tapeFile.equals(deck.getTapeFilename())) {
      deck.stop();
      deck.eject();
      if (!deck.insert(tapeFile)) {
        JOptionPane.showMessageDialog(this, "The deck could not read " + tapeFile.getName() + ".",
            "Play", JOptionPane.ERROR_MESSAGE);
        return;
      }
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

  /** The deck this is plugged into, which is the machine's own. */
  Tape deck() {
    return deck;
  }

  /** Whether there is a cassette in this deck, or it is an empty one waiting for one. */
  public boolean hasTape() {
    return tapeFile != null;
  }

  @Override
  public boolean empty() {
    return tapeFile == null;
  }

  /** A cassette from whoever is in front, which is the only thing this window asks of the desk. */
  private void chooseATape() {
    File chosen = Desk.theOne().choose("Open Tape");
    if (chosen != null) {
      open(chosen);
    }
  }

  /** Loads a cassette into this window. No emulator is needed to look at what is on it. */
  @Override
  public void open(File file) {
    tapeFile = file;
    blocks = TapeBlock.read(file);
    currentBlock = -1;
    paused = false;
    model.fireTableDataChanged();
    refresh();
  }


  /**
   * The window's name doubles as its status line, the way the recording player's does.
   * <p>
   * There was a label for it in the row of buttons, which grew as the tape played - "block 7 of
   * 23, Turbo speed data" - until the row wrapped and took the expand and attach buttons onto a
   * second line the compact form cuts off. The title bar has room for the sentence and is
   * already there.
   */
  private String title(String state) {
    return "Cassette" + (tapeFile == null ? "" : " - " + tapeFile.getName())
        + (state == null || state.isEmpty() ? "" : " - " + state);
  }

  private void refresh() {
    boolean hasTape = !blocks.isEmpty();
    boolean playing = hasTape && deck != null && deck.isTapePlaying();
    playPauseButton.setIcon(Widgets.loadIcon(playing ? "23F8.svg" : "25B6.svg"));
    playPauseButton.setToolTipText(playing
        ? "Stops the tape where it is; playing again restarts the current block"
        : "Play the tape");
    playPauseButton.setEnabled(hasTape);
    stopButton.setEnabled(hasTape && (playing || paused));

    if (!hasTape) {
      setTitle(title("no cassette - use Open Tape"));
      return;
    }

    String where = currentBlock >= 0 && currentBlock < blocks.size()
        ? "block " + (currentBlock + 1) + " of " + blocks.size() + ", " + blocks.get(currentBlock).type()
        : blocks.size() + " blocks";
    String state = playing ? "Playing" : paused ? "Paused"
        : deck == null ? "not plugged in - clip this onto a machine" : "Stopped";
    setTitle(title(state + " - " + where));

    showProgress(blocks.isEmpty() || currentBlock < 0 ? 0
        : (currentBlock + progressOf(currentBlock) / 100.0) / blocks.size());
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
