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

package com.fpetrola.oozx.speccy.tools.games;

import com.fpetrola.oozx.api.GameFingerprint;
import com.fpetrola.oozx.speccy.devices.Desk;
import com.fpetrola.oozx.api.GameLibrary;
import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;
import com.fpetrola.oozx.speccy.media.LocalGames;
import com.fpetrola.oozx.speccy.windows.LazyImageIconLoader;
import com.fpetrola.oozx.speccy.windows.Widgets;
import com.fpetrola.oozx.api.*;
import com.fpetrola.oozx.rzx.RzxArchive;
import com.fpetrola.oozx.rzx.RzxOption;
import com.fpetrola.oozx.rzx.RzxRecording;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.MenuSelectionManager;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// --- NEW: Game Browser Internal Frame ---
public class GamesFrame extends JInternalFrame implements com.fpetrola.oozx.speccy.devices.KeepsItsPlace {
  private JTextField searchField;
  private JButton searchButton;
  private JProgressBar searchProgress;
  private JPanel resultsPanel;
  private SwingWorker<List<GameSearchResult>, Void> runningSearch;
  private boolean loading;
  private JComboBox<String> sourceFilter;
  private JCheckBox unknownFilter;
  private JCheckBox colourFilter;
  private JLabel libraryLabel;
  private JTree folderTree;
  private JScrollPane folderView;
  private JToggleButton folderExpander;
  private JPanel rescanRow;
  private String onlyInFolder;
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
  private static final String EVERYWHERE = "Everywhere";
  private static final String ON_THE_NET = "On the net";
  private static final String ON_THIS_MACHINE = "On this machine";
  /** The side of a tile, and so what decides how many columns fit. */
  private static final int TILE = 230;
  private static final int GAP = 10;
  /** How far into a row of the folder tree the box reaches, which is where a click turns it off. */
  private static final int BOX = 20;
  /** How many tiles are built at once, which is also how many pictures get asked for. */
  private static final int AT_A_TIME = 60;
  private static final String ANY_GENRE = "Any genre";
  public static Gson gson = new Gson();
  private final RzxArchive archive = new RzxArchive();
  private final com.fpetrola.oozx.speccy.config.OOZxConfiguration config =
      com.fpetrola.oozx.config.Configuration.shared().of(com.fpetrola.oozx.speccy.config.OOZxConfiguration.class);

  /** What a file brings beside itself - colours, for one - said by whoever can recognise it. */
  private static String besides(String file) {
    String brings = com.fpetrola.oozx.plugins.BesideTheGame.whatIsBeside(file);
    return brings == null ? "" : "   -   " + brings;
  }

  private static int idOf(String id) {
    try {
      return Integer.parseInt(id);
    } catch (RuntimeException e) {
      return -1;
    }
  }

  /** A row of this list as the desk takes it: which file, which machine, and what it is. */
  private static Desk.Game asAGame(GameSearchResult result) {
    return new Desk.Game(result.filename, result.machine, result.id, result.title);
  }

  public GamesFrame() {
    super("Game Browser", true, true, true, true);
    setSize(980, 640);
    setLocation(50, 50);

    searchField = new JTextField();
    searchField.setFont(new Font("Arial", Font.PLAIN, 14));
    searchButton = new JButton("Search");
    searchButton.setPreferredSize(new Dimension(130, 30));

    // Indeterminate: neither the API nor a scan of the disk says how far along it is, this only
    // says that something is running.
    searchProgress = new JProgressBar();
    searchProgress.setIndeterminate(true);
    searchProgress.setPreferredSize(new Dimension(0, 4));
    searchProgress.setVisible(false);

    resultsPanel = new FollowsViewportWidth();
    resultsPanel.setLayout(new GridLayout(0, 1, GAP, GAP));
    resultsPanel.setBackground(UIManager.getColor("Panel.background"));
    resultsPanel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

    JScrollPane gallery = new JScrollPane(resultsPanel);
    gallery.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    gallery.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    gallery.getVerticalScrollBar().setUnitIncrement(16);
    // The gallery wraps by being given the number of columns the width allows, which has to be
    // worked out again every time the window or the divider moves.
    gallery.addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        layOutInColumns(gallery.getViewport().getWidth());
      }
    });

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createFilterPanel(), gallery);
    split.setDividerLocation(250);
    split.setResizeWeight(0);
    add(split, BorderLayout.CENTER);

    searchButton.addActionListener(e -> performSearch());
    searchField.addActionListener(e -> performSearch());
  }

  /**
   * Machine and genre are narrowed by the server, which takes them as search parameters and
   * publishes the values it accepts. Whether an entry has an RZX recording, a map, or anything
   * to load at all is not something it can filter on - those live in additionalDownloads - so
   * they are applied to the results here.
   */
  /** As many columns of tiles as fit, so the gallery wraps instead of scrolling sideways. */
  private void layOutInColumns(int width) {
    // Rounded up, not down: a column more makes the tiles narrower than the ideal and so square,
    // where a column less stretches them across the width and the picture goes tall with it.
    int columns = Math.max(1, (int) Math.ceil((double) width / (TILE + GAP)));
    GridLayout grid = (GridLayout) resultsPanel.getLayout();
    if (grid.getColumns() != columns) {
      grid.setColumns(columns);
      grid.setRows(0);
      resultsPanel.revalidate();
    }
  }

  /**
   * Everything that narrows what is shown, down the left side: what to search, where to look for
   * it, and the filters over what comes back. They were a strip along the top, which had room for
   * the four the server takes and none for the ones that are about this machine.
   */
  private JComponent createFilterPanel() {
    JPanel bar = new FollowsViewportWidth();
    bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
    bar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    JButton clear = new JButton("\u2715");
    clear.setToolTipText("Empty the search");
    clear.setMargin(new java.awt.Insets(0, 4, 0, 4));
    clear.addActionListener(e -> {
      searchField.setText("");
      performSearch();
    });
    JPanel searching = new JPanel(new BorderLayout(4, 0));
    searching.setOpaque(false);
    searching.add(searchField, BorderLayout.CENTER);
    searching.add(clear, BorderLayout.EAST);
    bar.add(labelled("Search", searching));
    bar.add(Box.createVerticalStrut(4));
    bar.add(row(searchButton));
    bar.add(Box.createVerticalStrut(4));
    bar.add(searchProgress);
    bar.add(Box.createVerticalStrut(12));

    sourceFilter = new JComboBox<>(new String[]{EVERYWHERE, ON_THE_NET, ON_THIS_MACHINE});
    sourceFilter.setToolTipText("Where to look: ZXInfo's catalogue, the games on this machine, or both");
    bar.add(labelled("Where", sourceFilter));
    bar.add(Box.createVerticalStrut(12));

    machineFilter = new JComboBox<>(new String[]{ANY_MACHINE});
    genreFilter = new JComboBox<>(new String[]{ANY_GENRE});

    machineFilter.setToolTipText("Narrow to one machine, applied by the server");
    genreFilter.setToolTipText("Narrow to one genre, applied by the server");

    rzxFilter = new JCheckBox("RZX");
    rzxFilter.setToolTipText("Only games with a recorded playthrough to replay");
    mapFilter = new JCheckBox("Map");
    mapFilter.setToolTipText("Only games with a game map");
    loadableFilter = new JCheckBox("Loadable", true);
    loadableFilter.setToolTipText("Hide entries with nothing to download");

    unknownFilter = new JCheckBox("Only unknown");
    unknownFilter.setToolTipText("Games on this machine the catalogue could not name");
    colourFilter = new JCheckBox("256 colors");
    colourFilter.setToolTipText("Games with Spec256 colours of their own beside them");

    bar.add(labelled("Machine", machineFilter));
    bar.add(Box.createVerticalStrut(6));
    bar.add(labelled("Genre", genreFilter));
    bar.add(Box.createVerticalStrut(8));
    bar.add(row(rzxFilter));
    bar.add(row(mapFilter));
    bar.add(row(loadableFilter));
    bar.add(row(unknownFilter));
    bar.add(row(colourFilter));
    bar.add(Box.createVerticalStrut(12));
    bar.add(createLibraryPanel());

    // Machine and genre change the query, so they need the server asked again. The rest only
    // narrow what came back, but the results are not kept, so a search is the simplest honest
    // way to reapply them.
    ActionListener research = e -> performSearch();
    machineFilter.addActionListener(research);
    genreFilter.addActionListener(research);
    rzxFilter.addActionListener(research);
    mapFilter.addActionListener(research);
    loadableFilter.addActionListener(research);
    unknownFilter.addActionListener(research);
    colourFilter.addActionListener(research);
    sourceFilter.addActionListener(research);

    loadFilterValues();

    JScrollPane scrolling = new JScrollPane(bar);
    scrolling.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrolling.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrolling.setBorder(BorderFactory.createEmptyBorder());
    scrolling.getVerticalScrollBar().setUnitIncrement(16);
    return scrolling;
  }


  private static JPanel labelled(String text, JComponent field) {
    JPanel panel = new JPanel(new BorderLayout(0, 2));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
    panel.add(label, BorderLayout.NORTH);
    panel.add(field, BorderLayout.CENTER);
    return panel;
  }

  private static JPanel row(JComponent field) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
    panel.add(field, BorderLayout.WEST);
    return panel;
  }

  /**
   * What this machine has, and the way to add to it. Scanning is what turns a folder of files with
   * names like RENE256.SNA into games with titles, and it is offered here rather than run by
   * itself because reading every file of a collection is not something to do behind somebody's
   * back.
   */
  private JPanel createLibraryPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    libraryLabel = new JLabel();
    libraryLabel.setFont(libraryLabel.getFont().deriveFont(Font.PLAIN, 11f));
    JButton scan = new JButton("Add folder...");
    scan.addActionListener(e -> scanFolder());

    panel.add(labelled("This machine", libraryLabel));
    panel.add(Box.createVerticalStrut(4));
    panel.add(row(scan));
    panel.add(Box.createVerticalStrut(4));
    panel.add(createFolderTree());
    sayWhatIsOnThisMachine();
    return panel;
  }

  /**
   * The folders the games are being taken from, out of the way until asked for: most of the time
   * the answer is "the ones I added" and the space belongs to the gallery. Each one says how many
   * games are under it, counting what is in the folders below it too, and picking one narrows the
   * gallery to it.
   */
  private JPanel createFolderTree() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    folderTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("folders")));
    folderTree.setRootVisible(false);
    folderTree.setShowsRootHandles(true);
    folderTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    folderTree.setCellRenderer(new FolderRenderer());
    folderTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent clicked) {
        java.nio.file.Path folder = folderAt(clicked);
        if (folder == null) {
          return;
        }
        // Only the box turns a folder off; clicking the name selects it, which narrows the
        // gallery to it without changing what is being taken into account.
        if (clicked.getX() - folderTree.getPathBounds(folderTree.getPathForLocation(
            clicked.getX(), clicked.getY())).x < BOX) {
          config.takeIntoAccount(folder.toString(), !config.takesIntoAccount(folder.toString()));
          folderTree.repaint();
          performSearch();
        }
      }
    });
    folderTree.addTreeSelectionListener(picked -> {
      Object node = folderTree.getLastSelectedPathComponent();
      onlyInFolder = node instanceof DefaultMutableTreeNode chosen
          && chosen.getUserObject() instanceof Folder folder ? folder.path().toString() : null;
      performSearch();
    });

    folderView = new JScrollPane(folderTree);
    folderView.setPreferredSize(new Dimension(220, 180));
    folderView.setVisible(false);

    folderExpander = new JToggleButton(collapsedLabel());
    folderExpander.setBorderPainted(false);
    folderExpander.setContentAreaFilled(false);
    folderExpander.setHorizontalAlignment(SwingConstants.LEFT);
    folderExpander.addActionListener(e -> {
      folderView.setVisible(folderExpander.isSelected());
      rescanRow.setVisible(folderExpander.isSelected());
      folderExpander.setText(folderExpander.isSelected() ? "\u25be  Folders" : collapsedLabel());
      // Unfolding must not leave a folder chosen from last time narrowing an unseen gallery.
      if (!folderExpander.isSelected() && onlyInFolder != null) {
        folderTree.clearSelection();
      }
      panel.revalidate();
    });

    JButton rescan = new JButton("Rescan");
    rescan.setToolTipText("Look through every folder again, for games added since");
    rescan.addActionListener(e -> rescanEverything());
    rescanRow = row(rescan);
    rescanRow.setVisible(false);

    panel.add(folderExpander, BorderLayout.NORTH);
    panel.add(folderView, BorderLayout.CENTER);
    panel.add(rescanRow, BorderLayout.SOUTH);
    return panel;
  }

  private java.nio.file.Path folderAt(MouseEvent clicked) {
    javax.swing.tree.TreePath path = folderTree.getPathForLocation(clicked.getX(), clicked.getY());
    return path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node
        && node.getUserObject() instanceof Folder folder ? folder.path() : null;
  }

  /** A folder with a box saying whether it is being taken into account. */
  private class FolderRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    private final JCheckBox box = new JCheckBox();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
        boolean expanded, boolean leaf, int row, boolean focused) {
      super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focused);
      if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Folder folder) {
        box.setText(folder.toString());
        box.setSelected(config.takesIntoAccount(folder.path().toString()));
        box.setOpaque(false);
        box.setEnabled(tree.isEnabled());
        return box;
      }
      return this;
    }
  }

  /**
   * Every folder again, for games that were not there last time. A scan skips what has not changed
   * in size or date, so this costs a listing of the folders and a fingerprint of what is new.
   */
  private void rescanEverything() {
    setSearching(true);
    libraryLabel.setText("Looking again...");
    new SwingWorker<Integer, Void>() {
      @Override
      protected Integer doInBackground() throws Exception {
        int gone = LocalGames.library().forgetMissing();
        // Games under a folder nobody lists any more are dropped rather than lingering unseen.
        LocalGames.library().games().stream().filter(copy -> config.getGameFolders().stream()
                .noneMatch(folder -> copy.path().startsWith(folder + java.io.File.separator)))
            .forEach(copy -> LocalGames.library().forget(java.nio.file.Path.of(copy.path())));
        for (String folder : config.getGameFolders()) {
          if (config.takesIntoAccount(folder) && Files.isDirectory(java.nio.file.Path.of(folder))) {
            LocalGames.library().scan(java.nio.file.Path.of(folder), LocalGames.CERTAINTY);
          }
        }
        LocalGames.library().save(LocalGames.file());
        return gone;
      }

      @Override
      protected void done() {
        setSearching(false);
        try {
          get();
        } catch (Exception failed) {
          showMessage("Could not look again: " + rootCauseOf(failed));
        }
        sayWhatIsOnThisMachine();
        performSearch();
      }
    }.execute();
  }

  private String collapsedLabel() {
    return "\u25b8  Folders (" + config.getGameFolders().size() + ")";
  }

  /** A folder of the tree: where it is, and how many games are under it. */
  private record Folder(java.nio.file.Path path, java.util.Set<String> games) {
    @Override
    public String toString() {
      String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
      return name + "  (" + games.size() + ")";
    }
  }

  /**
   * The folders that hold games, each under the one it is in. Only folders with something in them
   * are listed: walking a collection of a thousand games otherwise draws a tree of empty branches.
   */
  private void fillFolderTree() {
    // Games, not files, so the tree and the gallery count the same things: two copies of one game
    // in a folder are one game, which is what the gallery shows as one tile.
    Map<java.nio.file.Path, java.util.Set<String>> directly = new LinkedHashMap<>();
    for (GameLibrary.Copy copy : LocalGames.library().games()) {
      directly.computeIfAbsent(java.nio.file.Path.of(copy.path()).getParent(), dir -> new LinkedHashSet<>())
          .add(copy.identified() ? copy.game().id : copy.path());
    }
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("folders");
    for (String folder : config.getGameFolders()) {
      java.nio.file.Path top = java.nio.file.Path.of(folder);
      Map<java.nio.file.Path, DefaultMutableTreeNode> nodes = new LinkedHashMap<>();
      directly.keySet().stream()
          .filter(dir -> dir != null && dir.startsWith(top))
          .sorted()
          .forEach(dir -> nodeFor(dir, top, nodes, directly));
      DefaultMutableTreeNode node = nodes.get(top);
      root.add(node == null ? new DefaultMutableTreeNode(new Folder(top, java.util.Set.of())) : node);
    }
    folderTree.setModel(new DefaultTreeModel(root));
    // Folded, showing the folders that were added and what each one holds in total; opening one is
    // how somebody asks about what is inside it.
    for (int row = folderTree.getRowCount() - 1; row >= 0; row--) {
      folderTree.collapseRow(row);
    }
    folderTree.scrollRowToVisible(0);
    folderExpander.setText(folderExpander.isSelected() ? "\u25be  Folders" : collapsedLabel());
  }

  /** The node for a folder, making the ones above it first, and counting it into all of them. */
  private DefaultMutableTreeNode nodeFor(java.nio.file.Path dir, java.nio.file.Path top,
      Map<java.nio.file.Path, DefaultMutableTreeNode> nodes,
      Map<java.nio.file.Path, java.util.Set<String>> directly) {
    DefaultMutableTreeNode already = nodes.get(dir);
    if (already != null) {
      return already;
    }
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Folder(dir, new LinkedHashSet<>()));
    nodes.put(dir, node);
    if (!dir.equals(top) && dir.getParent() != null) {
      nodeFor(dir.getParent(), top, nodes, directly).add(node);
    }
    // Counted into this folder and into every one above it, so a top folder says what its whole
    // tree holds rather than only what sits loose in it. A set rather than a sum, because the same
    // game in two of its folders is still one game.
    for (java.nio.file.Path at = dir; at != null && at.startsWith(top); at = at.getParent()) {
      DefaultMutableTreeNode counted = nodes.get(at);
      if (counted != null) {
        ((Folder) counted.getUserObject()).games().addAll(directly.getOrDefault(dir, java.util.Set.of()));
      }
    }
    return node;
  }

  private void sayWhatIsOnThisMachine() {
    GameLibrary library = LocalGames.library();
    long named = library.games().stream().filter(GameLibrary.Copy::identified).count();
    libraryLabel.setText(library.games().size() + " games, " + named + " named");
    fillFolderTree();
  }

  private void scanFolder() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Folder to look for games in");
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    java.nio.file.Path folder = chooser.getSelectedFile().toPath();
    config.addGameFolder(folder.toString());
    setSearching(true);
    libraryLabel.setText("Looking through " + folder.getFileName() + "...");
    new SwingWorker<Integer, Void>() {
      @Override
      protected Integer doInBackground() throws Exception {
        int seen = LocalGames.library().scan(folder, LocalGames.CERTAINTY);
        LocalGames.library().save(LocalGames.file());
        return seen;
      }

      @Override
      protected void done() {
        setSearching(false);
        try {
          get();
        } catch (Exception failed) {
          showMessage("Could not read " + folder + ": " + rootCauseOf(failed));
        }
        sayWhatIsOnThisMachine();
        performSearch();
      }
    }.execute();
  }

  /**
   * A game of this machine said in the same terms as one from the catalogue, so that the gallery,
   * the context menu and the loading path do not have to know where it came from. The picture is
   * not known yet: it is asked for by entry id once the tile exists.
   */
  private GameSearchResult asResult(GameLibrary.Copy copy) {
    GameSearchResult result = new GameSearchResult(copy.identified() ? copy.game().id : null,
        copy.title(), null, null, null, copy.path());
    result.available = true;
    result.files = List.of(copy.path());
    result.onThisMachine = true;
    result.subtitle = copy.identified()
        ? copy.game().yearOfRelease + "  -  " + copy.game().publisher : "unknown";
    // What the catalogue knows about the game beyond its name, so that the filters and the
    // pictures work the same for a game on the disk as for one on the net, and with no network.
    GameFingerprint.Known known = copy.identified()
        ? LocalGames.library().catalogue().known(copy.game().id) : null;
    if (known != null) {
      result.screenshot1 = known.screenshot();
      result.hasMap = known.hasMap();
    }
    result.hasRzx = copy.identified() && archive.hasRecordings(idOf(copy.game().id));
    result.inColour = copy.inColour();
    return result;
  }

  /**
   * Whether a game is under one of the folders being taken into account. The library outlives the
   * list of folders - a folder dropped from it leaves its games written down - and a gallery that
   * showed them anyway would disagree with the tree that counts them.
   */
  private boolean takenIntoAccount(GameLibrary.Copy copy) {
    return config.getGameFolders().stream()
        .anyMatch(folder -> copy.path().startsWith(folder + java.io.File.separator))
        && config.takesIntoAccount(java.nio.file.Path.of(copy.path()).getParent().toString());
  }

  /**
   * What this machine has, one result per file; putting the copies of one game together is done
   * afterwards, by the same merge that joins them to what the net has.
   */
  private List<GameSearchResult> gamesOnThisMachine(String query, boolean onlyUnknown) {
    return LocalGames.library().games().stream()
        .filter(copy -> onlyInFolder == null || copy.path().startsWith(onlyInFolder))
        .filter(this::takenIntoAccount)
        .filter(copy -> !onlyUnknown || !copy.identified())
        .filter(copy -> query.isEmpty() || copy.title().toLowerCase().contains(query.toLowerCase()))
        .map(this::asResult)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * One tile per game, whatever it was found as. Seven copies of Manic Miner in seven folders and
   * the ZXInfo entry for it are eight things and one game; knowing which game each file is - which
   * is what the fingerprint is for - is what makes saying so possible. Every copy stays reachable
   * under Load Version, the one on this machine first, because it is already here.
   * <p>
   * What could not be identified is its own game, since there is nothing to say it is not.
   */
  private List<GameSearchResult> oneTilePerGame(List<GameSearchResult> found) {
    Map<String, GameSearchResult> byGame = new LinkedHashMap<>();
    for (GameSearchResult result : found) {
      GameSearchResult already = byGame.get(keyOf(result));
      if (already == null) {
        byGame.put(keyOf(result), result);
      } else {
        join(already, result);
      }
    }
    byGame.values().forEach(this::sayWhereItIs);
    return new ArrayList<>(byGame.values());
  }

  private static String keyOf(GameSearchResult result) {
    return result.id != null ? result.id : result.filename;
  }

  /** Folds the second into the first, keeping whichever of the two knows more. */
  private void join(GameSearchResult kept, GameSearchResult other) {
    // Files from this machine go first, so the one a click loads is the copy already on the disk.
    kept.files = kept.onThisMachine
        ? concat(kept.files, other.files) : concat(other.files, kept.files);
    kept.filename = kept.files.isEmpty() ? kept.filename : kept.files.get(0);
    kept.copies = kept.copies + other.copies;
    kept.onThisMachine |= other.onThisMachine;
    kept.available |= other.available;
    kept.hasRzx |= other.hasRzx;
    kept.hasMap |= other.hasMap;
    kept.inColour |= other.inColour;
    kept.screenshot1 = kept.screenshot1 != null ? kept.screenshot1 : other.screenshot1;
    kept.screenshot2 = kept.screenshot2 != null ? kept.screenshot2 : other.screenshot2;
    kept.title = kept.title.length() >= other.title.length() ? kept.title : other.title;
    if (kept.recordings.isEmpty()) {
      kept.recordings = other.recordings;
    }
    if (kept.url == null) {
      kept.url = other.url;
    }
  }

  private static List<String> concat(List<String> first, List<String> second) {
    return java.util.stream.Stream.concat(first.stream(), second.stream()).distinct().toList();
  }

  private void sayWhereItIs(GameSearchResult result) {
    String where = result.onThisMachine
        ? (result.copies > 1 ? result.copies + " on disk" : "on disk") : "on the net";
    result.subtitle = result.subtitle == null ? where : result.subtitle + "  -  " + where;
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
    String where = String.valueOf(sourceFilter.getSelectedItem());
    boolean net = !ON_THIS_MACHINE.equals(where);
    boolean machine = !ON_THE_NET.equals(where);
    boolean onlyUnknown = unknownFilter.isSelected();
    boolean onlyColour = colourFilter.isSelected();
    // Asking the net for everything is not a search, but the games on this machine are a list
    // that can simply be shown, so an empty box browses them instead of doing nothing.
    if (query.isEmpty() && net && !machine) {
      // Emptying the box with nothing but the net to look at leaves the last search's tiles on
      // screen, which then say nothing about what is being asked.
      showMessage("Type what to look for on the net, or look at what is on this machine");
      setTitle("Game Browser");
      return;
    }

    // A search in flight is abandoned rather than left to overwrite the newer one's results.
    if (runningSearch != null && !runningSearch.isDone()) {
      runningSearch.cancel(true);
    }

    String machineType = selected(machineFilter);
    String genre = selected(genreFilter);
    boolean onTheNet = net;
    boolean onThisMachine = machine;
    boolean onlyRzx = rzxFilter.isSelected();
    boolean onlyMap = mapFilter.isSelected();
    boolean onlyLoadable = loadableFilter.isSelected();

    setSearching(true);
    showMessage(query.isEmpty() ? "Reading what is on this machine..."
        : "Searching for \"" + query + "\"...");

    SwingWorker<List<GameSearchResult>, Void> search = new SwingWorker<>() {
      @Override
      protected List<GameSearchResult> doInBackground() {
        // Off the EDT: this is a network round trip to ZXInfo, and running it on the event
        // thread froze the window until the results were ready, so nothing indicated that
        // the search had even started. Reading the library is quick, but it goes the same way
        // so that both sources arrive by the same door.
        List<GameSearchResult> found = new ArrayList<>();
        if (onThisMachine) {
          found.addAll(gamesOnThisMachine(query, onlyUnknown));
        }
        if (onTheNet && !query.isEmpty()) {
          found.addAll(createMockResults(query, machineType, genre));
        }
        return found;
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

        results = oneTilePerGame(results);
        int found = results.size();
        // The extras and the availability are things a catalogue entry has; a file already on
        // the disk is available by being there, so those filters are not asked of it.
        // Asked of everything, wherever it came from. They used to be asked only of what came
        // from the net, so ticking RZX left every game on the disk showing and the filter looked
        // broken; what a game on the disk offers is known too, and offline: its recordings from
        // the archive that ships, its map from the catalogue that ships.
        results.removeIf(result ->
            (onlyColour && !result.inColour)
                || (onlyRzx && !result.hasRzx)
                || (onlyMap && !result.hasMap)
                // Not merely "has a file": one the archive will not hand over cannot be
                // loaded either, and a filter for what can be loaded that still shows those is
                // a filter that lies.
                || (onlyLoadable && (result.filename == null || !result.available)));

        if (results.isEmpty()) {
          showMessage(found == 0
              ? (query.isEmpty() ? "Nothing on this machine yet - add a folder to look in"
                  : "No games found for \"" + query + "\"")
              : "None of the " + found + " games found match the filters");
          return;
        }

        resultsPanel.removeAll();
        // A wall of tiles is a wall of pictures to fetch, so only a screenful's worth of them is
        // built at a time. Everything found is still counted, and the search says so.
        for (GameSearchResult result : results.subList(0, Math.min(results.size(), AT_A_TIME))) {
          resultsPanel.add(createGameTile(result));
        }
        // In the title bar, where it does not cost a widget and does not push the gallery down.
        setTitle(results.size() > AT_A_TIME
            ? "Game Browser - showing " + AT_A_TIME + " of " + results.size()
            : "Game Browser - " + results.size() + (results.size() == 1 ? " game" : " games"));
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
    // Clicking the name loads it; hovering opens the machines for when the file is wrong about
    // which one it wants. The entry that used to say "As the file says" was this same thing with
    // a label instead of the name of what it would load.
    menu.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent clicked) {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        load(result, file, null);
      }
    });
    fill(menu, result, file);
    return menu;
  }

  /**
   * The entry that is always there, and under it whatever machines exist by now.
   * <p>
   * Called again every time the menu is about to show, because a search is rendered before any
   * machine has been built: asking then answers nothing, and a menu filled once keeps the nothing
   * for the life of the window.
   */
  private void fill(JMenu menu, GameSearchResult result, String file) {
    menu.removeAll();
    List<String> machines = Desk.theOne().machines();
    if (!machines.isEmpty()) {
      JMenuItem heading = new JMenuItem("Or on a machine of your choosing:");
      heading.setEnabled(false);
      menu.add(heading);
      menu.addSeparator();
      for (String machine : machines) {
        JMenuItem item = new JMenuItem(machine);
        item.addActionListener(e -> load(result, file, machine));
        menu.add(item);
      }
    }
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
              + "to hand it out, and no TOSEC set has a dump of it either.",
          "Not available", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (loading) {
      return;
    }

    setLoading(true, result.title);
    Desk.theOne().open(asAGame(result), () -> setLoading(false, null));
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
    ZxInfoApiHandler api = new ZxInfoApiHandler();
    List<Hit> search = api.search(query, machineType, genreType);

    List<GameSearchResult> results = new ArrayList<>();

    for (Hit hit : search) {
      GameEntry game = api.withTosecFiles(hit._id, hit._source);
      if (game.contentType.equals("SOFTWARE")) {
        List<String> screenshots = new ArrayList<>();
        game.screens.forEach(s1 -> {
          Screen screen = Screen.from(s1);

          if (screen != null) {
            String filename = ZxInfoApiHandler.mediaUrl(screen.url);

            screenshots.add(filename);
          }
        });
        // What the entry offers that cannot be loaded, so a refusal can say what it was rather
        // than "no tape available", which is true and tells nobody anything. Both lists come from
        // the one place that knows: this end once accepted three formats while the scorer ranked
        // seven, and an entry offered only as a TAP was dropped before the scorer ever saw it.
        Map<String, String> offers = ZxInfoApiHandler.filesOf(game, DownloadAndUnzip::loadable);
        List<String> files = offers.keySet().stream().filter(DownloadAndUnzip::loadable).toList();
        Set<String> offered = offers.entrySet().stream()
            .filter(offer -> !DownloadAndUnzip.loadable(offer.getKey()))
            .map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedHashSet::new));

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
            recordings.add(new RzxOption(DownloadAndUnzip.nameOf(download.path) + "  (ZXDB)",
                ZxInfoApiHandler.mediaUrl(download.path)));
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

  private String getFileURL(List<String> screenshots, int x) {
    if (x == -1)
      return "https://i.sstatic.net/wAz1X.gif";
    else
      return screenshots.size() > x ? screenshots.get(x) : getFileURL(screenshots, x - 1);
  }

  private GameData getGameData() {
    String text = "https://worldofspectrum.net/pub/sinclair/screens/in-game/e/EveryonesAWally.gif";
    ImageIcon img1 = getImageIcon(text);
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

  /**
   * One square of the gallery: the loading screen, and under it what the game is. Square because a
   * wall of them is read by the picture, and a row as wide as the window let four games fill it.
   */
  private JPanel createGameTile(GameSearchResult result) {
    JPanel row = new JPanel();
    row.setPreferredSize(new Dimension(TILE, TILE));
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

    Screenshots shots = new Screenshots(l, result.screenshot1);
    shots.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Context menu
    JPopupMenu contextMenu = new JPopupMenu();
    if (!result.recordings.isEmpty()) {
      JMenu playRecording = new JMenu("Play Recording");
      for (RzxOption option : result.recordings) {
        JMenuItem item = new JMenuItem(option.label());
        item.addActionListener(e -> Desk.theOne().play(option.url(), option.label()));
        playRecording.add(item);
      }
      contextMenu.add(playRecording);
      contextMenu.addSeparator();
    }
    java.util.List<Runnable> refills = new ArrayList<>();
    JMenu loadItem = machineMenu(result.inColour ? "Load Game   -   256 colors" : "Load Game", result, null);
    refills.add(() -> fill(loadItem, result, null));
    // When there is more than one, the whole list, in the order the scorer would have taken
    // them, so what is picked by default is the one at the top.
    if (result.files.size() > 1) {
      JMenu versions = new JMenu("Load Version");
      for (String each : result.files) {
        // Which ones are ZXDB's own and which were found elsewhere, since a version that is
        // only in TOSEC is a different thing to choose than one the archive itself hands out.
        String shown = DownloadAndUnzip.nameOf(each)
            + (ZxInfoApiHandler.fromTosec(each) ? "  (found)" : "")
            + besides(each);
        JMenu item = machineMenu(
            DownloadAndUnzip.available(each) ? shown : shown + "  (not available)", result, each);
        item.setEnabled(DownloadAndUnzip.available(each));
        refills.add(() -> fill(item, result, each));
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

    contextMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
        refills.forEach(Runnable::run);
      }

      @Override
      public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
      }
    });

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

    detailsItem.addActionListener(e -> Desk.theOne().showDetails(asAGame(result)));
    favoriteItem.addActionListener(e -> Desk.theOne().keep(asAGame(result)));
    downloadItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
        "Downloading: " + result.url + "\n(Download feature coming soon)", "Download",
        JOptionPane.INFORMATION_MESSAGE));

    // Picture first, caption under it: in a grid the eye finds the game by its loading screen,
    // and the title is what confirms it.
    row.add(shots);
    row.add(Box.createVerticalStrut(4));
    row.add(createTileCaption(result));

    return row;
  }

  private JPanel createTileCaption(GameSearchResult result) {
    JPanel caption = new JPanel();
    caption.setOpaque(false);
    caption.setAlignmentX(Component.LEFT_ALIGNMENT);

    caption.setLayout(new BoxLayout(caption, BoxLayout.Y_AXIS));
    JLabel title = new JLabel(result.title);
    title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
    title.setToolTipText(result.title);
    caption.add(title);
    if (result.subtitle != null) {
      JLabel subtitle = new JLabel(result.subtitle);
      subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 10f));
      subtitle.setForeground(Color.GRAY);
      caption.add(subtitle);
    }

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
  /**
   * A panel that is as wide as whatever is scrolling it and as tall as its contents. Both columns
   * of the window use it: the gallery, so the tiles wrap into the width instead of scrolling
   * sideways, and the filters, so the tree at the bottom can be reached when the window is short.
   */
  private static class FollowsViewportWidth extends JPanel implements Scrollable {

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

  @Override
  public OOZxConfiguration.WindowState saveWindowState() {
    OOZxConfiguration.WindowState state = new OOZxConfiguration.WindowState(
        "GAME_BROWSER", getX(), getY(), getWidth(), getHeight());
    state.setSearchQuery(searchField.getText());
    state.setZOrder(Widgets.zOrderOf(this));
    return state;
  }

  @Override
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
