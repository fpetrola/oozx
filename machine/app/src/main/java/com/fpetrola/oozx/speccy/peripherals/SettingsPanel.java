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
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
      {"Audio", "sound", "volume", "covox", "specdrum", "melodik"},
      {"Input", "input"},
      {"Storage", "tape", "floppy", "plus3", "beta128", "divide", "divmmc", "zxatasp", "zxcf"},
      {"Peripherals", "interface1", "multiface"},
  };

  private final EmulatorCore emulatorCore;
  private final OOZxConfiguration config;
  /** Where the settings being shown will be written, which is not where they are being read from. */
  private final List<Settings.Configurable> targets;
  private final List<Settings.Configurable> shown;
  private final Settings.Edits edits;
  /** The picture being set when there is no machine whose picture it is: written out on apply. */
  private final ScreenSettings pendingScreen;
  private final boolean live;

  public SettingsPanel(EmulatorCore core, OOZxConfiguration config, Settings.Edits edits, boolean live) {
    super(new BorderLayout());
    this.emulatorCore = core;
    this.config = config;
    this.edits = edits;
    this.targets = java.util.stream.Stream.concat(java.util.stream.Stream.of(core.ownSettings()),
        core.deviceSettings().stream()).toList();
    // Nothing changed here yet and a machine in front of it: this window becomes that machine's
    // settings, so unclipping it and applying them makes them what every machine starts with, and
    // clipping it onto another machine makes that one like this one.
    if (live && edits.isEmpty()) {
      edits.copyFrom(targets);
    }
    this.shown = edits.over(targets, live);
    this.live = live;
    // Nothing whose picture it is - no machine, or one with no screen of its own - so the knobs
    // are on a set of settings of this window's own.
    this.pendingScreen = core.getPanel() instanceof SpeccyScreen ? null : screenOverTheDefaults();

    JTabbedPane tabs = new JTabbedPane();
    Set<String> placed = new LinkedHashSet<>();

    tabs.addTab("Video", theScreensOwnKnobs());
    for (String[] tab : TABS) {
      List<String> sections = List.of(tab).subList(1, tab.length);
      placed.addAll(sections);
      tabs.addTab(tab[0], sectionsOf(sections));
    }
    tabs.addTab("Machine", machineTab(placed));
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
    if (emulatorCore.getPanel() instanceof SpeccyScreen screen && pendingScreen == null) {
      // The same panel the screen's own window shows, looks and all: it is the same question -
      // what this picture should be - asked from another place.
      return new com.fpetrola.oozx.speccy.desktop.ScreenSettingsPanel(screen.getScreenSettings(),
          kept -> {
            config.setScreenDefaults(new LinkedHashMap<>(kept));
            config.save();
          }, () -> { });
    }
    // Its own, which becomes what a new screen is opened with when this is applied.
    return new com.fpetrola.oozx.speccy.desktop.ScreenSettingsPanel(pendingScreen, kept -> {
      ScreenSettings.setDefaults(new LinkedHashMap<>(kept));
      config.setScreenDefaults(new LinkedHashMap<>(kept));
      config.save();
    }, () -> { });
  }

  /** What the machine is, before how fast it runs and what it remembers: all of it declared. */
  private JComponent machineTab(Set<String> placed) {
    List<String> mine = List.of("hardware", "speed", "memory", "machine");
    placed.addAll(mine);
    return sectionsOf(mine);
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
        JComponent control = controlFor(part, property);
        // What it is and where it goes, which is all anybody said about it: the declaration gives
        // a name and a type and no words, and the name of the section is where the value lands.
        // Whatever was said about it, and where the value lands either way: most of them said
        // nothing, and then the name and the type is all there is to tell anybody.
        String said = part.device().saidAbout(property);
        control.setToolTipText((said.isEmpty() ? "" : said + "  -  ") + part.device().name() + "."
            + property + "  (" + part.device().typeOf(property).getSimpleName() + ")");
        row(all, at, readably(property), control);
      }
    }
    fillTheRest(all, at);
    return scrolling(all);
  }

  private JComponent controlFor(Settings.Configurable device, String property) {
    Class<?> type = device.device().typeOf(property);
    Object value = device.values().get(property);
    // An enum says what it can be by being one; anything else that is one of a few says so itself.
    List<?> choices = type.isEnum() ? List.of(type.getEnumConstants()) : device.device().choicesFor(property);
    if (!choices.isEmpty()) {
      JComboBox<Object> offered = new JComboBox<>(choices.toArray());
      offered.setSelectedItem(value);
      offered.setEnabled(choices.size() > 1);
      com.fpetrola.oozx.speccy.windows.Widgets.whenChosen(offered,
          () -> device.values().get(property), chosen -> {
            device.values().set(property, chosen);
            // What it is now, which is not what was asked for when a machine's ROMs did not arrive.
            offered.setSelectedItem(device.values().get(property));
          });
      return offered;
    }
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
      spinner.addChangeListener(e -> {
        device.values().set(property, spinner.getValue());
        // What stuck, which is not always what was asked for: a part that knows what it can do
        // with a number takes the nearest one it can, and the box has to say so rather than show
        // something nothing is.
        Object kept = device.values().get(property);
        if (kept instanceof Integer held && !held.equals(spinner.getValue())) {
          spinner.setValue(held);
        }
      });
      return spinner;
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
    // Greyed rather than disabled: Swing does not show the tooltip of a disabled component, so
    // saying why it cannot be changed and then disabling it says nothing to anybody.
    asItIs.setForeground(java.awt.Color.GRAY);
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
    return shown;
  }

  /**
   * Writes every change made here into whatever this window is on now: the machine it is clipped
   * onto, or - clipped onto nothing - the file every machine opened from now on is built from.
   * <p>
   * This is also the only place the file is written from, which is why nothing else here saves it:
   * what is set in this window is not written down until somebody says to write it.
   */
  public void apply() {
    edits.applyTo(targets);
    if (!live && pendingScreen != null) {
      Map<String, String> picture = pendingScreen.values();
      ScreenSettings.setDefaults(picture);
      config.setScreenDefaults(new LinkedHashMap<>(picture));
    }
    config.save();
  }

  private static ScreenSettings screenOverTheDefaults() {
    ScreenSettings starting = new ScreenSettings();
    starting.apply(ScreenSettings.getDefaults());
    return starting;
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
    JLabel name = new JLabel(label + ":");
    // The name says what the control says: a tooltip is looked for where the pointer is, and it is
    // as often on the name as on the box.
    name.setToolTipText(control.getToolTipText());
    panel.add(name, at);
    at.gridx = 1;
    at.weightx = 1;
    at.fill = GridBagConstraints.HORIZONTAL;
    panel.add(control, at);
    at.gridy++;
  }

  private static JComponent saying(String what) {
    JPanel panel = new JPanel(new BorderLayout());
    JLabel says = new JLabel("  " + what);
    says.setForeground(java.awt.Color.GRAY);
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
