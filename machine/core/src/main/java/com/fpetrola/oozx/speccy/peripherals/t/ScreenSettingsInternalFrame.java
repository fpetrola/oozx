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

import com.fpetrola.oozx.speccy.screen.ScreenProfile;
import com.fpetrola.oozx.speccy.screen.ScreenSetting;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * How one emulator draws its picture.
 * <p>
 * Built by walking the knobs the screen offers rather than listing them here, so an effect added
 * to the engine appears in this window without anybody editing it. That is the whole point of
 * the settings describing themselves: a window written as a hand-made mirror of the list goes
 * out of step with it on the third change and nobody notices which side is wrong.
 * <p>
 * Changes land on the emulator as they are made — this is the window someone watches the picture
 * through while they turn things, so it would be no use if it needed closing first. They belong
 * to that emulator alone; the button at the bottom is how they become what new ones open with.
 */
public class ScreenSettingsInternalFrame extends JInternalFrame {

  private final ScreenSettings settings;
  private final Consumer<Map<String, String>> keepAsDefault;
  private final Runnable profilesChanged;
  private final Map<ScreenSetting, Runnable> refreshers = new LinkedHashMap<>();
  private final JComboBox<ScreenProfile> profiles = new JComboBox<>();
  private final JButton forget = new JButton("Forget");
  /** Set while the window is writing to its own controls, so echoes are not read as choices. */
  private boolean settingUp;

  public ScreenSettingsInternalFrame(String machineName, ScreenSettings settings,
                                     Consumer<Map<String, String>> keepAsDefault,
                                     Runnable profilesChanged) {
    super("Screen - " + machineName, true, true, true, true);
    this.settings = settings;
    this.keepAsDefault = keepAsDefault;
    this.profilesChanged = profilesChanged;

    setLayout(new BorderLayout());
    add(profiles(), BorderLayout.NORTH);
    add(new JScrollPane(knobs()), BorderLayout.CENTER);
    add(buttons(), BorderLayout.SOUTH);
    showCurrentProfile();
    setSize(430, 560);
  }

  /**
   * The looks to start from, above the knobs that adjust them.
   * <p>
   * The list is asked for the profile that matches what is on screen rather than remembering
   * which was picked: turning a knob after choosing one means you are no longer looking at it,
   * and a box still naming it would be describing a picture that is not there. That is what the
   * blank entry says.
   */
  private JPanel profiles() {
    JPanel bar = new JPanel(new BorderLayout(6, 0));
    bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
    bar.add(new JLabel("Look"), BorderLayout.WEST);

    profiles.setRenderer((list, profile, index, selected, focused) ->
        new JLabel(profile == null ? "(adjusted)" : profile.name()));
    profiles.addActionListener(e -> {
      if (settingUp) return;
      ScreenProfile chosen = (ScreenProfile) profiles.getSelectedItem();
      if (chosen == null) return;
      settings.apply(chosen);
      refreshers.values().forEach(Runnable::run);
      forget.setEnabled(!chosen.builtIn());
    });
    bar.add(profiles, BorderLayout.CENTER);

    JPanel keep = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
    keep.setOpaque(false);

    JButton save = new JButton("Save as...");
    save.setToolTipText("Keep what is set here as a look of its own");
    save.addActionListener(e -> saveAsProfile());
    keep.add(save);

    forget.setToolTipText("Remove this saved look");
    forget.addActionListener(e -> forgetProfile());
    keep.add(forget);

    bar.add(keep, BorderLayout.EAST);
    return bar;
  }

  private void saveAsProfile() {
    String name = JOptionPane.showInputDialog(this, "Name for this look:", "Save look",
        JOptionPane.QUESTION_MESSAGE);
    if (name == null || name.isBlank()) return;

    settings.keepAs(name.trim());
    profilesChanged.run();
    showCurrentProfile();
  }

  private void forgetProfile() {
    ScreenProfile chosen = (ScreenProfile) profiles.getSelectedItem();
    // The six that come with it are what someone goes back to when their own experiment went
    // wrong, so they are not theirs to remove.
    if (chosen == null || chosen.builtIn()) return;

    ScreenSettings.forget(chosen.name());
    profilesChanged.run();
    showCurrentProfile();
  }

  /** Reloads the list and points it at whatever the picture currently matches, or at nothing. */
  private void showCurrentProfile() {
    settingUp = true;
    try {
      profiles.removeAllItems();
      for (ScreenProfile profile : ScreenSettings.profiles()) {
        profiles.addItem(profile);
      }
      ScreenProfile showing = settings.currentProfile();
      profiles.setSelectedItem(showing);
      forget.setEnabled(showing != null && !showing.builtIn());
    } finally {
      settingUp = false;
    }
  }

  private JPanel knobs() {
    JPanel all = new JPanel();
    all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));

    ScreenSetting.Group showing = null;
    JPanel section = null;
    int row = 0;

    for (ScreenSetting knob : settings.settings()) {
      if (knob.group() != showing) {
        showing = knob.group();
        section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(showing.label()));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        all.add(section);
        row = 0;
      }
      addRow(section, knob, row++);
    }

    all.add(Box.createVerticalGlue());
    return all;
  }

  private void addRow(JPanel section, ScreenSetting knob, int row) {
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

  private JComponent controlFor(ScreenSetting knob) {
    switch (knob.kind()) {
      case CHOICE -> {
        JComboBox<String> box = new JComboBox<>(knob.options().toArray(new String[0]));
        box.setSelectedItem(String.valueOf(knob.value()));
        box.addActionListener(e -> {
          knob.set(box.getSelectedItem());
          showCurrentProfile();
        });
        refreshers.put(knob, () -> box.setSelectedItem(String.valueOf(knob.value())));
        return box;
      }
      case SWITCH -> {
        JCheckBox check = new JCheckBox("", Boolean.parseBoolean(String.valueOf(knob.value())));
        check.addActionListener(e -> {
          knob.set(check.isSelected());
          showCurrentProfile();
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
  private JComponent slider(ScreenSetting knob) {
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
      showCurrentProfile();
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

  private int notchOf(ScreenSetting knob, double step) {
    return (int) Math.round((asDouble(knob.value()) - knob.minimum()) / step);
  }

  private double asDouble(Object value) {
    return value instanceof Number number ? number.doubleValue()
        : Double.parseDouble(String.valueOf(value));
  }

  private JToolBar buttons() {
    JToolBar bar = new JToolBar();
    bar.setFloatable(false);

    JButton asDefault = new JButton("Use as default");
    asDefault.setToolTipText("Emulators opened from now on start with what is set here");
    asDefault.addActionListener(e -> {
      settings.makeDefault();
      keepAsDefault.accept(ScreenSettings.getDefaults());
    });
    bar.add(asDefault);

    JButton restore = new JButton("Restore default");
    restore.setToolTipText("Put this emulator back to what new ones start with");
    restore.addActionListener(e -> {
      settings.apply(ScreenSettings.getDefaults());
      refreshers.values().forEach(Runnable::run);
    });
    bar.add(restore);

    return bar;
  }
}
