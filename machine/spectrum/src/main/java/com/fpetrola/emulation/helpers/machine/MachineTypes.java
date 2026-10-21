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
package com.fpetrola.emulation.helpers.machine;

/**
 * 
 * @author jsanchez
 */ 
public enum MachineTypes { 

    SPECTRUM16K(0),
    SPECTRUM48K(1),
    SPECTRUM128K(2),
    SPECTRUMPLUS2(3),
    SPECTRUMPLUS2A(4),
    SPECTRUMPLUS3(5),
    /*
     * The rest of the machines a snapshot can name, as libspectrum numbers them: what a format needs
     * of each is which RAM it has and how long its frame is. The rest of their timings are the
     * machines' own.
     */
    SPECTRUMPLUS3E("ZX Spectrum +3e", "+3e", CodeModel.SPECTRUMPLUS3, 8, 70908),
    PENTAGON128("Pentagon 128", "Pentagon", CodeModel.SPECTRUM128K, 8, 71680),
    TC2048("Timex TC2048", "TC2048", CodeModel.SPECTRUM48K, 3, 69888),
    TC2068("Timex TC2068", "TC2068", CodeModel.SPECTRUM48K, 3, 69888),
    SCORPION("Scorpion ZS-256", "Scorpion", CodeModel.SPECTRUMPLUS3, 16, 69888),
    SPECTRUMSE("ZX Spectrum SE", "SE", CodeModel.SPECTRUM128K, 9, 69888),
    TS2068("Timex TS2068", "TS2068", CodeModel.SPECTRUM48K, 3, 58688),
    PENTAGON512("Pentagon 512", "Pentagon 512", CodeModel.SPECTRUM128K, 32, 71680),
    PENTAGON1024("Pentagon 1024", "Pentagon 1024", CodeModel.SPECTRUM128K, 64, 71680),
    SPECTRUM48KNTSC("ZX Spectrum 48K (NTSC)", "48k NTSC", CodeModel.SPECTRUM48K, 3, 59136),
    SPECTRUM128KE("ZX Spectrum 128Ke", "128Ke", CodeModel.SPECTRUM128K, 8, 70908);

    public enum CodeModel { SPECTRUM48K, SPECTRUM128K, SPECTRUMPLUS3 }

  public CodeModel codeModel;
    private String longModelName;
    private String shortModelName;
    public int clockFreq;
    public int tstatesFrame;
    public int tstatesLine;
    public int upBorderWidth;
    public int scanLines;
    public int firstScrByte;
    public int lastScrUpdate;
    public int outOffset;
    public int outBorderOffset;
    public int lengthINT;
    private boolean hasAY8912;
    private boolean hasDisk;
    private int ramBanks = 8;

    MachineTypes(int model) {
        switch (model) {
            case 0:
                this.longModelName = "ZX Spectrum 16K";
                this.shortModelName = "16k";
                this.clockFreq = 3500000;
                this.tstatesFrame = 69888;
                this.tstatesLine = 224;
                this.upBorderWidth = 64;
                this.scanLines = 312;
                this.lengthINT = 32;
                this.firstScrByte = 14336;
                this.lastScrUpdate = 57246;
                this.outOffset = 3;
                this.outBorderOffset = 0;
                this.hasAY8912 = false;
                this.hasDisk = false;
                this.codeModel = CodeModel.SPECTRUM48K;
                break;
            case 1:
                this.longModelName = "ZX Spectrum 48K";
                this.shortModelName = "48k";
                this.clockFreq = 3500000;
                this.tstatesFrame = 69888;
                this.tstatesLine = 224;
                this.upBorderWidth = 64;
                this.scanLines = 312;
                this.lengthINT = 32;
                this.firstScrByte = 14336;
                this.lastScrUpdate = 57246;
                this.outOffset = 3;
                this.outBorderOffset = 0;
                this.hasAY8912 = false;
                this.hasDisk = false;
                this.codeModel = CodeModel.SPECTRUM48K;
                break;
            case 2:
                this.longModelName = "ZX Spectrum 128K";
                this.shortModelName = "128";
                this.clockFreq = 3546900;
                this.tstatesFrame = 70908;
                this.tstatesLine = 228;
                this.upBorderWidth = 63;
                this.scanLines = 311;
                this.lengthINT = 36;
                this.firstScrByte = 14362;
                this.lastScrUpdate = 58040;
                this.outOffset = 1;
                this.outBorderOffset = 2;
                this.hasAY8912 = true;
                this.hasDisk = false;
                this.codeModel = CodeModel.SPECTRUM128K;
                break;
            case 3:
                this.longModelName = "Amstrad ZX Spectrum +2";
                this.shortModelName = "  +2";
                this.clockFreq = 3546900;
                this.tstatesFrame = 70908;
                this.tstatesLine = 228;
                this.upBorderWidth = 63;
                this.scanLines = 311;
                this.lengthINT = 36;
                this.firstScrByte = 14362;
                this.lastScrUpdate = 58040;
                this.outOffset = 1;
                this.outBorderOffset = 2;
                this.hasAY8912 = true;
                this.hasDisk = false;
                this.codeModel = CodeModel.SPECTRUM128K;
                break;
            case 4:
                this.longModelName = "ZX Spectrum +2A";
                this.shortModelName = "+2A";
                this.clockFreq = 3546900;
                this.tstatesFrame = 70908;
                this.tstatesLine = 228;
                this.upBorderWidth = 63;
                this.scanLines = 311;
                this.lengthINT = 32;
                this.firstScrByte = 14364;
                this.lastScrUpdate = 58044;
                this.outOffset = 1;
                this.outBorderOffset = -1;
                this.hasAY8912 = true;
                this.hasDisk = false;
                this.codeModel = CodeModel.SPECTRUMPLUS3;
                break;
            case 5:
                this.longModelName = "ZX Spectrum +3";
                this.shortModelName = "  +3";
                this.clockFreq = 3546900;
                this.tstatesFrame = 70908;
                this.tstatesLine = 228;
                this.upBorderWidth = 63;
                this.scanLines = 311;
                this.lengthINT = 32;
                this.firstScrByte = 14364;
                this.lastScrUpdate = 58044;
                this.outOffset = 1;
                this.outBorderOffset = -1;
                this.hasAY8912 = true;
                this.hasDisk = true;
                this.codeModel = CodeModel.SPECTRUMPLUS3;
                break;
        }
    }

    MachineTypes(String longModelName, String shortModelName, CodeModel codeModel, int ramBanks, int tstatesFrame) {
        this.longModelName = longModelName;
        this.shortModelName = shortModelName;
        this.codeModel = codeModel;
        this.ramBanks = ramBanks;
        this.tstatesFrame = tstatesFrame;
    }

    /** The banks of 16K of RAM a paged one has; one without paging has those of a 48K. */
    public int ramBanks() {
        return ramBanks;
    }

    public String getLongModelName() {
        return longModelName;
    }

    public String getShortModelName() {
        return shortModelName;
    }

    public int getTstatesFrame() {
        return tstatesFrame;
    }

    public int getTstatesLine() {
        return tstatesLine;
    }

    public int getBorderLines() {
        return upBorderWidth;
    }

    public int getScanLines() {
        return scanLines;
    }

    public int getTstatesINT() {
        return lengthINT;
    }

    public boolean hasAY8912() {
        return hasAY8912;
    }

    public boolean hasDisk() {
        return hasDisk;
    }

}
