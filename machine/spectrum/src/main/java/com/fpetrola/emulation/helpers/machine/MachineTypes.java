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
    SPECTRUMPLUS3(5);

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
