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

import com.fpetrola.oozx.speccy.screen.Knob;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A window made out of knobs that describe themselves.
 * <p>
 * Given a list of {@link Knob}, this lays out one row each - the name, a control of whatever
 * sort the knob says it is, and a button to put it back - grouped into sections by the name each
 * knob gives for where it belongs. Nothing here knows what any of them do.
 * <p>
 * It was written inside the screen settings window, for the ten knobs a screen has. The mouse
 * turned out to have knobs too, and they are the same sort of thing: a name, a range, a
 * sentence about what it does, and somewhere to read and write it. So it lives here now, and a
 * device that grows a setting writes one line rather than a window.
 */
public class KnobRows {

  private final Map<Knob, Runnable> refreshers = new LinkedHashMap<>();
  private final Runnable whenChanged;

  public KnobRows(Runnable whenChanged) {
    this.whenChanged = whenChanged == null ? () -> { } : whenChanged;
  }

  /** Puts every control back in step with what its knob actually says now. */
  public void refresh() {
    refreshers.values().forEach(Runnable::run);
  }

  public JPanel of(java.util.List<Knob> knobs) {
    JPanel all = new JPanel();
    all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));

    String showing = null;
    JPanel section = null;
    int row = 0;

    for (Knob knob : knobs) {
      if (!knob.group().equals(showing)) {
        showing = knob.group();
        section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(showing));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        all.add(section);
        row = 0;
      }
      addRow(section, knob, row++);
    }

    all.add(Box.createVerticalGlue());
    return all;
  }

  private void addRow(JPanel section, Knob knob, int row) {
    GridBagConstraints at = new GridBagConstraints();
    at.insets = new Insets(2, 6, 2, 6);
    at.gridy = row;
    at.anchor = GridBagConstraints.WEST;

    JLabel label = new JLabel(knob.label());
    label.setToolTipText(knob.about());
    at.gridx = 0;
    section.add(label, at);

    JComponent control = controlFor(knob);
    // The one line the engine gives about each knob. Without it a window of names like
    // "halation" and "persistence" says what can be changed and nothing about what it does.
    control.setToolTipText(knob.about());
    at.gridx = 1;
    at.weightx = 1;
    at.fill = GridBagConstraints.HORIZONTAL;
    section.add(control, at);

    JButton reset = new JButton("↺");
    reset.setToolTipText("Back to " + knob.fallback());
    reset.setMargin(new Insets(0, 4, 0, 4));
    reset.addActionListener(e -> {
      knob.reset();
      refreshers.get(knob).run();
    });
    at.gridx = 2;
    at.weightx = 0;
    at.fill = GridBagConstraints.NONE;
    section.add(reset, at);
  }

  private JComponent controlFor(Knob knob) {
    switch (knob.kind()) {
      case CHOICE -> {
        JComboBox<String> box = new JComboBox<>(knob.options().toArray(new String[0]));
        box.setSelectedItem(String.valueOf(knob.value()));
        box.addActionListener(e -> {
          knob.set(box.getSelectedItem());
          whenChanged.run();
        });
        refreshers.put(knob, () -> box.setSelectedItem(String.valueOf(knob.value())));
        return box;
      }
      case SWITCH -> {
        JCheckBox check = new JCheckBox("", Boolean.parseBoolean(String.valueOf(knob.value())));
        check.addActionListener(e -> {
          knob.set(check.isSelected());
          whenChanged.run();
        });
        refreshers.put(knob, () -> check.setSelected(
            Boolean.parseBoolean(String.valueOf(knob.value()))));
        return check;
      }
      default -> {
        return slider(knob);
      }
    }
  }

  /**
   * Sliders count in whole numbers, so the knob's own step decides how many notches there are.
   * Asking the engine rather than picking a granularity here is what keeps a knob that moves
   * between 0 and 0.8 from getting three useful positions.
   */
  private JComponent slider(Knob knob) {
    double step = knob.step() > 0 ? knob.step() : 0.05;
    int notches = (int) Math.round((knob.maximum() - knob.minimum()) / step);

    JSlider slider = new JSlider(0, notches, notchOf(knob, step));
    slider.setPreferredSize(new Dimension(150, slider.getPreferredSize().height));

    JLabel reading = new JLabel();
    reading.setFont(reading.getFont().deriveFont(Font.PLAIN, 11f));
    Runnable show = () -> reading.setText(String.format("%.2f", asDouble(knob.value())));
    show.run();

    slider.addChangeListener(e -> {
      knob.set(knob.minimum() + slider.getValue() * step);
      show.run();
      whenChanged.run();
    });
    refreshers.put(knob, () -> {
      slider.setValue(notchOf(knob, step));
      show.run();
    });

    JPanel row = new JPanel(new BorderLayout(6, 0));
    row.setOpaque(false);
    row.add(slider, BorderLayout.CENTER);
    row.add(reading, BorderLayout.EAST);
    return row;
  }

  private int notchOf(Knob knob, double step) {
    return (int) Math.round((asDouble(knob.value()) - knob.minimum()) / step);
  }

  private double asDouble(Object value) {
    return value instanceof Number number ? number.doubleValue()
        : Double.parseDouble(String.valueOf(value));
  }

}
