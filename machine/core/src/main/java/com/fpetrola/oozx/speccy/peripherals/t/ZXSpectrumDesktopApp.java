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
import java.util.Map;
import java.util.LinkedHashMap;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.SpeccyScreen;
import com.fpetrola.oozx.speccy.TvScreen;
import com.fpetrola.oozx.api.Hit;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.peripherals.EmulatorCore;
import com.fpetrola.oozx.speccy.peripherals.EmulatorListener;
import com.fpetrola.oozx.speccy.peripherals.SettingsDialog;
import com.fpetrola.oozx.speccy.pokes.PokesManager;
import com.fpetrola.oozx.speccy.pokes.PokesDialog;
import com.fpetrola.emulation.helpers.snapshots.SnapshotSaver;
import com.fpetrola.z80.cpu.RegistersGetter;
import com.fpetrola.z80.cpu.State;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.*;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.fpetrola.oozx.speccy.peripherals.t.EmulatorInternalFrame.loadIcon;

// Emulator Internal Frame
class EmulatorInternalFrame extends JInternalFrame {
  public EmulatorCore emulatorCore;
  private ZXSpectrumDesktopApp parentApp;
  private GameSearchResult gameSearchResult;
  //  private JLabel statusLabel;
  private JProgressBar speedBar;
  private JComboBox<String> modelCombo;
  private JLabel pauseIndicator;
  private JLabel turboIndicator;
  private JButton muteButton;
  private boolean isMuted = false;
  private float scaleFactor0 = 1.73f;
  private float scaleFactor = scaleFactor0;
  //  private JLabel tapeStatusLabel;
  private List<com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod> appliedPokes = new ArrayList<>();


  public EmulatorInternalFrame(EmulatorCore core, int x, int y, ZXSpectrumDesktopApp parentApp) {
    this(core, x, y, parentApp, null);
  }

  public EmulatorInternalFrame(EmulatorCore core, int x, int y, ZXSpectrumDesktopApp parentApp, GameSearchResult gameSearchResult) {
     super("ZX Spectrum Emulator", true, true, true, true);
     this.parentApp = parentApp;
     this.gameSearchResult = gameSearchResult;
     this.emulatorCore = core;
    setSize(420, 380);
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
//        statusLabel.setText("State: " + state);
      }

      @Override
      public void onError(String message) {
        JOptionPane.showMessageDialog(EmulatorInternalFrame.this, message, "Error", JOptionPane.ERROR_MESSAGE);
      }

      @Override
      public void onEmulationSpeedChanged(double speed) {
        speedBar.setValue((int) (speed * 1));
        speedBar.setString(String.format("%.2f%%", speed));
      }

      @Override
      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
        setTitle("ZX Spectrum Emulator - " + model);
      }

      @Override
      public void onPauseStateChanged(boolean paused) {
        pauseIndicator.setBackground(paused ? Color.RED : Color.GREEN);
        pauseIndicator.setForeground(Color.DARK_GRAY);
        pauseIndicator.setToolTipText(paused ? "Paused" : "Running");
      }

      @Override
      public void onTurboModeChanged(boolean turbo) {
        updateTurboLabel(turbo);
      }

      @Override
      public void onTapeStatusChanged(String status) {
//        tapeStatusLabel.setIcon(status.equals("Loaded") ?
//            UIManager.getIcon("OptionPane.informationIcon") :
//            UIManager.getIcon("OptionPane.warningIcon"));
//        tapeStatusLabel.setToolTipText("Tape: " + status);
      }
    });
  }

  private void updateTurboLabel(boolean turbo) {
    turboIndicator.setText(turbo ? "✔ Turbo" : "✘ Turbo");
    turboIndicator.setForeground(turbo ? Color.BLUE : Color.GRAY);
  }

  private void toggleMute() {
    emulatorCore.setGeneralOption("mute", !emulatorCore.isMuted());
    muteButton.setIcon(loadIcon(!emulatorCore.isMuted() ? "1F507.svg" : "1F509.svg"));
    muteButton.setToolTipText(emulatorCore.isMuted() ? "Unmute Sound" : "Mute Sound");
  }

  private JPanel createStatusBar() {
    JPanel statusBar = new JPanel();
//    statusBar.setBorder(BorderFactory.createEtchedBorder());
    statusBar.setPreferredSize(new Dimension(600, 48));
    GroupLayout layout = new GroupLayout(statusBar);
    statusBar.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    // Fixed height for all components
    int componentHeight = 20;

    // State Label
//    statusLabel = new JLabel("State: Ready");
//    statusLabel.setPreferredSize(new Dimension(100, componentHeight));

    // Speed Progress Bar
    speedBar = new JProgressBar(0, 7000); // Max 4x speed
    speedBar.setValue((int) (emulatorCore.getEmulationSpeed()));
    speedBar.setStringPainted(true);
    speedBar.setString(String.format("%.2f%%", emulatorCore.getEmulationSpeed()));
    speedBar.setPreferredSize(new Dimension(150, 24));
    speedBar.setMinimumSize(new Dimension(100, 24));

    // Model Combo
    String[] models = {"Spectrum 16K", "Spectrum 48K", "Spectrum 128K", "Spectrum Plus 2", "Spectrum Plus 3", "Pentagon"};
    modelCombo = new JComboBox<>(models);
    modelCombo.setRenderer(new CustomComboBox());
    modelCombo.setSelectedItem(emulatorCore.getCurrentModel());
    modelCombo.setPreferredSize(new Dimension(80, 10));
    modelCombo.addActionListener(e -> emulatorCore.setMachineModel((String) modelCombo.getSelectedItem()));

    // Pause Indicator (LED-like)
    pauseIndicator = new JLabel(emulatorCore.isPaused() ? "Paused" : "Running");
    pauseIndicator.setHorizontalAlignment(JLabel.CENTER);
    pauseIndicator.setOpaque(true);
    pauseIndicator.setForeground(Color.DARK_GRAY);
    pauseIndicator.setBackground(emulatorCore.isPaused() ? Color.RED : Color.GREEN);
    pauseIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    pauseIndicator.setMinimumSize(new Dimension(70, componentHeight + 4));
    pauseIndicator.setToolTipText(emulatorCore.isPaused() ? "Paused" : "Running");

    // Turbo Indicator
    turboIndicator = new JLabel(emulatorCore.isTurboMode() ? "✔ Turbo" : "✘ Turbo");
    turboIndicator.setForeground(emulatorCore.isTurboMode() ? Color.BLUE : Color.GRAY);
    turboIndicator.setPreferredSize(new Dimension(40, componentHeight));

    // Tape Status
//    tapeStatusLabel = new JLabel();
//    tapeStatusLabel.setIcon(emulatorCore.getTapeStatus().equals("Loaded") ?
//        UIManager.getIcon("FileChooser.upFolderIcon") :
//        UIManager.getIcon("FileChooser.newFolderIcon"));
//    tapeStatusLabel.setPreferredSize(new Dimension(80, componentHeight));
//    tapeStatusLabel.setToolTipText("Tape: " + emulatorCore.getTapeStatus());


    layout.setHorizontalGroup(layout.createSequentialGroup()
//        .addComponent(statusLabel)
            .addComponent(speedBar)
            .addComponent(modelCombo)
            .addComponent(pauseIndicator)
            .addComponent(turboIndicator)
//        .addComponent(tapeStatusLabel)
    );

    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
//        .addComponent(statusLabel)
            .addComponent(speedBar)
            .addComponent(modelCombo)
            .addComponent(pauseIndicator)
            .addComponent(turboIndicator)
//        .addComponent(tapeStatusLabel)
    );

    // Bind data to components
    emulatorCore.addEmulatorListener(new EmulatorListener() {
      public void onEmulationStateChanged(String state) {
//        statusLabel.setText("State: " + state);
      }

      public void onError(String message) {
      }

      public void onEmulationSpeedChanged(double speed) {
        speedBar.setValue((int) (speed));
        speedBar.setString(String.format("%.2f%%", speed));
      }

      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
      }

      public void onPauseStateChanged(boolean paused) {
        pauseIndicator.setBackground(paused ? Color.RED : Color.GREEN);
        pauseIndicator.setText(paused ? "Paused" : "Running");
        pauseIndicator.setToolTipText(paused ? "Paused" : "Running");
      }

      public void onTurboModeChanged(boolean turbo) {
        updateTurboLabel(turbo);
      }

      public void onTapeStatusChanged(String status) {
//        tapeStatusLabel.setIcon(status.equals("Loaded") ?
//            UIManager.getIcon("OptionPane.informationIcon") :
//            UIManager.getIcon("OptionPane.warningIcon"));
//        tapeStatusLabel.setToolTipText("Tape: " + status);
      }
    });

    return statusBar;
  }

  static class CustomComboBox extends JLabel implements ListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      JLabel label = new JLabel() {
        public Dimension getPreferredSize() {
          return new Dimension(200, 15);
        }
      };
      label.setText(String.valueOf(value));

      return label;
    }
  }

  /**
   * Trims the padding a button keeps around its icon. Shrinking the icon alone leaves the
   * button the size it was, since most of a toolbar button is margin.
   */
  /**
   * A button showing a drawing, falling back to its word if the drawing will not load — the
   * loader throws on a missing file, so without this a mistyped name takes the whole window
   * down instead of one picture.
   */
  static JButton iconButton(String iconFile, String text, String tooltip) {
    JButton button = new JButton();
    try {
      button.setIcon(loadIcon(iconFile));
    } catch (RuntimeException missing) {
      button.setText(text);
    }
    if (tooltip != null) button.setToolTipText(tooltip);
    return button;
  }

  /**
   * The same, for a button that stays down: a toggle is not a JButton, so {@link #iconButton}
   * cannot make one and the border button was the last unguarded drawing in this toolbar.
   */
  static JToggleButton iconToggle(String iconFile, String text, String tooltip) {
    JToggleButton button = new JToggleButton();
    try {
      button.setIcon(loadIcon(iconFile));
    } catch (RuntimeException missing) {
      button.setText(text);
    }
    if (tooltip != null) button.setToolTipText(tooltip);
    return button;
  }

  static void tighten(Container toolBar) {
    for (Component component : toolBar.getComponents()) {
      if (component instanceof AbstractButton button) {
        button.setMargin(new Insets(2, 3, 2, 3));
        button.setFocusPainted(false);
      }
    }
  }

  private JToolBar createToolBar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);

    //    Icon turboIcon = UIManager.getIcon("FileChooser.upFolderIcon");
    JButton turboButton = new JButton(loadIcon("1F680.svg"));
    turboButton.setToolTipText("Toggle Turbo Mode");
    turboButton.addActionListener(e -> emulatorCore.setGeneralOption("turbo", !emulatorCore.isTurboMode()));
    toolBar.add(turboButton);

    toolBar.addSeparator();

//    // Use built-in Swing icons
//    Icon openIcon = UIManager.getIcon("FileView.fileIcon");
//    JButton openButton = new JButton(openIcon);
//    openButton.setPreferredSize(new Dimension(40, 40));
//    openButton.setToolTipText("Open File");
//    openButton.addActionListener(e -> {
//      JFileChooser fc = new JFileChooser();
//      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//        emulatorCore.loadFile(fc.getSelectedFile().getPath());
//      }
//    });
//    toolBar.add(openButton);
//
//    Icon saveIcon = UIManager.getIcon("FileChooser.newFolderIcon");
//    JButton saveStateButton = new JButton(saveIcon);
//    saveStateButton.setPreferredSize(new Dimension(40, 40));
//    saveStateButton.setToolTipText("Save State");
//    saveStateButton.addActionListener(e -> {
//      JFileChooser fc = new JFileChooser();
//      if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
//        emulatorCore.saveState(fc.getSelectedFile().getPath());
//      }
//    });
//    toolBar.add(saveStateButton);
//
//    Icon loadIcon = UIManager.getIcon("FileChooser.upFolderIcon");
//    JButton loadStateButton = new JButton(loadIcon);
//    loadStateButton.setToolTipText("Load State");
//    loadStateButton.addActionListener(e -> {
//      JFileChooser fc = new JFileChooser();
//      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//        emulatorCore.loadState(fc.getSelectedFile().getPath());
//      }
//    });
//    toolBar.add(loadStateButton);
//
//    toolBar.addSeparator();
//
//    Icon startIcon = UIManager.getIcon("FileChooser.upFolderIcon");
//    JButton startButton = new JButton(startIcon);
//    startButton.setToolTipText("Start Emulation");
//    startButton.addActionListener(e -> emulatorCore.startEmulation());
//    toolBar.add(startButton);
//
//    Icon stopIcon = UIManager.getIcon("FileChooser.upFolderIcon");
//    JButton stopButton = new JButton(stopIcon);
//    stopButton.setToolTipText("Stop Emulation");
//    stopButton.addActionListener(e -> emulatorCore.stopEmulation());
//    toolBar.add(stopButton);

    // A toggle rather than a button: it stays down while the border is showing, the way the
    // border either is there or is not. Up to start with - see SpeccyScreen.
    JToggleButton borderButton = iconToggle("border-stripes.svg", "Border", "Show the Border");
    // The border is a screen knob like the rest, so the button shows what this emulator has and
    // writing it goes to the same place the window writes.
    if (emulatorCore.getPanel() instanceof SpeccyScreen screen) {
      borderButton.setSelected(screen.getScreenSettings().isBorder());
    }
    borderButton.addActionListener(e -> {
      emulatorCore.setGeneralOption("border", borderButton.isSelected());
      if (parentApp != null) {
        parentApp.setScreenDefault("border", String.valueOf(borderButton.isSelected()));
      }
    });
    toolBar.add(borderButton);

    toolBar.addSeparator();

    JButton pauseButton = new JButton(loadIcon("23EF.svg"));
    pauseButton.setToolTipText("Pause Emulation");
    pauseButton.addActionListener(e -> emulatorCore.pauseEmulation());
    toolBar.add(pauseButton);

    toolBar.addSeparator();

    muteButton = new JButton(loadIcon("1F507.svg"));
    muteButton.setToolTipText("Mute/Unmute Sound");
    muteButton.addActionListener(e -> toggleMute());
    toolBar.add(muteButton);

    toolBar.addSeparator();

    if (parentApp != null) {
      JButton pokesButton = new JButton(loadIcon("1F513.svg"));
      pokesButton.setToolTipText("Cheats/Pokes");
      pokesButton.addActionListener(e -> openPokesDialog());
      toolBar.add(pokesButton);
      
      JButton viewDetailsButton = new JButton(loadIcon("E259.svg"));
      viewDetailsButton.setToolTipText("View Game Details");
      viewDetailsButton.addActionListener(e -> openGameDetails());
      toolBar.add(viewDetailsButton);
    }

//    Icon resumeIcon = UIManager.getIcon("FileChooser.upFolderIcon");
//    JButton resumeButton = new JButton(resumeIcon);
//    resumeButton.setToolTipText("Resume Emulation");
//    resumeButton.addActionListener(e -> emulatorCore.resumeEmulation());
//    toolBar.add(resumeButton);
//
//    Icon resetIcon = UIManager.getIcon("Tree.closedIcon");
//    JButton resetButton = new JButton(resetIcon);
//    resetButton.setToolTipText("Reset Emulation");
//    resetButton.addActionListener(e -> emulatorCore.resetEmulation());
//    toolBar.add(resetButton);
//
//    toolBar.addSeparator();
//
//    Icon modelIcon = UIManager.getIcon("Tree.openIcon");
//    JButton model48KButton = new JButton(modelIcon);
//    model48KButton.setToolTipText("Switch to 48K Model");
//    model48KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 48K"));
//    toolBar.add(model48KButton);
//
//    JButton model128KButton = new JButton(modelIcon);
//    model128KButton.setToolTipText("Switch to 128K Model");
//    model128KButton.addActionListener(e -> emulatorCore.setMachineModel("Spectrum 128K"));
//    toolBar.add(model128KButton);

    JButton fullscreenButton = new JButton(loadIcon("1F4FA.svg"));
    fullscreenButton.setToolTipText("Toggle Fullscreen");
//    fullscreenButton.addActionListener(e -> setExtendedState(getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
    toolBar.add(fullscreenButton);

    JButton changeSize = new JButton(loadIcon("E243.svg"));
    changeSize.setToolTipText("Change size");
    changeSize.addActionListener(e -> {
      Dimension size = EmulatorInternalFrame.this.getSize();
      if (size.width > 1000)
        scaleFactor = 1 / scaleFactor0;
      if (size.width < 600)
        scaleFactor = scaleFactor0;
      EmulatorInternalFrame.this.setSize((int) (size.width * scaleFactor), (int) (size.height * scaleFactor));
    });
    toolBar.add(changeSize);

    JButton screenButton = new JButton(loadIcon("1F39B.svg"));
    screenButton.setToolTipText("Screen - scaling, television and colour");
    screenButton.addActionListener(e -> {
      if (parentApp != null) parentApp.openScreenSettings(emulatorCore, getTitle());
    });
    toolBar.add(screenButton);

    JButton favoriteButton = new JButton(loadIcon("2B50.svg"));
    favoriteButton.setToolTipText("Keep this game in Favorites");
    favoriteButton.addActionListener(e -> {
      if (parentApp != null) parentApp.keepAsFavorite(gameSearchResult, emulatorCore.getFilename());
    });
    toolBar.add(favoriteButton);

    tighten(toolBar);
    return toolBar;
  }

  /** Every toolbar in the application draws its icons at this size. */
  public static final int TOOLBAR_ICON_SIZE = 19;

  public static ImageIcon loadIcon(String iconFile) {
    int size = TOOLBAR_ICON_SIZE;
    ImageIcon turboIcon = SvgIconLoader.loadSvgAsImageIcon("/icons/" + iconFile, size, size);
    return turboIcon;
  }

  private void openPokesDialog() {
    if (parentApp == null) return;

    String gameName = emulatorCore.getFilename();
    if (gameName != null) {
      gameName = new java.io.File(gameName).getName().replace(".tap", "").replace(".tzx", "")
          .replace(".z80", "").replace(".sna", "").replace(".szx", "");
    }

    if (gameName == null || gameName.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No game loaded", "Info", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    List<com.fpetrola.oozx.speccy.pokes.PokFile> availablePokes =
        parentApp.pokesManager.findPokesForGame(gameName);

    if (availablePokes.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "No pokes found for: " + gameName,
          "Pokes Not Found",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    PokesDialog pokesDialog = new PokesDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        gameName,
        availablePokes,
        parentApp.pokesManager,
        new ArrayList<>(appliedPokes)); // Pasar pokes previamente aplicados en el constructor

    pokesDialog.setOnPokesAppliedListener(selectedMods -> {
      if (!selectedMods.isEmpty()) {
        applyPokes(selectedMods);
      }
    });

    // Listener para revertir pokes removidos
    pokesDialog.setOnPokesChangedListener(removedMods -> {
      if (!removedMods.isEmpty()) {
        revertPokes(removedMods);
      }
    });

    pokesDialog.setVisible(true);
  }
  
  private void openGameDetails() {
    String gameId = null;
    String gameName = null;
    
    // Try to get game ID from gameSearchResult first
    if (gameSearchResult != null) {
      gameId = gameSearchResult.id;
    } else {
      // If no gameSearchResult, try to extract game name from filename
      String filename = emulatorCore.getFilename();
      if (filename != null && !filename.isEmpty()) {
        gameName = new java.io.File(filename).getName()
            .replace(".tap", "").replace(".tzx", "")
            .replace(".z80", "").replace(".sna", "").replace(".szx", "")
            .replace(".dsk", "").replace(".vg", "");
      }
    }
    
    // If we don't have either gameId or gameName, show error
    if (gameId == null && (gameName == null || gameName.isEmpty())) {
      JOptionPane.showMessageDialog(this, 
          "No game information available", 
          "Info", 
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    
    // Show loading dialog while fetching from API
    JDialog loadingDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
        "Loading Game Details", true);
    loadingDialog.setSize(300, 100);
    loadingDialog.setLocationRelativeTo(this);
    JLabel loadingLabel = new JLabel("Fetching game details from ZXInfo API...");
    loadingLabel.setHorizontalAlignment(JLabel.CENTER);
    loadingDialog.add(loadingLabel);
    
    final String finalGameId = gameId;
    final String finalGameName = gameName;
    
    // Fetch details in background thread
    SwingWorker<com.fpetrola.oozx.api.GameDetail, Void> worker =
        new SwingWorker<com.fpetrola.oozx.api.GameDetail, Void>() {
          @Override
          protected com.fpetrola.oozx.api.GameDetail doInBackground() throws Exception {
            try {
              ZxInfoApiHandler apiHandler =
                  new ZxInfoApiHandler();
              
              // If we have gameId, use it directly
              if (finalGameId != null) {
                return apiHandler.fetchGameDetails(finalGameId);
              } else {
                // Otherwise search by game name and use the first result
                List<Hit> results = apiHandler.search(finalGameName);
                if (results == null || results.isEmpty()) {
                  return null;
                }
                Hit bestMatch = results.get(0);
                return apiHandler.fetchGameDetails(bestMatch._id);
              }
            } catch (Exception e) {
              System.err.println("Error fetching game details: " + e.getMessage());
              e.printStackTrace();
              return null;
            }
          }
          
          @Override
          protected void done() {
            loadingDialog.dispose();
            try {
              com.fpetrola.oozx.api.GameDetail detail = get();
              if (detail != null) {
                GameDetailsDialog dialog = new GameDetailsDialog(
                    (Frame) SwingUtilities.getWindowAncestor(EmulatorInternalFrame.this), 
                    detail);
                dialog.setVisible(true);
              } else {
                String searchTerm = finalGameId != null ? "game ID" : ("\"" + finalGameName + "\"");
                JOptionPane.showMessageDialog(EmulatorInternalFrame.this,
                    "No game details found for " + searchTerm,
                    "Game Not Found", JOptionPane.INFORMATION_MESSAGE);
              }
            } catch (Exception e) {
              JOptionPane.showMessageDialog(EmulatorInternalFrame.this,
                  "Error loading game details: " + e.getMessage(),
                  "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        };
    worker.execute();
    loadingDialog.setVisible(true);
  }

  private void applyPokes(List<com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod> mods) {
    System.out.println("Aplicando " + mods.size() + " pokes:");

    // Identificar pokes nuevos que no estaban aplicados
    List<com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod> newMods = new ArrayList<>();
    for (com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod mod : mods) {
      boolean wasAlreadyApplied = appliedPokes.stream()
          .anyMatch(p -> p.getName().equals(mod.getName()) &&
                         p.getRawInstruction().equals(mod.getRawInstruction()));
      if (!wasAlreadyApplied) {
        newMods.add(mod);
      }
    }

    // Actualizar la lista de pokes aplicados
    appliedPokes.clear();
    appliedPokes.addAll(mods);

    // Solo aplicar los nuevos pokes
    for (com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod mod : newMods) {
      System.out.println("  - " + mod.getName() + ": " + mod.getDescription());
      System.out.println("    Type: " + mod.getInstructionType() + ", Raw: " + mod.getRawInstruction());
      emulatorCore.applyMod(mod);
    }

    if (newMods.isEmpty()) {
      System.out.println("  (No nuevos pokes para aplicar)");
    }
  }

  private void revertPokes(List<com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod> mods) {
    System.out.println("Revertiendo " + mods.size() + " pokes:");
    for (com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod mod : mods) {
      System.out.println("  - " + mod.getName() + ": " + mod.getDescription());
      // Remover del registro
      appliedPokes.removeIf(p -> p.getName().equals(mod.getName()) &&
                                 p.getRawInstruction().equals(mod.getRawInstruction()));
      // Revertir el poke en el emulador (el valor previo está guardado en PokInstruction)
      emulatorCore.revertMod(mod);
    }
  }

  public OOZxConfiguration.WindowState saveWindowState(String filePath) {
    OOZxConfiguration.WindowState state = new OOZxConfiguration.WindowState(
        "EMULATOR", getX(), getY(), getWidth(), getHeight());
    state.setFilePath(filePath);
    state.setZOrder(ZXSpectrumDesktopApp.getComponentZOrder(this));

    // Guardar el nombre legible del archivo/snapshot
    if (filePath != null && !filePath.isEmpty()) {
      state.setSnapshotName(new java.io.File(filePath).getName());
    }

    state.setTurboMode(emulatorCore.isTurboMode());
    state.setMuted(emulatorCore.isMuted());
    state.setPaused(emulatorCore.isPaused());

    // Guardar los pokes aplicados con información completa y valores de reversión
    List<OOZxConfiguration.PokModState> pokModStates = new ArrayList<>();
    for (com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod mod : appliedPokes) {
      com.fpetrola.oozx.speccy.pokes.PokInstruction instruction = mod.getParsedInstruction();
      OOZxConfiguration.PokModState pokState = new OOZxConfiguration.PokModState(
          mod.getName(),
          mod.getRawInstruction(),
          mod.getPokFileName(),
          mod.getGameName(),
          mod.getInstructionType(),
          mod.getDescription(),
          instruction.getPreviousValue(),
          instruction.getPreviousBank(),
          instruction.getPreviousAddress()
      );
      pokModStates.add(pokState);
    }
    state.setAppliedPokes(pokModStates);

    // Guardar el estado actual del emulador en formato Unicode empaquetado y obtener su ID
    try {
      String unicodePackedSnapshot = SnapshotSaver.getSnapshotAsUnicodePacked(
          emulatorCore.getRegistersGetter(),
          emulatorCore.getState()
      );
      // Guardar el snapshot en el mapa centralizado y obtener su ID
      String snapshotId = ((ZXSpectrumDesktopApp) SwingUtilities.getWindowAncestor(this)).config.saveSnapshot(unicodePackedSnapshot);
      state.setSnapshotId(snapshotId);
    } catch (Exception e) {
      System.err.println("Error guardando snapshot en configuración: " + e.getMessage());
    }

    return state;
  }

  public void restoreWindowState(OOZxConfiguration.WindowState state) {
    if (state.getWidth() > 0 && state.getHeight() > 0) {
      setSize(state.getWidth(), state.getHeight());
    }
    if (state.getX() >= 0 && state.getY() >= 0) {
      setLocation(state.getX(), state.getY());
    }

    // Restaurar el mute
    isMuted = state.isMuted();
    if (isMuted) {
      emulatorCore.setGeneralOption("mute", true);
      muteButton.setIcon(loadIcon("1F509.svg"));
      muteButton.setToolTipText("Unmute Sound");
    } else {
      emulatorCore.setGeneralOption("mute", false);
      muteButton.setIcon(loadIcon("1F507.svg"));
      muteButton.setToolTipText("Mute Sound");
    }

    emulatorCore.setGeneralOption("turbo", state.isTurboMode());
    emulatorCore.setGeneralOption("pause", state.isPaused());

    emulatorCore.setFilename(state.getFilePath());
    // Restaurar el estado del emulador desde el snapshot empaquetado
    if (state.getSnapshotId() != null && !state.getSnapshotId().isEmpty()) {
      try {
        ZXSpectrumDesktopApp parentApp = (ZXSpectrumDesktopApp) SwingUtilities.getWindowAncestor(this);
        String snapshotData = parentApp.config.getSnapshot(state.getSnapshotId());
        if (snapshotData != null && !snapshotData.isEmpty()) {
          SnapshotSaver.loadSnapshotFromUnicodePacked(snapshotData);
        }
      } catch (Exception e) {
        System.err.println("Error restaurando snapshot desde configuración: " + e.getMessage());
      }
    }

    // Restaurar los pokes aplicados
    if (state.getAppliedPokes() != null && !state.getAppliedPokes().isEmpty()) {
      appliedPokes.clear();
      List<com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod> mods = new ArrayList<>();
      for (OOZxConfiguration.PokModState pokState : state.getAppliedPokes()) {
        com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod mod = new com.fpetrola.oozx.speccy.pokes.PokFile.PokeMod(
            pokState.getName(),
            pokState.getRawInstruction(),
            pokState.getPokFileName(),
            pokState.getGameName()
        );
        // Restaurar los valores de reversión en la instrucción parseada
        com.fpetrola.oozx.speccy.pokes.PokInstruction instruction = mod.getParsedInstruction();
        if (instruction != null) {
          instruction.setPreviousValue(pokState.getPreviousValue());
          instruction.setPreviousBank(pokState.getPreviousBank());
          instruction.setPreviousAddress(pokState.getPreviousAddress());
        }
        mods.add(mod);
        appliedPokes.add(mod);
      }
      // Aplicar los pokes restaurados
      if (!mods.isEmpty()) {
        applyPokes(mods);
      }
    }
  }
}

// --- NEW: Game Search Result Model ---
class GameSearchResult {
  public String id;
  String title;
  String url;
  String screenshot1;
  String screenshot2;
  String filename;
  /** What the entry offers that cannot be loaded, for saying so instead of "no tape". */
  String offers;
  /** Extras the entry carries, for filters the server cannot apply itself. */
  boolean hasRzx;
  boolean hasMap;
  /** Recordings of this game offered for playing, from both catalogues. */
  java.util.List<RzxOption> recordings = java.util.List.of();

  public GameSearchResult(String _id, String title, String url, String screenshot1, String screenshot2, String filename) {
    id = _id;
    this.title = title;
    this.url = url;
    this.screenshot1 = screenshot1;
    this.screenshot2 = screenshot2;
    this.filename = filename;
  }
}

// --- NEW: Game Browser Listener Interface ---
interface GameBrowserListener {
  /** @param whenDone run on the event thread once the game is up, or the attempt failed. */
  void onGameSelected(GameSearchResult gameUrl, Runnable whenDone);

  void onViewDetails(GameSearchResult gameSearchResult);

  void onAddToFavorites(GameSearchResult game);

  void onDownloadGame(String gameUrl);

  /** Fetches a recording of the game and plays it. */
  void onPlayRecording(RzxOption recording);
}

// --- UPDATED: ZXSpectrumDesktopApp with Game Browser ---
public class ZXSpectrumDesktopApp extends JFrame {
  private final Function<String, EmulatorCore> mockCore;
  private final Function<SpectrumState, EmulatorCore> mockCoreState;
  private JDesktopPane desktop;
  private int emulatorCount = 0;
  private GameBrowserInternalFrame gameBrowser;
  private SnapshotHistoryInternalFrame snapshotHistory;
  private FavoritesInternalFrame favorites;
  private final JFileChooser fileChooser = new JFileChooser();
  protected OOZxConfiguration config;
  private JMenu recentFilesMenu;
  protected PokesManager pokesManager;

  {
    // Configuración única del file chooser
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
        "ZX Spectrum files (*.tap, *.tzx, *.z80, *.sna, *.szx)",
        "tap", "tzx", "z80", "sna", "szx");
    fileChooser.setFileFilter(filter);
    fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
  }

  private void openFile() {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      config.addRecentFile(path);
      updateRecentFilesMenu();
//      EmulatorInternalFrame target = getActiveEmulatorOrCreateNew();
//      if (target != null) {
      loadInNewEmulator(path);
//        target.emulatorCore.loadFile(path);
//      }
    }
  }

  private void saveState() {
    EmulatorInternalFrame active = getActiveEmulator();
    if (active == null) {
      JOptionPane.showMessageDialog(this, "No hay emulador activo para guardar estado.", "Save State", JOptionPane.WARNING_MESSAGE);
      return;
    }
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastSaveStateDirectory()));
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastSaveStateDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      active.emulatorCore.saveState(path);
    }
  }

  private void loadState() {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastLoadStateDirectory()));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastLoadStateDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      EmulatorInternalFrame target = getActiveEmulatorOrCreateNew();
      if (target != null) {
        target.emulatorCore.loadState(path);
      }
    }
  }

  // Devuelve el emulador que está seleccionado o visible, o null
  private EmulatorInternalFrame getActiveEmulator() {
    JInternalFrame selected = desktop.getSelectedFrame();
    if (selected instanceof EmulatorInternalFrame) {
      return (EmulatorInternalFrame) selected;
    }
    // Si no hay seleccionado, devuelve el primero visible
    for (JInternalFrame f : desktop.getAllFrames()) {
      if (f instanceof EmulatorInternalFrame && f.isVisible()) {
        return (EmulatorInternalFrame) f;
      }
    }
    return null;
  }

  // Devuelve el emulador activo o crea uno nuevo si no existe ninguno
  private EmulatorInternalFrame getActiveEmulatorOrCreateNew() {
    EmulatorInternalFrame frame = getActiveEmulator();
    if (frame == null) {
      EmulatorCore core = mockCore.apply("");
      frame = createNewEmulator(core);
    }
    try {
      frame.setSelected(true);
    } catch (Exception ignored) {
    }
    return frame;
  }

  public ZXSpectrumDesktopApp(Function<String, EmulatorCore> mockCore, Function<SpectrumState, EmulatorCore> mockCoreState1) {
    this.mockCore = mockCore;
    this.mockCoreState = mockCoreState1;
    this.config = OOZxConfiguration.load();
    applySavedLookAndFeel();
    // Emulators apply the defaults themselves as they are built, so putting the saved ones in
    // place here is all it takes for the next window to open configured.
    if (config.getScreenDefaults() != null && !config.getScreenDefaults().isEmpty()) {
      ScreenSettings.setDefaults(config.getScreenDefaults());
    }
    restoreKeptProfiles();
    this.pokesManager = new PokesManager();

    setTitle("ZX Spectrum Multi-Emulator");
    setSize(1200, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    desktop = new JDesktopPane();
    add(desktop, BorderLayout.CENTER);

    // Menu Bar
    JMenuBar menuBar = createMenuBar();
    setJMenuBar(menuBar);

    // Toolbar
    JToolBar toolBar = createMainToolBar();
    add(toolBar, BorderLayout.NORTH);

    // Restaurar estado de la ventana principal
    restoreMainWindowState();

    // Restaurar ventanas abiertas
    restoreOpenWindows();

    config.getSnapshots().clear();

    // Guardar configuración al cerrar la aplicación
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        saveMainWindowState();
        saveOpenWindows();
        config.save();
      }
    });
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    // ====================== MENU FILE ======================
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    // ---- Open (cargar tape/snapshot) ----
    JMenuItem openItem = new JMenuItem("Open...");
    openItem.setMnemonic(KeyEvent.VK_O);
    openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    openItem.addActionListener(e -> openFile());
    fileMenu.add(openItem);

    fileMenu.addSeparator();

    // ---- Recent Files ----
    recentFilesMenu = new JMenu("Recent Files");
    recentFilesMenu.setMnemonic(KeyEvent.VK_R);
    updateRecentFilesMenu();
    fileMenu.add(recentFilesMenu);

    fileMenu.addSeparator();

    // ---- Save State ----
    JMenuItem saveStateItem = new JMenuItem("Save State...");
    saveStateItem.setMnemonic(KeyEvent.VK_S);
    saveStateItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    saveStateItem.addActionListener(e -> saveState());
    fileMenu.add(saveStateItem);

    // ---- Load State ----
    JMenuItem loadStateItem = new JMenuItem("Load State...");
    loadStateItem.setMnemonic(KeyEvent.VK_L);
    loadStateItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
    loadStateItem.addActionListener(e -> loadState());
    fileMenu.add(loadStateItem);

    fileMenu.addSeparator();

    // ---- Quit ----
    JMenuItem quitItem = new JMenuItem("Quit");
    quitItem.setMnemonic(KeyEvent.VK_Q);
    quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
    quitItem.addActionListener(e -> System.exit(0));
    fileMenu.add(quitItem);

    menuBar.add(fileMenu);

    // ====================== MENU EMULATOR ======================
    JMenu emulatorMenu = new JMenu("Emulator");
    emulatorMenu.setMnemonic(KeyEvent.VK_E);

    AbstractAction newEmulatorAction = new AbstractAction("New Emulator") {
      @Override
      public void actionPerformed(ActionEvent e) {
        EmulatorCore emulatorCore = mockCore.apply("");
        createNewEmulator(emulatorCore);
      }
    };
    newEmulatorAction.putValue(AbstractAction.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
    emulatorMenu.add(newEmulatorAction);

    JMenuItem openTapeItem = new JMenuItem("Open Tape...");
    openTapeItem.addActionListener(e -> chooseTapeForBrowser());
    emulatorMenu.add(openTapeItem);

    JMenuItem rzxPlayerItem = new JMenuItem("RZX Player...");
    rzxPlayerItem.addActionListener(e -> showRzxPlayer());
    emulatorMenu.add(rzxPlayerItem);

    JMenuItem tapeBrowserItem = new JMenuItem("Cassette Browser");
    tapeBrowserItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    tapeBrowserItem.addActionListener(e -> showTapeBrowser());
    emulatorMenu.add(tapeBrowserItem);

    JMenuItem gameBrowserMenuItem = new JMenuItem("Game Browser...");
    gameBrowserMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
    gameBrowserMenuItem.addActionListener(e -> openGameBrowser());
    emulatorMenu.add(gameBrowserMenuItem);

    emulatorMenu.addSeparator();

    // ---- Pause/Resume ----
    JMenuItem pauseResumeItem = new JMenuItem("Pause/Resume");
    pauseResumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
    pauseResumeItem.addActionListener(e -> {
      EmulatorInternalFrame active = getActiveEmulator();
      if (active != null) {
        active.emulatorCore.pauseEmulation();
      }
    });
    emulatorMenu.add(pauseResumeItem);

    // ---- Turbo Mode ----
    JMenuItem turboModeItem = new JMenuItem("Toggle Turbo Mode");
    turboModeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    turboModeItem.addActionListener(e -> {
      EmulatorInternalFrame active = getActiveEmulator();
      if (active != null) {
        active.emulatorCore.setGeneralOption("turbo", !active.emulatorCore.isTurboMode());
      }
    });
    emulatorMenu.add(turboModeItem);

    // ---- Mute/Unmute ----
    JMenuItem muteItem = new JMenuItem("Toggle Mute");
    muteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
    muteItem.addActionListener(e -> {
      EmulatorInternalFrame active = getActiveEmulator();
      if (active != null) {
        active.emulatorCore.setGeneralOption("mute", !active.emulatorCore.isMuted());
      }
    });
    emulatorMenu.add(muteItem);

    menuBar.add(emulatorMenu);

    // ====================== MENU OPTIONS ======================
    JMenu optionsMenu = new JMenu("Options");
    optionsMenu.setMnemonic(KeyEvent.VK_O);

    AbstractAction settingsAction = new AbstractAction("Settings...") {
      public void actionPerformed(ActionEvent e) {
        openSettings();
      }
    };
    optionsMenu.add(settingsAction);
    optionsMenu.addSeparator();
    optionsMenu.add(createTvMenu());
    menuBar.add(optionsMenu);

    // Window menu (includes Look&Feel submenu)
    addWindowMenu(menuBar);

    // ====================== MENU HELP ======================
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);

    JMenuItem readmeItem = new JMenuItem("View README");
    readmeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    readmeItem.addActionListener(e -> openReadme());
    helpMenu.add(readmeItem);

    helpMenu.addSeparator();

    JMenuItem aboutItem = new JMenuItem("About");
    aboutItem.addActionListener(e -> showAboutDialog());
    helpMenu.add(aboutItem);

    menuBar.add(helpMenu);

    return menuBar;
  }

  private void openSettings() {
    SettingsDialog settingsDialog = new SettingsDialog(ZXSpectrumDesktopApp.this, null);
    settingsDialog.setLocationRelativeTo(ZXSpectrumDesktopApp.this);
    settingsDialog.setVisible(true);
  }

  private void openReadme() {
    SwingUtilities.invokeLater(() -> {
      try {
        // Leer el README desde resources
        String readmeContent = loadReadmeFromResources();

        if (readmeContent != null && !readmeContent.isEmpty()) {
          // Convertir markdown a HTML
          String html = markdownToHtml(readmeContent);

          // Mostrar en una ventana interna
          showReadmeWindow(html);
        } else {
          JOptionPane.showMessageDialog(this,
              "Could not load README",
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Error loading README:n" + e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
      }
    });
  }

  private String loadReadmeFromResources() throws Exception {
    try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("README.md")) {
      if (in == null) {
        throw new Exception("README.md not found in resources");
      }
      return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  private String markdownToHtml(String markdown) {
    org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder()
        .extensions(List.of(
            org.commonmark.ext.gfm.tables.TablesExtension.create()
        ))
        .build();
    org.commonmark.node.Node document = parser.parse(markdown);
    org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
        .extensions(List.of(
            org.commonmark.ext.gfm.tables.TablesExtension.create()
        ))
        .build();
    String html = renderer.render(document);

    // Agregar estilos CSS
    String styledHtml = "<html><head><style>" +
                        "body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; color: #333; }" +
                        "h1 { color: #1f77b4; border-bottom: 2px solid #1f77b4; padding-bottom: 10px; }" +
                        "h2 { color: #ff7f0e; margin-top: 20px; }" +
                        "h3 { color: #2ca02c; }" +
                        "code { background-color: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New'; }" +
                        "pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; }" +
                        "pre code { background-color: transparent; padding: 0; }" +
                        "a { color: #1f77b4; text-decoration: none; }" +
                        "a:hover { text-decoration: underline; }" +
                        "blockquote { border-left: 4px solid #ddd; padding-left: 15px; color: #666; margin-left: 0; }" +
                        "img { max-width: 100%; height: auto; }" +
                        "table { border-collapse: collapse; width: 100%; }" +
                        "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
                        "th { background-color: #f5f5f5; }" +
                        "</style></head><body>" +
                        html +
                        "</body></html>";

    return styledHtml;
  }

  private void showReadmeWindow(String html) {
    JInternalFrame readmeFrame = new JInternalFrame("README - OOZX", true, true, true, true);
    readmeFrame.setSize(800, 600);
    readmeFrame.setLocation(50, 50);

    // Crear un JEditorPane para renderizar HTML
    JEditorPane editorPane = new JEditorPane();
    editorPane.setContentType("text/html");
    editorPane.setText(html);
    editorPane.setEditable(false);
    editorPane.setCaretPosition(0);

    // Agregar scroll
    JScrollPane scrollPane = new JScrollPane(editorPane);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    readmeFrame.add(scrollPane, BorderLayout.CENTER);
    desktop.add(readmeFrame);
    readmeFrame.setVisible(true);

    try {
      readmeFrame.setSelected(true);
    } catch (java.beans.PropertyVetoException e) {
      // Ignore
    }
  }

  private void showAboutDialog() {
    String appName = "OOZX";
    String version = "0.0.1";
    String versionSuffix = "SNAPSHOT";
    String shortDescription = "Modern ZX Spectrum Emulator";
    String fullDescription = "Object-Oriented emulator with modular, pluggable architecture";
    String author = "Fernando Damian Petrola";
    String copyright = "Copyright (C) 2023-2025";
    String license = "Apache License 2.0";
    String website = "github.com/fpetrola/oozx";

    String about = String.format("""
            <html>
                  <head>
                    <title></title>
                  </head>
                  <body style='font-family: Segoe UI, Arial, sans-serif; width: 420px; color: #333;'>
                    <div style='text-align: center;'>
                      <h1 style='margin: 0; padding: 0; font-size: 28px; color: #1f77b4;'>%s</h1>
                      <p style='margin: 2px 0; font-size: 10px; color: #999;'>v%s <span style='color: #aaa;'>%s</span></p>
                    </div>
                    <p style='text-align: center; margin: 8px 0; font-size: 12px; color: #555;'><b>%s</b></p>
                    <p style='text-align: center; margin: 4px 0; font-size: 11px; color: #777;'>%s</p>
                    <hr style='border: none; border-top: 1px solid #ddd; margin: 12px 0;'>
                    <table style='width: 100%%; font-size: 11px; line-height: 1.8;'>
                      <tr>
                        <td style='color: #666;'><b>Author:</b></td>
                        <td style='text-align: right; color: #333;'>%s</td>
                      </tr>
                      <tr>
                        <td style='color: #666;'><b>License:</b></td>
                        <td style='text-align: right; color: #333;'>%s</td>
                      </tr>
                      <tr>
                        <td style='color: #666;'><b>Repository:</b></td>
                        <td style='text-align: right;'><span style='color: #0066cc;'>%s</span></td>
                      </tr>
                    </table>
                    <hr style='border: none; border-top: 1px solid #ddd; margin: 12px 0;'>
                    <div style='padding: 8px; border-radius: 4px; border-left: 3px solid #1f77b4;'>
                      <p style='font-size: 10px; color: #666; margin: 0; line-height: 1.5;'>A high-performance, multi-model Z80 emulator built with pure object-oriented design. Features pluggable peripherals, simultaneous multi-game emulation, online game discovery, and automatic state preservation between sessions.</p>
                    </div>
                    <p style='text-align: center; margin-top: 12px; font-size: 9px; color: #999;'>%s</p>
                  </body>
                  </html>
            """,
        appName, version, versionSuffix, shortDescription, fullDescription, author, license, website, copyright
    );

    JOptionPane.showMessageDialog(this,
        new JLabel(about),
        "About " + appName,
        JOptionPane.INFORMATION_MESSAGE);
  }

  private void addLaf(JMenu menu, final Theme theme) {
    AbstractAction themeAction = new AbstractAction(theme.getName()) {
      public void actionPerformed(ActionEvent e) {
        LafManager.install(theme);
        rememberLookAndFeel(theme.getName());
      }
    };
    menu.add(themeAction);
  }

  /** The themes the menu offers, so a saved name can be turned back into one on the next run. */
  private static final List<Theme> THEMES = List.of(new DarculaTheme(), new OneDarkTheme(),
      new SolarizedLightTheme(), new SolarizedDarkTheme(), new IntelliJTheme());

  private static final String METAL = "Metal";

  private void rememberLookAndFeel(String name) {
    config.setLookAndFeel(name);
    config.save();
  }

  /**
   * Puts back the theme chosen last time. The launcher installs one before there is any
   * configuration to read, so this runs afterwards and replaces it; picking a theme is a
   * decision worth surviving the window closing.
   */
  private void applySavedLookAndFeel() {
    String saved = config.getLookAndFeel();
    if (saved == null) return;

    try {
      if (METAL.equals(saved)) {
        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        LafManager.updateLaf();
        return;
      }
      THEMES.stream().filter(t -> t.getName().equals(saved)).findFirst()
          .ifPresent(LafManager::install);
    } catch (Exception e) {
      // A theme that no longer exists is not a reason to refuse to start.
      System.err.println("could not restore the look and feel '" + saved + "': " + e);
    }
  }

  /**
   * Which lead the picture is imagined to arrive through, and the two things that go with it.
   * <p>
   * The modes come from the enum rather than being listed here, so adding one to the engine puts
   * it in the menu without anybody remembering to.
   */
  /** Looks saved in an earlier run, put back where the windows look for them. */
  private void restoreKeptProfiles() {
    List<ScreenProfile> kept = new java.util.ArrayList<>();
    config.getKeptScreenProfiles().forEach((name, values) ->
        kept.add(new ScreenProfile(name, values, false)));
    ScreenSettings.setKeptProfiles(kept);
  }

  /** Writes back whatever the engine now holds, after one was saved or forgotten. */
  void rememberKeptProfiles() {
    Map<String, Map<String, String>> kept = new LinkedHashMap<>();
    for (ScreenProfile profile : ScreenSettings.getKeptProfiles()) {
      kept.put(profile.name(), new LinkedHashMap<>(profile.values()));
    }
    config.setKeptScreenProfiles(kept);
    config.save();
  }

  /** The screen knobs of one emulator, in a window of their own. */
  void openScreenSettings(EmulatorCore core, String machineName) {
    if (!(core.getPanel() instanceof SpeccyScreen screen)) {
      JOptionPane.showMessageDialog(this, "This emulator has no adjustable screen.",
          "Screen", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    ScreenSettingsInternalFrame window = new ScreenSettingsInternalFrame(machineName,
        screen.getScreenSettings(), kept -> {
      config.setScreenDefaults(new LinkedHashMap<>(kept));
      config.save();
    }, this::rememberKeptProfiles);
    desktop.add(window);
    window.setVisible(true);
    window.toFront();
    try {
      window.setSelected(true);
    } catch (java.beans.PropertyVetoException ignored) {
    }
  }

  private JMenu createTvMenu() {
    JMenu tvMenu = new JMenu("TV");
    ButtonGroup leads = new ButtonGroup();

    String current = screenDefault("tv", TvScreen.RGB_MONITOR.label());
    for (TvScreen lead : TvScreen.values()) {
      boolean chosen = lead.label().equals(current) || lead.name().equals(current);
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(lead.label(), chosen);
      item.addActionListener(e -> setScreenDefault("tv", lead.label()));
      leads.add(item);
      tvMenu.add(item);
    }

    tvMenu.addSeparator();

    // Scan lines are a depth in the window, not a switch, so the shortcut ticks to a visible
    // amount and unticks to none rather than pretending there are only two states.
    JCheckBoxMenuItem scanLines = new JCheckBoxMenuItem("Scan lines",
        Double.parseDouble(screenDefault("scanlines", "0")) > 0);
    scanLines.addActionListener(e ->
        setScreenDefault("scanlines", scanLines.isSelected() ? "0.35" : "0"));
    tvMenu.add(scanLines);

    return tvMenu;
  }

  /**
   * Through the core, the same way the border button asks for its option, rather than reaching
   * into the panel to find the screen: the screen is somewhere under a component tree that is
   * not ours to depend on, and a search that stops finding it would fail silently.
   */
  /**
   * Changes one screen knob everywhere: in what new emulators start with, in the file, and in
   * the windows already open.
   * <p>
   * There used to be two ways to remember the same thing — this menu had its own fields in the
   * configuration, older than the knobs — and the one that ran last won. A new emulator applied
   * the saved defaults in its constructor and then had them overwritten by whatever the menu
   * happened to hold, which for scan lines nobody had touched was off. One place now.
   */
  void setScreenDefault(String key, String value) {
    Map<String, String> defaults = new LinkedHashMap<>(config.getScreenDefaults());
    defaults.put(key, value);
    config.setScreenDefaults(defaults);
    config.save();
    ScreenSettings.setDefaults(defaults);
    applyScreenSettingsToAll();
  }

  private String screenDefault(String key, String fallback) {
    return config.getScreenDefaults().getOrDefault(key, fallback);
  }

  private void applyScreenSettingsToAll() {
    for (JInternalFrame frame : desktop.getAllFrames()) {
      if (frame instanceof EmulatorInternalFrame emulator
          && emulator.emulatorCore != null
          && emulator.emulatorCore.getPanel() instanceof SpeccyScreen screen) {
        screen.getScreenSettings().apply(config.getScreenDefaults());
      }
    }
  }

  private void addWindowMenu(JMenuBar menuBar) {
    JMenu windowMenu = new JMenu("Window");
    windowMenu.setMnemonic(KeyEvent.VK_W);

    // ---- Close Active Window ----
    AbstractAction closeWindowAction = new AbstractAction("Close Active Window") {
      @Override
      public void actionPerformed(ActionEvent e) {
        closeActiveWindow();
      }
    };
    closeWindowAction.putValue(AbstractAction.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
    windowMenu.add(closeWindowAction);

    // ---- Close All Windows ----
    AbstractAction closeAllWindowsAction = new AbstractAction("Close All Windows") {
      @Override
      public void actionPerformed(ActionEvent e) {
        closeAllWindows();
      }
    };
    closeAllWindowsAction.putValue(AbstractAction.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
    windowMenu.add(closeAllWindowsAction);

    windowMenu.addSeparator();

    AbstractAction cascadeAction = new AbstractAction("Cascade") {
      @Override
      public void actionPerformed(ActionEvent e) {
        cascadeWindows();
      }
    };
    cascadeAction.putValue(AbstractAction.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK));
    windowMenu.add(cascadeAction);

    AbstractAction tileAction = new AbstractAction("Tile") {
      @Override
      public void actionPerformed(ActionEvent e) {
        tileWindows();
      }
    };
    tileAction.putValue(AbstractAction.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK));
    windowMenu.add(tileAction);

    windowMenu.addSeparator();

    // ---- Look&Feel Submenu ----
    JMenu lookAndFeelMenu = new JMenu("Look&Feel");
    lookAndFeelMenu.setMnemonic(KeyEvent.VK_L);

    addLaf(lookAndFeelMenu, new DarculaTheme());
    addLaf(lookAndFeelMenu, new OneDarkTheme());
    addLaf(lookAndFeelMenu, new SolarizedLightTheme());
    addLaf(lookAndFeelMenu, new SolarizedDarkTheme());
    addLaf(lookAndFeelMenu, new IntelliJTheme());

    AbstractAction metalAction = new AbstractAction("Metal") {
      public void actionPerformed(ActionEvent e) {
        try {
          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
          LafManager.updateLaf();
          rememberLookAndFeel(METAL);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
    lookAndFeelMenu.add(metalAction);

    windowMenu.add(lookAndFeelMenu);

    menuBar.add(windowMenu);
  }

  private void closeActiveWindow() {
    JInternalFrame frame = desktop.getSelectedFrame();
    if (frame != null) {
      try {
        frame.setClosed(true);
      } catch (java.beans.PropertyVetoException e) {
        // Window refused to close
      }
    }
  }

  private void closeAllWindows() {
    JInternalFrame[] frames = desktop.getAllFrames();
    for (JInternalFrame frame : frames) {
      try {
        frame.setClosed(true);
      } catch (java.beans.PropertyVetoException e) {
        // Window refused to close
      }
    }
  }

  private JToolBar createMainToolBar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);

    JButton newEmulatorBtn = new JButton(loadIcon("Sinclair_ZX_Spectrum-02b.svg"));
    newEmulatorBtn.setToolTipText("New Emulator");
    newEmulatorBtn.addActionListener(e -> {
      EmulatorCore core = mockCore.apply("");
      createNewEmulator(core);
    });
    toolBar.add(newEmulatorBtn);

    JButton gameBrowserBtn = new JButton(loadIcon("1F579.svg"));
    gameBrowserBtn.setToolTipText("Open Game Browser");
    gameBrowserBtn.addActionListener(e -> openGameBrowser());
    toolBar.add(gameBrowserBtn);

    JButton historyBtn = new JButton(loadIcon("E260.svg"));
    historyBtn.setToolTipText("Snapshot History");
    historyBtn.addActionListener(e -> openSnapshotHistory());
    toolBar.add(historyBtn);

    JButton favoritesBtn = new JButton(loadIcon("2B50.svg"));
    favoritesBtn.setToolTipText("Favorites");
    favoritesBtn.addActionListener(e -> openFavorites());
    toolBar.add(favoritesBtn);

    JButton settingsBtn = new JButton(loadIcon("2699.svg"));
    settingsBtn.setToolTipText("Settings");
    settingsBtn.addActionListener(e -> openSettings());
    toolBar.add(settingsBtn);

    EmulatorInternalFrame.tighten(toolBar);
    return toolBar;
  }

  private void openGameBrowser() {
    if (gameBrowser == null || gameBrowser.isClosed()) {
      gameBrowser = new GameBrowserInternalFrame(createGameBrowserListener());
      desktop.add(gameBrowser);
      gameBrowser.setVisible(true);
      try {
        gameBrowser.setSelected(true);
      } catch (java.beans.PropertyVetoException ex) {
      }
    } else {
      try {
        gameBrowser.setSelected(true);
        gameBrowser.toFront();
      } catch (java.beans.PropertyVetoException ex) {
      }
    }
  }

  /**
   * Keeps a game to come back to. What is stored is the thing the launcher can open — the
   * downloadable file or the path already loaded — and not the game's page, which would make an
   * entry that can be read and not played.
   */
  void keepAsFavorite(GameSearchResult game, String loadedPath) {
    String source = game != null && game.filename != null ? game.filename : loadedPath;
    // A machine started from a recording has neither: it was not opened from a search and it
    // has no file of its own, so the thing worth keeping is the recording driving it.
    RzxPlayerInternalFrame driving = playerDriving(getActiveEmulator());
    if (source == null && driving != null && driving.getSourceUrl() != null) {
      keepRecordingAsFavorite(driving);
      return;
    }
    if (source == null) {
      JOptionPane.showMessageDialog(this,
          (game != null ? game.title : "This game") + " has nothing to download, so there is "
              + "nothing to come back to.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    String title = game != null && game.title != null ? game.title
        : new java.io.File(source).getName();
    boolean added = config.addFavorite(new OOZxConfiguration.Favorite(source, title, "GAME",
        game == null ? null : game.id));
    if (favorites != null && !favorites.isClosed()) favorites.refresh();

    JOptionPane.showMessageDialog(this, added ? title + " is now a favourite."
        : title + " was already a favourite.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Keeps the recording a player has open. What is stored is where it came from plus which file
   * inside the archive it turned out to be, because a URL alone comes back to a zip and not to
   * the recording that was actually being watched.
   */
  void keepRecordingAsFavorite(RzxPlayerInternalFrame player) {
    String source = player.getSourceUrl();
    if (source == null) {
      JOptionPane.showMessageDialog(this, "There is no recording open to keep.",
          "Favorites", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    String title = player.getRecordingName() == null ? source : player.getRecordingName();
    boolean added = config.addFavorite(new OOZxConfiguration.Favorite(source, title, "RECORDING",
        null, player.getSourceEntry()));
    if (favorites != null && !favorites.isClosed()) favorites.refresh();

    JOptionPane.showMessageDialog(this, added ? title + " is now a favourite."
        : title + " was already a favourite.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
  }

  public void openFavorites() {
    if (favorites == null || favorites.isClosed()) {
      favorites = new FavoritesInternalFrame(config, favorite -> {
        // Straight back through the paths the application already uses, so a favourite opens
        // exactly the way it opened the first time.
        if (favorite.isRecording()) {
          // Fetching wants something it can open as a URL, and a favourite made from a file on
          // the disk kept a plain path because that is what is worth reading in the list.
          String from = favorite.getSource();
          if (!from.startsWith("http") && !from.startsWith("file:")) {
            from = new java.io.File(from).toURI().toString();
          }
          playRecording(new RzxOption(favorite.getTitle(), from), favorite.getEntry());
        } else {
          loadInNewEmulator(favorite.getSource());
        }
      });
      desktop.add(favorites);
    }
    favorites.setVisible(true);
    favorites.toFront();
    try {
      favorites.setSelected(true);
    } catch (java.beans.PropertyVetoException ignored) {
    }
  }

  private void openSnapshotHistory() {
    if (snapshotHistory == null || snapshotHistory.isClosed()) {
      snapshotHistory = new SnapshotHistoryInternalFrame(config);

      // Listener para cargar un snapshot del historial
      snapshotHistory.setOnSnapshotSelectedListener(entry -> {
        // Si hay estado guardado, usarlo; si no, cargar desde el archivo
        if (entry.getInitialStateId() != null && !entry.getInitialStateId().isEmpty()) {
          try {
            String snapshotData = config.getSnapshot(entry.getInitialStateId());
            if (snapshotData != null && !snapshotData.isEmpty()) {
              SpectrumState spectrumState = SnapshotSaver.loadSnapshotFromUnicodePacked(snapshotData);
              EmulatorCore core = mockCoreState.apply(spectrumState);
              createNewEmulator(core, entry.getFilePath());
              return;
            }
          } catch (Exception ex) {
            System.err.println("Error restaurando snapshot guardado: " + ex.getMessage());
            // Fallback a cargar desde archivo
          }
        }

        // Fallback: cargar desde el archivo
        EmulatorCore core = mockCore.apply(entry.getFilePath());
        createNewEmulator(core, entry.getFilePath());
      });

      // Listener para remover del historial
      snapshotHistory.setOnSnapshotRemovedListener(entry -> {
        if (entry == null) {
          // Limpiar el historial completo
          config.getSnapshotHistory().clear();
        } else {
          // Remover un snapshot específico
          String key = new java.io.File(entry.getFilePath()).getAbsolutePath();
          config.getSnapshotHistory().remove(key);
        }
        // Guardar (se limpian automáticamente snapshots huérfanos)
        config.save();
      });

      // Listener para ver detalles del juego
      snapshotHistory.setOnViewDetailsListener(gameName -> {
        showGameDetailsFromHistory(gameName);
      });

      // Callback para refrescar la lista cuando cambia el historial
      config.setOnHistoryChanged(() -> {
        if (snapshotHistory != null && !snapshotHistory.isClosed()) {
          snapshotHistory.refreshHistory(config);
        }
      });

      // Callback para guardar estado cuando se cierre la ventana
      snapshotHistory.setOnClosedListener(() -> {
        config.save();
      });

      desktop.add(snapshotHistory);
      snapshotHistory.setVisible(true);
      try {
        snapshotHistory.setSelected(true);
      } catch (java.beans.PropertyVetoException ex) {
      }
    } else {
      try {
        snapshotHistory.setSelected(true);
        snapshotHistory.toFront();
      } catch (java.beans.PropertyVetoException ex) {
      }
    }
  }

  /**
   * Search for game by name and show details dialog
   */
  private void showGameDetailsFromHistory(String gameName) {
    // Remove file extensions from game name
    String cleanGameName = gameName
        .replaceAll("\\.(z80|sna|tap|tzx|szx)$", "")
        .trim();

    // Show loading dialog
    JDialog loadingDialog = new JDialog(this, "Loading Game Details", true);
    loadingDialog.setSize(300, 100);
    loadingDialog.setLocationRelativeTo(this);
    JLabel loadingLabel = new JLabel("Searching for: " + cleanGameName);
    loadingLabel.setHorizontalAlignment(JLabel.CENTER);
    loadingDialog.add(loadingLabel);

    // Search in background
    SwingWorker<com.fpetrola.oozx.api.GameDetail, Void> worker =
        new SwingWorker<com.fpetrola.oozx.api.GameDetail, Void>() {
          @Override
          protected com.fpetrola.oozx.api.GameDetail doInBackground() throws Exception {
            try {
              // Search for the game by name (without file extension)
              ZxInfoApiHandler apiHandler = new ZxInfoApiHandler();
              List<Hit> results = apiHandler.search(cleanGameName);

              if (results == null || results.isEmpty()) {
                return null;
              }

              // Get the best matching result (first one)
              Hit bestMatch = results.get(0);
              String gameId = bestMatch._id;

              // Fetch full details from API
              return apiHandler.fetchGameDetails(gameId);
            } catch (Exception e) {
              System.err.println("Error searching for game: " + e.getMessage());
              return null;
            }
          }

          @Override
          protected void done() {
            loadingDialog.dispose();
            try {
              com.fpetrola.oozx.api.GameDetail gameDetail = get();

              if (gameDetail == null) {
                // Show dialog to allow user to search again
                showGameNotFoundDialog(cleanGameName);
                return;
              }

              GameDetailsDialog dialog = new GameDetailsDialog(ZXSpectrumDesktopApp.this, gameDetail);
              dialog.setVisible(true);
            } catch (Exception e) {
              JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
                  "Error loading game details: " + e.getMessage(),
                  "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        };

    worker.execute();
    loadingDialog.setVisible(true);
  }

  /**
   * Show dialog when game not found, allowing user to search with different name
   */
  private void showGameNotFoundDialog(String attemptedName) {
    GameNotFoundDialog.showWithRetry(this, "Game not found: " + attemptedName,
        newName -> showGameDetailsFromHistory(newName));
  }

  private void getActiveEmulatorOrCreateNew(GameSearchResult gameSearchResult, Runnable whenDone) {
  //    JInternalFrame[] frames = desktop.getAllFrames();
  //    for (JInternalFrame frame : frames) {
  //      if (frame instanceof EmulatorInternalFrame && frame.isVisible()) {
  //        try {
  //          frame.setSelected(true);
  //          return (EmulatorInternalFrame) frame;
  //        } catch (java.beans.PropertyVetoException ex) {
  //        }
  //      }
  //    }
    // mockCore.apply downloads and unzips the game and builds the machine, so it cannot run on
    // the event thread: doing that froze the whole window until the emulator was ready.
    new SwingWorker<EmulatorCore, Void>() {
      @Override
      protected EmulatorCore doInBackground() {
        return mockCore.apply(gameSearchResult.filename);
      }

      @Override
      protected void done() {
        try {
          createNewEmulator(get(), gameSearchResult);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Could not load \"" + gameSearchResult.title + "\".\n\n" + reason(e),
              "Load failed", JOptionPane.ERROR_MESSAGE);
        } finally {
          whenDone.run();
        }
      }
    }.execute();
  }

  // ... (rest of the methods: createNewEmulator, cascadeWindows, tileWindows remain unchanged)

  /**
   * The deck belonging to each machine. Weak so a closed emulator can be collected; the browser
   * looks a tape up from whichever emulator is in front rather than being tied to one.
   */
  private final java.util.Map<EmulatorCore, com.fpetrola.oozx.speccy.modules.tape.Tape> tapesByCore =
      new java.util.WeakHashMap<>();

  public void registerTape(EmulatorCore core, com.fpetrola.oozx.speccy.modules.tape.Tape tape) {
    tapesByCore.put(core, tape);
  }

  /** The deck of the emulator in front, or null when there is no emulator. */
  public com.fpetrola.oozx.speccy.modules.tape.Tape getActiveTape() {
    EmulatorInternalFrame active = getActiveEmulator();
    return active == null ? null : tapesByCore.get(active.emulatorCore);
  }

  private TapeBrowserInternalFrame tapeBrowser;

  /**
   * Every player open at once. There used to be one, kept in a field and reused, so a second
   * recording took the first one's window and its machine: watching two was watching the later
   * one. A recording already builds its own machine - {@link RzxSession#open} makes its own
   * injector and its own Speccy - so nothing below the window ever required there to be one.
   */
  private final java.util.List<RzxPlayerInternalFrame> rzxPlayers = new java.util.ArrayList<>();

  /**
   * A player ready to take a recording: the open one if it is still empty, otherwise another.
   * <p>
   * Reusing an empty one keeps the menu item from leaving a trail of blank players behind
   * someone who clicked it twice, and opening a second recording while the first is playing
   * gets its own window, which is the point.
   */
  public RzxPlayerInternalFrame showRzxPlayer() {
    rzxPlayers.removeIf(JInternalFrame::isClosed);
    for (RzxPlayerInternalFrame open : rzxPlayers) {
      if (!open.hasRecording()) {
        open.setVisible(true);
        open.toFront();
        return open;
      }
    }
    return newRzxPlayer();
  }

  /** Another player, whatever the open ones are doing. */
  public RzxPlayerInternalFrame newRzxPlayer() {
    rzxPlayers.removeIf(JInternalFrame::isClosed);
    RzxPlayerInternalFrame player =
        new RzxPlayerInternalFrame(nextPlayerNumber(), this::chooseRecording, this::showRzxMachine);
    player.setOnFavorite(() -> keepRecordingAsFavorite(player));
    // Cascaded like the emulators, so the second one does not land exactly on the first.
    player.setLocation(120 + (rzxPlayers.size() * 30) % 300, 60 + (rzxPlayers.size() * 30) % 200);
    player.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        rzxPlayers.remove(player);
        // Closing the controls closes the picture they were driving. Leaving the machine behind
        // would leave a window nobody can stop, still running and still making sound.
        JInternalFrame machine = player.getMachineWindow();
        if (machine != null && !machine.isClosed()) {
          machine.dispose();
        }
      }

      @Override
      public void internalFrameActivated(InternalFrameEvent e) {
        raisePartner(player.getMachineWindow(), player);
      }
    });
    rzxPlayers.add(player);
    desktop.add(player);
    player.setVisible(true);
    player.toFront();
    return player;
  }

  /**
   * The smallest number not in use, rather than one more than the last: with four open, closing
   * #2 and opening another should give back #2 and not #5, or the numbers climb forever while
   * the desktop stays the same size.
   */
  private int nextPlayerNumber() {
    for (int candidate = 1; ; candidate++) {
      boolean taken = false;
      for (RzxPlayerInternalFrame open : rzxPlayers) {
        taken |= open.getNumber() == candidate;
      }
      if (!taken) {
        return candidate;
      }
    }
  }

  /** Which player, if any, is driving that machine's window. */
  private RzxPlayerInternalFrame playerDriving(JInternalFrame machine) {
    if (machine == null) {
      return null;
    }
    for (RzxPlayerInternalFrame player : rzxPlayers) {
      if (player.getMachineWindow() == machine) {
        return player;
      }
    }
    return null;
  }

  /**
   * Brings a pair up together, the activated one on top.
   * <p>
   * Raising the partner first and the activated window second is what keeps clicking the
   * controls from burying them under their own picture. The two are not dragged together: with
   * several recordings open the whole point is to put the pictures side by side and the controls
   * where there is room, and windows that follow each other about would be fought all day.
   */
  private void raisePartner(JInternalFrame partner, JInternalFrame activated) {
    if (partner != null && !partner.isClosed() && partner.isVisible()) {
      partner.toFront();
      activated.toFront();
    }
  }

  /**
   * Fetches a recording and plays it. The fetching is off the event thread, and a recording may
   * arrive on its own or inside a zip, which is the same choice a game download already makes.
   */
  public void playRecording(RzxOption option) {
    playRecording(option, null);
  }

  /**
   * @param preferredEntry the file inside the archive to open, when one was chosen before and
   *                       kept. With it a favourite comes back to the same recording instead of
   *                       asking again which part was meant.
   */
  public void playRecording(RzxOption option, String preferredEntry) {
    // A player of its own when the open ones are busy: picking a second recording out of the
    // archive while the first plays should add to the desktop, not replace what is on it.
    RzxPlayerInternalFrame player = showRzxPlayer();
    // The same window a tape gets. A line inside the player was easy to miss, and fetching a
    // recording takes as long as fetching anything else.
    JDialog loading = showLoading("Fetching " + option.label() + "...");
    new SwingWorker<java.io.File, Void>() {
      @Override
      protected java.io.File doInBackground() throws Exception {
        java.util.List<java.nio.file.Path> parts = DownloadAndUnzip.fetchAll(option.url(),
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "rzx-downloads"));
        if (parts.isEmpty()) {
          throw new java.io.IOException("nothing playable came out of it");
        }
        return choosePart(option, parts, preferredEntry).toFile();
      }

      @Override
      protected void done() {
        loading.dispose();
        try {
          java.io.File part = get();
          player.setPendingSource(option.url(), part.getName());
          player.openRecording(part);
        } catch (Exception e) {
          player.setBusy(null);
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Could not fetch " + option.label() + ".\n\n" + reason(e),
              "Play recording", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  /**
   * Puts a recording's machine in an ordinary emulator window. It is an emulator like any other
   * from here on - which is what lets the recording be stopped and the game carried on by hand.
   */
  private void showRzxMachine(RzxPlayerInternalFrame player,
                              com.fpetrola.oozx.speccy.rzx.RzxSession session) {
    SwingUtilities.invokeLater(() -> {
      // One window per player, reused: starting the recording over builds a new machine, and
      // leaving the old window behind would pile up a dead emulator per press of Stop. Per
      // PLAYER, though - the previous one belonging to this player, not whichever was made last,
      // which with two recordings open was the other player's and still being watched.
      JInternalFrame previous = player.getMachineWindow();
      if (previous != null && !previous.isClosed()) {
        previous.dispose();
      }
      EmulatorInternalFrame machine = createNewEmulator(session.getSpeccy().z80.mockCore);
      // The same number the controls carry, so it is possible to tell across a crowded desktop
      // which picture belongs to which set of buttons.
      String name = player.getRecordingFile() == null ? "" : ": " + player.getRecordingFile().getName();
      machine.setTitle("Spectrum #" + player.getNumber() + name);
      player.setMachineWindow(machine);
      machine.addInternalFrameListener(new InternalFrameAdapter() {
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
          // Closing the machine's window stops the recording driving it; otherwise the thread
          // goes on running a machine nobody can see, and it goes on making sound.
          player.machineClosed();
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
          raisePartner(player, machine);
        }
      });
    });
  }

  /**
   * Which part to play. A game recorded over several sittings arrives as one file per part -
   * Rick Dangerous 2 comes as five - and playing whichever one happened to be picked shows a
   * fifth of the game and looks like a fault, so the choice is the person's.
   */
  private java.nio.file.Path choosePart(RzxOption option, java.util.List<java.nio.file.Path> parts,
                                        String preferredEntry) {
    if (parts.size() == 1) {
      return parts.get(0);
    }
    if (preferredEntry != null) {
      java.util.Optional<java.nio.file.Path> remembered = parts.stream()
          .filter(part -> part.getFileName().toString().equals(preferredEntry)).findFirst();
      if (remembered.isPresent()) return remembered.get();
    }
    Object[] names = parts.stream().map(part -> part.getFileName().toString()).toArray();
    Object chosen = JOptionPane.showInputDialog(this,
        option.label() + " was recorded in " + parts.size() + " parts. Which one?",
        "Play recording", JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
    return parts.stream()
        .filter(part -> part.getFileName().toString().equals(chosen))
        .findFirst().orElse(parts.get(0));
  }

  /** Asks for a recording. A recording brings its own machine, so no emulator is needed. */
  private java.io.File chooseRecording(RzxPlayerInternalFrame player) {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    javax.swing.filechooser.FileFilter previous = fileChooser.getFileFilter();
    // Zips as well as recordings: the archive serves a good part of its catalogue that way, and
    // someone who saved one has a zip on their disk, not a .rzx. Offering only .rzx hid those
    // files from the dialog, and picking one through All Files handed the zip straight to the
    // parser, which rightly says it is not a recording it can read.
    javax.swing.filechooser.FileNameExtensionFilter recordings =
        new javax.swing.filechooser.FileNameExtensionFilter("Recordings (*.rzx, *.zip)", "rzx", "zip");
    fileChooser.addChoosableFileFilter(recordings);
    fileChooser.setFileFilter(recordings);
    int answer = fileChooser.showOpenDialog(this);
    fileChooser.removeChoosableFileFilter(recordings);
    fileChooser.setFileFilter(previous);
    if (answer != JFileChooser.APPROVE_OPTION) {
      return null;
    }
    config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
    return openableRecording(player, fileChooser.getSelectedFile());
  }

  /**
   * What to hand the player for a file off the disk: the file itself, or, if it is an archive,
   * whichever recording inside it the person picked.
   * <p>
   * Fetching one from the archive already unpacks and asks; opening the same file from disk did
   * not, so the two ways of arriving at the same recording did not agree. A zip holding one
   * recording opens without a question; one holding several asks the same way a download does.
   */
  private java.io.File openableRecording(RzxPlayerInternalFrame player, java.io.File chosen) {
    if (chosen == null || !chosen.getName().toLowerCase().endsWith(".zip")) {
      return chosen;
    }
    try {
      java.util.List<java.nio.file.Path> parts = DownloadAndUnzip.fetchAll(
          chosen.toURI().toString(),
          java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "rzx-downloads"));
      if (parts.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "There is nothing playable inside " + chosen.getName() + ".",
            "Open recording", JOptionPane.ERROR_MESSAGE);
        return null;
      }
      java.io.File part =
          choosePart(new RzxOption(chosen.getName(), chosen.toURI().toString()), parts, null).toFile();
      // The unpacked entry lives in a temporary directory that will not be there tomorrow. What
      // lasts is the zip the person picked plus which recording inside it this was, so a
      // favourite made from here can unpack the same one again.
      player.setPendingSource(chosen.getAbsolutePath(), part.getName());
      return part;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Could not open " + chosen.getName() + ".\n\n" + reason(e),
          "Open recording", JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  /**
   * Opens a machine on a file and lets it load itself, which for a tape means the auto loader
   * types LOAD "" and plays from the start. Off the event thread: building a machine downloads,
   * unzips and boots, and doing that on the event thread freezes the window.
   */
  /**
   * What to tell someone about a failure: what went wrong, not the pile of wrappers that carried
   * it here. A download refused by an archive arrives as an ExecutionException around a
   * RuntimeException around an IOException, and printing that put three Java class names and a
   * full URL in front of the one sentence that says anything.
   */
  static String reason(Throwable failure) {
    Throwable deepest = failure;
    while (deepest.getCause() != null && deepest.getCause() != deepest) {
      deepest = deepest.getCause();
    }
    String message = deepest.getMessage();
    return message == null || message.isBlank() ? deepest.getClass().getSimpleName() : message;
  }

  public void loadInNewEmulator(String path) {
    // Downloading, unzipping and booting can take several seconds, and until now they took them
    // in silence: nothing appeared until the machine did, which reads as the click not working.
    JDialog loading = showLoading(path.startsWith("http") ? "Fetching " + nameOf(path) + "..."
        : "Loading " + nameOf(path) + "...");

    new SwingWorker<EmulatorCore, Void>() {
      @Override
      protected EmulatorCore doInBackground() {
        return mockCore.apply(path);
      }

      @Override
      protected void done() {
        loading.dispose();
        try {
          createNewEmulator(get(), path);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Could not load " + path + ".\n\n" + reason(e), "Load failed", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private static String nameOf(String path) {
    int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf(java.io.File.separatorChar));
    return slash < 0 ? path : path.substring(slash + 1);
  }

  /**
   * A small window saying something is happening, for work that runs off the event thread.
   * Deliberately not modal: it says wait, it does not take the application away.
   */
  JDialog showLoading(String message) {
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Loading");
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    panel.add(new JLabel(message), BorderLayout.NORTH);

    JProgressBar progress = new JProgressBar();
    progress.setIndeterminate(true);
    panel.add(progress, BorderLayout.CENTER);

    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    return dialog;
  }

  /** Opens the cassette browser, or brings the open one to the front. */
  public TapeBrowserInternalFrame showTapeBrowser() {
    if (tapeBrowser == null || tapeBrowser.isClosed()) {
      tapeBrowser = new TapeBrowserInternalFrame(this::getActiveTape, this::chooseTapeForBrowser,
          file -> loadInNewEmulator(file.getAbsolutePath()));
      desktop.add(tapeBrowser);
    }
    tapeBrowser.setVisible(true);
    tapeBrowser.toFront();
    return tapeBrowser;
  }

  /** Opens the browser on a cassette already loaded and running, as a game from the browser is. */
  public void showTapeBrowser(java.io.File tapeFile, com.fpetrola.oozx.speccy.modules.tape.Tape deck) {
    SwingUtilities.invokeLater(() -> showTapeBrowser().adopt(tapeFile, deck));
  }

  /** Asks for a tape file and loads it into the cassette browser. No emulator is needed. */
  public void chooseTapeForBrowser() {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    java.io.File file = fileChooser.getSelectedFile();
    config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
    showTapeBrowser().openTape(file);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1) {
    return createNewEmulator(core1, (String) null);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, String filePath) {
    return createNewEmulator(core1, filePath, null);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, GameSearchResult gameSearchResult) {
    return createNewEmulator(core1, null, gameSearchResult);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, String filePath, GameSearchResult gameSearchResult) {
    EmulatorCore core = core1;

    // Asignar el filename si se proporciona
    if (filePath != null && !filePath.isEmpty()) {
      core.setFilename(filePath);
    }

    JComponent panel = core.getPanel();
    int x = (emulatorCount * 30) % 400;
    int y = (emulatorCount * 30) % 300;
    EmulatorInternalFrame frame = new EmulatorInternalFrame(core, x, y, this, gameSearchResult);
    frame.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosed(InternalFrameEvent e) {
        core1.finishEmulation();
      }
    });

    panel.setFocusable(true);
    KeyListener keyListener = core1.getKeyListener();
    panel.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        panel.addKeyListener(keyListener);
        System.out.println("gained");
      }

      public void focusLost(FocusEvent e) {
        panel.removeKeyListener(keyListener);
        System.out.println("lost");

      }

      private Container getParent() {
        return frame;
      }
    });

    desktop.add(frame);
    frame.setVisible(true);
    emulatorCount++;

    // Registrar en el historial - capturar el estado inicial después de cargar
    // Usar un timer para permitir que se cargue el snapshot completamente
    final String finalFilePath = filePath;
    final EmulatorCore finalCore = core;

    Timer stateCapture = new Timer(500, e -> {
      try {
        // Obtener el path real del emulator
        String actualFilePath = finalFilePath;
        if (actualFilePath == null || actualFilePath.isEmpty()) {
          actualFilePath = finalCore.getFilename();
        }

        if (actualFilePath != null && !actualFilePath.isEmpty()) {
          String gameName = new java.io.File(actualFilePath).getName();

          // Intentar capturar el estado inicial
          String initialStateData = null;
          try {
            RegistersGetter registersGetter = finalCore.getRegistersGetter();
            State state = finalCore.getState();
            if (registersGetter != null && state != null) {
              initialStateData = SnapshotSaver.getSnapshotAsUnicodePacked(registersGetter, state);
            }
          } catch (Exception ex) {
            System.err.println("Error capturando estado inicial: " + ex.getMessage());
          }

          // Registrar en el historial con el estado (o sin si no se pudo capturar)
          config.addToSnapshotHistory(actualFilePath, gameName, initialStateData);
        }
      } finally {
        ((Timer) e.getSource()).stop();
      }
    });
    stateCapture.setRepeats(false);
//    stateCapture.start();

    return frame;
  }


  private void saveMainWindowState() {
    OOZxConfiguration.WindowState mainState = new OOZxConfiguration.WindowState(
        "MAIN_WINDOW", getX(), getY(), getWidth(), getHeight());
    config.setMainWindowState(mainState);
  }

  private void restoreMainWindowState() {
    OOZxConfiguration.WindowState mainState = config.getMainWindowState();
    if (mainState != null) {
      if (mainState.getWidth() > 0 && mainState.getHeight() > 0) {
        setSize(mainState.getWidth(), mainState.getHeight());
      }
      if (mainState.getX() >= 0 && mainState.getY() >= 0) {
        setLocation(mainState.getX(), mainState.getY());
      }
    }
  }

  private void saveOpenWindows() {
    config.getOpenWindows().clear();

    for (JInternalFrame frame : desktop.getAllFrames()) {
      if (frame instanceof EmulatorInternalFrame) {
        EmulatorInternalFrame eFrame = (EmulatorInternalFrame) frame;
        // Obtener el archivo que se está emulando (si existe)
        String filePath = eFrame.emulatorCore.getFilename();
        config.getOpenWindows().add(eFrame.saveWindowState(filePath));
      } else if (frame instanceof GameBrowserInternalFrame) {
        GameBrowserInternalFrame gFrame = (GameBrowserInternalFrame) frame;
        config.getOpenWindows().add(gFrame.saveWindowState());
      } else if (frame instanceof SnapshotHistoryInternalFrame) {
        SnapshotHistoryInternalFrame hFrame = (SnapshotHistoryInternalFrame) frame;
        config.getOpenWindows().add(hFrame.saveWindowState());
      }
    }
  }

  private void restoreOpenWindows() {
    // Crear todas las ventanas primero
    for (OOZxConfiguration.WindowState windowState : config.getOpenWindows()) {
      if ("EMULATOR".equals(windowState.getType())) {
        // Restaurar el estado del emulador desde el snapshot comprimido
        if (windowState.getSnapshotId() != null && !windowState.getSnapshotId().isEmpty()) {
          try {
            String snapshotData = config.getSnapshot(windowState.getSnapshotId());
            if (snapshotData != null && !snapshotData.isEmpty()) {
              SpectrumState spectrumState = SnapshotSaver.loadSnapshotFromUnicodePacked(snapshotData);
              EmulatorCore core = mockCoreState.apply(spectrumState);
              EmulatorInternalFrame frame = createNewEmulator(core);
              frame.restoreWindowState(windowState);
            }
          } catch (Exception e) {
            System.err.println("Error restaurando snapshot desde configuración: " + e.getMessage());
          }
        }
      } else if ("GAME_BROWSER".equals(windowState.getType())) {
        if (gameBrowser == null || gameBrowser.isClosed()) {
          gameBrowser = new GameBrowserInternalFrame(createGameBrowserListener());
          desktop.add(gameBrowser);
          gameBrowser.setVisible(true);
        }
        gameBrowser.restoreWindowState(windowState);
      } else if ("SNAPSHOT_HISTORY".equals(windowState.getType())) {
        if (snapshotHistory == null || snapshotHistory.isClosed()) {
          openSnapshotHistory();
        }
        if (snapshotHistory != null && !snapshotHistory.isClosed()) {
          snapshotHistory.restoreWindowState(windowState);
        }
      }
    }

//    // Restaurar el orden Z de todas las ventanas
//    for (OOZxConfiguration.WindowState windowState : config.getOpenWindows()) {
//      JInternalFrame frame = findFrameByWindowState(windowState);
//      if (frame != null && windowState.getZOrder() >= 0) {
//        desktop.setComponentZOrder(frame, windowState.getZOrder());
//      }
//    }
  }

  private JInternalFrame findFrameByWindowState(OOZxConfiguration.WindowState windowState) {
    for (JInternalFrame frame : desktop.getAllFrames()) {
      if ("EMULATOR".equals(windowState.getType()) && frame instanceof EmulatorInternalFrame) {
        EmulatorInternalFrame eFrame = (EmulatorInternalFrame) frame;
        if (windowState.getSnapshotId() != null && eFrame.saveWindowState(eFrame.emulatorCore.getFilename()).getSnapshotId().equals(windowState.getSnapshotId())) {
          return frame;
        }
      } else if ("GAME_BROWSER".equals(windowState.getType()) && frame instanceof GameBrowserInternalFrame) {
        return frame;
      }
    }
    return null;
  }

  private GameBrowserListener createGameBrowserListener() {
    return new GameBrowserListener() {
      @Override
      public void onGameSelected(GameSearchResult gameSearchResult, Runnable whenDone) {
        getActiveEmulatorOrCreateNew(gameSearchResult, whenDone);
      }

      @Override
      public void onPlayRecording(RzxOption recording) {
        playRecording(recording);
      }

      @Override
      public void onViewDetails(GameSearchResult gameSearchResult) {
        // Show loading dialog while fetching from API
        JDialog loadingDialog = new JDialog(ZXSpectrumDesktopApp.this, "Loading Game Details", true);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(ZXSpectrumDesktopApp.this);
        JLabel loadingLabel = new JLabel("Fetching game details from ZXInfo API...");
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        loadingDialog.add(loadingLabel);

        // Fetch details in background thread
        SwingWorker<com.fpetrola.oozx.api.GameDetail, Void> worker =
            new SwingWorker<com.fpetrola.oozx.api.GameDetail, Void>() {
              @Override
              protected com.fpetrola.oozx.api.GameDetail doInBackground() throws Exception {
                // Extract ID from URL (e.g., "https://zxinfo.dk/games/xxxx")
                String gameId = gameSearchResult.id;
                // Fetch full details from API
                ZxInfoApiHandler apiHandler = new ZxInfoApiHandler();
                return apiHandler.fetchGameDetails(gameId);
              }

              @Override
              protected void done() {
                loadingDialog.dispose();
                try {
                  com.fpetrola.oozx.api.GameDetail gameDetail = get();

                  if (gameDetail == null) {
                    // Fallback to basic info if API call fails
                    gameDetail = new com.fpetrola.oozx.api.GameDetail();
                    gameDetail.id = gameSearchResult.url;
                    gameDetail.title = gameSearchResult.title;
                    gameDetail.yearOfRelease = "Unknown";
                    gameDetail.publisher = "Unknown";
                    gameDetail.genre = "Unknown";
                    gameDetail.machineType = "Spectrum 48K";
                    gameDetail.memoryRequired = "48K";
                    gameDetail.screenshots = new ArrayList<>();
                    if (gameSearchResult.screenshot1 != null && !gameSearchResult.screenshot1.isEmpty()) {
                      gameDetail.screenshots.add(gameSearchResult.screenshot1);
                    }
                    if (gameSearchResult.screenshot2 != null && !gameSearchResult.screenshot2.isEmpty()) {
                      gameDetail.screenshots.add(gameSearchResult.screenshot2);
                    }
                    gameDetail.description = "Game description not available";
                  }

                  GameDetailsDialog dialog = new GameDetailsDialog(ZXSpectrumDesktopApp.this, gameDetail);
                  dialog.setVisible(true);
                } catch (Exception e) {
                  JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
                      "Error loading game details: " + e.getMessage(),
                      "Error", JOptionPane.ERROR_MESSAGE);
                }
              }
            };

        worker.execute();
        loadingDialog.setVisible(true);
      }

      @Override
      public void onAddToFavorites(GameSearchResult game) {
        keepAsFavorite(game, null);
      }

      @Override
      public void onDownloadGame(String gameUrl) {
        JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
            "Downloading: " + gameUrl + "n(Download feature coming soon)", "Download",
            JOptionPane.INFORMATION_MESSAGE);
      }
    };
  }

  private void updateRecentFilesMenu() {
    recentFilesMenu.removeAll();
    List<String> recentFiles = config.getRecentFiles();

    if (recentFiles.isEmpty()) {
      JMenuItem emptyItem = new JMenuItem("(No recent files)");
      emptyItem.setEnabled(false);
      recentFilesMenu.add(emptyItem);
      return;
    }

    for (String filePath : recentFiles) {
      JMenuItem item = new JMenuItem(new java.io.File(filePath).getName());
      item.setToolTipText(filePath);
      item.addActionListener(e -> {
        EmulatorCore emulatorCore = mockCore.apply(filePath);
        createNewEmulator(emulatorCore);
      });
      recentFilesMenu.add(item);
    }

    recentFilesMenu.addSeparator();
    JMenuItem clearItem = new JMenuItem("Clear Recent Files");
    clearItem.addActionListener(e -> {
      config.getRecentFiles().clear();
      config.save();
      updateRecentFilesMenu();
    });
    recentFilesMenu.add(clearItem);
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

  public static int getComponentZOrder(JInternalFrame component) {
    Container parent = component.getParent();
    if (parent != null) {
      Component[] components = parent.getComponents();
      for (int i = 0; i < components.length; i++) {
        if (components[i] == component) {
          return i;
        }
      }
    }
    return 0;
  }

  public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            ZXSpectrumDesktopApp app = new ZXSpectrumDesktopApp(() -> new MockEmulatorCore(null));
//            app.setVisible(true);
//            app.createNewEmulator(new MockEmulatorCore(null));
//        });
  }
}