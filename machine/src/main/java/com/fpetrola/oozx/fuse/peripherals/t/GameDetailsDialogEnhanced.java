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
import com.fpetrola.oozx.api.Author;
import com.fpetrola.oozx.api.Publisher;
import com.fpetrola.oozx.api.Release;
import com.fpetrola.oozx.api.Screen;
import com.fpetrola.oozx.api.GameFile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameDetailsDialogEnhanced extends JDialog {
    private GameEntry gameEntry;
    
    public GameDetailsDialogEnhanced(Frame owner, GameEntry gameEntry) {
        super(owner, "Game Details - " + gameEntry.title, true);
        this.gameEntry = gameEntry;
        
        setSize(1100, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Left side: Game cover/image
        JPanel leftPanel = createLeftPanel();
        
        // Right side: Tabs with information
        JPanel rightPanel = createRightPanel();
        
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        
        // Bottom: Buttons
        JPanel buttonsPanel = createButtonsPanel();
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(280, 600));
        
        // Game cover image
        JPanel coverPanel = new JPanel(new BorderLayout());
        coverPanel.setBorder(BorderFactory.createTitledBorder("Game Cover"));
        JLabel coverLabel = new JLabel();
        coverLabel.setIcon(null);
        coverLabel.setBackground(Color.DARK_GRAY);
        coverLabel.setOpaque(true);
        coverLabel.setHorizontalAlignment(JLabel.CENTER);
        coverLabel.setVerticalAlignment(JLabel.CENTER);
        coverLabel.setText("(Cover Image)");
        coverLabel.setForeground(Color.WHITE);
        coverLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        coverLabel.setPreferredSize(new Dimension(260, 320));
        coverPanel.add(coverLabel, BorderLayout.CENTER);
        
        // Quick Stats Panel
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Quick Stats"));
        
        // Score
        if (gameEntry.score != null && gameEntry.score.score != null) {
            JLabel scoreLabel = new JLabel("Rating: " + String.format("%.1f", gameEntry.score.score) + "/10");
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 11));
            statsPanel.add(scoreLabel);
            
            JProgressBar scoreBar = new JProgressBar(0, 100);
            int scoreValue = (int)(gameEntry.score.score * 10);
            scoreBar.setValue(scoreValue);
            scoreBar.setStringPainted(true);
            scoreBar.setString(String.format("%.1f", gameEntry.score.score) + "/10");
            statsPanel.add(scoreBar);
        }
        
        statsPanel.add(Box.createVerticalStrut(5));
        
        // Release date
        JLabel yearLabel = new JLabel("Year: " + (gameEntry.originalYearOfRelease != null ? gameEntry.originalYearOfRelease : "N/A"));
        statsPanel.add(yearLabel);
        
        // Genre
        JLabel genreLabel = new JLabel("Genre: " + (gameEntry.genre != null ? gameEntry.genre : "N/A"));
        statsPanel.add(genreLabel);
        
        // Machine Type
        JLabel machineLabel = new JLabel("Machine: " + (gameEntry.machineType != null ? gameEntry.machineType : "N/A"));
        statsPanel.add(machineLabel);
        
        // Content Rating
        if (gameEntry.xrated > 0) {
            JLabel xratedLabel = new JLabel("⚠ X-Rated Content");
            xratedLabel.setForeground(Color.RED);
            statsPanel.add(xratedLabel);
        }
        
        // Assemble left panel
        panel.add(coverPanel, BorderLayout.NORTH);
        
        JPanel bottomLeftPanel = new JPanel();
        bottomLeftPanel.setLayout(new BoxLayout(bottomLeftPanel, BoxLayout.Y_AXIS));
        bottomLeftPanel.add(statsPanel);
        bottomLeftPanel.add(Box.createVerticalGlue());
        
        panel.add(bottomLeftPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        
        // Tab 1: General Info
        tabbedPane.addTab("General", createGeneralInfoPanel());
        
        // Tab 2: Authors/Creators
        if (gameEntry.authors != null && !gameEntry.authors.isEmpty()) {
            tabbedPane.addTab("Authors", createAuthorsPanel());
        }
        
        // Tab 3: Publishers
        if (gameEntry.publishers != null && !gameEntry.publishers.isEmpty()) {
            tabbedPane.addTab("Publishers", createPublishersPanel());
        }
        
        // Tab 4: Releases/Files
        if (gameEntry.releases != null && !gameEntry.releases.isEmpty()) {
            tabbedPane.addTab("Releases", createReleasesPanel());
        }
        
        // Tab 5: Screenshots
        if (gameEntry.screens != null && !gameEntry.screens.isEmpty()) {
            tabbedPane.addTab("Screenshots", createScreenshotsPanel());
        }
        
        // Tab 6: Downloads
        if (gameEntry.additionalDownloads != null && !gameEntry.additionalDownloads.isEmpty()) {
            tabbedPane.addTab("Downloads", createDownloadsPanel());
        }
        
        // Tab 7: Details Tree
        tabbedPane.addTab("Details", createDetailsTreePanel());
        
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
        JLabel titleLabel = new JLabel(gameEntry.title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Information grid
        JPanel infoPanel = new JPanel();
        GroupLayout layout = new GroupLayout(infoPanel);
        infoPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        JLabel yearLabel = new JLabel("Year:");
        JLabel yearValue = new JLabel(formatDate());
        
        JLabel genreLabel = new JLabel("Genre:");
        JLabel genreValue = new JLabel(gameEntry.genre != null ? gameEntry.genre : "N/A");
        
        JLabel genreTypeLabel = new JLabel("Type:");
        JLabel genreTypeValue = new JLabel(gameEntry.genreSubType != null ? gameEntry.genreSubType : "N/A");
        
        JLabel machineLabel = new JLabel("Machine:");
        JLabel machineValue = new JLabel(gameEntry.machineType != null ? gameEntry.machineType : "N/A");
        
        JLabel isbnLabel = new JLabel("ISBN:");
        JLabel isbnValue = new JLabel(gameEntry.isbn != null ? gameEntry.isbn : "N/A");
        
        JLabel availabilityLabel = new JLabel("Availability:");
        JLabel availabilityValue = new JLabel(gameEntry.availability != null ? gameEntry.availability : "N/A");
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(yearLabel)
                .addComponent(genreLabel)
                .addComponent(genreTypeLabel)
                .addComponent(machineLabel)
                .addComponent(isbnLabel)
                .addComponent(availabilityLabel))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(yearValue)
                .addComponent(genreValue)
                .addComponent(genreTypeValue)
                .addComponent(machineValue)
                .addComponent(isbnValue)
                .addComponent(availabilityValue))
        );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(yearLabel)
                .addComponent(yearValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(genreLabel)
                .addComponent(genreValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(genreTypeLabel)
                .addComponent(genreTypeValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(machineLabel)
                .addComponent(machineValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(isbnLabel)
                .addComponent(isbnValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(availabilityLabel)
                .addComponent(availabilityValue))
        );
        
        mainPanel.add(titlePanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createVerticalGlue());
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(mainPanel, BorderLayout.NORTH);
        return wrapper;
    }
    
    private JPanel createAuthorsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnNames = {"Name", "Type", "Country", "Role"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        if (gameEntry.authors != null) {
            for (Author author : gameEntry.authors) {
                String role = author.roles != null && !author.roles.isEmpty() 
                    ? author.roles.get(0).roleName 
                    : "N/A";
                model.addRow(new Object[]{
                    author.name != null ? author.name : (author.groupName != null ? author.groupName : "Unknown"),
                    author.type != null ? author.type : "N/A",
                    author.country != null ? author.country : "N/A",
                    role
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPublishersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnNames = {"Publisher", "Country", "Type"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        if (gameEntry.publishers != null) {
            for (Publisher publisher : gameEntry.publishers) {
                model.addRow(new Object[]{
                    publisher.name != null ? publisher.name : "Unknown",
                    publisher.country != null ? publisher.country : "N/A",
                    publisher.labelType != null ? publisher.labelType : "N/A"
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReleasesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnNames = {"Release #", "Publisher", "Files"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        if (gameEntry.releases != null) {
            int releaseNum = 1;
            for (Release release : gameEntry.releases) {
                String publisherName = "N/A";
                if (release.publishers != null && !release.publishers.isEmpty()) {
                    publisherName = release.publishers.get(0).name;
                }
                int fileCount = release.files != null ? release.files.size() : 0;
                
                model.addRow(new Object[]{
                    releaseNum++,
                    publisherName,
                    fileCount + " file(s)"
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createScreenshotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        if (gameEntry.screens == null || gameEntry.screens.isEmpty()) {
            JLabel noScreenshotsLabel = new JLabel("No screenshots available");
            noScreenshotsLabel.setHorizontalAlignment(JLabel.CENTER);
            panel.add(noScreenshotsLabel, BorderLayout.CENTER);
        } else {
            JPanel screenshotsGridPanel = new JPanel();
            int cols = Math.min(3, gameEntry.screens.size());
            int rows = (int) Math.ceil((double) gameEntry.screens.size() / cols);
            screenshotsGridPanel.setLayout(new GridLayout(rows, cols, 10, 10));
            
            for (Screen screen : gameEntry.screens) {
                JPanel screenshotPanel = new JPanel(new BorderLayout());
                screenshotPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                
                JLabel screenshotLabel = new JLabel();
                screenshotLabel.setBackground(Color.BLACK);
                screenshotLabel.setOpaque(true);
                screenshotLabel.setHorizontalAlignment(JLabel.CENTER);
                screenshotLabel.setVerticalAlignment(JLabel.CENTER);
                screenshotLabel.setText(screen.title != null ? screen.title : "(Screenshot)");
                screenshotLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                screenshotLabel.setForeground(Color.WHITE);
                screenshotLabel.setPreferredSize(new Dimension(200, 150));
                
                screenshotPanel.add(screenshotLabel, BorderLayout.CENTER);
                screenshotsGridPanel.add(screenshotPanel);
            }
            
            JScrollPane scrollPane = new JScrollPane(screenshotsGridPanel);
            panel.add(scrollPane, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    private JPanel createDownloadsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnNames = {"Path", "Type", "Format", "Size", "Language"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        if (gameEntry.additionalDownloads != null) {
            for (com.fpetrola.oozx.api.AdditionalDownload download : gameEntry.additionalDownloads) {
                model.addRow(new Object[]{
                    download.path != null ? download.path : "Unknown",
                    download.type != null ? download.type : "N/A",
                    download.format != null ? download.format : "N/A",
                    download.size != null ? download.size : "N/A",
                    download.language != null ? download.language : "N/A"
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDetailsTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create tree structure with game details
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Game Details: " + gameEntry.title);
        
        // General Information
        DefaultMutableTreeNode generalNode = new DefaultMutableTreeNode("General Information");
        generalNode.add(new DefaultMutableTreeNode("Title: " + gameEntry.title));
        generalNode.add(new DefaultMutableTreeNode("Year: " + formatDate()));
        generalNode.add(new DefaultMutableTreeNode("Genre: " + (gameEntry.genre != null ? gameEntry.genre : "N/A")));
        generalNode.add(new DefaultMutableTreeNode("Machine: " + (gameEntry.machineType != null ? gameEntry.machineType : "N/A")));
        generalNode.add(new DefaultMutableTreeNode("Availability: " + (gameEntry.availability != null ? gameEntry.availability : "N/A")));
        if (gameEntry.xrated > 0) {
            generalNode.add(new DefaultMutableTreeNode("⚠ X-Rated Content"));
        }
        root.add(generalNode);
        
        // Authors
        if (gameEntry.authors != null && !gameEntry.authors.isEmpty()) {
            DefaultMutableTreeNode authorsNode = new DefaultMutableTreeNode("Authors (" + gameEntry.authors.size() + ")");
            for (Author author : gameEntry.authors) {
                String authorName = author.name != null ? author.name : (author.groupName != null ? author.groupName : "Unknown");
                DefaultMutableTreeNode authorNode = new DefaultMutableTreeNode(authorName + " (" + (author.type != null ? author.type : "N/A") + ")");
                if (author.country != null) {
                    authorNode.add(new DefaultMutableTreeNode("Country: " + author.country));
                }
                if (author.roles != null && !author.roles.isEmpty()) {
                    authorNode.add(new DefaultMutableTreeNode("Role: " + author.roles.get(0).roleName));
                }
                authorsNode.add(authorNode);
            }
            root.add(authorsNode);
        }
        
        // Publishers
        if (gameEntry.publishers != null && !gameEntry.publishers.isEmpty()) {
            DefaultMutableTreeNode publishersNode = new DefaultMutableTreeNode("Publishers (" + gameEntry.publishers.size() + ")");
            for (Publisher publisher : gameEntry.publishers) {
                DefaultMutableTreeNode pubNode = new DefaultMutableTreeNode(publisher.name != null ? publisher.name : "Unknown");
                if (publisher.country != null) {
                    pubNode.add(new DefaultMutableTreeNode("Country: " + publisher.country));
                }
                publishersNode.add(pubNode);
            }
            root.add(publishersNode);
        }
        
        // Releases
        if (gameEntry.releases != null && !gameEntry.releases.isEmpty()) {
            DefaultMutableTreeNode releasesNode = new DefaultMutableTreeNode("Releases (" + gameEntry.releases.size() + ")");
            int releaseNum = 1;
            for (Release release : gameEntry.releases) {
                int fileCount = release.files != null ? release.files.size() : 0;
                releasesNode.add(new DefaultMutableTreeNode("Release " + releaseNum + " (" + fileCount + " files)"));
                releaseNum++;
            }
            root.add(releasesNode);
        }
        
        // Screenshots
        if (gameEntry.screens != null && !gameEntry.screens.isEmpty()) {
            DefaultMutableTreeNode screenshotsNode = new DefaultMutableTreeNode("Screenshots (" + gameEntry.screens.size() + ")");
            for (int i = 0; i < gameEntry.screens.size(); i++) {
                Screen screen = gameEntry.screens.get(i);
                String screenTitle = screen.title != null ? screen.title : ("Screenshot " + (i + 1));
                screenshotsNode.add(new DefaultMutableTreeNode(screenTitle));
            }
            root.add(screenshotsNode);
        }
        
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        
        JScrollPane scrollPane = new JScrollPane(tree);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Top row: Model and other options
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Emulation Options"));
        
        JLabel modelLabel = new JLabel("Model:");
        String[] models = {"Spectrum 48K", "Spectrum 128K", "Spectrum +3", "Pentagon"};
        JComboBox<String> modelCombo = new JComboBox<>(models);
        modelCombo.setSelectedIndex(0);
        modelCombo.setPreferredSize(new Dimension(120, 25));
        modelCombo.setMaximumSize(new Dimension(120, 25));
        
        JLabel speedLabel = new JLabel("Speed:");
        SpinnerModel speedModel = new javax.swing.SpinnerNumberModel(1.0, 0.5, 4.0, 0.5);
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
        
        // Second row: Checkboxes for features
        JPanel featuresPanel = new JPanel();
        featuresPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        featuresPanel.setBorder(BorderFactory.createTitledBorder("Features"));
        
        JCheckBox muteCheckBox = new JCheckBox("Mute Sound");
        JCheckBox turboCheckBox = new JCheckBox("Turbo Mode");
        JCheckBox fullscreenCheckBox = new JCheckBox("Fullscreen");
        
        featuresPanel.add(muteCheckBox);
        featuresPanel.add(turboCheckBox);
        featuresPanel.add(fullscreenCheckBox);
        
        // Third row: Action buttons
        JPanel buttonsActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        JButton playButton = new JButton("▶ Play Game");
        playButton.setFont(new Font("Arial", Font.BOLD, 12));
        playButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Play functionality to be implemented", "Info", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton downloadButton = new JButton("↓ Download");
        downloadButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Download functionality to be implemented", "Info", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton openLinkButton = new JButton("🌐 ZxInfo Link");
        openLinkButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Would open ZxInfo page", "Info", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        buttonsActionPanel.add(playButton);
        buttonsActionPanel.add(downloadButton);
        buttonsActionPanel.add(openLinkButton);
        buttonsActionPanel.add(closeButton);
        
        // Assemble bottom panel
        panel.add(optionsPanel);
        panel.add(featuresPanel);
        panel.add(buttonsActionPanel);
        
        return panel;
    }
    
    private String formatDate() {
        if (gameEntry.originalYearOfRelease != null) {
            String date = gameEntry.originalYearOfRelease.toString();
            if (gameEntry.originalMonthOfRelease != null) {
                date += "/" + String.format("%02d", gameEntry.originalMonthOfRelease);
                if (gameEntry.originalDayOfRelease != null) {
                    date += "/" + String.format("%02d", gameEntry.originalDayOfRelease);
                }
            }
            return date;
        }
        return "N/A";
    }
}
