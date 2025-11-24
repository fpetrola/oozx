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
import java.util.function.Consumer;

public class SnapshotHistoryInternalFrame extends JInternalFrame {
  private JList<OOZxConfiguration.SnapshotHistoryEntry> historyList;
  private DefaultListModel<OOZxConfiguration.SnapshotHistoryEntry> listModel;
  private Consumer<OOZxConfiguration.SnapshotHistoryEntry> onSnapshotSelected;
  private Consumer<OOZxConfiguration.SnapshotHistoryEntry> onSnapshotRemoved;

  public SnapshotHistoryInternalFrame(OOZxConfiguration config) {
    super("Snapshot History", true, true, true, true);
    setSize(400, 400);
    setLocation(100, 100);

    // Panel principal
    JPanel mainPanel = new JPanel(new BorderLayout());

    // Panel de lista
    listModel = new DefaultListModel<>();
    historyList = new JList<>(listModel);
    historyList.setCellRenderer(new SnapshotHistoryCellRenderer());
    historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    // Agregar el historial a la lista
    for (OOZxConfiguration.SnapshotHistoryEntry entry : config.getSnapshotHistory().values()) {
      listModel.addElement(entry);
    }

    JScrollPane scrollPane = new JScrollPane(historyList);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    // Panel de botones
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    JButton loadButton = new JButton("Load");
    loadButton.addActionListener(e -> {
      OOZxConfiguration.SnapshotHistoryEntry selected = historyList.getSelectedValue();
      if (selected != null && onSnapshotSelected != null) {
        onSnapshotSelected.accept(selected);
      }
    });
    buttonPanel.add(loadButton);

    JButton removeButton = new JButton("Remove from History");
    removeButton.addActionListener(e -> {
      OOZxConfiguration.SnapshotHistoryEntry selected = historyList.getSelectedValue();
      if (selected != null) {
        listModel.removeElement(selected);
        if (onSnapshotRemoved != null) {
          onSnapshotRemoved.accept(selected);
        }
      }
    });
    buttonPanel.add(removeButton);

    JButton clearButton = new JButton("Clear All");
    clearButton.addActionListener(e -> {
      int confirm = JOptionPane.showConfirmDialog(
          this,
          "Clear all snapshot history?",
          "Confirm",
          JOptionPane.YES_NO_OPTION);
      if (confirm == JOptionPane.YES_OPTION) {
        listModel.clear();
        // Notificar el borrado completo
        if (onSnapshotRemoved != null) {
          onSnapshotRemoved.accept(null); // null para indicar borrado completo
        }
      }
    });
    buttonPanel.add(clearButton);

    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);
  }

  public OOZxConfiguration.SnapshotHistoryEntry getSelectedEntry() {
    return historyList.getSelectedValue();
  }

  public void setOnSnapshotSelectedListener(Consumer<OOZxConfiguration.SnapshotHistoryEntry> listener) {
    this.onSnapshotSelected = listener;
  }

  public void setOnSnapshotRemovedListener(Consumer<OOZxConfiguration.SnapshotHistoryEntry> listener) {
    this.onSnapshotRemoved = listener;
  }

  public void refreshHistory(OOZxConfiguration config) {
    listModel.clear();
    for (OOZxConfiguration.SnapshotHistoryEntry entry : config.getSnapshotHistory().values()) {
      listModel.addElement(entry);
    }
  }

  /**
   * Renderer personalizado para mostrar las entradas del historial
   */
  private static class SnapshotHistoryCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      if (value instanceof OOZxConfiguration.SnapshotHistoryEntry) {
        OOZxConfiguration.SnapshotHistoryEntry entry = (OOZxConfiguration.SnapshotHistoryEntry) value;
        super.getListCellRendererComponent(list, entry.getDisplayName(), index, isSelected, cellHasFocus);
        
        // Mostrar la ruta como tooltip
        this.setToolTipText(entry.getFilePath());
        return this;
      }

      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }
}
