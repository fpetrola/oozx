package com.fpetrola.oozx.speccy.modules.timer;

import com.google.inject.Singleton;

/**
 * How fast the machine is allowed to run. One object, because the timer paces by it, the sound
 * sizes its frame by it, and whoever turns the knob turns this.
 */
@Singleton
public class Speed {
    /** As fast as it will go: not a speed but the absence of one. */
    public static final int UNLIMITED = 1_000_000;

    /** What a real Spectrum runs at, which is what whoever is watching one wants. */
    public static final int REAL_TIME = 100;

    /**
     * Per cent of a real Spectrum. A hundred is real time; what ships is far above it, so that
     * anything headless runs flat out, and whoever is showing the machine to a person asks for
     * real time before init - the sound sizes a frame of audio by this, and at twenty thousand
     * per cent that frame is three samples.
     */
    public int emulation;
    /** Whether a tape loads at whatever speed the machine can manage rather than at its own. */
    public boolean fastLoading;
}
