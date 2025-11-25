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
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameDetailsDialog extends JDialog {
    private GameDetail gameDetail;
    
    public GameDetailsDialog(Frame owner, GameDetail gameDetail) {
        super(owner, "Game Details - " + gameDetail.title, true);
        this.gameDetail = gameDetail;
        
        setSize(900, 600);
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
        panel.setPreferredSize(new Dimension(250, 500));
        
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
        coverLabel.setPreferredSize(new Dimension(230, 300));
        coverPanel.add(coverLabel, BorderLayout.CENTER);
        
        // Rating panel with stars
        JPanel ratingPanel = new JPanel(new BorderLayout());
        ratingPanel.setBorder(BorderFactory.createTitledBorder("Rating"));
        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 5; i++) {
            JLabel star = new JLabel("★");
            star.setFont(new Font("Arial", Font.PLAIN, 18));
            if (i < 3) {
                star.setForeground(Color.YELLOW);
            } else {
                star.setForeground(Color.LIGHT_GRAY);
            }
            starsPanel.add(star);
        }
        ratingPanel.add(starsPanel, BorderLayout.NORTH);
        JLabel ratingLabel = new JLabel("Rating: 3.0/5.0");
        ratingPanel.add(ratingLabel, BorderLayout.CENTER);
        
        // Favorite checkbox
        JCheckBox favoriteCheckBox = new JCheckBox("Mark as Favorite");
        JPanel favoritePanel = new JPanel(new BorderLayout());
        favoritePanel.add(favoriteCheckBox, BorderLayout.NORTH);
        favoritePanel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        // Game type radio buttons
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
        typePanel.setBorder(BorderFactory.createTitledBorder("Game Type"));
        ButtonGroup typeGroup = new ButtonGroup();
        JRadioButton actionRadio = new JRadioButton("Action", true);
        JRadioButton adventureRadio = new JRadioButton("Adventure");
        JRadioButton puzzleRadio = new JRadioButton("Puzzle");
        typeGroup.add(actionRadio);
        typeGroup.add(adventureRadio);
        typeGroup.add(puzzleRadio);
        typePanel.add(actionRadio);
        typePanel.add(adventureRadio);
        typePanel.add(puzzleRadio);
        
        // Assemble left panel
        panel.add(coverPanel, BorderLayout.NORTH);
        panel.add(ratingPanel, BorderLayout.CENTER);
        
        JPanel bottomLeftPanel = new JPanel();
        bottomLeftPanel.setLayout(new BoxLayout(bottomLeftPanel, BoxLayout.Y_AXIS));
        bottomLeftPanel.add(favoritePanel);
        bottomLeftPanel.add(Box.createVerticalStrut(5));
        bottomLeftPanel.add(typePanel);
        
        panel.add(bottomLeftPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        
        // Tab 1: General Info
        tabbedPane.addTab("General", createGeneralInfoPanel());
        
        // Tab 2: Technical Specs
        tabbedPane.addTab("Technical", createTechnicalSpecsPanel());
        
        // Tab 3: Description
        tabbedPane.addTab("Description", createDescriptionPanel());
        
        // Tab 4: Screenshots
        tabbedPane.addTab("Screenshots", createScreenshotsPanel());
        
        // Tab 5: Compatibility
        tabbedPane.addTab("Compatibility", createCompatibilityPanel());
        
        // Tab 6: Details Tree
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
        JLabel titleLabel = new JLabel(gameDetail.title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Information grid
        JPanel infoPanel = new JPanel();
        GroupLayout layout = new GroupLayout(infoPanel);
        infoPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        JLabel yearLabel = new JLabel("Year of Release:");
        JLabel yearValue = new JLabel(gameDetail.yearOfRelease != null ? gameDetail.yearOfRelease : "N/A");
        
        JLabel publisherLabel = new JLabel("Publisher:");
        JLabel publisherValue = new JLabel(gameDetail.publisher != null ? gameDetail.publisher : "N/A");
        
        JLabel genreLabel = new JLabel("Genre:");
        JLabel genreValue = new JLabel(gameDetail.genre != null ? gameDetail.genre : "N/A");
        
        JLabel idLabel = new JLabel("Game ID:");
        JLabel idValue = new JLabel(gameDetail.id != null ? gameDetail.id : "N/A");
        idValue.setFont(new Font("Courier", Font.PLAIN, 10));
        
        // Tags/Labels
        JLabel tagsLabel = new JLabel("Tags:");
        JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] tags = {"Retro", "Action", "Arcade"};
        for (String tag : tags) {
            JLabel tagLabel = new JLabel(tag);
            tagLabel.setBackground(new Color(200, 220, 255));
            tagLabel.setOpaque(true);
            tagLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            tagLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            tagsPanel.add(tagLabel);
        }
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(yearLabel)
                .addComponent(publisherLabel)
                .addComponent(genreLabel)
                .addComponent(idLabel)
                .addComponent(tagsLabel))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(yearValue)
                .addComponent(publisherValue)
                .addComponent(genreValue)
                .addComponent(idValue)
                .addComponent(tagsPanel))
        );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(yearLabel)
                .addComponent(yearValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(publisherLabel)
                .addComponent(publisherValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(genreLabel)
                .addComponent(genreValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(idLabel)
                .addComponent(idValue))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(tagsLabel)
                .addComponent(tagsPanel))
        );
        
        mainPanel.add(titlePanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createVerticalGlue());
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(mainPanel, BorderLayout.NORTH);
        return wrapper;
    }
    
    private JPanel createTechnicalSpecsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create table with technical specs
        String[] columnNames = {"Specification", "Value"};
        Object[][] data = {
            {"Machine Type", gameDetail.machineType != null ? gameDetail.machineType : "N/A"},
            {"Memory Required", gameDetail.memoryRequired != null ? gameDetail.memoryRequired : "N/A"},
            {"ID", gameDetail.id != null ? gameDetail.id : "N/A"}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
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
        panel.add(scrollPane, BorderLayout.CENTER);
        
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
    
    private JPanel createCompatibilityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Compatibility ratings using sliders and checkboxes
        String[] models = {"Spectrum 48K", "Spectrum 128K", "Pentagon", "Timex"};
        
        for (String model : models) {
            JPanel modelPanel = new JPanel();
            modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.X_AXIS));
            modelPanel.setBorder(BorderFactory.createTitledBorder(model));
            
            JCheckBox compatibleCheckBox = new JCheckBox("Compatible");
            compatibleCheckBox.setSelected(true);
            
            JLabel compatLabel = new JLabel("Compatibility:");
            JSlider compatSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 85);
            compatSlider.setMajorTickSpacing(20);
            compatSlider.setMinorTickSpacing(5);
            compatSlider.setPaintTicks(true);
            compatSlider.setPaintLabels(true);
            compatSlider.setPreferredSize(new Dimension(200, 40));
            compatSlider.setMaximumSize(new Dimension(200, 40));
            
            JLabel scoreLabel = new JLabel("85%");
            compatSlider.addChangeListener(e -> scoreLabel.setText(compatSlider.getValue() + "%"));
            
            modelPanel.add(compatibleCheckBox);
            modelPanel.add(Box.createHorizontalStrut(10));
            modelPanel.add(compatLabel);
            modelPanel.add(Box.createHorizontalStrut(5));
            modelPanel.add(compatSlider);
            modelPanel.add(Box.createHorizontalStrut(5));
            modelPanel.add(scoreLabel);
            modelPanel.add(Box.createHorizontalGlue());
            
            panel.add(modelPanel);
            panel.add(Box.createVerticalStrut(5));
        }
        
        // Overall compatibility progress bar
        JPanel overallPanel = new JPanel();
        overallPanel.setLayout(new BoxLayout(overallPanel, BoxLayout.X_AXIS));
        overallPanel.setBorder(BorderFactory.createTitledBorder("Overall Compatibility"));
        
        JLabel overallLabel = new JLabel("Score:");
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(85);
        progressBar.setStringPainted(true);
        progressBar.setString("85%");
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressBar.setMaximumSize(new Dimension(300, 20));
        
        overallPanel.add(overallLabel);
        overallPanel.add(Box.createHorizontalStrut(10));
        overallPanel.add(progressBar);
        overallPanel.add(Box.createHorizontalGlue());
        
        panel.add(overallPanel);
        panel.add(Box.createVerticalGlue());
        
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
            screenshotsGridPanel.setLayout(new GridLayout(2, 2, 10, 10));
            
            for (String screenshotUrl : gameDetail.screenshots) {
                JPanel screenshotPanel = new JPanel(new BorderLayout());
                screenshotPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                
                JLabel screenshotLabel = new JLabel();
                screenshotLabel.setBackground(Color.BLACK);
                screenshotLabel.setOpaque(true);
                screenshotLabel.setHorizontalAlignment(JLabel.CENTER);
                screenshotLabel.setVerticalAlignment(JLabel.CENTER);
                screenshotLabel.setText(screenshotUrl != null ? screenshotUrl : "(Screenshot)");
                screenshotLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                screenshotLabel.setForeground(Color.WHITE);
                
                screenshotPanel.add(screenshotLabel, BorderLayout.CENTER);
                screenshotsGridPanel.add(screenshotPanel);
            }
            
            JScrollPane scrollPane = new JScrollPane(screenshotsGridPanel);
            panel.add(scrollPane, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    private JPanel createDetailsTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create tree structure with game details
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Game Details");
        
        DefaultMutableTreeNode generalNode = new DefaultMutableTreeNode("General Information");
        generalNode.add(new DefaultMutableTreeNode("Title: " + gameDetail.title));
        generalNode.add(new DefaultMutableTreeNode("Year: " + (gameDetail.yearOfRelease != null ? gameDetail.yearOfRelease : "N/A")));
        generalNode.add(new DefaultMutableTreeNode("Publisher: " + (gameDetail.publisher != null ? gameDetail.publisher : "N/A")));
        generalNode.add(new DefaultMutableTreeNode("Genre: " + (gameDetail.genre != null ? gameDetail.genre : "N/A")));
        root.add(generalNode);
        
        DefaultMutableTreeNode technicalNode = new DefaultMutableTreeNode("Technical");
        technicalNode.add(new DefaultMutableTreeNode("Machine: " + (gameDetail.machineType != null ? gameDetail.machineType : "N/A")));
        technicalNode.add(new DefaultMutableTreeNode("Memory: " + (gameDetail.memoryRequired != null ? gameDetail.memoryRequired : "N/A")));
        root.add(technicalNode);
        
        if (gameDetail.screenshots != null && !gameDetail.screenshots.isEmpty()) {
            DefaultMutableTreeNode screenshotsNode = new DefaultMutableTreeNode("Screenshots (" + gameDetail.screenshots.size() + ")");
            for (int i = 0; i < gameDetail.screenshots.size(); i++) {
                screenshotsNode.add(new DefaultMutableTreeNode("Screenshot " + (i + 1)));
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
        String[] models = {"Spectrum 48K", "Spectrum 128K", "Spectrum +3"};
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
        
        JButton openLinkButton = new JButton("🌐 Open Link");
        openLinkButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Would open external link", "Info", JOptionPane.INFORMATION_MESSAGE);
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
}
