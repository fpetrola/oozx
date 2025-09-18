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

package com.fpetrola.oozx.speccy.windows;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A window that clips onto a machine's window and behaves as part of it: a piece of equipment
 * against the side of the computer rather than a window of its own.
 * <p>
 * Attached, it follows the machine about, keeps directly in front of it in the stack, and goes
 * when the machine goes. Detached, it is an ordinary window. A drag that finishes near the
 * machine attaches; one that finishes away from it lets go. That is the whole of what the
 * cassette deck and the recording player have in common, and all of it lives here.
 * <p>
 * What a subclass supplies: its buttons, into {@link #controls}, and one component for the part
 * that is hidden in the compact form. Then it calls {@link #assemble}.
 */
public abstract class AttachedFrame extends JInternalFrame {

  /** How near a drag has to finish for it to attach, in pixels. */
  private static final int STICKY = 40;

  /** -Ddock.trace=true prints what the docking decides and why. */
  private static final boolean TRACE = Boolean.getBoolean("dock.trace");

  /**
   * Where this sits against its machine's window, if it sits against it at all.
   * <p>
   * Bottom by default because that is where it belongs: the controls for a picture go under the
   * picture, the same width, the way the transport bar of anything else does.
   */
  public enum Dock { FREE, BOTTOM, TOP, LEFT, RIGHT }

  /** The subclass's buttons go here before it calls {@link #assemble}. */
  protected final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

  private final Progress progress = new Progress();

  /** The part that the compact form hides: a listing, usually. */
  private JComponent detail;

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
  private Rectangle placedAt = new Rectangle();

  /**
   * Set while THIS code is moving the window, so that its own placement is not mistaken for
   * somebody dragging it. Without it, docking moves the window, the move looks like a drag, the
   * drag re-docks, and the two feed each other.
   */

  private JToggleButton dockButton;
  private JToggleButton expandButton;

  private static final java.util.concurrent.atomic.AtomicInteger BUILT = new java.util.concurrent.atomic.AtomicInteger();

  /** Fixes the order windows sit in along a shared edge, for as long as they are open. */
  private final int order = BUILT.incrementAndGet();

  /** True while a whole edge is being re-shared, so the windows on it do not each start it again. */
  private static boolean resettling;

  /** True from the first move of a drag until the mouse button is let go. */
  private boolean dragging;
  private final java.awt.event.AWTEventListener letGo = this::released;
  /** The machine a live drag is over and would attach to, lit on both windows until let go. */
  private JInternalFrame glowingMachine;
  private final SnapGlow glow = new SnapGlow();

  private JInternalFrame machineWindow;

  /** Watches the machine's window so this follows it about while attached. */
  private ComponentAdapter machineWatcher;

  /** Notices the machine closing, which while attached takes this window with it. */
  private InternalFrameAdapter machineEnd;

  protected AttachedFrame(String title) {
    super(title, true, true, true, true);
  }

  /**
   * Builds the window round what the subclass has made: its buttons, already in
   * {@link #controls}, and the part the compact form hides.
   * <p>
   * Called by the subclass rather than by this constructor, because the pieces belong to the
   * subclass and do not exist until its own constructor has run.
   */
  protected void assemble(JComponent detail) {
    this.detail = detail;

    expandButton = Widgets.iconToggle("expand-panel.svg", "Expand", expandTip());
    expandButton.addActionListener(e -> setCompact(!expandButton.isSelected()));

    dockButton = Widgets.iconToggle("dock-bottom.svg", "Attach", attachTip());
    dockButton.setSelected(true);
    dockButton.addActionListener(e -> {
      dock = dockButton.isSelected() ? Dock.BOTTOM : Dock.FREE;
      attachmentChanged();
      place();
    });
    // Held against the right edge rather than added to the row of buttons: the row wraps when
    // what a subclass put in it grows - a cassette that says which block it is reading, say -
    // and whatever wrapped went onto a second line that the compact height cuts off. These two
    // are how the window is folded and unfolded, so they are the last thing that may disappear.
    JPanel corner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    corner.add(expandButton);
    corner.add(dockButton);
    Widgets.tighten(controls);
    Widgets.tighten(corner);

    JPanel row = new JPanel(new BorderLayout());
    row.add(controls, BorderLayout.CENTER);
    row.add(corner, BorderLayout.EAST);

    JPanel top = new JPanel(new BorderLayout());
    top.add(row, BorderLayout.NORTH);
    // Under the buttons and across the whole width: how far along it is is the one thing worth
    // seeing at a glance, and the compact form has no room for anything that takes a row.
    top.add(progress, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(top, BorderLayout.NORTH);
    add(detail, BorderLayout.CENTER);
    detail.setVisible(false);                   // compact until asked otherwise

    // Sticky edges: a drag that finishes near the machine's window attaches to whichever side it
    // finished nearest, and one that finishes away from it lets go.
    // Decided when the button is LET GO, not at every position along the way and not after a
    // pause either. A drag is a stream of moves, and most of them are nowhere near anything:
    // judging each one in turn means a quick drag downwards detaches the moment it clears the
    // machine. And a hand that stops to think, still holding, has not decided; a timer that
    // attached after a moment of stillness took the choice out of it. The only question that
    // matters is where it was LEFT, and letting go is what answers it.
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(ComponentEvent moved) {
        if (!getBounds().equals(placedAt)) {
          if (!dragging) {
            dragging = true;
            Toolkit.getDefaultToolkit().addAWTEventListener(letGo, AWTEvent.MOUSE_EVENT_MASK);
          }
          previewSnap();
        }
      }

      @Override
      public void componentResized(ComponentEvent resized) {
        // Only a resize that somebody did counts. The ones this code makes are how it gets
        // placed, and treating those as a preference is how a window slowly grows on its own.
        if (!compact && getHeight() != placedAt.height) {
          chosenHeight = getHeight();
        }
      }
    });
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        stopDragging();
        clearGlow();
      }

    });

  }

  private boolean sized;

  /**
   * Takes the compact height as soon as there is a container to measure against.
   * <p>
   * Not in {@link #assemble}: a frame that has not been added to anything has no border yet, so
   * its preferred height comes out an inset short - 67 where it wanted 91 - and the toolbar is
   * clipped by exactly the border it did not know it had. Placing a window against a machine
   * gives it this height anyway, so before this the size came from being attached, and a deck
   * with no machine to clip onto kept whatever its constructor had asked for.
   */
  @Override
  public void addNotify() {
    super.addNotify();
    // Only while folded: a window that opens showing its listing - a waveform, say - asked for
    // the height it has, and taking the compact height there folds it shut before anyone sees it.
    if (!sized && compact) {
      sized = true;
      setSize(getWidth(), compactHeight());
      placedAt = getBounds();
    }
  }

  /** What the expand button says it shows. */
  protected abstract String expandTip();

  /** What the attach button says it does. */
  protected abstract String attachTip();

  /**
   * Attached or detached, just now. For whoever treats the attachment as a connection rather
   * than as a place to sit - a deck plays into the machine it is plugged into.
   */
  protected void attachmentChanged() {
  }

  /**
   * The machine this was attached to has closed.
   * <p>
   * Attached, this window goes with it: it was part of that machine, and controls for a picture
   * that is no longer there are a window nobody can use. Detached, it stays and lets go.
   */
  protected void machineClosed() {
    clearGlow();
    boolean wasAttached = dock != Dock.FREE;
    setMachineWindow(null);
    attachmentChanged();
    if (wasAttached) {
      dispose();
    }
  }

  /** How far along whatever this drives is, 0 to 1, as the line under the buttons. */
  protected void showProgress(double howFar) {
    progress.setHowFar(howFar);
  }

  /** The machine's window this is attached to, or null while there is none. */
  public JInternalFrame getMachineWindow() {
    return machineWindow;
  }

  public void setMachineWindow(JInternalFrame machineWindow) {
    if (this.machineWindow != null) {
      this.machineWindow.removeComponentListener(machineWatcher);
      this.machineWindow.removeInternalFrameListener(machineEnd);
    }
    this.machineWindow = machineWindow;
    if (machineWindow != null) {
      machineWatcher = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent moved) {
          if (TRACE) {
            System.out.printf("dock: the machine moved to %s, dock is %s%n",
                AttachedFrame.this.machineWindow.getBounds(), dock);
          }
          place();
        }

        @Override
        public void componentResized(ComponentEvent resized) {
          place();
        }
      };
      machineEnd = new InternalFrameAdapter() {
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
          machineClosed();
        }
      };
      machineWindow.addComponentListener(machineWatcher);
      machineWindow.addInternalFrameListener(machineEnd);
      place();
    }
    attachmentChanged();
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
    boolean showing = detail.isVisible();
    detail.setVisible(false);
    int tall = getPreferredSize().height;
    detail.setVisible(showing);
    return tall;
  }

  /** Puts this against its machine, if it is attached to it. */
  private void place() {
    if (machineWindow == null || machineWindow.isClosed() || dock == Dock.FREE) {
      return;
    }
    Rectangle m = machineWindow.getBounds();
    int tall = compact ? compactHeight() : Math.max(compactHeight(), chosenHeight);
    int[] mine = share(machineWindow, dock);
    int along = mine[1] - mine[0];
    switch (dock) {
      // Along the top or the bottom it takes the machine's width, which is the whole point of
      // putting it there: the controls for a picture, the width of the picture. Or the part of
      // that width another window has not already taken.
      // Overlapped by the two borders that meet, or the frames sit a seam apart: each draws
      // its own edge and the gap between the picture and the buttons is the sum of the two.
      case BOTTOM -> setBounds(mine[0], m.y + m.height - seam(), along, tall);
      case TOP -> setBounds(mine[0], Math.max(0, m.y - tall + seam()), along, tall);
      // Against a side it is moved, and only shortened when it does not fit what is left there.
      // Stretching a toolbar to the machine's height turns it into a column of empty space, and
      // a window that grows because it drifted near something else fights whoever is holding it.
      case LEFT -> setBounds(Math.max(0, m.x - getWidth() + sideSeam()), mine[0],
          getWidth(), Math.min(getHeight(), along));
      case RIGHT -> setBounds(m.x + m.width - sideSeam(), mine[0],
          getWidth(), Math.min(getHeight(), along));
      default -> { }
    }
    placedAt = getBounds();
    keepWithMachine();
    resettle(machineWindow, dock);
  }

  /**
   * Puts this immediately in front of its machine in the stack of windows.
   * <p>
   * While attached it is part of that window, not a window in its own right, so it has no
   * business having its own place in the order: anything raised over the machine - the game
   * browser, most often, which brings itself to the front whenever it is used - would otherwise
   * come up over the controls as well and leave them buried under something unrelated.
   * <p>
   * Index nought is the front in AWT, so taking the machine's index puts this one in front of it
   * and pushes the machine back by one. Detached, none of this applies and it takes its chances
   * like any other window.
   */
  private void keepWithMachine() {
    Container desktop = getParent();
    if (desktop == null || machineWindow == null || machineWindow.getParent() != desktop) {
      return;
    }
    int machineAt = desktop.getComponentZOrder(machineWindow);
    if (machineAt >= 0 && desktop.getComponentZOrder(this) != machineAt) {
      desktop.setComponentZOrder(this, machineAt);
    }
  }

  /** The two borders that meet where the frames touch, which is what to overlap by. */
  private int seam() {
    return seamWith(machineWindow);
  }

  private int seamWith(JInternalFrame machine) {
    return machine == null ? 0 : machine.getInsets().bottom + getInsets().top;
  }

  private int sideSeam() {
    return machineWindow == null ? 0 : machineWindow.getInsets().left + getInsets().right;
  }

  private void released(AWTEvent event) {
    if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED && dragging) {
      stopDragging();
      // After the frame's own drag handling has finished with the release, so the bounds are final.
      SwingUtilities.invokeLater(this::snapIfNear);
    }
  }

  private void stopDragging() {
    dragging = false;
    Toolkit.getDefaultToolkit().removeAWTEventListener(letGo);
  }

  /**
   * Attaches to whichever side of the machine's window this one was left nearest, or lets go.
   * <p>
   * Only the sides it actually overlaps count: a window dragged well below and to the right of
   * the machine is near its bottom edge by one measure and near nothing by eye.
   */
  public void snapIfNear() {
    clearGlow();
    JInternalFrame leftBehind = machineWindow;
    Dock leftSide = dock;
    Rectangle me = getBounds();
    JInternalFrame nearestMachine = snapCandidate();
    Dock nearest = nearestMachine == null ? Dock.FREE : nearestSideOf(nearestMachine, me);
    if (TRACE) {
      int closest = nearestMachine == null ? STICKY : distanceTo(nearestMachine, me, nearest);
      System.out.printf("dock: window=%s -> %s of %s (nearest %d px)%n",
          me, nearest, nearestMachine, closest);
    }
    boolean was = dock != Dock.FREE;
    dock = nearest;
    dockButton.setSelected(nearest != Dock.FREE);
    if (nearestMachine != null && nearestMachine != machineWindow) {
      // Carried from one computer to another: the lead comes out of the first and goes into
      // the second, which is the whole of how a deck is moved.
      setMachineWindow(nearestMachine);
      resettle(leftBehind, leftSide);
      return;
    }
    if (was != (nearest != Dock.FREE)) {
      attachmentChanged();
    }
    place();
    if (nearest != leftSide) {
      resettle(leftBehind, leftSide);
    }
  }

  /**
   * Every machine on this desktop, so a deck can be carried from one to another.
   * <p>
   * Measured against all of them rather than against the one it is already on: a deck that only
   * ever notices its own machine can be taken off that one and never put onto any other, which
   * leaves moving it between computers impossible by the one gesture that should do it.
   */
  /**
   * This window's stretch of a machine's side, as {start, end} along it: the side split evenly
   * between the windows on it, each keeping the same slot for as long as it is open.
   * <p>
   * Read from who is there rather than from where they are. Measuring the gap the others leave
   * made each window's place depend on the others' live bounds: two dropped on the same spot each
   * read the other as being first and stepped past it, then past it again, until they had walked
   * off the edge and one of them had no width left at all.
   */
  private int[] share(JInternalFrame machine, Dock side) {
    Rectangle m = machine.getBounds();
    boolean flat = side == Dock.TOP || side == Dock.BOTTOM;
    int start = flat ? m.x : m.y;
    int length = flat ? m.width : m.height;
    List<AttachedFrame> sharing = sharing(machine, side);
    int mine = sharing.indexOf(this);
    int each = length / sharing.size();
    return new int[] {start + mine * each,
        mine == sharing.size() - 1 ? start + length : start + (mine + 1) * each};
  }

  /**
   * Everyone else on that side takes their share again, now that this window has joined it or
   * left it. Without this the window already there kept the whole edge and the new one sat on
   * top of half of it.
   */
  private void resettle(JInternalFrame machine, Dock side) {
    if (machine == null || side == Dock.FREE || resettling) {
      return;
    }
    resettling = true;
    try {
      for (AttachedFrame other : sharing(machine, side)) {
        if (other != this) {
          other.place();
        }
      }
    } finally {
      resettling = false;
    }
  }

  /** Every window on that side of that machine, this one included, in their settled order. */
  private List<AttachedFrame> sharing(JInternalFrame machine, Dock side) {
    List<AttachedFrame> sharing = new ArrayList<>();
    Container desktop = machine.getParent();
    if (desktop != null) {
      for (Component component : desktop.getComponents()) {
        if (component instanceof AttachedFrame other && !other.isClosed()
            && other.machineWindow == machine && other.dock == side) {
          sharing.add(other);
        }
      }
    }
    if (!sharing.contains(this)) {
      sharing.add(this);
    }
    sharing.sort(java.util.Comparator.comparingInt(frame -> frame.order));
    return sharing;
  }

  /** The machine this would attach to if let go now, or null if it is not near one. */
  private JInternalFrame snapCandidate() {
    Rectangle me = getBounds();
    JInternalFrame nearest = null;
    int closest = STICKY;
    for (JInternalFrame candidate : machinesAround()) {
      Dock side = nearestSideOf(candidate, me);
      if (side != Dock.FREE) {
        int away = distanceTo(candidate, me, side);
        if (away < closest) {
          closest = away;
          nearest = candidate;
        }
      }
    }
    return nearest;
  }

  /**
   * While a drag is over a machine it would attach to, a soft line runs along the two edges about
   * to meet - the machine's and this window's - the whole length of each, title bar included, so
   * it is plain before letting go that dropping it there joins the two. It is drawn from the
   * desktop, over both windows, and cleared the moment the drag ends.
   */
  public void previewSnap() {
    glowingMachine = getBounds().equals(placedAt) ? null : snapCandidate();
    if (!(getParent() instanceof JLayeredPane desktop)) {
      return;
    }
    if (glowingMachine == null) {
      clearGlow();
      return;
    }
    if (glow.getParent() != desktop) {
      desktop.add(glow, JLayeredPane.DRAG_LAYER);
    }
    glow.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
    glow.repaint();
  }

  /** The machine a live drag would attach to, lit on both windows, or null. */
  public JInternalFrame previewTarget() {
    return glowingMachine;
  }

  private void clearGlow() {
    glowingMachine = null;
    Container desktop = glow.getParent();
    if (desktop != null) {
      desktop.remove(glow);
      desktop.repaint();
    }
  }

  /** The two meeting edges, lit; transparent to the mouse so the drag and the machine go on. */
  private final class SnapGlow extends JComponent {
    @Override
    public boolean contains(int x, int y) {
      return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
      JInternalFrame machine = glowingMachine;
      if (machine == null) {
        return;
      }
      Rectangle me = AttachedFrame.this.getBounds();
      Dock side = nearestSideOf(machine, me);
      int[] offered = share(machine, side);
      Graphics2D pen = (Graphics2D) g.create();
      // The machine's edge is lit only along the share on offer, which is where this window will
      // end up; this window's own facing edge is lit end to end.
      edge(pen, machine.getBounds(), side, offered[0], offered[1]);
      boolean flat = side == Dock.TOP || side == Dock.BOTTOM;
      edge(pen, me, facing(side), flat ? me.x : me.y, flat ? me.x + me.width : me.y + me.height);
      pen.dispose();
    }

    private Dock facing(Dock side) {
      return switch (side) {
        case BOTTOM -> Dock.TOP;
        case TOP -> Dock.BOTTOM;
        case LEFT -> Dock.RIGHT;
        case RIGHT -> Dock.LEFT;
        default -> Dock.FREE;
      };
    }

    /**
     * A line on one side of a frame, fading inward from the edge so it hugs it. Drawn as rows of
     * pixels rather than a stroked line, which put half its width outside the frame and, with
     * rounded ends, ran past the corners as well.
     */
    private void edge(Graphics2D pen, Rectangle r, Dock side, int from, int to) {
      if (side == Dock.FREE || to <= from) {
        return;
      }
      int[] alpha = {235, 130, 70, 30};
      for (int inward = 0; inward < alpha.length; inward++) {
        pen.setColor(new Color(110, 190, 255, alpha[inward]));
        switch (side) {
          case BOTTOM -> pen.fillRect(from, r.y + r.height - 1 - inward, to - from, 1);
          case TOP -> pen.fillRect(from, r.y + inward, to - from, 1);
          case RIGHT -> pen.fillRect(r.x + r.width - 1 - inward, from, 1, to - from);
          case LEFT -> pen.fillRect(r.x + inward, from, 1, to - from);
          default -> { }
        }
      }
    }
  }

  private List<JInternalFrame> machinesAround() {
    List<JInternalFrame> machines = new ArrayList<>();
    Container desktop = getParent();
    if (desktop != null) {
      for (Component component : desktop.getComponents()) {
        if (component instanceof MachineWindow && component instanceof JInternalFrame machine && !machine.isClosed()) {
          machines.add(machine);
        }
      }
    }
    if (machines.isEmpty() && machineWindow != null && !machineWindow.isClosed()) {
      machines.add(machineWindow);
    }
    return machines;
  }

  /** Which side of that machine this window was left against, or FREE for none of them. */
  private Dock nearestSideOf(JInternalFrame machine, Rectangle me) {
    Rectangle m = machine.getBounds();
    Dock nearest = Dock.FREE;
    int closest = STICKY;
    // Measured against where it would SIT if attached, not against the machine's outline. The
    // two are a seam apart, and measuring to the outline meant that the moment docking placed
    // the window - a seam inside the edge - the next look found it exactly a seam away and let
    // go again. With the seam at 24 and the reach at 24, "closer than" was never true and the
    // window attached and detached in the same breath: it moved into place and never held.
    for (Dock side : new Dock[] {Dock.BOTTOM, Dock.TOP, Dock.RIGHT, Dock.LEFT}) {
      if (overlapsFor(side, m, me)) {
        int away = distanceTo(machine, me, side);
        if (away < closest) {
          closest = away;
          nearest = side;
        }
      }
    }
    return nearest;
  }

  /** Only the sides it actually lies alongside count; the others are near by arithmetic alone. */
  private static boolean overlapsFor(Dock side, Rectangle m, Rectangle me) {
    return side == Dock.BOTTOM || side == Dock.TOP
        ? me.x < m.x + m.width && m.x < me.x + me.width
        : me.y < m.y + m.height && m.y < me.y + me.height;
  }

  /**
   * How far this window is from where it would SIT against that side.
   * <p>
   * Measured against its resting place rather than against the machine's outline. The two are a
   * seam apart, and measuring to the outline meant that the moment docking placed the window - a
   * seam inside the edge - the next look found it exactly a seam away and let go again: it moved
   * into place and never held.
   */
  private int distanceTo(JInternalFrame machine, Rectangle me, Dock side) {
    Rectangle m = machine.getBounds();
    int seam = machine.getInsets().bottom + getInsets().top;
    int sideSeam = machine.getInsets().left + getInsets().right;
    return switch (side) {
      case BOTTOM -> Math.abs(me.y - (m.y + m.height - seam));
      case TOP -> Math.abs(me.y - (m.y - me.height + seam));
      case RIGHT -> Math.abs(me.x - (m.x + m.width - sideSeam));
      case LEFT -> Math.abs(me.x - (m.x - me.width + sideSeam));
      default -> Integer.MAX_VALUE;
    };
  }


  /**
   * Clips this back onto a machine, which is what a deck asking for a computer wants.
   * <p>
   * Setting the window alone is not enough once somebody has unplugged this: the window would
   * be remembered and the lead would still be out, so it would sit beside a machine it cannot
   * play into. Asking for a machine is asking to be plugged into it.
   */
  public void attachTo(JInternalFrame machine) {
    dock = Dock.BOTTOM;
    dockButton.setSelected(true);
    setMachineWindow(machine);
  }

  /**
   * Clips onto a machine that was opened FOR this window, by bringing the machine here.
   * <p>
   * A deck somebody carried across the desk and then pressed play on has been put where they
   * want it. Plugging it in by moving it to wherever the new machine happened to land takes
   * that away, so the machine is placed above this window instead: it is the one that has just
   * arrived and has no place of its own yet.
   */
  public void takeMachine(JInternalFrame machine) {
    // The machine keeps the size it was made at; the controls are the ones that fit under it.
    machine.setLocation(getX(), Math.max(0, getY() - machine.getHeight() + seamWith(machine)));
    setSize(machine.getWidth(), getHeight());
    attachTo(machine);
  }

  /** Which side of the machine's window this is attached to, or FREE. */
  public Dock dockedTo() {
    return dock;
  }

  /** Whether this is clipped onto a machine, which is what plugs it in. */
  public boolean isAttached() {
    return dock != Dock.FREE && machineWindow != null && !machineWindow.isClosed();
  }

  /** The compact form is the controls alone; expanded adds the listing under them. */
  public void setCompact(boolean wanted) {
    if (!wanted && chosenHeight < compactHeight() + 40) {
      chosenHeight = 300;                       // never expanded before: a sensible first size
    }
    compact = wanted;
    detail.setVisible(!wanted);
    expandButton.setSelected(!wanted);
    // Who decides the height here. Along the top or the bottom the machine does, through place().
    // Loose, or against a side - where attaching is a move and never a resize - this window's own
    // height is its own business, and folding is exactly that: it used to hide the contents and
    // leave the frame standing at whatever size it had. Asked as "is it clipped onto something",
    // not as "which side would it clip onto": a window that has never been attached still says
    // BOTTOM, and taking that as attached sent it to place(), which returns at once with no
    // machine, so nothing resized at all.
    boolean machineSetsTheHeight = isAttached() && (dock == Dock.TOP || dock == Dock.BOTTOM);
    if (!machineSetsTheHeight) {
      setSize(getWidth(), wanted ? compactHeight() : Math.max(compactHeight(), chosenHeight));
      placedAt = getBounds();
    }
    if (isAttached()) {
      place();
    }
    revalidate();
    repaint();
  }

  /**
   * How far along, as a line under the buttons.
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
}
