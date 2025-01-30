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

package com.fpetrola.z80.minizx.emulation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VerticalToolbarExample extends JFrame {

  public boolean pause;
  private final Z80Rewinder z80Rewinder;
  private Runnable restart;

  public VerticalToolbarExample(Z80Rewinder z80Rewinder, Runnable restart) {
    this.z80Rewinder = z80Rewinder;
    this.restart = restart;
    // Set up the main frame
    setTitle("Vertical Toolbar Example");
    setSize(70, 300);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Create a vertical toolbar
    JToolBar toolBar = new JToolBar();
    toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS)); // Vertical layout
    toolBar.setFloatable(false); // Prevent the toolbar from being moved

    // Create buttons
    JButton button1 = new JButton("Pause");
    JButton button2 = new JButton("Restart");
    JButton button3 = new JButton("Rewind");
    JButton button4 = new JButton("Action 4");

    // Add action listeners to the buttons
    button1.addActionListener(e -> pause = true);

    button2.addActionListener(e -> {
      pause = false;
      restart.run();
    });

    button3.addActionListener(e -> {
      pause= true;
      z80Rewinder.rewind(100000);
    });

    button4.addActionListener(e -> performAction("Action 4"));

    // Add buttons to the toolbar
    toolBar.add(button1);
    toolBar.add(button2);
    toolBar.add(button3);
    toolBar.add(button4);

    // Add the toolbar to the frame
    add(toolBar, BorderLayout.WEST);

    // Make the frame visible
    setVisible(true);
  }

  // Method to handle button actions
  private void performAction(String action) {
    JOptionPane.showMessageDialog(this, "You clicked: " + action);
  }
}