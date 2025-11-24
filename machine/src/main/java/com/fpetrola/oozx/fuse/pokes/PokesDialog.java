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

package com.fpetrola.oozx.fuse.pokes;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Diálogo para seleccionar y aplicar pokes a un juego
 */
public class PokesDialog extends JDialog {
  private List<PokFile> availablePokes;
  private Map<String, List<JCheckBox>> pokCheckboxes = new HashMap<>();
  private OnPokesAppliedListener onPokesAppliedListener;
  
  public interface OnPokesAppliedListener {
    void onPokesApplied(List<PokFile.PokeMod> selectedMods);
  }

  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes) {
    super(owner, "Game Cheats/Pokes - " + gameName, true);
    this.availablePokes = availablePokes;
    
    setSize(600, 700);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    initializeUI();
  }

  private void initializeUI() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    // Header
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BorderLayout());
    
    JLabel titleLabel = new JLabel("Available Cheats & Pokes");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
    headerPanel.add(titleLabel, BorderLayout.WEST);
    
    JLabel countLabel = new JLabel(availablePokes.size() + " poke file(s) found");
    countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    countLabel.setForeground(Color.GRAY);
    headerPanel.add(countLabel, BorderLayout.EAST);
    
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    
    // Content area with scrollable pokes
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    
    if (availablePokes.isEmpty()) {
      JLabel noLabel = new JLabel("No pokes found for this game");
      noLabel.setForeground(Color.GRAY);
      noLabel.setFont(new Font("Arial", Font.ITALIC, 12));
      contentPanel.add(noLabel);
      contentPanel.add(Box.createVerticalGlue());
    } else {
      for (PokFile pokFile : availablePokes) {
        JPanel pokPanel = createPokPanel(pokFile);
        contentPanel.add(pokPanel);
        contentPanel.add(Box.createVerticalStrut(10));
      }
      contentPanel.add(Box.createVerticalGlue());
    }
    
    JScrollPane scrollPane = new JScrollPane(contentPanel);
    scrollPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    
    // Buttons
    mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    
    add(mainPanel);
  }

  private JPanel createPokPanel(PokFile pokFile) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new TitledBorder(pokFile.getDisplayName()));
    panel.setBackground(new Color(245, 245, 245));
    
    List<JCheckBox> pokCheckboxList = new ArrayList<>();
    
    if (pokFile.getMods().isEmpty()) {
      JLabel noMods = new JLabel("No mods found in this file");
      noMods.setForeground(Color.GRAY);
      panel.add(noMods);
    } else {
      for (PokFile.PokeMod mod : pokFile.getMods()) {
        JCheckBox checkBox = new JCheckBox(mod.getName());
        checkBox.setBackground(new Color(245, 245, 245));
        checkBox.addActionListener(e -> {
          // Mostrar detalles en tooltip
          if (checkBox.isSelected()) {
            String instruction = mod.getInstruction();
            checkBox.setToolTipText("Instruction: " + instruction);
          }
        });
        panel.add(checkBox);
        pokCheckboxList.add(checkBox);
      }
    }
    
    pokCheckboxes.put(pokFile.getName(), pokCheckboxList);
    
    return panel;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    
    JButton selectAllButton = new JButton("Select All");
    selectAllButton.addActionListener(e -> selectAll(true));
    panel.add(selectAllButton);
    
    JButton clearAllButton = new JButton("Clear All");
    clearAllButton.addActionListener(e -> selectAll(false));
    panel.add(clearAllButton);
    
    panel.add(Box.createHorizontalStrut(10));
    
    JButton applyButton = new JButton("Apply Selected");
    applyButton.addActionListener(e -> applySelected());
    panel.add(applyButton);
    
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());
    panel.add(closeButton);
    
    return panel;
  }

  private void selectAll(boolean selected) {
    for (List<JCheckBox> checkboxes : pokCheckboxes.values()) {
      for (JCheckBox checkbox : checkboxes) {
        checkbox.setSelected(selected);
      }
    }
  }

  private void applySelected() {
    List<PokFile.PokeMod> selectedMods = new ArrayList<>();
    
    for (PokFile pokFile : availablePokes) {
      List<JCheckBox> checkboxes = pokCheckboxes.get(pokFile.getName());
      if (checkboxes != null) {
        List<PokFile.PokeMod> mods = pokFile.getMods();
        for (int i = 0; i < checkboxes.size() && i < mods.size(); i++) {
          if (checkboxes.get(i).isSelected()) {
            selectedMods.add(mods.get(i));
          }
        }
      }
    }
    
    if (onPokesAppliedListener != null) {
      onPokesAppliedListener.onPokesApplied(selectedMods);
    }
    
    if (!selectedMods.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "Applied " + selectedMods.size() + " cheat(s)",
          "Success",
          JOptionPane.INFORMATION_MESSAGE);
    }
    
    dispose();
  }

  public void setOnPokesAppliedListener(OnPokesAppliedListener listener) {
    this.onPokesAppliedListener = listener;
  }
}
