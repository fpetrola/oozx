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

import com.fpetrola.z80.minizx.emulation.finders.MemoryRangesFinder;
import com.fpetrola.z80.minizx.emulation.finders.MultimapAdapter;
import com.fpetrola.z80.minizx.emulation.finders.Z80Rewinder;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;

public class VerticalToolbarExample extends JFrame {

  public boolean pause;
  private final GameData gameData;
  private final Z80Rewinder z80Rewinder;
  private final MemoryRangesFinder memoryRangesFinder;
  private Runnable restart;

  public VerticalToolbarExample(GameData gameData, Z80Rewinder z80Rewinder, MemoryRangesFinder memoryRangesFinder, Runnable restart) {
    this.gameData = gameData;
    this.z80Rewinder = z80Rewinder;
    this.memoryRangesFinder = memoryRangesFinder;
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
    JButton button4 = new JButton("Save Game Data");
    JButton button5 = new JButton("Load Game Data");

    // Add action listeners to the buttons
    button1.addActionListener(e -> pause = true);

    button2.addActionListener(e -> {
      pause = false;
      restart.run();
    });

    button3.addActionListener(e -> {
      pause = true;
      z80Rewinder.rewind(100000);
    });

    button4.addActionListener(e -> {
      pause = true;
      memoryRangesFinder.processMemoryAccesses();

      MemoryRangesFinder.saveToJson(gameData.name + ".json", MultimapAdapter.getGson(), gameData);
    });

    button5.addActionListener(e -> {
      pause = true;
      Gson gson = MultimapAdapter.getGson();
      GameData gameData1 = MemoryRangesFinder.loadFromJson(gameData.name + ".json", gson);
      MemoryRangesFinder.saveToJson(gameData.name + ".json", MultimapAdapter.getGson(), gameData1);
    });

    // Add buttons to the toolbar
    toolBar.add(button1);
    toolBar.add(button2);
    toolBar.add(button3);
    toolBar.add(button4);
    toolBar.add(button5);

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