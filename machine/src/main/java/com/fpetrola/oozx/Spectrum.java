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

package com.fpetrola.oozx;// Assuming the following classes are ported:
// - Libspectrum (with byte as int for unsigned, dword as long)
// - Compat
// - Debugger
// - Display
// - EventManager (from event.h)
// - Keyboard
// - StartupManager, StartupManagerModule
// - Loader
// - Machine
// - Memory (with MEMORY_CURRENT_SCREEN, etc.)
// - Module, ModuleInfo
// - Printer
// - Ula
// - PhantomTypist
// - Psg
// - Profile
// - Rzx
// - Settings
// - Sound
// - SpectrumConstants (for SPECTRUM_RAM_PAGES, DISPLAY_BORDER_HEIGHT, etc.)
// - Tape
// - Timer
// - Ui, UiJoystick
// - Z80
// Use int for libspectrum_byte (0-255), long for libspectrum_dword


public class Spectrum {
    // How many tstates have elapsed since the last interrupt?
    public static long tstates;

    // RAM array: 65 pages of 16KB each (from SPECTRUM_RAM_PAGES)
    public static byte[][] RAM = new byte[Memory.SPECTRUM_RAM_PAGES][0x4000];

    // Functional interface for checking if a port is handled by the ULA
    @FunctionalInterface
    interface PortFromUlaFunction {
        boolean apply(int port);
    }

    // Functional interface for contention delay calculation
    @FunctionalInterface
    interface ContentionDelayFunction {
        int apply(long time);
    }

    // Instance of RamInfo
    public static RamInfo ramInfo = new RamInfo();


    // Contention patterns
    private static int[] contentionPattern65432100 = {5, 4, 3, 2, 1, 0, 0, 6};
    private static int[] contentionPattern76543210 = {5, 4, 3, 2, 1, 0, 7, 6};

    // Event
    public static int spectrumFrameEvent;

    // Debugger variable prefix
    private static final String DEBUGGER_TYPE_STRING = "spectrum";

    // Debugger variable for frame count
    private static final String FRAME_COUNT_NAME = "frames";

    // Count of frames since last reset
    private static long framesSinceReset;

    private static void spectrumReset(int a) {
        framesSinceReset = 0;
    }

    private static ModuleInfo moduleInfo = new ModuleInfo(
         Spectrum::spectrumReset,  null, null, null, null);
    ;

    private static void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
        if (Rzx.playback) EventManager.eventForceEvents();
        Rzx.frame();
        Psg.frame();
        spectrumFrame();
        Z80.interrupt();
        UiJoystick.poll();
        Timer.estimateSpeed();
        Debugger.addTimeEvents();
        Ui.event();
        Ui.errorFrame();
    }

    private static long getFrameCount() {
        return framesSinceReset;
    }

    static int spectrumInit(Object context) {
        spectrumFrameEvent = EventManager.eventRegister(Spectrum::spectrumFrameEventFn, "End of frame");

        Module.register(moduleInfo);

        Debugger.systemVariableRegister(DEBUGGER_TYPE_STRING, FRAME_COUNT_NAME, Spectrum::getFrameCount, null);

        return 0;
    }

    public static void registerStartup() {
        StartupManager.register(new SpectrumStartupModule());
        //reg1();
    }

//    private static void reg1() {
//        StartupManagerModule[] dependencies = {
//            StartupManagerModule.DEBUGGER,
//            StartupManagerModule.EVENT,
//            StartupManagerModule.SETUID
//        };
//        StartupManager.register(StartupManagerModule.SPECTRUM, dependencies, Spectrum::spectrumInit, null, null);
//    }

    public static int spectrumFrame() {
        long frameLength = Rzx.playback ? tstates : Machine.current.timings.tstatesPerFrame;

        EventManager.eventFrame(frameLength);
        Debugger.breakpointReduceTstates(frameLength);
        tstates -= frameLength;

        if (Z80.interruptsEnabledAt >= 0) {
            Z80.interruptsEnabledAt -= frameLength;
        }

        if (Sound.enabled) Sound.frame();

        if (Display.frame() != 0) return 1;
        if (Profile.active) Profile.frame(frameLength);
        Printer.frame();

        if (!Rzx.playback) {
            EventManager.eventAdd(Machine.current.timings.tstatesPerFrame, spectrumFrameEvent);
        }

        Loader.frame(frameLength);
        PhantomTypist.frame();

        framesSinceReset++;

        return 0;
    }

    public static int contendDelayNone(long time) {
        return 0;
    }

    private static int contendDelayCommon(long time, int[] timings, int offset) {
        int line = (int) ((time - Machine.current.lineTimes[0]) / Machine.current.timings.tstatesPerLine);

        int tstatesThroughLine = (int) (time - Machine.current.lineTimes[0] +
                (Machine.current.timings.leftBorder - Display.BORDER_WIDTH_COLS * 4));

        tstatesThroughLine %= Machine.current.timings.tstatesPerLine;

        if (line < Display.BORDER_HEIGHT ||
                line >= Display.BORDER_HEIGHT + Display.HEIGHT) return 0;

        if (tstatesThroughLine < Machine.current.timings.leftBorder - offset) return 0;

        if (tstatesThroughLine >= Machine.current.timings.leftBorder +
                Machine.current.timings.horizontalScreen - offset) return 0;

        return timings[tstatesThroughLine % 8];
    }

    public static int contendDelay65432100(long time) {
        return contendDelayCommon(time, contentionPattern65432100, 1);
    }

    public static int contendDelay76543210(long time) {
        return contendDelayCommon(time, contentionPattern76543210, 4);
    }

    public static int spectrumUnattachedPort() {
        if (tstates < Machine.current.lineTimes[Display.BORDER_HEIGHT]) return 0xff;

        int line = (int) ((tstates - Machine.current.lineTimes[Display.BORDER_HEIGHT]) /
                Machine.current.timings.tstatesPerLine);

        if (line >= Display.HEIGHT) return 0xff;

        int tstatesThroughLine = (int) (tstates -
                Machine.current.lineTimes[Display.BORDER_HEIGHT + line] +
                (Machine.current.timings.leftBorder - Display.BORDER_WIDTH_COLS * 4));

        if (tstatesThroughLine < Machine.current.timings.leftBorder) return 0xff;

        if (tstatesThroughLine >= Machine.current.timings.leftBorder +
                Machine.current.timings.horizontalScreen) return 0xff;

        int column = ((tstatesThroughLine - Machine.current.timings.leftBorder) / 8) * 2;

        switch (tstatesThroughLine % 8) {
            case 5:
                column++;
            case 3:
                return RAM[Memory.currentScreen][Display.attrStart[line] + column];

            case 4:
                column++;
            case 2:
                return RAM[Memory.currentScreen][Display.lineStart[line] + column];

            case 0:
            case 1:
            case 6:
            case 7:
                return 0xff;
        }

        return 0; // Keep compiler happy
    }

    public static int spectrumUnattachedPortNone() {
        return 0xff;
    }
}