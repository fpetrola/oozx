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

import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Everything there is to set, for whoever is being configured: a machine that is running, or what
 * a machine starts with when nobody has configured it.
 * <p>
 * Who that is belongs to the window this sits in, not to here: it is handed a core and builds its
 * controls against it, and is built again when the window is attached to another machine.
 * <p>
 * Most of these controls do nothing yet. They are shown disabled rather than removed, so that what
 * the emulator cannot be told is as visible as what it can: of the 43 options this window used to
 * send, the machine answered five and the screen two, and the rest were printed to the console by
 * a stand-in core and forgotten.
 */
public class SettingsPanel extends JPanel {
  private EmulatorCore emulatorCore;
  private final OOZxConfiguration config;

  public SettingsPanel(EmulatorCore core, OOZxConfiguration config) {
    super(new BorderLayout());
    this.emulatorCore = core;
    this.config = config;

    JTabbedPane tabbedPane = new JTabbedPane();

    // Video Tab
    JPanel videoPanel = createVideoPanel();
    tabbedPane.addTab("Video", videoPanel);

    // Audio Tab
    JPanel audioPanel = createAudioPanel();
    tabbedPane.addTab("Audio", audioPanel);

    // Input Tab
    JPanel inputPanel = createInputPanel();
    tabbedPane.addTab("Input", inputPanel);

    // Storage Tab
    JPanel storagePanel = createStoragePanel();
    tabbedPane.addTab("Storage", storagePanel);

    // Machine Tab
    JPanel machinePanel = createMachinePanel();
    tabbedPane.addTab("Machine", machinePanel);

    // Peripherals Tab
    JPanel peripheralsPanel = createPeripheralsPanel();
    tabbedPane.addTab("Peripherals", peripheralsPanel);

    // General Tab
    JPanel generalPanel = createGeneralPanel();
    tabbedPane.addTab("General", generalPanel);

    greyOutWhatGoesNowhere(tabbedPane);
    // After the greying, because what a device declared does reach it and is built already marked.
    // In one tab of its own rather than thirteen more along the top: "Peripherals" is the list of
    // what can be fitted, which nothing reads yet, and this is what each fitted one can be told.
    tabbedPane.addTab("Devices", whatEachDeviceDeclared());
    add(tabbedPane, BorderLayout.CENTER);
  }

  /**
   * One tab for each device that said it has settings, built from what it said rather than from a
   * list kept here: the device's own module names the properties, and the type of each one decides
   * the control. A device nobody has declared settings for has no tab, and a property added to a
   * declaration turns up here without anybody writing a control for it.
   */
  private JComponent whatEachDeviceDeclared() {
    java.util.List<com.fpetrola.oozx.config.Settings.Configurable> devices = emulatorCore.deviceSettings();
    if (devices.isEmpty()) {
      return new JLabel("  No device has said what it can be told yet");
    }
    // Down the side: thirteen names along the top is a scrolling strip, and down a side they are
    // a list of what this machine has.
    JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    for (com.fpetrola.oozx.config.Settings.Configurable device : devices) {
      tabs.addTab(readably(lastPartOf(device.device().name())), deviceTab(device));
    }
    return tabs;
  }

  private JPanel deviceTab(com.fpetrola.oozx.config.Settings.Configurable device) {
    JPanel panel = new JPanel(new java.awt.GridBagLayout());
    java.awt.GridBagConstraints at = new java.awt.GridBagConstraints();
    at.insets = new Insets(4, 8, 4, 8);
    at.anchor = java.awt.GridBagConstraints.WEST;
    at.gridy = 0;
    for (String property : device.device().properties()) {
      at.gridx = 0;
      at.weightx = 0;
      panel.add(new JLabel(readably(property) + ":"), at);
      at.gridx = 1;
      at.weightx = 1;
      at.fill = java.awt.GridBagConstraints.HORIZONTAL;
      panel.add(controlFor(device, property), at);
      at.gridy++;
    }
    at.gridy++;
    at.weighty = 1;
    at.fill = java.awt.GridBagConstraints.BOTH;
    panel.add(new JPanel(), at);
    return panel;
  }

  /** The control a setting gets, which is decided by what kind of thing the device says it is. */
  private JComponent controlFor(com.fpetrola.oozx.config.Settings.Configurable device, String property) {
    Class<?> type = device.device().typeOf(property);
    Object value = device.values().get(property);
    if (type == boolean.class || type == Boolean.class) {
      JCheckBox box = new JCheckBox();
      box.setSelected(Boolean.TRUE.equals(value));
      box.addActionListener(e -> device.values().set(property, box.isSelected()));
      return reachesTheMachine(box);
    }
    if (type == int.class || type == Integer.class) {
      int number = value instanceof Integer held ? held : 0;
      JSpinner spinner = new JSpinner(new SpinnerNumberModel(number, 0, Integer.MAX_VALUE, 1));
      spinner.addChangeListener(e -> device.values().set(property, spinner.getValue()));
      return reachesTheMachine(spinner);
    }
    if (type.isEnum()) {
      JComboBox<Object> choices = new JComboBox<>(type.getEnumConstants());
      choices.setSelectedItem(value);
      choices.addActionListener(e -> device.values().set(property, choices.getSelectedItem()));
      return reachesTheMachine(choices);
    }
    if (type == String.class) {
      JTextField text = new JTextField(value == null ? "" : String.valueOf(value), 16);
      text.addActionListener(e -> device.values().set(property, text.getText()));
      return reachesTheMachine(text);
    }
    // A setting that is a thing rather than a value - a pad, a key map - needs a control that
    // knows what it is. Shown as what it is and not as a box: a box would offer to put a piece of
    // text where the device wants an object of its own.
    // Its own words if it has any: a thing that never learnt to say what it is prints as its
    // class and a hash, which says less than the name of the kind of thing it is.
    String said = value == null ? null : String.valueOf(value);
    JLabel asItIs = new JLabel(said == null || said.matches(".*@[0-9a-f]+$") ? type.getSimpleName() : said);
    asItIs.setEnabled(false);
    asItIs.setToolTipText("A " + type.getSimpleName()
        + ", which needs a control of its own before it can be set from here");
    return asItIs;
  }

  /** "machine.plus3" is the +3's disk controller: the tab is named after the part, not the section. */
  private static String lastPartOf(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }

  /** "writeProtect" as a person reads it, since the name is all the device said about it. */
  private static String readably(String name) {
    String spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  /** What a control that reaches the machine is marked with; everything else is shown but dead. */
  private static final String REACHES_THE_MACHINE = "reachesTheMachine";
  private static final String GOES_NOWHERE =
      "The emulator cannot be told this yet: nothing reads it, so it is here to be seen and not used";

  /**
   * Marks a control as one that arrives somewhere. Marking is the way round it is, rather than
   * listing what is dead, so that a control added later is dead until somebody says otherwise -
   * which is what went wrong here in the first place.
   */
  private static <T extends JComponent> T reachesTheMachine(T control) {
    control.putClientProperty(REACHES_THE_MACHINE, true);
    return control;
  }

  private static void greyOutWhatGoesNowhere(Container where) {
    for (Component child : where.getComponents()) {
      if (child instanceof JComponent control && takesInput(control)
          && control.getClientProperty(REACHES_THE_MACHINE) == null) {
        control.setEnabled(false);
        control.setToolTipText(GOES_NOWHERE);
      }
      if (child instanceof Container inside) {
        greyOutWhatGoesNowhere(inside);
      }
    }
  }

  private static boolean takesInput(JComponent control) {
    return control instanceof JCheckBox || control instanceof JComboBox<?> || control instanceof JSlider
        || control instanceof JSpinner || control instanceof JTextField;
  }

  private JPanel createVideoPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel borderLabel = new JLabel("Show Border:");
    JCheckBox borderCheck = new JCheckBox();
    if (emulatorCore.getPanel() instanceof com.fpetrola.oozx.speccy.screen.SpeccyScreen screen) {
      borderCheck.setSelected(screen.getScreenSettings().isBorder());
    }
    reachesTheMachine(borderCheck).addActionListener(e -> emulatorCore.setVideoOption("border", borderCheck.isSelected()));

    JLabel scanlinesLabel = new JLabel("Scanlines:");
    JCheckBox scanlinesCheck = new JCheckBox();
    reachesTheMachine(scanlinesCheck).addActionListener(e -> emulatorCore.setVideoOption("scanlines", scanlinesCheck.isSelected()));

    JLabel brightnessLabel = new JLabel("Brightness:");
    JSlider brightnessSlider = new JSlider(0, 100, 50);
    brightnessSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (!brightnessSlider.getValueIsAdjusting()) {
          emulatorCore.setVideoOption("brightness", brightnessSlider.getValue());
        }
      }
    });

    JLabel contrastLabel = new JLabel("Contrast:");
    JSlider contrastSlider = new JSlider(0, 100, 50);
    contrastSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (!contrastSlider.getValueIsAdjusting()) {
          emulatorCore.setVideoOption("contrast", contrastSlider.getValue());
        }
      }
    });

    JLabel ulaTypeLabel = new JLabel("ULA Type:");
    String[] ulaTypes = {"Standard", "Timex", "Pentagon"};
    JComboBox<String> ulaTypeCombo = new JComboBox<>(ulaTypes);
    ulaTypeCombo.addActionListener(e -> emulatorCore.setVideoOption("ula_type", ulaTypeCombo.getSelectedItem()));

    JLabel filterLabel = new JLabel("Display Filter:");
    String[] filters = {"None", "TV2x", "TV3x", "HQ2x", "HQ3x", "Dot Matrix", "PAL TV"};
    JComboBox<String> filterCombo = new JComboBox<>(filters);
    filterCombo.addActionListener(e -> emulatorCore.setVideoOption("filter", filterCombo.getSelectedItem()));

    JLabel aspectLabel = new JLabel("Preserve Aspect Ratio:");
    JCheckBox aspectCheck = new JCheckBox();
    aspectCheck.addActionListener(e -> emulatorCore.setVideoOption("aspect_ratio", aspectCheck.isSelected()));

    JLabel scalingLabel = new JLabel("Scaling Method:");
    String[] scalings = {"Nearest Neighbor", "Bilinear", "Bicubic"};
    JComboBox<String> scalingCombo = new JComboBox<>(scalings);
    scalingCombo.addActionListener(e -> emulatorCore.setVideoOption("scaling", scalingCombo.getSelectedItem()));

    JLabel snowLabel = new JLabel("Snow Effect:");
    JCheckBox snowCheck = new JCheckBox();
    snowCheck.addActionListener(e -> emulatorCore.setVideoOption("snow", snowCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(borderLabel)
            .addComponent(scanlinesLabel)
            .addComponent(brightnessLabel)
            .addComponent(contrastLabel)
            .addComponent(ulaTypeLabel)
            .addComponent(filterLabel)
            .addComponent(aspectLabel)
            .addComponent(scalingLabel)
            .addComponent(snowLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(borderCheck)
            .addComponent(scanlinesCheck)
            .addComponent(brightnessSlider)
            .addComponent(contrastSlider)
            .addComponent(ulaTypeCombo)
            .addComponent(filterCombo)
            .addComponent(aspectCheck)
            .addComponent(scalingCombo)
            .addComponent(snowCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(borderLabel)
            .addComponent(borderCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(scanlinesLabel)
            .addComponent(scanlinesCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(brightnessLabel)
            .addComponent(brightnessSlider))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(contrastLabel)
            .addComponent(contrastSlider))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(ulaTypeLabel)
            .addComponent(ulaTypeCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(filterLabel)
            .addComponent(filterCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(aspectLabel)
            .addComponent(aspectCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(scalingLabel)
            .addComponent(scalingCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(snowLabel)
            .addComponent(snowCheck))
    );

    return panel;
  }

  private JPanel createAudioPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel volumeLabel = new JLabel("Master Volume:");
    JSlider volumeSlider = new JSlider(0, 100, 50);
    reachesTheMachine(volumeSlider).addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (!volumeSlider.getValueIsAdjusting()) {
          emulatorCore.setAudioOption("volume", volumeSlider.getValue());
        }
      }
    });

    JLabel ayChipLabel = new JLabel("AY Chip Emulation:");
    JCheckBox ayCheck = new JCheckBox();
    ayCheck.addActionListener(e -> emulatorCore.setAudioOption("ay", ayCheck.isSelected()));

    JLabel beeperVolumeLabel = new JLabel("Beeper Volume:");
    JSlider beeperVolumeSlider = new JSlider(0, 100, 50);
    beeperVolumeSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (!beeperVolumeSlider.getValueIsAdjusting()) {
          emulatorCore.setAudioOption("beeper_volume", beeperVolumeSlider.getValue());
        }
      }
    });

    JLabel ayVolumeLabel = new JLabel("AY Volume:");
    JSlider ayVolumeSlider = new JSlider(0, 100, 50);
    ayVolumeSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (!ayVolumeSlider.getValueIsAdjusting()) {
          emulatorCore.setAudioOption("ay_volume", ayVolumeSlider.getValue());
        }
      }
    });

    JLabel stereoLabel = new JLabel("AY Stereo Separation:");
    String[] stereos = {"None", "ABC", "ACB", "Mono"};
    JComboBox<String> stereoCombo = new JComboBox<>(stereos);
    stereoCombo.addActionListener(e -> emulatorCore.setAudioOption("stereo_separation", stereoCombo.getSelectedItem()));

    JLabel sampleRateLabel = new JLabel("Sample Rate:");
    String[] rates = {"22050 Hz", "44100 Hz", "48000 Hz"};
    JComboBox<String> sampleRateCombo = new JComboBox<>(rates);
    sampleRateCombo.addActionListener(e -> emulatorCore.setAudioOption("sample_rate", sampleRateCombo.getSelectedItem()));

    JLabel hqBeeperLabel = new JLabel("High Quality Beeper:");
    JCheckBox hqBeeperCheck = new JCheckBox();
    hqBeeperCheck.addActionListener(e -> emulatorCore.setAudioOption("hq_beeper", hqBeeperCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(volumeLabel)
            .addComponent(ayChipLabel)
            .addComponent(beeperVolumeLabel)
            .addComponent(ayVolumeLabel)
            .addComponent(stereoLabel)
            .addComponent(sampleRateLabel)
            .addComponent(hqBeeperLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(volumeSlider)
            .addComponent(ayCheck)
            .addComponent(beeperVolumeSlider)
            .addComponent(ayVolumeSlider)
            .addComponent(stereoCombo)
            .addComponent(sampleRateCombo)
            .addComponent(hqBeeperCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(volumeLabel)
            .addComponent(volumeSlider))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(ayChipLabel)
            .addComponent(ayCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(beeperVolumeLabel)
            .addComponent(beeperVolumeSlider))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(ayVolumeLabel)
            .addComponent(ayVolumeSlider))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(stereoLabel)
            .addComponent(stereoCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(sampleRateLabel)
            .addComponent(sampleRateCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(hqBeeperLabel)
            .addComponent(hqBeeperCheck))
    );

    return panel;
  }

  private JPanel createInputPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel keyboardLabel = new JLabel("Keyboard Layout:");
    String[] keyboards = {"QWERTY", "AZERTY", "Spanish", "Russian"};
    JComboBox<String> keyboardCombo = new JComboBox<>(keyboards);
    keyboardCombo.addActionListener(e -> emulatorCore.setInputOption("keyboard_layout", keyboardCombo.getSelectedItem()));

    JLabel joystickLabel = new JLabel("Joystick Type:");
    String[] joysticks = {"None", "Kempston", "Sinclair 1", "Sinclair 2", "Cursor", "Fuller"};
    JComboBox<String> joystickCombo = new JComboBox<>(joysticks);
    joystickCombo.addActionListener(e -> emulatorCore.setInputOption("joystick", joystickCombo.getSelectedItem()));

    JLabel issueLabel = new JLabel("Keyboard Issue:");
    ButtonGroup issueGroup = new ButtonGroup();
    JRadioButton issue2 = new JRadioButton("Issue 2");
    issue2.addActionListener(e -> emulatorCore.setInputOption("keyboard_issue", "2"));
    issueGroup.add(issue2);
    JRadioButton issue3 = new JRadioButton("Issue 3");
    issue3.addActionListener(e -> emulatorCore.setInputOption("keyboard_issue", "3"));
    issueGroup.add(issue3);

    JLabel mouseLabel = new JLabel("Emulate Mouse:");
    JCheckBox mouseCheck = new JCheckBox();
    mouseCheck.addActionListener(e -> emulatorCore.setInputOption("mouse", mouseCheck.isSelected()));

    JLabel joystickPromptLabel = new JLabel("Joystick Keyboard Prompt:");
    JCheckBox joystickPromptCheck = new JCheckBox();
    joystickPromptCheck.addActionListener(e -> emulatorCore.setInputOption("joystick_prompt", joystickPromptCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(keyboardLabel)
            .addComponent(joystickLabel)
            .addComponent(issueLabel)
            .addComponent(mouseLabel)
            .addComponent(joystickPromptLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(keyboardCombo)
            .addComponent(joystickCombo)
            .addGroup(layout.createSequentialGroup()
                .addComponent(issue2)
                .addComponent(issue3))
            .addComponent(mouseCheck)
            .addComponent(joystickPromptCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(keyboardLabel)
            .addComponent(keyboardCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(joystickLabel)
            .addComponent(joystickCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(issueLabel)
            .addComponent(issue2)
            .addComponent(issue3))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(mouseLabel)
            .addComponent(mouseCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(joystickPromptLabel)
            .addComponent(joystickPromptCheck))
    );

    return panel;
  }

  private JPanel createStoragePanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel tapeSpeedLabel = new JLabel("Tape Loading Speed:");
    String[] tapeSpeeds = {"Normal", "Fast", "Turbo"};
    JComboBox<String> tapeSpeedCombo = new JComboBox<>(tapeSpeeds);
    tapeSpeedCombo.addActionListener(e -> emulatorCore.setStorageOption("tape_speed", tapeSpeedCombo.getSelectedItem()));

    JLabel diskTypeLabel = new JLabel("Disk Interface:");
    String[] diskTypes = {"None", "+3", "Beta 128", "Opus", "TRDOS"};
    JComboBox<String> diskTypeCombo = new JComboBox<>(diskTypes);
    diskTypeCombo.addActionListener(e -> emulatorCore.setStorageOption("disk_interface", diskTypeCombo.getSelectedItem()));

    JLabel fastLoadLabel = new JLabel("Accelerate Tape Loading:");
    JCheckBox fastLoadCheck = new JCheckBox();
    fastLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("fast_load", fastLoadCheck.isSelected()));

    JLabel autoLoadLabel = new JLabel("Auto Load Tapes:");
    JCheckBox autoLoadCheck = new JCheckBox();
    autoLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("auto_load", autoLoadCheck.isSelected()));

    JLabel trapLoadLabel = new JLabel("Trap Tape Loading:");
    JCheckBox trapLoadCheck = new JCheckBox();
    trapLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("trap_load", trapLoadCheck.isSelected()));

    JLabel microdriveLabel = new JLabel("Emulate Microdrives:");
    JCheckBox microdriveCheck = new JCheckBox();
    microdriveCheck.addActionListener(e -> emulatorCore.setStorageOption("microdrive", microdriveCheck.isSelected()));

    JLabel writeProtectLabel = new JLabel("Write Protect Disks:");
    JCheckBox writeProtectCheck = new JCheckBox();
    writeProtectCheck.addActionListener(e -> emulatorCore.setStorageOption("write_protect", writeProtectCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tapeSpeedLabel)
            .addComponent(diskTypeLabel)
            .addComponent(fastLoadLabel)
            .addComponent(autoLoadLabel)
            .addComponent(trapLoadLabel)
            .addComponent(microdriveLabel)
            .addComponent(writeProtectLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tapeSpeedCombo)
            .addComponent(diskTypeCombo)
            .addComponent(fastLoadCheck)
            .addComponent(autoLoadCheck)
            .addComponent(trapLoadCheck)
            .addComponent(microdriveCheck)
            .addComponent(writeProtectCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(tapeSpeedLabel)
            .addComponent(tapeSpeedCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(diskTypeLabel)
            .addComponent(diskTypeCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(fastLoadLabel)
            .addComponent(fastLoadCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(autoLoadLabel)
            .addComponent(autoLoadCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(trapLoadLabel)
            .addComponent(trapLoadCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(microdriveLabel)
            .addComponent(microdriveCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(writeProtectLabel)
            .addComponent(writeProtectCheck))
    );

    return panel;
  }

  private JPanel createMachinePanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel modelLabel = new JLabel("Machine Model:");
    JComboBox<String> modelCombo = new JComboBox<>(emulatorCore.getMachineModels().toArray(new String[0]));
    // Chosen before the listener is on, so opening the dialog is not a change of machine.
    modelCombo.setSelectedItem(emulatorCore.getCurrentModel());
    modelCombo.addActionListener(e -> emulatorCore.setMachineModel((String) modelCombo.getSelectedItem()));

    // The same machine was sold with other ROMs in it: another language, a later revision. Empty
    // for a machine that only ever had one set, and then there is nothing to offer.
    JLabel romSetLabel = new JLabel("ROMs:");
    JComboBox<String> romSetCombo = new JComboBox<>(emulatorCore.getRomSets().toArray(new String[0]));
    romSetCombo.setEnabled(romSetCombo.getItemCount() > 1);
    romSetCombo.setSelectedItem(emulatorCore.getRomSet());
    romSetCombo.addActionListener(e -> {
      emulatorCore.setRomSet((String) romSetCombo.getSelectedItem());
      // What it is running now, which is not what was asked for when the ROMs did not arrive.
      romSetCombo.setSelectedItem(emulatorCore.getRomSet());
    });

    JLabel romLabel = new JLabel("Custom ROM:");
    JTextField romField = new JTextField(20);
    JButton browseButton = new JButton("Browse...");
    browseButton.addActionListener(e -> {
      JFileChooser fc = new JFileChooser();
      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        romField.setText(fc.getSelectedFile().getPath());
        emulatorCore.setGeneralOption("custom_rom", romField.getText());
      }
    });

    JLabel lateTimingsLabel = new JLabel("Late Timings:");
    JCheckBox lateTimingsCheck = new JCheckBox();
    lateTimingsCheck.addActionListener(e -> emulatorCore.setGeneralOption("late_timings", lateTimingsCheck.isSelected()));

    JLabel contentionLabel = new JLabel("Memory Contention:");
    JCheckBox contentionCheck = new JCheckBox();
    contentionCheck.addActionListener(e -> emulatorCore.setGeneralOption("contention", contentionCheck.isSelected()));

    JLabel highResLabel = new JLabel("High Resolution Mode:");
    JCheckBox highResCheck = new JCheckBox();
    highResCheck.addActionListener(e -> emulatorCore.setGeneralOption("high_res", highResCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(modelLabel)
            .addComponent(romSetLabel)
            .addComponent(romLabel)
            .addComponent(lateTimingsLabel)
            .addComponent(contentionLabel)
            .addComponent(highResLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(modelCombo)
            .addComponent(romSetCombo)
            .addGroup(layout.createSequentialGroup()
                .addComponent(romField)
                .addComponent(browseButton))
            .addComponent(lateTimingsCheck)
            .addComponent(contentionCheck)
            .addComponent(highResCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(modelLabel)
            .addComponent(modelCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(romSetLabel)
            .addComponent(romSetCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(romLabel)
            .addComponent(romField)
            .addComponent(browseButton))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(lateTimingsLabel)
            .addComponent(lateTimingsCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(contentionLabel)
            .addComponent(contentionCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(highResLabel)
            .addComponent(highResCheck))
    );

    return panel;
  }

  private JPanel createPeripheralsPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel if1Label = new JLabel("ZX Interface 1:");
    JCheckBox if1Check = new JCheckBox();
    if1Check.addActionListener(e -> emulatorCore.setPeripheralOption("if1", if1Check.isSelected()));

    JLabel if2Label = new JLabel("ZX Interface 2:");
    JCheckBox if2Check = new JCheckBox();
    if2Check.addActionListener(e -> emulatorCore.setPeripheralOption("if2", if2Check.isSelected()));

    JLabel printerLabel = new JLabel("ZX Printer:");
    JCheckBox printerCheck = new JCheckBox();
    printerCheck.addActionListener(e -> emulatorCore.setPeripheralOption("printer", printerCheck.isSelected()));

    JLabel kempstonMouseLabel = new JLabel("Kempston Mouse:");
    JCheckBox kempstonMouseCheck = new JCheckBox();
    kempstonMouseCheck.addActionListener(e -> emulatorCore.setPeripheralOption("kempston_mouse", kempstonMouseCheck.isSelected()));

    JLabel fullerLabel = new JLabel("Fuller Box:");
    JCheckBox fullerCheck = new JCheckBox();
    fullerCheck.addActionListener(e -> emulatorCore.setPeripheralOption("fuller", fullerCheck.isSelected()));

    JLabel melodikLabel = new JLabel("Melodik AY:");
    JCheckBox melodikCheck = new JCheckBox();
    melodikCheck.addActionListener(e -> emulatorCore.setPeripheralOption("melodik", melodikCheck.isSelected()));

    JLabel specdrumLabel = new JLabel("SpecDrum:");
    JCheckBox specdrumCheck = new JCheckBox();
    specdrumCheck.addActionListener(e -> emulatorCore.setPeripheralOption("specdrum", specdrumCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(if1Label)
            .addComponent(if2Label)
            .addComponent(printerLabel)
            .addComponent(kempstonMouseLabel)
            .addComponent(fullerLabel)
            .addComponent(melodikLabel)
            .addComponent(specdrumLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(if1Check)
            .addComponent(if2Check)
            .addComponent(printerCheck)
            .addComponent(kempstonMouseCheck)
            .addComponent(fullerCheck)
            .addComponent(melodikCheck)
            .addComponent(specdrumCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(if1Label)
            .addComponent(if1Check))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(if2Label)
            .addComponent(if2Check))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(printerLabel)
            .addComponent(printerCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(kempstonMouseLabel)
            .addComponent(kempstonMouseCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(fullerLabel)
            .addComponent(fullerCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(melodikLabel)
            .addComponent(melodikCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(specdrumLabel)
            .addComponent(specdrumCheck))
    );

    return panel;
  }

  private JPanel createGeneralPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel turboLabel = new JLabel("Turbo Mode:");
    JCheckBox turboCheck = new JCheckBox("", config.isTurboByDefault());
    turboCheck.setToolTipText("For this machine, and for every one opened from now on");
    reachesTheMachine(turboCheck).addActionListener(e -> {
      config.setTurboByDefault(turboCheck.isSelected());
      config.save();
      emulatorCore.setGeneralOption("turbo", turboCheck.isSelected());
    });

    JLabel processorLabel = new JLabel("Processor:");
    JComboBox<String> processorCombo = new JComboBox<>(emulatorCore.getProcessors().toArray(new String[0]));
    processorCombo.setToolTipText("The implementation the machine runs on: the one generated from the model, or the model itself, which is the one to debug");
    // Chosen before the listener is on, so opening the dialog does not change the processor.
    processorCombo.setSelectedItem(emulatorCore.getProcessor());
    processorCombo.addActionListener(e -> {
      String chosen = (String) processorCombo.getSelectedItem();
      emulatorCore.setProcessor(chosen);
      config.setProcessor(chosen);
      config.save();
      Processors.startsOn = chosen;
    });

    JLabel frameRateLabel = new JLabel("Frame Rate:");
    JSpinner frameRateSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1));
    frameRateSpinner.addChangeListener(e -> emulatorCore.setGeneralOption("frame_rate", frameRateSpinner.getValue()));

    JLabel confirmLabel = new JLabel("Confirm Actions:");
    JCheckBox confirmCheck = new JCheckBox();
    confirmCheck.addActionListener(e -> emulatorCore.setGeneralOption("confirm_actions", confirmCheck.isSelected()));

    JLabel embedLabel = new JLabel("Embed Snapshot:");
    JCheckBox embedCheck = new JCheckBox();
    embedCheck.addActionListener(e -> emulatorCore.setGeneralOption("embed_snapshot", embedCheck.isSelected()));

    JLabel strictAspectLabel = new JLabel("Strict Aspect Ratio:");
    JCheckBox strictAspectCheck = new JCheckBox();
    strictAspectCheck.addActionListener(e -> emulatorCore.setGeneralOption("strict_aspect", strictAspectCheck.isSelected()));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(turboLabel)
            .addComponent(processorLabel)
            .addComponent(frameRateLabel)
            .addComponent(confirmLabel)
            .addComponent(embedLabel)
            .addComponent(strictAspectLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(turboCheck)
            .addComponent(processorCombo)
            .addComponent(frameRateSpinner)
            .addComponent(confirmCheck)
            .addComponent(embedCheck)
            .addComponent(strictAspectCheck))
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(turboLabel)
            .addComponent(turboCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(processorLabel)
            .addComponent(processorCombo))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(frameRateLabel)
            .addComponent(frameRateSpinner))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(confirmLabel)
            .addComponent(confirmCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(embedLabel)
            .addComponent(embedCheck))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(strictAspectLabel)
            .addComponent(strictAspectCheck))
    );

    return panel;
  }
}
