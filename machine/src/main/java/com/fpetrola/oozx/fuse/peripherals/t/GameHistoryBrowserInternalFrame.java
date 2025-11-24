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

import com.fpetrola.oozx.fuse.config.OOZxConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GameHistoryBrowserInternalFrame extends JInternalFrame {
  private GameHistoryListener listener;
  private JList<GameHistoryEntry> gameList;
  private DefaultListModel<GameHistoryEntry> listModel;

  public interface GameHistoryListener {
    void onGameSelected(OOZxConfiguration.GameHistoryEntry entry);
  }

  public GameHistoryBrowserInternalFrame(GameHistoryListener listener) {
    super("Game History", true, true, true, true);
    this.listener = listener;
    setSize(400, 500);
    setLocation(50, 50);
    
    JPanel panel = new JPanel(new BorderLayout());
    
    // Title
    JLabel titleLabel = new JLabel("Game History");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    panel.add(titleLabel, BorderLayout.NORTH);
    
    // List
    listModel = new DefaultListModel<>();
    gameList = new JList<>(listModel);
    gameList.setCellRenderer(new GameHistoryRenderer());
    gameList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = gameList.getSelectedIndex();
          if (index >= 0) {
            GameHistoryEntry selected = listModel.getElementAt(index);
            if (listener != null) {
              listener.onGameSelected(selected.entry);
            }
          }
        }
      }
    });
    
    JScrollPane scrollPane = new JScrollPane(gameList);
    panel.add(scrollPane, BorderLayout.CENTER);
    
    // Buttons panel
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    
    JButton loadButton = new JButton("Load");
    loadButton.addActionListener(e -> loadSelectedGame());
    buttonsPanel.add(loadButton);
    
    JButton deleteButton = new JButton("Delete");
    deleteButton.addActionListener(e -> deleteSelectedGame());
    buttonsPanel.add(deleteButton);
    
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(e -> refreshGameHistory());
    buttonsPanel.add(refreshButton);
    
    panel.add(buttonsPanel, BorderLayout.SOUTH);
    
    add(panel);
  }

  public void loadGameHistory(List<OOZxConfiguration.GameHistoryEntry> history) {
    listModel.clear();
    if (history != null) {
      for (OOZxConfiguration.GameHistoryEntry entry : history) {
        listModel.addElement(new GameHistoryEntry(entry));
      }
    }
  }

  private void loadSelectedGame() {
    int index = gameList.getSelectedIndex();
    if (index >= 0) {
      GameHistoryEntry selected = listModel.getElementAt(index);
      if (listener != null) {
        listener.onGameSelected(selected.entry);
      }
    }
  }

  private void deleteSelectedGame() {
    int index = gameList.getSelectedIndex();
    if (index >= 0) {
      listModel.remove(index);
    }
  }

  private void refreshGameHistory() {
    // Will be called by parent to refresh the list
  }

  public OOZxConfiguration.WindowState saveWindowState() {
    OOZxConfiguration.WindowState state = new OOZxConfiguration.WindowState(
        "GAME_HISTORY", getX(), getY(), getWidth(), getHeight());
    return state;
  }

  public void restoreWindowState(OOZxConfiguration.WindowState state) {
    if (state.getWidth() > 0 && state.getHeight() > 0) {
      setSize(state.getWidth(), state.getHeight());
    }
    if (state.getX() >= 0 && state.getY() >= 0) {
      setLocation(state.getX(), state.getY());
    }
  }

  // Inner class to represent game history entries in the UI
  private static class GameHistoryEntry {
    OOZxConfiguration.GameHistoryEntry entry;

    GameHistoryEntry(OOZxConfiguration.GameHistoryEntry entry) {
      this.entry = entry;
    }

    @Override
    public String toString() {
      return entry.getGameName();
    }
  }

  // Custom renderer for game history
  private static class GameHistoryRenderer extends DefaultListCellRenderer {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                   boolean isSelected, boolean cellHasFocus) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      
      if (isSelected) {
        panel.setBackground(list.getSelectionBackground());
        panel.setForeground(list.getSelectionForeground());
      } else {
        panel.setBackground(list.getBackground());
        panel.setForeground(list.getForeground());
      }

      if (value instanceof GameHistoryEntry) {
        GameHistoryEntry entry = (GameHistoryEntry) value;
        OOZxConfiguration.GameHistoryEntry histEntry = entry.entry;

        JLabel nameLabel = new JLabel(histEntry.getGameName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(nameLabel, BorderLayout.WEST);

        JLabel timeLabel = new JLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm")
            .format(new Date(histEntry.getTimestamp())));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);
        panel.add(timeLabel, BorderLayout.EAST);
      }

      return panel;
    }
  }
}
