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
     * The slowest it will be asked to go, because the machine is what takes up a change of speed:
     * at nothing per cent a frame never ends, so the change that would undo it is never read and
     * the emulator does not come back. A tenth of real time is slow enough to watch a frame happen
     * and quick enough to answer.
     */
    public static final int SLOWEST = 10;

    /** A speed the machine can be asked for: anything below the slowest is the slowest. */
    public static int sensible(int perCent) {
        return Math.max(SLOWEST, Math.min(UNLIMITED, perCent));
    }

    /**
     * Per cent of a real Spectrum. A hundred is real time; what ships is far above it, so that
     * anything headless runs flat out, and whoever is showing the machine to a person asks for
     * real time before init - the sound sizes a frame of audio by this, and at twenty thousand
     * per cent that frame is three samples.
     */
    public int emulation;
    /** Whether a tape loads at whatever speed the machine can manage rather than at its own. */
    public boolean fastLoading;

    /**
     * Told when the speed is set from outside rather than read every frame. Whoever is pacing the
     * machine has an estimate of how long a frame takes that a new speed makes meaningless, and
     * setting the field alone left it pacing to the old one.
     */
    public Runnable whenChanged = () -> { };

    public int emulation() {
        return emulation;
    }

    public void setEmulation(int perCent) {
        emulation = sensible(perCent);
        whenChanged.run();
    }

    public boolean fastLoading() {
        return fastLoading;
    }

    public void setFastLoading(boolean fast) {
        fastLoading = fast;
    }
}
