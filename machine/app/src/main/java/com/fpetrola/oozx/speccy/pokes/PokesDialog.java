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

package com.fpetrola.oozx.speccy.pokes;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Diálogo para seleccionar y aplicar pokes a un juego
 */
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
    
    System.out.println("PokesDialog creado con " + this.previouslyAppliedMods.size() + " pokes previamente aplicados");
    for (PokFile.PokeMod mod : this.previouslyAppliedMods) {
      System.out.println("  - " + mod.getName() + " : " + mod.getRawInstruction());
    }
    
    setSize(800, 750);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    initializeUI();
  }

  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes, PokesManager pokesManager) {
    this(owner, gameName, availablePokes, pokesManager, null);
  }
  
  // Constructor compatible con la versión anterior
  public PokesDialog(Frame owner, String gameName, List<PokFile> availablePokes) {
    this(owner, gameName, availablePokes, null, null);
  }

  /**
   * Establece los pokes que fueron previamente aplicados para marcarlos como chequeados
   * (puede usarse si se requiere actualizar después de la construcción)
   */
  public void setPreviouslyAppliedMods(List<PokFile.PokeMod> appliedMods) {
    this.previouslyAppliedMods = new ArrayList<>(appliedMods);
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
    
    countLabel = new JLabel(availablePokes.size() + " poke file(s) found");
    countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    headerPanel.add(countLabel, BorderLayout.EAST);
    
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    
    // Search panel
    if (pokesManager != null) {
      JPanel searchPanel = createSearchPanel();
      mainPanel.add(searchPanel, BorderLayout.BEFORE_FIRST_LINE);
    }
    
    // Content area with scrollable pokes
    contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    updateContentPanel();
    
    JScrollPane scrollPane = new JScrollPane(contentPanel);
    scrollPane.setBorder(new LineBorder(UIManager.getColor("controlShadow")));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    // Configurar velocidad de scroll
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.getVerticalScrollBar().setBlockIncrement(50);
    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
    scrollPane.getHorizontalScrollBar().setBlockIncrement(50);
    
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    
    // Buttons
    mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    
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
    
    // Buscar por nombre exacto en la lista actual
    for (PokFile pok : availablePokes) {
      if (pok.getName().toLowerCase().contains(lowerSearch) || 
          pok.getDisplayName().toLowerCase().contains(lowerSearch)) {
        results.add(pok);
      }
    }
    
    // Si no hay resultados, buscar en todos los archivos disponibles
    if (results.isEmpty()) {
      List<PokFile> allResults = pokesManager.searchPokFilesByName(searchTerm);
      results.addAll(allResults);
      availablePokes.addAll(allResults); // Agregar a la lista disponible
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

  /**
   * Verifica si un PokeMod fue previamente aplicado
   */
  private boolean wasPreviouslyApplied(PokFile.PokeMod mod) {
    for (PokFile.PokeMod applied : previouslyAppliedMods) {
      // Comparar por nombre y por instrucción raw para identificar el mismo poke
      if (applied.getName().equals(mod.getName()) && 
          applied.getRawInstruction().equals(mod.getRawInstruction())) {
        System.out.println("✓ Poke previamente aplicado encontrado: " + mod.getName());
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
       // Crear un panel para cada cheat con nombre y descripción
       JPanel cheatPanel = new JPanel();
       cheatPanel.setLayout(new BorderLayout(5, 5));
       cheatPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
       cheatPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
       
       JCheckBox checkBox = new JCheckBox(mod.getName());
       // Marcar si fue previamente aplicado
       if (wasPreviouslyApplied(mod)) {
         checkBox.setSelected(true);
       }
       cheatPanel.add(checkBox, BorderLayout.WEST);
       
       // Descripción del tipo de instrucción
       JLabel descLabel = new JLabel(mod.getDescription());
       descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
       descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
       descLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
       cheatPanel.add(descLabel, BorderLayout.CENTER);
       
       // Tipo de instrucción como badge
       JLabel typeLabel = new JLabel(mod.getInstructionType());
       typeLabel.setFont(new Font("Arial", Font.BOLD, 9));
       typeLabel.setForeground(Color.WHITE);
       typeLabel.setBackground(getColorForInstructionType(mod.getInstructionType()));
       typeLabel.setOpaque(true);
       typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
       cheatPanel.add(typeLabel, BorderLayout.EAST);
       
       // Tooltip con instrucción raw
       checkBox.setToolTipText("<html>Raw: " + mod.getRawInstruction() + "<br>" + mod.getDescription() + "</html>");
       
       panel.add(cheatPanel);
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
    
    // Agregar ESC para cerrar el diálogo
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
    
    // Detectar pokes que fueron removidos (deschequeados)
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
    
    // Notificar de pokes removidos si hay listener
    if (!removedMods.isEmpty() && onPokesChangedListener != null) {
      onPokesChangedListener.onPokesRemoved(removedMods);
    }
    
    if (onPokesAppliedListener != null) {
      onPokesAppliedListener.onPokesApplied(selectedMods);
    }
    
    if (!selectedMods.isEmpty()) {
      // Mostrar diálogo con detalles de los pokes aplicados
      PokesAppliedDialog appliedDialog = new PokesAppliedDialog(
          (Frame) SwingUtilities.getWindowAncestor(this),
          selectedMods
      );
      appliedDialog.setVisible(true);
    }
    
    dispose();
  }

  public void setOnPokesAppliedListener(OnPokesAppliedListener listener) {
    this.onPokesAppliedListener = listener;
  }

  public void setOnPokesChangedListener(OnPokesChangedListener listener) {
    this.onPokesChangedListener = listener;
  }

  /**
   * Retorna un color para el badge según el tipo de instrucción
   */
  private Color getColorForInstructionType(String type) {
    // Usar colores más saturados para mejor visibilidad en ambos temas
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
