/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Display {

  // Constants for the width and height of the Speccy's screen
  public final int WIDTH_COLS = 32;
  public final int HEIGHT_ROWS = 24;

  public final int WIDTH = WIDTH_COLS * 16;
  public final int HEIGHT = HEIGHT_ROWS * 8;

  // Constants for the width and height of the emulated border
  public final int BORDER_WIDTH_COLS = 4;
  public final int BORDER_HEIGHT_COLS = 3;

  public final int BORDER_WIDTH = BORDER_WIDTH_COLS * 16;
  public final int BORDER_ASPECT_WIDTH = BORDER_WIDTH_COLS * 8;
  public final int BORDER_HEIGHT = BORDER_HEIGHT_COLS * 8;

  // Constants for the width and height of the window we'll be displaying
  public final int SCREEN_WIDTH = WIDTH + 2 * BORDER_WIDTH;
  public final int SCREEN_HEIGHT = HEIGHT + 2 * BORDER_HEIGHT;

  public final int SCREEN_WIDTH_COLS = WIDTH_COLS + 2 * BORDER_WIDTH_COLS;

  // Aspect ratio corrected display width
  public final int ASPECT_WIDTH = SCREEN_WIDTH / 2;

  // Set once we have initialized the UI
  public boolean uiInitialised;

  // The current border color
  private byte loresBorder;
  private byte hiresBorder;
  private byte lastBorder;

  // Stores the pixel, attribute, and SCLD screen mode information used to
  // draw each 8x1 group of pixels (including border) last frame
  private int[] lastScreen = new int[SCREEN_WIDTH_COLS * SCREEN_HEIGHT];

  // Offsets as to where the data and the attributes for each pixel line start
  public int[] lineStart = new int[HEIGHT];
  public int[] attrStart = new int[HEIGHT];

  // If you write to the byte at display_dirty_?table[n+0x4000], then
  // the eight pixels starting at (8*xtable[n],ytable[n]) must be replotted
  private int[] dirtyYtable = new int[WIDTH_COLS * HEIGHT];
  private int[] dirtyXtable = new int[WIDTH_COLS * HEIGHT];

  // If you write to the byte at display_dirty_?table2[n+0x5800], then
  // the 64 pixels starting at (8*xtable2[n],ytable2[n]) must be replotted
  private int[] dirtyYtable2 = new int[WIDTH_COLS * HEIGHT_ROWS];
  private int[] dirtyXtable2 = new int[WIDTH_COLS * HEIGHT_ROWS];

  // The number of frames mod 32 that have elapsed
  private int frameCount;
  private boolean flashReversed;

  // Which eight-pixel chunks on each line (including border) need to be redisplayed
  private long[] isDirty = new long[SCREEN_HEIGHT];

  // Which eight-pixel chunks on each line may need to be redisplayed
  private int[] maybeDirty = new int[HEIGHT];

  // This value signifies that the entire line must be redisplayed
  private long allDirty;

  // Used to signify that we're redrawing the entire screen
  private boolean redrawAll;

  // The last point at which we updated the screen display
  private int criticalRegionX;
  private int criticalRegionY;

  public int init(Object initContext) {
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

    borderChanges.clear();
    int error = addBorderSentinel();
    if (error != 0) return error;
    lastBorder = Scld.lastDec.name.hires ? hiresBorder : loresBorder;

    return 0;
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
  private int[][] currentBorder = new int[SCREEN_HEIGHT][SCREEN_WIDTH_COLS];

  // Functional interfaces for dirty handling
  @FunctionalInterface
  interface DisplayDirtyFn {
    void apply(int offset);
  }

  @FunctionalInterface
  interface DisplayWriteIfDirtyFn {
    void apply(int x, int y);
  }

  @FunctionalInterface
  interface DisplayDirtyFlashingFn {
    void apply();
  }

  public DisplayDirtyFn dirty = this::dirtySinclair;
  public DisplayWriteIfDirtyFn writeIfDirty = this::writeIfDirtySinclair;
  public DisplayDirtyFlashingFn dirtyFlashing = this::dirtyFlashingSinclair;

  List<BorderChange> borderChanges = new ArrayList<>();

  public void dirtySinclair(int offset) {
    if (offset >= 0x1b00) return;
    if (offset < 0x1800) {
      dirty8(offset);
    } else {
      dirty64(offset);
    }
  }

  private byte getAttrByte(int x, int y) {
    int attr;
    if (Scld.lastDec.name.hires) {
      attr = Hires.getAttr();
    } else {
      int offset;
      if (Scld.lastDec.name.b1) {
        offset = lineStart[y] + x + Constants.ALTDFILE_OFFSET;
      } else if (Scld.lastDec.name.altdfile) {
        offset = attrStart[y] + x + Constants.ALTDFILE_OFFSET;
      } else {
        offset = attrStart[y] + x;
      }
      attr = Spectrum.RAM[Memory.currentScreen][offset];
    }
    return (byte) attr;
  }

  private void updateDirtyRects() {
    for (int y = 0; y < SCREEN_HEIGHT; y++) {
      int x = 0;
      while (isDirty[y] != 0) {
        while ((isDirty[y] & 0x01) == 0) {
          isDirty[y] >>>= 1;
          x++;
        }
        int start = x;
        do {
          isDirty[y] >>>= 1;
          x++;
        } while ((isDirty[y] & 0x01) != 0);
        Rectangle.add(y, start, x - start);
      }
      Rectangle.endLine(y);
    }
    Rectangle.endLine(SCREEN_HEIGHT);
  }

  public void writeIfDirtySinclair(int x, int y) {
    int beamX = x + BORDER_WIDTH_COLS;
    int beamY = y + BORDER_HEIGHT;
    int offset = getOffset(x, y);

    byte[] screen = Spectrum.RAM[Memory.currentScreen];
    int data = screen[offset];
    byte data2 = getAttrByte(x, y);

    int lastChunkDetail = ((int) (flashReversed ? 1 : 0) << 24) | ((data2 & 0xFF) << 8) | (data & 0xFF);
    int index = beamX + beamY * SCREEN_WIDTH_COLS;
    if (lastScreen[index] != lastChunkDetail) {
      byte[] inkPaper = new byte[2];
      parseAttr(data2, inkPaper);
      byte ink = inkPaper[0], paper = inkPaper[1];
//            System.err.printf("display_write_if_dirty_sinclair: x=%d y=%d data=%02x attr=%02x ink=%d paper=%d\n", x, y, data, data2, ink, paper );
      UiDisplay.plot8(beamX, beamY, (byte) (data & 0xff), ink, paper);
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
          writeIfDirty.apply(x, y);
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

  public int[] getBeamPosition() {
    int[] beam = new int[2];
    long tstates = Spectrum.tstates;
    FuseMachineInfo current = Machine.current;
    long[] lineTimes = current.lineTimes;

    if (tstates < lineTimes[0]) {
      beam[0] = beam[1] = -1;
      return beam;
    }

    beam[1] = (int) ((tstates - lineTimes[0]) / current.timings.tstatesPerLine);

    if (beam[1] >= 0 && beam[1] <= SCREEN_HEIGHT) {
      beam[0] = (int) ((tstates - lineTimes[beam[1]]) / 4);
    } else {
      beam[0] = 0;
    }
    return beam;
  }

  public void updateCritical(int x, int y) {
    int[] beam = getBeamPosition();
    int beamX = beam[0] - BORDER_WIDTH_COLS;
    int beamY = beam[1] - BORDER_HEIGHT;

    if (beamY < 0) {
      beamX = beamY = 0;
    } else if (beamY >= HEIGHT) {
      beamX = WIDTH_COLS;
      beamY = HEIGHT - 1;
    }

    if (beamX < 0) {
      beamX = 0;
    } else if (beamX > WIDTH_COLS) {
      beamX = WIDTH_COLS;
    }

    if (y < beamY || (y == beamY && x < beamX)) {
      copyCriticalRegion(beamX, beamY);
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

  private void getAttr(int x, int y, byte[] inkPaper) {
    parseAttr(getAttrByte(x, y), inkPaper);
  }

  public void parseAttr(byte attr, byte[] inkPaper) {
    if ((attr & 0x80) != 0 && flashReversed) {
      inkPaper[0] = (byte) ((attr & (0x0f << 3)) >> 3);
      inkPaper[1] = (byte) ((attr & 0x07) + ((attr & 0x40) >> 3));
    } else {
      inkPaper[0] = (byte) ((attr & 0x07) + ((attr & 0x40) >> 3));
      inkPaper[1] = (byte) ((attr & (0x0f << 3)) >> 3);
    }
  }

  private BorderChange allocChange() {
    BorderChange change = new BorderChange();
    borderChanges.add(change);
    return change;
  }

  int addBorderSentinel() {
    BorderChange sentinel = allocChange();
    sentinel.x = sentinel.y = 0;
    sentinel.colour = Scld.lastDec.name.hires ? hiresBorder : loresBorder;
    return 0;
  }

  private void pushBorderChange(int colour) {
    int[] beam = getBeamPosition();
    int beamX = beam[0], beamY = beam[1];

    if (beamY >= SCREEN_HEIGHT) return;

    if (beamX < 0) beamX = 0;
    if (beamX > SCREEN_WIDTH_COLS) beamX = SCREEN_WIDTH_COLS;
    if (beamY < 0) beamY = 0;

    BorderChange change = allocChange();
    change.x = beamX;
    change.y = beamY;
    change.colour = colour;
  }

  private void checkBorderChange() {
    if (Scld.lastDec.name.hires && hiresBorder != lastBorder) {
      pushBorderChange(hiresBorder);
      lastBorder = hiresBorder;
    } else if (!Scld.lastDec.name.hires && loresBorder != lastBorder) {
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
        UiDisplay.plot8(start, y, (byte) 0x00, (byte) 0, (byte) colour);
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

    for (int pos = 0; pos < borderChanges.size() - 1; pos++) {
      doBorderChange(borderChanges.get(pos), borderChanges.get(pos + 1));
    }

    borderChanges.clear();
    addBorderSentinel();
  }

  int frameCountLocal = 0;

  private void updateUiScreen() {
    int scale = Machine.current.timex ? 2 : 1;

    if (Settings.current.frameRate <= ++frameCountLocal) {
      frameCountLocal = 0;
      if (Movie.recording) {
        Movie.startFrame();
      }

      if (redrawAll) {
        if (Movie.recording) {
          Movie.addArea(0, 0, ASPECT_WIDTH >> 3, SCREEN_HEIGHT);
        }
        //UiDisplay.area(0, 0, scale * ASPECT_WIDTH, scale * SCREEN_HEIGHT);
        redrawAll = false;
      } else {
        for (Rect rect : Rectangle.inactive) {
          if (Movie.recording) {
            Movie.addArea(rect.x, rect.y, rect.getW(), rect.getH());
          }
          UiDisplay.area(8 * scale * rect.x, scale * rect.y, 8 * scale * rect.getW(), scale * rect.getH());
        }
      }

      Rectangle.inactive.clear();

      UiDisplay.frameEnd();
    }
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
      dirtyFlashing.apply();
    } else if (frameCount == 32) {
      flashReversed = false;
      dirtyFlashing.apply();
      frameCount = 0;
    }

    return 0;
  }

  public void dirtyFlashingSinclair() {
    byte[] screen = Spectrum.RAM[Memory.currentScreen];
    for (int offset = 0x1800; offset < 0x1b00; offset++) {
      byte attr = screen[offset];
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
    return Scld.lastDec.name.altdfile ? getOffset(x, y) + Constants.ALTDFILE_OFFSET : getOffset(x, y);
  }

  public int getPixel(int x, int y) {
    byte[] inkPaper = new byte[2];
    byte data, data2;
    int mask = 1 << (7 - (x % 8));
    int index;

    if (Machine.current.timex) {
      int column = x >> 4;
      y >>= 1;
      index = column + y * SCREEN_WIDTH_COLS;

      data = (byte) (lastScreen[index] & 0xff);
      data2 = (byte) ((lastScreen[index] & 0xff00) >> 8);
      long modeData = (lastScreen[index] & 0xff0000) >> 16;

      if (Scld.fromByte(modeData).name.hires) {
        if (x % 16 > 7) data = data2;
        parseAttr(Hires.convertDec(modeData), inkPaper);
      } else {
        mask = 1 << (7 - ((x >> 1) % 8));
        parseAttr(data2, inkPaper);
      }
    } else {
      int column = x >> 3;
      index = column + y * SCREEN_WIDTH_COLS;

      data = (byte) (lastScreen[index] & 0xff);
      data2 = (byte) ((lastScreen[index] & 0xff00) >> 8);
      parseAttr(data2, inkPaper);
    }

    return (data & mask) != 0 ? inkPaper[0] : inkPaper[1];
  }

  public int dirtyBorder() {
    // Placeholder: Implement border dirty logic if needed
    return 0;
  }

  public void line() {
    // Placeholder: Implement line drawing logic if needed
  }
}