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
import java.util.List;

/**
 * Diálogo que muestra los pokes aplicados con un tilde verde y sus descripciones
 */
public class PokesAppliedDialog extends JDialog {
  private List<PokFile.PokeMod> appliedMods;

  public PokesAppliedDialog(Frame owner, List<PokFile.PokeMod> appliedMods) {
    super(owner, "Pokes Applied", true);
    this.appliedMods = appliedMods;
    
    setSize(600, 400);
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    initializeUI();
  }

  private void initializeUI() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
    
    // Header con contador
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BorderLayout());
    
    JLabel titleLabel = new JLabel("✓ " + appliedMods.size() + " poke(s) applied");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
    titleLabel.setForeground(new Color(34, 139, 34)); // Dark green
    headerPanel.add(titleLabel, BorderLayout.WEST);
    
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    
    // Contenedor con lista de pokes aplicados
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    for (PokFile.PokeMod mod : appliedMods) {
      JPanel pokPanel = createPokPanel(mod);
      pokPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pokPanel.getPreferredSize().height));
      contentPanel.add(pokPanel);
      contentPanel.add(Box.createVerticalStrut(8));
    }
    
    JScrollPane scrollPane = new JScrollPane(contentPanel);
    scrollPane.setBorder(new LineBorder(UIManager.getColor("controlShadow")));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    
    // Botón de cierre
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());
    buttonPanel.add(closeButton);
    
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    
    add(mainPanel);
    
    // Agregar ESC para cerrar el diálogo
    KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "closeDialog");
    getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
      }
    });
  }

  private JPanel createPokPanel(PokFile.PokeMod mod) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout(10, 5));
    panel.setBorder(BorderFactory.createCompoundBorder(
        new LineBorder(new Color(200, 220, 200)),
        new EmptyBorder(8, 10, 8, 10)
    ));
    panel.setBackground(new Color(245, 255, 245)); // Fondo muy claro verdoso
    
    // Checkmark verde a la izquierda
    JLabel checkLabel = new JLabel("✓");
    checkLabel.setFont(new Font("Arial", Font.BOLD, 18));
    checkLabel.setForeground(new Color(34, 139, 34)); // Dark green
    panel.add(checkLabel, BorderLayout.WEST);
    
    // Panel central con nombre y descripción
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
    textPanel.setAlignmentY(Component.TOP_ALIGNMENT);
    textPanel.setOpaque(false);
    
    // Nombre del poke
    JLabel nameLabel = new JLabel(mod.getName());
    nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
    nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    textPanel.add(nameLabel);
    
    // Descripción de la instrucción
    JLabel descLabel = new JLabel(mod.getDescription());
    descLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    textPanel.add(descLabel);
    
    panel.add(textPanel, BorderLayout.CENTER);
    
    // Badge con tipo de instrucción a la derecha
    JLabel typeLabel = new JLabel(mod.getInstructionType());
    typeLabel.setFont(new Font("Arial", Font.BOLD, 9));
    typeLabel.setForeground(Color.WHITE);
    typeLabel.setBackground(getColorForInstructionType(mod.getInstructionType()));
    typeLabel.setOpaque(true);
    typeLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
    panel.add(typeLabel, BorderLayout.EAST);
    
    return panel;
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
