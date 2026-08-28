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

package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.api.*;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
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
  private JButton searchButton;
  private JProgressBar searchProgress;
  private JPanel resultsPanel;
  private GameBrowserListener listener;
  private SwingWorker<List<GameSearchResult>, Void> runningSearch;
  private boolean loading;
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
    searchButton = new JButton("Search");
    // Wide enough for the "Searching..." label it shows while a search is in flight.
    searchButton.setPreferredSize(new Dimension(130, 30));

    topPanel.add(searchField, BorderLayout.CENTER);
    topPanel.add(searchButton, BorderLayout.EAST);

    // Indeterminate: the API gives no progress, this only says the search is running.
    searchProgress = new JProgressBar();
    searchProgress.setIndeterminate(true);
    searchProgress.setPreferredSize(new Dimension(0, 4));
    searchProgress.setVisible(false);
    topPanel.add(searchProgress, BorderLayout.SOUTH);

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
    if (query.isEmpty()) {
      return;
    }

    // A search in flight is abandoned rather than left to overwrite the newer one's results.
    if (runningSearch != null && !runningSearch.isDone()) {
      runningSearch.cancel(true);
    }

    setSearching(true);
    showMessage("Searching for \"" + query + "\"...");

    SwingWorker<List<GameSearchResult>, Void> search = new SwingWorker<>() {
      @Override
      protected List<GameSearchResult> doInBackground() {
        // Off the EDT: this is a network round trip to ZXInfo, and running it on the event
        // thread froze the window until the results were ready, so nothing indicated that
        // the search had even started.
        return createMockResults(query);
      }

      @Override
      protected void done() {
        if (isCancelled() || runningSearch != this) {
          return;
        }
        runningSearch = null;
        setSearching(false);

        List<GameSearchResult> results;
        try {
          results = get();
        } catch (Exception e) {
          showMessage("Search failed: " + rootCauseOf(e));
          return;
        }

        if (results.isEmpty()) {
          showMessage("No games found for \"" + query + "\"");
          return;
        }

        resultsPanel.removeAll();
        for (GameSearchResult result : results) {
          resultsPanel.add(createGameRow(result));
          resultsPanel.add(Box.createVerticalStrut(10));
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
      }
    };

    runningSearch = search;
    search.execute();
  }

  /**
   * Loading a game downloads and unzips it before an emulator window can appear, which takes
   * long enough that a click used to look like nothing had happened. Say what is going on, and
   * say plainly when there is nothing to load rather than ignoring the click.
   */
  private void startLoading(GameSearchResult result) {
    if (result.filename == null) {
      JOptionPane.showMessageDialog(this,
          "No tape or snapshot is available to download for \"" + result.title + "\".",
          "Nothing to load", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (loading) {
      return;
    }

    setLoading(true, result.title);
    listener.onGameSelected(result, () -> setLoading(false, null));
  }

  private void setLoading(boolean busy, String title) {
    loading = busy;
    searchProgress.setVisible(busy);
    setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    if (busy) {
      setTitle("Game Browser - loading " + title + "...");
    } else {
      setTitle("Game Browser");
    }
  }

  private void setSearching(boolean searching) {
    searchProgress.setVisible(searching);
    searchButton.setEnabled(!searching);
    searchButton.setText(searching ? "Searching..." : "Search");
    setCursor(Cursor.getPredefinedCursor(searching ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
  }

  /** Replaces the result list with a single centred line of text. */
  private void showMessage(String message) {
    resultsPanel.removeAll();
    JLabel label = new JLabel(message);
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
    resultsPanel.add(label);
    resultsPanel.revalidate();
    resultsPanel.repaint();
  }

  private static String rootCauseOf(Throwable e) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getMessage() != null ? cause.getMessage() : cause.toString();
  }

  private List<GameSearchResult> createMockResults(String query) {
    List<Hit> search = new ZxInfoApiHandler().search(query);

    List<GameSearchResult> results = new ArrayList<>();

    for (Hit hit : search) {
      GameEntry game = hit._source;
      if (game.contentType.equals("SOFTWARE")) {
        List<String> screenshots = new ArrayList<>();
        game.screens.forEach(s1 -> {
          Screen screen = getScreen(s1);

          if (screen != null) {
            String filename = getFileURL(screen.url);

            screenshots.add(filename);
          }
        });
        List<String> files = new ArrayList<>();

        game.releases.forEach(s -> {
          s.files.forEach(f -> {
            if (f.format != null) {
              System.out.println(f.format);
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)", "Tape (TAP)", "Perfect tape (TZX)");
              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)", "Perfect tape (TZX)");
//              List<String> anObject = List.of("Snapshot (Z80)", "Snapshot (SNA)");
              if (anObject.contains(f.format)) {
                String filename = getFileURL(f.path);
                files.add(filename);
              }
            }
          });
        });

        String screenshot1 = getFileURL(screenshots, 0);
        String screenshot2 = getFileURL(screenshots, 1);
        // Entries with nothing downloadable used to be dropped, so a game simply was not in the
        // results and there was no way to tell that from it not existing. Keep them, with a null
        // filename, and say so when one is clicked.
        String file = files.isEmpty() ? null : files.get(0);
        results.add(new GameSearchResult(hit._id, game.title, "http://example.com/game/" + query,
            screenshot1, screenshot2, file));
      }
    }
    return results;
  }

  public static String getFileURL(String f1) {
    String result = "https://worldofspectrum.net" + f1;
    if (f1.startsWith("/zxscreens"))
      return "https://zxinfo.dk/media" + f1;
    else if (f1.startsWith("/zxdb"))
      return "https://spectrumcomputing.co.uk" + f1;

    return result;
  }

  public static Screen getScreen(Object s1) {
    Screen screen = null;
    try {
      screen = gson.fromJson(gson.toJson(s1), Screen.class);
    } catch (JsonSyntaxException e) {
//      e.printStackTrace();
    }
    return screen;
  }

  private String getFileURL(List<String> screenshots, int x) {
    if (x == -1)
      return "https://i.sstatic.net/wAz1X.gif";
    else
      return screenshots.size() > x ? screenshots.get(x) : getFileURL(screenshots, x - 1);
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
          startLoading(result);
        }
      }
    };

    imgLabel1.addMouseListener(mouseAdapter);
    imgLabel2.addMouseListener(mouseAdapter);

    loadItem.addActionListener(e -> startLoading(result));
    detailsItem.addActionListener(e -> listener.onViewDetails(result));
    favoriteItem.addActionListener(e -> listener.onAddToFavorites(result.url));
    downloadItem.addActionListener(e -> listener.onDownloadGame(result.url));

    // The two screenshots already fill the width, so the caption goes above them rather than
    // beside, where it fell outside the viewport and the horizontal scrollbar is disabled.
    JPanel shots = new JPanel();
    shots.setLayout(new BoxLayout(shots, BoxLayout.X_AXIS));
    shots.setOpaque(false);
    shots.setAlignmentX(Component.LEFT_ALIGNMENT);
    shots.add(imgLabel1);
    shots.add(Box.createHorizontalStrut(10));
    shots.add(imgLabel2);
    shots.add(Box.createHorizontalGlue());

    row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
    row.add(createRowCaption(result));
    row.add(Box.createVerticalStrut(4));
    row.add(shots);

    return row;
  }

  /**
   * The rows used to be two screenshots and nothing else, so there was no way to tell which game
   * a row was, let alone that one of them had nothing to download. Entries without a file are
   * kept in the results now, so they have to say so here rather than only when clicked.
   */
  private JPanel createRowCaption(GameSearchResult result) {
    JPanel caption = new JPanel();
    caption.setOpaque(false);
    caption.setLayout(new BoxLayout(caption, BoxLayout.X_AXIS));
    caption.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel title = new JLabel(result.title);
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    caption.add(title);

    if (result.filename == null) {
      title.setForeground(Color.GRAY);
      JLabel unavailable = new JLabel("  -  No tape available");
      unavailable.setForeground(Color.GRAY);
      caption.add(unavailable);
    }

    caption.add(Box.createHorizontalGlue());
    return caption;
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
