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


package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.oozx.TellsThePerson;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.display.Colouring;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Painting;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.peripherals.FilesOfItsOwn;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A game in 256 colours for as long as one is loaded: a snapshot with its colours in a file
 * beside it puts the machine on the nine processors, and anything else puts it back where it was.
 * <p>
 * It has no ports and no machine has it on board. What it is, is a session: it starts when the
 * colours arrive and ends at a reset, at a change of machine, or at a snapshot that brought none.
 * While it lasts it is also who paints the screen, because the pixels are on its planes and not
 * in the machine's memory.
 */
@Singleton
public class Spec256Peripheral extends AbstractPeripheral implements FilesOfItsOwn, Painting.Line {
  private static final String COLOURS = ".gfx";
  private static final String SAYS = ".cfg";
  /**
   * Where a game keeps the colours of the letters and shapes the machine itself draws with.
   * <p>
   * Two spellings for the same thing. One game calls it after the ROM it belongs to, the way a
   * 128K names its two - {@code .gfa} the first and {@code .gfb} the second, which is the one a
   * 48K machine has and so the one to look for first. Another calls it {@code rom0.gfx} and does
   * not put the game's name on it at all.
   */
  private static final String[] ROM_COLOURS = {".gfb", ".gfa"};
  private static final String ANY_ROM = "rom0.gfx";
  /** A background is 320 by 200 of a colour each, laid under the screen and centred on it. */
  private static final int BACKGROUND_WIDTH = 320, BACKGROUND_HEIGHT = 200;
  private static final int BACKGROUND_SIZE = BACKGROUND_WIDTH * BACKGROUND_HEIGHT;
  private static final int LEFT_OF_THE_SCREEN = Display.BORDER_WIDTH_COLS * 8;
  private static final int ABOVE_THE_SCREEN = Display.BORDER_HEIGHT - (Display.SCREEN_HEIGHT - BACKGROUND_HEIGHT) / 2;

  private final Planes planes;
  private final Processors processors;
  private final Display display;
  private final SpectrumMemory banks;
  private final Alignment alignment;
  private final Rules rules;
  private final List<byte[]> backgrounds = new ArrayList<>();
  private int showing;
  private String playing;
  private String wasOn;
  private int bitmap, ink, paper;
  private boolean flashedAway;
  private boolean inItsColours = true;

  @Inject
  public Spec256Peripheral(Planes planes, Processors processors, Display display, SpectrumMemory banks,
                           Alignment alignment, Rules rules) {
    super(List.of());
    this.planes = planes;
    this.processors = processors;
    this.display = display;
    this.banks = banks;
    this.alignment = alignment;
    this.rules = rules;
  }

  /** Every machine, because what decides is the file beside the snapshot and not the hardware. */
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }

  /**
   * Whether a game has colours of its own beside it, which is the same question this peripheral
   * asks before taking them: a file with the game's name and {@code .gfx} next to it. Anything
   * that wants to say a game is in 256 colours - a filter, a menu - asks here rather than knowing
   * what the file is called.
   */
  public static boolean hasColours(String url) {
    return withTheSameName(url, COLOURS) != null;
  }

  public void beside(String url) {
    File colours = withTheSameName(url, COLOURS);
    if (colours == null) {
      over();
      return;
    }
    try {
      planes.take(Files.readAllBytes(colours.toPath()));
    } catch (IOException | IllegalArgumentException notColoursAfterAll) {
      TellsThePerson.that("%s is beside the snapshot but is not a game's colours: %s".formatted(colours, notColoursAfterAll.getMessage()));
      over();
      return;
    }
    playing = colours.getName();
    romBeside(colours);
    backgroundsBeside(colours);
    rules.read(withTheSameName(url, SAYS));
    takeWhatTheGameAsksFor();
    if (wasOn == null) wasOn = processors.current();
    processors.use(Spec256Core.NAME);
    paintFromThePlanes(inItsColours);
  }

  /** Whether the screen is the game's colours or the ones the machine itself would show. */
  public boolean inItsColours() {
    return inItsColours;
  }

  /**
   * The switch every Spec256 emulator has: the same game either way and the followers keep
   * running, so it is only a question of who paints and out of which palette.
   */
  public void inItsColours(boolean colourful) {
    inItsColours = colourful;
    if (playing != null) paintFromThePlanes(colourful);
  }

  private void paintFromThePlanes(boolean colourful) {
    display.painting.line(colourful ? this : null);
    if (colourful) theTwoHundredAndFiftySixColours();
    else display.picture().sinclairColours();
    display.refreshAll();
  }

  /**
   * The eight pixels of each dirty column, a colour each, taken one bit out of each of the eight
   * planes, and then whatever the game's own rules have to say about how they meet the attribute.
   */
  public void paint(int y, int bits) {
    byte[] shown = banks.shown().bytes;
    boolean reversed = display.colouring.reversed();
    Picture canvas = display.picture();
    int row = (y + Display.BORDER_HEIGHT) * Picture.STRIDE;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      int at = display.layout.pixelsAt(y, x);
      byte attribute = shown[display.layout.colourAt(y, x)];
      bitmap = shown[at] & 0xff;
      ink = attributeColour(attribute, attribute & 0x07);
      paper = attributeColour(attribute, (attribute >> 3) & 0x07);
      flashedAway = rules.pictureThroughTheFlash && Colouring.flashes(attribute) && reversed;
      int address = Planes.RAM + at;
      int pixel = row + (x + Display.BORDER_WIDTH_COLS) * 8;
      for (int i = 0; i < 8; i++) {
        canvas.pixels[pixel + i] = rgbOf(address, x, y, i);
      }
    }
  }

  /** The planes move where the machine's memory does not, so no write says which pixel changed. */
  @Override
  public boolean allOfItEveryFrame() {
    return true;
  }

  /** One of the machine's own sixteen, which is what the game's colours are mixed with. */
  private int attributeColour(byte attribute, int which) {
    return Picture.SINCLAIR[rules.brightInTheMix && (attribute & 0x40) != 0 ? which + 8 : which];
  }

  /**
   * What one pixel comes out as. The colour on the planes to begin with, then the game's rules:
   * which colours the cell's own ink and paper stand in for, which ones let the picture under the
   * screen through, and which ones are mixed half and half with what the machine would have
   * painted there.
   */
  private int rgbOf(int address, int x, int y, int pixel) {
    int colour = planes.colourOf(address, pixel);
    int rgb = display.picture().palette[colour];
    boolean underneath = !backgrounds.isEmpty();
    boolean covered = rules.hiddenWhereInkIsPaper && ink == paper && planes.noColoursOfItsOwn(address);
    boolean draw = true;
    if (!underneath) {
      if (covered) {
        rgb = ink;
      } else if (rules.paperForNoneInkForAll) {
        if (colour == 0) rgb = paper;
        else if (colour == 255) rgb = ink;
      }
    } else if (rules.paperForNoneInkForAll) {
      if (colour == 0) rgb = paper;
      else if (colour == 255) rgb = ink;
      else draw = !(flashedAway || covered);
    } else {
      draw = !(flashedAway || covered || colour == 0 || (rules.backgroundOverTheLast && colour == 255));
    }
    if (draw && rules.mixed(colour)) {
      rgb = halfway((bitmap & (0x80 >> pixel)) != 0 ? ink : paper, rgb);
    }
    return draw ? rgb : under(x, y, pixel);
  }

  /** The picture that lies under the screen, at the pixel the screen has there, or nothing. */
  private int under(int x, int y, int pixel) {
    int row = y + ABOVE_THE_SCREEN, column = x * 8 + pixel + LEFT_OF_THE_SCREEN;
    if (row < 0 || row >= BACKGROUND_HEIGHT || column < 0 || column >= BACKGROUND_WIDTH) return 0;
    return display.picture().palette[backgrounds.get(showing)[row * BACKGROUND_WIDTH + column] & 0xff];
  }

  private static int halfway(int one, int other) {
    return ((one >> 16 & 0xff) + (other >> 16 & 0xff)) / 2 << 16
        | ((one >> 8 & 0xff) + (other >> 8 & 0xff)) / 2 << 8
        | ((one & 0xff) + (other & 0xff)) / 2;
  }

  /**
   * How many cells of the screen have no colours of their own, out of the 768 there are. A shape
   * that looks white and black is either a white shape the game drew that way or a shape whose
   * colours never arrived, and this is the difference.
   */
  public int cellsWithNoColours() {
    int without = 0;
    for (int line = 0; line < Display.HEIGHT; line += 8) {
      for (int column = 0; column < Display.SCREEN_WIDTH_COLS - 2 * Display.BORDER_WIDTH_COLS; column++) {
        if (noColoursAnywhereIn(line, column)) without++;
      }
    }
    return without;
  }

  private boolean noColoursAnywhereIn(int line, int column) {
    boolean anything = false;
    for (int within = 0; within < 8; within++) {
      int address = Planes.RAM + display.layout.pixelsAt(line + within, column);
      if (!planes.noColoursOfItsOwn(address)) {
        if (planes.colourOf(address, 0) != 0 || banks.shown().bytes[address - Planes.RAM] != 0) return false;
      } else {
        anything = true;
      }
    }
    return anything;
  }

  /** How many pictures lie under this game's screen, and which of them is showing. */
  public int backgrounds() {
    return backgrounds.size();
  }

  public int showing() {
    return showing;
  }

  public void show(int background) {
    if (background < 0 || background >= backgrounds.size()) return;
    showing = background;
    display.refreshAll();
  }

  /**
   * The colours of the ROM, which are not named after the game: every one of them that has any
   * calls the file the same thing, because the ROM is the machine's and not the game's.
   */
  private void romBeside(File colours) {
    for (String ending : ROM_COLOURS) {
      if (takeTheRomFrom(withTheSameName(colours.getPath(), ending))) return;
    }
    File[] beside = colours.getParentFile().listFiles();
    if (beside == null) return;
    for (File file : beside) {
      if (file.getName().equalsIgnoreCase(ANY_ROM) && takeTheRomFrom(file)) return;
    }
  }

  private boolean takeTheRomFrom(File file) {
    if (file == null) return false;
    try {
      planes.takeTheRom(Files.readAllBytes(file.toPath()));
      return true;
    } catch (IOException | IllegalArgumentException notTheRomsColours) {
      TellsThePerson.that("%s is not this ROM's colours: %s".formatted(file, notTheRomsColours.getMessage()));
      return false;
    }
  }

  /**
   * The pictures that lie under the screen, in files named after the game and numbered. Whatever
   * is not the right size is somebody else's file with an unlucky name.
   */
  private void backgroundsBeside(File colours) {
    backgrounds.clear();
    showing = 0;
    String stem = withoutItsEnding(colours.getName());
    File[] beside = colours.getParentFile().listFiles();
    if (beside == null) return;
    File[] sorted = beside.clone();
    Arrays.sort(sorted, (one, other) -> one.getName().compareToIgnoreCase(other.getName()));
    for (File file : sorted) {
      String name = file.getName();
      if (!withoutItsEnding(name).equalsIgnoreCase(stem) || !name.matches("(?i).*\\.b[0-9][0-9]")) continue;
      try {
        byte[] picture = Files.readAllBytes(file.toPath());
        if (picture.length == BACKGROUND_SIZE) backgrounds.add(picture);
      } catch (IOException unreadable) {
        TellsThePerson.that("%s looks like a background of this game but cannot be read: %s".formatted(file, unreadable.getMessage()));
      }
    }
  }

  /**
   * The colours a Spec256 game is painted in. They belong to the format rather than to the game,
   * so they are the emulator's and are the same for every one of them.
   */
  private void theTwoHundredAndFiftySixColours() {
    try (InputStream from = Spec256Peripheral.class.getResourceAsStream("/spec256/spec256.pal")) {
      byte[] palette = from.readAllBytes();
      for (int colour = 0; colour < palette.length / 3; colour++) {
        display.picture().colour(colour, (palette[colour * 3] & 0xff) << 16
            | (palette[colour * 3 + 1] & 0xff) << 8 | (palette[colour * 3 + 2] & 0xff));
      }
    } catch (IOException | NullPointerException itIsNotThere) {
      TellsThePerson.that("the 256 colours of a Spec256 game are not in this build: %s".formatted(itIsNotThere));
    }
  }

  private static String withoutItsEnding(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? name : name.substring(0, dot);
  }

  /** Which game's colours are loaded, or nothing, for whoever is showing what is going on. */
  public String playing() {
    return playing;
  }

  /** What this game said about its colours, and what its followers take, for whoever shows them. */
  public Rules rules() {
    return rules;
  }

  public Alignment alignment() {
    return alignment;
  }

  /**
   * The letters a game's file gives for what its followers take, as they stand - a game that
   * brought no file says the default. Said again from a window they replace what the file said,
   * because most games need a line of their own and few bring one.
   */
  public String registersTaken() {
    return rules.registersTaken;
  }

  public void registersTaken(String letters) {
    alignment.says(letters);
    rules.registersTaken = letters;
    takeWhatTheGameAsksFor();
  }

  /** Whether a follower reads where the machine reads, except from a table every plane shares. */
  public boolean readsWhereTheMachineReads() {
    return rules.readingWhereTheMachineReads;
  }

  public void readsWhereTheMachineReads(boolean whereTheMachineReads) {
    rules.readingWhereTheMachineReads = whereTheMachineReads;
    display.refreshAll();
  }

  private void takeWhatTheGameAsksFor() {
    alignment.says(rules.registersTaken);
  }

  @Override
  public void machineWasReset(boolean hard) {
    over();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    over();
  }

  @Override
  public void deactivate() {
    over();
  }

  /** Back to the processor the machine was on, and the room the colours took given back. */
  private void over() {
    if (wasOn == null) return;
    processors.use(wasOn);
    wasOn = null;
    playing = null;
    backgrounds.clear();
    planes.blank();
    rules.asTheyComeByDefault();
    takeWhatTheGameAsksFor();
    display.painting.line(null);
    display.picture().sinclairColours();
    display.refreshAll();
  }

  /**
   * The colours of this game: the same name as the snapshot with another ending. Which letters
   * they are written in is the operating system's business and not the game's.
   */
  private static File withTheSameName(String url, String ending) {
    File snapshot = new File(url);
    File where = snapshot.getParentFile();
    if (where == null) return null;
    String wanted = withoutItsEnding(snapshot.getName()) + ending;
    File[] beside = where.listFiles();
    if (beside == null) return null;
    for (File file : beside) {
      if (file.getName().equalsIgnoreCase(wanted)) return file;
    }
    return null;
  }
}
