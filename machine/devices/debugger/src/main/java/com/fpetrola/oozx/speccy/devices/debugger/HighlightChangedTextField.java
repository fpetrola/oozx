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

package com.fpetrola.oozx.speccy.devices.debugger;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;

/**
 * A field that goes red when what it holds is not what it held last time somebody looked.
 * <p>
 * Which register a step touched is the question a debugger is asked most, and eight fields of
 * four hex digits do not answer it: they all look the same and the one that moved is the one you
 * did not notice. Kept from the debugger this comes from, with the colour it settles back to
 * taken from the theme rather than hard-coded black, because the desk is dark.
 */
public class HighlightChangedTextField extends JTextField {

  private final Color unchanged = getForeground();
  private String previousValue = "";

  public HighlightChangedTextField(String value, int columns) {
    super(value, columns);
    getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        checkForChange();
      }

      public void removeUpdate(DocumentEvent e) {
        checkForChange();
      }

      public void changedUpdate(DocumentEvent e) {
        checkForChange();
      }
    });
  }

  private void checkForChange() {
    setForeground(getText().equals(previousValue) ? unchanged : Color.RED);
  }

  /** What it holds now is what it is expected to hold: stop calling it changed. */
  public void settle() {
    previousValue = getText();
    setForeground(unchanged);
  }
}
