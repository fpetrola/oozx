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

package com.fpetrola.oozx.fuse.peripherals.t;

import com.fpetrola.oozx.api.GameEntry;
import com.fpetrola.oozx.api.Hit;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.fuse.peripherals.EmulatorCore;
import com.fpetrola.oozx.fuse.peripherals.EmulatorListener;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.*;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.fpetrola.oozx.fuse.peripherals.t.EmulatorInternalFrame.loadIcon;

// Emulator Internal Frame
class EmulatorInternalFrame extends JInternalFrame {
  private EmulatorCore emulatorCore;
  //  private JLabel statusLabel;
  private JProgressBar speedBar;
  private JComboBox<String> modelCombo;
  private JLabel pauseIndicator;
  private JLabel turboIndicator;
  private float scaleFactor0 = 1.73f;
  private float scaleFactor = scaleFactor0;
  //  private JLabel tapeStatusLabel;

  public EmulatorInternalFrame(EmulatorCore core, int x, int y) {
    super("ZX Spectrum Emulator", true, true, true, true);
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

  private JToolBar createToolBar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);

    //    Icon turboIcon = UIManager.getIcon("FileChooser.upFolderIcon");
    JButton turboButton = new JButton(loadIcon("1F680.svg"));
    turboButton.setToolTipText("Toggle Turbo Mode");
    turboButton.addActionListener(e -> emulatorCore.setGeneralOption("turbo", emulatorCore.isTurboMode()));
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

    JButton pauseButton = new JButton(loadIcon("23EF.svg"));
    pauseButton.setToolTipText("Pause Emulation");
    pauseButton.addActionListener(e -> emulatorCore.pauseEmulation());
    toolBar.add(pauseButton);

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

    return toolBar;
  }

  public static ImageIcon loadIcon(String iconFile) {
    int size = 24;
    ImageIcon turboIcon = SvgIconLoader.loadSvgAsImageIcon("/icons/" + iconFile, size, size);
    return turboIcon;
  }
}

// --- NEW: Game Search Result Model ---
class GameSearchResult {
  String title;
  String url;
  String screenshot1;
  String screenshot2;
  String filename;

  public GameSearchResult(String title, String url, String screenshot1, String screenshot2, String filename) {
    this.title = title;
    this.url = url;
    this.screenshot1 = screenshot1;
    this.screenshot2 = screenshot2;
    this.filename = filename;
  }
}

// --- NEW: Game Browser Listener Interface ---
interface GameBrowserListener {
  void onGameSelected(GameSearchResult gameUrl);

  void onViewDetails(String gameUrl);

  void onAddToFavorites(String gameUrl);

  void onDownloadGame(String gameUrl);
}

// --- NEW: Game Browser Internal Frame ---
class GameBrowserInternalFrame extends JInternalFrame {
  private JTextField searchField;
  private JPanel resultsPanel;
  private GameBrowserListener listener;

  public GameBrowserInternalFrame(GameBrowserListener listener) {
    super("Game Browser", true, true, true, true);
    this.listener = listener;
    setSize(560, 600);
    setLocation(50, 50);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());
    topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    searchField = new JTextField();
    searchField.setFont(new Font("Arial", Font.PLAIN, 16));
    JButton searchButton = new JButton("Search");
    searchButton.setPreferredSize(new Dimension(100, 30));

    topPanel.add(searchField, BorderLayout.CENTER);
    topPanel.add(searchButton, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);

    resultsPanel = new JPanel();
    resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
    resultsPanel.setBackground(Color.WHITE);

    JScrollPane scrollPane = new JScrollPane(resultsPanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    add(scrollPane, BorderLayout.CENTER);

    searchButton.addActionListener(e -> performSearch());
    searchField.addActionListener(e -> performSearch());
  }

  private void performSearch() {
    String query = searchField.getText().trim();
    if (query.isEmpty()) return;

    // Clear previous results
    resultsPanel.removeAll();

    SwingUtilities.invokeLater(() -> {
      // Mock search results (in real app, this would be async from web)
      List<GameSearchResult> mockResults = createMockResults(query);

      for (GameSearchResult result : mockResults) {
        JPanel gameRow = createGameRow(result);
        resultsPanel.add(gameRow);
        resultsPanel.add(Box.createVerticalStrut(10));
      }

      resultsPanel.revalidate();
      resultsPanel.repaint();
    });
  }

  private List<GameSearchResult> createMockResults(String query) {
    List<Hit> search = new ZxInfoApiHandler().search(query);

    List<GameSearchResult> results = new ArrayList<>();

    for (Hit hit : search) {
      GameEntry game = hit._source;
      if (game.contentType.equals("SOFTWARE")) {
        List<String> screenshots = new ArrayList<>();
        game.screens.forEach(s -> {
          String filename = "https://worldofspectrum.net" + s.url;
          if (s.url.startsWith("/zxscreens"))
            filename = "https://zxinfo.dk/media" + s.url;

          screenshots.add(filename);
        });
        List<String> files = new ArrayList<>();

        game.releases.forEach(s -> {
          s.files.forEach(f -> {
            if (f.format != null) {
              System.out.println(f.format);
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)", "Tape (TAP)", "Perfect tape (TZX)");
              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)", "Perfect tape (TZX)");
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)");
              if (anObject.contains(f.format)) {
                String filename = "https://worldofspectrum.net" + f.path;
                if (f.path.startsWith("/zxdb"))
                  filename = "https://spectrumcomputing.co.uk" + f.path;
                files.add(filename);
              }
            }
          });
        });

        String screenshot1 = getString(screenshots, 0);
        String screenshot2 = getString(screenshots, 1);
        if (!files.isEmpty()) {
          String s = !files.isEmpty() ? files.get(0) : null;
          results.add(new GameSearchResult(game.title, "http://example.com/game/" + query, screenshot1, screenshot2, s));
        }
      }
    }
    return results;
  }

  private String getString(List<String> screenshots, int x) {
    if (x == -1)
      return "https://i.sstatic.net/wAz1X.gif";
    else
      return screenshots.size() > x ? screenshots.get(x) : getString(screenshots, x - 1);
  }

  private GameData getGameData() {
    String text = "https://worldofspectrum.net/pub/sinclair/screens/in-game/e/EveryonesAWally.gif";
    ImageIcon img1 = getImageIcon(text);
//      ImageIcon img1 = createPlaceholderImage(256, 192, Color.LIGHT_GRAY, text);
//    String text1 = "https://worldofspectrum.net/pub/sinclair/screens/in-game/e/EveryonesAWally.gif";
//      ImageIcon img2 = createPlaceholderImage(256, 192, Color.GRAY, text1);
//    ImageIcon img2 = new ImageIcon(url);
    GameData gameData = new GameData(img1, img1);
    return gameData;
  }

  private ImageIcon getImageIcon(String text) {
    try {
      return new ImageIcon(new URL(text));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private record GameData(ImageIcon img1, ImageIcon img2) {
  }

  private ImageIcon createPlaceholderImage(int w, int h, Color bg, String text) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(bg);
    g.fillRect(0, 0, w, h);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, w - 1, h - 1);
    g.setFont(new Font("Arial", Font.BOLD, 16));
    FontMetrics fm = g.getFontMetrics();
    int x = (w - fm.stringWidth(text)) / 2;
    int y = (h - fm.getHeight()) / 2 + fm.getAscent();
    g.drawString(text, x, y);
    g.dispose();
    return new ImageIcon(img);
  }

  private JPanel createGameRow(GameSearchResult result) {
    JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));
    MouseAdapter l = new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        Color color = UIManager.getColor("List.selectionBackground");
        row.setBackground(color);
      }

      public void mouseExited(MouseEvent e) {
        row.setBackground(Color.WHITE);
      }
    };
    row.addMouseListener(l);
    row.setBackground(Color.WHITE);

    JLabel imgLabel1 = new JLabel();
    loadLazyImage(imgLabel1, result.screenshot1, l);
    imgLabel1.setAlignmentY(Component.TOP_ALIGNMENT);
    JLabel imgLabel2 = new JLabel();
    loadLazyImage(imgLabel2, result.screenshot2, l);
    imgLabel2.setAlignmentY(Component.TOP_ALIGNMENT);

    // Context menu
    JPopupMenu contextMenu = new JPopupMenu();
    JMenuItem loadItem = new JMenuItem("Load Game");
    JMenuItem detailsItem = new JMenuItem("View Details");
    JMenuItem favoriteItem = new JMenuItem("Add to Favorites");
    JMenuItem downloadItem = new JMenuItem("Download");
    contextMenu.add(loadItem);
    contextMenu.add(detailsItem);
    contextMenu.add(favoriteItem);
    contextMenu.add(downloadItem);

    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) showPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) showPopup(e);
      }

      private void showPopup(MouseEvent e) {
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
          listener.onGameSelected(result);
        }
      }
    };

    imgLabel1.addMouseListener(mouseAdapter);
    imgLabel2.addMouseListener(mouseAdapter);

    loadItem.addActionListener(e -> listener.onGameSelected(result));
    detailsItem.addActionListener(e -> listener.onViewDetails(result.title));
    favoriteItem.addActionListener(e -> listener.onAddToFavorites(result.url));
    downloadItem.addActionListener(e -> listener.onDownloadGame(result.url));

    row.add(imgLabel1);
    row.add(Box.createHorizontalStrut(10));
    row.add(imgLabel2);
    row.add(Box.createHorizontalGlue());

    return row;
  }

  private void loadLazyImage(JLabel imgLabel1, String screenshot1, MouseAdapter mouseAdapter) {
    LazyImageIconLoader lazyImageIconLoader = new LazyImageIconLoader(imgLabel1, screenshot1, mouseAdapter);
    lazyImageIconLoader.execute();
  }

  public void setSearchQuery(String query) {
    searchField.setText(query);
    performSearch();
  }
}

// --- UPDATED: ZXSpectrumDesktopApp with Game Browser ---
public class ZXSpectrumDesktopApp extends JFrame {
  private final Function<String, EmulatorCore> mockCore;
  private JDesktopPane desktop;
  private int emulatorCount = 0;
  private GameBrowserInternalFrame gameBrowser;

  public ZXSpectrumDesktopApp(Function<String, EmulatorCore> mockCore) {
    this.mockCore = mockCore;
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
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    AbstractAction newEmulatorAction = new AbstractAction("New Emulator") {
      @Override
      public void actionPerformed(ActionEvent e) {
        EmulatorCore emulatorCore = mockCore.apply("");
        createNewEmulator(emulatorCore);
      }
    };
    fileMenu.add(newEmulatorAction);

    JMenuItem gameBrowserMenuItem = new JMenuItem("Game Browser...");
    gameBrowserMenuItem.addActionListener(e -> openGameBrowser());
    fileMenu.add(gameBrowserMenuItem);

    AbstractAction exitAction = new AbstractAction("Exit") {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    };
    fileMenu.add(exitAction);

    menuBar.add(fileMenu);

    addLFMenu(menuBar);
    addWindowMenu(menuBar);
    return menuBar;
  }

  private void addLFMenu(JMenuBar menuBar) {
    JMenu windowMenu = new JMenu("Look&Feel");
    windowMenu.setMnemonic(KeyEvent.VK_W);

    addLaf(windowMenu, new DarculaTheme());
    addLaf(windowMenu, new OneDarkTheme());
    addLaf(windowMenu, new SolarizedLightTheme());
    addLaf(windowMenu, new SolarizedDarkTheme());
    addLaf(windowMenu, new IntelliJTheme());

    AbstractAction tileAction = new AbstractAction("Metal") {
      public void actionPerformed(ActionEvent e) {
        try {
//          LafManager.install(new DarculaTheme());

          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
          LafManager.updateLaf();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
    windowMenu.add(tileAction);

    menuBar.add(windowMenu);
  }

  private void addLaf(JMenu windowMenu, final Theme theme) {
    AbstractAction cascadeAction = new AbstractAction(theme.getName()) {
      public void actionPerformed(ActionEvent e) {
        LafManager.install(theme);
      }
    };
    windowMenu.add(cascadeAction);
  }

  private void addWindowMenu(JMenuBar menuBar) {
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

    return toolBar;
  }

  private void openGameBrowser() {
    if (gameBrowser == null || gameBrowser.isClosed()) {
      gameBrowser = new GameBrowserInternalFrame(new GameBrowserListener() {
        @Override
        public void onGameSelected(GameSearchResult gameSearchResult) {
          // Find or create an emulator and load the game
          EmulatorInternalFrame target = getActiveEmulatorOrCreateNew(gameSearchResult);
          if (target != null) {
            // In real app: download and load ROM/tape
//            JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
//                "Loading game from: " + gameSearchResult + "",
//                "Load Game", JOptionPane.INFORMATION_MESSAGE);
          }
        }

        @Override
        public void onViewDetails(String gameUrl) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Game Details: " + gameUrl + "",
              "Game Details", JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void onAddToFavorites(String gameUrl) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Added to favorites: " + gameUrl,
              "Favorites", JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void onDownloadGame(String gameUrl) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              "Downloading: " + gameUrl + "\n(Download feature coming soon)",
              "Download", JOptionPane.INFORMATION_MESSAGE);
        }
      });
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

  private EmulatorInternalFrame getActiveEmulatorOrCreateNew(GameSearchResult gameSearchResult) {
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
    // Create new if none active
    EmulatorCore core = mockCore.apply(gameSearchResult.filename);
    EmulatorInternalFrame newFrame = createNewEmulator(core);
//    EmulatorInternalFrame newFrame = new EmulatorInternalFrame(core, 100, 100);
//    desktop.add(newFrame);
//    newFrame.setVisible(true);
    return newFrame;
  }

  // ... (rest of the methods: createNewEmulator, cascadeWindows, tileWindows remain unchanged)

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1) {
    EmulatorCore core = core1;
    JComponent panel = core.getPanel();
    int x = (emulatorCount * 30) % 400;
    int y = (emulatorCount * 30) % 300;
    EmulatorInternalFrame frame = new EmulatorInternalFrame(core, x, y);
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
    return frame;
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
//        SwingUtilities.invokeLater(() -> {
//            ZXSpectrumDesktopApp app = new ZXSpectrumDesktopApp(() -> new MockEmulatorCore(null));
//            app.setVisible(true);
//            app.createNewEmulator(new MockEmulatorCore(null));
//        });
  }
}