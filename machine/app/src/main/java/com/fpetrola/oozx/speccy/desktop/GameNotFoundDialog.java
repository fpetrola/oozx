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

package com.fpetrola.oozx.speccy.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Unified dialog for displaying "Game Not Found" messages.
 * Can be used in simple mode (just a message) or in search mode (with retry capability).
 */
public class GameNotFoundDialog extends JDialog {
    
    /**
     * Create a simple game not found dialog with just OK button
     */
    public static void showSimple(Window owner, String message) {
        GameNotFoundDialog dialog = new GameNotFoundDialog(owner, message, null);
        dialog.setVisible(true);
    }
    
    /**
     * Create a game not found dialog with retry search capability
     */
    public static void showWithRetry(Window owner, String message, SearchCallback callback) {
        GameNotFoundDialog dialog = new GameNotFoundDialog(owner, message, callback);
        dialog.setVisible(true);
    }
    
    /**
     * Callback interface for retry searches
     */
    @FunctionalInterface
    public interface SearchCallback {
        void onSearch(String gameName);
    }
    
    private GameNotFoundDialog(Window owner, String message, SearchCallback callback) {
        super((Frame) owner, "Game Not Found", true);
        
        setSize(400, (callback != null) ? 150 : 120);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Setup Escape key to close
        setupEscapeToClose();
        
        // Create content
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Message label
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(messageLabel, BorderLayout.NORTH);
        
        // Search panel (only if callback provided)
        if (callback != null) {
            JPanel searchPanel = new JPanel(new FlowLayout());
            searchPanel.add(new JLabel("Try another name:"), BorderLayout.WEST);
            JTextField searchField = new JTextField(20);
            searchField.addActionListener(e -> performSearch(callback, searchField));
            searchPanel.add(searchField, BorderLayout.CENTER);
            panel.add(searchPanel, BorderLayout.CENTER);
            
            // Buttons with Search and Cancel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            
            JButton searchButton = new JButton("Search");
            searchButton.addActionListener(e -> performSearch(callback, searchField));
            buttonPanel.add(searchButton);
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dispose());
            buttonPanel.add(cancelButton);
            
            panel.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            // Only OK button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> dispose());
            buttonPanel.add(okButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        }
        
        add(panel);
    }
    
    private void performSearch(SearchCallback callback, JTextField searchField) {
        String newName = searchField.getText().trim();
        if (!newName.isEmpty()) {
            dispose();
            callback.onSearch(newName);
        }
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
}
