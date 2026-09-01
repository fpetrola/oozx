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
      }
    }
  }
}
