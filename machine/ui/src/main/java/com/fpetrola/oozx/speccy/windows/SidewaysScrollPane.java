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

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * A row of buttons in one line that scrolls when the window is narrower than they are: wrapped,
 * the second line fell under the compact height and could not be reached. Its height counts the
 * scroll bar while one is showing, so the compact form does not cut the buttons with it.
 */
class SidewaysScrollPane extends JScrollPane {
  private final JComponent row;

  SidewaysScrollPane(JComponent row) {
    super(row, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    this.row = row;
    setBorder(null);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension wanted = row.getPreferredSize();
    boolean narrower = getWidth() > 0 && wanted.width > getViewport().getWidth();
    return new Dimension(wanted.width, wanted.height + (narrower ? getHorizontalScrollBar().getPreferredSize().height : 0));
  }
}
