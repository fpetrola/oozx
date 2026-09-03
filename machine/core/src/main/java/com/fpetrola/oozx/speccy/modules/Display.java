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

package com.fpetrola.oozx.speccy.modules;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class Display implements ZxModule, MachineChangeListener {
  private final Memory memory;

  // Constants for the width and height of the Speccy's screen
  private final int WIDTH_COLS = 32;
  private final int HEIGHT_ROWS = 24;

  private final int WIDTH = WIDTH_COLS * 16;
  public final int HEIGHT = HEIGHT_ROWS * 8;

  // Constants for the width and height of the emulated border
  public final int BORDER_WIDTH_COLS = 4;
  private final int BORDER_HEIGHT_COLS = 3;

  private final int BORDER_WIDTH = BORDER_WIDTH_COLS * 16;
  private final int BORDER_ASPECT_WIDTH = BORDER_WIDTH_COLS * 8;
  public final int BORDER_HEIGHT = BORDER_HEIGHT_COLS * 8;

  // Constants for the width and height of the window we'll be displaying
  public final int SCREEN_WIDTH = WIDTH + 2 * BORDER_WIDTH;
  public final int SCREEN_HEIGHT = HEIGHT + 2 * BORDER_HEIGHT;

  private final int SCREEN_WIDTH_COLS = WIDTH_COLS + 2 * BORDER_WIDTH_COLS;

  // Aspect ratio corrected display width
  public final int ASPECT_WIDTH = SCREEN_WIDTH / 2;

  // Set once we have initialized the UI
  private boolean uiInitialised;

  // The current border color
  private byte loresBorder;
  private byte hiresBorder;
  private byte lastBorder;

  // Stores the pixel, attribute, and SCLD screen mode information used to
  // draw each 8x1 group of pixels (including border) last frame
  private final int[] lastScreen = new int[SCREEN_WIDTH_COLS * SCREEN_HEIGHT];

  // Offsets as to where the data and the attributes for each pixel line start
  public int[] lineStart = new int[HEIGHT];
  public int[] attrStart = new int[HEIGHT];

  // If you write to the byte at display_dirty_?table[n+0x4000], then
  // the eight pixels starting at (8*xtable[n],ytable[n]) must be replotted
  private final int[] dirtyYtable = new int[WIDTH_COLS * HEIGHT];
  private final int[] dirtyXtable = new int[WIDTH_COLS * HEIGHT];

  // If you write to the byte at display_dirty_?table2[n+0x5800], then
  // the 64 pixels starting at (8*xtable2[n],ytable2[n]) must be replotted
  private final int[] dirtyYtable2 = new int[WIDTH_COLS * HEIGHT_ROWS];
  private final int[] dirtyXtable2 = new int[WIDTH_COLS * HEIGHT_ROWS];

  // The number of frames mod 32 that have elapsed
  private int frameCount;
  private boolean flashReversed;

  // Which eight-pixel chunks on each line (including border) need to be redisplayed
  private final long[] isDirty = new long[SCREEN_HEIGHT];

  // Which eight-pixel chunks on each line may need to be redisplayed
  private final int[] maybeDirty = new int[HEIGHT];

  // This value signifies that the entire line must be redisplayed
  private long allDirty;

  // Used to signify that we're redrawing the entire screen
  private boolean redrawAll;

  // The last point at which we updated the screen display
  private int criticalRegionX;
  private int criticalRegionY;
  private final Z80Clock z80Clock;
  private final UiDisplay uiDisplay;
  private final byte[][] ram;
  private final BeanPosition beam;
  private SpectrumMachine spectrumMachine;

@Inject
  public Display(Memory memory, Z80Clock z80Clock, RAMHolder ramHolder, UiDisplay uiDisplay) {
    this.memory = memory;
    this.z80Clock = z80Clock;
    this.uiDisplay = uiDisplay;
    this.ram = ramHolder.getRAM();
    beam = new BeanPosition();
  }

  public void start() {
    int i, j, k, x, y;

    // Set up the 'all pixels must be refreshed' marker
    allDirty = 0;
    for (i = 0; i < SCREEN_WIDTH_COLS; i++) {
      allDirty = (allDirty << 1) | 0x01;
    }

    for (i = 0; i < 3; i++) {
      for (j = 0; j < 8; j++) {
        for (k = 0; k < 8; k++) {
          lineStart[(64 * i) + (8 * j) + k] = 32 * ((64 * i) + j + (k * 8));
        }
      }
    }

    for (y = 0; y < HEIGHT; y++) {
      attrStart[y] = 6144 + (32 * (y / 8));
    }

    for (y = 0; y < HEIGHT; y++) {
      for (x = 0; x < WIDTH_COLS; x++) {
        dirtyYtable[lineStart[y] + x] = y;
        dirtyXtable[lineStart[y] + x] = x;
      }
    }

    for (y = 0; y < HEIGHT_ROWS; y++) {
      for (x = 0; x < WIDTH_COLS; x++) {
        dirtyYtable2[(32 * y) + x] = y * 8;
        dirtyXtable2[(32 * y) + x] = x;
      }
    }

    frameCount = 0;
    flashReversed = false;

    refreshAll();

    borderChangesUsed = 0;
    if (addBorderSentinel() != 0) {
      throw new IllegalStateException("the display could not record where the border starts");
    }
    lastBorder = loresBorder;

    return;
  }

  @Override
  public void end() {

  }

  @Override
  public void machineChanged(SpectrumMachine newMachine) {
    this.spectrumMachine = newMachine;
  }

  // Structure for border change
  private class BorderChange {
    int x, y, colour;
  }

  private final BorderChange BORDER_CHANGE_END_SENTINEL =
      new BorderChange() {{
        x = SCREEN_WIDTH_COLS;
        y = SCREEN_HEIGHT - 1;
        colour = 0;
      }};

  // The current border color array
  private final int[][] currentBorder = new int[SCREEN_HEIGHT][SCREEN_WIDTH_COLS];

  List<BorderChange> borderChanges = new ArrayList<>();

  public void dirtySinclair(final int offset) {
    if (offset < 0x1800) {
      dirty8(offset);
    } else {
      dirty64(offset);
    }
  }

  private byte getAttrByte(int x, int y) {
    int attr;
    int offset;
    offset = attrStart[y] + x;
    attr = ram[memory.currentScreen][offset];
    return (byte) attr;
  }

  public void writeIfDirtySinclair(int x, int y) {
    int beamX = x + BORDER_WIDTH_COLS;
    int beamY = y + BORDER_HEIGHT;
    int offset = getOffset(x, y);

    byte[] screen = ram[memory.currentScreen];
    int data = screen[offset] & 0xff;
    byte data2 = getAttrByte(x, y);

    int lastChunkDetail = ((flashReversed ? 1 : 0) << 24) | ((data2 & 0xFF) << 8) | (data & 0xFF);
    int index = beamX + beamY * SCREEN_WIDTH_COLS;
    if (lastScreen[index] != lastChunkDetail) {
      byte ink = ink(data2), paper = paper(data2);
//            System.err.printf("display_write_if_dirty_sinclair: x=%d y=%d data=%02x attr=%02x ink=%d paper=%d\n", x, y, data, data2, ink, paper );
      uiDisplay.plot8(beamX, beamY, (byte) (data & 0xff), ink, paper);
      lastScreen[index] = lastChunkDetail;
      isDirty[beamY] |= (1L << beamX);
    }
  }

  private void copyCriticalRegionLine(int y, int x, int end) {
    if (x < WIDTH_COLS) {
      int i = 32 - end;
      int bitMask = (int) allDirty >>> x << (x + i) >>> i;
      int dirty = (maybeDirty[y] & bitMask) >>> x;
      maybeDirty[y] &= ~bitMask;
      while (dirty != 0) {
        if ((dirty & 0x01) != 0)
          writeIfDirtySinclair(x, y);
        dirty >>>= 1;
        x++;
      }
    }
  }

  private void copyCriticalRegion(int beamX, int beamY) {
    if (criticalRegionY == beamY) {
      copyCriticalRegionLine(criticalRegionY, criticalRegionX, beamX);
    } else {
      copyCriticalRegionLine(criticalRegionY++, criticalRegionX, WIDTH_COLS);
      for (; criticalRegionY < beamY; criticalRegionY++) {
        copyCriticalRegionLine(criticalRegionY, 0, WIDTH_COLS);
      }
      copyCriticalRegionLine(criticalRegionY, 0, beamX);
    }
    criticalRegionX = beamX;
  }

  public BeanPosition getBeamPosition() {
    long[] lineTimes = spectrumMachine.getLineTimes();

    long tStates = z80Clock.getTStates();
    if (tStates < lineTimes[0]) {
      beam.x = beam.y = -1;
      return beam;
    }

    beam.y = (int) ((tStates - lineTimes[0]) / spectrumMachine.getTimings().tstatesPerLine);

    if (beam.y >= 0 && beam.y <= SCREEN_HEIGHT) {
      beam.x = (int) ((tStates - lineTimes[beam.y]) / 4);
    } else {
      beam.x = 0;
    }
    return beam;
  }

  public void updateCritical(int x, int y) {
    BeanPosition beam = getBeamPosition();
    beam.x = beam.x - BORDER_WIDTH_COLS;
    beam.y = beam.y - BORDER_HEIGHT;

    if (beam.y < 0) {
      beam.x = beam.y = 0;
    } else if (beam.y >= HEIGHT) {
      beam.x = WIDTH_COLS;
      beam.y = HEIGHT - 1;
    }

    if (beam.x < 0) {
      beam.x = 0;
    } else if (beam.x > WIDTH_COLS) {
      beam.x = WIDTH_COLS;
    }

    if (y < beam.y || (y == beam.y && x < beam.x)) {
      copyCriticalRegion(beam.x, beam.y);
    }
  }

  private void dirtyChunk(int x, int y) {
    if (y > criticalRegionY || (y == criticalRegionY && x >= criticalRegionX)) {
      updateCritical(x, y);
    }
    maybeDirty[y] |= (1 << x);
  }

  private void dirty8(int offset) {
    int x = dirtyXtable[offset];
    int y = dirtyYtable[offset];
    dirtyChunk(x, y);
  }

  private void dirty64(int offset) {
    int x = dirtyXtable2[offset - 0x1800];
    int y = dirtyYtable2[offset - 0x1800];
    for (int i = 0; i < 8; i++) {
      dirtyChunk(x, y + i);
    }
  }

  /** The ink of an attribute, or its paper while it flashes the other way round. */
  private byte ink(byte attr) {
    return (attr & 0x80) != 0 && flashReversed ? paperBits(attr) : inkBits(attr);
  }

  private byte paper(byte attr) {
    return (attr & 0x80) != 0 && flashReversed ? inkBits(attr) : paperBits(attr);
  }

  private static byte inkBits(byte attr) {
    return (byte) ((attr & 0x07) + ((attr & 0x40) >> 3));
  }

  private static byte paperBits(byte attr) {
    return (byte) ((attr & (0x0f << 3)) >> 3);
  }

  /** How many of borderChanges are this frame's; the rest are last frame's, kept to be reused. */
  private int borderChangesUsed;

  private BorderChange allocChange() {
    if (borderChangesUsed == borderChanges.size()) {
      borderChanges.add(new BorderChange());
    }
    return borderChanges.get(borderChangesUsed++);
  }

  int addBorderSentinel() {
    BorderChange sentinel = allocChange();
    sentinel.x = sentinel.y = 0;
    sentinel.colour = loresBorder;
    return 0;
  }

  private void pushBorderChange(int colour) {
    BeanPosition beam = getBeamPosition();

    if (beam.y >= SCREEN_HEIGHT) return;

    if (beam.x < 0) beam.x = 0;
    if (beam.x > SCREEN_WIDTH_COLS) beam.x = SCREEN_WIDTH_COLS;
    if (beam.y < 0) beam.y = 0;

    BorderChange change = allocChange();
    change.x = beam.x;
    change.y = beam.y;
    change.colour = colour;
  }

  private void checkBorderChange() {
    if (loresBorder != lastBorder) {
      pushBorderChange(loresBorder);
      lastBorder = loresBorder;
    }
  }

  public void setLoresBorder(int colour) {
    if (loresBorder != colour) {
      loresBorder = (byte) colour;
    }
    checkBorderChange();
  }

  public void setHiresBorder(int colour) {
    if (hiresBorder != colour) {
      hiresBorder = (byte) colour;
    }
    checkBorderChange();
  }

  private void setBorder(int y, int start, int end, int colour) {
    int chunkDetail = (int) colour << 11;
    int index = start + y * SCREEN_WIDTH_COLS;

    for (; start < end; start++) {
      if (lastScreen[index] != chunkDetail) {
        uiDisplay.plot8(start, y, (byte) 0x00, (byte) 0, (byte) colour);
        lastScreen[index] = chunkDetail;
        isDirty[y] |= (1L << start);
      }
      index++;
    }
  }

  private void borderChangeWrite(int y, int start, int end, int colour) {
    if (y < BORDER_HEIGHT || y >= BORDER_HEIGHT + HEIGHT) {
      setBorder(y, start, end, colour);
      return;
    }

    if (start < BORDER_WIDTH_COLS) {
      int leftEnd = end > BORDER_WIDTH_COLS ? BORDER_WIDTH_COLS : end;
      setBorder(y, start, leftEnd, colour);
    }

    if (end > BORDER_WIDTH_COLS + WIDTH_COLS) {
      int startRight = Math.max(start, BORDER_WIDTH_COLS + WIDTH_COLS);
      setBorder(y, startRight, end, colour);
    }
  }

  private void borderChangeLinePart(int y, int start, int end, int colour) {
    borderChangeWrite(y, start, end, colour);
  }

  private void borderChangeLine(int y, int colour) {
    borderChangeWrite(y, 0, SCREEN_WIDTH_COLS, colour);
  }

  private void doBorderChange(BorderChange first, BorderChange second) {
    if (first.x != 0) {
      if (first.x != SCREEN_WIDTH_COLS) {
        borderChangeLinePart(first.y, first.x, SCREEN_WIDTH_COLS, first.colour);
      }
      if (first.y < SCREEN_HEIGHT - 1) first.y++;
    }

    for (; first.y < second.y; first.y++) {
      borderChangeLine(first.y, first.colour);
    }

    if (second.x != 0) {
      if (second.x == SCREEN_WIDTH_COLS) {
        borderChangeLine(first.y, first.colour);
      } else {
        borderChangeLinePart(first.y, 0, second.x, first.colour);
      }
    }
  }

  private void updateBorder() {
    BorderChange endSentinel = allocChange();
    endSentinel.x = BORDER_CHANGE_END_SENTINEL.x;
    endSentinel.y = BORDER_CHANGE_END_SENTINEL.y;
    endSentinel.colour = BORDER_CHANGE_END_SENTINEL.colour;

    for (int pos = 0; pos < borderChangesUsed - 1; pos++) {
      doBorderChange(borderChanges.get(pos), borderChanges.get(pos + 1));
    }

    borderChangesUsed = 0;
    addBorderSentinel();
  }

  public int frame() {
    copyCriticalRegion(WIDTH_COLS, HEIGHT - 1);
    criticalRegionX = criticalRegionY = 0;

    updateBorder();
//        updateDirtyRects();
//        updateUiScreen();

    frameCount++;
    if (frameCount == 16) {
      flashReversed = true;
      dirtyFlashingSinclair();
    } else if (frameCount == 32) {
      flashReversed = false;
      dirtyFlashingSinclair();
      frameCount = 0;
    }

    return 0;
  }

  public void dirtyFlashingSinclair() {
    byte[] screen = ram[memory.currentScreen];
    for (int offset = 0x1800; offset < 0x1b00; offset++) {
      int attr = screen[offset];
      if ((attr & 0x80) != 0) dirty64(offset);
    }
  }

  public void refreshMainScreen() {
    Arrays.fill(maybeDirty, (int) allDirty);
  }

  public void refreshAll() {
    redrawAll = true;
    refreshMainScreen();
    Arrays.fill(isDirty, allDirty);
    Arrays.fill(lastScreen, 0xffffffff);
  }

  public int getOffset(int x, int y) {
    return lineStart[y] + x;
  }

  public int getAddr(int x, int y) {
    return getOffset(x, y);
  }

  public int getPixel(int x, int y) {
    byte data, data2;
    int mask = 1 << (7 - (x % 8));
    int index;

    int column = x >> 3;
    index = column + y * SCREEN_WIDTH_COLS;

    data = (byte) (lastScreen[index] & 0xff);
    data2 = (byte) ((lastScreen[index] & 0xff00) >> 8);
    return (data & mask) != 0 ? ink(data2) : paper(data2);
  }

  public int dirtyBorder() {
    // Placeholder: Implement border dirty logic if needed
    return 0;
  }

  public void line() {
    // Placeholder: Implement line drawing logic if needed
  }
}