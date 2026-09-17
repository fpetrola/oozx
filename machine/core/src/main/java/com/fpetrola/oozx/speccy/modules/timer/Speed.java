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
        emulation = perCent;
        whenChanged.run();
    }

    public boolean fastLoading() {
        return fastLoading;
    }

    public void setFastLoading(boolean fast) {
        fastLoading = fast;
    }
}
