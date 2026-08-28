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
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


// Main UI class
public class ZXSpectrumEmulatorUI extends JFrame {
  private EmulatorCore emulatorCore;
  private JLabel statusLabel;
  private JLabel speedLabel;
  private JLabel modelLabel;
  private JLabel pauseLabel;
  private JLabel turboLabel;
  private JLabel tapeLabel;

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
//    JComponent mainPanel = core.getPanel();
//    mainPanel.setBackground(Color.BLACK);
//    JLabel screenLabel = new JLabel("");
//    screenLabel.setForeground(Color.WHITE);
//    mainPanel.add(screenLabel);
//    add(mainPanel, BorderLayout.CENTER);

    // Status bar
    JPanel statusBar = createStatusBar();
    add(statusBar, BorderLayout.SOUTH);


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
    model48K.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 48K"));
    modelGroup.add(model48K);
    modelSubMenu.add(model48K);

    JRadioButtonMenuItem model128K = new JRadioButtonMenuItem("128K");
    model128K.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 128K"));
    modelGroup.add(model128K);
    modelSubMenu.add(model128K);

    JRadioButtonMenuItem modelPlus2 = new JRadioButtonMenuItem("+2");
    modelPlus2.addActionListener(e -> emulatorCore.setMachineModel("Spectrum Plus 2"));
    modelGroup.add(modelPlus2);
    modelSubMenu.add(modelPlus2);

    JRadioButtonMenuItem modelPlus3 = new JRadioButtonMenuItem("+3");
    modelPlus3.addActionListener(e -> emulatorCore.setMachineModel("Spectrum Plus 3"));
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

    JCheckBoxMenuItem interface1 = new JCheckBoxMenuItem("Interface 1");
    interface1.addActionListener(e -> emulatorCore.setPeripheralOption("interface1", interface1.isSelected()));
    peripheralsSubMenu.add(interface1);

    optionsMenu.add(peripheralsSubMenu);

    // Media submenu for tape, disk
    JMenu mediaSubMenu = new JMenu("Media");
    AbstractAction insertTape = new AbstractAction("Insert Tape...") {
      @Override
      public void actionPerformed(ActionEvent e) {
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

    Icon turboIcon = UIManager.getIcon("OptionPane.errorIcon");
    JButton turboButton = new JButton(turboIcon);
    turboButton.setToolTipText("Toggle Turbo Mode");
    turboButton.addActionListener(e -> emulatorCore.setGeneralOption("turbo", !emulatorCore.isTurboMode()));
    toolBar.add(turboButton);

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

    Icon modelIcon = UIManager.getIcon("Tree.openIcon");
    JButton model48KButton = new JButton(modelIcon);
    model48KButton.setToolTipText("Switch to 48K Model");
    model48KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 48K"));
    toolBar.add(model48KButton);

    JButton model128KButton = new JButton(modelIcon);
    model128KButton.setToolTipText("Switch to 128K Model");
    model128KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 128K"));
    toolBar.add(model128KButton);

    Icon fullscreenIcon = UIManager.getIcon("Tree.leafIcon");
    JButton fullscreenButton = new JButton(fullscreenIcon);
    fullscreenButton.setToolTipText("Toggle Fullscreen");
    fullscreenButton.addActionListener(e -> setExtendedState(getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
    toolBar.add(fullscreenButton);

    return toolBar;
  }

  private JPanel createStatusBar() {
    JPanel statusBar = new JPanel();
    statusBar.setBorder(BorderFactory.createEtchedBorder());
    GroupLayout layout = new GroupLayout(statusBar);
    statusBar.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    // Fixed height for all components
    int componentHeight = 20;

    // State Label
    JLabel statusLabel = new JLabel("State: Ready");
    statusLabel.setPreferredSize(new Dimension(100, componentHeight));

    // Speed Progress Bar
    JProgressBar speedBar = new JProgressBar(0, 2000); // Max 4x speed
    speedBar.setValue((int)(emulatorCore.getEmulationSpeed() ));
    speedBar.setStringPainted(true);
    speedBar.setString(String.format("%.2fx", emulatorCore.getEmulationSpeed()));
    speedBar.setPreferredSize(new Dimension(150, componentHeight));

    // Model Combo
    String[] models = {"Spectrum 16K", "Spectrum 48K", "Spectrum 128K", "Spectrum Plus 2", "Spectrum Plus 3", "Pentagon"};
    JComboBox<String> modelCombo = new JComboBox<>(models);
    modelCombo.setSelectedItem(emulatorCore.getCurrentModel());
    modelCombo.setPreferredSize(new Dimension(120, componentHeight));
    modelCombo.addActionListener(e -> emulatorCore.setMachineModel((String) modelCombo.getSelectedItem()));

    // Pause Indicator (LED-like)
    JLabel pauseIndicator = new JLabel(emulatorCore.isPaused() ? "Paused" : "Running");
    pauseIndicator.setOpaque(true);
    pauseIndicator.setBackground(emulatorCore.isPaused() ? Color.RED : Color.GREEN);
    pauseIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    pauseIndicator.setPreferredSize(new Dimension(componentHeight, componentHeight));
    pauseIndicator.setToolTipText(emulatorCore.isPaused() ? "Paused" : "Running");

    // Turbo Indicator
    JLabel turboIndicator = new JLabel(emulatorCore.isTurboMode() ? "✔xxxxxxxxxxxxxx Turbo" : "✘ eeeeeeeeeeeeeee Turbo");
    turboIndicator.setForeground(emulatorCore.isTurboMode() ? Color.BLUE : Color.GRAY);
    turboIndicator.setPreferredSize(new Dimension(80, componentHeight));

    // Tape Status
    JLabel tapeStatusLabel = new JLabel();
    tapeStatusLabel.setIcon(emulatorCore.getTapeStatus().equals("Loaded") ?
        UIManager.getIcon("OptionPane.informationIcon") :
        UIManager.getIcon("OptionPane.warningIcon"));
    tapeStatusLabel.setPreferredSize(new Dimension(80, componentHeight));
    tapeStatusLabel.setToolTipText("Tape: " + emulatorCore.getTapeStatus());

    // Bind data to components
    emulatorCore.addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {
        statusLabel.setText("State: " + state);
      }

      @Override
      public void onError(String message) {}

      @Override
      public void onEmulationSpeedChanged(double speed) {
        speedBar.setValue((int)(speed));
        speedBar.setString(String.format("%.2fx", speed));
      }

      @Override
      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
      }

      @Override
      public void onPauseStateChanged(boolean paused) {
        pauseIndicator.setBackground(paused ? Color.RED : Color.GREEN);
        pauseIndicator.setText(paused ? "Paused" : "Running");
        pauseIndicator.setToolTipText(paused ? "Paused" : "Running");
      }

      @Override
      public void onTurboModeChanged(boolean turbo) {
        turboIndicator.setText(turbo ? "✔ Turbo" : "✘ Turbo");
        turboIndicator.setForeground(turbo ? Color.BLUE : Color.GRAY);
      }

      @Override
      public void onTapeStatusChanged(String status) {
        tapeStatusLabel.setIcon(status.equals("Loaded") ?
            UIManager.getIcon("OptionPane.informationIcon") :
            UIManager.getIcon("OptionPane.warningIcon"));
        tapeStatusLabel.setToolTipText("Tape: " + status);
      }


    });

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addComponent(statusLabel)
        .addComponent(speedBar)
        .addComponent(modelCombo)
        .addComponent(pauseIndicator)
        .addComponent(turboIndicator)
        .addComponent(tapeStatusLabel)
    );

    layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
        .addComponent(statusLabel)
        .addComponent(speedBar)
        .addComponent(modelCombo)
        .addComponent(pauseIndicator)
        .addComponent(turboIndicator)
        .addComponent(tapeStatusLabel)
    );

    return statusBar;
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      EmulatorCore mockCore = new MockEmulatorCore(null);
      ZXSpectrumEmulatorUI ui = new ZXSpectrumEmulatorUI(mockCore);
      ui.setVisible(true);
    });
  }
}

// About Dialog
class AboutDialog extends JDialog {
  public AboutDialog(Frame owner) {
    super(owner, "About", true);
    setSize(300, 200);

    JPanel panel = new JPanel();
    JLabel label = new JLabel("<html><center>ZX Spectrum Emulator<br>Version 1.0<br>Built with Swing<br></center></html>");
    panel.add(label);
    add(panel, BorderLayout.CENTER);

    JButton okButton = new JButton("OK");
    okButton.addActionListener(e -> dispose());
    add(okButton, BorderLayout.SOUTH);
  }
}