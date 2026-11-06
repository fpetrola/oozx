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

import dev.plugdesk.desk.WhatIsMissing;


import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;
import dev.plugdesk.desk.Widgets;
import com.fpetrola.oozx.speccy.windows.AttachedFrame;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.EmulatorWindow;
import com.fpetrola.oozx.speccy.devices.Desk;
import com.fpetrola.oozx.speccy.devices.WhatGameThisIs;
import com.fpetrola.oozx.speccy.devices.DeskEquipment;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.Opens;
import java.util.ServiceLoader;

import com.fpetrola.oozx.speccy.screen.ScreenProfile;
import java.util.Map;
import java.util.LinkedHashMap;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import com.fpetrola.oozx.speccy.screen.TvScreen;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.peripherals.EmulatorCore;
import com.fpetrola.oozx.EmulatorListener;
import dev.plugdesk.app.TellsThePerson;
import com.fpetrola.oozx.speccy.peripherals.DefaultsCore;
import com.fpetrola.z80.cpu.State;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.function.Function;

import static com.fpetrola.oozx.speccy.desktop.EmulatorInternalFrame.loadIcon;

// Emulator Internal Frame
class EmulatorInternalFrame extends JInternalFrame implements EmulatorWindow {
  @Override
  public JComponent picture() {
    return emulatorCore.getPanel();
  }

  /**
   * This machine's keyboard, asked for once.
   * <p>
   * getKeyListener builds a new one on every call, which was harmless while it was called once
   * per window and would be one keyboard per keystroke now that the keys are routed rather than
   * bound to the panel.
   */
  KeyListener keys() {
    if (keys == null) {
      keys = emulatorCore.getKeyListener();
    }
    return keys;
  }

  @Override
  public Desk.Game game() {
    return game;
  }

  public com.fpetrola.oozx.Speccy machine() {
    return parentApp.machineOf(this);
  }

  /** The toolbar's small change lives below this now; these keep the name every caller here uses. */
  public static ImageIcon loadIcon(String iconFile) {
    return Widgets.loadIcon(iconFile);
  }

  static JButton iconButton(String iconFile, String text, String tooltip) {
    return Widgets.iconButton(iconFile, text, tooltip);
  }

  static JToggleButton iconToggle(String iconFile, String text, String tooltip) {
    return Widgets.iconToggle(iconFile, text, tooltip);
  }

  static void tighten(Container toolBar) {
    Widgets.tighten(toolBar);
  }

  static void tighten(Container toolBar, int sideMargin) {
    Widgets.tighten(toolBar, sideMargin);
  }

  public EmulatorCore emulatorCore;
  private ZXSpectrumDesktopApp parentApp;
  private Desk.Game game;
  //  private JLabel statusLabel;
  private JComboBox<String> modelCombo;
  private TellsThePerson.Listening listening;
  private KeyListener keys;
  //  private JLabel tapeStatusLabel;

  public EmulatorInternalFrame(EmulatorCore core, int x, int y, ZXSpectrumDesktopApp parentApp) {
    this(core, x, y, parentApp, null);
  }

  public EmulatorInternalFrame(EmulatorCore core, int x, int y, ZXSpectrumDesktopApp parentApp, Desk.Game game) {
     super("ZX Spectrum Emulator", true, true, true, true);
     this.parentApp = parentApp;
     this.game = game;
     this.emulatorCore = core;
     // Every emulator window is built here, so this is where the machines a build has become
     // known - the browser needs them before one is open and cannot make one to ask.
     parentApp.rememberMachines(core);
    setLocation(x, y);

    // Main Panel (emulator screen)

    JComponent mainPanel = core.getPanel();
    mainPanel.setBackground(Color.BLACK);
    add(mainPanel, BorderLayout.CENTER);
    onRightClick(mainPanel);

    JToolBar toolBar = createToolBar();
    add(toolBar, BorderLayout.NORTH);
    // Status bar
    JPanel statusBar = createStatusBar();
    add(statusBar, BorderLayout.SOUTH);

    // Bind data
    emulatorCore.addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {
//        statusLabel.setText("State: " + state);
      }

      @Override
      public void onError(String message) {
        JOptionPane.showMessageDialog(EmulatorInternalFrame.this, message, "Error", JOptionPane.ERROR_MESSAGE);
      }

      @Override
      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
        setTitle("ZX Spectrum Emulator - " + model);
      }

      @Override
      public void onTapeStatusChanged(String status) {
      }
    });
  }


  /**
   * The last thing the emulator had to say, beside the speed. A snapshot asking for a machine
   * this build does not carry, a plugin that could not be read: the emulator carries on, and
   * this is where the person is told what it did instead. It stays for a while and then goes;
   * the tooltip keeps the last few, since they arrive faster than they can be read.
   */
  private JLabel whatTheEmulatorHasToSay() {
    JLabel said = new JLabel();
    said.setForeground(new Color(0x80, 0x60, 0x00));
    List<String> lately = new ArrayList<>();
    Timer clears = new Timer(12000, gone -> said.setText(""));
    clears.setRepeats(false);
    TellsThePerson.listens(listening = what -> SwingUtilities.invokeLater(() -> {
      System.out.println("oozx: " + what);
      if (lately.size() == 8) lately.remove(0);
      lately.add(what);
      said.setText(what.length() > 70 ? what.substring(0, 68) + "..." : what);
      said.setToolTipText("<html>" + String.join("<br>", lately) + "</html>");
      clears.restart();
    }));
    return said;
  }

  void stopsBeingTold() {
    TellsThePerson.stops(listening);
  }

  /**
   * Cerrada, suelta todo lo que llega a su maquina. Swing y los looks guardan en caches suyas
   * piezas de la ventana - la barra pintada, quien tuvo el foco - y desde cualquiera se llegaba a
   * la maquina: el plugin con que se armo seguia retenido y no se podia sacar.
   */
  void letGoOfTheMachine() {
    emptied(getContentPane());
    emulatorCore = null;
    modelCombo = null;
    keys = null;
    listening = null;
  }

  private static void emptied(java.awt.Container container) {
    for (java.awt.Component part : container.getComponents()) {
      if (part instanceof java.awt.Container inside) emptied(inside);
    }
    container.removeAll();
  }


  private JPanel createStatusBar() {
    JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    // Asked for, not written out again: the hand-written list had no +2A, no +3e and no NTSC,
    // so selecting one of those found nothing in the box and left it naming the machine before.
    modelCombo = new JComboBox<>(emulatorCore.getMachineModels().toArray(new String[0]));
    modelCombo.setSelectedItem(emulatorCore.getCurrentModel());
    // Por el campo y no con el core adentro: cerrada la ventana, el combo sobrevive en caches de
    // Swing y no tiene que llegar a la maquina.
    Widgets.whenChosen(modelCombo, () -> emulatorCore == null ? null : emulatorCore.getCurrentModel(),
        model -> { if (emulatorCore != null) emulatorCore.setMachineModel(model); });
    statusBar.add(modelCombo);
    // Lo que muestran las herramientas: la velocidad, la pausa, el turbo, si hay quien los traiga.
    for (com.fpetrola.oozx.speccy.devices.MachineTool tool : WhatIsPluggedIn.theOne().tools()) {
      JComponent shown = tool.status(this);
      if (shown != null) statusBar.add(shown);
    }
    statusBar.add(whatTheEmulatorHasToSay());
    emulatorCore.addEmulatorListener(new EmulatorListener() {
      public void onModelChanged(String model) {
        modelCombo.setSelectedItem(model);
      }
    });

    return statusBar;
  }

  /**
   * Trims the padding a button keeps around its icon. Shrinking the icon alone leaves the
   * button the size it was, since most of a toolbar button is margin.
   */
  /**
   * A button showing a drawing, falling back to its word if the drawing will not load — the
   * loader throws on a missing file, so without this a mistyped name takes the whole window
   * down instead of one picture.
   */

  /**
   * The same, for a button that stays down: a toggle is not a JButton, so {@link #iconButton}
   * cannot make one and the border button was the last unguarded drawing in this toolbar.
   */

  private JToolBar toolBar;

  private JToolBar createToolBar() {
    toolBar = new JToolBar();
    toolBar.setFloatable(false);
    refillTools();
    return toolBar;
  }

  /**
   * Lo que traen los plugins: cada herramienta pone su boton, y al final un + con las que se
   * podrian traer. Rearmada cuando cambia lo enchufado, asi que lo que llega aparece aca tambien.
   */
  void refillTools() {
    toolBar.removeAll();
    if (parentApp != null) {
      for (com.fpetrola.oozx.speccy.devices.MachineTool tool : WhatIsPluggedIn.theOne().tools()) {
        toolBar.add(tool.button(this));
      }
      List<dev.plugdesk.desk.Action> holes = parentApp.offered.of(com.fpetrola.oozx.speccy.devices.MachineTool.class);
      if (!holes.isEmpty()) toolBar.add(parentApp.workbench.more(holes, "More for this machine: choosing one brings its plugin"));
    }
    tighten(toolBar, 1);
    toolBar.revalidate();
    toolBar.repaint();
  }


  /**
   * A right click on the picture opens the few things a person changes while playing.
   * <p>
   * The menu is built again on every click rather than kept, so what it shows is what the machine
   * says right then: nothing here holds a copy of the speed, the volume or the look.
   */
  private void onRightClick(JComponent picture) {
    picture.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent clicked) {
        offer(clicked);
      }

      @Override
      public void mouseReleased(MouseEvent clicked) {
        offer(clicked);
      }

      // Which of the two is the popup trigger is the desk's business and not the same everywhere.
      private void offer(MouseEvent clicked) {
        if (clicked.isPopupTrigger()) quickMenu().show(clicked.getComponent(), clicked.getX(), clicked.getY());
      }
    });
  }

  /** One choice among several: the group is what makes the previous tick go out when a new one lights. */
  private static JMenu radioMenu(String title, java.util.Map<String, Runnable> options, String chosen) {
    JMenu menu = new JMenu(title);
    ButtonGroup group = new ButtonGroup();
    options.forEach((name, action) -> {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, name.equals(chosen));
      item.addActionListener(e -> action.run());
      group.add(item);
      menu.add(item);
    });
    return menu;
  }

  /**
   * The look, the speed, the volume and the way out, which is what is worth reaching with no
   * toolbar in sight. Every item writes where the toolbar writes, so the two never disagree.
   */
  private JPopupMenu quickMenu() {
    JPopupMenu menu = new JPopupMenu();

    if (emulatorCore.getPanel() instanceof SpeccyScreen screen) {
      ScreenSettings settings = screen.getScreenSettings();
      ScreenProfile current = settings.currentProfile();
      java.util.Map<String, Runnable> looks = new java.util.LinkedHashMap<>();
      for (ScreenProfile profile : ScreenSettings.profiles())
        looks.put(profile.name(), () -> settings.apply(profile));
      menu.add(radioMenu("Look", looks, current == null ? null : current.name()));
    }

    for (com.fpetrola.oozx.speccy.devices.MachineTool tool : WhatIsPluggedIn.theOne().tools()) {
      JMenuItem offered = tool.menuItem(this);
      if (offered != null) menu.add(offered);
    }

    menu.addSeparator();
    return menu;
  }

  /** Every toolbar in the application draws its icons at this size. */
  public static final int TOOLBAR_ICON_SIZE = 19;
}


// --- UPDATED: ZXSpectrumDesktopApp with Game Browser ---
public class ZXSpectrumDesktopApp extends JPanel implements Desk {
  /** Builds a machine for a file; the second argument names the machine, or is null for automatic. */
  private final java.util.function.BiFunction<String, String, EmulatorCore> mockCore;
  /** The machines this build has, learnt from the last emulator made, for offering them. */
  private java.util.List<String> knownMachines = java.util.List.of();

  void rememberMachines(EmulatorCore core) {
    if (knownMachines.isEmpty() && core != null) {
      knownMachines = core.getMachineModels();
    }
  }
  private final Function<String, EmulatorCore> mockCoreState;
  /** The windows of the desk, free inside it, and the desk that offers what the menus and the bar have. */
  private dev.plugdesk.desk.Mdi mdi;
  private JDesktopPane desktop;
  dev.plugdesk.desk.Workbench workbench;
  dev.plugdesk.desk.Offered offered;
  private final dev.plugdesk.desk.PluginsWindow pluginsWindow =
      new dev.plugdesk.desk.PluginsWindow("Emulator/Plugins...", loadIcon("1F9E9.svg"));
  private int emulatorCount = 0;
  private final JFileChooser fileChooser = new JFileChooser();
  protected OOZxConfiguration config;
  {
    // Everything this can be given, including the zips the archives serve: what to do with a
    // file is decided from the file, so there is no reason to ask for one kind at a time.
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
        "ZX Spectrum files (*.tap, *.tzx, *.csw, *.z80, *.sna, *.szx, *.rzx, *.zip)",
        "tap", "tzx", "csw", "z80", "sna", "szx", "rzx", "zip");
    fileChooser.setFileFilter(filter);
    fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
  }

  private void openFile() {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      config.addRecentFile(path);
      workbench.changed();
      open(path);
    }
  }

  /**
   * Opens whatever was picked in the window that plays it: a recording drives a machine of its
   * own from its own controls, anything else is a machine's to load. Every way of arriving at a
   * file goes through here - the menu, the recent list - so that a recording opens the same way
   * whichever of them it came from.
   */
  public void open(String path) {
    java.io.File file = new java.io.File(path);
    // A zip is not a file anything opens: what is inside it is. Unpacking it and asking which
    // part was meant takes seconds and a question, so it happens off the event thread, and what
    // comes out is opened exactly as it would have been if it had been the file picked.
    if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
      decided("Could not open " + DownloadAndUnzip.nameOf(path) + ".",
          () -> openableRecording(file), this::open);
    } else if (opensFor(file) != null) {
      open(new Chosen(file, null, null));
    } else {
      loadInNewEmulator(path);
    }
  }

  /** A recording goes into its own player, keeping where it came from; anything else is a machine's. */
  private void open(Chosen chosen) {
    open(chosen, true);
  }

  /**
   * Lo abre quien diga que lo abre -un reproductor de grabaciones, una casetera- y si nadie lo
   * abre es de una maquina, que cuando tampoco sabe abrirlo ofrece traer lo que falta.
   */
  private void open(Chosen chosen, boolean mayAsk) {
    Opens player = opensFor(chosen.file());
    if (player == null) {
      loadInNewEmulator(chosen.file().getAbsolutePath());
      return;
    }
    player.open(chosen.file(), chosen.source(), chosen.entry());
  }

  private void saveState() {
    EmulatorInternalFrame active = getActiveEmulator();
    if (active == null) {
      JOptionPane.showMessageDialog(this, "No hay emulador activo para guardar estado.", "Save State", JOptionPane.WARNING_MESSAGE);
      return;
    }
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastSaveStateDirectory()));
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastSaveStateDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      active.emulatorCore.saveState(path);
    }
  }

  private void loadState() {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastLoadStateDirectory()));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      config.setLastLoadStateDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
      EmulatorInternalFrame target = getActiveEmulatorOrCreateNew();
      if (target != null) {
        target.emulatorCore.loadState(path);
      }
    }
  }

  /**
   * Hace lo que ofrece una extension puesta, segun lo que sea: una ventana del escritorio, un
   * equipo, una herramienta de la maquina de adelante, o un look.
   */
  void doWhatIsOffered(dev.crystal.plugins.api.Offer offer) {
    java.util.function.Predicate<Object> offering = one -> one.getClass().getName().equals(offer.extension());
    has.deskWindows().stream().filter(offering).findFirst().ifPresent(this::show);
    has.equipment().stream().filter(offering).findFirst().ifPresent(this::show);
    has.tools().stream().filter(offering).findFirst().ifPresent(tool -> {
      EmulatorInternalFrame machine = getActiveEmulator();
      if (machine != null) tool.use(machine);
    });
  }

  private EmulatorInternalFrame getActiveEmulator() {
    JInternalFrame selected = desktop.getSelectedFrame();
    if (selected instanceof EmulatorInternalFrame) {
      return (EmulatorInternalFrame) selected;
    }
    for (JInternalFrame f : desktop.getAllFrames()) {
      if (f instanceof EmulatorInternalFrame && f.isVisible()) {
        return (EmulatorInternalFrame) f;
      }
    }
    return null;
  }

  private EmulatorInternalFrame getActiveEmulatorOrCreateNew() {
    EmulatorInternalFrame frame = getActiveEmulator();
    if (frame == null) {
      EmulatorCore core = mockCore.apply("", null);
      knownMachines = core.getMachineModels();
      frame = createNewEmulator(core);
    }
    try {
      frame.setSelected(true);
    } catch (Exception ignored) {
    }
    return frame;
  }

  /**
   * A snapshot on a machine this build does not carry, a plugin that could not be read: the
   * emulator carries on, and what comes up is a screen that never starts, with nothing saying
   * why. Those stop the person; a remark about how something was done does not, and goes to the
   * status bar of each machine window instead.
   * <p>
   * The desk shows it rather than any one machine window, so there is one of these however many
   * machines are open. Shown from the window's thread and never waited on, since what is telling
   * us is usually the emulator's own thread, and it must not wait on a window.
   */
  private void stopsThePersonWhenSomethingIsMissing() {
    TellsThePerson.listens(new TellsThePerson.Listening() {
      public void about(String what) {
      }

      public void aboutNotDoingIt(String what, String thatNeeds) {
        SwingUtilities.invokeLater(() -> {
          if (thatNeeds == null) {
            JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this, what,
                "This build cannot do that", JOptionPane.WARNING_MESSAGE);
          } else {
            WhatIsMissing.bringWhatIsNeeded(ZXSpectrumDesktopApp.this, thatNeeds, what);
          }
        });
      }
    });
  }

  public ZXSpectrumDesktopApp(java.util.function.BiFunction<String, String, EmulatorCore> mockCore, Function<String, EmulatorCore> mockCoreState1) {
    super(new BorderLayout());
    this.mockCore = mockCore;
    this.mockCoreState = mockCoreState1;
    this.config = dev.plugdesk.app.Configuration.shared().of(OOZxConfiguration.class);
    // One place for the keyboard, rather than one per emulator window: which machine is being
    // typed into is a question about this desktop, not about any one of the machines on it.
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(this::typeIntoTheMachineInFront);
    // What the windows that are found ask of whoever put them on the screen: this desk.
    com.fpetrola.oozx.speccy.devices.Desk.isRunBy(this);
    dev.plugdesk.desk.WhatIsMissing.answering(List.of(com.fpetrola.oozx.speccy.machine.StartsAMachineOn.class,
        com.fpetrola.emulation.helpers.snapshots.SnapshotFile.class, com.fpetrola.oozx.formats.SnapshotFormat.class,
        Equipment.class, com.fpetrola.oozx.Extension.class, com.fpetrola.oozx.plugins.BesideTheGame.class));
    stopsThePersonWhenSomethingIsMissing();
    whatThereIs();
    adoptWhatTheBaseKeepsNow();
    // Emulators apply the defaults themselves as they are built, so putting the saved ones in
    // place here is all it takes for the next window to open configured.
    if (config.getScreenDefaults() != null && !config.getScreenDefaults().isEmpty()) {
      ScreenSettings.setDefaults(config.getScreenDefaults());
    }
    restoreKeptProfiles();

    // Made after the saved look is on: a pane takes its look from whoever is on when it is made.
    mdi = new dev.plugdesk.desk.Mdi();
    desktop = (JDesktopPane) mdi.area();
    new Timer(1000, e -> openWhatTheMachineUses()).start();

    // The menus and the bar are what is offered: this desk's own, the plugins' windows, and what
    // the plugins that are not here would bring, each where its kind goes.
    java.util.Map<Class<?>, String> whereEachGoes = new java.util.LinkedHashMap<>();
    whereEachGoes.put(com.fpetrola.oozx.speccy.devices.Explains.class, "Help");
    whereEachGoes.put(DeskEquipment.class, "Emulator");
    whereEachGoes.put(Equipment.class, "Emulator/Equipment");
    whereEachGoes.put(dev.plugdesk.desk.Look.class, "Window/Look&Feel");
    offered = new dev.plugdesk.desk.Offered(whereEachGoes, null, offer -> {
      somethingWasPluggedIn();
      doWhatIsOffered(offer);
    });
    workbench = new dev.plugdesk.desk.Workbench(mdi, dev.plugdesk.app.Plugins.live(dev.plugdesk.desk.Action.class),
        List.of(this::offers, offered, () -> dev.plugdesk.app.Plugins.found(dev.plugdesk.desk.Actions.class).stream()
            .flatMap(theirs -> theirs.actions().stream()).toList()),
        dev.plugdesk.app.Plugins.live(dev.plugdesk.desk.Part.class));
    add(workbench, BorderLayout.CENTER);
    offered.look(workbench::changed);
  }

  /** For the window the desk is put in. */
  public JMenuBar menus() {
    return workbench.menus();
  }

  /**
   * What the desk kept before the base kept it, taken once: the look it wore and where its window was. Those two are
   * plugdesk's now, and a desk opened for the first time after that would forget them.
   */
  private void adoptWhatTheBaseKeepsNow() {
    dev.plugdesk.desk.DeskConfiguration desk = dev.plugdesk.desk.DeskConfiguration.kept();
    if (desk.look == null) desk.look = config.getLookAndFeel();
    OOZxConfiguration.WindowState main = config.getMainWindowState();
    if (desk.place() == null && main != null && main.getWidth() > 0)
      desk.place(new Rectangle(main.getX(), main.getY(), main.getWidth(), main.getHeight()));
  }

  /**
   * What was open when the desktop was last closed comes back. Not from the constructor: a machine
   * being rebuilt asks the desktop to take it, and the desktop is not there until it is built.
   */
  public void comeBack() {
    restoreOpenWindows();
  }

  /** Every window open, written down for the next start to find it as it was, whichever way the desk closes. */
  public void keepTheSession() {
    saveOpenWindows();
  }

  private static final String PLUGINS = "Plugins";

  /** What this desk offers itself, each where it goes in the menus: the rest comes from the plugins. */
  private List<dev.plugdesk.desk.Action> offers() {
    List<dev.plugdesk.desk.Action> offers = new java.util.ArrayList<>(List.of(
        offering("File/Open...", "ctrl O", 100, this::openFile),
        offering("File/Save State...", "ctrl S", 300, this::saveState),
        offering("File/Load State...", "ctrl L", 301, this::loadState),
        offering("File/Quit", "ctrl Q", 400, () -> dev.plugdesk.desk.DeskWindow.close(this)),
        new Offering("Emulator/New Emulator", List.of(KeyStroke.getKeyStroke("ctrl N")), 100,
            loadIcon("Sinclair_ZX_Spectrum-02b.svg"), null, () -> createNewEmulator(mockCore.apply("", null)))));
    List<String> recent = config.getRecentFiles();
    for (int i = 0; i < recent.size(); i++) {
      String file = recent.get(i);
      offers.add(offering("File/Recent Files/" + dev.plugdesk.desk.Action.step(new java.io.File(file).getName()), i < 10 ? "ctrl " + (i + 1) % 10 : null, 200, () -> open(file)));
    }
    if (!recent.isEmpty()) offers.add(offering("File/Recent Files/Clear Recent Files", null, 300, () -> {
      config.getRecentFiles().clear();
      config.save();
      workbench.changed();
    }));
    for (Equipment kind : has.equipment())
      offers.add(offering("Emulator/Equipment/" + dev.plugdesk.desk.Action.step(kind.name()), null, 100, () -> show(kind)));
    offers.add(pluginsWindow);
    for (com.fpetrola.oozx.speccy.devices.MachineTool tool : has.tools()) {
      if (tool.key() == null) continue;
      offers.add(new Offering("Emulator/" + dev.plugdesk.desk.Action.step(tool.tooltip()), List.of(tool.key()), 200, null, null, () -> {
        EmulatorInternalFrame active = getActiveEmulator();
        if (active != null) tool.use(active);
      }));
    }
    String lead = screenDefault("tv", TvScreen.RGB_MONITOR.label());
    for (TvScreen tv : TvScreen.values())
      offers.add(new Offering("Window/TV/" + tv.label(), List.of(), 301, null, tv.label().equals(lead) || tv.name().equals(lead),
          () -> {
            setScreenDefault("tv", tv.label());
            workbench.changed();
          }));
    boolean scanLines = Double.parseDouble(screenDefault("scanlines", "0")) > 0;
    // Scan lines are a depth in the window, not a switch, so the shortcut ticks to a visible
    // amount and unticks to none rather than pretending there are only two states.
    offers.add(new Offering("Window/TV/Scan lines", List.of(), 401, null, scanLines,
        () -> {
          setScreenDefault("scanlines", scanLines ? "0" : "0.35");
          workbench.changed();
        }));
    return offers;
  }

  private record Offering(String path, List<KeyStroke> keys, int place, javax.swing.Icon icon, Boolean ticked,
                          Runnable doing) implements dev.plugdesk.desk.Action {
    public void run() {
      doing.run();
    }
  }

  private static dev.plugdesk.desk.Action offering(String path, String key, int place, Runnable doing) {
    return new Offering(path, key == null ? List.of() : List.of(KeyStroke.getKeyStroke(key)), place, null, null, doing);
  }

  /**
   * Which lead the picture is imagined to arrive through, and the two things that go with it.
   * <p>
   * The modes come from the enum rather than being listed here, so adding one to the engine puts
   * it in the menu without anybody remembering to.
   */
  /** Looks saved in an earlier run, put back where the windows look for them. */
  private void restoreKeptProfiles() {
    List<ScreenProfile> kept = new java.util.ArrayList<>();
    config.getKeptScreenProfiles().forEach((name, values) ->
        kept.add(new ScreenProfile(name, values, false)));
    ScreenSettings.setKeptProfiles(kept);
  }

  /**
   * Through the core, the same way the border button asks for its option, rather than reaching
   * into the panel to find the screen: the screen is somewhere under a component tree that is
   * not ours to depend on, and a search that stops finding it would fail silently.
   */
  /**
   * Changes one screen knob everywhere: in what new emulators start with, in the file, and in
   * the windows already open.
   * <p>
   * There used to be two ways to remember the same thing — this menu had its own fields in the
   * configuration, older than the knobs — and the one that ran last won. A new emulator applied
   * the saved defaults in its constructor and then had them overwritten by whatever the menu
   * happened to hold, which for scan lines nobody had touched was off. One place now.
   */
  @Override
  public void rememberScreen(String setting, String value) {
    setScreenDefault(setting, value);
  }

  void setScreenDefault(String key, String value) {
    Map<String, String> defaults = new LinkedHashMap<>(config.getScreenDefaults());
    defaults.put(key, value);
    config.setScreenDefaults(defaults);
    config.save();
    ScreenSettings.setDefaults(defaults);
    for (JInternalFrame frame : desktop.getAllFrames()) {
      if (frame instanceof EmulatorInternalFrame emulator
          && emulator.emulatorCore != null
          && emulator.emulatorCore.getPanel() instanceof SpeccyScreen screen) {
        screen.getScreenSettings().apply(Map.of(key, value));
      }
    }
  }

  private String screenDefault(String key, String fallback) {
    return config.getScreenDefaults().getOrDefault(key, fallback);
  }

  @Override
  public void place(JInternalFrame window) {
    mdi.show(window.getTitle(), window, dev.plugdesk.desk.Window.Side.FREE);
  }

  @Override
  public void show(dev.plugdesk.desk.Window window) {
    workbench.show(window);
  }

  @Override
  public void changed() {
    somethingWasPluggedIn();
  }

  @Override
  public String nameOf(javax.swing.JComponent window) {
    String path = workbench.nameOf(window);
    if (path != null) return path.equals(pluginsWindow.path()) ? PLUGINS
        : has.deskWindows().stream().filter(kind -> kind.path().equals(path)).map(DeskEquipment::name).findFirst().orElse(path);
    return equipment.keySet().stream().filter(name -> equipment.get(name).contains(window)).findFirst().orElse(null);
  }

  @Override
  public JInternalFrame window(String name) {
    String path = PLUGINS.equals(name) ? pluginsWindow.path()
        : has.deskWindows().stream().filter(kind -> kind.name().equals(name)).map(DeskEquipment::path).findFirst().orElse(name);
    javax.swing.JComponent window = workbench.window(path);
    return window instanceof JInternalFrame frame ? frame : equipment(name);
  }

  /**
   * Keeps a game to come back to. What is stored is the thing the launcher can open — the
   * downloadable file or the path already loaded — and not the game's page, which would make an
   * entry that can be read and not played.
   */
  void keepAsFavorite(Desk.Game game, String loadedPath) {
    String source = game != null && game.file() != null ? game.file() : loadedPath;
    // A machine started from a recording has neither: it was not opened from a search and it
    // has no file of its own, so the thing worth keeping is the recording driving it.
    Opens playing = playingInto(getActiveEmulator());
    if (source == null && playing != null) {
      keepRecording(playing.cameFrom(), playing.entryInside(), playing.label());
      return;
    }
    if (source == null) {
      JOptionPane.showMessageDialog(this,
          (game != null ? game.title() : "This game") + " has nothing to download, so there is "
              + "nothing to come back to.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    String title = game != null && game.title() != null ? game.title()
        : new java.io.File(source).getName();
    boolean added = config.addFavorite(new OOZxConfiguration.Favorite(source, title, "GAME",
        game == null ? null : game.id()));

    JOptionPane.showMessageDialog(this, added ? title + " is now a favourite."
        : title + " was already a favourite.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Keeps the recording a player has open. What is stored is where it came from plus which file
   * inside the archive it turned out to be, because a URL alone comes back to a zip and not to
   * the recording that was actually being watched.
   */
  /** What is playing into that machine, for keeping it: the window clipped on knows, not this. */
  private Opens playingInto(JInternalFrame machine) {
    if (machine == null) {
      return null;
    }
    for (java.util.List<MachineFrame> open : equipment.values()) {
      for (MachineFrame window : open) {
        if (!window.isClosed() && window.getMachineWindow() == machine
            && window instanceof Opens playing && playing.cameFrom() != null) {
          return playing;
        }
      }
    }
    return null;
  }

  @Override
  public OOZxConfiguration configuration() {
    return config;
  }

  @Override
  public void openSaved(String savedStateId, String file) {
    if (savedStateId != null && !savedStateId.isEmpty()) {
      try {
        String snapshotData = config.getSnapshot(savedStateId);
        if (snapshotData != null && !snapshotData.isEmpty()) {
          createNewEmulator(mockCoreState.apply(snapshotData), file);
          return;
        }
      } catch (Exception ex) {
        System.err.println("Error restaurando snapshot guardado: " + ex.getMessage());
      }
    }
    createNewEmulator(mockCore.apply(file, null), file);
  }

  @Override
  public void play(String url, String label, String entry) {
    playRecording(label, url, entry);
  }

  /**
   * Lo que se sabe de un juego, mostrado por quien sepa. El escritorio no: abre archivos y arma
   * maquinas, y de un catalogo de juegos no sabe nada. Sigue estando el metodo porque un plugin
   * ya publicado lo llama.
   */
  @Override
  public void showDetails(Desk.Game game) {
    WhatGameThisIs knows = has.aboutGames();
    if (knows == null) {
      TellsThePerson.thisBuildCannot("nada en este emulador sabe de juegos, asi que no hay"
          + " detalles que mostrar de " + game.title() + ".", "games");
      return;
    }
    knows.show(game);
  }

  /** Keeps a game to come back to, asked by whoever is showing it. */
  @Override
  public void keep(Desk.Game game) {
    keepAsFavorite(game, null);
  }

  @Override
  public void open(Desk.Game game, Runnable whenDone) {
    load(game.file(), game, whenDone, true);
  }

  /**
   * The machines this build can open a game on.
   * <p>
   * Learnt from a machine that exists, because the list belongs to the build and there is no
   * copy of it to read; the first one made is remembered so a window can offer them without one
   * having to be open.
   * <p>
   * With none made yet, one is built and never started, which is what the settings already do to
   * ask the same sort of question. A written-down list stood here before, and a machine that
   * arrives in a jar is not on any list written down.
   */
  @Override
  public java.util.List<String> machines() {
    if (knownMachines.isEmpty()) {
      knownMachines = new DefaultsCore(config).getMachineModels();
    }
    return knownMachines;
  }

  /** A recording from wherever it lives, which is a machine driven by it. */
  @Override
  public void play(String url, String label) {
    playRecording(label, url, null);
  }

  // ... (rest of the methods: createNewEmulator, cascadeWindows, tileWindows remain unchanged)

  /**
   * The deck belonging to each machine. Weak so a closed emulator can be collected; the browser
   * looks a tape up from whichever emulator is in front rather than being tied to one.
   */
  private final java.util.Map<EmulatorCore, com.fpetrola.oozx.Speccy> machinesByCore =
      new java.util.HashMap<>();

  /** The deck inside a machine's window, or null when that window is not a machine. */
  /** The emulator behind a window, for a peripheral that has to reach the machine it is clipped to. */
  public void registerMachine(EmulatorCore core, com.fpetrola.oozx.Speccy speccy) {
    machinesByCore.put(core, speccy);
  }

  /**
   * Lo que se cerro deja de estar anotado.
   * <p>
   * Una maquina cerrada seguia en este mapa, y una ventana de un plugin en el de lo que hay
   * abierto, asi que nada de eso se moria: ni la maquina, ni sus perifericos, ni el plugin que
   * trajo la ventana, ni el cargador de ese plugin. Sacar un plugin no podia funcionar porque
   * algo cerrado seguia usandolo.
   */
  private void forgetWhatIsClosed(EmulatorCore closed) {
    machinesByCore.remove(closed);
    equipment.values().forEach(open -> open.removeIf(JInternalFrame::isClosed));
    equipment.values().removeIf(java.util.List::isEmpty);
  }

  public com.fpetrola.oozx.Speccy machineOf(JInternalFrame window) {
    return window instanceof EmulatorInternalFrame emulator
        ? machinesByCore.get(emulator.emulatorCore) : null;
  }

  /** The core behind a machine's window, for a window that configures whichever it is clipped onto. */
  @Override
  public EmulatorCore coreOf(java.awt.Component window) {
    return window instanceof EmulatorInternalFrame emulator ? emulator.emulatorCore : null;
  }

  /**
   * Every cassette deck open at once, one per machine.
   * <p>
   * There used to be one window, kept in a field, which played into whichever machine was in
   * front - so the tape you were watching load jumped to another computer when you clicked on
   * it. A deck is a piece of equipment: it is clipped onto one machine, plays into that one, and
   * you move it by unplugging it and clipping it onto the next.
   */

  /**
   * Every player open at once. There used to be one, kept in a field and reused, so a second
   * recording took the first one's window and its machine: watching two was watching the later
   * one. A recording already builds its own machine - its session makes its own
   * injector and its own Speccy - so nothing below the window ever required there to be one.
   */
  /**
   * Fetches a recording and plays it. The fetching is off the event thread, and a recording may
   * arrive on its own or inside a zip, which is the same choice a game download already makes.
   */

  /**
   * @param preferredEntry the file inside the archive to open, when one was chosen before and
   *                       kept. With it a favourite comes back to the same recording instead of
   *                       asking again which part was meant.
   */
  public void playRecording(String label, String url, String preferredEntry) {
    playWhatever("Could not fetch " + label + ".", () -> {
      java.util.List<java.nio.file.Path> parts = DownloadAndUnzip.fetchAll(url);
      if (parts.isEmpty()) {
        throw new java.io.IOException("nothing playable came out of it");
      }
      java.io.File part = choosePart(label, parts, preferredEntry).toFile();
      return new Chosen(part, url, part.getName());
    });
  }

  /**
   * The one way in for a recording, wherever it was named: off the disk, out of the archive, or
   * from a favourite. What is decided before anything is on the desktop is which file to play;
   * only then does a player appear, and it appears with its machine.
   *
   * @param decide what to play, run off the event thread when it fetches; null if nothing was chosen
   */
  private void playWhatever(String whenItFails, java.util.concurrent.Callable<Chosen> decide) {
    playInto(null, whenItFails, decide);
  }

  /**
   * Decides which file off the event thread - it may be fetched, unpacked and asked about - and
   * then does something with it on the event thread.
   */
  private void decided(String whenItFails, java.util.concurrent.Callable<Chosen> decide,
                       java.util.function.Consumer<Chosen> then) {
    // Deciding takes seconds and nothing is on the desktop yet to say so. The pointer is what
    // is left.
    working(true);
    new SwingWorker<Chosen, Void>() {
      @Override
      protected Chosen doInBackground() throws Exception {
        return decide.call();
      }

      @Override
      protected void done() {
        working(false);
        try {
          Chosen chosen = get();
          if (chosen != null) {
            then.accept(chosen);
          }
        } catch (Exception itWouldNot) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this,
              whenItFails + "\n\n" + reason(itWouldNot), "Open", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  /**
   * @param asking the window the recording goes into, or null for whichever free one takes it
   */
  private void playInto(MachineFrame asking, String whenItFails,
                        java.util.concurrent.Callable<Chosen> decide) {
    decided(whenItFails, decide, chosen -> {
      if (asking instanceof Opens itsOwn) {
        itsOwn.open(chosen.file(), chosen.source(), chosen.entry());
      } else {
        open(chosen);
      }
    });
  }

  /** The wait pointer over the whole application, for work nobody can see yet. */
  private void working(boolean busy) {
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    java.awt.Component root = window == null ? this : window;
    root.setCursor(java.awt.Cursor.getPredefinedCursor(busy ? java.awt.Cursor.WAIT_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
    if (root instanceof javax.swing.RootPaneContainer panes) {
      panes.getGlassPane().setCursor(root.getCursor());
      panes.getGlassPane().setVisible(busy);
    }
  }

  /**
   * Puts a machine somebody else built in an ordinary emulator window. It is an emulator like any
   * other from here on - which is what lets a recording be stopped and the game carried on by hand.
   */
  @Override
  public EmulatorWindow show(com.fpetrola.oozx.Speccy machine, String title, MachineFrame asking) {
    com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore core =
        new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(machine);
    machine.control = core;
    registerMachine(core, machine);
    EmulatorInternalFrame window = createNewEmulator(core);
    window.setTitle(title);
    // Put the picture straight above the controls that were already on screen, so the pair comes
    // up joined where the person is looking rather than the picture landing elsewhere and the
    // controls jumping across to it once it is loaded.
    if (asking != null) {
      asking.takeMachine(window);
    }
    return window;
  }

  /** Asked by a window with nothing to play: the desk chooses, fetches and unpacks, it plays. */
  @Override
  public void openRecording(MachineFrame asking) {
    playInto(asking, "Could not open a recording.", this::chooseRecording);
  }

  /** Keeps the recording a player has open, which it is the one that knows. */
  @Override
  public void keepRecording(String url, String entry, String title) {
    if (url == null) {
      JOptionPane.showMessageDialog(this, "There is no recording open to keep.",
          "Favorites", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    String name = title == null ? url : title;
    boolean added = config.addFavorite(new OOZxConfiguration.Favorite(url, name, "RECORDING",
        null, entry));
    JOptionPane.showMessageDialog(this, added ? name + " is now a favourite."
        : name + " was already a favourite.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Which part to play. A game recorded over several sittings arrives as one file per part -
   * Rick Dangerous 2 comes as five - and playing whichever one happened to be picked shows a
   * fifth of the game and looks like a fault, so the choice is the person's.
   */
  private java.nio.file.Path choosePart(String label, java.util.List<java.nio.file.Path> parts,
                                        String preferredEntry) {
    if (parts.size() == 1) {
      return parts.get(0);
    }
    if (preferredEntry != null) {
      java.util.Optional<java.nio.file.Path> remembered = parts.stream()
          .filter(part -> part.getFileName().toString().equals(preferredEntry)).findFirst();
      if (remembered.isPresent()) return remembered.get();
    }
    Object[] names = parts.stream().map(part -> part.getFileName().toString()).toArray();
    Object chosen = JOptionPane.showInputDialog(this,
        label + " was recorded in " + parts.size() + " parts. Which one?",
        "Play recording", JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
    return parts.stream()
        .filter(part -> part.getFileName().toString().equals(chosen))
        .findFirst().orElse(parts.get(0));
  }

  /** What a chosen recording is: the file to play, and what it lasts as - a URL or a zip plus the entry inside it. */
  record Chosen(java.io.File file, String source, String entry) {
  }

  private Chosen chooseRecording() throws java.io.IOException {
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    javax.swing.filechooser.FileFilter previous = fileChooser.getFileFilter();
    // Zips as well as recordings: the archive serves a good part of its catalogue that way, and
    // someone who saved one has a zip on their disk, not a .rzx. Offering only .rzx hid those
    // files from the dialog, and picking one through All Files handed the zip straight to the
    // parser, which rightly says it is not a recording it can read.
    javax.swing.filechooser.FileNameExtensionFilter recordings =
        new javax.swing.filechooser.FileNameExtensionFilter("Recordings (*.rzx, *.zip)", "rzx", "zip");
    fileChooser.addChoosableFileFilter(recordings);
    fileChooser.setFileFilter(recordings);
    int answer = fileChooser.showOpenDialog(this);
    fileChooser.removeChoosableFileFilter(recordings);
    fileChooser.setFileFilter(previous);
    if (answer != JFileChooser.APPROVE_OPTION) {
      return null;
    }
    config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
    return openableRecording(fileChooser.getSelectedFile());
  }

  /**
   * What to hand the player for a file off the disk: the file itself, or, if it is an archive,
   * whichever recording inside it the person picked.
   * <p>
   * Fetching one from the archive already unpacks and asks; opening the same file from disk did
   * not, so the two ways of arriving at the same recording did not agree. A zip holding one
   * recording opens without a question; one holding several asks the same way a download does.
   */
  private Chosen openableRecording(java.io.File chosen) throws java.io.IOException {
    if (chosen == null) {
      return null;
    }
    if (!chosen.getName().toLowerCase().endsWith(".zip")) {
      return new Chosen(chosen, null, null);
    }
    java.util.List<java.nio.file.Path> parts = DownloadAndUnzip.fetchAll(chosen.toURI().toString());
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("there is nothing playable inside it");
    }
    java.io.File part =
        choosePart(chosen.getName(), parts, null).toFile();
    // The unpacked entry is this emulator's copy. What the person has is the zip they picked,
    // plus which recording inside it this was, so a favourite made from here can unpack the
    // same one again.
    return new Chosen(part, chosen.getAbsolutePath(), part.getName());
  }

  /**
   * Opens a machine on a file and lets it load itself, which for a tape means the auto loader
   * types LOAD "" and plays from the start. Off the event thread: building a machine downloads,
   * unzips and boots, and doing that on the event thread freezes the window.
   */
  /**
   * What to tell someone about a failure: what went wrong, not the pile of wrappers that carried
   * it here. A download refused by an archive arrives as an ExecutionException around a
   * RuntimeException around an IOException, and printing that put three Java class names and a
   * full URL in front of the one sentence that says anything.
   */
  static String reason(Throwable failure) {
    return dev.plugdesk.desk.Widgets.reason(failure);
  }

  public void loadInNewEmulator(String path) {
    load(path, null, () -> { }, true);
  }

  /**
   * El unico camino de un archivo a una maquina: se baja si esta lejos, se pregunta si algo lo
   * abre y recien entonces se arma la maquina. Preguntar despues de armarla dejaba un emulador en
   * el BASIC al lado de la oferta del plugin; preguntar antes de bajarlo no sabia que habia en el zip.
   *
   * @param game   lo que el catalogo sabe de el, o null si llego como archivo
   * @param mayAsk si es el primer intento: despues de traer lo que faltaba no se vuelve a preguntar
   */
  private void load(String path, Desk.Game game, Runnable whenDone, boolean mayAsk) {
    String name = DownloadAndUnzip.nameOf(path);
    JDialog loading = showLoading(path.startsWith("http") ? "Fetching " + name + "..." : "Loading " + name + "...");
    new SwingWorker<Object, Void>() {
      private String local;

      @Override
      protected Object doInBackground() {
        local = path.startsWith("http") ? new DownloadAndUnzip().unzip(path).toAbsolutePath().toString() : path;
        java.io.File file = new java.io.File(local);
        if (file.isFile() && !OOSpectrumLauncher.somethingOpens(file)) return file;
        if (mayAsk && file.isFile()) {
          List<dev.plugdesk.app.Releases.Board> beside =
              WhatIsMissing.bringingBeside(com.fpetrola.oozx.plugins.BesideTheGame.kindsBeside(file));
          if (!beside.isEmpty()) return beside;
        }
        EmulatorCore core = mockCore.apply(local, game == null ? null : game.machine());
        knownMachines = core.getMachineModels();
        return core;
      }

      @Override
      protected void done() {
        loading.dispose();
        try {
          Object got = get();
          if (got instanceof java.io.File nobodyOpens) {
            nothingOpens(nobodyOpens, game, whenDone, mayAsk);
            return;
          }
          if (got instanceof List<?> brings) {
            @SuppressWarnings("unchecked")
            List<dev.plugdesk.app.Releases.Board> boards = (List<dev.plugdesk.app.Releases.Board>) brings;
            WhatIsMissing.offer(ZXSpectrumDesktopApp.this, boards,
                "Beside " + DownloadAndUnzip.nameOf(local) + " there is more of the game than this build uses.",
                () -> { somethingWasPluggedIn(); load(local, game, whenDone, false); },
                () -> load(local, game, whenDone, false));
            return;
          }
          if (game != null) createNewEmulator((EmulatorCore) got, game);
          else createNewEmulator((EmulatorCore) got, path);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(ZXSpectrumDesktopApp.this, "Could not load "
              + (game != null ? "\"" + game.title() + "\"" : path) + ".\n\n" + reason(e),
              "Load failed", JOptionPane.ERROR_MESSAGE);
        }
        whenDone.run();
      }
    }.execute();
  }

  /** Lo que falta se ofrece junto con lo que se queria: traido, se abre ese mismo archivo. */
  private void nothingOpens(java.io.File file, Desk.Game game, Runnable whenDone, boolean mayAsk) {
    String saidIt = "Nothing in this build knows how to open " + file.getName() + ".";
    if (!mayAsk) {
      JOptionPane.showMessageDialog(this, saidIt + "\n\nWhat was brought in did not change that.",
          "This build cannot do that", JOptionPane.WARNING_MESSAGE);
      whenDone.run();
      return;
    }
    List<String> wanted = new java.util.ArrayList<>(List.of(OOSpectrumLauncher.kindOf(file)));
    wanted.addAll(WhatIsMissing.nobodyHereAnswers(com.fpetrola.oozx.plugins.BesideTheGame.kindsBeside(file)));
    WhatIsMissing.bringWhatIsNeeded(this, wanted, saidIt,
        () -> {
          somethingWasPluggedIn();
          // Lo que llego puede ser una ventana que lo abre, como el reproductor de grabaciones,
          // y no una maquina que arranca con el: se lo vuelve a abrir por donde entra todo.
          if (has.equipment().stream().anyMatch(kind -> kind.opens(file))) {
            open(new Chosen(file, null, null));
            whenDone.run();
          } else {
            load(file.getAbsolutePath(), game, whenDone, false);
          }
        });
  }

  /**
   * A small window saying something is happening, for work that runs off the event thread.
   * Deliberately not modal: it says wait, it does not take the application away.
   */
  JDialog showLoading(String message) {
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Loading");
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    panel.add(new JLabel(message), BorderLayout.NORTH);

    JProgressBar progress = new JProgressBar();
    progress.setIndeterminate(true);
    panel.add(progress, BorderLayout.CENTER);

    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    return dialog;
  }

  /**
   * A window for this file: one of the kind that takes it with nothing in it yet, or another.
   * <p>
   * Reusing an empty one keeps the menu item from leaving a trail of blank decks behind someone
   * who clicked it twice; opening a second cassette while the first is loading gets its own
   * window, which is the point of there being more than one.
   */
  /** Whoever opens this kind of file, opened but not yet given it. */
  private Opens opensFor(java.io.File file) {
    for (Equipment kind : has.equipment()) {
      if (kind.opens(file)) {
        MachineFrame window = show(kind, open -> open instanceof Opens holds && holds.empty());
        return window instanceof Opens holds ? holds : null;
      }
    }
    return null;
  }

  public MachineFrame openFor(java.io.File file) {
    for (Equipment kind : has.equipment()) {
      if (kind.opens(file)) {
        MachineFrame window = show(kind, open -> open instanceof Opens holds && holds.empty());
        ((Opens) window).open(file);
        return window;
      }
    }
    return null;
  }

  /**
   * Lo que hay para enchufar, recibido de un injector y no buscado. Es el unico lugar del
   * escritorio que lo nombra: de aca para abajo son listas que ya estan al dia solas.
   */
  private final WhatIsPluggedIn has = WhatIsPluggedIn.theOne();

  /**
   * Los menus se rearman cuando cambia lo enchufado, lo diga la ventana de plugins o lo haga
   * alguien por codigo: quien los maneja avisa, asi que no hay que preguntarle a cada rato.
   */
  private final AutoCloseable whileItChanges = dev.plugdesk.app.Plugins.managing()
      .onChange(() -> javax.swing.SwingUtilities.invokeLater(this::somethingWasPluggedIn));

  {
    dev.plugdesk.app.Plugins.whenHeld(leaving -> putMachinesAway());
    dev.plugdesk.app.Plugins.whenCatalogArrives(() -> SwingUtilities.invokeLater(() -> offered.look(workbench::changed)));
  }


  /** Said once the folder changed: the menus offer what is there now, and only that. */
  public void somethingWasPluggedIn() {
    whatThereIs();
    refillMachineToolBars();
    workbench.changed();
    // What could not be done before may be done now, so it is worth saying again if it is not.
    TellsThePerson.thatMayHaveChanged();
    offered.look(workbench::changed);
  }

  private void refillMachineToolBars() {
    for (JInternalFrame frame : desktop.getAllFrames()) {
      if (frame instanceof EmulatorInternalFrame machine) machine.refillTools();
    }
  }


  /**
   * What was found, handed to whoever cannot go looking for it.
   * <p>
   * The screen knows about pictures and windows and nothing about jars, so it is told what
   * there is instead of asking. Said again whenever that changes. The snapshot formats are told
   * by the module that loads snapshots, which is the one that owns the question.
   */
  private void whatThereIs() {
    com.fpetrola.oozx.speccy.screen.ScreenSettings.thereAre(has.effects());
    com.fpetrola.emulation.helpers.snapshots.SnapshotFactory.alsoRead(has::formats);
  }


  /**
   * Every piece of equipment open at once, one per machine, clipped on the same way a deck is.
   * <p>
   * Kept under the name rather than the offer itself: what is offered is asked for again whenever
   * the plugins change, and two offers of the same board are the same board as far as this goes.
   */
  private final java.util.Map<String, java.util.List<MachineFrame>> equipment = new java.util.HashMap<>();

  /**
   * That piece of equipment, clipped onto the machine in front - which is what plugs it in.
   * Opening the one that is already there rather than a second one, the way the cassette does:
   * two mice on one Spectrum would be two sets of counters answering the same three ports.
   */
  public MachineFrame show(Equipment kind) {
    return show(kind, window -> !window.isAttached() || window.getMachineWindow() == machineBeingUsed());
  }

  /**
   * @param reusable which of the ones already open counts as the same window being asked for
   */
  private MachineFrame show(Equipment kind, java.util.function.Predicate<MachineFrame> reusable) {
    java.util.List<MachineFrame> open = equipment.computeIfAbsent(kind.name(), k -> new java.util.ArrayList<>());
    open.removeIf(JInternalFrame::isClosed);
    for (MachineFrame window : open) {
      if (reusable.test(window)) {
        window.setVisible(true);
        window.toFront();
        return window;
      }
    }

    MachineFrame window = another(kind);
    if (machineBeingUsed() != null) {
      window.attachTo(machineBeingUsed());
    }
    return window;
  }

  /** One more window of that kind on the desk, clipped onto nothing yet. */
  private MachineFrame another(Equipment kind) {
    java.util.List<MachineFrame> open = equipment.computeIfAbsent(kind.name(), k -> new java.util.ArrayList<>());
    MachineFrame window = kind.open();
    window.setLocation(360 + (open.size() * 30) % 300, 60 + (open.size() * 30) % 200);
    open.add(window);
    desktop.add(window);
    window.setVisible(true);
    return window;
  }

  @Override
  public MachineFrame equipment(String name) {
    return has.equipment().stream().filter(kind -> kind.name().equals(name)).findFirst().map(this::another).orElse(null);
  }

  @Override
  public java.util.Map<MachineFrame, String> equipmentOn(EmulatorWindow machine) {
    java.util.Map<MachineFrame, String> on = new java.util.LinkedHashMap<>();
    equipment.forEach((kind, open) -> open.stream()
        .filter(window -> !window.isClosed() && window.isAttached() && window.getMachineWindow() == machine)
        .forEach(window -> on.put(window, kind)));
    return on;
  }

  /**
   * The window that asked for a computer with nothing to play into, while that computer is being
   * built.
   * <p>
   * Without it, the machine came up and the cassette that had asked for it was handed a second
   * deck holding the same tape: the one you pressed play on stayed where it was, unplugged, and
   * a new one appeared under the new machine.
   */
  private com.fpetrola.oozx.speccy.devices.MachineFrame windowWantingAMachine;

  /** Asked by a window with no machine to play into: one is built and that window gets it. */
  @Override
  public void openMachineFor(java.io.File file, com.fpetrola.oozx.speccy.devices.MachineFrame asking) {
    windowWantingAMachine = asking;
    loadInNewEmulator(file.getAbsolutePath());
  }

  /** A file from whoever is in front, remembering where they were looking. */
  @Override
  public java.io.File choose(String what) {
    fileChooser.setDialogTitle(what);
    fileChooser.setCurrentDirectory(new java.io.File(config.getLastOpenDirectory()));
    if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return null;
    }
    config.setLastOpenDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
    return fileChooser.getSelectedFile();
  }


  /**
   * Sends what is typed to the machine being used, whatever happens to hold the focus.
   * <p>
   * The keys used to be wired to the emulator's own panel gaining and losing focus, which made
   * every window that appears beside a machine able to take its keyboard away: clicking a
   * cassette deck clipped under it left the machine deaf until somebody clicked the picture
   * again, and each such window had to be taught to hand the keys back. What somebody means by
   * "the machine I am typing into" is the machine in front, or the machine that the thing in
   * front is clipped onto - not whichever component Swing last gave the focus to.
   */
  private boolean typeIntoTheMachineInFront(KeyEvent event) {
    // Run with -Doozx.keys=true to watch what the keyboard is actually sending, which is the only
    // way to tell a key this build is reading wrongly from one the desktop is sending twice.
    if (Boolean.getBoolean("oozx.keys")) {
      System.out.printf("key %s code=%d (%s) char=%d (%s) modifiers=%s%n",
          event.getID() == KeyEvent.KEY_PRESSED ? "down" : event.getID() == KeyEvent.KEY_RELEASED ? "up  " : "typed",
          event.getKeyCode(), KeyEvent.getKeyText(event.getKeyCode()), (int) event.getKeyChar(),
          event.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? "none" : String.valueOf(event.getKeyChar()),
          KeyEvent.getModifiersExText(event.getModifiersEx()));
    }
    if (somebodyIsWriting()) {
      return false;
    }
    EmulatorInternalFrame machine = machineBeingUsed();
    KeyListener keys = machine == null ? null : machine.keys();
    if (keys == null) {
      return false;
    }
    switch (event.getID()) {
      case KeyEvent.KEY_PRESSED -> keys.keyPressed(event);
      case KeyEvent.KEY_RELEASED -> keys.keyReleased(event);
      case KeyEvent.KEY_TYPED -> keys.keyTyped(event);
      default -> { }
    }
    return false;
  }

  /** Which machine windows already had each kind opened for them, so closing it keeps it closed. */
  private final java.util.Map<JInternalFrame, java.util.Set<String>> openedFor = new java.util.WeakHashMap<>();

  private void openWhatTheMachineUses() {
    EmulatorInternalFrame front = machineBeingUsed();
    if (front == null || front.machine() == null) return;
    for (Equipment kind : has.equipment()) {
      if (kind.usedBy(front.machine()) && openedFor.computeIfAbsent(front, k -> new java.util.HashSet<>()).add(kind.name())
          && !equipmentOn(front).containsValue(kind.name())) {
        MachineFrame window = show(kind);
        window.setSize(Math.max(front.getWidth() / 3, 280), front.getHeight() / 2);
        window.attachTo(front, com.fpetrola.oozx.speccy.windows.AttachedFrame.Dock.RIGHT);
      }
    }
  }

  /** The machine in front, or the machine whatever is in front is clipped onto. */
  private EmulatorInternalFrame machineBeingUsed() {
    JInternalFrame selected = desktop == null ? null : desktop.getSelectedFrame();
    if (selected instanceof EmulatorInternalFrame machine && !machine.isClosed()) {
      return machine;
    }
    if (selected instanceof AttachedFrame clipped && clipped.isAttached()
        && clipped.getMachineWindow() instanceof EmulatorInternalFrame machine) {
      return machine;
    }
    return null;
  }

  /**
   * Whether the keys belong to somebody writing rather than to a machine: a box being typed in,
   * a list being chosen from, or a dialog that has taken the keyboard for itself.
   */
  private static boolean somebodyIsWriting() {
    KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    Window window = focus.getFocusedWindow();
    if (window instanceof Dialog dialog && dialog.isModal()) {
      return true;
    }
    Component owner = focus.getFocusOwner();
    return owner instanceof javax.swing.text.JTextComponent
        || owner instanceof JComboBox<?> list && (list.isEditable() || list.isPopupVisible());
  }

  /** Placed, shown and clipped onto the machine in front, which is what wires it to that machine. */
  private <T extends AttachedFrame> T clipOntoTheMachineInFront(T window) {
    window.setLocation(60 + (desktop.getAllFrames().length * 20) % 200,
        60 + (desktop.getAllFrames().length * 20) % 160);
    desktop.add(window);
    window.setVisible(true);
    window.setMachineWindow(getActiveEmulator());
    window.toFront();
    return window;
  }

  /**
   * The deck that goes with a machine built on a cassette.
   * <p>
   * The window that pressed play stays where it was left and the machine is placed above it;
   * one opened along with the machine goes under it, because it has not been put anywhere yet.
   */
  private void cassetteFor(EmulatorInternalFrame machine, String path) {
    MachineFrame asked = windowWantingAMachine;
    windowWantingAMachine = null;
    if (asked != null && !asked.isClosed()) {
      asked.takeMachine(machine);
      return;
    }
    java.io.File file = path == null ? null : new java.io.File(path);
    if (file == null || !file.isFile()) {
      return;
    }
    MachineFrame window = openFor(file);
    if (window != null) {
      window.attachTo(machine);
    }
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1) {
    return createNewEmulator(core1, (String) null);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, String filePath) {
    return createNewEmulator(core1, filePath, null);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, Desk.Game game) {
    return createNewEmulator(core1, null, game);
  }

  public EmulatorInternalFrame createNewEmulator(EmulatorCore core1, String filePath, Desk.Game game) {
    EmulatorCore core = core1;

    if (filePath != null && !filePath.isEmpty()) {
      core.setFilename(filePath);
    }

    if (config.isTurboByDefault()) core.setGeneralOption("turbo", true);
    JComponent panel = core.getPanel();
    int x = (emulatorCount * 30) % 400;
    int y = (emulatorCount * 30) % 300;
    EmulatorInternalFrame frame = new EmulatorInternalFrame(core, x, y, this, game);
    frame.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosed(InternalFrameEvent e) {
        core1.finishEmulation();
        frame.stopsBeingTold();
        forgetWhatIsClosed(core1);
        WhatIsPluggedIn.theOne().tools().forEach(tool -> tool.closed(frame));
        frame.letGoOfTheMachine();
        frame.removeInternalFrameListener(this);
      }
    });

    // Focusable so that clicking the picture still raises its window; the keys themselves no
    // longer depend on it holding the focus - see typeIntoTheMachineInFront.
    panel.setFocusable(true);

    desktop.add(frame);
    frame.pack();
    frame.setVisible(true);
    emulatorCount++;
    // A machine built on a cassette comes with a deck showing it: the window that asked for the
    // machine, if one did, or a new one. Done here, where every machine passes, rather than at
    // each of the places one gets built.
    cassetteFor(frame, filePath != null && !filePath.isEmpty() ? filePath : core.getFilename());

    final String finalFilePath = filePath;
    final EmulatorCore finalCore = core;

    Timer stateCapture = new Timer(500, e -> {
      try {
        String actualFilePath = finalFilePath;
        if (actualFilePath == null || actualFilePath.isEmpty()) {
          actualFilePath = finalCore.getFilename();
        }

        if (actualFilePath != null && !actualFilePath.isEmpty()) {
          String gameName = new java.io.File(actualFilePath).getName();

          String initialStateData = null;
          try {
            initialStateData = finalCore.packedState();
          } catch (Exception ex) {
            System.err.println("Error capturando estado inicial: " + ex.getMessage());
          }

          config.addToSnapshotHistory(actualFilePath, gameName, initialStateData);
        }
      } finally {
        ((Timer) e.getSource()).stop();
      }
    });
    stateCapture.setRepeats(false);
//    stateCapture.start();

    return frame;
  }

  private void saveOpenWindows() {
    config.getOpenWindows().clear();
    config.getOpenWindows().addAll(keep(session(), backToFront(), sessionFolder()));
  }

  /** Every window on the desk, from the back to the front, which is the order they are kept and brought back in. */
  public List<JInternalFrame> backToFront() {
    return List.of(desktop.getAllFrames()).reversed();
  }

  private static File sessionFolder() {
    return new File(dev.plugdesk.app.Configuration.home(), "session");
  }

  private static com.fpetrola.oozx.speccy.devices.Session session() {
    return dev.plugdesk.app.Plugins.found(com.fpetrola.oozx.speccy.devices.Session.class)
        .stream().findFirst().orElse(null);
  }

  /**
   * Lo que se guardo al cerrar. Las maquinas que se guardaron empaquetadas en la configuracion,
   * antes de que la sesion fuera archivos, vuelven una vez por donde volvian.
   */
  private void restoreOpenWindows() {
    List<OOZxConfiguration.WindowState> kept = config.getOpenWindows();
    kept.stream().filter(ZXSpectrumDesktopApp::packed).forEach(this::reopenPacked);
    bringBack(session(), kept.stream().filter(state -> !packed(state)).toList());
  }

  private static boolean packed(OOZxConfiguration.WindowState windowState) {
    return windowState.getSnapshotId() != null && !windowState.getSnapshotId().isEmpty();
  }

  private void reopenPacked(OOZxConfiguration.WindowState windowState) {
    try {
      String snapshotData = config.getSnapshot(windowState.getSnapshotId());
      if (snapshotData != null && !snapshotData.isEmpty()) {
        EmulatorInternalFrame window = createNewEmulator(mockCoreState.apply(snapshotData), windowState.getFilePath());
        if (windowState.getWidth() > 0) {
          window.setBounds(windowState.getX(), windowState.getY(), windowState.getWidth(), windowState.getHeight());
        }
      }
    } catch (Exception e) {
      System.err.println("Error restaurando snapshot desde configuración: " + e.getMessage());
    }
  }

  /** El archivo no es de donde salio la maquina: eso lo dice la sesion. */
  @Override
  public EmulatorWindow machineFrom(File file) {
    EmulatorCore core = mockCore.apply(file.getPath(), null);
    core.setFilename(null);
    return createNewEmulator(core);
  }

  /**
   * Guarda cada maquina abierta y la cierra; lo devuelto las vuelve a abrir desde ahi. Una maquina
   * retiene los plugins con que se armo, y esto es lo que la deja rearmarse sin uno. La que nadie
   * puede guardar queda como esta.
   */
  Runnable putMachinesAway() {
    File away;
    try {
      away = java.nio.file.Files.createTempDirectory("oozx-away").toFile();
    } catch (java.io.IOException cannot) {
      return () -> { };
    }
    List<OOZxConfiguration.WindowState> were = new ArrayList<>();
    for (JInternalFrame frame : backToFront()) {
      if (!(frame instanceof EmulatorInternalFrame machine)) continue;
      OOZxConfiguration.WindowState state = keep(session(), machine, away, were.size());
      if (state == null) continue;
      were.add(state);
      machine.dispose();
    }
    // Lo que las maquinas dejaron encolado para este hilo las nombra: se atiende antes de volver,
    // sin salir del evento que pregunto, como hace un dialogo modal.
    if (SwingUtilities.isEventDispatchThread()) {
      java.awt.SecondaryLoop pending = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
      SwingUtilities.invokeLater(pending::exit);
      pending.enter();
    }
    return () -> {
      bringBack(session(), were);
      were.forEach(state -> new File(state.getFilePath()).delete());
      away.delete();
    };
  }


  public static void main(String[] args) {
  }
}