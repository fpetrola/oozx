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
import java.util.List;

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
    
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BorderLayout());
    
    JLabel titleLabel = new JLabel("✓ " + appliedMods.size() + " poke(s) applied");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
    titleLabel.setForeground(new Color(34, 139, 34)); // Dark green
    headerPanel.add(titleLabel, BorderLayout.WEST);
    
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    
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
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());
    buttonPanel.add(closeButton);
    
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    
    add(mainPanel);
    
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
    
    JLabel checkLabel = new JLabel("✓");
    checkLabel.setFont(new Font("Arial", Font.BOLD, 18));
    checkLabel.setForeground(new Color(34, 139, 34)); // Dark green
    panel.add(checkLabel, BorderLayout.WEST);
    
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
    textPanel.setAlignmentY(Component.TOP_ALIGNMENT);
    textPanel.setOpaque(false);
    
    JLabel nameLabel = new JLabel(mod.getName());
    nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
    nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    textPanel.add(nameLabel);
    
    JLabel descLabel = new JLabel(mod.getDescription());
    descLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    textPanel.add(descLabel);
    
    panel.add(textPanel, BorderLayout.CENTER);
    
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
