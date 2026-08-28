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

import com.fpetrola.oozx.api.GameDetail;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.AbstractAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GameDetailsDialog extends JDialog {
  private GameDetail gameDetail;
  private static final int DIALOG_WIDTH = 1000;
  private static final int DIALOG_HEIGHT = 700;

  public GameDetailsDialog(Frame owner, GameDetail gameDetail) {
    super(owner, "Game Details - " + gameDetail.title, true);
    this.gameDetail = gameDetail;

    setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    initializeComponents();
    setupEscapeToClose();
  }

  private void setupEscapeToClose() {
    KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "closeDialog");
    getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
  }

  /**
   * Build complete image URL using same logic as createMockResults
   */
  private String buildCompleteImageUrl(String imagePath) {
    if (imagePath == null || imagePath.isEmpty()) {
      return null;
    }

    // If already a complete URL, return as is
    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
      return imagePath;
    }

    // Use the same logic as createMockResults:
    // Default to WorldOfSpectrum, but if path starts with /zxscreens use zxinfo.dk/media

    return GameBrowserInternalFrame.getFileURL(imagePath);
  }

  private void initializeComponents() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Top: Search bar for finding another game
    JPanel searchPanel = createSearchPanel();
    mainPanel.add(searchPanel, BorderLayout.NORTH);

    // Left side: Cover and basic info
    JPanel leftPanel = createLeftPanel();

    // Right side: Tabs with detailed information
    JPanel rightPanel = createRightPanel();

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(leftPanel, BorderLayout.WEST);
    centerPanel.add(rightPanel, BorderLayout.CENTER);

    mainPanel.add(centerPanel, BorderLayout.CENTER);

    // Bottom: Action buttons
    JPanel buttonsPanel = createButtonsPanel();
    mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private JPanel createSearchPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 0));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Search Another Game"),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));

    JTextField searchField = new JTextField();
    searchField.setToolTipText("Enter game name to search for details");
    searchField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 30));
    searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

    JButton searchButton = new JButton("Search");
    searchButton.addActionListener(e -> searchForGame(searchField.getText()));

    // Add Enter key support to search field
    searchField.addActionListener(e -> searchForGame(searchField.getText()));

    panel.add(searchField, BorderLayout.CENTER);
    panel.add(searchButton, BorderLayout.EAST);
    panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 60));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

    return panel;
  }

  private void searchForGame(String gameName) {
    if (gameName == null || gameName.trim().isEmpty()) {
      GameNotFoundDialog.showSimple(getOwner(), "Please enter a game name");
      return;
    }

    // Show loading dialog
    JDialog loadingDialog = new JDialog(this, "Searching", true);
    loadingDialog.setSize(300, 100);
    loadingDialog.setLocationRelativeTo(this);
    JLabel loadingLabel = new JLabel("Searching for: " + gameName);
    loadingLabel.setHorizontalAlignment(JLabel.CENTER);
    loadingDialog.add(loadingLabel);

    // Search in background
    SwingWorker<GameDetail, Void> worker =
        new SwingWorker<GameDetail, Void>() {
          @Override
          protected GameDetail doInBackground() throws Exception {
            try {
              com.fpetrola.oozx.api.ZxInfoApiHandler apiHandler =
                  new com.fpetrola.oozx.api.ZxInfoApiHandler();
              List<com.fpetrola.oozx.api.Hit> results = apiHandler.search(gameName);

              if (results == null || results.isEmpty()) {
                return null;
              }

              com.fpetrola.oozx.api.Hit bestMatch = results.get(0);
              String gameId = bestMatch._id;

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
              GameDetail detail = get();

              if (detail == null) {
                GameNotFoundDialog.showSimple(getOwner(), "Game not found: " + gameName);
                return;
              }

              // Update the dialog with new game details
              gameDetail = detail;
              setTitle("Game Details - " + gameDetail.title);

              // Refresh all panels
              Container contentPane = GameDetailsDialog.this.getContentPane();
              contentPane.removeAll();
              initializeComponents();
              contentPane.revalidate();
              contentPane.repaint();
            } catch (Exception e) {
              JOptionPane.showMessageDialog(GameDetailsDialog.this,
                  "Error loading game details: " + e.getMessage(),
                  "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        };

    worker.execute();
    loadingDialog.setVisible(true);
  }

  private JPanel createLeftPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setPreferredSize(new Dimension(280, 500));

    // Game cover image
    JPanel coverPanel = new JPanel(new BorderLayout());
    coverPanel.setBorder(BorderFactory.createTitledBorder("Game Cover"));
    JLabel coverLabel = new JLabel();
    coverLabel.setBackground(Color.DARK_GRAY);
    coverLabel.setOpaque(true);
    coverLabel.setHorizontalAlignment(JLabel.CENTER);
    coverLabel.setVerticalAlignment(JLabel.CENTER);
    coverLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Try to load cover image in background, or use first screenshot as fallback
    String imageUrlToLoad = null;
    boolean isScreenshotFallback = false;

    if (gameDetail.coverImageUrl != null && !gameDetail.coverImageUrl.isEmpty()) {
      imageUrlToLoad = gameDetail.coverImageUrl;
      coverLabel.setPreferredSize(new Dimension(240, 300));
    } else if (gameDetail.screenshots != null && !gameDetail.screenshots.isEmpty()) {
      // Use first screenshot as fallback cover - use ZX Spectrum resolution proportions
      imageUrlToLoad = buildCompleteImageUrl(gameDetail.screenshots.get(0));
      isScreenshotFallback = true;
      coverLabel.setPreferredSize(new Dimension(256, 192));
    }

    if (imageUrlToLoad != null) {
      int width = isScreenshotFallback ? 256 : 240;
      int height = isScreenshotFallback ? 192 : 300;
      loadImageAsync(imageUrlToLoad, coverLabel, width, height, "Loading cover...");
    } else {
      coverLabel.setPreferredSize(new Dimension(240, 300));
      coverLabel.setText("No cover image");
      coverLabel.setForeground(Color.WHITE);
    }

    coverPanel.add(coverLabel, BorderLayout.CENTER);

    // Rating panel
    JPanel ratingPanel = createRatingPanel();

    // Game info summary panel
    JPanel infoSummaryPanel = createInfoSummaryPanel();

    // Favorite checkbox
    JCheckBox favoriteCheckBox = new JCheckBox("Mark as Favorite");
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.add(favoriteCheckBox, BorderLayout.NORTH);
    statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));

    // Assemble left panel
    panel.add(coverPanel, BorderLayout.NORTH);

    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    centerPanel.add(ratingPanel);
    centerPanel.add(Box.createVerticalStrut(5));
    centerPanel.add(infoSummaryPanel);

    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(statusPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createRatingPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Rating"));
    JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    double rating = 0;
    if (gameDetail.score != null) {
      rating = Math.round(gameDetail.score); // Convert 0-100 to 0-5
      rating = Math.min(5, rating);
    }

    for (int i = 0; i < 5; i++) {
      JLabel star = new JLabel("★");
      star.setFont(new Font("Arial", Font.PLAIN, 18));
      if (i < rating) {
        star.setForeground(Color.YELLOW.darker());
      } else {
        star.setForeground(Color.LIGHT_GRAY);
      }
      starsPanel.add(star);
    }

    panel.add(starsPanel, BorderLayout.NORTH);
    String ratingText = gameDetail.score != null ? String.format("%.1f", gameDetail.score) : "N/A";
    JLabel ratingLabel = new JLabel("Score: " + ratingText);
    panel.add(ratingLabel, BorderLayout.CENTER);

    if (gameDetail.xrated != null && gameDetail.xrated > 0) {
      JLabel xratedLabel = new JLabel("⚠ X-Rated");
      xratedLabel.setForeground(Color.RED);
      panel.add(xratedLabel, BorderLayout.SOUTH);
    }

    return panel;
  }

  private JPanel createInfoSummaryPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createTitledBorder("Quick Info"));

    addInfoLine(panel, "Year:", gameDetail.yearOfRelease);
    addInfoLine(panel, "Publisher:", gameDetail.publisher);
    addInfoLine(panel, "Genre:", gameDetail.genre);
    addInfoLine(panel, "Machine:", gameDetail.machineType);
    addInfoLine(panel, "Memory:", gameDetail.memoryRequired);
    addInfoLine(panel, "Status:", gameDetail.availability);

    return panel;
  }

  private void addInfoLine(JPanel panel, String label, String value) {
    JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    linePanel.setMaximumSize(new Dimension(250, 20));
    JLabel labelComponent = new JLabel(label);
    labelComponent.setFont(new Font("Arial", Font.BOLD, 10));
    labelComponent.setPreferredSize(new Dimension(80, 20));
    JLabel valueComponent = new JLabel(value != null ? value : "N/A");
    valueComponent.setFont(new Font("Arial", Font.PLAIN, 10));
    linePanel.add(labelComponent);
    linePanel.add(valueComponent);
    panel.add(linePanel);
  }

  private JPanel createRightPanel() {
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setTabPlacement(JTabbedPane.TOP);

    tabbedPane.addTab("General", createGeneralInfoPanel());
    tabbedPane.addTab("Technical", createTechnicalSpecsPanel());
    tabbedPane.addTab("Publishers", createPublishersPanel());
    tabbedPane.addTab("Authors", createAuthorsPanel());
    tabbedPane.addTab("Description", createDescriptionPanel());
    tabbedPane.addTab("Screenshots", createScreenshotsPanel());
    tabbedPane.addTab("Game map", createGameMapsPanel());
    tabbedPane.addTab("Releases", createReleasesPanel());
    tabbedPane.addTab("Downloads", createDownloadsPanel());

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tabbedPane, BorderLayout.CENTER);
    return panel;
  }

  private JPanel createGeneralInfoPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Title section
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(BorderFactory.createTitledBorder("Game Information"));
    JLabel titleLabel = new JLabel("<html><b>" + gameDetail.title + "</b></html>");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
    titlePanel.add(titleLabel, BorderLayout.CENTER);
    titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

    mainPanel.add(titlePanel);
    mainPanel.add(Box.createVerticalStrut(10));

    // Information table
    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.setBorder(BorderFactory.createTitledBorder("Details"));

    String[][] data = {
        {"Title", gameDetail.title},
        {"Year of Release", gameDetail.yearOfRelease != null ? gameDetail.yearOfRelease : "N/A"},
        {"Release Date", formatReleaseDate()},
        {"Genre", gameDetail.genre != null ? gameDetail.genre : "N/A"},
        {"Genre Type", gameDetail.genreType != null ? gameDetail.genreType : "N/A"},
        {"Genre Sub-Type", gameDetail.genreSubType != null ? gameDetail.genreSubType : "N/A"},
        {"Machine Type", gameDetail.machineType != null ? gameDetail.machineType : "N/A"},
        {"Availability", gameDetail.availability != null ? gameDetail.availability : "N/A"},
        {"Game ID", gameDetail.id != null ? gameDetail.id : "N/A"},
        {"Score", gameDetail.score != null ? String.format("%.1f", gameDetail.score) : "N/A"},
        {"ISBN", gameDetail.isbn != null ? gameDetail.isbn : "N/A"},
    };

    String[] columns = {"Property", "Value"};
    DefaultTableModel model = new DefaultTableModel(data, columns) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JTable table = new JTable(model);
    table.setRowHeight(25);
    table.getColumnModel().getColumn(0).setPreferredWidth(150);
    table.getColumnModel().getColumn(1).setPreferredWidth(300);

    JScrollPane scrollPane = new JScrollPane(table);
    infoPanel.add(scrollPane, BorderLayout.CENTER);

    mainPanel.add(infoPanel);
    mainPanel.add(Box.createVerticalGlue());

    return mainPanel;
  }

  private String formatReleaseDate() {
    if (gameDetail.yearOfRelease == null) return "N/A";
    StringBuilder date = new StringBuilder(gameDetail.yearOfRelease);
    if (gameDetail.originalMonthOfRelease != null) {
      date.append("-").append(String.format("%02d", gameDetail.originalMonthOfRelease));
      if (gameDetail.originalDayOfRelease != null) {
        date.append("-").append(String.format("%02d", gameDetail.originalDayOfRelease));
      }
    }
    return date.toString();
  }

  private JPanel createTechnicalSpecsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    String[][] data = {
        {"Machine Type", gameDetail.machineType != null ? gameDetail.machineType : "N/A"},
        {"Memory Required", gameDetail.memoryRequired != null ? gameDetail.memoryRequired : "N/A"},
        {"Content Type", gameDetail.contentType != null ? gameDetail.contentType : "N/A"},
        {"Format", gameDetail.zxinfoVersion != null ? gameDetail.zxinfoVersion : "N/A"},
    };

    if (gameDetail.machines != null && !gameDetail.machines.isEmpty()) {
      JPanel machinesPanel = new JPanel(new BorderLayout());
      machinesPanel.setBorder(BorderFactory.createTitledBorder("Compatible Machines"));

      String[][] machineData = new String[gameDetail.machines.size()][1];
      for (int i = 0; i < gameDetail.machines.size(); i++) {
        machineData[i][0] = gameDetail.machines.get(i);
      }

      DefaultTableModel machineModel = new DefaultTableModel(machineData, new String[]{"Machine"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

      JTable machineTable = new JTable(machineModel);
      machineTable.setRowHeight(20);
      JScrollPane machineScroll = new JScrollPane(machineTable);
      machinesPanel.add(machineScroll, BorderLayout.CENTER);
      machinesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

      panel.add(machinesPanel);
      panel.add(Box.createVerticalStrut(10));
    }

    DefaultTableModel model = new DefaultTableModel(data, new String[]{"Property", "Value"}) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JTable table = new JTable(model);
    table.setRowHeight(25);
    table.getColumnModel().getColumn(0).setPreferredWidth(150);
    table.getColumnModel().getColumn(1).setPreferredWidth(300);

    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.setBorder(BorderFactory.createTitledBorder("Specifications"));
    JScrollPane scrollPane = new JScrollPane(table);
    infoPanel.add(scrollPane, BorderLayout.CENTER);

    panel.add(infoPanel);
    panel.add(Box.createVerticalGlue());

    return panel;
  }

  private JPanel createPublishersPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.publishers == null || gameDetail.publishers.isEmpty()) {
      JLabel noDataLabel = new JLabel("No publisher information available");
      noDataLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noDataLabel, BorderLayout.CENTER);
    } else {
      String[][] data = new String[gameDetail.publishers.size()][1];
      for (int i = 0; i < gameDetail.publishers.size(); i++) {
        data[i][0] = gameDetail.publishers.get(i);
      }

      DefaultTableModel model = new DefaultTableModel(data, new String[]{"Publisher"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

      JTable table = new JTable(model);
      table.setRowHeight(25);
      JScrollPane scrollPane = new JScrollPane(table);
      panel.add(scrollPane, BorderLayout.CENTER);
    }

    return panel;
  }

  private JPanel createAuthorsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.authors == null || gameDetail.authors.isEmpty()) {
      JLabel noDataLabel = new JLabel("No author information available");
      noDataLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noDataLabel, BorderLayout.CENTER);
    } else {
      String[][] data = new String[gameDetail.authors.size()][1];
      for (int i = 0; i < gameDetail.authors.size(); i++) {
        data[i][0] = gameDetail.authors.get(i);
      }

      DefaultTableModel model = new DefaultTableModel(data, new String[]{"Author"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

      JTable table = new JTable(model);
      table.setRowHeight(25);
      JScrollPane scrollPane = new JScrollPane(table);
      panel.add(scrollPane, BorderLayout.CENTER);
    }

    return panel;
  }

  private JPanel createDescriptionPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JTextArea descriptionArea = new JTextArea();
    descriptionArea.setText(gameDetail.description != null ? gameDetail.description : "No description available");
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);
    descriptionArea.setEditable(false);
    descriptionArea.setFont(new Font("Arial", Font.PLAIN, 12));

    JScrollPane scrollPane = new JScrollPane(descriptionArea);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createScreenshotsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.screenshots == null || gameDetail.screenshots.isEmpty()) {
      JLabel noScreenshotsLabel = new JLabel("No screenshots available");
      noScreenshotsLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noScreenshotsLabel, BorderLayout.CENTER);
    } else {
      JPanel screenshotsGridPanel = new JPanel();
      int cols = (int) Math.ceil(Math.sqrt(gameDetail.screenshots.size()));
      screenshotsGridPanel.setLayout(new GridLayout(cols, cols, 10, 10));

      for (String screenshotUrl : gameDetail.screenshots) {
        JPanel screenshotPanel = new JPanel(new BorderLayout());
        screenshotPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel screenshotLabel = new JLabel();
        screenshotLabel.setBackground(Color.BLACK);
        screenshotLabel.setOpaque(true);
        screenshotLabel.setHorizontalAlignment(JLabel.CENTER);
        screenshotLabel.setVerticalAlignment(JLabel.CENTER);
        screenshotLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        screenshotLabel.setPreferredSize(new Dimension(200, 150));

        // Load screenshot asynchronously with complete URL
        if (screenshotUrl != null && !screenshotUrl.isEmpty()) {
          String completeUrl = buildCompleteImageUrl(screenshotUrl);
          loadImageAsync(completeUrl, screenshotLabel, 200, 150, "Loading...");
        } else {
          screenshotLabel.setText("No URL");
          screenshotLabel.setForeground(Color.WHITE);
        }

        screenshotPanel.add(screenshotLabel, BorderLayout.CENTER);
        screenshotsGridPanel.add(screenshotPanel);
      }

      JScrollPane scrollPane = new JScrollPane(screenshotsGridPanel);
      panel.add(scrollPane, BorderLayout.CENTER);
    }

    return panel;
  }

  /**
   * Game maps come from additionalDownloads with type "Game map": scanned or fan-drawn
   * images (JPG/PNG), often several MB. Thumbnails are shown here; double-clicking one
   * opens it full size in the download viewer.
   */
  private JPanel createGameMapsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.gameMaps == null || gameDetail.gameMaps.isEmpty()) {
      JLabel noMapsLabel = new JLabel("No game map available for this entry");
      noMapsLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noMapsLabel, BorderLayout.CENTER);
      return panel;
    }

    JPanel mapsGridPanel = new JPanel();
    int cols = Math.max(1, (int) Math.ceil(Math.sqrt(gameDetail.gameMaps.size())));
    mapsGridPanel.setLayout(new GridLayout(0, cols, 10, 10));

    for (com.fpetrola.oozx.api.AdditionalDownload map : gameDetail.gameMaps) {
      final String mapPath = map.path;

      JPanel mapPanel = new JPanel(new BorderLayout());
      mapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

      JLabel mapLabel = new JLabel();
      mapLabel.setBackground(Color.BLACK);
      mapLabel.setOpaque(true);
      mapLabel.setHorizontalAlignment(JLabel.CENTER);
      mapLabel.setVerticalAlignment(JLabel.CENTER);
      mapLabel.setFont(new Font("Arial", Font.PLAIN, 10));
      mapLabel.setPreferredSize(new Dimension(320, 240));

      if (mapPath != null && !mapPath.isEmpty()) {
        loadScaledImageAsync(buildCompleteImageUrl(mapPath), mapLabel, 320, 240, "Map unavailable");
      } else {
        mapLabel.setText("No URL");
        mapLabel.setForeground(Color.WHITE);
      }

      StringBuilder caption = new StringBuilder();
      caption.append(map.format != null ? map.format : "Picture");
      if (map.size != null) {
        caption.append(" - ").append(map.size / 1024).append(" KB");
      }
      caption.append(" (double-click to open)");
      JLabel captionLabel = new JLabel(caption.toString(), JLabel.CENTER);
      captionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
      captionLabel.setToolTipText(mapPath);

      mapPanel.add(mapLabel, BorderLayout.CENTER);
      mapPanel.add(captionLabel, BorderLayout.SOUTH);

      mapPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      mapLabel.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
          if (evt.getClickCount() == 2) {
            openDownloadUrl(mapPath);
          }
        }
      });

      mapsGridPanel.add(mapPanel);
    }

    panel.add(new JScrollPane(mapsGridPanel), BorderLayout.CENTER);
    return panel;
  }

  /**
   * Same as loadImageAsync but fits the image inside the given box instead of stretching it.
   * Game maps are wide, so a fixed-size scale would distort them badly.
   */
  private void loadScaledImageAsync(String imageUrl, JLabel label, int maxWidth, int maxHeight, String fallbackText) {
    label.setText("Loading...");
    label.setForeground(Color.WHITE);

    SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
      @Override
      protected ImageIcon doInBackground() throws Exception {
        try {
          ImageIcon icon = new ImageIcon(new URL(imageUrl));
          int w = icon.getIconWidth();
          int h = icon.getIconHeight();
          if (w <= 0 || h <= 0) {
            return null;
          }
          double scale = Math.min(maxWidth / (double) w, maxHeight / (double) h);
          if (scale > 1.0) {
            scale = 1.0;
          }
          int targetWidth = Math.max(1, (int) Math.round(w * scale));
          int targetHeight = Math.max(1, (int) Math.round(h * scale));
          return new ImageIcon(icon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH));
        } catch (Exception e) {
          return null;
        }
      }

      @Override
      protected void done() {
        try {
          ImageIcon icon = get();
          if (icon != null) {
            label.setIcon(icon);
            label.setText(null);
          } else {
            label.setText(fallbackText);
          }
        } catch (Exception e) {
          label.setText(fallbackText);
        }
      }
    };
    worker.execute();
  }

  private JPanel createReleasesPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.releases == null || gameDetail.releases.isEmpty()) {
      JLabel noReleasesLabel = new JLabel("No release information available");
      noReleasesLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noReleasesLabel, BorderLayout.CENTER);
    } else {
      String[][] data = new String[gameDetail.releases.size()][gameDetail.releases.get(0).size()];
      String[] columns = gameDetail.releases.get(0).keySet().toArray(new String[0]);

      for (int i = 0; i < gameDetail.releases.size(); i++) {
        int j = 0;
        for (String value : gameDetail.releases.get(i).values()) {
          data[i][j++] = value;
        }
      }

      DefaultTableModel model = new DefaultTableModel(data, columns) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

      JTable table = new JTable(model);
      table.setRowHeight(25);
      for (int i = 0; i < columns.length; i++) {
        table.getColumnModel().getColumn(i).setPreferredWidth(150);
      }

      JScrollPane scrollPane = new JScrollPane(table);
      panel.add(scrollPane, BorderLayout.CENTER);
    }

    return panel;
  }

  private JPanel createDownloadsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    if (gameDetail.additionalDownloads == null || gameDetail.additionalDownloads.isEmpty()) {
      JLabel noDownloadsLabel = new JLabel("No additional downloads available");
      noDownloadsLabel.setHorizontalAlignment(JLabel.CENTER);
      panel.add(noDownloadsLabel, BorderLayout.CENTER);
    } else {
      // Prepare data for table with multiple columns
      String[][] data = new String[gameDetail.additionalDownloads.size()][5];
      String[] columns = {"Path", "Type", "Format", "Size", "Language"};
      
      for (int i = 0; i < gameDetail.additionalDownloads.size(); i++) {
        com.fpetrola.oozx.api.AdditionalDownload download = gameDetail.additionalDownloads.get(i);
        data[i][0] = download.path != null ? download.path : "";
        data[i][1] = download.type != null ? download.type : "";
        data[i][2] = download.format != null ? download.format : "";
        data[i][3] = download.size != null ? String.valueOf(download.size) : "";
        data[i][4] = download.language != null ? download.language : "";
      }

      DefaultTableModel model = new DefaultTableModel(data, columns) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

      JTable table = new JTable(model);
      table.setRowHeight(25);
      table.getColumnModel().getColumn(0).setPreferredWidth(300);
      table.getColumnModel().getColumn(1).setPreferredWidth(100);
      table.getColumnModel().getColumn(2).setPreferredWidth(100);
      table.getColumnModel().getColumn(3).setPreferredWidth(80);
      table.getColumnModel().getColumn(4).setPreferredWidth(80);

      // Add double-click listener to open downloads
      table.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
          if (evt.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row >= 0) {
              String downloadUrl = (String) table.getValueAt(row, 0);
              openDownloadUrl(downloadUrl);
            }
          }
        }
      });

      JScrollPane scrollPane = new JScrollPane(table);
      panel.add(scrollPane, BorderLayout.CENTER);
    }

    return panel;
  }

  /**
   * Loads an image from URL asynchronously and sets it on a JLabel
   */
  private void loadImageAsync(String imageUrl, JLabel label, int width, int height, String fallbackText) {
    SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
      @Override
      protected ImageIcon doInBackground() throws Exception {
        try {
          URL url = new URL(imageUrl);
          ImageIcon icon = new ImageIcon(url);
          if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
          }
          return null;
        } catch (Exception e) {
          return null;
        }
      }

      @Override
      protected void done() {
        try {
          ImageIcon icon = get();
          if (icon != null) {
            label.setIcon(icon);
            label.setText(null);
          } else {
            label.setText(fallbackText);
            label.setForeground(Color.WHITE);
          }
        } catch (Exception e) {
          label.setText(fallbackText);
          label.setForeground(Color.WHITE);
        }
      }
    };
    worker.execute();
  }

  /**
   * Play game with selected options
   */
  private void playGame(String model, double speed, boolean muted, boolean turbo) {
    JOptionPane.showMessageDialog(this,
        "Loading: " + gameDetail.title + "\n" +
        "Model: " + model + "\n" +
        "Speed: " + String.format("%.1fx", speed) + "\n" +
        "Muted: " + muted + "\n" +
        "Turbo: " + turbo,
        "Game Loaded", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Open the ZXInfo page for this game
   */
  private void openZXInfoLink() {
    try {
      String zxinfoUrl = "https://zxinfo.dk/details/" + gameDetail.id;
      Desktop.getDesktop().browse(new java.net.URI(zxinfoUrl));
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Unable to open link: " + e.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Search for download links (opens search engine or zxinfo)
   */
  private void openDownloadLink() {
    try {
      String searchUrl = "https://zxinfo.dk/search?q=" +
                         java.net.URLEncoder.encode(gameDetail.title, "UTF-8");
      Desktop.getDesktop().browse(new java.net.URI(searchUrl));
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Unable to open download link: " + e.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Open a download URL or file in a Swing viewer dialog
   */
  private void openDownloadUrl(String downloadUrl) {
      try {
          if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
              return;
          }
          
          // Convert relative paths to full URLs
          String fullUrl = GameBrowserInternalFrame.getFileURL(downloadUrl);
          
          // Extract filename from URL
          String fileName = downloadUrl;
          int lastSlash = downloadUrl.lastIndexOf('/');
          if (lastSlash >= 0) {
              fileName = downloadUrl.substring(lastSlash + 1);
          }
          
          // Open in Swing viewer dialog
          Frame owner = (Frame) getOwner();
          if (owner == null) {
              owner = new Frame();
          }
          
          DownloadViewerDialog viewer = new DownloadViewerDialog(owner, fullUrl, fileName);
          viewer.setVisible(true);
      } catch (Exception e) {
          JOptionPane.showMessageDialog(this,
              "Unable to open: " + e.getMessage(),
              "Error", JOptionPane.ERROR_MESSAGE);
      }
  }

  private JPanel createButtonsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

    // Top row: Emulation options
    JPanel optionsPanel = new JPanel();
    optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS));
    optionsPanel.setBorder(BorderFactory.createTitledBorder("Emulation Options"));
    optionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

    JLabel modelLabel = new JLabel("Model:");
    String[] models = {"Spectrum 48K", "Spectrum 128K", "Spectrum +3"};
    JComboBox<String> modelCombo = new JComboBox<>(models);
    modelCombo.setSelectedIndex(0);
    modelCombo.setPreferredSize(new Dimension(120, 25));
    modelCombo.setMaximumSize(new Dimension(120, 25));

    JLabel speedLabel = new JLabel("Speed:");
    SpinnerModel speedModel = new SpinnerNumberModel(1.0, 0.5, 4.0, 0.5);
    JSpinner speedSpinner = new JSpinner(speedModel);
    speedSpinner.setPreferredSize(new Dimension(80, 25));
    speedSpinner.setMaximumSize(new Dimension(80, 25));

    optionsPanel.add(modelLabel);
    optionsPanel.add(Box.createHorizontalStrut(5));
    optionsPanel.add(modelCombo);
    optionsPanel.add(Box.createHorizontalStrut(15));
    optionsPanel.add(speedLabel);
    optionsPanel.add(Box.createHorizontalStrut(5));
    optionsPanel.add(speedSpinner);
    optionsPanel.add(Box.createHorizontalGlue());

    // Second row: Feature checkboxes (read-only)
    JPanel featuresPanel = new JPanel();
    featuresPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    featuresPanel.setBorder(BorderFactory.createTitledBorder("Features"));
    featuresPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

    JCheckBox muteCheckBox = new JCheckBox("Mute Sound");
    muteCheckBox.setEnabled(true);
    JCheckBox turboCheckBox = new JCheckBox("Turbo Mode");
    turboCheckBox.setEnabled(true);
    JCheckBox fullscreenCheckBox = new JCheckBox("Fullscreen");
    fullscreenCheckBox.setEnabled(true);

    featuresPanel.add(muteCheckBox);
    featuresPanel.add(turboCheckBox);
    featuresPanel.add(fullscreenCheckBox);

    // Third row: Action buttons
    JPanel buttonsActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
    buttonsActionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

    JButton playButton = new JButton("▶ Play Game");
    playButton.setFont(new Font("Arial", Font.BOLD, 12));
    playButton.addActionListener(e -> playGame(
        (String) modelCombo.getSelectedItem(),
        ((Number) speedSpinner.getValue()).doubleValue(),
        muteCheckBox.isSelected(),
        turboCheckBox.isSelected()
    ));

    JButton downloadButton = new JButton("↓ Download");
    downloadButton.addActionListener(e -> openDownloadLink());

    JButton openLinkButton = new JButton("🌐 Open Link");
    openLinkButton.addActionListener(e -> openZXInfoLink());

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());

    buttonsActionPanel.add(playButton);
    buttonsActionPanel.add(downloadButton);
    buttonsActionPanel.add(openLinkButton);
    buttonsActionPanel.add(closeButton);

    // Assemble panel
    panel.add(optionsPanel);
    panel.add(featuresPanel);
    panel.add(buttonsActionPanel);

    return panel;
  }
}
