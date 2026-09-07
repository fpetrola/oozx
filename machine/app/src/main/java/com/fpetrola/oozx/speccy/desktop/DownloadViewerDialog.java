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
import javax.swing.AbstractAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Dialog for viewing downloaded files from the internet
 */
public class DownloadViewerDialog extends JDialog {
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    public DownloadViewerDialog(Frame owner, String downloadUrl, String fileName) {
        super(owner, "Download Viewer - " + fileName, false);
        
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        // Setup Escape key to close
        setupEscapeToClose();
        
        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Show loading message
        JLabel loadingLabel = new JLabel("Loading " + fileName + "...");
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(loadingLabel, BorderLayout.CENTER);
        
        add(contentPanel);
        
        // Load file asynchronously
        SwingWorker<Component, Void> worker = new SwingWorker<Component, Void>() {
            @Override
            protected Component doInBackground() throws Exception {
                return createViewerComponent(downloadUrl, fileName);
            }
            
            @Override
            protected void done() {
                try {
                    Component viewer = get();
                    contentPanel.removeAll();
                    contentPanel.add(viewer, BorderLayout.CENTER);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                } catch (Exception e) {
                    contentPanel.removeAll();
                    JTextArea errorArea = new JTextArea();
                    errorArea.setText("Error loading file:\n" + e.getMessage());
                    errorArea.setEditable(false);
                    contentPanel.add(new JScrollPane(errorArea), BorderLayout.CENTER);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Create appropriate viewer component based on file type
     */
    private Component createViewerComponent(String downloadUrl, String fileName) throws IOException {
        String lowerFileName = fileName.toLowerCase();
        
        // Determine file type and create appropriate viewer
        if (isImageFile(lowerFileName)) {
            return createImageViewer(downloadUrl);
        } else if (isTextFile(lowerFileName)) {
            return createTextViewer(downloadUrl);
        } else if (isPdfFile(lowerFileName)) {
            return createPdfViewer(downloadUrl);
        } else {
            return createGenericViewer(downloadUrl, fileName);
        }
    }
    
    private Component createImageViewer(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        ImageIcon icon = new ImageIcon(url);
        
        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            // Scale if too large
            int maxWidth = 750;
            int maxHeight = 550;
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            
            if (width > maxWidth || height > maxHeight) {
                double scaleX = (double) maxWidth / width;
                double scaleY = (double) maxHeight / height;
                double scale = Math.min(scaleX, scaleY);
                width = (int) (width * scale);
                height = (int) (height * scale);
                
                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaledImage);
            }
        }
        
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        return scrollPane;
    }
    
    private Component createTextViewer(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        URLConnection conn = url.openConnection();
        InputStream inputStream = conn.getInputStream();
        
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }
        } finally {
            inputStream.close();
        }
        
        JTextArea textArea = new JTextArea();
        textArea.setText(content.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        return new JScrollPane(textArea);
    }
    
    private Component createPdfViewer(String downloadUrl) {
        JTextArea textArea = new JTextArea();
        textArea.setText("PDF viewing not supported in this dialog.\n\n" +
                "URL: " + downloadUrl + "\n\n" +
                "Please open with your default PDF viewer.");
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        return new JScrollPane(textArea);
    }
    
    private Component createGenericViewer(String downloadUrl, String fileName) {
        JTextArea textArea = new JTextArea();
        textArea.setText("File: " + fileName + "\n" +
                "Type: " + getFileExtension(fileName).toUpperCase() + "\n\n" +
                "URL: " + downloadUrl + "\n\n" +
                "This file type cannot be previewed in this dialog.\n" +
                "Double-click to open with default application.");
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        return new JScrollPane(textArea);
    }
    
    private boolean isImageFile(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") || fileName.endsWith(".webp");
    }
    
    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".txt") || fileName.endsWith(".md") ||
               fileName.endsWith(".html") || fileName.endsWith(".htm") ||
               fileName.endsWith(".xml") || fileName.endsWith(".json") ||
               fileName.endsWith(".csv") || fileName.endsWith(".log");
    }
    
    private boolean isPdfFile(String fileName) {
        return fileName.endsWith(".pdf");
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "unknown";
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
