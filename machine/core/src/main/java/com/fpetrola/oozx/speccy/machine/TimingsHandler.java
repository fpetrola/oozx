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

package com.fpetrola.oozx.speccy.machine;

public class TimingsHandler {

  public static void initMachineTimings(MachineTimings timings, SpectrumMachine machine) {
    Timings baseTiming = machine.getBaseTiming();

    timings.processorSpeed = processorSpeed(baseTiming);
    timings.leftBorder = leftBorder(baseTiming);
    timings.horizontalScreen = horizontalScreen(baseTiming);
    timings.rightBorder = rightBorder(baseTiming);
    timings.tstatesPerLine = tstatesPerLine(baseTiming);
    timings.interruptLength = interruptLength(baseTiming);
    timings.tstatesPerFrame = tstatesPerFrame(baseTiming);
  }

  // Structure for frame timings
  public static class FrameTimings {
    // Line timings in t-states
    final int leftBorder;
    final int horizontalScreen;
    final int rightBorder;
    final int horizontalRetrace;

    // Frame timings in lines
    final int topBorder;
    final int verticalScreen;
    final int bottomBorder;
    final int verticalRetrace;

    // Interrupt duration in t-states
    final int interruptLength;

    // T-states from interrupt to top-left pixel
    final long topLeftPixel;

    FrameTimings(int leftBorder, int horizontalScreen, int rightBorder, int horizontalRetrace,
                 int topBorder, int verticalScreen, int bottomBorder, int verticalRetrace,
                 int interruptLength, long topLeftPixel) {
      this.leftBorder = leftBorder;
      this.horizontalScreen = horizontalScreen;
      this.rightBorder = rightBorder;
      this.horizontalRetrace = horizontalRetrace;
      this.topBorder = topBorder;
      this.verticalScreen = verticalScreen;
      this.bottomBorder = bottomBorder;
      this.verticalRetrace = verticalRetrace;
      this.interruptLength = interruptLength;
      this.topLeftPixel = topLeftPixel;
    }
  }

  // Frame timing constants
  public static final FrameTimings FERRANTI_5C_6C = new FrameTimings(
      24, 128, 24, 48, // Horizontal: 224 clocks per line
      48, 192, 48, 24, // Vertical: 312 lines per frame
      32, 14336
  );

  public static final FrameTimings FERRANTI_60HZ = new FrameTimings(
      24, 128, 24, 48, // Horizontal: 224 clocks per line
      24, 192, 25, 23, // Vertical: 264 lines per frame
      32, 8960
  );

  public static final FrameTimings FERRANTI_7C = new FrameTimings(
      24, 128, 24, 52, // Horizontal: 228 clocks per line
      48, 192, 48, 23, // Vertical: 311 lines per frame
      36, 14362
  );

  public static final FrameTimings AMSTRAD_ASIC = new FrameTimings(
      24, 128, 24, 52, // Horizontal: 228 clocks per line
      48, 192, 48, 23, // Vertical: 311 lines per frame
      32, 14365
  );

  public static final FrameTimings TIMEX_SCLD_50HZ = new FrameTimings(
      24, 128, 24, 48, // Horizontal: 224 clocks per line
      48, 192, 48, 24, // Vertical: 312 lines per frame
      32, 14321
  );

  private static final FrameTimings TIMEX_SCLD_60HZ = new FrameTimings(
      24, 128, 24, 48, // Horizontal: 224 clocks per line
      24, 192, 25, 21, // Vertical: 262 lines per frame
      32, 9169
  );

  private static final FrameTimings SE = new FrameTimings(
      24, 128, 24, 48, // Horizontal: 224 clocks per line
      47, 192, 48, 25, // Vertical: 312 lines per frame
      32, 14336
  );

  /** 224 clocks over 320 lines is 71680 to a frame, which at 3.584MHz is fifty of them a second. */
  public static final FrameTimings PENTAGON = new FrameTimings(
      36, 128, 28, 32, // Horizontal: 224 clocks per line
      64, 192, 48, 16, // Vertical: 320 lines per frame
      36, 17988
  );

  private static final FrameTimings SCORPION = new FrameTimings(
      24, 128, 32, 40, // Horizontal: 224 clocks per line
      48, 192, 48, 24, // Vertical: 312 lines per frame
      36, 14336
  );

  // Structure for machine timings
  public static class Timings {
    final long processorSpeed; // Processor speed in Hz
    final long aySpeed; // AY clock speed in Hz
    final FrameTimings frameTimings;

    Timings(long processorSpeed, long aySpeed, FrameTimings frameTimings) {
      this.processorSpeed = processorSpeed;
      this.aySpeed = aySpeed;
      this.frameTimings = frameTimings;
    }
  }

  // Get processor speed for a machine
  public static long processorSpeed(Timings baseTiming) {
    return baseTiming.processorSpeed;
  }

  // Get AY clock speed for a machine
  public static long aySpeed(Timings baseTiming) {
    return baseTiming.aySpeed;
  }

  // Get left border t-states
  public static int leftBorder(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.leftBorder : 0;
  }

  // Get horizontal screen t-states
  public static int horizontalScreen(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.horizontalScreen : 0;
  }

  // Get right border t-states
  public static int rightBorder(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.rightBorder : 0;
  }

  // Get horizontal retrace t-states
  public static int horizontalRetrace(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.horizontalRetrace : 0;
  }

  // Get top border lines
  public static int topBorder(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.topBorder : 0;
  }

  // Get vertical screen lines
  public static int verticalScreen(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.verticalScreen : 0;
  }

  // Get bottom border lines
  public static int bottomBorder(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.bottomBorder : 0;
  }

  // Get vertical retrace lines
  public static int verticalRetrace(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.verticalRetrace : 0;
  }

  // Get interrupt length in t-states
  public static int interruptLength(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? f.interruptLength : 0;
  }

  // Get t-states from interrupt to top-left pixel
  public static int topLeftPixel(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    return f != null ? (int) f.topLeftPixel : 0;
  }

  // Get t-states per line
  public static int tstatesPerLine(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    if (f == null) return 0;
    return f.leftBorder + f.horizontalScreen + f.rightBorder + f.horizontalRetrace;
  }

  // Get lines per frame
  public static int linesPerFrame(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    if (f == null) return 0;
    return f.topBorder + f.verticalScreen + f.bottomBorder + f.verticalRetrace;
  }

  // Get t-states per frame
  public static int tstatesPerFrame(Timings baseTiming) {
    FrameTimings f = baseTiming.frameTimings;
    if (f == null) return 0;
    return (int) tstatesPerLine(baseTiming) * linesPerFrame(baseTiming);
  }
}
