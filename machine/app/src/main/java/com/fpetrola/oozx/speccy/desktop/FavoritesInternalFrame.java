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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * The games kept to come back to, and a way back into them.
 * <p>
 * A favourite stores what the launcher can open rather than the game's page, so playing one
 * again is the same journey as playing it the first time: a tape or a snapshot goes to a new
 * emulator, a recording goes to the player, and either can be a URL or a file already on disk.
 * Neither path is reimplemented here — both are the ones the application already uses, so the
 * awkward parts, picking the right entry out of a zip and reporting a download that failed,
 * keep working the way they do everywhere else.
 */
public class FavoritesInternalFrame extends JInternalFrame {

  private final OOZxConfiguration configuration;
  private final Consumer<OOZxConfiguration.Favorite> launcher;
  private final FavoritesModel model = new FavoritesModel();
  private final JList<OOZxConfiguration.Favorite> list = new JList<>(model);
  private final JLabel status = new JLabel();

  public FavoritesInternalFrame(OOZxConfiguration configuration,
                                Consumer<OOZxConfiguration.Favorite> launcher) {
    super("Favorites", true, true, true, true);
    this.configuration = configuration;
    this.launcher = launcher;

    setSize(420, 380);
    setLayout(new BorderLayout());

    JToolBar bar = new JToolBar();
    bar.setFloatable(false);

    JButton playButton = button(bar, "25B6.svg", "Play", "Open this favourite again");
    playButton.addActionListener(e -> launchSelected());

    JButton removeButton = button(bar, "1F5D1.svg", "Remove", "Forget this favourite");
    removeButton.addActionListener(e -> removeSelected());

    EmulatorInternalFrame.tighten(bar);
    add(bar, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer((jList, favorite, index, selected, focused) -> {
      JLabel label = new JLabel(favorite.getTitle(),
          EmulatorInternalFrame.loadIcon(favorite.isRecording() ? "1F39E.svg" : "1F4FC.svg"),
          SwingConstants.LEADING);
      label.setOpaque(true);
      label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
      label.setBackground(selected ? jList.getSelectionBackground() : jList.getBackground());
      label.setForeground(selected ? jList.getSelectionForeground() : jList.getForeground());
      label.setToolTipText(favorite.getSource());
      return label;
    });
    // Double click is how a list of things to open is expected to behave.
    list.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) launchSelected();
      }
    });
    add(new JScrollPane(list), BorderLayout.CENTER);

    status.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
    status.setForeground(Color.GRAY);
    add(status, BorderLayout.SOUTH);

    refresh();
  }

  private JButton button(JToolBar bar, String icon, String fallbackText, String tip) {
    JButton button = EmulatorInternalFrame.iconButton(icon, fallbackText, tip);
    bar.add(button);
    return button;
  }

  /** Called after favouriting from elsewhere, so an open window does not go stale. */
  public void refresh() {
    model.reload(configuration.getFavorites());
    status.setText(model.getSize() == 0
        ? "Nothing kept yet - use the star on an emulator's toolbar"
        : model.getSize() + (model.getSize() == 1 ? " favourite" : " favourites"));
  }

  private void launchSelected() {
    OOZxConfiguration.Favorite favorite = list.getSelectedValue();
    if (favorite == null) return;
    status.setText("Opening " + favorite.getTitle() + "...");
    launcher.accept(favorite);
  }

  private void removeSelected() {
    OOZxConfiguration.Favorite favorite = list.getSelectedValue();
    if (favorite == null) return;
    configuration.removeFavorite(favorite.getSource());
    refresh();
  }

  private static class FavoritesModel extends AbstractListModel<OOZxConfiguration.Favorite> {
    private List<OOZxConfiguration.Favorite> favorites = List.of();

    void reload(List<OOZxConfiguration.Favorite> favorites) {
      this.favorites = List.copyOf(favorites);
      fireContentsChanged(this, 0, Math.max(0, favorites.size() - 1));
    }

    public int getSize() {
      return favorites.size();
    }

    public OOZxConfiguration.Favorite getElementAt(int index) {
      return favorites.get(index);
    }
  }
}
