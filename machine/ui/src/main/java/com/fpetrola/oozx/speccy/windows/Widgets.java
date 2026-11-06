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

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.File;
import java.util.function.Consumer;

/** @deprecated plugdesk's, {@link dev.plugdesk.desk.Widgets}. Stays while a published plugin calls it from here. */
@Deprecated
public class Widgets {
  public static final int TOOLBAR_ICON_SIZE = dev.plugdesk.desk.Widgets.TOOLBAR_ICON_SIZE;

  public static int zOrderOf(java.awt.Component window) {
    return dev.plugdesk.desk.Widgets.zOrderOf(window);
  }

  public static void openFile(Component parent, String title, FileFilter filter, Consumer<File> chosen) {
    dev.plugdesk.desk.Widgets.openFile(parent, title, filter, chosen);
  }

  public static void saveFile(Component parent, String title, FileFilter filter, String suggestedName, Consumer<File> chosen) {
    dev.plugdesk.desk.Widgets.saveFile(parent, title, filter, suggestedName, chosen);
  }

  public static ImageIcon loadIcon(String iconFile) {
    return dev.plugdesk.desk.Widgets.loadIcon(iconFile);
  }

  public static JButton iconButton(String iconFile, String text, String tooltip) {
    return dev.plugdesk.desk.Widgets.iconButton(iconFile, text, tooltip);
  }

  public static <T> void whenChosen(javax.swing.JComboBox<T> box, java.util.function.Supplier<T> nowIs, java.util.function.Consumer<T> choose) {
    dev.plugdesk.desk.Widgets.whenChosen(box, nowIs, choose);
  }

  public static String reason(Throwable failure) {
    return dev.plugdesk.desk.Widgets.reason(failure);
  }

  public static JToggleButton iconToggle(String iconFile, String text, String tooltip) {
    return dev.plugdesk.desk.Widgets.iconToggle(iconFile, text, tooltip);
  }

  public static void jumpToClick(JSlider slider) {
    dev.plugdesk.desk.Widgets.jumpToClick(slider);
  }

  public static JSlider upright(JSlider slider, int width, int height) {
    return dev.plugdesk.desk.Widgets.upright(slider, width, height);
  }

  public static void popUpOnRightClick(AbstractButton button, JComponent content) {
    dev.plugdesk.desk.Widgets.popUpOnRightClick(button, content);
  }

  public static ImageIcon greyed(ImageIcon icon, float amount) {
    return dev.plugdesk.desk.Widgets.greyed(icon, amount);
  }

  public static void tighten(java.awt.Container toolBar) {
    dev.plugdesk.desk.Widgets.tighten(toolBar);
  }

  public static void tighten(java.awt.Container toolBar, int sideMargin) {
    dev.plugdesk.desk.Widgets.tighten(toolBar, sideMargin);
  }

  public static void scrollingWhenLong(javax.swing.JPopupMenu menu) {
    dev.plugdesk.desk.Widgets.scrollingWhenLong(menu);
  }

  public static void scrollingWhenLong(javax.swing.JPopupMenu menu, int fixedOnTop) {
    dev.plugdesk.desk.Widgets.scrollingWhenLong(menu, fixedOnTop);
  }
}
