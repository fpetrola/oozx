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


package com.fpetrola.oozx.speccy.tools.calls;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Every place a call went, under whoever called it, while this is clipped onto the machine. */
public class CallsFrame extends MachineFrame {

  /** Slow enough that a game is not redrawing this instead of running. */
  private static final int REFRESH_MILLIS = 500;

  private final DefaultMutableTreeNode program = new DefaultMutableTreeNode("Program");
  private final DefaultTreeModel model = new DefaultTreeModel(program);
  private final JTree tree = new JTree(model);
  private CallTree watching;
  private long drawn = -1;

  /** A call target under whoever called it: how often, and where it went back to. */
  private record Routine(int address, int times, String back) {
    public String toString() {
      return "%04X ×%d ← %s".formatted(address, times, back);
    }
  }

  public CallsFrame() {
    super("Calls");
    setSize(420, 480);

    JButton forget = new JButton("Forget");
    forget.setToolTipText("Throw the run so far away and watch the next one");
    forget.addActionListener(e -> {
      if (watching != null) {
        watching.forget();
      }
    });
    controls.add(forget);

    JScrollPane pane = new JScrollPane(tree);
    pane.setPreferredSize(new Dimension(400, 400));
    assemble(pane);
    setCompact(false);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> draw());
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    if (watching != null) {
      watching.close();
      watching = null;
    }
    drawn = -1;
    if (now != null) {
      watching = new CallTree(now);
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show the calls, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  /**
   * Drawn again only when a call has been made since the last time: the tree is built rather
   * than mended, so whatever was open is opened again by the addresses down to it.
   */
  private void draw() {
    long counted = watching == null ? -1 : watching.counted();
    if (counted == drawn) {
      return;
    }
    drawn = counted;
    List<String> open = expanded();
    program.removeAllChildren();
    if (watching != null) {
      watching.program().forEach(call -> program.add(nodeOf(call)));
    }
    model.reload();
    tree.expandRow(0);
    for (int row = 0; row < tree.getRowCount(); row++) {
      if (open.contains(pathOf(tree.getPathForRow(row)))) {
        tree.expandRow(row);
      }
    }
  }

  private static DefaultMutableTreeNode nodeOf(CallTree.Call call) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(
        new Routine(call.address(), call.times(), wayBack(call.back())));
    call.made().forEach(made -> node.add(nodeOf(made)));
    return node;
  }

  /** Where the calls to a routine went back to, the way most of them took first. */
  private static String wayBack(Map<Integer, Integer> back) {
    List<String> ways = back.entrySet().stream()
        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
        .map(way -> "%04X".formatted(way.getKey())).toList();
    return ways.size() <= 2 ? String.join(", ", ways)
        : String.join(", ", ways.subList(0, 2)) + " +" + (ways.size() - 2);
  }

  private List<String> expanded() {
    List<String> open = new ArrayList<>();
    for (int row = 0; row < tree.getRowCount(); row++) {
      if (tree.isExpanded(row)) {
        open.add(pathOf(tree.getPathForRow(row)));
      }
    }
    return open;
  }

  /** A path said as the addresses down to it, which is what survives the tree being built again. */
  private static String pathOf(TreePath path) {
    StringBuilder said = new StringBuilder();
    for (Object step : path.getPath()) {
      if (((DefaultMutableTreeNode) step).getUserObject() instanceof Routine routine) {
        said.append("%04X/".formatted(routine.address()));
      }
    }
    return said.toString();
  }
}
