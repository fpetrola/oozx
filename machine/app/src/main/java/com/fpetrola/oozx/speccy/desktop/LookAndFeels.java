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

package com.fpetrola.oozx.speccy.desktop;


import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.plaf.basic.BasicRootPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Every look the program can wear, named once. The menu, the launcher and the restore on startup
 * all ask here, so a look is added by naming it in {@link #all()} and nowhere else.
 */
public final class LookAndFeels {
  public static final String DEFAULT = "Solarized Light";

  private interface Install {
    void apply() throws Exception;
  }

  private record Laf(String family, String name, Install install) {
    String id() {
      return family + " / " + name;
    }
  }

  /** The one that comes with the runtime: what is left to fall back on when a look cannot paint. */
  private static final Laf METAL = new Laf("System", "Metal",
      () -> UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel()));


  /** What is being worn, and the one before it: the way back when a look cannot paint. */
  private static Laf worn;
  private static Laf wornBefore;
  private static boolean goingBack;
  private static long wentBackAt;
  private static int triesLeft = 2;

  /** The question being asked, so the way back can take it down when the screen is gone. */
  private static JDialog asking;

  static {
    watchForALookThatCannotPaint();
  }

  /** Wears the look saved under this name, or the default one when it is unknown or gone. */
  public static void install(String name) {
    List<Laf> ALL = all();
    ALL.stream().filter(laf -> laf.id().equals(name)).findFirst()
        .or(() -> ALL.stream().filter(laf -> laf.name.equals(name)).findFirst())
        .or(() -> ALL.stream().filter(laf -> laf.name.equals(DEFAULT)).findFirst())
        .ifPresent(LookAndFeels::wear);
  }

  /** Un plugin de looks que se reinicia deja lo puesto pintando con clases de un cargador cerrado. */
  public static void wearAgainIfFromAPlugin() {
    if (worn != null && !"System".equals(worn.family)) install(worn.id());
  }

  /** The families as submenus, each item telling the caller which name to remember. */
  public static void fillMenu(JMenu menu, Consumer<String> chosen) {
    Map<String, JMenu> families = new LinkedHashMap<>();
    for (Laf laf : all()) {
      families.computeIfAbsent(laf.family, family -> {
        JMenu submenu = new JMenu(family);
        menu.add(submenu);
        return submenu;
      }).add(new AbstractAction(laf.name) {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          tryOn(laf, chosen);
        }
      });
    }
  }

  /** Wears a look and asks whether to keep it, going back on its own if nobody says yes. */
  private static void tryOn(Laf laf, Consumer<String> chosen) {
    MenuSelectionManager.defaultManager().clearSelectedPath();
    if (worn == null) {
      wear(laf);
      chosen.accept(laf.id());
      return;
    }
    Laf asked = worn;
    wear(laf);
    if (worn == laf && confirmed(laf)) chosen.accept(laf.id());
    else if (worn == laf) wear(asked);
  }

  /** A look that has painted for this long is taken to be one that works. */
  private static final long TRUSTED_AFTER = 30_000;

  private static final int SECONDS_TO_CONFIRM = 12;
  private static final int WIDE_ENOUGH = 420;

  /**
   * Asks whether to keep the look being tried on, with the question drawn in Metal.
   * <p>
   * A component takes its look from whoever is installed when it is made, and this question has
   * to be readable even when the answer to it is that the look being tried on draws nothing.
   * Metal comes with the runtime and is the one that cannot be missing or broken. Only the
   * Only the question is made with it, and the look being tried on goes back in place before the
   * question goes up, the way it asks to be put on rather than by its class, which for some of
   * them is what loses half of it. It has to be back by then: a look is entitled to be the one
   * installed while its windows paint, and some refuse to paint at all when it is not.
   */
  private static boolean confirmed(Laf laf) {
    Window owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    boolean[] keep = {false};
    try {
      UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
    } catch (Exception withoutMetal) {
      System.err.println("the question goes up in '" + laf.name + "': " + withoutMetal);
    }
    try {
      asking = question(owner, laf.name, keep);
    } finally {
      try {
        laf.install.apply();
      } catch (Exception | LinkageError itStays) {
        System.err.println("could not put '" + laf.name + "' back: " + itStays);
      }
    }
    asking.setVisible(true);
    asking = null;
    return keep[0];
  }

  private static JDialog question(Window owner, String name, boolean[] keep) {
    JDialog question = new JDialog(owner, "Keep this look?", Dialog.ModalityType.APPLICATION_MODAL);
    JLabel counting = new JLabel("", SwingConstants.CENTER);
    JButton yes = new JButton("Keep it");
    JButton no = new JButton("Go back");
    JPanel buttons = new JPanel();
    JPanel body = new JPanel(new BorderLayout(8, 8));

    yes.addActionListener(e -> {
      keep[0] = true;
      question.dispose();
    });
    no.addActionListener(e -> question.dispose());
    buttons.add(yes);
    buttons.add(no);
    body.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    body.add(new JLabel(name, SwingConstants.CENTER), BorderLayout.NORTH);
    body.add(counting, BorderLayout.CENTER);
    body.add(buttons, BorderLayout.SOUTH);
    question.setContentPane(body);
    question.getRootPane().setDefaultButton(yes);
    question.getRootPane().registerKeyboardAction(e -> question.dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    Timer countdown = new Timer(1000, null);
    int[] left = {SECONDS_TO_CONFIRM};
    countdown.addActionListener(e -> {
      counting.setText("going back in " + --left[0] + "s unless kept (Enter)");
      if (left[0] <= 0) question.dispose();
    });
    counting.setText("going back in " + SECONDS_TO_CONFIRM + "s unless kept (Enter)");
    countdown.start();
    question.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosed(java.awt.event.WindowEvent e) {
        countdown.stop();
      }
    });

    question.pack();
    question.setSize(Math.max(WIDE_ENOUGH, question.getWidth()), question.getHeight());
    question.setLocationRelativeTo(owner);
    yes.requestFocusInWindow();
    return question;
  }

  private static void wear(Laf laf) {
    if (!SwingUtilities.isEventDispatchThread()) {
      try {
        SwingUtilities.invokeAndWait(() -> wear(laf));
      } catch (Exception notThisOne) {
        System.err.println("could not wear the look '" + laf.name + "': " + notThisOne);
      }
      return;
    }
    // Putting the same look on again is a way of clearing what an earlier one left behind, and
    // there is nothing to undress for that.
    if (worn != laf) for (Window window : Window.getWindows()) undecorate(window);
    try {
      laf.install.apply();
      if (worn != laf) wornBefore = worn;
      worn = laf;
    } catch (Exception | LinkageError notThisOne) {
      System.err.println("could not wear the look '" + laf.name + "': " + notThisOne);
    }
    for (Window window : Window.getWindows()) {
      redress(window);
      window.invalidate();
      window.validate();
      window.repaint();
    }
  }

  /** Puts the look that is now on over a window and everything in it. */
  private static void redress(Component window) {
    eachPart("redressing", window, part -> {
      undoStaleBorder(part);
      part.updateUI();
    });
  }

  /**
   * Takes the panes a look decorates itself back to the toolkit's own, while that look is still
   * the one installed.
   * <p>
   * A look puts its title bars and borders on the window panes, and takes them off by asking
   * itself for the parts it made them with. Asked after it has been replaced it has nothing to
   * answer with and gives up half way, leaving a pane that goes on painting with a look that is
   * no longer there and fails at every repaint from then on. Undressing them first is what makes
   * leaving one safe.
   */
  private static void undecorate(Component window) {
    eachPart("undressing", window, part -> {
      if (part instanceof JRootPane pane) pane.setUI(new BasicRootPaneUI());
      if (part instanceof JInternalFrame machine) machine.setUI(new BasicInternalFrameUI(machine));
    });
  }

  /**
   * Every part of a window, each one on its own so that one failing leaves the others alone: a
   * window half redone is a desktop with nothing drawn on it and no menu to pick another look
   * from. Swing's own walk stops at the first failure, and misses the icons of minimised windows
   * and the menus that are not open.
   */
  private static void eachPart(String what, Component component, Consumer<JComponent> doIt) {
    if (component instanceof JComponent part) {
      try {
        doIt.accept(part);
      } catch (Exception | LinkageError notThisOne) {
        System.err.println(what + " " + part.getClass().getSimpleName() + " failed: " + notThisOne);
      }
    }
    if (component instanceof JInternalFrame machine) eachPart(what, machine.getDesktopIcon(), doIt);
    if (component instanceof JMenu menu) eachPart(what, menu.getPopupMenu(), doIt);
    if (component instanceof Container holder) {
      for (Component child : holder.getComponents()) eachPart(what, child, doIt);
    }
  }

  /**
   * A border the previous look put on a window pane outlives it: Swing replaces one only when the
   * new look names its own, and the old one goes on asking for parts that are gone.
   */
  private static void undoStaleBorder(JComponent part) {
    String key = part instanceof JRootPane ? "RootPane.border"
        : part instanceof JInternalFrame ? "InternalFrame.border" : null;
    if (key != null && part.getBorder() instanceof UIResource) {
      part.setBorder(UIManager.getBorder(key));
    }
  }

  /**
   * A look that throws while painting takes the event thread down with it, over and over: nothing
   * is drawn again and no menu answers, so nobody can pick another one, and the countdown that
   * was going to put the previous look back went down with it. Whoever is left standing has to
   * notice and go back, and it cannot be the event thread, which by then is gone.
   */
  /**
   * Whether this is the look failing rather than anything else that went wrong on the window's
   * thread.
   * <p>
   * It used to be anything at all: a game that could not be opened, a dialog with a null in it,
   * and two of those in a row took the look off and left Metal on, which says nothing to anybody
   * about what actually happened. What a look failing looks like is a frame of its own classes
   * or of painting, somewhere down the stack.
   */
  private static boolean aboutPainting(Throwable problem) {
    for (Throwable each = problem; each != null; each = each.getCause()) {
      for (StackTraceElement frame : each.getStackTrace()) {
        String where = frame.getClassName();
        if (where.startsWith("javax.swing.plaf") || where.startsWith("com.formdev.flatlaf")
            || where.startsWith("com.bulenkov") || where.contains("LookAndFeel")
            || frame.getMethodName().startsWith("paint")) {
          return true;
        }
      }
    }
    return false;
  }

  private static void watchForALookThatCannotPaint() {
    Thread.UncaughtExceptionHandler others = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, problem) -> {
      if (others != null) others.uncaughtException(thread, problem);
      else problem.printStackTrace();
      if (goingBack || !thread.getName().startsWith("AWT-EventQueue") || !aboutPainting(problem)) {
        return;
      }
      goingBack = true;
      // Leftovers of the look that was worn before are what usually fails, and putting the one
      // that is on over the windows again clears them. Only when that is not it does the look
      // come off, for the one that cannot fail; going back to the one worn before would be
      // going back to where the leftovers came from, and the two would take turns failing.
      if (System.currentTimeMillis() - wentBackAt > TRUSTED_AFTER) triesLeft = 2;
      wentBackAt = System.currentTimeMillis();
      Laf back = triesLeft-- == 2 ? worn : METAL;
      if (back == null || worn == METAL && triesLeft < 1) {
        goingBack = false;
        return;
      }
      System.err.println("'" + (worn == null ? "?" : worn.name) + "' cannot paint, going to '"
          + back.name + "'");
      new Thread(() -> {
        if (asking != null) asking.dispose();
        wear(back);
        goingBack = false;
      }).start();
    });
  }

  /**
   * Los looks de los plugins primero, despues los que vienen con Java. Preguntado cada vez: un
   * plugin de looks que llega o se va cambia la lista sin que nadie la vuelva a armar.
   */
  private static List<Laf> all() {
    List<Laf> all = new ArrayList<>();
    for (com.fpetrola.oozx.speccy.devices.Look family : WhatIsPluggedIn.theOne().looks()) {
      for (String name : family.names()) all.add(new Laf(family.family(), name, () -> family.wear(name)));
    }
    all.add(METAL);
    all.add(new Laf("System", "Nimbus", () -> UIManager.setLookAndFeel(
        "javax.swing.plaf.nimbus.NimbusLookAndFeel")));
    all.add(new Laf("System", "Native", () -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())));
    return all;
  }

}
