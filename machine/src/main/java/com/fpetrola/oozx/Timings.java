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

package com.fpetrola.oozx;// Assuming ported dependencies:
// - Libspectrum (Machine)

public class Timings {

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
    private static final FrameTimings FERRANTI_5C_6C = new FrameTimings(
            24, 128, 24, 48, // Horizontal: 224 clocks per line
            48, 192, 48, 24, // Vertical: 312 lines per frame
            32, 14336
    );

    private static final FrameTimings FERRANTI_60HZ = new FrameTimings(
            24, 128, 24, 48, // Horizontal: 224 clocks per line
            24, 192, 25, 23, // Vertical: 264 lines per frame
            32, 8960
    );

    private static final FrameTimings FERRANTI_7C = new FrameTimings(
            24, 128, 24, 52, // Horizontal: 228 clocks per line
            48, 192, 48, 23, // Vertical: 311 lines per frame
            36, 14362
    );

    private static final FrameTimings AMSTRAD_ASIC = new FrameTimings(
            24, 128, 24, 52, // Horizontal: 228 clocks per line
            48, 192, 48, 23, // Vertical: 311 lines per frame
            32, 14365
    );

    private static final FrameTimings TIMEX_SCLD_50HZ = new FrameTimings(
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

    private static final FrameTimings PENTAGON = new FrameTimings(
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
    public static class MachineTimings {
        final long processorSpeed; // Processor speed in Hz
        final long aySpeed; // AY clock speed in Hz
        final FrameTimings frameTimings;

        MachineTimings(long processorSpeed, long aySpeed, FrameTimings frameTimings) {
            this.processorSpeed = processorSpeed;
            this.aySpeed = aySpeed;
            this.frameTimings = frameTimings;
        }
    }

    // Base timings for each machine
    private static final MachineTimings[] BASE_TIMINGS = {
            // 48K
            new MachineTimings(3500000, 0, FERRANTI_5C_6C),
            // TC2048
            new MachineTimings(3500000, 0, TIMEX_SCLD_50HZ),
            // 128K
            new MachineTimings(3546900, 1773400, FERRANTI_7C),
            // +2
            new MachineTimings(3546900, 1773400, FERRANTI_7C),
            // Pentagon
            new MachineTimings(3584000, 1792000, PENTAGON),
            // +2A
            new MachineTimings(3546900, 1773400, AMSTRAD_ASIC),
            // +3
            new MachineTimings(3546900, 1773400, AMSTRAD_ASIC),
            // Unknown machine
            new MachineTimings(0, 0, null),
            // 16K
            new MachineTimings(3500000, 0, FERRANTI_5C_6C),
            // TC2068
            new MachineTimings(3500000, 1750000, TIMEX_SCLD_50HZ),
            // Scorpion
            new MachineTimings(3500000, 1750000, SCORPION),
            // +3e
            new MachineTimings(3546900, 1773400, AMSTRAD_ASIC),
            // SE
            new MachineTimings(3500000, 1750000, SE),
            // TS2068
            new MachineTimings(3528000, 1764000, TIMEX_SCLD_60HZ),
            // Pentagon 512K
            new MachineTimings(3584000, 1792000, PENTAGON),
            // Pentagon 1024K
            new MachineTimings(3584000, 1792000, PENTAGON),
            // 48K NTSC
            new MachineTimings(3527500, 0, FERRANTI_60HZ),
            // 128Ke
            new MachineTimings(3546900, 1773400, AMSTRAD_ASIC)
    };

    // Get processor speed for a machine
    public static long processorSpeed(Libspectrum.Machine machine) {
        return BASE_TIMINGS[machine.ordinal()].processorSpeed;
    }

    // Get AY clock speed for a machine
    public static long aySpeed(Libspectrum.Machine machine) {
        return BASE_TIMINGS[machine.ordinal()].aySpeed;
    }

    // Get left border t-states
    public static int leftBorder(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.leftBorder : 0;
    }

    // Get horizontal screen t-states
    public static int horizontalScreen(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.horizontalScreen : 0;
    }

    // Get right border t-states
    public static int rightBorder(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.rightBorder : 0;
    }

    // Get horizontal retrace t-states
    public static int horizontalRetrace(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.horizontalRetrace : 0;
    }

    // Get top border lines
    public static int topBorder(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.topBorder : 0;
    }

    // Get vertical screen lines
    public static int verticalScreen(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.verticalScreen : 0;
    }

    // Get bottom border lines
    public static int bottomBorder(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.bottomBorder : 0;
    }

    // Get vertical retrace lines
    public static int verticalRetrace(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.verticalRetrace : 0;
    }

    // Get interrupt length in t-states
    public static int interruptLength(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? f.interruptLength : 0;
    }

    // Get t-states from interrupt to top-left pixel
    public static int topLeftPixel(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        return f != null ? (int) f.topLeftPixel : 0;
    }

    // Get t-states per line
    public static int tstatesPerLine(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        if (f == null) return 0;
        return f.leftBorder + f.horizontalScreen + f.rightBorder + f.horizontalRetrace;
    }

    // Get lines per frame
    public static int linesPerFrame(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        if (f == null) return 0;
        return f.topBorder + f.verticalScreen + f.bottomBorder + f.verticalRetrace;
    }

    // Get t-states per frame
    public static long tstatesPerFrame(Libspectrum.Machine machine) {
        FrameTimings f = BASE_TIMINGS[machine.ordinal()].frameTimings;
        if (f == null) return 0;
        return (long) tstatesPerLine(machine) * linesPerFrame(machine);
    }
}
