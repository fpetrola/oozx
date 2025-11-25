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
import java.awt.*;
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
}
