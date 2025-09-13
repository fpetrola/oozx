/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fpetrola.emulation.helpers.machine;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author jsanchez
 */  
public class Clock { 
    private static final Clock instance = new Clock();
    private MachineTypes spectrumModel = MachineTypes.SPECTRUM48K;
    private int tstates;
    private long frames;
    private int timeout;
    private final CopyOnWriteArrayList<ClockTimeoutListener> clockListeners;

    private Clock() {
        this.clockListeners = new CopyOnWriteArrayList<>();
    }

    public static Clock getInstance() {
        return instance;
    }
    /**
     * Adds a new event listener to the list of event listeners.
     *
     * @param listener The new event listener.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     */
    public void addClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Error: Listener can't be null");
        }

        if (!clockListeners.contains(listener)) {
            clockListeners.add(listener);
        }
    }

    /**
     * Remove a new event listener from the list of event listeners.
     *
     * @param listener The event listener to remove.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     * @throws IllegalArgumentException Thrown if the listener wasn't registered.
     */
    public void removeClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Internal Error: Listener can't be null");
        }

        if (!clockListeners.remove(listener)) {
            throw new IllegalArgumentException("Internal Error: Listener was not listening on object");
        }
        
        // No listeners left to fire it, so any timeout in progress is moot.
        if (clockListeners.isEmpty()) {
            timeout = 0;
        }
    }

    public void setSpectrumModel(MachineTypes spectrumModel) {
        this.spectrumModel = spectrumModel;
        reset();
    }

    public int getTstates() {
        return tstates;
    }

    public void setTstates(int states) {
        tstates = states;
        frames = timeout = 0;
    }

    public void addTstates(int states) {

        if (timeout > 0) {
            if (states < timeout) {
                timeout -= states;
                tstates += states;
            } else {
                tstates += timeout;
                long diff = states - timeout;
                timeout = 0;
                for (final ClockTimeoutListener listener : clockListeners) {
                    listener.clockTimeout();
                }
                tstates += diff;
                timeout -= diff;
            }
        } else {
            tstates += states;
        }
    }

    public long getFrames() {
        return frames;
    }

    public void endFrame() {
        frames++;
        tstates %= spectrumModel.tstatesFrame;
    }

    public long getAbsTstates() {
        return frames * spectrumModel.tstatesFrame + tstates;
    }

    public void reset() {
        frames = timeout = tstates = 0;
    }

    public void setTimeout(int ntstates) {
        if (timeout > 0) {
            throw new ConcurrentModificationException("A timeout is in progress. Can't set another timeout!");
        }

        timeout = ntstates > 10 ? ntstates : 10;
    } 
    
    @Override
    public String toString() {
        return String.format("Frame: %d, t-states: %d", frames, tstates);
    }
}
