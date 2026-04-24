/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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
