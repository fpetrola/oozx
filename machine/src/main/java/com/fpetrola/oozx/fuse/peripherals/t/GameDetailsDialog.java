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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class GameDetailsDialog extends JDialog {
  private GameSearchResult game;
  private OnGameLoadedListener onGameLoadedListener;

  public interface OnGameLoadedListener {
    void onGameLoaded(GameSearchResult game);
  }

  public GameDetailsDialog(Frame owner, GameSearchResult game) {
    super(owner, "Game Details - " + game.getTitle(), true);
    this.game = game;
    setSize(900, 750);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    // Create main panel with tabs
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));
    
    tabbedPane.addTab("Overview", createOverviewPanel());
    tabbedPane.addTab("Metadata", createMetadataPanel());
    tabbedPane.addTab("Media", createMediaPanel());
    tabbedPane.addTab("Tags & Ratings", createTagsRatingsPanel());
    tabbedPane.addTab("Details Tree", createDetailsTreePanel());
    tabbedPane.addTab("Comparison", createComparisonPanel());
    
    add(tabbedPane, BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
  }

  public void setOnGameLoadedListener(OnGameLoadedListener listener) {
    this.onGameLoadedListener = listener;
  }

  private JPanel createOverviewPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout(10, 10));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Title and description section
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    
    JLabel titleLabel = new JLabel(game.getTitle());
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    infoPanel.add(titleLabel);
    
    JLabel urlLabel = new JLabel("URL: " + (game.getUrl() != null ? game.getUrl() : "N/A"));
    urlLabel.setFont(new Font("Arial", Font.PLAIN, 10));
    urlLabel.setForeground(Color.GRAY);
    infoPanel.add(urlLabel);
    
    infoPanel.add(Box.createVerticalStrut(10));
    
    if (game.getDescription() != null) {
      JLabel descTitle = new JLabel("Description:");
      descTitle.setFont(new Font("Arial", Font.BOLD, 12));
      infoPanel.add(descTitle);
      
      JTextArea descArea = new JTextArea(game.getDescription());
      descArea.setEditable(false);
      descArea.setLineWrap(true);
      descArea.setWrapStyleWord(true);
      descArea.setBackground(UIManager.getColor("Panel.background"));
      descArea.setBorder(new LineBorder(Color.LIGHT_GRAY));
      descArea.setFont(new Font("Arial", Font.PLAIN, 11));
      
      JScrollPane scrollPane = new JScrollPane(descArea);
      scrollPane.setPreferredSize(new Dimension(0, 150));
      infoPanel.add(scrollPane);
    }
    
    panel.add(infoPanel, BorderLayout.CENTER);
    
    return panel;
  }

  private JPanel createMetadataPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    
    // Create two columns layout
    JPanel leftColumn = new JPanel();
    leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
    leftColumn.setBorder(new TitledBorder("Game Information"));
    
    leftColumn.add(createMetadataField("Genre:", game.getGenre()));
    leftColumn.add(createMetadataField("Developer:", game.getDeveloper()));
    leftColumn.add(createMetadataField("Publisher:", game.getPublisher()));
    leftColumn.add(createMetadataField("Release Year:", game.getReleaseYear()));
    
    JPanel rightColumn = new JPanel();
    rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
    rightColumn.setBorder(new TitledBorder("Gameplay"));
    
    rightColumn.add(createMetadataField("Platform:", game.getPlatform()));
    rightColumn.add(createMetadataField("Players:", game.getPlayers()));
    rightColumn.add(createMetadataField("Difficulty:", game.getDifficulty()));
    rightColumn.add(createMetadataField("Filename:", game.getFilename()));
    
    // Two-column layout
    JPanel twoColumnPanel = new JPanel();
    twoColumnPanel.setLayout(new GridLayout(1, 2, 10, 10));
    twoColumnPanel.add(leftColumn);
    twoColumnPanel.add(rightColumn);
    
    panel.add(twoColumnPanel);
    panel.add(Box.createVerticalGlue());
    
    return panel;
  }

  private JPanel createMediaPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    JLabel title = new JLabel("Screenshots");
    title.setFont(new Font("Arial", Font.BOLD, 14));
    panel.add(title);
    panel.add(Box.createVerticalStrut(10));
    
    // Screenshot grid
    JPanel screenshotPanel = new JPanel();
    screenshotPanel.setLayout(new GridLayout(1, 2, 10, 10));
    
    if (game.getScreenshot1() != null && !game.getScreenshot1().isEmpty()) {
      JPanel imagePanel1 = createImagePanel("Screenshot 1", game.getScreenshot1());
      screenshotPanel.add(imagePanel1);
    } else {
      JLabel placeholder1 = new JLabel("No Screenshot 1");
      placeholder1.setHorizontalAlignment(JLabel.CENTER);
      screenshotPanel.add(placeholder1);
    }
    
    if (game.getScreenshot2() != null && !game.getScreenshot2().isEmpty()) {
      JPanel imagePanel2 = createImagePanel("Screenshot 2", game.getScreenshot2());
      screenshotPanel.add(imagePanel2);
    } else {
      JLabel placeholder2 = new JLabel("No Screenshot 2");
      placeholder2.setHorizontalAlignment(JLabel.CENTER);
      screenshotPanel.add(placeholder2);
    }
    
    panel.add(screenshotPanel);
    panel.add(Box.createVerticalGlue());
    
    return panel;
  }

  private JPanel createTagsRatingsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    // Rating section
    JPanel ratingPanel = new JPanel();
    ratingPanel.setBorder(new TitledBorder("Rating"));
    ratingPanel.setLayout(new BoxLayout(ratingPanel, BoxLayout.X_AXIS));
    
    ButtonGroup ratingGroup = new ButtonGroup();
    for (int i = 1; i <= 5; i++) {
      JRadioButton rButton = new JRadioButton(String.valueOf(i) + " ★");
      rButton.setSelected(game.getRating() == i);
      rButton.setEnabled(false);
      ratingGroup.add(rButton);
      ratingPanel.add(rButton);
    }
    
    panel.add(ratingPanel);
    panel.add(Box.createVerticalStrut(15));
    
    // Tags section
    JLabel tagsTitle = new JLabel("Tags:");
    tagsTitle.setFont(new Font("Arial", Font.BOLD, 12));
    panel.add(tagsTitle);
    
    JPanel tagsPanel = new JPanel();
    tagsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    tagsPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));
    
    if (game.getTags() != null && !game.getTags().isEmpty()) {
      for (String tag : game.getTags()) {
        JCheckBox tagCheck = new JCheckBox(tag);
        tagCheck.setSelected(true);
        tagCheck.setEnabled(false);
        tagsPanel.add(tagCheck);
      }
    } else {
      JLabel noTags = new JLabel("No tags");
      noTags.setForeground(Color.GRAY);
      tagsPanel.add(noTags);
    }
    
    JScrollPane tagsScroll = new JScrollPane(tagsPanel);
    tagsScroll.setPreferredSize(new Dimension(0, 80));
    panel.add(tagsScroll);
    
    // Favorite checkbox
    panel.add(Box.createVerticalStrut(15));
    JCheckBox favoriteCheck = new JCheckBox("Mark as favorite");
    favoriteCheck.setSelected(game.isFavorite());
    panel.add(favoriteCheck);
    
    panel.add(Box.createVerticalGlue());
    
    return panel;
  }

  private JPanel createMetadataField(String label, String value) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout(5, 5));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    
    JLabel labelComponent = new JLabel(label);
    labelComponent.setFont(new Font("Arial", Font.BOLD, 11));
    panel.add(labelComponent, BorderLayout.WEST);
    
    JLabel valueComponent = new JLabel(value != null && !value.isEmpty() ? value : "N/A");
    valueComponent.setForeground(value != null && !value.isEmpty() ? Color.BLACK : Color.GRAY);
    panel.add(valueComponent, BorderLayout.CENTER);
    
    return panel;
  }

  private JPanel createImagePanel(String title, String imagePath) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBorder(new TitledBorder(title));
    
    JLabel imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(JLabel.CENTER);
    imageLabel.setPreferredSize(new Dimension(300, 200));
    
    // Try to load image
    try {
      ImageIcon icon = new ImageIcon(imagePath);
      if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
        Image scaledImage = icon.getImage().getScaledInstance(300, 200, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
      } else {
        imageLabel.setText("Image not found");
      }
    } catch (Exception e) {
      imageLabel.setText("Error loading image");
      imageLabel.setForeground(Color.RED);
    }
    
    panel.add(imageLabel, BorderLayout.CENTER);
    
    return panel;
  }

  private JPanel createComparisonPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout(10, 10));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    // Create table with game properties
    String[] columnNames = {"Property", "Value"};
    Object[][] data = {
        {"Title", game.getTitle()},
        {"Genre", game.getGenre() != null ? game.getGenre() : "N/A"},
        {"Developer", game.getDeveloper() != null ? game.getDeveloper() : "N/A"},
        {"Publisher", game.getPublisher() != null ? game.getPublisher() : "N/A"},
        {"Release Year", game.getReleaseYear() != null ? game.getReleaseYear() : "N/A"},
        {"Platform", game.getPlatform() != null ? game.getPlatform() : "N/A"},
        {"Players", game.getPlayers() != null ? game.getPlayers() : "N/A"},
        {"Difficulty", game.getDifficulty() != null ? game.getDifficulty() : "N/A"},
        {"Rating", game.getRating() + "/5 ⭐"},
        {"Filename", game.getFilename() != null ? game.getFilename() : "N/A"},
        {"Favorite", game.isFavorite() ? "Yes" : "No"},
    };
    
    JTable table = new JTable(data, columnNames);
    table.setEnabled(false);
    table.setRowHeight(25);
    table.getColumnModel().getColumn(0).setPreferredWidth(150);
    table.getColumnModel().getColumn(1).setPreferredWidth(300);
    
    JScrollPane scrollPane = new JScrollPane(table);
    panel.add(scrollPane, BorderLayout.CENTER);
    
    // Add a list of tags below
    JPanel tagsPanel = new JPanel();
    tagsPanel.setBorder(new TitledBorder("Associated Tags"));
    tagsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    
    if (game.getTags() != null && !game.getTags().isEmpty()) {
      for (String tag : game.getTags()) {
        JLabel tagLabel = new JLabel("• " + tag);
        tagLabel.setBackground(new Color(230, 240, 250));
        tagLabel.setOpaque(true);
        tagLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        tagsPanel.add(tagLabel);
      }
    } else {
      JLabel noTags = new JLabel("No tags available");
      noTags.setForeground(Color.GRAY);
      tagsPanel.add(noTags);
    }
    
    panel.add(tagsPanel, BorderLayout.SOUTH);
    
    return panel;
  }

  private JPanel createDetailsTreePanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout(10, 10));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    // Create tree structure
    javax.swing.tree.DefaultMutableTreeNode root = new javax.swing.tree.DefaultMutableTreeNode("Game Details");
    
    // Game Info branch
    javax.swing.tree.DefaultMutableTreeNode gameInfo = new javax.swing.tree.DefaultMutableTreeNode("Game Information");
    gameInfo.add(new javax.swing.tree.DefaultMutableTreeNode("Title: " + game.getTitle()));
    gameInfo.add(new javax.swing.tree.DefaultMutableTreeNode("Genre: " + (game.getGenre() != null ? game.getGenre() : "N/A")));
    gameInfo.add(new javax.swing.tree.DefaultMutableTreeNode("Developer: " + (game.getDeveloper() != null ? game.getDeveloper() : "N/A")));
    gameInfo.add(new javax.swing.tree.DefaultMutableTreeNode("Publisher: " + (game.getPublisher() != null ? game.getPublisher() : "N/A")));
    gameInfo.add(new javax.swing.tree.DefaultMutableTreeNode("Release Year: " + (game.getReleaseYear() != null ? game.getReleaseYear() : "N/A")));
    root.add(gameInfo);
    
    // Technical branch
    javax.swing.tree.DefaultMutableTreeNode technical = new javax.swing.tree.DefaultMutableTreeNode("Technical Details");
    technical.add(new javax.swing.tree.DefaultMutableTreeNode("Platform: " + (game.getPlatform() != null ? game.getPlatform() : "N/A")));
    technical.add(new javax.swing.tree.DefaultMutableTreeNode("Filename: " + (game.getFilename() != null ? game.getFilename() : "N/A")));
    technical.add(new javax.swing.tree.DefaultMutableTreeNode("URL: " + (game.getUrl() != null ? game.getUrl() : "N/A")));
    root.add(technical);
    
    // Gameplay branch
    javax.swing.tree.DefaultMutableTreeNode gameplay = new javax.swing.tree.DefaultMutableTreeNode("Gameplay");
    gameplay.add(new javax.swing.tree.DefaultMutableTreeNode("Players: " + (game.getPlayers() != null ? game.getPlayers() : "N/A")));
    gameplay.add(new javax.swing.tree.DefaultMutableTreeNode("Difficulty: " + (game.getDifficulty() != null ? game.getDifficulty() : "N/A")));
    gameplay.add(new javax.swing.tree.DefaultMutableTreeNode("Rating: " + game.getRating() + "/5"));
    root.add(gameplay);
    
    // Tags branch
    if (game.getTags() != null && !game.getTags().isEmpty()) {
      javax.swing.tree.DefaultMutableTreeNode tags = new javax.swing.tree.DefaultMutableTreeNode("Tags (" + game.getTags().size() + ")");
      for (String tag : game.getTags()) {
        tags.add(new javax.swing.tree.DefaultMutableTreeNode(tag));
      }
      root.add(tags);
    }
    
    // Media branch
    javax.swing.tree.DefaultMutableTreeNode media = new javax.swing.tree.DefaultMutableTreeNode("Media");
    media.add(new javax.swing.tree.DefaultMutableTreeNode("Screenshot 1: " + (game.getScreenshot1() != null && !game.getScreenshot1().isEmpty() ? "Available" : "N/A")));
    media.add(new javax.swing.tree.DefaultMutableTreeNode("Screenshot 2: " + (game.getScreenshot2() != null && !game.getScreenshot2().isEmpty() ? "Available" : "N/A")));
    root.add(media);
    
    JTree tree = new JTree(root);
    tree.setEditable(false);
    
    // Expand all nodes
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
    
    JScrollPane scrollPane = new JScrollPane(tree);
    panel.add(scrollPane, BorderLayout.CENTER);
    
    return panel;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());
    panel.add(closeButton);
    
    JButton downloadButton = new JButton("Download");
    downloadButton.setEnabled(false); // Placeholder
    panel.add(downloadButton);
    
    JButton loadButton = new JButton("Load Game");
    loadButton.addActionListener(e -> {
      if (onGameLoadedListener != null) {
        onGameLoadedListener.onGameLoaded(game);
      }
      dispose();
    });
    panel.add(loadButton);
    
    return panel;
  }
}
