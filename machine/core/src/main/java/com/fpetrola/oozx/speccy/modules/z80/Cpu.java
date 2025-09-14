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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.z80.cpu.*;

import static com.fpetrola.z80.registers.RegisterName.*;

/**
 * The processor a Spectrum drives, and everything the machine around it needs: raise an interrupt
 * at the end of a frame, read the clock to place events within one, watch the address bus, and
 * keep the processor's own time base aligned when a frame boundary moves the clock back.
 * <p>
 * One class and no interface over it. There was one, for machines to hold something that did not
 * drag in Swing, snapshot loading and poke files - and none of that is here any more, so the two
 * had the same shape and only this one ever implemented it.
 */
@Singleton
public class Cpu {
    private final Scheduler scheduler;
    private OOZ80 ooz80;
    private final MachineLoop loop;
    private final Task retriggeredInterrupt;
    private final SpectrumZ80Clock zxClock;
    private volatile boolean emulatorPaused;
    private boolean stopBeforeExecuting;

    private final PcTraps beforeFetch = new PcTraps();
    private final PcTraps afterInstruction = new PcTraps();
    private final Task nonMaskableInterrupt;

    @Inject
    public Cpu(Scheduler scheduler, MemoryBus memory, Display display, Ula ula, SpectrumZ80Clock zxClock, Processors processors, MachineLoop loop) {
        this.scheduler = scheduler;
        this.zxClock = zxClock;
        this.loop = loop;
        loop.drive(this);
        retriggeredInterrupt = scheduler.register(new RetriggeredInterrupt());
        nonMaskableInterrupt = scheduler.register(new NonMaskableInterrupt());
        processors.startOn(this);
    }

    /** The machine was reset; the processor goes back to where it starts. */
    public void machineWasReset(boolean hard) {
        reset(hard ? 1 : 0);
    }

    public void reset(int hardReset) {
        getOoz80().reset();

        State state = getOoz80().getState();

        state.getRegister(AF).write(0xffff);
        state.getRegister(AFx).write(0xffff);
        state.getRegister(BC).write(0);
        state.getRegister(DE).write(0);
        state.getRegister(HLx).write(0);
        state.getRegister(BC).write(0);
        state.getRegister(DEx).write(0);
        state.getRegister(HLx).write(0);
        state.getRegister(IX).write(0);
        state.getRegister(IY).write(0);
        state.getRegister(PC).write(0);
        state.getRegister(SP).write(0xffff);

        state.getRegister(I).write(0);
        state.getRegister(R).write(0);
    }

    /** The clock the CPU drives. Machines read it to place events within a frame. */
    public SpectrumZ80Clock getClock() {
        return zxClock;
    }


    /**
     * How long the machine holds /INT down, as it said when it raised it. A frame's clock starts at
     * zero, so it is both the length of the pulse and the T-state the line comes back up at.
     */
    private int interruptLength;

    public void interrupt(int forHowLong) {
        interruptLength = forHowLong;
        takeInterruptIfTheLineIsStillDown();
    }

    /**
     * The processor has just enabled interrupts: one is retried a T-state later, which is how a
     * program that starts its frame with interrupts off still gets the one raised while they were.
     * <p>
     * Scheduled on every EI and not only inside the interrupt window, which looks wasteful and is
     * not: the frame end rebases every due T-state by a frame, so a retry asked for near the end of
     * one frame comes due at the start of the next, with that frame's line down. Guarding it on the
     * window as it stands at the EI drops exactly that case, and it is the case the retry is for.
     */
    public void interruptsEnabled(long tStates) {
        scheduler.schedule(retriggeredInterrupt, tStates + 1);
    }

    /**
     * The line was pulled by the machine and is still down for as long as it said.
     */
    private void takeInterruptIfTheLineIsStillDown() {
        if (getOoz80().getState().isIff1() && zxClock.getTStates() < interruptLength) {
            zxClock.acknowledge(7);
            getOoz80().interruption();
        }
    }


    private final java.util.List<Runnable> nmiListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public OOZ80 getOoz80() {
        return ooz80;
    }

    public void setOoz80(OOZ80 ooz80) {
        this.ooz80 = ooz80;
    }

    /**
     * Eleven T-states: five of the processor's own, and the two pushes the memory counts.
     */
    private final class NonMaskableInterrupt extends Task {

        public void run(long due) {
            zxClock.acknowledge(5);
            nmiListeners.forEach(Runnable::run);
            getOoz80().nmi();
        }
    }

    /** Told as an NMI is taken, before the jump: a Beta pages its ROM in there. */
    public void onNmi(Runnable listener) {
        nmiListeners.add(listener);
    }

    public void offNmi(Runnable listener) {
        nmiListeners.remove(listener);
    }

    /** Where the processor goes on next: a device that boots the machine into its own ROM sets it. */
    public void jump(int address) {
        getOoz80().getState().getPc().write(address);
    }

    /** As if the instruction about to run were RST to that vector: the address after it is pushed. */
    public void rst(int vector) {
        var state = getOoz80().getState();
        com.fpetrola.z80.instructions.impl.Push.doPush((state.getPc().read() + 1) & 0xffff, state.getRegisterSP(), state.getMemory());
        state.getPc().write(vector);
    }

    /** Pulls /NMI: taken between this instruction and the next, at 0x0066. */
    public void nmi() {
        scheduler.schedule(nonMaskableInterrupt, zxClock.getTStates());
    }

    /** What is watching the address bus before each fetch. */
    public PcTraps beforeFetch() {
        return beforeFetch;
    }

    /** What is watching it once the instruction fetched there has run. */
    public PcTraps afterInstruction() {
        return afterInstruction;
    }

    /**
     * One instruction, and whatever is watching the address it was fetched from is told.
     * <p>
     * The watched case is a method of its own so that this one stays small enough to be inlined into
     * the loop that calls it: with the two folded together the JIT gave up part way down, and a port
     * read that is normally scalar-replaced started escaping - 12 bytes an access and 192 a frame,
     * which CostRegressionTest reports and nothing else notices.
     */
    public void step() {
        if (beforeFetch.armed() || afterInstruction.armed()) {
            stepWatched();
            return;
        }
        getOoz80().execute();
    }

    private void stepWatched() {
        int pc = getOoz80().getState().getPc().read();
        beforeFetch.at(pc);
        if (stopBeforeExecuting) {
            stopBeforeExecuting = false;
            return;
        }
        getOoz80().execute();
        afterInstruction.at(pc);
    }

    public boolean isPaused() {
        return emulatorPaused;
    }

    public void setPaused(boolean paused) {
        emulatorPaused = paused;
    }

    /**
     * From inside a {@link #beforeFetch} trap: the machine stops at the address being watched and
     * the instruction there does not run. Pausing on its own stops it after whatever it was in the
     * middle of, which for a breakpoint is one instruction too late.
     */
    public void stopHere() {
        stopBeforeExecuting = true;
        loop.setPaused(true);
    }

    private final class RetriggeredInterrupt extends Task {

        public void run(long due) {
            takeInterruptIfTheLineIsStillDown();
        }
    }
}
