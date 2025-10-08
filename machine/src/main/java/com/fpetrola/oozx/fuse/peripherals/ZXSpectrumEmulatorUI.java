/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse.peripherals;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

// Listener for core events to UI
interface EmulatorListener {
    void onEmulationStateChanged(String state);
    void onError(String message);
}

// Main UI class
public class ZXSpectrumEmulatorUI extends JFrame {
    private EmulatorCore emulatorCore;
    private JLabel statusLabel;

    public ZXSpectrumEmulatorUI(EmulatorCore core) {
        this.emulatorCore = core;
        setTitle("ZX Spectrum Emulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Menu Bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Toolbar
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // Main Panel (mock emulator screen)
        JComponent mainPanel = core.getPanel();
        mainPanel.setBackground(Color.BLACK);
        // Simulate screen with a label or canvas
        JLabel screenLabel = new JLabel("Mock Emulator Screen");
        screenLabel.setForeground(Color.WHITE);
        mainPanel.add(screenLabel);
        add(mainPanel, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Status: Ready");
        add(statusLabel, BorderLayout.SOUTH);

        // Add listener to core
        emulatorCore.addEmulatorListener(new EmulatorListener() {
            @Override
            public void onEmulationStateChanged(String state) {
                statusLabel.setText("Status: " + state);
            }

            @Override
            public void onError(String message) {
                JOptionPane.showMessageDialog(ZXSpectrumEmulatorUI.this, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        AbstractAction openAction = new AbstractAction("Open File...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.loadFile(fc.getSelectedFile().getPath());
                }
            }
        };
        fileMenu.add(openAction);

        AbstractAction saveStateAction = new AbstractAction("Save State...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.saveState(fc.getSelectedFile().getPath());
                }
            }
        };
        fileMenu.add(saveStateAction);

        AbstractAction loadStateAction = new AbstractAction("Load State...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.loadState(fc.getSelectedFile().getPath());
                }
            }
        };
        fileMenu.add(loadStateAction);

        fileMenu.addSeparator();

        AbstractAction exitAction = new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        fileMenu.add(exitAction);

        menuBar.add(fileMenu);

        // Machine Menu
        JMenu machineMenu = new JMenu("Machine");
        machineMenu.setMnemonic(KeyEvent.VK_M);

        AbstractAction resetAction = new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                emulatorCore.resetEmulation();
            }
        };
        machineMenu.add(resetAction);

        JMenu modelSubMenu = new JMenu("Select Model");
        ButtonGroup modelGroup = new ButtonGroup();
        JRadioButtonMenuItem model48K = new JRadioButtonMenuItem("48K");
        model48K.addActionListener(e -> emulatorCore.setMachineModel("48K"));
        modelGroup.add(model48K);
        modelSubMenu.add(model48K);

        JRadioButtonMenuItem model128K = new JRadioButtonMenuItem("128K");
        model128K.addActionListener(e -> emulatorCore.setMachineModel("128K"));
        modelGroup.add(model128K);
        modelSubMenu.add(model128K);

        // Add more models like +2, +3, Pentagon, etc.
        JRadioButtonMenuItem modelPlus2 = new JRadioButtonMenuItem("+2");
        modelPlus2.addActionListener(e -> emulatorCore.setMachineModel("+2"));
        modelGroup.add(modelPlus2);
        modelSubMenu.add(modelPlus2);

        JRadioButtonMenuItem modelPlus3 = new JRadioButtonMenuItem("+3");
        modelPlus3.addActionListener(e -> emulatorCore.setMachineModel("+3"));
        modelGroup.add(modelPlus3);
        modelSubMenu.add(modelPlus3);

        JRadioButtonMenuItem modelPentagon = new JRadioButtonMenuItem("Pentagon");
        modelPentagon.addActionListener(e -> emulatorCore.setMachineModel("Pentagon"));
        modelGroup.add(modelPentagon);
        modelSubMenu.add(modelPentagon);

        machineMenu.add(modelSubMenu);

        menuBar.add(machineMenu);

        // Options Menu
        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);

        AbstractAction settingsAction = new AbstractAction("Settings...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog dialog = new SettingsDialog(ZXSpectrumEmulatorUI.this, emulatorCore);
                dialog.setVisible(true);
            }
        };
        optionsMenu.add(settingsAction);

        // Submenus for quick options, e.g., Peripherals
        JMenu peripheralsSubMenu = new JMenu("Peripherals");
        JCheckBoxMenuItem kempstonJoystick = new JCheckBoxMenuItem("Kempston Joystick");
        kempstonJoystick.addActionListener(e -> emulatorCore.setPeripheralOption("kempston_joystick", kempstonJoystick.isSelected()));
        peripheralsSubMenu.add(kempstonJoystick);

        JCheckBoxMenuItem aySound = new JCheckBoxMenuItem("AY Sound");
        aySound.addActionListener(e -> emulatorCore.setAudioOption("ay", aySound.isSelected()));
        peripheralsSubMenu.add(aySound);

        JCheckBoxMenuItem zxPrinter = new JCheckBoxMenuItem("ZX Printer");
        zxPrinter.addActionListener(e -> emulatorCore.setPeripheralOption("zx_printer", zxPrinter.isSelected()));
        peripheralsSubMenu.add(zxPrinter);

        // Add more: Interface 1, Microdrive, etc.
        JCheckBoxMenuItem interface1 = new JCheckBoxMenuItem("Interface 1");
        interface1.addActionListener(e -> emulatorCore.setPeripheralOption("interface1", interface1.isSelected()));
        peripheralsSubMenu.add(interface1);

        optionsMenu.add(peripheralsSubMenu);

        // Media submenu for tape, disk
        JMenu mediaSubMenu = new JMenu("Media");
        AbstractAction insertTape = new AbstractAction("Insert Tape...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Mock insert tape
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.setStorageOption("tape", fc.getSelectedFile().getPath());
                }
            }
        };
        mediaSubMenu.add(insertTape);

        AbstractAction insertDisk = new AbstractAction("Insert Disk...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Mock insert disk
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.setStorageOption("disk", fc.getSelectedFile().getPath());
                }
            }
        };
        mediaSubMenu.add(insertDisk);

        AbstractAction insertMicrodrive = new AbstractAction("Insert Microdrive...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(ZXSpectrumEmulatorUI.this) == JFileChooser.APPROVE_OPTION) {
                    emulatorCore.setStorageOption("microdrive", fc.getSelectedFile().getPath());
                }
            }
        };
        mediaSubMenu.add(insertMicrodrive);

        optionsMenu.add(mediaSubMenu);

        menuBar.add(optionsMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        AbstractAction aboutAction = new AbstractAction("About") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog dialog = new AboutDialog(ZXSpectrumEmulatorUI.this);
                dialog.setVisible(true);
            }
        };
        helpMenu.add(aboutAction);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Use built-in Swing icons
        Icon openIcon = UIManager.getIcon("FileView.fileIcon");
        JButton openButton = new JButton(openIcon);
        openButton.setToolTipText("Open File");
        openButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                emulatorCore.loadFile(fc.getSelectedFile().getPath());
            }
        });
        toolBar.add(openButton);

        Icon saveIcon = UIManager.getIcon("FileChooser.newFolderIcon");
        JButton saveStateButton = new JButton(saveIcon);
        saveStateButton.setToolTipText("Save State");
        saveStateButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                emulatorCore.saveState(fc.getSelectedFile().getPath());
            }
        });
        toolBar.add(saveStateButton);

        Icon loadIcon = UIManager.getIcon("FileChooser.upFolderIcon");
        JButton loadStateButton = new JButton(loadIcon);
        loadStateButton.setToolTipText("Load State");
        loadStateButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                emulatorCore.loadState(fc.getSelectedFile().getPath());
            }
        });
        toolBar.add(loadStateButton);

        toolBar.addSeparator();

        Icon startIcon = UIManager.getIcon("OptionPane.informationIcon");
        JButton startButton = new JButton(startIcon);
        startButton.setToolTipText("Start Emulation");
        startButton.addActionListener(e -> emulatorCore.startEmulation());
        toolBar.add(startButton);

        Icon stopIcon = UIManager.getIcon("OptionPane.errorIcon");
        JButton stopButton = new JButton(stopIcon);
        stopButton.setToolTipText("Stop Emulation");
        stopButton.addActionListener(e -> emulatorCore.stopEmulation());
        toolBar.add(stopButton);

        Icon pauseIcon = UIManager.getIcon("OptionPane.warningIcon");
        JButton pauseButton = new JButton(pauseIcon);
        pauseButton.setToolTipText("Pause Emulation");
        pauseButton.addActionListener(e -> emulatorCore.pauseEmulation());
        toolBar.add(pauseButton);

        Icon resumeIcon = UIManager.getIcon("OptionPane.questionIcon");
        JButton resumeButton = new JButton(resumeIcon);
        resumeButton.setToolTipText("Resume Emulation");
        resumeButton.addActionListener(e -> emulatorCore.resumeEmulation());
        toolBar.add(resumeButton);

        Icon resetIcon = UIManager.getIcon("Tree.closedIcon");
        JButton resetButton = new JButton(resetIcon);
        resetButton.setToolTipText("Reset Emulation");
        resetButton.addActionListener(e -> emulatorCore.resetEmulation());
        toolBar.add(resetButton);

        toolBar.addSeparator();

        // Quick config buttons - using placeholders as no specific icons
        Icon modelIcon = UIManager.getIcon("Tree.openIcon");
        JButton model48KButton = new JButton(modelIcon);
        model48KButton.setToolTipText("Switch to 48K Model");
        model48KButton.addActionListener(e -> emulatorCore.setMachineModel("48K"));
        toolBar.add(model48KButton);

        JButton model128KButton = new JButton(modelIcon);
        model128KButton.setToolTipText("Switch to 128K Model");
        model128KButton.addActionListener(e -> emulatorCore.setMachineModel("128K"));
        toolBar.add(model128KButton);

        // More buttons: e.g., turbo, fullscreen, etc.
        Icon turboIcon = UIManager.getIcon("OptionPane.warningIcon");
        JButton turboButton = new JButton(turboIcon);
        turboButton.setToolTipText("Toggle Turbo Mode");
        turboButton.addActionListener(e -> emulatorCore.setGeneralOption("turbo", true)); // Mock
        toolBar.add(turboButton);

        Icon fullscreenIcon = UIManager.getIcon("Tree.leafIcon");
        JButton fullscreenButton = new JButton(fullscreenIcon);
        fullscreenButton.setToolTipText("Toggle Fullscreen");
        fullscreenButton.addActionListener(e -> setExtendedState(getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
        toolBar.add(fullscreenButton);

        return toolBar;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmulatorCore mockCore = new MockEmulatorCore(new JPanel());
            ZXSpectrumEmulatorUI ui = new ZXSpectrumEmulatorUI(mockCore);
            ui.setVisible(true);
        });
    }
}

// Settings Dialog with tabs
class SettingsDialog extends JDialog {
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
    }

    private JPanel createVideoPanel() {
        JPanel panel = new JPanel();

        // Use GroupLayout
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // Show Border
        JLabel borderLabel = new JLabel("Show Border:");
        JCheckBox borderCheck = new JCheckBox();
        borderCheck.addActionListener(e -> emulatorCore.setVideoOption("border", borderCheck.isSelected()));

        // Scanlines
        JLabel scanlinesLabel = new JLabel("Scanlines:");
        JCheckBox scanlinesCheck = new JCheckBox();
        scanlinesCheck.addActionListener(e -> emulatorCore.setVideoOption("scanlines", scanlinesCheck.isSelected()));

        // Brightness
        JLabel brightnessLabel = new JLabel("Brightness:");
        JSlider brightnessSlider = new JSlider(0, 100, 50);
        brightnessSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!brightnessSlider.getValueIsAdjusting()) {
                    emulatorCore.setVideoOption("brightness", brightnessSlider.getValue());
                }
            }
        });

        // Contrast
        JLabel contrastLabel = new JLabel("Contrast:");
        JSlider contrastSlider = new JSlider(0, 100, 50);
        contrastSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!contrastSlider.getValueIsAdjusting()) {
                    emulatorCore.setVideoOption("contrast", contrastSlider.getValue());
                }
            }
        });

        // ULA Type
        JLabel ulaTypeLabel = new JLabel("ULA Type:");
        String[] ulaTypes = {"Standard", "Timex", "Pentagon"};
        JComboBox<String> ulaTypeCombo = new JComboBox<>(ulaTypes);
        ulaTypeCombo.addActionListener(e -> emulatorCore.setVideoOption("ula_type", ulaTypeCombo.getSelectedItem()));

        // Display Filter
        JLabel filterLabel = new JLabel("Display Filter:");
        String[] filters = {"None", "TV2x", "TV3x", "HQ2x", "HQ3x", "Dot Matrix", "PAL TV"};
        JComboBox<String> filterCombo = new JComboBox<>(filters);
        filterCombo.addActionListener(e -> emulatorCore.setVideoOption("filter", filterCombo.getSelectedItem()));

        // Aspect Ratio
        JLabel aspectLabel = new JLabel("Preserve Aspect Ratio:");
        JCheckBox aspectCheck = new JCheckBox();
        aspectCheck.addActionListener(e -> emulatorCore.setVideoOption("aspect_ratio", aspectCheck.isSelected()));

        // Scaling Method
        JLabel scalingLabel = new JLabel("Scaling Method:");
        String[] scalings = {"Nearest Neighbor", "Bilinear", "Bicubic"};
        JComboBox<String> scalingCombo = new JComboBox<>(scalings);
        scalingCombo.addActionListener(e -> emulatorCore.setVideoOption("scaling", scalingCombo.getSelectedItem()));

        // Snow Effect
        JLabel snowLabel = new JLabel("Snow Effect:");
        JCheckBox snowCheck = new JCheckBox();
        snowCheck.addActionListener(e -> emulatorCore.setVideoOption("snow", snowCheck.isSelected()));

        // Horizontal groups
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(borderLabel)
                .addComponent(scanlinesLabel)
                .addComponent(brightnessLabel)
                .addComponent(contrastLabel)
                .addComponent(ulaTypeLabel)
                .addComponent(filterLabel)
                .addComponent(aspectLabel)
                .addComponent(scalingLabel)
                .addComponent(snowLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
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

        // Vertical groups
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(borderLabel)
                .addComponent(borderCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(scanlinesLabel)
                .addComponent(scanlinesCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(brightnessLabel)
                .addComponent(brightnessSlider))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(contrastLabel)
                .addComponent(contrastSlider))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(ulaTypeLabel)
                .addComponent(ulaTypeCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(filterLabel)
                .addComponent(filterCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(aspectLabel)
                .addComponent(aspectCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(scalingLabel)
                .addComponent(scalingCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // Volume
        JLabel volumeLabel = new JLabel("Master Volume:");
        JSlider volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!volumeSlider.getValueIsAdjusting()) {
                    emulatorCore.setAudioOption("volume", volumeSlider.getValue());
                }
            }
        });

        // AY Chip
        JLabel ayChipLabel = new JLabel("AY Chip Emulation:");
        JCheckBox ayCheck = new JCheckBox();
        ayCheck.addActionListener(e -> emulatorCore.setAudioOption("ay", ayCheck.isSelected()));

        // Beeper Volume
        JLabel beeperVolumeLabel = new JLabel("Beeper Volume:");
        JSlider beeperVolumeSlider = new JSlider(0, 100, 50);
        beeperVolumeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!beeperVolumeSlider.getValueIsAdjusting()) {
                    emulatorCore.setAudioOption("beeper_volume", beeperVolumeSlider.getValue());
                }
            }
        });

        // AY Volume
        JLabel ayVolumeLabel = new JLabel("AY Volume:");
        JSlider ayVolumeSlider = new JSlider(0, 100, 50);
        ayVolumeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!ayVolumeSlider.getValueIsAdjusting()) {
                    emulatorCore.setAudioOption("ay_volume", ayVolumeSlider.getValue());
                }
            }
        });

        // Stereo Separation
        JLabel stereoLabel = new JLabel("AY Stereo Separation:");
        String[] stereos = {"None", "ABC", "ACB", "Mono"};
        JComboBox<String> stereoCombo = new JComboBox<>(stereos);
        stereoCombo.addActionListener(e -> emulatorCore.setAudioOption("stereo_separation", stereoCombo.getSelectedItem()));

        // Sample Rate
        JLabel sampleRateLabel = new JLabel("Sample Rate:");
        String[] rates = {"22050 Hz", "44100 Hz", "48000 Hz"};
        JComboBox<String> sampleRateCombo = new JComboBox<>(rates);
        sampleRateCombo.addActionListener(e -> emulatorCore.setAudioOption("sample_rate", sampleRateCombo.getSelectedItem()));

        // High Quality Beeper
        JLabel hqBeeperLabel = new JLabel("High Quality Beeper:");
        JCheckBox hqBeeperCheck = new JCheckBox();
        hqBeeperCheck.addActionListener(e -> emulatorCore.setAudioOption("hq_beeper", hqBeeperCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(volumeLabel)
                .addComponent(ayChipLabel)
                .addComponent(beeperVolumeLabel)
                .addComponent(ayVolumeLabel)
                .addComponent(stereoLabel)
                .addComponent(sampleRateLabel)
                .addComponent(hqBeeperLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(volumeSlider)
                .addComponent(ayCheck)
                .addComponent(beeperVolumeSlider)
                .addComponent(ayVolumeSlider)
                .addComponent(stereoCombo)
                .addComponent(sampleRateCombo)
                .addComponent(hqBeeperCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(volumeLabel)
                .addComponent(volumeSlider))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(ayChipLabel)
                .addComponent(ayCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(beeperVolumeLabel)
                .addComponent(beeperVolumeSlider))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(ayVolumeLabel)
                .addComponent(ayVolumeSlider))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(stereoLabel)
                .addComponent(stereoCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(sampleRateLabel)
                .addComponent(sampleRateCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // Keyboard Layout
        JLabel keyboardLabel = new JLabel("Keyboard Layout:");
        String[] keyboards = {"QWERTY", "AZERTY", "Spanish", "Russian"};
        JComboBox<String> keyboardCombo = new JComboBox<>(keyboards);
        keyboardCombo.addActionListener(e -> emulatorCore.setInputOption("keyboard_layout", keyboardCombo.getSelectedItem()));

        // Joystick Type
        JLabel joystickLabel = new JLabel("Joystick Type:");
        String[] joysticks = {"None", "Kempston", "Sinclair 1", "Sinclair 2", "Cursor", "Fuller"};
        JComboBox<String> joystickCombo = new JComboBox<>(joysticks);
        joystickCombo.addActionListener(e -> emulatorCore.setInputOption("joystick", joystickCombo.getSelectedItem()));

        // Keyboard Issue
        JLabel issueLabel = new JLabel("Keyboard Issue:");
        ButtonGroup issueGroup = new ButtonGroup();
        JRadioButton issue2 = new JRadioButton("Issue 2");
        issue2.addActionListener(e -> emulatorCore.setInputOption("keyboard_issue", "2"));
        issueGroup.add(issue2);
        JRadioButton issue3 = new JRadioButton("Issue 3");
        issue3.addActionListener(e -> emulatorCore.setInputOption("keyboard_issue", "3"));
        issueGroup.add(issue3);

        // Mouse Emulation
        JLabel mouseLabel = new JLabel("Emulate Mouse:");
        JCheckBox mouseCheck = new JCheckBox();
        mouseCheck.addActionListener(e -> emulatorCore.setInputOption("mouse", mouseCheck.isSelected()));

        // Joystick Prompt
        JLabel joystickPromptLabel = new JLabel("Joystick Keyboard Prompt:");
        JCheckBox joystickPromptCheck = new JCheckBox();
        joystickPromptCheck.addActionListener(e -> emulatorCore.setInputOption("joystick_prompt", joystickPromptCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(keyboardLabel)
                .addComponent(joystickLabel)
                .addComponent(issueLabel)
                .addComponent(mouseLabel)
                .addComponent(joystickPromptLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(keyboardCombo)
                .addComponent(joystickCombo)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(issue2)
                    .addComponent(issue3))
                .addComponent(mouseCheck)
                .addComponent(joystickPromptCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(keyboardLabel)
                .addComponent(keyboardCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(joystickLabel)
                .addComponent(joystickCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(issueLabel)
                .addComponent(issue2)
                .addComponent(issue3))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(mouseLabel)
                .addComponent(mouseCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // Tape Loading Speed
        JLabel tapeSpeedLabel = new JLabel("Tape Loading Speed:");
        String[] tapeSpeeds = {"Normal", "Fast", "Turbo"};
        JComboBox<String> tapeSpeedCombo = new JComboBox<>(tapeSpeeds);
        tapeSpeedCombo.addActionListener(e -> emulatorCore.setStorageOption("tape_speed", tapeSpeedCombo.getSelectedItem()));

        // Disk Interface
        JLabel diskTypeLabel = new JLabel("Disk Interface:");
        String[] diskTypes = {"None", "+3", "Beta 128", "Opus", "TRDOS"};
        JComboBox<String> diskTypeCombo = new JComboBox<>(diskTypes);
        diskTypeCombo.addActionListener(e -> emulatorCore.setStorageOption("disk_interface", diskTypeCombo.getSelectedItem()));

        // Fast Load Tapes
        JLabel fastLoadLabel = new JLabel("Accelerate Tape Loading:");
        JCheckBox fastLoadCheck = new JCheckBox();
        fastLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("fast_load", fastLoadCheck.isSelected()));

        // Auto Load Tapes
        JLabel autoLoadLabel = new JLabel("Auto Load Tapes:");
        JCheckBox autoLoadCheck = new JCheckBox();
        autoLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("auto_load", autoLoadCheck.isSelected()));

        // Trap Tape Loading
        JLabel trapLoadLabel = new JLabel("Trap Tape Loading:");
        JCheckBox trapLoadCheck = new JCheckBox();
        trapLoadCheck.addActionListener(e -> emulatorCore.setStorageOption("trap_load", trapLoadCheck.isSelected()));

        // Microdrive Emulation
        JLabel microdriveLabel = new JLabel("Emulate Microdrives:");
        JCheckBox microdriveCheck = new JCheckBox();
        microdriveCheck.addActionListener(e -> emulatorCore.setStorageOption("microdrive", microdriveCheck.isSelected()));

        // Write Protect Disks
        JLabel writeProtectLabel = new JLabel("Write Protect Disks:");
        JCheckBox writeProtectCheck = new JCheckBox();
        writeProtectCheck.addActionListener(e -> emulatorCore.setStorageOption("write_protect", writeProtectCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(tapeSpeedLabel)
                .addComponent(diskTypeLabel)
                .addComponent(fastLoadLabel)
                .addComponent(autoLoadLabel)
                .addComponent(trapLoadLabel)
                .addComponent(microdriveLabel)
                .addComponent(writeProtectLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(tapeSpeedCombo)
                .addComponent(diskTypeCombo)
                .addComponent(fastLoadCheck)
                .addComponent(autoLoadCheck)
                .addComponent(trapLoadCheck)
                .addComponent(microdriveCheck)
                .addComponent(writeProtectCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(tapeSpeedLabel)
                .addComponent(tapeSpeedCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(diskTypeLabel)
                .addComponent(diskTypeCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(fastLoadLabel)
                .addComponent(fastLoadCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(autoLoadLabel)
                .addComponent(autoLoadCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(trapLoadLabel)
                .addComponent(trapLoadCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(microdriveLabel)
                .addComponent(microdriveCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // Machine Model
        JLabel modelLabel = new JLabel("Machine Model:");
        String[] models = {"16K", "48K", "48K NTSC", "128K", "+2", "+2A", "+3", "TC2048", "TC2068", "TS2068", "Pentagon 128", "Pentagon 512", "Pentagon 1024", "Scorpion ZS 256", "Spectrum SE"};
        JComboBox<String> modelCombo = new JComboBox<>(models);
        modelCombo.addActionListener(e -> emulatorCore.setMachineModel((String) modelCombo.getSelectedItem()));

        // Custom ROM
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

        // Late Timings
        JLabel lateTimingsLabel = new JLabel("Late Timings:");
        JCheckBox lateTimingsCheck = new JCheckBox();
        lateTimingsCheck.addActionListener(e -> emulatorCore.setGeneralOption("late_timings", lateTimingsCheck.isSelected()));

        // Memory Contention
        JLabel contentionLabel = new JLabel("Memory Contention:");
        JCheckBox contentionCheck = new JCheckBox();
        contentionCheck.addActionListener(e -> emulatorCore.setGeneralOption("contention", contentionCheck.isSelected()));

        // High Resolution
        JLabel highResLabel = new JLabel("High Resolution Mode:");
        JCheckBox highResCheck = new JCheckBox();
        highResCheck.addActionListener(e -> emulatorCore.setGeneralOption("high_res", highResCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(modelLabel)
                .addComponent(romLabel)
                .addComponent(lateTimingsLabel)
                .addComponent(contentionLabel)
                .addComponent(highResLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(modelCombo)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(romField)
                    .addComponent(browseButton))
                .addComponent(lateTimingsCheck)
                .addComponent(contentionCheck)
                .addComponent(highResCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(modelLabel)
                .addComponent(modelCombo))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(romLabel)
                .addComponent(romField)
                .addComponent(browseButton))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(lateTimingsLabel)
                .addComponent(lateTimingsCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(contentionLabel)
                .addComponent(contentionCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // ZX Interface 1
        JLabel if1Label = new JLabel("ZX Interface 1:");
        JCheckBox if1Check = new JCheckBox();
        if1Check.addActionListener(e -> emulatorCore.setPeripheralOption("if1", if1Check.isSelected()));

        // ZX Interface 2
        JLabel if2Label = new JLabel("ZX Interface 2:");
        JCheckBox if2Check = new JCheckBox();
        if2Check.addActionListener(e -> emulatorCore.setPeripheralOption("if2", if2Check.isSelected()));

        // ZX Printer
        JLabel printerLabel = new JLabel("ZX Printer:");
        JCheckBox printerCheck = new JCheckBox();
        printerCheck.addActionListener(e -> emulatorCore.setPeripheralOption("printer", printerCheck.isSelected()));

        // Kempston Mouse
        JLabel kempstonMouseLabel = new JLabel("Kempston Mouse:");
        JCheckBox kempstonMouseCheck = new JCheckBox();
        kempstonMouseCheck.addActionListener(e -> emulatorCore.setPeripheralOption("kempston_mouse", kempstonMouseCheck.isSelected()));

        // Fuller Box
        JLabel fullerLabel = new JLabel("Fuller Box:");
        JCheckBox fullerCheck = new JCheckBox();
        fullerCheck.addActionListener(e -> emulatorCore.setPeripheralOption("fuller", fullerCheck.isSelected()));

        // Melodik AY
        JLabel melodikLabel = new JLabel("Melodik AY:");
        JCheckBox melodikCheck = new JCheckBox();
        melodikCheck.addActionListener(e -> emulatorCore.setPeripheralOption("melodik", melodikCheck.isSelected()));

        // SpecDrum
        JLabel specdrumLabel = new JLabel("SpecDrum:");
        JCheckBox specdrumCheck = new JCheckBox();
        specdrumCheck.addActionListener(e -> emulatorCore.setPeripheralOption("specdrum", specdrumCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(if1Label)
                .addComponent(if2Label)
                .addComponent(printerLabel)
                .addComponent(kempstonMouseLabel)
                .addComponent(fullerLabel)
                .addComponent(melodikLabel)
                .addComponent(specdrumLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(if1Check)
                .addComponent(if2Check)
                .addComponent(printerCheck)
                .addComponent(kempstonMouseCheck)
                .addComponent(fullerCheck)
                .addComponent(melodikCheck)
                .addComponent(specdrumCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(if1Label)
                .addComponent(if1Check))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(if2Label)
                .addComponent(if2Check))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(printerLabel)
                .addComponent(printerCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(kempstonMouseLabel)
                .addComponent(kempstonMouseCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(fullerLabel)
                .addComponent(fullerCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(melodikLabel)
                .addComponent(melodikCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
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

        // Turbo Mode
        JLabel turboLabel = new JLabel("Turbo Mode:");
        JCheckBox turboCheck = new JCheckBox();
        turboCheck.addActionListener(e -> emulatorCore.setGeneralOption("turbo", turboCheck.isSelected()));

        // Frame Rate
        JLabel frameRateLabel = new JLabel("Frame Rate:");
        JSpinner frameRateSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1));
        frameRateSpinner.addChangeListener(e -> emulatorCore.setGeneralOption("frame_rate", frameRateSpinner.getValue()));

        // Confirm Actions
        JLabel confirmLabel = new JLabel("Confirm Actions:");
        JCheckBox confirmCheck = new JCheckBox();
        confirmCheck.addActionListener(e -> emulatorCore.setGeneralOption("confirm_actions", confirmCheck.isSelected()));

        // Embed Snapshot
        JLabel embedLabel = new JLabel("Embed Snapshot:");
        JCheckBox embedCheck = new JCheckBox();
        embedCheck.addActionListener(e -> emulatorCore.setGeneralOption("embed_snapshot", embedCheck.isSelected()));

        // Strict Aspect
        JLabel strictAspectLabel = new JLabel("Strict Aspect Ratio:");
        JCheckBox strictAspectCheck = new JCheckBox();
        strictAspectCheck.addActionListener(e -> emulatorCore.setGeneralOption("strict_aspect", strictAspectCheck.isSelected()));

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(turboLabel)
                .addComponent(frameRateLabel)
                .addComponent(confirmLabel)
                .addComponent(embedLabel)
                .addComponent(strictAspectLabel))
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(turboCheck)
                .addComponent(frameRateSpinner)
                .addComponent(confirmCheck)
                .addComponent(embedCheck)
                .addComponent(strictAspectCheck))
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(turboLabel)
                .addComponent(turboCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(frameRateLabel)
                .addComponent(frameRateSpinner))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(confirmLabel)
                .addComponent(confirmCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(embedLabel)
                .addComponent(embedCheck))
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(strictAspectLabel)
                .addComponent(strictAspectCheck))
        );

        return panel;
    }
}

// About Dialog
class AboutDialog extends JDialog {
    public AboutDialog(Frame owner) {
        super(owner, "About", true);
        setSize(300, 200);

        JPanel panel = new JPanel();
        JLabel label = new JLabel("<html><center>ZX Spectrum Emulator<br>Version 1.0<br>Built with Swing<br>By xAI Grok</center></html>");
        panel.add(label);
        add(panel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        add(okButton, BorderLayout.SOUTH);
    }
}