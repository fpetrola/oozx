/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.pokes;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PokesDialog extends JDialog {
  private List<PokFile> availablePokes;
  private List<PokFile> filteredPokes;
  private Map<String, List<JCheckBox>> pokCheckboxes = new HashMap<>();
  private OnPokesAppliedListener onPokesAppliedListener;
  private OnPokesChangedListener onPokesChangedListener;
  private PokesManager pokesManager;
  private JPanel contentPanel;
  private JLabel countLabel;
  private List<PokFile.PokeMod> previouslyAppliedMods = new ArrayList<>();
  private final JPanel appliedList = new JPanel();
  
  public interface OnPokesAppliedListener {
    void onPokesApplied(List<PokFile.PokeMod> selectedMods);
  }

  public interface OnPokesChangedListener {
    void onPokesRemoved(List<PokFile.PokeMod> removedMods);
  }

  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes, PokesManager pokesManager, 
                     List<PokFile.PokeMod> previouslyAppliedMods) {
    super(owner, "Game Cheats/Pokes - " + gameName, true);
    this.availablePokes = availablePokes;
    this.filteredPokes = new ArrayList<>(availablePokes);
    this.pokesManager = pokesManager;
    this.previouslyAppliedMods = previouslyAppliedMods != null ? new ArrayList<>(previouslyAppliedMods) : new ArrayList<>();
    
    setSize(800, 750);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    initializeUI();
  }

  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes, PokesManager pokesManager) {
    this(owner, gameName, availablePokes, pokesManager, null);
  }
  
  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes) {
    this(owner, gameName, availablePokes, null, null);
  }

  public void setPreviouslyAppliedMods(List<PokFile.PokeMod> appliedMods) {
    this.previouslyAppliedMods = new ArrayList<>(appliedMods);
    showWhatIsApplied();
  }

  private void initializeUI() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BorderLayout());
    
    JLabel titleLabel = new JLabel("Available Cheats & Pokes");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
    headerPanel.add(titleLabel, BorderLayout.WEST);
    
    countLabel = new JLabel(availablePokes.size() + " poke file(s) found");
    countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    headerPanel.add(countLabel, BorderLayout.EAST);
    
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    
    if (pokesManager != null) {
      JPanel searchPanel = createSearchPanel();
      mainPanel.add(searchPanel, BorderLayout.BEFORE_FIRST_LINE);
    }
    
    contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    updateContentPanel();
    
    JScrollPane scrollPane = new JScrollPane(contentPanel);
    scrollPane.setBorder(new LineBorder(UIManager.getColor("controlShadow")));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.getVerticalScrollBar().setBlockIncrement(50);
    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
    scrollPane.getHorizontalScrollBar().setBlockIncrement(50);
    
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    
    JPanel bottom = new JPanel(new BorderLayout());
    bottom.add(applied(), BorderLayout.CENTER);
    bottom.add(createButtonPanel(), BorderLayout.SOUTH);
    mainPanel.add(bottom, BorderLayout.SOUTH);
    
    add(mainPanel);
  }
  
  private JPanel createSearchPanel() {
    JPanel searchPanel = new JPanel();
    searchPanel.setLayout(new BorderLayout(5, 5));
    searchPanel.setBorder(BorderFactory.createCompoundBorder(
        new LineBorder(UIManager.getColor("controlShadow")),
        new EmptyBorder(5, 5, 5, 5)
    ));
    
    JLabel searchLabel = new JLabel("Search pok files:");
    searchLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    searchPanel.add(searchLabel, BorderLayout.WEST);
    
    JTextField searchField = new JTextField();
    searchField.setFont(new Font("Arial", Font.PLAIN, 12));
    searchField.setToolTipText("Enter part of the pok file name to search for similar files");
    
    searchField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
          filteredPokes = new ArrayList<>(availablePokes);
        } else {
          filteredPokes = searchPokFiles(searchTerm);
        }
        pokCheckboxes.clear();
        updateContentPanel();
      }
    });
    
    searchPanel.add(searchField, BorderLayout.CENTER);
    
    return searchPanel;
  }
  
  private List<PokFile> searchPokFiles(String searchTerm) {
    if (pokesManager == null) {
      return availablePokes;
    }
    
    List<PokFile> results = new ArrayList<>();
    String lowerSearch = searchTerm.toLowerCase();
    
    for (PokFile pok : availablePokes) {
      if (pok.getName().toLowerCase().contains(lowerSearch) || 
          pok.getDisplayName().toLowerCase().contains(lowerSearch)) {
        results.add(pok);
      }
    }
    
    if (results.isEmpty()) {
      List<PokFile> allResults = pokesManager.searchPokFilesByName(searchTerm);
      results.addAll(allResults);
      availablePokes.addAll(allResults);
    }
    
    return results;
  }
  
  private void updateContentPanel() {
    contentPanel.removeAll();
    pokCheckboxes.clear();
    
    if (filteredPokes.isEmpty()) {
      JLabel noLabel = new JLabel("No pokes found");
      noLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
      noLabel.setFont(new Font("Arial", Font.ITALIC, 12));
      contentPanel.add(noLabel);
    } else {
      for (PokFile pokFile : filteredPokes) {
        JPanel pokPanel = createPokPanel(pokFile);
        pokPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pokPanel.getPreferredSize().height));
        contentPanel.add(pokPanel);
        contentPanel.add(Box.createVerticalStrut(10));
      }
    }
    
    countLabel.setText(filteredPokes.size() + " poke file(s) found");
    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private boolean wasPreviouslyApplied(PokFile.PokeMod mod) {
    for (PokFile.PokeMod applied : previouslyAppliedMods) {
      if (applied.getName().equals(mod.getName()) &&
          applied.getRawInstruction().equals(mod.getRawInstruction())) {
        return true;
      }
    }
    return false;
  }

  private JPanel createPokPanel(PokFile pokFile) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new TitledBorder(pokFile.getDisplayName()));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    List<JCheckBox> pokCheckboxList = new ArrayList<>();
    
    if (pokFile.getMods().isEmpty()) {
      JLabel noMods = new JLabel("No mods found in this file");
      noMods.setForeground(UIManager.getColor("Label.disabledForeground"));
      panel.add(noMods);
    } else {
      for (PokFile.PokeMod mod : pokFile.getMods()) {
       JPanel cheatPanel = new JPanel();
       cheatPanel.setLayout(new BorderLayout(5, 5));
       cheatPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
       cheatPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
       
       JCheckBox checkBox = new JCheckBox(mod.getName());
       if (wasPreviouslyApplied(mod)) {
         checkBox.setSelected(true);
       }
       cheatPanel.add(checkBox, BorderLayout.WEST);
       
       JLabel descLabel = new JLabel(mod.getDescription());
       descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
       descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
       descLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
       cheatPanel.add(descLabel, BorderLayout.CENTER);
       
       JLabel typeLabel = new JLabel(mod.getInstructionType());
       typeLabel.setFont(new Font("Arial", Font.BOLD, 9));
       typeLabel.setForeground(Color.WHITE);
       typeLabel.setBackground(getColorForInstructionType(mod.getInstructionType()));
       typeLabel.setOpaque(true);
       typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
       cheatPanel.add(typeLabel, BorderLayout.EAST);
       
       checkBox.setToolTipText("<html>Raw: " + mod.getRawInstruction() + "<br>" + mod.getDescription() + "</html>");
       
       panel.add(cheatPanel);
       pokCheckboxList.add(checkBox);
      }
    }
    
    pokCheckboxes.put(pokFile.getName(), pokCheckboxList);
    
    return panel;
  }

  /**
   * What is applied right now, under the list of what there is to apply. It was a window of its
   * own that came up over this one and had to be dismissed to carry on choosing, which for
   * something you try, undo and try again is a door in the middle of the room.
   */
  private JComponent applied() {
    appliedList.setLayout(new BoxLayout(appliedList, BoxLayout.Y_AXIS));
    JScrollPane scroll = new JScrollPane(appliedList);
    scroll.setPreferredSize(new Dimension(0, 110));
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setBorder(BorderFactory.createTitledBorder("Applied"));
    showWhatIsApplied();
    return scroll;
  }

  /** Redrawn from what is applied after every change, which is what keeps it true. */
  private void showWhatIsApplied() {
    appliedList.removeAll();
    if (previouslyAppliedMods.isEmpty()) {
      JLabel none = new JLabel("  Nothing applied");
      none.setForeground(java.awt.Color.GRAY);
      appliedList.add(none);
    } else {
      for (PokFile.PokeMod mod : previouslyAppliedMods) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel what = new JLabel("\u2713  " + mod.getName());
        what.setToolTipText(mod.getGameName() + "  -  " + mod.getRawInstruction());
        row.add(what, BorderLayout.WEST);
        JLabel where = new JLabel(mod.getRawInstruction() + "  ");
        where.setForeground(java.awt.Color.GRAY);
        row.add(where, BorderLayout.EAST);
        appliedList.add(row);
      }
    }
    appliedList.revalidate();
    appliedList.repaint();
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
    
    KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "closeDialog");
    getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
      }
    });
    
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
    
    List<PokFile.PokeMod> removedMods = new ArrayList<>();
    for (PokFile.PokeMod previousMod : previouslyAppliedMods) {
      boolean stillSelected = false;
      for (PokFile.PokeMod selectedMod : selectedMods) {
        if (selectedMod.getName().equals(previousMod.getName()) && 
            selectedMod.getRawInstruction().equals(previousMod.getRawInstruction())) {
          stillSelected = true;
          break;
        }
      }
      if (!stillSelected) {
        removedMods.add(previousMod);
      }
    }
    
    if (!removedMods.isEmpty() && onPokesChangedListener != null) {
      onPokesChangedListener.onPokesRemoved(removedMods);
    }
    
    if (onPokesAppliedListener != null) {
      onPokesAppliedListener.onPokesApplied(selectedMods);
    }

    // What is applied is now what was just chosen, and the window stays open saying so: applying
    // is something you do again after seeing what it did, not once on the way out.
    previouslyAppliedMods = new ArrayList<>(selectedMods);
    showWhatIsApplied();
  }

  public void setOnPokesAppliedListener(OnPokesAppliedListener listener) {
    this.onPokesAppliedListener = listener;
  }

  public void setOnPokesChangedListener(OnPokesChangedListener listener) {
    this.onPokesChangedListener = listener;
  }

  private Color getColorForInstructionType(String type) {
    switch (type) {
      case "MEMORY_WRITE":
        return new Color(30, 150, 200);      // Cyan
      case "MEMORY_RESET":
        return new Color(220, 50, 50);       // Red
      case "MEMORY_ADD":
        return new Color(50, 180, 80);       // Green
      case "MEMORY_XOR":
        return new Color(200, 140, 50);      // Orange
      case "END":
        return new Color(120, 120, 120);     // Gray
      default:
        return new Color(100, 100, 150);     // Blue-gray
    }
  }
}
