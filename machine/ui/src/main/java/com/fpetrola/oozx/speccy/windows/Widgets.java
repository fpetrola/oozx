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

package com.fpetrola.oozx.speccy.windows;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JPopupMenu;
import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.JSlider;
import java.awt.event.MouseMotionAdapter;
import java.awt.Component;
import java.awt.Image;
import java.net.URL;
import java.awt.Container;
import java.awt.Insets;

/**
 * The small change of a toolbar: an icon off the classpath, a button with one on it, and the
 * margins that make a row of them look like a row rather than like a form.
 * <p>
 * They were statics on the emulator's own window, which is why anything wanting a button had to
 * reach into the application for one - the attachable frame included, and it is underneath.
 */
public class Widgets {
  public static final int TOOLBAR_ICON_SIZE = 20;

  /** Where this window sits in the pile on its desktop, which is what a saved layout keeps. */
  public static int zOrderOf(java.awt.Component window) {
    Container parent = window.getParent();
    if (parent == null) {
      return 0;
    }
    java.awt.Component[] there = parent.getComponents();
    for (int place = 0; place < there.length; place++) {
      if (there[place] == window) {
        return place;
      }
    }
    return 0;
  }

  public static ImageIcon loadIcon(String iconFile) {
    if (iconFile.endsWith(".png")) {
      URL file = Widgets.class.getResource("/icons/" + iconFile);
      if (file == null) {
        throw new IllegalArgumentException("no icon " + iconFile);
      }
      return new ImageIcon(new ImageIcon(file).getImage()
          .getScaledInstance(TOOLBAR_ICON_SIZE, TOOLBAR_ICON_SIZE, Image.SCALE_SMOOTH));
    }
    return SvgIconLoader.loadSvgAsImageIcon("/icons/" + iconFile, TOOLBAR_ICON_SIZE, TOOLBAR_ICON_SIZE);
  }

  /** A button with an icon, falling back to words when the icon is not there to be found. */
  public static JButton iconButton(String iconFile, String text, String tooltip) {
    JButton button = new JButton();
    button.setFocusable(false);
    try {
      button.setIcon(loadIcon(iconFile));
    } catch (RuntimeException missing) {
      button.setText(text);
    }
    if (tooltip != null) button.setToolTipText(tooltip);
    return button;
  }

  /** What to tell a person about a failure: the deepest message there is, or the kind of failure when there is none. */
  /**
   * Acts on what a person picked from a box, and not on the box being told what it already says.
   * <p>
   * A box that shows what something is and also changes it hears its own echo: the thing changes,
   * says so, every box showing it is set to the new value, and each of those looks exactly like
   * somebody picking it again. With two boxes on the same thing the echo goes round between them,
   * which is why changing the machine from one of them flickered through the old model on its way
   * to the new one. Asking what it is now before asking for it again is what breaks the round.
   */
  public static <T> void whenChosen(javax.swing.JComboBox<T> box, java.util.function.Supplier<T> nowIs,
      java.util.function.Consumer<T> choose) {
    box.addActionListener(e -> {
      T chosen = box.getItemAt(box.getSelectedIndex());
      if (chosen != null && !chosen.equals(nowIs.get())) {
        choose.accept(chosen);
      }
    });
  }

  public static String reason(Throwable failure) {
    Throwable deepest = failure;
    while (deepest.getCause() != null && deepest.getCause() != deepest) {
      deepest = deepest.getCause();
    }
    String message = deepest.getMessage();
    return message == null || message.isBlank() ? deepest.getClass().getSimpleName() : message;
  }

  /** The same, for a button that stays down. */
  public static JToggleButton iconToggle(String iconFile, String text, String tooltip) {
    JToggleButton button = new JToggleButton();
    button.setFocusable(false);
    try {
      button.setIcon(loadIcon(iconFile));
    } catch (RuntimeException missing) {
      button.setText(text);
    }
    if (tooltip != null) button.setToolTipText(tooltip);
    return button;
  }

  public static void tighten(Container toolBar) {
    tighten(toolBar, 3);
  }

  public static void tighten(Container toolBar, int sideMargin) {
    for (Component component : toolBar.getComponents()) {
      if (component instanceof AbstractButton button) {
        button.setMargin(new Insets(2, sideMargin, 2, sideMargin));
        button.setFocusPainted(false);
        // And it does not take the keyboard: a button that holds the focus is a button that Enter
        // presses, which is how Enter stopped reaching the machine and pressed the turbo instead.
        button.setFocusable(false);
      }
    }
  }

  /**
   * Shows the given component under the button on a right click, the way a menu would; the left
   * click keeps doing what the button does. For the setting behind a button - a slider for the
   * speed under the turbo button, one for the volume under the mute button.
   */
  /**
   * Opens over the toolbar and title bar, ending at the button's bottom edge: below the toolbar
   * is the picture. Kept inside the window on purpose - a popup that has to stand outside it
   * takes the focus, and the frame losing it closes the popup at once.
   */
  /**
   * A click anywhere on the track jumps the thumb to that point and drags from there, rather than
   * the L&F's block-step. The UI's own track listener is taken off first, since it is the one that
   * steps by a block and swallows the click.
   */
  public static void jumpToClick(JSlider slider) {
    for (java.awt.event.MouseListener listener : slider.getMouseListeners()) slider.removeMouseListener(listener);
    for (java.awt.event.MouseMotionListener listener : slider.getMouseMotionListeners()) slider.removeMouseMotionListener(listener);
    MouseAdapter toThePoint = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        setFromX(e);
      }
    };
    slider.addMouseListener(toThePoint);
    slider.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        setFromX(e);
      }
    });
  }

  static void setFromX(MouseEvent e) {
    JSlider slider = (JSlider) e.getSource();
    if (!slider.isEnabled()) return;
    Insets in = slider.getInsets();
    boolean vertical = slider.getOrientation() == JSlider.VERTICAL;
    int span = vertical ? slider.getHeight() - in.top - in.bottom : slider.getWidth() - in.left - in.right;
    if (span <= 0) return;
    double along = vertical ? span - (e.getY() - in.top) : e.getX() - in.left;
    double ratio = Math.max(0, Math.min(1, along / span));
    int min = slider.getMinimum();
    slider.setValue((int) Math.round(min + ratio * (slider.getMaximum() - min)));
  }

  /**
   * A thin, upright slider that can stand over the picture without hiding it: what a right click
   * on a toolbar button opens for the setting behind the button.
   */
  public static JSlider upright(JSlider slider, int width, int height) {
    slider.setOrientation(JSlider.VERTICAL);
    slider.setOpaque(false);
    slider.setPreferredSize(new java.awt.Dimension(width, height));
    jumpToClick(slider);
    return slider;
  }

  /**
   * Opens under the button, over the picture, in a see-through panel: beside the buttons there is
   * nothing else to cover, and a thin upright control hides next to none of the picture. Kept
   * inside the window on purpose - a popup that has to stand outside it takes the focus, and the
   * frame losing it closes the popup at once.
   */
  public static void popUpOnRightClick(AbstractButton button, JComponent content) {
    JPopupMenu popup = new JPopupMenu();
    popup.setLightWeightPopupEnabled(true);
    popup.setOpaque(false);
    popup.setBackground(new java.awt.Color(0, 0, 0, 0));
    popup.setBorder(BorderFactory.createEmptyBorder());
    JPanel glass = new JPanel(new java.awt.BorderLayout()) {
      @Override
      protected void paintComponent(java.awt.Graphics g) {
        g.setColor(new java.awt.Color(40, 40, 40, 90));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
      }
    };
    glass.setOpaque(false);
    glass.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
    glass.add(content);
    popup.add(glass);
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        mouseReleased(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        popup.show(button, 0, button.getHeight());
        // The panel Swing puts a light popup in is opaque whatever the popup says, and so is
        // whatever the look and feel paints under a slider: every layer between the picture and
        // the thumb is told to stay out of the way.
        for (Container layer = popup.getParent(); layer != null && !(layer instanceof javax.swing.JLayeredPane); layer = layer.getParent())
          if (layer instanceof JComponent c) c.setOpaque(false);
        for (Component inside : content.getComponents())
          if (inside instanceof JComponent c) c.setOpaque(false);
        popup.repaint();
      }
    });
  }

  /** The icon with that much of its colour gone: nought is the icon, one is it greyed out. */
  public static ImageIcon greyed(ImageIcon icon, float amount) {
    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(icon.getIconWidth(), icon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D g = image.createGraphics();
    g.drawImage(icon.getImage(), 0, 0, null);
    if (amount > 0) {
      g.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.min(1f, amount)));
      g.drawImage(javax.swing.GrayFilter.createDisabledImage(icon.getImage()), 0, 0, null);
    }
    g.dispose();
    return new ImageIcon(image);
  }

  /** Cuantos items muestra un menu largo a la vez. */
  private static final int ITEMS_AT_ONCE = 18;

  /**
   * Un menu con mas de lo que entra deja de llenar la pantalla: muestra unos cuantos items y dos
   * flechas que lo recorren al dejarles el mouse encima, y la rueda tambien. Los items siguen siendo
   * del menu: puestos en un panel con barra, Swing no los reconocia como suyos y cerraba el menu en
   * cuanto el mouse pasaba por uno. Llamar despues de llenarlo.
   */
  public static void scrollingWhenLong(javax.swing.JPopupMenu menu) {
    java.awt.Component[] all = menu.getComponents();
    if (all.length <= ITEMS_AT_ONCE) return;
    int[] first = {0};
    javax.swing.JMenuItem up = new javax.swing.JMenuItem("\u25B2");
    javax.swing.JMenuItem down = new javax.swing.JMenuItem("\u25BC");
    // Del ancho del mas ancho de todos, visible o no: si no, cambiaba al recorrerlo.
    java.awt.Insets border = menu.getInsets();
    int widest = java.util.Arrays.stream(all).mapToInt(item -> item.getPreferredSize().width).max().orElse(0)
        + border.left + border.right;
    Runnable show = () -> {
      menu.removeAll();
      menu.add(up);
      for (int i = first[0]; i < Math.min(all.length, first[0] + ITEMS_AT_ONCE); i++) menu.add(all[i]);
      menu.add(down);
      up.setEnabled(first[0] > 0);
      down.setEnabled(first[0] + ITEMS_AT_ONCE < all.length);
      menu.setPreferredSize(null);
      menu.setPreferredSize(new java.awt.Dimension(widest, menu.getPreferredSize().height));
      menu.pack();
      menu.revalidate();
      menu.repaint();
    };
    java.util.function.IntConsumer move = step -> {
      int moved = Math.max(0, Math.min(all.length - ITEMS_AT_ONCE, first[0] + step));
      if (moved != first[0]) {
        first[0] = moved;
        show.run();
      }
    };
    for (javax.swing.JMenuItem arrow : java.util.List.of(up, down)) {
      arrow.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
      int step = arrow == up ? -1 : 1;
      javax.swing.Timer going = new javax.swing.Timer(90, e -> move.accept(step));
      arrow.getModel().addChangeListener(e -> {
        if (arrow.isArmed() && arrow.isEnabled()) going.start();
        else going.stop();
      });
      // Una flecha no es una eleccion: tocarla no cierra el menu.
      arrow.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
        @Override
        protected void doClick(javax.swing.MenuSelectionManager manager) {
        }

        /** Centrada en si misma: el lugar que Swing le da depende de los items de al lado, que cambian. */
        @Override
        public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
          if (arrow.isArmed() && arrow.isEnabled()) {
            g.setColor(javax.swing.UIManager.getColor("MenuItem.selectionBackground"));
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
          }
          g.setColor(arrow.isEnabled() ? arrow.getForeground() : javax.swing.UIManager.getColor("MenuItem.disabledForeground"));
          g.setFont(arrow.getFont());
          java.awt.FontMetrics metrics = g.getFontMetrics();
          g.drawString(arrow.getText(), (c.getWidth() - metrics.stringWidth(arrow.getText())) / 2,
              (c.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        }
      });
    }
    menu.addMouseWheelListener(wheel -> move.accept(wheel.getWheelRotation()));
    show.run();
  }
}
