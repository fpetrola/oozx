/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.ide;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class HighlightChangedTextField extends JTextField {

    private String previousValue = "";

    public HighlightChangedTextField() {
        super();
        setupListener();
    }

    public HighlightChangedTextField(String number, int i) {
        super(number, i);
        setupListener();
    }

    private void setupListener() {
        // Add a document listener to monitor text changes
        this.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkForChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkForChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkForChange();
            }
        });
    }

    private void checkForChange() {
        // Compare the current text to the previous value
        String currentValue = getText();
        if (!currentValue.equals(previousValue)) {
            setForeground(Color.RED); // Highlight field in red if value has changed
        } else {
            setForeground(Color.BLACK); // Reset background if the value matches
        }
    }

    // Update the previous value explicitly when needed
    public void updatePreviousValue() {
        previousValue = getText();
        setForeground(Color.BLACK); // Reset background if the value matches
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Highlight Changed JTextField");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 100);

            // Create the custom JTextField
            HighlightChangedTextField textField = new HighlightChangedTextField();
            textField.setColumns(20);

            // Add a button to reset the previous value
            JButton resetButton = new JButton("Reset Value");
            resetButton.addActionListener(e -> textField.updatePreviousValue());

            // Layout
            JPanel panel = new JPanel();
            panel.add(textField);
            panel.add(resetButton);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
