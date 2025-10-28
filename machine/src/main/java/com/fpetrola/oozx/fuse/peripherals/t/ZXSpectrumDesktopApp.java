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

package com.fpetrola.oozx.fuse.peripherals.t;

import com.fpetrola.oozx.fuse.peripherals.EmulatorCore;
import com.fpetrola.oozx.fuse.peripherals.EmulatorListener;
import com.fpetrola.oozx.fuse.peripherals.MockEmulatorCore;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;


// Emulator Internal Frame
class EmulatorInternalFrame extends JInternalFrame {
  private EmulatorCore emulatorCore;
  private JLabel statusLabel;
  private JProgressBar speedBar;
  private JComboBox<String> modelCombo;
  private JLabel pauseIndicator;
  private JLabel turboIndicator;
  private JLabel tapeStatusLabel;

  public EmulatorInternalFrame(EmulatorCore core, int x, int y) {
    super("ZX Spectrum Emulator", true, true, true, true);
    this.emulatorCore = core;
    setSize(800, 600);
    setLocation(x, y);

    // Main Panel (emulator screen)


    JComponent mainPanel = core.getPanel();
    mainPanel.setBackground(Color.BLACK);
    String text = "Emulator Screen - " + core.getCurrentModel();
    JLabel screenLabel = new JLabel("");
    screenLabel.setForeground(Color.WHITE);
    mainPanel.add(screenLabel);
    add(mainPanel, BorderLayout.CENTER);


    JToolBar toolBar = createToolBar();
    add(toolBar, BorderLayout.NORTH);
    // Status bar
    JPanel statusBar = createStatusBar();
    add(statusBar, BorderLayout.SOUTH);

    // Bind data
    emulatorCore.addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {
        statusLabel.setText("State: " + state);
      }

      @Override
      public void onError(String message) {
        JOptionPane.showMessageDialog(EmulatorInternalFrame.this, message, "Error", JOptionPane.ERROR_MESSAGE);
      }

      @Override
      public void onEmulationSpeedChanged(double speed) {
        speedBar.setValue((int) (speed * 1));
        speedBar.setString(String.format("%.2fx", speed));
      }

      @Override
      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
        setTitle("ZX Spectrum Emulator - " + model);
      }

      @Override
      public void onPauseStateChanged(boolean paused) {
        pauseIndicator.setBackground(paused ? Color.RED : Color.GREEN);
        pauseIndicator.setToolTipText(paused ? "Paused" : "Running");
      }

      @Override
      public void onTurboModeChanged(boolean turbo) {
        turboIndicator.setText(turbo ? "Turbo On" : "Turbo Off");
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
    statusLabel = new JLabel("State: Ready");
    statusLabel.setPreferredSize(new Dimension(100, componentHeight));

    // Speed Progress Bar
    speedBar = new JProgressBar(0, 2000); // Max 4x speed
    speedBar.setValue((int) (emulatorCore.getEmulationSpeed()));
    speedBar.setStringPainted(true);
    speedBar.setString(String.format("%.2fx", emulatorCore.getEmulationSpeed()));
    speedBar.setPreferredSize(new Dimension(150, componentHeight));

    // Model Combo
    String[] models = {"Spectrum 16K", "Spectrum 48K", "Spectrum 128K", "Spectrum Plus 2", "Spectrum Plus 3", "Pentagon"};
    modelCombo = new JComboBox<>(models);
    modelCombo.setSelectedItem(emulatorCore.getCurrentModel());
    modelCombo.setPreferredSize(new Dimension(120, componentHeight));
    modelCombo.addActionListener(e -> emulatorCore.setMachineModel((String) modelCombo.getSelectedItem()));

    // Pause Indicator (LED-like)
    pauseIndicator = new JLabel(emulatorCore.isPaused() ? "Paused" : "Running");
    pauseIndicator.setOpaque(true);
    pauseIndicator.setBackground(emulatorCore.isPaused() ? Color.RED : Color.GREEN);
    pauseIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    pauseIndicator.setPreferredSize(new Dimension(componentHeight, componentHeight));
    pauseIndicator.setToolTipText(emulatorCore.isPaused() ? "Paused" : "Running");

    // Turbo Indicator
    turboIndicator = new JLabel(emulatorCore.isTurboMode() ? "✔ Turbo" : "✘ Turbo");
    turboIndicator.setForeground(emulatorCore.isTurboMode() ? Color.BLUE : Color.GRAY);
    turboIndicator.setPreferredSize(new Dimension(80, componentHeight));

    // Tape Status
    tapeStatusLabel = new JLabel();
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
      public void onError(String message) {
      }

      @Override
      public void onEmulationSpeedChanged(double speed) {
        speedBar.setValue((int) (speed));
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

    Icon modelIcon = UIManager.getIcon("Tree.openIcon");
    JButton model48KButton = new JButton(modelIcon);
    model48KButton.setToolTipText("Switch to 48K Model");
    model48KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 48K"));
    toolBar.add(model48KButton);

    JButton model128KButton = new JButton(modelIcon);
    model128KButton.setToolTipText("Switch to 128K Model");
    model128KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 128K"));
    toolBar.add(model128KButton);

    Icon turboIcon = UIManager.getIcon("OptionPane.warningIcon");
    JButton turboButton = new JButton(turboIcon);
    turboButton.setToolTipText("Toggle Turbo Mode");
    turboButton.addActionListener(e -> emulatorCore.setGeneralOption("turbo", !emulatorCore.isTurboMode()));
    toolBar.add(turboButton);

    Icon fullscreenIcon = UIManager.getIcon("Tree.leafIcon");
    JButton fullscreenButton = new JButton(fullscreenIcon);
    fullscreenButton.setToolTipText("Toggle Fullscreen");
//    fullscreenButton.addActionListener(e -> setExtendedState(getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
    toolBar.add(fullscreenButton);

    return toolBar;
  }
}

// Main Desktop Application
public class ZXSpectrumDesktopApp extends JFrame {
  private final Supplier<EmulatorCore> mockCore;
  private JDesktopPane desktop;
  private int emulatorCount = 0;

  public ZXSpectrumDesktopApp(Supplier<EmulatorCore> mockCore) {
    this.mockCore = mockCore;
    setTitle("ZX Spectrum Multi-Emulator");
    setSize(1200, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    desktop = new JDesktopPane();
    add(desktop, BorderLayout.CENTER);

    // Menu Bar
    JMenuBar menuBar = createMenuBar();
    setJMenuBar(menuBar);
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    AbstractAction newEmulatorAction = new AbstractAction("New Emulator") {
      @Override
      public void actionPerformed(ActionEvent e) {
        EmulatorCore emulatorCore = mockCore.get();
        createNewEmulator(emulatorCore);
      }
    };
    fileMenu.add(newEmulatorAction);

    AbstractAction exitAction = new AbstractAction("Exit") {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    };
    fileMenu.add(exitAction);

    menuBar.add(fileMenu);

    JMenu windowMenu = new JMenu("Window");
    windowMenu.setMnemonic(KeyEvent.VK_W);

    AbstractAction cascadeAction = new AbstractAction("Cascade") {
      @Override
      public void actionPerformed(ActionEvent e) {
        cascadeWindows();
      }
    };
    windowMenu.add(cascadeAction);

    AbstractAction tileAction = new AbstractAction("Tile") {
      @Override
      public void actionPerformed(ActionEvent e) {
        tileWindows();
      }
    };
    windowMenu.add(tileAction);

    menuBar.add(windowMenu);

    return menuBar;
  }

  public void createNewEmulator(EmulatorCore core1) {
    EmulatorCore core = core1;
    int x = (emulatorCount * 30) % 400;
    int y = (emulatorCount * 30) % 400;
    EmulatorInternalFrame frame = new EmulatorInternalFrame(core, x, y);
//    frame.addComponentListener(new ComponentAdapter() {
//      public void componentResized(ComponentEvent event) {
//        Rectangle bounds = core1.getPanel().getBounds();
//        Rectangle b = event.getComponent().getBounds();
//        double v = b.getHeight() / bounds.getHeight();
//        double ceil = Math.ceil(v * 10)/10;
//        event.getComponent().setBounds(b.x, b.y, b.width, (int) (b.width * 3f / 4f * ceil));
//      }
//    });
    desktop.add(frame);
    frame.setVisible(true);
    emulatorCount++;
  }

  private void cascadeWindows() {
    JInternalFrame[] frames = desktop.getAllFrames();
    int x = 0;
    int y = 0;
    int width = desktop.getWidth() / 2;
    int height = desktop.getHeight() / 2;

    for (int i = 0; i < frames.length; i++) {
      if (!frames[i].isIcon()) {
        try {
          frames[i].setMaximum(false);
          frames[i].reshape(x, y, width, height);
          x += 30;
          y += 30;
          if (x + width > desktop.getWidth()) x = 0;
          if (y + height > desktop.getHeight()) y = 0;
        } catch (Exception ex) {
        }
      }
    }
  }

  private void tileWindows() {
    JInternalFrame[] frames = desktop.getAllFrames();
    int count = frames.length;
    if (count == 0) return;

    int sqrt = (int) Math.sqrt(count);
    int rows = sqrt;
    int cols = count / rows;
    int extra = count % rows;

    int width = desktop.getWidth() / cols;
    int height = desktop.getHeight() / rows;

    int x = 0;
    int y = 0;
    int w = width;
    int h = height;

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        int index = i * cols + j;
        if (index >= count) break;
        if (!frames[index].isIcon()) {
          try {
            frames[index].setMaximum(false);
            frames[index].reshape(x, y, w, h);
          } catch (Exception ex) {
          }
        }
        x += w;
      }
      x = 0;
      y += h;
      if (i == rows - extra - 1) {
        cols++;
        w = desktop.getWidth() / cols;
      }
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      ZXSpectrumDesktopApp app = new ZXSpectrumDesktopApp(null);
      app.setVisible(true);
      // Create first emulator
      app.createNewEmulator(new MockEmulatorCore(null));
    });
  }
}
