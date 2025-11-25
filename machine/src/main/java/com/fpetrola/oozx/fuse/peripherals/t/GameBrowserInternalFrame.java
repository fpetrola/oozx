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

import com.fpetrola.oozx.api.GameEntry;
import com.fpetrola.oozx.api.Hit;
import com.fpetrola.oozx.api.Screen;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.fuse.config.OOZxConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// --- NEW: Game Browser Internal Frame ---
public class GameBrowserInternalFrame extends JInternalFrame {
  private JTextField searchField;
  private JPanel resultsPanel;
  private GameBrowserListener listener;
  public static Gson gson = new Gson();

  public GameBrowserInternalFrame(GameBrowserListener listener) {
    super("Game Browser", true, true, true, true);
    this.listener = listener;
    setSize(560, 600);
    setLocation(50, 50);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());
    topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    searchField = new JTextField();
    searchField.setFont(new Font("Arial", Font.PLAIN, 16));
    JButton searchButton = new JButton("Search");
    searchButton.setPreferredSize(new Dimension(100, 30));

    topPanel.add(searchField, BorderLayout.CENTER);
    topPanel.add(searchButton, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);

    resultsPanel = new JPanel();
    resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
    Color color = UIManager.getColor("Panel.background");
    resultsPanel.setBackground(color);

    JScrollPane scrollPane = new JScrollPane(resultsPanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    add(scrollPane, BorderLayout.CENTER);

    searchButton.addActionListener(e -> performSearch());
    searchField.addActionListener(e -> performSearch());
  }

  private void performSearch() {
    String query = searchField.getText().trim();
    if (query.isEmpty()) return;

    // Clear previous results
    resultsPanel.removeAll();

    SwingUtilities.invokeLater(() -> {
      // Mock search results (in real app, this would be async from web)
      java.util.List<GameSearchResult> mockResults = createMockResults(query);

      for (GameSearchResult result : mockResults) {
        JPanel gameRow = createGameRow(result);
        resultsPanel.add(gameRow);
        resultsPanel.add(Box.createVerticalStrut(10));
      }

      resultsPanel.revalidate();
      resultsPanel.repaint();
    });
  }

  private java.util.List<GameSearchResult> createMockResults(String query) {
    java.util.List<Hit> search = new ZxInfoApiHandler().search(query);

    java.util.List<GameSearchResult> results = new ArrayList<>();

    for (Hit hit : search) {
      GameEntry game = hit._source;
      if (game.contentType.equals("SOFTWARE")) {
        java.util.List<String> screenshots = new ArrayList<>();
        game.screens.forEach(s1 -> {
          Screen screen = getScreen(s1);

          if (screen != null) {
            String filename = "https://worldofspectrum.net" + screen.url;
            if (screen.url.startsWith("/zxscreens"))
              filename = "https://zxinfo.dk/media" + screen.url;

            screenshots.add(filename);
          }
        });
        java.util.List<String> files = new ArrayList<>();

        game.releases.forEach(s -> {
          s.files.forEach(f -> {
            if (f.format != null) {
              System.out.println(f.format);
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)", "Tape (TAP)", "Perfect tape (TZX)");
              java.util.List<String> anObject = java.util.List.of("Snapshot (Z80)", "Snapshot (SNA)", "Perfect tape (TZX)");
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)");
              if (anObject.contains(f.format)) {
                String filename = "https://worldofspectrum.net" + f.path;
                if (f.path.startsWith("/zxdb"))
                  filename = "https://spectrumcomputing.co.uk" + f.path;
                files.add(filename);
              }
            }
          });
        });

        String screenshot1 = getString(screenshots, 0);
        String screenshot2 = getString(screenshots, 1);
        if (!files.isEmpty()) {
          String s = !files.isEmpty() ? files.get(0) : null;
          results.add(new GameSearchResult(hit._id, game.title, "http://example.com/game/" + query, screenshot1, screenshot2, s));
        }
      }
    }
    return results;
  }

  public static Screen getScreen(Object s1) {
    Screen screen = null;
    try {
      screen = gson.fromJson(gson.toJson(s1), Screen.class);
    } catch (JsonSyntaxException e) {
      e.printStackTrace();
    }
    return screen;
  }

  private String getString(List<String> screenshots, int x) {
    if (x == -1)
      return "https://i.sstatic.net/wAz1X.gif";
    else
      return screenshots.size() > x ? screenshots.get(x) : getString(screenshots, x - 1);
  }

  private GameData getGameData() {
    String text = "https://worldofspectrum.net/pub/sinclair/screens/in-game/e/EveryonesAWally.gif";
    ImageIcon img1 = getImageIcon(text);
//      ImageIcon img1 = createPlaceholderImage(256, 192, Color.LIGHT_GRAY, text);
//    String text1 = "https://worldofspectrum.net/pub/sinclair/screens/in-game/e/EveryonesAWally.gif";
//      ImageIcon img2 = createPlaceholderImage(256, 192, Color.GRAY, text1);
//    ImageIcon img2 = new ImageIcon(url);
    GameData gameData = new GameData(img1, img1);
    return gameData;
  }

  private ImageIcon getImageIcon(String text) {
    try {
      return new ImageIcon(new URL(text));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private record GameData(ImageIcon img1, ImageIcon img2) {
  }

  private ImageIcon createPlaceholderImage(int w, int h, Color bg, String text) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(bg);
    g.fillRect(0, 0, w, h);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, w - 1, h - 1);
    g.setFont(new Font("Arial", Font.BOLD, 16));
    FontMetrics fm = g.getFontMetrics();
    int x = (w - fm.stringWidth(text)) / 2;
    int y = (h - fm.getHeight()) / 2 + fm.getAscent();
    g.drawString(text, x, y);
    g.dispose();
    return new ImageIcon(img);
  }

  private JPanel createGameRow(GameSearchResult result) {
    JPanel row = new JPanel();
    Color color = UIManager.getColor("List.background");
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));
    MouseAdapter l = new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        Color color = UIManager.getColor("List.selectionBackground");
        row.setBackground(color);
      }

      public void mouseExited(MouseEvent e) {
        row.setBackground(color);
      }
    };
    row.addMouseListener(l);

    row.setBackground(color);

    JLabel imgLabel1 = new JLabel();
    loadLazyImage(imgLabel1, result.screenshot1, l);
    imgLabel1.setAlignmentY(Component.TOP_ALIGNMENT);
    JLabel imgLabel2 = new JLabel();
    loadLazyImage(imgLabel2, result.screenshot2, l);
    imgLabel2.setAlignmentY(Component.TOP_ALIGNMENT);

    // Context menu
    JPopupMenu contextMenu = new JPopupMenu();
    JMenuItem loadItem = new JMenuItem("Load Game");
    JMenuItem detailsItem = new JMenuItem("View Details");
    JMenuItem favoriteItem = new JMenuItem("Add to Favorites");
    JMenuItem downloadItem = new JMenuItem("Download");
    contextMenu.add(loadItem);
    contextMenu.add(detailsItem);
    contextMenu.add(favoriteItem);
    contextMenu.add(downloadItem);

    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) showPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) showPopup(e);
      }

      private void showPopup(MouseEvent e) {
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
          listener.onGameSelected(result);
        }
      }
    };

    imgLabel1.addMouseListener(mouseAdapter);
    imgLabel2.addMouseListener(mouseAdapter);

    loadItem.addActionListener(e -> listener.onGameSelected(result));
    detailsItem.addActionListener(e -> listener.onViewDetails(result));
    favoriteItem.addActionListener(e -> listener.onAddToFavorites(result.url));
    downloadItem.addActionListener(e -> listener.onDownloadGame(result.url));

    row.add(imgLabel1);
    row.add(Box.createHorizontalStrut(10));
    row.add(imgLabel2);
    row.add(Box.createHorizontalGlue());

    return row;
  }

  private void loadLazyImage(JLabel imgLabel1, String screenshot1, MouseAdapter mouseAdapter) {
    LazyImageIconLoader lazyImageIconLoader = new LazyImageIconLoader(imgLabel1, screenshot1, mouseAdapter);
    lazyImageIconLoader.execute();
  }

  public void setSearchQuery(String query) {
    searchField.setText(query);
    performSearch();
  }

  public OOZxConfiguration.WindowState saveWindowState() {
    OOZxConfiguration.WindowState state = new OOZxConfiguration.WindowState(
        "GAME_BROWSER", getX(), getY(), getWidth(), getHeight());
    state.setSearchQuery(searchField.getText());
    state.setZOrder(ZXSpectrumDesktopApp.getComponentZOrder(this));
    return state;
  }

  public void restoreWindowState(OOZxConfiguration.WindowState state) {
    if (state.getWidth() > 0 && state.getHeight() > 0) {
      setSize(state.getWidth(), state.getHeight());
    }
    if (state.getX() >= 0 && state.getY() >= 0) {
      setLocation(state.getX(), state.getY());
    }
    if (state.getSearchQuery() != null && !state.getSearchQuery().isEmpty()) {
      setSearchQuery(state.getSearchQuery());
    }
  }
}
