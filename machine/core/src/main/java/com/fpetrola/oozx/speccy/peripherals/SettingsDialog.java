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

package com.fpetrola.oozx.speccy.peripherals;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

// Settings Dialog with tabs
public class SettingsDialog extends JDialog {
  private EmulatorCore emulatorCore;

  public SettingsDialog(Frame owner, EmulatorCore core) {
    super(owner, "Settings", true);
    this.emulatorCore = core;
    setSize(600, 400);

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

    add(tabbedPane, BorderLayout.CENTER);

    // OK/Cancel buttons
    JPanel buttonPanel = new JPanel();
    JButton okButton = new JButton("OK");
    okButton.addActionListener(e -> dispose());
    buttonPanel.add(okButton);

    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> dispose());
    buttonPanel.add(cancelButton);

    add(buttonPanel, BorderLayout.SOUTH);
    
    // Agregar ESC para cerrar el diálogo
    KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "closeDialog");
    getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
      }
    });
  }

  private JPanel createVideoPanel() {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel borderLabel = new JLabel("Show Border:");
    JCheckBox borderCheck = new JCheckBox();
    if (emulatorCore.getPanel() instanceof com.fpetrola.oozx.speccy.SpeccyScreen screen) {
      borderCheck.setSelected(screen.getScreenSettings().isBorder());
    }
    borderCheck.addActionListener(e -> emulatorCore.setVideoOption("border", borderCheck.isSelected()));

    JLabel scanlinesLabel = new JLabel("Scanlines:");
    JCheckBox scanlinesCheck = new JCheckBox();
    scanlinesCheck.addActionListener(e -> emulatorCore.setVideoOption("scanlines", scanlinesCheck.isSelected()));

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
    volumeSlider.addChangeListener(new ChangeListener() {
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
            .addComponent(romLabel)
            .addComponent(lateTimingsLabel)
            .addComponent(contentionLabel)
            .addComponent(highResLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(modelCombo)
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
    JCheckBox turboCheck = new JCheckBox();
    turboCheck.addActionListener(e -> emulatorCore.setGeneralOption("turbo", turboCheck.isSelected()));

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
            .addComponent(frameRateLabel)
            .addComponent(confirmLabel)
            .addComponent(embedLabel)
            .addComponent(strictAspectLabel))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(turboCheck)
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
