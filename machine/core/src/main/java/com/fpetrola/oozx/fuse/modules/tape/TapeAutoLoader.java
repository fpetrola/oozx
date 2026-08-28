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

package com.fpetrola.oozx.fuse.modules.tape;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.KeyboardKeyName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Types LOAD "" on the emulated keyboard and starts the tape.
 * <p>
 * This is a state machine stepped from the emulation loop rather than a thread that sleeps
 * alongside it: the emulator runs at {@code emulationSpeed} (200x real time by default), so
 * wall-clock delays translate into wildly different amounts of emulated time from one run to
 * the next, and the keystrokes race the loop that is supposed to read them.
 * <p>
 * Keys go through {@link com.fpetrola.oozx.fuse.modules.Keyboard}. Writing the key code into
 * LAST_K and raising bit 5 of FLAGS does not register as a keypress here, so a LOAD typed that
 * way never reaches the interpreter and the machine stays in the editor.
 */
public class TapeAutoLoader {

  /** Frames to let the ROM reach the BASIC prompt before typing. */
  private static final int BOOT_FRAMES = 120;
  /** Frames a key is held down, and the gap after releasing it. */
  private static final int KEY_HOLD_FRAMES = 4;
  private static final int KEY_GAP_FRAMES = 6;

  private final Fuse fuse;
  private final File tapeFile;
  private final List<Runnable> steps = new ArrayList<>();

  private long previousTStates;
  private int framesToWait = BOOT_FRAMES;
  private int nextStep;
  private String error;

  public TapeAutoLoader(Fuse fuse, File tapeFile) {
    this.fuse = fuse;
    this.tapeFile = tapeFile;
    this.previousTStates = fuse.zxClock.getTStates();

    // LOAD "" ENTER, in 48K BASIC keyword entry.
    press(KeyboardKeyName.KEYBOARD_j);
    press(KeyboardKeyName.KEYBOARD_Symbol, KeyboardKeyName.KEYBOARD_p);
    press(KeyboardKeyName.KEYBOARD_Symbol, KeyboardKeyName.KEYBOARD_p);
    press(KeyboardKeyName.KEYBOARD_Enter);
    steps.add(this::startTape);
  }

  private void press(KeyboardKeyName... keys) {
    steps.add(() -> {
      for (KeyboardKeyName key : keys) {
        fuse.keyboard.press(key);
      }
      framesToWait = KEY_HOLD_FRAMES;
    });
    steps.add(() -> {
      for (KeyboardKeyName key : keys) {
        fuse.keyboard.release(key);
      }
      framesToWait = KEY_GAP_FRAMES;
    });
  }

  private void startTape() {
    fuse.tape.stop();
    fuse.tape.eject();

    if (!fuse.tape.insert(tapeFile)) {
      error = "the tape deck rejected " + tapeFile;
    } else if (!fuse.tape.play(false)) {
      error = "the tape deck refused to play " + tapeFile;
    }
  }

  /**
   * Advances the sequence. Call once per iteration of the emulation loop, on that same thread.
   * Frames are counted by watching the clock wrap, since {@code Spectrum.spectrumFrame}
   * subtracts a frame's worth of tStates at every frame boundary.
   */
  public void step() {
    if (isDone()) {
      return;
    }

    long tStates = fuse.zxClock.getTStates();
    boolean frameElapsed = tStates < previousTStates;
    previousTStates = tStates;
    if (!frameElapsed) {
      return;
    }

    if (framesToWait > 0) {
      framesToWait--;
      return;
    }

    steps.get(nextStep++).run();
  }

  public boolean isDone() {
    return nextStep >= steps.size();
  }

  /** Null unless the tape could not be inserted or played. */
  public String getError() {
    return error;
  }
}
