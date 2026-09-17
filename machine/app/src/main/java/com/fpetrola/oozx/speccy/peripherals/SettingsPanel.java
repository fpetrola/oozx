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
package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.config.Settings;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import com.fpetrola.oozx.speccy.windows.KnobRows;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything there is to set, for whoever is being configured: a machine that is running, or what
 * a machine starts with when nobody has configured it. Who that is belongs to the window this sits
 * in, not to here.
 * <p>
 * Almost nothing here is written by hand any more. What can be set is what the parts of the
 * machine and the devices declared about themselves, and each tab is a list of which of those
 * belong to its subject: the sound of the machine and the sound cards under Audio, the tape and
 * the disks under Storage. A device that declares something nobody placed still turns up, under
 * Devices, so nothing can be declared and not shown.
 * <p>
 * This used to be forty-three controls written out one by one, of which the machine answered five
 * and the screen two. The rest were printed to the console by a stand-in core and forgotten.
 */
public class SettingsPanel extends JPanel {
  /** Which tab a declared section belongs to. What is not here shows up under Devices. */
  private static final String[][] TABS = {
      {"Audio", "sound", "covox", "specdrum", "melodik"},
      {"Input", "input"},
      {"Storage", "tape", "floppy", "plus3", "beta128", "divide", "divmmc", "zxatasp", "zxcf"},
      {"Peripherals", "interface1", "multiface"},
  };

  private final EmulatorCore emulatorCore;
  private final OOZxConfiguration config;

  public SettingsPanel(EmulatorCore core, OOZxConfiguration config) {
    super(new BorderLayout());
    this.emulatorCore = core;
    this.config = config;

    JTabbedPane tabs = new JTabbedPane();
    Set<String> placed = new LinkedHashSet<>();

    tabs.addTab("Video", theScreensOwnKnobs());
    for (String[] tab : TABS) {
      List<String> sections = List.of(tab).subList(1, tab.length);
      placed.addAll(sections);
      tabs.addTab(tab[0], "Audio".equals(tab[0])
          ? withTheVolumeOnTop(sectionsOf(sections)) : sectionsOf(sections));
    }
    tabs.addTab("Machine", machineTab(placed));
    tabs.addTab("General", generalTab());
    List<Settings.Configurable> rest = declared().stream()
        .filter(one -> !placed.contains(lastPartOf(one.device().name()))).toList();
    if (!rest.isEmpty()) {
      tabs.addTab("Devices", blocksOf(rest));
    }

    add(tabs, BorderLayout.CENTER);
  }

  /**
   * The screen's own knobs, which are the controls that work: brightness, the phosphor, the
   * scaler and the rest live on the screen and have had a window of their own for a while. This
   * used to be nine controls of its own, of which two arrived anywhere.
   */
  private JComponent theScreensOwnKnobs() {
    if (emulatorCore.getPanel() instanceof SpeccyScreen screen) {
      ScreenSettings settings = screen.getScreenSettings();
      return scrolling(new KnobRows(() -> { }).of(settings.settings()));
    }
    // No screen to turn the knobs of: what a new one is opened with is kept by the screen itself,
    // and is set from the window of a machine that has one.
    return saying("The picture is set on a machine that has one, from its own screen knobs");
  }

  /** The machine's own settings, under what it is and which ROMs it runs, which it also answers. */
  private JComponent machineTab(Set<String> placed) {
    JPanel all = new JPanel(new BorderLayout());
    all.add(whatMachineAndWhichRoms(), BorderLayout.NORTH);
    List<String> mine = List.of("speed", "memory", "machine");
    placed.addAll(mine);
    all.add(sectionsOf(mine), BorderLayout.CENTER);
    return all;
  }

  private JComponent whatMachineAndWhichRoms() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints at = at();

    JComboBox<String> model = new JComboBox<>(emulatorCore.getMachineModels().toArray(new String[0]));
    model.setSelectedItem(emulatorCore.getCurrentModel());
    model.addActionListener(e -> emulatorCore.setMachineModel((String) model.getSelectedItem()));

    JComboBox<String> roms = new JComboBox<>(emulatorCore.getRomSets().toArray(new String[0]));
    roms.setSelectedItem(emulatorCore.getRomSet());
    roms.setEnabled(roms.getItemCount() > 1);
    roms.addActionListener(e -> {
      emulatorCore.setRomSet((String) roms.getSelectedItem());
      // What it is running now, which is not what was asked for when the ROMs did not arrive.
      roms.setSelectedItem(emulatorCore.getRomSet());
    });

    row(panel, at, "Machine model", model);
    row(panel, at, "ROMs", roms);
    return panel;
  }

  /**
   * The volume above the sound settings. It is not one of the declared ones - it is asked of the
   * machine and answered by it - but it belongs here as much as they do, and it works.
   */
  private JComponent withTheVolumeOnTop(JComponent sound) {
    JPanel all = new JPanel(new BorderLayout());
    JPanel top = new JPanel(new GridBagLayout());
    GridBagConstraints at = at();
    javax.swing.JSlider volume = new javax.swing.JSlider(0, 100, emulatorCore.getVolume());
    volume.addChangeListener(e -> {
      if (!volume.getValueIsAdjusting()) {
        emulatorCore.setAudioOption("volume", volume.getValue());
      }
    });
    row(top, at, "Volume", volume);
    all.add(top, BorderLayout.NORTH);
    all.add(sound, BorderLayout.CENTER);
    return all;
  }

  /** What belongs to the program rather than to any machine: how fast it runs and what runs it. */
  private JComponent generalTab() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints at = at();

    JCheckBox turbo = new JCheckBox("", config.isTurboByDefault());
    turbo.setToolTipText("For this machine, and for every one opened from now on");
    turbo.addActionListener(e -> {
      config.setTurboByDefault(turbo.isSelected());
      config.save();
      emulatorCore.setGeneralOption("turbo", turbo.isSelected());
    });

    JComboBox<String> processor = new JComboBox<>(emulatorCore.getProcessors().toArray(new String[0]));
    processor.setToolTipText("The implementation the machine runs on: the one generated from the"
        + " model, or the model itself, which is the one to debug");
    // Chosen before the listener is on, so opening this does not change the processor.
    processor.setSelectedItem(emulatorCore.getProcessor());
    processor.addActionListener(e -> {
      String chosen = (String) processor.getSelectedItem();
      emulatorCore.setProcessor(chosen);
      config.setProcessor(chosen);
      config.save();
      Processors.startsOn = chosen;
    });

    row(panel, at, "Turbo mode", turbo);
    row(panel, at, "Processor", processor);
    return panel;
  }

  /** The declared settings of these sections, in the order they are named, each under its name. */
  private JComponent sectionsOf(List<String> sections) {
    List<Settings.Configurable> mine = new ArrayList<>();
    for (String section : sections) {
      declared().stream().filter(one -> lastPartOf(one.device().name()).equals(section)).forEach(mine::add);
    }
    return mine.isEmpty() ? saying("Nothing here has said what it can be told") : blocksOf(mine);
  }

  /**
   * Every setting of every part, in one grid: the names in one column and the controls in another,
   * with a line across for each part. Built as a panel per part - which is the obvious way - the
   * columns were each part's own, so the controls of one started where the longest name of that
   * part ended and nothing lined up with anything below it.
   */
  private JComponent blocksOf(List<Settings.Configurable> parts) {
    JPanel all = new JPanel(new GridBagLayout());
    GridBagConstraints at = at();
    for (Settings.Configurable part : parts) {
      heading(all, at, readably(lastPartOf(part.device().name())));
      for (String property : part.device().properties()) {
        row(all, at, readably(property), controlFor(part, property));
      }
    }
    fillTheRest(all, at);
    return scrolling(all);
  }

  private JComponent controlFor(Settings.Configurable device, String property) {
    Class<?> type = device.device().typeOf(property);
    Object value = device.values().get(property);
    if (type == boolean.class || type == Boolean.class) {
      JCheckBox box = new JCheckBox();
      box.setSelected(Boolean.TRUE.equals(value));
      box.addActionListener(e -> device.values().set(property, box.isSelected()));
      return box;
    }
    if (type == int.class || type == Integer.class) {
      int number = value instanceof Integer held ? held : 0;
      // A step that is worth something next to what is there: one at a time is right for forty-two
      // tracks and useless for a speed of a million per cent, where it is a hundred thousand.
      JSpinner spinner = new JSpinner(new SpinnerNumberModel(number, 0, Integer.MAX_VALUE, stepFor(number)));
      spinner.addChangeListener(e -> device.values().set(property, spinner.getValue()));
      return spinner;
    }
    if (type.isEnum()) {
      JComboBox<Object> choices = new JComboBox<>(type.getEnumConstants());
      choices.setSelectedItem(value);
      choices.addActionListener(e -> device.values().set(property, choices.getSelectedItem()));
      return choices;
    }
    if (type == String.class) {
      JTextField text = new JTextField(value == null ? "" : String.valueOf(value), 16);
      text.addActionListener(e -> device.values().set(property, text.getText()));
      return text;
    }
    // A setting that is a thing rather than a value - a pad, a key map - needs a control that
    // knows what it is. Shown as what it is and not as a box: a box would offer to put a piece of
    // text where the device wants an object of its own. Its own words if it has any: something
    // that never learnt to say what it is prints as its class and a hash, which says less.
    String said = value == null ? null : String.valueOf(value);
    JLabel asItIs = new JLabel(said == null || said.matches(".*@[0-9a-f]+$") ? type.getSimpleName() : said);
    asItIs.setEnabled(false);
    asItIs.setToolTipText("A " + type.getSimpleName()
        + ", which needs a control of its own before it can be set from here");
    return asItIs;
  }

  /** A tenth of the size of what is there, near enough: 1 for tens, 10 for hundreds, and so on. */
  private static int stepFor(int value) {
    int step = 1;
    for (int digits = String.valueOf(Math.abs(value)).length(); digits > 2; digits--) {
      step *= 10;
    }
    return step;
  }

  private List<Settings.Configurable> declared() {
    return emulatorCore.deviceSettings();
  }

  private static GridBagConstraints at() {
    GridBagConstraints at = new GridBagConstraints();
    at.insets = new Insets(4, 8, 4, 8);
    at.anchor = GridBagConstraints.WEST;
    at.gridy = 0;
    return at;
  }

  /** A line across with the part's name on it, which is what tells one part from the next. */
  private static void heading(JPanel panel, GridBagConstraints at, String name) {
    JLabel title = new JLabel(name);
    title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD));
    at.gridx = 0;
    at.gridwidth = 2;
    at.weightx = 1;
    at.fill = GridBagConstraints.HORIZONTAL;
    at.insets = new Insets(at.gridy == 0 ? 6 : 16, 8, 2, 8);
    panel.add(title, at);
    at.insets = new Insets(3, 8, 3, 8);
    at.gridwidth = 1;
    at.gridy++;
  }

  /** Everything left over goes to the bottom, so the rows stay at the top and keep their heights. */
  private static void fillTheRest(JPanel panel, GridBagConstraints at) {
    at.gridx = 0;
    at.gridwidth = 2;
    at.weighty = 1;
    at.fill = GridBagConstraints.BOTH;
    panel.add(new JPanel(), at);
  }

  /** A scroll pane that moves by a line of settings at a time rather than by three pixels. */
  private static JScrollPane scrolling(JComponent what) {
    JScrollPane scroll = new JScrollPane(what);
    scroll.getVerticalScrollBar().setUnitIncrement(24);
    scroll.getHorizontalScrollBar().setUnitIncrement(24);
    return scroll;
  }

  private static void row(JPanel panel, GridBagConstraints at, String label, JComponent control) {
    at.gridx = 0;
    at.weightx = 0;
    at.fill = GridBagConstraints.NONE;
    panel.add(new JLabel(label + ":"), at);
    at.gridx = 1;
    at.weightx = 1;
    at.fill = GridBagConstraints.HORIZONTAL;
    panel.add(control, at);
    at.gridy++;
  }

  private static JComponent saying(String what) {
    JPanel panel = new JPanel(new BorderLayout());
    JLabel says = new JLabel("  " + what);
    says.setEnabled(false);
    panel.add(says, BorderLayout.NORTH);
    return panel;
  }

  /** "machine.plus3" is the +3's disk controller: named after the part, not after the section. */
  private static String lastPartOf(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }

  /** "writeProtect" as a person reads it, since the name is all the device said about it. */
  private static String readably(String name) {
    String spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }
}
