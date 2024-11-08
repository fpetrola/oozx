/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.CPU.RunState;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.SleepUtils;
import net.emustudio.plugins.cpu.intel8080.api.CpuEngine;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main implementation class for CPU emulation CPU works in a separate thread
 * (parallel with other hardware)
 */
// TODO: set frequency runtime
public class EmulatorEngine implements CpuEngine {
    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;

    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_Y = 0x20, FLAG_H = 0x10, FLAG_X = 0x8, FLAG_PV = 0x4, FLAG_N = 0x02, FLAG_C = 0x1;
    public static final int FLAG_SZP = FLAG_S | FLAG_Z | FLAG_PV;
    private static final int FLAG_SZC = FLAG_S | FLAG_Z | FLAG_C;
    private static final int FLAG_XY = FLAG_X | FLAG_Y;

    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);
    private final static int[] CONDITION = new int[]{
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };

    private final MemoryContext<Byte> memory;

    public final int[] regs = new int[8];
    public final int[] regs2 = new int[8];
    public final boolean[] IFF = new boolean[2]; // interrupt enable flip-flops

    public int flags = 2;
    public int flags2 = 2;

    // special registers
    public int PC = 0, SP = 0, IX = 0, IY = 0;
    public int I = 0, R = 0; // interrupt r., refresh r.
    public int memptr = 0; // internal register, https://gist.github.com/drhelius/8497817
    public int Q = 0; // internal register
    public int lastQ = 0; // internal register

    private final Queue<byte[]> pendingInterrupts = new ConcurrentLinkedQueue<>(); // must be thread-safe; can cause stack overflow
    // non-maskable interrupts are always executed
    private final AtomicBoolean pendingNonMaskableInterrupt = new AtomicBoolean();

    public byte interruptMode = 0;
    private boolean interruptSkip; // when EI enabled, skip next instruction interrupt

    private int lastOpcode;
    private RunState currentRunState = RunState.STATE_STOPPED_NORMAL;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Byte> memory) {
        this.memory = Objects.requireNonNull(memory);
        LOGGER.info("Sleep precision: " + SleepUtils.SLEEP_PRECISION + " nanoseconds.");
    }

    @SuppressWarnings("unused")
    public static String intToFlags(int flags) {
        String flagsString = "";
        if ((flags & FLAG_S) == FLAG_S) {
            flagsString += "S";
        }
        if ((flags & FLAG_Z) == FLAG_Z) {
            flagsString += "Z";
        }
        if ((flags & FLAG_Y) == FLAG_Y) {
            flagsString += "Y";
        }
        if ((flags & FLAG_H) == FLAG_H) {
            flagsString += "H";
        }
        if ((flags & FLAG_X) == FLAG_X) {
            flagsString += "X";
        }
        if ((flags & FLAG_PV) == FLAG_PV) {
            flagsString += "P";
        }
        if ((flags & FLAG_N) == FLAG_N) {
            flagsString += "N";
        }
        if ((flags & FLAG_C) == FLAG_C) {
            flagsString += "C";
        }
        return flagsString;
    }

    @Override
    public void setDispatchListener(DispatchListener dispatchListener) {
        this.dispatchListener = dispatchListener;
    }

    void reset(int startPos) {
        IX = IY = 0;
        SP = 0xFFFF;
        I = R = 0;
        memptr = 0;
        Q = lastQ = 0;
        Arrays.fill(regs, 0);
        Arrays.fill(regs2, 0);
        flags = 0;
        flags2 = 0;
        interruptMode = 0;
        IFF[0] = false;
        IFF[1] = false;
        pendingNonMaskableInterrupt.set(false);
        PC = startPos;
        pendingInterrupts.clear();
        currentRunState = RunState.STATE_STOPPED_BREAK;
    }

    RunState step() throws Exception {
        currentRunState = RunState.STATE_STOPPED_BREAK;
        try {
            dispatch();
        } catch (Throwable e) {
            throw new Exception(e);
        }
        return currentRunState;
    }

    public RunState run(CPU cpu) {
        // In Z80, 1 t-state = 250 ns = 0.25 microseconds = 0.00025 milliseconds
        // in 1 millisecond time slot = 1 / 0.00025 = 4000 t-states are executed uncontrollably
        currentRunState = RunState.STATE_RUNNING;
        return currentRunState;
    }

    private void dispatch() throws Throwable {
        DispatchListener tmpListener = dispatchListener;
        if (tmpListener != null) {
            tmpListener.beforeDispatch();
        }

        try {
            lastQ = Q;
            Q = 0;
            if (pendingNonMaskableInterrupt.getAndSet(false)) {
                if (memory.read(PC) == 0x76) {
                    // jump over HALT - this is probably wrong
                    writeWord((SP - 2) & 0xFFFF, (PC + 1) & 0xFFFF);
                } else {
                    writeWord((SP - 2) & 0xFFFF, PC);
                }

                SP = (SP - 2) & 0xffff;
                PC = 0x66;
                return;
            }
            if (interruptSkip) {
                interruptSkip = false; // See EI
            } else if (IFF[0] && !pendingInterrupts.isEmpty()) {
                doInterrupt();
            } else if (!pendingInterrupts.isEmpty()) {
                pendingInterrupts.poll(); // if interrupts are disabled, ignore it; otherwise stack overflow
            }
            //DISPATCH(DISPATCH_TABLE);
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
    }

    private void DISPATCH(MethodHandle[] table) throws Throwable {
        lastOpcode = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            instr.invokeExact(this);
        }
    }

    private void doInterrupt() throws Throwable {
    }

    private int getReg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        }
        return regs[reg];
    }

    private void putReg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (val & 0xFF));
        } else {
            regs[reg] = val & 0xFF;
        }
    }

    private void putPair(int reg, int val) {
        int high = (val >>> 8) & 0xFF;
        int low = val & 0xFF;
        int index = reg * 2;

        if (reg == 3) {
            SP = val;
        } else {
            regs[index] = high;
            regs[index + 1] = low;
        }
    }

    private boolean getCC1(int cc) {
        switch (cc) {
            case 0:
                return ((flags & FLAG_Z) == 0); // NZ
            case 1:
                return ((flags & FLAG_Z) != 0); // Z
            case 2:
                return ((flags & FLAG_C) == 0); // NC
            case 3:
                return ((flags & FLAG_C) != 0); // C
        }
        return false;
    }

    private int readWord(int address) {
        Byte[] read = memory.read(address, 2);
        return ((read[1] << 8) | (read[0] & 0xFF)) & 0xFFFF;
    }

    private void writeWord(int address, int value) {
        memory.write(address, new Byte[]{(byte) (value & 0xFF), (byte) ((value >>> 8) & 0xFF)}, 2);
    }

    private void incrementR() {
        R = (R & 0x80) | ((R + 1) & 0x7F);
    }
}
