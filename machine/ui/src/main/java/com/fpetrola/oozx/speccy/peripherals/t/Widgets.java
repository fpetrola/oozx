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

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JPopupMenu;
import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import java.awt.Component;
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

  public static ImageIcon loadIcon(String iconFile) {
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
    for (Component component : toolBar.getComponents()) {
      if (component instanceof AbstractButton button) {
        button.setMargin(new Insets(2, 3, 2, 3));
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
  public static void popUpOnRightClick(AbstractButton button, JComponent content) {
    JPopupMenu popup = new JPopupMenu();
    popup.add(content);
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) popup.show(button, 0, button.getHeight());
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) popup.show(button, 0, button.getHeight());
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
}
