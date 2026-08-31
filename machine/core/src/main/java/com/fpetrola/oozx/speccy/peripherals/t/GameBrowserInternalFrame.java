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
import com.fpetrola.oozx.rzx.RzxArchive;
import com.fpetrola.oozx.rzx.RzxRecording;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
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
  private JComboBox<String> machineFilter;
  private JComboBox<String> genreFilter;
  private JCheckBox rzxFilter;
  private JCheckBox mapFilter;
  private JCheckBox loadableFilter;

  /**
   * First entry of each filter combo, meaning "do not narrow by this". They name what they
   * filter so the bar needs no labels beside them, which is what makes it fit the window.
   */
  private static final String ANY_MACHINE = "Any machine";
  private static final String ANY_GENRE = "Any genre";
  public static Gson gson = new Gson();
  private final RzxArchive archive = new RzxArchive();

  private static int idOf(String id) {
    try {
      return Integer.parseInt(id);
    } catch (RuntimeException e) {
      return -1;
    }
  }

  private static String nameOf(String path) {
    return path.substring(path.lastIndexOf('/') + 1);
  }

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

    topPanel.add(createFilterBar(), BorderLayout.NORTH);

    // Indeterminate: the API gives no progress, this only says the search is running.
    searchProgress = new JProgressBar();
    searchProgress.setIndeterminate(true);
    searchProgress.setPreferredSize(new Dimension(0, 4));
    searchProgress.setVisible(false);
    topPanel.add(searchProgress, BorderLayout.SOUTH);

    add(topPanel, BorderLayout.NORTH);

    resultsPanel = new ResultsPanel();
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

  /**
   * Machine and genre are narrowed by the server, which takes them as search parameters and
   * publishes the values it accepts. Whether an entry has an RZX recording, a map, or anything
   * to load at all is not something it can filter on - those live in additionalDownloads - so
   * they are applied to the results here.
   */
  private JPanel createFilterBar() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

    machineFilter = new JComboBox<>(new String[]{ANY_MACHINE});
    genreFilter = new JComboBox<>(new String[]{ANY_GENRE});
    machineFilter.setPreferredSize(new Dimension(155, 24));
    genreFilter.setPreferredSize(new Dimension(135, 24));
    machineFilter.setToolTipText("Narrow to one machine, applied by the server");
    genreFilter.setToolTipText("Narrow to one genre, applied by the server");

    rzxFilter = new JCheckBox("RZX");
    rzxFilter.setToolTipText("Only games with a recorded playthrough to replay");
    mapFilter = new JCheckBox("Map");
    mapFilter.setToolTipText("Only games with a game map");
    loadableFilter = new JCheckBox("Loadable", true);
    loadableFilter.setToolTipText("Hide entries with nothing to download");

    bar.add(machineFilter);
    bar.add(genreFilter);
    bar.add(rzxFilter);
    bar.add(mapFilter);
    bar.add(loadableFilter);

    // Machine and genre change the query, so they need the server asked again. The rest only
    // narrow what came back, but the results are not kept, so a search is the simplest honest
    // way to reapply them.
    ActionListener research = e -> performSearch();
    machineFilter.addActionListener(research);
    genreFilter.addActionListener(research);
    rzxFilter.addActionListener(research);
    mapFilter.addActionListener(research);
    loadableFilter.addActionListener(research);

    loadFilterValues();
    return bar;
  }

  /** Fills the combos from /metadata/, off the event thread, leaving them usable if it fails. */
  private void loadFilterValues() {
    new SwingWorker<Metadata, Void>() {
      @Override
      protected Metadata doInBackground() {
        return new ZxInfoApiHandler().getMetadata();
      }

      @Override
      protected void done() {
        try {
          Metadata metadata = get();
          // 200 entries is enough to be worth a line in a combo; below that the list is noise.
          fill(machineFilter, Metadata.namesOf(metadata.machinetypes, 200));
          fill(genreFilter, Metadata.namesOf(metadata.genretypes, 200));
        } catch (Exception e) {
          System.err.println("Could not read the filter values: " + rootCauseOf(e));
        }
      }

      private void fill(JComboBox<String> combo, List<String> values) {
        for (String value : values) {
          combo.addItem(value);
        }
      }
    }.execute();
  }

  private String selected(JComboBox<String> combo) {
    Object value = combo.getSelectedItem();
    if (value == null || ANY_MACHINE.equals(value) || ANY_GENRE.equals(value)) {
      return null;
    }
    return value.toString();
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

    String machine = selected(machineFilter);
    String genre = selected(genreFilter);
    boolean onlyRzx = rzxFilter.isSelected();
    boolean onlyMap = mapFilter.isSelected();
    boolean onlyLoadable = loadableFilter.isSelected();

    setSearching(true);
    showMessage("Searching for \"" + query + "\"...");

    SwingWorker<List<GameSearchResult>, Void> search = new SwingWorker<>() {
      @Override
      protected List<GameSearchResult> doInBackground() {
        // Off the EDT: this is a network round trip to ZXInfo, and running it on the event
        // thread froze the window until the results were ready, so nothing indicated that
        // the search had even started.
        return createMockResults(query, machine, genre);
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

        int found = results.size();
        results.removeIf(result ->
            (onlyRzx && !result.hasRzx)
                || (onlyMap && !result.hasMap)
                // Not merely "has a file": one the archive will not hand over cannot be
                // loaded either, and a filter for what can be loaded that still shows those is
                // a filter that lies.
                || (onlyLoadable && (result.filename == null || !result.available)));

        if (results.isEmpty()) {
          showMessage(found == 0
              ? "No games found for \"" + query + "\""
              : "None of the " + found + " games found for \"" + query + "\" match the filters");
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
  /**
   * A file, and under it the machines it can be started on.
   * <p>
   * Most tapes do not say which machine they were made for and cannot: a game that loads once and
   * then looks for a sound chip only finds out at run time. Where the file does say, the first
   * entry follows it, and the rest are there for when somebody knows better than the file - which
   * for a game with AY music and no way to declare it is every time.
   *
   * @param file the release to load, or null for whichever the entry already points at
   */
  private JMenu machineMenu(String label, GameSearchResult result, String file) {
    JMenu menu = new JMenu(label);
    // Filled when it is opened, not when the row is drawn. A search is rendered before any
    // machine has been built, so asking then gets an empty list and a menu that never grows one.
    menu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(MenuEvent e) {
        menu.removeAll();
        JMenuItem automatic = new JMenuItem("As the file says");
        automatic.addActionListener(chosen -> load(result, file, null));
        menu.add(automatic);

        List<String> machines = listener.machines();
        if (!machines.isEmpty()) {
          menu.addSeparator();
          for (String machine : machines) {
            JMenuItem item = new JMenuItem(machine);
            item.addActionListener(chosen -> load(result, file, machine));
            menu.add(item);
          }
        }
      }

      @Override
      public void menuDeselected(MenuEvent e) {
      }

      @Override
      public void menuCanceled(MenuEvent e) {
      }
    });
    // A menu with nothing in it does not open, and Swing measures it before the listener has
    // run, so it starts with the entry that is always there.
    JMenuItem automatic = new JMenuItem("As the file says");
    automatic.addActionListener(e -> load(result, file, null));
    menu.add(automatic);
    return menu;
  }

  private void load(GameSearchResult result, String file, String machine) {
    if (file != null) {
      result.filename = file;
      result.available = DownloadAndUnzip.available(file);
    }
    result.machine = machine;
    startLoading(result);
  }

  private void startLoading(GameSearchResult result) {
    if (result.filename == null) {
      JOptionPane.showMessageDialog(this,
          "There is nothing here this emulator can open for \"" + result.title + "\"."
              + (result.offers == null || result.offers.isBlank() ? ""
              : "\n\nThe archive has it as: " + result.offers + "."),
          "Nothing to load", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (!result.available) {
      // Known beforehand, so there is no reason to spend a download finding out.
      JOptionPane.showMessageDialog(this,
          "\"" + result.title + "\" is not available: the archive holds it but is not allowed "
              + "to hand it out.",
          "Not available", JOptionPane.INFORMATION_MESSAGE);
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

  private List<GameSearchResult> createMockResults(String query, String machineType, String genreType) {
    List<Hit> search = new ZxInfoApiHandler().search(query, machineType, genreType);

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

        // What the entry offers that cannot be loaded, so a refusal can say what it was rather
        // than "no tape available", which is true and tells nobody anything.
        java.util.Set<String> offered = new java.util.LinkedHashSet<>();
        game.releases.forEach(s -> {
          s.files.forEach(f -> {
            if (f.format != null) {
              // Asked of the one thing that knows, instead of a second list kept here. The two
              // disagreed: this end accepted three formats and the scorer ranks seven, so an
              // entry offered only as a TAP was dropped before the scorer ever saw it - and
              // dropped silently, which is how it came to look like a download that refused.
              String filename = getFileURL(f.path);
              if (DownloadAndUnzip.loadable(filename)) {
                files.add(filename);
              } else {
                offered.add(f.format);
              }
            }
          });
        });

        String screenshot1 = getFileURL(screenshots, 0);
        String screenshot2 = getFileURL(screenshots, 1);
        // Entries with nothing downloadable used to be dropped, so a game simply was not in the
        // results and there was no way to tell that from it not existing. Keep them, with a null
        // filename, and say so when one is clicked.
        // Not files.get(0): ZXDB lists several downloads per game and the first is whatever the
        // database happens to return, which for Three Weeks in Paradise is its 128K tape.
        boolean hasMap = false;
        List<RzxOption> recordings = new ArrayList<>();
        for (AdditionalDownload download : game.additionalDownloads == null
            ? List.<AdditionalDownload>of() : game.additionalDownloads) {
          if ("RZX playback file".equals(download.type)) {
            recordings.add(new RzxOption(nameOf(download.path) + "  (ZXDB)", getFileURL(download.path)));
          }
          hasMap |= ZxInfoApiHandler.GAME_MAP_TYPE.equalsIgnoreCase(download.type);
        }
        // The RZX Archive lists recordings ZXDB does not, and knows who made them.
        for (RzxRecording recording : archive.recordingsFor(idOf(hit._id))) {
          if (recording.isPlayable()) {
            String by = recording.submitter() == null || recording.submitter().isBlank()
                ? "RZX Archive" : "by " + recording.submitter();
            recordings.add(new RzxOption(recording.title() + "  (" + by + ")",
                recording.download().url()));
          }
        }
        boolean hasRzx = !recordings.isEmpty();

        // The whole URL, not just the last part of it: the scorer needs the path to see that a
        // file sits under /denied/ and is not going to come down.
        String file = files.isEmpty() ? null : DownloadAndUnzip.preferred(files, url -> url);
        GameSearchResult result = new GameSearchResult(hit._id, game.title,
            "http://example.com/game/" + query, screenshot1, screenshot2, file);
        result.offers = String.join(", ", offered);
        result.available = DownloadAndUnzip.available(file);
        // Kept, not thrown away: an entry often has a 48K release and a 128K one, and the choice
        // between them is the person's to make rather than the scorer's to impose.
        result.files = DownloadAndUnzip.byPreference(files, url -> url);
        result.hasRzx = hasRzx;
        result.hasMap = hasMap;
        result.recordings = recordings;
        results.add(result);
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
    row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
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

    ScreenshotPair shots = new ScreenshotPair(result.screenshot1, result.screenshot2, l);
    shots.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Context menu
    JPopupMenu contextMenu = new JPopupMenu();
    if (!result.recordings.isEmpty()) {
      JMenu playRecording = new JMenu("Play Recording");
      for (RzxOption option : result.recordings) {
        JMenuItem item = new JMenuItem(option.label());
        item.addActionListener(e -> listener.onPlayRecording(option));
        playRecording.add(item);
      }
      contextMenu.add(playRecording);
      contextMenu.addSeparator();
    }
    JMenu loadItem = machineMenu("Load Game", result, null);
    // When there is more than one, the whole list, in the order the scorer would have taken
    // them, so what is picked by default is the one at the top.
    if (result.files.size() > 1) {
      JMenu versions = new JMenu("Load Version");
      for (String each : result.files) {
        String shown = each.substring(each.lastIndexOf('/') + 1);
        JMenu item = machineMenu(
            DownloadAndUnzip.available(each) ? shown : shown + "  (not available)", result, each);
        item.setEnabled(DownloadAndUnzip.available(each));
        versions.add(item);
      }
      contextMenu.add(versions);
    }
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

    shots.addMouseListener(mouseAdapter);

    detailsItem.addActionListener(e -> listener.onViewDetails(result));
    favoriteItem.addActionListener(e -> listener.onAddToFavorites(result));
    downloadItem.addActionListener(e -> listener.onDownloadGame(result.url));

    // The two screenshots already fill the width, so the caption goes above them rather than
    // beside, where it fell outside the viewport and the horizontal scrollbar is disabled.
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

    if (result.filename == null || !result.available) {
      title.setForeground(Color.GRAY);
      JLabel unavailable = new JLabel(
          result.filename == null ? "  -  Nothing this emulator can open" : "  -  Not available");
      unavailable.setForeground(Color.GRAY);
      caption.add(unavailable);
    }

    if (!result.recordings.isEmpty()) {
      JLabel recordings = new JLabel("  -  " + result.recordings.size()
          + (result.recordings.size() == 1 ? " recording" : " recordings"));
      recordings.setForeground(new Color(0, 110, 0));
      recordings.setToolTipText("Right-click to play one");
      caption.add(recordings);
    }

    caption.add(Box.createHorizontalGlue());
    return caption;
  }

  /**
   * The list of results, sized to the viewport rather than to itself.
   * <p>
   * A plain panel in a scroll pane keeps its own preferred width, and with the horizontal
   * scrollbar disabled the extra room simply went unused: the window grew and the rows did not.
   * Tracking the viewport is what passes the new width down to the rows, and from them to the
   * screenshots.
   */
  private static class ResultsPanel extends JPanel implements Scrollable {

    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
      return 16;
    }

    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
      return visible.height;
    }

    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
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
