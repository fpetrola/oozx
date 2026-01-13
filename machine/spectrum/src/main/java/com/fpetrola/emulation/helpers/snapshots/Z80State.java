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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fpetrola.emulation.helpers.snapshots;

import z80core.IntMode;

/**
 *
 * @author jsanchez
 */ 
public class Z80State {
    private int regA, regB, regC, regD, regE, regH, regL;
    private int regF;
    private boolean flagQ;
    private int regAx;
    private int regFx;
    private int regBx, regCx, regDx, regEx, regHx, regLx;
    private int regPC;
    private int regIX;
    private int regIY;
    private int regSP;
    private int regI;
    private int regR;
    private boolean ffIFF1 = false;
    private boolean ffIFF2 = false;
    // EI only enables interrupts after the instruction that follows it has executed.
    private boolean pendingEI = false;
    private boolean activeNMI = false;
    // The INT line is held active for 32 T-states on a 48K and 36 on a 128K or later model.
    private boolean activeINT = false;
    private IntMode modeINT = IntMode.IM0;
    private boolean halted = false;
    // Undocumented internal register (WZ): holds H's value before ADD HL,xx, the high byte of
    // IX/IY+d for indexed loads, and the high byte of a JR's target address; see
    // http://zx.pk.ru/attachment.php?attachmentid=2989 for the instructions that also touch it.
    private int memptr;
    
    public Z80State() {
    }
    
    public final int getRegA() {
        return regA;
    }

    public final void setRegA(int value) {
        regA = value & 0xff;
    }
    
    public final int getRegF() {
        return regF;
    }

    public final void setRegF(int value) {
        regF = value & 0xff;
    }

    public final int getRegB() {
        return regB;
    }

    public final void setRegB(int value) {
        regB = value & 0xff;
    }

    public final int getRegC() {
        return regC;
    }

    public final void setRegC(int value) {
        regC = value & 0xff;
    }

    public final int getRegD() {
        return regD;
    }

    public final void setRegD(int value) {
        regD = value & 0xff;
    }

    public final int getRegE() {
        return regE;
    }

    public final void setRegE(int value) {
        regE = value & 0xff;
    }

    public final int getRegH() {
        return regH;
    }

    public final void setRegH(int value) {
        regH = value & 0xff;
    }

    public final int getRegL() {
        return regL;
    }

    public final void setRegL(int value) {
        regL = value & 0xff;
    }

    public final int getRegAx() {
        return regAx;
    }

    public final void setRegAx(int value) {
        regAx = value & 0xff;
    }
    
    public final int getRegFx() {
        return regFx;
    }

    public final void setRegFx(int value) {
        regFx = value & 0xff;
    }

    public final int getRegBx() {
        return regBx;
    }

    public final void setRegBx(int value) {
        regBx = value & 0xff;
    }

    public final int getRegCx() {
        return regCx;
    }

    public final void setRegCx(int value) {
        regCx = value & 0xff;
    }

    public final int getRegDx() {
        return regDx;
    }

    public final void setRegDx(int value) {
        regDx = value & 0xff;
    }

    public final int getRegEx() {
        return regEx;
    }

    public final void setRegEx(int value) {
        regEx = value & 0xff;
    }

    public final int getRegHx() {
        return regHx;
    }

    public final void setRegHx(int value) {
        regHx = value & 0xff;
    }

    public final int getRegLx() {
        return regLx;
    }

    public final void setRegLx(int value) {
        regLx = value & 0xff;
    }

    public final int getRegAF() {
        return (regA << 8) | regF;
    }

    public final void setRegAF(int word) {
        regA = (word >>> 8) & 0xff;

        regF = word & 0xff;
    }

    public final int getRegAFx() {
        return (regAx << 8) | regFx;
    }

    public final void setRegAFx(int word) {
        regAx = (word >>> 8) & 0xff;
        regFx = word & 0xff;
    }

    public final int getRegBC() {
        return (regB << 8) | regC;
    }

    public final void setRegBC(int word) {
        regB = (word >>> 8) & 0xff;
        regC = word & 0xff;
    }

    public final int getRegBCx() {
        return (regBx << 8) | regCx;
    }

    public final void setRegBCx(int word) {
        regBx = (word >>> 8) & 0xff;
        regCx = word & 0xff;
    }

    public final int getRegDE() {
        return (regD << 8) | regE;
    }

    public final void setRegDE(int word) {
        regD = (word >>> 8) & 0xff;
        regE = word & 0xff;
    }

    public final int getRegDEx() {
        return (regDx << 8) | regEx;
    }

    public final void setRegDEx(int word) {
        regDx = (word >>> 8) & 0xff;
        regEx = word & 0xff;
    }

    public final int getRegHL() {
        return (regH << 8) | regL;
    }

    public final void setRegHL(int word) {
        regH = (word >>> 8) & 0xff;
        regL = word & 0xff;
    }

    public final int getRegHLx() {
        return (regHx << 8) | regLx;
    }

    public final void setRegHLx(int word) {
        regHx = (word >>> 8) & 0xff;
        regLx = word & 0xff;
    }

    public final int getRegPC() {
        return regPC;
    }

    public final void setRegPC(int address) {
        regPC = address & 0xffff;
    }

    public final int getRegSP() {
        return regSP;
    }

    public final void setRegSP(int word) {
        regSP = word & 0xffff;
    }

    public final int getRegIX() {
        return regIX;
    }

    public final void setRegIX(int word) {
        regIX = word & 0xffff;
    }

    public final int getRegIY() {
        return regIY;
    }

    public final void setRegIY(int word) {
        regIY = word & 0xffff;
    }

    public final int getRegI() {
        return regI;
    }

    public final void setRegI(int value) {
        regI = value & 0xff;
    }

    public final int getRegR() {
        return regR;
    }

    public final void setRegR(int value) {
        regR = value & 0xff;
    }

    public final int getMemPtr() {
        return memptr;
    }

    public final void setMemPtr(int word) {
        memptr = word & 0xffff;
    }
    
    public final boolean isIFF1() {
        return ffIFF1;
    }

    public final void setIFF1(boolean state) {
        ffIFF1 = state;
    }

    public final boolean isIFF2() {
        return ffIFF2;
    }

    public final void setIFF2(boolean state) {
        ffIFF2 = state;
    }

    public final boolean isNMI() {
        return activeNMI;
    }
    
    public final void setNMI(boolean nmi) {
        activeNMI = nmi;
    }

    // NMI is edge-triggered, not level-triggered.
    public final void triggerNMI() {
        activeNMI = true;
    }

    // INT is level-triggered.
    public final boolean isINTLine() {
        return activeINT;
    }
    
    public final void setINTLine(boolean intLine) {
        activeINT = intLine;
    }

    public final IntMode getIM() {
        return modeINT;
    }

    public final void setIM(IntMode mode) {
        modeINT = mode;
    }

    public final boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean state) {
        halted = state;
    }
    
    public final boolean isPendingEI() {
        return pendingEI;
    }
    
    public final void setPendingEI(boolean state) {
        pendingEI = state;
    }

    public boolean isFlagQ() {
        return flagQ;
    }

    public void setFlagQ(boolean flagQ) {
        this.flagQ = flagQ;
    }
}
